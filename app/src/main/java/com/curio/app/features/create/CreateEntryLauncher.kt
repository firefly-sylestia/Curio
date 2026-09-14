package com.curio.app.features.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Presentation-only creation launcher. Navigation and persistence stay with the caller. */
@Composable
fun CreateEntryLauncher(
    modifier: Modifier = Modifier,
    onJournal: () -> Unit,
    onBook: () -> Unit,
    onQuickNote: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "create-plus-rotation"
    )

    Box(
        modifier = modifier.fillMaxSize().navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut() + scaleOut(targetScale = 0.94f, animationSpec = spring())
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.06f)).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = false }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(initialScale = 0.86f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)),
            exit = fadeOut() + scaleOut(targetScale = 0.86f, animationSpec = spring()),
            modifier = Modifier.padding(bottom = 86.dp)
        ) {
            Surface(
                modifier = Modifier.padding(horizontal = 18.dp),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 5.dp,
                shadowElevation = 12.dp
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CreateOption("Start a Journal", "A page for today", "✦", onJournal)
                    CreateOption("Start a Book", "Chapters, pages, thoughts", "▤", onBook)
                    CreateOption("Quick Note", "Capture it before it disappears", "✎", onQuickNote)
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(if (expanded) 62.dp else 58.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = if (expanded) 1.03f else 1f
                    scaleY = if (expanded) 1.03f else 1f
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { expanded = !expanded }
                .semantics { contentDescription = if (expanded) "Close creation menu" else "Create new entry" },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 32.sp, lineHeight = 32.sp, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun CreateOption(title: String, subtitle: String, glyph: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Text(glyph, fontSize = 19.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
