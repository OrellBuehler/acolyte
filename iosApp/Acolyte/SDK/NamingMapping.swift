@preconcurrency import shared
import Foundation

extension Mapping {
    static func naming(_ s: SettingsNamingState) -> NamingInfo {
        var values: [String: String] = [:]
        for item in s.values { values[item.wire] = item.value }
        var errors: [String: String] = [:]
        for kind in NamingNumberKind.allCases {
            let found = s.numberErrorKey(wire: kind.rawValue)
            if !found.isEmpty { errors[kind.rawValue] = found }
        }
        var examples: [String: String] = [:]
        for field in NamingField.allCases { examples[field.rawValue] = s.exampleFor(wire: field.rawValue) }
        return NamingInfo(
            load: load(s.status),
            allowed: s.allowed,
            canClean: s.canClean,
            values: values,
            switches: Set(s.switchesOn),
            numberErrors: errors,
            focusExamples: examples,
            example: s.example,
            exampleLoading: s.exampleLoading,
            exampleError: error(s.exampleError),
            episodeRows: s.episodeRows.map { NamingRowInfo(key: $0.key, path: $0.path) },
            musicRows: s.musicRows.map { NamingRowInfo(key: $0.key, path: $0.path) },
            languages: s.languages.map { NamingLanguageInfo(code: $0.code, name: $0.name) },
            dirty: s.dirty,
            canSave: s.canSave,
            saving: s.saving,
            saveError: error(s.saveError),
            savedCount: Int(s.savedCount),
            cleaning: s.cleaning,
            cleanNote: s.cleanNote.flatMap { NamingCleanKind(rawValue: key($0)) },
            cleanDays: Int(s.cleanDays),
            cleanCount: Int(s.cleanCount),
            cleanError: error(s.cleanError)
        )
    }

    static func searchSettings(_ s: SettingsSearchState) -> SearchSettingsInfo {
        var values: [String: String] = [:]
        for item in s.values { values[item.wire] = item.value }
        var errors: [String: String] = [:]
        var ranges: [String: [Int]] = [:]
        for wire in searchNumberWires {
            let found = s.numberErrorKey(wire: wire)
            if !found.isEmpty { errors[wire] = found }
            ranges[wire] = [Int(s.numberMin(wire: wire)), Int(s.numberMax(wire: wire))]
        }
        return SearchSettingsInfo(
            load: load(s.status),
            allowed: s.allowed,
            canRun: s.canRun,
            values: values,
            switches: Set(s.switchesOn),
            numberErrors: errors,
            ranges: ranges,
            dirty: s.dirty,
            canSave: s.canSave,
            saving: s.saving,
            saveError: error(s.saveError),
            savedCount: Int(s.savedCount),
            running: s.running.flatMap { SearchSettingsRunKind(rawValue: key($0)) },
            note: s.note.flatMap { SearchSettingsRunKind(rawValue: key($0)) },
            noteCount: Int(s.noteCount),
            runError: error(s.runError),
            runErrorCount: Int(s.runErrorCount)
        )
    }

    private static let searchNumberWires = [
        "missing_search_count", "feed_check_minutes", "early_release_grace_hours", "import_min_video_mb", "import_min_track_kb",
        "episode_default_minutes", "album_track_tolerance", "album_extra_files", "artist_search_cap",
    ]

    static func namingWords(_ words: [SettingsNamingWord]) -> [NamingWordInfo] {
        words.map { NamingWordInfo(key: $0.key, about: $0.about) }
    }
}
