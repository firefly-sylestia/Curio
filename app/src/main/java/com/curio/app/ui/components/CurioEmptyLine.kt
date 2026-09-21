package com.curio.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * v439 — THE ONE WAY A BARE LIST SAYS "NOTHING HERE".
 *
 * The member's own pick, from five unification passes: *\"One empty state\"*. The
 * app had grown a sentence per surface for the same fact — the reader's marks list
 * said *\"Nothing marked yet — hold a passage while you read.\"*, a reply list
 * said *\"No replies yet. Say something.\"*, the text history said *\"No snapshots
 * yet — edits you type or paste are kept here\"* — and every one of them is read
 * again by a member who already knows the list is empty, because **the surface
 * around it has already said what it is**. The reader's own sheet is where the
 * member named the fault: *\"highliths and notes empty stat eis bad dot use erm
 * dash\"*, and v438 answered it there. This is that answer, shared.
 *
 * ONE RULE, AND IT IS THE REASON THIS IS NOT APPLIED EVERYWHERE:
 *
 *   · **A BARE LIST inside a surface that already names itself says nothing but
 *     a dash** — the comments sheet under \"Replies\", the history panel under
 *     \"Text history\", the marks sheet under \"Highlights\".
 *   · **AN EMPTY SCREEN keeps its headline, its subtext and its door.** \"No
 *     conversations yet\" with a door to start one is not an empty state; it is
 *     the only affordance on that screen, and a dash there would take the app's
 *     invitation away. The same goes for any message that tells the member how to
 *     FIX the emptiness (\"Lookups are off in Settings — turn them on…\") — that is
 *     an instruction, not a state.
 *
 * So: reach for this when the emptiness is the whole content and nothing is being
 * asked of the member. Do not reach for it to replace a sentence that carries an
 * action.
 *
 * The dash is drawn in the surrounding surface's own muted ink (or the [color] a
 * caller passes, as the reader does for its shelf of papers), because this line is
 * meant to read as absence rather than as content.
 */
@Composable
fun CurioEmptyLine(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
) {
    Text(
        "\u2014",
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier
    )
}
