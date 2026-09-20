package jp.trackrail.shopclipper.share

import jp.trackrail.shopclipper.FakeTransport
import jp.trackrail.shopclipper.SharedSamples
import jp.trackrail.shopclipper.net.HttpRequest
import jp.trackrail.shopclipper.net.HttpResponse
import jp.trackrail.shopclipper.net.HttpTransport
import jp.trackrail.shopclipper.share.ShortUrlResolver.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ShortUrlResolverTest {
    private fun redirect(location: String?, status: Int = 301): (HttpRequest) -> HttpResponse = {
        HttpResponse(status, "", if (location == null) emptyMap() else mapOf("location" to listOf(location)))
    }

    private fun on(url: String): (HttpRequest) -> Boolean = { it.url == url }

    @Test
    fun everyMeasuredShortUrlResolvesWithOneGetAndOnlyTheLocationIsRead() = runTest {
        // 計画書 §2: five short URLs, each answered 301 with Location on www.amazon.co.jp/dp/<ASIN>.
        for ((short, location) in SharedSamples.LOCATIONS) {
            val transport = FakeTransport(on(short) to redirect(location))
            assertEquals(Result.Resolved(location), ShortUrlResolver(transport).resolve(short))
            assertEquals(1, transport.calls.size) // the product page itself is never requested (D6)
            val call = transport.calls[0]
            assertEquals("GET", call.method)
            assertEquals(false, call.followRedirects)
            assertEquals(ShortUrlResolver.TIMEOUT_MILLIS, call.timeoutMillis)
            assertEquals(emptyMap<String, String>(), call.headers) // no User-Agent needed, no token ever
        }
    }

    @Test
    fun hopsOnTheShortHostAreFollowedUpToTheLimit() = runTest {
        val a = "https://amzn.asia/d/00nnga31"
        val b = "https://amzn.asia/d/next0001"
        val transport = FakeTransport(on(a) to redirect("/d/next0001", 302), on(b) to redirect(SharedSamples.LOCATIONS.getValue(a)))
        assertEquals(Result.Resolved(SharedSamples.LOCATIONS.getValue(a)), ShortUrlResolver(transport).resolve(a))
        assertEquals(listOf(a, b), transport.calls.map { it.url })
        // A short URL that only ever points at itself gives up after maxHops requests.
        val loop = FakeTransport(on(a) to redirect(a))
        assertEquals(Result.Failed("転送が多すぎます"), ShortUrlResolver(loop, maxHops = 3).resolve(a))
        assertEquals(3, loop.calls.size)
    }

    @Test
    fun aRedirectAwayFromTheShopStopsTheWalk() = runTest {
        val a = "https://amzn.asia/d/00nnga31"
        assertEquals(
            Result.Failed("Amazon 以外への転送を止めました"),
            ShortUrlResolver(FakeTransport(on(a) to redirect("https://evil.example.com/dp/B0CLD7LW4T"))).resolve(a),
        )
        assertEquals(
            Result.Failed("Amazon 以外への転送を止めました"),
            ShortUrlResolver(FakeTransport(on(a) to redirect("https://www.amazon.co.jp@evil.example.com/dp/B0CLD7LW4T"))).resolve(a),
        )
        // Another short host of the same shop is only followed on https, without user info, on 443.
        for (hop in listOf("http://amzn.asia/d/x", "https://user@amzn.asia/d/x", "https://amzn.asia:8443/d/x", "https://amzn.asia/d/x\\y")) {
            val t = FakeTransport(on(a) to redirect(hop))
            assertEquals(hop, Result.Failed("この短縮 URL には接続しません"), ShortUrlResolver(t).resolve(a))
            assertEquals(hop, 1, t.calls.size)
        }
        assertEquals(
            Result.Resolved("https://www.amazon.co.jp/dp/B0CLD7LW4T"),
            ShortUrlResolver(FakeTransport(on(a) to redirect("https://amzn.asia:443/x"), on("https://amzn.asia:443/x") to redirect("https://www.amazon.co.jp/dp/B0CLD7LW4T"))).resolve(a),
        )
    }

    @Test
    fun anythingElseFailsAndTheCallerKeepsTheShortUrl() = runTest {
        val a = "https://amzn.asia/d/00nnga31"
        suspend fun resolveWith(reply: (HttpRequest) -> HttpResponse) = ShortUrlResolver(FakeTransport(on(a) to reply)).resolve(a)
        assertEquals(Result.Failed("転送されませんでした（HTTP 200）"), resolveWith { HttpResponse(200, "<html>") })
        assertEquals(Result.Failed("転送されませんでした（HTTP 404）"), resolveWith { HttpResponse(404) })
        assertEquals(Result.Failed("転送先がありません（HTTP 301）"), resolveWith(redirect(null)))
        assertEquals(Result.Failed("転送先がありません（HTTP 301）"), resolveWith(redirect("  ")))
        assertEquals(Result.Failed("転送先を読めません"), resolveWith(redirect("/d/a b")))
        val offline = object : HttpTransport {
            override suspend fun execute(request: HttpRequest): HttpResponse = throw IOException("Unable to resolve host \"amzn.asia\"")
        }
        assertEquals(Result.Failed("短縮 URL を開けませんでした（Unable to resolve host \"amzn.asia\"）"), ShortUrlResolver(offline).resolve(a))
        // Not a short URL at all, or one this app will not open: no request is made.
        val none = FakeTransport()
        assertEquals(Result.Failed("短縮 URL ではありません"), ShortUrlResolver(none).resolve("https://www.amazon.co.jp/dp/B0CLD7LW4T"))
        assertEquals(Result.Failed("この短縮 URL には接続しません"), ShortUrlResolver(none).resolve("http://amzn.asia/d/00nnga31"))
        assertEquals(Result.Failed("この短縮 URL には接続しません"), ShortUrlResolver(none).resolve("https://amzn.asia/d/a{b}"))
        assertEquals(Result.Failed("この短縮 URL には接続しません"), ShortUrlResolver(none).resolve("https://amzn.asia:abc/d/x")) // URI finds no host
        assertEquals(0, none.calls.size)
    }
}
