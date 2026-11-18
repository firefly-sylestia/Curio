package com.curio.app.features.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.CaptureFormat
import com.curio.app.data.FormatLabels
import com.curio.app.data.shortName
import androidx.compose.ui.platform.LocalContext

/**
 * THE EXPRESS-YOURSELF PAGE IS ONE DOCUMENT OF ROWS (v514).
 *
 * Before this, a page held several "takes" but showed ONE at a time behind a
 * rail of pills, and a take's name was whatever format it happened to be. The
 * member asked for the other shape: one continuous page where each note is a
 * ROW stacked in the page, everything visible together, and each row carries a
 * name they choose — "instead of separate formats … user can add a row in
 * journal itself at the bottom, it will show as a whole, each as one row, and
 * user can rename it".
 *
 * The name is the FORMAT's name, and the member asked for it to change
 * globally, so renaming lives in [FormatLabels] and every surface that prints a
 * format picks it up.
 */
@Composable
internal fun StudioRowHeader(
    format: CaptureFormat,
    /** The last row keeps no remove door — a page is never empty. */
    canRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var renaming by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Text(
            text = format.shortName,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { renaming = true }) {
            Text("Rename", style = MaterialTheme.typography.labelMedium)
        }
        if (canRemove) {
            TextButton(onClick = onRemove) {
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (renaming) {
        FormatRenameDialog(format = format, onDismiss = { renaming = false })
    }
}

/**
 * The door at the foot of the page: a new row is appended below the ones already
 * written, in whatever shape the picker hands back.
 */
@Composable
internal fun StudioAddRowDoor(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Add a row",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "A voice take, a review, a moodboard or field notes — below what you have written.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Renames a format for the whole app (the member's choice: the name is the
 * format's, not this one row's). Blank puts the app's own name back, which is
 * why the hint states it.
 */
@Composable
internal fun FormatRenameDialog(
    format: CaptureFormat,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(format) { mutableStateOf(FormatLabels.labelOf(format).orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this format") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    label = { Text("Name") },
                    placeholder = { Text(FormatLabels.defaultLabel(format)) }
                )
                Text(
                    text = "This name is used everywhere: the row, the page, the cabinet and the entry. " +
                        "Leave it empty to go back to \"${FormatLabels.defaultLabel(format)}\".",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    FormatLabels.rename(context, format, name)
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
