package jp.trackrail.shopclipper.share

import jp.trackrail.shopclipper.SharedSamples
import jp.trackrail.shopclipper.core.SharedText
import jp.trackrail.shopclipper.core.sites.Amazon
import jp.trackrail.shopclipper.core.sites.Yodobashi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// M11: every share captured on the Pixel 9 Pro (計画書 §2) goes through the parser.
class SharedTextParserTest {
    @Test
    fun theSamplesAreTheOnesRecordedInThePlan() {
        // Lengths from 計画書 §2: name 105 / 169 (with "セール: ") + " " + 28-character short URL.
        assertEquals(105 + 1 + 28, SharedSamples.S1_TEXT.length)
        assertEquals(169 + 1 + 28, SharedSamples.S2_TEXT.length)
        assertEquals(SharedSamples.S2_TEXT.length, SharedSamples.S2_TEXT_2.length)
        assertEquals(5, SharedSamples.LOCATIONS.size)
    }

    @Test
    fun s1AmazonAppNameThenShortUrlAndTheBoilerplateSubjectIsIgnored() {
        val s = SharedTextParser.parse(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)!!
        assertEquals("https://amzn.asia/d/00nnga31", s.url)
        assertEquals(
            "TINMORRY TPU 95Aフィラメント 1.75mm 3Dプリンター用 柔軟フィラメント 1kg クリア | 95A硬度 柔軟素材 高弾性・高靭性・高耐久性 ほとんどのFDMプリンターに対応 柔軟パーツ用",
            s.name,
        )
        assertEquals(105, s.name.length)
        assertEquals(Amazon, s.site)
        assertEquals(true, s.isShortUrl)
    }

    @Test
    fun s2AmazonAppOnSaleTheSalePrefixGoes() {
        val expected = "Anker Nano Charger (45W, Display, スイングプラグ) ホワイト | 45W 充電器 USB PD USB-C 【PSE技術基準適合/約180度折りたたみ式プラグ】iPhone MacBook Air タブレット その他各種機器対応 iPhone 18 / 17 / 16シリーズ / Air 対応"
        val first = SharedTextParser.parse(SharedSamples.S2_TEXT, SharedSamples.S2_SUBJECT)!!
        assertEquals(SharedText("https://amzn.asia/d/0au4KMzt", expected, Amazon, isShortUrl = true), first)
        assertEquals(169 - "セール: ".length, first.name.length) // D13
        // The second share of the same product: only the short URL differs.
        val second = SharedTextParser.parse(SharedSamples.S2_TEXT_2, SharedSamples.S2_SUBJECT)!!
        assertEquals(first.copy(url = "https://amzn.asia/d/08V8WZTw"), second)
    }

    @Test
    fun s4YodobashiAppNameShopSuffixAndProductUrlWithoutSubject() {
        assertEquals(
            SharedText(
                url = "https://www.yodobashi.com/product/100000001006818169/",
                name = "コンサイス 抗菌/耐コピー クリアカバー 文庫・コミック文庫 KC-3",
                site = Yodobashi,
                isShortUrl = false,
            ),
            SharedTextParser.parse(SharedSamples.S4_TEXT, null),
        )
    }

    @Test
    fun s5ChromeUrlOnlyTextAndThePageTitleAsSubject() {
        assertEquals(
            SharedText(
                url = "https://www.yodobashi.com/product/100000001001099348/",
                name = "ティービーケー 鼻洗浄器用洗浄剤 ハナクリーンS専用洗浄剤 （50包入） サーレS",
                site = Yodobashi,
            ),
            SharedTextParser.parse(SharedSamples.S5_TEXT, SharedSamples.S5_SUBJECT),
        )
    }

    @Test
    fun aShortUrlAloneNeverTakesTheAmazonAppsBoilerplateAsTheName() {
        val s = SharedTextParser.parse("https://amzn.asia/d/00nnga31", SharedSamples.S1_SUBJECT)!!
        assertEquals("", s.name)
        assertEquals(true, s.isShortUrl)
    }

    @Test
    fun anyAppWithAUrlIsAcceptedAsPageNamePlusUrl() {
        // D7: a page no adapter claims keeps its title as is.
        assertEquals(
            SharedText("https://example.com/a?x=1", "Example ページ", null),
            SharedTextParser.parse("https://example.com/a?x=1", "  Example   ページ "),
        )
        assertEquals(SharedText("https://example.com/", "", null), SharedTextParser.parse("https://example.com/", null))
        assertEquals(
            SharedText("http://example.com/b", "記事 の まとめ", null),
            SharedTextParser.parse("記事\nhttp://example.com/b\nの　まとめ", "件名は使わない"),
        )
        assertEquals("HTTPS://EXAMPLE.COM/", SharedTextParser.parse("見て HTTPS://EXAMPLE.COM/", null)?.url)
    }

    @Test
    fun theUrlEndsAtASpaceAJapaneseCharacterOrSentencePunctuation() {
        assertEquals("https://example.com/a", SharedTextParser.parse("見て https://example.com/a。次", null)?.url)
        assertEquals("https://example.com/a", SharedTextParser.parse("見て https://example.com/a.", null)?.url)
        assertEquals("https://example.com/a", SharedTextParser.parse("(https://example.com/a)", null)?.url)
        assertEquals("https://example.com/a", SharedTextParser.parse("https://example.com/a?!", null)?.url)
        // A ")" that closes a "(" inside the URL stays.
        assertEquals("https://ja.wikipedia.org/wiki/A_(B)", SharedTextParser.parse("https://ja.wikipedia.org/wiki/A_(B).", null)?.url)
        // The first URL wins; the rest of the text is the name.
        assertEquals(
            SharedText("https://example.com/1", "https://example.com/2", null),
            SharedTextParser.parse("https://example.com/1 https://example.com/2", null),
        )
    }

    @Test
    fun noUsableUrlMeansNothingToAdd() {
        assertNull(SharedTextParser.parse(null, "https://example.com/in-the-subject"))
        assertNull(SharedTextParser.parse("", null))
        assertNull(SharedTextParser.parse("URL の無い文章", "件名"))
        assertNull(SharedTextParser.parse("ftp://example.com/file", null))
        assertNull(SharedTextParser.parse("https://.", null)) // no host left after the "."
    }
}
