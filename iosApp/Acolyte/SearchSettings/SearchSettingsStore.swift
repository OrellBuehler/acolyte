import Foundation
import Observation

@MainActor
@Observable
final class SearchSettingsStore {
    private(set) var info: SearchSettingsInfo = .initial

    private let driver = SDKClient.shared.makeSearchSettings()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func setSwitch(_ wire: String, _ on: Bool) { driver.setSwitch(wire, on) }

    func setChoice(_ wire: String, _ value: String) { driver.setChoice(wire, value) }

    func setNumber(_ wire: String, _ text: String) { driver.setNumber(wire, text) }

    func discard() { driver.discard() }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }

    func checkFeeds() {
        let driver = self.driver
        Task { await driver.checkFeeds() }
    }

    func findBetter() {
        let driver = self.driver
        Task { await driver.findBetter() }
    }
}
