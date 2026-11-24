import Foundation

enum CardKindID: String, Hashable, Sendable {
    case movie
    case show
    case artist

    var wire: String { rawValue }
}

enum MarkStateID: String, Hashable, Sendable {
    case available
    case partial
    case downloading
    case requested
    case declined
}

enum MarkLabelID: String, Hashable, Sendable {
    case inLibrary = "inlibrary"
    case partlyAvailable = "partlyavailable"
    case downloading
    case requested
    case approvedWaiting = "approvedwaiting"
    case waitingForCopy = "waitingforcopy"
    case declined
    case hidden
}

enum FreshKindID: String, Hashable, Sendable {
    case newEpisode = "newepisode"
    case newSeason = "newseason"
    case recentlyAdded = "recentlyadded"
    case coming
    case seasonComing = "seasoncoming"
    case newRelease = "newrelease"
}

enum CardActionID: String, Hashable, Sendable {
    case add
    case ask
}

enum NoteKindID: String, Hashable, Sendable {
    case downloading
    case missingEpisodes = "missingepisodes"
    case seasonsUpToDate = "seasonsuptodate"
    case missingTracks = "missingtracks"
    case albumsUpToDate = "albumsuptodate"
    case director
}

struct CardMarkInfo: Hashable, Sendable {
    let state: MarkStateID
    let label: MarkLabelID
    let percent: Int
}

struct CardFreshInfo: Hashable, Sendable {
    let kind: FreshKindID
    let text: String
}

struct CardNoteInfo: Hashable, Sendable {
    let kind: NoteKindID
    let count: Int
}

struct DiscoverCardInfo: Identifiable, Hashable, Sendable {
    let id: String
    let kind: CardKindID
    let tmdb: String
    let titleId: Int64
    let name: String
    let year: Int
    let subtitle: String
    let poster: String?
    let backdrop: String?
    let logo: String?
    let overview: String
    let genres: [String]
    let mark: CardMarkInfo?
    let fresh: CardFreshInfo?
    let action: CardActionID?
    let streaming: String
    let rank: Int
    let mine: Bool
    let rowType: String
    let rowTitle: String
    let note: CardNoteInfo?

    var opensTitle: Bool { !tmdb.isEmpty && kind != .artist }
    var wideImage: String? { backdrop ?? poster }
    var posterImage: String? { poster ?? backdrop }
}

struct CardRowInfo: Identifiable, Hashable, Sendable {
    let id: String
    let type: String
    let title: String
    let subtitle: String
    let cards: [DiscoverCardInfo]
    let wide: Bool
    let ranked: Bool
    let seeAll: Bool
}

struct PersonCardInfo: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let note: String
    let photo: String?
}

enum ExploreTypeID: String, Hashable, Sendable, CaseIterable, Identifiable {
    case all
    case movies
    case shows

    var id: Self { self }
}

enum BrowseTargetID: String, Hashable, Sendable {
    case category
    case mood
    case genre
}

struct BrowseLinkInfo: Hashable, Sendable {
    let target: BrowseTargetID
    let type: ExploreTypeID
    let genre: Int
    let category: String
    let mood: String
}

struct BrowseTileInfo: Identifiable, Hashable, Sendable {
    let id: String
    let stripKey: String
    let name: String
    let isGenre: Bool
    let backdrop: String?
    let section: String
    let hue: Int
    let link: BrowseLinkInfo
}

struct GenreCardInfo: Identifiable, Hashable, Sendable {
    let id: String
    let genre: Int
    let name: String
    let backdrop: String?
    let type: ExploreTypeID
}

struct GenreOption: Identifiable, Hashable, Sendable {
    let id: Int
    let name: String
}

struct ServiceChipInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
}

enum TopSourceID: String, Hashable, Sendable {
    case today
    case asOf = "asof"
    case popular
}

struct TopServicesInfo: Hashable, Sendable {
    let chips: [ServiceChipInfo]
    let selected: Int64
    let ranked: Bool
    let source: TopSourceID
    let serviceName: String
    let fetchedAt: String
    let attribution: String
}

