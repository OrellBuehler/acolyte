import Foundation

enum SettingsControlsActionKind: String, Hashable, Sendable {
    case partOn = "parton"
    case partOff = "partoff"
    case partRestart = "partrestart"
    case stagePause = "stagepause"
    case stageResume = "stageresume"
    case stageRestart = "stagerestart"
    case linkReconnect = "linkreconnect"
    case linkTest = "linktest"
    case appRestart = "apprestart"
    case appSafeMode = "appsafemode"
    case appShutdown = "appshutdown"
}

enum SettingsControlsStageActionKind: Hashable, Sendable {
    case pause
    case resume
    case restart
}

enum SettingsControlsRunKind: String, Hashable, Sendable {
    case off
    case starting
    case running
    case retrying
    case failed
    case stuck
}

enum SettingsControlsLinkType: String, Hashable, Sendable {
    case source
    case downloader
    case mediaServer = "mediaserver"
    case subtitleSite = "subtitlesite"
    case other
}

enum SettingsControlsGroupKind: String, Hashable, Sendable, CaseIterable {
    case library
    case copies
    case metadata
    case upkeep
}

enum SettingsControlsEveryKind: String, Hashable, Sendable {
    case always
    case days
    case hours
    case minutes
    case seconds
}

struct SettingsControlsEveryInfo: Hashable, Sendable {
    let kind: SettingsControlsEveryKind
    let count: Int
}

enum SettingsControlsWhenKind: String, Hashable, Sendable {
    case unplanned = "none"
    case turnedOff = "turnedoff"
    case partOff = "partoff"
    case safeMode = "safemode"
    case running
    case queued
    case now
    case minutes
    case hours
    case days
}

struct SettingsControlsWhenInfo: Hashable, Sendable {
    let kind: SettingsControlsWhenKind
    let count: Int
}

enum SettingsControlsDotKind: String, Hashable, Sendable {
    case ok
    case busy
    case fail
    case off
}

enum SettingsControlsReasonKind: String, Hashable, Sendable {
    case clear = "none"
    case nothing
    case settingOff = "settingoff"
    case nobody
    case starting
    case plexOff = "plexoff"
    case plexNobody = "plexnobody"
}

enum SettingsControlsStaleKind: String, Hashable, Sendable {
    case fresh = "none"
    case refreshOff = "refreshoff"
    case rescanOff = "rescanoff"
}

enum SettingsControlsUnitKind: String, Hashable, Sendable {
    case hours
    case minutes
}

enum SettingsControlsWaitKind: String, Hashable, Sendable {
    case stopping
    case waiting
    case back
}

struct SettingsControlsAskInfo: Hashable, Sendable {
    let kind: SettingsControlsActionKind
    let name: String
    let text: String
    let expiresInSeconds: Int
}

struct SettingsControlsNoticeInfo: Hashable, Sendable {
    let id: Int
    let kind: SettingsControlsActionKind
    let name: String
    let ok: Bool
    let detail: String
    let error: ErrorInfo?
}

struct SettingsControlsPartRowInfo: Hashable, Sendable, Identifiable {
    let key: String
    let part: String
    let name: String
    let on: Bool
    let run: SettingsControlsRunKind
    let tone: ToneKind
    let reason: String

    var id: String { key }
}

struct SettingsControlsStageRowInfo: Hashable, Sendable, Identifiable {
    let key: String
    let stage: String
    let name: String
    let run: SettingsControlsRunKind
    let tone: ToneKind
    let reason: String
    let pausable: Bool
    let paused: Bool

    var id: String { key }
}

struct SettingsControlsLinkRowInfo: Hashable, Sendable, Identifiable {
    let key: String
    let kind: SettingsControlsLinkType
    let name: String
    let run: SettingsControlsRunKind
    let tone: ToneKind
    let reason: String
    let canTest: Bool

    var id: String { key }
}

struct SettingsControlsEventRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let summary: String
    let at: Date?
}

