import Foundation

struct DlTitleLink: Hashable, Sendable {
    let kind: String
    let tmdb: String
}

struct DlDay: Hashable, Comparable, Sendable {
    let year: Int
    let month: Int
    let day: Int

    init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    init(date: Date) {
        let parts = Calendar.current.dateComponents([.year, .month, .day], from: date)
        year = parts.year ?? 1970
        month = parts.month ?? 1
        day = parts.day ?? 1
    }

    static func < (a: DlDay, b: DlDay) -> Bool {
        if a.year != b.year { return a.year < b.year }
        if a.month != b.month { return a.month < b.month }
        return a.day < b.day
    }

    static var today: DlDay { DlDay(date: Date()) }

    var key: String { String(format: "%04d-%02d-%02d", year, month, day) }

    var date: Date {
        Calendar.current.date(from: DateComponents(year: year, month: month, day: day)) ?? Date()
    }

    func adding(days: Int) -> DlDay {
        DlDay(date: Calendar.current.date(byAdding: .day, value: days, to: date) ?? date)
    }
}

struct DlMonth: Hashable, Sendable {
    let year: Int
    let month: Int

    var date: Date {
        Calendar.current.date(from: DateComponents(year: year, month: month, day: 1)) ?? Date()
    }
}

enum DownloadsTabKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case all
    case waiting
    case progress
    case problems
    case done

    var id: String { rawValue }
}

enum DownloadsSubKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case queued
    case paused
    case downloading
    case checking
    case importing
    case failed
    case decide

    var id: String { rawValue }
}

enum StatusWordKind: String, Hashable, Sendable {
    case failed
    case packWaiting = "packwaiting"
    case moving
    case checking
    case queued
    case paused
    case downloading
    case stalled
    case importing
    case seeding
    case review
    case doneSeeding = "doneseeding"
    case done
    case imported
    case taken
}

enum ProblemKindInfo: String, Hashable, Sendable {
    case missingArticles = "missingarticles"
    case repairNeeded = "repairneeded"
    case repairing
    case importBlocked = "importblocked"
    case trackerError = "trackererror"
    case noTrackerReply = "notrackerreply"
    case downloaderStopped = "downloaderstopped"
    case noFullCopy = "nofullcopy"
    case noPeers = "nopeers"
    case trackerWarning = "trackerwarning"
}

enum DetailKindInfo: String, Hashable, Sendable {
    case filesGone = "filesgone"
    case unpacking
    case upNext = "upnext"
    case placing
    case importFailed = "importfailed"
    case noFile = "nofile"
    case removed
    case refused
    case noMatch = "nomatch"
    case cleanupHold = "cleanuphold"
    case ratio
}

enum NoteKindInfo: String, Hashable, Sendable {
    case nextInLine = "nextinline"
    case inLine = "inline"
    case verifying
    case repairing
    case unpacking
    case finishing
    case script
    case filesMissing = "filesmissing"
    case downloaderError = "downloadererror"
    case importFailed = "importfailed"
    case removedOutside = "removedoutside"
    case noMatch = "nomatch"
    case filesGone = "filesgone"
    case stillIn = "stillin"
    case clientText = "clienttext"
    case detail
}

enum BarToneKind: String, Hashable, Sendable {
    case go
    case queued
    case held
    case bad
    case done
}

enum SubLineKindInfo: String, Hashable, Sendable {
    case text
    case season
    case seasons
}

enum DlKindFilter: String, CaseIterable, Hashable, Sendable, Identifiable {
    case all
    case movie
    case show
    case music

    var id: String { rawValue }
}

enum DlNetworkFilter: String, CaseIterable, Hashable, Sendable, Identifiable {
    case any
    case usenet
    case torrent

    var id: String { rawValue }
}

enum DownloadActionKind: String, Hashable, Sendable, Identifiable {
    case pause
    case resume
    case retry
    case moveUp = "moveup"
    case moveDown = "movedown"
    case moveTop = "movetop"
    case moveBottom = "movebottom"
    case priority
    case importAgain = "importagain"
    case openTitle = "opentitle"
    case remove
    case recheck
    case reannounce
    case forceStart = "forcestart"
    case seedingRules = "seedingrules"
    case setLocation = "setlocation"
    case speedLimits = "speedlimits"
    case markFailed = "markfailed"

