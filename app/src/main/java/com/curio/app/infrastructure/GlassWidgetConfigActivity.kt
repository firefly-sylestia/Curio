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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * v277 — Custom pane gets a REAL HSV picker: a saturation/value square for
 * the current hue plus a hue bar — opacity is its own separate slider and
 * never bleeds into the color. The live preview renders an accurate
 * miniature of the placed widget (wallpaper bands + pane + icon tile +
 * two-line text) using the exact same [GlassWidgetPane.gradientColors] math
 * the provider draws with.
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
                var `val` by remember { mutableFloatStateOf(initialHsv[2]) }
                var opacity by remember { mutableFloatStateOf(initialOpacity) }
                var corner by remember { mutableFloatStateOf(initialCorner) }
                // Preview corners track the roundness slider (28dp baseline).
                val previewCornerRatio = (corner / 28f).coerceIn(0.15f, 1.2f)
                val textMeasurer = rememberTextMeasurer()

                val customRgb = Color.HSVToColor(floatArrayOf(hue, sat, `val`))
                // EXACT stops the widget will render — shared math.
                val customStops = GlassWidgetPane.gradientColors(customRgb, opacity)
                val preset = GlassWidgetPane.Preset.entries.firstOrNull { it.name == style }
                val previewTop =
                    if (style == GlassWidgetPane.STYLE_CUSTOM) ComposeColor(customStops.first)
                    else ComposeColor(preset?.top ?: 0)
                val previewBottom =
                    if (style == GlassWidgetPane.STYLE_CUSTOM) ComposeColor(customStops.second)
                    else ComposeColor(preset?.bottom ?: 0)

                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(padding)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Glass widget",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                        Text(
                            text = "Choose what this widget shows and how its glass looks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // ── Content mode ────────────────────────────────
                        GlassWidgetMode.entries.forEach { m ->
                            Card(
                                onClick = { mode = m },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (mode == m)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = mode == m, onClick = { mode = m })
                                    Column {
                                        Text(m.label, fontWeight = FontWeight.Bold)
                                        Text(
                                            m.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // ── ACCURATE WIDGET PREVIEW ─────────────────────
                        // A miniature of the real thing: wallpaper bands, the
                        // pane (or bare blur tint in Default), the icon tile
                        // and the same two text lines the widget draws.
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(22.dp)
                                ),
                            onDraw = {
                                val r = 20.dp.toPx()
                                // Wallpaper stand-in bands.
                                drawRect(ComposeColor(0xFF7E57C2))
                                drawRect(
                                    ComposeColor(0xFFEF9A9A),
                                    topLeft = Offset(size.width * 0.34f, 0f),
                                    size = this.size.copy(width = size.width * 0.33f)
                                )
                                drawRect(
                                    ComposeColor(0xFF80DEEA),
                                    topLeft = Offset(size.width * 0.67f, 0f),
                                    size = this.size.copy(width = size.width * 0.33f)
                                )
                                // Pane (Default = root tint only).
                                drawRoundRect(
                                    brush = if (style == GlassWidgetPane.STYLE_DEFAULT)
                                        Brush.verticalGradient(listOf(ComposeColor(0x66FFFFFF), ComposeColor(0x66FFFFFF)))
                                    else Brush.verticalGradient(
                                        listOf(previewTop, previewBottom)
                                    ),
                                    cornerRadius = CornerRadius(r * previewCornerRatio)
                                )
                                // Icon tile.
                                val iconR = 18.dp.toPx()
                                val iconC = Offset(14.dp.toPx() + iconR, size.height / 2f)
                                drawCircle(ComposeColor(0x40FFFFFF), radius = iconR, center = iconC)
                                drawText(
                                    textMeasurer,
                                    mode.glyph,
                                    style = TextStyle(
                                        fontFamily = MaterialSymbolsFontFamily,
                                        fontSize = 22.sp,
                                        color = ComposeColor.White
                                    ),
                                    topLeft = Offset(
                                        iconC.x - 11.sp.toPx(),
                                        iconC.y - 11.sp.toPx()
                                    ),
                                    maxLines = 1
                                )
                                // Title line.
                                drawText(
                                    textMeasurer,
                                    mode.label,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = ComposeColor.White
                                    ),
                                    topLeft = Offset(iconC.x + iconR + 10.dp.toPx(), size.height / 2f - 17.sp.toPx()),
                                    maxLines = 1
                                )
                                // Info line.
                                drawText(
                                    textMeasurer,
                                    sampleInfo(mode),
                                    style = TextStyle(fontSize = 12.sp, color = ComposeColor(0xE6FFFFFF)),
                                    topLeft = Offset(iconC.x + iconR + 10.dp.toPx(), size.height / 2f + 1.sp.toPx()),
                                    maxLines = 1
                                )
                            }
                        )

                        // ── Default card ────────────────────────────────
                        val selectedDefault = style == GlassWidgetPane.STYLE_DEFAULT
                        Card(
                            onClick = { style = GlassWidgetPane.STYLE_DEFAULT },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDefault)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                RadioButton(selected = selectedDefault, onClick = { style = GlassWidgetPane.STYLE_DEFAULT })
                                Column {
                                    Text("Default · Samsung blur", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Just the launcher's frosted blur — no pane customization",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // ── Preset chips ────────────────────────────────
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                selected = style == GlassWidgetPane.STYLE_CUSTOM,
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

                        // ── Corner roundness (any pane style) ──────────
                        if (style != GlassWidgetPane.STYLE_DEFAULT) {
                            Text("Corner roundness · ${corner.toInt()}dp")
                            Slider(
                                value = corner,
                                onValueChange = { corner = it },
                                valueRange = 8f..32f
                            )
                        }

                        // ── HSV picker (custom only) ────────────────────
                        if (style == GlassWidgetPane.STYLE_CUSTOM) {
                            val padShape = RoundedCornerShape(14.dp)

                            // Saturation/value square for the current hue.
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(padShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures { pos ->
                                            sat = (pos.x / size.width).coerceIn(0f, 1f)
                                            `val` = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                            sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                            `val` = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                        }
                                    },
                                onDraw = {
                                    val pureHue = ComposeColor(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                    // White → pure hue, then transparent → black.
                                    drawRect(Brush.horizontalGradient(listOf(ComposeColor.White, pureHue)))
                                    drawRect(Brush.verticalGradient(listOf(ComposeColor.Transparent, ComposeColor.Black)))
                                    // Pointer.
                                    val p = Offset(sat * size.width, (1f - `val`) * size.height)
                                    drawCircle(ComposeColor.White, radius = 9.dp.toPx(), center = p, style = Stroke(2.5.dp.toPx()))
                                    drawCircle(ComposeColor.Black, radius = 11.dp.toPx(), center = p, style = Stroke(1.5.dp.toPx()))
                                }
                            )

                            // Hue bar.
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

                            Spacer(Modifier.height(2.dp))

                            // OPACITY — fully independent of the color above.
                            Text("Opacity · ${(opacity * 100).toInt()}%")
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0.05f..0.9f
                            )
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
                            }) { Text("Save") }
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

    /** Selectable swatch chip with a bold accent ring when picked. */
    @androidx.compose.runtime.Composable
    private fun StyleChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
        swatch: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Canvas(
                modifier = Modifier
                    .padding(2.dp)
                    .size(52.dp)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    ),
                onDraw = swatch
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
