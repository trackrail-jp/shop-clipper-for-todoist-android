package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.Fixtures.USB_PAGE
import jp.trackrail.shopclipper.Fixtures.YODOBASHI_PAGE
import jp.trackrail.shopclipper.Fixtures.YODOBASHI_PAGE_2
import jp.trackrail.shopclipper.core.sites.Amazon
import jp.trackrail.shopclipper.core.sites.Yodobashi
import jp.trackrail.shopclipper.share.SharedTextParser
import jp.trackrail.shopclipper.todoist.TaskSearcher
import jp.trackrail.shopclipper.todoist.TodoistTask
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

// Ported from the extension's test/core.test.js (draft, task body, duplicates).
// draftFromPage became draftFromShare; the pages are shared the way Chrome shares
// them (TEXT = the URL, SUBJECT = the page title, 計画書 §2 の S5).
class DraftTest {
    private val jst = TimeZone.getTimeZone("Asia/Tokyo")
    private val now = Calendar.getInstance(jst).apply { clear(); set(2026, 8, 11, 21, 0) }.time

    private fun chromeShare(page: jp.trackrail.shopclipper.PageFixture) = SharedTextParser.parse(page.url, page.pageTitle)!!

    private fun draft(share: SharedText, settings: Settings = DEFAULT_SETTINGS, resolved: String? = null) =
        draftFromShare(share, resolvedUrl = resolved, settings = settings, now = now, zone = jst)

    private class FakeSearch(private val found: List<TodoistTask> = emptyList()) : TaskSearcher {
        val calls = mutableListOf<String>()

        override suspend fun searchTasks(text: String): List<TodoistTask> {
            calls += text
            return found
        }
    }

    @Test
    fun draftFromShareOnTheRealShapedUsbPageShortNameDpAsinDate() {
        assertEquals(
            Draft(
                site = Amazon,
                code = "B0FPCWWBCH",
                fullTitle = USB_PAGE.title,
                name = "USB 3.0 延長ケーブル 1M USB 延長 タイプAオス-タイプAメス USBケーブル データ高速転送5Gbps…",
                url = "https://www.amazon.co.jp/dp/B0FPCWWBCH",
                dateIso = "2026-09-11",
            ),
            draft(chromeShare(USB_PAGE)),
        )
    }

    @Test
    fun draftFromShareOnTheLiveYodobashiPages18DigitCodeAndProductUrl() {
        assertEquals(
            Draft(
                site = Yodobashi,
                code = "100000001001099348",
                fullTitle = YODOBASHI_PAGE.title,
                name = YODOBASHI_PAGE.title, // under the 60-character limit, so untouched
                url = "https://www.yodobashi.com/product/100000001001099348/",
                dateIso = "2026-09-11",
            ),
            draft(chromeShare(YODOBASHI_PAGE)),
        )
        val d2 = draft(chromeShare(YODOBASHI_PAGE_2))
        assertEquals("100000001009510449", d2.code)
        assertEquals("https://www.yodobashi.com/product/100000001009510449/", d2.url)
    }

