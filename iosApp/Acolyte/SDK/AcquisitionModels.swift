import Foundation

enum AcquisitionPageID: String, Hashable, Sendable, Identifiable {
    case sources
    case downloaders
    case pickRules = "pickrules"
    case naming
    case subtitles
    case search
    case move

    var id: String { rawValue }
}
