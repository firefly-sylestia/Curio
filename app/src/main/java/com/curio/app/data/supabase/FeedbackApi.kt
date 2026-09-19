package com.curio.app.data.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

/**
 * What a question asks. The wire values are the SCHEMA's own (`SINGLE`,
 * `MULTI`, `TEXT`) and are held as strings rather than an enum on the way in,
 * so a kind a newer server adds can never fail to be READ by this build.
 */
object FeedbackKinds {
    const val SINGLE = "SINGLE"
    const val MULTI = "MULTI"
    const val TEXT = "TEXT"
}

/** One question of a form: its prompt, what it asks for, and its options. */
data class FeedbackQuestion(
    /** Stable per form — this is the key an answer comes back under. */
    val id: String,
    val prompt: String,
    val kind: String,
    /** Empty for a TEXT question. */
    val options: List<String>
) {
    val single: Boolean get() = kind == FeedbackKinds.SINGLE
    val multi: Boolean get() = kind == FeedbackKinds.MULTI
    val written: Boolean get() = kind == FeedbackKinds.TEXT
}

/**
 * One question sheet. [isTest] is the team's own walkthrough: a test is never
 * shown to a member, never becomes the live form and never touches the
 * fourteen-day clock.
 */
data class FeedbackForm(
    val id: String,
    val title: String,
    val intro: String,
    val questions: List<FeedbackQuestion>,
    /** draft | live | closed */
    val status: String,
    val isTest: Boolean,
    val createdAtMillis: Long,
    val publishedAtMillis: Long?
) {
    val live: Boolean get() = status == "live" && !isTest
    val draft: Boolean get() = status == "draft"
}

/** A form being written or edited. */
data class FeedbackDraft(
    val title: String,
    val intro: String,
    val questions: List<FeedbackQuestion>,
    val isTest: Boolean = false
)

/**
 * One member's answer to one question. A sealed shape rather than a map, so a
 * written answer can never be mistaken for a picked option on the way out.
 */
sealed interface FeedbackAnswer {
    val questionId: String

    data class One(override val questionId: String, val option: Int) : FeedbackAnswer
    data class Many(override val questionId: String, val options: List<Int>) : FeedbackAnswer
    data class Written(override val questionId: String, val text: String) : FeedbackAnswer
}

/**
 * One line of the tally, as the server counts it: a question's option (or its
 * written answers, or a skip) and how many members it has.
 */
data class FeedbackTallyRow(
    val questionId: String,
    val kind: String,
    val optionIndex: Int,
    val total: Int
)

/** The whole tally of one form, with the percentages the results page draws. */
data class FeedbackResults(
    val form: FeedbackForm,
    val rows: List<FeedbackTallyRow>
) {
    /** Everyone who answered AT LEAST one question (the biggest option total). */
    val answered: Int
        get() = rows.filter { it.optionIndex >= 0 }.maxOfOrNull { it.total } ?: 0

    /** How many members answered this question at all (its fullest option). */
    fun answeredFor(questionId: String): Int =
        rows.filter { it.questionId == questionId && it.optionIndex >= 0 }
            .maxOfOrNull { it.total } ?: 0

    /** The written-answer count for a TEXT question. */
    fun writtenFor(questionId: String): Int =
        rows.firstOrNull { it.questionId == questionId && it.optionIndex < 0 }?.total ?: 0

    /** One option's count. */
    fun votesFor(questionId: String, optionIndex: Int): Int =
        rows.firstOrNull { it.questionId == questionId && it.optionIndex == optionIndex }
            ?.total ?: 0

    /** One option's share of everyone who answered the question, 0f..1f. */
    fun shareFor(questionId: String, optionIndex: Int): Float {
        val base = if (rows.any { it.questionId == questionId && it.kind == FeedbackKinds.MULTI }) {
            // A multi-select question's base is the ANSWERS, not the votes: two
            // picks by one member are one member, so the shares of the options
            // are allowed to add up past 100% and mean it.
            answeredFor(questionId)
        } else {
            rows.filter {
                it.questionId == questionId && it.optionIndex >= 0
            }.sumOf { it.total }
        }
        if (base <= 0) return 0f
        return votesFor(questionId, optionIndex).toFloat() / base.toFloat()
    }


    /** How many members waved the form away, and how many never want it again. */
    fun skips(kind: String): Int =
        rows.firstOrNull { it.questionId == "__${kind.lowercase()}" }?.total ?: 0
}

