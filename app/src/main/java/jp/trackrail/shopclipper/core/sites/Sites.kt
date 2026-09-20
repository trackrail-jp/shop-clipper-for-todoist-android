package jp.trackrail.shopclipper.core.sites

import jp.trackrail.shopclipper.core.hostOf

// The site registry (ported from the Chrome extension's sites/index.js, v2.0.0).
object Sites {
    val ALL: List<SiteAdapter> = listOf(Amazon, Yodobashi)

    // The adapter for a URL, or null for a page no adapter claims (it can still be
    // added as "ページ名 + URL"). Hosts and their subdomains only, never look-alikes.
    fun siteForUrl(url: String?): SiteAdapter? {
        val host = hostOf(url) ?: return null
        return ALL.firstOrNull { site -> site.hostSuffixes.any { host == it || host.endsWith(".$it") } }
    }

    /** The adapter whose link shortener made [url] (exact host), or null. */
    fun siteForShortUrl(url: String?): SiteAdapter? {
        val host = hostOf(url) ?: return null
        return ALL.firstOrNull { host in it.shortUrlHosts }
    }
}
