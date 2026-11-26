import SwiftUI

struct OwnedMarkView: View {
    let mark: CardMarkInfo

    var body: some View {
        ZStack {
            Circle().fill(fill)
            Circle().strokeBorder(Theme.markRing, lineWidth: 1)
            if mark.state == .downloading, mark.percent >= 0 {
                Circle()
                    .trim(from: 0, to: CGFloat(mark.percent) / 100)
                    .stroke(Theme.onArt, style: StrokeStyle(lineWidth: 2, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .padding(2)
            }
            Image(systemName: symbol)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(mark.state == .requested ? Theme.markAskedGlyph : Theme.onArt)
        }
        .frame(width: 24, height: 24)
        .accessibilityLabel(DiscoverText.mark(mark))
    }

    private var fill: Color {
        switch mark.state {
        case .available, .partial: Theme.markHave
        case .downloading: Theme.markDownloading
        case .requested: Theme.markAsked
        case .declined: Theme.markNo
        }
    }

    private var symbol: String {
        switch mark.state {
        case .available: "checkmark"
        case .partial: "minus"
        case .downloading: "arrow.down"
        case .requested: "clock"
        case .declined: "xmark"
        }
    }
}

struct FreshTabView: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(Theme.onAccent)
            .lineLimit(1)
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(Theme.accentFill, in: UnevenRoundedRectangle(topLeadingRadius: 6, topTrailingRadius: 6))
    }
}

struct CardArtView: View {
    let image: String?
    let name: String
    var size: ImageSizeKind = .backdrop
    var logo: String?
    var mark: CardMarkInfo?
    var fresh: String?

    var body: some View {
        Color.clear
            .aspectRatio(16 / 9, contentMode: .fit)
            .overlay {
                AuthImage(source: image, size: size) { ArtPlaceholderView(name: name) }
            }
            .overlay(alignment: .bottomLeading) {
                if let logo {
                    ZStack(alignment: .bottomLeading) {
                        LinearGradient(colors: [.clear, Theme.artScrim], startPoint: .center, endPoint: .bottom)
                        AuthImage(source: logo, size: .poster) { Color.clear }
                            .scaledToFit()
                            .frame(maxWidth: 140, maxHeight: 36, alignment: .leading)
                            .padding(10)
                    }
                }
            }
            .overlay(alignment: .topTrailing) {
                if let mark { OwnedMarkView(mark: mark).padding(8) }
            }
            .overlay(alignment: .bottom) {
                if let fresh { FreshTabView(text: fresh) }
            }
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}

@MainActor
@Observable
final class CardHost {
    var hiding: HideTarget?
    var toast: HideDoneInfo?
    var canHide = false
    var onChanged: () -> Void = {}
    var onOpen: (String) -> Void = { _ in }
    var onAct: (String) -> Void = { _ in }
    private let undoDriver = SDKClient.shared.makeHidden()

    func undo(_ done: HideDoneInfo) {
        toast = nil
        let driver = undoDriver
        Task {
            await driver.showAgain(done.id)
            self.onChanged()
        }
    }
}

struct CardActionsModifier: ViewModifier {
    let card: DiscoverCardInfo
    @Environment(Navigator.self) private var navigator
    @Environment(CardHost.self) private var host: CardHost?

    func body(content: Content) -> some View {
        content
            .contextMenu {
                if let action = card.action {
                    Button(DiscoverText.actionShort(action), systemImage: "plus") { act(card, action) }
                }
                if card.opensTitle {
                    Button(L10n.tr("discover.more.info"), systemImage: "info.circle") { open(card) }
                }
                if host?.canHide == true, card.opensTitle {
                    Button(L10n.tr("discover.hide.menu"), systemImage: "eye.slash") {
                        host?.hiding = HideTarget(kind: card.kind, tmdb: card.tmdb, name: card.name, genres: card.genres)
                    }
                }
            } preview: {
                CardPreview(card: card)
            }
    }

