# ShopClipper（ショップクリップ for Todoist）

**日本語** ・ [English](./README.md)

> GitHub の入口に出る [`README.md`](./README.md) は英語の概要（使い方・ビルド・データの扱い）です。**開発の記録（受け入れ記録・lint の内訳・Kover の対象外・Chrome 拡張との二重管理）はこのファイルが正本**で、英語版には載せていません（計画書 D15・規約 §4.5）。

Android の Amazon ショッピング アプリなどの「共有」から、Chrome 拡張「ショップクリップ for Todoist」と同じ形式で Todoist にタスクを登録するアプリ。

- **applicationId／namespace**: `jp.trackrail.shopclipper`（2026-09-20 決定。**規約 §1.3 の接頭辞ではない**＝下の「規約からの例外」。公開後は変更不可）
- **計画書（正本・進捗管理）**: [`docs/20260920_共有からTodoist登録/計画書.md`](./docs/20260920_共有からTodoist登録/計画書.md)。次にやることは計画書の §11
- **共通規約**: `..\Androidアプリ開発プロジェクト規約.md`（正本）。本 README には**このアプリ固有の事項だけ**を書く（コーディング規約 S6・S7／Android 規約 §11）
- **元にした Chrome 拡張**: `C:\Dropbox\go_cloud_sync\projects\くろー_Chrome拡張_ショッピングサイトTodoist登録\repo\`（GitHub [`trackrail-jp/shop-clipper-for-todoist`](https://github.com/trackrail-jp/shop-clipper-for-todoist)）。移植のあいだは**読むだけ**。2026-09-22 に D12 の宿題として**相互リンクと二重管理の注記だけ**を書き足した（下の「Chrome 拡張との二重管理」）
- **GitHub**: [`trackrail-jp/shop-clipper-for-todoist-android`](https://github.com/trackrail-jp/shop-clipper-for-todoist-android)（公開・MIT © 2026 TrackRail・既定ブランチ `main`）。2026-09-21 の I6 で作成して push した（計画書 D4・規約 §4.5）。**リモートがあっても `.git` の Dropbox 同期は続ける**
- **配布**: adb で自分の端末に入れる（計画書 D3）。**Google Play には出さない。** パッケージ名と署名鍵は、**既存の Google Play Console アカウント（`trackrail_jp`）の「Android デベロッパーの確認」に登録**した（計画書 **D16**・2026-09-21 決定。**限定配布アカウントは作らない**＝D14 を上書き）。詳細は下の「署名と配布」
- **状態**: **完了**（2026-09-22）。計画書の増分 I1〜I8 に加え、残っていた 3 点も片づいた — ①Play Console のフィンガープリントが **確認済み**（パッケージ名は登録済み）②**Xiaomi Pad 6S Pro に I8 を反映**（3 台とも I8）③計画書 D12＝**Chrome 拡張リポジトリとの相互リンク**（拡張側も英語 README に）。詳細は[計画書の §11](./docs/20260920_共有からTodoist登録/計画書.md)

## 規約からの例外

| 項目 | 内容 | 理由 |
|---|---|---|
| applicationId・namespace | 規約 §1.3 の接頭辞（`io.github.trackrail_jp`）ではなく `jp.trackrail.shopclipper` | trackrail.jp のドメインに合わせる（2026-09-20 ユーザー判断。HizukeOsaho の `jp.trackrail.dtformatjp` と同じ扱い） |

## 構成（作成時の実測値）

| 項目 | 値 |
|---|---|
| 作成日 | 2026-09-20（Android Studio 2026.1.4 の New Project → Empty Activity） |
| minSdk / targetSdk / compileSdk | 24（ウィザード既定）／ 37 ／ 37（`compileSdk { version = release(37) }`） |
| AGP / Kotlin / Gradle | 作成時: AGP 9.4.1（built-in Kotlin）／ Kotlin の compose プラグイン 2.2.10 ／ Gradle 9.6.0。**現在**: AGP 9.4.1 ／ Kotlin **2.4.20**（ルートの `buildscript` で KGP を固定＝下記）／ Gradle **9.7.1** |
| Gradle Daemon JVM | `gradle/gradle-daemon-jvm.properties` の `toolchainVersion=25`（テンプレートが生成。`settings.gradle.kts` に foojay-resolver-convention 1.0.0） |
| CLI の `JAVA_HOME` | Android Studio 同梱の JBR 25（`C:\Program Files\Android\Android Studio\jbr`）。Gradle 9.1 以上のため（規約 §7.2）。Daemon JVM の条件もこれで満たし、JDK の追加ダウンロードは起きない |
| 主な依存 | Compose BOM 2026.02.01・Material 3・activity-compose 1.8.0・core-ktx 1.10.1・lifecycle-runtime-ktx 2.6.1（いずれもテンプレートの生成値＝規約 §6.1） |
| 追加した依存（I3） | kotlinx-serialization-json **1.9.0**＋コンパイラ プラグイン **2.2.10**（AGP 9.4.1 の built-in Kotlin が使う KGP が 2.2.10 のため同じ版。実行時ライブラリは Kotlin 2.2 系で作られた最後の版＝1.10.0 以降は Kotlin 2.3）／ kotlinx-coroutines-test **1.11.0**（テストのみ） |
| 追加した依存（I4） | androidx.datastore:datastore-preferences **1.2.1**（そのときの最新安定版。1.3.0 は alpha）／ androidx.lifecycle:lifecycle-viewmodel-compose は**テンプレートの lifecycle と同じ 2.6.1** を宣言（Compose BOM 経由で実際には **2.9.4** に解決される。`gradlew :app:dependencies --configuration debugRuntimeClasspath` で確認）。**I6 で 2.11.0 を明示**した |
| **I6 でまとめて上げた版**（2026-09-21） | **AGP 9.4.1 は据え置き**（すでに最新で、Upgrade Assistant に出せるものが無い）。Gradle ラッパー 9.6.0 → **9.7.1**／core-ktx 1.10.1 → **1.19.0**／androidx.test.ext:junit 1.1.5 → **1.3.0**／espresso-core 3.5.1 → **3.7.0**／lifecycle 2.6.1 → **2.11.0**／activity-compose 1.8.0 → **1.13.0**／Compose BOM 2026.02.01 → **2026.09.00**／Kover 0.9.8 → **0.9.9**／Kotlin のコンパイラ プラグイン 2.2.10 → **2.4.20**／kotlinx-serialization-json 1.9.0 → **1.11.0** |
| アイコン（I6） | Chrome 拡張と同じ意匠（`#1565C0` の地に白いカート＋プラス）。`res/drawable/ic_launcher_foreground.xml`（ベクター。adaptive の 66dp セーフゾーンに収まるので `<monochrome>` にもそのまま使える）と `ic_launcher_background.xml`（単色）。`mipmap-*/ic_launcher*.webp`（API 24〜25 用）は**同じ座標から生成**した（108 の中央 72 を切り出し、角丸／円のマスク） |
| テスト | JVM 単体テスト（JUnit 4.13.2）＋ Kover 0.9.9（C1 90%） |
| Minimum SDK をウィザード既定から変えた理由 | 既定のまま |

