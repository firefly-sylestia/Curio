package com.curio.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor

/**
 * v26 — double-confirmation delete dialog. Step one confirms the move to
 * the recycle bin (recoverable); step two is the FINAL delete. [onConfirmed]
 * fires only after both confirms. The step resets on every dismiss, so each
 * open always starts from step one.
 *
 * @param title a noun phrase for the thing being deleted — e.g. "this
 *   capture" or "3 selected captures" — used to build both dialog bodies.
 * @param body one sentence describing the action, e.g. "This capture moves
 *   to the Recycle bin."
 */
@Composable
fun CurioTwoStepDeleteDialog(
    visible: Boolean,
    title: String,
    body: String,
    confirmLabel: String = "Delete",
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    if (!visible) return
    var step by remember { mutableIntStateOf(1) }

    fun close() {
        step = 1
        onDismiss()
    }

    if (step == 1) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { close() },
            title = { Text("Move to Recycle bin?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "$body\n\nIt stays recoverable in the Recycle bin (Settings) until you empty it."
                )
            },
            confirmButton = {
                TextButton(onClick = { step = 2 }) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { close() }, colors = curioDialogActionButtonColors()) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { close() },
            title = { Text("Delete $title?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This is the final step, after which $title will no longer appear in your Cabinet.\n\n" +
                        "It stays in the Recycle bin until you empty it, so nothing is gone for good yet."
                )
            },
            confirmButton = {
                TextButton(onClick = { step = 1; onConfirmed() }) {
                    Text(
                        confirmLabel,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 1 }, colors = curioDialogActionButtonColors()) {
                    Text("Back")
                }
            }
        )
    }
}
