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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.theme.curioGoldInk
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.components.ConfettiBurst
import com.curio.app.ui.components.BadgeTier
import com.curio.app.ui.components.CurioBadgeDetailDialog
import com.curio.app.ui.components.CurioBadgeMedal
import com.curio.app.ui.components.MergedChainBadge
import com.curio.app.ui.components.badgeTier
import com.curio.app.ui.components.mergedChainBadges
import com.curio.app.ui.components.tierAccent
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.components.curioGlassEdge
import com.curio.app.ui.pet.CurioPetHeroCard
import com.curio.app.ui.pet.CurioPetSprite
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import kotlinx.coroutines.delay
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.isCurioDarkTheme
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
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the lane wash.
            .background(heroPageBackground())
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
                state = listState,
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
        // Side scroll indicator — thin overlay knob, grows on touch.
        CurioVerticalScrollIndicator(
            state = listState.scrollIndicatorState,
            onScrollBy = { listState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = SettingsHeroTotalHeight + 10.dp, bottom = 16.dp)
        )
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
                            color = curioSageInk()
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
                // than the coral used everywhere else (v7.103). v20 — deep
                // gold ink on light cream so it never washes out.
                tint = curioGoldInk(),
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
            color = curioRoseInk(),
            trackColor = curioRoseInk().copy(alpha = 0.14f)
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
    // v27l — follows the shared hero family so the azure hero option applies
    // here too (settingsRoseAccent already branches on heroBlueState).
    val roseHero = settingsRoseAccent()
    CurioSettingsCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    // v81 — dark: the deep rose fill keeps the white check
                    // readable (pale coral + white washes out on black).
                    .background(if (isCurioDarkTheme()) CurioColors.HomeRosewoodDark else CurioColors.CoralBlush),
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
        // v90 — the actionable CTA stays the solid brand rose; the
        // informational "In progress" state flips to a DYNAMIC tinted glass
        // (theme-aware rose ink on the surface glass, glass edge in dark) —
        // no more solid pink block for a non-action.
        val actionable = stage.navRoute != null
        Surface(
            onClick = { stage.navRoute?.let(onNavigate) },
            shape = RoundedCornerShape(50),
            color = if (actionable) curioRoseInk()
                    else lerp(MaterialTheme.colorScheme.surfaceContainerHigh, curioRoseInk(), 0.12f),
            enabled = actionable,
            modifier = Modifier
                .fillMaxWidth()
                .curioGlassEdge(RoundedCornerShape(50))
        ) {
            Text(
                text = if (actionable) "Start · +${stage.xpReward} XP"
                       else "In progress · ${done.coerceAtMost(stage.target)}/${stage.target}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (actionable) Color.White else curioRoseInk(),
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
 * v42 — "Quest paths": every path is a tappable CARD in a two-column grid
 * (no more boring list rows). Each card shows the chain glyph, its live
 * progress, and the chain's merged medal (best-earned tier). Tapping a card
 * opens its stage trail in a dialog — the next actionable stage carries a
 * Go chip. Below sits the MERGED badge shelf: one medal per chain, earned
 * badges first by rarity, and tapping a medal opens its detail dialog.
 */
@Composable
private fun PathsCard(
    onNavigate: (String) -> Unit = {}
) {
    val allStages = CurioQuests.allStages()
    val unlockedCount = allStages.count { CurioQuests.isStageDone(it) }
    var showBadges by rememberSaveable { mutableStateOf(false) }
    // v42 — per-path detail + medal detail both open from this card.
    var pathDetail by remember { mutableStateOf<QuestChain?>(null) }
    var badgeDetail by remember { mutableStateOf<QuestStage?>(null) }
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.Flag,
            "Quest paths",
            "${CurioQuests.Chains.size} paths · $unlockedCount badges"
        )
        Spacer(Modifier.height(6.dp))
        if (CurioQuests.Chains.isEmpty()) {
            Text(
                "Every path complete. The whole shelf is yours!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        // v42 — card-per-path grid: completed paths keep their medals, open
        // ones show live progress. Tap a card for its stage trail.
        CurioQuests.Chains.chunked(2).forEach { rowChains ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowChains.forEach { chain ->
                    PathCard(
                        chain = chain,
                        onClick = { pathDetail = chain },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowChains.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        // Badge shelf — one tappable row that opens the merged shelf dialog.
        // (v58 — the pinned medal strip that used to sit ABOVE this row is
        // gone from the quest view: only this shelf row + the path-card
        // medals remain; the full shelf still opens from here.)
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
                    tint = curioRoseInk(),
                    size = 18.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Badge shelf",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "$unlockedCount of ${allStages.size} stages earned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "View",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = curioRoseInk()
            )
            CurioForwardArrow(
                "View badge shelf",
                tint = curioRoseInk(),
                size = 14.dp
            )
        }
    }
    // v42 — medal detail: tapping a pinned badge or a shelf tile opens its
    // name, tier, description, and live progress.
    badgeDetail?.let { stage ->
        CurioBadgeDetailDialog(stage = stage, onDismiss = { badgeDetail = null })
    }
    // v42 — per-path dialog: the chain's stage trail with Go chips.
    pathDetail?.let { chain ->
        PathDetailDialog(
            chain = chain,
            onNavigate = onNavigate,
            onDismiss = { pathDetail = null }
        )
    }
    if (showBadges) {
        MergedBadgeShelfDialog(
            onDismiss = { showBadges = false },
            onBadgeClick = { stage ->
                badgeDetail = stage
                showBadges = false
            }
        )
    }
}

/** v42 — ONE path card in the quest-paths grid: glyph, progress, medal. */
@Composable
private fun PathCard(
    chain: QuestChain,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = CurioQuests.chainProgress(chain)
    val complete = done == chain.stages.size
    // The card wears the chain's merged medal — its best-earned tier, or a
    // silhouette previewing its best rarity while locked.
    val merged = mergedChainBadges().firstOrNull { it.chain.id == chain.id }
    val display = merged?.displayStage ?: chain.stages.last()
    val tier = badgeTier(display)
    val accent = tierAccent(tier)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        // v27n — OPAQUE fills so the elevation shadow stays clean.
        color = if (complete) {
            lerp(MaterialTheme.colorScheme.surfaceContainerLow, curioSageInk(), 0.08f)
        } else {
            lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.06f)
        },
        border = BorderStroke(1.dp, accent.copy(alpha = if (complete) 0.35f else 0.22f)),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        // v81 — dark: the deep rose chip keeps the white glyph
                        // readable (the pale coral gradient washes white out).
                        .background(Brush.verticalGradient(CurioGradients.cardGradient(
                            if (isCurioDarkTheme()) CurioColors.HomeRosewoodDark else CurioColors.CoralBlush
                        ))),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = chain.glyph,
                        contentDescription = null,
                        tint = Color.White,
                        size = 17.dp
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
                // The chain's merged medal (best-earned tier).
                CurioBadgeMedal(stage = display, medalSize = 34.dp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / chain.stages.size.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (complete) curioSageInk() else accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (complete) "Complete · +${chain.stages.sumOf { it.xpReward }} XP"
                    else "$done / ${chain.stages.size} stages",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (complete) curioSageInk() else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (complete) {
                    CurioIcon(CurioIcons.Check, null, tint = curioSageInk(), size = 14.dp)
                } else {
                    Text(
                        tier.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = accent,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** v42 — a path's stage trail in a dialog (tap the card to open it). */
@Composable
private fun PathDetailDialog(
    chain: QuestChain,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val done = CurioQuests.chainProgress(chain)
    val merged = mergedChainBadges().firstOrNull { it.chain.id == chain.id }
    val display = merged?.displayStage ?: chain.stages.last()
    val nextIndex = chain.stages.indexOfFirst { !CurioQuests.isStageDone(it) }
    AlertDialog(
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CurioBadgeMedal(stage = display, medalSize = 56.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    chain.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    textAlign = TextAlign.Center
                )
                Text(
                    "$done of ${chain.stages.size} stages · ${chain.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chain.stages.forEachIndexed { index, stage ->
                    val stageDone = CurioQuests.isStageDone(stage)
                    val isCurrent = !stageDone && stage.id == CurioQuests.currentQuest()?.id
                    ChainStageRow(
                        index = index,
                        stage = stage,
                        done = stageDone,
                        isCurrent = isCurrent,
                        isNext = index == nextIndex,
                        onNavigate = {
                            stage.navRoute?.let { route ->
                                onNavigate(route)
                                onDismiss()
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) { Text("Close") }
        }
    )
}

/** v42 — the MERGED badge shelf: one medal per chain, earned first by
 *  rarity with labeled sections; locked chains silhouette their best tier.
 */
@Composable
private fun MergedBadgeShelfDialog(
    onDismiss: () -> Unit,
    onBadgeClick: (QuestStage) -> Unit
) {
    val merged = mergedChainBadges()
    val earned = merged.filter { it.earned }
    val locked = merged.filterNot { it.earned }
    val allStages = CurioQuests.allStages()
    val unlockedCount = allStages.count { CurioQuests.isStageDone(it) }
    AlertDialog(
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        onDismissRequest = onDismiss,
        title = { Text("Badge shelf · $unlockedCount stages earned") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (earned.isNotEmpty()) {
                    ShelfSectionLabel("Earned")
                    earned.chunked(2).forEach { rowBadges ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowBadges.forEach { m ->
                                MergedBadgeTile(
                                    m = m,
                                    onClick = { onBadgeClick(m.displayStage) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowBadges.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (locked.isNotEmpty()) {
                    ShelfSectionLabel("Locked · still to earn")
                    locked.chunked(2).forEach { rowBadges ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowBadges.forEach { m ->
                                MergedBadgeTile(
                                    m = m,
                                    onClick = { onBadgeClick(m.displayStage) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowBadges.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) { Text("Close") }
        }
    )
}

/** v42 — a small caps section label inside the shelf dialog. */
@Composable
private fun ShelfSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 2.dp)
    )
}

/** v42 — one MERGED medal tile: the chain's best-earned tier stands in for
 *  the whole chain; an "upgraded" chip shows when earlier rarities were
 *  earned too. Locked chains preview their highest-rarity silhouette.
 */
@Composable
private fun MergedBadgeTile(
    m: MergedChainBadge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tier = badgeTier(m.displayStage)
    val accent = tierAccent(tier)
    val secretLocked = !m.earned && tier == BadgeTier.SECRET
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        // v27n — OPAQUE fills (the old alphas let the elevation shadow bleed).
        color = if (m.earned) {
            lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.10f)
        } else {
            lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceVariant, 0.35f)
        },
        border = BorderStroke(1.dp, accent.copy(alpha = if (m.earned) 0.30f else 0.10f)),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            CurioBadgeMedal(stage = m.displayStage, medalSize = 54.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                m.chain.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (m.earned) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = if (m.earned) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(3.dp))
            if (m.earned) {
                // Best earned rarity; an "upgraded" chip when earlier tiers
                // were earned too (bronze → silver shows silver + upgraded).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        tier.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = accent,
                        maxLines = 1
                    )
                    if (m.earnedTiers.size > 1) {
                        Text(
                            "· upgraded",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = curioSageInk(),
                            maxLines = 1
                        )
                    }
                }
            } else if (secretLocked) {
                Text(
                    "Secret · hidden",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            } else {
                val progress = CurioQuests.stageProgress(m.displayStage)
                Text(
                    "$progress / ${m.displayStage.target}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { (progress.toFloat() / m.displayStage.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
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
        done -> curioSageInk()
        isCurrent -> curioRoseInk()
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
                color = if (isCurrent) curioRoseInk() else MaterialTheme.colorScheme.onSurfaceVariant
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
                color = if (isCurrent) curioRoseInk()
                else curioRoseInk().copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (isCurrent) "Go" else "Start",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCurrent) Color.White else curioRoseInk()
                    )
                    CurioForwardArrow(
                        if (isCurrent) "Go to quest" else "Start this quest",
                        tint = if (isCurrent) Color.White else curioRoseInk(),
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
                color = curioSageInk(),
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
                    // v27n — opaque sage-tinted fill (was 14% alpha, which let
                    // the elevation shadow bleed through).
                    color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, curioSageInk(), 0.14f),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        "+${xpPop?.first ?: 0} XP",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = curioSageInk(),
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
    // v81 — the Claim pill is a FILL: white text needs a deep enough teal in
    // BOTH modes (the soft legacy teal washes white out); icons, progress
    // and the +XP text keep the soft teal identity.
    val claimFill = CurioColors.CategoryTeal
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
                        if (done) curioSageInk().copy(alpha = 0.18f)
                        else accent.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = if (done) CurioIcons.Check else CurioIcons.CalendarToday,
                    contentDescription = null,
                    tint = if (done) curioSageInk() else accent,
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
                    color = if (done) curioSageInk() else accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            if (claimable) {
                Surface(
                    onClick = onClaim,
                    shape = RoundedCornerShape(50),
                    color = claimFill
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
                    color = if (done) curioSageInk() else accent
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
                // v27n — shadow FIRST (behind the fill) and the fill OPAQUE:
                // the old order painted the shadow on top of a translucent
                // fill, smearing blur over the tile.
                .shadow(2.dp, RoundedCornerShape(11.dp))
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
 * background), bright butter in dark mode where it pops. v20 — delegates to
 * the shared [curioGoldInk] helper so the light-mode fix lives in one place.
 */
@Composable
private fun bonusGold(): Color = curioGoldInk()

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
    val accent = if (quest.bonus) bonusGold() else curioRoseInk()
    // v81 — the Claim pill is a FILL: white text needs the DEEP ink twin in
    // both modes (in dark the bright CoralBlush / ButterYellow twins would
    // wash the white out); icons, progress and the +XP text keep the
    // bright accent.
    val claimFill = if (quest.bonus) CurioColors.GoldInk else CurioColors.CoralInk
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
                        if (done) curioSageInk().copy(alpha = 0.18f)
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
                    tint = if (done) curioSageInk() else accent,
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
                    color = if (done) curioSageInk() else accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            if (claimable) {
                // v8.3 — Claim pill: a tap grants the quest's XP.
                Surface(
                    onClick = { onClaim(quest.id) },
                    shape = RoundedCornerShape(50),
                    color = claimFill,
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
                    color = if (done) curioSageInk() else accent
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
    // v8.5 — the passport reads as a 4×3 stamp grid per page (12 lanes); the
    // 21 lanes page into two swipeable pages with a dot indicator.
    val pages = cats.chunked(12)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.Star,
            "Category passport",
            "$mastered of ${cats.size} lanes mastered"
        )
        Spacer(Modifier.height(4.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageCats = pages[page]
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pageCats.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { cat ->
                            PassportStamp(
                                cat = cat,
                                modifier = Modifier.weight(1f),
                                onClick = { onSpin(cat.id.routeSlug) }
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (pages.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
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
            label = "Mastered"; glyph = CurioIcons.TaskAlt; tint = curioSageInk()
        }
        CurioPassport.Stamp.EXPLORED -> {
            label = "Explored"; glyph = CurioIcons.Check; tint = curioSageInk()
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
            // v27n — OPAQUE stamp fills (the old 10–45% alphas let the
            // elevation shadow bleed through). v27r — the stamp's ring
            // border is back (the elevation pass removed it); UNSEEN stamps
            // wear their accent ring, the rest a neutral outline.
            CurioPassport.Stamp.MASTERED ->
                lerp(MaterialTheme.colorScheme.surfaceContainerLow, curioSageInk(), 0.12f)
            CurioPassport.Stamp.UNSEEN ->
                lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.10f)
            else ->
                lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceVariant, 0.45f)
        },
        border = BorderStroke(
            1.dp,
            if (stamp == CurioPassport.Stamp.UNSEEN) ink.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shadowElevation = 2.dp,
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
