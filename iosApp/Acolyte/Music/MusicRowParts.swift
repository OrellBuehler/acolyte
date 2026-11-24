import SwiftUI

@MainActor
struct MusicOpener {
    let openRoute: OpenRouteAction
    let navigator: Navigator

    func open(_ target: MusicTargetInfo) {
        if target.kind == .search {
            navigator.open(.search)
        } else if let route = Route.music(target) {
            openRoute(route)
        }
    }

    func open(_ item: MusicItemInfo) {
        open(item.target)
    }

    func artist(_ mbid: String) {
        if !mbid.isEmpty { openRoute(.musicArtist(mbid: mbid)) }
    }

    func act(_ item: MusicItemInfo) {
        let name = item.artist.isEmpty ? item.name : item.artist
        switch item.action {
        case .add:
            openRoute(.musicAdd(mbid: item.artistMbid, name: name))
        case .ask:
            let album = item.kind == .album ? item.mbid : ""
            openRoute(.askMusic(artist: item.artistMbid, name: name, album: album, edition: ""))
        case .none:
            break
        }
    }

    func act(_ hit: MusicArtistHitInfo) {
        switch hit.action {
        case .add:
            openRoute(.musicAdd(mbid: hit.mbid, name: hit.name))
        case .ask:
            openRoute(.askMusic(artist: hit.mbid, name: hit.name, album: "", edition: ""))
        case .none:
            break
        }
    }

    func browseAll() {
        navigator.open(.search)
    }
}

struct MusicOpenerReader<Content: View>: View {
    @ViewBuilder var content: (MusicOpener) -> Content
    @Environment(\.openRoute) private var openRoute
    @Environment(Navigator.self) private var navigator

    var body: some View {
        content(MusicOpener(openRoute: openRoute, navigator: navigator))
    }
}

func musicImageSource(_ path: String, size: Int) -> String? {
    path.isEmpty ? nil : MusicFormat.image(path, size: size)
}

struct MusicTileCell: Identifiable {
    let id: String
    let name: String
    let color: String
    let cover: String?
    let target: MusicTargetInfo

    init(tile: MusicTileInfo) {
        id = tile.key
        name = tile.name
        color = tile.color.isEmpty ? MusicFormat.tileColor(tile.key) : tile.color
        cover = tile.covers.first
        target = tile.target
    }

    init(item: MusicItemInfo) {
        id = item.key
        name = item.name
        color = item.color.isEmpty ? MusicFormat.tileColor(item.key) : item.color
        cover = item.images.first ?? (item.image.isEmpty ? nil : item.image)
        target = item.target
    }
}

