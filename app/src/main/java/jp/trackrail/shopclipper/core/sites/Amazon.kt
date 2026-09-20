package jp.trackrail.shopclipper.core.sites

import jp.trackrail.shopclipper.core.collapse

// Amazon.co.jp (ported from the Chrome extension's sites/amazon.js, v2.0.0).
object Amazon : SiteAdapter {
    private val ASIN = Regex("[A-Z0-9]{10}")

    // Matched against the whole URL, not just the path: the canonical link puts a
    // Japanese slug before /dp/, and search-result links add /ref=... after it.
    private val ASIN_URL = Regex("/(?:dp|gp/product|gp/aw/d|exec/obidos/ASIN|o/ASIN)/([A-Z0-9]{10})(?=[/?#]|\\z)")
    private val COLON = Regex("^Amazon\\.co\\.jp\\s*[:：]\\s*", RegexOption.IGNORE_CASE)
    private val PIPE = Regex("^Amazon(?:\\.co\\.jp)?\\s*[|｜]\\s*", RegexOption.IGNORE_CASE)
    private val PIPE_SPLIT = Regex("\\s[|｜]\\s")

    // The Amazon app puts "セール: " in front of the name of a product on sale
    // (計画書 §2 の S2). Not part of the name, so it goes (D13).
    private val SALE = Regex("^セール\\s*[:：]\\s*")

    override val id = "amazon"
    override val displayName = "Amazon"
    override val hostSuffixes = listOf("amazon.co.jp")
    override val shortUrlHosts = listOf("amzn.asia")
    override val label = "Shopping_Amazon"
    override val codeName = "ASIN"
    override val codeLabel = "【ASIN】"
    override val sourceLabel = "Androidアプリ（Amazon共有）"

    override fun isValidCode(value: String?): Boolean = value != null && ASIN.matches(value)

    override fun codeFromUrl(url: String?): String? {
        val match = ASIN_URL.find(url ?: return null) ?: return null
        return match.groupValues[1]
    }

    override fun canonicalUrl(code: String): String = "https://www.amazon.co.jp/dp/$code"

    // "Amazon.co.jp: 商品名 : カテゴリ" / "Amazon | 商品名 | ブランド | カテゴリ" -> "商品名"
    override fun cleanPageTitle(title: String?): String {
        var t = collapse(title)
        if (COLON.containsMatchIn(t)) {
            t = t.replaceFirst(COLON, "")
            val parts = t.split(" : ")
            if (parts.size > 1) t = parts.dropLast(1).joinToString(" : ")
        } else if (PIPE.containsMatchIn(t)) {
            t = t.replaceFirst(PIPE, "").split(PIPE_SPLIT)[0]
        }
        return collapse(t)
    }

    override fun cleanSharedName(text: String?): String {
        val name = cleanPageTitle(text)
        return collapse(name.replaceFirst(SALE, "")).ifEmpty { name }
    }
}
