@preconcurrency import shared
import Foundation

enum DlFormat {
    static func size(_ bytes: Int64) -> String {
        DownloadFormat.shared.size(bytes: bytes)
    }

    static func freeSize(_ bytes: Int64) -> String {
        DownloadFormat.shared.freeSize(bytes: bytes)
    }

    static func speed(_ bytes: Int64) -> String {
        DownloadFormat.shared.speed(bytes: bytes)
    }

    static func percent(_ progress: Double) -> Int {
        Int(DownloadFormat.shared.percent(progress: progress))
    }

    static func shortRatio(_ ratio: Double) -> String {
        DownloadRules.shared.shortRatio(ratio: ratio)
    }

    static func ratioText(_ ratio: Double, decimals: Int) -> String {
        DownloadRules.shared.ratioText(ratio: ratio, decimals: Int32(decimals))
    }

    static func limitsProblem(ratio: Double, seedingMinutes: Int64, inactiveMinutes: Int64) -> Bool {
        DownloadRules.shared.limitsProblem(ratio: ratio, seedingMinutes: seedingMinutes, inactiveMinutes: inactiveMinutes)
    }

    static func firstDescending(_ sortKey: SortKeyKind) -> Bool {
        DownloadRules.shared.firstDescending(key: Mapping.kotlinSort(sortKey))
    }
}

extension Mapping {
    static func truthy(_ value: Any) -> Bool {
        if let flag = value as? Bool { return flag }
        if let number = value as? NSNumber { return number.boolValue }
        return false
    }

    static func strings(_ value: Any) -> [String] {
        if let list = value as? [String] { return list }
        if let list = value as? [Any] { return list.compactMap { $0 as? String } }
        return []
    }

