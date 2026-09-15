package com.curio.app.features.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

@Composable
fun BookCreateScreen(
    onClose: () -> Unit,
    onCreated: () -> Unit = onClose
) {
    BackHandler(onBack = onClose)

    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var subtitle by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var firstChapter by rememberSaveable { mutableStateOf("") }
    var paletteIndex by rememberSaveable { mutableStateOf(0) }

    val palettes = listOf(
        "Dawn" to listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary),
        "Tide" to listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary),
        "Meadow" to listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary),
        "Ink" to listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
    )
    val selectedPalette = palettes[paletteIndex].second
    val canCreate = title.isNotBlank()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onClose,
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CurioIcon(CurioIcons.Close, "Close book creator", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CurioIcon(CurioIcons.MenuBook, null, tint = MaterialTheme.colorScheme.primary, size = 19.dp)
                        Text("New book", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                    Text("Build it one page at a time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.size(44.dp))
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Surface(
                            shape = RoundedCornerShape(19.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier.size(width = 118.dp, height = 170.dp)
                        ) {
                            Box(
                                Modifier.fillMaxSize().background(Brush.linearGradient(selectedPalette)).padding(14.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        title.ifBlank { "Untitled" },
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 25.sp),
                                        color = contentInk(selectedPalette[1]),
                                        maxLines = 4
                                    )
                                    Text(
                                        author.ifBlank { "Your name" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentInk(selectedPalette[1]).copy(alpha = 0.78f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Cover", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Choose a mood for the first edition. The preview updates as you write.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                palettes.forEachIndexed { index, item ->
                                    val selected = paletteIndex == index
                                    Surface(
                                        onClick = { paletteIndex = index },
                                        shape = RoundedCornerShape(15.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Row(Modifier.padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(shape = RoundedCornerShape(5.dp), color = item.second[1], modifier = Modifier.size(14.dp)) {}
                                            Text(item.first, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                        Text("The cover page", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp), color = MaterialTheme.colorScheme.primary)
                        BookTextField(title, { title = it }, "Book title", "Name the world you are making", TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface), true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BookTextField(author, { author = it }, "Author", "You", MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), true, Modifier.weight(1f))
                            BookTextField(subtitle, { subtitle = it }, "Subtitle", "Optional", MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), true, Modifier.weight(1f))
                        }
                        BookTextField(
                            description,
                            { description = it },
                            "A note about this book",
                            "What is it about? What feeling should it leave behind?",
                            TextStyle(fontSize = 17.sp, lineHeight = 28.sp, color = MaterialTheme.colorScheme.onSurface),
                            false,
                            Modifier.fillMaxWidth().heightIn(min = 125.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(CurioIcons.Edit, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 19.dp) }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Begin the story", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Give chapter one a name. The writing comes next.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        BookTextField(firstChapter, { firstChapter = it }, "Chapter 1", "Where the story begins", MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), true, Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(92.dp))
            }
        }

        Surface(
            shape = RoundedCornerShape(25.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(Modifier.navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(if (canCreate) "Ready to begin" else "A title is all you need", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Text(if (firstChapter.isNotBlank()) "Chapter 1 is outlined" else "You can add the first chapter later", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    onClick = onCreated,
                    enabled = canCreate,
                    shape = RoundedCornerShape(20.dp),
                    color = if (canCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.height(46.dp)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AnimatedContent(targetState = canCreate, transitionSpec = { fadeIn(tween(120)) + scaleIn(initialScale = 0.92f) togetherWith fadeOut(tween(90)) + scaleOut(targetScale = 0.92f) }, label = "book-create-icon") { enabled ->
                            CurioIcon(if (enabled) CurioIcons.Check else CurioIcons.MenuBook, null, tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                        }
                        Text("Create book", color = if (canCreate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun BookTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    textStyle: TextStyle,
    singleLine: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(17.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = textStyle,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (value.isBlank()) Text(placeholder, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)))
                            inner()
                        }
                    }
                )
            }
        }
    }
}

private fun contentInk(accent: Color): Color {
    val luminance = (0.299f * accent.red) + (0.587f * accent.green) + (0.114f * accent.blue)
    return if (luminance > 0.58f) Color(0xFF1B1B1D) else Color.White
}