package ch.orellbuehler.acolyte.tv.explore

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.nav.TvSectionPlaceholder

@Composable
fun TvExploreScreen(account: Account) {
    TvSectionPlaceholder(Section.EXPLORE)
}
