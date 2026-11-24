import Foundation

enum DownloaderHealthKind: String, Hashable, Sendable {
    case working
    case paused
    case resting
    case notAnswering = "notanswering"
    case signInRefused = "signinrefused"
    case unknown
}

enum DownloaderSignInKind: String, Hashable, Sendable {
    case qbittorrent
    case userAndPassword = "userandpassword"
    case password
    case secretToken = "secrettoken"
    case apiKey = "apikey"
}

enum DownloaderSeedingKind: String, Hashable, Sendable {
    case nothing = "none"
    case remove
    case goals
}

enum DownloaderIssueKind: String, Hashable, Sendable {
    case name
    case url
    case category
    case categoryLabel = "categorylabel"
    case categoryFolder = "categoryfolder"
    case kindCategory = "kindcategory"
    case afterImport = "afterimport"
    case afterImportSame = "afterimportsame"
    case priority
    case path
    case ratio
    case hours
    case secret
    case username
}

enum DownloaderKindField: String, CaseIterable, Hashable, Sendable, Identifiable {
    case movies
    case shows
    case music

    var id: Self { self }
}

enum DownloaderKeyNoteKind: String, Hashable, Sendable {
    case fetched
    case needsLogin = "needslogin"
    case wrongLogin = "wronglogin"
    case notFound = "notfound"
}

enum DownloaderMessageKindID: String, Hashable, Sendable {
    case saved
    case pauseFailed = "pausefailed"
    case resumeFailed = "resumefailed"
    case removeFailed = "removefailed"
    case categoryMade = "categorymade"
    case categoryFailed = "categoryfailed"
    case abandoned
    case abandonFailed = "abandonfailed"
}

enum DownloaderSwitchKind: String, CaseIterable, Hashable, Sendable {
    case removeFailed = "removefailed"
    case autoAdd = "autoadd"
    case sabOutside = "saboutside"
    case deleteFiles = "deletefiles"
    case turnDown = "turndown"
    case searchAgain = "searchagain"
    case failDangerous = "faildangerous"
    case replaceVanished = "replacevanished"
    case tryNext = "trynext"
}

enum DownloaderNumberKind: String, CaseIterable, Hashable, Sendable {
    case stalledAfter = "stalledafter"
    case stalledAct = "stalledact"
    case spacing
    case deadMinutes = "deadminutes"
    case crawlKbps = "crawlkbps"
    case crawlHours = "crawlhours"
}

enum DownloaderRulesIssueKind: String, Hashable, Sendable {
    case notANumber = "notanumber"
    case stalledAfterRange = "stalledafterrange"
    case actSooner = "actsooner"
    case actRange = "actrange"
    case spacingRange = "spacingrange"
    case deadMinutesRange = "deadminutesrange"
    case kbpsRange = "kbpsrange"
    case hoursRange = "hoursrange"
}

struct DownloaderKindInfo: Hashable, Sendable, Identifiable {
    let wire: String
    let appName: String
    let badge: String
    let placeholder: String
    let usenet: Bool
    let signIn: DownloaderSignInKind
    let seeding: DownloaderSeedingKind

    var id: String { wire }
}

struct DownloaderTestInfo: Hashable, Sendable {
    let ok: Bool
    let text: String
    let tag: String
    let category: String
    let canMake: Bool
    let makeOff: Bool
    let error: ErrorInfo?
}

struct DownloaderAttemptInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let releaseId: Int64
    let slotId: Int64
    let createdAt: Date?
}

struct DownloaderRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let appName: String
    let badge: String
    let usenet: Bool
    let host: String
    let priority: Int
    let category: String
    let tags: [String]
    let allowLocal: Bool
    let locked: Bool
    let handsOff: Bool
    let active: Int
    let checkedAt: Date?
    let health: DownloaderHealthKind
    let healthWord: String
    let showHealthWord: Bool
    let tone: ToneKind
    let healthText: String
    let showHealthText: Bool
    let healthTextFails: Bool
    let paused: Bool
    let canChange: Bool
    let testing: Bool
    let busy: Bool
    let test: DownloaderTestInfo?
    let helpURL: URL?
    let pending: [DownloaderAttemptInfo]
    let attemptError: ErrorInfo?
    let abandoning: Int64
}

