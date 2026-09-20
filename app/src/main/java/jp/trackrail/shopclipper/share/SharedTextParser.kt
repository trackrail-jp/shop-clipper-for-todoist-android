package jp.trackrail.shopclipper.share

import jp.trackrail.shopclipper.core.SharedText
import jp.trackrail.shopclipper.core.collapse
import jp.trackrail.shopclipper.core.hostOf
import jp.trackrail.shopclipper.core.sites.Sites

// EXTRA_TEXT / EXTRA_SUBJECT of an ACTION_SEND text/plain intent -> URL + name.
// Rules measured on a Pixel 9 Pro on 2026-09-20 (計画書 §2, the samples are in
// SharedTextParserTest):
//   * Amazon app : TEXT = "商品名 https://amzn.asia/d/…", SUBJECT = boilerplate
//                  (「Amazonでご覧ください」), a product on sale starts with "セール: "
//   * ヨドバシ app: TEXT = "商品名 通販【全品無料配達】 https://www.yodobashi.com/product/…/",
//                  no SUBJECT, referrer null
//   * Chrome     : TEXT = the URL only, SUBJECT = the page title
// So: take the first http(s) URL in TEXT and use the rest as the name; when the
// rest is empty, use SUBJECT - except for a shop's short URL, whose SUBJECT is
// the app's boilerplate. The shop is decided by the URL's host (the referrer can
// be null). The received text is not trusted: it is only split, never fetched.
object SharedTextParser {
    // Printable ASCII after the scheme: a URL in a shared text is percent-encoded,
    // and a Japanese character or a space right after it ends it.
    private val URL = Regex("https?://[!-~]+", RegexOption.IGNORE_CASE)

    /** null when [text] holds no http(s) URL. */
    fun parse(text: String?, subject: String?): SharedText? {
        val body = text.orEmpty()
        val match = URL.find(body) ?: return null
        val url = trimTrailingPunctuation(match.value)
        if (hostOf(url) == null) return null
        val rest = collapse(body.removeRange(match.range))
        val shortener = Sites.siteForShortUrl(url)
        val site = Sites.siteForUrl(url) ?: shortener
        val raw = when {
            rest.isNotEmpty() -> rest
            shortener != null -> "" // the shop app's SUBJECT is boilerplate, not the name
            else -> collapse(subject)
        }
        val name = if (site == null) raw else site.cleanSharedName(raw)
        return SharedText(url = url, name = name, site = site, isShortUrl = shortener != null)
    }

    // "…を見て https://example.com/a." -> the sentence's "." is not part of the URL.
    // A ")" is kept when it closes a "(" inside the URL (Wikipedia-style links).
    private fun trimTrailingPunctuation(raw: String): String {
        var url = raw
        while (true) {
            val last = url.last()
            val unbalanced = last == ')' && url.count { it == ')' } > url.count { it == '(' }
            if (last !in ".,;:!?" && !unbalanced) return url
            url = url.dropLast(1)
        }
    }
}
