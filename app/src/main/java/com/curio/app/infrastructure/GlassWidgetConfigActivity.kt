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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioTheme

/**
 * v272 — Glass widget CONFIGURATION. Launched by the launcher when the
 * widget is placed (android:configure) and on long-press → Edit
 * (widgetFeatures="reconfigurable"). Each placed widget gets its own
 * persisted mode + pane style, so one widget can show your streak while
 * another shows quests in a completely different glass tint.
 *
 * v274 — Pane styling: five preset swatches (each previewing its real
 * gradient) plus a CUSTOM mode with full color (hue) and opacity sliders.
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
                var customHue by remember {
                    // Store as HSV hue 0..360 derived from the saved ARGB.
                    mutableFloatStateOf(colorHue(initialColor))
                }
                var customOpacity by remember { mutableFloatStateOf(initialOpacity) }

                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                            text = "Choose what this widget shows and how its glass looks. Long-press the widget → Edit to change it later.",
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

                        // ── Pane presets ────────────────────────────────
                        Text(
                            "Pane style",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassWidgetPane.Preset.entries.forEach { preset ->
                                val selected = style == preset.name
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Canvas(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .border(
                                                width = if (selected) 3.dp else 1.dp,
                                                color = if (selected)
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                    ) {
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color(preset.top),
                                                    Color(preset.bottom)
                                                )
                                            ),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension / 2f)
                                        )
                                        drawCircle(
                                            color = Color(preset.rim),
                                            radius = size.minDimension / 2f - 2.dp.toPx(),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                    Text(
                                        preset.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // ── Custom color + opacity ──────────────────────
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Canvas(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .border(
                                                width = if (style == GlassWidgetPane.STYLE_CUSTOM) 3.dp else 1.dp,
                                                color = if (style == GlassWidgetPane.STYLE_CUSTOM)
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            ),
                                        onDraw = {
                                            drawCircle(
                                                brush = Brush.sweepGradient(
                                                    listOf(
                                                        Color.Red, Color.Yellow, Color.Green,
                                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                                    )
                                                )
                                            )
                                        }
                                    )
                                    Text(
                                        "  Custom",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { style = GlassWidgetPane.STYLE_CUSTOM }) {
                                        Text(if (style == GlassWidgetPane.STYLE_CUSTOM) "Editing" else "Edit")
                                    }
                                }
                                Text("Color")
                                Slider(
                                    value = customHue,
                                    onValueChange = {
                                        customHue = it
                                        style = GlassWidgetPane.STYLE_CUSTOM
                                    },
                                    valueRange = 0f..360f
                                )
                                Text("Opacity · ${(customOpacity * 100).toInt()}%")
                                Slider(
                                    value = customOpacity,
                                    onValueChange = {
                                        customOpacity = it
                                        style = GlassWidgetPane.STYLE_CUSTOM
                                    },
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
                                if (style == GlassWidgetPane.STYLE_CUSTOM) {
                                    GlassWidgetPane.writeCustomColor(
                                        ctx, appWidgetId, argbFromHue(customHue)
                                    )
                                    GlassWidgetPane.writeCustomOpacity(ctx, appWidgetId, customOpacity)
                                }
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
