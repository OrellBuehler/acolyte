import SwiftUI

struct TVPersonView: View {
    @State private var store: PersonStore
    @State private var about = false
    @Environment(TVNavigator.self) private var navigator

    init(id: String) {
        _store = State(initialValue: PersonStore(id: id))
    }

    var body: some View {
        let info = store.info
        let rows = TVPersonView.rows(info)
        ZStack {
            Theme.background.ignoresSafeArea()
            TVStatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                EmptyView()
            } content: {
                TVAmbientPage(lookup: TVFocusLookup.images(rows), fallback: info.photo) { focus in
                    header(info)
                    if let used = info.limitUsed, let left = info.limitLeft {
                        TVNoticeBanner(text: L10n.format("discover.person.limit", used, left))
                    }
                    ForEach(rows) { row in
                        if row.id == "filmography" {
                            filmography(info)
                        }
                        if !row.cards.isEmpty {
                            TVCardRow(row: row, focus: focus)
                        }
                    }
                    if info.decades.isEmpty, !info.reloading {
                        TVEmptyView(title: L10n.tr("discover.person.no.movies"), systemImage: "film")
                    }
                    if info.oftenWith.count >= 4 {
                        TVShelf(title: L10n.tr("discover.person.often")) {
                            ForEach(info.oftenWith) { person in
                                TVPersonCard(name: person.name, role: person.note, photo: person.photo) {
                                    navigator.open(.person(id: person.id))
                                }
                            }
                        }
                    }
                    if !info.attribution.isEmpty {
                        Text(info.attribution)
                            .font(.caption)
                            .foregroundStyle(Theme.textFaint)
                    }
                }
            }
        }
        .fullScreenCover(isPresented: $about) { TVPersonAbout(info: store.info) }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.refreshOwned() }
    }

    private func header(_ info: PersonPageInfo) -> some View {
        HStack(alignment: .top, spacing: 48) {
            AuthImage(source: info.photo, size: .poster) { TVArtPlaceholder(name: info.name) }
                .frame(width: 240, height: 240)
                .clipShape(Circle())
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 16) {
                Text(info.name)
                    .font(.title2.bold())
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                    .accessibilityAddTraits(.isHeader)
                let since = info.firstYear > 0 ? L10n.format("discover.person.since", info.firstYear) : ""
                Text((info.roles.map(TVPersonView.roleWord) + [since]).filter { !$0.isEmpty }.joined(separator: " · "))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
                if !info.biography.isEmpty {
                    Text(info.biography)
                        .font(.footnote)
                        .foregroundStyle(Theme.text)
                        .lineLimit(3)
                        .frame(maxWidth: Theme.TV.readableWidth, alignment: .leading)
                }
                if !info.biography.isEmpty || !info.birthday.isEmpty {
                    Button(L10n.tr("discover.person.about")) { about = true }
                        .buttonStyle(.bordered)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, Theme.TV.focusPad)
    }

    private func filmography(_ info: PersonPageInfo) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(L10n.tr("discover.person.filmography"))
                .font(.headline)
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            TVChipRow {
                ForEach(FilmRoleID.allCases) { role in
                    TVChip(title: TVPersonView.roleText(role), selected: role == info.role) {
                        store.select(role: role, sort: info.sort)
                    }
                }
            }
            TVChipRow {
                ForEach(FilmSortID.allCases) { sort in
                    TVChip(title: TVPersonView.sortText(sort), selected: sort == info.sort) {
                        store.select(role: info.role, sort: sort)
                    }
                }
            }
        }
    }

    static func rows(_ info: PersonPageInfo) -> [CardRowInfo] {
        var result: [CardRowInfo] = []
        let groups: [(String, String, [DiscoverCardInfo])] = [
            ("known", L10n.tr("discover.person.known.for"), info.knownFor),
            ("coming", L10n.tr("discover.person.coming.soon"), info.comingSoon),
            ("library", L10n.tr("discover.search.in.library"), info.inLibrary),
            ("asked", L10n.tr("discover.person.asked"), info.asked),
        ]
        for group in groups where !group.2.isEmpty {
            result.append(CardRowInfo.local(id: group.0, title: group.1, cards: group.2))
        }
        result.append(CardRowInfo.local(id: "filmography", title: "", cards: []))
        for decade in info.decades {
            let title = decade.year == 0 ? L10n.tr("discover.person.coming.soon") : L10n.format("discover.person.decade", decade.year)
            result.append(CardRowInfo.local(id: "decade-\(decade.year)", title: title, cards: decade.cards))
        }
        return result
    }

    static func roleWord(_ role: String) -> String {
        switch role {
        case "Actor": L10n.tr("discover.role.actor")
        case "Director": L10n.tr("discover.role.director")
        default: role
        }
    }

    static func roleText(_ role: FilmRoleID) -> String {
        switch role {
        case .all: L10n.tr("discover.type.all")
        case .actor: L10n.tr("discover.role.actor")
        case .director: L10n.tr("discover.role.director")
        }
    }

    static func sortText(_ sort: FilmSortID) -> String {
        switch sort {
        case .oldest: L10n.tr("discover.sort.oldest")
        case .newest: L10n.tr("discover.sort.newest")
        case .popular: L10n.tr("discover.sort.popular")
        }
    }
}

private struct TVPersonAbout: View {
    let info: PersonPageInfo
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text(L10n.format("discover.person.about.name", info.name))
                    .font(.title2.bold())
                    .foregroundStyle(Theme.text)
                    .accessibilityAddTraits(.isHeader)
                Button(L10n.tr("discover.done")) { dismiss() }
                    .buttonStyle(.borderedProminent)
                ForEach(facts, id: \.0) { fact in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(fact.0).font(.footnote).foregroundStyle(Theme.textMuted)
                        Text(fact.1).font(.callout).foregroundStyle(Theme.text)
                    }
                }
                if !info.biography.isEmpty {
                    Text(L10n.tr("discover.person.story")).font(.headline).foregroundStyle(Theme.text)
                    Text(info.biography)
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                        .focusable()
                }
            }
            .frame(maxWidth: Theme.TV.readableWidth, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.background.ignoresSafeArea())
    }

    private var facts: [(String, String)] {
        var result: [(String, String)] = []
        if let born = date(info.birthday) {
            let died = date(info.deathday)
            let years = Calendar(identifier: .gregorian).dateComponents([.year], from: born, to: died ?? Date()).year ?? 0
            let age = died == nil ? L10n.format("discover.person.age", years) : L10n.format("discover.person.aged", years)
            result.append((L10n.tr("discover.person.born"), born.formatted(date: .long, time: .omitted) + " · " + age))
        }
        if !info.birthplace.isEmpty {
            result.append((L10n.tr("discover.person.from"), info.birthplace))
        }
        if let died = date(info.deathday) {
            result.append((L10n.tr("discover.person.died"), died.formatted(date: .long, time: .omitted)))
        }
        if !info.department.isEmpty {
            result.append((L10n.tr("discover.person.known.for"), info.department))
        }
        return result
    }

    private func date(_ text: String) -> Date? {
        guard text.count >= 10 else { return nil }
        return Format.date(iso: String(text.prefix(10)) + "T12:00:00Z")
    }
}
