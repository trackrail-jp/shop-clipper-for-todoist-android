package jp.trackrail.shopclipper.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.trackrail.shopclipper.core.DEFAULT_PRIORITY
import jp.trackrail.shopclipper.core.DEFAULT_TARGET_LABEL
import jp.trackrail.shopclipper.core.DEFAULT_TITLE_LIMIT
import jp.trackrail.shopclipper.core.ProjectOption
import jp.trackrail.shopclipper.core.SETTINGS_VERSION
import jp.trackrail.shopclipper.core.SettingsStore
import jp.trackrail.shopclipper.core.buildProjectOptions
import jp.trackrail.shopclipper.core.collapse
import jp.trackrail.shopclipper.core.loadSettings
import jp.trackrail.shopclipper.core.saveSettings
import jp.trackrail.shopclipper.core.sortSections
import jp.trackrail.shopclipper.core.targetLabelOf
import jp.trackrail.shopclipper.net.HttpTransport
import jp.trackrail.shopclipper.todoist.TodoistClient
import jp.trackrail.shopclipper.todoist.TodoistError
import jp.trackrail.shopclipper.todoist.TodoistSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** A message under a button: green when it worked, red when it did not. */
data class Notice(val text: String, val ok: Boolean)

data class SettingsUiState(
    val loaded: Boolean = false,
    val token: String = "",
    val projects: List<ProjectOption> = emptyList(),
    val sections: List<TodoistSection> = emptyList(),
    val projectId: String = "",
    val sectionId: String = "",
    val labels: String = "",
    val siteLabels: Boolean = false,
    val priority: Int = DEFAULT_PRIORITY,
    // Text while it is being typed; core.normalizeSettings clamps it on save.
    val titleLimit: String = DEFAULT_TITLE_LIMIT.toString(),
    val targetLabel: String = DEFAULT_TARGET_LABEL,
    val busy: Boolean = false,
    val testNotice: Notice? = null,
    val saveNotice: Notice? = null,
    // 一過性のフラグ（計画書 D17 ④）: 保存できた直後だけ true。ダイアログを閉じると false に戻る。
    val justSaved: Boolean = false,
) {
    // ⛔ Never print the API token (logs, crash reports, test failures).
    override fun toString(): String =
        "SettingsUiState(loaded=$loaded, token=${if (token.isEmpty()) "" else "***"}, projects=${projects.size}, " +
            "sections=${sections.size}, projectId=$projectId, sectionId=$sectionId, labels=$labels, " +
            "siteLabels=$siteLabels, priority=$priority, titleLimit=$titleLimit, targetLabel=$targetLabel, " +
            "busy=$busy, testNotice=$testNotice, saveNotice=$saveNotice, justSaved=$justSaved)"
}

