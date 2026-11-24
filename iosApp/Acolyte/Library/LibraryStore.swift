import Foundation
import Observation

@MainActor
@Observable
final class LibraryStore {
    private(set) var info: LibraryInfo = .initial

    private let kind: LibraryKindID?

    @ObservationIgnored private var cachedDriver: LibraryDriver?
    @ObservationIgnored private var searchTask: Task<Void, Never>?

    init(kind: LibraryKindID?) {
        self.kind = kind
    }

    private var driver: LibraryDriver {
        if let cachedDriver { return cachedDriver }
        let made = SDKClient.shared.makeLibrary(kind: kind)
        cachedDriver = made
        return made
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func followLive() async {
        await driver.followLive()
    }

    func setKind(_ kind: LibraryKindID) {
        driver.setKind(kind)
    }

    func search(_ text: String) {
        searchTask?.cancel()
        let driver = self.driver
        searchTask = Task { await driver.search(text) }
    }

    func setLayout(_ layout: LibraryLayoutID) {
        let driver = self.driver
        Task { await driver.setLayout(layout) }
    }

    func setSort(_ sort: LibrarySortID) {
        driver.setSort(sort)
    }

    func setDescending(_ descending: Bool) {
        driver.setDescending(descending)
    }

    func setStatus(_ id: String) {
        driver.setStatus(id)
    }

    func toggleKept(_ id: String) {
        driver.toggleKept(id)
    }

    func setAsker(_ id: String) {
        driver.setAsker(id)
    }

    func setSeries(_ id: Int64) {
        driver.setSeries(id)
    }

    func setCodec(_ id: String) {
        driver.setCodec(id)
    }

    func toggleSubtitles() {
        driver.toggleSubtitles()
    }

    func toggleWatched() {
        driver.toggleWatched()
    }

    func setGrouped(_ grouped: Bool) {
        driver.setGrouped(grouped)
    }

    func removeChip(_ filter: ActiveFilterID) {
        driver.removeChip(filter)
    }

    func clearFilters() {
        driver.clearFilters()
    }

    func saveFilter(_ name: String) {
        driver.saveFilter(name)
    }

    func applySavedFilter(_ name: String) {
        driver.applySavedFilter(name)
    }

    func deleteSavedFilter(_ name: String) {
        driver.deleteSavedFilter(name)
    }

    func startSelecting() {
        let driver = self.driver
        Task { await driver.startSelecting() }
    }

    func startSelecting(with id: Int64) {
        let driver = self.driver
        Task { await driver.startSelecting() }
        driver.toggle(id)
    }

    func stopSelecting() {
        driver.stopSelecting()
    }

    func toggle(_ id: Int64) {
        driver.toggle(id)
    }

    func selectAll() {
        driver.selectAll()
    }

    func selectNone() {
        driver.selectNone()
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }

    func keepSelected(_ on: Bool) {
        let driver = self.driver
        Task { await driver.keepSelected(on) }
    }

    func setRules(_ id: Int64) {
        let driver = self.driver
        Task { await driver.setRules(id) }
    }

    func setAudio(_ text: String) {
        let driver = self.driver
        Task { await driver.setAudio(text) }
    }

    func searchSelected() {
        let driver = self.driver
        Task { await driver.searchSelected() }
    }

    func refreshSelected() {
        let driver = self.driver
        Task { await driver.refreshSelected() }
    }

    func moveSelected(_ folderId: Int64) {
        let driver = self.driver
        Task { await driver.moveSelected(folderId) }
    }

    func removeSelected() {
        let driver = self.driver
        Task { await driver.removeSelected() }
    }

    func reviewCopies() {
        let driver = self.driver
        Task { await driver.reviewCopies() }
    }

    func cancelCopies() {
        driver.cancelCopies()
    }

    func startCopies() {
        let driver = self.driver
        Task { await driver.startCopies() }
    }

    func searchAllMissing() {
        let driver = self.driver
        Task { await driver.searchAllMissing() }
    }

    func searchSubtitles() {
        let driver = self.driver
        Task { await driver.searchSubtitles() }
    }
}