    var id: String { rawValue }
}

enum UsenetPriorityKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case low
    case normal
    case high
    case force

    var id: String { rawValue }
}

enum SortKeyKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case title
    case progress
    case status
    case size
    case quality
    case added
    case seeds
    case peers
    case ratio
    case tracker
    case requested
    case downloader

    var id: String { rawValue }
}

struct DlSortInfo: Hashable, Sendable {
    let key: SortKeyKind
    let descending: Bool
}

enum TrackerFilterKindInfo: String, Hashable, Sendable {
    case host
    case error
    case warning
    case announce
}

struct TrackerOptionInfo: Identifiable, Hashable, Sendable {
    let key: String
    let kind: TrackerFilterKindInfo
    let label: String
    let count: Int

    var id: String { key }
}

struct TabCountInfo: Identifiable, Hashable, Sendable {
    let tab: DownloadsTabKind
    let count: Int

    var id: DownloadsTabKind { tab }
}

struct SubCountInfo: Identifiable, Hashable, Sendable {
    let sub: DownloadsSubKind
    let count: Int

    var id: DownloadsSubKind { sub }
}

struct SubLineInfo: Hashable, Sendable {
    let kind: SubLineKindInfo
    let text: String
    let from: Int
    let to: Int
}

struct RowDetailInfo: Hashable, Sendable {
    let kind: DetailKindInfo
    let text: String
}

struct RowNoteInfo: Hashable, Sendable {
    let kind: NoteKindInfo
    let number: Int
    let text: String
    let detail: RowDetailInfo?
}

struct RowProblemInfo: Hashable, Sendable {
    let kind: ProblemKindInfo
    let tone: ToneKind
}

enum DecisionStepKind: String, Hashable, Sendable {
    case search
    case importAgain = "importagain"
    case importFile = "importfile"
    case setEdition = "setedition"
    case openVersions = "openversions"
    case openReleases = "openreleases"
}

enum DecisionHintKind: String, Hashable, Sendable {
    case none
    case pathPair = "pathpair"
    case matchByHand = "matchbyhand"
}

enum ReasonLineKindInfo: String, Hashable, Sendable {
    case reading
    case editionUnknown = "editionunknown"
    case copyInPlace = "copyinplace"
    case noMatchMovie = "nomatchmovie"
    case noMatchEpisode = "nomatchepisode"
    case noMatchTrack = "nomatchtrack"
    case server
    case why
}

enum WhyKindInfo: String, Hashable, Sendable {
    case ranOut = "ranout"
    case conflict
    case notReplaced = "notreplaced"
    case notSent = "notsent"
    case linkRefused = "linkrefused"
    case taken
}

enum DecideKind: Hashable, Sendable {
    case search
    case turnDown
    case stop
    case importFile
}

struct ReasonLineInfo: Hashable, Sendable {
    let kind: ReasonLineKindInfo
    let text: String
    let why: WhyKindInfo
}

struct DecisionPlanInfo: Hashable, Sendable {
    let lead: DecisionStepKind
    let beside: DecisionStepKind?
    let more: [DecisionStepKind]
    let turnDown: Bool
    let hint: DecisionHintKind
}

struct DecisionInfo: Identifiable, Hashable, Sendable {
    let slotId: Int64
    let titleId: Int64
    let downloadId: Int64
    let name: String
    let releaseName: String
    let version: String
    let since: String
    let retryAt: String
    let reading: Bool
    let hasCopy: Bool
    let kind: String
    let importFile: String
    let packTotal: Int
    let episodeLabel: String
    let episodeName: String
    let reason: String
    let reasonLine: ReasonLineInfo
    let why: WhyKindInfo
    let plan: DecisionPlanInfo

    var id: Int64 { slotId }
}

struct DecisionGroupInfo: Identifiable, Hashable, Sendable {
    let key: String
    let lead: DecisionInfo
    let items: [DecisionInfo]
    let pack: Bool
    let packImportAgain: Bool

