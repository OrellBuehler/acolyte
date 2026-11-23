import Foundation
import Observation

@MainActor
@Observable
final class ConnectStore {
    private(set) var info: SignInInfo = .initial

    private let driver = SDKClient.shared.makeSignIn()

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

    func back() {
        driver.back()
    }

    func close() {
        driver.close()
    }
}
