import Foundation
import Observation

@MainActor
@Observable
final class SettingsControlsToaster {
    private(set) var text: String?

    @ObservationIgnored private var token = 0

    func show(_ message: String) {
        text = message
        token += 1
        let current = token
        Task {
            try? await Task.sleep(for: .seconds(4))
            if token == current { text = nil }
        }
    }
}

@MainActor
@Observable
final class SettingsControlsPartsStore {
    private(set) var info: SettingsControlsPartsInfo = .initial
    let toaster = SettingsControlsToaster()

    @ObservationIgnored private var lastNotice = 0

    private let driver = SDKClient.shared.makeSettingsControlsParts()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.take(value)
        }
    }

    private func take(_ value: SettingsControlsPartsInfo) {
        info = value
        guard let notice = value.notice, notice.id != lastNotice else { return }
        lastNotice = notice.id
        toaster.show(SettingsControlsText.notice(notice))
    }

    func load() async { await driver.load() }

    func followLive() async { await driver.followLive() }

    func refresh() async { await driver.refresh() }

    func turnOn(_ part: String) {
        let driver = self.driver
        Task { await driver.turnOn(part) }
    }

    func turnOff(_ part: String) {
        let driver = self.driver
        Task { await driver.turnOff(part) }
    }

    func restart(_ part: String) {
        let driver = self.driver
        Task { await driver.restart(part) }
    }

    func stage(_ stage: String, _ action: SettingsControlsStageActionKind) {
        let driver = self.driver
        Task { await driver.stage(stage, action) }
    }

    func reconnect(_ key: String) {
        let driver = self.driver
        Task { await driver.reconnect(key) }
    }

    func confirm() {
        let driver = self.driver
        Task { await driver.confirm() }
    }

    func cancel() { driver.cancel() }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }
}

@MainActor
@Observable
final class SettingsControlsDangerStore {
    private(set) var info: SettingsControlsDangerInfo = .initial
    let toaster = SettingsControlsToaster()

    @ObservationIgnored private var lastNotice = 0

    private let driver = SDKClient.shared.makeSettingsControlsDanger()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.take(value)
        }
    }

    private func take(_ value: SettingsControlsDangerInfo) {
        info = value
        guard let notice = value.notice, notice.id != lastNotice else { return }
        lastNotice = notice.id
        toaster.show(SettingsControlsText.notice(notice))
    }

    func load() async { driver.load() }

    func waitForServer() async { await driver.waitForServer() }

    func request(_ kind: SettingsControlsActionKind) {
        let driver = self.driver
        Task { await driver.request(kind) }
    }

    func confirm() {
        let driver = self.driver
        Task { await driver.confirm() }
    }

    func cancel() { driver.cancel() }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }

    func dismissStopped() { driver.dismissStopped() }
}

@MainActor
@Observable
final class SettingsControlsThisAppStore {
    private(set) var info: SettingsControlsThisAppInfo = .initial
    let toaster = SettingsControlsToaster()

    @ObservationIgnored private var lastNotice = 0

    private let driver = SDKClient.shared.makeSettingsControlsThisApp()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.take(value)
        }
    }

    private func take(_ value: SettingsControlsThisAppInfo) {
        info = value
        guard let saved = value.saved, saved.id != lastNotice else { return }
        lastNotice = saved.id
        toaster.show(SettingsControlsText.saved(saved))
    }

    func load() async { await driver.load() }

    func followLive() async { await driver.followLive() }

    func refresh() async { await driver.refresh() }

    func setHistoryRoutine(_ days: Int) {
        let driver = self.driver
        Task { await driver.setHistoryRoutine(days) }
    }

    func setHistoryOther(_ days: Int) {
        let driver = self.driver
        Task { await driver.setHistoryOther(days) }
    }

    func setCacheGb(_ gb: Int) {
        let driver = self.driver
        Task { await driver.setCacheGb(gb) }
    }

    func setCountInstall(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setCountInstall(on) }
    }

    func proveStorage() {
        let driver = self.driver
        Task { await driver.proveStorage() }
    }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }

    func setUrlBase(_ text: String) { driver.setUrlBase(text) }

    func saveUrlBase() {
        let driver = self.driver
        Task { await driver.saveUrlBase() }
    }

    func setProxyType(_ type: String) { driver.setProxyType(type) }

    func setProxyHost(_ text: String) { driver.setProxyHost(text) }

    func setProxyPort(_ text: String) { driver.setProxyPort(text) }

    func setProxyUsername(_ text: String) { driver.setProxyUsername(text) }

    func setProxyPassword(_ text: String) { driver.setProxyPassword(text) }

    func setProxyBypass(_ text: String) { driver.setProxyBypass(text) }

    func setProxyBypassLocal(_ on: Bool) { driver.setProxyBypassLocal(on) }

    func saveProxy() {
        let driver = self.driver
        Task { await driver.saveProxy() }
    }
}

@MainActor
@Observable
final class SettingsControlsSchedulesStore {
    private(set) var info: SettingsControlsSchedulesInfo = .initial
    let toaster = SettingsControlsToaster()

    @ObservationIgnored private var lastNotice = 0

    private let driver = SDKClient.shared.makeSettingsControlsSchedules()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.take(value)
        }
    }

    private func take(_ value: SettingsControlsSchedulesInfo) {
        info = value
        guard let notice = value.notice, notice.id != lastNotice else { return }
        lastNotice = notice.id
        toaster.show(SettingsControlsText.scheduleNote(notice))
    }

    func load() async { await driver.load() }

    func followLive() async { await driver.followLive() }

    func refresh() async { await driver.refresh() }

    func runNow(_ kind: String) {
        let driver = self.driver
        Task { await driver.runNow(kind) }
    }

    func edit(_ kind: String) { driver.edit(kind) }

    func closeEditor() { driver.closeEditor() }

    func setAmount(_ text: String) { driver.setAmount(text) }

    func stepAmount(up: Bool) { driver.stepAmount(up: up) }

    func saveEditor() {
        let driver = self.driver
        Task { await driver.saveEditor() }
    }

    func setEnabled(_ kind: String, _ enabled: Bool) {
        let driver = self.driver
        Task { await driver.setEnabled(kind, enabled) }
    }
}
