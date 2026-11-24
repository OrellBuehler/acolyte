import Foundation

enum DownloadsText {
    static func duration(_ seconds: Int64) -> String {
        let s = max(seconds, 0)
        if s < 60 { return L10n.format("downloads.duration.s", Int(s)) }
        if s < 3600 { return L10n.format("downloads.duration.m", Int(s / 60)) }
        if s < 48 * 3600 { return L10n.format("downloads.duration.hm", Int(s / 3600), Int((s % 3600) / 60)) }
        return L10n.format("downloads.duration.dh", Int(s / 86400), Int((s % 86400) / 3600))
    }

    static func limits(_ l: ShareLimitsInfo) -> String {
        var parts: [String] = []
        if l.ratio >= 0 {
            parts.append(L10n.format("downloads.rules.ratio", DlFormat.shortRatio(l.ratio)))
        } else if l.ratio != -2 {
            parts.append(L10n.tr("downloads.rules.no.limit"))
        }
        if l.seedingMinutes >= 0 {
            parts.append(L10n.format("downloads.rules.seeding", duration(l.seedingMinutes * 60)))
        }
        if l.inactiveMinutes >= 0 {
            parts.append(L10n.format("downloads.rules.idle", duration(l.inactiveMinutes * 60)))
        }
        let base = parts.isEmpty ? L10n.tr("downloads.rules.downloader") : parts.joined(separator: " · ")
        let then = thenLabel(l.action)
        if then.isEmpty { return base }
        return L10n.format("downloads.rules.then", base, then.lowercased())
    }

    static func thenLabel(_ action: String) -> String {
        switch action {
        case "stop": L10n.tr("downloads.then.stop")
        case "remove": L10n.tr("downloads.then.remove")
        case "remove_files": L10n.tr("downloads.then.remove.files")
        case "super_seed": L10n.tr("downloads.then.super")
        default: ""
        }
    }

    static func dateTime(_ date: Date) -> String {
        date.formatted(date: .abbreviated, time: .shortened)
    }

    static func clock(_ date: Date) -> String {
        date.formatted(date: .omitted, time: .shortened)
    }

    static func day(_ day: DlDay) -> String {
        if day.year == Calendar.current.component(.year, from: Date()) {
            return day.date.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated))
        }
        return day.date.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated).year())
    }

    static func dayHeading(_ day: DlDay, today: DlDay, yesterday: Bool) -> String {
        if day == today { return L10n.tr("downloads.day.today") }
        if yesterday && day == today.adding(days: -1) { return L10n.tr("downloads.day.yesterday") }
        return self.day(day)
    }

    static func monthTitle(_ month: DlMonth) -> String {
        month.date.formatted(.dateTime.month(.wide).year())
    }

    static func time(minutes: Int) -> String {
        let base = Calendar.current.startOfDay(for: Date())
        let date = Calendar.current.date(byAdding: .minute, value: minutes, to: base) ?? base
        return date.formatted(date: .omitted, time: .shortened)
    }

    static func uppercaseFirst(_ text: String) -> String {
        guard let first = text.first else { return text }
        return first.uppercased() + text.dropFirst()
    }

    static func factValue(_ f: InfoFactInfo) -> String {
        switch f.kind {
        case .downloaded:
            return L10n.format("downloads.value.of", DlFormat.size(f.a), DlFormat.size(f.b))
        case .seeds, .peers:
            if f.b > 0 { return L10n.format("downloads.value.of", String(f.a), String(f.b)) }
            return String(f.a)
        case .isPrivate:
            return f.a > 0 ? L10n.tr("downloads.value.yes") : L10n.tr("downloads.value.no")
        case .speedLimits:
            let down = f.a > 0 ? DlFormat.speed(f.a) : L10n.tr("downloads.value.no.cap")
            let up = f.b > 0 ? DlFormat.speed(f.b) : L10n.tr("downloads.value.no.cap")
            return L10n.format("downloads.value.caps", down, up)
        default:
            return otherFactValue(f)
        }
    }

    private static func otherFactValue(_ f: InfoFactInfo) -> String {
        if let limits = f.limits { return DownloadsText.limits(limits) }
        if let at = f.at { return dateTime(at) + " · " + Format.relative(at) }
        if f.seconds > 0 { return duration(f.seconds) }
        return f.text
    }

    static func trackerStatus(_ status: String) -> String {
        switch status {
        case "working": L10n.tr("downloads.tracker.working")
        case "not_contacted": L10n.tr("downloads.tracker.not.contacted")
        case "not_working": L10n.tr("downloads.tracker.not.working")
        case "error": L10n.tr("downloads.tracker.error")
        case "unreachable": L10n.tr("downloads.tracker.unreachable")
        case "disabled": L10n.tr("downloads.tracker.disabled")
        default: status
        }
    }

    static func repairLabel(_ stage: String) -> String {
        switch stage {
        case "download": L10n.tr("downloads.repair.download")
        case "repair": L10n.tr("downloads.repair.repair")
        case "join": L10n.tr("downloads.repair.join")
        case "unpack": L10n.tr("downloads.repair.unpack")
        case "rename": L10n.tr("downloads.repair.rename")
        case "move": L10n.tr("downloads.repair.move")
        default: L10n.tr("downloads.repair.script")
        }
    }

    static func filePriority(_ value: Int) -> String {
        switch value {
        case 0: L10n.tr("downloads.file.skip")
        case 1: L10n.tr("downloads.file.normal")
        case 6: L10n.tr("downloads.file.high")
        case 7: L10n.tr("downloads.file.max")
        default: L10n.tr("downloads.file.mixed")
        }
    }

    static let filePriorities: [Int] = [0, 1, 6, 7]
}

