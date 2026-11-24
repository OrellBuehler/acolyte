package ch.orellbuehler.acolyte.ui.nav

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
data object MoreRoute

@Serializable
data class TitleRoute(
    val kind: String,
    val tmdb: String,
    val libraryId: Long = 0,
    val tab: String = "",
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

object Routes {
    fun title(
        kind: String,
        tmdb: String,
    ): TitleRoute = TitleRoute(kind, tmdb)

    fun libraryTitle(id: Long): TitleRoute = TitleRoute("", "", id)

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

    fun report(titleId: Long): Any = ReportProblemRoute(titleId)

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
