import Foundation
import Observation

@MainActor
@Observable
final class SourcesStore {
    private(set) var info: SourcesInfo = .initial

    private let driver = SDKClient.shared.makeSources()

    var sites: [SourceSiteInfo] { driver.sites() }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func reloadQuietly() async { await driver.reloadQuietly() }

    func followLive() async { await driver.followLive() }

    func startAdd() { driver.startAdd() }

    func startChange(_ id: Int64) { driver.startChange(id) }

    func cancelEdit() { driver.cancelEdit() }

    func settled(_ form: SourceFormInfo) -> SourceFormInfo { driver.settled(form) }

    func withFormat(_ form: SourceFormInfo, _ format: String) -> SourceFormInfo { driver.withFormat(form, format) }

    func withSite(_ form: SourceFormInfo, _ site: String) -> SourceFormInfo { driver.withSite(form, site) }

    func withKind(_ form: SourceFormInfo, _ kind: String, _ on: Bool) -> SourceFormInfo { driver.withKind(form, kind, on) }

    func withTagText(_ form: SourceFormInfo, _ text: String) -> SourceFormInfo { driver.withTagText(form, text) }

    func save(_ form: SourceFormInfo) {
        let driver = self.driver
        Task { await driver.save(form) }
    }

    func test(_ id: Int64) {
        let driver = self.driver
        Task { await driver.test(id) }
    }

    func tryNow(_ id: Int64) {
        let driver = self.driver
        Task { await driver.tryNow(id) }
    }

    func setPaused(_ id: Int64, _ paused: Bool) {
        let driver = self.driver
        Task { await driver.setPaused(id, paused) }
    }

    func remove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.remove(id) }
    }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }
}

@MainActor
@Observable
final class BridgeStore {
    private(set) var info: BridgesInfo = .initial

    let reauth: ReauthStore

    private let driver: BridgeDriver

    init() {
        let driver = SDKClient.shared.makeBridges()
        self.driver = driver
        reauth = ReauthStore(driver: driver.reauth)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func startConnect(_ kind: String) { driver.startConnect(kind) }

    func startChange(_ id: Int64) { driver.startChange(id) }

    func cancelForm() { driver.cancelForm() }

    func test(_ draft: BridgeDraftInfo) {
        let driver = self.driver
        Task { await driver.test(draft) }
    }

    func link(_ draft: BridgeDraftInfo) {
        let driver = self.driver
        Task { await driver.link(draft) }
    }

    func sync(_ id: Int64) {
        let driver = self.driver
        Task { await driver.sync(id) }
    }

    func unlink(_ id: Int64, keepSources: Bool) async -> Bool {
        await driver.unlink(id, keepSources: keepSources)
    }

    func clearRemoveError() { driver.clearRemoveError() }

    func dismissKeys() { driver.dismissKeys() }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }
}

@MainActor
@Observable
final class SourceCheckStore {
    private(set) var info: SourceCheckInfo = .initial

    private let driver = SDKClient.shared.makeSourceCheck()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func run(_ kind: String, _ title: String) {
        let driver = self.driver
        Task { await driver.run(kind, title) }
    }

    func titleAfterKind(_ old: String, _ title: String, _ new: String) -> String {
        driver.titleAfterKind(old, title, new)
    }

    func sample(_ kind: String) -> String { driver.sample(kind) }
}

@MainActor
@Observable
final class SourceHealthStore {
    private(set) var info: SourceHealthInfo = .initial

    private let driver: SourceHealthDriver

    init(id: Int64) {
        driver = SDKClient.shared.makeSourceHealth(id)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func setDays(_ days: Int) {
        let driver = self.driver
        Task { await driver.setDays(days) }
    }

    func tryNow() {
        let driver = self.driver
        Task { await driver.tryNow() }
    }
}
