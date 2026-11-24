import Foundation

enum StatusText {
    static func word(_ word: StatusWordID) -> String {
        switch word {
        case .working: L10n.tr("settings.status.word.working")
        case .warning: L10n.tr("settings.status.word.warning")
        case .broken: L10n.tr("settings.status.word.broken")
        case .stuck: L10n.tr("settings.status.word.stuck")
        case .starting: L10n.tr("settings.status.word.starting")
        case .retrying: L10n.tr("settings.status.word.retrying")
        case .off: L10n.tr("settings.status.word.off")
        case .notSynced: L10n.tr("settings.status.word.not.synced")
        case .notSent: L10n.tr("settings.status.word.not.sent")
        case .turnedOff: L10n.tr("settings.status.word.turned.off")
        case .partOff: L10n.tr("settings.status.word.part.off")
        case .safeMode: L10n.tr("settings.status.word.safe.mode")
        case .running: L10n.tr("settings.status.word.running")
        case .failed: L10n.tr("settings.status.word.failed")
        case .late: L10n.tr("settings.status.word.late")
        case .notRun: L10n.tr("settings.status.word.not.run")
        case .cancelled: L10n.tr("settings.status.word.cancelled")
        case .ok: L10n.tr("settings.status.word.ok")
        }
    }

    static func title(_ row: StatusRowInfo) -> String {
        let label: String
        switch row.title {
        case .plain: return row.name
        case .folder: label = L10n.tr("settings.status.title.folder")
        case .freeSpace: label = L10n.tr("settings.status.title.free.space")
        case .backups: label = L10n.tr("settings.status.title.backups")
        case .update: label = L10n.tr("settings.status.title.update")
        case .configDisk: label = L10n.tr("settings.status.title.config.disk")
        case .configFolder: label = L10n.tr("settings.status.title.config.folder")
        case .waitingDownloads: label = L10n.tr("settings.status.title.waiting.downloads")
        case .clearWaiting: label = L10n.tr("settings.status.title.clear.waiting")
        case .saveFolder: label = L10n.tr("settings.status.title.save.folder")
        case .torrentDownloader: label = L10n.tr("settings.status.title.torrent.downloader")
        case .usenetDownloader: label = L10n.tr("settings.status.title.usenet.downloader")
        case .sameDownloader: label = L10n.tr("settings.status.title.same.downloader")
        case .peopleCheck: label = L10n.tr("settings.status.title.people.check")
        case .metadata: label = L10n.tr("settings.status.title.metadata")
        }
        return row.name.isEmpty ? label : "\(label) \(row.name)"
    }

    static func reason(_ row: StatusRowInfo) -> String {
        switch row.reasonKind {
        case .none: ""
        case .text: row.reason
        case .jobLate: L10n.tr("settings.status.reason.late")
        case .jobFailed: L10n.tr("settings.status.reason.failed")
        case .configSteps: L10n.tr("settings.status.reason.config.steps")
        case .saveFolder: L10n.format("settings.status.reason.save.folder", row.reason)
        case .noTorrentDownloader: L10n.tr("settings.status.reason.no.torrent")
        case .noUsenetDownloader: L10n.tr("settings.status.reason.no.usenet")
        case .sameDownloader: L10n.format("settings.status.reason.same.downloader", row.reason)
        case .keyKit: L10n.tr("settings.status.reason.key.kit")
        case .keyKitOwner: L10n.tr("settings.status.reason.key.kit.owner")
        case .metadataDown: L10n.tr("settings.status.reason.metadata.down")
        }
    }

    static func group(_ group: StatusGroupID) -> String {
        switch group {
        case .sources: L10n.tr("settings.status.group.sources")
        case .downloaders: L10n.tr("settings.status.group.downloaders")
        case .library: L10n.tr("settings.status.group.library")
        case .media: L10n.tr("settings.status.group.media")
        case .subtitles: L10n.tr("settings.status.group.subtitles")
        case .lists: L10n.tr("settings.status.group.lists")
        case .alerts: L10n.tr("settings.status.group.alerts")
        case .metadata: L10n.tr("settings.status.group.metadata")
        case .app: L10n.tr("settings.status.group.app")
        case .jobs: L10n.tr("settings.status.group.jobs")
        }
    }

    static func none(_ group: StatusGroupID) -> String {
        switch group {
        case .sources: L10n.tr("settings.status.none.sources")
        case .downloaders: L10n.tr("settings.status.none.downloaders")
        case .media: L10n.tr("settings.status.none.media")
        case .subtitles: L10n.tr("settings.status.none.subtitles")
        case .lists: L10n.tr("settings.status.none.lists")
        case .alerts: L10n.tr("settings.status.none.alerts")
        case .library, .metadata, .app, .jobs: L10n.tr("settings.status.none.metadata")
        }
    }

    static func summary(_ kind: StatusKindInfo) -> String {
        switch kind.summary {
        case .nothing: L10n.tr("settings.status.summary.nothing")
        case .needs: L10n.plural("settings.status.summary.needs", kind.summaryCount)
        case .word: kind.summaryWord.map { word($0) } ?? ""
        case .off: L10n.format("settings.status.summary.off", kind.summaryCount)
        case .working: L10n.plural("settings.status.summary.working", kind.summaryCount)
        case .waiting: L10n.plural("settings.status.summary.waiting", kind.summaryCount)
        }
    }

    static func diskName(_ disk: StatusDiskInfo) -> String {
        if !disk.label.isEmpty { return disk.label }
        switch disk.kind {
        case .movies: return L10n.tr("settings.status.disk.movies")
        case .shows: return L10n.tr("settings.status.disk.shows")
        case .music: return L10n.tr("settings.status.disk.music")
        case .other: return L10n.tr("settings.status.disk.library")
        }
    }

