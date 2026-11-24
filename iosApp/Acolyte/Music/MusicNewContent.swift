import SwiftUI

enum MusicNewText {
    static let tracksRow = "np_music_tracks"

    static func eyebrow(_ item: MusicItemInfo) -> String {
        if item.upcoming, let days = daysUntil(item.date), (0...6).contains(days), let day = weekday(item.date) {
            return L10n.format("music.new.out.day", day)
        }
        if !item.badge.isEmpty { return item.badge }
        if !item.kicker.isEmpty, !item.kicker.hasPrefix("New") { return item.kicker }
        return L10n.tr("music.rows.popular.now")
    }

    private static func localDay(_ iso: String) -> Date? {
        let parts = iso.prefix(10).split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return Calendar.current.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    private static func daysUntil(_ iso: String) -> Int? {
        guard let day = localDay(iso) else { return nil }
        let today = Calendar.current.startOfDay(for: Date())
        return Calendar.current.dateComponents([.day], from: today, to: day).day
    }

    private static func weekday(_ iso: String) -> String? {
        localDay(iso)?.formatted(.dateTime.weekday(.wide))
    }
}

struct MusicNewContent<Header: View>: View {
    @State private var store = MusicNewStore(region: "global")
    @State private var seeAll: String?
    @ViewBuilder var header: () -> Header

    var body: some View {
        MusicOpenerReader { opener in
            VStack(spacing: 0) {
                header()
                if let id = seeAll, let row = store.info.rows.first(where: { $0.id == id }) {
                    MusicNewSeeAll(row: row, opener: opener, store: store, back: { seeAll = nil })
                } else {
                    MusicNewBody(info: store.info, store: store, opener: opener, seeAll: { seeAll = $0 })
                }
            }
        }
        .musicNote(store.info.note)
        .task { await store.watch() }
        .task { await store.load() }
    }
}

struct MusicNewBody: View {
    let info: MusicNewInfo
    let store: MusicNewStore
    let opener: MusicOpener
    let seeAll: (String) -> Void

    private var bare: Bool {
        info.hero.isEmpty && info.rows.isEmpty
    }

    var body: some View {
        if info.load.phase == .failed {
            failure
        } else {
            StatusContent(
                load: info.load,
                isEmpty: info.kid || info.isEmpty || (info.building && bare),
                reload: { await store.refresh() }
            ) {
                MusicNewEmpty(info: info)
            } content: {
                list
            }
        }
    }

    private var failure: some View {
        ContentUnavailableView {
            Label(L10n.tr("music.new.error.title"), systemImage: "music.note")
        } description: {
            Text(L10n.tr("music.new.error.message"))
        } actions: {
            Button(L10n.tr("common.retry")) { Task { await store.refresh() } }
                .buttonStyle(.borderedProminent)
        }
    }

    private var list: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Theme.Spacing.small) {
                if let error = info.load.error {
                    NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
                }
                if info.building {
                    NoticeBanner(text: L10n.tr("music.new.building.message")).padding(.horizontal, Theme.Spacing.screen)
                }
                if !info.hero.isEmpty {
                    MusicNewHero(items: info.hero, opener: opener, follow: { store.toggleFollow($0) })
                }
                ForEach(Array(info.rows.enumerated()), id: \.offset) { _, row in
                    rowView(row)
                }
                explore
            }
            .padding(.bottom, Theme.Spacing.xLarge)
        }
        .refreshable { await store.refresh() }
    }

    @ViewBuilder
    private func rowView(_ row: MusicShelfRowInfo) -> some View {
        if !row.items.isEmpty {
            if row.id == MusicNewText.tracksRow {
                MusicSongList(
                    row: MusicShelfRowInfo(
                        id: row.id,
                        type: row.type,
                        title: row.title,
                        subtitle: row.subtitle,
                        shape: row.shape,
                        items: Array(row.items.prefix(5)),
                        regional: row.regional
                    ),
                    compact: true,
                    seeAll: { seeAll(row.id) },
                    opener: opener
                )
            } else {
                MusicShelfRowView(
                    row: row,
                    seeAll: { seeAll(row.id) },
                    opener: opener,
                    header: { EmptyView() },
                    card: { item in
                        MusicNewCard(item: item, opener: opener, follow: { store.toggleFollow($0) })
                    }
                )
            }
        }
    }

    @ViewBuilder
    private var explore: some View {
        if !info.tiles.isEmpty {
            SectionHeader(
                title: L10n.tr("music.new.explore.more"),
                actionTitle: L10n.tr("music.rows.browse.all"),
                action: { opener.browseAll() }
            )
            .padding(.horizontal, Theme.Spacing.screen)
            MusicTileGrid(cells: info.tiles.prefix(4).map { MusicTileCell(tile: $0) }, opener: opener)
        }
    }
}

struct MusicNewEmpty: View {
    let info: MusicNewInfo

