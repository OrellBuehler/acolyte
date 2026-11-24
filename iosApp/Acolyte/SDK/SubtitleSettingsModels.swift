import Foundation

enum SubtitleCfgKeys {
    static let numbers = [
        "pass_mark_episode", "pass_mark_movie", "search_hours", "batch", "better_hours", "better_days", "sync_step_ms",
        "sync_from_fps", "sync_to_fps",
    ]
    static let flags = ["better", "adaptive", "use_built_in", "ignore_picture_tracks"]
    static let steps = ["ads", "hi", "tags", "emoji", "ocr", "caps", "spacing", "overlap"]
    static let siteNumbers = ["priority", "gap_ms", "per_window", "window_seconds", "downloads_per_day"]
    static let siteFlags = ["allow_local_network", "machine_made", "enabled"]
    static let accountFields = ["username", "password", "api_key"]
    static let hearingTypes = ["normal", "hi", "either"]
    static let hiSuffixes = ["hi", "cc", "sdh"]
    static let everyLanguage = "*"
    static let ranges: [String: ClosedRange<Int>] = [
        "pass_mark_episode": 0...100, "pass_mark_movie": 0...100, "search_hours": 1...168, "batch": 1...1000,
        "better_hours": 6...168, "better_days": 0...30, "sync_step_ms": 10...60000, "sync_from_fps": 1...120,
        "sync_to_fps": 1...120, "priority": 1...100, "gap_ms": 0...60000, "per_window": 1...10000,
        "window_seconds": 60...86400, "downloads_per_day": 0...100000,
    ]
}

enum SubtitleCfgProblem: String, Hashable, Sendable {
    case tooMany = "toomany"
    case pick
    case twice
}

enum SubtitleCfgCovers: String, Hashable, Sendable {
    case both
    case movies
    case shows
}

enum SubtitleCfgSiteState: String, Hashable, Sendable {
    case needsAccount = "needsaccount"
    case paused
    case resting
    case on
}

enum SubtitleCfgNoteKind: String, Hashable, Sendable {
    case settingsSaved = "settingssaved"
    case translatorSaved = "translatorsaved"
    case siteSaved = "sitesaved"
    case sitePaused = "sitepaused"
    case siteResumed = "siteresumed"
    case pauseFailed = "pausefailed"
    case resumeFailed = "resumefailed"
    case removeFailed = "removefailed"
    case cleanQueued = "cleanqueued"
    case cleanFailed = "cleanfailed"
}

struct SubtitleCfgLanguage: Hashable, Sendable, Identifiable {
    let code: String
    let name: String

    var id: String { code }
}

struct SubtitleCfgWant: Hashable, Sendable, Identifiable {
    let index: Int
    let language: String
    let type: String
    let translateFrom: String
    let forced: Bool

    var id: Int { index }
}

struct SubtitleCfgKind: Hashable, Sendable, Identifiable {
    let kind: String
    let name: String
    let note: String
    let needsAccount: Bool
    let account: [String]
    let covers: SubtitleCfgCovers

    var id: String { kind }
}

struct SubtitleCfgSite: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let url: String
    let kindName: String
    let offered: Bool
    let covers: SubtitleCfgCovers
    let priority: Int
    let needsAccount: Bool
    let hasAccount: Bool
    let allowLocalNetwork: Bool
    let downloadsToday: Int
    let downloadsMax: Int
    let state: SubtitleCfgSiteState
    let turnedOff: Bool
    let pauseReason: String
    let pausedUntil: Date?
    let testing: Bool
    let testOk: Bool?
    let testMessage: String
    let testExtra: String
    let testError: ErrorInfo?
}

struct SubtitleCfgTranslator: Hashable, Sendable {
    let kind: String
    let url: String
    let model: String
    let key: String
    let clearKey: Bool
    let builtInAuto: Bool
    let hasKey: Bool
    let dirty: Bool
    let canSave: Bool
    let canTest: Bool
    let saving: Bool
    let testing: Bool
    let error: ErrorInfo?
    let testOk: Bool?
    let testMessage: String
    let models: [String]

    static let initial = SubtitleCfgTranslator(
        kind: "", url: "", model: "", key: "", clearKey: false, builtInAuto: false, hasKey: false, dirty: false, canSave: false,
        canTest: false, saving: false, testing: false, error: nil, testOk: nil, testMessage: "", models: []
    )
}

struct SubtitleCfgForm: Hashable, Sendable {
    let isNew: Bool
    let id: Int64
    let kind: String
    let name: String
    let url: String
    let numbers: [String: String]
    let numberValid: [String: Bool]
    let account: [String: String]
    let flags: [String: Bool]
    let hasAccount: Bool
    let busy: Bool
    let missing: [String]
    let error: ErrorInfo?
    let canSubmit: Bool
}

struct SubtitleCfgNote: Hashable, Sendable {
    let id: Int
    let kind: SubtitleCfgNoteKind
    let name: String
    let error: ErrorInfo?
}

struct SubtitleCfgInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canClean: Bool
    let dirty: Bool
    let canSave: Bool
    let saving: Bool
    let saveError: ErrorInfo?
    let wants: [SubtitleCfgWant]
    let wishProblem: SubtitleCfgProblem?
    let canAddLanguage: Bool
    let translatorReady: Bool
    let languages: [SubtitleCfgLanguage]
    let numbers: [String: String]
    let numberValid: [String: Bool]
    let flags: [String: Bool]
    let hiSuffix: String
    let cleanRows: [String]
    let cleanAddable: [SubtitleCfgLanguage]
    let cleanLanguage: String
    let steps: [String: Bool]
    let translator: SubtitleCfgTranslator
    let kinds: [SubtitleCfgKind]
    let sites: [SubtitleCfgSite]
    let form: SubtitleCfgForm?
    let cleaning: Bool
    let note: SubtitleCfgNote?

    static let initial = SubtitleCfgInfo(
        load: .loading, allowed: true, canClean: false, dirty: false, canSave: false, saving: false, saveError: nil, wants: [],
        wishProblem: nil, canAddLanguage: true, translatorReady: false, languages: [], numbers: [:], numberValid: [:], flags: [:],
        hiSuffix: "hi", cleanRows: [], cleanAddable: [], cleanLanguage: "*", steps: [:], translator: .initial, kinds: [], sites: [],
        form: nil, cleaning: false, note: nil
    )

    func languageName(_ code: String) -> String {
        languages.first { $0.code == code }?.name ?? code
    }
}
