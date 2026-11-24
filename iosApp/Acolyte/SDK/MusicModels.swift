import Foundation

enum MusicMarkKind: String, Hashable, Sendable {
    case none
    case complete
    case partial
    case downloading
    case missing
    case requested
    case approved
}

enum MusicAlbumKindID: String, Hashable, Sendable {
    case album
    case ep
    case single
    case broadcast
    case compilation
    case live
    case soundtrack
    case remix
    case other
}

enum MusicShelfKindID: String, Hashable, Sendable {
    case albums
    case singles
    case compilations
    case live
    case other
}

enum MusicShapeKind: String, Hashable, Sendable {
    case square
    case round
    case tile
    case song
    case grid
    case wide
}

enum MusicCardKindID: String, Hashable, Sendable {
    case album
    case artist
    case song
    case genre
}

enum MusicLineKindID: String, Hashable, Sendable {
    case none
    case text
    case outOn = "outon"
    case outNow = "outnow"
    case releasedOn = "releasedon"
    case listens
    case arrivedOn = "arrivedon"
    case artistYear = "artistyear"
    case artistText = "artisttext"
    case asked
    case approved
    case turnedDown = "turneddown"
    case arrived
}

enum MusicNoteKindID: String, Hashable, Sendable {
    case failed
    case stillReading = "stillreading"
    case followFailed = "followfailed"
    case askedRequested = "askedrequested"
    case askedApproved = "askedapproved"
    case askedJoined = "askedjoined"
    case askedAlready = "askedalready"
    case added
    case alreadyInLibrary = "alreadyinlibrary"
    case notFound = "notfound"
    case keepAlbumOn = "keepalbumon"
    case keepAlbumOff = "keepalbumoff"
    case keepArtistOn = "keepartiston"
    case keepArtistOff = "keepartistoff"
    case searching
    case searchingAll = "searchingall"
    case kindsSaved = "kindssaved"
    case refreshing
    case moving
    case pressingChanged = "pressingchanged"
    case pressingFailed = "pressingfailed"
    case settingsSaved = "settingssaved"
    case matched
    case notMatched = "notmatched"
    case unmatched
}

enum MusicProblemKind: String, Hashable, Sendable {
    case none
    case offline
    case notFound = "notfound"
    case badLink = "badlink"
    case forbidden
    case slow
}

enum MusicArtistKindID: String, Hashable, Sendable {
    case solo
    case group
    case artist
}

enum MusicMainKind: String, Hashable, Sendable {
    case none
    case openLibrary = "openlibrary"
    case add
    case ask
    case asked
}

enum MusicHaveKind: String, Hashable, Sendable {
    case unknown
    case inLibrary = "inlibrary"
    case downloading
    case missing
}

enum MusicAlbumCtaKind: String, Hashable, Sendable {
    case none
    case openLibrary = "openlibrary"
    case manageLibrary = "managelibrary"
    case add
    case ask
    case asked
    case openArtist = "openartist"
}

enum MusicCardActionKind: String, Hashable, Sendable {
    case none
    case add
    case ask
}

enum MusicAddOutcomeKind: String, Hashable, Sendable {
    case added
    case already
}

enum MusicAlbumStateID: String, Hashable, Sendable {
    case notIncluded = "notincluded"
    case noTracks = "notracks"
    case complete
    case notOutYet = "notoutyet"
    case notKept = "notkept"
    case partial
    case missing
}

enum MusicLibGroupID: String, Hashable, Sendable, CaseIterable {
    case albums
    case eps
    case singles
    case live
    case compilations
    case other
}

enum MusicLibFilterKind: String, Hashable, Sendable, CaseIterable {
    case included
    case leftOut = "leftout"
    case all
}

enum MusicKindsPresetKind: String, Hashable, Sendable {
    case albums
    case albumsEps = "albumseps"
    case albumsEpsSingles = "albumsepssingles"
    case custom
}

enum MusicQuickRuleKind: String, Hashable, Sendable {
    case any
    case lossy
    case lossless
}

struct MusicNoteInfo: Hashable, Sendable, Identifiable {
    let id: Int
    let kind: MusicNoteKindID
    let text: String
    let count: Int
    let error: ErrorInfo?
}

struct MusicLineInfo: Hashable, Sendable {
    let kind: MusicLineKindID
    let artist: String
    let text: String
    let date: String
    let number: Int64

