@preconcurrency import shared
import Foundation

enum LibraryLayoutID: String, Hashable, Sendable {
    case list
    case grid
}

enum LibStateKeyID: String, Hashable, Sendable {
    case failed
    case needsDecision = "needsdecision"
    case stalled
    case missingFile = "missingfile"
    case missing
    case betterWanted = "betterwanted"
    case paused
    case downloading
    case queued
    case approved
    case waitingApproval = "waitingapproval"
    case notOutYet = "notoutyet"
    case notKept = "notkept"
    case specialsOnly = "specialsonly"
    case declined
    case inLibrary = "inlibrary"
    case upToDate = "uptodate"
    case ended
}

enum FactKindID: String, Hashable, Sendable {
    case released
    case homeRelease = "homerelease"
    case inCinemas = "incinemas"
    case cinemas
    case releaseDate = "releasedate"
    case notKnown = "notknown"
    case episodes
    case tracks
    case nextEpisode = "nextepisode"
    case nextRelease = "nextrelease"
    case ended
    case lastAired = "lastaired"
    case added
    case size
    case runtime
    case score
    case year
    case network
    case quality
}

enum LibrarySortID: String, Hashable, Sendable {
    case status
    case name = "title"
    case year
    case score
    case release = "out"
    case added
    case size
    case runtime
    case rules
    case next
    case last
    case progress
    case network
}

enum ActiveFilterID: String, Hashable, Sendable {
    case status
    case kept
    case asker
    case series
    case codec
    case subs
    case watched
    case search
    case grouped
}

enum StatusFilterID: String, Hashable, Sendable {
    case all
    case missing
    case notOutYet = "notoutyet"
    case better
    case have
}

enum KeptFilterID: String, Hashable, Sendable {
    case any
    case on
    case off
}

struct LibStateInfo: Hashable, Sendable {
    let key: LibStateKeyID
    let tone: ToneKind
    let count: Int
    let percent: Int
    let quality: String
    let date: String
    let reasons: [String]
    let noReason: Bool
    let fileNotFound: Bool
    let importing: Bool
}

struct LibraryFactInfo: Hashable, Sendable {
    let kind: FactKindID
    let date: String
    let number: Int64
    let total: Int64
    let score: Double
    let text: String
}

struct LibraryItemInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let kind: LibraryKindID
    let name: String
    let year: Int
    let poster: String?
    let tmdb: String
    let state: LibStateInfo?
    let have: Int
    let total: Int
    let kept: Bool
    let watchedSeen: Int
    let watchedOf: Int
    let fact: LibraryFactInfo?
    let overview: String
    let canOpen: Bool
    let share: Double
}

struct ActiveChipInfo: Hashable, Sendable, Identifiable {
    let filter: ActiveFilterID
    let value: String
    let label: String

    var id: ActiveFilterID { filter }
}

struct LibraryGroupInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let items: [LibraryItemInfo]
}

struct SavedFilterInfo: Hashable, Sendable, Identifiable {
    let name: String
    let active: Bool

    var id: String { name }
}

struct CopiesReviewItemInfo: Hashable, Sendable, Identifiable {
    let id: Int
    let title: String
    let version: String
    let codec: String
}

struct CopiesReviewInfo: Hashable, Sendable {
    let items: [CopiesReviewItemInfo]
    let slotCount: Int
}