    static func byteArray(_ data: Data) -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(data.count))
        for (index, byte) in data.enumerated() {
            array.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return array
    }

    static func dayInfo(_ d: Day) -> DlDay {
        DlDay(year: Int(d.year), month: Int(d.month), day: Int(d.day))
    }

    static func kotlinDay(_ d: DlDay) -> Day {
        Day(year: Int32(d.year), month: Int32(d.month), day: Int32(d.day))
    }

    static func kotlinTab(_ t: DownloadsTabKind) -> DownloadsTab {
        switch t {
        case .all: DownloadsTab.all
        case .waiting: DownloadsTab.waiting
        case .progress: DownloadsTab.progress
        case .problems: DownloadsTab.problems
        case .done: DownloadsTab.done
        }
    }

    static func kotlinSub(_ s: DownloadsSubKind) -> DownloadsSub {
        switch s {
        case .queued: DownloadsSub.queued
        case .paused: DownloadsSub.paused
        case .downloading: DownloadsSub.downloading
        case .checking: DownloadsSub.checking
        case .importing: DownloadsSub.importing
        case .failed: DownloadsSub.failed
        case .decide: DownloadsSub.decide
        }
    }

    static func kotlinKind(_ k: DlKindFilter) -> DownloadKindFilter {
        switch k {
        case .all: DownloadKindFilter.all
        case .movie: DownloadKindFilter.movie
        case .show: DownloadKindFilter.show
        case .music: DownloadKindFilter.music
        }
    }

    static func kotlinNetwork(_ n: DlNetworkFilter) -> NetworkFilter {
        switch n {
        case .any: NetworkFilter.any
        case .usenet: NetworkFilter.usenet
        case .torrent: NetworkFilter.torrent
        }
    }

    static func kotlinSort(_ k: SortKeyKind) -> SortKey {
        switch k {
        case .title: SortKey.title
        case .progress: SortKey.progress
        case .status: SortKey.status
        case .size: SortKey.size
        case .quality: SortKey.quality
        case .added: SortKey.added
        case .seeds: SortKey.seeds
        case .peers: SortKey.peers
        case .ratio: SortKey.ratio
        case .tracker: SortKey.tracker
        case .requested: SortKey.requested
        case .downloader: SortKey.downloader
        }
    }

    static func kotlinAction(_ a: DownloadActionKind) -> DownloadAction {
        switch a {
        case .pause: DownloadAction.pause
        case .resume: DownloadAction.resume
        case .retry: DownloadAction.retry
        case .moveUp: DownloadAction.moveUp
        case .moveDown: DownloadAction.moveDown
        case .moveTop: DownloadAction.moveTop
        case .moveBottom: DownloadAction.moveBottom
        case .priority: DownloadAction.priority
        case .importAgain: DownloadAction.importAgain
        case .openTitle: DownloadAction.openTitle
        case .remove: DownloadAction.remove
        case .recheck: DownloadAction.recheck
        case .reannounce: DownloadAction.reannounce
        case .forceStart: DownloadAction.forceStart
        case .seedingRules: DownloadAction.seedingRules
        case .setLocation: DownloadAction.setLocation
        case .speedLimits: DownloadAction.speedLimits
        case .markFailed: DownloadAction.markFailed
        }
    }

    static func kotlinPriority(_ p: UsenetPriorityKind) -> UsenetPriority {
        switch p {
        case .low: UsenetPriority.low
        case .normal: UsenetPriority.normal
        case .high: UsenetPriority.high
        case .force: UsenetPriority.force
        }
    }

    static func kotlinDecide(_ d: DecideKind) -> DecideAction {
        switch d {
        case .search: DecideAction.search
        case .turnDown: DecideAction.turnDown
        case .stop: DecideAction.stop
        case .importFile: DecideAction.importFile
        }
    }

    static func kotlinDetailTab(_ t: DetailTabKind) -> DetailTab {
        switch t {
        case .info: DetailTab.info
        case .files: DetailTab.files
        case .peers: DetailTab.peers
        case .trackers: DetailTab.trackers
        case .servers: DetailTab.servers
        case .log: DetailTab.log
        }
    }

    static func kotlinCalendarType(_ t: CalendarTypeKind) -> CalendarType {
        switch t {
        case .all: CalendarType.all
        case .movies: CalendarType.movies
        case .shows: CalendarType.shows
        case .music: CalendarType.music
        }
    }

    static func kotlinCalendarFollows(_ f: CalendarFollowsKind) -> CalendarFollows {
        switch f {
        case .mine: CalendarFollows.mine
        case .everyone: CalendarFollows.everyone
        case .library: CalendarFollows.library
        }
    }

    static func kotlinCalendarView(_ v: CalendarViewKind) -> CalendarLayout {
        switch v {
        case .agenda: CalendarLayout.agenda
        case .month: CalendarLayout.month
        }
    }

    static func downloads(_ s: DownloadsState, actions: [Any], targets: [SpeedTarget]) -> DownloadsInfo {
        let items = s.items.map { downloadItem($0) }
        let tabs = s.tabs.compactMap { tabCount($0) }
        let subs = s.subs.compactMap { subCount($0) }
        let trackers = s.trackers.map { trackerOption($0) }
        let notes = s.notes.compactMap { clientNote($0) }
        let matches = s.matches.matches.map { match($0) }
        let addTargets = s.addTargets.map { DownloaderTargetInfo(id: $0.id, name: $0.name, category: $0.category) }
        let speedTargets = targets.map { speedTarget($0) }
        let selectionActions = actions.compactMap { DownloadActionKind(rawValue: key($0)) }
        let empty = s.empty.flatMap { downloadsEmpty($0) }
        return DownloadsInfo(
            load: load(s.status),
            manager: s.manager,
            canSettings: s.canSettings,
            filter: downloadsFilter(s.filter),
            items: items,
            total: Int(s.total),
            tabs: tabs,
            subs: subs,
            trackers: trackers,
            trackerIssues: Int(s.trackerIssues),
            empty: empty,
            notes: notes,
            statusLine: statusLine(s.statusLine),
            matches: matches,
            matchesPending: Int(s.matches.pending),
            addTargets: addTargets,
            speedTargets: speedTargets,
            moreOutside: Int(s.moreOutside),
            loadingMore: s.loadingMore,
            selection: s.selection,
            busy: s.busy,
            batchBusy: s.batchBusy,
            selectionActions: selectionActions,
            notice: downloadsNotice(s.notice),
            actionError: error(s.actionError),
            isEmpty: s.isEmpty
        )
    }

    static func tabCount(_ c: TabCount) -> TabCountInfo? {
        guard let tab = DownloadsTabKind(rawValue: key(c.tab)) else { return nil }
        return TabCountInfo(tab: tab, count: Int(c.count))
    }

    static func subCount(_ c: SubCount) -> SubCountInfo? {
        guard let sub = DownloadsSubKind(rawValue: key(c.sub)) else { return nil }
        return SubCountInfo(sub: sub, count: Int(c.count))
    }

    static func trackerOption(_ t: TrackerOption) -> TrackerOptionInfo {
        TrackerOptionInfo(
            key: t.key,
            kind: TrackerFilterKindInfo(rawValue: key(t.kind)) ?? .host,
            label: t.label,
            count: Int(t.count)
        )
    }

    static func clientNote(_ n: ClientNote) -> ClientNoteInfo? {
        guard let kind = ClientNoteKindInfo(rawValue: key(n.kind)) else { return nil }
        return ClientNoteInfo(name: n.name, kind: kind)
    }

    static func match(_ m: MatchView) -> DlMatchInfo {
        DlMatchInfo(id: m.id, name: m.name, candidates: m.candidates.map { matchCandidate($0) })
    }

    static func matchCandidate(_ c: MatchCandidate) -> MatchCandidateInfo {
        MatchCandidateInfo(id: c.id, kind: c.kind, name: c.name, year: Int(c.year), poster: c.poster)
    }

    static func speedTarget(_ t: SpeedTarget) -> SpeedTargetInfo {
        SpeedTargetInfo(
            id: t.id,
            name: t.name,
            torrent: t.torrent,
            downMb: t.downMb,
            upMb: t.upMb,
            slowMode: t.slowMode?.boolValue
        )
    }

    static func kotlinSpeedTarget(_ t: SpeedTargetInfo) -> SpeedTarget {
        let slow: KotlinBoolean? = t.slowMode.map { KotlinBoolean(bool: $0) }
        return SpeedTarget(id: t.id, name: t.name, torrent: t.torrent, downMb: t.downMb, upMb: t.upMb, slowMode: slow)
    }

    static func statusLine(_ l: StatusLine) -> StatusLineInfo {
        StatusLineInfo(speed: l.speed, upSpeed: l.upSpeed, free: l.free, left: l.left, review: Int(l.review), limited: l.limited)
    }

    static func downloadsEmpty(_ e: DownloadsEmpty) -> DownloadsEmptyInfo? {
        guard let kind = DownloadsEmptyKindInfo(rawValue: key(e.kind)) else { return nil }
        return DownloadsEmptyInfo(kind: kind, count: Int(e.count))
    }

    static func downloadsFilter(_ f: DownloadsFilter) -> DownloadsFilterInfo {
        let sub = f.sub.flatMap { DownloadsSubKind(rawValue: key($0)) }
        var sort: DlSortInfo?
        if let order = f.sort, let sortKey = SortKeyKind(rawValue: key(order.key)) {
            sort = DlSortInfo(key: sortKey, descending: order.descending)
        }
        return DownloadsFilterInfo(
            tab: DownloadsTabKind(rawValue: key(f.tab)) ?? .all,
            sub: sub,
            query: f.query,
            kind: DlKindFilter(rawValue: key(f.kind)) ?? .all,
            network: DlNetworkFilter(rawValue: key(f.network)) ?? .any,
            tracker: f.tracker,
            sort: sort,
            filtersOn: Int(f.filtersOn)
        )
    }

    static func downloadsNotice(_ n: DownloadsNotice?) -> DownloadsNoticeInfo? {
        guard let n, let kind = NoticeKindInfo(rawValue: key(n.kind)) else { return nil }
        return DownloadsNoticeInfo(
            id: n.id,
            kind: kind,
            count: Int(n.count),
            skipped: Int(n.skipped),
            text: n.text,
            tone: tone(n.tone),
            outcome: n.outcome.map { addOutcome($0) }
        )
    }

    static func addOutcome(_ o: AddOutcome) -> AddOutcomeInfo {
        AddOutcomeInfo(
            added: Int(o.added),
            failed: Int(o.failed),
            pending: Int(o.pending),
            duplicates: Int(o.duplicates),
            bad: o.bad,
            dropped: Int(o.dropped)
        )
    }

    static func downloadItem(_ i: DownloadItem) -> DownloadItemInfo {
        let actions = i.actions.compactMap { DownloadActionKind(rawValue: key($0)) }
        let row = i.row
        return DownloadItemInfo(
            key: i.key,
            name: i.name,
            title: i.title,
            subLine: subLine(i.subLine),
            word: StatusWordKind(rawValue: key(i.word)) ?? .done,
            note: i.note.flatMap { rowNote($0) },
            problem: i.problem.flatMap { rowProblem($0) },
            detail: i.detail.flatMap { rowDetail($0) },
            bar: BarToneKind(rawValue: key(i.bar)) ?? .go,
            tone: tone(i.tone),
            progress: i.progress,
            moving: i.moving,
            history: i.history,
            poster: i.poster,
            square: i.square,
            actions: actions,
            selectable: i.selectable,
            titleId: row.titleId,
            size: row.size,
            speed: row.speed,
            upSpeed: row.upSpeed,
            eta: row.eta,
            decisions: decisionGroups(i.decisions)
        )
    }

    static func subLine(_ l: SubLine) -> SubLineInfo {
        SubLineInfo(kind: SubLineKindInfo(rawValue: key(l.kind)) ?? .text, text: l.text, from: Int(l.from), to: Int(l.to))
    }

    static func rowDetail(_ d: RowDetail) -> RowDetailInfo? {
        guard let kind = DetailKindInfo(rawValue: key(d.kind)) else { return nil }
        return RowDetailInfo(kind: kind, text: d.text)
    }

    static func rowNote(_ n: RowNote) -> RowNoteInfo? {
        guard let kind = NoteKindInfo(rawValue: key(n.kind)) else { return nil }
        return RowNoteInfo(kind: kind, number: Int(n.number), text: n.text, detail: n.detail.flatMap { rowDetail($0) })
    }

    static func rowProblem(_ p: RowProblem) -> RowProblemInfo? {
        guard let kind = ProblemKindInfo(rawValue: key(p.kind)) else { return nil }
        return RowProblemInfo(kind: kind, tone: tone(p.tone))
    }

    static func decisionGroups(_ items: [Decision]) -> [DecisionGroupInfo] {
        if items.isEmpty { return [] }
        let groups = DecisionRules.shared.groups(items: items)
        return groups.map { group in
            DecisionGroupInfo(
                key: group.key,
                lead: decision(group.lead),
                items: group.items.map { decision($0) },
                pack: group.pack,
                packImportAgain: group.packImportAgain
            )
        }
    }

    static func decision(_ d: Decision) -> DecisionInfo {
        let rules = DecisionRules.shared
        let plan = rules.plan(d: d)
        let line = rules.reasonLine(d: d)
        let steps = plan.more.compactMap { DecisionStepKind(rawValue: key($0)) }
        let planInfo = DecisionPlanInfo(
            lead: DecisionStepKind(rawValue: key(plan.lead)) ?? .search,
            beside: plan.beside.flatMap { DecisionStepKind(rawValue: key($0)) },
            more: steps,
            turnDown: plan.turnDown,
            hint: DecisionHintKind(rawValue: key(plan.hint)) ?? DecisionHintKind.none
        )
        let reason = ReasonLineInfo(
            kind: ReasonLineKindInfo(rawValue: key(line.kind)) ?? .why,
            text: line.text,
            why: WhyKindInfo(rawValue: key(line.why)) ?? .ranOut
        )
        return DecisionInfo(
            slotId: d.slotId,
            titleId: d.titleId,
            downloadId: d.downloadId,
            name: DownloadRules.shared.decisionName(d: d),
            releaseName: d.releaseName,
            version: d.version,
            since: d.since,
            retryAt: d.retryAt,
            reading: d.reading,
            hasCopy: d.hasCopy,
            kind: d.kind,
            importFile: d.importFile,
            packTotal: d.pack.count,
            episodeLabel: d.episode?.label ?? "",
            episodeName: d.episode?.name ?? "",
            reason: d.reason,
            reasonLine: reason,
            why: WhyKindInfo(rawValue: key(rules.whyOf(d: d))) ?? .ranOut,
            plan: planInfo
        )
    }

    static func removePlan(_ p: DownloadRemovePlan) -> RemovePlanInfo {
        RemovePlanInfo(
            keys: p.keys,
            names: p.names,
            more: Int(p.more),
            single: p.single,
            release: p.releaseName,
            doneLine: p.doneLine.flatMap { RemoveDoneLineKind(rawValue: key($0)) },
            downloader: p.downloader,
            showDeleteFiles: p.showDeleteFiles,
            showTurnDown: p.showTurnDown,
            deleteFiles: p.deleteFiles,
            turnDown: p.turnDown,
            searchAgain: p.searchAgain,
            blocked: Int(p.blocked),
            filesLine: RemoveFilesLineKind(rawValue: key(p.filesLine)) ?? .one,
            filesPath: p.filesPath,
            filesClients: p.filesClients,
            keepsSome: p.keepsSome,
            keepsPath: p.keepsPath
        )
    }

    static func shareLimits(_ l: ShareLimits) -> ShareLimitsInfo {
        ShareLimitsInfo(ratio: l.ratio, seedingMinutes: l.seedingMinutes, inactiveMinutes: l.inactiveMinutes, action: l.action)
    }

    static func downloadDetail(_ s: DownloadDetailState) -> DownloadDetailInfo {
        let tabs = s.tabs.compactMap { DetailTabKind(rawValue: key($0)) }
        let groups = s.info.compactMap { infoGroup($0) }
        let tree = s.fileTree.map { fileNode($0) }
        let peers = s.peers.enumerated().map { peerRow($0.offset, $0.element) }
        let trackers = s.trackers.enumerated().map { trackerRow($0.offset, $0.element) }
        let servers = s.servers.enumerated().map { serverRow($0.offset, $0.element) }
        let repair = s.repair.map { RepairStepInfo(stage: $0.stage, lines: $0.lines) }
        let log = s.log.enumerated().map { logEntry($0.offset, $0.element) }
        let torrentError = !(s.detail?.torrentError.isEmpty ?? true)
        return DownloadDetailInfo(
            tabs: tabs,
            tab: DetailTabKind(rawValue: key(s.tab)) ?? .info,
            live: s.live,
            reading: s.reading,
            readFailed: s.readFailed,
            canEdit: s.canEdit,
            busy: s.busy,
            actionError: error(s.actionError),
            needsReauth: s.needsReauth,
            notice: s.notice.flatMap { DetailNoticeKind(rawValue: key($0)) },
            info: groups,
            torrentError: torrentError,
            fileCount: s.files?.count ?? 0,
            singleFile: s.singleFile,
            fileFilter: s.fileFilter,
            fileTree: tree,
            peers: peers,
            trackers: trackers,
            servers: servers,
            repair: repair,
            log: log
        )
    }

    static func infoGroup(_ g: InfoGroup) -> InfoGroupInfo? {
        guard let kind = InfoGroupKindInfo(rawValue: key(g.kind)) else { return nil }
        let facts = g.facts.enumerated().compactMap { infoFact($0.offset, $0.element) }
        return InfoGroupInfo(kind: kind, facts: facts)
    }

    static func infoFact(_ index: Int, _ f: InfoFact) -> InfoFactInfo? {
        guard let kind = FactKindInfo(rawValue: key(f.kind)) else { return nil }
        return InfoFactInfo(
            id: index,
            kind: kind,
            text: f.text,
            at: Format.date(iso: f.at),
            seconds: f.seconds,
            a: f.a,
            b: f.b,
            limits: f.limits.map { shareLimits($0) }
        )
    }

    static func fileNode(_ n: FileNode) -> FileNodeInfo {
        FileNodeInfo(
            path: n.path,
            name: n.name,
            depth: Int(n.depth),
            folder: n.folder,
            size: n.size,
            progress: n.progress,
            priority: Int(n.priority),
            indexes: n.indexes.map { Int($0.intValue) }
        )
    }

    static func peerRow(_ index: Int, _ p: PeerInfo) -> PeerRowInfo {
        PeerRowInfo(id: index, address: p.address, client: p.client, progress: p.progress, speed: p.speed, upSpeed: p.upSpeed)
    }

    static func trackerRow(_ index: Int, _ t: TrackerInfo) -> TrackerRowInfo {
        TrackerRowInfo(
            id: index,
            url: t.url,
            tier: Int(t.tier),
            status: t.status,
            message: t.message,
            seeds: Int(t.seeds),
            peers: Int(t.peers),
            fixed: t.fixed,
            ref: t.ref
        )
    }

    static func serverRow(_ index: Int, _ r: ServerRow) -> ServerRowInfo {
        ServerRowInfo(id: index, name: r.name, job: r.job, found: r.found)
    }

    static func logEntry(_ index: Int, _ e: LogEntry) -> LogEntryInfo {
        LogEntryInfo(id: index, at: Format.date(iso: e.at), day: e.day.map { dayInfo($0) }, summary: e.summary, warn: e.warn)
    }

    static func history(_ s: HistoryState) -> HistoryInfo {
        let days = s.days.map { historyDay($0) }
        let sources = s.sources.map { DlSourceInfo(id: $0.id, name: $0.name) }
        return HistoryInfo(
            load: load(s.status),
            titleId: s.titleId,
            episodeId: s.episodeId,
            scopeName: s.scopeName,
            filter: historyFilter(s.filter),
            query: s.query,
            days: days,
            total: Int(s.total),
            loaded: Int(s.loaded),
            more: s.more,
            loadingMore: s.loadingMore,
            sources: sources,
            canSources: s.canSources,
            busy: s.busy,
            notice: s.notice.flatMap { HistoryNoticeKind(rawValue: key($0)) },
            noticeId: s.noticeId,
            actionError: error(s.actionError),
            isEmpty: s.isEmpty,
            filtered: s.filtered
        )
    }

    static func historyFilter(_ f: HistoryFilter) -> HistoryFilterInfo {
        let steps = f.steps.compactMap { HistoryStepKind(rawValue: key($0)) }
        return HistoryFilterInfo(
            steps: Set(steps),
            type: HistoryTypeKind(rawValue: key(f.type)) ?? .all,
            who: HistoryWhoKind(rawValue: key(f.who)) ?? .all,
            period: HistoryWhenKind(rawValue: key(f.period)) ?? .all,
            from: f.from,
            to: f.to,
            source: f.source
        )
    }

    static func historyDay(_ d: HistoryDay) -> HistoryDayInfo {
        HistoryDayInfo(
            day: dayInfo(d.day),
            items: d.items.map { historyItem($0) },
            failed: Int(d.failed),
            needsYou: Int(d.needsYou)
        )
    }

    static func historyItem(_ i: HistoryItem) -> HistoryItemInfo {
        let steps = i.steps.compactMap { downloadStep($0) }
        let facts = i.facts.enumerated().compactMap { historyFact($0.offset, $0.element) }
        let line = HistoryLineInfo(
            kind: DlLineKind(rawValue: key(i.line.kind)) ?? .summary,
            text: i.line.text,
            number: Int(i.line.number)
        )
        let who = HistoryWhoInfo(
            kind: WhoKindInfo(rawValue: key(i.who.kind)) ?? .holyArrs,
            name: i.who.name,
            forName: i.who.forName
        )
        return HistoryItemInfo(
            key: i.key,
            verb: HistoryVerbKind(rawValue: key(i.verb)) ?? .other,
            tone: tone(i.verb.tone),
            title: i.title,
            episode: i.episode,
            version: i.version,
            line: line,
            extra: i.extra,
            who: who,
            poster: i.poster,
            square: i.square,
            facts: facts,
            steps: steps,
            redo: i.redo.flatMap { HistoryRedoKind(rawValue: key($0)) },
            releaseId: i.releaseId,
            downloadId: i.downloadId,
            titleId: i.head.titleId,
            at: Format.date(iso: i.head.at),
            failed: i.failed,
            needsYou: i.needsYou,
            libraryLink: HistoryLibraryLinkKind(rawValue: key(i.libraryLink)) ?? .none
        )
    }

    static func downloadStep(_ s: DownloadStep) -> DownloadStepInfo? {
        guard let stepKey = DownloadStepKeyKind(rawValue: key(s.key)), let state = StepStateKind(rawValue: key(s.state)) else {
            return nil
        }
        return DownloadStepInfo(key: stepKey, state: state)
    }

    static func historyFact(_ index: Int, _ f: HistoryFact) -> HistoryFactInfo? {
        guard let kind = HistoryFactKindInfo(rawValue: key(f.kind)) else { return nil }
        return HistoryFactInfo(id: index, kind: kind, text: f.text)
    }

    static func calendar(_ s: CalendarState) -> CalendarInfo {
        let today = s.today
        let days = s.days.map { calendarDay($0, today: today) }
        let cells = s.cells.map { monthCell($0) }
        return CalendarInfo(
            load: load(s.status),
            view: CalendarViewKind(rawValue: key(s.view)) ?? .agenda,
            month: DlMonth(year: Int(s.month.year), month: Int(s.month.month)),
            today: dayInfo(today),
            selected: s.selected.map { dayInfo($0) },
            type: CalendarTypeKind(rawValue: key(s.type)) ?? .all,
            follows: CalendarFollowsKind(rawValue: key(s.follows)) ?? .library,
            people: Int(s.people),
            days: days,
            cells: cells,
            hidden: Int(s.hidden),
            missing: Int(s.missing),
            canSearch: s.canSearch,
            musicOn: s.musicOn,
            canPrevious: s.canPrevious,
            canNext: s.canNext,
            busy: s.busy,
            notice: calendarNotice(s.notice),
            actionError: error(s.actionError)
        )
    }

    static func calendarDay(_ d: CalendarDay, today: Day) -> CalendarDayInfo {
        CalendarDayInfo(day: dayInfo(d.day), groups: d.groups.map { calendarGroup($0, today: today) })
    }

    static func monthCell(_ c: MonthCell) -> MonthCellInfo {
        MonthCellInfo(day: dayInfo(c.day), inMonth: c.inMonth, today: c.today, count: Int(c.count), tone: c.tone.map { tone($0) })
    }

    static func calendarNotice(_ n: CalendarNotice?) -> CalendarNoticeInfo? {
        guard let n, let kind = CalendarNoticeKindInfo(rawValue: key(n.kind)) else { return nil }
        return CalendarNoticeInfo(id: n.id, kind: kind, name: n.name, count: Int(n.count))
    }

    static func calendarGroup(_ g: CalendarGroup, today: Day) -> CalendarGroupInfo {
        let rules = CalendarRules.shared
        let groupDot = EntryDotKind(rawValue: key(g.dot)) ?? .upcoming
        let dots = g.dots.map { EntryDotKind(rawValue: key($0)) ?? .upcoming }
        let entries = g.entries.enumerated().map { index, row -> CalendarEntryInfo in
            let dot = index < dots.count ? dots[index] : groupDot
            return CalendarEntryInfo(
                key: rules.entryKey(e: row),
                season: Int(row.season),
                episode: Int(row.episode),
                episodeName: rules.episodeName(name: row.episodeName),
                dot: dot
            )
        }
        let movieKinds = g.movieKinds.compactMap { MovieDateKindInfo(rawValue: key($0)) }
        return CalendarGroupInfo(
            key: g.key,
            day: dayInfo(g.day),
            kind: g.kind,
            title: g.title,
            tag: EntryTagKind(rawValue: key(g.tag)) ?? .newMovie,
            count: Int(g.count),
            movieKinds: movieKinds,
            whenKind: EntryWhenKind(rawValue: key(g.whenKind)) ?? .allDay,
            minutes: Int(g.minutes),
            span: EpisodeSpanKind(rawValue: key(g.span)) ?? .one,
            season: Int(g.season),
            episode: Int(g.episode),
            season2: Int(g.season2),
            episode2: Int(g.episode2),
            episodeName: g.episodeName,
            network: g.network,
            caption: g.caption,
            album: g.album,
            dot: groupDot,
            art: g.art,
            following: g.following,
            followers: g.followers,
            canSearch: g.canSearch,
            leadTitleId: g.lead.titleId,
            leadName: g.lead.name,
            leadKey: rules.entryKey(e: g.lead),
            entries: entries
        )
    }

    static func downloadBadge(_ b: DownloadsBadge) -> DownloadsBadgeInfo {
        DownloadsBadgeInfo(active: Int(b.active), percent: Int(b.percent), decide: Int(b.decide))
    }
}
