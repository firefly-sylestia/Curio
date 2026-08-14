package com.curio.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The desktop Spin screen: lane bar + deck (2 peek cards) + reveal + browse. */
@Composable
internal fun DesktopSpin() {
    val pool = remember(shell.selectedSlug) { DesktopCatalog.load(shell.selectedSlug) }
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    DesktopCatalog.displayName(shell.selectedSlug),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${pool.size} topics",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DesktopPill("Spin", !shell.browseMode) { shell.browseMode = false }
            Spacer(Modifier.width(8.dp))
            DesktopPill("Browse", shell.browseMode) { shell.browseMode = true }
        }
        LaneChipsRow(Modifier.padding(horizontal = 28.dp))
        Spacer(Modifier.height(10.dp))
        if (shell.browseMode) {
            BrowseList()
        } else {
            SpinPane(pool)
        }
    }
}

@Composable
private fun SpinPane(pool: List<DesktopTopic>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 8.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        DeckStack(pool)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { shell.currentTopic = pickRandomTopic(pool) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(52.dp)
        ) {
            Text(
                "SPIN",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 36.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
        RevealCard()
        Spacer(Modifier.height(16.dp))
        SaveToCabinetPill()
        Spacer(Modifier.height(40.dp))
    }
}

/** Save the revealed topic to the Cabinet (persisted via DesktopEntryStore). */
@Composable
private fun SaveToCabinetPill() {
    val topic = shell.currentTopic ?: return
    val alreadySaved = DesktopEntryStore.entries.any {
        it.slug == shell.selectedSlug && it.topicId == topic.id
    }
    DesktopPill(
        label = if (alreadySaved) "Saved to Cabinet ✓" else "Save to Cabinet",
        active = alreadySaved,
        enabled = !alreadySaved,
        onClick = { DesktopEntryStore.add(shell.selectedSlug, topic.id) }
    )
}

@Composable
private fun DeckStack(pool: List<DesktopTopic>) {
    val topic = shell.currentTopic
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Two peek cards behind the front ticket.
        repeat(2) { i ->
            val backTopic = pool.getOrNull(i + 1) ?: pool.getOrNull(0)
            DeckCard(
                title = backTopic?.name ?: "?",
                subtitle = backTopic?.subtype ?: "",
                dim = 0.45f + i * 0.18f,
                modifier = Modifier
                    .width(300.dp)
                    .height(180.dp)
                    .offset(x = (22 + i * 14).dp, y = (18 + i * 10).dp)
            )
        }
        // Front ticket.
        if (topic != null) {
            DeckCard(
                title = topic.name,
                subtitle = topic.subtype,
                dim = 0f,
                modifier = Modifier
                    .width(340.dp)
                    .height(210.dp)
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(340.dp)
                    .height(210.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Pick a lane, then spin.",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckCard(
    title: String,
    subtitle: String,
    dim: Float,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (dim <= 0f) Coral.copy(alpha = 0.10f) else Color.Transparent,
                    RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    subtitle.ifBlank { "Curio" }.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (dim <= 0f) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    fontSize = if (dim <= 0f) 21.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (dim <= 0f) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RevealCard() {
    val topic = shell.currentTopic ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    topic.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(10.dp))
                if (topic.safeByline.isNotBlank()) {
                    Surface(
                        color = Coral.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            topic.safeByline,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                topic.teaser,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${topic.exploreAction.verb} ${topic.exploreAction.targetName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (topic.exploreAction.durationMinutes > 0) {
                        Text(
                            "~${topic.exploreAction.durationMinutes} min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        topic.exploreAction.instruction,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseList() {
    val pool = remember(shell.selectedSlug) { DesktopCatalog.load(shell.selectedSlug) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
    ) {
        items(pool, key = { it.id }) { topic ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        shell.currentTopic = topic
                        shell.browseMode = false
                    }
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            topic.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (topic.safeByline.isNotBlank()) {
                            Text(
                                topic.safeByline,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        topic.teaser,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
