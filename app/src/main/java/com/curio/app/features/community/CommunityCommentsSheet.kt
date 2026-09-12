package com.curio.app.features.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityComment
import com.curio.app.data.supabase.SocialApi
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import kotlinx.coroutines.launch

/**
 * REPLIES — the community's comment sheet.
 *
 * One sheet, used by the wall and by a card's own view, so replies read the
 * same everywhere: drag handle, swipe-down or back to close (the app never
 * puts a close cross in a sheet), the composer riding above the keyboard.
 *
 * The list is capped rather than weighted: a weighted scrollable inside a
 * bottom sheet's wrap-content column is measured with an infinite maximum
 * height and crashes, so the sheet keeps a bounded list and stays short when
 * a card has only one reply.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommunityCommentsSheet(
    card: CommunityCard,
    accessToken: String,
    myUserId: String?,
    onDismiss: () -> Unit,
    /** Called after a reply is added or removed, so the host can refresh its
     *  comment count. */
    onChanged: () -> Unit = {},
    onAddFriend: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var replies by remember { mutableStateOf<List<CommunityComment>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        CommunityApi.comments(accessToken, card.id, myUserId).fold(
            onSuccess = {
                replies = it
                error = null
            },
            onFailure = { error = it.message }
        )
        loading = false
    }

    LaunchedEffect(card.id) { load() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Replies",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = if (card.hoursLeft <= 0L) "expiring"
                        else "${card.hoursLeft}h left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = card.topicName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (replies.isEmpty() && !loading && error == null) {
                    item(key = "empty") {
                        Text(
                            text = "No replies yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(replies, key = { it.id }) { reply ->
                    CommunityReplyRow(
                        reply = reply,
                        onAddFriend = {
                            if (myUserId != null) {
                                scope.launch {
                                    SocialApi.ask(accessToken, reply.authorId, myUserId).fold(
                                        onSuccess = { error = "Friend request sent" },
                                        onFailure = { error = it.message ?: "Could not send request" }
                                    )
                                }
                            } else {
                                onAddFriend(reply.authorId)
                            }
                        },
                        onDelete = {
                            scope.launch {
                                CommunityApi.deleteComment(accessToken, reply.id).fold(
                                    onSuccess = {
                                        load()
                                        onChanged()
                                    },
                                    onFailure = { error = it.message }
                                )
                            }
                        }
                    )
                }
            }

            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= CommunityApi.MAX_COMMENT_CHARS) text = it
                    },
                    label = { Text("Add a reply") },
                    maxLines = 3,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            CommunityApi.comment(
                                accessToken,
                                card.id,
                                text,
                                    AppPreferences.getUsername(context).ifBlank {
                                        AppPreferences.getDisplayName(context)
                                    }

                            ).fold(
                                onSuccess = {
                                    text = ""
                                    load()
                                    onChanged()
                                },
                                onFailure = { error = it.message }
                            )
                        }
                    },
                    enabled = text.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    colors = curioDialogActionButtonColors()
                ) {
                    Text(
                        text = "Send",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

/** One reply: author, when, the words, and a remove for your own. */
@Composable
internal fun CommunityReplyRow(
    reply: CommunityComment,
    onAddFriend: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = reply.authorHandle,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = agoLabel(reply.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (reply.mine) {
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.labelSmall,
                        color = curioDialogActionColor()
                    )
                }
            } else if (reply.authorId.isNotBlank()) {
                TextButton(onClick = onAddFriend) {
                    Text(
                        text = "Add friend",
                        style = MaterialTheme.typography.labelSmall,
                        color = curioDialogActionColor()
                    )
                }
            }
        }
        Text(
            text = reply.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** "just now" / "12m ago" / "3h ago" — replies live at most 24 hours. */
internal fun agoLabel(millis: Long): String {
    if (millis <= 0L) return ""
    val minutes = ((System.currentTimeMillis() - millis).coerceAtLeast(0L)) / 60_000L
    return when {
        minutes < 1L -> "just now"
        minutes < 60L -> "${minutes}m ago"
        else -> "${minutes / 60L}h ago"
    }
}