- テンプレート（AGP 9.4）では R8 のルールが `app/src/main/keepRules/rules.keep`（`proguard-rules.pro` ではない）、release の最適化が `optimization { enable = false }`、`gradle.properties` で configuration cache が有効になっている。

## C1 90% の対象外（Kover）

`app/build.gradle.kts` の `kover { }`。規約 §6.2 の既定どおり:

- `androidGeneratedClasses()`（Activity・Fragment・BuildConfig・R など）
- `@Composable`（`@Preview` を含む）・`*ComposableSingletons*`・`*.ui.theme`
- `@Serializable` のクラス（`annotatedBy("kotlinx.serialization.Serializable")`・I3 で追加）: kotlinx.serialization のプラグインが各モデルに `write$Self` を生成し、Kover はそれを分岐として数える（Todoist の 4 モデルだけで 140 分岐・40%）。モデルは値を持つだけで、JSON の形は `ModelsTest`・`TodoistClientTest` で確かめている
- `KeystoreTokenCipher*`・`UrlConnectionTransport*`（I4 で追加）: 端末の Keystore とネットワークが要るため、実機で確かめる。**末尾の `*` が要る**（suspend 関数の本体は `UrlConnectionTransport$execute$2` のような入れ子クラスに入るので、クラス名だけでは外れない）

**I3 の計測（2026-09-20）**: 1 回目は 388 分岐中 17 未達（95.6%）で、計測が効いていることを確かめた。未達の多くは Kotlin の `?.` の連鎖と、比較の両側へ展開される inline のラムダが作る「到達しない分岐」だったので書き直し、実在する境界はテストを足した → **366 分岐・100%**（単体テスト 90 件）。

**陽性対照（2026-09-20・I1）**: テンプレートのままでは測る分岐が 0 件で、**閾値 100 でも `koverVerifyDebug` は通った**。分岐を 1 つ持つ一時クラスを片側だけテストすると、閾値 90 で「branches covered percentage is 50.000000, but expected minimum is 90」と失敗した（一時クラスは削除済み）。⇒ **ロジックのクラスが入るまで C1 90% の検証は空振りする。** 受け入れではテストの実行件数を必ず併記する（S10）。

HTML レポート（`app/build/reports/kover/htmlDebug/index.html`）には、`MainActivity`・Composable の本体・`ui.theme` は出ない。`MainActivityKt` だけが「メソッド 0/2・分岐なし」で残る（Compose コンパイラが生成する再描画用のメソッドと見られる）。分岐が無いので C1 には影響しない。

## 受け入れ記録（コーディング規約 M11：実機で 1 回）

