package ch.orellbuehler.acolyte.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.WatchedTitle
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.library.LibraryStatsPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryStatsState
import ch.orellbuehler.acolyte.feature.library.StatsFact
import ch.orellbuehler.acolyte.feature.library.StatsKind
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorView
import ch.orellbuehler.acolyte.ui.components.LoadingView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing

private typealias StatsModel = PresenterViewModel<LibraryStatsPresenter>

@Composable
fun LibraryStatsScreen(onBack: (() -> Unit)?) {
    val sdk = LocalSdk.current
    val model: StatsModel = viewModel(key = "library-stats") { PresenterViewModel(sdk.libraryStats()) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var removing by remember { mutableStateOf<WatchedTitle?>(null) }

    ScreenScaffold(title = stringResource(R.string.library_stats), onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
                    listOf(R.string.library_stats_tab_files, R.string.library_stats_tab_watched).forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = tab == index,
                            onClick = { tab = index },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                        ) { Text(stringResource(label), maxLines = 1) }
                    }
                }
                if (tab == 0) {
                    StatusContent(
                        status = state.status,
                        isEmpty = state.isEmpty,
                        onRefresh = { model.run { refresh() } },
                        modifier = Modifier.weight(1f),
                        empty = {
                            EmptyView(
                                title = stringResource(R.string.library_stats_empty_title),
                                message = stringResource(R.string.library_stats_empty_message),
                                icon = Icons.Rounded.BarChart,
                            )
                        },
                    ) { FilesTab(state, model) }
                } else {
                    Box(Modifier.weight(1f)) { WatchedTab(state, model) { removing = it } }
                }
            }
            NoticeHost(state.notice) { model.presenter.dismissNotice(it) }
        }
    }
    removing?.let { title ->
        RemoveWatchedDialog(title, onDismiss = { removing = null }) { files, never ->
            model.run { removeWatched(title.id, files, never) }
        }
    }
}

@Composable
private fun FilesTab(
    state: LibraryStatsState,
    model: StatsModel,
) {
    val current = state.current ?: return
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        if (state.kinds.size > 1) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen)) {
                    state.kinds.forEachIndexed { index, kind ->
                        SegmentedButton(
                            selected = current.kind == kind.kind,
                            onClick = { model.presenter.setKind(kind.kind) },
                            shape = SegmentedButtonDefaults.itemShape(index, state.kinds.size),
                        ) { Text(statsKindLabel(kind.kind), maxLines = 1) }
                    }
                }
            }
        }
        item { Tiles(current) }
        items(current.facts, key = { it.name }) { fact -> FactCard(fact, current) }
    }
}

@Composable
private fun statsKindLabel(kind: String): String =
    stringResource(
        when (kind) {
            "shows" -> R.string.library_stats_kind_shows
            "music" -> R.string.library_stats_kind_music
            else -> R.string.library_stats_kind_movies
        },
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tiles(kind: StatsKind) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        maxItemsInEachRow = 2,
    ) {
        Tile(stringResource(R.string.library_stats_titles), kind.titles.toString(), Modifier.weight(1f))
        Tile(stringResource(R.string.library_stats_files), kind.files.toString(), Modifier.weight(1f))
        Tile(stringResource(R.string.library_stats_on_disk), LibraryFormat.bytes(kind.bytes), Modifier.weight(1f))
        Tile(stringResource(R.string.library_stats_unchecked), kind.unchecked.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun Tile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = modifier) {
        Column(Modifier.padding(Spacing.Large)) {
            MutedText(label)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun FactCard(
    fact: StatsFact,
    kind: StatsKind,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(factName(fact.name), style = MaterialTheme.typography.titleSmall)
            fact.buckets.forEach { bucket ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            bucketName(fact.name, bucket.value),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        MutedText(stringResource(R.string.library_stats_bucket_line, bucket.files, LibraryFormat.bytes(bucket.bytes)))
                    }
                    LinearProgressIndicator(progress = { bucket.share }, modifier = Modifier.fillMaxWidth())
                    bucketHint(bucket.value)?.let { MutedText(it) }
                }
            }
            if (kind.files == 0) MutedText(stringResource(R.string.library_stats_empty_message))
        }
    }
}

