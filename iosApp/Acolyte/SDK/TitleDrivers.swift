@preconcurrency import shared
import Foundation

private func truth(_ value: Any) -> Bool {
    if let bool = value as? Bool { return bool }
    return (value as? KotlinBoolean)?.boolValue ?? false
}

private func today() -> String {
    let formatter = DateFormatter()
    formatter.calendar = Calendar(identifier: .gregorian)
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.string(from: Date())
}

final class TitleDriver: @unchecked Sendable {
    private let presenter: TitlePresenter
    private let images: Images

    init(presenter: TitlePresenter, images: Images) {
        self.presenter = presenter
        self.images = images
    }

    func watch(deliver: @MainActor @Sendable (TitleInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.title(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func refresh() async { try? await presenter.refresh() }

    func reloadOwned() async { try? await presenter.reloadOwned() }

    func retryOwned() async { try? await presenter.retryOwned() }

    func followLive() async { try? await presenter.watch() }

    func selectTab(_ tab: TitleTabKind) { presenter.selectTab(tab: Mapping.kotlinTab(tab)) }

    func loadChoices() async { try? await presenter.loadChoices(force: false) }

    func searchNow() async { try? await presenter.searchNow() }

    func searchMissing() async { try? await presenter.searchMissing() }

    func searchSeason(_ season: Int) async { try? await presenter.searchSeason(season: Int32(season)) }

    func searchSlot(_ slotId: Int64, code: String) async { try? await presenter.searchSlot(slotId: slotId, code: code) }

    func toggleKeep() async { try? await presenter.toggleKeep() }

    func setRules(_ id: Int64) async { try? await presenter.setRules(rulesId: id) }

    func setPackWindow(_ window: Int) async { try? await presenter.setPackWindow(window: Int32(window)) }

    func setGate(_ gate: String, days: Int) async { try? await presenter.setGate(gate: gate, delayDays: Int32(days)) }

    func setOrder(_ order: String) async { try? await presenter.setOrder(order: order) }

    func setEpisodeGroup(_ id: String) async { try? await presenter.setEpisodeGroup(groupId: id) }

    func setSeasons(_ pick: SeasonPickKind) async { try? await presenter.setSeasons(pick: Mapping.kotlinSeasonPick(pick)) }

    func refreshDetails() async { try? await presenter.refreshDetails() }

    func toggleFollow() async { try? await presenter.toggleFollow() }

    func moveTo(_ folderId: Int64) async { try? await presenter.moveTo(folderId: folderId) }

    func markFailed(_ downloadId: Int64) async { try? await presenter.markFailed(downloadId: downloadId) }

    func loadPoints(_ slotId: Int64) async { try? await presenter.loadPoints(slotId: slotId) }

    func addVersion(label: String, rulesId: Int64, folderId: Int64, keep: Bool, edition: String, audio: String, folderName: String) async -> Bool {
        guard let ok = try? await presenter.addVersion(
            label: label, rulesId: rulesId, folderId: folderId, keepUpToDate: keep, edition: edition, audioLanguages: audio, folderName: folderName
        ) else { return false }
        return truth(ok)
    }

    func keepAndAdd(fileId: Int64, label: String, rulesId: Int64, folderId: Int64, keep: Bool, edition: String, audio: String, folderName: String) async -> Bool {
        guard let ok = try? await presenter.keepAndAdd(
            fileId: fileId, label: label, rulesId: rulesId, folderId: folderId, keepUpToDate: keep, edition: edition, audioLanguages: audio,
            folderName: folderName
        ) else { return false }
        return truth(ok)
    }

    func changeVersion(_ id: Int64, label: String, rulesId: Int64, keep: Bool, audio: String) async -> Bool {
        guard let ok = try? await presenter.changeVersion(versionId: id, label: label, rulesId: rulesId, keepUpToDate: keep, audioLanguages: audio) else {
            return false
        }
        return truth(ok)
    }

    func removeVersion(_ id: Int64) async { try? await presenter.removeVersion(versionId: id) }

    func setEdition(_ fileId: Int64, edition: String) async -> Bool {
        guard let ok = try? await presenter.setEdition(fileId: fileId, edition: edition) else { return false }
        return truth(ok)
    }

    func protect(_ fileId: Int64, on: Bool) async { _ = try? await presenter.protect(fileId: fileId, on: on) }

    func adopt(_ versionId: Int64, path: String) async -> Bool {
        guard let ok = try? await presenter.adopt(versionId: versionId, relativePath: path) else { return false }
        return truth(ok)
    }

    func restoreHeld() async { try? await presenter.restoreHeld() }

    func loadUsers() async { try? await presenter.loadUsers() }

    func hide(genre: String, userId: Int64, who: String) async -> Bool {
        guard let ok = try? await presenter.hide(genre: genre, userId: userId, whoName: who) else { return false }
        return truth(ok)
    }

    func unhide(_ id: Int64) async { try? await presenter.unhide(hideId: id) }

    func trailer() async -> TrailerSourceInfo? {
        guard let source = try? await presenter.trailerStream() else { return nil }
        return Mapping.trailer(source)
    }

    func mediaRequest(_ path: String) -> ImageRequestInfo? {
        guard let text = images.url(source: path, size: ImageSize.poster), let url = URL(string: text) else { return nil }
        return ImageRequestInfo(url: url, headers: images.headers(url: text))
    }

    func makeSeasons() -> ShowSeasonsDriver {
        ShowSeasonsDriver(presenter: presenter.seasons(), title: presenter)
    }

    func makeReleases(scope: ReleaseScopeValue, season: Int, episode: Int, itemId: Int64, slotId: Int64) -> ReleasesDriver {
        let value = ReleaseScope(kind: Mapping.kotlinScope(scope), season: Int32(season), episode: Int32(episode), itemId: itemId, slotId: slotId)
        return ReleasesDriver(presenter: presenter.releases(scope: value))
    }

    func makeAdd() -> AddTitleDriver {
        AddTitleDriver(presenter: presenter.add(choose: false, checkId: 0, release: nil))
    }

    func makeAdd(after releases: ReleasesDriver) -> AddTitleDriver {
        let state = releases.current
        return AddTitleDriver(presenter: presenter.add(choose: true, checkId: state.checkId, release: state.picked))
    }

    func makeAddAll() -> AddTitleDriver {
        AddTitleDriver(presenter: presenter.addAll())
    }

    func makeSubtitles(itemId: Int64) -> SubtitlesDriver {
        SubtitlesDriver(presenter: presenter.subtitles(itemId: itemId))
    }

    func makeLeftovers() -> LeftoversDriver {
        LeftoversDriver(presenter: presenter.leftovers())
    }

    func makeRename(season: Int) -> RenameDriver {
        RenameDriver(presenter: presenter.rename(season: Int32(season)))
    }

    func makeRemove(season: Int, episodeIds: [Int64]) -> RemoveDriver {
        RemoveDriver(presenter: presenter.remove(season: Int32(season), episodeIds: episodeIds.map { KotlinLong(value: $0) }))
    }

    func makeSeasonBrowser() -> SeasonBrowserDriver {
        let year = Calendar.current.component(.year, from: Date())
        return SeasonBrowserDriver(presenter: presenter.seasonBrowser(thisYear: Int32(year)))
    }

    func makeReviews() -> ReviewsDriver {
        ReviewsDriver(presenter: presenter.reviews())
    }
}

final class ShowSeasonsDriver: @unchecked Sendable {
    private let presenter: ShowSeasonsPresenter
    private let title: TitlePresenter

    init(presenter: ShowSeasonsPresenter, title: TitlePresenter) {
        self.presenter = presenter
        self.title = title
    }

    func watch(deliver: @MainActor @Sendable (ShowSeasonsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.seasons(state, today: today())
            await deliver(mapped)
        }
    }

    func toggle(_ season: Int) async { try? await presenter.toggle(season: Int32(season)) }

    func expandAll(_ seasons: [Int]) async { try? await presenter.expandAll(seasons: seasons.map { KotlinInt(value: Int32($0)) }) }

    func collapseAll() { presenter.collapseAll() }

    func toggleBelow() { presenter.toggleBelow() }

    func ensure(_ season: Int) async { try? await presenter.ensure(season: Int32(season)) }

    func load(_ season: Int) async { try? await presenter.load(season: Int32(season)) }

    func reset(grouped: Bool) async { try? await presenter.reset(grouped: grouped) }

    func loadOutside() async { try? await presenter.loadOutside() }

    func keepSeason(_ season: Int, keep: Bool) async { try? await presenter.keepSeason(season: Int32(season), keep: keep) }

    func keepEpisode(_ id: Int64, keep: Bool) async {
        guard let episode = presenter.state.value.find(episodeId: id) else { return }
        try? await presenter.keepEpisode(episode: episode, keep: keep)
    }

    func packNext(_ season: Int) async { try? await presenter.packNext(season: Int32(season)) }

    func loadHistory() async { try? await presenter.loadHistory() }

    func makeEpisode(_ id: Int64) -> EpisodeDriver? {
        guard let episode = presenter.state.value.find(episodeId: id) else { return nil }
        return EpisodeDriver(presenter: title.episode(episode: episode))
    }
}

final class EpisodeDriver: @unchecked Sendable {
    private let presenter: EpisodePresenter

    init(presenter: EpisodePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (EpisodeSheetInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.episodeSheet(state, today: today())
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func lookFor() async { try? await presenter.lookFor() }

    func setKeep(_ keep: Bool) async { try? await presenter.setKeep(keep: keep) }
}

final class ReleasesDriver: @unchecked Sendable {
    private let presenter: ReleasesPresenter

    init(presenter: ReleasesPresenter) {
        self.presenter = presenter
    }

    var current: ReleasesState { presenter.state.value }

    func watch(deliver: @MainActor @Sendable (ReleasesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.releases(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func poll() async { try? await presenter.watch() }

    func search() async { try? await presenter.search() }

    func pick(_ id: Int64) async {
        guard let release = presenter.state.value.find(releaseId: id) else { return }
        try? await presenter.pick(release: release)
    }

    func turnDown(_ id: Int64, blockGroup: Bool) async {
        guard let release = presenter.state.value.find(releaseId: id) else { return }
        try? await presenter.turnDown(release: release, blockGroup: blockGroup)
    }

    func setAllTab(_ all: Bool) { presenter.setAllTab(all: all) }

    func setSort(_ sort: ReleaseSortKind) { presenter.setSort(sort: Mapping.kotlinSort(sort)) }

    func setResolution(_ resolution: Int) { presenter.setResolution(resolution: Int32(resolution)) }

    func setSource(_ source: String) { presenter.setSource(source: source) }

    func setLanguage(_ language: String) { presenter.setLanguage(language: language) }

    func setQuery(_ query: String) { presenter.setQuery(query: query) }

    func clearFilters() { presenter.clearFilters() }

    func showMore() async { try? await presenter.showMore() }

    func setRules(_ id: Int64) { presenter.setRules(rulesId: id) }
}

final class AddTitleDriver: @unchecked Sendable {
    private let presenter: AddTitlePresenter

    init(presenter: AddTitlePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (AddTitleInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.add(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func submit() async { try? await presenter.submit() }

    func setFolder(_ id: Int64) { presenter.setFolder(id: id) }

    func setRules(_ id: Int64) { presenter.setRules(id: id) }

    func setGate(_ gate: String) { presenter.setGate(gate: gate) }

    func setKeep(_ on: Bool) { presenter.setKeep(on: on) }

    func setSearch(_ on: Bool) { presenter.setSearch(on: on) }

    func setFollow(_ on: Bool) { presenter.setFollow(on: on) }

    func setSecond(_ on: Bool) { presenter.setSecond(on: on) }

    func setSecondLabel(_ label: String) { presenter.setSecondLabel(label: label) }

    func setSecondRules(_ id: Int64) { presenter.setSecondRules(id: id) }

    func setSecondFolder(_ id: Int64) { presenter.setSecondFolder(id: id) }

    func setSeasons(_ choice: String) { presenter.setSeasons(choice: choice) }

    func setNewSeasons(_ choice: String) { presenter.setNewSeasons(choice: choice) }

    func setOrder(_ order: String) { presenter.setOrder(order: order) }

    func setSource(_ source: String) { presenter.setSource(source: source) }
}

final class SubtitlesDriver: @unchecked Sendable {
    private let presenter: SubtitlesPresenter

    init(presenter: SubtitlesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SubtitlesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.subtitles(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func loadExtras() async { try? await presenter.loadExtras() }

    func followLive() async { try? await presenter.watch() }

    func useDefault() async { try? await presenter.useDefault() }

    func turnOff() async { try? await presenter.turnOff() }

    func startEdit() { presenter.startEdit() }

    func cancelEdit() { presenter.cancelEdit() }

    func setEditOwn(_ own: Bool) { presenter.setEditOwn(own: own) }

    func addWant() { presenter.addWant() }

    func removeWant(_ index: Int) { presenter.removeWant(index: Int32(index)) }

    func updateWant(_ index: Int, _ want: SubtitleWantInfo) {
        let value = SubtitleWant(language: want.language, hearingImpaired: want.hearingImpaired, forced: want.forced, translateFrom: want.translateFrom)
        presenter.updateWant(index: Int32(index), want: value)
    }

    func saveEdit() async { try? await presenter.saveEdit() }

    func search() async { try? await presenter.search() }

    func openChoices(_ fileId: Int64) async { try? await presenter.openChoices(fileId: fileId) }

    func closeChoices() { presenter.closeChoices() }

    func choose(_ fileId: Int64, site: String, key: String) async { try? await presenter.choose(fileId: fileId, site: site, key: key) }

    func translate(_ fileId: Int64, wantId: Int64, fromId: Int64) async {
        let state = presenter.state.value
        guard let want = state.row(id: wantId), let from = state.row(id: fromId) else { return }
        try? await presenter.translate(fileId: fileId, want: want, from: from)
    }

    func sync(_ id: Int64, offset: Int, from: Double?, to: Double?) async {
        _ = try? await presenter.sync(
            subtitleId: id,
            offsetMs: Int32(offset),
            fromFps: from.map { KotlinDouble(value: $0) },
            toFps: to.map { KotlinDouble(value: $0) }
        )
    }

    func clean(_ id: Int64) async { try? await presenter.clean(subtitleId: id) }

    func cleanAll() async { try? await presenter.cleanAll() }

    func turnDown(_ id: Int64, reason: String) async { _ = try? await presenter.turnDown(subtitleId: id, reason: reason) }
}

final class LeftoversDriver: @unchecked Sendable {
    private let presenter: LeftoversPresenter

    init(presenter: LeftoversPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LeftoversInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.leftovers(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func rescan() async { try? await presenter.rescan() }

    func dismiss(_ fileId: Int64, dismissed: Bool) async {
        guard let leftover = presenter.state.value.find(fileId: fileId) else { return }
        try? await presenter.dismiss(leftover: leftover, dismissed: dismissed)
    }

    func openMatch(_ fileId: Int64) {
        guard let leftover = presenter.state.value.find(fileId: fileId) else { return }
        presenter.openMatch(leftover: leftover)
    }

    func matchMovie(_ fileId: Int64) async {
        guard let leftover = presenter.state.value.find(fileId: fileId) else { return }
        try? await presenter.matchMovie(leftover: leftover)
    }

    func closeMatch() { presenter.closeMatch() }

    func matchSeason(_ season: Int) { presenter.matchSeason(season: Int32(season)) }

    func togglePick(_ id: Int64) { presenter.togglePick(episodeId: id) }

    func confirmMatch() async { try? await presenter.confirmMatch() }

    func matchShift() async { try? await presenter.matchShift() }

    func undoMatch() async { try? await presenter.undoMatch() }
}

final class RenameDriver: @unchecked Sendable {
    private let presenter: RenamePresenter

    init(presenter: RenamePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (RenameInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.rename(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func apply() async { try? await presenter.apply() }
}

final class RemoveDriver: @unchecked Sendable {
    private let presenter: RemovePresenter

    init(presenter: RemovePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (RemoveInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.remove(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func setDeleteFiles(_ on: Bool) async { try? await presenter.setDeleteFiles(on: on) }

    func setNeverAdd(_ on: Bool) async { try? await presenter.setNeverAdd(on: on) }

    func apply() async { try? await presenter.apply() }

    func undo() async { try? await presenter.undo() }
}

final class SeasonBrowserDriver: @unchecked Sendable {
    private let presenter: SeasonBrowserPresenter

    init(presenter: SeasonBrowserPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SeasonBrowserInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.seasonBrowser(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func select(_ season: Int) async { try? await presenter.select(season: Int32(season)) }
}

final class ReviewsDriver: @unchecked Sendable {
    private let presenter: ReviewsPresenter

    init(presenter: ReviewsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (ReviewsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.reviews(state)
            await deliver(mapped)
        }
    }

    func load() async { try? await presenter.load() }

    func setSource(_ source: String) async { try? await presenter.setSource(source: source) }

    func setOrder(_ order: String) async { try? await presenter.setOrder(order: order) }

    func more() async { try? await presenter.more() }
}
