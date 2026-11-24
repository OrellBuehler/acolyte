import Foundation

enum SettingsServerAlertKindInfo: String, CaseIterable, Hashable, Sendable, Identifiable {
    case discord
    case telegram
    case ntfy
    case gotify
    case pushover
    case email
    case webhook

    var id: Self { self }
}

enum SettingsServerMissingInfo: String, Hashable, Sendable {
    case nothing
    case bot
    case botAndChat = "botandchat"
    case chat
    case topic
    case app
    case server
    case serverAndApp = "serverandapp"
    case userAndApp = "userandapp"
    case user
    case link
    case address
}

enum SettingsServerTestKindInfo: String, Hashable, Sendable {
    case sent
    case typeFirst = "typefirst"
    case failed
}

enum SettingsServerFieldInfo: String, Hashable, Sendable {
    case alertName = "alertname"
    case url
    case botToken = "bottoken"
    case chatId = "chatid"
    case serverUrl = "serverurl"
    case topic
    case userKey = "userkey"
    case appToken = "apptoken"
    case secret
    case webhookAuth = "webhookauth"
    case smtpHost = "smtphost"
    case smtpPort = "smtpport"
    case smtpSecurity = "smtpsecurity"
    case smtpFrom = "smtpfrom"
    case smtpUser = "smtpuser"
    case smtpPass = "smtppass"
    case staffTo = "staffto"
}

struct SettingsServerTestInfo: Hashable, Sendable {
    let kind: SettingsServerTestKindInfo
    let error: ErrorInfo?
}

struct SettingsServerAlertItem: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let kind: String
    let enabled: Bool
    let moments: [String]
    let missing: SettingsServerMissingInfo
    let lastError: String
    let lastSent: Date?
    let test: SettingsServerTestInfo?
    let testing: Bool
    let editable: Bool
}

struct SettingsServerMomentInfo: Identifiable, Hashable, Sendable {
    let id: String
    let on: Bool
}

struct SettingsServerMomentGroupInfo: Identifiable, Hashable, Sendable {
    let key: String
    let moments: [SettingsServerMomentInfo]
    let allOn: Bool

    var id: String { key }
}

struct SettingsServerFormInfo: Hashable, Sendable {
    let id: Int64
    let isNew: Bool
    let kind: SettingsServerAlertKindInfo
    let name: String
    let enabled: Bool
    let allowLocalNetwork: Bool
    let url: String
    let botToken: String
    let chatId: String
    let serverUrl: String
    let topic: String
    let userKey: String
    let appToken: String
    let secret: String
    let webhookAuth: String
    let smtpHost: String
    let smtpPort: String
    let smtpSecurity: String
    let smtpFrom: String
    let smtpUser: String
    let smtpPass: String
    let staffTo: String
    let clearAppToken: Bool
    let canClearToken: Bool
    let urlStored: Bool
    let botTokenStored: Bool
    let appTokenStored: Bool
    let secretStored: Bool
    let passStored: Bool
    let portValid: Bool
    let groups: [SettingsServerMomentGroupInfo]
    let canSave: Bool
    let saving: Bool
    let error: ErrorInfo?
}

struct SettingsServerPackInfo: Hashable, Sendable {
    let style: String
    let loading: Bool
    let saving: Bool
    let error: ErrorInfo?

    static let initial = SettingsServerPackInfo(style: "", loading: true, saving: false, error: nil)
}

enum SettingsServerAlertsNoticeKindInfo: String, Hashable, Sendable {
    case saved
    case removeFailed = "removefailed"
    case packSaved = "packsaved"
    case packFailed = "packfailed"
}

struct SettingsServerAlertsNoticeInfo: Hashable, Sendable {
    let seq: Int
    let kind: SettingsServerAlertsNoticeKindInfo
    let name: String
    let error: ErrorInfo?
}

struct SettingsServerAlertsInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let alerts: [SettingsServerAlertItem]
    let form: SettingsServerFormInfo?
    let pack: SettingsServerPackInfo
    let showPack: Bool
    let notice: SettingsServerAlertsNoticeInfo?

    static let initial = SettingsServerAlertsInfo(
        load: .loading,
        allowed: true,
        alerts: [],
        form: nil,
        pack: .initial,
        showPack: true,
        notice: nil
    )
}

