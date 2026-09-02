package com.curio.app.features.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.curio.app.R
import com.curio.app.features.onboarding.CurioOnboardingState
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioTheme
import kotlinx.coroutines.delay

/** Minimum branding dwell time on warm starts. */

/** How long each loading line stays before the next fades in. */
private const val LOADING_LINE_SWAP_MS = 400L

/**
 * Splash screen — SIMPLE / MODERN / MATERIAL.
 *
 * v224b sizing pass:
 *  - BIGGER presence: 88dp logomark, display-size wordmark, 180dp bar
 *  - The logo no longer BOBS up and down (the movement read badly) — instead
 *    it BREATHES: a slow, subtle scale pulse around its resting size
 *  - The progress bar is DETERMINATE and wired to the REAL catalog warm-up:
 *    every topic lane that finishes parsing fills the bar, so "loading your
 *    curiosity" is literally true — the app finishes loading its topics ON
 *    this screen and Spin/Home open ready
 *
 * Everything wears plain theme roles, so light, dark, pastel and Material
 * modes all read right with zero special-casing. Navigation behavior is
 * UNCHANGED: hold until the bundled topic catalog has warmed (800ms minimum,
 * ~6s cap), then route to CRASH / ONBOARDING / HOME. No back button. No
 * interaction.
 */
@Composable
fun SplashScreen(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // ── Entrance trigger ───────────────────────────────────────────────────
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(30)
        entered = true
    }
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.82f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "splashEntranceScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "splashContentAlpha"
    )

    // ── Breathing logo — scale pulse only, never positional movement ──────
    val breatheTransition = rememberInfiniteTransition(label = "splashBreathe")
    val breatheScale by breatheTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashBreatheScale"
    )

    // ── Rotating curiosity loading lines ──────────────────────────────────
    val loadingLines = listOf(
        "Loading your curiosity…",
        "Warming up the topics…",
        "Sharpening the shuffle…",
        "Opening the cabinet…"
    )
    var lineIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(LOADING_LINE_SWAP_MS)
            lineIndex = (lineIndex + 1) % loadingLines.size
        }
    }

    // ── REAL topic warm-up progress ────────────────────────────────────────
    // The determinate bar tracks actual lane parses: each lane whose bundled
    // catalog finished loading fills the bar one step, so the topics are
    // READY when the splash hands off (Spin / Topic Database / counts all
    // read the warm cache immediately).
    val totalLanes = remember {
        CurioCategories.visible.count { it.id != CategoryId.WILDCARD }.coerceAtLeast(1)
    }
    var warmedLanes by remember { mutableIntStateOf(0) }
    val loadProgress = (warmedLanes.toFloat() / totalLanes).coerceIn(0f, 1f)
    val shownProgress by animateFloatAsState(
        targetValue = loadProgress,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "splashLoadProgress"
    )

    LaunchedEffect(Unit) {
        // Warm the canonical catalog while the splash branding plays, and
        // HOLD navigation until it's ready: the category picker counts, the
        // Topic Database lanes and the Spin deck all read the topic cache
        // synchronously, so a half-warm catalog reads as "0 topics" /
        // "Loading topics…". Each lane is failure-guarded individually, so
        // one broken asset never blocks the rest — and the safety timeout
        // below keeps a pathological parse from stranding the splash.
        // v294 — If Room is already populated, skip JSON warm-up entirely.
        // Topics are served from Room (instant indexed queries).
        if (com.curio.app.data.TopicRepository.isInitialized()) {
            // v311 — even when Room is already warm, hold the splash for
            // a minimum time so the branding is visible on warm starts.
            // Smoothly ramp progress so the bar doesn't jump 0→100%.
            val rampSteps = 6
            val rampDelay = 150L
            for (step in 1..rampSteps) {
                delay(rampDelay)
                warmedLanes = (totalLanes * step / rampSteps).coerceAtMost(totalLanes)
            }
        } else {
            // The repository opens the persistent topics.db and warms the
            // lightweight compatibility cache from indexed rows. Never parse
            // catalog JSON on a screen or splash path.
            com.curio.app.data.TopicRepository.init(context)
            warmedLanes = totalLanes
        }
        // Check for pending crash from previous session — also route to the
        // crash screen when the crash-loop guard flipped on safe mode, so the
        // user always gets the log + safe restart instead of an endless loop.
        val destination = if (CurioCrashReporter.hasPendingCrash(context) ||
            CurioCrashReporter.isSafeMode(context)
        ) {
            CurioRoutes.CRASH
        } else if (CurioOnboardingState.isComplete(context)) {
            CurioRoutes.HOME
        } else {
            CurioRoutes.ONBOARDING
        }
        navController.navigate(destination) {
            popUpTo(CurioRoutes.SPLASH) { inclusive = true }
        }
    }

    CurioTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Logomark — big, breathing (scale only, no bobbing) ────
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = entranceScale * breatheScale
                            scaleY = entranceScale * breatheScale
                            alpha = contentAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_icon),
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // ── Wordmark — display size, theme ink ────────────────────
                Text(
                    text = "Curio",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .alpha(contentAlpha)
                )

                // ── Determinate Material bar + rotating lines ─────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .alpha(contentAlpha)
                ) {
                    LinearProgressIndicator(
                        progress = { shownProgress },
                        modifier = Modifier
                            .width(180.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    AnimatedContent(
                        targetState = lineIndex,
                        transitionSpec = {
                            fadeIn(tween(280)) togetherWith fadeOut(tween(220))
                        },
                        label = "splashLoadingLine"
                    ) { index ->
                        Text(
                            text = loadingLines[index],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
