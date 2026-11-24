import Foundation
import Observation

@MainActor
@Observable
final class RequestsStore {
    private(set) var info: RequestsInfo = .initial
    private(set) var pages: [RequestPageKind] = []

    private let driver = SDKClient.shared.makeRequests()

    func watch() async {
        await driver.watch { [weak self] value, pages in
            self?.info = value
            self?.pages = pages
        }
    }

    func load() async { await driver.load() }

    func followLive() async { await driver.followLive() }

    func refresh() async { await driver.refresh() }

    func loadMore() {
        let driver = self.driver
        Task { await driver.loadMore() }
    }

    func setMemberTab(_ tab: MemberTabKind) { driver.setMemberTab(tab) }

    func setApproverTab(_ tab: ApproverTabKind) { driver.setApproverTab(tab) }

    func setKind(_ kind: KindFilterKind) { driver.setKind(kind) }

    func setStage(_ stage: StageKind?) { driver.setStage(stage) }

    func clearFilters() { driver.clearFilters() }

    func approve(_ id: Int64, review: AskReviewStore? = nil) { driver.approve(id, review: review?.driver) }

    func decline(_ id: Int64, reason: String) { driver.decline(id, reason: reason) }

    func approveAll() {
        let driver = self.driver
        Task { await driver.approveAll() }
    }

    func undo(_ keys: [Int64]) { driver.undo(keys) }

    func remove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.remove(id) }
    }

    func clear(_ which: ClearKindInfo) {
        let driver = self.driver
        Task { await driver.clear(which) }
    }

    func takeBack(_ id: Int64) {
        let driver = self.driver
        Task { await driver.takeBack(id) }
    }

    func dismissNote() { driver.dismissNote() }

    func flush() { driver.flush() }
}

@MainActor
@Observable
final class AskReviewStore {
    private(set) var info: AskReviewInfo = .initial

    @ObservationIgnored let driver: AskReviewDriver

    init(askId: Int64) {
        driver = SDKClient.shared.makeAskReview(askId)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func toggleSeason(_ number: Int) { driver.toggleSeason(number) }

    func toggleEpisode(_ number: Int, _ episode: Int) { driver.toggleEpisode(number, episode) }

    func expandSeason(_ number: Int) {
        let driver = self.driver
        Task { await driver.expandSeason(number) }
    }

    func toggleAlbum(_ mbid: String) { driver.toggleAlbum(mbid) }

    func toggleSong(_ mbid: String, _ key: String) { driver.toggleSong(mbid, key) }

    func expandAlbum(_ mbid: String) { driver.expandAlbum(mbid) }

    func select(_ key: String) { driver.select(key) }
}

@MainActor
@Observable
final class AskSheetStore {
    private(set) var info: AskSheetInfo = .initial

    @ObservationIgnored private let driver: AskDriver

    init(kind: String, tmdb: String, fourK: Bool) {
        driver = SDKClient.shared.makeAsk(kind: kind, tmdb: tmdb, fourK: fourK)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func select(_ key: String) { driver.select(key) }

    func toggleAlso(_ key: String) { driver.toggleAlso(key) }

    func setFollow(_ on: Bool) { driver.setFollow(on) }

    func setMode(_ mode: ShowModeKind) { driver.setMode(mode) }

    func toggleSeason(_ number: Int) { driver.toggleSeason(number) }

    func toggleEpisode(_ number: Int, _ episode: Int) { driver.toggleEpisode(number, episode) }

    func expandSeason(_ number: Int) {
        let driver = self.driver
        Task { await driver.expandSeason(number) }
    }

    func submit() {
        let driver = self.driver
        Task { await driver.submit() }
    }
}

@MainActor
@Observable
final class AskMusicStore {
    private(set) var info: AskMusicInfo = .initial

    @ObservationIgnored private let driver: AskMusicDriver

    init(artist: String, name: String, album: String, edition: String) {
        driver = SDKClient.shared.makeAskMusic(artist: artist, name: name, album: album, edition: edition)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func setMode(_ mode: MusicModeKind) { driver.setMode(mode) }

    func setFollow(_ on: Bool) { driver.setFollow(on) }

    func toggle(_ mbid: String) { driver.toggle(mbid) }

    func submit() {
        let driver = self.driver
        Task { await driver.submit() }
    }
}

@MainActor
@Observable
final class ReportsStore {
    private(set) var info: ReportsInfo = .initial

    private let driver = SDKClient.shared.makeReports()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func setFilter(_ filter: ReportFilterKind) { driver.setFilter(filter) }

    func fix(_ id: Int64, reply: String) async -> Bool {
        await driver.fix(id, reply: reply)
    }

    func reopen(_ id: Int64) async -> Bool {
        await driver.reopen(id)
    }

    func betterCopy(_ id: Int64) {
        let driver = self.driver
        Task { await driver.betterCopy(id) }
    }

    func dismissNote() { driver.dismissNote() }
}

@MainActor
@Observable
final class ReportProblemStore {
    private(set) var info: ReportProblemInfo = .initial

