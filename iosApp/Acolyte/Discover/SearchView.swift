import SwiftUI

struct SearchView: View {
    let account: AccountInfo
    @State private var store = SearchStore()
    @State private var host = CardHost()
    @Environment(Navigator.self) private var navigator

    var body: some View {
        @Bindable var store = store
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Theme.Spacing.small) {
                if store.info.scopes.count > 2 {
                    Picker(L10n.tr("discover.scope.people"), selection: Binding(get: { store.info.scope }, set: { store.selectScope($0) })) {
                        ForEach(store.info.scopes) { Text(scopeText($0)).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, Theme.Spacing.screen)
                }
                if store.text.trimmingCharacters(in: .whitespaces).isEmpty {
                    if !store.info.recents.isEmpty {
                        RecentsView(recents: store.info.recents, pick: { store.submit($0) }, clear: { store.clearRecents() })
                    }
                    if store.info.scope == .people {
                        PeopleBrowseView(info: store.people, select: { store.selectPeople($0) }, more: { store.morePeople() })
                    } else {
                        BrowseGridView(info: store.browse, loadSection: { store.loadSection($0) })
                    }
                } else {
                    Picker(
                        L10n.tr("discover.search.from"),
                        selection: Binding(get: { store.info.onlyLibrary }, set: { store.setOnlyLibrary($0) })
                    ) {
                        Text(L10n.tr("discover.search.everywhere")).tag(false)
                        Text(L10n.tr("discover.search.in.library")).tag(true)
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, Theme.Spacing.screen)
                    ResultsView(info: store.info, text: store.text, search: { store.submit($0) }, scope: { store.selectScope($0) })
                }
            }
            .padding(.bottom, Theme.Spacing.xLarge)
        }
        .refreshable {
            if store.text.isEmpty {
                await store.refreshBrowse()
            } else {
                await store.run(store.text)
            }
        }
        .navigationTitle(SectionID.search.title)
        .searchable(text: $store.text, prompt: L10n.tr("discover.search.placeholder"))
        .searchSuggestions {
            ForEach(store.info.completions, id: \.self) { completion in
                Label(completion, systemImage: "magnifyingglass").searchCompletion(completion)
            }
            ForEach(store.info.suggestions) { suggestion in
                Button {
                    open(suggestion)
                } label: {
                    SuggestionRow(suggestion: suggestion)
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
        .task(id: "\(store.text)|\(store.info.scope.rawValue)") {
            try? await Task.sleep(for: .milliseconds(250))
            guard !Task.isCancelled else { return }
            await store.run(store.text)
        }
        .cardHost(host)
        .onAppear {
            host.canHide = store.info.canHide
            host.onChanged = { store.submit(store.text) }
        }
        .onChange(of: store.info.canHide) { _, value in host.canHide = value }
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
}

func scopeText(_ scope: SearchScopeID) -> String {
    switch scope {
    case .all: L10n.tr("discover.type.all")
    case .people: L10n.tr("discover.scope.people")
    case .movie: L10n.tr("discover.type.movies")
    case .show: L10n.tr("discover.type.shows")
    }
}

private struct SuggestionRow: View {
    let suggestion: SuggestionInfo

    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            AuthImage(source: suggestion.art, size: .thumb) { ArtPlaceholderView(name: suggestion.name) }
                .frame(width: 44, height: 44)
                .clipShape(suggestion.kind == .person ? AnyShape(Circle()) : AnyShape(RoundedRectangle(cornerRadius: 8)))
            VStack(alignment: .leading, spacing: 2) {
                Text(suggestion.name).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text).lineLimit(1)
                if !suggestion.sub.isEmpty {
                    Text(suggestion.sub).font(.footnote).foregroundStyle(Theme.textMuted).lineLimit(1)
                }
            }
            Spacer(minLength: 0)
            switch suggestion.state {
            case .have: StatusChip(text: L10n.tr("discover.suggest.have"), tone: .ok)
            case .missing: StatusChip(text: L10n.tr("discover.suggest.missing"), tone: .wait)
            case .asked: StatusChip(text: L10n.tr("discover.suggest.asked"), tone: .idle)
            case .none: EmptyView()
            }
        }
    }
}

private struct RecentsView: View {
    let recents: [String]
    let pick: (String) -> Void
    let clear: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("discover.recent"), actionTitle: L10n.tr("discover.recent.clear"), action: clear)
                .padding(.horizontal, Theme.Spacing.screen)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(recents, id: \.self) { text in
                        Button {
                            pick(text)
                        } label: {
                            Label(text, systemImage: "clock")
                                .font(.subheadline)
                                .padding(.horizontal, 12)
                                .frame(minHeight: 32)
                                .background(Theme.surfaceRaised, in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, Theme.Spacing.screen)
            }
        }
    }
}