enum SettingsServerBackupPresetInfo: String, CaseIterable, Hashable, Sendable, Identifiable {
    case balanced
    case light
    case careful
    case off

    var id: Self { self }
}

enum SettingsServerRestoreStageInfo: String, Hashable, Sendable {
    case idle
    case uploading
    case restarting
}

enum SettingsServerFileKindInfo: String, Hashable, Sendable {
    case backup
    case keyKit = "keykit"
    case log
}

struct SettingsServerFileInfo: Hashable, Sendable {
    let seq: Int
    let kind: SettingsServerFileKindInfo
    let path: String
    let name: String
    let bytes: Int64
}

struct SettingsServerBackupItem: Identifiable, Hashable, Sendable {
    let name: String
    let kind: String
    let createdAt: Date?
    let size: Int64
    let detail: String
    let busy: Bool

    var id: String { name }
}

struct SettingsServerKeyKitItem: Hashable, Sendable {
    let needed: Bool
    let fingerprint: String
    let warn: Bool
}

enum SettingsServerBackupsNoticeKindInfo: String, Hashable, Sendable {
    case made
    case makeFailed = "makefailed"
    case deleted
    case deleteFailed = "deletefailed"
    case planSaved = "plansaved"
    case planFailed = "planfailed"
    case downloadFailed = "downloadfailed"
    case kitReady = "kitready"
    case kitFailed = "kitfailed"
    case restoreStaged = "restorestaged"
    case restoreFailed = "restorefailed"
    case restarted
    case restartSlow = "restartslow"
}

struct SettingsServerBackupsNoticeInfo: Hashable, Sendable {
    let seq: Int
    let kind: SettingsServerBackupsNoticeKindInfo
    let name: String
    let error: ErrorInfo?
}

struct SettingsServerBackupsInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let off: Bool
    let backups: [SettingsServerBackupItem]
    let preset: SettingsServerBackupPresetInfo
    let presetSaving: Bool
    let restoreWaiting: Bool
    let keyNote: String
    let showKeyNote: Bool
    let keyKit: SettingsServerKeyKitItem?
    let canChange: Bool
    let isOwner: Bool
    let canDownloadKit: Bool
    let keyKitNeedsSession: Bool
    let making: Bool
    let keyKitBusy: Bool
    let restore: SettingsServerRestoreStageInfo
    let restoreName: String
    let file: SettingsServerFileInfo?
    let notice: SettingsServerBackupsNoticeInfo?

    static let initial = SettingsServerBackupsInfo(
        load: .loading,
        allowed: true,
        off: false,
        backups: [],
        preset: .balanced,
        presetSaving: false,
        restoreWaiting: false,
        keyNote: "",
        showKeyNote: false,
        keyKit: nil,
        canChange: false,
        isOwner: false,
        canDownloadKit: false,
        keyKitNeedsSession: false,
        making: false,
        keyKitBusy: false,
        restore: .idle,
        restoreName: "",
        file: nil,
        notice: nil
    )
}

struct SettingsServerLogsFilterItem: Hashable, Sendable {
    let level: String
    let query: String
    let from: String
    let to: String
    let active: Bool
}

struct SettingsServerLogLineItem: Identifiable, Hashable, Sendable {
    let id: Int
    let at: Date?
    let level: String
    let text: String
    let tone: ToneKind
}

struct SettingsServerLogsInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let filter: SettingsServerLogsFilterItem
    let lines: [SettingsServerLogLineItem]
    let total: Int
    let rangeFrom: Int
    let rangeTo: Int
    let hasNewer: Bool
    let hasOlder: Bool
    let noMatch: Bool
    let busy: Bool
    let noLogFile: Bool
    let downloading: Bool
    let downloadError: ErrorInfo?
    let file: SettingsServerFileInfo?

    static let initial = SettingsServerLogsInfo(
        load: .loading,
        allowed: true,
        filter: SettingsServerLogsFilterItem(level: "", query: "", from: "", to: "", active: false),
        lines: [],
        total: 0,
        rangeFrom: 1,
        rangeTo: 0,
        hasNewer: false,
        hasOlder: false,
        noMatch: false,
        busy: false,
        noLogFile: false,
        downloading: false,
        downloadError: nil,
        file: nil
    )
}
