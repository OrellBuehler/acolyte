@preconcurrency import shared
import Foundation

enum LibraryFolderKindID: String, Hashable, Sendable, Identifiable, CaseIterable {
    case movie
    case show
    case music

    var id: String { rawValue }
}

enum LibraryImportStatusID: String, Hashable, Sendable {
    case matched
    case unsure
    case unmatched
    case notLookedUp = "notlookedup"
    case inLibrary = "inlibrary"
    case neverAdd = "neveradd"
    case noVideo = "novideo"
    case unknown

    init(wire: String) {
        let normalised = wire.replacingOccurrences(of: "_", with: "").lowercased()
        self = LibraryImportStatusID(rawValue: normalised) ?? .unknown
    }
}

struct LibraryImportFolderInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let kind: LibraryFolderKindID
    let label: String
    let path: String
}

struct LibraryImportPickInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let year: Int
    let poster: String
    let score: Double

    var id: String { key }
}

struct LibraryImportRowInfo: Hashable, Sendable, Identifiable {
    let folder: String
    let status: LibraryImportStatusID
    let why: String
    let pick: LibraryImportPickInfo?
    let choices: [LibraryImportPickInfo]
    let shownName: String
    let shownYear: Int
    let ticked: Bool
    let canTick: Bool
    let titleId: Int64
    let episodes: Int
    let files: Int
    let unknown: Int
    let albums: Int
    let tracks: Int

    var id: String { folder }
}

struct LibraryImportLookupResultInfo: Hashable, Sendable, Identifiable {
    let key: String
    let name: String
    let year: Int
    let poster: String
    let inLibrary: Bool

    var id: String { key }
}

struct LibraryImportLookupInfo: Hashable, Sendable {
    let folder: String
    let term: String
    let busy: Bool
    let results: [LibraryImportLookupResultInfo]
    let nothing: Bool
    let error: ErrorInfo?
}

struct LibraryImportPlanInfo: Hashable, Sendable {
    let kind: LibraryFolderKindID
    let path: String
    let total: Int
    let offset: Int
    let shown: Int
    let more: Bool
    let next: String
    let warning: String
    let partial: Bool
    let looseMovies: Int
    let looseTop: Int
    let suggestFlat: Bool
    let flat: Bool
}

struct LibraryImportAdoptedInfo: Hashable, Sendable, Identifiable {
    let folder: String
    let titleId: Int64
    let error: String

    var id: String { folder }
}

struct LibraryImportOutcomeInfo: Hashable, Sendable {
    let adopted: Int
    let failed: Int
    let unknown: Int
    let folders: [LibraryImportAdoptedInfo]
}

struct LibraryTidyFileInfo: Hashable, Sendable, Identifiable {
    let from: String
    let to: String

    var id: String { from }
}

struct LibraryTidyMoveInfo: Hashable, Sendable, Identifiable {
    let from: String
    let to: String
    let name: String
    let year: Int
    let files: [LibraryTidyFileInfo]
    let problem: String
    let why: String
    let path: String

    var id: String { from }
}

struct LibraryTidyFailureInfo: Hashable, Sendable, Identifiable {
    let from: String
    let why: String

    var id: String { from }
}

struct LibraryTidyResultInfo: Hashable, Sendable {
    let moved: Int
    let failed: [LibraryTidyFailureInfo]
}

struct LibraryImportTidyInfo: Hashable, Sendable {
    let moves: [LibraryTidyMoveInfo]
    let blocked: [LibraryTidyMoveInfo]
    let unmatched: Int
    let busy: Bool
    let changed: Bool
    let result: LibraryTidyResultInfo?
}

struct LibraryImportStatusOptionInfo: Hashable, Sendable, Identifiable {
    let id: String
    let status: LibraryImportStatusID?
    let count: Int
    let selected: Bool
}

