package com.curio.app.features.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioContentFilter
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityComment
import com.curio.app.data.supabase.CommunityReportReasons
import com.curio.app.data.supabase.ModerationReasons
import com.curio.app.data.supabase.SocialApi
import com.curio.app.data.supabase.RealtimeWatch
import com.curio.app.data.supabase.SupabaseRealtime
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
    /** True when this account may remove someone ELSE's reply (a moderator
     *  holding the replies permission). The database checks again on the call. */
    canModerateReplies: Boolean = false,
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
    // The reply being EDITED: its words load into the composer and the button
    // becomes Save until the edit lands or is dropped.
    var editing by remember { mutableStateOf<CommunityComment?>(null) }
    // Branch roots whose folded answers the member expanded ("show more").
    var expandedRoots by remember { mutableStateOf(setOf<String>()) }
    var friendIds by remember {
        mutableStateOf(AppPreferences.getLocalFriendIds(context))
    }
    var pushed by remember(card.id) { mutableStateOf(0) }
    // The reply a report reason sheet is open for, and the one a moderator is
    // removing — both leave the composer alone.
    var reportingReply by remember { mutableStateOf<CommunityComment?>(null) }
    var moderatingReply by remember { mutableStateOf<CommunityComment?>(null) }
    var moderationBusy by remember { mutableStateOf(false) }

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
        if (myUserId != null) {
            SocialApi.friends(accessToken, myUserId).onSuccess { friends ->
                friendIds = friends.mapTo(mutableSetOf()) { it.person.userId }
            }
        }
    }

    // A socket frame is only a hint; reload through the normal RLS-protected
    // API so replies, edits and deletions update live without trusting payloads.
    DisposableEffect(card.id, accessToken) {
        val owner = "comments:${card.id}"
        SupabaseRealtime.watch(
            owner = owner,
            accessToken = accessToken,
            watches = listOf(
                RealtimeWatch(
                    table = "community_comments",
                    filter = "card_id=eq.${card.id}",
                    events = listOf("INSERT", "UPDATE", "DELETE")
                )
            )
        ) { scope.launch { pushed++ } }
        onDispose { SupabaseRealtime.unwatch(owner) }
    }

    LaunchedEffect(pushed) {
        if (pushed > 0) load()
    }

    // BRANCH ORDER, derived once per reply list — and HERE, in the composable
    // scope: a `LazyColumn`'s content lambda is a LazyListScope, not a
    // composable context, so `remember` cannot live inside it. Each top-level
    // reply is followed by the replies that answer it, so a branch reads under
    // the line it belongs to instead of at the bottom of the sheet.
    val branch = remember(replies, expandedRoots) { branchRenderList(replies, expandedRoots) }

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
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                items(branch.filterIsInstance<BranchRender.Reply>(), key = { it.comment.id }) { node ->
                    val reply = node.comment
                    val depth = node.depth
                    CommunityReplyRow(
                        reply = reply,
                        depth = depth,
                        onAuthor = { if (reply.authorId.isNotBlank()) onOpenProfile(reply.authorId) },
                        onReply = { replyTo = if (replyTo?.id == reply.id) null else reply },
                        onEdit = if (reply.mine && AppPreferences.socialTextEditingState) {
                            {
                                editing = reply
                                text = reply.body
                                replyTo = null
                            }
                        } else null,
                        canAddFriend = myUserId == null || (reply.authorId != myUserId && reply.authorId !in friendIds),
                        onReport = if (reply.mine) null else { { reportingReply = reply } },
                        onModerate = if (!reply.mine && canModerateReplies) {
                            { moderatingReply = reply }
                        } else null,
                        onAddFriend = {
                            if (myUserId != null) {
                                scope.launch {
                                    SocialApi.ask(accessToken, reply.authorId, myUserId).fold(
  onSuccess = {
  friendIds = friendIds + reply.authorId
  AppPreferences.rememberLocalFriend(context, reply.authorId)
  error = "Friend request sent"
                                        },
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
                                        if (editing?.id == reply.id) {
                                            editing = null
                                            text = ""
                                        }
                                        load()
                                        onChanged()
                                    },
                                    onFailure = { error = it.message }
                                )
                            }
                        }
                    )
                }
                items(
                    branch.filterIsInstance<BranchRender.More>(),
                    key = { "more-${it.rootId}" }
                ) { more ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .padding(start = 18.dp)
                            .clickable { expandedRoots = expandedRoots + more.rootId }
                    ) {
                        Text(
                            text = "Show ${more.hidden} more",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = curioDialogActionColor(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // The exact canonical match is surfaced as the person types. The
            // API and schema enforce the same public-text rule on send.
            CurioContentFilter.problem(text)?.let { problem ->
                Text(
                    text = problem,
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
                            text = "Replying to ${target.authorLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        CurioIcon(
                            name = CurioIcons.Close,
                            contentDescription = "Stop replying to ${target.authorLabel}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 14.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { replyTo = null }
                        )
                    }
                }
            }
            // The composer is the app's OWN writing surface (a rounded field
            // with a hairline border that lights up in the brand rose while it
            // has the cursor) instead of Material's outlined box, which sat in
            // this sheet like a form control.
            var composerFocused by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.Bottom) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (composerFocused) curioDialogActionColor()
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = {
                            if (it.length <= CommunityApi.MAX_COMMENT_CHARS) text = it
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(curioDialogActionColor()),
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { composerFocused = it.isFocused }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        decorationBox = { inner ->
                            Box {
                                if (text.isEmpty()) {
                                    Text(
                                        text = when {
                                            editing != null -> "Edit your reply"
                                            replyTo == null -> "Add a reply"
                                            else -> "Reply to ${replyTo?.authorLabel}"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val editTarget = editing
                        val parent = replyTo
                        val handle = AppPreferences.getUsername(context).ifBlank {
                            AppPreferences.getDisplayName(context)
                        }
                        scope.launch {
                            when {
                                editTarget != null -> CommunityApi.editComment(
                                    accessToken, editTarget.id, text
                                ).fold(
                                    onSuccess = {
                                        replies = replies.map { current ->
                                            if (current.id == editTarget.id) current.copy(
                                                body = text,
                                                editedAtMillis = System.currentTimeMillis()
                                            ) else current
                                        }
                                        SocialCommentsCache.write(context, card.id, replies)
                                        editing = null
                                        text = ""
                                    },
                                    onFailure = { error = it.message }
                                )
                                else -> CommunityApi.comment(
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
                        }
                    },
                    enabled = text.isNotBlank() && CurioContentFilter.isClean(text),
                    shape = RoundedCornerShape(50),
                    colors = curioDialogActionButtonColors(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Text(
                        text = if (editing != null) "Save" else "Send",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }

    // Reporting a reply, and a moderator's removal of one — both ask for a
    // reason, both are decided by the database.
    reportingReply?.let { target ->
        ReportTargetDialog(
            title = "Report this reply",
            subtitle = "Reports go to the moderation team, with your reason.",
            reasons = CommunityReportReasons.CONTENT,
            onDismiss = { reportingReply = null },
            onReport = { reason, note ->
                scope.launch {
                    CommunityApi.report(accessToken, "comment", target.id, reason, note).fold(
                        onSuccess = {
                            reportingReply = null
                            error = "Thanks — we'll take a look."
                        },
                        onFailure = { error = it.message }
                    )
                }
            }
        )
    }

    moderatingReply?.let { target ->
        ModerationReasonDialog(
            title = "Remove this reply",
            subtitle = "It disappears from the thread for everyone. The author is not told why.",
            reasons = ModerationReasons.REMOVAL,
            confirmLabel = "Remove",
            busy = moderationBusy,
            onDismiss = { if (!moderationBusy) moderatingReply = null },
            onConfirm = { reason, note ->
                moderationBusy = true
                scope.launch {
                    CommunityApi.removeCommentWithReason(accessToken, target.id, reason, note).fold(
                        onSuccess = {
                            moderatingReply = null
                            moderationBusy = false
                            if (editing?.id == target.id) {
                                editing = null
                                text = ""
                            }
                            load()
                            onChanged()
                        },
                        onFailure = {
                            error = it.message
                            moderationBusy = false
                        }
                    )
                }
            }
        )
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
    canAddFriend: Boolean = true,
    onAddFriend: () -> Unit,
    onDelete: () -> Unit,
    /** Loads the reply's words into the composer as an EDIT — mine only. */
    onEdit: (() -> Unit)? = null,
    /** Files a report on this reply — not offered on your own. */
    onReport: (() -> Unit)? = null,
    /** A moderator's removal of someone else's reply. */
    onModerate: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (depth == 0) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            // A branch is quieter than its parent: the wall's own surface
            // container, one step back — the reply reads as an answer.
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            SocialAvatar(
                style = reply.authorAvatar,
                avatarSize = 30.dp,
                onClick = onAuthor
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 9.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
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
                    // The AGE only: the display name above already says who this
                    // is, and a @handle on every line turns a conversation into
                    // a directory. (A branch's parent is named in the composer
                    // chip, by name as well.)
                    Text(
                        text = agoLabel(reply.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.weight(1f))
                    if (reply.mine) {
                        if (onEdit != null) {
                            ReplyPill(
                                glyph = CurioIcons.Edit,
                                label = "Edit",
                                onClick = onEdit
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        ReplyPill(
                            glyph = CurioIcons.Close,
                            label = "Remove",
                            onClick = onDelete
                        )
                    } else {
                        // Reply arms the composer for this line (branches), and
                        // Add friend stays available beside it. A moderator gets
                        // the removal instead of the report — they are the
                        // person the report would go to.
                        if (reply.authorId.isNotBlank()) {
                            ReplyPill(
                                glyph = CurioIcons.FormatQuote,
                                label = "Reply",
                                onClick = onReply
                            )
                            Spacer(Modifier.width(6.dp))
                            if (canAddFriend) {
                                ReplyPill(
                                    glyph = CurioIcons.Person,
                                    label = "Add",
                                    onClick = onAddFriend
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            if (onModerate != null) {
                                ReplyPill(
                                    glyph = CurioIcons.Delete,
                                    label = "Remove",
                                    onClick = onModerate
                                )
                            } else if (onReport != null) {
                                ReplyPill(
                                    glyph = CurioIcons.Flag,
                                    label = "Report",
                                    onClick = onReport
                                )
                            }
                        }
                    }
                }
                Text(
                    text = reply.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (reply.editedAtMillis != null) {
                    Text(
                        text = "edited",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
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
internal fun branchOrder(replies: List<CommunityComment>): List<Pair<CommunityComment, Int>> {
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

/** One row of a rendered branch: a reply, or a "show N more" door. */
internal sealed class BranchRender {
    internal data class Reply(val comment: CommunityComment, val depth: Int) : BranchRender()
    internal data class More(val rootId: String, val hidden: Int) : BranchRender()
}

/** How many answers show under one top-level reply before the door. */
internal const val BRANCH_PREVIEW = 3

/**
 * The render list for a branch thread: each top-level reply shows its first
 * [BRANCH_PREVIEW] answers, and a branch longer than that folds behind a
 * "show N more" door instead of stretching the page. Roots with a folded
 * branch re-render in full once their door is expanded.
 */
internal fun branchRenderList(
    replies: List<CommunityComment>,
    expandedRoots: Set<String>
): List<BranchRender> {
    val children = replies.filter { it.parentId != null }.groupBy { it.parentId }
    val seen = mutableSetOf<String>()
    val out = ArrayList<BranchRender>(replies.size)
    replies.filter { it.parentId == null }.forEach { root ->
        seen += root.id
        out += BranchRender.Reply(root, 0)
        val answers = children[root.id].orEmpty()
        if (answers.size <= BRANCH_PREVIEW || root.id in expandedRoots) {
            answers.forEach { child ->
                seen += child.id
                out += BranchRender.Reply(child, 1)
            }
        } else {
            answers.take(BRANCH_PREVIEW).forEach { child ->
                seen += child.id
                out += BranchRender.Reply(child, 1)
            }
            out += BranchRender.More(root.id, answers.size - BRANCH_PREVIEW)
        }
    }
    // Orphans (their parent vanished) stay visible at the top level.
    replies.filterNot { it.id in seen }.forEach {
        out += BranchRender.Reply(it, 0)
    }
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