    static let none = MusicLineInfo(kind: .none, artist: "", text: "", date: "", number: 0)
}

struct MusicAccessInfo: Hashable, Sendable {
    let manage: Bool
    let ask: Bool
    let show: Bool
    let settings: Bool
    let kid: Bool
    let handsOff: Bool

    static let none = MusicAccessInfo(manage: false, ask: false, show: false, settings: false, kid: false, handsOff: false)
}

struct MusicOptionInfo: Hashable, Sendable, Identifiable {
    let id: String
    let key: String
}

struct MusicAlbumInfo: Hashable, Sendable, Identifiable {
    let mbid: String
    let name: String
    let kind: MusicAlbumKindID
    let year: String
    let date: String
    let image: String
    let explicit: Bool
    let upcoming: Bool
    let mark: MusicMarkKind
    let following: Bool
    let partId: Int64
    let artistMbid: String
    let artistName: String
    let asked: Bool

    var id: String { mbid }
}

struct MusicShelfInfo: Hashable, Sendable, Identifiable {
    let kind: MusicShelfKindID
    let albums: [MusicAlbumInfo]

    var id: MusicShelfKindID { kind }
}

struct MusicSongInfo: Hashable, Sendable, Identifiable {
    let title: String
    let album: String
    let albumMbid: String
    let year: String
    let length: String
    let image: String
    let explicit: Bool

    var id: String { "\(albumMbid)-\(title)-\(length)" }
}

struct MusicStoreItemInfo: Hashable, Sendable, Identifiable {
    let title: String
    let year: String
    let kind: MusicAlbumKindID
    let tracks: Int
    let image: String
    let explicit: Bool

    var id: String { "\(title)-\(year)" }
}

struct MusicVideoInfo: Hashable, Sendable, Identifiable {
    let id: String
    let title: String
    let length: String
    let thumb: String
    let url: String
}

struct MusicArtistCardInfo: Hashable, Sendable, Identifiable {
    let mbid: String
    let name: String
    let image: String
    let note: String

    var id: String { mbid }
}

struct MusicBioInfo: Hashable, Sendable {
    let text: String
    let url: String
    let license: String
    let licenseUrl: String
    let attribution: String
}

struct MusicAskAllInfo: Hashable, Sendable {
    let count: Int
    let gb: Bool
    let size: String
}

struct MusicArtistInfo: Hashable, Sendable {
    let load: LoadInfo
    let mbid: String
    let name: String
    let image: String
    let wide: String
    let tint: Int64
    let kind: MusicArtistKindID
    let country: String
    let disambiguation: String
    let ended: Bool
    let genres: [String]
    let listeners: Int64
    let rank: Int
    let change: Int64
    let plays: Int64
    let titleId: Int64
    let followKnown: Bool
    let following: Bool
    let bio: MusicBioInfo?
    let top: [MusicSongInfo]
    let essential: [MusicAlbumInfo]
    let shelves: [MusicShelfInfo]
    let storeAlbums: [MusicStoreItemInfo]
    let coming: [MusicAlbumInfo]
    let videos: [MusicVideoInfo]
    let appearsOn: [MusicAlbumInfo]
    let related: [MusicArtistCardInfo]
    let similar: [MusicArtistCardInfo]
    let similarLoading: Bool
    let albumCount: Int
    let singleCount: Int
    let activeSince: String
    let activeUntil: String
    let gap: String
    let incomplete: Bool
    let lookupLoaded: Bool
    let asked: Bool
    let askedWhole: Bool
    let neverAdd: Bool
    let canAdd: Bool
    let canAsk: Bool
    let askAll: MusicAskAllInfo?
    let asking: Bool
    let note: MusicNoteInfo?
    let main: MusicMainKind
    let mainReady: Bool
    let problem: MusicProblemKind
    let hasDiscography: Bool

    static let initial = MusicArtistInfo(
        load: .loading, mbid: "", name: "", image: "", wide: "", tint: 0, kind: .artist, country: "", disambiguation: "", ended: false,
        genres: [], listeners: 0, rank: 0, change: 0, plays: 0, titleId: 0, followKnown: false, following: false, bio: nil, top: [],
        essential: [], shelves: [], storeAlbums: [], coming: [], videos: [], appearsOn: [], related: [], similar: [], similarLoading: true,
        albumCount: 0, singleCount: 0, activeSince: "", activeUntil: "", gap: "", incomplete: false, lookupLoaded: false, asked: false,
        askedWhole: false, neverAdd: false, canAdd: false, canAsk: false, askAll: nil, asking: false, note: nil, main: .none,
        mainReady: false, problem: .none, hasDiscography: false
    )
}