struct LibraryImportInfo: Hashable, Sendable {
    let load: LoadInfo
    let folders: [LibraryImportFolderInfo]
    let folderId: Int64
    let canOpenFolders: Bool
    let scanning: Bool
    let adopting: Bool
    let progress: JobProgressInfo
    let progressDone: Int
    let progressTotal: Int
    let adoptDone: Int
    let adoptTotal: Int
    let plan: LibraryImportPlanInfo?
    let rows: [LibraryImportRowInfo]
    let statusOptions: [LibraryImportStatusOptionInfo]
    let statusFilter: String
    let query: String
    let tickedCount: Int
    let shownTickable: Int
    let follow: Bool
    let lookup: LibraryImportLookupInfo?
    let outcome: LibraryImportOutcomeInfo?
    let undoFiles: Int
    let tidy: LibraryImportTidyInfo?
    let error: ErrorInfo?
    let notice: LibraryNoticeInfo?
    let isEmpty: Bool

    var busy: Bool { scanning || adopting }

    static let initial = LibraryImportInfo(
        load: .loading,
        folders: [],
        folderId: 0,
        canOpenFolders: false,
        scanning: false,
        adopting: false,
        progress: .idle,
        progressDone: -1,
        progressTotal: -1,
        adoptDone: 0,
        adoptTotal: 0,
        plan: nil,
        rows: [],
        statusOptions: [],
        statusFilter: "all",
        query: "",
        tickedCount: 0,
        shownTickable: 0,
        follow: true,
        lookup: nil,
        outcome: nil,
        undoFiles: 0,
        tidy: nil,
        error: nil,
        notice: nil,
        isEmpty: false
    )
}

enum LibraryFilesModeID: String, Hashable, Sendable {
    case copy
    case move
}

enum LibraryFilesTitleKindID: String, Hashable, Sendable {
    case movie
    case show
    case artist
}

enum LibraryFilesFileKindID: String, Hashable, Sendable {
    case video
    case audio
}

enum LibraryFilesBlockerID: String, Hashable, Sendable {
    case clear = "none"
    case inLibrary = "inlibrary"
    case noPick = "nopick"
    case noEpisode = "noepisode"
    case hasFile = "hasfile"
    case noTrack = "notrack"
}

enum LibraryFilesConfidenceID: String, Hashable, Sendable {
    case unknown = "none"
    case likely
    case guess
}

enum LibraryFilesOutcomeID: String, Hashable, Sendable {
    case placed
    case refused
    case failed
}

enum LibraryFilesPlacedByID: String, Hashable, Sendable {
    case move
    case copy
    case hardlink
    case other
}

struct LibraryFilesPickInfo: Hashable, Sendable {
    let titleId: Int64
    let titleName: String
    let titleKind: LibraryFilesTitleKindID
    let year: Int
    let versionId: Int64
    let season: Int
    let episode: Int
    let slotId: Int64
    let hasFile: Bool
    let albumId: Int64
    let albumName: String
    let albumYear: Int
    let disc: Int
    let track: Int
    let trackName: String
}

struct LibraryFilesResultInfo: Hashable, Sendable {
    let path: String
    let outcome: LibraryFilesOutcomeID
    let placedBy: LibraryFilesPlacedByID
    let titleId: Int64
    let error: String
}

struct LibraryFilesRowInfo: Hashable, Sendable, Identifiable {
    let path: String
    let name: String
    let size: Int64
    let quality: String
    let kind: LibraryFilesFileKindID
    let pick: LibraryFilesPickInfo?
    let confidence: LibraryFilesConfidenceID
    let blocker: LibraryFilesBlockerID
    let note: String
    let duplicate: Bool
    let ticked: Bool
    let canTick: Bool
    let canChange: Bool
    let replace: Bool
    let result: LibraryFilesResultInfo?

    var id: String { path }
}

struct LibraryFilesBrowseFolderInfo: Hashable, Sendable, Identifiable {
    let name: String
    let path: String

    var id: String { path }
}

struct LibraryFilesBrowserInfo: Hashable, Sendable {
    let path: String
    let parent: String
    let folders: [LibraryFilesBrowseFolderInfo]
    let files: Int
    let more: Bool
    let loading: Bool
    let error: ErrorInfo?
    let atRoot: Bool
}

struct LibraryFilesTitleInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let kind: LibraryFilesTitleKindID
    let name: String
    let year: Int
}

struct LibraryFilesVersionInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let label: String
    let main: Bool
}

struct LibraryFilesEpisodeInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let season: Int
    let episode: Int
    let name: String
}

