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
)

@Serializable
data class ReportRoute(
    val titleId: Long,
)

object Routes {
    fun title(
        kind: String,
        tmdb: String,
    ): TitleRoute = TitleRoute(kind, tmdb)

    fun libraryTitle(id: Long): TitleRoute = TitleRoute("", "", id)

    fun person(id: String): PersonRoute = PersonRoute(id)

    fun collection(id: String): CollectionRoute = CollectionRoute(id)

    fun ask(
        kind: String,
        tmdb: String,
    ): AskRoute = AskRoute(kind, tmdb)

    fun report(titleId: Long): ReportRoute = ReportRoute(titleId)

    val settingsLibraryFolders: Any = SettingsLibraryFoldersRoute
}
