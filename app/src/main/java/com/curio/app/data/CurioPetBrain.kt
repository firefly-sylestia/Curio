package com.curio.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * CurioPetBrain (v8.43) — the pet's local LEARNING model (spec §10.6 v3,
 * on-device edition). NOT a cloud/LLM integration: everything lives on the
 * phone and is driven by the user's real activity, so the pet grows a
 * PERSONALITY instead of reciting fixed lines forever.
 *
 *  - Observation  — every screen visit, explore, level-up, touch and play
 *                   is fed here from the existing [CurioPet] hooks.
 *  - Traits       — a persistent vector (curiosity, playfulness, warmth,
 *                   energy, night-owl-ness) that drifts with behavior and
 *                   gently decays after long idle.
 *  - Preferences  — a favorite lane (from the passport counters) and a
 *                   favorite time of day (from an activity histogram).
 *  - Catchphrases — when a strong recurring pattern is detected the pet
 *                   COINS its own line, remembered forever (up to a cap),
 *                   so it visibly develops its own things over time.
 *  - Voice        — [say] composes one-sentence, fully GROUNDED lines
 *                   (real local stats only — lanes, streak, level, saves,
 *                   time of day, bond; never invented topic facts) in the
 *                   pet's learned voice.
 *
 * Gated by `AppPreferences.petBrainEnabledState` (default ON — see the
 * Appearance settings). When OFF, or before enough signal exists, callers
 * fall back to the classic rule-based library in [CurioPet].
 */
object CurioPetBrain {

    private const val PREFS_NAME = "curio_pet_brain"
    private const val KEY_TRAITS = "traits"
    private const val KEY_TIME_HIST = "time_hist"
    private const val KEY_COINED = "coined"
    private const val KEY_LAST_COIN_DAY = "last_coin_day"
    private const val KEY_OBSERVED_AT = "observed_at"

    /** How many catchphrases the pet may ever develop. */
    private const val MAX_COINED = 8

    /** The pet's learned personality dimensions (0..1, persisted). */
    enum class Trait { CURIOSITY, PLAYFULNESS, WARMTH, ENERGY, NIGHT_OWL }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Trait vector (one JSON object of 0..1 floats) ──────────────────
    private fun defaultTraits(): MutableMap<Trait, Float> = mutableMapOf(
        Trait.CURIOSITY to 0.15f,
        Trait.PLAYFULNESS to 0.15f,
        Trait.WARMTH to 0.10f,
        Trait.ENERGY to 0.40f,
        Trait.NIGHT_OWL to 0.20f
    )

    private fun readTraits(context: Context): MutableMap<Trait, Float> {
        val raw = prefs(context).getString(KEY_TRAITS, null) ?: return defaultTraits()
        return try {
            val def = defaultTraits()
            val o = JSONObject(raw)
            Trait.entries.associateTo(mutableMapOf()) { t ->
                t to o.optDouble(t.name, def[t]!!.toDouble()).toFloat().coerceIn(0f, 1f)
            }
        } catch (_: Exception) {
            defaultTraits()
        }
    }

    private fun writeTraits(context: Context, traits: Map<Trait, Float>) {
        val o = JSONObject()
        traits.forEach { (t, v) -> o.put(t.name, v.toDouble()) }
        prefs(context).edit().putString(KEY_TRAITS, o.toString()).apply()
    }

    /** Nudge one trait by [delta], clamped to 0..1. */
    private fun shift(context: Context, trait: Trait, delta: Float) {
        val traits = readTraits(context)
        traits[trait] = (traits[trait]!! + delta).coerceIn(0f, 1f)
        writeTraits(context, traits)
    }

