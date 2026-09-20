package jp.trackrail.shopclipper.core.sites

import jp.trackrail.shopclipper.Fixtures.USB_CANONICAL
import jp.trackrail.shopclipper.Fixtures.USB_PAGE
import jp.trackrail.shopclipper.Fixtures.USB_SEARCH_URL
import jp.trackrail.shopclipper.Fixtures.YODOBASHI_PAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the extension's test/sites.test.js. allUrlPatterns / extractSpecTable
// (right-click menus and the DOM) have no Android counterpart and are not ported.
class SitesTest {
    @Test
    fun everyAdapterExposesTheSameShape() {
        assertEquals(listOf("amazon", "yodobashi"), Sites.ALL.map { it.id })
        for (site in Sites.ALL) {
            for (value in listOf(site.id, site.displayName, site.label, site.codeName, site.codeLabel, site.sourceLabel)) {
                assertTrue("${site.id}: $value", value.isNotEmpty())
            }
            assertTrue(site.id, site.hostSuffixes.isNotEmpty())
            assertTrue(Regex("^【.+】$").matches(site.codeLabel))
            assertTrue(site.sourceLabel.startsWith("Androidアプリ（"))
            // The code must survive the round trip through its own canonical URL.
            val sample = if (site.id == "amazon") "B0FPCWWBCH" else "100000001001099348"
            assertEquals(sample, site.codeFromUrl(site.canonicalUrl(sample)))
        }
    }

    @Test
    fun amazonAsinFromDpGpProductGpAwDObidosRejectsTheRest() {
        assertEquals("B0FPCWWBCH", Amazon.codeFromUrl(USB_PAGE.url))
        assertEquals("B0FPCWWBCH", Amazon.codeFromUrl(USB_SEARCH_URL))
        assertEquals("B0FPCWWBCH", Amazon.codeFromUrl(USB_CANONICAL))
        assertEquals("B0065C8IUQ", Amazon.codeFromUrl("https://www.amazon.co.jp/gp/product/B0065C8IUQ?th=1"))
        assertEquals("B0065C8IUQ", Amazon.codeFromUrl("https://www.amazon.co.jp/gp/aw/d/B0065C8IUQ/"))
        assertEquals("4003360419", Amazon.codeFromUrl("https://www.amazon.co.jp/exec/obidos/ASIN/4003360419#x"))
        assertNull(Amazon.codeFromUrl("https://www.amazon.co.jp/dp/B0065C8IUQX")) // 11 chars
        assertNull(Amazon.codeFromUrl("https://www.amazon.co.jp/s?k=usb"))
        assertNull(Amazon.codeFromUrl(""))
        assertNull(Amazon.codeFromUrl(null))
    }

    @Test
    fun amazonIsValidCodeAndCleanPageTitle() {
        assertTrue(Amazon.isValidCode("B0FPCWWBCH"))
        assertTrue(Amazon.isValidCode("4003360419")) // ISBN-10 style
        assertFalse(Amazon.isValidCode("b0fpcwwbch"))
        assertFalse(Amazon.isValidCode("B0FPCWWBC"))
        assertFalse(Amazon.isValidCode("B0FPCWWBCH\n")) // the whole value, not a line of it
        assertFalse(Amazon.isValidCode(null))
        assertEquals(USB_PAGE.title, Amazon.cleanPageTitle(USB_PAGE.pageTitle))
        assertEquals("商品A", Amazon.cleanPageTitle("Amazon.co.jp：商品A"))
        assertEquals("Ulanzi 自由雲台 セット", Amazon.cleanPageTitle("Amazon | Ulanzi 自由雲台 セット | ウランジ(Ulanzi) | 雲台・ヘッド"))
        assertEquals("普通のページ : 見出し", Amazon.cleanPageTitle("普通のページ : 見出し"))
        assertEquals("", Amazon.cleanPageTitle(""))
    }

    @Test
    fun yodobashiThe18DigitCodeComesFromProductAndNowhereElse() {
        assertEquals("100000001001099348", Yodobashi.codeFromUrl(YODOBASHI_PAGE.url))
        assertEquals("100000001001099348", Yodobashi.codeFromUrl("https://www.yodobashi.com/product/100000001001099348/?lid=x#tab"))
        assertEquals("100000001001099348", Yodobashi.codeFromUrl("/product/100000001001099348/")) // bare path
        assertEquals("100000001001099348", Yodobashi.codeFromUrl("https://www.yodobashi.com/product/100000001001099348"))
        // ⚠ review and stock pages repeat the digits but are not the product page
        assertNull(Yodobashi.codeFromUrl("https://www.yodobashi.com/community/product/100000001001099348/index.html"))
        assertNull(Yodobashi.codeFromUrl("https://www.yodobashi.com/ec/product/stock/100000001001099348/"))
        assertNull(Yodobashi.codeFromUrl("https://www.yodobashi.com/product/12345/")) // not 18 digits
        assertNull(Yodobashi.codeFromUrl("https://www.yodobashi.com/category/19531/"))
        assertNull(Yodobashi.codeFromUrl("https://www.yodobashi.com"))
        assertNull(Yodobashi.codeFromUrl(""))
        assertNull(Yodobashi.codeFromUrl(null))
    }

