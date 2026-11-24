import Foundation

enum Route: Hashable, Sendable, Identifiable {
    case title(kind: String, tmdb: String)
    case libraryTitle(id: Int64, kind: String, tmdb: String)
    case person(id: String)
    case collection(id: String)
    case libraryTools
    case libraryDeleted(titleId: Int64)
    case libraryCopies
    case libraryReleases(turnedDown: Bool)
    case librarySubtitles
    case libraryRename
    case libraryImport
    case libraryImportFiles(path: String)
    case settingsLibraryFolders
    case settingsSources
    case settingsSourceHealth(id: Int64)
    case settingsDownloaders
    case settingsPickRules
    case settingsPickRuleEdit(id: Int64, from: Int64)
    case settingsNaming
    case settingsSubtitles
    case settingsSearch
    case settingsMove
    case libraryTab(id: Int64, kind: String, tmdb: String)
    case downloads(show: String, open: String)
    case history(titleId: Int64, episodeId: Int64)
    case ask(kind: String, tmdb: String, fourK: Bool = false)
    case askMusic(artist: String, name: String = "", album: String = "", edition: String = "")
    case reports
    case reportProblem(titleId: Int64, name: String = "", season: Int = -1, itemId: Int64 = 0, albumId: Int64 = 0, versionId: Int64 = 0)
    case settingsRequests
    case settingsOutsideLists
    case settingsNeverAdd
    case settingsSignIn
    case settingsApi
    case settingsAi
    case settingsRemoteAccess
    case settingsAppearance
    case settingsTitleDefaults
    case settingsMetadata
    case settingsAlerts
    case settingsBackups
    case settingsLogs
    case settingsHealth
    case settingsParts
    case settingsThisApp
    case settingsDanger
    case settingsSchedules
    case settingsMediaServers
    case genre(type: ExploreTypeID, genre: Int)
    case browse(BrowseLinkInfo)
    case hidden
    case exploreStats
    case musicArtist(mbid: String)
    case musicAlbum(mbid: String, artist: String)
    case musicGenre(slug: String)
    case musicPage(kind: String, slug: String)
    case musicLibraryArtist(titleId: Int64, tab: String)
    case musicLibraryAlbum(titleId: Int64, albumId: Int64)
    case musicAdd(mbid: String, name: String)
    case settingsMusic

    var id: Self { self }

    var isSheet: Bool {
        switch self {
        case .ask, .askMusic, .reportProblem, .musicAdd: true
        default: false
        }
    }
}
