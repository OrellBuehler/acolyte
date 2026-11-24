@preconcurrency import shared
import Foundation

extension Mapping {
    static func requestError(_ e: ApiError?) -> RequestErrorInfo? {
        guard let e, let info = error(e) else { return nil }
        return RequestErrorInfo(error: info, failure: AskFailureKind(rawValue: key(e.failureKind)) ?? .other)
    }

    static func ask(_ a: Ask) -> AskInfo {
        let life = a.life
        let askedWhat: String
        switch a.kind {
        case "show": askedWhat = a.seasonWords
        case "artist": askedWhat = a.albumWords
        default: askedWhat = ""
        }
        return AskInfo(
            id: a.id,
            kind: a.kind,
            tmdbId: a.tmdbId,
            name: a.name,
            year: Int(a.year),
            poster: a.poster,
            state: a.state,
            stateLabel: a.stateLabel,
            facts: [],
            qualityTag: a.qualityTagLine,
            qualityName: a.qualityName,
            askedWhat: askedWhat,
            seasons: a.seasons.map { Int($0.int32Value) },
            futureOnly: a.futureOnly,
            askedAt: Format.date(iso: a.askedAt),
            decidedAt: Format.date(iso: a.decidedAt),
            askers: a.askerNames,
            watchlist: a.people.filter { $0.fromWatchlist }.map { AskPersonInfo(id: $0.id, name: $0.name, fromWatchlist: true) },
            decider: a.deciderName,
            autoApproved: a.autoApproved,
            note: a.note,
            alsoDeclined: a.alsoDeclined.map { $0.displayName.isEmpty ? $0.name : $0.displayName },
            problem: a.problem,
            step: StepKindInfo(rawValue: key(a.stepKind)) ?? .idle,
            stepText: a.step?.text ?? "",
            searchedAt: a.step.flatMap { Format.date(iso: $0.searchedAt) },
            viaPlugin: a.viaPlugin,
            mine: a.mine,
            isWaiting: a.isWaiting,
            isFailed: a.isFailed,
            isArtist: a.isArtist,
            stage: StageKind(rawValue: key(life.stage)) ?? .processing,
            tone: tone(life.tone),
            progress: Double(life.progress),
            sharedCount: a.people.count
        )
    }

    static func inbox(_ s: InboxState) -> InboxInfo {
        InboxInfo(
            unread: Int(s.unread),
            waiting: Int(s.waiting),
            reportsOpen: Int(s.reportsOpen),
            requestsBadge: Int(s.requestsBadge),
            hasAttention: s.hasAttention
        )
    }

    private static func intMap<K: Hashable>(_ map: [AnyHashable: KotlinInt], _ make: (String) -> K?) -> [K: Int] {
        var out: [K: Int] = [:]
        for (k, v) in map {
            if let kind = make(key(k.base)) { out[kind] = Int(v.int32Value) }
        }
        return out
    }

    static func requests(_ s: RequestsState) -> RequestsInfo {
        var pending: [Int64: PendingKindInfo] = [:]
        for (k, v) in s.pending {
            pending[k.int64Value] = PendingKindInfo(rawValue: key(v)) ?? .approve
        }
        let note = s.note.map { n in
            RequestsNoteInfo(
                id: n.id,
                kind: RequestsNoteKindInfo(rawValue: key(n.kind)) ?? .notice,
                name: n.name,
                count: Int(n.count),
                by: n.by,
                state: n.state,
                text: n.text,
                error: requestError(n.error),
                keys: n.keys.map { $0.int64Value }
            )
        }
        return RequestsInfo(
            load: load(s.status),
            approver: s.approver,
            canAsk: s.canAsk,
            myId: s.myId,
            shown: s.shown.map { ask($0) },
            isEmpty: s.isEmpty,
            memberTab: MemberTabKind(rawValue: key(s.memberTab)) ?? .waiting,
            memberTabs: s.memberTabs.compactMap { MemberTabKind(rawValue: key($0)) },
            memberCounts: intMap(s.memberCounts) { MemberTabKind(rawValue: $0) },
            approverTab: ApproverTabKind(rawValue: key(s.approverTab)) ?? .waiting,
            approverCounts: intMap(s.approverCounts) { ApproverTabKind(rawValue: $0) },
            failedCount: Int(s.failedCount),
            kind: KindFilterKind(rawValue: key(s.kind)) ?? .all,
            kinds: s.kinds.compactMap { KindFilterKind(rawValue: key($0)) },
            stage: s.stage.flatMap { StageKind(rawValue: key($0)) },
            stages: s.stages.compactMap { StageKind(rawValue: key($0)) },
            stageCounts: intMap(s.stageCounts) { StageKind(rawValue: $0) },
            filtersOn: Int(s.filtersOn),
            pending: pending,
            limits: s.limits.map { line in
                LimitLineInfo(
                    kind: LimitKindInfo(rawValue: key(line.kind)) ?? .movies,
                    left: Int(line.left),
                    perWeek: Int(line.perWeek),
                    wait: line.wait.map { WaitInfo(days: Int($0.days), hours: Int($0.hours), minutes: Int($0.minutes), underMinute: $0.underMinute) }
                )
            },
            newIds: Set(s.newIds.map { $0.int64Value }),
            reportsOpen: Int(s.reportsOpen),
            hasOwnReports: s.hasOwnReports,
            isCut: s.isCut,
            loaded: s.asks.count,
            total: Int(s.total),
            busy: s.busy,
            canApproveAll: s.canApproveAll,
            waitingToApprove: Int(s.waitingToApprove),
            clearable: s.clearable.flatMap { ClearKindInfo(rawValue: key($0)) },
            clearCount: Int(s.clearCount),
            note: note
        )
    }

