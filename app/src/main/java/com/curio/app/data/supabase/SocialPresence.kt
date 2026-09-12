package com.curio.app.data.supabase

import android.content.Context
import com.curio.app.data.AppPreferences

/**
 * PRESENCE — the member's own last-active stamp.
 *
 * Presence is a courtesy, not a record: it exists so a profile can say
 * "Active now" instead of nothing, and it is the ONE thing the privacy screen
 * can switch off completely. The promise is kept by ABSENCE — a member who
 * hid activity has no stamp on the server at all ([SocialApi.updatePrivacy]
 * clears it in the same write), so there is nothing for another account to
 * read whether a policy is consulted or not.
 *
 * Two rules keep it cheap and honest:
 *
 *  - **Throttled.** The watcher ticks every five minutes, and the write itself
 *    backs off to at most once per [GAP_MS], so a member who leaves Curio open
 *    all day costs a handful of tiny writes.
 *  - **Silent.** A failed stamp is never surfaced — presence is decoration on
 *    somebody else's screen, so it must never become an error on yours.
 */
object SocialPresence {
    /** The longest gap between two published stamps. */
    private const val GAP_MS = 5L * 60 * 1000

    @Volatile
    private var lastStampAt = 0L

    /**
     * Publishes a fresh stamp for the signed-in account, unless the member hid
     * activity or Online Mode is off. Safe to call from a loop.
     */
    suspend fun publish(context: Context, force: Boolean = false) {
        val token = OnlineAccount.state.session?.accessToken ?: return
        if (!AppPreferences.isOnlineModeEnabled(context)) return
        if (AppPreferences.isActivityHidden(context)) return
        val now = System.currentTimeMillis()
        if (!force && now - lastStampAt < GAP_MS) return
        lastStampAt = now
        SocialApi.setPresence(token, now)
    }

    /**
     * Takes the stamp away — called the moment activity hiding is switched ON,
     * so the choice lands on the server immediately rather than at the next
     * tick.
     */
    suspend fun withdraw(context: Context) {
        val token = OnlineAccount.state.session?.accessToken ?: return
        lastStampAt = 0L
        SocialApi.setPresence(token, null)
    }
}
