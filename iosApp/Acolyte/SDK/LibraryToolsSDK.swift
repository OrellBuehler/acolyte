@preconcurrency import shared
import Foundation

struct LibraryStatsBucketInfo: Hashable, Sendable, Identifiable {
    let value: String
    let files: Int
    let bytes: Int64
    let share: Double

    var id: String { value }
}

struct LibraryStatsFactInfo: Hashable, Sendable, Identifiable {
    let name: String
    let buckets: [LibraryStatsBucketInfo]

    var id: String { name }
}

struct LibraryStatsKindInfo: Hashable, Sendable, Identifiable {
    let kind: String
    let titles: Int
    let files: Int
    let bytes: Int64
    let unchecked: Int
    let facts: [LibraryStatsFactInfo]

    var id: String { kind }
}

struct LibraryWatchedPersonInfo: Hashable, Sendable, Identifiable {
    let server: String
    let name: String
    let linked: Bool
    let userName: String
    let movies: Int
    let episodes: Int
    let plays: Int
    let lastWatchedAt: String

    var id: String { server + "/" + name }
}

struct LibraryCleanupPersonInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let server: String

    var id: String { key }
}

struct LibraryWatchedTitleInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let year: Int
    let bytes: Int64
    let lastWatchedAt: String
}

struct LibraryCleanupInfo: Hashable, Sendable {
    let people: [LibraryCleanupPersonInfo]
    let found: [LibraryWatchedTitleInfo]
}

struct LibraryStatsInfo: Hashable, Sendable {
    let load: LoadInfo
    let isEmpty: Bool
    let kinds: [LibraryStatsKindInfo]
    let kind: String
    let watchedLoad: LoadInfo
    let watchedEnabled: Bool
    let readAt: String
    let people: [LibraryWatchedPersonInfo]
    let canClean: Bool
    let cleanup: LibraryCleanupInfo?
    let cleanupDays: Int
    let cleanupPicked: [String]
    let cleanupLoading: Bool
    let removing: Int64
    let notice: LibraryNoticeInfo?

    var current: LibraryStatsKindInfo? {
        kinds.first(where: { $0.kind == kind }) ?? kinds.first
    }

    static let initial = LibraryStatsInfo(
        load: .loading,
        isEmpty: false,
        kinds: [],
        kind: "",
        watchedLoad: .loading,
        watchedEnabled: false,
        readAt: "",
        people: [],
        canClean: false,
        cleanup: nil,
        cleanupDays: 30,
        cleanupPicked: [],
        cleanupLoading: false,
        removing: 0,
        notice: nil
    )
}

struct LibraryCopyRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let titleId: Int64
    let title: String
    let year: Int
    let version: String
    let edition: String
    let codec: String
    let container: String
    let inspectionStatus: String
    let audioLanguages: [String]
    let audioTracks: Int
    let audioUnknown: Bool
    let isProtected: Bool
    let poster: String?
    let fileName: String
    let selected: Bool
}

struct LibraryCopyReviewItemInfo: Hashable, Sendable, Identifiable {
    let id: Int
    let title: String
    let version: String
    let codec: String
}

struct LibraryCopiesReviewInfo: Hashable, Sendable, Identifiable {
    let items: [LibraryCopyReviewItemInfo]
    let slotCount: Int

    var id: String { "review" }
}

struct LibraryCopiesInfo: Hashable, Sendable {
    let load: LoadInfo
    let isEmpty: Bool
    let rows: [LibraryCopyRowInfo]
    let total: Int
    let codec: String
    let sort: String
    let codecs: [FilterOptionInfo]
    let selectedCount: Int
    let review: LibraryCopiesReviewInfo?
    let started: Int
    let busy: Bool
    let hasPrevious: Bool
    let hasNext: Bool
    let from: Int
    let to: Int
    let notice: LibraryNoticeInfo?

    static let initial = LibraryCopiesInfo(
        load: .loading,
        isEmpty: false,
        rows: [],
        total: 0,
        codec: "all",
        sort: "title",
        codecs: [],
        selectedCount: 0,
        review: nil,
        started: -1,
        busy: false,
        hasPrevious: false,
        hasNext: false,
        from: 0,
        to: 0,
        notice: nil
    )
}

