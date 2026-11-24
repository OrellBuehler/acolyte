import Foundation
import Observation

@MainActor
@Observable
final class SettingsApiStore {
    private(set) var info: SettingsApiInfo = .initial

    private let driver = SDKClient.shared.makeSettingsApi()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func reloadSeerr() {
        let driver = self.driver
        Task { await driver.reloadSeerr() }
    }

    func makeKey(_ label: String) {
        let driver = self.driver
        Task { await driver.makeKey(label) }
    }

    func revokeKey(_ id: Int64) {
        let driver = self.driver
        Task { await driver.revokeKey(id) }
    }

    func dismissFresh() { driver.dismissFresh() }

    func setSeerrEnabled(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setSeerrEnabled(on) }
    }

    func setSeerrDeleteFiles(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setSeerrDeleteFiles(on) }
    }

    func makeSeerrKey(_ side: String) {
        let driver = self.driver
        Task { await driver.makeSeerrKey(side) }
    }

    func removeSeerrKey(_ side: String) {
        let driver = self.driver
        Task { await driver.removeSeerrKey(side) }
    }

    func dismissSeerrFresh() { driver.dismissSeerrFresh() }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }
}

@MainActor
@Observable
final class SettingsAiStore {
    private(set) var info: SettingsAiInfo = .initial

    private let driver = SDKClient.shared.makeSettingsAi()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func setEnabled(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setEnabled(on) }
    }

    func makeKey(_ label: String, groups: [String]) {
        let driver = self.driver
        Task { await driver.makeKey(label, groups: groups) }
    }

    func changeGroups(_ id: Int64, groups: [String]) {
        let driver = self.driver
        Task { await driver.changeGroups(id, groups: groups) }
    }

    func clearChangeError() { driver.clearChangeError() }

    func revokeKey(_ id: Int64) {
        let driver = self.driver
        Task { await driver.revokeKey(id) }
    }

    func dismissFresh() { driver.dismissFresh() }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }
}