extension DownloadsTabKind {
    var label: String {
        switch self {
        case .all: L10n.tr("downloads.tab.all")
        case .waiting: L10n.tr("downloads.tab.waiting")
        case .progress: L10n.tr("downloads.tab.progress")
        case .problems: L10n.tr("downloads.tab.problems")
        case .done: L10n.tr("downloads.tab.done")
        }
    }
}

extension DownloadsSubKind {
    var label: String {
        switch self {
        case .queued: L10n.tr("downloads.sub.queued")
        case .paused: L10n.tr("downloads.sub.paused")
        case .downloading: L10n.tr("downloads.sub.downloading")
        case .checking: L10n.tr("downloads.sub.checking")
        case .importing: L10n.tr("downloads.sub.importing")
        case .failed: L10n.tr("downloads.sub.failed")
        case .decide: L10n.tr("downloads.sub.decide")
        }
    }

    var emptyTitle: String {
        switch self {
        case .queued: L10n.tr("downloads.empty.sub.queued")
        case .paused: L10n.tr("downloads.empty.sub.paused")
        case .downloading: L10n.tr("downloads.empty.sub.downloading")
        case .checking: L10n.tr("downloads.empty.sub.checking")
        case .importing: L10n.tr("downloads.empty.sub.importing")
        case .failed: L10n.tr("downloads.empty.sub.failed")
        case .decide: L10n.tr("downloads.empty.sub.decide")
        }
    }
}

extension DownloadsTabKind {
    var emptyTitle: String {
        switch self {
        case .all: L10n.tr("downloads.empty.all.title")
        case .waiting: L10n.tr("downloads.empty.waiting.title")
        case .progress: L10n.tr("downloads.empty.progress.title")
        case .problems: L10n.tr("downloads.empty.problems.title")
        case .done: L10n.tr("downloads.empty.done.title")
        }
    }

    var emptyMessage: String {
        switch self {
        case .all: L10n.tr("downloads.empty.all")
        case .waiting: L10n.tr("downloads.empty.waiting")
        case .progress: L10n.tr("downloads.empty.progress")
        case .problems: L10n.tr("downloads.empty.problems")
        case .done: L10n.tr("downloads.empty.done")
        }
    }
}

extension StatusWordKind {
    var label: String {
        switch self {
        case .failed: L10n.tr("downloads.word.failed")
        case .packWaiting: L10n.tr("downloads.word.pack.waiting")
        case .moving: L10n.tr("downloads.word.moving")
        case .checking: L10n.tr("downloads.word.checking")
        case .queued: L10n.tr("downloads.word.queued")
        case .paused: L10n.tr("downloads.word.paused")
        case .downloading: L10n.tr("downloads.word.downloading")
        case .stalled: L10n.tr("downloads.word.stalled")
        case .importing: L10n.tr("downloads.word.importing")
        case .seeding: L10n.tr("downloads.word.seeding")
        case .review: L10n.tr("downloads.word.review")
        case .doneSeeding: L10n.tr("downloads.word.done.seeding")
        case .done: L10n.tr("downloads.word.done")
        case .imported: L10n.tr("downloads.word.imported")
        case .taken: L10n.tr("downloads.word.taken")
        }
    }
}

extension ProblemKindInfo {
    var label: String {
        switch self {
        case .missingArticles: L10n.tr("downloads.problem.missing.articles")
        case .repairNeeded: L10n.tr("downloads.problem.repair.needed")
        case .repairing: L10n.tr("downloads.problem.repairing")
        case .importBlocked: L10n.tr("downloads.problem.import.blocked")
        case .trackerError: L10n.tr("downloads.problem.tracker.error")
        case .noTrackerReply: L10n.tr("downloads.problem.no.tracker.reply")
        case .downloaderStopped: L10n.tr("downloads.problem.downloader.stopped")
        case .noFullCopy: L10n.tr("downloads.problem.no.full.copy")
        case .noPeers: L10n.tr("downloads.problem.no.peers")
        case .trackerWarning: L10n.tr("downloads.problem.tracker.warning")
        }
    }
}

extension RowDetailInfo {
    var message: String {
        switch kind {
        case .filesGone: L10n.tr("downloads.detail.files.gone")
        case .unpacking: L10n.tr("downloads.detail.unpacking")
        case .upNext: L10n.tr("downloads.detail.up.next")
        case .placing: L10n.tr("downloads.detail.placing")
        case .importFailed: L10n.format("downloads.detail.import.failed", text)
        case .noFile: L10n.tr("downloads.detail.no.file")
        case .removed: L10n.tr("downloads.detail.removed")
        case .refused: text
        case .noMatch: L10n.tr("downloads.detail.no.match")
        case .cleanupHold: L10n.format("downloads.detail.cleanup", text)
        case .ratio: L10n.format("downloads.detail.ratio", text)
        }
    }
}

