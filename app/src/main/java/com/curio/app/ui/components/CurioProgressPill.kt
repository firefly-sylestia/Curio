package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicProgressStore
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.components.curioInnerGlow
import kotlin.math.roundToInt

/**
 * v29 — the per-topic progress pill (REDESIGNED): a compact OPAQUE pill that
 * shows how far the user is through a book (pages read) or anime (episodes
 * watched). `showBar` adds a slim category-accent progress bar under the
 * count; the badge variant (reveal hero top-right corner) is count-only.
 * Tap opens the redesigned [CurioProgressEditorDialog] — ring, −/+ steppers,
 * stepped slider, Finish/Save only (no Reset, no Cancel). Writes to
 * [TopicProgressStore]; every surface shares the same topic progress.
 *
 * @param accent the category accent — used for the progress bar / progress
 *   arc.
 * @param ink text color on the pill background (category ink on a tinted
 *   pill, the accent itself on a light frosted pill).
 * @param background the pill's OPAQUE background (a category tint on the
 *   detail hero, a light frosted fill on the gradient heroes).
 */
@Composable
fun CurioProgressPill(
    topic: CurioTopic,
    accent: Color,
    ink: Color,
    background: Color,
    modifier: Modifier = Modifier,
    showBar: Boolean = true,
    // v45 — the editor dialog's content color. The pill's [ink] is tuned
    // for the pill's OWN background, but the dialog sits on the theme
    // dialog container — a pill ink that matches the accent (e.g. the
    // reveal hero's deep-accent text on a light frosted pill) would render
    // invisible against it. v53 — defaults to the theme's onSurface. v66 —
    // the reveal + detail callers pass [categoryInk] (deep accent in light,
    // light twin in dark) so the dialog is category-colored AND readable.
    dialogContentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val target = topic.progressTarget ?: return
    if (target <= 0) return
    val unit = topic.progressUnitLabel
    val current = TopicProgressStore.get(topic.id)
    val fraction = (current.toFloat() / target).coerceIn(0f, 1f)
    var showEditor by remember { mutableStateOf(false) }

    if (showEditor) {
        CurioProgressEditorDialog(
            topic = topic,
            contentColor = dialogContentColor,
            onDismiss = { showEditor = false }
        )
    }

    Surface(
        onClick = { showEditor = true },
        shape = RoundedCornerShape(50),
        color = background,
        shadowElevation = 2.dp,
        // v29 — dark mode elevation visibility (glow).
        modifier = modifier
            .curioDarkGlow(2.dp, RoundedCornerShape(50))
            // v81 — One UI 9.5: the colorful pill carries a soft radial
            // glow of the accent's light twin, reflected inside the pill
            // (dark mode only).
            .curioInnerGlow(RoundedCornerShape(50), accent, strength = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$current / $target $unit",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = ink,
                maxLines = 1
            )
            if (showBar) {
                // v66 — visible in pastel light mode: [accent] resolves to a
                // light pastel twin that washes out against the tinted pill,
                // so the bar fill uses the deep category ink (hue-preserving,
                // readable). v78 — light only (the dark accent fill is gone).
                val barFill = ink
                // Slim progress bar — the category accent fills on the pill.
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(barFill.copy(alpha = 0.30f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(barFill)
                    )
                }
            }
        }
    }
}

/**
 * v29 — the redesigned progress editor: a circular progress ring with the
 * big % + count, −/+ steppers for precise ±1 changes, a stepped slider for
 * sweeping, and only Finish (quick-set to the target) + Save (persist +
 * close) — no Reset, no Cancel. Dismiss is tap-outside / back.
 *
 * v66 — color fix: the dialog used to mix the caller's raw accent with the
 * theme's onSurface, which went dark-on-dark when the accent was a deep
 * category color (the reveal hero's pill). [contentColor] now drives EVERY
 * element (callers pass [com.curio.app.ui.theme.categoryInk] — a readable
 * deep accent in light mode / light twin in dark), the steppers tint a
 * 14% wash of it instead of solid circles, and the Save button pairs it
 * against the theme surface so the label always contrasts.
 */
@Composable
fun CurioProgressEditorDialog(
    topic: CurioTopic,
    contentColor: Color,
    onDismiss: () -> Unit
) {
    val target = topic.progressTarget ?: return
    if (target <= 0) return
    val unit = topic.progressUnitLabel
    val start = TopicProgressStore.get(topic.id).coerceIn(0, target)
    var value by remember { mutableIntStateOf(start) }
    val context = LocalContext.current
    val fraction = (value.toFloat() / target).coerceIn(0f, 1f)

    AlertDialog(
        onDismissRequest = onDismiss,
        // v53 — the dialog wears the standard page-background tint like
        // every other dialog (the accent container was too loud). v66 —
        // [contentColor] drives every element (readable category ink), so
        // the ring, steppers, slider and Save all read in both modes.
        containerColor = curioDialogContainerColor(),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = topic.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = contentColor,
                maxLines = 2
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Circular progress ring + big % + count ──
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(132.dp)) {
                        val stroke = 10.dp.toPx()
                        // Track
                        drawArc(
                            color = contentColor.copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        // Progress arc
                        drawArc(
                            color = contentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * fraction,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${(fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp
                            ),
                            color = contentColor
                        )
                        Text(
                            text = "$value / $target $unit",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = contentColor.copy(alpha = 0.85f)
                        )
                    }
                }

                // ── − / + steppers for precise change ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StepButton(
                        glyph = CurioIcons.Remove,
                        contentDescription = "One less $unit",
                        enabled = value > 0,
                        contentColor = contentColor
                    ) { value = (value - 1).coerceAtLeast(0) }
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(72.dp)
                    )
                    StepButton(
                        glyph = CurioIcons.Add,
                        contentDescription = "One more $unit",
                        enabled = value < target,
                        contentColor = contentColor
                    ) { value = (value + 1).coerceAtMost(target) }
                }

                // ── Stepped slider — snaps to whole units for accuracy ──
                Slider(
                    value = value.toFloat(),
                    onValueChange = { value = it.roundToInt().coerceIn(0, target) },
                    valueRange = 0f..target.toFloat(),
                    // One step per unit (capped so a 1000+ page book keeps
                    // the thumb responsive — ± buttons stay exact).
                    steps = (target - 1).coerceAtMost(600),
                    colors = SliderDefaults.colors(
                        thumbColor = contentColor,
                        activeTrackColor = contentColor,
                        inactiveTrackColor = contentColor.copy(alpha = 0.30f)
                    )
                )
            }
        },
        // ── Finish + Save only (no Reset, no Cancel) ──
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = { value = target },
                    enabled = value < target
                ) {
                    Text(
                        "Finished",
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        val v = value
                        if (v <= 0) TopicProgressStore.clear(context, topic.id)
                        else TopicProgressStore.set(context, topic.id, v, target)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        // v66 — the theme surface is the dark-on-light /
                        // light-on-dark partner for the content-colored
                        // container in BOTH modes, so the label never sinks
                        // into a deep accent button.
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 22.dp, vertical = 10.dp
                    )
                ) {
                    Text(
                        "Save",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        dismissButton = null
    )
}

/** One circular − / + stepper button in the progress dialog. */
@Composable
private fun StepButton(
    glyph: String,
    contentDescription: String,
    enabled: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        // v66 — a soft tint of the content color (not a solid fill) so the
        // glyph contrasts cleanly against it in every theme.
        color = if (enabled) contentColor.copy(alpha = 0.14f) else contentColor.copy(alpha = 0.06f),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = glyph,
                contentDescription = contentDescription,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
                size = 22.dp
            )
        }
    }
}
