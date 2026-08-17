package com.curio.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioQuests
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * v8.28 — shared quest badge primitives (Quests page + Profile both pin
 * earned badges). The glyph/color mapping lives here so every medal draws
 * the same way everywhere.
 *
 * Every badge wears a TIER — Bronze, Silver, Gold, Platinum, or Secret —
 * so the shelf reads like a metal ladder instead of a flat color wall.
 * Platinum medals gleam with an animated shine sweep; Secret badges stay
 * hidden ("?") until earned, then shimmer iridescent.
 */

/**
 * Badge rarity ladder. [displayName] is shown under earned medals in the
 * shelf; tier rank drives the ordering (rarest first) in pinned strips.
 */
enum class BadgeTier(val displayName: String) {
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold"),
    PLATINUM("Platinum"),
    SECRET("Secret")
}

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

/**
 * v8.28 — the badge TIER ladder: early milestones are Bronze, the middle
 * Silver, the far end Gold, the summits Platinum, and a few ultra-rare
 * milestones are SECRET — hidden as "?" until earned, then revealed with
 * an iridescent shimmer.
 */
fun badgeTier(stage: CurioQuests.QuestStage): BadgeTier = when (stage.id) {
    // The Deck — spin milestones.
    "deck-1", "deck-3" -> BadgeTier.BRONZE
    "deck-5", "deck-10" -> BadgeTier.SILVER
    "deck-25" -> BadgeTier.GOLD
    "deck-50" -> BadgeTier.PLATINUM
    "deck-100" -> BadgeTier.SECRET
    // Discovery — explore milestones.
    "disc-1", "disc-3" -> BadgeTier.BRONZE
    "disc-5", "disc-10" -> BadgeTier.SILVER
    "disc-25", "disc-lane3" -> BadgeTier.GOLD
    "disc-lanes" -> BadgeTier.PLATINUM
    // Keepsakes — save milestones.
    "keep-1", "keep-3" -> BadgeTier.BRONZE
    "keep-5", "keep-10" -> BadgeTier.SILVER
    "keep-25", "keep-formats" -> BadgeTier.GOLD
    "keep-50" -> BadgeTier.PLATINUM
    "keep-100" -> BadgeTier.SECRET
    // The Tour — guided walkthrough (easy onboarding steps).
    "tour-settings", "tour-profile", "tour-pin", "tour-quote", "tour-daily" -> BadgeTier.BRONZE
    "tour-achievement" -> BadgeTier.SILVER
    // The Shelf — quote milestones.
    "quote-1" -> BadgeTier.BRONZE
    "quote-3" -> BadgeTier.SILVER
    "quote-5" -> BadgeTier.GOLD
    // Pin Board — pin milestones.
    "pin-1" -> BadgeTier.BRONZE
    "pin-3" -> BadgeTier.SILVER
    "pin-5" -> BadgeTier.GOLD
    // The Flame — streak milestones.
    "flame-1", "flame-3" -> BadgeTier.BRONZE
    "flame-7" -> BadgeTier.SILVER
    "flame-14" -> BadgeTier.GOLD
    "flame-30" -> BadgeTier.SECRET
    // Taste — like milestones.
    "like-1" -> BadgeTier.BRONZE
    "like-3" -> BadgeTier.SILVER
    "like-10" -> BadgeTier.GOLD
    // The Ladder — rank milestones.
    "rank-5" -> BadgeTier.BRONZE
    "rank-10" -> BadgeTier.SILVER
    "rank-20", "rank-30" -> BadgeTier.GOLD
    "rank-40" -> BadgeTier.PLATINUM
    "rank-50" -> BadgeTier.SECRET
    else -> BadgeTier.BRONZE
}

/** Rarity order — higher ranks sort first in pinned strips. */
fun badgeTierRank(tier: BadgeTier): Int = when (tier) {
    BadgeTier.BRONZE -> 0
    BadgeTier.SILVER -> 1
    BadgeTier.GOLD -> 2
    BadgeTier.PLATINUM -> 3
    BadgeTier.SECRET -> 4
}

