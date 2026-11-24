import SwiftUI

struct TVTrailerItem: Identifiable {
    let source: TrailerSourceInfo
    var id: String { source.hls + source.mp4 + source.relay + source.youTubeURL }
}

struct TVTitleView: View {
    @State private var store: TitleStore
    @Environment(TVRouter.self) private var router

    init(kind: String, tmdb: String, libraryId: Int64) {
        _store = State(initialValue: TitleStore(kind: kind, tmdb: tmdb, libraryId: libraryId, openLibrary: false))
    }

    var body: some View {
        let info = store.info
        ZStack {
            Theme.background.ignoresSafeArea()
            if info.problem == TitleProblemKind.none, info.load.phase == .ready {
                TVBackdropLayer(source: info.backdrop.isEmpty ? info.poster : info.backdrop)
            }
            content(info)
        }
        .toolbarVisibility(.hidden, for: .tabBar)
        .fullScreenCover(item: trailerItem) { item in
            TVTrailerPlayerView(source: item.source, store: store)
        }
        .onChange(of: router.ask) { old, new in
            if old != nil, new == nil { Task { await store.refresh() } }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private var trailerItem: Binding<TVTrailerItem?> {
        Binding(get: { store.trailer.map(TVTrailerItem.init) }, set: { store.trailer = $0?.source })
    }

    @ViewBuilder
    private func content(_ info: TitleInfo) -> some View {
        switch info.problem {
        case .blocked:
            TVEmptyView(title: L10n.tr("title.blocked"), systemImage: "hand.raised")
        case .notfound:
            TVEmptyView(title: L10n.tr("title.not.found"), message: L10n.tr("title.not.found.hint"), systemImage: "magnifyingglass")
        case .notinlibrary:
            TVEmptyView(title: L10n.tr("title.not.in.library"), message: L10n.tr("title.not.in.library.hint"), systemImage: "books.vertical")
        case TitleProblemKind.none:
            TVStatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                EmptyView()
            } content: {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: Theme.TV.sectionGap) {
                        TVTitleHero(info: info, store: store)
                            .containerRelativeFrame(.vertical, alignment: .bottomLeading) { height, _ in height * 0.8 }
                        TVTitleShelves(info: info, store: store)
                    }
                }
            }
        }
    }
}

private struct TVTitleHero: View {
    let info: TitleInfo
    let store: TitleStore

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Spacer(minLength: 0)
            titleView
            chips
            let meta = metaLine
            if !meta.isEmpty {
                Text(meta)
                    .font(.callout)
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
            }
            if !info.tagline.isEmpty {
                Text(info.tagline)
                    .font(.callout.italic())
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                    .frame(maxWidth: Theme.TV.readableWidth, alignment: .leading)
            }
            if !info.overview.isEmpty {
                Text(info.overview)
                    .font(.footnote)
                    .foregroundStyle(Theme.text)
                    .lineLimit(4)
                    .frame(maxWidth: Theme.TV.readableWidth, alignment: .leading)
            }
            if !info.starring.isEmpty {
                Text(L10n.tr("title.starring") + ": " + info.starring.map(\.name).joined(separator: ", "))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
                    .frame(maxWidth: Theme.TV.readableWidth, alignment: .leading)
            }
            TVTitleActions(info: info, store: store)
                .padding(.top, 8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, Theme.TV.focusPad)
    }

    @ViewBuilder
    private var titleView: some View {
        if info.logo.isEmpty {
            Text(info.name)
                .font(.largeTitle.weight(.bold))
                .foregroundStyle(Theme.text)
                .lineLimit(2)
                .minimumScaleFactor(0.6)
        } else {
            AuthImage(source: info.logo, size: .posterLarge) {
                Text(info.name).font(.largeTitle.weight(.bold)).foregroundStyle(Theme.text)
            }
            .scaledToFit()
            .frame(maxWidth: 560, maxHeight: 160, alignment: .leading)
            .accessibilityLabel(info.name)
        }
    }

    private var chips: some View {
        HStack(spacing: 16) {
            if let status = info.libraryStatus {
                TVStatusChip(text: TitleText.library(status), tone: status.tone)
            }
            if let pill = TitleText.pill(info.pill, mine: info.askIsMine) {
                TVStatusChip(text: pill, tone: info.pill == .approved || info.pill == .available ? .ok : .idle)
            }
        }
    }

    private var metaLine: String {
        var parts: [String] = []
        if info.year > 0 { parts.append(String(info.year)) }
        if info.isShow && info.seasonCount > 0 {
            parts.append(L10n.plural("title.seasons.count", info.seasonCount))
        } else {
            let runtime = TitleText.runtime(info.runtime)
            if !runtime.isEmpty { parts.append(runtime) }
        }
        if !info.certification.isEmpty { parts.append(info.certification) }
        parts.append(contentsOf: info.genres.prefix(3))
        if !info.factTmdb.isEmpty { parts.append("TMDB " + info.factTmdb) }
        return parts.joined(separator: " · ")
    }
}

private struct TVTitleActions: View {
    let info: TitleInfo
    let store: TitleStore
    @Environment(TVNavigator.self) private var navigator

    var body: some View {
        HStack(spacing: 24) {
            if let offer = info.askOffer {
                TVActionButton(title: askText(offer), systemImage: offer == .asked ? "checkmark" : "plus") {
                    navigator.open(.ask(kind: info.kind, tmdb: info.tmdb))
                }
                .disabled(offer == .asked)
            }
            if !info.trailerKey.isEmpty {
                TVActionButton(title: L10n.tr("title.trailer"), systemImage: "play.rectangle") {
                    store.playTrailer()
                }
                .disabled(store.trailerLoading)
            }
            if let offer = info.fourKOffer {
                TVActionButton(title: offer == .asked ? L10n.tr("title.requested.4k") : L10n.tr("title.request.4k")) {
                    navigator.open(.ask(kind: info.kind, tmdb: info.tmdb, fourK: true))
                }
                .disabled(offer == .asked)
            }
            if info.access.followOn && info.hasTmdb {
                TVActionButton(
                    title: info.following ? L10n.tr("title.following") : L10n.tr("title.follow"),
                    systemImage: info.following ? "bell.fill" : "bell"
                ) {
                    store.run { await $0.toggleFollow() }
                }
                .disabled(info.followBusy)
            }
        }
        .focusSection()
    }

    private func askText(_ offer: AskOfferKind) -> String {
        switch offer {
        case .ask, .seasons: L10n.tr("title.request")
        case .asktoo: L10n.tr("title.request.too")
        case .asked: L10n.tr("title.requested")
        }
    }
}