| 日付 | versionCode / versionName | 端末・OS | 確認した内容 | 単体テスト件数 / C1 | 結果 |
|---|---|---|---|---|---|
| 2026-09-20 | 1 / 1.0 | Pixel 9 Pro・Android 17 / API 37（Wi-Fi） | I1: debug APK を `adb install -r` → `am start -W` で `MainActivity` が前面（`ResumedActivity`）、「Hello Android!」を表示（[画面](./docs/20260920_共有からTodoist登録/証跡/I1_初回起動_Pixel9Pro.png)）。`dumpsys package` で versionCode=1・minSdk=24・targetSdk=37 | 1 件（`ExampleUnitTest`）／ 測る分岐 0 件 | ✅ |
| 2026-09-20 | 1 / 1.0 | 同上 | I2: 共有シートに「ショップクリップ」が出て、受信画面（仮）が Amazon アプリ（2 商品）・ヨドバシ アプリ・Chrome からの共有を受け取った。原文を計画書 §2 に記録（[画面](./docs/20260920_共有からTodoist登録/証跡/I2_S1_Amazonアプリ.png)ほか）。`adb shell am start … -f 0x18080000` で S1・S5 を再現し、原文の一致を確認 | 1 件 ／ 測る分岐 0 件（受信画面は Activity なので対象外） | ✅ |
| 2026-09-20 | 1 / 1.0 | 同上 | I3: 純粋ロジック（`core`・`todoist`・`share`）を追加。計画書 §2 の原文 5 件と短縮 URL の `Location` 5 件で、共有テキスト → 短縮 URL の解決 → タスクの本文までを単体テストで確認（`ShareToTaskTest`）。debug APK を入れ直し、`am start -W` で `MainActivity` が前面（新しいコードはまだ画面から呼ばれない） | 90 件 ／ 366 分岐・100% | ✅ |
| 2026-09-21 | 1 / 1.0 | 同上 | I4: 設定画面。**ユーザーが端末でトークンを入力**（Claude は見ていない）→「接続OK（プロジェクト 36 件）」（[画面](./docs/20260920_共有からTodoist登録/証跡/I4_接続テスト_Pixel9Pro.png)）→ 既定の登録先「🛒 購入候補・単発 / 00 📥 未整理」・サイト別ラベル ON で保存（[画面](./docs/20260920_共有からTodoist登録/証跡/I4_保存_Pixel9Pro.png)）→ `am force-stop` 後に開き直しても残り、自動の接続テストが通る（[画面](./docs/20260920_共有からTodoist登録/証跡/I4_開き直して復元_Pixel9Pro.png)）。`adb logcat` 918 行に `Bearer` 0 件・40 桁 16 進 0 件。`files/datastore/settings.preferences_pb` は 331 バイトで、トークンは 92 文字の Base64（IV＋暗号文＋タグ）だけ | 109 件 ／ 442 分岐・100% | ✅ |
| 2026-09-21 | 1 / 1.0 | 同上 | I5: 共有 → フォーム → 登録の受け入れ（計画書 §9 の I5）。①Amazon アプリから共有 → 追加 → Todoist で件名（リンク形式・100 字）・説明欄（【メモ】【商品名】【ASIN】【取込元】が空行 1 つ区切り）・ラベル `Shopping_Amazon`・登録先を確認（**価格を入れなかったので【現在価格】は出ない＝D6 どおり**）。②優先度 **P1** で送ったタスクが Todoist で P1 → **API の `priority=4` が P1**（計画書 R3 を解決）。③同じ ASIN の既存タスクで**重複の警告**。④解決できない短縮 URL で **D8 の警告**（[画面](./docs/20260920_共有からTodoist登録/証跡/I5_D8警告_短縮URL解決失敗_Pixel9Pro.png)）。機内モードの共有ではオフライン時の警告 2 件（[画面](./docs/20260920_共有からTodoist登録/証跡/I5_機内モード_オフライン警告_Pixel9Pro.png)）。⑤Chrome で対応外のページ → 「ページ名＋URL」＋【ページ名】＋【取込元】`Androidアプリ（共有）`・ラベルなし。`find-tasks` と `find-activity` の両方で、登録したタスクの client が `Dalvik/…Pixel 9 Pro`＝アプリ自身であることを確認（S10）。テスト用の 3 件は承認のうえ削除（D10） | 123 件 ／ 476 分岐・99.37% | ✅ |
| 2026-09-21 | 1 / 1.0 | 同上 | I6: 仕上げ。①**新しいアイコン**が端末で出る（[画面](./docs/20260920_共有からTodoist登録/証跡/I6_アイコン_Pixel9Pro.png)。Pixel は円形マスク）。②`adb` の `am start … SEND` でヨドバシの商品 URL を共有 → フォームに**重複の警告**（実データ）・商品コード・登録先（🛒 購入候補・単発 / 00 📥 未整理）・ラベル `Shopping_ヨドバシ`・P4・`85 / 500 文字`（[画面](./docs/20260920_共有からTodoist登録/証跡/I6_共有フォーム_Pixel9Pro.png)）。③**今回直した表示**: 端末の Private DNS を存在しないホストにして `api.todoist.com` の名前解決だけを止め（adb は IP 接続なので生きたまま）、同じ共有を送ると、選択欄の代わりに **「登録先: 🛒 購入候補・単発 / 00 📥 未整理」** が出た（[画面](./docs/20260920_共有からTodoist登録/証跡/I6_一覧が読めないときの登録先_Pixel9Pro.png)）。確認後に Private DNS を元の `off` へ戻し、`ping api.todoist.com` が通ることを確かめた。**タスクは 1 件も作っていない** | 124 件 ／ 476 分岐・99.37%（命令 96.53%） | ✅ |
| 2026-09-21 | 1 / 1.0 | 同上 | I7: **R8 を有効にした release ビルド**（`optimization { enable = true }`・アプリ署名鍵で署名）。debug をアンインストールして `adb install` → `am start -W` が **Status: ok / COLD / 269ms**、`ResumedActivity` が `MainActivity`（[画面](./docs/20260920_共有からTodoist登録/証跡/I7_release初回起動_設定初期化_Pixel9Pro.png)。データが消えるので設定は初期値に戻る）。ユーザーがトークンを入れ直し、**接続OK（プロジェクト 36 件）**。`am start … SEND` で §2 の S1（TINMORRY TPU・短縮 URL 付き）を送ると、短縮 URL が `dp/B0CLD7LW4T` に解決され、登録先・`Shopping_Amazon`・`100 / 500 文字` が入った（[画面](./docs/20260920_共有からTodoist登録/証跡/I7_release共有フォーム_Pixel9Pro.png)）→ **「追加」で登録成功**（[画面](./docs/20260920_共有からTodoist登録/証跡/I7_release追加しました_Pixel9Pro.png)）。Todoist で件名・説明欄（【商品名】→【ASIN】→【取込元】。**価格未入力なので【現在価格】なし**）・ラベル・P4・🛒 購入候補・単発 / 00 📥 未整理 を確認し、`find-activity` の client が `Dalvik/…Pixel 9 Pro` であることも確認（S10）。**テスト登録の 1 件は承認のうえ削除**（D10）。logcat 2,374 行に例外 0 件・`Bearer` 0 件 | コード変更なし（124 件 ／ 99.37% のまま） | ✅ |
| 2026-09-21 | 1 / 1.0 | 同上 | I8: 設定画面の案内と保存後の動線（計画書 D17）。**同じ鍵の release を `adb install -r` で上書き**したので、保存済みのトークンと登録先はそのまま残った。①冒頭の「はじめに」カード（3 ステップ・**保存を押すまで反映されない**）が出て、節ごとに区切り線・太字の説明・28dp の余白が付いた（[改善前](./docs/20260920_共有からTodoist登録/証跡/I8_設定画面_改善前_Pixel9Pro.png) → [改善後 上部](./docs/20260920_共有からTodoist登録/証跡/I8_設定画面_改善後_上部_Pixel9Pro.png) ／ [改善後 下部](./docs/20260920_共有からTodoist登録/証跡/I8_設定画面_改善後_下部_Pixel9Pro.png)）。②「保存」を押すと**ダイアログ「保存しました」**が出て既定の登録先を表示（[画面](./docs/20260920_共有からTodoist登録/証跡/I8_保存ダイアログ_Pixel9Pro.png)）、「閉じる」で消え、画面下の通知は残る。③ダイアログの **「Amazon.co.jp を開く」で Amazon アプリが前面**（`dumpsys activity activities` の `topResumedActivity` が `com.amazon.mShop.android.shopping/…MainActivity`）。**ヨドバシはアプリが入っていても Chrome が開いた**（App Links の対象 URL はアプリ側が決める＝設計どおりのフォールバック）。logcat（753 行・陽性対照として `shopclipper` を含む行 14 件）に**アプリの pid 由来の例外 0 件・`Bearer` 0 件・40 桁 16 進 0 件**。**タスクは 1 件も作っていない** | 125 件 ／ 476 分岐・99.37%（`@Composable` は Kover の対象外なので UI を足しても動かない） | ✅ |
| 2026-09-21 | 1 / 1.0 | **Pixel 10 Pro**・Android 17 / API 37（Wi-Fi） | I7 の⑤: **2 台目以降からの「共有 → 登録」**。I8 入りの release を `adb install -r` で上書き（同じ鍵なので**トークンと登録先はそのまま**。開くと自動の接続テストが「接続OK（プロジェクト 36 件）」＝[画面](./docs/20260920_共有からTodoist登録/証跡/I8_設定画面_改善後_Pixel10Pro.png)）。計画書 §2 の S1（Amazon アプリの原文・短縮 URL 付き）を端末へ push したシェル スクリプトから `am start … SEND` で送り、フォームに **ASIN `B0CLD7LW4T`**（短縮 URL を解決）・`100 / 500 文字`・🛒 購入候補・単発 / 00 📥 未整理・P4・`Shopping_Amazon` が入ることを確認（[画面](./docs/20260920_共有からTodoist登録/証跡/I7_共有フォーム_Pixel10Pro.png)）→「追加」→ **「追加しました」**（[画面](./docs/20260920_共有からTodoist登録/証跡/I7_追加しました_Pixel10Pro.png)）。**`find-tasks` と `find-activity` の 2 通りで裏取り**し、`added` イベントの client が `Dalvik/2.1.0 (Linux; U; Android 17; Pixel 10 Pro Build/CP2A.260805.005)`＝アプリ自身であることを確認（S10）。**テスト登録 1 件は承認のうえ削除**（D10） | コード変更なし（125 件 ／ 99.37% のまま） | ✅ |
| 2026-09-22 | 1 / 1.0 | **Xiaomi Pad 6S Pro**（`24018RPACG`）・Android 16 / API 36・HyperOS OS3.0 | 3 台目にも I8 を反映。**HyperOS は `adb install` を `INSTALL_FAILED_USER_RESTRICTED` で弾く**ので、`adb push` で `/sdcard/Download/ShopClipper-release.apk`（I7 版）に**上書き**して置き、**ユーザーが端末のファイル アプリから手でインストール**した。裏取り（S10）: 端末に入った `base.apk`（`pm path` で特定）の **MD5 が PC の release と一致**（`580158a3…ddb3`・1,590,974 バイト）、**`firstInstallTime` は 2026-09-21 19:17:12 のまま**で `lastUpdateTime` だけ 2026-09-22 07:41:58 ＝ **上書き更新なのでトークンと設定は消えていない**。`am start -W` が **Status: ok / COLD / 303ms**、アプリの pid（2727）由来の例外 0 件。**Todoist にタスクは 1 件も作っていない** | コード変更なし（125 件 ／ 99.37% のまま） | ✅ |