enum LibraryDeletedShowKind: String, Hashable, Sendable {
    case all
    case deleted
    case replaced
}

struct LibraryDeletedRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let titleId: Int64
    let title: String
    let titleDeleted: Bool
    let fileName: String
    let size: Int64
    let deletedAt: String
    let replaced: Bool
    let replacedBy: String
    let daysLeft: Int
    let poster: String?
    let selected: Bool
}

struct LibraryDeletedQuestionInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let canSwap: Bool
}

struct LibraryDeletedInfo: Hashable, Sendable {
    let load: LoadInfo
    let isEmpty: Bool
    let rows: [LibraryDeletedRowInfo]
    let count: Int
    let show: LibraryDeletedShowKind
    let hasBoth: Bool
    let holdingDays: Int
    let titleId: Int64
    let titleName: String
    let selectedCount: Int
    let allSelected: Bool
    let busy: Bool
    let question: LibraryDeletedQuestionInfo?
    let canRestore: Bool
    let notice: LibraryNoticeInfo?

    static let initial = LibraryDeletedInfo(
        load: .loading,
        isEmpty: false,
        rows: [],
        count: 0,
        show: .all,
        hasBoth: false,
        holdingDays: -1,
        titleId: 0,
        titleName: "",
        selectedCount: 0,
        allSelected: false,
        busy: false,
        question: nil,
        canRestore: true,
        notice: nil
    )
}

enum LibraryReleaseVerdictKind: String, Hashable, Sendable {
    case turnedDown = "turneddown"
    case chosen
    case held
    case wouldChoose = "wouldchoose"
    case wouldHold = "wouldhold"
    case refused
    case passedOver = "passedover"
    case judged

    var tone: ToneKind {
        switch self {
        case .turnedDown, .refused: .fail
        case .chosen: .info
        case .held, .wouldHold: .wait
        case .wouldChoose: .ok
        case .passedOver, .judged: .idle
        }
    }
}

struct LibraryReleaseRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let title: String
    let version: String
    let name: String
    let poster: String?
    let tags: [String]
    let verdict: LibraryReleaseVerdictKind
    let sourceName: String
    let size: Int64
    let seeders: Int
    let transport: String
    let points: Int
    let rank: Int
    let holdUntil: String
    let since: String
    let reasons: [String]
}

struct LibraryReleasesInfo: Hashable, Sendable {
    let load: LoadInfo
    let isEmpty: Bool
    let turnedDown: Bool
    let rows: [LibraryReleaseRowInfo]
    let total: Int
    let hasPrevious: Bool
    let hasNext: Bool
    let from: Int
    let to: Int
    let busy: Bool
    let notice: LibraryNoticeInfo?

    static func initial(turnedDown: Bool) -> LibraryReleasesInfo {
        LibraryReleasesInfo(
            load: .loading,
            isEmpty: false,
            turnedDown: turnedDown,
            rows: [],
            total: 0,
            hasPrevious: false,
            hasNext: false,
            from: 0,
            to: 0,
            busy: false,
            notice: nil
        )
    }
}

struct LibrarySubtitlesRowInfo: Hashable, Sendable, Identifiable {
    let fileId: Int64
    let titleId: Int64
    let title: String
    let isEpisode: Bool
    let season: Int
    let episode: Int
    let language: String
    let languageName: String
    let forced: Bool
    let hearingImpaired: Bool
    let state: String
    let tries: Int
    let nextTryAt: String
    let poster: String?
    let busy: Bool

    var id: String { "\(fileId)/\(language)/\(forced)/\(hearingImpaired)" }
}

struct LibrarySubtitlesTitleInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let count: Int
}

struct LibrarySubtitlesLanguageInfo: Hashable, Sendable, Identifiable {
    let code: String
    let name: String

    var id: String { code }
}

struct LibrarySubtitlesInfo: Hashable, Sendable {
    let load: LoadInfo
    let isEmpty: Bool
    let rows: [LibrarySubtitlesRowInfo]
    let total: Int
    let titles: [LibrarySubtitlesTitleInfo]
    let languages: [LibrarySubtitlesLanguageInfo]
    let language: String
    let kind: String
    let titleId: Int64
    let filtered: Bool
    let queuing: Bool
    let searching: Bool
    let searchTotal: Int
    let searchDone: Int
    let hasPrevious: Bool
    let hasNext: Bool
    let from: Int
    let to: Int
    let notice: LibraryNoticeInfo?

