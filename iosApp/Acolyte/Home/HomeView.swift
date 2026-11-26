import SwiftUI

struct HomeView: View {
    let account: AccountInfo
    @State private var store = HomeStore()
    @State private var host = CardHost()
    @Environment(Navigator.self) private var navigator

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                    Greeting(info: info)
                    if info.system {
                        Picker(L10n.tr("home.board.home"), selection: Binding(get: { info.board }, set: { store.show($0) })) {
                            Text(L10n.tr("home.board.home")).tag(HomeBoardID.home)
                            Text(L10n.tr("home.board.system")).tag(HomeBoardID.system)
                        }
                        .pickerStyle(.segmented)
                        .padding(.horizontal, Theme.Spacing.screen)
                    }
                    if let error = info.load.error {
                        NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
                    }
                    if info.board == .system, let board = info.systemBoard {
                        SystemBoardView(board: board, info: info)
                    } else {
                        HomeBoardView(info: info, dismissWelcome: { store.dismissWelcome() })
                    }
                }
                .padding(.bottom, Theme.Spacing.xLarge)
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(SectionID.home.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button(SectionID.search.title, systemImage: "magnifyingglass") { navigator.open(.search) }
                Button(SectionID.explore.title, systemImage: "safari") { navigator.open(.explore) }
            }
        }
        .cardHost(host)
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }
}

private struct Greeting: View {
    let info: HomeInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            let word: String =
                switch info.greeting {
                case .morning: L10n.tr("home.good.morning")
                case .afternoon: L10n.tr("home.good.afternoon")
                case .evening: L10n.tr("home.good.evening")
                }
            Text(info.name.isEmpty ? word : L10n.format("home.greeting.name", word, info.name))
                .font(.largeTitle.bold())
                .foregroundStyle(Theme.text)
            if let news = HomeText.news(info.news) {
                Text(news).font(.callout).foregroundStyle(Theme.textMuted).lineLimit(2)
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.top, Theme.Spacing.small)
    }
}

private struct HomeBoardView: View {
    let info: HomeInfo
    let dismissWelcome: () -> Void
    @Environment(Navigator.self) private var navigator

    var body: some View {
        if info.finishSetup {
            LinkCard(title: L10n.tr("home.finish.setup"), message: L10n.tr("home.finish.setup.body")) { navigator.open(.settings) }
        }
        if let joined = info.joined {
            JoinedWelcomeCard(joined: joined, dismiss: dismissWelcome)
        }
        if info.welcome {
            WelcomeCard(info: info)
        }
        TileGrid(info: info)
        if let row = info.arrived {
            CardRowView(row: row, title: HomeText.arrived(info.arrivedTitle), seeAll: {
                if let kind = row.cards.first?.kind { navigator.open(HomeText.section(kind)) }
            })
        }
        if !info.comingUp.isEmpty {
            let calendar: (() -> Void)? = info.system ? { navigator.open(.calendar) } : nil
            SectionHeader(title: L10n.tr("home.coming.up"), actionTitle: SectionID.calendar.title, action: calendar)
                .padding(.horizontal, Theme.Spacing.screen)
            ForEach(info.comingUp) { up in ComingUpRow(up: up) }
        }
        ForEach(info.borrowed) { row in
            CardRowView(row: row, seeAll: { navigator.open(.explore) })
        }
    }
}

