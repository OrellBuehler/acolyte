import Foundation

enum SourcesWords {
    static func word(_ row: SourceRowInfo) -> String {
        switch row.word {
        case .slowed: L10n.tr("acq.src.word.slowed")
        case .down: L10n.tr("acq.src.word.down")
        case .blocked: L10n.tr("acq.src.word.blocked")
        case .keyRefused: L10n.tr("acq.src.word.key.refused")
        case .working: L10n.tr("acq.src.word.working")
        case .unreachable: L10n.tr("acq.src.word.unreachable")
        case .keyTurnedDown: L10n.tr("acq.src.word.key.turned.down")
        case .paused: L10n.tr("acq.src.word.paused")
        case .other: row.rawWord
        }
    }

    static func protocolName(_ protocolName: String) -> String {
        switch protocolName {
        case "torrent": L10n.tr("acq.src.proto.torrent")
        case "usenet": L10n.tr("acq.src.proto.usenet")
        default: protocolName.capitalized
        }
    }

    static func uses(feed: Bool, auto: Bool, hand: Bool) -> String {
        var parts: [String] = []
        if feed { parts.append(L10n.tr("acq.src.use.feed")) }
        if auto { parts.append(L10n.tr("acq.src.use.auto")) }
        if hand { parts.append(L10n.tr("acq.src.use.hand")) }
        return parts.isEmpty ? L10n.tr("acq.src.use.none") : parts.joined(separator: " · ")
    }

    static func problem(_ kind: SourceProblemKind) -> String {
        switch kind {
        case .name: L10n.tr("acq.src.problem.name")
        case .url: L10n.tr("acq.src.problem.url")
        case .urlQuery: L10n.tr("acq.src.problem.url.query")
        case .priority: L10n.tr("acq.src.problem.priority")
        case .seeders: L10n.tr("acq.src.problem.seeders")
        case .searches: L10n.tr("acq.src.problem.searches")
        case .window: L10n.tr("acq.src.problem.window")
        case .gap: L10n.tr("acq.src.problem.gap")
        case .timeout: L10n.tr("acq.src.problem.timeout")
        case .proxy: L10n.tr("acq.src.problem.proxy")
        case .siteUser: L10n.tr("acq.src.problem.site.user")
        case .siteAgent: L10n.tr("acq.src.problem.site.agent")
        case .languages: L10n.tr("acq.src.problem.languages")
        }
    }

    static func cause(_ kind: SourceCauseKind) -> String {
        switch kind {
        case .nameNotFound: L10n.tr("acq.src.cause.name.not.found")
        case .refused: L10n.tr("acq.src.cause.refused")
        case .timeout: L10n.tr("acq.src.cause.timeout")
        case .certificate: L10n.tr("acq.src.cause.certificate")
        case .useHttp: L10n.tr("acq.src.cause.use.http")
        case .useHttps: L10n.tr("acq.src.cause.use.https")
        case .auth: L10n.tr("acq.src.cause.auth")
        case .wrongPath: L10n.tr("acq.src.cause.wrong.path")
        case .wrongApp: L10n.tr("acq.src.cause.wrong.app")
        case .categoryMissing: L10n.tr("acq.src.cause.category.missing")
        case .localNetwork: L10n.tr("acq.src.cause.local.network")
        }
    }

    static func op(_ kind: SourceOpKind, raw: String) -> String {
        switch kind {
        case .caps: L10n.tr("acq.src.op.caps")
        case .search: L10n.tr("acq.src.op.search")
        case .feed: L10n.tr("acq.src.op.feed")
        case .test: L10n.tr("acq.src.op.test")
        case .other: raw
        }
    }

    static func errorWord(_ kind: SourceErrorWordKind, raw: String) -> String {
        switch kind {
        case .down: L10n.tr("acq.src.err.down")
        case .rateLimited: L10n.tr("acq.src.err.rate.limited")
        case .loginRefused: L10n.tr("acq.src.err.login.refused")
        case .brokenDefinition: L10n.tr("acq.src.err.broken.definition")
        case .blocked: L10n.tr("acq.src.err.blocked")
        case .other: raw.isEmpty ? L10n.tr("acq.src.err.other") : raw
        }
    }