struct MusicTrackRowInfo: Hashable, Sendable, Identifiable {
    let disc: Int
    let number: Int
    let name: String
    let length: String
    let explicit: Bool
    let have: MusicHaveKind

    var id: String { "\(disc)-\(number)-\(name)" }
}

struct MusicEditionInfo: Hashable, Sendable, Identifiable {
    let mbid: String
    let label: String
    let tracks: Int

    var id: String { mbid }
}

struct MusicAlbumPageInfo: Hashable, Sendable {
    let load: LoadInfo
    let mbid: String
    let artistMbid: String
    let name: String
    let artistName: String
    let kind: MusicAlbumKindID
    let year: String
    let date: String
    let image: String
    let tint: Int64
    let explicit: Bool
    let disambiguation: String
    let label: String
    let songs: Int
    let minutes: Int
    let tracks: [MusicTrackRowInfo]
    let discs: Int
    let tracksWaiting: Bool
    let gap: String
    let editions: [MusicEditionInfo]
    let edition: String
    let titleId: Int64
    let partId: Int64
    let inLibraryAlbum: Bool
    let mark: MusicMarkKind
    let have: Int
    let missing: Int
    let moreBy: [MusicAlbumInfo]
    let near: [MusicAlbumInfo]
    let similar: [MusicArtistCardInfo]
    let followKnown: Bool
    let following: Bool
    let lookupLoaded: Bool
    let asked: Bool
    let canAdd: Bool
    let canAsk: Bool
    let busy: Bool
    let note: MusicNoteInfo?
    let cta: MusicAlbumCtaKind
    let ctaReady: Bool
    let problem: MusicProblemKind
    let showCounts: Bool

    static let initial = MusicAlbumPageInfo(
        load: .loading, mbid: "", artistMbid: "", name: "", artistName: "", kind: .album, year: "", date: "", image: "", tint: 0,
        explicit: false, disambiguation: "", label: "", songs: 0, minutes: 0, tracks: [], discs: 1, tracksWaiting: true, gap: "",
        editions: [], edition: "", titleId: 0, partId: 0, inLibraryAlbum: false, mark: .none, have: 0, missing: 0, moreBy: [], near: [],
        similar: [], followKnown: false, following: false, lookupLoaded: false, asked: false, canAdd: false, canAsk: false, busy: false,
        note: nil, cta: .none, ctaReady: false, problem: .none, showCounts: false
    )
}

struct MusicItemInfo: Hashable, Sendable, Identifiable {
    let key: String
    let kind: MusicCardKindID
    let mbid: String
    let name: String
    let artist: String
    let artistMbid: String
    let titleId: Int64
    let partId: Int64
    let image: String
    let images: [String]
    let cover: String
    let line: MusicLineInfo
    let mark: MusicMarkKind
    let fresh: String
    let kicker: String
    let badge: String
    let explicit: Bool
    let upcoming: Bool
    let following: Bool
    let rank: Int
    let hue: Int
    let color: String
    let target: MusicTargetInfo
    let action: MusicCardActionKind
    let date: String
    let track: String

    var id: String { key }
}

struct MusicShelfRowInfo: Hashable, Sendable, Identifiable {
    let id: String
    let type: String
    let title: String
    let subtitle: String
    let shape: MusicShapeKind
    let items: [MusicItemInfo]
    let regional: Bool
}

struct MusicSimilarInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let target: MusicTargetInfo

    var id: String { key }
}

struct MusicRowsInfo: Hashable, Sendable {
    let load: LoadInfo
    let kind: String
    let slug: String
    let tileKind: String
    let title: String
    let hue: Int
    let color: String
    let collage: [String]
    let leads: [String]
    let hero: MusicItemInfo?
    let spotlight: [MusicItemInfo]
    let strip: [MusicItemInfo]
    let grid: [MusicShelfRowInfo]
    let rows: [MusicShelfRowInfo]
    let region: String
    let regions: [String]
    let cold: Bool
    let thin: Bool
    let similar: [MusicSimilarInfo]
    let kid: Bool
    let partial: Bool
    let more: Bool
    let loadingMore: Bool
    let canAdd: Bool
    let canAsk: Bool
    let isEmpty: Bool
    let isPage: Bool
    let bandHue: Int

