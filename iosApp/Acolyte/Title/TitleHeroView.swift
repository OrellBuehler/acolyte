import SwiftUI

struct TitleHeroView: View {
    let info: TitleInfo
    let store: TitleStore
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            ZStack(alignment: .bottomLeading) {
                AuthImage(source: info.backdrop.isEmpty ? info.poster : info.backdrop, size: .hero) {
                    ArtPlaceholderView(name: info.name)
                }
                .frame(maxWidth: .infinity)
                .aspectRatio(16 / 11, contentMode: .fit)
                .clipped()
                LinearGradient(
                    stops: [
                        .init(color: Theme.artScrim.opacity(0.35), location: 0),
                        .init(color: Theme.background.opacity(0), location: 0.45),
                        .init(color: Theme.background, location: 1),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                Group {
                    if info.logo.isEmpty {
                        Text(info.name)
                            .font(.largeTitle.weight(.bold))
                            .foregroundStyle(Theme.text)
                            .lineLimit(3)
                    } else {
                        AuthImage(source: info.logo, size: .posterLarge) {
                            Text(info.name).font(.largeTitle.weight(.bold)).foregroundStyle(Theme.text)
                        }
                        .scaledToFit()
                        .frame(maxWidth: 260, maxHeight: 88, alignment: .leading)
                        .accessibilityLabel(info.name)
                    }
                }
                .padding(.horizontal, Theme.Spacing.screen)
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                if !info.tagline.isEmpty {
                    Text(info.tagline).font(.body.italic()).foregroundStyle(Theme.textMuted)
                }
                MetaLineView(info: info)
                if let pill = TitleText.pill(info.pill, mine: info.askIsMine) {
                    StatusChip(text: pill, tone: info.pill == .approved ? .ok : .idle)
                }
                if !info.askers.isEmpty {
                    Text(L10n.format("title.asked.by", info.askers.prefix(3).joined(separator: ", ")))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                if let outcome = info.outcome, outcome.empty {
                    Text(outcomeText(outcome)).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                HeroActionsView(info: info, store: store)
                    .padding(.top, Theme.Spacing.xSmall)
            }
            .padding(.horizontal, Theme.Spacing.screen)
        }
    }

    private func outcomeText(_ outcome: OutcomeInfo) -> String {
        if outcome.refused { return L10n.tr("title.outcome.refused") }
        if !outcome.sources.isEmpty {
            let whys = outcome.sources.map { TitleText.outcomeWhy($0.name, $0.why) }.joined(separator: ", ")
            return L10n.format("title.outcome.nothing", whys) + (outcome.note.isEmpty ? "" : " · \(outcome.note)")
        }
        return L10n.tr("title.outcome.none")
    }
}

private struct MetaLineView: View {
    let info: TitleInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            let parts = metaParts
            if !parts.isEmpty {
                Text(parts.joined(separator: " · ")).font(.subheadline).foregroundStyle(Theme.textMuted)
            }
            if !info.factScores.isEmpty || !info.factTmdb.isEmpty || !info.awardBadges.isEmpty {
                HStack(spacing: Theme.Spacing.medium) {
                    if !info.factTmdb.isEmpty { score("TMDB", info.factTmdb) }
                    ForEach(info.factScores) { score(TitleText.scoreName($0.source), $0.text) }
                    ForEach(info.awardBadges, id: \.self) { badge in
                        Label(badge, systemImage: "star.fill")
                            .font(.footnote.weight(.medium))
                            .foregroundStyle(Theme.waitText)
                    }
                }
            }
        }
    }

    private var metaParts: [String] {
        var parts: [String] = []
        if info.year > 0 { parts.append(String(info.year)) }
        if info.isShow && info.seasonCount > 0 {
            parts.append(L10n.plural("title.seasons.count", info.seasonCount))
        } else {
            let runtime = TitleText.runtime(info.runtime)
            if !runtime.isEmpty { parts.append(runtime) }
        }
        if !info.certification.isEmpty { parts.append(info.certification) }
        parts.append(contentsOf: info.genres.prefix(info.isShow ? 2 : 3))
        return parts
    }

    private func score(_ name: String, _ value: String) -> some View {
        HStack(spacing: 4) {
            Text(value).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
            Text(name).font(.footnote).foregroundStyle(Theme.textMuted)
        }
    }
}

private struct HeroActionsView: View {
    let info: TitleInfo
    let store: TitleStore
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                if let status = info.libraryStatus {
                    Button {
                        if info.primaryRuns { store.run { await $0.searchNow() } } else { store.selectTab(.library) }
                    } label: {
                        Label(primaryText(status), systemImage: icon(status.kind))
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                    .disabled(info.busy == "search")
                }
                if info.canAdd {
                    Button { store.openAdd() } label: { Label(L10n.tr("title.add.to.library"), systemImage: "plus") }
                        .buttonStyle(.borderedProminent)
                        .tint(Theme.accent)
                }
                if info.canCheckReleases {
                    Button { store.openReleases(.check) } label: { Label(L10n.tr("title.check.releases"), systemImage: "square.grid.2x2") }
                        .buttonStyle(.bordered)
                }
                if let offer = info.askOffer {
                    Button { openRoute(.ask(kind: info.kind, tmdb: info.tmdb)) } label: {
                        Text(askText(offer))
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                    .disabled(offer == .asked)
                }
                if let offer = info.fourKOffer {
                    Button { openRoute(.ask(kind: info.kind, tmdb: info.tmdb, fourK: true)) } label: {
                        Text(offer == .asked ? L10n.tr("title.requested.4k") : L10n.tr("title.request.4k"))
                    }
                    .buttonStyle(.bordered)
                    .disabled(offer == .asked)
                }
                if !info.trailerKey.isEmpty {
                    Button { store.playTrailer() } label: {
                        if store.trailerLoading {
                            ProgressView()
                        } else {
                            Label(L10n.tr("title.trailer"), systemImage: "play.fill")
                        }
                    }
                    .buttonStyle(.bordered)
                }
                if info.access.manage, let status = info.libraryStatus, status.kind != .missing {
                    ForEach(info.watchLinks) { link in
                        let twins = info.watchLinks.filter { $0.kind == link.kind }.count
                        Button {
                            if let url = URL(string: link.url) { openURL(url) }
                        } label: {
                            Label(L10n.format("title.open.in", twins > 1 ? link.server : serverKind(link.kind)), systemImage: "play.rectangle")
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }
        }
    }

    private func primaryText(_ status: LibraryStatusInfo) -> String {
        if info.primaryRuns { return status.kind == .failed ? L10n.tr("title.retry") : L10n.tr("title.search.now") }
        return TitleText.library(status)
    }

    private func icon(_ kind: LibraryKindValue) -> String {
        switch kind {
        case .have: "checkmark"
        case .failed: "arrow.clockwise"
        case .downloading: "arrow.down"
        case .missing: "magnifyingglass"
        }
    }

    private func askText(_ offer: AskOfferKind) -> String {
        switch offer {
        case .ask, .seasons: L10n.tr("title.request")
        case .asktoo: L10n.tr("title.request.too")
        case .asked: L10n.tr("title.requested")
        }
    }

    private func serverKind(_ kind: String) -> String {
        switch kind {
        case "plex": "Plex"
        case "jellyfin": "Jellyfin"
        case "emby": "Emby"
        default: kind
        }
    }
}
