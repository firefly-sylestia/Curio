package com.curio.app.features.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v449 — THE DICTIONARY'S OWN PAGE.
 *
 * The member, on the sheet: *"when the dictionary is opened from the 3 dot one [it
 * should be] more longer and let user search any word"*, and then, as a follow-up of
 * its own: **a page of its own, opened from the reader's ⋯ menu and from Home's +,
 * standing taller and free to search any word**.
 *
 * The sheet is answer-shaped: it opens on the words the member swept and it lives
 * over the page they were reading. A page is SEARCH-shaped — nothing to do with the
 * book in front of them, no reason to stay short, and a keyboard that should never
 * cover the answer (which is why the whole page rides the IME's own inset). So this
 * is the same doors and the same lookup, told at a different size:
 *
 *  - **One field, always at the top**, and it is the point of the page (the member
 *    types a word the book never gave them).
 *  - **The doors are the badges**, exactly as in the sheet — since v452 they are
 *    **Offline · Wiktionary · Free**, and the one Offline door answers from every
 *    volume the phone has (with each missing volume still offered as its own
 *    download chip, and a volume that is here removable from its own row).
 *  - **The words you looked up stay in reach**: this page keeps its own short
 *    history for the visit, so walking back to the word before last is a tap rather
 *    than a retype. Nothing is written to disk for it.
 */
@Composable
internal fun ReaderDictionaryPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = readerPalette(ReaderLook.inkKey)
    var word by remember { mutableStateOf("") }
    var lookup by remember { mutableStateOf<DictionaryPageLookup>(DictionaryPageLookup.Idle) }
    var guesses by remember { mutableStateOf<List<String>>(emptyList()) }
    // Where the answer came from, so the words the member just fell in love with are
    // one tap away (see the file's note).
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    // ── The doors, and the volumes that live on the phone ────────────────
    var ready by remember {
        mutableStateOf(
            ReaderOfflineDictionary.Volume.entries.associateWith {
                ReaderOfflineDictionary.isReady(context, it)
            }
        )
    }
    var diagnosing by remember { mutableStateOf<ReaderOfflineDictionary.Volume?>(null) }
    var fetched by remember { mutableFloatStateOf(0f) }
    var door by remember {
        mutableStateOf(
            when {
                // v452 — offline first once ANY volume is on the phone (the door
                // searches all of them — see [DictionaryPageDoor]).
                ReaderOfflineDictionary.Volume.entries.any { ready[it] == true } ->
                    DictionaryPageDoor.OFFLINE
                ReaderLook.dictionary == ReaderDictionarySource.FREE -> DictionaryPageDoor.FREE
                else -> DictionaryPageDoor.WIKTIONARY
            }
        )
    }

    suspend fun ask(term: String): List<ReaderDictionarySense>? {
        // v452 — the merged offline answer (the same rule as the sheet's: ask every
        // volume the door has, best first, and keep "no dictionary" and "no such
        // word" apart).
        if (door.volumes.isNotEmpty()) {
            var anyReady = false
            for (volume in door.volumes) {
                val senses = ReaderOfflineDictionary.define(context, volume, term)
                    ?: continue
                anyReady = true
                if (senses.isNotEmpty()) return senses
            }
            return if (anyReady) emptyList() else null
        }
        val online = door.online ?: ReaderDictionarySource.WIKTIONARY
        return ReaderDictionary.define(term, online)
    }

    LaunchedEffect(word, door) {
        val term = ReaderDictionary.headword(word)
        if (term.isBlank()) {
            lookup = DictionaryPageLookup.Idle
            guesses = emptyList()
            return@LaunchedEffect
        }
        lookup = DictionaryPageLookup.Asking(term)
        // The typing pause: a definition is a call (or a scan of a file).
        delay(320)
        val found = withContext(Dispatchers.IO) { runCatching { ask(term) }.getOrNull() }
        if (found == null) {
            lookup = DictionaryPageLookup.Unreachable(term)
            guesses = emptyList()
            return@LaunchedEffect
        }
        if (found.isNotEmpty()) {
            lookup = DictionaryPageLookup.Answer(term, found)
            guesses = emptyList()
            history = (listOf(term) + history.filterNot { it.equals(term, true) }).take(12)
            return@LaunchedEffect
        }
        // A miss is usually a spelling: the nearest page names are offered, and the
        // first one that HAS an entry simply answers (see [ReaderDictionary.suggest]).
        val near = withContext(Dispatchers.IO) {
            runCatching { ReaderDictionary.suggest(term) }.getOrNull().orEmpty()
        }
        lookup = DictionaryPageLookup.Answer(term, emptyList())
        guesses = near
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // The page rides the keyboard, so the field and what it answered are
                // never under the keys.
                .imePadding()
        ) {
            // ── THE HEAD: the way back, and the page's own name ──────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = palette.surface,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.ArrowBack,
                            "Go back",
                            tint = palette.ink.copy(alpha = 0.85f),
                            size = 21.dp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.surface,
                    shadowElevation = 10.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "Dictionary",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.ink
                            )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── THE FIELD ─────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.ink.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CurioIcon(
                            CurioIcons.Search,
                            null,
                            tint = palette.ink.copy(alpha = 0.5f),
                            size = 18.dp
                        )
                        BasicTextField(
                            value = word,
                            onValueChange = { next -> word = next },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = readerTypeFamily(ReaderLook.typeFace),
                                fontSize = 17.sp,
                                color = palette.ink
                            ),
                            cursorBrush = SolidColor(palette.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            decorationBox = { inner ->
                                Box {
                                    if (word.isEmpty()) {
                                        Text(
                                            "Any word",
                                            style = TextStyle(fontSize = 17.sp),
                                            color = palette.ink.copy(alpha = 0.35f)
                                        )
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp)
                        )
                    }
                }

                // ── THE DOORS, AS THE SHEET'S BADGE ROW ──────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DictionaryPageDoor.entries.forEach { option ->
                        val chosen = option == door
                        // v452 — dim only when NONE of the door's volumes is here.
                        val live = if (option.volumes.isEmpty()) {
                            true
                        } else {
                            option.volumes.any { ready[it] == true }
                        }
                        Surface(
                            onClick = { door = option },
                            shape = RoundedCornerShape(50),
                            color = if (chosen) {
                                palette.accent.copy(alpha = 0.18f)
                            } else {
                                palette.ink.copy(alpha = 0.06f)
                            },
                            contentColor = palette.ink
                        ) {
                            Text(
                                option.label,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = palette.ink.copy(
                                    alpha = when {
                                        chosen -> 1f
                                        live -> 0.75f
                                        else -> 0.45f
                                    }
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // ── THE DOOR YOU ARE ON: ITS VOLUME, OR ITS DOWNLOAD ─────
                val wanted = door.volume
                if (wanted != null && ready[wanted] != true) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    wanted.source,
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.ink
                                    )
                                )
                                Text(
                                    if (diagnosing == wanted) {
                                        "Downloading\u2026 ${(fetched * 100f).toInt()}%"
                                    } else {
                                        wanted.blurb + " \u00b7 " + wanted.size
                                    },
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = palette.ink.copy(alpha = 0.55f)
                                    )
                                )
                            }
                            if (diagnosing == null) {
                                Surface(
                                    onClick = {
                                        diagnosing = wanted
                                        fetched = 0f
                                        scope.launch {
                                            val saved = ReaderOfflineDictionary.download(
                                                context,
                                                wanted
                                            ) { ratio -> fetched = ratio }
                                            diagnosing = null
                                            ready = ready + (wanted to saved)
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = palette.accent,
                                    contentColor = journalInkOn(palette.accent)
                                ) {
                                    Text(
                                        "Download",
                                        style = TextStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 8.dp
                                        )
                                    )
                                }
                            }
                        }
                        if (diagnosing == wanted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(palette.ink.copy(alpha = 0.10f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fetched.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.accent)
                                )
                            }
                        }
                    }
                } else if (wanted != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            wanted.source + " \u00b7 " + wanted.blurb,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = palette.ink.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                ReaderOfflineDictionary.remove(context, wanted)
                                val now = ready + (wanted to false)
                                ready = now
                                // v452 — the door stays put while it still has a volume.
                                door = if (door.volumes.any { now[it] == true }) {
                                    DictionaryPageDoor.OFFLINE
                                } else {
                                    DictionaryPageDoor.WIKTIONARY
                                }
                            },
                            shape = RoundedCornerShape(50),
                            color = palette.ink.copy(alpha = 0.06f),
                            contentColor = palette.ink
                        ) {
                            Text(
                                "Remove",
                                style = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // ── v452 — THE DOOR'S OTHER VOLUMES ─────────────────────
                //
                // One Offline badge means one row for its BEST volume; the rest have
                // to be reachable from the page too, or a member could only ever
                // download WordNet here. A missing one is a chip, the same object the
                // sheet offers (and a volume already on the phone is not offered —
                // there is nothing left to do to it from this row).
                val alsoMissing = door.volumes.filter { it != wanted && ready[it] != true }
                if (alsoMissing.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        alsoMissing.forEach { volume ->
                            Surface(
                                onClick = {
                                    diagnosing = volume
                                    fetched = 0f
                                    scope.launch {
                                        val saved = ReaderOfflineDictionary.download(
                                            context,
                                            volume
                                        ) { ratio -> fetched = ratio }
                                        diagnosing = null
                                        ready = ready + (volume to saved)
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                color = palette.ink.copy(alpha = 0.06f),
                                contentColor = palette.ink
                            ) {
                                Text(
                                    if (diagnosing == volume) {
                                        volume.source + " \u2026 " +
                                            (fetched * 100f).toInt() + "%"
                                    } else {
                                        "Download " + volume.source + " \u00b7 " + volume.size
                                    },
                                    style = TextStyle(fontSize = 12.sp),
                                    color = palette.ink.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // ── THE ANSWER ───────────────────────────────────────────
                when (val state = lookup) {
                    DictionaryPageLookup.Idle -> Unit
                    is DictionaryPageLookup.Asking -> Text(
                        "Looking up \u201c" + state.term + "\u201d\u2026",
                        style = TextStyle(fontSize = 14.sp, color = palette.ink.copy(alpha = 0.5f))
                    )
                    is DictionaryPageLookup.Unreachable -> Text(
                        "The dictionary could not be reached. A volume you have downloaded " +
                            "answers without a connection \u2014 see the badges above.",
                        style = TextStyle(fontSize = 14.sp, color = palette.ink.copy(alpha = 0.7f))
                    )
                    is DictionaryPageLookup.Answer -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            state.term,
                            style = TextStyle(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.ink
                            )
                        )
                        if (state.senses.isEmpty()) {
                            Text(
                                "Nothing for \u201c" + state.term + "\u201d in this dictionary.",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = palette.ink.copy(alpha = 0.7f)
                                )
                            )
                        }
                        state.senses.forEach { sense ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    sense.partOfSpeech.uppercase(),
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.2.sp,
                                        color = palette.accent
                                    )
                                )
                                sense.definitions.forEach { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 7.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(palette.accent.copy(alpha = 0.6f))
                                        )
                                        Text(
                                            line,
                                            style = TextStyle(
                                                fontFamily = readerTypeFamily(ReaderLook.typeFace),
                                                fontSize = 16.sp,
                                                color = palette.ink
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── AND THE SPELLINGS IT THINKS YOU MEANT ───────────────
                if (guesses.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        guesses.forEach { guess ->
                            Surface(
                                onClick = { word = guess },
                                shape = RoundedCornerShape(50),
                                color = palette.ink.copy(alpha = 0.06f),
                                contentColor = palette.ink
                            ) {
                                Text(
                                    guess,
                                    style = TextStyle(fontSize = 13.sp),
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 7.dp
                                    )
                                )
                            }
                        }
                    }
                }

                // ── AND THE WORDS YOU HAVE ALREADY BEEN TO ──────────────
                if (history.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "LOOKED UP",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.2.sp,
                                color = palette.ink.copy(alpha = 0.5f)
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            history.forEach { seen ->
                                Surface(
                                    onClick = { word = seen },
                                    shape = RoundedCornerShape(50),
                                    color = palette.ink.copy(alpha = 0.06f),
                                    contentColor = palette.ink
                                ) {
                                    Text(
                                        seen,
                                        style = TextStyle(fontSize = 13.sp),
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 7.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        // The bar under the page keeps the last row of the scroll clear of the
        // member's navigation bar.
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

/**
 * The doors, exactly as the sheet's badge row wears them (v444/v446) — and, since
 * v452, THE THREE the sheet wears: **Offline · Wiktionary · Free**, with the one
 * Offline badge answering from every volume the phone has (see the sheet's
 * `DictionaryDoor`, whose note is the long version of this).
 */
private enum class DictionaryPageDoor(
    val label: String,
    val volumes: List<ReaderOfflineDictionary.Volume> = emptyList(),
    val online: ReaderDictionarySource? = null
) {
    OFFLINE(
        "Offline",
        volumes = listOf(
            ReaderOfflineDictionary.Volume.MODERN,
            ReaderOfflineDictionary.Volume.FULL,
            ReaderOfflineDictionary.Volume.WEBSTER
        )
    ),
    WIKTIONARY("Wiktionary", online = ReaderDictionarySource.WIKTIONARY),
    FREE("Free", online = ReaderDictionarySource.FREE);

    /** The volume whose row this page shows first (the door's best one). */
    val volume: ReaderOfflineDictionary.Volume? get() = volumes.firstOrNull()
}

/** What this page is showing: its own states, for its own size. */
private sealed interface DictionaryPageLookup {
    data object Idle : DictionaryPageLookup
    data class Asking(val term: String) : DictionaryPageLookup
    data class Unreachable(val term: String) : DictionaryPageLookup
    data class Answer(
        val term: String,
        val senses: List<ReaderDictionarySense>
    ) : DictionaryPageLookup
}