struct LibraryFilesEditorInfo: Hashable, Sendable {
    let path: String
    let query: String
    let searching: Bool
    let results: [LibraryFilesTitleInfo]
    let nothing: Bool
    let error: ErrorInfo?
    let titleId: Int64
    let titleName: String
    let titleKind: LibraryFilesTitleKindID?
    let year: Int
    let loading: Bool
    let versions: [LibraryFilesVersionInfo]
    let versionId: Int64
    let seasons: [Int]
    let season: Int
    let episodes: [LibraryFilesEpisodeInfo]
    let episode: Int
    let canUse: Bool
}

struct LibraryFilesInfo: Hashable, Sendable {
    let allowed: Bool
    let canBrowse: Bool
    let path: String
    let scanning: Bool
    let scanned: Bool
    let scanPath: String
    let more: Bool
    let rows: [LibraryFilesRowInfo]
    let mode: LibraryFilesModeID
    let tickedCount: Int
    let replaceCount: Int
    let importing: Bool
    let browser: LibraryFilesBrowserInfo?
    let editor: LibraryFilesEditorInfo?
    let error: ErrorInfo?
    let notice: LibraryNoticeInfo?

    static let initial = LibraryFilesInfo(
        allowed: false,
        canBrowse: false,
        path: "",
        scanning: false,
        scanned: false,
        scanPath: "",
        more: false,
        rows: [],
        mode: .copy,
        tickedCount: 0,
        replaceCount: 0,
        importing: false,
        browser: nil,
        editor: nil,
        error: nil,
        notice: nil
    )
}

enum LibraryFolderHealthID: String, Hashable, Sendable {
    case ok
    case missing
    case readOnly = "readonly"
    case noHardlink = "nohardlink"
    case copiesExpected = "copiesexpected"
    case lookOnly = "lookonly"
    case unknown
}

struct LibraryFoldersRowInfo: Hashable, Sendable, Identifiable {
    let id: Int64
    let kind: LibraryFolderKindID
    let label: String
    let path: String
    let shortPath: String
    let health: LibraryFolderHealthID
    let tone: ToneKind
    let reason: String
    let freeSpace: Int64
    let checkedAt: String
    let busy: Bool
    let rulesId: Int64
    let rulesName: String
    let rulesIsDefault: Bool
    let copiesExpected: Bool
    let tags: [String]
    let onlyTagged: Bool
    let audio: [String]
    let lookOnly: Bool
    let flat: Bool

    var title: String { label.isEmpty ? path : label }
}

struct LibraryFoldersGroupInfo: Hashable, Sendable, Identifiable {
    let kind: LibraryFolderKindID
    let folders: [LibraryFoldersRowInfo]

    var id: String { kind.rawValue }
}

struct LibraryFoldersImportInfo: Hashable, Sendable {
    let extra: [String]
    let watch: [String]
}

struct LibraryFoldersSideInfo: Hashable, Sendable {
    let on: Bool
    let types: [String]
}

struct LibraryFoldersInfo: Hashable, Sendable {
    let load: LoadInfo
    let groups: [LibraryFoldersGroupInfo]
    let rulesAvailable: Bool
    let movieRules: [MenuOptionInfo]
    let showRules: [MenuOptionInfo]
    let musicRules: [MenuOptionInfo]
    let canEdit: Bool
    let isOwner: Bool
    let handsOff: Bool
    let handsOffBusy: Bool
    let needsPassword: Bool
    let passwordError: ErrorInfo?
    let canSaveImportFolders: Bool
    let importFolders: LibraryFoldersImportInfo?
    let sideFiles: LibraryFoldersSideInfo?
    let tagSuggestions: [String]
    let adding: Bool
    let notice: LibraryNoticeInfo?
    let isEmpty: Bool

    func rules(for kind: LibraryFolderKindID) -> [MenuOptionInfo] {
        switch kind {
        case .movie: movieRules
        case .show: showRules
        case .music: musicRules
        }
    }

    static let initial = LibraryFoldersInfo(
        load: .loading,
        groups: [],
        rulesAvailable: false,
        movieRules: [],
        showRules: [],
        musicRules: [],
        canEdit: false,
        isOwner: false,
        handsOff: false,
        handsOffBusy: false,
        needsPassword: false,
        passwordError: nil,
        canSaveImportFolders: false,
        importFolders: nil,
        sideFiles: nil,
        tagSuggestions: [],
        adding: false,
        notice: nil,
        isEmpty: false
    )
}

