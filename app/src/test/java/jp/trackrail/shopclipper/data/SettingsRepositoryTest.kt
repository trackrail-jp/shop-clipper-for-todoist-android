package jp.trackrail.shopclipper.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import jp.trackrail.shopclipper.FakeTokenCipher
import jp.trackrail.shopclipper.core.Settings
import jp.trackrail.shopclipper.core.normalizeSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// The token never reaches the store in the clear, and a token that cannot be
// decrypted is treated as "not set" (計画書 R6).
class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val saved = Settings(
        token = "tok-123",
        projectId = "P1",
        sectionId = "S1",
        labels = listOf("Shopping_Amazon", "価格待ち"),
        siteLabels = true,
        priority = 4,
        titleLimit = 40,
        targetLabel = "🛍 買い物 / 🛒 ほしいもの",
    )

    private fun repository(cipher: TokenCipher = FakeTokenCipher()) =
        SettingsRepository(PreferenceDataStoreFactory.create { File(folder.newFolder(), "settings.preferences_pb") }, cipher)

    @Test
    fun settingsSurviveARoundTripAndTheTokenIsStoredEncrypted() = runTest {
        val repository = repository()
        repository.write(saved.toMap())
        val read = repository.read()
        assertEquals(saved, normalizeSettings(read))
        assertEquals("tok-123", read["token"])
        // Labels are one string; parseLabels splits it again.
        assertEquals("Shopping_Amazon, 価格待ち", read["labels"])
        // Nothing in the file is the token itself.
        val prefs = mutablePreferencesOf()
        repository.toPreferences(saved.toMap(), prefs)
        assertEquals("${FakeTokenCipher.PREFIX}tok-123", prefs[stringPreferencesKey("tokenCipher")])
        assertFalse(prefs.asMap().values.any { it == "tok-123" })
    }

    @Test
    fun anEmptyStoreReadsAsTheDefaults() = runTest {
        assertEquals(emptyMap<String, Any?>(), repository().read())
        assertEquals(Settings(), normalizeSettings(repository().read()))
    }

    @Test
    fun aTokenThatCannotBeReadCountsAsNotSet() {
        val prefs = mutablePreferencesOf(stringPreferencesKey("tokenCipher") to "not-encrypted-by-us")
        val values = repository().fromPreferences(prefs)
        assertFalse("token" in values)
        assertEquals("", normalizeSettings(values).token)
    }

    @Test
    fun anEmptyTokenOrACipherThatFailsRemovesTheStoredToken() {
        val prefs = mutablePreferencesOf(stringPreferencesKey("tokenCipher") to "${FakeTokenCipher.PREFIX}old")
        repository().toPreferences(mapOf("token" to "  "), prefs)
        assertFalse(stringPreferencesKey("tokenCipher") in prefs.asMap())
        val broken = mutablePreferencesOf(stringPreferencesKey("tokenCipher") to "${FakeTokenCipher.PREFIX}old")
        repository(FakeTokenCipher(failing = true)).toPreferences(mapOf("token" to "tok"), broken)
        assertFalse(stringPreferencesKey("tokenCipher") in broken.asMap())
    }

    @Test
    fun aTokenGivenAsNullIsTreatedAsEmpty() {
        val prefs = mutablePreferencesOf(stringPreferencesKey("tokenCipher") to "${FakeTokenCipher.PREFIX}old")
        repository().toPreferences(mapOf("token" to null), prefs)
        assertFalse(stringPreferencesKey("tokenCipher") in prefs.asMap())
    }

    @Test
    fun onlyTheKeysThatWereGivenAreWritten() {
        val prefs = mutablePreferencesOf()
        repository().toPreferences(mapOf("projectId" to "P", "priority" to 3, "siteLabels" to true), prefs)
        assertEquals(3, prefs.asMap().size)
        // Values of the wrong type are left out rather than crashing the save.
        repository().toPreferences(mapOf("priority" to "3", "titleLimit" to null), prefs)
        assertEquals(3, prefs.asMap().size)
        assertTrue(prefs.asMap().keys.map { it.name }.containsAll(listOf("projectId", "priority", "siteLabels")))
    }

    @Test
    fun aSavedSectionIdStaysEmptyWhenTheUserChoseNoSection() = runTest {
        val repository = repository()
        repository.write(Settings(projectId = "P1", sectionId = "").toMap())
        val read: Map<String, Any?> = repository.read()
        assertEquals("", read["sectionId"])
        assertEquals("", normalizeSettings(read).sectionId)
        val prefs: MutablePreferences = mutablePreferencesOf()
        repository.toPreferences(mapOf("sectionId" to ""), prefs)
        assertEquals("", prefs[stringPreferencesKey("sectionId")])
    }
}
