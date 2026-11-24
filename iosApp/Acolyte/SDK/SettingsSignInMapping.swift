@preconcurrency import shared
import Foundation

extension Mapping {
    static func kotlinSignInWay(_ id: SettingsSignInWayID) -> SettingsSignInWayKind {
        switch id {
        case .plex: SettingsSignInWayKind.plex
        case .jellyfin: SettingsSignInWayKind.jellyfin
        case .emby: SettingsSignInWayKind.emby
        }
    }

    static func kotlinSignInJoin(_ join: SettingsSignInJoinKind) -> SettingsSignInJoin {
        switch join {
        case .nobody: SettingsSignInJoin.nobody
        case .list: SettingsSignInJoin.list
        case .everyone: SettingsSignInJoin.everyone
        }
    }

    static func signInTest(_ t: SettingsSignInTest?) -> SettingsSignInTestInfo? {
        guard let t else { return nil }
        return SettingsSignInTestInfo(ok: t.ok, text: t.text, error: error(t.error))
    }

    static func signInWay(_ w: SettingsSignInWay) -> SettingsSignInWayInfo {
        SettingsSignInWayInfo(
            id: SettingsSignInWayID(rawValue: key(w.kind)) ?? .plex,
            on: w.on,
            url: w.url,
            serverId: w.serverId,
            join: SettingsSignInJoinKind(rawValue: key(w.join)) ?? .nobody,
            allow: w.allow,
            saving: w.saving,
            testing: w.testing,
            test: signInTest(w.test),
            untested: w.untested
        )
    }

    static func signInPlex(_ p: SettingsSignInPlex) -> SettingsSignInPlexInfo {
        SettingsSignInPlexInfo(
            linked: p.linked,
            name: p.name,
            canImport: p.canImport,
            phase: SettingsSignInLinkPhaseKind(rawValue: key(p.phase)) ?? .idle,
            failure: SettingsSignInLinkFailureKind(rawValue: key(p.failure)) ?? .other,
            error: error(p.error),
            authUrl: p.authUrl,
            openCount: Int(p.openCount),
            servers: p.servers.map { SettingsSignInPlexServerInfo(id: $0.id, name: $0.name) },
            unlinking: p.unlinking,
            linking: p.linking
        )
    }

    static func signInEveryone(_ a: SettingsSignInEveryoneAsk?) -> SettingsSignInEveryoneInfo? {
        guard let a else { return nil }
        return SettingsSignInEveryoneInfo(
            kind: SettingsSignInWayID(rawValue: key(a.kind)) ?? .plex,
            who: SettingsSignInWhoKind(rawValue: key(a.who)) ?? .server,
            count: Int(a.count)
        )
    }

    static func signInImport(_ i: SettingsSignInImport?) -> SettingsSignInImportInfo? {
        guard let i else { return nil }
        let people = i.people.map { person in
            SettingsSignInCandidateInfo(
                id: person.id,
                name: person.name,
                email: person.email,
                serverAdmin: person.serverAdmin,
                linkedTo: person.linkedTo,
                picked: person.picked,
                asKid: person.asKid,
                locked: person.locked
            )
        }
        return SettingsSignInImportInfo(
            kind: SettingsSignInWayID(rawValue: key(i.kind)) ?? .plex,
            phase: SettingsSignInImportPhaseKind(rawValue: key(i.phase)) ?? .ask,
            people: people,
            busy: i.busy,
            error: error(i.error),
            confirmAdults: i.confirmAdults,
            pickedCount: Int(i.pickedCount)
        )
    }

