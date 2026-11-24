@preconcurrency import shared
import Foundation

final class SourcesDriver: @unchecked Sendable {
    private let presenter: SettingsSourcesPresenter

    init(presenter: SettingsSourcesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SourcesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.sources(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func reloadQuietly() async {
        do { try await presenter.reloadQuietly() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }

    func startAdd() { presenter.startAdd() }

    func startChange(_ id: Int64) { presenter.startChange(id: id) }

    func cancelEdit() { presenter.cancelEdit() }

    func settled(_ form: SourceFormInfo) -> SourceFormInfo {
        Mapping.sourceForm(presenter.settled(form: Mapping.kotlinSourceForm(form)))
    }

    func withFormat(_ form: SourceFormInfo, _ format: String) -> SourceFormInfo {
        Mapping.sourceForm(presenter.settled(form: Mapping.kotlinSourceForm(form).withFormat(value: format)))
    }

    func withSite(_ form: SourceFormInfo, _ site: String) -> SourceFormInfo {
        Mapping.sourceForm(presenter.settled(form: Mapping.kotlinSourceForm(form).withSite(id: site)))
    }

    func withKind(_ form: SourceFormInfo, _ kind: String, _ on: Bool) -> SourceFormInfo {
        Mapping.sourceForm(Mapping.kotlinSourceForm(form).withKind(kind: kind, on: on))
    }

    func withTagText(_ form: SourceFormInfo, _ text: String) -> SourceFormInfo {
        Mapping.sourceForm(Mapping.kotlinSourceForm(form).withTagText(text: text))
    }

    func save(_ form: SourceFormInfo) async {
        do { _ = try await presenter.save(form: Mapping.kotlinSourceForm(form)) } catch {}
    }

    func test(_ id: Int64) async {
        do { try await presenter.test(id: id) } catch {}
    }

    func tryNow(_ id: Int64) async {
        do { try await presenter.tryNow(id: id) } catch {}
    }

    func setPaused(_ id: Int64, _ paused: Bool) async {
        do { try await presenter.setPaused(id: id, paused: paused) } catch {}
    }

    func remove(_ id: Int64) async {
        do { try await presenter.remove(id: id) } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    func sites() -> [SourceSiteInfo] {
        Mapping.builtInSites()
    }
}

final class BridgeDriver: @unchecked Sendable {
    private let presenter: SettingsBridgePresenter

    init(presenter: SettingsBridgePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (BridgesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.bridges(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func startConnect(_ kind: String) { presenter.startConnect(kind: kind) }

    func startChange(_ id: Int64) { presenter.startChange(id: id) }

    func cancelForm() { presenter.cancelForm() }

    func test(_ draft: BridgeDraftInfo) async {
        do { try await presenter.test(draft: Mapping.kotlinBridgeDraft(draft)) } catch {}
    }

    func link(_ draft: BridgeDraftInfo) async {
        do { _ = try await presenter.link(draft: Mapping.kotlinBridgeDraft(draft)) } catch {}
    }

    func confirmReauth(_ password: String, _ code: String) async {
        do { try await presenter.confirmReauth(password: password, code: code) } catch {}
    }

    func cancelReauth() { presenter.cancelReauth() }

    func sync(_ id: Int64) async {
        do { try await presenter.sync(id: id) } catch {}
    }

    func unlink(_ id: Int64, keepSources: Bool) async -> Bool {
        do {
            let done = try await presenter.unlink(id: id, keepSources: keepSources)
            return done.boolValue
        } catch {
            return false
        }
    }

    func clearRemoveError() { presenter.clearRemoveError() }

    func dismissKeys() { presenter.dismissKeys() }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class SourceCheckDriver: @unchecked Sendable {
    private let presenter: SettingsSourceCheckPresenter

    init(presenter: SettingsSourceCheckPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SourceCheckInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.sourceCheck(state)
            await deliver(mapped)
        }
    }

    func run(_ kind: String, _ title: String) async {
        do { try await presenter.run(kind: kind, title: title) } catch {}
    }

    func titleAfterKind(_ old: String, _ title: String, _ new: String) -> String {
        presenter.titleAfterKind(old: old, title: title, new: new)
    }

    func sample(_ kind: String) -> String {
        presenter.sample(kind: kind)
    }
}

final class SourceHealthDriver: @unchecked Sendable {
    private let presenter: SettingsSourceHealthPresenter

    init(presenter: SettingsSourceHealthPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SourceHealthInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.sourceHealth(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func setDays(_ days: Int) async {
        do { try await presenter.setDays(days: Int32(days)) } catch {}
    }

    func tryNow() async {
        do { try await presenter.tryNow() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }
}
