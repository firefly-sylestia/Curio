package com.curio.app.features.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * ── v469 — THE READ-ALOUD SETTINGS, AS A PAGE OF THEIR OWN ────────────────
 *
 * The member: *"the read aloud settings from the setting page … its own screen"*.
 * They were a section of the Reading page — five long lists (speed, engine, voice,
 * narrator, the voice packs, and whether a reading survives leaving the screen)
 * between the paper and the dictionary — and not one of them is about the PAGE.
 * A voice is not a property of a page of paper, which is the whole argument for a
 * page of its own, and the member made it themselves.
 *
 * **THE SAME READER'S WORLD.** This page wears the reader's paper, ink, type and
 * capsules exactly as [ReaderSettingsScreen] does (the member's own instruction
 * for that one: *"dont use settings style use the reader style ui for it"*), and it
 * reuses that page's own components — the head pill, [ReaderChromeButton],
 * [ReaderSliderRow], [ReaderSegmentRow] and [VoiceChoice] — so a control learned on
 * one is the same shape on the other.
 *
 * **THE BODY ITSELF IS [ReadAloudSettingsBody]**, kept beside the page it was moved
 * out of with its original lines rather than re-typed into this file (see its own
 * note). This file owns the shell, the route and the door.
 *
 * Reachable two ways, and it is the SAME composable both times:
 *
 *  · from the Reading page's own door ([ReadAloudDoor]), which is where the section
 *    used to stand, and
 *  · as a destination of its own from the settings side (`CurioRoutes
 *    .READ_ALOUD_SETTINGS`, wired on the Dev page beside Reading settings).
 */
@Composable
internal fun ReadAloudSettingsPage(palette: ReaderPalette, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
            // The READER's own top floor, not the (hidden) status bar: this page is
            // also drawn inside the reader, which hides the bar — the same reason
            // the Reading page reads it (see [readerChromeTopInset]).
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
                ReaderChromeButton(CurioIcons.ChevronLeft, "Back to reading", palette) { onBack() }
                Text(
                    "Read aloud",
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
            ReadAloudSettingsBody(palette)
            Spacer(Modifier.height(6.dp))
        }
    }
}

/**
 * THE SAME PAGE WITHOUT A BOOK OPEN — the settings side's door.
 *
 * Nothing here needs a book: every preference it changes belongs to the PROCESS
 * rather than to one novel (see [ReaderLook]), which is what lets a member point
 * read aloud at a better voice before they have opened anything at all.
 */
@Composable
internal fun ReadAloudSettingsRoute(onBack: () -> Unit) {
    ReadAloudSettingsPage(palette = readerPalette(ReaderLook.inkKey), onBack = onBack)
}

/**
 * ── v469 — THE DOOR, WHERE THE SECTION USED TO STAND ─────────────────────
 *
 * One row on the Reading page, and it is deliberately a DOOR rather than a section
 * heading: the page keeps one shape (a run of full-width capsules), and the
 * subtitle names the five things waiting behind it so the tap is informed rather
 * than hopeful. It wears the accent because it is the one row on the page that
 * leads somewhere instead of setting something.
 */
@Composable
internal fun ReadAloudDoor(palette: ReaderPalette, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(50),
        color = palette.accent.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CurioIcon(CurioIcons.PlayArrow, null, tint = palette.accent, size = 17.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Read aloud",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = palette.ink.copy(alpha = 0.9f)
                )
                Text(
                    "Speed, engine, voice, narrator and the voice packs",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink.copy(alpha = 0.6f)
                )
            }
            CurioIcon(CurioIcons.ChevronRight, null, tint = palette.accent, size = 17.dp)
        }
    }
}
