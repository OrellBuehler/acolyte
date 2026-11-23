@preconcurrency import shared
import Foundation

final class SignInDriver: @unchecked Sendable {
    private let flow: SignInFlow

    init(flow: SignInFlow) {
        self.flow = flow
    }

    func watch(deliver: @MainActor @Sendable (SignInInfo) -> Void) async {
        for await state in flow.state {
            let mapped = Mapping.signIn(state)
            await deliver(mapped)
        }
    }

    func connect(_ address: String) async {
        do {
            try await flow.connect(address: address)
        } catch {}
    }

    func signIn(name: String, password: String, way: SignInWayKind) async {
        do {
            try await flow.signIn(name: name, password: password, way: Mapping.kotlinWay(way))
        } catch {}
    }

    func verify(_ code: String) async {
        do {
            try await flow.verify(code: code)
        } catch {}
    }

    func useApiKey(_ key: String) async {
        do {
            try await flow.useApiKey(key: key)
        } catch {}
    }

    func back() {
        flow.back()
    }

    func close() {
        flow.close()
    }
}

final class HealthDriver: @unchecked Sendable {
    private let presenter: HealthPresenter

    init(presenter: HealthPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (HealthInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.health(state)
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

    func runChecks() async {
        do {
            try await presenter.runChecks()
        } catch {}
    }

    func followLive() async {
        do {
            try await presenter.watch()
        } catch {}
    }
}
