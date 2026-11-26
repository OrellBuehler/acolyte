package ch.orellbuehler.acolyte.tv.calendar

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.nav.TvSectionPlaceholder

@Composable
fun TvCalendarScreen(account: Account) {
    TvSectionPlaceholder(Section.CALENDAR)
}
