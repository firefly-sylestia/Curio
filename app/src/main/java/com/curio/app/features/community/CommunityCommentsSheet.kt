package com.curio.app.features.community

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.Surface
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
import com.curio.app.data.CurioContentFilter
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityComment
import com.curio.app.data.supabase.SocialApi
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
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
    onAddFriend: (String) -> Unit = {},
    /** Opens a reply author's profile — the second place a member is reachable
     *  from, next to the card's own author row. */
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var replies by remember { mutableStateOf<List<CommunityComment>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }
    // BRANCHED REPLIES: the reply this one answers. Null = a top-level reply;
    // the chip above the composer names the person, so a branch is never
    // posted at the wrong place by accident.
    var replyTo by remember { mutableStateOf<CommunityComment?>(null) }

    suspend fun load() {
        loading = true
        CommunityApi.comments(accessToken, card.id, myUserId).fold(
            onSuccess = {
                replies = it
                // Remembered per card, so opening the same sheet again draws
                // its replies on the first frame instead of on a spinner.
                SocialCommentsCache.write(context, card.id, it)
                error = null
            },
            onFailure = { error = it.message }
        )
        loading = false
    }

    LaunchedEffect(card.id) {
        // The device's copy FIRST — the replies, their branches and the reply
        // pills are on screen before the network is asked. The network then
        // replaces it, and only an empty list is ever filled from the cache.
        if (replies.isEmpty()) {
            val cached = SocialCommentsCache.read(context, card.id, myUserId)
            if (cached.isNotEmpty()) replies = cached
        }
        load()
    }

    // BRANCH ORDER, derived once per reply list — and HERE, in the composable
    // scope: a `LazyColumn`'s content lambda is a LazyListScope, not a
    // composable context, so `remember` cannot live inside it. Each top-level
    // reply is followed by the replies that answer it, so a branch reads under
    // the line it belongs to instead of at the bottom of the sheet.
    val branch = remember(replies) { branchOrder(replies) }

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
                items(branch, key = { it.first.id }) { (reply, depth) ->
                    CommunityReplyRow(
                        reply = reply,
                        depth = depth,
                        onAuthor = { if (reply.authorId.isNotBlank()) onOpenProfile(reply.authorId) },
                        onReply = { replyTo = if (replyTo?.id == reply.id) null else reply },
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

            // v3xx53 — the app-level filter, surfaced as you type: send stays
            // disabled and the line says why (the API refuses it again on the
            // way out, and the schema's CHECK is the third gate).
            if (CurioContentFilter.carriesBadWord(text)) {
                Text(
                    text = CurioContentFilter.BLOCKED_MESSAGE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Who this reply answers — the one line that makes a branch
            // explicit, and the only way to leave it again.
            replyTo?.let { target ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = "Replying to @${target.authorLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        CurioIcon(
                            name = CurioIcons.Close,
                            contentDescription = "Stop replying to @${target.authorLabel}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 14.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { replyTo = null }
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= CommunityApi.MAX_COMMENT_CHARS) text = it
                    },
                    label = {
                        Text(
                            if (replyTo == null) "Add a reply"
                            else "Reply to @${replyTo?.authorLabel}"
                        )
                    },
                    maxLines = 3,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val parent = replyTo
                        val handle = AppPreferences.getUsername(context).ifBlank {
                            AppPreferences.getDisplayName(context)
                        }
                        scope.launch {
                            CommunityApi.comment(
                                accessToken,
                                card.id,
                                text,
                                handle,
                                parentId = parent?.id
                            ).fold(
                                onSuccess = {
                                    text = ""
                                    replyTo = null
                                    load()
                                    onChanged()
                                },
                                onFailure = { error = it.message }
                            )
                        }
                    },
                    enabled = text.isNotBlank() && CurioContentFilter.isClean(text),
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

/**
 * One reply: the author's PORTRAIT and live username (tap either to open their
 * profile), when it was written, the words in a card of their own, and one
 * compact pill for the only action that fits — Remove on your own reply,
 * Add friend on someone else's.
 */
@Composable
internal fun CommunityReplyRow(
    reply: CommunityComment,
    /** 0 = a reply to the card, 1 = a reply inside a branch. */
    depth: Int,
    onAuthor: () -> Unit,
    /** Arms the composer to answer THIS reply — the branch's own door. */
    onReply: () -> Unit,
    onAddFriend: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (depth == 0) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            // A branch is quieter than its parent: the wall's own surface
            // container, one step back — the reply reads as an answer.
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 18).dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            SocialAvatar(
                style = reply.authorAvatar,
                avatarSize = 34.dp,
                onClick = onAuthor
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // The DISPLAY name leads; the @username rides the meta line
                // beneath it, beside the age and the reply's own actions.
                Text(
                    text = reply.authorLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.clickable(onClick = onAuthor)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${reply.authorHandleLabel} · ${agoLabel(reply.createdAtMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.weight(1f))
                    if (reply.mine) {
                        ReplyPill(
                            glyph = CurioIcons.Close,
                            label = "Remove",
                            onClick = onDelete
                        )
                    } else {
                        // Reply arms the composer for this line (branches), and
                        // Add friend stays available beside it.
                        if (reply.authorId.isNotBlank()) {
                            ReplyPill(
                                glyph = CurioIcons.FormatQuote,
                                label = "Reply",
                                onClick = onReply
                            )
                            Spacer(Modifier.width(6.dp))
                            ReplyPill(
                                glyph = CurioIcons.Person,
                                label = "Add",
                                onClick = onAddFriend
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
    }
}

/** A compact action pill — icon plus a label, the app's own pill language. */
@Composable
private fun ReplyPill(glyph: String, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                tint = curioDialogActionColor(),
                size = 13.dp
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = curioDialogActionColor()
            )
        }
    }
}

/**
 * Branch order: every top-level reply, each immediately followed by the
 * replies that answer it. A reply whose parent was removed (or that arrived
 * with a parent we cannot see) still renders — at the top level — so nothing
 * a member wrote is ever hidden.
 */
private fun branchOrder(replies: List<CommunityComment>): List<Pair<CommunityComment, Int>> {
    val children = replies.filter { it.parentId != null }.groupBy { it.parentId }
    val out = ArrayList<Pair<CommunityComment, Int>>(replies.size)
    replies.filter { it.parentId == null }.forEach { root ->
        out += root to 0
        children[root.id].orEmpty().forEach { child -> out += child to 1 }
    }
    val placed = out.map { it.first.id }.toSet()
    replies.filterNot { it.id in placed }.forEach { out += it to 0 }
    return out
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
