package ch.orellbuehler.acolyte.tv.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvOpenOnPhoneView

@Composable
fun TvCollectionScreen(id: String) {
    TvOpenOnPhoneView(stringResource(R.string.tv_phone_what_collection))
}
