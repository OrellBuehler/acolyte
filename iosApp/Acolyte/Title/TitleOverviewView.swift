import SwiftUI

struct TitleOverviewView: View {
    let info: TitleInfo
    let store: TitleStore
    @State private var expanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.large) {
            story
            if !info.trailers.isEmpty {
                ShelfView(title: L10n.tr("title.trailers")) {
                    ForEach(info.trailers) { video in TrailerCard(video: video, info: info, store: store) }
                }
            }
            if info.tmdbSeasons { SeasonBrowserView(store: store.seasonBrowser, backdrop: info.backdrop) }
            if !info.cast.isEmpty {
                ShelfView(title: L10n.tr("title.cast"), action: { store.open(.cast) }) {
                    ForEach(info.cast.prefix(12)) { PersonCircle(person: $0) }
                }
            }
            if info.legacy && !info.credits.isEmpty {
                ShelfView(title: L10n.tr("title.cast")) {
                    ForEach(info.credits.prefix(12)) { PersonCircle(person: $0) }
                }
            }
            if info.hasTmdb { ReviewsShelf(store: store.reviews, title: store) }
            if !info.collectionParts.isEmpty { collection }
            cards("similar", L10n.tr("title.more.like.this"), info.similar)
            cards(
                "more",
                info.moreFromPerson.isEmpty ? L10n.tr("title.more.from.them") : L10n.format("title.more.from", info.moreFromPerson),
                info.moreFromItems
            )
            if !info.leadName.isEmpty { cards("lead", L10n.format("title.more.with", info.leadName), info.leadItems) }
            AboutBand(info: info)
        }
        .padding(.top, Theme.Spacing.medium)
    }

    private var story: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            if !info.overview.isEmpty {
                Text(info.overview)
                    .font(.body)
                    .foregroundStyle(Theme.text)
                    .lineLimit(expanded ? nil : 5)
                    .onTapGesture { expanded.toggle() }
            }
            if !info.directors.isEmpty {
                CreditLine(label: info.isShow ? L10n.tr("title.created.by") : L10n.tr("title.directed.by"), people: info.directors)
            }
            if !info.starring.isEmpty { CreditLine(label: L10n.tr("title.starring"), people: info.starring) }
            if !info.genres.isEmpty { InfoLine(label: L10n.tr("title.genres"), value: info.genres.prefix(3).joined(separator: ", ")) }
            if !info.contentNotes.isEmpty { InfoLine(label: L10n.tr("title.contains"), value: info.contentNotes.joined(separator: ", ")) }
            if !info.studio.isEmpty { InfoLine(label: L10n.tr("title.studio"), value: info.studio) }
            if !info.network.isEmpty { InfoLine(label: L10n.tr("title.network"), value: info.network) }
            if !info.keywords.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Theme.Spacing.small) {
                        ForEach(info.keywords, id: \.self) { keyword in
                            Text(keyword)
                                .font(.footnote.weight(.medium))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Theme.surfaceRaised, in: Capsule())
                        }
                    }
                }
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }

    private var collection: some View {
        let requestable = info.collectionParts.filter { $0.titleId == 0 && !$0.inLibrary && (info.access.manage || !$0.asked) }.count
        let batch = requestable >= 2 && (info.access.manage || info.access.mayAsk)
        return ShelfView(
            title: info.collectionName,
            actionTitle: batch ? (info.access.manage ? L10n.tr("title.add.all") : L10n.tr("title.request.all")) : nil,
            action: batch ? { store.openAddAll() } : nil,
            link: batch || info.collectionId.isEmpty ? nil : Route.collection(id: info.collectionId)
        ) {
            ForEach(info.collectionParts) { TitleCardLink(card: $0, manage: info.access.manage) }
        }
    }

    @ViewBuilder
    private func cards(_ key: String, _ title: String, _ list: [TitleCardInfo]) -> some View {
        if !list.isEmpty {
            ShelfView(title: title, action: list.count > 6 ? { store.open(.cards(title, list)) } : nil) {
                ForEach(list.prefix(12)) { TitleCardLink(card: $0, manage: info.access.manage) }
            }
        }
    }
}

