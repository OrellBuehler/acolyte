package ch.orellbuehler.acolyte.tv.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvOpenOnPhoneView
import ch.orellbuehler.acolyte.tv.nav.BrowseRoute

@Composable
fun TvBrowseScreen(route: BrowseRoute) {
    TvOpenOnPhoneView(stringResource(R.string.tv_section_explore))
}
