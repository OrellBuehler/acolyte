import Foundation

struct SettingsReauthInfo: Hashable, Sendable {
    let needed: Bool
    let busy: Bool
    let error: ErrorInfo?

    static let idle = SettingsReauthInfo(needed: false, busy: false, error: nil)
}
