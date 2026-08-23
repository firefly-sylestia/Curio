package com.curio.app.features.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.TopicJsonLoader
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.components.shareComposableCard
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * Promo mode — the hidden, share-ready promo page. v24: reached from the
 * Experiments screen (Settings → Experiments → Promo mode, or the Version
 * row's five-tap in Support & diagnostics). Promo mode itself is OFF by
 * default; this page's own toggle is the one control for it.
 *
 * While promo mode is ON the whole app shows promotional SAMPLE content
 * (Home hero stats + recents, Profile level, Quests level, Cabinet grid —
 * all derived from real topics, all tappable), so the user can screenshot
 * and share store-ready images. This page shows the current ON/OFF state
 * with a toggle, a live preview of the promo poster (WYSIWYG — exactly
 * what the share sheet sends), and one Share action that renders the
 * poster off-screen at 360×560 dp via [shareComposableCard].
 *
 * The [PromoShareCard] poster is fully self-contained (explicit colors, no
 * app-theme dependency) so the off-screen export renders identically on any
 * device.
 */
@Composable
fun PromoModeScreen(navController: NavController) {
    val context = LocalContext.current
    // Reactive — flips the whole page the instant the toggle changes.
    val promoOn = AppPreferences.promoModeState
    // Count the JSON records without parsing and retaining every catalog.
    // Promo artwork needs the number, not the full topic object graph.
    var topicTotal by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        topicTotal = TopicJsonLoader.countCanonicalTopics()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the lane wash.
            // v31 — the Promo page wears the soft page tint (a small
            // rose-lean of the background shade; the spin-lane wash when
            // Adaptive Hero is on) instead of the plain cream background.
            .background(heroPageBackground(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
    ) {
        // ── Watermark backdrop — muted category glyphs (settings family).
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        // ── Scroll content — fills the screen, runs under the ragged tear.
        ScreenEntrance {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // v255 — SCROLLING HERO: the banner is the list's first item
                // and scrolls away with the page (the Home/Profile way).
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = 10.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SettingsHeroHeader(
                        title = "Promo mode",
                        subtitle = if (promoOn) "Demo content on · share-ready" else "Store-ready promo art",
                        onBack = { navController.popBackStack() }
                    )
                }
                item { CurioSectionLabel("Demo content") }
                item { PromoStatusCard(on = promoOn, onToggle = { AppPreferences.setPromoModeEnabled(context, !promoOn) }) }
                item { CurioSectionLabel("Promo card") }
                item {
                    // Live preview — the exact poster the share sheet sends.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 14f)
                            .shadow(10.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        PromoShareCard(topicsTotal = topicTotal)
                    }
                }
                item {
                    PromoShareButton("Share promo card") {
                        shareComposableCard(
                            context = context,
                            cardSize = DpSize(360.dp, 560.dp),
                            authority = "${context.packageName}.fileprovider",
                            card = { PromoShareCard(topicsTotal = topicTotal) },
                            exportDensity = 4f
                        )
                    }
                }
                item { CurioSectionLabel("Feature graphic · 16:9") }
                item {
                    // 16:9 wide banner — wordmark + three phone mockups of
                    // the app (Home / Spin / Cabinet) + the stat strip.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .shadow(10.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        PromoFeatureGraphic(topicsTotal = topicTotal)
                    }
                }
                item {
                    PromoShareButton("Share feature graphic") {
                        shareComposableCard(
                            context = context,
                            cardSize = DpSize(512.dp, 288.dp),
                            authority = "${context.packageName}.fileprovider",
                            card = { PromoFeatureGraphic(topicsTotal = topicTotal) },
                            exportDensity = 4f
                        )
                    }
                }
                item { CurioSectionLabel("App screenshot") }
                item {
                    // 9:16 portrait — a phone-sized Home screen mockup with
                    // the brand caption bar beneath it.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f)
                            .shadow(10.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        PromoAppScreenshot()
                    }
                }
                item {
                    PromoShareButton("Share screenshot") {
                        shareComposableCard(
                            context = context,
                            cardSize = DpSize(360.dp, 640.dp),
                            authority = "${context.packageName}.fileprovider",
                            card = { PromoAppScreenshot() },
                            exportDensity = 4f
                        )
                    }
                }
                item {
                    Text(
                        text = if (promoOn) {
                            "All screens now show demo content: Home, Profile, Quests & Cabinet. Screenshot away; everything stays tappable."
                        } else {
                            "Home, Profile, Quests & Cabinet show your real data while promo mode is off."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(
                        "Reopen anytime from the Experiments screen (Settings, or the Version row's five-tap in Support & diagnostics). Turn demo content off right here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** The ON/OFF status card with the big toggle — the one control for the
 *  hidden promo mode (reached via the Experiments screen). */
@Composable
private fun PromoStatusCard(on: Boolean, onToggle: () -> Unit) {
    val accent = if (on) curioSageInk() else curioRoseInk()
    val icon = if (on) CurioIcons.Check else CurioIcons.Close
    Surface(
        shape = RoundedCornerShape(20.dp),
        // v27n — opaque tinted fill (was 12% alpha, which let the elevation
        // shadow bleed through).
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.12f),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(icon, null, tint = accent, size = 18.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (on) "Demo content is ON" else "Demo content is OFF",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (on) {
                            "Home, Profile, Quests & Cabinet show promotional sample data for screenshots."
                        } else {
                            "Every screen shows your real data."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = onToggle,
                shape = RoundedCornerShape(50),
                color = accent,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (on) "Turn demo content off" else "Turn demo content on",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// The promo poster — the torn-rose family's share card.
// ═══════════════════════════════════════════════════════════════════════

/** Fixed promo-poster tear seed — the seam never re-rolls across preview
 *  and export. */
private const val PROMO_TEAR_SEED = 0x50AC0

/** The poster's rose banner gradient — soft pink melting into a deeper
 *  rose at the tear. */
/** Deep rose-plum banner top: richer and less washed-out in promo previews. */
private val PromoRoseTop = Color(0xFFE7A0B5)
/** Berry-rose banner depth: keeps the promo identity warm without neon pink. */
private val PromoRoseDeep = Color(0xFF9E4668)
/** Creamy rose ink with strong contrast against the richer banner. */
private val PromoBannerInk = Color(0xFF3B1728)
/** Warm blush paper: softer than white while staying bright enough for export. */
private val PromoPaper = Color(0xFFFFF7F2)
/** Deep plum body ink for the promise rows. */
private val PromoBodyInk = Color(0xFF321622)
private val PromoBodyMuted = Color(0xFF795564)
/** Warm gold for the 5-star social-proof row. */
private val PromoGold = Color(0xFFE3A33B)

/**
 * The self-contained promo poster: a rose banner (editors'-choice chip,
 * wordmark + tagline + category chips) torn onto a paper body (the three
 * promises + the die + 5-star social proof + real topic-count stats), all
 * drawn with explicit colors so the off-screen export and the on-screen
 * preview match. v7.107 — taller 9:14 canvas so the extra stat row fits
 * without crowding.
 */
@Composable
fun PromoShareCard(topicsTotal: Int) {
    val bannerTorn = remember(PROMO_TEAR_SEED) { SoftTornBottomShape(PROMO_TEAR_SEED, bold = true) }
    val sheetShape = remember(PROMO_TEAR_SEED) {
        SoftTornSheetShape(PROMO_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PromoPaper)
    ) {
        // ── Banner (top 60%) — rose gradient with a torn bottom seam ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.60f)
        ) {
            // Cream sheet peeking up through the tear bites.
            // v108 — OFF by default (the same torn-hero under-sheet
            // toggle in Settings → Experiments → Paper & headers).
            if (AppPreferences.heroTearSheetState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp)
                    .clip(sheetShape)
                    .background(PromoPaper)
            )
            }
            // The rose banner, clipped to the seeded torn bottom edge.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(bannerTorn)
                    .background(Brush.verticalGradient(listOf(PromoRoseTop, PromoRoseDeep)))
            ) {
                // Watermark glyphs — white at a soft alpha (banner family).
                val glyphs = listOf(
                    Triple(CurioIcons.AutoAwesome, BiasAlignment(-0.92f, -0.92f), 30f),
                    Triple(CurioIcons.Casino, BiasAlignment(0.92f, -0.82f), 26f),
                    Triple(CurioIcons.Star, BiasAlignment(-0.84f, 0.10f), 22f),
                    Triple(CurioIcons.AutoAwesome, BiasAlignment(0.90f, 0.06f), 24f)
                )
                glyphs.forEach { (glyph, bias, glyphSize) ->
                    CurioIcon(
                        name = glyph,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.20f),
                        size = glyphSize.dp,
                        modifier = Modifier
                            .align(bias)
                            .padding(12.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.weight(0.08f))
                    // Editors'-choice chip — a small glass pill above the
                    // wordmark so the card reads as a curated pick.
                    Row(
                        modifier = Modifier
                            // v27n — shadow FIRST + OPAQUE glass fill (lerp of
                            // the white into the rose banner at the old glass
                            // alpha): the old order painted the shadow on top
                            // of a translucent white pill.
                            .shadow(2.dp, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .background(lerp(PromoRoseDeep, Color.White, 0.22f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CurioIcon(CurioIcons.Star, null, tint = PromoBannerInk, size = 11.dp)
                        Text(
                            "EDITORS' CHOICE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.6.sp
                            ),
                            color = PromoBannerInk
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Wordmark
                    Text(
                        text = "C U R I O",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 6.sp
                        ),
                        color = PromoBannerInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Discover something new,\nexplore it your way.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 21.sp
                        ),
                        color = PromoBannerInk.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.weight(0.12f))
                    // Category chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PromoChip(CurioIcons.Movies, "Films", PromoBannerInk)
                        PromoChip(CurioIcons.Music, "Albums", PromoBannerInk)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PromoChip(CurioIcons.Books, "Books", PromoBannerInk)
                        PromoChip(CurioIcons.Science, "Discoveries", PromoBannerInk)
                    }
                    Spacer(Modifier.weight(0.10f))
                }
            }
        }
        // ── Paper body (bottom 40%) — promises, the die, social proof ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PromoPromise(CurioIcons.Star, "Shuffle the deck", "Films, albums, books & discoveries")
            Spacer(Modifier.height(8.dp))
            PromoPromise(CurioIcons.MoodInspired, "Explore it your way", "Your pace, your notes, your words")
            Spacer(Modifier.height(8.dp))
            PromoPromise(CurioIcons.Bookmark, "Keep what moves you", "Quotes & entries in your Cabinet")
            Spacer(Modifier.height(12.dp))
            // The die — the wildcard shuffle mark.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PromoRoseDeep),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(CurioIcons.Casino, null, tint = Color.White, size = 22.dp)
            }
            Spacer(Modifier.height(6.dp))
            // Social proof — five gold stars + a whisper line.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) {
                    CurioIcon(CurioIcons.Star, null, tint = PromoGold, size = 13.dp)
                }
            }
            Spacer(Modifier.height(4.dp))
            // Honest stats — real topic count + lanes + ads-free.
            Text(
                text = "${topicsTotal.coerceAtLeast(0)}+ topics · ${CurioCategories.all.size} lanes · 0 ads",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = PromoBodyMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A white-glass category chip on the rose banner — glyph + label. */
@Composable
private fun PromoChip(glyph: String, label: String, ink: Color) {
    Row(
        modifier = Modifier
            // v27n — shadow FIRST + OPAQUE glass fill (see the editors'-choice
            // chip above for the same fix).
            .shadow(2.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(lerp(PromoRoseDeep, Color.White, 0.20f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        CurioIcon(glyph, null, tint = ink, size = 14.dp)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = ink
        )
    }
}

/** One of the poster's three promise rows — tinted glyph chip + title +
 *  subtitle. */
@Composable
private fun PromoPromise(glyph: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(CurioColors.HomeRosewood.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(glyph, null, tint = CurioColors.HomeRosewood, size = 15.dp)
        }
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PromoBodyInk
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = PromoBodyMuted,
                maxLines = 1
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// The promo gallery's extra share art (v7.109) — the feature graphic and
// the app screenshot. All self-contained (explicit colors), so the
// off-screen exports match the on-screen previews exactly.
// ═══════════════════════════════════════════════════════════════════════

/** Shared rose share button — one shape for every promo share action. */
@Composable
private fun PromoShareButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = PromoRoseDeep,
        contentColor = PromoPaper,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CurioIcon(CurioIcons.Share, null, tint = PromoPaper, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

/**
 * The 16:9 feature graphic — a wide rose banner with the wordmark +
 * tagline and category chips on the left, three mini phone mockups of the
 * app (Home / Spin / Cabinet) on the right, and the honest topic-count
 * stat strip along the bottom. The classic Google-Play-feature-graphic
 * shape, built entirely in Compose.
 */
@Composable
fun PromoFeatureGraphic(topicsTotal: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PromoRoseTop, PromoRoseDeep)))
    ) {
        // Watermark glyphs at the corners (white, soft).
        CurioIcon(
            CurioIcons.AutoAwesome, null,
            tint = Color.White.copy(alpha = 0.22f),
            size = 38.dp,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
        )
        CurioIcon(
            CurioIcons.Casino, null,
            tint = Color.White.copy(alpha = 0.18f),
            size = 32.dp,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "C U R I O",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp
                    ),
                    color = PromoBannerInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Discover something new,\nexplore it your way.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 15.sp
                    ),
                    color = PromoBannerInk.copy(alpha = 0.92f)
                )
                Spacer(Modifier.height(10.dp))
                // Two compact chip rows (the poster's arrangement, scaled).
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PromoChip(CurioIcons.Movies, "Films", PromoBannerInk)
                    PromoChip(CurioIcons.Music, "Albums", PromoBannerInk)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PromoChip(CurioIcons.Books, "Books", PromoBannerInk)
                    PromoChip(CurioIcons.Science, "Discoveries", PromoBannerInk)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${topicsTotal.coerceAtLeast(0)}+ topics · ${CurioCategories.all.size} lanes · 0 ads",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = PromoBannerInk.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.width(8.dp))
            // Three overlapping phone mockups — Home / Spin / Cabinet.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(-9.dp)
            ) {
                PhoneMockup(
                    accent = PromoRoseDeep,
                    heroLight = PromoRoseTop,
                    ink = PromoBannerInk,
                    rotation = -7f
                )
                PhoneMockup(
                    accent = Color(0xFF7FA8CE),
                    heroLight = Color(0xFFDCE8F5),
                    ink = Color(0xFF24364A),
                    rotation = 0f
                )
                PhoneMockup(
                    accent = Color(0xFF93AC82),
                    heroLight = Color(0xFFE4EBDD),
                    ink = Color(0xFF2A3A24),
                    rotation = 7f
                )
            }
        }
    }
}

/**
 * The 9:16 app screenshot — a phone-sized mockup of the Home screen
 * (torn-rose hero, streak pills, shuffle CTA, recents) over a brand
 * caption bar with the five gold stars. Reads as a real store screenshot.
 */
@Composable
fun PromoAppScreenshot() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PromoPaper)
    ) {
        // ── The phone, filling the top ~70% ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.70f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.60f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF1A1B20))
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(PromoPaper)
                ) {
                    // Status strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .background(Brush.verticalGradient(listOf(PromoRoseTop, PromoRoseDeep)))
                    ) {
                        Text(
                            "9:41",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PromoBannerInk,
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(5.dp)
                                .align(Alignment.TopCenter)
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                    // Torn-rose hero with the greeting + streak pills
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                            .background(Brush.verticalGradient(listOf(PromoRoseTop, PromoRoseDeep)))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 12.dp, top = 10.dp)
                        ) {
                            Text(
                                "Curio",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                ),
                                color = PromoBannerInk
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Good morning",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = PromoBannerInk
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ScreenshotStatPill("27", "Streak", PromoBannerInk)
                                ScreenshotStatPill("128", "Cabinet", PromoBannerInk)
                                ScreenshotStatPill("6", "Recent", PromoBannerInk)
                            }
                        }
                    }
                    // Shuffle CTA pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PromoRoseDeep.copy(alpha = 0.9f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurioIcon(CurioIcons.Casino, null, tint = Color.White, size = 12.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Shuffle the deck",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    // Recents header + three cards
                    Text(
                        "Recents",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = PromoBodyInk,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp)
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ScreenshotRow("The Grand Budapest Hotel (2014)", "FILMS", PromoRoseDeep)
                        ScreenshotRow("OK Computer (1997)", "ALBUMS", Color(0xFF7FA8CE))
                        ScreenshotRow("Beloved (1987)", "BOOKS", Color(0xFF93AC82))
                    }
                    // Bottom nav strip — fills the screen's lower edge so the
                    // mockup reads like a real Home screen, not a top-heavy
                    // card (v7.109 review fix).
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .background(Color(0xFFF7F4F1)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) { i ->
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == 0) PromoRoseDeep else Color(0xFFD9D2CE)
                                    )
                            )
                        }
                    }
                }
            }
        }
        // ── Caption bar — brand + stars ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.30f)
                .background(Brush.verticalGradient(listOf(PromoRoseTop, PromoRoseDeep))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) {
                        CurioIcon(CurioIcons.Star, null, tint = PromoGold, size = 13.dp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Curio. Discover something new,\nexplore it your way.",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    ),
                    color = PromoBannerInk,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "FREE · ON-DEVICE · NO ADS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp
                    ),
                    color = PromoBannerInk.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/** A tiny glass stat pill inside the screenshot's phone hero. */
@Composable
private fun ScreenshotStatPill(value: String, label: String, ink: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.20f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = ink
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = ink.copy(alpha = 0.85f)
        )
    }
}

