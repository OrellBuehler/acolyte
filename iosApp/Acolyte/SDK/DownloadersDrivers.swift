@preconcurrency import shared
import Foundation

final class DownloadersDriver: @unchecked Sendable {
    private let presenter: SettingsDownloadersPresenter

    init(presenter: SettingsDownloadersPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (DownloadersInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.downloaders(state)
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

    func startAdd() { presenter.startAdd() }

    func startChange(_ id: Int64) async {
        do { try await presenter.startChange(id: id) } catch {}
    }

    func cancelForm() { presenter.cancelForm() }

    func pickKind(_ wire: String) { presenter.pickKind(wire: wire) }

    func setName(_ value: String) { presenter.setName(value: value) }

    func setUrl(_ value: String) { presenter.setUrl(value: value) }

    func setUsername(_ value: String) { presenter.setUsername(value: value) }

    func setSecret(_ value: String) { presenter.setSecret(value: value) }

    func setLogin(_ on: Bool) { presenter.setLogin(on: on) }

    func setCategory(_ value: String) { presenter.setCategory(value: value) }

    func setPriority(_ value: String) { presenter.setPriority(value: value) }

    func setKindCategory(_ field: DownloaderKindField, _ value: String) {
        presenter.setKindCategory(field: Mapping.kotlinKindField(field), value: value)
    }

    func addTag(_ value: String) { presenter.addTag(raw: value) }

    func removeTag(_ value: String) { presenter.removeTag(tag: value) }

    func setCategoryAfterImport(_ value: String) { presenter.setCategoryAfterImport(value: value) }

    func setOwnOnly(_ on: Bool) { presenter.setOwnOnly(on: on) }

    func addPathPair() { presenter.addPathPair() }

    func setPathTheirs(_ index: Int, _ value: String) { presenter.setPathTheirs(index: Int32(index), value: value) }

    func setPathOurs(_ index: Int, _ value: String) { presenter.setPathOurs(index: Int32(index), value: value) }

    func setPathLink(_ index: Int, _ on: Bool) { presenter.setPathLink(index: Int32(index), on: on) }

    func removePathPair(_ index: Int) { presenter.removePathPair(index: Int32(index)) }

    func setAllowLocalNetwork(_ on: Bool) { presenter.setAllowLocalNetwork(on: on) }

    func setWritable(_ on: Bool) { presenter.setWritable(on: on) }

    func setSeedRemove(_ on: Bool) { presenter.setSeedRemove(on: on) }

    func setSeedRatio(_ value: String) { presenter.setSeedRatio(value: value) }

    func setSeedHours(_ value: String) { presenter.setSeedHours(value: value) }

    func setSeedDeleteFiles(_ on: Bool) { presenter.setSeedDeleteFiles(on: on) }

    func setClearFinished(_ on: Bool) { presenter.setClearFinished(on: on) }

    func setSabUser(_ value: String) { presenter.setSabUser(value: value) }

    func setSabPass(_ value: String) { presenter.setSabPass(value: value) }

    func fetchKey() async {
        do { try await presenter.fetchKey() } catch {}
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func test(_ id: Int64) async {
        do { try await presenter.test(id: id) } catch {}
    }

    func testForm() async {
        do { try await presenter.testForm() } catch {}
    }

    func pause(_ id: Int64) async {
        do { try await presenter.pause(id: id) } catch {}
    }

    func resume(_ id: Int64) async {
        do { try await presenter.resume(id: id) } catch {}
    }

    func remove(_ id: Int64) async {
        do { try await presenter.remove(id: id) } catch {}
    }

    func makeCategory(_ id: Int64, _ name: String) async {
        do { try await presenter.makeCategory(id: id, name: name) } catch {}
    }

    func abandon(_ id: Int64, _ attemptId: Int64) async {
        do { try await presenter.abandon(id: id, attemptId: attemptId) } catch {}
    }
}

final class DownloaderRulesDriver: @unchecked Sendable {
    private let presenter: SettingsDownloaderRulesPresenter

    init(presenter: SettingsDownloaderRulesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (DownloaderRulesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.downloaderRules(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func setSwitch(_ kind: DownloaderSwitchKind, _ on: Bool) {
        presenter.setSwitch(flag: Mapping.kotlinSwitch(kind), on: on)
    }

    func setNumber(_ kind: DownloaderNumberKind, _ text: String) {
        presenter.setNumber(kind: Mapping.kotlinNumber(kind), raw: text)
    }

    func setDeadRule(_ on: Bool) { presenter.setDeadRule(on: on) }

    func discard() { presenter.discard() }

    func save() async {
        do { try await presenter.save() } catch {}
    }
}