extension RowNoteInfo {
    var message: String {
        switch kind {
        case .nextInLine: L10n.tr("downloads.note.next")
        case .inLine: L10n.format("downloads.note.in.line", number)
        case .verifying: L10n.tr("downloads.note.verifying")
        case .repairing: L10n.tr("downloads.note.repairing")
        case .unpacking: L10n.tr("downloads.note.unpacking")
        case .finishing: L10n.tr("downloads.note.finishing")
        case .script: L10n.tr("downloads.note.script")
        case .filesMissing: L10n.tr("downloads.note.files.missing")
        case .downloaderError: L10n.tr("downloads.note.downloader.error")
        case .importFailed: L10n.tr("downloads.note.import.failed")
        case .removedOutside: L10n.tr("downloads.note.removed.outside")
        case .noMatch: L10n.tr("downloads.note.no.match")
        case .filesGone: L10n.tr("downloads.word.taken")
        case .stillIn: L10n.format("downloads.note.still.in", text)
        case .clientText: text
        case .detail: DownloadsText.uppercaseFirst(detail?.message ?? "")
        }
    }
}

extension SubLineInfo {
    var display: String {
        switch kind {
        case .text: text
        case .season: L10n.format("downloads.season", from)
        case .seasons: L10n.format("downloads.seasons", from, to)
        }
    }
}

extension DownloadActionKind {
    var label: String {
        switch self {
        case .pause: L10n.tr("downloads.action.pause")
        case .resume: L10n.tr("downloads.action.resume")
        case .retry: L10n.tr("downloads.action.retry")
        case .moveUp: L10n.tr("downloads.action.up")
        case .moveDown: L10n.tr("downloads.action.down")
        case .moveTop: L10n.tr("downloads.action.top")
        case .moveBottom: L10n.tr("downloads.action.bottom")
        case .priority: L10n.tr("downloads.action.priority")
        case .importAgain: L10n.tr("downloads.action.import")
        case .openTitle: L10n.tr("downloads.action.open")
        case .remove: L10n.tr("downloads.action.remove")
        case .recheck: L10n.tr("downloads.action.recheck")
        case .reannounce: L10n.tr("downloads.action.reannounce")
        case .forceStart: L10n.tr("downloads.action.force")
        case .seedingRules: L10n.tr("downloads.action.seeding")
        case .setLocation: L10n.tr("downloads.action.location")
        case .speedLimits: L10n.tr("downloads.action.speeds")
        case .markFailed: L10n.tr("downloads.action.mark.failed")
        }
    }

    var symbol: String {
        switch self {
        case .pause: "pause"
        case .resume: "play"
        case .retry: "arrow.clockwise"
        case .moveUp: "arrow.up"
        case .moveDown: "arrow.down"
        case .moveTop: "arrow.up.to.line"
        case .moveBottom: "arrow.down.to.line"
        case .priority: "flag"
        case .importAgain: "square.and.arrow.down"
        case .openTitle: "arrow.up.right.square"
        case .remove: "trash"
        case .recheck: "arrow.triangle.2.circlepath"
        case .reannounce: "dot.radiowaves.left.and.right"
        case .forceStart: "forward.fill"
        case .seedingRules: "arrow.up.arrow.down"
        case .setLocation: "folder"
        case .speedLimits: "speedometer"
        case .markFailed: "xmark.octagon"
        }
    }

    var destructive: Bool { self == .remove || self == .markFailed }
}

extension UsenetPriorityKind {
    var label: String {
        switch self {
        case .low: L10n.tr("downloads.priority.low")
        case .normal: L10n.tr("downloads.priority.normal")
        case .high: L10n.tr("downloads.priority.high")
        case .force: L10n.tr("downloads.priority.force")
        }
    }
}

extension SortKeyKind {
    var label: String {
        switch self {
        case .title: L10n.tr("downloads.sort.title")
        case .progress: L10n.tr("downloads.sort.progress")
        case .status: L10n.tr("downloads.sort.status")
        case .size: L10n.tr("downloads.sort.size")
        case .quality: L10n.tr("downloads.sort.quality")
        case .added: L10n.tr("downloads.sort.added")
        case .seeds: L10n.tr("downloads.sort.seeds")
        case .peers: L10n.tr("downloads.sort.peers")
        case .ratio: L10n.tr("downloads.sort.ratio")
        case .tracker: L10n.tr("downloads.sort.tracker")
        case .requested: L10n.tr("downloads.sort.requested")
        case .downloader: L10n.tr("downloads.sort.downloader")
        }
    }
}

extension DlKindFilter {
    var label: String {
        switch self {
        case .all: L10n.tr("downloads.filter.everything")
        case .movie: L10n.tr("downloads.filter.movies")
        case .show: L10n.tr("downloads.filter.shows")
        case .music: L10n.tr("downloads.filter.music")
        }
    }
}

