import Foundation
import Observation

enum TitleSheet: Identifiable {
    case cast
    case cards(String, [TitleCardInfo])
    case history
    case hide
    case gate
    case releases(ReleasesStore)
    case add(AddTitleStore)
    case episode(EpisodeStore)
    case seasonTools(Int)
    case subtitles(SubtitlesStore)
    case leftovers(LeftoversStore)
    case rename(RenameStore)
    case remove(RemoveStore)
    case version(TitleVersionInfo?, Int64)
    case edition(Int64, String)
    case adopt(Int64)
    case review(ReviewInfo)

    var id: String {
        switch self {
        case .cast: "cast"
        case let .cards(title, _): "cards-\(title)"
        case .history: "history"
        case .hide: "hide"
        case .gate: "gate"
        case .releases: "releases"
        case .add: "add"
        case .episode: "episode"
        case let .seasonTools(season): "tools-\(season)"
        case .subtitles: "subtitles"
        case .leftovers: "leftovers"
        case .rename: "rename"
        case .remove: "remove"
        case let .version(version, file): "version-\(version?.id ?? 0)-\(file)"
        case let .edition(file, _): "edition-\(file)"
        case let .adopt(version): "adopt-\(version)"
        case let .review(review): "review-\(review.id)"
        }
    }
}

enum TitleConfirm: Identifiable {
    case markFailed(Int64)
    case unprotect(Int64)
    case removeVersion(TitleVersionInfo)
    case move(FolderInfo)

    var id: String {
        switch self {
        case let .markFailed(id): "fail-\(id)"
        case let .unprotect(id): "unprotect-\(id)"
        case let .removeVersion(version): "remove-\(version.id)"
        case let .move(folder): "move-\(folder.id)"
        }
    }
}

@MainActor
@Observable
final class TitleStore {
    private(set) var info: TitleInfo
    var sheet: TitleSheet?
    var confirm: TitleConfirm?
    var trailer: TrailerSourceInfo?
    private(set) var trailerLoading = false

    let driver: TitleDriver
    @ObservationIgnored private var seasonsStore: ShowSeasonsStore?
    @ObservationIgnored private var seasonsFor: Int64 = -1
    @ObservationIgnored private(set) lazy var seasonBrowser = SeasonBrowserStore(driver: driver.makeSeasonBrowser())
    @ObservationIgnored private(set) lazy var reviews = ReviewsStore(driver: driver.makeReviews())

    init(kind: String, tmdb: String, libraryId: Int64, openLibrary: Bool) {
        info = .initial(kind: kind, tmdb: tmdb)
        driver = SDKClient.shared.makeTitle(kind: kind, tmdb: tmdb, libraryId: libraryId, openLibrary: openLibrary)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func run(_ action: @escaping @Sendable (TitleDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }

    func seasons() -> ShowSeasonsStore {
        if let store = seasonsStore, seasonsFor == info.ownedId { return store }
        let store = ShowSeasonsStore(driver: driver.makeSeasons())
        seasonsStore = store
        seasonsFor = info.ownedId
        return store
    }

    func selectTab(_ tab: TitleTabKind) { driver.selectTab(tab) }

    func open(_ next: TitleSheet) { sheet = next }

    func dismissed() {
        run { await $0.reloadOwned() }
    }

    func openReleases(_ scope: ReleaseScopeValue, season: Int = 0, episode: Int = 0, itemId: Int64 = 0, slotId: Int64 = 0) {
        sheet = .releases(ReleasesStore(driver: driver.makeReleases(scope: scope, season: season, episode: episode, itemId: itemId, slotId: slotId)))
    }

    func openAdd() { sheet = .add(AddTitleStore(driver: driver.makeAdd())) }

    func openAdd(after releases: ReleasesStore) { sheet = .add(AddTitleStore(driver: driver.makeAdd(after: releases.driver))) }

    func openAddAll() { sheet = .add(AddTitleStore(driver: driver.makeAddAll())) }

    func openEpisode(_ id: Int64) {
        guard let episode = seasons().driver.makeEpisode(id) else { return }
        sheet = .episode(EpisodeStore(driver: episode))
    }

    func subtitles(itemId: Int64) -> SubtitlesStore { SubtitlesStore(driver: driver.makeSubtitles(itemId: itemId)) }

    func openLeftovers() { sheet = .leftovers(LeftoversStore(driver: driver.makeLeftovers())) }

    func openRename(season: Int = -1) { sheet = .rename(RenameStore(driver: driver.makeRename(season: season))) }

    func rename(season: Int) -> RenameStore { RenameStore(driver: driver.makeRename(season: season)) }

    func openRemove(season: Int = -1, episodeIds: [Int64] = []) {
        sheet = .remove(RemoveStore(driver: driver.makeRemove(season: season, episodeIds: episodeIds)))
    }

    func playTrailer() {
        guard !trailerLoading else { return }
        trailerLoading = true
        let driver = self.driver
        Task {
            let source = await driver.trailer()
            trailerLoading = false
            trailer = source
        }
    }

    func mediaRequest(_ path: String) -> ImageRequestInfo? { driver.mediaRequest(path) }
}

@MainActor
@Observable
final class ShowSeasonsStore {
    private(set) var info: ShowSeasonsInfo = .initial
    let driver: ShowSeasonsDriver

    init(driver: ShowSeasonsDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (ShowSeasonsDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class EpisodeStore {
    private(set) var info: EpisodeSheetInfo?
    let driver: EpisodeDriver

    init(driver: EpisodeDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (EpisodeDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class ReleasesStore {
    private(set) var info: ReleasesInfo?
    let driver: ReleasesDriver

    init(driver: ReleasesDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (ReleasesDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class AddTitleStore {
    private(set) var info: AddTitleInfo?
    let driver: AddTitleDriver

    init(driver: AddTitleDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (AddTitleDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class SubtitlesStore {
    private(set) var info: SubtitlesInfo?
    let driver: SubtitlesDriver

    init(driver: SubtitlesDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (SubtitlesDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class LeftoversStore {
    private(set) var info: LeftoversInfo?
    let driver: LeftoversDriver

    init(driver: LeftoversDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (LeftoversDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class RenameStore {
    private(set) var info: RenameInfo?
    let driver: RenameDriver

    init(driver: RenameDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (RenameDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class RemoveStore {
    private(set) var info: RemoveInfo?
    let driver: RemoveDriver

    init(driver: RemoveDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (RemoveDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class SeasonBrowserStore {
    private(set) var info: SeasonBrowserInfo = .initial
    let driver: SeasonBrowserDriver

    init(driver: SeasonBrowserDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (SeasonBrowserDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}

@MainActor
@Observable
final class ReviewsStore {
    private(set) var info: ReviewsInfo = .initial
    let driver: ReviewsDriver

    init(driver: ReviewsDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func run(_ action: @escaping @Sendable (ReviewsDriver) async -> Void) {
        let driver = self.driver
        Task { await action(driver) }
    }
}
