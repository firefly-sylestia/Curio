package com.curio.app.features.community

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CurioDirectMessage
import com.curio.app.data.supabase.CurioDmCrypto
import com.curio.app.data.supabase.CurioDmIdentity
import com.curio.app.data.supabase.CurioDmReaction
import com.curio.app.data.supabase.DmCryptoDiagnostics
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.RealtimeWatch
import com.curio.app.data.supabase.SocialApi
import com.curio.app.data.supabase.SupabaseRealtime
import com.curio.app.data.supabase.dmConversationId
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
import com.curio.app.features.settings.SettingsOptionRow
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.rememberCurioPressSource
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.isCurioDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOCAL_ID_PREFIX = "local-"
private const val LIVE_TICK_MS = 1_200L
private const val SAFETY_TICK_MS = 20_000L
private const val TYPING_TICK_MS = 1_500L
private const val TYPING_SAFETY_TICK_MS = 5_000L
private const val SWIPE_REPLY_THRESHOLD_DP = 62f
private const val SWIPE_REPLY_MAX_DP = 82f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectMessageScreen(
    navController: NavController,
    otherUserId: String,
    handle: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val density = LocalDensity.current
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()
    val haptics = LocalHapticFeedback.current

    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val eligible = account.signedIn && AppPreferences.onlineModeEnabledState && token != null && myUserId != null

    var messages by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    var pending by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    var sentShadow by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    var person by remember {
        mutableStateOf(
            SocialPeopleCache.read(context, otherUserId)
                ?: handle.trim().takeIf { it.isNotBlank() }?.let {
                    CurioPerson(userId = otherUserId, displayName = it)
                }
        )
    }
    var reactions by remember { mutableStateOf<Map<String, List<CurioDmReaction>>>(emptyMap()) }
    var draft by remember { mutableStateOf("") }
    var peerTyping by remember { mutableStateOf(false) }
    var reactionTarget by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var encryptionEnabled by remember(otherUserId) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadedOnce by remember { mutableStateOf(false) }
    var pushed by remember(otherUserId) { mutableStateOf(0) }
    var pendingRealtimeRevisions by remember(otherUserId) { mutableStateOf(false) }
    var editing by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }
    var replyTarget by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }
    var actionTarget by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }
    var failedDraft by remember(otherUserId) { mutableStateOf("") }
    var encryptionIssue by remember(otherUserId) { mutableStateOf<String?>(null) }

    val anchorBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    var actionAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var actionWiggle by remember { mutableStateOf(false) }

    fun pickReaction(messageId: String, kind: String) {
        val activeToken = token ?: return
        val activeUserId = myUserId ?: return
        scope.launch {
            val mine = reactions[messageId]?.firstOrNull { it.userId == activeUserId }
            reactionTarget = null
            actionTarget = null
            val optimistic = if (mine?.kind == kind) {
                reactions[messageId].orEmpty().filterNot { it.userId == activeUserId }
            } else {
                reactions[messageId].orEmpty()
                    .filterNot { it.userId == activeUserId } + CurioDmReaction(messageId, activeUserId, kind)
            }
            reactions = reactions + (messageId to optimistic)
            val result = if (mine?.kind == kind) {
                SocialApi.clearReaction(activeToken, messageId)
            } else {
                SocialApi.react(activeToken, messageId, kind)
            }
            result.fold(
                onSuccess = {
                    SocialApi.reactions(activeToken, listOf(messageId)).onSuccess { fresh ->
                        reactions = reactions + (messageId to fresh.getOrElse(messageId) { emptyList() })
                    }
                },
                onFailure = {
                    reactions = reactions + (messageId to (mine?.let(::listOf) ?: emptyList()))
                    error = it.message
                }
            )
        }
    }

    suspend fun load(active: String, me: String) {
        loading = true
        SocialApi.messages(active, otherUserId, me).fold(
            onSuccess = { raw ->
                val conversationId = dmConversationId(me, otherUserId)
                encryptionEnabled = SocialApi.dmConversation(active, conversationId)
                    .getOrDefault(com.curio.app.data.supabase.CurioDmConversation()).encryptionEnabled
                val identity = runCatching { CurioDmCrypto.identity(context) }.getOrElse { failure ->
                    error = "This device's encrypted-message identity is unavailable."
                    DmCryptoDiagnostics.failure("identity_load", null, null, null, failure)
                    return@fold
                }
                var identityUsable = true
                SocialApi.publishDmIdentity(active, identity, me).getOrElse { failure ->
                    identityUsable = false
                    DmCryptoDiagnostics.failure("identity_publish", null, null, null, failure)
                }
                if (identityUsable) {
                    SocialApi.dmIdentities(active, listOf(me)).getOrDefault(emptyList())
                        .filter { it.deviceId != identity.deviceId }
                        .forEach { stale -> SocialApi.retireDmDevice(active, stale.deviceId) }
                }

                val requiredVersions = raw.mapNotNull {
                    CurioDmCrypto.messageKeyVersion(it.encryptionVersion.orEmpty())
                }.toSet()
                val envelopeResult = SocialApi.dmEnvelopes(
                    active, conversationId, identity.deviceId, requiredVersions
                )
                val envelopeFailure = envelopeResult.exceptionOrNull()
                val envelopes = envelopeResult.getOrDefault(emptyMap())
                val installedVersions = mutableSetOf<Int>()
                requiredVersions.forEach { version ->
                    envelopes[version]?.let { envelope ->
                        runCatching { CurioDmCrypto.installEnvelope(context, conversationId, envelope) }
                            .onSuccess { key ->
                                installedVersions += version
                                DmCryptoDiagnostics.event(
                                    stage = "envelope_install",
                                    conversationId = conversationId,
                                    keyVersion = version,
                                    deviceId = identity.deviceId,
                                    detail = "found=true installed=true keyLength=${key.size} rsaUnwrap=success"
                                )
                            }
                            .onFailure { failure ->
                                DmCryptoDiagnostics.failure(
                                    "envelope_install", conversationId, null, version, failure,
                                    "found=true installed=false rsaUnwrap=failure"
                                )
                            }
                    }
                }
                val fresh = raw.map { message ->
                    when {
                        message.migrationState == "legacy" -> message.copy(body = "Legacy message • re-encryption required")
                        message.migrationState == "plaintext" -> message
                        else -> {
                            val encrypted = com.curio.app.data.supabase.CurioEncryptedMessage(
                                message.ciphertext.orEmpty(),
                                message.nonce.orEmpty(),
                                message.encryptionVersion.orEmpty()
                            )
                            val version = CurioDmCrypto.messageKeyVersion(encrypted.version)
                            when {
                                version == null -> message.copy(body = "Unsupported encrypted message format")
                                envelopeFailure != null -> message.copy(body = "Message key could not be retrieved")
                                envelopes[version] == null -> message.copy(body = "Message key is unavailable on this device")
                                version !in installedVersions -> message.copy(body = "Message key envelope is invalid")
                                else -> runCatching {
                                    CurioDmCrypto.decrypt(context, conversationId, encrypted)
                                }.fold(
                                    onSuccess = { plaintext -> message.copy(body = plaintext) },
                                    onFailure = { failure ->
                                        message.copy(
                                            body = if (failure is javax.crypto.AEADBadTagException) {
                                                "Message authentication failed"
                                            } else {
                                                "Message key or encrypted data is invalid"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                val hidden = SocialMessageCache.hiddenIds(context, otherUserId)
                val known = SocialMessageCache.read(context, otherUserId, me).filterNot { it.id in hidden }
                val merged = (known + fresh.filterNot { it.id in hidden })
                    .distinctBy { it.id }
                    .sortedBy { it.createdAtMillis }
                messages = merged
                SocialMessageCache.write(context, otherUserId, merged)
                sentShadow = sentShadow.filterNot { shadow ->
                    merged.any { real ->
                        real.mine && real.body == shadow.body &&
                            kotlin.math.abs(real.createdAtMillis - shadow.createdAtMillis) < 10_000L
                    }
                }
                loadedOnce = true
                error = null
            },
            onFailure = { failure -> if (messages.isEmpty()) error = failure.message }
        )
        loading = false
        val ids = messages.map { it.id }.filterNot { it.startsWith(LOCAL_ID_PREFIX) }
        if (ids.isNotEmpty()) SocialApi.reactions(active, ids).onSuccess { reactions = it }
    }

    suspend fun commitEdit(active: String, message: CurioDirectMessage) {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        SocialApi.editMessage(active, message.id, text).fold(
            onSuccess = {
                editing = null
                draft = ""
                load(active, myUserId ?: message.senderId)
            },
            onFailure = { error = it.message ?: "Couldn't edit that message." }
        )
        sending = false
    }

    suspend fun send(active: String, me: String, forcePlaintext: Boolean = false) {
        editing?.let {
            commitEdit(active, it)
            return
        }
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        error = null
        encryptionIssue = null
        val optimistic = CurioDirectMessage(
            id = "local-${System.currentTimeMillis()}",
            senderId = me,
            body = text,
            createdAtMillis = System.currentTimeMillis(),
            readAtMillis = null,
            mine = true
        )
        pending += optimistic
        draft = ""
        replyTarget = null
        val conversationId = dmConversationId(me, otherUserId)

        if (!encryptionEnabled || forcePlaintext) {
            if (forcePlaintext && encryptionEnabled) {
                SocialApi.setDmEncryption(active, conversationId, me, otherUserId, false).fold(
                    onSuccess = { encryptionEnabled = false },
                    onFailure = {
                        pending = pending.filterNot { it.id == optimistic.id }
                        draft = text
                        error = it.message ?: "Couldn't change message encryption."
                        sending = false
                        return
                    }
                )
            }
            SocialApi.sendPlaintext(active, otherUserId, text, me).fold(
                onSuccess = {
                    SocialApi.setTyping(active, otherUserId, false)
                    pending = pending.filterNot { it.id == optimistic.id }
                    sentShadow += optimistic
                    load(active, me)
                },
                onFailure = {
                    pending = pending.filterNot { it.id == optimistic.id }
                    draft = text
                    error = it.message ?: "That message didn't send."
                }
            )
            sending = false
            return
        }

        val encrypted = runCatching {
            val mine = CurioDmCrypto.identity(context)
            SocialApi.dmIdentities(active, listOf(me)).getOrDefault(emptyList())
                .filter { it.deviceId != mine.deviceId }
                .forEach { stale -> SocialApi.retireDmDevice(active, stale.deviceId) }
            SocialApi.publishDmIdentity(active, mine, me).getOrThrow()
            val ownEnvelope = SocialApi.dmEnvelope(active, conversationId, mine.deviceId).getOrNull()
            val restoredKey = ownEnvelope?.let {
                runCatching { CurioDmCrypto.installEnvelope(context, conversationId, it) }.getOrNull()
            }
            val keyVersion = if (restoredKey != null) ownEnvelope!!.keyVersion else {
                SocialApi.dmHighestKeyVersion(active, conversationId).getOrThrow()
                    .coerceAtMost(Int.MAX_VALUE - 1) + 1
            }
            val key = restoredKey
                ?: CurioDmCrypto.existingKey(context, conversationId, keyVersion)
                ?: CurioDmCrypto.newKey(context, conversationId, keyVersion)
            val missing = SocialApi.dmMissingEnvelopes(active, conversationId, keyVersion).getOrNull()
            val peers = when {
                missing != null -> missing.mapNotNull { it.toIdentity() }.ifEmpty {
                    SocialApi.dmIdentities(active, listOf(otherUserId, me))
                        .getOrDefault(emptyList()).filter(CurioDmCrypto::canWrapFor)
                }
                else -> SocialApi.dmIdentities(active, listOf(otherUserId, me))
                    .getOrDefault(emptyList()).filter(CurioDmCrypto::canWrapFor)
            }
            check(peers.any { it.userId == otherUserId }) {
                "This friend needs to open Curio once before encrypted messages can reach them."
            }
            coroutineScope {
                peers.map { peer ->
                    async {
                        SocialApi.saveDmEnvelope(
                            active,
                            conversationId,
                            peer.userId,
                            CurioDmCrypto.wrapConversationKey(key, peer, keyVersion)
                        ).getOrThrow()
                    }
                }.awaitAll()
            }
            CurioDmCrypto.encrypt(context, conversationId, key, keyVersion, text)
        }.getOrElse { failure ->
            pending = pending.filterNot { it.id == optimistic.id }
            draft = text
            failedDraft = text
            encryptionIssue = failure.message ?: "Couldn't prepare this encrypted message."
            sending = false
            return
        }

        SocialApi.sendEncrypted(
            active,
            otherUserId,
            encrypted.ciphertext,
            encrypted.nonce,
            encrypted.version,
            me
        ).fold(
            onSuccess = {
                SocialApi.setTyping(active, otherUserId, false)
                pending = pending.filterNot { it.id == optimistic.id }
                sentShadow += optimistic
                load(active, me)
            },
            onFailure = {
                pending = pending.filterNot { it.id == optimistic.id }
                draft = text
                failedDraft = text
                encryptionIssue = it.message ?: "That message didn't send."
            }
        )
        sending = false
    }

    suspend fun refreshLiveBits(active: String, me: String) {
        peerTyping = SocialApi.isTyping(active, me, otherUserId)
        val ids = messages.map { it.id }.filterNot { it.startsWith(LOCAL_ID_PREFIX) }
        if (ids.isNotEmpty()) SocialApi.reactions(active, ids).onSuccess { reactions = it }
    }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    LaunchedEffect(eligible, token, myUserId, otherUserId) {
        if (!eligible || token == null || myUserId == null) {
            messages = emptyList()
            pending = emptyList()
            sentShadow = emptyList()
            reactions = emptyMap()
            peerTyping = false
            return@LaunchedEffect
        }
        SocialMessageCache.migrateIfNeeded(context)
        SocialMessageCache.read(context, otherUserId, myUserId).takeIf { it.isNotEmpty() }?.let { messages = it }
        load(token, myUserId)
        SocialApi.profile(token, otherUserId).onSuccess { fresh ->
            if (fresh != null) {
                person = fresh
                SocialPeopleCache.remember(context, fresh)
            }
        }
        SocialApi.markRead(token, otherUserId, myUserId)
    }

    LaunchedEffect(eligible, token, myUserId, otherUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            peerTyping = SocialApi.isTyping(token, myUserId, otherUserId)
            delay(if (SupabaseRealtime.isLinked) TYPING_SAFETY_TICK_MS else TYPING_TICK_MS)
        }
    }

    LaunchedEffect(draft, eligible, token) {
        if (!eligible || token == null) return@LaunchedEffect
        if (draft.isBlank()) {
            SocialApi.setTyping(token, otherUserId, false)
            return@LaunchedEffect
        }
        delay(700)
        SocialApi.setTyping(token, otherUserId, true)
        delay(6_000)
        SocialApi.setTyping(token, otherUserId, false)
    }

    DisposableEffect(eligible, token, myUserId, otherUserId) {
        val active = token
        val me = myUserId
        val owner = "dm:$otherUserId"
        if (eligible && active != null && me != null) {
            SupabaseRealtime.watch(
                owner = owner,
                accessToken = active,
                watches = listOf(
                    RealtimeWatch("dm_messages", "sender=eq.$otherUserId", listOf("INSERT", "UPDATE", "DELETE")),
                    RealtimeWatch("dm_messages", "recipient=eq.$otherUserId", listOf("UPDATE")),
                    RealtimeWatch("dm_typing", "sender=eq.$otherUserId", listOf("INSERT", "UPDATE")),
                    RealtimeWatch("dm_reactions", "user_id=eq.$otherUserId", listOf("INSERT", "UPDATE", "DELETE"))
                )
            ) {
                scope.launch { pendingRealtimeRevisions = true; pushed++ }
                scope.launch { refreshLiveBits(active, me) }
            }
        }
        onDispose { SupabaseRealtime.unwatch(owner) }
    }

    LaunchedEffect(pushed, eligible, token, myUserId, otherUserId) {
        if (!eligible || token == null || myUserId == null || pushed == 0) return@LaunchedEffect
        SocialApi.messages(token, otherUserId, myUserId).onSuccess { fresh ->
            if (pendingRealtimeRevisions) {
                pendingRealtimeRevisions = false
                messages = fresh
                    .filterNot { it.id in SocialMessageCache.hiddenIds(context, otherUserId) }
                    .sortedBy { it.createdAtMillis }
                SocialMessageCache.write(context, otherUserId, messages)
            }
            scope.launch { load(token, myUserId) }
        }
    }

    LaunchedEffect(eligible, token, myUserId, otherUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            delay(if (SupabaseRealtime.isLinked) SAFETY_TICK_MS else LIVE_TICK_MS)
            load(token, myUserId)
        }
    }

    val thread = remember(messages, pending, sentShadow) {
        messages + (sentShadow + pending).sortedBy { it.createdAtMillis }
    }

    val fallback = person?.label?.takeIf { it.isNotBlank() } ?: handle.ifBlank { "Message" }
    val seenIndex = thread.indexOfLast { it.mine && it.readAtMillis != null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(heroPageBackground(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
    ) {
        if (!wide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        Column(
            modifier = Modifier
                .layerBackdrop(glassBackdrop)
                .fillMaxSize()
        ) {
            if (eligible && token != null && myUserId != null) {
                val activeToken = token
                val activeUserId = myUserId
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = if (wide) 0.dp else SettingsHeroTotalHeight,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (wide) {
                        item(key = "hero") {
                            SettingsHeroHeader(
                                title = fallback,
                                subtitle = "Private messages",
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    item(key = "peer") {
                        MessagePeerHeader(
                            person = person,
                            fallback = fallback,
                            typing = peerTyping,
                            onOpenProfile = {
                                navController.navigate(CurioRoutes.socialProfile(otherUserId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    error?.let { note -> item(key = "error") { SocialNote(note, true) } }
                    if (thread.isEmpty() && !loading && loadedOnce) {
                        item(key = "empty") {
                            SocialEmptyCard(
                                icon = CurioIcons.Notes,
                                title = "Nothing said yet",
                                body = "Start with a hello."
                            )
                        }
                    }
                    dmItemsWithFraming(thread) { index, message, first, last ->
                        MessageEntry(
                            message = message,
                            firstOfRun = first,
                            lastOfRun = last,
                            receipt = if (index == seenIndex) message.readAtMillis else null,
                            accent = reactionTarget == message.id,
                            reactions = reactions[message.id].orEmpty(),
                            myUserId = activeUserId,
                            anchorBounds = anchorBounds,
                            onTap = {
                                actionTarget = null
                                reactionTarget = if (reactionTarget == message.id) null else message.id
                            },
                            onLongPress = {
                                reactionTarget = null
                                actionTarget = message
                                actionAnchor = anchorBounds[message.id]
                                actionWiggle = !actionWiggle
                            },
                            onSwipeReply = {
                                actionTarget = null
                                reactionTarget = null
                                replyTarget = message
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onPick = ::pickReaction,
                            onEdit = {
                                if (message.mine && message.migrationState == "plaintext" && !message.id.startsWith(LOCAL_ID_PREFIX)) {
                                    editing = message
                                    draft = message.body
                                    replyTarget = null
                                    actionTarget = null
                                }
                            },
                            onCopy = {
                                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                clip?.setPrimaryClip(android.content.ClipData.newPlainText("message", message.body))
                                actionTarget = null
                            },
                            onRemove = if (message.mine && !message.id.startsWith(LOCAL_ID_PREFIX)) {
                                {
                                    actionTarget = null
                                    scope.launch {
                                        SocialApi.deleteMessage(activeToken, message.id).fold(
                                            onSuccess = {
                                                messages = messages.filterNot { it.id == message.id }
                                                SocialMessageCache.write(context, otherUserId, messages)
                                            },
                                            onFailure = { error = it.message ?: "Couldn't remove that message." }
                                        )
                                    }
                                }
                            } else null,
                            animateIn = message.id.startsWith(LOCAL_ID_PREFIX)
                        )
                    }
                    if (peerTyping && thread.isNotEmpty()) {
                        item(key = "typing") { TypingBubble(person) }
                    }
                }

                MessageComposer(
                    draft = draft,
                    title = fallback,
                    sending = sending,
                    editTarget = editing,
                    replyTarget = replyTarget,
                    onDropEdit = { editing = null; draft = "" },
                    onDropReply = { replyTarget = null },
                    onDraftChange = { if (it.length <= SocialApi.MAX_MESSAGE_CHARS) draft = it },
                    onSend = { scope.launch { send(activeToken, activeUserId) } }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = if (wide) 0.dp else SettingsHeroTotalHeight,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (wide) {
                        item(key = "hero") {
                            SettingsHeroHeader(
                                title = fallback,
                                subtitle = "Private messages",
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    item { SettingsSectionHeading("Before you start") }
                    item {
                        SettingsOptionCard {
                            when {
                                !OnlineAccount.configured -> SettingsOptionInfoRow(
                                    CurioIcons.Warning,
                                    "Not set up in this build",
                                    "This build has no Curio online project, so messages can't load."
                                )
                                !account.signedIn -> {
                                    SettingsOptionInfoRow(
                                        CurioIcons.Info,
                                        "Sign in to message",
                                        "Messages are tied to a Curio account."
                                    )
                                    SettingsOptionRow(
                                        icon = CurioIcons.Person,
                                        title = "Sign in or create an account",
                                        subtitle = "Settings → Online mode",
                                        onClick = { navController.navigate(CurioRoutes.SETTINGS_ONLINE) }
                                    )
                                }
                                else -> {
                                    SettingsOptionInfoRow(
                                        CurioIcons.Info,
                                        "Online mode is off",
                                        "Nothing loads while Online mode is off."
                                    )
                                    SettingsOptionRow(
                                        icon = CurioIcons.Refresh,
                                        title = "Turn Online mode on",
                                        subtitle = "Settings → Online mode",
                                        onClick = { navController.navigate(CurioRoutes.SETTINGS_ONLINE) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (actionTarget != null && actionAnchor != null) {
            val target = actionTarget!!
            val screenWidthPx = with(density) { wideContentEdgePadding().toPx() * 2f }
            val trayHeightPx = with(density) { 142.dp.toPx() }
            val trayY = (actionAnchor!!.top - trayHeightPx - with(density) { 8.dp.toPx() }).coerceAtLeast(
                with(density) { SettingsHeroTotalHeight.toPx() }
            )
            val trayX = actionAnchor!!.left.coerceAtLeast(screenWidthPx / 2f).coerceAtMost(
                actionAnchor!!.right - with(density) { 220.dp.toPx() }
            )
            DmActionTray(
                modifier = Modifier
                    .offset {
                        IntOffset(trayX.toInt(), trayY.toInt())
                    }
                    .animateContentPlacementCompat(actionWiggle),
                target = target,
                currentReaction = reactions[target.id]?.firstOrNull { it.userId == myUserId }?.kind,
                onPickReaction = ::pickReaction,
                onReply = {
                    replyTarget = target
                    actionTarget = null
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onCopy = {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clip?.setPrimaryClip(android.content.ClipData.newPlainText("message", target.body))
                    actionTarget = null
                },
                onEdit = {
                    if (target.mine && target.migrationState == "plaintext" && !target.id.startsWith(LOCAL_ID_PREFIX)) {
                        editing = target
                        draft = target.body
                        replyTarget = null
                        actionTarget = null
                    }
                },
                onRemove = if (target.mine && !target.id.startsWith(LOCAL_ID_PREFIX)) {
                    {
                        val active = token
                        if (active != null) {
                            scope.launch {
                                SocialApi.deleteMessage(active, target.id).fold(
                                    onSuccess = {
                                        messages = messages.filterNot { it.id == target.id }
                                        SocialMessageCache.write(context, otherUserId, messages)
                                    },
                                    onFailure = { error = it.message ?: "Couldn't remove that message." }
                                )
                            }
                        }
                        actionTarget = null
                    }
                } else null,
                onDismiss = { actionTarget = null }
            )
        }

        encryptionIssue?.let { reason ->
            SocialConfirmDialog(
                title = "Encrypted message didn't send",
                body = "$reason\n\nYou can send this message with encryption turned off for this chat instead.",
                confirmLabel = "Send without encryption",
                destructive = false,
                busy = sending,
                onDismiss = { if (!sending) encryptionIssue = null },
                onConfirm = {
                    val active = token
                    val me = myUserId
                    if (active == null || me == null) {
                        encryptionIssue = null
                    } else {
                        scope.launch {
                            draft = failedDraft
                            failedDraft = ""
                            encryptionIssue = null
                            send(active, me, forcePlaintext = true)
                        }
                    }
                }
            )
        }

        if (!wide) {
            SettingsHeroHeader(
                title = person?.label ?: fallback,
                subtitle = if (peerTyping) "Typing…" else "",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop,
                titleLeading = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        SocialAvatar(
                            style = person?.avatarStyle ?: 0,
                            avatarSize = 38.dp,
                            online = person?.isActiveNow == true
                        )
                    }
                }
            )
        }
    }
}

private fun LazyListScope.dmItemsWithFraming(
    thread: List<CurioDirectMessage>,
    row: @Composable (index: Int, message: CurioDirectMessage, first: Boolean, last: Boolean) -> Unit
) {
    itemsIndexed(thread, key = { _, message -> message.id }) { index, message ->
        val previous = thread.getOrNull(index - 1)
        val next = thread.getOrNull(index + 1)
        val samePrevious = previous != null && previous.mine == message.mine &&
            message.createdAtMillis - previous.createdAtMillis <= 5 * 60 * 1000
        val sameNext = next != null && next.mine == message.mine &&
            next.createdAtMillis - message.createdAtMillis <= 5 * 60 * 1000
        row(index, message, !samePrevious, !sameNext)
    }
}

@Composable
private fun MessagePeerHeader(
    person: CurioPerson?,
    fallback: String,
    typing: Boolean,
    onOpenProfile: () -> Unit
) {
    val dark = isCurioDarkTheme()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
        else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.86f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onOpenProfile, onLongClick = {})
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            SocialAvatar(
                style = person?.avatarStyle ?: 0,
                avatarSize = 46.dp,
                online = person?.isActiveNow == true
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = person?.label ?: fallback,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = if (typing) "Typing…" else (person?.handleLabel ?: "Open profile"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (typing) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (typing) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CurioIcon(
                name = CurioIcons.ChevronRight,
                contentDescription = "Open profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        }
    }
}

@Composable
private fun MessageEntry(
    message: CurioDirectMessage,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    receipt: Long?,
    accent: Boolean,
    reactions: List<CurioDmReaction>,
    myUserId: String,
    anchorBounds: MutableMap<String, androidx.compose.ui.geometry.Rect>,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeReply: () -> Unit,
    onPick: (String, String) -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onRemove: (() -> Unit)?,
    animateIn: Boolean
) {
    val visible = remember {
        MutableTransitionState(!animateIn).apply { if (animateIn) targetState = true }
    }
    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(CurioMotion.Durations.Quick)),
        exit = androidx.compose.animation.ExitTransition.None
    ) {
        SwipeReplyMessage(
            message = message,
            firstOfRun = firstOfRun,
            lastOfRun = lastOfRun,
            receipt = receipt,
            reactions = reactions,
            myUserId = myUserId,
            anchorBounds = anchorBounds,
            onTap = onTap,
            onLongPress = onLongPress,
            onSwipeReply = onSwipeReply,
            animateIn = animateIn
        )
        if (accent) {
            ReactionBar(
                current = reactions.firstOrNull { it.userId == myUserId }?.kind,
                mine = message.mine,
                onPick = { onPick(message.id, it) }
            )
        }
    }
}

@Composable
private fun SwipeReplyMessage(
    message: CurioDirectMessage,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    receipt: Long?,
    reactions: List<CurioDmReaction>,
    myUserId: String,
    anchorBounds: MutableMap<String, androidx.compose.ui.geometry.Rect>,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeReply: () -> Unit,
    animateIn: Boolean
) {
    val mine = message.mine
    val dark = isCurioDarkTheme()
    val haptics = LocalHapticFeedback.current
    var dragPx by remember(message.id) { mutableStateOf(0f) }
    var armed by remember(message.id) { mutableStateOf(false) }
    val threshold = with(LocalDensity.current) { SWIPE_REPLY_THRESHOLD_DP.dp.toPx() }
    val maxSwipe = with(LocalDensity.current) { SWIPE_REPLY_MAX_DP.dp.toPx() }
    val swipeOffset by animateFloatAsState(
        targetValue = dragPx,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 700f),
        label = "dmSwipe"
    )

    val shape = if (mine) {
        RoundedCornerShape(20.dp, 20.dp, if (lastOfRun) 20.dp else 7.dp, 7.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 7.dp, if (lastOfRun) 20.dp else 7.dp)
    }
    val mineGlyph = reactions.firstOrNull { it.userId == myUserId }?.kind
    val others = reactions.filterNot { it.userId == myUserId }

    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (mine && lastOfRun) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)) {
                Text(socialStamp(message.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                receipt?.let {
                    Text("Seen ${socialStamp(it)}", style = MaterialTheme.typography.labelSmall, color = curioDialogActionColor())
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .onGloballyPositioned { coordinates -> anchorBounds[message.id] = coordinates.boundsInParent() }
        ) {
            val bubbleStart = if (mine) Alignment.End else Alignment.Start
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                val replyProgress = (kotlin.math.abs(swipeOffset) / threshold).coerceIn(0f, 1f)
                val replySideIsVisible = (mine && swipeOffset < 0f) || (!mine && swipeOffset > 0f)
                if (replySideIsVisible) {
                    Surface(
                        shape = CircleShape,
                        color = curioDialogActionColor().copy(alpha = 0.10f + 0.14f * replyProgress),
                        modifier = Modifier
                            .size(34.dp)
                            .alpha(0.6f + 0.4f * replyProgress)
                            .scale(0.72f + 0.28f * replyProgress)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CurioIcon(
                                name = CurioIcons.Reply,
                                contentDescription = "Reply",
                                tint = curioDialogActionColor(),
                                size = 17.dp
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                val press = rememberCurioPressSource(pressedScale = 0.96f)
                Box(
                    modifier = Modifier
                        .then(press.modifier)
                        .graphicsLayer { translationX = swipeOffset }
                        .clip(shape)
                        .background(
                            when {
                                mine -> if (dark) androidx.compose.ui.graphics.Color(0xFF3A2A33) else MaterialTheme.colorScheme.primary
                                dark -> androidx.compose.ui.graphics.Color(0xFF3C3A3B)
                                else -> androidx.compose.ui.graphics.Color(0xFFE9E1DA)
                            }
                        )
                        .combinedClickable(
                            interactionSource = press.interactionSource,
                            indication = LocalIndication.current,
                            onClickLabel = "React to this message",
                            onLongClickLabel = "Message options",
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            },
                            onClick = onTap
                        )
                        .pointerInput(message.id) {
                            detectHorizontalDragGestures(
                                onDragStart = { armed = false },
                                onDragCancel = {
                                    dragPx = 0f
                                    armed = false
                                },
                                onDragEnd = {
                                    val shouldReply = kotlin.math.abs(dragPx) >= threshold
                                    if (shouldReply) onSwipeReply()
                                    dragPx = 0f
                                    armed = false
                                },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    val next = (dragPx + amount).coerceIn(-maxSwipe, maxSwipe)
                                    val correctDirection = (mine && next <= 0f) || (!mine && next >= 0f)
                                    if (correctDirection) {
                                        val wasArmed = armed
                                        dragPx = next
                                        armed = kotlin.math.abs(next) >= threshold
                                        if (!wasArmed && armed) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mine) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        if (message.editedAtMillis != null) {
                            Text(
                                text = "edited",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mine) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (mineGlyph != null || others.isNotEmpty()) {
                ReactionChips(mineGlyph, others)
            }
        }

        if (!mine && lastOfRun) {
            Text(
                socialStamp(message.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun ReactionChips(mineGlyph: String?, others: List<CurioDmReaction>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 3.dp)) {
        mineGlyph?.let { ReactionChip(it, 1, true) }
        others.groupBy { it.kind }.forEach { (kind, group) -> ReactionChip(kind, group.size, false) }
    }
}

@Composable
private fun ReactionChip(kind: String, count: Int, mine: Boolean) {
    val ink = if (mine) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(SocialReactions.emojiFor(kind), style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), color = ink)
            if (count > 1) Text(count.toString(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ink)
        }
    }
}

@Composable
private fun ReactionBar(current: String?, mine: Boolean, onPick: (String) -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(CurioMotion.Durations.Quick)) + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut(tween(CurioMotion.Durations.Quick))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (mine) 0.dp else 4.dp, top = 2.dp)
        ) {
            if (mine) Spacer(Modifier.weight(1f))
            SocialReactions.PALETTE.forEach { (emoji, meaning) ->
                val chosen = current != null && SocialReactions.emojiFor(current) == emoji
                Surface(
                    onClick = { onPick(emoji) },
                    shape = RoundedCornerShape(50),
                    color = if (chosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.semantics { contentDescription = meaning }
                ) {
                    Text(
                        emoji,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        color = if (chosen) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                    )
                }
            }
            if (!mine) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TypingBubble(person: CurioPerson?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SocialAvatar(style = person?.avatarStyle ?: 0, avatarSize = 26.dp)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(7.dp, 18.dp, 7.dp, 18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                repeat(3) { Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun DmActionTray(
    modifier: Modifier,
    target: CurioDirectMessage,
    currentReaction: String?,
    onPickReaction: (String, String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = scaleIn(animationSpec = spring(dampingRatio = 0.72f, stiffness = 550f), initialScale = 0.84f) + fadeIn(tween(130)),
        exit = scaleOut(tween(100), targetScale = 0.92f) + fadeOut(tween(80)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
            shadowElevation = 18.dp,
            tonalElevation = 5.dp,
            modifier = Modifier
                .width(250.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    SocialReactions.PALETTE.forEach { (emoji, _) ->
                        val selected = currentReaction != null && SocialReactions.emojiFor(currentReaction) == emoji
                        Surface(
                            onClick = { onPickReaction(target.id, emoji) },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 18.sp) }
                        }
                    }
                }
                Text(
                    text = target.body.take(76) + if (target.body.length > 76) "…" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    DmTrayAction(CurioIcons.Reply, "Reply", onReply)
                    DmTrayAction(CurioIcons.Copy, "Copy", onCopy)
                    if (target.mine && target.migrationState == "plaintext" && !target.id.startsWith(LOCAL_ID_PREFIX)) {
                        DmTrayAction(CurioIcons.Edit, "Edit", onEdit)
                    }
                    onRemove?.let { DmTrayAction(CurioIcons.Delete, "Remove", it, destructive = true) }
                    DmTrayAction(CurioIcons.Close, "Close", onDismiss)
                }
            }
        }
    }
}

@Composable
private fun DmTrayAction(icon: String, label: String, onClick: () -> Unit, destructive: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.weight(1f).semantics { contentDescription = label }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 7.dp)) {
            CurioIcon(
                name = icon,
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MessageComposer(
    modifier: Modifier = Modifier,
    draft: String,
    title: String,
    sending: Boolean,
    editTarget: CurioDirectMessage?,
    replyTarget: CurioDirectMessage?,
    onDropEdit: () -> Unit,
    onDropReply: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val dark = isCurioDarkTheme()
    val armed = draft.isNotBlank() && !sending
    val sendScale by animateFloatAsState(
        targetValue = if (armed) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 850f),
        label = "sendScale"
    )
    val nearLimit = draft.length > SocialApi.MAX_MESSAGE_CHARS * 8 / 10

    Column(
        modifier = modifier.fillMaxWidth().imePadding().padding(start = wideContentEdgePadding(), end = wideContentEdgePadding(), bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedVisibility(visible = editTarget != null) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp)) {
                    CurioIcon(CurioIcons.Edit, null, curioDialogActionColor(), 14.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Editing message", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                    Text("Cancel", color = curioDialogActionColor(), modifier = Modifier.combinedClickable(onClick = onDropEdit, onLongClick = {}).padding(6.dp))
                }
            }
        }
        AnimatedVisibility(visible = replyTarget != null) {
            replyTarget?.let { target ->
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Box(Modifier.width(3.dp).height(30.dp).clip(RoundedCornerShape(2.dp)).background(curioDialogActionColor()))
                        Column(Modifier.weight(1f).padding(start = 9.dp)) {
                            Text("Replying to ${if (target.mine) "yourself" else "them"}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = curioDialogActionColor())
                            Text(target.body, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(onClick = onDropReply, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(30.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CurioIcon(CurioIcons.Close, "Cancel reply", MaterialTheme.colorScheme.onSurfaceVariant, 15.dp)
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp)
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(curioDialogActionColor()),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    modifier = Modifier.weight(1f)
                ) { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text("Message $title", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                        inner()
                    }
                }
            }
            Surface(
                onClick = { if (armed) onSend() },
                shape = CircleShape,
                color = if (armed) curioDialogActionColor() else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(52.dp).scale(sendScale)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        name = if (editTarget != null) CurioIcons.Check else CurioIcons.ArrowForward,
                        contentDescription = if (editTarget != null) "Save edit" else "Send",
                        tint = if (armed) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp
                    )
                }
            }
        }
        if (nearLimit) {
            Text(
                "${draft.length}/${SocialApi.MAX_MESSAGE_CHARS}",
                style = MaterialTheme.typography.labelSmall,
                color = if (draft.length >= SocialApi.MAX_MESSAGE_CHARS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun androidx.compose.ui.Modifier.animateContentPlacementCompat(trigger: Boolean): Modifier =
    this.graphicsLayer {
        alpha = if (trigger) 0.99f else 1f
    }