enum HeroPillID: String, Hashable, Sendable {
    case newSeason = "newseason"
    case pickedForYou = "pickedforyou"
    case newThisWeek = "newthisweek"
    case rank
}

struct GenreBannerInfo: Hashable, Sendable {
    let genre: Int
    let name: String
    let type: ExploreTypeID
    let have: Int
    let picks: [DiscoverCardInfo]
}

struct ExploreInfo: Hashable, Sendable {
    let load: LoadInfo
    let type: ExploreTypeID
    let hero: DiscoverCardInfo?
    let heroPills: [HeroPillID]
    let banner: GenreBannerInfo?
    let strip: [BrowseTileInfo]
    let rows: [CardRowInfo]
    let top: TopServicesInfo?
    let genres: [GenreCardInfo]
    let more: Bool
    let loadingMore: Bool
    let sourceOff: Bool
    let canHide: Bool
    let canSeeStats: Bool
    let isEmpty: Bool

    static let initial = ExploreInfo(
        load: .loading, type: .all, hero: nil, heroPills: [], banner: nil, strip: [], rows: [], top: nil, genres: [],
        more: false, loadingMore: false, sourceOff: false, canHide: false, canSeeStats: false, isEmpty: false
    )
}

struct GenrePageInfo: Hashable, Sendable {
    let load: LoadInfo
    let name: String
    let canHide: Bool
    let hero: DiscoverCardInfo?
    let rows: [CardRowInfo]
    let top: TopServicesInfo?
    let isEmpty: Bool

    static let initial = GenrePageInfo(load: .loading, name: "", canHide: false, hero: nil, rows: [], top: nil, isEmpty: false)
}

enum WhatKindID: String, Hashable, Sendable {
    case onService = "onservice"
    case out
    case coming
    case other
}

struct RailEntryInfo: Identifiable, Hashable, Sendable {
    var id: String { card.id }
    let card: DiscoverCardInfo
    let what: WhatKindID
    let text: String
    let alsoComing: Bool
}

struct RailDayInfo: Identifiable, Hashable, Sendable {
    var id: String { day }
    let day: String
    let offset: Int
    let entries: [RailEntryInfo]
}

struct PopularRowInfo: Identifiable, Hashable, Sendable {
    var id: String { row.id }
    let row: CardRowInfo
    let chips: [ServiceChipInfo]
    let selected: Int64
    let loading: Bool
}

struct PopularInfo: Hashable, Sendable {
    let load: LoadInfo
    let type: ExploreTypeID
    let hero: DiscoverCardInfo?
    let heroPills: [HeroPillID]
    let heroRank: Int
    let heroService: String
    let days: [RailDayInfo]
    let rows: [PopularRowInfo]
    let more: Bool
    let loadingMore: Bool
    let sourceOff: Bool
    let canHide: Bool
    let isEmpty: Bool

    static let initial = PopularInfo(
        load: .loading, type: .all, hero: nil, heroPills: [], heroRank: 0, heroService: "", days: [], rows: [],
        more: false, loadingMore: false, sourceOff: false, canHide: false, isEmpty: false
    )
}

enum SearchScopeID: String, Hashable, Sendable, Identifiable {
    case all
    case people
    case movie
    case show

    var id: Self { self }
}

enum SuggestKindID: String, Hashable, Sendable {
    case movie
    case show
    case person
}

enum SuggestStateID: String, Hashable, Sendable {
    case none
    case have
    case missing
    case asked
}

struct SuggestionInfo: Identifiable, Hashable, Sendable {
    var id: String { "\(kind.rawValue)-\(tmdb)-\(personId)-\(name)" }
    let kind: SuggestKindID
    let name: String
    let sub: String
    let art: String?
    let state: SuggestStateID
    let tmdb: String
    let personId: String
}

struct SearchHitInfo: Identifiable, Hashable, Sendable {
    var id: String { card?.id ?? ("p" + (person?.id ?? "")) }
    let card: DiscoverCardInfo?
    let person: PersonCardInfo?
}

struct UnderstoodInfo: Hashable, Sendable {
    let name: String
    let genres: [String]
    let kind: CardKindID?
    let music: Bool
    let yearFrom: Int
    let yearTo: Int
    let people: [String]
    let words: [String]
}

