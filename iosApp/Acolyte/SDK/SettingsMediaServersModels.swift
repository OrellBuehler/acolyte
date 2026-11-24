import Foundation

struct SettingsMediaServersTagInfo: Hashable, Sendable {
    let ok: Bool
    let label: String
    let libraries: Int
    let hover: String
    let failure: String
    let error: ErrorInfo?
    let helpUrl: String
}

struct SettingsMediaServersRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let kindName: String
    let url: String
    let enabled: Bool
    let onImport: Bool
    let onRename: Bool
    let onDelete: Bool
    let toldNothing: Bool
    let pathFrom: String
    let pathTo: String
    let hasPathMap: Bool
    let showsFailure: Bool
    let showsLastError: Bool
    let showsLastChecked: Bool
    let lastError: String
    let lastOkAt: Date?
    let tag: SettingsMediaServersTagInfo?
    let testing: Bool
    let busy: Bool
}

struct SettingsMediaServersKindOptionInfo: Hashable, Sendable, Identifiable {
    let wire: String
    let name: String

    var id: String { wire }
}

struct SettingsMediaServersFormInfo: Hashable, Sendable {
    let id: Int64
    let kind: String
    let name: String
    let originalName: String
    let url: String
    let username: String
    let token: String
    let tokenStored: Bool
    let webUrl: String
    let plainPassword: Bool
    let onImport: Bool
    let onRename: Bool
    let onDelete: Bool
    let pathFrom: String
    let pathTo: String
    let allowLocalNetwork: Bool
    let enabled: Bool
    let usePlexSignIn: Bool
    let isNew: Bool
    let isPlex: Bool
    let isSubsonic: Bool
    let isKodi: Bool
    let takesUsername: Bool
    let takesWebUrl: Bool
    let takesPathMap: Bool
    let showsToken: Bool
    let urlExample: String
}

struct SettingsMediaServersPlexRowInfo: Hashable, Sendable, Identifiable {
    let index: Int
    let name: String
    let address: String
    let place: String

    var id: Int { index }
}

struct SettingsMediaServersPlexInfo: Hashable, Sendable {
    let finding: Bool
    let searched: Bool
    let rows: [SettingsMediaServersPlexRowInfo]
    let picked: Int
    let notLinked: Bool
    let error: ErrorInfo?
    let addresses: [String]

    static let initial = SettingsMediaServersPlexInfo(
        finding: false, searched: false, rows: [], picked: -1, notLinked: false, error: nil, addresses: []
    )
}

struct SettingsMediaServersLanguageInfo: Hashable, Sendable, Identifiable {
    let code: String
    let name: String

    var id: String { code }
}

struct SettingsMediaServersTrackRowInfo: Hashable, Sendable, Identifiable {
    let index: Int
    let code: String
    let name: String
    let options: [SettingsMediaServersLanguageInfo]
    let canUp: Bool
    let canDown: Bool

    var id: Int { index }
}

enum SettingsMediaServersNoticeKindInfo: String, Hashable, Sendable {
    case saved
    case removeFailed = "removefailed"
    case watchOn = "watchon"
    case watchOff = "watchoff"
    case watchFailed = "watchfailed"
    case labelsSaved = "labelssaved"
    case labelsFailed = "labelsfailed"
    case labelsStarted = "labelsstarted"
    case labelsStartFailed = "labelsstartfailed"
    case tracksSaved = "trackssaved"
    case tracksSavedOn = "trackssavedon"
    case tracksFailed = "tracksfailed"
}

struct SettingsMediaServersNoticeInfo: Hashable, Sendable {
    let id: Int64
    let kind: SettingsMediaServersNoticeKindInfo
    let name: String
    let error: ErrorInfo?
    let detail: String
}

struct SettingsMediaServersInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let isEmpty: Bool
    let servers: [SettingsMediaServersRowInfo]
    let kinds: [SettingsMediaServersKindOptionInfo]
    let form: SettingsMediaServersFormInfo?
    let formRevision: Int
    let formError: ErrorInfo?
    let formDetail: String
    let canSave: Bool
    let saving: Bool
    let plex: SettingsMediaServersPlexInfo
    let showsPlexPicker: Bool
    let showWatch: Bool
    let watchOn: Bool
    let watchBusy: Bool
    let showLabels: Bool
    let labelsEnabled: Bool
    let labelsText: String
    let labelsAskers: Bool
    let labelsSavedOn: Bool
    let labelsCanSave: Bool
    let labelsCanRun: Bool
    let showTracks: Bool
    let tracksEnabled: Bool
    let audioRows: [SettingsMediaServersTrackRowInfo]
    let subtitleRows: [SettingsMediaServersTrackRowInfo]
    let canAddAudio: Bool
    let canAddSubtitle: Bool
    let tracksCanSave: Bool
    let cardsRevision: Int
    let notice: SettingsMediaServersNoticeInfo?
    let reauth: SettingsReauthInfo

    static let initial = SettingsMediaServersInfo(
        load: .loading, allowed: true, isEmpty: false, servers: [], kinds: [], form: nil, formRevision: 0, formError: nil,
        formDetail: "", canSave: false, saving: false, plex: .initial, showsPlexPicker: false, showWatch: false, watchOn: false,
        watchBusy: false, showLabels: false, labelsEnabled: false, labelsText: "", labelsAskers: false, labelsSavedOn: false,
        labelsCanSave: false, labelsCanRun: false, showTracks: false, tracksEnabled: false, audioRows: [], subtitleRows: [],
        canAddAudio: false, canAddSubtitle: false, tracksCanSave: false, cardsRevision: 0, notice: nil, reauth: .idle
    )
}
