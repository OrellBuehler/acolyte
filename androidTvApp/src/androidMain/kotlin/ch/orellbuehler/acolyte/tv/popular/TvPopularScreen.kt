package ch.orellbuehler.acolyte.tv.popular

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.nav.TvSectionPlaceholder

@Composable
fun TvPopularScreen(account: Account) {
    TvSectionPlaceholder(Section.POPULAR)
}
