package jp.trackrail.shopclipper.todoist

// Ported from the Chrome extension's todoist.js (v2.0.0).

/** A failure worth showing to the user. [status] is the HTTP status, 0 when there was none. */
class TodoistError(message: String, val status: Int = 0) : Exception(message) {
    /** The same message, but never null (unlike [Throwable.message]), so screens can show it as is. */
    val text: String = message
}

fun messageFor(status: Int, detail: String = ""): String {
    val tail = if (detail.isNotEmpty()) "（${detail.take(200)}）" else ""
    return when {
        status == 401 -> "APIトークンが無効です。設定画面でトークンを確認してください。"
        status == 403 -> "この操作の権限がありません$tail"
        status == 404 -> "登録先が見つかりません。プロジェクトまたはセクションが削除された可能性があります$tail"
        status == 429 -> "リクエストが多すぎます。少し待ってから再試行してください。"
        status >= 500 -> "Todoist 側でエラーが発生しました（HTTP $status）。少し待ってから再試行してください。"
        else -> "Todoist への送信に失敗しました（HTTP $status）$tail"
    }
}