struct DownloaderPairInfo: Hashable, Sendable {
    let theirs: String
    let ours: String
    let link: Bool
    let same: Bool
}

struct DownloaderFormInfo: Hashable, Sendable {
    let id: Int64
    let isNew: Bool
    let kind: DownloaderKindInfo?
    let kindWire: String
    let picked: Bool
    let showFields: Bool
    let name: String
    let url: String
    let username: String
    let secret: String
    let hasSecret: Bool
    let login: Bool
    let hasUsername: Bool
    let category: String
    let priority: String
    let categoryMovies: String
    let categoryShows: String
    let categoryMusic: String
    let tags: [String]
    let pathMap: [DownloaderPairInfo]
    let allowLocal: Bool
    let writable: Bool
    let writableLocked: Bool
    let seedRemove: Bool
    let seedRatio: String
    let seedHours: String
    let seedDelete: Bool
    let categoryAfterImport: String
    let ownOnly: Bool
    let clearFinished: Bool
    let seeding: DownloaderSeedingKind
    let usenet: Bool
    let canSave: Bool
    let canFetchKey: Bool
    let keyPageURL: URL?
    let issues: Set<DownloaderIssueKind>

    func kindCategory(_ field: DownloaderKindField) -> String {
        switch field {
        case .movies: categoryMovies
        case .shows: categoryShows
        case .music: categoryMusic
        }
    }
}

struct DownloaderCategoriesInfo: Hashable, Sendable {
    let loading: Bool
    let listed: Bool?
    let categories: [String]

    func options(_ current: String) -> [String] {
        !current.isEmpty && !categories.contains(current) ? [current] + categories : categories
    }
}

struct DownloaderKeyInfo: Hashable, Sendable {
    let fetching: Bool
    let loginShown: Bool
    let user: String
    let pass: String
    let note: DownloaderKeyNoteKind?
}

struct DownloaderMessageInfo: Hashable, Sendable {
    let kind: DownloaderMessageKindID
    let name: String
    let app: String
    let error: ErrorInfo?
}

struct DownloadersInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canEdit: Bool
    let owner: Bool
    let rows: [DownloaderRowInfo]
    let torrentKinds: [DownloaderKindInfo]
    let usenetKinds: [DownloaderKindInfo]
    let form: DownloaderFormInfo?
    let formError: ErrorInfo?
    let saving: Bool
    let formTesting: Bool
    let formResult: DownloaderTestInfo?
    let categories: DownloaderCategoriesInfo
    let key: DownloaderKeyInfo
    let canFetchKey: Bool
    let message: DownloaderMessageInfo?
    let messageCount: Int

    static let initial = DownloadersInfo(
        load: .loading,
        allowed: true,
        canEdit: false,
        owner: false,
        rows: [],
        torrentKinds: [],
        usenetKinds: [],
        form: nil,
        formError: nil,
        saving: false,
        formTesting: false,
        formResult: nil,
        categories: DownloaderCategoriesInfo(loading: false, listed: nil, categories: []),
        key: DownloaderKeyInfo(fetching: false, loginShown: false, user: "", pass: "", note: nil),
        canFetchKey: false,
        message: nil,
        messageCount: 0
    )
}

struct DownloaderRulesInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let canEdit: Bool
    let flags: [DownloaderSwitchKind: Bool]
    let numbers: [DownloaderNumberKind: String]
    let deadOn: Bool
    let dirty: Bool
    let canSave: Bool
    let saving: Bool
    let saveError: ErrorInfo?
    let issue: DownloaderRulesIssueKind?
    let savedCount: Int

    static let initial = DownloaderRulesInfo(
        load: .loading,
        allowed: true,
        canEdit: false,
        flags: [:],
        numbers: [:],
        deadOn: false,
        dirty: false,
        canSave: false,
        saving: false,
        saveError: nil,
        issue: nil,
        savedCount: 0
    )
}
