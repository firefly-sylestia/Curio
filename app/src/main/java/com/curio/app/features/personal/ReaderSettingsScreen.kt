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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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

/**
 * v431 — THE READING SETTINGS, AS A PAGE OF ITS OWN.
 *
 * The member asked for it in their own words — "settings gets its own screen" —
 * and asked for the one thing that makes it belong here rather than in the app's
 * settings family: *"dont use settings style use the reader style ui for it"*. So
 * it is a full screen of the READER's own paper, its own ink, its own type and its
 * own pills: a page of the book's world that happens to be about the book.
 *
 * It is reachable two ways and it is the SAME composable both times:
 *
 *  · from the reader's ⋯ menu, where it opens OVER the book so back puts the
 *    member on the page they were reading (see the reader body), and
 *  · as a destination of its own for the settings side — wired on the Dev page
 *    for now, which is where the member asked for it.
 *
 * WHAT IS HERE, AND WHY IT IS NOT ALL IN THE APPEARANCE SHEET: the sheet is the
 * quick door the member described (type size, face, paper, two switches). This is
 * the COMPLETE one — every reader preference in one scroll, including the two that
 * have no switch of their own: WHERE the page stands (the three-way orientation,
 * which a single switch cannot say) and the page's TAP ZONES with their own
 * placement editor.
 */
@Composable
internal fun ReaderSettingsScreen(
    palette: ReaderPalette,
    /** Whether a type size and a face mean anything for what is open. */
    showType: Boolean,
    /** Whether the reading is being read as PAGES right now. */
    paged: Boolean,
    onTogglePaged: () -> Unit,
    /** False when no book is open, so there are no zones to place. */
    canPlaceZones: Boolean = false,
    onEditTapZones: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var moreInks by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
            .statusBarsPadding()
    ) {
        // ── THE HEAD, IN THE READER'S OWN PILL ──────────────────────────
        // The same shape the reader itself wears, because this page IS the reader
        // — one step further in.
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
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

            // ── THE TYPE ────────────────────────────────────────────────
            if (showType) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Type", palette)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderStepperButton(CurioIcons.TextDecrease, "Smaller type", palette) {
                            ReaderLook.textScale =
                                (ReaderLook.textScale - 0.08f).coerceIn(0.8f, 2.6f)
                        }
                        Slider(
                            value = ReaderLook.textScale,
                            onValueChange = { next ->
                                ReaderLook.textScale = next.coerceIn(0.8f, 2.6f)
                            },
                            valueRange = 0.8f..2.6f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accent,
                                activeTrackColor = palette.accent,
                                inactiveTrackColor = palette.ink.copy(alpha = 0.15f)
                            )
                        )
                        ReaderStepperButton(CurioIcons.TextIncrease, "Larger type", palette) {
                            ReaderLook.textScale =
                                (ReaderLook.textScale + 0.08f).coerceIn(0.8f, 2.6f)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderTypeFace.entries.forEach { face ->
                            val live = ReaderLook.typeFace == face.key
                            Surface(
                                onClick = { ReaderLook.typeFace = face.key },
                                shape = RoundedCornerShape(50),
                                color = if (live) palette.accent else palette.ink.copy(alpha = 0.07f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    face.label,
                                    style = TextStyle(
                                        fontFamily = readerTypeFamily(face.key),
                                        fontSize = 15.sp,
                                        fontWeight = if (live) FontWeight.SemiBold
                                        else FontWeight.Normal
                                    ),
                                    color = if (live) palette.paper else palette.ink.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── HOW THE PAGE STANDS ─────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("How the page stands", palette)
                // THE THREE-WAY CHOICE, which is why this page exists as well as
                // the sheet's two switches: a switch can say "auto or not", it
                // cannot say "auto, upright or wide" (see [ReaderOrientation]).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderOrientation.entries.forEach { option ->
                        val live = ReaderLook.orientation == option
                        Surface(
                            onClick = { ReaderLook.orientation = option },
                            shape = RoundedCornerShape(50),
                            color = if (live) palette.accent else palette.ink.copy(alpha = 0.07f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                option.label,
                                style = TextStyle(
                                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (live) palette.paper else palette.ink.copy(alpha = 0.75f),
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }
                ReaderSwitchRow(
                    label = "Horizontal pages",
                    on = paged,
                    palette = palette,
                    onToggle = onTogglePaged
                )
            }

            // ── THE PAGE'S OWN TAP ZONES ────────────────────────────────
            if (canPlaceZones) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Tap zones", palette)
                    ReaderSwitchRow(
                        label = "A tap on an edge acts",
                        on = ReaderLook.tapZones,
                        palette = palette,
                        onToggle = { ReaderLook.tapZones = !ReaderLook.tapZones }
                    )
                    // WHERE they sit and WHAT they do, on the page itself: a zone
                    // is something the member has to see against the words it
                    // governs (see [ReaderTapZoneEditor]).
                    Surface(
                        onClick = { onEditTapZones?.invoke() },
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
                                    .background(palette.accent, RoundedCornerShape(50))
                            )
                            Text(
                                "Place the zones",
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
