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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import java.util.Date
import java.util.Locale

/**
 * Writing-first journal composer.
 *
 * The journal is intentionally a writing canvas rather than a form. Secondary
 * metadata is tucked behind lightweight controls so the words stay primary.
 */
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
    var showMoodPicker by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }

    val dateLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }
    val hasContent = title.isNotBlank() || body.isNotBlank()
    val saveScale by animateFloatAsState(
        targetValue = if (hasContent) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 420f),
        label = "journal-save-scale"
    )

    val ruleColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    val marginColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    val moods = listOf(
        "Calm" to CurioIcons.MoodCalm,
        "Curious" to CurioIcons.MoodCurious,
        "Happy" to CurioIcons.MoodHappy,
        "Inspired" to CurioIcons.MoodInspired
    )
    val selectedMoodIcon = moods.firstOrNull { it.first == mood }?.second ?: CurioIcons.MoodCalm

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CurioIcon(
                            CurioIcons.Close,
                            "Close journal",
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = 20.dp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Journal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        if (hasContent) "Draft" else "A quiet page",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurioIcon(
                            CurioIcons.CalendarToday,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 15.dp
                        )
                        Text(
                            dateLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = tween(
                                    260,
                                    easing = FastOutSlowInEasing
                                )
                            )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val lineStep = 31.dp.toPx()
                                val firstLine = 92.dp.toPx()
                                var y = firstLine
                                while (y < size.height) {
                                    drawLine(
                                        color = ruleColor,
                                        start = androidx.compose.ui.geometry.Offset(0f, y),
                                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                    y += lineStep
                                }
                                drawLine(
                                    color = marginColor,
                                    start = androidx.compose.ui.geometry.Offset(34.dp.toPx(), 0f),
                                    end = androidx.compose.ui.geometry.Offset(34.dp.toPx(), size.height),
                                    strokeWidth = 1.5f
                                )
                            }

                            Column(
                                modifier = Modifier.padding(
                                    start = 54.dp,
                                    end = 24.dp,
                                    top = 26.dp,
                                    bottom = 30.dp
                                )
                            ) {
                                Text(
                                    "NEW PAGE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.7.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)
                                )
                                Spacer(Modifier.height(10.dp))

                                BasicTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 44.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (title.isBlank()) {
                                                Text(
                                                    "Give this page a name",
                                                    style = MaterialTheme.typography.displaySmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                                                        lineHeight = 44.sp
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                Spacer(Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    CurioIcon(
                                        CurioIcons.CalendarToday,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 15.dp
                                    )
                                    Text(
                                        dateLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "·",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                    Text(
                                        "Write freely",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(Modifier.height(24.dp))
                                BasicTextField(
                                    value = body,
                                    onValueChange = { body = it },
                                    textStyle = TextStyle(
                                        fontSize = 18.sp,
                                        lineHeight = 31.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 520.dp),
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (body.isBlank()) {
                                                Text(
                                                    "What happened today?\n\nWhat stayed with you?\n\nYou do not have to make it beautiful. Just make it yours.",
                                                    style = TextStyle(
                                                        fontSize = 18.sp,
                                                        lineHeight = 31.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                AnimatedVisibility(
                                    visible = showDetails,
                                    enter = fadeIn(tween(180)) + slideInVertically(
                                        tween(220),
                                        initialOffsetY = { it / 2 }
                                    ),
                                    exit = fadeOut(tween(140)) + slideOutVertically(
                                        tween(180),
                                        targetOffsetY = { it / 2 }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    "A little context",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Text(
                                                    "Optional. Add only what matters.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            CurioIcon(
                                                CurioIcons.Note,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                size = 20.dp
                                            )
                                        }

                                        BasicTextField(
                                            value = tags,
                                            onValueChange = { tags = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth(),
                                            decorationBox = { innerTextField ->
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(
                                                            horizontal = 14.dp,
                                                            vertical = 11.dp
                                                        )
                                                    ) {
                                                        if (tags.isBlank()) {
                                                            Text(
                                                                "Add a few tags",
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                }
            }

            AnimatedVisibility(
                visible = showMoodPicker,
                enter = fadeIn(tween(170)) + slideInVertically(
                    tween(210),
                    initialOffsetY = { it / 2 }
                ),
                exit = fadeOut(tween(130)) + slideOutVertically(
                    tween(160),
                    targetOffsetY = { it / 2 }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        moods.forEach { (label, icon) ->
                            val selected = mood == label
                            Surface(
                                onClick = {
                                    mood = label
                                    showMoodPicker = false
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                modifier = Modifier.height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CurioIcon(
                                        icon,
                                        null,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        size = 18.dp
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showMoodPicker = !showMoodPicker },
                        shape = RoundedCornerShape(22.dp),
                        color = if (showMoodPicker) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        modifier = Modifier.height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            CurioIcon(
                                selectedMoodIcon,
                                "Mood: $mood",
                                tint = MaterialTheme.colorScheme.primary,
                                size = 19.dp
                            )
                            Text(
                                mood,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        onClick = { showDetails = !showDetails },
                        shape = RoundedCornerShape(22.dp),
                        color = if (showDetails) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        modifier = Modifier.height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.Note,
                                "Journal details",
                                tint = MaterialTheme.colorScheme.primary,
                                size = 18.dp
                            )
                            Text(
                                "Details",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Surface(
                        onClick = onSaved,
                        enabled = hasContent,
                        shape = RoundedCornerShape(22.dp),
                        color = if (hasContent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        shadowElevation = if (hasContent) 4.dp else 0.dp,
                        modifier = Modifier
                            .height(46.dp)
                            .graphicsLayer {
                                scaleX = saveScale
                                scaleY = saveScale
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.Check,
                                "Save journal",
                                tint = if (hasContent) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                size = 19.dp
                            )
                            Text(
                                "Save",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (hasContent) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
