package ch.orellbuehler.acolyte.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.library.LibraryItem
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.feature.library.LibraryLayout
import ch.orellbuehler.acolyte.feature.library.LibraryPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryState
import ch.orellbuehler.acolyte.feature.library.StatusFilter
import ch.orellbuehler.acolyte.nav.LibraryTool
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.PosterCard
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.music.MusicUnmatchedNote
import ch.orellbuehler.acolyte.ui.nav.LibraryDeletedRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryImportRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryToolsRoute
import ch.orellbuehler.acolyte.ui.nav.MusicRoute
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.nav.SearchRoute
import ch.orellbuehler.acolyte.ui.nav.StatsRoute
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

private enum class LibraryDialog {
    REMOVE,
    AUDIO,
    SAVE_FILTER,
    MISSING,
    RULES,
    MOVE,
    CANNOT_OPEN,
}

private typealias LibraryModel = PresenterViewModel<LibraryPresenter>

@Composable
fun LibraryScreen(
    kind: LibraryKind?,
    onBack: (() -> Unit)?,
    open: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val model: LibraryModel = viewModel(key = "library") { PresenterViewModel(sdk.library(kind)) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    WhileVisible { model.presenter.watch() }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }

    BackHandler(enabled = state.selecting) { model.presenter.stopSelecting() }
    BackHandler(enabled = searchOpen && !state.selecting) {
        searchOpen = false
        query = ""
        model.latest("search") { search("") }
    }

    fun openItem(item: LibraryItem) {
        when {
            state.selecting -> model.presenter.toggle(item.id)
            item.kind == LibraryKind.ARTIST -> open(Routes.musicLibraryArtist(item.id))
            item.canOpen -> open(Routes.title(item.kind.wire, item.tmdb))
            else -> dialog = LibraryDialog.CANNOT_OPEN
        }
    }

    fun holdItem(item: LibraryItem) {
        if (!state.canManage) return
        if (!state.selecting) model.run { startSelecting() }
        model.presenter.toggle(item.id)
    }

    ScreenScaffold(
        title = kindLabel(state.kind),
        onBack = onBack,
        actions = { LibraryActions(state, model, open, onSearch = { searchOpen = !searchOpen }, onFilters = { sheet = true }) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                if (searchOpen) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it.take(256)
                            model.latest("search") { search(query) }
                        },
                        placeholder = {
                            Text(
                                stringResource(
                                    if (state.kind ==
                                        LibraryKind.SHOW
                                    ) {
                                        R.string.library_search_show
                                    } else {
                                        R.string.library_search_movie
                                    },
                                ),
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    query = ""
                                    model.latest("search") { search("") }
                                }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.library_search_clear)) }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
                    )
                }
                if (state.kinds.size > 1) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
                        state.kinds.forEachIndexed { index, k ->
                            SegmentedButton(
                                selected = state.kind == k,
                                onClick = {
                                    query = ""
                                    model.presenter.setKind(k)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, state.kinds.size),
                            ) { Text(kindLabel(k), maxLines = 1) }
                        }
                    }
                }
                if (state.kind == LibraryKind.ARTIST) MusicUnmatchedNote(open)
                ChipsRow(state, model, onMissing = { dialog = LibraryDialog.MISSING })
                if (state.searchFailed) NoticeBanner(stringResource(R.string.library_search_failed))
                StatusContent(
                    status = state.status,
                    isEmpty = state.isEmpty,
                    onRefresh = { model.run { refresh() } },
                    modifier = Modifier.weight(1f),
                    empty = { LibraryEmpty(state, open) },
                ) {
                    when {
                        state.nothingMatches -> NothingMatches(state) { model.presenter.clearFilters() }
                        state.layout == LibraryLayout.GRID -> LibraryGrid(state, ::openItem, ::holdItem)
                        else -> LibraryList(state, ::openItem, ::holdItem)
                    }
                }
                if (state.selecting) {
                    SelectBar(state, model, onDialog = { dialog = it })
                }
            }
            NoticeHost(state.notice) { model.presenter.dismissNotice(it) }
        }
    }

    if (sheet) LibraryFilterSheet(state, model, onSave = { dialog = LibraryDialog.SAVE_FILTER }, onDismiss = { sheet = false })
    LibraryDialogs(dialog, state, model) { dialog = null }
}

