import Foundation
import SwiftUI

enum TitleText {
    static func library(_ status: LibraryStatusInfo) -> String {
        switch status.label {
        case .inlibrary: L10n.tr("title.state.in.library")
        case .downloaded: L10n.tr("title.state.downloaded")
        case .downloading: L10n.tr("title.state.downloading")
        case .downloadingpercent: L10n.format("title.state.downloading.percent", status.count)
        case .downloadingepisodes: L10n.plural("title.state.downloading.episodes", status.count)
        case .failedepisodes: L10n.plural("title.state.failed.episodes", status.count)
        case .episodesof: L10n.format("title.state.episodes.of", status.count, status.total)
        case .downloadfailed: L10n.tr("title.state.download.failed")
        case .notdownloaded: L10n.tr("title.state.not.downloaded")
        }
    }

    static func pill(_ pill: HeroPillKind, mine: Bool) -> String? {
        let base: String
        switch pill {
        case HeroPillKind.none: return nil
        case .hidden: return L10n.tr("title.pill.hidden")
        case .neveradd: return L10n.tr("title.pill.never.add")
        case .asked: base = L10n.tr("title.pill.asked")
        case .approved: base = L10n.tr("title.pill.approved")
        case .turneddown: base = L10n.tr("title.pill.turned.down")
        case .available: base = L10n.tr("title.pill.available")
        }
        return mine ? L10n.format("title.pill.you.asked", base) : base
    }

    static func feedback(_ f: FeedbackInfo) -> String {
        switch f.kind {
        case .failed: return f.error?.localized ?? (f.text.isEmpty ? L10n.tr("error.unknown") : f.text)
        case .searching: return L10n.format("title.note.searching", f.text)
        case .searchingseason: return L10n.format("title.note.searching.season", f.count)
        case .searchingepisode: return L10n.format("title.note.searching.episode", f.text)
        case .searchingmissing: return L10n.tr("title.note.searching.missing")
        case .nothingtosearch: return L10n.tr("title.note.nothing.to.search")
        case .turnshowon: return L10n.tr("title.note.turn.show.on")
        case .refreshing: return L10n.format("title.note.refreshing", f.text)
        case .saved, .versionadded, .versionsaved, .filesaved: return L10n.format("title.note.saved", f.text)
        case .orderchanged: return L10n.tr("title.note.order.changed")
        case .moving: return L10n.format("title.note.moving", f.text)
        case .markedfailed: return L10n.tr("title.note.marked.failed")
        case .versionremoved: return L10n.format("title.note.version.removed", f.text)
        case .fileadopted: return L10n.format("title.note.file.adopted", f.text)
        case .packnext: return L10n.tr("title.note.pack.next")
        case .hidden: return f.text.isEmpty ? L10n.tr("title.note.hidden.everyone") : L10n.format("title.note.hidden.for", f.text)
        case .shownagain: return L10n.format("title.note.shown.again", f.text)
        case .restored: return L10n.format("title.note.restored", f.text)
        case .sent: return sent(L10n.tr("title.note.sent"), f)
        case .sending: return sent(L10n.tr("title.note.sending"), f)
        case .notsent: return L10n.format("title.note.not.sent", f.text)
        case .turneddown: return L10n.tr("title.note.turned.down")
        case .groupblocked: return L10n.format("title.note.group.blocked", f.text)
        case .subtitlessearching: return L10n.tr("title.note.subtitles.searching")
        case .subtitlesnofile: return L10n.tr("title.note.subtitles.no.file")
        case .subtitlessaved: return L10n.tr("title.note.subtitles.saved")
        case .subtitlesdefault: return L10n.tr("title.note.subtitles.default")
        case .subtitlesaved: return L10n.format("title.note.subtitle.saved", f.text)
        case .translating: return L10n.format("title.note.translating", f.text)
        case .timingfixed: return L10n.tr("title.note.timing.fixed")
        case .cleaned: return L10n.plural("title.note.cleaned", f.count)
        case .nothingtoclean: return L10n.tr("title.note.nothing.to.clean")
        case .subtitleturneddown: return L10n.tr("title.note.subtitle.turned.down")
        case .matched: return L10n.plural("title.note.matched", f.count)
        case .unmatched: return L10n.plural("title.note.unmatched", f.count)
        case .rescanning: return L10n.tr("title.note.rescanning")
        case .rescandone: return L10n.plural("title.note.rescan.done", f.count)
        case .renamed: return L10n.plural("title.note.renamed", f.count)
        case .renamechanged: return L10n.tr("title.note.rename.changed")
        case .renamefailed: return L10n.tr("title.note.rename.failed")
        case .deleted: return f.count > 0 ? L10n.format("title.note.deleted.kept", f.text, f.count) : L10n.format("title.note.deleted", f.text)
        case .added: return L10n.format("title.note.added", f.text)
        }
    }

