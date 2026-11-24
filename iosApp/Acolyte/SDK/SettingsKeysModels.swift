import Foundation

enum SettingsKeysNoticeKindInfo: String, Hashable, Sendable {
    case keyRevoked = "keyrevoked"
    case keyRevokeFailed = "keyrevokefailed"
    case keyMakeFailed = "keymakefailed"
    case seerrSaveFailed = "seerrsavefailed"
    case seerrKeyMakeFailed = "seerrkeymakefailed"
    case seerrKeyRemoved = "seerrkeyremoved"
    case seerrKeyRemoveFailed = "seerrkeyremovefailed"
    case aiOn = "aion"
    case aiOff = "aioff"
    case aiSaveFailed = "aisavefailed"
    case groupsSaved = "groupssaved"
}

struct SettingsKeysNoticeInfo: Hashable, Sendable {
    let seq: Int
    let kind: SettingsKeysNoticeKindInfo
    let name: String
    let error: ErrorInfo?
    let detail: String
}

struct SettingsKeysFreshInfo: Hashable, Sendable {
    let id: Int64
    let name: String
    let key: String
}

struct SettingsKeysRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let label: String
    let hint: String
    let createdAt: Date?
    let lastUsedAt: Date?
}

struct SettingsAiKeyInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let label: String
    let hint: String
    let createdAt: Date?
    let lastUsedAt: Date?
    let groups: [String]
}

struct SettingsSeerrSideInfo: Hashable, Sendable, Identifiable {
    let side: String
    let path: String
    let hasKey: Bool
    let hint: String
    let owner: String
    let ownerActive: Bool
    let lastUsedAt: Date?
    let busy: Bool

    var id: String { side }
}

struct SettingsSeerrSentInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let kind: String
    let side: String
    let updatedAt: Date?

    var id: String { key }
}

struct SettingsSeerrFreshInfo: Hashable, Sendable {
    let side: String
    let key: String
}

struct SettingsSeerrCardInfo: Hashable, Sendable {
    let enabled: Bool
    let deleteFiles: Bool
    let sides: [SettingsSeerrSideInfo]
    let sent: [SettingsSeerrSentInfo]
}

struct SettingsApiInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let baseAddress: String
    let openApiAddress: String
    let keys: [SettingsKeysRowInfo]
    let keysError: ErrorInfo?
    let keysNeedPassword: Bool
    let fresh: SettingsKeysFreshInfo?
    let making: Bool
    let revokingId: Int64
    let seerr: SettingsSeerrCardInfo?
    let seerrError: ErrorInfo?
    let seerrGone: Bool
    let seerrSaving: Bool
    let seerrFresh: SettingsSeerrFreshInfo?
    let notice: SettingsKeysNoticeInfo?
    let reauth: SettingsReauthInfo
    let labelMax: Int

    static let initial = SettingsApiInfo(
        load: .loading,
        allowed: true,
        baseAddress: "",
        openApiAddress: "",
        keys: [],
        keysError: nil,
        keysNeedPassword: false,
        fresh: nil,
        making: false,
        revokingId: 0,
        seerr: nil,
        seerrError: nil,
        seerrGone: false,
        seerrSaving: false,
        seerrFresh: nil,
        notice: nil,
        reauth: .idle,
        labelMax: 64
    )
}

struct SettingsAiInfo: Hashable, Sendable {
    let load: LoadInfo
    let available: Bool
    let enabled: Bool
    let isOwner: Bool
    let canManageKeys: Bool
    let canChange: Bool
    let changeNeedsSession: Bool
    let address: String
    let saving: Bool
    let keys: [SettingsAiKeyInfo]
    let keysError: ErrorInfo?
    let keysNeedPassword: Bool
    let fresh: SettingsKeysFreshInfo?
    let making: Bool
    let revokingId: Int64
    let changing: Bool
    let changeError: ErrorInfo?
    let changeDetail: String
    let changedCount: Int
    let notice: SettingsKeysNoticeInfo?
    let reauth: SettingsReauthInfo
    let offeredGroups: [String]
    let defaultGroups: [String]
    let labelMax: Int

    static let initial = SettingsAiInfo(
        load: .loading,
        available: true,
        enabled: false,
        isOwner: false,
        canManageKeys: false,
        canChange: false,
        changeNeedsSession: false,
        address: "",
        saving: false,
        keys: [],
        keysError: nil,
        keysNeedPassword: false,
        fresh: nil,
        making: false,
        revokingId: 0,
        changing: false,
        changeError: nil,
        changeDetail: "",
        changedCount: 0,
        notice: nil,
        reauth: .idle,
        offeredGroups: [],
        defaultGroups: [],
        labelMax: 64
    )
}
