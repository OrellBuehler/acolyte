import Foundation

enum MoveStepKind: String, Hashable, Sendable {
    case read
    case choose
    case run
    case done
}

enum MoveWordKind: String, Hashable, Sendable {
    case willBeAdded = "willbeadded"
    case added
    case willUseTheOne = "willusetheone"
    case usingTheOne = "usingtheone"
    case willBeUpdated = "willbeupdated"
    case updated
    case matchesOneHere = "matchesonehere"
    case matched
    case alreadyHere = "alreadyhere"
    case leftOut = "leftout"
    case noFolderHere = "nofolderhere"
    case wontWork = "wontwork"
    case failed
    case removed
    case kept
    case notRemoved = "notremoved"
    case other
}

enum MoveToneKind: String, Hashable, Sendable {
    case ok
    case warn
    case bad
    case quiet
}

enum MoveStateKind: String, Hashable, Sendable {
    case waiting
    case importing
    case imported
    case stopped
    case undone
    case undoing
    case undoFailed = "undofailed"
}

enum MoveCountWord: String, Hashable, Sendable {
    case movie
    case show
    case artist
    case title
    case folder
    case pickRules = "pickrules"
    case trait
    case waitRule = "waitrule"
    case source
    case downloader
    case outsideList = "outsidelist"
    case alert
    case linkedApp = "linkedapp"
    case person
    case request
    case tag
    case subtitleLanguage = "subtitlelanguage"
    case subtitleSet = "subtitleset"
    case subtitleSite = "subtitlesite"
    case keyToRetype = "keytoretype"
}

enum MoveGroupKind: String, Hashable, Sendable {
    case folders
    case pickRules = "pickrules"
    case sources
    case titles
    case neverAdd = "neveradd"
    case outsideLists = "outsidelists"
    case downloaders
    case turnedDown = "turneddown"
    case naming
    case subtitles
    case people
    case wishlist
    case alerts
    case other
    case unknown
}

enum MovePathProblemKind: String, Hashable, Sendable {
    case none
    case fillBoth = "fillboth"
    case fullPath = "fullpath"
    case dots
    case typePath = "typepath"
}

enum MoveGateKind: String, Hashable, Sendable {
    case none
    case turnOnRead = "turnonread"
    case updating
    case progress
    case fixFolder = "fixfolder"
    case fixRule = "fixrule"
    case nothingNew = "nothingnew"
    case turnOnGroup = "turnongroup"
    case safeMode = "safemode"
    case testFailed = "testfailed"
    case changedSinceTest = "changedsincetest"
    case trying
    case oneNeedsChoice = "oneneedschoice"
    case manyNeedChoice = "manyneedchoice"
}

enum MoveCheckStepKind: String, Hashable, Sendable {
    case reading
    case folders
    case titles
}

enum MovePersonChoiceKind: String, Hashable, Sendable {
    case owner
    case admin
    case merge
    case skip
}

enum MoveNamesKind: String, Hashable, Sendable {
    case keep
    case rename
}

enum MoveWhyKind: String, Hashable, Sendable {
    case sameName = "samename"
    case numericId = "numericid"
    case mergeGone = "mergegone"
}

enum MoveReachKind: String, Hashable, Sendable {
    case other
    case tryHttp = "tryhttp"
    case cantReach = "cantreach"
    case timeout
    case tooBig = "toobig"
    case refusedKey = "refusedkey"
    case wrongApp = "wrongapp"
    case unsupported
    case safeMode = "safemode"
    case notOwner = "notowner"
}

enum MovePickChoiceKind: String, Hashable, Sendable {
    case none
    case leave
    case other
    case path
}

enum MoveDialogKind: String, Hashable, Sendable {
    case none
    case before
    case confirm
    case undo
}

enum MoveLookKind: String, Hashable, Sendable {
    case ready
    case pending
    case failed
}

enum MoveNoteKind: String, Hashable, Sendable {
    case none
    case leftOut = "leftout"
    case invite
    case keys
    case keysOrPasswords = "keysorpasswords"
    case checkShows = "checkshows"
}

enum MoveListSource: Hashable, Sendable {
    case planGroup
    case reportGroup
    case beforeGroup
    case notMoved
    case notMovedApp
    case keysAgain
    case notes
    case invitees
    case sourceKeys
    case downloaderKeys
}

enum MoveNoticeType: String, Hashable, Sendable {
    case readDone = "readdone"
    case readFailed = "readfailed"
    case readStopped = "readstopped"
    case linkedFailed = "linkedfailed"
    case importFinished = "importfinished"
    case importFailed = "importfailed"
    case importUndone = "importundone"
    case undoFailed = "undofailed"
}

enum MoveBadgeKind: String, Hashable, Sendable {
    case none
    case you
    case plex
    case jellyfin
    case emby
    case invite
}

struct MoveAppInfo: Hashable, Sendable, Identifiable {
    let kind: String
    let name: String
    let port: Int

