import Foundation

enum MemberTabKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case waiting
    case approved
    case declined
    case ready
    case failed

    var id: Self { self }
}

enum ApproverTabKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case waiting
    case processing
    case done

    var id: Self { self }
}

enum KindFilterKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case all
    case movie
    case show
    case artist

    var id: Self { self }
}

enum StageKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case failed
    case waiting
    case upcoming
    case searching
    case downloading
    case importing
    case needsDecision = "needsdecision"
    case looking
    case processing
    case ready
    case declined

    var id: Self { self }
}

enum StepKindInfo: String, Hashable, Sendable {
    case searching
    case downloading
    case importing
    case waitingOnAdmin = "waitingonadmin"
    case waiting
    case notOut = "notout"
    case idle = "none"
}

enum PendingKindInfo: String, Hashable, Sendable {
    case approve
    case decline
}

enum RequestsNoteKindInfo: String, Hashable, Sendable {
    case approved
    case declined
    case approvedMany = "approvedmany"
    case approveFailed = "approvefailed"
    case declineFailed = "declinefailed"
    case alreadyDecided = "alreadydecided"
    case notice
    case removed
    case nothingToRemove = "nothingtoremove"
    case removeFailed = "removefailed"
    case takenBack = "takenback"
    case takeBackFailed = "takebackfailed"
}

enum ClearKindInfo: String, Hashable, Sendable {
    case failed
    case finished
}

enum LimitKindInfo: String, CaseIterable, Hashable, Sendable {
    case movies
    case shows
    case music
}

enum AskFailureKind: String, Hashable, Sendable {
    case weeklyLimit = "weeklylimit"
    case neverAdd = "neveradd"
    case inLibrary = "inlibrary"
    case ageLimit = "agelimit"
    case noMusic = "nomusic"
    case alreadyDecided = "alreadydecided"
    case askedAgain = "askedagain"
    case notIncluded = "notincluded"
    case other
}

enum RequestPageKind: String, Hashable, Sendable, Identifiable {
    case rules
    case outsideLists = "outsidelists"
    case neverAdd = "neveradd"

    var id: Self { self }
}

struct WaitInfo: Hashable, Sendable {
    let days: Int
    let hours: Int
    let minutes: Int
    let underMinute: Bool
}

struct LimitLineInfo: Hashable, Sendable, Identifiable {
    let kind: LimitKindInfo
    let left: Int
    let perWeek: Int
    let wait: WaitInfo?

    var id: LimitKindInfo { kind }
}

struct RequestErrorInfo: Hashable, Sendable {
    let error: ErrorInfo
    let failure: AskFailureKind
}

struct AskPersonInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let fromWatchlist: Bool
}

struct AskInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let kind: String
    let tmdbId: String
    let name: String
    let year: Int
    let poster: String
    let state: String
    let stateLabel: String
    let facts: [String]
    let qualityTag: String?
    let qualityName: String
    let askedWhat: String
    let seasons: [Int]
    let futureOnly: Bool
    let askedAt: Date?
    let decidedAt: Date?
    let askers: [String]
    let watchlist: [AskPersonInfo]
    let decider: String
    let autoApproved: Bool
    let note: String
    let alsoDeclined: [String]
    let problem: String
    let step: StepKindInfo
    let stepText: String
    let searchedAt: Date?
    let viaPlugin: String
    let mine: Bool
    let isWaiting: Bool
    let isFailed: Bool
    let isArtist: Bool
    let stage: StageKind
    let tone: ToneKind
    let progress: Double
    let sharedCount: Int
}

struct RequestsNoteInfo: Hashable, Sendable {
    let id: Int64
    let kind: RequestsNoteKindInfo
    let name: String
    let count: Int
    let by: String
    let state: String
    let text: String
    let error: RequestErrorInfo?
    let keys: [Int64]
}

