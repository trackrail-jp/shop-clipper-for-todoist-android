package jp.trackrail.shopclipper.core.sites

// Everything shop-specific lives in an adapter (ported from the Chrome extension's
// sites/*.js, v2.0.0). Adding a shop = one more object next to Amazon and one line
// in Sites.ALL. The DOM parts of the extension (extractSpec, urlPatterns) have no
// counterpart on Android: the share sheet is the entry point.
interface SiteAdapter {
    val id: String
    val displayName: String
    val hostSuffixes: List<String>

    /** Hosts of the shop's own link shortener, matched exactly (the Amazon app shares https://amzn.asia/d/…). */
    val shortUrlHosts: List<String> get() = emptyList()
    val label: String
    val codeName: String
    val codeLabel: String

    /** 【取込元】 for a task that came from this shop. */
    val sourceLabel: String

    fun isValidCode(value: String?): Boolean

    fun codeFromUrl(url: String?): String?

    fun canonicalUrl(code: String): String

    /** The shop's page title -> the product name. */
    fun cleanPageTitle(title: String?): String

    /** A name taken from a shared text -> the product name. The page-title rules by default. */
    fun cleanSharedName(text: String?): String = cleanPageTitle(text)
}
