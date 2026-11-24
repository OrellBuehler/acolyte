import Foundation

enum SourceWordKind: String, Hashable, Sendable {
    case slowed
    case down
    case blocked
    case keyRefused = "keyrefused"
    case working
    case unreachable
    case keyTurnedDown = "keyturneddown"
    case paused
    case other
}

enum SourceCauseKind: String, Hashable, Sendable {
    case nameNotFound = "namenotfound"
    case refused
    case timeout
    case certificate
    case useHttp = "usehttp"
    case useHttps = "usehttps"
    case auth
    case wrongPath = "wrongpath"
    case wrongApp = "wrongapp"
    case categoryMissing = "categorymissing"
    case localNetwork = "localnetwork"
}

enum SourceOpKind: String, Hashable, Sendable {
    case caps
    case search
    case feed
    case test
    case other
}

enum SourceErrorWordKind: String, Hashable, Sendable {
    case down
    case rateLimited = "ratelimited"
    case loginRefused = "loginrefused"
    case brokenDefinition = "brokendefinition"
    case blocked
    case other
}

enum SourceProblemKind: String, Hashable, Sendable {
    case name
    case url
    case urlQuery = "urlquery"
    case priority
    case seeders
    case searches
    case window
    case gap
    case timeout
    case proxy
    case siteUser = "siteuser"
    case siteAgent = "siteagent"
    case languages
}

enum SourceKeyKindInfo: String, Hashable, Sendable {
    case none
    case apiKey = "apikey"
    case passkey
    case cookie
}

enum SourceAddressHintKind: String, Hashable, Sendable {
    case feed
    case rss
    case site
}

enum SourceCheckWordKind: String, Hashable, Sendable {
    case connected
    case notConnected = "notconnected"
    case paused
}

enum SourceNoticeKind: String, Hashable, Sendable {
    case saved
    case pauseFailed = "pausefailed"
    case resumeFailed = "resumefailed"
    case removeFailed = "removefailed"
    case bridgeConnected = "bridgeconnected"
    case bridgeDisconnected = "bridgedisconnected"
    case bridgeSynced = "bridgesynced"
    case bridgeUpToDate = "bridgeuptodate"
    case bridgeSyncingAgain = "bridgesyncingagain"
    case bridgeSyncFailed = "bridgesyncfailed"
}

struct SourceNoticeInfo: Hashable, Sendable {
    let seq: Int
    let kind: SourceNoticeKind
    let name: String
    let detail: String
    let count: Int
    let added: Int
    let removed: Int
    let changed: Int
    let error: ErrorInfo?
}

struct SourceOutcomeInfo: Hashable, Sendable {
    let ok: Bool
    let text: String
    let ms: Int
    let items: Int
    let cause: SourceCauseKind?
    let botCheck: Bool
    let error: ErrorInfo?
}

struct SourceRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let formatLabel: String
    let format: String
    let protocolName: String
    let priority: Int
    let state: String
    let word: SourceWordKind
    let rawWord: String
    let tone: ToneKind
    let note: String
    let noteBad: Bool
    let showNote: Bool
    let until: String
    let botCheckHint: Bool
    let cause: SourceCauseKind?
    let avgMs: Int
    let calls24h: Int
    let failures24h: Int
    let lastSuccess: String
    let lastFailure: String
    let feed: Bool
    let autoSearch: Bool
    let handSearch: Bool
    let testing: Bool
    let busy: Bool
    let test: SourceOutcomeInfo?
    let isDisabled: Bool
    let canTryNow: Bool
    let showAvg: Bool
    let showUntil: Bool
}

struct SourceDownloaderInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let protocolName: String
}

struct SourceSiteInfo: Hashable, Sendable, Identifiable {
    let id: String
    let name: String
}

struct SourceFormInfo: Hashable, Sendable {
    var id: Int64
    var isNew: Bool
    var name: String
    var format: String
    var site: String
    var siteUser: String
    var siteLogin: String
    var siteAgent: String
    var protocolName: String
    var url: String
    var apiKey: String
    var feed: Bool
    var autoSearch: Bool
    var handSearch: Bool
    var kinds: [String]
    var minSeeders: String
    var priority: String
    var searchesPerWindow: String
    var windowHours: String
    var gapMs: String
    var timeoutSeconds: String
    var allowLocalNetwork: Bool
    var proxy: String
    var proxyPassword: String
    var proxyAllowLocalNetwork: Bool
    var tags: [String]
    var multiLanguages: String
    var noLanguageLanguages: String
    var downloaderId: Int64
    var sendGone: Bool
    var originalProtocol: String
    var originalDownloaderId: Int64
    var isSite: Bool
    var isRss: Bool
    var showSiteFormat: Bool
    var formatLocked: Bool
    var siteLocked: Bool
    var showLoginChoice: Bool
    var showUser: Bool
    var showAgent: Bool
    var showProtocol: Bool
    var showSearchUse: Bool
    var showSeeders: Bool
    var sendProtocol: String
    var keyKind: SourceKeyKindInfo
    var addressHint: SourceAddressHintKind
    var newestListWorks: Bool
    var canSave: Bool
    var problems: [SourceProblemKind]

    func problem(_ kind: SourceProblemKind, shown: Bool) -> Bool {
        shown && problems.contains(kind)
    }
}

