import Observation
import SwiftUI

struct TVAskTarget: Identifiable, Hashable {
    let kind: String
    let tmdb: String
    let fourK: Bool

    var id: String { "\(kind)-\(tmdb)-\(fourK)" }
}

@MainActor
@Observable
final class TVTabSelection {
    var value: SectionID

    init(_ value: SectionID) {
        self.value = value
    }
}

@MainActor
@Observable
final class TVRouter {
    var path = NavigationPath()
    var ask: TVAskTarget?

    func open(_ route: Route) {
        switch route {
        case let .ask(kind, tmdb, fourK):
            ask = TVAskTarget(kind: kind, tmdb: tmdb, fourK: fourK)
        default:
            path.append(route)
        }
    }
}

@MainActor
@Observable
final class TVNavigator {
    let account: AccountInfo
    let router: TVRouter
    private let tabs: TVTabSelection

    init(account: AccountInfo, router: TVRouter, tabs: TVTabSelection) {
        self.account = account
        self.router = router
        self.tabs = tabs
    }

    func open(_ route: Route) {
        router.open(route)
    }

    func open(_ section: SectionID) {
        if TVSections.tabs(for: account).contains(section) {
            tabs.value = section
        } else {
            router.path.append(section)
        }
    }

    func openCard(_ card: DiscoverCardInfo) {
        guard card.opensTitle else {
            if card.kind == .artist { open(.musicArtist(mbid: card.tmdb)) }
            return
        }
        open(.title(kind: card.kind.wire, tmdb: card.tmdb))
    }
}

enum TVSections {
    static let order: [SectionID] = [.home, .explore, .search, .requests, .calendar, .downloads]

    static func visible(_ section: SectionID, for account: AccountInfo) -> Bool {
        account.tabs.contains(section) || account.moreGroups.contains { $0.sections.contains(section) }
    }

    static func tabs(for account: AccountInfo) -> [SectionID] {
        order.filter { visible($0, for: account) } + [.settings]
    }
}

struct TVTabHost<Root: View>: View {
    let account: AccountInfo
    @ViewBuilder var root: () -> Root
    @State private var router: TVRouter
    @State private var navigator: TVNavigator

    init(account: AccountInfo, tabs: TVTabSelection, @ViewBuilder root: @escaping () -> Root) {
        self.account = account
        self.root = root
        let router = TVRouter()
        _router = State(initialValue: router)
        _navigator = State(initialValue: TVNavigator(account: account, router: router, tabs: tabs))
    }

    var body: some View {
        NavigationStack(path: $router.path) {
            root()
                .navigationDestination(for: SectionID.self) { TVSectionScreen(section: $0, account: account) }
                .navigationDestination(for: Route.self) { TVRouteScreen(route: $0) }
        }
        .environment(navigator)
        .environment(router)
        .fullScreenCover(item: $router.ask) { target in
            TVAskView(kind: target.kind, tmdb: target.tmdb, fourK: target.fourK)
        }
    }
}

struct TVSectionScreen: View {
    let section: SectionID
    let account: AccountInfo

    var body: some View {
        switch section {
        case .home: TVHomeView()
        case .explore: TVExploreView(account: account)
        case .search: TVSearchView(account: account)
        case .requests: TVRequestsView(account: account)
        case .calendar: TVCalendarView(account: account)
        case .downloads: TVDownloadsView(account: account)
        case .popular: TVPopularView(account: account)
        case .settings: TVSettingsView(account: account)
        default: TVOpenOnPhoneView(what: section.title)
        }
    }
}

struct TVRouteScreen: View {
    let route: Route

    var body: some View {
        switch route {
        case let .title(kind, tmdb):
            TVTitleView(kind: kind, tmdb: tmdb, libraryId: 0)
        case let .libraryTitle(id, kind, tmdb), let .libraryTab(id, kind, tmdb):
            TVTitleView(kind: kind, tmdb: tmdb, libraryId: id)
        case let .person(id):
            TVPersonView(id: id)
        case let .collection(id):
            TVCollectionView(id: id)
        case let .genre(type, genre):
            TVGenreView(type: type, genre: genre)
        case let .browse(link):
            TVBrowseListView(link: link)
        default:
            TVOpenOnPhoneView(what: TVRouteName.title(route))
        }
    }
}

enum TVRouteName {
    static func title(_ route: Route) -> String {
        switch route {
        case .person: L10n.tr("tv.phone.what.person")
        case .collection: L10n.tr("tv.phone.what.collection")
        case .genre, .browse, .hidden, .exploreStats: SectionID.explore.title
        case .musicArtist, .musicAlbum, .musicGenre, .musicPage, .musicLibraryArtist, .musicLibraryAlbum, .musicAdd, .askMusic, .settingsMusic:
            SectionID.music.title
        case .downloads: SectionID.downloads.title
        case .history: SectionID.history.title
        case .reports, .reportProblem: L10n.tr("tv.phone.what.reports")
        case .ask, .settingsRequests, .settingsOutsideLists, .settingsNeverAdd: SectionID.requests.title
        case .libraryTools, .libraryDeleted, .libraryCopies, .libraryReleases, .librarySubtitles, .libraryRename, .libraryImport, .libraryImportFiles:
            SectionID.library.title
        case .title, .libraryTitle, .libraryTab: L10n.tr("tv.phone.what.other")
        default: SectionID.settings.title
        }
    }
}

struct TVOpenOnPhoneView: View {
    let what: String

    var body: some View {
        VStack(spacing: 28) {
            Image(systemName: "iphone")
                .font(.system(size: 96))
                .foregroundStyle(Theme.textMuted)
                .accessibilityHidden(true)
            Text(L10n.tr("tv.phone.title"))
                .font(.title2.bold())
                .foregroundStyle(Theme.text)
            Text(L10n.format("tv.phone.body", what))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
                .frame(maxWidth: Theme.TV.readableWidth)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct TVPlaceholderView: View {
    let section: SectionID

    var body: some View {
        VStack(spacing: 28) {
            Image(systemName: section.symbol)
                .font(.system(size: 96))
                .foregroundStyle(Theme.textMuted)
                .accessibilityHidden(true)
            Text(section.title)
                .font(.title2.bold())
                .foregroundStyle(Theme.text)
            Text(L10n.format("tv.placeholder.body", section.title))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
                .frame(maxWidth: Theme.TV.readableWidth)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
