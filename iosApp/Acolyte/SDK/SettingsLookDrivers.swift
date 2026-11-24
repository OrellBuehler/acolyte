import Foundation
@preconcurrency import shared

final class SettingsLookAppearanceDriver: @unchecked Sendable {
    private let presenter: SettingsLookAppearancePresenter

    init(presenter: SettingsLookAppearancePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsLookAppearanceInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsLookAppearance(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func setLanguage(_ code: String) async {
        do { try await presenter.setLanguage(code: code) } catch {}
    }

    func setAutoplayTrailers(_ on: Bool) async {
        do { try await presenter.setAutoplayTrailers(on: on) } catch {}
    }

    func setSignInArt(_ on: Bool) async {
        do { try await presenter.setSignInArt(on: on) } catch {}
    }
}

final class SettingsLookDefaultsDriver: @unchecked Sendable {
    private let presenter: SettingsLookDefaultsPresenter

    init(presenter: SettingsLookDefaultsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsLookDefaultsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsLookDefaults(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func setFolder(show: Bool, _ id: Int64) {
        presenter.setFolder(show: show, id: id)
    }

    func setRules(show: Bool, _ id: Int64) {
        presenter.setRules(show: show, id: id)
    }

    func setKeepUpToDate(show: Bool, _ on: Bool) {
        presenter.setKeepUpToDate(show: show, on: on)
    }

    func setSearchOnAdd(show: Bool, _ on: Bool) {
        presenter.setSearchOnAdd(show: show, on: on)
    }

    func setReleaseGate(_ value: String) {
        presenter.setReleaseGate(value: value)
    }

    func setGateDays(_ text: String) {
        presenter.setGateDays(text: text)
    }

    func setSeasonChoice(_ value: String) {
        presenter.setSeasonChoice(value: value)
    }

    func setNewSeasons(_ value: String) {
        presenter.setNewSeasons(value: value)
    }

    func setEpisodeOrder(_ value: String) {
        presenter.setEpisodeOrder(value: value)
    }

    func setDetailsSource(_ value: String) {
        presenter.setDetailsSource(value: value)
    }

    func discard() {
        presenter.discard()
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }
}

final class SettingsLookMetadataDriver: @unchecked Sendable {
    private let presenter: SettingsLookMetadataPresenter

    init(presenter: SettingsLookMetadataPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsLookMetadataInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsLookMetadata(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func chooseMode(_ mode: String) async {
        do { try await presenter.chooseMode(mode: mode) } catch {}
    }

    func openTvdb() {
        presenter.openTvdb()
    }

    func setKey(_ name: String, _ text: String) {
        presenter.setKey(name: name, text: text)
    }

    func clearKey(_ name: String) {
        presenter.clearKey(name: name)
    }

    func restoreKey(_ name: String) {
        presenter.restoreKey(name: name)
    }

    func testKey(_ name: String) async {
        do { try await presenter.testKey(name: name) } catch {}
    }

    func checkAgain() async {
        do { try await presenter.checkAgain() } catch {}
    }

    func discard() {
        presenter.discard()
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }
}
