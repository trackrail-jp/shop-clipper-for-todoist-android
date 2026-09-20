package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.Fixtures.USB_CANONICAL
import jp.trackrail.shopclipper.Fixtures.USB_PAGE
import jp.trackrail.shopclipper.Fixtures.USB_SEARCH_URL
import jp.trackrail.shopclipper.Fixtures.YODOBASHI_PAGE
import jp.trackrail.shopclipper.core.sites.Amazon
import jp.trackrail.shopclipper.core.sites.Yodobashi
import jp.trackrail.shopclipper.todoist.TaskPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the extension's test/lib.test.js (task format).
class TaskFormatTest {
    @Test
    fun buildContentAmazonItemFitsWellUnder500AndIsAMarkdownLink() {
        val (content, linkDropped) = buildContent(name = "USB 3.0 延長ケーブル 3M", url = "https://www.amazon.co.jp/dp/B0FPCWWBCH")
        assertEquals("[USB 3.0 延長ケーブル 3M](https://www.amazon.co.jp/dp/B0FPCWWBCH)", content)
        assertFalse(linkDropped)
    }

    @Test
    fun buildContentVeryLongLabelIsCutSoTheWholeTitleStaysWithin500() {
        val url = "https://www.amazon.co.jp/dp/B0FPCWWBCH"
        val content = buildContent(name = "あ".repeat(800), url = url).content
        assertEquals(TASK_CONTENT_LIMIT, content.length)
        assertTrue(content.endsWith("…]($url)"))
    }

    @Test
    fun buildContentLinkTooLongToFitIsDroppedAndKeptForTheDescription() {
        val url = "https://example.com/" + "p".repeat(600)
        assertEquals(TaskContent("ページ", linkDropped = true), buildContent(name = "ページ", url = url))
    }

    @Test
    fun buildContentNoUrlEmptyNameAndHugeNameWithoutLink() {
        assertEquals(TaskContent("(無題)", linkDropped = false), buildContent(name = "", url = ""))
        assertEquals(500, buildContent(name = "x".repeat(900)).content.length)
        assertEquals("(無題)", buildContent().content)
    }

    @Test
    fun buildDescriptionForAnAmazonProductFollowsTheLabelLayout() {
        val text = buildDescription(
            code = "B0FPCWWBCH",
            codeLabel = Amazon.codeLabel,
            sourceLabel = Amazon.sourceLabel,
            price = "￥999",
            fullTitle = USB_PAGE.title,
            url = "https://www.amazon.co.jp/dp/B0FPCWWBCH",
            dateIso = "2026-09-11",
            linkDropped = false,
        )
        // Bold label, value on the next line, blank line between blocks, and NO "- " bullet.
        assertEquals(
            listOf(
                "**【商品名】**\n${USB_PAGE.title}",
                "**【現在価格】**\n￥999（2026-09-11 登録時点）",
                "**【ASIN】**\nB0FPCWWBCH",
                "**【取込元】**\nAndroidアプリ（Amazon共有）",
            ).joinToString("\n\n"),
            text,
        )
        assertFalse("no bullet in the description", text.contains("\n- "))
    }

    @Test
    fun buildDescriptionForAYodobashiProductSwapsOnlyTheCodeAndSourceLabels() {
        val text = buildDescription(
            code = "100000001001099348",
            codeLabel = Yodobashi.codeLabel,
            sourceLabel = Yodobashi.sourceLabel,
            price = "￥926",
            fullTitle = YODOBASHI_PAGE.title,
            url = YODOBASHI_PAGE.url,
            dateIso = "2026-09-14",
        )
        assertEquals(
            listOf(
                "**【商品名】**\n${YODOBASHI_PAGE.title}",
                "**【現在価格】**\n￥926（2026-09-14 登録時点）",
                "**【商品コード】**\n100000001001099348",
                "**【取込元】**\nAndroidアプリ（ヨドバシ.com共有）",
            ).joinToString("\n\n"),
            text,
        )
        // Same skeleton as Amazon: 4 bold labels, 3 blank lines, 商品名 first.
        assertEquals(4, Regex("\\*\\*【").findAll(text).count())
        assertEquals(4, text.split("\n\n").size)
    }

