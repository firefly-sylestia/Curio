package com.curio.app.data.supabase

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.curio.app.data.AppPreferences

/**
 * The app's ONLINE ACCOUNT state — the single observable source for the
 * account UI (Online Mode / sign-in page), built on [SupabaseClient] +
 * [SupabaseSessionStore].
 *
 * Compose reads [state], so every screen that shows account UI repaints on
 * its own after a sign-in, sign-out or failure. The layer is deliberately
 * thin: Room stays the source of truth for local Curio data, and nothing
 * here uploads media — the sync slice only ever ships text metadata.
 *
 * Offline-first: with no stored session the account is simply signed out,
 * and Online Mode stays off until the user turns it on.
 */
object OnlineAccount {

    /** What the account UI renders. */
    data class State(
        val session: SupabaseClient.Session? = null,
        val busy: Boolean = false,
        /** Actionable failure text, already safe to show (never a raw body). */
        val error: String? = null,
        /** Non-error outcome worth telling the user (e.g. confirm your email). */
        val notice: String? = null
    )

    var state: State by mutableStateOf(State())
        private set

    /** True when a session is held, so Online Mode can be offered. */
    val signedIn: Boolean get() = state.session != null

    /** The signed-in email, when the account reported one. */
    val email: String? get() = state.session?.email

    /** True when this build carries a Supabase URL + publishable key. */
    val configured: Boolean get() = SupabaseClient.isConfigured

    /**
     * Loads the stored session so a returning user is already signed in.
     * Safe to call from every entry point — an existing in-memory session
     * is never overwritten, so a screen entering twice costs nothing.
     */
    fun restore(context: Context) {
        if (state.session != null) return
        val stored = SupabaseSessionStore.get(context)
        if (stored != null) state = state.copy(session = stored)
    }

    /** Signs in and, on success, turns Online Mode on (the user asked for it). */
    suspend fun signIn(context: Context, email: String, password: String): Boolean {
        val address = email.trim()
        if (address.isEmpty() || password.isEmpty()) {
            state = state.copy(error = "Enter your email and password.")
            return false
        }
        state = state.copy(busy = true, error = null, notice = null)
        return SupabaseClient.signIn(address, password).fold(
            onSuccess = { session ->
                SupabaseSessionStore.save(context, session)
                AppPreferences.setOnlineModeEnabled(context, true)
                state = State(session = session)
                true
            },
            onFailure = { failure ->
                SupabaseSessionStore.clear(context)
                state = State(error = onlineAuthMessage(failure))
                false
            }
        )
    }

    /**
     * Creates the account. Supabase only answers with a session when email
     * confirmation is disabled for the project; otherwise the user has to
     * confirm first, so that case reports a notice instead of signing in.
     */
    suspend fun signUp(context: Context, email: String, password: String): Boolean {
        val address = email.trim()
        if (address.isEmpty() || password.isEmpty()) {
            state = state.copy(error = "Enter your email and password.")
            return false
        }
        state = state.copy(busy = true, error = null, notice = null)
        return SupabaseClient.signUp(address, password).fold(
            onSuccess = { session ->
                if (session == null) {
                    state = State(
                        notice = "Almost there — confirm the link we emailed to " +
                            "$address, then sign in."
                    )
                    false
                } else {
                    SupabaseSessionStore.save(context, session)
                    AppPreferences.setOnlineModeEnabled(context, true)
                    state = State(session = session)
                    true
                }
            },
            onFailure = { failure ->
                state = State(error = onlineAuthMessage(failure))
                false
            }
        )
    }

    /** Signs out locally no matter what the server says, and drops Online Mode. */
    suspend fun signOut(context: Context) {
        val token = state.session?.accessToken
        state = state.copy(busy = true, error = null, notice = null)
        if (token != null) SupabaseClient.signOut(token)
        SupabaseSessionStore.clear(context)
        AppPreferences.setOnlineModeEnabled(context, false)
        state = State()
    }

    /**
     * Flips Online Mode. The local preference is authoritative (so the app
     * keeps working offline); the server row is mirrored best-effort and a
     * failure is surfaced instead of silently dropping the change.
     */
    suspend fun setOnlineMode(context: Context, enabled: Boolean) {
        AppPreferences.setOnlineModeEnabled(context, enabled)
        if (!enabled) return
        val token = state.session?.accessToken ?: return
        SupabaseClient.updateOnlineMode(token, true).onFailure { failure ->
            state = state.copy(error = onlineAuthMessage(failure))
        }
    }

    /** Clears a shown error/notice (the UI calls this when it moves on). */
    fun clearMessage() {
        state = state.copy(error = null, notice = null)
    }
}

/**
 * Maps a transport failure to copy that is safe to render: rate limits and
 * unconfirmed emails stay actionable, invalid credentials get a plain
 * phrasing, and anything unrecognised collapses to one generic line so a
 * raw response body can never leak into the UI.
 */
internal fun onlineAuthMessage(failure: Throwable): String {
    val raw = failure.message.orEmpty()
    return when {
        raw.contains("rate limit", true) || raw.contains("too many", true) ->
            "Too many attempts — wait a minute and try again."
        raw.contains("not confirmed", true) ->
            "That email isn't confirmed yet — check your inbox for the link."
        raw.contains("already registered", true) || raw.contains("already been registered", true) ->
            "That email already has an account — sign in instead."
        raw.contains("invalid login credentials", true) || raw.contains("invalid_grant", true) ->
            "That email and password don't match."
        raw.contains("at least 6 characters", true) || raw.contains("Password should be", true) ->
            "Passwords need at least 6 characters."
        raw.contains("email", true) && raw.contains("invalid", true) ->
            "That doesn't look like a valid email address."
        raw.contains("not configured", true) ->
            "Online features aren't set up in this build."
        raw.contains("unable to resolve host", true) ||
            raw.contains("failed to connect", true) ||
            raw.contains("timeout", true) ->
            "No connection — check your network and try again."
        else -> "Something went wrong. Try again in a moment."
    }
}