private struct ResultsView: View {
    let info: SearchInfo
    let text: String
    let search: (String) -> Void
    let scope: (SearchScopeID) -> Void
    @Environment(Navigator.self) private var navigator

    var body: some View {
        if let results = info.results, !(info.searching && results.searched != text.trimmingCharacters(in: .whitespaces)) {
            content(results)
        } else {
            ProgressView().frame(maxWidth: .infinity).padding(Theme.Spacing.xLarge)
        }
    }

    @ViewBuilder
    private func content(_ results: SearchResultsInfo) -> some View {
        let single = info.scope != .all
        if results.metadataDown {
            NoticeBanner(text: L10n.tr("discover.metadata.down")).padding(.horizontal, Theme.Spacing.screen)
        }
        if let error = results.error {
            NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
        }
        let count = results.library.count + results.movies.count + results.shows.count + results.people.count + results.more.count
        Text(L10n.plural("discover.results", count))
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
            .padding(.horizontal, Theme.Spacing.screen)
        if let understood = results.understood {
            PlainBlock(understood: understood, results: results)
        }
        if !results.library.isEmpty {
            CardShelf(title: L10n.tr("discover.search.in.library")) {
                ForEach(results.library) { TitleWideCard(card: $0, typed: !single) }
            }
        }
        if !single, !results.top.isEmpty {
            CardShelf(title: L10n.tr("discover.top.matches")) {
                ForEach(results.top) { hit in
                    if let card = hit.card {
                        TitleWideCard(card: card, typed: true)
                    } else if let person = hit.person {
                        PersonCircleView(person: person)
                    }
                }
            }
        }
        if single {
            switch info.scope {
            case .movie:
                CardGrid(cards: results.movies)
            case .show:
                CardGrid(cards: results.shows, fallback: { search($0.name) })
            case .people:
                PeopleGrid(people: results.people)
            case .all:
                EmptyView()
            }
        } else {
            if !results.people.isEmpty {
                CardShelf(title: L10n.tr("discover.scope.people"), seeAll: { scope(.people) }) {
                    ForEach(results.people) { PersonCircleView(person: $0) }
                }
            }
            if !results.movies.isEmpty {
                CardShelf(title: L10n.tr("discover.type.movies"), seeAll: { scope(.movie) }) {
                    ForEach(results.movies) { TitleWideCard(card: $0) }
                }
            }
            if !results.shows.isEmpty {
                CardShelf(title: L10n.tr("discover.type.shows"), seeAll: { scope(.show) }) {
                    ForEach(results.shows) { card in TitleWideCard(card: card, fallback: { search(card.name) }) }
                }
            }
        }
        if results.isEmpty {
            VStack(spacing: Theme.Spacing.small) {
                Image(systemName: "magnifyingglass").font(.largeTitle).foregroundStyle(Theme.textMuted)
                Text(L10n.format("discover.nothing.found", results.searched)).font(.headline).foregroundStyle(Theme.text)
                Text(L10n.tr("discover.tip.spelling")).font(.callout).foregroundStyle(Theme.textMuted)
                Text(L10n.tr("discover.tip.year")).font(.callout).foregroundStyle(Theme.textMuted)
                Text(L10n.tr("discover.tip.id")).font(.callout).foregroundStyle(Theme.textMuted)
                Text(L10n.tr("discover.tip.describe")).font(.callout).foregroundStyle(Theme.textMuted)
                if info.scope != .all {
                    Button(L10n.tr("discover.look.everywhere")) { scope(.all) }.buttonStyle(.bordered)
                }
                if info.canManage {
                    Button(L10n.tr("discover.check.sources")) { navigator.open(.settings) }
                }
            }
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(Theme.Spacing.xLarge)
        }
    }
}