    var id: String { kind }
}

struct MoveCountInfo: Hashable, Sendable {
    let kind: MoveCountWord
    let n: Int
}

struct MoveSummaryInfo: Hashable, Sendable {
    let n: Int
    let word: MoveWordKind
    let raw: String
}

struct MoveGateInfo: Hashable, Sendable {
    var ok = true
    var reason: MoveGateKind = .none
    var n = 0
}

struct MoveCheckNoteInfo: Hashable, Sendable {
    let step: MoveCheckStepKind
    let done: Int
    let total: Int
    let now: String
    let issues: Int

    var counting: Bool { step == .titles && total > 0 }
}

struct MoveProgressInfo: Hashable, Sendable {
    var done = 0
    var total = 0
    var waitLeft = 0
    var waitMinutes = 0
    var raw = ""

    var isCount: Bool { total > 0 }
}

struct MoveProblemInfo: Hashable, Sendable {
    let error: ErrorInfo
    let reach: MoveReachKind
}

struct MoveNoticeInfo: Hashable, Sendable, Identifiable {
    let seq: Int
    let kind: MoveNoticeType
    let name: String
    let n: Int
    let moveId: Int64
    let counts: [MoveCountInfo]
    let problem: MoveProblemInfo?

    var id: Int { seq }
}

struct MoveReadInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let kind: String
    let name: String
    let address: String
    let host: String
    let showHost: Bool
    let version: String
    let createdAt: Date?
    let stats: [MoveCountInfo]
    let usedBy: Int64
    let picked: Bool
}

struct MoveSavedKeyInfo: Hashable, Sendable {
    let kind: String
    let address: String
}

struct MoveEarlierInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let createdAt: Date?
    let counts: [MoveSummaryInfo]
    let word: MoveStateKind
    let tone: ToneKind
    let canRemove: Bool
}

struct MoveBroughtInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let app: String
    let name: String
    let kind: String
    let needs: String
}

struct MoveFoundInfo: Hashable, Sendable {
    let kind: String
    let name: String
    let address: String
    let version: String
}

struct MoveLinkedInfo: Hashable, Sendable {
    let kind: String
    let name: String
    let address: String
    let hasKey: Bool
    let reachable: Bool
    let version: String
    let offered: Bool
}

struct MoveProbeInfo: Hashable, Sendable {
    let ok: Bool
    let app: String
    let version: String
    let problem: MoveProblemInfo?
}

struct MoveSheetInfo: Hashable, Sendable {
    var probing = false
    var probe: MoveProbeInfo?
    var detected = ""
    var finding = false
    var found: [MoveFoundInfo] = []
    var searched: [String] = []
    var findRan = false
    var findProblem: MoveProblemInfo?
    var linked: [MoveLinkedInfo] = []
    var linkedFrom = ""
    var addingLinked = false
    var reading = false
    var readProblem: MoveProblemInfo?
    var stopped = false

    var addable: [MoveLinkedInfo] { linked.filter { $0.hasKey && $0.reachable } }
}

struct MoveFolderInfo: Hashable, Sendable, Identifiable {
    let oldPath: String
    let kind: String
    let unseen: Bool
    let options: [String]
    let suggested: String
    let pick: MovePickChoiceKind
    let path: String
    let other: String
    let problem: MovePathProblemKind
    let gone: String
    let twins: [String]
    let canAddRule: Bool

    var id: String { oldPath }
}

struct MoveGroupInfo: Hashable, Sendable, Identifiable {
    let id: String
    let group: MoveGroupKind
    let label: String
    let count: Int
    let on: Bool
    let canSkip: Bool
    let note: MoveNoteKind
    let noteCount: Int
    let showFormats: Bool
}

struct MoveTileInfo: Hashable, Sendable, Identifiable {
    let id: String
    let group: MoveGroupKind
    let label: String
    let n: Int
}

struct MovePersonInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let app: String
    let owner: Bool
    let on: Bool

    var id: String { key }
}

struct MoveRuleInfo: Hashable, Sendable {
    let from: String
    let to: String
    let problem: MovePathProblemKind
}

struct MoveNotMovedAppInfo: Hashable, Sendable, Identifiable {
    let app: String
    let name: String
    let n: Int

    var id: String { app }
}

struct MoveChoiceInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let why: MoveWhyKind
    let choice: MovePersonChoiceKind?

    var id: String { key }
}

struct MoveBeforeInfo: Hashable, Sendable, Identifiable {
    let id: String
    let group: MoveGroupKind
    let label: String
    let n: Int
    let word: MoveWordKind
}

struct MoveOutcomeInfo: Hashable, Sendable {
    let came: Int
    let attention: Int
    let attentionFolders: [String]
    let unchecked: Int
    let uncheckedFolders: [String]
}