/**
 * v42 — MERGED badge shelf. One medal per quest CHAIN (category): the
 * chain's HIGHEST-earned stage (its best rarity) stands in for the whole
 * chain, so earning Deck bronze then silver upgrades the single Deck medal
 * to silver instead of stacking two medals. Locked chains show a silhouette
 * of their highest-rarity stage for aspiration. Used by the Quests badge
 * dialog and the Profile strip so every shelf reads the same.
 */
data class MergedChainBadge(
    val chain: CurioQuests.QuestChain,
    /** The stage this chain's medal currently displays (best earned, else best rarity). */
    val displayStage: CurioQuests.QuestStage,
    val earned: Boolean,
    /** Every earned tier in this chain, rarest first (drives the "upgraded" chip). */
    val earnedTiers: List<BadgeTier>
)

/** Merge every chain into one display badge each. */
fun mergedChainBadges(): List<MergedChainBadge> = CurioQuests.Chains.map { chain ->
    val earned = chain.stages.filter { CurioQuests.isStageDone(it) }
        .sortedByDescending { badgeTierRank(badgeTier(it)) }
    val best = earned.firstOrNull()
    // A locked chain previews its highest-rarity stage (aspiration); a
    // fully-earned chain stands on its best medal.
    val display = best ?: chain.stages.maxByOrNull { badgeTierRank(badgeTier(it)) }!!
    MergedChainBadge(
        chain = chain,
        displayStage = display,
        earned = best != null,
        earnedTiers = earned.map { badgeTier(it) }.distinct()
    )
}.sortedByDescending { m -> badgeTierRank(badgeTier(m.displayStage)) }

/**
 * v42 — the shared badge DETAIL dialog: a medal, its tier chip, name,
 * description, and (for locked badges) live progress. Opened by tapping a
 * badge on the Profile and in the Quests badge shelf.
 */