    private static func sent(_ head: String, _ f: FeedbackInfo) -> String {
        if f.count > 1 { return head + " · " + L10n.plural("title.note.warnings", f.count) }
        if !f.text.isEmpty { return head + " · " + f.text }
        return head
    }

    static func verdict(_ kind: VerdictKindValue) -> String {
        switch kind {
        case .turneddown: L10n.tr("title.verdict.turned.down")
        case .chosen: L10n.tr("title.verdict.chosen")
        case .held: L10n.tr("title.verdict.held")
        case .wouldchoose: L10n.tr("title.verdict.would.choose")
        case .wouldhold: L10n.tr("title.verdict.would.hold")
        case .refused: L10n.tr("title.verdict.refused")
        case .passedover: L10n.tr("title.verdict.passed.over")
        case .judged: L10n.tr("title.verdict.judged")
        }
    }

    static func label(_ problem: LabelProblemKind) -> String {
        switch problem {
        case .empty: L10n.tr("title.label.empty")
        case .toolong: L10n.tr("title.label.too.long")
        case .badcharacter: L10n.tr("title.label.bad.character")
        case .trailingdot: L10n.tr("title.label.trailing.dot")
        case .devicename: L10n.tr("title.label.device")
        case .taken: L10n.tr("title.label.taken")
        }
    }

    static func wish(_ problem: WishProblemKind) -> String {
        switch problem {
        case .toomany: L10n.tr("title.wish.too.many")
        case .pick: L10n.tr("title.wish.pick")
        case .twice: L10n.tr("title.wish.twice")
        }
    }

    static func runtime(_ minutes: Int) -> String {
        if minutes <= 0 { return "" }
        if minutes < 60 { return L10n.format("title.runtime.minutes", minutes) }
        if minutes % 60 == 0 { return L10n.format("title.runtime.hours", minutes / 60) }
        return L10n.format("title.runtime.hours.minutes", minutes / 60, minutes % 60)
    }

    static func season(_ number: Int) -> String {
        number == 0 ? L10n.tr("title.specials") : L10n.format("title.season.number", number)
    }

    static func bytes(_ value: Int64) -> String {
        guard value > 0 else { return "" }
        return ByteCountFormatter.string(fromByteCount: value, countStyle: .binary)
    }

    static func date(_ value: Date?) -> String {
        guard let value else { return "" }
        return value.formatted(date: .abbreviated, time: .omitted)
    }