struct MusicTileGrid: View {
    let cells: [MusicTileCell]
    let opener: MusicOpener

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.flexible(), spacing: Theme.Spacing.medium), GridItem(.flexible(), spacing: Theme.Spacing.medium)],
            spacing: Theme.Spacing.medium
        ) {
            ForEach(cells) { cell in
                MusicTileView(name: cell.name, color: cell.color, cover: cell.cover) { opener.open(cell.target) }
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

enum MusicRowsText {
    static func region(_ code: String) -> String {
        if code.isEmpty || code == "global" { return L10n.tr("music.rows.region.global") }
        return Locale.current.localizedString(forRegionCode: code) ?? code
    }

    static func kind(_ tileKind: String) -> String {
        switch tileKind {
        case "mood": L10n.tr("music.rows.kind.mood")
        case "season": L10n.tr("music.rows.kind.season")
        case "decade": L10n.tr("music.rows.kind.decade")
        case "activity": L10n.tr("music.rows.kind.activity")
        default: L10n.tr("music.rows.kind.genre")
        }
    }

    static func color(hue: Double, saturation: Double, lightness: Double) -> Color {
        let value = lightness + saturation * min(lightness, 1 - lightness)
        let sat = value == 0 ? 0 : 2 * (1 - lightness / value)
        return Color(hue: hue.truncatingRemainder(dividingBy: 360) / 360, saturation: sat, brightness: value)
    }
}

struct MusicRowsBand: View {
    let info: MusicRowsInfo

    var body: some View {
        texts
            .frame(maxWidth: .infinity, minHeight: 150, alignment: .leading)
            .background { gradient }
            .overlay(alignment: .trailing) { collage }
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.panel, style: .continuous))
            .padding(.horizontal, Theme.Spacing.screen)
    }

    private var gradient: some View {
        let hue = Double(info.bandHue)
        return LinearGradient(
            colors: [
                MusicRowsText.color(hue: hue, saturation: 0.52, lightness: 0.36),
                MusicRowsText.color(hue: hue + 40, saturation: 0.60, lightness: 0.18),
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private var texts: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(MusicRowsText.kind(info.tileKind))
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.onArt.opacity(0.8))
            Text(info.title)
                .font(.title.bold())
                .foregroundStyle(Theme.onArt)
                .lineLimit(2)
            Text(L10n.plural("music.rows.count", info.rows.count))
                .font(.footnote)
                .foregroundStyle(Theme.onArt.opacity(0.8))
            if !info.leads.isEmpty {
                Text(L10n.format("music.rows.lead", info.leads.joined(separator: ", ")))
                    .font(.footnote)
                    .foregroundStyle(Theme.onArt.opacity(0.8))
                    .lineLimit(2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.leading, Theme.Spacing.large)
        .padding(.vertical, Theme.Spacing.large)
        .padding(.trailing, 120)
    }

    private var collage: some View {
        ZStack(alignment: .topLeading) {
            ForEach(Array(info.collage.prefix(5).enumerated()), id: \.offset) { index, path in
                MusicArt(path: path, name: "", size: 250)
                    .frame(width: 60, height: 60)
                    .rotationEffect(.degrees(Double(index - 2) * 6))
                    .offset(x: CGFloat(index) * 12, y: index % 2 == 0 ? 0 : 28)
            }
        }
        .frame(width: 110, height: 100, alignment: .topLeading)
        .padding(.trailing, Theme.Spacing.large)
        .accessibilityHidden(true)
    }
}

struct MusicSpotlightCard: View {
    let item: MusicItemInfo
    let opener: MusicOpener

    private var kicker: String {
        if !item.kicker.isEmpty { return item.kicker }
        if !item.badge.isEmpty { return item.badge }
        return L10n.tr("music.rows.popular.now")
    }

    private var path: String {
        item.cover.isEmpty ? item.image : item.cover
    }

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            AuthImage(source: musicImageSource(path, size: 500), size: .hero) {
                ArtPlaceholderView(name: item.name)
            }
            LinearGradient(colors: [.clear, Theme.artScrimStrong], startPoint: UnitPoint(x: 0.5, y: 0.4), endPoint: .bottom)
            texts
        }
        .frame(width: 280, height: 210)
        .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .overlay(alignment: .topTrailing) { MusicCornerMark(mark: item.mark).padding(6) }
        .overlay(alignment: .bottomTrailing) { act }
        .contentShape(Rectangle())
        .onTapGesture { opener.open(item) }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }

    private var texts: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(kicker)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.onArt.opacity(0.85))
                .lineLimit(1)
            HStack(spacing: 4) {
                Text(item.name)
                    .font(.headline)
                    .foregroundStyle(Theme.onArt)
                    .lineLimit(2)
                if item.explicit { MusicExplicitBadge() }
            }
            if !item.artist.isEmpty, item.artist != item.name {
                Text(item.artist)
                    .font(.footnote)
                    .foregroundStyle(Theme.onArt.opacity(0.85))
                    .lineLimit(1)
            }
        }
        .padding(Theme.Spacing.medium)
        .padding(.trailing, 36)
    }

    @ViewBuilder
    private var act: some View {
        if item.action != .none {
            MusicAddButton(action: item.action, name: item.artist.isEmpty ? item.name : item.artist) { opener.act(item) }
                .padding(6)
        }
    }
}