    static func requestPages(_ s: RequestsState) -> [RequestPageKind] {
        s.pages.compactMap { RequestPageKind(rawValue: key($0)) }
    }

    private static func tick(_ t: Any) -> TickKind {
        TickKind(rawValue: key(t)) ?? .all
    }

    private static func pill(_ p: QualityPill) -> QualityPillInfo {
        QualityPillInfo(
            key: p.key,
            line: p.line,
            name: p.name,
            rulesId: p.rulesId,
            extra: p.extra,
            disabled: p.disabled,
            note: PillNoteKind(rawValue: key(p.note)) ?? .plain
        )
    }

    static func review(_ s: AskReviewState) -> AskReviewInfo {
        AskReviewInfo(
            load: load(s.status),
            ask: s.ask.map { ask($0) },
            seasons: s.seasons.map { row in
                SeasonPickInfo(
                    number: Int(row.number),
                    all: Int(row.all),
                    asked: Int(row.asked),
                    kept: Int(row.kept),
                    tick: tick(row.tick),
                    expanded: row.expanded,
                    loading: row.loading,
                    failed: row.failed,
                    episodes: row.episodes.map { EpisodeTickInfo(number: Int($0.number), name: $0.name, ticked: $0.ticked) }
                )
            },
            albums: s.albums.map { row in
                AlbumPickInfo(
                    mbid: row.mbid,
                    name: row.name,
                    year: Int(row.year),
                    kind: AlbumKindInfo(rawValue: key(row.kind)) ?? .other,
                    editions: Int(row.editions),
                    tick: tick(row.tick),
                    expanded: row.expanded,
                    songs: row.songs.map { SongTickInfo(key: $0.key, name: $0.name, disc: Int($0.disc), number: Int($0.number), ticked: $0.ticked) },
                    kept: Int(row.kept)
                )
            },
            albumCounts: intMap(s.albumCounts) { AlbumKindInfo(rawValue: $0) },
            pills: s.pills.map { pill($0) },
            selected: s.selected,
            changed: s.changed,
            empty: s.empty,
            canApprove: s.canApprove,
            showsQuality: s.showsQuality,
            extraOnly: s.extraOnly,
            keptSeasons: Int(s.keptSeasons),
            keptEpisodes: Int(s.keptEpisodes),
            allSongs: Int(s.allSongs),
            keptSongs: Int(s.keptSongs)
        )
    }

    private static func brief(_ b: AskBrief?) -> AskBriefInfo? {
        guard let b else { return nil }
        return AskBriefInfo(stateLabel: b.stateLabel, mine: b.mine, seasons: b.seasons.map { Int($0.int32Value) }, albums: b.albums.map { $0.name })
    }

    private static func outcome(_ o: Any?) -> AskOutcomeKind? {
        guard let o else { return nil }
        return AskOutcomeKind(rawValue: key(o))
    }