    static func day(_ iso: String) -> Date? {
        guard iso.count >= 10 else { return nil }
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "UTC")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: String(iso.prefix(10)))
    }

    static func dayText(_ iso: String) -> String {
        guard let date = day(iso) else { return iso }
        return date.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted, timeZone: TimeZone(identifier: "UTC") ?? .current))
    }

    static func air(_ iso: String) -> String {
        guard let date = day(iso) else { return "" }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .current
        let todayParts = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        guard let today = calendar.date(from: todayParts) else { return dayText(iso) }
        let diff = calendar.dateComponents([.day], from: today, to: date).day ?? 0
        let zone = TimeZone(identifier: "UTC") ?? .current
        switch diff {
        case 0: return L10n.tr("title.air.today")
        case -1: return L10n.tr("title.air.yesterday")
        case 1: return L10n.tr("title.air.tomorrow")
        default:
            let sameYear = calendar.component(.year, from: date) == todayParts.year
            if sameYear && abs(diff) < 7 {
                return date.formatted(Date.FormatStyle(timeZone: zone).weekday(.abbreviated).day().month(.abbreviated))
            }
            if sameYear { return date.formatted(Date.FormatStyle(timeZone: zone).day().month(.abbreviated)) }
            return dayText(iso)
        }
    }

    static func language(_ code: String) -> String {
        guard !code.isEmpty else { return "" }
        return Locale.current.localizedString(forLanguageCode: code) ?? code
    }

    static func country(_ code: String) -> String {
        Locale.current.localizedString(forRegionCode: code) ?? code
    }

    static func money(_ amount: Int64) -> String {
        "$" + Double(amount).formatted(.number.notation(.compactName).precision(.fractionLength(0 ... 1)))
    }

    static func scoreName(_ source: String) -> String {
        switch source {
        case "imdb": "IMDb"
        case "rt_critics": L10n.tr("title.score.rt.critics")
        case "rt_audience": L10n.tr("title.score.rt.audience")
        case "metacritic": "Metacritic"
        case "letterboxd": "Letterboxd"
        case "trakt": "Trakt"
        default: source
        }
    }

    static func gate(_ gate: String) -> String {
        switch gate {
        case "announced": L10n.tr("title.gate.announced")
        case "in_cinemas": L10n.tr("title.gate.in.cinemas")
        default: L10n.tr("title.gate.released")
        }
    }

    static func pack(_ window: Int) -> String {
        window == 0 ? L10n.tr("title.pack.whole") : L10n.plural("title.pack.window", window)
    }

    static func step(_ step: String) -> String {
        switch step {
        case "": return L10n.tr("title.step.unknown")
        case "cam": return L10n.tr("title.step.cam")
        case "telesync": return L10n.tr("title.step.telesync")
        case "sd_dvd": return L10n.tr("title.step.sd.dvd")
        case "sd_tv": return L10n.tr("title.step.sd.tv")
        default:
            let parts = step.split(separator: "_").map(String.init)
            let res = parts.first == "2160p" ? "4K" : (parts.first ?? "")
            let source: String
            switch parts.dropFirst().first {
            case "bluray": source = "Blu-ray"
            case "remux": source = "Remux"
            case "web": source = "Web"
            case "tv": source = "TV"
            default: source = parts.dropFirst().joined(separator: " ")
            }
            return "\(res) \(source)".trimmingCharacters(in: .whitespaces)
        }
    }

    static func episodeCode(_ season: Int, _ episode: Int) -> String {
        String(format: "S%02dE%02d", season, episode)
    }

    static func outcomeWhy(_ name: String, _ why: String) -> String {
        switch why {
        case "rate_limited": L10n.format("title.outcome.rate.limited", name)
        case "down": L10n.format("title.outcome.down", name)
        case "blocked": L10n.format("title.outcome.blocked", name)
        case "login_refused": L10n.format("title.outcome.login.refused", name)
        case "broken_definition": L10n.format("title.outcome.broken", name)
        case "bot_check": L10n.format("title.outcome.bot.check", name)
        case "site_ban": L10n.format("title.outcome.site.ban", name)
        case "paused": L10n.format("title.outcome.paused", name)
        default: why.isEmpty ? name : "\(name) (\(why))"
        }
    }

    static func leftoverReason(_ leftover: LeftoverInfo) -> String {
        switch leftover.reason {
        case "no_episode_numbers": L10n.tr("title.reason.no.numbers")
        case "whole_season": L10n.tr("title.reason.whole.season")
        case "episode_not_listed":
            leftover.droppedSeason > 0 ? L10n.format("title.reason.season.not.listed", leftover.droppedSeason) : L10n.tr("title.reason.not.listed")
        case "bigger_file_same_episode": L10n.tr("title.reason.bigger")
        case "episode_has_file": L10n.tr("title.reason.has.file")
        case "movie_more_than_one": L10n.tr("title.reason.more.than.one")
        case "movie_copy_choice": L10n.tr("title.reason.copy.choice")
        case "movie_has_file": L10n.tr("title.reason.movie.has.file")
        case "replaced_by_hand": L10n.tr("title.reason.replaced")
        case "unmatched": L10n.tr("title.reason.unmatched")
        case "seasons_differ": L10n.tr("title.reason.seasons.differ")
        default: L10n.tr("title.reason.not.listed")
        }
    }
}

struct FeedbackToast: View {
    let feedback: FeedbackInfo?
    @State private var shown: FeedbackInfo?

    var body: some View {
        VStack {
            Spacer()
            if let shown {
                Text(TitleText.feedback(shown))
                    .font(.callout)
                    .foregroundStyle(Theme.text)
                    .padding(.horizontal, Theme.Spacing.large)
                    .padding(.vertical, Theme.Spacing.medium)
                    .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
                    .padding(Theme.Spacing.large)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .accessibilityAddTraits(.isStaticText)
            }
        }
        .allowsHitTesting(false)
        .animation(.easeOut(duration: 0.24), value: shown)
        .task(id: feedback) {
            guard let feedback else { return }
            shown = feedback
            try? await Task.sleep(for: .seconds(feedback.kind == .failed ? 6 : 4))
            if shown == feedback { shown = nil }
        }
    }
}
