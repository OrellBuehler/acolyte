import Foundation

enum TitleTabKind: String, Hashable, Sendable {
    case overview
    case library
}

enum TitleProblemKind: String, Hashable, Sendable {
    case none
    case blocked
    case notfound
    case notinlibrary
}

enum AskOfferKind: String, Hashable, Sendable {
    case ask
    case asktoo
    case asked
    case seasons
}

enum HeroPillKind: String, Hashable, Sendable {
    case none
    case hidden
    case neveradd
    case asked
    case approved
    case turneddown
    case available
}

enum TitleActionKind: String, Hashable, Sendable {
    case search
    case chooserelease
    case rename
    case addversion
    case settings
    case keep
    case stopkeeping
    case refresh
    case history
    case delete
    case report
    case hide
}

enum LibraryKindValue: String, Hashable, Sendable {
    case have
    case downloading
    case failed
    case missing
}

enum LibraryLabelKind: String, Hashable, Sendable {
    case inlibrary
    case downloaded
    case downloading
    case downloadingpercent
    case downloadingepisodes
    case failedepisodes
    case episodesof
    case downloadfailed
    case notdownloaded
}

enum StatusSubKind: String, Hashable, Sendable {
    case leftindownloader
    case downloadinghandsoff
    case fullseasons
    case havemovie
    case onitsway
    case lasttryfailed
    case waitsforcopy
    case notkept
}

enum StatusButtonKind: String, Hashable, Sendable {
    case none
    case tryagain
    case searchnow
}

enum FactKindValue: String, Hashable, Sendable {
    case network
    case studio
    case released
    case releasedyear
    case aired
    case runtime
    case seasons
    case rated
    case status
    case origin
    case language
    case spoken
    case originaltitle
    case alsoknown
    case digital
    case disc
    case nextepisode
    case budget
    case revenue
}

enum SeasonPickKind: String, Hashable, Sendable {
    case all
    case latest
    case off
    case custom
}

enum EpisodeToneKind: String, Hashable, Sendable {
    case have
    case downloading
    case failed
    case missing
}

enum FeedbackKindValue: String, Hashable, Sendable {
    case failed
    case searching
    case searchingseason
    case searchingepisode
    case searchingmissing
    case nothingtosearch
    case turnshowon
    case refreshing
    case saved
    case orderchanged
    case moving
    case markedfailed
    case versionadded
    case versionsaved
    case versionremoved
    case filesaved
    case fileadopted
    case packnext
    case hidden
    case shownagain
    case restored
    case sent
    case sending
    case notsent
    case turneddown
    case groupblocked
    case subtitlessearching
    case subtitlesnofile
    case subtitlessaved
    case subtitlesdefault
    case subtitlesaved
    case translating
    case timingfixed
    case cleaned
    case nothingtoclean
    case subtitleturneddown
    case matched
    case unmatched
    case rescanning
    case rescandone
    case renamed
    case renamechanged
    case renamefailed
    case deleted
    case added
}

struct FeedbackInfo: Hashable, Sendable {
    let serial: Int
    let kind: FeedbackKindValue
    let text: String
    let count: Int
    let error: ErrorInfo?
}

struct TitleAccessInfo: Hashable, Sendable {
    let manage: Bool
    let mayAsk: Bool
    let mayHide: Bool
    let followOn: Bool
    let subtitlesOn: Bool
    let handsOff: Bool
    let isKid: Bool
    let settings: Bool
}

struct LibraryStatusInfo: Hashable, Sendable {
    let kind: LibraryKindValue
    let label: LibraryLabelKind
    let count: Int
    let total: Int
    let tone: ToneKind
}

struct TitlePersonInfo: Identifiable, Hashable, Sendable {
    let id: String
    let personId: String
    let name: String
    let role: String
    let character: String
    let profile: String
}

struct TitleVideoInfo: Identifiable, Hashable, Sendable {
    var id: String { key }
    let key: String
    let name: String
    let type: String
    let language: String
}

struct TitleScoreInfo: Identifiable, Hashable, Sendable {
    var id: String { source }
    let source: String
    let text: String
}

struct TitleCardInfo: Identifiable, Hashable, Sendable {
    var id: String { kind + tmdbId + String(titleId) }
    let tmdbId: String
    let kind: String
    let name: String
    let year: Int
    let poster: String
    let titleId: Int64
    let inLibrary: Bool
    let asked: Bool
}