    static func askSheet(_ s: AskSheetState) -> AskSheetInfo {
        AskSheetInfo(
            load: load(s.status),
            isShow: s.isShow,
            name: s.name,
            year: Int(s.year),
            neverAdd: s.neverAdd,
            brief: brief(s.brief),
            joining: s.joining,
            pills: s.pills.map { pill($0) },
            showsQuality: s.showsQuality,
            selected: s.selected,
            alsoPills: s.alsoPills.map { pill($0) },
            also: Set(s.also),
            follow: s.follow,
            mode: ShowModeKind(rawValue: key(s.mode)) ?? .all,
            chooseAllowed: s.chooseAllowed,
            seasons: s.seasons.map { row in
                AskSeasonInfo(
                    number: Int(row.number),
                    name: row.name,
                    kept: row.kept,
                    complete: row.complete,
                    ticked: row.ticked,
                    expanded: row.expanded,
                    load: EpisodesLoadKind(rawValue: key(row.load)) ?? .idle,
                    episodes: row.episodes.map { EpisodeTickInfo(number: Int($0.number), name: $0.name, ticked: $0.ticked) },
                    picked: Int(row.picked),
                    available: row.available
                )
            },
            pickedSeasons: s.pickedSeasons.map { Int($0.int32Value) },
            pickedEpisodes: Int(s.pickedEpisodes),
            busy: s.busy,
            error: requestError(s.error),
            outcome: outcome(s.outcome),
            outcomeFourK: s.outcomeFourK,
            canSubmit: s.canSubmit
        )
    }

    static func askMusic(_ s: AskMusicState) -> AskMusicInfo {
        AskMusicInfo(
            load: load(s.status),
            name: s.name,
            albums: s.albums.map { row in
                AskAlbumInfo(
                    mbid: row.mbid,
                    name: row.name,
                    year: Int(row.year),
                    kind: AlbumKindInfo(rawValue: key(row.kind)) ?? .other,
                    kept: row.kept,
                    leftOut: row.leftOut,
                    ticked: row.ticked,
                    available: row.available
                )
            },
            mode: MusicModeKind(rawValue: key(s.mode)) ?? .every,
            follow: s.follow,
            brief: brief(s.brief),
            picked: s.picked.map { $0.name },
            someAllowed: s.someAllowed,
            busy: s.busy,
            error: requestError(s.error),
            outcome: outcome(s.outcome),
            canSubmit: s.canSubmit
        )
    }

    static func report(_ r: Report) -> ReportInfo {
        ReportInfo(
            id: r.id,
            titleName: r.titleName,
            titleKind: r.titleKind,
            year: Int(r.year),
            poster: r.poster,
            part: r.partWords,
            version: r.versionWord,
            kind: ReportKindInfo(rawValue: key(r.reportKind)) ?? .other,
            note: r.note,
            isOpen: r.isOpen,
            mine: r.mine,
            reporter: r.reporterName,
            fixer: r.fixerName,
            reply: r.reply,
            createdAt: Format.date(iso: r.createdAt),
            closedAt: Format.date(iso: r.closedAt)
        )
    }

    static func reports(_ s: ReportsState) -> ReportsInfo {
        ReportsInfo(
            load: load(s.status),
            manager: s.manager,
            filter: ReportFilterKind(rawValue: key(s.filter)) ?? .all,
            counts: intMap(s.counts) { ReportFilterKind(rawValue: $0) },
            shown: s.shown.map { report($0) },
            busy: Set(s.busy.map { $0.int64Value }),
            searching: Set(s.searching.map { $0.int64Value }),
            note: s.note.map { n in
                ReportsNoteInfo(
                    id: n.id,
                    kind: ReportsNoteKindInfo(rawValue: key(n.kind)) ?? .fixed,
                    name: n.name,
                    reporter: n.reporter,
                    error: requestError(n.error)
                )
            }
        )
    }

    static func reportProblem(_ s: ReportProblemState) -> ReportProblemInfo {
        ReportProblemInfo(
            kind: s.kind.flatMap { ReportKindInfo(rawValue: key($0)) },
            note: s.note,
            left: Int(s.left),
            busy: s.busy,
            error: requestError(s.error),
            missingKind: s.missingKind,
            sent: s.sent.map { report($0) }
        )
    }

