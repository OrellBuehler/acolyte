import Foundation

enum RequestsWords {
    static func memberTab(_ tab: MemberTabKind) -> String {
        switch tab {
        case .waiting: L10n.tr("requests.tab.waiting")
        case .approved: L10n.tr("requests.tab.approved")
        case .declined: L10n.tr("requests.tab.declined")
        case .ready: L10n.tr("requests.tab.ready")
        case .failed: L10n.tr("requests.tab.failed")
        }
    }

    static func approverTab(_ tab: ApproverTabKind) -> String {
        switch tab {
        case .waiting: L10n.tr("requests.tab.waiting")
        case .processing: L10n.tr("requests.tab.processing")
        case .done: L10n.tr("requests.tab.done")
        }
    }

    static func stage(_ stage: StageKind) -> String {
        switch stage {
        case .failed: L10n.tr("requests.stage.failed")
        case .waiting: L10n.tr("requests.stage.waiting")
        case .upcoming: L10n.tr("requests.stage.upcoming")
        case .searching: L10n.tr("requests.stage.searching")
        case .downloading: L10n.tr("requests.stage.downloading")
        case .importing: L10n.tr("requests.stage.importing")
        case .needsDecision: L10n.tr("requests.stage.needs.decision")
        case .looking: L10n.tr("requests.stage.looking")
        case .processing: L10n.tr("requests.stage.processing")
        case .ready: L10n.tr("requests.stage.ready")
        case .declined: L10n.tr("requests.stage.declined")
        }
    }

    static func kindFilter(_ kind: KindFilterKind) -> String {
        switch kind {
        case .all: L10n.tr("requests.kind.all")
        case .movie: L10n.tr("requests.kind.movies")
        case .show: L10n.tr("requests.kind.shows")
        case .artist: L10n.tr("requests.kind.music")
        }
    }

    static func kind(_ kind: String) -> String {
        switch kind {
        case "show": L10n.tr("requests.kind.show")
        case "artist": L10n.tr("requests.kind.artist")
        default: L10n.tr("requests.kind.movie")
        }
    }

    static func quality(_ line: String, _ name: String) -> String {
        if !name.isEmpty { return name }
        return line == "4k" ? L10n.tr("requests.quality.four.k") : L10n.tr("requests.quality.standard")
    }

    static func wait(_ wait: WaitInfo?) -> String {
        guard let wait else { return L10n.tr("requests.wait.few.days") }
        if wait.underMinute { return L10n.tr("requests.wait.minute") }
        let days = L10n.plural("requests.days", wait.days)
        let hours = L10n.plural("requests.hours", wait.hours)
        let minutes = L10n.plural("requests.minutes", wait.minutes)
        if wait.days > 0, wait.hours > 0 { return L10n.format("requests.wait.and", days, hours) }
        if wait.days > 0 { return days }
        if wait.hours > 0, wait.minutes > 0 { return L10n.format("requests.wait.and", hours, minutes) }
        if wait.hours > 0 { return hours }
        return minutes
    }

    static func waitFor(seconds: Int) -> WaitInfo {
        let millis = Int64(seconds) * 1000
        if millis <= 60000 { return WaitInfo(days: 0, hours: 0, minutes: 1, underMinute: true) }
        let minutes: Int64 = millis > 86_400_000 ? ((millis + 3_599_999) / 3_600_000) * 60 : (millis + 59999) / 60000
        return WaitInfo(days: Int(minutes / 1440), hours: Int((minutes % 1440) / 60), minutes: Int(minutes % 60), underMinute: false)
    }

    static func limit(_ line: LimitLineInfo) -> String {
        let key: String = switch line.kind {
        case .movies: "requests.movie.asks"
        case .shows: "requests.show.asks"
        case .music: "requests.music.asks"
        }
        if line.left > 0 {
            return L10n.format("requests.limit.left", L10n.plural(key, line.left), line.perWeek)
        }
        return L10n.format("requests.limit.used", L10n.plural(key, line.perWeek), wait(line.wait))
    }

    static func names(_ names: [String]) -> String {
        guard names.count > 1 else { return names.first ?? "" }
        return L10n.format("requests.names.join", names.dropLast().joined(separator: ", "), names[names.count - 1])
    }