private struct PlainBlock: View {
    let understood: UnderstoodInfo
    let results: SearchResultsInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("discover.plain.title")).padding(.horizontal, Theme.Spacing.screen)
            Text(L10n.format("discover.plain.understood", line))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .padding(.horizontal, Theme.Spacing.screen)
            if !results.plainLibrary.isEmpty {
                CardShelf(title: L10n.tr("discover.search.in.library")) {
                    ForEach(results.plainLibrary) { TitleWideCard(card: $0) }
                }
            }
            if !results.more.isEmpty {
                CardShelf(title: L10n.tr("discover.plain.more")) {
                    ForEach(results.more) { TitleWideCard(card: $0, typed: true) }
                }
            }
        }
    }

    private var line: String {
        let kind: String =
            if understood.music {
                L10n.tr("discover.kind.music")
            } else if understood.kind == .movie {
                L10n.tr("discover.type.movies")
            } else if understood.kind == .show {
                L10n.tr("discover.type.shows")
            } else {
                L10n.tr("discover.plain.titles")
            }
        let genres = understood.genres.map(DiscoverText.genre).joined(separator: L10n.tr("discover.plain.or"))
        let head = [genres, kind].filter { !$0.isEmpty }.joined(separator: " ")
        var years = ""
        if understood.yearFrom > 0, understood.yearFrom == understood.yearTo {
            years = String(understood.yearFrom)
        } else if understood.yearFrom > 0, understood.yearTo > 0 {
            years = "\(understood.yearFrom)–\(understood.yearTo)"
        } else if understood.yearFrom > 0 {
            years = L10n.format("discover.plain.from", understood.yearFrom)
        } else if understood.yearTo > 0 {
            years = L10n.format("discover.plain.to", understood.yearTo)
        }
        let people = understood.people.isEmpty ? "" : L10n.format("discover.plain.with", understood.people.joined(separator: ", "))
        let words = understood.words.isEmpty ? "" : L10n.format("discover.plain.about", understood.words.joined(separator: ", "))
        let name = understood.name.isEmpty ? "" : "“\(understood.name)”"
        return [name, head, years, people, words].filter { !$0.isEmpty }.joined(separator: " · ")
    }
}

struct PeopleGrid: View {
    let people: [PersonCardInfo]

    var body: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: Theme.Spacing.small), count: 3), spacing: Theme.Spacing.large) {
            ForEach(people) { PersonCircleView(person: $0) }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct BrowseGridView: View {
    let info: BrowseInfo
    let loadSection: (String) -> Void
    @Environment(Navigator.self) private var navigator

    var body: some View {
        if let error = info.load.error {
            NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
        }
        if !info.tiles.isEmpty {
            SectionHeader(title: L10n.tr("discover.browse.genres")).padding(.horizontal, Theme.Spacing.screen)
            grid(info.tiles)
        }
        ForEach(info.sections) { section in
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                SectionHeader(title: DiscoverText.browseSection(section.id, fallback: section.title))
                    .padding(.horizontal, Theme.Spacing.screen)
                if section.tiles.isEmpty, !section.loaded || section.building {
                    ProgressView().frame(maxWidth: .infinity).padding(Theme.Spacing.large)
                } else {
                    grid(section.tiles)
                }
            }
            .onAppear { loadSection(section.id) }
        }
    }

    private func grid(_ tiles: [BrowseTileInfo]) -> some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: Theme.Spacing.medium), GridItem(.flexible(), spacing: Theme.Spacing.medium)], spacing: Theme.Spacing.medium) {
            ForEach(tiles) { tile in
                Button {
                    navigator.openBrowse(tile.link)
                } label: {
                    BrowseTileView(name: tile.isGenre ? DiscoverText.genre(tile.name) : tile.name, image: tile.backdrop, hue: tile.hue)
                        .aspectRatio(3 / 4, contentMode: .fit)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct PeopleBrowseView: View {
    let info: PeopleBrowseInfo
    let select: (PeopleFilterID) -> Void
    let more: () -> Void

    var body: some View {
        if let error = info.load.error {
            NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
        }
        if info.filters.count > 1 {
            ServiceChipsView(
                chips: info.filters.enumerated().map { ServiceChipInfo(id: Int64($0.offset), name: filterText($0.element)) },
                selected: Int64(info.filters.firstIndex(of: info.filter) ?? 0),
                pick: { index in
                    let position = Int(index)
                    if info.filters.indices.contains(position) { select(info.filters[position]) }
                }
            )
        }
        ForEach(info.shelves) { shelf in
            CardShelf(title: shelf.id == "library" ? L10n.tr("discover.people.library") : L10n.tr("discover.people.hot")) {
                ForEach(shelf.people) { PersonCircleView(person: $0) }
            }
        }
        if !info.popular.isEmpty {
            SectionHeader(title: L10n.tr("discover.people.popular")).padding(.horizontal, Theme.Spacing.screen)
            PeopleGrid(people: info.popular)
            if info.more {
                ProgressView().frame(maxWidth: .infinity).padding(Theme.Spacing.large).onAppear { more() }
            }
        } else if info.load.phase == .ready, info.shelves.isEmpty {
            EmptyStateView(title: L10n.tr("discover.people.empty"), systemImage: "person.2")
        }
        if !info.attribution.isEmpty {
            Text(info.attribution).font(.caption2).foregroundStyle(Theme.textFaint).padding(Theme.Spacing.screen)
        }
    }

    private func filterText(_ filter: PeopleFilterID) -> String {
        switch filter {
        case .popular: L10n.tr("discover.people.popular")
        case .actors: L10n.tr("discover.people.actors")
        case .directors: L10n.tr("discover.people.directors")
        case .composers: L10n.tr("discover.people.composers")
        }
    }
}
