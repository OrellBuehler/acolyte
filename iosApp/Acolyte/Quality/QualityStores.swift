import SwiftUI
import Observation

@MainActor
@Observable
final class QualityListStore {
    private(set) var info: QualityListInfo = .initial

    private let driver = SDKClient.shared.makeSettingsPickRules()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }

    func askRemove(_ id: Int64) { driver.askRemove(id) }

    func cancelRemove() { driver.cancelRemove() }

    func confirmRemove() {
        let driver = self.driver
        Task { await driver.confirmRemove() }
    }

    func openTrait(_ id: Int64) { driver.openTrait(id) }

    func closeTrait() { driver.closeTrait() }

    func setTraitName(_ text: String) { driver.setTraitName(text) }

    func setTraitMatch(_ match: String) { driver.setTraitMatch(match) }

    func setTraitTerms(_ text: String) { driver.setTraitTerms(text) }

    func setTraitOver(_ text: String) { driver.setTraitOver(text) }

    func setTraitUpTo(_ text: String) { driver.setTraitUpTo(text) }

    func saveTrait() {
        let driver = self.driver
        Task { await driver.saveTrait() }
    }

    func askRemoveTrait(_ id: Int64) { driver.askRemoveTrait(id) }

    func cancelRemoveTrait() { driver.cancelRemoveTrait() }

    func confirmRemoveTrait() {
        let driver = self.driver
        Task { await driver.confirmRemoveTrait() }
    }

    func setCheckName(_ text: String) { driver.setCheckName(text) }

    func setCheckRules(_ id: Int64) { driver.setCheckRules(id) }

    func setCheckSize(_ text: String) { driver.setCheckSize(text) }

    func runCheck() {
        let driver = self.driver
        Task { await driver.runCheck() }
    }
}

@MainActor
@Observable
final class QualityEditStore {
    private(set) var info: QualityEditInfo = .initial

    private let driver: QualityEditDriver

    init(id: Int64, from: Int64) {
        driver = SDKClient.shared.makeSettingsPickRuleEdit(id: id, from: from)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }

    func discard() { driver.discard() }

    func setName(_ text: String) { driver.setName(text) }

    func setKind(_ kind: String, _ on: Bool) { driver.setKind(kind, on) }

    func setStepAllowed(_ quality: String, _ on: Bool) { driver.setStepAllowed(quality, on) }

    func moveStep(_ quality: String, up: Bool) { driver.moveStep(quality, up: up) }

    func setGoodEnoughAt(_ quality: String) { driver.setGoodEnoughAt(quality) }

    func setGoodEnoughPoints(_ text: String) { driver.setGoodEnoughPoints(text) }

    func setTraitKey(_ index: Int, _ key: String) { driver.setTraitKey(index, key) }

    func setTraitPoints(_ index: Int, _ text: String) { driver.setTraitPoints(index, text) }

    func addTrait() { driver.addTrait() }

    func removeTrait(_ index: Int) { driver.removeTrait(index) }

    func setOrder(_ order: String) { driver.setOrder(order) }

    func setUpgradeMinPoints(_ text: String) { driver.setUpgradeMinPoints(text) }

    func setMinPoints(_ text: String) { driver.setMinPoints(text) }

    func setProperMode(_ mode: String) { driver.setProperMode(mode) }

    func setProperAge(_ text: String) { driver.setProperAge(text) }

    func addMustHave(_ id: String) { driver.addMustHave(id) }

    func removeMustHave(_ id: String) { driver.removeMustHave(id) }

    func addNever(_ id: String) { driver.addNever(id) }

    func removeNever(_ id: String) { driver.removeNever(id) }

    func addBlockedGroup(_ raw: String) { driver.addBlockedGroup(raw) }

    func removeBlockedGroup(_ group: String) { driver.removeBlockedGroup(group) }

    func toggleDevice(_ id: String) { driver.toggleDevice(id) }

    func setSizeMin(_ text: String) { driver.setSizeMin(text) }

    func setSizeMax(_ text: String) { driver.setSizeMax(text) }

    func addStepSize(_ step: String) { driver.addStepSize(step) }

    func setStepSize(_ step: String, _ which: String, _ text: String) { driver.setStepSize(step, which, text) }

    func removeStepSize(_ step: String) { driver.removeStepSize(step) }

    func setReplaceUnderMin(_ on: Bool) { driver.setReplaceUnderMin(on) }

    func setPrefer(_ prefer: String) { driver.setPrefer(prefer) }

    func setWaitTorrent(_ text: String) { driver.setWaitTorrent(text) }

    func setWaitUsenet(_ text: String) { driver.setWaitUsenet(text) }

    func setSkipWait(_ on: Bool) { driver.setSkipWait(on) }

    func addTag(_ raw: String) { driver.addTag(raw) }

    func removeTag(_ tag: String) { driver.removeTag(tag) }

    func toggleLanguage(_ code: String) { driver.toggleLanguage(code) }

    func toggleFrench(_ version: String) { driver.toggleFrench(version) }

    func setOriginalOk(_ on: Bool) { driver.setOriginalOk(on) }

    func setLanguageWait(_ text: String) { driver.setLanguageWait(text) }

    func save() {
        let driver = self.driver
        Task { await driver.save() }
    }

    func setTrashApp(_ app: String) {
        let driver = self.driver
        Task { await driver.setTrashApp(app) }
    }

    func loadTrash() {
        let driver = self.driver
        Task { await driver.loadTrash() }
    }

    func setTrashPick(_ id: String) { driver.setTrashPick(id) }

    func applyTrash() {
        let driver = self.driver
        Task { await driver.applyTrash() }
    }

    func syncAgain() {
        let driver = self.driver
        Task { await driver.syncAgain() }
    }
}
