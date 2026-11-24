import SwiftUI

struct TileText {
    let symbol: String
    let label: String
    let big: String
    let small: String
    var tone: ToneKind?
    var posters: [String] = []
}

enum HomeText {
    static func news(_ news: HomeNewsInfo?) -> String? {
        guard let news else { return nil }
        switch news.kind {
        case .askedArrivedAny: return L10n.tr("home.news.asked.any")
        case .askedArrivedOne: return L10n.format("home.news.asked.one", news.title)
        case .askedArrivedMany: return L10n.format("home.news.asked.many", news.count)
        case .waiting: return L10n.plural("home.news.waiting", news.count)
        case .waitingPage: return L10n.plural("home.news.waiting.page", news.count)
        case .newTodayAny: return L10n.tr("home.news.new.any")
        case .newTodayOne: return L10n.format("home.news.new.one", news.title)
        case .newTodayMany: return L10n.format("home.news.new.many", news.count)
        case .mineFinishing: return L10n.tr("home.news.mine.finishing")
        case .finishing: return L10n.plural("home.news.finishing", news.count)
        }
    }

    static func arrived(_ title: ArrivedTitleID) -> String {
        switch title {
        case .newToday: L10n.tr("home.new.today")
        case .newThisWeek: L10n.tr("home.new.this.week")
        case .justArrived: L10n.tr("home.just.arrived")
        case .recentlyAdded: L10n.tr("home.recently.added")
        }
    }

    static func picture(_ picture: HomePictureInfo) -> String {
        switch picture.label {
        case .newSeason: L10n.tr("home.pill.new.season")
        case .comingSoon: L10n.tr("home.pill.coming.soon")
        case .following: L10n.tr("home.pill.following")
        case .arrived: arrived(picture.arrived)
        }
    }

    static func section(_ kind: CardKindID) -> SectionID {
        switch kind {
        case .movie: .movies
        case .show: .shows
        case .artist: .music
        }
    }

    static func kinds(_ kind: CardKindID) -> String {
        section(kind).title
    }

    static func stepTitle(_ kind: SetupStepKindID) -> String {
        switch kind {
        case .folder: L10n.tr("home.step.folder")
        case .source: L10n.tr("home.step.source")
        case .downloader: L10n.tr("home.step.downloader")
        case .title: L10n.tr("home.step.title")
        }
    }

    static func stepDetail(_ kind: SetupStepKindID) -> String {
        switch kind {
        case .folder: L10n.tr("home.step.folder.detail")
        case .source: L10n.tr("home.step.source.detail")
        case .downloader: L10n.tr("home.step.downloader.detail")
        case .title: L10n.tr("home.step.title.detail")
        }
    }

    static func stepAction(_ kind: SetupStepKindID) -> String {
        switch kind {
        case .folder: L10n.tr("home.step.folder.action")
        case .source: L10n.tr("home.step.source")
        case .downloader: L10n.tr("home.step.downloader")
        case .title: L10n.tr("home.step.title.action")
        }
    }

    static func when(_ up: ComingUpInfo) -> String {
        switch up.when {
        case .outNow: L10n.tr("home.out.now")
        case .today: L10n.tr("discover.day.today")
        case .date: DiscoverText.shortDate(up.date)
        case .text: up.text
        }
    }

    static func bytes(_ value: Int64) -> String {
        let units = ["B", "KB", "MB", "GB", "TB"]
        var size = Double(value)
        var unit = 0
        while size >= 1024, unit < units.count - 1 {
            size /= 1024
            unit += 1
        }
        return size < 10 && unit > 0 ? String(format: "%.1f %@", size, units[unit]) : "\(Int64(size)) \(units[unit])"
    }

    static func duration(_ seconds: Int64) -> String {
        if seconds < 60 { return "\(seconds)s" }
        if seconds < 3600 { return "\(seconds / 60)m" }
        if seconds < 48 * 3600 { return "\(seconds / 3600)h \((seconds % 3600) / 60)m" }
        return "\(seconds / 86400)d \((seconds % 86400) / 3600)h"
    }

    static func attentionTitle(_ row: AttentionRowInfo) -> String {
        switch row.kind {
        case .downloadFailed, .importRefused:
            row.count > 1 ? L10n.plural("home.downloads.failed", row.count) : row.title
        case .decision:
            row.count > 1 ? L10n.plural("home.need.decision", row.count) : row.title
        case .health, .setup:
            row.count > 1 ? "\(row.count) × \(healthWord(row.checkKind))" : healthWord(row.checkKind)
        }
    }

