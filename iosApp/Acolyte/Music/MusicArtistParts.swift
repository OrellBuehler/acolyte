import SwiftUI
import UIKit

enum MusicArtistText {
    static func plural(_ key: String, _ count: Int, _ arguments: CVarArg...) -> String {
        let resolved = count == 1 ? key + ".one" : key + ".other"
        return String(format: NSLocalizedString(resolved, comment: ""), arguments: arguments)
    }

    static func image(_ path: String, size: Int) -> String? {
        path.isEmpty ? nil : MusicFormat.image(path, size: size)
    }

    static func listeners(_ info: MusicArtistInfo) -> String {
        if info.listeners > 0 {
            return plural("music.artist.listeners", Int(clamping: info.listeners), MusicFormat.shortCount(info.listeners))
        }
        if info.plays > 0 {
            return L10n.format("music.artist.plays", MusicFormat.shortCount(info.plays))
        }
        return ""
    }

    static func shelfTitle(_ kind: MusicShelfKindID) -> String {
        switch kind {
        case .albums: L10n.tr("music.artist.shelf.albums")
        case .singles: L10n.tr("music.artist.shelf.singles")
        case .compilations: L10n.tr("music.artist.shelf.compilations")
        case .live: L10n.tr("music.artist.shelf.live")
        case .other: L10n.tr("music.artist.shelf.other")
        }
    }

    static func kind(_ kind: MusicArtistKindID) -> String {
        switch kind {
        case .solo: L10n.tr("music.artist.kind.solo")
        case .group: L10n.tr("music.artist.kind.group")
        case .artist: L10n.tr("music.artist.kind.artist")
        }
    }

    static func country(_ code: String) -> String {
        Locale.current.localizedString(forRegionCode: code) ?? code
    }

    static func activeYears(_ info: MusicArtistInfo) -> String {
        guard !info.activeSince.isEmpty else { return "" }
        let until = info.activeUntil.isEmpty ? L10n.tr("music.artist.about.now") : info.activeUntil
        return L10n.format("music.artist.about.active", info.activeSince, until)
    }

    static func sentences(_ text: String, limit: Int) -> String {
        var result: [String] = []
        text.enumerateSubstrings(in: text.startIndex..<text.endIndex, options: .bySentences) { sentence, _, _, stop in
            guard let sentence else { return }
            result.append(sentence.trimmingCharacters(in: .whitespacesAndNewlines))
            if result.count >= limit { stop = true }
        }
        return result.joined(separator: " ")
    }

    static func change(_ change: Int64) -> String {
        let amount = MusicFormat.shortCount(change < 0 ? -change : change)
        return L10n.format(change > 0 ? "music.artist.stats.up" : "music.artist.stats.down", amount)
    }
}

struct MusicArtistFlowLayout: Layout {
    var spacing: CGFloat = Theme.Spacing.small

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        let result = arrange(width: width, subviews: subviews)
        let finite = width.isFinite ? width : result.size.width
        return CGSize(width: finite, height: result.size.height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(width: bounds.width, subviews: subviews)
        for (index, origin) in result.origins.enumerated() where index < subviews.count {
            let point = CGPoint(x: bounds.minX + origin.x, y: bounds.minY + origin.y)
            subviews[index].place(at: point, anchor: .topLeading, proposal: .unspecified)
        }
    }

    private func arrange(width: CGFloat, subviews: Subviews) -> (origins: [CGPoint], size: CGSize) {
        var origins: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var widest: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            origins.append(CGPoint(x: x, y: y))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            widest = max(widest, x - spacing)
        }
        return (origins, CGSize(width: widest, height: y + rowHeight))
    }
}

struct MusicArtistChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.text)
            .lineLimit(1)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Theme.surfaceHigh, in: Capsule())
    }
}

struct MusicArtistNote: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.callout)
            .foregroundStyle(Theme.text)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.medium)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
            .padding(.horizontal, Theme.Spacing.screen)
    }
}

struct MusicArtistShelf<Content: View>: View {
    let title: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            SectionHeader(title: title)
                .padding(.horizontal, Theme.Spacing.screen)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: Theme.Spacing.medium) {
                    content()
                }
                .padding(.horizontal, Theme.Spacing.screen)
            }
        }
    }
}

struct MusicArtistHero: View {
    let info: MusicArtistInfo

    private var tint: Color {
        Color(musicTint: info.tint) ?? Theme.surfaceHigh
    }

