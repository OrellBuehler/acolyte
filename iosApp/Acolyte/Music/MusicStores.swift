import Foundation
import Observation

@MainActor
@Observable
final class MusicArtistStore {
    private(set) var info: MusicArtistInfo = .initial

    @ObservationIgnored let driver: MusicArtistDriver

    init(mbid: String) {
        driver = SDKClient.shared.makeMusicArtist(mbid: mbid)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func reloadQuiet() async { await driver.reloadQuiet() }

    func toggleFollow() {
        let driver = self.driver
        Task { await driver.toggleFollow() }
    }

    func toggleAlbumFollow(_ mbid: String) {
        let driver = self.driver
        Task { await driver.toggleAlbumFollow(mbid) }
    }

    func askAll() {
        let driver = self.driver
        Task { await driver.askAll() }
    }

    func stillReading() { driver.stillReading() }
}

@MainActor
@Observable
final class MusicAlbumStore {
    private(set) var info: MusicAlbumPageInfo = .initial

    @ObservationIgnored let driver: MusicAlbumDriver

    init(mbid: String, artist: String) {
        driver = SDKClient.shared.makeMusicAlbum(mbid: mbid, artist: artist)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func reloadQuiet() async { await driver.reloadQuiet() }

    func toggleFollow() {
        let driver = self.driver
        Task { await driver.toggleFollow() }
    }

    func askWithEdition() {
        let driver = self.driver
        Task { await driver.askWithEdition() }
    }

    func pickEdition(_ id: String) { driver.pickEdition(id) }

    func stillReading() { driver.stillReading() }
}

@MainActor
@Observable
final class MusicRowsStore {
    private(set) var info: MusicRowsInfo = .initial

    @ObservationIgnored let driver: MusicRowsDriver

    init(kind: String, slug: String, region: String) {
        driver = SDKClient.shared.makeMusicRows(kind: kind, slug: slug, region: region)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func loadMore() {
        let driver = self.driver
        Task { await driver.loadMore() }
    }

    func setRegion(_ region: String) {
        let driver = self.driver
        Task { await driver.setRegion(region) }
    }
}

@MainActor
@Observable
final class MusicNewStore {
    private(set) var info: MusicNewInfo = .initial

    @ObservationIgnored let driver: MusicNewDriver

    init(region: String) {
        driver = SDKClient.shared.makeMusicNew(region: region)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func toggleFollow(_ key: String) {
        let driver = self.driver
        Task { await driver.toggleFollow(key) }
    }
}

@MainActor
@Observable
final class MusicBrowseStore {
    private(set) var info: MusicBrowseInfo = .initial

    @ObservationIgnored let driver: MusicBrowseDriver

    init(region: String) {
        driver = SDKClient.shared.makeMusicBrowse(region: region)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }
}

@MainActor
@Observable
final class MusicSearchStore {
    private(set) var info: MusicSearchInfo = .initial

    @ObservationIgnored let driver = SDKClient.shared.makeMusicSearch()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func search(_ text: String) async { await driver.search(text) }

    func clear() { driver.clear() }
}

@MainActor
@Observable
final class MusicAddStore {
    private(set) var info: MusicAddInfo = .initial

    @ObservationIgnored let driver: MusicAddDriver

    init(mbid: String, name: String) {
        driver = SDKClient.shared.makeMusicAdd(mbid: mbid, name: name)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func submit() async { await driver.submit() }

    func pickFolder(_ id: Int64) { driver.pickFolder(id) }

    func pickRules(_ id: Int64) { driver.pickRules(id) }

    func setAlbumChoice(_ id: String) { driver.setAlbumChoice(id) }

    func setNewAlbums(_ id: String) { driver.setNewAlbums(id) }

    func toggleAlbumType(_ id: String, on: Bool) { driver.toggleAlbumType(id, on: on) }

    func toggleSecondary(_ id: String, on: Bool) { driver.toggleSecondary(id, on: on) }

    func toggleStatus(_ id: String, on: Bool) { driver.toggleStatus(id, on: on) }

    func setKeep(_ on: Bool) { driver.setKeep(on) }

    func setFollow(_ on: Bool) { driver.setFollow(on) }
}

@MainActor
@Observable
final class MusicSettingsStore {
    private(set) var info: MusicSettingsInfo = .initial

    @ObservationIgnored let driver = SDKClient.shared.makeMusicSettings()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func save() async { await driver.save() }

    func discard() { driver.discard() }

    func setAlbumChoice(_ id: String) { driver.setAlbumChoice(id) }

    func setNewAlbums(_ id: String) { driver.setNewAlbums(id) }

    func setPressingRule(_ id: String) { driver.setPressingRule(id) }

    func toggleAlbumType(_ id: String, on: Bool) { driver.toggleAlbumType(id, on: on) }

    func toggleSecondary(_ id: String, on: Bool) { driver.toggleSecondary(id, on: on) }

    func toggleStatus(_ id: String, on: Bool) { driver.toggleStatus(id, on: on) }

    func setUrl(_ text: String) { driver.setUrl(text) }

    func setHours(_ text: String) { driver.setHours(text) }
}

@MainActor
@Observable
final class MusicUnmatchedStore {
    private(set) var info: MusicUnmatchedInfo = .initial

