@preconcurrency import shared
import Foundation

extension Mapping {
    static func settingsKeysNotice(_ n: SettingsKeysNotice?) -> SettingsKeysNoticeInfo? {
        guard let n else { return nil }
        return SettingsKeysNoticeInfo(
            seq: Int(n.seq),
            kind: SettingsKeysNoticeKindInfo(rawValue: key(n.kind)) ?? .keyMakeFailed,
            name: n.name,
            error: error(n.error),
            detail: n.detail
        )
    }

    static func settingsKeysFresh(_ f: SettingsKeysFresh?) -> SettingsKeysFreshInfo? {
        guard let f else { return nil }
        return SettingsKeysFreshInfo(id: f.id, name: f.name, key: f.key)
    }

    static func settingsAiKey(_ k: SettingsKeysAiKey) -> SettingsAiKeyInfo {
        SettingsAiKeyInfo(
            id: k.id,
            label: k.label,
            hint: k.hint,
            createdAt: Format.date(iso: k.createdAt),
            lastUsedAt: k.lastUsedAt.flatMap { Format.date(iso: $0) },
            groups: k.groups
        )
    }

    static func settingsSeerrCard(_ c: SettingsSeerrCard?) -> SettingsSeerrCardInfo? {
        guard let c else { return nil }
        let sides = c.sides.map { side in
            SettingsSeerrSideInfo(
                side: side.side,
                path: side.path,
                hasKey: side.hasKey,
                hint: side.hint,
                owner: side.owner,
                ownerActive: side.ownerActive,
                lastUsedAt: side.lastUsedAt.flatMap { Format.date(iso: $0) },
                busy: side.busy
            )
        }
        let sent = c.sent.map { row in
            SettingsSeerrSentInfo(
                key: row.key,
                name: row.name,
                kind: row.kind,
                side: row.side,
                updatedAt: Format.date(iso: row.updatedAt)
            )
        }
        return SettingsSeerrCardInfo(enabled: c.enabled, deleteFiles: c.deleteFiles, sides: sides, sent: sent)
    }

    static func settingsApi(_ s: SettingsApiState) -> SettingsApiInfo {
        SettingsApiInfo(
            load: load(s.status),
            allowed: s.allowed,
            baseAddress: s.baseAddress,
            openApiAddress: s.openApiAddress,
            keys: s.keys.map { k in
                SettingsKeysRowInfo(
                    id: k.id,
                    label: k.label,
                    hint: k.hint,
                    createdAt: Format.date(iso: k.createdAt),
                    lastUsedAt: k.lastUsedAt.flatMap { Format.date(iso: $0) }
                )
            },
            keysError: error(s.keysError),
            keysNeedPassword: s.keysNeedPassword,
            fresh: settingsKeysFresh(s.fresh),
            making: s.making,
            revokingId: s.revokingId,
            seerr: settingsSeerrCard(s.seerr),
            seerrError: error(s.seerrError),
            seerrGone: s.seerrGone,
            seerrSaving: s.seerrSaving,
            seerrFresh: s.seerrFresh.map { SettingsSeerrFreshInfo(side: $0.side, key: $0.key) },
            notice: settingsKeysNotice(s.notice),
            reauth: settingsReauth(s.reauth),
            labelMax: Int(s.labelMax)
        )
    }

    static func settingsAi(_ s: SettingsAiState) -> SettingsAiInfo {
        SettingsAiInfo(
            load: load(s.status),
            available: s.available,
            enabled: s.enabled,
            isOwner: s.isOwner,
            canManageKeys: s.canManageKeys,
            canChange: s.canChange,
            changeNeedsSession: s.changeNeedsSession,
            address: s.address,
            saving: s.saving,
            keys: s.keys.map { settingsAiKey($0) },
            keysError: error(s.keysError),
            keysNeedPassword: s.keysNeedPassword,
            fresh: settingsKeysFresh(s.fresh),
            making: s.making,
            revokingId: s.revokingId,
            changing: s.changing,
            changeError: error(s.changeError),
            changeDetail: s.changeDetail,
            changedCount: Int(s.changedCount),
            notice: settingsKeysNotice(s.notice),
            reauth: settingsReauth(s.reauth),
            offeredGroups: s.offeredGroups,
            defaultGroups: s.defaultGroups,
            labelMax: Int(s.labelMax)
        )
    }
}