    private static var folderFields: [(FolderFieldKind, FolderField)] {
        [
            (.movie, FolderField.movie), (.show, FolderField.show), (.music, FolderField.music),
            (.movieFourK, FolderField.movieFourK), (.showFourK, FolderField.showFourK),
        ]
    }

    private static var settingsFlags: [(SettingsFlagKind, SettingsFlag)] {
        [
            (.searchOnApprove, SettingsFlag.searchOnApprove), (.ownerAutoApprove, SettingsFlag.ownerAutoApprove),
            (.plexWatchlists, SettingsFlag.plexWatchlists), (.plexEveryone, SettingsFlag.plexEveryone),
            (.plexFriends, SettingsFlag.plexFriends), (.pickMany, SettingsFlag.pickMany), (.skipOnServices, SettingsFlag.skipOnServices),
        ]
    }

    static func requestRules(_ s: RequestSettingsState) -> RequestRulesInfo {
        var folders: [FolderFieldKind: [PickOption]] = [:]
        var rules: [FolderFieldKind: [PickOption]] = [:]
        var folder: [FolderFieldKind: Int64] = [:]
        var pickRules: [FolderFieldKind: Int64] = [:]
        for (kind, field) in folderFields {
            folders[kind] = s.foldersFor(field: field).map { PickOption(id: $0.id, name: $0.label.isEmpty ? $0.path : "\($0.label) (\($0.path))") }
            rules[kind] = s.rulesFor(field: field).map { PickOption(id: $0.id, name: $0.name) }
            folder[kind] = s.folder(field: field)
            pickRules[kind] = s.pickRules(field: field)
        }
        var flags: [SettingsFlagKind: Bool] = [:]
        var enabled: [SettingsFlagKind: Bool] = [:]
        for (kind, flag) in settingsFlags {
            flags[kind] = s.flag(flag: flag)
            enabled[kind] = s.flagEnabled(flag: flag)
        }
        var limits: [LimitKindInfo: String] = [:]
        limits[.movies] = s.limit(kind: LimitKind.movies)
        limits[.shows] = s.limit(kind: LimitKind.shows)
        limits[.music] = s.limit(kind: LimitKind.music)
        var lines: [LineKindInfo: [QualityLineInfo]] = [:]
        var canAdd: [LineKindInfo: Bool] = [:]
        for (kind, line) in [(LineKindInfo.movie, LineKind.movie), (LineKindInfo.show, LineKind.show)] {
            lines[kind] = s.lines(kind: line).enumerated().map { index, l in
                QualityLineInfo(
                    index: index,
                    lineId: l.id,
                    name: l.name,
                    label: l.label,
                    hidden: l.hidden,
                    pickRulesId: l.pickRulesId,
                    rulesName: s.ruleName(id: l.pickRulesId),
                    extra: l.extra,
                    ready: l.ready,
                    isOwn: l.isOwn
                )
            }
            canAdd[kind] = s.canAddLine(kind: line)
        }
        let picked = Set(s.draft.streamingServices.map { $0.int64Value })
        let check = s.cleanup.map { c in
            CleanupInfo(
                running: c.running,
                checked: Int(c.checked),
                total: Int(c.total),
                failed: Int(c.failed),
                finished: !c.finishedAt.isEmpty,
                found: c.found.map {
                    OnServicesInfo(id: $0.id, kind: $0.kind, name: $0.name, year: Int($0.year), tmdbId: $0.tmdbId, bytes: $0.bytes, service: $0.service)
                }
            )
        }
        return RequestRulesInfo(
            load: load(s.status),
            allowed: s.allowed,
            limits: limits,
            limitsValid: s.limitsValid,
            folders: folders,
            rules: rules,
            folder: folder,
            pickRules: pickRules,
            flags: flags,
            flagEnabled: enabled,
            lines: lines,
            canAddLine: canAdd,
            showFriends: s.showFriends,
            friendsLoaded: s.plexFriends != nil,
            ownerLinked: s.plexFriends?.ownerLinked ?? false,
            friends: (s.plexFriends?.people ?? []).map {
                PlexFriendInfo(
                    id: $0.userId,
                    name: $0.name,
                    plexName: $0.plexName,
                    kid: $0.kid,
                    checkedAt: Format.date(iso: $0.checkedAt),
                    error: $0.error,
                    asked: Int($0.asked)
                )
            },
            region: s.draft.streamingRegion,
            services: s.sortedServices.map { ServiceInfo(id: $0.id, name: $0.name, logo: $0.logo, picked: picked.contains($0.id)) },
            servicesLoading: s.servicesLoading,
            servicesError: error(s.servicesError),
            pickedCount: picked.count,
            cleanup: check,
            cleanupError: requestError(s.cleanupError),
            canCheck: s.canCheck,
            dirty: s.dirty,
            canSave: s.canSave,
            saving: s.saving,
            saveError: requestError(s.saveError),
            savedCount: Int(s.savedCount)
        )
    }

