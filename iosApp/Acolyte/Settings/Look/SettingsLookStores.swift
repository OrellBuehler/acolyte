import Foundation
import Observation

@MainActor
@Observable
final class SettingsLookAppearanceStore {
    private(set) var info: SettingsLookAppearanceInfo = .initial

    private let driver = SDKClient.shared.makeSettingsLookAppearance()

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

    func setLanguage(_ code: String) {
        let driver = self.driver
        Task { await driver.setLanguage(code) }
    }

    func setAutoplayTrailers(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setAutoplayTrailers(on) }
    }

    func setSignInArt(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setSignInArt(on) }
    }
}

@MainActor
@Observable
final class SettingsLookDefaultsStore {
    private(set) var info: SettingsLookDefaultsInfo = .initial

    private let driver = SDKClient.shared.makeSettingsLookDefaults()

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

    func setFolder(show: Bool, _ id: Int64) { driver.setFolder(show: show, id) }

    func setRules(show: Bool, _ id: Int64) { driver.setRules(show: show, id) }

    func setKeepUpToDate(show: Bool, _ on: Bool) { driver.setKeepUpToDate(show: show, on) }

    func setSearchOnAdd(show: Bool, _ on: Bool) { driver.setSearchOnAdd(show: show, on) }

    func setReleaseGate(_ value: String) { driver.setReleaseGate(value) }

    func setGateDays(_ text: String) { driver.setGateDays(text) }

    func setSeasonChoice(_ value: String) { driver.setSeasonChoice(value) }

    func setNewSeasons(_ value: String) { driver.setNewSeasons(value) }

    func setEpisodeOrder(_ value: String) { driver.setEpisodeOrder(value) }

    func setDetailsSource(_ value: String) { driver.setDetailsSource(value) }

    func discard() { driver.discard() }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }
}

@MainActor
@Observable
final class SettingsLookMetadataStore {
    private(set) var info: SettingsLookMetadataInfo = .initial

    private let driver = SDKClient.shared.makeSettingsLookMetadata()

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

    func chooseMode(_ mode: String) {
        let driver = self.driver
        Task { await driver.chooseMode(mode) }
    }

    func openTvdb() { driver.openTvdb() }

    func setKey(_ name: String, _ text: String) { driver.setKey(name, text) }

    func clearKey(_ name: String) { driver.clearKey(name) }

    func restoreKey(_ name: String) { driver.restoreKey(name) }

    func testKey(_ name: String) {
        let driver = self.driver
        Task { await driver.testKey(name) }
    }

    func checkAgain() {
        let driver = self.driver
        Task { await driver.checkAgain() }
    }

    func discard() { driver.discard() }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }
}