struct MusicStripTile: View {
    let item: MusicItemInfo
    let opener: MusicOpener

    var body: some View {
        let cell = MusicTileCell(item: item)
        MusicTileView(name: cell.name, color: cell.color, cover: cell.cover) { opener.open(item) }
            .frame(width: 184)
    }
}

struct MusicWideCard: View {
    let item: MusicItemInfo
    let opener: MusicOpener

    private var path: String {
        item.image.isEmpty ? item.cover : item.image
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            AuthImage(source: musicImageSource(path, size: 500), size: .backdrop) {
                ArtPlaceholderView(name: item.name)
            }
            .frame(width: 240, height: 135)
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
            Text(item.name)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(1)
                .padding(.top, 6)
            let line = MusicText.line(item.line)
            if !line.isEmpty {
                Text(line)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        }
        .frame(width: 240, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture { opener.open(item) }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

struct MusicSongLine: View {
    let item: MusicItemInfo
    let compact: Bool
    let opener: MusicOpener

    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            MusicArt(path: item.image, name: item.name, size: 250)
                .frame(width: 44, height: 44)
            texts
            Spacer(minLength: Theme.Spacing.small)
            trailing
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, 6)
        .contentShape(Rectangle())
        .onTapGesture { opener.open(item) }
    }

    private var texts: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 4) {
                Text(item.track.isEmpty ? item.name : item.track)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if item.explicit { MusicExplicitBadge() }
            }
            let line = MusicText.line(item.line)
            Text(line.isEmpty ? item.artist : line)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .lineLimit(1)
        }
    }

    @ViewBuilder
    private var trailing: some View {
        if compact {
            if item.action != .none {
                MusicAddButton(action: item.action, name: item.artist.isEmpty ? item.name : item.artist) { opener.act(item) }
            }
        } else {
            Button(L10n.tr("music.rows.ask.album")) { opener.open(item) }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.accentText)
                .lineLimit(1)
        }
    }
}

struct MusicSongList: View {
    let row: MusicShelfRowInfo
    var compact = false
    var seeAll: (() -> Void)?
    let opener: MusicOpener

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: row.title, action: seeAll)
                .padding(.horizontal, Theme.Spacing.screen)
            if !row.subtitle.isEmpty {
                Text(row.subtitle)
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                    .padding(.horizontal, Theme.Spacing.screen)
            }
            ForEach(row.items) { item in
                MusicSongLine(item: item, compact: compact, opener: opener)
            }
        }
        .padding(.vertical, Theme.Spacing.small)
    }
}

struct MusicRegionMenu: View {
    let region: String
    let regions: [String]
    let select: (String) -> Void

    var body: some View {
        Menu {
            ForEach(regions, id: \.self) { code in
                Button(MusicRowsText.region(code)) { select(code) }
            }
        } label: {
            Label(MusicRowsText.region(region), systemImage: "chevron.down")
                .labelStyle(.titleAndIcon)
                .font(.subheadline.weight(.semibold))
        }
        .accessibilityLabel(L10n.tr("music.rows.region"))
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

struct MusicShelfRowView<Header: View, Card: View>: View {
    let row: MusicShelfRowInfo
    var seeAll: (() -> Void)?
    let opener: MusicOpener
    @ViewBuilder var header: () -> Header
    @ViewBuilder var card: (MusicItemInfo) -> Card

    var body: some View {
        switch row.shape {
        case .song:
            MusicSongList(row: row, seeAll: seeAll, opener: opener)
        case .wide:
            CardShelf(title: row.title, subtitle: row.subtitle, seeAll: seeAll, header: header) {
                ForEach(row.items) { MusicWideCard(item: $0, opener: opener) }
            }
        default:
            CardShelf(title: row.title, subtitle: row.subtitle, seeAll: seeAll, header: header) {
                ForEach(row.items) { card($0) }
            }
        }
    }
}
