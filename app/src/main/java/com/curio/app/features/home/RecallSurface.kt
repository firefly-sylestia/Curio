package com.curio.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioRecall
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioSheetContainerColor

/**
 * v489 — WHERE A RECALL IS ANSWERED.
 *
 * A sheet, not a field inside the hero: the writing wants the width, the
 * keyboard wants the bottom of the screen, and the app already answers every
 * "a control needs a page" question with a bottom sheet. It carries the
 * member's own previous answer back to them when there is one — the whole point
 * of a second return is that there was a first.
 *
 * **The answer is kept even when it is empty**: "I don't remember" is a real
 * record and the honest one, and requiring words would turn a return into a
 * chore. [onKeep] hands the caller the text; the scoring and the rescheduling
 * belong to [CurioRecall], not to a composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecallSheet(
    topicName: String,
    agoText: String,
    /** The member's words from the last return, if this topic has been back before. */
    previousAnswer: String,
    onDismiss: () -> Unit,
    onKeep: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var answer by rememberSaveable { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioSheetContainerColor(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        CurioGlassWindowBlur()
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "ONE TO REMEMBER",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = topicName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "You finished this $agoText — what do you still remember?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (previousAnswer.isNotBlank()) {
                // The last return, given back quietly. Quoted, not styled as a
                // card: it is the member's own words, not a record of progress.
                Text(
                    text = "“$previousAnswer”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                singleLine = true,
                label = { Text("One line is enough") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    // Confirm haptic: this is a completion, and the app's own
                    // convention is Confirm for one.
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onKeep(answer)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keep it")
            }
        }
    }
}
