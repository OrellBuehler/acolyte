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
        case .health:
            HealthView()
        case .account:
            AccountView(account: account)
        default:
            PlaceholderView(section: section)
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

enum Route: Hashable, Sendable {
    case title(kind: String, tmdb: String)
    case libraryTools
    case libraryDeleted(titleId: Int64)
    case libraryCopies
    case libraryReleases(turnedDown: Bool)
    case librarySubtitles
    case libraryRename
    case libraryImport
    case libraryImportFiles(path: String)
    case settingsLibraryFolders
}

struct RouteScreen: View {
    let route: Route
    let account: AccountInfo

    var body: some View {
        switch route {
        case let .title(kind, tmdb):
            TitlePlaceholderView(kind: kind, tmdb: tmdb)
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
        }
    }
}

struct TitlePlaceholderView: View {
    let kind: String
    let tmdb: String

    var body: some View {
        EmptyStateView(title: L10n.tr("placeholder.title"), message: L10n.tr("placeholder.message"), systemImage: kind == "show" ? "tv" : "film")
    }
}
