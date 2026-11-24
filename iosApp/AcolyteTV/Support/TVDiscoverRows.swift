import SwiftUI

struct TVDiscoverRows: View {
    let rows: [CardRowInfo]
    let top: TopServicesInfo?
    var genres: [GenreCardInfo] = []
    let type: ExploreTypeID
    var topGenre: String?
    var seeAllNew: (() -> Void)?
    let pickService: (Int64) -> Void
    let focus: FocusState<String?>.Binding
    @Environment(TVNavigator.self) private var navigator

    var body: some View {
        ForEach(rows) { row in
            switch row.type {
            case "top_services":
                if let top {
                    TVChipShelf(
                        title: DiscoverText.topTitle(type, genre: topGenre),
                        subtitle: top.serviceName.isEmpty ? nil : DiscoverText.topSource(top),
                        note: top.attribution,
                        chips: top.chips,
                        selected: top.selected,
                        pick: pickService,
                        row: row,
                        ranked: top.ranked,
                        focus: focus
                    )
                }
            case "genre_browse" where !genres.isEmpty:
                TVShelf(title: L10n.tr("discover.browse.by.genre")) {
                    ForEach(genres) { genre in
                        TVBrowseTile(name: DiscoverText.genre(genre.name), image: genre.backdrop, hue: TVGenreHue.of(genre.name)) {
                            navigator.open(.genre(type: genre.type, genre: genre.genre))
                        }
                    }
                }
            case "genre_browse":
                EmptyView()
            case "np_services":
                TVCardRow(row: row, title: L10n.tr("discover.new.on.services"), seeAll: seeAllNew, focus: focus)
            default:
                TVCardRow(row: row, typed: type == .all, focus: focus)
            }
        }
    }
}

struct TVServiceChips: View {
    let chips: [ServiceChipInfo]
    let selected: Int64
    var showAll = false
    let pick: (Int64) -> Void

    var body: some View {
        TVChipRow {
            if showAll {
                TVChip(title: L10n.tr("discover.type.all"), selected: selected == 0) { pick(0) }
            }
            ForEach(chips) { item in
                TVChip(title: item.name, selected: item.id == selected) { pick(item.id) }
            }
        }
    }
}

struct TVChipShelf: View {
    let title: String
    var subtitle: String?
    var note: String?
    var chips: [ServiceChipInfo] = []
    var selected: Int64 = 0
    var showAll = false
    var pick: ((Int64) -> Void)?
    let row: CardRowInfo
    var ranked = false
    var wide = false
    var loading = false
    let focus: FocusState<String?>.Binding

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.headline)
                .foregroundStyle(Theme.text)
                .lineLimit(1)
                .accessibilityAddTraits(.isHeader)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
            if chips.count > 1, let pick {
                TVServiceChips(chips: chips, selected: selected, showAll: showAll, pick: pick)
            }
            if loading {
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: Theme.TV.backdropHeight)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: Theme.TV.gutter) {
                        ForEach(Array(row.cards.enumerated()), id: \.element.id) { index, card in
                            TVDiscoverCard(
                                card: card,
                                wide: wide,
                                badge: ranked ? "#\(card.rank > 0 ? card.rank : index + 1)" : nil,
                                focus: TVFocusKey(binding: focus, id: TVFocusKey.id(row: row.id, card: card.id))
                            )
                        }
                    }
                    .padding(.vertical, Theme.TV.focusPad)
                }
                .scrollClipDisabled()
                .focusSection()
            }
            if let note, !note.isEmpty {
                Text(note)
                    .font(.caption)
                    .foregroundStyle(Theme.textFaint)
            }
        }
    }
}
