package com.curio.app.infrastructure

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioTheme

/**
 * v272 — Glass widget CONFIGURATION. Launched by the launcher when the
 * widget is placed (android:configure) and on long-press → Edit
 * (widgetFeatures="reconfigurable"). Each placed widget gets its own
 * persisted mode + frost tint, so one widget can show your streak while
 * another shows quests.
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
        val initialDark = GlassWidgetProvider.readDarkFrost(this, appWidgetId)

        setContent {
            CurioTheme {
                var mode by remember { mutableStateOf(initialMode) }
                var darkFrost by remember { mutableStateOf(initialDark) }
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
                            text = "Choose what this widget shows. Long-press the widget → Edit to change it later.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dark frost", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Smoky glass instead of light frost — pick what reads best on your wallpaper.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = darkFrost, onCheckedChange = { darkFrost = it })
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { finish() }) { Text("Cancel") }
                            TextButton(onClick = {
                                GlassWidgetProvider.writeMode(this@GlassWidgetConfigActivity, appWidgetId, mode)
                                GlassWidgetProvider.writeDarkFrost(this@GlassWidgetConfigActivity, appWidgetId, darkFrost)
                                val manager = AppWidgetManager.getInstance(this@GlassWidgetConfigActivity)
                                GlassWidgetProvider.updateAppWidget(
                                    this@GlassWidgetConfigActivity, manager, appWidgetId
                                )
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
}
