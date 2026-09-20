package jp.trackrail.shopclipper

// Real values from the Chrome extension's test/helpers.js (v2.0.0), copied verbatim.
// They were captured on the live pages; the ported tests keep using them so the
// Kotlin port is checked against the same strings as the JavaScript (計画書 R5).
data class PageFixture(val url: String, val pageTitle: String, val title: String, val price: String, val canonicalUrl: String)

object Fixtures {
    // https://www.amazon.co.jp/dp/B0FPCWWBCH on 2026-09-11; the canonical link is 746 characters.
    const val USB_CANONICAL = "https://www.amazon.co.jp/%E3%82%BF%E3%82%A4%E3%83%97A%E3%82%AA%E3%82%B9-%E3%82%BF%E3%82%A4%E3%83%97A%E3%83%A1%E3%82%B9-USB%E3%82%B1%E3%83%BC%E3%83%96%E3%83%AB-%E3%83%87%E3%83%BC%E3%82%BF%E9%AB%98%E9%80%9F%E8%BB%A2%E9%80%815Gbps-%E5%BB%B6%E9%95%B7%E3%82%B3%E3%83%BC%E3%83%89%E9%87%91%E3%83%A1%E3%83%83%E3%82%AD%E3%82%B3%E3%83%8D%E3%82%AF%E3%82%BF-%E3%83%97%E3%83%AA%E3%83%B3%E3%82%BF%E3%83%BC%E3%80%81%E3%82%B9%E3%82%AD%E3%83%A3%E3%83%8A%E3%83%BC%E3%80%81%E3%82%AB%E3%83%A1%E3%83%A9%E3%80%81%E3%83%89%E3%83%A9%E3%82%A4%E3%83%96%E3%80%81USB%E3%83%87%E3%82%A3%E3%82%B9%E3%82%AF%E3%80%81%E3%83%9E%E3%82%A6%E3%82%B9%E3%80%81Xbox%E3%80%81%E3%82%AD%E3%83%BC%E3%83%9C%E3%83%BC%E3%83%89%E3%81%AB%E5%AF%BE%E5%BF%9C-%E3%82%B0%E3%83%AC%E3%83%BC/dp/B0FPCWWBCH"

    val USB_PAGE = PageFixture(
        url = "https://www.amazon.co.jp/dp/B0FPCWWBCH?th=1",
        pageTitle = "Amazon.co.jp: USB 3.0 延長ケーブル 1M USB 延長 タイプAオス-タイプAメス USBケーブル データ高速転送5Gbps USBケーブル 延長コード金メッキコネクタ プリンター、スキャナー、カメラ、ドライブ、USBディスク、マウス、Xbox、キーボードに対応(グレー) : パソコン・周辺機器",
        title = "USB 3.0 延長ケーブル 1M USB 延長 タイプAオス-タイプAメス USBケーブル データ高速転送5Gbps USBケーブル 延長コード金メッキコネクタ プリンター、スキャナー、カメラ、ドライブ、USBディスク、マウス、Xbox、キーボードに対応(グレー)",
        price = "￥999",
        canonicalUrl = "https://www.amazon.co.jp/%E3%82%BF%E3%82%A4%E3%83%97A%E3%82%AA%E3%82%B9-%E3%82%BF%E3%82%A4%E3%83%97A%E3%83%A1%E3%82%B9-USB%E3%82%B1%E3%83%BC%E3%83%96%E3%83%AB-%E3%83%87%E3%83%BC%E3%82%BF%E9%AB%98%E9%80%9F%E8%BB%A2%E9%80%815Gbps-%E5%BB%B6%E9%95%B7%E3%82%B3%E3%83%BC%E3%83%89%E9%87%91%E3%83%A1%E3%83%83%E3%82%AD%E3%82%B3%E3%83%8D%E3%82%AF%E3%82%BF-%E3%83%97%E3%83%AA%E3%83%B3%E3%82%BF%E3%83%BC%E3%80%81%E3%82%B9%E3%82%AD%E3%83%A3%E3%83%8A%E3%83%BC%E3%80%81%E3%82%AB%E3%83%A1%E3%83%A9%E3%80%81%E3%83%89%E3%83%A9%E3%82%A4%E3%83%96%E3%80%81USB%E3%83%87%E3%82%A3%E3%82%B9%E3%82%AF%E3%80%81%E3%83%9E%E3%82%A6%E3%82%B9%E3%80%81Xbox%E3%80%81%E3%82%AD%E3%83%BC%E3%83%9C%E3%83%BC%E3%83%89%E3%81%AB%E5%AF%BE%E5%BF%9C-%E3%82%B0%E3%83%AC%E3%83%BC/dp/B0FPCWWBCH",
    )

