import Foundation
import Observation

@MainActor
@Observable
final class SettingsServerAlertsStore {
    private(set) var info: SettingsServerAlertsInfo = .initial

    private let driver = SDKClient.shared.makeSettingsAlerts()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func retryPack() {
        let driver = self.driver
        Task { await driver.retryPack() }
    }

    func startAdd() { driver.startAdd() }

    func startChange(_ id: Int64) { driver.startChange(id) }

    func cancelForm() { driver.cancelForm() }

    func setText(_ field: SettingsServerFieldInfo, _ value: String) { driver.setText(field, value) }

    func setKind(_ kind: SettingsServerAlertKindInfo) { driver.setKind(kind) }

    func setEnabled(_ on: Bool) { driver.setEnabled(on) }

    func setAllowLocalNetwork(_ on: Bool) { driver.setAllowLocalNetwork(on) }

    func setClearToken(_ on: Bool) { driver.setClearToken(on) }

    func setMoment(_ id: String, _ on: Bool) { driver.setMoment(id, on) }

    func setGroup(_ key: String, _ on: Bool) { driver.setGroup(key, on) }

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

    func setPack(_ style: String) {
        let driver = self.driver
        Task { await driver.setPack(style) }
    }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }
}

@MainActor
@Observable
final class SettingsServerBackupsStore {
    private(set) var info: SettingsServerBackupsInfo = .initial

    private let driver = SDKClient.shared.makeSettingsBackups()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func makeNow() {
        let driver = self.driver
        Task { await driver.makeNow() }
    }

    func setPreset(_ preset: SettingsServerBackupPresetInfo) {
        let driver = self.driver
        Task { await driver.setPreset(preset) }
    }

    func download(_ name: String) {
        let driver = self.driver
        Task { await driver.download(name) }
    }

    func delete(_ name: String) {
        let driver = self.driver
        Task { await driver.delete(name) }
    }

    func downloadKeyKit() {
        let driver = self.driver
        Task { await driver.downloadKeyKit() }
    }

    func stagingPath(_ name: String) -> String { driver.stagingPath(name) }

    func restore(_ path: String, _ name: String) {
        let driver = self.driver
        Task { await driver.restore(path, name) }
    }

    func release() { driver.release() }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }
}

@MainActor
@Observable
final class SettingsServerLogsStore {
    private(set) var info: SettingsServerLogsInfo = .initial

    private let driver = SDKClient.shared.makeSettingsLogs()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func applyFilter(_ level: String, _ query: String, _ from: String, _ to: String) {
        let driver = self.driver
        Task { await driver.applyFilter(level, query, from, to) }
    }

    func clear() {
        let driver = self.driver
        Task { await driver.clear() }
    }

    func older() {
        let driver = self.driver
        Task { await driver.older() }
    }

    func newer() {
        let driver = self.driver
        Task { await driver.newer() }
    }

    func download() {
        let driver = self.driver
        Task { await driver.download() }
    }

    func release() { driver.release() }

    func dismissDownloadError() { driver.dismissDownloadError() }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }
}
