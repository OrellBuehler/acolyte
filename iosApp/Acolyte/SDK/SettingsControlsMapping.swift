@preconcurrency import shared
import Foundation

extension Mapping {
    static func controlKind(_ value: Any) -> SettingsControlsActionKind {
        SettingsControlsActionKind(rawValue: key(value)) ?? .partOn
    }

    static func kotlinStageAction(_ action: SettingsControlsStageActionKind) -> SettingsControlsStageAction {
        switch action {
        case .pause: SettingsControlsStageAction.pause
        case .resume: SettingsControlsStageAction.resume
        case .restart: SettingsControlsStageAction.restart
        }
    }

    static func kotlinControlKind(_ kind: SettingsControlsActionKind) -> SettingsControlsKind {
        switch kind {
        case .partOn: SettingsControlsKind.partOn
        case .partOff: SettingsControlsKind.partOff
        case .partRestart: SettingsControlsKind.partRestart
        case .stagePause: SettingsControlsKind.stagePause
        case .stageResume: SettingsControlsKind.stageResume
        case .stageRestart: SettingsControlsKind.stageRestart
        case .linkReconnect: SettingsControlsKind.linkReconnect
        case .linkTest: SettingsControlsKind.linkTest
        case .appRestart: SettingsControlsKind.appRestart
        case .appSafeMode: SettingsControlsKind.appSafeMode
        case .appShutdown: SettingsControlsKind.appShutdown
        }
    }

    static func controlRun(_ value: Any) -> SettingsControlsRunKind {
        SettingsControlsRunKind(rawValue: key(value)) ?? .running
    }

    static func controlEvery(_ value: SettingsControlsEvery) -> SettingsControlsEveryInfo {
        SettingsControlsEveryInfo(kind: SettingsControlsEveryKind(rawValue: key(value.unit)) ?? .always, count: Int(value.count))
    }

    static func controlWhen(_ value: SettingsControlsNext) -> SettingsControlsWhenInfo {
        SettingsControlsWhenInfo(kind: SettingsControlsWhenKind(rawValue: key(value.kind)) ?? .unplanned, count: Int(value.count))
    }

    static func controlAsk(_ value: SettingsControlsAsk?) -> SettingsControlsAskInfo? {
        guard let value else { return nil }
        return SettingsControlsAskInfo(
            kind: controlKind(value.kind),
            name: value.name,
            text: value.text,
            expiresInSeconds: Int(value.expiresInSeconds)
        )
    }

    static func controlNotice(_ value: SettingsControlsNotice?) -> SettingsControlsNoticeInfo? {
        guard let value else { return nil }
        return SettingsControlsNoticeInfo(
            id: Int(value.id),
            kind: controlKind(value.kind),
            name: value.name,
            ok: value.ok,
            detail: value.detail,
            error: error(value.error)
        )
    }

    static func controlPartRow(_ row: SettingsControlsPartRow) -> SettingsControlsPartRowInfo {
        SettingsControlsPartRowInfo(
            key: row.key,
            part: row.part,
            name: row.name,
            on: row.on,
            run: controlRun(row.run),
            tone: tone(row.tone),
            reason: row.reason
        )
    }

    static func controlStageRow(_ row: SettingsControlsStageRow) -> SettingsControlsStageRowInfo {
        SettingsControlsStageRowInfo(
            key: row.key,
            stage: row.stage,
            name: row.name,
            run: controlRun(row.run),
            tone: tone(row.tone),
            reason: row.reason,
            pausable: row.pausable,
            paused: row.paused
        )
    }

    static func controlLinkRow(_ row: SettingsControlsLinkRow) -> SettingsControlsLinkRowInfo {
        SettingsControlsLinkRowInfo(
            key: row.key,
            kind: SettingsControlsLinkType(rawValue: key(row.kind)) ?? .other,
            name: row.name,
            run: controlRun(row.run),
            tone: tone(row.tone),
            reason: row.reason,
            canTest: row.canTest
        )
    }

    static func controlEventRow(_ row: SettingsControlsEventRow) -> SettingsControlsEventRowInfo {
        SettingsControlsEventRowInfo(id: row.id, summary: row.summary, at: Format.date(iso: row.at))
    }

