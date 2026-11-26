import SwiftUI

struct TVPopularView: View {
    let account: AccountInfo
    @State private var store = PopularStore()

    var body: some View {
        let info = store.info
        ZStack {
            Theme.background.ignoresSafeArea()
            TVStatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
                TVEmptyView(
                    title: L10n.tr("discover.popular.building.title"),
                    message: L10n.tr("discover.explore.building.message"),
                    systemImage: "sparkles"
                )
            } content: {
                TVBrowsePage(
                    hero: info.hero,
                    pills: info.heroPills.map { DiscoverText.pill($0, rank: info.heroRank, service: info.heroService) },
                    lookup: lookup(info),
                    initialFocus: info.days.first.flatMap { day in day.entries.first.map { TVFocusKey.id(row: "day-" + day.day, card: $0.card.id) } }
                ) {
                    header(info)
                } content: { focus in
                    if !info.days.isEmpty {
                        Text(L10n.tr("discover.this.week"))
                            .font(.title2.bold())
                            .foregroundStyle(Theme.text)
                            .accessibilityAddTraits(.isHeader)
                        ForEach(info.days) { day in
                            TVShelf(title: "\(dayWord(day)) · \(DiscoverText.shortDate(day.day))") {
                                ForEach(day.entries) { entry in
                                    TVDiscoverCard(
                                        card: entry.card,
                                        wide: true,
                                        subtitle: what(entry),
                                        focus: TVFocusKey(binding: focus, id: TVFocusKey.id(row: "day-" + day.day, card: entry.card.id))
                                    )
                                }
                            }
                        }
                    }
                    ForEach(info.rows) { item in
                        TVChipShelf(
                            title: item.row.title,
                            subtitle: item.row.subtitle,
                            chips: item.chips,
                            selected: item.selected,
                            showAll: true,
                            pick: { store.selectService(row: item.row.id, service: $0) },
                            row: item.row,
                            ranked: item.row.ranked,
                            wide: !item.row.ranked,
                            loading: item.loading,
                            focus: focus
                        )
                    }
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
    }

    private func header(_ info: PopularInfo) -> some View {
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

    private func lookup(_ info: PopularInfo) -> [String: DiscoverCardInfo] {
        var result = TVFocusLookup.cards(info.rows.map(\.row))
        for day in info.days {
            for entry in day.entries {
                result[TVFocusKey.id(row: "day-" + day.day, card: entry.card.id)] = entry.card
            }
        }
        return result
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
