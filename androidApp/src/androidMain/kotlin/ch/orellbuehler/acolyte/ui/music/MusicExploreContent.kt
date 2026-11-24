package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicItem
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.discover.deviceRegion
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun MusicExploreContent(
    nav: MusicNav,
    typeBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sdk = LocalSdk.current
    val model = viewModel(key = "music-explore") { PresenterViewModel(sdk.musicRows("", "", deviceRegion(), musicUtcOffset())) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    RefreshOnResume { model.run { refresh() } }
    Column(modifier) {
        typeBar()
        MusicGenresBar(state.strip, nav)
        MusicRowsContent(
            state = state,
            nav = nav,
            onRefresh = { model.run { refresh() } },
            onRegion = { model.run { setRegion(it) } },
            onLoadMore = { model.run { loadMore() } },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MusicGenresBar(
    strip: List<MusicItem>,
    nav: MusicNav,
) {
    if (strip.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
        horizontalArrangement = Arrangement.End,
    ) {
        Column {
            OutlinedButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = Spacing.Medium)) {
                Text(stringResource(R.string.discover_genres))
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                strip.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            open = false
                            nav.open(item)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_explore_all_genres)) },
                    onClick = {
                        open = false
                        nav.openSection(Section.SEARCH)
                    },
                )
            }
        }
    }
}
