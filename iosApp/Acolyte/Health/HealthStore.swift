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

    func checkAll() {
        let driver = self.driver
        Task { await driver.checkAll() }
    }

    func check(_ key: String) {
        let driver = self.driver
        Task { await driver.check(key) }
    }

    func checkUpdates() {
        let driver = self.driver
        Task { await driver.checkUpdates() }
    }

    func setCheckOn(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setCheckOn(on) }
    }

    func setMode(_ mode: StatusModeID) {
        let driver = self.driver
        Task { await driver.setMode(mode) }
    }

    func installNow() {
        let driver = self.driver
        Task { await driver.installNow() }
    }

    func goBack() {
        let driver = self.driver
        Task { await driver.goBack() }
    }

    func downloadBundle() {
        let driver = self.driver
        Task { await driver.downloadBundle() }
    }

    func bundleHandled() {
        driver.bundleHandled()
    }

    func dismissNotice() {
        driver.dismissNotice()
    }
}
