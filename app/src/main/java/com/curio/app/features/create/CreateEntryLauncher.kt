package com.curio.app.features.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/** Fixed Home creation launcher. Its caller owns navigation and visibility. */
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
        animationSpec = spring(dampingRatio = 1f, stiffness = 360f),
        label = "create-plus-rotation"
    )

    val fabColor = MaterialTheme.colorScheme.secondaryContainer
    val fabContentColor = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(160, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = false }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 420f)
                ),
            exit = fadeOut(tween(130, easing = FastOutSlowInEasing)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 154.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 5.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CreateOption(
                        title = "Start a Journal",
                        subtitle = "A page for today",
                        icon = CurioIcons.Note,
                        onClick = {
                            expanded = false
                            onJournal()
                        }
                    )
                    CreateOption(
                        title = "Start a Book",
                        subtitle = "Chapters, pages, thoughts",
                        icon = CurioIcons.MenuBook,
                        onClick = {
                            expanded = false
                            onBook()
                        }
                    )
                    CreateOption(
                        title = "Quick Note",
                        subtitle = "Capture it before it disappears",
                        icon = CurioIcons.Edit,
                        onClick = {
                            expanded = false
                            onQuickNote()
                        }
                    )
                }
            }
        }

        Surface(
            onClick = { expanded = !expanded },
            shape = CircleShape,
            color = fabColor,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 86.dp)
                .size(if (expanded) 60.dp else 56.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = if (expanded) 1.02f else 1f
                    scaleY = if (expanded) 1.02f else 1f
                }
                .semantics {
                    contentDescription = if (expanded) "Close creation menu" else "Create new entry"
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CurioIcon(
                    CurioIcons.Add,
                    contentDescription = null,
                    tint = fabContentColor,
                    size = 28.dp
                )
            }
        }
    }
}

@Composable
private fun CreateOption(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(21.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        size = 20.dp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    title,
                    style = MaterialTheme.typography.titleSmall
                )
                androidx.compose.material3.Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CurioIcon(
                CurioIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 19.dp
            )
        }
    }
}
