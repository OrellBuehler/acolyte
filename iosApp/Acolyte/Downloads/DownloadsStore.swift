import Foundation
import Observation

@MainActor
@Observable
final class DownloadsStore {
    private(set) var info: DownloadsInfo = .initial
    var route: Route?
    let toasts = DlToastModel()

    private let driver = SDKClient.shared.makeDownloads()
    @ObservationIgnored private var started = false
    @ObservationIgnored private var seenNotice: Int64 = 0

    var listDriver: DownloadsDriver { driver }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.apply(value)
        }
    }

    func start(show: String) async {
        if !started {
            started = true
            if !show.isEmpty { driver.show(show) }
        }
        await driver.load()
    }

    func followLive() async {
        await driver.followLive()
    }

    func refresh() async {
        await driver.refresh()
    }

    private func apply(_ value: DownloadsInfo) {
        info = value
        if let notice = value.notice, notice.id != seenNotice {
            seenNotice = notice.id
            toasts.show(notice.message, tone: notice.tone)
            driver.consumeNotice(notice.id)
        }
        if let error = value.actionError {
            toasts.show(error.detailed, tone: .fail)
            driver.clearActionError()
        }
    }

    func show(_ value: String) {
        driver.show(value)
    }

    func setQuery(_ query: String) {
        driver.setQuery(query)
    }

    func setTab(_ tab: DownloadsTabKind) {
        driver.setTab(tab)
    }

    func setSub(_ sub: DownloadsSubKind) {
        driver.setSub(sub)
    }

    func setKind(_ kind: DlKindFilter) {
        driver.setKind(kind)
    }

    func setNetwork(_ network: DlNetworkFilter) {
        driver.setNetwork(network)
    }

    func setTracker(_ tracker: String) {
        driver.setTracker(tracker)
    }

    func setSort(_ sort: DlSortInfo?) {
        driver.setSort(sort)
    }

    func clearFilters() {
        driver.clearFilters()
    }

    func toggleSelect(_ key: String) {
        driver.toggleSelect(key)
    }

    func selectAll() {
        driver.selectAll()
    }

    func clearSelection() {
        driver.clearSelection()
    }

    func selectedKeys() -> [String] {
        let ordered = info.items.map(\.key).filter { info.selection.contains($0) }
        let rest = info.selection.subtracting(ordered).sorted()
        return ordered + rest
    }

    func showMore() {
        let driver = self.driver
        Task { await driver.showMore() }
    }

    func act(_ key: String, _ action: DownloadActionKind) {
        let driver = self.driver
        Task { await driver.act(key: key, action: action) }
    }

    func setPriority(_ key: String, _ priority: UsenetPriorityKind) {
        let driver = self.driver
        Task { await driver.setPriority(key: key, priority: priority) }
    }

    func bulk(_ action: DownloadActionKind) {
        let driver = self.driver
        Task { await driver.bulk(action) }
    }

    func startAll() {
        let driver = self.driver
        Task { await driver.startAll() }
    }

    func pauseAll() {
        let driver = self.driver
        Task { await driver.pauseAll() }
    }

    func removePlan(keys: [String]) -> RemovePlanInfo? {
        driver.removePlan(keys: keys)
    }

    func remove(keys: [String], deleteFiles: Bool, turnDown: Bool, searchAgain: Bool) async -> [String] {
        await driver.remove(keys: keys, deleteFiles: deleteFiles, turnDown: turnDown, searchAgain: searchAgain)
    }

    func loadClients() async {
        await driver.loadClients()
    }

    func saveSpeedLimits(_ targets: [SpeedTargetInfo]) async {
        await driver.saveSpeedLimits(targets)
    }

    func setSlowMode(id: Int64, on: Bool) {
        let driver = self.driver
        Task { await driver.setSlowMode(id: id, on: on) }
    }

    func addTorrents(downloaderId: Int64, text: String, category: String, savePath: String, paused: Bool, files: [DlTorrentFile]) async -> [String] {
        await driver.addTorrents(downloaderId: downloaderId, text: text, category: category, savePath: savePath, paused: paused, files: files)
    }

    func steerStart(keys: [String]) -> ShareLimitsInfo {
        driver.steerStart(keys: keys)
    }

    func setShareLimits(keys: [String], ratio: Double, seedingMinutes: Int64, inactiveMinutes: Int64, action: String) async -> Bool {
        await driver.setShareLimits(
            keys: keys,
            ratio: ratio,
            seedingMinutes: seedingMinutes,
            inactiveMinutes: inactiveMinutes,
            action: action
        )
    }

    func setLocation(keys: [String], category: String, folder: String) async -> Bool {
        await driver.setLocation(keys: keys, category: category, folder: folder)
    }

    func setTorrentSpeeds(keys: [String], downKb: Double, upKb: Double) async -> Bool {
        await driver.setTorrentSpeeds(keys: keys, downKb: downKb, upKb: upKb)
    }

    func decide(slotId: Int64, action: DecideKind, pack: Bool) {
        let driver = self.driver
        Task { await driver.decide(slotId: slotId, action: action, pack: pack) }
    }

    func importAgain(slotId: Int64) {
        let driver = self.driver
        Task { await driver.importAgain(slotId: slotId) }
    }

    func setEdition(slotId: Int64, edition: String) async -> Bool {
        await driver.setEdition(slotId: slotId, edition: edition)
    }

    func pickMatch(id: Int64, candidate: String?, notMedia: Bool) {
        let driver = self.driver
        Task { await driver.pickMatch(id: id, candidate: candidate, notMedia: notMedia) }
    }

    func runMatches() {
        let driver = self.driver
        Task { await driver.runMatches() }
    }

    func openTitle(_ titleId: Int64, library: Bool = false) {
        let driver = self.driver
        Task {
            guard var link = await driver.titleLink(titleId: titleId) else { return }
            link.openLibrary = library
            route = link.route
        }
    }

    var canImportFiles: Bool {
        SDKClient.shared.libraryTools().contains(.importFiles)
    }

    func makeDetailStore(key: String) -> DownloadDetailStore {
        DownloadDetailStore(key: key, list: driver)
    }
}

