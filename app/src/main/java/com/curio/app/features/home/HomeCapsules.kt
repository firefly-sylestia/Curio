package com.curio.app.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioRecall
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioSheetContainerColor

/**
 * v496 — HOME'S TWO SMALL CAPSULES: THE RETURN, AND THE LETTERS.
 *
 * The member, on the Return: *"the return doesn't show up — make it a small
 * capsule which shows always"*. They were right about the cause: the Return was a
 * CARD IN THE HERO that existed only between the moment a topic fell due and the
 * moment it was answered, so a member with a fresh ladder — or one who had just
 * answered — saw nothing at all and had no way to know the feature was there. A
 * feature whose only surface is a due day is invisible on every other day.
 *
 * So the Return is a CAPSULE under the hero, and it is ALWAYS drawn while the
 * feature is on:
 *
 *  · **Something due** — the capsule wears the accent, counts what is waiting
 *    ("3 waiting") and opens the answering sheet. It is the loudest thing it ever
 *    needs to be, and it is still one line tall.
 *  · **Nothing due yet** — it says when the next one comes round ("next in 3
 *    days") and opens the Return's own page, where the ladder is written out, so
 *    the schedule the member is walking is a thing they can READ rather than only
 *    wait for.
 *  · **Nothing on the ladder at all** — it says so in four words ("nothing
 *    waiting yet"). It is not a dead pill: the sheet behind it explains what
 *    starts a return (finishing a topic).
 *
 * The hero itself is the QUESTS card again, always: a hero that changes identity
 * on a due day is the thing the member could not see, and the capsule says the
 * same fact without moving anything.
 *
 * The LETTERS capsule is the same shape for the other kind of later-mail, and it
 * is drawn only once there is at least one sealed letter — a capsule that said
 * "0 letters" would be furniture. It says how many are sealed and when the next
 * lands, and it opens the writing page (see `TimeCapsuleScreen`).
 */
@Composable
fun ReturnCapsule(
    /** How many returns are waiting to be answered right now. */
    due: Int,
    /** When the next one comes round, phrased ([CurioRecall.aheadText]); null if none. */
    aheadText: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val live = due > 0
    // ⚠️ v501 — WORDS, NOT A BUTTON (the member: "make it small capsule in the
    // welcome back with no backgroud just text and dont say nothing aiting").
    //
    // This was a filled capsule with a glyph and a chevron, and in its quiet
    // state it said "The Return · nothing waiting yet" — a button on the member's
    // Home whose whole message was that there was nothing to press. It is a line
    // of text now, in the welcome-back block where the rest of Home's greeting
    // lives, and the empty state is simply NOT DRAWN: a line that has nothing to
    // say says nothing.
    val label = when {
        due > 1 -> "$due topics to remember"
        live -> "1 topic to remember"
        aheadText != null -> "Next return $aheadText"
        else -> null
    }
    if (label == null) return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (live) accent else scheme.onSurfaceVariant
        )
        Text(
            "›",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (live) accent.copy(alpha = 0.8f) else scheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/** How many letters are sealed and when the next one lands, for [ReturnCapsule]'s sibling. */
@Composable
fun LettersCapsule(
    sealed: Int,
    /** When the next letter lands, phrased ([com.curio.app.data.CurioTimeCapsules.untilText]). */
    nextText: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.tertiary
    HomeCapsule(
        glyph = CurioIcons.Lock,
        label = when {
            sealed == 1 -> "1 letter sealed · lands $nextText"
            else -> "$sealed letters sealed · next lands $nextText"
        },
        fill = scheme.surfaceContainerHigh,
        ink = scheme.onSurface,
        accent = accent,
        onClick = onOpen,
        modifier = modifier
    )
}

/**
 * The capsule itself: one line, one glyph, one chevron.
 *
 * Deliberately NOT a card — no eyebrow, no second line, no shadow. It is a
 * status line the member can press, and it has to be cheap enough to sit under
 * the hero on every visit to Home.
 */
@Composable
private fun HomeCapsule(
    glyph: String,
    label: String,
    fill: Color,
    ink: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = fill,
        modifier = modifier.height(46.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurioIcon(
                glyph,
                null,
                // On an accent fill the glyph is the on-accent ink; on a plain
                // surface it is the accent, so the pill still shows its family.
                tint = if (fill == accent) ink else accent,
                size = 18.dp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            CurioIcon(
                CurioIcons.ChevronRight,
                null,
                tint = ink.copy(alpha = 0.55f),
                size = 18.dp
            )
        }
    }
}
