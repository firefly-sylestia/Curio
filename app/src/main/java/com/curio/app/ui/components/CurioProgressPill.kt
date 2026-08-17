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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicProgressStore
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.components.curioInnerGlow
import kotlin.math.abs
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
 * v126 — the target (pages/episodes total) is the topic's baked-in
 * `progressTarget` UNLESS the user corrected it in the editor dialog
 * (wrong baked-in totals — merged anime seasons, edition-dependent book
 * page counts), in which case [TopicProgressStore.getTarget] wins.
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
    val bakedTarget = topic.progressTarget ?: return
    if (bakedTarget <= 0) return
    val target = TopicProgressStore.getTarget(topic.id, bakedTarget)
    val unit = topic.progressUnitLabel
    val current = TopicProgressStore.get(topic.id).coerceIn(0, target)
    val fraction = (current.toFloat() / target).coerceIn(0f, 1f)
    var showEditor by remember { mutableStateOf(false) }
    // v126 — which pill opened the editor: the MAIN pill opens with the
    // current target, the alternate-edition pill pre-fills the alternate
    // count (nothing persists until Save, so a Cancel leaves everything
    // untouched).
    var editorPrefill by remember { mutableStateOf<Int?>(null) }
    // v126 — an alternate-edition pill (books): when the topic data carries
    // a second common edition with a big page gap, a small "or N pp ·
    // Edition" pill renders beside the main one. Tapping it opens the
    // editor pre-set to that count (Save applies it).
    // v128 — threshold lowered 20% → 8%: real translation/edition gaps
    // (Rutherford vs Grossman Don Quixote, Denny vs Signet Les Misérables,
    // etc.) run 8–16%, so the old ≥20% rule hid 4 of the 5 books that
    // carried alt data. 8% still excludes trivial trim/font variance
    // (3–5%).
    val altCount = topic.altPageCount
    val altGapHuge = altCount != null && altCount > 0 &&
        abs(altCount - bakedTarget) >= (bakedTarget * 0.08).toInt()

    if (showEditor) {
        CurioProgressEditorDialog(
            topic = topic,
            contentColor = dialogContentColor,
            // v126 — the alternate pill pre-fills its count; the main pill
            // passes null (current target).
            initialTarget = editorPrefill,
            onDismiss = { showEditor = false }
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = {
                editorPrefill = null
                showEditor = true
            },
            shape = RoundedCornerShape(50),
            color = background,
            shadowElevation = 2.dp,
            // v29 — dark mode elevation visibility (glow).
            modifier = Modifier
                .curioDarkGlow(2.dp, RoundedCornerShape(50))
                // v81 — One UI 9.5: the colorful pill carries a soft radial
                // glow of the accent's light twin, reflected inside the pill
                // (dark mode only).
                .curioInnerGlow(RoundedCornerShape(50), accent, strength = 0.14f)
        ) {
            // v86 — fuller pill: the 7dp vertical padding made the pill a slim
            // strip (especially the reveal's count-only badge), so the v81 inner
            // glow's radial (radius = width) washed over the whole sliver and
            // read as a glow bleeding past the pill. 11dp vertical + 14dp
            // horizontal gives it a proper pill body the glow can live inside.
            // v98 — WIDER still: 14 → 18dp horizontal so the pill reads as a
            // proper pill (the slim strip let the shadow/glow show around it).
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
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
        // v126 — alternate-edition pill: a quieter sibling showing the other
        // common edition's page count; tapping it pre-fills the editor with
        // that count (Save applies it as the target override).
        if (altGapHuge) {
            Surface(
                onClick = {
                    editorPrefill = altCount
                    showEditor = true
                },
                shape = RoundedCornerShape(50),
                color = background.copy(alpha = 0.55f),
                modifier = Modifier.curioDarkGlow(2.dp, RoundedCornerShape(50))
            ) {
                Text(
                    text = buildString {
                        append("or ").append(altCount).append(" ")
                        append(if (topic.altPageLabel.isNotBlank()) topic.altPageLabel else unit)
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ink.copy(alpha = 0.85f),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                )
            }
        }
    }
}

/**
 * v167 — the count QUICK-EDIT moved to the dialog's top corner: ONE
 * number (the current count, no "/ total" suffix) sits top-right — tap it
 * and the inline numeric field opens right there, Enter saves it, and a
 * small replay icon beside it resets the count to the default (0). The
 * "0 / pages" line under the ring and the "Edit total" chip are GONE
 * (the user asked to remove both — the total now comes only from the
 * topic data or the alternate-edition pill's prefill). The ring keeps the
 * big %, the −/+ steppers and slider still adjust the count, and Finish +
 * Save remain the persist actions.
 *
 * v149 — REVERTED to the ring layout (the v135 stepper-first redesign was
 * too much — the user meant only the page-count EDITING, not the whole
 * progress UI): the circular progress ring with the big % + count is back,
 * with −/+ steppers, a slider, and only Finish + Save. Two improvements
 * kept: (1) the slider snaps per whole unit on small totals and runs
 * continuous (rounded) on big ones (the "editor isn't working" bug), and
 * (2) editing the PAGE COUNT is now an EXPLICIT "Edit total" chip with an
 * edit pencil under the count — the old hidden tappable count is gone.
 * (v167 — both the chip and the count line were then removed, see above.)
 *
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
 *
 * v126 — the TARGET is no longer locked to the topic data: an inline
 * number field corrects the total pages/episodes (wrong baked-in totals
 * — merged anime seasons, edition-dependent book page counts). The
 * corrected target persists per-topic via [TopicProgressStore.setTarget]
 * and overrides the baked-in count everywhere the pill/card shows it.
 * v149 — the field is opened by the explicit "Edit total" chip under the
 * count, not by tapping the count itself (which read as plain text).
 * v167 — that chip is gone; the alternate-edition pill still pre-fills
 * the target via [initialTarget].
 */
@Composable
fun CurioProgressEditorDialog(
    topic: CurioTopic,
    contentColor: Color,
    onDismiss: () -> Unit,
    // v126 — the alternate-edition pill pre-fills the editor with another
    // common edition's page count; nothing persists until Save.
    initialTarget: Int? = null
) {
    val bakedTarget = topic.progressTarget ?: return
    if (bakedTarget <= 0) return
    val unit = topic.progressUnitLabel
    val context = LocalContext.current
    var target by remember {
        mutableIntStateOf(initialTarget ?: TopicProgressStore.getTarget(topic.id, bakedTarget))
    }
    val start = TopicProgressStore.get(topic.id).coerceIn(0, target)
    var value by remember { mutableIntStateOf(start) }
    val fraction = (value.toFloat() / target).coerceIn(0f, 1f)
    // v167 — the dialog's TOP-CORNER single count is the quick-edit: tap
    // it and an inline numeric field opens; Enter saves it; the replay
    // icon beside it resets the count to the default (0). The old "0 /
    // pages" line under the ring AND the "Edit total" chip are GONE (the
    // user asked to remove them — the count moved to the corner and the
    // total stays baked / alt-pill prefill).
    var editingValue by remember { mutableStateOf(false) }
    var valueText by remember { mutableStateOf(value.toString()) }
    // Enter on the field saves the typed count immediately.
    val commitValueEdit: () -> Unit = {
        val typed = valueText.toIntOrNull()
        value = (typed ?: value).coerceIn(0, target)
        valueText = value.toString()
        editingValue = false
        if (value <= 0) TopicProgressStore.clear(context, topic.id)
        else TopicProgressStore.set(context, topic.id, value, target)
    }
    // Reset the count back to the default (0 — the store treats it as
    // cleared), persisted the same way the Enter-save does.
    val resetValue: () -> Unit = {
        value = 0
        valueText = "0"
        editingValue = false
        TopicProgressStore.clear(context, topic.id)
    }

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
                // ── Top corner — the single current count (v167): tap it
                //    and the inline numeric field opens right there, Enter
                //    saves it, and the replay icon resets it to the default
                //    (0). The old "0 / pages" line and "Edit total" chip
                //    are gone — the count now lives here, one number only.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editingValue) {
                        BasicTextField(
                            value = valueText,
                            onValueChange = { valueText = it.filter { c -> c.isDigit() }.take(5) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { commitValueEdit() }),
                            cursorBrush = SolidColor(contentColor),
                            modifier = Modifier
                                .width(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(contentColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    } else {
                        Surface(
                            onClick = {
                                valueText = value.toString()
                                editingValue = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = contentColor.copy(alpha = 0.08f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$value",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = contentColor
                                )
                                CurioIcon(
                                    name = CurioIcons.Edit,
                                    contentDescription = "Edit progress",
                                    tint = contentColor.copy(alpha = 0.6f),
                                    size = 12.dp
                                )
                            }
                        }
                    }
                    // Reset to default (0).
                    Surface(
                        onClick = { resetValue() },
                        shape = CircleShape,
                        color = contentColor.copy(alpha = 0.08f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CurioIcon(
                                name = CurioIcons.Replay,
                                contentDescription = "Reset to default",
                                tint = contentColor.copy(alpha = 0.6f),
                                size = 14.dp
                            )
                        }
                    }
                }

                // ── Circular progress ring + big % (the count now lives
                //    in the top corner — v167) ──
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${(fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp
                            ),
                            color = contentColor
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

                // ── Stepped slider — v135 fix kept after the v149 revert:
                //    whole-unit steps only when the total is small (≤ 200);
                //    big totals run continuous (rounded), so the thumb never
                //    fights a non-integer snap position on 1000-page books
                //    (the "editor isn't working" bug).
                Slider(
                    value = value.toFloat(),
                    onValueChange = { value = it.roundToInt().coerceIn(0, target) },
                    valueRange = 0f..target.toFloat(),
                    steps = if (target <= 200) (target - 1).coerceAtLeast(0) else 0,
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
                        // v126 — persist the corrected total when it differs
                        // from the baked-in data; clear it when the user put
                        // it back to the data value.
                        if (target != bakedTarget) {
                            TopicProgressStore.setTarget(context, topic.id, target)
                        } else {
                            TopicProgressStore.clearTarget(context, topic.id)
                        }
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
