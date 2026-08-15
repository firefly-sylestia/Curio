package com.curio.app.features.settings

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
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioQuality
import com.curio.app.data.MusicService
import com.curio.app.data.SearchEngine
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor

/**
 * v27q — content ink that reads on the SOLID [curioDialogActionColor]
 * selected-row fill. v78 — light Curio only: white on the rose/primary
 * rows (the AMOLED black flip is gone with dark mode).
 */
@Composable
private fun dialogRowSelectedInk(): Color = Color.White

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
                        // v27q — selection reads as a SOLID action fill with
                        // readable on-fill content; unselected rows wear an
                        // opaque surface so the flat 2dp shadow renders
                        // cleanly behind every row.
                        color = if (selected) curioDialogActionColor()
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) dialogRowSelectedInk()
                                       else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            // v28 — dark mode elevation visibility.
                            .curioDarkGlow(2.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (selected) dialogRowSelectedInk()
                                                   else curioDialogActionColor()
                                )
                            )
                            Column {
                                Text(quality.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    quality.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) dialogRowSelectedInk().copy(alpha = 0.8f)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * v19 — single-choice picker for the explore search engine (Settings →
 * Notifications → Search engine). Mirrors [AudioQualityDialog]'s styling so
 * the picker feels native to the settings section.
 */
@Composable
fun SearchEngineDialog(
    current: SearchEngine,
    onDismiss: () -> Unit,
    onSelected: (SearchEngine) -> Unit
) {
    AlertDialog(
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        onDismissRequest = onDismiss,
        title = { Text("Search engine", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Which search engine the \"Explore in browser\" button searches with.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SearchEngine.entries.forEach { engine ->
                    val selected = engine == current
                    Surface(
                        onClick = { onSelected(engine) },
                        shape = RoundedCornerShape(16.dp),
                        // v27q — see the audio-quality rows above: solid
                        // action fill, flat 2dp shadow behind every row.
                        color = if (selected) curioDialogActionColor()
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) dialogRowSelectedInk()
                                       else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            // v28 — dark mode elevation visibility.
                            .curioDarkGlow(2.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (selected) dialogRowSelectedInk()
                                                   else curioDialogActionColor()
                                )
                            )
                            Column {
                                Text(engine.displayName, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    engine.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) dialogRowSelectedInk().copy(alpha = 0.8f)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * v27s — single-choice picker for the explore music service (Settings →
 * Notifications → Music service). Mirrors [SearchEngineDialog]'s styling so
 * the picker feels native to the settings section.
 */
@Composable
fun MusicServiceDialog(
    current: MusicService,
    onDismiss: () -> Unit,
    onSelected: (MusicService) -> Unit
) {
    AlertDialog(
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        onDismissRequest = onDismiss,
        title = { Text("Music service", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Which streaming service the \"Watch in\" button opens for albums, artists and songs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MusicService.entries.forEach { service ->
                    val selected = service == current
                    Surface(
                        onClick = { onSelected(service) },
                        shape = RoundedCornerShape(16.dp),
                        // v27q — see the audio-quality rows above: solid
                        // action fill, flat 2dp shadow behind every row.
                        color = if (selected) curioDialogActionColor()
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) dialogRowSelectedInk()
                                       else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            // v28 — dark mode elevation visibility.
                            .curioDarkGlow(2.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (selected) dialogRowSelectedInk()
                                                   else curioDialogActionColor()
                                )
                            )
                            Column {
                                Text(service.displayName, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    service.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) dialogRowSelectedInk().copy(alpha = 0.8f)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
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
