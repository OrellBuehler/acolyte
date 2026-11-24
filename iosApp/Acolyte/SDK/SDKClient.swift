@preconcurrency import shared
import Foundation

final class SDKClient: @unchecked Sendable {
    static let shared = SDKClient()

    private let sdk: AcolyteSdk

    private init() {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "dev"
        sdk = AcolyteSdk(store: KeychainStore(), appVersion: version)
    }

    func watchSession(deliver: @MainActor @Sendable (SessionSnapshot) -> Void) async {
        for await state in sdk.session.state {
            let mapped = Mapping.session(state)
            await deliver(mapped)
        }
    }

    func watchLive(deliver: @MainActor @Sendable (LiveState) -> Void) async {
        for await status in sdk.session.liveStatus {
            let mapped = Mapping.live(status)
            await deliver(mapped)
        }
    }

    func restore() async {
        do {
            try await sdk.session.restore()
        } catch {}
    }

    func refresh() async {
        do {
            _ = try await sdk.session.refresh()
        } catch {}
    }

    func signOut() async {
        do {
            try await sdk.session.signOut()
        } catch {}
    }

    func makeSignIn() -> SignInDriver {
        SignInDriver(flow: sdk.signIn())
    }

    func makeHealth() -> HealthDriver {
        HealthDriver(presenter: sdk.health())
    }

    func makeLibrary(kind: LibraryKindID?) -> LibraryDriver {
        LibraryDriver(presenter: sdk.library(kind: Mapping.kotlinLibraryKind(kind)))
    }

    func libraryTools() -> [LibraryToolID] {
        sdk.libraryTools().compactMap { LibraryToolID(rawValue: Mapping.key($0)) }
    }

    func acquisitionPages() -> [AcquisitionPageID] {
        sdk.acquisitionPages().compactMap { AcquisitionPageID(rawValue: Mapping.key($0)) }
    }

    func makeLibraryStats() -> LibraryStatsDriver {
        LibraryStatsDriver(presenter: sdk.libraryStats())
    }

    func makeLibraryCopies() -> LibraryCopiesDriver {
        LibraryCopiesDriver(presenter: sdk.libraryCopies())
    }

    func makeLibraryDeleted(titleId: Int64) -> LibraryDeletedDriver {
        LibraryDeletedDriver(presenter: sdk.libraryDeleted(titleId: titleId))
    }

    func makeLibraryReleases(turnedDown: Bool) -> LibraryReleasesDriver {
        LibraryReleasesDriver(presenter: sdk.libraryReleases(turnedDown: turnedDown))
    }

    func makeLibrarySubtitles() -> LibrarySubtitlesDriver {
        LibrarySubtitlesDriver(presenter: sdk.librarySubtitles())
    }

    func makeLibraryRename() -> LibraryRenameDriver {
        LibraryRenameDriver(presenter: sdk.libraryRename())
    }

    func makeLibraryImport() -> LibraryImportDriver {
        LibraryImportDriver(presenter: sdk.libraryImport())
    }

    func makeLibraryFiles(path: String) -> LibraryFilesDriver {
        LibraryFilesDriver(presenter: sdk.libraryFiles(path: path))
    }

    func makeLibraryFolders() -> LibraryFoldersDriver {
        LibraryFoldersDriver(presenter: sdk.libraryFolders())
    }

    func makeTitle(kind: String, tmdb: String, libraryId: Int64, openLibrary: Bool) -> TitleDriver {
        TitleDriver(presenter: sdk.title(kind: kind, tmdb: tmdb, libraryId: libraryId, openLibrary: openLibrary), images: sdk.images)
    }

    func makeInbox() -> InboxDriver {
        InboxDriver(inbox: sdk.inbox)
    }

    func makeRequests() -> RequestsDriver {
        RequestsDriver(presenter: sdk.requests())
    }

    func makeAskReview(_ askId: Int64) -> AskReviewDriver {
        AskReviewDriver(presenter: sdk.askReview(askId: askId))
    }

    func makeAsk(kind: String, tmdb: String, fourK: Bool) -> AskDriver {
        AskDriver(presenter: sdk.ask(kind: kind, tmdb: tmdb, fourK: fourK))
    }

    func makeAskMusic(artist: String, name: String, album: String, edition: String) -> AskMusicDriver {
        AskMusicDriver(presenter: sdk.askMusic(artist: artist, name: name, album: album, edition: edition))
    }

    func makeReports() -> ReportsDriver {
        ReportsDriver(presenter: sdk.reports())
    }

    func makeReportProblem(titleId: Int64, season: Int, itemId: Int64, albumId: Int64, versionId: Int64) -> ReportProblemDriver {
        ReportProblemDriver(
            presenter: sdk.reportProblem(titleId: titleId, season: Int32(season), itemId: itemId, albumId: albumId, versionId: versionId)
        )
    }

