import Foundation

extension DlTitleLink {
    var route: Route {
        if libraryId > 0 {
            return openLibrary ? .libraryTab(id: libraryId, kind: kind, tmdb: tmdb) : .libraryTitle(id: libraryId, kind: kind, tmdb: tmdb)
        }
        return .title(kind: kind, tmdb: tmdb)
    }
}
