package jp.trackrail.shopclipper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// One DataStore per process for the app's settings. The file is
// <filesDir>/datastore/settings.preferences_pb and is kept out of backups
// (res/xml/backup_rules.xml・data_extraction_rules.xml).
const val SETTINGS_STORE_NAME = "settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_STORE_NAME)

fun settingsRepository(context: Context): SettingsRepository =
    SettingsRepository(context.applicationContext.settingsDataStore, KeystoreTokenCipher())
