package com.curio.app.features.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcons
import kotlin.math.roundToInt

/**
 * v434 — THE READING SETTINGS, AS A PAGE OF ITS OWN.
 *
 * The member asked for it in their own words — "settings gets its own screen" —
 * and asked for the one thing that makes it belong here rather than in the app's
 * settings family: *"dont use settings style use the reader style ui for it"*. So
 * it is a full screen of the READER's own paper, its own ink, its own type and its
 * own capsules: a page of the book's world that happens to be about the book.
 *
 * Reachable two ways, and it is the SAME composable both times:
 *
 *  · from the reader's ⋯ menu, where it opens OVER the book so back puts the
 *    member on the page they were reading (see the reader body), and
 *  · as a destination of its own for the settings side — wired on the Dev page.
 *
 * ── v434 — IT IS THE APPEARANCE SHEET'S COMPLETE TWIN NOW ───────────────
 *
 * The two shared one vocabulary but not one voice: the sheet used `Switch`es and
 * the page used full-width pill rows. Both now use the SAME components — the
 * animated [ReaderSegmentRow] for every either/or (or three-way) choice, the
 * [ReaderSliderRow] for every size, the capsule swatches for the paper — so a
 * setting learned in one place is the same shape in the other (member: "use a
 * similar design system to samsung … proper visual consistency also extend this
 * to settings").
 *
 * WHAT IS HERE, AND WHY: the sheet is the quick door (the few things a reader
 * changes while reading); this is the COMPLETE one — every reader preference in
 * one scroll, including the ones the sheet has no room for, the three-way
 * orientation, the page's own gestures, and the settings the member asked for by
 * name (line spacing, margins, paragraph spacing, alignment, keeping the screen
 * awake, and the night dim).
 */
