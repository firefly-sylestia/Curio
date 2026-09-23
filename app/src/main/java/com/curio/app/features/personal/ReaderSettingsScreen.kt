package com.curio.app.features.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import kotlin.math.roundToInt

/**
 * v434 — THE READING SETTINGS, AS A PAGE OF ITS OWN.
 *
 * The member asked for it in their own words — "settings gets its own screen" —
 * and asked for the one thing that makes it belong here rather than in the app's
 * settings family: *"dont use settings style use the reader style ui for it"*. So
 * it is a full screen of the READER's own paper, its own ink, its own type and its
 * own capsules: a page of the book's world that happens to be about the book.
 *
 * Reachable two ways, and it is the SAME composable both times:
 *
 *  · from the reader's ⋯ menu, where it opens OVER the book so back puts the
 *    member on the page they were reading (see the reader body), and
 *  · as a destination of its own for the settings side — wired on the Dev page.
 *
 * ── v434 — IT IS THE APPEARANCE SHEET'S COMPLETE TWIN NOW ───────────────
 *
 * The two shared one vocabulary but not one voice: the sheet used `Switch`es and
 * the page used full-width pill rows. Both now use the SAME components — the
 * animated [ReaderSegmentRow] for every either/or (or three-way) choice, the
 * [ReaderSliderRow] for every size, the capsule swatches for the paper — so a
 * setting learned in one place is the same shape in the other (member: "use a
 * similar design system to samsung … proper visual consistency also extend this
 * to settings").
 *
 * WHAT IS HERE, AND WHY: the sheet is the quick door (the few things a reader
 * changes while reading); this is the COMPLETE one — every reader preference in
 * one scroll, including the ones the sheet has no room for, the three-way
 * orientation, the page's own gestures, and the settings the member asked for by
 * name (line spacing, margins, paragraph spacing, alignment, keeping the screen
 * awake, and the night dim).
 */
