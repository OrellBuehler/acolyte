#if DEBUG
import Foundation

@MainActor
enum TVScreenshotLaunch {
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

    static func startTab(for account: AccountInfo) -> SectionID {
        guard let target = pending, let section = SectionID(rawValue: target.lowercased()),
              TVSections.tabs(for: account).contains(section) else { return .home }
        return section
    }

    static func open(_ navigator: TVNavigator) async {
        guard let target = pending else { return }
        pending = nil
        let parts = target.split(separator: ":").map(String.init)
        if parts.count == 3, parts[0] == "title" {
            try? await Task.sleep(for: .milliseconds(600))
            navigator.open(.title(kind: parts[1], tmdb: parts[2]))
        } else if let section = SectionID(rawValue: target.lowercased()),
                  !TVSections.tabs(for: navigator.account).contains(section) {
            try? await Task.sleep(for: .milliseconds(600))
            navigator.open(section)
        }
    }
}
#endif
