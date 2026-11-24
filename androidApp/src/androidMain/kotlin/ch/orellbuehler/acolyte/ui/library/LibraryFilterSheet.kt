package ch.orellbuehler.acolyte.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.library.FilterOption
import ch.orellbuehler.acolyte.feature.library.LibraryPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryState
import ch.orellbuehler.acolyte.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryFilterSheet(
    state: LibraryState,
    model: PresenterViewModel<LibraryPresenter>,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val presenter = model.presenter
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
            item { SheetHeading(stringResource(R.string.library_sort_title)) }
            state.sorts.forEach { sort ->
                item(key = "sort-${sort.name}") {
                    val current = state.sort == sort
                    ListItem(
                        headlineContent = { Text(sortLabel(sort, state.kind)) },
                        trailingContent = {
                            if (current) {
                                Icon(
                                    if (state.descending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                    contentDescription =
                                        stringResource(
                                            if (state.descending) R.string.library_sort_descending else R.string.library_sort_ascending,
                                        ),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clickable { presenter.setSort(sort) },
                    )
                }
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
                    SegmentedButton(
                        selected = !state.descending,
                        onClick = { presenter.setDescending(false) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(R.string.library_sort_ascending), maxLines = 1) }
                    SegmentedButton(
                        selected = state.descending,
                        onClick = { presenter.setDescending(true) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(R.string.library_sort_descending), maxLines = 1) }
                }
            }
            if (state.canGroup) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.library_group_series)) },
                        trailingContent = { Switch(checked = state.grouped, onCheckedChange = { presenter.setGrouped(it) }) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    )
                }
            }
            item { SheetHeading(stringResource(R.string.library_filter_title)) }
            item { ChipGroup(state.statusOptions, { statusFilterLabel(it.id) }) { presenter.setStatus(it.id) } }
            item { SheetHeading(stringResource(R.string.library_filter_kept_title)) }
            item {
                ChipGroup(state.keptOptions, {
                    stringResource(if (it.id == "on") R.string.library_filter_kept_on else R.string.library_filter_kept_off)
                }) { presenter.toggleKept(it.id) }
            }
            if (state.askerOptions.size > 2 || state.filter.asker != "anyone") {
                item { SheetHeading(stringResource(R.string.library_filter_asker_title)) }
                item {
                    ChipGroup(state.askerOptions, {
                        when (it.id) {
                            "anyone" -> stringResource(R.string.library_filter_anyone)
                            "me" -> stringResource(R.string.library_filter_me)
                            else -> it.label
                        }
                    }) { presenter.setAsker(it.id) }
                }
            }
            if (state.codecOptions.isNotEmpty()) {
                item { SheetHeading(stringResource(R.string.library_filter_codec_title)) }
                item { ChipGroup(state.codecOptions, { codecLabel(it.id) }) { presenter.setCodec(it.id) } }
            }
            state.subtitlesOption?.let { option ->
                item { SheetHeading(stringResource(R.string.library_filter_subs_title)) }
                item { ChipGroup(listOf(option), { stringResource(R.string.library_filter_subs_missing) }) { presenter.toggleSubtitles() } }
            }
            state.watchedOption?.let { option ->
                item { SheetHeading(stringResource(R.string.library_filter_watched_title)) }
                item { ChipGroup(listOf(option), { stringResource(R.string.library_filter_watched_anyone) }) { presenter.toggleWatched() } }
            }
            if (state.seriesOptions.isNotEmpty()) {
                item { SheetHeading(stringResource(R.string.library_filter_series_title)) }
                item { SeriesPicker(state.seriesOptions) { presenter.setSeries(it) } }
            }
            item { SheetHeading(stringResource(R.string.library_saved_title)) }
            item { SavedFilters(state, model, onSave) }
            item {
                Box(Modifier.fillMaxWidth().padding(Spacing.Screen), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = { presenter.clearFilters() }, enabled = state.filter.isActive || state.chips.isNotEmpty()) {
                        Text(stringResource(R.string.library_clear_filters))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.padding(start = Spacing.Screen, end = Spacing.Screen, top = Spacing.Large, bottom = Spacing.XSmall).semantics {
                heading()
            },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    options: List<FilterOption>,
    label: @Composable (FilterOption) -> String,
    onPick: (FilterOption) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        options.forEach { option ->
            val text = label(option)
            FilterChip(
                selected = option.selected,
                onClick = { onPick(option) },
                label = { Text(if (option.count >= 0) "$text  ${option.count}" else text, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun SeriesPicker(
    options: List<FilterOption>,
    onPick: (Long) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.selected } ?: options.first()
    Box(Modifier.padding(horizontal = Spacing.Screen)) {
        OutlinedButton(onClick = { open = true }) {
            Text(if (current.id == "0") stringResource(R.string.library_filter_every_movie) else current.label, maxLines = 1)
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (option.id == "0") {
                                stringResource(R.string.library_filter_every_movie)
                            } else {
                                "${option.label}  ${option.count.coerceAtLeast(0)}"
                            },
                        )
                    },
                    onClick = {
                        open = false
                        onPick(option.id.toLongOrNull() ?: 0)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedFilters(
    state: LibraryState,
    model: PresenterViewModel<LibraryPresenter>,
    onSave: () -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        state.savedFilters.forEach { saved ->
            InputChip(
                selected = saved.active,
                onClick = { model.presenter.applySavedFilter(saved.name) },
                label = { Text(saved.name, maxLines = 1) },
                leadingIcon = { Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    IconButton(onClick = { model.presenter.deleteSavedFilter(saved.name) }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.library_saved_delete, saved.name),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
            )
        }
    }
    Row(Modifier.padding(horizontal = Spacing.Small)) {
        TextButton(onClick = onSave, enabled = state.canSaveFilter) { Text(stringResource(R.string.library_saved_save)) }
    }
}
