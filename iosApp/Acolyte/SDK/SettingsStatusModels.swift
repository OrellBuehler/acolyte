import Foundation

enum StatusGroupID: String, Hashable, Sendable {
    case sources
    case downloaders
    case library
    case media
    case subtitles
    case lists
    case alerts
    case metadata
    case app
    case jobs
}

enum StatusWordID: String, Hashable, Sendable {
    case working
    case warning
    case broken
    case stuck
    case starting
    case retrying
    case off
    case notSynced = "notsynced"
    case notSent = "notsent"
    case turnedOff = "turnedoff"
    case partOff = "partoff"
    case safeMode = "safemode"
    case running
    case failed
    case late
    case notRun = "notrun"
    case cancelled
    case ok
}

enum StatusTitleID: String, Hashable, Sendable {
    case plain
    case folder
    case freeSpace = "freespace"
    case backups
    case update
    case configDisk = "configdisk"
    case configFolder = "configfolder"
    case waitingDownloads = "waitingdownloads"
    case clearWaiting = "clearwaiting"
    case saveFolder = "savefolder"
    case torrentDownloader = "torrentdownloader"
    case usenetDownloader = "usenetdownloader"
    case sameDownloader = "samedownloader"
    case peopleCheck = "peoplecheck"
    case metadata
}

enum StatusReasonID: String, Hashable, Sendable {
    case none
    case text
    case jobLate = "joblate"
    case jobFailed = "jobfailed"
    case configSteps = "configsteps"
    case saveFolder = "savefolder"
    case noTorrentDownloader = "notorrentdownloader"
    case noUsenetDownloader = "nousenetdownloader"
    case sameDownloader = "samedownloader"
    case keyKit = "keykit"
    case keyKitOwner = "keykitowner"
    case metadataDown = "metadatadown"
}

enum StatusFixID: String, Hashable, Sendable {
    case sources
    case downloaders
    case mediaServers = "mediaservers"
    case subtitles
    case libraryFolders = "libraryfolders"
    case backups
    case thisApp = "thisapp"
    case outsideLists = "outsidelists"
    case alerts
    case schedules
    case downloadsWaiting = "downloadswaiting"
    case downloads
}

enum StatusCheckKindID: String, Hashable, Sendable {
    case source
    case downloader
    case job
    case checks
}

enum StatusMarkerID: String, Hashable, Sendable {
    case none
    case idle
    case ok
    case wait
    case fail
}

enum StatusSummaryID: String, Hashable, Sendable {
    case nothing
    case needs
    case word
    case off
    case working
    case waiting
}

enum StatusHeadlineID: String, Hashable, Sendable {
    case fine
    case unknown
    case nothingLinked = "nothinglinked"
    case attention
}

enum StatusReadID: String, Hashable, Sendable {
    case parts
    case checks
    case jobs
    case lists
    case alerts
}

enum StatusDiskKindID: String, Hashable, Sendable {
    case movies
    case shows
    case music
    case other
}

enum StatusDiskLevelID: String, Hashable, Sendable {
    case normal
    case warn
    case full
}

enum StatusDueKindID: String, Hashable, Sendable {
    case none
    case now
    case inTime = "in"
    case overdue
}

enum StatusUnitID: String, Hashable, Sendable {
    case minutes
    case hours
    case days
}

enum StatusStandingID: String, Hashable, Sendable {
    case loading
    case off
    case behind
    case problem
    case none
    case current
    case unchecked
}

enum StatusModeID: String, Hashable, Sendable, CaseIterable {
    case off
    case ask
    case auto
}

enum StatusPhaseID: String, Hashable, Sendable {
    case idle
    case downloading
    case checking
    case ready
    case waiting
    case copying
    case restarting
    case failed
}

enum StatusAboutLoadID: String, Hashable, Sendable {
    case loading
    case ready
    case denied
    case failed
}

enum StatusNoticeKindID: String, Hashable, Sendable {
    case checksRan = "checksran"
    case checksFailed = "checksfailed"
    case rowWorking = "rowworking"
    case rowWord = "rowword"
    case rowConnected = "rowconnected"
    case rowCantConnect = "rowcantconnect"
    case rowCantCheck = "rowcantcheck"
    case jobStarted = "jobstarted"
    case jobAlready = "jobalready"
    case updateCheckFailed = "updatecheckfailed"
    case updateSaved = "updatesaved"
    case updateSaveFailed = "updatesavefailed"
    case updateInstallFailed = "updateinstallfailed"
    case updateGoBackFailed = "updategobackfailed"
    case bundleFailed = "bundlefailed"
}

