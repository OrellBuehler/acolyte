@preconcurrency import shared
import Foundation

extension Mapping {
    static func settingsHome(_ s: SettingsHomeState) -> SettingsHomeInfo {
        SettingsHomeInfo(
            load: load(s.status),
            allowed: s.allowed,
            groups: s.groups.compactMap { settingsGroup($0) },
            accountPages: s.accountPages.compactMap { SettingsPageID(rawValue: $0.key) },
            reasons: s.reasons.map { settingsReason($0) },
            showFinishSetup: s.showFinishSetup,
            needsAttention: s.needsAttention
        )
    }

    private static func settingsGroup(_ g: SettingsHomeGroup) -> SettingsHomeGroupInfo? {
        guard let id = SettingsGroupID(rawValue: key(g.group)) else { return nil }
        return SettingsHomeGroupInfo(
            group: id,
            pages: g.pages.compactMap { entry in
                SettingsPageID(rawValue: entry.page.key).map {
                    SettingsHomePageInfo(page: $0, standing: standing(entry.standing), count: Int(entry.count))
                }
            },
            standing: standing(g.standing),
            count: Int(g.count)
        )
    }

    private static func settingsReason(_ r: SettingsHomeReason) -> SettingsReasonInfo {
        SettingsReasonInfo(
            key: r.key,
            kind: SettingsReasonKind(rawValue: key(r.kind)) ?? .setting,
            tone: tone(r.tone),
            page: r.page.flatMap { SettingsPageID(rawValue: $0.key) },
            title: r.title,
            body: r.body,
            count: Int(r.count)
        )
    }

    private static func standing(_ value: SettingsHomeStanding) -> SettingsStandingKind {
        SettingsStandingKind(rawValue: key(value)) ?? .unknown
    }
}
