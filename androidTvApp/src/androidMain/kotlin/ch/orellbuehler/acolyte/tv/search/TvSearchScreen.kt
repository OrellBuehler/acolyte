package ch.orellbuehler.acolyte.tv.search

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.nav.TvSectionPlaceholder

@Composable
fun TvSearchScreen(account: Account) {
    TvSectionPlaceholder(Section.SEARCH)
}
