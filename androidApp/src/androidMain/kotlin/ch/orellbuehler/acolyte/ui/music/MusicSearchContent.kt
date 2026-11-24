package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.music.MusicArtistHit
import ch.orellbuehler.acolyte.feature.music.MusicBrowseState
import ch.orellbuehler.acolyte.feature.music.MusicCardAction
import ch.orellbuehler.acolyte.feature.music.MusicMark
import ch.orellbuehler.acolyte.feature.music.MusicSearchState
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.theme.Spacing

fun LazyListScope.musicSearchItems(
    query: String,
    browse: MusicBrowseState,
    search: MusicSearchState,
    nav: MusicNav,
) {
    if (query.isBlank()) {
        musicBrowseItems(browse, nav)
    } else {
        musicHitItems(query.trim(), search, nav)
    }
}

private fun LazyListScope.musicBrowseItems(
    browse: MusicBrowseState,
    nav: MusicNav,
) {
    item(key = "music-browse") { SectionHeader(stringResource(R.string.music_search_browse)) }
    if (browse.status.phase == Phase.LOADING) {
        item(key = "music-loading") { Spinner() }
        return
    }
    browse.status.error?.let { item(key = "music-error") { ErrorBanner(it) } }
    if (browse.isEmpty || browse.kid) {
        item(key = "music-empty") { Hint(stringResource(R.string.music_search_empty)) }
        return
    }
    if (browse.sections.isEmpty()) {
        musicTileRows(browse.tiles, nav, "music-tiles")
        return
    }
    browse.sections.forEach { section ->
        item(key = "music-title-${section.id}") { SectionHeader(section.title) }
        musicTileRows(section.tiles, nav, "music-${section.id}")
    }
}

private fun LazyListScope.musicHitItems(
    query: String,
    search: MusicSearchState,
    nav: MusicNav,
) {
    if (search.searched != query) {
        item(key = "music-pending") {
            if (search.searching) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spinner()
                    Hint(stringResource(R.string.music_search_searching))
                }
            } else {
                Hint(stringResource(R.string.music_search_hint))
            }
        }
        return
    }
    search.error?.let { item(key = "music-error") { ErrorBanner(it) } }
    if (search.hits.isEmpty()) {
        if (search.error == null) item(key = "music-none") { Hint(stringResource(R.string.music_search_none)) }
        return
    }
    items(search.hits, key = { "hit-" + it.mbid }) { hit -> MusicHitCard(hit, nav) }
}

@Composable
private fun Spinner() {
    Box(Modifier.fillMaxWidth().padding(Spacing.XLarge), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    )
}

@Composable
private fun MusicHitCard(
    hit: MusicArtistHit,
    nav: MusicNav,
) {
    val sub = hit.disambiguation.ifEmpty { listOf(hit.type, hit.country).filter { it.isNotEmpty() }.joinToString(" · ") }
    Row(
        Modifier.fillMaxWidth().clickable { nav.openArtist(hit.mbid) }.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicArt(hit.image, hit.name, Modifier.size(56.dp), round = true, size = 250)
        Column(Modifier.weight(1f)) {
            Text(hit.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            hit.titleId > 0 -> MusicMarkChip(MusicMark.COMPLETE)
            hit.action == MusicCardAction.ADD ->
                FilledTonalButton(onClick = { nav.act(hit) }) { Text(stringResource(R.string.music_search_add), maxLines = 1) }
            hit.action == MusicCardAction.ASK ->
                FilledTonalButton(onClick = { nav.act(hit) }) {
                    Text(stringResource(if (hit.tooAction) R.string.music_search_ask_too else R.string.music_search_ask), maxLines = 1)
                }
            hit.asked -> MusicMarkChip(MusicMark.REQUESTED)
        }
    }
}
