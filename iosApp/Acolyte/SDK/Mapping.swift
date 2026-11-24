@preconcurrency import shared
import Foundation

// Kotlin enums are read by name: SKIE turns a Kotlin enum into a Swift enum, but an enum inside a
// Kotlin List keeps the Kotlin class (as jackdaw found), so every enum value is compared as its
// name without case or underscores, which both spellings share. The Swift enums carry those names
// as raw values. Nested sealed subclasses go through SKIE `onEnum(of:)`.
enum Mapping {
    static func key(_ value: Any) -> String {
        String(describing: value).replacingOccurrences(of: "_", with: "").lowercased()
    }

    static func session(_ state: SessionState) -> SessionSnapshot {
        switch onEnum(of: state) {
        case .signedOut(let value):
            return .signedOut(lastServer: value.lastServerUrl, lastUser: value.lastUserName, expired: value.expired)
        case .signedIn(let value):
            return .signedIn(account(value.account))
        default:
            return .restoring
        }
    }

    static func account(_ a: Account) -> AccountInfo {
        AccountInfo(
            serverURL: a.serverUrl,
            serverName: a.serverName,
            method: MethodKind(rawValue: key(a.method)) ?? .password,
            userId: a.userId,
            userName: a.userName,
            displayName: a.displayName,
            role: RoleKind(rawValue: key(a.role)) ?? .unknown,
            permissions: a.permissions,
            serverVersion: a.serverVersion,
            avatarPath: a.avatarPath,
            mustSetUpTwoFactor: a.mustSetUpTwoFactor,
            isKid: a.isKid,
            isManager: a.isManager,
            safeMode: a.safeMode,
            tabs: a.tabs.compactMap { SectionID(rawValue: key($0)) },
            moreTab: key(a.moreTab) == "manage" ? .manage : .more,
            moreGroups: a.moreGroups.compactMap { group in
                SectionGroupID(rawValue: key(group.group)).map { id in
                    MoreGroupInfo(group: id, sections: group.sections.compactMap { SectionID(rawValue: key($0)) })
                }
            }
        )
    }

    static func live(_ status: Any) -> LiveState {
        LiveState(rawValue: key(status)) ?? .off
    }

    static func error(_ e: ApiError?) -> ErrorInfo? {
        guard let e else { return nil }
        return ErrorInfo(
            kind: ErrorKind(rawValue: key(e.code)) ?? .unknown,
            status: Int(e.status),
            message: e.message,
            retryAfter: e.retryAfterSeconds?.intValue
        )
    }

    static func load(_ s: LoadStatus) -> LoadInfo {
        LoadInfo(phase: LoadPhase(rawValue: key(s.phase)) ?? .loading, refreshing: s.refreshing, error: error(s.error))
    }

    static func tone(_ t: Any) -> ToneKind {
        ToneKind(rawValue: key(t)) ?? .idle
    }

    static func signIn(_ s: SignInState) -> SignInInfo {
        SignInInfo(
            step: SignInStepKind(rawValue: key(s.step)) ?? .server,
            serverInput: s.serverInput,
            serverName: s.serverName,
            ways: s.ways.compactMap { SignInWayKind(rawValue: key($0)) },
            otherWays: s.otherWays.compactMap { SignInWayKind(rawValue: key($0)) },
            userName: s.userName,
            hostRefused: s.hostRefused,
            busy: s.busy,
            error: s.error.map {
                SignInErrorInfo(kind: SignInErrorKind(rawValue: key($0.code)) ?? .server, retryAfter: $0.retryAfterSeconds?.intValue)
            }
        )
    }

    static func kotlinWay(_ w: SignInWayKind) -> SignInWay {
        switch w {
        case .password: SignInWay.password
        case .jellyfin: SignInWay.jellyfin
        case .emby: SignInWay.emby
        case .plex: SignInWay.plex
        case .oidc: SignInWay.oidc
        case .forwardAuth: SignInWay.forwardAuth
        }
    }

    static func kotlinSize(_ s: ImageSizeKind) -> ImageSize {
        switch s {
        case .thumb: ImageSize.thumb
        case .poster: ImageSize.poster
        case .posterLarge: ImageSize.posterLarge
        case .backdrop: ImageSize.backdrop
        case .hero: ImageSize.hero
        case .original: ImageSize.original
        }
    }
}
