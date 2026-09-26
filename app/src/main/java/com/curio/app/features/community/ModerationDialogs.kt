package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import com.curio.app.data.supabase.BAN_CONTENT
import com.curio.app.data.supabase.BAN_TIERS
import com.curio.app.data.supabase.ModerationReasons
import com.curio.app.data.supabase.banTierBlurb
import com.curio.app.data.supabase.banTierLabel
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.curioFillInk

/**
 * The moderation dialogs, in one place.
 *
 * A member REPORTING something and a moderator DECIDING something are the same
 * interaction — a reason, an optional line of detail, one confirming tap — so
 * they are built from the same pieces here. Both name the exact thing they act
 * on, because a reason attached to the wrong post is worse than no reason.
 */

/** The reasons plus note body both dialogs draw. */
@Composable
private fun ReasonPickerBody(
    subtitle: String,
    reasons: List<String>,
    chosen: String?,
    onChoose: (String) -> Unit,
    note: String,
    onNote: (String) -> Unit,
    notePlaceholder: String
) {
    Column(
        modifier = Modifier
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        reasons.forEach { reason ->
            ReasonChoiceRow(
                label = reason,
                selected = chosen == reason,
                onClick = { onChoose(reason) }
            )
        }
        ModerationNoteField(
            value = note,
            onValueChange = onNote,
            placeholder = notePlaceholder
        )
    }
}

/** One selectable reason — a bordered pill row, filled once it is chosen. */
@Composable
private fun ReasonChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = curioDialogActionColor()
    val shape = RoundedCornerShape(14.dp)
    Surface(
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
        )
    }
}

/**
 * The optional detail line — the app's own typing box (rounded, bordered,
 * accent cursor) rather than a bare Material field, so moderation reads like
 * the rest of the social surfaces.
 */
@Composable
internal fun ModerationNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(500)) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(curioDialogActionColor()),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        decorationBox = { inner ->
            Box {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        }
    )
}

/**
 * The dialog a member files a report through — for a post, a reply or a
 * member. A reason is required (the queue is unreadable without one); the note
 * is where anything specific goes.
 */
