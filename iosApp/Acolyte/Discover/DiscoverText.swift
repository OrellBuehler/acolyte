import Foundation

enum DiscoverText {
    static func mark(_ mark: CardMarkInfo) -> String {
        switch mark.label {
        case .inLibrary: L10n.tr("discover.mark.in.library")
        case .partlyAvailable: L10n.tr("discover.mark.partly")
        case .downloading:
            mark.percent >= 0 ? L10n.format("discover.mark.downloading.percent", mark.percent) : L10n.tr("discover.mark.downloading")
        case .requested: L10n.tr("discover.mark.requested")
        case .approvedWaiting: L10n.tr("discover.mark.approved.waiting")
        case .waitingForCopy: L10n.tr("discover.mark.waiting")
        case .declined: L10n.tr("discover.mark.declined")
        case .hidden: L10n.tr("discover.mark.hidden")
        }
    }

    static func fresh(_ fresh: CardFreshInfo?) -> String? {
        guard let fresh else { return nil }
        switch (fresh.kind, fresh.text) {
        case (.newEpisode, "New Episode"): return L10n.tr("discover.fresh.new.episode")
        case (.newSeason, "New Season"): return L10n.tr("discover.fresh.new.season")
        case (.recentlyAdded, "Recently Added"): return L10n.tr("discover.fresh.recently.added")
        case (.newRelease, "New Release"): return L10n.tr("discover.fresh.new.release")
        case (_, "New series"): return L10n.tr("discover.fresh.new.series")
        case (_, "New episodes"): return L10n.tr("discover.fresh.new.episodes")
        default: return fresh.text
        }
    }

    static func kind(_ kind: CardKindID) -> String {
        switch kind {
        case .movie: L10n.tr("discover.kind.movie")
        case .show: L10n.tr("discover.kind.show")
        case .artist: L10n.tr("discover.kind.artist")
        }
    }

    static func note(_ note: CardNoteInfo) -> String {
        switch note.kind {
        case .downloading: L10n.tr("discover.note.downloading")
        case .missingEpisodes: L10n.plural("discover.note.missing.episodes", note.count)
        case .seasonsUpToDate: L10n.plural("discover.note.seasons.up.to.date", note.count)
        case .missingTracks: L10n.plural("discover.note.missing.tracks", note.count)
        case .albumsUpToDate: L10n.plural("discover.note.albums.up.to.date", note.count)
        case .director: L10n.tr("discover.role.director")
        }
    }

    static func line(_ card: DiscoverCardInfo, typed: Bool = false) -> String {
        let noteText = card.note.map { DiscoverText.note($0) }
        let detail: String
        if let noteText, card.note?.kind == .director, card.year > 0 {
            detail = "\(noteText) · \(card.year)"
        } else if let noteText {
            detail = noteText
        } else if !card.subtitle.isEmpty, card.rowType == "film", card.year > 0 {
            detail = "\(card.subtitle) · \(card.year)"
        } else if !card.subtitle.isEmpty {
            detail = serverWords(card.subtitle)
        } else if card.year > 0 {
            detail = String(card.year)
        } else {
            detail = ""
        }
        guard typed else { return detail }
        let typeText = kind(card.kind)
        return detail.isEmpty || detail == typeText ? typeText : "\(typeText) · \(detail)"
    }

    static func action(_ card: DiscoverCardInfo) -> String? {
        switch card.action {
        case .add: L10n.format("discover.action.add", card.name)
        case .ask: L10n.format("discover.action.ask", card.name)
        case nil: nil
        }
    }

    static func actionShort(_ action: CardActionID) -> String {
        switch action {
        case .add: L10n.tr("discover.action.add.short")
        case .ask: L10n.tr("discover.action.ask.short")
        }
    }

    static func type(_ type: ExploreTypeID) -> String {
        switch type {
        case .all: L10n.tr("discover.type.all")
        case .movies: L10n.tr("discover.type.movies")
        case .shows: L10n.tr("discover.type.shows")
        }
    }

    static func pill(_ pill: HeroPillID, rank: Int = 0, service: String = "") -> String {
        switch pill {
        case .newSeason: L10n.tr("discover.pill.new.season")
        case .pickedForYou: L10n.tr("discover.pill.picked")
        case .newThisWeek: L10n.tr("discover.pill.new.week")
        case .rank: L10n.format("discover.pill.rank", rank, service)
        }
    }

    static func serverWords(_ text: String) -> String {
        switch text {
        case "Movie": L10n.tr("discover.kind.movie")
        case "Show": L10n.tr("discover.kind.show")
        case "Music": L10n.tr("discover.kind.music")
        case "New movie": L10n.tr("discover.line.new.movie")
        case "New show": L10n.tr("discover.line.new.show")
        case "New music": L10n.tr("discover.line.new.music")
        default: text
        }
    }

    static func slug(_ name: String) -> String {
        let lowered = name.lowercased()
        var out = ""
        var gap = false
        for char in lowered {
            if char.isLetter || char.isNumber, char.isASCII {
                if gap, !out.isEmpty { out.append("_") }
                out.append(char)
                gap = false
            } else {
                gap = true
            }
        }
        return out
    }