@MainActor
@Observable
final class DownloadDetailStore {
    private(set) var info: DownloadDetailInfo = .initial

    private let key: String
    private let list: DownloadsDriver
    @ObservationIgnored private var driver: DownloadDetailDriver?
    @ObservationIgnored private var noticeTask: Task<Void, Never>?
    @ObservationIgnored private var pendingFiles: PendingFiles?

    init(key: String, list: DownloadsDriver) {
        self.key = key
        self.list = list
    }

    private func ensure() -> DownloadDetailDriver? {
        if let driver { return driver }
        let made = SDKClient.shared.makeDownloadDetail(list: list, key: key)
        driver = made
        return made
    }

    func watch() async {
        guard let driver = ensure() else { return }
        await driver.watch { [weak self] value in
            self?.apply(value)
        }
    }

    func poll() async {
        guard let driver = ensure() else { return }
        await driver.poll()
    }

    private func apply(_ value: DownloadDetailInfo) {
        info = value
        if value.notice != nil && noticeTask == nil {
            noticeTask = Task { [weak self] in
                try? await Task.sleep(for: .seconds(4))
                guard !Task.isCancelled else { return }
                self?.finishNotice()
            }
        }
    }

    private func finishNotice() {
        noticeTask = nil
        driver?.consumeNotice()
    }

    func syncRow() {
        driver?.syncRow()
    }

    func setTab(_ tab: DetailTabKind) {
        guard let driver = ensure() else { return }
        driver.setTab(tab)
        Task { await driver.refresh() }
    }

    func refresh() {
        guard let driver = ensure() else { return }
        Task { await driver.refresh() }
    }

    func setFileFilter(_ text: String) {
        driver?.setFileFilter(text)
    }

    func clearActionError() {
        driver?.clearActionError()
    }

    func setFilePriority(indexes: [Int], priority: Int) {
        guard let driver = ensure() else { return }
        pendingFiles = PendingFiles(indexes: indexes, priority: priority)
        Task { await driver.setFilePriority(indexes: indexes, priority: priority) }
    }

    func reauth(password: String, code: String) async -> Bool {
        let trimmed = code.trimmingCharacters(in: .whitespacesAndNewlines)
        let ok = await SDKClient.shared.reauth(password: password, code: trimmed)
        guard ok, let driver = ensure() else { return false }
        driver.clearActionError()
        if let pending = pendingFiles {
            await driver.setFilePriority(indexes: pending.indexes, priority: pending.priority)
        }
        return true
    }

    func addTrackers(_ text: String) async -> [String] {
        guard let driver = ensure() else { return [] }
        return await driver.addTrackers(text)
    }

    func editTracker(ref: String, url: String, tier: Int?) {
        guard let driver = ensure() else { return }
        Task { await driver.editTracker(ref: ref, url: url, tier: tier) }
    }

    func removeTracker(ref: String) {
        guard let driver = ensure() else { return }
        Task { await driver.removeTracker(ref: ref) }
    }
}

private struct PendingFiles: Sendable {
    let indexes: [Int]
    let priority: Int
}

@MainActor
@Observable
final class DownloadBadgesStore {
    private(set) var badge: DownloadsBadgeInfo = .initial

    private let driver = SDKClient.shared.makeDownloadBadges()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.badge = value
        }
    }

    func follow() async {
        await driver.follow()
    }
}
