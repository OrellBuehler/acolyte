import Foundation
@preconcurrency import shared

extension Mapping {
    static func settingsLookAppearance(_ s: SettingsLookAppearanceState) -> SettingsLookAppearanceInfo {
        SettingsLookAppearanceInfo(
            load: load(s.status),
            language: s.language,
            languageSaving: s.languageSaving,
            languageError: error(s.languageError),
            showEveryone: s.showEveryone,
            autoplayTrailers: s.autoplayTrailers,
            signInArt: s.signInArt,
            everyoneSaving: s.everyoneSaving,
            everyoneError: error(s.everyoneError),
            canSeeStats: s.canSeeStats,
            mostUsedLanguages: s.mostUsedLanguages,
            otherLanguages: s.otherLanguages,
            savedCount: Int(s.savedCount),
            saved: SettingsLookSavedKind(rawValue: key(s.saved)) ?? .none
        )
    }

    static func settingsLookOptions(_ options: [SettingsLookOption]) -> [SettingsLookOptionInfo] {
        options.map { SettingsLookOptionInfo(id: $0.id, name: $0.name) }
    }

    static func settingsLookDefaults(_ s: SettingsLookDefaultsState) -> SettingsLookDefaultsInfo {
        SettingsLookDefaultsInfo(
            load: load(s.status),
            canEdit: s.canEdit,
            canOpenFolders: s.canOpenFolders,
            canOpenRequests: s.canOpenRequests,
            canOpenOutsideLists: s.canOpenOutsideLists,
            isEmpty: s.isEmpty,
            readError: error(s.readError),
            showMovies: s.showMovies,
            showShows: s.showShows,
            showGate: s.showGate,
            showShowSettings: s.showShowSettings,
            movieFolders: settingsLookOptions(s.folderOptions(show: false)),
            showFolders: settingsLookOptions(s.folderOptions(show: true)),
            movieRules: settingsLookOptions(s.ruleOptions(show: false)),
            showRules: settingsLookOptions(s.ruleOptions(show: true)),
            movieFolder: s.folderId(show: false),
            showFolder: s.folderId(show: true),
            movieRulesId: s.rulesId(show: false),
            showRulesId: s.rulesId(show: true),
            movieKeep: s.keepUpToDate(show: false),
            movieSearch: s.searchOnAdd(show: false),
            showKeep: s.keepUpToDate(show: true),
            showSearch: s.searchOnAdd(show: true),
            gate: s.releaseGate,
            gateChoices: s.gateChoices,
            gateDays: s.gateDaysText,
            gateDaysValid: s.gateDaysValid,
            seasonChoice: s.seasonChoice,
            seasonChoices: s.seasonChoices,
            laterSeasons: s.laterSeasons,
            laterSeasonChoices: s.laterSeasonChoices,
            episodeOrder: s.episodeOrder,
            orderChoices: s.orderChoices,
            detailsSource: s.detailsSource,
            sourceChoices: s.sourceChoices,
            dirty: s.dirty,
            canSave: s.canSave,
            saving: s.saving,
            saveError: error(s.saveError),
            savedCount: Int(s.savedCount)
        )
    }

    static func settingsLookKey(_ f: SettingsLookKeyField) -> SettingsLookKeyInfo {
        let result = f.result.map {
            SettingsLookKeyResultInfo(ok: $0.ok, code: $0.code, message: $0.message, error: error($0.error))
        }
        return SettingsLookKeyInfo(
            name: f.name,
            saved: f.saved,
            stored: f.stored,
            removing: f.removing,
            fromDocker: f.fromDocker,
            testing: f.testing,
            isPin: f.isPin,
            shownText: f.shownText,
            link: f.link,
            result: result
        )
    }

    static func settingsLookMetadata(_ s: SettingsLookMetadataState) -> SettingsLookMetadataInfo {
        let health = s.health.map { SettingsLookHealthInfo(up: $0.up, fakeHost: $0.fakeHost) }
        return SettingsLookMetadataInfo(
            load: load(s.status),
            allowed: s.allowed,
            mode: s.mode,
            locked: s.locked,
            dockerValue: s.dockerValue,
            canChooseMode: s.canChooseMode,
            ownMode: s.ownMode,
            fullMode: s.fullMode,
            tvdbClosed: s.tvdbClosed,
            shownKeys: s.shownKeys.map { settingsLookKey($0) },
            offSources: s.offSources.map { SettingsLookSourceInfo(id: $0.id, name: $0.name, state: $0.state) },
            hosts: s.hosts,
            health: health,
            healthError: error(s.healthError),
            checking: s.checking,
            modeError: error(s.modeError),
            modeSavedCount: Int(s.modeSavedCount),
            dirty: s.dirty,
            canSave: s.canSave,
            saving: s.saving,
            saveError: error(s.saveError),
            keysSavedCount: Int(s.keysSavedCount)
        )
    }
}
