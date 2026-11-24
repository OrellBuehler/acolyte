import SwiftUI

struct TVTitleShelves: View {
    let info: TitleInfo
    let store: TitleStore
    @Environment(TVNavigator.self) private var navigator

    var body: some View {
        if info.isShow && info.tmdbSeasons {
            TVEpisodesSection(info: info, store: store)
        }
        if !info.cast.isEmpty {
            TVShelf(title: L10n.tr("title.cast")) {
                ForEach(info.cast.prefix(20)) { person in personCard(person) }
            }
        } else if info.legacy && !info.credits.isEmpty {
            TVShelf(title: L10n.tr("title.cast")) {
                ForEach(info.credits.prefix(20)) { person in personCard(person) }
            }
        }
        if !info.collectionParts.isEmpty {
            TVShelf(title: info.collectionName, seeAll: collectionAction) {
                ForEach(info.collectionParts) { TVTitleCard(card: $0, manage: info.access.manage) }
            }
        }
        cards(L10n.tr("title.more.like.this"), info.similar)
        cards(info.moreFromPerson.isEmpty ? L10n.tr("title.more.from.them") : L10n.format("title.more.from", info.moreFromPerson), info.moreFromItems)
        if !info.leadName.isEmpty {
            cards(L10n.format("title.more.with", info.leadName), info.leadItems)
        }
    }

    private var collectionAction: (() -> Void)? {
        info.collectionId.isEmpty ? nil : { navigator.open(.collection(id: info.collectionId)) }
    }

    @ViewBuilder
    private func cards(_ title: String, _ list: [TitleCardInfo]) -> some View {
        if !list.isEmpty {
            TVShelf(title: title) {
                ForEach(list.prefix(20)) { TVTitleCard(card: $0, manage: info.access.manage) }
            }
        }
    }

    private func personCard(_ person: TitlePersonInfo) -> some View {
        TVPersonCard(
            name: person.name,
            role: person.character.isEmpty ? roleText(person.role) : person.character,
            photo: person.profile,
            action: person.personId.isEmpty ? nil : { navigator.open(.person(id: person.personId)) }
        )
    }

    private func roleText(_ role: String) -> String {
        switch role {
        case "director": L10n.tr("title.role.director")
        case "creator": L10n.tr("title.role.creator")
        case "actor", "": ""
        default: role.capitalized
        }
    }
}

private struct TVEpisodesSection: View {
    let info: TitleInfo
    let store: TitleStore

    var body: some View {
        let browser = store.seasonBrowser
        let state = browser.info
        let seasons = store.seasons()
        let have = info.isOwned ? seasons.info.seasons[state.selected]?.episodes ?? [] : []
        VStack(alignment: .leading, spacing: 0) {
            Text(L10n.tr("title.seasons"))
                .font(.headline)
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 24) {
                    ForEach(state.seasons) { season in
                        TVChip(
                            title: TitleText.season(season.number) + (season.number == state.newSeason ? " •" : ""),
                            selected: season.number == state.selected
                        ) {
                            browser.run { await $0.select(season.number) }
                        }
                    }
                }
                .padding(.vertical, Theme.TV.focusPad)
            }
            .scrollClipDisabled()
            .focusSection()
            if state.failed {
                Text(L10n.format("title.season.failed", TitleText.season(state.selected)))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: Theme.TV.gutter) {
                        ForEach(state.episodes) { episode in
                            TVEpisodeCard(episode: episode, backdrop: info.backdrop, chip: chip(episode, have: have))
                        }
                    }
                    .padding(.vertical, Theme.TV.focusPad)
                }
                .scrollClipDisabled()
                .focusSection()
                .frame(minHeight: state.loading && state.episodes.isEmpty ? 240 : nil)
            }
        }
        .task { await browser.watch() }
        .task { await browser.driver.load() }
        .task { await seasons.watch() }
        .task(id: state.selected) {
            if info.isOwned { await seasons.driver.ensure(state.selected) }
        }
    }

    private func chip(_ episode: TMDBEpisodeInfo, have: [EpisodeInfo]) -> TVChipInfo? {
        guard let owned = have.first(where: { $0.episode == episode.number }) else { return nil }
        switch owned.tone {
        case .have: return TVChipInfo(text: L10n.tr("title.state.in.library"), tone: .ok)
        case .downloading: return TVChipInfo(text: L10n.tr("title.state.downloading"), tone: .wait)
        case .failed: return TVChipInfo(text: L10n.tr("title.state.download.failed"), tone: .fail)
        case .missing: return owned.notAired ? nil : TVChipInfo(text: L10n.tr("title.state.not.downloaded"), tone: .idle)
        }
    }
}

private struct TVEpisodeCard: View {
    let episode: TMDBEpisodeInfo
    let backdrop: String
    let chip: TVChipInfo?

    var body: some View {
        let name = episode.name.isEmpty ? L10n.format("title.episode.number", episode.number) : episode.name
        TVBackdropCard(
            title: "\(episode.number). \(name)",
            subtitle: line,
            detail: episode.overview,
            chip: chip,
            image: episode.still.isEmpty ? backdrop : episode.still
        ) {}
    }

    private var line: String {
        var parts: [String] = []
        let runtime = TitleText.runtime(episode.runtime)
        if !runtime.isEmpty { parts.append(runtime) }
        parts.append(episode.airDay.isEmpty ? L10n.tr("title.no.air.date") : TitleText.dayText(episode.airDay))
        return parts.joined(separator: " · ")
    }
}
