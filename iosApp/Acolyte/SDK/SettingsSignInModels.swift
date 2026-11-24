import Foundation

enum SettingsSignInWayID: String, CaseIterable, Hashable, Sendable, Identifiable {
    case plex
    case jellyfin
    case emby

    var id: Self { self }

    var app: String {
        switch self {
        case .plex: "Plex"
        case .jellyfin: "Jellyfin"
        case .emby: "Emby"
        }
    }
}

enum SettingsSignInJoinKind: String, CaseIterable, Hashable, Sendable, Identifiable {
    case nobody
    case list
    case everyone

    var id: Self { self }
}

enum SettingsSignInLinkPhaseKind: String, Hashable, Sendable {
    case idle
    case starting
    case waiting
    case linkedpick
    case linkednone
    case timedout
    case failed
}

enum SettingsSignInLinkFailureKind: String, Hashable, Sendable {
    case ended
    case taken
    case toomany
    case needspassword
    case other
}

enum SettingsSignInWhoKind: String, Hashable, Sendable {
    case shared
    case everyplex
    case server
}

enum SettingsSignInImportPhaseKind: String, Hashable, Sendable {
    case ask
    case loading
    case ready
}

enum SettingsSignInSsoPhaseKind: String, Hashable, Sendable {
    case hidden
    case needssession
    case locked
    case loading
    case ready
    case failed
}

enum SettingsSignInSsoErrorKind: String, Hashable, Sendable {
    case retype
    case confirmbox
    case wrongproof
    case grouproles
    case other
}

enum SettingsSignInRoleKind: String, Hashable, Sendable {
    case owner
    case admin
    case member
}

enum SettingsSignInSkipOffKind: String, Hashable, Sendable {
    case idle = "none"
    case kid
    case persongone
}

enum SettingsSignInSeenKind: String, Hashable, Sendable {
    case idle = "none"
    case owngateway
    case bridge
    case covered
    case notcovered
}

enum SettingsSignInFixKindInfo: String, Hashable, Sendable {
    case part
    case missing
    case letter
    case extra
    case bits
    case wide
    case bridge
    case notaddress
}

enum SettingsSignInNoticeKindInfo: String, Hashable, Sendable {
    case minsaved
    case twofactorrequired
    case twofactoroptional
    case forgotshown
    case forgothidden
    case waysaved
    case wayunreachable
    case plexlinked
    case plexunlinked
    case imported
    case importnone
    case ssosaved
    case localon
    case localoff
    case localsaved
    case hostssaved
    case unlinkfailed
    case failed
}

struct SettingsSignInTestInfo: Hashable, Sendable {
    let ok: Bool
    let text: String
    let error: ErrorInfo?
}

struct SettingsSignInWayInfo: Hashable, Sendable, Identifiable {
    let id: SettingsSignInWayID
    let on: Bool
    let url: String
    let serverId: String
    let join: SettingsSignInJoinKind
    let allow: String
    let saving: Bool
    let testing: Bool
    let test: SettingsSignInTestInfo?
    let untested: Bool
}

struct SettingsSignInPlexServerInfo: Hashable, Sendable, Identifiable {
    let id: String
    let name: String
}

struct SettingsSignInPlexInfo: Hashable, Sendable {
    let linked: Bool
    let name: String
    let canImport: Bool
    let phase: SettingsSignInLinkPhaseKind
    let failure: SettingsSignInLinkFailureKind
    let error: ErrorInfo?
    let authUrl: String
    let openCount: Int
    let servers: [SettingsSignInPlexServerInfo]
    let unlinking: Bool
    let linking: Bool
}

struct SettingsSignInEveryoneInfo: Hashable, Sendable {
    let kind: SettingsSignInWayID
    let who: SettingsSignInWhoKind
    let count: Int
}

struct SettingsSignInCandidateInfo: Hashable, Sendable, Identifiable {
    let id: String
    let name: String
    let email: String
    let serverAdmin: Bool
    let linkedTo: String
    let picked: Bool
    let asKid: Bool
    let locked: Bool
}

struct SettingsSignInImportInfo: Hashable, Sendable {
    let kind: SettingsSignInWayID
    let phase: SettingsSignInImportPhaseKind
    let people: [SettingsSignInCandidateInfo]
    let busy: Bool
    let error: ErrorInfo?
    let confirmAdults: [String]
    let pickedCount: Int
}

struct SettingsSignInSsoInfo: Hashable, Sendable {
    let phase: SettingsSignInSsoPhaseKind
    let loadError: ErrorInfo?
    let environment: Bool
    let viewOn: Bool
    let viewIssuer: String
    let viewClientId: String
    let viewAllowJoin: Bool
    let viewLinks: Int
    let forwardOn: Bool
    let forwardIssuer: String
    let callback: String
    let on: Bool
    let issuer: String
    let clientId: String
    let secret: String
    let secretKept: Bool
    let publicClient: Bool
    let allowJoin: Bool
    let groupsClaim: String
    let groupRoles: String
    let confirmEveryone: Bool
    let opening: Bool
    let moving: Bool
    let showCode: Bool
    let needsCode: Bool
    let testing: Bool
    let test: SettingsSignInTestInfo?
    let saving: Bool
    let saveError: SettingsSignInSsoErrorKind?
    let error: ErrorInfo?
}

