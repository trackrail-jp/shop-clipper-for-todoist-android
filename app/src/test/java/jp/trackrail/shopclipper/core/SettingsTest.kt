package jp.trackrail.shopclipper.core

import jp.trackrail.shopclipper.core.sites.Amazon
import jp.trackrail.shopclipper.core.sites.Yodobashi
import jp.trackrail.shopclipper.todoist.TaskPayload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the extension's test/core.test.js (settings) and lib.test.js (DEFAULTS).
// The Android app starts at settings version 1, so the extension's v1-v3 migration
// cases became tests of the migration *mechanism* with a made-up legacy table
// (計画書 §8).
class SettingsTest {
    private class FakeStore(initial: Map<String, Any?> = emptyMap()) : SettingsStore {
        val data = initial.toMutableMap()
        var writes = 0

        override suspend fun read(): Map<String, Any?> = data.toMap()

        override suspend fun write(values: Map<String, Any?>) {
            writes += 1
            data.putAll(values)
        }
    }

    // A made-up history: v1 cut names at 30, v2 shipped a label, v3 is current.
    private val legacy = mapOf(1 to mapOf("titleLimit" to 30), 2 to mapOf("labels" to listOf("Shopping_Amazon")))
    private fun migrateV3(raw: Map<String, Any?>) = migrateSettings(raw, version = 3, legacyDefaults = legacy)

    @Test
    fun normalizeSettingsFillsDefaultsAndClampsValues() {
        assertEquals(Settings(), normalizeSettings())
        assertEquals(
            Settings(token = "tok", projectId = "P", sectionId = "", labels = listOf("a", "b"), siteLabels = true, priority = 4, titleLimit = 10, targetLabel = "Inbox"),
            normalizeSettings(
                mapOf(
                    "token" to " tok ",
                    "projectId" to "P",
                    "sectionId" to "",
                    "labels" to "a, b",
                    "siteLabels" to true,
                    "priority" to "9",
                    "titleLimit" to "3",
                    "targetLabel" to "Inbox",
                ),
            ),
        )
        assertFalse(normalizeSettings(mapOf("siteLabels" to "yes")).siteLabels) // only a real true turns it on
        assertEquals(200, normalizeSettings(mapOf("titleLimit" to 999)).titleLimit)
        assertEquals(emptyList<String>(), normalizeSettings(mapOf("labels" to "")).labels)
        assertEquals("S", normalizeSettings(mapOf("sectionId" to " S ")).sectionId)
        assertEquals("", normalizeSettings(mapOf("sectionId" to null)).sectionId) // present but empty = no section
        assertEquals(SETTINGS_VERSION, normalizeSettings(mapOf("settingsVersion" to 99)).settingsVersion)
    }

    @Test
    fun loadSettingsAndSaveSettingsRoundTripThroughTheStore() = runTest {
        val store = FakeStore(mapOf("token" to "abc", "projectId" to "P2", "unrelated" to 1))
        val loaded = loadSettings(store)
        assertEquals("abc", loaded.token)
        assertEquals("P2", loaded.projectId)
        assertEquals(0, store.writes) // already current: nothing written back
        val saved = saveSettings(store, loaded.toMap() + mapOf("sectionId" to "", "titleLimit" to 40))
        assertEquals("", store.data["sectionId"])
        assertEquals(40, store.data["titleLimit"])
        assertEquals("", saved.sectionId)
        assertEquals(SETTINGS_KEYS.sorted(), Settings().toMap().keys.sorted())
        assertEquals("", loadSettings(FakeStore()).projectId)
    }

    @Test
    fun migrationMovesOldDefaultsForwardButKeepsValuesTheUserChose() {
        assertEquals(
            Migration(mapOf("token" to "t", "titleLimit" to 60, "labels" to emptyList<String>(), "settingsVersion" to 3), migrated = true),
            migrateV3(mapOf("token" to "t", "titleLimit" to 30, "labels" to listOf("Shopping_Amazon"))),
        )
        assertEquals(60, migrateV3(mapOf("titleLimit" to "30")).settings["titleLimit"]) // stored as text
        assertEquals(45, migrateV3(mapOf("titleLimit" to 45)).settings["titleLimit"]) // the user's own value
        // v2 -> v3 releases the label only; a titleLimit chosen back then is untouched.
        assertEquals(
            mapOf("settingsVersion" to 3, "titleLimit" to 30, "labels" to emptyList<String>()),
            migrateV3(mapOf("settingsVersion" to 2, "titleLimit" to 30, "labels" to listOf("Shopping_Amazon"))).settings,
        )
        val current = mapOf("titleLimit" to 30, "settingsVersion" to 3)
        assertEquals(Migration(current, migrated = false), migrateV3(current))
        assertEquals(Migration(mapOf("settingsVersion" to 3), migrated = true), migrateV3(emptyMap()))
    }

