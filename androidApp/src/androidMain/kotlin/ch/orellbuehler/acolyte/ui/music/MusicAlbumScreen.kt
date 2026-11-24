package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicAlbumPresenter
import ch.orellbuehler.acolyte.feature.music.MusicAlbumState
import ch.orellbuehler.acolyte.feature.music.MusicProblem
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun MusicAlbumScreen(
    mbid: String,
    artist: String,
    nav: MusicNav,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val vm = viewModel(key = "music-album-$mbid-$artist") { PresenterViewModel(sdk.musicAlbum(mbid, artist)) { load() } }
    val presenter = vm.presenter
    val state by presenter.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(vm, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { presenter.reloadQuiet() } }
    MusicNoteEffect(state.note, snackbar)
    ScreenScaffold(title = state.name, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.problem != MusicProblem.NONE) {
                MusicProblemView(state.problem, true, { vm.run { refresh() } })
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = { vm.run { refresh() } }) {
                    AlbumBody(state, nav, vm, snackbar)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun AlbumBody(
    state: MusicAlbumState,
    nav: MusicNav,
    vm: PresenterViewModel<MusicAlbumPresenter>,
    snackbar: SnackbarHostState,
) {
    val sdk = LocalSdk.current
    val server =
        remember {
            sdk.session.account
                ?.serverUrl
                .orEmpty()
                .trimEnd('/')
        }
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        item(key = "hero") { AlbumHero(state, nav, vm) }
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        if (state.gap.isNotEmpty()) item(key = "gap") { AlbumGapNotice(state.gap) }
        item(key = "tracks-header") {
            SectionHeader(stringResource(R.string.music_album_tracks))
            AlbumCounts(state)
        }
        if (state.tracks.isEmpty()) {
            item(key = "tracks-empty") { AlbumEmptyTracks(state) }
        }
        itemsIndexed(state.tracks, key = { index, _ -> "track-$index" }) { index, row ->
            val first = index == 0 || state.tracks[index - 1].disc != row.disc
            AlbumTrackRow(row, state.discs > 1 && first, state, nav, server, snackbar)
        }
        item(key = "footer") { AlbumFooter(state, vm) }
        albumRails(state, nav)
    }
}
