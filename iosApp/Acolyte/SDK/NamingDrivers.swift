@preconcurrency import shared
import Foundation

final class NamingDriver: @unchecked Sendable {
    private let presenter: SettingsNamingPresenter

    init(presenter: SettingsNamingPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (NamingInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.naming(state)
            await deliver(mapped)
        }
    }

    func words(_ wire: String, _ query: String) -> [NamingWordInfo] {
        Mapping.namingWords(presenter.state.value.words(wire: wire, query: query))
    }

    func text(_ wire: String) -> String {
        presenter.state.value.text(wire: wire)
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func followExample() async {
        do { try await presenter.followExample() } catch {}
    }

    func setText(_ wire: String, _ text: String) {
        presenter.setText(wire: wire, text: text)
    }

    func insertWord(_ wire: String, _ word: String) {
        let length = Int32(presenter.state.value.text(wire: wire).utf16.count)
        _ = presenter.insertWord(wire: wire, word: word, start: length, end: length)
    }

    func setSwitch(_ wire: String, _ on: Bool) {
        presenter.setSwitch(wire: wire, on: on)
    }

    func setChoice(_ wire: String, _ value: String) {
        presenter.setChoice(wire: wire, value: value)
    }

    func setNumber(_ wire: String, _ text: String) {
        presenter.setNumber(wire: wire, text: text)
    }

    func applyStyle(_ wire: String) {
        presenter.applyStyle(wire: wire)
    }

    func discard() {
        presenter.discard()
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func cleanHolding() async {
        do { try await presenter.cleanHolding() } catch {}
    }
}

final class SearchSettingsDriver: @unchecked Sendable {
    private let presenter: SettingsSearchPresenter

    init(presenter: SettingsSearchPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SearchSettingsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.searchSettings(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func setSwitch(_ wire: String, _ on: Bool) {
        presenter.setSwitch(wire: wire, on: on)
    }

    func setChoice(_ wire: String, _ value: String) {
        presenter.setChoice(wire: wire, value: value)
    }

    func setNumber(_ wire: String, _ text: String) {
        presenter.setNumber(wire: wire, text: text)
    }

    func discard() {
        presenter.discard()
    }

    func save() async {
        do { try await presenter.save() } catch {}
    }

    func checkFeeds() async {
        do { try await presenter.checkFeeds() } catch {}
    }

    func findBetter() async {
        do { try await presenter.findBetter() } catch {}
    }
}
