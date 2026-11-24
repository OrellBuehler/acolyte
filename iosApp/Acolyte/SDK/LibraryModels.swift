import Foundation

enum LibraryKindID: String, Hashable, Sendable {
    case movie
    case show
    case artist
}

enum LibraryToolID: String, Hashable, Sendable, Identifiable {
    case stats
    case deleted
    case copies
    case held
    case turnedDown = "turneddown"
    case missingSubtitles = "missingsubtitles"
    case rename
    case importLibrary = "import"
    case importFiles = "importfiles"
    case libraryFolders = "libraryfolders"

    var id: String { rawValue }
}

enum LibraryNoticeKindID: String, Hashable, Sendable {
    case kept
    case unkept
    case rulesSet = "rulesset"
    case audioSet = "audioset"
    case searching
    case refreshing
    case moving
    case removed
    case copiesSearching = "copiessearching"
    case missingSearching = "missingsearching"
    case subtitlesSearching = "subtitlessearching"
    case filterSaved = "filtersaved"
    case restored
    case swapped
    case restoreFailed = "restorefailed"
    case takenOff = "takenoff"
    case cleared
    case renamed
    case renameChanged = "renamechanged"
    case adopted
    case tidied
    case untidied
    case flatOn = "flaton"
    case imported
    case folderAdded = "folderadded"
    case folderChecked = "folderchecked"
    case folderRemoved = "folderremoved"
    case pathChanged = "pathchanged"
    case importFoldersSaved = "importfolderssaved"
    case sideFilesSaved = "sidefilessaved"
    case handsOffOn = "handsoffon"
    case handsOffOff = "handsoffoff"
    case titleDeleted = "titledeleted"
    case failed
}

struct LibraryNoticeInfo: Hashable, Sendable {
    let seq: Int
    let kind: LibraryNoticeKindID
    let count: Int
    let total: Int
    let name: String
    let error: ErrorInfo?
    let partly: Bool
}

struct FilterOptionInfo: Hashable, Sendable, Identifiable {
    let id: String
    let label: String
    let count: Int
    let selected: Bool
}

struct MenuOptionInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let label: String
}

struct JobProgressInfo: Hashable, Sendable {
    let running: Bool
    let progress: Double
    let note: String

    static let idle = JobProgressInfo(running: false, progress: 0, note: "")
}
