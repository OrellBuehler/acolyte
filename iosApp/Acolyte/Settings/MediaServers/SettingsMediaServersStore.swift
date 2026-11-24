import Foundation
import Observation

@MainActor
@Observable
final class SettingsMediaServersStore {
    private(set) var info: SettingsMediaServersInfo = .initial

    private let driver = SDKClient.shared.makeSettingsMediaServers()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func startAdd() { driver.startAdd() }

    func startChange(_ id: Int64) { driver.startChange(id) }

    func cancelForm() { driver.cancelForm() }

    func setName(_ text: String) { driver.setName(text) }

    func setKind(_ wire: String) { driver.setKind(wire) }

    func setUrl(_ text: String) { driver.setUrl(text) }

    func setUsername(_ text: String) { driver.setUsername(text) }

    func setToken(_ text: String) { driver.setToken(text) }

    func setWebUrl(_ text: String) { driver.setWebUrl(text) }

    func setPlainPassword(_ on: Bool) { driver.setPlainPassword(on) }

    func setOnImport(_ on: Bool) { driver.setOnImport(on) }

    func setOnRename(_ on: Bool) { driver.setOnRename(on) }

    func setOnDelete(_ on: Bool) { driver.setOnDelete(on) }

    func setPathFrom(_ text: String) { driver.setPathFrom(text) }

    func setPathTo(_ text: String) { driver.setPathTo(text) }

    func setAllowLocalNetwork(_ on: Bool) { driver.setAllowLocalNetwork(on) }

    func setEnabled(_ on: Bool) { driver.setEnabled(on) }

    func findPlexServers() {
        let driver = self.driver
        Task { await driver.findPlexServers() }
    }

    func pickPlex(_ index: Int) { driver.pickPlex(index) }

    func pickPlexAddress(_ url: String) { driver.pickPlexAddress(url) }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }

    func test(_ id: Int64) {
        let driver = self.driver
        Task { await driver.test(id) }
    }

    func remove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.remove(id) }
    }

    func setWatchHistory(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setWatchHistory(on) }
    }

    func setLabelsEnabled(_ on: Bool) { driver.setLabelsEnabled(on) }

    func setLabelText(_ text: String) { driver.setLabelText(text) }

    func setLabelAskers(_ on: Bool) { driver.setLabelAskers(on) }

    func saveLabels() {
        let driver = self.driver
        Task { await driver.saveLabels() }
    }

    func runLabels() {
        let driver = self.driver
        Task { await driver.runLabels() }
    }

    func setTracksEnabled(_ on: Bool) { driver.setTracksEnabled(on) }

    func addTrackLanguage(_ subtitles: Bool) { driver.addTrackLanguage(subtitles) }

    func removeTrackLanguage(_ subtitles: Bool, _ index: Int) { driver.removeTrackLanguage(subtitles, index) }

    func moveTrackLanguage(_ subtitles: Bool, _ index: Int, up: Bool) { driver.moveTrackLanguage(subtitles, index, up: up) }

    func setTrackLanguage(_ subtitles: Bool, _ index: Int, _ code: String) { driver.setTrackLanguage(subtitles, index, code) }

    func saveTracks() {
        let driver = self.driver
        Task { await driver.saveTracks() }
    }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }

    func dismissNotice(_ id: Int64) { driver.dismissNotice(id) }
}
