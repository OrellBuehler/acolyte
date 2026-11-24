@preconcurrency import shared
import Foundation

final class SettingsSignInDriver: @unchecked Sendable {
    private let presenter: SettingsSignInPresenter

    init(presenter: SettingsSignInPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (SettingsSignInInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.settingsSignIn(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do { try await presenter.load() } catch {}
    }

    func refresh() async {
        do { try await presenter.refresh() } catch {}
    }

    func confirmReauth(_ password: String, _ code: String) async {
        do { try await presenter.confirmReauth(password: password, code: code) } catch {}
    }

    func cancelReauth() {
        presenter.cancelReauth()
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    func setMinLength(_ text: String) {
        presenter.setMinLength(text: text)
    }

    func saveMinLength() async {
        do { try await presenter.saveMinLength() } catch {}
    }

    func setRequireTwoFactor(_ on: Bool) async {
        do { try await presenter.setRequireTwoFactor(on: on) } catch {}
    }

    func setForgotLink(_ on: Bool) async {
        do { try await presenter.setForgotLink(on: on) } catch {}
    }

    func setWayOn(_ id: SettingsSignInWayID, _ on: Bool) {
        presenter.setWayOn(kind: Mapping.kotlinSignInWay(id), on: on)
    }

    func setWayUrl(_ id: SettingsSignInWayID, _ text: String) {
        presenter.setWayUrl(kind: Mapping.kotlinSignInWay(id), text: text)
    }

    func setWayServerId(_ id: SettingsSignInWayID, _ text: String) {
        presenter.setWayServerId(kind: Mapping.kotlinSignInWay(id), text: text)
    }

    func setWayJoin(_ id: SettingsSignInWayID, _ join: SettingsSignInJoinKind) {
        presenter.setWayJoin(kind: Mapping.kotlinSignInWay(id), join: Mapping.kotlinSignInJoin(join))
    }

    func setWayAllow(_ id: SettingsSignInWayID, _ text: String) {
        presenter.setWayAllow(kind: Mapping.kotlinSignInWay(id), text: text)
    }

    func testWay(_ id: SettingsSignInWayID) async {
        do { try await presenter.testWay(kind: Mapping.kotlinSignInWay(id)) } catch {}
    }

    func saveWay(_ id: SettingsSignInWayID, confirmed: Bool) async {
        do { try await presenter.saveWay(kind: Mapping.kotlinSignInWay(id), confirmed: confirmed) } catch {}
    }

    func dismissEveryone() {
        presenter.dismissEveryone()
    }

    func linkPlex() async {
        do { try await presenter.linkPlex() } catch {}
    }

    func cancelLink() {
        presenter.cancelLink()
    }

    func unlinkPlex() async {
        do { try await presenter.unlinkPlex() } catch {}
    }

    func openImport(_ id: SettingsSignInWayID) async {
        do { try await presenter.openImport(kind: Mapping.kotlinSignInWay(id)) } catch {}
    }

    func closeImport() {
        presenter.closeImport()
    }

    func previewImport(_ name: String, _ password: String) async {
        do { try await presenter.previewImport(name: name, password: password) } catch {}
    }

    func togglePerson(_ id: String) {
        presenter.togglePerson(id: id)
    }

    func toggleKid(_ id: String) {
        presenter.toggleKid(id: id)
    }

    func dismissConfirmAdults() {
        presenter.dismissConfirmAdults()
    }

    func runImport(confirmAdults: Bool) async {
        do { try await presenter.runImport(confirmAdults: confirmAdults) } catch {}
    }

    func loadSso() async {
        do { try await presenter.loadSso() } catch {}
    }

    func setSsoOn(_ on: Bool) {
        presenter.setSsoOn(on: on)
    }

    func setSsoIssuer(_ text: String) {
        presenter.setSsoIssuer(text: text)
    }

    func setSsoClientId(_ text: String) {
        presenter.setSsoClientId(text: text)
    }

    func setSsoSecret(_ text: String) {
        presenter.setSsoSecret(text: text)
    }

    func setSsoPublicClient(_ on: Bool) {
        presenter.setSsoPublicClient(on: on)
    }

    func setSsoAllowJoin(_ on: Bool) {
        presenter.setSsoAllowJoin(on: on)
    }

    func setSsoGroupsClaim(_ text: String) {
        presenter.setSsoGroupsClaim(text: text)
    }

    func setSsoGroupRoles(_ text: String) {
        presenter.setSsoGroupRoles(text: text)
    }

    func setSsoConfirmEveryone(_ on: Bool) {
        presenter.setSsoConfirmEveryone(on: on)
    }

    func testSso() async {
        do { try await presenter.testSso() } catch {}
    }

    func saveSso(_ password: String, _ code: String) async {
        do { try await presenter.saveSso(password: password, code: code) } catch {}
    }

    func setSkip(_ on: Bool) async {
        do { try await presenter.setSkip(on: on) } catch {}
    }

    func setLocalNetworks(_ text: String) {
        presenter.setLocalNetworks(text: text)
    }

    func setLocalProxies(_ text: String) {
        presenter.setLocalProxies(text: text)
    }

    func pickPerson(_ id: Int64) {
        presenter.pickPerson(id: id)
    }

    func addHomeRanges() {
        presenter.addHomeRanges()
    }

    func setTailscale(_ on: Bool) {
        presenter.setTailscale(on: on)
    }

    func saveLocal() async {
        do { try await presenter.saveLocal() } catch {}
    }

    func retryPeople() async {
        do { try await presenter.retryPeople() } catch {}
    }

    func setHosts(_ text: String) {
        presenter.setHosts(text: text)
    }

    func saveHosts() async {
        do { try await presenter.saveHosts() } catch {}
    }
}