struct MoveChooseInfo: Hashable, Sendable {
    var planning = false
    var planned = false
    var note: MoveCheckNoteInfo?
    var planProblem: MoveProblemInfo?
    var empty = false
    var tiles: [MoveTileInfo] = []
    var alsoGroups: [MoveGroupKind] = []
    var stillChecking = 0
    var needFolders: [MoveFolderInfo] = []
    var folders: [MoveFolderInfo] = []
    var people: [MovePersonInfo] = []
    var namesAsk = false
    var names: MoveNamesKind = .keep
    var formatsAsk = false
    var noFormats = false
    var sourceKeys = 0
    var downloaderKeys = 0
    var invitees = 0
    var groups: [MoveGroupInfo] = []
    var emptyGroups: [String] = []
    var rules: [MoveRuleInfo] = []
    var showRules = false
    var notMovedCount = 0
    var notMovedApps: [MoveNotMovedAppInfo] = []
    var trying = false
    var tried = false
    var stale = false
    var tryFailed = false
    var tryProblem: MoveProblemInfo?
    var tryNotice = false
    var problems = 0
    var openChoices = 0
    var total = 0
    var choices: [MoveChoiceInfo] = []
    var before: [MoveBeforeInfo] = []
    var outcome: MoveOutcomeInfo?
    var apps: [String] = []
    var toCheck = 0
    var checkingNow = false
}

struct MoveSeasonInfo: Hashable, Sendable {
    let name: String
    let titleId: Int64
}

struct MoveResultGroupInfo: Hashable, Sendable, Identifiable {
    let id: String
    let group: MoveGroupKind
    let summary: [MoveSummaryInfo]
}

struct MoveRunInfo: Hashable, Sendable {
    let id: Int64
    let createdAt: Date?
    let word: MoveStateKind
    let tone: ToneKind
    let running: Bool
    let undoing: Bool
    let undone: Bool
    let failed: Bool
    let undoFailed: Bool
    let progress: Double
    let note: MoveProgressInfo
    let lost: Bool
    let counts: [MoveSummaryInfo]
    let failedCount: Int
    let hasReport: Bool
    let look: MoveLookKind
    let next: [String]
    let plainNext: Bool
    let noFolder: Int
    let renameNext: Bool
    let openChoices: Int
    let seasons: [MoveSeasonInfo]
    let madeAlerts: Bool
    let outcome: MoveOutcomeInfo?
    let groups: [MoveResultGroupInfo]
    let notMoved: Int
    let keysAgain: Int
    let notes: Int
    let canUndo: Bool
    let needsYou: Bool
}

struct MoveInfo: Hashable, Sendable {
    var load: LoadInfo = .loading
    var allowed = true
    var owner = false
    var safeMode = false
    var step: MoveStepKind = .read
    var reads: [MoveReadInfo] = []
    var pickedCount = 0
    var savedKeys: [MoveSavedKeyInfo] = []
    var moves: [MoveEarlierInfo] = []
    var movesFailed = false
    var brought: [MoveBroughtInfo] = []
    var sheet = MoveSheetInfo()
    var choose = MoveChooseInfo()
    var run: MoveRunInfo?
    var gate = MoveGateInfo()
    var dialog: MoveDialogKind = .none
    var starting = false
    var problem: MoveProblemInfo?
    var gone = false
    var notices: [MoveNoticeInfo] = []
    var readingKind = ""
    var canStart = false

    static let initial = MoveInfo()

    func savedKey(kind: String, startAddress: String, editing: Bool) -> MoveSavedKeyInfo? {
        if editing {
            return savedKeys.first { $0.kind == kind && (startAddress.isEmpty || $0.address == startAddress) }
        }
        return savedKeys.first { key in key.kind == kind && !reads.contains { $0.address == key.address } }
    }
}

struct MoveFormInfo: Hashable, Sendable {
    var kind: String
    var address = ""
    var key = ""
    var username = ""
    var password = ""
    var useSavedKey = false
}

struct MoveItemInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let hint: String
    let word: MoveWordKind?
    let raw: String
    let tone: MoveToneKind
    let look: Bool
    let traits: String
    let skipped: String
    let needs: String
    let badge: MoveBadgeKind
    let group: MoveGroupKind
    let section: String
    let n: Int

    var id: String { key }

    func matches(_ query: String) -> Bool {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        return q.isEmpty || name.lowercased().contains(q) || hint.lowercased().contains(q)
    }
}

struct MoveListInfo: Hashable, Sendable {
    let rows: [MoveItemInfo]

    var showFilter: Bool { rows.contains { $0.look } && rows.contains { !$0.look } }

    func page(query: String, onlyLook: Bool, limit: Int) -> (rows: [MoveItemInfo], total: Int) {
        let ordered = showFilter ? rows.filter { $0.look } + rows.filter { !$0.look } : rows
        let all = ordered.filter { (!onlyLook || !showFilter || $0.look) && $0.matches(query) }
        return (Array(all.prefix(limit)), all.count)
    }
}
