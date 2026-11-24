@preconcurrency import shared
import Foundation

enum MusicFormat {
    static func image(_ path: String, size: Int = 500) -> String {
        MusicRules.shared.image(path: path, size: Int32(size))
    }

    static func shortCount(_ n: Int64) -> String {
        MusicRules.shared.shortCount(n: n)
    }

    static func lengthText(_ ms: Int64) -> String {
        MusicRules.shared.lengthText(ms: ms)
    }

    static func sizeValue(_ bytes: Int64) -> String {
        MusicRules.shared.sizeValue(bytes: bytes)
    }

    static func sizeIsGb(_ bytes: Int64) -> Bool {
        MusicRules.shared.sizeIsGb(bytes: bytes)
    }

    static func bandHue(_ hue: Int) -> Int {
        Int(MusicRules.shared.bandHue(hue: Int32(hue)))
    }

    static func tileColor(_ key: String) -> String {
        MusicRules.shared.tileColor(key: key)
    }
}
