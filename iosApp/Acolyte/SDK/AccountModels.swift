import Foundation

struct InviteTargetInfo: Hashable, Sendable, Identifiable {
    let baseURL: String
    let token: String

    var id: String { baseURL + "|" + token }
}

enum InvitePhaseKind: String, Hashable, Sendable {
    case loading
    case open
    case gone
    case done
}

enum InviteRoleKind: String, Hashable, Sendable {
    case admin
    case kid
    case member
}

struct InviteInfo: Hashable, Sendable {
    let phase: InvitePhaseKind
    let serverName: String
    let role: InviteRoleKind
    let kidLimit: String
    let ways: [SignInWayKind]
    let minPasswordLength: Int
    let signedInAs: String?
    let plexURL: URL?
    let busy: Bool
    let error: SignInErrorInfo?

    static let initial = InviteInfo(
        phase: .loading,
        serverName: "",
        role: .member,
        kidLimit: "",
        ways: [],
        minPasswordLength: 8,
        signedInAs: nil,
        plexURL: nil,
        busy: false,
        error: nil
    )
}

enum ReauthModeKind: String, Hashable, Sendable {
    case password
    case signInAgain = "signinagain"
    case setPassword = "setpassword"
}

struct ReauthInfo: Hashable, Sendable {
    let visible: Bool
    let mode: ReauthModeKind
    let needsCode: Bool
    let busy: Bool
    let error: ErrorInfo?

    static let hidden = ReauthInfo(visible: false, mode: .password, needsCode: false, busy: false, error: nil)
}

enum WizardStepKind: String, Hashable, Sendable {
    case servers
    case done
}

enum WizardProviderKind: String, CaseIterable, Identifiable, Hashable, Sendable {
    case plex
    case jellyfin
    case emby

    var id: Self { self }
}

struct WizardFoundInfo: Identifiable, Hashable, Sendable {
    let kind: String
    let name: String
    let address: String

    var id: String { kind + "|" + address }
}

struct WizardLinkInfo: Hashable, Sendable {
    let provider: WizardProviderKind
    let name: String
}

struct WizardInfo: Hashable, Sendable {
    let step: WizardStepKind
    let found: [WizardFoundInfo]
    let discovering: Bool
    let busy: Bool
    let finished: Bool
    let error: ErrorInfo?

    static let initial = WizardInfo(step: .servers, found: [], discovering: false, busy: false, finished: false, error: nil)
}

struct WizardLinksInfo: Hashable, Sendable {
    let links: [WizardLinkInfo]
    let plexURL: URL?
    let busy: Bool
    let plexTimedOut: Bool
    let error: ErrorInfo?

    static let initial = WizardLinksInfo(links: [], plexURL: nil, busy: false, plexTimedOut: false, error: nil)

    func link(_ provider: WizardProviderKind) -> WizardLinkInfo? {
        links.first { $0.provider == provider }
    }
}