/**
 * The feedback forms' REST layer — the only place the app talks to
 * `feedback_forms`, `feedback_answers` and `feedback_skips`.
 *
 * Every rule the UI enforces (the team's forms switch, the one-form-at-a-time
 * rule, the fourteen-day clock, the question and option ceilings) is enforced
 * again in `supabase/schema.sql`. This layer never assumes the client is the
 * guard, and it never sends anything that could identify the member who
 * answered: [FeedbackAnswer] carries no user, device or account field, and the
 * schema has nowhere to put one.
 */
object FeedbackApi {

    private const val FORMS = "/rest/v1/feedback_forms"
    private const val ANSWERS = "/rest/v1/feedback_answers"
    private const val SKIPS = "/rest/v1/feedback_skips"
    private const val RPC_PUBLISH = "/rest/v1/rpc/curio_publish_feedback_form"
    private const val RPC_CLOSE = "/rest/v1/rpc/curio_close_feedback_form"
    private const val RPC_TALLY = "/rest/v1/rpc/curio_feedback_tally"

    private const val COLUMNS =
        "id,title,intro,questions,status,is_test,created_at,published_at"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** The ceilings the schema checks, said once for the whole app. */
    const val MAX_QUESTIONS = 5
    const val MAX_OPTIONS = 4
    const val MIN_OPTIONS = 2
    const val MAX_TITLE = 120
    const val MAX_INTRO = 400
    const val MAX_PROMPT = 140
    const val MAX_OPTION = 120
    const val MAX_WRITTEN = 280

    /** How long the app waits before a published form may be replaced. */
    const val CADENCE_DAYS = 14

    /**
     * Why this draft cannot be published, or null when it is fine. Mirrors the
     * schema's checks so the team is told in the editor instead of by a refusal
     * after the fact.
     */
    fun draftProblem(draft: FeedbackDraft): String? {
        val title = draft.title.trim()
        if (title.length < 3) return "Give the form a name first."
        if (title.length > MAX_TITLE) return "Keep the name under $MAX_TITLE characters."
        if (draft.intro.length > MAX_INTRO) {
            return "Keep the opening line under $MAX_INTRO characters."
        }
        if (draft.questions.isEmpty()) return "Add at least one question."
        if (draft.questions.size > MAX_QUESTIONS) {
            return "A form can ask up to $MAX_QUESTIONS questions."
        }
        draft.questions.forEachIndexed { index, question ->
            if (question.prompt.isBlank()) return "Question ${index + 1} needs its words."
            if (question.prompt.length > MAX_PROMPT) {
                return "Keep question ${index + 1} under $MAX_PROMPT characters."
            }
            if (!question.written) {
                if (question.options.size < MIN_OPTIONS) {
                    return "Question ${index + 1} needs at least $MIN_OPTIONS options."
                }
                if (question.options.size > MAX_OPTIONS) {
                    return "Question ${index + 1} can offer up to $MAX_OPTIONS options."
                }
                if (question.options.any { it.isBlank() }) {
                    return "Every option of question ${index + 1} needs its words."
                }
                if (question.options.any { it.length > MAX_OPTION }) {
                    return "Keep each option of question ${index + 1} under $MAX_OPTION characters."
                }
            }
        }
        return null
    }

    /** Wires one draft into the JSON the schema stores. */
    private fun questionsJson(questions: List<FeedbackQuestion>): JSONArray {
        val array = JSONArray()
        questions.forEach { question ->
            val row = JSONObject()
                .put("id", question.id)
                .put("prompt", question.prompt.trim())
                .put("kind", question.kind)
            if (!question.written) {
                val options = JSONArray()
                question.options.forEach { options.put(it.trim()) }
                row.put("options", options)
            }
            array.put(row)
        }
        return array
    }

    /**
     * The one PUBLISHED form, or null when there is none. A test is invisible
     * here: RLS hides a live test from a member, so the same query answers null
     * for them and the real form for the team.
     */
    suspend fun liveForm(accessToken: String): Result<FeedbackForm?> =
        withContext(Dispatchers.IO) {
            mapped {
                val path = "$FORMS?select=$COLUMNS" +
                    "&status=eq.live&is_test=eq.false" +
                    "&order=published_at.desc&limit=1"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                parseForms(SupabaseClient.executeBody(request)).firstOrNull()
            }
        }

