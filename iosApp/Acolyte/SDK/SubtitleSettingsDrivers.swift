@preconcurrency import shared
import Foundation

final class SubtitleSettingsDriver: @unchecked Sendable {
    private let presenter: SettingsSubtitlePresenter

    init(presenter: SettingsSubtitlePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SubtitleCfgInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.subtitleSettings(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func followLive() async {
        do { try await presenter.watch() } catch {}
    }

    func setNumber(_ key: String, _ text: String) {
        presenter.setNumber(key: key, text: text)
    }

    func setFlag(_ key: String, _ on: Bool) {
        presenter.setFlag(key: key, on: on)
    }

    func setHiSuffix(_ suffix: String) {
        presenter.setHiSuffix(suffix: suffix)
    }

    func addLanguage() {
        presenter.addLanguage()
    }

    func removeLanguage(_ index: Int) {
        presenter.removeLanguage(index: Int32(index))
    }

    func setWantLanguage(_ index: Int, _ code: String) {
        presenter.setWantLanguage(index: Int32(index), code: code)
    }

    func setWantType(_ index: Int, _ type: String) {
        presenter.setWantType(index: Int32(index), type: type)
    }

    func setWantForced(_ index: Int, _ forced: Bool) {
        presenter.setWantForced(index: Int32(index), forced: forced)
    }

    func setWantTranslateFrom(_ index: Int, _ code: String) {
        presenter.setWantTranslateFrom(index: Int32(index), code: code)
    }

    func pickCleanLanguage(_ code: String) {
        presenter.pickCleanLanguage(code: code)
    }

    func setStep(_ key: String, _ on: Bool) {
        presenter.setStep(key: key, on: on)
    }

    func removeCleanLanguage() {
        presenter.removeCleanLanguage()
    }

    func discard() {
        presenter.discard()
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func setTranslatorKind(_ kind: String) {
        presenter.setTranslatorKind(kind: kind)
    }

    func setTranslatorUrl(_ url: String) {
        presenter.setTranslatorUrl(url: url)
    }

    func setTranslatorModel(_ model: String) {
        presenter.setTranslatorModel(model: model)
    }

    func setTranslatorKey(_ key: String) {
        presenter.setTranslatorKey(key: key)
    }

    func setTranslatorClearKey(_ clear: Bool) {
        presenter.setTranslatorClearKey(clear: clear)
    }

    func setTranslatorBuiltInAuto(_ on: Bool) {
        presenter.setTranslatorBuiltInAuto(on: on)
    }

    func saveTranslator() async {
        do { try await presenter.saveTranslator() } catch {}
    }

    func testTranslator() async {
        do { try await presenter.testTranslator() } catch {}
    }

    func startAdd() {
        presenter.startAdd()
    }

    func startChange(_ id: Int64) {
        presenter.startChange(id: id)
    }

    func pickKind(_ kind: String) {
        presenter.pickKind(kind: kind)
    }

    func cancelForm() {
        presenter.cancelForm()
    }

    func setFormName(_ name: String) {
        presenter.setFormName(name: name)
    }

    func setFormUrl(_ url: String) {
        presenter.setFormUrl(url: url)
    }

    func setFormNumber(_ key: String, _ text: String) {
        presenter.setFormNumber(key: key, text: text)
    }

    func setFormAccount(_ field: String, _ text: String) {
        presenter.setFormAccount(field: field, text: text)
    }

    func setFormFlag(_ key: String, _ on: Bool) {
        presenter.setFormFlag(key: key, on: on)
    }

    func submitForm() async {
        do { try await presenter.submitForm() } catch {}
    }

    func testSite(_ id: Int64) async {
        do { try await presenter.testSite(id: id) } catch {}
    }

    func togglePaused(_ id: Int64) async {
        do { try await presenter.togglePaused(id: id) } catch {}
    }

    func removeSite(_ id: Int64) async {
        do { try await presenter.removeSite(id: id) } catch {}
    }

    func cleanAll() async {
        do { try await presenter.cleanAll() } catch {}
    }
}