    val YODOBASHI_PAGE = PageFixture(
        url = "https://www.yodobashi.com/product/100000001001099348/",
        pageTitle = "ヨドバシ.com - ティービーケー 鼻洗浄器用洗浄剤 ハナクリーンS専用洗浄剤 （50包入） サーレS 通販【全品無料配達】",
        title = "ティービーケー 鼻洗浄器用洗浄剤 ハナクリーンS専用洗浄剤 （50包入） サーレS",
        price = "￥926",
        canonicalUrl = "https://www.yodobashi.com/product/100000001001099348/",
    )

    val YODOBASHI_PAGE_2 = PageFixture(
        url = "https://www.yodobashi.com/product/100000001009510449/",
        pageTitle = "ヨドバシ.com - 明治 meiji ほほえみ 明治ほほえみ らくらくキューブ 1620g 赤ちゃん用 0ヶ月～1歳頃 通販【全品無料配達】",
        title = "明治 meiji ほほえみ 明治ほほえみ らくらくキューブ 1620g 赤ちゃん用 0ヶ月～1歳頃",
        price = "￥5,580",
        canonicalUrl = "https://www.yodobashi.com/product/100000001009510449/",
    )

    // Live search-result title for B0CCRMZQDG (2026-09-12, 198 characters).
    const val ULANZI_TITLE = "Ulanzi 自由雲台 カメラ磁気スタンドセット ボールベッド雲台 360°回転可能 超強力磁力 垂直耐荷重20kg 66MMマグネット台座 GoPro用 アクセサリー アルミ合金製 1/4インチネジ カメラマウト GoPro hero13/12/11 Insta360 X5/X4/X3 DJI Osmo Pocket 4/3用 アクションカメラ/ビデオ カメラ/三脚/一眼レフ/DSLRに対応"

    // The USB product opened from search results: slug + ref/query parameters.
    const val USB_SEARCH_URL = "https://www.amazon.co.jp/%E3%82%BF%E3%82%A4%E3%83%97A%E3%82%AA%E3%82%B9-%E3%82%BF%E3%82%A4%E3%83%97A%E3%83%A1%E3%82%B9-USB%E3%82%B1%E3%83%BC%E3%83%96%E3%83%AB-%E3%83%87%E3%83%BC%E3%82%BF%E9%AB%98%E9%80%9F%E8%BB%A2%E9%80%815Gbps-%E5%BB%B6%E9%95%B7%E3%82%B3%E3%83%BC%E3%83%89%E9%87%91%E3%83%A1%E3%83%83%E3%82%AD%E3%82%B3%E3%83%8D%E3%82%AF%E3%82%BF-%E3%83%97%E3%83%AA%E3%83%B3%E3%82%BF%E3%83%BC%E3%80%81%E3%82%B9%E3%82%AD%E3%83%A3%E3%83%8A%E3%83%BC%E3%80%81%E3%82%AB%E3%83%A1%E3%83%A9%E3%80%81%E3%83%89%E3%83%A9%E3%82%A4%E3%83%96%E3%80%81USB%E3%83%87%E3%82%A3%E3%82%B9%E3%82%AF%E3%80%81%E3%83%9E%E3%82%A6%E3%82%B9%E3%80%81Xbox%E3%80%81%E3%82%AD%E3%83%BC%E3%83%9C%E3%83%BC%E3%83%89%E3%81%AB%E5%AF%BE%E5%BF%9C-%E3%82%B0%E3%83%AC%E3%83%BC/dp/B0FPCWWBCH/ref=sr_1_5?crid=2ABCDEFG&keywords=usb+%E5%BB%B6%E9%95%B7&qid=1789000000&sr=8-5"
}