    static let initial = LibrarySubtitlesInfo(
        load: .loading,
        isEmpty: false,
        rows: [],
        total: 0,
        titles: [],
        languages: [],
        language: "",
        kind: "",
        titleId: 0,
        filtered: false,
        queuing: false,
        searching: false,
        searchTotal: 0,
        searchDone: 0,
        hasPrevious: false,
        hasNext: false,
        from: 0,
        to: 0,
        notice: nil
    )
}

enum LibraryRenameFailureKind: String, Hashable, Sendable {
    case nothing = "none"
    case planChanged = "planchanged"
    case failed
}

struct LibraryRenameSubtitleInfo: Hashable, Sendable, Identifiable {
    let id: Int
    let from: String
    let to: String
    let skip: String
}

struct LibraryRenameRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let title: String
    let poster: String?
    let from: String
    let to: String
    let skip: String
    let subtitles: [LibraryRenameSubtitleInfo]
}

struct LibraryRenameInfo: Hashable, Sendable {
    let load: LoadInfo
    let isEmpty: Bool
    let changes: [LibraryRenameRowInfo]
    let skipped: [LibraryRenameRowInfo]
    let same: Int
    let running: Bool
    let job: JobProgressInfo
    let failure: LibraryRenameFailureKind
    let failureText: String
    let canRename: Bool
    let notice: LibraryNoticeInfo?

    static let initial = LibraryRenameInfo(
        load: .loading,
        isEmpty: false,
        changes: [],
        skipped: [],
        same: 0,
        running: false,
        job: .idle,
        failure: .nothing,
        failureText: "",
        canRename: true,
        notice: nil
    )
}

extension Mapping {
    static func libraryStats(_ s: LibraryStatsState) -> LibraryStatsInfo {
        LibraryStatsInfo(
            load: load(s.status),
            isEmpty: s.isEmpty,
            kinds: s.kinds.map { libraryStatsKind($0) },
            kind: s.current?.kind ?? s.kind,
            watchedLoad: load(s.watchedStatus),
            watchedEnabled: s.watched.enabled,
            readAt: s.watched.readAt,
            people: s.watched.people.map { libraryWatchedPerson($0) },
            canClean: s.canClean,
            cleanup: s.cleanup.map { libraryCleanup($0) },
            cleanupDays: Int(s.cleanupDays),
            cleanupPicked: s.cleanupPicked,
            cleanupLoading: s.cleanupLoading,
            removing: s.removing,
            notice: libraryNotice(s.notice)
        )
    }

    private static func libraryStatsKind(_ kind: StatsKind) -> LibraryStatsKindInfo {
        LibraryStatsKindInfo(
            kind: kind.kind,
            titles: Int(kind.titles),
            files: Int(kind.files),
            bytes: kind.bytes,
            unchecked: Int(kind.unchecked),
            facts: kind.facts.map { libraryStatsFact($0) }
        )
    }

    private static func libraryStatsFact(_ fact: StatsFact) -> LibraryStatsFactInfo {
        LibraryStatsFactInfo(name: fact.name, buckets: fact.buckets.map { libraryStatsBucket($0) })
    }

    private static func libraryStatsBucket(_ bucket: StatsBucketRow) -> LibraryStatsBucketInfo {
        LibraryStatsBucketInfo(value: bucket.value, files: Int(bucket.files), bytes: bucket.bytes, share: Double(bucket.share))
    }

    private static func libraryWatchedPerson(_ person: WatchedPerson) -> LibraryWatchedPersonInfo {
        LibraryWatchedPersonInfo(
            server: person.server,
            name: person.name,
            linked: person.linked,
            userName: person.userName,
            movies: Int(person.movies),
            episodes: Int(person.episodes),
            plays: Int(person.plays),
            lastWatchedAt: person.lastWatchedAt
        )
    }

