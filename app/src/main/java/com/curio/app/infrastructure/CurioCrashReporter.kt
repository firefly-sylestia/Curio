package com.curio.app.infrastructure

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.curio.app.data.ExploreReminderScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal crash reporter for Curio.
 *
 * Captures uncaught exceptions, persists them to SharedPreferences,
 * and can launch a Compose crash-recovery screen. Much simpler than the
 * legacy FieldMind CrashReporter — no ANR watchdog, no separate crash process.
 *
 * Crash-loop guard (always-on): consecutive crashes inside a short window
 * mean the app keeps dying before the user can see anything — the explore
 * service's constructor crash was exactly that. When [CRASH_LOOP_THRESHOLD]
 * crashes land within [CRASH_LOOP_WINDOW_MS], the reporter enters "safe
 * mode": it stops the background re-arm sources (explore service + reminder)
 * so the loop ends, and the next launch routes to the crash screen where the
 * user can read the log and restart cleanly ([resetLoopGuard]).
 */
object CurioCrashReporter {

    private const val TAG = "CurioCrashReporter"
    private const val PREFS_NAME = "curio_crash_logs"
    private const val KEY_LAST_CRASH = "last_crash_log"
    private const val KEY_CRASH_HISTORY = "crash_history"
    private const val KEY_HAS_PENDING_CRASH = "has_pending_crash"
    private const val KEY_CRASH_TIMESTAMPS = "crash_timestamps"
    private const val KEY_SAFE_MODE = "safe_mode"

    // A crash loop = this many crashes within this window (gap-based: a lone
    // crash after a long healthy stretch never trips it — old stamps drop out
    // of the window naturally).
    private const val CRASH_LOOP_WINDOW_MS = 90_000L
    private const val CRASH_LOOP_THRESHOLD = 3
    private const val MAX_TRACKED_CRASHES = 6

    private val handlingCrash = AtomicBoolean(false)
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null

    @Volatile var lastCrashLog: String? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (!handlingCrash.compareAndSet(false, true)) {
                previousHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            }

            val crashLog = buildCrashLog(thread, throwable)
            lastCrashLog = crashLog
            Log.e(TAG, "Uncaught: ${throwable::class.java.name}: ${throwable.message}")

            persistCrash(context.applicationContext, crashLog)

            // Crash-loop guard: if this is the 3rd+ crash inside the window,
            // enter safe mode and STOP the background re-arm sources so the
            // loop can't continue on the next process start — the user lands
            // on the crash-log screen instead of an endless restart.
            if (enterSafeModeIfLooping(context.applicationContext)) {
                Log.w(TAG, "Crash loop detected — entering safe mode; stopping explore service + reminder")
                runCatching {
                    context.applicationContext.stopService(
                        Intent(context.applicationContext, ExploreSessionService::class.java)
                    )
                }
                ExploreReminderScheduler.cancel(context.applicationContext)
            }

            try { Thread.sleep(300) } catch (_: InterruptedException) {}
            previousHandler?.uncaughtException(thread, throwable)
                ?: Process.killProcess(Process.myPid())
        }

        Log.d(TAG, "CrashReporter initialized")
    }

    fun testCrash() {
        throw RuntimeException("Test crash from Curio. This is intentional.")
    }

    fun buildCrashLog(thread: Thread, throwable: Throwable): String = buildString {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        appendLine("Curio Crash Report")
        appendLine("Time: ${fmt.format(Date())}")
        appendLine("Thread: ${thread.name}")
        appendLine("Exception: ${throwable::class.java.name}")
        appendLine("Message: ${throwable.message ?: "no message"}")
        runCatching {
            val ctx = appContext ?: return@runCatching
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            @Suppress("DEPRECATION")
            val vc = pi.versionCode
            appendLine("App: ${ctx.packageName} ${pi.versionName} ($vc)")
        }
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine()
        appendLine(Log.getStackTraceString(throwable))
    }

    private fun persistCrash(context: Context, log: String) {
        // .commit() (synchronous disk write), NOT .apply(): this runs in the
        // dying process right before it's killed, and the 300ms sleep happens
        // ON the crashing thread — the async apply() flush is not guaranteed
        // to land, which would silently drop the pending-crash flag.
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_CRASH, log)
            .putBoolean(KEY_HAS_PENDING_CRASH, true)
            .commit()
        val history = getCrashHistory(context).toMutableList()
        history.add(0, log)
        prefs.edit().putString(KEY_CRASH_HISTORY, history.take(20).joinToString("\n---\n")).commit()
    }

    fun getCrashHistory(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CRASH_HISTORY, null) ?: return emptyList()
        return raw.split("\n---\n").filter { it.isNotBlank() }
    }

    fun getLastCrash(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CRASH, null)
    }

    fun hasPendingCrash(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_PENDING_CRASH, false)
    }

    fun clearPendingCrash(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAS_PENDING_CRASH, false).apply()
    }

    /**
     * Records this crash in the loop-detection window and, when the loop
     * threshold is hit, flips on safe mode. Returns true once safe mode is
     * active so the caller can tear down the re-arm sources.
     */
    private fun enterSafeModeIfLooping(context: Context): Boolean {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stamps = prefs.getString(KEY_CRASH_TIMESTAMPS, null)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()
        // Only crashes inside the window count — older stamps decay away, so
        // a lone crash long after a healthy session is never a "loop".
        // .commit() for the same dying-process reason as persistCrash — the
        // safe-mode flag must survive the imminent process kill.
        val recent = (stamps + now).filter { now - it < CRASH_LOOP_WINDOW_MS }
        prefs.edit()
            .putString(KEY_CRASH_TIMESTAMPS, recent.takeLast(MAX_TRACKED_CRASHES).joinToString(","))
            .commit()
        val looping = recent.size >= CRASH_LOOP_THRESHOLD
        if (looping) prefs.edit().putBoolean(KEY_SAFE_MODE, true).commit()
        return looping
    }

    /**
     * True when the crash-loop guard has detected repeated crashes. While
     * safe mode is on, background re-arms (explore service, boot receiver)
     * are suppressed so the app can open on the crash-log screen.
     */
    fun isSafeMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SAFE_MODE, false)

    /**
     * Clears the crash-loop guard after a safe restart: pending-crash flag,
     * safe mode and the timestamp window. Crash HISTORY is kept so the user
     * can still review past crashes in the bug-report screen.
     */
    fun resetLoopGuard(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_PENDING_CRASH, false)
            .putBoolean(KEY_SAFE_MODE, false)
            .remove(KEY_CRASH_TIMESTAMPS)
            .apply()
    }

    fun clearCrashHistory(context: Context) {
        clearPendingCrash(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
