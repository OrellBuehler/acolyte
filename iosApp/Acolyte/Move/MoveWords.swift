import Foundation

enum MoveWords {
    static func word(_ word: MoveWordKind, raw: String) -> String {
        switch word {
        case .willBeAdded: L10n.tr("acq.mv.w.will.be.added")
        case .added: L10n.tr("acq.mv.w.added")
        case .willUseTheOne: L10n.tr("acq.mv.w.will.use")
        case .usingTheOne: L10n.tr("acq.mv.w.using")
        case .willBeUpdated: L10n.tr("acq.mv.w.will.update")
        case .updated: L10n.tr("acq.mv.w.updated")
        case .matchesOneHere: L10n.tr("acq.mv.w.matches")
        case .matched: L10n.tr("acq.mv.w.matched")
        case .alreadyHere: L10n.tr("acq.mv.w.already")
        case .leftOut: L10n.tr("acq.mv.w.left.out")
        case .noFolderHere: L10n.tr("acq.mv.w.no.folder")
        case .wontWork: L10n.tr("acq.mv.w.wont.work")
        case .failed: L10n.tr("acq.mv.w.failed")
        case .removed: L10n.tr("acq.mv.w.removed")
        case .kept: L10n.tr("acq.mv.w.kept")
        case .notRemoved: L10n.tr("acq.mv.w.not.removed")
        case .other: raw
        }
    }

    static func summaryWord(_ word: MoveWordKind, raw: String) -> String {
        switch word {
        case .willBeAdded: L10n.tr("acq.mv.s.will.be.added")
        case .added: L10n.tr("acq.mv.s.added")
        case .willUseTheOne: L10n.tr("acq.mv.s.will.use")
        case .usingTheOne: L10n.tr("acq.mv.s.using")
        case .willBeUpdated: L10n.tr("acq.mv.s.will.update")
        case .updated: L10n.tr("acq.mv.s.updated")
        case .matchesOneHere: L10n.tr("acq.mv.s.matches")
        case .matched: L10n.tr("acq.mv.s.matched")
        case .alreadyHere: L10n.tr("acq.mv.s.already")
        case .leftOut: L10n.tr("acq.mv.s.left.out")
        case .noFolderHere: L10n.tr("acq.mv.s.no.folder")
        case .wontWork: L10n.tr("acq.mv.s.wont.work")
        case .failed: L10n.tr("acq.mv.s.failed")
        case .removed: L10n.tr("acq.mv.s.removed")
        case .kept: L10n.tr("acq.mv.s.kept")
        case .notRemoved: L10n.tr("acq.mv.s.not.removed")
        case .other: raw
        }
    }

    static func summary(_ parts: [MoveSummaryInfo]) -> String {
        parts.map { "\($0.n) \(summaryWord($0.word, raw: $0.raw))" }.joined(separator: " · ")
    }

    static func count(_ part: MoveCountInfo) -> String {
        let key: String
        switch part.kind {
        case .movie: key = "acq.mv.c.movie"
        case .show: key = "acq.mv.c.show"
        case .artist: key = "acq.mv.c.artist"
        case .title: key = "acq.mv.c.title"
        case .folder: key = "acq.mv.c.folder"
        case .pickRules: key = "acq.mv.c.pick.rules"
        case .trait: key = "acq.mv.c.trait"
        case .waitRule: key = "acq.mv.c.wait.rule"
        case .source: key = "acq.mv.c.source"
        case .downloader: key = "acq.mv.c.downloader"
        case .outsideList: key = "acq.mv.c.outside.list"
        case .alert: key = "acq.mv.c.alert"
        case .linkedApp: key = "acq.mv.c.linked.app"
        case .person: key = "acq.mv.c.person"
        case .request: key = "acq.mv.c.request"
        case .tag: key = "acq.mv.c.tag"
        case .subtitleLanguage: key = "acq.mv.c.subtitle.language"
        case .subtitleSet: key = "acq.mv.c.subtitle.set"
        case .subtitleSite: key = "acq.mv.c.subtitle.site"
        case .keyToRetype: key = "acq.mv.c.key.again"
        }
        return L10n.plural(key, part.n)
    }