    static func checkWord(_ kind: SourceCheckWordKind) -> String {
        switch kind {
        case .connected: L10n.tr("acq.src.check.connected")
        case .notConnected: L10n.tr("acq.src.check.not.connected")
        case .paused: L10n.tr("acq.src.check.paused")
        }
    }

    static func separator() -> String {
        Locale.current.decimalSeparator ?? "."
    }

    static func duration(_ ms: Int) -> String {
        if ms <= 0 { return "—" }
        if ms < 1000 { return "\(ms) ms" }
        let tenths = Int((Double(ms) / 100).rounded())
        return "\(tenths / 10)\(separator())\(tenths % 10) s"
    }

    static func outcome(_ outcome: SourceOutcomeInfo) -> String {
        if !outcome.ok {
            if let error = outcome.error { return error.sourceText }
            return outcome.text
        }
        let time = duration(outcome.ms)
        if outcome.items > 0 {
            return L10n.plural("acq.src.test.items", outcome.items) + " · " + time
        }
        return L10n.format("acq.src.test.answered", time)
    }

    static func time(_ iso: String) -> String? {
        guard let date = Format.date(iso: iso) else { return nil }
        return date.formatted(date: .omitted, time: .shortened)
    }

    static func ago(_ iso: String) -> String? {
        guard let date = Format.date(iso: iso) else { return nil }
        return Format.relative(date)
    }

    static func pausedUntil(_ iso: String) -> String? {
        time(iso).map { L10n.format("acq.src.paused.until", $0) }
    }

    static func dayLabel(_ day: String) -> String {
        let parser = DateFormatter()
        parser.dateFormat = "yyyy-MM-dd"
        parser.locale = Locale(identifier: "en_US_POSIX")
        parser.timeZone = TimeZone(identifier: "UTC")
        guard let date = parser.date(from: day) else { return day }
        let style = Date.FormatStyle(timeZone: TimeZone(identifier: "UTC") ?? .current).day().month(.abbreviated)
        return date.formatted(style)
    }

    static func bridgeName(_ kind: String) -> String {
        kind.prefix(1).uppercased() + kind.dropFirst()
    }

    static func notice(_ notice: SourceNoticeInfo) -> String {
        switch notice.kind {
        case .saved:
            return L10n.format("acq.src.saved", notice.name)
        case .pauseFailed:
            return failed(L10n.tr("acq.src.pause.failed"), notice.error)
        case .resumeFailed:
            return failed(L10n.tr("acq.src.resume.failed"), notice.error)
        case .removeFailed:
            return failed(L10n.tr("acq.src.remove.failed"), notice.error)
        case .bridgeConnected:
            let body = notice.detail == "jackett" ? L10n.plural("acq.src.bridge.added", notice.count) : L10n.tr("acq.src.bridge.arrive")
            return L10n.format("acq.src.bridge.connected", notice.name) + ". " + body
        case .bridgeDisconnected:
            return L10n.format("acq.src.bridge.disconnected", notice.name)
        case .bridgeSynced:
            let counts = L10n.format("acq.src.bridge.sync.body", notice.added, notice.removed, notice.changed)
            return L10n.tr("acq.src.bridge.synced") + ". " + counts
        case .bridgeUpToDate:
            return L10n.tr("acq.src.bridge.up.to.date")
        case .bridgeSyncingAgain:
            return L10n.format("acq.src.bridge.syncing.again", notice.name)
        case .bridgeSyncFailed:
            return failed(L10n.tr("acq.src.bridge.sync.failed"), notice.error)
        }
    }

    private static func failed(_ text: String, _ error: ErrorInfo?) -> String {
        guard let error else { return text }
        return text + ". " + error.sourceText
    }
}
