package com.curio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color

/**
 * The long-press OPTION PILL — a centered solid pill of actions over a
 * dim scrim (the CategoryOptionPill / MixOptionPill language from the
 * category picker). Used wherever a long-press surfaces "more": Cabinet v2
 * collection cards + members, the Recents rows, and the reveal's File-to
 * flow. Tapping the scrim dismisses; the pill itself never auto-dismisses
 * until an action (or scrim) is tapped.
 *
 * Actions render as one line each; [destructive] indexes paint in the
 * error color so delete-style actions read as danger.
 */
@Composable
fun CurioHoldPill(
    title: String? = null,
    actions: List<Pair<String, () -> Unit>>,
    destructiveIndexes: Set<Int> = emptySet(),
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(indication = null, interactionSource = null) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clickable(enabled = false, indication = null, interactionSource = null) {}
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    actions.forEachIndexed { i, (label, action) ->
                        val destructive = i in destructiveIndexes
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = null,
                                    onClick = {
                                        onDismiss()
                                        action()
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (destructive) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}