    static func regions(_ s: RequestSettingsState) -> [String] {
        s.regions
    }

    private static func listKind(_ wire: String) -> ListKindInfo {
        switch wire {
        case "trakt_list": .trakt
        case "mdblist_list": .mdblist
        default: .tmdb
        }
    }

    static func outsideLists(_ s: OutsideListsState) -> OutsideListsInfo {
        let busy = Set(s.busy.map { $0.int64Value })
        let draft = s.formOpen ? s.draft : nil
        return OutsideListsInfo(
            load: load(s.status),
            allowed: s.allowed,
            owner: s.owner,
            lists: s.lists.map { l in
                OutsideListInfo(
                    id: l.id,
                    name: l.name,
                    kind: listKind(l.kind),
                    isShow: l.titleKind == "show",
                    ref: l.ref,
                    link: l.link,
                    enabled: l.enabled,
                    syncEveryHours: Int(l.syncEveryHours),
                    lastSyncedAt: Format.date(iso: l.lastSyncedAt),
                    lastResult: l.lastResult,
                    lastError: l.lastError,
                    busy: busy.contains(l.id)
                )
            },
            isEmpty: s.isEmpty,
            formOpen: s.formOpen,
            isNew: (s.editing?.int64Value ?? 0) == 0,
            draft: draft.map { d in
                ListDraftInfo(
                    name: d.name,
                    kind: listKind(d.kind),
                    isShow: d.titleKind == "show",
                    ref: d.ref,
                    folderId: d.libraryFolderId,
                    rulesId: d.pickRulesId,
                    keepUpToDate: d.keepUpToDate,
                    searchOnAdd: d.searchOnAdd,
                    seasons: ListSeasonsKind(rawValue: d.seasons) ?? .all,
                    newSeasons: d.newSeasons == "all",
                    enabled: d.enabled,
                    syncEveryHours: Int(d.syncEveryHours)
                )
            },
            draftFolders: s.draftFolders.map { PickOption(id: $0.id, name: $0.label.isEmpty ? $0.path : "\($0.label) (\($0.path))") },
            draftRules: s.draftRules.map { PickOption(id: $0.id, name: $0.name) },
            formError: requestError(s.formError),
            saving: s.saving,
            canSave: s.canSave,
            needsTraktKey: s.needsTraktKey,
            traktKeySet: s.traktKeySet,
            traktTokenSet: !s.traktToken.isEmpty,
            hasTraktList: s.hasTraktList,
            traktKnown: s.trakt != nil,
            traktSignedIn: s.trakt?.signedIn ?? false,
            traktUser: s.trakt?.username ?? "",
            traktProblem: s.trakt?.problem ?? "",
            traktSecret: s.trakt?.clientSecret ?? false,
            canSignIn: s.canSignIn,
            mdblistSet: !s.mdblistKey.isEmpty,
            keyBusy: s.keyBusy,
            keyError: requestError(s.keyError),
            device: s.device.map { DeviceCodeInfo(userCode: $0.userCode, url: $0.url) },
            note: s.note.map { OutsideNoteInfo(id: $0.id, kind: OutsideNoteKindInfo(rawValue: key($0.kind)) ?? .saved, name: $0.name) }
        )
    }

    static func neverAdd(_ s: NeverAddState) -> NeverAddInfo {
        let removing = Set(s.removing.map { $0.int64Value })
        return NeverAddInfo(
            load: load(s.status),
            allowed: s.allowed,
            entries: s.entries.map {
                NeverAddEntryInfo(id: $0.id, isShow: $0.kind == "show", tmdb: $0.tmdb, name: $0.name, reason: $0.reason, removing: removing.contains($0.id))
            },
            busy: s.busy,
            error: requestError(s.error),
            missing: s.missing,
            added: Int(s.added),
            removed: s.removed
        )
    }

