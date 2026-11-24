import SwiftUI

struct ExploreView: View {
    let account: AccountInfo
    @State private var store = ExploreStore()
    @State private var host = CardHost()
    @State private var notice: String?
    @Environment(Navigator.self) private var navigator

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            ContentUnavailableView {
                Label(L10n.tr("discover.explore.building.title"), systemImage: "safari")
            } description: {
                Text(L10n.tr("discover.explore.building.message"))
            } actions: {
                Button(SectionID.search.title) { navigator.open(.search) }
                    .buttonStyle(.borderedProminent)
            }
        } content: {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    TypePicker(type: store.info.type, select: { store.select($0) }, genre: { navigator.open(.genre(type: store.info.type, genre: $0)) })
                    if let error = store.info.load.error {
                        NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
                    }
                    if let notice {
                        NoticeBanner(text: notice).padding(.horizontal, Theme.Spacing.screen)
                    }
                    if store.info.sourceOff {
                        NoticeBanner(text: L10n.tr("discover.source.off")).padding(.horizontal, Theme.Spacing.screen)
                    }
                    if let hero = store.info.hero {
                        HeroView(card: hero, pills: store.info.heroPills.map { DiscoverText.pill($0) })
                    }
                    if let banner = store.info.banner {
                        BannerView(banner: banner)
                    }
                    if !store.info.strip.isEmpty {
                        StripView(tiles: store.info.strip, type: store.info.type, opened: { store.stripOpened($0) })
                            .onAppear { store.stripShown() }
                    }
                    ForEach(store.info.rows) { row in
                        ExploreRow(row: row, info: store.info, store: store)
                    }
                    if store.info.more {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .padding(Theme.Spacing.large)
                            .onAppear { store.loadMore() }
                    }
                }
                .padding(.bottom, Theme.Spacing.xLarge)
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(SectionID.explore.title)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    if store.info.canHide {
                        Button(L10n.tr("discover.hidden.title"), systemImage: "eye.slash") { navigator.open(.hidden) }
                    }
                    if store.info.canSeeStats {
                        Button(L10n.tr("discover.stats.title"), systemImage: "chart.bar") { navigator.open(.exploreStats) }
                    }
                    Button(L10n.tr("discover.reset"), systemImage: "arrow.counterclockwise") {
                        store.reset { ok in notice = L10n.tr(ok ? "discover.reset.done" : "discover.reset.failed") }
                    }
                } label: {
                    Label(L10n.tr("discover.more.actions"), systemImage: "ellipsis.circle")
                }
            }
        }
        .cardHost(host)
        .onAppear {
            host.canHide = store.info.canHide
            host.onChanged = { Task { await store.refresh() } }
            host.onOpen = { store.opened($0) }
            host.onAct = { store.asking($0) }
        }
        .onChange(of: store.info.canHide) { _, value in host.canHide = value }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .task { await store.refreshOwned() }
    }
}

private struct ExploreRow: View {
    let row: CardRowInfo
    let info: ExploreInfo
    let store: ExploreStore
    @Environment(Navigator.self) private var navigator

    var body: some View {
        switch row.type {
        case "top_services":
            if let top = info.top {
                TopServicesRowView(
                    top: top,
                    row: row,
                    title: DiscoverText.topTitle(info.type, genre: nil),
                    pick: { store.selectService($0) },
                    seeAll: { navigator.open(.popular) }
                )
            }
        case "genre_browse":
            GenreBrowseRowView(genres: info.genres)
        case "np_services":
            CardRowView(row: row, title: L10n.tr("discover.new.on.services"), seeAll: { navigator.open(.popular) })
        default:
            CardRowView(row: row, typed: info.type == .all)
        }
    }
}

struct TypePicker: View {
    let type: ExploreTypeID
    let select: (ExploreTypeID) -> Void
    var genre: ((Int) -> Void)?