extension DlNetworkFilter {
    var label: String {
        switch self {
        case .any: L10n.tr("downloads.filter.any.network")
        case .usenet: L10n.tr("downloads.filter.usenet")
        case .torrent: L10n.tr("downloads.filter.torrent")
        }
    }
}

extension TrackerOptionInfo {
    var display: String {
        switch kind {
        case .host: label
        case .error: L10n.tr("downloads.problem.tracker.error")
        case .warning: L10n.tr("downloads.problem.tracker.warning")
        case .announce: L10n.tr("downloads.problem.no.tracker.reply")
        }
    }
}

extension ClientNoteInfo {
    var message: String {
        switch kind {
        case .waiting: L10n.format("downloads.note.client.waiting", name)
        case .stale: L10n.format("downloads.note.client.stale", name)
        case .offline: L10n.format("downloads.note.client.offline", name)
        }
    }
}

extension DownloadItemInfo {
    var secondLine: String {
        if let problem { return problem.kind.label }
        if let note { return note.message }
        return ""
    }

    var factsLine: String {
        var facts: [String] = []
        if size > 0 { facts.append(DlFormat.size(size)) }
        if !history { facts.append("\(DlFormat.percent(progress))%") }
        if moving && speed >= 1000 {
            facts.append(L10n.format("downloads.speed.down", DlFormat.speed(speed)))
            if upSpeed > 0 { facts.append(L10n.format("downloads.speed.up", DlFormat.speed(upSpeed))) }
            if eta > 0 { facts.append(L10n.format("downloads.time.left", DownloadsText.duration(eta))) }
        }
        return facts.joined(separator: " · ")
    }

    var healthLine: String {
        var health: [String] = [word.label]
        if !history { health.append("\(DlFormat.percent(progress))%") }
        if moving { health.append(DlFormat.speed(speed)) }
        if moving && eta > 0 { health.append(L10n.format("downloads.time.left", DownloadsText.duration(eta))) }
        return health.joined(separator: " · ")
    }
}

extension DownloadsNoticeInfo {
    var message: String {
        switch kind {
        case .paused: L10n.tr("downloads.notice.paused")
        case .started: L10n.tr("downloads.notice.started")
        case .retried: L10n.tr("downloads.notice.retried")
        case .movedTop: L10n.tr("downloads.notice.moved.top")
        case .movedBottom: L10n.tr("downloads.notice.moved.bottom")
        case .movedUp: L10n.tr("downloads.notice.moved.up")
        case .movedDown: L10n.tr("downloads.notice.moved.down")
        case .prioritySet: L10n.tr("downloads.notice.priority")
        case .checking: L10n.tr("downloads.notice.checking")
        case .announced: L10n.tr("downloads.notice.announced")
        case .forceStarted: L10n.tr("downloads.notice.force")
        case .importQueued: L10n.tr("downloads.notice.import.queued")
        case .removed: L10n.format("downloads.notice.removed", count)
        case .removedKept: L10n.format("downloads.notice.removed.kept", text)
        case .markedFailed: L10n.tr("downloads.notice.marked.failed")
        case .allStarted: L10n.tr("downloads.notice.all.started")
        case .allPaused: L10n.tr("downloads.notice.all.paused")
        case .allFailed: L10n.format("downloads.notice.all.failed", text)
        case .speedSaved: L10n.tr("downloads.notice.speed.saved")
        case .seedingSet: L10n.tr("downloads.notice.seeding.set")
        case .moving: L10n.tr("downloads.notice.moving")
        case .speedsSet: L10n.tr("downloads.notice.speeds.set")
        case .searching: L10n.format("downloads.notice.searching", text)
        case .turnedDown: L10n.format("downloads.notice.turned.down", text)
        case .stopped: L10n.format("downloads.notice.stopped", text)
        case .imported: L10n.format("downloads.notice.imported", text)
        case .editionSet: L10n.tr("downloads.notice.edition.set")
        case .matching: L10n.tr("downloads.notice.matching")
        case .matched: L10n.tr("downloads.notice.matched")
        case .downloadFailed: failedMessage
        case .bulkDone: bulkMessage
        case .notHere: L10n.tr("downloads.notice.not.here")
        case .added: addedMessage
        }
    }

    private var failedMessage: String {
        let reason = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if reason.isEmpty { return L10n.tr("downloads.notice.download.failed.plain") }
        return L10n.format("downloads.notice.download.failed", reason)
    }

    private var bulkMessage: String {
        if skipped > 0 { return L10n.format("downloads.notice.bulk.skipped", count, skipped) }
        return L10n.format("downloads.notice.bulk", count)
    }

