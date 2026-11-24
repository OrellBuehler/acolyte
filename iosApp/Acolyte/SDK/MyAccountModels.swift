import Foundation

enum MyLanguages {
    static let codes: [String] = (
        "en es pt-br fr de ru uk zh-cn ja ko hi ar it nl pl tr pt-pt sv nb da fi cs hu el he ro id vi th zh-tw fa bg sk ca ms sr hr " +
            "lt fil bn et lv is ta"
    ).split(separator: " ").map { String($0) }

    nonisolated static func identifier(_ code: String) -> String {
        let parts = code.split(separator: "-").map { String($0) }
        guard parts.count == 2 else { return code }
        return parts[0] + "-" + parts[1].uppercased()
    }

    nonisolated static func nativeName(_ code: String) -> String {
        let id = identifier(code)
        let name = Locale(identifier: id).localizedString(forIdentifier: id) ?? code
        return name.prefix(1).uppercased() + String(name.dropFirst())
    }

    nonisolated static func localName(_ code: String) -> String {
        Locale.current.localizedString(forIdentifier: identifier(code)) ?? code
    }
}

enum PeoplePresetKind: String, CaseIterable, Identifiable, Hashable, Sendable {
    case admin
    case manager
    case member
    case kid
    case custom

    var id: Self { self }
}

enum PeopleRules {
    static let perms: [String] = ["ask", "auto_approve", "auto_approve_four_k", "approve", "manage_library", "settings"]
    static let ratings: [String] = ["G", "PG", "PG-13", "R"]

    nonisolated static func ratingForShows(_ rating: String) -> String {
        switch rating {
        case "G": "TV-G"
        case "PG": "TV-PG"
        case "PG-13": "TV-14"
        case "R": "TV-MA"
        default: ""
        }
    }

    nonisolated static func presetPerms(_ preset: PeoplePresetKind) -> [String] {
        switch preset {
        case .admin: ["ask", "auto_approve", "approve", "manage_library", "settings"]
        case .manager: ["ask", "auto_approve", "approve", "manage_library"]
        case .member, .kid: ["ask"]
        case .custom: []
        }
    }

    nonisolated static func presetRating(_ preset: PeoplePresetKind) -> String {
        preset == .kid ? "PG" : ""
    }

    nonisolated static func preset(perms: [String], rating: String) -> PeoplePresetKind {
        let wanted = Set(perms)
        for candidate in [PeoplePresetKind.admin, .manager, .member, .kid]
        where Set(presetPerms(candidate)) == wanted && presetRating(candidate) == rating {
            return candidate
        }
        return .custom
    }
}

enum MyProfileSavedKind: String, Hashable, Sendable {
    case name
    case picture
    case pictureRemoved = "pictureremoved"
    case language
    case alerts
    case unfollowed
}

enum MyProfileProblemKind: String, Hashable, Sendable {
    case nameInvalid = "nameinvalid"
    case pictureTooBig = "picturetoobig"
    case pictureEmpty = "pictureempty"
}

struct MyFollowInfo: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let year: Int
    let kind: String
    let artist: String
}

struct MyProfileInfo: Hashable, Sendable {
    let load: LoadInfo
    let displayName: String
    let username: String
    let language: String
    let email: String
    let discordId: String
    let newEpisodes: Bool
    let follows: [MyFollowInfo]
    let followsOn: Bool
    let busy: Bool
    let saved: MyProfileSavedKind?
    let problem: MyProfileProblemKind?
    let actionError: ErrorInfo?

    static let initial = MyProfileInfo(
        load: .loading,
        displayName: "",
        username: "",
        language: "",
        email: "",
        discordId: "",
        newEpisodes: true,
        follows: [],
        followsOn: false,
        busy: false,
        saved: nil,
        problem: nil,
        actionError: nil
    )
}

enum MySecurityNoticeKind: String, Hashable, Sendable {
    case passwordChanged = "passwordchanged"
    case passwordSet = "passwordset"
    case twoFactorOff = "twofactoroff"
    case codesRenewed = "codesrenewed"
    case plexLinked = "plexlinked"
    case plexUnlinked = "plexunlinked"
    case mediaLinked = "medialinked"
    case mediaUnlinked = "mediaunlinked"
    case ssoUnlinked = "ssounlinked"
}

enum MySecurityProblemKind: String, Hashable, Sendable {
    case passwordShort = "passwordshort"
    case passwordsDiffer = "passwordsdiffer"
    case codeInvalid = "codeinvalid"
    case plexTimeout = "plextimeout"
}

struct MyTwoFactorSetupInfo: Hashable, Sendable {
    let secret: String
    let uri: String
    let qr: [String]
}

struct MyMediaServerInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let kind: String
}

struct MyMediaLinkInfo: Hashable, Sendable {
    let provider: String
    let name: String
}

struct MySsoLinkInfo: Identifiable, Hashable, Sendable {
    let kind: String
    let issuer: String
    let name: String

    var id: String { kind + "|" + issuer }
}

struct MySecurityInfo: Hashable, Sendable {
    let load: LoadInfo
    let setup: MyTwoFactorSetupInfo?
    let backupCodes: [String]
    let plexLinked: Bool
    let plexName: String
    let plexURL: URL?
    let servers: [MyMediaServerInfo]
    let mediaLinks: [MyMediaLinkInfo]
    let sso: [MySsoLinkInfo]
    let busy: Bool
    let notice: MySecurityNoticeKind?
    let noticeText: String
    let problem: MySecurityProblemKind?
    let error: ErrorInfo?
    let minPasswordLength: Int