struct StatusRowInfo: Identifiable, Hashable, Sendable {
    let group: StatusGroupID
    let key: String
    let title: StatusTitleID
    let name: String
    let tone: ToneKind
    let word: StatusWordID
    let reasonKind: StatusReasonID
    let reason: String
    let at: Date?
    let fix: StatusFixID?
    let checkKind: StatusCheckKindID?
    let lastRun: Date?
    let needsAttention: Bool

    var id: String { key }
}

struct StatusKindInfo: Identifiable, Hashable, Sendable {
    let group: StatusGroupID
    let rows: [StatusRowInfo]
    let okCount: Int
    let onCount: Int
    let marker: StatusMarkerID
    let summary: StatusSummaryID
    let summaryCount: Int
    let summaryWord: StatusWordID?
    let fix: StatusFixID

    var id: StatusGroupID { group }
}

struct StatusDueInfo: Hashable, Sendable {
    let kind: StatusDueKindID
    let unit: StatusUnitID
    let amount: Int
}

struct StatusDiskInfo: Identifiable, Hashable, Sendable {
    let path: String
    let label: String
    let kind: StatusDiskKindID
    let freeBytes: Int64
    let totalBytes: Int64
    let usedPercent: Int
    let level: StatusDiskLevelID
    let problem: StatusRowInfo?

    var id: String { path }
}

struct StatusRunningInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let queued: Bool
    let progress: Double
    let percent: Int
    let hasProgress: Bool
    let note: String
}

struct StatusUpcomingInfo: Identifiable, Hashable, Sendable {
    let key: String
    let name: String
    let tone: ToneKind
    let word: StatusWordID
    let due: StatusDueInfo

    var id: String { key }
}

struct StatusFinishedInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let tone: ToneKind
    let word: StatusWordID
    let endedAt: Date?
}

struct StatusJobLineInfo: Identifiable, Hashable, Sendable {
    let row: StatusRowInfo
    let due: StatusDueInfo

    var id: String { row.key }
}

struct StatusInstallInfo: Hashable, Sendable {
    let available: Bool
    let mode: StatusModeID
    let modeLocked: Bool
    let checkOn: Bool
    let fromUpdate: Bool
    let previous: String
    let wentBack: Bool
    let wentBackTo: String
    let wentBackLeft: String
    let rollback: String
    let execError: String
    let phase: StatusPhaseID
    let progressVersion: String
    let progressMessage: String
    let ready: String
    let oneWay: Bool
}

struct StatusAboutInfo: Hashable, Sendable {
    let version: String
    let go: String
    let os: String
    let container: Bool
    let sqlite: String
    let migration: Int
    let configDir: String
    let startDir: String
    let uptimeSeconds: Int64
    let hasDetails: Bool
    let hasServer: Bool
    let writes: Int64
    let writeWaitP95Ms: Double
    let urlBase: String
    let liveClients: Int
    let loadedAt: Int64

    static let empty = StatusAboutInfo(
        version: "",
        go: "",
        os: "",
        container: false,
        sqlite: "",
        migration: 0,
        configDir: "",
        startDir: "",
        uptimeSeconds: 0,
        hasDetails: false,
        hasServer: false,
        writes: 0,
        writeWaitP95Ms: 0,
        urlBase: "",
        liveClients: 0,
        loadedAt: 0
    )
}

struct StatusNoticeInfo: Hashable, Sendable {
    let id: Int
    let kind: StatusNoticeKindID
    let row: StatusRowInfo?
    let detail: String
    let error: ErrorInfo?
}

struct StatusFileInfo: Identifiable, Hashable, Sendable {
    let path: String
    let name: String

    var id: String { path }
}

struct StatusHeadlineInfo: Hashable, Sendable {
    let kind: StatusHeadlineID
    let checks: Int
    let jobs: Int
}