    private func open(_ card: DiscoverCardInfo) {
        host?.onOpen(card.rowType)
        navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb))
    }

    private func act(_ card: DiscoverCardInfo, _ action: CardActionID) {
        host?.onAct(card.rowType)
        switch action {
        case .add: navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb))
        case .ask: navigator.open(.ask(kind: card.kind.wire, tmdb: card.tmdb))
        }
    }
}

struct CardPreview: View {
    let card: DiscoverCardInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            CardArtView(image: card.wideImage, name: card.name, size: .hero, logo: card.logo, mark: card.mark)
            Text(card.name).font(.headline).foregroundStyle(Theme.text)
            let facts = [DiscoverText.kind(card.kind), card.year > 0 ? String(card.year) : ""] + card.genres.prefix(3).map(DiscoverText.genre)
            Text(facts.filter { !$0.isEmpty }.joined(separator: " · "))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if let context = DiscoverText.context(card) {
                Text(context).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.accentText)
            }
            if !card.overview.isEmpty {
                Text(card.overview).font(.callout).foregroundStyle(Theme.text).lineLimit(6)
            }
        }
        .padding(Theme.Spacing.large)
        .frame(width: 320)
        .background(Theme.surface)
    }
}

extension View {
    func cardActions(_ card: DiscoverCardInfo) -> some View {
        modifier(CardActionsModifier(card: card))
    }
}

struct QuickActionButton: View {
    let card: DiscoverCardInfo
    @Environment(Navigator.self) private var navigator
    @Environment(CardHost.self) private var host: CardHost?

    var body: some View {
        if let action = card.action, let label = DiscoverText.action(card) {
            Button {
                host?.onAct(card.rowType)
                switch action {
                case .add: navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb))
                case .ask: navigator.open(.ask(kind: card.kind.wire, tmdb: card.tmdb))
                }
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.onArt)
                    .frame(width: 36, height: 36)
                    .background(Theme.artScrimStrong, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
            .padding(6)
        }
    }
}

struct TitleWideCard: View {
    let card: DiscoverCardInfo
    var typed = false
    var subtitle: String?
    var width: CGFloat? = 264
    var fallback: (() -> Void)?
    @Environment(Navigator.self) private var navigator
    @Environment(CardHost.self) private var host: CardHost?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .bottomTrailing) {
                Button(action: open) {
                    CardArtView(
                        image: card.wideImage,
                        name: card.name,
                        size: (width ?? 0) > 320 ? .hero : .backdrop,
                        logo: card.logo,
                        mark: card.mark,
                        fresh: DiscoverText.fresh(card.fresh)
                    )
                }
                .buttonStyle(.plain)
                QuickActionButton(card: card)
            }
            Button(action: open) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(card.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                        .lineLimit(1)
                        .padding(.top, 8)
                    let line = subtitle ?? DiscoverText.line(card, typed: typed)
                    if !line.isEmpty {
                        Text(line).font(.footnote).foregroundStyle(Theme.textMuted).lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .frame(width: width, alignment: .leading)
        .frame(maxWidth: width == nil ? .infinity : nil, alignment: .leading)
        .cardActions(card)
    }

    private func open() {
        if card.opensTitle {
            host?.onOpen(card.rowType)
            navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb))
        } else {
            fallback?()
        }
    }
}

struct TitlePosterCard: View {
    let card: DiscoverCardInfo
    var width: CGFloat = 120
    @Environment(Navigator.self) private var navigator
    @Environment(CardHost.self) private var host: CardHost?

    var body: some View {
        Button {
            guard card.opensTitle else { return }
            host?.onOpen(card.rowType)
            navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb))
        } label: {
            PosterCard(
                title: card.name,
                subtitle: DiscoverText.line(card),
                image: card.posterImage,
                width: width,
                progress: nil,
                mark: card.mark,
                fresh: DiscoverText.fresh(card.fresh)
            ) { EmptyView() }
        }
        .buttonStyle(.plain)
        .overlay(alignment: .topTrailing) {
            QuickActionButton(card: card).padding(.top, width * 1.5 - 48)
        }
        .cardActions(card)
    }
}