    @Test
    fun yodobashiIsValidCodeCanonicalUrlAndCleanPageTitle() {
        assertTrue(Yodobashi.isValidCode("100000001001099348"))
        assertFalse(Yodobashi.isValidCode("10000000100109934")) // 17 digits
        assertFalse(Yodobashi.isValidCode("10000000100109934X"))
        assertFalse(Yodobashi.isValidCode(null))
        assertEquals(YODOBASHI_PAGE.url, Yodobashi.canonicalUrl("100000001001099348"))
        assertEquals(YODOBASHI_PAGE.title, Yodobashi.cleanPageTitle(YODOBASHI_PAGE.pageTitle))
        assertEquals("商品名", Yodobashi.cleanPageTitle("ヨドバシ.com－商品名 通販【送料無料】"))
        assertEquals("ヨドバシ.com", Yodobashi.cleanPageTitle("ヨドバシ.com"))
        assertEquals("", Yodobashi.cleanPageTitle(""))
    }

    @Test
    fun yodobashiUsesTheShoppingYodobashiLabelAndItsOwnCodeLabel() {
        // The label is only added when settings.siteLabels is on (core.labelsFor).
        assertEquals("Shopping_ヨドバシ", Yodobashi.label)
        assertEquals("【商品コード】", Yodobashi.codeLabel)
        assertEquals("【ASIN】", Amazon.codeLabel) // ⛔ kept per site on purpose
        assertNotEquals(Amazon.sourceLabel, Yodobashi.sourceLabel) // the route can be told apart later
    }

    @Test
    fun siteForUrlMatchesHostsAndSubdomainsNeverLookAlikes() {
        assertSame(Amazon, Sites.siteForUrl(USB_PAGE.url))
        assertSame(Amazon, Sites.siteForUrl("https://amazon.co.jp/"))
        assertSame(Amazon, Sites.siteForUrl("HTTPS://WWW.AMAZON.CO.JP/dp/B0FPCWWBCH"))
        assertSame(Yodobashi, Sites.siteForUrl("https://www.yodobashi.com/product/100000001001099348/"))
        assertSame(Yodobashi, Sites.siteForUrl("https://yodobashi.com/"))
        assertNull(Sites.siteForUrl("https://www.amazon.com/dp/B0FPCWWBCH"))
        assertNull(Sites.siteForUrl("https://evil-yodobashi.com.example.com/"))
        assertNull(Sites.siteForUrl("https://www.amazon.co.jp@evil.example.com/")) // user info, not the host
        assertNull(Sites.siteForUrl("https://www.biccamera.com/bc/item/1234/")) // 次回対応
        assertNull(Sites.siteForUrl("not a url"))
        assertNull(Sites.siteForUrl(null))
        // The short link host is not the shop's site.
        assertNull(Sites.siteForUrl("https://amzn.asia/d/00nnga31"))
    }

    @Test
    fun siteForShortUrlKnowsTheAmazonAppsShortener() {
        assertSame(Amazon, Sites.siteForShortUrl("https://amzn.asia/d/00nnga31"))
        assertSame(Amazon, Sites.siteForShortUrl("https://AMZN.ASIA:443/d/00nnga31"))
        assertNull(Sites.siteForShortUrl("https://www.amzn.asia/d/00nnga31")) // exact host only
        assertNull(Sites.siteForShortUrl(USB_PAGE.url))
        assertNull(Sites.siteForShortUrl("https:///nohost"))
        assertEquals(listOf("amzn.asia"), Amazon.shortUrlHosts)
        assertEquals(emptyList<String>(), Yodobashi.shortUrlHosts)
    }

    @Test
    fun cleanSharedNameDropsTheAmazonAppsSalePrefixAndTheYodobashiSuffix() {
        // D13: "セール: " is the Amazon app's, not part of the name.
        assertEquals("Anker Nano Charger", Amazon.cleanSharedName("セール: Anker Nano Charger"))
        assertEquals("Anker", Amazon.cleanSharedName("セール：Anker"))
        assertEquals("商品 セール: 中", Amazon.cleanSharedName("商品 セール: 中")) // only in front
        assertEquals("セール:", Amazon.cleanSharedName("セール:")) // never empties the name
        assertEquals("商品名", Amazon.cleanSharedName("Amazon.co.jp: 商品名 : カテゴリ")) // Chrome's page title
        assertEquals("商品名", Yodobashi.cleanSharedName("商品名 通販【全品無料配達】"))
    }
}
