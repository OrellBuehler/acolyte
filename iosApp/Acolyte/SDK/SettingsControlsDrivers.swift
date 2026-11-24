@preconcurrency import shared
import Foundation

final class SettingsControlsPartsDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: SettingsControlsPartsPresenter

    let reauth: ReauthDriver

    init(presenter: SettingsControlsPartsPresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func watch(deliver: @MainActor @Sendable (SettingsControlsPartsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsControlsParts(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }

    func turnOn(_ part: String) async {
        do { try await presenter.turnOn(part: part) } catch {}
    }

    func turnOff(_ part: String) async {
        do { try await presenter.turnOff(part: part) } catch {}
    }

    func restart(_ part: String) async {
        do { try await presenter.restart(part: part) } catch {}
    }

    func stage(_ stage: String, _ action: SettingsControlsStageActionKind) async {
        do { try await presenter.stage(stage: stage, action: Mapping.kotlinStageAction(action)) } catch {}
    }

    func reconnect(_ key: String) async {
        do { try await presenter.reconnect(key: key) } catch {}
    }

    func confirm() async {
        do { try await presenter.confirm() } catch {}
    }

    func cancel() {
        presenter.cancel()
    }
}

final class SettingsControlsDangerDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: SettingsControlsDangerPresenter

    let reauth: ReauthDriver

    init(presenter: SettingsControlsDangerPresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func watch(deliver: @MainActor @Sendable (SettingsControlsDangerInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsControlsDanger(state)
            await deliver(mapped)
        }
    }

    func load() {
        presenter.load()
    }

    func request(_ kind: SettingsControlsActionKind) async {
        do { try await presenter.request(kind: Mapping.kotlinControlKind(kind)) } catch {}
    }

    func confirm() async {
        do { try await presenter.confirm() } catch {}
    }

    func cancel() {
        presenter.cancel()
    }

    func dismissStopped() {
        presenter.dismissStopped()
    }

    func waitForServer() async {
        do { try await presenter.waitForServer() } catch {}
    }
}

final class SettingsControlsThisAppDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: SettingsControlsThisAppPresenter

    let reauth: ReauthDriver

    init(presenter: SettingsControlsThisAppPresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func watch(deliver: @MainActor @Sendable (SettingsControlsThisAppInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsControlsThisApp(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }

    func setHistoryRoutine(_ days: Int) async {
        do { try await presenter.setHistoryRoutine(days: Int32(days)) } catch {}
    }

    func setHistoryOther(_ days: Int) async {
        do { try await presenter.setHistoryOther(days: Int32(days)) } catch {}
    }

    func setCacheGb(_ gb: Int) async {
        do { try await presenter.setCacheGb(gb: Int32(gb)) } catch {}
    }

    func setCountInstall(_ on: Bool) async {
        do { try await presenter.setCountInstall(on: on) } catch {}
    }

    func proveStorage() async {
        do { try await presenter.proveStorage() } catch {}
    }

    func setUrlBase(_ text: String) {
        presenter.setUrlBase(text: text)
    }

    func saveUrlBase() async {
        do { try await presenter.saveUrlBase() } catch {}
    }

    func setProxyType(_ type: String) {
        presenter.setProxyType(type: type)
    }

    func setProxyHost(_ text: String) {
        presenter.setProxyHost(text: text)
    }

    func setProxyPort(_ text: String) {
        presenter.setProxyPort(text: text)
    }

    func setProxyUsername(_ text: String) {
        presenter.setProxyUsername(text: text)
    }

    func setProxyPassword(_ text: String) {
        presenter.setProxyPassword(text: text)
    }

    func setProxyBypass(_ text: String) {
        presenter.setProxyBypass(text: text)
    }

    func setProxyBypassLocal(_ on: Bool) {
        presenter.setProxyBypassLocal(on: on)
    }

    func saveProxy() async {
        do { try await presenter.saveProxy() } catch {}
    }
}

final class SettingsControlsSchedulesDriver: @unchecked Sendable {
    private let presenter: SettingsControlsSchedulesPresenter

    init(presenter: SettingsControlsSchedulesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsControlsSchedulesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsControlsSchedules(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }

    func runNow(_ kind: String) async {
        do { try await presenter.runNow(kind: kind) } catch {}
    }

    func edit(_ kind: String) {
        presenter.edit(kind: kind)
    }

    func closeEditor() {
        presenter.closeEditor()
    }

    func setAmount(_ text: String) {
        presenter.setAmount(text: text)
    }

    func stepAmount(up: Bool) {
        presenter.stepAmount(up: up)
    }

    func saveEditor() async {
        do { try await presenter.saveEditor() } catch {}
    }

    func setEnabled(_ kind: String, _ enabled: Bool) async {
        do { try await presenter.setEnabled(kind: kind, enabled: enabled) } catch {}
    }
}
