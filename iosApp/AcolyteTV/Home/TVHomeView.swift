import SwiftUI

struct TVHomeView: View {
    @State private var store = HomeStore()
    @State private var billboard: DiscoverCardInfo?
    @State private var ambient: DiscoverCardInfo?
    @FocusState private var focus: String?
    @Environment(\.scenePhase) private var scenePhase
    @Environment(TVNavigator.self) private var navigator

    private static let featuredRow = "featured"

    var body: some View {
        let info = store.info
        let featured = info.pictures
        let rows = TVHomeView.rows(info)
        let lookup = TVHomeView.lookup(featured: featured, rows: rows)
        let first = featured.first?.card ?? rows.first?.row.cards.first
        let shown = billboard ?? first
        ZStack {
            Theme.background.ignoresSafeArea()
            TVBackdropLayer(source: (ambient ?? shown)?.wideImage)
            TVStatusContent(
                load: info.load,
                isEmpty: first == nil && info.comingUp.isEmpty,
                reload: { await store.refresh() }
            ) {
                TVEmptyView()
            } content: {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: Theme.TV.sectionGap) {
                        VStack(alignment: .leading, spacing: 0) {
                            if let error = info.load.error {
                                TVNoticeBanner(text: error.localized)
                            }
                            Spacer(minLength: 0)
                            if let shown {
                                TVBillboard(card: shown, label: featured.first { $0.card.id == shown.id }.map { HomeText.picture($0) })
                                    .padding(.bottom, 20)
                            }
                            if !featured.isEmpty {
                                TVShelf(title: L10n.tr("tv.home.featured")) {
                                    ForEach(featured) { picture in
                                        TVDiscoverCard(
                                            card: picture.card,
                                            wide: true,
                                            badge: HomeText.picture(picture),
                                            showsText: false,
                                            focus: TVFocusKey(binding: $focus, id: TVFocusKey.id(row: TVHomeView.featuredRow, card: picture.card.id))
                                        )
                                    }
                                }
                            }
                        }
                        .containerRelativeFrame(.vertical, alignment: .bottomLeading) { height, _ in height }
                        ForEach(rows, id: \.row.id) { entry in
                            TVCardRow(row: entry.row, title: entry.title, seeAll: seeAll(entry.row), focus: $focus)
                        }
                        if !info.comingUp.isEmpty {
                            TVShelf(title: L10n.tr("home.coming.up"), seeAll: calendarAction) {
                                ForEach(info.comingUp) { up in
                                    TVBackdropCard(
                                        title: up.name,
                                        subtitle: [up.sub, HomeText.when(up)].filter { !$0.isEmpty }.joined(separator: " · "),
                                        image: up.image
                                    ) {
                                        if !up.tmdb.isEmpty { navigator.open(.title(kind: up.kind, tmdb: up.tmdb)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        .onChange(of: focus) { _, new in
            guard let new, let card = lookup[new] else { return }
            ambient = card
            if new.hasPrefix(TVHomeView.featuredRow + "|") { billboard = card }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await store.refresh() } }
        }
    }

    private var calendarAction: (() -> Void)? {
        TVSections.visible(.calendar, for: navigator.account) ? { navigator.open(.calendar) } : nil
    }

    private func seeAll(_ row: CardRowInfo) -> (() -> Void)? {
        row.seeAll && TVSections.visible(.explore, for: navigator.account) ? { navigator.open(.explore) } : nil
    }

    struct Entry {
        let title: String
        let row: CardRowInfo
    }

    static func rows(_ info: HomeInfo) -> [Entry] {
        var result: [Entry] = []
        if let arrived = info.arrived {
            result.append(Entry(title: HomeText.arrived(info.arrivedTitle), row: arrived))
        }
        for row in info.borrowed {
            result.append(Entry(title: row.title, row: row))
        }
        return result.filter { !$0.row.cards.isEmpty }
    }

    static func lookup(featured: [HomePictureInfo], rows: [Entry]) -> [String: DiscoverCardInfo] {
        var result: [String: DiscoverCardInfo] = [:]
        for picture in featured {
            result[TVFocusKey.id(row: featuredRow, card: picture.card.id)] = picture.card
        }
        for entry in rows {
            for card in entry.row.cards {
                result[TVFocusKey.id(row: entry.row.id, card: card.id)] = card
            }
        }
        return result
    }
}
