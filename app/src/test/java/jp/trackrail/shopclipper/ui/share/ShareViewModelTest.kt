package jp.trackrail.shopclipper.ui.share

import jp.trackrail.shopclipper.FakeSettingsStore
import jp.trackrail.shopclipper.FakeTransport
import jp.trackrail.shopclipper.SharedSamples
import jp.trackrail.shopclipper.json
import jp.trackrail.shopclipper.net.HttpRequest
import jp.trackrail.shopclipper.net.HttpResponse
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

// The share flow on the real shares of 計画書 §2 (M11): the same texts the Pixel
// received go in, and the POST /tasks body comes out.
@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {
    private val saved = mapOf(
        "token" to "tok",
        "siteLabels" to true,
        "projectId" to "cand",
        "sectionId" to "sec1",
        "targetLabel" to "🛒 ほしいもの / いますぐ",
    )

    private val projectsJson = """{"results":[{"id":"cand","name":"🛒 ほしいもの","child_order":1}],"next_cursor":null}"""
    private val sectionsJson = """{"results":[{"id":"sec1","name":"いますぐ","section_order":1}],"next_cursor":null}"""
    private val createdJson = """{"id":"T99","content":"c"}"""

    private val redirect: (HttpRequest) -> HttpResponse = { request ->
        HttpResponse(301, "", mapOf("Location" to listOf(SharedSamples.LOCATIONS.getValue(request.url))))
    }

    private fun transport(
        search: String = """{"results":[],"next_cursor":null}""",
        shortUrl: (HttpRequest) -> HttpResponse = redirect,
        projects: (HttpRequest) -> HttpResponse = json(projectsJson),
        create: (HttpRequest) -> HttpResponse = json(createdJson),
    ) = FakeTransport(
        urlContains("amzn.asia") to shortUrl,
        urlContains("/tasks/filter") to json(search),
        urlContains("/projects") to projects,
        urlContains("/sections") to json(sectionsJson),
        urlContains("/tasks") to create,
    )

    private fun model(transport: FakeTransport, store: FakeSettingsStore = FakeSettingsStore(saved)) =
        ShareViewModel(store, transport)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun withoutATokenTheSheetOnlyOffersTheSettings() = runTest {
        val transport = transport()
        val model = ShareViewModel(FakeSettingsStore(), transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        assertEquals(ShareStage.NeedsToken, model.state.value.stage)
        assertEquals(0, transport.calls.size)
    }

    @Test
    fun aSharedTextWithoutAUrlIsRefused() = runTest {
        val transport = transport()
        val model = model(transport)
        model.load("URL の無い文章", "件名")
        assertEquals(ShareStage.NoUrl, model.state.value.stage)
        assertEquals(0, transport.calls.size)
    }

    @Test
    fun theAmazonAppShareResolvesTheShortUrlFillsTheFormAndAdds() = runTest {
        val transport = transport()
        val model = model(transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        val form = model.state.value
        assertEquals(ShareStage.Form, form.stage)
        assertEquals("TINMORRY TPU 95Aフィラメント 1.75mm 3Dプリンター用 柔軟フィラメント 1kg クリア |…", form.name)
        assertEquals("B0CLD7LW4T", form.code)
        assertEquals("【ASIN】", form.codeLabel)
        assertTrue(form.isProduct)
        assertEquals("https://www.amazon.co.jp/dp/B0CLD7LW4T", form.url)
        assertEquals("Shopping_Amazon", form.labels)
        assertEquals(emptyList<String>(), form.warnings)
        assertEquals(100, form.contentLength)
        assertTrue(form.destinationsLoaded) // the pickers have their contents

        model.add()
        val added = model.state.value
        assertEquals(ShareStage.Added, added.stage)
        assertEquals("https://app.todoist.com/app/task/T99", added.addedTaskUrl)
        val post = transport.calls.last { it.method == "POST" }
        assertEquals("https://api.todoist.com/api/v1/tasks", post.url)
        assertTrue(post.body!!.contains(""""content":"[TINMORRY TPU 95Aフィラメント"""))
        assertTrue(post.body!!.contains(""""project_id":"cand""""))
        assertTrue(post.body!!.contains(""""section_id":"sec1""""))
        assertTrue(post.body!!.contains(""""labels":["Shopping_Amazon"]"""))
        assertTrue(post.body!!.contains("""【ASIN】**\nB0CLD7LW4T"""))
        assertTrue(post.body!!.contains("""【取込元】**\nAndroidアプリ（Amazon共有）"""))
        // D6: no price typed -> no 【現在価格】 block.
        assertFalse(post.body!!.contains("現在価格"))
    }

    @Test
    fun theYodobashiAppShareNeedsNoShortUrlLookup() = runTest {
        val transport = transport()
        val model = model(transport)
        model.load(SharedSamples.S4_TEXT, null)
        val form = model.state.value
        assertEquals("100000001006818169", form.code)
        assertEquals("【商品コード】", form.codeLabel)
        assertEquals("コンサイス 抗菌/耐コピー クリアカバー 文庫・コミック文庫 KC-3", form.name)
        assertEquals("Shopping_ヨドバシ", form.labels)
        assertFalse(transport.calls.any { it.url.contains("amzn.asia") })
    }

    @Test
    fun aShortUrlThatCannotBeResolvedWarnsAndKeepsTheShortLink() = runTest {
        val transport = transport(shortUrl = json("<html>", 200))
        val model = model(transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        val form = model.state.value
        assertEquals(ShareStage.Form, form.stage)
        assertEquals("https://amzn.asia/d/00nnga31", form.url)
        assertNull(form.code)
        assertEquals(1, form.warnings.size)
        assertTrue(form.warnings[0], form.warnings[0].startsWith("短縮 URL の先を確かめられませんでした"))
        // Without a code it is a page, so the description uses 【ページ名】.
        model.add()
        val post = transport.calls.last { it.method == "POST" }
        assertTrue(post.body!!.contains("""【ページ名】"""))
        assertEquals(ShareStage.Added, model.state.value.stage)
    }

    @Test
    fun theSameProductIsOnlyAWarning() = runTest {
        val duplicate = """{"results":[{"id":"OLD","content":"[TPU](https://www.amazon.co.jp/dp/B0CLD7LW4T)"}],"next_cursor":null}"""
        val transport = transport(search = duplicate)
        val model = model(transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        val warnings = model.state.value.warnings
        assertEquals(1, warnings.size)
        assertEquals("同じASINのタスクがすでにあります: TPU", warnings[0])
        assertEquals(ShareStage.Form, model.state.value.stage) // 追加 is still allowed (D5)
    }

    @Test
    fun anAddThatFailsKeepsTheSheetOpen() = runTest {
        val transport = transport(create = json("boom", 500))
        val model = model(transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        model.add()
        val state = model.state.value
        assertEquals(ShareStage.Form, state.stage)
        assertFalse(state.busy)
        assertTrue(state.error!!, state.error!!.startsWith("Todoist 側でエラーが発生しました"))
        assertNull(state.addedTaskUrl)
    }

    @Test
    fun aDestinationListThatCannotBeReadIsOnlyAWarning() = runTest {
        val transport = transport(projects = json("Unauthorized", 401))
        val model = model(transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        val state = model.state.value
        assertEquals(ShareStage.Form, state.stage)
        assertEquals(emptyList<Any>(), state.projects)
        assertTrue(state.warnings.last(), state.warnings.last().startsWith("登録先の一覧を読めませんでした"))
        // The saved destination is still used.
        model.add()
        val post = transport.calls.last { it.method == "POST" }
        assertTrue(post.body!!.contains(""""project_id":"cand""""))
    }

    // I6: the pickers used to fall back to 「（インボックス・既定）」「（セクションなし）」, which
    // disagreed with the warning and with where the task actually goes.
    @Test
    fun aDestinationListThatCannotBeReadShowsTheSavedTargetInstead() = runTest {
        val transport = transport(projects = json("Unauthorized", 401))
        val model = model(transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        val state = model.state.value
        assertFalse(state.destinationsLoaded)
        assertEquals("🛒 ほしいもの / いますぐ", state.targetLabel)
        assertEquals("cand", state.projectId)
        assertEquals("sec1", state.sectionId)
    }

    @Test
    fun whatTheUserTypesGoesIntoTheTask() = runTest {
        val transport = transport()
        val model = model(transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        model.onNameChange("TPU フィラメント 1kg")
        model.onPriceChange(" ￥2,480 ")
        model.onMemoChange("3Dプリンター用の補充\n2,000円以下なら買う")
        model.onLabelsChange("Shopping_Amazon, @価格待ち")
        model.onPriorityChange(4)
        model.onProjectSelected("")
        model.add()
        val post = transport.calls.last { it.method == "POST" }
        assertTrue(post.body!!.contains(""""content":"[TPU フィラメント 1kg](https://www.amazon.co.jp/dp/B0CLD7LW4T)""""))
        assertTrue(post.body!!.contains(""""priority":4"""))
        assertTrue(post.body!!.contains(""""labels":["Shopping_Amazon","価格待ち"]"""))
        assertTrue(post.body!!.contains("""【メモ】**\n3Dプリンター用の補充\n2,000円以下なら買う"""))
        assertTrue(post.body!!.contains("""【現在価格】**\n￥2,480（"""))
        // The Inbox was chosen, so no project_id / section_id is sent.
        assertFalse(post.body!!.contains("project_id"))
        assertFalse(post.body!!.contains("section_id"))
    }

    @Test
    fun aPageNoShopClaimsIsAddedAsPageNamePlusUrl() = runTest {
        // The saved project is not in the list any more, so the form falls back to the Inbox.
        val transport = transport(projects = json("""{"results":[{"id":"other","name":"ほか"}],"next_cursor":null}"""))
        val model = model(transport)
        model.load("https://example.com/a?x=1", "Example ページ")
        val form = model.state.value
        assertEquals("Example ページ", form.name)
        assertNull(form.code)
        assertFalse(form.isProduct)
        assertEquals("", form.codeLabel)
        assertEquals("", form.projectId)
        assertEquals("", form.sectionId)
        assertEquals(emptyList<String>(), form.warnings)
        model.add()
        val post = transport.calls.last { it.method == "POST" }
        assertTrue(post.body!!.contains("""【ページ名】**\nExample ページ"""))
        assertTrue(post.body!!.contains("""【取込元】**\nAndroidアプリ（共有）"""))
        assertFalse(post.body!!.contains("project_id"))
    }

    @Test
    fun aDuplicateCheckThatFailsIsOnlyAWarning() = runTest {
        val transport = FakeTransport(
            urlContains("amzn.asia") to redirect,
            urlContains("/tasks/filter") to json("boom", 500),
            urlContains("/projects") to json(projectsJson),
            urlContains("/sections") to json(sectionsJson),
            urlContains("/tasks") to json(createdJson),
        )
        val model = model(transport)
        model.load(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        assertEquals(ShareStage.Form, model.state.value.stage)
        assertTrue(model.state.value.warnings[0], model.state.value.warnings[0].startsWith("重複を確かめられませんでした"))
    }

    @Test
    fun addBeforeTheFormIsReadyDoesNothing() = runTest {
        val transport = transport()
        val model = model(transport)
        model.add()
        assertEquals(ShareStage.Loading, model.state.value.stage)
        assertEquals(0, transport.calls.size)
    }

    @Test
    fun aTaskTodoistAnswersWithoutABodyStillCounts() = runTest {
        val transport = transport(create = json("", 204))
        val model = model(transport)
        model.load(SharedSamples.S4_TEXT, null)
        model.add()
        assertEquals(ShareStage.Added, model.state.value.stage)
        assertNull(model.state.value.addedTaskUrl)
    }

    @Test
    fun choosingAnotherProjectLoadsItsSections() = runTest {
        val transport = transport()
        val model = model(transport)
        model.load(SharedSamples.S5_TEXT, SharedSamples.S5_SUBJECT)
        model.onProjectSelected("cand")
        assertEquals(listOf("sec1"), model.state.value.sections.map { it.id })
        model.onSectionSelected("sec1")
        assertEquals("sec1", model.state.value.sectionId)
        model.onProjectSelected("")
        assertEquals(emptyList<String>(), model.state.value.sections.map { it.id })
        assertEquals("", model.state.value.sectionId)
    }
}
