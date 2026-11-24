@preconcurrency import shared
import Foundation

final class InboxDriver: @unchecked Sendable {
    private let inbox: RequestsInbox

    init(inbox: RequestsInbox) {
        self.inbox = inbox
    }

    func watch(deliver: @MainActor @Sendable (InboxInfo) -> Void) async {
        for await state in inbox.state {
            let mapped = Mapping.inbox(state)
            await deliver(mapped)
        }
    }

    func poll() async {
        do {
            try await inbox.poll()
        } catch {}
    }

    func refresh() async {
        do {
            try await inbox.refresh()
        } catch {}
    }
}

final class RequestsDriver: @unchecked Sendable {
    private let presenter: RequestsPresenter

    init(presenter: RequestsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (RequestsInfo, [RequestPageKind]) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.requests(state)
            let pages = Mapping.requestPages(state)
            await deliver(mapped, pages)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func loadMore() async {
        do { try await presenter.loadMore() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }

    func setMemberTab(_ tab: MemberTabKind) {
        presenter.setMemberTab(tab: Mapping.kotlinMemberTab(tab))
    }

    func setApproverTab(_ tab: ApproverTabKind) {
        presenter.setApproverTab(tab: Mapping.kotlinApproverTab(tab))
    }

    func setKind(_ kind: KindFilterKind) {
        presenter.setKind(kind: Mapping.kotlinKind(kind))
    }

    func setStage(_ stage: StageKind?) {
        if let stage {
            presenter.setStage(stage: Mapping.kotlinStage(stage))
        } else {
            presenter.clearStage()
        }
    }

    func clearFilters() {
        presenter.clearFilters()
    }

    func approve(_ id: Int64, review: AskReviewDriver?) {
        presenter.approve(id: id, review: review?.presenter)
    }

    func decline(_ id: Int64, reason: String) {
        presenter.decline(id: id, reason: reason)
    }

    func approveAll() async {
        do { try await presenter.approveAll() } catch {}
    }

    func undo(_ keys: [Int64]) {
        presenter.undo(keys: keys.map { KotlinLong(value: $0) })
    }

    func remove(_ id: Int64) async {
        do { try await presenter.remove(id: id) } catch {}
    }

    func clear(_ which: ClearKindInfo) async {
        do { try await presenter.clear(which: Mapping.kotlinClear(which)) } catch {}
    }

    func takeBack(_ id: Int64) async {
        do { _ = try await presenter.takeBack(id: id) } catch {}
    }

    func dismissNote() {
        presenter.dismissNote()
    }

    func flush() {
        presenter.flush()
    }
}

final class AskReviewDriver: @unchecked Sendable {
    let presenter: AskReviewPresenter

    init(presenter: AskReviewPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (AskReviewInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.review(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func toggleSeason(_ number: Int) {
        presenter.toggleSeason(number: Int32(number))
    }

    func toggleEpisode(_ number: Int, _ episode: Int) {
        presenter.toggleEpisode(number: Int32(number), episode: Int32(episode))
    }

    func expandSeason(_ number: Int) async {
        do { try await presenter.expandSeason(number: Int32(number)) } catch {}
    }

    func toggleAlbum(_ mbid: String) {
        presenter.toggleAlbum(mbid: mbid)
    }

    func toggleSong(_ mbid: String, _ key: String) {
        presenter.toggleSong(mbid: mbid, key: key)
    }

    func expandAlbum(_ mbid: String) {
        presenter.expandAlbum(mbid: mbid)
    }

    func select(_ key: String) {
        presenter.select(key: key)
    }
}

final class AskDriver: @unchecked Sendable {
    private let presenter: AskPresenter

    init(presenter: AskPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (AskSheetInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.askSheet(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func select(_ key: String) {
        presenter.select(key: key)
    }

    func toggleAlso(_ key: String) {
        presenter.toggleAlso(key: key)
    }

    func setFollow(_ on: Bool) {
        presenter.setFollow(on: on)
    }

    func setMode(_ mode: ShowModeKind) {
        presenter.setMode(mode: Mapping.kotlinShowMode(mode))
    }

    func toggleSeason(_ number: Int) {
        presenter.toggleSeason(number: Int32(number))
    }

    func toggleEpisode(_ number: Int, _ episode: Int) {
        presenter.toggleEpisode(number: Int32(number), episode: Int32(episode))
    }

    func expandSeason(_ number: Int) async {
        do { try await presenter.expandSeason(number: Int32(number)) } catch {}
    }

    func submit() async {
        do { try await presenter.submit() } catch {}
    }
}

final class AskMusicDriver: @unchecked Sendable {
    private let presenter: AskMusicPresenter

    init(presenter: AskMusicPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (AskMusicInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.askMusic(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func setMode(_ mode: MusicModeKind) {
        presenter.setMode(mode: Mapping.kotlinMusicMode(mode))
    }

    func setFollow(_ on: Bool) {
        presenter.setFollow(on: on)
    }

    func toggle(_ mbid: String) {
        presenter.toggle(mbid: mbid)
    }

    func submit() async {
        do { try await presenter.submit() } catch {}
    }
}

final class ReportsDriver: @unchecked Sendable {
    private let presenter: ReportsPresenter

    init(presenter: ReportsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (ReportsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.reports(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }

    func setFilter(_ filter: ReportFilterKind) {
        presenter.setFilter(filter: Mapping.kotlinReportFilter(filter))
    }

    func fix(_ id: Int64, reply: String) async -> Bool {
        do {
            _ = try await presenter.fix(id: id, reply: reply)
        } catch {}
        return presenter.state.value.report(id: id)?.isOpen == false
    }

    func reopen(_ id: Int64) async -> Bool {
        do {
            _ = try await presenter.reopen(id: id)
        } catch {}
        return presenter.state.value.report(id: id)?.isOpen == true
    }

    func betterCopy(_ id: Int64) async {
        do { try await presenter.betterCopy(id: id) } catch {}
    }

    func dismissNote() {
        presenter.dismissNote()
    }
}

final class ReportProblemDriver: @unchecked Sendable {
    private let presenter: ReportProblemPresenter

    init(presenter: ReportProblemPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (ReportProblemInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.reportProblem(state)
            await deliver(mapped)
        }
    }

    func setKind(_ kind: ReportKindInfo) {
        presenter.setKind(kind: Mapping.kotlinReportKind(kind))
    }

    func setNote(_ note: String) {
        presenter.setNote(note: note)
    }

    func send() async {
        do { try await presenter.send() } catch {}
    }
}

final class RequestRulesDriver: @unchecked Sendable {
    private let presenter: RequestSettingsPresenter

    init(presenter: RequestSettingsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (RequestRulesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.requestRules(state)
            await deliver(mapped)
        }
    }

    func regions() -> [String] {
        Mapping.regions(presenter.state.value)
    }

    func load() async {
        do {
            try await presenter.load()
            try await presenter.followCleanup()
        } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func setLimit(_ kind: LimitKindInfo, _ text: String) {
        presenter.setLimit(kind: Mapping.kotlinLimit(kind), text: text)
    }

    func setFolder(_ field: FolderFieldKind, _ id: Int64) {
        presenter.setFolder(field: Mapping.kotlinFolder(field), id: id)
    }

    func setPickRules(_ field: FolderFieldKind, _ id: Int64) {
        presenter.setPickRules(field: Mapping.kotlinFolder(field), id: id)
    }

    func setFlag(_ flag: SettingsFlagKind, _ on: Bool) {
        presenter.setFlag(flag: Mapping.kotlinFlag(flag), on: on)
    }

    func setLineName(_ kind: LineKindInfo, _ index: Int, _ name: String) {
        presenter.setLineName(kind: Mapping.kotlinLine(kind), index: Int32(index), name: name)
    }

    func setLineLabel(_ kind: LineKindInfo, _ index: Int, _ label: String) {
        presenter.setLineLabel(kind: Mapping.kotlinLine(kind), index: Int32(index), label: label)
    }

    func setLineShown(_ kind: LineKindInfo, _ index: Int, _ shown: Bool) {
        presenter.setLineShown(kind: Mapping.kotlinLine(kind), index: Int32(index), shown: shown)
    }

    func setLineRules(_ kind: LineKindInfo, _ index: Int, _ id: Int64) {
        presenter.setLineRules(kind: Mapping.kotlinLine(kind), index: Int32(index), id: id)
    }

    func moveLine(_ kind: LineKindInfo, _ index: Int, up: Bool) {
        presenter.moveLine(kind: Mapping.kotlinLine(kind), index: Int32(index), up: up)
    }

    func addLine(_ kind: LineKindInfo) {
        presenter.addLine(kind: Mapping.kotlinLine(kind))
    }

    func removeLine(_ kind: LineKindInfo, _ index: Int) {
        presenter.removeLine(kind: Mapping.kotlinLine(kind), index: Int32(index))
    }

    func setRegion(_ region: String) async {
        do { try await presenter.setRegion(region: region) } catch {}
    }

    func toggleService(_ id: Int64) {
        presenter.toggleService(id: id)
    }

    func discard() {
        presenter.discard()
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func startCleanup() async {
        do { try await presenter.startCleanup() } catch {}
    }
}

final class OutsideListsDriver: @unchecked Sendable {
    private let presenter: OutsideListsPresenter

    init(presenter: OutsideListsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (OutsideListsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.outsideLists(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }

    func startNew() { presenter.startNew() }

    func edit(_ id: Int64) { presenter.edit(id: id) }

    func cancelEdit() { presenter.cancelEdit() }

    func setName(_ name: String) { presenter.setName(name: name) }

    func setKind(_ kind: ListKindInfo) { presenter.setKind(kind: Mapping.kotlinListKind(kind)) }

    func setTitleKind(show: Bool) { presenter.setTitleKind(show: show) }

    func setRef(_ ref: String) { presenter.setRef(ref: ref) }

    func setFolder(_ id: Int64) { presenter.setFolder(id: id) }

    func setPickRules(_ id: Int64) { presenter.setPickRules(id: id) }

    func setSeasons(_ seasons: ListSeasonsKind) { presenter.setSeasons(seasons: Mapping.kotlinListSeasons(seasons)) }

    func setNewSeasons(_ on: Bool) { presenter.setNewSeasons(on: on) }

    func setEvery(_ text: String) { presenter.setEvery(text: text) }

    func setKeepUpToDate(_ on: Bool) { presenter.setKeepUpToDate(on: on) }

    func setSearchOnAdd(_ on: Bool) { presenter.setSearchOnAdd(on: on) }

    func setEnabled(_ on: Bool) { presenter.setEnabled(on: on) }

    func saveList() async {
        do { try await presenter.saveList() } catch {}
    }

    func sync(_ id: Int64) async {
        do { try await presenter.sync(id: id) } catch {}
    }

    func remove(_ id: Int64) async {
        do { try await presenter.remove(id: id) } catch {}
    }

    func saveTrakt(key: String, token: String, secret: String) async -> Bool {
        do {
            _ = try await presenter.saveTrakt(key: key, token: token, secret: secret)
        } catch {}
        return presenter.state.value.keyError == nil
    }

    func signIn() async {
        do { try await presenter.signInTrakt() } catch {}
    }

    func cancelSignIn() { presenter.cancelSignIn() }

    func signOut() async {
        do { try await presenter.signOutTrakt() } catch {}
    }

    func saveMdblist(_ key: String) async -> Bool {
        do {
            _ = try await presenter.saveMdblist(key: key)
        } catch {}
        return presenter.state.value.keyError == nil
    }

    func dismissNote() { presenter.dismissNote() }
}

final class NeverAddDriver: @unchecked Sendable {
    private let presenter: NeverAddPresenter

    init(presenter: NeverAddPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (NeverAddInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.neverAdd(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func add(show: Bool, tmdb: String, name: String, reason: String) async -> Bool {
        do {
            _ = try await presenter.add(show: show, tmdb: tmdb, name: name, reason: reason)
        } catch {}
        let state = presenter.state.value
        return state.error == nil && !state.missing
    }

    func remove(_ id: Int64) async {
        do { try await presenter.remove(id: id) } catch {}
    }
}