struct TitleFactInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let kind: FactKindValue
    let text: String
    let number: Int64
    let end: String
    let season: Int
    let episode: Int
}

struct TitleLinkInfo: Identifiable, Hashable, Sendable {
    var id: String { url }
    let name: String
    let url: String
}

struct OutcomeInfo: Hashable, Sendable {
    let empty: Bool
    let refused: Bool
    let note: String
    let sources: [OutcomeSourceInfo]
}

struct OutcomeSourceInfo: Hashable, Sendable {
    let name: String
    let why: String
}

struct WatchLinkInfo: Identifiable, Hashable, Sendable {
    var id: String { url }
    let server: String
    let kind: String
    let url: String
}

struct TitleFileInfo: Hashable, Sendable {
    let id: Int64
    let fileName: String
    let size: Int64
    let addedAt: Date?
    let resolutionLabel: String
    let edition: String
    let isProtected: Bool
    let audioLanguages: [String]
    let subtitleLanguages: [String]
    let epQuality: String
    let group: String
}

struct TitleDownloadInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let label: String
    let text: String
    let progress: Double
}

struct SlotCardInfo: Identifiable, Hashable, Sendable {
    var id: Int64 { slotId }
    let slotId: Int64
    let label: String
    let main: Bool
    let badges: [String]
    let versionId: Int64
    let removable: Bool
    let stateLabel: String
    let leftInDownloader: Bool
    let file: TitleFileInfo?
}

struct TitleVersionInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let label: String
    let main: Bool
    let pickRulesId: Int64
    let pickRulesName: String
    let libraryFolderId: Int64
    let folderPath: String
    let keepUpToDate: Bool
    let edition: String
    let audioLanguages: [String]
}

struct HistoryRowInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let summary: String
    let at: Date?
    let episode: String
    let version: String
    let tone: ToneKind
}

struct OwnedSeasonInfo: Identifiable, Hashable, Sendable {
    var id: Int { number }
    let number: Int
    let name: String
    let keepUpToDate: Bool
    let episodes: Int
    let inLibrary: Int
    let missing: Int
    let downloading: Int
    let failed: Int
    let nextAir: String
}

struct FolderInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let path: String
    let shown: String
}

struct RulesInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
}

struct GroupInfo: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let seasons: Int
    let episodes: Int
}

struct PointsInfo: Hashable, Sendable {
    let step: String
    let goodEnoughAt: String
    let stepReached: Bool
    let points: Int
    let goodEnoughPoints: Int?
    let goodEnough: Bool
    let meetsVersion: Bool
    let traits: [String]
}

struct UserInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
}

struct HideInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let genre: String
    let name: String
    let userId: Int64
}

struct TrailerSourceInfo: Hashable, Sendable {
    let hls: String
    let mp4: String
    let relay: String
    let youTubeURL: String
    let playable: Bool
}

