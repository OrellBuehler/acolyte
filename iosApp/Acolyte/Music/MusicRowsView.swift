import SwiftUI

struct MusicRowsView: View {
    let kind: String
    let slug: String
    let account: AccountInfo
    @State private var store: MusicRowsStore

    init(kind: String, slug: String, account: AccountInfo) {
        self.kind = kind
        self.slug = slug
        self.account = account
        _store = State(initialValue: MusicRowsStore(kind: kind, slug: slug, region: SDKClient.region))
    }

    var body: some View {
        MusicRowsContent(info: store.info, store: store)
            .navigationTitle(store.info.title.isEmpty ? SectionID.music.title : store.info.title)
            .navigationBarTitleDisplayMode(.inline)
            .task { await store.watch() }
            .task { await store.load() }
    }
}

struct MusicRowsContent: View {
    let info: MusicRowsInfo
    let store: MusicRowsStore

    var body: some View {
        MusicOpenerReader { opener in
            if info.load.phase == .failed {
                failure
            } else {
                StatusContent(load: info.load, isEmpty: info.isEmpty || info.kid, reload: { await store.refresh() }) {
                    MusicRowsEmpty(info: info)
                } content: {
                    list(opener)
                }
            }
        }
    }

    private var failure: some View {
        ContentUnavailableView {
            Label(L10n.tr("music.rows.error.title"), systemImage: "music.note")
        } description: {
            Text(L10n.tr("music.rows.error.message"))
        } actions: {
            Button(L10n.tr("common.retry")) { Task { await store.refresh() } }
                .buttonStyle(.borderedProminent)
        }
    }

    private func list(_ opener: MusicOpener) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Theme.Spacing.small) {
                if let error = info.load.error {
                    NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
                }
                if info.isPage { MusicRowsBand(info: info) }
                spotlight(opener)
                strip(opener)
                browse(opener)
                if info.thin { MusicRowsThin(info: info, opener: opener) }
                rows(opener)
                if info.more {
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

    @ViewBuilder
    private func spotlight(_ opener: MusicOpener) -> some View {
        let items = (info.hero.map { [$0] } ?? []) + info.spotlight
        if !items.isEmpty {
            CardShelf(title: L10n.tr("music.rows.spotlight")) {
                ForEach(items) { MusicSpotlightCard(item: $0, opener: opener) }
            }
        }
    }

    @ViewBuilder
    private func strip(_ opener: MusicOpener) -> some View {
        if !info.strip.isEmpty {
            CardShelf(title: L10n.tr("discover.genres")) {
                ForEach(info.strip) { MusicStripTile(item: $0, opener: opener) }
            }
        }
    }

    @ViewBuilder
    private func browse(_ opener: MusicOpener) -> some View {
        let items = info.grid.flatMap { $0.items }
        if !items.isEmpty {
            SectionHeader(
                title: L10n.tr("music.rows.browse"),
                actionTitle: L10n.tr("music.rows.browse.all"),
                action: { opener.browseAll() }
            )
            .padding(.horizontal, Theme.Spacing.screen)
            MusicTileGrid(cells: items.map { MusicTileCell(item: $0) }, opener: opener)
        }
    }

    private var regionRowIndex: Int? {
        if let index = info.rows.firstIndex(where: { $0.type == "popular_country" }) { return index }
        return info.rows.firstIndex(where: { $0.type == "top_albums" })
    }

    private func rows(_ opener: MusicOpener) -> some View {
        let regional = info.regions.count > 1 ? regionRowIndex : nil
        return ForEach(Array(info.rows.enumerated()), id: \.offset) { index, row in
            if !row.items.isEmpty {
                MusicShelfRowView(
                    row: row,
                    opener: opener,
                    header: {
                        if index == regional {
                            MusicRegionMenu(region: info.region, regions: info.regions, select: { store.setRegion($0) })
                        }
                    },
                    card: { item in
                        MusicItemCard(item: item, onOpen: { opener.open(item) }, onAct: { opener.act(item) })
                    }
                )
            }
        }
    }
}

struct MusicRowsEmpty: View {
    let info: MusicRowsInfo

    var body: some View {
        if info.kid {
            EmptyStateView(
                title: L10n.tr("music.rows.kid.title"),
                message: L10n.tr("music.rows.kid.message"),
                systemImage: "music.note"
            )
        } else {
            EmptyStateView(title: title, message: L10n.tr("music.rows.empty.message"), systemImage: "music.note")
        }
    }

    private var title: String {
        if !info.isPage { return L10n.tr("music.rows.empty.explore") }
        if !info.title.isEmpty { return L10n.format("music.rows.empty.page", info.title) }
        return L10n.tr("music.rows.empty.page.plain")
    }
}

struct MusicRowsThin: View {
    let info: MusicRowsInfo
    let opener: MusicOpener

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("music.rows.thin.title"))
                .font(.headline)
                .foregroundStyle(Theme.text)
            Text(L10n.tr("music.rows.thin.message"))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
            ForEach(info.similar) { similar in
                Button(similar.name) { opener.open(similar.target) }
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Theme.accentText)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.screen)
    }
}