    /** One form by id — the editor's own read (team only, by RLS). */
    suspend fun form(accessToken: String, formId: String): Result<FeedbackForm?> =
        withContext(Dispatchers.IO) {
            mapped {
                val path = "$FORMS?select=$COLUMNS&id=eq.$formId&limit=1"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                parseForms(SupabaseClient.executeBody(request)).firstOrNull()
            }
        }

    /** Every form the team keeps: newest publication first, drafts at the end. */
    suspend fun forms(accessToken: String, limit: Int = 50): Result<List<FeedbackForm>> =
        withContext(Dispatchers.IO) {
            mapped {
                val path = "$FORMS?select=$COLUMNS&order=created_at.desc&limit=$limit"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                parseForms(SupabaseClient.executeBody(request))
            }
        }

    /** Writes a new form (a draft until it is published). Hands back its id. */
    suspend fun createForm(accessToken: String, draft: FeedbackDraft): Result<String> =
        withContext(Dispatchers.IO) {
            mapped {
                draftProblem(draft)?.let { throw IllegalArgumentException(it) }
                val payload = JSONObject()
                    .put("title", draft.title.trim())
                    .put("intro", draft.intro.trim())
                    .put("questions", questionsJson(draft.questions))
                    .put("is_test", draft.isTest)
                    .put("status", "draft")
                val request = SupabaseClient.requestBuilder(FORMS, accessToken)
                    .header("Prefer", "return=representation")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                parseForms(SupabaseClient.executeBody(request)).firstOrNull()?.id
                    ?: throw CommunityError("The form could not be saved.")
            }
        }

