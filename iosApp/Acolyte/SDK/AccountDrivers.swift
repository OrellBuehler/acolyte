@preconcurrency import shared
import Foundation

protocol ReauthHosting: AnyObject {
    var reauth: ReauthDriver { get }
}

final class ReauthDriver: @unchecked Sendable {
    private let gate: ReauthGate

    init(gate: ReauthGate) {
        self.gate = gate
    }

    func watch(deliver: @MainActor @Sendable (ReauthInfo) -> Void) async {
        for await state in gate.state {
            let mapped = Mapping.reauth(state)
            await deliver(mapped)
        }
    }

    func confirm(password: String, code: String) async {
        do {
            try await gate.confirm(password: password, code: code)
        } catch {}
    }

    func cancel() {
        gate.cancel()
    }
}

extension SignInDriver {
    func createOwner(name: String, displayName: String, password: String, picture: Data?) async {
        let bytes = picture.map { Mapping.byteArray($0) }
        do {
            try await flow.createOwner(name: name, displayName: displayName, password: password, picture: bytes)
        } catch {}
    }

    func signInWithPlex() async {
        do {
            try await flow.signInWithPlex()
        } catch {}
    }

    func startReset() {
        flow.startReset()
    }

    func makeResetCode() async {
        do {
            try await flow.makeResetCode()
        } catch {}
    }

    func checkResetCode(_ code: String) async {
        do {
            try await flow.checkResetCode(code: code)
        } catch {}
    }

    func finishReset(code: String, name: String, password: String) async {
        do {
            try await flow.finishReset(code: code, name: name, password: password)
        } catch {}
    }

    func clearError() {
        flow.clearError()
    }
}

final class InviteDriver: @unchecked Sendable {
    private let flow: InviteFlow

    init(flow: InviteFlow) {
        self.flow = flow
    }

    func watch(deliver: @MainActor @Sendable (InviteInfo) -> Void) async {
        for await state in flow.state {
            let mapped = Mapping.invite(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await flow.load()
        } catch {}
    }

    func join(name: String, password: String) async {
        do {
            try await flow.join(name: name, password: password)
        } catch {}
    }

    func joinMediaServer(way: SignInWayKind, name: String, password: String) async {
        do {
            try await flow.joinMediaServer(way: Mapping.kotlinWay(way), name: name, password: password)
        } catch {}
    }

    func joinPlex() async {
        do {
            try await flow.joinPlex()
        } catch {}
    }

    func signOutAndJoin() async {
        do {
            try await flow.signOutAndJoin()
        } catch {}
    }

    func close() {
        flow.close()
    }
}

final class AccountWizardDriver: @unchecked Sendable, ReauthHosting {
    private let wizard: AccountWizardPresenter
    private let security: AccountSecurityPresenter

    let reauth: ReauthDriver
    let securityReauth: ReauthDriver

    init(wizard: AccountWizardPresenter, security: AccountSecurityPresenter) {
        self.wizard = wizard
        self.security = security
        reauth = ReauthDriver(gate: wizard.reauth)
        securityReauth = ReauthDriver(gate: security.reauth)
    }

    func watch(deliver: @MainActor @Sendable (WizardInfo) -> Void) async {
        for await state in wizard.state {
            let mapped = Mapping.wizard(state)
            await deliver(mapped)
        }
    }

    func watchLinks(deliver: @MainActor @Sendable (WizardLinksInfo) -> Void) async {
        for await state in security.state {
            let mapped = Mapping.wizardLinks(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await security.load()
        } catch {}
    }

    func discover() async {
        do {
            try await wizard.discover()
        } catch {}
    }

    func linkServer(_ provider: WizardProviderKind, address: String, name: String, password: String) async {
        do {
            try await wizard.linkServer(kind: provider.rawValue, address: address, name: name, password: password)
            try await security.refresh()
        } catch {}
    }

    func linkPlex() async {
        do {
            try await security.linkPlex()
        } catch {}
    }

    func unlinkPlex() async {
        do {
            try await security.unlinkPlex()
        } catch {}
    }

    func unlinkMedia(_ provider: WizardProviderKind) async {
        do {
            try await security.unlinkMedia(kind: provider.rawValue)
        } catch {}
    }

    func clearLinkError() {
        security.consume()
    }

    func next() {
        wizard.next()
    }

    func back() {
        wizard.back()
    }

    func finish(skipped: Bool) async {
        do {
            try await wizard.finish(skipped: skipped)
        } catch {}
    }
}
