package io.github.kloeggaa.tickoff.data

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against the real Android Keystore. */
@RunWith(AndroidJUnit4::class)
class ApiKeyCipherDeviceTest {

    private val key = "sk-test-0123456789abcdefghijklmnopqrstuvwxyz"

    @Test
    fun roundTrip() {
        val stored = ApiKeyCipher.encrypt(key)
        assertFalse("plain key must not appear in the stored value", stored.contains("sk-test"))
        assertEquals(key, ApiKeyCipher.decrypt(stored))
    }

    @Test
    fun freshIvEachTime() {
        assertNotEquals(ApiKeyCipher.encrypt(key), ApiKeyCipher.encrypt(key))
    }

    @Test
    fun unreadableValuesGiveNull() {
        // A plaintext key from before encryption, garbage, and a tampered ciphertext.
        assertNull(ApiKeyCipher.decrypt(key))
        assertNull(ApiKeyCipher.decrypt(""))
        val bytes = Base64.decode(ApiKeyCipher.encrypt(key), Base64.NO_WRAP)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()
        assertNull(ApiKeyCipher.decrypt(Base64.encodeToString(bytes, Base64.NO_WRAP)))
    }
}
