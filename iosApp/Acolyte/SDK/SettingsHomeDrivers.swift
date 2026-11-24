@preconcurrency import shared
import Foundation

final class SettingsHomeDriver: @unchecked Sendable {
    private let presenter: SettingsHomePresenter

    init(presenter: SettingsHomePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsHomeInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsHome(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func followLive() async {
        do {
            try await presenter.watch()
        } catch {}
    }

    func search(_ query: String, texts: [SettingsSearchText]) -> [String] {
        let converted = texts.map { SettingsPageText(key: $0.key, label: $0.label, about: $0.about, group: $0.group) }
        return SettingsCatalog.shared.search(query: query, texts: converted)
    }
}