struct TitleInfo: Hashable, Sendable {
    let load: LoadInfo
    let problem: TitleProblemKind
    let tab: TitleTabKind
    let kind: String
    let tmdb: String
    let ownedId: Int64
    let name: String
    let year: Int
    let overview: String
    let poster: String
    let backdrop: String
    let logo: String
    let tagline: String
    let runtime: Int
    let seasonCount: Int
    let genres: [String]
    let certification: String
    let isShow: Bool
    let isOwned: Bool
    let hasTmdb: Bool
    let legacy: Bool
    let ready: Bool
    let access: TitleAccessInfo
    let libraryStatus: LibraryStatusInfo?
    let libraryAlert: Bool
    let pill: HeroPillKind
    let askIsMine: Bool
    let canAdd: Bool
    let canCheckReleases: Bool
    let askOffer: AskOfferKind?
    let fourKOffer: AskOfferKind?
    let primaryRuns: Bool
    let actionGroups: [[TitleActionKind]]
    let trailers: [TitleVideoInfo]
    let trailerKey: String
    let directors: [TitlePersonInfo]
    let starring: [TitlePersonInfo]
    let cast: [TitlePersonInfo]
    let credits: [TitlePersonInfo]
    let keywords: [String]
    let contentNotes: [String]
    let studio: String
    let network: String
    let factScores: [TitleScoreInfo]
    let factTmdb: String
    let scores: [TitleScoreInfo]
    let tmdbScore: String
    let awardBadges: [String]
    let hasAwards: Bool
    let wins: Int
    let nominations: Int
    let collectionId: String
    let collectionName: String
    let collectionParts: [TitleCardInfo]
    let similar: [TitleCardInfo]
    let moreFromPerson: String
    let moreFromItems: [TitleCardInfo]
    let leadName: String
    let leadItems: [TitleCardInfo]
    let tmdbSeasons: Bool
    let facts: [TitleFactInfo]
    let links: [TitleLinkInfo]
    let askers: [String]
    let outcome: OutcomeInfo?
    let following: Bool
    let followBusy: Bool
    let watchLinks: [WatchLinkInfo]
    let ownedError: ErrorInfo?
    let noDownloader: String
    let statusSub: StatusSubKind?
    let fullSeasons: Int
    let ownBadges: [String]
    let mainFile: TitleFileInfo?
    let statusPercent: Int?
    let statusButton: StatusButtonKind
    let folderName: String
    let sizeBytes: Int64
    let pickRulesId: Int64
    let pickRulesName: String
    let keepUpToDate: Bool
    let showStatus: String
    let originalLanguage: String
    let addedAt: Date?
    let gateNote: Bool
    let gateReason: String
    let gateOpensAt: Date?
    let releaseGate: String
    let gateDelayDays: Int
    let libraryFolderId: Int64
    let slotCards: [SlotCardInfo]
    let versions: [TitleVersionInfo]
    let movieFileHeading: Bool
    let activeDownloads: [TitleDownloadInfo]
    let history: [HistoryRowInfo]
    let heldCount: Int
    let heldUntil: Date?
    let ownedSeasons: [OwnedSeasonInfo]
    let seasonPick: SeasonPickKind?
    let episodeOrder: String
    let episodeGroupId: String
    let episodeGroupName: String
    let packWindow: Int
    let getAllMissing: Bool
    let showOffHint: Bool
    let kindFolders: [FolderInfo]
    let kindRules: [RulesInfo]
    let presetIds: [Int64]
    let groups: [GroupInfo]
    let choicesLoaded: Bool
    let choicesError: ErrorInfo?
    let renameCount: Int
    let leftoverCount: Int
    let points: [Int64: PointsInfo]
    let users: [UserInfo]
    let hides: [HideInfo]
    let busy: String
    let feedback: FeedbackInfo?

    static func initial(kind: String, tmdb: String) -> TitleInfo {
        TitleInfo(
            load: .loading, problem: .none, tab: .overview, kind: kind, tmdb: tmdb, ownedId: 0, name: "", year: 0, overview: "",
            poster: "", backdrop: "", logo: "", tagline: "", runtime: 0, seasonCount: 0, genres: [], certification: "",
            isShow: kind == "show", isOwned: false, hasTmdb: !tmdb.isEmpty, legacy: false, ready: false,
            access: TitleAccessInfo(manage: false, mayAsk: false, mayHide: false, followOn: false, subtitlesOn: false, handsOff: false, isKid: false, settings: false),
            libraryStatus: nil, libraryAlert: false, pill: .none, askIsMine: false, canAdd: false, canCheckReleases: false,
            askOffer: nil, fourKOffer: nil, primaryRuns: false, actionGroups: [], trailers: [], trailerKey: "", directors: [],
            starring: [], cast: [], credits: [], keywords: [], contentNotes: [], studio: "", network: "", factScores: [], factTmdb: "",
            scores: [], tmdbScore: "", awardBadges: [], hasAwards: false, wins: 0, nominations: 0, collectionId: "", collectionName: "",
            collectionParts: [], similar: [], moreFromPerson: "", moreFromItems: [], leadName: "", leadItems: [], tmdbSeasons: false,
            facts: [], links: [], askers: [], outcome: nil, following: false, followBusy: false, watchLinks: [], ownedError: nil,
            noDownloader: "", statusSub: nil, fullSeasons: 0, ownBadges: [], mainFile: nil, statusPercent: nil, statusButton: .none,
            folderName: "", sizeBytes: 0, pickRulesId: 0, pickRulesName: "", keepUpToDate: false, showStatus: "", originalLanguage: "",
            addedAt: nil, gateNote: false, gateReason: "", gateOpensAt: nil, releaseGate: "", gateDelayDays: 0, libraryFolderId: 0,
            slotCards: [], versions: [], movieFileHeading: true, activeDownloads: [], history: [], heldCount: 0, heldUntil: nil,
            ownedSeasons: [], seasonPick: nil, episodeOrder: "aired", episodeGroupId: "", episodeGroupName: "", packWindow: 0,
            getAllMissing: false, showOffHint: false, kindFolders: [], kindRules: [], presetIds: [], groups: [], choicesLoaded: false,
            choicesError: nil, renameCount: 0, leftoverCount: 0, points: [:], users: [], hides: [], busy: "", feedback: nil
        )
    }
}

