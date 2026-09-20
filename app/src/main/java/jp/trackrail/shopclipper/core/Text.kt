package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.core.sites.SiteAdapter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

// Text.kt - text helpers ported from the Chrome extension's lib.js
// (shop-clipper-for-todoist v2.0.0). Pure Kotlin: no Android, no network.
// ⚠ The extension keeps the same rules in JavaScript. Change both (README「二重管理」).

const val TASK_CONTENT_LIMIT = 500
const val FULL_TITLE_LIMIT = 1000

// The form's free-text memo. Todoist allows 16,383 characters of description;
// this is a sanity cap so one paste cannot bury the standard blocks.
const val MEMO_LIMIT = 1000

// JavaScript's \s (and String.prototype.trim): ASCII whitespace plus the Unicode
// spaces, notably U+3000 (全角スペース) and U+00A0. Java's \s is ASCII only.
private const val SPACE = "[\\t\\n\\x0B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]"
private val ONE_SPACE = Regex(SPACE)
private val SPACE_RUN = Regex("$SPACE+")
private val EDGE_SPACE = Regex("^$SPACE+|$SPACE+\\z")
private val LINE_END_SPACE = Regex("$SPACE+\\z")
private val LINE_BREAK = Regex("\r\n?")
private val BLANK_LINES = Regex("\n{3,}")
private val PROMO = Regex("^(?:【[^】]*】|\\[[^\\]]*]|［[^］]*］)$SPACE*")
private val BRACKETS = Regex("[\\[\\]]")
private val PRICE_PLACEHOLDERS = setOf("N/A", "NA", "-", "—")

/** String.prototype.trim with JavaScript's idea of whitespace. */
fun jsTrim(text: String): String = text.replace(EDGE_SPACE, "")

fun collapse(text: String?): String = jsTrim(text.orEmpty().replace(SPACE_RUN, " "))

// The memo keeps the line breaks the user typed (collapse would flatten them),
// but CRLF, trailing spaces and runs of blank lines are normalised: a blank line
// ends a Markdown block, so two of them in a row would tear the description apart.
fun cleanMemo(text: String?): String = jsTrim(
    text.orEmpty()
        .replace(LINE_BREAK, "\n")
        .split('\n')
        .joinToString("\n") { it.replace(LINE_END_SPACE, "") }
        .replace(BLANK_LINES, "\n\n"),
)

// The shared URL first, then the URL a short link resolved to (Android has no
// DOM field). The first value the site accepts wins. Without a site, the page is
// not a product page this app understands.
fun resolveCode(site: SiteAdapter?, vararg urls: String?): String? {
    if (site == null) return null
    return urls.map { site.codeFromUrl(it) }.firstOrNull { site.isValidCode(it) }
}

// Shops dress titles up ("Amazon.co.jp: … : カテゴリ" / "ヨドバシ.com - … 通販【…】");
// each adapter undoes its own. Pages no adapter claims keep their title as is.
fun cleanPageTitle(site: SiteAdapter?, title: String?): String = if (site == null) collapse(title) else site.cleanPageTitle(title)

// Drop leading promo blocks such as "【2026年最新版】" or "[by Amazon]".
fun stripPromo(name: String?): String {
    val original = collapse(name)
    var t = original
    while (true) {
        val m = PROMO.find(t) ?: break
        t = t.substring(m.range.last + 1)
    }
    return t.ifEmpty { original }
}

// List-friendly title: cut at a word boundary when one is close to the limit.
fun shorten(name: String?, limit: Int = DEFAULT_TITLE_LIMIT): String {
    val t = collapse(name)
    if (t.length <= limit) return t
    val cut = t.substring(0, limit)
    val space = cut.lastIndexOf(' ')
    // Same double arithmetic as the extension's Math.floor(limit * 0.6).
    val head = if (space >= floor(limit * 0.6).toInt()) cut.substring(0, space) else cut
    return "${head.trimEnd()}…"
}

// "[" / "]" inside the label would close the Markdown link early.
fun safeLinkText(text: String?): String = collapse(text.orEmpty().replace(BRACKETS, " "))

// Keep a Markdown link intact: parentheses and spaces break "[x](url)".
fun safeUrl(url: String?): String =
    jsTrim(url.orEmpty()).replace("(", "%28").replace(")", "%29").replace(ONE_SPACE, "%20")

// Drops the query and the fragment. The extension used the WHATWG URL parser;
// this keeps the text as it is and only cuts it (an authority with no path gets
// "/", as the parser did). Not a URL -> "".
fun stripQuery(url: String?): String = withoutQuery(jsTrim(url.orEmpty()))

fun cleanPrice(price: String?): String? {
    val p = collapse(price)
    return if (p.isEmpty() || p in PRICE_PLACEHOLDERS) null else p
}

// java.time needs API 26 and minSdk is 24, so the date comes from Calendar.
// Locale.ROOT keeps it Gregorian even on a ja_JP_JP (和暦) device.
fun todayIso(now: Date = Date(), zone: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(zone, Locale.ROOT).apply { time = now }
    return String.format(
        Locale.ROOT,
        "%04d-%02d-%02d",
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH),
    )
}

internal fun truncate(text: String, max: Int): String =
    if (text.length > max) "${text.substring(0, max - 1)}…" else text
