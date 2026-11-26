package ch.orellbuehler.acolyte.tv.downloads

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.nav.TvSectionPlaceholder

@Composable
fun TvDownloadsScreen(account: Account) {
    TvSectionPlaceholder(Section.DOWNLOADS)
}
