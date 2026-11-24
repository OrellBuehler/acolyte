import Foundation

enum TVSearchText {
    static func scope(_ scope: SearchScopeID) -> String {
        switch scope {
        case .all: L10n.tr("discover.type.all")
        case .people: L10n.tr("discover.scope.people")
        case .movie: L10n.tr("discover.type.movies")
        case .show: L10n.tr("discover.type.shows")
        case .music: L10n.tr("section.music")
        }
    }

    static func people(_ filter: PeopleFilterID) -> String {
        switch filter {
        case .popular: L10n.tr("discover.people.popular")
        case .actors: L10n.tr("discover.people.actors")
        case .directors: L10n.tr("discover.people.directors")
        case .composers: L10n.tr("discover.people.composers")
        }
    }

    static func understood(_ understood: UnderstoodInfo) -> String {
        let kind: String =
            if understood.music {
                L10n.tr("discover.kind.music")
            } else if understood.kind == .movie {
                L10n.tr("discover.type.movies")
            } else if understood.kind == .show {
                L10n.tr("discover.type.shows")
            } else {
                L10n.tr("discover.plain.titles")
            }
        let genres = understood.genres.map(DiscoverText.genre).joined(separator: L10n.tr("discover.plain.or"))
        let head = [genres, kind].filter { !$0.isEmpty }.joined(separator: " ")
        var years = ""
        if understood.yearFrom > 0, understood.yearFrom == understood.yearTo {
            years = String(understood.yearFrom)
        } else if understood.yearFrom > 0, understood.yearTo > 0 {
            years = "\(understood.yearFrom)–\(understood.yearTo)"
        } else if understood.yearFrom > 0 {
            years = L10n.format("discover.plain.from", understood.yearFrom)
        } else if understood.yearTo > 0 {
            years = L10n.format("discover.plain.to", understood.yearTo)
        }
        let people = understood.people.isEmpty ? "" : L10n.format("discover.plain.with", understood.people.joined(separator: ", "))
        let words = understood.words.isEmpty ? "" : L10n.format("discover.plain.about", understood.words.joined(separator: ", "))
        let name = understood.name.isEmpty ? "" : "“\(understood.name)”"
        return [name, head, years, people, words].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    static func rows(_ results: SearchResultsInfo) -> [String: CardRowInfo] {
        var topCards: [DiscoverCardInfo] = []
        for hit in results.top {
            if let card = hit.card { topCards.append(card) }
        }
        let groups: [(String, String, [DiscoverCardInfo])] = [
            ("library", L10n.tr("discover.search.in.library"), results.library),
            ("top", L10n.tr("discover.top.matches"), topCards),
            ("movies", L10n.tr("discover.type.movies"), results.movies),
            ("shows", L10n.tr("discover.type.shows"), results.shows),
            ("plainLibrary", L10n.tr("discover.search.in.library"), results.plainLibrary),
            ("more", L10n.tr("discover.plain.more"), results.more),
        ]
        var result: [String: CardRowInfo] = [:]
        for group in groups where !group.2.isEmpty {
            result[group.0] = CardRowInfo.local(id: group.0, title: group.1, cards: group.2)
        }
        return result
    }
}
