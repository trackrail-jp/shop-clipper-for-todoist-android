package jp.trackrail.shopclipper.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import jp.trackrail.shopclipper.ui.common.Chooser
import jp.trackrail.shopclipper.ui.common.InboxOption
import jp.trackrail.shopclipper.ui.common.NoSectionOption
import jp.trackrail.shopclipper.ui.common.Priorities

// The settings screen (計画書 §4). The layout follows the extension's options
// page: 1. API トークン → 2. 既定の登録先 → 3. 登録内容 → 4. 保存.
// I8（計画書 D17）: 冒頭に「はじめに」のカードを置き、節ごとに説明を立て、
// 節の間を空け、保存できたらダイアログで知らせて買い物へ送り出す。

// 保存のあとに開く店（D17 ⑤）。パッケージ名は書かず https の URL を渡すだけにする:
// App Links が効く端末ではアプリが、入っていない端末ではブラウザが開く（<queries> も要らない）。
private const val AMAZON_URL = "https://www.amazon.co.jp/"
private const val YODOBASHI_URL = "https://www.yodobashi.com/"

/** 節と節の間（D17 ③。以前は全体が一律 12dp だった）。 */
private val SectionGap = 28.dp

/** 節の中の行の間。 */
private val RowGap = 12.dp

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
    // D17 ④⑤。この画面から Android の API は呼ばない（プレビューとテストのしやすさを保つ）:
    // URL を渡すだけにして、startActivity は MainActivity 側で行う。
    val onOpenUrl: (String) -> Unit = {},
    val onDismissSaved: () -> Unit = {},
)

@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            Text("ショップクリップ for Todoist", style = MaterialTheme.typography.headlineSmall)
            IntroCard()
            TokenSection(state, actions)
            DestinationSection(state, actions)
            ContentSection(state, actions)
            SaveSection(state, actions)
        }
    }
    if (state.justSaved) SavedDialog(state.targetLabel, actions)
}

/** 冒頭の「はじめに」（D17 ①）。折りたたまず、いつでも読める形で置く。 */
@Composable
private fun IntroCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "はじめに — この画面ですること（設定は 1 回だけ）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                buildAnnotatedString {
                    append("商品ページを")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("共有") }
                    append("すると、Todoist にタスクを作れるようになります。使い始める前に、次の 3 つを済ませてください。")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("① Todoist の API トークンを貼る", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("② タスクの入れ先（プロジェクト・セクション）を選ぶ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("③ 一番下までスクロールして「保存」を押す", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                buildAnnotatedString {
                    append("⚠ ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("「保存」を押すまで設定は反映されません。") }
                    append("設定はあとからこの画面でいつでも変えられます。")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** 節の見出し（D17 ②③）。区切り線と見出しを付け、中身は 12dp で並べる。 */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        HorizontalDivider()
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun TokenSection(state: SettingsUiState, actions: SettingsActions) {
    Section("1. API トークン") {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("まず、Todoist の API トークンを貼ります。") }
                append("Todoist の「設定 → 連携機能 → 開発者」でトークンをコピーし、下の欄に貼り付けて「接続テスト」を押してください。")
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        TokenField(state.token, actions.onToken)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = actions.onTest, enabled = !state.busy) { Text("接続テスト") }
            if (state.busy) Text("確認中…", style = MaterialTheme.typography.bodySmall)
        }
        NoticeText(state.testNotice)
        Text(
            "トークンはこの端末の中だけに暗号化して保存されます（バックアップにも入りません）。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DestinationSection(state: SettingsUiState, actions: SettingsActions) {
    Section("2. 既定の登録先") {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("共有したときに、タスクを入れる先です。") }
                append("共有のたびに変えられるので、いちばんよく使う先を選んでおきます。")
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            buildAnnotatedString {
                append("⚠ ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("一覧は、上の「接続テスト」が成功してから出ます。") }
                append("まだ選べないときは、先に接続テストを押してください。")
            },
            style = MaterialTheme.typography.bodySmall,
        )
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
        Text(
            "セクションはプロジェクトを選んだあとに読み込まれます。セクションのないプロジェクトでは選べません。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ContentSection(state: SettingsUiState, actions: SettingsActions) {
    Section("3. 登録内容（任意）") {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("タスクに付けるラベル・優先度と、タスク名の長さです。") }
                append("そのままでも使えます。")
            },
            style = MaterialTheme.typography.bodyMedium,
        )
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
            options = Priorities,
            selected = Priorities.firstOrNull { it.first == state.priority } ?: Priorities.last(),
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
    }
}

@Composable
private fun SaveSection(state: SettingsUiState, actions: SettingsActions) {
    Section("4. 保存") {
        Text(
            buildAnnotatedString {
                append("⚠ ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("ここで「保存」を押すまで、上の変更は反映されません。") }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = actions.onSave, enabled = state.loaded) { Text("保存") }
        NoticeText(state.saveNotice)
        Text(
            "本アプリは Todoist（Doist Inc.）が作成・提携・サポートするものではありません。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** 保存できたことをはっきり知らせ、そのまま買い物へ送り出す（D17 ④⑤）。 */
@Composable
private fun SavedDialog(targetLabel: String, actions: SettingsActions) {
    AlertDialog(
        onDismissRequest = actions.onDismissSaved,
        title = { Text("保存しました") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
                Text("既定の登録先: $targetLabel", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "これで準備は完了です。商品ページの共有メニューから「ショップクリップ」を選ぶと、Todoist にタスクを作れます。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = {
                        actions.onDismissSaved()
                        actions.onOpenUrl(AMAZON_URL)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Amazon.co.jp を開く") }
                OutlinedButton(
                    onClick = {
                        actions.onDismissSaved()
                        actions.onOpenUrl(YODOBASHI_URL)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("ヨドバシ.com を開く") }
            }
        },
        confirmButton = { TextButton(onClick = actions.onDismissSaved) { Text("閉じる") } },
    )
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
private fun NoticeText(notice: Notice?) {
    if (notice != null) {
        Text(
            notice.text,
            style = MaterialTheme.typography.bodySmall,
            color = if (notice.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