    @Test
    fun aLabelTheUserAddedIsTheirsAndASavedOldDefaultNeverRidesAlong() = runTest {
        // The user added one to the shipped default, so the list is theirs now.
        assertEquals(
            listOf("Shopping_Amazon", "価格待ち"),
            migrateV3(mapOf("settingsVersion" to 2, "labels" to listOf("Shopping_Amazon", "価格待ち"))).settings["labels"],
        )
        // The shipped default itself is released and written back once.
        val store = FakeStore(mapOf("token" to "abc", "labels" to listOf("Shopping_Amazon"), "settingsVersion" to 2))
        val migrated = loadSettings(store) { migrateV3(it) }
        assertEquals(emptyList<String>(), migrated.labels)
        assertEquals(emptyList<String>(), store.data["labels"])
        assertEquals(1, store.writes)
        assertEquals(listOf("Shopping_ヨドバシ"), labelsFor(Yodobashi, migrated.copy(siteLabels = true)))
    }

    @Test
    fun loadSettingsWritesAMigrationBackOnceAndNothingForAFreshInstall() = runTest {
        // A saved old default is replaced and written back ...
        val store = FakeStore(mapOf("token" to "abc", "titleLimit" to 30, "targetLabel" to "X"))
        assertEquals(60, loadSettings(store) { migrateV3(it) }.titleLimit)
        assertEquals(60, store.data["titleLimit"])
        assertEquals(1, store.writes)
        // ... a value chosen after the stamp survives, and nothing is written.
        val chosen = FakeStore(mapOf("titleLimit" to 30, "settingsVersion" to 3))
        assertEquals(30, loadSettings(chosen) { migrateV3(it) }.titleLimit)
        assertEquals(0, chosen.writes)
        // Nothing saved yet: the defaults already apply, so storage stays empty.
        val fresh = FakeStore()
        assertEquals(60, loadSettings(fresh) { migrateV3(it) }.titleLimit)
        assertEquals(0, fresh.writes)
        assertTrue(fresh.data.isEmpty())
    }

    @Test
    fun labelsForAddsTheSiteLabelOnlyWhenSiteLabelsIsOnAndKeepsExtraLabels() {
        // Off by default: a new install adds no label nobody asked for.
        assertEquals(emptyList<String>(), labelsFor(Amazon, normalizeSettings()))
        assertEquals(emptyList<String>(), labelsFor(Yodobashi))
        assertEquals(listOf("価格待ち"), labelsFor(Yodobashi, Settings(labels = listOf("@価格待ち"))))
        val on = Settings(siteLabels = true)
        assertEquals(listOf("Shopping_Amazon"), labelsFor(Amazon, on))
        assertEquals(listOf("Shopping_ヨドバシ"), labelsFor(Yodobashi, on))
        assertEquals(listOf("Shopping_ヨドバシ", "価格待ち"), labelsFor(Yodobashi, Settings(siteLabels = true, labels = listOf("@価格待ち"))))
        assertEquals(listOf("Shopping_Amazon"), labelsFor(Amazon, Settings(siteLabels = true, labels = listOf("Shopping_Amazon")))) // deduped
        assertEquals(listOf("メモ"), labelsFor(null, Settings(siteLabels = true, labels = listOf("メモ")))) // unknown shop
        assertEquals(emptyList<String>(), labelsFor(null))
    }

    @Test
    fun defaultsPointAtTheInboxP4NoLabelsNothingPersonalShips() {
        // Empty ids = no project_id / section_id in the request = Todoist's Inbox.
        val d = DEFAULT_SETTINGS
        assertEquals("", d.projectId)
        assertEquals("", d.sectionId)
        assertEquals("インボックス", d.targetLabel)
        assertEquals(emptyList<String>(), d.labels)
        assertFalse(d.siteLabels)
        assertEquals(1, d.priority)
        assertEquals(60, d.titleLimit)
        assertEquals(TaskPayload(content = "c", priority = 1), buildTaskPayload(content = "c", projectId = d.projectId))
    }

    @Test
    fun everyChangedDefaultIsReleasedThroughLegacyDefaults() {
        // ⛔ The extension's v1.1.1 lesson: a saved value always beats a new default.
        // Every version below the current one must be listed once a default changes.
        assertEquals(1, SETTINGS_VERSION)
        assertEquals(emptyMap<Int, Map<String, Any?>>(), LEGACY_DEFAULTS)
        for (v in 1 until SETTINGS_VERSION) assertTrue("LEGACY_DEFAULTS[$v]", v in LEGACY_DEFAULTS)
        // With nothing to migrate yet, the real migration never rewrites anything.
        assertEquals(Migration(mapOf("titleLimit" to 30), migrated = false), migrateSettings(mapOf("titleLimit" to 30)))
    }

    @Test
    fun toStringNeverShowsTheToken() {
        val text = Settings(token = "secret-token-123").toString()
        assertFalse(text, text.contains("secret-token-123"))
        assertTrue(text, text.startsWith("Settings(token=***, projectId="))
        assertTrue(Settings().toString().startsWith("Settings(token=, "))
    }
}