    static func attentionReason(_ row: AttentionRowInfo) -> String {
        switch row.kind {
        case .downloadFailed:
            return row.count > 1 ? row.names.joined(separator: ", ") : L10n.tr("home.reason.download.failed")
        case .importRefused:
            return L10n.tr("home.reason.import.refused")
        case .decision:
            return row.count > 1 ? row.names.joined(separator: ", ") : L10n.tr("home.reason.decision")
        case .health, .setup:
            let reason = healthReason(row.checkKind, error: row.error, many: row.count > 1)
            let names = row.names.joined(separator: ", ")
            return names.isEmpty ? reason : "\(names): \(reason)"
        }
    }

    static func actionWord(_ row: AttentionRowInfo) -> String {
        if row.kind == .decision { return L10n.tr("home.action.decide") }
        switch row.target {
        case .downloads: return SectionID.downloads.title
        case .settings: return L10n.tr("home.action.fix")
        case .health: return L10n.tr("home.action.look")
        }
    }

    static func healthWord(_ kind: String) -> String {
        switch kind {
        case "library_folder", "disk_space": L10n.tr("home.check.library.folder")
        case "source": L10n.tr("home.check.source")
        case "downloader", "save_folder", "no_downloader", "same_downloader": L10n.tr("home.check.downloader")
        case "backups": L10n.tr("home.check.backups")
        case "update": L10n.tr("home.check.update")
        case "config_disk", "config_folder": L10n.tr("home.check.config")
        case "waiting_downloads", "clear_waiting": SectionID.downloads.title
        case "key_kit": L10n.tr("home.check.master.key")
        case "trailers": L10n.tr("home.check.trailers")
        case "people_check": L10n.tr("home.check.people")
        default: L10n.tr("home.check.holyarrs")
        }
    }

    static func healthReason(_ kind: String, error: Bool, many: Bool) -> String {
        switch kind {
        case "library_folder": L10n.tr(error ? "home.reason.unreachable" : "home.reason.needs.look")
        case "disk_space": L10n.tr(error ? "home.reason.out.of.space" : "home.reason.low.space")
        case "source", "downloader": L10n.tr(error ? "home.reason.not.answering" : "home.reason.problem")
        case "backups": L10n.tr(error ? "home.reason.backups.missing" : "home.reason.backups.old")
        case "update": L10n.tr(error ? "home.reason.check.failed" : "home.reason.waiting")
        case "config_disk": L10n.tr(error ? "home.reason.almost.full" : "home.reason.low.space")
        case "config_folder": L10n.tr(error ? "home.reason.container" : "home.reason.needs.look")
        case "waiting_downloads": L10n.tr("home.reason.waiting.files")
        case "clear_waiting": L10n.tr("home.reason.waiting.cleared")
        case "save_folder": L10n.tr(many ? "home.reason.no.save.folder.many" : "home.reason.no.save.folder")
        case "no_downloader": L10n.tr("home.reason.no.downloader")
        case "same_downloader": L10n.tr(many ? "home.reason.added.twice.many" : "home.reason.added.twice")
        case "key_kit": L10n.tr("home.reason.no.key.kit")
        case "trailers": L10n.tr(error ? "home.reason.not.loading" : "home.reason.lookups.failing")
        case "people_check": L10n.tr("home.reason.users")
        default: L10n.tr(error ? "home.reason.not.working" : "home.reason.needs.look")
        }
    }