    func makeRequestRules() -> RequestRulesDriver {
        RequestRulesDriver(presenter: sdk.requestSettings())
    }

    func makeSubtitleSettings() -> SubtitleSettingsDriver {
        SubtitleSettingsDriver(presenter: sdk.settingsSubtitles())
    }

    func makeOutsideLists() -> OutsideListsDriver {
        OutsideListsDriver(presenter: sdk.outsideLists())
    }

    func makeNeverAdd() -> NeverAddDriver {
        NeverAddDriver(presenter: sdk.neverAdd())
    }

    func makeHome() -> HomeDriver {
        HomeDriver(presenter: sdk.home(utcOffsetSeconds: Int32(TimeZone.current.secondsFromGMT())))
    }

    func makeExplore() -> ExploreDriver {
        ExploreDriver(presenter: sdk.explore(region: SDKClient.region, timeZone: TimeZone.current.identifier))
    }

    func makeGenre(type: ExploreTypeID, genre: Int) -> GenreDriver {
        GenreDriver(presenter: sdk.exploreGenre(type: Mapping.kotlinType(type), genre: Int32(genre), region: SDKClient.region))
    }

    func makePopular() -> PopularDriver {
        PopularDriver(presenter: sdk.popular(utcOffsetSeconds: Int32(TimeZone.current.secondsFromGMT())))
    }

    func makeSearch() -> SearchDriver {
        SearchDriver(search: sdk.search(), browse: sdk.browse(), people: sdk.peopleBrowse())
    }

    func makeBrowseView(_ link: BrowseLinkInfo) -> BrowseViewDriver {
        BrowseViewDriver(presenter: sdk.browseView(link: Mapping.kotlinLink(link)))
    }

    func makePerson(_ id: String) -> PersonDriver {
        PersonDriver(presenter: sdk.person(id: id))
    }

    func makeSeries(_ id: String) -> SeriesDriver {
        SeriesDriver(presenter: sdk.movieSeries(id: id))
    }

    func makeHidden() -> HiddenDriver {
        HiddenDriver(presenter: sdk.hidden(userId: -1))
    }

    func makeHideSheet(_ target: HideTarget) -> HideSheetDriver {
        HideSheetDriver(
            presenter: sdk.hideTitle(kind: Mapping.kotlinKind(target.kind), tmdb: target.tmdb, name: target.name, genres: target.genres)
        )
    }

    func makeExploreStats() -> ExploreStatsDriver {
        ExploreStatsDriver(presenter: sdk.exploreStats())
    }

    func genreOptions(_ type: ExploreTypeID) -> [GenreOption] {
        DiscoverKit.shared.genres(type: Mapping.kotlinType(type)).map { GenreOption(id: Int($0.id), name: $0.name) }
    }

    func genreName(_ id: Int, type: ExploreTypeID) -> String {
        DiscoverKit.shared.genreName(id: Int32(id), type: Mapping.kotlinType(type))
    }

    static var region: String {
        let code = Locale.current.region?.identifier ?? ""
        return code.count == 2 ? code : "US"
    }

    func makeDownloads() -> DownloadsDriver {
        DownloadsDriver(presenter: sdk.downloads())
    }

    func makeDownloadDetail(list: DownloadsDriver, key: String) -> DownloadDetailDriver? {
        guard let row = list.row(for: key) else { return nil }
        return DownloadDetailDriver(presenter: sdk.downloadDetail(row: row), key: key, list: list)
    }

    func makeHistory(titleId: Int64, episodeId: Int64) -> HistoryDriver {
        HistoryDriver(presenter: sdk.history(titleId: titleId, episodeId: episodeId))
    }

    func makeCalendar() -> CalendarDriver {
        CalendarDriver(presenter: sdk.calendar())
    }

    func makeDownloadBadges() -> DownloadBadgesDriver {
        DownloadBadgesDriver(badges: sdk.downloadBadges)
    }

    func reauth(password: String, code: String) async -> Bool {
        do {
            let failure = try await sdk.session.reauth(password: password, code: code.isEmpty ? nil : code)
            return failure == nil
        } catch {
            return false
        }
    }

    func imageRequest(_ source: String?, size: ImageSizeKind) -> ImageRequestInfo? {
        guard let text = sdk.images.url(source: source, size: Mapping.kotlinSize(size)), let url = URL(string: text) else {
            return nil
        }
        return ImageRequestInfo(url: url, headers: sdk.images.headers(url: text))
    }

    func storeCookies(_ cookies: [String], from url: URL) async {
        guard !cookies.isEmpty else { return }
        do {
            try await sdk.images.storeCookies(url: url.absoluteString, setCookieHeaders: cookies)
        } catch {}
    }

    func placeholder(_ name: String) -> PlaceholderInfo {
        PlaceholderInfo(text: ArtPlaceholder.shared.text(name: name), hue: Double(ArtPlaceholder.shared.hue(name: name)))
    }
}
