@preconcurrency import shared
import Foundation

extension Mapping {
    static func downloaderKinds(_ s: SettingsDownloadersState) -> [DownloaderKindInfo] {
        (s.torrentKinds + s.usenetKinds).map {
            DownloaderKindInfo(
                wire: $0.wire,
                appName: $0.appName,
                badge: $0.badge,
                placeholder: $0.placeholder,
                usenet: $0.usenet,
                signIn: DownloaderSignInKind(rawValue: key($0.signIn)) ?? .apiKey,
                seeding: DownloaderSeedingKind(rawValue: key($0.seeding)) ?? .nothing
            )
        }
    }

    static func downloaderTest(_ t: SettingsDownloaderTestView?) -> DownloaderTestInfo? {
        guard let t else { return nil }
        return DownloaderTestInfo(
            ok: t.ok,
            text: t.text,
            tag: t.tag,
            category: t.category,
            canMake: t.canMake,
            makeOff: t.makeOff,
            error: error(t.error)
        )
    }

    static func downloaderRow(_ r: SettingsDownloaderRow, kinds: [DownloaderKindInfo]) -> DownloaderRowInfo {
        let info = r.info
        let kind = kinds.first { $0.wire == info.kind }
        return DownloaderRowInfo(
            id: info.id,
            name: info.name,
            appName: r.appName,
            badge: kind?.badge ?? String(r.appName.prefix(2)),
            usenet: r.usenet,
            host: r.host,
            priority: Int(info.priority),
            category: info.category,
            tags: info.tags,
            allowLocal: info.allowLocalNetwork,
            locked: r.locked,
            handsOff: r.handsOff,
            active: Int(info.activeDownloads),
            checkedAt: Format.date(iso: info.checkedAt),
            health: DownloaderHealthKind(rawValue: key(r.health)) ?? .unknown,
            healthWord: r.healthWord,
            showHealthWord: r.showHealthWord,
            tone: tone(r.tone),
            healthText: info.health.text,
            showHealthText: r.showHealthText,
            healthTextFails: r.healthTextFails,
            paused: r.paused,
            canChange: r.canChange,
            testing: r.testing,
            busy: r.busy,
            test: downloaderTest(r.test),
            helpURL: r.helpUrl.isEmpty ? nil : URL(string: r.helpUrl),
            pending: r.pending.map {
                DownloaderAttemptInfo(id: $0.id, releaseId: $0.releaseId, slotId: $0.slotId, createdAt: Format.date(iso: $0.createdAt))
            },
            attemptError: error(r.attemptError),
            abandoning: r.abandoning
        )
    }

    static func downloaderForm(_ f: SettingsDownloaderForm, state s: SettingsDownloadersState, kinds: [DownloaderKindInfo]) -> DownloaderFormInfo {
        let same = f.samePairs.map { $0.boolValue }
        let pairs = f.pathMap.enumerated().map { index, pair in
            DownloaderPairInfo(theirs: pair.theirs, ours: pair.ours, link: pair.link, same: index < same.count ? same[index] : false)
        }
        return DownloaderFormInfo(
            id: f.id,
            isNew: f.isNew,
            kind: kinds.first { $0.wire == f.kind },
            kindWire: f.kind,
            picked: f.picked,
            showFields: f.showFields,
            name: f.name,
            url: f.url,
            username: f.username,
            secret: f.secret,
            hasSecret: f.hasSecret,
            login: f.login,
            hasUsername: f.hasUsername,
            category: f.category,
            priority: f.priority,
            categoryMovies: f.categoryMovies,
            categoryShows: f.categoryShows,
            categoryMusic: f.categoryMusic,
            tags: f.tags,
            pathMap: pairs,
            allowLocal: f.allowLocalNetwork,
            writable: f.writable,
            writableLocked: f.writableLocked,
            seedRemove: f.seedRemove,
            seedRatio: f.seedRatio,
            seedHours: f.seedHours,
            seedDelete: f.seedDeleteFiles,
            categoryAfterImport: f.categoryAfterImport,
            ownOnly: f.ownOnly,
            clearFinished: f.clearFinished,
            seeding: DownloaderSeedingKind(rawValue: key(f.seedingFields)) ?? .nothing,
            usenet: f.usenet,
            canSave: f.canSave,
            canFetchKey: f.canFetchKey,
            keyPageURL: URL(string: f.keyPageUrl),
            issues: Set(f.issues.compactMap { DownloaderIssueKind(rawValue: key($0)) })
        )
    }

