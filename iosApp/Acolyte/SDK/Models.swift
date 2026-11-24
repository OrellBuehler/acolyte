import Foundation

enum SectionID: String, CaseIterable, Hashable, Sendable, Identifiable {
    case home
    case explore
    case library
    case search
    case popular
    case movies
    case shows
    case music
    case stats
    case downloads
    case calendar
    case requests
    case history
    case health
    case people
    case settings
    case account

    var id: String { rawValue }
}

enum SectionGroupID: String, Hashable, Sendable {
    case discover
    case library
    case yours
    case activity
    case system
    case you
}

enum MoreTabKind: Hashable, Sendable {
    case more
    case manage
}

struct MoreGroupInfo: Identifiable, Hashable, Sendable {
    let group: SectionGroupID
    let sections: [SectionID]

    var id: SectionGroupID { group }
}

enum RoleKind: String, Hashable, Sendable {
    case owner
    case admin
    case member
    case unknown
}

enum MethodKind: String, Hashable, Sendable {
    case password
    case mediaServer = "mediaserver"
    case apiKey = "apikey"
}

enum LiveState: String, Hashable, Sendable {
    case off
    case connecting
    case live
    case reconnecting
}

struct AccountInfo: Hashable, Sendable {
    let serverURL: String
    let serverName: String
    let method: MethodKind
    let userId: Int64
    let userName: String
    let displayName: String
    let role: RoleKind
    let permissions: [String]
    let serverVersion: String?
    let avatarPath: String?
    let mustSetUpTwoFactor: Bool
    let isKid: Bool
    let isManager: Bool
    let safeMode: Bool
    let tabs: [SectionID]
    let moreTab: MoreTabKind
    let moreGroups: [MoreGroupInfo]
}

enum SessionSnapshot: Hashable, Sendable {
    case restoring
    case signedOut(lastServer: String?, lastUser: String?, expired: Bool)
    case signedIn(AccountInfo)
}

enum ErrorKind: String, Hashable, Sendable {
    case offline
    case timeout
    case tls
    case unauthorized
    case forbidden
    case notFound = "notfound"
    case badRequest = "badrequest"
    case conflict
    case partOff = "partoff"
    case handsOff = "handsoff"
    case reauth
    case rateLimited = "ratelimited"
    case server
    case invalidResponse = "invalidresponse"
    case unknown
}

struct ErrorInfo: Hashable, Sendable {
    let kind: ErrorKind
    let status: Int
    let message: String
    let retryAfter: Int?

    static let unknown = ErrorInfo(kind: .unknown, status: 0, message: "", retryAfter: nil)
}

enum LoadPhase: String, Hashable, Sendable {
    case loading
    case ready
    case failed
}

struct LoadInfo: Hashable, Sendable {
    let phase: LoadPhase
    let refreshing: Bool
    let error: ErrorInfo?

    static let loading = LoadInfo(phase: .loading, refreshing: false, error: nil)
}

enum ToneKind: String, Hashable, Sendable {
    case ok
    case wait
    case fail
    case info
    case idle
}

enum ImageSizeKind: Hashable, Sendable {
    case thumb
    case poster
    case posterLarge
    case backdrop
    case hero
    case original
}

struct ImageRequestInfo: Hashable, Sendable {
    let url: URL
    let headers: [String: String]
}

struct PlaceholderInfo: Hashable, Sendable {
    let text: String
    let hue: Double
}

enum SignInStepKind: String, Hashable, Sendable {
    case server
    case credentials
    case twoFactor = "twofactor"
    case done
}

enum SignInWayKind: String, Hashable, Sendable, Identifiable {
    case password
    case jellyfin
    case emby
    case plex
    case oidc
    case forwardAuth = "forwardauth"

    var id: Self { self }
}

enum SignInErrorKind: String, Hashable, Sendable {
    case invalidAddress = "invalidaddress"
    case unreachable
    case tls
    case notHolyArrs = "notholyarrs"
    case needsSetup = "needssetup"
    case hostRefused = "hostrefused"
    case wrongCredentials = "wrongcredentials"
    case wrongCode = "wrongcode"
    case signInEnded = "signinended"
    case keyRejected = "keyrejected"
    case notAllowed = "notallowed"
    case wayOff = "wayoff"
    case rateLimited = "ratelimited"
    case server
}

struct SignInErrorInfo: Hashable, Sendable {
    let kind: SignInErrorKind
    let retryAfter: Int?
}

struct SignInInfo: Hashable, Sendable {
    let step: SignInStepKind
    let serverInput: String
    let serverName: String
    let ways: [SignInWayKind]
    let otherWays: [SignInWayKind]
    let userName: String
    let hostRefused: String?
    let busy: Bool
    let error: SignInErrorInfo?

    static let initial = SignInInfo(
        step: .server,
        serverInput: "",
        serverName: "",
        ways: [.password],
        otherWays: [],
        userName: "",
        hostRefused: nil,
        busy: false,
        error: nil
    )
}