    static let initial = MySecurityInfo(
        load: .loading,
        setup: nil,
        backupCodes: [],
        plexLinked: false,
        plexName: "",
        plexURL: nil,
        servers: [],
        mediaLinks: [],
        sso: [],
        busy: false,
        notice: nil,
        noticeText: "",
        problem: nil,
        error: nil,
        minPasswordLength: 8
    )

    func mediaLink(_ kind: String) -> MyMediaLinkInfo? {
        mediaLinks.first { $0.provider == kind }
    }
}

struct MySessionInfo: Identifiable, Hashable, Sendable {
    let id: String
    let device: String
    let createdAt: Date?
    let lastUsedAt: Date?
    let current: Bool
}

struct MyDevicesInfo: Hashable, Sendable {
    let load: LoadInfo
    let sessions: [MySessionInfo]
    let hasOthers: Bool
    let busy: Bool
    let ended: Int?
    let actionError: ErrorInfo?

    static let initial = MyDevicesInfo(load: .loading, sessions: [], hasOthers: false, busy: false, ended: nil, actionError: nil)
}

enum PeopleNoticeKind: String, Hashable, Sendable {
    case inviteRevoked = "inviterevoked"
    case saved
    case turnedOff = "turnedoff"
    case turnedOn = "turnedon"
    case passwordSet = "passwordset"
    case twoFactorReset = "twofactorreset"
    case deleted
    case ssoUnlinked = "ssounlinked"
    case mediaLinked = "medialinked"
    case mediaUnlinked = "mediaunlinked"
}

enum PeopleProblemKind: String, Hashable, Sendable {
    case invalidDays = "invaliddays"
    case invalidUses = "invaliduses"
    case passwordShort = "passwordshort"
}

enum PeopleInviteStateKind: String, Hashable, Sendable {
    case open
    case used
    case ended
}

struct PeopleLinkInfo: Hashable, Sendable {
    let provider: String
    let name: String
}

struct PeopleUserInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let role: RoleKind
    let off: Bool
    let offReason: String
    let twoFactor: Bool
    let hasPassword: Bool
    let links: [PeopleLinkInfo]
    let sso: [MySsoLinkInfo]
    let avatar: String?
    let lastSeen: Date?
    let maxRating: String
    let perms: [String]
    let mayChange: Bool
    let maySeeDevices: Bool
    let isKid: Bool
    let preset: PeoplePresetKind

    func link(_ provider: String) -> PeopleLinkInfo? {
        links.first { $0.provider == provider }
    }
}

struct PeopleInviteInfo: Identifiable, Hashable, Sendable {
    let id: String
    let role: String
    let perms: [String]
    let note: String
    let createdBy: String
    let createdAt: Date?
    let expiresAt: Date?
    let usedAt: Date?
    let usedBy: String
    let maxRating: String
    let maxUses: Int
    let uses: Int
    let joined: [String]
    let neverExpires: Bool
    let state: PeopleInviteStateKind
}

struct PeopleFreshInfo: Hashable, Sendable {
    let url: String
    let singleUse: Bool
}

struct PeopleNoWayInInfo: Hashable, Sendable {
    let userId: Int64
    let userName: String
    let link: MySsoLinkInfo
}

struct PeopleServerAccountInfo: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let takenBy: Int64
    let takenName: String
}

struct PeopleServerInfo: Identifiable, Hashable, Sendable {
    let id: Int64
    let name: String
    let kind: String
    let accounts: [PeopleServerAccountInfo]
    let failed: Bool
}

struct PeopleInviteDraftInfo: Hashable, Sendable {
    let admin: Bool
    let perms: [String]
    let rating: String
    let note: String
    let days: Int
    let maxUses: Int
}

struct PeopleListInfo: Hashable, Sendable {
    let load: LoadInfo
    let users: [PeopleUserInfo]
    let invites: [PeopleInviteInfo]
    let viewerId: Int64
    let viewerOwner: Bool
    let grantable: [String]
    let busy: Bool
    let fresh: PeopleFreshInfo?
    let notice: PeopleNoticeKind?
    let noticeName: String
    let problem: PeopleProblemKind?
    let actionError: ErrorInfo?
    let noWayIn: PeopleNoWayInInfo?
    let servers: [PeopleServerInfo]
    let minPasswordLength: Int

    static let initial = PeopleListInfo(
        load: .loading,
        users: [],
        invites: [],
        viewerId: 0,
        viewerOwner: false,
        grantable: [],
        busy: false,
        fresh: nil,
        notice: nil,
        noticeName: "",
        problem: nil,
        actionError: nil,
        noWayIn: nil,
        servers: [],
        minPasswordLength: 8
    )

    func user(_ id: Int64) -> PeopleUserInfo? {
        users.first { $0.id == id }
    }
}

struct AiConnectInfo: Hashable, Sendable {
    let load: LoadInfo
    let valid: Bool
    let clientName: String
    let returnsTo: String
    let local: Bool
    let nameVerified: Bool
    let enabled: Bool
    let denyURL: URL?
    let mayAllow: Bool
    let canAllow: Bool
    let busy: Bool
    let redirect: URL?
    let error: ErrorInfo?

    static let initial = AiConnectInfo(
        load: .loading,
        valid: true,
        clientName: "",
        returnsTo: "",
        local: false,
        nameVerified: false,
        enabled: false,
        denyURL: nil,
        mayAllow: false,
        canAllow: false,
        busy: false,
        redirect: nil,
        error: nil
    )
}

extension AccountInfo {
    var canChangeSettings: Bool {
        role == .owner || permissions.contains("settings")
    }

    var mayConnectAiApps: Bool {
        aiAppsAvailable && canChangeSettings
    }
}