    var body: some View {
        HStack(spacing: Theme.Spacing.small) {
            Picker(L10n.tr("discover.type.all"), selection: Binding(get: { type }, set: { select($0) })) {
                ForEach(ExploreTypeID.allCases) { Text(DiscoverText.type($0)).tag($0) }
            }
            .pickerStyle(.segmented)
            if let genre, type != .all {
                Menu {
                    ForEach(SDKClient.shared.genreOptions(type)) { option in
                        Button(DiscoverText.genre(option.name)) { genre(option.id) }
                    }
                } label: {
                    Label(L10n.tr("discover.genres"), systemImage: "chevron.down")
                        .labelStyle(.titleAndIcon)
                        .font(.subheadline.weight(.semibold))
                }
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.small)
    }
}

private struct BannerView: View {
    let banner: GenreBannerInfo
    @Environment(Navigator.self) private var navigator

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Button {
                navigator.open(.genre(type: banner.type, genre: banner.genre))
            } label: {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("discover.banner.eyebrow")).font(.footnote.weight(.semibold)).foregroundStyle(Theme.accentText)
                    Text(L10n.format("discover.banner.title", DiscoverText.genre(banner.name))).font(.title2.bold()).foregroundStyle(Theme.text)
                    Text(L10n.format("discover.banner.line", banner.have, banner.picks.count))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, Theme.Spacing.large)
                .padding(.top, Theme.Spacing.large)
            }
            .buttonStyle(.plain)
            CardShelf(title: "") {
                ForEach(banner.picks) { TitlePosterCard(card: $0) }
            }
        }
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.panel, style: .continuous))
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct StripView: View {
    let tiles: [BrowseTileInfo]
    let type: ExploreTypeID
    let opened: (String) -> Void
    @Environment(Navigator.self) private var navigator

    var body: some View {
        CardShelf(title: L10n.tr("discover.categories")) {
            ForEach(tiles) { tile in
                Button {
                    opened(tile.stripKey)
                    navigator.openBrowse(tile.link)
                } label: {
                    BrowseTileView(
                        name: tile.isGenre ? DiscoverText.genre(tile.name) : tile.name,
                        image: tile.backdrop,
                        hue: tile.hue,
                        label: DiscoverText.stripLabel(tile.section),
                        plain: true
                    )
                    .frame(width: 184, height: 138)
                }
                .buttonStyle(.plain)
            }
            Button {
                navigator.open(.search)
            } label: {
                BrowseTileView(
                    name: L10n.tr("discover.all.categories"),
                    image: tiles.first?.backdrop,
                    hue: 0,
                    label: L10n.tr("discover.strip.browse")
                )
                .frame(width: 184, height: 138)
            }
            .buttonStyle(.plain)
        }
    }
}

extension Navigator {
    func openBrowse(_ link: BrowseLinkInfo) {
        if link.target == .genre {
            open(.genre(type: link.type, genre: link.genre))
        } else {
            open(.browse(link))
        }
    }
}

struct GenreView: View {
    let type: ExploreTypeID
    let genre: Int
    let account: AccountInfo
    @State private var store: GenreStore
    @State private var host = CardHost()

    init(type: ExploreTypeID, genre: Int, account: AccountInfo) {
        self.type = type
        self.genre = genre
        self.account = account
        _store = State(initialValue: GenreStore(type: type, genre: genre))
    }

    var body: some View {
        let name = DiscoverText.genre(store.info.name.isEmpty ? SDKClient.shared.genreName(genre, type: type) : store.info.name)
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(title: L10n.tr("discover.genre.empty"), systemImage: "square.grid.2x2")
        } content: {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    if let error = store.info.load.error {
                        NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
                    }
                    if let hero = store.info.hero {
                        HeroView(card: hero, pills: [])
                    }
                    ForEach(store.info.rows) { row in
                        if row.type == "top_services", let top = store.info.top {
                            TopServicesRowView(
                                top: top,
                                row: row,
                                title: DiscoverText.topTitle(type, genre: name),
                                pick: { store.selectService($0) }
                            )
                        } else {
                            CardRowView(row: row)
                        }
                    }
                }
                .padding(.bottom, Theme.Spacing.xLarge)
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(name)
        .cardHost(host)
        .onAppear {
            host.canHide = store.info.canHide
            host.onChanged = { Task { await store.refresh() } }
        }
        .onChange(of: store.info.canHide) { _, value in host.canHide = value }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .task { await store.refreshOwned() }
    }
}

struct BrowseView: View {
    let link: BrowseLinkInfo
    let account: AccountInfo
    @State private var store: BrowseViewStore
    @State private var host = CardHost()
    @Environment(Navigator.self) private var navigator

    init(link: BrowseLinkInfo, account: AccountInfo) {
        self.link = link
        self.account = account
        _store = State(initialValue: BrowseViewStore(link: link))
    }

    var body: some View {
        let genre = DiscoverText.genre(store.info.genreName)
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            if link.target == .mood {
                ContentUnavailableView {
                    Label(L10n.tr("empty.title"), systemImage: "square.grid.2x2")
                } description: {
                    Text(L10n.tr("discover.mood.empty"))
                } actions: {
                    Button(L10n.format("discover.open.genre", genre)) { navigator.open(.genre(type: link.type, genre: link.genre)) }
                }
            } else {
                EmptyStateView(title: L10n.tr("discover.category.empty"), systemImage: "square.grid.2x2")
            }
        } content: {
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                    if let error = store.info.load.error {
                        NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
                    }
                    if link.target == .mood, !genre.isEmpty {
                        Text("\(genre) · \(kinds)")
                            .font(.callout)
                            .foregroundStyle(Theme.textMuted)
                            .padding(.horizontal, Theme.Spacing.screen)
                    }
                    CardGrid(cards: store.info.cards, typed: store.info.type == .all)
                }
                .padding(.bottom, Theme.Spacing.xLarge)
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(store.info.title.isEmpty ? genre : store.info.title)
        .cardHost(host)
        .onAppear {
            host.canHide = store.info.canHide
            host.onChanged = { Task { await store.refresh() } }
        }
        .onChange(of: store.info.canHide) { _, value in host.canHide = value }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.refreshOwned() }
    }

    private var kinds: String {
        switch store.info.type {
        case .movies: L10n.tr("discover.type.movies")
        case .shows: L10n.tr("discover.type.shows")
        case .all: L10n.tr("discover.movies.and.shows")
        }
    }
}