struct SearchResultsInfo: Hashable, Sendable {
    let searched: String
    let library: [DiscoverCardInfo]
    let top: [SearchHitInfo]
    let movies: [DiscoverCardInfo]
    let shows: [DiscoverCardInfo]
    let people: [PersonCardInfo]
    let understood: UnderstoodInfo?
    let plainLibrary: [DiscoverCardInfo]
    let more: [DiscoverCardInfo]
    let metadataDown: Bool
    let error: ErrorInfo?
    let isEmpty: Bool
}

struct SearchInfo: Hashable, Sendable {
    let query: String
    let scope: SearchScopeID
    let scopes: [SearchScopeID]
    let onlyLibrary: Bool
    let recents: [String]
    let completions: [String]
    let suggestions: [SuggestionInfo]
    let searching: Bool
    let results: SearchResultsInfo?
    let canManage: Bool
    let canHide: Bool

    static let initial = SearchInfo(
        query: "", scope: .all, scopes: [], onlyLibrary: false, recents: [], completions: [], suggestions: [], searching: false,
        results: nil, canManage: false, canHide: false
    )
}

struct BrowseSectionInfo: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let tiles: [BrowseTileInfo]
    let loaded: Bool
    let building: Bool
}

struct BrowseInfo: Hashable, Sendable {
    let load: LoadInfo
    let tab: ExploreTypeID
    let tiles: [BrowseTileInfo]
    let sections: [BrowseSectionInfo]

    static let initial = BrowseInfo(load: .loading, tab: .all, tiles: [], sections: [])
}

struct BrowseViewInfo: Hashable, Sendable {
    let load: LoadInfo
    let title: String
    let genreName: String
    let type: ExploreTypeID
    let cards: [DiscoverCardInfo]
    let canHide: Bool
    let isEmpty: Bool

    static let initial = BrowseViewInfo(load: .loading, title: "", genreName: "", type: .all, cards: [], canHide: false, isEmpty: false)
}

enum PeopleFilterID: String, Hashable, Sendable, Identifiable {
    case popular
    case actors
    case directors
    case composers

    var id: Self { self }
}

struct PeopleShelfInfo: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let people: [PersonCardInfo]
}

struct PeopleBrowseInfo: Hashable, Sendable {
    let load: LoadInfo
    let filter: PeopleFilterID
    let filters: [PeopleFilterID]
    let shelves: [PeopleShelfInfo]
    let popular: [PersonCardInfo]
    let more: Bool
    let loadingMore: Bool
    let attribution: String

    static let initial = PeopleBrowseInfo(
        load: .loading, filter: .popular, filters: [.popular], shelves: [], popular: [], more: false, loadingMore: false, attribution: ""
    )
}

enum FilmRoleID: String, Hashable, Sendable, CaseIterable, Identifiable {
    case all
    case actor
    case director

    var id: Self { self }
}

enum FilmSortID: String, Hashable, Sendable, CaseIterable, Identifiable {
    case oldest
    case newest
    case popular

    var id: Self { self }
}

struct DecadeInfo: Identifiable, Hashable, Sendable {
    var id: Int { year }
    let year: Int
    let cards: [DiscoverCardInfo]
}

struct PersonPageInfo: Hashable, Sendable {
    let load: LoadInfo
    let name: String
    let photo: String?
    let roles: [String]
    let firstYear: Int
    let films: Int
    let biography: String
    let birthday: String
    let deathday: String
    let birthplace: String
    let department: String
    let knownFor: [DiscoverCardInfo]
    let comingSoon: [DiscoverCardInfo]
    let inLibrary: [DiscoverCardInfo]
    let asked: [DiscoverCardInfo]
    let decades: [DecadeInfo]
    let oftenWith: [PersonCardInfo]
    let role: FilmRoleID
    let sort: FilmSortID
    let limitUsed: Int?
    let limitLeft: Int?
    let reloading: Bool
    let attribution: String
    let canHide: Bool

