package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.todoist.TaskPayload
import java.math.BigInteger

// TaskFormat.kt - the task's title, description and POST /tasks body, ported from
// the Chrome extension's lib.js (v2.0.0).
//   * title   : "[short name](<正規化した商品URL>)", at most 500 characters
//   * details : （【メモ】）【商品名】（【現在価格】）【ASIN】or【商品コード】【取込元】
// Todoist caps a task name at 500 characters; the official extension inserts the
// page title twice plus the raw URL, which is why long Amazon pages fail.
// ⚠ The extension keeps the same format in JavaScript. Change both.

/** 【取込元】 when no shop adapter claims the page. */
const val DEFAULT_SOURCE_LABEL = "Androidアプリ（共有）"
const val NO_PROJECT_LABEL = "（プロジェクト未選択）"

data class TaskContent(val content: String, val linkDropped: Boolean)

// Always returns content within TASK_CONTENT_LIMIT. If even the bare link would
// not fit, the link is dropped from the title (it is kept in the description).
fun buildContent(name: String? = null, url: String? = null): TaskContent {
    val label = safeLinkText(name).ifEmpty { "(無題)" }
    val link = safeUrl(url)
    if (link.isEmpty()) return TaskContent(truncate(label, TASK_CONTENT_LIMIT), linkDropped = false)
    val overhead = link.length + 4 // "[" "]" "(" ")"
    if (overhead + 1 > TASK_CONTENT_LIMIT) {
        return TaskContent(truncate(label, TASK_CONTENT_LIMIT), linkDropped = true)
    }
    val room = TASK_CONTENT_LIMIT - overhead
    return TaskContent("[${truncate(label, room)}]($link)", linkDropped = false)
}

// Todoist renders the description as Markdown, so every block is
//   **【label】**
//   value
// with a BLANK LINE between blocks. Two rules, both easy to undo by accident:
//   * the blank line is required - without it the next 【…】 is read as a lazy
//     continuation and swallowed into the previous block;
//   * no "- " bullet - one product means exactly one value per label.
private fun block(label: String, value: String): String = "**$label**\n$value"

// Order: (メモ) -> 商品名 -> (現在価格) -> 商品コード -> 取込元.
// Differences from the extension (計画書 §5・§6):
//   * 【現在価格】 appears only when a price was typed in the form (D6). The
//     extension read it from the page and wrote 取得できず when it could not.
//   * 【取込元】 defaults to DEFAULT_SOURCE_LABEL; the adapters carry the rest.
fun buildDescription(
    memo: String? = null,
    code: String? = null,
    codeLabel: String? = null,
    sourceLabel: String? = null,
    price: String? = null,
    fullTitle: String? = null,
    url: String? = null,
    dateIso: String? = null,
    linkDropped: Boolean = false,
): String {
    val blocks = mutableListOf<String>()
    // 【メモ】 goes first: why the product is on the list is what you want to read
    // before the product name.
    val memoText = cleanMemo(memo)
    if (memoText.isNotEmpty()) blocks += block("【メモ】", truncate(memoText, MEMO_LIMIT))
    val title = collapse(fullTitle)
    val productCode = code.orEmpty()
    val isProduct = productCode.isNotEmpty()
    if (title.isNotEmpty()) blocks += block(if (isProduct) "【商品名】" else "【ページ名】", truncate(title, FULL_TITLE_LIMIT))
    if (isProduct) {
        if (!price.isNullOrEmpty()) blocks += block("【現在価格】", "${price}（${dateIso} 登録時点）")
        blocks += block(codeLabel.orEmpty().ifEmpty { "【商品コード】" }, productCode)
    }
    if (linkDropped && !url.isNullOrEmpty()) blocks += block("【リンク】", url)
    blocks += block("【取込元】", sourceLabel.orEmpty().ifEmpty { DEFAULT_SOURCE_LABEL })
    return blocks.joinToString("\n\n")
}

private val LABEL_SEPARATOR = Regex("[,、]")

/** Labels from a list or from "a, b、c" text: trimmed, "@" dropped, de-duplicated. */
fun parseLabels(value: Any?): List<String> {
    val list = if (value is Collection<*>) value.map { it?.toString() } else value?.toString().orEmpty().split(LABEL_SEPARATOR)
    return list.map { collapse(it).removePrefix("@") }.filter { it.isNotEmpty() }.distinct()
}

private val LEADING_INT = Regex("^[+-]?\\d+")
private val INT_MIN = BigInteger.valueOf(Int.MIN_VALUE.toLong())
private val INT_MAX = BigInteger.valueOf(Int.MAX_VALUE.toLong())

/** Number.parseInt(value, 10): the leading integer of the value's text, or null. */
internal fun jsParseInt(value: Any?): Int? {
    if (value == null) return null
    val match = LEADING_INT.find(jsTrim(value.toString())) ?: return null
    return BigInteger(match.value).max(INT_MIN).min(INT_MAX).toInt()
}

fun clampInt(value: Any?, min: Int, max: Int, fallback: Int): Int = jsParseInt(value)?.coerceIn(min, max) ?: fallback

fun buildTaskPayload(
    content: String,
    description: String? = null,
    projectId: String? = null,
    sectionId: String? = null,
    labels: Any? = null,
    priority: Any? = null,
): TaskPayload = TaskPayload(
    content = content,
    priority = clampInt(priority, 1, 4, DEFAULT_PRIORITY),
    description = description?.ifEmpty { null },
    projectId = projectId?.ifEmpty { null },
    sectionId = sectionId?.ifEmpty { null },
    labels = parseLabels(labels).ifEmpty { null },
)

fun taskWebUrl(id: String): String = "https://app.todoist.com/app/task/$id"

fun targetLabelOf(projectName: String?, sectionName: String?): String {
    val p = collapse(projectName).ifEmpty { NO_PROJECT_LABEL }
    return if (sectionName.isNullOrEmpty()) p else "$p / ${collapse(sectionName)}"
}

private val MARKDOWN_LINK = Regex("\\[([^\\]]*)]\\([^)]*\\)")

// Markdown "[label](url)" -> "label" (for messages and warnings).
fun stripMarkdownLinks(text: String?): String = collapse(text.orEmpty().replace(MARKDOWN_LINK, "\$1"))