    private var addedMessage: String {
        guard let o = outcome else { return L10n.tr("downloads.add.nothing") }
        var parts: [String] = []
        if o.added > 0 { parts.append(L10n.format("downloads.add.added", o.added)) }
        if o.pending > 0 { parts.append(L10n.format("downloads.add.pending", o.pending)) }
        if o.duplicates > 0 { parts.append(L10n.format("downloads.add.duplicates", o.duplicates)) }
        if o.failed > 0 { parts.append(L10n.format("downloads.add.failed", o.failed)) }
        if !o.bad.isEmpty { parts.append(L10n.format("downloads.add.bad", o.bad.joined(separator: ", "))) }
        if o.dropped > 0 { parts.append(L10n.format("downloads.add.dropped", o.dropped)) }
        if parts.isEmpty { return L10n.tr("downloads.add.nothing") }
        return parts.joined(separator: " · ")
    }
}

extension DetailTabKind {
    var label: String {
        switch self {
        case .info: L10n.tr("downloads.detail.tab.info")
        case .files: L10n.tr("downloads.detail.tab.files")
        case .peers: L10n.tr("downloads.detail.tab.peers")
        case .trackers: L10n.tr("downloads.detail.tab.trackers")
        case .servers: L10n.tr("downloads.detail.tab.servers")
        case .log: L10n.tr("downloads.detail.tab.log")
        }
    }
}

extension InfoGroupKindInfo {
    var label: String {
        switch self {
        case .transfer: L10n.tr("downloads.group.transfer")
        case .swarm: L10n.tr("downloads.group.swarm")
        case .dates: L10n.tr("downloads.group.dates")
        case .rules: L10n.tr("downloads.group.rules")
        case .place: L10n.tr("downloads.group.where")
        }
    }
}

extension FactKindInfo {
    var label: String {
        switch self {
        case .status: L10n.tr("downloads.fact.status")
        case .downloaded: L10n.tr("downloads.fact.downloaded")
        case .size: L10n.tr("downloads.fact.size")
        case .down: L10n.tr("downloads.fact.down")
        case .up: L10n.tr("downloads.fact.up")
        case .timeLeft: L10n.tr("downloads.fact.time.left")
        case .uploaded: L10n.tr("downloads.fact.uploaded")
        case .ratio: L10n.tr("downloads.fact.ratio")
        case .lastActivity: L10n.tr("downloads.fact.last.activity")
        case .wasted: L10n.tr("downloads.fact.wasted")
        case .seedingTime: L10n.tr("downloads.fact.seeding.time")
        case .seeds: L10n.tr("downloads.fact.seeds")
        case .peers: L10n.tr("downloads.fact.peers")
        case .isPrivate: L10n.tr("downloads.fact.private")
        case .added: L10n.tr("downloads.fact.added")
        case .posted: L10n.tr("downloads.fact.posted")
        case .doneAt: L10n.tr("downloads.fact.done")
        case .timeActive: L10n.tr("downloads.fact.time.active")
        case .release: L10n.tr("downloads.fact.release")
        case .savePath: L10n.tr("downloads.fact.save.path")
        case .downloader: L10n.tr("downloads.fact.downloader")
        case .category: L10n.tr("downloads.fact.category")
        case .folder: L10n.tr("downloads.fact.folder")
        case .requestedBy: L10n.tr("downloads.fact.requested.by")
        case .speedLimits: L10n.tr("downloads.fact.speed.limits")
        case .seedingRules: L10n.tr("downloads.fact.seeding.rules")
        }
    }
}

extension DetailNoticeKind {
    var message: String {
        switch self {
        case .filesSet: L10n.tr("downloads.files.set")
        case .trackerSaved: L10n.tr("downloads.tracker.saved")
        case .trackerRemoved: L10n.tr("downloads.tracker.removed")
        case .trackersAdded: L10n.tr("downloads.trackers.added")
        }
    }
}

extension WhyKindInfo {
    var message: String {
        switch self {
        case .ranOut: L10n.tr("downloads.why.ran.out")
        case .conflict: L10n.tr("downloads.why.conflict")
        case .notReplaced: L10n.tr("downloads.why.not.replaced")
        case .notSent: L10n.tr("downloads.why.not.sent")
        case .linkRefused: L10n.tr("downloads.why.link.refused")
        case .taken: L10n.tr("downloads.why.taken")
        }
    }
}

extension ReasonLineInfo {
    var message: String {
        switch kind {
        case .reading: L10n.tr("downloads.reason.reading")
        case .editionUnknown: L10n.tr("downloads.reason.edition")
        case .copyInPlace: L10n.tr("downloads.reason.copy")
        case .noMatchMovie: L10n.tr("downloads.reason.no.match.movie")
        case .noMatchEpisode: L10n.tr("downloads.reason.no.match.episode")
        case .noMatchTrack: L10n.tr("downloads.reason.no.match.track")
        case .server: text
        case .why: why.message
        }
    }
}

extension DecisionStepKind {
    var label: String {
        switch self {
        case .search: L10n.tr("downloads.decide.search")
        case .importAgain: L10n.tr("downloads.decide.import.again")
        case .importFile: L10n.tr("downloads.decide.import.file")
        case .setEdition: L10n.tr("downloads.decide.set.edition")
        case .openVersions: L10n.tr("downloads.decide.versions")
        case .openReleases: L10n.tr("downloads.decide.releases")
        }
    }
}

