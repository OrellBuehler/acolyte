import SwiftUI

struct TVGenreView: View {
    let type: ExploreTypeID
    let genre: Int
    @State private var store: GenreStore

    init(type: ExploreTypeID, genre: Int) {
        self.type = type
        self.genre = genre
        _store = State(initialValue: GenreStore(type: type, genre: genre))
    }

    var body: some View {
        let info = store.info
        let name = DiscoverText.genre(info.name.isEmpty ? SDKClient.shared.genreName(genre, type: type) : info.name)
        ZStack {
            Theme.background.ignoresSafeArea()
            TVStatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
                TVEmptyView(title: L10n.tr("discover.genre.empty"), systemImage: "square.grid.2x2")
            } content: {
                TVBrowsePage(hero: info.hero, lookup: TVFocusLookup.cards(info.rows)) {
                    VStack(alignment: .leading, spacing: 0) {
                        Text(name)
                            .font(.title2.bold())
                            .foregroundStyle(Theme.text)
                            .accessibilityAddTraits(.isHeader)
                        if let error = info.load.error {
                            TVNoticeBanner(text: error.localized)
                        }
                    }
                } content: { focus in
                    TVDiscoverRows(
                        rows: info.rows,
                        top: info.top,
                        type: type,
                        topGenre: name,
                        pickService: { store.selectService($0) },
                        focus: focus
                    )
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .task { await store.refreshOwned() }
    }
}

struct TVBrowseListView: View {
    let link: BrowseLinkInfo
    @State private var store: BrowseViewStore
    @Environment(TVNavigator.self) private var navigator

    init(link: BrowseLinkInfo) {
        self.link = link
        _store = State(initialValue: BrowseViewStore(link: link))
    }

    var body: some View {
        let info = store.info
        let genre = DiscoverText.genre(info.genreName)
        let rows = [CardRowInfo.local(id: "browse", title: "", cards: info.cards, wide: false)]
        ZStack {
            Theme.background.ignoresSafeArea()
            TVStatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
                if link.target == .mood {
                    VStack(spacing: 28) {
                        TVEmptyView(title: L10n.tr("empty.title"), message: L10n.tr("discover.mood.empty"), systemImage: "square.grid.2x2")
                        Button(L10n.format("discover.open.genre", genre)) {
                            navigator.open(.genre(type: link.type, genre: link.genre))
                        }
                        .buttonStyle(.borderedProminent)
                    }
                } else {
                    TVEmptyView(title: L10n.tr("discover.category.empty"), systemImage: "square.grid.2x2")
                }
            } content: {
                TVAmbientPage(lookup: TVFocusLookup.images(rows)) { focus in
                    VStack(alignment: .leading, spacing: 8) {
                        Text(info.title.isEmpty ? genre : info.title)
                            .font(.title2.bold())
                            .foregroundStyle(Theme.text)
                            .accessibilityAddTraits(.isHeader)
                        if link.target == .mood, !genre.isEmpty {
                            Text("\(genre) · \(kinds(info.type))")
                                .font(.callout)
                                .foregroundStyle(Theme.textMuted)
                        }
                        if let error = info.load.error {
                            TVNoticeBanner(text: error.localized)
                        }
                    }
                    TVPosterGrid(cards: info.cards, typed: info.type == .all, rowId: "browse", focus: focus)
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.refreshOwned() }
    }

    private func kinds(_ type: ExploreTypeID) -> String {
        switch type {
        case .movies: L10n.tr("discover.type.movies")
        case .shows: L10n.tr("discover.type.shows")
        case .all: L10n.tr("discover.movies.and.shows")
        }
    }
}

struct TVPosterGrid: View {
    let cards: [DiscoverCardInfo]
    var typed = false
    let rowId: String
    let focus: FocusState<String?>.Binding

    var body: some View {
        LazyVGrid(
            columns: Array(repeating: GridItem(.fixed(Theme.TV.posterWidth), spacing: Theme.TV.gutter, alignment: .top), count: 6),
            alignment: .leading,
            spacing: Theme.TV.gutter
        ) {
            ForEach(cards) { card in
                TVDiscoverCard(
                    card: card,
                    typed: typed,
                    focus: TVFocusKey(binding: focus, id: TVFocusKey.id(row: rowId, card: card.id))
                )
            }
        }
        .padding(.vertical, Theme.TV.focusPad)
        .focusSection()
    }
}