    static let initial = MusicRowsInfo(
        load: .loading, kind: "", slug: "", tileKind: "", title: "", hue: 0, color: "", collage: [], leads: [], hero: nil, spotlight: [],
        strip: [], grid: [], rows: [], region: "", regions: [], cold: false, thin: false, similar: [], kid: false, partial: false,
        more: false, loadingMore: false, canAdd: false, canAsk: false, isEmpty: false, isPage: false, bandHue: 0
    )
}

struct MusicTileInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let target: MusicTargetInfo
    let color: String
    let hue: Int
    let covers: [String]

    var id: String { key }
}

struct MusicTileSectionInfo: Hashable, Sendable, Identifiable {
    let id: String
    let title: String
    let tiles: [MusicTileInfo]
}

struct MusicNewInfo: Hashable, Sendable {
    let load: LoadInfo
    let kid: Bool
    let building: Bool
    let hero: [MusicItemInfo]
    let rows: [MusicShelfRowInfo]
    let tiles: [MusicTileInfo]
    let genres: [String]
    let canAdd: Bool
    let canAsk: Bool
    let note: MusicNoteInfo?
    let isEmpty: Bool

    static let initial = MusicNewInfo(
        load: .loading, kid: false, building: false, hero: [], rows: [], tiles: [], genres: [], canAdd: false, canAsk: false, note: nil,
        isEmpty: false
    )
}

struct MusicBrowseInfo: Hashable, Sendable {
    let load: LoadInfo
    let kid: Bool
    let sections: [MusicTileSectionInfo]
    let tiles: [MusicTileInfo]
    let isEmpty: Bool

    static let initial = MusicBrowseInfo(load: .loading, kid: false, sections: [], tiles: [], isEmpty: false)
}

struct MusicArtistHitInfo: Hashable, Sendable, Identifiable {
    let mbid: String
    let name: String
    let type: String
    let country: String
    let disambiguation: String
    let image: String
    let titleId: Int64
    let asked: Bool
    let askedByMe: Bool
    let action: MusicCardActionKind
    let tooAction: Bool

    var id: String { mbid }
}

struct MusicSearchInfo: Hashable, Sendable {
    let query: String
    let searching: Bool
    let searched: String
    let hits: [MusicArtistHitInfo]
    let error: ErrorInfo?

    static let initial = MusicSearchInfo(query: "", searching: false, searched: "", hits: [], error: nil)
}

struct MusicFolderInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let label: String
    let path: String
    let defaultRules: Int64
}

struct MusicRulesOptionInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
}

struct MusicAddInfo: Hashable, Sendable {
    let load: LoadInfo
    let mbid: String
    let name: String
    let disambiguation: String
    let folders: [MusicFolderInfo]
    let rules: [MusicRulesOptionInfo]
    let folderId: Int64
    let rulesId: Int64
    let handsOff: Bool
    let albumChoice: String
    let newAlbums: String
    let albumTypes: [String]
    let secondaryTypes: [String]
    let statuses: [String]
    let keep: Bool
    let follow: Bool
    let titleId: Int64
    let neverAdd: Bool
    let busy: Bool
    let error: ErrorInfo?
    let outcome: MusicAddOutcomeKind?
    let addedTitleId: Int64
    let noFolder: Bool
    let canSubmit: Bool

    static let initial = MusicAddInfo(
        load: .loading, mbid: "", name: "", disambiguation: "", folders: [], rules: [], folderId: 0, rulesId: 0, handsOff: false,
        albumChoice: "all", newAlbums: "all", albumTypes: ["album", "ep"], secondaryTypes: ["studio"], statuses: ["official"], keep: true,
        follow: true, titleId: 0, neverAdd: false, busy: false, error: nil, outcome: nil, addedTitleId: 0, noFolder: false, canSubmit: false
    )
}

struct MusicSettingsInfo: Hashable, Sendable {
    let load: LoadInfo
    let albumChoice: String
    let newAlbums: String
    let albumTypes: [String]
    let secondaryTypes: [String]
    let statuses: [String]
    let pressingRule: String
    let url: String
    let hours: String
    let canChange: Bool
    let saving: Bool
    let error: ErrorInfo?
    let justSaved: Bool
    let dirty: Bool
    let emptyKinds: Bool
    let emptyExtra: Bool
    let emptyStatuses: Bool
    let hoursValid: Bool
    let canSave: Bool