@Composable
internal fun ReportTargetDialog(
    title: String,
    subtitle: String,
    reasons: List<String>,
    onDismiss: () -> Unit,
    onReport: (reason: String, note: String?) -> Unit,
    busy: Boolean = false
) {
    var chosen by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            ReasonPickerBody(
                subtitle = subtitle,
                reasons = reasons,
                chosen = chosen,
                onChoose = { chosen = it },
                note = note,
                onNote = { note = it },
                notePlaceholder = "Add anything specific (optional)"
            )
        },
        confirmButton = {
            CurioGlassWindowBlur()
            TextButton(
                onClick = {
                    val reason = chosen ?: return@TextButton
                    onReport(reason, note.trim().takeIf { it.isNotEmpty() })
                },
                enabled = chosen != null && !busy,
                colors = curioDialogActionButtonColors()
            ) {
                Text(
                    text = if (busy) "Sending…" else "Report",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/**
 * THE BAN SHEET — a tier, a clock and a reason, chosen in one place.
 *
 * One dialog rather than four, because the tiers differ only in how far the ban
 * reaches: the moderator picks the reach, then how long it lasts, then says
 * why. Every tier's sentence comes from [banTierBlurb], so the promise made
 * here is the same sentence the member is shown afterwards, and the same one
 * the ban list repeats — a moderation surface that describes itself
 * differently in three places is a moderation surface nobody can trust.
 *
 * A ban already in force opens the same sheet with its tier pre-picked (so a
 * ban can be softened or hardened without being lifted first) and offers the
 * way out at the foot of it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModerationBanDialog(
    memberName: String,
    /** The tier already in force, blank when the member is not banned. */
    currentKind: String = "",
    busy: Boolean = false,
    /**
     * What the server said when the last attempt failed, shown inside the sheet
     * (see the title slot). Blank while nothing has gone wrong.
     */
    error: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (kind: String, reason: String, hours: Int?) -> Unit,
    /** Offered only while a ban is in force. */
    onLift: (() -> Unit)? = null
) {
    val alreadyBanned = currentKind.isNotBlank()
    var kind by remember(currentKind) { mutableStateOf(currentKind.ifBlank { BAN_CONTENT }) }
    // Picked fresh each time: a ban that is already permanent must not look
    // like it was set for a day, and a new ban defaults to the gentlest clock
    // a moderator can walk away from (a week, not forever).
    var hours by remember(currentKind) { mutableStateOf<Int?>(if (alreadyBanned) null else WEEK_HOURS) }
    var chosen by remember { mutableStateOf<String?>(null) }
    // v406 — the reason list is OPEN until it has answered. Once a reason is
    // picked the eight chips fold away to the one that was chosen, so the note
    // field and the Ban button are on the screen the moment the decision is made
    // (member's request: "after piking a ban reason collape the reasons").
    var reasonsOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (alreadyBanned) "Change the ban on $memberName" else "Ban $memberName?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                // ── WHAT THE SERVER SAID, WHERE IT CAN BE SEEN ──────────────
                // A refusal used to land on the page BEHIND this sheet, so a ban
                // the database turned down looked like a button that did nothing
                // (user report: "i am not able to ban any members the ban button
                // isnt working"). The line sits under the title, which is the one
                // part of this sheet nothing can scroll away.
                error?.takeIf { it.isNotBlank() }?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "How far should this ban reach?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BAN_TIERS.forEach { tier ->
                    BanTierRow(
                        kind = tier,
                        selected = kind == tier,
                        onClick = { kind = tier }
                    )
                }

                Text(
                    text = "How long?",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BAN_DURATIONS.forEach { (label, value) ->
                        BanDurationChip(
                            label = label,
                            selected = hours == value,
                            onClick = { hours = value },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = if (hours == null) {
                        "The ban holds until a moderator lifts it."
                    } else {
                        "It lifts itself when the time is up, and the record stays."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (chosen == null) "Why (required — pick one)" else "Why (required)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (chosen == null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
                // ── THE REASONS FIT ON THE SHEET ────────────────────────────
                // Eight full-width rows pushed the list below the fold, so a
                // moderator could open this sheet, choose a tier and a clock,
                // and then find a Ban button that never lit up (the reason is
                // required, and the reason was off-screen). As wrapping chips
                // the whole decision is on one screen.
                // ── AND THE LIST FOLDS AWAY ONCE IT HAS ANSWERED (v406) ──
                //
                // The eight chips are what a moderator needs BEFORE they pick,
                // and eight chips of wrapping text are what stands between them
                // and the note field AFTER. So the list collapses to the one
                // reason that was chosen, with the way back to the full list
                // beside it — the sheet gets shorter at exactly the moment the
                // decision is made.
                if (chosen != null && !reasonsOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = chosen.orEmpty(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        TextButton(
                            onClick = { reasonsOpen = true },
                            colors = curioDialogActionButtonColors()
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModerationReasons.BAN.forEach { reason ->
                            BanReasonChip(
                                label = reason,
                                selected = chosen == reason,
                                onClick = {
                                    chosen = reason
                                    reasonsOpen = false
                                }
                            )
                        }
                    }
                }
                ModerationNoteField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "What happened, in your own words (optional)"
                )

                if (alreadyBanned && onLift != null) {
                    TextButton(
                        onClick = { if (!busy) onLift() },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Lift the ban instead",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            CurioGlassWindowBlur()
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A dimmed button that does not say what it is waiting for reads
                // as a broken button, so it says it.
                if (chosen == null) {
                    Text(
                        text = "Pick a reason",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                TextButton(
                    onClick = {
                        val reason = chosen ?: return@TextButton
                        onConfirm(kind, reason, hours)
                    },
                    enabled = chosen != null && !busy,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = if (busy) "Working…" else if (alreadyBanned) "Update ban" else "Ban",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/**
 * One ban reason as a chip, so the eight of them wrap into a few lines instead
 * of a column that runs off the bottom of the sheet.
 */
@Composable
private fun BanReasonChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = curioDialogActionColor()
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
        )
    }
}

/** The clocks a ban can be set to. `null` hours = until lifted. */
private val BAN_DURATIONS: List<Pair<String, Int?>> = listOf(
    "24 h" to 24,
    "7 days" to 24 * 7,
    "30 days" to 24 * 30,
    "Forever" to null
)

/** The default clock a fresh ban opens on — a week, so the gentlest ban is
 *  also the one a hurried moderator sets by accident. */
private const val WEEK_HOURS = 24 * 7

/** One clock choice: a small filled pill, equal weight so the four fit one row
 *  on a 320dp screen without ellipsizing. */
@Composable
private fun BanDurationChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = curioDialogActionColor()
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = if (selected) accent else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            // v412 — the ink ASKS the accent fill (see [curioFillInk]).
            color = if (selected) curioFillInk(accent) else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
        )
    }
}

/**
 * One tier of the ladder: its name, and one sentence on what it does — the
 * sentence is the point of the row, so it is never truncated away.
 */
@Composable
private fun BanTierRow(
    kind: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = curioDialogActionColor()
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = banTierLabel(kind),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = banTierBlurb(kind),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The dialog a moderator decides through: a removal / hide / dismissal always
 * carries a reason, and the reason list is the one that fits the action.
 */
@Composable
internal fun ModerationReasonDialog(
    title: String,
    subtitle: String,
    reasons: List<String>,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, note: String?) -> Unit,
    destructive: Boolean = true,
    /** Dismissals can go through without a written reason. */
    reasonRequired: Boolean = true,
    busy: Boolean = false,
    /**
     * What the server said when the last attempt failed, shown inside the sheet.
     * Every decision a moderator makes is a server function, and a refusal that
     * lands on the page behind the dialog is a refusal nobody reads.
     */
    error: String? = null
) {
    var chosen by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                error?.takeIf { it.isNotBlank() }?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            ReasonPickerBody(
                subtitle = subtitle,
                reasons = reasons,
                chosen = chosen,
                onChoose = { chosen = it },
                note = note,
                onNote = { note = it },
                notePlaceholder = "Note for the record (optional)"
            )
        },
        confirmButton = {
            CurioGlassWindowBlur()
            TextButton(
                onClick = {
                    if (reasonRequired && chosen == null) return@TextButton
                    onConfirm(chosen ?: "no action needed", note.trim().takeIf { it.isNotEmpty() })
                },
                enabled = (!reasonRequired || chosen != null) && !busy,
                colors = if (destructive) {
                    androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    curioDialogActionButtonColors()
                }
            ) {
                Text(
                    text = if (busy) "Working…" else confirmLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
