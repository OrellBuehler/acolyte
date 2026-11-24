import SwiftUI

struct SettingsPickRulesView: View {
    let account: AccountInfo
    @State private var store = QualityListStore()
    @State private var appeared = false
    @Environment(\.openRoute) private var openRoute

    private var info: QualityListInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    list
                }
            }
        }
        .navigationTitle(L10n.tr("acq.page.quality"))
        .toolbar {
            if info.allowed, info.mayEdit, info.load.phase == .ready {
                ToolbarItem(placement: .primaryAction) {
                    Button(L10n.tr("acq.q.l.add"), systemImage: "plus") { addSet() }
                }
            }
        }
        .qualityNotice(info.notice) { store.dismissNotice($0) }
        .sheet(isPresented: Binding(get: { info.remove != nil }, set: { if !$0 { store.cancelRemove() } })) {
            removeSheet
        }
        .sheet(isPresented: Binding(get: { info.traitSheet != nil }, set: { if !$0 { store.closeTrait() } })) {
            QualityTraitSheet(store: store)
        }
        .sheet(isPresented: Binding(get: { info.traitRemove != nil }, set: { if !$0 { store.cancelRemoveTrait() } })) {
            removeTraitSheet
        }
        .task { await store.watch() }
        .task {
            if appeared {
                await store.refresh()
            } else {
                appeared = true
                await store.load()
            }
        }
    }

    private func addSet() {
        openRoute(.settingsPickRuleEdit(id: 0, from: 0))
    }

    private var list: some View {
        List {
            setsSection
            traitsSection
            checkSection
            whereSection
        }
        .scrollContentBackground(.hidden)
        .background(Theme.background)
        .refreshable { await store.refresh() }
    }

    private var setsSection: some View {
        Section {
            if info.rules.isEmpty {
                QualityEmptyBlock(
                    title: L10n.tr("acq.q.l.none"),
                    text: L10n.tr("acq.q.l.none.text"),
                    actionTitle: info.mayEdit ? L10n.tr("acq.q.l.add") : nil
                ) { addSet() }
            } else {
                ForEach(info.rules) { row in
                    QualityRuleRowView(
                        row: row,
                        mayEdit: info.mayEdit,
                        show: { openRoute(.settingsPickRuleEdit(id: row.id, from: 0)) },
                        copy: { openRoute(.settingsPickRuleEdit(id: 0, from: row.id)) },
                        remove: { store.askRemove(row.id) }
                    )
                    .swipeActions(edge: .trailing) {
                        if info.mayEdit, !row.preset {
                            Button(role: .destructive) {
                                store.askRemove(row.id)
                            } label: {
                                Label(L10n.tr("acq.q.l.remove"), systemImage: "trash")
                            }
                        }
                    }
                }
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.l.sets"), subtitle: L10n.tr("acq.q.l.sets.sub"))
        } footer: {
            if info.mayEdit, !info.rules.isEmpty {
                Text(L10n.tr("acq.q.l.hint"))
            }
        }
    }

    private var traitsSection: some View {
        Section {
            if info.traitsError != nil, !info.traitsLoaded {
                QualityErrorLine(error: info.traitsError)
                Button(L10n.tr("common.retry")) { Task { await store.refresh() } }
            } else if !info.traitsLoaded {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel(L10n.tr("common.loading"))
            } else {
                handMadeRows
                if !info.trashTraits.isEmpty {
                    QualityTraitGroupView(
                        title: L10n.format("acq.q.ot.from.trash", info.trashTraits.count),
                        note: L10n.tr("acq.q.ot.sync.again"),
                        rows: info.trashTraits,
                        mayEdit: info.mayEdit
                    ) { store.askRemoveTrait($0) }
                }
                if !info.importedTraits.isEmpty {
                    QualityTraitGroupView(
                        title: L10n.format("acq.q.ot.from.old", info.importedTraits.count),
                        note: L10n.tr("acq.q.ot.import.again"),
                        rows: info.importedTraits,
                        mayEdit: info.mayEdit
                    ) { store.askRemoveTrait($0) }
                }
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.ot.title"), subtitle: L10n.tr("acq.q.ot.desc"))
        }
    }

    @ViewBuilder
    private var handMadeRows: some View {
        if info.traitsEmpty {
            QualityEmptyBlock(
                title: L10n.tr("acq.q.ot.none"),
                text: L10n.tr("acq.q.ot.none.text"),
                actionTitle: info.mayEdit ? L10n.tr("acq.q.ot.make") : nil
            ) { store.openTrait(0) }
        } else {
            ForEach(info.handMade) { row in
                QualityTraitRowView(
                    row: row,
                    mayEdit: info.mayEdit,
                    change: { store.openTrait(row.id) },
                    remove: { store.askRemoveTrait(row.id) }
                )
            }
            if info.mayEdit {
                Button(L10n.tr("acq.q.ot.make"), systemImage: "plus") { store.openTrait(0) }
            }
        }
    }

    private var checkSection: some View {
        Section {
            QualityField(
                title: L10n.tr("acq.q.nc.name"),
                text: info.check.name,
                prompt: L10n.tr("acq.q.nc.placeholder"),
                kind: .text(1000)
            ) { store.setCheckName($0) }
            Picker(
                L10n.tr("acq.q.nc.rules"),
                selection: Binding(get: { info.check.rulesId }, set: { store.setCheckRules($0) })
            ) {
                Text(L10n.tr("acq.q.nc.copies")).tag(Int64(0))
                ForEach(info.rules) { rule in
                    Text(rule.name).tag(rule.id)
                }
            }
            QualityField(title: L10n.tr("acq.q.nc.size"), text: info.check.size, kind: .decimal) { store.setCheckSize($0) }
            Button {
                store.runCheck()
            } label: {
                if info.check.busy {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Text(L10n.tr("acq.q.nc.check"))
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(!info.check.canCheck)
            if let error = info.check.error {
                Text(L10n.tr("acq.q.nc.failed") + ": " + QualityWords.errorText(error))
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
            }
            if let result = info.check.result {
                QualityCheckResultView(result: result)
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.nc.title"), subtitle: L10n.tr("acq.q.nc.desc"))
        }
    }

    private var whereSection: some View {
        Section {
            QualityWhereRow(
                title: L10n.tr("acq.q.l.where.folders"),
                text: L10n.tr("acq.q.l.where.folders.text")
            ) { openRoute(.settingsLibraryFolders) }
            QualityWhereRow(
                title: L10n.tr("acq.q.l.where.requests"),
                text: L10n.tr("acq.q.l.where.requests.text")
            ) { openRoute(.settingsRequests) }
            QualityWhereRow(
                title: L10n.tr("acq.q.l.where.lists"),
                text: L10n.tr("acq.q.l.where.lists.text")
            ) { openRoute(.settingsOutsideLists) }
            QualityWhereRow(
                title: L10n.tr("acq.q.l.where.defaults"),
                text: L10n.tr("acq.q.l.where.defaults.text"),
                action: nil
            )
            QualityWhereRow(title: L10n.tr("acq.q.l.where.title"), text: "", action: nil)
        } header: {
            QualityHeader(title: L10n.tr("acq.q.l.where"))
        } footer: {
            if !info.mayEdit {
                Text(L10n.tr("acq.q.l.read.only"))
            }
        }
    }

    @ViewBuilder
    private var removeSheet: some View {
        if let remove = info.remove {
            QualityConfirmSheet(
                title: L10n.tr("acq.q.l.remove.title"),
                name: remove.name,
                notes: removeNotes(remove),
                error: removeError(remove),
                cancelTitle: L10n.tr("acq.q.l.cancel"),
                confirmTitle: L10n.tr("acq.q.l.remove"),
                busy: remove.busy,
                cancel: { store.cancelRemove() },
                confirm: { store.confirmRemove() }
            )
        }
    }

    private func removeNotes(_ remove: QualityRemoveInfo) -> [String] {
        var notes = [L10n.tr("acq.q.l.remove.note")]
        if remove.use == nil, remove.inUse > 0 {
            notes.append(L10n.format("acq.q.l.remove.used", L10n.plural("acq.q.used.in", remove.inUse)))
        }
        return notes
    }

    private func removeError(_ remove: QualityRemoveInfo) -> String? {
        if let use = remove.use {
            return L10n.tr("acq.q.l.remove.in.use") + " " + QualityWords.useWords(use) + "."
        }
        if let error = remove.error { return QualityWords.errorText(error) }
        return nil
    }

    @ViewBuilder
    private var removeTraitSheet: some View {
        if let remove = info.traitRemove {
            QualityConfirmSheet(
                title: L10n.tr("acq.q.ot.remove.title"),
                name: remove.name,
                notes: [L10n.tr("acq.q.ot.remove.note")],
                error: remove.error.map { QualityWords.errorText($0) },
                cancelTitle: L10n.tr("acq.q.ot.cancel"),
                confirmTitle: L10n.tr("acq.q.ot.remove"),
                busy: remove.busy,
                cancel: { store.cancelRemoveTrait() },
                confirm: { store.confirmRemoveTrait() }
            )
        }
    }
}
