import Foundation

enum DownloadersWords {
    nonisolated static func error(_ e: ErrorInfo) -> String {
        if (400 ... 409).contains(e.status), !e.message.isEmpty { return e.message }
        return e.localized
    }

    nonisolated static func health(_ row: DownloaderRowInfo) -> String {
        switch row.health {
        case .working: L10n.tr("acq.dl.state.working")
        case .paused: L10n.tr("acq.dl.state.paused")
        case .resting: L10n.tr("acq.dl.state.resting")
        case .notAnswering: L10n.tr("acq.dl.state.not.answering")
        case .signInRefused: L10n.tr("acq.dl.state.sign.in.refused")
        case .unknown: row.healthWord
        }
    }

    nonisolated static func testText(_ t: DownloaderTestInfo) -> String {
        if !t.text.isEmpty { return t.text }
        if let e = t.error { return error(e) }
        return ""
    }

    nonisolated static func testTag(_ t: DownloaderTestInfo) -> String {
        if !t.ok { return L10n.tr("acq.dl.not.connected") }
        return t.tag.isEmpty ? L10n.tr("acq.dl.connected") : t.tag
    }

    nonisolated static func ago(_ date: Date?) -> String {
        date.map { Format.relative($0) } ?? ""
    }

    nonisolated static func issue(_ i: DownloaderIssueKind) -> String {
        switch i {
        case .name: L10n.tr("acq.dl.issue.name")
        case .url: L10n.tr("acq.dl.issue.url")
        case .category, .kindCategory, .afterImport: L10n.tr("acq.dl.issue.category")
        case .categoryLabel: L10n.tr("acq.dl.issue.label")
        case .categoryFolder: L10n.tr("acq.dl.issue.folder")
        case .afterImportSame: L10n.tr("acq.dl.issue.after.same")
        case .priority: L10n.tr("acq.dl.issue.priority")
        case .path: L10n.tr("acq.dl.issue.path")
        case .ratio: L10n.tr("acq.dl.issue.ratio")
        case .hours: L10n.tr("acq.dl.issue.hours")
        case .secret: L10n.tr("acq.dl.issue.secret")
        case .username: L10n.tr("acq.dl.issue.username")
        }
    }

    nonisolated static func rulesIssue(_ i: DownloaderRulesIssueKind) -> String {
        switch i {
        case .notANumber: L10n.tr("acq.dl.rules.issue.number")
        case .stalledAfterRange: L10n.tr("acq.dl.rules.issue.after")
        case .actSooner: L10n.tr("acq.dl.rules.issue.sooner")
        case .actRange: L10n.tr("acq.dl.rules.issue.act")
        case .spacingRange: L10n.tr("acq.dl.rules.issue.spacing")
        case .deadMinutesRange: L10n.tr("acq.dl.rules.issue.dead")
        case .kbpsRange: L10n.tr("acq.dl.rules.issue.kbps")
        case .hoursRange: L10n.tr("acq.dl.rules.issue.hours")
        }
    }

    nonisolated static func keyNote(_ n: DownloaderKeyNoteKind) -> String {
        switch n {
        case .fetched: L10n.tr("acq.dl.key.fetched")
        case .needsLogin: L10n.tr("acq.dl.key.needs.login")
        case .wrongLogin: L10n.tr("acq.dl.key.wrong.login")
        case .notFound: L10n.tr("acq.dl.key.not.found")
        }
    }

    nonisolated static func categoryHint(_ wire: String) -> String {
        switch wire {
        case "sabnzbd": L10n.tr("acq.dl.cat.sab")
        case "transmission": L10n.tr("acq.dl.cat.transmission")
        case "deluge": L10n.tr("acq.dl.cat.deluge")
        case "rtorrent": L10n.tr("acq.dl.cat.rtorrent")
        case "aria2": L10n.tr("acq.dl.cat.aria2")
        case "nzbget": L10n.tr("acq.dl.cat.nzbget")
        default: L10n.tr("acq.dl.cat.qbit")
        }
    }

    nonisolated static func message(_ m: DownloaderMessageInfo) -> String {
        let head: String
        switch m.kind {
        case .saved: head = L10n.format("acq.dl.saved", m.name)
        case .pauseFailed: head = L10n.tr("acq.dl.pause.failed")
        case .resumeFailed: head = L10n.tr("acq.dl.resume.failed")
        case .removeFailed: head = L10n.tr("acq.dl.remove.failed")
        case .categoryMade: head = L10n.format("acq.dl.category.made", m.name, m.app)
        case .categoryFailed: head = L10n.format("acq.dl.category.failed", m.name)
        case .abandoned: head = L10n.tr("acq.dl.abandoned")
        case .abandonFailed: head = L10n.tr("acq.dl.abandon.failed")
        }
        guard let e = m.error else { return head }
        return L10n.format("acq.dl.with.reason", head, error(e))
    }
}