    static func signInSso(_ s: SettingsSignInSso) -> SettingsSignInSsoInfo {
        let view = s.view
        return SettingsSignInSsoInfo(
            phase: SettingsSignInSsoPhaseKind(rawValue: key(s.phase)) ?? .hidden,
            loadError: error(s.loadError),
            environment: s.environment,
            viewOn: view.on,
            viewIssuer: view.issuer,
            viewClientId: view.clientId,
            viewAllowJoin: view.allowJoin,
            viewLinks: Int(view.links),
            forwardOn: view.forward.on,
            forwardIssuer: view.forward.issuer,
            callback: s.callback,
            on: s.on,
            issuer: s.issuer,
            clientId: s.clientId,
            secret: s.secret,
            secretKept: s.secretKept,
            publicClient: s.publicClient,
            allowJoin: s.allowJoin,
            groupsClaim: s.groupsClaim,
            groupRoles: s.groupRoles,
            confirmEveryone: s.confirmEveryone,
            opening: s.opening,
            moving: s.moving,
            showCode: s.showCode,
            needsCode: s.needsCode,
            testing: s.testing,
            test: signInTest(s.test),
            saving: s.saving,
            saveError: s.saveError.flatMap { SettingsSignInSsoErrorKind(rawValue: key($0)) },
            error: error(s.error)
        )
    }

    static func signInProblems(_ list: [SettingsSignInLineProblem]) -> [SettingsSignInProblemInfo] {
        list.map { problem in
            SettingsSignInProblemInfo(
                line: Int(problem.line),
                text: problem.text,
                kind: SettingsSignInFixKindInfo(rawValue: key(problem.kind)) ?? .notaddress,
                fix: problem.fix,
                letter: problem.letter
            )
        }
    }

    static func signInLocal(_ l: SettingsSignInLocal) -> SettingsSignInLocalInfo {
        let people = l.people.map { list in
            list.map { person in
                SettingsSignInPersonInfo(
                    id: person.id,
                    name: person.name,
                    role: SettingsSignInRoleKind(rawValue: key(person.role)) ?? .member,
                    kidLimit: person.kidLimit,
                    avatar: person.avatar
                )
            }
        }
        return SettingsSignInLocalInfo(
            skip: l.skip,
            skipSaving: l.skipSaving,
            networks: l.networks,
            proxies: l.proxies,
            saving: l.saving,
            saveError: error(l.saveError),
            people: people,
            peopleError: error(l.peopleError),
            pickedId: l.pickedId,
            fixedProxies: l.fixedProxies,
            cgnatRange: l.cgnatRange,
            hasTailscale: l.hasTailscale,
            seenAddress: l.seenAddress,
            seen: SettingsSignInSeenKind(rawValue: key(l.seen)) ?? .idle,
            skipOff: SettingsSignInSkipOffKind(rawValue: key(l.skipOff)) ?? .idle,
            proxyUntrusted: l.proxyUntrusted,
            mediaApps: l.mediaApps,
            netProblems: signInProblems(l.netProblems),
            proxyProblems: signInProblems(l.proxyProblems),
            problemCount: Int(l.problemCount),
            canSave: l.canSave
        )
    }

    static func signInNotice(_ n: SettingsSignInNotice?) -> SettingsSignInNoticeInfo? {
        guard let n else { return nil }
        return SettingsSignInNoticeInfo(
            seq: Int(n.seq),
            kind: SettingsSignInNoticeKindInfo(rawValue: key(n.kind)) ?? .failed,
            name: n.name,
            count: Int(n.count),
            count2: Int(n.count2),
            error: error(n.error)
        )
    }

    static func settingsSignIn(_ s: SettingsSignInState) -> SettingsSignInInfo {
        SettingsSignInInfo(
            load: load(s.status),
            isOwner: s.isOwner,
            apiKey: s.apiKey,
            minLengthText: s.minLengthText,
            minLengthSaving: s.minLengthSaving,
            shortPasswordWarning: s.shortPasswordWarning,
            canSaveMinLength: s.canSaveMinLength,
            requireTwoFactor: s.requireTwoFactor,
            requireTwoFactorSaving: s.requireTwoFactorSaving,
            forgotLink: s.forgotLink,
            forgotLinkSaving: s.forgotLinkSaving,
            ways: s.ways.map { signInWay($0) },
            plex: signInPlex(s.plex),
            everyone: signInEveryone(s.everyoneAsk),
            importSheet: signInImport(s.importSheet),
            sso: signInSso(s.sso),
            local: signInLocal(s.local),
            hosts: SettingsSignInHostsInfo(text: s.hosts.text, saving: s.hosts.saving),
            reauth: settingsReauth(s.reauth),
            notice: signInNotice(s.notice),
            showLocal: s.showLocal
        )
    }
}
