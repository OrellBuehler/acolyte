package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsKeysAddresses
import ch.orellbuehler.acolyte.api.deviceLanguage
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.platformEngine
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowsePresenter
import ch.orellbuehler.acolyte.feature.discover.BrowseViewPresenter
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.DiscoverPrefs
import ch.orellbuehler.acolyte.feature.discover.ExplorePresenter
import ch.orellbuehler.acolyte.feature.discover.ExploreStatsPresenter
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.GenrePresenter
import ch.orellbuehler.acolyte.feature.discover.HiddenPresenter
import ch.orellbuehler.acolyte.feature.discover.HideSheetPresenter
import ch.orellbuehler.acolyte.feature.discover.OwnedLibrary
import ch.orellbuehler.acolyte.feature.discover.PeopleBrowsePresenter
import ch.orellbuehler.acolyte.feature.discover.PersonPresenter
import ch.orellbuehler.acolyte.feature.discover.PopularPresenter
import ch.orellbuehler.acolyte.feature.discover.SearchPresenter
import ch.orellbuehler.acolyte.feature.discover.SeriesPresenter
import ch.orellbuehler.acolyte.feature.downloads.CalendarPresenter
import ch.orellbuehler.acolyte.feature.downloads.DownloadBadges
import ch.orellbuehler.acolyte.feature.downloads.DownloadDetailPresenter
import ch.orellbuehler.acolyte.feature.downloads.DownloadsPresenter
import ch.orellbuehler.acolyte.feature.downloads.HistoryPresenter
import ch.orellbuehler.acolyte.feature.health.HealthPresenter
import ch.orellbuehler.acolyte.feature.home.HomePresenter
import ch.orellbuehler.acolyte.feature.home.localToday
import ch.orellbuehler.acolyte.feature.library.LibraryCopiesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryDeletedPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryFilesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryFoldersPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryImportPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.feature.library.LibraryPrefs
import ch.orellbuehler.acolyte.feature.library.LibraryPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryReleasesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryRenamePresenter
import ch.orellbuehler.acolyte.feature.library.LibraryStatsPresenter
import ch.orellbuehler.acolyte.feature.library.LibrarySubtitlesPresenter
import ch.orellbuehler.acolyte.feature.requests.AskMusicPresenter
import ch.orellbuehler.acolyte.feature.requests.AskPresenter
import ch.orellbuehler.acolyte.feature.requests.AskReviewPresenter
import ch.orellbuehler.acolyte.feature.requests.NeverAddPresenter
import ch.orellbuehler.acolyte.feature.requests.OutsideListsPresenter
import ch.orellbuehler.acolyte.feature.requests.ReportProblemPresenter
import ch.orellbuehler.acolyte.feature.requests.ReportsPresenter
import ch.orellbuehler.acolyte.feature.requests.RequestSettingsPresenter
import ch.orellbuehler.acolyte.feature.requests.RequestsInbox
import ch.orellbuehler.acolyte.feature.requests.RequestsPresenter
import ch.orellbuehler.acolyte.feature.settings.SettingsHomePresenter
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookAppearancePresenter
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookDefaultsPresenter
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookMetadataPresenter
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersPresenter
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsAiPresenter
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsApiPresenter
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksPresenter
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsDangerPresenter
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsPartsPresenter
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsSchedulesPresenter
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsThisAppPresenter
import ch.orellbuehler.acolyte.feature.title.TitlePresenter
import ch.orellbuehler.acolyte.image.Images
import ch.orellbuehler.acolyte.nav.LibraryTool
import ch.orellbuehler.acolyte.session.SecureStore
import ch.orellbuehler.acolyte.session.SessionManager
import ch.orellbuehler.acolyte.session.SignInFlow
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AcolyteSdk internal constructor(
    private val store: SecureStore,
    appVersion: String,
    engineFactory: () -> HttpClientEngine,
    scope: CoroutineScope,
) {
    constructor(store: SecureStore, appVersion: String) : this(
        store,
        appVersion,
        ::platformEngine,
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    val session: SessionManager = SessionManager(store, engineFactory, appVersion, scope)

    val images: Images = Images { session.client }

    fun signIn(): SignInFlow = SignInFlow(session)

    fun health(): HealthPresenter = HealthPresenter(session.api, { session.account })

    private val libraryPrefs = LibraryPrefs(store)

    fun library(kind: LibraryKind?): LibraryPresenter = LibraryPresenter(session.api, { session.account }, libraryPrefs, kind)

    fun libraryTools(): List<LibraryTool> = session.account?.libraryTools.orEmpty()

    fun libraryStats(): LibraryStatsPresenter = LibraryStatsPresenter(session.api) { session.account }

    fun libraryCopies(): LibraryCopiesPresenter = LibraryCopiesPresenter(session.api)

    fun libraryDeleted(titleId: Long): LibraryDeletedPresenter = LibraryDeletedPresenter(session.api, { session.account }, titleId)

    fun libraryReleases(turnedDown: Boolean): LibraryReleasesPresenter = LibraryReleasesPresenter(session.api, turnedDown)

    fun librarySubtitles(): LibrarySubtitlesPresenter = LibrarySubtitlesPresenter(session.api)

    fun libraryRename(): LibraryRenamePresenter = LibraryRenamePresenter(session.api) { session.account }

    fun libraryImport(): LibraryImportPresenter =
        LibraryImportPresenter(session.api) { session.account?.libraryTools?.contains(LibraryTool.LIBRARY_FOLDERS) == true }

    fun libraryFiles(path: String): LibraryFilesPresenter = LibraryFilesPresenter(session.api, { session.account }, path)

    fun libraryFolders(): LibraryFoldersPresenter =
        LibraryFoldersPresenter(
            session.api,
            { session.account },
            { password, code -> session.reauth(password, code) },
            { session.refresh() },
        )

    fun title(
        kind: String,
        tmdb: String,
        libraryId: Long,
        openLibrary: Boolean,
    ): TitlePresenter = TitlePresenter(session.api, kind, tmdb, libraryId, openLibrary, session.account, deviceLanguage())

    private val requestsScope: CoroutineScope = scope

    val inbox: RequestsInbox = RequestsInbox(session.api) { session.account }

    fun requests(): RequestsPresenter = RequestsPresenter(session.api, { session.account }, inbox, requestsScope)

    fun askReview(askId: Long): AskReviewPresenter = AskReviewPresenter(session.api, { session.account }, askId)

    fun ask(
        kind: String,
        tmdb: String,
        fourK: Boolean,
    ): AskPresenter = AskPresenter(session.api, inbox, kind, tmdb, fourK)

    fun askMusic(
        artist: String,
        name: String,
        album: String,
        edition: String,
    ): AskMusicPresenter = AskMusicPresenter(session.api, inbox, artist, name, album, edition)

    fun reports(): ReportsPresenter = ReportsPresenter(session.api, { session.account }, inbox)

    fun reportProblem(
        titleId: Long,
        season: Int,
        itemId: Long,
        albumId: Long,
        versionId: Long,
    ): ReportProblemPresenter = ReportProblemPresenter(session.api, titleId, season, itemId, albumId, versionId)

    fun requestSettings(): RequestSettingsPresenter = RequestSettingsPresenter(session.api) { session.account }

    fun outsideLists(): OutsideListsPresenter = OutsideListsPresenter(session.api) { session.account }

    fun neverAdd(): NeverAddPresenter = NeverAddPresenter(session.api) { session.account }

    private val discoverWho: () -> String = { session.account?.let { "${it.serverUrl}|${it.userId}" }.orEmpty() }
    private val ownedLibrary = OwnedLibrary(session.api, discoverWho)
    private val discoverPrefs = DiscoverPrefs(store, discoverWho)

    fun home(utcOffsetSeconds: Int): HomePresenter = HomePresenter(session.api, { session.account }, ownedLibrary, utcOffsetSeconds)

    fun explore(
        region: String,
        timeZone: String,
    ): ExplorePresenter = ExplorePresenter(session.api, { session.account }, ownedLibrary, discoverPrefs, region, timeZone)

    fun exploreGenre(
        type: ExploreType,
        genre: Int,
        region: String,
    ): GenrePresenter = GenrePresenter(session.api, { session.account }, ownedLibrary, type, genre, region)

    fun popular(utcOffsetSeconds: Int): PopularPresenter =
        PopularPresenter(session.api, { session.account }, ownedLibrary, discoverPrefs) { localToday(utcOffsetSeconds) }

    fun search(): SearchPresenter = SearchPresenter(session.api, { session.account }, ownedLibrary, discoverPrefs)

    fun browse(): BrowsePresenter = BrowsePresenter(session.api) { session.account }

    fun browseView(link: BrowseLink): BrowseViewPresenter = BrowseViewPresenter(session.api, { session.account }, ownedLibrary, link)

    fun peopleBrowse(): PeopleBrowsePresenter = PeopleBrowsePresenter(session.api)

    fun person(id: String): PersonPresenter = PersonPresenter(session.api, { session.account }, ownedLibrary, discoverPrefs, id)

    fun movieSeries(id: String): SeriesPresenter = SeriesPresenter(session.api, { session.account }, id)

    fun hidden(userId: Long): HiddenPresenter = HiddenPresenter(session.api, userId.takeIf { it >= 0 })

    fun hideTitle(
        kind: CardKind,
        tmdb: String,
        name: String,
        genres: List<String>,
    ): HideSheetPresenter = HideSheetPresenter(session.api, { session.account }, tmdb, kind, name, genres)

    fun exploreStats(): ExploreStatsPresenter = ExploreStatsPresenter(session.api) { session.account }

    val downloadBadges: DownloadBadges by lazy { DownloadBadges(session.api, { session.account }) }

    fun downloads(): DownloadsPresenter = DownloadsPresenter(session.api, session.liveStatus, downloadBadges, { session.account })

    fun downloadDetail(row: DownloadRow): DownloadDetailPresenter =
        DownloadDetailPresenter(
            session.api,
            row,
            session.account?.can(Need.MANAGE_LIBRARY) == true,
            session.account?.can(Need.SETTINGS) == true,
        )

    fun history(
        titleId: Long,
        episodeId: Long,
    ): HistoryPresenter = HistoryPresenter(session.api, { session.account }, titleId, episodeId)

    fun calendar(): CalendarPresenter = CalendarPresenter(session.api, { session.account })

    fun settingsHome(): SettingsHomePresenter = SettingsHomePresenter(session.api, { session.account }, downloadBadges, inbox)
    fun settingsLookAppearance(): SettingsLookAppearancePresenter = SettingsLookAppearancePresenter(session.api) { session.account }

    fun settingsLookDefaults(): SettingsLookDefaultsPresenter = SettingsLookDefaultsPresenter(session.api) { session.account }

    fun settingsLookMetadata(): SettingsLookMetadataPresenter = SettingsLookMetadataPresenter(session.api) { session.account }
    fun settingsMediaServers(): SettingsMediaServersPresenter =
        SettingsMediaServersPresenter(session.api, { session.account }, { password, code -> session.reauth(password, code) })
    fun settingsApi(): SettingsApiPresenter =
        SettingsApiPresenter(session.api, { session.account }, { password, code -> session.reauth(password, code) })

    fun settingsAi(): SettingsAiPresenter =
        SettingsAiPresenter(session.api, { session.account }, { password, code -> session.reauth(password, code) })

    fun settingsRemoteAccessUrl(): String = session.account?.let { SettingsKeysAddresses.remoteAccess(it.serverUrl) }.orEmpty()
    fun settingsHealthChecks(): SettingsHealthChecksPresenter = SettingsHealthChecksPresenter(session.api) { session.account }
    fun settingsControlsParts(): SettingsControlsPartsPresenter =
        SettingsControlsPartsPresenter(
            session.api,
            { session.account },
            { password, code -> session.reauth(password, code) },
            { session.refresh() },
        )

    fun settingsControlsThisApp(): SettingsControlsThisAppPresenter =
        SettingsControlsThisAppPresenter(session.api, { session.account }, { password, code -> session.reauth(password, code) })

    fun settingsControlsDanger(): SettingsControlsDangerPresenter =
        SettingsControlsDangerPresenter(
            session.api,
            { session.account },
            { password, code -> session.reauth(password, code) },
            { session.refresh() },
        )

    fun settingsControlsSchedules(): SettingsControlsSchedulesPresenter =
        SettingsControlsSchedulesPresenter(session.api, { session.account })
}
