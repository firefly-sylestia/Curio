package com.curio.app.data.supabase

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Local-only AES-GCM envelope for the DM transport. Plaintext never crosses SocialApi. */
data class CurioEncryptedMessage(
    val ciphertext: String,
    val nonce: String,
    val version: String = VERSION
)

object CurioDmCrypto {
    const val VERSION = "curio-dm-aesgcm-v1"
    private const val KEY_PREFIX = "dm-conversation-key-"
    private const val KEY_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    fun encrypt(context: Context, conversationId: String, plaintext: String): CurioEncryptedMessage {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        cipher.init(Cipher.ENCRYPT_MODE, key(context, conversationId), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
        return CurioEncryptedMessage(
            ciphertext = b64(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))),
            nonce = b64(nonce)
        )
    }

    fun decrypt(context: Context, conversationId: String, encrypted: CurioEncryptedMessage): String {
        require(encrypted.version == VERSION) { "Unsupported message encryption version." }
        val nonce = Base64.decode(encrypted.nonce, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(context, conversationId), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(Base64.decode(encrypted.ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun key(context: Context, conversationId: String): SecretKey {
        val name = KEY_PREFIX + conversationId
        val stored = CurioSecureStore.get(context, name)
        if (stored != null) return SecretKeySpec(Base64.decode(stored, Base64.NO_WRAP), "AES")
        val generated = ByteArray(KEY_BYTES).also(SecureRandom()::nextBytes)
        check(CurioSecureStore.put(context, name, b64(generated))) { "Secure storage unavailable." }
        return SecretKeySpec(generated, "AES")
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}

internal fun dmConversationId(first: String, second: String): String =
    listOf(first, second).sorted().joinToString(":")