// The settings screen's state and actions (計画書 §4 の「ランチャーのアイコン → 設定画面」).
// The Todoist client is built from the token being typed, so 接続テスト checks the
// token before it is saved - the same order as the extension's options page.
class SettingsViewModel(
    private val store: SettingsStore,
    private val transport: HttpTransport,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Reads the saved settings and, when a token is already saved, runs 接続テスト once. */
    fun load() {
        viewModelScope.launch {
            val settings = loadSettings(store)
            _state.value = _state.value.copy(
                loaded = true,
                token = settings.token,
                projectId = settings.projectId,
                sectionId = settings.sectionId,
                labels = settings.labels.joinToString(", "),
                siteLabels = settings.siteLabels,
                priority = settings.priority,
                titleLimit = settings.titleLimit.toString(),
                targetLabel = settings.targetLabel,
            )
            if (settings.token.isNotEmpty()) connect()
        }
    }

    fun onTokenChange(value: String) {
        _state.value = _state.value.copy(token = value, testNotice = null)
    }

    fun onLabelsChange(value: String) {
        _state.value = _state.value.copy(labels = value, saveNotice = null)
    }

    fun onSiteLabelsChange(value: Boolean) {
        _state.value = _state.value.copy(siteLabels = value, saveNotice = null)
    }

    fun onPriorityChange(value: Int) {
        _state.value = _state.value.copy(priority = value, saveNotice = null)
    }

    fun onTitleLimitChange(value: String) {
        _state.value = _state.value.copy(titleLimit = value, saveNotice = null)
    }

    fun onSectionSelected(id: String) {
        _state.value = _state.value.copy(sectionId = id, saveNotice = null)
    }

    /** 接続テスト: reads the projects (and the sections of the chosen project) with the typed token. */
    fun testConnection() {
        viewModelScope.launch { connect() }
    }

    fun onProjectSelected(id: String) {
        _state.value = _state.value.copy(projectId = id, sectionId = "", sections = emptyList(), saveNotice = null)
        if (id.isEmpty()) return // the Inbox has no sections to choose
        viewModelScope.launch {
            val client = client() ?: return@launch
            _state.value = _state.value.copy(busy = true)
            _state.value = try {
                _state.value.copy(busy = false, sections = sortSections(client.listSections(id)))
            } catch (e: TodoistError) {
                _state.value.copy(busy = false, testNotice = Notice(e.text, ok = false))
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            val projectName = current.projects.firstOrNull { it.id == current.projectId }?.name.orEmpty()
            val sectionName = current.sections.firstOrNull { it.id == current.sectionId }?.name.orEmpty()
            val patch = mapOf(
                "token" to current.token,
                "projectId" to current.projectId,
                "sectionId" to current.sectionId,
                "labels" to current.labels,
                "siteLabels" to current.siteLabels,
                "priority" to current.priority,
                "titleLimit" to current.titleLimit,
                // Shown in the share form later, so it is stored with the ids.
                "targetLabel" to if (current.projectId.isEmpty()) DEFAULT_TARGET_LABEL else targetLabelOf(projectName, sectionName),
                "settingsVersion" to SETTINGS_VERSION,
            )
            _state.value = try {
                val saved = saveSettings(store, patch)
                _state.value.copy(
                    token = saved.token,
                    labels = saved.labels.joinToString(", "),
                    priority = saved.priority,
                    titleLimit = saved.titleLimit.toString(),
                    targetLabel = saved.targetLabel,
                    saveNotice = Notice("保存しました（既定の登録先: ${saved.targetLabel}）", ok = true),
                    justSaved = true,
                )
            } catch (e: IOException) {
                _state.value.copy(
                    saveNotice = Notice("保存できませんでした（${e.message}）", ok = false),
                    justSaved = false,
                )
            }
        }
    }

    /** 保存できたダイアログを閉じる（D17 ④）。 */
    fun dismissSaved() {
        _state.value = _state.value.copy(justSaved = false)
    }

    private suspend fun connect() {
        val client = client() ?: return
        _state.value = _state.value.copy(busy = true, testNotice = null)
        _state.value = try {
            val projects = client.listProjects()
            val options = buildProjectOptions(projects)
            val projectId = if (options.any { it.id == _state.value.projectId }) _state.value.projectId else ""
            val sections = if (projectId.isEmpty()) emptyList() else sortSections(client.listSections(projectId))
            val sectionId = if (sections.any { it.id == _state.value.sectionId }) _state.value.sectionId else ""
            _state.value.copy(
                busy = false,
                projects = options,
                sections = sections,
                projectId = projectId,
                sectionId = sectionId,
                testNotice = Notice("接続OK（プロジェクト ${projects.size} 件）", ok = true),
            )
        } catch (e: TodoistError) {
            _state.value.copy(busy = false, testNotice = Notice(e.text, ok = false))
        }
    }

    /** null when there is no token yet; the message says so. */
    private fun client(): TodoistClient? = try {
        TodoistClient(collapse(_state.value.token), transport)
    } catch (e: TodoistError) {
        _state.value = _state.value.copy(busy = false, testNotice = Notice(e.text, ok = false))
        null
    }
}
