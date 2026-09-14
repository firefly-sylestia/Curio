package com.curio.app.features.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookCreateScreen(
    onClose: () -> Unit,
    onCreated: () -> Unit = onClose
) {
    BackHandler(onBack = onClose)

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var firstChapter by remember { mutableStateOf("") }
    var colorIndex by remember { mutableStateOf(0) }

    val covers = listOf(
        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary),
        listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary),
        listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary),
        listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "New book",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            "Create a world of your own",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        CurioIcon(
                            CurioIcons.Close,
                            "Close book creator",
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = 22.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = onCreated,
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text("Create book") }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(width = 108.dp, height = 150.dp),
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = covers[colorIndex]
                                )
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = contentInk(covers[colorIndex][1]),
                                maxLines = 3
                            )
                            if (author.isNotBlank()) {
                                Text(
                                    author,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentInk(covers[colorIndex][1]).copy(alpha = 0.82f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Cover mood",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Pick a simple palette. You can change the cover later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        covers.forEachIndexed { index, colors ->
                            FilterChip(
                                selected = colorIndex == index,
                                onClick = { colorIndex = index },
                                label = {
                                    Text(
                                        when (index) {
                                            0 -> "Rose"
                                            1 -> "Sky"
                                            2 -> "Garden"
                                            else -> "Ink"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Book title") },
                placeholder = { Text("The title of your next world") },
                shape = RoundedCornerShape(18.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Author") },
                    placeholder = { Text("You") },
                    shape = RoundedCornerShape(18.dp)
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Subtitle") },
                    placeholder = { Text("Optional") },
                    shape = RoundedCornerShape(18.dp)
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                label = { Text("Description") },
                placeholder = { Text("What is this book about?") },
                shape = RoundedCornerShape(22.dp)
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.MenuBook,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                size = 19.dp,
                                modifier = Modifier.padding(9.dp)
                            )
                        }
                        Column {
                            Text(
                                "First chapter",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Start with a chapter name, then build from there.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedTextField(
                        value = firstChapter,
                        onValueChange = { firstChapter = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Chapter 1 title") },
                        placeholder = { Text("Where it begins") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun contentInk(accent: Color): Color {
    val luminance = (0.299f * accent.red) + (0.587f * accent.green) + (0.114f * accent.blue)
    return if (luminance > 0.58f) Color(0xFF1B1B1D) else Color.White
}
