package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.ReleaseEmpty
import ch.orellbuehler.acolyte.feature.title.ReleaseScopeKind
import ch.orellbuehler.acolyte.feature.title.ReleaseSort
import ch.orellbuehler.acolyte.feature.title.ReleasesPresenter
import ch.orellbuehler.acolyte.feature.title.ReleasesState
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReleasesContent(
    presenter: ReleasesPresenter,
    viewModel: TitleViewModel,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    LaunchedEffect(presenter) { presenter.watch() }
    FeedbackEffect(state.feedback)
    LaunchedEffect(state.done, state.picked) {
        if (state.done) {
            val picked = state.picked
            viewModel.close()
            if (picked != null) viewModel.openAdd(choose = true, checkId = state.checkId, release = picked)
        }
    }
    val scope =
        when (state.scope.kind) {
            ReleaseScopeKind.SEASON -> seasonName(state.scope.season)
            ReleaseScopeKind.EPISODE -> TitleLogic.episodeCode(state.scope.season, state.scope.episode)
            else -> null
        }
    SheetTitle(
        stringResource(if (state.check) R.string.title_check_releases else R.string.title_choose_release),
        listOfNotNull(state.titleName, scope).joinToString(" · "),
    )
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        if (state.check && state.rules.isNotEmpty()) {
            ChoiceRow(
                stringResource(R.string.title_pick_rules),
                state.rules
                    .firstOrNull { it.id == state.rulesId }
                    ?.name
                    .orEmpty(),
                state.rules.map { it.id to it.name },
                state.rulesId,
            ) { presenter.setRules(it) }
        }
        if (state.wholeShow) Text(stringResource(R.string.title_releases_whole_show), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            SegmentRow(
                listOf(
                    false to stringResource(R.string.title_releases_good),
                    true to stringResource(R.string.title_releases_all, state.total),
                ),
                state.allTab,
            ) { presenter.setAllTab(it) }
        }
        if (!state.wholeShow) {
            Button(onClick = { viewModel.launch { presenter.search() } }, enabled = !state.running && (!state.check || state.rulesId > 0)) {
                if (state.running) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(
                        if (state.running) {
                            R.string.title_searching
                        } else if (state.total >
                            0
                        ) {
                            R.string.title_search_again
                        } else {
                            R.string.title_search_now
                        },
                    ),
                    modifier = Modifier.padding(start = Spacing.XSmall),
                )
            }
        }
        state.status.error?.let { ErrorBanner(it) }
        if (state.page?.rulesChanged == true) NoticeBanner(stringResource(R.string.title_releases_rules_changed))
        if (state.running) Text(stringResource(R.string.title_releases_asking), style = MaterialTheme.typography.bodySmall)
        if (state.all.isNotEmpty()) ReleaseTools(state, presenter)
        when {
            state.status.phase == Phase.LOADING -> CircularProgressIndicator()
            state.empty != ReleaseEmpty.NONE -> ReleaseEmptyView(state, presenter)
            else -> state.shown.forEach { release -> ReleaseRow(release, state, presenter, viewModel) }
        }
        if (state.canShowMore) {
            TextButton(onClick = { viewModel.launch { presenter.showMore() } }) {
                Text(stringResource(R.string.title_releases_more, state.all.size, state.total))
            }
        }
    }
}

