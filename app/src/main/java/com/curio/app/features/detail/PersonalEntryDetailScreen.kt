package com.curio.app.features.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.CaptureData
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.JournalMood
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PersonalEntryDetailScreen(entryId: String, navController: NavController) {
    BackHandler { navController.popBackStack() }
    val entry by produceState<CurioEntry?>(initialValue = null, entryId) {
        runCatching { CurioRepositoryHolder.repo.observeById(entryId).collect { value = it } }
    }
    val resolved = entry ?: run {
        PersonalEntryLoading(onBack = { navController.popBackStack() })
        return
    }
    if (resolved.topic.subtype.equals("Book", ignoreCase = true)) {
        PersonalBookView(resolved, onBack = { navController.popBackStack() })
    } else {
        PersonalJournalView(resolved, onBack = { navController.popBackStack() })
    }
}

@Composable
private fun PersonalEntryLoading(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Opening your keepsake…", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Loading the personal page", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PersonalBookView(entry: CurioEntry, onBack: () -> Unit) {
    val chapters = entry.topic.chapters.orEmpty()
    val data = entry.captureData as? CaptureData.Marginalia
    val body = data?.journalText.orEmpty()
    val date = remember(entry.capturedAtMillis) { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.capturedAtMillis)) }
    val accent = MaterialTheme.colorScheme.primary

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 92.dp)) {
            Box(Modifier.fillMaxWidth().height(430.dp).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)))) {
                Surface(onClick = onBack, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .94f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)), modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 10.dp).size(44.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(CurioIcons.Close, "Back", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp) }
                }
                Column(Modifier.align(Alignment.BottomCenter).padding(horizontal = 28.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), shadowElevation = 12.dp, modifier = Modifier.width(154.dp).height(220.dp)) {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, accent))).padding(17.dp), contentAlignment = Alignment.BottomStart) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CURIO EDITION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(entry.title ?: entry.topic.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp), color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 5, overflow = TextOverflow.Ellipsis)
                                if (entry.topic.byline.isNotBlank()) Text(entry.topic.byline, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .8f))
                            }
                        }
                    }
                    Text("Personal book · $date", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(Modifier.padding(horizontal = 18.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (!entry.topic.synopsis.isNullOrBlank()) {
                    Text(entry.topic.synopsis.orEmpty(), style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersonalMetaChip("${chapters.size} chapters", CurioIcons.MenuBook)
                    PersonalMetaChip("Personal", CurioIcons.Bookmark)
                }
                Text("Contents", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                if (chapters.isEmpty()) {
                    PersonalChapterPage("", body)
                } else {
                    chapters.forEachIndexed { index, chapter ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CHAPTER ${index + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp), color = accent)
                                Text(chapter.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                if (chapter.summary.isNotBlank()) Text(chapter.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    PersonalChapterPage("", body)
                }
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CurioIcon(CurioIcons.MenuBook, null, tint = accent, size = 20.dp); Spacer(Modifier.width(9.dp)); Text("Your personal book", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)); Spacer(Modifier.weight(1f)); Text("${body.trim().split(Regex("\\s+")).count { it.isNotBlank() }} words", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PersonalJournalView(entry: CurioEntry, onBack: () -> Unit) {
    val data = entry.captureData as? CaptureData.Marginalia
    val mood = data?.mood
    val date = remember(entry.capturedAtMillis) { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(entry.capturedAtMillis)) }
    val rules = MaterialTheme.colorScheme.outline.copy(alpha = .10f)
    val margin = MaterialTheme.colorScheme.primary.copy(alpha = .18f)
    val body = data?.journalText.orEmpty()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 88.dp)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(CurioIcons.Close, "Back", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp) }
                }
                Spacer(Modifier.width(12.dp)); Column { Text("Journal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)); Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(CurioIcons.MenuBook, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 18.dp) } }
                            Text("A day worth keeping", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(entry.title ?: "Untitled journal", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 43.sp))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            PersonalMetaChip(date, CurioIcons.CalendarToday)
                            mood?.let { PersonalMetaChip(it.label, CurioIcons.MoodCalm) }
                        }
                        Spacer(Modifier.height(22.dp))
                        Box(Modifier.fillMaxWidth().heightIn(min = 520.dp)) {
                            Canvas(Modifier.matchParentSize()) {
                                var y = 27.dp.toPx(); val step = 31.dp.toPx()
                                while (y < size.height) { drawLine(rules, androidx.compose.ui.geometry.Offset(12.dp.toPx(), y), androidx.compose.ui.geometry.Offset(size.width - 12.dp.toPx(), y), 1f); y += step }
                                drawLine(margin, androidx.compose.ui.geometry.Offset(25.dp.toPx(), 0f), androidx.compose.ui.geometry.Offset(25.dp.toPx(), size.height), 1.5f)
                            }
                            Text(body, modifier = Modifier.fillMaxWidth().padding(start = 42.dp, end = 14.dp, top = 4.dp, bottom = 26.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 31.sp))
                        }
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CurioIcon(CurioIcons.Bookmark, null, tint = MaterialTheme.colorScheme.primary, size = 19.dp); Spacer(Modifier.width(8.dp)); Text("Saved in Personal", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)); Spacer(Modifier.weight(1f)); Text("${body.trim().split(Regex("\\s+")).count { it.isNotBlank() }} words", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PersonalMetaChip(text: String, icon: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CurioIcon(icon, null, tint = MaterialTheme.colorScheme.primary, size = 15.dp)
            Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun PersonalChapterPage(label: String, text: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp)) {
            if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 29.sp))
        }
    }
}
