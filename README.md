# ShopClipper（ショップクリップ for Todoist）

Android の Amazon ショッピング アプリなどの「共有」から、Chrome 拡張「ショップクリップ for Todoist」と同じ形式で Todoist にタスクを登録するアプリ。

- **applicationId／namespace**: `jp.trackrail.shopclipper`（2026-09-20 決定。**規約 §1.3 の接頭辞ではない**＝下の「規約からの例外」。公開後は変更不可）
- **計画書（正本・進捗管理）**: [`docs/20260920_共有からTodoist登録/計画書.md`](./docs/20260920_共有からTodoist登録/計画書.md)。次にやることは計画書の §11
- **共通規約**: `..\Androidアプリ開発プロジェクト規約.md`（正本）。本 README には**このアプリ固有の事項だけ**を書く（コーディング規約 S6・S7／Android 規約 §11）
- **元にした Chrome 拡張**: `C:\Dropbox\go_cloud_sync\projects\くろー_Chrome拡張_ショッピングサイトTodoist登録\repo\`（GitHub `trackrail-jp/shop-clipper-for-todoist`）。本件では**読むだけ**（計画書 D12）
- **GitHub**: `trackrail-jp/shop-clipper-for-todoist-android`（公開・MIT © 2026 TrackRail）を受け入れ後に作って push する予定（計画書 D4）。まだリモートは無い
- **配布**: adb で自分の Pixel に入れる（計画書 D3）。Google Play は別途判断
- **状態**: 開発中（**I5＝共有 → フォーム → 登録まで完了**。2026-09-21 に実機の受け入れが通った。次は I6＝仕上げ・GitHub へ push）

## 規約からの例外

| 項目 | 内容 | 理由 |
|---|---|---|
| applicationId・namespace | 規約 §1.3 の接頭辞（`io.github.trackrail_jp`）ではなく `jp.trackrail.shopclipper` | trackrail.jp のドメインに合わせる（2026-09-20 ユーザー判断。HizukeOsaho の `jp.trackrail.dtformatjp` と同じ扱い） |

## 構成（作成時の実測値）

| 項目 | 値 |
|---|---|
| 作成日 | 2026-09-20（Android Studio 2026.1.4 の New Project → Empty Activity） |
| minSdk / targetSdk / compileSdk | 24（ウィザード既定）／ 37 ／ 37（`compileSdk { version = release(37) }`） |
| AGP / Kotlin / Gradle | AGP 9.4.1（built-in Kotlin）／ Kotlin の compose プラグイン 2.2.10 ／ Gradle 9.6.0 |
| Gradle Daemon JVM | `gradle/gradle-daemon-jvm.properties` の `toolchainVersion=25`（テンプレートが生成。`settings.gradle.kts` に foojay-resolver-convention 1.0.0） |
| CLI の `JAVA_HOME` | Android Studio 同梱の JBR 25（`C:\Program Files\Android\Android Studio\jbr`）。Gradle 9.1 以上のため（規約 §7.2）。Daemon JVM の条件もこれで満たし、JDK の追加ダウンロードは起きない |
| 主な依存 | Compose BOM 2026.02.01・Material 3・activity-compose 1.8.0・core-ktx 1.10.1・lifecycle-runtime-ktx 2.6.1（いずれもテンプレートの生成値＝規約 §6.1） |
| 追加した依存（I3） | kotlinx-serialization-json **1.9.0**＋コンパイラ プラグイン **2.2.10**（AGP 9.4.1 の built-in Kotlin が使う KGP が 2.2.10 のため同じ版。実行時ライブラリは Kotlin 2.2 系で作られた最後の版＝1.10.0 以降は Kotlin 2.3）／ kotlinx-coroutines-test **1.11.0**（テストのみ） |
| 追加した依存（I4） | androidx.datastore:datastore-preferences **1.2.1**（そのときの最新安定版。1.3.0 は alpha）／ androidx.lifecycle:lifecycle-viewmodel-compose は**テンプレートの lifecycle と同じ 2.6.1** を宣言（Compose BOM 経由で実際には **2.9.4** に解決される。`gradlew :app:dependencies --configuration debugRuntimeClasspath` で確認） |
| テスト | JVM 単体テスト（JUnit 4.13.2）＋ Kover 0.9.8（C1 90%） |
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

## 残している lint 警告

2026-09-20（I1）の `lintDebug`: **エラー 0・警告 17**。I2 で `RedundantLabel` を直し、**警告 16**。I3 で依存を足し、**警告 18**（kotlinx.serialization の 2 件が増えた）。I4 で lifecycle-viewmodel-compose を足し、**警告 19**（エラーは 0 のまま）。I5 は依存を足していないので **警告 19 のまま**（I5 で新しく出た 2 件＝`Chooser` の `ModifierParameter`〈`modifier` を他の任意引数より前に〉と `ShareActivity` の `UseKtx`〈`Uri.parse` → `androidx.core.net.toUri`〉は、その場で直した）。

| 警告 | 件数 | 残す理由 |
|---|---|---|
| `GradleDependency`・`NewerVersionAvailable`・`AndroidGradlePluginVersion`（core-ktx・lifecycle-runtime-ktx・lifecycle-viewmodel-compose・activity-compose・Compose BOM・androidx.test・Kotlin の compose／serialization プラグイン 2.4.20・kotlinx-serialization-json 1.11.0・Kover 0.9.9・Gradle 9.7.1 が出ている） | 12 | **I3 で「上げない」と決めた**。版はテンプレートの生成値を採用し、手で 1 つずつ上げない（規約 §6.1）。Kotlin のプラグインと serialization は AGP 9.4.1 の built-in Kotlin（KGP 2.2.10）に合わせてある（上げるには KGP を buildscript で上書きする必要がある）。**I4 でも上げなかった**: lifecycle-viewmodel-compose はテンプレートの lifecycle と同じ 2.6.1 を宣言し、Compose BOM 経由で 2.9.4 に解決されることを `gradlew :app:dependencies` で確かめた（宣言を上げても実際の版は変わらない）。まとめて上げるのは I6（仕上げ）で判断する |
| `UnusedResources`（テンプレートの `colors.xml` の 7 色） | 7 | テンプレートのまま。I6（仕上げ）で整理する |
| ~~`RedundantLabel`（`MainActivity` の `android:label` がアプリ名と同じ）~~ | 0 | I2 で直した（`MainActivity` の `android:label` を削除） |

## アプリ固有の知見・インシデント

（番号は `INC-SC-NNN`。複数のアプリに効く知見は共通規約へ移す）

- **Todoist API の優先度は `4` が P1**（2026-09-21・I5 の受け入れで実測）。公式ドキュメントは Create/Update Task で「1-4, where 1 is highest」と書いているが、タスクのオブジェクト説明の「4 for very urgent … p1 will return 4」が正しい。**Chrome 拡張と本アプリの実装（4＝P1）のままでよい**（計画書 R3 を解決）。
- **オフライン（機内モード）では、Amazon アプリは短縮 URL（`amzn.asia`）を作らずフル URL を共有する**（2026-09-21・I5）。短縮 URL は Amazon のサーバーが作るため。**機内モードでは D8（短縮 URL を解決できない）の経路に入らない**ので、D8 の実機確認には解決できない短縮 URL（`https://amzn.asia/d/00000000`）を `adb` で送る。オフラインでは代わりに「重複を確かめられませんでした」「登録先の一覧を読めませんでした（…保存済みの登録先に追加します）」の 2 件が出る（どちらも警告だけで登録は続けられる＝計画書 D5）。
- I1 で分かった Kover の挙動（分岐 0 件では閾値 100 でも通る・Gradle 9.6 での非推奨警告）と、I3 で分かったこと（Kover が `@Serializable` の生成コードや `?.` の連鎖を分岐として数える・built-in Kotlin の KGP の版の確かめ方・minSdk 24 で使えない API）は、複数のアプリに効くので共通規約 §6.1・§6.2 に書いた

