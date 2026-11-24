@preconcurrency import shared
import Foundation

private final class LatestBox<Value>: @unchecked Sendable {
    private let lock = NSLock()
    private var stored: Value?

    func set(_ value: Value?) {
        lock.lock()
        stored = value
        lock.unlock()
    }

    func get() -> Value? {
        lock.lock()
        defer { lock.unlock() }
        return stored
    }
}

final class DownloadsDriver: @unchecked Sendable {
    private let presenter: DownloadsPresenter
    private let latest = LatestBox<DownloadsState>()
    private let plan = LatestBox<RemovePlan>()

    init(presenter: DownloadsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (DownloadsInfo) -> Void) async {
        for await state in presenter.state {
            latest.set(state)
            let actions = presenter.selectionActions()
            let targets = presenter.speedTargets()
            let mapped = Mapping.downloads(state, actions: actions, targets: targets)
            await deliver(mapped)
        }
    }

    func row(for key: String) -> DownloadRow? {
        latest.get()?.item(key: key)?.row
    }

    private func decision(_ slotId: Int64) -> Decision? {
        guard let state = latest.get() else { return nil }
        if let found = state.decisions.first(where: { $0.slotId == slotId }) { return found }
        for item in state.items {
            if let found = item.decisions.first(where: { $0.slotId == slotId }) { return found }
        }
        return nil
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

    func showMore() async {
        do {
            try await presenter.showMore()
        } catch {}
    }

    func followLive() async {
        do {
            try await presenter.watch()
        } catch {}
    }

    func loadClients() async {
        do {
            try await presenter.loadClients()
        } catch {}
    }

    func show(_ value: String) {
        presenter.show(value: value)
    }

    func setTab(_ tab: DownloadsTabKind) {
        presenter.setTab(tab: Mapping.kotlinTab(tab))
    }

    func setSub(_ sub: DownloadsSubKind) {
        presenter.setSub(sub: Mapping.kotlinSub(sub))
    }

    func setQuery(_ query: String) {
        presenter.setQuery(query: query)
    }

    func setKind(_ kind: DlKindFilter) {
        presenter.setKind(kind: Mapping.kotlinKind(kind))
    }

    func setNetwork(_ network: DlNetworkFilter) {
        presenter.setNetwork(network: Mapping.kotlinNetwork(network))
    }

    func setTracker(_ tracker: String) {
        presenter.setTracker(key: tracker)
    }

    func setSort(_ sort: DlSortInfo?) {
        if let sort {
            presenter.setSortName(key: sort.key.rawValue, descending: sort.descending)
        } else {
            presenter.setSort(sort: nil)
        }
    }

    func clearFilters() {
        presenter.clearFilters()
    }

    func toggleSelect(_ key: String) {
        presenter.toggleSelect(key: key)
    }

    func selectAll() {
        presenter.selectAll()
    }

    func clearSelection() {
        presenter.clearSelection()
    }

    func consumeNotice(_ id: Int64) {
        presenter.consumeNotice(id: id)
    }

    func clearActionError() {
        presenter.clearActionError()
    }

    func act(key: String, action: DownloadActionKind) async {
        do {
            try await presenter.act(key: key, action: Mapping.kotlinAction(action))
        } catch {}
    }

    func setPriority(key: String, priority: UsenetPriorityKind) async {
        do {
            try await presenter.setPriority(key: key, priority: Mapping.kotlinPriority(priority))
        } catch {}
    }

    func bulk(_ action: DownloadActionKind) async {
        do {
            try await presenter.bulk(action: Mapping.kotlinAction(action))
        } catch {}
    }

    func startAll() async {
        do {
            try await presenter.startAll()
        } catch {}
    }

    func pauseAll() async {
        do {
            try await presenter.pauseAll()
        } catch {}
    }

    func removePlan(keys: [String]) -> RemovePlanInfo? {
        guard let found = presenter.removePlan(keys: keys) else { return nil }
        plan.set(found)
        return Mapping.removePlan(found)
    }

    func remove(keys: [String], deleteFiles: Bool, turnDown: Bool, searchAgain: Bool) async -> [String] {
        var chosen: RemovePlan?
        if let held = plan.get(), Set(held.keys) == Set(keys) {
            chosen = held
        } else {
            chosen = presenter.removePlan(keys: keys)
        }
        guard let target = chosen else { return [] }
        do {
            let left = try await presenter.remove(plan: target, deleteFiles: deleteFiles, turnDown: turnDown, searchAgain: searchAgain)
            return Mapping.strings(left)
        } catch {
            return keys
        }
    }

    func saveSpeedLimits(_ targets: [SpeedTargetInfo]) async {
        let converted = targets.map { Mapping.kotlinSpeedTarget($0) }
        do {
            try await presenter.saveSpeedLimits(targets: converted)
        } catch {}
    }

    func setSlowMode(id: Int64, on: Bool) async {
        do {
            try await presenter.setSlowMode(clientId: id, on: on)
        } catch {}
    }

    func addTorrents(downloaderId: Int64, text: String, category: String, savePath: String, paused: Bool, files: [DlTorrentFile]) async -> [String] {
        let uploads = files.map { TorrentUpload(name: $0.name, bytes: Mapping.byteArray($0.data)) }
        do {
            let left = try await presenter.addTorrentFiles(
                downloaderId: downloaderId,
                text: text,
                files: uploads,
                category: category,
                savePath: savePath,
                paused: paused
            )
            return Mapping.strings(left)
        } catch {
            return [""]
        }
    }

    func steerStart(keys: [String]) -> ShareLimitsInfo {
        Mapping.shareLimits(presenter.steerStart(keys: keys))
    }

    func setShareLimits(keys: [String], ratio: Double, seedingMinutes: Int64, inactiveMinutes: Int64, action: String) async -> Bool {
        do {
            let done = try await presenter.setShareLimits(
                keys: keys,
                ratio: ratio,
                seedingMinutes: seedingMinutes,
                inactiveMinutes: inactiveMinutes,
                action: action
            )
            return Mapping.truthy(done)
        } catch {
            return false
        }
    }

    func setLocation(keys: [String], category: String, folder: String) async -> Bool {
        do {
            let done = try await presenter.setLocation(keys: keys, category: category, folder: folder)
            return Mapping.truthy(done)
        } catch {
            return false
        }
    }

    func setTorrentSpeeds(keys: [String], downKb: Double, upKb: Double) async -> Bool {
        do {
            let done = try await presenter.setTorrentSpeeds(keys: keys, downKb: downKb, upKb: upKb)
            return Mapping.truthy(done)
        } catch {
            return false
        }
    }

    func decide(slotId: Int64, action: DecideKind, pack: Bool) async {
        guard let found = decision(slotId) else { return }
        do {
            try await presenter.decide(decision: found, action: Mapping.kotlinDecide(action), pack: pack)
        } catch {}
    }

    func importAgain(slotId: Int64) async {
        guard let found = decision(slotId) else { return }
        do {
            try await presenter.importAgain(decision: found)
        } catch {}
    }

    func setEdition(slotId: Int64, edition: String) async -> Bool {
        guard let found = decision(slotId) else { return false }
        do {
            let done = try await presenter.setEdition(decision: found, edition: edition)
            return Mapping.truthy(done)
        } catch {
            return false
        }
    }

    func pickMatch(id: Int64, candidate: String?, notMedia: Bool) async {
        do {
            try await presenter.pickMatch(matchId: id, candidate: candidate, notMedia: notMedia)
        } catch {}
    }

    func runMatches() async {
        do {
            try await presenter.runMatches()
        } catch {}
    }

    func titleLink(titleId: Int64) async -> DlTitleLink? {
        do {
            guard let link = try await presenter.titleLink(titleId: titleId) else { return nil }
            return DlTitleLink(kind: link.kind, tmdb: link.tmdb, libraryId: link.libraryId, openLibrary: link.openLibrary)
        } catch {
            return nil
        }
    }
}

final class DownloadDetailDriver: @unchecked Sendable {
    private let presenter: DownloadDetailPresenter
    private let key: String
    private let list: DownloadsDriver

