import Foundation
import Observation

@MainActor
@Observable
final class InviteStore {
    private(set) var info: InviteInfo = .initial
    private(set) var plexWaiting = false

    @ObservationIgnored private let driver: InviteDriver
    @ObservationIgnored private var plexTask: Task<Void, Never>?
    @ObservationIgnored private var plexRun = 0

    init(target: InviteTargetInfo) {
        driver = SDKClient.shared.makeInvite(target)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async {
        await driver.load()
    }

    func reload() {
        let driver = self.driver
        Task { await driver.load() }
    }

    func join(name: String, password: String) {
        let driver = self.driver
        Task { await driver.join(name: name, password: password) }
    }

    func joinMediaServer(way: SignInWayKind, name: String, password: String) {
        let driver = self.driver
        Task { await driver.joinMediaServer(way: way, name: name, password: password) }
    }

    func joinPlex() {
        guard plexTask == nil else { return }
        let driver = self.driver
        plexRun += 1
        let run = plexRun
        plexWaiting = true
        plexTask = Task { [weak self] in
            await driver.joinPlex()
            guard let self, self.plexRun == run else { return }
            self.plexTask = nil
            self.plexWaiting = false
        }
    }

    func cancelPlex() {
        plexRun += 1
        plexTask?.cancel()
        plexTask = nil
        plexWaiting = false
    }

    func signOutAndJoin() {
        let driver = self.driver
        Task { await driver.signOutAndJoin() }
    }

    func close() {
        cancelPlex()
        driver.close()
    }
}