    var id: String { key }
}

struct DownloadItemInfo: Identifiable, Hashable, Sendable {
    let key: String
    let name: String
    let title: String
    let subLine: SubLineInfo
    let word: StatusWordKind
    let note: RowNoteInfo?
    let problem: RowProblemInfo?
    let detail: RowDetailInfo?
    let bar: BarToneKind
    let tone: ToneKind
    let progress: Double
    let moving: Bool
    let history: Bool
    let poster: String?
    let square: Bool
    let actions: [DownloadActionKind]
    let selectable: Bool
    let titleId: Int64
    let size: Int64
    let speed: Int64
    let upSpeed: Int64
    let eta: Int64
    let decisions: [DecisionGroupInfo]

    var id: String { key }
}

struct DownloadsFilterInfo: Hashable, Sendable {
    let tab: DownloadsTabKind
    let sub: DownloadsSubKind?
    let query: String
    let kind: DlKindFilter
    let network: DlNetworkFilter
    let tracker: String
    let sort: DlSortInfo?
    let filtersOn: Int

    static let initial = DownloadsFilterInfo(
        tab: .all,
        sub: nil,
        query: "",
        kind: .all,
        network: .any,
        tracker: "",
        sort: nil,
        filtersOn: 0
    )
}

enum DownloadsEmptyKindInfo: String, Hashable, Sendable {
    case noMatches = "nomatches"
    case noKind = "nokind"
    case noNetwork = "nonetwork"
    case loadingMore = "loadingmore"
    case tab
    case sub
    case nothing
}

struct DownloadsEmptyInfo: Hashable, Sendable {
    let kind: DownloadsEmptyKindInfo
    let count: Int
}

enum ClientNoteKindInfo: String, Hashable, Sendable {
    case waiting
    case stale
    case offline
}

struct ClientNoteInfo: Identifiable, Hashable, Sendable {
    let name: String
    let kind: ClientNoteKindInfo

    var id: String { name }
}

struct StatusLineInfo: Hashable, Sendable {
    let speed: Int64
    let upSpeed: Int64
    let free: Int64
    let left: Int64
    let review: Int
    let limited: Bool

    static let initial = StatusLineInfo(speed: 0, upSpeed: 0, free: 0, left: 0, review: 0, limited: false)
}

enum NoticeKindInfo: String, Hashable, Sendable {
    case paused
    case started
    case retried
    case movedTop = "movedtop"
    case movedBottom = "movedbottom"
    case movedUp = "movedup"
    case movedDown = "moveddown"
    case prioritySet = "priorityset"
    case checking
    case announced
    case forceStarted = "forcestarted"
    case importQueued = "importqueued"
    case removed
    case removedKept = "removedkept"
    case markedFailed = "markedfailed"
    case allStarted = "allstarted"
    case allPaused = "allpaused"
    case allFailed = "allfailed"
    case speedSaved = "speedsaved"
    case seedingSet = "seedingset"
    case moving
    case speedsSet = "speedsset"
    case added
    case searching
    case turnedDown = "turneddown"
    case stopped
    case imported
    case editionSet = "editionset"
    case matching
    case matched
    case downloadFailed = "downloadfailed"
    case bulkDone = "bulkdone"
    case notHere = "nothere"
}

struct AddOutcomeInfo: Hashable, Sendable {
    let added: Int
    let failed: Int
    let pending: Int
    let duplicates: Int
    let bad: [String]
    let dropped: Int
}

struct DownloadsNoticeInfo: Hashable, Sendable {
    let id: Int64
    let kind: NoticeKindInfo
    let count: Int
    let skipped: Int
    let text: String
    let tone: ToneKind
    let outcome: AddOutcomeInfo?
}

struct MatchCandidateInfo: Identifiable, Hashable, Sendable {
    let id: String
    let kind: String
    let name: String
    let year: Int
    let poster: String
}

struct DlMatchInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let candidates: [MatchCandidateInfo]
}

struct DownloaderTargetInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let category: String
}

