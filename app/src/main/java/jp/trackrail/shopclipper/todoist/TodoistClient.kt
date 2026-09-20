package jp.trackrail.shopclipper.todoist

import jp.trackrail.shopclipper.net.HttpRequest
import jp.trackrail.shopclipper.net.HttpTransport
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.net.URLEncoder

// Minimal Todoist API v1 client, ported from the Chrome extension's todoist.js
// (v2.0.0). The transport is injected, so everything here runs in JVM tests.
// ⛔ The token goes into the Authorization header only - never into a URL, a log
// or an error message.

const val API_BASE = "https://api.todoist.com/api/v1"
private const val PAGE_LIMIT = 200
private const val MAX_PAGES = 20
private const val SEARCH_LIMIT = 50

/** What duplicate checking needs (core.findDuplicate); a fake in tests. */
interface TaskSearcher {
    suspend fun searchTasks(text: String): List<TodoistTask>
}

class TodoistClient(private val token: String, private val transport: HttpTransport) : TaskSearcher {
    init {
        if (token.isEmpty()) throw TodoistError("APIトークンが未設定です。設定画面で登録してください。")
    }

    suspend fun listProjects(): List<TodoistProject> = decodeAll(TodoistProject.serializer(), listAll("projects"))

    suspend fun listSections(projectId: String): List<TodoistSection> =
        decodeAll(TodoistSection.serializer(), listAll("sections", linkedMapOf("project_id" to projectId)))

    /** The created task, or null when Todoist answered 204 No Content. */
    suspend fun createTask(payload: TaskPayload): TodoistTask? {
        val body = request("tasks", method = "POST", body = TodoistJson.encodeToString(TaskPayload.serializer(), payload))
        return if (body is JsonNull) null else decode(TodoistTask.serializer(), body)
    }

    // Todoist "search:" matches the code in the title URL and in 【ASIN】 lines.
    override suspend fun searchTasks(text: String): List<TodoistTask> {
        val body = request("tasks/filter", params = linkedMapOf("query" to "search: $text", "limit" to SEARCH_LIMIT))
        return decodeAll(TodoistTask.serializer(), resultsOf(body))
    }

    private suspend fun request(
        path: String,
        method: String = "GET",
        params: Map<String, Any?> = emptyMap(),
        body: String? = null,
    ): JsonElement {
        val query = params.filterValues { it != null && it.toString().isNotEmpty() }
            .entries.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v.toString())}" }
        val url = "$API_BASE/$path" + if (query.isEmpty()) "" else "?$query"
        val headers = linkedMapOf("Authorization" to "Bearer $token")
        if (body != null) headers["Content-Type"] = "application/json"
        val response = try {
            transport.execute(HttpRequest(method = method, url = url, headers = headers, body = body))
        } catch (e: IOException) {
            throw TodoistError("Todoist に接続できませんでした（${e.message}）")
        }
        if (!response.isSuccessful) throw TodoistError(messageFor(response.status, response.body.orEmpty()), response.status)
        if (response.status == 204) return JsonNull
        return try {
            TodoistJson.parseToJsonElement(response.body.orEmpty())
        } catch (e: SerializationException) {
            throw TodoistError("Todoist の応答を読めませんでした（HTTP ${response.status}）", response.status)
        }
    }

    // v1 list endpoints return { results, next_cursor }; follow the cursor.
    private suspend fun listAll(path: String, params: Map<String, Any?> = emptyMap()): List<JsonElement> {
        val out = mutableListOf<JsonElement>()
        var cursor: String? = null
        repeat(MAX_PAGES) {
            val body = request(path, params = params + linkedMapOf("limit" to PAGE_LIMIT, "cursor" to cursor))
            if (body is JsonArray) return out + body
            out += resultsOf(body)
            cursor = ((body as? JsonObject)?.get("next_cursor") as? JsonPrimitive)?.contentOrNull
            if (cursor.isNullOrEmpty()) return out
        }
        return out
    }

    private fun resultsOf(body: JsonElement): List<JsonElement> = when (body) {
        is JsonArray -> body
        is JsonObject -> body["results"] as? JsonArray ?: emptyList()
        else -> emptyList()
    }

    private fun <T> decode(serializer: KSerializer<T>, element: JsonElement): T = try {
        TodoistJson.decodeFromJsonElement(serializer, element)
    } catch (e: SerializationException) {
        throw TodoistError("Todoist の応答を読めませんでした（${serializer.descriptor.serialName.substringAfterLast('.')}）")
    }

    private fun <T> decodeAll(serializer: KSerializer<T>, elements: List<JsonElement>): List<T> = elements.map { decode(serializer, it) }

    // URLSearchParams in the extension: application/x-www-form-urlencoded, space -> "+".
    // (The Charset overload needs API 33; minSdk is 24.)
    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")
}
