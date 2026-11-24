@preconcurrency import shared
import Foundation

extension Mapping {
    static func myFollow(_ f: AccountFollow) -> MyFollowInfo {
        MyFollowInfo(id: f.unfollow, name: f.name, year: Int(f.year), kind: f.kind, artist: f.artist)
    }

    static func myProfile(_ s: AccountProfileState) -> MyProfileInfo {
        MyProfileInfo(
            load: load(s.status),
            displayName: s.profile.displayName,
            username: s.profile.username,
            language: s.profile.language,
            email: s.alerts.email,
            discordId: s.alerts.discordId,
            newEpisodes: s.newEpisodes,
            follows: s.follows.map { myFollow($0) },
            followsOn: s.followsOn,
            busy: s.busy,
            saved: s.saved.flatMap { MyProfileSavedKind(rawValue: key($0)) },
            problem: s.problem.flatMap { MyProfileProblemKind(rawValue: key($0)) },
            actionError: error(s.actionError)
        )
    }

    static func mySso(_ l: AccountSsoLink) -> MySsoLinkInfo {
        MySsoLinkInfo(kind: l.kind, issuer: l.issuer, name: l.name)
    }

    static func mySecurity(_ s: AccountSecurityState) -> MySecurityInfo {
        MySecurityInfo(
            load: load(s.status),
            setup: s.setup.map { MyTwoFactorSetupInfo(secret: $0.secret, uri: $0.uri, qr: $0.qr) },
            backupCodes: s.backupCodes,
            plexLinked: s.plex.linked,
            plexName: s.plex.name,
            plexURL: browserURL(s.plexUrl),
            servers: s.media.servers.map { MyMediaServerInfo(id: $0.id, name: $0.name, kind: $0.kind) },
            mediaLinks: s.media.links.map { MyMediaLinkInfo(provider: $0.provider, name: $0.name) },
            sso: s.sso.map { mySso($0) },
            busy: s.busy,
            notice: s.notice.flatMap { MySecurityNoticeKind(rawValue: key($0)) },
            noticeText: s.noticeText,
            problem: s.problem.flatMap { MySecurityProblemKind(rawValue: key($0)) },
            error: error(s.error),
            minPasswordLength: Int(s.minPasswordLength)
        )
    }

    static func myDevices(_ s: AccountDevicesState) -> MyDevicesInfo {
        MyDevicesInfo(
            load: load(s.status),
            sessions: s.ordered.map { session in
                MySessionInfo(
                    id: session.id,
                    device: session.device,
                    createdAt: Format.date(iso: session.createdAt),
                    lastUsedAt: Format.date(iso: session.lastUsedAt),
                    current: session.current
                )
            },
            hasOthers: s.hasOthers,
            busy: s.busy,
            ended: s.ended?.intValue,
            actionError: error(s.actionError)
        )
    }

    static func peopleUser(_ u: TitleUser, in s: PeopleState) -> PeopleUserInfo {
        PeopleUserInfo(
            id: u.id,
            name: u.name,
            role: RoleKind(rawValue: u.role) ?? .unknown,
            off: !u.state.isEmpty && u.state != "active",
            offReason: u.offReason,
            twoFactor: u.twoFactor,
            hasPassword: u.password,
            links: u.links.map { PeopleLinkInfo(provider: $0.provider, name: $0.name) },
            sso: u.ssoLinks.map { mySso($0) },
            avatar: u.avatar,
            lastSeen: Format.date(iso: u.lastSeenAt),
            maxRating: u.maxRating,
            perms: u.perms,
            mayChange: s.mayChange(user: u),
            maySeeDevices: s.maySeeDevices(user: u),
            isKid: s.isKid(user: u),
            preset: PeoplePresetKind(rawValue: key(s.presetOf(user: u))) ?? .custom
        )
    }

    static func peopleInvite(_ i: PeopleInvite) -> PeopleInviteInfo {
        let expires = Format.date(iso: i.expiresAt)
        let maxUses = Int(i.maxUses)
        let uses = Int(i.uses)
        let full = maxUses > 0 && uses >= maxUses
        let expired = !i.neverExpires && (expires.map { $0 < Date() } ?? false)
        let state: PeopleInviteStateKind
        if !i.usedAt.isEmpty && maxUses == 1 {
            state = .used
        } else if full || expired {
            state = .ended
        } else {
            state = .open
        }
        return PeopleInviteInfo(
            id: i.id,
            role: i.role,
            perms: i.perms,
            note: i.note,
            createdBy: i.createdBy,
            createdAt: Format.date(iso: i.createdAt),
            expiresAt: expires,
            usedAt: Format.date(iso: i.usedAt),
            usedBy: i.usedBy,
            maxRating: i.maxRating,
            maxUses: maxUses,
            uses: uses,
            joined: i.joined,
            neverExpires: i.neverExpires,
            state: state
        )
    }

    static func peopleServer(_ s: PeopleMediaServer) -> PeopleServerInfo {
        PeopleServerInfo(
            id: s.id,
            name: s.name,
            kind: s.kind,
            accounts: s.accounts.map {
                PeopleServerAccountInfo(id: $0.id, name: $0.name, takenBy: $0.takenBy, takenName: $0.takenName)
            },
            failed: !s.error.isEmpty
        )
    }

    static func peopleList(_ s: PeopleState) -> PeopleListInfo {
        let noWayIn = s.noWayIn.map { ask in
            PeopleNoWayInInfo(userId: ask.userId, userName: s.users.first { $0.id == ask.userId }?.name ?? "", link: mySso(ask.link))
        }
        return PeopleListInfo(
            load: load(s.status),
            users: s.users.map { peopleUser($0, in: s) },
            invites: s.invites.map { peopleInvite($0) },
            viewerId: s.viewerId,
            viewerOwner: s.viewerOwner,
            grantable: s.grantable,
            busy: s.busy,
            fresh: s.fresh.map { PeopleFreshInfo(url: $0.url, singleUse: $0.singleUse) },
            notice: s.notice.flatMap { PeopleNoticeKind(rawValue: key($0)) },
            noticeName: s.noticeName,
            problem: s.problem.flatMap { PeopleProblemKind(rawValue: key($0)) },
            actionError: error(s.actionError),
            noWayIn: noWayIn,
            servers: s.servers.map { peopleServer($0) },
            minPasswordLength: Int(s.minPasswordLength)
        )
    }

    static func aiConnect(_ s: AiConnectState) -> AiConnectInfo {
        AiConnectInfo(
            load: load(s.status),
            valid: s.valid,
            clientName: s.info.clientName,
            returnsTo: s.info.returnsTo,
            local: s.info.local,
            nameVerified: s.info.nameVerified,
            enabled: s.info.enabled,
            denyURL: s.info.denyUrl.isEmpty ? nil : URL(string: s.info.denyUrl),
            mayAllow: s.mayAllow,
            canAllow: s.canAllow,
            busy: s.busy,
            redirect: s.redirect.flatMap { $0.isEmpty ? nil : URL(string: $0) },
            error: error(s.error)
        )
    }
}