    @Test
    fun draftFromShareFallsBackForGenericPagesUnresolvedShortUrlsAndBlanks() {
        val generic = draft(SharedText(url = "https://example.com/a?x=1", name = "Example ページ", site = null), Settings(titleLimit = 0))
        assertNull(generic.site)
        assertNull(generic.code)
        assertEquals("https://example.com/a", generic.url)
        assertEquals("Example ページ", generic.name)
        // A ヨドバシ page that is not a product page (レビュー) stays codeless.
        val review = draft(SharedText(url = "https://www.yodobashi.com/community/product/100000001001099348/index.html", name = "レビュー", site = Yodobashi))
        assertEquals(Yodobashi, review.site)
        assertNull(review.code)
        // D8: a short URL that could not be resolved keeps the short URL itself.
        val short = SharedText(url = "https://amzn.asia/d/00nnga31", name = "TPU", site = Amazon, isShortUrl = true)
        assertEquals("https://amzn.asia/d/00nnga31", draft(short).url)
        assertNull(draft(short).code)
        // Resolved: the ASIN comes from where the short URL led.
        assertEquals("https://www.amazon.co.jp/dp/B0CLD7LW4T", draft(short, resolved = "https://www.amazon.co.jp/dp/B0CLD7LW4T?ref=x").url)
        val blank = draft(SharedText(url = "", name = "", site = null))
        assertEquals("", blank.url)
        assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}").matches(draftFromShare(SharedText("", "", null)).dateIso))
    }

    @Test
    fun buildTaskFromDraftTheFailingUsbPageNowFitsAndMatchesTheFormat() {
        val d = draft(chromeShare(USB_PAGE))
        val payload = buildTaskFromDraft(
            d,
            DraftForm(price = "￥999", labels = labelsFor(d.site, Settings(siteLabels = true)), priority = 1),
        )
        assertEquals(
            "[USB 3.0 延長ケーブル 1M USB 延長 タイプAオス-タイプAメス USBケーブル データ高速転送5Gbps…](https://www.amazon.co.jp/dp/B0FPCWWBCH)",
            payload.content,
        )
        assertTrue(payload.content.length < TASK_CONTENT_LIMIT)
        assertNull(payload.projectId) // default destination = the Inbox
        assertNull(payload.sectionId)
        assertEquals(listOf("Shopping_Amazon"), payload.labels)
        assertEquals(1, payload.priority)
        assertEquals(
            listOf(
                "**【商品名】**\n${USB_PAGE.title}",
                "**【現在価格】**\n￥999（2026-09-11 登録時点）",
                "**【ASIN】**\nB0FPCWWBCH",
                "**【取込元】**\nAndroidアプリ（Amazon共有）",
            ).joinToString("\n\n"),
            payload.description,
        )
    }

    @Test
    fun buildTaskFromDraftAYodobashiProductComesOutInTheSameShape() {
        val d = draft(chromeShare(YODOBASHI_PAGE))
        val payload = buildTaskFromDraft(d, DraftForm(labels = labelsFor(d.site, Settings(siteLabels = true)), priority = 1))
        assertEquals(
            "[ティービーケー 鼻洗浄器用洗浄剤 ハナクリーンS専用洗浄剤 （50包入） サーレS](https://www.yodobashi.com/product/100000001001099348/)",
            payload.content,
        )
        assertEquals(listOf("Shopping_ヨドバシ"), payload.labels) // ⛔ never Shopping_Amazon
        assertNull(payload.projectId)
        assertEquals(
            listOf(
                "**【商品名】**\n${YODOBASHI_PAGE.title}",
                "**【商品コード】**\n100000001001099348",
                "**【取込元】**\nAndroidアプリ（ヨドバシ.com共有）",
            ).joinToString("\n\n"),
            payload.description,
        )
    }

    @Test
    fun buildTaskFromDraftAppliesTheEditedNameAndPriceFromTheForm() {
        val d = draft(chromeShare(USB_PAGE))
        val payload = buildTaskFromDraft(d, DraftForm(name = "USB延長ケーブル 3m", price = "N/A", projectId = "P", sectionId = ""))
        assertEquals("[USB延長ケーブル 3m](https://www.amazon.co.jp/dp/B0FPCWWBCH)", payload.content)
        assertTrue(payload.description!!.contains("**【商品名】**\n${USB_PAGE.title}\n\n**【ASIN】**")) // N/A = no price
        assertEquals("P", payload.projectId)
        assertNull(payload.sectionId)
        assertNull(buildTaskFromDraft(d).projectId)
    }

    @Test
    fun buildTaskFromDraftOnAPageNoAdapterClaimsPageNameAndSourceOnly() {
        val d = draft(SharedTextParser.parse("https://example.com/a?x=1", "Example ページ")!!)
        val payload = buildTaskFromDraft(d)
        assertEquals("[Example ページ](https://example.com/a)", payload.content)
        assertEquals("**【ページ名】**\nExample ページ\n\n**【取込元】**\nAndroidアプリ（共有）", payload.description)
    }

    @Test
    fun findDuplicateMatchesTheCodeInTheTitleUrlOrTheDescriptionOnly() = runTest {
        assertNull(findDuplicate(FakeSearch(), null))
        val none = FakeSearch()
        assertNull(findDuplicate(none, ""))
        assertEquals(emptyList<String>(), none.calls)
        val hitInUrl = TodoistTask(id = "A", content = "[x](https://www.amazon.co.jp/dp/B0FPCWWBCH)")
        val hitInDesc = TodoistTask(id = "B", content = "y", description = "**【ASIN】**\nB0FPCWWBCH")
        val fuzzy = TodoistTask(id = "C", content = "b0fpcwwbch lower-case only")
        val search = FakeSearch(listOf(fuzzy, hitInUrl))
        assertEquals("A", findDuplicate(search, "B0FPCWWBCH")?.id)
        assertEquals(listOf("B0FPCWWBCH"), search.calls)
        assertEquals("B", findDuplicate(FakeSearch(listOf(hitInDesc)), "B0FPCWWBCH")?.id)
        assertNull(findDuplicate(FakeSearch(listOf(fuzzy, TodoistTask())), "B0FPCWWBCH"))
        // ヨドバシ: the 18-digit code sits in 【商品コード】 and in the /product/ URL.
        val yodo = TodoistTask(id = "Y", content = "z", description = "**【商品コード】**\n100000001001099348")
        assertEquals("Y", findDuplicate(FakeSearch(listOf(yodo)), "100000001001099348")?.id)
    }
}