    static let initial = MusicSettingsInfo(
        load: .loading, albumChoice: "all", newAlbums: "all", albumTypes: ["album", "ep"], secondaryTypes: ["studio"],
        statuses: ["official"], pressingRule: "common", url: "", hours: "24", canChange: false, saving: false, error: nil, justSaved: false,
        dirty: false, emptyKinds: false, emptyExtra: false, emptyStatuses: false, hoursValid: true, canSave: false
    )
}

struct MusicUnmatchedArtistInfo: Hashable, Sendable, Identifiable {
    let titleId: Int64
    let name: String
    let files: Int

    var id: Int64 { titleId }
}

struct MusicUnmatchedInfo: Hashable, Sendable {
    let total: Int
    let shown: [MusicUnmatchedArtistInfo]
    let more: Int

    static let initial = MusicUnmatchedInfo(total: 0, shown: [], more: 0)
}

struct MusicLibAlbumInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let mbid: String
    let name: String
    let year: String
    let date: String
    let group: MusicLibGroupID
    let tracks: Int
    let inLibrary: Int
    let missing: Int
    let notOutYet: Int
    let included: Bool
    let keep: Bool
    let dropped: Bool
    let state: MusicAlbumStateID
    let future: Bool
    let cover: String
}

struct MusicSlotChipInfo: Hashable, Sendable {
    let version: String
    let label: String
    let tone: ToneKind
}

struct MusicLibTrackInfo: Hashable, Sendable, Identifiable {
    let albumId: Int64
    let id: Int64
    let disc: Int
    let number: Int
    let name: String
    let length: String
    let slotId: Int64
    let stateLabel: String
    let tone: ToneKind
    let multiDisc: Bool
    let slots: [MusicSlotChipInfo]
}

struct MusicAskLineInfo: Hashable, Sendable, Identifiable {
    let askedBy: String
    let askedAt: String
    let albums: String
    let decidedBy: String
    let decidedAt: String
    let stateLabel: String
    let note: String

    var id: String { "\(askedBy)-\(askedAt)" }
}

struct MusicQuickRulesInfo: Hashable, Sendable, Identifiable {
    let kind: MusicQuickRuleKind
    let id: Int64
}

struct MusicLibraryArtistInfo: Hashable, Sendable {
    let load: LoadInfo
    let titleId: Int64
    let mbid: String
    let name: String
    let image: String
    let disambiguation: String
    let canManage: Bool
    let canAsk: Bool
    let handsOff: Bool
    let keepUpToDate: Bool
    let folderPath: String
    let folderId: Int64
    let sizeBytes: Int64
    let rulesId: Int64
    let rulesName: String
    let gateReason: String
    let noDownloader: String
    let refreshedAt: String
    let nextRelease: String
    let albumCount: Int
    let includedCount: Int
    let haveTracks: Int
    let totalTracks: Int
    let missingTracks: Int
    let notOutYet: Int
    let fullReleases: Int
    let keptReleases: Int
    let newAlbums: String
    let albumTypes: [String]
    let secondaryTypes: [String]
    let statuses: [String]
    let preset: MusicKindsPresetKind
    let albums: [MusicLibAlbumInfo]
    let shown: [MusicLibAlbumInfo]
    let filter: MusicLibFilterKind
    let leftOut: Int
    let expanded: [Int64]
    let tracks: [MusicLibTrackInfo]
    let loadingTracks: [Int64]
    let busy: [Int64]
    let quick: [MusicQuickRulesInfo]
    let rules: [MusicRulesOptionInfo]
    let folders: [MusicFolderInfo]
    let unmatched: Int
    let asks: [MusicAskLineInfo]
    let working: Bool
    let note: MusicNoteInfo?
    let removed: Bool
    let progress: Float
    let customQuality: Bool
    let waitingForTracks: Bool

