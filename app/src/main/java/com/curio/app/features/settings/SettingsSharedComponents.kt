package com.curio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.AudioQuality
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor

@Composable
fun AudioQualityDialog(
    currentQuality: AudioQuality,
    onDismiss: () -> Unit,
    onSelected: (AudioQuality) -> Unit
) {
    AlertDialog(
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        onDismissRequest = onDismiss,
        title = { Text("Recording quality", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Higher quality sounds clearer but uses more storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AudioQuality.entries.forEach { quality ->
                    val selected = quality == currentQuality
                    Surface(
                        onClick = { onSelected(quality) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) curioDialogActionColor().copy(alpha = 0.12f) else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (selected) curioDialogActionColor() else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = curioDialogActionColor())
                            )
                            Column {
                                Text(quality.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    quality.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) { Text("Close", fontWeight = FontWeight.Bold) }
        }
    )
}
