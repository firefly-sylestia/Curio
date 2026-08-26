package com.curio.app.features.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.infrastructure.GlassWidgetMode
import com.curio.app.infrastructure.GlassWidgetPane
import com.curio.app.infrastructure.GlassWidgetProvider
import com.curio.app.infrastructure.GlassWidgetConfigActivity

/**
 * v281 — IN-APP WIDGET EDITOR. Some launchers don't surface the standard
 * long-press → Edit flow for reconfigurable widgets; this screen lists every
 * placed Curio glass widget and opens the SAME config UI directly in-app,
 * so editing always works. The config's Apply button re-renders the home
 * widget's RemoteViews immediately.
 */
@Composable
fun WidgetEditorScreen(navController: NavController) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ids = remember(refreshTick) {
        val manager = AppWidgetManager.getInstance(context)
        manager.getAppWidgetIds(
            ComponentName(context, GlassWidgetProvider::class.java)
        ).toList()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsHeroHeader(
            title = "Home screen widgets",
            subtitle = if (ids.isEmpty()) "No Curio widgets placed yet" else "Tap a widget to edit it",
            onBack = { navController.popBackStack() }
        )
        Text(
            text = "Edits apply straight to the widget on your home screen — no launcher support needed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        ids.forEach { id ->
            val mode = GlassWidgetProvider.readMode(context, id)
            val styleLabel = when (val st = GlassWidgetPane.readStyle(context, id)) {
                GlassWidgetPane.STYLE_DEFAULT -> "Samsung blur"
                GlassWidgetPane.STYLE_CUSTOM -> "Custom color"
                else -> runCatching {
                    GlassWidgetPane.Preset.valueOf(st).label + " pane"
                }.getOrDefault(st)
            }
            Card(
                onClick = {
                    context.startActivity(
                        Intent(context, GlassWidgetConfigActivity::class.java)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    )
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Glass widget · ${mode.label}", fontWeight = FontWeight.Bold)
                        Text(
                            styleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Edit",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
