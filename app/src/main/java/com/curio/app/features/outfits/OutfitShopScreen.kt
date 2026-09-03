package com.curio.app.features.outfits

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.data.LevelRewards
import com.curio.app.data.PetDesign
import com.curio.app.data.PetOutfits
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.pet.CurioPetSprite
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioSageInk
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * v9.x — the PET OUTFIT SHOP: spend sparkles (earned from daily/weekly
 * claims + streak milestones) on pure-cosmetic accessory outfits for Curie.
 * Every outfit is level-gated first (LevelRewards), then purchasable, then
 * equippable — the equipped outfit overlays the pet sprite everywhere.
 */
@Composable
fun OutfitShopScreen(navController: NavController) {
    val context = LocalContext.current
    val level = CurioQuests.levelForXp(CurioQuests.xpState)
    val sparkles = AppPreferences.sparklesState
    val owned = AppPreferences.ownedOutfitsState
    val equipped = AppPreferences.equippedOutfitState
    // v323 — owned toys/games unlock a "Play" action (a real pet moment).
    val ownedGames = AppPreferences.ownedGamesState
    var toast by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    val glassBackdrop = rememberLayerBackdrop()

    // Preview sprites: the outfit layered over a neutral default body so the
    // shop card always shows exactly what you'd get.
    val previewDesign = remember { PetDesign.evolutionDesign(CurioPet.Stage.FIRST_EVO, null) }

    fun buy(outfit: PetOutfits.Outfit) {
        if (outfit.id in owned) return
        if (!PetOutfits.isUnlocked(outfit, level)) {
            toast = "Unlocks at Level ${outfit.levelRequired}"
            return
        }
        if (!AppPreferences.spendSparkles(context, outfit.price)) {
            toast = "Not enough sparkles"
            return
        }
        AppPreferences.buyOutfit(context, outfit.id)
        AppPreferences.setEquippedOutfit(context, outfit.id)
        toast = "${outfit.name} equipped!"
    }

    // v323 — one-time toy purchase; owning a game unlocks its Play action.
    fun buyGame(game: PetOutfits.PetGame) {
        if (game.id in ownedGames) return
        if (!PetOutfits.isGameUnlocked(game, level)) {
            toast = "Unlocks at Level ${game.levelRequired}"
            return
        }
        if (!AppPreferences.spendSparkles(context, game.price)) {
            toast = "Not enough sparkles"
            return
        }
        AppPreferences.buyGame(context, game.id)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        toast = "${game.name} unlocked!"
    }

    // v323 — Play a purchased toy: a real pet play moment (feeds the
    // "Play with your pet" daily + the pet's persona) with a confirm buzz.
    fun playGame(game: PetOutfits.PetGame) {
        CurioPet.notePlay(context)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        toast = "Playing ${game.name} with Curie!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(heroPageBackground())
    ) {
        ScreenEntrance {
            LazyColumn(
                modifier = Modifier
                    .layerBackdrop(glassBackdrop)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = SettingsHeroTotalHeight,
                    bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Sparkle balance — the shop's wallet ────────────────
                item("wallet") {
                    CurioSettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CurioIcon(
                                        name = CurioIcons.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 24.dp
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$sparkles sparkles",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Earn sparkles by claiming daily & weekly quests and new streak milestones.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                // ── Outfit cards ───────────────────────────────────────
                items(PetOutfits.Catalog, key = { it.id }) { outfit ->
                    val unlocked = PetOutfits.isUnlocked(outfit, level)
                    val isOwned = outfit.id in owned
                    val isEquipped = equipped == outfit.id
                    val price = outfit.price
                    val design = remember(previewDesign, outfit) {
                        val resized = PetDesign.resizeGrid(outfit.art, 16, previewDesign.gridSize)
                        previewDesign.withDetailGrid("accessories", resized)
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                            alpha = if (unlocked) 1f else 0.6f
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Sprite preview.
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CurioPetSprite(
                                    stage = CurioPet.Stage.FIRST_EVO,
                                    mood = CurioPet.Mood.HAPPY,
                                    spriteSize = 56.dp,
                                    design = design,
                                    staticPose = true,
                                    pointerAware = false
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CurioIcon(
                                        name = outfit.glyph,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 16.dp
                                    )
                                    Text(
                                        text = outfit.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = when {
                                        !unlocked -> "Unlocks at Level ${outfit.levelRequired}"
                                        isEquipped -> "Equipped · ${outfit.id.capitalizeFirst()}"
                                        isOwned -> "Owned — tap to equip"
                                        else -> "Level ${outfit.levelRequired} · $price sparkles"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = curioSageInk(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Buy / Equip pill.
                            Surface(
                                onClick = { buy(outfit) },
                                shape = CircleShape,
                                color = when {
                                    isEquipped -> MaterialTheme.colorScheme.surfaceVariant
                                    isOwned -> MaterialTheme.colorScheme.secondaryContainer
                                    unlocked -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                modifier = Modifier.size(width = 88.dp, height = 38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when {
                                            isEquipped -> "On"
                                            isOwned -> "Equip"
                                            unlocked -> "Buy"
                                            else -> "Locked"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = when {
                                            isEquipped || !unlocked -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.onPrimary
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                // ── Toys & games (v323) — one-time sparkle purchases that
                // unlock a play mode with Curie. Level-gated first (some tie
                // to a LevelRewards.GAME reward), then Buy → Play.
                item("games-header") {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text(
                            "Toys & games",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "One-time buys that unlock play modes with Curie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(PetOutfits.Games, key = { "game-" + it.id }) { game ->
                    val unlocked = PetOutfits.isGameUnlocked(game, level)
                    val isOwned = game.id in ownedGames
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                            alpha = if (unlocked) 1f else 0.6f
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CurioIcon(
                                    name = game.glyph,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    size = 22.dp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = game.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = when {
                                        !unlocked -> "Unlocks at Level ${game.levelRequired}"
                                        isOwned -> game.tagline
                                        else -> "Level ${game.levelRequired} · ${game.price} sparkles"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = curioSageInk(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                onClick = { if (isOwned) playGame(game) else buyGame(game) },
                                shape = CircleShape,
                                color = when {
                                    isOwned -> MaterialTheme.colorScheme.secondaryContainer
                                    unlocked -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                modifier = Modifier.size(width = 88.dp, height = 38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when {
                                            !unlocked -> "Locked"
                                            isOwned -> "Play"
                                            else -> "Buy"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = when {
                                            isOwned -> MaterialTheme.colorScheme.onSecondaryContainer
                                            unlocked -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                item("unlock-note") {
                    Text(
                        text = levelUnlockHint(level),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
        // Sticky hero (the settings-family torn banner).
        SettingsHeroHeader(
            title = "Pet shop",
            subtitle = "Outfits & toys for Curie · sparkles only",
            onBack = { navController.popBackStack() },
            glassBackdrop = glassBackdrop
        )
        // Toast — transient feedback for buy/unlock/equip.
        toast?.let { message ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private fun String.capitalizeFirst(): String =
    if (isEmpty()) this else replaceFirstChar { it.uppercase() }

/** A one-line hint about the next outfit/palette/lane-order unlock. */
private fun levelUnlockHint(level: Int): String {
    val next = LevelRewards.nextReward(level) ?: return "Max level — every reward unlocked!"
    return "Next unlock at Level ${next.level}: ${next.title}"
}