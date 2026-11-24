import SwiftUI

struct TVDiscoverCard: View {
    let card: DiscoverCardInfo
    var wide = false
    var typed = false
    var subtitle: String?
    var badge: String?
    var showsText = true
    var focus: TVFocusKey?
    @Environment(TVNavigator.self) private var navigator

    var body: some View {
        if wide {
            TVBackdropCard(
                title: card.name,
                subtitle: subtitle ?? DiscoverText.line(card, typed: typed),
                image: card.wideImage,
                logo: card.logo,
                mark: card.mark,
                fresh: DiscoverText.fresh(card.fresh),
                badge: badge,
                menu: menu,
                showsText: showsText,
                focus: focus
            ) { navigator.openCard(card) }
        } else {
            TVPosterCard(
                title: card.name,
                subtitle: subtitle ?? DiscoverText.line(card, typed: typed),
                image: card.posterImage,
                mark: card.mark,
                fresh: DiscoverText.fresh(card.fresh),
                badge: badge,
                menu: menu,
                focus: focus
            ) { navigator.openCard(card) }
        }
    }

    private var menu: [TVMenuAction] {
        guard let action = card.action, let title = DiscoverText.action(card) else { return [] }
        return [
            TVMenuAction(id: "action", title: title, systemImage: "plus") {
                switch action {
                case .add: navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb))
                case .ask: navigator.open(.ask(kind: card.kind.wire, tmdb: card.tmdb))
                }
            }
        ]
    }
}

struct TVCardRow: View {
    let row: CardRowInfo
    var title: String?
    var typed = false
    var seeAll: (() -> Void)?
    var focus: FocusState<String?>.Binding?

    var body: some View {
        TVShelf(title: title ?? row.title, subtitle: row.subtitle.isEmpty ? nil : row.subtitle, seeAll: seeAll) {
            ForEach(row.cards) { card in
                TVDiscoverCard(card: card, wide: row.wide, typed: typed, focus: key(card))
            }
        }
    }

    private func key(_ card: DiscoverCardInfo) -> TVFocusKey? {
        focus.map { TVFocusKey(binding: $0, id: TVFocusKey.id(row: row.id, card: card.id)) }
    }
}

extension TVFocusKey {
    static func id(row: String, card: String) -> String { row + "|" + card }
}

struct TVTitleCard: View {
    let card: TitleCardInfo
    let manage: Bool
    @Environment(TVNavigator.self) private var navigator

    var body: some View {
        TVPosterCard(title: card.name, subtitle: subtitle, image: card.poster) {
            navigator.open(route)
        }
    }

    private var route: Route {
        card.titleId > 0 ? .libraryTitle(id: card.titleId, kind: card.kind, tmdb: card.tmdbId) : .title(kind: card.kind, tmdb: card.tmdbId)
    }

    private var subtitle: String? {
        if card.inLibrary || card.titleId > 0 { return manage ? L10n.tr("title.card.in.library") : L10n.tr("title.card.ready") }
        if card.asked { return L10n.tr("title.card.requested") }
        return card.year > 0 ? String(card.year) : nil
    }
}
