import SwiftUI

struct PersonView: View {
    let id: String
    let account: AccountInfo
    @State private var store: PersonStore
    @State private var host = CardHost()
    @State private var about = false

    init(id: String, account: AccountInfo) {
        self.id = id
        self.account = account
        _store = State(initialValue: PersonStore(id: id))
    }

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: Theme.Spacing.small) {
                        if let error = info.load.error {
                            NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
                        }
                        PersonHeader(info: info, filmography: { withAnimation { proxy.scrollTo("filmography", anchor: .top) } }, about: { about = true })
                        if let used = info.limitUsed, let left = info.limitLeft {
                            NoticeBanner(text: L10n.format("discover.person.limit", used, left)).padding(.horizontal, Theme.Spacing.screen)
                        }
                        if !info.knownFor.isEmpty {
                            CardShelf(title: L10n.tr("discover.person.known.for")) {
                                ForEach(Array(info.knownFor.enumerated()), id: \.element.id) { index, card in
                                    RankedPoster(rank: index + 1) { TitlePosterCard(card: card) }
                                }
                            }
                        }
                        shelf(L10n.tr("discover.person.coming.soon"), info.comingSoon)
                        shelf(L10n.tr("discover.search.in.library"), info.inLibrary)
                        shelf(L10n.tr("discover.person.asked"), info.asked)
                        FilmographyBar(info: info, select: { role, sort in store.select(role: role, sort: sort) })
                            .id("filmography")
                        if info.decades.isEmpty, !info.reloading {
                            EmptyStateView(title: L10n.tr("discover.person.no.movies"), systemImage: "film")
                        }
                        ForEach(info.decades) { decade in
                            CardShelf(
                                title: decade.year == 0
                                    ? L10n.tr("discover.person.coming.soon")
                                    : L10n.format("discover.person.decade", decade.year)
                            ) {
                                ForEach(decade.cards) { TitleWideCard(card: $0) }
                            }
                        }
                        if !info.biography.isEmpty {
                            PersonAboutCard(info: info) { about = true }
                        }
                        if info.oftenWith.count >= 4 {
                            CardShelf(title: L10n.tr("discover.person.often")) {
                                ForEach(info.oftenWith) { PersonCircleView(person: $0) }
                            }
                        }
                        if !info.attribution.isEmpty {
                            Text(info.attribution).font(.caption2).foregroundStyle(Theme.textFaint).padding(Theme.Spacing.screen)
                        }
                    }
                    .padding(.bottom, Theme.Spacing.xLarge)
                }
                .refreshable { await store.refresh() }
            }
        }
        .navigationTitle(info.name)
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $about) { AboutSheet(info: info) }
        .cardHost(host)
        .onAppear {
            host.canHide = info.canHide
            host.onChanged = { Task { await store.refresh() } }
        }
        .onChange(of: info.canHide) { _, value in host.canHide = value }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.refreshOwned() }
    }

    @ViewBuilder
    private func shelf(_ title: String, _ cards: [DiscoverCardInfo]) -> some View {
        if !cards.isEmpty {
            CardShelf(title: title) {
                ForEach(cards) { TitleWideCard(card: $0) }
            }
        }
    }
}

private func roleWord(_ role: String) -> String {
    switch role {
    case "Actor": L10n.tr("discover.role.actor")
    case "Director": L10n.tr("discover.role.director")
    default: role
    }
}

private struct PersonHeader: View {
    let info: PersonPageInfo
    let filmography: () -> Void
    let about: () -> Void