@Composable
internal fun ReaderSettingsScreen(
    palette: ReaderPalette,
    /** Whether a type size and a face mean anything for what is open. */
    showType: Boolean,
    /** v434 — whether a PDF is open, for the ZOOM row it needs instead. */
    showZoom: Boolean = false,
    /** Whether the reading is being read as PAGES right now. */
    paged: Boolean,
    onTogglePaged: () -> Unit,
    /** False when no book is open, so there are no zones to place. */
    canPlaceZones: Boolean = false,
    onGestures: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var moreInks by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
            // v438 — the READER's own top floor, not the (hidden) status bar: this
            // page is drawn inside the reader, which hides the bar, so
            // `statusBarsPadding()` collapsed to zero and the head sat on the
            // glass (member: "in settings the header is again over the status
            // bar"). See [readerChromeTopInset].
            .readerChromeTopInset()
    ) {
        // ── THE HEAD, IN THE READER'S OWN PILL ──────────────────────────
        Surface(
            shape = RoundedCornerShape(50),
            color = palette.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReaderChromeButton(CurioIcons.ArrowBack, "Back to the book", palette) { onBack() }
                Text(
                    "Reading",
                    style = TextStyle(
                        fontFamily = readerTypeFamily(ReaderLook.typeFace),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.ink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── THE PAPER ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Page", palette)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ReaderSkin.primary.forEach { skin ->
                        ReaderInkSwatch(skin, palette, Modifier.weight(1f))
                    }
                    ReaderMoreInkTile(palette, open = moreInks, modifier = Modifier.weight(0.8f)) {
                        moreInks = !moreInks
                    }
                }
                if (moreInks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        ReaderSkin.extra.forEach { skin ->
                            ReaderInkSwatch(skin, palette, Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── THE TYPE, or the PDF's zoom ─────────────────────────────
            if (showType) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Type", palette)
                    ReaderSliderRow(
                        label = "Text size",
                        value = ReaderLook.textScale,
                        range = 0.8f..2.6f,
                        step = 0.08f,
                        valueLabel = "${(ReaderLook.textScale * 100f).roundToInt()}%",
                        palette = palette,
                        onValue = { next -> ReaderLook.textScale = next.coerceIn(0.8f, 2.6f) },
                        leadingGlyph = CurioIcons.TextDecrease,
                        leadingLabel = "Smaller type",
                        trailingGlyph = CurioIcons.TextIncrease,
                        trailingLabel = "Larger type"
                    )
                    ReaderSegmentRow(
                        segments = ReaderTypeFace.entries.map { ReaderSegment(it.label) },
                        selectedIndex = ReaderTypeFace.entries.indexOf(
                            ReaderTypeFace.of(ReaderLook.typeFace)
                        ),
                        palette = palette,
                        onSelect = { at ->
                            ReaderTypeFace.entries.getOrNull(at)?.let { ReaderLook.typeFace = it.key }
                        }
                    )
                }
            } else if (showZoom) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Zoom", palette)
                    ReaderSliderRow(
                        label = "Page zoom",
                        value = ReaderLook.pdfZoom,
                        range = 1f..4f,
                        step = 0.25f,
                        valueLabel = "${(ReaderLook.pdfZoom * 100f).roundToInt()}%",
                        palette = palette,
                        onValue = { next -> ReaderLook.pdfZoom = next.coerceIn(1f, 4f) },
                        leadingGlyph = CurioIcons.Remove,
                        leadingLabel = "Zoom out",
                        trailingGlyph = CurioIcons.Add,
                        trailingLabel = "Zoom in"
                    )
                }
            }

            // ── HOW THE BOOK IS LAID OUT ────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Reading mode", palette)
                ReaderSegmentRow(
                    segments = ReaderFlow.entries.map { ReaderSegment(it.label, it.modeGlyph()) },
                    selectedIndex = if (paged) 1 else 0,
                    palette = palette,
                    onSelect = { if ((it == 1) != paged) onTogglePaged() }
                )

                ReaderSettingsSection("How the page stands", palette)
                ReaderSegmentRow(
                    segments = ReaderOrientation.entries.map {
                        ReaderSegment(it.label, it.orientationGlyph())
                    },
                    selectedIndex = ReaderOrientation.entries.indexOf(ReaderLook.orientation),
                    palette = palette,
                    onSelect = { at ->
                        ReaderOrientation.entries.getOrNull(at)?.let { ReaderLook.orientation = it }
                    }
                )
            }

            // ── THE WORDS' OWN LAYOUT ───────────────────────────────────
            if (showType) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Lines", palette)
                    ReaderSliderRow(
                        label = "Line spacing",
                        value = ReaderLook.lineSpacing,
                        range = 0.85f..1.6f,
                        step = 0.05f,
                        valueLabel = "${(ReaderLook.lineSpacing * 100f).roundToInt()}%",
                        palette = palette,
                        onValue = { next -> ReaderLook.lineSpacing = next.coerceIn(0.85f, 1.6f) },
                        leadingGlyph = CurioIcons.Remove,
                        leadingLabel = "Tighter lines",
                        trailingGlyph = CurioIcons.Add,
                        trailingLabel = "Looser lines"
                    )
                    ReaderSliderRow(
                        label = "Page margins",
                        value = ReaderLook.pageMargin,
                        range = 10f..40f,
                        step = 2f,
                        valueLabel = "${ReaderLook.pageMargin.roundToInt()} dp",
                        palette = palette,
                        onValue = { next -> ReaderLook.pageMargin = next.coerceIn(10f, 40f) },
                        leadingGlyph = CurioIcons.Remove,
                        leadingLabel = "Narrower margins",
                        trailingGlyph = CurioIcons.Add,
                        trailingLabel = "Wider margins"
                    )
                    ReaderSliderRow(
                        label = "Paragraph spacing",
                        value = ReaderLook.paraSpacing,
                        range = 0.6f..2f,
                        step = 0.1f,
                        valueLabel = "${(ReaderLook.paraSpacing * 100f).roundToInt()}%",
                        palette = palette,
                        onValue = { next -> ReaderLook.paraSpacing = next.coerceIn(0.6f, 2f) },
                        leadingGlyph = CurioIcons.Remove,
                        leadingLabel = "Closer paragraphs",
                        trailingGlyph = CurioIcons.Add,
                        trailingLabel = "Further paragraphs"
                    )
                    ReaderAlignRow(palette)
                }
            }

            // ── THE SCREEN ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Screen", palette)
                ReaderSegmentRow(
                    segments = listOf(
                        ReaderSegment("Awake", CurioIcons.Lightbulb),
                        ReaderSegment("Let it sleep", CurioIcons.Bedtime)
                    ),
                    selectedIndex = if (ReaderLook.keepScreenOn) 0 else 1,
                    palette = palette,
                    onSelect = { at -> ReaderLook.keepScreenOn = at == 0 }
                )
                ReaderSliderRow(
                    label = "Night dim",
                    value = ReaderLook.dim,
                    range = 0f..0.6f,
                    step = 0.05f,
                    valueLabel = if (ReaderLook.dim <= 0f) "Off"
                    else "${(ReaderLook.dim / 0.6f * 100f).roundToInt()}%",
                    palette = palette,
                    onValue = { next -> ReaderLook.dim = next.coerceIn(0f, 0.6f) },
                    leadingGlyph = CurioIcons.DarkMode,
                    leadingLabel = "Less dim",
                    trailingGlyph = CurioIcons.Nightlight,
                    trailingLabel = "More dim"
                )
            }

            // ── v439 — WHAT THE READING COSTS ───────────────────────────
            //
            // The member: *"in pdf reader, a high charge save turns on which makes
            // the app cache and background usage very low in reder so the phone
            // doesnt heat"*.
            //
            // It is a two-option row rather than a switch on purpose [see
            // ReaderLook.lowPower]: the choice is not "do a thing or not" but
            // WHICH WAY the reader spends — a cooler page and a shorter cache, or
            // the sharpest page the screen can take with its neighbour already
            // rendered. A switch would have made the second one the odd state.
            //
            // Both segments are glyphs this screen already draws
            // ([CurioIcons.Lightbulb] above), so nothing is promised here that the
            // bundled icon subset might not carry.
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Power", palette)
                ReaderSegmentRow(
                    segments = listOf(
                        ReaderSegment("Cooler", CurioIcons.Lightbulb),
                        ReaderSegment("Sharpest", CurioIcons.AutoAwesome)
                    ),
                    selectedIndex = if (ReaderLook.lowPower) 0 else 1,
                    palette = palette,
                    onSelect = { at -> ReaderLook.lowPower = at == 0 }
                )
            }

            // ── THE PAGE'S OWN GESTURES ─────────────────────────────────
            if (canPlaceZones) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Gestures", palette)
                    ReaderSegmentRow(
                        segments = listOf(
                            ReaderSegment("On", CurioIcons.Check),
                            ReaderSegment("Off", CurioIcons.Close)
                        ),
                        selectedIndex = if (ReaderLook.tapZones) 0 else 1,
                        palette = palette,
                        onSelect = { at -> ReaderLook.tapZones = at == 0 }
                    )
                    // WHERE they sit and WHAT they do, on the page itself: a
                    // gesture is something the member has to see against the words
                    // it governs (see [ReaderTapZoneEditor]).
                    Surface(
                        onClick = { onGestures?.invoke() },
                        shape = RoundedCornerShape(50),
                        color = palette.ink.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(palette.accent, CircleShape)
                            )
                            Text(
                                "Place the gestures",
                                style = TextStyle(
                                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.ink
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

/**
 * THE SAME PAGE WITHOUT A BOOK OPEN — the settings side's door.
 *
 * The member asked for the reader's settings to be reachable from the settings
 * side too, and for now that door is on the Dev page (their own instruction:
 * "wire it in dev exp for now"). Nothing here needs a book: the preferences it
 * changes belong to the PROCESS, not to one novel (see [ReaderLook]).
 */
@Composable
internal fun ReaderSettingsRoute(onBack: () -> Unit) {
    val palette = readerPalette(ReaderLook.inkKey)
    ReaderSettingsScreen(
        palette = palette,
        showType = true,
        paged = ReaderLook.textFlow == ReaderFlow.PAGED,
        onTogglePaged = { ReaderLook.textFlow = ReaderLook.textFlow.flipped() },
        onBack = onBack
    )
}

/** A section's name on the settings page — the reader's own furniture. */
@Composable
private fun ReaderSettingsSection(label: String, palette: ReaderPalette) {
    Text(
        label.uppercase(),
        style = TextStyle(
            fontFamily = readerTypeFamily(ReaderLook.typeFace),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.3.sp,
            color = palette.accent
        )
    )
}
