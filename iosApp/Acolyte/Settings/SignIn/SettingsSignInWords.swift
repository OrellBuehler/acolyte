import Foundation

enum SettingsSignInWords {
    static func failure(_ error: ErrorInfo) -> String {
        let base = error.kind == .forbidden ? L10n.tr("settings.signin.sign.in.to.change") : error.localized
        let detail = [400, 409, 422, 502, 503].contains(error.status) && !error.message.isEmpty
        return detail ? base + "\n" + error.message : base
    }

    static func notice(_ notice: SettingsSignInNoticeInfo) -> String {
        let failed = notice.error.map { "\n" + failure($0) } ?? ""
        switch notice.kind {
        case .minsaved:
            return L10n.tr("settings.signin.min.saved") + "\n" + L10n.format("settings.signin.characters", notice.count)
        case .twofactorrequired: return L10n.tr("settings.signin.tf.required")
        case .twofactoroptional: return L10n.tr("settings.signin.tf.optional")
        case .forgotshown: return L10n.tr("settings.signin.forgot.shown")
        case .forgothidden: return L10n.tr("settings.signin.forgot.hidden")
        case .waysaved: return L10n.format("settings.signin.way.saved", notice.name)
        case .wayunreachable:
            return L10n.format("settings.signin.way.unreachable", notice.name) + "\n" + L10n.tr("settings.signin.way.not.working")
        case .plexlinked: return L10n.format("settings.signin.plex.linked", notice.name)
        case .plexunlinked: return L10n.tr("settings.signin.plex.unlinked")
        case .imported: return L10n.plural("settings.signin.made", notice.count) + "\n" + notice.name
        case .importnone: return L10n.tr("settings.signin.nobody.new")
        case .ssosaved:
            var gone: [String] = []
            if notice.count > 0 { gone.append(L10n.plural("settings.signin.sso.links.removed", notice.count)) }
            if notice.count2 > 0 { gone.append(L10n.format("settings.signin.sso.signed.out", notice.count2)) }
            let saved = L10n.tr("settings.signin.sso.saved")
            return gone.isEmpty ? saved : saved + "\n" + gone.joined(separator: ", ")
        case .localon: return L10n.tr("settings.signin.skips")
        case .localoff: return L10n.tr("settings.signin.needed.everywhere")
        case .localsaved: return L10n.tr("settings.signin.local.saved")
        case .hostssaved: return L10n.tr("settings.signin.hosts.saved")
        case .unlinkfailed: return L10n.tr("settings.signin.unlink.failed") + failed
        case .failed: return L10n.tr("settings.signin.couldnt.save") + failed
        }
    }

    static func join(_ join: SettingsSignInJoinKind, _ id: SettingsSignInWayID) -> String {
        switch join {
        case .nobody: L10n.tr("settings.signin.join.nobody")
        case .list: L10n.tr("settings.signin.join.list")
        case .everyone:
            id == .plex ? L10n.tr("settings.signin.join.everyone.plex") : L10n.format("settings.signin.join.everyone", id.app)
        }
    }

    static func fix(_ problem: SettingsSignInProblemInfo) -> String {
        switch problem.kind {
        case .part: L10n.tr("settings.signin.fix.part")
        case .missing: L10n.format("settings.signin.fix.missing", problem.fix)
        case .letter: L10n.format("settings.signin.fix.letter", problem.letter, problem.fix)
        case .extra: L10n.tr("settings.signin.fix.extra")
        case .bits: L10n.tr("settings.signin.fix.bits")
        case .wide: L10n.tr("settings.signin.fix.wide")
        case .bridge: L10n.tr("settings.signin.fix.bridge")
        case .notaddress: L10n.tr("settings.signin.fix.not")
        }
    }

    static func role(_ person: SettingsSignInPersonInfo) -> String {
        if !person.kidLimit.isEmpty { return L10n.format("settings.signin.kid.line", person.kidLimit) }
        switch person.role {
        case .owner: return L10n.tr("settings.signin.owner.line")
        case .admin: return L10n.tr("settings.signin.admin.line")
        case .member: return L10n.tr("settings.signin.member.line")
        }
    }

    static func seen(_ local: SettingsSignInLocalInfo) -> String? {
        switch local.seen {
        case .idle: nil
        case .owngateway: L10n.format("settings.signin.you.bridge.ok", local.seenAddress)
        case .bridge: L10n.format("settings.signin.you.bridge", local.seenAddress)
        case .covered: L10n.format("settings.signin.you.covered", local.seenAddress)
        case .notcovered: L10n.format("settings.signin.you.not", local.seenAddress)
        }
    }

    static func everyone(_ ask: SettingsSignInEveryoneInfo) -> String {
        let who: String = switch ask.who {
        case .shared: L10n.tr("settings.signin.who.shared")
        case .everyplex: L10n.tr("settings.signin.who.every")
        case .server: L10n.format("settings.signin.who.server", ask.kind.app)
        }
        let count = ask.count >= 0 ? " " + L10n.format("settings.signin.last.list", L10n.plural("settings.signin.people", ask.count)) : ""
        return L10n.format("settings.signin.everyone.text", who, count)
    }

    static func linkFailure(_ plex: SettingsSignInPlexInfo) -> String {
        switch plex.failure {
        case .ended: L10n.tr("settings.signin.link.ended")
        case .taken: L10n.tr("settings.signin.link.taken")
        case .toomany: L10n.tr("settings.signin.link.too.many")
        case .needspassword: L10n.tr("settings.signin.sign.in.to.change")
        case .other: plex.error.map { failure($0) } ?? ""
        }
    }

    static func ssoError(_ sso: SettingsSignInSsoInfo) -> String? {
        guard let error = sso.saveError else { return nil }
        switch error {
        case .retype: return L10n.tr("settings.signin.sso.retype")
        case .confirmbox: return L10n.tr("settings.signin.sso.confirm.box")
        case .wrongproof: return L10n.tr("settings.signin.sso.wrong.proof")
        case .grouproles: return L10n.tr("settings.signin.sso.group.roles.hint")
        case .other: return sso.error.map { failure($0) } ?? ""
        }
    }
}
