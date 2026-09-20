package jp.trackrail.shopclipper.todoist

import jp.trackrail.shopclipper.FakeTransport
import jp.trackrail.shopclipper.json
import jp.trackrail.shopclipper.net.HttpResponse
import jp.trackrail.shopclipper.net.HttpTransport
import jp.trackrail.shopclipper.urlContains
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

// Ported from the extension's test/todoist.test.js. The one test not ported is
// "createClient falls back to the global fetch": Kotlin has no global transport,
// the client always gets one.
class TodoistClientTest {
    private suspend fun failure(block: suspend () -> Unit): TodoistError {
        try {
            block()
        } catch (e: TodoistError) {
            return e
        }
        throw AssertionError("expected a TodoistError")
    }

    @Test
    fun theClientRequiresAToken() {
        val err = assertThrows(TodoistError::class.java) { TodoistClient("", FakeTransport()) }
        assertEquals(0, err.status)
        assertEquals("APIトークンが未設定です。設定画面で登録してください。", err.message)
    }

    @Test
    fun listProjectsFollowsNextCursorAndSendsBearerAuth() = runTest {
        val transport = FakeTransport(
            { r: jp.trackrail.shopclipper.net.HttpRequest -> r.url.contains("/projects") && r.url.contains("cursor=c2") } to json("""{"results":[{"id":"b","name":"B"}],"next_cursor":null}"""),
            urlContains("/projects") to json("""{"results":[{"id":"a","name":"A","color":"red"}],"next_cursor":"c2"}"""),
        )
        val projects = TodoistClient("tok", transport).listProjects()
        assertEquals(listOf("a", "b"), projects.map { it.id })
        assertEquals(2, transport.calls.size)
        val first = transport.calls[0]
        assertEquals("$API_BASE/projects?limit=200", first.url)
        assertEquals("$API_BASE/projects?limit=200&cursor=c2", transport.calls[1].url)
        assertEquals("Bearer tok", first.headers["Authorization"])
        assertNull(first.headers["Content-Type"])
        assertEquals("GET", first.method)
        assertEquals(false, first.followRedirects)
    }

    @Test
    fun listEndpointsAcceptAFlatArrayAndAnEmptyBody() = runTest {
        val flat = FakeTransport(urlContains("/sections") to json("""[{"id":"s1","name":"x"}]"""))
        assertEquals(listOf(TodoistSection(id = "s1", name = "x")), TodoistClient("t", flat).listSections("P1"))
        assertTrue(flat.calls[0].url.endsWith("sections?project_id=P1&limit=200"))
        val empty = FakeTransport(urlContains("/sections") to json("null"))
        assertEquals(emptyList<TodoistSection>(), TodoistClient("t", empty).listSections("P1"))
        val noResults = FakeTransport(urlContains("/sections") to json("""{"next_cursor":null}"""))
        assertEquals(emptyList<TodoistSection>(), TodoistClient("t", noResults).listSections("P1"))
        // An empty parameter is left out of the query, as URLSearchParams did in the extension.
        val all = FakeTransport(urlContains("/sections") to json("[]"))
        TodoistClient("t", all).listSections("")
        assertEquals("$API_BASE/sections?limit=200", all.calls[0].url)
        // A 200 whose body could not be read is an error, not an empty list.
        val unread = FakeTransport(urlContains("/sections") to json(null))
        assertEquals("Todoist の応答を読めませんでした（HTTP 200）", failure { TodoistClient("t", unread).listSections("P1") }.message)
    }

    @Test
    fun listAllStopsAfterThePageCapEvenIfTheCursorNeverEnds() = runTest {
        val transport = FakeTransport(urlContains("/projects") to json("""{"results":[{"id":"x"}],"next_cursor":"again"}"""))
        val projects = TodoistClient("t", transport).listProjects()
        assertEquals(20, transport.calls.size)
        assertEquals(20, projects.size)
    }

    @Test
    fun createTaskPostsJsonAndReturnsTheCreatedTask() = runTest {
        val transport = FakeTransport(urlContains("/tasks") to json("""{"id":"T1","content":"c","priority":1}"""))
        val task = TodoistClient("tok", transport).createTask(TaskPayload(content = "c", priority = 1, projectId = "P"))
        assertEquals("T1", task?.id)
        val call = transport.calls[0]
        assertEquals("POST", call.method)
        assertEquals("$API_BASE/tasks", call.url)
        assertEquals("application/json", call.headers["Content-Type"])
        assertEquals("""{"content":"c","priority":1,"project_id":"P"}""", call.body)
    }