struct RequestsInfo: Hashable, Sendable {
    let load: LoadInfo
    let approver: Bool
    let canAsk: Bool
    let myId: Int64
    let shown: [AskInfo]
    let isEmpty: Bool
    let memberTab: MemberTabKind
    let memberTabs: [MemberTabKind]
    let memberCounts: [MemberTabKind: Int]
    let approverTab: ApproverTabKind
    let approverCounts: [ApproverTabKind: Int]
    let failedCount: Int
    let kind: KindFilterKind
    let kinds: [KindFilterKind]
    let stage: StageKind?
    let stages: [StageKind]
    let stageCounts: [StageKind: Int]
    let filtersOn: Int
    let pending: [Int64: PendingKindInfo]
    let limits: [LimitLineInfo]
    let newIds: Set<Int64>
    let reportsOpen: Int
    let hasOwnReports: Bool
    let isCut: Bool
    let loaded: Int
    let total: Int
    let busy: Bool
    let canApproveAll: Bool
    let waitingToApprove: Int
    let clearable: ClearKindInfo?
    let clearCount: Int
    let note: RequestsNoteInfo?

    static let initial = RequestsInfo(
        load: .loading, approver: false, canAsk: false, myId: 0, shown: [], isEmpty: false, memberTab: .waiting, memberTabs: [],
        memberCounts: [:], approverTab: .waiting, approverCounts: [:], failedCount: 0, kind: .all, kinds: [], stage: nil, stages: [],
        stageCounts: [:], filtersOn: 0, pending: [:], limits: [], newIds: [], reportsOpen: -1, hasOwnReports: false, isCut: false,
        loaded: 0, total: 0, busy: false, canApproveAll: false, waitingToApprove: 0, clearable: nil, clearCount: 0, note: nil
    )
}

struct InboxInfo: Hashable, Sendable {
    let unread: Int
    let waiting: Int
    let reportsOpen: Int
    let requestsBadge: Int
    let hasAttention: Bool

    static let empty = InboxInfo(unread: 0, waiting: 0, reportsOpen: 0, requestsBadge: 0, hasAttention: false)
}

enum TickKind: String, Hashable, Sendable {
    case all
    case some
    case off = "none"
}

enum AlbumKindInfo: String, CaseIterable, Hashable, Sendable {
    case albums
    case singles
    case live
    case compilations
    case other
}

enum PillNoteKind: String, Hashable, Sendable {
    case plain = "none"
    case inLibrary = "inlibrary"
    case requested
}

struct QualityPillInfo: Hashable, Sendable, Identifiable {
    let key: String
    let line: String
    let name: String
    let rulesId: Int64
    let extra: Bool
    let disabled: Bool
    let note: PillNoteKind

    var id: String { key }
}

struct EpisodeTickInfo: Hashable, Sendable, Identifiable {
    let number: Int
    let name: String
    let ticked: Bool

    var id: Int { number }
}

struct SeasonPickInfo: Hashable, Sendable, Identifiable {
    let number: Int
    let all: Int
    let asked: Int
    let kept: Int
    let tick: TickKind
    let expanded: Bool
    let loading: Bool
    let failed: Bool
    let episodes: [EpisodeTickInfo]

    var id: Int { number }
}

struct SongTickInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let disc: Int
    let number: Int
    let ticked: Bool

    var id: String { key }
}

struct AlbumPickInfo: Hashable, Sendable, Identifiable {
    let mbid: String
    let name: String
    let year: Int
    let kind: AlbumKindInfo
    let editions: Int
    let tick: TickKind
    let expanded: Bool
    let songs: [SongTickInfo]
    let kept: Int

    var id: String { mbid }
}

struct AskReviewInfo: Hashable, Sendable {
    let load: LoadInfo
    let ask: AskInfo?
    let seasons: [SeasonPickInfo]
    let albums: [AlbumPickInfo]
    let albumCounts: [AlbumKindInfo: Int]
    let pills: [QualityPillInfo]
    let selected: String
    let changed: Bool
    let empty: Bool
    let canApprove: Bool
    let showsQuality: Bool
    let extraOnly: Bool
    let keptSeasons: Int
    let keptEpisodes: Int
    let allSongs: Int
    let keptSongs: Int

    static let initial = AskReviewInfo(
        load: .loading, ask: nil, seasons: [], albums: [], albumCounts: [:], pills: [], selected: "", changed: false, empty: false,
        canApprove: false, showsQuality: false, extraOnly: false, keptSeasons: 0, keptEpisodes: 0, allSongs: 0, keptSongs: 0
    )
}