struct HealthInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canAct: Bool
    let canManageUpdates: Bool
    let canBundle: Bool
    let headline: StatusHeadlineInfo
    let failedReads: [StatusReadID]
    let problems: [StatusRowInfo]
    let nothingLinked: Bool
    let linkedUnreadable: Bool
    let kinds: [StatusKindInfo]
    let defaultKind: StatusGroupID
    let disks: [StatusDiskInfo]
    let disksLoaded: Bool
    let backups: StatusRowInfo?
    let keyKit: StatusRowInfo?
    let jobLines: [StatusJobLineInfo]
    let jobsBadge: StatusMarkerID
    let running: [StatusRunningInfo]
    let nextUp: [StatusUpcomingInfo]
    let finished: [StatusFinishedInfo]
    let updateOn: Bool
    let updateVersion: String
    let updateLatest: String
    let updateCheckedAt: Date?
    let updateProblem: String
    let updateFailed: Bool
    let standing: StatusStandingID
    let updateNotes: [String]
    let updateLink: String
    let updateAvailable: Bool
    let checkingUpdate: Bool
    let install: StatusInstallInfo?
    let installReady: Bool
    let installCanGoBack: Bool
    let installMoving: Bool
    let installBusy: Bool
    let about: StatusAboutInfo
    let aboutLoad: StatusAboutLoadID
    let bundleBusy: Bool
    let bundle: StatusFileInfo?
    let busy: [String]
    let notice: StatusNoticeInfo?

    func isBusy(_ key: String) -> Bool { busy.contains(key) }

    static let initial = HealthInfo(
        load: .loading,
        allowed: true,
        canAct: false,
        canManageUpdates: false,
        canBundle: false,
        headline: StatusHeadlineInfo(kind: .unknown, checks: 0, jobs: 0),
        failedReads: [],
        problems: [],
        nothingLinked: false,
        linkedUnreadable: false,
        kinds: [],
        defaultKind: .sources,
        disks: [],
        disksLoaded: false,
        backups: nil,
        keyKit: nil,
        jobLines: [],
        jobsBadge: .none,
        running: [],
        nextUp: [],
        finished: [],
        updateOn: false,
        updateVersion: "",
        updateLatest: "",
        updateCheckedAt: nil,
        updateProblem: "",
        updateFailed: false,
        standing: .loading,
        updateNotes: [],
        updateLink: "",
        updateAvailable: false,
        checkingUpdate: false,
        install: nil,
        installReady: false,
        installCanGoBack: false,
        installMoving: false,
        installBusy: false,
        about: .empty,
        aboutLoad: .loading,
        bundleBusy: false,
        bundle: nil,
        busy: [],
        notice: nil
    )
}

enum HealthChecksKindID: String, Hashable, Sendable {
    case libraryFolder = "libraryfolder"
    case diskSpace = "diskspace"
    case source
    case downloader
    case backups
    case masterKey = "masterkey"
    case update
    case configDisk = "configdisk"
    case configFolder = "configfolder"
    case waitingDownloads = "waitingdownloads"
    case clearWaiting = "clearwaiting"
    case saveFolder = "savefolder"
    case sameDownloader = "samedownloader"
    case torrentDownloader = "torrentdownloader"
    case usenetDownloader = "usenetdownloader"
    case trailers
    case peopleCheck = "peoplecheck"
    case other
}

enum HealthChecksFieldID: Hashable, Sendable {
    case everyMinutes
    case flapGuard
    case lowSpace
}

struct HealthChecksRowInfo: Identifiable, Hashable, Sendable {
    let key: String
    let kind: HealthChecksKindID
    let label: String
    let subject: String
    let message: String
    let tone: ToneKind
    let since: Date?

    var id: String { key }
}

struct HealthChecksSyncInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let degraded: Bool
    let seenAt: Date?
}

struct HealthChecksInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canSeeChecks: Bool
    let canEdit: Bool
    let worst: ToneKind
    let checks: [HealthChecksRowInfo]
    let sync: [HealthChecksSyncInfo]
    let hasSettings: Bool
    let days: [Int]
    let updateDays: Int
    let feed: String
    let defaultFeed: String
    let local: Bool
    let prerelease: Bool
    let everyText: String
    let guardText: String
    let lowText: String
    let everyValid: Bool
    let guardValid: Bool
    let lowValid: Bool
    let valid: Bool
    let version: String
    let running: Bool
    let saving: Bool
    let saveError: ErrorInfo?
    let dirty: Bool
    let canSave: Bool
    let noticeId: Int
    let noticeSaved: Bool
    let noticeRunFailed: Bool
    let noticeError: ErrorInfo?

    static let initial = HealthChecksInfo(
        load: .loading,
        allowed: true,
        canSeeChecks: false,
        canEdit: false,
        worst: .idle,
        checks: [],
        sync: [],
        hasSettings: false,
        days: [1, 3, 7, 14, 30],
        updateDays: 1,
        feed: "",
        defaultFeed: "",
        local: false,
        prerelease: false,
        everyText: "",
        guardText: "",
        lowText: "",
        everyValid: true,
        guardValid: true,
        lowValid: true,
        valid: true,
        version: "",
        running: false,
        saving: false,
        saveError: nil,
        dirty: false,
        canSave: false,
        noticeId: 0,
        noticeSaved: false,
        noticeRunFailed: false,
        noticeError: nil
    )
}