    static func tile(_ tile: HomeTileInfo, info: HomeInfo) -> TileText {
        switch tile.kind {
        case .setup:
            let label = info.nextStep?.kind == .folder ? SectionID.library.title : SectionID.downloads.title
            if let next = info.nextStep, next.kind != .title {
                return TileText(symbol: "books.vertical", label: label, big: L10n.tr("home.tile.not.set.up"), small: stepTitle(next.kind))
            }
            return TileText(symbol: "books.vertical", label: label, big: stepTitle(.title), small: stepDetail(.title))
        case .attention:
            return attention(info)
        case .asks:
            return asks(info)
        case .library:
            return library(info)
        case .disk:
            let disk = info.disk
            let small: String
            if disk == nil || disk?.failed == true {
                small = L10n.tr("home.disk.unread")
            } else if let free = disk?.free {
                if free == 0 {
                    small = L10n.tr("home.disk.full")
                } else if disk?.many == true {
                    small = L10n.format("home.disk.free.fullest", bytes(free))
                } else {
                    small = L10n.format("home.disk.free", bytes(free))
                }
            } else {
                small = L10n.tr("home.disk.across")
            }
            let tone: ToneKind? = disk?.tone == .fail || disk?.tone == .wait ? disk?.tone : nil
            return TileText(symbol: "archivebox", label: L10n.tr("home.tile.disk"), big: bytes(disk?.size ?? 0), small: small, tone: tone)
        case .week:
            let week = info.week
            return TileText(
                symbol: "calendar",
                label: L10n.tr("home.tile.week"),
                big: String(week?.count ?? 0),
                small: L10n.tr(week?.borrowed == true ? "home.tile.week.borrowed" : "home.tile.week.own"),
                posters: week?.posters ?? []
            )
        case .downloads:
            let d = info.downloads
            var words: [String] = []
            if let d {
                if d.downloading > 0 { words.append(L10n.format("home.stage.downloading", d.downloading)) }
                if d.importing > 0 { words.append(L10n.format("home.stage.importing", d.importing)) }
                if d.waiting > 0 { words.append(L10n.format("home.stage.waiting", d.waiting)) }
                if d.paused > 0 { words.append(L10n.format("home.stage.paused", d.paused)) }
            }
            return TileText(
                symbol: "arrow.down.to.line",
                label: SectionID.downloads.title,
                big: String(d?.fetching ?? 0),
                small: words.joined(separator: " · ")
            )
        case .new:
            let small: String
            if info.newArrivals > 0, info.newSeasons > 0 {
                small = L10n.tr("home.new.both")
            } else if info.newArrivals > 0 {
                small = L10n.tr("home.new.arrivals")
            } else {
                small = L10n.plural("home.new.seasons", info.newSeasons)
            }
            return TileText(symbol: "sparkles", label: L10n.tr("home.tile.new"), big: String(info.newArrivals + info.newSeasons), small: small)
        case .explore:
            return TileText(
                symbol: "safari",
                label: SectionID.explore.title,
                big: L10n.tr("home.tile.explore.big"),
                small: L10n.tr("home.tile.explore.small")
            )
        case .popular:
            let big = info.popularBuilding && info.popularArt == nil ? L10n.tr("home.getting.ready") : L10n.tr("home.tile.popular.big")
            return TileText(symbol: "star", label: SectionID.popular.title, big: big, small: L10n.tr("home.tile.popular.small"))
        case .calendar:
            return TileText(
                symbol: "calendar",
                label: SectionID.calendar.title,
                big: L10n.tr("home.tile.calendar.big"),
                small: L10n.tr("home.tile.calendar.small")
            )
        case .search:
            return TileText(
                symbol: "magnifyingglass",
                label: SectionID.search.title,
                big: L10n.tr("home.tile.search.big"),
                small: L10n.tr("home.tile.search.small")
            )
        }
    }

    private static func attention(_ info: HomeInfo) -> TileText {
        let label = L10n.tr("home.tile.attention")
        guard info.settled else { return TileText(symbol: "exclamationmark.triangle", label: label, big: "…", small: "") }
        let att = info.attention
        let rows = att?.rows ?? []
        if let first = rows.first {
            let line = attentionTitle(first) + ": " + attentionReason(first)
            let more = att?.more ?? 0
            return TileText(
                symbol: "exclamationmark.triangle",
                label: label,
                big: String(rows.reduce(0) { $0 + $1.count }),
                small: more > 0 ? L10n.format("home.tile.and.more", line, more) : line,
                tone: att?.worst
            )
        }
        if att?.unchecked == true {
            return TileText(symbol: "exclamationmark.triangle", label: label, big: L10n.tr("home.could.not.check"), small: L10n.tr("home.try.again"))
        }
        return TileText(symbol: "checkmark.circle", label: label, big: L10n.tr("home.all.good"), small: L10n.tr("home.all.good.line"), tone: .ok)
    }