struct SettingsControlsPartsInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let safeMode: Bool
    let parts: [SettingsControlsPartRowInfo]
    let stages: [SettingsControlsStageRowInfo]
    let links: [SettingsControlsLinkRowInfo]
    let events: [SettingsControlsEventRowInfo]
    let busyKey: String
    let ask: SettingsControlsAskInfo?
    let notice: SettingsControlsNoticeInfo?

    var busy: Bool { !busyKey.isEmpty }

    static let initial = SettingsControlsPartsInfo(
        load: .loading,
        allowed: true,
        safeMode: false,
        parts: [],
        stages: [],
        links: [],
        events: [],
        busyKey: "",
        ask: nil,
        notice: nil
    )
}

struct SettingsControlsStoppedInfo: Hashable, Sendable {
    let kind: SettingsControlsActionKind
    let text: String
    let wait: SettingsControlsWaitKind
}

struct SettingsControlsDangerInfo: Hashable, Sendable {
    let allowed: Bool
    let busyKind: SettingsControlsActionKind?
    let ask: SettingsControlsAskInfo?
    let notice: SettingsControlsNoticeInfo?
    let stopped: SettingsControlsStoppedInfo?

    var busy: Bool { busyKind != nil }

    static let initial = SettingsControlsDangerInfo(
        allowed: true,
        busyKind: nil,
        ask: nil,
        notice: nil,
        stopped: nil
    )
}

struct SettingsControlsPathRowInfo: Hashable, Sendable {
    let name: String
    let path: String
    let size: String
    let free: String
    let kind: String
    let freeKnown: Bool
}

struct SettingsControlsStorageInfo: Hashable, Sendable {
    let app: [SettingsControlsPathRowInfo]
    let folders: [SettingsControlsPathRowInfo]
    let savePath: String
}

struct SettingsControlsUrlBaseInfo: Hashable, Sendable {
    let saved: String
    let inUse: String
    let restartNeeded: Bool
}

struct SettingsControlsProxyFormInfo: Hashable, Sendable {
    let type: String
    let host: String
    let port: String
    let username: String
    let password: String
    let bypass: String
    let bypassLocal: Bool

    static let empty = SettingsControlsProxyFormInfo(
        type: "",
        host: "",
        port: "8080",
        username: "",
        password: "",
        bypass: "",
        bypassLocal: true
    )
}

enum SettingsControlsSavedNoteKind: String, Hashable, Sendable {
    case history
    case cache
    case countOn = "counton"
    case countOff = "countoff"
    case urlBase = "urlbase"
    case proxyOn = "proxyon"
    case proxyOff = "proxyoff"
    case failed
}

struct SettingsControlsSavedInfo: Hashable, Sendable {
    let id: Int
    let kind: SettingsControlsSavedNoteKind
    let value: Int
    let restartNeeded: Bool
    let error: ErrorInfo?
}

struct SettingsControlsThisAppInfo: Hashable, Sendable {
    let load: LoadInfo
    let canSettings: Bool
    let isOwner: Bool
    let version: String
    let uptime: String
    let schema: Int
    let liveClients: Int
    let uiLoaded: Bool
    let historyRoutineDays: Int
    let historyOtherDays: Int
    let cacheGb: Int
    let countInstall: Bool
    let uiBusy: Bool
    let routineChoices: [Int]
    let otherChoices: [Int]
    let cacheChoices: [Int]
    let storage: SettingsControlsStorageInfo?
    let storageProof: Bool
    let storageError: ErrorInfo?
    let urlBaseAvailable: Bool
    let urlBase: SettingsControlsUrlBaseInfo?
    let urlBaseDraft: String
    let urlBaseSaving: Bool
    let urlBaseError: ErrorInfo?
    let canSaveUrlBase: Bool
    let proxyAvailable: Bool
    let proxyLoaded: Bool
    let proxy: SettingsControlsProxyFormInfo
    let proxyPasswordSaved: Bool
    let proxySaving: Bool
    let proxyError: ErrorInfo?
    let proxyOn: Bool
    let proxyValid: Bool
    let canSaveProxy: Bool
    let saved: SettingsControlsSavedInfo?

    var canChangeHistory: Bool { isOwner }

