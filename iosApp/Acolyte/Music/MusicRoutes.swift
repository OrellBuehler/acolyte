import Foundation

enum MusicOpenKindID: String, Hashable, Sendable {
    case album
    case artist
    case genre
    case page
    case libraryalbum
    case libraryartist
    case search
    case none
}

struct MusicTargetInfo: Hashable, Sendable {
    let kind: MusicOpenKindID
    let mbid: String
    let artist: String
    let titleId: Int64
    let partId: Int64
    let page: String
    let slug: String
    let query: String
}

extension Route {
    static func music(_ target: MusicTargetInfo) -> Route? {
        switch target.kind {
        case .album: .musicAlbum(mbid: target.mbid, artist: target.artist)
        case .artist: target.mbid.isEmpty ? nil : .musicArtist(mbid: target.mbid)
        case .genre: .musicGenre(slug: target.slug)
        case .page: .musicPage(kind: target.page, slug: target.slug)
        case .libraryalbum: .musicLibraryAlbum(titleId: target.titleId, albumId: target.partId)
        case .libraryartist: .musicLibraryArtist(titleId: target.titleId, tab: "")
        case .search, .none: nil
        }
    }
}
