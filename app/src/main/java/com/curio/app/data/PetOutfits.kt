package com.curio.app.data

/**
 * Pet outfit catalog (v9.x) — purchasable accessory layers funded by
 * sparkles. An outfit is a 16×16 pixel grid drawn on the pet's
 * `accessories` detail layer (resized to the design's grid by the sprite).
 *
 * Keys use the default pet palette (s/S coral scarf, G gold, c/C/d/D
 * pastels, o ink) so outfits read correctly on the default and evolved
 * designs; a custom design's palette is applied as-is — unknown keys fall
 * back to the default palette via [PetDesign.colorFor].
 *
 * Shop pricing is deliberately gentle: daily claims pay ~2 sparkles and
 * streak milestones ~5, so a 20-sparkle outfit is a few days of play, not
 * a grind (pure cosmetics — no real-money path anywhere).
 */
object PetOutfits {

    data class Outfit(
        val id: String,
        val name: String,
        val glyph: String,
        /** Sparkle price. */
        val price: Int,
        /** Level required to unlock in the shop (see [LevelRewards]). */
        val levelRequired: Int,
        /** The level-reward id that unlocks this outfit ("" = shop-only). */
        val rewardId: String = "",
        /** 16×16 accessory art — every row exactly 16 chars. */
        val art: List<String>
    )

    /** Purchasable outfits, ordered by price (shop display order). */
    val Catalog: List<Outfit> = listOf(
        Outfit(
            id = "scarf",
            name = "Explorer Scarf",
            glyph = "pets",
            price = 20,
            levelRequired = 3,
            rewardId = "outfit-scarf",
            art = listOf(
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "......ssssss....",
                ".....s......s...",
                "....s........s..",
                ".....s......s...",
                "......ssssss....",
                "................",
                "................"
            )
        ),
        Outfit(
            id = "coat",
            name = "Scholar Coat",
            glyph = "pets",
            price = 45,
            levelRequired = 10,
            rewardId = "outfit-coat",
            art = listOf(
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "..CCCCCCCCCCCC..",
                "..C..........C..",
                "..C....SS....C..",
                "..C....SS....C..",
                "..CCCCCCCCCCCC..",
                "...C........C...",
                "...C........C...",
                "....CCCCCCCC....",
                "................",
                "................"
            )
        ),
        Outfit(
            id = "crown",
            name = "Curio Crown",
            glyph = "workspace_premium",
            price = 80,
            levelRequired = 20,
            rewardId = "outfit-crown",
            art = listOf(
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "..G......G......",
                ".G.G....G.G.....",
                "G...G..G...G....",
                "GGGGGGGGGGGGGG..",
                ".GGGGGGGGGGGG...",
                "..GGGGGGGGGG....",
                "................",
                "................",
                "................",
                "................"
            )
        ),
        Outfit(
            id = "bowtie",
            name = "Polka Bowtie",
            glyph = "pets",
            price = 12,
            levelRequired = 5,
            art = listOf(
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".....ss..ss.....",
                "......ssss......",
                ".....ssssss.....",
                "......ssss......",
                ".......ss.......",
                "................",
                "................",
                "................",
                "................"
            )
        ),
        Outfit(
            id = "sunhat",
            name = "Sun Hat",
            glyph = "pets",
            price = 55,
            levelRequired = 15,
            art = listOf(
                "................",
                "................",
                ".oooooooooooooo.",
                ".o............o.",
                "..oooooooooooo..",
                "...oooooooooo...",
                "....oooooooo....",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"
            )
        ),
        Outfit(
            id = "glasses",
            name = "Curio Glasses",
            glyph = "pets",
            price = 75,
            levelRequired = 25,
            art = listOf(
                "................",
                "................",
                "................",
                "................",
                "....oo....oo....",
                "...oooo..oooo...",
                "....oo....oo....",
                "......oooo......",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"
            )
        ),
        Outfit(
            id = "tailpuff",
            name = "Tail Puff",
            glyph = "pets",
            price = 100,
            levelRequired = 35,
            art = listOf(
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "...........oo...",
                "..........o..o..",
                "..........o..o..",
                "...........oo...",
                "................",
                "................",
                "................"
            )
        ),
        Outfit(
            id = "galaxy",
            name = "Galaxy Drifter",
            glyph = "auto_awesome",
            price = 140,
            levelRequired = 40,
            rewardId = "outfit-galaxy",
            art = listOf(
                "................",
                "................",
                "................",
                "................",
                "................",
                ".d..............",
                "..C....d........",
                "...DDD...C......",
                "......DDD...d...",
                ".....c...DDD....",
                ".........c..C...",
                "..............c.",
                "................",
                "................",
                "................",
                "................"
            )
        )
    )

    /** The outfit with [id], or null. */
    fun byId(id: String?): Outfit? = Catalog.firstOrNull { it.id == id }

    /** Whether [outfit] is unlocked at [level] (level + reward gate). */
    fun isUnlocked(outfit: Outfit, level: Int): Boolean =
        level >= outfit.levelRequired &&
            (outfit.rewardId.isEmpty() || LevelRewards.isUnlocked(level, outfit.rewardId))

    // ── Toys & games — one-time sparkle purchases that unlock a play mode
    //    with Curie (v323). Each game is level-gated first (some tie to a
    //    LevelRewards.GAME reward), then purchasable; owning it unlocks the
    //    "Play" action in the shop, which fires a real pet play moment.
    data class PetGame(
        val id: String,
        val name: String,
        val glyph: String,
        /** Sparkle price. */
        val price: Int,
        /** Level required to unlock in the shop. */
        val levelRequired: Int,
        /** The level-reward id that unlocks this game ("" = shop-only). */
        val rewardId: String = "",
        /** One-line flavor shown under the name. */
        val tagline: String
    )

    val Games: List<PetGame> = listOf(
        PetGame("ball", "Ball fetch", "play_circle", 25, 12, "game-ball", "Toss the ball — Curie fetches!"),
        PetGame("starcatch", "Star catch", "star", 60, 25, "game-starcatch", "Catch falling stars together."),
        PetGame("bubbles", "Bubble storm", "bubble_chart", 90, 35, "game-bubbles", "A storm of bubbles to pop.")
    )

    /** The game with [id], or null. */
    fun gameById(id: String?): PetGame? = Games.firstOrNull { it.id == id }

    /** Whether [game] is unlocked at [level] (level + reward gate). */
    fun isGameUnlocked(game: PetGame, level: Int): Boolean =
        level >= game.levelRequired &&
            (game.rewardId.isEmpty() || LevelRewards.isUnlocked(level, game.rewardId))
}