enum ShowModeKind: String, Hashable, Sendable {
    case all
    case choose
    case future
}

enum AskOutcomeKind: String, Hashable, Sendable {
    case requested
    case approved
    case joined
    case already
    case turnedDown = "turneddown"
}

enum EpisodesLoadKind: String, Hashable, Sendable {
    case idle = "none"
    case loading
    case failed
    case ready
}

struct AskSeasonInfo: Hashable, Sendable, Identifiable {
    let number: Int
    let name: String
    let kept: Bool
    let complete: Bool
    let ticked: Bool
    let expanded: Bool
    let load: EpisodesLoadKind
    let episodes: [EpisodeTickInfo]
    let picked: Int
    let available: Bool

    var id: Int { number }
}

struct AskBriefInfo: Hashable, Sendable {
    let stateLabel: String
    let mine: Bool
    let seasons: [Int]
    let albums: [String]
}

struct AskSheetInfo: Hashable, Sendable {
    let load: LoadInfo
    let isShow: Bool
    let name: String
    let year: Int
    let neverAdd: Bool
    let brief: AskBriefInfo?
    let joining: Bool
    let pills: [QualityPillInfo]
    let showsQuality: Bool
    let selected: String
    let alsoPills: [QualityPillInfo]
    let also: Set<String>
    let follow: Bool
    let mode: ShowModeKind
    let chooseAllowed: Bool
    let seasons: [AskSeasonInfo]
    let pickedSeasons: [Int]
    let pickedEpisodes: Int
    let busy: Bool
    let error: RequestErrorInfo?
    let outcome: AskOutcomeKind?
    let outcomeFourK: Bool
    let canSubmit: Bool

    static let initial = AskSheetInfo(
        load: .loading, isShow: false, name: "", year: 0, neverAdd: false, brief: nil, joining: false, pills: [], showsQuality: false,
        selected: "", alsoPills: [], also: [], follow: true, mode: .all, chooseAllowed: false, seasons: [], pickedSeasons: [],
        pickedEpisodes: 0, busy: false, error: nil, outcome: nil, outcomeFourK: false, canSubmit: false
    )
}

enum MusicModeKind: String, Hashable, Sendable {
    case every
    case some
}

struct AskAlbumInfo: Hashable, Sendable, Identifiable {
    let mbid: String
    let name: String
    let year: Int
    let kind: AlbumKindInfo
    let kept: Bool
    let leftOut: Bool
    let ticked: Bool
    let available: Bool

    var id: String { mbid }
}

struct AskMusicInfo: Hashable, Sendable {
    let load: LoadInfo
    let name: String
    let albums: [AskAlbumInfo]
    let mode: MusicModeKind
    let follow: Bool
    let brief: AskBriefInfo?
    let picked: [String]
    let someAllowed: Bool
    let busy: Bool
    let error: RequestErrorInfo?
    let outcome: AskOutcomeKind?
    let canSubmit: Bool

    static let initial = AskMusicInfo(
        load: .loading, name: "", albums: [], mode: .every, follow: true, brief: nil, picked: [], someAllowed: false, busy: false,
        error: nil, outcome: nil, canSubmit: false
    )
}

enum ReportFilterKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case open
    case fixed
    case all

    var id: Self { self }
}

enum ReportKindInfo: String, CaseIterable, Hashable, Sendable, Identifiable {
    case picture
    case sound
    case subtitles
    case wontPlay = "wontplay"
    case other

    var id: Self { self }
}

enum ReportsNoteKindInfo: String, Hashable, Sendable {
    case fixed
    case fixFailed = "fixfailed"
    case reopened
    case reopenFailed = "reopenfailed"
    case searchStarted = "searchstarted"
    case searchFailed = "searchfailed"
}

struct ReportInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let titleName: String
    let titleKind: String
    let year: Int
    let poster: String
    let part: String
    let version: String
    let kind: ReportKindInfo
    let note: String
    let isOpen: Bool
    let mine: Bool
    let reporter: String
    let fixer: String
    let reply: String
    let createdAt: Date?
    let closedAt: Date?
}