    private static func libraryCleanup(_ cleanup: WatchedCleanup) -> LibraryCleanupInfo {
        LibraryCleanupInfo(
            people: cleanup.people.map { LibraryCleanupPersonInfo(key: $0.key, name: $0.name, server: $0.server) },
            found: cleanup.found.map { libraryWatchedTitle($0) }
        )
    }

    private static func libraryWatchedTitle(_ title: WatchedTitle) -> LibraryWatchedTitleInfo {
        LibraryWatchedTitleInfo(
            id: title.id,
            name: title.name,
            year: Int(title.year),
            bytes: title.bytes,
            lastWatchedAt: title.lastWatchedAt
        )
    }

    static func libraryCopies(_ s: LibraryCopiesState) -> LibraryCopiesInfo {
        LibraryCopiesInfo(
            load: load(s.status),
            isEmpty: s.isEmpty,
            rows: s.rows.map { libraryCopyRow($0) },
            total: Int(s.total),
            codec: s.codec,
            sort: s.sort,
            codecs: s.codecs.map { filterOption($0) },
            selectedCount: Int(s.selectedCount),
            review: s.review.map { libraryCopyReview($0) },
            started: Int(s.started),
            busy: s.busy,
            hasPrevious: s.hasPrevious,
            hasNext: s.hasNext,
            from: Int(s.from),
            to: Int(s.to),
            notice: libraryNotice(s.notice)
        )
    }

    private static func libraryCopyRow(_ row: CopyRow) -> LibraryCopyRowInfo {
        let copy = row.item
        return LibraryCopyRowInfo(
            id: copy.fileId,
            titleId: copy.titleId,
            title: copy.title,
            year: Int(copy.year),
            version: copy.version,
            edition: copy.edition,
            codec: copy.codec,
            container: copy.container,
            inspectionStatus: copy.inspectionStatus,
            audioLanguages: copy.audioLanguages,
            audioTracks: Int(copy.audioTracks),
            audioUnknown: copy.audioUnknown,
            isProtected: copy.isProtected,
            poster: row.poster,
            fileName: row.fileName,
            selected: row.selected
        )
    }

    private static func libraryCopyReview(_ review: CopyReview) -> LibraryCopiesReviewInfo {
        LibraryCopiesReviewInfo(
            items: review.items.enumerated().map { entry in
                LibraryCopyReviewItemInfo(id: entry.offset, title: entry.element.title, version: entry.element.version, codec: entry.element.codec)
            },
            slotCount: Int(review.slotCount)
        )
    }

    static func libraryDeleted(_ s: LibraryDeletedState) -> LibraryDeletedInfo {
        LibraryDeletedInfo(
            load: load(s.status),
            isEmpty: s.isEmpty,
            rows: s.rows.map { libraryDeletedRow($0) },
            count: Int(s.count),
            show: LibraryDeletedShowKind(rawValue: key(s.show)) ?? .all,
            hasBoth: s.hasBoth,
            holdingDays: Int(s.holdingDays),
            titleId: s.titleId,
            titleName: s.titleName,
            selectedCount: Int(s.selectedCount),
            allSelected: s.allSelected,
            busy: s.busy,
            question: s.question.map { LibraryDeletedQuestionInfo(id: $0.id, name: $0.name, canSwap: $0.canSwap) },
            canRestore: s.canRestore,
            notice: libraryNotice(s.notice)
        )
    }

    private static func libraryDeletedRow(_ row: HeldRow) -> LibraryDeletedRowInfo {
        let file = row.file
        return LibraryDeletedRowInfo(
            id: file.id,
            titleId: file.titleId,
            title: file.title,
            titleDeleted: file.titleDeleted,
            fileName: row.fileName,
            size: file.size,
            deletedAt: file.deletedAt,
            replaced: file.replaced,
            replacedBy: file.replacedBy,
            daysLeft: Int(row.daysLeft),
            poster: row.poster,
            selected: row.selected
        )
    }

    static func libraryReleases(_ s: LibraryReleasesState) -> LibraryReleasesInfo {
        LibraryReleasesInfo(
            load: load(s.status),
            isEmpty: s.isEmpty,
            turnedDown: s.turnedDown,
            rows: s.rows.map { libraryReleaseRow($0) },
            total: Int(s.total),
            hasPrevious: s.hasPrevious,
            hasNext: s.hasNext,
            from: Int(s.from),
            to: Int(s.to),
            busy: s.busy,
            notice: libraryNotice(s.notice)
        )
    }

