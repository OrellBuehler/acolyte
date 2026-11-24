import Foundation

@MainActor
@Observable
final class SubtitleSettingsStore {
    private(set) var info: SubtitleCfgInfo = .initial

    private let driver = SDKClient.shared.makeSubtitleSettings()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func setNumber(_ key: String, _ text: String) { driver.setNumber(key, text) }

    func setFlag(_ key: String, _ on: Bool) { driver.setFlag(key, on) }

    func setHiSuffix(_ suffix: String) { driver.setHiSuffix(suffix) }

    func addLanguage() { driver.addLanguage() }

    func removeLanguage(_ index: Int) { driver.removeLanguage(index) }

    func setWantLanguage(_ index: Int, _ code: String) { driver.setWantLanguage(index, code) }

    func setWantType(_ index: Int, _ type: String) { driver.setWantType(index, type) }

    func setWantForced(_ index: Int, _ forced: Bool) { driver.setWantForced(index, forced) }

    func setWantTranslateFrom(_ index: Int, _ code: String) { driver.setWantTranslateFrom(index, code) }

    func pickCleanLanguage(_ code: String) { driver.pickCleanLanguage(code) }

    func setStep(_ key: String, _ on: Bool) { driver.setStep(key, on) }

    func removeCleanLanguage() { driver.removeCleanLanguage() }

    func discard() { driver.discard() }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }

    func setTranslatorKind(_ kind: String) { driver.setTranslatorKind(kind) }

    func setTranslatorUrl(_ url: String) { driver.setTranslatorUrl(url) }

    func setTranslatorModel(_ model: String) { driver.setTranslatorModel(model) }

    func setTranslatorKey(_ key: String) { driver.setTranslatorKey(key) }

    func setTranslatorClearKey(_ clear: Bool) { driver.setTranslatorClearKey(clear) }

    func setTranslatorBuiltInAuto(_ on: Bool) { driver.setTranslatorBuiltInAuto(on) }

    func saveTranslator() {
        let driver = self.driver
        Task { await driver.saveTranslator() }
    }

    func testTranslator() {
        let driver = self.driver
        Task { await driver.testTranslator() }
    }

    func startAdd() { driver.startAdd() }

    func startChange(_ id: Int64) { driver.startChange(id) }

    func pickKind(_ kind: String) { driver.pickKind(kind) }

    func cancelForm() { driver.cancelForm() }

    func setFormName(_ name: String) { driver.setFormName(name) }

    func setFormUrl(_ url: String) { driver.setFormUrl(url) }

    func setFormNumber(_ key: String, _ text: String) { driver.setFormNumber(key, text) }

    func setFormAccount(_ field: String, _ text: String) { driver.setFormAccount(field, text) }

    func setFormFlag(_ key: String, _ on: Bool) { driver.setFormFlag(key, on) }

    func submitForm() {
        let driver = self.driver
        Task { await driver.submitForm() }
    }

    func testSite(_ id: Int64) {
        let driver = self.driver
        Task { await driver.testSite(id) }
    }

    func togglePaused(_ id: Int64) {
        let driver = self.driver
        Task { await driver.togglePaused(id) }
    }

    func removeSite(_ id: Int64) {
        let driver = self.driver
        Task { await driver.removeSite(id) }
    }

    func cleanAll() {
        let driver = self.driver
        Task { await driver.cleanAll() }
    }
}
