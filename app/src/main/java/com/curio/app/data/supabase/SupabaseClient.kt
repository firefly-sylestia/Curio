package com.curio.app.data.supabase

import android.util.Base64
import com.curio.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * What the app managed to do about an access token the server refused.
 *
 * The distinction is the honest answer to "why did that fail": a device with
 * no usable network gets a connection sentence, while a REFUSED refresh means
 * the session itself is over and the member has to sign in again. Reporting a
 * stale token as "JWT expired" told the member nothing either way.
 */
internal sealed class TokenRefresh {
    /** A working token, minted (or already held) by the session layer. */
    class Fresh(val accessToken: String) : TokenRefresh()
    /** The device could not reach the server at all. */
    object Unreachable : TokenRefresh()
    /** The server refused the refresh: the session has ended. */
    object Refused : TokenRefresh()
}

/** The raw outcome of a blocking session refresh. */
internal sealed class RefreshAttempt {
    class Ok(val session: SupabaseClient.Session) : RefreshAttempt()
    object Unreachable : RefreshAttempt()
    object Refused : RefreshAttempt()
}

/**
 * Small Android-only Supabase REST/Auth client.
 *
 * Curio uses the public publishable key plus the user's access token. A
 * service-role key is intentionally not supported by this class.
 *
 * **A stale access token is fixed here, not reported.** Every authorised call
 * goes through [executeBody], and a 401 refreshes the session and re-sends the
 * SAME request once before the failure ever reaches a screen. Without that, a
 * token that quietly expired (or a clock that drifted, or a device that was
 * offline when its refresh came due) turned every tap into "JWT expired"
 * until something else happened to refresh the session.
 */
object SupabaseClient {
    private val http = OkHttpClient.Builder()
        // Slow mobile networks, not dead ones. OkHttp's 10s defaults fire while
        // a send is still on the wire, which reads to the member as "the
        // message never sent" even though it did — they then leave the chat
        // and reopen it to find it there. 30s of silence is a real failure; a
        // few seconds of slowness is not.
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * The app's session authority, registered by `OnlineAccount`.
     *
     * Given the token the server just refused, it refreshes the session and
     * answers the token to retry with. BLOCKING on purpose: it runs on the
     * REST layer's own IO thread, in the middle of a request that is already
     * on the wire, and the caller cannot continue without an answer.
     */
    internal var sessionRefresher: ((String) -> TokenRefresh)? = null

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    data class Session(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val email: String?
    )

    suspend fun signUp(email: String, password: String): Result<Session?> =
        authRequest("/auth/v1/signup", email, password)

    suspend fun signIn(email: String, password: String): Result<Session> =
        authRequest("/auth/v1/token?grant_type=password", email, password)
            .mapCatching { it ?: error("Supabase did not return a session") }

    suspend fun refreshSession(refreshToken: String): Result<Session> = withContext(Dispatchers.IO) {
        runCatching {
            requireConfigured()
            when (val attempt = refreshSessionBlocking(refreshToken)) {
                is RefreshAttempt.Ok -> attempt.session
                RefreshAttempt.Unreachable -> throw java.io.IOException(OFFLINE_MESSAGE)
                RefreshAttempt.Refused -> error("Could not refresh the Curio session")
            }
        }
    }

    /**
     * The refresh itself, blocking and WITHOUT the 401 retry wrapper.
     *
     * Called from [executeBody]'s own 401 path, where going through
     * [executeBody] again would recurse. It also has to say WHY it failed: a
     * refresh that could not reach the network is a connection problem, while
     * a refresh the server refused means the session is genuinely over.
     */
    internal fun refreshSessionBlocking(refreshToken: String): RefreshAttempt {
        if (refreshToken.isBlank() || !isConfigured) return RefreshAttempt.Refused
        return try {
            val body = JSONObject().put("refresh_token", refreshToken)
            val request = requestBuilder("/auth/v1/token?grant_type=refresh_token")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val answer = ask(request)
            if (answer.success) RefreshAttempt.Ok(parseSession(answer.body))
            else RefreshAttempt.Refused
        } catch (offline: java.io.IOException) {
            RefreshAttempt.Unreachable
        } catch (failure: Throwable) {
            RefreshAttempt.Refused
        }
    }

