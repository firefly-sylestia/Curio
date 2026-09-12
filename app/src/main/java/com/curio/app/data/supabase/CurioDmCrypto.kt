package com.curio.app.data.supabase

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** End-to-end DM crypto. A message permanently records the conversation-key version it used. */
data class CurioEncryptedMessage(val ciphertext: String, val nonce: String, val version: String)
data class CurioDmIdentity(val deviceId: String, val publicKey: String, val userId: String = "")
data class CurioDmEnvelope(val deviceId: String, val keyVersion: Int, val encryptedKey: String, val version: String)

object CurioDmCrypto {
    const val VERSION = "curio-dm-aesgcm-v1"
    const val ENVELOPE_VERSION = "curio-dm-rsa-oaep-v1"
    private const val KEY_PREFIX = "dm-conversation-key-"
    private const val KEY_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    fun identity(context: Context): CurioDmIdentity = runCatching {
        val pair = CurioSecureStore.identityKeyPair()
        CurioDmIdentity(CurioSecureStore.ensureDeviceId(context), b64(pair.public.encoded))
    }.getOrElse {
        CurioSecureStore.resetDmStorage(context)
        val pair = CurioSecureStore.identityKeyPair()
        CurioDmIdentity(CurioSecureStore.ensureDeviceId(context), b64(pair.public.encoded))
    }

    fun wrapConversationKey(key: ByteArray, recipient: CurioDmIdentity, keyVersion: Int): CurioDmEnvelope {
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.decode(recipient.publicKey, Base64.NO_WRAP)))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return CurioDmEnvelope(recipient.deviceId, keyVersion, b64(cipher.doFinal(key)), ENVELOPE_VERSION)
    }

    fun unwrapConversationKey(envelope: CurioDmEnvelope): ByteArray {
        require(envelope.version == ENVELOPE_VERSION) { "Unsupported key envelope version." }
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, CurioSecureStore.identityKeyPair().private)
        return cipher.doFinal(Base64.decode(envelope.encryptedKey, Base64.NO_WRAP))
    }

    private fun keyName(conversationId: String, keyVersion: Int) = "$KEY_PREFIX$conversationId-$keyVersion"
    private fun stored(context: Context, conversationId: String, keyVersion: Int): ByteArray? =
        CurioSecureStore.get(context, keyName(conversationId, keyVersion))
            ?.let { Base64.decode(it, Base64.NO_WRAP) }
            // v1 data stored before versioned keys remains readable when its old
            // envelope/key version is one.
            ?: if (keyVersion == 1) CurioSecureStore.get(context, KEY_PREFIX + conversationId)
                ?.let { Base64.decode(it, Base64.NO_WRAP) } else null

    fun installEnvelope(context: Context, conversationId: String, envelope: CurioDmEnvelope): ByteArray {
        val key = unwrapConversationKey(envelope)
        check(key.size == KEY_BYTES) { "Invalid conversation key." }
        check(CurioSecureStore.put(context, keyName(conversationId, envelope.keyVersion), b64(key))) { "Secure storage unavailable." }
        return key
    }

    fun existingKey(context: Context, conversationId: String, keyVersion: Int): ByteArray? =
        stored(context, conversationId, keyVersion)

    fun newKey(context: Context, conversationId: String, keyVersion: Int): ByteArray {
        val key = ByteArray(KEY_BYTES).also(SecureRandom()::nextBytes)
        check(CurioSecureStore.put(context, keyName(conversationId, keyVersion), b64(key))) { "Secure storage unavailable." }
        return key
    }

    fun encrypt(context: Context, conversationId: String, key: ByteArray, keyVersion: Int, plaintext: String): CurioEncryptedMessage {
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
        return CurioEncryptedMessage(b64(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))), b64(nonce), "$VERSION:$keyVersion")
    }

    fun decrypt(context: Context, conversationId: String, encrypted: CurioEncryptedMessage): String {
        val version = encrypted.version.removePrefix("$VERSION:").toIntOrNull()
            ?: if (encrypted.version == VERSION) 1 else throw IllegalArgumentException("Unsupported message encryption version.")
        val key = stored(context, conversationId, version) ?: throw IllegalStateException("This device does not have this message's key.")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, Base64.decode(encrypted.nonce, Base64.NO_WRAP)))
        cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(Base64.decode(encrypted.ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
}

internal fun dmConversationId(first: String, second: String): String = listOf(first, second).sorted().joinToString(":")