    @ObservationIgnored let driver = SDKClient.shared.makeMusicUnmatched()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }
}

@MainActor
@Observable
final class MusicLibraryArtistStore {
    private(set) var info: MusicLibraryArtistInfo = .initial

    @ObservationIgnored let driver: MusicLibraryArtistDriver
    let titleId: Int64

    init(titleId: Int64) {
        self.titleId = titleId
        driver = SDKClient.shared.makeMusicLibraryArtist(titleId: titleId)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func reloadQuiet() async { await driver.reloadQuiet() }

    func followLive() async { await driver.followLive() }

    func setFilter(_ filter: MusicLibFilterKind) { driver.setFilter(filter) }

    func collapseAll() { driver.collapseAll() }

    func expand(_ albumId: Int64) {
        let driver = self.driver
        Task { await driver.expand(albumId) }
    }

    func expandAll() {
        let driver = self.driver
        Task { await driver.expandAll() }
    }

    func toggleKeepAlbum(_ albumId: Int64) {
        let driver = self.driver
        Task { await driver.toggleKeepAlbum(albumId) }
    }

    func setKeepArtist(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setKeepArtist(on) }
    }

    func setRules(_ id: Int64) {
        let driver = self.driver
        Task { await driver.setRules(id) }
    }

    func searchAll() {
        let driver = self.driver
        Task { await driver.searchAll() }
    }

    func searchAlbum(_ albumId: Int64) {
        let driver = self.driver
        Task { await driver.searchAlbum(albumId) }
    }

    func searchTrack(albumId: Int64, slotId: Int64, name: String) {
        let driver = self.driver
        Task { await driver.searchTrack(albumId: albumId, slotId: slotId, name: name) }
    }

    func applyPreset(_ preset: MusicKindsPresetKind) {
        let driver = self.driver
        Task { await driver.applyPreset(preset) }
    }

    func saveKinds(newAlbums: String, types: [String], secondary: [String], statuses: [String]) async -> Bool {
        await driver.saveKinds(newAlbums: newAlbums, types: types, secondary: secondary, statuses: statuses)
    }

    func moveTo(_ folderId: Int64) {
        let driver = self.driver
        Task { await driver.moveTo(folderId) }
    }

    func refreshDetails() {
        let driver = self.driver
        Task { await driver.refreshDetails() }
    }

    func removed() { driver.removed() }

    func makeLeftovers() -> MusicLeftoversStore { MusicLeftoversStore(titleId: titleId) }

    func makeRename() -> RenameStore { RenameStore(driver: SDKClient.shared.makeMusicRename(titleId: titleId)) }

    func makeRemove() -> RemoveStore {
        RemoveStore(driver: SDKClient.shared.makeMusicRemove(titleId: titleId, name: info.name, albumId: 0))
    }
}

@MainActor
@Observable
final class MusicLibraryAlbumStore {
    private(set) var info: MusicLibraryAlbumInfo = .initial

    @ObservationIgnored let driver: MusicLibraryAlbumDriver
    let titleId: Int64
    let albumId: Int64

    init(titleId: Int64, albumId: Int64) {
        self.titleId = titleId
        self.albumId = albumId
        driver = SDKClient.shared.makeMusicLibraryAlbum(titleId: titleId, albumId: albumId)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func toggleKeep() {
        let driver = self.driver
        Task { await driver.toggleKeep() }
    }

    func changePressing(_ id: Int64) {
        let driver = self.driver
        Task { await driver.changePressing(id) }
    }

    func searchNow() {
        let driver = self.driver
        Task { await driver.searchNow() }
    }

    func removed() { driver.removed() }

    func makeRename() -> RenameStore {
        RenameStore(driver: SDKClient.shared.makeMusicRename(titleId: info.titleId > 0 ? info.titleId : titleId))
    }

    func makeRemove() -> RemoveStore {
        RemoveStore(driver: SDKClient.shared.makeMusicRemove(titleId: info.titleId > 0 ? info.titleId : titleId, name: info.name, albumId: albumId))
    }
}

@MainActor
@Observable
final class MusicLeftoversStore {
    private(set) var info: MusicLeftoversInfo = .initial

    @ObservationIgnored let driver: MusicLeftoversDriver

    init(titleId: Int64) {
        driver = SDKClient.shared.makeMusicLeftovers(titleId: titleId)
    }

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func dismiss(_ fileId: Int64, dismissed: Bool) {
        let driver = self.driver
        Task { await driver.dismiss(fileId, dismissed: dismissed) }
    }

    func openMatch(_ fileId: Int64) {
        let driver = self.driver
        Task { await driver.openMatch(fileId) }
    }

    func closeMatch() { driver.closeMatch() }

    func pickAlbum(_ albumId: Int64) {
        let driver = self.driver
        Task { await driver.pickAlbum(albumId) }
    }

    func pickTrack(_ id: Int64) { driver.pickTrack(id) }

    func confirmMatch() {
        let driver = self.driver
        Task { await driver.confirmMatch() }
    }

    func undoMatch() {
        let driver = self.driver
        Task { await driver.undoMatch() }
    }

    func usePressing(_ hint: MusicPressingHintInfo) {
        let driver = self.driver
        Task { await driver.usePressing(albumId: hint.albumId, pressingId: hint.pressingId) }
    }
}
