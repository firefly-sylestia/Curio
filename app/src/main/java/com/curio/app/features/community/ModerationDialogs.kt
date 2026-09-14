package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor

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
                notePlaceholder = "Note for the record (optional)"
            )
        },
        confirmButton = {
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
