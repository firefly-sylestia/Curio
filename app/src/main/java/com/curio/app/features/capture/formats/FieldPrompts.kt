package com.curio.app.features.capture.formats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * THE FIELD-NOTES QUESTIONS BELONG TO THE MEMBER (v514).
 *
 * "What I observed / What surprised me / What I want to learn next" were three
 * hardcoded strings in `FieldNotesFormat`. A field note about a garden, a bird
 * or a repair wants its own headings, so each entry can rename its three
 * questions and the wording is saved WITH that entry — one capture's wording
 * never leaks into another's.
 *
 * The wording is stored on `CaptureData.FieldNotes`; a legacy entry carries no
 * labels and falls back to the app's own three questions, which are the
 * constants below — the same text the format shipped with.
 */
const val DEFAULT_OBSERVED_LABEL = "What I observed"
const val DEFAULT_SURPRISED_LABEL = "What surprised me"
const val DEFAULT_LEARN_LABEL = "What I want to learn next"

/**
 * The door at the head of a field note: states the three questions are
 * editable, and opens the editor. It reads its own state from the labels the
 * format passes in, so the dialog always opens on what is actually shown.
 */
@Composable
fun FieldPromptsDoor(
    observedLabel: String,
    surprisedLabel: String,
    learnNextLabel: String,
    onLabels: (observed: String, surprised: String, learnNext: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }

    Surface(
        onClick = { open = true },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Rename the questions",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$observedLabel · $surprisedLabel · $learnNextLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (open) {
        FieldPromptsDialog(
            observedLabel = observedLabel,
            surprisedLabel = surprisedLabel,
            learnNextLabel = learnNextLabel,
            onDismiss = { open = false },
            onApply = { observed, surprised, learnNext ->
                onLabels(observed, surprised, learnNext)
                open = false
            }
        )
    }
}

@Composable
private fun FieldPromptsDialog(
    observedLabel: String,
    surprisedLabel: String,
    learnNextLabel: String,
    onDismiss: () -> Unit,
    onApply: (observed: String, surprised: String, learnNext: String) -> Unit
) {
    var observed by remember { mutableStateOf(observedLabel) }
    var surprised by remember { mutableStateOf(surprisedLabel) }
    var learnNext by remember { mutableStateOf(learnNextLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name the three questions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = observed,
                    onValueChange = { observed = it.take(40) },
                    singleLine = true,
                    label = { Text("First") },
                    placeholder = { Text(DEFAULT_OBSERVED_LABEL) }
                )
                OutlinedTextField(
                    value = surprised,
                    onValueChange = { surprised = it.take(40) },
                    singleLine = true,
                    label = { Text("Second") },
                    placeholder = { Text(DEFAULT_SURPRISED_LABEL) }
                )
                OutlinedTextField(
                    value = learnNext,
                    onValueChange = { learnNext = it.take(40) },
                    singleLine = true,
                    label = { Text("Third") },
                    placeholder = { Text(DEFAULT_LEARN_LABEL) }
                )
                Text(
                    text = "Saved with this entry only.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        observed.trim().ifBlank { DEFAULT_OBSERVED_LABEL },
                        surprised.trim().ifBlank { DEFAULT_SURPRISED_LABEL },
                        learnNext.trim().ifBlank { DEFAULT_LEARN_LABEL }
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
