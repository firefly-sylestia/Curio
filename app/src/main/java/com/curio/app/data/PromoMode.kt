package com.curio.app.data

/**
 * Promo/demo-content mode (v7.107) — the hidden promotional state the app
 * shows when `AppPreferences.promoModeState` is ON (unlocked by tapping the
 * Version row in Support & diagnostics five times; tap five times again to
 * turn it OFF).
 *
 * While ON, the app surfaces promotional SAMPLE content — derived entirely
 * from real topics via [TopicCatalog.sampleEntries] and the loaded pools —
 * so the user can screenshot Home, Profile, Quests and the Cabinet for
 * store promotion. No user data is read or written; turning the mode off
 * instantly reverts every screen to real data (all consumers read the
 * reactive [AppPreferences.promoModeState]).
 */
object PromoMode {

    /** Demo Streak value shown in the Home + Profile hero stat bars. */
    const val DEMO_STREAK = 27

    /** Demo Cabinet count shown in the Home + Profile hero stat bars. */
    const val DEMO_SAVED = 128

    /**
     * Demo XP — deliberately past the final threshold of the 50-rank curve
     * (top rank ≈ 12.1k XP), so the promo level card reads "Curio Sovereign"
     * with a full bar and never lands one rank short.
     */
    const val DEMO_XP = 20000

    /**
     * The six fully-working demo entries — one per capture format, built
     * from real topics by [TopicCatalog.sampleEntries]. Tapping any of
     * them opens a REAL detail page (EntryDetail falls back to the sample
     * pool for `sample-*` ids), so demo screenshots stay fully tappable.
     */
    suspend fun demoEntries(): List<CurioEntry> =
        runCatching { TopicCatalog.sampleEntries() }.getOrDefault(emptyList())

    /**
     * "Explored" markers derived from the same demo entries — the Home
     * recents feed pairs each saved-entry card with its explored row (the
     * normal feed's shape), so the demo feed renders with the familiar
     * Explore-topic + Saved-entry mix.
     */
    fun demoExplored(entries: List<CurioEntry>): List<ExploredTopic> =
        entries.map { entry ->
            ExploredTopic(
                categoryId = entry.topic.categoryId,
                topicName = entry.topic.name,
                exploredAtMillis = entry.capturedAtMillis,
                wasUnexplored = false
            )
        }

    /**
     * Real total topic count across all loaded pools — shown on the promo
     * poster's stat strip ("…+ topics") so the number is honest, not fake.
     * Delegates to [TopicCatalog.totalTopicCount] (the same source of
     * truth the Home hero's Topics stat uses), so the two can never drift.
     */
    fun topicTotal(): Int = TopicCatalog.totalTopicCount()
}