@Composable
private fun ReleaseTools(
    state: ReleasesState,
    presenter: ReleasesPresenter,
) {
    val sorts =
        listOf(
            ReleaseSort.BEST to stringResource(R.string.title_sort_best),
            ReleaseSort.POINTS to stringResource(R.string.title_sort_points),
            ReleaseSort.QUALITY to stringResource(R.string.title_sort_quality),
            ReleaseSort.SIZE to stringResource(R.string.title_sort_size),
            ReleaseSort.SEEDERS to stringResource(R.string.title_sort_seeders),
            ReleaseSort.AGE to stringResource(R.string.title_sort_age),
        )
    ChoiceRow(
        stringResource(R.string.title_sort),
        sorts.first { it.first == state.sort }.second,
        sorts.mapIndexed { i, s -> i.toLong() to s.second },
        sorts.indexOfFirst { it.first == state.sort }.toLong(),
    ) { presenter.setSort(sorts[it.toInt()].first) }
    if (state.resolutions.size > 1) {
        ChipChoices(
            listOf(0 to stringResource(R.string.title_any)) +
                state.resolutions.map {
                    it to TitleLogic.resolutionWord(it)
                },
            state.resolution,
        ) {
            presenter.setResolution(it)
        }
    }
    if (state.sources.size > 1) {
        ChoiceRow(
            stringResource(R.string.title_source),
            state.source.ifBlank { stringResource(R.string.title_all_sources) },
            listOf(-1L to stringResource(R.string.title_all_sources)) + state.sources.mapIndexed { i, s -> i.toLong() to s },
            if (state.source.isBlank()) -1L else state.sources.indexOf(state.source).toLong(),
        ) { presenter.setSource(if (it < 0) "" else state.sources[it.toInt()]) }
    }
    if (state.languages.isNotEmpty()) {
        ChoiceRow(
            stringResource(R.string.title_language),
            state.language.ifBlank { stringResource(R.string.title_all_languages) }.uppercase(),
            listOf(-1L to stringResource(R.string.title_all_languages)) +
                state.languages.mapIndexed {
                    i,
                    l,
                    ->
                    i.toLong() to l.uppercase()
                },
            if (state.language.isBlank()) -1L else state.languages.indexOf(state.language).toLong(),
        ) { presenter.setLanguage(if (it < 0) "" else state.languages[it.toInt()]) }
    }
    OutlinedTextField(
        value = state.query,
        onValueChange = presenter::setQuery,
        label = { Text(stringResource(R.string.title_filter_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReleaseEmptyView(
    state: ReleasesState,
    presenter: ReleasesPresenter,
) {
    val last = state.page?.lastSearch
    val whenText = last?.at?.let { relativeTime(it) }.orEmpty()
    val (title, body) =
        when (state.empty) {
            ReleaseEmpty.NO_SEARCH ->
                stringResource(R.string.title_releases_no_search) to
                    stringResource(if (state.check) R.string.title_releases_no_search_check else R.string.title_releases_no_search_hint)
            ReleaseEmpty.NOTHING_FOUND ->
                stringResource(R.string.title_releases_nothing) to
                    stringResource(R.string.title_releases_nothing_hint, whenText)
            ReleaseEmpty.DROPPED ->
                stringResource(R.string.title_releases_dropped) to
                    stringResource(
                        R.string.title_releases_dropped_hint,
                        whenText,
                        last?.droppedSources.orEmpty().joinToString(", "),
                        last?.dropped ?: 0,
                    )
            ReleaseEmpty.FAILED ->
                stringResource(R.string.title_releases_failed) to
                    stringResource(R.string.title_releases_failed_hint, whenText)
            ReleaseEmpty.NO_GOOD -> stringResource(R.string.title_releases_no_good) to ""
            ReleaseEmpty.NO_MATCH -> stringResource(R.string.title_releases_no_match) to ""
            ReleaseEmpty.NONE -> "" to ""
        }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.empty == ReleaseEmpty.NO_GOOD) {
            TextButton(onClick = { presenter.setAllTab(true) }) {
                Text(stringResource(R.string.title_releases_show_hidden, state.all.size - state.goodCount))
            }
        }
        if (state.empty ==
            ReleaseEmpty.NO_MATCH
        ) {
            TextButton(onClick = presenter::clearFilters) { Text(stringResource(R.string.title_clear_filters)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleaseRow(
    release: TitleRelease,
    state: ReleasesState,
    presenter: ReleasesPresenter,
    viewModel: TitleViewModel,
) {
    val verdict = TitleLogic.verdict(release)
    val loc = locale()
    var showAll by remember(release.id) { mutableStateOf(false) }
    var menu by remember(release.id) { mutableStateOf(false) }
    val sent = release.id in state.sent || release.state == "chosen"
    val uri = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall),
                verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
            ) {
                StatusChip(verdictText(verdict.kind), verdict.tone)
                if (TitleLogic.recommended(release)) StatusChip(stringResource(R.string.title_recommended), Tone.OK)
                TitleLogic.readTags(release).forEach { StatusChip(it, Tone.IDLE) }
            }
            Text(
                release.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (release.infoUrl.startsWith("http")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.clickable(
                        enabled = release.infoUrl.startsWith("http"),
                    ) { runCatching { uri.openUri(release.infoUrl) } },
            )
            val facts = mutableListOf(formatBytes(release.size, loc))
            release.seeders?.let { facts += pluralStringResource(R.plurals.title_seeders, it, it) }
            if (release.sourceName.isNotBlank()) facts += release.sourceName
            facts += if (release.protocolName == "usenet") "Usenet" else stringResource(R.string.title_torrent)
            if (release.passed) facts += pluralStringResource(R.plurals.title_release_points, release.points, release.points)
            ageText(release.publishedAt)?.let { facts += it }
            Text(
                facts.filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color =
                    if ((release.seeders ?: 5) < 5 &&
                        release.protocolName == "torrent"
                    ) {
                        MaterialTheme.acolyte.failText
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            val reasons = TitleLogic.keyReasons(release.reasons)
            (if (showAll) release.reasons else reasons.take(2)).forEach { reason ->
                Text("· " + reason.text, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (release.reasons.size > 2) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(
                        if (showAll) {
                            stringResource(
                                R.string.title_fewer_reasons,
                            )
                        } else {
                            stringResource(R.string.title_all_reasons, release.reasons.size)
                        },
                    )
                }
            }
            val armed = release.id in state.armed
            if (armed) {
                Text(
                    stringResource(R.string.title_pick_anyway_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.acolyte.waitText,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (sent) {
                    StatusChip(stringResource(R.string.title_sent), Tone.INFO)
                } else {
                    val busy = state.busy == release.id
                    if (armed) {
                        OutlinedButton(onClick = { viewModel.launch { presenter.pick(release) } }, enabled = !busy) {
                            Text(stringResource(R.string.title_pick_anyway))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.launch { presenter.pick(release) } },
                            enabled = !busy,
                        ) { Text(stringResource(R.string.title_pick)) }
                    }
                }
                if (!state.check && release.state != "turned_down") {
                    androidx.compose.foundation.layout
                        .Spacer(Modifier.weight(1f))
                    androidx.compose.foundation.layout.Box {
                        IconButton(
                            onClick = { menu = true },
                        ) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.title_more_actions)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_never_pick)) },
                                onClick = {
                                    menu = false
                                    viewModel.launch { presenter.turnDown(release, false) }
                                },
                            )
                            val group = release.read?.group.orEmpty()
                            if (group.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_block_group, group)) },
                                    onClick = {
                                        menu = false
                                        viewModel.launch { presenter.turnDown(release, true) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ageText(published: String): String? {
    val date = localDate(published) ?: return null
    val days =
        java.time.temporal.ChronoUnit.DAYS
            .between(date, java.time.LocalDate.now())
            .toInt()
    return when {
        days <= 0 -> stringResource(R.string.title_age_today)
        days < 60 -> pluralStringResource(R.plurals.title_age_days, days, days)
        days < 730 -> pluralStringResource(R.plurals.title_age_months, days / 30, days / 30)
        else -> pluralStringResource(R.plurals.title_age_years, days / 365, days / 365)
    }
}
