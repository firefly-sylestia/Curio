package com.curio.app.features.quests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioPassport
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioQuests.DailyQuest
import com.curio.app.data.CurioQuests.QuestChain
import com.curio.app.data.CurioQuests.QuestStage
import com.curio.app.data.CurioQuests.WeeklyQuest
import com.curio.app.data.PromoMode
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToQuestRoute
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.components.ConfettiBurst
import com.curio.app.ui.components.BadgeTier
import com.curio.app.ui.components.CurioBadgeMedal
import com.curio.app.ui.components.CurioBadgeStrip
import com.curio.app.ui.components.badgeTier
import com.curio.app.ui.components.tierAccent
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.pet.CurioPetHeroCard
import com.curio.app.ui.pet.CurioPetSprite
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import kotlinx.coroutines.delay
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.readableAccentInk
import com.curio.app.ui.theme.themedAccent

/**
 * Quests & levels — Curio's gamification home (v8.0).
 *
 * Its own page, styled like the settings family: a compact torn rose hero
 * header on a watermark backdrop, then the rank card and the quest CHAINS.
 * Reads live reactive state from [CurioQuests], so badges pop the moment
 * they unlock and the current stage updates in place.
 *
 * Layout:
 *  1. Hero — the shared settings torn-banner header.
 *  2. Rank card — the XP bar, current rank, and "X of 50" ladder note.
 *  3. The current quest — the active stage across all chains, with a
 *     jump-to-it button when the stage has a screen.
 *  4. Quest chains — every chain (Tour, Deck, Discovery, Keepsakes, Shelf,
 *     Pin Board, Flame, Taste, Ladder) with its stages; the next stage is
 *     the hero, later stages preview as locked.
 *  5. Today's quests — five dailies a day (three core + two bonus), with
 *     mini progress bars and claimable rewards.
 *  6. Badge shelf — every chain stage as a badge, in a two-column grid.
 */
