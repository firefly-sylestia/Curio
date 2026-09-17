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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.PAGE_KIND_TODO
import com.curio.app.data.PersonalDoc
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily

/**
 * v389 — A TO-DO LIST AS ITS OWN PAGE.
 *
 * This used to be a journal day with `kind = "todo"`: same date bar, same
 * heading, and a checklist hidden behind one toolbar button. A list of things to
 * do is not a day either — it has a NAME, a running count of what is left, and
 * rows you tick. So it is its own page:
 *
 *  · every new line arrives as a checkbox ROW — `checklistFirst` arms the
 *    empty line it opens with, and Enter at the end of a row makes the next row
 *    (see `PersonalEditorState.keepsChecklistRows`), while Enter on an empty
 *    row ends the list,
 *  · the ticks are STORED with their rows, so reopening the page shows what was
 *    finished (they used to live in the row's widget state and reset on reload),
 *  · the head says how far along the list is ("3 of 7 done").
 *
 * The writing core (autosave, the dock, the flush on app switch) is
 * [PersonalWritingPage]; this screen is the list's own head and how it reads.
 */
@Composable
fun TodoScreen(
    navController: NavController,
    entryIdArg: String,
    photos: PersonalPhotoOverlayState = rememberPersonalPhotoOverlayState()
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = personalAccent()
    var title by remember { mutableStateOf("") }
    // A list is not a DAY, but it still keeps the day it was started: the
    // journals list groups pages by date, and a page with no date would land in
    // January 1970. It is set once and never moved — the day the list was made.
    var dateMillis by remember { mutableLongStateOf(startOfToday()) }
    // The document, mirrored so the page can count its own rows without reaching
    // into the editor's state.
    var doc by remember { mutableStateOf(PersonalDoc(emptyList())) }
    val (done, total) = doc.checklistProgress()

    PersonalWritingPage(
        entryIdArg = entryIdArg,
        photos = photos,
        // A checklist page: opens with the pen down and the first empty line
        // armed as a row.
        checklistFirst = true,
        // The list's own tools: no quote, no heading, no photo — a to-do page is
        // rows, emphasis and nesting.
        showJournalTools = false,
        onDoc = { updated -> doc = updated },
        meta = {
            PersonalPageMeta(
                title = title,
                mood = "",
                dateMillis = dateMillis,
                kind = PAGE_KIND_TODO
            )
        },
        onLoaded = { existing ->
            title = existing.title
            if (existing.dateMillis > 0L) dateMillis = existing.dateMillis
        },
        // The page's own way out (the core guards it: a live voice recording is
        // asked about before a back gesture can drop it — see PersonalVoice).
        onExit = { navController.popBackStack() },
        voiceRoute = { CurioRoutes.todo(it) },
        header = { editing, saving, onToggleMode, onBack ->
            TodoTopBar(
                title = title,
                done = done,
                total = total,
                saving = saving,
                editing = editing,
                onToggleMode = onToggleMode,
                onBack = onBack
            )
        },
        readView = { readDoc ->
            TodoReadView(
                title = title,
                done = done,
                total = total,
                doc = readDoc,
                ink = ink,
                accent = accent,
                onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
            )
        },
        // The list's NAME: a to-do is remembered by what it is for, and the
        // journal list shows it. The caret does NOT start here — a checklist
        // opens ready to type a ROW (the core arms the first empty line), which
        // is what the member came to do.
        aboveCanvas = {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FrauncesFontFamily,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink
                ),
                cursorBrush = SolidColor(accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "Name this list",
                                style = TextStyle(
                                    fontFamily = FrauncesFontFamily,
                                    fontSize = 24.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ink.copy(alpha = 0.32f)
                                )
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(14.dp))
        }
    )
}

/**
 * The list's own bar: back, the name, how much is left, the read/write switch
 * and the save dot. The switch is here for the same reason it is on a journal:
 * reading a long list without the tool dock riding the keyboard is worth a tap,
 * and it is the pen — not the eye — that switches the ROWS back on.
 */
@Composable
private fun TodoTopBar(
    title: String,
    done: Int,
    total: Int,
    saving: Boolean,
    editing: Boolean,
    onToggleMode: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = personalAccent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(CurioIcons.ArrowBack, "Back", tint = ink, size = 19.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title.ifBlank { "To-do" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (title.isBlank()) ink.copy(alpha = 0.6f) else ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                checklistLabel(done, total),
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.55f)
            )
        }
        PersonalModeSwitch(editing = editing, onToggleMode = onToggleMode)
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(
                    color = if (saving) accent else ink.copy(alpha = 0.20f),
                    shape = CircleShape
                )
        )
    }
}

/** "3 of 7 done" — the list's own line, and the bar under it. */
internal fun checklistLabel(done: Int, total: Int): String = when {
    total == 0 -> "No rows yet"
    done == 0 -> "$total to do"
    done >= total -> "All $total done"
    else -> "$done of $total done"
}

@Composable
private fun TodoReadView(
    title: String,
    done: Int,
    total: Int,
    doc: PersonalDoc,
    ink: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    onOpenPhoto: (String, androidx.compose.ui.geometry.Rect?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .widthIn(max = 680.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            title.ifBlank { "To-do" },
            style = TextStyle(
                fontFamily = FrauncesFontFamily,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink
            )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            checklistLabel(done, total),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = personalAccentInk()
        )
        Spacer(Modifier.height(18.dp))
        PersonalDocView(doc = doc, ink = ink, accent = accent, onOpenPhoto = onOpenPhoto)
        Spacer(Modifier.height(120.dp))
    }
}