struct LibraryInfo: Hashable, Sendable {
    var load: LoadInfo = .loading
    var kind: LibraryKindID = .movie
    var kinds: [LibraryKindID] = []
    var musicEntry = false
    var canManage = false
    var showsPosterState = false
    var tools: [LibraryToolID] = []
    var addKinds: [LibraryKindID] = []
    var canOpenFolders = false
    var layout: LibraryLayoutID = .list
    var items: [LibraryItemInfo] = []
    var groups: [LibraryGroupInfo] = []
    var total = 0
    var query = ""
    var searchFailed = false
    var sort: LibrarySortID = .added
    var descending = true
    var sorts: [LibrarySortID] = []
    var statusFilter: StatusFilterID = .all
    var keptFilter: KeptFilterID = .any
    var statusOptions: [FilterOptionInfo] = []
    var keptOptions: [FilterOptionInfo] = []
    var askerOptions: [FilterOptionInfo] = []
    var codecOptions: [FilterOptionInfo] = []
    var seriesOptions: [FilterOptionInfo] = []
    var subtitlesOption: FilterOptionInfo?
    var watchedOption: FilterOptionInfo?
    var chips: [ActiveChipInfo] = []
    var grouped = false
    var canGroup = false
    var savedFilters: [SavedFilterInfo] = []
    var selecting = false
    var selected: Set<Int64> = []
    var moveOptions: [MenuOptionInfo] = []
    var ruleOptions: [MenuOptionInfo] = []
    var canFindCopies = false
    var canSetAudio = false
    var missingCount = 0
    var canSearchSubtitles = false
    var deletedCount = 0
    var working = false
    var notice: LibraryNoticeInfo?
    var copiesReview: CopiesReviewInfo?
    var isEmpty = false
    var nothingMatches = false
    var selectedCount = 0
    var canSaveFilter = false
    var activeFilterCount = 0

    static let initial = LibraryInfo()
}

extension Mapping {
    private static func gridState(_ state: LibState) -> LibStateInfo {
        LibStateInfo(
            key: LibStateKeyID(rawValue: key(state.key)) ?? .missing,
            tone: tone(state.tone),
            count: Int(state.count),
            percent: Int(state.percent),
            quality: state.quality,
            date: state.date,
            reasons: state.reasons,
            noReason: state.noReason,
            fileNotFound: state.fileNotFound,
            importing: state.importing
        )
    }

    private static func gridFact(_ fact: LibraryFact) -> LibraryFactInfo {
        LibraryFactInfo(
            kind: FactKindID(rawValue: key(fact.kind)) ?? .notKnown,
            date: fact.date,
            number: fact.number,
            total: fact.total,
            score: fact.score,
            text: fact.text
        )
    }

    private static func gridItem(_ item: LibraryItem) -> LibraryItemInfo {
        LibraryItemInfo(
            id: item.id,
            kind: libraryKind(item.kind),
            name: item.name,
            year: Int(item.year),
            poster: item.poster,
            tmdb: item.tmdb,
            state: item.state.map { gridState($0) },
            have: Int(item.have),
            total: Int(item.total),
            kept: item.kept,
            watchedSeen: Int(item.watchedSeen),
            watchedOf: Int(item.watchedOf),
            fact: item.fact.map { gridFact($0) },
            overview: item.overview,
            canOpen: item.canOpen,
            share: Double(item.share)
        )
    }