@Composable
private fun LibraryActions(
    state: LibraryState,
    model: LibraryModel,
    open: (Any) -> Unit,
    onSearch: () -> Unit,
    onFilters: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.library_search)) }
    IconButton(onClick = onFilters) {
        BadgedBox(badge = { if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) } }) {
            Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.library_sort_filter))
        }
    }
    if (state.canManage && state.total > 0) {
        IconButton(onClick = { if (state.selecting) model.presenter.stopSelecting() else model.run { startSelecting() } }) {
            Icon(
                if (state.selecting) Icons.Rounded.Close else Icons.Rounded.Checklist,
                contentDescription = stringResource(if (state.selecting) R.string.library_select_stop else R.string.library_select),
            )
        }
    }
    Box {
        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.library_more)) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val grid = state.layout == LibraryLayout.GRID
            DropdownMenuItem(
                text = { Text(stringResource(if (grid) R.string.library_layout_list else R.string.library_layout_grid)) },
                leadingIcon = {
                    Icon(
                        if (grid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = null,
                    )
                },
                onClick = {
                    menu = false
                    model.run { setLayout(if (grid) LibraryLayout.LIST else LibraryLayout.GRID) }
                },
            )
            if (LibraryTool.DELETED in state.tools) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (state.deletedCount > 0) {
                                stringResource(R.string.library_deleted_count, state.deletedCount)
                            } else {
                                stringResource(R.string.library_deleted)
                            },
                        )
                    },
                    onClick = {
                        menu = false
                        open(LibraryDeletedRoute())
                    },
                )
            }
            if (LibraryTool.STATS in state.tools) {
                DropdownMenuItem(text = { Text(stringResource(R.string.library_stats)) }, onClick = {
                    menu = false
                    open(StatsRoute)
                })
            }
            if (state.tools.isNotEmpty()) {
                DropdownMenuItem(text = { Text(stringResource(R.string.library_tools)) }, onClick = {
                    menu = false
                    open(LibraryToolsRoute)
                })
            }
            if (state.musicEntry) {
                DropdownMenuItem(text = { Text(stringResource(R.string.library_open_music)) }, onClick = {
                    menu = false
                    open(MusicRoute)
                })
            }
            if (state.addKinds.isNotEmpty() || LibraryTool.IMPORT in state.tools) HorizontalDivider()
            state.addKinds.forEach { k ->
                DropdownMenuItem(
                    text = { Text(stringResource(if (k == LibraryKind.SHOW) R.string.library_add_show else R.string.library_add_movie)) },
                    onClick = {
                        menu = false
                        open(SearchRoute)
                    },
                )
            }
            if (LibraryTool.IMPORT in state.tools) {
                DropdownMenuItem(text = { Text(stringResource(R.string.library_add_import)) }, onClick = {
                    menu = false
                    open(LibraryImportRoute)
                })
            }
        }
    }
}

