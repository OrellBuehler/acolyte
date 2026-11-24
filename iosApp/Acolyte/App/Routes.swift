import SwiftUI

enum TabID: Hashable {
    case section(SectionID)
    case more
}

extension SectionID {
    var title: String {
        switch self {
        case .home: L10n.tr("section.home")
        case .explore: L10n.tr("section.explore")
        case .library: L10n.tr("section.library")
        case .search: L10n.tr("section.search")
        case .popular: L10n.tr("section.popular")
        case .movies: L10n.tr("section.movies")
        case .shows: L10n.tr("section.shows")
        case .music: L10n.tr("section.music")
        case .stats: L10n.tr("section.stats")
        case .downloads: L10n.tr("section.downloads")
        case .calendar: L10n.tr("section.calendar")
        case .requests: L10n.tr("section.requests")
        case .history: L10n.tr("section.history")
        case .health: L10n.tr("section.health")
        case .people: L10n.tr("section.people")
        case .settings: L10n.tr("section.settings")
        case .account: L10n.tr("section.account")
        }
    }

    var symbol: String {
        switch self {
        case .home: "house"
        case .explore: "safari"
        case .library: "books.vertical"
        case .search: "magnifyingglass"
        case .popular: "sparkles"
        case .movies: "film"
        case .shows: "tv"
        case .music: "music.note"
        case .stats: "gauge.with.dots.needle.33percent"
        case .downloads: "arrow.down.to.line"
        case .calendar: "calendar"
        case .requests: "checklist"
        case .history: "clock"
        case .health: "shield"
        case .people: "person.2"
        case .settings: "gearshape"
        case .account: "person.crop.circle"
        }
    }
}

extension MoreTabKind {
    var title: String {
        switch self {
        case .more: L10n.tr("nav.more")
        case .manage: L10n.tr("nav.manage")
        }
    }

    var symbol: String {
        switch self {
        case .more: "ellipsis"
        case .manage: "tray"
        }
    }
}

extension SectionGroupID {
    var title: String? {
        switch self {
        case .discover: nil
        case .library: L10n.tr("group.library")
        case .yours: L10n.tr("group.yours")
        case .activity: L10n.tr("group.activity")
        case .system: L10n.tr("group.system")
        case .you: L10n.tr("group.you")
        }
    }
}

struct SectionScreen: View {
    let section: SectionID
    let account: AccountInfo

    var body: some View {
        switch section {
        case .library:
            LibraryView(kind: nil)
        case .movies:
            LibraryView(kind: .movie)
        case .shows:
            LibraryView(kind: .show)
        case .stats:
            LibraryStatsView()
        case .home:
            HomeView(account: account)
        case .explore:
            ExploreView(account: account)
        case .search:
            SearchView(account: account)
        case .popular:
            PopularView(account: account)
        case .health:
            HealthView()
        case .requests:
            RequestsView(account: account)
        case .account:
            AccountView(account: account)
        case .downloads:
            DownloadsView(show: "", open: "")
        case .calendar:
            CalendarView()
        case .history:
            HistoryView(titleId: 0, episodeId: 0)
        default:
            PlaceholderView(section: section)
        }
    }
}

enum Route: Hashable, Sendable, Identifiable {
    case title(kind: String, tmdb: String)
    case libraryTitle(id: Int64, kind: String, tmdb: String)
    case person(id: String)
    case collection(id: String)
    case libraryTools
    case libraryDeleted(titleId: Int64)
    case libraryCopies
    case libraryReleases(turnedDown: Bool)
    case librarySubtitles
    case libraryRename
    case libraryImport
    case libraryImportFiles(path: String)
    case settingsLibraryFolders
    case libraryTab(id: Int64, kind: String, tmdb: String)
    case downloads(show: String, open: String)
    case history(titleId: Int64, episodeId: Int64)
    case ask(kind: String, tmdb: String, fourK: Bool = false)
    case askMusic(artist: String, name: String = "", album: String = "", edition: String = "")
    case reports
    case reportProblem(titleId: Int64, name: String = "", season: Int = -1, itemId: Int64 = 0, albumId: Int64 = 0, versionId: Int64 = 0)
    case settingsRequests
    case settingsOutsideLists
    case settingsNeverAdd
    case genre(type: ExploreTypeID, genre: Int)
    case browse(BrowseLinkInfo)
    case hidden
    case exploreStats

    var id: Self { self }