    suspend fun signOut(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConfigured()
            val request = requestBuilder("/auth/v1/logout", accessToken)
                .post("".toRequestBody(jsonMediaType))
                .build()
            execute(request)
        }
    }

    /**
     * Mirrors the account's Online Mode switch into its `profiles` row.
     *
     * An UPSERT (not a PATCH): a brand-new account has no row yet, and the
     * community RLS policies read `profiles.online_mode_enabled` — a PATCH
     * that matched zero rows would leave the switch looking on in the app
     * while the server still refused every community call.
     */
    suspend fun updateOnlineMode(accessToken: String, enabled: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConfigured()
                val userId = userIdFromAccessToken(accessToken)
                val body = JSONObject()
                    .put("id", userId)
                    .put("online_mode_enabled", enabled)
                val request = requestBuilder("/rest/v1/profiles?on_conflict=id", accessToken)
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()
                execute(request)
            }
        }

    private suspend fun authRequest(path: String, email: String, password: String): Result<Session?> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConfigured()
                val body = JSONObject().put("email", email).put("password", password)
                val request = requestBuilder(path)
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()
                val response = executeBody(request)
                if (response.isBlank()) return@runCatching null
                // A signup against a project with email confirmation ON comes
                // back as the new USER with NO session. That is a success with
                // a step left, not a failure: parsing it as a session threw on
                // the missing access_token, which is what surfaced "Something
                // went wrong" for a perfectly normal signup and made the user
                // sign in again to be told to confirm their email.
                val json = runCatching { JSONObject(response) }.getOrNull()
                    ?: return@runCatching null
                val access = json.optString("access_token")
                if (json.isNull("access_token") || access.isBlank()) null
                else parseSession(response)
            }
        }

    internal fun requestBuilder(path: String, accessToken: String? = null): Request.Builder {
        val builder = Request.Builder()
            .url(BuildConfig.SUPABASE_URL.trimEnd('/') + path)
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Content-Type", "application/json")
        accessToken?.let { builder.header("Authorization", "Bearer $it") }
        return builder
    }

    private fun execute(request: Request) {
        executeBody(request)
    }

    /** One HTTP round trip, kept as data so a 401 can be re-sent. */
    private class Answer(val code: Int, val body: String) {
        val success: Boolean get() = code in 200..299

        /**
         * The server's own words about the failure, or a plain status line
         * when it sent none (a signed-out request, an empty body).
         */
        fun serverMessage(): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            return json?.optString("msg").orEmpty()
                .ifBlank { json?.optString("message").orEmpty() }
                .ifBlank { json?.optString("error_description").orEmpty() }
                .ifBlank { "Supabase request failed ($code)" }
        }
    }

    private fun ask(request: Request): Answer = http.newCall(request).execute().use { response ->
        Answer(response.code, response.body?.string().orEmpty())
    }

    /**
     * Runs [request], and on a 401 refreshes the session and re-sends it ONCE.
     *
     * This is the difference between "your session expired, sign in again"
     * and an app that simply keeps working: the access token is only stale, and
     * the refresh token can mint a new one. The retry is bounded (one attempt),
     * skipped for requests that carried no token (a 401 on sign-in is about the
     * credentials), and every failure is re-worded so a screen never renders
     * "JWT expired" at the member.
     */
    internal fun executeBody(request: Request): String {
        val first = ask(request)
        if (first.success) return first.body
        if (first.code == HTTP_UNAUTHORIZED) {
            val refused = request.header("Authorization")?.removePrefix(BEARER_PREFIX).orEmpty()
            if (refused.isNotBlank()) {
                when (val fresh = runCatching { sessionRefresher?.invoke(refused) }.getOrNull()) {
                    is TokenRefresh.Fresh -> {
                        val retry = ask(
                            request.newBuilder()
                                .header("Authorization", "$BEARER_PREFIX${fresh.accessToken}")
                                .build()
                        )
                        if (retry.success) return retry.body
                        // The fresh token was refused too: the request itself is
                        // the problem, so report what the server said about IT.
                        error(retry.serverMessage())
                    }
                    TokenRefresh.Unreachable -> error(OFFLINE_MESSAGE)
                    TokenRefresh.Refused -> error(SESSION_ENDED_MESSAGE)
                    else -> error(STALE_SESSION_MESSAGE)
                }
            }
        }
        error(first.serverMessage())
    }

    private fun parseSession(body: String): Session {
        val json = JSONObject(body)
        val user = json.optJSONObject("user")
        return Session(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            userId = user?.getString("id") ?: userIdFromAccessToken(json.getString("access_token")),
            email = user?.optString("email")?.takeIf { it.isNotBlank() }
        )
    }

    internal fun userIdFromAccessToken(token: String): String {
        val payload = token.split('.').getOrNull(1) ?: error("Invalid Supabase access token")
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return JSONObject(String(decoded, Charsets.UTF_8)).getString("sub")
    }

    /**
     * When the access token stops being accepted, or 0 when its `exp` claim
     * cannot be read. Read without verifying the signature: this is only used
     * to decide WHEN to refresh, never to decide whether a token is valid.
     */
    internal fun accessTokenExpiresAtMillis(token: String): Long = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return@runCatching 0L
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val seconds = JSONObject(String(decoded, Charsets.UTF_8)).optLong("exp")
        if (seconds > 0L) seconds * 1_000L else 0L
    }.getOrDefault(0L)

    private fun requireConfigured() {
        check(isConfigured) {
            "Supabase is not configured. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY for the Android build."
        }
    }

    private const val HTTP_UNAUTHORIZED = 401
    private const val BEARER_PREFIX = "Bearer "

    /**
     * The three sentences a refused token can honestly produce. None of them
     * is the server's own jargon ("JWT expired", "invalid claim"): those name
     * the mechanism, not what the member should do about it.
     */
    private const val OFFLINE_MESSAGE = "No connection. Check your network and try again."
    private const val SESSION_ENDED_MESSAGE =
        "Your Curio session ended. Sign in again in Settings → Online mode."
    private const val STALE_SESSION_MESSAGE =
        "That session has expired. Reopen Curio, or sign in again in Settings → Online mode."
}
