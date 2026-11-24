import Foundation

enum SettingsControlsText {
    private static let detailStatuses: Set<Int> = [400, 403, 409, 422, 502, 503]

    static func errorText(_ error: ErrorInfo) -> String {
        guard detailStatuses.contains(error.status), !error.message.isEmpty else { return error.localized }
        return error.localized + "\n" + error.message
    }

    static func label(_ kind: SettingsControlsActionKind, _ name: String) -> String {
        switch kind {
        case .partOn: L10n.format("settings.controls.label.part.on", name)
        case .partOff: L10n.format("settings.controls.label.part.off", name)
        case .partRestart, .stageRestart: L10n.format("settings.controls.label.restart", name)
        case .stagePause: L10n.format("settings.controls.label.pause", name)
        case .stageResume: L10n.format("settings.controls.label.resume", name)
        case .linkReconnect: L10n.format("settings.controls.label.reconnect", name)
        case .linkTest: L10n.format("settings.controls.label.test", name)
        case .appRestart: L10n.tr("settings.controls.label.app.restart")
        case .appSafeMode: L10n.tr("settings.controls.label.app.safe")
        case .appShutdown: L10n.tr("settings.controls.label.app.shutdown")
        }
    }

    static func yes(_ kind: SettingsControlsActionKind, _ name: String) -> String {
        switch kind {
        case .partOn: L10n.format("settings.controls.yes.part.on", name)
        case .partOff: L10n.format("settings.controls.yes.part.off", name)
        case .partRestart, .stageRestart: L10n.format("settings.controls.yes.restart", name)
        case .stagePause: L10n.format("settings.controls.yes.pause", name)
        case .stageResume: L10n.format("settings.controls.yes.resume", name)
        case .linkReconnect: L10n.format("settings.controls.yes.reconnect", name)
        case .linkTest: L10n.format("settings.controls.yes.test", name)
        case .appRestart: L10n.tr("settings.controls.yes.app.restart")
        case .appSafeMode: L10n.tr("settings.controls.yes.app.safe")
        case .appShutdown: L10n.tr("settings.controls.yes.app.shutdown")
        }
    }

    private static func failed(_ kind: SettingsControlsActionKind, _ name: String) -> String {
        switch kind {
        case .partOn: L10n.format("settings.controls.failed.part.on", name)
        case .partOff: L10n.format("settings.controls.failed.part.off", name)
        case .partRestart, .stageRestart: L10n.format("settings.controls.failed.restart", name)
        case .stagePause: L10n.format("settings.controls.failed.pause", name)
        case .stageResume: L10n.format("settings.controls.failed.resume", name)
        case .linkReconnect, .linkTest: L10n.format("settings.controls.failed.connect", name)
        case .appRestart: L10n.tr("settings.controls.failed.app.restart")
        case .appSafeMode: L10n.tr("settings.controls.failed.app.safe")
        case .appShutdown: L10n.tr("settings.controls.failed.app.shutdown")
        }
    }

    private static func done(_ kind: SettingsControlsActionKind, _ name: String) -> String {
        switch kind {
        case .partOn: L10n.format("settings.controls.done.part.on", name)
        case .partOff: L10n.format("settings.controls.done.part.off", name)
        case .partRestart, .stageRestart: L10n.format("settings.controls.done.restarted", name)
        case .stagePause: L10n.format("settings.controls.done.paused", name)
        case .stageResume: L10n.format("settings.controls.done.resumed", name)
        case .linkReconnect, .linkTest: L10n.format("settings.controls.done.connected", name)
        case .appRestart, .appSafeMode, .appShutdown: L10n.tr("settings.controls.done.stopping")
        }
    }

    static func notice(_ notice: SettingsControlsNoticeInfo) -> String {
        let isLink = notice.kind == .linkReconnect || notice.kind == .linkTest
        let title = notice.ok ? done(notice.kind, notice.name) : failed(notice.kind, notice.name)
        var body = notice.detail
        if let error = notice.error {
            body = errorText(error)
        } else if isLink && notice.ok && body.isEmpty {
            body = L10n.tr("settings.controls.connected.plain")
        }
        return body.isEmpty ? title : title + "\n" + body
    }

