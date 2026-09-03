package com.curio.app.data

import kotlin.math.min

/**
 * v208 — CURIO BRAIN STATS: a real, science-based cognitive model that
 * replaces the constellation's save-count stars (user: \"a real science
 * based stats system that will help user improve their brain in a certain
 * ways and it shows the knowledge based on the category user explored and
 * the amout of wrting user does etc etc, and this will be rplaced the
 * category stars glow from costelation\").
 *
 * The model has two layers:
 *  1. PER-LANE KNOWLEDGE ([LaneKnowledge]) — how much knowledge you've
 *     actually BUILT in each category: explores + saves + words written in
 *     that lane. The constellation stars size by this score (not by saved
 *     count) and glow when the lane was recently active.
 *  2. THE BRAIN PROFILE ([brainProfile]) — six cognitive dimensions, each
 *     mapped to a real learning-science mechanism, so the user can see
 *     which brain muscle they're training and how to sharpen it:
 *       - KNOWLEDGE   — breadth × depth of explored domains (cumulative,
 *                        connected knowledge building).
 *       - MEMORY      — saves + pins + quotes kept (retrieval-practice
 *                        effect: stored cues strengthen recall).
 *       - EXPRESSION  — words actually written (the GENERATION EFFECT:
 *                        producing language consolidates learning).
 *       - FOCUS       — explore sessions + daily quests (sustained
 *                        attention practice).
 *       - CONSISTENCY — best streak (the SPACING EFFECT: distributed
 *                        practice builds durable memory).
 *       - CURIOSITY   — spins + lanes sampled (novelty-seeking, the engine
 *                        of exploration).
 *
 * Everything is computed from REAL user data already recorded (passport
 * per-lane counters, saved captures, lifetime counters, streak) — nothing
 * is guessed.
 */

/** Real word count of the text the user WROTE in this capture — every text
 *  field (journal, review, notes, field notes, captions, quotes), recursive
 *  through portfolios and the wildcard notebook. Voice transcripts are NOT
 *  counted (they're machine-transcribed speech, not the user's writing). */
fun CaptureData.wordCount(): Int {
    fun words(s: String?): Int =
        if (s.isNullOrBlank()) 0 else s.trim().split(Regex("\\s+")).size
    return when (this) {
        is CaptureData.SoundBite -> words(note) + quotes.orEmpty().sumOf { words(it) }
        is CaptureData.ReelNotes -> words(reviewText) + quotes.orEmpty().sumOf { words(it) }
        is CaptureData.Marginalia -> words(journalText) + quotes.orEmpty().sumOf { words(it) }
        is CaptureData.GalleryWall -> words(caption) + quotes.orEmpty().sumOf { words(it) }
        is CaptureData.FieldNotes -> words(observed) + words(surprised) + words(learnNext)
        is CaptureData.OpenNotebook -> subData.wordCount()
        is CaptureData.Portfolio -> sections.orEmpty().sumOf { it.data.wordCount() }
    }
}

/** Word count across an entry's whole capture (any format). */
fun CurioEntry.wordCount(): Int = captureData.wordCount()

/**
 * One lane's knowledge — the constellation star's data. [score] is what the
 * star SIZE shows (what you've built in that domain, not just how many
 * cards you saved); [lastAt] drives the recent glow.
 */
data class LaneKnowledge(
    val saves: Int,
    val explores: Int,
    val spins: Int,
    val words: Int,
    val lastAt: Long
) {
    /** Knowledge score in this lane — explores and saves are the core
     *  knowledge acts, writing deepens them, spins nudge. Saturation caps
     *  naturally in the constellation (star radius caps at 60). */
    val score: Int
        get() = explores * 30 + saves * 40 + words / 20 + spins * 2

    val explored: Boolean
        get() = explores > 0 || saves > 0
}

/** Merge the passport's per-lane counters with the words written per lane
 *  (from saved captures) into one knowledge map. Both the drawer and the
 *  Your Curiosity page feed the constellation from this so they can never
 *  drift apart. */