@Composable
fun CurioBadgeDetailDialog(
    stage: CurioQuests.QuestStage,
    onDismiss: () -> Unit
) {
    val unlocked = CurioQuests.isStageDone(stage)
    val tier = badgeTier(stage)
    val accent = tierAccent(tier)
    val secretLocked = !unlocked && tier == BadgeTier.SECRET
    val progress = CurioQuests.stageProgress(stage)
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (secretLocked) "Secret badge" else "${tier.displayName} badge",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = accent
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CurioBadgeMedal(stage = stage, medalSize = 84.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (secretLocked) "???" else stage.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    tier.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = accent
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (secretLocked) "A rare badge hides here. Keep exploring to reveal it."
                    else stage.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                if (unlocked) {
                    Text(
                        "Earned · +${stage.xpReward} XP",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = curioSageInk()
                    )
                } else if (!secretLocked) {
                    Text(
                        "$progress / ${stage.target} · +${stage.xpReward} XP",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (progress.toFloat() / stage.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.16f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

/** Secret badges hide their identity (and never show as locked silhouettes). */
fun isSecretBadge(stage: CurioQuests.QuestStage): Boolean =
    badgeTier(stage) == BadgeTier.SECRET

/**
 * The metallic gradient for a tier — lit from the top-left, deep in the
 * bottom-right, so the medal reads as polished metal rather than flat fill.
 */
fun tierMetals(tier: BadgeTier): List<Color> = when (tier) {
    BadgeTier.BRONZE -> listOf(Color(0xFFE6AF83), Color(0xFFC47A3F), Color(0xFF7C431C))
    BadgeTier.SILVER -> listOf(Color(0xFFF2F5F9), Color(0xFFBDC4CD), Color(0xFF6E7884))
    BadgeTier.GOLD -> listOf(Color(0xFFFFEBA6), Color(0xFFE9BE4B), Color(0xFF9A6B0A))
    BadgeTier.PLATINUM -> listOf(Color(0xFFF7FBFF), Color(0xFFD3E0EC), Color(0xFF5E7A99))
    BadgeTier.SECRET -> listOf(Color(0xFFF3DCFF), Color(0xFF9B6BD6), Color(0xFF472A75))
}

/** The bright signature color of a tier — chips, progress, halos. */
fun tierAccent(tier: BadgeTier): Color = when (tier) {
    BadgeTier.BRONZE -> Color(0xFFC07A3E)
    BadgeTier.SILVER -> Color(0xFF8A93A0)
    BadgeTier.GOLD -> Color(0xFFD9A421)
    BadgeTier.PLATINUM -> Color(0xFF8FB4D9)
    BadgeTier.SECRET -> Color(0xFFA479E0)
}

/**
 * One badge MEDAL — a round metal coin with a double ring. Earned badges
 * show the badge IN FULL: a polished metallic gradient, a specular sheen,
 * a tier gem knotted on top and an earned marker on the bottom-right.
 * Platinum medals sweep a diagonal shine; Secret badges shimmer an
 * iridescent band and — while locked — stay anonymous behind a "?".
 * Locked badges are silhouette medals, so the shelf reads as a set of
 * badges rather than a list of chores.
 */
@Composable
fun CurioBadgeMedal(
    stage: CurioQuests.QuestStage,
    medalSize: Dp = 58.dp,
    modifier: Modifier = Modifier
) {
    val unlocked = CurioQuests.isStageDone(stage)
    val tier = badgeTier(stage)
    val accent = tierAccent(tier)
    val metals = tierMetals(tier)
    val density = LocalDensity.current
    val sizePx = with(density) { medalSize.toPx() }
    // Platinum and Secret medals carry an animated shine sweep. The infinite
    // transition is only created for them (conditional composable call is
    // fine — the branch is stable per tier/unlocked state).
    val fancy = unlocked && (tier == BadgeTier.PLATINUM || tier == BadgeTier.SECRET)
    val sweep = if (fancy) {
        val transition = rememberInfiniteTransition(label = "badgeShine")
        transition.animateFloat(
            initialValue = -0.75f,
            targetValue = 1.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (tier == BadgeTier.SECRET) 2600 else 2000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        ).value
    } else 0f
    val questionSize = with(density) { (medalSize * 0.42f).toSp() }

    Box(
        // v27r — the coin is defined by its ring BORDERS again, not shadows:
        // the elevation pass's outer shadow clipped against the shelf edges
        // ("weirdly getting cut") and the medals lost their crisp edge.
        modifier = modifier.size(medalSize),
        contentAlignment = Alignment.Center
    ) {
        // A soft halo behind the rarest metals (platinum + secret).
        if (fancy) {
            Box(
                modifier = Modifier
                    .size(medalSize * 1.14f)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (tier == BadgeTier.SECRET) 0.30f else 0.22f))
            )
        }
        if (unlocked) {
            // Metallic base — lit top-left, deep bottom-right.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(metals))
            )
            // Specular highlight on the upper-left.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(medalSize * 0.62f)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                                center = Offset.Zero
                            )
                        )
                )
            }
            // Animated shine sweep — platinum gleams, secret shimmers iridescent.
            if (fancy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (tier == BadgeTier.SECRET) {
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFFFFD700).copy(alpha = 0.45f),
                                        Color(0xFFB388FF).copy(alpha = 0.45f),
                                        Color(0xFFFF8A80).copy(alpha = 0.40f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                },
                                start = Offset(sizePx * sweep, 0f),
                                end = Offset(sizePx * sweep + sizePx * 0.62f, sizePx)
                            )
                        )
                )
            }
        } else {
            // Locked silhouette — Secret badges wear a darker, violet-tinted one.
            // v27r — the coin edge is the ring border again (v27n's opaque
            // fill stays; the shadow is gone so nothing smears or clips).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        if (tier == BadgeTier.SECRET) {
                            lerp(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                0.22f
                            )
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), CircleShape)
            )
        }
        // The inner glyph plate — the crisp ring edge that defines the coin
        // (v27r — restored; the elevation pass swapped this border for
        // shadows and the medals lost their definition).
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
            when {
                // Locked Secret badges hide their identity behind a mystery mark.
                !unlocked && tier == BadgeTier.SECRET -> Text(
                    "?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = questionSize
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
                // v163 — the badge glyphs render at NORMAL weight: Material
                // Symbols at Bold (wght 700) draw very heavy, and in the
                // medal's tight inner ring they crowded the plate and read
                // squished/clipped. Normal keeps the clean outlined stroke
                // with visible breathing room around the ring.
                unlocked -> CurioIcon(
                    name = badgeGlyph(stage),
                    contentDescription = null,
                    tint = Color.White,
                    size = medalSize * 0.45f
                )
                else -> CurioIcon(
                    name = CurioIcons.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    size = medalSize * 0.45f
                )
            }
        }
        // The ribbon gem — the tier's metal color, knotted on top of earned medals.
        if (unlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (medalSize * -0.05f))
                    .size(medalSize * 0.26f)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color.White, accent)))
                    // v27r — restored white rim so the gem reads as a knot.
                    .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
        }
        // Earned marker — a gold check, or a sparkle on secret badges.
        if (unlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(medalSize * 0.31f)
                    .clip(CircleShape)
                    .background(if (tier == BadgeTier.SECRET) accent else CurioColors.ButterYellow)
                    // v27r — restored crisp white rim.
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = if (tier == BadgeTier.SECRET) CurioIcons.AutoAwesome else CurioIcons.Check,
                    contentDescription = null,
                    tint = if (tier == BadgeTier.SECRET) Color.White else Color(0xFF7A5A00),
                    // v163 — normal weight (the tiny check at Bold read blobby).
                    size = medalSize * 0.21f
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
 *
 * v8.28 — earned medals are ordered rarest-first (Secret, Platinum, Gold,
 * Silver, Bronze) so the strip shows off the best of the shelf, and locked
 * Secret badges never appear as silhouettes — they stay a mystery.
 */
@Composable
fun CurioBadgeStrip(
    earnedLimit: Int = 5,
    lockedPreview: Int = 2,
    medalSize: Dp = 44.dp,
    onViewAll: () -> Unit = {},
    onBadgeClick: ((CurioQuests.QuestStage) -> Unit)? = null,
    modifier: Modifier = Modifier,
    emptyText: String = "Complete quests to pin your first badge"
) {
    // v42 — the strip now shows the MERGED shelf (one medal per chain):
    // earned chains first by best-earned rarity, then a few locked chain
    // silhouettes for aspiration. Tapping a medal opens its detail dialog.
    val merged = mergedChainBadges()
    val earned = merged.filter { it.earned }
    val locked = merged.filterNot { it.earned }
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
        // Earned chain medals, best-earned rarity first.
        items(
            earned.take(earnedLimit),
            key = { it.chain.id }
        ) { m ->
            Box(
                modifier = Modifier.then(
                    if (onBadgeClick != null) Modifier.clickable { onBadgeClick(m.displayStage) } else Modifier
                )
            ) {
                CurioBadgeMedal(stage = m.displayStage, medalSize = medalSize)
            }
        }
        if (earned.size > earnedLimit) {
            item {
                Surface(
                    onClick = onViewAll,
                    shape = CircleShape,
                    // v27r — the +N tile wears its sage ring again (v27n's
                    // opaque fill stays); the shadow is gone so it never clips.
                    color = lerp(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        curioSageInk(),
                        0.13f
                    ),
                    border = BorderStroke(1.dp, curioSageInk().copy(alpha = 0.28f))
                ) {
                    Box(
                        modifier = Modifier.size(medalSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+${earned.size - earnedLimit}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = curioSageInk()
                        )
                    }
                }
            }
        }
        // A couple of locked chain silhouettes for aspiration — never Secrets.
        items(
            locked.filterNot { isSecretBadge(it.displayStage) }.take(lockedPreview),
            key = { it.chain.id }
        ) { m ->
            Box(
                modifier = Modifier.then(
                    if (onBadgeClick != null) Modifier.clickable { onBadgeClick(m.displayStage) } else Modifier
                )
            ) {
                CurioBadgeMedal(stage = m.displayStage, medalSize = medalSize)
            }
        }
    }
}
