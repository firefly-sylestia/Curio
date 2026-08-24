package com.curio.app.infrastructure

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioTheme

/**
 * v272 — Glass widget CONFIGURATION. Launched by the launcher when the
 * widget is placed (android:configure) and on long-press → Edit
 * (widgetFeatures="reconfigurable"). Each placed widget gets its own
 * persisted mode + pane style.
 *
 * v276 — Styling rebuilt around ONE live preview pane that always shows the
 * exact resulting look for the current selection (Blur / preset / custom
 * hue+opacity), so choosing a style is never guesswork.
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
        val initialColor = GlassWidgetPane.readCustomColor(this, appWidgetId)
        val initialOpacity = GlassWidgetPane.readCustomOpacity(this, appWidgetId)

        setContent {
            CurioTheme {
                var mode by remember { mutableStateOf(initialMode) }
                var style by remember { mutableStateOf(initialStyle) }
                var customHue by remember { mutableFloatStateOf(colorHue(initialColor)) }
                var customOpacity by remember { mutableFloatStateOf(initialOpacity) }

                // The exact colors the current selection will render with.
                val previewTop: ComposeColor
                val previewBottom: ComposeColor
                if (style == GlassWidgetPane.STYLE_CUSTOM) {
                    val base = argbFromHue(customHue)
                    val alpha = (customOpacity * 255).toInt().coerceIn(8, 235)
                    val top = (alpha shl 24) or (base and 0x00FFFFFF)
                    val r = (Color.red(base) * 0.70f).toInt()
                    val g = (Color.green(base) * 0.70f).toInt()
                    val b = (Color.blue(base) * 0.70f).toInt()
                    val bottom = ((alpha * 0.85f).toInt() shl 24) or (r shl 16) or (g shl 8) or b
                    previewTop = ComposeColor(top)
                    previewBottom = ComposeColor(bottom)
                } else {
                    val preset = GlassWidgetPane.Preset.entries.firstOrNull {
                        it.name == style
                    } ?: GlassWidgetPane.Preset.LIGHT
                    previewTop = ComposeColor(preset.top)
                    previewBottom = ComposeColor(preset.bottom)
                }

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

                        // ── LIVE PREVIEW ────────────────────────────────
                        // Exactly what the widget pane will render — over a
                        // colorful stand-in wallpaper. Blur shows the pure
                        // launcher-blur state (no pane).
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                                .clip(RoundedCornerShape(24.dp))
                        ) {
                            // Stand-in wallpaper bands.
                            drawRect(ComposeColor(0xFF7E57C2))
                            drawRect(ComposeColor(0xEF9A9A), topLeft = Offset(size.width * 0.33f, 0f), size = size.copy(width = size.width * 0.34f))
                            drawRect(ComposeColor(0x80DEEA), topLeft = Offset(size.width * 0.67f, 0f), size = size.copy(width = size.width * 0.33f))
                            if (style != GlassWidgetPane.STYLE_DEFAULT) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(previewTop, previewBottom)),
                                    cornerRadius = CornerRadius(20.dp.toPx())
                                )
                            } else {
                                // Blur: soft white wash only (the tint).
                                drawRoundRect(
                                    color = ComposeColor(0x66FFFFFF),
                                    cornerRadius = CornerRadius(20.dp.toPx())
                                )
                            }
                        }
                        Text(
                            when (style) {
                                GlassWidgetPane.STYLE_DEFAULT -> "Preview · Samsung wallpaper blur, no pane"
                                GlassWidgetPane.STYLE_CUSTOM -> "Preview · custom color"
                                else -> "Preview · " + (
                                    GlassWidgetPane.Preset.entries.firstOrNull {
                                        it.name == style
                                    }?.label ?: "pane"
                                    )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // v276 - DEFAULT as a proper card: pure One UI
                        // wallpaper blur, customization OFF. Chips below opt
                        // into a custom pane.
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
                                    Text("Default \u00b7 Samsung blur", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Just the launcher's frosted blur - no pane customization",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // ── Pane style chips ────────────────────────────
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassWidgetPane.Preset.entries.forEach { preset ->
                                StyleChip(
                                    label = preset.label,
                                    selected = style == preset.name,
                                    onClick = { style = preset.name }
                                ) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                ComposeColor(preset.top),
                                                ComposeColor(preset.bottom)
                                            )
                                        ),
                                        cornerRadius = CornerRadius(size.minDimension / 4f)
                                    )
                                }
                            }
                            // Custom chip: rainbow ring swatch.
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

                        // ── Custom controls ─────────────────────────────
                        if (style == GlassWidgetPane.STYLE_CUSTOM) {
                            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text("Color", fontWeight = FontWeight.Bold)
                                Slider(
                                    value = customHue,
                                    onValueChange = { customHue = it },
                                    valueRange = 0f..360f
                                )
                                Text("Opacity · ${(customOpacity * 100).toInt()}%")
                                Slider(
                                    value = customOpacity,
                                    onValueChange = { customOpacity = it },
                                    valueRange = 0.05f..0.9f
                                )
                            }
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
                                GlassWidgetPane.writeCustomColor(ctx, appWidgetId, argbFromHue(customHue))
                                GlassWidgetPane.writeCustomOpacity(ctx, appWidgetId, customOpacity)
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

    /** Small selectable swatch chip with a bold accent ring when picked. */
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

    private fun colorHue(argb: Int): Float {
        val hsv = FloatArray(3)
        Color.colorToHSV(argb or 0xFF000000.toInt(), hsv)
        return hsv[0]
    }

    private fun argbFromHue(hue: Float): Int {
        val hsv = floatArrayOf(hue, 0.55f, 1f)
        return Color.HSVToColor(hsv)
    }
}
