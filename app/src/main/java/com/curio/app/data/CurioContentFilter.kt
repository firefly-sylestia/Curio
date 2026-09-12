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
 *  2. **Two views** of the folded text: the **squash** view (all separators
 *     gone, so `f u c k` and `f.u.c.k` collapse onto each other) and the
 *     **collapse** view (runs of the same letter squeezed to one, so `fuuuuck`
 *     and `shiiit` collapse too).
 *  3. **Two match modes.** Unambiguous words are matched anywhere in either
 *     view. Ambiguous ones — words that hide inside innocent ones (`ass` in
 *     "class", `sex` in "Essex", `hoe` in "shoes", `cock` in "cocktail") — are
 *     matched only as whole words, and consecutive single-letter words (`a s s`,
 *     `f u c k`) are merged into one word first, so the spaced trick fails too.
 *
 * Deliberately NOT clever about context: an over-eager filter is recoverable
 * (the person rewords), a leak is not. The one known cost is the classic
 * boundary case — a long innocent word that literally contains a severe one
 * (the "Scunthorpe problem") is refused; that trade-off is taken on purpose.
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
        if (text.isNullOrBlank() || isClean(text)) null else BLOCKED_MESSAGE

    /** Null when EVERY field is fine — the shape an API call uses. */
    fun problemIn(vararg texts: String?): String? =
        texts.firstNotNullOfOrNull { problem(it) }

    /** True when [text] carries nothing from the lexicon. */
    fun isClean(text: String): Boolean = !carriesBadWord(text)

    /** True when [text] carries something from the lexicon. */
    fun carriesBadWord(text: String): Boolean {
        val folded = fold(text)
        val squash = NON_LETTER.replace(folded, "")
        if (squash.length < 3) return false
        val collapse = COLLAPSE.replace(squash, "$1")

        // 1. Unambiguous words — matched ANYWHERE in either view, which is what
        //    defeats every separator and character-substitution trick.
        for (word in UNAMBIGUOUS) {
            if (squash.contains(word) || collapse.contains(word)) return true
        }
        // 2. Phrases ("kill yourself"), matched on the squash view so spacing
        //    inside the phrase can't break it.
        for (phrase in PHRASES) {
            if (squash.contains(phrase)) return true
        }
        // 3. Name-like words — whole words only, so "class", "Essex", "shoes",
        //    "cocktail", "analysis" and "title" all stay usable.
        val words = mergedWords(folded)
        for (word in NAME_LIKE) {
            if (word in words) return true
        }
        return false
    }

    // ── the fold ──────────────────────────────────────────────────────────

    private val MARKS = Regex("\\p{M}+")
    private val NON_LETTER = Regex("[^a-z]")
    private val COLLAPSE = Regex("(.)\\1+")

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
    // ≥ 3 letters that is NOT a substring of an ordinary word goes in
    // UNAMBIGUOUS; anything that hides inside a harmless word goes in NAME_LIKE.
    //
    // Deliberately EXCLUDED: mild words (damn, hell, crap, suck, butt), body
    // parts used plainly, and identity words (jew, queer, gay, gypsy) — those
    // are ordinary vocabulary and blocking them would refuse legitimate speech.
    // What is blocked is profanity, explicit sexual content and sexual slang,
    // real slurs, and harassment/threats.

    /** Profanity + explicit sexual content and slang. */
    private val EXPLICIT = listOf(
        "fuck", "fucker", "fuckers", "fucking", "fuk", "fuking", "fukk", "fck",
        "fuxk", "fux", "fuq", "fook", "phuck", "phuk", "fvck", "fvk", "fack",
        "fucked", "motherfucker", "motherfucking",
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

    /** Matched ANYWHERE in the squash/collapse views. */
    private val UNAMBIGUOUS: List<String> = (EXPLICIT + SLURS)
        .map { NON_LETTER.replace(fold(it), "") }
        .filter { it.length >= 3 }
        .distinct()

    /**
     * Words that hide inside ordinary ones, so they are matched as WHOLE words
     * only: `ass` in "class"/"pass", `sex` in "Essex"/"Sussex", `hoe` in
     * "shoes", `cock` in "cocktail", `cum` in "cucumber", `tit` in "title",
     * `rape` in "grape", `dic` in "dictionary", `nude` in "nudged".
     */
    private val NAME_LIKE: Set<String> = setOf(
        "ass", "arse", "asses", "dumbass", "jackass", "kickass",
        "sex", "sexy", "sexual", "sexist", "sexton", "sextoy", "sextoys",
        "nude", "nudes", "naked", "boob", "boobs", "tit", "tits", "titties",
        "cum", "anal", "rape", "raped", "raping", "rapist", "rapists",
        "dic", "dick", "dicks", "cock", "cocks", "hoe", "hoes",
        "horny", "wank", "wanker", "bugger", "fag", "fags",
        "homo", "escort", "escorts", "coon", "paki", "kys"
    ).map { fold(it) }.toSet()

    /** Whole phrases, matched on the squash view so spacing can't break them. */
    private val PHRASES: List<String> = listOf(
        "killyourself", "killyourselfplease", "neckyourself",
        "rapeyou", "rapeher", "rapehim", "rapekids",
        "sendnudes", "sendnude", "childporn", "cpforsale"
    ).map { NON_LETTER.replace(fold(it), "") }
}
