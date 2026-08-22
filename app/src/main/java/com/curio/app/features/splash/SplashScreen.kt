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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.scale
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

/** How long each loading line stays before the next fades in. */
private const val LOADING_LINE_SWAP_MS = 1_100L

/**
 * Splash screen — SIMPLE / MODERN / MATERIAL (v224 redesign).
 *
 * The old splash was a tall stack: 144dp logo box, 72sp gradient wordmark,
 * tagline, dot loader, animated halo and a ground band. This redesign strips
 * it to a compact, restrained M3 composition on the plain theme background:
 *
 *  - Small 64dp logomark: scales+fades in, then floats gently forever
 *  - Modest "Curio" wordmark in the theme ink (Geom display face)
 *  - ONE Material [LinearProgressIndicator] — the M3 loading language
 *  - Rotating curiosity lines ("Loading your curiosity…") crossfading under
 *    the bar — the dynamic, alive part of the page
 *
 * Everything oversized/shimmery is gone; the theme owns every color so light,
 * dark, pastel and Material modes all read right with zero special-casing.
 *
 * Navigation behavior is UNCHANGED: hold until the bundled topic catalog has
 * warmed (800ms minimum, ~6s cap), then route to CRASH / ONBOARDING / HOME.
 * No back button. No interaction.
 */
@Composable
fun SplashScreen(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // ── Entrance trigger: everything animates from this single flag ──────
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        entered = true
    }
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.7f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "splashLogoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "splashContentAlpha"
    )

    // ── Gentle endless float — the logo breathes while the catalog warms ─
    val floatTransition = rememberInfiniteTransition(label = "splashFloat")
    val floatPhase by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashFloatPhase"
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
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Logomark — small, floating ────────────────────────────
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer { translationY = floatPhase * 5.dp.toPx() }
                        .scale(logoScale),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_icon),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // ── Wordmark — modest, theme ink ──────────────────────────
                Text(
                    text = "Curio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .alpha(contentAlpha)
                )

                // ── Material loading: one linear indicator + rotating lines ─
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .padding(top = 26.dp)
                        .alpha(contentAlpha)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.width(148.dp)
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
