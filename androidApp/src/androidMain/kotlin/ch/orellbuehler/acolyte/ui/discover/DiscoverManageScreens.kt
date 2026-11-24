package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.ExploreStatsState
import ch.orellbuehler.acolyte.feature.discover.HiddenState
import ch.orellbuehler.acolyte.feature.discover.SeriesMovie
import ch.orellbuehler.acolyte.feature.discover.SeriesOutcome
import ch.orellbuehler.acolyte.feature.discover.SeriesState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SeriesViewModel(
    sdk: AcolyteSdk,
    id: String,
) : ViewModel() {
    private val presenter = sdk.movieSeries(id)

    val state: StateFlow<SeriesState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun toggle(id: Long) = presenter.toggle(id)

    fun setDeleteFiles(on: Boolean) = presenter.setDeleteFiles(on)

    fun setNeverAdd(on: Boolean) = presenter.setNeverAdd(on)

    fun setAcknowledge(on: Boolean) = presenter.setAcknowledge(on)

    fun cancelReview() = presenter.cancelReview()

    fun review() {
        viewModelScope.launch { presenter.review() }
    }

    fun apply() {
        viewModelScope.launch { presenter.apply() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    id: String,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "series-$id") { SeriesViewModel(sdk, id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(title = state.name.ifEmpty { stringResource(R.string.discover_series_title) }, onBack = onBack) { padding ->
        StatusContent(
            status = state.status,
            isEmpty = state.isEmpty,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            empty = { EmptyView(icon = Icons.Rounded.Movie) },
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                state.actionError?.let { item(key = "action") { ErrorBanner(it) } }
                item(key = "note") {
                    Text(
                        stringResource(R.string.discover_series_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.Screen),
                    )
                }
                items(state.movies, key = { it.id }) { movie ->
                    SeriesRow(
                        movie,
                        selected = movie.id in state.selected,
                        enabled = state.canManage && state.results == null,
                        onToggle = { viewModel.toggle(movie.id) },
                    )
                }
                val results = state.results
                if (results != null) {
                    item(key = "results") {
                        ResultCard(state, onAgain = viewModel::refresh, onBack = onBack)
                    }
                } else if (state.canManage) {
                    item(key = "choices") { ChoicesCard(state, viewModel) }
                }
            }
        }
    }
    val review = state.review
    if (review != null) {
        ModalBottomSheet(onDismissRequest = viewModel::cancelReview) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.Screen)
                    .padding(bottom = Spacing.XLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                Text(stringResource(R.string.discover_series_confirm_title), style = MaterialTheme.typography.titleLarge)
                review.forEach { row ->
                    Column {
                        Text(movieName(row.movie), style = MaterialTheme.typography.titleSmall)
                        Text(
                            movieCounts(row.movie),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (row.skip.isNotEmpty()) {
                            Text(
                                stringResource(R.string.discover_series_skip, row.skip),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (state.showDetails) {
                            Text(
                                stringResource(
                                    R.string.discover_series_details,
                                    row.movie.versionIds.joinToString(", "),
                                    row.movie.fileIds.joinToString(", "),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End)) {
                    OutlinedButton(onClick = viewModel::cancelReview) { Text(stringResource(R.string.common_cancel)) }
                    Button(
                        onClick = viewModel::apply,
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.discover_series_remove)) }
                }
            }
        }
    }
}

@Composable
private fun movieName(movie: SeriesMovie): String = if (movie.year > 0) "${movie.name} (${movie.year})" else movie.name

@Composable
private fun movieCounts(movie: SeriesMovie): String =
    pluralStringResource(R.plurals.discover_series_versions, movie.versions, movie.versions) + " · " +
        pluralStringResource(R.plurals.discover_series_files, movie.files, movie.files)

@Composable
private fun SeriesRow(
    movie: SeriesMovie,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    ListItem(
        leadingContent = if (enabled) ({ Checkbox(checked = selected, onCheckedChange = { onToggle() }) }) else null,
        headlineContent = { Text(movieName(movie)) },
        supportingContent = { Text(movieCounts(movie)) },
        trailingContent = if (movie.hasProtected) ({ StatusChip(stringResource(R.string.discover_series_protected), Tone.WAIT) }) else null,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun ChoicesCard(
    state: SeriesState,
    viewModel: SeriesViewModel,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.discover_series_review_title), style = MaterialTheme.typography.titleMedium)
            CheckLine(stringResource(R.string.discover_series_delete_files), state.deleteFiles, viewModel::setDeleteFiles)
            CheckLine(stringResource(R.string.discover_series_never_add), state.neverAdd, viewModel::setNeverAdd)
            if (state.needsAcknowledge) {
                CheckLine(stringResource(R.string.discover_series_acknowledge), state.acknowledge, viewModel::setAcknowledge)
            }
            if (!state.neverAdd) {
                Text(
                    stringResource(R.string.discover_series_outside_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = viewModel::review, enabled = state.canReview) {
                Text(stringResource(R.string.discover_series_review, state.selected.size))
            }
        }
    }
}

@Composable
private fun CheckLine(
    text: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ResultCard(
    state: SeriesState,
    onAgain: () -> Unit,
    onBack: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.discover_series_done_title), style = MaterialTheme.typography.titleMedium)
            state.results.orEmpty().forEach { row ->
                Text(
                    when (row.outcome) {
                        SeriesOutcome.REMOVED -> stringResource(R.string.discover_series_removed, row.name)
                        SeriesOutcome.SKIPPED ->
                            stringResource(
                                R.string.discover_series_stayed,
                                row.name,
                                row.why.ifEmpty { stringResource(R.string.discover_series_why_skipped) },
                            )
                        SeriesOutcome.FAILED ->
                            stringResource(
                                R.string.discover_series_failed,
                                row.name,
                                row.why.ifEmpty { stringResource(R.string.discover_series_why_again) },
                            )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Button(onClick = onAgain) { Text(stringResource(R.string.discover_series_review_remaining)) }
                TextButton(onClick = onBack) { Text(stringResource(R.string.discover_series_back)) }
            }
        }
    }
}

class HiddenViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.hidden(-1)

    val state: StateFlow<HiddenState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun showAgain(id: Long) {
        viewModelScope.launch { presenter.showAgain(id) }
    }
}

@Composable
fun HiddenScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { HiddenViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.discover_hidden_title), onBack = onBack) { padding ->
        StatusContent(
            status = state.status,
            isEmpty = state.isEmpty,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            empty = { EmptyView(title = stringResource(R.string.discover_hidden_empty), icon = Icons.Rounded.VisibilityOff) },
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                state.actionError?.let { item(key = "action") { ErrorBanner(it) } }
                items(state.rows, key = { it.id }) { row ->
                    HiddenLine(row, onShowAgain = { viewModel.showAgain(row.id) }, modifier = Modifier.padding(horizontal = Spacing.Screen))
                    HorizontalDivider(Modifier.padding(horizontal = Spacing.Screen))
                }
            }
        }
    }
}

class ExploreStatsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.exploreStats()

    val state: StateFlow<ExploreStatsState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }
}

@Composable
fun ExploreStatsScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { ExploreStatsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.discover_stats_title), onBack = onBack) { padding ->
        StatusContent(
            status = state.status,
            isEmpty = state.isEmpty,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            empty = { EmptyView(title = stringResource(R.string.discover_stats_empty), icon = Icons.Rounded.BarChart) },
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                item(key = "intro") {
                    Text(
                        stringResource(R.string.discover_stats_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.Screen),
                    )
                }
                item(key = "rows-head") {
                    StatHead(stringResource(R.string.discover_stats_row), true)
                }
                items(state.rows, key = { it.rowType + it.title }) { row ->
                    StatLine(row.title.ifEmpty { rowTypeText(row.rowType) }, row.shown, row.opens, row.asks, null)
                }
                if (state.layers.isNotEmpty()) {
                    item(key = "layers-head") {
                        SectionHeader(stringResource(R.string.discover_stats_titles))
                        StatHead(stringResource(R.string.discover_stats_layer), false)
                    }
                    items(state.layers, key = { it.layer }) { layer ->
                        val rate = if (layer.rate < 0) "–" else "%.1f%%".format(layer.rate * 100)
                        StatLine(layerText(layer.layer), layer.shown, layer.opens, layer.asks, rate)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatHead(
    first: String,
    rows: Boolean,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
        Text(first, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        listOf(R.string.discover_stats_shown, R.string.discover_stats_opens, R.string.discover_stats_asks).forEach {
            Text(stringResource(it), Modifier.width(56.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        if (!rows) Text(stringResource(R.string.discover_stats_rate), Modifier.width(56.dp), style = MaterialTheme.typography.labelMedium)
    }
    HorizontalDivider(Modifier.padding(horizontal = Spacing.Screen))
}

@Composable
private fun StatLine(
    name: String,
    shown: Int,
    opens: Int,
    asks: Int,
    rate: String?,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        listOf(shown, opens, asks).forEach { Text(it.toString(), Modifier.width(56.dp), style = MaterialTheme.typography.bodyMedium) }
        if (rate != null) Text(rate, Modifier.width(56.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun rowTypeText(type: String): String =
    when (type) {
        "because_have" -> stringResource(R.string.discover_stats_because_have)
        "because_asked" -> stringResource(R.string.discover_stats_because_asked)
        "person" -> stringResource(R.string.discover_stats_person)
        "genre" -> stringResource(R.string.discover_stats_genre)
        "popular_genre" -> stringResource(R.string.discover_stats_popular_genre)
        "popular_decade" -> stringResource(R.string.discover_stats_popular_decade)
        "decade" -> stringResource(R.string.discover_stats_decade)
        "mood" -> stringResource(R.string.discover_stats_mood)
        "hero" -> stringResource(R.string.discover_stats_hero)
        else -> type
    }

@Composable
private fun layerText(layer: String): String =
    when (layer) {
        "occasion" -> stringResource(R.string.discover_stats_layer_occasion)
        "time" -> stringResource(R.string.discover_stats_layer_time)
        "genre" -> stringResource(R.string.discover_stats_layer_genre)
        "taste" -> stringResource(R.string.discover_stats_layer_taste)
        "reason" -> stringResource(R.string.discover_stats_layer_reason)
        "plain" -> stringResource(R.string.discover_stats_layer_plain)
        else -> layer
    }
