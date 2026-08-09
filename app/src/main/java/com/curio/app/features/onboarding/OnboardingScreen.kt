package com.curio.app.features.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.R
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.features.settings.settingsReadableInk
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.MorphEntrance
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.launch

/**
 * First-launch onboarding — the torn-rose family redesign (v7.102).
 *
 * The hero is the whole show: a solid rose banner torn with the shared
 * [SoftTornBottomShape] seam covers well over HALF the screen, and every
 * slide's content — the intro kicker + headline + subtext, the theme
 * options, and the permission cards — lives INSIDE the banner in its
 * readable ink. The illustration tiles are gone; only a slim Curio
 * wordmark (with the tagline) sits at the top of the banner above a
 * subtle mirrored watermark collage. Below the tear: page dots and the
 * Skip / Next controls. The setup step's permission cards are borderless
 * [CurioSettingsCard] paper boxes with coral icon chips.
 */
@Composable
fun OnboardingScreen(navController: NavController) {
    // Intro slides + theme step + permission setup (v7.100 adds the theme
    // picker between the intros and the permissions).
    val pagerState = rememberPagerState(pageCount = { OnboardingSlides.size + 2 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isLastSlide = pagerState.currentPage == OnboardingSlides.size + 1

    // ── Setup-step permission state ───────────────────────────────────
    var notificationGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var micGranted by remember { mutableStateOf(hasMicPermission(context)) }
    // "Display over other apps" — special access for the floating explore
    // bubble. No runtime dialog on Android 10+, so "Allow" opens the system
    // settings page; the ON_RESUME observer picks up the grant on return.
    // v7.35 — [AppPreferences.overlayActuallyUsable] (not raw canDrawOverlays):
    // an Android 15+ first-time grant can sit in the system's PENDING state
    // where canDrawOverlays() lies and no overlay ever shows — the card
    // stays "Allow" until the AppOps state actually settles (toggle off/on
    // in the system page resolves it).
    var overlayGranted by remember { mutableStateOf(AppPreferences.overlayActuallyUsable(context)) }
    // v8.1 — tracks a trip to the overlay settings page so the ON_RESUME
    // observer can record a decline (return without granting) and stop the
    // app from re-asking the permission on every explore.
    var overlayAwaitingReturn by remember { mutableStateOf(false) }
    // "Want the daily shuffle reminder on?" — only reachable once
    // notifications are granted; applied to prefs the moment it flips.
    var reminderWanted by rememberSaveable { mutableStateOf(false) }
    val finishOnboardingNow: () -> Unit = {
        finishOnboarding(context, navController)
    }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
        // If they asked for the reminder before granting, it lands now.
        if (granted && reminderWanted) {
            AppPreferences.setReminderEnabled(context, true)
        }
    }
    val requestMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micGranted = granted }
    // The result callback is empty on purpose: `StartActivityForResult`
    // fires while the settings page is still open (permission not yet
    // granted), so the ON_RESUME observer above is the real source of truth.
    val requestOverlay = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    fun openOverlaySettings() {
        val launched = runCatching {
            overlayAwaitingReturn = true
            requestOverlay.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
        if (launched.isFailure) overlayAwaitingReturn = false
    }

    // Re-read permission state when returning from the system Settings
    // screen — users can flip grants mid-session and the cards should
    // reflect reality the moment they come back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = hasNotificationPermission(context)
                micGranted = hasMicPermission(context)
                overlayGranted = AppPreferences.overlayActuallyUsable(context)
                // v8.1 — returning from the overlay settings page: a grant
                // opens the door again; coming back without granting records
                // the "no" so the app stops re-asking (the Settings toggle
                // still grants it anytime).
                if (overlayAwaitingReturn) {
                    overlayAwaitingReturn = false
                    if (AppPreferences.overlayActuallyUsable(context)) {
                        AppPreferences.setOverlayAskDeclined(context, false)
                    } else {
                        AppPreferences.setOverlayAskDeclined(context, true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs behind the slides
        // (the Home/Profile language; the wildcard sparkle leads because
        // onboarding is category-neutral).
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            // ── The big torn-rose hero — covers well over half the screen
            //    (v7.111: deepened from 0.62 to 0.70 of the screen height so
            //    the tear dips toward the lower third). EVERY slide renders
            //    INSIDE the banner: intro texts, the theme options and the
            //    permission cards all sit on the rose fill in its readable
            //    ink, with the Curio wordmark + tagline at the top. Below
            //    the tear are only the page dots and the Skip / Next controls.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.70f)
            ) {
                // The rose banner + ragged tear + watermark collage fill the
                // whole box (drawn first; the wordmark + pager overlay it).
                OnboardingHeroBackdrop()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // ── Brand wordmark + tagline — v7.112: enlarged again
                    //    so Curio leads the intro without crowding the tagline ──
                    Spacer(Modifier.height(26.dp))
                    Text(
                        text = "Curio",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        ),
                        color = heroInk(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.labelSmall,
                        color = heroInk().copy(alpha = 0.82f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    // ── Slide area — the pager fills the rest of the banner ──
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(start = 24.dp, end = 24.dp, bottom = 30.dp)
                    ) { pageIndex ->
                        when (pageIndex) {
                            OnboardingSlides.size -> {
                                // Theme step: light / dark / system + pastel toggle.
                                MorphEntrance {
                                    ThemeSlide()
                                }
                            }
                            OnboardingSlides.size + 1 -> {
                                // Final step: permission setup, not an intro slide.
                                SetupSlide(
                                    notificationGranted = notificationGranted,
                                    micGranted = micGranted,
                                    overlayGranted = overlayGranted,
                                    reminderWanted = reminderWanted,
                                    onReminderChange = { wanted ->
                                        reminderWanted = wanted
                                        AppPreferences.setReminderEnabled(context, wanted)
                                    },
                                    onRequestNotifications = {
                                        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    onRequestMic = {
                                        requestMic.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                    onRequestOverlay = { openOverlaySettings() }
                                )
                            }
                            else -> {
                                MorphEntrance {
                                    OnboardingSlide(slide = OnboardingSlides[pageIndex])
                                }
                            }
                        }
                    }
                }
            }

            // ── Page dots (empty on the final setup step — keeps layout stable) ─
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (!isLastSlide) {
                    // One dot per intro slide + one for the theme step.
                    (0..OnboardingSlides.size).forEach { index ->
                        val selected = pagerState.currentPage == index
                        PageDot(
                            selected = selected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }
            }

            // ── Bottom controls — anchored to the bottom edge: a flexible
            //    spacer absorbs the space between the tear and the controls,
            //    so the Skip / Next row sits on the navigation inset on every
            //    screen height instead of floating high under the banner ──
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = finishOnboardingNow,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        if (isLastSlide) {
                            finishOnboardingNow()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(26.dp),
                    contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (isLastSlide) "Let's go" else "Next",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Big torn-rose hero backdrop — v7.102, deepened in v7.111: the banner now
// fills well past half the screen (its host Box is sized to 70% height in
// the screen body), tears with the shared bold seam at its bottom edge, and
// wears the mirrored wildcard watermark collage at a whisper. The wordmark
// + pager overlay it. The tear geometry adapts to the banner's height
// (align + small offsets instead of the old fixed 170dp construction).
// ─────────────────────────────────────────────────────────────────────────────

/** Fixed tear seed — the onboarding hero always tears in the SAME pattern
 *  (its own seed; Settings wears 0x5EED, Profile 0xC0FEE). Never re-rolls. */
private const val ONBOARDING_TEAR_SEED = 0x0B0A5EED

/** One mirrored hero watermark pair — the left glyph mirrors the right
 *  (the Settings/Profile hero construction). */
private data class OnboardingHeroPair(
    val biasX: Float,
    val biasY: Float,
    val size: Dp,
    val rotation: Float,
    val alpha: Float
)

/** The hero's readable ink — shared by the backdrop, the wordmark and every
 *  slide that renders inside the banner. */
@Composable
private fun heroInk(): Color = settingsReadableInk(settingsRoseAccent())

@Composable
private fun OnboardingHeroBackdrop() {
    val heroTornShape = remember(ONBOARDING_TEAR_SEED) {
        SoftTornBottomShape(ONBOARDING_TEAR_SEED, bold = true)
    }
    val sheetShape = remember(ONBOARDING_TEAR_SEED) {
        SoftTornSheetShape(ONBOARDING_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
    }
    val fill = settingsRoseAccent()
    val ink = settingsReadableInk(fill)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Under-sheet — the shared white paper layer, so the tear stays
        // bright beneath the rose hero in every theme. It pokes a couple of
        // dp BELOW the hero so the ragged seam reads.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset(y = 12.dp)
                .height(46.dp)
                .clip(sheetShape)
                .background(CurioColors.CreamWhite)
        )
        // ── Torn-edge shadow — hairline dark rim under the seam.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
                .offset(y = 1.dp)
                .clip(heroTornShape)
                .background(Color.Black.copy(alpha = 0.20f))
        )
        // ── Solid rose banner, torn bottom edge ────────────────────────
        Surface(
            shape = heroTornShape,
            color = fill,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mirrored watermark collage — the wildcard family's symbols
                // float near the banner edges at a whisper so the slide
                // content always reads first (Settings' exact construction,
                // spread for the taller banner).
                val symbols = CurioIcons.heroWatermarkSymbols(CategoryFamily.WILDCARD)
                val pairs = listOf(
                    OnboardingHeroPair(biasX = 0.93f, biasY = -0.80f, size = 46.dp, rotation = 12f, alpha = 0.10f),
                    OnboardingHeroPair(biasX = 0.58f, biasY = -0.55f, size = 50.dp, rotation = 8f, alpha = 0.11f),
                    OnboardingHeroPair(biasX = 0.95f, biasY = 0.10f, size = 58.dp, rotation = 14f, alpha = 0.10f),
                    OnboardingHeroPair(biasX = 0.60f, biasY = 0.70f, size = 52.dp, rotation = 10f, alpha = 0.11f),
                    OnboardingHeroPair(biasX = 0.95f, biasY = 1.05f, size = 46.dp, rotation = 6f, alpha = 0.10f)
                )
                pairs.forEachIndexed { i, pair ->
                    OnboardingHeroSymbol(
                        glyph = symbols[i * 2],
                        alignment = BiasAlignment(-pair.biasX, pair.biasY),
                        size = pair.size,
                        rotation = -pair.rotation,
                        alpha = pair.alpha,
                        tint = ink
                    )
                    OnboardingHeroSymbol(
                        glyph = symbols[i * 2 + 1],
                        alignment = BiasAlignment(pair.biasX, pair.biasY),
                        size = pair.size,
                        rotation = pair.rotation,
                        alpha = pair.alpha,
                        tint = ink
                    )
                }
            }
        }
    }
}

/** One mirrored watermark glyph on the hero banner — the banner's readable
 *  ink at a soft alpha (the Settings/Profile collage construction). */
@Composable
private fun BoxScope.OnboardingHeroSymbol(
    glyph: String,
    alignment: Alignment,
    size: Dp,
    rotation: Float,
    alpha: Float,
    tint: Color
) {
    CurioIcon(
        name = glyph,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        size = size,
        modifier = Modifier
            .align(alignment)
            .padding(10.dp)
            .graphicsLayer { rotationZ = rotation }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Intro slides — v7.102: no more illustration tile. Each slide renders INSIDE
// the big rose hero: a step kicker, a bold headline and a short subtext, all
// in the banner's readable ink.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingSlide(slide: OnboardingSlideData) {
    val ink = heroInk()
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val compact = maxHeight < 300.dp
        Column(
            // Scrollable like the theme/setup steps — large system font
            // scales must never clip the headline/subtext (the Box centers
            // the scrollable column as a whole, so it stays centered when
            // it fits).
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Step kicker — SHUFFLE / EXPLORE / KEEP ────────────────
            Text(
                text = slide.kicker,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = ink.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))

            Text(
                text = slide.headline,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = if (compact) 34.sp else 42.sp
                ),
                color = ink,
                textAlign = TextAlign.Center,
                maxLines = 3
            )

            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

            Text(
                text = slide.subtext,
                style = MaterialTheme.typography.bodyLarge,
                color = ink.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun SetupSlide(
    notificationGranted: Boolean,
    micGranted: Boolean,
    overlayGranted: Boolean,
    reminderWanted: Boolean,
    onReminderChange: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    // Renders INSIDE the big rose hero (v7.102): heading in the banner's
    // ink, permission cards as paper boxes. Centered when the content fits,
    // scrollable on very small screens — the Box centers the scrollable
    // column as a whole.
    val ink = heroInk()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Make Curio yours",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = ink,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Grant what you like. You can change it anytime in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = ink.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            // ── Notifications ─────────────────────────────────────────
            PermissionCard(
                glyph = CurioIcons.Notifications,
                title = "Notifications",
                subtitle = "Explore-session timer & reminders, plus the daily shuffle nudge",
                granted = notificationGranted,
                onRequest = onRequestNotifications
            ) {
                // Ask whether the daily shuffle reminder should be on —
                // only once notifications are actually granted (it can't
                // work without them). Rides INSIDE the notifications card
                // behind a divider.
                if (notificationGranted) {
                    CurioSettingsDivider()
                    ReminderRow(
                        reminderWanted = reminderWanted,
                        onReminderChange = onReminderChange
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Microphone ────────────────────────────────────────────
            PermissionCard(
                glyph = CurioIcons.Mic,
                title = "Microphone",
                subtitle = "Voice notes (Sound Bite) & voice attachments in your journal",
                granted = micGranted,
                onRequest = onRequestMic
            )

            Spacer(Modifier.height(10.dp))

            // ── Display over other apps (floating explore bubble) ─────
            PermissionCard(
                glyph = CurioIcons.BubbleChart,
                title = "Display over other apps",
                subtitle = "Floating explore bubble while you research a topic",
                granted = overlayGranted,
                onRequest = onRequestOverlay
            )
        }
    }
}

/** A borderless settings-box permission card — coral icon chip + label +
 *  Allow/Granted, the torn-family language of the Settings hub. */
@Composable
private fun PermissionCard(
    glyph: String,
    title: String,
    subtitle: String,
    granted: Boolean,
    onRequest: () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    val accent = MaterialTheme.colorScheme.primary
    CurioSettingsCard(border = null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Coral icon chip (the CurioCardHeader construction) ─────
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CurioColors.CoralBlush.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = glyph,
                    contentDescription = null,
                    tint = CurioColors.CoralBlush,
                    size = 20.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (granted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CurioIcon(
                        name = CurioIcons.Check,
                        contentDescription = null,
                        tint = accent,
                        size = 16.dp
                    )
                    Text(
                        "Granted",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
            } else {
                Button(
                    onClick = onRequest,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "Allow",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        extraContent()
    }
}

@Composable
private fun ReminderRow(
    reminderWanted: Boolean,
    onReminderChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CurioIcon(
            name = CurioIcons.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 18.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Daily shuffle reminder",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "A gentle nudge to discover something new",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(checked = reminderWanted, onCheckedChange = onReminderChange)
    }
}

/** POST_NOTIFICATIONS is a no-op below API 33 — treated as granted. */
private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

@Composable
private fun PageDot(selected: Boolean, onClick: () -> Unit) {
    val size = if (selected) 12.dp else 8.dp
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    }
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(size)
            .scale(if (selected) 1.2f else 1f)
            .background(color, shape = CircleShape)
            .clickable(onClick = onClick)
    )
}

private fun finishOnboarding(context: Context, navController: NavController) {
    CurioOnboardingState.markComplete(context)
    navController.navigate(CurioRoutes.HOME) {
        popUpTo(CurioRoutes.ONBOARDING) { inclusive = true }
        // launchSingleTop dedups the replay path: onboarding is pushed on
        // top of an existing HOME, so without it [HOME, ONBOARDING] → pops
        // onboarding → pushes a second HOME and back walks Home twice.
        launchSingleTop = true
    }
}

private data class OnboardingSlideData(
    val kicker: String,
    val headline: String,
    val subtext: String
)

// v7.102 — intro copy rewritten; each slide opens with a step kicker and
// renders inside the big rose hero (no illustration tiles).
private val OnboardingSlides = listOf(
    OnboardingSlideData(
        kicker = "SHUFFLE",
        headline = "Every shuffle, something new",
        subtext = "Spin the deck and Curio deals you a film, an album, a book, or a discovery you didn't know you wanted."
    ),
    OnboardingSlideData(
        kicker = "EXPLORE",
        headline = "Explore it your way",
        subtext = "Listen, read, watch, or scroll. Your time is timed, never rushed. Wander wherever curiosity leads."
    ),
    OnboardingSlideData(
        kicker = "KEEP",
        headline = "Keep what moves you",
        subtext = "Voice notes, reviews, moodboards, journal pages: save what stays with you, in the format that fits how you think."
    )
)

/** The theme step — a simple Light / Dark / System picker and one pastel
 *  toggle, nothing else (v7.100). Applies instantly via the reactive
 *  [AppPreferences] theme state, so picking Dark flips the whole app while
 *  you look. */
@Composable
private fun ThemeSlide() {
    val context = LocalContext.current
    val mode = AppPreferences.themeModeState
    val pastel = AppPreferences.pastelColorsState
    val fill = settingsRoseAccent()
    val ink = settingsReadableInk(fill)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val compact = maxHeight < 380.dp
        Column(
            // Scrollable like the setup step — short screens must never
            // clip the pastel card (the Box centers the scrollable column
            // as a whole).
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Step kicker ───────────────────────────────────────────
            Text(
                text = "THEME",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = ink.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))

            Text(
                text = "Pick your look",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = ink,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))

            Text(
                text = "Light, dark, or follow your phone, and keep Curio's soft pastel colors?",
                style = MaterialTheme.typography.bodyLarge,
                color = ink.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(if (compact) 16.dp else 22.dp))

            // ── Mode chips — Light / Dark / System (and nothing else) ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeModeChip(
                    label = "Light",
                    glyph = CurioIcons.LightMode,
                    selected = mode == AppPreferences.THEME_LIGHT,
                    fill = fill,
                    ink = ink,
                    onClick = { AppPreferences.setThemeMode(context, AppPreferences.THEME_LIGHT) }
                )
                ThemeModeChip(
                    label = "Dark",
                    glyph = CurioIcons.DarkMode,
                    selected = mode == AppPreferences.THEME_DARK,
                    fill = fill,
                    ink = ink,
                    onClick = { AppPreferences.setThemeMode(context, AppPreferences.THEME_DARK) }
                )
                ThemeModeChip(
                    label = "System",
                    glyph = CurioIcons.Contrast,
                    selected = mode == AppPreferences.THEME_SYSTEM,
                    fill = fill,
                    ink = ink,
                    onClick = { AppPreferences.setThemeMode(context, AppPreferences.THEME_SYSTEM) }
                )
            }

            Spacer(Modifier.height(if (compact) 12.dp else 16.dp))

            // ── Pastel toggle — borderless box, the setup-card language ──
            CurioSettingsCard(border = null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CurioIcon(
                        name = CurioIcons.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Pastel colors",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Soft pastel accents instead of deep tones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = pastel,
                        onCheckedChange = { AppPreferences.setPastelColorsEnabled(context, it) }
                    )
                }
            }
        }
    }
}

/** One mode chip in the theme picker — sits ON the rose banner: selected
 *  fills with the banner's ink (text flips to the rose fill), unselected is
 *  a translucent ink glass pill with a hairline ink rim. */
@Composable
private fun ThemeModeChip(
    label: String,
    glyph: String,
    selected: Boolean,
    fill: Color,
    ink: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) ink else ink.copy(alpha = 0.14f),
        border = if (selected) null else BorderStroke(1.dp, ink.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                tint = if (selected) fill else ink.copy(alpha = 0.9f),
                size = 16.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) fill else ink
            )
        }
    }
}

object CurioOnboardingState {
    private const val PREFS = "curio_onboarding"
    private const val KEY_COMPLETE = "complete"

    fun isComplete(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETE, false)

    fun markComplete(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETE, true)
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETE, false)
            .apply()
    }
}
