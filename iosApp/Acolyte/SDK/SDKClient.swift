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

    func makeReauth() -> ReauthDriver {
        ReauthDriver(gate: sdk.reauthGate())
    }

    func makeInvite(_ target: InviteTargetInfo) -> InviteDriver {
        InviteDriver(flow: sdk.invite(target: Mapping.kotlinInviteTarget(target)))
    }

    func parseInvite(_ text: String) -> InviteTargetInfo? {
        InviteLink.shared.parse(text: text).map { Mapping.inviteTarget($0) }
    }

    func makeAccountWizard() -> AccountWizardDriver {
        AccountWizardDriver(wizard: sdk.accountWizard(), security: sdk.accountSecurity())
    }

    func makeAccountProfile() -> MyProfileDriver {
        MyProfileDriver(presenter: sdk.accountProfile())
    }

    func makeAccountSecurity() -> MySecurityDriver {
        MySecurityDriver(presenter: sdk.accountSecurity())
    }

    func makeAccountDevices(userId: Int64?) -> MyDevicesDriver {
        MyDevicesDriver(presenter: sdk.accountDevices(userId: userId.map { KotlinLong(value: $0) }))
    }

    func makePeople() -> PeopleDriver {
        PeopleDriver(presenter: sdk.people())
    }

    func makeAiConnect(_ link: String) -> AiConnectDriver {
        AiConnectDriver(presenter: sdk.aiConnect(link: link))
    }

    func cleanName(_ raw: String) -> String {
        AccountRules.shared.cleanName(raw: raw)
    }

    func validName(_ name: String) -> Bool {
        AccountRules.shared.validName(name: name)
    }

    func validPassword(_ password: String, minimum: Int) -> Bool {
        AccountRules.shared.validPassword(password: password, minimum: Int32(minimum))
    }

    func validDisplayName(_ name: String) -> Bool {
        AccountRules.shared.validDisplayName(name: name)
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

    func makeSettingsHome() -> SettingsHomeDriver {
        SettingsHomeDriver(presenter: sdk.settingsHome())
    func makeSettingsLookAppearance() -> SettingsLookAppearanceDriver {
        SettingsLookAppearanceDriver(presenter: sdk.settingsLookAppearance())
    }

    func makeSettingsLookDefaults() -> SettingsLookDefaultsDriver {
        SettingsLookDefaultsDriver(presenter: sdk.settingsLookDefaults())
    }

    func makeSettingsLookMetadata() -> SettingsLookMetadataDriver {
        SettingsLookMetadataDriver(presenter: sdk.settingsLookMetadata())
    func makeSettingsMediaServers() -> SettingsMediaServersDriver {
        SettingsMediaServersDriver(presenter: sdk.settingsMediaServers())
    func makeSettingsApi() -> SettingsApiDriver {
        SettingsApiDriver(presenter: sdk.settingsApi())
    }

    func makeSettingsAi() -> SettingsAiDriver {
        SettingsAiDriver(presenter: sdk.settingsAi())
    }

    func settingsRemoteAccessURL() -> URL? {
        URL(string: sdk.settingsRemoteAccessUrl())
    func makeHealthChecks() -> HealthChecksDriver {
        HealthChecksDriver(presenter: sdk.settingsHealthChecks())
    func makeSettingsControlsParts() -> SettingsControlsPartsDriver {
        SettingsControlsPartsDriver(presenter: sdk.settingsControlsParts())
    }

    func makeSettingsControlsThisApp() -> SettingsControlsThisAppDriver {
        SettingsControlsThisAppDriver(presenter: sdk.settingsControlsThisApp())
    }

    func makeSettingsControlsDanger() -> SettingsControlsDangerDriver {
        SettingsControlsDangerDriver(presenter: sdk.settingsControlsDanger())
    }

    func makeSettingsControlsSchedules() -> SettingsControlsSchedulesDriver {
        SettingsControlsSchedulesDriver(presenter: sdk.settingsControlsSchedules())
    func makeSettingsAlerts() -> SettingsServerAlertsDriver {
        SettingsServerAlertsDriver(presenter: sdk.settingsAlerts())
    }

    func makeSettingsBackups() -> SettingsServerBackupsDriver {
        SettingsServerBackupsDriver(presenter: sdk.settingsBackups())
    }

    func makeSettingsLogs() -> SettingsServerLogsDriver {
        SettingsServerLogsDriver(presenter: sdk.settingsLogs())
    func makeSettingsSignIn() -> SettingsSignInDriver {
        SettingsSignInDriver(presenter: sdk.settingsSignIn())
    // music
    func musicAccess() -> MusicAccessInfo {
        Mapping.musicAccess(sdk.musicAccess())
    }

    func musicOptions() -> MusicOptionLists {
        MusicOptionLists(
            albumChoices: Mapping.musicOptions(MusicOptions.shared.albumChoices),
            newAlbums: Mapping.musicOptions(MusicOptions.shared.newAlbums),
            albumTypes: Mapping.musicOptions(MusicOptions.shared.albumTypes),
            secondaryTypes: Mapping.musicOptions(MusicOptions.shared.secondaryTypes),
            statuses: Mapping.musicOptions(MusicOptions.shared.statuses),
            pressingRules: Mapping.musicOptions(MusicOptions.shared.pressingRules)
        )
    }

    func makeMusicArtist(mbid: String) -> MusicArtistDriver {
        MusicArtistDriver(presenter: sdk.musicArtist(mbid: mbid, utcOffsetSeconds: Self.utcOffset()))
    }

    func makeMusicAlbum(mbid: String, artist: String) -> MusicAlbumDriver {
        MusicAlbumDriver(presenter: sdk.musicAlbum(mbid: mbid, artist: artist))
    }

    func makeMusicRows(kind: String, slug: String, region: String) -> MusicRowsDriver {
        MusicRowsDriver(presenter: sdk.musicRows(kind: kind, slug: slug, region: region, utcOffsetSeconds: Self.utcOffset()))
    }

    func makeMusicNew(region: String) -> MusicNewDriver {
        MusicNewDriver(presenter: sdk.musicNew(region: region, utcOffsetSeconds: Self.utcOffset()))
    }

    func makeMusicBrowse(region: String) -> MusicBrowseDriver {
        MusicBrowseDriver(presenter: sdk.musicBrowse(region: region))
    }

    func makeMusicSearch() -> MusicSearchDriver {
        MusicSearchDriver(presenter: sdk.musicSearch())
    }

    func makeMusicAdd(mbid: String, name: String) -> MusicAddDriver {
        MusicAddDriver(presenter: sdk.musicAdd(mbid: mbid, name: name))
    }

    func makeMusicSettings() -> MusicSettingsDriver {
        MusicSettingsDriver(presenter: sdk.musicSettings())
    }

    func makeMusicUnmatched() -> MusicUnmatchedDriver {
        MusicUnmatchedDriver(presenter: sdk.musicUnmatched())
    }

    func makeMusicLibraryArtist(titleId: Int64) -> MusicLibraryArtistDriver {
        MusicLibraryArtistDriver(presenter: sdk.musicLibraryArtist(titleId: titleId, utcOffsetSeconds: Self.utcOffset()))
    }

    func makeMusicLibraryAlbum(titleId: Int64, albumId: Int64) -> MusicLibraryAlbumDriver {
        MusicLibraryAlbumDriver(presenter: sdk.musicLibraryAlbum(titleId: titleId, albumId: albumId, utcOffsetSeconds: Self.utcOffset()))
    }

    func makeMusicLeftovers(titleId: Int64) -> MusicLeftoversDriver {
        MusicLeftoversDriver(presenter: sdk.musicLeftovers(titleId: titleId))
    }

    func makeMusicRename(titleId: Int64) -> RenameDriver {
        RenameDriver(presenter: sdk.musicRename(titleId: titleId))
    }

    func makeMusicRemove(titleId: Int64, name: String, albumId: Int64) -> RemoveDriver {
        RemoveDriver(presenter: sdk.musicRemove(titleId: titleId, name: name, albumId: albumId))
    }

    private static func utcOffset() -> Int32 {
        Int32(TimeZone.current.secondsFromGMT())
    }
}