struct RankedPoster<Content: View>: View {
    let rank: Int
    @ViewBuilder var content: () -> Content

    var body: some View {
        HStack(alignment: .bottom, spacing: 2) {
            Text(String(rank))
                .font(.system(size: 64, weight: .black))
                .foregroundStyle(Theme.textMuted)
                .padding(.bottom, 40)
                .accessibilityHidden(true)
            content()
        }
    }
}

struct PersonCircleView: View {
    let person: PersonCardInfo
    var size: CGFloat = 88
    @Environment(Navigator.self) private var navigator

    var body: some View {
        Button {
            navigator.open(.person(id: person.id))
        } label: {
            VStack(spacing: Theme.Spacing.small) {
                AuthImage(source: person.photo, size: .thumb) { ArtPlaceholderView(name: person.name) }
                    .frame(width: size, height: size)
                    .clipShape(Circle())
                Text(person.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                if !person.note.isEmpty {
                    Text(person.note).font(.caption).foregroundStyle(Theme.textMuted).lineLimit(1)
                }
            }
            .frame(width: size + 16)
        }
        .buttonStyle(.plain)
    }
}

struct BrowseTileView: View {
    let name: String
    let image: String?
    let hue: Int
    var label: String?
    var plain = false

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            Theme.surfaceRaised
            AuthImage(source: image, size: .backdrop) { Theme.surfaceRaised }
            if plain {
                LinearGradient(colors: [.clear, Theme.artScrim], startPoint: .top, endPoint: .bottom)
            } else {
                let color = Theme.tileHues[min(max(hue, 0), Theme.tileHues.count - 1)]
                LinearGradient(colors: [color.opacity(0.55), color.opacity(0.92)], startPoint: .top, endPoint: .bottom)
            }
            VStack(alignment: .leading, spacing: 2) {
                if let label {
                    Text(label).font(.caption2.weight(.semibold)).foregroundStyle(Theme.onArt.opacity(0.84))
                }
                Text(name)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Theme.onArt)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
            }
            .padding(Theme.Spacing.medium)
        }
        .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}

struct CardShelf<Header: View, Content: View>: View {
    let title: String
    var subtitle: String?
    var seeAll: (() -> Void)?
    @ViewBuilder var header: () -> Header
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if !title.isEmpty {
                SectionHeader(title: title, action: seeAll)
                    .padding(.horizontal, Theme.Spacing.screen)
            }
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle).font(.callout).foregroundStyle(Theme.textMuted).padding(.horizontal, Theme.Spacing.screen)
            }
            header()
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: Theme.Spacing.medium) { content() }
                    .padding(.horizontal, Theme.Spacing.screen)
            }
        }
        .padding(.vertical, Theme.Spacing.small)
    }
}

extension CardShelf where Header == EmptyView {
    init(title: String, subtitle: String? = nil, seeAll: (() -> Void)? = nil, @ViewBuilder content: @escaping () -> Content) {
        self.init(title: title, subtitle: subtitle, seeAll: seeAll, header: { EmptyView() }, content: content)
    }
}

struct CardRowView: View {
    let row: CardRowInfo
    var title: String?
    var typed = false
    var seeAll: (() -> Void)?

    var body: some View {
        CardShelf(title: title ?? row.title, subtitle: row.subtitle, seeAll: seeAll) {
            ForEach(Array(row.cards.enumerated()), id: \.element.id) { index, card in
                if row.ranked {
                    RankedPoster(rank: card.rank > 0 ? card.rank : index + 1) { TitlePosterCard(card: card) }
                } else if row.wide {
                    TitleWideCard(card: card, typed: typed)
                } else {
                    TitlePosterCard(card: card)
                }
            }
        }
    }
}