    init(presenter: DownloadDetailPresenter, key: String, list: DownloadsDriver) {
        self.presenter = presenter
        self.key = key
        self.list = list
    }

    func watch(deliver: @MainActor @Sendable (DownloadDetailInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.downloadDetail(state)
            await deliver(mapped)
        }
    }

    func poll() async {
        do {
            try await presenter.watch()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func syncRow() {
        if let row = list.row(for: key) {
            presenter.update(row: row)
        }
    }

    func setTab(_ tab: DetailTabKind) {
        presenter.setTab(tab: Mapping.kotlinDetailTab(tab))
    }

    func setFileFilter(_ text: String) {
        presenter.setFileFilter(text: text)
    }

    func consumeNotice() {
        presenter.consumeNotice()
    }

    func clearActionError() {
        presenter.clearActionError()
    }

    func setFilePriority(indexes: [Int], priority: Int) async {
        let converted = indexes.map { KotlinInt(int: Int32($0)) }
        do {
            try await presenter.setFilePriority(indexes: converted, priority: Int32(priority))
        } catch {}
    }

    func addTrackers(_ text: String) async -> [String] {
        do {
            let left = try await presenter.addTrackers(text: text)
            return Mapping.strings(left)
        } catch {
            return [""]
        }
    }

    func editTracker(ref: String, url: String, tier: Int?) async {
        let converted: KotlinInt? = tier.map { KotlinInt(int: Int32($0)) }
        do {
            try await presenter.editTracker(ref: ref, url: url, tier: converted)
        } catch {}
    }

    func removeTracker(ref: String) async {
        do {
            try await presenter.removeTracker(ref: ref)
        } catch {}
    }
}

final class HistoryDriver: @unchecked Sendable {
    private let presenter: HistoryPresenter

    init(presenter: HistoryPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (HistoryInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.history(state)
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

    func setQuery(_ query: String) {
        presenter.setQuery(query: query)
    }

    func setFilter(_ filter: HistoryFilterInfo) async {
        let steps = filter.steps.map { $0.rawValue }.sorted()
        do {
            try await presenter.setFilterNames(
                steps: steps,
                type: filter.type.rawValue,
                who: filter.who.rawValue,
                period: filter.period.rawValue,
                from: filter.from,
                to: filter.to,
                source: filter.source
            )
        } catch {}
    }

    func clearScope() async {
        do {
            try await presenter.clearScope()
        } catch {}
    }

    func showMore() async {
        do {
            try await presenter.showMore()
        } catch {}
    }

    func findAnother(key: String) async {
        do {
            try await presenter.findAnother(key: key)
        } catch {}
    }

    func consumeNotice() {
        presenter.consumeNotice()
    }

    func titleLink(titleId: Int64) async -> DlTitleLink? {
        do {
            guard let link = try await presenter.titleLink(titleId: titleId) else { return nil }
            return DlTitleLink(kind: link.kind, tmdb: link.tmdb, libraryId: link.libraryId, openLibrary: link.openLibrary)
        } catch {
            return nil
        }
    }
}

final class CalendarDriver: @unchecked Sendable {
    private let presenter: CalendarPresenter
    private let latest = LatestBox<CalendarState>()

    init(presenter: CalendarPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (CalendarInfo) -> Void) async {
        for await state in presenter.state {
            latest.set(state)
            let mapped = Mapping.calendar(state)
            await deliver(mapped)
        }
    }

    private func group(_ key: String) -> CalendarGroup? {
        latest.get()?.group(key: key)
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

    func setView(_ view: CalendarViewKind) {
        presenter.setView(view: Mapping.kotlinCalendarView(view))
    }

    func setType(_ type: CalendarTypeKind) {
        presenter.setType(type: Mapping.kotlinCalendarType(type))
    }

    func setFollows(_ follows: CalendarFollowsKind) {
        presenter.setFollows(follows: Mapping.kotlinCalendarFollows(follows))
    }

    func select(_ day: DlDay) {
        presenter.select(day: Mapping.kotlinDay(day))
    }

    func next() async {
        do {
            try await presenter.next()
        } catch {}
    }

    func previous() async {
        do {
            try await presenter.previous()
        } catch {}
    }

    func goToday() async {
        do {
            try await presenter.goToday()
        } catch {}
    }

    func consumeNotice(_ id: Int64) {
        presenter.consumeNotice(id: id)
    }

    func clearActionError() {
        presenter.clearActionError()
    }

    func follow(groupKey: String, on: Bool) async {
        guard let found = group(groupKey) else { return }
        do {
            try await presenter.follow(group: found, on: on)
        } catch {}
    }

    func search(groupKey: String) async {
        guard let found = group(groupKey) else { return }
        do {
            try await presenter.search(group: found)
        } catch {}
    }

    func searchMissing() async {
        do {
            try await presenter.searchMissing()
        } catch {}
    }

    func describe(groupKey: String) async -> [String: String] {
        guard let found = group(groupKey) else { return [:] }
        do {
            let texts = try await presenter.describe(group: found)
            return texts
        } catch {
            return [:]
        }
    }

    func link(groupKey: String) -> DlTitleLink? {
        guard let found = group(groupKey), let link = presenter.link(group: found) else { return nil }
        return DlTitleLink(kind: link.kind, tmdb: link.tmdb)
    }
}

final class DownloadBadgesDriver: @unchecked Sendable {
    private let badges: DownloadBadges

    init(badges: DownloadBadges) {
        self.badges = badges
    }

    func watch(deliver: @MainActor @Sendable (DownloadsBadgeInfo) -> Void) async {
        for await state in badges.state {
            let mapped = Mapping.downloadBadge(state)
            await deliver(mapped)
        }
    }

    func follow() async {
        do {
            try await badges.watch()
        } catch {}
    }
}
