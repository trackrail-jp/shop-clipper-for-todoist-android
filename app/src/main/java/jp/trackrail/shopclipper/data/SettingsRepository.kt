package jp.trackrail.shopclipper.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import jp.trackrail.shopclipper.core.SettingsStore
import jp.trackrail.shopclipper.core.collapse
import kotlinx.coroutines.flow.first

// The settings live in a Preferences DataStore; the token lives there too, but
// only as the cipher text KeystoreTokenCipher produced (計画書 §7).
// ⚠ DataStore is excluded from backup (res/xml/backup_rules.xml and
// data_extraction_rules.xml): restored on another device the cipher text could
// not be read anyway, and the token should not travel.
// The values are the ones core.normalizeSettings understands: a missing key
// means "not set" (so an explicit empty sectionId still means「セクションなし」),
// and labels are kept as one "a, b" string, which parseLabels splits.
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) : SettingsStore {

    override suspend fun read(): Map<String, Any?> = fromPreferences(dataStore.data.first())

    override suspend fun write(values: Map<String, Any?>) {
        dataStore.edit { prefs -> toPreferences(values, prefs) }
    }

    internal fun fromPreferences(prefs: Preferences): Map<String, Any?> {
        val out = linkedMapOf<String, Any?>()
        // A token that cannot be decrypted is left out = "not set" (R6).
        val stored = prefs[Keys.TOKEN]
        val token = if (stored == null) null else cipher.decrypt(stored)
        if (token != null) out["token"] = token
        prefs[Keys.PROJECT_ID]?.let { out["projectId"] = it }
        prefs[Keys.SECTION_ID]?.let { out["sectionId"] = it }
        prefs[Keys.LABELS]?.let { out["labels"] = it }
        prefs[Keys.SITE_LABELS]?.let { out["siteLabels"] = it }
        prefs[Keys.PRIORITY]?.let { out["priority"] = it }
        prefs[Keys.TITLE_LIMIT]?.let { out["titleLimit"] = it }
        prefs[Keys.TARGET_LABEL]?.let { out["targetLabel"] = it }
        prefs[Keys.VERSION]?.let { out["settingsVersion"] = it }
        return out
    }

    // [values] comes from core.Settings.toMap(), so the numbers are already
    // clamped and the labels are already a list.
    internal fun toPreferences(values: Map<String, Any?>, prefs: MutablePreferences) {
        if (values.containsKey("token")) {
            val token = collapse(values["token"]?.toString())
            val encrypted = if (token.isEmpty()) null else cipher.encrypt(token)
            if (encrypted == null) prefs.remove(Keys.TOKEN) else prefs[Keys.TOKEN] = encrypted
        }
        putText(prefs, values, "projectId", Keys.PROJECT_ID)
        putText(prefs, values, "sectionId", Keys.SECTION_ID)
        putText(prefs, values, "labels", Keys.LABELS)
        putText(prefs, values, "targetLabel", Keys.TARGET_LABEL)
        (values["siteLabels"] as? Boolean)?.let { prefs[Keys.SITE_LABELS] = it }
        (values["priority"] as? Number)?.let { prefs[Keys.PRIORITY] = it.toInt() }
        (values["titleLimit"] as? Number)?.let { prefs[Keys.TITLE_LIMIT] = it.toInt() }
        (values["settingsVersion"] as? Number)?.let { prefs[Keys.VERSION] = it.toInt() }
    }

    private fun putText(prefs: MutablePreferences, values: Map<String, Any?>, name: String, key: Preferences.Key<String>) {
        val value = values[name] ?: return
        prefs[key] = if (value is Collection<*>) value.joinToString(", ") else value.toString()
    }

    private object Keys {
        // The token is stored encrypted; the name says so, so nobody reads it as plain text.
        val TOKEN = stringPreferencesKey("tokenCipher")
        val PROJECT_ID = stringPreferencesKey("projectId")
        val SECTION_ID = stringPreferencesKey("sectionId")
        val LABELS = stringPreferencesKey("labels")
        val SITE_LABELS = booleanPreferencesKey("siteLabels")
        val PRIORITY = intPreferencesKey("priority")
        val TITLE_LIMIT = intPreferencesKey("titleLimit")
        val TARGET_LABEL = stringPreferencesKey("targetLabel")
        val VERSION = intPreferencesKey("settingsVersion")
    }
}
