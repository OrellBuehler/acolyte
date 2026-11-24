import Foundation
import Observation

@MainActor
@Observable
final class SettingsHomeStore {
    private(set) var info: SettingsHomeInfo = .initial

    private let driver = SDKClient.shared.makeSettingsHome()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func search(_ query: String, texts: [SettingsSearchText]) -> [SettingsPageID] {
        driver.search(query, texts: texts).compactMap { SettingsPageID(rawValue: $0) }
    }
}
