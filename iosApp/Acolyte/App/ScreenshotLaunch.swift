#if DEBUG
import Foundation

@MainActor
enum ScreenshotLaunch {
    private static let defaults = UserDefaults.standard
    private static var pending = defaults.string(forKey: "acolyte_screenshot_open")

    static func signIn() async {
        guard let server = defaults.string(forKey: "acolyte_screenshot_server") else { return }
        let user = defaults.string(forKey: "acolyte_screenshot_user") ?? ""
        let password = defaults.string(forKey: "acolyte_screenshot_password") ?? ""
        let driver = SDKClient.shared.makeSignIn()
        await driver.connect(server)
        await driver.signIn(name: user, password: password, way: .password)
    }

    static func open(_ navigator: Navigator) {
        guard let target = pending else { return }
        pending = nil
        let parts = target.split(separator: ":").map(String.init)
        if parts.count == 3, parts[0] == "title" {
            navigator.open(.title(kind: parts[1], tmdb: parts[2]))
        } else if let section = SectionID(rawValue: target.lowercased()) {
            navigator.open(section)
        }
    }
}
#endif
