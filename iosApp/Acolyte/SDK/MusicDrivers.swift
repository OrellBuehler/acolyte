@preconcurrency import shared
import Foundation

final class MusicArtistDriver: @unchecked Sendable {
    private let presenter: MusicArtistPresenter

    init(presenter: MusicArtistPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicArtistInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicArtist(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func refresh() async { do { try await presenter.refresh() } catch {} }

    func reloadQuiet() async { do { try await presenter.reloadQuiet() } catch {} }

    func toggleFollow() async { do { try await presenter.toggleFollow() } catch {} }

    func toggleAlbumFollow(_ mbid: String) async { do { try await presenter.toggleAlbumFollow(albumMbid: mbid) } catch {} }

    func askAll() async { do { try await presenter.askAll() } catch {} }

    func stillReading() { presenter.stillReading() }
}

final class MusicAlbumDriver: @unchecked Sendable {
    private let presenter: MusicAlbumPresenter

    init(presenter: MusicAlbumPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicAlbumPageInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicAlbumPage(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func refresh() async { do { try await presenter.refresh() } catch {} }

    func reloadQuiet() async { do { try await presenter.reloadQuiet() } catch {} }

    func toggleFollow() async { do { try await presenter.toggleFollow() } catch {} }

    func askWithEdition() async { do { try await presenter.askWithEdition() } catch {} }

    func pickEdition(_ id: String) { presenter.pickEdition(id: id) }

    func stillReading() { presenter.stillReading() }
}

final class MusicRowsDriver: @unchecked Sendable {
    private let presenter: MusicRowsPresenter

    init(presenter: MusicRowsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicRowsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicRows(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func refresh() async { do { try await presenter.refresh() } catch {} }

    func loadMore() async { do { try await presenter.loadMore() } catch {} }

    func setRegion(_ region: String) async { do { try await presenter.setRegion(region: region) } catch {} }
}

final class MusicNewDriver: @unchecked Sendable {
    private let presenter: MusicNewPresenter

    init(presenter: MusicNewPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicNewInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicNew(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func refresh() async { do { try await presenter.refresh() } catch {} }

    func toggleFollow(_ key: String) async { do { try await presenter.toggleFollow(key: key) } catch {} }
}

final class MusicBrowseDriver: @unchecked Sendable {
    private let presenter: MusicBrowsePresenter

    init(presenter: MusicBrowsePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicBrowseInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicBrowse(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func refresh() async { do { try await presenter.refresh() } catch {} }
}

final class MusicSearchDriver: @unchecked Sendable {
    private let presenter: MusicSearchPresenter

    init(presenter: MusicSearchPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicSearchInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicSearch(state)
            await deliver(mapped)
        }
    }

    func search(_ text: String) async { do { try await presenter.search(text: text) } catch {} }

    func clear() { presenter.clear() }
}

final class MusicAddDriver: @unchecked Sendable {
    private let presenter: MusicAddPresenter

    init(presenter: MusicAddPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicAddInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicAdd(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func submit() async { do { try await presenter.submit() } catch {} }

    func pickFolder(_ id: Int64) { presenter.pickFolder(id: id) }

    func pickRules(_ id: Int64) { presenter.pickRules(id: id) }

    func setAlbumChoice(_ id: String) { presenter.setAlbumChoice(id: id) }

    func setNewAlbums(_ id: String) { presenter.setNewAlbums(id: id) }

    func toggleAlbumType(_ id: String, on: Bool) { presenter.toggleAlbumType(id: id, on: on) }

    func toggleSecondary(_ id: String, on: Bool) { presenter.toggleSecondary(id: id, on: on) }

    func toggleStatus(_ id: String, on: Bool) { presenter.toggleStatus(id: id, on: on) }

    func setKeep(_ on: Bool) { presenter.setKeep(on: on) }

    func setFollow(_ on: Bool) { presenter.setFollow(on: on) }
}

final class MusicSettingsDriver: @unchecked Sendable {
    private let presenter: MusicSettingsPresenter

    init(presenter: MusicSettingsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicSettingsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicSettings(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func save() async { do { try await presenter.save() } catch {} }

    func discard() { presenter.discard() }

    func setAlbumChoice(_ id: String) { presenter.setAlbumChoice(id: id) }

    func setNewAlbums(_ id: String) { presenter.setNewAlbums(id: id) }

    func setPressingRule(_ id: String) { presenter.setPressingRule(id: id) }

    func toggleAlbumType(_ id: String, on: Bool) { presenter.toggleAlbumType(id: id, on: on) }

    func toggleSecondary(_ id: String, on: Bool) { presenter.toggleSecondary(id: id, on: on) }

    func toggleStatus(_ id: String, on: Bool) { presenter.toggleStatus(id: id, on: on) }

    func setUrl(_ text: String) { presenter.setUrl(text: text) }

    func setHours(_ text: String) { presenter.setHours(text: text) }
}

final class MusicUnmatchedDriver: @unchecked Sendable {
    private let presenter: MusicUnmatchedPresenter

    init(presenter: MusicUnmatchedPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicUnmatchedInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicUnmatched(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }
}

final class MusicLibraryArtistDriver: @unchecked Sendable {
    private let presenter: MusicLibraryArtistPresenter

    init(presenter: MusicLibraryArtistPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicLibraryArtistInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicLibraryArtist(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func refresh() async { do { try await presenter.refresh() } catch {} }

    func reloadQuiet() async { do { try await presenter.reloadQuiet() } catch {} }

    func followLive() async { do { try await presenter.watch() } catch {} }

    func setFilter(_ filter: MusicLibFilterKind) { presenter.setFilter(filter: Mapping.kotlinMusicLibFilter(filter)) }

    func collapseAll() { presenter.collapseAll() }

    func expand(_ albumId: Int64) async { do { try await presenter.expand(albumId: albumId) } catch {} }

    func expandAll() async { do { try await presenter.expandAll() } catch {} }

    func toggleKeepAlbum(_ albumId: Int64) async { do { try await presenter.toggleKeepAlbum(albumId: albumId) } catch {} }

    func setKeepArtist(_ on: Bool) async { do { try await presenter.setKeepArtist(on: on) } catch {} }

    func setRules(_ id: Int64) async { do { try await presenter.setRules(id: id) } catch {} }

    func searchAll() async { do { try await presenter.searchAll() } catch {} }

    func searchAlbum(_ albumId: Int64) async { do { try await presenter.searchAlbum(albumId: albumId) } catch {} }

    func searchTrack(albumId: Int64, slotId: Int64, name: String) async {
        do { try await presenter.searchTrack(albumId: albumId, slotId: slotId, name: name) } catch {}
    }

    func applyPreset(_ preset: MusicKindsPresetKind) async {
        do { try await presenter.applyPreset(preset: Mapping.kotlinMusicPreset(preset)) } catch {}
    }

    func saveKinds(newAlbums: String, types: [String], secondary: [String], statuses: [String]) async -> Bool {
        do {
            let result = try await presenter.saveKinds(newAlbums: newAlbums, types: types, secondary: secondary, statuses: statuses)
            return result.boolValue
        } catch {
            return false
        }
    }

    func moveTo(_ folderId: Int64) async { do { try await presenter.moveTo(folderId: folderId) } catch {} }

    func refreshDetails() async { do { try await presenter.refreshDetails() } catch {} }

    func removed() { presenter.removed() }
}

final class MusicLibraryAlbumDriver: @unchecked Sendable {
    private let presenter: MusicLibraryAlbumPresenter

    init(presenter: MusicLibraryAlbumPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicLibraryAlbumInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicLibraryAlbum(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func refresh() async { do { try await presenter.refresh() } catch {} }

    func toggleKeep() async { do { try await presenter.toggleKeep() } catch {} }

    func changePressing(_ id: Int64) async { do { try await presenter.changePressing(id: id) } catch {} }

    func searchNow() async { do { try await presenter.searchNow() } catch {} }

    func reloadDetail() async { do { try await presenter.reloadDetail() } catch {} }

    func removed() { presenter.removed() }
}

final class MusicLeftoversDriver: @unchecked Sendable {
    private let presenter: MusicLeftoversPresenter

    init(presenter: MusicLeftoversPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MusicLeftoversInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.musicLeftovers(state)
            await deliver(mapped)
        }
    }

    func load() async { do { try await presenter.load() } catch {} }

    func dismiss(_ fileId: Int64, dismissed: Bool) async { do { try await presenter.dismiss(fileId: fileId, dismissed: dismissed) } catch {} }

    func openMatch(_ fileId: Int64) async { do { try await presenter.openMatch(fileId: fileId) } catch {} }

    func closeMatch() { presenter.closeMatch() }

    func pickAlbum(_ albumId: Int64) async { do { try await presenter.pickAlbum(albumId: albumId) } catch {} }

    func pickTrack(_ id: Int64) { presenter.pickTrack(id: id) }

    func confirmMatch() async { do { try await presenter.confirmMatch() } catch {} }

    func undoMatch() async { do { try await presenter.undoMatch() } catch {} }

    func usePressing(albumId: Int64, pressingId: Int64) async {
        guard let hint = presenter.state.value.pressings.first(where: { $0.albumId == albumId && $0.pressingId == pressingId }) else { return }
        do { try await presenter.usePressing(hint: hint) } catch {}
    }
}