## lint 警告（2026-09-21・I6 で 0 件にした）

`lintDebug` は **エラー 0・警告 0**。経緯: I1 は 17 件 → I2 で `RedundantLabel` を直して 16 件 → I3 で依存を足して 18 件 → I4 で 19 件 → I5 は同数（新しく出た `ModifierParameter`・`UseKtx` はその場で直した）→ **I6 で残り 19 件を全部片づけた**。

| 警告 | I5 時点 | I6 での扱い |
|---|---|---|
| `UnusedResources`（テンプレートの `colors.xml` の 7 色） | 7 | **`values/colors.xml` ごと削除**。`themes.xml` はプラットフォームの属性を使い、Compose のテーマは自前の色を持つので、どこからも参照されていなかった |
| `GradleDependency`・`NewerVersionAvailable`・`AndroidGradlePluginVersion` | 12 | **まとめて上げた**（上の「I6 でまとめて上げた版」）。**AGP 9.4.1 自体は最新**で、`AndroidGradlePluginVersion` の 1 件は Gradle ラッパー 9.6.0 → 9.7.1 のことだった＝**AGP Upgrade Assistant に出せるものは無かった**ので、Version Catalog を直接上げた |
| ~~`RedundantLabel`~~ | 0 | I2 で直した（`MainActivity` の `android:label` を削除） |

