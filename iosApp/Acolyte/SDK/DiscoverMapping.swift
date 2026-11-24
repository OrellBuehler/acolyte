@preconcurrency import shared
import Foundation

extension Mapping {
    static func cardKind(_ value: Any) -> CardKindID {
        CardKindID(rawValue: key(value)) ?? .movie
    }

    static func exploreType(_ value: Any) -> ExploreTypeID {
        ExploreTypeID(rawValue: key(value)) ?? .all
    }

    static func kotlinType(_ type: ExploreTypeID) -> ExploreType {
        switch type {
        case .all: ExploreType.all
        case .movies: ExploreType.movies
        case .shows: ExploreType.shows
        }
    }

    static func kotlinKind(_ kind: CardKindID) -> CardKind {
        switch kind {
        case .movie: CardKind.movie
        case .show: CardKind.show
        case .artist: CardKind.artist
        }
    }

    static func kotlinLink(_ link: BrowseLinkInfo) -> BrowseLink {
        let target: BrowseTarget =
            switch link.target {
            case .category: BrowseTarget.category
            case .mood: BrowseTarget.mood
            case .genre: BrowseTarget.genre
            }
        return BrowseLink(target: target, type: kotlinType(link.type), genre: Int32(link.genre), category: link.category, mood: link.mood)
    }

    static func kotlinScope(_ scope: SearchScopeID) -> SearchScope {
        switch scope {
        case .all: SearchScope.all
        case .people: SearchScope.people
        case .movie: SearchScope.movie
        case .show: SearchScope.show
        }
    }

    static func kotlinPeopleFilter(_ filter: PeopleFilterID) -> PeopleFilter {
        switch filter {
        case .popular: PeopleFilter.popular
        case .actors: PeopleFilter.actors
        case .directors: PeopleFilter.directors
        case .composers: PeopleFilter.composers
        }
    }

    static func kotlinRole(_ role: FilmRoleID) -> FilmRole {
        switch role {
        case .all: FilmRole.all
        case .actor: FilmRole.actor
        case .director: FilmRole.director
        }
    }

    static func kotlinFilmSort(_ sort: FilmSortID) -> FilmSort {
        switch sort {
        case .oldest: FilmSort.oldest
        case .newest: FilmSort.newest
        case .popular: FilmSort.popular
        }
    }

    static func kotlinBoard(_ board: HomeBoardID) -> HomeBoard {
        switch board {
        case .home: HomeBoard.home
        case .system: HomeBoard.system
        }
    }

    static func card(_ c: DiscoverCard) -> DiscoverCardInfo {
        let mark: CardMarkInfo? = c.mark.map { m in
            CardMarkInfo(
                state: MarkStateID(rawValue: key(m.state)) ?? .requested,
                label: MarkLabelID(rawValue: key(m.label)) ?? .requested,
                percent: Int(m.percent)
            )
        }
        let fresh: CardFreshInfo? = c.fresh.flatMap { f in
            FreshKindID(rawValue: key(f.kind)).map { CardFreshInfo(kind: $0, text: f.text) }
        }
        let note: CardNoteInfo? = c.note.flatMap { n in
            NoteKindID(rawValue: key(n.kind)).map { CardNoteInfo(kind: $0, count: Int(n.count)) }
        }
        let action: CardActionID? = c.action.flatMap { CardActionID(rawValue: key($0)) }
        return DiscoverCardInfo(
            id: c.key,
            kind: cardKind(c.kind),
            tmdb: c.tmdb,
            titleId: c.titleId,
            name: c.name,
            year: Int(c.year),
            subtitle: c.subtitle,
            poster: c.poster,
            backdrop: c.backdrop,
            logo: c.logo,
            overview: c.overview,
            genres: c.genres,
            mark: mark,
            fresh: fresh,
            action: action,
            streaming: c.streaming,
            rank: Int(c.rank),
            mine: c.mine,
            rowType: c.rowType,
            rowTitle: c.rowTitle,
            note: note
        )
    }

    static func row(_ r: CardRow) -> CardRowInfo {
        CardRowInfo(
            id: r.id,
            type: r.type,
            title: r.title,
            subtitle: r.subtitle,
            cards: r.cards.map(card),
            wide: r.wide,
            ranked: r.ranked,
            seeAll: r.seeAll
        )
    }

    static func person(_ p: PersonCard) -> PersonCardInfo {
        PersonCardInfo(id: p.id, name: p.name, note: p.note, photo: p.photo)
    }

