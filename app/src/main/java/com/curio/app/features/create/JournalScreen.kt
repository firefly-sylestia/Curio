package com.curio.app.features.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.DatePicker
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit = onClose
) {
    BackHandler(onBack = onClose)

    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf("Calm") }
    var selectedDateMillis by rememberSaveable { mutableStateOf(startOfTodayMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showMoodPicker by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }

    val dateLabel = remember(selectedDateMillis) { formatDate(selectedDateMillis, "EEEE, MMM d") }
    val compactDateLabel = remember(selectedDateMillis) { formatDate(selectedDateMillis, "MMM d") }
    val hasContent = title.isNotBlank() || body.isNotBlank()
    val wordCount = remember(body) {
        body.trim().split(Regex("\\s+")).let { if (it.size == 1 && it[0].isBlank()) 0 else it.size }
    }
    val saveScale by animateFloatAsState(
        targetValue = if (hasContent) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 420f),
        label = "journal-save-scale"
    )

    val moods = listOf(
        "Calm" to CurioIcons.MoodCalm,
        "Curious" to CurioIcons.MoodCurious,
        "Happy" to CurioIcons.MoodHappy,
        "Inspired" to CurioIcons.MoodInspired
    )
    val selectedMoodIcon = moods.firstOrNull { it.first == mood }?.second ?: CurioIcons.MoodCalm

    val background = MaterialTheme.colorScheme.background
    val pageColor = MaterialTheme.colorScheme.surface
    val pageBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val ruleColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    val marginColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Box(
        modifier = Modifier.fillMaxSize().background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, pageBorder),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CurioIcon(CurioIcons.Close, "Close journal", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Journal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text(if (hasContent) "Draft" else "A quiet page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, pageBorder)
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CurioIcon(CurioIcons.CalendarToday, null, tint = MaterialTheme.colorScheme.primary, size = 16.dp)
                        Text(compactDateLabel, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = pageColor,
                        border = BorderStroke(1.dp, pageBorder),
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().animateContentSize(tween(240, easing = FastOutSlowInEasing))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 25.dp, vertical = 23.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp)) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                CurioIcon(CurioIcons.Note, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 18.dp)
                                            }
                                        }
                                        Column {
                                            Text("Journal page", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                            Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text("$wordCount words", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(16.dp))
                                BasicTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 46.sp),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (title.isBlank()) Text("A title for this memory", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f), lineHeight = 46.sp))
                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            Box(Modifier.fillMaxWidth().heightIn(min = 585.dp)) {
                                Canvas(Modifier.matchParentSize()) {
                                    val lineStep = 31.dp.toPx()
                                    val firstLine = 29.dp.toPx()
                                    var y = firstLine
                                    while (y < size.height) {
                                        drawLine(ruleColor, androidx.compose.ui.geometry.Offset(16.dp.toPx(), y), androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), y), 1f)
                                        y += lineStep
                                    }
                                    drawLine(marginColor, androidx.compose.ui.geometry.Offset(29.dp.toPx(), 0f), androidx.compose.ui.geometry.Offset(29.dp.toPx(), size.height), 1.5f)
                                }
                                BasicTextField(
                                    value = body,
                                    onValueChange = { body = it },
                                    textStyle = TextStyle(fontSize = 18.sp, lineHeight = 31.sp, color = MaterialTheme.colorScheme.onSurface),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 585.dp).padding(start = 48.dp, end = 24.dp, top = 6.dp, bottom = 30.dp),
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (body.isBlank()) Text("Start writing here...\n\nDescribe the little things you do not want to forget.\n\nLet the page be honest. It does not have to be perfect.", style = TextStyle(fontSize = 18.sp, lineHeight = 31.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)))
                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = showDetails,
                                enter = fadeIn(tween(180)) + slideInVertically(tween(200), initialOffsetY = { it / 2 }),
                                exit = fadeOut(tween(140)) + slideOutVertically(tween(180), targetOffsetY = { it / 2 })
                            ) {
                                Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(shape = RoundedCornerShape(19.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, pageBorder), modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                                            Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(38.dp)) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    CurioIcon(CurioIcons.BookmarkBorder, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 19.dp)
                                                }
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text("Tags", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                Text("Add a few words to find this memory later.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    BasicTextField(
                                        value = tags,
                                        onValueChange = { tags = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { innerTextField ->
                                            Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                                                Box(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                                                    if (tags.isBlank()) Text("life, travel, people...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f))
                                                    innerTextField()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            AnimatedVisibility(
                visible = showMoodPicker,
                enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.98f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.98f)
            ) {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, pageBorder), tonalElevation = 2.dp, shadowElevation = 3.dp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        moods.forEach { (label, icon) ->
                            val selected = mood == label
                            Surface(onClick = { mood = label; showMoodPicker = false }, shape = RoundedCornerShape(17.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.height(42.dp)) {
                                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CurioIcon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                    Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
            }

            Surface(shape = RoundedCornerShape(25.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, pageBorder), tonalElevation = 2.dp, shadowElevation = 3.dp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = { showMoodPicker = !showMoodPicker }, shape = RoundedCornerShape(20.dp), color = if (showMoodPicker) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.height(44.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurioIcon(selectedMoodIcon, "Mood: $mood", tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                            Text(mood, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                    Spacer(Modifier.width(7.dp))
                    Surface(onClick = { showDetails = !showDetails }, shape = RoundedCornerShape(20.dp), color = if (showDetails) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.height(44.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurioIcon(CurioIcons.BookmarkBorder, "Journal tags", tint = MaterialTheme.colorScheme.secondary, size = 18.dp)
                            Text("Details", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(onClick = onSaved, enabled = hasContent, shape = RoundedCornerShape(20.dp), color = if (hasContent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 2.dp, modifier = Modifier.height(44.dp).graphicsLayer { scaleX = saveScale; scaleY = saveScale }) {
                        Row(Modifier.padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurioIcon(CurioIcons.Check, "Save journal", tint = if (hasContent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                            Text("Save", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = if (hasContent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }) { Text("Set date") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
            ) {
                DatePicker(state = pickerState, showModeToggle = true)
            }
        }
    }
}

private fun startOfTodayMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun formatDate(millis: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))