    // ── Time-of-day histogram (activity by phase) ──────────────────────
    private fun readHist(context: Context): MutableMap<String, Int> {
        val raw = prefs(context).getString(KEY_TIME_HIST, null) ?: return mutableMapOf()
        return try {
            val o = JSONObject(raw)
            CurioPet.TimeOfDay.entries.associateTo(mutableMapOf()) { it.name to o.optInt(it.name) }
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun writeHist(context: Context, hist: Map<String, Int>) {
        val o = JSONObject()
        hist.forEach { (k, v) -> o.put(k, v) }
        prefs(context).edit().putString(KEY_TIME_HIST, o.toString()).apply()
    }

    private fun histTotal(context: Context): Int = readHist(context).values.sum()

    // ── Catchphrase book (the pet's own developed lines) ───────────────
    private fun readCoined(context: Context): MutableList<String> {
        val raw = prefs(context).getString(KEY_COINED, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun writeCoined(context: Context, coined: List<String>) {
        prefs(context).edit().putString(KEY_COINED, JSONArray(coined).toString()).apply()
    }

    /** How many catchphrases the pet has developed so far (UI/curiosity). */
    fun coinedCount(context: Context): Int = readCoined(context).size

    // ── Observation — every real action feeds the model ────────────────
    /** A screen visit (called from [CurioPet.bubbleFor]): histogram + decay. */
    fun observeActivity(context: Context, timeOfDay: CurioPet.TimeOfDay) {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        // Trait decay: long idle pulls the personality back toward baseline
        // so it stays responsive instead of freezing at old maxima.
        val last = p.getLong(KEY_OBSERVED_AT, now)
        val idleDays = (now - last).coerceAtLeast(0L) / 86_400_000L
        if (idleDays >= 1) {
            val traits = readTraits(context)
            val decay = 0.03f * idleDays
            traits[Trait.CURIOSITY] = (traits[Trait.CURIOSITY]!! - decay).coerceAtLeast(0.10f)
            traits[Trait.PLAYFULNESS] = (traits[Trait.PLAYFULNESS]!! - decay).coerceAtLeast(0.10f)
            traits[Trait.WARMTH] = (traits[Trait.WARMTH]!! - decay).coerceAtLeast(0.08f)
            traits[Trait.ENERGY] = (traits[Trait.ENERGY]!! - decay).coerceAtLeast(0.20f)
            writeTraits(context, traits)
        }
        p.edit().putLong(KEY_OBSERVED_AT, now).apply()
        // Time-of-day histogram; ENERGY rides how present the user is;
        // NIGHT_OWL tracks the share of activity after dark.
        val hist = readHist(context)
        hist[timeOfDay.name] = (hist[timeOfDay.name] ?: 0) + 1
        val total = hist.values.sum().coerceAtLeast(1)
        val nightShare = (hist[CurioPet.TimeOfDay.NIGHT.name] ?: 0).toFloat() / total
        val traits = readTraits(context)
        traits[Trait.ENERGY] = (traits[Trait.ENERGY]!! * 0.85f + 0.15f).coerceIn(0f, 1f)
        traits[Trait.NIGHT_OWL] = (traits[Trait.NIGHT_OWL]!! * 0.9f + nightShare * 0.1f).coerceIn(0f, 1f)
        writeTraits(context, traits)
        writeHist(context, hist)
        maybeCoin(context, total)
    }

    /** A lane was explored for the first time (from noteLaneExplored). */
    fun observeExplore(context: Context) {
        shift(context, Trait.CURIOSITY, 0.08f)
        shift(context, Trait.ENERGY, 0.04f)
    }

    /** A level-up (from noteLevelUp) — the deep warmth moments. */
    fun observeLevelUp(context: Context) {
        shift(context, Trait.WARMTH, 0.12f)
        shift(context, Trait.CURIOSITY, 0.04f)
    }

    /** Any XP earned (from noteXpEarned) — slow warmth creep. */
    fun observeXp(context: Context) {
        shift(context, Trait.WARMTH, 0.004f)
    }

    /** A touch/boop (from noteTouch) — playfulness. */
    fun observeTouch(context: Context) {
        shift(context, Trait.PLAYFULNESS, 0.06f)
    }

    /** A play session (from notePlay) — playfulness, big bump. */
    fun observePlay(context: Context) {
        shift(context, Trait.PLAYFULNESS, 0.09f)
    }

    // ── Preference model — derived from the passport + histogram ───────
    private fun laneEngagement(context: Context, lane: CurioCategory): Int {
        val pr = CurioPassport.progress(context, lane.id)
        return pr.saves * 3 + pr.explores * 2 + pr.spins + pr.reveals
    }

    private fun laneEngagementShare(context: Context, lane: CurioCategory): Float {
        val total = CurioCategories.visible.sumOf { laneEngagement(context, it) }.coerceAtLeast(1)
        return laneEngagement(context, lane).toFloat() / total
    }

    /** The lane the user engages with most, once it's meaningfully dominant. */
    fun favoriteLane(context: Context): CurioCategory? {
        val best = CurioCategories.visible.maxByOrNull { laneEngagement(context, it) } ?: return null
        return best.takeIf { laneEngagementShare(context, best) > 0.25f }
    }

    /** The strongest learned trait — the pet's dominant voice. */
    fun dominantTrait(context: Context): Trait {
        val traits = readTraits(context)
        // NIGHT_OWL is a learned habit, not a voice — skip it here.
        return listOf(Trait.WARMTH, Trait.PLAYFULNESS, Trait.CURIOSITY)
            .maxByOrNull { traits[it] ?: 0f } ?: Trait.CURIOSITY
    }

    // ── Catchphrase coining — the pet develops its own things ──────────
    /**
     * Detect a strong recurring pattern and coin the pet's own line — at
     * most one per day, capped at [MAX_COINED] total. These persist and
     * surface in the pet's voice, so it visibly grows its own personality.
     */
    private fun maybeCoin(context: Context, observations: Int) {
        if (observations < 12) return // needs real history before it speaks up
        val p = prefs(context)
        val day = CurioQuests.todayEpochDay()
        if (p.getLong(KEY_LAST_COIN_DAY, -1L) == day) return
        p.edit().putLong(KEY_LAST_COIN_DAY, day).apply()

        val traits = readTraits(context)
        val coined = readCoined(context)
        val hist = readHist(context)
        val total = hist.values.sum().coerceAtLeast(1)
        val nightShare = (hist[CurioPet.TimeOfDay.NIGHT.name] ?: 0).toFloat() / total
        val morningShare = (hist[CurioPet.TimeOfDay.MORNING.name] ?: 0).toFloat() / total
        val lane = favoriteLane(context)
        val saves = CurioQuests.lifetimeState.saves
        val streak = CurioQuests.bestStreakState

        fun tryCoin(condition: Boolean, phrase: String): Boolean {
            if (condition && phrase !in coined && coined.size < MAX_COINED) {
                coined += phrase
                writeCoined(context, coined)
                return true
            }
            return false
        }
        // Ordered from the most personal to the most generic.
        // All coined lines are one sentence (spec §10.7 — they surface as
        // passive bubbles).
        if (tryCoin(traits[Trait.NIGHT_OWL]!! > 0.55f && nightShare > 0.4f, "The night deck is OUR thing now.")) return
        if (tryCoin(morningShare > 0.4f, "Morning spins — that's our little ritual.")) return
        if (tryCoin(lane != null && laneEngagementShare(context, lane) > 0.6f, "I'll always cheer for ${lane.displayName}.")) return
        if (tryCoin(streak >= 7, "A full week together, and I'm keeping count.")) return
        if (tryCoin(saves >= 10, "Every saved spark is a little memory we share.")) return
        if (tryCoin(traits[Trait.PLAYFULNESS]!! > 0.55f, "Boops are my love language — just so you know.")) return
        if (tryCoin(traits[Trait.CURIOSITY]!! > 0.55f, "New lanes are our favorite kind of adventure.")) return
        if (tryCoin(traits[Trait.WARMTH]!! > 0.5f, "We've grown a lot together — I can feel it.")) return
    }

    // ── Voice — one-sentence, fully grounded lines in the learned voice ─
    /**
     * A personalized one-liner. Returns null when the brain is OFF or too
     * young (fewer than ~6 screen visits), so the caller falls back to the
     * classic rule-based library.
     */
    fun say(context: Context, mood: CurioPet.Mood, lanes: Set<String>): String? {
        if (!AppPreferences.petBrainEnabledState) return null
        if (histTotal(context) < 6) return null
        val traits = readTraits(context)
        val voice = dominantTrait(context)
        val lane = favoriteLane(context)
        val level = CurioQuests.levelForXp(CurioQuests.xpState)
        val streak = CurioQuests.bestStreakState
        val saves = CurioQuests.lifetimeState.saves
        val coined = readCoined(context)
        // v8.43 — familiar lines stay gated behind the FRIEND bond tier,
        // matching the classic library's intimacy rule (v8.29): a brand-new
        // pet never acts like an old friend.
        val warm = CurioPet.bond().ordinal >= CurioPet.Bond.FRIEND.ordinal
        // Coined catchphrases surface often once developed.
        if (coined.isNotEmpty() && (0..9).random() < 3) return coined.random()
        val opening = when (voice) {
            Trait.CURIOSITY -> listOf("Ooh", "Hmm", "I wonder").random()
            Trait.PLAYFULNESS -> listOf("Wheee", "Hehe", "Boop").random()
            // dominantTrait only ever returns CURIOSITY/PLAYFULNESS/WARMTH.
            else -> (if (warm) listOf("Hey you", "Friend", "Glad you're here")
            else listOf("Hey", "Hello", "Well now")).random()
        }
        // One sentence max (spec §10.7), fully grounded in real stats.
        val body = when (mood) {
            CurioPet.Mood.PROUD -> "Level $level — we earned that."
            CurioPet.Mood.EXCITED -> "Fresh ground again — I can feel it."
            CurioPet.Mood.CURIOUS ->
                lane?.let { "We keep circling ${it.displayName} — let's go for real?" }
                    ?: "Something new is calling us."
            CurioPet.Mood.HAPPY -> when {
                streak >= 7 -> if (warm) {
                    "Day $streak of our streak, and I'm keeping count."
                } else {
                    "Seven days of spinning — that's a real rhythm."
                }
                lane != null -> "I like that we keep coming back to ${lane.displayName}."
                saves >= 5 -> if (warm) {
                    "$saves sparks saved — the shelf is ours."
                } else {
                    "$saves sparks saved, and the shelf is growing."
                }
                else -> "More of this, please."
            }
            CurioPet.Mood.SLEEPY -> "Even my glow dims for you — I'll be here tomorrow."
            CurioPet.Mood.FOCUSED -> "Write it down — I'll keep watch."
            CurioPet.Mood.BOUNCY -> "That game did me good — again soon?"
        }
        return "$opening, $body"
    }
}