struct ServiceChipsView: View {
    let chips: [ServiceChipInfo]
    let selected: Int64
    var showAll = false
    let pick: (Int64) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                if showAll {
                    chip(L10n.tr("discover.type.all"), on: selected == 0) { pick(0) }
                }
                ForEach(chips) { item in
                    chip(item.name, on: item.id == selected) { pick(item.id) }
                }
            }
            .padding(.horizontal, Theme.Spacing.screen)
        }
    }

    private func chip(_ text: String, on: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.subheadline.weight(on ? .semibold : .regular))
                .foregroundStyle(on ? Theme.background : Theme.text)
                .padding(.horizontal, 12)
                .frame(minHeight: 32)
                .background(on ? Theme.text : Theme.surfaceRaised, in: Capsule())
        }
        .buttonStyle(.plain)
    }
}

struct TopServicesRowView: View {
    let top: TopServicesInfo
    let row: CardRowInfo
    let title: String
    let pick: (Int64) -> Void
    var seeAll: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            CardShelf(title: title, seeAll: seeAll) {
                VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    if top.chips.count > 1 {
                        ServiceChipsView(chips: top.chips, selected: top.selected, pick: pick)
                    }
                    if !top.serviceName.isEmpty {
                        Text(DiscoverText.topSource(top))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                            .padding(.horizontal, Theme.Spacing.screen)
                    }
                }
            } content: {
                ForEach(Array(row.cards.enumerated()), id: \.element.id) { index, card in
                    if top.ranked {
                        RankedPoster(rank: card.rank > 0 ? card.rank : index + 1) { TitlePosterCard(card: card) }
                    } else {
                        TitlePosterCard(card: card)
                    }
                }
            }
            if !top.attribution.isEmpty {
                Text(top.attribution)
                    .font(.caption2)
                    .foregroundStyle(Theme.textFaint)
                    .padding(.horizontal, Theme.Spacing.screen)
            }
        }
    }
}

struct GenreBrowseRowView: View {
    let genres: [GenreCardInfo]
    @Environment(Navigator.self) private var navigator

