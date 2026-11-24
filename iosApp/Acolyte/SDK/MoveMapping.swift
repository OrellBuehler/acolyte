@preconcurrency import shared
import Foundation

extension Mapping {
    static func moveCount(_ part: MoveCountPart) -> MoveCountInfo {
        MoveCountInfo(kind: MoveCountWord(rawValue: key(part.kind)) ?? .title, n: Int(part.n))
    }

    static func moveSummary(_ part: MoveSummaryPart) -> MoveSummaryInfo {
        MoveSummaryInfo(n: Int(part.n), word: MoveWordKind(rawValue: key(part.word)) ?? .other, raw: part.raw)
    }

    static func moveProblem(_ problem: MoveProblem?) -> MoveProblemInfo? {
        guard let problem else { return nil }
        return MoveProblemInfo(
            error: error(problem.error) ?? .unknown,
            reach: MoveReachKind(rawValue: key(problem.reach)) ?? .other,
            noPassword: problem.noPassword
        )
    }

    static func moveNotice(_ notice: MoveNotice) -> MoveNoticeInfo {
        MoveNoticeInfo(
            seq: Int(notice.seq),
            kind: MoveNoticeType(rawValue: key(notice.kind)) ?? .readFailed,
            name: notice.name,
            n: Int(notice.n),
            moveId: notice.id,
            counts: notice.counts.map { moveCount($0) },
            problem: moveProblem(notice.problem)
        )
    }

    static func moveOutcome(_ outcome: MoveFolderOutcome?) -> MoveOutcomeInfo? {
        guard let outcome else { return nil }
        return MoveOutcomeInfo(
            came: Int(outcome.came),
            attention: Int(outcome.attention),
            attentionFolders: outcome.attentionFolders,
            unchecked: Int(outcome.unchecked),
            uncheckedFolders: outcome.uncheckedFolders
        )
    }

    static func moveGate(_ gate: MoveGate) -> MoveGateInfo {
        MoveGateInfo(ok: gate.ok, reason: MoveGateKind(rawValue: key(gate.reason)) ?? .none, n: Int(gate.n))
    }

    static func moveCheckNote(_ note: MoveCheckNote?) -> MoveCheckNoteInfo? {
        guard let note else { return nil }
        return MoveCheckNoteInfo(
            step: MoveCheckStepKind(rawValue: key(note.step)) ?? .reading,
            done: Int(note.done),
            total: Int(note.total),
            now: note.now,
            issues: Int(note.issues)
        )
    }

    static func moveRead(_ row: MoveReadRow) -> MoveReadInfo {
        MoveReadInfo(
            id: row.id,
            kind: row.kind,
            name: row.name,
            address: row.address,
            host: row.host,
            showHost: row.showHost,
            version: row.version,
            createdAt: Format.date(iso: row.createdAt),
            stats: row.stats.map { moveCount($0) },
            usedBy: row.usedBy,
            picked: row.picked
        )
    }

    static func moveEarlier(_ row: MoveEarlierRow) -> MoveEarlierInfo {
        MoveEarlierInfo(
            id: row.id,
            createdAt: Format.date(iso: row.createdAt),
            counts: row.counts.map { moveSummary($0) },
            word: MoveStateKind(rawValue: key(row.word)) ?? .waiting,
            tone: tone(row.tone),
            canRemove: row.canRemove
        )
    }

    static func moveSheet(_ sheet: MoveSheetState) -> MoveSheetInfo {
        var probe: MoveProbeInfo?
        if let p = sheet.probe {
            probe = MoveProbeInfo(ok: p.ok, app: p.app, version: p.version, problem: moveProblem(p.problem))
        }
        return MoveSheetInfo(
            probing: sheet.probing,
            probe: probe,
            detected: sheet.detected,
            finding: sheet.finding,
            found: sheet.found.map { MoveFoundInfo(kind: $0.kind, name: $0.name, address: $0.address, version: $0.version) },
            searched: sheet.searched,
            findRan: sheet.findRan,
            findProblem: moveProblem(sheet.findProblem),
            linked: sheet.linked.map {
                MoveLinkedInfo(
                    kind: $0.kind,
                    name: $0.name,
                    address: $0.address,
                    hasKey: $0.hasKey,
                    reachable: $0.reachable,
                    version: $0.version,
                    offered: $0.offered
                )
            },
            linkedFrom: sheet.linkedFrom,
            addingLinked: sheet.addingLinked,
            reading: sheet.reading,
            readProblem: moveProblem(sheet.readProblem),
            stopped: sheet.stopped
        )
    }

    static func moveFolder(_ row: MoveFolderRow) -> MoveFolderInfo {
        MoveFolderInfo(
            oldPath: row.oldPath,
            kind: row.kind,
            unseen: row.unseen,
            options: row.options,
            suggested: row.suggested,
            pick: MovePickChoiceKind(rawValue: key(row.pick)) ?? .none,
            path: row.path,
            other: row.other,
            problem: MovePathProblemKind(rawValue: key(row.problem)) ?? .none,
            gone: row.gone,
            twins: row.twins,
            canAddRule: row.canAddRule
        )
    }

