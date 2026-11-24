package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.OwnedEpisode
import ch.orellbuehler.acolyte.api.model.OwnedSeason
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.EpisodeTone
import ch.orellbuehler.acolyte.feature.title.ReleaseScope
import ch.orellbuehler.acolyte.feature.title.ReleaseScopeKind
import ch.orellbuehler.acolyte.feature.title.ShowSeasonsPresenter
import ch.orellbuehler.acolyte.feature.title.ShowSeasonsState
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowSeasons(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val show = state.owned?.show ?: return
    val seasonsPresenter = remember(state.ownedId) { viewModel.seasons() }
    val seasons by seasonsPresenter.state.collectAsStateWithLifecycle()
    val grouped = show.episodeGroup?.id?.isNotEmpty() == true
    LaunchedEffect(seasonsPresenter, grouped) { if (grouped) seasonsPresenter.loadOutside() }
    val manage = state.access.manage
    val ordered = TitleLogic.seasonOrder(show.seasons.map { it.number }).mapNotNull { n -> show.seasons.firstOrNull { it.number == n } }
    FeedbackEffect(seasons.feedback)
    Column {
        SectionHeader(stringResource(R.string.title_seasons))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            modifier = Modifier.padding(horizontal = Spacing.Screen),
        ) {
            show.episodeGroup
                ?.name
                ?.takeIf { it.isNotBlank() }
                ?.let { AssistChip(onClick = {}, label = { Text(it) }) }
            if (state.getAllMissing) {
                OutlinedButton(onClick = { viewModel.launch { viewModel.presenter.searchMissing() } }, enabled = state.busy != "missing") {
                    Text(stringResource(R.string.title_get_all_missing))
                }
            }
            if (manage && (seasons.belowCount > 0 || seasons.belowOnly)) {
                FilterChip(
                    selected = seasons.belowOnly,
                    onClick = seasonsPresenter::toggleBelow,
                    label = { Text(stringResource(R.string.title_below_count, seasons.belowCount)) },
                )
            }
            val allOpen = seasons.open.size >= ordered.size && ordered.isNotEmpty()
            TextButton(onClick = {
                if (allOpen) seasonsPresenter.collapseAll() else viewModel.launch { seasonsPresenter.expandAll(ordered.map { it.number }) }
            }) { Text(stringResource(if (allOpen) R.string.title_collapse_all else R.string.title_expand_all)) }
        }
        if (state.showOffHint) NoticeBanner(stringResource(R.string.title_turn_show_on))
        ordered.forEach { season ->
            SeasonRow(season, state, seasons, seasonsPresenter, viewModel)
            if (season.number in seasons.open) SeasonEpisodes(season.number, state, seasons, seasonsPresenter, viewModel)
            HorizontalDivider(Modifier.padding(horizontal = Spacing.Screen))
        }
        if (seasons.outside.isNotEmpty()) {
            var open by rememberSaveable { mutableStateOf(false) }
            TextButton(onClick = { open = !open }, modifier = Modifier.padding(horizontal = Spacing.Small)) {
                Text(stringResource(R.string.title_not_in_order, seasons.outside.size))
            }
            if (open) {
                Text(
                    stringResource(R.string.title_not_in_order_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = Spacing.Screen),
                )
                seasons.outside.forEach { episode -> EpisodeRow(episode, state, seasons, seasonsPresenter, viewModel) }
            }
        }
    }
}