struct SourcesInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canEdit: Bool
    let partOff: Bool
    let rows: [SourceRowInfo]
    let downloaders: [SourceDownloaderInfo]?
    let editing: SourceFormInfo?
    let editToken: Int
    let saving: Bool
    let saveError: ErrorInfo?
    let notice: SourceNoticeInfo?
    let hasRows: Bool
    let canAdd: Bool

    static let initial = SourcesInfo(
        load: .loading,
        allowed: true,
        canEdit: false,
        partOff: false,
        rows: [],
        downloaders: nil,
        editing: nil,
        editToken: 0,
        saving: false,
        saveError: nil,
        notice: nil,
        hasRows: false,
        canAdd: false
    )
}

struct BridgeRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let kind: String
    let url: String
    let callbackUrl: String
    let lastCheck: String
    let lastError: String
    let sites: Int
    let sources: Int
}

struct BridgeDraftInfo: Hashable, Sendable {
    var kind: String
    var url: String
    var key: String
    var callbackUrl: String
    var newKeys: Bool
    var sonarrKey: String
    var radarrKey: String
    var editingId: Int64

    var isProwlarr: Bool { kind == "prowlarr" }

    var canSubmit: Bool {
        !url.trimmingCharacters(in: .whitespaces).isEmpty && !key.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var defaultPort: String { isProwlarr ? "9696" : "9117" }

    static let blank = BridgeDraftInfo(
        kind: "jackett", url: "", key: "", callbackUrl: "", newKeys: false, sonarrKey: "", radarrKey: "", editingId: 0
    )
}

struct BridgeResultInfo: Hashable, Sendable {
    let ok: Bool
    let version: String
    let siteCount: Int
    let sites: [String]
    let text: String
}

struct BridgeFormInfo: Hashable, Sendable {
    let initial: BridgeDraftInfo
    let testing: Bool
    let connecting: Bool
    let result: BridgeResultInfo?
    let error: ErrorInfo?
    let kindError: String
    let urlError: String
    let keyError: String
    let callbackError: String
    let keysNeeded: [String]
    let needsReauth: Bool
    let canReauth: Bool
    let reauthBusy: Bool
    let reauthError: ErrorInfo?

    var busy: Bool { testing || connecting }
}

struct BridgeKeyInfo: Hashable, Sendable, Identifiable {
    let side: String
    let key: String

    var id: String { side }
}

struct BridgesInfo: Hashable, Sendable {
    let load: LoadInfo
    let canEdit: Bool
    let bridges: [BridgeRowInfo]
    let form: BridgeFormInfo?
    let formToken: Int
    let syncing: Int64
    let removing: Bool
    let removeError: ErrorInfo?
    let madeKeys: [BridgeKeyInfo]
    let notice: SourceNoticeInfo?
    let changed: Int

    static let initial = BridgesInfo(
        load: .loading,
        canEdit: false,
        bridges: [],
        form: nil,
        formToken: 0,
        syncing: 0,
        removing: false,
        removeError: nil,
        madeKeys: [],
        notice: nil,
        changed: 0
    )
}

struct SourceCheckLineInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let word: SourceCheckWordKind
    let tone: ToneKind
    let count: Int
    let ms: Int
    let detail: String
}

struct SourceCheckResultInfo: Hashable, Sendable {
    let lines: [SourceCheckLineInfo]
    let total: Int
    let connected: Int
    let paused: Int
    let failed: Int
}

struct SourceCheckInfo: Hashable, Sendable {
    let running: Bool
    let result: SourceCheckResultInfo?
    let error: ErrorInfo?

    static let initial = SourceCheckInfo(running: false, result: nil, error: nil)
}

struct SourceBarInfo: Hashable, Sendable, Identifiable {
    let day: String
    let calls: Int
    let failures: Int
    let answered: Int
    let grabs: Int
    let avgMs: Int
    let callsPercent: Int
    let failedPercent: Int
    let grabsPercent: Int
    let timePercent: Int

    var id: String { day }
}

struct SourceCallInfo: Hashable, Sendable, Identifiable {
    let at: String
    let op: SourceOpKind
    let rawOp: String
    let ok: Bool
    let ms: Int
    let errorKind: SourceErrorWordKind
    let rawError: String

    var id: String { at + rawOp + String(ms) }
}

struct SourceHealthInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canEdit: Bool
    let row: SourceRowInfo?
    let days: Int
    let bars: [SourceBarInfo]
    let recent: [SourceCallInfo]
    let totalCalls: Int
    let totalFailures: Int
    let totalGrabs: Int
    let trying: Bool
    let hasCalls: Bool
    let hasTimes: Bool
    let choices: [Int]

    static let initial = SourceHealthInfo(
        load: .loading,
        allowed: true,
        canEdit: false,
        row: nil,
        days: 14,
        bars: [],
        recent: [],
        totalCalls: 0,
        totalFailures: 0,
        totalGrabs: 0,
        trying: false,
        hasCalls: false,
        hasTimes: false,
        choices: [7, 14, 30, 90]
    )
}

extension ErrorInfo {
    var sourceText: String {
        let server = kind == .badRequest || kind == .conflict || kind == .server
        return server && !message.isEmpty ? message : localized
    }
}