    var isSheet: Bool {
        switch self {
        case .ask, .askMusic, .reportProblem: true
        default: false
        }
    }
}

@MainActor
@Observable
final class Router {
    var path = NavigationPath()
    var sheet: Route?

    func open(_ route: Route) {
        if route.isSheet {
            sheet = route
        } else {
            sheet = nil
            path.append(route)
        }
    }
}

struct OpenRouteAction: Sendable {
    let action: @MainActor @Sendable (Route) -> Void

    @MainActor
    func callAsFunction(_ route: Route) {
        action(route)
    }
}

extension EnvironmentValues {
    @Entry var openRoute = OpenRouteAction { _ in }
}

struct RoutedStack<Root: View>: View {
    let account: AccountInfo
    @ViewBuilder var root: () -> Root
    @State private var router: Router
    @State private var navigator: Navigator

    init(account: AccountInfo, tabs: TabSelection, @ViewBuilder root: @escaping () -> Root) {
        self.account = account
        self.root = root
        let router = Router()
        _router = State(initialValue: router)
        _navigator = State(initialValue: Navigator(account: account, router: router, tabs: tabs))
    }

    var body: some View {
        let current = router
        let open = OpenRouteAction { current.open($0) }
        NavigationStack(path: $router.path) {
            root()
                .navigationDestination(for: SectionID.self) { SectionScreen(section: $0, account: account) }
                .navigationDestination(for: Route.self) { RouteScreen(route: $0, account: account) }
        }
        .environment(\.openRoute, open)
        .environment(navigator)
        .sheet(item: $router.sheet) { route in
            RouteScreen(route: route, account: account)
                .environment(\.openRoute, open)
                .environment(navigator)
        }
    }
}

struct RouteScreen: View {
    let route: Route
    var account: AccountInfo?
    @Environment(Navigator.self) private var navigator

    private var resolved: AccountInfo { account ?? navigator.account }

    var body: some View {
        switch route {
        case let .title(kind, tmdb):
            TitleView(kind: kind, tmdb: tmdb, libraryId: 0)
        case let .libraryTitle(id, kind, tmdb):
            TitleView(kind: kind, tmdb: tmdb, libraryId: id)
        case let .person(id):
            PersonView(id: id, account: resolved)
        case let .collection(id):
            SeriesView(id: id)
        case .libraryTools:
            LibraryToolsView()
        case let .libraryDeleted(titleId):
            LibraryDeletedView(titleId: titleId)
        case .libraryCopies:
            LibraryCopiesView()
        case let .libraryReleases(turnedDown):
            LibraryReleasesView(turnedDown: turnedDown)
        case .librarySubtitles:
            LibrarySubtitlesView()
        case .libraryRename:
            LibraryRenameView()
        case .libraryImport:
            LibraryImportView()
        case let .libraryImportFiles(path):
            LibraryFilesView(path: path)
        case .settingsLibraryFolders:
            LibraryFoldersView()
        case let .libraryTab(id, kind, tmdb):
            TitleView(kind: kind, tmdb: tmdb, libraryId: id, openLibrary: true)
        case let .downloads(show, open):
            DownloadsView(show: show, open: open)
        case let .history(titleId, episodeId):
            HistoryView(titleId: titleId, episodeId: episodeId)
        case let .ask(kind, tmdb, fourK):
            AskSheetView(kind: kind, tmdb: tmdb, fourK: fourK)
        case let .askMusic(artist, name, album, edition):
            AskMusicView(artist: artist, name: name, album: album, edition: edition)
        case .reports:
            ReportsView()
        case let .reportProblem(titleId, name, season, itemId, albumId, versionId):
            ReportProblemView(titleId: titleId, name: name, season: season, itemId: itemId, albumId: albumId, versionId: versionId)
        case .settingsRequests:
            RequestRulesView()
        case .settingsOutsideLists:
            OutsideListsView()
        case .settingsNeverAdd:
            NeverAddView()
        case let .genre(type, genre):
            GenreView(type: type, genre: genre, account: resolved)
        case let .browse(link):
            BrowseView(link: link, account: resolved)
        case .hidden:
            HiddenView()
        case .exploreStats:
            ExploreStatsView()
        }
    }
}

struct PlaceholderView: View {
    let section: SectionID

    var body: some View {
        EmptyStateView(title: L10n.tr("placeholder.title"), message: L10n.tr("placeholder.message"), systemImage: section.symbol)
            .navigationTitle(section.title)
    }
}
