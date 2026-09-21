package jp.trackrail.shopclipper.ui.settings

import jp.trackrail.shopclipper.FakeSettingsStore
import jp.trackrail.shopclipper.FakeTransport
import jp.trackrail.shopclipper.core.SETTINGS_VERSION
import jp.trackrail.shopclipper.json
import jp.trackrail.shopclipper.net.HttpRequest
import jp.trackrail.shopclipper.urlContains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// The settings screen's logic (計画書 §4・§9 の I4). The Todoist client is the
// real one; only the transport is fake, so the requests are checked too.
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val projectsJson = """
        {"results":[
          {"id":"inbox","name":"インボックス","inbox_project":true,"child_order":5},
          {"id":"shop","name":"🛍 買い物","child_order":3},
          {"id":"cand","name":"🛒 ほしいもの","parent_id":"shop","child_order":1}
        ],"next_cursor":null}
    """.trimIndent()

    private val sectionsJson = """{"results":[{"id":"sec2","name":"あとで","section_order":2},{"id":"sec1","name":"いますぐ","section_order":1}],"next_cursor":null}"""

    private fun transport() = FakeTransport(
        urlContains("/projects") to json(projectsJson),
        urlContains("/sections") to json(sectionsJson),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aFreshInstallShowsTheDefaultsAndDoesNotTouchTheNetwork() = runTest {
        val transport = transport()
        val model = SettingsViewModel(FakeSettingsStore(), transport)
        model.load()
        val state = model.state.value
        assertTrue(state.loaded)
        assertEquals("", state.token)
        assertEquals("", state.projectId)
        assertEquals("インボックス", state.targetLabel)
        assertEquals(1, state.priority) // 1 = P4 on Todoist's scale
        assertEquals("60", state.titleLimit)
        assertEquals(emptyList<HttpRequest>(), transport.calls)
        assertNull(state.testNotice)
    }

    @Test
    fun aSavedTokenRunsTheConnectionTestOnOpeningAndKeepsTheSavedDestination() = runTest {
        val store = FakeSettingsStore(mapOf("projectId" to "cand", "sectionId" to "sec1", "labels" to "価格待ち", "siteLabels" to true))
        val model = SettingsViewModel(store, transport())
        model.onTokenChange("tok") // nothing saved yet, so the token is typed first
        model.load()
        val state = model.state.value
        // load() reads the saved settings, so the typed token is replaced by the saved one (none here).
        assertEquals("", state.token)
        assertEquals("価格待ち", state.labels)
        assertTrue(state.siteLabels)
        assertNull(state.testNotice) // no token -> no connection test

        val saved = FakeSettingsStore(mapOf("token" to "tok", "projectId" to "cand", "sectionId" to "sec1"))
        val withToken = SettingsViewModel(saved, transport())
        withToken.load()
        val connected = withToken.state.value
        assertEquals(Notice("接続OK（プロジェクト 3 件）", ok = true), connected.testNotice)
        assertEquals(listOf("inbox", "shop", "cand"), connected.projects.map { it.id })
        assertEquals("cand", connected.projectId) // the saved project is still there
        assertEquals(listOf("sec1", "sec2"), connected.sections.map { it.id }) // sorted by section_order
        assertEquals("sec1", connected.sectionId)
        assertFalse(connected.busy)
    }

    @Test
    fun aDestinationThatIsGoneFallsBackToTheInbox() = runTest {
        val store = FakeSettingsStore(mapOf("token" to "tok", "projectId" to "deleted", "sectionId" to "gone"))
        val model = SettingsViewModel(store, transport())
        model.load()
        assertEquals("", model.state.value.projectId)
        assertEquals(emptyList<String>(), model.state.value.sections.map { it.id })
        assertEquals("", model.state.value.sectionId)
    }

    @Test
    fun withoutATokenTheConnectionTestSaysSoAndSendsNothing() = runTest {
        val transport = transport()
        val model = SettingsViewModel(FakeSettingsStore(), transport)
        model.testConnection()
        assertEquals(Notice("APIトークンが未設定です。設定画面で登録してください。", ok = false), model.state.value.testNotice)
        assertEquals(0, transport.calls.size)
    }

    @Test
    fun anApiErrorIsShownAsIs() = runTest {
        val transport = FakeTransport(urlContains("/projects") to json("Unauthorized", 401))
        val model = SettingsViewModel(FakeSettingsStore(), transport)
        model.onTokenChange("bad")
        model.testConnection()
        val notice = model.state.value.testNotice!!
        assertFalse(notice.ok)
        assertEquals("APIトークンが無効です。設定画面でトークンを確認してください。", notice.text)
        assertFalse(model.state.value.busy)
        // The token went into the Authorization header only.
        assertEquals("Bearer bad", transport.calls[0].headers["Authorization"])
        assertFalse(transport.calls[0].url.contains("bad"))
    }

    @Test
    fun choosingAProjectLoadsItsSectionsAndTheInboxHasNone() = runTest {
        val transport = transport()
        val model = SettingsViewModel(FakeSettingsStore(), transport)
        model.onTokenChange("tok")
        model.onProjectSelected("shop")
        assertEquals(listOf("sec1", "sec2"), model.state.value.sections.map { it.id })
        assertEquals("", model.state.value.sectionId)
        model.onSectionSelected("sec2")
        assertEquals("sec2", model.state.value.sectionId)
        val requests = transport.calls.size
        model.onProjectSelected("")
        assertEquals(emptyList<String>(), model.state.value.sections.map { it.id })
        assertEquals(requests, transport.calls.size) // the Inbox needs no request
    }

    @Test
    fun aFailingSectionRequestKeepsTheScreenUsable() = runTest {
        val transport = FakeTransport(urlContains("/sections") to json("boom", 500))
        val model = SettingsViewModel(FakeSettingsStore(), transport)
        model.onTokenChange("tok")
        model.onProjectSelected("shop")
        assertFalse(model.state.value.busy)
        assertTrue(model.state.value.testNotice!!.text.startsWith("Todoist 側でエラーが発生しました"))
    }

    @Test
    fun savingNormalisesTheValuesAndNamesTheDestination() = runTest {
        val store = FakeSettingsStore(mapOf("token" to "tok"))
        val model = SettingsViewModel(store, transport())
        model.load()
        model.onProjectSelected("cand")
        model.onSectionSelected("sec1")
        model.onLabelsChange(" 価格待ち , @あとで ")
        model.onSiteLabelsChange(true)
        model.onPriorityChange(4)
        model.onTitleLimitChange("3") // below the minimum
        model.save()
        val state = model.state.value
        assertEquals("10", state.titleLimit) // clamped by normalizeSettings
        assertEquals("価格待ち, あとで", state.labels)
        assertEquals("🛒 ほしいもの / いますぐ", state.targetLabel)
        assertEquals(Notice("保存しました（既定の登録先: 🛒 ほしいもの / いますぐ）", ok = true), state.saveNotice)
        assertEquals(1, store.writes)
        assertEquals("cand", store.data["projectId"])
        assertEquals("sec1", store.data["sectionId"])
        assertEquals(listOf("価格待ち", "あとで"), store.data["labels"])
        assertEquals(true, store.data["siteLabels"])
        assertEquals(4, store.data["priority"])
        assertEquals(10, store.data["titleLimit"])
        assertEquals(SETTINGS_VERSION, store.data["settingsVersion"])
        assertEquals("tok", store.data["token"])
    }

    @Test
    fun savingWithNoProjectKeepsTheInboxAsTheDestination() = runTest {
        val store = FakeSettingsStore()
        val model = SettingsViewModel(store, transport())
        model.load()
        model.save()
        assertEquals("インボックス", model.state.value.targetLabel)
        assertEquals("", store.data["projectId"])
        assertEquals("", store.data["token"])
    }

    @Test
    fun choosingAProjectWithoutATokenSaysSoAndSendsNothing() = runTest {
        val transport = transport()
        val model = SettingsViewModel(FakeSettingsStore(), transport)
        model.onProjectSelected("shop")
        assertEquals(Notice("APIトークンが未設定です。設定画面で登録してください。", ok = false), model.state.value.testNotice)
        assertEquals(0, transport.calls.size)
        assertFalse(model.state.value.busy)
    }

    @Test
    fun aStoreThatCannotBeWrittenIsReported() = runTest {
        val broken = object : jp.trackrail.shopclipper.core.SettingsStore {
            override suspend fun read(): Map<String, Any?> = emptyMap()

            override suspend fun write(values: Map<String, Any?>) = throw java.io.IOException("disk full")
        }
        val model = SettingsViewModel(broken, transport())
        model.load()
        model.save()
        assertEquals(Notice("保存できませんでした（disk full）", ok = false), model.state.value.saveNotice)
    }

    @Test
    fun theUiStateNeverPrintsTheToken() {
        val state = SettingsUiState(token = "secret-token-123", loaded = true)
        assertFalse(state.toString(), state.toString().contains("secret-token-123"))
        assertTrue(state.toString().contains("token=***"))
        assertTrue(SettingsUiState().toString().contains("token=,"))
    }
}
