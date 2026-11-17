package com.onfaa.tunnel.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM envelope for server credentials stored in Room.
 *
 * The key never leaves the Android Keystore (hardware-backed when available);
 * only the ciphertext is written to the database. A failure to obtain the key
 * is reported to the caller so a profile is never silently stored in clear
 * text.
 */
object SecretCipher {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "onfaa_server_payload_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128
    private const val IV_BYTES = 12

    class SecretUnavailable(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun isAvailable(): Boolean = try {
        keyOrCreate()
        true
    } catch (_: Throwable) {
        false
    }

    /** Returns base64(iv || ciphertext). Throws [SecretUnavailable] on failure. */
    fun encrypt(plainText: String): String = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyOrCreate())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(cipherText, 0, payload, iv.size, cipherText.size)
        Base64.encodeToString(payload, Base64.NO_WRAP)
    } catch (t: Throwable) {
        throw SecretUnavailable("Chiffrement local indisponible", t)
    }

    fun decrypt(payload: String): String = try {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        if (bytes.size <= IV_BYTES) throw SecretUnavailable("Charge chiffrée invalide")
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipherText = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyOrCreate(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    } catch (t: SecretUnavailable) {
        throw t
    } catch (t: Throwable) {
        throw SecretUnavailable("Déchiffrement local impossible", t)
    }

    @Synchronized
    private fun keyOrCreate(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val entry = store.getEntry(ALIAS, null)
        if (entry is KeyStore.SecretKeyEntry) return entry.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
