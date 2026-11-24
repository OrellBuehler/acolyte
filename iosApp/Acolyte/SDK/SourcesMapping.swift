@preconcurrency import shared
import Foundation

extension Mapping {
    private static func cause(_ value: Any?) -> SourceCauseKind? {
        guard let value else { return nil }
        return SourceCauseKind(rawValue: key(value))
    }

    private static func outcome(_ o: SettingsSourceOutcome?) -> SourceOutcomeInfo? {
        guard let o else { return nil }
        return SourceOutcomeInfo(
            ok: o.ok,
            text: o.text,
            ms: Int(o.ms),
            items: Int(o.items),
            cause: cause(o.cause),
            botCheck: o.botCheck,
            error: error(o.error)
        )
    }

    static func sourceRow(_ r: SettingsSourceRow) -> SourceRowInfo {
        SourceRowInfo(
            id: r.id,
            name: r.name,
            formatLabel: r.formatLabel,
            format: r.format,
            protocolName: r.protocolName,
            priority: Int(r.priority),
            state: r.state,
            word: SourceWordKind(rawValue: key(r.word)) ?? .other,
            rawWord: r.rawWord,
            tone: tone(r.tone),
            note: r.note,
            noteBad: r.noteBad,
            showNote: r.showNote,
            until: r.until,
            botCheckHint: r.botCheckHint,
            cause: cause(r.cause),
            avgMs: Int(r.avgMs),
            calls24h: Int(r.calls24h),
            failures24h: Int(r.failures24h),
            lastSuccess: r.lastSuccess,
            lastFailure: r.lastFailure,
            feed: r.feed,
            autoSearch: r.autoSearch,
            handSearch: r.handSearch,
            testing: r.testing,
            busy: r.busy,
            test: outcome(r.test),
            isDisabled: r.isDisabled,
            canTryNow: r.canTryNow,
            showAvg: r.showAvg,
            showUntil: r.showUntil
        )
    }

    static func sourceNotice(_ n: SettingsSourcesNotice?) -> SourceNoticeInfo? {
        guard let n else { return nil }
        return SourceNoticeInfo(
            seq: Int(n.seq),
            kind: SourceNoticeKind(rawValue: key(n.kind)) ?? .saved,
            name: n.name,
            detail: n.detail,
            count: Int(n.count),
            added: Int(n.added),
            removed: Int(n.removed),
            changed: Int(n.changed),
            error: error(n.error)
        )
    }

    static func sourceForm(_ f: SettingsSourceForm) -> SourceFormInfo {
        SourceFormInfo(
            id: f.id,
            isNew: f.isNew,
            name: f.name,
            format: f.format,
            site: f.site,
            siteUser: f.siteUser,
            siteLogin: f.siteLogin,
            siteAgent: f.siteAgent,
            protocolName: f.protocolName,
            url: f.url,
            apiKey: f.apiKey,
            feed: f.feed,
            autoSearch: f.autoSearch,
            handSearch: f.handSearch,
            kinds: f.kinds,
            minSeeders: f.minSeeders,
            priority: f.priority,
            searchesPerWindow: f.searchesPerWindow,
            windowHours: f.windowHours,
            gapMs: f.gapMs,
            timeoutSeconds: f.timeoutSeconds,
            allowLocalNetwork: f.allowLocalNetwork,
            proxy: f.proxy,
            proxyPassword: f.proxyPassword,
            proxyAllowLocalNetwork: f.proxyAllowLocalNetwork,
            tags: f.tags,
            multiLanguages: f.multiLanguages,
            noLanguageLanguages: f.noLanguageLanguages,
            downloaderId: f.downloaderId,
            sendGone: f.sendGone,
            originalProtocol: f.originalProtocol,
            originalDownloaderId: f.originalDownloaderId,
            isSite: f.isSite,
            isRss: f.isRss,
            showSiteFormat: f.showSiteFormat,
            formatLocked: f.formatLocked,
            siteLocked: f.siteLocked,
            showLoginChoice: f.showLoginChoice,
            showUser: f.showUser,
            showAgent: f.showAgent,
            showProtocol: f.showProtocol,
            showSearchUse: f.showSearchUse,
            showSeeders: f.showSeeders,
            sendProtocol: f.sendProtocol,
            keyKind: SourceKeyKindInfo(rawValue: key(f.keyKind)) ?? .none,
            addressHint: SourceAddressHintKind(rawValue: key(f.addressHint)) ?? .feed,
            newestListWorks: f.newestListWorks,
            canSave: f.canSave,
            problems: f.problems().compactMap { SourceProblemKind(rawValue: key($0)) }
        )
    }

    static func kotlinSourceForm(_ f: SourceFormInfo) -> SettingsSourceForm {
        SettingsSourceForm(
            id: f.id,
            isNew: f.isNew,
            name: f.name,
            format: f.format,
            site: f.site,
            siteUser: f.siteUser,
            siteLogin: f.siteLogin,
            siteAgent: f.siteAgent,
            protocolName: f.protocolName,
            url: f.url,
            apiKey: f.apiKey,
            feed: f.feed,
            autoSearch: f.autoSearch,
            handSearch: f.handSearch,
            kinds: f.kinds,
            minSeeders: f.minSeeders,
            priority: f.priority,
            searchesPerWindow: f.searchesPerWindow,
            windowHours: f.windowHours,
            gapMs: f.gapMs,
            timeoutSeconds: f.timeoutSeconds,
            allowLocalNetwork: f.allowLocalNetwork,
            proxy: f.proxy,
            proxyPassword: f.proxyPassword,
            proxyAllowLocalNetwork: f.proxyAllowLocalNetwork,
            tags: f.tags,
            multiLanguages: f.multiLanguages,
            noLanguageLanguages: f.noLanguageLanguages,
            downloaderId: f.downloaderId,
            sendGone: f.sendGone,
            originalProtocol: f.originalProtocol,
            originalDownloaderId: f.originalDownloaderId
        )
    }

