@preconcurrency import shared
import Foundation

extension Mapping {
    static func statusRow(_ r: SettingsStatusRow) -> StatusRowInfo {
        StatusRowInfo(
            group: StatusGroupID(rawValue: key(r.group)) ?? .jobs,
            key: r.key,
            title: StatusTitleID(rawValue: key(r.title)) ?? .plain,
            name: r.name,
            tone: tone(r.tone),
            word: StatusWordID(rawValue: key(r.word)) ?? .off,
            reasonKind: StatusReasonID(rawValue: key(r.reasonKind)) ?? .none,
            reason: r.reason,
            at: Format.date(iso: r.at),
            fix: r.fix.flatMap { StatusFixID(rawValue: key($0)) },
            checkKind: r.check.flatMap { StatusCheckKindID(rawValue: key($0.kind)) },
            lastRun: Format.date(iso: r.lastRun),
            needsAttention: r.needsAttention
        )
    }

    static func statusDue(_ d: SettingsStatusDue) -> StatusDueInfo {
        StatusDueInfo(
            kind: StatusDueKindID(rawValue: key(d.kind)) ?? .none,
            unit: StatusUnitID(rawValue: key(d.unit)) ?? .minutes,
            amount: Int(d.amount)
        )
    }

    static func statusKind(_ k: SettingsStatusKind) -> StatusKindInfo {
        StatusKindInfo(
            group: StatusGroupID(rawValue: key(k.group)) ?? .sources,
            rows: k.rows.map { statusRow($0) },
            okCount: Int(k.okCount),
            onCount: Int(k.onCount),
            marker: StatusMarkerID(rawValue: key(k.marker)) ?? .none,
            summary: StatusSummaryID(rawValue: key(k.summary)) ?? .nothing,
            summaryCount: Int(k.summaryCount),
            summaryWord: k.summaryWord.flatMap { StatusWordID(rawValue: key($0)) },
            fix: StatusFixID(rawValue: key(k.fix)) ?? .sources
        )
    }

    static func statusDisk(_ d: SettingsStatusDiskView) -> StatusDiskInfo {
        StatusDiskInfo(
            path: d.path,
            label: d.label,
            kind: StatusDiskKindID(rawValue: key(d.kind)) ?? .other,
            freeBytes: d.freeBytes,
            totalBytes: d.totalBytes,
            usedPercent: Int(d.usedPercent),
            level: StatusDiskLevelID(rawValue: key(d.level)) ?? .normal,
            problem: d.problem.map { statusRow($0) }
        )
    }

    static func statusInstall(_ i: SettingsStatusInstallView) -> StatusInstallInfo {
        StatusInstallInfo(
            available: i.available,
            mode: StatusModeID(rawValue: key(i.mode)) ?? .off,
            modeLocked: i.modeLocked,
            checkOn: i.checkOn,
            fromUpdate: i.fromUpdate,
            previous: i.previous,
            wentBack: i.wentBack,
            wentBackTo: i.wentBackTo,
            wentBackLeft: i.wentBackLeft,
            rollback: i.rollback,
            execError: i.execError,
            phase: StatusPhaseID(rawValue: key(i.phase)) ?? .idle,
            progressVersion: i.progressVersion,
            progressMessage: i.progressMessage,
            ready: i.ready,
            oneWay: i.oneWay
        )
    }

    static func statusAbout(_ a: SettingsStatusAbout) -> StatusAboutInfo {
        StatusAboutInfo(
            version: a.version,
            go: a.go,
            os: a.os,
            container: a.container,
            sqlite: a.sqlite,
            migration: Int(a.migration),
            configDir: a.configDir,
            startDir: a.startDir,
            uptimeSeconds: a.uptimeSeconds,
            hasDetails: a.hasDetails,
            hasServer: a.hasServer,
            writes: a.writes,
            writeWaitP95Ms: a.writeWaitP95Ms,
            urlBase: a.urlBase,
            liveClients: Int(a.liveClients),
            loadedAt: a.loadedAt
        )
    }

    static func statusNotice(_ n: SettingsStatusNotice) -> StatusNoticeInfo {
        StatusNoticeInfo(
            id: Int(n.id),
            kind: StatusNoticeKindID(rawValue: key(n.kind)) ?? .checksRan,
            row: n.row.map { statusRow($0) },
            detail: n.detail,
            error: error(n.error)
        )
    }

