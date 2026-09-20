package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.Fixtures.USB_PAGE
import jp.trackrail.shopclipper.core.sites.Amazon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the extension's test/memo.test.js: the form's free-text memo.
// Without a memo the standard blocks must stay byte-identical.
class MemoTest {
    private val base = mapOf(
        "code" to "B0FPCWWBCH",
        "codeLabel" to "【ASIN】",
        "sourceLabel" to Amazon.sourceLabel,
        "price" to "￥999",
        "fullTitle" to "USB 3.0 延長ケーブル",
        "dateIso" to "2026-09-16",
    )

    private fun describe(memo: String?) = buildDescription(
        memo = memo,
        code = base["code"],
        codeLabel = base["codeLabel"],
        sourceLabel = base["sourceLabel"],
        price = base["price"],
        fullTitle = base["fullTitle"],
        dateIso = base["dateIso"],
    )

    @Test
    fun cleanMemoKeepsTheTypedLineBreaksButTidiesEverythingElse() {
        // collapse() would flatten these into one line; a memo must not be flattened.
        assertEquals("1行目\n2行目", cleanMemo("1行目\n2行目"))
        assertEquals("windows\nでも\nLF に", cleanMemo("windows\r\nでも\rLF に"))
        assertEquals("末尾の空白\n全角も", cleanMemo("末尾の空白  \n全角も　\n"))
        // ⛔ Two blank lines in a row would end the Markdown block early.
        assertEquals("上\n\n下", cleanMemo("上\n\n\n\n下"))
        assertEquals("前後の空行", cleanMemo("\n\n  前後の空行  \n\n"))
        assertEquals("", cleanMemo(null))
        assertEquals("", cleanMemo("   \n \n  "))
    }

    @Test
    fun aMemoBecomesTheFirstBlockAheadOfTheProductName() {
        val text = describe("値下がり待ち\n2,500円以下なら買う")
        assertTrue(text.startsWith("**【メモ】**\n値下がり待ち\n2,500円以下なら買う\n\n**【商品名】**"))
        // The standard blocks keep their order and their blank-line separators.
        assertEquals(
            listOf("**【メモ】**", "**【商品名】**", "**【現在価格】**", "**【ASIN】**", "**【取込元】**"),
            text.split("\n\n").map { it.split("\n")[0] },
        )
    }

    @Test
    fun noMemoLeavesTheDescriptionExactlyAsWithoutTheField() {
        val plain = describe(null)
        assertTrue(plain.startsWith("**【商品名】**"))
        assertFalse(plain.contains("【メモ】"))
        // An empty, blank or missing memo must all be indistinguishable.
        for (memo in listOf(null, "", "   ", "\n\n", "　")) assertEquals("memo=$memo", plain, describe(memo))
    }

    @Test
    fun anOverlongMemoIsCutSoItCannotBuryTheStandardBlocks() {
        val text = describe("あ".repeat(MEMO_LIMIT + 50))
        val memoBlock = text.split("\n\n")[0].split("\n")[1]
        assertEquals(MEMO_LIMIT, memoBlock.length)
        assertTrue(memoBlock.endsWith("…"))
        assertTrue(text.contains("**【取込元】**"))
    }

    @Test
    fun buildTaskFromDraftCarriesTheMemoThrough() {
        val share = SharedText(url = USB_PAGE.url, name = USB_PAGE.title, site = Amazon)
        val draft = draftFromShare(share)
        val withMemo = buildTaskFromDraft(draft, DraftForm(memo = "3Dプリンター用の補充"))
        assertTrue(withMemo.description!!.startsWith("**【メモ】**\n3Dプリンター用の補充\n\n"))
        // Same draft, no memo.
        val without = buildTaskFromDraft(draft)
        assertTrue(without.description!!.startsWith("**【商品名】**"))
        assertEquals(withMemo.content, without.content)
    }
}