struct ReportsNoteInfo: Hashable, Sendable {
    let id: Int64
    let kind: ReportsNoteKindInfo
    let name: String
    let reporter: String
    let error: RequestErrorInfo?
}

struct ReportsInfo: Hashable, Sendable {
    let load: LoadInfo
    let manager: Bool
    let filter: ReportFilterKind
    let counts: [ReportFilterKind: Int]
    let shown: [ReportInfo]
    let busy: Set<Int64>
    let searching: Set<Int64>
    let note: ReportsNoteInfo?

    static let initial = ReportsInfo(load: .loading, manager: false, filter: .open, counts: [:], shown: [], busy: [], searching: [], note: nil)
}

struct ReportProblemInfo: Hashable, Sendable {
    let kind: ReportKindInfo?
    let note: String
    let left: Int
    let busy: Bool
    let error: RequestErrorInfo?
    let missingKind: Bool
    let sent: ReportInfo?

    static let initial = ReportProblemInfo(kind: nil, note: "", left: 500, busy: false, error: nil, missingKind: false, sent: nil)
}

enum FolderFieldKind: String, Hashable, Sendable {
    case movie
    case show
    case music
    case movieFourK = "moviefourk"
    case showFourK = "showfourk"
}

enum SettingsFlagKind: String, Hashable, Sendable {
    case searchOnApprove = "searchonapprove"
    case ownerAutoApprove = "ownerautoapprove"
    case plexWatchlists = "plexwatchlists"
    case plexEveryone = "plexeveryone"
    case plexFriends = "plexfriends"
    case pickMany = "pickmany"
    case skipOnServices = "skiponservices"
}

enum LineKindInfo: String, CaseIterable, Hashable, Sendable, Identifiable {
    case movie
    case show

    var id: Self { self }
}

struct PickOption: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
}

struct QualityLineInfo: Hashable, Sendable, Identifiable {
    let index: Int
    let lineId: String
    let name: String
    let label: String
    let hidden: Bool
    let pickRulesId: Int64
    let rulesName: String
    let extra: Bool
    let ready: Bool
    let isOwn: Bool

    var id: String { lineId }
}

struct PlexFriendInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let plexName: String
    let kid: Bool
    let checkedAt: Date?
    let error: String
    let asked: Int
}

struct ServiceInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let logo: String
    let picked: Bool
}

struct OnServicesInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let kind: String
    let name: String
    let year: Int
    let tmdbId: String
    let bytes: Int64
    let service: String
}

struct CleanupInfo: Hashable, Sendable {
    let running: Bool
    let checked: Int
    let total: Int
    let failed: Int
    let finished: Bool
    let found: [OnServicesInfo]
}

struct RequestRulesInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let limits: [LimitKindInfo: String]
    let limitsValid: Bool
    let folders: [FolderFieldKind: [PickOption]]
    let rules: [FolderFieldKind: [PickOption]]
    let folder: [FolderFieldKind: Int64]
    let pickRules: [FolderFieldKind: Int64]
    let flags: [SettingsFlagKind: Bool]
    let flagEnabled: [SettingsFlagKind: Bool]
    let lines: [LineKindInfo: [QualityLineInfo]]
    let canAddLine: [LineKindInfo: Bool]
    let showFriends: Bool
    let friendsLoaded: Bool
    let ownerLinked: Bool
    let friends: [PlexFriendInfo]
    let region: String
    let services: [ServiceInfo]
    let servicesLoading: Bool
    let servicesError: ErrorInfo?
    let pickedCount: Int
    let cleanup: CleanupInfo?
    let cleanupError: RequestErrorInfo?
    let canCheck: Bool
    let dirty: Bool
    let canSave: Bool
    let saving: Bool
    let saveError: RequestErrorInfo?
    let savedCount: Int

    static let initial = RequestRulesInfo(
        load: .loading, allowed: true, limits: [:], limitsValid: true, folders: [:], rules: [:], folder: [:], pickRules: [:], flags: [:],
        flagEnabled: [:], lines: [:], canAddLine: [:], showFriends: false, friendsLoaded: false, ownerLinked: false, friends: [], region: "",
        services: [], servicesLoading: false, servicesError: nil, pickedCount: 0, cleanup: nil, cleanupError: nil, canCheck: false,
        dirty: false, canSave: false, saving: false, saveError: nil, savedCount: 0
    )
}

