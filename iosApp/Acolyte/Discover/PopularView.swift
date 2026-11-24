import SwiftUI

struct PopularView: View {
    let account: AccountInfo
    @State private var store = PopularStore()
    @State private var host = CardHost()
    @Environment(Navigator.self) private var navigator

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            ContentUnavailableView {
                Label(L10n.tr("discover.popular.building.title"), systemImage: "sparkles")
            } description: {
                Text(L10n.tr("discover.explore.building.message"))
            } actions: {
                Button(SectionID.explore.title) { navigator.open(.explore) }
                    .buttonStyle(.borderedProminent)
            }
        } content: {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    TypePicker(type: store.info.type, select: { store.select($0) })
                    if let error = store.info.load.error {
                        NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
                    }
                    if store.info.sourceOff {
                        NoticeBanner(text: L10n.tr("discover.source.off")).padding(.horizontal, Theme.Spacing.screen)
                    }
                    if let hero = store.info.hero {
                        HeroView(
                            card: hero,
                            pills: store.info.heroPills.map { DiscoverText.pill($0, rank: store.info.heroRank, service: store.info.heroService) }
                        )
                    }
                    if !store.info.days.isEmpty {
                        Text(L10n.tr("discover.this.week"))
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(Theme.text)
                            .padding(.horizontal, Theme.Spacing.screen)
                            .accessibilityAddTraits(.isHeader)
                        ForEach(store.info.days) { day in
                            CardShelf(title: "\(dayWord(day)) · \(DiscoverText.shortDate(day.day))") {
                                ForEach(day.entries) { entry in
                                    TitleWideCard(card: entry.card, subtitle: what(entry))
                                }
                            }
                        }
                    }
                    ForEach(store.info.rows) { row in
                        PopularRowView(row: row, pick: { store.selectService(row: row.row.id, service: $0) })
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
        .navigationTitle(SectionID.popular.title)
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

    private func dayWord(_ day: RailDayInfo) -> String {
        switch day.offset {
        case 0: L10n.tr("discover.day.today")
        case -1: L10n.tr("discover.day.yesterday")
        case 1: L10n.tr("discover.day.tomorrow")
        default: DiscoverText.weekday(day.day)
        }
    }

    private func what(_ entry: RailEntryInfo) -> String {
        let base: String =
            switch entry.what {
            case .onService: L10n.format("discover.on.service", entry.text)
            case .out: L10n.tr("discover.what.out")
            case .coming: L10n.tr("discover.what.coming")
            case .other: entry.text
            }
        return entry.alsoComing ? "\(base) · \(L10n.tr("discover.what.coming"))" : base
    }
}

private struct PopularRowView: View {
    let row: PopularRowInfo
    let pick: (Int64) -> Void

    var body: some View {
        CardShelf(title: row.row.title, subtitle: row.row.subtitle) {
            if !row.chips.isEmpty {
                ServiceChipsView(chips: row.chips, selected: row.selected, showAll: true, pick: pick)
            }
        } content: {
            if row.loading {
                ProgressView().padding(Theme.Spacing.large)
            } else {
                ForEach(Array(row.row.cards.enumerated()), id: \.element.id) { index, card in
                    if row.row.ranked {
                        RankedPoster(rank: card.rank > 0 ? card.rank : index + 1) { TitlePosterCard(card: card) }
                    } else {
                        TitleWideCard(card: card, subtitle: card.streaming.isEmpty ? nil : L10n.format("discover.on.service", card.streaming))
                    }
                }
            }
        }
    }
}
