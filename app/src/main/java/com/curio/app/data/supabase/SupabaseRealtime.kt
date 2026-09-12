package com.curio.app.data.supabase

import android.util.Log
import com.curio.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * One table a screen wants to hear about.
 *
 * [filter] is PostgREST's own syntax (`recipient=eq.<uuid>`) and is applied by
 * the SERVER, so a filtered subscription costs nothing until a matching row
 * changes. [events] are Postgres events — `INSERT` is the default because a new
 * row is what almost every surface here reacts to.
 */
data class RealtimeWatch(
    val table: String,
    val filter: String? = null,
    val events: List<String> = listOf("INSERT")
)

/**
 * SUPABASE REALTIME — Curio's own tiny phoenix-protocol client.
 *
 * Why it exists: every social surface used to be driven by a timer. A message
 * that arrived while you were looking at the thread waited up to a tick to
 * appear, and the wall waited longer; the only way to make that feel live was
 * to shorten the tick, which costs battery and data for the 99% of ticks that
 * find nothing. This flips it around: the SERVER says when something changed,
 * and the screen immediately pulls the same small delta it already knew how to
 * pull.
 *
 * Why hand-rolled: this is one WebSocket with four JSON frame types, and the
 * app already ships OkHttp. A realtime SDK would be a large dependency for
 * that, and the AGENTS contract for this package is explicit about not adding
 * a Supabase SDK.
 *
 * Three deliberate properties:
 *
 *  - **A push is a HINT, never the data.** [watch]'s callback says "something
 *    in this table changed"; the screen then refetches through the normal REST
 *    path, so RLS still decides what is visible and a spoofed frame cannot
 *    inject a row into the UI.
 *  - **It degrades to polling, quietly.** [isLinked] is false until the channel
 *    is actually joined; screens read it to choose between a fast fallback tick
 *    and a slow safety tick. A socket that cannot connect (a blocked network, a
 *    project without the publication) never breaks a screen.
 *  - **A refused channel is not retried forever.** A join the server rejects
 *    (RLS, or a table missing from the `supabase_realtime` publication) will
 *    not start working on the eleventh attempt, so it stops and reports why.
 *
 * Frames are guarded everywhere: a WebSocket callback runs on its own thread,
 * and an exception escaping one would take the process down.
 */
object SupabaseRealtime {
    private const val TAG = "SupabaseRealtime"

    /** The channel name. One channel carries every binding the app asks for. */
    private const val TOPIC = "realtime:curio"

    /** How often the channel is kept alive (the server drops it at ~30s idle). */
    private const val HEARTBEAT_MS = 25_000L

    /** Backoff ceiling for a transport reconnect. */
    private const val MAX_BACKOFF_MS = 30_000L

    /**
     * True while the socket is open AND the channel is joined. Screens read
     * this to decide whether the timer is a fallback or just a safety net.
     */
    @Volatile
    var isLinked: Boolean = false
        private set

    /** Why realtime is off, when it is — a short, safe-to-show sentence. */
    @Volatile
    var lastProblem: String? = null
        private set

    private class Subscription(
        val watches: List<RealtimeWatch>,
        val onChange: () -> Unit
    )

    private val http = OkHttpClient.Builder()
        // The transport-level keep-alive, so a dead link is noticed even when
        // the server is not answering frames.
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Owner key → what that screen wants. A re-watch replaces its own entry. */
    private val subscriptions = ConcurrentHashMap<String, Subscription>()

    private var socket: WebSocket? = null
    private var heartbeat: Job? = null
    private var reconnect: Job? = null

    /** The token the current channel was joined with — its RLS context. */
    @Volatile
    private var token: String? = null

    /**
     * Set when the server REFUSED the join. Retrying cannot fix that, so it
     * stops until a new token arrives.
     */
    @Volatile
    private var refused: Boolean = false

    /**
     * Subscribes [owner] to [watches]. Calling again for the same owner
     * replaces that owner's set, so a screen can simply re-declare on every
     * recomposition without leaking listeners.
     *
     * [onChange] runs on a background thread, at most once per server event.
     * It should be a cheap "something moved" signal.
     */
    fun watch(
        owner: String,
        accessToken: String,
        watches: List<RealtimeWatch>,
        onChange: () -> Unit
    ) {
        if (watches.isEmpty() || !SupabaseClient.isConfigured) return
        val tokenChanged = token != null && token != accessToken
        token = accessToken
        subscriptions[owner] = Subscription(watches, onChange)
        if (tokenChanged) {
            // A new session is a new RLS context: the old channel's bindings
            // were authorised as somebody else, so it has to be rebuilt.
            refused = false
            reopen()
        } else {
            connect()
        }
    }

    /** Drops [owner]. The socket closes once nothing is left to hear. */
    fun unwatch(owner: String) {
        subscriptions.remove(owner)
        if (subscriptions.isEmpty()) shutdown()
    }

    /** Forgets EVERYTHING — sign-out, so no channel outlives the session. */
    fun reset() {
        subscriptions.clear()
        shutdown()
        token = null
        refused = false
        lastProblem = null
    }

    // ── transport ────────────────────────────────────────────────────────

