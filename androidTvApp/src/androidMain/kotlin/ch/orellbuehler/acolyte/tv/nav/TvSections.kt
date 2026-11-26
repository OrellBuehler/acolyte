package ch.orellbuehler.acolyte.tv.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvPlaceholderView

object TvSections {
    val order: List<Section> =
        listOf(Section.HOME, Section.EXPLORE, Section.SEARCH, Section.REQUESTS, Section.CALENDAR, Section.DOWNLOADS)

    val pushed: Set<Section> = setOf(Section.POPULAR)

    fun visible(
        section: Section,
        account: Account,
    ): Boolean = section in account.tabs || account.moreGroups.any { section in it.sections }

    fun tabs(account: Account): List<Section> = order.filter { visible(it, account) } + Section.SETTINGS
}

@get:StringRes
val Section.titleRes: Int
    get() =
        when (this) {
            Section.HOME -> R.string.tv_section_home
            Section.EXPLORE -> R.string.tv_section_explore
            Section.LIBRARY -> R.string.tv_section_library
            Section.SEARCH -> R.string.tv_section_search
            Section.POPULAR -> R.string.tv_section_popular
            Section.MOVIES -> R.string.tv_section_movies
            Section.SHOWS -> R.string.tv_section_shows
            Section.MUSIC -> R.string.tv_section_music
            Section.STATS -> R.string.tv_section_stats
            Section.DOWNLOADS -> R.string.tv_section_downloads
            Section.CALENDAR -> R.string.tv_section_calendar
            Section.REQUESTS -> R.string.tv_section_requests
            Section.HISTORY -> R.string.tv_section_history
            Section.HEALTH -> R.string.tv_section_health
            Section.PEOPLE -> R.string.tv_section_people
            Section.SETTINGS -> R.string.tv_section_settings
            Section.ACCOUNT -> R.string.tv_section_account
        }

@get:StringRes
val PhoneTopic.titleRes: Int
    get() =
        when (this) {
            PhoneTopic.PERSON -> R.string.tv_phone_what_person
            PhoneTopic.COLLECTION -> R.string.tv_phone_what_collection
            PhoneTopic.REPORTS -> R.string.tv_phone_what_reports
            PhoneTopic.OTHER -> R.string.tv_phone_what_other
        }

val Section.icon: ImageVector
    get() =
        when (this) {
            Section.HOME -> Icons.Rounded.Home
            Section.EXPLORE -> Icons.Rounded.Explore
            Section.LIBRARY -> Icons.Rounded.VideoLibrary
            Section.SEARCH -> Icons.Rounded.Search
            Section.POPULAR -> Icons.Rounded.AutoAwesome
            Section.MOVIES -> Icons.Rounded.Movie
            Section.SHOWS -> Icons.Rounded.Tv
            Section.MUSIC -> Icons.Rounded.LibraryMusic
            Section.STATS -> Icons.Rounded.Speed
            Section.DOWNLOADS -> Icons.Rounded.Download
            Section.CALENDAR -> Icons.Rounded.CalendarMonth
            Section.REQUESTS -> Icons.Rounded.Checklist
            Section.HISTORY -> Icons.Rounded.History
            Section.HEALTH -> Icons.Rounded.Shield
            Section.PEOPLE -> Icons.Rounded.Group
            Section.SETTINGS -> Icons.Rounded.Settings
            Section.ACCOUNT -> Icons.Rounded.AccountCircle
        }

@Composable
fun TvSectionPlaceholder(section: Section) {
    TvPlaceholderView(stringResource(section.titleRes), section.icon)
}
