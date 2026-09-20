package jp.trackrail.shopclipper

import jp.trackrail.shopclipper.net.HttpRequest
import jp.trackrail.shopclipper.net.HttpResponse
import jp.trackrail.shopclipper.net.HttpTransport

/** The extension's fakeFetch: routes are (predicate, reply) pairs tried in order; every call is recorded. */
class FakeTransport(private vararg val routes: Pair<(HttpRequest) -> Boolean, (HttpRequest) -> HttpResponse>) : HttpTransport {
    val calls = mutableListOf<HttpRequest>()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        calls += request
        val route = routes.firstOrNull { it.first(request) } ?: error("unexpected request: ${request.url}")
        return route.second(request)
    }
}

fun urlContains(part: String): (HttpRequest) -> Boolean = { it.url.contains(part) }

/** A canned reply. A null body = a body that could not be read. */
fun json(body: String?, status: Int = 200): (HttpRequest) -> HttpResponse = { HttpResponse(status, body) }
