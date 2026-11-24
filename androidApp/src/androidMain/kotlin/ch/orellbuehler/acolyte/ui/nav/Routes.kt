package ch.orellbuehler.acolyte.ui.nav

import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object ExploreRoute

@Serializable
data object LibraryRoute

@Serializable
data object SearchRoute

@Serializable
data object PopularRoute

@Serializable
data object MoviesRoute

@Serializable
data object ShowsRoute

@Serializable
data object MusicRoute

@Serializable
data object StatsRoute

@Serializable
data object DownloadsRoute

@Serializable
data object CalendarRoute

@Serializable
data object RequestsRoute

@Serializable
data object HistoryRoute

@Serializable
data object HealthRoute

@Serializable
data object PeopleRoute

@Serializable
data object SettingsRoute

@Serializable
data object AccountRoute

@Serializable
data object AccountProfileRoute

@Serializable
data object AccountSecurityRoute

@Serializable
data object AccountAlertsRoute

@Serializable
data object AccountDevicesRoute

@Serializable
data object AiConnectRoute

@Serializable
data class PeopleDetailRoute(
    val id: Long,
)

@Serializable
data class PeopleDevicesRoute(
    val id: Long,
    val name: String,
)

@Serializable
data object MoreRoute

@Serializable
data class TitleRoute(
    val kind: String,
    val tmdb: String,
    val libraryId: Long = 0,
    val tab: String = "",
)

@Serializable
data class DownloadsFocusRoute(
    val show: String = "",
    val open: String = "",
)

@Serializable
data class HistoryScopeRoute(
    val titleId: Long = 0,
    val episodeId: Long = 0,
)

@Serializable
data object LibraryToolsRoute

@Serializable
data class LibraryDeletedRoute(
    val titleId: Long = 0,
)

@Serializable
data object LibraryCopiesRoute

@Serializable
data class LibraryReleasesRoute(
    val turnedDown: Boolean = false,
)

@Serializable
data object LibrarySubtitlesRoute

@Serializable
data object LibraryRenameRoute

@Serializable
data object LibraryImportRoute

@Serializable
data class LibraryImportFilesRoute(
    val path: String = "",
)

@Serializable
data object SettingsLibraryFoldersRoute

@Serializable
data object SettingsSignInRoute

@Serializable
data object SettingsApiRoute

@Serializable
data object SettingsAiRoute

@Serializable
data object SettingsRemoteAccessRoute

@Serializable
data object SettingsAppearanceRoute

@Serializable
data object SettingsTitleDefaultsRoute

@Serializable
data object SettingsMetadataRoute

@Serializable
data object SettingsAlertsRoute

@Serializable
data object SettingsBackupsRoute

@Serializable
data object SettingsLogsRoute

@Serializable
data object SettingsHealthRoute

@Serializable
data object SettingsPartsRoute

@Serializable
data object SettingsThisAppRoute

@Serializable
data object SettingsDangerRoute

@Serializable
data object SettingsSchedulesRoute

@Serializable
data object SettingsMediaServersRoute

@Serializable
data object SettingsSourcesRoute

@Serializable
data object SettingsDownloadersRoute

@Serializable
data object SettingsPickRulesRoute

@Serializable
data object SettingsNamingRoute

@Serializable
data object SettingsSubtitlesRoute

@Serializable
data object SettingsSearchRoute

@Serializable
data object SettingsMoveRoute

@Serializable
data object SettingsMusicRoute

@Serializable
data class PersonRoute(
    val id: String,
)

@Serializable
data class CollectionRoute(
    val id: String,
)

@Serializable
data class AskRoute(
    val kind: String,
    val tmdb: String,
    val fourK: Boolean = false,
)

@Serializable
data class AskMusicRoute(
    val artist: String,
    val name: String = "",
    val album: String = "",
    val edition: String = "",
)

@Serializable
data object ReportsRoute

@Serializable
data class ReportProblemRoute(
    val titleId: Long,
    val name: String = "",
    val season: Int = -1,
    val itemId: Long = 0,
    val albumId: Long = 0,
    val versionId: Long = 0,
)

@Serializable
data object RequestRulesRoute

@Serializable
data object OutsideListsRoute

@Serializable
data object NeverAddRoute

@Serializable
data class DiscoverGenreRoute(
    val type: String,
    val genre: Int,
)

@Serializable
data class DiscoverBrowseRoute(
    val target: String,
    val type: String,
    val genre: Int,
    val category: String,
    val mood: String,
)

@Serializable
data object DiscoverHiddenRoute

@Serializable
data object DiscoverStatsRoute

object Routes {
    fun title(
        kind: String,
        tmdb: String,
    ): TitleRoute = TitleRoute(kind, tmdb)

    fun libraryTitle(id: Long): TitleRoute = TitleRoute("", "", id)

    fun downloadTitle(link: DownloadTitleLink): Any =
        TitleRoute(link.kind, link.tmdb, link.libraryId, if (link.openLibrary) "library" else "")

    fun downloads(
        show: String = "",
        open: String = "",
    ): Any = DownloadsFocusRoute(show, open)

    fun history(
        titleId: Long,
        episodeId: Long = 0,
    ): Any = HistoryScopeRoute(titleId, episodeId)

    fun person(id: String): PersonRoute = PersonRoute(id)

    fun collection(id: String): CollectionRoute = CollectionRoute(id)

    val settingsLibraryFolders: Any = SettingsLibraryFoldersRoute

    fun ask(
        kind: String,
        tmdb: String,
        fourK: Boolean = false,
    ): Any = AskRoute(kind, tmdb, fourK)

    fun askMusic(
        artist: String,
        name: String = "",
        album: String = "",
        edition: String = "",
    ): Any = AskMusicRoute(artist, name, album, edition)

    val reports: Any = ReportsRoute

    fun reportProblem(
        titleId: Long,
        name: String = "",
        season: Int = -1,
        itemId: Long = 0,
        albumId: Long = 0,
        versionId: Long = 0,
    ): Any = ReportProblemRoute(titleId, name, season, itemId, albumId, versionId)

    val settingsRequests: Any = RequestRulesRoute
    val settingsOutsideLists: Any = OutsideListsRoute
    val settingsNeverAdd: Any = NeverAddRoute
}
