import SwiftUI

struct TVSearchView: View {
    let account: AccountInfo
    @State private var store = SearchStore()
    @Environment(TVNavigator.self) private var navigator

    var body: some View {
        @Bindable var store = store
        let info = store.info
        let query = store.text.trimmingCharacters(in: .whitespaces)
        let rows = info.results.map(TVSearchText.rows) ?? [:]
        ZStack {
            Theme.background.ignoresSafeArea()
            TVAmbientPage(lookup: TVFocusLookup.images(Array(rows.values))) { focus in
                scopes(info, query: query)
                if query.isEmpty {
                    idle(info)
                } else {
                    results(info, query: query, rows: rows, focus: focus)
                }
            }
        }
        .searchable(text: $store.text, prompt: L10n.tr("discover.search.placeholder"))
        .searchSuggestions {
            ForEach(info.completions, id: \.self) { completion in
                Label(completion, systemImage: "magnifyingglass").searchCompletion(completion)
            }
            ForEach(info.suggestions) { suggestion in
                Button { open(suggestion) } label: {
                    Text([suggestion.name, suggestion.sub].filter { !$0.isEmpty }.joined(separator: " · "))
                }
            }
        }
        .onSubmit(of: .search) { store.submit(store.text) }
        .onChange(of: store.text) { _, value in store.type(value) }
        .task(id: store.text) {
            try? await Task.sleep(for: .milliseconds(150))
            guard !Task.isCancelled else { return }
            await store.suggest(store.text)
        }
        .task(id: "\(store.text)|\(info.scope.rawValue)") {
            try? await Task.sleep(for: .milliseconds(250))
            guard !Task.isCancelled else { return }
            await store.run(store.text)
        }
        .task { await store.watch() }
        .task { await store.watchBrowse() }
        .task { await store.watchPeople() }
        .task { await store.load() }
        .task { await store.followBrowse() }
        .task { await store.refreshOwned() }
    }

    private func open(_ suggestion: SuggestionInfo) {
        switch suggestion.kind {
        case .person:
            navigator.open(.person(id: suggestion.personId))
        case .movie, .show:
            if suggestion.tmdb.isEmpty {
                store.submit(suggestion.name)
            } else {
                navigator.open(.title(kind: suggestion.kind == .show ? "show" : "movie", tmdb: suggestion.tmdb))
            }
        }
    }

    @ViewBuilder
    private func scopes(_ info: SearchInfo, query: String) -> some View {
        let shown = info.scopes.filter { $0 != .music }
        if shown.count > 1 {
            TVChipRow {
                ForEach(shown) { scope in
                    TVChip(title: TVSearchText.scope(scope), selected: scope == info.scope) { store.selectScope(scope) }
                }
            }
        }
        if !query.isEmpty {
            TVChipRow {
                TVChip(title: L10n.tr("discover.search.everywhere"), selected: !info.onlyLibrary) { store.setOnlyLibrary(false) }
                TVChip(title: L10n.tr("discover.search.in.library"), selected: info.onlyLibrary) { store.setOnlyLibrary(true) }
            }
        }
    }

