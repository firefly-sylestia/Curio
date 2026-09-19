package com.curio.app.features.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.BuildConfig
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsNavRail
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.navigateToSettingsSection
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioRoseInk
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * What's New (v403): the release's own highlights, written by hand and kept
 * in the APK ([WHATS_NEW_RELEASES]), so the page is instant and needs no
 * network. Each line names one thing a release shipped and, where there is
 * somewhere to go, carries a "Take me there" door straight to it.
 *
 * The page opens ITSELF once per version ([AppPreferences.getWhatsNewSeenVersion]
 * is stamped the moment it is shown), on a fresh install and on an update
 * alike; the Settings row under Updates stays the way back at any time.
 */
internal data class WhatsNewItem(
    /** A [CurioIcons] glyph: the item's own mark. */
    val glyph: String,
    val title: String,
    val detail: String,
    /** Where "Take me there" goes; null when the item has no one place. */
    val route: String? = null
)

/** One release's highlights, keyed by the app's versionCode. */
internal data class WhatsNewRelease(
    val versionCode: Int,
    val versionName: String,
    val headline: String,
    val items: List<WhatsNewItem>
)

/** Newest release first, so the page shows the one this build IS. */
internal val WHATS_NEW_RELEASES: List<WhatsNewRelease> = listOf(
    WhatsNewRelease(
        versionCode = 20260922,
        versionName = "1.1.1",
        headline = "Four screens rebuilt, and one sheet that reaches them",
        items = listOf(
            WhatsNewItem(
                glyph = CurioIcons.Notes,
                title = "The journal page",
                detail = "Your writing page, rebuilt: a print carries its own caption (face, size " +
                    "and a date), a picture or a voice note sits inside a sentence, headings pin " +
                    "to the top as you scroll, and a to-do row keeps its box.",
                route = CurioRoutes.JOURNALS
            ),
            WhatsNewItem(
                glyph = CurioIcons.Inventory2,
                title = "The Cabinet, rebuilt",
                detail = "Everything you saved on one shelf, with search, sort and category chips, " +
                    "and your own writing beside it: the pages you wrote about a topic and your " +
                    "to-do lists each open from their own shelf.",
                route = CurioRoutes.CABINET
            ),
            WhatsNewItem(
                glyph = CurioIcons.MenuBook,
                title = "The book screen",
                detail = "One page for the whole book: its own chapters and page count, your " +
                    "review, the reading side when something is written, and a reader that zooms " +
                    "where you pinch and opens with its own reader ink.",
                route = CurioRoutes.CABINET
            ),
            WhatsNewItem(
                glyph = CurioIcons.Refresh,
                title = "Online mode",
                detail = "Sign in and keep your account, your profile and your writing in sync, " +
                    "with your own privacy rules stated on the page beside it.",
                route = CurioRoutes.SETTINGS_ONLINE
            ),
            WhatsNewItem(
                glyph = CurioIcons.Apps,
                title = "One sheet opens them all",
                detail = "The \"+\" on Home drops one sheet with every door: a journal page, a " +
                    "to-do list, a note on any topic you like and a book, each straight into its " +
                    "own screen.",
                route = CurioRoutes.HOME
            )
        )
    )
)

/** The release this build IS, when one has been authored for it. */
internal fun whatsNewRelease(versionCode: Int): WhatsNewRelease? =
    WHATS_NEW_RELEASES.firstOrNull { it.versionCode == versionCode }

@Composable
fun WhatsNewScreen(navController: NavController) {
    val context = LocalContext.current
    val release = whatsNewRelease(BuildConfig.VERSION_CODE)

    // Shown once per version: stamping it here (not on the way out) means a
    // page the user backs out of is not offered again on the next launch.
    LaunchedEffect(Unit) {
        AppPreferences.setWhatsNewSeenVersion(context, BuildConfig.VERSION_CODE)
    }

    val accent = curioRoseInk()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                heroPageBackground(
                    lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)
                )
            )
    ) {
        // ── Watermark backdrop: muted category glyphs (settings family).
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        ScreenEntrance {
            val listState = rememberLazyListState()
            val glassBackdrop = rememberLayerBackdrop()
            val wide = windowWidthSizeClass().isWide
            LazyColumn(
                state = listState,
                modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = if (wide) 0.dp else SettingsHeroTotalHeight,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (wide) {
                    item(key = "hero", contentType = "hero") {
                        SettingsHeroHeader(
                            title = "What's New",
                            subtitle = "The highlights of this version",
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                item(key = "settings-nav", contentType = "settings-nav") {
                    SettingsNavRail(
                        active = null,
                        onSelect = { navigateToSettingsSection(navController, it) },
                        navController = navController
                    )
                }
                // ── The version's own opening card.
                item {
                    SettingsOptionCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CurioIcon(
                                        CurioIcons.AutoAwesome, null,
                                        tint = accent,
                                        size = 22.dp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        release?.headline ?: "Welcome to Curio",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                    Text(
                                        "v${release?.versionName ?: BuildConfig.VERSION_NAME} · " +
                                            "build ${BuildConfig.VERSION_CODE}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = accent.copy(alpha = 0.12f),
                                    contentColor = accent
                                ) {
                                    Text(
                                        "New",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (release == null) {
                    item { SettingsSectionHeading("This version") }
                    item {
                        SettingsOptionCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "The highlights for this build are on their way. " +
                                        "You are reading the newest Curio that exists.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                } else {
                    item { SettingsSectionHeading("In this version") }
                    release.items.forEachIndexed { index, item ->
                        item(key = "item-$index", contentType = "whats-new-item") {
                            WhatsNewItemCard(item = item) { route ->
                                navController.navigate(route) { launchSingleTop = true }
                            }
                        }
                    }
                }
                item { SettingsSectionHeading("How this works") }
                item {
                    SettingsOptionCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "This page opens itself once for every version, installed fresh " +
                                    "or updated, and then waits here. Come back any time from " +
                                    "Settings, under Updates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Pinned hero overlay: phone-only, like every settings page.
            if (!wide) {
                SettingsHeroHeader(
                    title = "What's New",
                    subtitle = "The highlights of this version",
                    onBack = { navController.popBackStack() },
                    glassBackdrop = glassBackdrop
                )
            }
        }
    }
}

/** One highlight: its mark, its name, one line about it, and the door. */
@Composable
private fun WhatsNewItemCard(item: WhatsNewItem, onOpen: (String) -> Unit) {
    val accent = curioRoseInk()
    SettingsOptionCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(item.glyph, null, tint = accent, size = 20.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        item.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val route = item.route
            if (route != null) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Surface(
                        onClick = { onOpen(route) },
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.12f),
                        contentColor = accent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Take me there",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            CurioIcon(
                                CurioIcons.ArrowForward, null,
                                tint = accent,
                                size = 16.dp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