    var body: some View {
        Rectangle()
            .fill(tint)
            .frame(maxWidth: .infinity)
            .frame(height: 200)
            .overlay { layers }
            .clipped()
            .accessibilityHidden(true)
    }

    private var layers: some View {
        ZStack {
            AuthImage(source: MusicArtistText.image(info.image, size: 250), size: .poster) { Color.clear }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .blur(radius: 24)
            AuthImage(source: MusicArtistText.image(info.wide, size: 1000), size: .backdrop) { Color.clear }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            LinearGradient(colors: [Color.clear, Theme.background], startPoint: .top, endPoint: .bottom)
        }
    }
}

struct MusicArtistSongRow: View {
    let number: Int
    let song: MusicSongInfo
    let artistMbid: String
    @Environment(\.openRoute) private var openRoute

    private var subtitle: String {
        [song.album, song.year].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private var hasAlbum: Bool { !song.albumMbid.isEmpty }

    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            Text("\(number)")
                .font(.subheadline.monospacedDigit())
                .foregroundStyle(Theme.textMuted)
                .frame(width: 24, alignment: .trailing)
            MusicArt(path: song.image, name: song.title, size: 250)
                .frame(width: 48, height: 48)
            titles
            Spacer(minLength: 4)
            if !song.length.isEmpty {
                Text(song.length)
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(Theme.textMuted)
            }
            if hasAlbum { menu }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .contentShape(Rectangle())
        .onTapGesture { openAlbum() }
    }

    private var titles: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 4) {
                Text(song.title)
                    .font(.body)
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if song.explicit { MusicExplicitBadge() }
            }
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        }
    }

    private var menu: some View {
        Menu {
            Button(L10n.tr("music.artist.open.album")) { openAlbum() }
            Button(L10n.tr("music.artist.copy.song")) { UIPasteboard.general.string = song.title }
        } label: {
            Image(systemName: "ellipsis")
                .foregroundStyle(Theme.textMuted)
                .frame(width: 36, height: 36)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(L10n.format("music.artist.more.for", song.title))
    }

    private func openAlbum() {
        guard hasAlbum else { return }
        openRoute(.musicAlbum(mbid: song.albumMbid, artist: artistMbid))
    }
}

struct MusicArtistAlbumShelf: View {
    let title: String
    let albums: [MusicAlbumInfo]
    let info: MusicArtistInfo
    let withKind: Bool
    let openSheet: (Route) -> Void
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        MusicArtistShelf(title: title) {
            ForEach(albums) { album in
                card(album)
            }
        }
    }

    private func subtitle(_ album: MusicAlbumInfo) -> String? {
        guard withKind else { return nil }
        return [album.year, MusicText.kind(album.kind)].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private func card(_ album: MusicAlbumInfo) -> some View {
        MusicAlbumCard(album: album, subtitle: subtitle(album)) {
            openRoute(.musicAlbum(mbid: album.mbid, artist: info.mbid))
        }
        .overlay(alignment: .topLeading) { plus(album) }
    }

    @ViewBuilder
    private func plus(_ album: MusicAlbumInfo) -> some View {
        if let kind = plusKind(album) {
            MusicAddButton(action: kind, name: kind == .add ? info.name : album.name) {
                if kind == .add {
                    openSheet(.musicAdd(mbid: info.mbid, name: info.name))
                } else {
                    openSheet(.askMusic(artist: info.mbid, name: info.name, album: album.mbid, edition: ""))
                }
            }
            .padding(6)
        }
    }

    private func plusKind(_ album: MusicAlbumInfo) -> MusicCardActionKind? {
        guard info.titleId <= 0, info.mainReady, !album.asked, album.mark == .none else { return nil }
        switch info.main {
        case .add: return .add
        case .ask: return .ask
        default: return nil
        }
    }
}

struct MusicArtistStoreCard: View {
    let album: MusicStoreItemInfo