struct ShelfView<Content: View>: View {
    let title: String
    var actionTitle: String?
    var action: (() -> Void)?
    var link: Route?
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack {
                SectionHeader(title: title, actionTitle: actionTitle, action: action)
                if let link {
                    NavigationLink(value: link) { Text(L10n.tr("common.see.all")).font(.subheadline.weight(.semibold)) }
                        .foregroundStyle(Theme.accentText)
                }
            }
            .padding(.horizontal, Theme.Spacing.screen)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: Theme.Spacing.medium) { content() }
                    .padding(.horizontal, Theme.Spacing.screen)
            }
        }
    }
}

struct TitleCardLink: View {
    let card: TitleCardInfo
    let manage: Bool

    var body: some View {
        NavigationLink(value: route) {
            PosterCard(title: card.name, subtitle: subtitle, image: card.poster)
        }
        .buttonStyle(.plain)
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

struct PersonCircle: View {
    let person: TitlePersonInfo

    var body: some View {
        let content = VStack(spacing: Theme.Spacing.xSmall) {
            AuthImage(source: person.profile, size: .thumb) { ArtPlaceholderView(name: person.name) }
                .frame(width: 84, height: 84)
                .clipShape(Circle())
            Text(person.name)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(2)
                .multilineTextAlignment(.center)
            let role = person.character.isEmpty ? roleText(person.role) : person.character
            if !role.isEmpty {
                Text(role).font(.caption).foregroundStyle(Theme.textMuted).lineLimit(1)
            }
        }
        .frame(width: 96)
        if person.personId.isEmpty {
            content
        } else {
            NavigationLink(value: Route.person(id: person.personId)) { content }.buttonStyle(.plain)
        }
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

private struct CreditLine: View {
    let label: String
    let people: [TitlePersonInfo]

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: Theme.Spacing.small) {
            Text(label).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.textMuted).frame(width: 110, alignment: .leading)
            VStack(alignment: .leading, spacing: 2) {
                ForEach(people) { person in
                    if person.personId.isEmpty {
                        Text(person.name).font(.subheadline)
                    } else {
                        NavigationLink(value: Route.person(id: person.personId)) {
                            Text(person.name).font(.subheadline).foregroundStyle(Theme.accentText)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

struct InfoLine: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: Theme.Spacing.small) {
            Text(label).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.textMuted).frame(width: 110, alignment: .leading)
            Text(value).font(.subheadline).foregroundStyle(Theme.text).frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct TrailerCard: View {
    let video: TitleVideoInfo
    let info: TitleInfo
    let store: TitleStore
    @Environment(\.openURL) private var openURL

    var body: some View {
        Button {
            if video.key == info.trailerKey {
                store.playTrailer()
            } else if let url = URL(string: "https://www.youtube.com/watch?v=\(video.key)") {
                openURL(url)
            }
        } label: {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                AuthImage(source: "https://i.ytimg.com/vi/\(video.key)/mqdefault.jpg", size: .backdrop) {
                    ArtPlaceholderView(name: video.name.isEmpty ? info.name : video.name)
                }
                .frame(width: 240, height: 135)
                .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
                Text(video.name.isEmpty ? L10n.format("title.trailer.of", info.name) : video.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                Text(typeLine).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            .frame(width: 240, alignment: .leading)
        }
        .buttonStyle(.plain)
    }

    private var typeLine: String {
        let type = video.type == "Teaser" ? L10n.tr("title.teaser") : L10n.tr("title.trailer")
        if video.language.isEmpty || video.language == "en" { return type }
        return type + " · " + TitleText.language(video.language)
    }
}

private struct SeasonBrowserView: View {
    let store: SeasonBrowserStore
    let backdrop: String

    var body: some View {
        let info = store.info
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("title.seasons")).padding(.horizontal, Theme.Spacing.screen)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(info.seasons) { season in
                        Button {
                            store.run { await $0.select(season.number) }
                        } label: {
                            Text(TitleText.season(season.number) + (season.number == info.newSeason ? " •" : ""))
                                .font(.subheadline.weight(.medium))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 7)
                                .foregroundStyle(season.number == info.selected ? Theme.background : Theme.text)
                                .background(season.number == info.selected ? Theme.text : Theme.surfaceRaised, in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, Theme.Spacing.screen)
            }
            if let season = info.seasons.first(where: { $0.number == info.selected }) {
                let parts = [
                    season.episodeCount > 0 ? L10n.plural("title.episodes.count", season.episodeCount) : "",
                    season.year > 0 ? String(season.year) : "",
                ].filter { !$0.isEmpty }
                if !parts.isEmpty {
                    Text(parts.joined(separator: " · ")).font(.footnote).foregroundStyle(Theme.textMuted).padding(.horizontal, Theme.Spacing.screen)
                }
            }
            if info.failed {
                Text(L10n.format("title.season.failed", TitleText.season(info.selected)))
                    .font(.callout)
                    .padding(.horizontal, Theme.Spacing.screen)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: Theme.Spacing.medium) {
                        ForEach(info.episodes) { episode in episodeCard(episode) }
                    }
                    .padding(.horizontal, Theme.Spacing.screen)
                }
                .frame(minHeight: info.loading && info.episodes.isEmpty ? 120 : nil)
            }
        }
        .task { await store.watch() }
        .task { await store.driver.load() }
    }

    private func episodeCard(_ episode: TMDBEpisodeInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            AuthImage(source: episode.still.isEmpty ? backdrop : episode.still, size: .backdrop) {
                ArtPlaceholderView(name: episode.name.isEmpty ? String(episode.number) : episode.name)
            }
            .frame(width: 240, height: 135)
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
            Text("\(episode.number). " + (episode.name.isEmpty ? L10n.format("title.episode.number", episode.number) : episode.name))
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
            Text(episodeLine(episode)).font(.footnote).foregroundStyle(Theme.textMuted)
            if !episode.overview.isEmpty {
                Text(episode.overview).font(.footnote).foregroundStyle(Theme.text).lineLimit(3)
            }
        }
        .frame(width: 240, alignment: .leading)
    }

    private func episodeLine(_ episode: TMDBEpisodeInfo) -> String {
        var line: [String] = []
        let runtime = TitleText.runtime(episode.runtime)
        if !runtime.isEmpty { line.append(runtime) }
        line.append(episode.airDay.isEmpty ? L10n.tr("title.no.air.date") : TitleText.dayText(episode.airDay))
        if episode.absolute > 0 { line.append(L10n.format("title.absolute", episode.absolute)) }
        return line.joined(separator: " · ")
    }
}

private struct ReviewsShelf: View {
    let store: ReviewsStore
    let title: TitleStore

    var body: some View {
        let info = store.info
        Group {
            if info.visible {
                VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    SectionHeader(title: L10n.tr("title.reviews")).padding(.horizontal, Theme.Spacing.screen)
                    Text(summary(info)).font(.subheadline).padding(.horizontal, Theme.Spacing.screen)
                    if info.sources.count > 1 || info.canHelpful {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: Theme.Spacing.small) {
                                if info.sources.count > 1 {
                                    ForEach(info.sources) { source in
                                        pill(source.name, selected: info.source == source.id) { store.run { await $0.setSource(source.id) } }
                                    }
                                }
                                if info.canHelpful {
                                    pill(L10n.tr("title.reviews.newest"), selected: info.order == "newest") { store.run { await $0.setOrder("newest") } }
                                    pill(L10n.tr("title.reviews.helpful"), selected: info.order == "helpful") { store.run { await $0.setOrder("helpful") } }
                                }
                            }
                            .padding(.horizontal, Theme.Spacing.screen)
                        }
                    }
                    if info.reviews.isEmpty {
                        Text(info.load.error != nil ? L10n.tr("title.reviews.failed") : L10n.tr("title.reviews.empty"))
                            .font(.callout)
                            .foregroundStyle(Theme.textMuted)
                            .padding(.horizontal, Theme.Spacing.screen)
                    } else {
                        ScrollView(.horizontal, showsIndicators: false) {
                            LazyHStack(alignment: .top, spacing: Theme.Spacing.medium) {
                                ForEach(info.reviews) { review in reviewCard(review) }
                                if info.hasMore {
                                    Button(L10n.tr("title.reviews.more")) { store.run { await $0.more() } }
                                        .disabled(info.loadingMore)
                                }
                            }
                            .padding(.horizontal, Theme.Spacing.screen)
                        }
                    }
                }
            }
        }
        .task { await store.watch() }
        .task { await store.driver.load() }
    }

