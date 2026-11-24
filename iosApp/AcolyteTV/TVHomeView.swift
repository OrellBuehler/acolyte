import SwiftUI

struct TVHomeView: View {
    @State private var store = HomeStore()

    var body: some View {
        let info = store.info
        NavigationStack {
            switch info.load.phase {
            case .loading:
                ProgressView()
                    .accessibilityLabel(L10n.tr("common.loading"))
            case .failed:
                VStack(spacing: 32) {
                    Text(L10n.tr("error.title"))
                        .font(.title2.bold())
                    Text((info.load.error ?? .unknown).localized)
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                    Button(L10n.tr("common.retry")) { Task { await store.refresh() } }
                }
            case .ready:
                let rows = TVHomeView.rows(info)
                if rows.isEmpty {
                    Text(L10n.tr("empty.title"))
                        .font(.title3)
                        .foregroundStyle(Theme.textMuted)
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 48) {
                            ForEach(rows, id: \.row.id) { entry in
                                TVShelf(title: entry.title, cards: entry.row.cards)
                            }
                        }
                        .padding(.vertical, 40)
                    }
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private struct Entry {
        let title: String
        let row: CardRowInfo
    }

    private static func rows(_ info: HomeInfo) -> [Entry] {
        var result: [Entry] = []
        if let arrived = info.arrived {
            result.append(Entry(title: arrivedTitle(info.arrivedTitle), row: arrived))
        }
        for row in info.borrowed {
            result.append(Entry(title: row.title, row: row))
        }
        return result.filter { !$0.row.cards.isEmpty }
    }

    private static func arrivedTitle(_ title: ArrivedTitleID) -> String {
        switch title {
        case .newToday: L10n.tr("home.new.today")
        case .newThisWeek: L10n.tr("home.new.this.week")
        case .justArrived: L10n.tr("home.just.arrived")
        case .recentlyAdded: L10n.tr("home.recently.added")
        }
    }
}

private struct TVShelf: View {
    let title: String
    let cards: [DiscoverCardInfo]

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
                .padding(.horizontal, 80)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 40) {
                    ForEach(cards) { card in
                        TVPosterCard(card: card)
                    }
                }
                .padding(.horizontal, 80)
                .padding(.vertical, 30)
            }
        }
    }
}

private struct TVPosterCard: View {
    let card: DiscoverCardInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {} label: {
                AuthImage(source: card.poster, size: .posterLarge) {
                    Theme.surfaceRaised
                }
                .frame(width: 240, height: 360)
            }
            .buttonStyle(.card)
            Text(card.name)
                .font(.callout.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(1)
                .frame(width: 240, alignment: .leading)
        }
    }
}