extension Mapping {
    static func libraryFolderKind(_ wire: String) -> LibraryFolderKindID {
        LibraryFolderKindID(rawValue: wire) ?? .movie
    }

    static func libraryFilesTitleKind(_ wire: String) -> LibraryFilesTitleKindID {
        LibraryFilesTitleKindID(rawValue: wire) ?? .movie
    }

    static func libraryImport(_ s: LibraryImportState) -> LibraryImportInfo {
        let folders = s.folders.map { libraryImportFolder($0) }
        let rows = s.rows.map { libraryImportRow($0) }
        let options = s.statusOptions.map { libraryImportStatusOption($0) }
        return LibraryImportInfo(
            load: load(s.status),
            folders: folders,
            folderId: s.folderId,
            canOpenFolders: s.canOpenFolders,
            scanning: s.scanning,
            adopting: s.adopting,
            progress: jobProgress(s.progress),
            progressDone: Int(s.progressDone),
            progressTotal: Int(s.progressTotal),
            adoptDone: Int(s.adoptDone),
            adoptTotal: Int(s.adoptTotal),
            plan: s.plan.map { libraryImportPlan($0) },
            rows: rows,
            statusOptions: options,
            statusFilter: s.statusFilter,
            query: s.query,
            tickedCount: Int(s.tickedCount),
            shownTickable: Int(s.shownTickable),
            follow: s.follow,
            lookup: s.lookup.map { libraryImportLookup($0) },
            outcome: s.outcome.map { libraryImportOutcome($0) },
            undoFiles: Int(s.undoFiles),
            tidy: s.tidy.map { libraryImportTidy($0) },
            error: error(s.error),
            notice: libraryNotice(s.notice),
            isEmpty: s.isEmpty
        )
    }

    static func libraryImportFolder(_ f: ImportFolderOption) -> LibraryImportFolderInfo {
        LibraryImportFolderInfo(id: f.id, kind: libraryFolderKind(f.kind), label: f.label, path: f.path)
    }

    static func libraryImportPick(_ p: ImportPick) -> LibraryImportPickInfo {
        LibraryImportPickInfo(key: p.key, name: p.name, year: Int(p.year), poster: p.poster, score: p.score)
    }

    static func libraryImportRow(_ r: ImportRow) -> LibraryImportRowInfo {
        LibraryImportRowInfo(
            folder: r.folder,
            status: LibraryImportStatusID(wire: r.status),
            why: r.why,
            pick: r.pick.map { libraryImportPick($0) },
            choices: r.choices.map { libraryImportPick($0) },
            shownName: r.shownName,
            shownYear: Int(r.shownYear),
            ticked: r.ticked,
            canTick: r.canTick,
            titleId: r.titleId,
            episodes: Int(r.episodes),
            files: Int(r.files),
            unknown: Int(r.unknown),
            albums: Int(r.albums),
            tracks: Int(r.tracks)
        )
    }

    static func libraryImportStatusOption(_ o: FilterOption) -> LibraryImportStatusOptionInfo {
        LibraryImportStatusOptionInfo(
            id: o.id,
            status: o.id == "all" ? nil : LibraryImportStatusID(wire: o.id),
            count: Int(o.count),
            selected: o.selected
        )
    }

    static func libraryImportPlan(_ p: ImportPlanInfo) -> LibraryImportPlanInfo {
        LibraryImportPlanInfo(
            kind: libraryFolderKind(p.kind),
            path: p.path,
            total: Int(p.total),
            offset: Int(p.offset),
            shown: Int(p.shown),
            more: p.more,
            next: p.next,
            warning: p.warning,
            partial: p.partial,
            looseMovies: Int(p.looseMovies),
            looseTop: Int(p.looseTop),
            suggestFlat: p.suggestFlat,
            flat: p.flat
        )
    }

