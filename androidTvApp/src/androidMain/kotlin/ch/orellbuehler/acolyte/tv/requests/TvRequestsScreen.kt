package ch.orellbuehler.acolyte.tv.requests

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.nav.TvSectionPlaceholder

@Composable
fun TvRequestsScreen(account: Account) {
    TvSectionPlaceholder(Section.REQUESTS)
}