struct EpisodeInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let season: Int
    let episode: Int
    let name: String
    let airDay: String
    let minutes: Int
    let still: String
    let keepUpToDate: Bool
    let tone: EpisodeToneKind
    let notAired: Bool
    let slotId: Int64?
    let stateLabel: String
    let reason: String
    let slotState: String
    let points: Int?
    let goodEnoughPoints: Int?
    let file: TitleFileInfo?
}

struct SeasonEpisodesInfo: Hashable, Sendable {
    let loading: Bool
    let failed: Bool
    let episodes: [EpisodeInfo]
}

struct ShowSeasonsInfo: Hashable, Sendable {
    let open: Set<Int>
    let seasons: [Int: SeasonEpisodesInfo]
    let shown: [Int: [EpisodeInfo]]
    let outside: [EpisodeInfo]
    let belowOnly: Bool
    let belowCount: Int
    let busy: String
    let packBusy: Bool
    let historyLoaded: Bool
    let seasonHistory: [Int: [HistoryRowInfo]]
    let filesCount: [Int: Int]
    let filesSize: [Int: Int64]
    let feedback: FeedbackInfo?

    static let initial = ShowSeasonsInfo(
        open: [], seasons: [:], shown: [:], outside: [], belowOnly: false, belowCount: 0, busy: "", packBusy: false,
        historyLoaded: false, seasonHistory: [:], filesCount: [:], filesSize: [:], feedback: nil
    )
}

struct EpisodeSheetInfo: Hashable, Sendable {
    let episode: EpisodeInfo
    let manage: Bool
    let subtitlesOn: Bool
    let synopsis: String
    let events: [HistoryRowInfo]
    let eventsLoaded: Bool
    let points: PointsInfo?
    let busy: Bool
    let code: String
    let downloading: Bool
    let chosenWhy: String
    let feedback: FeedbackInfo?
}

enum ReleaseScopeValue: String, Hashable, Sendable {
    case whole
    case season
    case episode
    case check
}

enum ReleaseEmptyKind: String, Hashable, Sendable {
    case none
    case nosearch
    case nothingfound
    case dropped
    case failed
    case nogood
    case nomatch
}

enum ReleaseSortKind: String, Hashable, Sendable, CaseIterable {
    case best
    case points
    case quality
    case size
    case seeders
    case age
}

enum VerdictKindValue: String, Hashable, Sendable {
    case turneddown
    case chosen
    case held
    case wouldchoose
    case wouldhold
    case refused
    case passedover
    case judged
}

struct ReleaseReasonInfo: Hashable, Sendable {
    let text: String
}

struct ReleaseInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let size: Int64
    let seeders: Int?
    let protocolName: String
    let state: String
    let sourceName: String
    let infoURL: String
    let publishedAt: Date?
    let passed: Bool
    let points: Int
    let needsForce: Bool
    let verdict: VerdictKindValue
    let verdictTone: ToneKind
    let recommended: Bool
    let tags: [String]
    let keyReasons: [String]
    let reasons: [String]
    let group: String
}

struct ReleasesInfo: Hashable, Sendable {
    let load: LoadInfo
    let scope: ReleaseScopeValue
    let season: Int
    let episode: Int
    let titleName: String
    let check: Bool
    let wholeShow: Bool
    let running: Bool
    let allTab: Bool
    let total: Int
    let allCount: Int
    let goodCount: Int
    let sort: ReleaseSortKind
    let resolution: Int
    let source: String
    let language: String
    let query: String
    let resolutions: [Int]
    let sources: [String]
    let languages: [String]
    let shown: [ReleaseInfo]
    let empty: ReleaseEmptyKind
    let lastSearchAt: Date?
    let droppedSources: [String]
    let dropped: Int
    let rulesChanged: Bool
    let canShowMore: Bool
    let armed: Set<Int64>
    let sent: Set<Int64>
    let busy: Int64
    let rules: [RulesInfo]
    let rulesId: Int64
    let checkId: Int64
    let pickedId: Int64?
    let done: Bool
    let feedback: FeedbackInfo?
}