    static func link(_ l: BrowseLink) -> BrowseLinkInfo {
        BrowseLinkInfo(
            target: BrowseTargetID(rawValue: key(l.target)) ?? .category,
            type: exploreType(l.type),
            genre: Int(l.genre),
            category: l.category,
            mood: l.mood
        )
    }

    static func tile(_ t: BrowseTile, tab: ExploreType) -> BrowseTileInfo {
        BrowseTileInfo(
            id: DiscoverKit.shared.stripKey(tile: t) + "-\(t.kind)-\(t.genre)-\(t.mood)",
            stripKey: DiscoverKit.shared.stripKey(tile: t),
            name: t.name,
            isGenre: t.category.isEmpty && t.mood.isEmpty,
            backdrop: t.backdrop,
            section: t.section,
            hue: Int(DiscoverKit.shared.hue(name: t.name)),
            link: link(DiscoverKit.shared.link(tile: t, tab: tab))
        )
    }

    static func top(_ t: TopServices) -> TopServicesInfo {
        TopServicesInfo(
            chips: t.chips.map { ServiceChipInfo(id: $0.id, name: $0.name) },
            selected: t.selected,
            ranked: t.ranked,
            source: TopSourceID(rawValue: key(t.source)) ?? .popular,
            serviceName: t.serviceName,
            fetchedAt: t.fetchedAt,
            attribution: t.attribution
        )
    }

    static func genreCard(_ g: GenreCard) -> GenreCardInfo {
        let type: ExploreTypeID = g.both ? .all : (g.kind == "show" ? .shows : .movies)
        return GenreCardInfo(id: "\(g.kind)-\(g.id)", genre: Int(g.id), name: g.name, backdrop: g.backdrop, type: type)
    }

    static func pills(_ list: [Any]) -> [HeroPillID] {
        list.compactMap { HeroPillID(rawValue: key($0)) }
    }

    static func explore(_ s: ExploreState) -> ExploreInfo {
        let banner: GenreBannerInfo? = s.banner.map { b in
            let type: ExploreTypeID = b.both ? .all : (key(b.genre.kind) == "show" ? .shows : .movies)
            return GenreBannerInfo(genre: Int(b.genre.id), name: b.genre.name, type: type, have: Int(b.have), picks: b.picks.map(card))
        }
        return ExploreInfo(
            load: load(s.status),
            type: exploreType(s.type),
            hero: s.hero.first.map(card),
            heroPills: pills(s.heroPills),
            banner: banner,
            strip: s.strip.map { tile($0, tab: s.type) },
            rows: s.rows.map(row),
            top: s.top.map(top),
            genres: s.genres.map(genreCard),
            more: s.more,
            loadingMore: s.loadingMore,
            sourceOff: s.sourceOff,
            canHide: s.canHide,
            canSeeStats: s.canSeeStats,
            isEmpty: s.isEmpty
        )
    }

    static func genrePage(_ s: GenreState) -> GenrePageInfo {
        GenrePageInfo(
            load: load(s.status),
            name: s.name,
            canHide: s.canHide,
            hero: s.hero.first.map(card),
            rows: s.rows.map(row),
            top: s.top.map(top),
            isEmpty: s.isEmpty
        )
    }

    static func popular(_ s: PopularState) -> PopularInfo {
        PopularInfo(
            load: load(s.status),
            type: exploreType(s.type),
            hero: s.hero.map(card),
            heroPills: pills(s.heroPills),
            heroRank: Int(s.heroRank),
            heroService: s.heroService,
            days: s.days.map { day in
                RailDayInfo(
                    day: day.day,
                    offset: Int(day.offset),
                    entries: day.entries.map { entry in
                        RailEntryInfo(
                            card: card(entry.card),
                            what: WhatKindID(rawValue: key(entry.what)) ?? .other,
                            text: entry.text,
                            alsoComing: entry.alsoComing
                        )
                    }
                )
            },
            rows: s.rows.map { r in
                PopularRowInfo(
                    row: row(r.row),
                    chips: r.chips.map { ServiceChipInfo(id: $0.id, name: $0.name) },
                    selected: r.selected,
                    loading: r.loading
                )
            },
            more: s.more,
            loadingMore: s.loadingMore,
            sourceOff: s.sourceOff,
            canHide: s.canHide,
            isEmpty: s.isEmpty
        )
    }

    static func hit(_ h: SearchHit) -> SearchHitInfo {
        SearchHitInfo(card: h.card.map(card), person: h.person.map(person))
    }