@Composable
private fun ChipsRow(
    state: LibraryState,
    model: LibraryModel,
    onMissing: () -> Unit,
) {
    val showMissing = state.canManage && state.filter.status == StatusFilter.MISSING && state.missingCount > 0
    if (state.chips.isEmpty() && !showMissing && !state.canSearchSubtitles) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.XSmall),
    ) {
        items(state.chips, key = { it.filter.name + it.id }) { chip ->
            val label = chipLabel(chip)
            InputChip(
                selected = true,
                onClick = { model.presenter.removeChip(chip.filter) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.library_chip_remove, label)) },
            )
        }
        if (state.chips.isNotEmpty()) {
            item(
                key = "clear",
            ) { TextButton(onClick = { model.presenter.clearFilters() }) { Text(stringResource(R.string.library_clear_filters)) } }
        }
        if (state.canSearchSubtitles) {
            item(key = "subs") {
                OutlinedButton(onClick = { model.run { searchSubtitles() } }, enabled = !state.working) {
                    Text(stringResource(R.string.library_search_subtitles))
                }
            }
        }
        if (showMissing) {
            item(key = "missing") {
                OutlinedButton(onClick = onMissing, enabled = !state.working) {
                    Text(
                        stringResource(
                            if (state.kind ==
                                LibraryKind.SHOW
                            ) {
                                R.string.library_search_missing_show
                            } else {
                                R.string.library_search_missing_movie
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryEmpty(
    state: LibraryState,
    open: (Any) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.library_empty_title), style = MaterialTheme.typography.titleMedium)
        MutedText(stringResource(R.string.library_empty_message))
        if (state.kind in state.addKinds) {
            Button(onClick = { open(SearchRoute) }) {
                Text(stringResource(if (state.kind == LibraryKind.SHOW) R.string.library_add_show else R.string.library_add_movie))
            }
        }
        if (state.canOpenFolders) {
            OutlinedButton(onClick = { open(Routes.settingsLibraryFolders) }) { Text(stringResource(R.string.library_empty_folders)) }
        }
    }
}

@Composable
private fun NothingMatches(
    state: LibraryState,
    onClear: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val query = state.query.trim()
        Text(
            if (query.isNotEmpty() && !state.filter.isActive) {
                stringResource(R.string.library_nothing_for, query)
            } else {
                stringResource(R.string.library_nothing_matches)
            },
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = onClear) { Text(stringResource(R.string.library_clear_filters)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryList(
    state: LibraryState,
    onOpen: (LibraryItem) -> Unit,
    onHold: (LibraryItem) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        if (state.grouped) {
            state.groups.forEach { group ->
                groupHeader(group.id, group.name, group.items.size)
                items(group.items, key = { "${group.id}-${it.id}" }) { LibraryRow(it, state, onOpen, onHold) }
            }
        } else {
            items(state.items, key = { it.id }) { LibraryRow(it, state, onOpen, onHold) }
        }
    }
}

private fun LazyListScope.groupHeader(
    id: Long,
    name: String,
    count: Int,
) {
    item(key = "group-$id") { GroupHeader(name, count) }
}

@Composable
private fun GroupHeader(
    name: String,
    count: Int,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
        Text(name.ifEmpty { stringResource(R.string.library_group_none) }, style = MaterialTheme.typography.titleMedium)
        MutedText(pluralStringResource(R.plurals.library_group_count, count, count))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryRow(
    item: LibraryItem,
    state: LibraryState,
    onOpen: (LibraryItem) -> Unit,
    onHold: (LibraryItem) -> Unit,
) {
    val selected = item.id in state.selected
    ListItem(
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.selecting) Checkbox(checked = selected, onCheckedChange = { onOpen(item) })
                PosterThumb(item.poster, item.name)
            }
        },
        headlineContent = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall), modifier = Modifier.padding(top = Spacing.XSmall)) {
                item.state?.let { StatusChip(stateLabel(it), it.tone) }
                item.fact?.let { MutedText(factText(it)) }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (item.year > 0) MutedText(item.year.toString())
                WatchedMark(item)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.combinedClickable(onClick = { onOpen(item) }, onLongClick = { onHold(item) }),
    )
}

@Composable
private fun WatchedMark(item: LibraryItem) {
    if (item.watchedOf <= 0 || item.watchedSeen <= 0) return
    val text =
        if (item.watchedSeen >= item.watchedOf) {
            stringResource(R.string.library_watched)
        } else {
            stringResource(R.string.library_watched_part, item.watchedSeen, item.watchedOf)
        }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Icon(Icons.Rounded.Visibility, contentDescription = null, tint = MaterialTheme.acolyte.okText, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.acolyte.okText)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGrid(
    state: LibraryState,
    onOpen: (LibraryItem) -> Unit,
    onHold: (LibraryItem) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val gap = Spacing.Medium
        val columns = ((maxWidth - Spacing.Screen * 2 + gap) / (112.dp + gap)).toInt().coerceAtLeast(2)
        val width = (maxWidth - Spacing.Screen * 2 - gap * (columns - 1)) / columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = Spacing.Screen, end = Spacing.Screen, bottom = Spacing.XLarge),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.grouped) {
                state.groups.forEach { group ->
                    item(key = "group-${group.id}", span = { GridItemSpan(maxLineSpan) }) { GroupHeader(group.name, group.items.size) }
                    posters(group.items, "${group.id}", state, width, onOpen, onHold)
                }
            } else {
                posters(state.items, "", state, width, onOpen, onHold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyGridScope.posters(
    list: List<LibraryItem>,
    prefix: String,
    state: LibraryState,
    width: Dp,
    onOpen: (LibraryItem) -> Unit,
    onHold: (LibraryItem) -> Unit,
) {
    items(list, key = { "$prefix-${it.id}" }) { item ->
        val selected = item.id in state.selected
        val itemState = item.state
        PosterCard(
            title = item.name,
            image = item.poster,
            subtitle = if (item.year > 0) item.year.toString() else null,
            width = width,
            progress = if (item.kind != LibraryKind.MOVIE && item.total > 1) item.share else null,
            badges = {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                    if (state.selecting) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.acolyte.onArt,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    if (state.showsPosterState && itemState != null && itemState.isProblem) {
                        StatusChip(stateLabel(itemState), itemState.tone, onArt = true)
                    }
                    if (item.watchedOf > 0 && item.watchedSeen >= item.watchedOf) {
                        Surface(shape = CircleShape, color = MaterialTheme.acolyte.artScrim) {
                            Icon(
                                Icons.Rounded.Visibility,
                                contentDescription = stringResource(R.string.library_watched),
                                tint = MaterialTheme.acolyte.onArt,
                                modifier = Modifier.padding(4.dp).size(14.dp),
                            )
                        }
                    }
                }
            },
            modifier = Modifier.combinedClickable(onClick = { onOpen(item) }, onLongClick = { onHold(item) }),
        )
    }
}

@Composable
private fun SelectBar(
    state: LibraryState,
    model: LibraryModel,
    onDialog: (LibraryDialog) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val count = state.selectedCount
    val all = count == state.items.size && count > 0
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.Small, vertical = Spacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { model.presenter.stopSelecting() }) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.library_select_stop))
            }
            Text(
                pluralStringResource(R.plurals.library_selected, count, count),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            TextButton(onClick = { if (all) model.presenter.selectNone() else model.presenter.selectAll() }) {
                Text(stringResource(if (all) R.string.library_select_none else R.string.library_select_all), maxLines = 1)
            }
            Box {
                IconButton(onClick = { menu = true }, enabled = count > 0 && !state.working) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.library_actions))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    fun pick(action: () -> Unit) {
                        menu = false
                        action()
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.library_action_search)) }, onClick = {
                        pick { model.run { searchSelected() } }
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.library_action_refresh)) }, onClick = {
                        pick { model.run { refreshSelected() } }
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.library_action_keep)) }, onClick = {
                        pick { model.run { keepSelected(true) } }
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.library_action_unkeep)) }, onClick = {
                        pick { model.run { keepSelected(false) } }
                    })
                    if (state.ruleOptions.isNotEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.library_action_quality)) }, onClick = {
                            pick { onDialog(LibraryDialog.RULES) }
                        })
                    }
                    if (state.canSetAudio) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.library_action_audio)) }, onClick = {
                            pick { onDialog(LibraryDialog.AUDIO) }
                        })
                    }
                    if (state.moveOptions.isNotEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.library_action_move)) }, onClick = {
                            pick { onDialog(LibraryDialog.MOVE) }
                        })
                    }
                    if (state.canFindCopies) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.library_action_copies)) }, onClick = {
                            pick { model.run { reviewCopies() } }
                        })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_action_remove), color = MaterialTheme.colorScheme.error) },
                        onClick = { pick { onDialog(LibraryDialog.REMOVE) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryDialogs(
    dialog: LibraryDialog?,
    state: LibraryState,
    model: LibraryModel,
    onDismiss: () -> Unit,
) {
    val count = state.selectedCount
    when (dialog) {
        LibraryDialog.REMOVE ->
            ConfirmDialog(
                title = pluralStringResource(R.plurals.library_remove_title, count, count),
                text = stringResource(R.string.library_remove_text),
                confirm = stringResource(R.string.library_action_remove),
                danger = true,
                onConfirm = { model.run { removeSelected() } },
                onDismiss = onDismiss,
            )
        LibraryDialog.AUDIO ->
            TextInputDialog(
                title = pluralStringResource(R.plurals.library_audio_title, count, count),
                label = stringResource(R.string.library_audio_field),
                hint = stringResource(R.string.library_audio_hint),
                confirm = stringResource(R.string.library_audio_set),
                allowEmpty = true,
                onConfirm = { text -> model.run { setAudio(text) } },
                onDismiss = onDismiss,
            )
        LibraryDialog.SAVE_FILTER ->
            TextInputDialog(
                title = stringResource(R.string.library_saved_save),
                label = stringResource(R.string.library_saved_name),
                confirm = stringResource(R.string.library_save),
                maxLength = 60,
                onConfirm = { model.presenter.saveFilter(it) },
                onDismiss = onDismiss,
            )
        LibraryDialog.MISSING ->
            ConfirmDialog(
                title = pluralStringResource(R.plurals.library_search_missing_title, state.missingCount, state.missingCount),
                text = stringResource(R.string.library_search_missing_note),
                confirm = stringResource(R.string.library_search),
                onConfirm = { model.run { searchAllMissing() } },
                onDismiss = onDismiss,
            )
        LibraryDialog.RULES ->
            OptionDialog(stringResource(R.string.library_action_quality), state.ruleOptions.map { it.id to it.label }, onDismiss) { id ->
                model.run { setRules(id) }
            }
        LibraryDialog.MOVE ->
            OptionDialog(stringResource(R.string.library_action_move), state.moveOptions.map { it.id to it.label }, onDismiss) { id ->
                model.run { moveSelected(id) }
            }
        LibraryDialog.CANNOT_OPEN ->
            AlertDialog(
                onDismissRequest = onDismiss,
                text = { Text(stringResource(R.string.library_cannot_open)) },
                confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_close)) } },
            )
        null -> Unit
    }
    val review = state.copiesReview
    if (review != null) {
        AlertDialog(
            onDismissRequest = { model.presenter.cancelCopies() },
            title = { Text(pluralStringResource(R.plurals.library_copies_title, review.items.size, review.items.size)) },
            text = {
                LazyColumn(Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    item { MutedText(pluralStringResource(R.plurals.library_copies_note, review.slotCount, review.slotCount)) }
                    items(review.items) { copy ->
                        Column {
                            Text(copy.title, style = MaterialTheme.typography.titleSmall)
                            MutedText(stringResource(R.string.library_copy_line, copy.version, codecLabel(copy.codec)))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { model.run { startCopies() } },
                    enabled = !state.working,
                ) { Text(stringResource(R.string.library_search)) }
            },
            dismissButton = { TextButton(onClick = { model.presenter.cancelCopies() }) { Text(stringResource(R.string.library_cancel)) } },
        )
    }
}

@Composable
fun OptionDialog(
    title: String,
    options: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.height(320.dp)) {
                items(options, key = { it.first }) { (id, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier =
                            Modifier.clickable {
                                onDismiss()
                                onPick(id)
                            },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) } },
    )
}