struct SpeedTargetInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let torrent: Bool
    let downMb: Double
    let upMb: Double
    let slowMode: Bool?
}

struct ShareLimitsInfo: Hashable, Sendable {
    let ratio: Double
    let seedingMinutes: Int64
    let inactiveMinutes: Int64
    let action: String

    static let unset = ShareLimitsInfo(ratio: -2, seedingMinutes: -2, inactiveMinutes: -2, action: "")
}

enum RemoveDoneLineKind: String, Hashable, Sendable {
    case doneSeeding = "doneseeding"
    case doneIdle = "doneidle"
    case partSeeding = "partseeding"
    case partIdle = "partidle"
}

enum RemoveFilesLineKind: String, Hashable, Sendable {
    case path
    case one
    case many
}

struct RemovePlanInfo: Identifiable, Hashable, Sendable {
    let keys: [String]
    let names: [String]
    let more: Int
    let single: String
    let release: String
    let doneLine: RemoveDoneLineKind?
    let downloader: String
    let showDeleteFiles: Bool
    let showTurnDown: Bool
    let deleteFiles: Bool
    let turnDown: Bool
    let searchAgain: Bool
    let blocked: Int
    let filesLine: RemoveFilesLineKind
    let filesPath: String
    let filesClients: String
    let keepsSome: Bool
    let keepsPath: String

    var id: String { keys.joined(separator: ",") }

    func remaining(_ left: [String]) -> RemovePlanInfo {
        RemovePlanInfo(
            keys: left,
            names: [],
            more: 0,
            single: "",
            release: release,
            doneLine: doneLine,
            downloader: downloader,
            showDeleteFiles: showDeleteFiles,
            showTurnDown: showTurnDown,
            deleteFiles: deleteFiles,
            turnDown: turnDown,
            searchAgain: searchAgain,
            blocked: blocked,
            filesLine: filesLine,
            filesPath: filesPath,
            filesClients: filesClients,
            keepsSome: keepsSome,
            keepsPath: keepsPath
        )
    }
}

struct DownloadsInfo: Hashable, Sendable {
    let load: LoadInfo
    let manager: Bool
    let canSettings: Bool
    let filter: DownloadsFilterInfo
    let items: [DownloadItemInfo]
    let total: Int
    let tabs: [TabCountInfo]
    let subs: [SubCountInfo]
    let trackers: [TrackerOptionInfo]
    let trackerIssues: Int
    let empty: DownloadsEmptyInfo?
    let notes: [ClientNoteInfo]
    let statusLine: StatusLineInfo
    let matches: [DlMatchInfo]
    let matchesPending: Int
    let addTargets: [DownloaderTargetInfo]
    let speedTargets: [SpeedTargetInfo]
    let moreOutside: Int
    let loadingMore: Bool
    let selection: Set<String>
    let busy: Set<String>
    let batchBusy: Bool
    let selectionActions: [DownloadActionKind]
    let notice: DownloadsNoticeInfo?
    let actionError: ErrorInfo?
    let isEmpty: Bool

    var selecting: Bool { !selection.isEmpty }

    func item(_ key: String) -> DownloadItemInfo? {
        items.first { $0.key == key }
    }

    static let initial = DownloadsInfo(
        load: .loading,
        manager: false,
        canSettings: false,
        filter: .initial,
        items: [],
        total: 0,
        tabs: [],
        subs: [],
        trackers: [],
        trackerIssues: 0,
        empty: nil,
        notes: [],
        statusLine: .initial,
        matches: [],
        matchesPending: 0,
        addTargets: [],
        speedTargets: [],
        moreOutside: 0,
        loadingMore: false,
        selection: [],
        busy: [],
        batchBusy: false,
        selectionActions: [],
        notice: nil,
        actionError: nil,
        isEmpty: false
    )
}

enum DetailTabKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case info
    case files
    case peers
    case trackers
    case servers
    case log

    var id: String { rawValue }
}

enum InfoGroupKindInfo: String, Hashable, Sendable {
    case transfer
    case swarm
    case dates
    case rules
    case place = "where"
}

