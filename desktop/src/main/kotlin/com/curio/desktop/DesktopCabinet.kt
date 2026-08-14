package com.curio.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val savedDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/** The desktop Cabinet — every topic you saved from the Spin reveal. */
@Composable
internal fun DesktopCabinet() {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Cabinet", "Your saved discoveries")
        if (DesktopEntryStore.entries.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Nothing saved yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Spin a lane, then tap \u201cSave to Cabinet\u201d on the reveal.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
            ) {
                items(DesktopEntryStore.entries, key = { it.id }) { entry ->
                    CabinetRow(entry)
                }
            }
        }
    }
}

@Composable
private fun CabinetRow(entry: DesktopEntry) {
    // Resolve the topic from its lane (a topic can vanish from the data —
    // the row then shows a fallback instead of crashing).
    val topic = remember(entry.id) {
        DesktopCatalog.load(entry.slug).firstOrNull { it.id == entry.topicId }
    }
    val date = remember(entry.savedAt) {
        savedDateFormatter.format(Instant.ofEpochMilli(entry.savedAt).atZone(ZoneId.systemDefault()))
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    topic?.name ?: entry.topicId,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${DesktopCatalog.displayName(entry.slug)} · ${topic?.subtype ?: "saved"} · $date",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            DesktopPill(
                label = "Open",
                active = false,
                onClick = {
                    if (topic != null) {
                        shell.currentTopic = topic
                        shell.browseMode = false
                        shell.screen = DesktopScreen.SPIN
                    }
                }
            )
            Spacer(Modifier.width(6.dp))
            DesktopPill(
                label = "Remove",
                active = false,
                onClick = { DesktopEntryStore.remove(entry.id) }
            )
        }
    }
}