    private var subtitle: String {
        var parts: [String] = []
        if !album.year.isEmpty { parts.append(album.year) }
        if album.kind != .album { parts.append(MusicText.kind(album.kind)) }
        if album.tracks > 0 { parts.append(L10n.plural("music.artist.tracks", album.tracks)) }
        return parts.joined(separator: " · ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MusicArt(path: album.image, name: album.title, size: 250)
                .frame(width: 148, height: 148)
            HStack(spacing: 4) {
                Text(album.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if album.explicit { MusicExplicitBadge() }
            }
            .padding(.top, 6)
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        }
        .frame(width: 148, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

struct MusicArtistComingRow: View {
    let album: MusicAlbumInfo
    let artistMbid: String
    let onFollow: () -> Void
    @Environment(\.openRoute) private var openRoute

    private var subtitle: String {
        let out = album.date.isEmpty ? "" : L10n.format("music.line.out.on", MusicText.shortDay(album.date))
        return [MusicText.kind(album.kind), out].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            MusicArt(path: album.image, name: album.name, size: 250)
                .frame(width: 72, height: 72)
            VStack(alignment: .leading, spacing: 2) {
                Text(album.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
            .contentShape(Rectangle())
            .onTapGesture { openRoute(.musicAlbum(mbid: album.mbid, artist: artistMbid)) }
            Spacer(minLength: 4)
            MusicBellButton(following: album.following, name: album.name, action: onFollow)
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

struct MusicArtistVideoCard: View {
    let video: MusicVideoInfo
    @Environment(\.openURL) private var openURL

    var body: some View {
        Button(action: play) {
            VStack(alignment: .leading, spacing: 6) {
                thumb
                Text(video.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Text(L10n.tr("music.artist.video.caption"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            .frame(width: 260, alignment: .leading)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L10n.format("music.artist.video.play", video.title))
    }

    private var thumb: some View {
        AuthImage(source: video.thumb.isEmpty ? nil : video.thumb, size: .backdrop) {
            ArtPlaceholderView(name: video.title)
        }
        .frame(width: 260, height: 146)
        .overlay(alignment: .bottomTrailing) { length }
        .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }

    @ViewBuilder
    private var length: some View {
        if !video.length.isEmpty {
            Text(video.length)
                .font(.caption2.monospacedDigit())
                .foregroundStyle(Theme.onArt)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(Theme.artScrim, in: RoundedRectangle(cornerRadius: 4, style: .continuous))
                .padding(6)
        }
    }

    private func play() {
        guard let url = URL(string: video.url) else { return }
        openURL(url)
    }
}

struct MusicArtistStatsBand: View {
    let info: MusicArtistInfo
    let onAskAll: () -> Void
    @State private var confirming = false

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.listeners > 0 || info.rank > 0 { numbers }
            if let all = info.askAll { askAll(all) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Theme.Spacing.screen)
    }

    private var numbers: some View {
        MusicArtistFlowLayout(spacing: Theme.Spacing.large) {
            if info.listeners > 0 {
                Text(MusicArtistText.listeners(info))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
            }
            if info.rank > 0 {
                Text(L10n.format("music.artist.stats.rank", info.rank))
                    .font(.subheadline)
                    .foregroundStyle(Theme.text)
            }
            if info.change != 0 {
                Text(MusicArtistText.change(info.change))
                    .font(.subheadline)
                    .foregroundStyle(info.change > 0 ? Theme.okText : Theme.failText)
            }
        }
    }

    @ViewBuilder
    private func askAll(_ all: MusicAskAllInfo) -> some View {
        if confirming {
            Text(MusicArtistText.plural(all.gb ? "music.artist.ask.all.gb" : "music.artist.ask.all.mb", all.count, all.count, all.size))
                .font(.subheadline)
                .foregroundStyle(Theme.text)
            HStack(spacing: Theme.Spacing.small) {
                Button(L10n.tr("music.artist.ask.confirm")) {
                    confirming = false
                    onAskAll()
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .disabled(info.asking)
                Button(L10n.tr("common.cancel")) { confirming = false }
                    .buttonStyle(.bordered)
            }
        } else {
            Button(L10n.format("music.artist.ask.all", all.count)) { confirming = true }
                .buttonStyle(.bordered)
                .disabled(info.asking)
        }
    }
}

struct MusicArtistPlaceholderCircle: View {
    var body: some View {
        Circle()
            .fill(Theme.surfaceHigh)
            .frame(width: 120, height: 120)
            .accessibilityHidden(true)
    }
}

private struct MusicReloadModifier: ViewModifier {
    let ticket: Int
    let reload: @MainActor @Sendable () async -> Void

    func body(content: Content) -> some View {
        content.task(id: ticket) {
            guard ticket > 0 else { return }
            for _ in 0..<6 {
                try? await Task.sleep(for: .seconds(5))
                if Task.isCancelled { return }
                await reload()
            }
        }
    }
}

extension View {
    func musicReload(after ticket: Int, _ reload: @escaping @MainActor @Sendable () async -> Void) -> some View {
        modifier(MusicReloadModifier(ticket: ticket, reload: reload))
    }
}