    static func library(_ s: LibraryState) -> LibraryInfo {
        let items = s.items.map { gridItem($0) }
        let byID = Dictionary(items.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let groups = s.groups.map { group in
            LibraryGroupInfo(id: group.id, name: group.name, items: group.items.compactMap { byID[$0.id] })
        }
        let chips = s.chips.map { chip in
            ActiveChipInfo(filter: ActiveFilterID(rawValue: key(chip.filter)) ?? .search, value: chip.id, label: chip.label)
        }
        let saved = s.savedFilters.map { SavedFilterInfo(name: $0.name, active: $0.active) }
        let review = s.copiesReview.map { copies in
            CopiesReviewInfo(
                items: copies.items.enumerated().map { index, copy in
                    CopiesReviewItemInfo(id: index, title: copy.title, version: copy.version, codec: copy.codec)
                },
                slotCount: Int(copies.slotCount)
            )
        }
        let kinds = s.kinds.map { libraryKind($0) }
        let addKinds = s.addKinds.map { libraryKind($0) }
        let tools = s.tools.compactMap { LibraryToolID(rawValue: key($0)) }
        let sorts = s.sorts.compactMap { LibrarySortID(rawValue: key($0)) }
        let statusOptions = s.statusOptions.map { filterOption($0) }
        let keptOptions = s.keptOptions.map { filterOption($0) }
        let askerOptions = s.askerOptions.map { filterOption($0) }
        let codecOptions = s.codecOptions.map { filterOption($0) }
        let seriesOptions = s.seriesOptions.map { filterOption($0) }
        let moveOptions = s.moveOptions.map { menuOption($0) }
        let ruleOptions = s.ruleOptions.map { menuOption($0) }
        let selected = Set(s.selected.map { $0.int64Value })
        return LibraryInfo(
            load: load(s.status),
            kind: libraryKind(s.kind),
            kinds: kinds,
            musicEntry: s.musicEntry,
            canManage: s.canManage,
            showsPosterState: s.showsPosterState,
            tools: tools,
            addKinds: addKinds,
            canOpenFolders: s.canOpenFolders,
            layout: LibraryLayoutID(rawValue: key(s.layout)) ?? .list,
            items: items,
            groups: groups,
            total: Int(s.total),
            query: s.query,
            searchFailed: s.searchFailed,
            sort: LibrarySortID(rawValue: key(s.sort)) ?? .added,
            descending: s.descending,
            sorts: sorts,
            statusFilter: StatusFilterID(rawValue: key(s.filter.status)) ?? .all,
            keptFilter: KeptFilterID(rawValue: key(s.filter.kept)) ?? .any,
            statusOptions: statusOptions,
            keptOptions: keptOptions,
            askerOptions: askerOptions,
            codecOptions: codecOptions,
            seriesOptions: seriesOptions,
            subtitlesOption: s.subtitlesOption.map { filterOption($0) },
            watchedOption: s.watchedOption.map { filterOption($0) },
            chips: chips,
            grouped: s.grouped,
            canGroup: s.canGroup,
            savedFilters: saved,
            selecting: s.selecting,
            selected: selected,
            moveOptions: moveOptions,
            ruleOptions: ruleOptions,
            canFindCopies: s.canFindCopies,
            canSetAudio: s.canSetAudio,
            missingCount: Int(s.missingCount),
            canSearchSubtitles: s.canSearchSubtitles,
            deletedCount: Int(s.deletedCount),
            working: s.working,
            notice: libraryNotice(s.notice),
            copiesReview: review,
            isEmpty: s.isEmpty,
            nothingMatches: s.nothingMatches,
            selectedCount: Int(s.selectedCount),
            canSaveFilter: s.canSaveFilter,
            activeFilterCount: Int(s.activeFilterCount)
        )
    }
}

final class LibraryDriver: @unchecked Sendable {
    private let presenter: LibraryPresenter