enum BatchLineKind: String, Hashable, Sendable {
    case waiting
    case done
    case already
    case failed
}

struct BatchItemInfo: Identifiable, Hashable, Sendable {
    var id: String { tmdbId }
    let tmdbId: String
    let name: String
    let year: Int
    let line: BatchLineKind
    let error: ErrorInfo?
}

enum LabelProblemKind: String, Hashable, Sendable {
    case empty
    case toolong
    case badcharacter
    case trailingdot
    case devicename
    case taken
}

struct AddTitleInfo: Hashable, Sendable {
    let kind: String
    let name: String
    let manage: Bool
    let handsOff: Bool
    let choose: Bool
    let load: LoadInfo
    let defaultsLate: Bool
    let folders: [FolderInfo]
    let rules: [RulesInfo]
    let folderId: Int64
    let rulesId: Int64
    let gate: String
    let keepUpToDate: Bool
    let searchOnAdd: Bool
    let secondOn: Bool
    let secondLabel: String
    let secondRulesId: Int64
    let secondFolderId: Int64
    let follow: Bool
    let seasons: String
    let newSeasons: String
    let order: String
    let source: String
    let submitting: Bool
    let error: ErrorInfo?
    let addedId: Int64
    let openPick: Bool
    let batch: [BatchItemInfo]
    let batchDone: Bool
    let isBatch: Bool
    let noFolder: Bool
    let secondProblem: LabelProblemKind?
    let canSubmit: Bool
    let sameFolder: Bool
    let newSeasonsLocked: Bool
    let feedback: FeedbackInfo?
}

enum SubtitleModeKind: String, Hashable, Sendable {
    case `default`
    case off
    case custom
}

struct SubtitleWantInfo: Hashable, Sendable {
    let language: String
    let hearingImpaired: String
    let forced: Bool
    let translateFrom: String
}

struct SubtitleRowInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let language: String
    let forced: Bool
    let hearingImpaired: Bool
    let place: String
    let codec: String
    let relativePath: String
    let site: String
    let score: Int?
    let origin: String
    let state: String
    let savedAt: Date?
    let tries: Int
    let nextTryAt: Date?
    let made: String
    let picture: Bool
    let ours: Bool
}

struct SubtitleFileInfo: Identifiable, Hashable, Sendable {
    var id: Int64 { fileId }
    let fileId: Int64
    let versionLabel: String
    let fileName: String
    let present: [SubtitleRowInfo]
    let missing: [SubtitleRowInfo]
    let translatable: [String: [SubtitleRowInfo]]
}

struct SubtitleLanguageInfo: Identifiable, Hashable, Sendable {
    var id: String { code }
    let code: String
    let name: String
}

struct SubtitleCandidateInfo: Identifiable, Hashable, Sendable {
    var id: String { site + key }
    let site: String
    let key: String
    let name: String
    let hearingImpaired: Bool
    let forced: Bool
    let trusted: Bool
    let downloads: Int
    let machineMade: Bool
    let score: Int
    let refused: Bool
}

struct SubtitleOutcomeInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let want: SubtitleWantInfo
    let result: String
    let coveredBeside: Bool
    let coveredPicture: Bool
    let covered: Bool
    let choices: [SubtitleCandidateInfo]
}

struct SubtitleChoicesInfo: Hashable, Sendable {
    let fileId: Int64
    let loading: Bool
    let sitesAsked: [String]
    let outcomes: [SubtitleOutcomeInfo]
}

enum WishProblemKind: String, Hashable, Sendable {
    case toomany
    case pick
    case twice
}

struct SubtitlesInfo: Hashable, Sendable {
    let load: LoadInfo
    let itemId: Int64
    let movie: Bool
    let listMode: Bool
    let mode: SubtitleModeKind
    let wish: [SubtitleWantInfo]
    let wishFrom: String
    let wishCodes: String
    let missingCount: Int
    let canCleanAll: Bool
    let files: [SubtitleFileInfo]
    let languages: [SubtitleLanguageInfo]
    let translator: Bool
    let editing: Bool
    let editOwn: Bool
    let editWants: [SubtitleWantInfo]
    let editProblem: WishProblemKind?
    let choices: SubtitleChoicesInfo?
    let busy: String
    let feedback: FeedbackInfo?

