package com.curio.app.features.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.curio.app.R
import com.curio.app.features.onboarding.CurioOnboardingState
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.CurioTheme
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.TopicJsonLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Safety cap on startup catalog warm-up — never strand the splash on a
 *  slow parse. The splash holds navigation until the canonical lanes are
 *  cached so counts/loading states never read a half-warm catalog, but a
 *  pathological parse must not block the app past this. */
private const val CATALOG_WARM_TIMEOUT_MS = 6_000L

/**
 * Splash screen — see Curio splash contract.
 *
 * The first thing the user sees on app launch. Covers the gap between
 * process start and MainActivity being ready.
 *
 * Upgraded with:
 *  - Morph entrance: logo scales from 0 → 1 with elastic spring
 *  - Shimmer effect: subtle gradient sweep across the logo icon
 *  - Breathing dots: 3-dot loader with ambient pulse + individual wave
 *  - Animated background gradient: subtle tone shift during loading
 *
 * Phase 3+ will replace the LaunchedEffect body with:
 *   - init Room DB
 *   - read `onboardingComplete` flag from DataStore
 *   - if false → `CurioRoutes.ONBOARDING`
 *   - else    → `CurioRoutes.HOME`
 *
 * No back button. No interaction. Auto-dismisses after the branding plays
 * and the bundled topic catalog has warmed (800ms minimum, ~6s cap).
 */
@Composable
fun SplashScreen(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pulseIndex by remember { mutableStateOf(0) }
    var entranceReady by remember { mutableStateOf(false) }

    // ── Logo morph entrance trigger ───────────────────────────────────────
    LaunchedEffect(Unit) {
        entranceReady = true
    }

    // ── Breathing background gradient ─────────────────────────────────────
    val bgTransition = rememberInfiniteTransition(label = "splashBg")
    val bgShift by bgTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgShift"
    )

    // ── Shimmer for the logo icon ─────────────────────────────────────────
    val shimmerTransition = rememberInfiniteTransition(label = "splashShimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CurioMotion.Durations.Shimmer,
                easing = FastOutSlowInEasing
            )
        ),
        label = "shimmerOffset"
    )

    // ── Dot loader pulse wave ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            pulseIndex = (pulseIndex + 1) % 3
            delay(200)
        }
    }

    LaunchedEffect(Unit) {
        // Warm the canonical catalog while the splash branding plays, and
        // HOLD navigation until it's ready: the category picker counts, the
        // Topic Database lanes and the Spin deck all read the topic cache
        // synchronously, so a half-warm catalog reads as "0 topics" /
        // "Loading topics…". Each lane is failure-guarded individually, so
        // one broken asset never blocks the rest — and the safety timeout
        // below keeps a pathological parse from stranding the splash.
        val warmCatalog = launch(Dispatchers.Default) {
            CurioCategories.visible
                .filter { it.id != CategoryId.WILDCARD }
                .forEach { category ->
                    try {
                        TopicJsonLoader.load(category.id)
                    } catch (e: CancellationException) {
                        // Let the timeout's cancellation abort the loop promptly
                        // (a hard ~6s cap — runCatching would swallow it).
                        throw e
                    } catch (_: Throwable) {
                        // One broken lane never blocks the rest from warming.
                    }
                }
        }
        delay(800)
        // Cap the total warm-up at ~6s (a cold first parse of the bundled
        // 5MB+ catalogs can outlast the 800ms branding on slow devices).
        withTimeoutOrNull(CATALOG_WARM_TIMEOUT_MS) { warmCatalog.join() }
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // ── Animated background halo ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .offset(x = ((bgShift - 0.5f) * 40).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CurioColors.CoralBlush.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(50)
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // ── Logomark with morph entrance + shimmer ────────────────
                    Box(
                        modifier = Modifier.size(144.dp)
                            .scale(
                                if (entranceReady) 1f else 0f
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Shimmer overlay
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            CurioColors.CreamWhite.copy(alpha = 0.25f),
                                            Color.Transparent
                                        ),
                                        startX = shimmerOffset * 200f,
                                        endX = (shimmerOffset + 0.3f) * 200f
                                    ),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        // v126 — the splash renders the NEW v2 card art
                        // directly (drawable-nodpi/ic_launcher_icon.png, the
                        // same source as the launcher foreground), NOT the
                        // old ic_launcher_art raster which carried the
                        // previous white border. The raw art fills the 112dp
                        // logo box at its native proportions — this is why
                        // the icon looked BIGGER here than on the launcher
                        // (the launcher foreground was inset to the adaptive
                        // safe zone; v126 reduced that inset 28→18dp so the
                        // launcher card now matches the splash's presence).
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_icon),
                            contentDescription = null,
                            modifier = Modifier.size(112.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // ── App name ──────────────────────────────────────────────
                    Text(
                        text = "Curio",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // ── Tagline — the app's identity line under the name ─────
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── 3-dot loader — breathing pulse + sequence wave ──────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val isPulsing = index == pulseIndex
                            val dotScale by animateFloatAsState(
                                targetValue = if (isPulsing) 1.35f else 1f,
                                animationSpec = CurioMotion.Springs.Snappy,
                                label = "dotScale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .scale(dotScale)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isPulsing)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
