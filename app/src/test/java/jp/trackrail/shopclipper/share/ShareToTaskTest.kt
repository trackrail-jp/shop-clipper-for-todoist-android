package jp.trackrail.shopclipper.share

import jp.trackrail.shopclipper.FakeTransport
import jp.trackrail.shopclipper.SharedSamples
import jp.trackrail.shopclipper.core.DEFAULT_SETTINGS
import jp.trackrail.shopclipper.core.DraftForm
import jp.trackrail.shopclipper.core.buildTaskFromDraft
import jp.trackrail.shopclipper.core.draftFromShare
import jp.trackrail.shopclipper.core.labelsFor
import jp.trackrail.shopclipper.net.HttpRequest
import jp.trackrail.shopclipper.net.HttpResponse
import jp.trackrail.shopclipper.todoist.TaskPayload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

// M11: the real shares (計画書 §2) all the way to the POST /tasks body. The expected
// titles were computed with the Chrome extension's own shorten()/buildContent()
// (node, 2026-09-20), so both implementations agree on them (計画書 R5).
class ShareToTaskTest {
    private val jst = TimeZone.getTimeZone("Asia/Tokyo")
    private val now = Calendar.getInstance(jst).apply { clear(); set(2026, 8, 20, 20, 0) }.time

    private suspend fun taskFor(text: String, subject: String?): TaskPayload {
        val share = SharedTextParser.parse(text, subject)!!
        val resolved = if (share.isShortUrl) {
            val anyRequest: (HttpRequest) -> Boolean = { true }
            val measured: (HttpRequest) -> HttpResponse = { r ->
                HttpResponse(301, "", mapOf("Location" to listOf(SharedSamples.LOCATIONS.getValue(r.url))))
            }
            val transport = FakeTransport(anyRequest to measured)
            (ShortUrlResolver(transport).resolve(share.url) as ShortUrlResolver.Result.Resolved).url
        } else {
            null
        }
        val draft = draftFromShare(share, resolved, DEFAULT_SETTINGS, now, jst)
        return buildTaskFromDraft(draft, DraftForm(labels = labelsFor(draft.site, DEFAULT_SETTINGS)))
    }

    @Test
    fun s1AmazonAppTpuFilament() = runTest {
        val task = taskFor(SharedSamples.S1_TEXT, SharedSamples.S1_SUBJECT)
        assertEquals(
            "[TINMORRY TPU 95Aフィラメント 1.75mm 3Dプリンター用 柔軟フィラメント 1kg クリア |…](https://www.amazon.co.jp/dp/B0CLD7LW4T)",
            task.content,
        )
        assertEquals(
            listOf(
                "**【商品名】**\nTINMORRY TPU 95Aフィラメント 1.75mm 3Dプリンター用 柔軟フィラメント 1kg クリア | 95A硬度 柔軟素材 高弾性・高靭性・高耐久性 ほとんどのFDMプリンターに対応 柔軟パーツ用",
                "**【ASIN】**\nB0CLD7LW4T",
                "**【取込元】**\nAndroidアプリ（Amazon共有）",
            ).joinToString("\n\n"),
            task.description,
        )
        assertEquals(TaskPayload(task.content, 1, task.description), task) // Inbox, p4, no labels (defaults)
    }

    @Test
    fun s2AmazonAppOnSaleBothSharesGiveTheSameTask() = runTest {
        val first = taskFor(SharedSamples.S2_TEXT, SharedSamples.S2_SUBJECT)
        assertEquals(
            "[Anker Nano Charger (45W, Display, スイングプラグ) ホワイト | 45W 充電器…](https://www.amazon.co.jp/dp/B0GF24NN7N)",
            first.content,
        )
        assertEquals(
            "**【商品名】**\nAnker Nano Charger (45W, Display, スイングプラグ) ホワイト | 45W 充電器 USB PD USB-C 【PSE技術基準適合/約180度折りたたみ式プラグ】iPhone MacBook Air タブレット その他各種機器対応 iPhone 18 / 17 / 16シリーズ / Air 対応",
            first.description!!.split("\n\n")[0],
        )
        // A different short URL for the same product: the ASIN, not the short URL, identifies it.
        assertEquals(first, taskFor(SharedSamples.S2_TEXT_2, SharedSamples.S2_SUBJECT))
    }

    @Test
    fun s4YodobashiApp() = runTest {
        val task = taskFor(SharedSamples.S4_TEXT, null)
        assertEquals(
            "[コンサイス 抗菌/耐コピー クリアカバー 文庫・コミック文庫 KC-3](https://www.yodobashi.com/product/100000001006818169/)",
            task.content,
        )
        assertEquals(
            "**【商品名】**\nコンサイス 抗菌/耐コピー クリアカバー 文庫・コミック文庫 KC-3\n\n**【商品コード】**\n100000001006818169\n\n**【取込元】**\nAndroidアプリ（ヨドバシ.com共有）",
            task.description,
        )
    }

    @Test
    fun s5ChromeOnAYodobashiPage() = runTest {
        val task = taskFor(SharedSamples.S5_TEXT, SharedSamples.S5_SUBJECT)
        assertEquals(
            "[ティービーケー 鼻洗浄器用洗浄剤 ハナクリーンS専用洗浄剤 （50包入） サーレS](https://www.yodobashi.com/product/100000001001099348/)",
            task.content,
        )
        assertEquals("**【商品コード】**\n100000001001099348", task.description!!.split("\n\n")[1])
    }
}