@Composable
private fun factName(name: String): String =
    stringResource(
        when (name) {
            "video_codec" -> R.string.library_stats_fact_video_codec
            "hdr" -> R.string.library_stats_fact_hdr
            "audio_codec" -> R.string.library_stats_fact_audio_codec
            "audio_channels" -> R.string.library_stats_fact_audio_channels
            "container" -> R.string.library_stats_fact_container
            else -> R.string.library_stats_fact_resolution
        },
    )

@Composable
private fun bucketHint(value: String): String? =
    when (value) {
        "unknown" -> stringResource(R.string.library_stats_value_unknown_hint)
        "unsupported" -> stringResource(R.string.library_stats_value_unsupported_hint)
        else -> null
    }

@Composable
private fun bucketName(
    fact: String,
    value: String,
): String =
    when (value) {
        "other" -> stringResource(R.string.library_stats_value_other)
        "unchecked" -> stringResource(R.string.library_stats_value_unchecked)
        "unknown" -> stringResource(R.string.library_stats_value_unknown)
        "unsupported" -> stringResource(R.string.library_stats_value_unsupported)
        else -> StatsNames.of(fact, value)
    }

internal object StatsNames {
    private val video =
        mapOf(
            "h264" to "AVC (H.264)",
            "hevc" to "HEVC (H.265)",
            "av1" to "AV1",
            "vp9" to "VP9",
            "mpeg2" to "MPEG-2",
            "xvid" to "Xvid",
            "vc1" to "VC-1",
        )
    private val hdr =
        mapOf("hdr10" to "HDR10", "hlg" to "HLG", "dv" to "Dolby Vision", "dv+hdr10" to "Dolby Vision + HDR10", "sdr" to "SDR")
    private val container = mapOf("matroska" to "MKV", "webm" to "WebM", "mp4" to "MP4")
    private val audio = mapOf("truehd" to "TrueHD", "wavpack" to "WavPack", "vorbis" to "Vorbis", "opus" to "Opus")

    fun of(
        fact: String,
        value: String,
    ): String =
        when (fact) {
            "video_codec" -> video[value]
            "hdr" -> hdr[value]
            "container" -> container[value]
            "audio_codec" -> audio[value]
            else -> value
        } ?: value.uppercase()
}

@Composable
private fun WatchedTab(
    state: LibraryStatsState,
    model: StatsModel,
    onRemove: (WatchedTitle) -> Unit,
) {
    val status = state.watchedStatus
    val error = status.error
    when {
        status.phase == Phase.LOADING -> LoadingView()
        status.phase == Phase.FAILED && error != null -> ErrorView(error, onRetry = { model.run { refresh() } })
        !state.watched.enabled ->
            EmptyView(
                title = stringResource(R.string.library_stats_watch_off),
                message = stringResource(R.string.library_stats_watch_off_owner),
            )
        state.watched.readAt.isEmpty() ->
            EmptyView(title = stringResource(R.string.library_stats_watch_on), message = stringResource(R.string.library_stats_watch_first))
        else -> WatchedList(state, model, onRemove)
    }
}