**Kotlin を 2.4.20 に上げるには KGP の固定が要る**（2026-09-21・I6 で実測）。AGP 9.4.1 の built-in Kotlin は KGP 2.2.10 なので、ルートの `build.gradle.kts` で

```kotlin
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)   // libs は buildscript ブロックでも使える
    }
}
```

と固定した。`gradlew buildEnvironment` が `kotlin-gradle-plugin:2.2.10 -> 2.4.20` と出れば効いている。これで kotlinx-serialization-json も 1.11.0（Kotlin 2.4 系）に上げられる。**Studio 側は Gradle Sync が 1 回要る。**

⚠️ **Kotlin 2.4 は最初の `!!` のあと val を smart-cast する**ので、`post.body!!` を繰り返すと「Unnecessary non-null assertion」が 15 件出た。ローカルの `val` に受け直して 0 件にした。

## 署名と配布（I7・計画書 D16）

🔴 **Play App Signing は使わない。** Google Play に出さないので、`ShopClipper-upload.jks` の鍵が**そのままアプリ署名鍵**。失うと `jp.trackrail.shopclipper` を更新も再登録もできず、**Play Console からリセットを申請する道も無い**（規約 §9 の「アップロード鍵を失ってもリセットできる」は Play App Signing 前提で、本アプリには当てはまらない）。ファイル名の `upload` は規約 §9 の命名に合わせただけで、実体はアップロード鍵ではない。

| 項目 | 値（2026-09-21 実測） |
|---|---|
| 鍵 | `C:\Dropbox\software\AndroidStudio\99_secret\keystores\ShopClipper-upload.jks`（PKCS12・4,296 バイト）。Dropbox 同期 `StorageProviderState=3`、ルートの `.gitignore:15` の `/99_secret/` で Git から除外（`git ls-files` に 0 件） |
| 鍵の作成 | `keytool -genkeypair -storetype PKCS12 -alias upload -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=TrackRail, O=TrackRail, C=JP"`。**実行とパスワード入力はユーザー。Claude は見ていない** |
| 証明書 SHA-256 | `E8:62:6D:B8:63:6E:71:87:FC:0C:54:B4:0E:32:AF:75:EC:F8:AA:57:A3:E5:E1:80:07:FC:DB:24:BD:D2:86:93`（有効期限 2054-02-06） |
| パスワード | `%USERPROFILE%\.gradle\gradle.properties` の `SHOPCLIPPER_UPLOAD_*`（Dropbox の外＝規約 §9）。**PKCS12 はストアと鍵が同じパスワード** |
| release APK | `app/build/outputs/apk/release/app-release.apk` ＝ **1,554,826 バイト**（debug 12.26 MB → **1.48 MB**）。`apksigner verify` が **v2 scheme** で検証 OK、DN は `CN=TrackRail, O=TrackRail, C=JP` |
| 登録先 | Google Play Console（`trackrail_jp`・個人用）の「**Android デベロッパーの確認**」→ パッケージ名 `jp.trackrail.shopclipper`（表示名 `ShopClipper`）。2026-09-21 に SHA-256 を登録 → 直後は「審査中」→ **2026-09-22 にパッケージ名「登録済み」・鍵のフィンガープリント「確認済み」**（鍵 1 個・最終更新日 2026年9月21日）。Play には出さないのでパッケージ自体は「未公開」のまま |
| ⚠️ 証跡 | **Play Console の画面はアカウント ID とメールアドレスが写るので、公開リポジトリの `証跡/` には置かない。** 状態は本表に文字で残す |

**I7 で分かったこと**

