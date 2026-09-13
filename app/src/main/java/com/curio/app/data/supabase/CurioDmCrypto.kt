package com.curio.app.data.supabase

import android.content.Context
import android.util.Log
import java.util.Base64
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
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
    private val OAEP_SPEC = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        PSource.PSpecified.DEFAULT
    )

    fun identity(context: Context): CurioDmIdentity = runCatching {
        val pair = CurioSecureStore.identityKeyPair()
        val publicKey = b64(pair.public.encoded)
        CurioDmIdentity(CurioSecureStore.ensureDmIdentityBinding(context, publicKey), publicKey)
    }.getOrElse { failure ->
        DmCryptoDiagnostics.failure("identity", null, null, null, failure)
        throw IllegalStateException("This device's encrypted-message identity is unavailable.", failure)
    }

    fun wrapConversationKey(key: ByteArray, recipient: CurioDmIdentity, keyVersion: Int): CurioDmEnvelope {
        require(key.size == KEY_BYTES) { "Invalid conversation key length." }
        require(recipient.deviceId.isNotBlank() && recipient.publicKey.isNotBlank()) { "Invalid recipient device identity." }
        require(keyVersion > 0) { "Invalid conversation key version." }
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(b64decode(recipient.publicKey)))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SPEC)
        return CurioDmEnvelope(recipient.deviceId, keyVersion, b64(cipher.doFinal(key)), ENVELOPE_VERSION)
    }

    /** Returns whether a stored public identity is usable for a new envelope. */
    fun canWrapFor(recipient: CurioDmIdentity): Boolean = runCatching {
        KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(b64decode(recipient.publicKey))
        )
    }.isSuccess

    fun unwrapConversationKey(envelope: CurioDmEnvelope): ByteArray {
        return unwrapConversationKey(envelope, CurioSecureStore.identityKeyPair().private)
    }

    internal fun unwrapConversationKey(envelope: CurioDmEnvelope, privateKey: java.security.PrivateKey): ByteArray {
        require(envelope.version == ENVELOPE_VERSION) { "Unsupported key envelope version." }
        require(envelope.keyVersion > 0) { "Invalid conversation key version." }
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SPEC)
        return cipher.doFinal(b64decode(envelope.encryptedKey))
    }

    private fun keyName(conversationId: String, keyVersion: Int) = "$KEY_PREFIX$conversationId-$keyVersion"
    private fun stored(context: Context, conversationId: String, keyVersion: Int): ByteArray? =
        CurioSecureStore.get(context, keyName(conversationId, keyVersion))
            ?.let(::b64decode)
            // v1 data stored before versioned keys remains readable when its old
            // envelope/key version is one.
            ?: if (keyVersion == 1) CurioSecureStore.get(context, KEY_PREFIX + conversationId)
                ?.let(::b64decode) else null

    fun installEnvelope(context: Context, conversationId: String, envelope: CurioDmEnvelope): ByteArray {
        val identity = identity(context)
        require(envelope.deviceId == identity.deviceId) { "Envelope belongs to a different device." }
        val key = unwrapConversationKey(envelope)
        check(key.size == KEY_BYTES) { "Invalid conversation key." }
        check(CurioSecureStore.put(context, keyName(conversationId, envelope.keyVersion), b64(key))) { "Secure storage unavailable." }
        return key
    }

    fun existingKey(context: Context, conversationId: String, keyVersion: Int): ByteArray? =
        stored(context, conversationId, keyVersion)

    /** The envelope version required to read an encrypted message, or null for an unsupported payload. */
    fun messageKeyVersion(encryptionVersion: String): Int? {
        if (encryptionVersion == VERSION) return 1
        val prefix = "$VERSION:"
        val value = encryptionVersion.removePrefix(prefix)
        val version = value.toIntOrNull()
        return version?.takeIf { it > 0 && value == it.toString() }
    }

    fun newKey(context: Context, conversationId: String, keyVersion: Int): ByteArray {
        val key = ByteArray(KEY_BYTES).also(SecureRandom()::nextBytes)
        check(CurioSecureStore.put(context, keyName(conversationId, keyVersion), b64(key))) { "Secure storage unavailable." }
        return key
    }

    fun encrypt(context: Context, conversationId: String, key: ByteArray, keyVersion: Int, plaintext: String): CurioEncryptedMessage {
        return encryptWithKey(conversationId, key, keyVersion, plaintext)
    }

    internal fun encryptWithKey(conversationId: String, key: ByteArray, keyVersion: Int, plaintext: String): CurioEncryptedMessage {
        require(key.size == KEY_BYTES) { "Invalid conversation key length." }
        require(keyVersion > 0) { "Invalid conversation key version." }
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
        return CurioEncryptedMessage(b64(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))), b64(nonce), "$VERSION:$keyVersion")
    }

    fun decrypt(context: Context, conversationId: String, encrypted: CurioEncryptedMessage): String {
        val version = messageKeyVersion(encrypted.version)
            ?: throw IllegalArgumentException("Unsupported message encryption version.")
        val key = stored(context, conversationId, version) ?: throw IllegalStateException("This device does not have this message's key.")
        return decryptWithKey(conversationId, key, encrypted)
    }

    internal fun decryptWithKey(conversationId: String, key: ByteArray, encrypted: CurioEncryptedMessage): String {
        require(key.size == KEY_BYTES) { "Invalid stored conversation key length." }
        val nonce = b64decode(encrypted.nonce)
        require(nonce.size == NONCE_BYTES) { "Invalid AES-GCM nonce length." }
        val ciphertext = b64decode(encrypted.ciphertext)
        require(ciphertext.size >= TAG_BITS / 8) { "Invalid AES-GCM ciphertext length." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun b64(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
    private fun b64decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}

/** Redacted, structural diagnostics for encrypted-DM recovery; never logs secrets or message content. */
internal object DmCryptoDiagnostics {
    private const val TAG = "CurioDmCrypto"

    fun event(
        stage: String,
        conversationId: String?,
        messageId: String? = null,
        keyVersion: Int? = null,
        deviceId: String? = null,
        detail: String
    ) = Log.i(TAG, "stage=$stage conversation=${redact(conversationId)} message=${redact(messageId)} keyVersion=$keyVersion device=${redact(deviceId)} $detail")

    fun failure(
        stage: String,
        conversationId: String?,
        messageId: String?,
        keyVersion: Int?,
        failure: Throwable,
        detail: String = ""
    ) = Log.w(TAG, "stage=$stage conversation=${redact(conversationId)} message=${redact(messageId)} keyVersion=$keyVersion $detail exception=${failure.javaClass.simpleName}")

    private fun redact(value: String?): String = value?.let { Integer.toHexString(it.hashCode()) } ?: "-"
}

internal fun dmConversationId(first: String, second: String): String = listOf(first, second).sorted().joinToString(":")
