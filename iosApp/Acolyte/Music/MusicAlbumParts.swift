import SwiftUI
import UIKit

enum MusicAlbumText {
    static func plural(_ key: String, _ count: Int, _ arguments: CVarArg...) -> String {
        let resolved = count == 1 ? key + ".one" : key + ".other"
        return String(format: NSLocalizedString(resolved, comment: ""), arguments: arguments)
    }

    static func longDate(_ iso: String) -> String {
        let day = String(iso.prefix(10))
        guard day.count == 10, let date = Format.date(iso: day + "T00:00:00Z") else { return "" }
        return date.formatted(Date.FormatStyle(date: .long, time: .omitted, timeZone: TimeZone.gmt))
    }

    static func state(_ key: MusicAlbumStateID) -> String {
        switch key {
        case .notIncluded: L10n.tr("music.album.state.not.included")
        case .noTracks: L10n.tr("music.album.state.no.tracks")
        case .complete: L10n.tr("music.album.state.complete")
        case .notOutYet: L10n.tr("music.album.state.not.out.yet")
        case .notKept: L10n.tr("music.album.state.not.kept")
        case .partial: L10n.tr("music.album.state.partial")
        case .missing: L10n.tr("music.album.state.missing")
        }
    }

    static func stateTone(_ key: MusicAlbumStateID) -> ToneKind {
        switch key {
        case .complete: .ok
        case .partial, .missing, .notOutYet: .wait
        case .notIncluded, .noTracks, .notKept: .idle
        }
    }

    static func duration(_ minutes: Int) -> String {
        if minutes <= 0 { return "" }
        if minutes >= 60 { return L10n.format("music.album.lib.hours", minutes / 60, minutes % 60) }
        return L10n.format("music.album.lib.min", minutes)
    }

    static func pressing(_ pressing: MusicPressingInfo) -> String {
        let name = pressing.disambiguation.isEmpty ? pressing.name : "\(pressing.name) (\(pressing.disambiguation))"
        let tracks = plural("music.album.lib.tracks", pressing.trackCount, pressing.trackCount)
        return [name, pressing.year, pressing.country, pressing.formats, tracks].filter { !$0.isEmpty }.joined(separator: " · ")
    }
}

extension MusicAlbumPageInfo {
    var libraryRoute: Route {
        partId > 0 ? .musicLibraryAlbum(titleId: titleId, albumId: partId) : .musicLibraryArtist(titleId: titleId, tab: "")
    }
}