- 🔴 **AGP 9 では `isMinifyEnabled` ではなく `optimization { enable = true }`** で R8 を有効にする（テンプレートが `false` で生成している）。これで `minifyReleaseWithR8` タスクが走ることを実測した。規約 §9 の雛形は `isMinifyEnabled` なので、AGP 9 のプロジェクトでは読み替える。
- **`signingConfigs` を `providers.gradleProperty(...).orNull` で書けば、プロパティが無い PC でも debug は通る**。`gradle.properties` が存在しない状態で `:app:assembleDebug` が BUILD SUCCESSFUL することを実測（規約 §12 V2 の半分）。
- **SHA-256 は `gradlew :app:signingReport` で読める**（パスワードは出力されない）。⚠️ **鍵が読めなくてもビルドは成功する**: release の行に `Error: Failed to read key … keystore password was incorrect` と出るだけで `BUILD SUCCESSFUL` になるため、フィンガープリントの行が出ているかを必ず見る。
- ⚠️ **`.properties` のパスワード行が空でも同じエラーになる。** `Read-Host` を含む複数行を 1 度に貼ると入力を取り逃すことがあるので、書けた値の**文字数だけ**を確かめる（値は表示しない）。
- **release ビルドでは `run-as` が使えない**（`package not debuggable`）。I4 でやった `files/datastore/…` の直接確認は debug 限定。
- 🔴 **debug から release へは上書きインストールできない**（署名が違う）。アンインストールが要り、**端末内のトークンと設定は消える**（Keystore の鍵も一緒に消えるので、ファイルが残っても読めない＝R6 のとおり「未設定」扱い）。**ユーザーの承認を取ってから行う。**
- **`input tap` の座標は `uiautomator dump` から採る**（画面 1280×2856）。Compose のボタンもアクセシビリティ ツリーに `text="追加"` で出るので、`bounds` の中心を計算すれば当たる。
- logcat に 40 桁 16 進が出ても、`com.android.systemui` の `go/retraceme <hash>`（R8 の retrace ID）のことがある。**プロセスで切り分ける**（アプリの pid 由来が 0 件ならトークンの漏れではない）。

## アプリ固有の知見・インシデント

（番号は `INC-SC-NNN`。複数のアプリに効く知見は共通規約へ移す）

- **Todoist API の優先度は `4` が P1**（2026-09-21・I5 の受け入れで実測）。公式ドキュメントは Create/Update Task で「1-4, where 1 is highest」と書いているが、タスクのオブジェクト説明の「4 for very urgent … p1 will return 4」が正しい。**Chrome 拡張と本アプリの実装（4＝P1）のままでよい**（計画書 R3 を解決）。
- **オフライン（機内モード）では、Amazon アプリは短縮 URL（`amzn.asia`）を作らずフル URL を共有する**（2026-09-21・I5）。短縮 URL は Amazon のサーバーが作るため。**機内モードでは D8（短縮 URL を解決できない）の経路に入らない**ので、D8 の実機確認には解決できない短縮 URL（`https://amzn.asia/d/00000000`）を `adb` で送る。オフラインでは代わりに「重複を確かめられませんでした」「登録先の一覧を読めませんでした（…保存済みの登録先に追加します）」の 2 件が出る（どちらも警告だけで登録は続けられる＝計画書 D5）。
- I1 で分かった Kover の挙動（分岐 0 件では閾値 100 でも通る・Gradle 9.6 での非推奨警告）と、I3 で分かったこと（Kover が `@Serializable` の生成コードや `?.` の連鎖を分岐として数える・built-in Kotlin の KGP の版の確かめ方・minSdk 24 で使えない API）は、複数のアプリに効くので共通規約 §6.1・§6.2 に書いた
- I6 で分かったこと（AGP が最新のときの依存の上げ方と KGP の固定・Kotlin 2.4 の `!!` の扱い・アイコンの差し替え方・**Private DNS を使ったオフラインの試し方**）も複数のアプリに効くので、共通規約 §6.1・§6.4・§8 に書いた
- **保存後の「ヨドバシ.com を開く」はブラウザが開く**（2026-09-21・I8）。`ACTION_VIEW` に `https://www.yodobashi.com/` を渡しているが、**ヨドバシ アプリが入っている端末でも Chrome が開いた**（Amazon アプリは `https://www.amazon.co.jp/` で開く）。App Links の対象 URL はアプリ側が決めるため。**パッケージ名を直書きして無理に開きにいかない**（アプリ未導入の端末で壊れる）。一般化した書き方は共通規約 §6.5、release を上書きして実機確認する手は §9 に書いた

## データの扱い（計画書 R7・D6）

- **Todoist の API トークンは、この端末の中だけ**に置く。Android Keystore の鍵（端末から取り出せない）で AES-256-GCM で暗号化し、暗号文だけを DataStore（`files/datastore/settings.preferences_pb`）に書く。**バックアップと端末間コピーの対象から外している**（`res/xml/backup_rules.xml`・`data_extraction_rules.xml`）。鍵が無くなって復号できないときは「未設定」として扱い、入れ直してもらう（計画書 R6）。
- **ログに出さない**: トークンと `Authorization` ヘッダーはどこにも書き出さない。`HttpRequest.toString()` と `Settings.toString()`・`SettingsUiState.toString()` は伏せ字にする。2026-09-21 の実機確認では `adb logcat` 918 行に `Bearer` も 40 桁 16 進も 0 件だった。
- **通信先は 2 つだけ**: Todoist API（`https://api.todoist.com/api/v1/…`）と、Amazon の短縮 URL（`https://amzn.asia/…`）の転送先を調べるための GET 1 回。**商品ページ本体は取りに行かない**（計画書 D6）。価格は手入力で、ページからは読まない。
- 共有されたテキストは解析するだけで、`clipData` の画像（Chrome が付けてくるサムネイル）は読まない。