@Composable
private fun WatchedList(
    state: LibraryStatsState,
    model: StatsModel,
    onRemove: (WatchedTitle) -> Unit,
) {
    val watched = state.watched
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        item {
            Column(
                Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
            ) {
                MutedText(stringResource(R.string.library_stats_watch_help))
                MutedText(stringResource(R.string.library_stats_watch_read, LibraryFormat.ago(watched.readAt)))
                if (watched.people.isEmpty()) MutedText(stringResource(R.string.library_stats_watch_nobody))
            }
        }
        items(watched.people, key = { it.server + it.name }) { person ->
            val name = if (person.linked && person.userName.isNotEmpty()) person.userName else person.name
            ListItem(
                headlineContent = { Text(name) },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                        MutedText(
                            if (person.linked &&
                                person.name != name
                            ) {
                                stringResource(R.string.library_stats_person_server, person.server, person.name)
                            } else {
                                person.server
                            },
                        )
                        MutedText(
                            listOf(
                                pluralStringResource(R.plurals.library_stats_movies, person.movies, person.movies),
                                pluralStringResource(R.plurals.library_stats_episodes, person.episodes, person.episodes),
                                pluralStringResource(R.plurals.library_stats_plays, person.plays, person.plays),
                            ).joinToString(" · "),
                        )
                        if (person.lastWatchedAt.isNotEmpty()) {
                            MutedText(stringResource(R.string.library_stats_last_watched, LibraryFormat.ago(person.lastWatchedAt)))
                        }
                    }
                },
                trailingContent = { if (!person.linked) StatusChip(stringResource(R.string.library_stats_not_linked), Tone.IDLE) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
        val cleanup = state.cleanup
        if (state.canClean && cleanup != null && watched.people.isNotEmpty()) {
            item { CleanupCard(state, model) }
            items(cleanup.found, key = { "found-${it.id}" }) { title ->
                ListItem(
                    headlineContent = {
                        Text(if (title.year > 0) stringResource(R.string.library_stats_name_year, title.name, title.year) else title.name)
                    },
                    supportingContent = {
                        MutedText(
                            stringResource(
                                R.string.library_stats_cleanup_line,
                                LibraryFormat.ago(title.lastWatchedAt),
                                LibraryFormat.bytes(title.bytes),
                            ),
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { onRemove(title) }, enabled = state.removing != title.id) {
                            Text(stringResource(R.string.library_stats_cleanup_remove), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CleanupCard(
    state: LibraryStatsState,
    model: StatsModel,
) {
    val cleanup = state.cleanup ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.library_stats_cleanup_title), style = MaterialTheme.typography.titleSmall)
            MutedText(stringResource(R.string.library_stats_cleanup_about))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.library_stats_cleanup_days), modifier = Modifier.weight(1f))
                IconButton(onClick = { model.run { setCleanupDays(state.cleanupDays - DAY_STEP) } }, enabled = state.cleanupDays > 0) {
                    Icon(Icons.Rounded.Remove, contentDescription = null)
                }
                Text(state.cleanupDays.toString(), modifier = Modifier.widthIn(min = 32.dp))
                IconButton(onClick = { model.run { setCleanupDays(state.cleanupDays + DAY_STEP) } }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }
            Text(stringResource(R.string.library_stats_cleanup_wait), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                cleanup.people.forEach { person ->
                    FilterChip(
                        selected = person.key in state.cleanupPicked,
                        onClick = { model.run { toggleCleanupPerson(person.key) } },
                        label = { Text(stringResource(R.string.library_stats_person_server, person.name, person.server), maxLines = 1) },
                    )
                }
            }
            val bytes = cleanup.found.sumOf { it.bytes }
            MutedText(
                when {
                    state.cleanupPicked.isEmpty() -> stringResource(R.string.library_stats_cleanup_pick)
                    cleanup.found.isEmpty() -> stringResource(R.string.library_stats_cleanup_none, state.cleanupDays)
                    else ->
                        pluralStringResource(
                            R.plurals.library_stats_cleanup_found,
                            cleanup.found.size,
                            cleanup.found.size,
                            LibraryFormat.bytes(bytes),
                        )
                },
            )
        }
    }
}

@Composable
private fun RemoveWatchedDialog(
    title: WatchedTitle,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit,
) {
    var files by remember { mutableStateOf(false) }
    var never by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_stats_remove_title, title.name)) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.library_stats_remove_files)) },
                    supportingContent = { Text(stringResource(R.string.library_stats_remove_files_hint)) },
                    trailingContent = { Switch(checked = files, onCheckedChange = { files = it }) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.library_stats_remove_never)) },
                    trailingContent = { Checkbox(checked = never, onCheckedChange = { never = it }) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm(files, never)
            }) { Text(stringResource(R.string.library_stats_remove_confirm), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) } },
    )
}

private const val DAY_STEP = 5