@Composable
fun QuestsScreen(navController: NavController) {
    // v7.107 — promo/demo-content mode shows the promotional sample XP (top
    // rank, Curio Sovereign) while ON; only the level card is demoed here.
    val promoOn = AppPreferences.promoModeState
    val xp = if (promoOn) PromoMode.DEMO_XP else CurioQuests.xpState
    val level = CurioQuests.levelForXp(xp)
    val (progress, nextThreshold) = CurioQuests.xpProgress(xp)
    val current = CurioQuests.currentQuest()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    // v8.5 — pet celebration: a daily claim bumps this key and the pet hero
    // hops once (spec §9). A soft confirmation haptic marks the reward
    // moment (spec §9.3 — light touch, respects device settings).
    var celebrate by remember { mutableStateOf(0) }
    // v8.6 — level-up celebration (spec §9.1): a claim that crosses a level
    // shows a brief non-blocking banner (tap to dismiss, auto-dismisses).
    var levelUpBanner by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(levelUpBanner) {
        if (levelUpBanner != null) {
            delay(2500)
            levelUpBanner = null
        }
    }
    // v8.27 — live badge-unlock toast: a chain badge earned while this page
    // is open (e.g. right after a daily claim) pops a medal toast and hops
    // the pet (spec §9.1 — the reward moment).
    var seenAwardedIds by remember { mutableStateOf(CurioQuests.awardedStagesState) }
    var newBadgeStage by remember { mutableStateOf<QuestStage?>(null) }
    LaunchedEffect(CurioQuests.awardedStagesState) {
        val newIds = CurioQuests.awardedStagesState - seenAwardedIds
        if (newIds.isNotEmpty()) {
            seenAwardedIds = CurioQuests.awardedStagesState
            val stage = CurioQuests.allStages().firstOrNull { it.id in newIds }
            if (stage != null) {
                newBadgeStage = stage
                celebrate++
            }
        }
    }
    LaunchedEffect(newBadgeStage) {
        if (newBadgeStage != null) {
            delay(2600)
            newBadgeStage = null
        }
    }
    // The pet's one-shot bubble for this visit (spec §10.7 — one per screen
    // visit). Fetched in a LaunchedEffect so the bubble's prefs write never
    // happens during composition.
    var petBubble by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (AppPreferences.petEnabledState) {
            petBubble = CurioPet.bubbleFor(context, "quests", CurioQuests.categoriesState)
        }
    }
    // The pet's accent — the least-engaged lane's tint, or the page coral
    // when every lane is already explored. (themedAccent is @Composable, so
    // only the prefs read is remembered.)
    val onQuestNavigate: (String) -> Unit = { route ->
        navController.navigateToQuestRoute(route)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs behind the content
        // (the settings/profile language). Quests are category-neutral, so
        // the wildcard sparkle leads the collage.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD)
            )
        }
        // The hero is drawn LAST (on top of the scroll content): the quest
        // cards scroll UP and disappear behind the ragged tear instead of
        // clipping at a straight line — the same overlay construction as
        // every settings screen.
        ScreenEntrance {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight + 10.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // v8.5 — Pet hero: the level card is replaced by the pet
                // companion (level + XP ring + growth line + speech bubble)
                // when the pet is enabled; the classic level card returns
                // when the toggle is off.
                item {
                    if (AppPreferences.petEnabledState) {
                        CurioPetHeroCard(
                            bubbleText = petBubble,
                            onGo = onQuestNavigate,
                            celebrateKey = celebrate
                        )
                    } else {
                        LevelCard(
                            level = level,
                            xp = xp,
                            nextThreshold = nextThreshold,
                            progress = progress,
                            isMaxLevel = level >= CurioQuests.maxLevel
                        )
                    }
                }
                // v8.5 — Daily quests are FIRST under the hero: the page
                // answers "what can I do today" before anything else
                // (spec §3 + §4.1). Completing one fires the pet's
                // celebration hop.
                item {
                    // v8.18 — Today's quests is a CURIOUS landmark: the pet
                    // sometimes tiptoes over and reads what's on for today.
                    PetLandmark(
                        id = "daily",
                        kind = PetLandmarks.Kind.CURIOUS,
                        screen = "quests"
                    ) { m ->
                        DailyCard(
                            quests = CurioQuests.dailyQuestsFor(CurioQuests.todayEpochDay(), context),
                            // v8.3 — complete dailies are CLAIMED here (a tap
                            // grants the XP) and in-progress ones can Go straight
                            // to where the action happens. v8.6 — a claim that
                            // crosses a level raises the level-up banner.
                            onClaim = { questId ->
                                val levelBefore = CurioQuests.levelForXp(CurioQuests.xpState)
                                CurioQuests.claimDaily(context, questId)
                                val levelAfter = CurioQuests.levelForXp(CurioQuests.xpState)
                                if (levelAfter > levelBefore) levelUpBanner = levelAfter
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                celebrate++
                            },
                            onGo = onQuestNavigate,
                            modifier = m
                        )
                    }
                }
                // v8.42 — This week's quests: three rotating week-long goals
                // under the daily stack (always-on, a new mix every Monday).
                item {
                    WeeklyCard(
                        quests = CurioQuests.weeklyQuestsFor(CurioQuests.currentWeekKey()),
                        onClaim = { questId ->
                            val levelBefore = CurioQuests.levelForXp(CurioQuests.xpState)
                            CurioQuests.claimWeekly(context, questId)
                            val levelAfter = CurioQuests.levelForXp(CurioQuests.xpState)
                            if (levelAfter > levelBefore) levelUpBanner = levelAfter
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            celebrate++
                        }
                    )
                }
                if (current != null) {
                    item {
                        // v8.18 — the active quest card is a FUN landmark:
                        // the pet sometimes dashes over and boops it.
                        PetLandmark(
                            id = "quest",
                            kind = PetLandmarks.Kind.FUN,
                            screen = "quests"
                        ) { m ->
                            CurrentQuestCard(
                                stage = current,
                                onNavigate = onQuestNavigate,
                                modifier = m
                            )
                        }
                    }
                }
                // v8.5 — Category passport: every lane's stamp, tappable to
                // spin that lane (spec §6).
                item {
                    PassportCard(
                        onSpin = { slug ->
                            navController.navigate(CurioRoutes.spinWithCategory(slug)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                // v8.7 — ONE compact "Quest paths" card replaces the wall of
                // per-chain cards and the always-visible badge grid: every
                // unfinished path is a tappable row (tap to see its stages),
                // and the badge shelf lives behind a single tappable row →
                // dialog. Nothing on the page is a dead display board.
                item {
                    PathsCard(onNavigate = onQuestNavigate)
                }
            }
        }
        // Drawn on top of the scroll content — cards slide under the ragged
        // tear as they scroll up.
        SettingsHeroHeader(
            title = "Quests & levels",
            subtitle = "Grow your curiosity, one chain at a time",
            onBack = { navController.popBackStack() }
        )
        // v8.6 — non-blocking level-up celebration (spec §9.1): tap to
        // dismiss; also auto-dismisses after ~2.5s. The pet hops with the
        // claim and wears its proud mood.
        levelUpBanner?.let { newLevel ->
            Surface(
                onClick = { levelUpBanner = null },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CurioPetSprite(
                        stage = CurioPet.currentStage(),
                        mood = CurioPet.Mood.PROUD,
                        spriteSize = 52.dp,
                        celebrateKey = celebrate
                    )
                    Column {
                        Text(
                            "Level $newLevel reached!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            "Curie grew a little. Keep going!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        // v8.27 — reward moments: confetti rains on a level-up, and a medal
        // toast pops when a badge unlocks live on this page.
        if (levelUpBanner != null) {
            ConfettiBurst(
                colors = listOf(
                    CurioColors.CoralBlush, CurioColors.ButterYellow,
                    CurioColors.Sage, CurioColors.SkyMint
                ),
                // Level-up can't smart-cast the delegated property, so feed
                // the confetti a plain value (the block only runs non-null).
                trigger = levelUpBanner ?: 0,
                particleCount = CurioMotion.ConfettiParticleCountLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(),
                onComplete = {}
            )
        }
        newBadgeStage?.let { stage ->
            Surface(
                onClick = { newBadgeStage = null },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CurioBadgeMedal(stage = stage, medalSize = 42.dp)
                    Column {
                        Text(
                            "Badge unlocked!",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = CurioColors.Sage
                        )
                        Text(
                            "${badgeTier(stage).displayName} · ${stage.title}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = tierAccent(badgeTier(stage)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

}

/** The rank card — big level badge, title, and the XP progress bar. */
@Composable
private fun LevelCard(level: Int, xp: Int, nextThreshold: Int, progress: Float, isMaxLevel: Boolean) {
    CurioSettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(CurioGradients.WildcardGradientStops.take(3))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$level",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Level $level · ${CurioQuests.levelTitle(level)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isMaxLevel) "Curio Sovereign, the whole shelf is yours."
                    else "Rank $level of ${CurioQuests.maxLevel} · ${CurioQuests.maxLevel - level} to go",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CurioIcon(
                name = CurioIcons.WorkspacePremium,
                contentDescription = null,
                // Gold trophy — an earned rank reads better in warm gold
                // than the coral used everywhere else (v7.103).
                tint = CurioColors.ButterYellow,
                size = 30.dp
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = CurioColors.CoralBlush,
            trackColor = CurioColors.CoralBlush.copy(alpha = 0.14f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isMaxLevel) "Curio Sovereign, the whole shelf is yours."
            else "$xp / $nextThreshold XP · ${(nextThreshold - xp).coerceAtLeast(0)} XP to Level ${level + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The single active quest across all chains — the hero of the page. */
@Composable
private fun CurrentQuestCard(
    stage: QuestStage,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val roseHero = if (isCurioDarkTheme()) {
        CurioColors.HomeRosewoodDark
    } else {
        CurioColors.HomeRosewood
    }
    CurioSettingsCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(CurioColors.CoralBlush),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.TaskAlt,
                    contentDescription = null,
                    tint = Color.White,
                    size = 14.dp
                )
            }
            Text(
                // v8.5 — this is the single recommended next step (spec §3:
                // "One primary next step"), so the label says what it is.
                "RECOMMENDED NEXT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = roseHero
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stage.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            stage.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Hint: ${stage.hint}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        val done = CurioQuests.stageProgress(stage)
        val chain = CurioQuests.Chains.firstOrNull { it.stages.any { s -> s.id == stage.id } }
        Surface(
            onClick = { stage.navRoute?.let(onNavigate) },
            shape = RoundedCornerShape(50),
            color = CurioColors.CoralBlush,
            enabled = stage.navRoute != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (stage.navRoute != null) "Start · +${stage.xpReward} XP"
                else "In progress · ${done.coerceAtMost(stage.target)}/${stage.target}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(vertical = 9.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (chain != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "From the ${chain.title} chain, ${CurioQuests.chainProgress(chain)} of ${chain.stages.size} stages done",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * v8.7 — "Quest paths": ONE compact card replaces the wall of per-chain
 * cards and the always-visible badge grid. Every unfinished path is a
 * tappable row — tap to expand its stage trail (the next actionable stage
 * carries a Go chip). The badge shelf lives behind a single tappable row
 * that opens a dialog, so no part of the page is a dead display board.
 */
@Composable
private fun PathsCard(
    onNavigate: (String) -> Unit = {}
) {
    val activeChains = CurioQuests.Chains.filter { chain ->
        CurioQuests.chainProgress(chain) < chain.stages.size
    }
    val allStages = CurioQuests.allStages()
    val unlockedCount = allStages.count { CurioQuests.isStageDone(it) }
    var showBadges by rememberSaveable { mutableStateOf(false) }
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.Flag,
            "Quest paths",
            "${activeChains.size} open · $unlockedCount badges earned"
        )
        Spacer(Modifier.height(2.dp))
        if (activeChains.isEmpty()) {
            Text(
                "Every path complete. The whole shelf is yours!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        activeChains.forEach { chain ->
            PathRow(chain = chain, onNavigate = onNavigate)
        }
        // v8.27 — PINNED badges: the earned medals (and a couple of locked
        // silhouettes for aspiration) live ON the page; the row below still
        // opens the full two-column shelf.
        CurioBadgeStrip(
            earnedLimit = 5,
            lockedPreview = 2,
            onViewAll = { showBadges = true }
        )
        // Badge shelf — one tappable row that opens the grid in a dialog
        // (v8.7 — no permanent two-column board dominating the page).
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBadges = true }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(CurioColors.CoralBlush.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.Star,
                    contentDescription = null,
                    tint = CurioColors.CoralBlush,
                    size = 18.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Badge shelf",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "$unlockedCount of ${allStages.size} earned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "View",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = CurioColors.CoralBlush
            )
            CurioForwardArrow(
                "View badge shelf",
                tint = CurioColors.CoralBlush,
                size = 14.dp
            )
        }
    }
    if (showBadges) {
        AlertDialog(
            onDismissRequest = { showBadges = false },
            title = { Text("Badges · $unlockedCount of ${allStages.size} earned") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    allStages.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { stage ->
                                BadgeTile(
                                    stage = stage,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBadges = false }) { Text("Close") }
            }
        )
    }
}

/** One quest path row — tap to expand its stage trail. */
@Composable
private fun PathRow(
    chain: QuestChain,
    onNavigate: (String) -> Unit
) {
    val chainDone = CurioQuests.chainProgress(chain)
    // v8.7 — rows are compact by default; tapping expands the stage trail
    // (the next actionable stage carries a Go/Start chip).
    var expanded by rememberSaveable(chain.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(CurioGradients.cardGradient(CurioColors.CoralBlush))),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(
                name = chain.glyph,
                contentDescription = null,
                tint = Color.White,
                size = 18.dp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                chain.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                chain.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "$chainDone/${chain.stages.size}",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = if (chainDone == chain.stages.size) CurioColors.Sage else CurioColors.CoralBlush
        )
        CurioIcon(
            name = if (expanded) CurioIcons.KeyboardArrowUp else CurioIcons.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse ${chain.title}" else "Expand ${chain.title}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 18.dp
        )
    }
    // v8.3 — the chain's NEXT actionable stage carries a Go/Start chip too.
    AnimatedVisibility(visible = expanded) {
        Column {
            val nextIndex = chain.stages.indexOfFirst { !CurioQuests.isStageDone(it) }
            chain.stages.forEachIndexed { index, stage ->
                val done = CurioQuests.isStageDone(stage)
                val isCurrent = !done && stage.id == CurioQuests.currentQuest()?.id
                ChainStageRow(
                    index = index,
                    stage = stage,
                    done = done,
                    isCurrent = isCurrent,
                    isNext = index == nextIndex,
                    onNavigate = { stage.navRoute?.let(onNavigate) }
                )
            }
        }
    }
}

/** One stage row in a chain — number circle, title, and done/current state. */
@Composable
private fun ChainStageRow(
    index: Int,
    stage: QuestStage,
    done: Boolean,
    isCurrent: Boolean,
    isNext: Boolean,
    onNavigate: () -> Unit
) {
    val accent = when {
        done -> CurioColors.Sage
        isCurrent -> CurioColors.CoralBlush
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (done || isCurrent) 1f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                CurioIcon(CurioIcons.Check, null, tint = Color.White, size = 16.dp)
            } else {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stage.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrent || done) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (done) "Done · +${stage.xpReward} XP"
                else "+${stage.xpReward} XP",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) CurioColors.CoralBlush else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // v8.3 — the chain's next actionable quest gets a Go/Start chip that
        // jumps the user to the quest's screen. The globally current quest is
        // the solid coral "Go"; the other chains' next quests are muted
        // "Start".
        if (!done && stage.navRoute != null && isNext) {
            Surface(
                onClick = onNavigate,
                shape = RoundedCornerShape(50),
                color = if (isCurrent) CurioColors.CoralBlush
                else CurioColors.CoralBlush.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (isCurrent) "Go" else "Start",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCurrent) Color.White else CurioColors.CoralBlush
                    )
                    CurioForwardArrow(
                        if (isCurrent) "Go to quest" else "Start this quest",
                        tint = if (isCurrent) Color.White else CurioColors.CoralBlush,
                        size = 14.dp
                    )
                }
            }
        }
    }
}

/**
 * Where a daily quest's action happens — null when the quest has no single
 * destination screen (saving/bookmarking need an open topic or entry).
 */
private fun dailyGoRoute(kind: CurioQuests.DailyKind): String? = when (kind) {
    CurioQuests.DailyKind.SPIN -> CurioRoutes.SPIN
    // Exploring starts on a topic's reveal page — spinning is the fastest
    // way to land on one.
    CurioQuests.DailyKind.EXPLORE -> CurioRoutes.SPIN
    CurioQuests.DailyKind.PROFILE -> CurioRoutes.PROFILE
    else -> null
}

/**
 * Today's quests — three CORE quests with mini progress bars (v8.3 —
 * claimable rewards), then TWO BONUS quests that unlock once the core trio
 * is claimed (v8.27). Before they unlock, the bonus pair peeks through as
 * locked "??" silhouettes so players can see more is coming (v8.28).
 * Completed quests animate away, the bonus group pops in with a gold
 * sparkle, and claiming pops a "+N XP" chip and hops the pet.
 */
@Composable
private fun DailyCard(
    quests: List<DailyQuest>,
    onClaim: (String) -> Unit,
    onGo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val awarded = CurioQuests.dailyAwardedState
    val core = quests.filterNot { it.bonus }
    val bonus = quests.filter { it.bonus }
    val coreDone = core.all { it.id in awarded }
    // v8.27 — while the core trio is open, claimed core quests HIDE (they
    // animate out); once ALL THREE are claimed the bonus quests take over.
    val doneCount = quests.count { it.id in awarded }
    // v8.27 — claim XP pop: a tiny "+N XP" chip pops near the header every
    // time a quest's reward is claimed.
    var xpPop by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(xpPop?.second) {
        if (xpPop != null) {
            delay(1000)
            xpPop = null
        }
    }
    CurioSettingsCard(modifier = modifier) {
        CurioCardHeader(
            CurioIcons.EmojiEvents,
            "Today's quests",
            "$doneCount of ${quests.size} done · Resets at 4 AM"
        )
        Spacer(Modifier.height(2.dp))
        // v8.27 — bonus unlock line: pops in gold once the core trio is done.
        AnimatedVisibility(
            visible = coreDone && bonus.isNotEmpty(),
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.85f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.AutoAwesome,
                    contentDescription = null,
                    tint = bonusGold(),
                    size = 16.dp
                )
                Text(
                    "Bonus quests unlocked!",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = bonusGold()
                )
            }
        }
        // Both groups stay composed and CROSS-FADE when the trio completes:
        // the core list (+ locked bonus silhouettes) fades out while the real
        // bonus pair pops in (fade + grow).
        AnimatedVisibility(
            visible = !coreDone,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180))
        ) {
            Column {
                core.forEach { quest ->
                    DailyQuestRow(
                        quest = quest,
                        done = quest.id in awarded,
                        onClaim = { id ->
                            onClaim(id)
                            xpPop = quest.xpReward to ((xpPop?.second ?: 0) + 1)
                        },
                        onGo = onGo
                    )
                }
                // v8.28 — the two bonus quests peek through as locked "??"
                // silhouettes (dimmed, mystery reward) so there's a visible
                // reason to finish the trio; they swap for the gold rows on
                // unlock. (coreRemaining >= 1 here: the group only shows
                // while the trio is still open.)
                val coreRemaining = core.count { it.id !in awarded }
                bonus.forEach {
                    BonusLockedRow(coreRemaining = coreRemaining)
                }
            }
        }
        AnimatedVisibility(
            visible = coreDone,
            enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.92f)
        ) {
            Column {
                bonus.forEach { quest ->
                    DailyQuestRow(
                        quest = quest,
                        done = quest.id in awarded,
                        onClaim = { id ->
                            onClaim(id)
                            xpPop = quest.xpReward to ((xpPop?.second ?: 0) + 1)
                        },
                        onGo = onGo
                    )
                }
            }
        }
        if (doneCount == quests.size) {
            Text(
                "All done today! Fresh quests land at 4 AM.",
                style = MaterialTheme.typography.bodySmall,
                color = CurioColors.Sage,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        // The floating "+N XP" claim chip.
        AnimatedVisibility(
            visible = xpPop != null,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.6f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.7f, animationSpec = tween(200))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CurioColors.Sage.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, CurioColors.Sage.copy(alpha = 0.30f))
                ) {
                    Text(
                        "+${xpPop?.first ?: 0} XP",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CurioColors.Sage,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * v8.42 — this week's quests: three week-long goals under the daily stack,
 * resetting every Monday 4 AM (always-on, user-confirmed). Same row rhythm
 * as the dailies; claimed goals animate away and the header counts them.
 */
@Composable
private fun WeeklyCard(
    quests: List<WeeklyQuest>,
    onClaim: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val awarded = CurioQuests.weeklyAwardedState
    val doneCount = quests.count { it.id in awarded }
    CurioSettingsCard(modifier = modifier) {
        CurioCardHeader(
            CurioIcons.CalendarToday,
            "This week's quests",
            "$doneCount of ${quests.size} done · New goals every Monday"
        )
        Spacer(Modifier.height(2.dp))
        Column {
            quests.forEach { quest ->
                WeeklyQuestRow(
                    quest = quest,
                    done = quest.id in awarded,
                    onClaim = { onClaim(quest.id) }
                )
            }
        }
        if (doneCount == quests.size) {
            Text(
                "All done this week! Fresh goals land Monday.",
                style = MaterialTheme.typography.bodySmall,
                color = CurioColors.Teal,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** One weekly quest row — icon, title, description, progress bar, claim. */
@Composable
private fun WeeklyQuestRow(
    quest: WeeklyQuest,
    done: Boolean,
    onClaim: () -> Unit
) {
    val progress = CurioQuests.weeklyProgress(quest)
    val fraction by animateFloatAsState(
        targetValue = (progress.toFloat() / quest.target.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(450),
        label = "weeklyProgress"
    )
    val claimable = !done && progress >= quest.target
    val accent = CurioColors.Teal
    // Claimed goals animate OUT (scale + fade) instead of vanishing.
    AnimatedVisibility(
        visible = !done,
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (done) CurioColors.Sage.copy(alpha = 0.18f)
                        else accent.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = if (done) CurioIcons.Check else CurioIcons.CalendarToday,
                    contentDescription = null,
                    tint = if (done) CurioColors.Sage else accent,
                    size = 18.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (done) FontWeight.ExtraBold else FontWeight.SemiBold
                    ),
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    quest.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (done) CurioColors.Sage else accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            if (claimable) {
                Surface(
                    onClick = onClaim,
                    shape = RoundedCornerShape(50),
                    color = accent
                ) {
                    Text(
                        "Claim +${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            } else {
                Text(
                    if (done) "Done" else "+${quest.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (done) CurioColors.Sage else accent
                )
            }
        }
    }
}

/**
 * v8.28 — a locked BONUS silhouette: the same row rhythm as a real bonus
 * quest, but dimmed with a mystery "?? XP" reward and a hint counting how
 * many core quests are left. Swaps for the real gold row on unlock.
 */
@Composable
private fun BonusLockedRow(coreRemaining: Int) {
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val dimSoft = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(
                name = CurioIcons.AutoAwesome,
                contentDescription = null,
                tint = dimSoft,
                size = 18.dp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Bonus quest",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (coreRemaining == 1) "Complete the last core quest to unlock"
                else "Complete $coreRemaining more core quests to unlock",
                style = MaterialTheme.typography.bodySmall,
                color = dimSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "?? XP",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = dimSoft
        )
    }
}

/**
 * v8.33 — the bonus quest gold, theme-aware: dark gold ink on light cream
 * surfaces (ButterYellow is a pale pastel that vanishes on the light
 * background), bright butter in dark mode where it pops. @Composable so it
 * can read the live theme state.
 */
@Composable
private fun bonusGold(): Color =
    if (isCurioDarkTheme()) CurioColors.ButterYellow else CurioColors.GoldInk

/** One daily quest row — title, animated progress, and Claim / Go chip. */
@Composable
private fun DailyQuestRow(
    quest: DailyQuest,
    done: Boolean,
    onClaim: (String) -> Unit,
    onGo: (String) -> Unit
) {
    val context = LocalContext.current
    val progress = CurioQuests.dailyProgressState[quest.kind.name] ?: 0
    val fraction by animateFloatAsState(
        targetValue = (progress.toFloat() / quest.target.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(450),
        label = "dailyProgress"
    )
    // v8.6 — the discovery daily names the lane the passport wants the user
    // to try, and its Go chip routes straight into that lane's Spin deck
    // (spec §6.2/§6.3).
    val discoveryLane = if (quest.kind == CurioQuests.DailyKind.DISCOVERY) {
        CurioPassport.leastEngaged(context)
    } else null
    val questTitle = when {
        discoveryLane != null -> "New lane, try ${discoveryLane.displayName}"
        quest.kind == CurioQuests.DailyKind.DISCOVERY -> "Try a new lane"
        else -> quest.title
    }
    val goRoute = when {
        discoveryLane != null -> CurioRoutes.spinWithCategory(discoveryLane.id.routeSlug)
        quest.kind == CurioQuests.DailyKind.DISCOVERY -> null
        else -> dailyGoRoute(quest.kind)
    }
    val claimable = !done && progress >= quest.target
    // v8.27 — bonus quests wear gold + a sparkle glyph; the Claim pill
    // softly pulses ONLY while it's ready to claim (spec §5.3: "card glows
    // softly"), so an idle page never drives a permanent animation loop.
    // v8.33 — bonus gold reads on LIGHT cream surfaces: dark gold ink in
    // light mode (ButterYellow vanishes on the pale background), bright
    // butter in dark mode where it pops.
    val accent = if (quest.bonus) bonusGold() else CurioColors.CoralBlush
    val pulseAlpha = if (claimable) {
        val pulse = rememberInfiniteTransition(label = "claimPulse")
        pulse.animateFloat(
            initialValue = 1f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "claimPulseAlpha"
        ).value
    } else 1f
    // Claimed quests animate OUT (scale + fade) instead of vanishing.
    AnimatedVisibility(
        visible = !done,
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (done) CurioColors.Sage.copy(alpha = 0.18f)
                        else accent.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = when {
                        done -> CurioIcons.Check
                        quest.bonus -> CurioIcons.AutoAwesome
                        else -> CurioIcons.TaskAlt
                    },
                    contentDescription = null,
                    tint = if (done) CurioColors.Sage else accent,
                    size = 18.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        questTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (done) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (quest.bonus && !done) {
                        Text(
                            "BONUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = bonusGold()
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (done) CurioColors.Sage else accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            if (claimable) {
                // v8.3 — Claim pill: a tap grants the quest's XP.
                Surface(
                    onClick = { onClaim(quest.id) },
                    shape = RoundedCornerShape(50),
                    color = accent,
                    modifier = Modifier.graphicsLayer { alpha = pulseAlpha }
                ) {
                    Text(
                        "Claim +${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            } else {
                Text(
                    if (done) "Done" else "+${quest.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (done) CurioColors.Sage else accent
                )
                // v8.3 — Go chip on in-progress dailies: jump to the screen
                // where this quest's action happens. v8.6 — the discovery
                // quest's Go chip targets the least-engaged lane's Spin deck.
                if (!done) {
                    goRoute?.let { route ->
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            onClick = { onGo(route) },
                            shape = RoundedCornerShape(50),
                            color = accent.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    "Go",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                                CurioForwardArrow(
                                    "Go to quest",
                                    tint = accent,
                                    size = 12.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The badge shelf — every chain stage as a badge in a two-column grid. */
@Composable
private fun BadgeShelf() {
    val allStages = CurioQuests.allStages()
    val unlockedCount = allStages.count { CurioQuests.isStageDone(it) }
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.EmojiEvents,
            "Badges",
            "$unlockedCount of ${allStages.size} earned"
        )
        Spacer(Modifier.height(4.dp))
        allStages.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { stage ->
                    BadgeTile(
                        stage = stage,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * One badge tile — a round MEDAL with a per-stage glyph (v8.13). Earned
 * badges show the badge IN FULL: gradient medal + gold check, title and
 * reward — no task text and no progress bar. Locked badges are silhouette
 * medals with the stage's progress, so the shelf reads as a set of badges
 * rather than a list of chores.
 */
@Composable
private fun BadgeTile(
    stage: QuestStage,
    modifier: Modifier = Modifier
) {
    val unlocked = CurioQuests.isStageDone(stage)
    val progress = CurioQuests.stageProgress(stage)
    val tier = badgeTier(stage)
    val accent = tierAccent(tier)
    val secretLocked = !unlocked && tier == BadgeTier.SECRET
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (unlocked) accent.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        border = BorderStroke(
            1.dp,
            if (unlocked) accent.copy(alpha = 0.30f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            // The medal — shared with Profile and the unlock toast, so every
            // badge renders identically everywhere.
            CurioBadgeMedal(stage = stage, medalSize = 58.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (secretLocked) "???" else stage.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (unlocked) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = if (unlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            if (unlocked) {
                // v8.28 — earned badges show the badge IN FULL: the metal tier
                // chip and the reward, no task text, no progress bar.
                Text(
                    "${tier.displayName} · +${stage.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (secretLocked) {
                // Secret badges never reveal their identity, title or target.
                Text(
                    "Secret · hidden",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    "$progress / ${stage.target}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (progress.toFloat() / stage.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

/**
 * v8.5 — the category passport (spec §6): every lane as a tappable stamp.
 * Explored/mastered lanes show their stamp; unseen lanes look enticing and
 * route straight into that lane's Spin deck (spec §6.3).
 */
@Composable
private fun PassportCard(
    onSpin: (String) -> Unit
) {
    val context = LocalContext.current
    val cats = CurioCategories.visible
    val mastered = cats.count {
        CurioPassport.progress(context, it.id).stamp == CurioPassport.Stamp.MASTERED
    }
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.Star,
            "Category passport",
            "$mastered of ${cats.size} lanes mastered"
        )
        Spacer(Modifier.height(4.dp))
        cats.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { cat ->
                    PassportStamp(
                        cat = cat,
                        modifier = Modifier.weight(1f),
                        onClick = { onSpin(cat.id.routeSlug) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** One passport stamp — the lane's glyph, name and state. */
@Composable
private fun PassportStamp(
    cat: CurioCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val stamp = CurioPassport.progress(context, cat.id).stamp
    // v8.28 — stamp text/glyphs use the READABLE accent ink (deep accent in
    // light, deep hue twin for pale accents + in pastel mode, light twin in
    // dark) instead of the pastel fill accent, which washed the labels and
    // glyphs out. The fills below keep the pastel accent for the soft
    // tinted surfaces.
    val accent = cat.themedAccent()
    val ink = cat.readableAccentInk()
    val label: String
    val glyph: String
    val tint: Color
    when (stamp) {
        CurioPassport.Stamp.MASTERED -> {
            label = "Mastered"; glyph = CurioIcons.TaskAlt; tint = CurioColors.Sage
        }
        CurioPassport.Stamp.EXPLORED -> {
            label = "Explored"; glyph = CurioIcons.Check; tint = CurioColors.Sage
        }
        CurioPassport.Stamp.PEEKED -> {
            label = "Peeked"; glyph = CurioIcons.Star; tint = ink
        }
        CurioPassport.Stamp.UNSEEN -> {
            label = "New · spin!"; glyph = CurioIcons.StarOutline; tint = ink
        }
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = when (stamp) {
            CurioPassport.Stamp.MASTERED -> CurioColors.Sage.copy(alpha = 0.12f)
            CurioPassport.Stamp.UNSEEN -> accent.copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            1.dp,
            if (stamp == CurioPassport.Stamp.UNSEEN) ink.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            CurioIcon(
                name = cat.iconGlyph,
                contentDescription = cat.displayName,
                tint = ink,
                size = 20.dp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                cat.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