    private static func libraryReleaseRow(_ row: ReleaseRow) -> LibraryReleaseRowInfo {
        LibraryReleaseRowInfo(
            id: row.id,
            title: row.title,
            version: row.version,
            name: row.name,
            poster: row.poster,
            tags: row.tags,
            verdict: LibraryReleaseVerdictKind(rawValue: key(row.verdict)) ?? .judged,
            sourceName: row.sourceName,
            size: row.size,
            seeders: Int(row.seeders),
            transport: row.transport,
            points: Int(row.points),
            rank: Int(row.rank),
            holdUntil: row.holdUntil,
            since: row.since,
            reasons: row.reasons
        )
    }

    static func librarySubtitles(_ s: LibrarySubtitlesState) -> LibrarySubtitlesInfo {
        LibrarySubtitlesInfo(
            load: load(s.status),
            isEmpty: s.isEmpty,
            rows: s.rows.map { librarySubtitlesRow($0) },
            total: Int(s.total),
            titles: s.titles.map { LibrarySubtitlesTitleInfo(id: $0.id, name: $0.name, count: Int($0.count)) },
            languages: s.languages.map { LibrarySubtitlesLanguageInfo(code: $0.code, name: $0.name) },
            language: s.language,
            kind: s.kind,
            titleId: s.titleId,
            filtered: s.filtered,
            queuing: s.queuing,
            searching: s.searching,
            searchTotal: Int(s.searchTotal),
            searchDone: Int(s.searchDone),
            hasPrevious: s.hasPrevious,
            hasNext: s.hasNext,
            from: Int(s.from),
            to: Int(s.to),
            notice: libraryNotice(s.notice)
        )
    }

    private static func librarySubtitlesRow(_ row: SubtitleRow) -> LibrarySubtitlesRowInfo {
        let missing = row.row
        return LibrarySubtitlesRowInfo(
            fileId: missing.fileId,
            titleId: missing.titleId,
            title: missing.title,
            isEpisode: missing.kind == "episode",
            season: Int(missing.season),
            episode: Int(missing.episode),
            language: missing.language,
            languageName: row.languageName,
            forced: missing.forced,
            hearingImpaired: missing.hearingImpaired,
            state: missing.state,
            tries: Int(missing.tries),
            nextTryAt: missing.nextTryAt,
            poster: row.poster,
            busy: row.busy
        )
    }

    static func libraryRename(_ s: LibraryRenameState) -> LibraryRenameInfo {
        LibraryRenameInfo(
            load: load(s.status),
            isEmpty: s.isEmpty,
            changes: s.changes.map { libraryRenameRow($0) },
            skipped: s.skipped.map { libraryRenameRow($0) },
            same: Int(s.same),
            running: s.running,
            job: jobProgress(s.job),
            failure: LibraryRenameFailureKind(rawValue: key(s.failure)) ?? .nothing,
            failureText: s.failureText,
            canRename: s.canRename,
            notice: libraryNotice(s.notice)
        )
    }

    private static func libraryRenameRow(_ row: RenameRow) -> LibraryRenameRowInfo {
        LibraryRenameRowInfo(
            id: row.fileId,
            title: row.title,
            poster: row.poster,
            from: row.from,
            to: row.to,
            skip: row.skip,
            subtitles: row.subtitles.enumerated().map { entry in
                LibraryRenameSubtitleInfo(id: entry.offset, from: entry.element.from, to: entry.element.to, skip: entry.element.skip)
            }
        )
    }
}

final class LibraryStatsDriver: @unchecked Sendable {
    private let presenter: LibraryStatsPresenter

