package com.curio.app.features.feedback

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.AppPreferences
import com.curio.app.data.supabase.FeedbackAnswer
import com.curio.app.data.supabase.FeedbackApi
import com.curio.app.data.supabase.FeedbackForm
import com.curio.app.data.supabase.FeedbackKinds
import com.curio.app.data.supabase.FeedbackQuestion
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The live feedback form, held once for the whole app (v403).
 *
 * The member asked for a form with a door in Support for later, so the state
 * cannot live inside one screen: Home shows the card while a form is live,
 * Support keeps the way back to it, and both read this. The form itself is
 * fetched once per app run ([refresh]) and the two local flags decide whether
 * it is still worth showing — answered, or asked never to appear again.
 */
object FeedbackFormState {

    /** The published form, or null when there is none (or nobody could ask). */
    var liveForm by mutableStateOf<FeedbackForm?>(null)
        private set

    /** True while the fetch is in flight. */
    var loading by mutableStateOf(false)
        private set

    /** A failure worth showing, already worded. */
    var problem by mutableStateOf<String?>(null)
        private set

    /** True while the answering sheet is up. */
    var open by mutableStateOf(false)
        private set

    /** The holder's own scope: no screen owns a request it did not make. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** One successful read is enough for a run; a failure may be retried. */
    private var fetched = false

    /** True while a read is parked, waiting for the account's session. */
    private var watching = false

    /** What THIS device has already answered, and what it never wants again. */
    private var answeredIds by mutableStateOf<Set<String>>(emptySet())
    private var hiddenIds by mutableStateOf<Set<String>>(emptySet())

    /** True when a live form exists that this device has not dealt with yet. */
    val visible: Boolean
        get() {
            val form = liveForm ?: return false
            return !answeredIds.contains(form.id) && !hiddenIds.contains(form.id)
        }

    /** Reads this device's two flags, so the cards react to a send at once. */
    fun loadFlags(context: Context) {
        answeredIds = AppPreferences.feedbackAnsweredIds(context)
        hiddenIds = AppPreferences.feedbackHiddenIds(context)
    }

    fun openSheet() {
        if (liveForm != null) open = true
    }

    fun dismissSheet() {
        open = false
    }

    /**
     * v425 — the read waits for the ACCOUNT, it does not give up on it.
     *
     * The form lives on the server and is read with the member's own token, so
     * the read needs a restored session — and on a cold start the first
     * composition happens a beat before the account has one ([OnlineAccount
     * .restore] is a sandbox read plus a token refresh). The old read simply
     * returned on a null token, and because nothing asked again, the card only
     * turned up once some OTHER screen happened to enter and read it — which is
     * exactly the member's "it only appears after I open it in Settings".
     *
     * So a read made too early now parks itself on the session and runs the
     * moment one arrives; [watching] keeps it to one parked read.
     */
    fun refresh(context: Context, force: Boolean = false) {
        loadFlags(context)
        if (!AppPreferences.isOnlineModeEnabled(context)) return
        val token = OnlineAccount.state.session?.accessToken
        if (token == null) {
            awaitAccount(context)
            return
        }
        if (!force && fetched) return
        loading = true
        scope.launch {
            FeedbackApi.liveForm(token).fold(
                onSuccess = { form ->
                    liveForm = form
                    problem = null
                    fetched = true
                },
                onFailure = { failure ->
                    // A failure is not news for the member: Home simply keeps
                    // its usual shape, and Support says nothing until a form
                    // really is live.
                    problem = failure.message
                }
            )
            loading = false
        }
    }

    /**
     * Parks one read until the account reports a session, then runs it. The
     * application context is what is kept, so no activity is ever held.
     */
    private fun awaitAccount(context: Context) {
        if (watching) return
        watching = true
        val app = context.applicationContext
        scope.launch {
            snapshotFlow { OnlineAccount.state.session?.accessToken }
                .first { it != null }
            watching = false
            refresh(app)
        }
    }