    var body: some View {
        if info.kid {
            EmptyStateView(
                title: L10n.tr("music.rows.kid.title"),
                message: L10n.tr("music.rows.kid.message"),
                systemImage: "music.note"
            )
        } else if info.building {
            EmptyStateView(
                title: L10n.tr("music.new.building.title"),
                message: L10n.tr("music.new.building.message"),
                systemImage: "music.note"
            )
        } else {
            EmptyStateView(
                title: L10n.tr("music.new.empty.title"),
                message: L10n.tr("music.new.empty.message"),
                systemImage: "music.note"
            )
        }
    }
}

struct MusicNewCard: View {
    let item: MusicItemInfo
    let opener: MusicOpener
    let follow: (String) -> Void
    var width: CGFloat = 148

    var body: some View {
        MusicItemCard(item: item, width: width, onOpen: { opener.open(item) }, onAct: { opener.act(item) })
            .overlay(alignment: .topLeading) { bell }
    }

    @ViewBuilder
    private var bell: some View {
        if item.upcoming, item.kind == .album {
            MusicBellButton(following: item.following, name: item.name) { follow(item.key) }
                .padding(6)
        }
    }
}

struct MusicNewHero: View {
    let items: [MusicItemInfo]
    let opener: MusicOpener
    let follow: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(alignment: .top, spacing: Theme.Spacing.medium) {
                ForEach(items) { item in
                    MusicNewHeroCard(item: item, opener: opener, follow: follow)
                }
            }
            .scrollTargetLayout()
            .padding(.horizontal, Theme.Spacing.screen)
        }
        .scrollTargetBehavior(.viewAligned)
        .padding(.vertical, Theme.Spacing.small)
    }
}

struct MusicNewHeroCard: View {
    let item: MusicItemInfo
    let opener: MusicOpener
    let follow: (String) -> Void

    private var path: String {
        item.cover.isEmpty ? item.image : item.cover
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            art
            Text(MusicNewText.eyebrow(item))
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.accentText)
                .lineLimit(1)
                .padding(.top, 6)
            HStack(spacing: 4) {
                Text(item.name)
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if item.explicit { MusicExplicitBadge() }
            }
            let line = MusicText.line(item.line)
            if !line.isEmpty {
                Text(line)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(2)
            }
        }
        .frame(width: 300, alignment: .leading)
    }

    private var art: some View {
        AuthImage(source: musicImageSource(path, size: 500), size: .hero) {
            ArtPlaceholderView(name: item.name)
        }
        .frame(width: 300, height: 188)
        .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .overlay(alignment: .topTrailing) { MusicCornerMark(mark: item.mark).padding(6) }
        .overlay(alignment: .bottomTrailing) { corner }
        .contentShape(Rectangle())
        .onTapGesture { opener.open(item) }
    }

    @ViewBuilder
    private var corner: some View {
        if item.upcoming, item.kind == .album {
            MusicBellButton(following: item.following, name: item.name) { follow(item.key) }
                .padding(6)
        } else if item.action != .none {
            MusicAddButton(action: item.action, name: item.artist.isEmpty ? item.name : item.artist) { opener.act(item) }
                .padding(6)
        }
    }
}

struct MusicNewSeeAll: View {
    let row: MusicShelfRowInfo
    let opener: MusicOpener
    let store: MusicNewStore
    let back: () -> Void
    @State private var width: CGFloat = 0

    private var cardWidth: CGFloat {
        let available = width - Theme.Spacing.screen * 2 - Theme.Spacing.medium
        return available > 0 ? (available / 2).rounded(.down) : 148
    }

    private var asSongs: Bool {
        row.id == MusicNewText.tracksRow || row.shape == .song
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Button(action: back) {
                    Label(L10n.tr("music.new.title"), systemImage: "chevron.left")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.accentText)
                }
                .padding(.horizontal, Theme.Spacing.screen)
                Text(row.title)
                    .font(.title2.bold())
                    .foregroundStyle(Theme.text)
                    .padding(.horizontal, Theme.Spacing.screen)
                    .accessibilityAddTraits(.isHeader)
                if asSongs {
                    ForEach(row.items) { MusicSongLine(item: $0, compact: true, opener: opener) }
                } else {
                    grid
                }
            }
            .padding(.bottom, Theme.Spacing.xLarge)
        }
        .onGeometryChange(for: CGFloat.self, of: { proxy in proxy.size.width }, action: { value in width = value })
    }

    private var grid: some View {
        let width = cardWidth
        let columns = [
            GridItem(.fixed(width), spacing: Theme.Spacing.medium, alignment: .top),
            GridItem(.fixed(width), spacing: Theme.Spacing.medium, alignment: .top),
        ]
        return LazyVGrid(columns: columns, alignment: .leading, spacing: Theme.Spacing.large) {
            ForEach(row.items) { item in
                MusicNewCard(item: item, opener: opener, follow: { store.toggleFollow($0) }, width: width)
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }
}
