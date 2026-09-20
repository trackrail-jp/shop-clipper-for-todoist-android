package jp.trackrail.shopclipper.core.sites

import jp.trackrail.shopclipper.core.collapse
import jp.trackrail.shopclipper.core.pathOf

// ヨドバシ.com (ported from the Chrome extension's sites/yodobashi.js, v2.0.0).
object Yodobashi : SiteAdapter {
    private val CODE = Regex("\\d{18}")

    // Only the product page itself. /community/product/<code>/… (レビュー) and
    // /ec/product/stock/<code>/ carry the same digits, so match from the path's start.
    private val PRODUCT_PATH = Regex("^/product/(\\d{18})(?:/|\\z)")
    private val SITE_PREFIX = Regex("^ヨドバシ\\.com\\s*[-‐―ー－]\\s*", RegexOption.IGNORE_CASE)
    private val SHOP_SUFFIX = Regex("\\s*通販【[^】]*】\\s*\\z")

    override val id = "yodobashi"
    override val displayName = "ヨドバシ.com"
    override val hostSuffixes = listOf("yodobashi.com")
    override val label = "Shopping_ヨドバシ"
    override val codeName = "商品コード"
    override val codeLabel = "【商品コード】"
    override val sourceLabel = "Androidアプリ（ヨドバシ.com共有）"

    override fun isValidCode(value: String?): Boolean = value != null && CODE.matches(value)

    override fun codeFromUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        val path = pathOf(url) ?: url // already a path, or not a URL at all
        val match = PRODUCT_PATH.find(path) ?: return null
        return match.groupValues[1]
    }

    override fun canonicalUrl(code: String): String = "https://www.yodobashi.com/product/$code/"

    // "ヨドバシ.com - ブランド 商品名 通販【全品無料配達】" -> "ブランド 商品名".
    // The ヨドバシ app shares "商品名 通販【全品無料配達】 URL" (計画書 §2 の S4), so the
    // same rule cleans the shared name (cleanSharedName's default).
    override fun cleanPageTitle(title: String?): String =
        collapse(collapse(title).replaceFirst(SITE_PREFIX, "").replaceFirst(SHOP_SUFFIX, ""))
}