    static func counts(_ parts: [MoveCountInfo]) -> String {
        parts.map { count($0) }.joined(separator: " · ")
    }

    static func group(_ group: MoveGroupKind, raw: String) -> String {
        switch group {
        case .folders: L10n.tr("acq.mv.g.folders")
        case .pickRules: L10n.tr("acq.mv.g.pick.rules")
        case .sources: L10n.tr("acq.mv.g.sources")
        case .titles: L10n.tr("acq.mv.g.titles")
        case .neverAdd: L10n.tr("acq.mv.g.never.add")
        case .outsideLists: L10n.tr("acq.mv.g.outside.lists")
        case .downloaders: L10n.tr("acq.mv.g.downloaders")
        case .turnedDown: L10n.tr("acq.mv.g.turned.down")
        case .naming: L10n.tr("acq.mv.g.naming")
        case .subtitles: L10n.tr("acq.mv.g.subtitles")
        case .people: L10n.tr("acq.mv.g.people")
        case .wishlist: L10n.tr("acq.mv.g.wishlist")
        case .alerts: L10n.tr("acq.mv.g.alerts")
        case .other: L10n.tr("acq.mv.g.other")
        case .unknown: raw
        }
    }

    static func also(_ group: MoveGroupKind) -> String {
        switch group {
        case .folders: L10n.tr("acq.mv.also.folders")
        case .pickRules: L10n.tr("acq.mv.also.pick.rules")
        case .sources: L10n.tr("acq.mv.also.sources")
        case .neverAdd: L10n.tr("acq.mv.also.never.add")
        case .outsideLists: L10n.tr("acq.mv.also.outside.lists")
        case .downloaders: L10n.tr("acq.mv.also.downloaders")
        case .turnedDown: L10n.tr("acq.mv.also.turned.down")
        case .naming: L10n.tr("acq.mv.also.naming")
        case .subtitles: L10n.tr("acq.mv.also.subtitles")
        case .alerts: L10n.tr("acq.mv.also.alerts")
        case .titles, .people, .wishlist, .other, .unknown: L10n.tr("acq.mv.also.other")
        }
    }

    static func andList(_ items: [String]) -> String {
        guard let last = items.last else { return "" }
        if items.count == 1 { return last }
        return L10n.format("acq.mv.and.list", items.dropLast().joined(separator: ", "), last)
    }

    static func state(_ word: MoveStateKind) -> String {
        switch word {
        case .waiting: L10n.tr("acq.mv.state.waiting")
        case .importing: L10n.tr("acq.mv.state.importing")
        case .imported: L10n.tr("acq.mv.state.imported")
        case .stopped: L10n.tr("acq.mv.state.stopped")
        case .undone: L10n.tr("acq.mv.state.undone")
        case .undoing: L10n.tr("acq.mv.state.undoing")
        case .undoFailed: L10n.tr("acq.mv.state.undo.failed")
        }
    }

    static func tone(_ tone: MoveToneKind) -> ToneKind {
        switch tone {
        case .ok: .ok
        case .warn: .wait
        case .bad: .fail
        case .quiet: .idle
        }
    }

    static func check(_ note: MoveCheckNoteInfo) -> String {
        if note.counting {
            var text = L10n.format("acq.mv.check.of", Int32(note.done), Int32(note.total))
            if !note.now.isEmpty { text += " · \(note.now)" }
            if note.issues > 0 { text += " · " + L10n.format("acq.mv.check.issues", Int32(note.issues)) }
            return text
        }
        switch note.step {
        case .titles: return L10n.tr("acq.mv.check.titles")
        case .folders: return L10n.tr("acq.mv.check.folders")
        case .reading: return L10n.tr("acq.mv.check.reading")
        }
    }

