package jp.trackrail.shopclipper.share

import jp.trackrail.shopclipper.core.jsTrim
import jp.trackrail.shopclipper.core.sites.Sites
import jp.trackrail.shopclipper.net.HttpRequest
import jp.trackrail.shopclipper.net.HttpTransport
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

// A shop's short URL (https://amzn.asia/d/…) -> the product URL it redirects to.
// Measured 2026-09-20 (計画書 §2): one GET to amzn.asia answers 301 with
// Location: https://www.amazon.co.jp/dp/<ASIN>?ref=…, no User-Agent needed.
//   * Only the Location header is read. The product page itself is never
//     requested (D6): the resolved URL is only used to find the ASIN.
//   * Redirects are followed by hand, and only while they stay on the shop's own
//     short host (at most [maxHops] requests). A redirect anywhere else stops the
//     walk unless it lands on the same shop's site.
// Whatever fails, the caller keeps the short URL and warns (D8).
class ShortUrlResolver(
    private val transport: HttpTransport,
    private val maxHops: Int = MAX_HOPS,
) {
    sealed interface Result {
        data class Resolved(val url: String) : Result

        data class Failed(val reason: String) : Result
    }

    suspend fun resolve(shortUrl: String): Result {
        val shop = Sites.siteForShortUrl(shortUrl) ?: return Result.Failed("短縮 URL ではありません")
        var current = shortUrl
        repeat(maxHops) {
            if (!isFollowable(current, shop.shortUrlHosts)) return Result.Failed("この短縮 URL には接続しません")
            val response = try {
                transport.execute(HttpRequest(method = "GET", url = current, timeoutMillis = TIMEOUT_MILLIS))
            } catch (e: IOException) {
                return Result.Failed("短縮 URL を開けませんでした（${e.message}）")
            }
            if (response.status !in 300..399) return Result.Failed("転送されませんでした（HTTP ${response.status}）")
            val location = jsTrim(response.header("Location").orEmpty())
            if (location.isEmpty()) return Result.Failed("転送先がありません（HTTP ${response.status}）")
            val next = absolute(current, location) ?: return Result.Failed("転送先を読めません")
            if (Sites.siteForShortUrl(next) == null) {
                return if (Sites.siteForUrl(next) === shop) Result.Resolved(next) else Result.Failed("${shop.displayName} 以外への転送を止めました")
            }
            current = next
        }
        return Result.Failed("転送が多すぎます")
    }

    private fun absolute(base: String, location: String): String? {
        if (ABSOLUTE.containsMatchIn(location)) return location
        return try {
            URI(base).resolve(location).toString()
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // Strict, because this decides where the app connects: https only, the shop's
    // exact short host, no user info, default port, and a URL java.net.URI accepts
    // (it rejects "\" and other characters that parsers read differently).
    private fun isFollowable(url: String, hosts: List<String>): Boolean {
        val uri = try {
            URI(url)
        } catch (e: URISyntaxException) {
            return false
        }
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port == 443) &&
            uri.host?.lowercase() in hosts
    }

    companion object {
        const val MAX_HOPS = 5
        const val TIMEOUT_MILLIS = 10_000
        private val ABSOLUTE = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")
    }
}
