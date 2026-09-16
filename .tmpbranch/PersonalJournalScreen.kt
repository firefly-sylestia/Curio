package com.curio.app.features.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.curio.app.data.CaptureData
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreAction
import com.curio.app.data.JournalMood
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun journalDayStart(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun journalDate(millis: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalJournalScreen(onClose: () -> Unit, onSaved: () -> Unit = onClose) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var moodName by rememberSaveable { mutableStateOf(JournalMood.CALM.name) }
    var selectedDateMillis by rememberSaveable { mutableStateOf(journalDayStart()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showMoodPicker by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    val hasContent = title.isNotBlank() || body.isNotBlank()
    val words = remember(body) { body.trim().split(Regex("\\s+")).let { if (it.size == 1 && it[0].isBlank()) 0 else it.size } }
    val mood = remember(moodName) { runCatching { JournalMood.valueOf(moodName) }.getOrDefault(JournalMood.CALM) }
    val dateLabel = remember(selectedDateMillis) { journalDate(selectedDateMillis, "EEEE, MMM d, yyyy") }
    val compactDate = remember(selectedDateMillis) { journalDate(selectedDateMillis, "MMM d") }
    val pageRuleColor = MaterialTheme.colorScheme.outline.copy(alpha = .10f)
    val pageMarginColor = MaterialTheme.colorScheme.primary.copy(alpha = .18f)
    val moods = listOf(JournalMood.CALM, JournalMood.HAPPY, JournalMood.CURIOUS, JournalMood.INSPIRED, JournalMood.TIRED, JournalMood.OVERWHELMED)

    fun save() {
        if (!hasContent || saving) return
        saving = true
        scope.launch {
            val entry = CurioEntry(
                id = newCreatedEntryId(),
                topic = CurioTopic(
                    id = newCreatedEntryId(),
                    categoryId = CategoryId.WILDCARD,
                    subtype = "Journal",
                    name = title.trim().ifBlank { "Untitled journal" },
                    teaser = body.trim().take(180).ifBlank { "A personal journal entry." },
                    imageUrl = "",
                    exploreAction = ExploreAction("Reflect", "Personal journal", 0, "Return to this page whenever you want to reflect."),
                    tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                ),
                format = CaptureFormat.Marginalia,
                captureData = CaptureData.Marginalia(
                    journalText = body.trim(),
                    quotes = emptyList(),
                    mood = mood
                ),
                title = title.trim().ifBlank { "Untitled journal" },
                capturedAtMillis = selectedDateMillis,
                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            )
            runCatching { saveCreatedEntryToPersonalCollection(context, entry) }
                .onSuccess { onSaved() }
                .onFailure { saving = false }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(onClick = onClose, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(CurioIcons.Close, "Close journal", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Journal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text(if (hasContent) "$words words" else "A quiet page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(onClick = { showDatePicker = true }, shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CurioIcon(CurioIcons.CalendarToday, null, tint = MaterialTheme.colorScheme.primary, size = 16.dp)
                        Text(compactDate, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 5.dp)) {
                Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f)), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth().animateContentSize(tween(220))) {
                    Column(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 26.dp, vertical = 24.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(CurioIcons.MenuBook, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 18.dp) } }
                                    Column {
                                        Text("Journal page", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                        Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("$words words", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(16.dp))
                            BasicTextField(value = title, onValueChange = { title = it }, singleLine = true, textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 46.sp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Box { if (title.isBlank()) Text("A title for this memory", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .20f), lineHeight = 46.sp)); inner() } })
                        }
                        Box(Modifier.fillMaxWidth().heightIn(min = 620.dp)) {
                            Canvas(Modifier.matchParentSize()) {
                                val step = 31.dp.toPx(); var y = 29.dp.toPx()
                                while (y < size.height) { drawLine(pageRuleColor, androidx.compose.ui.geometry.Offset(16.dp.toPx(), y), androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), y), 1f); y += step }
                                drawLine(pageMarginColor, androidx.compose.ui.geometry.Offset(29.dp.toPx(), 0f), androidx.compose.ui.geometry.Offset(29.dp.toPx(), size.height), 1.5f)
                            }
                            BasicTextField(value = body, onValueChange = { body = it }, textStyle = TextStyle(fontSize = 18.sp, lineHeight = 31.sp, color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().heightIn(min = 620.dp).padding(start = 48.dp, end = 24.dp, top = 6.dp, bottom = 30.dp), decorationBox = { inner -> Box { if (body.isBlank()) Text("Start writing here...\n\nDescribe the little things you do not want to forget.\n\nLet the page be honest. It does not have to be perfect.", style = TextStyle(fontSize = 18.sp, lineHeight = 31.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .28f))); inner() } })
                        }
                        AnimatedVisibility(visible = showDetails, enter = fadeIn(tween(160)) + slideInVertically(tween(180), initialOffsetY = { it / 2 }), exit = fadeOut(tween(120))) {
                            Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BasicTextField(value = tags, onValueChange = { tags = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) { Box(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) { if (tags.isBlank()) Text("life, travel, people...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f)); inner() } } })
                            }
                        }
                        Surface(onClick = { showDetails = !showDetails }, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                CurioIcon(CurioIcons.BookmarkBorder, null, tint = MaterialTheme.colorScheme.secondary, size = 18.dp)
                                Spacer(Modifier.width(8.dp)); Text("Details", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)); Spacer(Modifier.weight(1f))
                                Text(if (showDetails) "Hide" else "Tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(onClick = { showMoodPicker = !showMoodPicker }, shape = RoundedCornerShape(18.dp), color = if (showMoodPicker) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            CurioIcon(CurioIcons.MoodCalm, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp); Text(mood.label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                    Surface(onClick = { showDatePicker = true }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            CurioIcon(CurioIcons.CalendarToday, null, tint = MaterialTheme.colorScheme.secondary, size = 18.dp); Text(dateLabel, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
                if (showMoodPicker) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            moods.forEach { option ->
                                Surface(onClick = { moodName = option.name; showMoodPicker = false }, shape = CircleShape, color = if (option == mood) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.size(42.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text(option.label.take(1), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(100.dp))
            }
        }
        Surface(shape = RoundedCornerShape(25.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)), tonalElevation = 2.dp, shadowElevation = 2.dp, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) { CurioIcon(CurioIcons.Bookmark, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp); Spacer(Modifier.width(8.dp)); Text("Personal collection", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
                Surface(onClick = ::save, enabled = hasContent && !saving, shape = RoundedCornerShape(20.dp), color = if (hasContent && !saving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.height(46.dp)) { Text(if (saving) "Saving…" else "Save journal", modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp), color = if (hasContent && !saving) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = { pickerState.selectedDateMillis?.let { selectedDateMillis = it }; showDatePicker = false }) { Text("Set date") }
        }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }) {
            androidx.compose.material3.DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}