    static func downloaders(_ s: SettingsDownloadersState) -> DownloadersInfo {
        let kinds = downloaderKinds(s)
        return DownloadersInfo(
            load: load(s.status),
            allowed: s.allowed,
            canEdit: s.canEdit,
            owner: s.owner,
            rows: s.rows.map { downloaderRow($0, kinds: kinds) },
            torrentKinds: kinds.filter { !$0.usenet },
            usenetKinds: kinds.filter { $0.usenet },
            form: s.form.map { downloaderForm($0, state: s, kinds: kinds) },
            formError: error(s.formError),
            saving: s.saving,
            formTesting: s.formTesting,
            formResult: downloaderTest(s.formResult),
            categories: DownloaderCategoriesInfo(
                loading: s.categories.loading,
                listed: s.categories.listed?.boolValue,
                categories: s.categories.categories
            ),
            key: DownloaderKeyInfo(
                fetching: s.key.fetching,
                loginShown: s.key.loginShown,
                user: s.key.user,
                pass: s.key.pass,
                note: s.key.note.flatMap { DownloaderKeyNoteKind(rawValue: key($0)) }
            ),
            canFetchKey: s.canFetchKey,
            message: s.message.map {
                DownloaderMessageInfo(
                    kind: DownloaderMessageKindID(rawValue: key($0.kind)) ?? .saved,
                    name: $0.name,
                    app: $0.app,
                    error: error($0.error)
                )
            },
            messageCount: Int(s.messageCount)
        )
    }

    private static var downloaderSwitches: [(DownloaderSwitchKind, SettingsDownloaderSwitch)] {
        [
            (.removeFailed, SettingsDownloaderSwitch.removeFailed), (.autoAdd, SettingsDownloaderSwitch.autoAdd),
            (.sabOutside, SettingsDownloaderSwitch.sabOutside), (.deleteFiles, SettingsDownloaderSwitch.deleteFiles),
            (.turnDown, SettingsDownloaderSwitch.turnDown), (.searchAgain, SettingsDownloaderSwitch.searchAgain),
            (.failDangerous, SettingsDownloaderSwitch.failDangerous), (.replaceVanished, SettingsDownloaderSwitch.replaceVanished),
            (.tryNext, SettingsDownloaderSwitch.tryNext),
        ]
    }

    private static var downloaderNumbers: [(DownloaderNumberKind, SettingsDownloaderNumber)] {
        [
            (.stalledAfter, SettingsDownloaderNumber.stalledAfter), (.stalledAct, SettingsDownloaderNumber.stalledAct),
            (.spacing, SettingsDownloaderNumber.spacing), (.deadMinutes, SettingsDownloaderNumber.deadMinutes),
            (.crawlKbps, SettingsDownloaderNumber.crawlKbps), (.crawlHours, SettingsDownloaderNumber.crawlHours),
        ]
    }

    static func downloaderRules(_ s: SettingsDownloaderRulesState) -> DownloaderRulesInfo {
        var flags: [DownloaderSwitchKind: Bool] = [:]
        for (kind, flag) in downloaderSwitches {
            flags[kind] = s.flag(flag: flag)
        }
        var numbers: [DownloaderNumberKind: String] = [:]
        for (kind, number) in downloaderNumbers {
            numbers[kind] = s.number(kind: number)
        }
        return DownloaderRulesInfo(
            load: load(s.status),
            allowed: s.allowed,
            canEdit: s.canEdit,
            flags: flags,
            numbers: numbers,
            deadOn: s.deadOn,
            dirty: s.dirty,
            canSave: s.canSave,
            saving: s.saving,
            saveError: error(s.saveError),
            issue: s.issue.flatMap { DownloaderRulesIssueKind(rawValue: key($0)) },
            savedCount: Int(s.savedCount)
        )
    }

    static func kotlinSwitch(_ k: DownloaderSwitchKind) -> SettingsDownloaderSwitch {
        downloaderSwitches.first { $0.0 == k }?.1 ?? SettingsDownloaderSwitch.removeFailed
    }

    static func kotlinNumber(_ k: DownloaderNumberKind) -> SettingsDownloaderNumber {
        downloaderNumbers.first { $0.0 == k }?.1 ?? SettingsDownloaderNumber.stalledAfter
    }

    static func kotlinKindField(_ k: DownloaderKindField) -> SettingsDownloaderKindField {
        switch k {
        case .movies: SettingsDownloaderKindField.movies
        case .shows: SettingsDownloaderKindField.shows
        case .music: SettingsDownloaderKindField.music
        }
    }
}