@Composable
private fun SeasonRow(
    season: OwnedSeason,
    state: TitleState,
    seasons: ShowSeasonsState,
    presenter: ShowSeasonsPresenter,
    viewModel: TitleViewModel,
) {
    val manage = state.access.manage
    val open = season.number in seasons.open
    Row(
        Modifier.fillMaxWidth().clickable { viewModel.launch { presenter.toggle(season.number) } }.padding(horizontal = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (manage) {
            Checkbox(
                checked = season.keepUpToDate,
                onCheckedChange = { viewModel.launch { presenter.keepSeason(season.number, it) } },
                enabled = seasons.busy != "season-${season.number}",
            )
        } else {
            Box(Modifier.width(Spacing.Small))
        }
        Column(Modifier.weight(1f).padding(vertical = Spacing.Small)) {
            Text(season.name.ifBlank { seasonName(season.number) }, style = MaterialTheme.typography.titleSmall)
            val notes = mutableListOf<String>()
            if (season.downloading > 0) notes += stringResource(R.string.title_season_downloading, season.downloading)
            if (season.failed > 0) notes += stringResource(R.string.title_season_failed_count, season.failed)
            val next = season.nextAirDay.ifBlank { season.nextAirDate }
            if (next.isNotBlank()) notes += stringResource(R.string.title_season_next, airText(next))
            if (notes.isNotEmpty()) {
                Text(
                    notes.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val tone =
            when {
                season.episodes > 0 && season.inLibrary >= season.episodes -> Tone.OK
                season.inLibrary > 0 -> Tone.INFO
                season.missing > 0 -> Tone.WAIT
                else -> Tone.IDLE
            }
        StatusChip("${season.inLibrary} / ${season.episodes}", tone)
        if (manage) {
            IconButton(onClick = { viewModel.launch { viewModel.presenter.searchSeason(season.number) } }, enabled = season.missing > 0) {
                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.title_get_missing_season, season.number))
            }
            IconButton(onClick = { viewModel.openReleases(ReleaseScope(ReleaseScopeKind.SEASON, season = season.number)) }) {
                Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.title_choose_release_season, season.number))
            }
            IconButton(onClick = { viewModel.open(TitleSheet.SeasonTools(season.number)) }) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(R.string.title_season_tools, season.number))
            }
        }
        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun SeasonEpisodes(
    season: Int,
    state: TitleState,
    seasons: ShowSeasonsState,
    presenter: ShowSeasonsPresenter,
    viewModel: TitleViewModel,
) {
    val loaded = seasons.seasons[season]
    when {
        loaded == null || loaded.loading ->
            Text(
                stringResource(R.string.common_loading),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(Spacing.Screen),
            )
        loaded.failed ->
            Row(Modifier.padding(horizontal = Spacing.Screen), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.title_season_load_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.launch { presenter.load(season) } }) { Text(stringResource(R.string.common_retry)) }
            }
        else -> {
            val list = seasons.episodesOf(season)
            val last = list.maxOfOrNull { it.episode } ?: 0
            list.forEach { episode ->
                EpisodeRow(
                    episode,
                    state,
                    seasons,
                    presenter,
                    viewModel,
                    finale =
                        list.size > 1 && episode.episode == last,
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: OwnedEpisode,
    state: TitleState,
    seasons: ShowSeasonsState,
    presenter: ShowSeasonsPresenter,
    viewModel: TitleViewModel,
    finale: Boolean = false,
) {
    val manage = state.access.manage
    val tone = TitleLogic.episodeTone(episode)
    val slot = episode.mainSlot
    val file = slot?.file
    val loc = locale()
    val notAired = TitleLogic.notAired(episode, today())
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { viewModel.openEpisode(episode) }
            .padding(start = if (manage) 0.dp else Spacing.Screen, end = Spacing.Small, top = Spacing.XSmall, bottom = Spacing.XSmall)
            .alpha(if (notAired && file == null) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        if (manage) {
            Checkbox(
                checked = episode.keepUpToDate,
                onCheckedChange = { viewModel.launch { presenter.keepEpisode(episode, it) } },
                enabled = seasons.busy != "episode-${episode.id}",
            )
        }
        ServerImage(
            source = episode.still,
            contentDescription = null,
            size = ImageSize.BACKDROP,
            placeholderName = episode.episode.toString(),
            modifier = Modifier.width(84.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(
                    "${episode.episode}. " + episode.name.ifBlank { stringResource(R.string.title_episode_number, episode.episode) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (finale) {
                    Text(
                        stringResource(R.string.title_finale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.acolyte.accentText,
                    )
                }
            }
            val line = mutableListOf<String>()
            airText(episode.airDay.ifBlank { episode.airDate }).takeIf { it.isNotBlank() }?.let { line += it }
            if (episode.minutes > 0) line += runtimeText(episode.minutes)
            file?.let { line += formatBytes(it.size, loc) }
            val points = slot?.points
            if (manage && points != null) line += stringResource(R.string.title_points_inline, points, slot.goodEnoughPoints ?: 0)
            if (line.isNotEmpty()) {
                Text(
                    line.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (manage && slot?.state == "chosen" && slot.reason.isNotBlank()) {
                Text(slot.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.acolyte.waitText, maxLines = 2)
            }
            val status =
                when {
                    file != null -> TitleLogic.epQuality(file).ifBlank { stringResource(R.string.title_state_in_library) }
                    notAired -> stringResource(R.string.title_not_out_yet)
                    slot != null -> slot.stateLabel.ifBlank { stringResource(R.string.title_missing) }
                    else -> stringResource(R.string.title_missing)
                }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(MaterialTheme.acolyte.dot(episodeTone(tone)), CircleShape))
                Text(status, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (manage && slot != null) {
            IconButton(
                onClick = {
                    viewModel.launch {
                        viewModel.presenter.searchSlot(
                            slot.id,
                            TitleLogic.episodeCode(episode.season, episode.episode),
                        )
                    }
                },
                enabled = tone != EpisodeTone.DOWNLOADING,
            ) { Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.title_look_for_episode)) }
            IconButton(onClick = {
                viewModel.openReleases(ReleaseScope(ReleaseScopeKind.EPISODE, episode.season, episode.episode, episode.id, slot.id))
            }) { Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.title_choose_release_episode)) }
        }
    }
}

fun episodeTone(tone: EpisodeTone): Tone =
    when (tone) {
        EpisodeTone.HAVE -> Tone.OK
        EpisodeTone.DOWNLOADING -> Tone.INFO
        EpisodeTone.FAILED -> Tone.FAIL
        EpisodeTone.MISSING -> Tone.IDLE
    }

@Composable
fun SeasonToolsContent(
    season: Int,
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val presenter = remember(state.ownedId) { viewModel.seasons() }
    val seasons by presenter.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    val loc = locale()
    LaunchedEffect(presenter, season) { presenter.ensure(season) }
    LaunchedEffect(tab) { if (tab == 2 && !seasons.historyLoaded) presenter.loadHistory() }
    val show = state.owned?.show
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text(seasonName(season), style = MaterialTheme.typography.titleLarge)
        SegmentRow(
            listOf(
                0 to stringResource(R.string.title_tools_files),
                1 to stringResource(R.string.title_tools_rename),
                2 to stringResource(R.string.title_history),
            ),
            tab,
        ) { tab = it }
        when (tab) {
            0 -> {
                val files = seasons.seasonFiles(season)
                val all = seasons.seasons[season]?.episodes?.size ?: 0
                Text(
                    stringResource(R.string.title_tools_summary, files.size, all, formatBytes(seasons.seasonSize(season), loc)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (show != null && show.packWindow > 0) {
                    Text(stringResource(R.string.title_tools_pack_note, show.packWindow), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { viewModel.launch { presenter.packNext(season) } }, enabled = !seasons.packBusy) {
                        Text(stringResource(R.string.title_tools_pack_next, show.packWindow))
                    }
                }
                if (files.isEmpty()) Text(stringResource(R.string.title_tools_no_files), style = MaterialTheme.typography.bodyMedium)
                files.forEach { e ->
                    val f = e.mainSlot?.file ?: return@forEach
                    Column {
                        Text(
                            "${e.episode}. ${f.fileName}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOf(TitleLogic.epQuality(f), formatBytes(f.size, loc), f.group, f.audioLanguages.take(4).joinToString(" "))
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            1 -> RenameContent(remember(season) { viewModel.presenter.rename(season) }, state.name, viewModel)
            else -> {
                val rows = seasons.seasonHistory(season)
                if (!seasons.historyLoaded) {
                    Text(stringResource(R.string.common_loading))
                } else if (rows.isEmpty()) {
                    Text(stringResource(R.string.title_tools_history_empty))
                }
                rows.forEach { HistoryLine(it, TitleLogic.eventTone(it.kind)) }
            }
        }
        TextButton(onClick = {
            viewModel.close()
            viewModel.openRemove(season = season)
        }) { Text(stringResource(R.string.title_delete_season, seasonName(season)), color = MaterialTheme.acolyte.failText) }
    }
}

@Composable
fun EpisodeContent(
    presenter: ch.orellbuehler.acolyte.feature.title.EpisodePresenter,
    viewModel: TitleViewModel,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    val e = state.episode
    val file = state.file
    val loc = locale()
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text(stringResource(R.string.title_episode_title, e.season, e.episode), style = MaterialTheme.typography.titleLarge)
        ServerImage(
            source = e.still,
            contentDescription = null,
            size = ImageSize.BACKDROP,
            placeholderName = e.name.ifBlank { state.code },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
        )
        Text(e.name.ifBlank { stringResource(R.string.title_episode_number, e.episode) }, style = MaterialTheme.typography.titleMedium)
        val meta = listOfNotNull(airText(e.airDay.ifBlank { e.airDate }).ifBlank { null }, runtimeText(e.minutes).ifBlank { null })
        if (meta.isNotEmpty()) {
            Text(
                meta.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (file != null) StatusChip(stringResource(R.string.title_state_in_library), Tone.OK)
        if (state.synopsis.isNotBlank()) Text(state.synopsis, style = MaterialTheme.typography.bodyMedium)
        if (state.tone != EpisodeTone.HAVE) {
            LibraryCard {
                Text(
                    e.mainSlot?.stateLabel?.ifBlank { null } ?: stringResource(R.string.title_missing),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (state.manage && state.chosenWhy.isNotBlank()) Text(state.chosenWhy, style = MaterialTheme.typography.bodySmall)
                if (state.manage && !state.downloading && e.mainSlot != null) {
                    OutlinedButton(onClick = { viewModel.launch { presenter.lookFor() } }, enabled = !state.busy) {
                        Text(stringResource(R.string.title_look_for_it))
                    }
                }
            }
        }
        if (file != null) {
            LibraryCard {
                InfoLine(stringResource(R.string.title_quality), TitleLogic.epQuality(file).ifBlank { file.resolutionLabel })
                InfoLine(stringResource(R.string.title_size), formatBytes(file.size, loc))
                if (file.addedAt.isNotBlank()) InfoLine(stringResource(R.string.title_added), formatDate(file.addedAt, loc))
                if (file.audioLanguages.isNotEmpty()) {
                    InfoLine(
                        stringResource(R.string.title_audio),
                        file.audioLanguages.take(8).joinToString(" "),
                    )
                }
                if (file.subtitleLanguages.isNotEmpty()) {
                    InfoLine(
                        stringResource(R.string.title_subtitles),
                        file.subtitleLanguages.take(8).joinToString(" "),
                    )
                }
                Text(file.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.manage) PointsView(state.points) {}
            }
        }
        if (state.manage) {
            SettingSwitch(
                stringResource(R.string.title_keep_up_to_date),
                stringResource(R.string.title_better_copies),
                e.keepUpToDate,
                enabled = !state.busy,
            ) {
                viewModel.launch { presenter.setKeep(it) }
            }
            if (state.subtitlesOn) {
                MenuRow(stringResource(R.string.title_subtitles), stringResource(R.string.title_subtitles_choose)) {
                    viewModel.openSubtitles(e.id)
                }
            }
            TextButton(onClick = {
                viewModel.close()
                viewModel.openRemove(episodeIds = listOf(e.id))
            }) { Text(stringResource(R.string.title_delete_episode), color = MaterialTheme.acolyte.failText) }
            Text(stringResource(R.string.title_history), style = MaterialTheme.typography.titleMedium)
            if (state.eventsLoaded &&
                state.events.isEmpty()
            ) {
                Text(stringResource(R.string.title_history_nothing_yet), style = MaterialTheme.typography.bodyMedium)
            }
            state.events.forEach { HistoryLine(it, TitleLogic.eventTone(it.kind)) }
        }
    }
}
