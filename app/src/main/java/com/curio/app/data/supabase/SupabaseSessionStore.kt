package com.curio.app.data.supabase

import android.content.Context

/**
 * Where the signed-in session lives between launches.
 *
 * **The tokens are SEALED, never written in the clear.** Values go through
 * [CurioSecureStore], which encrypts them with an AES/GCM key held in the
 * Android Keystore; the app process can use that key but cannot read it. A
 * session token is a bearer credential — anyone who can read it can read the
 * account's messages, cards and profile — so a plain `SharedPreferences` file
 * is not an acceptable place for it.
 *
 * Two consequences worth stating plainly:
 *
 *  - **No silent downgrade.** If this device cannot produce a vault key,
 *    [save] keeps NOTHING. The session survives the running process and the
 *    member signs in again next launch, which is strictly better than leaving
 *    a working token on disk in plain text.
 *  - **A one-time migration.** Sessions written by the older build sat in
 *    `curio_supabase_session` unencrypted. [migrateLegacy] moves them into the
 *    vault and deletes the plaintext copy — so an already-signed-in member
 *    stays signed in AND stops being exposed, with no re-login.
 */
object SupabaseSessionStore {
    /** The OLD plaintext file. Read once (to migrate) and cleared. */
    private const val LEGACY_PREFS = "curio_supabase_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val USER_ID = "user_id"
    private const val EMAIL = "email"

    private fun legacy(context: Context) =
        context.applicationContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)

    fun get(context: Context): SupabaseClient.Session? {
        // Anything the older build left in plain text is sealed (and erased)
        // before a session is ever read from it.
        migrateLegacy(context)
        val access = CurioSecureStore.get(context, ACCESS_TOKEN) ?: return null
        val refresh = CurioSecureStore.get(context, REFRESH_TOKEN) ?: return null
        val userId = CurioSecureStore.get(context, USER_ID) ?: return null
        return SupabaseClient.Session(access, refresh, userId, CurioSecureStore.get(context, EMAIL))
    }

    fun save(context: Context, session: SupabaseClient.Session) {
        val sealed = CurioSecureStore.put(context, ACCESS_TOKEN, session.accessToken) &&
            CurioSecureStore.put(context, REFRESH_TOKEN, session.refreshToken) &&
            CurioSecureStore.put(context, USER_ID, session.userId)
        if (!sealed) {
            // No vault on this device: keep the session for this run only. A
            // token that cannot be encrypted is never written in the clear.
            clear(context)
            return
        }
        CurioSecureStore.put(context, EMAIL, session.email)
        // Nothing of the plaintext shape may survive a successful save.
        runCatching { legacy(context).edit().clear().apply() }
    }

    /**
     * Signs out without destroying the device's DM identity or conversation
     * keys. Those are not session credentials and are required to restore
     * encrypted conversations after a later sign-in on this same device.
     */
    fun clear(context: Context) {
        CurioSecureStore.clearSession(context)
        runCatching { legacy(context).edit().clear().apply() }
    }

    /**
     * Moves a pre-encryption session into the vault, once.
     *
     * The plaintext copy is deleted whether or not the vault accepted it:
     * after this runs the tokens are either sealed or gone, never readable.
     */
    private fun migrateLegacy(context: Context) {
        val store = runCatching { legacy(context) }.getOrNull() ?: return
        if (!store.contains(ACCESS_TOKEN)) return
        val access = store.getString(ACCESS_TOKEN, null)
        val refresh = store.getString(REFRESH_TOKEN, null)
        val userId = store.getString(USER_ID, null)
        val email = store.getString(EMAIL, null)
        if (access.isNullOrBlank() || refresh.isNullOrBlank() || userId.isNullOrBlank()) {
            runCatching { store.edit().clear().apply() }
            return
        }
        CurioSecureStore.put(context, ACCESS_TOKEN, access)
        CurioSecureStore.put(context, REFRESH_TOKEN, refresh)
        CurioSecureStore.put(context, USER_ID, userId)
        CurioSecureStore.put(context, EMAIL, email)
        runCatching { store.edit().clear().apply() }
    }
}