    static func results(_ r: SearchResults) -> SearchResultsInfo {
        let understood: UnderstoodInfo? = r.understood.map { u in
            UnderstoodInfo(
                name: u.name,
                genres: u.genres,
                kind: u.kind.map(cardKind),
                music: u.music,
                yearFrom: Int(u.yearFrom),
                yearTo: Int(u.yearTo),
                people: u.people,
                words: u.words
            )
        }
        return SearchResultsInfo(
            searched: r.searched,
            library: r.library.map(card),
            top: r.top.map(hit),
            movies: r.movies.map(card),
            shows: r.shows.map(card),
            people: r.people.map(person),
            understood: understood,
            plainLibrary: r.plainLibrary.map(card),
            more: r.more.map(card),
            metadataDown: r.metadataDown,
            error: error(r.error),
            isEmpty: r.isEmpty
        )
    }

    static func search(_ s: SearchState) -> SearchInfo {
        SearchInfo(
            query: s.query,
            scope: SearchScopeID(rawValue: key(s.scope)) ?? .all,
            scopes: s.scopes.compactMap { SearchScopeID(rawValue: key($0)) },
            onlyLibrary: s.onlyLibrary,
            recents: s.recents,
            completions: s.completions,
            suggestions: s.suggestions.map { g in
                SuggestionInfo(
                    kind: SuggestKindID(rawValue: key(g.kind)) ?? .movie,
                    name: g.name,
                    sub: g.sub,
                    art: g.art,
                    state: SuggestStateID(rawValue: key(g.state)) ?? .none,
                    tmdb: g.tmdb,
                    personId: g.personId
                )
            },
            searching: s.searching,
            results: s.shown.map(results),
            canManage: s.canManage,
            canHide: s.canHide
        )
    }

    static func browse(_ s: BrowseGridState) -> BrowseInfo {
        BrowseInfo(
            load: load(s.status),
            tab: exploreType(s.tab),
            tiles: s.tiles.map { tile($0, tab: s.tab) },
            sections: s.sections.map { section in
                BrowseSectionInfo(
                    id: section.id,
                    title: section.title,
                    tiles: section.tiles.map { tile($0, tab: s.tab) },
                    loaded: section.loaded,
                    building: section.building
                )
            }
        )
    }

    static func browseView(_ s: BrowseViewState) -> BrowseViewInfo {
        BrowseViewInfo(
            load: load(s.status),
            title: s.title,
            genreName: s.genreName,
            type: exploreType(s.type),
            cards: s.cards.map(card),
            canHide: s.canHide,
            isEmpty: s.isEmpty
        )
    }

    static func peopleBrowse(_ s: PeopleBrowseState) -> PeopleBrowseInfo {
        PeopleBrowseInfo(
            load: load(s.status),
            filter: PeopleFilterID(rawValue: key(s.filter)) ?? .popular,
            filters: s.filters.compactMap { PeopleFilterID(rawValue: key($0)) },
            shelves: s.shelves.map { PeopleShelfInfo(id: $0.id, title: $0.title, people: $0.people.map(person)) },
            popular: s.popular.map(person),
            more: s.more,
            loadingMore: s.loadingMore,
            attribution: s.attribution
        )
    }

    static func personPage(_ s: PersonState) -> PersonPageInfo {
        PersonPageInfo(
            load: load(s.status),
            name: s.name,
            photo: s.photo,
            roles: s.roles,
            firstYear: Int(s.firstYear),
            films: Int(s.films),
            biography: s.about.biography,
            birthday: s.about.birthday,
            deathday: s.about.deathday,
            birthplace: s.about.birthplace,
            department: s.about.department,
            knownFor: s.knownFor.map(card),
            comingSoon: s.comingSoon.map(card),
            inLibrary: s.inLibrary.map(card),
            asked: s.asked.map(card),
            decades: s.decades.map { DecadeInfo(year: Int($0.year), cards: $0.cards.map(card)) },
            oftenWith: s.oftenWith.map(person),
            role: FilmRoleID(rawValue: key(s.role)) ?? .all,
            sort: FilmSortID(rawValue: key(s.sort)) ?? .oldest,
            limitUsed: s.limit.map { Int($0.used) },
            limitLeft: s.limit.map { Int($0.left) },
            reloading: s.reloading,
            attribution: s.attribution,
            canHide: s.canHide
        )
    }

