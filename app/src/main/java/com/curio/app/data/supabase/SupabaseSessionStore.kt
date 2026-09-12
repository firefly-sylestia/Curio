package com.curio.app.data.supabase

import android.content.Context

/**
 * Minimal session holder for the first auth slice. Tokens are kept out of
 * logs and are cleared on sign-out; encrypted storage can be added before
 * production release once the auth UI flow is wired end to end.
 */
object SupabaseSessionStore {
    private const val PREFS = "curio_supabase_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val USER_ID = "user_id"
    private const val EMAIL = "email"

    fun get(context: Context): SupabaseClient.Session? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val access = prefs.getString(ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(REFRESH_TOKEN, null) ?: return null
        val userId = prefs.getString(USER_ID, null) ?: return null
        return SupabaseClient.Session(access, refresh, userId, prefs.getString(EMAIL, null))
    }

    fun save(context: Context, session: SupabaseClient.Session) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ACCESS_TOKEN, session.accessToken)
            .putString(REFRESH_TOKEN, session.refreshToken)
            .putString(USER_ID, session.userId)
            .putString(EMAIL, session.email)
            .apply()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