    init(presenter: LibraryStatsPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryStatsInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.libraryStats(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func setKind(_ kind: String) {
        presenter.setKind(kind: kind)
    }

    func setCleanupDays(_ days: Int) async {
        do {
            try await presenter.setCleanupDays(days: Int32(days))
        } catch {}
    }

    func toggleCleanupPerson(_ key: String) async {
        do {
            try await presenter.toggleCleanupPerson(key: key)
        } catch {}
    }

    func removeWatched(id: Int64, deleteFiles: Bool, neverAdd: Bool) async {
        do {
            try await presenter.removeWatched(id: id, deleteFiles: deleteFiles, neverAdd: neverAdd)
        } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class LibraryCopiesDriver: @unchecked Sendable {
    private let presenter: LibraryCopiesPresenter

    init(presenter: LibraryCopiesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryCopiesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.libraryCopies(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func search(_ text: String) async {
        do {
            try await presenter.search(text: text)
        } catch {}
    }

    func setCodec(_ codec: String) async {
        do {
            try await presenter.setCodec(codec: codec)
        } catch {}
    }

    func setSort(_ sort: String) async {
        do {
            try await presenter.setSort(sort: sort)
        } catch {}
    }

    func nextPage() async {
        do {
            try await presenter.nextPage()
        } catch {}
    }

    func previousPage() async {
        do {
            try await presenter.previousPage()
        } catch {}
    }

    func toggle(_ fileId: Int64) {
        presenter.toggle(fileId: fileId)
    }

    func review() async {
        do {
            try await presenter.review()
        } catch {}
    }

    func cancelReview() {
        presenter.cancelReview()
    }

    func start() async {
        do {
            try await presenter.start()
        } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class LibraryDeletedDriver: @unchecked Sendable {
    private let presenter: LibraryDeletedPresenter

    init(presenter: LibraryDeletedPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryDeletedInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.libraryDeleted(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func setShow(_ show: LibraryDeletedShowKind) {
        switch show {
        case .all: presenter.setShow(show: HeldShow.all)
        case .deleted: presenter.setShow(show: HeldShow.deleted)
        case .replaced: presenter.setShow(show: HeldShow.replaced)
        }
    }

    func clearTitle() async {
        do {
            try await presenter.clearTitle()
        } catch {}
    }

    func toggle(_ id: Int64) {
        presenter.toggle(id: id)
    }

    func toggleAll() {
        presenter.toggleAll()
    }

    func restore() async {
        do {
            try await presenter.restore()
        } catch {}
    }

    func answer(_ how: String) async {
        do {
            try await presenter.answer(how: how)
        } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class LibraryReleasesDriver: @unchecked Sendable {
    private let presenter: LibraryReleasesPresenter

    init(presenter: LibraryReleasesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryReleasesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.libraryReleases(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func nextPage() async {
        do {
            try await presenter.nextPage()
        } catch {}
    }

    func previousPage() async {
        do {
            try await presenter.previousPage()
        } catch {}
    }

    func remove(_ id: Int64) async {
        do {
            try await presenter.remove(id: id)
        } catch {}
    }

    func clearAll() async {
        do {
            try await presenter.clearAll()
        } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    func followLive() async {
        do {
            try await presenter.watch()
        } catch {}
    }
}

final class LibrarySubtitlesDriver: @unchecked Sendable {
    private let presenter: LibrarySubtitlesPresenter

    init(presenter: LibrarySubtitlesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibrarySubtitlesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.librarySubtitles(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func setLanguage(_ code: String) async {
        do {
            try await presenter.setLanguage(code: code)
        } catch {}
    }

    func setKind(_ kind: String) async {
        do {
            try await presenter.setKind(kind: kind)
        } catch {}
    }

    func setTitle(_ id: Int64) async {
        do {
            try await presenter.setTitle(id: id)
        } catch {}
    }

    func nextPage() async {
        do {
            try await presenter.nextPage()
        } catch {}
    }

    func previousPage() async {
        do {
            try await presenter.previousPage()
        } catch {}
    }

    func searchAll() async {
        do {
            try await presenter.searchAll()
        } catch {}
    }

    func searchRow(_ fileId: Int64) async {
        do {
            try await presenter.searchRow(fileId: fileId)
        } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    func followLive() async {
        do {
            try await presenter.watch()
        } catch {}
    }
}

final class LibraryRenameDriver: @unchecked Sendable {
    private let presenter: LibraryRenamePresenter

    init(presenter: LibraryRenamePresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryRenameInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.libraryRename(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func refresh() async {
        do {
            try await presenter.refresh()
        } catch {}
    }

    func rename() async {
        do {
            try await presenter.rename()
        } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}