enum FactKindInfo: String, Hashable, Sendable {
    case status
    case downloaded
    case size
    case down
    case up
    case timeLeft = "timeleft"
    case uploaded
    case ratio
    case lastActivity = "lastactivity"
    case wasted
    case seedingTime = "seedingtime"
    case seeds
    case peers
    case isPrivate = "private"
    case added
    case posted
    case doneAt = "doneat"
    case timeActive = "timeactive"
    case release
    case savePath = "savepath"
    case downloader
    case category
    case folder
    case requestedBy = "requestedby"
    case speedLimits = "speedlimits"
    case seedingRules = "seedingrules"
}

struct InfoFactInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let kind: FactKindInfo
    let text: String
    let at: Date?
    let seconds: Int64
    let a: Int64
    let b: Int64
    let limits: ShareLimitsInfo?
}

struct InfoGroupInfo: Identifiable, Hashable, Sendable {
    let kind: InfoGroupKindInfo
    let facts: [InfoFactInfo]

    var id: InfoGroupKindInfo { kind }
}

struct FileNodeInfo: Identifiable, Hashable, Sendable {
    let path: String
    let name: String
    let depth: Int
    let folder: Bool
    let size: Int64
    let progress: Double
    let priority: Int
    let indexes: [Int]

    var id: String { path }
}

struct PeerRowInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let address: String
    let client: String
    let progress: Double
    let speed: Int64
    let upSpeed: Int64
}

struct TrackerRowInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let url: String
    let tier: Int
    let status: String
    let message: String
    let seeds: Int
    let peers: Int
    let fixed: Bool
    let ref: String
}

struct ServerRowInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let name: String
    let job: String
    let found: String
}

struct RepairStepInfo: Identifiable, Hashable, Sendable {
    let stage: String
    let lines: [String]

    var id: String { stage }
}

struct LogEntryInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let at: Date?
    let day: DlDay?
    let summary: String
    let warn: Bool
}

enum DetailNoticeKind: String, Hashable, Sendable {
    case filesSet = "filesset"
    case trackerSaved = "trackersaved"
    case trackerRemoved = "trackerremoved"
    case trackersAdded = "trackersadded"
}

struct DownloadDetailInfo: Hashable, Sendable {
    let tabs: [DetailTabKind]
    let tab: DetailTabKind
    let live: Bool
    let reading: Bool
    let readFailed: Bool
    let canEdit: Bool
    let busy: Bool
    let actionError: ErrorInfo?
    let needsReauth: Bool
    let notice: DetailNoticeKind?
    let info: [InfoGroupInfo]
    let torrentError: Bool
    let fileCount: Int
    let singleFile: Bool
    let fileFilter: String
    let fileTree: [FileNodeInfo]
    let peers: [PeerRowInfo]
    let trackers: [TrackerRowInfo]
    let servers: [ServerRowInfo]
    let repair: [RepairStepInfo]
    let log: [LogEntryInfo]

    static let initial = DownloadDetailInfo(
        tabs: [],
        tab: .info,
        live: false,
        reading: true,
        readFailed: false,
        canEdit: false,
        busy: false,
        actionError: nil,
        needsReauth: false,
        notice: nil,
        info: [],
        torrentError: false,
        fileCount: 0,
        singleFile: true,
        fileFilter: "",
        fileTree: [],
        peers: [],
        trackers: [],
        servers: [],
        repair: [],
        log: []
    )
}

enum HistoryStepKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case sent
    case downloaded
    case imported
    case upgraded
    case renamed
    case deleted
    case declined
    case failed
    case asked
    case approved
    case added
    case subtitles
    case reported
    case decide
    case system

    var id: String { rawValue }
}

enum HistoryTypeKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case all
    case movies
    case shows
    case music

    var id: String { rawValue }
}

enum HistoryWhoKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case all
    case me
    case people
    case ai
    case holyArrs = "holyarrs"

    var id: String { rawValue }
}

enum HistoryWhenKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case all
    case day
    case week
    case month
    case custom

    var id: String { rawValue }
}

