package jp.trackrail.shopclipper.net

import java.io.IOException

// The one door to the network. The app's implementation (HttpURLConnection) comes
// with the first screen that talks to Todoist (I4); tests pass a fake.

const val DEFAULT_TIMEOUT_MILLIS = 15_000

data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    /** Redirects are never followed unless asked for (ShortUrlResolver reads Location itself). */
    val followRedirects: Boolean = false,
    val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) {
    // ⛔ Never print the API token: the Authorization header is masked.
    override fun toString(): String {
        val shown = headers.mapValues { (name, value) -> if (name.equals("Authorization", ignoreCase = true)) "***" else value }
        return "HttpRequest(method=$method, url=$url, headers=$shown, body=${body?.length ?: 0} chars, " +
            "followRedirects=$followRedirects, timeoutMillis=$timeoutMillis)"
    }
}

data class HttpResponse(
    val status: Int,
    /** The response body, or null when it could not be read. */
    val body: String? = "",
    val headers: Map<String, List<String>> = emptyMap(),
) {
    val isSuccessful: Boolean get() = status in 200..299

    /** First value of a header, case-insensitively. */
    fun header(name: String): String? {
        val entry = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) } ?: return null
        return entry.value.firstOrNull()
    }
}

interface HttpTransport {
    /** Sends [request] and returns whatever status came back. Throws [IOException] when there is no response at all. */
    @Throws(IOException::class)
    suspend fun execute(request: HttpRequest): HttpResponse
}
