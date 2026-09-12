package com.curio.app.data.supabase

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * THE SESSION VAULT — small values sealed with AES/GCM through the Android
 * Keystore.
 *
 * Why it exists: `SupabaseSessionStore` used to keep the access and refresh
 * tokens in plain `SharedPreferences`. On a rooted device, an ADB backup or any
 * process that can read the app's data directory, those strings are a working
 * session — a bearer token that can read the account's messages, cards and
 * profile until it is revoked. A SharedPreferences file is not a secret store.
 *
 * How it works:
 *
 *  - One AES-256/GCM key lives in the Android Keystore under [KEY_ALIAS]. The
 *    key material is generated inside the keystore and is never readable by the
 *    app process (on devices with a secure element or TEE it never leaves it).
 *  - Each value is sealed with a FRESH random IV ([KeyGenParameterSpec]
 *    `setRandomizedEncryptionRequired`), and `iv:ciphertext` (base64) is what
 *    lands in prefs. GCM authenticates: a tampered blob fails to decrypt rather
 *    than yielding garbage.
 *  - [put] answers whether the value was actually kept. A device whose keystore
 *    cannot produce a key is NOT silently downgraded to plain text — see
 *    [SupabaseSessionStore], which drops the session instead.
 *
 * This is a deliberately small, dependency-free store: it exists for the
 * session, not as a general secrets manager.
 */
internal object CurioSecureStore {
    private const val TAG = "CurioSecureStore"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "curio_session_vault"
    private const val DM_KEY_ALIAS = "curio_dm_vault"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val DM_NAME_PREFIX = "dm-"

    private fun keyFor(name: String): SecretKey = key(
        if (name.startsWith(DM_NAME_PREFIX) || name == "dm-device-id") DM_KEY_ALIAS else KEY_ALIAS
    )
    private const val GCM_TAG_BITS = 128

    /**
     * Its own prefs file, so sign-out can wipe the sealed values without
     * touching anything else the app stores. It is excluded from backup and
     * device transfer alongside the legacy session file (see
     * `res/xml/backup_rules.xml`) — a sealed token must not travel to another
     * device whose keystore cannot open it anyway.
     */
    private const val PREFS = "curio_secure_store"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The vault's key, created on first use. */
    private fun key(alias: String = KEY_ALIAS): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = runCatching { store.getEntry(alias, null) }.getOrNull()
        if (existing is KeyStore.SecretKeyEntry) {
            return runCatching { existing.secretKey }.getOrElse {
                runCatching { store.deleteEntry(alias) }
                return generateKey(alias)
            }
        }
        return generateKey(alias)
    }

    private fun generateKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    /** True when this device can actually keep a secret. */
    fun available(): Boolean = runCatching { key() }.isSuccess

    /**
     * Seals [value] under [name]. A null/blank value removes the entry.
     * Returns false when nothing was written — the caller must then treat the
     * value as UNSTORED rather than falling back to plain text.
     */
    fun put(context: Context, name: String, value: String?): Boolean {
        if (value.isNullOrEmpty()) {
            runCatching { prefs(context).edit().remove(name).apply() }
            return true
        }
        val sealed = runCatching { seal(context, name, value) }.recoverCatching { failure ->
            if (!name.startsWith(DM_NAME_PREFIX) && name != "dm-device-id") throw failure
            runCatching {
                KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(DM_KEY_ALIAS)
            }
            seal(context, name, value)
        }
        return sealed.getOrElse { failure ->
            // Never log the value; the failure itself is what matters.
            Log.w(TAG, "Could not seal a stored value", failure)
            false
        }
    }

    private fun seal(context: Context, name: String, value: String): Boolean {
        val cipher = Cipher.getInstance(TRANSFORMATION)
            .apply { init(Cipher.ENCRYPT_MODE, keyFor(name)) }
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val body = Base64.encodeToString(
            cipher.doFinal(value.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
        prefs(context).edit().putString(name, "$iv:$body").apply()
        return true
    }

    /** The sealed value, or null when it is absent, expired or tampered with. */
    fun get(context: Context, name: String): String? {
        val sealed = runCatching { prefs(context).getString(name, null) }.getOrNull() ?: return null
        val split = sealed.indexOf(':')
        if (split <= 0) return null
        return runCatching {
            val iv = Base64.decode(sealed.substring(0, split), Base64.NO_WRAP)
            val body = Base64.decode(sealed.substring(split + 1), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, keyFor(name), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            String(cipher.doFinal(body), Charsets.UTF_8)
        }.getOrNull()
    }

    /**
     * Stores a device identifier and an RSA identity keypair in the Android
     * Keystore. The private key is non-exportable; only the public key leaves
     * the device.
     */
    fun deviceId(context: Context): String? = get(context, "dm-device-id")

    fun ensureDeviceId(context: Context): String {
        deviceId(context)?.let { return it }
        val value = java.util.UUID.randomUUID().toString()
        if (!put(context, "dm-device-id", value)) {
            resetDmStorage(context)
            check(put(context, "dm-device-id", value)) { "Secure storage unavailable." }
        }
        return value
    }

    /** Clears only recoverable DM material when Android Keystore invalidates it. */
    fun resetDmStorage(context: Context) {
        runCatching {
            prefs(context).edit()
                .remove("dm-device-id")
                .apply()
        }
        runCatching {
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.also { store ->
                store.deleteEntry(DM_KEY_ALIAS)
                store.deleteEntry("curio_dm_identity")
            }
        }
    }

    fun identityKeyPair(): java.security.KeyPair {
        val alias = "curio_dm_identity"
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = store.getEntry(alias, null)
        if (existing is KeyStore.PrivateKeyEntry) {
            return java.security.KeyPair(existing.certificate.publicKey, existing.privateKey)
        }
        val generator = java.security.KeyPairGenerator.getInstance("RSA", KEYSTORE)
        generator.initialize(
            android.security.keystore.KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKeyPair()
    }

    /** Forgets every sealed value AND the key that opened them. */
    fun wipe(context: Context) {
        runCatching { prefs(context).edit().clear().apply() }
        runCatching { KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS) }
    }
}