    static func seriesMovie(_ m: SeriesMovie) -> SeriesMovieInfo {
        SeriesMovieInfo(
            id: m.id,
            name: m.name,
            year: Int(m.year),
            versions: Int(m.versions),
            files: Int(m.files),
            isProtected: m.hasProtected,
            versionIds: m.versionIds.map { $0.int64Value },
            fileIds: m.fileIds.map { $0.int64Value }
        )
    }

    static func series(_ s: SeriesState) -> SeriesInfo {
        SeriesInfo(
            load: load(s.status),
            name: s.name,
            movies: s.movies.map(seriesMovie),
            selected: Set(s.selected.map { $0.int64Value }),
            deleteFiles: s.deleteFiles,
            neverAdd: s.neverAdd,
            acknowledge: s.acknowledge,
            review: s.review.map { rows in rows.map { SeriesReviewRowInfo(movie: seriesMovie($0.movie), skip: $0.skip) } },
            results: s.results.map { rows in
                rows.map { SeriesOutcomeInfo(name: $0.name, outcome: SeriesOutcomeID(rawValue: key($0.outcome)) ?? .failed, why: $0.why) }
            },
            busy: s.busy,
            actionError: error(s.actionError),
            canManage: s.canManage,
            showDetails: s.showDetails,
            needsAcknowledge: s.needsAcknowledge,
            canReview: s.canReview,
            isEmpty: s.isEmpty
        )
    }

    static func hiddenRow(_ r: HiddenRow) -> HiddenRowInfo {
        HiddenRowInfo(
            id: r.id,
            name: r.name,
            what: HideWhatID(rawValue: key(r.what)) ?? .movie,
            who: HideForID(rawValue: key(r.who)) ?? .everyone,
            person: r.person,
            isGenre: r.genre
        )
    }

    static func hidden(_ s: HiddenState) -> HiddenInfo {
        HiddenInfo(load: load(s.status), rows: s.rows.map(hiddenRow), actionError: error(s.actionError), isEmpty: s.isEmpty)
    }

    static func hideSheet(_ s: HideSheetState) -> HideSheetInfo {
        HideSheetInfo(
            load: load(s.status),
            genres: s.genres,
            hidden: s.hidden.map(hiddenRow),
            people: s.people.map { HidePersonInfo(id: $0.id, name: $0.name) },
            genre: s.genre,
            everyone: s.everyone,
            personId: s.personId,
            saving: s.saving,
            needsPerson: s.needsPerson,
            error: error(s.error)
        )
    }

    static func exploreStats(_ s: ExploreStatsState) -> ExploreStatsInfo {
        ExploreStatsInfo(
            load: load(s.status),
            rows: s.rows.map { RowStatInfo(rowType: $0.rowType, title: $0.title, shown: Int($0.shown), opens: Int($0.opens), asks: Int($0.asks)) },
            layers: s.layers.map {
                LayerStatInfo(layer: $0.layer, shown: Int($0.shown), opens: Int($0.opens), asks: Int($0.asks), rate: $0.rate)
            },
            isEmpty: s.isEmpty
        )
    }

    static func section(_ value: Any?) -> SectionID? {
        guard let value else { return nil }
        return SectionID(rawValue: key(value))
    }

    static func attentionRow(_ r: AttentionRow) -> AttentionRowInfo {
        AttentionRowInfo(
            id: r.key,
            tone: tone(r.tone),
            kind: AttentionKindID(rawValue: key(r.kind)) ?? .health,
            count: Int(r.count),
            title: r.title,
            checkKind: r.checkKind,
            error: r.error,
            names: r.names,
            target: AttentionTargetID(rawValue: key(r.target)) ?? .health
        )
    }

    static func partCount(_ p: PartCount?) -> PartCountInfo? {
        guard let p else { return nil }
        return PartCountInfo(total: Int(p.total), working: Int(p.working), off: Int(p.off), troubled: p.troubled)
    }

    static func systemDownload(_ d: SystemDownload) -> SystemDownloadInfo {
        SystemDownloadInfo(
            id: d.id,
            name: d.name,
            stage: DownloadStageID(rawValue: key(d.stage)) ?? .done,
            progress: d.progress,
            speed: d.speed,
            finishedAt: Format.date(iso: d.finishedAt)
        )
    }

    static func step(_ s: SetupStep) -> SetupStepInfo {
        SetupStepInfo(kind: SetupStepKindID(rawValue: key(s.kind)) ?? .title, done: s.done, target: section(s.target))
    }