struct SettingsSignInPersonInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let role: SettingsSignInRoleKind
    let kidLimit: String
    let avatar: String
}

struct SettingsSignInProblemInfo: Hashable, Sendable, Identifiable {
    let line: Int
    let text: String
    let kind: SettingsSignInFixKindInfo
    let fix: String
    let letter: String

    var id: Int { line }
}

struct SettingsSignInLocalInfo: Hashable, Sendable {
    let skip: Bool
    let skipSaving: Bool
    let networks: String
    let proxies: String
    let saving: Bool
    let saveError: ErrorInfo?
    let people: [SettingsSignInPersonInfo]?
    let peopleError: ErrorInfo?
    let pickedId: Int64
    let fixedProxies: [String]
    let cgnatRange: String
    let hasTailscale: Bool
    let seenAddress: String
    let seen: SettingsSignInSeenKind
    let skipOff: SettingsSignInSkipOffKind
    let proxyUntrusted: Bool
    let mediaApps: [String]
    let netProblems: [SettingsSignInProblemInfo]
    let proxyProblems: [SettingsSignInProblemInfo]
    let problemCount: Int
    let canSave: Bool
}

struct SettingsSignInHostsInfo: Hashable, Sendable {
    let text: String
    let saving: Bool
}

struct SettingsSignInNoticeInfo: Hashable, Sendable {
    let seq: Int
    let kind: SettingsSignInNoticeKindInfo
    let name: String
    let count: Int
    let count2: Int
    let error: ErrorInfo?
}

struct SettingsSignInInfo: Hashable, Sendable {
    let load: LoadInfo
    let isOwner: Bool
    let apiKey: Bool
    let minLengthText: String
    let minLengthSaving: Bool
    let shortPasswordWarning: Bool
    let canSaveMinLength: Bool
    let requireTwoFactor: Bool
    let requireTwoFactorSaving: Bool
    let forgotLink: Bool
    let forgotLinkSaving: Bool
    let ways: [SettingsSignInWayInfo]
    let plex: SettingsSignInPlexInfo
    let everyone: SettingsSignInEveryoneInfo?
    let importSheet: SettingsSignInImportInfo?
    let sso: SettingsSignInSsoInfo
    let local: SettingsSignInLocalInfo
    let hosts: SettingsSignInHostsInfo
    let notice: SettingsSignInNoticeInfo?
    let showLocal: Bool

    func way(_ id: SettingsSignInWayID) -> SettingsSignInWayInfo? {
        ways.first { $0.id == id }
    }

    func canImport(_ id: SettingsSignInWayID) -> Bool {
        if id == .plex { return plex.canImport }
        return !(way(id)?.url.isEmpty ?? true)
    }

    static let initial = SettingsSignInInfo(
        load: .loading,
        isOwner: false,
        apiKey: false,
        minLengthText: "8",
        minLengthSaving: false,
        shortPasswordWarning: false,
        canSaveMinLength: false,
        requireTwoFactor: false,
        requireTwoFactorSaving: false,
        forgotLink: true,
        forgotLinkSaving: false,
        ways: [],
        plex: SettingsSignInPlexInfo(
            linked: false,
            name: "",
            canImport: false,
            phase: .idle,
            failure: .other,
            error: nil,
            authUrl: "",
            openCount: 0,
            servers: [],
            unlinking: false,
            linking: false
        ),
        everyone: nil,
        importSheet: nil,
        sso: SettingsSignInSsoInfo(
            phase: .hidden,
            loadError: nil,
            environment: false,
            viewOn: false,
            viewIssuer: "",
            viewClientId: "",
            viewAllowJoin: false,
            viewLinks: 0,
            forwardOn: false,
            forwardIssuer: "",
            callback: "",
            on: false,
            issuer: "",
            clientId: "",
            secret: "",
            secretKept: false,
            publicClient: false,
            allowJoin: false,
            groupsClaim: "",
            groupRoles: "",
            confirmEveryone: false,
            opening: false,
            moving: false,
            showCode: true,
            needsCode: false,
            testing: false,
            test: nil,
            saving: false,
            saveError: nil,
            error: nil
        ),
        local: SettingsSignInLocalInfo(
            skip: false,
            skipSaving: false,
            networks: "",
            proxies: "",
            saving: false,
            saveError: nil,
            people: nil,
            peopleError: nil,
            pickedId: -1,
            fixedProxies: [],
            cgnatRange: "100.64.0.0/10",
            hasTailscale: false,
            seenAddress: "",
            seen: .idle,
            skipOff: .idle,
            proxyUntrusted: false,
            mediaApps: [],
            netProblems: [],
            proxyProblems: [],
            problemCount: 0,
            canSave: false
        ),
        hosts: SettingsSignInHostsInfo(text: "", saving: false),
        notice: nil,
        showLocal: true
    )
}
