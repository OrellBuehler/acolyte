import SwiftUI

struct TVHomeView: View {
    @State private var store = HomeStore()
    @Environment(\.scenePhase) private var scenePhase
    @Environment(TVNavigator.self) private var navigator

    var body: some View {
        let info = store.info
        let hero = info.pictures.first
        let rest = Array(info.pictures.dropFirst())
        let rows = TVHomeView.rows(info)
        TVStatusContent(
            load: info.load,
            isEmpty: hero == nil && rows.isEmpty && info.comingUp.isEmpty,
            reload: { await store.refresh() }
        ) {
            TVEmptyView()
        } content: {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Theme.TV.sectionGap) {
                    TVGreeting(info: info)
                    if let error = info.load.error {
                        TVNoticeBanner(text: error.localized)
                    }
                    if let hero {
                        TVHeroCard(picture: hero) { navigator.openCard(hero.card) }
                    }
                    if !rest.isEmpty {
                        TVShelf(title: L10n.tr("tv.home.featured")) {
                            ForEach(rest) { picture in
                                TVBackdropCard(
                                    title: picture.card.name,
                                    subtitle: HomeText.picture(picture),
                                    image: picture.card.wideImage,
                                    logo: picture.card.logo,
                                    mark: picture.card.mark
                                ) { navigator.openCard(picture.card) }
                            }
                        }
                    }
                    ForEach(rows, id: \.row.id) { entry in
                        TVCardRow(row: entry.row, title: entry.title, seeAll: seeAll(entry.row))
                    }
                    if !info.comingUp.isEmpty {
                        TVShelf(title: L10n.tr("home.coming.up"), seeAll: calendarAction) {
                            ForEach(info.comingUp) { up in
                                TVBackdropCard(title: up.name, subtitle: [up.sub, HomeText.when(up)].filter { !$0.isEmpty }.joined(separator: " · "), image: up.image) {
                                    if !up.tmdb.isEmpty { navigator.open(.title(kind: up.kind, tmdb: up.tmdb)) }
                                }
                            }
                        }
                    }
                }
                .padding(.vertical, Theme.TV.focusPad)
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await store.refresh() } }
        }
    }

    private var calendarAction: (() -> Void)? {
        TVSections.visible(.calendar, for: navigator.account) ? { navigator.open(.calendar) } : nil
    }

    private func seeAll(_ row: CardRowInfo) -> (() -> Void)? {
        row.seeAll && TVSections.visible(.explore, for: navigator.account) ? { navigator.open(.explore) } : nil
    }

    struct Entry {
        let title: String
        let row: CardRowInfo
    }

    static func rows(_ info: HomeInfo) -> [Entry] {
        var result: [Entry] = []
        if let arrived = info.arrived {
            result.append(Entry(title: HomeText.arrived(info.arrivedTitle), row: arrived))
        }
        for row in info.borrowed {
            result.append(Entry(title: row.title, row: row))
        }
        return result.filter { !$0.row.cards.isEmpty }
    }
}

private struct TVGreeting: View {
    let info: HomeInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            let word: String =
                switch info.greeting {
                case .morning: L10n.tr("home.good.morning")
                case .afternoon: L10n.tr("home.good.afternoon")
                case .evening: L10n.tr("home.good.evening")
                }
            Text(info.name.isEmpty ? word : L10n.format("home.greeting.name", word, info.name))
                .font(.title2.bold())
                .foregroundStyle(Theme.text)
            if let news = HomeText.news(info.news) {
                Text(news)
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(2)
            }
        }
    }
}

private struct TVHeroCard: View {
    let picture: HomePictureInfo
    let action: () -> Void

    var body: some View {
        let card = picture.card
        Button(action: action) {
            ZStack(alignment: .bottomLeading) {
                Theme.surfaceRaised
                AuthImage(source: card.backdrop ?? card.poster, size: .hero) { TVArtPlaceholder(name: card.name) }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                LinearGradient(colors: [.clear, Theme.artScrimStrong], startPoint: UnitPoint(x: 0.5, y: 0.35), endPoint: .bottom)
                VStack(alignment: .leading, spacing: 10) {
                    Text(HomeText.picture(picture))
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Theme.onArt)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 6)
                        .background(Theme.artScrim, in: Capsule())
                    if let logo = card.logo {
                        AuthImage(source: logo, size: .poster) {
                            Text(card.name).font(.title2.bold()).foregroundStyle(Theme.onArt)
                        }
                        .scaledToFit()
                        .frame(maxWidth: 420, maxHeight: 110, alignment: .leading)
                    } else {
                        Text(card.name)
                            .font(.title2.bold())
                            .foregroundStyle(Theme.onArt)
                            .lineLimit(2)
                    }
                    if !card.subtitle.isEmpty {
                        Text(DiscoverText.serverWords(card.subtitle))
                            .font(.callout)
                            .foregroundStyle(Theme.onArt.opacity(0.85))
                            .lineLimit(1)
                    }
                }
                .padding(40)
            }
            .frame(maxWidth: .infinity)
            .frame(height: Theme.TV.heroHeight)
        }
        .buttonStyle(.card)
        .accessibilityLabel(card.name)
    }
}
