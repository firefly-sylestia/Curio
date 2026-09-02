package com.curio.app.data

/**
 * Level rewards — what hitting a level actually UNLOCKS (v9.x).
 *
 * Levels were already named; this catalog gives XP a reason: each milestone
 * unlocks one concrete thing. Reward kinds:
 *  - [RewardKind.OUTFIT] — a new purchasable pet outfit (see [PetOutfits]).
 *  - [RewardKind.PALETTE] — a new premium share-card tone (see the curated
 *    tone list in TopicShareCard).
 *  - [RewardKind.LANE_ORDER] — the drag-reorder in Manage Categories.
 *
 * Reads are pure (no prefs) — a reward is unlocked iff the player's level
 * is at or past its [level]. The Quests screen shows the next unlock on the
 * level card, and the level-up banner lists everything newly unlocked.
 */
object LevelRewards {

    enum class RewardKind { OUTFIT, PALETTE, LANE_ORDER }

    data class Reward(
        val level: Int,
        val id: String,
        val title: String,
        val kind: RewardKind,
        /** Material-Symbols glyph shown beside the reward. */
        val glyph: String
    )

    val Catalog: List<Reward> = listOf(
        Reward(2, "palette-midnight", "Midnight share-card tone", RewardKind.PALETTE, "dark_mode"),
        Reward(3, "outfit-scarf", "Explorer Scarf outfit", RewardKind.OUTFIT, "pets"),
        Reward(5, "lane-order", "Custom lane order", RewardKind.LANE_ORDER, "drag_handle"),
        Reward(8, "palette-forest", "Forest share-card tone", RewardKind.PALETTE, "pets"),
        Reward(10, "outfit-coat", "Scholar Coat outfit", RewardKind.OUTFIT, "pets"),
        Reward(15, "palette-lavender", "Lavender share-card tone", RewardKind.PALETTE, "dark_mode"),
        Reward(20, "outfit-crown", "Curio Crown outfit", RewardKind.OUTFIT, "workspace_premium"),
        Reward(30, "palette-ember", "Ember share-card tone", RewardKind.PALETTE, "local_fire_department"),
        Reward(40, "outfit-galaxy", "Galaxy Drifter outfit", RewardKind.OUTFIT, "auto_awesome")
    )

    /** All rewards earned at or below [level]. */
    fun unlockedFor(level: Int): List<Reward> = Catalog.filter { level >= it.level }

    /** Rewards that become available when crossing [fromLevel] → [toLevel]. */
    fun newlyUnlocked(fromLevel: Int, toLevel: Int): List<Reward> =
        Catalog.filter { it.level in (fromLevel + 1)..toLevel }

    /** The next reward the player hasn't reached yet, or null at max level. */
    fun nextReward(level: Int): Reward? = Catalog.firstOrNull { it.level > level }

    /** True once [id] is unlocked at [level]. */
    fun isUnlocked(level: Int, id: String): Boolean =
        Catalog.firstOrNull { it.id == id }?.let { level >= it.level } ?: false

    /** How many premium palette tones the player has unlocked at [level]. */
    fun unlockedPaletteCount(level: Int): Int =
        Catalog.count { it.kind == RewardKind.PALETTE && level >= it.level }

    /** The lane-order reward (used to gate Manage Categories reorder). */
    val laneOrderReward: Reward? get() = Catalog.firstOrNull { it.kind == RewardKind.LANE_ORDER }
}