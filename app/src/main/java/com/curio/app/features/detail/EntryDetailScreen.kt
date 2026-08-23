package com.curio.app.features.detail

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.AdaptiveImageGallery
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioDropdownItem
import com.curio.app.ui.components.CurioDropdownMenu
import com.curio.app.ui.components.CurioProgressPill
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.CurioTwoStepDeleteDialog
import com.curio.app.ui.components.NotePaperCard
import com.curio.app.ui.components.WaveformExtractor
import com.curio.app.ui.components.buildRichAnnotated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.curio.app.data.CaptureData
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CurioCategories
import com.curio.app.data.JournalMood
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.data.TextSpan
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.FieldMindMetadata
import com.curio.app.data.formatElapsed
import com.curio.app.data.formatSessionShort
import com.curio.app.data.TopicCatalog
import com.curio.app.data.shortName
import com.curio.app.features.capture.formats.FilledStar
import com.curio.app.navigation.CurioRoutes
import coil.compose.rememberAsyncImagePainter
import java.io.File
import com.curio.app.ui.components.CurioMoodBoardBackdrop
import com.curio.app.ui.components.MoodBoardExport
import com.curio.app.ui.components.MoodBoardFloatingCards
import com.curio.app.ui.components.MoodBoardTiles
import com.curio.app.ui.components.MoodBoardZoomOverlay
import com.curio.app.ui.components.QuoteLimits
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.formatGlyph
import com.curio.app.ui.components.limitQuoteContent
import com.curio.app.ui.components.rememberMoodBoardZoomState
import com.curio.app.ui.components.shareComposableCard
import com.curio.app.ui.components.PaperTitleLines
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.components.isInScreenGlassActive
import com.curio.app.ui.components.liquidGlassCapsule
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.curio.app.ui.components.CurioDialogEntrance
import com.curio.app.ui.components.TornStatPaperShape
import com.curio.app.ui.components.paperStatCardColor
import com.curio.app.ui.components.paperStatCardFill
import com.curio.app.data.AppPreferences
import com.curio.app.data.OfflineTranscriber
import com.curio.app.data.VoskModels
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.headerAccent
import com.curio.app.ui.theme.readableAccentInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.curioPillTintLift
import com.curio.app.ui.theme.categorySurfaceMoodBoard
import com.curio.app.ui.theme.heroHeaderInk
import com.curio.app.ui.theme.lightAccentTint
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.glyph
import com.curio.app.ui.theme.notePaperHighlight
import com.curio.app.ui.theme.notePaperInk
import com.curio.app.ui.theme.notePaperSurface
import com.curio.app.ui.theme.PatrickHandFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Entry Detail — see Curio detail contract. Framed presentation of a saved capture.
 *
 * Upgraded with:
 *  - Room database persistence (loads from CaptureRepository)
 *  - Structured CaptureData rendering per format
 *  - MorphEntrance for hero image; topic meta + format body render at once
 *  - Delete functionality with Room
 */

/**
 * v7.31 — the saved-entry reading bump: note text renders at 18sp on 28dp
 * ruled lines, so captured writing is easier to read on the detail page.
 * Every saved-view paper card passes [SavedNoteRuleSpacing] so the drawn
 * rules align with the actual text line height (the paper's default cadence
 * is the bodyLarge line height, which no longer matches once the text
 * grows).
 */
@Composable
private fun savedNoteStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontSize = 18.sp,
        lineHeight = 28.sp,
        fontFamily = PatrickHandFontFamily
    )

/** The ruled-line cadence matching [savedNoteStyle]'s line height. */
private val SavedNoteRuleSpacing = 28.dp

/** Extra bottom space for saved-view notes — ~2 blank ruled lines (v7.33),
 *  so a note reads like a page with room to keep writing instead of ending
 *  flush at the last line. */
private val SavedNoteTailSpace = SavedNoteRuleSpacing * 2f

/**
 * v7.37 — stable per-entry torn-paper seed: entry-derived base XORed with a
 * per-card salt, so the detail page's paper tears are unique per entry AND
 * distinct within it, and never re-roll between opens (String.hashCode is
 * deterministic — the same entry always tears identically).
 */
private fun noteSeed(entryId: String, salt: Int): Int =
    (entryId.hashCode() xor (salt * 0x1F31)) and 0x7fffffff

