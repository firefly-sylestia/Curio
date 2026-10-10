package com.curio.app.features.personal

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.PersonalDoc
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioSheetContainerColor

/**
 * v500 — THE PAGE'S CONTENTS, AS A BRANCHING LIST.
 *
 * The member: *\"add a 3 dot menu style to browse though contents of the journal
 * in eye view, with branching view properly\"*, and when asked where it should
 * live: **both views**. Asked how headings should work they answered **\"heading
 * sizes and numbering\"** — so this is the numbering, and it is the same two
 * facts the page itself draws: **Heading I** is the entry's own line and takes a
 * roman numeral in order (I, II, III …), **Heading II** is a section under it and
 * takes its parent's numeral with its own index (I.1, I.2 …), and **content**
 * appears here not at all — a contents list of every line would be the page
 * again.
 *
 * The nesting is the branch: a level-II row is indented under the level-I row it
 * belongs to, which is exactly how it is written on the page.
 *
 * ⚠️ EVERY ROW CARRIES ITS BLOCK'S OWN ID. Two headings can read the same
 * ("Notes" twice on one page is ordinary writing), so tapping by words would jump
 * to the first one twice; the id is the thing that cannot be duplicated.
 */
internal data class PersonalHeading(
    /** The block the heading is written in — what a tap jumps to. */
    val id: String,
    /** The heading's own words, as they are on the page. */
    val label: String,
    /** 1 for Heading I, 2 for Heading II. */
    val level: Int,
    /** Its number: "I", "II", "I.1" … */
    val number: String
)

/**
 * Every heading on the page, in page order, numbered.
 *
 * Read from the STORED runs rather than from an editor mask: the read view, the
 * writing view and an export all ask this same function, so the contents list
 * cannot disagree with the page it describes.
 */
internal fun personalHeadingsOf(doc: PersonalDoc): List<PersonalHeading> {
    val out = ArrayList<PersonalHeading>()
    var top = 0          // level-I headings seen so far
    var sub = 0          // level-II headings under the current level-I
    doc.blocks.forEach { block ->
        if (block.isPhoto || block.isAudio || block.text.isBlank()) return@forEach
        val mask = runsToMask(block.text.length, block.runs)
        val level = personalHeadingLevel(block.text, mask)
        if (level == 0) return@forEach
        val label = personalHeadingLabel(block.text, mask)
        if (label.isBlank()) return@forEach
        if (level == 1) {
            top++
            sub = 0
            out.add(PersonalHeading(block.id, label, 1, roman(top)))
        } else {
            sub++
            // A section written before any level-I heading has no parent to
            // number under, so it is numbered on its own — never "0.1".
            val number = if (top == 0) roman(sub) else "${roman(top)}.$sub"
            out.add(PersonalHeading(block.id, label, 2, number))
        }
    }
    return out
}

/** 1 → I, 2 → II, … — the numbering the member chose for headings. */
private fun roman(n: Int): String {
    if (n <= 0) return "I"
    val table = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )
    var left = n
    val out = StringBuilder()
    table.forEach { (value, glyph) ->
        while (left >= value) {
            out.append(glyph)
            left -= value
        }
    }
    return out.toString()
}

/**
 * THE DOOR: a three-dot capsule in the page's own corner.
 *
 * It wears the page-level capsule the mode switch wears ([JournalCapsule]) so the
 * page's controls stay one family, and an OPAQUE fill (root rule 11) so its lift
 * reads clean against the paper.
 */
@Composable
internal fun PersonalContentsPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = JournalCapsule.Shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.size(JournalCapsule.Height)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CurioIcon(
                CurioIcons.MoreVert,
                "Contents",
                tint = personalAccentInk(),
                size = 20.dp
            )
        }
    }
}

/**
 * THE LIST ITSELF — a sheet, because it is a place you look at and leave.
 *
 * A row says its NUMBER first (that is what makes it a contents list rather than
 * a second copy of the page), then the heading's own words. Tapping one is a
 * jump, so the sheet closes on the same tap: a member who has picked a heading
 * wants the page, not the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalContentsSheet(
    headings: List<PersonalHeading>,
    onGo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioSheetContainerColor(scheme.surface),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        CurioGlassWindowBlur()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "CONTENTS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                color = personalAccentInk()
            )
            if (headings.isEmpty()) {
                // The honest empty state, and it teaches the tool in one line:
                // a page with no heading has nothing to list, and the way to give
                // it one is one dock button away.
                Text(
                    "No headings on this page yet. Select a line and press I or II in the writing tools to give the page its parts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    headings.forEach { heading ->
                        Surface(
                            onClick = {
                                onGo(heading.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // The BRANCH: a section sits in under its own
                                    // heading, one step of indentation deep.
                                    .padding(
                                        start = if (heading.level == 1) 14.dp else 34.dp,
                                        end = 14.dp,
                                        top = 12.dp,
                                        bottom = 12.dp
                                    )
                            ) {
                                Text(
                                    heading.number,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = personalAccentInk()
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    heading.label,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (heading.level == 1) {
                                            FontWeight.Bold
                                        } else FontWeight.Medium
                                    ),
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