    static func genre(_ name: String) -> String {
        switch slug(name) {
        case "action": L10n.tr("discover.genre.action")
        case "adventure": L10n.tr("discover.genre.adventure")
        case "animation": L10n.tr("discover.genre.animation")
        case "comedy": L10n.tr("discover.genre.comedy")
        case "crime": L10n.tr("discover.genre.crime")
        case "documentary": L10n.tr("discover.genre.documentary")
        case "drama": L10n.tr("discover.genre.drama")
        case "family": L10n.tr("discover.genre.family")
        case "fantasy": L10n.tr("discover.genre.fantasy")
        case "history": L10n.tr("discover.genre.history")
        case "horror": L10n.tr("discover.genre.horror")
        case "music": L10n.tr("discover.genre.music")
        case "mystery": L10n.tr("discover.genre.mystery")
        case "romance": L10n.tr("discover.genre.romance")
        case "science_fiction": L10n.tr("discover.genre.science.fiction")
        case "thriller": L10n.tr("discover.genre.thriller")
        case "war": L10n.tr("discover.genre.war")
        case "western": L10n.tr("discover.genre.western")
        case "tv_movie": L10n.tr("discover.genre.tv.movie")
        case "action_adventure": L10n.tr("discover.genre.action.adventure")
        case "kids": L10n.tr("discover.genre.kids")
        case "reality": L10n.tr("discover.genre.reality")
        case "sci_fi_fantasy": L10n.tr("discover.genre.sci.fi.fantasy")
        case "war_politics": L10n.tr("discover.genre.war.politics")
        case "news": L10n.tr("discover.genre.news")
        case "soap": L10n.tr("discover.genre.soap")
        case "talk": L10n.tr("discover.genre.talk")
        default: name
        }
    }

    static func stripLabel(_ section: String) -> String? {
        switch section {
        case "genres": L10n.tr("discover.strip.genre")
        case "moods": L10n.tr("discover.strip.mood")
        case "stories": L10n.tr("discover.strip.story")
        case "worlds": L10n.tr("discover.strip.world")
        case "eras": L10n.tr("discover.strip.era")
        case "world": L10n.tr("discover.strip.around")
        case "collections": L10n.tr("discover.strip.collection")
        default: nil
        }
    }

    static func browseSection(_ id: String, fallback: String) -> String {
        switch id {
        case "genres": L10n.tr("discover.browse.genres")
        case "moods": L10n.tr("discover.browse.moods")
        case "stories": L10n.tr("discover.browse.stories")
        case "worlds": L10n.tr("discover.browse.worlds")
        case "eras": L10n.tr("discover.browse.eras")
        case "world": L10n.tr("discover.browse.world")
        case "collections": L10n.tr("discover.browse.collections")
        default: fallback
        }
    }

    static func topTitle(_ type: ExploreTypeID, genre: String?) -> String {
        if let genre { return L10n.format("discover.top.genre", genre) }
        switch type {
        case .movies: return L10n.tr("discover.top.movies")
        case .shows: return L10n.tr("discover.top.shows")
        case .all: return L10n.tr("discover.top.all")
        }
    }

    static func topSource(_ top: TopServicesInfo) -> String {
        switch top.source {
        case .today: L10n.format("discover.top.today", top.serviceName)
        case .asOf: L10n.format("discover.top.as.of", top.serviceName, String(top.fetchedAt.prefix(10)))
        case .popular: L10n.format("discover.top.popular", top.serviceName)
        }
    }

    static func context(_ card: DiscoverCardInfo) -> String? {
        if card.mark != nil, card.mine { return L10n.tr("discover.context.mine") }
        if let mark = card.mark { return DiscoverText.mark(mark) }
        if card.rank > 0, !card.streaming.isEmpty { return L10n.format("discover.context.rank", card.rank, card.streaming) }
        if !card.streaming.isEmpty { return L10n.format("discover.on.service", card.streaming) }
        let reasons: Set<String> = ["because_have", "because_asked", "person", "household"]
        if reasons.contains(card.rowType), !card.rowTitle.isEmpty { return card.rowTitle }
        return fresh(card.fresh)
    }

    static func hiddenLine(_ row: HiddenRowInfo) -> String {
        let what: String =
            switch row.what {
            case .movie: L10n.tr("discover.kind.movie")
            case .show: L10n.tr("discover.kind.show")
            case .movieGenre: L10n.tr("discover.hide.movie.genre")
            case .showGenre: L10n.tr("discover.hide.show.genre")
            }
        let who: String =
            switch row.who {
            case .everyone: L10n.tr("discover.hide.everyone")
            case .person: row.person
            case .removedPerson: L10n.tr("discover.hide.removed.person")
            }
        return "\(what) · \(who)"
    }

    static func shortDate(_ day: String) -> String {
        guard let date = Format.date(iso: day.count == 10 ? day + "T00:00:00Z" : day) else { return day }
        var style = Date.FormatStyle().weekday(.abbreviated).month(.abbreviated).day()
        style.timeZone = TimeZone(identifier: "UTC") ?? .current
        return date.formatted(style)
    }

    static func weekday(_ day: String) -> String {
        guard let date = Format.date(iso: day + "T00:00:00Z") else { return day }
        var style = Date.FormatStyle().weekday(.wide)
        style.timeZone = TimeZone(identifier: "UTC") ?? .current
        return date.formatted(style)
    }
}