    static func run(_ run: SettingsControlsRunKind) -> String {
        switch run {
        case .off: L10n.tr("settings.controls.run.off")
        case .starting: L10n.tr("settings.controls.run.starting")
        case .running: L10n.tr("settings.controls.run.running")
        case .retrying: L10n.tr("settings.controls.run.retrying")
        case .failed: L10n.tr("settings.controls.run.failed")
        case .stuck: L10n.tr("settings.controls.run.stuck")
        }
    }

    static func linkType(_ type: SettingsControlsLinkType) -> String {
        switch type {
        case .source: L10n.tr("settings.controls.link.source")
        case .downloader: L10n.tr("settings.controls.link.downloader")
        case .mediaServer: L10n.tr("settings.controls.link.media.server")
        case .subtitleSite: L10n.tr("settings.controls.link.subtitle.site")
        case .other: L10n.tr("settings.controls.link.other")
        }
    }

    static func partName(_ part: String, _ fallback: String) -> String {
        switch part {
        case "movies": L10n.tr("settings.controls.part.movies")
        case "shows": L10n.tr("settings.controls.part.shows")
        case "music": L10n.tr("settings.controls.part.music")
        case "subtitles": L10n.tr("settings.controls.part.subtitles")
        case "wishlist": L10n.tr("settings.controls.part.wishlist")
        case "sources": L10n.tr("settings.controls.part.sources")
        default: fallback
        }
    }

    static func partStops(_ part: String) -> String? {
        switch part {
        case "movies": L10n.tr("settings.controls.stops.movies")
        case "shows": L10n.tr("settings.controls.stops.shows")
        case "music": L10n.tr("settings.controls.stops.music")
        case "subtitles": L10n.tr("settings.controls.stops.subtitles")
        case "wishlist": L10n.tr("settings.controls.stops.wishlist")
        case "sources": L10n.tr("settings.controls.stops.sources")
        default: nil
        }
    }

    static func partNeeds(_ part: String) -> String? {
        switch part {
        case "subtitles": L10n.tr("settings.controls.needs.subtitles")
        case "wishlist": L10n.tr("settings.controls.needs.wishlist")
        default: nil
        }
    }

    static func every(_ every: SettingsControlsEveryInfo) -> String {
        switch every.kind {
        case .always: L10n.tr("settings.controls.every.always")
        case .days: L10n.plural("settings.controls.every.days", every.count)
        case .hours: L10n.plural("settings.controls.every.hours", every.count)
        case .minutes: L10n.plural("settings.controls.every.minutes", every.count)
        case .seconds: L10n.plural("settings.controls.every.seconds", every.count)
        }
    }

    static func runs(_ every: SettingsControlsEveryInfo) -> String {
        switch every.kind {
        case .always: L10n.tr("settings.controls.runs.always")
        case .days: L10n.plural("settings.controls.runs.days", every.count)
        case .hours: L10n.plural("settings.controls.runs.hours", every.count)
        case .minutes: L10n.plural("settings.controls.runs.minutes", every.count)
        case .seconds: L10n.plural("settings.controls.runs.seconds", every.count)
        }
    }

    static func next(_ next: SettingsControlsWhenInfo) -> String {
        switch next.kind {
        case .unplanned: L10n.tr("settings.controls.next.none")
        case .turnedOff: L10n.tr("settings.controls.next.off")
        case .partOff: L10n.tr("settings.controls.next.part.off")
        case .safeMode: L10n.tr("settings.controls.next.safe.mode")
        case .running: L10n.tr("settings.controls.next.running")
        case .queued: L10n.tr("settings.controls.next.queued")
        case .now: L10n.tr("settings.controls.next.now")
        case .minutes: L10n.format("settings.controls.next.minutes", next.count)
        case .hours: L10n.format("settings.controls.next.hours", next.count)
        case .days: L10n.format("settings.controls.next.days", next.count)
        }
    }