    @Test
    fun noContentResolvesToNull() = runTest {
        val transport = FakeTransport(urlContains("/tasks") to json("", 204))
        assertNull(TodoistClient("t", transport).createTask(TaskPayload(content = "c", priority = 1)))
    }

    @Test
    fun searchTasksUsesTheFilterEndpointWithASearchQuery() = runTest {
        val transport = FakeTransport(urlContains("/tasks/filter") to json("""{"results":[{"id":"T","content":"x"}]}"""))
        val found = TodoistClient("t", transport).searchTasks("B0FPCWWBCH")
        assertEquals(listOf(TodoistTask(id = "T", content = "x")), found)
        // URLSearchParams in the extension: "search: B0FPCWWBCH" -> search%3A+B0FPCWWBCH
        assertEquals("$API_BASE/tasks/filter?query=search%3A+B0FPCWWBCH&limit=50", transport.calls[0].url)
    }

    @Test
    fun searchTasksAcceptsAFlatArrayAndAnEmptyBody() = runTest {
        val flat = FakeTransport(urlContains("/tasks/filter") to json("""[{"id":"T"}]"""))
        assertEquals(listOf(TodoistTask(id = "T")), TodoistClient("t", flat).searchTasks("A"))
        val empty = FakeTransport(urlContains("/tasks/filter") to json("null"))
        assertEquals(emptyList<TodoistTask>(), TodoistClient("t", empty).searchTasks("A"))
        val odd = FakeTransport(urlContains("/tasks/filter") to json("""{"results":"nope"}"""))
        assertEquals(emptyList<TodoistTask>(), TodoistClient("t", odd).searchTasks("A"))
    }

    @Test
    fun httpErrorsBecomeTodoistErrorWithAJapaneseMessage() = runTest {
        val transport = FakeTransport(urlContains("/tasks") to json("Invalid argument value", 400))
        val err = failure { TodoistClient("t", transport).createTask(TaskPayload(content = "", priority = 1)) }
        assertEquals(400, err.status)
        assertEquals("Todoist への送信に失敗しました（HTTP 400）（Invalid argument value）", err.message)
    }

    @Test
    fun anErrorBodyThatCannotBeReadIsTolerated() = runTest {
        val transport = FakeTransport(urlContains("/projects") to json(null, 401))
        val err = failure { TodoistClient("t", transport).listProjects() }
        assertEquals("APIトークンが無効です。設定画面でトークンを確認してください。", err.message)
        assertEquals(401, err.status)
    }

    @Test
    fun networkFailureBecomesTodoistError() = runTest {
        val down = object : HttpTransport {
            override suspend fun execute(request: jp.trackrail.shopclipper.net.HttpRequest): HttpResponse = throw IOException("Failed to connect")
        }
        val err = failure { TodoistClient("t", down).listProjects() }
        assertEquals(0, err.status)
        assertEquals("Todoist に接続できませんでした（Failed to connect）", err.message)
    }

    @Test
    fun unreadableSuccessBodiesBecomeTodoistError() = runTest {
        val broken = FakeTransport(urlContains("/tasks") to json("{"))
        assertEquals(
            "Todoist の応答を読めませんでした（HTTP 200）",
            failure { TodoistClient("t", broken).createTask(TaskPayload(content = "c", priority = 1)) }.message,
        )
        // parseToJsonElement reads a bare word as a primitive; decoding it then fails.
        val html = FakeTransport(urlContains("/tasks") to json("<html>"))
        assertEquals(
            "Todoist の応答を読めませんでした（TodoistTask）",
            failure { TodoistClient("t", html).createTask(TaskPayload(content = "c", priority = 1)) }.message,
        )
        val wrongShape = FakeTransport(urlContains("/projects") to json("""{"results":[null]}"""))
        assertEquals("Todoist の応答を読めませんでした（TodoistProject）", failure { TodoistClient("t", wrongShape).listProjects() }.message)
    }

    @Test
    fun messageForCoversEachStatusFamily() {
        assertEquals("APIトークンが無効です。設定画面でトークンを確認してください。", messageFor(401))
        assertEquals("この操作の権限がありません（forbidden）", messageFor(403, "forbidden"))
        assertTrue(messageFor(404).startsWith("登録先が見つかりません"))
        assertTrue(messageFor(429).contains("多すぎます"))
        assertTrue(messageFor(503).contains("HTTP 503"))
        assertEquals("Todoist への送信に失敗しました（HTTP 418）", messageFor(418))
        assertEquals(
            "（".length + 200 + "）".length + "Todoist への送信に失敗しました（HTTP 400）".length,
            messageFor(400, "y".repeat(300)).length,
        )
    }
}