    static func gate(_ gate: MoveGateInfo, note: MoveCheckNoteInfo?) -> String {
        switch gate.reason {
        case .none: ""
        case .turnOnRead: L10n.tr("acq.mv.gate.turn.on.read")
        case .updating: L10n.tr("acq.mv.gate.updating")
        case .progress: note.map { check($0) } ?? L10n.tr("acq.mv.gate.updating")
        case .fixFolder: L10n.tr("acq.mv.gate.fix.folder")
        case .fixRule: L10n.tr("acq.mv.gate.fix.rule")
        case .nothingNew: L10n.tr("acq.mv.gate.nothing.new")
        case .turnOnGroup: L10n.tr("acq.mv.gate.turn.on.group")
        case .safeMode: L10n.tr("acq.mv.gate.safe.mode")
        case .testFailed: L10n.tr("acq.mv.gate.test.failed")
        case .changedSinceTest: L10n.tr("acq.mv.gate.changed")
        case .trying: L10n.tr("acq.mv.gate.trying")
        case .oneNeedsChoice: L10n.tr("acq.mv.gate.one.choice")
        case .manyNeedChoice: L10n.format("acq.mv.gate.many.choice", Int32(gate.n))
        }
    }

    static func pathProblem(_ problem: MovePathProblemKind) -> String {
        switch problem {
        case .none: ""
        case .fillBoth: L10n.tr("acq.mv.p.fill.both")
        case .fullPath: L10n.tr("acq.mv.p.full.path")
        case .dots: L10n.tr("acq.mv.p.dots")
        case .typePath: L10n.tr("acq.mv.p.type.path")
        }
    }

    static func problem(_ problem: MoveProblemInfo) -> String {
        if problem.noPassword { return L10n.tr("acq.mv.no.password") }
        if problem.error.kind == .conflict && problem.error.message.lowercased().contains("safe mode") {
            return L10n.tr("acq.mv.safe.mode")
        }
        switch problem.reach {
        case .tryHttp: return L10n.tr("acq.mv.reach.try.http")
        case .cantReach: return L10n.tr("acq.mv.reach.cant")
        case .timeout: return L10n.tr("acq.mv.reach.timeout")
        case .tooBig: return L10n.tr("acq.mv.reach.too.big")
        case .refusedKey: return L10n.tr("acq.mv.reach.refused")
        case .wrongApp: return L10n.tr("acq.mv.reach.wrong.app")
        case .unsupported: return L10n.tr("acq.mv.reach.unsupported")
        case .notOwner: return L10n.tr("acq.mv.reach.owner")
        case .safeMode: return L10n.tr("acq.mv.safe.mode")
        case .other:
            if problem.error.kind == .badRequest && !problem.error.message.isEmpty { return problem.error.message }
            return problem.error.localized
        }
    }

    static func notice(_ notice: MoveNoticeInfo) -> String {
        switch notice.kind {
        case .readDone:
            var text = L10n.format("acq.mv.read.done", notice.name)
            if notice.n > 0 {
                text += " · " + L10n.plural("acq.mv.read.keys", notice.n)
            } else if !notice.counts.isEmpty {
                text += " · " + counts(notice.counts)
            }
            return text
        case .readFailed, .linkedFailed:
            var text = L10n.format("acq.mv.read.failed", notice.name)
            if let problem = notice.problem { text += " · " + MoveWords.problem(problem) }
            return text
        case .readStopped: return L10n.tr("acq.mv.stopped")
        case .importFinished: return L10n.format("acq.mv.n.import.finished", Int32(truncatingIfNeeded: notice.moveId))
        case .importFailed: return L10n.format("acq.mv.n.import.failed", Int32(truncatingIfNeeded: notice.moveId)) + " · " + L10n.tr("acq.mv.n.see.report")
        case .importUndone: return L10n.format("acq.mv.n.import.undone", Int32(truncatingIfNeeded: notice.moveId))
        case .undoFailed: return L10n.tr("acq.mv.n.undo.failed")
        }
    }

    static func date(_ date: Date?) -> String {
        guard let date else { return "" }
        return date.formatted(date: .abbreviated, time: .shortened)
    }

    static func shortDate(_ date: Date?) -> String {
        guard let date else { return "" }
        return date.formatted(date: .abbreviated, time: .omitted)
    }
}
