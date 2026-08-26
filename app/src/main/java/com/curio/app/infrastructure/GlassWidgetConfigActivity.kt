package com.curio.app.infrastructure

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioTheme
import com.curio.app.ui.theme.MaterialSymbolsFontFamily

/**
 * v272 — Glass widget CONFIGURATION (placement + long-press → Edit).
 *
 * v282 — Layout rebuilt from user feedback:
 *  - Content modes are compact GRID PILLS (2×2), not tall radio cards.
 *  - ONE accurate miniature preview over a NEUTRAL wallpaper stand-in —
 *    same gradient math ([GlassWidgetPane.gradientColors]), icon tile and
 *    two-line text as the real widget; corner slider scales live.
 *  - Style chips: tidy swatch row; Custom reveals a ROOMY picker section
 *    (SV square + hue bar + independent opacity).
 */
class GlassWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val initialMode = GlassWidgetProvider.readMode(this, appWidgetId)
        val initialStyle = GlassWidgetPane.readStyle(this, appWidgetId)
        val initialHsv = FloatArray(3)
        Color.colorToHSV(
            GlassWidgetPane.readCustomColor(this, appWidgetId) or 0xFF000000.toInt(),
            initialHsv
        )
        val initialOpacity = GlassWidgetPane.readCustomOpacity(this, appWidgetId)
        val initialCorner = GlassWidgetPane.readCorner(this, appWidgetId)

        setContent {
            CurioTheme {
                var mode by remember { mutableStateOf(initialMode) }
                var style by remember { mutableStateOf(initialStyle) }
                var hue by remember { mutableFloatStateOf(initialHsv[0]) }
                var sat by remember { mutableFloatStateOf(initialHsv[1]) }
                var value by remember { mutableFloatStateOf(initialHsv[2]) }
                var opacity by remember { mutableFloatStateOf(initialOpacity) }
                var corner by remember { mutableFloatStateOf(initialCorner) }
                val textMeasurer = rememberTextMeasurer()

                val isCustom = style == GlassWidgetPane.STYLE_CUSTOM
                val isDefault = style == GlassWidgetPane.STYLE_DEFAULT
                val customRgb = Color.HSVToColor(floatArrayOf(hue, sat, value))
                val stops = GlassWidgetPane.gradientColors(customRgb, opacity)
                val preset = GlassWidgetPane.Preset.entries.firstOrNull { it.name == style }
                val previewTop =
                    if (isCustom) ComposeColor(stops.first) else ComposeColor(preset?.top ?: 0)
                val previewBottom =
                    if (isCustom) ComposeColor(stops.second) else ComposeColor(preset?.bottom ?: 0)
                val previewCornerRatio = (corner / 28f).coerceIn(0.15f, 1.2f)

                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(padding)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Glass widget",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 24.dp)
                        )

                        // ── ACCURATE PREVIEW ────────────────────────────
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(22.dp)
                                ),
                            onDraw = {
                                // Neutral wallpaper stand-in (no rainbow noise).
                                drawRect(Brush.verticalGradient(listOf(ComposeColor(0xFF2A2C33), ComposeColor(0xFF14151A))))
                                if (!isDefault) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(listOf(previewTop, previewBottom)),
                                        cornerRadius = CornerRadius(24.dp.toPx() * previewCornerRatio)
                                    )
                                } else {
                                    drawRoundRect(
                                        color = ComposeColor(0x66FFFFFF),
                                        cornerRadius = CornerRadius(24.dp.toPx() * previewCornerRatio)
                                    )
                                }
                                // Icon tile + two text lines — widget ratios.
                                val iconR = 17.dp.toPx()
                                val iconCx = 15.dp.toPx() + iconR
                                val cy = size.height / 2f
                                drawCircle(ComposeColor(0x40FFFFFF), radius = iconR, center = Offset(iconCx, cy))
                                drawText(
                                    textMeasurer,
                                    mode.glyph,
                                    style = TextStyle(
                                        fontFamily = MaterialSymbolsFontFamily,
                                        fontSize = 19.sp,
                                        color = ComposeColor.White
                                    ),
                                    topLeft = Offset(iconCx - 9.5.sp.toPx(), cy - 9.5.sp.toPx()),
                                    maxLines = 1
                                )
                                drawText(
                                    textMeasurer,
                                    mode.label,
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ComposeColor.White),
                                    topLeft = Offset(iconCx + iconR + 10.dp.toPx(), cy - 16.sp.toPx()),
                                    maxLines = 1
                                )
                                drawText(
                                    textMeasurer,
                                    sampleInfo(mode),
                                    style = TextStyle(fontSize = 12.sp, color = ComposeColor(0xE6FFFFFF)),
                                    topLeft = Offset(iconCx + iconR + 10.dp.toPx(), cy + 1.sp.toPx()),
                                    maxLines = 1
                                )
                            }
                        )

                        // ── Content mode — GRID PILLS (2×2) ─────────────
                        Text("Content", fontWeight = FontWeight.Bold)
                        GlassWidgetMode.entries.chunked(2).forEach { rowModes ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowModes.forEach { m ->
                                    val selected = mode == m
                                    Surface(
                                        onClick = { mode = m },
                                        shape = RoundedCornerShape(50),
                                        color = if (selected)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            m.label,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected)
                                                MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // ── Pane style (compact scrollable pill row) ───
                        Text("Pane style", fontWeight = FontWeight.Bold)
                        val selectedDefault = isDefault

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            GlassWidgetPane.Preset.entries.forEach { presetChip ->
                                StyleChip(
                                    label = presetChip.label,
                                    selected = style == presetChip.name,
                                    onClick = { style = presetChip.name }
                                ) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                ComposeColor(presetChip.top),
                                                ComposeColor(presetChip.bottom)
                                            )
                                        ),
                                        cornerRadius = CornerRadius(size.minDimension / 4f)
                                    )
                                }
                            }
                            StyleChip(
                                label = "Custom",
                                selected = isCustom,
                                onClick = { style = GlassWidgetPane.STYLE_CUSTOM }
                            ) {
                                drawRoundRect(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            ComposeColor.Red, ComposeColor.Yellow, ComposeColor.Green,
                                            ComposeColor.Cyan, ComposeColor.Blue, ComposeColor.Magenta,
                                            ComposeColor.Red
                                        )
                                    ),
                                    cornerRadius = CornerRadius(size.minDimension / 4f)
                                )
                            }
                        }

                        // ── Default card ────────────────────────────────
                        Card(
                            onClick = { style = GlassWidgetPane.STYLE_DEFAULT },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDefault)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Default · Samsung blur", fontWeight = FontWeight.Bold)
                                Text(
                                    "Just the launcher's frosted blur — no pane customization",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ── Custom picker — roomy ───────────────────────
                        if (isCustom) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("Custom color", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(10.dp))
                                    val padShape = RoundedCornerShape(14.dp)
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(padShape)
                                            .pointerInput(Unit) {
                                                detectTapGestures { pos ->
                                                    sat = (pos.x / size.width).coerceIn(0f, 1f)
                                                    value = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, _ ->
                                                    change.consume()
                                                    sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                                    value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                                }
                                            },
                                        onDraw = {
                                            val pureHue = ComposeColor(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                            drawRect(Brush.horizontalGradient(listOf(ComposeColor.White, pureHue)))
                                            drawRect(Brush.verticalGradient(listOf(ComposeColor.Transparent, ComposeColor.Black)))
                                            val p = Offset(sat * size.width, (1f - value) * size.height)
                                            drawCircle(ComposeColor.White, radius = 9.dp.toPx(), center = p, style = Stroke(2.5.dp.toPx()))
                                            drawCircle(ComposeColor.Black, radius = 11.dp.toPx(), center = p, style = Stroke(1.5.dp.toPx()))
                                        }
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(26.dp)
                                            .clip(padShape)
                                            .pointerInput(Unit) {
                                                detectTapGestures { pos ->
                                                    hue = (pos.x / size.width).coerceIn(0f, 1f) * 360f
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, _ ->
                                                    change.consume()
                                                    hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                                                }
                                            },
                                        onDraw = {
                                            drawRect(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        ComposeColor.Red, ComposeColor.Yellow, ComposeColor.Green,
                                                        ComposeColor.Cyan, ComposeColor.Blue, ComposeColor.Magenta,
                                                        ComposeColor.Red
                                                    )
                                                )
                                            )
                                            val x = (hue / 360f) * size.width
                                            drawCircle(ComposeColor.White, radius = 10.dp.toPx(), center = Offset(x, size.height / 2f), style = Stroke(2.5.dp.toPx()))
                                        }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text("Opacity · ${(opacity * 100).toInt()}%")
                                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.05f..0.9f)
                                }
                            }
                        }

                        // ── Corner roundness ────────────────────────────
                        if (!isDefault) {
                            Text("Corner roundness · ${corner.toInt()}dp")
                            Slider(value = corner, onValueChange = { corner = it }, valueRange = 8f..32f)
                        }

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { finish() }) { Text("Cancel") }
                            TextButton(onClick = {
                                val ctx = this@GlassWidgetConfigActivity
                                GlassWidgetProvider.writeMode(ctx, appWidgetId, mode)
                                GlassWidgetPane.writeStyle(ctx, appWidgetId, style)
                                GlassWidgetPane.writeCustomColor(ctx, appWidgetId, customRgb)
                                GlassWidgetPane.writeCustomOpacity(ctx, appWidgetId, opacity)
                                GlassWidgetPane.writeCorner(ctx, appWidgetId, corner)
                                val manager = AppWidgetManager.getInstance(ctx)
                                GlassWidgetProvider.updateAppWidget(ctx, manager, appWidgetId)
                                setResult(
                                    Activity.RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                )
                                finish()
                            }) { Text("Apply") }
                        }
                    }
                }
            }
        }
    }

    private fun sampleInfo(mode: GlassWidgetMode): String = when (mode) {
        GlassWidgetMode.STREAK -> "5-day explore streak"
        GlassWidgetMode.QUESTS -> "Level 4 · 940 quest XP earned"
        GlassWidgetMode.CABINET -> "3 saved discoveries"
        GlassWidgetMode.SESSIONS -> "session live right now"
    }

    /** v285 — COMPACT style chip: inline swatch + label in one small pill. */
    @androidx.compose.runtime.Composable
    private fun StyleChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
        swatch: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
    ) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            color = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            border = if (selected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Canvas(modifier = Modifier.size(26.dp)) { swatch() }
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
