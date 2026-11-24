@preconcurrency import shared
import Foundation

final class QualityListDriver: @unchecked Sendable {
    private let presenter: SettingsPickRulesPresenter

    init(presenter: SettingsPickRulesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (QualityListInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.qualityList(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    func askRemove(_ id: Int64) {
        presenter.askRemove(id: id)
    }

    func cancelRemove() {
        presenter.cancelRemove()
    }

    func confirmRemove() async {
        do { try await presenter.confirmRemove() } catch {}
    }

    func openTrait(_ id: Int64) {
        presenter.openTrait(id: id)
    }

    func closeTrait() {
        presenter.closeTrait()
    }

    func setTraitName(_ text: String) {
        presenter.setTraitName(text: text)
    }

    func setTraitMatch(_ match: String) {
        presenter.setTraitMatch(match: match)
    }

    func setTraitTerms(_ text: String) {
        presenter.setTraitTerms(text: text)
    }

    func setTraitOver(_ text: String) {
        presenter.setTraitOver(text: text)
    }

    func setTraitUpTo(_ text: String) {
        presenter.setTraitUpTo(text: text)
    }

    func saveTrait() async {
        do { try await presenter.saveTrait() } catch {}
    }

    func askRemoveTrait(_ id: Int64) {
        presenter.askRemoveTrait(id: id)
    }

    func cancelRemoveTrait() {
        presenter.cancelRemoveTrait()
    }

    func confirmRemoveTrait() async {
        do { try await presenter.confirmRemoveTrait() } catch {}
    }

    func setCheckName(_ text: String) {
        presenter.setCheckName(text: text)
    }

    func setCheckRules(_ id: Int64) {
        presenter.setCheckRules(id: id)
    }

    func setCheckSize(_ text: String) {
        presenter.setCheckSize(text: text)
    }

    func runCheck() async {
        do { try await presenter.runCheck() } catch {}
    }
}

final class QualityEditDriver: @unchecked Sendable {
    private let presenter: SettingsPickRuleEditPresenter

    init(presenter: SettingsPickRuleEditPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (QualityEditInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.qualityEdit(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    func discard() {
        presenter.discard()
    }

    func setName(_ text: String) {
        presenter.setName(text: text)
    }

    func setKind(_ kind: String, _ on: Bool) {
        presenter.setKind(kind: kind, on: on)
    }

    func setStepAllowed(_ quality: String, _ on: Bool) {
        presenter.setStepAllowed(quality: quality, on: on)
    }

    func moveStep(_ quality: String, up: Bool) {
        presenter.moveStep(quality: quality, up: up)
    }

    func setGoodEnoughAt(_ quality: String) {
        presenter.setGoodEnoughAt(quality: quality)
    }

    func setGoodEnoughPoints(_ text: String) {
        presenter.setGoodEnoughPoints(text: text)
    }

    func setTraitKey(_ index: Int, _ key: String) {
        presenter.setTraitKey(index: Int32(index), key: key)
    }

    func setTraitPoints(_ index: Int, _ text: String) {
        presenter.setTraitPoints(index: Int32(index), text: text)
    }

    func addTrait() {
        presenter.addTrait()
    }

    func removeTrait(_ index: Int) {
        presenter.removeTrait(index: Int32(index))
    }

    func setOrder(_ order: String) {
        presenter.setOrder(order: order)
    }

    func setUpgradeMinPoints(_ text: String) {
        presenter.setUpgradeMinPoints(text: text)
    }

    func setMinPoints(_ text: String) {
        presenter.setMinPoints(text: text)
    }

    func setProperMode(_ mode: String) {
        presenter.setProperMode(mode: mode)
    }

    func setProperAge(_ text: String) {
        presenter.setProperAge(text: text)
    }

    func addMustHave(_ id: String) {
        presenter.addMustHave(id: id)
    }

    func removeMustHave(_ id: String) {
        presenter.removeMustHave(id: id)
    }

    func addNever(_ id: String) {
        presenter.addNever(id: id)
    }

    func removeNever(_ id: String) {
        presenter.removeNever(id: id)
    }

    func addBlockedGroup(_ raw: String) {
        presenter.addBlockedGroup(raw: raw)
    }

    func removeBlockedGroup(_ group: String) {
        presenter.removeBlockedGroup(group: group)
    }

    func toggleDevice(_ id: String) {
        presenter.toggleDevice(id: id)
    }

    func setSizeMin(_ text: String) {
        presenter.setSizeMin(text: text)
    }

    func setSizeMax(_ text: String) {
        presenter.setSizeMax(text: text)
    }

    func addStepSize(_ step: String) {
        presenter.addStepSize(step: step)
    }

    func setStepSize(_ step: String, _ which: String, _ text: String) {
        presenter.setStepSize(step: step, which: which, text: text)
    }

    func removeStepSize(_ step: String) {
        presenter.removeStepSize(step: step)
    }

    func setReplaceUnderMin(_ on: Bool) {
        presenter.setReplaceUnderMin(on: on)
    }

    func setPrefer(_ prefer: String) {
        presenter.setPrefer(prefer: prefer)
    }

    func setWaitTorrent(_ text: String) {
        presenter.setWaitTorrent(text: text)
    }

    func setWaitUsenet(_ text: String) {
        presenter.setWaitUsenet(text: text)
    }

    func setSkipWait(_ on: Bool) {
        presenter.setSkipWait(on: on)
    }

    func addTag(_ raw: String) {
        presenter.addTag(raw: raw)
    }

    func removeTag(_ tag: String) {
        presenter.removeTag(tag: tag)
    }

    func toggleLanguage(_ code: String) {
        presenter.toggleLanguage(code: code)
    }

    func toggleFrench(_ version: String) {
        presenter.toggleFrench(version: version)
    }

    func setOriginalOk(_ on: Bool) {
        presenter.setOriginalOk(on: on)
    }

    func setLanguageWait(_ text: String) {
        presenter.setLanguageWait(text: text)
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func setTrashApp(_ app: String) async {
        do { try await presenter.setTrashApp(app: app) } catch {}
    }

    func loadTrash() async {
        do { try await presenter.loadTrash() } catch {}
    }

    func setTrashPick(_ id: String) {
        presenter.setTrashPick(id: id)
    }

    func applyTrash() async {
        do { try await presenter.applyTrash() } catch {}
    }

    func syncAgain() async {
        do { try await presenter.syncAgain() } catch {}
    }
}

enum QualityChoices {
    static let languages: [String] = SettingsPickRuleChoices.shared.languages
    static let frenchVersions: [String] = SettingsPickRuleChoices.shared.frenchVersions
    static let devices: [String] = SettingsPickRuleChoices.shared.devices
    static let kinds: [String] = SettingsPickRuleChoices.shared.kinds
    static let orders: [String] = SettingsPickRuleChoices.shared.orders
    static let properModes: [String] = SettingsPickRuleChoices.shared.properModes
    static let prefers: [String] = SettingsPickRuleChoices.shared.prefers
    static let trashApps: [String] = SettingsPickRuleChoices.shared.trashApps
}
