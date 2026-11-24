@preconcurrency import shared
import Foundation

final class SettingsMoveDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: SettingsMovePresenter

    let reauth: ReauthDriver

    init(presenter: SettingsMovePresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func apps() -> [MoveAppInfo] {
        MoveVocabulary.shared.apps.map { MoveAppInfo(kind: $0.kind, name: $0.name, port: Int($0.port)) }
    }

    func placeholder(_ kind: String) -> String {
        MoveVocabulary.shared.placeholder(kind: kind)
    }

    func watch(deliver: @MainActor @Sendable (MoveInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.move(state)
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

    func enter(_ step: MoveStepKind) {
        presenter.enter(step: Mapping.kotlinMoveStep(step))
    }

    func goBackToReads() {
        presenter.goBackToReads()
    }

    func togglePick(_ id: Int64, on: Bool) {
        presenter.togglePick(id: id, on: on)
    }

    func forget(_ id: Int64) async {
        do { _ = try await presenter.forget(id: id) } catch {}
    }

    func removeMove(_ id: Int64) async {
        do { try await presenter.removeMove(id: id) } catch {}
    }

    func openMove(_ id: Int64) async {
        do { try await presenter.openMove(id: id) } catch {}
    }

    func resetSheet() {
        presenter.resetSheet()
    }

    func clearFound() {
        presenter.clearFound()
    }

    func clearProbe() {
        presenter.clearProbe()
    }

    func probe(_ form: MoveFormInfo) async {
        do { try await presenter.probe(form: Mapping.kotlinMoveForm(form)) } catch {}
    }

    func find(host: String, form: MoveFormInfo) async {
        do { try await presenter.find(host: host, form: Mapping.kotlinMoveForm(form)) } catch {}
    }

    func addLinked(_ form: MoveFormInfo) async {
        do { try await presenter.addLinked(form: Mapping.kotlinMoveForm(form)) } catch {}
    }

    func read(_ form: MoveFormInfo) async -> Bool {
        do {
            let ok: Any = try await presenter.read(form: Mapping.kotlinMoveForm(form))
            return Self.flag(ok)
        } catch {
            return false
        }
    }

    func pickFolder(_ oldPath: String, kind: MovePickChoiceKind, path: String) {
        presenter.pickFolder(oldPath: oldPath, kind: Mapping.kotlinMovePick(kind), path: path)
    }

    func typeOther(_ oldPath: String, text: String) {
        presenter.typeOther(oldPath: oldPath, text: text)
    }

    func commitFolders() {
        presenter.commitFolders()
    }

    func addRule(_ from: String) {
        presenter.addRule(from: from)
    }

    func editRule(_ index: Int, from: String, to: String) {
        presenter.editRule(index: Int32(index), from: from, to: to)
    }

    func removeRule(_ index: Int) {
        presenter.removeRule(index: Int32(index))
    }

    func setGroup(_ id: String, on: Bool) {
        presenter.setGroup(id: id, on: on)
    }

    func setPerson(_ key: String, choice: MovePersonChoiceKind?) {
        presenter.setPerson(key: key, choice: Mapping.kotlinMoveChoice(choice))
    }

    func setNames(_ names: MoveNamesKind) {
        presenter.setNames(choice: Mapping.kotlinMoveNames(names))
    }

    func setNoFormats(_ noFormats: Bool) {
        presenter.setNoFormats(noFormats: noFormats)
    }

    func chooseOnce(_ key: String, choice: MovePersonChoiceKind) {
        guard let kotlin = Mapping.kotlinMoveChoice(choice) else { return }
        presenter.chooseOnce(key: key, choice: kotlin)
    }

    func importNow() async {
        do { try await presenter.importNow() } catch {}
    }

    func closeDialog() {
        presenter.closeDialog()
    }

    func askUndo() {
        presenter.askUndo()
    }

    func start() async {
        do { try await presenter.start() } catch {}
    }

    func undo() async {
        do { try await presenter.undo() } catch {}
    }

    func done() async {
        do { try await presenter.done() } catch {}
    }

    func chooseAgain() {
        presenter.chooseAgain()
    }

    private static func flag(_ value: Any) -> Bool {
        if let plain = value as? Bool { return plain }
        return (value as? NSNumber)?.boolValue ?? false
    }

    func reportText() async -> (name: String, text: String)? {
        do {
            guard let text = try await presenter.reportText() else { return nil }
            return (presenter.reportName(), text)
        } catch {
            return nil
        }
    }

    func items(_ source: MoveListSource, id: String) -> MoveListInfo {
        let list = presenter.items(kind: Mapping.kotlinMoveList(source), id: id)
        return MoveListInfo(rows: list.rows.map { Mapping.moveItem($0) })
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    func dismissProblem() {
        presenter.dismissProblem()
    }
}