struct MusicAlbumFlowLayout: Layout {
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

struct MusicAlbumShelf<Content: View>: View {
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

struct MusicAlbumChip: View {
    let text: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.footnote.weight(.medium))
                .foregroundStyle(selected ? Theme.onAccent : Theme.text)
                .lineLimit(1)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(selected ? Theme.accentFill : Theme.surfaceHigh, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

struct MusicAlbumSkeletonRow: View {
    var body: some View {
        HStack(spacing: Theme.Spacing.large) {
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(Theme.surfaceHigh)
                .frame(width: 20, height: 12)
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(Theme.surfaceHigh)
                .frame(height: 14)
            Spacer(minLength: Theme.Spacing.xLarge)
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.medium)
        .accessibilityHidden(true)
    }
}

struct MusicAlbumHero: View {
    let info: MusicAlbumPageInfo
    let store: MusicAlbumStore
    let onPrimary: () -> Void
    @Environment(\.openRoute) private var openRoute

    private var tint: Color {
        Color(musicTint: info.tint) ?? Theme.surfaceHigh
    }

    private var hasArtist: Bool {
        !info.artistMbid.isEmpty && !info.artistName.isEmpty
    }

    var body: some View {
        VStack(spacing: Theme.Spacing.small) {
            MusicArt(path: info.image, name: info.name, size: 1200)
                .frame(width: 260, height: 260)
            titleRow
            if !info.artistName.isEmpty { artistLink }
            Text(facts)
                .font(.subheadline)
                .foregroundStyle(Theme.textMuted)
            if !info.disambiguation.isEmpty {
                Text(info.disambiguation)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            }
            actions
            marks
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.large)
        .frame(maxWidth: .infinity)
        .background(LinearGradient(colors: [tint.opacity(0.6), Theme.background], startPoint: .top, endPoint: .bottom))
    }

    private var facts: String {
        [MusicText.kind(info.kind), info.year].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private var titleRow: some View {
        HStack(spacing: 6) {
            Text(info.name)
                .font(.title2.weight(.bold))
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.center)
            if info.explicit { MusicExplicitBadge() }
        }
    }

    private var artistLink: some View {
        Button {
            openRoute(.musicArtist(mbid: info.artistMbid))
        } label: {
            Text(info.artistName)
                .font(.headline)
                .foregroundStyle(Theme.accentText)
                .lineLimit(2)
                .multilineTextAlignment(.center)
        }
        .buttonStyle(.plain)
        .disabled(info.artistMbid.isEmpty)
    }

    private var actions: some View {
        VStack(spacing: Theme.Spacing.small) {
            primaryButton
            HStack(spacing: Theme.Spacing.small) {
                if hasArtist { goTo }
                if info.followKnown { follow }
            }
        }
    }

    @ViewBuilder
    private var primaryButton: some View {
        if info.cta != .none {
            Button(action: onPrimary) {
                Text(primaryLabel)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accentFill)
            .controlSize(.large)
            .disabled(info.cta == .asked)
        }
    }

    private var primaryLabel: String {
        switch info.cta {
        case .openLibrary: L10n.tr("music.album.open.library")
        case .manageLibrary: L10n.tr("music.album.manage.library")
        case .add: L10n.tr("music.album.add")
        case .ask: L10n.tr("music.album.ask")
        case .asked: L10n.tr("music.album.asked")
        case .openArtist, .none: L10n.tr("music.album.open.artist")
        }
    }

    private var goTo: some View {
        Button {
            openRoute(.musicArtist(mbid: info.artistMbid))
        } label: {
            Text(L10n.format("music.album.go.to", info.artistName))
                .lineLimit(1)
        }
        .buttonStyle(.bordered)
        .tint(Theme.text)
    }

    private var follow: some View {
        Button {
            store.toggleFollow()
        } label: {
            Image(systemName: info.following ? "bell.fill" : "bell")
                .font(.body)
                .foregroundStyle(Theme.text)
                .frame(width: 44, height: 44)
                .background(Theme.surfaceHigh, in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L10n.format(info.following ? "music.album.unfollow.artist" : "music.album.follow.artist", info.artistName))
    }

    @ViewBuilder
    private var marks: some View {
        if info.mark != .none || info.showCounts {
            HStack(spacing: Theme.Spacing.small) {
                MusicMarkChip(mark: info.mark)
                if info.showCounts {
                    Text(L10n.format("music.album.lib.of", info.have, info.have + info.missing))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }
}

struct MusicAlbumTrackRow: View {
    let row: MusicTrackRowInfo
    let info: MusicAlbumPageInfo
    let showDisc: Bool
    let serverURL: String
    let onCopied: @MainActor (String) -> Void
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if showDisc {
                Text(L10n.format("music.album.disc", row.disc))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .padding(.horizontal, Theme.Spacing.screen)
                    .padding(.top, Theme.Spacing.medium)
                    .padding(.bottom, Theme.Spacing.xSmall)
            }
            line
        }
    }

    private var line: some View {
        HStack(spacing: Theme.Spacing.small) {
            Text("\(row.number)")
                .font(.subheadline.monospacedDigit())
                .foregroundStyle(Theme.textMuted)
                .frame(width: 30, alignment: .leading)
            HStack(spacing: 6) {
                Text(row.name)
                    .font(.body)
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                if row.explicit { MusicExplicitBadge() }
            }
            Spacer(minLength: 4)
            haveIcon
            if !row.length.isEmpty {
                Text(row.length)
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(Theme.textMuted)
            }
            menu
        }
        .padding(.leading, Theme.Spacing.screen)
        .padding(.trailing, Theme.Spacing.xSmall)
        .frame(minHeight: 52)
    }

    @ViewBuilder
    private var haveIcon: some View {
        switch row.have {
        case .unknown:
            EmptyView()
        case .inLibrary:
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Theme.ok)
                .accessibilityLabel(L10n.tr("music.album.in.library"))
        case .downloading:
            Image(systemName: "arrow.down.circle.fill")
                .foregroundStyle(Theme.info)
                .accessibilityLabel(L10n.tr("music.album.downloading"))
        case .missing:
            Image(systemName: "minus")
                .foregroundStyle(Theme.textMuted)
                .accessibilityLabel(L10n.tr("music.album.missing"))
        }
    }

    private var menu: some View {
        Menu {
            Button(L10n.tr("music.album.copy.song")) { copySong() }
            if !serverURL.isEmpty {
                Button(L10n.tr("music.album.copy.link")) { copyLink() }
            }
            if info.titleId > 0 {
                Button(L10n.tr("music.album.open.library")) { openRoute(info.libraryRoute) }
            }
        } label: {
            Image(systemName: "ellipsis")
                .foregroundStyle(Theme.textMuted)
                .frame(width: 36, height: 36)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(L10n.format("music.album.more.for", row.name))
    }

    private func copySong() {
        UIPasteboard.general.string = row.name
        onCopied(L10n.tr("music.album.song.copied"))
    }

    private func copyLink() {
        UIPasteboard.general.string = "\(serverURL)/music/album/\(info.mbid)"
        onCopied(L10n.tr("music.album.link.copied"))
    }
}

struct MusicAlbumFooter: View {
    let info: MusicAlbumPageInfo
    let store: MusicAlbumStore
    @State private var open = false

    private var released: String {
        MusicAlbumText.longDate(info.date)
    }

    private var summary: String {
        guard info.songs > 0 else { return "" }
        let songs = MusicAlbumText.plural("music.album.songs", info.songs, info.songs)
        let minutes = info.minutes > 0 ? MusicAlbumText.plural("music.album.minutes", info.minutes, info.minutes) : ""
        return [songs, minutes].filter { !$0.isEmpty }.joined(separator: ", ")
    }

    private var editionsTitle: String {
        let count = MusicAlbumText.plural("music.album.editions", info.editions.count, info.editions.count)
        let picked = info.editions.first { $0.mbid == info.edition }?.label
        return [count, picked].compactMap { $0 }.joined(separator: " · ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            if !released.isEmpty { hint(L10n.format("music.album.released", released)) }
            if !summary.isEmpty { hint(summary) }
            if !info.label.isEmpty {
                hint(L10n.format("music.album.label", info.year, info.label).trimmingCharacters(in: .whitespaces))
            }
            if info.editions.count > 1 { editions }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.large)
    }

    private func hint(_ text: String) -> some View {
        Text(text).font(.footnote).foregroundStyle(Theme.textMuted)
    }

    @ViewBuilder
    private var editions: some View {
        Button {
            open.toggle()
        } label: {
            HStack(spacing: 4) {
                Text(editionsTitle).font(.subheadline.weight(.medium)).lineLimit(1)
                Image(systemName: open ? "chevron.up" : "chevron.down").font(.caption.weight(.semibold))
            }
            .foregroundStyle(Theme.accentText)
        }
        .buttonStyle(.plain)
        .padding(.top, Theme.Spacing.small)
        if open { editionChips }
        if !info.edition.isEmpty && info.canAsk && !info.canAdd {
            hint(L10n.tr("music.album.edition.asking"))
        }
    }

    private var editionChips: some View {
        MusicAlbumFlowLayout {
            MusicAlbumChip(text: L10n.tr("music.album.edition.usual"), selected: info.edition.isEmpty) {
                if !info.edition.isEmpty { store.pickEdition(info.edition) }
            }
            ForEach(info.editions) { edition in
                MusicAlbumChip(text: chipText(edition), selected: edition.mbid == info.edition) {
                    store.pickEdition(edition.mbid)
                }
            }
        }
    }

    private func chipText(_ edition: MusicEditionInfo) -> String {
        MusicAlbumText.plural("music.album.edition.tracks", edition.tracks, edition.label, edition.tracks)
    }
}

struct MusicAlbumRails: View {
    let info: MusicAlbumPageInfo
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.large) {
            if !info.moreBy.isEmpty { moreBy }
            if !info.near.isEmpty { near }
            if !info.similar.isEmpty { similar }
        }
        .padding(.bottom, Theme.Spacing.xLarge)
    }

    private func subtitle(_ album: MusicAlbumInfo) -> String {
        [album.year, MusicText.kind(album.kind)].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private var moreBy: some View {
        MusicAlbumShelf(title: L10n.format("music.album.more.by", info.artistName)) {
            ForEach(info.moreBy) { album in
                MusicAlbumCard(album: album, subtitle: subtitle(album)) {
                    openRoute(.musicAlbum(mbid: album.mbid, artist: album.artistMbid.isEmpty ? info.artistMbid : album.artistMbid))
                }
            }
        }
    }

    private var near: some View {
        MusicAlbumShelf(title: L10n.tr("music.album.fans")) {
            ForEach(info.near) { album in
                MusicAlbumCard(album: album, subtitle: album.artistName.isEmpty ? album.year : album.artistName) {
                    openRoute(.musicAlbum(mbid: album.mbid, artist: album.artistMbid))
                }
            }
        }
    }

    private var similar: some View {
        MusicAlbumShelf(title: L10n.format("music.album.if.you.like", info.artistName)) {
            ForEach(info.similar) { artist in
                MusicArtistCircle(artist: artist, size: 152) {
                    openRoute(.musicArtist(mbid: artist.mbid))
                }
            }
        }
    }
}