    var body: some View {
        CardShelf(title: L10n.tr("discover.browse.by.genre")) {
            ForEach(genres) { genre in
                Button {
                    navigator.open(.genre(type: genre.type, genre: genre.genre))
                } label: {
                    BrowseTileView(name: DiscoverText.genre(genre.name), image: genre.backdrop, hue: hue(genre.name))
                        .frame(width: 200, height: 112)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func hue(_ name: String) -> Int {
        var hash: UInt32 = 0x811C_9DC5
        for unit in name.lowercased().unicodeScalars {
            hash ^= UInt32(unit.value & 0xFFFF)
            hash = hash &* 0x0100_0193
        }
        return Int(hash % 12)
    }
}

struct HeroView: View {
    let card: DiscoverCardInfo
    let pills: [String]
    @Environment(Navigator.self) private var navigator
    @Environment(CardHost.self) private var host: CardHost?

    var body: some View {
        Color.clear
            .aspectRatio(4 / 5, contentMode: .fit)
            .overlay(alignment: .bottomLeading) { content }
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.panel, style: .continuous))
            .cardActions(card)
            .padding(.horizontal, Theme.Spacing.screen)
    }

    private var content: some View {
        ZStack(alignment: .bottomLeading) {
            Theme.surfaceRaised
            Color.clear.overlay { AuthImage(source: card.wideImage ?? card.poster, size: .hero) { ArtPlaceholderView(name: card.name) } }
                .clipped()
            LinearGradient(colors: [.clear, Theme.artScrimStrong], startPoint: UnitPoint(x: 0.5, y: 0.35), endPoint: .bottom)
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                if let logo = card.logo {
                    AuthImage(source: logo, size: .posterLarge) { Text(card.name).font(.title.bold()).foregroundStyle(Theme.onArt) }
                        .scaledToFit()
                        .frame(maxWidth: 240, maxHeight: 72, alignment: .leading)
                } else {
                    Text(card.name).font(.title.bold()).foregroundStyle(Theme.onArt).lineLimit(2)
                }
                let genres = card.genres.prefix(3).map(DiscoverText.genre)
                let meta = [card.year > 0 ? String(card.year) : ""] + (genres.isEmpty ? [DiscoverText.kind(card.kind)] : genres)
                Text(meta.filter { !$0.isEmpty }.joined(separator: " · "))
                    .font(.callout)
                    .foregroundStyle(Theme.onArt.opacity(0.9))
                if !pills.isEmpty {
                    HStack(spacing: Theme.Spacing.small) {
                        ForEach(pills, id: \.self) { StatusChip(text: $0, tone: .info, onArt: true) }
                    }
                }
                if !card.overview.isEmpty {
                    Text(String(card.overview.prefix(220)))
                        .font(.footnote)
                        .foregroundStyle(Theme.onArt.opacity(0.84))
                        .lineLimit(3)
                }
                HStack(spacing: Theme.Spacing.small) {
                    Button {
                        host?.onOpen("hero")
                        navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb))
                    } label: {
                        Label(L10n.tr("discover.more.info"), systemImage: "info.circle")
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(!card.opensTitle)
                    if let action = card.action {
                        Button {
                            host?.onAct("hero")
                            switch action {
                            case .add: navigator.open(.title(kind: card.kind.wire, tmdb: card.tmdb))
                            case .ask: navigator.open(.ask(kind: card.kind.wire, tmdb: card.tmdb))
                            }
                        } label: {
                            Label(DiscoverText.actionShort(action), systemImage: "plus")
                        }
                        .buttonStyle(.bordered)
                        .tint(Theme.onArt)
                    }
                }
            }
            .padding(Theme.Spacing.large)
        }
    }
}

struct CardGrid: View {
    let cards: [DiscoverCardInfo]
    var typed = false
    var fallback: ((DiscoverCardInfo) -> Void)?

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.flexible(), spacing: Theme.Spacing.medium), GridItem(.flexible(), spacing: Theme.Spacing.medium)],
            alignment: .leading,
            spacing: Theme.Spacing.large
        ) {
            ForEach(cards) { card in
                TitleWideCard(card: card, typed: typed, width: nil, fallback: fallback.map { action in { action(card) } })
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

struct CardHostModifier: ViewModifier {
    let host: CardHost

    func body(content: Content) -> some View {
        @Bindable var host = host
        content
            .environment(host)
            .sheet(item: $host.hiding) { target in
                HideSheetView(target: target) { done in
                    host.hiding = nil
                    host.toast = done
                    host.onChanged()
                }
            }
            .overlay(alignment: .bottom) {
                if let done = host.toast {
                    HideToast(done: done, undo: { host.undo(done) }, close: { host.toast = nil })
                        .padding(Theme.Spacing.screen)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .task(id: done) {
                            try? await Task.sleep(for: .seconds(6))
                            if host.toast == done { host.toast = nil }
                        }
                }
            }
            .animation(.easeOut(duration: 0.24), value: host.toast)
    }
}

struct HideToast: View {
    let done: HideDoneInfo
    let undo: () -> Void
    let close: () -> Void

    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            Text(
                done.everyone
                    ? L10n.format("discover.hide.done.everyone", done.name)
                    : L10n.format("discover.hide.done.person", done.name, done.person)
            )
                .font(.callout)
                .foregroundStyle(Theme.text)
            Spacer(minLength: 0)
            Button(L10n.tr("discover.hide.undo")) { undo() }
                .font(.callout.weight(.semibold))
        }
        .padding(Theme.Spacing.medium)
        .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
        .onTapGesture { close() }
    }
}

extension View {
    func cardHost(_ host: CardHost) -> some View {
        modifier(CardHostModifier(host: host))
    }
}
