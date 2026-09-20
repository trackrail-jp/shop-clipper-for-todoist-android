package jp.trackrail.shopclipper.core

// URL pieces without java.net.URI: URI rejects characters that shops and shared
// texts do use (| { } ^ and friends) and would throw. These helpers only split
// the string; they never fetch anything.

private val SCHEME_AUTHORITY = Regex("^[A-Za-z][A-Za-z0-9+.-]*://([^/?#\\\\]*)")

/** Lower-cased host of an absolute URL, or null when there is none. */
internal fun hostOf(url: String?): String? {
    val match = SCHEME_AUTHORITY.find(url.orEmpty()) ?: return null
    val host = match.groupValues[1].substringAfterLast('@').substringBefore(':').lowercase()
    return host.ifEmpty { null }
}

/**
 * Path of an absolute URL ("/" when it has none), or null when [url] is not an
 * absolute URL (the caller then treats the text itself as a path).
 */
internal fun pathOf(url: String): String? {
    val head = SCHEME_AUTHORITY.find(url) ?: return null
    val path = url.substring(head.range.last + 1).substringBefore('#').substringBefore('?')
    return path.ifEmpty { "/" }
}

/** "scheme://authority" plus the path, without the query and the fragment; "" when not a URL. */
internal fun withoutQuery(url: String): String {
    val head = SCHEME_AUTHORITY.find(url) ?: return ""
    return url.substring(0, head.range.last + 1) + pathOf(url)
}