    /** Sends the answers, and remembers them on THIS device. */
    fun submit(
        context: Context,
        form: FeedbackForm,
        answers: List<FeedbackAnswer>,
        onDone: (Boolean, String?) -> Unit
    ) {
        val token = OnlineAccount.state.session?.accessToken
        if (token == null) {
            onDone(false, "Turn Online mode on to send this.")
            return
        }
        scope.launch {
            FeedbackApi.submit(token, form.id, answers).fold(
                onSuccess = {
                    AppPreferences.markFeedbackAnswered(context, form.id)
                    answeredIds = AppPreferences.feedbackAnsweredIds(context)
                    onDone(true, null)
                },
                onFailure = { onDone(false, it.message) }
            )
        }
    }

    /** "Not now" or "never show me this again". */
    fun skip(context: Context, form: FeedbackForm, never: Boolean) {
        val token = OnlineAccount.state.session?.accessToken
        if (never) {
            AppPreferences.markFeedbackHidden(context, form.id)
            hiddenIds = AppPreferences.feedbackHiddenIds(context)
        }
        if (token == null) return
        scope.launch {
            // Best effort: the member's own choice is already local, and the
            // count only feeds the team's results.
            FeedbackApi.recordSkip(token, form.id, never)
        }
    }
}

/**
 * The forms' card: the page's standout tile while a form is live.
 *
 * Filled in the app's own accent tone (the same strong pair the updater's
 * "Download & install" pill wears) instead of the pale wash a settings card
 * has, because this is the one tile on Support and Home asking the member to
 * DO something.
 */