    static func seasons(_ seasons: [Int]) -> String {
        if seasons.isEmpty { return L10n.tr("requests.seasons.whole") }
        let regular = seasons.filter { $0 > 0 }.sorted()
        var parts: [String] = []
        if seasons.contains(0) { parts.append(L10n.tr("requests.season.specials")) }
        if regular.count == 1 { parts.append(L10n.format("requests.season.one", regular[0])) }
        if regular.count > 1 { parts.append(L10n.format("requests.season.many", regular.map(String.init).joined(separator: ", "))) }
        return parts.joined(separator: ", ")
    }

    static func seasonName(_ number: Int) -> String {
        number == 0 ? L10n.tr("requests.season.specials") : L10n.format("requests.season.one", number)
    }

    static func albumKinds(_ kind: AlbumKindInfo, _ count: Int) -> String {
        switch kind {
        case .albums: L10n.plural("requests.albums", count)
        case .singles: L10n.plural("requests.singles", count)
        case .live: L10n.plural("requests.live.albums", count)
        case .compilations: L10n.plural("requests.compilations", count)
        case .other: L10n.plural("requests.other.albums", count)
        }
    }

    static func albumKind(_ kind: AlbumKindInfo) -> String {
        switch kind {
        case .albums: L10n.tr("requests.album.kind.album")
        case .singles: L10n.tr("requests.album.kind.single")
        case .live: L10n.tr("requests.album.kind.live")
        case .compilations: L10n.tr("requests.album.kind.compilation")
        case .other: L10n.tr("requests.album.kind.other")
        }
    }

    static func askedWhat(_ ask: AskInfo) -> String {
        switch ask.kind {
        case "show":
            if ask.futureOnly { return L10n.tr("requests.seasons.future") }
            return ask.askedWhat.isEmpty ? seasons(ask.seasons) : ask.askedWhat
        case "artist":
            return ask.askedWhat.isEmpty ? L10n.tr("requests.albums.every") : ask.askedWhat
        default:
            return ""
        }
    }

    static func facts(_ ask: AskInfo) -> String {
        var parts: [String] = []
        if let tag = ask.qualityTag { parts.append(quality(tag, ask.qualityName)) }
        parts.append(kind(ask.kind))
        if ask.year > 0 { parts.append(String(ask.year)) }
        let what = askedWhat(ask)
        if !what.isEmpty { parts.append(what) }
        return parts.joined(separator: " · ")
    }

    static func titleWithYear(_ name: String, _ year: Int) -> String {
        year > 0 ? "\(name) (\(year))" : name
    }

    static func memberStatus(_ ask: AskInfo) -> String {
        if ask.state == "asked" { return L10n.tr("requests.status.requested") }
        if ask.state == "turned_down" { return L10n.tr("requests.status.declined") }
        if ask.state == "available" { return L10n.tr(ask.isArtist ? "requests.status.ready.listen" : "requests.status.ready.watch") }
        if ask.isFailed { return L10n.tr("requests.status.failed") }
        return L10n.tr("requests.status.approved")
    }

    static func step(_ ask: AskInfo) -> String? {
        switch ask.step {
        case .searching: L10n.tr("requests.step.searching")
        case .downloading: L10n.tr("requests.step.downloading")
        case .importing: L10n.tr("requests.step.importing")
        case .waitingOnAdmin: L10n.tr("requests.step.admin")
        case .notOut: L10n.tr("requests.step.not.out")
        case .waiting:
            if let at = ask.searchedAt { L10n.format("requests.step.nothing.found", Format.relative(at)) } else { L10n.tr("requests.step.not.looked") }
        case .idle: nil
        }
    }

    static func keeps(_ ask: AskInfo) -> String {
        switch ask.kind {
        case "show": L10n.tr("requests.keeps.show")
        case "artist": L10n.tr("requests.keeps.artist")
        default: L10n.tr("requests.keeps.movie")
        }
    }

    static func error(_ e: RequestErrorInfo) -> String {
        switch e.failure {
        case .weeklyLimit:
            if let seconds = e.error.retryAfter { return L10n.format("requests.error.limit.wait", wait(waitFor(seconds: seconds))) }
            return L10n.tr("requests.error.limit")
        case .neverAdd: return L10n.tr("requests.error.never.add")
        case .inLibrary: return L10n.tr("requests.error.in.library")
        case .ageLimit: return L10n.tr("requests.error.age")
        case .noMusic: return L10n.tr("requests.error.no.music")
        case .alreadyDecided: return L10n.tr("requests.error.decided")
        case .askedAgain: return L10n.tr("requests.error.asked.again")
        case .notIncluded: return L10n.tr("requests.error.not.included")
        case .other:
            if !e.error.message.isEmpty, (400 ..< 500).contains(e.error.status) { return e.error.message }
            return e.error.localized
        }
    }

