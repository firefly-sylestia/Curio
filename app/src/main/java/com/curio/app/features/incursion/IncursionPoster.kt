package com.curio.app.features.incursion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.curio.app.data.AppPreferences
import com.curio.app.data.IncursionEntry
import com.curio.app.features.reveal.FilmPosterFetch
import com.curio.app.features.reveal.SeriesPosterFetch
import com.curio.app.features.reveal.TmdbFetch
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioTintOn
import com.curio.app.ui.theme.isCurioDarkTheme
import java.util.concurrent.ConcurrentHashMap

/**
 * v427 — THE ART ON AN INCURSION ROW.
 *
 * The member, of the page's list: *"with movie cover also in list i want proper
 * moview cover fetching"*. The data has always carried what that needs — each row
 * knows its own [IncursionEntry.tmdbId] — so this is the app's own poster chain
 * with one better first step:
 *
 *  1. **TMDB by ID** ([TmdbFetch.posterUrlById]). A row that knows its number does
 *     not need a name search, and a name search is exactly how a remake's poster
 *     lands on the original. The kind comes from the row itself (a series is a
 *     show, a film is a movie) and the OTHER catalogue is tried only when the
 *     first answers nothing, so an id upstream filed under the other kind still
 *     finds its art.
 *  2. **The keyless pair** — [FilmPosterFetch] for a film, [SeriesPosterFetch] for
 *     a series — which is the same free-first chain the reveal uses (iTunes, then
 *     TVMaze, then TMDB by name).
 *
 * Two project rules hold it:
 *
 *  - **Consent first.** Every candidate here is a network URL, so nothing is
 *    asked while the app's own artwork switch is off
 *    (`AppPreferences.coverFetchEnabledState`, the Experiments page's "Cover
 *    fetching"). With it off the row keeps the plate it is drawn with instead of
 *    silently downloading — the same rule `BookBrowserScreen`'s rail follows.
 *  - **Nothing is asked twice.** A resolved URL and a MISS are both remembered per
 *    storage key for the life of the process (`""` = asked, nothing there); Coil's
 *    own disk cache holds the bytes after that.
 */
internal object IncursionPosters {

    /** `storageKey → poster URL` (`""` = asked, nothing there). */
    private val cache = ConcurrentHashMap<String, String>()

    /** The poster for a row, or null when there is none to be had. */
    suspend fun resolve(entry: IncursionEntry): String? {
        val key = entry.storageKey
        cache[key]?.let { remembered -> return remembered.ifEmpty { null } }
        val isSeries = entry.type.lowercase() == "series"
        val byId = entry.tmdbId?.takeIf { it > 0 }?.let { id ->
            TmdbFetch.posterUrlById(id, isShow = isSeries)
                ?: TmdbFetch.posterUrlById(id, isShow = !isSeries)
        }
        // v428 — A SERIES IS ASKED FOR BY ITS OWN NAME, WITHOUT THE SEASON OR THE
        // YEAR.
        //
        // These rows read "WandaVision S1", "Loki S2", "The Gifted S1" — the
        // season is WHICH PART of the show a row is, never which show — and that
        // is a string no catalogue answers: TVMaze returned nothing for every one
        // of them, so a series row kept its plate while its title sat there in
        // plain sight (member: "the posters are not loading for films and in
        // incursion movies or series"). A FILM wants its year, because that is
        // what tells the 2010 poster from the 1980 one; a show does not, and every
        // door now strips such suffixes anyway ([stripNaming]).
        val resolved = byId ?: if (isSeries) {
            SeriesPosterFetch.resolvePosterUrl(entry.title)
        } else {
            FilmPosterFetch.resolvePosterUrl(
                entry.year?.takeIf { it > 0 }?.let { "${entry.title} ($it)" } ?: entry.title
            )
        }
        cache[key] = resolved.orEmpty()
        return resolved
    }
}

/**
 * THE PLATE A ROW WEARS — the poster when one has arrived, and the app's own
 * drawn plate while it has not (or while fetching is off).
 *
 * The plate is not a grey box with a spinner: it is the row's ORDER in the page's
 * accent over a tinted card, so a list of thirty rows reads as a list of thirty
 * numbered films with artwork arriving into them, and a member who never turns
 * fetching on still sees a finished page. The mark beside the number is the row's
 * own KIND (a reel for a series, a film for a film).
 *
 * THE CALLER OWNS THE SIZE — a row wants 40×56, a grid tile wants the whole tile
 * width by its own height — so the plate takes a [modifier] and never imposes one.
 */
@Composable
internal fun IncursionPosterPlate(
    entry: IncursionEntry,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    val consent = AppPreferences.coverFetchEnabledState
    val accent = settingsRoseAccent()
    var url by remember(entry.storageKey, consent) { mutableStateOf<String?>(null) }
    // Resolution runs only while the row is composed — a lazy list therefore asks
    // for the covers the member actually scrolled to, never for all three hundred.
    LaunchedEffect(entry.storageKey, consent) {
        url = if (consent) IncursionPosters.resolve(entry) else null
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                curioTintOn(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    accent,
                    if (isCurioDarkTheme()) 0.10f else 0.06f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CurioIcon(
                    name = if (entry.type.lowercase() == "series") {
                        CurioIcons.Movies
                    } else {
                        CurioIcons.Movie
                    },
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.55f),
                    size = 15.dp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    entry.orderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent.copy(alpha = 0.75f)
                )
            }
        }
    }
}