    static func health(_ s: HealthState) -> HealthInfo {
        HealthInfo(
            load: load(s.status),
            allowed: s.allowed,
            canAct: s.canAct,
            canManageUpdates: s.canManageUpdates,
            canBundle: s.canBundle,
            headline: StatusHeadlineInfo(
                kind: StatusHeadlineID(rawValue: key(s.headline.kind)) ?? .unknown,
                checks: Int(s.headline.checks),
                jobs: Int(s.headline.jobs)
            ),
            failedReads: s.failedReads.compactMap { StatusReadID(rawValue: key($0)) },
            problems: s.problems.map { statusRow($0) },
            nothingLinked: s.nothingLinked,
            linkedUnreadable: s.linkedUnreadable,
            kinds: s.kinds.map { statusKind($0) },
            defaultKind: StatusGroupID(rawValue: key(s.defaultKind)) ?? .sources,
            disks: s.disks.map { statusDisk($0) },
            disksLoaded: s.disksLoaded,
            backups: s.backups.map { statusRow($0) },
            keyKit: s.keyKit.map { statusRow($0) },
            jobLines: s.jobLines.map { StatusJobLineInfo(row: statusRow($0.row), due: statusDue($0.due)) },
            jobsBadge: StatusMarkerID(rawValue: key(s.jobsBadge)) ?? .none,
            running: s.running.map { statusRunning($0) },
            nextUp: s.nextUp.map { statusUpcoming($0) },
            finished: s.finished.map { statusFinished($0) },
            updateOn: s.updateOn,
            updateVersion: s.updateVersion,
            updateLatest: s.updateLatest,
            updateCheckedAt: Format.date(iso: s.updateCheckedAt),
            updateProblem: s.updateProblem,
            updateFailed: s.updateFailed,
            standing: StatusStandingID(rawValue: key(s.standing)) ?? .loading,
            updateNotes: s.updateNotes,
            updateLink: s.updateLink,
            updateAvailable: s.updateAvailable,
            checkingUpdate: s.checkingUpdate,
            install: s.install.map { statusInstall($0) },
            installReady: s.installReady,
            installCanGoBack: s.installCanGoBack,
            installMoving: s.installMoving,
            installBusy: s.installBusy,
            about: statusAbout(s.about),
            aboutLoad: StatusAboutLoadID(rawValue: key(s.aboutLoad)) ?? .loading,
            bundleBusy: s.bundleBusy,
            bundle: s.bundle.map { StatusFileInfo(path: $0.path, name: $0.name) },
            busy: s.busy,
            notice: s.notice.map { statusNotice($0) }
        )
    }

    static func statusRunning(_ r: SettingsStatusRunning) -> StatusRunningInfo {
        StatusRunningInfo(
            id: r.id,
            name: r.name,
            queued: r.queued,
            progress: r.progress,
            percent: Int(r.percent),
            hasProgress: r.hasProgress,
            note: r.note
        )
    }

    static func statusUpcoming(_ u: SettingsStatusUpcoming) -> StatusUpcomingInfo {
        StatusUpcomingInfo(
            key: u.key,
            name: u.name,
            tone: tone(u.tone),
            word: StatusWordID(rawValue: key(u.word)) ?? .off,
            due: statusDue(u.due)
        )
    }

    static func statusFinished(_ f: SettingsStatusFinished) -> StatusFinishedInfo {
        StatusFinishedInfo(
            id: f.id,
            name: f.name,
            tone: tone(f.tone),
            word: StatusWordID(rawValue: key(f.word)) ?? .off,
            endedAt: Format.date(iso: f.endedAt)
        )
    }

    static func kotlinStatusMode(_ m: StatusModeID) -> SettingsStatusMode {
        switch m {
        case .off: SettingsStatusMode.off
        case .ask: SettingsStatusMode.ask
        case .auto: SettingsStatusMode.auto
        }
    }

    static func healthChecks(_ s: SettingsHealthChecksState) -> HealthChecksInfo {
        HealthChecksInfo(
            load: load(s.status),
            allowed: s.allowed,
            canSeeChecks: s.canSeeChecks,
            canEdit: s.canEdit,
            worst: tone(s.worst),
            checks: s.checks.map { healthChecksRow($0) },
            sync: s.sync.map { HealthChecksSyncInfo(id: $0.id, name: $0.name, degraded: $0.degraded, seenAt: Format.date(iso: $0.seenAt)) },
            hasSettings: s.hasSettings,
            days: s.days.map { $0.intValue },
            updateDays: Int(s.draft.updateCheckEveryDays),
            feed: s.draft.updateCheckUrl,
            defaultFeed: s.saved?.updateCheckDefault ?? "",
            local: s.draft.updateCheckLocal,
            prerelease: s.draft.updatePrerelease,
            everyText: s.everyText,
            guardText: s.guardText,
            lowText: s.lowText,
            everyValid: s.everyValid,
            guardValid: s.guardValid,
            lowValid: s.lowValid,
            valid: s.valid,
            version: s.version,
            running: s.running,
            saving: s.saving,
            saveError: error(s.saveError),
            dirty: s.dirty,
            canSave: s.canSave,
            noticeId: Int(s.notice?.id ?? 0),
            noticeSaved: s.notice.map { key($0.kind) == "saved" } ?? false,
            noticeRunFailed: s.notice.map { key($0.kind) == "runfailed" } ?? false,
            noticeError: error(s.notice?.error)
        )
    }

    static func healthChecksRow(_ r: SettingsHealthChecksRow) -> HealthChecksRowInfo {
        HealthChecksRowInfo(
            key: r.key,
            kind: HealthChecksKindID(rawValue: key(r.kind)) ?? .other,
            label: r.label,
            subject: r.subject,
            message: r.message,
            tone: tone(r.tone),
            since: Format.date(iso: r.since)
        )
    }

    static func kotlinHealthChecksField(_ f: HealthChecksFieldID) -> SettingsHealthChecksField {
        switch f {
        case .everyMinutes: SettingsHealthChecksField.everyMinutes
        case .flapGuard: SettingsHealthChecksField.flapGuard
        case .lowSpace: SettingsHealthChecksField.lowSpace
        }
    }
}