    static func moveGroup(_ row: MoveGroupRow) -> MoveGroupInfo {
        MoveGroupInfo(
            id: row.id,
            group: MoveGroupKind(rawValue: key(row.group)) ?? .unknown,
            label: row.label,
            count: Int(row.count),
            on: row.on,
            canSkip: row.canSkip,
            note: MoveNoteKind(rawValue: key(row.note)) ?? .none,
            noteCount: Int(row.noteCount),
            showFormats: row.showFormats
        )
    }

    static func moveChoose(_ c: MoveChooseState) -> MoveChooseInfo {
        MoveChooseInfo(
            planning: c.planning,
            planned: c.planned,
            note: moveCheckNote(c.note),
            planProblem: moveProblem(c.planProblem),
            empty: c.empty,
            tiles: c.tiles.map { MoveTileInfo(id: $0.id, group: MoveGroupKind(rawValue: key($0.group)) ?? .unknown, label: $0.label, n: Int($0.n)) },
            alsoGroups: c.alsoGroups.map { MoveGroupKind(rawValue: key($0)) ?? .unknown },
            stillChecking: Int(c.stillChecking),
            needFolders: c.needFolders.map { moveFolder($0) },
            folders: c.folders.map { moveFolder($0) },
            people: c.people.map { MovePersonInfo(key: $0.key, name: $0.name, app: $0.app, owner: $0.owner, on: $0.on) },
            namesAsk: c.namesAsk,
            names: MoveNamesKind(rawValue: key(c.names)) ?? .keep,
            formatsAsk: c.formatsAsk,
            noFormats: c.noFormats,
            sourceKeys: Int(c.sourceKeys),
            downloaderKeys: Int(c.downloaderKeys),
            invitees: Int(c.invitees),
            groups: c.groups.map { moveGroup($0) },
            emptyGroups: c.emptyGroups,
            rules: c.rules.map { MoveRuleInfo(from: $0.from, to: $0.to, problem: MovePathProblemKind(rawValue: key($0.problem)) ?? .none) },
            showRules: c.showRules,
            notMovedCount: Int(c.notMovedCount),
            notMovedApps: c.notMovedApps.map { MoveNotMovedAppInfo(app: $0.app, name: $0.name, n: Int($0.n)) },
            trying: c.trying,
            tried: c.tried,
            stale: c.stale,
            tryFailed: c.tryFailed,
            tryProblem: moveProblem(c.tryProblem),
            tryNotice: c.tryNotice,
            problems: Int(c.problems),
            openChoices: Int(c.openChoices),
            total: Int(c.total),
            choices: c.choices.map { moveChoice($0) },
            before: c.before.map {
                MoveBeforeInfo(
                    id: $0.id,
                    group: MoveGroupKind(rawValue: key($0.group)) ?? .unknown,
                    label: $0.label,
                    n: Int($0.n),
                    word: MoveWordKind(rawValue: key($0.word)) ?? .other
                )
            },
            outcome: moveOutcome(c.outcome),
            apps: c.apps,
            toCheck: Int(c.toCheck),
            checkingNow: c.checkingNow
        )
    }

    static func moveChoice(_ row: MoveChoiceRow) -> MoveChoiceInfo {
        var choice: MovePersonChoiceKind?
        if let value = row.choice { choice = MovePersonChoiceKind(rawValue: key(value)) }
        return MoveChoiceInfo(
            key: row.key,
            name: row.name,
            why: MoveWhyKind(rawValue: key(row.why)) ?? .sameName,
            choice: choice
        )
    }

    static func moveRun(_ r: MoveRunState) -> MoveRunInfo {
        MoveRunInfo(
            id: r.id,
            createdAt: Format.date(iso: r.createdAt),
            word: MoveStateKind(rawValue: key(r.word)) ?? .waiting,
            tone: tone(r.tone),
            running: r.running,
            undoing: r.undoing,
            undone: r.undone,
            failed: r.failed,
            undoFailed: r.undoFailed,
            progress: Double(r.progress),
            note: MoveProgressInfo(
                done: Int(r.note.done),
                total: Int(r.note.total),
                waitLeft: Int(r.note.waitLeft),
                waitMinutes: Int(r.note.waitMinutes),
                raw: r.note.raw
            ),
            lost: r.lost,
            counts: r.counts.map { moveSummary($0) },
            failedCount: Int(r.failedCount),
            hasReport: r.hasReport,
            look: MoveLookKind(rawValue: key(r.look)) ?? .ready,
            next: r.next,
            plainNext: r.plainNext,
            noFolder: Int(r.noFolder),
            renameNext: r.renameNext,
            openChoices: Int(r.openChoices),
            seasons: r.seasons.map { MoveSeasonInfo(name: $0.name, titleId: $0.titleId) },
            madeAlerts: r.madeAlerts,
            outcome: moveOutcome(r.outcome),
            groups: r.groups.map {
                MoveResultGroupInfo(
                    id: $0.id,
                    group: MoveGroupKind(rawValue: key($0.group)) ?? .unknown,
                    summary: $0.summary.map { moveSummary($0) }
                )
            },
            notMoved: Int(r.notMoved),
            keysAgain: Int(r.keysAgain),
            notes: Int(r.notes),
            canUndo: r.canUndo,
            needsYou: r.needsYou
        )
    }

