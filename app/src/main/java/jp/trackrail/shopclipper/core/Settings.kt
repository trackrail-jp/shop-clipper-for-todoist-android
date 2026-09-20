package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.core.sites.SiteAdapter

// Settings.kt - the settings model and its migration, ported from the Chrome
// extension's lib.js / core.js (v2.0.0). Storage itself (DataStore + Keystore)
// lives behind SettingsStore, so everything here is unit-testable.

const val DEFAULT_TARGET_LABEL = "インボックス"
const val DEFAULT_PRIORITY = 1 // API scale: 1 = p4 (lowest) .. 4 = p1 (計画書 R3)
const val DEFAULT_TITLE_LIMIT = 60 // roughly what Amazon's search results show before cutting

// The Android app starts its own numbering at 1. Bump it whenever a default
// below changes, and record the outgoing value in LEGACY_DEFAULTS so saved
// settings follow it (migrateSettings). The extension learned this the hard way:
// a saved value always beats the default, so a raised default did nothing.
const val SETTINGS_VERSION = 1
val LEGACY_DEFAULTS: Map<Int, Map<String, Any?>> = emptyMap()

data class Settings(
    val token: String = "",
    // Empty = no project_id / section_id in the request, so Todoist files the
    // task in the Inbox.
    val projectId: String = "",
    val sectionId: String = "",
    // Extra labels added to every task.
    val labels: List<String> = emptyList(),
    // Also add the site's own label (Shopping_Amazon / Shopping_ヨドバシ). Opt-in.
    val siteLabels: Boolean = false,
    val priority: Int = DEFAULT_PRIORITY,
    val titleLimit: Int = DEFAULT_TITLE_LIMIT,
    val targetLabel: String = DEFAULT_TARGET_LABEL,
    val settingsVersion: Int = SETTINGS_VERSION,
) {
    fun toMap(): Map<String, Any?> = linkedMapOf(
        "token" to token,
        "projectId" to projectId,
        "sectionId" to sectionId,
        "labels" to labels,
        "siteLabels" to siteLabels,
        "priority" to priority,
        "titleLimit" to titleLimit,
        "targetLabel" to targetLabel,
        "settingsVersion" to settingsVersion,
    )

    // ⛔ Never print the API token (logs, crash reports, test failures).
    override fun toString(): String =
        "Settings(token=${if (token.isEmpty()) "" else "***"}, projectId=$projectId, sectionId=$sectionId, " +
            "labels=$labels, siteLabels=$siteLabels, priority=$priority, titleLimit=$titleLimit, " +
            "targetLabel=$targetLabel, settingsVersion=$settingsVersion)"
}

val DEFAULT_SETTINGS = Settings()
val SETTINGS_KEYS: List<String> = DEFAULT_SETTINGS.toMap().keys.toList()

/** Raw values from storage (or a form) -> Settings with defaults filled in and numbers clamped. */
fun normalizeSettings(raw: Map<String, Any?> = emptyMap()): Settings = Settings(
    token = collapse(raw["token"]?.toString()),
    projectId = collapse(raw["projectId"]?.toString()).ifEmpty { DEFAULT_SETTINGS.projectId },
    // An explicit "" means "no section"; only a missing value falls back.
    sectionId = if ("sectionId" in raw) collapse(raw["sectionId"]?.toString()) else DEFAULT_SETTINGS.sectionId,
    labels = if ("labels" in raw) parseLabels(raw["labels"]) else DEFAULT_SETTINGS.labels,
    siteLabels = if ("siteLabels" in raw) raw["siteLabels"] == true else DEFAULT_SETTINGS.siteLabels,
    priority = clampInt(raw["priority"], 1, 4, DEFAULT_PRIORITY),
    titleLimit = clampInt(raw["titleLimit"], 10, 200, DEFAULT_TITLE_LIMIT),
    targetLabel = collapse(raw["targetLabel"]?.toString()).ifEmpty { DEFAULT_TARGET_LABEL },
    settingsVersion = clampInt(raw["settingsVersion"], 1, SETTINGS_VERSION, SETTINGS_VERSION),
)

data class Migration(val settings: Map<String, Any?>, val migrated: Boolean)

// JavaScript's String(value) for the values settings hold (lists join with ",").
private fun asText(value: Any?): String = if (value is Collection<*>) value.joinToString(",") else value.toString()

/**
 * Values still equal to the default they shipped with follow the new default;
 * anything the user chose is left alone. [version], [legacyDefaults] and
 * [defaults] are parameters so the mechanism can be tested before the first
 * real migration exists (the Android app is still on version 1).
 */
fun migrateSettings(
    raw: Map<String, Any?> = emptyMap(),
    version: Int = SETTINGS_VERSION,
    legacyDefaults: Map<Int, Map<String, Any?>> = LEGACY_DEFAULTS,
    defaults: Map<String, Any?> = DEFAULT_SETTINGS.toMap(),
): Migration {
    val from = clampInt(raw["settingsVersion"], 1, version, 1)
    if (from >= version) return Migration(raw, migrated = false)
    val settings = raw.toMutableMap().apply { put("settingsVersion", version) }
    val shipped = legacyDefaults.filterKeys { it >= from }.toSortedMap().values.fold(emptyMap<String, Any?>()) { acc, m -> acc + m }
    for ((key, old) in shipped) {
        if (key in settings && asText(settings[key]) == asText(old)) settings[key] = defaults[key]
    }
    return Migration(settings, migrated = true)
}

/** Where settings are kept. The DataStore + Keystore implementation comes with the settings screen (I4). */
interface SettingsStore {
    suspend fun read(): Map<String, Any?>
    suspend fun write(values: Map<String, Any?>)
}

suspend fun loadSettings(
    store: SettingsStore,
    migrate: (Map<String, Any?>) -> Migration = { migrateSettings(it) },
): Settings {
    val raw = store.read()
    val (settings, migrated) = migrate(raw)
    val normalized = normalizeSettings(settings)
    // Write the migration back so the settings screen and the next load agree.
    // Nothing saved yet -> the defaults already apply, so leave storage empty.
    if (migrated && raw.isNotEmpty()) store.write(normalized.toMap())
    return normalized
}

suspend fun saveSettings(store: SettingsStore, patch: Map<String, Any?>): Settings {
    val next = normalizeSettings(patch)
    store.write(next.toMap())
    return next
}

// The labels a task gets: the site's own label first when settings.siteLabels is
// on (Amazon -> Shopping_Amazon, ヨドバシ -> Shopping_ヨドバシ), plus the extra labels.
fun labelsFor(site: SiteAdapter?, settings: Settings = DEFAULT_SETTINGS): List<String> {
    val own = if (settings.siteLabels && site != null) listOf(site.label) else emptyList()
    return parseLabels(own + settings.labels)
}
