import Foundation

enum NamingField: String, CaseIterable, Hashable, Sendable, Identifiable {
    case filePattern = "file_pattern"
    case folderPattern = "folder_pattern"
    case showFolderPattern = "show_folder_pattern"
    case seasonFolderPattern = "season_folder_pattern"
    case specialsFolder = "specials_folder"
    case episodePattern = "episode_pattern"
    case dailyPattern = "daily_pattern"
    case animePattern = "anime_pattern"
    case artistFolderPattern = "artist_folder_pattern"
    case albumFolderPattern = "album_folder_pattern"
    case trackPattern = "track_pattern"
    case multiDiscTrackPattern = "multi_disc_track_pattern"
    case discFolderPattern = "disc_folder_pattern"

    var id: String { rawValue }

    var insertable: Bool { self != .specialsFolder }
}

enum NamingNumberKind: String, CaseIterable, Hashable, Sendable {
    case holdingDays = "holding_days"
    case minFreeMb = "min_free_mb"
    case adoptScanLimit = "adopt_scan_limit"
    case musicAdoptScanLimit = "music_adopt_scan_limit"
}

enum NamingCleanKind: String, Hashable, Sendable {
    case removing
    case nothing
}

struct NamingRowInfo: Identifiable, Hashable, Sendable {
    let key: String
    let path: String

    var id: String { key }
}

struct NamingLanguageInfo: Identifiable, Hashable, Sendable {
    let code: String
    let name: String

    var id: String { code }
}

struct NamingWordInfo: Identifiable, Hashable, Sendable {
    let key: String
    let about: String

    var id: String { key }
}

struct NamingInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canClean: Bool
    let values: [String: String]
    let switches: Set<String>
    let numberErrors: [String: String]
    let focusExamples: [String: String]
    let example: String
    let exampleLoading: Bool
    let exampleError: ErrorInfo?
    let episodeRows: [NamingRowInfo]
    let musicRows: [NamingRowInfo]
    let languages: [NamingLanguageInfo]
    let dirty: Bool
    let canSave: Bool
    let saving: Bool
    let saveError: ErrorInfo?
    let savedCount: Int
    let cleaning: Bool
    let cleanNote: NamingCleanKind?
    let cleanDays: Int
    let cleanCount: Int
    let cleanError: ErrorInfo?

    static let initial = NamingInfo(
        load: .loading, allowed: true, canClean: false, values: [:], switches: [], numberErrors: [:], focusExamples: [:], example: "",
        exampleLoading: false, exampleError: nil, episodeRows: [], musicRows: [], languages: [], dirty: false, canSave: false,
        saving: false, saveError: nil, savedCount: 0, cleaning: false, cleanNote: nil, cleanDays: 0, cleanCount: 0, cleanError: nil
    )

    func text(_ wire: String) -> String { values[wire] ?? "" }

    func isOn(_ wire: String) -> Bool { switches.contains(wire) }
}

enum SearchSettingsRunKind: String, Hashable, Sendable {
    case feeds
    case better
}

struct SearchSettingsInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canRun: Bool
    let values: [String: String]
    let switches: Set<String>
    let numberErrors: [String: String]
    let ranges: [String: [Int]]
    let dirty: Bool
    let canSave: Bool
    let saving: Bool
    let saveError: ErrorInfo?
    let savedCount: Int
    let running: SearchSettingsRunKind?
    let note: SearchSettingsRunKind?
    let noteCount: Int
    let runError: ErrorInfo?
    let runErrorCount: Int

    static let initial = SearchSettingsInfo(
        load: .loading, allowed: true, canRun: false, values: [:], switches: [], numberErrors: [:], ranges: [:], dirty: false, canSave: false,
        saving: false, saveError: nil, savedCount: 0, running: nil, note: nil, noteCount: 0, runError: nil, runErrorCount: 0
    )

    func text(_ wire: String) -> String { values[wire] ?? "" }

    func isOn(_ wire: String) -> Bool { switches.contains(wire) }
}
