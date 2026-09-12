package com.curio.app.data.supabase

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.KeyGenerator
import java.security.SecureRandom

/** End-to-end DM crypto. Only ciphertext and wrapped conversation keys cross the API. */
data class CurioEncryptedMessage(val ciphertext: String, val nonce: String, val version: String = CurioDmCrypto.VERSION)
data class CurioDmIdentity(val deviceId: String, val publicKey: String, val userId: String = "")
data class CurioDmEnvelope(val deviceId: String, val keyVersion: Int, val encryptedKey: String, val version: String)

object CurioDmCrypto {
    const val VERSION = "curio-dm-aesgcm-v1"
    const val ENVELOPE_VERSION = "curio-dm-rsa-oaep-v1"
    private const val KEY_PREFIX = "dm-conversation-key-"
    private const val KEY_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    fun identity(context: Context): CurioDmIdentity {
        return runCatching {
            val pair = CurioSecureStore.identityKeyPair()
            CurioDmIdentity(
                CurioSecureStore.ensureDeviceId(context),
                b64(pair.public.encoded)
            )
        }.getOrElse {
            CurioSecureStore.resetDmStorage(context)
            val pair = CurioSecureStore.identityKeyPair()
            CurioDmIdentity(
                CurioSecureStore.ensureDeviceId(context),
                b64(pair.public.encoded)
            )
        }
    }

    fun wrapConversationKey(conversationKey: ByteArray, recipient: CurioDmIdentity, keyVersion: Int = 1): CurioDmEnvelope {
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(Base64.decode(recipient.publicKey, Base64.NO_WRAP))
        )
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return CurioDmEnvelope(recipient.deviceId, keyVersion, b64(cipher.doFinal(conversationKey)), ENVELOPE_VERSION)
    }

    fun unwrapConversationKey(envelope: CurioDmEnvelope): ByteArray {
        require(envelope.version == ENVELOPE_VERSION) { "Unsupported key envelope version." }
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, CurioSecureStore.identityKeyPair().private)
        return cipher.doFinal(Base64.decode(envelope.encryptedKey, Base64.NO_WRAP))
    }

    fun hasConversationKey(context: Context, conversationId: String): Boolean =
        CurioSecureStore.get(context, KEY_PREFIX + conversationId) != null

    fun ensureConversationKey(context: Context, conversationId: String, envelope: CurioDmEnvelope? = null): ByteArray {
        if (envelope != null) {
            val key = unwrapConversationKey(envelope)
            check(key.size == KEY_BYTES) { "Invalid conversation key." }
  val stored = CurioSecureStore.put(context, KEY_PREFIX + conversationId, b64(key))
  if (!stored) {
  CurioSecureStore.resetDmStorage(context)
  check(CurioSecureStore.put(context, KEY_PREFIX + conversationId, b64(key))) { "Secure storage unavailable." }
  }
  return key
        }
        CurioSecureStore.get(context, KEY_PREFIX + conversationId)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val key = ByteArray(KEY_BYTES).also(SecureRandom()::nextBytes)
        check(CurioSecureStore.put(context, KEY_PREFIX + conversationId, b64(key))) { "Secure storage unavailable." }
        return key
    }

    fun conversationKey(context: Context, conversationId: String): ByteArray = ensureConversationKey(context, conversationId)

    fun encrypt(context: Context, conversationId: String, plaintext: String): CurioEncryptedMessage {
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(ensureConversationKey(context, conversationId), "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
        return CurioEncryptedMessage(b64(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))), b64(nonce))
    }

    fun decrypt(context: Context, conversationId: String, encrypted: CurioEncryptedMessage): String {
        require(encrypted.version == VERSION) { "Unsupported message encryption version." }
        val nonce = Base64.decode(encrypted.nonce, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(ensureConversationKey(context, conversationId), "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(Base64.decode(encrypted.ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}

internal fun dmConversationId(first: String, second: String): String = listOf(first, second).sorted().joinToString(":")
