@preconcurrency import shared
import Foundation

final class MyProfileDriver: @unchecked Sendable {
    private let presenter: AccountProfilePresenter

    init(presenter: AccountProfilePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MyProfileInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.myProfile(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func consume() {
        presenter.consume()
    }

    func saveName(_ name: String) async {
        do {
            try await presenter.saveName(name: name)
        } catch {}
    }

    func saveLanguage(_ code: String) async {
        do {
            try await presenter.saveLanguage(code: code)
        } catch {}
    }

    func uploadPicture(_ data: Data) async {
        do {
            try await presenter.uploadPicture(bytes: Mapping.byteArray(data), type: "image/jpeg")
        } catch {}
    }

    func removePicture() async {
        do {
            try await presenter.removePicture()
        } catch {}
    }

    func saveAlerts(email: String, discordId: String, newEpisodes: Bool) async {
        do {
            try await presenter.saveAlerts(email: email, discordId: discordId, newEpisodes: newEpisodes)
        } catch {}
    }

    func unfollow(_ id: String) async {
        guard let follow = presenter.state.value.follows.first(where: { $0.unfollow == id }) else { return }
        do {
            try await presenter.unfollow(follow: follow)
        } catch {}
    }
}

final class MySecurityDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: AccountSecurityPresenter

    let reauth: ReauthDriver

    init(presenter: AccountSecurityPresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func watch(deliver: @MainActor @Sendable (MySecurityInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.mySecurity(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func consume() {
        presenter.consume()
    }

    func dismissBackupCodes() {
        presenter.dismissBackupCodes()
    }

    func cancelTwoFactor() {
        presenter.cancelTwoFactor()
    }

    func changePassword(current: String, new: String, confirm: String) async {
        do {
            try await presenter.changePassword(current: current, new: new, confirm: confirm)
        } catch {}
    }

    func startTwoFactor() async {
        do {
            try await presenter.startTwoFactor()
        } catch {}
    }

    func enableTwoFactor(_ code: String) async {
        do {
            try await presenter.enableTwoFactor(code: code)
        } catch {}
    }

    func disableTwoFactor(_ code: String) async {
        do {
            try await presenter.disableTwoFactor(code: code)
        } catch {}
    }

    func renewBackupCodes(_ code: String) async {
        do {
            try await presenter.renewBackupCodes(code: code)
        } catch {}
    }

    func linkPlex() async {
        do {
            try await presenter.linkPlex()
        } catch {}
    }

    func unlinkPlex() async {
        do {
            try await presenter.unlinkPlex()
        } catch {}
    }

    func linkMedia(serverId: Int64, name: String, password: String) async {
        do {
            try await presenter.linkMedia(serverId: serverId, name: name, password: password)
        } catch {}
    }

    func unlinkMedia(_ kind: String) async {
        do {
            try await presenter.unlinkMedia(kind: kind)
        } catch {}
    }

    func unlinkSso(_ id: String) async {
        guard let link = presenter.state.value.sso.first(where: { $0.kind + "|" + $0.issuer == id }) else { return }
        do {
            try await presenter.unlinkSso(link: link)
        } catch {}
    }
}

final class MyDevicesDriver: @unchecked Sendable {
    private let presenter: AccountDevicesPresenter

    init(presenter: AccountDevicesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (MyDevicesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.myDevices(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func consume() {
        presenter.consume()
    }

    func end(_ id: String) async {
        guard let session = presenter.state.value.sessions.first(where: { $0.id == id }) else { return }
        do {
            try await presenter.end(session: session)
        } catch {}
    }

    func endAll() async {
        do {
            try await presenter.endAll()
        } catch {}
    }
}

final class PeopleDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: PeoplePresenter

    let reauth: ReauthDriver

    init(presenter: PeoplePresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func watch(deliver: @MainActor @Sendable (PeopleListInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.peopleList(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func consume() {
        presenter.consume()
    }

    func dismissInvite() {
        presenter.dismissInvite()
    }

    func dismissNoWayIn() {
        presenter.dismissNoWayIn()
    }

    func makeInvite(_ draft: PeopleInviteDraftInfo) async {
        let kotlinDraft = PeopleInviteDraft(
            admin: draft.admin,
            perms: draft.perms,
            rating: draft.rating,
            note: draft.note,
            days: Int32(draft.days),
            maxUses: Int32(draft.maxUses)
        )
        do {
            try await presenter.makeInvite(draft: kotlinDraft)
        } catch {}
    }

    func revokeInvite(_ id: String) async {
        guard let invite = presenter.state.value.invites.first(where: { $0.id == id }) else { return }
        do {
            try await presenter.revokeInvite(invite: invite)
        } catch {}
    }

    func save(userId: Int64, admin: Bool, perms: [String], rating: String) async {
        guard let user = find(userId) else { return }
        do {
            try await presenter.save(user: user, admin: admin, perms: perms, rating: rating)
        } catch {}
    }

    func turnOff(_ userId: Int64) async {
        guard let user = find(userId) else { return }
        do {
            try await presenter.turnOff(user: user)
        } catch {}
    }

    func turnOn(_ userId: Int64) async {
        guard let user = find(userId) else { return }
        do {
            try await presenter.turnOn(user: user)
        } catch {}
    }

    func setPassword(userId: Int64, password: String) async {
        guard let user = find(userId) else { return }
        do {
            try await presenter.setPassword(user: user, password: password)
        } catch {}
    }

    func resetTwoFactor(_ userId: Int64) async {
        guard let user = find(userId) else { return }
        do {
            try await presenter.resetTwoFactor(user: user)
        } catch {}
    }

    func delete(_ userId: Int64) async {
        guard let user = find(userId) else { return }
        do {
            try await presenter.delete(user: user)
        } catch {}
    }

    func unlinkSso(userId: Int64, link: MySsoLinkInfo) async {
        guard let user = find(userId), let found = user.ssoLinks.first(where: { $0.kind == link.kind && $0.issuer == link.issuer }) else {
            return
        }
        do {
            try await presenter.unlinkSso(user: user, link: found, confirmed: false)
        } catch {}
    }

    func unlinkSsoConfirmed(userId: Int64, link: MySsoLinkInfo) async {
        guard let user = find(userId), let found = user.ssoLinks.first(where: { $0.kind == link.kind && $0.issuer == link.issuer }) else {
            return
        }
        do {
            try await presenter.unlinkSso(user: user, link: found, confirmed: true)
        } catch {}
    }

    func loadServers() async {
        do {
            try await presenter.loadServers()
        } catch {}
    }

    func linkMedia(userId: Int64, serverId: Int64, accountId: String) async {
        guard let user = find(userId) else { return }
        do {
            try await presenter.linkMedia(user: user, serverId: serverId, accountId: accountId)
        } catch {}
    }

    func unlinkMedia(userId: Int64, kind: String) async {
        guard let user = find(userId) else { return }
        do {
            try await presenter.unlinkMedia(user: user, kind: kind)
        } catch {}
    }

    private func find(_ id: Int64) -> TitleUser? {
        presenter.state.value.users.first { $0.id == id }
    }
}

final class AiConnectDriver: @unchecked Sendable, ReauthHosting {
    private let presenter: AiConnectPresenter

    let reauth: ReauthDriver

    init(presenter: AiConnectPresenter) {
        self.presenter = presenter
        reauth = ReauthDriver(gate: presenter.reauth)
    }

    func watch(deliver: @MainActor @Sendable (AiConnectInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.aiConnect(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func allow() async {
        do {
            try await presenter.allow()
        } catch {}
    }
}