    static func kotlinMemberTab(_ t: MemberTabKind) -> MemberTab {
        switch t {
        case .waiting: MemberTab.waiting
        case .approved: MemberTab.approved
        case .declined: MemberTab.declined
        case .ready: MemberTab.ready
        case .failed: MemberTab.failed
        }
    }

    static func kotlinApproverTab(_ t: ApproverTabKind) -> ApproverTab {
        switch t {
        case .waiting: ApproverTab.waiting
        case .processing: ApproverTab.processing
        case .done: ApproverTab.done
        }
    }

    static func kotlinKind(_ k: KindFilterKind) -> KindFilter {
        switch k {
        case .all: KindFilter.all
        case .movie: KindFilter.movie
        case .show: KindFilter.show
        case .artist: KindFilter.artist
        }
    }

    static func kotlinStage(_ s: StageKind) -> Stage {
        switch s {
        case .failed: Stage.failed
        case .waiting: Stage.waiting
        case .upcoming: Stage.upcoming
        case .searching: Stage.searching
        case .downloading: Stage.downloading
        case .importing: Stage.importing
        case .needsDecision: Stage.needsDecision
        case .looking: Stage.looking
        case .processing: Stage.processing
        case .ready: Stage.ready
        case .declined: Stage.declined
        }
    }

    static func kotlinClear(_ c: ClearKindInfo) -> ClearKind {
        switch c {
        case .failed: ClearKind.failed
        case .finished: ClearKind.finished
        }
    }

    static func kotlinShowMode(_ m: ShowModeKind) -> ShowMode {
        switch m {
        case .all: ShowMode.all
        case .choose: ShowMode.choose
        case .future: ShowMode.future
        }
    }

    static func kotlinMusicMode(_ m: MusicModeKind) -> MusicMode {
        switch m {
        case .every: MusicMode.every
        case .some: MusicMode.some
        }
    }

    static func kotlinReportFilter(_ f: ReportFilterKind) -> ReportFilter {
        switch f {
        case .open: ReportFilter.open
        case .fixed: ReportFilter.fixed
        case .all: ReportFilter.all
        }
    }

    static func kotlinReportKind(_ k: ReportKindInfo) -> ReportKind {
        switch k {
        case .picture: ReportKind.picture
        case .sound: ReportKind.sound
        case .subtitles: ReportKind.subtitles
        case .wontPlay: ReportKind.wontPlay
        case .other: ReportKind.other
        }
    }

    static func kotlinLimit(_ k: LimitKindInfo) -> LimitKind {
        switch k {
        case .movies: LimitKind.movies
        case .shows: LimitKind.shows
        case .music: LimitKind.music
        }
    }

    static func kotlinFolder(_ f: FolderFieldKind) -> FolderField {
        switch f {
        case .movie: FolderField.movie
        case .show: FolderField.show
        case .music: FolderField.music
        case .movieFourK: FolderField.movieFourK
        case .showFourK: FolderField.showFourK
        }
    }

    static func kotlinFlag(_ f: SettingsFlagKind) -> SettingsFlag {
        switch f {
        case .searchOnApprove: SettingsFlag.searchOnApprove
        case .ownerAutoApprove: SettingsFlag.ownerAutoApprove
        case .plexWatchlists: SettingsFlag.plexWatchlists
        case .plexEveryone: SettingsFlag.plexEveryone
        case .plexFriends: SettingsFlag.plexFriends
        case .pickMany: SettingsFlag.pickMany
        case .skipOnServices: SettingsFlag.skipOnServices
        }
    }

    static func kotlinLine(_ k: LineKindInfo) -> LineKind {
        switch k {
        case .movie: LineKind.movie
        case .show: LineKind.show
        }
    }

    static func kotlinListKind(_ k: ListKindInfo) -> ListKind {
        switch k {
        case .tmdb: ListKind.tmdb
        case .trakt: ListKind.trakt
        case .mdblist: ListKind.mdblist
        }
    }

    static func kotlinListSeasons(_ s: ListSeasonsKind) -> ListSeasons {
        switch s {
        case .all: ListSeasons.all
        case .future: ListSeasons.future
        case .latest: ListSeasons.latest
        case .first: ListSeasons.first
        case .noSeasons: ListSeasons.none
        }
    }
}
