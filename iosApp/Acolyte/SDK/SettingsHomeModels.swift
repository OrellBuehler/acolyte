import Foundation

enum SettingsPageID: String, CaseIterable, Hashable, Sendable, Identifiable {
    case libraryFolders = "library_folders"
    case titleDefaults = "title_defaults"
    case music = "music"
    case subtitles = "subtitles"
    case naming = "naming"
    case parts = "parts"
    case outsideLists = "outside_lists"
    case move = "move"
    case downloaders = "downloaders"
    case sources = "sources"
    case pickRules = "pick_rules"
    case search = "search"
    case schedules = "schedules"
    case users = "users"
    case requests = "requests"
    case signIn = "sign_in"
    case appearance = "appearance"
    case mediaServers = "media_servers"
    case alerts = "alerts"
    case ai = "ai"
    case api = "api"
    case remoteAccess = "remote_access"
    case metadata = "metadata"
    case health = "health"
    case backups = "backups"
    case logs = "logs"
    case thisApp = "this_app"
    case danger = "danger"
    case account = "account"

    var id: Self { self }

    var labelKey: String { "settings.page." + rawValue.replacingOccurrences(of: "_", with: ".") }

    var aboutKey: String { labelKey + ".about" }

    var symbol: String {
        switch self {
        case .libraryFolders: "folder"
        case .titleDefaults: "tv"
        case .music: "music.note"
        case .subtitles: "captions.bubble"
        case .naming: "textformat"
        case .parts: "square.grid.2x2"
        case .outsideLists: "tray"
        case .move: "square.and.arrow.down"
        case .downloaders: "arrow.down.circle"
        case .sources: "antenna.radiowaves.left.and.right"
        case .pickRules: "star"
        case .search: "magnifyingglass"
        case .schedules: "clock"
        case .users: "person.2"
        case .requests: "checklist"
        case .signIn: "lock.shield"
        case .appearance: "paintpalette"
        case .mediaServers: "server.rack"
        case .alerts: "bell"
        case .ai: "sparkles"
        case .api: "key"
        case .remoteAccess: "globe"
        case .metadata: "text.magnifyingglass"
        case .health: "checkmark.circle"
        case .backups: "archivebox"
        case .logs: "doc.text"
        case .thisApp: "info.circle"
        case .danger: "exclamationmark.triangle"
        case .account: "person.crop.circle"
        }
    }

    var route: Route? {
        switch self {
        case .libraryFolders: .settingsLibraryFolders
        case .titleDefaults: .settingsTitleDefaults
        case .music: .settingsMusic
        case .subtitles: .settingsSubtitles
        case .naming: .settingsNaming
        case .parts: .settingsParts
        case .outsideLists: .settingsOutsideLists
        case .move: .settingsMove
        case .downloaders: .settingsDownloaders
        case .sources: .settingsSources
        case .pickRules: .settingsPickRules
        case .search: .settingsSearch
        case .schedules: .settingsSchedules
        case .requests: .settingsRequests
        case .signIn: .settingsSignIn
        case .appearance: .settingsAppearance
        case .mediaServers: .settingsMediaServers
        case .alerts: .settingsAlerts
        case .ai: .settingsAi
        case .api: .settingsApi
        case .remoteAccess: .settingsRemoteAccess
        case .metadata: .settingsMetadata
        case .health: .settingsHealth
        case .backups: .settingsBackups
        case .logs: .settingsLogs
        case .thisApp: .settingsThisApp
        case .danger: .settingsDanger
        case .users, .account: nil
        }
    }

    var section: SectionID? {
        switch self {
        case .users: .people
        case .account: .account
        default: nil
        }
    }
}

enum SettingsGroupID: String, Hashable, Sendable, Identifiable {
    case library
    case downloads
    case users
    case connections
    case system
    case account

    var id: Self { self }

    var titleKey: String { "settings.group." + rawValue }

    var lineKey: String { "settings.group." + rawValue + ".line" }
}

enum SettingsStandingKind: String, Hashable, Sendable {
    case ok
    case attention
    case unset
    case unknown
}

enum SettingsReasonKind: String, Hashable, Sendable {
    case setting
    case update
    case decisions
    case requests
}

struct SettingsHomePageInfo: Identifiable, Hashable, Sendable {
    let page: SettingsPageID
    let standing: SettingsStandingKind
    let count: Int

    var id: SettingsPageID { page }
}

struct SettingsHomeGroupInfo: Identifiable, Hashable, Sendable {
    let group: SettingsGroupID
    let pages: [SettingsHomePageInfo]
    let standing: SettingsStandingKind
    let count: Int

    var id: SettingsGroupID { group }
}

struct SettingsReasonInfo: Identifiable, Hashable, Sendable {
    let key: String
    let kind: SettingsReasonKind
    let tone: ToneKind
    let page: SettingsPageID?
    let title: String
    let body: String
    let count: Int

    var id: String { key }
}

struct SettingsHomeInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let groups: [SettingsHomeGroupInfo]
    let accountPages: [SettingsPageID]
    let reasons: [SettingsReasonInfo]
    let showFinishSetup: Bool
    let needsAttention: Bool

    var pages: [SettingsPageID] { groups.flatMap { $0.pages.map(\.page) } + accountPages }

    static let initial = SettingsHomeInfo(
        load: .loading,
        allowed: false,
        groups: [],
        accountPages: [],
        reasons: [],
        showFinishSetup: false,
        needsAttention: false
    )
}

struct SettingsSearchText: Hashable, Sendable {
    let key: String
    let label: String
    let about: String
    let group: String
}
