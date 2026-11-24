import Foundation

enum GreetingTimeID: String, Hashable, Sendable {
    case morning
    case afternoon
    case evening
}

enum NewsKindID: String, Hashable, Sendable {
    case askedArrivedAny = "askedarrivedany"
    case askedArrivedOne = "askedarrivedone"
    case askedArrivedMany = "askedarrivedmany"
    case waiting
    case waitingPage = "waitingpage"
    case newTodayAny = "newtodayany"
    case newTodayOne = "newtodayone"
    case newTodayMany = "newtodaymany"
    case mineFinishing = "minefinishing"
    case finishing
}

struct HomeNewsInfo: Hashable, Sendable {
    let kind: NewsKindID
    let count: Int
    let title: String
}

enum HomeBoardID: String, Hashable, Sendable, CaseIterable, Identifiable {
    case home
    case system

    var id: Self { self }
}

enum HomeTileKindID: String, Hashable, Sendable {
    case setup
    case attention
    case asks
    case library
    case disk
    case week
    case downloads
    case new
    case explore
    case popular
    case calendar
    case search
}

struct HomeTileInfo: Identifiable, Hashable, Sendable {
    var id: HomeTileKindID { kind }
    let kind: HomeTileKindID
    let target: SectionID?
}

enum PictureLabelID: String, Hashable, Sendable {
    case newSeason = "newseason"
    case arrived
    case comingSoon = "comingsoon"
    case following
}

enum ArrivedTitleID: String, Hashable, Sendable {
    case newToday = "newtoday"
    case newThisWeek = "newthisweek"
    case justArrived = "justarrived"
    case recentlyAdded = "recentlyadded"
}

struct HomePictureInfo: Identifiable, Hashable, Sendable {
    var id: String { card.id }
    let card: DiscoverCardInfo
    let label: PictureLabelID
    let arrived: ArrivedTitleID
}

enum SetupStepKindID: String, Hashable, Sendable {
    case folder
    case source
    case downloader
    case title
}

struct SetupStepInfo: Identifiable, Hashable, Sendable {
    var id: SetupStepKindID { kind }
    let kind: SetupStepKindID
    let done: Bool
    let target: SectionID?
}

struct AsksTileInfo: Hashable, Sendable {
    let approver: Bool
    let waiting: Int
    let posters: [String]
    let open: Int
    let declined: Int
    let failed: Bool
}

struct LibraryKindInfo: Hashable, Sendable {
    let kind: CardKindID
    let titles: Int
    let parts: Int
    let items: Int
}

struct DiskTileInfo: Hashable, Sendable {
    let size: Int64
    let free: Int64?
    let many: Bool
    let tone: ToneKind
    let failed: Bool
}

struct DownloadsTileInfo: Hashable, Sendable {
    let fetching: Int
    let downloading: Int
    let importing: Int
    let waiting: Int
    let paused: Int
}

struct WeekTileInfo: Hashable, Sendable {
    let count: Int
    let posters: [String]
    let borrowed: Bool
}

enum AttentionKindID: String, Hashable, Sendable {
    case health
    case downloadFailed = "downloadfailed"
    case importRefused = "importrefused"
    case decision
    case setup
}

enum AttentionTargetID: String, Hashable, Sendable {
    case settings
    case health
    case downloads

    var section: SectionID {
        switch self {
        case .settings: .settings
        case .health: .health
        case .downloads: .downloads
        }
    }
}

struct AttentionRowInfo: Identifiable, Hashable, Sendable {
    let id: String
    let tone: ToneKind
    let kind: AttentionKindID
    let count: Int
    let title: String
    let checkKind: String
    let error: Bool
    let names: [String]
    let target: AttentionTargetID
}

struct AttentionTileInfo: Hashable, Sendable {
    let rows: [AttentionRowInfo]
    let worst: ToneKind
    let unchecked: Bool
    let more: Int
}

struct KindLimitInfo: Hashable, Sendable {
    let kind: CardKindID
    let noLimit: Bool
    let perWeek: Int
    let left: Int
}

struct JoinedWelcomeInfo: Hashable, Sendable {
    let name: String
    let canAsk: Bool
    let kinds: [CardKindID]
    let noLimit: Bool
    let limits: [KindLimitInfo]
}

enum DownloadStageID: String, Hashable, Sendable {
    case checking
    case queued
    case paused
    case downloading
    case stalled
    case importing
    case decide
    case failed
    case seeding
    case done
}

struct SystemDownloadInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let stage: DownloadStageID
    let progress: Double
    let speed: Double
    let finishedAt: Date?
}

struct PartCountInfo: Hashable, Sendable {
    let total: Int
    let working: Int
    let off: Int
    let troubled: [String]
}

struct SystemBoardInfo: Hashable, Sendable {
    let speed: Double
    let waiting: Int
    let downloading: Int
    let importing: Int
    let running: [SystemDownloadInfo]
    let finished: [SystemDownloadInfo]
    let paused: Int
    let notSent: Int
    let downloadsFailed: Bool
    let attention: [AttentionRowInfo]
    let passed: Int
    let unchecked: Bool
    let sources: PartCountInfo?
    let downloaders: PartCountInfo?
    let sourcesOff: Bool
    let uptimeSeconds: Int64
    let version: String
}

enum UpWhenID: String, Hashable, Sendable {
    case outNow = "outnow"
    case today
    case date
    case text
}

struct ComingUpInfo: Identifiable, Hashable, Sendable {
    let id: String
    let tmdb: String
    let kind: String
    let name: String
    let image: String?
    let sub: String
    let when: UpWhenID
    let date: String
    let text: String
}

struct HomeInfo: Hashable, Sendable {
    let load: LoadInfo
    let greeting: GreetingTimeID
    let name: String
    let news: HomeNewsInfo?
    let system: Bool
    let board: HomeBoardID
    let owner: Bool
    let finishSetup: Bool
    let joined: JoinedWelcomeInfo?
    let welcome: Bool
    let steps: [SetupStepInfo]
    let stepsDone: Int
    let nextStep: SetupStepInfo?
    let pictures: [HomePictureInfo]
    let tiles: [HomeTileInfo]
    let attention: AttentionTileInfo?
    let asks: AsksTileInfo?
    let library: [LibraryKindInfo]
    let libraryTotal: Int
    let disk: DiskTileInfo?
    let downloads: DownloadsTileInfo?
    let week: WeekTileInfo?
    let newArrivals: Int
    let newSeasons: Int
    let popularArt: String?
    let popularBuilding: Bool
    let arrived: CardRowInfo?
    let arrivedTitle: ArrivedTitleID
    let comingUp: [ComingUpInfo]
    let borrowed: [CardRowInfo]
    let systemBoard: SystemBoardInfo?
    let settled: Bool

    static let initial = HomeInfo(
        load: .loading, greeting: .morning, name: "", news: nil, system: false, board: .home, owner: false, finishSetup: false,
        joined: nil, welcome: false, steps: [], stepsDone: 0, nextStep: nil, pictures: [], tiles: [], attention: nil, asks: nil,
        library: [], libraryTotal: 0, disk: nil, downloads: nil, week: nil, newArrivals: 0, newSeasons: 0, popularArt: nil,
        popularBuilding: false, arrived: nil, arrivedTitle: .justArrived, comingUp: [], borrowed: [], systemBoard: nil, settled: false
    )
}