    func languageName(_ code: String) -> String {
        languages.first { $0.code == code }?.name ?? Locale.current.localizedString(forLanguageCode: code) ?? code
    }
}

struct LeftoverInfo: Identifiable, Hashable, Sendable {
    var id: Int64 { fileId }
    let fileId: Int64
    let fileName: String
    let size: Int64
    let resolution: Int
    let reason: String
    let droppedSeason: Int
}

struct MatchEpisodeInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let season: Int
    let episode: Int
    let name: String
    let hasFile: Bool
}

struct ShiftMoveInfo: Identifiable, Hashable, Sendable {
    var id: Int64 { fileId }
    let fileId: Int64
    let from: String
    let to: String
}

struct LeftoversInfo: Hashable, Sendable {
    let load: LoadInfo
    let show: Bool
    let handsOff: Bool
    let open: [LeftoverInfo]
    let leftAlone: [LeftoverInfo]
    let episodesLoaded: Bool
    let seasonNumbers: [Int]
    let matchFileId: Int64?
    let matchName: String
    let matchSeason: Int
    let matchPicked: Set<Int64>
    let matchEpisodes: [MatchEpisodeInfo]
    let pickedHasFile: Bool
    let shiftFrom: Int
    let shiftTo: Int
    let shiftBy: Int
    let shiftMoves: [ShiftMoveInfo]
    let rescanning: Bool
    let busy: Bool
    let renameOffered: Bool
    let feedback: FeedbackInfo?
}

struct RenameStepInfo: Identifiable, Hashable, Sendable {
    var id: Int64 { fileId }
    let fileId: Int64
    let from: String
    let to: String
    let skip: String
    let subtitles: [String]
}

struct RenameInfo: Hashable, Sendable {
    let load: LoadInfo
    let season: Int
    let changes: [RenameStepInfo]
    let skipped: [RenameStepInfo]
    let alreadyRight: Int
    let running: Bool
    let progress: Double
    let note: String
    let done: Bool
    let feedback: FeedbackInfo?
}

struct RemoveInfo: Hashable, Sendable {
    let name: String
    let show: Bool
    let season: Int
    let episodeCount: Int
    let handsOff: Bool
    let whole: Bool
    let deleteFiles: Bool
    let neverAdd: Bool
    let hasPlan: Bool
    let planScope: String
    let planSeason: Int
    let planEpisodes: [String]
    let heldCount: Int
    let keptCount: Int
    let holdingDays: Int
    let activeDownloads: Int
    let paths: [String]
    let planError: ErrorInfo?
    let running: Bool
    let done: Bool
    let ready: Bool
    let canUndo: Bool
    let feedback: FeedbackInfo?
}

struct TMDBSeasonInfo: Identifiable, Hashable, Sendable {
    var id: Int { number }
    let number: Int
    let episodeCount: Int
    let year: Int
}

struct TMDBEpisodeInfo: Identifiable, Hashable, Sendable {
    var id: Int { number }
    let number: Int
    let name: String
    let overview: String
    let runtime: Int
    let still: String
    let airDay: String
    let absolute: Int
}

struct SeasonBrowserInfo: Hashable, Sendable {
    let seasons: [TMDBSeasonInfo]
    let selected: Int
    let newSeason: Int?
    let episodes: [TMDBEpisodeInfo]
    let loading: Bool
    let failed: Bool

    static let initial = SeasonBrowserInfo(seasons: [], selected: 0, newSeason: nil, episodes: [], loading: false, failed: false)
}

struct ReviewInfo: Identifiable, Hashable, Sendable {
    let id: Int
    let author: String
    let date: String
    let rating: Double
    let text: String
    let likes: Int
    let spoiler: Bool
}

struct ReviewSourceInfo: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
}

struct ReviewsInfo: Hashable, Sendable {
    let load: LoadInfo
    let visible: Bool
    let score: Double
    let votes: Int
    let watching: Int
    let sources: [ReviewSourceInfo]
    let source: String
    let order: String
    let canHelpful: Bool
    let reviews: [ReviewInfo]
    let hasMore: Bool
    let loadingMore: Bool

    static let initial = ReviewsInfo(
        load: .loading, visible: false, score: 0, votes: 0, watching: 0, sources: [], source: "tmdb", order: "newest",
        canHelpful: false, reviews: [], hasMore: false, loadingMore: false
    )
}
