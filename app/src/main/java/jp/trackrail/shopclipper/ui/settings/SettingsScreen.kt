package jp.trackrail.shopclipper.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import jp.trackrail.shopclipper.core.ProjectOption
import jp.trackrail.shopclipper.todoist.TodoistSection

// The settings screen (計画書 §4). The layout follows the extension's options
// page: 1. API トークン → 2. 既定の登録先 → 3. 登録内容 → 保存.

/** What the screen can do; the ViewModel supplies them. */
data class SettingsActions(
    val onToken: (String) -> Unit = {},
    val onTest: () -> Unit = {},
    val onProject: (String) -> Unit = {},
    val onSection: (String) -> Unit = {},
    val onSiteLabels: (Boolean) -> Unit = {},
    val onLabels: (String) -> Unit = {},
    val onPriority: (Int) -> Unit = {},
    val onTitleLimit: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

/** The Inbox is "no project_id in the request", so its id is empty (計画書 D10). */
private val INBOX = ProjectOption(id = "", name = "インボックス", depth = 0, label = "（インボックス・既定）")
private val NO_SECTION = TodoistSection(id = "", name = "（セクションなし）")
private val PRIORITIES = listOf(4 to "P1", 3 to "P2", 2 to "P3", 1 to "P4")

@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ショップクリップ for Todoist", style = MaterialTheme.typography.headlineSmall)

            Text("1. API トークン", style = MaterialTheme.typography.titleMedium)
            Text(
                "Todoist の「設定 → 連携機能 → 開発者」にある API トークンを貼り付けて「接続テスト」を押します。" +
                    "トークンはこの端末の中だけに、暗号化して保存されます（バックアップにも入りません）。",
                style = MaterialTheme.typography.bodySmall,
            )
            TokenField(state.token, actions.onToken)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = actions.onTest, enabled = !state.busy) { Text("接続テスト") }
                if (state.busy) Text("確認中…", style = MaterialTheme.typography.bodySmall)
            }
            NoticeText(state.testNotice)

            HorizontalDivider()
            Text("2. 既定の登録先", style = MaterialTheme.typography.titleMedium)
            Text(
                "共有したときのフォームの初期値です。接続テストを押すとプロジェクトの一覧が出ます。",
                style = MaterialTheme.typography.bodySmall,
            )
            val projects = listOf(INBOX) + state.projects
            Chooser(
                label = "プロジェクト",
                options = projects,
                selected = projects.firstOrNull { it.id == state.projectId } ?: INBOX,
                text = { it.label },
                onSelect = { actions.onProject(it.id) },
                enabled = !state.busy,
            )
            val sections = listOf(NO_SECTION) + state.sections
            Chooser(
                label = "セクション",
                options = sections,
                selected = sections.firstOrNull { it.id == state.sectionId } ?: NO_SECTION,
                text = { it.name },
                onSelect = { actions.onSection(it.id) },
                enabled = !state.busy && state.sections.isNotEmpty(),
            )

            HorizontalDivider()
            Text("3. 登録内容", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = state.siteLabels, onCheckedChange = actions.onSiteLabels)
                Text(
                    "サイト別のラベルを付ける（Amazon.co.jp = Shopping_Amazon ／ ヨドバシ.com = Shopping_ヨドバシ）",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = state.labels,
                onValueChange = actions.onLabels,
                label = { Text("追加ラベル（カンマ区切り・空欄可）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Chooser(
                label = "優先度",
                options = PRIORITIES,
                selected = PRIORITIES.firstOrNull { it.first == state.priority } ?: PRIORITIES.last(),
                text = { it.second },
                onSelect = { actions.onPriority(it.first) },
            )
            OutlinedTextField(
                value = state.titleLimit,
                onValueChange = actions.onTitleLimit,
                label = { Text("タスク名の短縮文字数（10〜200・既定 60）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Button(onClick = actions.onSave, enabled = state.loaded) { Text("保存") }
            NoticeText(state.saveNotice)
            Text(
                "本アプリは Todoist（Doist Inc.）が作成・提携・サポートするものではありません。",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TokenField(token: String, onToken: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = token,
        onValueChange = onToken,
        label = { Text("API トークン") },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "隠す" else "表示") } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> Chooser(
    label: String,
    options: List<T>,
    selected: T?,
    text: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.let(text) ?: "—", modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun NoticeText(notice: Notice?) {
    if (notice != null) {
        Text(
            notice.text,
            style = MaterialTheme.typography.bodySmall,
            color = if (notice.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