    @ViewBuilder
    private func idle(_ info: SearchInfo) -> some View {
        if !info.recents.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                Text(L10n.tr("discover.recent"))
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                    .accessibilityAddTraits(.isHeader)
                TVChipRow {
                    ForEach(info.recents, id: \.self) { text in
                        TVChip(title: text, selected: false) { store.submit(text) }
                    }
                    TVChip(title: L10n.tr("discover.recent.clear"), selected: false) { store.clearRecents() }
                }
            }
        }
        if info.scope == .people {
            people(store.people)
        } else {
            browse(store.browse)
        }
    }

    @ViewBuilder
    private func browse(_ info: BrowseInfo) -> some View {
        if let error = info.load.error {
            TVNoticeBanner(text: error.localized)
        }
        if !info.tiles.isEmpty {
            tileGrid(title: L10n.tr("discover.browse.genres"), tiles: info.tiles)
        }
        ForEach(info.sections) { section in
            if section.tiles.isEmpty, !section.loaded || section.building {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .onAppear { store.loadSection(section.id) }
            } else if !section.tiles.isEmpty {
                tileGrid(title: DiscoverText.browseSection(section.id, fallback: section.title), tiles: section.tiles)
                    .onAppear { store.loadSection(section.id) }
            }
        }
    }

    private func tileGrid(title: String, tiles: [BrowseTileInfo]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.headline)
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            LazyVGrid(
                columns: Array(repeating: GridItem(.fixed(Theme.TV.backdropWidth), spacing: Theme.TV.gutter, alignment: .top), count: 4),
                alignment: .leading,
                spacing: Theme.TV.gutter
            ) {
                ForEach(tiles) { tile in
                    TVBrowseTile(
                        name: tile.isGenre ? DiscoverText.genre(tile.name) : tile.name,
                        image: tile.backdrop,
                        hue: tile.hue
                    ) { navigator.openBrowse(tile.link) }
                }
            }
            .padding(.vertical, Theme.TV.focusPad)
            .focusSection()
        }
    }

    @ViewBuilder
    private func people(_ info: PeopleBrowseInfo) -> some View {
        if let error = info.load.error {
            TVNoticeBanner(text: error.localized)
        }
        if info.filters.count > 1 {
            TVChipRow {
                ForEach(info.filters) { filter in
                    TVChip(title: TVSearchText.people(filter), selected: filter == info.filter) { store.selectPeople(filter) }
                }
            }
        }
        ForEach(info.shelves) { shelf in
            personShelf(
                title: shelf.id == "library" ? L10n.tr("discover.people.library") : L10n.tr("discover.people.hot"),
                people: shelf.people
            )
        }
        if !info.popular.isEmpty {
            Text(L10n.tr("discover.people.popular"))
                .font(.headline)
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            personGrid(info.popular)
            if info.more {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .onAppear { store.morePeople() }
            }
        } else if info.load.phase == .ready, info.shelves.isEmpty {
            TVEmptyView(title: L10n.tr("discover.people.empty"), systemImage: "person.2")
        }
        if !info.attribution.isEmpty {
            Text(info.attribution)
                .font(.caption)
                .foregroundStyle(Theme.textFaint)
        }
    }

    private func personShelf(title: String, people: [PersonCardInfo], seeAll: (() -> Void)? = nil) -> some View {
        TVShelf(title: title, seeAll: seeAll) {
            ForEach(people) { person in
                TVPersonCard(name: person.name, role: person.note, photo: person.photo) {
                    navigator.open(.person(id: person.id))
                }
            }
        }
    }

    private func personGrid(_ people: [PersonCardInfo]) -> some View {
        LazyVGrid(
            columns: Array(repeating: GridItem(.fixed(Theme.TV.personSize + 40), spacing: Theme.TV.gutter, alignment: .top), count: 7),
            alignment: .leading,
            spacing: Theme.TV.gutter
        ) {
            ForEach(people) { person in
                TVPersonCard(name: person.name, role: person.note, photo: person.photo) {
                    navigator.open(.person(id: person.id))
                }
            }
        }
        .padding(.vertical, Theme.TV.focusPad)
        .focusSection()
    }

    @ViewBuilder
    private func results(_ info: SearchInfo, query: String, rows: [String: CardRowInfo], focus: FocusState<String?>.Binding) -> some View {
        if let found = info.results, !(info.searching && found.searched != query) {
            content(info, found, rows: rows, focus: focus)
        } else {
            ProgressView().frame(maxWidth: .infinity, minHeight: Theme.TV.backdropHeight)
        }
    }

    @ViewBuilder
    private func content(_ info: SearchInfo, _ found: SearchResultsInfo, rows: [String: CardRowInfo], focus: FocusState<String?>.Binding) -> some View {
        let single = info.scope != .all
        if found.metadataDown {
            TVNoticeBanner(text: L10n.tr("discover.metadata.down"))
        }
        if let error = found.error {
            TVNoticeBanner(text: error.localized)
        }
        let count = found.library.count + found.movies.count + found.shows.count + found.people.count + found.more.count
        Text(L10n.plural("discover.results", count))
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
        if let understood = found.understood {
            VStack(alignment: .leading, spacing: 4) {
                Text(L10n.tr("discover.plain.title"))
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                    .accessibilityAddTraits(.isHeader)
                Text(L10n.format("discover.plain.understood", TVSearchText.understood(understood)))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            if let row = rows["plainLibrary"] { TVCardRow(row: row, focus: focus) }
            if let row = rows["more"] { TVCardRow(row: row, typed: true, focus: focus) }
        }
        if let row = rows["library"] {
            TVCardRow(row: row, typed: !single, focus: focus)
        }
        if !single, !found.top.isEmpty {
            TVShelf(title: L10n.tr("discover.top.matches")) {
                ForEach(found.top) { hit in
                    if let card = hit.card {
                        TVDiscoverCard(
                            card: card,
                            wide: true,
                            typed: true,
                            focus: TVFocusKey(binding: focus, id: TVFocusKey.id(row: "top", card: card.id))
                        )
                    } else if let person = hit.person {
                        TVPersonCard(name: person.name, role: person.note, photo: person.photo) {
                            navigator.open(.person(id: person.id))
                        }
                    }
                }
            }
        }
        if single {
            switch info.scope {
            case .movie:
                TVPosterGrid(cards: found.movies, rowId: "movies", focus: focus)
            case .show:
                TVPosterGrid(cards: found.shows, rowId: "shows", focus: focus)
            case .people:
                personGrid(found.people)
            case .all, .music:
                EmptyView()
            }
        } else {
            if !found.people.isEmpty {
                personShelf(title: L10n.tr("discover.scope.people"), people: found.people) { store.selectScope(.people) }
            }
            if let row = rows["movies"] {
                TVCardRow(row: row, seeAll: { store.selectScope(.movie) }, focus: focus)
            }
            if let row = rows["shows"] {
                TVCardRow(row: row, seeAll: { store.selectScope(.show) }, focus: focus)
            }
        }
        if found.isEmpty {
            VStack(spacing: 16) {
                Image(systemName: "magnifyingglass")
                    .font(.largeTitle)
                    .foregroundStyle(Theme.textMuted)
                    .accessibilityHidden(true)
                Text(L10n.format("discover.nothing.found", found.searched))
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                Text(L10n.tr("discover.tip.spelling")).font(.callout).foregroundStyle(Theme.textMuted)
                Text(L10n.tr("discover.tip.year")).font(.callout).foregroundStyle(Theme.textMuted)
                Text(L10n.tr("discover.tip.describe")).font(.callout).foregroundStyle(Theme.textMuted)
                if info.scope != .all {
                    Button(L10n.tr("discover.look.everywhere")) { store.selectScope(.all) }
                        .buttonStyle(.bordered)
                }
            }
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
        }
    }
}
