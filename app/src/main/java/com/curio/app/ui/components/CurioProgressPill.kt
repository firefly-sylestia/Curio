package com.curio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicProgressStore

/**
 * v29 — the per-topic progress pill: a LONG accent-shaped control that shows
 * how far the user is through a book (pages read) or anime (episodes
 * watched). Tap opens a small editor (slider + presets) that writes to
 * [TopicProgressStore]. Used on the reveal hero card, the Cabinet entry
 * cards and the entry detail hero — the same topic shares one progress
 * everywhere.
 */
@Composable
fun CurioProgressPill(
    topic: CurioTopic,
    accent: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    fill: Color = accent
) {
    val target = topic.progressTarget ?: return
    if (target <= 0) return
    val unit = topic.progressUnitLabel
    val current = TopicProgressStore.get(topic.id)
    val fraction = (current.toFloat() / target).coerceIn(0f, 1f)
    var showEditor by remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showEditor) {
        ProgressEditorDialog(
            topic = topic,
            accent = accent,
            onDismiss = { showEditor = false }
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(fill)
            .clickable { showEditor = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Tiny progress ring ──
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp
                    ),
                    color = contentColor
                )
            }
            // ── Count + unit ──
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$current / $target $unit",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = contentColor,
                    maxLines = 1
                )
                // ── Slim progress bar ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(contentColor)
                    )
                }
            }
            // ── Edit hint ──
            Text(
                text = "Edit",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Small editor dialog for a topic's progress: a slider over the full range
 * plus a "finished" quick-set, Reset, and Done.
 */
@Composable
private fun ProgressEditorDialog(
    topic: CurioTopic,
    accent: Color,
    onDismiss: () -> Unit
) {
    val target = topic.progressTarget ?: return
    val unit = topic.progressUnitLabel
    val start = TopicProgressStore.get(topic.id).coerceIn(0, target)
    var value by remember { mutableFloatStateOf(start.toFloat()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = topic.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 2
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${value.toInt()} / $target $unit",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..target.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = accent.copy(alpha = 0.25f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { value = 0f },
                        modifier = Modifier.weight(1f)
                    ) { Text("Reset") }
                    TextButton(
                        onClick = { value = target.toFloat() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Finished") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = value.toInt()
                    if (v <= 0) TopicProgressStore.clear(context, topic.id)
                    else TopicProgressStore.set(context, topic.id, v, target)
                    onDismiss()
                }
            ) { Text("Save", color = accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
