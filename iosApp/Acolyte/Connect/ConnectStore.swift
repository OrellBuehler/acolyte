import Foundation
import Observation

@MainActor
@Observable
final class ConnectStore {
    private(set) var info: SignInInfo = .initial
    private(set) var plexWaiting = false
    private(set) var resetCode = ""

    private let driver = SDKClient.shared.makeSignIn()
    @ObservationIgnored private var plexTask: Task<Void, Never>?
    @ObservationIgnored private var plexRun = 0

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func connect(_ address: String) {
        let driver = self.driver
        Task { await driver.connect(address) }
    }

    func signIn(name: String, password: String, way: SignInWayKind) {
        let driver = self.driver
        Task { await driver.signIn(name: name, password: password, way: way) }
    }

    func useApiKey(_ key: String) {
        let driver = self.driver
        Task { await driver.useApiKey(key) }
    }

    func verify(_ code: String) {
        let driver = self.driver
        Task { await driver.verify(code) }
    }

    func signInWithPlex() {
        guard plexTask == nil else { return }
        let driver = self.driver
        plexRun += 1
        let run = plexRun
        plexWaiting = true
        plexTask = Task { [weak self] in
            await driver.signInWithPlex()
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

    func createOwner(name: String, displayName: String, password: String, picture: Data?) {
        let driver = self.driver
        Task { await driver.createOwner(name: name, displayName: displayName, password: password, picture: picture) }
    }

    func startReset() {
        resetCode = ""
        driver.startReset()
    }

    func makeResetCode() {
        let driver = self.driver
        Task { await driver.makeResetCode() }
    }

    func checkResetCode(_ code: String) {
        let driver = self.driver
        resetCode = code
        Task { await driver.checkResetCode(code) }
    }

    func finishReset(name: String, password: String) {
        let driver = self.driver
        let code = resetCode
        Task { await driver.finishReset(code: code, name: name, password: password) }
    }

    func clearError() {
        driver.clearError()
    }

    func parseInvite(_ text: String) -> InviteTargetInfo? {
        SDKClient.shared.parseInvite(text)
    }

    func back() {
        cancelPlex()
        driver.back()
    }

    func close() {
        cancelPlex()
        driver.close()
    }
}
