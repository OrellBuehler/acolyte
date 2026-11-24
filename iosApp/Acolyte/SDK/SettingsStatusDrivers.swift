@preconcurrency import shared
import Foundation

final class HealthDriver: @unchecked Sendable {
    private let presenter: HealthPresenter

    init(presenter: HealthPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (HealthInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.health(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func checkAll() async {
        do { try await presenter.checkAll() } catch {}
    }

    func check(_ key: String) async {
        do { try await presenter.check(key: key) } catch {}
    }

    func checkUpdates() async {
        do { try await presenter.checkUpdates() } catch {}
    }

    func setCheckOn(_ on: Bool) async {
        do { try await presenter.setCheckOn(on: on) } catch {}
    }

    func setMode(_ mode: StatusModeID) async {
        do { try await presenter.setMode(mode: Mapping.kotlinStatusMode(mode)) } catch {}
    }

    func installNow() async {
        do { try await presenter.installNow() } catch {}
    }

    func goBack() async {
        do { try await presenter.goBack() } catch {}
    }

    func downloadBundle() async {
        do { try await presenter.downloadBundle() } catch {}
    }

    func bundleHandled() {
        presenter.bundleHandled()
    }

    func dismissNotice() {
        presenter.dismissNotice()
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }
}

final class HealthChecksDriver: @unchecked Sendable {
    private let presenter: SettingsHealthChecksPresenter

    init(presenter: SettingsHealthChecksPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (HealthChecksInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.healthChecks(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func runChecks() async {
        do { try await presenter.runChecks() } catch {}
    }

    func setNumber(_ field: HealthChecksFieldID, _ text: String) {
        presenter.setNumber(field: Mapping.kotlinHealthChecksField(field), text: text)
    }

    func setDays(_ days: Int) {
        presenter.setDays(days: Int32(days))
    }

    func setFeed(_ url: String) {
        presenter.setFeed(url: url)
    }

    func setLocal(_ on: Bool) {
        presenter.setLocal(on: on)
    }

    func setPrerelease(_ on: Bool) {
        presenter.setPrerelease(on: on)
    }

    func discard() {
        presenter.discard()
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func dismissNotice() {
        presenter.dismissNotice()
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }
}