private struct LinkCard: View {
    let title: String
    let message: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(title).font(.headline).foregroundStyle(Theme.text)
                Text(message).font(.callout).foregroundStyle(Theme.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.large)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        }
        .buttonStyle(.plain)
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct JoinedWelcomeCard: View {
    let joined: JoinedWelcomeInfo
    let dismiss: () -> Void
    @Environment(Navigator.self) private var navigator

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(joined.name.isEmpty ? L10n.tr("home.welcome.title") : L10n.format("home.joined.title", joined.name))
                .font(.title2.bold())
                .foregroundStyle(Theme.text)
            Label(L10n.tr("home.joined.find"), systemImage: "magnifyingglass").font(.callout)
            if joined.canAsk {
                Label(L10n.tr("home.joined.ask"), systemImage: "plus").font(.callout)
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(joined.kinds, id: \.self) { StatusChip(text: HomeText.kinds($0), tone: .idle) }
                }
                Label(L10n.tr("home.joined.follow"), systemImage: "checklist").font(.callout)
                if joined.noLimit || joined.limits.isEmpty {
                    Label(L10n.tr("home.joined.no.limit"), systemImage: "clock").font(.footnote)
                } else {
                    ForEach(joined.limits, id: \.self) { limit in
                        Label(
                            limit.noLimit
                                ? L10n.format("home.limit.none", HomeText.kinds(limit.kind))
                                : L10n.format("home.limit.line", HomeText.kinds(limit.kind), limit.perWeek, limit.left),
                            systemImage: "clock"
                        )
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    }
                }
            }
            HStack(spacing: Theme.Spacing.small) {
                Button(L10n.tr("home.joined.got.it"), action: dismiss).buttonStyle(.borderedProminent)
                Button(L10n.tr("home.joined.open.explore"), systemImage: "safari") { navigator.open(.explore) }.buttonStyle(.bordered)
            }
        }
        .foregroundStyle(Theme.text)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.large)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct WelcomeCard: View {
    let info: HomeInfo
    @Environment(Navigator.self) private var navigator

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.owner, !info.steps.isEmpty {
                Text(L10n.format("home.setup.done", info.stepsDone, info.steps.count))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.accentText)
                Text(L10n.tr("home.welcome.title")).font(.title2.bold()).foregroundStyle(Theme.text)
                ProgressView(value: Double(info.stepsDone), total: Double(max(info.steps.count, 1))).tint(Theme.accent)
                ForEach(Array(info.steps.enumerated()), id: \.element.id) { index, step in
                    Button {
                        if let target = step.target { navigator.open(target) }
                    } label: {
                        HStack(alignment: .top) {
                            Image(systemName: step.done ? "checkmark.circle.fill" : "\(index + 1).circle")
                                .foregroundStyle(step.done ? Theme.ok : Theme.textMuted)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(HomeText.stepTitle(step.kind)).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
                                Text(HomeText.stepDetail(step.kind)).font(.footnote).foregroundStyle(Theme.textMuted)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
                if let next = info.nextStep {
                    Button(HomeText.stepAction(next.kind)) {
                        if let target = next.target { navigator.open(target) }
                    }
                    .buttonStyle(.borderedProminent)
                }
            } else {
                Text(L10n.tr("home.welcome.title")).font(.title2.bold()).foregroundStyle(Theme.text)
                Text(L10n.tr("home.welcome.body")).font(.callout).foregroundStyle(Theme.textMuted)
                Button(L10n.tr("home.welcome.start")) { navigator.open(.explore) }.buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.large)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct TileGrid: View {
    let info: HomeInfo

    var body: some View {
        VStack(spacing: Theme.Spacing.medium) {
            if let first = info.pictures.first {
                PictureTile(picture: first).aspectRatio(16 / 9, contentMode: .fit)
            }
            let pairs = stride(from: 0, to: info.tiles.count, by: 2).map { Array(info.tiles[$0 ..< min($0 + 2, info.tiles.count)]) }
            let rest = Array(info.pictures.dropFirst())
            ForEach(Array(pairs.enumerated()), id: \.offset) { index, pair in
                HStack(alignment: .top, spacing: Theme.Spacing.medium) {
                    ForEach(pair) { StatTile(tile: $0, info: info) }
                    if pair.count == 1 { Color.clear.frame(maxWidth: .infinity) }
                }
                if index < rest.count {
                    PictureTile(picture: rest[index]).aspectRatio(2, contentMode: .fit)
                }
            }
            ForEach(rest.dropFirst(pairs.count)) { PictureTile(picture: $0).aspectRatio(2, contentMode: .fit) }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct PictureTile: View {
    let picture: HomePictureInfo
    @Environment(Navigator.self) private var navigator

    var body: some View {
        let card = picture.card
        Button {
            if card.opensTitle { navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb)) }
        } label: {
            ZStack(alignment: .bottomLeading) {
                Theme.surfaceRaised
                Color.clear.overlay { AuthImage(source: card.backdrop ?? card.poster, size: .hero) { ArtPlaceholderView(name: card.name) } }
                    .clipped()
                LinearGradient(colors: [.clear, Theme.artScrimStrong], startPoint: UnitPoint(x: 0.5, y: 0.4), endPoint: .bottom)
                VStack(alignment: .leading, spacing: 2) {
                    if let logo = card.logo {
                        AuthImage(source: logo, size: .poster) { Text(card.name).font(.title3.bold()).foregroundStyle(Theme.onArt) }
                            .scaledToFit()
                            .frame(maxWidth: 180, maxHeight: 44, alignment: .leading)
                    } else {
                        Text(card.name).font(.title3.bold()).foregroundStyle(Theme.onArt).lineLimit(2)
                    }
                    if !card.subtitle.isEmpty {
                        Text(DiscoverText.serverWords(card.subtitle)).font(.footnote).foregroundStyle(Theme.onArt.opacity(0.84))
                    }
                }
                .padding(Theme.Spacing.large)
            }
            .overlay(alignment: .topLeading) {
                Text(HomeText.picture(picture))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.onArt)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Theme.artScrim, in: Capsule())
                    .padding(Theme.Spacing.medium)
            }
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.panel, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct StatTile: View {
    let tile: HomeTileInfo
    let info: HomeInfo
    @Environment(Navigator.self) private var navigator

    var body: some View {
        let content = HomeText.tile(tile, info: info)
        Button {
            if let target = tile.target { navigator.open(target) }
        } label: {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Label(content.label, systemImage: content.symbol)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                Text(content.big)
                    .font(.title2.bold())
                    .foregroundStyle(content.tone.map(Theme.textColor) ?? Theme.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(content.small).font(.footnote).foregroundStyle(Theme.textMuted).lineLimit(3)
                if !content.posters.isEmpty {
                    HStack(spacing: 4) {
                        ForEach(content.posters, id: \.self) { poster in
                            AuthImage(source: poster, size: .thumb) { Theme.surfaceHigh }
                                .frame(width: 28, height: 42)
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, minHeight: 120, alignment: .topLeading)
            .padding(Theme.Spacing.large)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(tile.target == nil)
    }
}

private struct ComingUpRow: View {
    let up: ComingUpInfo
    @Environment(Navigator.self) private var navigator

    var body: some View {
        Button {
            if !up.tmdb.isEmpty { navigator.open(.title(kind: up.kind, tmdb: up.tmdb)) }
        } label: {
            HStack(spacing: Theme.Spacing.medium) {
                AuthImage(source: up.image, size: .backdrop) { ArtPlaceholderView(name: up.name) }
                    .frame(width: 112, height: 63)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 2) {
                    Text(up.name).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text).lineLimit(1)
                    if !up.sub.isEmpty {
                        Text(up.sub).font(.footnote).foregroundStyle(Theme.textMuted).lineLimit(1)
                    }
                }
                Spacer(minLength: 0)
                Text(HomeText.when(up))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(up.when == .outNow || up.when == .today ? Theme.accentText : Theme.textMuted)
            }
        }
        .buttonStyle(.plain)
        .padding(.horizontal, Theme.Spacing.screen)
    }
}
