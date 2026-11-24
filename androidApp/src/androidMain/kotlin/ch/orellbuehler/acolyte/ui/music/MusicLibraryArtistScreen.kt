package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.music.MusicArtistPresenter
import ch.orellbuehler.acolyte.feature.music.MusicLibraryArtistPresenter
import ch.orellbuehler.acolyte.feature.music.MusicLibraryArtistState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.LoadingView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.library.WhileVisible
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.title.LocalTitleSnackbar
import ch.orellbuehler.acolyte.ui.title.RemoveContent
import ch.orellbuehler.acolyte.ui.title.RenameContent
import kotlinx.coroutines.launch

class MusicLibraryArtistViewModel(
    private val sdk: AcolyteSdk,
    val titleId: Long,
) : ViewModel() {
    val presenter: MusicLibraryArtistPresenter = sdk.musicLibraryArtist(titleId, artistUtcOffset())
    val leftovers by lazy { sdk.musicLeftovers(titleId) }
    val rename by lazy { sdk.musicRename(titleId) }
    val remove by lazy { sdk.musicRemove(titleId, presenter.state.value.name, 0) }

    var artist by mutableStateOf<MusicArtistPresenter?>(null)
        private set

    init {
        viewModelScope.launch {
            presenter.load()
            attach()
        }
    }

    private fun attach() {
        val mbid = presenter.state.value.mbid
        if (artist != null || mbid.isEmpty()) return
        artist = sdk.musicArtist(mbid, artistUtcOffset()).also { viewModelScope.launch { it.load() } }
    }

    fun refresh() {
        viewModelScope.launch {
            presenter.refresh()
            attach()
            artist?.refresh()
        }
    }

    fun reloadQuiet() {
        viewModelScope.launch {
            presenter.reloadQuiet()
            attach()
            artist?.reloadQuiet()
        }
    }

    fun run(block: suspend MusicLibraryArtistPresenter.() -> Unit) {
        viewModelScope.launch { presenter.block() }
    }

    fun runArtist(block: suspend MusicArtistPresenter.() -> Unit) {
        val target = artist ?: return
        viewModelScope.launch { target.block() }
    }

    fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

@Composable
fun MusicLibraryArtistScreen(
    titleId: Long,
    tab: String,
    nav: MusicNav,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "music-library-artist-$titleId") { MusicLibraryArtistViewModel(sdk, titleId) }
    val state by viewModel.presenter.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tabKey by rememberSaveable { mutableStateOf(tab) }
    var sheet by rememberSaveable { mutableStateOf("") }
    var moveFolder by rememberSaveable { mutableLongStateOf(0L) }
    MusicNoteEffect(state.note, snackbar)
    WhileVisible { viewModel.presenter.watch() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.reloadQuiet() }
    LaunchedEffect(state.removed) { if (state.removed) nav.back() }
    val libraryTab = state.canManage && tabKey == "library"
    ScreenScaffold(title = state.name, onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.status.phase == Phase.FAILED) {
                MusicProblemView(state.problem, false, onRetry = viewModel::refresh)
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    Column(Modifier.fillMaxSize()) {
                        if (state.canManage) {
                            PrimaryTabRow(selectedTabIndex = if (libraryTab) 1 else 0) {
                                Tab(
                                    selected = !libraryTab,
                                    onClick = { tabKey = "overview" },
                                    text = { Text(stringResource(R.string.music_artist_tab_overview)) },
                                )
                                Tab(
                                    selected = libraryTab,
                                    onClick = { tabKey = "library" },
                                    text = { Text(stringResource(R.string.music_artist_tab_library)) },
                                )
                            }
                        }
                        if (libraryTab) {
                            val actions =
                                MusicLibraryActions(
                                    nav = nav,
                                    presenter = viewModel.presenter,
                                    act = viewModel::run,
                                    openSheet = { sheet = it },
                                    askMove = { moveFolder = it },
                                )
                            MusicLibraryTab(state, actions)
                        } else {
                            MusicLibraryOverview(viewModel, state, nav, snackbar) { tabKey = "library" }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    LibrarySheets(viewModel, state, sheet, { sheet = "" })
    if (moveFolder != 0L) {
        val folder = state.folders.firstOrNull { it.id == moveFolder }
        AlertDialog(
            onDismissRequest = { moveFolder = 0L },
            title = { Text(stringResource(R.string.music_lib_move_title, folder?.label.orEmpty())) },
            text = { Text(stringResource(R.string.music_lib_move_body) + "\n" + folder?.path.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    val id = moveFolder
                    moveFolder = 0L
                    viewModel.run { moveTo(id) }
                }) { Text(stringResource(R.string.music_lib_move_confirm)) }
            },
            dismissButton = { TextButton(onClick = { moveFolder = 0L }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun LibrarySheets(
    viewModel: MusicLibraryArtistViewModel,
    state: MusicLibraryArtistState,
    sheet: String,
    close: () -> Unit,
) {
    when (sheet) {
        "kinds" ->
            MusicKindsSheet(
                state,
                onSave = { newAlbums, types, secondary, statuses -> viewModel.presenter.saveKinds(newAlbums, types, secondary, statuses) },
                onClose = close,
            )
        "rename" ->
            LibraryToolSheet(onDismiss = {
                close()
                viewModel.reloadQuiet()
            }) { RenameContent(viewModel.rename, state.name, viewModel::launch) }
        "remove" -> {
            val removeState by viewModel.remove.state.collectAsStateWithLifecycle()
            val finish = {
                close()
                if (removeState.done) viewModel.presenter.removed()
            }
            LibraryToolSheet(onDismiss = finish) { RemoveContent(viewModel.remove, viewModel::launch, finish) }
        }
        "unmatched" ->
            MusicUnmatchedSheet(viewModel.leftovers) {
                close()
                viewModel.reloadQuiet()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryToolSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val host = remember { SnackbarHostState() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        CompositionLocalProvider(LocalTitleSnackbar provides host) {
            Box {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = Spacing.XLarge)
                        .navigationBarsPadding(),
                ) { content() }
                SnackbarHost(host, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun MusicLibraryOverview(
    viewModel: MusicLibraryArtistViewModel,
    state: MusicLibraryArtistState,
    nav: MusicNav,
    snackbar: SnackbarHostState,
    onOpenLibrary: () -> Unit,
) {
    val artist = viewModel.artist
    if (artist == null) {
        if (state.mbid.isEmpty()) EmptyView() else LoadingView()
        return
    }
    val artistState by artist.state.collectAsStateWithLifecycle()
    var about by rememberSaveable { mutableStateOf(false) }
    MusicNoteEffect(artistState.note, snackbar)
    val actions = rememberArtistActions(artist, viewModel::runArtist, nav) { about = true }
    when (artistState.status.phase) {
        Phase.LOADING -> LoadingView()
        Phase.FAILED -> MusicProblemView(artistState.problem, false, onRetry = { viewModel.runArtist { refresh() } })
        Phase.READY ->
            LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                musicArtistBody(artistState, actions, MusicArtistEmbed(state, onOpenLibrary))
                musicLibraryAsks(state)
            }
    }
    if (about) MusicArtistAboutSheet(artistState) { about = false }
}
