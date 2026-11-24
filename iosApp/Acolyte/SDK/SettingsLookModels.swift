import Foundation

enum SettingsLookSavedKind: String, Hashable, Sendable {
    case none
    case language
    case autoplayOn = "autoplayon"
    case autoplayOff = "autoplayoff"
    case artLibrary = "artlibrary"
    case artTrending = "arttrending"
}

struct SettingsLookAppearanceInfo: Hashable, Sendable {
    let load: LoadInfo
    let language: String
    let languageSaving: Bool
    let languageError: ErrorInfo?
    let showEveryone: Bool
    let autoplayTrailers: Bool
    let signInArt: Bool
    let everyoneSaving: Bool
    let everyoneError: ErrorInfo?
    let canSeeStats: Bool
    let mostUsedLanguages: [String]
    let otherLanguages: [String]
    let savedCount: Int
    let saved: SettingsLookSavedKind

    static let initial = SettingsLookAppearanceInfo(
        load: .loading,
        language: "",
        languageSaving: false,
        languageError: nil,
        showEveryone: false,
        autoplayTrailers: true,
        signInArt: false,
        everyoneSaving: false,
        everyoneError: nil,
        canSeeStats: false,
        mostUsedLanguages: [],
        otherLanguages: [],
        savedCount: 0,
        saved: .none
    )
}

struct SettingsLookOptionInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
}

struct SettingsLookDefaultsInfo: Hashable, Sendable {
    let load: LoadInfo
    let canEdit: Bool
    let canOpenFolders: Bool
    let canOpenRequests: Bool
    let canOpenOutsideLists: Bool
    let isEmpty: Bool
    let readError: ErrorInfo?
    let showMovies: Bool
    let showShows: Bool
    let showGate: Bool
    let showShowSettings: Bool
    let movieFolders: [SettingsLookOptionInfo]
    let showFolders: [SettingsLookOptionInfo]
    let movieRules: [SettingsLookOptionInfo]
    let showRules: [SettingsLookOptionInfo]
    let movieFolder: Int64
    let showFolder: Int64
    let movieRulesId: Int64
    let showRulesId: Int64
    let movieKeep: Bool
    let movieSearch: Bool
    let showKeep: Bool
    let showSearch: Bool
    let gate: String
    let gateChoices: [String]
    let gateDays: String
    let gateDaysValid: Bool
    let seasonChoice: String
    let seasonChoices: [String]
    let laterSeasons: String
    let laterSeasonChoices: [String]
    let episodeOrder: String
    let orderChoices: [String]
    let detailsSource: String
    let sourceChoices: [String]
    let dirty: Bool
    let canSave: Bool
    let saving: Bool
    let saveError: ErrorInfo?
    let savedCount: Int

    var editing: Bool { canEdit && dirty }

    static let initial = SettingsLookDefaultsInfo(
        load: .loading,
        canEdit: false,
        canOpenFolders: false,
        canOpenRequests: false,
        canOpenOutsideLists: false,
        isEmpty: false,
        readError: nil,
        showMovies: false,
        showShows: false,
        showGate: false,
        showShowSettings: false,
        movieFolders: [],
        showFolders: [],
        movieRules: [],
        showRules: [],
        movieFolder: 0,
        showFolder: 0,
        movieRulesId: 0,
        showRulesId: 0,
        movieKeep: true,
        movieSearch: true,
        showKeep: true,
        showSearch: false,
        gate: "",
        gateChoices: [],
        gateDays: "0",
        gateDaysValid: true,
        seasonChoice: "",
        seasonChoices: [],
        laterSeasons: "",
        laterSeasonChoices: [],
        episodeOrder: "",
        orderChoices: [],
        detailsSource: "",
        sourceChoices: [],
        dirty: false,
        canSave: false,
        saving: false,
        saveError: nil,
        savedCount: 0
    )
}

struct SettingsLookKeyResultInfo: Hashable, Sendable {
    let ok: Bool
    let code: String
    let message: String
    let error: ErrorInfo?
}

struct SettingsLookKeyInfo: Hashable, Sendable, Identifiable {
    let name: String
    let saved: Bool
    let stored: Bool
    let removing: Bool
    let fromDocker: Bool
    let testing: Bool
    let isPin: Bool
    let shownText: String
    let link: String
    let result: SettingsLookKeyResultInfo?

    var id: String { name }
}

struct SettingsLookSourceInfo: Hashable, Sendable, Identifiable {
    let id: String
    let name: String
    let state: String
}

struct SettingsLookHealthInfo: Hashable, Sendable {
    let up: Bool
    let fakeHost: String
}

struct SettingsLookMetadataInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let mode: String
    let locked: Bool
    let dockerValue: String
    let canChooseMode: Bool
    let ownMode: Bool
    let fullMode: Bool
    let tvdbClosed: Bool
    let shownKeys: [SettingsLookKeyInfo]
    let offSources: [SettingsLookSourceInfo]
    let hosts: [String]
    let health: SettingsLookHealthInfo?
    let healthError: ErrorInfo?
    let checking: Bool
    let modeError: ErrorInfo?
    let modeSavedCount: Int
    let dirty: Bool
    let canSave: Bool
    let saving: Bool
    let saveError: ErrorInfo?
    let keysSavedCount: Int

    var editing: Bool { allowed && dirty }

    static let initial = SettingsLookMetadataInfo(
        load: .loading,
        allowed: true,
        mode: "full",
        locked: false,
        dockerValue: "",
        canChooseMode: false,
        ownMode: false,
        fullMode: true,
        tvdbClosed: false,
        shownKeys: [],
        offSources: [],
        hosts: [],
        health: nil,
        healthError: nil,
        checking: false,
        modeError: nil,
        modeSavedCount: 0,
        dirty: false,
        canSave: false,
        saving: false,
        saveError: nil,
        keysSavedCount: 0
    )
}
