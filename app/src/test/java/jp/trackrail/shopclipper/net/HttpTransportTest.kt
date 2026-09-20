package jp.trackrail.shopclipper.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpTransportTest {
    @Test
    fun aRequestNeverPrintsTheToken() {
        val request = HttpRequest(
            method = "POST",
            url = "https://api.todoist.com/api/v1/tasks",
            headers = mapOf("authorization" to "Bearer secret-token-123", "Content-Type" to "application/json"),
            body = "{}",
        )
        val text = request.toString()
        assertFalse(text, text.contains("secret-token-123"))
        assertTrue(text, text.contains("authorization=***"))
        assertTrue(text, text.contains("Content-Type=application/json"))
        assertTrue(text, text.contains("body=2 chars"))
        assertTrue(HttpRequest("GET", "https://x/").toString().contains("body=0 chars"))
        assertEquals(DEFAULT_TIMEOUT_MILLIS, HttpRequest("GET", "https://x/").timeoutMillis)
        assertFalse(HttpRequest("GET", "https://x/").followRedirects)
    }

    @Test
    fun responseHeadersAreCaseInsensitiveAndStatusDecidesSuccess() {
        val response = HttpResponse(301, headers = mapOf("location" to listOf("https://a/", "https://b/")))
        assertEquals("https://a/", response.header("Location"))
        assertNull(response.header("Content-Type"))
        assertNull(HttpResponse(301, headers = mapOf("Location" to emptyList())).header("Location"))
        assertFalse(response.isSuccessful)
        assertTrue(HttpResponse(200).isSuccessful)
        assertTrue(HttpResponse(299).isSuccessful)
        assertFalse(HttpResponse(199).isSuccessful)
        assertEquals("", HttpResponse(204).body)
    }
}
