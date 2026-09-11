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

/**
 * Small Android-only Supabase REST/Auth client.
 *
 * Curio uses the public publishable key plus the user's access token. A
 * service-role key is intentionally not supported by this class.
 */
object SupabaseClient {
    private val http = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

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
            val body = JSONObject().put("refresh_token", refreshToken)
            val request = requestBuilder("/auth/v1/token?grant_type=refresh_token")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            executeBody(request).let(::parseSession)
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
                if (response.isBlank()) null else parseSession(response)
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

    internal fun executeBody(request: Request): String {
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(body).optString("msg") }
                    .getOrDefault("")
                    .ifBlank { runCatching { JSONObject(body).optString("message") }.getOrDefault("") }
                    .ifBlank { "Supabase request failed (${response.code})" }
                error(message)
            }
            return body
        }
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

    private fun userIdFromAccessToken(token: String): String {
        val payload = token.split('.').getOrNull(1) ?: error("Invalid Supabase access token")
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return JSONObject(String(decoded, Charsets.UTF_8)).getString("sub")
    }

    private fun requireConfigured() {
        check(isConfigured) {
            "Supabase is not configured. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY for the Android build."
        }
    }
}
