import SwiftUI

struct TVExploreView: View {
    let account: AccountInfo
    @State private var store = ExploreStore()
    @Environment(\.scenePhase) private var scenePhase
    @Environment(TVNavigator.self) private var navigator

    var body: some View {
        let info = store.info
        let banner = bannerRow(info.banner)
        let all = info.rows + (banner.map { [$0] } ?? [])
        ZStack {
            Theme.background.ignoresSafeArea()
            TVStatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
                TVEmptyView(
                    title: L10n.tr("discover.explore.building.title"),
                    message: L10n.tr("discover.explore.building.message"),
                    systemImage: "safari"
                )
            } content: {
                TVBrowsePage(
                    hero: info.hero,
                    pills: info.heroPills.map { DiscoverText.pill($0) },
                    lookup: TVFocusLookup.cards(all),
                    initialFocus: info.rows.first.flatMap { row in row.cards.first.map { TVFocusKey.id(row: row.id, card: $0.id) } },
                    screen: "explore"
                ) {
                    header(info)
                } content: { focus in
                    if let banner, let source = info.banner {
                        TVCardRow(row: banner, title: L10n.format("discover.banner.title", DiscoverText.genre(source.name)), seeAll: {
                            navigator.open(.genre(type: source.type, genre: source.genre))
                        }, focus: focus)
                    }
                    if !info.strip.isEmpty {
                        strip(info)
                    }
                    TVDiscoverRows(
                        rows: info.rows,
                        top: info.top,
                        genres: info.genres,
                        type: info.type,
                        seeAllNew: popularAction,
                        pickService: { store.selectService($0) },
                        focus: focus
                    )
                    if info.more {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .onAppear { store.loadMore() }
                    }
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .task { await store.refreshOwned() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await store.refresh() } }
        }
    }

    private func header(_ info: ExploreInfo) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            TVChipRow {
                TVTypeChips(type: info.type) { store.select($0) }
            }
            if let error = info.load.error {
                TVNoticeBanner(text: error.localized)
            }
            if info.sourceOff {
                TVNoticeBanner(text: L10n.tr("discover.source.off"))
            }
        }
    }

    private func strip(_ info: ExploreInfo) -> some View {
        TVShelf(title: L10n.tr("discover.categories")) {
            ForEach(info.strip) { tile in
                TVBrowseTile(
                    name: tile.isGenre ? DiscoverText.genre(tile.name) : tile.name,
                    image: tile.backdrop,
                    hue: tile.hue,
                    label: DiscoverText.stripLabel(tile.section)
                ) {
                    store.stripOpened(tile.stripKey)
                    navigator.openBrowse(tile.link)
                }
            }
            TVBrowseTile(
                name: L10n.tr("discover.all.categories"),
                image: info.strip.first?.backdrop,
                label: L10n.tr("discover.strip.browse")
            ) { navigator.open(.search) }
        }
        .onAppear { store.stripShown() }
    }

    private func bannerRow(_ banner: GenreBannerInfo?) -> CardRowInfo? {
        guard let banner, !banner.picks.isEmpty else { return nil }
        return CardRowInfo.local(
            id: "banner",
            title: L10n.format("discover.banner.title", DiscoverText.genre(banner.name)),
            subtitle: L10n.format("discover.banner.line", banner.have, banner.picks.count),
            cards: banner.picks
        )
    }

    private var popularAction: (() -> Void)? {
        TVSections.visible(.popular, for: account) ? { navigator.open(.popular) } : nil
    }
}
