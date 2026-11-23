import Foundation
import Observation

@MainActor
@Observable
final class HealthStore {
    private(set) var info: HealthInfo = .initial

    private let driver = SDKClient.shared.makeHealth()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async {
        await driver.load()
    }

    func followLive() async {
        await driver.followLive()
    }

    func refresh() async {
        await driver.refresh()
    }

    func runChecks() {
        let driver = self.driver
        Task { await driver.runChecks() }
    }
}