    private func summary(_ info: ReviewsInfo) -> String {
        var parts: [String] = []
        if info.score > 0 { parts.append(String(format: "★ %.1f /10", info.score)) }
        if info.votes > 0 { parts.append(L10n.plural("title.ratings.count", info.votes)) }
        if info.watching > 0 { parts.append(L10n.format("title.watching.now", info.watching)) }
        return parts.isEmpty ? L10n.tr("title.no.score") : parts.joined(separator: " · ")
    }

    private func pill(_ text: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.subheadline.weight(.medium))
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .foregroundStyle(selected ? Theme.background : Theme.text)
                .background(selected ? Theme.text : Theme.surfaceRaised, in: Capsule())
        }
        .buttonStyle(.plain)
    }

    private func reviewCard(_ review: ReviewInfo) -> some View {
        Button {
            title.open(.review(review))
        } label: {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(review.author.isEmpty ? L10n.tr("title.review.someone") : review.author)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                let meta = [
                    review.date.isEmpty ? "" : TitleText.dayText(review.date),
                    review.rating > 0 ? String(format: "★ %.1f", review.rating) : "",
                ].filter { !$0.isEmpty }
                if !meta.isEmpty { Text(meta.joined(separator: " · ")).font(.footnote).foregroundStyle(Theme.textMuted) }
                Text(review.spoiler ? L10n.tr("title.review.spoiler") : review.text)
                    .font(.callout)
                    .foregroundStyle(Theme.text)
                    .lineLimit(5)
                    .multilineTextAlignment(.leading)
                if review.likes > 0 { Text(L10n.plural("title.review.likes", review.likes)).font(.footnote).foregroundStyle(Theme.textMuted) }
            }
            .padding(Theme.Spacing.large)
            .frame(width: 300, alignment: .leading)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct AboutBand: View {
    let info: TitleInfo
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            SectionHeader(title: L10n.tr("title.about"))
            AboutCard {
                if let status = info.libraryStatus {
                    StatusChip(text: TitleText.library(status), tone: status.tone)
                    if !info.ownBadges.isEmpty { InfoLine(label: L10n.tr("title.quality"), value: info.ownBadges.joined(separator: " · ")) }
                    if let file = info.mainFile { InfoLine(label: L10n.tr("title.size"), value: TitleText.bytes(file.size)) }
                    if let added = info.addedAt { InfoLine(label: L10n.tr("title.added"), value: TitleText.date(added)) }
                    InfoLine(label: L10n.tr("title.kept.up.to.date"), value: info.keepUpToDate ? L10n.tr("title.yes") : L10n.tr("title.no"))
                } else {
                    Text(info.name).font(.headline)
                    let caption = info.genres.prefix(2).joined(separator: ", ")
                    Text(caption.isEmpty ? (info.isShow ? L10n.tr("title.kind.show") : L10n.tr("title.kind.movie")) : caption)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    if !info.overview.isEmpty { Text(info.overview).font(.callout).lineLimit(4) }
                }
            }
            if !info.scores.isEmpty || !info.tmdbScore.isEmpty {
                AboutCard {
                    Text(L10n.tr("title.ratings")).font(.headline)
                    if !info.tmdbScore.isEmpty { InfoLine(label: "TMDB", value: info.tmdbScore) }
                    ForEach(info.scores) { InfoLine(label: TitleText.scoreName($0.source), value: $0.text) }
                }
            }
            if info.hasAwards {
                AboutCard {
                    Text(info.awardBadges.first ?? L10n.plural("title.wins", info.wins)).font(.headline)
                    InfoLine(label: L10n.tr("title.award.wins"), value: String(info.wins))
                    InfoLine(label: L10n.tr("title.award.nominations"), value: String(info.nominations))
                }
            }
            if !info.facts.isEmpty {
                AboutCard {
                    Text(L10n.tr("title.information")).font(.headline)
                    ForEach(info.facts) { fact in InfoLine(label: label(fact.kind), value: value(fact)) }
                }
            }
            if !info.links.isEmpty {
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(info.links) { link in
                        Button(link.name) { if let url = URL(string: link.url) { openURL(url) } }
                            .buttonStyle(.bordered)
                    }
                }
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.top, Theme.Spacing.large)
    }

    private func label(_ kind: FactKindValue) -> String {
        switch kind {
        case .network: L10n.tr("title.network")
        case .studio: L10n.tr("title.studio")
        case .released, .releasedyear: L10n.tr("title.fact.released")
        case .aired: L10n.tr("title.fact.aired")
        case .runtime: L10n.tr("title.fact.runtime")
        case .seasons: L10n.tr("title.seasons")
        case .rated: L10n.tr("title.fact.rated")
        case .status: L10n.tr("title.fact.status")
        case .origin: L10n.tr("title.fact.origin")
        case .language: L10n.tr("title.fact.language")
        case .spoken: L10n.tr("title.fact.spoken")
        case .originaltitle: L10n.tr("title.fact.original.title")
        case .alsoknown: L10n.tr("title.fact.also.known")
        case .digital: L10n.tr("title.fact.digital")
        case .disc: L10n.tr("title.fact.disc")
        case .nextepisode: L10n.tr("title.fact.next.episode")
        case .budget: L10n.tr("title.fact.budget")
        case .revenue: L10n.tr("title.fact.revenue")
        }
    }

    private func value(_ fact: TitleFactInfo) -> String {
        switch fact.kind {
        case .released, .digital, .disc: return TitleText.dayText(fact.text)
        case .aired: return fact.end.isEmpty ? L10n.format("title.fact.aired.now", fact.text) : "\(fact.text)–\(fact.end)"
        case .runtime: return TitleText.runtime(Int(fact.number))
        case .seasons: return L10n.plural("title.seasons.count", Int(fact.number)) + " · " + L10n.plural("title.episodes.count", fact.episode)
        case .origin: return fact.text.split(separator: ",").map { TitleText.country(String($0)) }.joined(separator: ", ")
        case .language: return TitleText.language(fact.text)
        case .status: return fact.text.lowercased().capitalizingFirst
        case .nextepisode:
            return [fact.text.isEmpty ? nil : TitleText.dayText(fact.text), TitleText.episodeCode(fact.season, fact.episode)]
                .compactMap { $0 }
                .joined(separator: " · ")
        case .budget, .revenue: return TitleText.money(fact.number)
        default: return fact.text
        }
    }
}

struct AboutCard<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) { content() }
            .padding(Theme.Spacing.large)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}

extension String {
    var capitalizingFirst: String { prefix(1).uppercased() + String(dropFirst()) }
}
