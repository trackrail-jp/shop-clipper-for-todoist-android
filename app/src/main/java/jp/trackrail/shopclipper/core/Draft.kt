package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.core.sites.SiteAdapter
import jp.trackrail.shopclipper.todoist.TaskPayload
import jp.trackrail.shopclipper.todoist.TaskSearcher
import jp.trackrail.shopclipper.todoist.TodoistTask
import java.util.Date
import java.util.TimeZone

// Draft.kt - share -> draft -> POST /tasks body, ported from the Chrome extension's
// core.js (v2.0.0). draftFromPage became draftFromShare: the input is the shared
// text (share/SharedTextParser), not the page's DOM. quickAdd has no counterpart
// (計画書 D5: the share sheet always opens the form).

/** What a share gave us (share/SharedTextParser). */
data class SharedText(
    /** The first http(s) URL in EXTRA_TEXT. */
    val url: String,
    /** The product / page name with the shop's decorations removed. May be empty. */
    val name: String,
    /** The shop, by the URL's host (for a short URL, the shop that shortened it). */
    val site: SiteAdapter?,
    /** true for a shop's short URL (https://amzn.asia/d/…) that must be resolved to find the code. */
    val isShortUrl: Boolean = false,
)

data class Draft(
    val site: SiteAdapter?,
    val code: String?,
    val fullTitle: String,
    val name: String,
    val url: String,
    val dateIso: String,
)

/** What the form can change. null = keep the draft's value / leave out of the request. */
data class DraftForm(
    val name: String? = null,
    val price: String? = null,
    val memo: String? = null,
    val projectId: String? = null,
    val sectionId: String? = null,
    val labels: List<String>? = null,
    val priority: Int? = null,
)

// [resolvedUrl] is where a short URL led (share/ShortUrlResolver), or null. With a
// code the URL becomes the shop's canonical one; without one (a page no adapter
// claims, or a short URL that could not be resolved = D8) it is the shared URL
// without its query.
fun draftFromShare(
    share: SharedText,
    resolvedUrl: String? = null,
    settings: Settings = DEFAULT_SETTINGS,
    now: Date = Date(),
    zone: TimeZone = TimeZone.getDefault(),
): Draft {
    val site = share.site
    val code = resolveCode(site, share.url, resolvedUrl)
    val fullTitle = collapse(share.name)
    val limit = settings.titleLimit.takeIf { it > 0 } ?: DEFAULT_TITLE_LIMIT
    return Draft(
        site = site,
        code = code,
        fullTitle = fullTitle,
        name = shorten(stripPromo(fullTitle), limit),
        url = if (site != null && code != null) site.canonicalUrl(code) else stripQuery(share.url),
        dateIso = todayIso(now, zone),
    )
}

// Draft + (possibly edited) form values -> POST /tasks body.
fun buildTaskFromDraft(draft: Draft, form: DraftForm = DraftForm()): TaskPayload {
    val (content, linkDropped) = buildContent(name = form.name ?: draft.name, url = draft.url)
    val description = buildDescription(
        memo = form.memo,
        code = draft.code,
        codeLabel = draft.site?.codeLabel,
        sourceLabel = draft.site?.sourceLabel,
        price = cleanPrice(form.price),
        fullTitle = draft.fullTitle,
        url = draft.url,
        dateIso = draft.dateIso,
        linkDropped = linkDropped,
    )
    return buildTaskPayload(
        content = content,
        description = description,
        projectId = form.projectId,
        sectionId = form.sectionId,
        labels = form.labels,
        priority = form.priority,
    )
}

// Existing active task that already carries this product code, or null.
// Todoist "search:" also looks inside the description, so the code recorded in
// 【ASIN】/【商品コード】 is found even when the task name is a nickname.
suspend fun findDuplicate(client: TaskSearcher, code: String?): TodoistTask? {
    if (code.isNullOrEmpty()) return null
    return client.searchTasks(code).firstOrNull { "${it.content}\n${it.description}".contains(code) }
}
