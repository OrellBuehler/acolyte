@preconcurrency import shared
import Foundation

final class SettingsServerAlertsDriver: @unchecked Sendable {
    private let presenter: SettingsServerAlertsPresenter

    init(presenter: SettingsServerAlertsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsServerAlertsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsAlerts(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func retryPack() async {
        do { try await presenter.retryPack() } catch {}
    }

    func startAdd() {
        presenter.startAdd()
    }

    func startChange(_ id: Int64) {
        presenter.startChange(id: id)
    }

    func cancelForm() {
        presenter.cancelForm()
    }

    func setText(_ field: SettingsServerFieldInfo, _ value: String) {
        presenter.setText(field: Mapping.kotlinServerField(field), value: value)
    }

    func setKind(_ kind: SettingsServerAlertKindInfo) {
        presenter.setKind(kind: Mapping.kotlinServerKind(kind))
    }

    func setEnabled(_ on: Bool) {
        presenter.setEnabled(on: on)
    }

    func setAllowLocalNetwork(_ on: Bool) {
        presenter.setAllowLocalNetwork(on: on)
    }

    func setClearToken(_ on: Bool) {
        presenter.setClearToken(on: on)
    }

    func setMoment(_ id: String, _ on: Bool) {
        presenter.setMoment(id: id, on: on)
    }

    func setGroup(_ key: String, _ on: Bool) {
        presenter.setGroup(key: key, on: on)
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func test(_ id: Int64) async {
        do { try await presenter.test(id: id) } catch {}
    }

    func remove(_ id: Int64) async {
        do { try await presenter.remove(id: id) } catch {}
    }

    func setPack(_ style: String) async {
        do { try await presenter.setPack(style: style) } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class SettingsServerBackupsDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: SettingsServerBackupsPresenter

    let reauth: ReauthDriver

    init(presenter: SettingsServerBackupsPresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func watch(deliver: @MainActor @Sendable (SettingsServerBackupsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsBackups(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func makeNow() async {
        do { try await presenter.makeNow() } catch {}
    }

    func setPreset(_ preset: SettingsServerBackupPresetInfo) async {
        do { try await presenter.setPreset(next: Mapping.kotlinBackupPreset(preset)) } catch {}
    }

    func download(_ name: String) async {
        do { try await presenter.download(name: name) } catch {}
    }

    func delete(_ name: String) async {
        do { try await presenter.delete(name: name) } catch {}
    }

    func downloadKeyKit() async {
        do { try await presenter.downloadKeyKit() } catch {}
    }

    func stagingPath(_ name: String) -> String {
        presenter.stagingPath(name: name)
    }

    func restore(_ path: String, _ name: String) async {
        do { try await presenter.restore(path: path, name: name) } catch {}
    }

    func release() {
        presenter.release()
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class SettingsServerLogsDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: SettingsServerLogsPresenter

    let reauth: ReauthDriver

    init(presenter: SettingsServerLogsPresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func watch(deliver: @MainActor @Sendable (SettingsServerLogsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsLogs(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func applyFilter(_ level: String, _ query: String, _ from: String, _ to: String) async {
        do { try await presenter.applyFilter(level: level, query: query, from: from, to: to) } catch {}
    }

    func clear() async {
        do { try await presenter.clear() } catch {}
    }

    func older() async {
        do { try await presenter.older() } catch {}
    }

    func newer() async {
        do { try await presenter.newer() } catch {}
    }

    func download() async {
        do { try await presenter.download() } catch {}
    }

    func release() {
        presenter.release()
    }

    func dismissDownloadError() {
        presenter.dismissDownloadError()
    }
}
