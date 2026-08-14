package com.curio.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The desktop Settings — appearance, data, and about. */
@Composable
internal fun DesktopSettings() {
    var notice by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Settings", "Appearance, data and about")
        SettingsSection("Appearance") {
            Text(
                "Theme",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Row {
                DesktopPill("Light", !shell.darkMode) { shell.darkMode = false }
                Spacer(Modifier.width(8.dp))
                DesktopPill("Dark", shell.darkMode) { shell.darkMode = true }
            }
        }
        SettingsSection("Data") {
            SettingsActionRow(
                title = "Clear saved entries",
                subtitle = "${DesktopEntryStore.entries.size} entries in the Cabinet",
                actionLabel = "Clear",
                onAction = {
                    DesktopEntryStore.removeAll()
                    notice = "Cabinet cleared"
                }
            )
            SettingsActionRow(
                title = "Reset all preferences",
                subtitle = "Clears the theme, lane, window position and saved entries",
                actionLabel = "Reset",
                onAction = {
                    DesktopPreferences.clear()
                    DesktopEntryStore.removeAll()
                    shell.selectedSlug = "artists"
                    shell.currentTopic = null
                    shell.darkMode = false
                    shell.screen = DesktopScreen.HOME
                    notice = "All preferences reset"
                }
            )
        }
        SettingsSection("About") {
            Text(
                "Curio Desktop",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "The Compose Multiplatform port of the Android app. Spin lanes, " +
                    "explore topics, and save discoveries to your Cabinet — all " +
                    "data lives locally in ~/.curio.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 28.dp, vertical = 10.dp)) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        DesktopPill(label = actionLabel, active = false, onClick = onAction)
    }
}