    static let initial = MusicLibraryArtistInfo(
        load: .loading, titleId: 0, mbid: "", name: "", image: "", disambiguation: "", canManage: false, canAsk: false, handsOff: false,
        keepUpToDate: false, folderPath: "", folderId: 0, sizeBytes: 0, rulesId: 0, rulesName: "", gateReason: "", noDownloader: "",
        refreshedAt: "", nextRelease: "", albumCount: 0, includedCount: 0, haveTracks: 0, totalTracks: 0, missingTracks: 0, notOutYet: 0,
        fullReleases: 0, keptReleases: 0, newAlbums: "all", albumTypes: [], secondaryTypes: [], statuses: [], preset: .custom, albums: [],
        shown: [], filter: .included, leftOut: 0, expanded: [], tracks: [], loadingTracks: [], busy: [], quick: [], rules: [], folders: [],
        unmatched: 0, asks: [], working: false, note: nil, removed: false, progress: 0, customQuality: false, waitingForTracks: false
    )

    func tracks(of albumId: Int64) -> [MusicLibTrackInfo] {
        tracks.filter { $0.albumId == albumId }
    }
}

struct MusicPressingInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let disambiguation: String
    let year: String
    let country: String
    let formats: String
    let trackCount: Int
    let chosen: Bool
    let byPerson: Bool
}

struct MusicLibraryAlbumInfo: Hashable, Sendable {
    let load: LoadInfo
    let albumId: Int64
    let cover: String
    let titleId: Int64
    let artistName: String
    let artistImage: String
    let artistMbid: String
    let name: String
    let kind: MusicAlbumKindID
    let year: String
    let disambiguation: String
    let dropped: Bool
    let included: Bool
    let keep: Bool
    let stateKey: MusicAlbumStateID
    let trackCount: Int
    let inLibrary: Int
    let minutes: Int
    let tracks: [MusicLibTrackInfo]
    let tracksWaiting: Bool
    let pressings: [MusicPressingInfo]
    let byRule: Bool
    let moreBy: [MusicLibAlbumInfo]
    let canManage: Bool
    let hasFile: Bool
    let notFound: Bool
    let busy: Bool
    let note: MusicNoteInfo?
    let removed: Bool
    let problem: MusicProblemKind
    let complete: Bool

    static let initial = MusicLibraryAlbumInfo(
        load: .loading, albumId: 0, cover: "", titleId: 0, artistName: "", artistImage: "", artistMbid: "", name: "", kind: .album,
        year: "", disambiguation: "", dropped: false, included: false, keep: false, stateKey: .noTracks, trackCount: 0, inLibrary: 0,
        minutes: 0, tracks: [], tracksWaiting: true, pressings: [], byRule: true, moreBy: [], canManage: false, hasFile: false,
        notFound: false, busy: false, note: nil, removed: false, problem: .none, complete: false
    )
}

struct MusicLeftoverInfo: Hashable, Sendable, Identifiable {
    let fileId: Int64
    let name: String
    let size: Int64
    let reason: String
    let owner: String
    let closest: String
    let albumId: Int64
    let dismissed: Bool

    var id: Int64 { fileId }
}

struct MusicPressingHintInfo: Hashable, Sendable, Identifiable {
    let albumId: Int64
    let albumName: String
    let pressingId: Int64
    let pressingName: String
    let trackCount: Int
    let fits: Int
    let chosen: Int
    let files: Int

    var id: String { "\(albumId)-\(pressingId)" }
}

struct MusicMatchTrackInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let disc: Int
    let number: Int
    let name: String
    let hasFile: Bool
}

struct MusicMatchAlbumInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
}

struct MusicMatchInfo: Hashable, Sendable {
    let file: MusicLeftoverInfo
    let albums: [MusicMatchAlbumInfo]
    let albumId: Int64
    let tracks: [MusicMatchTrackInfo]
    let loading: Bool
    let picked: Int64
    let replaces: Bool
}

struct MusicLeftoversInfo: Hashable, Sendable {
    let load: LoadInfo
    let open: [MusicLeftoverInfo]
    let alone: [MusicLeftoverInfo]
    let pressings: [MusicPressingHintInfo]
    let match: MusicMatchInfo?
    let lastMatched: [Int64]
    let busy: Bool
    let rescanning: Bool
    let note: MusicNoteInfo?

    static let initial = MusicLeftoversInfo(
        load: .loading, open: [], alone: [], pressings: [], match: nil, lastMatched: [], busy: false, rescanning: false, note: nil
    )
}

struct MusicOptionLists: Hashable, Sendable {
    let albumChoices: [MusicOptionInfo]
    let newAlbums: [MusicOptionInfo]
    let albumTypes: [MusicOptionInfo]
    let secondaryTypes: [MusicOptionInfo]
    let statuses: [MusicOptionInfo]
    let pressingRules: [MusicOptionInfo]
}
