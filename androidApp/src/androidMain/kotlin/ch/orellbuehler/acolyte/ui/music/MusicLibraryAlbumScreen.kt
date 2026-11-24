package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicLibraryAlbumState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.util.TimeZone

@Composable
fun MusicLibraryAlbumScreen(
    titleId: Long,
    albumId: Long,
    nav: MusicNav,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val offset = remember { TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 }
    val vm =
        viewModel(key = "music-lib-album-$titleId-$albumId") {
            PresenterViewModel(sdk.musicLibraryAlbum(titleId, albumId, offset)) { load() }
        }
    val state by vm.presenter.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tool by remember { mutableStateOf<MusicAlbumTool?>(null) }
    MusicNoteEffect(state.note, snackbar)
    val id = state.titleId.takeIf { it > 0 } ?: titleId
    val actions =
        LibraryAlbumActions(
            search = { vm.run { searchNow() } },
            toggleKeep = { vm.run { toggleKeep() } },
            rename = { tool = MusicAlbumTool.RENAME },
            delete = { tool = MusicAlbumTool.DELETE },
            report = { nav.openRoute(Routes.reportProblem(id, state.artistName, albumId = albumId)) },
            history = { nav.openRoute(Routes.history(id)) },
            pressing = { pressing -> vm.run { changePressing(pressing) } },
        )
    ScreenScaffold(
        title = state.name,
        onBack = onBack,
        actions = { if (!state.notFound) LibraryAlbumMenu(state, actions) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(status = state.status, isEmpty = false, onRefresh = { vm.run { refresh() } }) {
                if (state.notFound) {
                    MessageView(
                        icon = Icons.Rounded.Album,
                        title = stringResource(R.string.music_album_lib_not_found),
                        message = stringResource(R.string.music_album_lib_not_found_hint),
                        action = stringResource(R.string.music_album_lib_back_to_artist),
                        onAction = onBack,
                    )
                } else {
                    LibraryAlbumBody(state, nav, actions)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    tool?.let { current ->
        MusicAlbumToolSheet(current, id, albumId, state.name) { deleted ->
            tool = null
            if (deleted) {
                vm.run { removed() }
                onBack()
            }
        }
    }
}

@Composable
private fun LibraryAlbumBody(
    state: MusicLibraryAlbumState,
    nav: MusicNav,
    actions: LibraryAlbumActions,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        item(key = "hero") { LibraryAlbumHero(state, nav) }
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        item(key = "tracks-header") { SectionHeader(stringResource(R.string.music_album_tracks)) }
        if (state.tracks.isEmpty()) item(key = "tracks-empty") { LibraryAlbumEmpty(state) }
        itemsIndexed(state.tracks, key = { index, _ -> "track-$index" }) { _, track ->
            LibraryTrackRow(track, state.canManage)
        }
        if (state.canManage) item(key = "card") { LibraryAlbumCard(state, actions) }
        libraryAlbumMore(state, nav)
    }
}