    @ObservationIgnored private let driver: ReportProblemDriver

    init(titleId: Int64, season: Int, itemId: Int64, albumId: Int64, versionId: Int64) {
        driver = SDKClient.shared.makeReportProblem(titleId: titleId, season: season, itemId: itemId, albumId: albumId, versionId: versionId)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func setKind(_ kind: ReportKindInfo) { driver.setKind(kind) }

    func setNote(_ note: String) { driver.setNote(note) }

    func send() {
        let driver = self.driver
        Task { await driver.send() }
    }
}

@MainActor
@Observable
final class RequestRulesStore {
    private(set) var info: RequestRulesInfo = .initial
    private(set) var regions: [String] = []

    private let driver = SDKClient.shared.makeRequestRules()

    func watch() async {
        let driver = self.driver
        await driver.watch { [weak self] value in
            self?.info = value
            if self?.regions.isEmpty == true { self?.regions = driver.regions() }
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func setLimit(_ kind: LimitKindInfo, _ text: String) { driver.setLimit(kind, text) }

    func setFolder(_ field: FolderFieldKind, _ id: Int64) { driver.setFolder(field, id) }

    func setPickRules(_ field: FolderFieldKind, _ id: Int64) { driver.setPickRules(field, id) }

    func setFlag(_ flag: SettingsFlagKind, _ on: Bool) { driver.setFlag(flag, on) }

    func setLineName(_ kind: LineKindInfo, _ index: Int, _ name: String) { driver.setLineName(kind, index, name) }

    func setLineLabel(_ kind: LineKindInfo, _ index: Int, _ label: String) { driver.setLineLabel(kind, index, label) }

    func setLineShown(_ kind: LineKindInfo, _ index: Int, _ shown: Bool) { driver.setLineShown(kind, index, shown) }

    func setLineRules(_ kind: LineKindInfo, _ index: Int, _ id: Int64) { driver.setLineRules(kind, index, id) }

    func moveLine(_ kind: LineKindInfo, _ index: Int, up: Bool) { driver.moveLine(kind, index, up: up) }

    func addLine(_ kind: LineKindInfo) { driver.addLine(kind) }

    func removeLine(_ kind: LineKindInfo, _ index: Int) { driver.removeLine(kind, index) }

    func setRegion(_ region: String) {
        let driver = self.driver
        Task { await driver.setRegion(region) }
    }

    func toggleService(_ id: Int64) { driver.toggleService(id) }

    func discard() { driver.discard() }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }

    func startCleanup() {
        let driver = self.driver
        Task { await driver.startCleanup() }
    }
}

@MainActor
@Observable
final class OutsideListsStore {
    private(set) var info: OutsideListsInfo = .initial

    private let driver = SDKClient.shared.makeOutsideLists()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func startNew() { driver.startNew() }

    func edit(_ id: Int64) { driver.edit(id) }

    func cancelEdit() { driver.cancelEdit() }

    func setName(_ name: String) { driver.setName(name) }

    func setKind(_ kind: ListKindInfo) { driver.setKind(kind) }

    func setTitleKind(show: Bool) { driver.setTitleKind(show: show) }

    func setRef(_ ref: String) { driver.setRef(ref) }

    func setFolder(_ id: Int64) { driver.setFolder(id) }

    func setPickRules(_ id: Int64) { driver.setPickRules(id) }

    func setSeasons(_ seasons: ListSeasonsKind) { driver.setSeasons(seasons) }

    func setNewSeasons(_ on: Bool) { driver.setNewSeasons(on) }

    func setEvery(_ text: String) { driver.setEvery(text) }

    func setKeepUpToDate(_ on: Bool) { driver.setKeepUpToDate(on) }

    func setSearchOnAdd(_ on: Bool) { driver.setSearchOnAdd(on) }

    func setEnabled(_ on: Bool) { driver.setEnabled(on) }

    func saveList() {
        let driver = self.driver
        Task { await driver.saveList() }
    }

    func sync(_ id: Int64) {
        let driver = self.driver
        Task { await driver.sync(id) }
    }

    func remove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.remove(id) }
    }

    func saveTrakt(key: String, token: String, secret: String) async -> Bool {
        await driver.saveTrakt(key: key, token: token, secret: secret)
    }

    func signIn() {
        let driver = self.driver
        Task { await driver.signIn() }
    }

    func cancelSignIn() { driver.cancelSignIn() }

    func signOut() {
        let driver = self.driver
        Task { await driver.signOut() }
    }

    func saveMdblist(_ key: String) async -> Bool {
        await driver.saveMdblist(key)
    }

    func dismissNote() { driver.dismissNote() }
}

@MainActor
@Observable
final class NeverAddStore {
    private(set) var info: NeverAddInfo = .initial

    private let driver = SDKClient.shared.makeNeverAdd()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func add(show: Bool, tmdb: String, name: String, reason: String) async -> Bool {
        await driver.add(show: show, tmdb: tmdb, name: name, reason: reason)
    }

    func remove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.remove(id) }
    }
}
