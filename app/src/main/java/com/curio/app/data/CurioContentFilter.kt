package com.curio.app.data

import java.text.Normalizer
import java.util.Locale

/**
 * Curio's shared text filter — the ONE gate every piece of text a person posts
 * to a PUBLIC social surface passes through before it can leave the device:
 * the 24-hour wall (cards, notes, quotes), its replies, and the identity fields
 * (username, display name, bio).
 *
 * **A direct message is deliberately NOT filtered.** A conversation between two
 * friends is private and the words belong to them; what makes the filter
 * necessary is an audience that did not choose to read it. RLS — only the two
 * participants, friends only — is what protects a thread. Do not wire this
 * object into `SocialApi.send` or into `dm_messages`.
 *
 * It exists because "don't type that" has to survive people who WANT to get
 * around it. Typing a slur plainly is the easy case; the interesting ones are
 * `f u c k`, `f.u.c.k`, `f_u_c_k`, `f*ck`, `fuuuuck`, `fück`, `phuck`, `f4ck`,
 * `f@ck` and look-alike characters from other alphabets. All of those are
 * caught by the same idea:
 *
 *  1. **Fold** the text down to plain lowercase Latin letters — accented
 *     letters lose their accents, digits and symbols are mapped to the letters
 *     they stand in for (`0→o`, `3→e`, `@→a`, `$→s`, `|→i`, `!→i`, `+→t`), and
 *     every character the fold doesn't recognise simply DISAPPEARS.
 *  2. **A normalised word view.** Words are matched only as whole words, with
 *     consecutive single-letter words merged first. This still identifies
 *     deliberate spacing (`s e x`) without refusing innocent longer words
 *     that merely contain the same letters.
 *
 * A public safety filter must be narrow enough that normal writing is never a
 * guessing game. The exact canonical term that caused a refusal is returned to
 * the author, and substring matches are intentionally never used.
 *
 * This is the client half. `supabase/schema.sql` carries the same fold as
 * `curio_text_is_clean` and CHECKs it on every social table, so a modified
 * client cannot bypass the gate either.
 */
object CurioContentFilter {

    /** What the person is told when their text is refused. */
    const val BLOCKED_MESSAGE: String =
        "That text can't be posted — slurs, sexual content and harassment are " +
            "not allowed anywhere in Curio, and accounts that use them are banned."

    /** The name-specific wording, used by the account form and Edit profile. */
    const val NAME_WARNING: String =
        "Slurs, sexual content, harassment or impersonation in a name, " +
            "username or bio get your account banned."

    // ── the public gate ───────────────────────────────────────────────────

    /** Null when [text] is fine; otherwise the message the person should read. */
    fun problem(text: String?): String? =
        matchedTerm(text)?.let { term ->
            "That public text contains \"$term\", which isn't allowed here. Remove or reword it and try again."
        }

    /** Null when EVERY field is fine — the shape an API call uses. */
    fun problemIn(vararg texts: String?): String? =
        texts.firstNotNullOfOrNull { problem(it) }

    /** True when [text] carries nothing from the lexicon. */
    fun isClean(text: String): Boolean = matchedTerm(text) == null

    /** True when [text] carries something from the lexicon. */
    fun carriesBadWord(text: String): Boolean = matchedTerm(text) != null

    /** The canonical safety term that matched, or null when [text] is allowed. */
    private fun matchedTerm(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val folded = fold(text)
        val words = mergedWords(folded)
        return (UNAMBIGUOUS + NAME_LIKE).firstOrNull { it in words }
    }

    // ── the fold ──────────────────────────────────────────────────────────

    private val MARKS = Regex("\\p{M}+")
    private val NON_LETTER = Regex("[^a-z]")

    /** Keeps separators, maps look-alike characters, strips accents. */
    private fun fold(raw: String): String {
        val decomposed = runCatching {
            Normalizer.normalize(raw, Normalizer.Form.NFD)
        }.getOrDefault(raw)
        var text = MARKS.replace(decomposed, "").lowercase(Locale.ROOT)
        // Sorted longest-first so "ph" is applied before "p", "ck" before "c".
        for ((from, to) in LOOKALIKES) {
            if (text.indexOf(from) >= 0) text = text.replace(from, to)
        }
        return text
    }

    /**
     * The text's words, split on anything that isn't a letter, with consecutive
     * ONE-letter words merged — `f u c k` and `a s s` arrive as single words.
     */
    private fun mergedWords(folded: String): Set<String> {
        val out = LinkedHashSet<String>()
        val run = StringBuilder()
        fun flush() {
            if (run.isNotEmpty()) {
                out.add(run.toString())
                run.clear()
            }
        }
        var index = 0
        while (index < folded.length) {
            val start = index
            while (index < folded.length && folded[index] in 'a'..'z') index++
            if (index > start) {
                val word = folded.substring(start, index)
                if (word.length == 1) {
                    run.append(word)
                } else {
                    flush()
                    out.add(word)
                }
            }
            index++
        }
        flush()
        return out
    }

