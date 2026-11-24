@preconcurrency import shared
import Foundation

final class HomeDriver: @unchecked Sendable {
    private let presenter: HomePresenter

    init(presenter: HomePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (HomeInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.home(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func show(_ board: HomeBoardID) {
        presenter.show(board: Mapping.kotlinBoard(board))
    }

    func dismissWelcome() async {
        do { try await presenter.dismissWelcome() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }
}

final class ExploreDriver: @unchecked Sendable {
    private let presenter: ExplorePresenter

    init(presenter: ExplorePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (ExploreInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.explore(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func select(_ type: ExploreTypeID) async {
        do { try await presenter.select(type: Mapping.kotlinType(type)) } catch {}
    }

    func loadMore() async {
        do { try await presenter.loadMore() } catch {}
    }

    func selectService(_ id: Int64) async {
        do { try await presenter.selectService(id: id) } catch {}
    }

    func stripShown() async {
        do { try await presenter.stripShown() } catch {}
    }

    func stripOpened(_ key: String) async {
        do { try await presenter.stripOpenedKey(key: key) } catch {}
    }

    func opened(_ rowType: String) async {
        do { try await presenter.openedFrom(rowType: rowType) } catch {}
    }

    func asking(_ rowType: String) async {
        do { try await presenter.askingFrom(rowType: rowType) } catch {}
    }

    func reset() async -> Bool {
        do {
            let error = try await presenter.resetRecommendations()
            return error == nil
        } catch {
            return false
        }
    }

    func refreshOwned() async {
        do { try await presenter.refreshOwned() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }
}

final class GenreDriver: @unchecked Sendable {
    private let presenter: GenrePresenter

    init(presenter: GenrePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (GenrePageInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.genrePage(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func selectService(_ id: Int64) async {
        do { try await presenter.selectService(id: id) } catch {}
    }

    func refreshOwned() async {
        do { try await presenter.refreshOwned() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }
}

final class PopularDriver: @unchecked Sendable {
    private let presenter: PopularPresenter

    init(presenter: PopularPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (PopularInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.popular(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func select(_ type: ExploreTypeID) async {
        do { try await presenter.select(type: Mapping.kotlinType(type)) } catch {}
    }

    func loadMore() async {
        do { try await presenter.loadMore() } catch {}
    }

    func selectService(row: String, service: Int64) async {
        do { try await presenter.selectService(rowId: row, service: service) } catch {}
    }

    func refreshOwned() async {
        do { try await presenter.refreshOwned() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }
}

final class SearchDriver: @unchecked Sendable {
    private let search: SearchPresenter
    private let browse: BrowsePresenter
    private let people: PeopleBrowsePresenter

    init(search: SearchPresenter, browse: BrowsePresenter, people: PeopleBrowsePresenter) {
        self.search = search
        self.browse = browse
        self.people = people
    }

    func watch(deliver: @MainActor @Sendable (SearchInfo) -> Void) async {
        for await state in search.state {
            let mapped = Mapping.search(state)
            await deliver(mapped)
        }
    }

    func watchBrowse(deliver: @MainActor @Sendable (BrowseInfo) -> Void) async {
        for await state in browse.state {
            let mapped = Mapping.browse(state)
            await deliver(mapped)
        }
    }

    func watchPeople(deliver: @MainActor @Sendable (PeopleBrowseInfo) -> Void) async {
        for await state in people.state {
            let mapped = Mapping.peopleBrowse(state)
            await deliver(mapped)
        }
    }

    func load() {
        search.load()
    }

    func type(_ text: String) {
        search.type(text: text)
    }

    func selectScope(_ scope: SearchScopeID) {
        search.selectScope(scope: Mapping.kotlinScope(scope))
    }

    func setOnlyLibrary(_ on: Bool) {
        search.setOnlyLibrary(on: on)
    }

    func clearRecents() {
        search.clearRecents()
    }

    func remember(_ text: String) {
        search.remember(text: text)
    }

    func suggest(_ text: String) async {
        do { try await search.suggest(text: text) } catch {}
    }

    func run(_ text: String) async {
        do { try await search.search(text: text) } catch {}
    }

    func refreshOwned() async {
        do { try await search.refreshOwned() } catch {}
    }

    func loadBrowse(_ tab: ExploreTypeID) async {
        do { try await browse.load(tab: Mapping.kotlinType(tab)) } catch {}
    }

    func refreshBrowse() async {
        do { try await browse.refresh() } catch {}
    }

    func loadSection(_ id: String) async {
        do { try await browse.loadSection(id: id) } catch {}
    }

    func followBrowse() async {
        do { try await browse.watch() } catch {}
    }

    func loadPeople() async {
        do { try await people.load() } catch {}
    }

    func refreshPeople() async {
        do { try await people.refresh() } catch {}
    }

    func selectPeople(_ filter: PeopleFilterID) async {
        do { try await people.select(filter: Mapping.kotlinPeopleFilter(filter)) } catch {}
    }

    func morePeople() async {
        do { try await people.loadMore() } catch {}
    }
}

final class BrowseViewDriver: @unchecked Sendable {
    private let presenter: BrowseViewPresenter

    init(presenter: BrowseViewPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (BrowseViewInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.browseView(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func refreshOwned() async {
        do { try await presenter.refreshOwned() } catch {}
    }
}

final class PersonDriver: @unchecked Sendable {
    private let presenter: PersonPresenter

    init(presenter: PersonPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (PersonPageInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.personPage(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func select(role: FilmRoleID, sort: FilmSortID) async {
        do { try await presenter.select(role: Mapping.kotlinRole(role), sort: Mapping.kotlinFilmSort(sort)) } catch {}
    }

    func refreshOwned() async {
        do { try await presenter.refreshOwned() } catch {}
    }
}

final class SeriesDriver: @unchecked Sendable {
    private let presenter: SeriesPresenter

    init(presenter: SeriesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SeriesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.series(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func toggle(_ id: Int64) {
        presenter.toggle(movie: id)
    }

    func setDeleteFiles(_ on: Bool) {
        presenter.setDeleteFiles(on: on)
    }

    func setNeverAdd(_ on: Bool) {
        presenter.setNeverAdd(on: on)
    }

    func setAcknowledge(_ on: Bool) {
        presenter.setAcknowledge(on: on)
    }

    func cancelReview() {
        presenter.cancelReview()
    }

    func review() async {
        do { try await presenter.review() } catch {}
    }

    func apply() async {
        do { try await presenter.apply() } catch {}
    }
}

final class HiddenDriver: @unchecked Sendable {
    private let presenter: HiddenPresenter

    init(presenter: HiddenPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (HiddenInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.hidden(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func showAgain(_ id: Int64) async {
        do { _ = try await presenter.showAgain(id: id) } catch {}
    }
}

final class HideSheetDriver: @unchecked Sendable {
    private let presenter: HideSheetPresenter

    init(presenter: HideSheetPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (HideSheetInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.hideSheet(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func pickGenre(_ genre: String) {
        presenter.pickGenre(genre: genre)
    }

    func pickEveryone(_ everyone: Bool) {
        presenter.pickEveryone(everyone: everyone)
    }

    func pickPerson(_ id: Int64) {
        presenter.pickPerson(id: id)
    }

    func hide() async -> HideDoneInfo? {
        do {
            guard let done = try await presenter.hide() else { return nil }
            return HideDoneInfo(id: done.id, name: done.name, everyone: done.everyone, person: done.person)
        } catch {
            return nil
        }
    }

    func showAgain(_ id: Int64) async {
        do { _ = try await presenter.showAgain(id: id) } catch {}
    }
}

final class ExploreStatsDriver: @unchecked Sendable {
    private let presenter: ExploreStatsPresenter

    init(presenter: ExploreStatsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (ExploreStatsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.exploreStats(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }
}
