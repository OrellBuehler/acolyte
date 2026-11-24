package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.discover.deviceRegion
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel

@Composable
fun MusicRowsScreen(
    kind: String,
    slug: String,
    nav: MusicNav,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val model =
        viewModel(
            key = "music-rows-$kind-$slug",
        ) { PresenterViewModel(sdk.musicRows(kind, slug, deviceRegion(), musicUtcOffset())) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    RefreshOnResume { model.run { refresh() } }
    ScreenScaffold(title = state.title.ifEmpty { stringResource(R.string.section_music) }, onBack = onBack) { padding ->
        MusicRowsContent(
            state = state,
            nav = nav,
            onRefresh = { model.run { refresh() } },
            onRegion = { model.run { setRegion(it) } },
            onLoadMore = { model.run { loadMore() } },
            modifier = Modifier.padding(padding),
        )
    }
}