    static func libraryImportLookup(_ l: ImportLookup) -> LibraryImportLookupInfo {
        let results = l.results.map { result in
            LibraryImportLookupResultInfo(key: result.key, name: result.name, year: Int(result.year), poster: result.poster, inLibrary: result.inLibrary)
        }
        return LibraryImportLookupInfo(folder: l.folder, term: l.term, busy: l.busy, results: results, nothing: l.nothing, error: error(l.error))
    }

    static func libraryImportOutcome(_ o: ImportOutcome) -> LibraryImportOutcomeInfo {
        let folders = o.folders.map { folder in
            LibraryImportAdoptedInfo(folder: folder.folder, titleId: folder.titleId, error: folder.error)
        }
        return LibraryImportOutcomeInfo(adopted: Int(o.adopted), failed: Int(o.failed), unknown: Int(o.unknown), folders: folders)
    }

    static func libraryImportTidy(_ t: TidyInfo) -> LibraryImportTidyInfo {
        LibraryImportTidyInfo(
            moves: t.moves.map { libraryTidyMove($0) },
            blocked: t.blocked.map { libraryTidyMove($0) },
            unmatched: Int(t.unmatched),
            busy: t.busy,
            changed: t.changed,
            result: t.result.map { libraryTidyResult($0) }
        )
    }

    static func libraryTidyMove(_ m: TidyMove) -> LibraryTidyMoveInfo {
        let files = m.files.map { file in
            LibraryTidyFileInfo(from: file.from, to: file.to)
        }
        return LibraryTidyMoveInfo(from: m.from, to: m.to, name: m.name, year: Int(m.year), files: files, problem: m.problem, why: m.why, path: m.path)
    }

    static func libraryTidyResult(_ r: TidyResult) -> LibraryTidyResultInfo {
        let failed = r.failed.map { failure in
            LibraryTidyFailureInfo(from: failure.from, why: failure.why)
        }
        return LibraryTidyResultInfo(moved: Int(r.moved), failed: failed)
    }

    static func libraryFiles(_ s: LibraryFilesState) -> LibraryFilesInfo {
        let rows = s.rows.map { libraryFilesRow($0) }
        return LibraryFilesInfo(
            allowed: s.allowed,
            canBrowse: s.canBrowse,
            path: s.path,
            scanning: s.scanning,
            scanned: s.scanned,
            scanPath: s.scanPath,
            more: s.more,
            rows: rows,
            mode: s.mode == "move" ? .move : .copy,
            tickedCount: Int(s.tickedCount),
            replaceCount: Int(s.replaceCount),
            importing: s.importing,
            browser: s.browser.map { libraryFilesBrowser($0) },
            editor: s.editor.map { libraryFilesEditor($0) },
            error: error(s.error),
            notice: libraryNotice(s.notice)
        )
    }

    static func libraryFilesPick(_ p: ByHandPick) -> LibraryFilesPickInfo {
        LibraryFilesPickInfo(
            titleId: p.titleId,
            titleName: p.titleName,
            titleKind: libraryFilesTitleKind(p.titleKind),
            year: Int(p.year),
            versionId: p.versionId,
            season: Int(p.season),
            episode: Int(p.episode),
            slotId: p.slotId,
            hasFile: p.hasFile,
            albumId: p.albumId,
            albumName: p.albumName,
            albumYear: Int(p.albumYear),
            disc: Int(p.disc),
            track: Int(p.track),
            trackName: p.trackName
        )
    }

    static func libraryFilesResult(_ r: ByHandResult) -> LibraryFilesResultInfo {
        LibraryFilesResultInfo(
            path: r.path,
            outcome: LibraryFilesOutcomeID(rawValue: key(r.outcome)) ?? .failed,
            placedBy: LibraryFilesPlacedByID(rawValue: key(r.placedBy)) ?? .other,
            titleId: r.titleId,
            error: r.error
        )
    }

    static func libraryFilesRow(_ r: ByHandRow) -> LibraryFilesRowInfo {
        LibraryFilesRowInfo(
            path: r.path,
            name: r.name,
            size: r.size,
            quality: r.quality,
            kind: r.kind == "audio" ? .audio : .video,
            pick: r.pick.map { libraryFilesPick($0) },
            confidence: LibraryFilesConfidenceID(rawValue: key(r.confidence)) ?? .unknown,
            blocker: LibraryFilesBlockerID(rawValue: key(r.blocker)) ?? .clear,
            note: r.note,
            duplicate: r.duplicate,
            ticked: r.ticked,
            canTick: r.canTick,
            canChange: r.canChange,
            replace: r.replace,
            result: r.result.map { libraryFilesResult($0) }
        )
    }

