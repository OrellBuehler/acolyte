import SwiftUI
import Observation

@MainActor
@Observable
final class MoveStore {
    private(set) var info: MoveInfo = .initial
    var shareFile: MoveShareFile?

    let apps: [MoveAppInfo]
    let reauth: ReauthStore

    private let driver: SettingsMoveDriver
    private var readTask: Task<Void, Never>?

    init() {
        let driver = SDKClient.shared.makeSettingsMove()
        self.driver = driver
        apps = driver.apps()
        reauth = ReauthStore(driver: driver.reauth)
    }

    func placeholder(_ kind: String) -> String { driver.placeholder(kind) }

    func appName(_ kind: String) -> String { apps.first { $0.kind == kind }?.name ?? kind }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func followLive() async { await driver.followLive() }

    func enter(_ step: MoveStepKind) { driver.enter(step) }

    func goBackToReads() { driver.goBackToReads() }

    func togglePick(_ id: Int64, on: Bool) { driver.togglePick(id, on: on) }

    func forget(_ id: Int64) {
        let driver = self.driver
        Task { await driver.forget(id) }
    }

    func removeMove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.removeMove(id) }
    }

    func openMove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.openMove(id) }
    }

    func resetSheet() { driver.resetSheet() }

    func clearFound() { driver.clearFound() }

    func clearProbe() { driver.clearProbe() }

    func probe(_ form: MoveFormInfo) {
        let driver = self.driver
        Task { await driver.probe(form) }
    }

    func find(host: String, form: MoveFormInfo) {
        let driver = self.driver
        Task { await driver.find(host: host, form: form) }
    }

    func addLinked(_ form: MoveFormInfo) {
        let driver = self.driver
        Task { await driver.addLinked(form) }
    }

    func read(_ form: MoveFormInfo, onDone: @escaping @MainActor (Bool) -> Void) {
        let driver = self.driver
        readTask = Task {
            let ok = await driver.read(form)
            onDone(ok)
        }
    }

    func stopRead() { readTask?.cancel() }

    func pickFolder(_ oldPath: String, kind: MovePickChoiceKind, path: String) { driver.pickFolder(oldPath, kind: kind, path: path) }

    func typeOther(_ oldPath: String, text: String) { driver.typeOther(oldPath, text: text) }

    func commitFolders() { driver.commitFolders() }

    func addRule(_ from: String) { driver.addRule(from) }

    func editRule(_ index: Int, from: String, to: String) { driver.editRule(index, from: from, to: to) }

    func removeRule(_ index: Int) { driver.removeRule(index) }

    func setGroup(_ id: String, on: Bool) { driver.setGroup(id, on: on) }

    func setPerson(_ key: String, choice: MovePersonChoiceKind?) { driver.setPerson(key, choice: choice) }

    func setNames(_ names: MoveNamesKind) { driver.setNames(names) }

    func setNoFormats(_ noFormats: Bool) { driver.setNoFormats(noFormats) }

    func chooseOnce(_ key: String, choice: MovePersonChoiceKind) { driver.chooseOnce(key, choice: choice) }

    func importNow() {
        let driver = self.driver
        Task { await driver.importNow() }
    }

    func closeDialog() { driver.closeDialog() }

    func askUndo() { driver.askUndo() }

    func start() {
        let driver = self.driver
        Task { await driver.start() }
    }

    func undo() {
        let driver = self.driver
        Task { await driver.undo() }
    }

    func done() {
        let driver = self.driver
        Task { await driver.done() }
    }

    func chooseAgain() { driver.chooseAgain() }

    func shareReport() {
        let driver = self.driver
        Task {
            guard let report = await driver.reportText() else { return }
            let url = FileManager.default.temporaryDirectory.appendingPathComponent(report.name)
            do {
                try report.text.write(to: url, atomically: true, encoding: .utf8)
                shareFile = MoveShareFile(url: url)
            } catch {}
        }
    }

    func items(_ source: MoveListSource, id: String) -> MoveListInfo { driver.items(source, id: id) }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }

    func dismissProblem() { driver.dismissProblem() }
}
