import Foundation
import Observation

@MainActor
@Observable
final class PeopleStore {
    private(set) var info: PeopleListInfo = .initial
    private(set) var successSerial = 0
    private(set) var detailOpen = false

    let reauth: ReauthStore
    let notices = NoticeState()

    private let driver: PeopleDriver

    init() {
        let driver = SDKClient.shared.makePeople()
        self.driver = driver
        reauth = ReauthStore(driver: driver.reauth)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.apply(value)
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func setDetailOpen(_ open: Bool) {
        detailOpen = open
    }

    func clearProblems() {
        driver.consume()
    }

    func dismissInvite() {
        driver.dismissInvite()
    }

    func dismissNoWayIn() {
        driver.dismissNoWayIn()
    }

    func makeInvite(_ draft: PeopleInviteDraftInfo) {
        let driver = self.driver
        Task { await driver.makeInvite(draft) }
    }

    func revokeInvite(_ id: String) {
        let driver = self.driver
        Task { await driver.revokeInvite(id) }
    }

    func save(userId: Int64, admin: Bool, perms: [String], rating: String) {
        let driver = self.driver
        Task { await driver.save(userId: userId, admin: admin, perms: perms, rating: rating) }
    }

    func turnOff(_ userId: Int64) {
        let driver = self.driver
        Task { await driver.turnOff(userId) }
    }

    func turnOn(_ userId: Int64) {
        let driver = self.driver
        Task { await driver.turnOn(userId) }
    }

    func setPassword(userId: Int64, password: String) {
        let driver = self.driver
        Task { await driver.setPassword(userId: userId, password: password) }
    }

    func resetTwoFactor(_ userId: Int64) {
        let driver = self.driver
        Task { await driver.resetTwoFactor(userId) }
    }

    func delete(_ userId: Int64) {
        let driver = self.driver
        Task { await driver.delete(userId) }
    }

    func unlinkSso(userId: Int64, link: MySsoLinkInfo) {
        let driver = self.driver
        Task { await driver.unlinkSso(userId: userId, link: link) }
    }

    func unlinkSsoConfirmed(userId: Int64, link: MySsoLinkInfo) {
        let driver = self.driver
        Task { await driver.unlinkSsoConfirmed(userId: userId, link: link) }
    }

    func loadServers() {
        let driver = self.driver
        Task { await driver.loadServers() }
    }

    func linkMedia(userId: Int64, serverId: Int64, accountId: String) {
        let driver = self.driver
        Task { await driver.linkMedia(userId: userId, serverId: serverId, accountId: accountId) }
    }

    func unlinkMedia(userId: Int64, kind: String) {
        let driver = self.driver
        Task { await driver.unlinkMedia(userId: userId, kind: kind) }
    }

    private func apply(_ value: PeopleListInfo) {
        info = value
        guard let notice = value.notice else { return }
        notices.show(Self.text(notice, name: value.noticeName))
        successSerial += 1
        driver.consume()
    }

    nonisolated static func text(_ notice: PeopleNoticeKind, name: String) -> String {
        switch notice {
        case .inviteRevoked: L10n.tr("people.notice.invite.revoked")
        case .saved: L10n.format("people.notice.saved", name)
        case .turnedOff: L10n.format("people.notice.turned.off", name)
        case .turnedOn: L10n.format("people.notice.turned.on", name)
        case .passwordSet: L10n.format("people.notice.password.set", name)
        case .twoFactorReset: L10n.format("people.notice.two.factor.reset", name)
        case .deleted: L10n.format("people.notice.deleted", name)
        case .ssoUnlinked: L10n.format("people.notice.sso.unlinked", name)
        case .mediaLinked: L10n.format("people.notice.media.linked", name)
        case .mediaUnlinked: L10n.format("people.notice.media.unlinked", name)
        }
    }

    nonisolated static func problemText(_ problem: PeopleProblemKind, minimum: Int, unlimited: Bool) -> String {
        switch problem {
        case .invalidDays: L10n.tr(unlimited ? "people.problem.days.unlimited" : "people.problem.days")
        case .invalidUses: L10n.tr(unlimited ? "people.problem.uses.unlimited" : "people.problem.uses")
        case .passwordShort: L10n.format("myaccount.password.short", minimum)
        }
    }
}
