package com.curio.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.CurioQuests
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * v8.27 — shared quest badge primitives (Quests page + Profile both pin
 * earned badges). The glyph/color mapping lives here so every medal draws
 * the same way everywhere.
 */

/** Every stage wears its OWN glyph, so badges stop sharing one icon per chain. */
fun badgeGlyph(stage: CurioQuests.QuestStage): String = when (stage.id) {
    // The Deck — spin milestones.
    "deck-1" -> "casino"
    "deck-3" -> "auto_awesome"
    "deck-5" -> "star"
    "deck-10" -> "layers"
    "deck-25" -> "replay"
    "deck-50" -> "refresh"
    "deck-100" -> "workspace_premium"
    // Discovery — explore milestones.
    "disc-1" -> "explore"
    "disc-3" -> "bolt"
    "disc-5" -> "public"
    "disc-10" -> "hub"
    "disc-25" -> "rocket_launch"
    "disc-lane3" -> "spa"
    "disc-lanes" -> "diamond"
    // Keepsakes — save milestones.
    "keep-1" -> "inventory_2"
    "keep-3" -> "bookmark"
    "keep-5" -> "local_library"
    "keep-10" -> "auto_stories"
    "keep-25" -> "library_books"
    "keep-50" -> "menu_book"
    "keep-100" -> "museum"
    "keep-formats" -> "photo_library"
    // The Tour — guided walkthrough.
    "tour-settings" -> "settings"
    "tour-profile" -> "person"
    "tour-pin" -> "bookmark_border"
    "tour-quote" -> "format_quote"
    "tour-daily" -> "schedule"
    "tour-achievement" -> "flag"
    // The Shelf — quote milestones.
    "quote-1" -> "format_quote"
    "quote-3" -> "auto_stories"
    "quote-5" -> "edit_note"
    // Pin Board — pin milestones.
    "pin-1" -> "bookmark"
    "pin-3" -> "bookmark_border"
    "pin-5" -> "star"
    // The Flame — streak milestones.
    "flame-1" -> "local_fire_department"
    "flame-3" -> "schedule"
    "flame-7" -> "calendar_today"
    "flame-14" -> "timer"
    "flame-30" -> "nightlight"
    // Taste — like milestones.
    "like-1" -> "thumb_up"
    "like-3" -> "sentiment_satisfied"
    "like-10" -> "star"
    // The Ladder — rank milestones.
    "rank-5" -> "flag"
    "rank-10" -> "workspace_premium"
    "rank-20" -> "auto_awesome"
    "rank-30" -> "diamond"
    "rank-40" -> "star"
    "rank-50" -> "rocket_launch"
    // Fallback — a sensible glyph per quest kind for any future stage.
    else -> when (stage.kind) {
        CurioQuests.QuestKind.SPIN -> "casino"
        CurioQuests.QuestKind.EXPLORE -> "explore"
        CurioQuests.QuestKind.SAVE -> "inventory_2"
        CurioQuests.QuestKind.SETTINGS -> "settings"
        CurioQuests.QuestKind.PROFILE -> "person"
        CurioQuests.QuestKind.PIN -> "bookmark"
        CurioQuests.QuestKind.QUOTE -> "format_quote"
        CurioQuests.QuestKind.DAILY -> "schedule"
        CurioQuests.QuestKind.ACHIEVEMENT -> "workspace_premium"
        CurioQuests.QuestKind.STREAK -> "local_fire_department"
        CurioQuests.QuestKind.LIKE -> "thumb_up"
        CurioQuests.QuestKind.FORMATS -> "photo_library"
        CurioQuests.QuestKind.LANES -> "public"
        CurioQuests.QuestKind.XP -> "star"
    }
}