    static func libraryFilesBrowser(_ b: BrowseState) -> LibraryFilesBrowserInfo {
        let folders = b.folders.map { folder in
            LibraryFilesBrowseFolderInfo(name: folder.name, path: folder.path)
        }
        return LibraryFilesBrowserInfo(
            path: b.path,
            parent: b.parent,
            folders: folders,
            files: Int(b.files),
            more: b.more,
            loading: b.loading,
            error: error(b.error),
            atRoot: b.atRoot
        )
    }

    static func libraryFilesEditor(_ e: EditorState) -> LibraryFilesEditorInfo {
        let results = e.results.map { title in
            LibraryFilesTitleInfo(id: title.id, kind: libraryFilesTitleKind(title.kind), name: title.name, year: Int(title.year))
        }
        let versions = e.versions.map { version in
            LibraryFilesVersionInfo(id: version.id, label: version.label, main: version.main)
        }
        let episodes = e.episodes.map { row in
            LibraryFilesEpisodeInfo(id: row.id, season: Int(row.season), episode: Int(row.episode), name: row.name)
        }
        let seasons = e.seasons.map { Int($0.intValue) }
        return LibraryFilesEditorInfo(
            path: e.path,
            query: e.query,
            searching: e.searching,
            results: results,
            nothing: e.nothing,
            error: error(e.error),
            titleId: e.titleId,
            titleName: e.titleName,
            titleKind: LibraryFilesTitleKindID(rawValue: e.titleKind),
            year: Int(e.year),
            loading: e.loading,
            versions: versions,
            versionId: e.versionId,
            seasons: seasons,
            season: Int(e.season),
            episodes: episodes,
            episode: Int(e.episode),
            canUse: e.canUse
        )
    }

    static func libraryFolders(
        _ s: LibraryFoldersState,
        movie: [PickRule],
        show: [PickRule],
        music: [PickRule]
    ) -> LibraryFoldersInfo {
        let groups = s.groups.map { libraryFoldersGroup($0) }
        let imports = s.importFolders.map { folders in
            LibraryFoldersImportInfo(extra: folders.extraFolders, watch: folders.watchFolders)
        }
        let side = s.sideFiles.map { files in
            LibraryFoldersSideInfo(on: files.on, types: files.types)
        }
        return LibraryFoldersInfo(
            load: load(s.status),
            groups: groups,
            rulesAvailable: s.rules != nil,
            movieRules: movie.map { menuRule($0) },
            showRules: show.map { menuRule($0) },
            musicRules: music.map { menuRule($0) },
            canEdit: s.canEdit,
            isOwner: s.isOwner,
            handsOff: s.handsOff,
            handsOffBusy: s.handsOffBusy,
            needsPassword: s.needsPassword,
            passwordError: error(s.passwordError),
            canSaveImportFolders: s.canSaveImportFolders,
            importFolders: imports,
            sideFiles: side,
            tagSuggestions: s.tagSuggestions,
            adding: s.adding,
            notice: libraryNotice(s.notice),
            isEmpty: s.isEmpty
        )
    }

    static func menuRule(_ rule: PickRule) -> MenuOptionInfo {
        MenuOptionInfo(id: rule.id, label: rule.name)
    }

    static func libraryFoldersGroup(_ g: FolderGroup) -> LibraryFoldersGroupInfo {
        LibraryFoldersGroupInfo(kind: libraryFolderKind(g.kind), folders: g.folders.map { libraryFoldersRow($0) })
    }

