@preconcurrency import shared
import Foundation

extension Mapping {
    static func settingsReauth(_ s: SettingsReauthState) -> SettingsReauthInfo {
        SettingsReauthInfo(needed: s.needed, busy: s.busy, error: error(s.error))
    }
}