/** Each chain's medals wear a distinct color, so the shelf reads like a set. */
fun chainBadgeColor(chainId: String?): Color = when (chainId) {
    "deck" -> CurioColors.DustyBlue
    "discovery" -> CurioColors.SkyMint
    "keepsakes" -> CurioColors.Teal
    "tour" -> CurioColors.CoralBlush
    "shelf" -> CurioColors.Peach
    "pinboard" -> CurioColors.Lilac
    "flame" -> CurioColors.FireOrange
    "taste" -> CurioColors.Sage
    "rank" -> CurioColors.ButterYellow
    else -> CurioColors.CoralBlush
}

/**
 * One badge MEDAL — a round medal with a double ring (v8.13 style, shared
 * so the Quests page and Profile pin the same badges). Earned badges show
 * the badge IN FULL: gradient medal + gold check + white glyph. Locked
 * badges are silhouette medals, so the shelf reads as a set of badges
 * rather than a list of chores.
 */
@Composable
fun CurioBadgeMedal(
    stage: CurioQuests.QuestStage,
    medalSize: Dp = 58.dp,
    modifier: Modifier = Modifier
) {
    val unlocked = CurioQuests.isStageDone(stage)
    val chainId = CurioQuests.Chains.firstOrNull { chain ->
        chain.stages.any { it.id == stage.id }
    }?.id
    val medalColor = chainBadgeColor(chainId)
    // Deepen the medal's lower stop so the white glyph always reads — even
    // on the pale gold/peach chains.
    val medalDeep = lerp(medalColor, Color.Black, 0.22f)
    Box(
        modifier = modifier.size(medalSize),
        contentAlignment = Alignment.Center
    ) {
        if (unlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(medalColor, medalDeep)))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(0.80f)
                .clip(CircleShape)
                .then(
                    if (unlocked) {
                        Modifier.border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(
                name = if (unlocked) badgeGlyph(stage) else CurioIcons.StarOutline,
                contentDescription = null,
                tint = if (unlocked) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                size = medalSize * 0.45f,
                weight = FontWeight.Bold
            )
        }
        // A tiny gold check pinned on earned medals.
        if (unlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(medalSize * 0.31f)
                    .clip(CircleShape)
                    .background(CurioColors.ButterYellow)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.Check,
                    contentDescription = null,
                    tint = Color(0xFF7A5A00),
                    size = medalSize * 0.21f,
                    weight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * v8.27 — a horizontal strip of PINNED badges: earned medals first (up to
 * [earnedLimit]), a "+N" tile when there are more (opens the full shelf
 * via [onViewAll]), then a few locked silhouettes for aspiration (spec
 * §4.1: "earned badges first, locked badges as silhouettes"). Renders
 * nothing but a hint when nothing is earned yet.
 */
@Composable
fun CurioBadgeStrip(
    earnedLimit: Int = 5,
    lockedPreview: Int = 2,
    medalSize: Dp = 44.dp,
    onViewAll: () -> Unit = {},
    modifier: Modifier = Modifier,
    emptyText: String = "Complete quests to pin your first badge"
) {
    val allStages = CurioQuests.allStages()
    val earned = allStages.filter { CurioQuests.isStageDone(it) }
    val locked = allStages.filterNot { CurioQuests.isStageDone(it) }
    if (earned.isEmpty()) {
        Text(
            emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(earned.take(earnedLimit), key = { it.id }) { stage ->
            CurioBadgeMedal(stage = stage, medalSize = medalSize)
        }
        if (earned.size > earnedLimit) {
            item {
                Surface(
                    onClick = onViewAll,
                    shape = CircleShape,
                    color = CurioColors.Sage.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, CurioColors.Sage.copy(alpha = 0.28f))
                ) {
                    Box(
                        modifier = Modifier.size(medalSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+${earned.size - earnedLimit}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = CurioColors.Sage
                        )
                    }
                }
            }
        }
        items(locked.take(lockedPreview), key = { it.id }) { stage ->
            CurioBadgeMedal(stage = stage, medalSize = medalSize)
        }
    }
}
