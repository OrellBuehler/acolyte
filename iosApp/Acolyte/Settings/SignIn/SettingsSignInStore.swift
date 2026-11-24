import Foundation
import Observation

@MainActor
@Observable
final class SettingsSignInStore {
    private(set) var info: SettingsSignInInfo = .initial

    @ObservationIgnored private let driver = SDKClient.shared.makeSettingsSignIn()
    @ObservationIgnored private var linkTask: Task<Void, Never>?

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async { await driver.load() }

    func refresh() async { await driver.refresh() }

    func confirmReauth(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.confirmReauth(password, code) }
    }

    func cancelReauth() { driver.cancelReauth() }

    func dismissNotice(_ seq: Int) { driver.dismissNotice(seq) }

    func setMinLength(_ text: String) { driver.setMinLength(text) }

    func saveMinLength() {
        let driver = self.driver
        Task { await driver.saveMinLength() }
    }

    func setRequireTwoFactor(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setRequireTwoFactor(on) }
    }

    func setForgotLink(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setForgotLink(on) }
    }

    func setWayOn(_ id: SettingsSignInWayID, _ on: Bool) { driver.setWayOn(id, on) }

    func setWayUrl(_ id: SettingsSignInWayID, _ text: String) { driver.setWayUrl(id, text) }

    func setWayServerId(_ id: SettingsSignInWayID, _ text: String) { driver.setWayServerId(id, text) }

    func setWayJoin(_ id: SettingsSignInWayID, _ join: SettingsSignInJoinKind) { driver.setWayJoin(id, join) }

    func setWayAllow(_ id: SettingsSignInWayID, _ text: String) { driver.setWayAllow(id, text) }

    func testWay(_ id: SettingsSignInWayID) {
        let driver = self.driver
        Task { await driver.testWay(id) }
    }

    func saveWay(_ id: SettingsSignInWayID, confirmed: Bool) {
        let driver = self.driver
        Task { await driver.saveWay(id, confirmed: confirmed) }
    }

    func dismissEveryone() { driver.dismissEveryone() }

    func linkPlex() {
        let driver = self.driver
        linkTask?.cancel()
        linkTask = Task { await driver.linkPlex() }
    }

    func stopLinking() {
        driver.cancelLink()
        linkTask?.cancel()
        linkTask = nil
    }

    func unlinkPlex() {
        let driver = self.driver
        Task { await driver.unlinkPlex() }
    }

    func openImport(_ id: SettingsSignInWayID) {
        let driver = self.driver
        Task { await driver.openImport(id) }
    }

    func closeImport() { driver.closeImport() }

    func previewImport(_ name: String, _ password: String) {
        let driver = self.driver
        Task { await driver.previewImport(name, password) }
    }

    func togglePerson(_ id: String) { driver.togglePerson(id) }

    func toggleKid(_ id: String) { driver.toggleKid(id) }

    func dismissConfirmAdults() { driver.dismissConfirmAdults() }

    func runImport(confirmAdults: Bool) {
        let driver = self.driver
        Task { await driver.runImport(confirmAdults: confirmAdults) }
    }

    func loadSso() {
        let driver = self.driver
        Task { await driver.loadSso() }
    }

    func setSsoOn(_ on: Bool) { driver.setSsoOn(on) }

    func setSsoIssuer(_ text: String) { driver.setSsoIssuer(text) }

    func setSsoClientId(_ text: String) { driver.setSsoClientId(text) }

    func setSsoSecret(_ text: String) { driver.setSsoSecret(text) }

    func setSsoPublicClient(_ on: Bool) { driver.setSsoPublicClient(on) }

    func setSsoAllowJoin(_ on: Bool) { driver.setSsoAllowJoin(on) }

    func setSsoGroupsClaim(_ text: String) { driver.setSsoGroupsClaim(text) }

    func setSsoGroupRoles(_ text: String) { driver.setSsoGroupRoles(text) }

    func setSsoConfirmEveryone(_ on: Bool) { driver.setSsoConfirmEveryone(on) }

    func testSso() {
        let driver = self.driver
        Task { await driver.testSso() }
    }

    func saveSso(_ password: String, _ code: String) {
        let driver = self.driver
        Task { await driver.saveSso(password, code) }
    }

    func setSkip(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setSkip(on) }
    }

    func setLocalNetworks(_ text: String) { driver.setLocalNetworks(text) }

    func setLocalProxies(_ text: String) { driver.setLocalProxies(text) }

    func pickPerson(_ id: Int64) { driver.pickPerson(id) }

    func addHomeRanges() { driver.addHomeRanges() }

    func setTailscale(_ on: Bool) { driver.setTailscale(on) }

    func saveLocal() {
        let driver = self.driver
        Task { await driver.saveLocal() }
    }

    func retryPeople() {
        let driver = self.driver
        Task { await driver.retryPeople() }
    }

    func setHosts(_ text: String) { driver.setHosts(text) }

    func saveHosts() {
        let driver = self.driver
        Task { await driver.saveHosts() }
    }
}
