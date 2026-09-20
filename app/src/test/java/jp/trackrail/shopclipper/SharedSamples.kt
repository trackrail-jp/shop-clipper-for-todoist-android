package jp.trackrail.shopclipper

// Share samples captured on a Pixel 9 Pro (Android 17) on 2026-09-20, copied
// verbatim from 計画書 §2 (the I2 receiver saved them byte for byte). M11: the
// parser tests run on these, not on made-up text. Do not "tidy" them.
object SharedSamples {
    // S1: Amazon app. TEXT = name + " " + short URL, SUBJECT = boilerplate.
    const val S1_TEXT = "TINMORRY TPU 95Aフィラメント 1.75mm 3Dプリンター用 柔軟フィラメント 1kg クリア | 95A硬度 柔軟素材 高弾性・高靭性・高耐久性 ほとんどのFDMプリンターに対応 柔軟パーツ用 https://amzn.asia/d/00nnga31"
    const val S1_SUBJECT = "Amazonでご覧ください"

    // S2: Amazon app, a product on sale ("セール: " in front) with a long name.
    // The second share of the same product differs only in the short URL.
    const val S2_TEXT = "セール: Anker Nano Charger (45W, Display, スイングプラグ) ホワイト | 45W 充電器 USB PD USB-C 【PSE技術基準適合/約180度折りたたみ式プラグ】iPhone MacBook Air タブレット その他各種機器対応 iPhone 18 / 17 / 16シリーズ / Air 対応 https://amzn.asia/d/0au4KMzt"
    const val S2_TEXT_2 = "セール: Anker Nano Charger (45W, Display, スイングプラグ) ホワイト | 45W 充電器 USB PD USB-C 【PSE技術基準適合/約180度折りたたみ式プラグ】iPhone MacBook Air タブレット その他各種機器対応 iPhone 18 / 17 / 16シリーズ / Air 対応 https://amzn.asia/d/08V8WZTw"
    const val S2_SUBJECT = "このセールをAmazonでチェック"

    // S4: ヨドバシ app. No SUBJECT (extras held android.intent.extra.TEXT only), referrer null.
    const val S4_TEXT = "コンサイス 抗菌/耐コピー クリアカバー 文庫・コミック文庫 KC-3 通販【全品無料配達】 https://www.yodobashi.com/product/100000001006818169/"

    // S5: Chrome on a ヨドバシ product page. TEXT = the URL only, SUBJECT = the page title.
    const val S5_TEXT = "https://www.yodobashi.com/product/100000001001099348/"
    const val S5_SUBJECT = "ヨドバシ.com - ティービーケー 鼻洗浄器用洗浄剤 ハナクリーンS専用洗浄剤 （50包入） サーレS 通販【全品無料配達】"

    // Where each short URL redirected (one GET from the PC, 301, Location read only).
    // 計画書 §2 records the Location up to its first "&"; the rest (ref_=…&social_share=…)
    // was left out there and is left out here.
    val LOCATIONS: Map<String, String> = linkedMapOf(
        "https://amzn.asia/d/0gYCOYRl" to "https://www.amazon.co.jp/dp/B0CLD7LW4T?ref=cm_sw_r_apan_dp_532MW8Y7JD12VN6BT1DS",
        "https://amzn.asia/d/00nnga31" to "https://www.amazon.co.jp/dp/B0CLD7LW4T?ref=cm_sw_r_apan_dp_R97D0FJ82CQ68NNWC27Z",
        "https://amzn.asia/d/0djS3By9" to "https://www.amazon.co.jp/dp/B0GF24NN7N?ref=cm_sw_r_apan_dp_NSFG51YCDTX102W24DNK",
        "https://amzn.asia/d/0au4KMzt" to "https://www.amazon.co.jp/dp/B0GF24NN7N?ref=cm_sw_r_apan_dp_NSFG51YCDTX102W24DNK_1",
        "https://amzn.asia/d/08V8WZTw" to "https://www.amazon.co.jp/dp/B0GF24NN7N?ref=cm_sw_r_apan_dp_NSFG51YCDTX102W24DNK_2",
    )
}
