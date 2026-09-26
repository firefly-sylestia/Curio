package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
import com.curio.app.ui.theme.curioSheetContainerColor
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.supabase.FeedbackApi
import com.curio.app.data.supabase.FeedbackAnswer
import com.curio.app.data.supabase.FeedbackDraft
import com.curio.app.data.supabase.FeedbackForm
import com.curio.app.data.supabase.FeedbackKinds
import com.curio.app.data.supabase.FeedbackQuestion
import com.curio.app.data.supabase.FeedbackResults
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCardDraft
import com.curio.app.data.supabase.KIND_NOTE
import com.curio.app.data.supabase.communityMessage
import com.curio.app.features.feedback.FeedbackFormSheet
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.launch

/**
 * The FORMS half of the moderation room (v403).
 *
 * What the team does here, in the words of the request that asked for it: write
 * a form (up to five questions, up to four options each), TEST it as often as
 * they like, then PUBLISH it. Publishing is one form every fourteen days, the
 * owner can override that, and a new publication closes the one before it. The
 * answers come back here as percentages, with the written ones underneath, and
 * the whole result can be posted to the wall as a note.
 *
 * Everything that actually decides any of this lives in the database (see
 * `supabase/schema.sql` §6f): this screen only draws the doors.
 */
@Composable
internal fun ModerationFormsTab(accessToken: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var forms by remember { mutableStateOf<List<FeedbackForm>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<FeedbackResults?>(null) }
    var written by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var building by remember { mutableStateOf<FeedbackForm?>(null) }
    var creating by remember { mutableStateOf(false) }
    var walking by remember { mutableStateOf<FeedbackForm?>(null) }
    var overrideFor by remember { mutableStateOf<FeedbackForm?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun reload() {
        loading = true
        scope.launch {
            FeedbackApi.forms(accessToken).fold(
                onSuccess = {
                    forms = it
                    error = null
                },
                onFailure = { error = it.message }
            )
            loading = false
        }
    }

    LaunchedEffect(accessToken) { reload() }

    val live = forms.firstOrNull { it.live }
    val tests = forms.filter { it.isTest }
    val drafts = forms.filter { it.draft }
    val closed = forms.filter { !it.live && !it.draft && !it.isTest }

    // The live form's tally, read once per form. Its written answers are a
    // second read because the tally counts them rather than carrying them.
    LaunchedEffect(live?.id) {
        val form = live ?: return@LaunchedEffect
        FeedbackApi.tally(accessToken, form).fold(
            onSuccess = { results = it },
            onFailure = { error = it.message }
        )
        FeedbackApi.writtenAnswers(accessToken, form.id).onSuccess { written = it }
    }

    fun publish(form: FeedbackForm, force: Boolean) {
        busy = true
        scope.launch {
            FeedbackApi.publish(accessToken, form.id, force).fold(
                onSuccess = {
                    notice = if (form.isTest) "A test run is up. Walk it as a member would."
                    else "Published. It is on Home and in Support now."
                    overrideFor = null
                    reload()
                },
                onFailure = { failure ->
                    val words = failure.message.orEmpty()
                    // The server's own refusal names how long the clock has to
                    // run. That refusal is what offers the owner the override,
                    // instead of a second copy of the rule living here.
                    if (!force && words.contains("days ago", true)) {
                        overrideFor = form
                        error = null
                    } else {
                        error = words
                    }
                }
            )
            busy = false
        }
    }

    /** A copy of a form, published as a test, opened for the team to walk. */
    fun testRun(form: FeedbackForm) {
        busy = true
        scope.launch {
            val draft = FeedbackDraft(
                title = form.title.take(FeedbackApi.MAX_TITLE),
                intro = form.intro,
                questions = form.questions,
                isTest = true
            )
            val id = FeedbackApi.createForm(accessToken, draft).getOrElse { failure ->
                error = failure.message
                busy = false
                return@launch
            }
            FeedbackApi.publish(accessToken, id, force = true).onFailure { failure ->
                error = failure.message
            }
            FeedbackApi.form(accessToken, id).fold(
                onSuccess = { copy ->
                    if (copy != null) walking = copy else error = "The test form could not be opened."
                },
                onFailure = { error = it.message }
            )
            busy = false
            reload()
        }
    }

    fun remove(form: FeedbackForm) {
        busy = true
        scope.launch {
            FeedbackApi.deleteForm(accessToken, form.id).fold(
                onSuccess = {
                    notice = "Form thrown away."
                    reload()
                },
                onFailure = { error = it.message }
            )
            busy = false
        }
    }

    fun closeLive(form: FeedbackForm) {
        busy = true
        scope.launch {
            FeedbackApi.close(accessToken, form.id).fold(
                onSuccess = {
                    notice = "The form is closed. Its answers are kept."
                    reload()
                },
                onFailure = { error = it.message }
            )
            busy = false
        }
    }

    /** Posts the live form's result to the wall as a plain note. */
    fun postResult(form: FeedbackForm, tally: FeedbackResults) {
        busy = true
        val handle = AppPreferences.getUsername(context).ifBlank { "A curious soul" }
        scope.launch {
            val lines = form.questions.map { question ->
                val best = question.options.indices
                    .maxByOrNull { tally.votesFor(question.id, it) }
                val share = best?.let { (tally.shareFor(question.id, it) * 100).toInt() } ?: 0
                val option = best?.let { question.options.getOrNull(it) }.orEmpty()
                "· ${question.prompt}\n   $option ($share%)"
            }.joinToString("\n")
            val body = buildString {
                append("We asked, and ")
                append(tally.answered)
                append(if (tally.answered == 1) " member answered" else " members answered")
                append(".\n\n")
                append(lines)
                if (tally.skips("never") > 0) {
                    append("\n\n")
                    append(tally.skips("never"))
                    append(" asked never to be asked again, and that is respected.")
                }
            }
            CommunityApi.post(
                accessToken = accessToken,
                draft = CommunityCardDraft(
                    topicName = "",
                    categoryName = "",
                    categorySlug = "",
                    categoryGlyph = "",
                    accentHex = "",
                    factText = body.take(CommunityApi.MAX_FACT_CHARS),
                    kind = KIND_NOTE
                ),
                handle = handle
            ).fold(
                onSuccess = { notice = "Posted to the wall." },
                onFailure = { error = communityMessage(it) }
            )
            busy = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SocialCard {
            SocialCardHeading(
                icon = CurioIcons.AutoAwesome,
                title = "Feedback forms",
                subtitle = "Ask the members once in a while. The answers come back counted, " +
                    "and never with a name on them."
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SocialPill(
                    label = "New form",
                    onClick = { creating = true },
                    icon = CurioIcons.Add,
                    tone = SocialPillTone.ACCENT,
                    enabled = !busy
                )
                SocialPill(
                    label = if (loading) "Reading…" else "Refresh",
                    onClick = { reload() },
                    icon = CurioIcons.Refresh,
                    enabled = !loading
                )
            }
            cadenceLine(forms)?.let { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            notice?.let { SocialNote(it, false) }
            error?.let { SocialNote(it, true) }
        }

        if (live != null) {
            val tally = results?.takeIf { it.form.id == live.id }
            SocialCard {
                SocialCardHeading(
                    icon = CurioIcons.TaskAlt,
                    title = "Live now: ${live.title}",
                    subtitle = "${live.questions.size} question" +
                        (if (live.questions.size == 1) "" else "s") +
                        " · answered by ${tally?.answered ?: 0}"
                )
                if (tally == null) {
                    Text(
                        "Counting the answers…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FormResults(form = live, tally = tally, written = written)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SocialPill(
                        label = "Post the result",
                        onClick = { tally?.let { postResult(live, it) } },
                        icon = CurioIcons.Hub,
                        tone = SocialPillTone.ACCENT,
                        enabled = tally != null && !busy
                    )
                    SocialPill(
                        label = "Close it",
                        onClick = { closeLive(live) },
                        icon = CurioIcons.Lock,
                        enabled = !busy
                    )
                }
            }
        }

        FormGroup(
            title = "Being written",
            blurb = "Nothing here is public yet. Test one as often as you like.",
            forms = drafts,
            busy = busy,
            onOpen = { building = it },
            onTest = { testRun(it) },
            onPublish = { publish(it, false) },
            onDelete = { remove(it) }
        )

        FormGroup(
            title = "Test runs",
            blurb = "Walkthroughs the members never see. They are counted on their own.",
            forms = tests,
            busy = busy,
            onOpen = { building = it },
            onTest = { walking = it },
            onPublish = null,
            onDelete = { remove(it) }
        )

        if (closed.isNotEmpty()) {
            FormGroup(
                title = "Answered and put away",
                blurb = "Closed forms keep their answers here.",
                forms = closed,
                busy = busy,
                onOpen = { building = it },
                onTest = null,
                onPublish = null,
                onDelete = { remove(it) }
            )
        }
    }

    if (creating || building != null) {
        FormBuilderSheet(
            existing = building,
            busy = busy,
            onDismiss = {
                building = null
                creating = false
            },
            onSave = { draft ->
                busy = true
                scope.launch {
                    val edit = building
                    val call = if (edit == null) {
                        FeedbackApi.createForm(accessToken, draft).map { }
                    } else {
                        FeedbackApi.updateForm(accessToken, edit.id, draft)
                    }
                    call.fold(
                        onSuccess = {
                            notice = if (edit == null) "Saved. Test it, then publish it."
                            else "Form updated."
                            building = null
                            creating = false
                            reload()
                        },
                        onFailure = { error = it.message }
                    )
                    busy = false
                }
            }
        )
    }

    overrideFor?.let { form ->
        AlertDialog(
            onDismissRequest = { if (!busy) overrideFor = null },
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            title = {
                Text(
                    "Publish before the clock?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            },
            text = {
                Text(
                    "A form went out recently. Publishing this one now closes it and " +
                        "starts the fourteen days again, which is the owner's call to make.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                CurioGlassWindowBlur()
                TextButton(
                    onClick = { publish(form, force = true) },
                    enabled = !busy,
                    colors = curioDialogActionButtonColors()
                ) {
                    Text(
                        "Publish anyway",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { overrideFor = null },
                    colors = curioDialogActionButtonColors()
                ) { Text("Wait", style = MaterialTheme.typography.labelLarge) }
            }
        )
    }

    walking?.let { form ->
        FeedbackFormSheet(
            form = form,
            onDismiss = { walking = null },
            testing = true,
            onSubmit = { answers, done ->
                scope.launch {
                    FeedbackApi.submit(accessToken, form.id, answers).fold(
                        onSuccess = {
                            done(true, null)
                            notice = "Test answer counted. The results are below."
                            reload()
                        },
                        onFailure = { done(false, it.message) }
                    )
                }
            },
            onSkip = { walking = null }
        )
    }
}

/** The cadence, said plainly: when the last one went out, and when's the next. */
private fun cadenceLine(forms: List<FeedbackForm>): String? {
    val last = forms.filter { !it.isTest }
        .mapNotNull { it.publishedAtMillis }
        .maxOrNull() ?: return null
    val days = ((System.currentTimeMillis() - last) / 86_400_000L).toInt()
    val left = FeedbackApi.CADENCE_DAYS - days
    return when {
        left <= 0 -> "The last form went out $days days ago, so another one may go out now."
        left == 1 -> "The last form went out $days days ago. The next may go out tomorrow."
        else -> "The last form went out $days days ago. The next may go out in $left days."
    }
}

/** One group of forms (drafts, tests, the ones put away) with its own doors. */
@Composable
private fun FormGroup(
    title: String,
    blurb: String,
    forms: List<FeedbackForm>,
    busy: Boolean,
    onOpen: (FeedbackForm) -> Unit,
    onTest: ((FeedbackForm) -> Unit)?,
    onPublish: ((FeedbackForm) -> Unit)?,
    onDelete: (FeedbackForm) -> Unit
) {
    if (forms.isEmpty()) return
    SocialCard {
        SocialCardHeading(icon = CurioIcons.Notes, title = title, subtitle = blurb)
        forms.forEachIndexed { index, form ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    form.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    "${form.questions.size} question" +
                        (if (form.questions.size == 1) "" else "s") +
                        " · " + form.questions.joinToString(", ") { feedbackKindShort(it.kind) } +
                        " · " + statusWord(form),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SocialPill(
                        label = "Edit",
                        onClick = { onOpen(form) },
                        icon = CurioIcons.Edit,
                        enabled = !busy
                    )
                    onTest?.let { run ->
                        SocialPill(
                            label = if (form.isTest) "Walk it" else "Test it",
                            onClick = { run(form) },
                            icon = CurioIcons.PlayArrow,
                            enabled = !busy
                        )
                    }
                    onPublish?.let { send ->
                        SocialPill(
                            label = "Publish",
                            onClick = { send(form) },
                            icon = CurioIcons.Public,
                            tone = SocialPillTone.ACCENT,
                            enabled = !busy
                        )
                    }
                    SocialPill(
                        label = "Throw away",
                        onClick = { onDelete(form) },
                        icon = CurioIcons.Delete,
                        tone = SocialPillTone.DESTRUCTIVE,
                        enabled = !busy
                    )
                }
            }
        }
    }
}

private fun statusWord(form: FeedbackForm): String = when {
    form.isTest && form.live -> "a test run, open"
    form.isTest -> "a test run, put away"
    form.draft -> "not published yet"
    form.live -> "live now"
    else -> "closed"
}

private fun feedbackKindShort(kind: String): String = when (kind) {
    FeedbackKinds.MULTI -> "pick any"
    FeedbackKinds.TEXT -> "written"
    else -> "pick one"
}

/** The tally as the team reads it: a bar and a count per option. */
@Composable
private fun FormResults(
    form: FeedbackForm,
    tally: FeedbackResults,
    written: List<Pair<String, String>>
) {
    val accent = curioRoseInk()
    form.questions.forEach { question ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                question.prompt,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (question.written) {
                val count = tally.writtenFor(question.id)
                Text(
                    if (count == 1) "1 written answer" else "$count written answers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                written.filter { it.first == question.id }
                    .take(5)
                    .forEach { (_, text) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = lerp(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                accent,
                                0.06f
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                if (count > 5) {
                    Text(
                        "…and ${count - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val base = tally.answeredFor(question.id)
                question.options.forEachIndexed { index, option ->
                    val votes = tally.votesFor(question.id, index)
                    val share = tally.shareFor(question.id, index)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                option,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(share * 100).toInt()}% · $votes",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(share.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(accent)
                            )
                        }
                    }
                }
                Text(
                    if (base == 1) "1 member answered" else "$base members answered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    val skipped = tally.skips("skip")
    val never = tally.skips("never")
    if (skipped > 0 || never > 0) {
        Text(
            buildString {
                append("Waved away $skipped time")
                append(if (skipped == 1) "" else "s")
                append(" · ")
                append(never)
                append(" asked never to be asked again")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The builder: the name, the line above the questions, and up to five
 * questions of the three kinds, each with up to four options. Saving writes a
 * draft; publishing is a separate, deliberate move on the list.
 */
@Composable
private fun FormBuilderSheet(
    existing: FeedbackForm?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (FeedbackDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var intro by remember(existing?.id) { mutableStateOf(existing?.intro.orEmpty()) }
    var questions by remember(existing?.id) {
        mutableStateOf(existing?.questions ?: listOf(blankQuestion(0)))
    }
    var trouble by remember(existing?.id) { mutableStateOf<String?>(null) }

    val draft = FeedbackDraft(
        title = title,
        intro = intro,
        questions = questions,
        isTest = existing?.isTest ?: false
    )
    val problem = FeedbackApi.draftProblem(draft)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioSheetContainerColor(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        CurioGlassWindowBlur()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (existing == null) "A new form" else "Edit the form",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                )
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(FeedbackApi.MAX_TITLE) },
                singleLine = true,
                label = { Text("What is it called") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = intro,
                onValueChange = { intro = it.take(FeedbackApi.MAX_INTRO) },
                minLines = 2,
                maxLines = 3,
                label = { Text("A line above the questions") },
                modifier = Modifier.fillMaxWidth()
            )

            questions.forEachIndexed { index, question ->
                QuestionEditor(
                    number = index + 1,
                    question = question,
                    canRemove = questions.size > 1,
                    onQuestion = { updated ->
                        questions = questions.toMutableList().also { it[index] = updated }
                    },
                    onRemove = {
                        questions = questions.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            if (questions.size < FeedbackApi.MAX_QUESTIONS) {
                SocialPill(
                    label = "Add a question",
                    onClick = { questions = questions + blankQuestion(questions.size) },
                    icon = CurioIcons.Add,
                    enabled = !busy
                )
            }

            (trouble ?: problem)?.let { line ->
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
                SocialPill(
                    label = if (busy) "Saving…" else "Save the draft",
                    onClick = { onSave(draft) },
                    icon = CurioIcons.Check,
                    tone = SocialPillTone.ACCENT,
                    enabled = problem == null && !busy
                )
                SocialPill(
                    label = "Cancel",
                    onClick = onDismiss,
                    enabled = !busy
                )
            }
        }
    }
}

/** One question in the builder: its kind, its words, and its options. */
@Composable
private fun QuestionEditor(
    number: Int,
    question: FeedbackQuestion,
    canRemove: Boolean,
    onQuestion: (FeedbackQuestion) -> Unit,
    onRemove: () -> Unit
) {
    val accent = curioRoseInk()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Question $number",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    CurioIcon(
                        CurioIcons.Close, "Remove this question",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onRemove() }
                            .padding(4.dp)
                    )
                }
            }
            OutlinedTextField(
                value = question.prompt,
                onValueChange = { onQuestion(question.copy(prompt = it.take(FeedbackApi.MAX_PROMPT))) },
                minLines = 1,
                maxLines = 2,
                label = { Text("What do you want to ask") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SocialPill(
                    label = "Pick one",
                    onClick = { onQuestion(question.copy(kind = FeedbackKinds.SINGLE)) },
                    tone = if (question.single) SocialPillTone.ACCENT else SocialPillTone.NEUTRAL
                )
                SocialPill(
                    label = "Pick any",
                    onClick = { onQuestion(question.copy(kind = FeedbackKinds.MULTI)) },
                    tone = if (question.multi) SocialPillTone.ACCENT else SocialPillTone.NEUTRAL
                )
                SocialPill(
                    label = "Written",
                    onClick = { onQuestion(question.copy(kind = FeedbackKinds.TEXT)) },
                    tone = if (question.written) SocialPillTone.ACCENT else SocialPillTone.NEUTRAL
                )
            }
            if (!question.written) {
                question.options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = option,
                            onValueChange = { updated ->
                                val options = question.options.toMutableList()
                                options[index] = updated.take(FeedbackApi.MAX_OPTION)
                                onQuestion(question.copy(options = options))
                            },
                            singleLine = true,
                            label = { Text("Option ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        if (question.options.size > FeedbackApi.MIN_OPTIONS) {
                            CurioIcon(
                                CurioIcons.Remove, "Remove this option",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 18.dp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onQuestion(
                                            question.copy(
                                                options = question.options.toMutableList()
                                                    .also { it.removeAt(index) }
                                            )
                                        )
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
                if (question.options.size < FeedbackApi.MAX_OPTIONS) {
                    SocialPill(
                        label = "Add an option",
                        onClick = {
                            onQuestion(question.copy(options = question.options + ""))
                        },
                        icon = CurioIcons.Add
                    )
                }
            }
        }
    }
}

/** A fresh question with the two options the schema asks a choice to have. */
private fun blankQuestion(index: Int): FeedbackQuestion = FeedbackQuestion(
    id = "q${index + 1}-${System.currentTimeMillis() % 100_000}",
    prompt = "",
    kind = FeedbackKinds.SINGLE,
    options = listOf("", "")
)