@Composable
fun FeedbackFormCard(
    form: FeedbackForm,
    onOpen: () -> Unit,
    onSkip: () -> Unit,
    onNever: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isCurioDarkTheme()
    val fill = if (dark) CurioColors.HomeRosewoodDark else CurioColors.CoralBlush
    val ink = if (dark) CurioColors.CoralBlush else CurioColors.DeepPlum
    val questions = form.questions.size
    val minutes = if (questions <= 2) "a minute" else "two minutes"

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(22.dp),
        color = fill,
        contentColor = ink,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ink.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(CurioIcons.AutoAwesome, null, tint = ink, size = 20.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "A word from us",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = ink.copy(alpha = 0.75f)
                    )
                    Text(
                        form.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = ink
                    )
                }
            }
            if (form.intro.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    form.intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "$questions question${if (questions == 1) "" else "s"} · about $minutes · " +
                    "answers are anonymous",
                style = MaterialTheme.typography.bodySmall,
                color = ink.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onOpen,
                    shape = RoundedCornerShape(50),
                    color = ink,
                    contentColor = fill
                ) {
                    Text(
                        "Take it now",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
                Text(
                    "Not now",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ink.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSkip() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Never",
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onNever() }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * The form itself. One question at a time down the sheet, three kinds of
 * question, and the two ways out the member asked for (Not now, Never show).
 */
@Composable
fun FeedbackFormSheet(
    form: FeedbackForm,
    onDismiss: () -> Unit,
    /**
     * True when the TEAM is walking the form (a test): the words change to say
     * so, nothing lands in this device's answered/never flags, and the answers
     * only ever reach a test form's tally.
     */
    testing: Boolean = false,
    /** How the team's walkthrough sends. Null = the member's own path. */
    onSubmit: ((List<FeedbackAnswer>, (Boolean, String?) -> Unit) -> Unit)? = null,
    /** How the team's walkthrough dismisses. Null = the member's own path. */
    onSkip: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = curioRoseInk()

    // The answers being built, keyed by question id.
    val picks = remember(form.id) { mutableStateMapOf<String, FeedbackAnswer>() }
    var sending by remember(form.id) { mutableStateOf(false) }
    var trouble by remember(form.id) { mutableStateOf<String?>(null) }

    val ready = form.questions.all { question ->
        when (val answer = picks[question.id]) {
            null -> false
            is FeedbackAnswer.One -> answer.option >= 0
            is FeedbackAnswer.Many -> answer.options.isNotEmpty()
            is FeedbackAnswer.Written -> answer.text.isNotBlank()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(CurioIcons.AutoAwesome, null, tint = accent, size = 20.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        form.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        (if (testing) "A test run · " else "Anonymous · ") +
                            "${form.questions.size} question" +
                            if (form.questions.size == 1) "" else "s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (form.intro.isNotBlank()) {
                Text(
                    form.intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            form.questions.forEachIndexed { index, question ->
                FeedbackQuestionCard(
                    number = index + 1,
                    of = form.questions.size,
                    question = question,
                    answer = picks[question.id],
                    onAnswer = { picks[question.id] = it }
                )
            }

            Text(
                if (testing) {
                    "This is the team's own walkthrough. It is counted separately, " +
                        "and a member never sees it."
                } else {
                    "Your answers arrive without your name, your account or your " +
                        "device attached to them. Nobody can tell which ones were yours."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            trouble?.let { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = {
                        if (!ready || sending) return@Surface
                        sending = true
                        val ordered = form.questions.mapNotNull { picks[it.id] }
                        val done: (Boolean, String?) -> Unit = { ok, message ->
                            sending = false
                            if (ok) onDismiss() else trouble = message
                        }
                        if (onSubmit != null) onSubmit(ordered, done)
                        else FeedbackFormState.submit(context, form, ordered, done)
                    },
                    shape = RoundedCornerShape(50),
                    color = if (ready) accent else accent.copy(alpha = 0.35f),
                    contentColor = if (isCurioDarkTheme()) CurioColors.CoralBlush
                    else CurioColors.DeepPlum
                ) {
                    Text(
                        when {
                            sending -> "Sending…"
                            testing -> "Send the test answers"
                            else -> "Send my answers"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (testing) "Close the walkthrough" else "Not now",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            if (onSkip != null) onSkip(false)
                            else FeedbackFormState.skip(context, form, never = false)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
            if (!testing) {
                Text(
                    "Never show me another one",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            FeedbackFormState.skip(context, form, never = true)
                            onDismiss()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/** One question: its number, its words, and the control its kind asks for. */
@Composable
private fun FeedbackQuestionCard(
    number: Int,
    of: Int,
    question: FeedbackQuestion,
    answer: FeedbackAnswer?,
    onAnswer: (FeedbackAnswer) -> Unit
) {
    val accent = curioRoseInk()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            accent,
            0.05f
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Question $number of $of",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                question.prompt,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                )
            )
            when {
                question.written -> {
                    val text = (answer as? FeedbackAnswer.Written)?.text.orEmpty()
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            onAnswer(
                                FeedbackAnswer.Written(
                                    question.id,
                                    it.take(FeedbackApi.MAX_WRITTEN)
                                )
                            )
                        },
                        minLines = 2,
                        maxLines = 5,
                        placeholder = { Text("Say it in your own words") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                question.multi -> {
                    val chosen = (answer as? FeedbackAnswer.Many)?.options.orEmpty()
                    question.options.forEachIndexed { index, option ->
                        val checked = chosen.contains(index)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onAnswer(
                                        FeedbackAnswer.Many(
                                            question.id,
                                            if (checked) chosen - index else chosen + index
                                        )
                                    )
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    onAnswer(
                                        FeedbackAnswer.Many(
                                            question.id,
                                            if (checked) chosen - index else chosen + index
                                        )
                                    )
                                }
                            )
                            Text(option, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                else -> {
                    val picked = (answer as? FeedbackAnswer.One)?.option ?: -1
                    question.options.forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAnswer(FeedbackAnswer.One(question.id, index)) }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = picked == index,
                                onClick = { onAnswer(FeedbackAnswer.One(question.id, index)) }
                            )
                            Text(option, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/** The kind's own name, for the form list in the moderation room. */
fun feedbackKindLabel(kind: String): String = when (kind) {
    FeedbackKinds.MULTI -> "Pick any"
    FeedbackKinds.TEXT -> "Written answer"
    else -> "Pick one"
}
