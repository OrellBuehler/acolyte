import Foundation
import Observation

@MainActor
@Observable
final class HomeStore {
    private(set) var info: HomeInfo = .initial
    private let driver = SDKClient.shared.makeHome()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func show(_ board: HomeBoardID) { driver.show(board) }

    func dismissWelcome() {
        let driver = self.driver
        Task { await driver.dismissWelcome() }
    }
}

@MainActor
@Observable
final class ExploreStore {
    private(set) var info: ExploreInfo = .initial
    private let driver = SDKClient.shared.makeExplore()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func refreshOwned() async { await driver.refreshOwned() }

    func select(_ type: ExploreTypeID) {
        let driver = self.driver
        Task { await driver.select(type) }
    }

    func loadMore() {
        let driver = self.driver
        Task { await driver.loadMore() }
    }

    func selectService(_ id: Int64) {
        let driver = self.driver
        Task { await driver.selectService(id) }
    }

    func stripShown() {
        let driver = self.driver
        Task { await driver.stripShown() }
    }

    func stripOpened(_ key: String) {
        let driver = self.driver
        Task { await driver.stripOpened(key) }
    }

    func opened(_ rowType: String) {
        let driver = self.driver
        Task { await driver.opened(rowType) }
    }

    func asking(_ rowType: String) {
        let driver = self.driver
        Task { await driver.asking(rowType) }
    }

    func reset(then: @escaping @MainActor (Bool) -> Void) {
        let driver = self.driver
        Task { then(await driver.reset()) }
    }
}

@MainActor
@Observable
final class GenreStore {
    private(set) var info: GenrePageInfo = .initial
    private let driver: GenreDriver

    init(type: ExploreTypeID, genre: Int) {
        driver = SDKClient.shared.makeGenre(type: type, genre: genre)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func refreshOwned() async { await driver.refreshOwned() }

    func selectService(_ id: Int64) {
        let driver = self.driver
        Task { await driver.selectService(id) }
    }
}

@MainActor
@Observable
final class PopularStore {
    private(set) var info: PopularInfo = .initial
    private let driver = SDKClient.shared.makePopular()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func refreshOwned() async { await driver.refreshOwned() }

    func select(_ type: ExploreTypeID) {
        let driver = self.driver
        Task { await driver.select(type) }
    }

    func loadMore() {
        let driver = self.driver
        Task { await driver.loadMore() }
    }

    func selectService(row: String, service: Int64) {
        let driver = self.driver
        Task { await driver.selectService(row: row, service: service) }
    }
}

@MainActor
@Observable
final class SearchStore {
    private(set) var info: SearchInfo = .initial
    private(set) var browse: BrowseInfo = .initial
    private(set) var people: PeopleBrowseInfo = .initial
    var text = ""
    private let driver = SDKClient.shared.makeSearch()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func watchBrowse() async {
        await driver.watchBrowse { [weak self] value in self?.browse = value }
    }

    func watchPeople() async {
        await driver.watchPeople { [weak self] value in self?.people = value }
    }

    func load() async {
        driver.load()
        await loadBrowse()
    }

    func followBrowse() async { await driver.followBrowse() }

    func refreshOwned() async { await driver.refreshOwned() }

    func type(_ value: String) {
        driver.type(value)
    }

    func suggest(_ value: String) async { await driver.suggest(value) }

    func run(_ value: String) async { await driver.run(value) }

    func submit(_ value: String) {
        text = value
        self.driver.type(value)
        self.driver.remember(value)
        let driver = self.driver
        Task { await driver.run(value) }
    }

    func selectScope(_ scope: SearchScopeID) {
        self.driver.selectScope(scope)
        let driver = self.driver
        Task {
            switch scope {
            case .people: await driver.loadPeople()
            case .movie: await driver.loadBrowse(.movies)
            case .show: await driver.loadBrowse(.shows)
            case .all: await driver.loadBrowse(.all)
            }
        }
    }

    func setOnlyLibrary(_ on: Bool) { driver.setOnlyLibrary(on) }

    func clearRecents() { driver.clearRecents() }

    func loadSection(_ id: String) {
        let driver = self.driver
        Task { await driver.loadSection(id) }
    }

    func refreshBrowse() async {
        if info.scope == .people {
            await driver.refreshPeople()
        } else {
            await driver.refreshBrowse()
        }
    }

    func selectPeople(_ filter: PeopleFilterID) {
        let driver = self.driver
        Task { await driver.selectPeople(filter) }
    }

    func morePeople() {
        let driver = self.driver
        Task { await driver.morePeople() }
    }

    private func loadBrowse() async {
        switch info.scope {
        case .people: await driver.loadPeople()
        case .movie: await driver.loadBrowse(.movies)
        case .show: await driver.loadBrowse(.shows)
        case .all: await driver.loadBrowse(.all)
        }
    }
}

@MainActor
@Observable
final class BrowseViewStore {
    private(set) var info: BrowseViewInfo = .initial
    private let driver: BrowseViewDriver

    init(link: BrowseLinkInfo) {
        driver = SDKClient.shared.makeBrowseView(link)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func refreshOwned() async { await driver.refreshOwned() }
}

@MainActor
@Observable
final class PersonStore {
    private(set) var info: PersonPageInfo = .initial
    private let driver: PersonDriver

    init(id: String) {
        driver = SDKClient.shared.makePerson(id)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func refreshOwned() async { await driver.refreshOwned() }

    func select(role: FilmRoleID, sort: FilmSortID) {
        let driver = self.driver
        Task { await driver.select(role: role, sort: sort) }
    }
}

@MainActor
@Observable
final class SeriesStore {
    private(set) var info: SeriesInfo = .initial
    private let driver: SeriesDriver

    init(id: String) {
        driver = SDKClient.shared.makeSeries(id)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func reload() {
        let driver = self.driver
        Task { await driver.refresh() }
    }

    func toggle(_ id: Int64) { driver.toggle(id) }

    func setDeleteFiles(_ on: Bool) { driver.setDeleteFiles(on) }

    func setNeverAdd(_ on: Bool) { driver.setNeverAdd(on) }

    func setAcknowledge(_ on: Bool) { driver.setAcknowledge(on) }

    func cancelReview() { driver.cancelReview() }

    func review() {
        let driver = self.driver
        Task { await driver.review() }
    }

    func apply() {
        let driver = self.driver
        Task { await driver.apply() }
    }
}

@MainActor
@Observable
final class HiddenStore {
    private(set) var info: HiddenInfo = .initial
    private let driver = SDKClient.shared.makeHidden()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func showAgain(_ id: Int64) {
        let driver = self.driver
        Task { await driver.showAgain(id) }
    }
}

@MainActor
@Observable
final class HideSheetStore {
    private(set) var info: HideSheetInfo = .initial
    private let driver: HideSheetDriver

    init(target: HideTarget) {
        driver = SDKClient.shared.makeHideSheet(target)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func pickGenre(_ genre: String) { driver.pickGenre(genre) }

    func pickEveryone(_ everyone: Bool) { driver.pickEveryone(everyone) }

    func pickPerson(_ id: Int64) { driver.pickPerson(id) }

    func hide(then: @escaping @MainActor (HideDoneInfo) -> Void) {
        let driver = self.driver
        Task {
            if let done = await driver.hide() { then(done) }
        }
    }

    func showAgain(_ id: Int64) {
        let driver = self.driver
        Task { await driver.showAgain(id) }
    }
}

@MainActor
@Observable
final class ExploreStatsStore {
    private(set) var info: ExploreStatsInfo = .initial
    private let driver = SDKClient.shared.makeExploreStats()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }
}
