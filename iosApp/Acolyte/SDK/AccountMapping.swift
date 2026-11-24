@preconcurrency import shared
import Foundation

extension Mapping {
    static func browserURL(_ text: String?) -> URL? {
        guard let text, !text.isEmpty else { return nil }
        let direct = URL(string: text)
        let escaped = text.replacingOccurrences(of: "[", with: "%5B").replacingOccurrences(of: "]", with: "%5D")
        guard let url = direct ?? URL(string: escaped), let scheme = url.scheme?.lowercased() else { return nil }
        return scheme == "http" || scheme == "https" ? url : nil
    }

    static func signInError(_ e: SignInError) -> SignInErrorInfo {
        SignInErrorInfo(kind: SignInErrorKind(rawValue: key(e.code)) ?? .server, retryAfter: e.retryAfterSeconds?.intValue)
    }

    static func reauth(_ s: ReauthState) -> ReauthInfo {
        ReauthInfo(
            visible: s.visible,
            mode: ReauthModeKind(rawValue: key(s.mode)) ?? .password,
            needsCode: s.needsCode,
            busy: s.busy,
            error: error(s.error)
        )
    }

    static func invite(_ s: InviteState) -> InviteInfo {
        InviteInfo(
            phase: InvitePhaseKind(rawValue: key(s.phase)) ?? .loading,
            serverName: s.serverName,
            role: InviteRoleKind(rawValue: key(s.kind)) ?? .member,
            kidLimit: s.kidLimit,
            ways: s.ways.compactMap { SignInWayKind(rawValue: key($0)) },
            minPasswordLength: Int(s.minPasswordLength),
            signedInAs: s.signedInAs,
            plexURL: browserURL(s.plexUrl),
            busy: s.busy,
            error: s.error.map { signInError($0) }
        )
    }

    static func inviteTarget(_ t: InviteTarget) -> InviteTargetInfo {
        InviteTargetInfo(baseURL: t.baseUrl, token: t.token)
    }

    static func kotlinInviteTarget(_ t: InviteTargetInfo) -> InviteTarget {
        InviteTarget(baseUrl: t.baseURL, token: t.token)
    }

    static func wizard(_ s: AccountWizardState) -> WizardInfo {
        WizardInfo(
            step: key(s.step) == "done" ? .done : .servers,
            found: s.found.map { WizardFoundInfo(kind: $0.kind, name: $0.name, address: $0.address) },
            discovering: s.discovering,
            busy: s.busy,
            finished: s.finished,
            error: error(s.error)
        )
    }

    static func wizardLinks(_ s: AccountSecurityState) -> WizardLinksInfo {
        var links: [WizardLinkInfo] = []
        if s.plex.linked {
            links.append(WizardLinkInfo(provider: .plex, name: s.plex.name))
        }
        for link in s.media.links {
            if let provider = WizardProviderKind(rawValue: key(link.provider)), provider != .plex {
                links.append(WizardLinkInfo(provider: provider, name: link.name))
            }
        }
        let problem = s.problem.map { key($0) }
        return WizardLinksInfo(
            links: links,
            plexURL: browserURL(s.plexUrl),
            busy: s.busy,
            plexTimedOut: problem == "plextimeout",
            error: error(s.error)
        )
    }
}