    static func reportKind(_ kind: ReportKindInfo) -> String {
        switch kind {
        case .picture: L10n.tr("requests.report.kind.picture")
        case .sound: L10n.tr("requests.report.kind.sound")
        case .subtitles: L10n.tr("requests.report.kind.subtitles")
        case .wontPlay: L10n.tr("requests.report.kind.wont.play")
        case .other: L10n.tr("requests.report.kind.other")
        }
    }

    static func reportHint(_ kind: ReportKindInfo) -> String {
        switch kind {
        case .picture: L10n.tr("requests.report.hint.picture")
        case .sound: L10n.tr("requests.report.hint.sound")
        case .subtitles: L10n.tr("requests.report.hint.subtitles")
        case .wontPlay: L10n.tr("requests.report.hint.wont.play")
        case .other: L10n.tr("requests.report.hint.other")
        }
    }

    static func reportFacts(_ r: ReportInfo) -> String {
        let part = r.version.isEmpty ? r.part : L10n.format("requests.report.version", r.version)
        return [kind(r.titleKind), r.year > 0 ? String(r.year) : "", part].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    static func page(_ page: RequestPageKind) -> String {
        switch page {
        case .rules: L10n.tr("requests.rules.title")
        case .outsideLists: L10n.tr("requests.outside.title")
        case .neverAdd: L10n.tr("requests.never.title")
        }
    }

    static func route(_ page: RequestPageKind) -> Route {
        switch page {
        case .rules: .settingsRequests
        case .outsideLists: .settingsOutsideLists
        case .neverAdd: .settingsNeverAdd
        }
    }

    static func outcomeTitle(_ outcome: AskOutcomeKind, _ name: String, fourK: Bool) -> String {
        switch outcome {
        case .approved: L10n.format("requests.outcome.approved", name)
        case .joined, .already: L10n.format("requests.outcome.already", name)
        case .turnedDown: L10n.format("requests.outcome.turned.down", name)
        case .requested: L10n.format(fourK ? "requests.outcome.requested.four.k" : "requests.outcome.requested", name)
        }
    }

    static func outcomeMessage(_ outcome: AskOutcomeKind) -> String {
        switch outcome {
        case .approved: L10n.tr("requests.outcome.searching")
        case .joined, .already: L10n.tr("requests.outcome.on.it")
        case .turnedDown: L10n.tr("requests.outcome.on.services")
        case .requested: L10n.tr("requests.outcome.waiting")
        }
    }

    static func bytes(_ bytes: Int64) -> String {
        let units = ["B", "KB", "MB", "GB", "TB"]
        var value = Double(bytes)
        var unit = 0
        while value >= 1024, unit < units.count - 1 {
            value /= 1024
            unit += 1
        }
        if value < 10, unit > 0 { return String(format: "%.1f %@", value, units[unit]) }
        return "\(Int64(value)) \(units[unit])"
    }

    static func region(_ code: String) -> String {
        Locale.current.localizedString(forRegionCode: code) ?? code
    }

    static func every(_ hours: Int) -> String {
        if hours == 24 { return L10n.tr("requests.outside.every.day") }
        if hours > 0, hours % 24 == 0 { return L10n.format("requests.outside.every.days", hours / 24) }
        return L10n.format("requests.outside.every.hours", hours)
    }

    static func listKind(_ kind: ListKindInfo) -> String {
        switch kind {
        case .tmdb: L10n.tr("requests.outside.kind.tmdb")
        case .trakt: L10n.tr("requests.outside.kind.trakt")
        case .mdblist: L10n.tr("requests.outside.kind.mdblist")
        }
    }

    static func listSeasons(_ seasons: ListSeasonsKind) -> String {
        switch seasons {
        case .all: L10n.tr("requests.outside.seasons.all")
        case .future: L10n.tr("requests.outside.seasons.future")
        case .latest: L10n.tr("requests.outside.seasons.latest")
        case .first: L10n.tr("requests.outside.seasons.first")
        case .noSeasons: L10n.tr("requests.outside.seasons.none")
        }
    }
}