    private static func asks(_ info: HomeInfo) -> TileText {
        guard let asks = info.asks, !asks.failed else {
            return TileText(
                symbol: "checklist",
                label: SectionID.requests.title,
                big: L10n.tr("home.could.not.read"),
                small: L10n.tr("home.could.not.read.asks")
            )
        }
        if asks.approver, asks.waiting > 0 {
            return TileText(
                symbol: "checklist",
                label: L10n.tr("home.tile.to.approve"),
                big: String(asks.waiting),
                small: L10n.plural("home.tile.to.look.at", asks.waiting),
                posters: asks.posters
            )
        }
        if asks.open > 0 {
            let open = L10n.tr("home.tile.still.open")
            let small = asks.declined > 0 ? open + " · " + L10n.format("home.tile.turned.down", asks.declined) : open
            return TileText(symbol: "checklist", label: L10n.tr("home.tile.your.asks"), big: String(asks.open), small: small)
        }
        if asks.declined > 0 {
            return TileText(symbol: "checklist", label: SectionID.requests.title, big: String(asks.declined), small: L10n.tr("home.tile.turned.down.word"))
        }
        return TileText(
            symbol: "checklist",
            label: SectionID.requests.title,
            big: L10n.tr("home.tile.nothing.waiting"),
            small: L10n.tr(asks.approver ? "home.tile.no.asks" : "home.tile.ask.on.explore")
        )
    }

    private static func library(_ info: HomeInfo) -> TileText {
        let kinds = info.library.filter { $0.titles > 0 }
        let shown = kinds.isEmpty ? Array(info.library.prefix(1)) : kinds
        let small: String
        if shown.count == 1, let kind = shown.first {
            switch kind.kind {
            case .show: small = L10n.plural("home.seasons", kind.parts) + " · " + L10n.plural("home.episodes", kind.items)
            case .artist: small = L10n.plural("home.albums", kind.parts) + " · " + L10n.plural("home.tracks", kind.items)
            case .movie: small = L10n.plural("home.movies", kind.titles)
            }
        } else {
            small = shown.map { kind in
                switch kind.kind {
                case .movie: L10n.plural("home.movies", kind.titles)
                case .show: L10n.plural("home.shows", kind.titles)
                case .artist: L10n.plural("home.artists", kind.titles)
                }
            }
            .joined(separator: " · ")
        }
        return TileText(symbol: "books.vertical", label: SectionID.library.title, big: String(info.libraryTotal), small: small)
    }
}

struct SystemBoardView: View {
    let board: SystemBoardInfo
    let info: HomeInfo
    @Environment(Navigator.self) private var navigator