enum HistoryVerbKind: String, Hashable, Sendable {
    case imported
    case partlyImported = "partlyimported"
    case upgraded
    case lowOnSpace = "lowonspace"
    case needsDecision = "needsdecision"
    case needsYou = "needsyou"
    case stoppedLooking = "stoppedlooking"
    case fixed
    case asked
    case approved
    case turnedDown = "turneddown"
    case failed
    case reported
    case reopened
    case downloaded
    case stalled
    case paused
    case queued
    case fileMissing = "filemissing"
    case fileBack = "fileback"
    case refreshed
    case albumsChanged = "albumschanged"
    case outNow = "outnow"
    case changed
    case settingsImported = "settingsimported"
    case adopted
    case added
    case searched
    case searchFailed = "searchfailed"
    case held
    case chosen
    case sent
    case downloading
    case renamed
    case deleted
    case subtitles
    case subtitlesWait = "subtitleswait"
    case scanned
    case health
    case requests
    case byAi = "byai"
    case notDone = "notdone"
    case alertFailed = "alertfailed"
    case mediaServer = "mediaserver"
    case mediaServerFailed = "mediaserverfailed"
    case system
    case other
}

enum StepStateKind: String, Hashable, Sendable {
    case done
    case failed
    case waiting
    case needs
}

enum DownloadStepKeyKind: String, Hashable, Sendable {
    case sent
    case downloaded
    case imported
}

struct DownloadStepInfo: Identifiable, Hashable, Sendable {
    let key: DownloadStepKeyKind
    let state: StepStateKind

    var id: DownloadStepKeyKind { key }
}

enum DlLineKind: String, Hashable, Sendable {
    case summary
    case reason
    case addedList = "addedlist"
    case addedImport = "addedimport"
    case addedBy = "addedby"
    case addedAsk = "addedask"
    case removedKept = "removedkept"
    case removedDeleted = "removeddeleted"
    case renamed
    case renameReady = "renameready"
    case feeds
    case looked
    case searchFailed = "searchfailed"
    case turnedDown = "turneddown"
    case unblocked
    case heldBack = "heldback"
    case heldGone = "heldgone"
    case waiting
    case downloading
    case downloadingPct = "downloadingpct"
    case stalled
    case paused
    case pausedAt = "pausedat"
    case sharing
    case movedOut = "movedout"
    case removedOutside = "removedoutside"
    case removedDl = "removeddl"
    case removedDlFiles = "removeddlfiles"
    case dlFailed = "dlfailed"
    case notRemoved = "notremoved"
    case notMoved = "notmoved"
    case partly
    case nothingPlaced = "nothingplaced"
    case better
    case fileBack = "fileback"
    case fileHeld = "fileheld"
    case holdFailed = "holdfailed"
    case holdingCleaned = "holdingcleaned"
    case lowSpace = "lowspace"
    case stoppedLooking = "stoppedlooking"
    case workingAgain = "workingagain"
    case mediaTold = "mediatold"
    case mediaFailed = "mediafailed"
    case alertFailed = "alertfailed"
    case outNow = "outnow"
    case report
    case checkedDetails = "checkeddetails"
}

struct HistoryLineInfo: Hashable, Sendable {
    let kind: DlLineKind
    let text: String
    let number: Int
}

enum WhoKindInfo: String, Hashable, Sendable {
    case person
    case aPerson = "aperson"
    case ai
    case job
    case holyArrs = "holyarrs"
}

struct HistoryWhoInfo: Hashable, Sendable {
    let kind: WhoKindInfo
    let name: String
    let forName: String
}

enum HistoryFactKindInfo: String, Hashable, Sendable {
    case quality
    case size
    case source
    case downloader
    case release
    case file
    case from
    case folder
    case why
    case album
}

struct HistoryFactInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let kind: HistoryFactKindInfo
    let text: String
}

enum HistoryRedoKind: String, Hashable, Sendable {
    case findAnother = "findanother"
    case replace
}

