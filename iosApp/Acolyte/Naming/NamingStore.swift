import Foundation
import Observation

@MainActor
@Observable
final class NamingStore {
    private(set) var info: NamingInfo = .initial

    private let driver = SDKClient.shared.makeNaming()

    func watch() async {
        await driver.watch { [weak self] value in self?.info = value }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followExample() async { await driver.followExample() }

    func words(_ field: NamingField, _ query: String) -> [NamingWordInfo] {
        driver.words(field.rawValue, query)
    }

    func setText(_ field: NamingField, _ text: String) { driver.setText(field.rawValue, text) }

    func insertWord(_ field: NamingField, _ word: String) { driver.insertWord(field.rawValue, word) }

    func setSwitch(_ wire: String, _ on: Bool) { driver.setSwitch(wire, on) }

    func setChoice(_ wire: String, _ value: String) { driver.setChoice(wire, value) }

    func setNumber(_ kind: NamingNumberKind, _ text: String) { driver.setNumber(kind.rawValue, text) }

    func applyStyle(_ wire: String) { driver.applyStyle(wire) }

    func discard() { driver.discard() }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }

    func cleanHolding() {
        let driver = self.driver
        Task { await driver.cleanHolding() }
    }
}
