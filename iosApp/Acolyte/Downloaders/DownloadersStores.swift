import Foundation
import Observation

@MainActor
@Observable
final class DownloadersStore {
    private(set) var info: DownloadersInfo = .initial

    private let driver = SDKClient.shared.makeDownloaders()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func startAdd() { driver.startAdd() }

    func startChange(_ id: Int64) {
        let driver = self.driver
        Task { await driver.startChange(id) }
    }

    func cancelForm() { driver.cancelForm() }

    func pickKind(_ wire: String) { driver.pickKind(wire) }

    func setName(_ value: String) { driver.setName(value) }

    func setUrl(_ value: String) { driver.setUrl(value) }

    func setUsername(_ value: String) { driver.setUsername(value) }

    func setSecret(_ value: String) { driver.setSecret(value) }

    func setLogin(_ on: Bool) { driver.setLogin(on) }

    func setCategory(_ value: String) { driver.setCategory(value) }

    func setPriority(_ value: String) { driver.setPriority(value) }

    func setKindCategory(_ field: DownloaderKindField, _ value: String) { driver.setKindCategory(field, value) }

    func addTag(_ value: String) { driver.addTag(value) }

    func removeTag(_ value: String) { driver.removeTag(value) }

    func setCategoryAfterImport(_ value: String) { driver.setCategoryAfterImport(value) }

    func setOwnOnly(_ on: Bool) { driver.setOwnOnly(on) }

    func addPathPair() { driver.addPathPair() }

    func setPathTheirs(_ index: Int, _ value: String) { driver.setPathTheirs(index, value) }

    func setPathOurs(_ index: Int, _ value: String) { driver.setPathOurs(index, value) }

    func setPathLink(_ index: Int, _ on: Bool) { driver.setPathLink(index, on) }

    func removePathPair(_ index: Int) { driver.removePathPair(index) }

    func setAllowLocalNetwork(_ on: Bool) { driver.setAllowLocalNetwork(on) }

    func setWritable(_ on: Bool) { driver.setWritable(on) }

    func setSeedRemove(_ on: Bool) { driver.setSeedRemove(on) }

    func setSeedRatio(_ value: String) { driver.setSeedRatio(value) }

    func setSeedHours(_ value: String) { driver.setSeedHours(value) }

    func setSeedDeleteFiles(_ on: Bool) { driver.setSeedDeleteFiles(on) }

    func setClearFinished(_ on: Bool) { driver.setClearFinished(on) }

    func setSabUser(_ value: String) { driver.setSabUser(value) }

    func setSabPass(_ value: String) { driver.setSabPass(value) }

    func fetchKey() {
        let driver = self.driver
        Task { await driver.fetchKey() }
    }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }

    func test(_ id: Int64) {
        let driver = self.driver
        Task { await driver.test(id) }
    }

    func testForm() {
        let driver = self.driver
        Task { await driver.testForm() }
    }

    func pause(_ id: Int64) {
        let driver = self.driver
        Task { await driver.pause(id) }
    }

    func resume(_ id: Int64) {
        let driver = self.driver
        Task { await driver.resume(id) }
    }

    func remove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.remove(id) }
    }

    func makeCategory(_ id: Int64, _ name: String) {
        let driver = self.driver
        Task { await driver.makeCategory(id, name) }
    }

    func abandon(_ id: Int64, _ attemptId: Int64) {
        let driver = self.driver
        Task { await driver.abandon(id, attemptId) }
    }
}

@MainActor
@Observable
final class DownloaderRulesStore {
    private(set) var info: DownloaderRulesInfo = .initial

    private let driver = SDKClient.shared.makeDownloaderRules()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func setSwitch(_ kind: DownloaderSwitchKind, _ on: Bool) { driver.setSwitch(kind, on) }

    func setNumber(_ kind: DownloaderNumberKind, _ text: String) { driver.setNumber(kind, text) }

    func setDeadRule(_ on: Bool) { driver.setDeadRule(on) }

    func discard() { driver.discard() }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }
}
