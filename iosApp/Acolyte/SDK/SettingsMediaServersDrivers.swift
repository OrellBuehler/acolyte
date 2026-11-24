@preconcurrency import shared
import Foundation

final class SettingsMediaServersDriver: @unchecked Sendable {
    private let presenter: SettingsMediaServersPresenter

    init(presenter: SettingsMediaServersPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsMediaServersInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsMediaServers(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func startAdd() {
        presenter.startAdd()
    }

    func startChange(_ id: Int64) {
        presenter.startChange(id: id)
    }

    func cancelForm() {
        presenter.cancelForm()
    }

    func setName(_ text: String) {
        presenter.setName(text: text)
    }

    func setKind(_ wire: String) {
        presenter.setKind(wire: wire)
    }

    func setUrl(_ text: String) {
        presenter.setUrl(text: text)
    }

    func setUsername(_ text: String) {
        presenter.setUsername(text: text)
    }

    func setToken(_ text: String) {
        presenter.setToken(text: text)
    }

    func setWebUrl(_ text: String) {
        presenter.setWebUrl(text: text)
    }

    func setPlainPassword(_ on: Bool) {
        presenter.setPlainPassword(on: on)
    }

    func setOnImport(_ on: Bool) {
        presenter.setOnImport(on: on)
    }

    func setOnRename(_ on: Bool) {
        presenter.setOnRename(on: on)
    }

    func setOnDelete(_ on: Bool) {
        presenter.setOnDelete(on: on)
    }

    func setPathFrom(_ text: String) {
        presenter.setPathFrom(text: text)
    }

    func setPathTo(_ text: String) {
        presenter.setPathTo(text: text)
    }

    func setAllowLocalNetwork(_ on: Bool) {
        presenter.setAllowLocalNetwork(on: on)
    }

    func setEnabled(_ on: Bool) {
        presenter.setEnabled(on: on)
    }

    func findPlexServers() async {
        do { try await presenter.findPlexServers() } catch {}
    }

    func pickPlex(_ index: Int) {
        presenter.pickPlex(index: Int32(index))
    }

    func pickPlexAddress(_ url: String) {
        presenter.pickPlexAddress(url: url)
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func test(_ id: Int64) async {
        do { try await presenter.test(id: id) } catch {}
    }

    func remove(_ id: Int64) async {
        do { try await presenter.remove(id: id) } catch {}
    }

    func setWatchHistory(_ on: Bool) async {
        do { try await presenter.setWatchHistory(on: on) } catch {}
    }

    func setLabelsEnabled(_ on: Bool) {
        presenter.setLabelsEnabled(on: on)
    }

    func setLabelText(_ text: String) {
        presenter.setLabelText(text: text)
    }

    func setLabelAskers(_ on: Bool) {
        presenter.setLabelAskers(on: on)
    }

    func saveLabels() async {
        do { try await presenter.saveLabels() } catch {}
    }

    func runLabels() async {
        do { try await presenter.runLabels() } catch {}
    }

    func setTracksEnabled(_ on: Bool) {
        presenter.setTracksEnabled(on: on)
    }

    func addTrackLanguage(_ subtitles: Bool) {
        presenter.addTrackLanguage(subtitles: subtitles)
    }

    func removeTrackLanguage(_ subtitles: Bool, _ index: Int) {
        presenter.removeTrackLanguage(subtitles: subtitles, index: Int32(index))
    }

    func moveTrackLanguage(_ subtitles: Bool, _ index: Int, up: Bool) {
        presenter.moveTrackLanguage(subtitles: subtitles, index: Int32(index), up: up)
    }

    func setTrackLanguage(_ subtitles: Bool, _ index: Int, _ code: String) {
        presenter.setTrackLanguage(subtitles: subtitles, index: Int32(index), code: code)
    }

    func saveTracks() async {
        do { try await presenter.saveTracks() } catch {}
    }

    func confirmReauth(_ password: String, _ code: String) async {
        do { try await presenter.confirmReauth(password: password, code: code) } catch {}
    }

    func cancelReauth() {
        presenter.cancelReauth()
    }

    func dismissNotice(_ id: Int64) {
        presenter.dismissNotice(id: id)
    }
}
