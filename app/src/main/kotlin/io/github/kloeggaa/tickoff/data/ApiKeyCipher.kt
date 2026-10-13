package io.github.kloeggaa.tickoff.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the AI API keys before they go into DataStore, with an AES-GCM key that lives in the
 * Android Keystore and can't be exported. A copy of the app's data (root backup tools, a file pulled
 * off the device) is useless without that key, which never leaves this device.
 *
 * Stored value: Base64(12-byte IV + ciphertext + GCM tag).
 */
object ApiKeyCipher {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "tickoff_api_keys"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val bytes = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * The plain key, or null when [stored] can't be decrypted on this device: it was copied from
     * another device or install, the Keystore key is gone, or the value isn't ours.
     */
    fun decrypt(stored: String): String? = try {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val key = existingKey()
        if (key == null || bytes.size <= IV_BYTES) {
            null
        } else {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            }
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        }
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        // Not Base64.
        null
    }

    private fun existingKey(): SecretKey? =
        KeyStore.getInstance(KEYSTORE).apply { load(null) }.getKey(KEY_ALIAS, null) as SecretKey?

    @Synchronized
    private fun getOrCreateKey(): SecretKey = existingKey() ?: KeyGenerator
        .getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        .apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }
        .generateKey()
}