extension DecisionInfo {
    func retryMinutes(now: Date) -> Int? {
        guard let at = Format.date(iso: retryAt) else { return nil }
        let minutes = Int((at.timeIntervalSince(now) / 60).rounded(.up))
        return max(1, minutes)
    }
}

extension HistoryStepKind {
    var label: String {
        switch self {
        case .sent: L10n.tr("downloads.history.step.sent")
        case .downloaded: L10n.tr("downloads.history.step.downloaded")
        case .imported: L10n.tr("downloads.history.step.imported")
        case .upgraded: L10n.tr("downloads.history.step.upgraded")
        case .renamed: L10n.tr("downloads.history.step.renamed")
        case .deleted: L10n.tr("downloads.history.step.deleted")
        case .declined: L10n.tr("downloads.history.step.declined")
        case .failed: L10n.tr("downloads.history.step.failed")
        case .asked: L10n.tr("downloads.history.step.asked")
        case .approved: L10n.tr("downloads.history.step.approved")
        case .added: L10n.tr("downloads.history.step.added")
        case .subtitles: L10n.tr("downloads.history.step.subtitles")
        case .reported: L10n.tr("downloads.history.step.reported")
        case .decide: L10n.tr("downloads.history.step.decide")
        case .system: L10n.tr("downloads.history.step.system")
        }
    }
}

extension HistoryTypeKind {
    var label: String {
        switch self {
        case .all: L10n.tr("downloads.history.all")
        case .movies: L10n.tr("downloads.history.movies")
        case .shows: L10n.tr("downloads.history.shows")
        case .music: L10n.tr("downloads.history.music")
        }
    }
}

extension HistoryWhoKind {
    var label: String {
        switch self {
        case .all: L10n.tr("downloads.history.all")
        case .me: L10n.tr("downloads.history.you")
        case .people: L10n.tr("downloads.history.members")
        case .ai: L10n.tr("downloads.history.ai")
        case .holyArrs: L10n.tr("downloads.history.holyarrs")
        }
    }
}

extension HistoryWhenKind {
    var label: String {
        switch self {
        case .all: L10n.tr("downloads.history.all.time")
        case .day: L10n.tr("downloads.history.day")
        case .week: L10n.tr("downloads.history.week")
        case .month: L10n.tr("downloads.history.month")
        case .custom: L10n.tr("downloads.history.custom")
        }
    }
}

extension HistoryVerbKind {
    var label: String {
        switch self {
        case .imported: L10n.tr("downloads.verb.imported")
        case .partlyImported: L10n.tr("downloads.verb.partly.imported")
        case .upgraded: L10n.tr("downloads.verb.upgraded")
        case .lowOnSpace: L10n.tr("downloads.verb.low.on.space")
        case .needsDecision: L10n.tr("downloads.verb.needs.decision")
        case .needsYou: L10n.tr("downloads.verb.needs.you")
        case .stoppedLooking: L10n.tr("downloads.verb.stopped.looking")
        case .fixed: L10n.tr("downloads.verb.fixed")
        case .asked: L10n.tr("downloads.verb.asked")
        case .approved: L10n.tr("downloads.verb.approved")
        case .turnedDown: L10n.tr("downloads.verb.turned.down")
        case .failed: L10n.tr("downloads.verb.failed")
        case .reported: L10n.tr("downloads.verb.reported")
        case .reopened: L10n.tr("downloads.verb.reopened")
        case .downloaded: L10n.tr("downloads.verb.downloaded")
        case .stalled: L10n.tr("downloads.verb.stalled")
        case .paused: L10n.tr("downloads.verb.paused")
        case .queued: L10n.tr("downloads.verb.queued")
        case .fileMissing: L10n.tr("downloads.verb.file.missing")
        case .fileBack: L10n.tr("downloads.verb.file.back")
        case .refreshed: L10n.tr("downloads.verb.refreshed")
        case .albumsChanged: L10n.tr("downloads.verb.albums.changed")
        case .outNow: L10n.tr("downloads.verb.out.now")
        case .changed: L10n.tr("downloads.verb.changed")
        case .settingsImported: L10n.tr("downloads.verb.settings.imported")
        case .adopted: L10n.tr("downloads.verb.adopted")
        case .added: L10n.tr("downloads.verb.added")
        case .searched: L10n.tr("downloads.verb.searched")
        case .searchFailed: L10n.tr("downloads.verb.search.failed")
        case .held: L10n.tr("downloads.verb.held")
        case .chosen: L10n.tr("downloads.verb.chosen")
        case .sent: L10n.tr("downloads.verb.sent")
        case .downloading: L10n.tr("downloads.verb.downloading")
        case .renamed: L10n.tr("downloads.verb.renamed")
        case .deleted: L10n.tr("downloads.verb.deleted")
        case .subtitles, .subtitlesWait: L10n.tr("downloads.verb.subtitles")
        case .scanned: L10n.tr("downloads.verb.scanned")
        case .health: L10n.tr("downloads.verb.health")
        case .requests: L10n.tr("downloads.verb.requests")
        case .byAi: L10n.tr("downloads.verb.by.ai")
        case .notDone: L10n.tr("downloads.verb.not.done")
        case .alertFailed: L10n.tr("downloads.verb.alert.failed")
        case .mediaServer, .mediaServerFailed: L10n.tr("downloads.verb.media.server")
        case .system: L10n.tr("downloads.verb.system")
        case .other: L10n.tr("downloads.verb.other")
        }
    }
}