    var body: some View {
        VStack(spacing: Theme.Spacing.small) {
            AuthImage(source: info.photo, size: .poster) { ArtPlaceholderView(name: info.name) }
                .frame(width: 128, height: 128)
                .clipShape(Circle())
            Text(info.name).font(.largeTitle.bold()).foregroundStyle(Theme.text).multilineTextAlignment(.center)
            let since = info.firstYear > 0 ? L10n.format("discover.person.since", info.firstYear) : ""
            Text((info.roles.map(roleWord) + [since]).filter { !$0.isEmpty }.joined(separator: " · "))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
            HStack(spacing: Theme.Spacing.small) {
                Button(L10n.tr("discover.person.filmography"), action: filmography).buttonStyle(.borderedProminent)
                if !info.biography.isEmpty || !info.birthday.isEmpty {
                    Button(L10n.tr("discover.person.about"), action: about).buttonStyle(.bordered)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(Theme.Spacing.screen)
    }
}

private struct FilmographyBar: View {
    let info: PersonPageInfo
    let select: (FilmRoleID, FilmSortID) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("discover.person.filmography")).padding(.horizontal, Theme.Spacing.screen)
            HStack(spacing: Theme.Spacing.small) {
                Picker(L10n.tr("discover.person.filmography"), selection: Binding(get: { info.role }, set: { select($0, info.sort) })) {
                    ForEach(FilmRoleID.allCases) { Text(roleText($0)).tag($0) }
                }
                .pickerStyle(.segmented)
                Menu {
                    Picker(L10n.tr("discover.person.sort"), selection: Binding(get: { info.sort }, set: { select(info.role, $0) })) {
                        ForEach(FilmSortID.allCases) { Text(sortText($0)).tag($0) }
                    }
                } label: {
                    Label(sortText(info.sort), systemImage: "arrow.up.arrow.down").font(.subheadline.weight(.semibold))
                }
                .accessibilityLabel(L10n.tr("discover.person.sort"))
            }
            .padding(.horizontal, Theme.Spacing.screen)
            if info.reloading {
                ProgressView().frame(maxWidth: .infinity)
            }
        }
    }

    private func roleText(_ role: FilmRoleID) -> String {
        switch role {
        case .all: L10n.tr("discover.type.all")
        case .actor: L10n.tr("discover.role.actor")
        case .director: L10n.tr("discover.role.director")
        }
    }

    private func sortText(_ sort: FilmSortID) -> String {
        switch sort {
        case .oldest: L10n.tr("discover.sort.oldest")
        case .newest: L10n.tr("discover.sort.newest")
        case .popular: L10n.tr("discover.sort.popular")
        }
    }
}

private struct PersonAboutCard: View {
    let info: PersonPageInfo
    let open: () -> Void

    var body: some View {
        Button(action: open) {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(L10n.tr("discover.person.about")).font(.title3.weight(.semibold)).foregroundStyle(Theme.text)
                if info.films > 0 {
                    Text(L10n.plural("discover.person.films", info.films)).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                Text(info.biography).font(.callout).foregroundStyle(Theme.text).lineLimit(4).multilineTextAlignment(.leading)
                Text(L10n.format("discover.person.more.about", String(info.name.split(separator: " ").first ?? "")))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.accentText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.large)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.panel, style: .continuous))
        }
        .buttonStyle(.plain)
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct AboutSheet: View {
    let info: PersonPageInfo
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if let born = date(info.birthday) {
                    let died = date(info.deathday)
                    let years = Calendar(identifier: .gregorian).dateComponents([.year], from: born, to: died ?? Date()).year ?? 0
                    LabeledContent(
                        L10n.tr("discover.person.born"),
                        value: born.formatted(date: .long, time: .omitted) + " · "
                            + (died == nil ? L10n.format("discover.person.age", years) : L10n.format("discover.person.aged", years))
                    )
                }
                if !info.birthplace.isEmpty {
                    LabeledContent(L10n.tr("discover.person.from"), value: info.birthplace)
                }
                if let died = date(info.deathday) {
                    LabeledContent(L10n.tr("discover.person.died"), value: died.formatted(date: .long, time: .omitted))
                }
                if !info.department.isEmpty {
                    LabeledContent(L10n.tr("discover.person.known.for"), value: info.department)
                }
                if !info.biography.isEmpty {
                    Section(L10n.tr("discover.person.story")) {
                        Text(info.biography).font(.body)
                    }
                }
            }
            .navigationTitle(L10n.format("discover.person.about.name", info.name))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("discover.done")) { dismiss() }
                }
            }
        }
    }

    private func date(_ text: String) -> Date? {
        guard text.count >= 10 else { return nil }
        return Format.date(iso: String(text.prefix(10)) + "T12:00:00Z")
    }
}