    /** Edits a form that has not gone out: its words and its questions. */
    suspend fun updateForm(
        accessToken: String,
        formId: String,
        draft: FeedbackDraft
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            draftProblem(draft)?.let { throw IllegalArgumentException(it) }
            val payload = JSONObject()
                .put("title", draft.title.trim())
                .put("intro", draft.intro.trim())
                .put("questions", questionsJson(draft.questions))
                .put("is_test", draft.isTest)
            val request = SupabaseClient.requestBuilder("$FORMS?id=eq.$formId", accessToken)
                .header("Prefer", "return=minimal")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Throws a form away, with every answer it collected. */
    suspend fun deleteForm(accessToken: String, formId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val request = SupabaseClient.requestBuilder("$FORMS?id=eq.$formId", accessToken)
                    .delete()
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /**
     * Publishes a form. `force` is the owner's override of the fourteen-day
     * clock; the server re-checks both the permission and the clock, so a
     * refused publish answers with the real reason (how many days are left).
     */
    suspend fun publish(
        accessToken: String,
        formId: String,
        force: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
                .put("p_form_id", formId)
                .put("p_force", force)
            val request = SupabaseClient.requestBuilder(RPC_PUBLISH, accessToken)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Closes the live form without publishing another one. */
    suspend fun close(accessToken: String, formId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val payload = JSONObject().put("p_form_id", formId)
                val request = SupabaseClient.requestBuilder(RPC_CLOSE, accessToken)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /**
     * Sends the member's answers. Anonymous by construction: the payload is the
     * answers and the form, and there is no identity column to add to it.
     */
    suspend fun submit(
        accessToken: String,
        formId: String,
        answers: List<FeedbackAnswer>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            if (answers.isEmpty()) throw IllegalArgumentException("Answer something first.")
            val array = JSONArray()
            answers.forEach { answer ->
                val row = JSONObject().put("id", answer.questionId)
                when (answer) {
                    is FeedbackAnswer.One -> row.put("option", answer.option)
                    is FeedbackAnswer.Many -> {
                        val picks = JSONArray()
                        answer.options.sorted().forEach { picks.put(it) }
                        row.put("options", picks)
                    }
                    is FeedbackAnswer.Written -> {
                        val text = answer.text.trim()
                        if (text.isEmpty()) throw IllegalArgumentException("Answer something first.")
                        if (text.length > MAX_WRITTEN) {
                            throw IllegalArgumentException(
                                "Keep it under $MAX_WRITTEN characters (it's ${text.length})."
                            )
                        }
                        row.put("text", text)
                    }
                }
                array.put(row)
            }
            val payload = JSONObject()
                .put("form_id", formId)
                .put("answers", array)
            val request = SupabaseClient.requestBuilder(ANSWERS, accessToken)
                .header("Prefer", "return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Records a "not now" or a "never again". Counted, never attributed. */
    suspend fun recordSkip(
        accessToken: String,
        formId: String,
        never: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
                .put("form_id", formId)
                .put("kind", if (never) "never" else "skip")
            val request = SupabaseClient.requestBuilder(SKIPS, accessToken)
                .header("Prefer", "return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** The whole tally of one form — one call, no answers downloaded. */
    suspend fun tally(accessToken: String, form: FeedbackForm): Result<FeedbackResults> =
        withContext(Dispatchers.IO) {
            mapped {
                val payload = JSONObject().put("p_form_id", form.id)
                val request = SupabaseClient.requestBuilder(RPC_TALLY, accessToken)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                val rows = JSONArray(SupabaseClient.executeBody(request))
                val parsed = buildList(rows.length()) {
                    for (index in 0 until rows.length()) {
                        val row = rows.optJSONObject(index) ?: continue
                        add(
                            FeedbackTallyRow(
                                questionId = row.optString("question_id"),
                                kind = row.optString("kind"),
                                optionIndex = row.optInt("option_index", -1),
                                total = row.optInt("total", 0)
                            )
                        )
                    }
                }
                FeedbackResults(form = form, rows = parsed)
            }
        }

    /**
     * The written answers themselves, in the order they arrived — the team
     * reads these beside the tally. Newest first, capped, and only readable by
     * a member with the forms switch (RLS refuses everyone else).
     */
    suspend fun writtenAnswers(
        accessToken: String,
        formId: String,
        limit: Int = 120
    ): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        mapped {
            val path = "$ANSWERS?select=answers,created_at&form_id=eq.$formId" +
                "&order=created_at.desc&limit=$limit"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            val rows = JSONArray(SupabaseClient.executeBody(request))
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val answers = row.optJSONArray("answers") ?: continue
                    for (a in 0 until answers.length()) {
                        val entry = answers.optJSONObject(a) ?: continue
                        val text = entry.optString("text")
                        if (text.isNotBlank() && text != "null") {
                            add(entry.optString("id") to text)
                        }
                    }
                }
            }
        }
    }

    // ── parsing ──────────────────────────────────────────────────────────

    private fun parseForms(body: String): List<FeedbackForm> {
        val array = JSONArray(body)
        val forms = ArrayList<FeedbackForm>(array.length())
        for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            forms += parseForm(row)
        }
        return forms
    }

    private fun parseForm(row: JSONObject): FeedbackForm {
        val questions = ArrayList<FeedbackQuestion>()
        val array = row.optJSONArray("questions")
        if (array != null) {
            for (index in 0 until array.length()) {
                val q = array.optJSONObject(index) ?: continue
                val options = ArrayList<String>()
                q.optJSONArray("options")?.let { opts ->
                    for (o in 0 until opts.length()) options += opts.optString(o)
                }
                questions += FeedbackQuestion(
                    id = q.optString("id").ifBlank { "q${index + 1}" },
                    prompt = q.optString("prompt"),
                    kind = q.optString("kind").ifBlank { FeedbackKinds.SINGLE },
                    options = options
                )
            }
        }
        return FeedbackForm(
            id = row.optString("id"),
            title = row.optString("title"),
            intro = row.optString("intro").takeIf { it != "null" }.orEmpty(),
            questions = questions,
            status = row.optString("status").ifBlank { "draft" },
            isTest = row.optBoolean("is_test", false),
            createdAtMillis = epochMillis(row.optString("created_at")),
            publishedAtMillis = row.optString("published_at")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.let { epochMillis(it) }
        )
    }

    private fun <T> mapped(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (failure: Throwable) {
        Result.failure(CommunityError(communityMessage(failure), failure.isTransportFailure()))
    }

    private fun mappedUnit(block: () -> Unit): Result<Unit> = mapped(block)

    /** PostgREST answers with an offset timestamp — 0 when it cannot be read. */
    private fun epochMillis(iso: String): Long =
        runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrDefault(0L)
}