@Composable
fun EntryDetailScreen(
    entryId: String,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authority = remember { "${context.packageName}.fileprovider" }
    // Observe the repository flow so edits (mood-board re-save) and deletes
    // reflect instantly when this screen regains focus.
    val entry by produceState<CurioEntry?>(initialValue = null, entryId) {
        runCatching {
            CurioRepositoryHolder.repo.observeAll().collect { entries ->
                value = entries.find { it.id == entryId }
                    ?: TopicCatalog.sampleEntries().find { it.id == entryId }
            }
        }
    }

    LaunchedEffect(entry) {
        if (entry == null) {
            kotlinx.coroutines.delay(400)
            if (entry == null) navController.popBackStack()
        }
    }

    val resolvedEntry = entry ?: return
    val cat = CurioCategories.byId(resolvedEntry.topic.categoryId)
    val isQuotesCategory = resolvedEntry.topic.categoryId ==
        com.curio.app.data.CategoryId.QUOTES
    // The page's category tint wash — the saved-entry page wears the entry's
    // wash over the theme background (same as Spin / Save / Cabinet), so a
    // capture from the Cabinet reads in its category's color story instead of
    // a plain patch. Hoisted once and shared with the hero's gradient so the
    // hero's final stop is, by construction, exactly the page color behind it.
    val wash = cat.categoryBackgroundWash()
    // v7.5 — pastel mode lightens the hero gradient, so the hero content
    // (glyph, title, frosted bar, watermark scatter) flips from white to the
    // theme-aware onAccent ink — deep accent in light, light twin in dark.
    // White when pastel mode is off, preserving the exact pre-pastel look.
    // v28 — dark mode hero title text is always white/creamish (never the
    // tinted light twin) so the banner headline stays crisp light-on-deep.
    val heroInk = cat.heroHeaderInk()
    // v81 — dark: the hero cards flip to light ink on a near-black sheet
    // (the exact light-mode reversal).
    val heroCardInk = if (isCurioDarkTheme()) Color(0xFFEDE7DC) else Color(0xFF232A35)
    val heroSheetColor = if (isCurioDarkTheme()) Color(0xFF121316) else Color(0xFFFDFCF9)
    val heroStart = CurioGradients.categoryCardFill(cat.headerAccent())
    // v75 — heroFrostBrush is gone: the Date · Mood · Session · Type card
    // is an OPAQUE theme-aware pane now (a heroSheetColor + heroStart blend,
    // see the meta card below), so the old translucent frost has no consumer.
    // v5.8 — saveable so rotation doesn't close the menu/dialog unexpectedly.
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var heroControlsVisible by remember(resolvedEntry.id) { mutableStateOf(false) }
    LaunchedEffect(resolvedEntry.id) {
        heroControlsVisible = true
    }
    val heroControlsProgress by animateFloatAsState(
        targetValue = if (heroControlsVisible) 1f else 0f,
        animationSpec = tween(320, delayMillis = 70, easing = FastOutSlowInEasing),
        label = "heroControlsEntrance"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wash)
    ) {
        // Muted category-glyph watermark behind the content — the same
        // backdrop language as Home / Spin / the mood boards, so a saved
        // entry reads as part of the app's paper-and-glyph world.
        // Every glyph stays BELOW the hero banner: the hero's gradient
        // blend used to chop the top glyphs at its bottom edge (the "cut"
        // look). [EntryDetailHeroClearance] clears the hero with a small
        // gap; the hero card's own symbol scatter is untouched.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = cat,
                topClearance = EntryDetailHeroClearance,
                // v7.76 — quieter still: the glyphs now sit at a faint whisper so
                // the text below the hero always reads first.
                alphaScale = 0.45f,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Hoisted scroll state — the sticky top bar (back + more controls)
        // reads it to pop out of the hero into frosted floating pills, the
        // same scroll-linked clock Home uses for its menu / profile pills.
        val detailScroll = rememberScrollState()
        // v241 — LOCAL GLASS CAPTURE: the scrolling page content records
        // into its own layer; the sticky back/more pills are a SIBLING of
        // this Column (outside the captured node), so they can never sample
        // themselves — the bottom-nav architecture, no self-capture cycle.
        val detailGlassBackdrop = rememberLayerBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(detailGlassBackdrop)
                .verticalScroll(detailScroll)
        ) {
        // ── Expressive hero banner — one composed card: the category glyph
        // watermark with the topic title UNDER it, both on a SOLID category
        // color (no gradient — the depth comes from the torn-paper seam at
        // the bottom edge), plus a frosted (blurred-glass) date + mood + type
        // grid card below the title.
        //
        // The banner runs edge-to-edge (square corners — no rounded card
        // look). Its bottom edge is clipped by a seeded SOFT torn shape with
        // ONE SOLID white under-sheet behind it: the white sheet extends
        // visibly below the seam as a very thin, uneven paper lip. Its broad
        // wave rhythm follows the hero while its fine tooth stays independent;
        // the page's wash starts only after the white sheet, so no background
        // gaps show through the teeth. The tear is seeded from the entry id,
        // so every detail page gets its own stable texture that never changes
        // when reopened.
        // v27j — header fill depth: the hero wears a slightly darker painter
        // accent by default (toggle in Experiments → Paper & headers).
        val heroAccent = cat.headerAccent()
        // v7.28 — the hero is a SOLID category color, no gradient. The depth
        // comes from the torn-paper seam: the solid banner is clipped by a
        // seeded soft tear, ONE white sheet sits just behind it, and the
        // page's wash starts right after the sheet's lip.
        // Keep the entry seed deterministic. The Detail-only tear personality
        // salts this seed inside the shared shape implementation, avoiding the
        // small set of phases that can make a seam look visually flat.
        val tearSeed = remember(entryId) { entryId.hashCode() and 0x7fffffff }
        // v7.29 — the torn SEAM cants, not the card. The per-entry slant
        // lives INSIDE the seeded tear path itself (SoftTearParams.tilt —
        // seeded from the same tearSeed, shared by the hero and its white
        // under-sheet so the two edges stay pixel-aligned), so every detail
        // page wears its own stable hand-torn angle (reopens identically,
        // never re-rolls) while the card rectangle — the title, the frosted
        // Date · Mood · Type card and the back / more controls — stays
        // perfectly LEVEL.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EntryDetailHeroHeight + EntryDetailSheetExtent)
        ) {
            // ── White under-sheet — ONE SOLID white sheet layered BEHIND
            // the hero's torn bottom edge. The tear lives ONLY on the hero
            // card: the sheet's top edge is the SAME seeded torn curve as
            // the hero's bottom edge (same seed → pixel-perfect alignment,
            // so the sheet's torn top hides behind the opaque hero and the
            // wavy bite marks read white through the hero's up-bites), and
            // the sheet's lower edge follows the same broad waves with a
            // thin uneven lip. Its small tooth is independent, creating a
            // believable layered-paper tear without a rigid parallel line or
            // visible gaps. (tearSeed is declared above, with the tilt.)
            // Remembered Shape instances so their internal outline caches
            // survive recompositions (built fresh in the modifier chain, the
            // caches would never hit).
            // v104 — the detail tear is now Home's EXACT construction: the
            // old v92 `detail = true` pattern (salted seed + mid-frequency
            // meander octaves) made the seam read as mechanical straight
            // lines for many entries — every other hero (Home, Profile,
            // Settings, Cabinet…) uses just `bold = true`. The detail flag
            // is gone from both the hero and the under-sheet so they stay
            // pixel-aligned on the plain bold tear.
            val heroTornShape = remember(tearSeed) {
                SoftTornBottomShape(tearSeed, bold = true)
            }
            val sheetShape = remember(tearSeed) {
                SoftTornSheetShape(
                    tearSeed,
                    lip = 10.dp,
                    baseline = 14.dp,
                    bold = true
                )
            }
            // v108 — OFF by default (Settings → Experiments → Paper &
            // headers): the hero tears straight into the page; the toggle
            // restores this extra paper layer.
            if (AppPreferences.heroTearSheetState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    // v91 — Home's clean tear construction: the sheet's torn
                    // top hides behind the banner and the wide 10dp lip reads
                    // as real paper below the seam (the old 3dp lip + the
                    // near-black dark sheet made the detail tear read as a
                    // straight line at night).
                    .offset(y = EntryDetailHeroHeight - 18.dp)
                    .clip(sheetShape)
                    .background(
                        // v91 — dark: a visible paper lip (Home's recipe —
                        // the hero's hue lifted toward white) instead of the
                        // near-black sheet that vanished on the black page.
                        if (isCurioDarkTheme()) lerp(heroStart, Color.White, 0.10f)
                        else heroSheetColor
                    )
            )
            }

            // ── Torn-edge shadow — a hairline dark rim just below the
            // hero's torn seam (the SAME seeded torn shape, nudged down
            // ~1dp, identical to Home's hero) so the tear reads as a real
            // paper edge casting a thin ~0.1 mm shadow onto the white
            // sheet. Hidden behind the opaque banner everywhere except the
            // sliver under the tear; through the up-bites the rim hugs the
            // bite's bottom edge while the white still reads above it.
            // v109 — the SAME 20% black hairline as every other hero
            // (Home, Profile, Cabinet, Settings…): the old warm paper-
            // colored 72% band made the detail seam read differently from
            // the rest of the app.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EntryDetailHeroHeight)
                    .offset(y = 1.dp)
                    .clip(heroTornShape)
                    .background(Color.Black.copy(alpha = 0.20f))
            )

            // ── Hero backdrop — the SOLID category color + symbol scatter.
            // No gradient: the depth comes from the torn seam below. The
            // banner itself is NOT blurred (the frosted look belongs to the
            // date / mood / type grid card below, which carries its own
            // blurred glass pane); the glyph scatter stays sharp so it reads
            // as a deliberate patterned backdrop. The bottom edge is torn
            // with the SOFT rounded shape (small rounded textures, canted a
            // touch, NOT the sharp jagged [TornPaperShape] of the note
            // cards) — the solid hero ends in a real torn-paper seam into
            // the white sheet + page wash below instead of a gradient
            // dissolve.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EntryDetailHeroHeight)
                    .clip(heroTornShape)
                    .background(heroStart)
            ) {
                // ── Hero watermark — a scatter of the entry's category-family
                //     symbols (instruments for Music, camera kit for Movies,
                //     books for Books, art tools for Visual Art, lab symbols
                //     for Science, curiosities for Wildcard) pinned around the
                //     banner's perimeter. Only in the hero — the page backdrop
                //     keeps its own muted glyph wash.
                HeroSymbolScatter(cat = cat)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EntryDetailHeroHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        // Keep the centered content (glyph + title + frosted bar)
                        // clear of the overlaid back / more buttons at the top of
                        // the banner — without this floor, a two-line title (or
                        // the frosted bar) pushes the column up under the buttons.
                        .padding(top = 80.dp, bottom = 16.dp)
                ) {
                    CurioIcon(
                        name = cat.iconGlyph,
                        contentDescription = null,
                        tint = heroInk.copy(alpha = 0.92f),
                        size = 76.dp
                    )
                    Spacer(Modifier.height(14.dp))

                    // v7.35 — title ink follows the hero's theme-aware
                    // [heroInk] (deep accent in pastel light, light twin in
                    // pastel dark, white otherwise) instead of a hardcoded
                    // white: on the airy pastel-light hero fill a white title
                    // washed out in every pastel mode. No colored plate,
                    // gradient, rim or other title background — the hero
                    // color remains the backdrop, the title stays crisp.
                    // QUOTES category: show the writer/byline instead of
                    // the quote text (the quote is shown in the body).
                    val heroTitle = if (isQuotesCategory && resolvedEntry.topic.byline.isNotBlank()) {
                        resolvedEntry.topic.byline
                    } else {
                        resolvedEntry.topic.name
                    }
                    Text(
                        text = heroTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = heroInk,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    // v113 — the session duration no longer duplicates as a
                    // hero pill above the stat card: the Date · Mood ·
                    // Session · Type grid right below owns it (its own
                    // "Session" segment). The old pill was removed.
                    // v27 — experimental paper-title underline (two short
                    // lines under the entry title; OFF by default).
                    if (AppPreferences.paperHeaderCutsState) {
                        PaperTitleLines(
                            ink = heroInk,
                            title = resolvedEntry.topic.name,
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize
                        )
                    }
                    Spacer(Modifier.height(18.dp))

                    // ── Frosted date / mood / session / type grid card — the
                    // meta card's date, mood, session and type segments moved
                    // into the hero on a genuine frosted-glass pane: a
                    // translucent layer that samples the gradient behind the
                    // bar, BLURS it, and renders it clipped to the card, with
                    // a white frosted-glass tint and a hairline rim so the
                    // card reads as frosted glass while
                    // the crisp hero backdrop stays sharp around it. Mood
                    // shows only when the entry has one; Session shows only
                    // when an explore-session duration was recorded.
                    val heroMood = resolvedEntry.moodOf()
                    val heroTypeLabel = if (resolvedEntry.captureData is CaptureData.Portfolio)
                        "Portfolio" else resolvedEntry.format.shortName
                    val heroTypeGlyph = if (resolvedEntry.captureData is CaptureData.Portfolio)
                        CurioIcons.Inventory2 else formatGlyph(resolvedEntry.format)
                    // v27v — the paper & headers experiment extends to this
                    // meta card: with "Paper stat card" on, the Date · Mood ·
                    // Session · Type grid wears the shared opaque paper
                    // surface (same fill, 3-hole column, rings, torn edges as
                    // Home/Profile) instead of the frosted glass. Holes punch
                    // through to the hero behind; the torn shape is seeded
                    // per entry so the rip never re-rolls.
                    val metaPaperOn = AppPreferences.paperStatCardsState
                    val metaPaperBg = paperStatCardColor(heroSheetColor)
                    val metaTearOn = metaPaperOn && AppPreferences.paperStatTearState
                    val metaShape: Shape = remember(tearSeed, metaTearOn) {
                        // v95 — 18 → 20dp: matches Home/Profile's stat-card
                        // corner radius.
                        if (metaTearOn) TornStatPaperShape(tearSeed xor 0x6B4E3E) else RoundedCornerShape(20.dp)
                    }
                    val metaHolesOn = metaPaperOn && AppPreferences.paperHeaderHolesState
                    val metaRingsOn = metaHolesOn && AppPreferences.paperHoleRingsState
                    val metaRingStyle = AppPreferences.paperHoleRingStyleState
                    // v200 — the Surface wrapper is GONE: M3 Surface (1.2+)
                    // clips its children to the shape, which CUT the coil's
                    // left peek at the card edge. A plain Box +
                    // shadow(clip = false) keeps the elevation without the
                    // clip — the paper fill self-clips to the shape outline,
                    // so the protruding wire can render outside the card.
                    // v27n — frosted glass pane over the hero: the
                    // translucent frost can't hold an elevation shadow
                    // (it bleeds through), so the frosted pane stays
                    // flat; the opaque paper card can lift like Home's.
                    // v75 — the default pane is OPAQUE now (the same
                    // language as Profile's stat pane + Home's Streak
                    // card), so it always carries the elevation + dark
                    // glow like those panes.
                    Box(
                        modifier = Modifier
                            .curioDarkGlow(3.dp, metaShape)
                            .shadow(3.dp, metaShape, clip = false)
                    ) {
                        // The card's content Box: the Row below defines the
                        // height, and the paper fill / frosted pane + glass
                        // tint match its size (BoxScope — the Surface content
                        // scope is NOT BoxScope, so matchParentSize must live
                        // in an explicit Box).
                        Box(Modifier.fillMaxWidth()) {
                            if (metaPaperOn) {
                                // v27v — the shared opaque paper card (fill +
                                // hole column + rings or rims), punched with
                                // the SAME shape the Surface wears.
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .paperStatCardFill(
                                            shape = metaShape,
                                            fill = metaPaperBg,
                                            holesOn = metaHolesOn,
                                            ringsOn = metaRingsOn,
                                            ringStyle = metaRingStyle,
                                            ink = heroCardInk,
                                            // v81 — dark: light metal ring tones.
                                            dark = isCurioDarkTheme()
                                        )
                                )
                            } else {
                                // ── Opaque theme-aware pane (v75) — the
                                // SAME recipe as Profile's stat pane: the
                                // hero fill lifted toward white 6→26% (the
                                // old near-white [heroSheetColor] blend
                                // washed the Date · Mood · Type card out
                                // next to Home/Profile's stat cards — v95
                                // aligns it to the Profile recipe).
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    lerp(heroStart, Color.White, 0.06f),
                                                    lerp(heroStart, Color.White, 0.26f)
                                                )
                                            )
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                )
                            }
                            Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FrostedSegment(
                                icon = CurioIcons.CalendarToday,
                                title = formatCapturedDate(resolvedEntry.capturedAtMillis),
                                subtitle = "Date",
                                ink = heroCardInk,
                                // v7.40 — the tiny line under the hero's
                                // date: captured today → the time; otherwise
                                // a short "yesterday" / "3d ago".
                                tiny = heroDateTinyLabel(resolvedEntry),
                                modifier = Modifier.weight(1f)
                            )
                            VerticalDivider(
                                modifier = Modifier.height(30.dp),
                                color = heroCardInk.copy(alpha = 0.25f)
                            )
                            if (heroMood != null) {
                                FrostedSegment(
                                    icon = heroMood.glyph,
                                    title = heroMood.label,
                                    subtitle = "Mood",
                                    ink = heroCardInk,
                                    modifier = Modifier.weight(1f)
                                )
                                VerticalDivider(
                                    modifier = Modifier.height(30.dp),
                                    color = heroCardInk.copy(alpha = 0.25f)
                                )
                            }
                            // v22 — the explore-session duration as its own
                            // segment, right beside Mood; only when a session
                            // was actually recorded.
                            if (resolvedEntry.sessionTimeMillis > 0L) {
                                FrostedSegment(
                                    icon = CurioIcons.Timer,
                                    title = formatSessionShort(resolvedEntry.sessionTimeMillis),
                                    subtitle = "Session",
                                    ink = heroCardInk,
                                    modifier = Modifier.weight(1f)
                                )
                                VerticalDivider(
                                    modifier = Modifier.height(30.dp),
                                    color = heroCardInk.copy(alpha = 0.25f)
                                )
                            }
                            FrostedSegment(
                                icon = heroTypeGlyph,
                                title = heroTypeLabel,
                                subtitle = "Type",
                                ink = heroCardInk,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        }
                    }
                }
            }

        }

        // The category identity belongs to the reading flow. Keeping it in
        // this same scroll column prevents it from floating over the quick
        // fact and capture body on short screens.
        // v8.36 — everything below the hero enters together with a soft
        // fade so the category, description, and captured body keep their
        // measured vertical positions instead of sliding through one another
        // while the shared-element morph is still settling.
        DetailContentEntrance {
            // Keep the category, topic description, and captured entry body in
            // one measured reading column. Previously these were loose
            // siblings inside the animated container, so the body could appear
            // to climb into the description while the entrance was running.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EntryDetailCategoryLabel(
                    entry = resolvedEntry,
                    category = cat
                )

                // ── Topic meta — quick fact and tags follow the category row.
                Column(
                    modifier = Modifier
                        .padding(horizontal = detailBodyGutter())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Quick fact (v7.38) — the topic's teaser directly under
                    // the fixed category label, backgroundless on the page wash.
                    // QUOTES category: show the full quote instead of the teaser.
                    if (isQuotesCategory) {
                        QuickFactCard(
                            cat = cat,
                            teaser = resolvedEntry.topic.name,
                            label = "Quote"
                        )
                    } else {
                        QuickFactCard(
                            cat = cat,
                            teaser = resolvedEntry.topic.teaser
                        )
                    }

                    // ── Saved quick title (v125) — the user's OWN title from
                    // Save your take, shown just below the quick fact on a
                    // clean background pill (the old torn-paper slip inside
                    // the body is gone). SoundBite entries carry it.
                    val soundBiteTitle = (resolvedEntry.captureData as? CaptureData.SoundBite)
                        ?.title?.takeIf { it.isNotBlank() }
                    if (soundBiteTitle != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            // v27n — opaque accent-tinted pill.
                            color = lerp(MaterialTheme.colorScheme.surfaceContainerHigh, cat.accent, 0.10f),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = soundBiteTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = cat.categoryInk(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // ── Custom tags (v7.17) — the labels added on the save
                    // page, rendered as small #chips (the captured-at line
                    // moved onto the hero's Date segment in v7.39). Keep this
                    // as a normal measured child: a nested scale/visibility
                    // animation can paint tags through the long body below.
                    if (resolvedEntry.tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            resolvedEntry.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    // v27n — opaque tinted tag chip (was the
                                    // 20%-accent tint at 14% alpha, which let
                                    // the elevation shadow bleed through).
                                    color = if (AppPreferences.tintWashEffective()) {
                                        lerp(MaterialTheme.colorScheme.surface, cat.accent, 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = cat.categoryInk(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Session attachments (v27) — the explore session's
                // SHARED note + captured screenshots, attached at save time.
                // Shown between the tags and the format body.
                if (resolvedEntry.sessionNote != null || resolvedEntry.sessionScreenshots.isNotEmpty()) {
                    SessionNoteBlock(
                        entry = resolvedEntry,
                        category = cat,
                        navController = navController
                    )
                }

                // ── Format body ────────────────────────────────────────
                // The category row and topic description now have explicit
                // breathing room before the captured content begins.
                Box(
                    modifier = Modifier
                        .padding(horizontal = detailBodyGutter())
                        .fillMaxWidth()
                ) {
                    FormatBody(entry = resolvedEntry, category = cat, navController = navController)
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        }

        // Keep scroll-linked controls in their own recomposition scope. The
        // detail body contains paper Canvas textures, rich text, and image
        // painters; reading detailScroll.value here would invalidate that
        // whole tree on every scroll pixel. The visual behavior stays the
        // same, but only this small overlay now follows the scroll clock.
        DetailStickyBar(
            detailScroll = detailScroll,
            glassBackdrop = detailGlassBackdrop,
            heroControlsProgress = heroControlsProgress,
            heroCardInk = heroCardInk,
            heroFill = heroStart,
            resolvedEntry = resolvedEntry,
            category = cat,
            context = context,
            authority = authority,
            navController = navController,
            onDeleteRequest = { deleteDialogVisible = true }
        )

        // v66 — the progress pill (books: pages / anime: episodes) floats at
        // the SCREEN's bottom-right corner (was the hero's bottom-right) so
        // it never rides away with the hero or overlaps the frosted meta
        // card — a small compact pill with the amount done, background TINT
        // for the pill, category accent for the progress bar; tapping opens
        // the redesigned editor. Same TopicProgressStore everywhere.
        if (resolvedEntry.topic.progressTarget != null) {
            CurioProgressPill(
                topic = resolvedEntry.topic,
                accent = cat.themedAccent(),
                ink = cat.categoryInk(),
                background = lerp(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    cat.themedAccent(),
                    0.16f
                ),
                showBar = true,
                // v66 — dialog content rides the readable category ink
                // (deep accent in light, light twin in dark) so the ring /
                // steppers / slider / Save never go dark-on-dark with the
                // raw accent.
                dialogContentColor = cat.categoryInk(),
                // The NavHost Scaffold already pads its content above the
                // system nav bar (contentWindowInsets = navigationBars), so
                // no navigationBarsPadding here — 16dp from the corner.
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            )
        }
    }

    if (deleteDialogVisible) {
        // v26 — double confirmation + recycle bin: the capture moves to the
        // recycle bin (media is kept so a restore works); nothing is erased.
        CurioTwoStepDeleteDialog(
            visible = deleteDialogVisible,
            title = "this capture",
            body = "This capture moves to the Recycle bin.",
            onDismiss = { deleteDialogVisible = false },
            onConfirmed = {
                deleteDialogVisible = false
                scope.launch {
                    runCatching { CurioRepositoryHolder.repo.softDeleteById(resolvedEntry.id) }
                    navController.popBackStack()
                }
            }
        )
    }
}

/**
 * The hero banner's height — the page watermark must stay entirely below
 * it (the backdrop's [com.curio.app.ui.components.CurioWatermarkBackdrop]
 * call passes [EntryDetailHeroClearance]), so the two are defined together
 * here and a hero-height change can't silently put glyphs back behind it.
 */
/** Page-level reading gutter for the detail body — wider on tablets so the
 *  reading column breathes inside the centered max-width content column
 *  (the hero and paper cards keep their own internal paddings). */
@Composable
private fun detailBodyGutter(): Dp = if (windowWidthSizeClass().isWide) 28.dp else 20.dp

/**
 * v8.36 — soft entrance for the detail content below the hero.
 * The body is placed in a normal measured [Box] immediately; only its alpha
 * animates, keeping long descriptions and note sections in their final
 * positions instead of resizing/translating siblings mid-entrance.
 * v227c — the 200ms DELAY is gone: it was paced to the old Cabinet→Detail
 * shared MORPH, which v8.38 replaced with a center pop-up — so opening an
 * entry from the Cabinet showed a blank gap before the quick fact + body
 * even started fading in. Now the fade begins immediately (260ms).
 */
@Composable
private fun DetailContentEntrance(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "detailContentFade"
    )
    Box(
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    ) {
        content()
    }
}

// Two-line topic names plus the frosted metadata strip need a little more
// vertical breathing room on compact screens. Keeping this as the shared
// hero/morph height prevents the body header from being painted underneath
// the category and entry text while preserving one stable transition target.
private val EntryDetailHeroHeight = 400.dp
/** Extra layout space reserved for the white sheet below the clipped hero. */
// v91 — 16 → 24dp: matches Home's under-sheet geometry so the detail tear
// carries the same clean paper lip below the seam.
private val EntryDetailSheetExtent = 24.dp

/** Hero height + a small gap — the watermark's top clearance on this page
 *  (keeps the backdrop glyphs clear of the narrow white under-sheet lip
 *  below the hero's torn edge). */
private val EntryDetailHeroClearance = EntryDetailHeroHeight + 30.dp

/** Scroll distance (dp) before the back / more pills fully pin as frosted
 *  floating pills — mirrors Home's sticky menu/profile bar threshold. */
private val DetailStickyBarThreshold = 90.dp
/** The controls' resting top offset below the status bar — level with the
 *  hero's glyph band, where they were anchored inside the hero. */
private val DetailStickyBarRestTop = 72.dp
/** The controls' fully-popped top offset below the status bar — the pills
 *  ride up here as the hero scrolls away (Home pins its pills at 12dp). */
private val DetailStickyBarPoppedTop = 12.dp

/**
 * Category identity directly below the detail hero's paper lip. It remains
 * part of the reading flow so it scrolls away with the quick fact and capture
 * content instead of overlaying them.
 */
@Composable
private fun EntryDetailCategoryLabel(
    entry: CurioEntry,
    category: CurioCategory,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(category.categoryBackgroundWash())
            .padding(horizontal = detailBodyGutter(), vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CurioIcon(
            name = category.iconGlyph,
            contentDescription = null,
            tint = category.categoryInk(),
            size = 22.dp
        )
        // Keep the category and saved-entry title in one constrained text
        // column. They used to be competing weighted children in the same
        // row, so long titles could squeeze into the category label and
        // appear to overlap it on narrow screens.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = category.categoryInk(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!entry.title.isNullOrBlank()) {
                Text(
                    text = entry.title.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (entry.isLegacy) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 14.dp
                )
                Text(
                    text = "Legacy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The hero's frosted-glass language for small controls — bright frosted
 * WHITE (fully opaque, brighter at the top) with a hairline rim and
 * deep-slate content. Worn by the banner's back / more buttons so the
 * controls stay legible in every theme; the big Date · Mood · Type card
 * below wears the same rim and slate but a more translucent frost that
 * lets the banner color bloom through — it is the showpiece of the
 * family. (The title now sits on its own TRANSPARENT tint pill, not this
 * plate.)
 *
 * v7.38 — the small pills' frost is now FULLY OPAQUE: the old 0.99→0.94
 * alphas let the hero's color — most visibly a blue Material dynamic
 * primary or a blue category wash — bleed through the translucent white
 * and tint the back / menu pills blue. The frost look now comes from the
 * vertical white gradient + hairline rim alone, so the controls read as
 * solid frosted glass over ANY hero color.
 */
/**
 * [frostBrush] clipped to [shape] with a hairline rim in [ink].
 *
 * v7.38 — the shadow moved IN HERE, drawn BEFORE the clip: the old Surface
 * shadowElevation was drawn inside the plate's clip, so as the pill
 * floated up on scroll its growing shadow got sliced at the circle rim
 * and read as a dark "donut" ring around the frost. Drawing the shadow
 * first (clip = false) keeps it a clean soft drop shadow OUTSIDE the pill
 * with no inner ring — the float reads as a lift, not a glitch.
 */
private fun Modifier.heroFrostPlate(
    ink: Color,
    shape: Shape,
    elevation: Dp = 0.dp,
    frostBrush: Brush
): Modifier =
    shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(frostBrush)

/**
 * Scroll-linked controls kept separate from the detail body so paper canvases,
 * rich text, and image content do not recompose for every scroll pixel.
 */
// v250 — the glass more-menu's expanded width (matches CurioDropdownMenu's default).
private val MoreMenuWidth = 236.dp

@Composable
private fun BoxScope.DetailStickyBar(
    detailScroll: androidx.compose.foundation.ScrollState,
    // v241 — the LOCAL capture of the scrolling content (sibling of the
    // Column), threaded in so the glass pills sample only what's behind.
    glassBackdrop: LayerBackdrop?,
    heroControlsProgress: Float,
    heroCardInk: Color,
    heroFill: Color,
    resolvedEntry: CurioEntry,
    category: CurioCategory,
    context: Context,
    authority: String,
    navController: NavController,
    onDeleteRequest: () -> Unit
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    // v149 — the new share UI: the More → Share item opens a share sheet
    // with a live PREVIEW of the card before sharing (see EntryShareSheet).
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    val stickyThresholdPx = with(LocalDensity.current) { DetailStickyBarThreshold.toPx() }
    val stickyProgress by remember {
        derivedStateOf { (detailScroll.value / stickyThresholdPx).coerceIn(0f, 1f) }
    }
    val frostShift = FastOutSlowInEasing.transform(stickyProgress)
    val pillScale = androidx.compose.ui.util.lerp(0.97f, 1f, frostShift)
    // v95 — THEME-AWARE frost plate: light keeps the bright frosted glass
    // (dark slate ink reads crisp); dark flips to a DARK hero-tinted glass
    // so the light cream ink actually reads — the old hardcoded WHITE plate
    // washed out the cream glyphs and glared on the black page (the
    // reversed light-in-dark contract).
    // v108 — the dark frost is a HERO-HUED glass (the same white-lift lip
    // the under-sheet wears) instead of the near-black plate, so the back /
    // more buttons read as part of the hero instead of a black slab on it.
    // v230 — the RESTING plate is now the exact SOLID hero fill (the old
    // 10%/38% lifts started the pills a shade off the hero); the lift is
    // applied through the scroll shift instead. When the liquid-glass
    // experiment is on AND the morph has begun, the classic frosted brush
    // is replaced by a real liquid-glass capsule entirely.
    // v241 — GLASS HANDOFF RESTORED through the SAFE architecture: the
    // pills sample the LOCAL capture on the scroll Column above (they are
    // siblings of it — nothing they sample contains them). Fully CLEAR
    // refracting glass, per the request.
    val glassOn = isInScreenGlassActive()
    val detailGlassActive = glassOn && frostShift > 0.01f
    val frostFill = if (isCurioDarkTheme())
        lerp(heroFill, lerp(heroFill, Color.White, 0.10f), frostShift)
        else lerp(heroFill, lerp(heroFill, curioPillTintLift(), 0.38f), frostShift)
    val stickyFrostBrush = Brush.verticalGradient(0f to frostFill, 1f to frostFill.copy(alpha = 0.97f))
    // The ride-up must be LAYOUT-space (Modifier.offset), not a draw-time
    // graphicsLayer translation — the more-menu's popup anchors to the
    // button's layout position, so a draw-time translate would leave the
    // menu hanging below the popped pill.
    val stickyLift = (DetailStickyBarRestTop - DetailStickyBarPoppedTop) * frostShift
        // v250 — iOS-STYLE MORPH state lives at function scope: the pill
        // below fades with it AND the floating glass panel appended after
        // this Box reads the same progress.
        val morph by animateFloatAsState(
            targetValue = if (menuExpanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
            label = "moreMenuMorph"
        )
        BackHandler(enabled = menuExpanded) { menuExpanded = false }

    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = DetailStickyBarRestTop)
            .offset(y = -stickyLift)
            .graphicsLayer {
                val eased = heroControlsProgress
                alpha = eased
                scaleX = (0.97f + (0.03f * eased)) * pillScale
                scaleY = (0.97f + (0.03f * eased)) * pillScale
                translationY = -2.dp.toPx() * eased
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // v246 — one gesture stream per pill, shared by the click and the
        // liquid-glass press feel (shrink + refraction bloom).
        val backPillInteraction = remember { MutableInteractionSource() }
        CurioBackButton(
            onClick = { navController.popBackStack() },
            containerColor = Color.Transparent,
            contentColor = heroCardInk,
            shadowElevation = 0.dp,
            disableRipple = true,
            pillInteraction = backPillInteraction,
            // v230 — scrolled endpoint: real liquid-glass (refraction + blur)
            // when the experiment is on; the classic frosted plate otherwise.
            modifier = if (detailGlassActive)
                Modifier.liquidGlassCapsule(
                    heroFill,
                    washAlpha = 0.45f,
                    backdrop = glassBackdrop,
                    alwaysClear = true,
                    interactionSource = backPillInteraction
                )
                else Modifier.heroFrostPlate(
                    heroCardInk,
                    RoundedCornerShape(50),
                    elevation = 6.dp * frostShift,
                    frostBrush = stickyFrostBrush
                )
        )
        Box {
            val moreInteraction = remember { MutableInteractionSource() }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = (if (detailGlassActive)
                    Modifier.liquidGlassCapsule(
                        heroFill,
                        washAlpha = 0.45f,
                        backdrop = glassBackdrop,
                        alwaysClear = true,
                        interactionSource = moreInteraction
                    )
                else Modifier.heroFrostPlate(
                    heroCardInk,
                    RoundedCornerShape(50),
                    elevation = 6.dp * frostShift,
                    frostBrush = stickyFrostBrush
                ))
                    .graphicsLayer { if (detailGlassActive) alpha = 1f - morph }
                    .clickable(
                        interactionSource = moreInteraction,
                        indication = null
                    ) { menuExpanded = !menuExpanded }
            ) {
                CurioIcon(
                    name = CurioIcons.MoreVert,
                    contentDescription = "More",
                    tint = heroCardInk,
                    size = 24.dp,
                    // v244 — matches the back pill's 44dp growth.
                    modifier = Modifier.padding(10.dp)
                )
            }
            if (!detailGlassActive) {
            // v30 — the shared accent-themed menu: an opaque surface tinted
            // toward the entry's category accent, Share/Edit in the themed
            // ink, Delete in error red. No more hardcoded light container.
            CurioDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                accent = category.themedAccent()
            ) {
                CurioDropdownItem(
                    text = { Text("Share") },
                    leadingIcon = {
                        CurioIcon(
                            name = CurioIcons.Share,
                            contentDescription = null,
                            size = 20.dp
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        showShareSheet = true
                    }
                )
                if (isMultiSectionEntry(resolvedEntry)) {
                    CurioDropdownItem(
                        text = { Text("Edit entry") },
                        leadingIcon = {
                            CurioIcon(
                                name = CurioIcons.Edit,
                                contentDescription = null,
                                size = 20.dp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            navController.navigate(CurioRoutes.editEntry(resolvedEntry.id)) {
                                launchSingleTop = true
                            }
                        }
                    )
                } else if (isMoodBoardEntry(resolvedEntry)) {
                    CurioDropdownItem(
                        text = { Text("Edit mood board") },
                        leadingIcon = {
                            CurioIcon(
                                name = CurioIcons.Edit,
                                contentDescription = null,
                                size = 20.dp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            navController.navigate(CurioRoutes.editMoodBoard(resolvedEntry.id)) {
                                launchSingleTop = true
                            }
                        }
                    )
                } else {
                    CurioDropdownItem(
                        text = { Text("Edit entry") },
                        leadingIcon = {
                            CurioIcon(
                                name = CurioIcons.Edit,
                                contentDescription = null,
                                size = 20.dp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            navController.navigate(CurioRoutes.editEntry(resolvedEntry.id)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                CurioDropdownItem(
                    text = { Text("Delete") },
                    danger = true,
                    leadingIcon = {
                        CurioIcon(
                            name = CurioIcons.Delete,
                            contentDescription = null,
                            size = 20.dp
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDeleteRequest()
                    }
                )
            }
            }
        }

        // v250 — the GLASS MORE-PANEL: floats over the page, blooming from
        // the pill's corner (top-end transform origin) with the same spring
        // that fades the pill, so the handoff reads as one morphing surface.
        // A full-screen scrim behind it dismisses on any outside tap.
        if (detailGlassActive && morph > 0.01f) {
            // v252 — an explicit Box WRAPPER supplies the BoxScope dispatch
            // receiver: K2 refuses BoxScope members called on the extension
            // receiver alone (matchParentSize / align below).
            Box(modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(interactionSource = null, indication = null) {
                        menuExpanded = false
                    }
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp)
                    .graphicsLayer {
                        alpha = morph
                        val sc = lerp(0.55f, 1f, morph)
                        scaleX = sc
                        scaleY = sc
                        transformOrigin = TransformOrigin(1f, 0f)
                    }
                    .liquidGlassCapsule(
                        heroFill,
                        washAlpha = 0.35f,
                        backdrop = glassBackdrop,
                        // v258 — the panel is BLURRY BY DEFAULT now: the old
                        // alwaysClear flag ran the near-zero-blur recipe, so
                        // the expanded menu read as bare transparent glass.
                        // Dropping it gives the standard 8dp×scale frost.
                        shape = RoundedCornerShape(20.dp)
                    )
                    .width(MoreMenuWidth)
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                                CurioDropdownItem(
                                    text = { Text("Share") },
                                    leadingIcon = {
                                        CurioIcon(name = CurioIcons.Share, contentDescription = null, size = 20.dp)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showShareSheet = true
                                    }
                                )
                                if (isMultiSectionEntry(resolvedEntry)) {
                                    CurioDropdownItem(
                                        text = { Text("Edit entry") },
                                        leadingIcon = {
                                            CurioIcon(name = CurioIcons.Edit, contentDescription = null, size = 20.dp)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            navController.navigate(CurioRoutes.editEntry(resolvedEntry.id)) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                } else if (isMoodBoardEntry(resolvedEntry)) {
                                    CurioDropdownItem(
                                        text = { Text("Edit mood board") },
                                        leadingIcon = {
                                            CurioIcon(name = CurioIcons.Edit, contentDescription = null, size = 20.dp)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            navController.navigate(CurioRoutes.editMoodBoard(resolvedEntry.id)) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                } else {
                                    CurioDropdownItem(
                                        text = { Text("Edit entry") },
                                        leadingIcon = {
                                            CurioIcon(name = CurioIcons.Edit, contentDescription = null, size = 20.dp)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            navController.navigate(CurioRoutes.editEntry(resolvedEntry.id)) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                CurioDropdownItem(
                                    text = { Text("Delete") },
                                    danger = true,
                                    leadingIcon = {
                                        CurioIcon(name = CurioIcons.Delete, contentDescription = null, size = 20.dp)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteRequest()
                                    }
                                )
                }
            }
        }
            }

        // v149 — the share sheet (preview + Image/Text picker) opens from
        // the More menu; it lives here so it survives the sticky bar's
        // scroll-driven recompositions without re-arming.
        if (showShareSheet) {
            EntryShareSheet(
                entry = resolvedEntry,
                category = category,
                context = context,
                authority = authority,
                onDismiss = { showShareSheet = false }
            )
        }
    }
}

/**
 * Decorative watermark for the saved-entry hero — a scatter of the entry's
 * category-family symbols (instruments for Music, camera kit for Movies,
 * books for Books, art tools for Visual Art, lab symbols for Science,
 * curiosities for Wildcard) pinned around the banner's perimeter as
 * MIRRORED PAIRS — each glyph on the left is mirrored by an equal glyph on
 * the right (same size, same alpha, opposite rotation), so the scatter
 * reads as a deliberate symmetric frame around the title instead of
 * randomly placed icons. Five tiers keep the glyphs clear of each other,
 * the centered content column (icon + title + frosted bar) and the top
 * back/more buttons, drawn in the hero's onAccent ink (white when pastel
 * mode is off) at a soft alpha so they read clearly against the gradient
 * (never a transparent wash).
 */
@Composable
private fun BoxScope.HeroSymbolScatter(cat: CurioCategory) {
    val symbols = CurioIcons.heroWatermarkSymbols(cat.family)
    // v7.5 — the scatter draws in the theme-aware onAccent ink (deep in
    // light, light twin in dark) so it stays visible on the pastel-lightened
    // gradient; solid white when pastel mode is off.
    val ink = cat.onAccent()
    // Mirrored pairs: biasX magnitude + biasY (-1..1), glyph size, rotation
    // magnitude, alpha. The left glyph is drawn at (-biasX, biasY) with
    // -rotation, the right at (+biasX, biasY) with +rotation.
    val pairs = listOf(
        // Top corners — just below the status-bar band, above the buttons.
        HeroWatermarkPair(biasX = 0.93f, biasY = -0.85f, size = 44.dp, rotation = 12f, alpha = 0.16f),
        // Inner pair under the corners — clear of the centered icon.
        HeroWatermarkPair(biasX = 0.55f, biasY = -0.64f, size = 48.dp, rotation = 8f, alpha = 0.19f),
        // Mid-edge pair — the widest, at the title's height, outside its width.
        HeroWatermarkPair(biasX = 0.94f, biasY = -0.12f, size = 56.dp, rotation = 14f, alpha = 0.21f),
        // Lower inner pair — outside the frosted bar's width.
        HeroWatermarkPair(biasX = 0.56f, biasY = 0.54f, size = 50.dp, rotation = 10f, alpha = 0.19f),
        // Bottom corners — biasY 0.80 keeps them clear of the hero's torn
        // bottom edge (the soft tear's broad up-bites can reach ~20dp into
        // the banner).
        HeroWatermarkPair(biasX = 0.94f, biasY = 0.80f, size = 44.dp, rotation = 6f, alpha = 0.16f)
    )
    pairs.forEachIndexed { i, pair ->
        // The 10-symbol family list maps 1:1 onto the 5 mirrored pairs.
        HeroWatermarkGlyph(symbols[i * 2], BiasAlignment(-pair.biasX, pair.biasY), pair.size, -pair.rotation, pair.alpha, ink)
        HeroWatermarkGlyph(symbols[i * 2 + 1], BiasAlignment(pair.biasX, pair.biasY), pair.size, pair.rotation, pair.alpha, ink)
    }
}

/** One mirrored hero watermark glyph — solid white at a soft alpha. */
@Composable
private fun BoxScope.HeroWatermarkGlyph(
    glyph: String,
    alignment: Alignment,
    size: Dp,
    rotation: Float,
    alpha: Float,
    tint: Color = Color.White
) {
    CurioIcon(
        name = glyph,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        size = size,
        modifier = Modifier
            .align(alignment)
            .graphicsLayer { rotationZ = rotation }
    )
}

/** One mirrored hero watermark pair — the left glyph mirrors the right. */
private data class HeroWatermarkPair(
    val biasX: Float,
    val biasY: Float,
    val size: Dp,
    val rotation: Float,
    val alpha: Float
)

/**
 * True when an entry is a multi-section Portfolio (2+ takes) — these reopen
 * with EVERY take via the universal "Edit entry" flow, not just one board.
 */
private fun isMultiSectionEntry(entry: CurioEntry): Boolean =
    entry.captureData is CaptureData.Portfolio

/**
 * True when an entry renders as a plain mood board — a direct GalleryWall or
 * a Wildcard Open Notebook whose chosen sub-format is a GalleryWall. (A
 * multi-section Portfolio containing a GalleryWall is handled by the
 * "Edit entry" flow instead of this mood-board label.)
 */
private fun isMoodBoardEntry(entry: CurioEntry): Boolean =
    entry.format == CaptureFormat.GalleryWall ||
        (entry.captureData as? CaptureData.OpenNotebook)?.subFormat == CaptureFormat.GalleryWall

/** Wall-clock time of a capture, e.g. "3:42 PM". */
private fun formatCapturedTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

/** Calendar date of a capture, e.g. "Aug 2, 2026". */
private fun formatCapturedDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

/**
 * The entry's mood — every format carries the shared mood row, and
 * OpenNotebook wildcard takes keep theirs inside
 * [CaptureData.OpenNotebook.subData], so unwrap those before reporting.
 * Used by the hero's frosted bar.
 */
private fun CurioEntry.moodOf(): JournalMood? = when (val d = captureData) {
    is CaptureData.Marginalia -> d.mood
    is CaptureData.ReelNotes -> d.mood
    is CaptureData.SoundBite -> d.mood
    is CaptureData.FieldNotes -> d.mood
    is CaptureData.GalleryWall -> d.mood
    is CaptureData.OpenNotebook -> when (val sub = d.subData) {
        is CaptureData.Marginalia -> sub.mood
        is CaptureData.ReelNotes -> sub.mood
        is CaptureData.SoundBite -> sub.mood
        is CaptureData.FieldNotes -> sub.mood
        is CaptureData.GalleryWall -> sub.mood
        else -> null
    }
    else -> null
}

/**
 * v7.40 — the hero's tiny date line: entries captured TODAY show the
 * capture time, older entries a short relative label ("yesterday" / "3d
 * ago") — the date itself already sits on the segment's title line.
 */
private fun heroDateTinyLabel(entry: CurioEntry): String {
    // The explore-session duration moved OUT of this tiny line in v22 — it
    // now has its own "Session" segment in the frosted bar right beside
    // Mood, so the date line stays just the capture time / relative day.
    return when (val days = entry.capturedAtDaysAgo) {
        0 -> formatCapturedTime(entry.capturedAtMillis)
        1 -> "yesterday"
        else -> "${days}d ago"
    }
}

/**
 * One half of the hero's frosted date/type bar — icon over value over a
 * "Date"/"Type" label, in the card's ink ([ink], deep slate, defined with
 * the hero's inks) so it reads on the frosted white glass in every theme
 * and pastel mode.
 */
@Composable
private fun FrostedSegment(
    icon: String,
    title: String,
    subtitle: String,
    ink: Color = Color.White,
    modifier: Modifier = Modifier,
    /** v7.39 — a whisper-small third line (e.g. the capture TIME under the
     *  hero's date), rendered tiny so it never competes with the title. */
    tiny: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CurioIcon(
            name = icon,
            contentDescription = null,
            tint = ink.copy(alpha = 0.95f),
            size = 18.dp
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = ink,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.85f)
        )
        if (tiny != null) {
            Text(
                text = tiny,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = ink.copy(alpha = 0.78f)
            )
        }
    }
}


@Composable
private fun FormatBody(
    entry: CurioEntry,
    category: CurioCategory,
    navController: NavController
) {
    // Multi-section entries render a compact section switcher that flips
    // between the individual format bodies (never merged into one page).
    if (entry.captureData is CaptureData.Portfolio) {
        PortfolioRender(entry, category, navController)
        return
    }
    when (entry.format) {
        CaptureFormat.SoundBite -> SoundBiteRender(entry, category)
        CaptureFormat.ReelNotes -> ReelNotesRender(entry, category)
        CaptureFormat.Marginalia -> MarginaliaRender(entry, category, navController)
        CaptureFormat.GalleryWall -> GalleryWallRender(entry, category, navController)
        CaptureFormat.FieldNotes -> FieldNotesRender(entry, category, navController)
        CaptureFormat.OpenNotebook -> OpenNotebookRender(entry, category, navController)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Quick fact card ("Quick fact" — the topic's one-line teaser)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * v7.38 — the saved-entry detail page's "quick fact": the topic's one-line
 * teaser under a small "Quick fact" heading, mirroring the Spin reveal's
 * TeaserCard language so a capture keeps the curiosity that introduced it.
 *
 * v7.38 — BACKGROUNDLESS: the card fill and border are gone — the quick
 * fact sits directly on the page wash, which is already the entry's tinted
 * background. The ink is the category's theme-aware [categoryInk]: deep
 * accent in light / pastel-light (reads on the airy pastel washes), so the
 * text stays readable in every color mode. Collapsed to 2 lines with a
 * "…" fold toggle (only shown when the teaser actually overflows);
 * tapping it expands, and when expanded the "…" turns invisible (the tap
 * target stays) so tapping the same spot folds it back — v115: the old
 * "…more" / "…less" words are gone.
 *
 * v7.38 — SECONDARY HIERARCHY: the whole block is deliberately SMALLER
 * than the category label above it — a labelMedium caption heading + a
 * bodyMedium teaser — so the category owns the top of the page and the
 * quick fact reads as a spark beneath it, not a peer.
 */
@Composable
private fun QuickFactCard(
    cat: CurioCategory,
    teaser: String?,
    modifier: Modifier = Modifier,
    // v221 — QUOTES category labels this "Quote" instead of "Quick fact".
    label: String = "Quick fact"
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    val ink = cat.categoryInk()
    // v115 — the box behind the quick fact is a theme-aware OPAQUE plate now:
    // the old translucent white @38% washed out against the tinted page wash
    // in light and glowed like a bright sheet in dark mode. The scheme's
    // surface is lifted toward the category ink (a whisper of the entry's
    // color, like the settings cards). v157 — the dark-mode hairline rim is
    // GONE (the user asked); the plate reads defined by its fill alone.
    val paneFill = if (isCurioDarkTheme()) {
        lerp(MaterialTheme.colorScheme.surfaceContainerHigh, ink, 0.10f)
    } else {
        lerp(MaterialTheme.colorScheme.surfaceContainerLow, ink, 0.06f)
    }
    val paneShape = RoundedCornerShape(16.dp)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = CurioIcons.AutoAwesome,
                contentDescription = null,
                tint = ink,
                size = 14.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ink
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(paneShape)
                .background(paneFill)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = teaser ?: "Loading topic…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink,
                    softWrap = true,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    onTextLayout = { layoutResult -> hasOverflow = layoutResult.hasVisualOverflow }
                )
                if (expanded || hasOverflow) {
                    // v115 — the "…more" / "…less" words are gone: collapsed
                    // shows a lone "…" affordance; expanded turns it
                    // INVISIBLE but keeps the same tap target, so the fold
                    // toggle still works by tapping the same spot.
                    Text(
                        text = "…",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (expanded) Color.Transparent else ink.copy(alpha = 0.85f),
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Multi-section render — a compact chip row switches between the entry's
 * sections; the active section's own format body renders below. Each chip
 * shows the section's format glyph + short name.
 */
@Composable
private fun PortfolioRender(entry: CurioEntry, category: CurioCategory, navController: NavController) {
    val data = entry.captureData as? CaptureData.Portfolio ?: return
    var activeIndex by rememberSaveable(entry.id) { mutableIntStateOf(0) }
    val section = data.sections.getOrNull(activeIndex) ?: return

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Section switcher chips ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.sections.forEachIndexed { i, s ->
                val selected = i == activeIndex
                Surface(
                    onClick = { activeIndex = i },
                    shape = RoundedCornerShape(50),
                    color = if (selected) category.themedAccent()
                            else category.categorySurface(MaterialTheme.colorScheme.surfaceVariant),
                    // v27q — flat 2dp: selection reads through the solid
                    // accent fill.
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurioIcon(
                            name = formatGlyph(s.format),
                            contentDescription = null,
                            tint = if (selected) category.onAccent() else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 14.dp
                        )
                        Text(
                            text = s.format.shortName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) category.onAccent() else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // ── Active section's format body ──────────────────────────────
        val subEntry = CurioEntry(
            id = entry.id,
            topic = entry.topic,
            format = section.format,
            captureData = section.data,
            title = section.title ?: entry.title,
            capturedAtMillis = entry.capturedAtMillis
        )
        // Synthesized section entry shares the parent entry's id; it is
        // rendered read-only (no save-affecting actions on a sub-entry).
        FormatBody(entry = subEntry, category = category, navController = navController)
    }
}

// ── Per-format render composables ─────────────────────────────────────

@Composable
private fun SoundBiteRender(
    entry: CurioEntry,
    category: CurioCategory
) {
    val data = entry.captureData as? CaptureData.SoundBite ?: return

    // ── Offline transcription (v125) — turns the RECORDING itself into
    // text using the downloaded Vosk model (Settings → Recording → Offline
    // model). Runs fully on-device; the transcript is persisted to Room so
    // it survives revisits and shows in a collapsible box below the player.
    val detailContext = LocalContext.current
    val detailScope = rememberCoroutineScope()
    var transcribing by remember { mutableStateOf(false) }
    var transcribeProgress by remember { mutableFloatStateOf(0f) }
    var transcribeError by remember { mutableStateOf<String?>(null) }
    var transcriptExpanded by remember { mutableStateOf(false) }
    // Re-read the model state every composition (and after a Settings
    // download/delete bumps the version) so the Transcribe affordance
    // appears the moment a model is available.
    AppPreferences.offlineModelVersionState
    val offlineModelId = AppPreferences.offlineModelIdState
    val offlineModelDownloaded = VoskModels.isDownloaded(detailContext, offlineModelId)

    fun startOfflineTranscription() {
        if (transcribing) return
        val modelId = AppPreferences.getOfflineModelId(detailContext)
        // v158 — only transcribe with a model that is still in the catalog
        // (the Full server-grade models were removed; a stale selection or
        // orphaned install must not try to load a removed model again).
        if (VoskModels.byId(modelId) == null || !VoskModels.isDownloaded(detailContext, modelId)) return
        transcribing = true
        transcribeError = null
        transcribeProgress = 0f
        detailScope.launch {
            val text = OfflineTranscriber.transcribe(
                context = detailContext,
                audioPath = data.audioFilePath,
                modelId = modelId
            ) { transcribeProgress = it }
            transcribing = false
            if (text != null) {
                // REPLACE by id — the detail flow refreshes reactively.
                runCatching {
                    CurioRepositoryHolder.repo.save(
                        entry.copy(captureData = data.copy(transcript = text))
                    )
                }
            } else {
                transcribeError = "Couldn't transcribe this recording. Check the audio and try again."
            }
        }
    }

    fun clearTranscript() {
        detailScope.launch {
            runCatching {
                CurioRepositoryHolder.repo.save(
                    entry.copy(captureData = data.copy(transcript = null))
                )
            }
        }
    }

    // v7.42 — no box: the voice note, note, and quotes sit directly on the
    // page wash (the main background), so the category artwork behind them
    // reads through — the old tinted Surface ("the white layer") is gone.
    // The shape is 0dp so the Surface never clips the paper cards' torn
    // edges (a 20dp round clip would shave the corners now that the cards
    // run to the full bounds instead of sitting behind a padded box).
    Surface(
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Voice-note label — restored (v7.43): "Voice note · 12s ·
            // 1.2MB" plus the optional title and the encoding chip, sitting
            // right ABOVE the capsule player so the recording reads as a
            // titled voice note instead of an anonymous bar on the page.
            // v7.44 — the primary line is titleMedium again (the size it
            // wore before the v7.42 removal), and the title moves to its
            // own line below it; the encoding chip rides the primary line.
            if (!data.audioFilePath.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurioIcon(
                            name = CurioIcons.Mic,
                            contentDescription = null,
                            tint = category.categoryInk(),
                            size = 18.dp
                        )
                        Text(
                            text = buildString {
                                append("Voice note · ${data.durationSeconds}s")
                                if (data.fileSizeBytes > 0) {
                                    append(" · ${formatFileSize(data.fileSizeBytes)}")
                                }
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = category.categoryInk(),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (data.fileSizeBytes > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = category.themedAccent().copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = data.encodingFormat,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = category.categoryInk(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Real audio player (when file path is available) ─────────
            if (!data.audioFilePath.isNullOrBlank()) {
                AudioPlayerBar(
                    audioFilePath = data.audioFilePath,
                    accent = category.themedAccent(),
                    // Played waveform bars: deep accent, the unplayed ones
                    // ride the 20% tint wash — the progress split reads
                    // consistently on the light page.
                    playedAccent = category.themedAccent(),
                    ink = category.categoryInk(),
                    tint = category.tint,
                    surface = category.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            }

            // ── Offline transcript (v137) — only when the take HAS audio:
            // a note-only Sound Bite (no recording) has nothing to
            // transcribe, so the Transcribe button and the transcript box
            // stay hidden entirely.
            val transcript = data.transcript?.takeIf { it.isNotBlank() }
            if (transcript == null && !data.audioFilePath.isNullOrBlank()) {
                if (offlineModelDownloaded) {
                    Surface(
                        onClick = { startOfflineTranscription() },
                        enabled = !transcribing,
                        shape = RoundedCornerShape(50),
                        // v27n — opaque accent-tinted fill.
                        color = if (transcribing) MaterialTheme.colorScheme.surfaceVariant
                                else lerp(MaterialTheme.colorScheme.surface, category.themedAccent(), 0.12f),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (transcribing) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = category.categoryInk(),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                CurioIcon(
                                    name = CurioIcons.AutoAwesome,
                                    contentDescription = null,
                                    tint = category.categoryInk(),
                                    size = 18.dp
                                )
                            }
                            Text(
                                text = if (transcribing) "Transcribing… ${(transcribeProgress * 100).toInt()}%"
                                       else "Transcribe voice note",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = category.categoryInk()
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurioIcon(
                            name = CurioIcons.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 16.dp
                        )
                        Text(
                            text = "Download an offline model in Settings → Recording to transcribe this voice note.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (transcribeError != null) {
                    Text(
                        text = transcribeError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (transcript != null && !data.audioFilePath.isNullOrBlank()) {
                // ── Transcript box — collapsed to a few lines with an
                // Expand button for the full text ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurioIcon(
                            name = CurioIcons.Mic,
                            contentDescription = null,
                            tint = category.categoryInk(),
                            size = 16.dp
                        )
                        Text(
                            text = "Transcript",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = category.categoryInk()
                        )
                        Spacer(Modifier.weight(1f))
                        // Re-transcribe (uses the downloaded model again).
                        Surface(
                            onClick = { startOfflineTranscription() },
                            enabled = !transcribing,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            CurioIcon(
                                name = CurioIcons.Refresh,
                                contentDescription = "Re-transcribe",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        // Clear the transcript.
                        Surface(
                            onClick = { clearTranscript() },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            CurioIcon(
                                name = CurioIcons.Close,
                                contentDescription = "Remove transcript",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                    if (transcribing) {
                        LinearProgressIndicator(
                            progress = { transcribeProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = category.themedAccent(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurioDarkTheme()) {
                            lerp(MaterialTheme.colorScheme.surfaceContainerHigh, category.categoryInk(), 0.08f)
                        } else {
                            lerp(MaterialTheme.colorScheme.surfaceContainerLow, category.categoryInk(), 0.05f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = transcript,
                                style = MaterialTheme.typography.bodyMedium,
                                color = category.categoryInk(),
                                maxLines = if (transcriptExpanded) Int.MAX_VALUE else 3,
                                overflow = if (transcriptExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                            )
                            TextButton(
                                onClick = { transcriptExpanded = !transcriptExpanded },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (transcriptExpanded) "Collapse" else "Expand",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = category.categoryInk()
                                )
                            }
                        }
                    }
                }
            }

            // ── Note — shown on the same note-paper slip the editor used ──
            if (!data.note.isNullOrBlank()) {
                val noteSheet = data.noteColor ?: NotePaperColor.CREAM
                NotePaperCard(
                    style = data.noteStyle ?: data.notePaperStyle(),
                    seed = noteSeed(entry.id, 1),
                    paperColor = noteSheet,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                    ruleSpacing = SavedNoteRuleSpacing,
                    tailSpace = SavedNoteTailSpace,
                    minHeight = 120.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        buildRichAnnotated(data.note, data.noteSpans.orEmpty(), notePaperHighlight(noteSheet)),
                        style = savedNoteStyle(),
                        color = notePaperInk(noteSheet)
                    )
                }
            }

            // ── Quote cards — shared hand-placed paper notecards ─────────
            RenderQuoteCards(
                // orEmpty() guards legacy Gson blobs where the quotes field is
            // absent — missing Kotlin-default List fields decode to null, not
            // empty (the mood-board crash).
            quotes = data.quotes.orEmpty(),
                spans = data.quoteSpans.orEmpty(),
                tilts = data.quoteTilts.orEmpty(),
                styles = data.quoteStyles.orEmpty(),
                colors = data.quoteColors.orEmpty(),
                fallbackStyle = data.notePaperStyle(),
                entryId = entry.id,
                topicName = entry.topic.name,
                category = category
            )
        }
    }
}

/**
 * Single-capsule ExoPlayer audio bar — ONE play/pause button with a real
 * waveform flowing past it, styled to mirror the recording visualizer's
 * capsule bars. The waveform is extracted from the audio file using
 * [WaveformExtractor]; played bars show in [accent], unplayed in [tint].
 * Tap or drag on the bars to seek.
 */
@Composable
private fun AudioPlayerBar(
    audioFilePath: String,
    accent: Color,
    playedAccent: Color,
    ink: Color,
    tint: Color,
    surface: Color
) {
    val context = LocalContext.current
    // v5.8 — saveable so rotation keeps the playback position + playing
    // state; the recreated player below re-seeks/resumes from them.
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var currentPosition by rememberSaveable { mutableLongStateOf(0L) }
    var duration by rememberSaveable { mutableLongStateOf(0L) }
    var sliderPosition by rememberSaveable { mutableFloatStateOf(0f) }

    // Extract waveform samples off the main thread. Same bar language as the
    // recording visualizer (LiveWaveform uses 36 capsule bars) so the saved
    // view looks identical to the meter the user recorded into.
    val waveformSamples by produceState<FloatArray>(
        initialValue = FloatArray(36),
        key1 = audioFilePath
    ) {
        value = withContext(kotlinx.coroutines.Dispatchers.Default) {
            WaveformExtractor.extract(audioFilePath, barCount = 36)
        } ?: FloatArray(36) { kotlin.random.Random.nextFloat() * 0.6f + 0.2f }
    }

    // The stored audioFilePath is a RAW absolute filesystem path (e.g.
    // /data/user/0/com.curio.app/files/audio/xyz.m4a) — feeding that string
    // straight to MediaItem.fromUri() parses it as a schemeless URI that
    // ExoPlayer's DefaultDataSource cannot resolve, so the audio would never
    // play. Wrap it in a file:// URI instead (pass through unchanged if the
    // path ever arrives already schemed, e.g. content:// from a picker).
    val audioUri = remember(audioFilePath) {
        val parsed = Uri.parse(audioFilePath)
        if (parsed.scheme != null) parsed else Uri.fromFile(File(audioFilePath))
    }
    val player = remember(audioUri) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            // Route to the media audio stream with proper focus handling —
            // without AudioAttributes some devices route to a silent output
            // or duck audio, which reads as "plays but no sound".
            setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            setHandleAudioBecomingNoisy(true)
            setVolume(1f)
            setMediaItem(MediaItem.fromUri(audioUri))
            prepare()
            playWhenReady = false
        }
    }

    // v5.8 — after rotation the player is recreated fresh; resume from the
    // saveable position/state so a voice note keeps its place.
    LaunchedEffect(player) {
        if (currentPosition > 0L) player.seekTo(currentPosition)
        if (isPlaying) player.play()
    }

    // ── Observe player state ────────────────────────────────────────────
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        duration = player.duration.coerceAtLeast(0)
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        // Park the player back at the start so the next tap
                        // on the play button replays instead of dead-ending.
                        currentPosition = 0L
                        sliderPosition = 0f
                        player.seekTo(0)
                    }
                    Player.STATE_IDLE -> {
                        // A failed load (missing/corrupt file) leaves the
                        // player IDLE — don't leave the UI stuck "playing".
                        isPlaying = false
                    }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                // Broken file or decode failure: reset the UI so the button
                // doesn't look stuck. DON'T seek here — by the time this
                // fires the player is typically already in the errored IDLE
                // state where seek commands are unavailable, and the call
                // would throw. The play-button retry path (prepare() on
                // IDLE) restarts from the top on the next tap.
                isPlaying = false
                currentPosition = 0L
                sliderPosition = 0f
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // ── Poll position while playing ─────────────────────────────────────
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition.coerceAtLeast(0)
            sliderPosition = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            kotlinx.coroutines.delay(200)
        }
    }

    // ── Single-capsule player — one button, capsule bars ───────────────
    // Mirrors the recording visualizer (rounded capsule bars): the ONE
    // play/pause button sits inside the capsule with the waveform flowing
    // past it, so playback reads like the live meter seen while recording.
    Surface(
        shape = RoundedCornerShape(50),
        color = surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── The one button ─────────────────────────────────────────
            Surface(
                onClick = {
                    if (isPlaying) {
                        player.pause()
                    } else {
                        // Replay from the start: if the clip ended, ExoPlayer
                        // won't restart on play() alone — re-seek to 0 first.
                        // If it errored into IDLE, play() also won't restart
                        // it: re-prepare the media item so the retry loads.
                        if (player.playbackState == Player.STATE_ENDED) {
                            player.seekTo(0)
                        } else if (player.playbackState == Player.STATE_IDLE) {
                            player.prepare()
                        }
                        player.play()
                    }
                },
                shape = RoundedCornerShape(50),
                color = accent,
                shadowElevation = 0.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        name = if (isPlaying) CurioIcons.Pause else CurioIcons.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        // v7.5 — pastel mode lightens the accent fill, so the
                        // glyph flips to a deep ink of the accent.
                        tint = pastelFillInk(accent),
                        size = 24.dp
                    )
                }
            }

            // ── Capsule-bar visualizer (tap/drag to seek) ─────────────
            WaveformCanvas(
                samples = waveformSamples,
                progress = sliderPosition,
                accent = playedAccent,
                tint = tint,
                onSeek = { fraction ->
                    sliderPosition = fraction.coerceIn(0f, 1f)
                    val seekMs = (fraction * duration).toLong()
                    player.seekTo(seekMs)
                    currentPosition = seekMs
                },
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
            )

            // ── Elapsed / total ────────────────────────────────────────
            Text(
                text = "${formatMs(currentPosition)} / ${formatMs(duration)}",
                style = MaterialTheme.typography.labelSmall,
                // The player receives a dedicated readable ink because its
                // waveform colors and its elapsed-label color serve different
                // contrast roles.
                color = if (AppPreferences.tintWashEffective()) ink
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Renders waveform capsule bars with progress coloring + seek support.
 *
 * Same bar language as the recording [LiveWaveform] (rounded capsule bars,
 * no indicator line — the accent/tint split IS the progress readout).
 *
 * @param samples  Normalized amplitude values (0.0–1.0) from [WaveformExtractor].
 * @param progress Playback progress fraction (0.0–1.0).
 * @param accent   Color for the played portion of the waveform.
 * @param tint     Color for the unplayed portion.
 * @param onSeek   Called with fraction (0.0–1.0) when the user taps or drags.
 */
@Composable
private fun WaveformCanvas(
    samples: FloatArray,
    progress: Float,
    accent: Color,
    tint: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragFraction by remember { mutableFloatStateOf(-1f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(dragFraction)
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragFraction = (dragFraction + dragAmount / size.width).coerceIn(0f, 1f)
                        onSeek(dragFraction)
                    },
                    onDragEnd = { dragFraction = -1f }
                )
            }
    ) {
        if (samples.isEmpty()) return@Canvas

        val barCount = samples.size
        val gap = 2.dp.toPx()
        val totalGap = gap * (barCount - 1)
        val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(1f)
        val playedIndex = (progress * barCount).toInt().coerceIn(0, barCount)

        // Capsule bars — played in accent, unplayed in tint. Matches the
        // recording LiveWaveform so saved voice notes look like the meter
        // the user recorded into.
        for (i in 0 until barCount) {
            val barHeight = samples[i] * size.height * 0.92f
            val x = i * (barWidth + gap)
            val y = (size.height - barHeight) / 2f
            val color = if (i <= playedIndex) accent else tint

            drawRoundRect(
                color = color.copy(alpha = if (i <= playedIndex) 0.95f else 0.5f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
private fun formatMs(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}

/** Format bytes to a human-readable size string (e.g. "1.2 MB"). */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
    }
}

@Composable
private fun ReelNotesRender(entry: CurioEntry, category: CurioCategory) {
    val data = entry.captureData as? CaptureData.ReelNotes
    
    // Handle null or malformed data gracefully
    if (data == null) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (AppPreferences.tintWashEffective()) category.tint.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurioIcon(
                    CurioIcons.Movie, null,
                    tint = category.categoryInk().copy(alpha = 0.5f),
                    size = 48.dp
                )
                Text(
                    "No review data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (AppPreferences.tintWashEffective()) category.categoryInk()
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Rating — Canvas stars (the Material Symbols Outlined font renders
        // even `star` as a hollow outline, so filled stars are solid paths
        // and the remainder ghost at low alpha as a 5-slot scale) ─────────
        if (data.rating > 0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                // Soft pastel card that matches the app's palette — a light,
                // barely-there whisper of the category, lighter and less
                // saturated than the other cards on the page. v81 — dark:
                // the dark category surface instead.
                color = if (isCurioDarkTheme()) {
                    category.categorySurface()
                } else {
                    lightAccentTint(category.accent, saturation = 0.18f, lightness = 0.93f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { i ->
                            val starFilled = i < data.rating.coerceIn(0, 5)
                            FilledStar(
                                color = if (starFilled) category.categoryInk()
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                filled = starFilled,
                                starSize = 24.dp
                            )
                        }
                    }
                    // Subtle caption under the stars — the same help language
                    // as the capture editor's rating row, so the saved card
                    // reads as the rating's label.
                    Text(
                        text = "Rate quality",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // v7.36 — adaptive aspect-ratio gallery (Google-Photos style):
        // images keep their real shape and pack into justified rows (a wide
        // shot + a portrait, then three portraits on the next line). Tapping
        // one zooms it IN PLACE over the page (the same no-scrim mood-board
        // zoom) — no Lightbox page. Legacy entries only stored a count, so
        // keep the badge fallback below. orEmpty() guards legacy Gson blobs
        // where the imageUris field is absent (missing Kotlin-default fields
        // decode to null, not default).
        val attachedUris = data.imageUris.orEmpty()
        if (attachedUris.isNotEmpty()) {
            AdaptiveImageGallery(
                uris = attachedUris,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (data.imageCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (AppPreferences.tintWashEffective()) category.tint
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurioIcon(
                        CurioIcons.Image, null,
                        tint = category.categoryInk(),
                        size = 18.dp
                    )
                    Text(
                        "${data.imageCount} image${if (data.imageCount != 1) "s" else ""} attached",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        // Review text with better styling — wears the same note-paper box
        // as the Marginalia journal, so the saved review reads as written
        // on paper in light AND dark mode (NotePaperCard is theme-aware and
        // renders the torn-note style when that's what was chosen).
        val reviewSheet = data.reviewColor ?: NotePaperColor.CREAM
        if (!data.reviewText.isNullOrBlank()) {
            NotePaperCard(
                style = data.reviewStyle ?: data.notePaperStyle(),
                seed = noteSeed(entry.id, 2),
                paperColor = reviewSheet,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                ruleSpacing = SavedNoteRuleSpacing,
                tailSpace = SavedNoteTailSpace,
                minHeight = 120.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    buildRichAnnotated(data.reviewText, data.reviewSpans.orEmpty(), notePaperHighlight(reviewSheet)),
                    style = savedNoteStyle(),
                    color = notePaperInk(reviewSheet)
                )
            }
        } else {
            // Fallback when no review text
            NotePaperCard(
                style = data.reviewStyle ?: data.notePaperStyle(),
                seed = noteSeed(entry.id, 3),
                paperColor = reviewSheet,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                ruleSpacing = SavedNoteRuleSpacing,
                tailSpace = SavedNoteTailSpace,
                minHeight = 120.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "No review written yet",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PatrickHandFontFamily),
                    color = notePaperInk(reviewSheet).copy(alpha = 0.55f)
                )
            }
        }

        // ── Quote cards — shared hand-placed paper notecards ─────────────
        RenderQuoteCards(
            // orEmpty() guards legacy Gson blobs where the quotes field is
            // absent — missing Kotlin-default List fields decode to null, not
            // empty (the mood-board crash).
            quotes = data.quotes.orEmpty(),
            spans = data.quoteSpans.orEmpty(),
            tilts = data.quoteTilts.orEmpty(),
            styles = data.quoteStyles.orEmpty(),
            colors = data.quoteColors.orEmpty(),
            fallbackStyle = data.notePaperStyle(),
            entryId = entry.id,
            topicName = entry.topic.name,
            category = category
        )
    }
}

@Composable
private fun MarginaliaRender(entry: CurioEntry, category: CurioCategory, navController: NavController) {
    val data = entry.captureData as? CaptureData.Marginalia ?: return
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        data.fieldMindMetadata?.let { metadata ->
            FieldMindMetadataCard(metadata = metadata, category = category)
        }
        // ── Journal — "My thoughts" on a note-paper page ──────────────
        if (!data.journalText.isNullOrBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MarginaliaSectionHeader(label = "My thoughts", category = category)
                val journalSheet = data.journalColor ?: NotePaperColor.CREAM
                NotePaperCard(
                    style = data.journalStyle ?: data.notePaperStyle(),
                    seed = noteSeed(entry.id, 4),
                    paperColor = journalSheet,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                    ruleSpacing = SavedNoteRuleSpacing,
                    tailSpace = SavedNoteTailSpace,
                    minHeight = 120.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        buildRichAnnotated(
                            data.journalText,
                            data.journalSpans.orEmpty(),
                            notePaperHighlight(journalSheet)
                        ),
                        style = savedNoteStyle(),
                        color = notePaperInk(journalSheet)
                    )
                }
            }
        }

        // ── Favorite quotes — shared hand-placed paper notecards ─────────
        RenderQuoteCards(
            // orEmpty() guards legacy Gson blobs where the quotes field is
            // absent — missing Kotlin-default List fields decode to null, not
            // empty (the mood-board crash).
            quotes = data.quotes.orEmpty(),
            spans = data.quoteSpans.orEmpty(),
            tilts = data.quoteTilts.orEmpty(),
            styles = data.quoteStyles.orEmpty(),
            colors = data.quoteColors.orEmpty(),
            fallbackStyle = data.notePaperStyle(),
            entryId = entry.id,
            topicName = entry.topic.name,
            category = category
        )

        // ── Attachments — gallery images + optional voice note ─────────
        // (orEmpty() guards legacy blobs where imageUris is absent.)
        // v7.36 — adaptive aspect-ratio gallery (Google-Photos style) with
        // in-place zoom — no Lightbox page, no dark scrim — matching the
        // other formats. ALL images show; none get dropped.
        val attachedUris = data.imageUris.orEmpty()
        if (attachedUris.isNotEmpty()) {
            AdaptiveImageGallery(
                uris = attachedUris,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!data.audioFilePath.isNullOrBlank()) {
            AudioPlayerBar(
                audioFilePath = data.audioFilePath,
                accent = category.themedAccent(),
                // Same played/unplayed split as the SoundBite section: deep
                // accent played, 20% tint wash unplayed.
                playedAccent = category.themedAccent(),
                ink = category.categoryInk(),
                tint = category.tint,
                surface = category.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        }
    }
}

/**
 * Shared saved-view quote cards — the hand-placed paper notecards used by
 * Marginalia, Reel Notes, Sound Bite and the Mood Board detail pages. Pads
 * quoteSpans to the quotes length (legacy Gson blobs may carry fewer/absent
 * span lists), keeps the ORIGINAL index through the blank filter so each
 * card can find its saved tilt (quoteTilts parallels quotes 1:1), and
 * renders the section header + a rotated paper card per non-blank quote.
 */
@Composable
private fun RenderQuoteCards(
    quotes: List<String>,
    spans: List<List<TextSpan>>,
    tilts: List<Float>,
    styles: List<NotePaperStyle>,
    colors: List<NotePaperColor>,
    fallbackStyle: NotePaperStyle,
    entryId: String,
    topicName: String,
    category: CurioCategory,
    label: String = "Favorite quotes",
    // v7.22 — optional per-index filter (mood board: render only the
    // BELOW-board subset; the on-board ones float on the collage).
    includeIndex: ((Int) -> Boolean)? = null
) {
    val context = LocalContext.current
    // Legacy Gson blobs decode missing Kotlin-default List fields to NULL
    // (not empty) — a null [quotes] crashed the saved mood-board detail view
    // here (.size() on null). Guard defensively so no caller can reintroduce
    // the crash.
    val safeQuotes = quotes.orEmpty()
    // Pad spans to the quotes length first (legacy Gson blobs may carry
    // fewer/absent span lists), then zip so the spans stay aligned with
    // their quote even when blank cards are filtered out. Keep the ORIGINAL
    // index through the blank filter so each card can find its saved tilt.
    val spansPadded = spans.toMutableList()
    while (spansPadded.size < safeQuotes.size) spansPadded.add(emptyList())
    val quotePairs = safeQuotes.mapIndexedNotNull { i, rawQuote ->
        val (quote, clippedSpans) = limitQuoteContent(rawQuote.orEmpty(), spansPadded[i])
        if (quote.isBlank()) null
        // Skip indices the caller excluded (mood board below-board split).
        else if (includeIndex != null && !includeIndex(i)) null
        else i to (quote to clippedSpans)
    }
    if (quotePairs.isNotEmpty()) {
        // v7.38 — no count pill: the header is just the glyph + label.
        MarginaliaSectionHeader(label = label, category = category)
        quotePairs.forEach { (origIndex, pair) ->
            val (quote, cardSpans) = pair
            // The tilt SAVED with this card (the exact angle the user added
            // with — never re-rolled). Legacy entries lack the field → fall
            // back to a stable random tilt keyed by the original index so
            // viewing never re-rolls it either.
            val rotation = tilts.getOrNull(origIndex)
                ?: remember(origIndex) { kotlin.random.Random.nextFloat() * 5f - 2.5f }
            val quoteSheet = colors.getOrNull(origIndex) ?: NotePaperColor.CREAM
            var renderedQuote by remember(quote, cardSpans) { mutableStateOf(quote) }
            NotePaperCard(
                style = styles.getOrNull(origIndex) ?: fallbackStyle,
                // Per-card salt so the quote cards on one page tear
                // distinctly, yet the same entry re-tears identically.
                seed = noteSeed(entryId, 50 + origIndex * 7),
                paperColor = quoteSheet,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                ruleSpacing = SavedNoteRuleSpacing,
                tailSpace = SavedNoteTailSpace,
                corner = 12.dp,
                // Hoist the 72dp floor INTO the modifier chain BEFORE the tilt
                // rotate: passing it as NotePaperCard's minHeight param appends
                // heightIn AFTER the call-site rotate (the card layer grew to
                // 72dp and the rotation pivot shifted for single-line quotes).
                // With heightIn first, the tilt pivots around the CONTENT's
                // center and stays put whether the quote is one line or five.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp)
                    .rotate(rotation)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CurioIcon(
                        name = CurioIcons.FormatQuote,
                        contentDescription = null,
                        tint = notePaperInk(quoteSheet).copy(alpha = 0.45f),
                        size = 20.dp
                    )
                    Text(
                        // Keep the quote symbols as real Material Symbols
                        // glyphs outside the text flow. The text therefore
                        // stays 1:1 with its saved rich spans, while the
                        // opening mark sits at the true start of the card.
                        text = buildRichAnnotated(
                            renderedQuote,
                            cardSpans,
                            notePaperHighlight(quoteSheet)
                        ),
                        modifier = Modifier.weight(1f),
                        style = savedNoteStyle(),
                        color = notePaperInk(quoteSheet),
                        // The closing glyph is anchored to the measured end
                        // of the text row below, so it follows the final
                        // visible line instead of floating at the card edge.
                        onTextLayout = { layout ->
                            if (layout.lineCount > QuoteLimits.MAX_LINES) {
                                val flowEnd = layout.getLineEnd(QuoteLimits.MAX_LINES - 1)
                                val contentEnd = flowEnd.coerceIn(0, renderedQuote.length)
                                if (contentEnd < renderedQuote.length) {
                                    renderedQuote = renderedQuote.take(contentEnd)
                                }
                            }
                        }
                    )
                    CurioIcon(
                        name = CurioIcons.FormatQuote,
                        contentDescription = null,
                        tint = notePaperInk(quoteSheet).copy(alpha = 0.45f),
                        size = 20.dp,
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .rotate(180f)
                    )
                    // ── Bookmark — saves the quote to the Home "Saved" shelf ──
                    val saved = AppPreferences.savedQuotesState.any {
                        it.entryId == entryId && it.quoteText == quote
                    }
                    Surface(
                        onClick = {
                            // Re-read the LATEST saved state on tap instead of
                            // trusting the composition-time snapshot, so the
                            // toggle always flips exactly ONE card's bookmark
                            // (each card is keyed by entryId + its own text).
                            val isSavedNow = AppPreferences.savedQuotesState.any {
                                it.entryId == entryId && it.quoteText == quote
                            }
                            if (isSavedNow) {
                                AppPreferences.removeSavedQuote(context, entryId, quote)
                            } else {
                                AppPreferences.saveQuote(
                                    context, entryId, topicName, category.id, quote
                                )
                            }
                        },
                        shape = CircleShape,
                        color = if (saved) category.themedAccent().copy(alpha = 0.16f)
                                else Color.Transparent
                    ) {
                        CurioIcon(
                            name = if (saved) CurioIcons.Bookmark else CurioIcons.BookmarkBorder,
                            contentDescription = if (saved) "Remove bookmark" else "Bookmark quote",
                            // v8.28 — saved bookmark wears the readable ink
                            // (deep in light + pastel, deep twin for pale
                            // accents, light twin in dark) so it never
                            // washes out on the pastel wash.
                            tint = if (saved) category.readableAccentInk()
                                   else notePaperInk(quoteSheet).copy(alpha = 0.45f),
                            size = 18.dp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Marginalia section header — small quote-mark glyph + label above the
 * journal / quotes sections, mirroring the capture form's section labels
 * so the saved view matches what the user wrote into.
 *
 * v7.38 — the count pill was removed from the detail view: the header is
 * just the glyph + label (a number next to the section name added noise
 * to the hierarchy the count was meant to summarize).
 */
@Composable
private fun MarginaliaSectionHeader(
    label: String,
    category: CurioCategory
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CurioIcon(
            name = CurioIcons.FormatQuote,
            contentDescription = null,
            tint = category.categoryInk(),
            size = 16.dp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Result of fitting a saved tile collage into a viewport — the uniformly
 * scaled tiles, the scaled board bounds and the scale. Shared by the inline
 * card (cover/crop) and the full-screen expanded dialog (contain/fit) so
 * both derive their geometry from the same helper: the expanded dialog
 * shows the whole collage, the inline card crops to its middle.
 */
private data class FitTileLayout(
    val scaledTiles: List<CaptureData.TileLayout>,
    val boardW: Float,
    val boardH: Float,
    val scale: Float
)

/**
 * Fits [tiles] into a [viewW]×[viewH] viewport: computes the collage's
 * bounds, scales uniformly, and returns the scaled tiles + scaled board
 * size + the scale. [cover] = false scales to the largest size that fits
 * BOTH dimensions (contain — the expanded full-board view); [cover] = true
 * scales to FILL the viewport (cover — the inline card's center-crop
 * "cropper" look, where the collage's middle art fills the card and the
 * overflow is clipped). With no tiles it returns an empty fit (scale 1f)
 * so callers can use the result unconditionally.
 */
private fun fitTileLayout(
    tiles: List<CaptureData.TileLayout>,
    viewW: Float,
    viewH: Float,
    cover: Boolean = false
): FitTileLayout {
    val safeTiles = sanitizeTileLayouts(tiles)
    if (safeTiles.isEmpty()) return FitTileLayout(emptyList(), 0f, 0f, 1f)
    val safeViewW = viewW.takeIf { it.isFinite() && it > 0f } ?: 1f
    val safeViewH = viewH.takeIf { it.isFinite() && it > 0f } ?: 1f
    val maxX = safeTiles.maxOf { it.offsetXPx + it.widthPx }
    val maxY = safeTiles.maxOf { it.offsetYPx + it.heightPx }
    val scale = if (maxX > 0f && maxY > 0f) {
        if (cover) (safeViewW / maxX).coerceAtLeast(safeViewH / maxY)
        else (safeViewW / maxX).coerceAtMost(safeViewH / maxY)
    } else 1f
    val safeScale = scale.takeIf { it.isFinite() && it > 0f }?.coerceAtMost(32f) ?: 1f
    val maxTileW = (safeViewW * 4f).coerceAtLeast(1f)
    val maxTileH = (safeViewH * 4f).coerceAtLeast(1f)
    val maxBoardW = maxTileW * 4f
    val maxBoardH = maxTileH * 4f
    val scaledTiles = safeTiles.map {
        CaptureData.TileLayout(
            uri = it.uri,
            // v113 — allow band placements (negative raw offsets from the
            // editor's full-card drag freedom); the ≥0 clamp pinned them to
            // the collage's top-left in the saved views.
            offsetXPx = (it.offsetXPx * safeScale).coerceIn(-maxBoardW, maxBoardW),
            offsetYPx = (it.offsetYPx * safeScale).coerceIn(-maxBoardH, maxBoardH),
            rotationDeg = it.rotationDeg,
            widthPx = (it.widthPx * safeScale).coerceIn(1f, maxTileW),
            heightPx = (it.heightPx * safeScale).coerceIn(1f, maxTileH)
        )
    }
    val boardW = (scaledTiles.maxOfOrNull { it.offsetXPx + it.widthPx } ?: 0f)
        .coerceAtMost(maxBoardW)
    val boardH = (scaledTiles.maxOfOrNull { it.offsetYPx + it.heightPx } ?: 0f)
        .coerceAtMost(maxBoardH)
    return FitTileLayout(scaledTiles, boardW, boardH, safeScale)
}

/**
 * Saved boards can contain malformed geometry from old drafts or interrupted
 * writes. Keep valid layouts unchanged, but prevent NaN/zero/absurd values
 * from reaching Compose's size/offset constraints.
 */
private fun sanitizeTileLayouts(tiles: List<CaptureData.TileLayout>): List<CaptureData.TileLayout> =
    tiles.mapNotNull { tile ->
        if (tile.uri.isBlank()) return@mapNotNull null
        val x = tile.offsetXPx.safeTileNumber(0f)
        val y = tile.offsetYPx.safeTileNumber(0f)
        val width = tile.widthPx.safeTileNumber(1f)
        val height = tile.heightPx.safeTileNumber(1f)
        tile.copy(
            offsetXPx = x,
            offsetYPx = y,
            widthPx = width,
            heightPx = height
        )
    }

private fun Float.safeTileNumber(fallback: Float): Float =
    if (isFinite()) coerceIn(fallback, 8192f) else fallback

/**
 * v7.23 — Save / Share the full mood board as a high-res PNG. Both actions
 * render the complete board off-screen (see [MoodBoardExport]) so the image
 * is sharp at any zoom. Shows a short toast confirming where it went.
 */
@Composable
private fun MoodBoardExportActions(
    data: CaptureData.GalleryWall,
    category: CurioCategory,
    boardSeed: Int,
    authority: String,
    entryId: String
) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    // v57 — Save/Share can render EITHER arrangement: the inline layout (what
    // the small saved card shows) or the full-screen layout (what the expanded
    // dialog shows) — the two views are saved separately on the entry now.
    var exportLayout by remember { mutableStateOf(MoodBoardExport.MoodBoardLayout.INLINE) }
    // v7.26 — Save/Share wear the frosted-glass look as the hero's Date ·
    // Mood · Type grid card: a translucent pane under a glass tint, a
    // hairline rim, and ink that reads in every theme. v81 — dark: light ink.
    val frostInk = if (isCurioDarkTheme()) Color(0xFFEDE7DC) else Color(0xFF232A35)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Layout picker — two compact glass pills, the same frosted language
        // as the buttons below.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportLayoutPill(
                label = "Inline view",
                selected = exportLayout == MoodBoardExport.MoodBoardLayout.INLINE,
                frostInk = frostInk,
                category = category,
                onClick = { exportLayout = MoodBoardExport.MoodBoardLayout.INLINE },
                modifier = Modifier.weight(1f)
            )
            ExportLayoutPill(
                label = "Full-screen view",
                selected = exportLayout == MoodBoardExport.MoodBoardLayout.FULL,
                frostInk = frostInk,
                category = category,
                onClick = { exportLayout = MoodBoardExport.MoodBoardLayout.FULL },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FrostedExportButton(
                busy = busy,
                icon = CurioIcons.Image,
                label = "Save PNG",
                frostInk = frostInk,
                category = category,
                onClick = {
                    if (busy) return@FrostedExportButton
                    busy = true
                    MoodBoardExport.saveMoodBoardPng(
                        context = context,
                        authority = authority,
                        data = data,
                        category = category,
                        boardSeed = boardSeed,
                        entryId = entryId,
                        layout = exportLayout
                    ) { path ->
                        busy = false
                        android.widget.Toast.makeText(
                            context,
                            if (path != null) "Mood board saved to Gallery"
                            else "Couldn't save the mood board",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            FrostedExportButton(
                busy = busy,
                icon = CurioIcons.Share,
                label = "Share PNG",
                frostInk = frostInk,
                category = category,
                onClick = {
                    if (busy) return@FrostedExportButton
                    busy = true
                    MoodBoardExport.shareMoodBoardPng(
                        context = context,
                        authority = authority,
                        data = data,
                        category = category,
                        boardSeed = boardSeed,
                        entryId = entryId,
                        layout = exportLayout
                    ) {
                        busy = false
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * v57 — one side of the Save/Share layout picker: a frosted-glass pill in
 * the same language as the export buttons below, tinted with the category
 * wash when selected so the active view reads at a glance.
 */
@Composable
private fun ExportLayoutPill(
    label: String,
    selected: Boolean,
    frostInk: Color,
    category: CurioCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                category.categoryBackgroundWash(),
                                category.categorySurface(MaterialTheme.colorScheme.surface)
                            )
                        )
                    )
                    .clip(RoundedCornerShape(14.dp))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (selected) Color.White.copy(alpha = 0.66f)
                        else Color.White.copy(alpha = 0.30f)
                    )
            )
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = frostInk.copy(alpha = if (selected) 1f else 0.72f)
                )
            }
        }
    }
}

/**
 * v7.26 — one Save/Share export button in the WHITE FROSTED-GLASS language
 * of the hero's Date · Mood · Type grid card: a translucent pane carrying
 * the page's category wash blurred + clipped to the button, a white 0.78
 * glass tint on top, a hairline deep-slate rim, and deep-slate ink content
 * that reads on white in every theme (mirrors the hero card exactly — the
 * slate was chosen because white/onAccent ink would vanish on the white
 * pane). [busy] swaps the label to "Rendering…" and the disabled state.
 */
@Composable
private fun FrostedExportButton(
    busy: Boolean,
    icon: String,
    label: String,
    frostInk: Color,
    category: CurioCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        // v27n — frosted glass button: the translucent wash + white frost
        // can't hold an elevation shadow (it bleeds through), stays flat.
        color = Color.Transparent,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Box(Modifier.fillMaxWidth()) {
            // ── Frosted pane: the page's category wash blurred + clipped to
            // the button, sitting BEHIND the crisp content — same structure
            // as the hero grid card's pane (the wash is what's actually
            // behind these buttons).
            Box(
                // Same static-glow treatment as the hero pane: the wash
                // gradient is already smooth, so the 18dp blur was a
                // per-frame GPU no-op during scroll.
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(category.categoryBackgroundWash(), category.categorySurface(MaterialTheme.colorScheme.surface))))
                    .clip(RoundedCornerShape(16.dp))
            )
            // ── Frosted white glass tint ── the blurred color blooms behind
            // stay faintly visible, but the button reads as WHITE frosted
            // glass with deep-slate content (no theme tint).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.78f))
            )
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurioIcon(
                    name = icon,
                    contentDescription = null,
                    tint = frostInk.copy(alpha = 0.95f),
                    size = 16.dp
                )
                Text(
                    text = if (busy) "Rendering…" else label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = frostInk
                )
            }
        }
    }
}

/**
 * v27 — the explore session's attachments block on the entry detail page:
 * the SHARED session note (universal — the same note that rode in on every
 * entry saved from the session) plus the captured screenshots as tappable
 * thumbnails (bubble capture + auto-attached device shots). Tap opens the
 * lightbox. Rendered between the tags and the format body.
 */
@Composable
private fun SessionNoteBlock(
    entry: CurioEntry,
    category: CurioCategory,
    navController: NavController
) {
    val accent = category.themedAccent()
    val ink = category.categoryInk()
    Column(
        modifier = Modifier
            .padding(horizontal = detailBodyGutter())
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Shared session note ──────────────────────────────────────
        // v27h — the saved note wears the theme-aware note-PAPER sheet with
        // its own readable ink instead of the faint tint wash, so it never
        // washes out against the tinted detail page in either theme.
        entry.sessionNote?.takeIf { it.isNotBlank() }?.let { note ->
            val paperInkColor = notePaperInk(NotePaperColor.CREAM)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = notePaperSurface(NotePaperColor.CREAM),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        CurioIcon(
                            name = CurioIcons.Note,
                            contentDescription = null,
                            tint = ink,
                            size = 18.dp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Session note",
                            style = MaterialTheme.typography.labelSmall,
                            color = paperInkColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = paperInkColor
                        )
                    }
                }
            }
        }

        // ── Session screenshots — tappable thumbnails → lightbox ────
        if (entry.sessionScreenshots.isNotEmpty()) {
            Text(
                text = "Session screenshots",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                entry.sessionScreenshots.forEach { path ->
                    val file = remember(path) { File(path) }
                    val painter = rememberAsyncImagePainter(file)
                    Surface(
                        onClick = {
                            navController.navigate(
                                CurioRoutes.lightbox(Uri.fromFile(file).toString())
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(96.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painter,
                            contentDescription = "Session screenshot",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryWallRender(entry: CurioEntry, category: CurioCategory, navController: NavController) {
    val data = entry.captureData as? CaptureData.GalleryWall ?: return
    val density = androidx.compose.ui.platform.LocalDensity.current
    var boardExpanded by remember { mutableStateOf(false) }
    // v7.24 — in-place SINGLE-IMAGE zoom: double-tapping a tile glides only
    // that image from its spot on the collage to the card's center (arc),
    // where pinch/pan refine it — the board around it never moves.
    val zoomState = rememberMoodBoardZoomState()

    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Mood board canvas with tile positions ──────────────────────
        // The board's watermark pattern is seeded from the entry id so each
        // saved mood board keeps its own stable background collage.
        val boardSeed = remember(entry.id) { entry.id.hashCode() }
        Surface(
            shape = RoundedCornerShape(28.dp),
        // The saved board wears an OPAQUE category-tinted card surface
        // ([categorySurfaceMoodBoard] — the same card language as the rest
        // of the page, honoring the manual Settings tint toggle). The old
        // 20%-alpha [CurioCategory.tint] let the page-level watermark glyphs
        // bleed through and collide with the board's own seeded glyph pattern
        // (two overlapping watermarks); an opaque surface hides the page
        // watermark so only the board's [CurioMoodBoardBackdrop] shows.
        color = category.categorySurfaceMoodBoard(),
            // The saved board sits on the tinted page — the elevation lift
            // keeps it from blending into the wash.
            shadowElevation = 3.dp,
            // v7.17 — the whole board (and its in-place zoom overlay) draws
            // ABOVE the caption + quote cards that follow it in this Column:
            // the zoomed image overflows the card, and later siblings would
            // otherwise paint over it (the "zoomed image behind the text
            // box" bug). zIndex only matters while zoomed — at rest nothing
            // overlaps.
            modifier = Modifier.fillMaxWidth().height(460.dp).zIndex(1000f)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasW = with(density) { maxWidth.toPx() }
                val canvasH = with(density) { 460.dp.toPx() }

                // ── Theme-aware watermark backdrop (random per board) ──
                CurioMoodBoardBackdrop(
                    seed = boardSeed,
                    accent = category.themedAccent(),
                    modifier = Modifier.fillMaxSize()
                )

                // v7.18 — the inline card is a MIDDLE-BAND CROP of the FULL
                // collage: the board is width-fit (the SAME arrangement the
                // expanded dialog shows — portrait collages there are also
                // width-driven), then the card's clip trims to the center
                // band, so art pinned near the top or bottom of the expanded
                // board stays hidden in the small view (the cropper look).
                // The old cover-fit re-scaled the whole collage to squeeze
                // into the card, shifting every tile's size/position — the
                // "arrangements look different" bug.
                // maxOfOrNull: legacy GalleryWall entries store only
                // imageCount (no tileLayouts) — an empty-list maxOf would
                // crash the detail screen before the isNotEmpty() guard.
                val safeTileLayouts = sanitizeTileLayouts(data.tileLayouts.orEmpty())
                val maxX = safeTileLayouts.maxOfOrNull { it.offsetXPx + it.widthPx } ?: 0f
                val maxY = safeTileLayouts.maxOfOrNull { it.offsetYPx + it.heightPx } ?: 0f
                val widthScale = if (maxX > 0f) canvasW / maxX else 1f
                // Edge case for wide/short collages: a width-fit board would
                // shrink to a sliver below the card — height-fit instead so
                // the collage stays presentable (nothing to crop then).
                val boardScale = (if (maxY * widthScale < canvasH * 0.55f && maxY > 0f)
                    canvasH / maxY else widthScale)
                    .takeIf { it.isFinite() && it > 0f }
                    ?.coerceAtMost(32f)
                    ?: 1f
                val scaledTiles = safeTileLayouts.map {
                    CaptureData.TileLayout(
                        uri = it.uri,
                        // v113 — allow band placements (negative raw offsets
                        // from the editor's full-card drag freedom); the ≥0
                        // clamp pinned them to the collage's top-left.
                        offsetXPx = (it.offsetXPx * boardScale).coerceIn(-canvasW * 2f, canvasW * 2f),
                        offsetYPx = (it.offsetYPx * boardScale).coerceIn(-canvasH * 2f, canvasH * 2f),
                        rotationDeg = it.rotationDeg,
                        widthPx = (it.widthPx * boardScale).coerceIn(1f, canvasW * 2f),
                        heightPx = (it.heightPx * boardScale).coerceIn(1f, canvasH * 2f)
                    )
                }
                val boardW = (scaledTiles.maxOfOrNull { it.offsetXPx + it.widthPx } ?: 0f)
                    .coerceAtMost(canvasW * 4f)
                val boardH = (scaledTiles.maxOfOrNull { it.offsetYPx + it.heightPx } ?: 0f)
                    .coerceAtMost(canvasH * 4f)

                if (sanitizeTileLayouts(data.tileLayouts.orEmpty()).isNotEmpty()) {
                    // ── Edit button — reopen this board in the editor ──────
                    Surface(
                        onClick = { navController.navigate(CurioRoutes.editMoodBoard(entry.id)) { launchSingleTop = true } },
                        shape = RoundedCornerShape(50),
                        color = category.categorySurface(MaterialTheme.colorScheme.surface).copy(alpha = 0.9f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(36.dp)
                            .zIndex(999f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CurioIcon(
                                name = CurioIcons.Edit,
                                contentDescription = "Edit mood board",
                                tint = MaterialTheme.colorScheme.onSurface,
                                size = 18.dp
                            )
                        }
                    }

                    // ── Expand button — full-screen collage ──────────────
                    Surface(
                        onClick = { boardExpanded = true },
                        shape = RoundedCornerShape(50),
                        color = category.categorySurface(MaterialTheme.colorScheme.surface).copy(alpha = 0.9f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .zIndex(999f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CurioIcon(
                                name = CurioIcons.Fullscreen,
                                contentDescription = "Expand mood board",
                                tint = MaterialTheme.colorScheme.onSurface,
                                size = 18.dp
                            )
                        }
                    }

                    // Inline (non-expanded) board: double-tap a tile to
                    // magnify it centered + straight (same gesture as the
                    // editor). Board-level pinch zoom is only enabled in the
                    // expanded full-screen dialog, so a stray two-finger
                    // pinch on the inline card can't hijack the page scroll.
                    // Cover-scaled + clipped to the card: the board's Surface
                    // doesn't clip overflowing children, so the explicit
                    // clip makes the crop clean (tiles past the card's edges
                    // stay hidden instead of spilling onto the page).
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                            .offset {
                                IntOffset(
                                    ((canvasW - boardW) / 2f).roundToInt(),
                                    ((canvasH - boardH) / 2f).roundToInt()
                                )
                            }
                    ) {
                        MoodBoardTiles(
                            tiles = scaledTiles,
                            canvasWPx = boardW,
                            canvasHPx = boardH,
                            onTileZoom = { uri, tx, ty, w, h, vw, vh ->
                                // v7.24 — only the tapped IMAGE zooms; the
                                // board stays still. The tile's resting spot
                                // is its position in VIEWPORT coords (the
                                // board is offset within the card by the
                                // center-crop), and the image glides from
                                // there to the card's center.
                                val boardOffsetX = (canvasW - boardW) / 2f
                                val boardOffsetY = (canvasH - boardH) / 2f
                                zoomState.zoomIn(
                                    uri = uri,
                                    centerX = boardOffsetX + tx + w / 2f,
                                    centerY = boardOffsetY + ty + h / 2f,
                                    tileW = w,
                                    tileH = h,
                                    viewW = canvasW,
                                    viewH = canvasH
                                )
                            }
                        )

                        // ── Floating quote cards (v7.20) — the board's paper
                        // notes float ON the saved collage too, at the exact
                        // spots they were dragged to in the editor (scaled
                        // with the same fit the tiles use). Never-dragged
                        // cards fall back to their deterministic slot. The
                        // Box above is offset by the center-crop, so the
                        // cards live in the same board space as the tiles.
                        // v7.22 — only ON-board cards float here; below-board
                        // ones render under the board in their own section.
                        MoodBoardFloatingCards(
                            quotes = data.quotes.orEmpty(),
                            styles = data.quoteStyles.orEmpty(),
                            colors = data.quoteColors.orEmpty(),
                            tilts = data.quoteTilts.orEmpty(),
                            positions = data.quotePositions.orEmpty(),
                            onBoard = data.quoteOnBoard.orEmpty(),
                            canvasWPx = boardW,
                            canvasHPx = boardH,
                            boardScale = boardScale,
                            // v113 — deterministic slots in RAW space.
                            boardMaxX = maxX,
                            boardMaxY = maxY,
                            // v7.37 — stable per-entry seed so the on-board
                            // paper slips never re-roll their tears.
                            seed = noteSeed(entry.id, 60)
                        )
                    }
                } else {
                    // Fallback: show images in a grid if no tile data
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CurioIcon(CurioIcons.Image, null, tint = category.categoryInk().copy(alpha = 0.3f), size = 48.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${data.imageCount} image${if (data.imageCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (AppPreferences.tintWashEffective()) category.categoryInk()
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Single-image zoom (v7.24) — only the tapped image
                // glides up and zooms; the board around it stays still.
                // Same fit-scaled geometry so what you magnify matches the
                // card.
                scaledTiles.firstOrNull { it.uri == zoomState.zoomedUri }?.let { zoomTile ->
                    // Clip the gliding image to the card's rounded corners —
                    // the board Surface doesn't clip children, so without
                    // this a tile near the edge bleeds past the card onto
                    // the page while it glides to the center.
                    MoodBoardZoomOverlay(
                        zoomState = zoomState,
                        tileUri = zoomTile.uri,
                        tileX = (canvasW - boardW) / 2f + zoomTile.offsetXPx,
                        tileY = (canvasH - boardH) / 2f + zoomTile.offsetYPx,
                        widthPx = zoomTile.widthPx,
                        heightPx = zoomTile.heightPx,
                        viewW = canvasW,
                        viewH = canvasH,
                        modifier = Modifier.clip(RoundedCornerShape(28.dp))
                    )
                }
            }
        }

        // v7.23 — export the FULL board as a high-res PNG: save to the
        // gallery or share via the system sheet. Both render the same
        // complete picture (surface + watermark + collage + floating quotes
        // + caption + below-board quotes) off-screen at export resolution,
        // so the saved image stays sharp even zoomed in.
        val exportAuthority = remember(entry.id) {
            "${context.packageName}.fileprovider"
        }
        MoodBoardExportActions(
            data = data,
            category = category,
            boardSeed = boardSeed,
            authority = exportAuthority,
            entryId = entry.id
        )

        if (!data.caption.isNullOrBlank()) {
            // Caption wears the same note-paper slip as the editor's field.
            val captionSheet = data.captionColor ?: NotePaperColor.CREAM
            NotePaperCard(
                style = data.captionStyle ?: data.notePaperStyle(),
                seed = noteSeed(entry.id, 5),
                paperColor = captionSheet,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                ruleSpacing = SavedNoteRuleSpacing,
                tailSpace = SavedNoteTailSpace,
                minHeight = 120.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    data.caption,
                    style = savedNoteStyle(),
                    color = notePaperInk(captionSheet)
                )
            }
        }

        // v7.22 — the ON-board quote cards float on the collage above; the
        // BELOW-board cards (added via the bottom Add-quote button) render
        // here as separate quote boxes under the board. Legacy entries lack
        // the flag → all cards are on-board → nothing renders below.
        val quoteOnBoardFlags = data.quoteOnBoard.orEmpty()
        val hasBelowBoard = data.quotes.orEmpty().indices.any {
            quoteOnBoardFlags.getOrElse(it) { true } == false
        }
        if (hasBelowBoard) {
            RenderQuoteCards(
                quotes = data.quotes.orEmpty(),
                spans = data.quoteSpans.orEmpty(),
                tilts = data.quoteTilts.orEmpty(),
                styles = data.quoteStyles.orEmpty(),
                colors = data.quoteColors.orEmpty(),
                fallbackStyle = data.notePaperStyle(),
                entryId = entry.id,
                topicName = entry.topic.name,
                category = category,
                label = "Quote boxes",
                includeIndex = { i -> quoteOnBoardFlags.getOrElse(i) { true } == false }
            )
        }

        if (boardExpanded) {
            ExpandedMoodBoardDialog(
                data = data,
                seed = boardSeed,
                accent = category.themedAccent(),
                // The expanded board rests on the SAME tinted surface as the
                // inline card — never the page wash.
                boardSurface = category.categorySurfaceMoodBoard(),
                onDismiss = { boardExpanded = false },
                onEdit = {
                    navController.navigate(CurioRoutes.editMoodBoard(entry.id)) { launchSingleTop = true }
                }
            )
        }
    }
}

/**
 * Full-screen expanded mood board — scales the tile collage up to fill the
 * screen, centers it, and keeps per-tile tap → Lightbox. Close button
 * top-right; back/outside tap dismisses. Rests on the same theme-aware
 * watermark backdrop as the inline board (seeded from the entry id) and on
 * the same tinted surface ([categorySurfaceMoodBoard]) so the full-screen
 * board keeps its category tint.
 */
@Composable
private fun ExpandedMoodBoardDialog(
    data: CaptureData.GalleryWall,
    seed: Int,
    accent: Color,
    boardSurface: Color,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current
    // In-place tile zoom inside the expanded board — pinch/tap, no Lightbox.
    // v7.19 — offsets animate inside the canvas; double-tap glides the
    // whole board (with its backdrop) to the tile, then the image pops.
    val zoomState = rememberMoodBoardZoomState()
    Dialog(
        onDismissRequest = onDismiss,
        // True full screen: the dialog draws behind the system bars so the
        // collage fills the whole display instead of floating like a dialog
        // page. The controls below pad for the bars.
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CurioDialogEntrance(scale = 1f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(boardSurface)
        ) {
            // ── Theme-aware watermark backdrop (matches inline board) ──
            CurioMoodBoardBackdrop(
                seed = seed,
                accent = accent,
                modifier = Modifier.fillMaxSize()
            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val dialogW = with(density) { maxWidth.toPx() }
                val dialogH = with(density) { maxHeight.toPx() }

                // v57 — the expanded dialog shows the FULL-SCREEN arrangement
                // (saved separately from the inline one the small card
                // renders). Legacy entries have no full layout → fall back to
                // the inline one so old boards keep their single arrangement.
                // v60 — .ifEmpty on a Gson-decoded list NPEs when the field
                // decoded to null (Gson bypasses Kotlin defaults), crashing
                // the expanded board's first measure — null-safe .orEmpty()
                // first, then the empty fallback.
                val expandedLayouts = sanitizeTileLayouts(
                    data.tileLayoutsFull.orEmpty().ifEmpty { data.tileLayouts.orEmpty() }
                )
                val expandedQuotePositions = data.quotePositionsFull.orEmpty().ifEmpty { data.quotePositions.orEmpty() }

                if (expandedLayouts.isNotEmpty()) {
                    // Fit the collage to the dialog with the SAME shared
                    // [fitTileLayout] the inline card uses — bounds →
                    // uniform scale → centered — so the full-screen board
                    // always matches the small card. Pinch on the board
                    // magnifies it; double-tap a tile magnifies the tile
                    // centered + straight.
                    val fit = fitTileLayout(expandedLayouts, dialogW, dialogH)
                    val scaledTiles = fit.scaledTiles
                    val boardW = fit.boardW
                    val boardH = fit.boardH
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    ((dialogW - boardW) / 2f).roundToInt(),
                                    ((dialogH - boardH) / 2f).roundToInt()
                                )
                            }
                    ) {
                        MoodBoardTiles(
                            tiles = scaledTiles,
                            canvasWPx = boardW,
                            canvasHPx = boardH,
                            onTileZoom = { uri, tx, ty, w, h, vw, vh ->
                                // v7.24 — only the tapped IMAGE zooms; the
                                // board stays still. Resting spot = the
                                // tile's VIEWPORT position (board centered
                                // in the dialog by the contain-fit).
                                val boardOffsetX = (dialogW - boardW) / 2f
                                val boardOffsetY = (dialogH - boardH) / 2f
                                zoomState.zoomIn(
                                    uri = uri,
                                    centerX = boardOffsetX + tx + w / 2f,
                                    centerY = boardOffsetY + ty + h / 2f,
                                    tileW = w,
                                    tileH = h,
                                    viewW = dialogW,
                                    viewH = dialogH
                                )
                            }
                        )

                        // ── Floating quote cards (v7.20) — same layer as the
                        // inline card: saved positions scaled by the fit, in
                        // the already-centered Box's board space. v7.22 — only
                        // on-board cards float; below-board ones stay under the
                        // board (never inside the expanded dialog).
                        MoodBoardFloatingCards(
                            quotes = data.quotes.orEmpty(),
                            styles = data.quoteStyles.orEmpty(),
                            colors = data.quoteColors.orEmpty(),
                            tilts = data.quoteTilts.orEmpty(),
                            positions = expandedQuotePositions,
                            onBoard = data.quoteOnBoard.orEmpty(),
                            canvasWPx = boardW,
                            canvasHPx = boardH,
                            boardScale = fit.scale,
                            // v113 — deterministic slots in RAW space (the
                            // scaled board ÷ the fit scale back to raw).
                            boardMaxX = boardW / fit.scale,
                            boardMaxY = boardH / fit.scale,
                            // v145 — the expanded dialog is the full-screen
                            // view: cards keep their exact raw width × the
                            // board fit (same as the full-screen editor), so
                            // what you arranged in full-screen is what shows
                            // here — the old 40% display cap shrank resized
                            // cards against the inline board's look.
                            rawSpace = true,
                            // v7.37 — stable per-entry seed so the on-board
                            // paper slips never re-roll their tears. The
                            // dialog carries its entry-derived [seed] (no
                            // CurioEntry is in scope here) — salt it (60) so
                            // the slips stay distinct from the board's own
                            // tear, matching the inline card's split.
                            seed = noteSeed(seed.toString(), 60)
                        )
                    }

                    // ── Single-image zoom (v7.24) — only the tapped image
                    // glides up and zooms; the board stays still. ─────────
                    scaledTiles.firstOrNull { it.uri == zoomState.zoomedUri }?.let { zoomTile ->
                        MoodBoardZoomOverlay(
                            zoomState = zoomState,
                            tileUri = zoomTile.uri,
                            tileX = (dialogW - boardW) / 2f + zoomTile.offsetXPx,
                            tileY = (dialogH - boardH) / 2f + zoomTile.offsetYPx,
                            widthPx = zoomTile.widthPx,
                            heightPx = zoomTile.heightPx,
                            viewW = dialogW,
                            viewH = dialogH
                        )
                    }
                }

                // ── Edit button — reopen this board in the editor ─────────
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CurioIcon(
                            name = CurioIcons.Edit,
                            contentDescription = "Edit mood board",
                            tint = Color.White,
                            size = 22.dp
                        )
                    }
                }

                // ── Close button ─────────────────────────────────────────
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CurioIcon(
                            name = CurioIcons.Close,
                            contentDescription = "Close expanded mood board",
                            tint = Color.White,
                            size = 22.dp
                        )
                    }
                }

                // ── Hint ─────────────────────────────────────────────────
                Text(
                    text = "Double-tap a tile to zoom · pinch to magnify",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                )
            }
        }
        }
    }
}

@Composable
private fun FieldNotesRender(entry: CurioEntry, category: CurioCategory, navController: NavController) {
    val data = entry.captureData as? CaptureData.FieldNotes ?: return
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        data.fieldMindMetadata?.let { metadata ->
            FieldMindMetadataCard(metadata = metadata, category = category)
        }
        data.observed.takeIf { it.isNotBlank() }?.let { text ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Observed", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = category.categoryInk())
                val observedSheet = data.observedColor ?: NotePaperColor.CREAM
                NotePaperCard(
                    style = data.observedStyle ?: data.notePaperStyle(),
                    seed = noteSeed(entry.id, 6),
                    paperColor = observedSheet,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                    ruleSpacing = SavedNoteRuleSpacing,
                    tailSpace = SavedNoteTailSpace,
                    minHeight = 120.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        buildRichAnnotated(text, data.observedSpans.orEmpty(), notePaperHighlight(observedSheet)),
                        style = savedNoteStyle(),
                        color = notePaperInk(observedSheet)
                    )
                }
            }
        }
        data.surprised.takeIf { it.isNotBlank() }?.let { text ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Surprised me", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = category.categoryInk())
                val surprisedSheet = data.surprisedColor ?: NotePaperColor.CREAM
                NotePaperCard(
                    style = data.surprisedStyle ?: data.notePaperStyle(),
                    seed = noteSeed(entry.id, 7),
                    paperColor = surprisedSheet,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                    ruleSpacing = SavedNoteRuleSpacing,
                    tailSpace = SavedNoteTailSpace,
                    minHeight = 120.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        buildRichAnnotated(text, data.surprisedSpans.orEmpty(), notePaperHighlight(surprisedSheet)),
                        style = savedNoteStyle(),
                        color = notePaperInk(surprisedSheet)
                    )
                }
            }
        }
        data.learnNext.takeIf { it.isNotBlank() }?.let { text ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Want to learn next", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = category.categoryInk())
                val learnNextSheet = data.learnNextColor ?: NotePaperColor.CREAM
                NotePaperCard(
                    style = data.learnNextStyle ?: data.notePaperStyle(),
                    seed = noteSeed(entry.id, 8),
                    paperColor = learnNextSheet,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                    ruleSpacing = SavedNoteRuleSpacing,
                    tailSpace = SavedNoteTailSpace,
                    minHeight = 120.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        buildRichAnnotated(text, data.learnNextSpans.orEmpty(), notePaperHighlight(learnNextSheet)),
                        style = savedNoteStyle(),
                        color = notePaperInk(learnNextSheet)
                    )
                }
            }
        }
        if (data.imageUris.isNotEmpty()) {
            // v7.36 — adaptive aspect-ratio gallery with in-place zoom (no
            // Lightbox page, no dark scrim), showing ALL images.
            AdaptiveImageGallery(
                uris = data.imageUris,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun structuredDetailRows(raw: String): List<Pair<String, String>> {
    if (raw.isBlank()) return emptyList()
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
    val rows = mutableListOf<Pair<String, String>>()

    fun titleize(key: String): String = key
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    fun flatten(value: Any?, path: String) {
        when (value) {
            is JSONObject -> {
                value.keys().forEach { key ->
                    val childPath = if (path.isBlank()) titleize(key) else "$path · ${titleize(key)}"
                    flatten(value.opt(key), childPath)
                }
            }
            is JSONArray -> {
                val values = (0 until value.length()).mapNotNull { index -> value.opt(index) }
                if (values.isNotEmpty()) rows += path to values.joinToString(", ") { it.toString() }
            }
            null, JSONObject.NULL -> Unit
            else -> {
                val text = value.toString().trim()
                if (text.isNotBlank()) rows += path to text
            }
        }
    }

    flatten(root, "")
    return rows
}

@Composable
private fun formatMetadataTimestamp(millis: Long): String {
    // Lint (NonObservableLocale): read the locale through Compose's observable
    // state so the timestamp re-formats when the user changes the system locale.
    val locale = LocalLocale.current.platformLocale
    return SimpleDateFormat("MMM d, yyyy · h:mm a", locale).format(Date(millis))
}

@Composable
private fun FieldMindMetadataCard(metadata: FieldMindMetadata, category: CurioCategory) {
    val species = metadata.species
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = category.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurioIcon(CurioIcons.ScienceGlyph, null, tint = category.themedAccent(), size = 22.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("FieldMind metadata", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        metadata.recordType.orEmpty().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (AppPreferences.tintWashEffective()) category.categoryInk()
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val rows = buildList {
                fun addRow(label: String, value: String?) { if (!value.isNullOrBlank()) add(label to value) }
                addRow("Category", metadata.category)
                addRow("Confidence", metadata.confidence)
                addRow("Date", listOf(metadata.date.orEmpty(), metadata.time.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "))
                addRow("Location", metadata.location)
                if (metadata.latitude != null && metadata.longitude != null) addRow("Coordinates", "${metadata.latitude}, ${metadata.longitude}")
                addRow("Weather", listOf(metadata.weather.orEmpty(), metadata.weatherCondition.orEmpty()).filter { it.isNotBlank() }.distinct().joinToString(" · "))
                metadata.weatherTemperature?.let { addRow("Temperature", "${it}°") }
                metadata.humidity?.let { addRow("Humidity", "$it%") }
                metadata.windSpeed?.let { addRow("Wind", it.toString()) }
                metadata.cloudCover?.let { addRow("Cloud cover", "$it%") }
                metadata.pressure?.let { addRow("Pressure", it.toString()) }
                metadata.durationMs?.let { addRow("Duration", formatElapsed(it)) }
                metadata.startedAt?.let { addRow("Started", formatMetadataTimestamp(it)) }
                metadata.endedAt?.let { addRow("Ended", formatMetadataTimestamp(it)) }
                metadata.changeObservedAt?.let { addRow("Change observed", formatMetadataTimestamp(it)) }
                metadata.changeDurationMs?.let { addRow("Change duration", formatElapsed(it)) }
                metadata.weatherSnapshotAt?.let { addRow("Weather snapshot", formatMetadataTimestamp(it)) }
                addRow("Status", metadata.status)
                metadata.projectId?.let { addRow("Project", it.toString()) }
                metadata.sourceId?.let { addRow("Source", it.toString()) }
                metadata.parentObservationId?.let { addRow("Parent observation", it.toString()) }
                metadata.followUpScheduledAt?.let { addRow("Follow-up", formatMetadataTimestamp(it)) }
                metadata.archivedAt?.let { addRow("Archived", formatMetadataTimestamp(it)) }
                metadata.deletedAt?.let { addRow("Deleted", formatMetadataTimestamp(it)) }
                metadata.createdAt?.let { addRow("Created", formatMetadataTimestamp(it)) }
                metadata.updatedAt?.let { addRow("Updated", formatMetadataTimestamp(it)) }
                metadata.archivedAt?.let { addRow("Archived", formatMetadataTimestamp(it)) }
                metadata.deletedAt?.let { addRow("Deleted", formatMetadataTimestamp(it)) }
                metadata.createdAt?.let { addRow("Created", formatMetadataTimestamp(it)) }
                metadata.updatedAt?.let { addRow("Updated", formatMetadataTimestamp(it)) }
                metadata.qualityScore?.let { addRow("Quality", it.toString()) }
                addRow("Time note", metadata.timeNote)
            }
            rows.forEach { (label, value) ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = category.categoryInk(), modifier = Modifier.width(92.dp))
                    Text(
                        value,
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (!metadata.tags.isNullOrEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    metadata.tags.orEmpty().forEach { tag ->
                        Surface(shape = RoundedCornerShape(50), color = category.themedAccent().copy(alpha = 0.12f)) {
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = category.categoryInk(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            val structuredRows = structuredDetailRows(metadata.structuredDetailsJson)
            if (structuredRows.isNotEmpty()) {
                Text(
                    "Structured details",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = category.categoryInk()
                )
                structuredRows.forEach { (label, value) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        // v27n — opaque row (was 72% alpha, which let the
                        // elevation shadow bleed through).
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = category.categoryInk(),
                                modifier = Modifier.width(112.dp)
                            )
                            Text(
                                value,
                                style = MaterialTheme.typography.bodySmall,
                                softWrap = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            species?.let { item ->
                Surface(shape = RoundedCornerShape(16.dp), color = category.themedAccent().copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurioIcon(CurioIcons.ScienceGlyph, null, tint = category.themedAccent(), size = 18.dp)
                            Text("Species", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Text(item.commonName.orEmpty().ifBlank { item.scientificName.orEmpty().ifBlank { "Unknown species" } }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        if (!item.scientificName.isNullOrBlank()) {
                            Text(
                                item.scientificName.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = if (AppPreferences.tintWashEffective()) category.categoryInk()
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val taxonomy = listOf(
                            "Kingdom" to item.kingdom,
                            "Phylum" to item.phylum,
                            "Class" to item.className,
                            "Order" to item.order,
                            "Family" to item.family,
                            "Genus" to item.genus,
                            "Species" to item.species,
                            "Life stage" to item.lifeStage,
                            "Sex" to item.sex,
                            "Conservation" to item.conservationStatus
                        ).filter { it.second.isNotBlank() }
                        taxonomy.forEach { (label, value) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = category.categoryInk(), modifier = Modifier.width(92.dp))
                                    Text(
                        value,
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = true,
                        modifier = Modifier.weight(1f)
                    )
                                }
                            }
                        }
                        item.observationCount?.let { Text("Recorded observations · $it", style = MaterialTheme.typography.labelSmall) }
                        if (!item.notes.isNullOrBlank()) Text(item.notes.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenNotebookRender(entry: CurioEntry, category: CurioCategory, navController: NavController) {
    val data = entry.captureData as? CaptureData.OpenNotebook ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Format: ${data.subFormat.name}",
            style = MaterialTheme.typography.labelMedium,
            color = if (AppPreferences.tintWashEffective()) category.categoryInk()
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Recursively render the sub-data
        val subEntry = CurioEntry(
            id = entry.id,
            topic = entry.topic,
            format = data.subFormat,
            captureData = data.subData
        )
        // Synthesized sub-entry shares the parent entry's id; rendered
        // read-only (no save-affecting actions on a sub-entry).
        FormatBody(entry = subEntry, category = category, navController = navController)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Share Card — rendered off-screen, captured as PNG, shared via Intent.ACTION_SEND
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Self-contained share card composable designed for bitmap capture.
 *
 * Rendered off-screen by [shareComposableCard] at 400×400 dp, captured
 * as a PNG, and shared via [Intent.ACTION_SEND] + [FileProvider].
 *
 * Layout (top to bottom):
 *   - Category gradient background (full-bleed, rounded corners)
 *   - Large category icon watermark (centered, low alpha)
 *   - Topic name (large, bold, white)
 *   - Category chip
 *   - Teaser text (3 lines max)
 *   - Format badge
 *   - "Curio ✦" branding footer
 */
/**
 * v170 — the share card is a 3:4 PORTRAIT now (was square 1:1) and it
 * carries the sharer's own touches: their display name, the session note
 * they added, and the first attached photo (a session screenshot) when
 * one exists — a richer, more personal card. The photo decodes
 * SYNCHRONOUSLY ([BitmapFactory]) so BOTH the live sheet preview and the
 * off-screen captured PNG show it (Coil's async painter would miss the
 * single-frame capture in [com.curio.app.ui.components.shareComposableCard]).
 */
@Composable
private fun CurioShareCard(
    entry: CurioEntry,
    category: CurioCategory
) {
    val bgColor = category.themedAccent().copy(alpha = 0.9f)
    // v7.5 — pastel mode lightens the card fill, so the share card content
    // flips from white to a deep ink of the accent. White when pastel mode
    // is off, preserving the exact pre-pastel share card.
    val ink = pastelFillInk(category.themedAccent())
    val context = LocalContext.current

    // v22 — the explore-session duration joins the captured-date line when
    // one was recorded ("Captured today · explored 12m"), matching the
    // detail hero's frosted-date language (same datePart + sessionPart style).
    val sessionPart = if (entry.sessionTimeMillis > 0L) {
        " · explored ${formatSessionShort(entry.sessionTimeMillis)}"
    } else {
        ""
    }
    val daysAgoText = when (entry.capturedAtDaysAgo) {
        0 -> "Captured today"
        1 -> "Captured yesterday"
        else -> "Captured ${entry.capturedAtDaysAgo}d ago"
    } + sessionPart

    // v170 — the sharer's display name (falls back to the default).
    val displayName = remember(entry.id) {
        AppPreferences.getDisplayName(context).ifBlank { "Curious Explorer" }
    }
    // v170 — the first attached photo, decoded synchronously (see above).
    val photo = remember(entry.id) {
        entry.sessionScreenshots.firstOrNull()?.let { path ->
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }
    val note = entry.sessionNote?.trim().orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor, RoundedCornerShape(28.dp))
    ) {
        // ── Watermark icon ────────────────────────────────────────────
        CurioIcon(
            name = category.iconGlyph,
            contentDescription = null,
            tint = ink.copy(alpha = 0.08f),
            size = 200.dp,
            modifier = Modifier.align(Alignment.Center)
        )

        // ── Content ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: category chip + sparkle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ink.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurioIcon(
                            name = category.iconGlyph,
                            contentDescription = null,
                            tint = ink,
                            size = 14.dp
                        )
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ink
                        )
                    }
                }
                CurioIcon(
                    name = CurioIcons.AutoAwesome,
                    contentDescription = null,
                    tint = ink.copy(alpha = 0.5f),
                    size = 20.dp
                )
            }

            // Middle: photo (optional) + topic name + teaser + note
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // v170 — the user's photo, a rounded block on the card.
                if (photo != null) {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(150.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ink.copy(alpha = 0.14f))
                    ) {
                        Image(
                            bitmap = photo,
                            contentDescription = "Attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Text(
                    text = entry.topic.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = ink,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.topic.teaser,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ink.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Compact meta line: format · captured date (v170 — chips
                // became one quiet text line to declutter the portrait card).
                Text(
                    text = "${entry.format.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")} · $daysAgoText",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                // v170 — the note the user added.
                if (note.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ink.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.Note,
                                contentDescription = null,
                                tint = ink.copy(alpha = 0.7f),
                                size = 16.dp
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ink,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom: the sharer's name + branding
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = ink.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurioIcon(
                        name = CurioIcons.AutoAwesome,
                        contentDescription = null,
                        tint = ink.copy(alpha = 0.6f),
                        size = 16.dp
                    )
                    Text(
                        text = "Curio",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = ink.copy(alpha = 0.9f)
                    )
                }
                Text(
                    text = "Stay curious",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Share sheet (v149) — preview + Image/Text picker before sharing
// ═══════════════════════════════════════════════════════════════════════════

/**
 * v149 — the NEW share UI for saved entries: a bottom sheet that shows a
 * live PREVIEW of the exact card that gets shared, an Image card / Text
 * format picker, and one Share action. Image renders the 400×400 category
 * card PNG via [shareComposableCard] (same output as before); Text sends a
 * plain-text summary. Opened from the entry's More menu (the old flow fired
 * the chooser straight from the menu with no preview).
 */
@Composable
private fun EntryShareSheet(
    entry: CurioEntry,
    category: CurioCategory,
    context: Context,
    authority: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var shareAsImage by rememberSaveable { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Share this entry",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // ── Preview — the exact card that gets shared, on a soft stage ──
            // v170 — 3:4 portrait to match the exported card (was square).
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .aspectRatio(3f / 4f)
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
            ) {
                CurioShareCard(entry = entry, category = category)
            }

            // ── Image / Text format picker ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShareFormatPill(
                    label = "Image card",
                    icon = CurioIcons.Image,
                    selected = shareAsImage
                ) { shareAsImage = true }
                ShareFormatPill(
                    label = "Text",
                    icon = CurioIcons.FormatText,
                    selected = !shareAsImage
                ) { shareAsImage = false }
            }

            // ── Share action ──
            Button(
                onClick = {
                    if (shareAsImage) {
                        shareComposableCard(
                            context = context,
                            // v170 — 3:4 portrait (was 400×400 square).
                            cardSize = DpSize(450.dp, 600.dp),
                            authority = authority,
                            card = { CurioShareCard(entry = entry, category = category) }
                        )
                    } else {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, entry.topic.name)
                            putExtra(Intent.EXTRA_TEXT, entryShareText(entry, category))
                        }
                        context.startActivity(Intent.createChooser(intent, "Share entry"))
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (shareAsImage) "Share image card" else "Share as text",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

/** One Image/Text pill in the share sheet — selected wears the solid
 *  secondary fill (v131 contract), unselected sits on the container. */
@Composable
private fun ShareFormatPill(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            CurioIcon(
                name = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onSecondary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Plain-text share payload for an entry (the sheet's Text format). */
private fun entryShareText(entry: CurioEntry, category: CurioCategory): String {
    val formatLabel = entry.format.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
    val daysAgo = when (entry.capturedAtDaysAgo) {
        0 -> "today"
        1 -> "yesterday"
        else -> "${entry.capturedAtDaysAgo}d ago"
    }
    return buildString {
        append(entry.topic.name).append('\n')
        append(entry.topic.teaser).append("\n\n")
        append(category.displayName).append(" · ").append(formatLabel).append(" · captured ").append(daysAgo)
        append("\n\nvia Curio — Stay curious")
    }
}