    static func move(_ s: SettingsMoveState) -> MoveInfo {
        MoveInfo(
            load: load(s.status),
            allowed: s.allowed,
            owner: s.owner,
            safeMode: s.safeMode,
            step: MoveStepKind(rawValue: key(s.step)) ?? .read,
            reads: s.reads.map { moveRead($0) },
            pickedCount: Int(s.pickedCount),
            savedKeys: s.savedKeys.map { MoveSavedKeyInfo(kind: $0.kind, address: $0.address) },
            moves: s.moves.map { moveEarlier($0) },
            movesFailed: s.movesProblem != nil,
            brought: s.brought.map { MoveBroughtInfo(id: $0.id, app: $0.app, name: $0.name, kind: $0.kind, needs: $0.needs) },
            sheet: moveSheet(s.sheet),
            choose: moveChoose(s.choose),
            run: s.run.map { moveRun($0) },
            gate: moveGate(s.gate),
            dialog: MoveDialogKind(rawValue: key(s.dialog)) ?? .none,
            starting: s.starting,
            problem: moveProblem(s.problem),
            gone: s.gone,
            notices: s.notices.map { moveNotice($0) },
            needsPassword: s.needsPassword,
            passwordBusy: s.passwordBusy,
            passwordError: error(s.passwordError),
            readingKind: s.readingKind,
            canStart: s.canStart
        )
    }

    static func moveItem(_ row: MoveItemRow) -> MoveItemInfo {
        var word: MoveWordKind?
        if let value = row.word { word = MoveWordKind(rawValue: key(value)) }
        return MoveItemInfo(
            key: row.key,
            name: row.name,
            hint: row.hint,
            word: word,
            raw: row.raw,
            tone: MoveToneKind(rawValue: key(row.tone)) ?? .quiet,
            look: row.look,
            traits: row.traits,
            skipped: row.skipped,
            needs: row.needs,
            badge: MoveBadgeKind(rawValue: key(row.badge)) ?? .none,
            group: MoveGroupKind(rawValue: key(row.group)) ?? .unknown,
            section: row.section,
            n: Int(row.n)
        )
    }

    static func kotlinMoveList(_ source: MoveListSource) -> MoveListKind {
        switch source {
        case .planGroup: MoveListKind.planGroup
        case .reportGroup: MoveListKind.reportGroup
        case .beforeGroup: MoveListKind.beforeGroup
        case .notMoved: MoveListKind.notMoved
        case .notMovedApp: MoveListKind.notMovedApp
        case .keysAgain: MoveListKind.keysAgain
        case .notes: MoveListKind.notes
        case .invitees: MoveListKind.invitees
        case .sourceKeys: MoveListKind.sourceKeys
        case .downloaderKeys: MoveListKind.downloaderKeys
        }
    }

    static func kotlinMovePick(_ kind: MovePickChoiceKind) -> MovePickKind {
        switch kind {
        case .none: MovePickKind.none
        case .leave: MovePickKind.leave
        case .other: MovePickKind.other
        case .path: MovePickKind.path
        }
    }

    static func kotlinMoveChoice(_ choice: MovePersonChoiceKind?) -> MovePersonChoice? {
        switch choice {
        case .owner: MovePersonChoice.owner
        case .admin: MovePersonChoice.admin
        case .merge: MovePersonChoice.merge
        case .skip: MovePersonChoice.skip
        case nil: nil
        }
    }

    static func kotlinMoveStep(_ step: MoveStepKind) -> SettingsMoveStep {
        switch step {
        case .read: SettingsMoveStep.read
        case .choose: SettingsMoveStep.choose
        case .run: SettingsMoveStep.run
        case .done: SettingsMoveStep.done
        }
    }

    static func kotlinMoveNames(_ names: MoveNamesKind) -> MoveNamesChoice {
        switch names {
        case .keep: MoveNamesChoice.keep
        case .rename: MoveNamesChoice.rename
        }
    }

    static func kotlinMoveForm(_ form: MoveFormInfo) -> MoveReadForm {
        MoveReadForm(
            kind: form.kind,
            address: form.address,
            key: form.key,
            username: form.username,
            password: form.password,
            useSavedKey: form.useSavedKey,
            allowLocalNetwork: true
        )
    }
}