    static func libraryFoldersRow(_ r: FolderRow) -> LibraryFoldersRowInfo {
        let f = r.folder
        return LibraryFoldersRowInfo(
            id: f.id,
            kind: libraryFolderKind(f.kind),
            label: f.label,
            path: f.path,
            shortPath: r.shortPath,
            health: LibraryFolderHealthID(rawValue: key(r.health)) ?? .unknown,
            tone: tone(r.tone),
            reason: f.healthReason,
            freeSpace: f.freeSpace,
            checkedAt: f.healthCheckedAt,
            busy: r.busy,
            rulesId: f.defaultPickRulesId?.int64Value ?? 0,
            rulesName: r.rulesName,
            rulesIsDefault: r.rulesIsDefault,
            copiesExpected: f.copiesExpected,
            tags: f.titleTags,
            onlyTagged: f.onlyTaggedSources,
            audio: f.titleAudio,
            lookOnly: f.lookOnly,
            flat: f.flat
        )
    }
}

final class LibraryImportDriver: @unchecked Sendable {
    private let presenter: LibraryImportPresenter

    init(presenter: LibraryImportPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryImportInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.libraryImport(state)
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

    func selectFolder(_ id: Int64) {
        presenter.selectFolder(id: id)
    }

    func scan() async {
        do {
            try await presenter.scan()
        } catch {}
    }

    func scanRest() async {
        do {
            try await presenter.scanRest()
        } catch {}
    }

    func setFilter(_ status: String) {
        presenter.setFilter(status: status)
    }

    func setQuery(_ text: String) {
        presenter.setQuery(text: text)
    }

    func setFollow(_ on: Bool) {
        presenter.setFollow(on: on)
    }

    func toggle(_ folder: String) {
        presenter.toggle(folder: folder)
    }

    func selectAll() {
        presenter.selectAll()
    }

    func selectNone() {
        presenter.selectNone()
    }

    func pick(folder: String, key: String) {
        presenter.pick(folder: folder, key: key)
    }

    func openLookup(_ folder: String) {
        presenter.openLookup(folder: folder)
    }

    func closeLookup() {
        presenter.closeLookup()
    }

    func lookup(_ term: String) async {
        do {
            try await presenter.lookup(term: term)
        } catch {}
    }

    func useLookup(_ result: LibraryImportLookupResultInfo) {
        let found = LookupResult(key: result.key, name: result.name, year: Int32(result.year), poster: result.poster, inLibrary: result.inLibrary)
        presenter.useLookup(result: found)
    }

    func adopt() async {
        do {
            try await presenter.adopt()
        } catch {}
    }

    func keepAsFiles() async {
        do {
            try await presenter.keepAsFiles()
        } catch {}
    }

    func openTidy() async {
        do {
            try await presenter.openTidy()
        } catch {}
    }

    func tidy() async {
        do {
            try await presenter.tidy()
        } catch {}
    }

    func undoTidy() async {
        do {
            try await presenter.undoTidy()
        } catch {}
    }

    func closeTidy() async {
        do {
            try await presenter.closeTidy()
        } catch {}
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class LibraryFilesDriver: @unchecked Sendable {
    private let presenter: LibraryFilesPresenter

    init(presenter: LibraryFilesPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryFilesInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.libraryFiles(state)
            await deliver(mapped)
        }
    }

    func load() async {
        do {
            try await presenter.load()
        } catch {}
    }

    func setMode(_ mode: LibraryFilesModeID) {
        presenter.setMode(mode: mode.rawValue)
    }

    func scan(path: String) async {
        presenter.setPath(path: path)
        do {
            try await presenter.scan()
        } catch {}
    }

    func toggle(_ path: String) {
        presenter.toggle(path: path)
    }

    func setReplace(path: String, on: Bool) {
        presenter.setReplace(path: path, on: on)
    }

    func importFiles() async {
        do {
            try await presenter.importFiles()
        } catch {}
    }

    func openBrowser() async {
        do {
            try await presenter.openBrowser()
        } catch {}
    }

    func browse(_ path: String) async {
        do {
            try await presenter.browse(path: path)
        } catch {}
    }

    func browseUp() async {
        do {
            try await presenter.browseUp()
        } catch {}
    }

    func useBrowsed() async {
        do {
            try await presenter.useBrowsed()
        } catch {}
    }

    func closeBrowser() {
        presenter.closeBrowser()
    }

    func openEditor(_ path: String) {
        presenter.openEditor(path: path)
    }

    func closeEditor() {
        presenter.closeEditor()
    }

    func editorSearch(_ query: String) async {
        do {
            try await presenter.editorSearch(query: query)
        } catch {}
    }

    func editorChoose(_ titleId: Int64) async {
        do {
            try await presenter.editorChoose(titleId: titleId)
        } catch {}
    }

    func editorVersion(_ id: Int64) {
        presenter.editorVersion(id: id)
    }

    func editorSeason(_ season: Int) {
        presenter.editorSeason(season: Int32(season))
    }

    func editorEpisode(_ episode: Int) {
        presenter.editorEpisode(episode: Int32(episode))
    }

    func editorUse() {
        presenter.editorUse()
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }
}

final class LibraryFoldersDriver: @unchecked Sendable {
    private let presenter: LibraryFoldersPresenter

    init(presenter: LibraryFoldersPresenter) {
        self.presenter = presenter
    }

    func watch(deliver: @MainActor @Sendable (LibraryFoldersInfo) -> Void) async {
        for await state in presenter.state {
            let mapped = Mapping.libraryFolders(
                state,
                movie: presenter.rulesFor(kind: "movie"),
                show: presenter.rulesFor(kind: "show"),
                music: presenter.rulesFor(kind: "music")
            )
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

    func add(
        kind: LibraryFolderKindID,
        path: String,
        label: String,
        rulesId: Int64,
        tags: [String],
        onlyTagged: Bool,
        lookOnly: Bool,
        flat: Bool
    ) async -> Bool {
        do {
            let added = try await presenter.add(
                kind: kind.rawValue,
                path: path,
                label: label,
                rulesId: rulesId,
                tags: tags,
                onlyTagged: onlyTagged,
                lookOnly: lookOnly,
                flat: flat
            )
            return Self.flag(added)
        } catch {
            return false
        }
    }

    func check(_ id: Int64) async {
        do {
            try await presenter.check(id: id)
        } catch {}
    }

    func rename(_ id: Int64, label: String) async {
        do {
            try await presenter.rename(id: id, label: label)
        } catch {}
    }

    func setRules(_ id: Int64, rulesId: Int64) async {
        do {
            try await presenter.setRules(id: id, rulesId: rulesId)
        } catch {}
    }

    func setTags(_ id: Int64, tags: [String], onlyTagged: Bool) async {
        do {
            try await presenter.setTags(id: id, tags: tags, onlyTagged: onlyTagged)
        } catch {}
    }

    func setAudio(_ id: Int64, text: String) async {
        do {
            try await presenter.setAudio(id: id, text: text)
        } catch {}
    }

    func setCopiesExpected(_ id: Int64, on: Bool) async {
        do {
            try await presenter.setCopiesExpected(id: id, on: on)
        } catch {}
    }

    func setLookOnly(_ id: Int64, on: Bool) async {
        do {
            try await presenter.setLookOnly(id: id, on: on)
        } catch {}
    }

    func setFlat(_ id: Int64, on: Bool) async {
        do {
            try await presenter.setFlat(id: id, on: on)
        } catch {}
    }

    func changePath(_ id: Int64, path: String) async -> Bool {
        do {
            let changed = try await presenter.changePath(id: id, path: path)
            return Self.flag(changed)
        } catch {
            return false
        }
    }

    func remove(_ id: Int64) async {
        do {
            try await presenter.remove(id: id)
        } catch {}
    }

    func saveImportFolders(extra: String, watch: String) async {
        do {
            try await presenter.saveImportFolders(extra: extra, watch: watch)
        } catch {}
    }

    func saveSideFiles(on: Bool, types: String) async {
        do {
            try await presenter.saveSideFiles(on: on, types: types)
        } catch {}
    }

    func setHandsOff(_ on: Bool) async {
        do {
            try await presenter.setHandsOff(on: on)
        } catch {}
    }

    func confirmPassword(password: String, code: String) async {
        do {
            try await presenter.confirmPassword(password: password, code: code)
        } catch {}
    }

    func cancelPassword() {
        presenter.cancelPassword()
    }

    func dismissNotice(_ seq: Int) {
        presenter.dismissNotice(seq: Int32(seq))
    }

    private static func flag(_ value: Any) -> Bool {
        if let plain = value as? Bool { return plain }
        return (value as? NSNumber)?.boolValue ?? false
    }
}