    var body: some View {
        VStack(spacing: Theme.Spacing.medium) {
            Button { navigator.open(.downloads) } label: { downloads }.buttonStyle(.plain)
            attention
            HStack(alignment: .top, spacing: Theme.Spacing.medium) {
                part(L10n.tr("home.check.sources"), board.sources, off: board.sourcesOff, add: L10n.tr("home.sys.add.source"))
                part(L10n.tr("home.check.downloaders"), board.downloaders, off: false, add: L10n.tr("home.sys.add.downloader"))
            }
            Button { navigator.open(.health) } label: {
                card {
                    Text(L10n.tr("home.sys.running.for")).font(.headline)
                    Text(HomeText.duration(board.uptimeSeconds)).font(.title2.bold())
                    if !board.version.isEmpty {
                        Text(L10n.format("home.sys.version", board.version)).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }

    private var downloads: some View {
        card {
            Text(L10n.tr("home.sys.downloading")).font(.headline)
            if board.downloadsFailed {
                Text(L10n.tr("home.could.not.read")).font(.title2.bold())
                Text(L10n.tr("home.could.not.read.downloads")).font(.footnote).foregroundStyle(Theme.textMuted)
            } else {
                Text(board.speed > 0 ? HomeText.bytes(Int64(board.speed)) + "/s" : L10n.format("home.stage.downloading", board.downloading))
                    .font(.title2.bold())
                HStack(spacing: Theme.Spacing.large) {
                    counter(L10n.tr("home.sys.waiting"), board.waiting)
                    counter(L10n.tr("home.sys.downloading"), board.downloading)
                    counter(L10n.tr("home.sys.importing"), board.importing)
                }
                ForEach(board.running) { download in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(download.name).font(.subheadline.weight(.semibold)).lineLimit(1)
                        Text(stateLine(download)).font(.footnote).foregroundStyle(Theme.textMuted)
                        if download.stage == .downloading {
                            ProgressView(value: min(max(download.progress, 0), 1)).tint(Theme.accent)
                        }
                    }
                }
                if board.waiting + board.downloading + board.importing == 0 {
                    if board.finished.isEmpty {
                        Text(L10n.tr("home.sys.idle")).font(.footnote).foregroundStyle(Theme.textMuted)
                    } else {
                        ForEach(board.finished) { done in
                            let when = done.finishedAt.map { L10n.format("home.sys.finished", Format.relative($0)) }
                            Text([done.name, when ?? ""].filter { !$0.isEmpty }.joined(separator: " · "))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                    }
                    if board.paused > 0 {
                        Text(L10n.format("home.sys.paused", board.paused)).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                    if board.notSent > 0 {
                        Text(L10n.format("home.sys.not.sent", board.notSent)).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            }
        }
    }

    private var attention: some View {
        card {
            Text(L10n.tr("home.tile.attention")).font(.headline)
            if !board.attention.isEmpty {
                ForEach(board.attention.prefix(4)) { row in
                    HStack(spacing: Theme.Spacing.small) {
                        Circle().fill(Theme.dotColor(row.tone)).frame(width: 8, height: 8)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(HomeText.attentionTitle(row)).font(.subheadline.weight(.semibold))
                            Text(HomeText.attentionReason(row)).font(.footnote).foregroundStyle(Theme.textMuted)
                        }
                        Spacer(minLength: 0)
                        Button(HomeText.actionWord(row)) { navigator.open(row.target.section) }
                            .font(.subheadline.weight(.semibold))
                    }
                }
                if board.attention.count > 4 {
                    Text(L10n.format("home.sys.more", board.attention.count - 4)).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                if board.attention.count <= 2, board.passed > 0 {
                    Text(L10n.plural("home.sys.passed", board.passed)).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            } else if board.unchecked {
                Text(L10n.tr("home.could.not.check")).font(.title2.bold())
                Text(L10n.tr("home.sys.unchecked")).font(.footnote).foregroundStyle(Theme.textMuted)
            } else {
                Text(L10n.tr("home.all.good")).font(.title2.bold()).foregroundStyle(Theme.okText)
                Text(L10n.tr("home.sys.all.good")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }

    private func part(_ label: String, _ count: PartCountInfo?, off: Bool, add: String) -> some View {
        Button { navigator.open(.settings) } label: {
            card {
                Text(label).font(.subheadline.weight(.semibold))
                if off {
                    Text(L10n.tr("home.sys.off")).font(.title3.bold())
                    Text(L10n.tr("home.sys.off.parts")).font(.footnote).foregroundStyle(Theme.textMuted)
                } else if let count {
                    if count.total == 0 {
                        Text(L10n.tr("home.sys.none.added")).font(.title3.bold())
                        Text(add).font(.footnote).foregroundStyle(Theme.textMuted)
                    } else {
                        Text(L10n.format("home.sys.working", count.working, count.total)).font(.title3.bold())
                        Text(partLine(count))
                            .font(.footnote)
                            .foregroundStyle(count.troubled.isEmpty ? Theme.textMuted : Theme.failText)
                    }
                } else {
                    Text(L10n.tr("home.could.not.read")).font(.title3.bold())
                    Text(L10n.tr("home.could.not.read.parts")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
        }
        .buttonStyle(.plain)
    }

    private func partLine(_ count: PartCountInfo) -> String {
        if count.troubled.count == 1 { return L10n.format("home.sys.has.problem", count.troubled[0]) }
        if count.troubled.count > 1 { return L10n.format("home.sys.have.problem", count.troubled.count) }
        if count.off > 0 { return L10n.format("home.sys.turned.off", count.off) }
        return L10n.tr("home.sys.all.working")
    }

    private func stateLine(_ download: SystemDownloadInfo) -> String {
        let percent = Int(min(max(download.progress, 0), 1) * 100)
        switch download.stage {
        case .downloading:
            return download.speed > 0 ? "\(percent)% · \(HomeText.bytes(Int64(download.speed)))/s" : "\(percent)%"
        case .queued: return L10n.tr("home.sys.waiting")
        case .importing: return L10n.tr("home.sys.importing")
        case .checking: return L10n.tr("home.sys.checking")
        case .stalled: return L10n.format("home.sys.stalled", percent)
        default: return ""
        }
    }

    private func counter(_ label: String, _ value: Int) -> some View {
        VStack(alignment: .leading) {
            Text(String(value)).font(.title3.weight(.semibold))
            Text(label).font(.footnote).foregroundStyle(Theme.textMuted)
        }
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) { content() }
            .foregroundStyle(Theme.text)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.large)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}