    init(presenter: LibraryPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.library(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func followLive() async {
        do {
            try await presenter.watch()
        } catch {}
    }

    func search(_ text: String) async {
        do {
            try await presenter.search(text: text)
        } catch {}
    }

    func setLayout(_ layout: LibraryLayoutID) async {
        do {
            try await presenter.setLayout(layout: Self.kotlinLayout(layout))
        } catch {}
    }

    func startSelecting() async {
        do {
            try await presenter.startSelecting()
        } catch {}
    }

    func keepSelected(_ on: Bool) async {
        do {
            try await presenter.keepSelected(on: on)
        } catch {}
    }

    func setRules(_ id: Int64) async {
        do {
            try await presenter.setRules(id: id)
        } catch {}
    }

    func setAudio(_ text: String) async {
        do {
            try await presenter.setAudio(text: text)
        } catch {}
    }

    func searchSelected() async {
        do {
            try await presenter.searchSelected()
        } catch {}
    }

    func refreshSelected() async {
        do {
            try await presenter.refreshSelected()
        } catch {}
    }

    func moveSelected(_ folderId: Int64) async {
        do {
            try await presenter.moveSelected(folderId: folderId)
        } catch {}
    }

    func removeSelected() async {
        do {
            try await presenter.removeSelected()
        } catch {}
    }

    func reviewCopies() async {
        do {
            try await presenter.reviewCopies()
        } catch {}
    }

    func startCopies() async {
        do {
            try await presenter.startCopies()
        } catch {}
    }

    func searchAllMissing() async {
        do {
            try await presenter.searchAllMissing()
        } catch {}
    }

    func searchSubtitles() async {
        do {
            try await presenter.searchSubtitles()
        } catch {}
    }

    func setKind(_ kind: LibraryKindID) {
        guard let value = Mapping.kotlinLibraryKind(kind) else { return }
        presenter.setKind(kind: value)
    }

    func setSort(_ sort: LibrarySortID) {
        presenter.setSort(sort: Self.kotlinSort(sort))
    }

    func setDescending(_ descending: Bool) {
        presenter.setDescending(descending: descending)
    }

    func setStatus(_ id: String) {
        presenter.setStatus(id: id)
    }

    func toggleKept(_ id: String) {
        presenter.toggleKept(id: id)
    }

    func setAsker(_ id: String) {
        presenter.setAsker(id: id)
    }

    func setSeries(_ id: Int64) {
        presenter.setSeries(id: id)
    }

    func setCodec(_ id: String) {
        presenter.setCodec(id: id)
    }

    func toggleSubtitles() {
        presenter.toggleSubtitles()
    }

    func toggleWatched() {
        presenter.toggleWatched()
    }

    func setGrouped(_ grouped: Bool) {
        presenter.setGrouped(grouped: grouped)
    }

    func removeChip(_ filter: ActiveFilterID) {
        presenter.removeChip(filter: Self.kotlinFilter(filter))
    }

    func clearFilters() {
        presenter.clearFilters()
    }

    func saveFilter(_ name: String) {
        presenter.saveFilter(name: name)
    }

    func applySavedFilter(_ name: String) {
        presenter.applySavedFilter(name: name)
    }

    func deleteSavedFilter(_ name: String) {
        presenter.deleteSavedFilter(name: name)
    }

    func stopSelecting() {
        presenter.stopSelecting()
    }

    func toggle(_ id: Int64) {
        presenter.toggle(id: id)
    }

    func selectAll() {
        presenter.selectAll()
    }

    func selectNone() {
        presenter.selectNone()
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    func cancelCopies() {
        presenter.cancelCopies()
    }

    private static func kotlinLayout(_ layout: LibraryLayoutID) -> LibraryLayout {
        switch layout {
        case .list: LibraryLayout.list
        case .grid: LibraryLayout.grid
        }
    }

    private static func kotlinSort(_ sort: LibrarySortID) -> LibrarySort {
        switch sort {
        case .status: LibrarySort.status
        case .name: LibrarySort.title
        case .year: LibrarySort.year
        case .score: LibrarySort.score
        case .release: LibrarySort.out
        case .added: LibrarySort.added
        case .size: LibrarySort.size
        case .runtime: LibrarySort.runtime
        case .rules: LibrarySort.rules
        case .next: LibrarySort.next
        case .last: LibrarySort.last
        case .progress: LibrarySort.progress
        case .network: LibrarySort.network
        }
    }

    private static func kotlinFilter(_ filter: ActiveFilterID) -> ActiveFilter {
        switch filter {
        case .status: ActiveFilter.status
        case .kept: ActiveFilter.kept
        case .asker: ActiveFilter.asker
        case .series: ActiveFilter.series
        case .codec: ActiveFilter.codec
        case .subs: ActiveFilter.subs
        case .watched: ActiveFilter.watched
        case .search: ActiveFilter.search
        case .grouped: ActiveFilter.grouped
        }
    }
}