## Chrome 拡張との二重管理（計画書 §6・R5）

件名・説明欄の書式は、拡張（JavaScript）と本アプリ（Kotlin）の両方にある。**片方を変えたら、もう片方も直す。**

✅ **拡張側への注記は 2026-09-22 に入れた**（計画書 D12 を完了）。拡張リポジトリ [`trackrail-jp/shop-clipper-for-todoist`](https://github.com/trackrail-jp/shop-clipper-for-todoist) の README 冒頭に Android 版へのリンク、末尾に「Android 版との関係」の節（同じ書式・**片方を変えたら両方**・この表へのリンク）を置いた。ついでに**拡張側も英語 `README.md` ＋日本語 `README.ja.md` の 2 本立て**にした（ユーザー判断。規約 §4.5 の「既存には遡及しない」を、手が入るこの機会に適用した）。

| 拡張 | 本アプリ |
|---|---|
| `extension/src/lib.js` | `core/Text.kt`・`core/TaskFormat.kt`・`core/ProjectOptions.kt`・`core/Settings.kt`（既定値と移行） |
| `extension/src/core.js` | `core/Settings.kt`・`core/Draft.kt`（`draftFromPage` → `draftFromShare`。`quickAdd` は無い） |
| `extension/src/sites/*.js` | `core/sites/*.kt`（`urlPatterns`・`extractSpec` は無い。`shortUrlHosts`・`cleanSharedName` を足した） |
| `extension/src/todoist.js` | `todoist/TodoistClient.kt`・`todoist/TodoistError.kt`（`fetch` の代わりに `net/HttpTransport`） |
| `test/*.test.js`・`test/helpers.js` | `app/src/test/…`（移植 64 件。実測値は `Fixtures.kt`） |

- 拡張と挙動を変えた点は計画書 §6 の表とその下の一覧。主なもの: 価格を入れなければ【現在価格】を出さない（D6）、【取込元】が `Androidアプリ（…共有）`、「セール: 」を除く（D13）、`stripQuery` は文字列を切るだけ（WHATWG の正規化はしない）、日付は `Calendar`。
- テストの期待値（タスク名）は、拡張自身の `shorten`・`buildContent` を node で動かして求めた（2026-09-20）。書式を変えたときは、拡張のテストと本アプリのテストの両方を直す。

## 変更履歴

| 日付 | 内容 |
|---|---|
| 2026-09-20 | 作成（I1）。Empty Activity から作成、雛形・Kover 0.9.8 を入れ、計画書を `docs/` へ移した。Pixel 9 Pro で起動を確認し、`gradlew` に実行権限を付けた（Windows の Git は `core.filemode=false` のため 100644 で入っていた） |
| 2026-09-20 | I2: 共有を受け取る仮の画面（`ui/share/ShareActivity`）を足し、共有テキストを実測（計画書 §2）。「セール: 」は取り除く（計画書 D13） |
| 2026-09-20 | I3: kotlinx.serialization（1.9.0／プラグイン 2.2.10）と kotlinx-coroutines-test（1.11.0）を追加。拡張の lib・core・sites・todoist を Kotlin へ移し（`core`・`todoist`・`net/HttpTransport`）、共有テキストの解析（`share/SharedTextParser`）と短縮 URL の解決（`share/ShortUrlResolver`）を新しく作った。Kover で `@Serializable` を対象外にした。「Chrome 拡張との二重管理」の節を追加 |
| 2026-09-21 | I4: DataStore（1.2.1）と lifecycle-viewmodel-compose を追加。`net/UrlConnectionTransport`・`data/KeystoreTokenCipher`（AES-256-GCM）・`data/SettingsRepository`・設定画面（`ui/settings`＋`MainActivity`）を作り、`INTERNET` 権限とバックアップ除外を足した。アプリ名を「ショップクリップ for Todoist」に（計画書 D1）。「データの扱い」の節を追加 |
| 2026-09-21 | I5: 共有シートの本体（`ui/share/ShareViewModel`・`ShareSheet`・本物の `ShareActivity`）を作り、設定画面と共用の選択肢を `ui/common/Pickers` に出した。`ShareActivity` を透過テーマ＋`excludeFromRecents` にして、送り元のアプリの上にボトムシートが出るようにした。I2 のデバッグ用の保存（`files/i2_samples.txt`）を消し、端末に残っていたファイルも消した。依存の追加は無し |
| 2026-09-21 | I5 の**受け入れ**（実機 5 項目）。計画書 R3 を解決（Todoist の優先度は `4`＝P1）。機内モードでの Amazon アプリの挙動と D8 の確かめ方を「アプリ固有の知見」に追加し、受け入れ記録に 1 行足した。一覧が読めないときの登録先の表示のずれは I6 の宿題（計画書 §11） |
| 2026-09-21 | 配布の方法を決めた（計画書 **D14**）。Google Play ではなく **Android の「限定配布アカウント」**（無料・政府発行 ID 不要・20 台まで）で配る。実作業は新しい増分 **I7**。判断の材料（開発者確認の全世界展開・Play の 12 人 × 14 日）は計画書 §3、共通の知見は規約 §10.4 |
| 2026-09-21 | I6: 仕上げ。①I5 の受け入れで出た宿題（登録先の一覧を読めないとき、選択欄が「（インボックス・既定）」と出て実際の登録先と食い違う）を直した（`ShareUiState.destinationsLoaded`。一覧が届くまでは保存済みの登録先名を出す）。②**アイコンを Chrome 拡張と同じ意匠に差し替え**（ベクター＋各解像度の webp）。③テンプレートの `colors.xml` を削除。④**依存の版をまとめて上げた**（androidx・Kover・Gradle ラッパー・Kotlin 2.4.20・serialization 1.11.0）。⑤`LICENSE`（MIT © 2026 TrackRail）を追加。単体テスト 124 件・C1 99.37%・lint エラー 0/警告 0 |
| 2026-09-21 | I6.5: **README を英語化**（計画書 D15）。このファイルを `README.md` → **`README.ja.md`** へ `git mv` で改名（履歴は保たれる）し、冒頭に相互リンクを置いた。英語の `README.md` を新設し、**概要・機能・対応サイト・タスクの書式・要件・インストール・初回設定・データの扱い・ビルドとチェック・拡張との関係・ライセンス**を載せた。**受け入れ記録・lint の内訳・Kover の対象外・二重管理の表は英訳せず**、英語版からこのファイルへリンクした。これは ShopClipper 固有の扱いではなく**公開リポジトリ共通のルール**として規約 §4.5・§11 に反映した |
| 2026-09-21 | I7（①〜④）: **署名と登録**（計画書 D16）。①ユーザーが `keytool` でアプリ署名鍵を作成（PKCS12・RSA 4096・10000 日）、`signingConfigs` を `app/build.gradle.kts` に追加。②**AGP 9 の `optimization { enable = true }`** で R8 を有効化し、release APK を 1.48 MB（debug 12.26 MB）で生成。③debug をアンインストールして release を実機へ入れ、起動・共有・登録まで実データで確認（受け入れ記録の I7 行）。④**登録先を限定配布アカウントから既存の Play Console アカウントへ変更**（D16。20 台の上限と端末ごとの承認が不要になる）し、`jp.trackrail.shopclipper` ＋ SHA-256 を登録 → 審査中。知見は規約 §9・§10.4・§12 V2 と本ファイルの「署名と配布」に反映。**残り: I7 の⑤＝2 台目の端末での確認** |
| 2026-09-21 | I8: **設定画面の案内と保存後の動線**（計画書 D17）。①冒頭に**カードで囲んだ「はじめに」**（設定は 1 回だけ・①トークン②登録先③一番下で保存・**保存を押すまで反映されない**）。②節を「1. API トークン／2. 既定の登録先／3. 登録内容（任意）／4. 保存」に整理し、**説明は太字で要点を立てて**操作の直前へ置いた（「一覧は接続テストのあとに出る」を含む）。③節の間の余白を **12dp → 28dp**（`SectionGap`）に広げ、節の頭に区切り線を入れた。④**保存に成功したらダイアログ**で知らせる（`SettingsUiState.justSaved` の一過性フラグ＋`dismissSaved()`）。⑤ダイアログから **Amazon.co.jp ／ ヨドバシ.com を開く**（`SettingsActions.onOpenUrl` に URL を渡すだけで、`startActivity` は `MainActivity` 側。パッケージ名は直書きしない）。単体テスト 125 件・C1 99.37%・lint エラー 0/警告 0。共通に効く知見は規約 §6.5〈新設〉・§9 に反映 |
| 2026-09-21 | I7 の⑤（最後の受け入れ）: **Pixel 10 Pro から「共有 → 登録」を 1 件**通し、`find-tasks` と `find-activity` の 2 通りで裏取りした（client が `Dalvik/…Pixel 10 Pro`）。⚠️ **Todoist の活動ログは数分遅れて載る**ので、登録直後の「0 件」で判断してはいけない（待って引き直す＝S10）。テスト登録は承認のうえ削除。**これで計画書の I1〜I8 がすべて完了**（残るは Play Console の審査結果・Xiaomi への I8 反映・D12 の相互リンク） |
| 2026-09-22 | **残りの 3 点を片づけて本件を完了**。①**Play Console のフィンガープリントが「確認済み」**・パッケージ名は「登録済み」になった（上の「署名と配布」）。②**Xiaomi Pad 6S Pro に I8 を反映**（`adb push` → 端末のファイル アプリから手動インストール。`base.apk` の MD5 一致と `firstInstallTime` 据え置きで上書き更新を確認）＝受け入れ記録に 1 行。③**計画書 D12 を完了**: 拡張リポジトリに相互リンクと「Android 版との関係」の節を入れ、拡張側も英語 `README.md` ＋ `README.ja.md` の 2 本立てにした（拡張は `npm test` 120 件・失敗 0・100% で push 済み）。**コードは 1 行も変えていない**（テスト 125 件・C1 99.37% のまま） |
