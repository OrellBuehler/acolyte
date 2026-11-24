import SwiftUI

struct MoveRunCapsule: View {
    let run: MoveRunInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if run.running && run.lost {
                HStack {
                    StatusChip(text: L10n.tr("acq.mv.cant.reach.word"), tone: .wait)
                    Text(L10n.tr("acq.mv.trying.again")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                Text(L10n.tr("acq.mv.cant.reach")).font(.footnote).foregroundStyle(Theme.textMuted)
            } else if run.running {
                StatusChip(text: MoveWords.state(run.undoing ? .undoing : .importing), tone: .info)
                ProgressView(value: run.progress)
                if run.note.isCount {
                    Text(L10n.format("acq.mv.progress.of", Int32(run.note.done), Int32(run.note.total))).font(.footnote)
                    if run.note.waitLeft > 0 {
                        Text(L10n.format("acq.mv.busy.wait", Int32(run.note.waitLeft), Int32(run.note.waitMinutes)))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                } else if !run.note.raw.isEmpty {
                    Text(run.note.raw).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            } else {
                HStack {
                    StatusChip(text: MoveWords.state(run.word), tone: tone)
                    Text(detail).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            Text(L10n.format("acq.mv.done.when", Int32(truncatingIfNeeded: run.id), MoveWords.date(run.createdAt)))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private var tone: ToneKind {
        if run.undoFailed || run.failed { return .fail }
        if run.undone { return .idle }
        switch run.look {
        case .failed: return .fail
        case .pending: return .wait
        case .ready: return .ok
        }
    }

    private var detail: String {
        if run.undoFailed { return L10n.plural("acq.mv.items", run.failedCount) }
        if run.needsYou && !run.undone && !run.failed && run.look != .ready { return L10n.tr("acq.mv.needs.you") }
        return MoveWords.summary(run.counts)
    }
}

struct MoveRunStepView: View {
    let info: MoveInfo

    var body: some View {
        List {
            Section {
                if let run = info.run { MoveRunCapsule(run: run) }
                Text(L10n.tr("acq.mv.leave.page")).font(.footnote).foregroundStyle(Theme.textMuted)
            } header: {
                Text(L10n.tr("acq.mv.step3.title"))
            }
        }
    }
}

struct MoveOutcomeView: View {
    let outcome: MoveOutcomeInfo
    let after: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("acq.mv.outcome.title")).font(.subheadline.weight(.semibold))
            Text(after ? L10n.format("acq.mv.came.over", Int32(outcome.came)) : L10n.format("acq.mv.will.come", Int32(outcome.came)))
            if outcome.attention > 0 {
                Text(L10n.format("acq.mv.need.attention", Int32(outcome.attention)))
                Text(L10n.format("acq.mv.need.attention.why", outcome.attentionFolders.joined(separator: ", ")))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if outcome.unchecked > 0 {
                Text(L10n.format("acq.mv.left.out.unchecked", Int32(outcome.unchecked)))
                ForEach(outcome.uncheckedFolders, id: \.self) { folder in
                    Text(L10n.format("acq.mv.left.out.unchecked.why", folder))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }
}

struct MoveDoneStepView: View {
    let store: MoveStore
    let onShow: (MoveListRequest) -> Void
    let onOpen: (Route) -> Void

    private var info: MoveInfo { store.info }

    var body: some View {
        List {
            if let run = info.run {
                Section {
                    MoveRunCapsule(run: run)
                } header: {
                    Text(L10n.tr("acq.mv.step4.title"))
                }
                if run.hasReport && !run.running {
                    if run.needsYou { todoSection(run) }
                    if !run.groups.isEmpty || run.notMoved > 0 || run.keysAgain > 0 || run.notes > 0 { resultsSection(run) }
                }
            }
        }
        .refreshable { await store.refresh() }
    }

    private func todoRow(_ title: String, hint: String, action: String?, perform: @escaping () -> Void) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                if !hint.isEmpty {
                    Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            Spacer()
            if let action {
                Button(action, action: perform).buttonStyle(.borderless)
            }
        }
    }

    private func todoSection(_ run: MoveRunInfo) -> some View {
        Section {
            if run.plainNext {
                ForEach(run.next, id: \.self) { line in Text(line) }
            } else {
                if run.noFolder > 0 {
                    todoRow(
                        L10n.plural("acq.mv.no.folder.n", run.noFolder),
                        hint: L10n.tr("acq.mv.add.folder.then"),
                        action: L10n.tr("acq.mv.folders")
                    ) { onOpen(.settingsLibraryFolders) }
                }
                if run.renameNext {
                    todoRow(
                        L10n.tr("acq.mv.rename.next"),
                        hint: L10n.tr("acq.mv.rename.next.hint"),
                        action: L10n.tr("acq.mv.check.renames")
                    ) { onOpen(.libraryRename) }
                }
                if run.openChoices > 0 {
                    todoRow(
                        L10n.plural("acq.mv.choose.people", run.openChoices),
                        hint: L10n.tr("acq.mv.they.stay"),
                        action: L10n.tr("acq.mv.choose")
                    ) { store.chooseAgain() }
                }
                if !run.seasons.isEmpty {
                    todoRow(
                        L10n.plural("acq.mv.note.check", run.seasons.count),
                        hint: L10n.format("acq.mv.seasons.why", run.seasons.map { $0.name }.joined(separator: ", ")),
                        action: nil
                    ) {}
                    ForEach(run.seasons.filter { $0.titleId > 0 }, id: \.titleId) { season in
                        todoRow(season.name, hint: "", action: L10n.tr("acq.mv.open")) {
                            onOpen(.libraryTitle(id: season.titleId, kind: "", tmdb: ""))
                        }
                    }
                }
                if run.failedCount > 0 {
                    todoRow(
                        L10n.plural("acq.mv.failed.n", run.failedCount),
                        hint: L10n.tr("acq.mv.failed.hint"),
                        action: L10n.tr("acq.mv.match.by.hand")
                    ) { onOpen(.libraryImport) }
                }
            }
        } header: {
            Text(L10n.tr("acq.mv.needs.you"))
        }
    }

    private func resultsSection(_ run: MoveRunInfo) -> some View {
        Section {
            if let outcome = run.outcome { MoveOutcomeView(outcome: outcome, after: true) }
            ForEach(run.groups) { group in
                let label = MoveWords.group(group.group, raw: group.id)
                todoRow(label, hint: MoveWords.summary(group.summary), action: L10n.tr("acq.mv.show")) {
                    onShow(MoveListRequest(source: .reportGroup, listId: group.id, title: label))
                }
            }
            if run.notMoved > 0 {
                todoRow(
                    L10n.tr("acq.mv.not.imported"),
                    hint: L10n.plural("acq.mv.not.imported.n", run.notMoved),
                    action: L10n.tr("acq.mv.show")
                ) { onShow(MoveListRequest(source: .notMoved, listId: "", title: L10n.tr("acq.mv.not.imported"))) }
            }
            if run.keysAgain > 0 {
                todoRow(L10n.tr("acq.mv.keys.again"), hint: "\(run.keysAgain)", action: L10n.tr("acq.mv.show")) {
                    onShow(MoveListRequest(source: .keysAgain, listId: "", title: L10n.tr("acq.mv.keys.again")))
                }
            }
            if run.notes > 0 {
                todoRow(L10n.tr("acq.mv.notes"), hint: "\(run.notes)", action: L10n.tr("acq.mv.show")) {
                    onShow(MoveListRequest(source: .notes, listId: "", title: L10n.tr("acq.mv.notes")))
                }
            }
        } header: {
            Text(L10n.tr("acq.mv.results"))
        }
    }
}