    static func settingsControlsParts(_ s: SettingsControlsPartsState) -> SettingsControlsPartsInfo {
        let parts = s.parts.map { controlPartRow($0) }
        let stages = s.stages.map { controlStageRow($0) }
        let links = s.links.map { controlLinkRow($0) }
        let events = s.events.map { controlEventRow($0) }
        return SettingsControlsPartsInfo(
            load: load(s.status),
            allowed: s.allowed,
            safeMode: s.safeMode,
            parts: parts,
            stages: stages,
            links: links,
            events: events,
            busyKey: s.busyKey,
            ask: controlAsk(s.ask),
            notice: controlNotice(s.notice)
        )
    }

    static func controlStopped(_ value: SettingsControlsStopped?) -> SettingsControlsStoppedInfo? {
        guard let value else { return nil }
        return SettingsControlsStoppedInfo(
            kind: controlKind(value.kind),
            text: value.text,
            wait: SettingsControlsWaitKind(rawValue: key(value.wait)) ?? .stopping
        )
    }

    static func settingsControlsDanger(_ s: SettingsControlsDangerState) -> SettingsControlsDangerInfo {
        let busyKind: SettingsControlsActionKind? = s.busyKind.map { controlKind($0) }
        return SettingsControlsDangerInfo(
            allowed: s.allowed,
            busyKind: busyKind,
            ask: controlAsk(s.ask),
            notice: controlNotice(s.notice),
            stopped: controlStopped(s.stopped)
        )
    }

    static func controlPathRow(_ row: SettingsControlsPathRow) -> SettingsControlsPathRowInfo {
        SettingsControlsPathRowInfo(
            name: row.name,
            path: row.path,
            size: row.size,
            free: row.free,
            kind: row.kind,
            freeKnown: row.freeKnown
        )
    }

    static func controlProxyForm(_ form: SettingsControlsProxyForm) -> SettingsControlsProxyFormInfo {
        SettingsControlsProxyFormInfo(
            type: form.type,
            host: form.host,
            port: form.port,
            username: form.username,
            password: form.password,
            bypass: form.bypass,
            bypassLocal: form.bypassLocal
        )
    }

    static func controlStorage(_ value: SettingsControlsStorage?) -> SettingsControlsStorageInfo? {
        guard let value else { return nil }
        return SettingsControlsStorageInfo(
            app: value.app.map { controlPathRow($0) },
            folders: value.folders.map { controlPathRow($0) },
            savePath: value.savePath
        )
    }

    static func controlUrlBase(_ value: SettingsControlsUrlBase?) -> SettingsControlsUrlBaseInfo? {
        guard let value else { return nil }
        return SettingsControlsUrlBaseInfo(saved: value.saved, inUse: value.inUse, restartNeeded: value.restartNeeded)
    }

    static func controlSaved(_ value: SettingsControlsSaved?) -> SettingsControlsSavedInfo? {
        guard let value else { return nil }
        return SettingsControlsSavedInfo(
            id: Int(value.id),
            kind: SettingsControlsSavedNoteKind(rawValue: key(value.kind)) ?? .failed,
            value: Int(value.value),
            restartNeeded: value.restartNeeded,
            error: error(value.error)
        )
    }

    static func settingsControlsThisApp(_ s: SettingsControlsThisAppState) -> SettingsControlsThisAppInfo {
        let routine: [Int] = s.routineChoices.map { $0.intValue }
        let other: [Int] = s.otherChoices.map { $0.intValue }
        let cache: [Int] = s.cacheChoices.map { $0.intValue }
        return SettingsControlsThisAppInfo(
            load: load(s.status),
            canSettings: s.canSettings,
            isOwner: s.isOwner,
            version: s.version,
            uptime: s.uptime,
            schema: Int(s.schema),
            liveClients: Int(s.liveClients),
            uiLoaded: s.uiLoaded,
            historyRoutineDays: Int(s.historyRoutineDays),
            historyOtherDays: Int(s.historyOtherDays),
            cacheGb: Int(s.cacheGb),
            countInstall: s.countInstall,
            uiBusy: s.uiBusy,
            routineChoices: routine,
            otherChoices: other,
            cacheChoices: cache,
            storage: controlStorage(s.storage),
            storageProof: s.storageProof,
            storageError: error(s.storageError),
            urlBaseAvailable: s.urlBaseAvailable,
            urlBase: controlUrlBase(s.urlBase),
            urlBaseDraft: s.urlBaseDraft,
            urlBaseSaving: s.urlBaseSaving,
            urlBaseError: error(s.urlBaseError),
            canSaveUrlBase: s.canSaveUrlBase,
            proxyAvailable: s.proxyAvailable,
            proxyLoaded: s.proxyLoaded,
            proxy: controlProxyForm(s.proxy),
            proxyPasswordSaved: s.proxyPasswordSaved,
            proxySaving: s.proxySaving,
            proxyError: error(s.proxyError),
            proxyOn: s.proxyOn,
            proxyValid: s.proxyValid,
            canSaveProxy: s.canSaveProxy,
            saved: controlSaved(s.saved)
        )
    }

