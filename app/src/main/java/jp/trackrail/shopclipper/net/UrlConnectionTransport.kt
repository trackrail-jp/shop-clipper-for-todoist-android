package jp.trackrail.shopclipper.net

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// The app's HttpTransport: HttpURLConnection, no extra dependency.
// ⚠ Kover's C1 does not cover this class (it needs the network). It is checked on
// the device: the settings screen's 接続テスト (I4) and the share form (I5).
//   * Redirects are only followed when the request asks for it, so
//     ShortUrlResolver can read Location itself (計画書 §7).
//   * A body that cannot be read becomes null, never an empty string, so the
//     caller can tell "no body" from "empty body".
class UrlConnectionTransport(private val io: CoroutineDispatcher = Dispatchers.IO) : HttpTransport {

    override suspend fun execute(request: HttpRequest): HttpResponse = withContext(io) {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.instanceFollowRedirects = request.followRedirects
            connection.connectTimeout = request.timeoutMillis
            connection.readTimeout = request.timeoutMillis
            for ((name, value) in request.headers) connection.setRequestProperty(name, value)
            val body = request.body
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            HttpResponse(status = status, body = readBody(connection, status), headers = headersOf(connection))
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection, status: Int): String? = try {
        val stream = if (status < HttpURLConnection.HTTP_BAD_REQUEST) connection.inputStream else connection.errorStream
        stream?.use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (e: IOException) {
        null
    }

    // headerFields has a null key for the status line; drop it.
    private fun headersOf(connection: HttpURLConnection): Map<String, List<String>> =
        connection.headerFields.entries.filter { it.key != null }.associate { it.key to it.value }
}