/** A recents row inside the screenshot's phone — glyph dot + title + lane. */
@Composable
private fun ScreenshotRow(title: String, lane: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFF1E6E5)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 7.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFBDAAB0))
            )
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD6C5CA))
            )
        }
        Text(
            lane,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
            color = accent,
            modifier = Modifier.padding(end = 7.dp)
        )
    }
}

/**
 * A mini phone mockup — dark bezel around a cream screen with an accent
 * hero, a greeting bar and three tinted content cards. Used in the 16:9
 * feature graphic to show "the app" (Home / Spin / Cabinet variants).
 */
@Composable
private fun PhoneMockup(
    accent: Color,
    heroLight: Color,
    ink: Color,
    rotation: Float = 0f
) {
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(98.dp)
            .graphicsLayer { rotationZ = rotation }
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF1A1B20))
            .padding(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(PromoPaper)
        ) {
            // Status strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(accent)
            )
            // Hero with the greeting bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Brush.verticalGradient(listOf(heroLight, accent))),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ink.copy(alpha = 0.85f))
                )
            }
            // Three tinted content cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(3) { i ->
                    val dot = when (i) {
                        0 -> accent
                        1 -> Color(0xFF8FA97C)
                        else -> Color(0xFFE8A33D)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1E6E5)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dot)
                        )
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))                                    .background(Color(0xFFBDAAB0))

                        )
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))                                    .background(Color(0xFFD6C5CA))

                        )
                    }
                }
            }
        }
    }
}
