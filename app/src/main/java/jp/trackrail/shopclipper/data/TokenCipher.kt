package jp.trackrail.shopclipper.data

/**
 * Encrypts the Todoist API token so it is never stored in the clear.
 * Both sides return null instead of throwing: a token that cannot be decrypted
 * (the key is gone after a restore or a device reset) is treated as "not set"
 * (計画書 R6), and the user types it again.
 */
interface TokenCipher {
    /** The stored form of [token], or null when it could not be encrypted. */
    fun encrypt(token: String): String?

    /** The token in [stored], or null when it could not be read. */
    fun decrypt(stored: String): String?
}