extension HistoryFactKindInfo {
    var label: String {
        switch self {
        case .quality: L10n.tr("downloads.history.fact.quality")
        case .size: L10n.tr("downloads.history.fact.size")
        case .source: L10n.tr("downloads.history.fact.source")
        case .downloader: L10n.tr("downloads.history.fact.downloader")
        case .release: L10n.tr("downloads.history.fact.release")
        case .file: L10n.tr("downloads.history.fact.file")
        case .from: L10n.tr("downloads.history.fact.from")
        case .folder: L10n.tr("downloads.history.fact.folder")
        case .why: L10n.tr("downloads.history.fact.why")
        case .album: L10n.tr("downloads.history.fact.album")
        }
    }

    var copyable: Bool { self == .folder || self == .file || self == .release }
}

extension HistoryWhoInfo {
    var message: String {
        switch kind {
        case .person: name
        case .aPerson: L10n.tr("downloads.who.a.person")
        case .ai: aiMessage
        case .job: L10n.tr("downloads.who.job")
        case .holyArrs: L10n.tr("downloads.history.holyarrs")
        }
    }

    private var aiMessage: String {
        let app = name.isEmpty ? L10n.tr("downloads.who.ai.app") : name
        if forName.isEmpty { return app }
        return L10n.format("downloads.who.ai", app, forName)
    }
}

extension HistoryLineInfo {
    var message: String {
        switch kind {
        case .summary, .reason: return text
        case .addedList: return addedList
        case .addedBy: return L10n.format("downloads.line.added.by", text)
        case .addedAsk: return L10n.format("downloads.line.added.ask", text)
        case .downloadingPct: return L10n.format("downloads.line.downloading.pct", number)
        case .pausedAt: return L10n.format("downloads.line.paused.at", number)
        default:
            if text.isEmpty { return phraseText }
            return phraseText + " · " + text
        }
    }

    private var addedList: String {
        if text.isEmpty { return L10n.tr("downloads.line.added.list") }
        return L10n.format("downloads.line.added.list.named", text)
    }

    private var phraseText: String {
        switch kind {
        case .addedImport: L10n.tr("downloads.line.added.import")
        case .removedKept: L10n.tr("downloads.line.removed.kept")
        case .removedDeleted: L10n.tr("downloads.line.removed.deleted")
        case .renamed: L10n.tr("downloads.line.renamed")
        case .renameReady: L10n.tr("downloads.line.rename.ready")
        case .feeds: L10n.tr("downloads.line.feeds")
        case .looked: L10n.tr("downloads.line.looked")
        case .searchFailed: L10n.tr("downloads.line.search.failed")
        case .turnedDown: L10n.tr("downloads.line.turned.down")
        case .unblocked: L10n.tr("downloads.line.unblocked")
        case .heldBack: L10n.tr("downloads.line.held.back")
        case .heldGone: L10n.tr("downloads.line.held.gone")
        case .waiting: L10n.tr("downloads.line.waiting")
        case .downloading: L10n.tr("downloads.line.downloading")
        case .stalled: L10n.tr("downloads.line.stalled")
        case .paused: L10n.tr("downloads.line.paused")
        case .sharing: L10n.tr("downloads.line.sharing")
        case .movedOut: L10n.tr("downloads.line.moved.out")
        case .removedOutside: L10n.tr("downloads.line.removed.outside")
        case .removedDl: L10n.tr("downloads.line.removed.dl")
        case .removedDlFiles: L10n.tr("downloads.line.removed.dl.files")
        case .dlFailed: L10n.tr("downloads.line.dl.failed")
        case .notRemoved: L10n.tr("downloads.line.not.removed")
        case .notMoved: L10n.tr("downloads.line.not.moved")
        case .partly: L10n.tr("downloads.line.partly")
        case .nothingPlaced: L10n.tr("downloads.line.nothing.placed")
        case .better: L10n.tr("downloads.line.better")
        case .fileBack: L10n.tr("downloads.line.file.back")
        case .fileHeld: L10n.tr("downloads.line.file.held")
        case .holdFailed: L10n.tr("downloads.line.hold.failed")
        case .holdingCleaned: L10n.tr("downloads.line.holding.cleaned")
        case .lowSpace: L10n.tr("downloads.line.low.space")
        case .stoppedLooking: L10n.tr("downloads.line.stopped.looking")
        case .workingAgain: L10n.tr("downloads.line.working.again")
        case .mediaTold: L10n.tr("downloads.line.media.told")
        case .mediaFailed: L10n.tr("downloads.line.media.failed")
        case .alertFailed: L10n.tr("downloads.line.alert.failed")
        case .outNow: L10n.tr("downloads.line.out.now")
        case .report: L10n.tr("downloads.line.report")
        case .checkedDetails: L10n.tr("downloads.line.checked.details")
        default: ""
        }
    }
}