    static func since(_ row: StatusRowInfo) -> String? {
        guard let at = row.at else { return nil }
        let ago = Format.relative(at)
        switch row.group {
        case .sources, .downloaders, .media, .subtitles: return L10n.format("settings.status.since", ago)
        case .library, .app, .metadata: return L10n.format("settings.status.checked", ago)
        case .lists: return L10n.format("settings.status.synced", ago)
        case .alerts: return L10n.format("settings.status.sent", ago)
        case .jobs: return L10n.format("settings.status.ran", ago)
        }
    }

    static func due(_ due: StatusDueInfo) -> String {
        switch due.kind {
        case .none: return L10n.tr("settings.status.due.none")
        case .now: return L10n.tr("settings.status.due.now")
        case .inTime:
            switch due.unit {
            case .minutes: return L10n.format("settings.status.due.in.min", due.amount)
            case .hours: return L10n.format("settings.status.due.in.hour", due.amount)
            case .days: return L10n.format("settings.status.due.in.day", due.amount)
            }
        case .overdue:
            switch due.unit {
            case .minutes: return L10n.format("settings.status.due.late.min", due.amount)
            case .hours: return L10n.format("settings.status.due.late.hour", due.amount)
            case .days: return L10n.format("settings.status.due.late.day", due.amount)
            }
        }
    }

    static func errorDetail(_ error: ErrorInfo) -> String {
        let line = error.localized
        let sentence = [400, 409, 422, 502, 503].contains(error.status) ? error.message : ""
        return sentence.isEmpty ? line : "\(line)\n\(sentence)"
    }

    static func notice(_ notice: StatusNoticeInfo) -> String {
        let name = notice.row.map { title($0) } ?? ""
        let word = notice.row.map { StatusText.word($0.word) } ?? ""
        let head: String
        switch notice.kind {
        case .checksRan: head = L10n.tr("settings.status.notice.checks.ran")
        case .checksFailed: head = L10n.tr("settings.status.notice.checks.failed")
        case .rowWorking: head = L10n.format("settings.status.notice.row.working", name)
        case .rowWord: head = L10n.format("settings.status.notice.row.word", name, word)
        case .rowConnected: head = L10n.format("settings.status.notice.connected", name)
        case .rowCantConnect: head = L10n.format("settings.status.notice.cant.connect", name)
        case .rowCantCheck: head = L10n.format("settings.status.notice.cant.check", name)
        case .jobStarted: head = L10n.format("settings.status.notice.job.started", name)
        case .jobAlready: head = L10n.format("settings.status.notice.job.already", name)
        case .updateCheckFailed: head = L10n.tr("settings.status.notice.update.check.failed")
        case .updateSaved: head = L10n.tr("settings.status.notice.saved")
        case .updateSaveFailed: head = L10n.tr("settings.status.notice.save.failed")
        case .updateInstallFailed: head = L10n.tr("settings.status.notice.install.failed")
        case .updateGoBackFailed: head = L10n.tr("settings.status.notice.go.back.failed")
        case .bundleFailed: head = L10n.tr("settings.status.notice.bundle.failed")
        }
        let detail: String
        if !notice.detail.isEmpty {
            detail = notice.detail
        } else if let error = notice.error {
            detail = errorDetail(error)
        } else {
            detail = ""
        }
        return detail.isEmpty ? head : "\(head)\n\(detail)"
    }

    static func headline(_ headline: StatusHeadlineInfo) -> String {
        switch headline.kind {
        case .fine: return L10n.tr("settings.status.headline.fine")
        case .unknown: return L10n.tr("settings.status.headline.none")
        case .nothingLinked: return L10n.tr("settings.status.headline.nothing.linked")
        case .attention:
            var parts: [String] = []
            if headline.checks > 0 { parts.append(L10n.plural("settings.status.headline.checks", headline.checks)) }
            if headline.jobs > 0 { parts.append(L10n.plural("settings.status.headline.jobs", headline.jobs)) }
            return parts.joined(separator: ", ")
        }
    }

    static func read(_ read: StatusReadID) -> String {
        switch read {
        case .parts: L10n.tr("settings.status.what.parts")
        case .checks: L10n.tr("settings.status.what.checks")
        case .jobs: L10n.tr("settings.status.what.jobs")
        case .lists: L10n.tr("settings.status.what.lists")
        case .alerts: L10n.tr("settings.status.what.alerts")
        }
    }

    static func uptime(_ seconds: Int64) -> String {
        let total = max(0, seconds)
        let days = total / 86400
        let hours = (total / 3600) % 24
        let minutes = (total / 60) % 60
        let rest = total % 60
        return String(format: "%ldd %02ld:%02ld:%02ld", Int(days), Int(hours), Int(minutes), Int(rest))
    }
}

extension StatusFixID {
    var route: Route {
        switch self {
        case .sources: .settingsSources
        case .downloaders: .settingsDownloaders
        case .mediaServers: .settingsMediaServers
        case .subtitles: .settingsSubtitles
        case .libraryFolders: .settingsLibraryFolders
        case .backups: .settingsBackups
        case .thisApp: .settingsThisApp
        case .outsideLists: .settingsOutsideLists
        case .alerts: .settingsAlerts
        case .schedules: .settingsSchedules
        case .downloadsWaiting: .downloads(show: "waiting.paused", open: "")
        case .downloads: .downloads(show: "", open: "")
        }
    }
}

enum StatusLinks {
    static let sponsor = "https://github.com/sponsors/CoderLuii"
    static let coffee = "https://buymeacoffee.com/coderluii"
    static let star = "https://github.com/HolyArrs/HolyArrs"
    static let note = "mailto:support@holyarrs.com?subject=A%20note%20about%20HolyArrs"
}
