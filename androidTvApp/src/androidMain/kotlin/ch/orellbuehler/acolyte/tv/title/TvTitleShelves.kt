package ch.orellbuehler.acolyte.tv.title

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.api.model.TitlePerson
import ch.orellbuehler.acolyte.api.model.TitleSeasonEpisode
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.EpisodeTone
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvBackdropCard
import ch.orellbuehler.acolyte.tv.components.TvChip
import ch.orellbuehler.acolyte.tv.components.TvChipInfo
import ch.orellbuehler.acolyte.tv.components.TvPersonCard
import ch.orellbuehler.acolyte.tv.components.TvShelf
import ch.orellbuehler.acolyte.tv.components.TvTitleCard
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import java.time.LocalDate

private const val SHELF_LIMIT = 20

fun LazyListScope.titleShelves(
    state: TitleState,
    episodes: (@Composable () -> Unit)?,
) {
    if (episodes != null) item(key = "episodes") { episodes() }
    val people = state.cast.ifEmpty { if (state.legacy) state.credits.map { it.asPerson() } else emptyList() }
    if (people.isNotEmpty()) {
        item(key = "cast") {
            val navigator = LocalTvNavigator.current
            TvShelf(stringResource(R.string.tv_title_cast)) {
                items(people.take(SHELF_LIMIT)) { person ->
                    TvPersonCard(
                        name = person.name,
                        photo = person.profile,
                        role = person.character.ifEmpty { roleText(person.role) },
                        onClick = if (person.id.isEmpty()) null else ({ navigator.openPerson(person.id) }),
                    )
                }
            }
        }
    }
    if (state.collectionParts.isNotEmpty()) {
        item(key = "collection") {
            val navigator = LocalTvNavigator.current
            val id =
                state.details
                    ?.collection
                    ?.id
                    .orEmpty()
            TvShelf(state.collectionName, seeAll = if (id.isEmpty()) null else ({ navigator.openCollection(id) })) {
                items(state.collectionParts) { TvTitleCard(it, state.access.manage) }
            }
        }
    }
    cardShelf("similar", state.similar, state) { stringResource(R.string.tv_title_more_like_this) }
    cardShelf("more-from", state.moreFromItems, state) {
        if (state.moreFrom.person.isEmpty()) {
            stringResource(R.string.tv_title_more_from_them)
        } else {
            stringResource(R.string.tv_title_more_from, state.moreFrom.person)
        }
    }
    val lead = state.leadPerson?.name.orEmpty()
    if (lead.isNotEmpty()) cardShelf("lead", state.leadItems, state) { stringResource(R.string.tv_title_more_with, lead) }
}

private fun LazyListScope.cardShelf(
    key: String,
    cards: List<TitleCard>,
    state: TitleState,
    title: @Composable () -> String,
) {
    if (cards.isEmpty()) return
    item(key = key) {
        TvShelf(title()) { items(cards.take(SHELF_LIMIT)) { TvTitleCard(it, state.access.manage) } }
    }
}

private fun ch.orellbuehler.acolyte.api.model.TitleCredit.asPerson() =
    TitlePerson(
        name = person.name,
        role = roles.firstOrNull().orEmpty(),
        character = characters.firstOrNull().orEmpty(),
        profile = person.profile,
        id = person.id,
    )

@Composable
private fun roleText(role: String): String =
    when (role) {
        "director" -> stringResource(R.string.tv_title_role_director)
        "creator" -> stringResource(R.string.tv_title_role_creator)
        "actor", "" -> ""
        else -> role.replaceFirstChar { it.uppercase() }
    }

@Composable
internal fun EpisodesSection(
    state: TitleState,
    viewModel: TvTitleViewModel,
) {
    val browser = viewModel.seasonBrowser
    val browse by browser.state.collectAsStateWithLifecycle()
    val seasons = viewModel.seasons()
    val owned by seasons.state.collectAsStateWithLifecycle()
    LaunchedEffect(browser) { browser.load() }
    LaunchedEffect(seasons, browse.selected, state.isOwned) { if (state.isOwned) seasons.ensure(browse.selected) }
    val have = if (state.isOwned) owned.seasons[browse.selected]?.episodes.orEmpty() else emptyList()
    val today = LocalDate.now().toString()
    Column {
        Text(
            stringResource(R.string.tv_title_seasons),
            style = TvType.headline,
            color = TvColors.text,
            modifier = Modifier.tvPageInset(),
        )
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = TvMetrics.marginX, vertical = TvMetrics.focusPad),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(browse.ordered, key = { it.number }) { season ->
                val mark = if (season.number == browse.newSeason) " •" else ""
                TvChip(
                    title = tvSeasonName(season.number) + mark,
                    selected = season.number == browse.selected,
                    onClick = { viewModel.selectSeason(season.number) },
                )
            }
        }
        if (browse.failed) {
            Text(
                stringResource(R.string.tv_title_season_failed, tvSeasonName(browse.selected)),
                style = TvType.footnote,
                color = TvColors.textMuted,
                modifier = Modifier.tvPageInset(),
            )
        } else {
            LazyRow(
                modifier = Modifier.focusRestorer(),
                contentPadding = PaddingValues(horizontal = TvMetrics.marginX, vertical = TvMetrics.focusPad),
                horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
                verticalAlignment = Alignment.Top,
            ) {
                items(browse.shown, key = { it.number }) { episode ->
                    EpisodeCard(episode, state.backdrop, episodeChip(episode, have, today))
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: TitleSeasonEpisode,
    backdrop: String,
    chip: TvChipInfo?,
) {
    val name = episode.name.ifEmpty { stringResource(R.string.tv_title_episode_number, episode.number) }
    val runtime = tvRuntimeText(episode.runtime)
    val air = if (episode.airDay.isEmpty()) stringResource(R.string.tv_title_no_air_date) else tvAirText(episode.airDay)
    TvBackdropCard(
        title = "${episode.number}. $name",
        subtitle = listOf(runtime, air).filter { it.isNotEmpty() }.joinToString(" · "),
        detail = episode.overview,
        chip = chip,
        image = episode.still.ifEmpty { backdrop },
        onClick = {},
    )
}

@Composable
private fun episodeChip(
    episode: TitleSeasonEpisode,
    have: List<ch.orellbuehler.acolyte.api.model.OwnedEpisode>,
    today: String,
): TvChipInfo? {
    val owned = have.firstOrNull { it.episode == episode.number } ?: return null
    return when (TitleLogic.episodeTone(owned)) {
        EpisodeTone.HAVE -> TvChipInfo(stringResource(R.string.tv_title_state_in_library), Tone.OK)
        EpisodeTone.DOWNLOADING -> TvChipInfo(stringResource(R.string.tv_title_state_downloading), Tone.WAIT)
        EpisodeTone.FAILED -> TvChipInfo(stringResource(R.string.tv_title_state_download_failed), Tone.FAIL)
        EpisodeTone.MISSING ->
            if (TitleLogic.notAired(owned, today)) null else TvChipInfo(stringResource(R.string.tv_title_state_not_downloaded), Tone.IDLE)
    }
}