    static func home(_ s: HomeState) -> HomeInfo {
        let board: SystemBoardInfo? = s.systemBoard.map { b in
            SystemBoardInfo(
                speed: b.speed,
                waiting: Int(b.waiting),
                downloading: Int(b.downloading),
                importing: Int(b.importing),
                running: b.running.map(systemDownload),
                finished: b.finished.map(systemDownload),
                paused: Int(b.paused),
                notSent: Int(b.notSent),
                downloadsFailed: b.downloadsFailed,
                attention: b.attention.map(attentionRow),
                passed: Int(b.passed),
                unchecked: b.unchecked,
                sources: partCount(b.sources),
                downloaders: partCount(b.downloaders),
                sourcesOff: b.sourcesOff,
                uptimeSeconds: b.uptimeSeconds,
                version: b.version
            )
        }
        let joined: JoinedWelcomeInfo? = s.joined.map { j in
            JoinedWelcomeInfo(
                name: j.name,
                canAsk: j.canAsk,
                kinds: j.kinds.map(cardKind),
                noLimit: j.noLimit,
                limits: j.limits.map { KindLimitInfo(kind: cardKind($0.kind), noLimit: $0.noLimit, perWeek: Int($0.perWeek), left: Int($0.left)) }
            )
        }
        let news: HomeNewsInfo? = s.news.flatMap { n in
            NewsKindID(rawValue: key(n.kind)).map { HomeNewsInfo(kind: $0, count: Int(n.count), title: n.title) }
        }
        return HomeInfo(
            load: load(s.status),
            greeting: GreetingTimeID(rawValue: key(s.greeting)) ?? .morning,
            name: s.name,
            news: news,
            system: s.system,
            board: HomeBoardID(rawValue: key(s.board)) ?? .home,
            owner: s.owner,
            finishSetup: s.finishSetup,
            joined: joined,
            welcome: s.welcome,
            steps: s.steps.map(step),
            stepsDone: Int(s.stepsDone),
            nextStep: s.nextStep.map(step),
            pictures: s.pictures.map {
                HomePictureInfo(
                    card: card($0.card),
                    label: PictureLabelID(rawValue: key($0.label)) ?? .arrived,
                    arrived: ArrivedTitleID(rawValue: key($0.arrived)) ?? .justArrived
                )
            },
            tiles: s.tiles.compactMap { t in
                HomeTileKindID(rawValue: key(t.kind)).map { HomeTileInfo(kind: $0, target: section(t.target)) }
            },
            attention: s.attention.map { a in
                AttentionTileInfo(rows: a.rows.map(attentionRow), worst: tone(a.worst), unchecked: a.unchecked, more: Int(a.more))
            },
            asks: s.asks.map { a in
                AsksTileInfo(approver: a.approver, waiting: Int(a.waiting), posters: a.posters, open: Int(a.open), declined: Int(a.declined), failed: a.failed)
            },
            library: s.library.map { LibraryKindInfo(kind: cardKind($0.kind), titles: Int($0.titles), parts: Int($0.parts), items: Int($0.items)) },
            libraryTotal: Int(s.libraryTotal),
            disk: s.disk.map { d in
                DiskTileInfo(size: d.size, free: d.free?.free, many: d.free?.many ?? false, tone: tone(d.tone), failed: d.failed)
            },
            downloads: s.downloads.map { d in
                DownloadsTileInfo(
                    fetching: Int(d.fetching),
                    downloading: Int(d.downloading),
                    importing: Int(d.importing),
                    waiting: Int(d.waiting),
                    paused: Int(d.paused)
                )
            },
            week: s.week.map { WeekTileInfo(count: Int($0.count), posters: $0.posters, borrowed: $0.borrowed) },
            newArrivals: Int(s.newArrivals),
            newSeasons: Int(s.newSeasons),
            popularArt: s.popularArt,
            popularBuilding: s.popularBuilding,
            arrived: s.arrived.map(row),
            arrivedTitle: ArrivedTitleID(rawValue: key(s.arrivedTitle)) ?? .justArrived,
            comingUp: s.comingUp.map { u in
                ComingUpInfo(
                    id: u.key,
                    tmdb: u.tmdb,
                    kind: u.kind,
                    name: u.name,
                    image: u.image,
                    sub: u.sub,
                    when: UpWhenID(rawValue: key(u.whenKind)) ?? .text,
                    date: u.date,
                    text: u.text
                )
            },
            borrowed: s.borrowed.map(row),
            systemBoard: board,
            settled: s.settled
        )
    }
}
