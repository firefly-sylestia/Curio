package com.curio.app.data.supabase

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.curio.app.BuildConfig
import com.curio.app.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The running token-refresh loop; replaced whenever a session starts. */
    private var refreshJob: Job? = null

    /**
     * The application context, captured on the first session-aware entry.
     *
     * [refreshForRetry] runs INSIDE the REST layer's own call, so it cannot be
     * handed a context by a screen: the vault read and the refreshed session's
     * save both happen here. Only the application context is kept, so no
     * activity is ever held.
     */
    private var appContext: Context? = null

    init {
        // The REST layer asks THIS for a fresh token whenever the server
        // refuses one (a 401 mid-request). Registering the hook here means
        // every social surface gets the retry without a line of its own.
        SupabaseClient.sessionRefresher = ::refreshForRetry
    }

    /** What the account UI renders. */
    data class State(
        val session: SupabaseClient.Session? = null,
        val busy: Boolean = false,
        /** Actionable failure text, already safe to show (never a raw body). */
        val error: String? = null,
        /** Non-error outcome worth telling the user (e.g. confirm your email). */
        val notice: String? = null
    ) {
        /** True when a session is held, so Online Mode can be offered. */
        val signedIn: Boolean get() = session != null

        /** The signed-in email, when the account reported one. */
        val email: String? get() = session?.email
    }

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
        appContext = context.applicationContext
        if (state.session != null) return
        val stored = SupabaseSessionStore.get(context) ?: return
        state = state.copy(session = stored, error = null)
        recoveryScope.launch {
            SupabaseClient.refreshSession(stored.refreshToken).fold(
                onSuccess = { refreshed ->
                    SupabaseSessionStore.save(context, refreshed)
                    enableOnlineMode(context)
                    SupabaseClient.updateOnlineMode(refreshed.accessToken, true)
                    state = state.copy(session = refreshed, busy = false, error = null)
                    keepSessionFresh(context, refreshed)
                    recoveryScope.launch { reconcileIdentity(context, refreshed) }
                },
                onFailure = {
                    // Keep the cached session for offline use, but avoid trapping
                    // the user in a dead token: the next protected call can sign in again.
                    state = state.copy(busy = false, error = null)
                }
            )
        }
    }

    /** Signs in and, on success, turns Online Mode on (the user asked for it). */
    suspend fun signIn(context: Context, email: String, password: String): Boolean {
        appContext = context.applicationContext
        val address = email.trim()
        if (address.isEmpty() || password.isEmpty()) {
            state = state.copy(error = "Enter your email and password.")
            return false
        }
        state = state.copy(busy = true, error = null, notice = null)
        return SupabaseClient.signIn(address, password).fold(
            onSuccess = { session ->
                SupabaseSessionStore.save(context, session)
                enableOnlineMode(context)
                SupabaseClient.updateOnlineMode(session.accessToken, true)
                reconcileIdentity(context, session)
                state = State(session = session)
                keepSessionFresh(context, session)
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
     * Where a confirmation email should land, or null when no account site is
     * configured for this build.
     *
     * The account site owns that page, because an email link has to open
     * somewhere a browser can show: the app's own sign-up form is not a URL, and
     * without this Supabase falls back to the project's Site URL, whose default
     * is `http://localhost:3000`. An empty [BuildConfig.CURIO_AUTH_SITE_URL]
     * sends no redirect at all, which is exactly the old behaviour, so a build
     * from before the site exists is unchanged.
     */
    private fun confirmationRedirect(): String? =
        BuildConfig.CURIO_AUTH_SITE_URL
            .trim()
            .trimEnd('/')
            .takeIf { it.isNotEmpty() }
            ?.let { "$it/confirm" }

    /**
     * Creates the account. Supabase only answers with a session when email
     * confirmation is disabled for the project; otherwise the user has to
     * confirm first, so that case reports a notice instead of signing in.
     */
    suspend fun signUp(context: Context, email: String, password: String): Boolean {
        appContext = context.applicationContext
        val address = email.trim()
        if (address.isEmpty() || password.isEmpty()) {
            state = state.copy(error = "Enter your email and password.")
            return false
        }
        state = state.copy(busy = true, error = null, notice = null)
        return SupabaseClient.signUp(address, password, confirmationRedirect()).fold(
            onSuccess = { session ->
                if (session == null) {
                    // Email confirmation is ON for this project, so the
                    // account exists and the next step belongs to the inbox.
                    // The email stays in the form and this reads as the
                    // SUCCESS it is, not as a failure the user has to retry.
                    state = State(
                        notice = "Account created. Confirm the link we emailed to " +
                            "$address, then sign in with the same email."
                    )
                    false
                } else {
                    SupabaseSessionStore.save(context, session)
                    enableOnlineMode(context)
                    SupabaseClient.updateOnlineMode(session.accessToken, true)
                    reconcileIdentity(context, session)
                    state = State(session = session)
                    keepSessionFresh(context, session)
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
        // The freshness loop must stop BEFORE the session is dropped, or a
        // refresh already in flight could write the signed-out session back.
        refreshJob?.cancel()
        refreshJob = null
        state = state.copy(busy = true, error = null, notice = null)
        if (token != null) SupabaseClient.signOut(token)
        SupabaseSessionStore.clear(context)
        AppPreferences.setOnlineModeEnabled(context, false)
        // The handle belongs to the ACCOUNT, not to this device, so it goes
        // with it. Leaving it behind is how the next account signed in here
        // inherited the previous member's username (and with it their place
        // in friends' searches) until somebody noticed. The display name and
        // the portrait stay: they are this person's own and are re-pulled from
        // whichever account signs in next.
        AppPreferences.setUsername(context, "")
        // The device's social copy goes too: every cached conversation, wall
        // page, inbox, reply set and remembered person lives in
        // [SocialCache] (see the caches in features/community). Signing out
        // must not leave the previous account's threads, names or cards
        // readable by whoever opens the app next, so the whole directory is
        // deleted — not just the newest account's entries.
        SocialCache.clear(context)
        // Realtime channels are authorised by the token that just went away:
        // they are dropped here rather than left open against a dead session.
        SupabaseRealtime.reset()
        // The prefs blobs the older build wrote are dropped as well, so an
        // upgrade cannot leave the previous shape behind either.
        runCatching {
            context.applicationContext
                .getSharedPreferences(SOCIAL_CACHE_PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
        state = State()
    }

    /**
     * Flips Online Mode. The local preference is authoritative (so the app
     * keeps working offline); the server row is mirrored best-effort and a
     * failure is surfaced instead of silently dropping the change.
     */
    suspend fun setOnlineMode(context: Context, enabled: Boolean) {
        if (enabled) enableOnlineMode(context)
        else AppPreferences.setOnlineModeEnabled(context, false)
        val token = state.session?.accessToken ?: return
        SupabaseClient.updateOnlineMode(token, enabled).onFailure { failure ->
            state = state.copy(error = onlineAuthMessage(failure))
        }
    }

    /**
     * Turns Online mode on, and brings the Social tab with it.
     *
     * The Social wall is what most members are actually turning this switch on
     * for, and reaching it meant knowing that a SECOND, separate switch existed
     * in the same page. The tab is only ever turned ON here, and only on a real
     * off-to-on change: a member who switched the tab off on purpose keeps it
     * off until they turn Online mode off and on again, and turning Online mode
     * off hides the tab anyway (see `AppPreferences.communityTabVisible`).
     */
    private fun enableOnlineMode(context: Context) {
        val wasOn = AppPreferences.isOnlineModeEnabled(context)
        AppPreferences.setOnlineModeEnabled(context, true)
        if (!wasOn) AppPreferences.setCommunityTabEnabled(context, true)
    }

    /** Clears a shown error/notice (the UI calls this when it moves on). */
    fun clearMessage() {
        state = state.copy(error = null, notice = null)
    }

    /**
     * Keeps the access token fresh for as long as the app runs.
     *
     * A Supabase access token lives about an hour, and EVERYTHING online is
     * authorised by it: the REST reads, and the realtime channel, which is
     * joined with that exact token and stops delivering when the server no
     * longer accepts it. Refreshing a few minutes before the JWT's own `exp`
     * means the screens re-run their `watch()` with a new token and the
     * channel is rebuilt as the new RLS context, instead of realtime going
     * quiet an hour into a session while the app still looks connected.
     *
     * A failed refresh is retried, never treated as a sign-out: the token in
     * hand is usually still usable, and a phone that lost signal must not lose
     * the session over it.
     */
    private fun keepSessionFresh(context: Context, session: SupabaseClient.Session) {
        refreshJob?.cancel()
        refreshJob = recoveryScope.launch {
            var current = session
            while (isActive) {
                val live = state.session ?: return@launch
                // A different session (sign-out, or another account) owns the
                // token now: this loop is the old one and retires itself.
                if (live.accessToken != current.accessToken) return@launch
                val expiresAt = SupabaseClient.accessTokenExpiresAtMillis(current.accessToken)
                delay(
                    if (expiresAt > 0L) {
                        (expiresAt - System.currentTimeMillis() - REFRESH_LEAD_MS)
                            .coerceAtLeast(MIN_REFRESH_WAIT_MS)
                    } else {
                        DEFAULT_REFRESH_INTERVAL_MS
                    }
                )
                SupabaseClient.refreshSession(current.refreshToken).fold(
                    onSuccess = { refreshed ->
                        SupabaseSessionStore.save(context, refreshed)
                        state = state.copy(session = refreshed, error = null)
                        current = refreshed
                    },
                    onFailure = { delay(REFRESH_RETRY_MS) }
                )
            }
        }
    }

    /**
     * The retry behind [SupabaseClient.sessionRefresher]: a request came back
     * 401, so refresh the session and hand back a usable token.
     *
     * Blocking by contract (the REST layer is mid-request on its own IO
     * thread). Three things can be true, and each one gets its own answer
     * rather than the server's "JWT expired":
     *
     *  - another call already refreshed while this request was in flight, so
     *    the newer token is reused and no second session is minted;
     *  - the refresh cannot reach the server (no signal, slow network), which
     *    is a CONNECTION problem, not a session problem;
     *  - the refresh token was refused, which really does end the session.
     *
     * A successful refresh also seals the session into the vault, publishes it
     * to [state] (screens holding the old token recompose with the new one and
     * the realtime channel re-joins as the new RLS context) and restarts the
     * freshness loop.
     */
    private fun refreshForRetry(refused: String): TokenRefresh {
        state.session?.let { live ->
            if (live.accessToken != refused) return TokenRefresh.Fresh(live.accessToken)
        }
        val context = appContext ?: return TokenRefresh.Refused
        val refresh = state.session?.refreshToken
            ?: SupabaseSessionStore.get(context)?.refreshToken
            ?: return TokenRefresh.Refused
        return when (val attempt = SupabaseClient.refreshSessionBlocking(refresh)) {
            is RefreshAttempt.Ok -> {
                SupabaseSessionStore.save(context, attempt.session)
                state = state.copy(session = attempt.session, busy = false, error = null)
                keepSessionFresh(context, attempt.session)
                TokenRefresh.Fresh(attempt.session.accessToken)
            }
            RefreshAttempt.Unreachable -> TokenRefresh.Unreachable
            RefreshAttempt.Refused -> TokenRefresh.Refused
        }
    }

    /**
     * Makes the ACCOUNT and this device agree on who the member is.
     *
     * The account owns the handle: it is how a member is found, added and
     * mentioned, so an account with none is given a generated one here rather
     * than being unable to use the social half at all. The device's own name
     * and bio are pushed ONLY into an account that has none of its own, so a
     * sign-in can never rewrite the name an existing account already owns. A
     * brand-new account therefore still reaches the wall with a real name, and
     * an existing one keeps the name its owner chose.
     *
     * Best-effort throughout: a failed push is a blank second line or an
     * absent bio, never a blocked sign-in.
     */
    private suspend fun reconcileIdentity(context: Context, session: SupabaseClient.Session) {
        val profile = SocialApi.profile(session.accessToken, session.userId).getOrNull() ?: return
        if (profile.username.isBlank()) {
            claimGeneratedUsername(context, session.accessToken)
        } else {
            AppPreferences.setUsername(context, profile.username)
        }
        if (profile.displayName.isNotBlank()) {
            AppPreferences.setDisplayName(context, profile.displayName)
        } else {
            val name = AppPreferences.getDisplayName(context)
            if (name.isNotBlank()) SocialApi.updateDisplayName(session.accessToken, name)
        }
        if (profile.bio.isBlank()) {
            val bio = AppPreferences.getCustomStreakTagline(context)
            if (bio.isNotBlank()) SocialApi.updateBio(session.accessToken, bio)
        }
        AppPreferences.setSocialAvatarStyle(context, profile.avatarStyle)
    }

    /**
     * Gives an account that has no handle one of its own.
     *
     * Without a username a member cannot be found, added or mentioned, which is
     * every social action there is, so a brand-new account used to be stuck
     * until somebody thought to claim one. The generated name is a normal
     * username and can be replaced in Edit profile at any time.
     *
     * ONE attempt on purpose: a name already being taken is the only expected
     * failure and the collision space is wide, while the client's rename
     * cooldown means a second try would be refused anyway. A genuine failure
     * (offline) simply leaves the account unnamed until the next sign-in, and
     * nothing about the sign-in itself is blocked either way.
     *
     * A short, ordinary name is used rather than a wall of digits: it is what
     * friends read on a card, and the member can replace it in one step.
     */
    private suspend fun claimGeneratedUsername(context: Context, accessToken: String) {
        val candidate = SocialApi.suggestUsername()
        SocialApi.updateUsername(accessToken, candidate).onSuccess {
            AppPreferences.setUsername(context, candidate)
        }
    }
}

/**
 * The prefs file the PREVIOUS build's social caches shared (messages +
 * remembered people). The caches live in [SocialCache] on disk now, but
 * signing out still clears this file so an upgraded device cannot keep
 * answering from the old shape.
 */
internal const val SOCIAL_CACHE_PREFS = "curio_social_cache"

/** How long before a token's own `exp` the session is refreshed. */
private const val REFRESH_LEAD_MS = 5L * 60 * 1000

/** The floor for a token that is already near (or past) its expiry. */
private const val MIN_REFRESH_WAIT_MS = 30L * 1000

/** Used when the token carries no readable `exp` claim. */
private const val DEFAULT_REFRESH_INTERVAL_MS = 50L * 60 * 1000

/** Backoff after a failed refresh, before asking again. */
private const val REFRESH_RETRY_MS = 2L * 60 * 1000

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
