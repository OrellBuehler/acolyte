import SwiftUI
import UIKit

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: opacity
        )
    }

    static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(
            uiColor: UIColor { traits in
                let hex = traits.userInterfaceStyle == .dark ? dark : light
                return UIColor(
                    red: CGFloat((hex >> 16) & 0xFF) / 255,
                    green: CGFloat((hex >> 8) & 0xFF) / 255,
                    blue: CGFloat(hex & 0xFF) / 255,
                    alpha: 1
                )
            }
        )
    }
}

enum Theme {
    static let background = Color.adaptive(light: 0xFFFFFF, dark: 0x05080D)
    static let surface = Color.adaptive(light: 0xFFFFFF, dark: 0x11151C)
    static let surfaceRaised = Color.adaptive(light: 0xF2F2F4, dark: 0x191E26)
    static let surfaceHigh = Color.adaptive(light: 0xE9E9EC, dark: 0x232932)
    static let line = Color.adaptive(light: 0xE3E3E6, dark: 0x1D2128)
    static let lineStrong = Color.adaptive(light: 0xD1D1D6, dark: 0x353B44)
    static let text = Color.adaptive(light: 0x1D1D1F, dark: 0xF5F5F7)
    static let textMuted = Color.adaptive(light: 0x6E6E73, dark: 0xA1A1A6)
    static let textFaint = Color.adaptive(light: 0x8E8E93, dark: 0x7C7C80)

    static let accent = Color.adaptive(light: 0xCF2C23, dark: 0xE5352B)
    static let accentText = Color.adaptive(light: 0xCF2C23, dark: 0xFF453A)
    static let accentFill = Color(hex: 0xD42D24)
    static let onAccent = Color.white

    static let ok = Color.adaptive(light: 0x248A3D, dark: 0x30D158)
    static let okText = Color.adaptive(light: 0x1A6E2E, dark: 0x30D158)
    static let wait = Color.adaptive(light: 0xB25000, dark: 0xFFB340)
    static let waitText = Color.adaptive(light: 0x8F4000, dark: 0xFFB340)
    static let fail = Color.adaptive(light: 0xD70015, dark: 0xFF6961)
    static let failText = Color.adaptive(light: 0xB3000F, dark: 0xFF6961)
    static let info = Color.adaptive(light: 0x0071E3, dark: 0x409CFF)
    static let infoText = Color.adaptive(light: 0x0068D1, dark: 0x409CFF)

    static let onArt = Color.white
    static let artScrim = Color.black.opacity(0.55)

    static let markHave = Color(hex: 0x1F9D48)
    static let markDownloading = Color(hex: 0x4F46E5)
    static let markAsked = Color(hex: 0xF0A020)
    static let markAskedGlyph = Color(hex: 0x2A1A00)
    static let markNo = Color(hex: 0xD92D20)
    static let markRing = Color.black.opacity(0.22)
    static let artScrimStrong = Color.black.opacity(0.86)
    static let artFill = Color.white.opacity(0.16)
    static let tileHues: [Color] = [
        Color(hex: 0xD0202A), Color(hex: 0xB3123E), Color(hex: 0x8E1F4F), Color(hex: 0x6D2A8A),
        Color(hex: 0xC2410C), Color(hex: 0x9F1D35), Color(hex: 0x3F6A8A), Color(hex: 0xB91C1C),
        Color(hex: 0x7A1D6B), Color(hex: 0x2C7A7B), Color(hex: 0xC0263F), Color(hex: 0xA3402B),
    ]

    static func dotColor(_ tone: ToneKind) -> Color {
        switch tone {
        case .ok: ok
        case .wait: wait
        case .fail: fail
        case .info: info
        case .idle: textFaint
        }
    }

    static func textColor(_ tone: ToneKind) -> Color {
        switch tone {
        case .ok: okText
        case .fail: failText
        default: text
        }
    }

    enum Radius {
        static let button: CGFloat = 10
        static let card: CGFloat = 12
        static let poster: CGFloat = 12
        static let panel: CGFloat = 16
        static let sheet: CGFloat = 20
    }

    enum Spacing {
        static let xSmall: CGFloat = 4
        static let small: CGFloat = 8
        static let medium: CGFloat = 12
        static let large: CGFloat = 16
        static let xLarge: CGFloat = 24
        static let screen: CGFloat = 16
    }
}