    static let initial = PersonPageInfo(
        load: .loading, name: "", photo: nil, roles: [], firstYear: 0, films: 0, biography: "", birthday: "", deathday: "",
        birthplace: "", department: "", knownFor: [], comingSoon: [], inLibrary: [], asked: [], decades: [], oftenWith: [],
        role: .all, sort: .oldest, limitUsed: nil, limitLeft: nil, reloading: false, attribution: "", canHide: false
    )
}

struct SeriesMovieInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let year: Int
    let versions: Int
    let files: Int
    let isProtected: Bool
    let versionIds: [Int64]
    let fileIds: [Int64]
}

struct SeriesReviewRowInfo: Identifiable, Hashable, Sendable {
    var id: Int64 { movie.id }
    let movie: SeriesMovieInfo
    let skip: String
}

enum SeriesOutcomeID: String, Hashable, Sendable {
    case removed
    case skipped
    case failed
}

struct SeriesOutcomeInfo: Identifiable, Hashable, Sendable {
    var id: String { name + outcome.rawValue }
    let name: String
    let outcome: SeriesOutcomeID
    let why: String
}

struct SeriesInfo: Hashable, Sendable {
    let load: LoadInfo
    let name: String
    let movies: [SeriesMovieInfo]
    let selected: Set<Int64>
    let deleteFiles: Bool
    let neverAdd: Bool
    let acknowledge: Bool
    let review: [SeriesReviewRowInfo]?
    let results: [SeriesOutcomeInfo]?
    let busy: Bool
    let actionError: ErrorInfo?
    let canManage: Bool
    let showDetails: Bool
    let needsAcknowledge: Bool
    let canReview: Bool
    let isEmpty: Bool

    static let initial = SeriesInfo(
        load: .loading, name: "", movies: [], selected: [], deleteFiles: false, neverAdd: false, acknowledge: false, review: nil,
        results: nil, busy: false, actionError: nil, canManage: false, showDetails: false, needsAcknowledge: false, canReview: false,
        isEmpty: false
    )
}

enum HideWhatID: String, Hashable, Sendable {
    case movie
    case show
    case movieGenre = "moviegenre"
    case showGenre = "showgenre"
}

enum HideForID: String, Hashable, Sendable {
    case everyone
    case person
    case removedPerson = "removedperson"
}

struct HiddenRowInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let what: HideWhatID
    let who: HideForID
    let person: String
    let isGenre: Bool
}

struct HiddenInfo: Hashable, Sendable {
    let load: LoadInfo
    let rows: [HiddenRowInfo]
    let actionError: ErrorInfo?
    let isEmpty: Bool

    static let initial = HiddenInfo(load: .loading, rows: [], actionError: nil, isEmpty: false)
}

struct HidePersonInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
}

struct HideSheetInfo: Hashable, Sendable {
    let load: LoadInfo
    let genres: [String]
    let hidden: [HiddenRowInfo]
    let people: [HidePersonInfo]
    let genre: String
    let everyone: Bool
    let personId: Int64
    let saving: Bool
    let needsPerson: Bool
    let error: ErrorInfo?

    static let initial = HideSheetInfo(
        load: .loading, genres: [], hidden: [], people: [], genre: "", everyone: true, personId: 0, saving: false, needsPerson: false,
        error: nil
    )
}

struct HideDoneInfo: Hashable, Sendable {
    let id: Int64
    let name: String
    let everyone: Bool
    let person: String
}

struct HideTarget: Identifiable, Hashable, Sendable {
    var id: String { "\(kind.rawValue)-\(tmdb)" }
    let kind: CardKindID
    let tmdb: String
    let name: String
    let genres: [String]
}

struct RowStatInfo: Identifiable, Hashable, Sendable {
    var id: String { rowType + title }
    let rowType: String
    let title: String
    let shown: Int
    let opens: Int
    let asks: Int
}

struct LayerStatInfo: Identifiable, Hashable, Sendable {
    var id: String { layer }
    let layer: String
    let shown: Int
    let opens: Int
    let asks: Int
    let rate: Double
}

struct ExploreStatsInfo: Hashable, Sendable {
    let load: LoadInfo
    let rows: [RowStatInfo]
    let layers: [LayerStatInfo]
    let isEmpty: Bool

    static let initial = ExploreStatsInfo(load: .loading, rows: [], layers: [], isEmpty: false)
}