    static func sources(_ s: SettingsSourcesState) -> SourcesInfo {
        SourcesInfo(
            load: load(s.status),
            allowed: s.allowed,
            canEdit: s.canEdit,
            partOff: s.partOff,
            rows: s.rows.map { sourceRow($0) },
            downloaders: s.downloaders?.map { SourceDownloaderInfo(id: $0.id, name: $0.name, protocolName: $0.protocolName) },
            editing: s.editing.map { sourceForm($0) },
            editToken: Int(s.editToken),
            saving: s.saving,
            saveError: error(s.saveError),
            notice: sourceNotice(s.notice),
            hasRows: s.hasRows,
            canAdd: s.canAdd
        )
    }

    static func kotlinBridgeDraft(_ d: BridgeDraftInfo) -> SettingsBridgeDraft {
        SettingsBridgeDraft(
            kind: d.kind,
            url: d.url,
            key: d.key,
            callbackUrl: d.callbackUrl,
            newKeys: d.newKeys,
            sonarrKey: d.sonarrKey,
            radarrKey: d.radarrKey,
            editingId: d.editingId
        )
    }

    private static func bridgeDraft(_ d: SettingsBridgeDraft) -> BridgeDraftInfo {
        BridgeDraftInfo(
            kind: d.kind,
            url: d.url,
            key: d.key,
            callbackUrl: d.callbackUrl,
            newKeys: d.newKeys,
            sonarrKey: d.sonarrKey,
            radarrKey: d.radarrKey,
            editingId: d.editingId
        )
    }

    private static func bridgeForm(_ f: SettingsBridgeFormState) -> BridgeFormInfo {
        let result = f.result.map {
            BridgeResultInfo(ok: $0.ok, version: $0.version, siteCount: Int($0.siteCount), sites: $0.sites, text: $0.text)
        }
        return BridgeFormInfo(
            initial: bridgeDraft(f.initial),
            testing: f.testing,
            connecting: f.connecting,
            result: result,
            error: error(f.error),
            kindError: f.kindError,
            urlError: f.urlError,
            keyError: f.keyError,
            callbackError: f.callbackError,
            keysNeeded: f.keysNeeded
        )
    }

    static func bridges(_ s: SettingsBridgeState) -> BridgesInfo {
        let rows = s.bridges.map {
            BridgeRowInfo(
                id: $0.id,
                kind: $0.kind,
                url: $0.url,
                callbackUrl: $0.callbackUrl,
                lastCheck: $0.lastCheck,
                lastError: $0.lastError,
                sites: Int($0.sites),
                sources: Int($0.sources)
            )
        }
        let keys = s.madeKeys.map { BridgeKeyInfo(side: $0.key, key: $0.value) }.sorted { $0.side < $1.side }
        return BridgesInfo(
            load: load(s.status),
            canEdit: s.canEdit,
            bridges: rows,
            form: s.form.map { bridgeForm($0) },
            formToken: Int(s.formToken),
            syncing: s.syncing,
            removing: s.removing,
            removeError: error(s.removeError),
            madeKeys: keys,
            notice: sourceNotice(s.notice),
            changed: Int(s.changed)
        )
    }

    static func sourceCheck(_ s: SettingsSourceCheckState) -> SourceCheckInfo {
        let result = s.result.map { r in
            SourceCheckResultInfo(
                lines: r.lines.map {
                    SourceCheckLineInfo(
                        id: $0.id,
                        name: $0.name,
                        word: SourceCheckWordKind(rawValue: key($0.word)) ?? .notConnected,
                        tone: tone($0.tone),
                        count: Int($0.count),
                        ms: Int($0.ms),
                        detail: $0.detail
                    )
                },
                total: Int(r.total),
                connected: Int(r.connected),
                paused: Int(r.paused),
                failed: Int(r.failed)
            )
        }
        return SourceCheckInfo(running: s.running, result: result, error: error(s.error))
    }

    static func sourceHealth(_ s: SettingsSourceHealthState) -> SourceHealthInfo {
        SourceHealthInfo(
            load: load(s.status),
            allowed: s.allowed,
            canEdit: s.canEdit,
            row: s.row.map { sourceRow($0) },
            days: Int(s.days),
            bars: s.bars.map {
                SourceBarInfo(
                    day: $0.day,
                    calls: Int($0.calls),
                    failures: Int($0.failures),
                    answered: Int($0.answered),
                    grabs: Int($0.grabs),
                    avgMs: Int($0.avgMs),
                    callsPercent: Int($0.callsPercent),
                    failedPercent: Int($0.failedPercent),
                    grabsPercent: Int($0.grabsPercent),
                    timePercent: Int($0.timePercent)
                )
            },
            recent: s.recent.map {
                SourceCallInfo(
                    at: $0.at,
                    op: SourceOpKind(rawValue: key($0.op)) ?? .other,
                    rawOp: $0.rawOp,
                    ok: $0.ok,
                    ms: Int($0.ms),
                    errorKind: SourceErrorWordKind(rawValue: key($0.errorKind)) ?? .other,
                    rawError: $0.rawError
                )
            },
            totalCalls: Int(s.totalCalls),
            totalFailures: Int(s.totalFailures),
            totalGrabs: Int(s.totalGrabs),
            trying: s.trying,
            hasCalls: s.hasCalls,
            hasTimes: s.hasTimes,
            choices: s.choices.map { Int($0.intValue) }
        )
    }

    static func builtInSites() -> [SourceSiteInfo] {
        SettingsSourceSites.shared.all.map { SourceSiteInfo(id: $0.id, name: $0.name) }
    }
}
