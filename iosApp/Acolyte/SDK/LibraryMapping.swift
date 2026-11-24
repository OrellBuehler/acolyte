@preconcurrency import shared
import Foundation

extension Mapping {
    static func libraryKind(_ kind: Any) -> LibraryKindID {
        LibraryKindID(rawValue: key(kind)) ?? .movie
    }

    static func kotlinLibraryKind(_ kind: LibraryKindID?) -> LibraryKind? {
        switch kind {
        case .movie: LibraryKind.movie
        case .show: LibraryKind.show
        case .artist: LibraryKind.artist
        case nil: nil
        }
    }

    static func libraryNotice(_ notice: LibraryNotice?) -> LibraryNoticeInfo? {
        guard let notice else { return nil }
        return LibraryNoticeInfo(
            seq: Int(notice.seq),
            kind: LibraryNoticeKindID(rawValue: key(notice.kind)) ?? .failed,
            count: Int(notice.count),
            total: Int(notice.total),
            name: notice.name,
            error: error(notice.error),
            partly: notice.partly
        )
    }

    static func filterOption(_ option: FilterOption) -> FilterOptionInfo {
        FilterOptionInfo(id: option.id, label: option.label, count: Int(option.count), selected: option.selected)
    }

    static func menuOption(_ option: MenuOption) -> MenuOptionInfo {
        MenuOptionInfo(id: option.id, label: option.label)
    }

    static func jobProgress(_ progress: JobProgress) -> JobProgressInfo {
        JobProgressInfo(running: progress.running, progress: progress.progress, note: progress.note)
    }
}