extension HistoryItemInfo {
    var bits: String {
        [episode, version].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    var foot: String {
        [extra, who.message].filter { !$0.isEmpty }.joined(separator: " · ")
    }
}

extension DownloadStepInfo {
    var label: String {
        switch state {
        case .failed: L10n.tr("downloads.history.step.failed")
        case .needs: L10n.tr("downloads.history.step.decide")
        case .done, .waiting:
            switch key {
            case .sent: L10n.tr("downloads.history.step.sent")
            case .downloaded: L10n.tr("downloads.history.step.downloaded")
            case .imported: L10n.tr("downloads.history.step.imported")
            }
        }
    }

    var tone: ToneKind {
        switch state {
        case .done: .ok
        case .failed: .fail
        case .needs: .wait
        case .waiting: .idle
        }
    }
}

extension CalendarTypeKind {
    var label: String {
        switch self {
        case .all: L10n.tr("downloads.calendar.all")
        case .movies: L10n.tr("downloads.calendar.movies")
        case .shows: L10n.tr("downloads.calendar.shows")
        case .music: L10n.tr("downloads.calendar.music")
        }
    }
}

extension CalendarFollowsKind {
    var label: String {
        switch self {
        case .mine: L10n.tr("downloads.calendar.mine")
        case .everyone: L10n.tr("downloads.calendar.everyone")
        case .library: L10n.tr("downloads.calendar.library")
        }
    }

    var hint: String {
        switch self {
        case .mine: L10n.tr("downloads.calendar.mine.hint")
        case .everyone: L10n.tr("downloads.calendar.everyone.hint")
        case .library: L10n.tr("downloads.calendar.library.hint")
        }
    }
}

extension EntryDotKind {
    var label: String {
        switch self {
        case .have: L10n.tr("downloads.calendar.dot.have")
        case .getting: L10n.tr("downloads.calendar.dot.getting")
        case .problem: L10n.tr("downloads.calendar.dot.problem")
        case .missing: L10n.tr("downloads.calendar.dot.missing")
        case .upcoming: L10n.tr("downloads.calendar.dot.upcoming")
        }
    }
}

extension CalendarNoticeInfo {
    var message: String {
        switch kind {
        case .followed: L10n.format("downloads.calendar.followed", name)
        case .unfollowed: L10n.format("downloads.calendar.unfollowed", name)
        case .searching: L10n.format("downloads.calendar.searching", name)
        case .nothingToSearch: L10n.tr("downloads.calendar.nothing.to.search")
        case .searchingMissing: L10n.format("downloads.calendar.searching.missing", count)
        }
    }
}

extension CalendarGroupInfo {
    var tagText: String {
        switch tag {
        case .showPremiere: L10n.tr("downloads.calendar.tag.show.premiere")
        case .seasonPremiere: L10n.tr("downloads.calendar.tag.season.premiere")
        case .newEpisode: L10n.tr("downloads.calendar.tag.new.episode")
        case .newAlbum: L10n.tr("downloads.calendar.tag.new.album")
        case .newSingle: L10n.tr("downloads.calendar.tag.new.single")
        case .newEp: L10n.tr("downloads.calendar.tag.new.ep")
        case .inCinemas: L10n.tr("downloads.calendar.tag.in.cinemas")
        case .newMovie: L10n.tr("downloads.calendar.tag.new.movie")
        case .episodes: L10n.format("downloads.calendar.tag.episodes", count)
        case .albums: L10n.format("downloads.calendar.tag.albums", count)
        case .movieDates: movieDatesText
        }
    }

    private var movieDatesText: String {
        let words = movieKinds.map { kind -> String in
            switch kind {
            case .cinema: L10n.tr("downloads.calendar.kind.cinema")
            case .digital: L10n.tr("downloads.calendar.kind.digital")
            case .disc: L10n.tr("downloads.calendar.kind.disc")
            }
        }
        guard let last = words.last else { return "" }
        if words.count > 1 {
            return L10n.format("downloads.calendar.and", words.dropLast().joined(separator: ", "), last.lowercased())
        }
        return last
    }

    var whenText: String {
        switch whenKind {
        case .time: DownloadsText.time(minutes: minutes)
        case .allDay: L10n.tr("downloads.calendar.all.day")
        case .album: L10n.tr("downloads.calendar.album")
        case .single: L10n.tr("downloads.calendar.single")
        case .ep: L10n.tr("downloads.calendar.ep")
        case .inTheaters: L10n.tr("downloads.calendar.in.theaters")
        case .streaming: L10n.tr("downloads.calendar.streaming")
        }
    }

    var episodesText: String {
        switch span {
        case .one: L10n.format("downloads.calendar.season.episode", season, episode)
        case .sameSeason: L10n.format("downloads.calendar.season.episodes", season, episode, episode2)
        case .across: L10n.format("downloads.calendar.season.span", season, episode, season2, episode2)
        }
    }

    var captionLines: [String] {
        let lines: [String]
        switch kind {
        case "episode": lines = [episodesText, episodeName, network]
        case "album": lines = [leadName, album != title ? album : ""]
        default: lines = [caption]
        }
        return lines.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
    }
}
