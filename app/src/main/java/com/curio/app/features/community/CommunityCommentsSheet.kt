package com.curio.app.features.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
import com.curio.app.ui.theme.curioSheetContainerColor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioContentFilter
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityComment
import com.curio.app.data.supabase.CommunityReportReasons
import com.curio.app.data.supabase.ModerationReasons
import com.curio.app.data.supabase.SocialApi
import com.curio.app.ui.components.CurioEmptyLine
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
    val listState = rememberLazyListState()

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
    // Replies whose folded answers the member expanded ("show more"). Keyed
    // by reply id, at ANY depth — a branch can fold its own answers too.
    var expandedNodes by remember { mutableStateOf(setOf<String>()) }
    // The reply that was just written, so the list can bring it into view
    // instead of leaving it below the fold of a 320dp thread window.
    var focusReplyId by remember { mutableStateOf<String?>(null) }
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

    /**
     * Hearts one reply, or takes the heart back.
     *
     * A heart is a single row, so it is drawn the moment it is tapped and put
     * back exactly as it was if the server refuses — the alternative (a spinner
     * on a pill) makes a one-tap gesture feel like a form submission.
     */
    fun toggleHeart(reply: CommunityComment) {
        val me = myUserId ?: return
        val optimistic = reply.copy(
            likes = (reply.likes + if (reply.likedByMe) -1 else 1).coerceAtLeast(0),
            likedByMe = !reply.likedByMe
        )
        fun draw(updated: CommunityComment) {
            replies = replies.map { if (it.id == reply.id) updated else it }
            SocialCommentsCache.write(context, card.id, replies)
        }
        draw(optimistic)
        scope.launch {
            val result = if (optimistic.likedByMe) {
                CommunityApi.likeComment(accessToken, reply.id, me)
            } else {
                CommunityApi.unlikeComment(accessToken, reply.id, me)
            }
            result.onFailure {
                draw(reply)
                error = it.message
            }
        }
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

    // THE RENDER LIST, derived once per reply list — and HERE, in the composable
    // scope: a `LazyColumn`'s content lambda is a LazyListScope, not a
    // composable context, so `remember` cannot live inside it. Each reply is
    // followed by the replies that answer it, to any depth (see
    // [branchRenderList]), so a branch reads under the line it belongs to.
    val branch = remember(replies, expandedNodes) { branchRenderList(replies, expandedNodes) }

    // A reply that was just written is BROUGHT INTO VIEW once the render list
    // contains it (the id is set before the reload lands).
    LaunchedEffect(focusReplyId, branch) {
        val id = focusReplyId ?: return@LaunchedEffect
        val index = branch.indexOfFirst { it is BranchRender.Reply && it.comment.id == id }
        if (index >= 0) {
            runCatching { listState.animateScrollToItem(index) }
            focusReplyId = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioSheetContainerColor(MaterialTheme.colorScheme.surface),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        CurioGlassWindowBlur()
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
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (replies.isEmpty() && !loading && error == null) {
                    item(key = "empty") {
                        // v439 — a bare list under a surface that has already named
                        // itself says nothing but a dash (see [CurioEmptyLine]).
                        CurioEmptyLine()
                    }
                }
                // ONE pass over the whole render list, in render order. A
                // reply and the door that unfolds its OWN answers are
                // consecutive rows, so a branch always reads as a branch. (Two
                // separate `items()` calls — one per render type — collected
                // every door at the bottom of the sheet, which is a large part
                // of why a branching thread looked glitchy.)
                items(branch, key = { it.key }) { row ->
                    when (row) {
                        is BranchRender.Reply -> {
                            val reply = row.comment
                            CommunityReplyRow(
                                reply = reply,
                                depth = row.depth,
                                onAuthor = {
                                    if (reply.authorId.isNotBlank()) {
                                        onOpenProfile(reply.authorId)
                                    }
                                },
                                onReply = {
                                    replyTo = if (replyTo?.id == reply.id) null else reply
                                },
                                onToggleLike = { toggleHeart(reply) },
                                onEdit = if (reply.mine && AppPreferences.socialTextEditingState) {
                                    {
                                        editing = reply
                                        text = reply.body
                                        replyTo = null
                                    }
                                } else null,
                                canAddFriend = myUserId == null ||
                                    (reply.authorId != myUserId && reply.authorId !in friendIds),
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
                                                    AppPreferences.rememberLocalFriend(
                                                        context,
                                                        reply.authorId
                                                    )
                                                    error = "Friend request sent"
                                                },
                                                onFailure = {
                                                    error = it.message ?: "Could not send request"
                                                }
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
                        is BranchRender.More -> ShowMoreReplies(
                            hidden = row.hidden,
                            depth = row.depth,
                            onClick = { expandedNodes = expandedNodes + row.nodeId }
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
                                    parentId = parent?.id,
                                    myUserId = myUserId
                                ).fold(
                                    onSuccess = { created ->
                                        text = ""
                                        replyTo = null
                                        // The server's OWN row goes straight
                                        // into the thread: the reply is on
                                        // screen the instant it is sent,
                                        // instead of waiting for a thread read
                                        // to come back and prove it exists.
                                        replies = replies + created
                                        SocialCommentsCache.write(context, card.id, replies)
                                        // A fresh answer is never hidden behind
                                        // its own branch's door.
                                        created.parentId?.let { parentId ->
                                            expandedNodes = expandedNodes + parentId
                                        }
                                        focusReplyId = created.id
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
 * One reply: the author's PORTRAIT and live name (tap either to open their
 * profile), the age and edit mark on that same line, the words, and the row of
 * actions underneath.
 *
 * THREE lines, always the same three: `name · age`, the body, the actions. The
 * reply is the unit a thread is read by, so the row stays compact (a branch's
 * worth of them has to fit in a 320dp sheet) and the actions always sit in the
 * same place: the heart, Reply on ANY reply — your own lines included, because
 * answering yourself is how a long thread keeps going — then the owner's or the
 * reader's own set. Secondary actions are ICON-ONLY pills, which is what keeps
 * four of them on one line even at the deepest indent.
 */
@Composable
internal fun CommunityReplyRow(
    reply: CommunityComment,
    /** 0 = a reply to the card, 1+ = how deep inside a branch it answers. */
    depth: Int,
    onAuthor: () -> Unit,
    /** Arms the composer to answer THIS reply — the branch's own door. */
    onReply: () -> Unit,
    /** Hearts it, or takes the heart back. */
    onToggleLike: () -> Unit,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // UNLIMITED NESTING, BOUNDED INDENT: an answer always steps right
            // of the line it answers, but the step stops after
            // [MAX_REPLY_INDENT] levels so a ten-deep thread keeps a usable
            // reading width — the thread rule carries the rest of the depth.
            .padding(start = (depth.coerceAtMost(MAX_REPLY_INDENT) * REPLY_INDENT_STEP_DP).dp)
            // IntrinsicSize.Min is what makes fillMaxHeight legal on the rule
            // below: a wrap-content row hands its children an INFINITE maximum
            // height, and fillMaxHeight on an infinite constraint has nothing
            // to resolve against.
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        if (depth > 0) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (depth == 0) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                // A branch is quieter than its parent: the ladder's nested
                // step, one rung back from the white card — the reply reads as
                // an answer. OPAQUE (v408): a 45% wash resolved to very nearly
                // the parent card it was laid on.
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SocialAvatar(
                        seed = blobatarSeed(reply.authorId, reply.authorName),
                        avatarSize = 26.dp,
                        onClick = onAuthor
                    )
                    Text(
                        text = reply.authorLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // `fill = false`: a long name is ellipsised instead of
                        // pushing the age (`name · age`, one meta line) off the
                        // edge of the bubble.
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = 8.dp)
                            .clickable(onClick = onAuthor)
                    )
                    Text(
                        // The AGE and the edit mark ride the NAME's line: one
                        // meta line, not two. The @handle is left out on
                        // purpose — a branch's parent is named in the composer
                        // chip, and a handle on every line turns a conversation
                        // into a directory.
                        text = if (reply.editedAtMillis != null) {
                            "${agoLabel(reply.createdAtMillis)} · edited"
                        } else {
                            agoLabel(reply.createdAtMillis)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Text(
                    text = reply.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ReplyPill(
                        glyph = CurioIcons.Favorite,
                        label = if (reply.likes > 0) reply.likes.toString() else "Like",
                        contentDescription = if (reply.likedByMe) {
                            "Take your heart back"
                        } else {
                            "Heart this reply"
                        },
                        tint = if (reply.likedByMe) {
                            curioDialogActionColor()
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = onToggleLike
                    )
                    Spacer(Modifier.width(6.dp))
                    ReplyPill(
                        glyph = CurioIcons.FormatQuote,
                        label = "Reply",
                        contentDescription = "Reply to ${reply.authorLabel}",
                        onClick = onReply
                    )
                    Spacer(Modifier.width(6.dp))
                    if (reply.mine) {
                        if (onEdit != null) {
                            ReplyPill(
                                glyph = CurioIcons.Edit,
                                label = null,
                                contentDescription = "Edit your reply",
                                onClick = onEdit
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        ReplyPill(
                            glyph = CurioIcons.Close,
                            label = null,
                            contentDescription = "Remove your reply",
                            onClick = onDelete
                        )
                    } else if (reply.authorId.isNotBlank()) {
                        // Add friend stays available on someone else's line; a
                        // moderator gets the removal instead of the report —
                        // they are the person the report would go to.
                        if (canAddFriend) {
                            ReplyPill(
                                glyph = CurioIcons.Person,
                                label = null,
                                contentDescription = "Add ${reply.authorLabel} as a friend",
                                onClick = onAddFriend
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        if (onModerate != null) {
                            ReplyPill(
                                glyph = CurioIcons.Delete,
                                label = null,
                                contentDescription = "Remove this reply",
                                onClick = onModerate
                            )
                        } else if (onReport != null) {
                            ReplyPill(
                                glyph = CurioIcons.Flag,
                                label = null,
                                contentDescription = "Report this reply",
                                onClick = onReport
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A compact action pill — icon plus an optional label, the app's own pill
 * language.
 *
 * `label = null` draws an ICON-ONLY pill, which is the size that lets four
 * actions share one line at the deepest indentation; [contentDescription] then
 * carries the meaning, because a glyph alone is not an answer for a screen
 * reader. [tint] is what paints a control's STATE (a heart you already gave is
 * the brand rose, one you have not is quiet) without a second glyph, which the
 * bundled icon subset cannot draw.
 */
@Composable
private fun ReplyPill(
    glyph: String,
    label: String?,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = curioDialogActionColor()
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = if (label == null) 7.dp else 9.dp,
                vertical = 5.dp
            )
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = if (label == null) contentDescription else null,
                tint = tint,
                size = 13.dp
            )
            if (label != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = tint
                )
            }
        }
    }
}

/**
 * One row of a rendered thread: a reply, or the door that reveals the rest of
 * ONE reply's own answers.
 *
 * [key] is the row's identity across the whole thread — unique by construction,
 * because [branchRenderList] emits every reply exactly once and a door is named
 * for the reply that owns it. It is also what lets a LazyList keep its scroll
 * position while a thread grows underneath it.
 */
internal sealed class BranchRender {
    internal abstract val key: String

    internal data class Reply(val comment: CommunityComment, val depth: Int) : BranchRender() {
        override val key: String get() = "r-${comment.id}"
    }

    /**
     * A folded branch: [hidden] of [nodeId]'s answers wait behind this door,
     * which renders directly under the answers already shown and indented with
     * them. [depth] is the OWNER's depth — the door belongs one step in.
     */
    internal data class More(
        val nodeId: String,
        val hidden: Int,
        val depth: Int
    ) : BranchRender() {
        override val key: String get() = "m-$nodeId"
    }
}

/** How many answers show under one reply before the rest fold behind a door. */
internal const val BRANCH_PREVIEW = 3

/** How far one branch level indents, and how many levels are drawn. */
internal const val REPLY_INDENT_STEP_DP = 10

/**
 * The deepest indent that is still DRAWN — deeper answers keep the same offset
 * (the thread rule beside them carries the rest), so a long thread never eats
 * its own reading width.
 */
internal const val MAX_REPLY_INDENT = 3

/**
 * The render list for a reply thread: a DEPTH-FIRST walk of the real tree.
 *
 * WHY it is written this way — the previous version only ever placed replies two
 * levels deep (a top-level reply plus its direct answers), which is what made a
 * branching thread behave badly in three separate ways:
 *
 *  1. An answer to an answer (any depth past the first) was not a child of
 *     anything it could place, so it surfaced at the TOP level of the thread,
 *     out of order — "it doesn't let me keep replying in a thread".
 *  2. ANY unseen reply was dropped into that same top-level sweep, including
 *     the answers a fold was hiding. So a reply that answered an answer
 *     reappeared as a flattened duplicate at the bottom, and when a branch was
 *     posted onto it the whole thread looked like it had rearranged itself.
 *  3. Folding was decided per top-level reply only, so a branch's own answers
 *     could never be unfolded at all.
 *
 * The walk now emits every reply EXACTLY once, in order, at every depth; each
 * reply folds its OWN answers (each door is its own node, so any level can be
 * opened); and only a reply whose parent is genuinely absent from this read is
 * promoted to the top level — a reply someone wrote is never hidden, but a reply
 * that is merely folded stays folded.
 */
internal fun branchRenderList(
    replies: List<CommunityComment>,
    expanded: Set<String>
): List<BranchRender> {
    if (replies.isEmpty()) return emptyList()
    val byId = replies.associateBy { it.id }
    val children = replies.filter { it.parentId != null }.groupBy { it.parentId }
    val seen = HashSet<String>(replies.size)
    // Answers folded behind a door ARE placed — they are one tap away. Marking
    // them (and their whole subtree) keeps the orphan sweep below from
    // mistaking them for replies whose parent is gone.
    val folded = HashSet<String>()
    val out = ArrayList<BranchRender>(replies.size)

    fun markFolded(id: String) {
        if (!folded.add(id)) return
        children[id].orEmpty().forEach { markFolded(it.id) }
    }

    fun emit(node: CommunityComment, depth: Int) {
        if (!seen.add(node.id)) return
        out += BranchRender.Reply(node, depth)
        val answers = children[node.id].orEmpty().filterNot { it.id in seen }
        if (answers.isEmpty()) return
        val open = answers.size <= BRANCH_PREVIEW || node.id in expanded
        val shown = if (open) answers else answers.take(BRANCH_PREVIEW)
        shown.forEach { emit(it, depth + 1) }
        if (shown.size < answers.size) {
            answers.drop(shown.size).forEach { markFolded(it.id) }
            out += BranchRender.More(node.id, answers.size - shown.size, depth)
        }
    }

    // 1. The thread's own order: every top-level reply, each with its answers
    //    immediately beneath it, all the way down.
    replies.filter { it.parentId == null }.forEach { emit(it, 0) }

    // 2. Then anything whose parent this read did not include (the parent was
    //    removed, or it sits past the page window): promoted to the top level
    //    together with its own subtree, so no reply ever disappears. A reply
    //    that IS placed — including one folded behind a door — is left where it
    //    is, and a cycle (only reachable from a hand-modified row) surfaces
    //    instead of vanishing.
    replies.forEach { reply ->
        if (reply.id in seen || reply.id in folded) return@forEach
        val parent = reply.parentId?.let { byId[it] }
        if (parent == null || parent.id !in seen) emit(reply, 0)
    }
    return out
}

/**
 * The "show N more" door under a folded branch. It sits ONE step inside the
 * reply it belongs to (the same offset as the answers it reveals), so opening a
 * branch never moves the thread around.
 */
@Composable
internal fun ShowMoreReplies(
    hidden: Int,
    /** The depth of the reply whose answers are folded. */
    depth: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.padding(
            start = ((depth + 1).coerceAtMost(MAX_REPLY_INDENT + 1) * REPLY_INDENT_STEP_DP).dp
        )
    ) {
        Text(
            text = "Show $hidden more",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = curioDialogActionColor(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
