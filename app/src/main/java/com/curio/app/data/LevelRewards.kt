package com.curio.app.data

/**
 * Level rewards — what hitting a level actually UNLOCKS (v9.x).
 *
 * Levels were already named; this catalog gives XP a reason: each milestone
 * unlocks one concrete thing. Reward kinds:
 *  - [RewardKind.OUTFIT] — a new purchasable pet outfit (see [PetOutfits]).
 *  - [RewardKind.PALETTE] — a new premium share-card tone (see the curated
 *    tone list in TopicShareCard).
 *  - [RewardKind.GAME] — a purchasable pet mini-game (see [PetOutfits.PetGame]).
 *  - [RewardKind.LANE_ORDER] — the drag-reorder in Manage Categories.
 *
 * Reads are pure (no prefs) — a reward is unlocked iff the player's level
 * is at or past its [level]. The Quests screen shows the next unlock on the
 * level card, and the level-up banner lists everything newly unlocked.
 */
object LevelRewards {

    enum class RewardKind { OUTFIT, PALETTE, GAME, LANE_ORDER }

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
        Reward(12, "palette-ocean", "Ocean share-card tone", RewardKind.PALETTE, "dark_mode"),
        Reward(12, "game-ball", "Ball fetch game", RewardKind.GAME, "play_circle"),
        Reward(15, "palette-lavender", "Lavender share-card tone", RewardKind.PALETTE, "dark_mode"),
        Reward(18, "palette-rosegold", "Rose Gold share-card tone", RewardKind.PALETTE, "dark_mode"),
        Reward(20, "outfit-crown", "Curio Crown outfit", RewardKind.OUTFIT, "workspace_premium"),
        Reward(25, "palette-moss", "Moss share-card tone", RewardKind.PALETTE, "dark_mode"),
        Reward(25, "game-starcatch", "Star catch game", RewardKind.GAME, "star"),
        Reward(30, "palette-ember", "Ember share-card tone", RewardKind.PALETTE, "local_fire_department"),
        Reward(35, "palette-storm", "Storm share-card tone", RewardKind.PALETTE, "dark_mode"),
        Reward(35, "game-bubbles", "Bubble storm game", RewardKind.GAME, "bubble_chart"),
        Reward(40, "outfit-galaxy", "Galaxy Drifter outfit", RewardKind.OUTFIT, "auto_awesome"),
        Reward(45, "palette-pearl", "Pearl share-card tone", RewardKind.PALETTE, "dark_mode"),
        Reward(50, "palette-sunburst", "Sunburst share-card tone", RewardKind.PALETTE, "local_fire_department")
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

    /** All game unlocks the player has earned at [level] (see [PetOutfits.PetGame]). */
    fun unlockedGameRewards(level: Int): Set<String> =
        Catalog.filter { it.kind == RewardKind.GAME && level >= it.level }.map { it.id }.toSet()

    /** The lane-order reward (used to gate Manage Categories reorder). */
    val laneOrderReward: Reward? get() = Catalog.firstOrNull { it.kind == RewardKind.LANE_ORDER }
}