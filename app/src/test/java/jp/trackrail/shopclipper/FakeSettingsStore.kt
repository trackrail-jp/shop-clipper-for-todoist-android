package jp.trackrail.shopclipper

import jp.trackrail.shopclipper.core.SettingsStore

/** Settings in memory, with a count of how often they were written. */
class FakeSettingsStore(initial: Map<String, Any?> = emptyMap()) : SettingsStore {
    val data = initial.toMutableMap()
    var writes = 0

    override suspend fun read(): Map<String, Any?> = data.toMap()

    override suspend fun write(values: Map<String, Any?>) {
        writes += 1
        data.putAll(values)
    }
}

/** A cipher that only marks the text, so tests can see what was stored. */
class FakeTokenCipher(private val failing: Boolean = false) : jp.trackrail.shopclipper.data.TokenCipher {
    override fun encrypt(token: String): String? = if (failing) null else "$PREFIX$token"

    override fun decrypt(stored: String): String? = if (stored.startsWith(PREFIX)) stored.removePrefix(PREFIX) else null

    companion object {
        const val PREFIX = "enc:"
    }
}