    static func controlScheduleRow(_ row: SettingsControlsScheduleRow) -> SettingsControlsScheduleRowInfo {
        SettingsControlsScheduleRowInfo(
            kind: row.kind,
            label: row.label,
            about: row.about,
            dot: SettingsControlsDotKind(rawValue: key(row.dot)) ?? .off,
            every: controlEvery(row.every),
            lastAt: Format.date(iso: row.lastAt),
            took: row.took,
            next: controlWhen(row.next),
            failed: row.failed,
            failedError: row.failedError,
            reason: SettingsControlsReasonKind(rawValue: key(row.reason)) ?? .clear,
            reasonLinks: row.reasonLinks,
            pageKey: row.pageKey,
            canRun: row.canRun,
            canChange: row.canChange,
            spinning: row.spinning,
            enabled: row.enabled,
            toggleable: row.toggleable,
            stale: SettingsControlsStaleKind(rawValue: key(row.stale)) ?? .fresh,
            editing: row.editing
        )
    }

    static func controlEditor(_ editor: SettingsControlsEditor?) -> SettingsControlsEditorInfo? {
        guard let editor else { return nil }
        return SettingsControlsEditorInfo(
            kind: editor.kind,
            label: editor.label,
            about: editor.about,
            unit: SettingsControlsUnitKind(rawValue: key(editor.unit)) ?? .minutes,
            amount: editor.amount,
            minAmount: Int(editor.minAmount),
            maxAmount: Int(editor.maxAmount),
            valid: editor.valid,
            toggleable: editor.toggleable,
            enabled: editor.enabled,
            pageKey: editor.pageKey,
            saving: editor.saving,
            busy: editor.busy,
            error: error(editor.error)
        )
    }

    static func controlRunning(_ row: SettingsControlsRunningRow) -> SettingsControlsRunningRowInfo {
        SettingsControlsRunningRowInfo(kind: row.kind, label: row.label, running: row.running)
    }

    static func controlGroup(_ group: SettingsControlsScheduleGroupRows) -> SettingsControlsGroupRowsInfo {
        let rows = group.rows.map { controlScheduleRow($0) }
        return SettingsControlsGroupRowsInfo(
            group: SettingsControlsGroupKind(rawValue: key(group.group)) ?? .upkeep,
            rows: rows
        )
    }

    static func controlLoop(_ loop: SettingsControlsLoopRow) -> SettingsControlsLoopRowInfo {
        SettingsControlsLoopRowInfo(
            key: loop.key,
            name: loop.name,
            reason: loop.reason,
            run: controlRun(loop.run),
            tone: tone(loop.tone),
            dot: SettingsControlsDotKind(rawValue: key(loop.dot)) ?? .off,
            every: controlEvery(loop.every),
            lastAt: Format.date(iso: loop.lastAt),
            next: controlWhen(loop.next)
        )
    }

    static func controlScheduleNote(_ value: SettingsControlsScheduleNotice?) -> SettingsControlsScheduleNoteInfo? {
        guard let value else { return nil }
        return SettingsControlsScheduleNoteInfo(
            id: Int(value.id),
            kind: SettingsControlsScheduleNoteKind(rawValue: key(value.kind)) ?? .finished,
            label: value.label,
            detail: value.detail,
            reason: SettingsControlsReasonKind(rawValue: key(value.reason)) ?? .clear,
            every: controlEvery(value.every),
            error: error(value.error)
        )
    }

    static func settingsControlsSchedules(_ s: SettingsControlsSchedulesState) -> SettingsControlsSchedulesInfo {
        let running = s.running.map { controlRunning($0) }
        let groups = s.groups.map { controlGroup($0) }
        let loops = s.loops.map { controlLoop($0) }
        return SettingsControlsSchedulesInfo(
            load: load(s.status),
            safeMode: s.safeMode,
            canRun: s.canRun,
            canChange: s.canChange,
            running: running,
            groups: groups,
            loops: loops,
            editor: controlEditor(s.editor),
            busyKind: s.busyKind,
            notice: controlScheduleNote(s.notice)
        )
    }
}
