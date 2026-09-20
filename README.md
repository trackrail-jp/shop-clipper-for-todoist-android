# ShopClipper（ショップクリップ for Todoist）

Android の Amazon ショッピング アプリなどの「共有」から、Chrome 拡張「ショップクリップ for Todoist」と同じ形式で Todoist にタスクを登録するアプリ。

- **applicationId／namespace**: `jp.trackrail.shopclipper`（2026-09-20 決定。**規約 §1.3 の接頭辞ではない**＝下の「規約からの例外」。公開後は変更不可）
- **計画書（正本・進捗管理）**: [`docs/20260920_共有からTodoist登録/計画書.md`](./docs/20260920_共有からTodoist登録/計画書.md)。次にやることは計画書の §11
- **共通規約**: `..\Androidアプリ開発プロジェクト規約.md`（正本）。本 README には**このアプリ固有の事項だけ**を書く（コーディング規約 S6・S7／Android 規約 §11）
- **元にした Chrome 拡張**: `C:\Dropbox\go_cloud_sync\projects\くろー_Chrome拡張_ショッピングサイトTodoist登録\repo\`（GitHub `trackrail-jp/shop-clipper-for-todoist`）。本件では**読むだけ**（計画書 D12）
- **GitHub**: `trackrail-jp/shop-clipper-for-todoist-android`（公開・MIT © 2026 TrackRail）を受け入れ後に作って push する予定（計画書 D4）。まだリモートは無い
- **配布**: adb で自分の Pixel に入れる（計画書 D3）。Google Play は別途判断
- **状態**: 開発中（I1＝新規作成）

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
| テスト | JVM 単体テスト（JUnit 4.13.2）＋ Kover 0.9.8（C1 90%） |
| Minimum SDK をウィザード既定から変えた理由 | 既定のまま |

- テンプレート（AGP 9.4）では R8 のルールが `app/src/main/keepRules/rules.keep`（`proguard-rules.pro` ではない）、release の最適化が `optimization { enable = false }`、`gradle.properties` で configuration cache が有効になっている。

## C1 90% の対象外（Kover）

`app/build.gradle.kts` の `kover { }`。規約 §6.2 の既定どおり:

- `androidGeneratedClasses()`（Activity・Fragment・BuildConfig・R など）
- `@Composable`（`@Preview` を含む）・`*ComposableSingletons*`・`*.ui.theme`
- 今後足す予定（計画書 §8）: `KeystoreTokenCipher`・`UrlConnectionTransport`（端末の Keystore とネットワークが要るため実機で確かめる）

**陽性対照（2026-09-20・I1）**: テンプレートのままでは測る分岐が 0 件で、**閾値 100 でも `koverVerifyDebug` は通った**。分岐を 1 つ持つ一時クラスを片側だけテストすると、閾値 90 で「branches covered percentage is 50.000000, but expected minimum is 90」と失敗した（一時クラスは削除済み）。⇒ **ロジックのクラスが入るまで C1 90% の検証は空振りする。** 受け入れではテストの実行件数を必ず併記する（S10）。

HTML レポート（`app/build/reports/kover/htmlDebug/index.html`）には、`MainActivity`・Composable の本体・`ui.theme` は出ない。`MainActivityKt` だけが「メソッド 0/2・分岐なし」で残る（Compose コンパイラが生成する再描画用のメソッドと見られる）。分岐が無いので C1 には影響しない。

## 受け入れ記録（コーディング規約 M11：実機で 1 回）

| 日付 | versionCode / versionName | 端末・OS | 確認した内容 | 単体テスト件数 / C1 | 結果 |
|---|---|---|---|---|---|
| 2026-09-20 | 1 / 1.0 | Pixel 9 Pro・Android 17 / API 37（Wi-Fi） | I1: debug APK を `adb install -r` → `am start -W` で `MainActivity` が前面（`ResumedActivity`）、「Hello Android!」を表示（[画面](./docs/20260920_共有からTodoist登録/証跡/I1_初回起動_Pixel9Pro.png)）。`dumpsys package` で versionCode=1・minSdk=24・targetSdk=37 | 1 件（`ExampleUnitTest`）／ 測る分岐 0 件 | ✅ |

## 残している lint 警告

2026-09-20（I1）の `lintDebug`: **エラー 0・警告 17**。

| 警告 | 件数 | 残す理由 |
|---|---|---|
| `GradleDependency`・`NewerVersionAvailable`・`AndroidGradlePluginVersion`（core-ktx・lifecycle・activity-compose・Compose BOM・androidx.test・Kotlin の compose プラグイン・Kover 0.9.9・Gradle 9.7.1 が出ている） | 9 | 版はテンプレートの生成値を採用し、上げるときは AGP Upgrade Assistant を使う（規約 §6.1）。依存を足す I3 以降で、上げるかどうかをまとめて判断する |
| `UnusedResources`（テンプレートの `colors.xml` の 7 色） | 7 | テンプレートのまま。I6（仕上げ）で整理する |
| `RedundantLabel`（`MainActivity` の `android:label` がアプリ名と同じ） | 1 | テンプレートのまま。I2 でマニフェストを触るときに直す |

## アプリ固有の知見・インシデント

（番号は `INC-SC-NNN`。複数のアプリに効く知見は共通規約へ移す）

- まだ無い。I1 で分かった Kover の挙動（分岐 0 件では閾値 100 でも通る・Gradle 9.6 での非推奨警告）は、複数のアプリに効くので共通規約 §6.2 に書いた

## 変更履歴

| 日付 | 内容 |
|---|---|
| 2026-09-20 | 作成（I1）。Empty Activity から作成、雛形・Kover 0.9.8 を入れ、計画書を `docs/` へ移した。Pixel 9 Pro で起動を確認し、`gradlew` に実行権限を付けた（Windows の Git は `core.filemode=false` のため 100644 で入っていた） |