struct HistoryItemInfo: Identifiable, Hashable, Sendable {
    let key: String
    let verb: HistoryVerbKind
    let tone: ToneKind
    let title: String
    let episode: String
    let version: String
    let line: HistoryLineInfo
    let extra: String
    let who: HistoryWhoInfo
    let poster: String?
    let square: Bool
    let facts: [HistoryFactInfo]
    let steps: [DownloadStepInfo]
    let redo: HistoryRedoKind?
    let releaseId: Int64
    let downloadId: Int64
    let titleId: Int64
    let at: Date?
    let failed: Bool
    let needsYou: Bool

    var id: String { key }
}

struct HistoryDayInfo: Identifiable, Hashable, Sendable {
    let day: DlDay
    let items: [HistoryItemInfo]
    let failed: Int
    let needsYou: Int

    var id: String { day.key }
}

struct HistoryFilterInfo: Hashable, Sendable {
    var steps: Set<HistoryStepKind>
    var type: HistoryTypeKind
    var who: HistoryWhoKind
    var period: HistoryWhenKind
    var from: String
    var to: String
    var source: Int64

    static let initial = HistoryFilterInfo(steps: [], type: .all, who: .all, period: .all, from: "", to: "", source: 0)

    var count: Int {
        var total = steps.count
        if type != .all { total += 1 }
        if who != .all { total += 1 }
        if period != .all { total += 1 }
        if source > 0 { total += 1 }
        return total
    }

    var badRange: Bool {
        period == .custom && !from.isEmpty && !to.isEmpty && from > to
    }
}

struct DlSourceInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
}

enum HistoryNoticeKind: String, Hashable, Sendable {
    case replacing
    case noRelease = "norelease"
}

struct HistoryInfo: Hashable, Sendable {
    let load: LoadInfo
    let titleId: Int64
    let episodeId: Int64
    let scopeName: String
    let filter: HistoryFilterInfo
    let query: String
    let days: [HistoryDayInfo]
    let total: Int
    let loaded: Int
    let more: Bool
    let loadingMore: Bool
    let sources: [DlSourceInfo]
    let canSources: Bool
    let busy: Set<String>
    let notice: HistoryNoticeKind?
    let noticeId: Int64
    let actionError: ErrorInfo?
    let isEmpty: Bool
    let filtered: Bool

    var scoped: Bool { titleId > 0 || episodeId > 0 }

    func item(_ key: String) -> HistoryItemInfo? {
        for day in days {
            if let found = day.items.first(where: { $0.key == key }) { return found }
        }
        return nil
    }

    static let initial = HistoryInfo(
        load: .loading,
        titleId: 0,
        episodeId: 0,
        scopeName: "",
        filter: .initial,
        query: "",
        days: [],
        total: 0,
        loaded: 0,
        more: false,
        loadingMore: false,
        sources: [],
        canSources: false,
        busy: [],
        notice: nil,
        noticeId: 0,
        actionError: nil,
        isEmpty: false,
        filtered: false
    )
}

enum CalendarTypeKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case all
    case movies
    case shows
    case music

    var id: String { rawValue }
}

enum CalendarFollowsKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case mine
    case everyone
    case library

    var id: String { rawValue }
}

enum CalendarViewKind: String, Hashable, Sendable {
    case agenda
    case month
}

enum EntryDotKind: String, Hashable, Sendable {
    case have
    case getting
    case problem
    case missing
    case upcoming

    var tone: ToneKind {
        switch self {
        case .have: .ok
        case .getting: .info
        case .problem: .fail
        case .missing: .wait
        case .upcoming: .idle
        }
    }
}

enum EntryTagKind: String, Hashable, Sendable {
    case showPremiere = "showpremiere"
    case seasonPremiere = "seasonpremiere"
    case newEpisode = "newepisode"
    case newAlbum = "newalbum"
    case newSingle = "newsingle"
    case newEp = "newep"
    case inCinemas = "incinemas"
    case newMovie = "newmovie"
    case episodes
    case albums
    case movieDates = "moviedates"
}

enum EntryWhenKind: String, Hashable, Sendable {
    case time
    case allDay = "allday"
    case album
    case single
    case ep
    case inTheaters = "intheaters"
    case streaming
}

