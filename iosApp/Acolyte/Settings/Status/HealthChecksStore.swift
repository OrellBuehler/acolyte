import Foundation
import Observation

@MainActor
@Observable
final class HealthChecksStore {
    private(set) var info: HealthChecksInfo = .initial

    private let driver = SDKClient.shared.makeHealthChecks()

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

    func runChecks() {
        let driver = self.driver
        Task { await driver.runChecks() }
    }

    func setNumber(_ field: HealthChecksFieldID, _ text: String) {
        driver.setNumber(field, text)
    }

    func setDays(_ days: Int) {
        driver.setDays(days)
    }

    func setFeed(_ url: String) {
        driver.setFeed(url)
    }

    func setLocal(_ on: Bool) {
        driver.setLocal(on)
    }

    func setPrerelease(_ on: Bool) {
        driver.setPrerelease(on)
    }

    func discard() {
        driver.discard()
    }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }

    func dismissNotice() {
        driver.dismissNotice()
    }
}
