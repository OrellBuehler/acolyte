package ch.orellbuehler.acolyte.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.nav.MoreTab
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.nav.SectionGroup

data class Destination(
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: Any,
)

val Section.destination: Destination
    get() =
        when (this) {
            Section.HOME -> Destination(R.string.section_home, Icons.Outlined.Home, Icons.Rounded.Home, HomeRoute)
            Section.EXPLORE -> Destination(R.string.section_explore, Icons.Outlined.Explore, Icons.Rounded.Explore, ExploreRoute)
            Section.LIBRARY -> Destination(R.string.section_library, Icons.Outlined.VideoLibrary, Icons.Rounded.VideoLibrary, LibraryRoute)
            Section.SEARCH -> Destination(R.string.section_search, Icons.Outlined.Search, Icons.Rounded.Search, SearchRoute)
            Section.POPULAR -> Destination(R.string.section_popular, Icons.Outlined.AutoAwesome, Icons.Rounded.AutoAwesome, PopularRoute)
            Section.MOVIES -> Destination(R.string.section_movies, Icons.Outlined.Movie, Icons.Rounded.Movie, MoviesRoute)
            Section.SHOWS -> Destination(R.string.section_shows, Icons.Outlined.Tv, Icons.Rounded.Tv, ShowsRoute)
            Section.MUSIC -> Destination(R.string.section_music, Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic, MusicRoute)
            Section.STATS -> Destination(R.string.section_stats, Icons.Outlined.Speed, Icons.Rounded.Speed, StatsRoute)
            Section.DOWNLOADS -> Destination(R.string.section_downloads, Icons.Outlined.Download, Icons.Rounded.Download, DownloadsRoute)
            Section.CALENDAR ->
                Destination(
                    R.string.section_calendar,
                    Icons.Outlined.CalendarMonth,
                    Icons.Rounded.CalendarMonth,
                    CalendarRoute,
                )
            Section.REQUESTS -> Destination(R.string.section_requests, Icons.Outlined.Checklist, Icons.Rounded.Checklist, RequestsRoute)
            Section.HISTORY -> Destination(R.string.section_history, Icons.Outlined.History, Icons.Rounded.History, HistoryRoute)
            Section.HEALTH -> Destination(R.string.section_health, Icons.Outlined.Shield, Icons.Rounded.Shield, HealthRoute)
            Section.PEOPLE -> Destination(R.string.section_people, Icons.Outlined.Group, Icons.Rounded.Group, PeopleRoute)
            Section.SETTINGS -> Destination(R.string.section_settings, Icons.Outlined.Settings, Icons.Rounded.Settings, SettingsRoute)
            Section.ACCOUNT ->
                Destination(
                    R.string.section_account,
                    Icons.Outlined.AccountCircle,
                    Icons.Rounded.AccountCircle,
                    AccountRoute,
                )
        }

val MoreTab.destination: Destination
    get() =
        when (this) {
            MoreTab.MORE -> Destination(R.string.nav_more, Icons.Outlined.MoreHoriz, Icons.Rounded.MoreHoriz, MoreRoute)
            MoreTab.MANAGE -> Destination(R.string.nav_manage, Icons.Outlined.Inbox, Icons.Rounded.Inbox, MoreRoute)
        }

@get:StringRes
val SectionGroup.title: Int?
    get() =
        when (this) {
            SectionGroup.DISCOVER -> null
            SectionGroup.LIBRARY -> R.string.group_library
            SectionGroup.YOURS -> R.string.group_yours
            SectionGroup.ACTIVITY -> R.string.group_activity
            SectionGroup.SYSTEM -> R.string.group_system
            SectionGroup.YOU -> R.string.group_you
        }