fun laneKnowledge(
    progress: Map<CategoryId, CurioPassport.CategoryProgress>,
    entries: List<CurioEntry>
): Map<CategoryId, LaneKnowledge> {
    val byLane = entries.groupBy { it.topic.categoryId }
    return progress.mapValues { (id, p) ->
        val laneEntries = byLane[id].orEmpty()
        LaneKnowledge(
            saves = p.saves,
            explores = p.explores,
            spins = p.spins,
            words = laneEntries.sumOf { it.wordCount() },
            lastAt = p.lastAt
        )
    }
}

/** One cognitive dimension in the brain profile. */
data class BrainDimension(
    val id: String,
    val name: String,
    val icon: String,
    /** 0..100 — how developed this brain muscle is. */
    val score: Int,
    val level: String,
    /** A real, actionable way to sharpen it (science-based). */
    val tip: String
)

/** Level labels for a 0..100 dimension score. */
private fun dimensionLevel(score: Int): String = when {
    score >= 80 -> "Mastered"
    score >= 60 -> "Strong"
    score >= 40 -> "Developing"
    score >= 20 -> "Growing"
    else -> "Awakening"
}

private fun norm(v: Int, cap: Int): Float = min(1f, v.toFloat() / cap)

/**
 * The six-dimension brain profile, computed from real activity.
 * [bestStreak] is the longest streak (spacing effect); [totalLanes] is the
 * full category count (knowledge breadth denominator).
 */
fun brainProfile(
    progress: Map<CategoryId, CurioPassport.CategoryProgress>,
    entries: List<CurioEntry>,
    lifetime: CurioQuests.LifetimeCounters,
    bestStreak: Int,
    totalLanes: Int
): List<BrainDimension> {
    val lanesExplored = progress.values.count { it.explores > 0 || it.saves > 0 }
    val totalExplores = progress.values.sumOf { it.explores }
    val totalSaves = progress.values.sumOf { it.saves }
    val totalWords = entries.sumOf { it.wordCount() }
    val totalSpins = progress.values.sumOf { it.spins }

    val breadth = norm(lanesExplored, totalLanes.coerceAtLeast(1))
    val depth = norm(totalExplores + totalSaves, 60)
    val knowledge = ((breadth * 0.55f + depth * 0.45f) * 100).toInt()

    val memoryRaw = totalSaves + lifetime.pins + lifetime.quotes
    val memory = (norm(memoryRaw, 50) * 100).toInt()

    val expression = (norm(totalWords, 2_000) * 100).toInt()

    val focusRaw = totalExplores + lifetime.dailyCompleted
    val focus = (norm(focusRaw, 80) * 100).toInt()

    val consistency = (norm(bestStreak, 30) * 100).toInt()

    val curiosityRaw = totalSpins + lanesExplored
    val curiosity = (norm(curiosityRaw, 150) * 100).toInt()

    return listOf(
        BrainDimension(
            "knowledge", "Knowledge", "explore", knowledge, dimensionLevel(knowledge),
            "Sample a new lane; a broad web of connected facts is how knowledge compounds."
        ),
        BrainDimension(
            "memory", "Memory", "bookmark", memory, dimensionLevel(memory),
            "Revisit a saved capture today; retrieval practice is the strongest memory builder."
        ),
        BrainDimension(
            "expression", "Expression", "edit_note", expression, dimensionLevel(expression),
            "Write a few journal lines after each explore; the generation effect deepens learning."
        ),
        BrainDimension(
            "focus", "Focus", "hub", focus, dimensionLevel(focus),
            "Start an explore session and stay in it; sustained attention trains focus."
        ),
        BrainDimension(
            "consistency", "Consistency", "local_fire_department", consistency, dimensionLevel(consistency),
            "Keep the streak alive; spaced practice is how memory consolidates."
        ),
        BrainDimension(
            "curiosity", "Curiosity", "auto_awesome", curiosity, dimensionLevel(curiosity),
            "Spin into unfamiliar lanes; novelty-seeking is the engine of learning."
        )
    )
}
