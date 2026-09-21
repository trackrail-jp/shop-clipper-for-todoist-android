package jp.trackrail.shopclipper.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// AES-256-GCM with a key the Android Keystore holds: the key itself never leaves
// the device, so a backup or a copy of the DataStore file is useless on its own
// (計画書 §7・androidx.security の EncryptedSharedPreferences は全 API 非推奨）.
// ⚠ Kover's C1 does not cover this class (it needs the device's Keystore). It is
// checked on the device: save the token, close the app, open it again (I4).
class KeystoreTokenCipher(private val alias: String = DEFAULT_ALIAS) : TokenCipher {

    override fun encrypt(token: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    } catch (e: GeneralSecurityException) {
        null
    } catch (e: IOException) {
        null
    }

    override fun decrypt(stored: String): String? = try {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        if (bytes.size <= IV_BYTES) {
            null
        } else {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        }
    } catch (e: GeneralSecurityException) {
        null
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        null // Base64 that is not Base64
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_ALIAS = "shopclipper.token"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }
}
