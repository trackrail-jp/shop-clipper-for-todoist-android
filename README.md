# ShopClipper — Shop Clip for Todoist

**English** ・ [日本語](./README.ja.md)

Share a product page from the Amazon shopping app — or from any Android app that can share a link — and ShopClipper files it in Todoist as a tidy task: a short linked title, and a description split into labelled blocks.

It is the Android counterpart of the Chrome extension [shop-clipper-for-todoist](https://github.com/trackrail-jp/shop-clipper-for-todoist) and writes tasks in exactly the same format, so a shopping list collected on a desktop and one collected on a phone stay consistent.

> **The app's interface is Japanese.** Its two shop adapters are Japanese stores (amazon.co.jp and yodobashi.com) and the description blocks are labelled in Japanese. Anything else you share is still accepted, as "page title + link".

## What it does

1. Share a product from the Amazon app, the Yodobashi app, Chrome, or anything else that shares text.
2. A bottom sheet opens on top of the app you shared from, already filled in: task name, full product name, ASIN or product code, price (optional), project, section, priority, labels, memo.
3. Press **追加** (Add). The task appears in Todoist and the sheet closes itself three seconds later.

Along the way it:

- **resolves Amazon short links** (`amzn.asia/d/…`) to the canonical `https://www.amazon.co.jp/dp/<ASIN>`, so the same product never ends up under two different URLs;
- **warns about duplicates** by searching Todoist for the ASIN or product code before you add;
- **keeps the task name within Todoist's 500-character limit**, shortening the product name (60 characters by default) instead of letting the request fail;
- **warns rather than blocks** — if it cannot reach the network, cannot resolve a short link, or cannot read your project list, it says so and still lets you add the task.

## Supported shops

| Site | Link it writes | Code block | Label |
|---|---|---|---|
| amazon.co.jp (including `amzn.asia` short links) | `https://www.amazon.co.jp/dp/<ASIN>` | 【ASIN】 | `Shopping_Amazon` |
| yodobashi.com | `https://www.yodobashi.com/product/<code>/` | 【商品コード】 | `Shopping_ヨドバシ` |
| anything else | the shared URL with the query string stripped | — | — |

Per-site labels are opt-in — there is a switch for them in the settings screen.

## The task it writes

The task name is a Markdown link, with the product name shortened to fit:

```text
[TINMORRY TPU 95Aフィラメント 1.75mm 3Dプリンター用 柔軟フィラメント 1kg クリア](https://www.amazon.co.jp/dp/B0D5CG9MPK)
```

The description is a series of Markdown blocks, one blank line between each, always in this order:

```text
**【メモ】**
whatever you typed in the form

**【商品名】**
the full product name, not shortened

**【現在価格】**
2,480円（2026-09-21 登録時点）

**【ASIN】**
B0D5CG9MPK

**【取込元】**
Androidアプリ（Amazon共有）
```

【メモ】 and 【現在価格】 appear only when you fill them in — the app never reads a price off the product page. For a page that no shop adapter claims, 【商品名】 becomes 【ページ名】 and the code block is left out.

## Requirements

- Android 7.0 (API 24) or newer.
- A Todoist account and a personal **API token** (Todoist → Settings → Integrations → Developer).

## Install

There is no Play Store listing. Build it yourself (see below) and install it over adb:

```text
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Distributing it to a second personal device through Android's *limited distribution* developer account is planned, but not done yet.

## First run

Open the app (it is called ショップクリップ for Todoist; the share sheet shows it as ショップクリップ) and fill in the settings screen:

| Field | What it does |
|---|---|
| API トークン | Your Todoist token. **接続テスト** (test connection) reports how many projects it can see. |
| 登録先（プロジェクト・セクション） | Where tasks go. Leave it empty to use the Inbox. |
| 優先度 | Default priority for new tasks (P1–P4). |
| ラベル | Labels added to every task. |
| サイト別ラベル | Also add `Shopping_Amazon` / `Shopping_ヨドバシ`. |
| タスク名の長さ | How far to shorten the product name in the task name. |

The share form lets you override the project, section, priority and labels for a single task.

## Privacy and data

- **Your API token never leaves the device.** It is encrypted with AES-256-GCM under a key held in the Android Keystore — which cannot be exported — and only the ciphertext is written to DataStore. It is excluded from Android backup and from device-to-device transfer, so a restored device asks you to enter it again.
- **It is never logged.** The token and the `Authorization` header are redacted in every `toString()`; a full `adb logcat` capture of a real run on a device contained no `Bearer` and no token-shaped string.
- **It talks to two hosts and no others**: `https://api.todoist.com/api/v1/…`, plus a single GET to `https://amzn.asia/…` to find out where a short link points. **It never fetches the product page itself** — no scraping, no price lookup.
- Shared text is parsed in memory. The thumbnail image Chrome attaches to a share is ignored.
- No analytics, no crash reporting, no third-party SDK.

## Build from source

Open the folder in Android Studio 2026.1.4 or newer and build. From the command line:

```text
# JAVA_HOME needs a JDK 25 — the JBR bundled with Android Studio works:
#   C:\Program Files\Android\Android Studio\jbr
gradlew.bat :app:assembleDebug
```

| | |
|---|---|
| Language and UI | Kotlin 2.4.20, Jetpack Compose (BOM 2026.09.00) |
| Build | AGP 9.4.1, Gradle 9.7.1, Gradle Kotlin DSL with a version catalog |
| SDK | compileSdk / targetSdk 37, minSdk 24 |
| Tests | JUnit 4 on the JVM, branch coverage (C1) gated at 90% by Kover |

### Checks

```text
gradlew.bat :app:testDebugUnitTest :app:koverVerifyDebug :app:lintDebug :app:assembleDebug
```

`koverVerifyDebug` fails the build below 90% branch coverage. Composables, generated classes and the two classes that need a real device (the Keystore cipher and the HTTP transport) are excluded, and are checked on a device instead. Lint is kept at zero errors *and* zero warnings.

## Relation to the Chrome extension

The task format lives in two places — in JavaScript in the extension, and in Kotlin here. **Change one, change the other.** The Kotlin tests assert the same expected strings the extension's tests do. [README.ja.md](./README.ja.md) has the file-by-file mapping.

Deliberate differences on Android: 【現在価格】 appears only when a price is typed in; 【取込元】 reads `Androidアプリ（…共有）`; a leading `セール: ` on an Amazon product name is stripped. The extension's context menu, keyboard shortcuts and page DOM reading have no Android equivalent.

## Documentation

The development notes are in Japanese:

- [README.ja.md](./README.ja.md) — acceptance log, coverage exclusions, lint history, and the dual-maintenance table
- [docs/20260920_共有からTodoist登録/計画書.md](./docs/20260920_共有からTodoist登録/計画書.md) — the plan and the progress record

## License

MIT © 2026 TrackRail. See [LICENSE](./LICENSE).
