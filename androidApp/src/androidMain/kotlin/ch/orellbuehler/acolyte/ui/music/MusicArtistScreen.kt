package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.music.MusicArtistPresenter
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.util.TimeZone

internal fun artistUtcOffset(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000

@Composable
fun MusicArtistScreen(
    mbid: String,
    nav: MusicNav,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "music-artist-$mbid") { PresenterViewModel(sdk.musicArtist(mbid, artistUtcOffset())) { load() } }
    val presenter = viewModel.presenter
    val state by presenter.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var about by rememberSaveable { mutableStateOf(false) }
    MusicNoteEffect(state.note, snackbar)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.run { reloadQuiet() } }
    val actions = rememberArtistActions(presenter, viewModel::run, nav) { about = true }
    ScreenScaffold(title = state.name, onBack = onBack) { padding ->
        Box(Modifier.padding(padding)) {
            if (state.status.phase == Phase.FAILED) {
                MusicProblemView(state.problem, false, onRetry = { viewModel.run { refresh() } })
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = { viewModel.run { refresh() } }) {
                    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                        musicArtistBody(state, actions)
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (about) MusicArtistAboutSheet(state) { about = false }
}

@Composable
fun rememberArtistActions(
    presenter: MusicArtistPresenter,
    run: (suspend MusicArtistPresenter.() -> Unit) -> Unit,
    nav: MusicNav,
    onAbout: () -> Unit,
): MusicArtistActions =
    MusicArtistActions(
        nav = nav,
        onStillReading = { presenter.stillReading() },
        onAbout = onAbout,
        onFollow = { run { toggleFollow() } },
        onAlbumFollow = { id -> run { toggleAlbumFollow(id) } },
        onAskAll = { run { askAll() } },
    )