    static let initial = SettingsControlsThisAppInfo(
        load: .loading,
        canSettings: false,
        isOwner: false,
        version: "",
        uptime: "",
        schema: 0,
        liveClients: 0,
        uiLoaded: false,
        historyRoutineDays: 30,
        historyOtherDays: 0,
        cacheGb: 5,
        countInstall: true,
        uiBusy: false,
        routineChoices: [],
        otherChoices: [],
        cacheChoices: [],
        storage: nil,
        storageProof: false,
        storageError: nil,
        urlBaseAvailable: true,
        urlBase: nil,
        urlBaseDraft: "",
        urlBaseSaving: false,
        urlBaseError: nil,
        canSaveUrlBase: false,
        proxyAvailable: true,
        proxyLoaded: false,
        proxy: .empty,
        proxyPasswordSaved: false,
        proxySaving: false,
        proxyError: nil,
        proxyOn: false,
        proxyValid: true,
        canSaveProxy: false,
        saved: nil
    )
}

enum SettingsControlsScheduleNoteKind: String, Hashable, Sendable {
    case started
    case queued
    case startFailed = "startfailed"
    case finished
    case nothing
    case jobFailed = "jobfailed"
    case saved
    case autoOn = "autoon"
    case autoOff = "autooff"
    case saveFailed = "savefailed"
}

struct SettingsControlsScheduleNoteInfo: Hashable, Sendable {
    let id: Int
    let kind: SettingsControlsScheduleNoteKind
    let label: String
    let detail: String
    let reason: SettingsControlsReasonKind
    let every: SettingsControlsEveryInfo
    let error: ErrorInfo?
}

struct SettingsControlsRunningRowInfo: Hashable, Sendable, Identifiable {
    let kind: String
    let label: String
    let running: Bool

    var id: String { kind }
}

struct SettingsControlsScheduleRowInfo: Hashable, Sendable, Identifiable {
    let kind: String
    let label: String
    let about: String
    let dot: SettingsControlsDotKind
    let every: SettingsControlsEveryInfo
    let lastAt: Date?
    let took: String
    let next: SettingsControlsWhenInfo
    let failed: Bool
    let failedError: String
    let reason: SettingsControlsReasonKind
    let reasonLinks: Bool
    let pageKey: String
    let canRun: Bool
    let canChange: Bool
    let spinning: Bool
    let enabled: Bool
    let toggleable: Bool
    let stale: SettingsControlsStaleKind
    let editing: Bool

    var id: String { kind }
}

struct SettingsControlsGroupRowsInfo: Hashable, Sendable, Identifiable {
    let group: SettingsControlsGroupKind
    let rows: [SettingsControlsScheduleRowInfo]

    var id: String { group.rawValue }
}

struct SettingsControlsLoopRowInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let reason: String
    let run: SettingsControlsRunKind
    let tone: ToneKind
    let dot: SettingsControlsDotKind
    let every: SettingsControlsEveryInfo
    let lastAt: Date?
    let next: SettingsControlsWhenInfo

    var id: String { key }
}

struct SettingsControlsEditorInfo: Hashable, Sendable {
    let kind: String
    let label: String
    let about: String
    let unit: SettingsControlsUnitKind
    let amount: String
    let minAmount: Int
    let maxAmount: Int
    let valid: Bool
    let toggleable: Bool
    let enabled: Bool
    let pageKey: String
    let saving: Bool
    let busy: Bool
    let error: ErrorInfo?
}

struct SettingsControlsSchedulesInfo: Hashable, Sendable {
    let load: LoadInfo
    let safeMode: Bool
    let canRun: Bool
    let canChange: Bool
    let running: [SettingsControlsRunningRowInfo]
    let groups: [SettingsControlsGroupRowsInfo]
    let loops: [SettingsControlsLoopRowInfo]
    let editor: SettingsControlsEditorInfo?
    let busyKind: String
    let notice: SettingsControlsScheduleNoteInfo?

    static let initial = SettingsControlsSchedulesInfo(
        load: .loading,
        safeMode: false,
        canRun: false,
        canChange: false,
        running: [],
        groups: [],
        loops: [],
        editor: nil,
        busyKind: "",
        notice: nil
    )
}
