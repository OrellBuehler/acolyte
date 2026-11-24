package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicArtistKind
import ch.orellbuehler.acolyte.feature.music.MusicArtistState
import ch.orellbuehler.acolyte.feature.music.MusicRules
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.text.NumberFormat
import java.util.Locale

private val Sentence = Regex("(?<=[.!?])\\s+")

@Composable
private fun activeYears(state: MusicArtistState): String {
    if (state.activeSince.isEmpty()) return ""
    val until = state.activeUntil.ifEmpty { stringResource(R.string.music_artist_about_now) }
    return "${state.activeSince} – $until"
}

@Composable
fun MusicArtistAboutCard(
    state: MusicArtistState,
    onOpen: () -> Unit,
) {
    val listeners =
        if (state.listeners > 0) {
            pluralStringResource(R.plurals.music_artist_listeners, state.listeners.artistCount(), MusicRules.shortCount(state.listeners))
        } else {
            ""
        }
    val line = listOf(listeners, activeYears(state), state.genres.take(2).joinToString(", ")).filter { it.isNotEmpty() }.joinToString(" · ")
    val bio =
        state.bio
            ?.text
            .orEmpty()
            .split(Sentence)
            .take(2)
            .joinToString(" ")
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                MusicArt(state.image.ifEmpty { state.wide }, state.name, Modifier.size(56.dp), round = true, size = 250)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.music_artist_about_title, state.name), style = MaterialTheme.typography.titleMedium)
                    if (state.rank > 0) {
                        Text(
                            stringResource(R.string.music_artist_stats_rank, state.rank),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (line.isNotEmpty()) {
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (bio.isNotEmpty()) Text(bio, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.music_artist_about_more, state.name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MusicArtistAboutSheet(
    state: MusicArtistState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val number = NumberFormat.getIntegerInstance()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.Screen)
                .padding(bottom = Spacing.XLarge)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                MusicArt(state.image.ifEmpty { state.wide }, state.name, Modifier.size(88.dp), round = true, size = 250)
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.music_artist_about_title, state.name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (state.rank > 0) {
                        Text(
                            stringResource(R.string.music_artist_stats_rank, state.rank),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.listeners > 0) AboutRow(stringResource(R.string.music_artist_about_listeners), number.format(state.listeners))
            if (state.change != 0L) {
                val up = state.change > 0
                AboutRow(
                    stringResource(R.string.music_artist_about_today),
                    stringResource(
                        if (up) R.string.music_artist_stats_up else R.string.music_artist_stats_down,
                        MusicRules.shortCount(kotlin.math.abs(state.change)),
                    ),
                )
            }
            if (state.albumCount > 0) AboutRow(stringResource(R.string.music_artist_about_albums), state.albumCount.toString())
            if (state.singleCount > 0) AboutRow(stringResource(R.string.music_artist_about_singles), state.singleCount.toString())
            if (state.country.isNotEmpty()) {
                val country = Locale("", state.country).displayCountry.ifEmpty { state.country }
                Text(stringResource(R.string.music_artist_about_from, country), style = MaterialTheme.typography.bodyMedium)
            }
            if (state.activeSince.isNotEmpty()) {
                val until = state.activeUntil.ifEmpty { stringResource(R.string.music_artist_about_now) }
                Text(
                    stringResource(R.string.music_artist_about_active, state.activeSince, until),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            AboutRow(
                stringResource(R.string.music_artist_about_kind),
                stringResource(
                    when (state.kind) {
                        MusicArtistKind.SOLO -> R.string.music_artist_kind_solo
                        MusicArtistKind.GROUP -> R.string.music_artist_kind_group
                        MusicArtistKind.ARTIST -> R.string.music_artist_kind_artist
                    },
                ),
            )
            state.bio?.let { bio ->
                bio.text
                    .split("\n\n")
                    .filter { it.isNotBlank() }
                    .forEach { Text(it.trim(), style = MaterialTheme.typography.bodyMedium) }
                if (bio.attribution.isNotEmpty()) {
                    Text(
                        stringResource(R.string.music_artist_bio_source, bio.attribution),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (bio.license.isNotEmpty()) {
                    Text(
                        stringResource(R.string.music_artist_bio_licence, bio.license),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (bio.licenseUrl.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.clickable(enabled = bio.licenseUrl.isNotEmpty()) { openArtistUrl(context, bio.licenseUrl) },
                    )
                }
                if (bio.url.isNotEmpty()) {
                    TextButton(onClick = { openArtistUrl(context, bio.url) }) { Text(stringResource(R.string.music_artist_bio_read)) }
                }
            }
            if (state.genres.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    state.genres.forEach { genre -> SuggestionChip(onClick = {}, label = { Text(genre) }) }
                }
            }
        }
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