    @Test
    fun buildDescriptionNoPriceGenericPageDroppedLinkLongTitle() {
        // D6: no price typed -> no 【現在価格】 block (the extension wrote 取得できず).
        assertEquals(
            "**【商品名】**\nx\n\n**【商品コード】**\nB0FPCWWBCH\n\n**【取込元】**\nAndroidアプリ（共有）",
            buildDescription(code = "B0FPCWWBCH", fullTitle = "x"),
        )
        val generic = buildDescription(fullTitle = "t".repeat(1200), url = "https://example.com/long", linkDropped = true)
        assertTrue(
            generic,
            Regex("^\\*\\*【ページ名】\\*\\*\nt+…\n\n\\*\\*【リンク】\\*\\*\nhttps://example\\.com/long\n\n\\*\\*【取込元】\\*\\*\nAndroidアプリ（共有）$")
                .matches(generic),
        )
        assertEquals(FULL_TITLE_LIMIT, generic.split("\n")[1].length)
        assertEquals("**【取込元】**\nAndroidアプリ（共有）", buildDescription())
        // A dropped link with no URL to show adds nothing.
        assertEquals("**【取込元】**\nAndroidアプリ（共有）", buildDescription(linkDropped = true))
    }

    @Test
    fun parseLabelsAcceptsStringsOrListsDedupsAndDropsAt() {
        assertEquals(listOf("Shopping_Amazon", "価格待ち"), parseLabels("Shopping_Amazon, @価格待ち、 Shopping_Amazon ,"))
        assertEquals(listOf("a"), parseLabels(listOf("a", " a ", "", null)))
        assertEquals(emptyList<String>(), parseLabels(null))
    }

    @Test
    fun clampInt() {
        assertEquals(3, clampInt("3", 1, 4, 1))
        assertEquals(4, clampInt(9, 1, 4, 1))
        assertEquals(1, clampInt(-2, 1, 4, 1))
        assertEquals(2, clampInt("abc", 1, 4, 2))
        // Number.parseInt: the leading integer of the text.
        assertEquals(3, clampInt(" 3.7kg", 1, 4, 1))
        assertEquals(200, clampInt("99999999999999999999", 10, 200, 60))
        assertEquals(60, clampInt(null, 10, 200, 60))
        assertEquals(60, clampInt(true, 10, 200, 60))
    }

    @Test
    fun buildTaskPayloadBuildsThePostTasksBody() {
        assertEquals(
            TaskPayload(content = "c", priority = 1, description = "d", projectId = "P", sectionId = "S", labels = listOf("Shopping_Amazon")),
            buildTaskPayload(content = "c", description = "d", projectId = "P", sectionId = "S", labels = listOf("Shopping_Amazon"), priority = "1"),
        )
        assertEquals(TaskPayload(content = "c", priority = 1), buildTaskPayload(content = "c"))
        assertEquals(TaskPayload(content = "c", priority = 1), buildTaskPayload(content = "c", description = "", projectId = "", sectionId = "", labels = ""))
    }

    @Test
    fun taskWebUrlTargetLabelOfStripMarkdownLinks() {
        assertEquals("https://app.todoist.com/app/task/6hX", taskWebUrl("6hX"))
        assertEquals("🛒 ほしいもの / 📥 あとで見る", targetLabelOf("🛒 ほしいもの", "📥 あとで見る"))
        assertEquals("Inbox", targetLabelOf("Inbox", ""))
        assertEquals("（プロジェクト未選択）", targetLabelOf("", null))
        assertEquals("🧂 除湿シリカゲル と x", stripMarkdownLinks("🧂 [除湿シリカゲル](https://a/b) と [x](y)"))
        assertEquals("", stripMarkdownLinks(null))
    }

    @Test
    fun rootCauseTheOfficialTitleFormOverflowsOnASearchResultUrl() {
        // Live page values: title 159 chars, canonical slug URL 746 chars.
        assertEquals(159, USB_PAGE.pageTitle.length)
        assertEquals(746, USB_CANONICAL.length)
        fun official(url: String) = "${USB_PAGE.pageTitle} [${USB_PAGE.pageTitle}]($url)"
        assertTrue(official(USB_PAGE.url).length <= TASK_CONTENT_LIMIT) // short /dp/ URL: 366 chars
        assertTrue(official(USB_SEARCH_URL).length > TASK_CONTENT_LIMIT) // from search results: over 1,000
        val ours = buildContent(
            name = shorten(USB_PAGE.title),
            url = Amazon.canonicalUrl(resolveCode(Amazon, USB_SEARCH_URL)!!),
        ).content
        assertEquals(
            "[USB 3.0 延長ケーブル 1M USB 延長 タイプAオス-タイプAメス USBケーブル データ高速転送5Gbps…](https://www.amazon.co.jp/dp/B0FPCWWBCH)",
            ours,
        )
        assertTrue(ours.length < 150)
        assertNull(resolveCode(Amazon, USB_CANONICAL.replace("/dp/", "/xx/")))
    }
}