## データの扱い（計画書 R7・D6）

- **Todoist の API トークンは、この端末の中だけ**に置く。Android Keystore の鍵（端末から取り出せない）で AES-256-GCM で暗号化し、暗号文だけを DataStore（`files/datastore/settings.preferences_pb`）に書く。**バックアップと端末間コピーの対象から外している**（`res/xml/backup_rules.xml`・`data_extraction_rules.xml`）。鍵が無くなって復号できないときは「未設定」として扱い、入れ直してもらう（計画書 R6）。
- **ログに出さない**: トークンと `Authorization` ヘッダーはどこにも書き出さない。`HttpRequest.toString()` と `Settings.toString()`・`SettingsUiState.toString()` は伏せ字にする。2026-09-21 の実機確認では `adb logcat` 918 行に `Bearer` も 40 桁 16 進も 0 件だった。
- **通信先は 2 つだけ**: Todoist API（`https://api.todoist.com/api/v1/…`）と、Amazon の短縮 URL（`https://amzn.asia/…`）の転送先を調べるための GET 1 回。**商品ページ本体は取りに行かない**（計画書 D6）。価格は手入力で、ページからは読まない。
- 共有されたテキストは解析するだけで、`clipData` の画像（Chrome が付けてくるサムネイル）は読まない。

## Chrome 拡張との二重管理（計画書 §6・R5）

件名・説明欄の書式は、拡張（JavaScript）と本アプリ（Kotlin）の両方にある。**片方を変えたら、もう片方も直す。** 拡張側への注記は Android 版を公開するときに足す（計画書 D12）。

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
