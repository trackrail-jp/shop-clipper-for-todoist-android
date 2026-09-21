package jp.trackrail.shopclipper.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.trackrail.shopclipper.core.DEFAULT_PRIORITY
import jp.trackrail.shopclipper.core.DEFAULT_SETTINGS
import jp.trackrail.shopclipper.core.Draft
import jp.trackrail.shopclipper.core.DraftForm
import jp.trackrail.shopclipper.core.ProjectOption
import jp.trackrail.shopclipper.core.Settings
import jp.trackrail.shopclipper.core.SettingsStore
import jp.trackrail.shopclipper.core.buildContent
import jp.trackrail.shopclipper.core.buildProjectOptions
import jp.trackrail.shopclipper.core.buildTaskFromDraft
import jp.trackrail.shopclipper.core.draftFromShare
import jp.trackrail.shopclipper.core.findDuplicate
import jp.trackrail.shopclipper.core.labelsFor
import jp.trackrail.shopclipper.core.loadSettings
import jp.trackrail.shopclipper.core.parseLabels
import jp.trackrail.shopclipper.core.sortSections
import jp.trackrail.shopclipper.core.stripMarkdownLinks
import jp.trackrail.shopclipper.core.taskWebUrl
import jp.trackrail.shopclipper.net.HttpTransport
import jp.trackrail.shopclipper.share.SharedTextParser
import jp.trackrail.shopclipper.share.ShortUrlResolver
import jp.trackrail.shopclipper.todoist.TodoistClient
import jp.trackrail.shopclipper.todoist.TodoistError
import jp.trackrail.shopclipper.todoist.TodoistSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which of the four things the sheet is showing (計画書 §4). */
enum class ShareStage { Loading, NeedsToken, NoUrl, Form, Added }

data class ShareUiState(
    val stage: ShareStage = ShareStage.Loading,
    // The form (計画書 §4). Everything but the name and the memo comes from the share.
    val name: String = "",
    val fullTitle: String = "",
    val code: String? = null,
    val codeLabel: String = "",
    val url: String = "",
    val price: String = "",
    val memo: String = "",
    val projects: List<ProjectOption> = emptyList(),
    val sections: List<TodoistSection> = emptyList(),
    val projectId: String = "",
    val sectionId: String = "",
    val labels: String = "",
    val priority: Int = DEFAULT_PRIORITY,
    val targetLabel: String = "",
    /** Duplicate, a short URL that could not be resolved, a list that could not be read. */
    val warnings: List<String> = emptyList(),
    /** Why 追加 failed. The sheet stays open so it can be pressed again (D9). */
    val error: String? = null,
    val busy: Boolean = false,
    val addedTaskUrl: String? = null,
) {
    /** How long the task name will be, so the form can show "n / 500". */
    val contentLength: Int get() = buildContent(name = name, url = url).content.length

    val isProduct: Boolean get() = !code.isNullOrEmpty()
}

