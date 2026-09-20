package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.Fixtures.ULANZI_TITLE
import jp.trackrail.shopclipper.Fixtures.USB_PAGE
import jp.trackrail.shopclipper.Fixtures.YODOBASHI_PAGE
import jp.trackrail.shopclipper.core.sites.Amazon
import jp.trackrail.shopclipper.core.sites.SiteAdapter
import jp.trackrail.shopclipper.core.sites.Yodobashi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

// Ported from the extension's test/lib.test.js (text helpers).
class TextTest {
    private val jst = TimeZone.getTimeZone("Asia/Tokyo")

    @Test
    fun collapseTrimsAndSquashesWhitespaceAndToleratesNull() {
        assertEquals("a b c", collapse("  a \n b\t c "))
        assertEquals("", collapse(null))
        // JavaScript's \s: the full-width space and NBSP count as whitespace too.
        assertEquals("あ い う", collapse("　あ　　い う﻿"))
    }

    @Test
    fun resolveCodePrefersTheSharedUrlThenTheResolvedUrl() {
        assertEquals("B0FPCWWBCH", resolveCode(Amazon, USB_PAGE.url, "https://www.amazon.co.jp/dp/B000000000"))
        assertEquals("B0CCRMZQDG", resolveCode(Amazon, "https://amzn.asia/d/0abcdefg", "https://www.amazon.co.jp/dp/B0CCRMZQDG"))
        assertEquals("B0CCRMZQDG", resolveCode(Amazon, "https://x/s", null, "https://www.amazon.co.jp/dp/B0CCRMZQDG"))
        // ヨドバシ: the code only ever comes from the /product/ URL.
        assertEquals("100000001001099348", resolveCode(Yodobashi, YODOBASHI_PAGE.url))
        assertEquals(
            "100000001001099348",
            resolveCode(Yodobashi, "https://www.yodobashi.com/category/19531/", YODOBASHI_PAGE.url),
        )
        assertNull(resolveCode(Amazon))
        assertNull(resolveCode(Amazon, null, "https://www.amazon.co.jp/s?k=usb"))
        assertNull(resolveCode(null, USB_PAGE.url)) // shop we do not know
    }

    @Test
    fun cleanPageTitleDelegatesToTheAdapterAndPassesUnknownPagesThrough() {
        assertEquals(USB_PAGE.title, cleanPageTitle(Amazon, USB_PAGE.pageTitle))
        assertEquals(YODOBASHI_PAGE.title, cleanPageTitle(Yodobashi, YODOBASHI_PAGE.pageTitle))
        assertEquals("Example ページ", cleanPageTitle(null, "  Example   ページ "))
        // A minimal adapter keeps the default cleanSharedName (= its cleanPageTitle).
        val plain = object : SiteAdapter {
            override val id = "plain"
            override val displayName = "Plain"
            override val hostSuffixes = listOf("example.com")
            override val label = "L"
            override val codeName = "C"
            override val codeLabel = "【C】"
            override val sourceLabel = "S"
            override fun isValidCode(value: String?) = false
            override fun codeFromUrl(url: String?): String? = null
            override fun canonicalUrl(code: String) = code
            override fun cleanPageTitle(title: String?) = collapse(title)
        }
        assertEquals("Amazon.co.jp: そのまま", cleanPageTitle(plain, "Amazon.co.jp: そのまま"))
        assertEquals("x y", plain.cleanSharedName(" x  y "))
        assertEquals(emptyList<String>(), plain.shortUrlHosts)
    }

    @Test
    fun stripPromoRemovesLeadingPromoBracketsButNeverEmptiesTheName() {
        assertEquals("充電器 65W", stripPromo("【2026年最新版】 [by Amazon] ［限定］ 充電器 65W"))
        assertEquals("【告知のみ】", stripPromo("【告知のみ】"))
        assertEquals("USB ケーブル【グレー】", stripPromo("USB ケーブル【グレー】"))
    }

    @Test
    fun shortenKeepsShortNamesCutsAtANearbySpaceElseHardCuts() {
        assertEquals("短い名前", shorten("短い名前", 30))
        val s = shorten(USB_PAGE.title, 30)
        assertTrue(s.endsWith("…"))
        assertTrue(s.length <= 31)
        assertEquals("USB 3.0 延長ケーブル 1M USB 延長…", s)
        assertEquals("あいうえおかきくけこ…", shorten("あいうえおかきくけこさしすせそ", 10)) // no space
        assertEquals("ab cdefghi…", shorten("ab cdefghijklmnop", 10)) // space too early -> hard cut
        assertEquals(61, shorten("x".repeat(80)).length) // default limit 60
    }

    @Test
    fun defaultLimitReproducesTheAmazonSearchResultCut() {
        // The live search listing for B0CCRMZQDG breaks right after 垂直耐荷重20kg.
        assertEquals(
            "Ulanzi 自由雲台 カメラ磁気スタンドセット ボールベッド雲台 360°回転可能 超強力磁力 垂直耐荷重20kg…",
            shorten(ULANZI_TITLE),
        )
        assertEquals(
            "USB 3.0 延長ケーブル 1M USB 延長 タイプAオス-タイプAメス USBケーブル データ高速転送5Gbps…",
            shorten(USB_PAGE.title),
        )
    }

    @Test
    fun safeLinkTextAndSafeUrlKeepTheMarkdownLinkIntact() {
        assertEquals("by Amazon 付箋 黄", safeLinkText("[by Amazon] 付箋 [黄]"))
        assertEquals("", safeLinkText(null))
        assertEquals("https://ja.wikipedia.org/wiki/A_%28B%29%20c", safeUrl(" https://ja.wikipedia.org/wiki/A_(B) c "))
        assertEquals("", safeUrl(null))
    }

    @Test
    fun stripQueryDropsSearchAndHashEmptyOnInvalid() {
        assertEquals("https://example.com/a/b", stripQuery("https://example.com/a/b?x=1#y"))
        assertEquals("", stripQuery("::nope::"))
        assertEquals("", stripQuery(null))
        // An authority with no path gets "/", as the WHATWG parser did in the extension.
        assertEquals("https://example.com/", stripQuery("https://example.com?x=1"))
        // A "?" after the "#" belongs to the fragment.
        assertEquals("https://example.com/a", stripQuery("https://example.com/a#b?c"))
        // Characters java.net.URI rejects do not make the URL disappear.
        assertEquals("https://example.com/a|b", stripQuery("https://example.com/a|b?q={1}"))
    }

    @Test
    fun cleanPriceNormalisesAndRejectsPlaceholders() {
        assertEquals("￥999", cleanPrice(" ￥999 "))
        for (p in listOf("", "N/A", "NA", "-", "—", null)) assertNull(p, cleanPrice(p))
    }

    @Test
    fun todayIsoUsesTheLocalCalendarDate() {
        assertEquals("2026-09-05", todayIso(jstDate(2026, 9, 5, 23, 59), jst))
        // 08:00 in Tokyo is still the previous day in UTC: the zone decides the date.
        val morning = jstDate(2026, 9, 6, 8, 0)
        assertEquals("2026-09-06", todayIso(morning, jst))
        assertEquals("2026-09-05", todayIso(morning, TimeZone.getTimeZone("UTC")))
        assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}").matches(todayIso()))
    }

    private fun jstDate(y: Int, m: Int, d: Int, h: Int, min: Int) =
        Calendar.getInstance(jst).apply { clear(); set(y, m - 1, d, h, min) }.time
}
