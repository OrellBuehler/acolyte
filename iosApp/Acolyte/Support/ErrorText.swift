import Foundation

extension ErrorInfo {
    var localized: String {
        switch kind {
        case .offline: L10n.tr("error.offline")
        case .timeout: L10n.tr("error.timeout")
        case .tls: L10n.tr("error.tls")
        case .unauthorized: L10n.tr("error.unauthorized")
        case .forbidden: L10n.tr("error.forbidden")
        case .notFound: L10n.tr("error.not.found")
        case .badRequest: L10n.tr("error.bad.request")
        case .conflict: L10n.tr("error.conflict")
        case .partOff: L10n.tr("error.part.off")
        case .handsOff: L10n.tr("error.hands.off")
        case .reauth: L10n.tr("error.reauth")
        case .rateLimited: L10n.tr("error.rate.limited")
        case .server: L10n.tr("error.server")
        case .invalidResponse: L10n.tr("error.invalid.response")
        case .unknown: L10n.tr("error.unknown")
        }
    }
}

extension SignInErrorInfo {
    var localized: String {
        switch kind {
        case .invalidAddress: L10n.tr("signin.error.invalid.address")
        case .unreachable: L10n.tr("signin.error.unreachable")
        case .tls: L10n.tr("signin.error.tls")
        case .notHolyArrs: L10n.tr("signin.error.not.holyarrs")
        case .needsSetup: L10n.tr("signin.error.needs.setup")
        case .hostRefused: L10n.tr("signin.error.host.refused")
        case .wrongCredentials: L10n.tr("signin.error.wrong.credentials")
        case .wrongCode: L10n.tr("signin.error.wrong.code")
        case .signInEnded: L10n.tr("signin.error.sign.in.ended")
        case .keyRejected: L10n.tr("signin.error.key.rejected")
        case .notAllowed: L10n.tr("signin.error.not.allowed")
        case .wayOff: L10n.tr("signin.error.way.off")
        case .rateLimited:
            if let retryAfter { L10n.format("signin.error.rate.limited", retryAfter) } else { L10n.tr("error.rate.limited") }
        case .invalidName: L10n.tr("signin.error.invalid.name")
        case .passwordShort: L10n.tr("signin.error.password.short")
        case .nameTaken: L10n.tr("signin.error.name.taken")
        case .ownerExists: L10n.tr("signin.error.owner.exists")
        case .plexTimeout: L10n.tr("signin.error.plex.timeout")
        case .resetCodeWrong: L10n.tr("signin.error.reset.code.wrong")
        case .inviteGone: L10n.tr("signin.error.invite.gone")
        case .linked: L10n.tr("signin.error.linked")
        case .server: L10n.tr("signin.error.server")
        }
    }
}