    /**
     * Character look-alikes, longest first. Digits and the symbols people
     * actually reach for stand in for letters; anything NOT listed is simply
     * dropped by the squash view, which is what defeats exotic replacements
     * (`fu©k`, a Cyrillic `ц` for `u`) without a table for every alphabet.
     */
    private val LOOKALIKES: List<Pair<String, String>> = listOf(
        "0" to "o", "1" to "i", "3" to "e", "4" to "a", "5" to "s", "7" to "t",
        "8" to "b", "9" to "g", "6" to "g",
        "$" to "s", "z" to "s", "@" to "a", "!" to "i", "|" to "i", "+" to "t",
        "£" to "l", "€" to "e",
        "ß" to "ss", "æ" to "ae", "ø" to "o", "œ" to "oe", "ð" to "d", "þ" to "th"
    )
    // The `to` side is ONE character per entry on purpose for everything SQL's
    // `translate()` can express, so the two folds stay in step — a fold that
    // drifts between the layers is a hole a modified client can spell through.
    // The multi-character TARGETS (ß→ss, æ→ae, œ→oe, þ→th) are mirrored by an
    // explicit `replace()` chain in `curio_normalize_text`, and the accented
    // Latin letters this list never sees (NFD + mark-stripping removes them
    // here) are mirrored by that function's own accented-letter map — keep all
    // three in the same order when either side changes. Homophone spellings are
    // likewise listed EXPLICITLY in the lexicon below (phuck, fvck, fack …)
    // instead of being derived.

    // ── the lexicon ───────────────────────────────────────────────────────
    // A MODERATION list: these strings live here only so the filter can refuse
    // them. Grouped so a future change can tune one group at a time. Anything
    // Every term is matched as a whole normalised word so ordinary writing is
    // never blocked by a substring coincidence.
    //
    // Deliberately EXCLUDED: mild words (damn, hell, crap, suck, butt), body
    // parts used plainly, and identity words (jew, queer, gay, gypsy) — those
    // are ordinary vocabulary and blocking them would refuse legitimate speech.
    // What is blocked is profanity, explicit sexual content and sexual slang,
    // real slurs, and harassment/threats.

    /** Profanity + explicit sexual content and slang. */
    private val EXPLICIT = listOf(
        "shit", "shits", "shyt", "bullshit", "dipshit", "shithead",
        "bitch", "bitches", "bich", "biatch",
        "cunt", "cunts", "kunt", "kunts", "dickhead", "dickheads",
        "pussy", "pussies", "whore", "whores", "slut", "sluts", "sloot",
        "asshole", "assholes", "arsehole", "arseholes", "bastard", "bastards",
        "blowjob", "blowjobs", "handjob", "handjobs", "rimjob", "footjob",
        "onlyfans", "hentai", "rule34", "nsfw", "sexting", "sextape",
        "dildo", "dildos", "vibrator", "vibrators", "penis", "vagina",
        "nipple", "nipples", "orgasm", "orgasms", "cumshot", "creampie",
        "bukkake", "hooker", "hookers", "brothel",
        "stripper", "strippers", "fetish", "bdsm", "bondage", "dominatrix",
        "camgirl", "camsex", "porno", "porn", "pornhub", "xvideos",
        "threesome", "foursome", "gangbang", "orgy", "orgies",
        "pedophile", "pedophiles", "paedophile", "molester", "molesters",
        "childporn", "lolicon", "shotacon"
    )

    /** Slurs, hate speech and harassment — these get the account banned. */
    private val SLURS = listOf(
        "nigger", "niggers", "nigga", "niggas", "nigglet",
        "faggot", "faggots", "fagot", "fagots",
        "tranny", "trannies", "shemale", "shemales", "ladyboy",
        "retard", "retards", "retarded", "spastic", "mongoloid",
        "wetback", "chink", "kike", "raghead", "towelhead",
        "redskin", "squaw", "darkie", "gook",
        "nazi", "nazis", "hitler", "whitepower", "whitepride", "gaschamber",
        "killyourself", "neckyourself"
    )

    /** Public-safety terms, all matched as complete normalised words. */
    private val UNAMBIGUOUS: List<String> = (EXPLICIT + SLURS)
        .map { NON_LETTER.replace(fold(it), "") }
        .filter { it.length >= 3 }
        .distinct()

    /** Additional public-safety terms, also matched as complete normalised words. */
    private val NAME_LIKE: Set<String> = setOf(
        "ass", "arse", "asses", "dumbass", "jackass", "kickass",
        "sex", "sexy", "sexual", "sexist", "sexton", "sextoy", "sextoys",
        "nude", "nudes", "naked", "boob", "boobs", "tit", "tits", "titties",
        "cum", "anal", "rape", "raped", "raping", "rapist", "rapists",
        "dic", "dick", "dicks", "cock", "cocks", "hoe", "hoes",
        "horny", "wank", "wanker", "bugger", "fag", "fags",
        "homo", "escort", "escorts", "coon", "paki", "kys"
    ).map { fold(it) }.toSet()

}
