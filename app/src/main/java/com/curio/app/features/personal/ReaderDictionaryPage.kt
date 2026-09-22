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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
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
    // ── v456 — THE WORD WHOSE SHEET IS OPEN ─────────────────────────────
    //
    // The answer is a panel now (see the note where the senses used to be drawn),
    // so the page has to remember WHICH word opened it. It is cleared the moment
    // the sheet shuts, and set the moment a lookup starts.
    var sheetWord by remember { mutableStateOf<String?>(null) }
    // ── AND A WORD CAN BE ASKED FOR TWICE ──────────────────────────────
    //
    // Closing the sheet clears [sheetWord], and `word` alone cannot say "again":
    // a tap on the word already sitting in the field changes no state at all, so
    // the lookup effect below would never re-run and the sheet would never come
    // back. This is the counter that makes a repeat a real request — bumped by
    // every deliberate ask (a word in the list, a suggestion, a word from the
    // history) and part of the effect's keys.
    var askSeq by remember { mutableIntStateOf(0) }
    // ── The doors, and the volumes that live on the phone ────────────────
    // v457 — A RETIRED VOLUME'S FILE IS CLEANED UP HERE, ONCE (see
    // [ReaderOfflineDictionary.purgeRetired]): removing a door must not leave
    // 9MB of unsearchable file behind on a phone that had downloaded it. It runs
    // off the main thread and nothing waits on it — the doors below are read from
    // the volumes that still exist.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { ReaderOfflineDictionary.purgeRetired(context) }
    }
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

    // ── v453 — THE VERSIONS OF THE DICTIONARY, AS A CHOICE ──────────────
    //
    // The member: *"only opening the word shows both of the version with badge to
    // switch"*. v452 merged the door's volumes into one best-first answer, which
    // is right for the SHEET (a passage was swept, the member wants the meaning
    // now) and wrong for a page someone is BROWSING: a reader who looked a word up
    // on purpose wants to see what each dictionary it could come from says, and to
    // be able to say "show me the other one". So the page keeps the volumes
    // separate: the badges under the rail are the DICTIONARIES THAT ARE HERE, and
    // the answer belongs to the one you are on.
    val shelf = remember(door, ready) { door.volumes.filter { ready[it] == true } }
    var version by remember(shelf) { mutableStateOf(shelf.firstOrNull()) }

    suspend fun ask(term: String): List<ReaderDictionarySense>? {
        if (door.volumes.isNotEmpty()) {
            val chosen = version ?: return null
            return ReaderOfflineDictionary.define(context, chosen, term)
        }
        val online = door.online ?: ReaderDictionarySource.WIKTIONARY
        return ReaderDictionary.define(term, online)
    }

    // ── v453 — AND THE PAGE BROWSES THE WORDS THEMSELVES ────────────────
    //
    // *"The dictionary from the home screen shows the full words it have, like a
    // physical dictionary and user can search words and it shows it"*. A page that
    // can only answer what you already spelled is a lookup box; a dictionary you
    // can WALK is what a shelf is for. One letter is one bucket (see
    // [ReaderOfflineDictionary.headwords]), so the words of the letter you are
    // standing on are read when you stand on it and never before.
    var searchOpen by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    var letter by remember { mutableStateOf('a') }
    var words by remember { mutableStateOf<List<String>>(emptyList()) }
    var readingLetter by remember { mutableStateOf(false) }
    // The volume the browse reads: the best one that is actually on the phone.
    val browseVolume = remember(door, ready) {
        door.volumes.firstOrNull { ready[it] == true }
    }
    LaunchedEffect(browseVolume, letter) {
        val volume = browseVolume ?: return@LaunchedEffect
        readingLetter = true
        words = withContext(Dispatchers.IO) {
            runCatching { ReaderOfflineDictionary.headwords(context, volume, letter) }
                .getOrDefault(emptyList())
        }
        readingLetter = false
    }

    // v453 — the VERSION is a key too: switching the badge asks the other
    // dictionary for the same word rather than showing the old answer beside a
    // badge claiming it.
    LaunchedEffect(word, door, version, askSeq) {
        val term = ReaderDictionary.headword(word)
        if (term.isBlank()) {
            lookup = DictionaryPageLookup.Idle
            guesses = emptyList()
            sheetWord = null
            return@LaunchedEffect
        }
        // v456 — THE WORD OPENS ITS OWN SHEET AS IT IS ASKED. It opens on the
        // looking-up line rather than after the answer, so the tap has an
        // immediate effect and the member watches the panel fill in.
        sheetWord = term
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
            // v453 — the words you have been to read in the order a dictionary is
            // filed in, not the order you happened to walk them (the member: "show
            // the dictionary words by alphabetical order").
            history = (listOf(term) + history.filterNot { it.equals(term, true) })
                .take(16)
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
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
                // ── v456 — AND THE PAGE'S OWN FOOT IS A REAL INSET ────────
                //
                // The member: *"the buttom area seems to have some glitch"*.
                // The navigation bar's height was taken by a zero-width SPACER
                // drawn over the bottom of the page, so the scroll ran underneath
                // an invisible strip: the last row of the word list slid behind
                // it, and a fling ended in a band of nothing with no edge to
                // explain it. The inset is a real one on the scroll's own column
                // now — the content stops above the bar instead of disappearing
                // under it — and the overlay is gone.
                .navigationBarsPadding()
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
                // ── v453 — THE SEARCH IS A PILL OF ITS OWN ──────────────
                //
                // The member: *"fix the dictionary page search box always open, show
                // it as a search pill to the right"*. The field used to sit across
                // the top of the page permanently — which for a page that now BROWSE
                // the words is a keyboard-height of furniture over a list nobody has
                // asked to filter yet. It is the reader's own object instead: a 50dp
                // circle at the right end of the head, exactly like the search door
                // beside the book's name in the reader, and the field opens when it
                // is tapped (see the field below).
                Surface(
                    onClick = { searchOpen = !searchOpen },
                    shape = CircleShape,
                    color = if (searchOpen) {
                        lerp(palette.surface, palette.accent, 0.28f)
                    } else {
                        palette.surface
                    },
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CurioIcon(
                            if (searchOpen) CurioIcons.Close else CurioIcons.Search,
                            if (searchOpen) "Close the search" else "Search the dictionary",
                            tint = palette.ink.copy(alpha = 0.85f),
                            size = 21.dp
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
                // ── THE FIELD, WHILE THE SEARCH IS OPEN ─────────────────
                //
                // It arrives on the pill clock from the edge its pill lives on (the
                // head), takes focus as it opens (a search that needs a second tap
                // before it can be typed into is not a search), and its own cross
                // empties the word as well as putting the field away — the row's
                // pill turns into that cross, so one object opens and closes it.
                LaunchedEffect(searchOpen) {
                    if (searchOpen) runCatching { searchFocus.requestFocus() }
                }
                AnimatedVisibility(
                    visible = searchOpen,
                    enter = CurioMotion.pillArrive(fromTop = true),
                    exit = CurioMotion.pillLeave(fromTop = true)
                ) {
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
                                                "Find a word",
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
                                    .focusRequester(searchFocus)
                            )
                        }
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
                    // ── v456 — AND THE PAGE DOES NOT OFFER TO REMOVE IT ──────
                    //
                    // The member: *"from dictionary dont show the provider
                    // removing in dictionary age"*. A page whose whole job is to
                    // SEARCH is the wrong place for a destructive control: the
                    // Remove button sat one mistaken tap away from the word list,
                    // beside a purely informational line, and freeing 11MB is not
                    // something a member reaches for while looking a word up. What
                    // the page keeps is the fact — which volume is answering, and
                    // what it is — and nothing else. Removing a volume still exists
                    // where managing one belongs: the reader's dictionary sheet.
                    Text(
                        wanted.source + " \u00b7 " + wanted.blurb,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = palette.ink.copy(alpha = 0.55f)
                        )
                    )
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

                // ── v456 — WHICH DICTIONARY ANSWERS LEFT THIS PAGE ──────
                //
                // The row of version badges stood here, straight under the door
                // badges, and it read as the same furniture twice (the member:
                // *"the badge to switch is kinda weird"*). Which dictionary a word
                // came from is a fact about the ANSWER, so it travels with the
                // answer: the version switch is one row inside the word's own
                // sheet now, labelled, where the senses it belongs to are (see
                // [DictionaryWordSheet]). Nothing was dropped — every volume on
                // the phone is still reachable, one tap away.

                // ── v456 — THE ANSWER IS A SHEET, NOT A BLOCK OF PAPER ───
                //
                // The member: *"fix the look and open the meanings in buttom
                // sheet"*. The senses used to be drawn inline, between the browse
                // rail and the words themselves — which pushed the dictionary down
                // the page every time a word was read, and left a definition and
                // the list it came from fighting for the same scroll. A word's
                // meanings are a THING THE MEMBER ASKED FOR, so they get a panel:
                // the reader's own sheet ([ReaderSheetFrame], the one every other
                // reader sheet uses — the same paper, the same drag-and-flick
                // close, the same keyboard inset), opened on the word and holding
                // its senses, its version switch and its spelling suggestions (see
                // [DictionaryWordSheet]).

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
                                    onClick = {
                                        word = seen
                                        askSeq++
                                    },
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
                // ── v453 — AND THE WORDS THEMSELVES, LIKE A SHELF ───────
                //
                // The member: *"the dictionary from the home screen shows the full
                // words it have, like a physical dictionary and user can search words
                // and it shows it"*. Below everything the page has answered stands the
                // dictionary itself: the letters along a rail, and the words of the
                // letter you are holding, read from that letter's own bucket (see
                // [ReaderOfflineDictionary.headwords]). A tap puts the word in the
                // field, which is what looks it up — so the list, the search and the
                // answer are one machine instead of three.
                if (browseVolume != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            "THE DICTIONARY \u00b7 " + browseVolume.source.uppercase(),
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
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ('a'..'z').forEach { l ->
                                val chosen = l == letter
                                Surface(
                                    onClick = { letter = l },
                                    shape = RoundedCornerShape(50),
                                    color = if (chosen) {
                                        palette.accent.copy(alpha = 0.18f)
                                    } else {
                                        palette.ink.copy(alpha = 0.06f)
                                    },
                                    contentColor = palette.ink
                                ) {
                                    Text(
                                        l.uppercase(),
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = palette.ink.copy(alpha = if (chosen) 1f else 0.7f),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        if (readingLetter) {
                            Text(
                                "Reading the \u201c" + letter.uppercase() + "\u201d pages\u2026",
                                style = TextStyle(fontSize = 13.sp, color = palette.ink.copy(alpha = 0.5f)),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else if (words.isEmpty()) {
                            Text(
                                "Nothing under \u201c" + letter.uppercase() + "\u201d.",
                                style = TextStyle(fontSize = 13.sp, color = palette.ink.copy(alpha = 0.6f)),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            // A bounded window of its own: the page already scrolls,
                            // so a list taller than this would fight it — the words
                            // get a real list inside the page's own scroll.
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                items(words) { head ->
                                    DictionaryHeadwordRow(
                                        head = head,
                                        palette = palette,
                                        chosen = head.equals(
                                            ReaderDictionary.headword(word),
                                            ignoreCase = true
                                        ),
                                        onClick = {
                                            word = head
                                            askSeq++
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        // v456 — the bottom overlay that used to stand here is gone (see the
        // column's own `navigationBarsPadding` above); the word's meanings are the
        // page's last child now, drawn over it as a sheet.
        val openWord = sheetWord
        if (openWord != null) {
            DictionaryWordSheet(
                word = openWord,
                state = lookup,
                guesses = guesses,
                shelf = shelf,
                version = version,
                palette = palette,
                onVersion = { chosen -> version = chosen },
                onGuess = { suggestion ->
                    word = suggestion
                    askSeq++
                },
                onDismiss = {
                    sheetWord = null
                    lookup = DictionaryPageLookup.Idle
                    guesses = emptyList()
                }
            )
        }
    }
}

/**
 * ONE WORD OF THE DICTIONARY, as a row you can walk down (v453).
 *
 * The row the member taps to look a word up: its headword in the reader's own type,
 * a quiet chevron saying the row opens something, and a wash when it is the word
 * currently answered — so a tap that lands on the word already open reads as one
 * state rather than as nothing happening.
 */
@Composable
private fun DictionaryHeadwordRow(
    head: String,
    palette: ReaderPalette,
    chosen: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (chosen) palette.accent.copy(alpha = 0.12f) else Color.Transparent,
        contentColor = palette.ink
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                head,
                style = TextStyle(
                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                    fontSize = 16.sp,
                    color = if (chosen) palette.accent else palette.ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            CurioIcon(
                CurioIcons.ChevronRight,
                null,
                tint = palette.ink.copy(alpha = 0.25f),
                size = 16.dp
            )
        }
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
        // v457 — TWO VOLUMES, best-first: the MODERN one carries the words the
        // 1913 editions predate, and the FULL 1913 is the complete article set.
        // The abridged 1913 conversion was the same text as the full edition in
        // a smaller conversion and is gone — see [ReaderOfflineDictionary].
        volumes = listOf(
            ReaderOfflineDictionary.Volume.MODERN,
            ReaderOfflineDictionary.Volume.FULL
        )
    ),
    WIKTIONARY("Wiktionary", online = ReaderDictionarySource.WIKTIONARY),
    FREE("Free", online = ReaderDictionarySource.FREE);

    /** The volume whose row this page shows first (the door's best one). */
    val volume: ReaderOfflineDictionary.Volume? get() = volumes.firstOrNull()
}

/**
 * v456 — A WORD'S MEANINGS, IN THE READER'S OWN SHEET.
 *
 * The member: *"fix the look and open the meanings in buttom sheet"*. The senses
 * used to be drawn INLINE on the dictionary page, between the browse rail and the
 * words — so reading one pushed the dictionary itself down the page, and the two
 * scrolls fought for the same drag.
 *
 * Measured against the app's own rules for a sheet, that was the wrong shape:
 *  · a word's meanings are a THING THE MEMBER ASKED FOR, so they belong in a panel
 *    they can dismiss rather than in the middle of a page they are browsing;
 *  · and the panel is the reader's own frame ([ReaderSheetFrame]), which is what
 *    makes it close the way every other reader sheet does — the flick, the
 *    measured travel, the motion system's clock — instead of being a second,
 *    differently-behaving sheet in the same app.
 *
 * WHAT IT HOLDS, and why each one is here rather than on the page:
 *  · **The version switch** — which dictionary is answering, and the one-tap way
 *    to the other. It belongs beside the senses it explains (the member's own
 *    complaint was that the row read as odd sitting under the door badges), and
 *    it is shown only when there is more than one volume to choose between: an
 *    online door has no versions, and a single volume has nothing to switch to.
 *  · **The senses** themselves, in the reader's own type.
 *  · **The spellings it thinks you meant** — a suggestion is only meaningful
 *    beside the miss that produced it, so it travels with the answer.
 */
@Composable
private fun DictionaryWordSheet(
    word: String,
    state: DictionaryPageLookup,
    guesses: List<String>,
    /** The offline volumes that are actually on the phone — the versions to switch between. */
    shelf: List<ReaderOfflineDictionary.Volume>,
    version: ReaderOfflineDictionary.Volume?,
    palette: ReaderPalette,
    onVersion: (ReaderOfflineDictionary.Volume) -> Unit,
    onGuess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ReaderSheetFrame(
        title = word,
        palette = palette,
        onDismiss = onDismiss,
        // The reader's own floor: a panel that wraps to two lines reads as a
        // strip rather than as an answer.
        minHeightFraction = 0.45f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── WHICH DICTIONARY IS ANSWERING ───────────────────────────
            //
            // Labelled, because an unlabelled row of two names under a word reads
            // as a category rather than as a choice — and shaped as the pills the
            // rest of the reader uses, so it is the same object as every other
            // badge in the app.
            if (shelf.size > 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        "SHOWING",
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        shelf.forEach { volume ->
                            val chosen = volume == version
                            Surface(
                                onClick = { onVersion(volume) },
                                shape = RoundedCornerShape(50),
                                color = if (chosen) {
                                    palette.accent.copy(alpha = 0.18f)
                                } else {
                                    palette.ink.copy(alpha = 0.06f)
                                },
                                contentColor = palette.ink
                            ) {
                                Text(
                                    volume.source,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = if (chosen) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    ),
                                    color = palette.ink.copy(alpha = if (chosen) 1f else 0.75f),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            when (state) {
                DictionaryPageLookup.Idle -> Unit
                is DictionaryPageLookup.Asking -> Text(
                    "Looking up \u201c" + state.term + "\u201d\u2026",
                    style = TextStyle(fontSize = 15.sp, color = palette.ink.copy(alpha = 0.5f))
                )
                is DictionaryPageLookup.Unreachable -> Text(
                    "The dictionary could not be reached. A volume you have downloaded " +
                        "answers without a connection \u2014 the badges on the page offer them.",
                    style = TextStyle(fontSize = 15.sp, color = palette.ink.copy(alpha = 0.75f))
                )
                is DictionaryPageLookup.Answer -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.senses.isEmpty()) {
                        Text(
                            "Nothing for \u201c" + state.term + "\u201d in this dictionary.",
                            style = TextStyle(
                                fontSize = 15.sp,
                                color = palette.ink.copy(alpha = 0.75f)
                            )
                        )
                    }
                    state.senses.forEach { sense ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                            .padding(top = 8.dp)
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

            // ── AND THE SPELLINGS IT THINKS YOU MEANT ───────────────────
            //
            // A tap asks the suggestion immediately (`askSeq`), so the same word
            // the list already holds still opens the sheet again.
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
                            onClick = { onGuess(guess) },
                            shape = RoundedCornerShape(50),
                            color = palette.ink.copy(alpha = 0.06f),
                            contentColor = palette.ink
                        ) {
                            Text(
                                guess,
                                style = TextStyle(fontSize = 13.sp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
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
