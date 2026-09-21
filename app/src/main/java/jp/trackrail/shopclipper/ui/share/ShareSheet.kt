package jp.trackrail.shopclipper.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jp.trackrail.shopclipper.core.TASK_CONTENT_LIMIT
import jp.trackrail.shopclipper.ui.common.Chooser
import jp.trackrail.shopclipper.ui.common.InboxOption
import jp.trackrail.shopclipper.ui.common.NoSectionOption
import jp.trackrail.shopclipper.ui.common.Priorities

// The bottom sheet the share sheet opens (計画書 §4・D9).

/** What the sheet can do; the ViewModel supplies them. */
data class ShareActions(
    val onName: (String) -> Unit = {},
    val onPrice: (String) -> Unit = {},
    val onMemo: (String) -> Unit = {},
    val onLabels: (String) -> Unit = {},
    val onPriority: (Int) -> Unit = {},
    val onProject: (String) -> Unit = {},
    val onSection: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onOpenTask: (String) -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onClose: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(state: ShareUiState, actions: ShareActions) {
    ModalBottomSheet(
        onDismissRequest = actions.onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (state.stage) {
                ShareStage.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("読み込み中…")
                }

                ShareStage.NeedsToken -> {
                    Text("API トークンが未設定です", style = MaterialTheme.typography.titleMedium)
                    Text("設定画面で Todoist の API トークンを登録してください。", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = actions.onOpenSettings) { Text("設定を開く") }
                }

                ShareStage.NoUrl -> {
                    Text("URL が見つかりませんでした", style = MaterialTheme.typography.titleMedium)
                    Text("共有されたテキストに http(s) の URL が含まれていませんでした。", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = actions.onClose) { Text("閉じる") }
                }

                ShareStage.Added -> {
                    Text("追加しました", style = MaterialTheme.typography.titleMedium)
                    Text(state.name, style = MaterialTheme.typography.bodySmall)
                    if (state.addedTaskUrl != null) {
                        Button(onClick = { actions.onOpenTask(state.addedTaskUrl) }) { Text("Todoist で開く") }
                    }
                }

                ShareStage.Form -> Form(state, actions)
            }
        }
    }
}

@Composable
private fun Form(state: ShareUiState, actions: ShareActions) {
    for (warning in state.warnings) {
        Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    OutlinedTextField(
        value = state.name,
        onValueChange = actions.onName,
        label = { Text("タスク名") },
        supportingText = { Text("${state.contentLength} / $TASK_CONTENT_LIMIT 文字（リンクを含む）") },
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.fullTitle.isNotEmpty()) {
        Text(state.fullTitle, style = MaterialTheme.typography.bodySmall)
    }
    if (state.isProduct) {
        Text("${state.codeLabel} ${state.code}", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = state.price,
            onValueChange = actions.onPrice,
            label = { Text("現在価格（空欄可）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val projects = listOf(InboxOption) + state.projects
    Chooser(
        label = "プロジェクト",
        options = projects,
        selected = projects.firstOrNull { it.id == state.projectId } ?: InboxOption,
        text = { it.label },
        onSelect = { actions.onProject(it.id) },
        enabled = !state.busy,
    )
    val sections = listOf(NoSectionOption) + state.sections
    Chooser(
        label = "セクション",
        options = sections,
        selected = sections.firstOrNull { it.id == state.sectionId } ?: NoSectionOption,
        text = { it.name },
        onSelect = { actions.onSection(it.id) },
        enabled = !state.busy && state.sections.isNotEmpty(),
    )
    Chooser(
        label = "優先度",
        options = Priorities,
        selected = Priorities.firstOrNull { it.first == state.priority } ?: Priorities.last(),
        text = { it.second },
        onSelect = { actions.onPriority(it.first) },
        enabled = !state.busy,
    )
    OutlinedTextField(
        value = state.labels,
        onValueChange = actions.onLabels,
        label = { Text("ラベル（カンマ区切り）") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.memo,
        onValueChange = actions.onMemo,
        label = { Text("メモ（説明欄の先頭に入ります）") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.error != null) {
        Text(state.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = actions.onAdd, enabled = !state.busy) { Text("追加") }
        TextButton(onClick = actions.onClose, enabled = !state.busy) { Text("やめる") }
        if (state.busy) CircularProgressIndicator(modifier = Modifier.padding(start = 4.dp))
    }
}
