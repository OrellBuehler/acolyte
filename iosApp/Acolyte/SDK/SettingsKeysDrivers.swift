@preconcurrency import shared
import Foundation

final class SettingsApiDriver: @unchecked Sendable {
    private let presenter: SettingsApiPresenter

    init(presenter: SettingsApiPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsApiInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsApi(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func reloadSeerr() async {
        do { try await presenter.reloadSeerr() } catch {}
    }

    func makeKey(_ label: String) async {
        do { try await presenter.makeKey(label: label) } catch {}
    }

    func revokeKey(_ id: Int64) async {
        do { try await presenter.revokeKey(id: id) } catch {}
    }

    func dismissFresh() {
        presenter.dismissFresh()
    }

    func setSeerrEnabled(_ on: Bool) async {
        do { try await presenter.setSeerrEnabled(on: on) } catch {}
    }

    func setSeerrDeleteFiles(_ on: Bool) async {
        do { try await presenter.setSeerrDeleteFiles(on: on) } catch {}
    }

    func makeSeerrKey(_ side: String) async {
        do { try await presenter.makeSeerrKey(side: side) } catch {}
    }

    func removeSeerrKey(_ side: String) async {
        do { try await presenter.removeSeerrKey(side: side) } catch {}
    }

    func dismissSeerrFresh() {
        presenter.dismissSeerrFresh()
    }

    func confirmReauth(_ password: String, _ code: String) async {
        do { try await presenter.confirmReauth(password: password, code: code) } catch {}
    }

    func cancelReauth() {
        presenter.cancelReauth()
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class SettingsAiDriver: @unchecked Sendable {
    private let presenter: SettingsAiPresenter

    init(presenter: SettingsAiPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsAiInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsAi(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func setEnabled(_ on: Bool) async {
        do { try await presenter.setEnabled(on: on) } catch {}
    }

    func makeKey(_ label: String, groups: [String]) async {
        do { try await presenter.makeKey(label: label, groups: groups) } catch {}
    }

    func changeGroups(_ id: Int64, groups: [String]) async {
        do { try await presenter.changeGroups(id: id, groups: groups) } catch {}
    }

    func clearChangeError() {
        presenter.clearChangeError()
    }

    func revokeKey(_ id: Int64) async {
        do { try await presenter.revokeKey(id: id) } catch {}
    }

    func dismissFresh() {
        presenter.dismissFresh()
    }

    func confirmReauth(_ password: String, _ code: String) async {
        do { try await presenter.confirmReauth(password: password, code: code) } catch {}
    }

    func cancelReauth() {
        presenter.cancelReauth()
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}