enum EpisodeSpanKind: String, Hashable, Sendable {
    case one
    case sameSeason = "sameseason"
    case across
}

enum MovieDateKindInfo: String, Hashable, Sendable {
    case cinema
    case digital
    case disc
}

struct CalendarEntryInfo: Identifiable, Hashable, Sendable {
    let key: String
    let season: Int
    let episode: Int
    let episodeName: String
    let dot: EntryDotKind

    var id: String { key }
}

struct CalendarGroupInfo: Identifiable, Hashable, Sendable {
    let key: String
    let day: DlDay
    let kind: String
    let title: String
    let tag: EntryTagKind
    let count: Int
    let movieKinds: [MovieDateKindInfo]
    let whenKind: EntryWhenKind
    let minutes: Int
    let span: EpisodeSpanKind
    let season: Int
    let episode: Int
    let season2: Int
    let episode2: Int
    let episodeName: String
    let network: String
    let caption: String
    let album: String
    let dot: EntryDotKind
    let art: String?
    let following: Bool
    let followers: [String]
    let canSearch: Bool
    let leadTitleId: Int64
    let leadName: String
    let leadKey: String
    let entries: [CalendarEntryInfo]

    var id: String { key }
}

struct CalendarDayInfo: Identifiable, Hashable, Sendable {
    let day: DlDay
    let groups: [CalendarGroupInfo]

    var id: String { day.key }
}

struct MonthCellInfo: Identifiable, Hashable, Sendable {
    let day: DlDay
    let inMonth: Bool
    let today: Bool
    let count: Int
    let tone: ToneKind?

    var id: String { day.key }
}

enum CalendarNoticeKindInfo: String, Hashable, Sendable {
    case followed
    case unfollowed
    case searching
    case nothingToSearch = "nothingtosearch"
    case searchingMissing = "searchingmissing"
}

struct CalendarNoticeInfo: Hashable, Sendable {
    let id: Int64
    let kind: CalendarNoticeKindInfo
    let name: String
    let count: Int
}

struct CalendarInfo: Hashable, Sendable {
    let load: LoadInfo
    let view: CalendarViewKind
    let month: DlMonth
    let today: DlDay
    let selected: DlDay?
    let type: CalendarTypeKind
    let follows: CalendarFollowsKind
    let people: Int
    let days: [CalendarDayInfo]
    let cells: [MonthCellInfo]
    let hidden: Int
    let missing: Int
    let canSearch: Bool
    let musicOn: Bool
    let canPrevious: Bool
    let canNext: Bool
    let busy: Set<String>
    let notice: CalendarNoticeInfo?
    let actionError: ErrorInfo?

    var selectedGroups: [CalendarGroupInfo] {
        guard let selected else { return [] }
        return days.first { $0.day == selected }?.groups ?? []
    }

    func group(_ key: String) -> CalendarGroupInfo? {
        for day in days {
            if let found = day.groups.first(where: { $0.key == key }) { return found }
        }
        return nil
    }

    static let initial = CalendarInfo(
        load: .loading,
        view: .agenda,
        month: DlMonth(year: 1970, month: 1),
        today: DlDay(year: 1970, month: 1, day: 1),
        selected: nil,
        type: .all,
        follows: .library,
        people: 0,
        days: [],
        cells: [],
        hidden: 0,
        missing: 0,
        canSearch: false,
        musicOn: true,
        canPrevious: true,
        canNext: true,
        busy: [],
        notice: nil,
        actionError: nil
    )
}

enum DownloadsBadgeKindInfo: String, Hashable, Sendable {
    case none
    case active
    case decide
}

struct DownloadsBadgeInfo: Hashable, Sendable {
    let active: Int
    let percent: Int
    let decide: Int

    var kind: DownloadsBadgeKindInfo {
        if active > 0 { return .active }
        if decide > 0 { return .decide }
        return .none
    }

    static let initial = DownloadsBadgeInfo(active: 0, percent: 0, decide: 0)
}

struct DlTorrentFile: Hashable, Sendable {
    let name: String
    let data: Data
}