    static func group(_ group: SettingsControlsGroupKind) -> String {
        switch group {
        case .library: L10n.tr("settings.controls.group.library")
        case .copies: L10n.tr("settings.controls.group.copies")
        case .metadata: L10n.tr("settings.controls.group.metadata")
        case .upkeep: L10n.tr("settings.controls.group.upkeep")
        }
    }

    static func reason(_ reason: SettingsControlsReasonKind) -> String {
        switch reason {
        case .clear, .nothing, .nobody: L10n.tr("settings.controls.skip.nothing")
        case .settingOff: L10n.tr("settings.controls.skip.setting.off")
        case .starting: L10n.tr("settings.controls.skip.starting")
        case .plexOff: L10n.tr("settings.controls.skip.plex.off")
        case .plexNobody: L10n.tr("settings.controls.skip.plex.nobody")
        }
    }

    static func stale(_ stale: SettingsControlsStaleKind) -> String? {
        switch stale {
        case .fresh: nil
        case .refreshOff: L10n.tr("settings.controls.stale.refresh")
        case .rescanOff: L10n.tr("settings.controls.stale.rescan")
        }
    }

    static func unit(_ unit: SettingsControlsUnitKind) -> String {
        switch unit {
        case .hours: L10n.tr("settings.controls.unit.hours")
        case .minutes: L10n.tr("settings.controls.unit.minutes")
        }
    }

    static func scheduleNote(_ note: SettingsControlsScheduleNoteInfo) -> String {
        let label = note.label
        let title: String = switch note.kind {
        case .started: L10n.format("settings.controls.sched.started", label)
        case .queued: L10n.format("settings.controls.sched.queued", label)
        case .startFailed: L10n.format("settings.controls.sched.start.failed", label)
        case .finished: L10n.format("settings.controls.sched.finished", label)
        case .nothing: L10n.format("settings.controls.sched.nothing", label)
        case .jobFailed: L10n.format("settings.controls.sched.job.failed", label)
        case .saved: L10n.format("settings.controls.sched.saved", label)
        case .autoOn: L10n.format("settings.controls.sched.auto.on", label)
        case .autoOff: L10n.format("settings.controls.sched.auto.off", label)
        case .saveFailed: L10n.tr("settings.controls.sched.save.failed")
        }
        var body = note.detail
        if let error = note.error {
            body = errorText(error)
        } else if note.kind == .nothing {
            body = reason(note.reason)
        } else if note.kind == .saved {
            body = runs(note.every)
        }
        return body.isEmpty ? title : title + "\n" + body
    }

    static func keep(_ days: Int) -> String {
        if days == 0 { return L10n.tr("settings.controls.keep.forever") }
        if days == 365 { return L10n.tr("settings.controls.keep.year") }
        return L10n.plural("settings.controls.keep.days", days)
    }

    static func saved(_ saved: SettingsControlsSavedInfo) -> String {
        switch saved.kind {
        case .history:
            return L10n.tr("settings.controls.saved.history")
        case .cache:
            return L10n.tr("settings.controls.saved.cache") + "\n" + L10n.format("settings.controls.saved.cache.up.to", saved.value)
        case .countOn:
            return L10n.tr("settings.controls.saved.counted")
        case .countOff:
            return L10n.tr("settings.controls.saved.not.counted")
        case .urlBase:
            let title = L10n.tr("settings.controls.saved.url.base")
            return saved.restartNeeded ? title + "\n" + L10n.tr("settings.controls.saved.restart.to.use") : title
        case .proxyOn:
            return L10n.tr("settings.controls.saved.proxy")
        case .proxyOff:
            return L10n.tr("settings.controls.saved.proxy.off")
        case .failed:
            let title = L10n.tr("settings.controls.saved.failed")
            guard let error = saved.error else { return title }
            return title + "\n" + errorText(error)
        }
    }
}