// The share sheet's logic (計画書 §4・§5). The share opens the form every time
// (D5), the price is typed by hand (D6), and a short URL is resolved before the
// form is filled (D8 when that fails).
class ShareViewModel(
    private val store: SettingsStore,
    private val transport: HttpTransport,
) : ViewModel() {

    private val _state = MutableStateFlow(ShareUiState())
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    private var settings: Settings = DEFAULT_SETTINGS
    private var draft: Draft? = null

    fun load(text: String?, subject: String?) {
        viewModelScope.launch {
            settings = loadSettings(store)
            if (settings.token.isEmpty()) {
                _state.value = _state.value.copy(stage = ShareStage.NeedsToken)
                return@launch
            }
            val share = SharedTextParser.parse(text, subject)
            if (share == null) {
                _state.value = _state.value.copy(stage = ShareStage.NoUrl)
                return@launch
            }
            val warnings = mutableListOf<String>()
            var resolved: String? = null
            if (share.isShortUrl) {
                when (val result = ShortUrlResolver(transport).resolve(share.url)) {
                    is ShortUrlResolver.Result.Resolved -> resolved = result.url
                    // D8: keep the short URL and say so; 追加 still works.
                    is ShortUrlResolver.Result.Failed ->
                        warnings += "短縮 URL の先を確かめられませんでした（${result.reason}）。このまま追加すると、短縮 URL のまま登録されます。"
                }
            }
            val draft = draftFromShare(share, resolved, settings)
            this@ShareViewModel.draft = draft
            _state.value = _state.value.copy(
                stage = ShareStage.Form,
                name = draft.name,
                fullTitle = draft.fullTitle,
                code = draft.code,
                codeLabel = draft.site?.codeLabel ?: "",
                url = draft.url,
                projectId = settings.projectId,
                sectionId = settings.sectionId,
                labels = labelsFor(draft.site, settings).joinToString(", "),
                priority = settings.priority,
                targetLabel = settings.targetLabel,
                warnings = warnings,
            )
            checkDuplicate(draft)
            loadDestinations()
        }
    }

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value, error = null)
    }

    fun onPriceChange(value: String) {
        _state.value = _state.value.copy(price = value, error = null)
    }

    fun onMemoChange(value: String) {
        _state.value = _state.value.copy(memo = value, error = null)
    }

    fun onLabelsChange(value: String) {
        _state.value = _state.value.copy(labels = value, error = null)
    }

    fun onPriorityChange(value: Int) {
        _state.value = _state.value.copy(priority = value, error = null)
    }

    fun onSectionSelected(id: String) {
        _state.value = _state.value.copy(sectionId = id, error = null)
    }

    fun onProjectSelected(id: String) {
        _state.value = _state.value.copy(projectId = id, sectionId = "", sections = emptyList(), error = null)
        if (id.isEmpty()) return
        viewModelScope.launch {
            _state.value = try {
                _state.value.copy(sections = sortSections(client().listSections(id)))
            } catch (e: TodoistError) {
                _state.value.copy(warnings = _state.value.warnings + "セクションを読めませんでした（${e.text}）")
            }
        }
    }

    /** 追加: builds the task the same way the extension does and posts it. */
    fun add() {
        val draft = draft ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            val current = _state.value
            val payload = buildTaskFromDraft(
                draft,
                DraftForm(
                    name = current.name,
                    price = current.price,
                    memo = current.memo,
                    projectId = current.projectId,
                    sectionId = current.sectionId,
                    labels = parseLabels(current.labels),
                    priority = current.priority,
                ),
            )
            _state.value = try {
                val task = client().createTask(payload)
                _state.value.copy(
                    busy = false,
                    stage = ShareStage.Added,
                    addedTaskUrl = task?.let { taskWebUrl(it.id) },
                )
            } catch (e: TodoistError) {
                _state.value.copy(busy = false, error = e.text)
            }
        }
    }

    // The same product is warned about, not blocked (D5). The code is looked up,
    // so a nickname in the task name does not hide it.
    private suspend fun checkDuplicate(draft: Draft) {
        val code = draft.code ?: return
        val existing = try {
            findDuplicate(client(), code)
        } catch (e: TodoistError) {
            _state.value = _state.value.copy(warnings = _state.value.warnings + "重複を確かめられませんでした（${e.text}）")
            return
        }
        if (existing != null) {
            val name = stripMarkdownLinks(existing.content)
            _state.value = _state.value.copy(warnings = _state.value.warnings + "同じ${draft.site?.codeName ?: "商品"}のタスクがすでにあります: $name")
        }
    }

    // The pickers' contents. Without them the form still works with the saved
    // destination, so a failure is only a warning.
    private suspend fun loadDestinations() {
        try {
            val projects = buildProjectOptions(client().listProjects())
            val projectId = if (projects.any { it.id == _state.value.projectId }) _state.value.projectId else ""
            val sections = if (projectId.isEmpty()) emptyList() else sortSections(client().listSections(projectId))
            val sectionId = if (sections.any { it.id == _state.value.sectionId }) _state.value.sectionId else ""
            _state.value = _state.value.copy(projects = projects, sections = sections, projectId = projectId, sectionId = sectionId)
        } catch (e: TodoistError) {
            _state.value = _state.value.copy(warnings = _state.value.warnings + "登録先の一覧を読めませんでした（${e.text}）。保存済みの登録先に追加します。")
        }
    }

    private fun client(): TodoistClient = TodoistClient(settings.token, transport)

}