enum ListKindInfo: String, CaseIterable, Hashable, Sendable, Identifiable {
    case tmdb
    case trakt
    case mdblist

    var id: Self { self }
}

enum ListSeasonsKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case all
    case future
    case latest
    case first
    case noSeasons = "none"

    var id: Self { self }
}

enum OutsideNoteKindInfo: String, Hashable, Sendable {
    case saved
    case savedOff = "savedoff"
    case reading
    case readFailed = "readfailed"
    case removed
    case removeFailed = "removefailed"
    case traktSaved = "traktsaved"
    case traktKeySaved = "traktkeysaved"
    case traktSignedIn = "traktsignedin"
    case traktDenied = "traktdenied"
    case traktExpired = "traktexpired"
    case traktSignedOut = "traktsignedout"
    case mdblistSaved = "mdblistsaved"
    case mdblistRemoved = "mdblistremoved"
}

struct OutsideListInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let kind: ListKindInfo
    let isShow: Bool
    let ref: String
    let link: String
    let enabled: Bool
    let syncEveryHours: Int
    let lastSyncedAt: Date?
    let lastResult: String
    let lastError: String
    let busy: Bool
}

struct ListDraftInfo: Hashable, Sendable {
    let name: String
    let kind: ListKindInfo
    let isShow: Bool
    let ref: String
    let folderId: Int64
    let rulesId: Int64
    let keepUpToDate: Bool
    let searchOnAdd: Bool
    let seasons: ListSeasonsKind
    let newSeasons: Bool
    let enabled: Bool
    let syncEveryHours: Int
}

struct DeviceCodeInfo: Hashable, Sendable {
    let userCode: String
    let url: String
}

struct OutsideListsInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let owner: Bool
    let lists: [OutsideListInfo]
    let isEmpty: Bool
    let formOpen: Bool
    let isNew: Bool
    let draft: ListDraftInfo?
    let draftFolders: [PickOption]
    let draftRules: [PickOption]
    let formError: RequestErrorInfo?
    let saving: Bool
    let canSave: Bool
    let needsTraktKey: Bool
    let traktKeySet: Bool
    let traktTokenSet: Bool
    let hasTraktList: Bool
    let traktKnown: Bool
    let traktSignedIn: Bool
    let traktUser: String
    let traktProblem: String
    let traktSecret: Bool
    let canSignIn: Bool
    let mdblistSet: Bool
    let keyBusy: Bool
    let keyError: RequestErrorInfo?
    let device: DeviceCodeInfo?
    let note: OutsideNoteInfo?

    static let initial = OutsideListsInfo(
        load: .loading, allowed: true, owner: false, lists: [], isEmpty: false, formOpen: false, isNew: true, draft: nil, draftFolders: [],
        draftRules: [], formError: nil, saving: false, canSave: false, needsTraktKey: false, traktKeySet: false, traktTokenSet: false,
        hasTraktList: false, traktKnown: false, traktSignedIn: false, traktUser: "", traktProblem: "", traktSecret: false, canSignIn: false,
        mdblistSet: false, keyBusy: false, keyError: nil, device: nil, note: nil
    )
}

struct OutsideNoteInfo: Hashable, Sendable {
    let id: Int64
    let kind: OutsideNoteKindInfo
    let name: String
}

struct NeverAddEntryInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let isShow: Bool
    let tmdb: String
    let name: String
    let reason: String
    let removing: Bool
}

struct NeverAddInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let entries: [NeverAddEntryInfo]
    let busy: Bool
    let error: RequestErrorInfo?
    let missing: Bool
    let added: Int
    let removed: String

    static let initial = NeverAddInfo(load: .loading, allowed: true, entries: [], busy: false, error: nil, missing: false, added: 0, removed: "")
}