    private fun connect() {
        if (socket != null || refused || subscriptions.isEmpty()) return
        val url = socketUrl() ?: return
        runCatching {
            socket = http.newWebSocket(Request.Builder().url(url).build(), Listener())
        }.onFailure {
            Log.w(TAG, "Could not open the realtime socket", it)
            lastProblem = "No realtime connection"
        }
    }

    private fun socketUrl(): String? {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (base.isBlank() || key.isBlank()) return null
        val ws = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> base
        }
        return "$ws/realtime/v1/websocket?apikey=$key&vsn=1.0.0"
    }

    private fun shutdown() {
        reconnect?.cancel()
        reconnect = null
        heartbeat?.cancel()
        heartbeat = null
        isLinked = false
        runCatching { socket?.close(1000, "no subscribers") }
        socket = null
    }

    private fun reopen() {
        shutdown()
        connect()
    }

    /** A transport loss: worth retrying, with backoff. */
    private fun lost(reason: String) {
        isLinked = false
        lastProblem = reason
        heartbeat?.cancel()
        heartbeat = null
        socket = null
        scheduleReconnect()
    }

    /**
     * A protocol refusal: the join itself was rejected. Retrying a refused join
     * would loop forever at full speed, so it stops and says so — the screens
     * keep polling, and a new token clears it.
     */
    private fun refuse(reason: String) {
        refused = true
        lastProblem = reason
        isLinked = false
        heartbeat?.cancel()
        heartbeat = null
        runCatching { socket?.close(1000, "channel refused") }
        socket = null
    }

    private fun scheduleReconnect() {
        if (subscriptions.isEmpty() || refused) return
        if (reconnect?.isActive == true) return
        reconnect = scope.launch {
            var backoff = 1_000L
            while (isActive && !isLinked && !refused && subscriptions.isNotEmpty()) {
                delay(backoff)
                if (isLinked) break
                // A socket that opened but never joined must not block the retry.
                if (socket == null) connect()
                backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                delay(2_000L)
            }
        }
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            runCatching { join(webSocket) }
                .onFailure { refuse("Could not join the realtime channel") }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching { handle(text) }
                .onFailure { Log.w(TAG, "Bad realtime frame", it) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (subscriptions.isEmpty()) return
            lost("closed ($code)")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (subscriptions.isEmpty()) return
            lost(t.message ?: "socket failure")
        }
    }

    // ── the protocol ─────────────────────────────────────────────────────

    private fun join(webSocket: WebSocket) {
        val bindings = JSONArray()
        val seen = HashSet<String>()
        subscriptions.values.forEach { subscription ->
            subscription.watches.forEach { watch ->
                watch.events.forEach { event ->
                    // The server rejects a duplicated binding, so the union is
                    // taken by hand rather than trusting the callers not to
                    // overlap (two screens legitimately watch the same table).
                    if (!seen.add("${watch.table}|${event}|${watch.filter.orEmpty()}")) return@forEach
                    val entry = JSONObject()
                        .put("event", event)
                        .put("schema", "public")
                        .put("table", watch.table)
                    watch.filter?.let { entry.put("filter", it) }
                    bindings.put(entry)
                }
            }
        }
        if (bindings.length() == 0) return
        val payload = JSONObject().put(
            "config",
            JSONObject().put("postgres_changes", bindings)
        )
        // The access token is what makes postgres_changes RLS-aware: without it
        // the channel would be joined as the anonymous key.
        token?.let { payload.put("access_token", it) }
        webSocket.send(
            JSONObject()
                .put("topic", TOPIC)
                .put("event", "phx_join")
                .put("payload", payload)
                .put("ref", "1")
                .toString()
        )
        startHeartbeat(webSocket)
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeat?.cancel()
        heartbeat = scope.launch {
            var ref = 1
            while (isActive) {
                delay(HEARTBEAT_MS)
                ref += 1
                val frame = JSONObject()
                    .put("topic", "phoenix")
                    .put("event", "heartbeat")
                    .put("payload", JSONObject())
                    .put("ref", ref.toString())
                // `send` answers false on a dead socket; the listener's own
                // failure/close callback is what drives the reconnect.
                if (!webSocket.send(frame.toString())) return@launch
            }
        }
    }

    private fun handle(text: String) {
        val frame = JSONObject(text)
        when (frame.optString("event")) {
            "phx_reply" -> {
                val payload = frame.optJSONObject("payload") ?: return
                if (payload.optString("status") == "ok") {
                    isLinked = true
                    lastProblem = null
                } else {
                    val reason = payload.optJSONObject("response")?.optString("reason")
                    refuse(reason?.takeIf { it.isNotBlank() } ?: "Realtime channel refused")
                }
            }
            // Some server versions announce readiness on a `system` frame
            // rather than the join reply; either one means the channel is live.
            "system" -> {
                val payload = frame.optJSONObject("payload") ?: return
                if (payload.optString("status") == "ok" &&
                    payload.optString("extension").contains("postgres_changes")
                ) {
                    isLinked = true
                    lastProblem = null
                }
            }
            "postgres_changes" -> {
                // A change is a HINT. The payload is deliberately ignored: the
                // screen refetches through REST, so RLS decides what is visible
                // and a tampered frame cannot put a row on screen.
                subscriptions.values.forEach { subscription ->
                    runCatching { subscription.onChange() }
                }
            }
            "phx_error" -> refuse("Realtime channel error")
        }
    }
}