@Composable
internal fun ReaderSettingsScreen(
    palette: ReaderPalette,
    /** Whether a type size and a face mean anything for what is open. */
    showType: Boolean,
    /**
     * v434 — whether a PDF is open, for the page control it needs instead.
     *
     * v439 — the ZOOM row is gone and the MOTION LOCK stands in its place: a
     * slider set the page's size, which the pinch already does better, and what a
     * member wants afterwards is for the page to stay where they left it (see
     * [ReaderLook.motionLock]).
     */
    showZoom: Boolean = false,
    /** Whether the reading is being read as PAGES right now. */
    paged: Boolean,
    onTogglePaged: () -> Unit,
    /** False when no book is open, so there are no zones to place. */
    canPlaceZones: Boolean = false,
    onGestures: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var moreInks by remember { mutableStateOf(false) }
    // v442 — WHICH END OF THE DIM'S WINDOW IS BEING SET (see [ReaderClockRow]).
    var clockPick by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
            // v438 — the READER's own top floor, not the (hidden) status bar: this
            // page is drawn inside the reader, which hides the bar, so
            // `statusBarsPadding()` collapsed to zero and the head sat on the
            // glass (member: "in settings the header is again over the status
            // bar"). See [readerChromeTopInset].
            .readerChromeTopInset()
    ) {
        // ── THE HEAD, IN THE READER'S OWN PILL ──────────────────────────
        Surface(
            shape = RoundedCornerShape(50),
            color = palette.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReaderChromeButton(CurioIcons.ArrowBack, "Back to the book", palette) { onBack() }
                Text(
                    "Reading",
                    style = TextStyle(
                        fontFamily = readerTypeFamily(ReaderLook.typeFace),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.ink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── THE PAPER ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Page", palette)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ReaderSkin.primary.forEach { skin ->
                        ReaderInkSwatch(skin, palette, Modifier.weight(1f))
                    }
                    ReaderMoreInkTile(palette, open = moreInks, modifier = Modifier.weight(0.8f)) {
                        moreInks = !moreInks
                    }
                }
                if (moreInks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        ReaderSkin.extra.forEach { skin ->
                            ReaderInkSwatch(skin, palette, Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── THE TYPE, or the PDF's zoom ─────────────────────────────
            if (showType) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Type", palette)
                    ReaderSliderRow(
                        label = "Text size",
                        value = ReaderLook.textScale,
                        range = 0.8f..2.6f,
                        step = 0.08f,
                        valueLabel = "${(ReaderLook.textScale * 100f).roundToInt()}%",
                        palette = palette,
                        onValue = { next -> ReaderLook.textScale = next.coerceIn(0.8f, 2.6f) },
                        leadingGlyph = CurioIcons.TextDecrease,
                        leadingLabel = "Smaller type",
                        trailingGlyph = CurioIcons.TextIncrease,
                        trailingLabel = "Larger type"
                    )
                    ReaderSegmentRow(
                        segments = ReaderTypeFace.entries.map { ReaderSegment(it.label) },
                        selectedIndex = ReaderTypeFace.entries.indexOf(
                            ReaderTypeFace.of(ReaderLook.typeFace)
                        ),
                        palette = palette,
                        onSelect = { at ->
                            ReaderTypeFace.entries.getOrNull(at)?.let { ReaderLook.typeFace = it.key }
                        }
                    )
                }
            }

            // ── v439 — THE ZOOM SLIDER IS GONE, THE LOCK STANDS IN ITS PLACE ──
            //
            // The member: *"in pdf only remove that zoom slider and add the motion
            // lock pill"*. A slider is the wrong control for a PDF page anyway:
            // the page is a picture, and the honest way to size it is the pinch
            // itself — which the member does, and then wants to KEEP. The lock
            // freezes that choice (pan and pinch both, see [ReaderLook.motionLock]),
            // and it is offered here as well as on the page's pill because
            // settings is where a member looks for a state they can't undo.
            if (showZoom) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Moving the page", palette)
                    ReaderSegmentRow(
                        segments = listOf(
                            ReaderSegment("Held still", CurioIcons.Lock),
                            ReaderSegment("Free", CurioIcons.DragHandle)
                        ),
                        selectedIndex = if (ReaderLook.motionLock) 0 else 1,
                        palette = palette,
                        onSelect = { at -> ReaderLook.motionLock = at == 0 }
                    )
                }
            }

            // ── HOW THE BOOK IS LAID OUT ────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Reading mode", palette)
                ReaderSegmentRow(
                    segments = ReaderFlow.entries.map { ReaderSegment(it.label, it.modeGlyph()) },
                    selectedIndex = if (paged) 1 else 0,
                    palette = palette,
                    onSelect = { if ((it == 1) != paged) onTogglePaged() }
                )

                ReaderSettingsSection("How the page stands", palette)
                ReaderSegmentRow(
                    segments = ReaderOrientation.entries.map {
                        ReaderSegment(it.label, it.orientationGlyph())
                    },
                    selectedIndex = ReaderOrientation.entries.indexOf(ReaderLook.orientation),
                    palette = palette,
                    onSelect = { at ->
                        ReaderOrientation.entries.getOrNull(at)?.let { ReaderLook.orientation = it }
                    }
                )
            }

            // ── THE WORDS' OWN LAYOUT ───────────────────────────────────
            if (showType) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Lines", palette)
                    ReaderSliderRow(
                        label = "Line spacing",
                        value = ReaderLook.lineSpacing,
                        range = 0.85f..1.6f,
                        step = 0.05f,
                        valueLabel = "${(ReaderLook.lineSpacing * 100f).roundToInt()}%",
                        palette = palette,
                        onValue = { next -> ReaderLook.lineSpacing = next.coerceIn(0.85f, 1.6f) },
                        leadingGlyph = CurioIcons.Remove,
                        leadingLabel = "Tighter lines",
                        trailingGlyph = CurioIcons.Add,
                        trailingLabel = "Looser lines"
                    )
                    ReaderSliderRow(
                        label = "Page margins",
                        value = ReaderLook.pageMargin,
                        range = 10f..40f,
                        step = 2f,
                        valueLabel = "${ReaderLook.pageMargin.roundToInt()} dp",
                        palette = palette,
                        onValue = { next -> ReaderLook.pageMargin = next.coerceIn(10f, 40f) },
                        leadingGlyph = CurioIcons.Remove,
                        leadingLabel = "Narrower margins",
                        trailingGlyph = CurioIcons.Add,
                        trailingLabel = "Wider margins"
                    )
                    ReaderSliderRow(
                        label = "Paragraph spacing",
                        value = ReaderLook.paraSpacing,
                        range = 0.6f..2f,
                        step = 0.1f,
                        valueLabel = "${(ReaderLook.paraSpacing * 100f).roundToInt()}%",
                        palette = palette,
                        onValue = { next -> ReaderLook.paraSpacing = next.coerceIn(0.6f, 2f) },
                        leadingGlyph = CurioIcons.Remove,
                        leadingLabel = "Closer paragraphs",
                        trailingGlyph = CurioIcons.Add,
                        trailingLabel = "Further paragraphs"
                    )
                    ReaderAlignRow(palette)
                }
            }

            // ── THE SCREEN ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Screen", palette)
                ReaderSegmentRow(
                    segments = listOf(
                        ReaderSegment("Awake", CurioIcons.Lightbulb),
                        ReaderSegment("Let it sleep", CurioIcons.Bedtime)
                    ),
                    selectedIndex = if (ReaderLook.keepScreenOn) 0 else 1,
                    palette = palette,
                    onSelect = { at -> ReaderLook.keepScreenOn = at == 0 }
                )
                // v440 — WHEN the dim comes on (member: *"Night dim on a schedule
                // (auto at sunset, not just manual)"*): always, or inside a window
                // of the member's own.
                //
                // ── v442 — AND THE WINDOW IS THEIRS TO SET ─────────────
                //
                // "At sunset" used to be the phone's own dark theme; it is the two
                // times below it now, which is what the member asked for (*"add at
                // sunset customisation to be able to set the tiem"*) — see
                // [ReaderLook.dimFromMinute] for why, and [ReaderClockRow] for the
                // rows themselves (the appearance sheet wears the same pair, so the
                // two surfaces can never disagree).
                ReaderSegmentRow(
                    segments = listOf(
                        ReaderSegment("Dim always", CurioIcons.DarkMode),
                        ReaderSegment("At sunset", CurioIcons.Nightlight)
                    ),
                    selectedIndex = if (ReaderLook.dimAuto) 1 else 0,
                    palette = palette,
                    onSelect = { at -> ReaderLook.dimAuto = at == 1 }
                )
                if (ReaderLook.dimAuto) {
                    ReaderClockRow(
                        label = "Dim from",
                        minuteOfDay = ReaderLook.dimFromMinute,
                        palette = palette
                    ) { clockPick = "from" }
                    ReaderClockRow(
                        label = "Dim until",
                        minuteOfDay = ReaderLook.dimUntilMinute,
                        palette = palette
                    ) { clockPick = "until" }
                    if (clockPick.isNotBlank()) {
                        val settingFrom = clockPick == "from"
                        ReaderClockDialog(
                            palette = palette,
                            minuteOfDay = if (settingFrom) {
                                ReaderLook.dimFromMinute
                            } else {
                                ReaderLook.dimUntilMinute
                            },
                            onDismiss = { clockPick = "" },
                            onPick = { minute ->
                                if (settingFrom) ReaderLook.dimFromMinute = minute
                                else ReaderLook.dimUntilMinute = minute
                            }
                        )
                    }
                }
                ReaderSliderRow(
                    label = "Night dim",
                    value = ReaderLook.dim,
                    range = 0f..0.6f,
                    step = 0.05f,
                    valueLabel = if (ReaderLook.dim <= 0f) "Off"
                    else "${(ReaderLook.dim / 0.6f * 100f).roundToInt()}%",
                    palette = palette,
                    onValue = { next -> ReaderLook.dim = next.coerceIn(0f, 0.6f) },
                    leadingGlyph = CurioIcons.DarkMode,
                    leadingLabel = "Less dim",
                    trailingGlyph = CurioIcons.Nightlight,
                    trailingLabel = "More dim"
                )
            }

            // ── v440 — THE VOICE: HOW FAST, AND WHOSE (see [ReaderSpeaker]) ──
            //
            // The member's own pick from the settings list: *"Read-aloud: a speed and
            // voice picker"*, and their answer for what it reads: *"The visible page,
            // then follow on"* (the reader's own driver). Both are remembered with the
            // rest of the look, so a member who needs a slower voice on a book has it
            // on the next one.
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Read aloud", palette)
                ReaderSliderRow(
                    label = "Speed",
                    value = ReaderLook.speakSpeed,
                    range = SPEAK_SLOW..SPEAK_FAST,
                    step = 0.1f,
                    valueLabel = "${(ReaderLook.speakSpeed * 10f).roundToInt() / 10f}\u00D7",
                    palette = palette,
                    onValue = { next ->
                        ReaderLook.speakSpeed = next.coerceIn(SPEAK_SLOW, SPEAK_FAST)
                    },
                    leadingGlyph = CurioIcons.Remove,
                    leadingLabel = "Slower",
                    trailingGlyph = CurioIcons.Add,
                    trailingLabel = "Faster"
                )
                // ── v464 — AND WHICH ENGINE READS AT ALL ───────────────
                //
                // The member: *"wire the read-aloud voice engine to any installed system
                // TTS engine, so I can point Curio at a better voice I install myself"*.
                // The platform's own engine is usually the plainest voice a phone has, and
                // Android lets ANY app supply speech — so the reader asks the platform
                // which engines answer `android.intent.action.TTS_SERVICE` and lets the
                // member point it at one (see [ReaderSpeaker.engines]). Nothing is
                // downloaded into Curio and no runtime is bundled: a better voice is a
                // better ENGINE, not a bigger APK.
                //
                // The list is asked for ONCE per visit (it is a PackageManager query, and
                // one on every recomposition is not a thing a settings page gets to do),
                // and the row names the engine rather than showing a raw package.
                val context = LocalContext.current
                var engines by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
                var enginePicker by remember { mutableStateOf(false) }
                var picker by remember { mutableStateOf(false) }
                var voices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
                // Re-asked whenever a picker has been open, so an engine installed WHILE
                // this page was open appears without the member having to leave it.
                LaunchedEffect(enginePicker, picker) {
                    engines = ReaderSpeaker.engines(context)
                }
                val engineLabel = engines.firstOrNull { it.first == ReaderLook.speakEngine }?.second
                    ?: if (ReaderLook.speakEngine.isBlank()) "The phone's own"
                    else ReaderLook.speakEngine
                Surface(
                    onClick = { enginePicker = true },
                    shape = RoundedCornerShape(50),
                    color = palette.ink.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CurioIcon(CurioIcons.Tune, null, tint = palette.accent, size = 17.dp)
                        Text(
                            "Engine",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = palette.ink.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            engineLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (enginePicker) {
                    AlertDialog(
                        onDismissRequest = { enginePicker = false },
                        containerColor = palette.paper,
                        title = {
                            Text(
                                "Which engine reads",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = readerTypeFamily(ReaderLook.typeFace)
                                ),
                                color = palette.ink
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                engines.forEach { (name, label) ->
                                    VoiceChoice(
                                        label = label,
                                        live = ReaderLook.speakEngine == name,
                                        palette = palette
                                    ) {
                                        if (ReaderLook.speakEngine != name) {
                                            ReaderLook.speakEngine = name
                                            // A VOICE NAME BELONGS TO ITS ENGINE, so the
                                            // old one goes with it: leaving it would point
                                            // the new engine at a voice it has never heard
                                            // of — harmless, but a lie in the row above.
                                            ReaderLook.speakVoice = ""
                                            voices = emptyList()
                                        }
                                        enginePicker = false
                                    }
                                }
                                Text(
                                    "A better voice means a better engine — a neural " +
                                        "text-to-speech engine you install shows up here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.ink.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { enginePicker = false }) {
                                Text("Done", color = palette.accent)
                            }
                        }
                    )
                }
                // ── AND WHICH VOICE ────────────────────────────────────
                //
                // The chosen ENGINE's own list, read the first time this door is opened
                // ([ReaderSpeaker.prepare] + `voices()`) — so nothing is downloaded and
                // the row says what the member has. The stored value is the engine's own
                // voice NAME, and blank means "whatever the phone reads with" (see
                // [ReaderLook.speakVoice]).
                Surface(
                    onClick = {
                        // v464 — the list is filled from the ENGINE'S OWN callback now, so
                        // the first open of this row no longer says "no voices are
                        // installed" while the engine is still binding: binding a speech
                        // engine is asynchronous, and the old call read the voice list on
                        // the line after asking for the engine — which is always too
                        // early (see [ReaderSpeaker.prepare]).
                        ReaderSpeaker.prepare(
                            context,
                            ReaderLook.speakEngine,
                            onReady = { voices = ReaderSpeaker.voices() }
                        )
                        voices = ReaderSpeaker.voices()
                        picker = true
                    },
                    shape = RoundedCornerShape(50),
                    color = palette.ink.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CurioIcon(CurioIcons.PlayArrow, null, tint = palette.accent, size = 17.dp)
                        Text(
                            "Voice",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = palette.ink.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            // The label when the picker has been opened in this visit,
                            // and otherwise the voice's own name — never "the phone's
                            // own" over a voice the member has CHOSEN: a row that
                            // describes the wrong state is worse than a terse one.
                            voices.firstOrNull { it.first == ReaderLook.speakVoice }?.second
                                ?: ReaderLook.speakVoice.substringAfterLast('#', "").ifBlank {
                                    if (ReaderLook.speakVoice.isBlank()) "The phone's own"
                                    else ReaderLook.speakVoice
                                },
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (picker) {
                    AlertDialog(
                        onDismissRequest = { picker = false },
                        containerColor = palette.paper,
                        title = {
                            Text(
                                "Which voice reads",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = readerTypeFamily(ReaderLook.typeFace)
                                ),
                                color = palette.ink
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                VoiceChoice(
                                    label = "The phone's own",
                                    live = ReaderLook.speakVoice.isBlank(),
                                    palette = palette
                                ) { ReaderLook.speakVoice = "" }
                                voices.forEach { (name, label) ->
                                    VoiceChoice(
                                        label = label,
                                        live = ReaderLook.speakVoice == name,
                                        palette = palette
                                    ) { ReaderLook.speakVoice = name }
                                }
                                if (voices.isEmpty()) {
                                    Text(
                                        "No voices are installed on this phone yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.ink.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { picker = false }) {
                                Text("Done", color = palette.accent)
                            }
                        }
                    )
                }
            }

            // ── v440 — WHERE A LOOKUP GOES ──────────────────────────────
            //
            // The member, asked what "bundled vs online" should mean once they heard
            // there is no bundled dictionary in the app: *"two online sources to
            // choose between"*. Both are keyless, so both work in every build — and
            // the choice is remembered with the rest of the reader's look (see
            // [ReaderDictionarySource]).
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Dictionary", palette)
                ReaderSegmentRow(
                    segments = ReaderDictionarySource.entries.map {
                        ReaderSegment(it.label, CurioIcons.MenuBook)
                    },
                    selectedIndex = ReaderDictionarySource.entries.indexOf(ReaderLook.dictionary),
                    palette = palette,
                    onSelect = { at ->
                        ReaderDictionarySource.entries.getOrNull(at)?.let { source ->
                            ReaderLook.dictionary = source
                        }
                    }
                )
            }

            // ── v439 — WHAT THE READING COSTS ───────────────────────────
            //
            // The member: *"in pdf reader, a high charge save turns on which makes
            // the app cache and background usage very low in reder so the phone
            // doesnt heat"*.
            //
            // It is a two-option row rather than a switch on purpose [see
            // ReaderLook.lowPower]: the choice is not "do a thing or not" but
            // WHICH WAY the reader spends — a cooler page and a shorter cache, or
            // the sharpest page the screen can take with its neighbour already
            // rendered. A switch would have made the second one the odd state.
            //
            // Both segments are glyphs this screen already draws
            // ([CurioIcons.Lightbulb] above), so nothing is promised here that the
            // bundled icon subset might not carry.
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ReaderSettingsSection("Power", palette)
                ReaderSegmentRow(
                    segments = listOf(
                        ReaderSegment("Cooler", CurioIcons.Lightbulb),
                        ReaderSegment("Sharpest", CurioIcons.AutoAwesome)
                    ),
                    selectedIndex = if (ReaderLook.lowPower) 0 else 1,
                    palette = palette,
                    onSelect = { at -> ReaderLook.lowPower = at == 0 }
                )
            }

            // ── THE PAGE'S OWN GESTURES ─────────────────────────────────
            if (canPlaceZones) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReaderSettingsSection("Gestures", palette)
                    ReaderSegmentRow(
                        segments = listOf(
                            ReaderSegment("On", CurioIcons.Check),
                            ReaderSegment("Off", CurioIcons.Close)
                        ),
                        selectedIndex = if (ReaderLook.tapZones) 0 else 1,
                        palette = palette,
                        onSelect = { at -> ReaderLook.tapZones = at == 0 }
                    )
                    // WHERE they sit and WHAT they do, on the page itself: a
                    // gesture is something the member has to see against the words
                    // it governs (see [ReaderTapZoneEditor]).
                    Surface(
                        onClick = { onGestures?.invoke() },
                        shape = RoundedCornerShape(50),
                        color = palette.ink.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(palette.accent, CircleShape)
                            )
                            Text(
                                "Place the gestures",
                                style = TextStyle(
                                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.ink
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

/** v440 — how slow and how fast the voice may read (see [ReaderSpeaker]). */
private const val SPEAK_SLOW = 0.6f
private const val SPEAK_FAST = 2f

/**
 * v440 — ONE VOICE IN THE PICKER, in the reader's own capsule language.
 *
 * A row rather than a chip: voice names are long ("English (United States) ·
 * female_1") and a chip that ellipsises them tells the member nothing — which is
 * the one thing a picker has to do.
 */
@Composable
private fun VoiceChoice(
    label: String,
    live: Boolean,
    palette: ReaderPalette,
    onPick: () -> Unit
) {
    Surface(
        onClick = onPick,
        shape = RoundedCornerShape(12.dp),
        color = if (live) palette.accent.copy(alpha = 0.14f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (live) {
                CurioIcon(CurioIcons.Check, null, tint = palette.accent, size = 16.dp)
            }
            Text(
                label,
                style = TextStyle(
                    fontFamily = readerTypeFamily(ReaderLook.typeFace),
                    fontSize = 14.sp,
                    color = palette.ink.copy(alpha = if (live) 0.95f else 0.75f)
                )
            )
        }
    }
}

/**
 * THE SAME PAGE WITHOUT A BOOK OPEN — the settings side's door.
 *
 * The member asked for the reader's settings to be reachable from the settings
 * side too, and for now that door is on the Dev page (their own instruction:
 * "wire it in dev exp for now"). Nothing here needs a book: the preferences it
 * changes belong to the PROCESS, not to one novel (see [ReaderLook]).
 */
@Composable
internal fun ReaderSettingsRoute(onBack: () -> Unit) {
    val palette = readerPalette(ReaderLook.inkKey)
    ReaderSettingsScreen(
        palette = palette,
        showType = true,
        paged = ReaderLook.textFlow == ReaderFlow.PAGED,
        onTogglePaged = { ReaderLook.textFlow = ReaderLook.textFlow.flipped() },
        onBack = onBack
    )
}

/** A section's name on the settings page — the reader's own furniture. */
@Composable
private fun ReaderSettingsSection(label: String, palette: ReaderPalette) {
    Text(
        label.uppercase(),
        style = TextStyle(
            fontFamily = readerTypeFamily(ReaderLook.typeFace),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.3.sp,
            color = palette.accent
        )
    )
}
