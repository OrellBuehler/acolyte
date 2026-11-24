import SwiftUI

struct SourceEditTarget: Identifiable {
    let token: Int
    let form: SourceFormInfo

    var id: Int { token }
}

struct SettingsSourcesView: View {
    let account: AccountInfo
    @State private var store = SourcesStore()
    @State private var bridges = BridgeStore()
    @State private var check = SourceCheckStore()
    @State private var removing: SourceRowInfo?
    @State private var unlinking: BridgeRowInfo?
    @Environment(\.openRoute) private var openRoute

    private var info: SourcesInfo { store.info }

    private var notice: SourceNoticeInfo? { info.notice ?? bridges.info.notice }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(
                    load: info.load,
                    isEmpty: false,
                    reload: { [store, bridges] in
                        await store.refresh()
                        await bridges.refresh()
                    }
                ) {
                    EmptyView()
                } content: {
                    list
                }
            }
        }
        .navigationTitle(L10n.tr("acq.page.sources"))
        .toolbar {
            if info.canAdd && info.hasRows {
                ToolbarItem(placement: .primaryAction) {
                    Button(L10n.tr("acq.src.add")) { store.startAdd() }
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .task { await bridges.watch() }
        .task { await bridges.load() }
        .task { await check.watch() }
        .onChange(of: bridges.info.changed) { _, _ in
            Task { await store.reloadQuietly() }
        }
        .sheet(item: editTarget) { target in
            SourceFormSheet(target: target, info: info, store: store)
        }
        .sheet(item: bridgeTarget) { target in
            BridgeFormSheet(target: target, store: bridges)
        }
        .sheet(item: $unlinking) { bridge in
            BridgeUnlinkSheet(bridge: bridge, store: bridges) { unlinking = nil }
        }
        .sheet(isPresented: Binding(get: { !bridges.info.madeKeys.isEmpty }, set: { if !$0 { bridges.dismissKeys() } })) {
            BridgeKeysSheet(store: bridges)
        }
        .confirmationDialog(
            removing.map { L10n.format("acq.src.remove.title", $0.name) } ?? "",
            isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } }),
            titleVisibility: .visible
        ) {
            Button(L10n.tr("acq.src.remove"), role: .destructive) {
                if let row = removing { store.remove(row.id) }
                removing = nil
            }
            Button(L10n.tr("acq.src.cancel"), role: .cancel) { removing = nil }
        } message: {
            Text(L10n.tr("acq.src.remove.text"))
        }
        .safeAreaInset(edge: .bottom) {
            if let notice {
                NoteToast(text: SourcesWords.notice(notice), undo: nil)
                    .task(id: notice.seq) {
                        try? await Task.sleep(for: .seconds(4))
                        store.dismissNotice(notice.seq)
                        bridges.dismissNotice(notice.seq)
                    }
            }
        }
    }

    private var editTarget: Binding<SourceEditTarget?> {
        Binding(
            get: { info.editing.map { SourceEditTarget(token: info.editToken, form: $0) } },
            set: { if $0 == nil { store.cancelEdit() } }
        )
    }

    private var bridgeTarget: Binding<BridgeFormTarget?> {
        Binding(
            get: { bridges.info.form.map { BridgeFormTarget(token: bridges.info.formToken, initial: $0.initial) } },
            set: { if $0 == nil { bridges.cancelForm() } }
        )
    }

    private var list: some View {
        ScrollViewReader { proxy in
            List {
                Section {
                    Text(L10n.tr("acq.src.lead"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    Text(L10n.tr("acq.src.built.in"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                if let error = info.load.error {
                    Section { NoticeBanner(text: error.localized) }
                }
                if info.partOff {
                    Section { offState }
                } else if !info.hasRows {
                    Section { emptyState }
                }
                if info.hasRows {
                    Section {
                        ForEach(info.rows) { row in
                            SourceRowView(
                                row: row,
                                canEdit: info.canEdit,
                                store: store,
                                onHealth: { openRoute(.settingsSourceHealth(id: row.id)) },
                                onRemove: { removing = row },
                                onBridge: { withAnimation { proxy.scrollTo(BridgeSection.anchor, anchor: .top) } }
                            )
                        }
                    }
                }
                if !info.partOff {
                    BridgeSection(store: bridges) { unlinking = $0 }
                    if info.hasRows && info.canEdit {
                        SourceCheckSection(store: check)
                    }
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: Theme.Spacing.medium) {
            Text(L10n.tr("acq.src.none.title")).font(.headline)
            Text(L10n.tr("acq.src.none.text"))
                .font(.subheadline)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            if info.canAdd {
                Button(L10n.tr("acq.src.add")) { store.startAdd() }
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Theme.Spacing.large)
    }

    private var offState: some View {
        VStack(spacing: Theme.Spacing.medium) {
            Text(L10n.tr("acq.src.off.title")).font(.headline)
            Text(L10n.tr("acq.src.off.text"))
                .font(.subheadline)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Theme.Spacing.large)
    }
}

struct SourceRowView: View {
    let row: SourceRowInfo
    let canEdit: Bool
    let store: SourcesStore
    let onHealth: () -> Void
    let onRemove: () -> Void
    let onBridge: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(row.name).font(.headline)
                    Text(row.formatLabel + " · " + SourcesWords.protocolName(row.protocolName))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Spacer(minLength: Theme.Spacing.small)
                if row.showAvg {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(SourcesWords.duration(row.avgMs)).font(.subheadline.weight(.semibold))
                        Text(L10n.tr("acq.src.avg")).font(.caption2).foregroundStyle(Theme.textMuted)
                    }
                }
            }
            StatusChip(text: SourcesWords.word(row), tone: row.tone)
            notes
            actions
        }
        .padding(.vertical, Theme.Spacing.xSmall)
        .contentShape(Rectangle())
        .onTapGesture { onHealth() }
        .swipeActions(edge: .trailing) {
            if canEdit {
                Button(L10n.tr("acq.src.remove"), role: .destructive) { onRemove() }
                Button(L10n.tr(row.isDisabled ? "acq.src.resume" : "acq.src.pause")) { store.setPaused(row.id, !row.isDisabled) }
                    .tint(Theme.wait)
            }
        }
    }

    @ViewBuilder
    private var notes: some View {
        if let test = row.test {
            Text(SourcesWords.outcome(test))
                .font(.footnote)
                .foregroundStyle(test.ok ? Theme.okText : Theme.failText)
            if !test.ok, let cause = test.cause {
                Text(SourcesWords.cause(cause)).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
        if row.showNote {
            let until = SourcesWords.pausedUntil(row.until).map { " " + $0 } ?? ""
            Text(row.note + until)
                .font(.footnote)
                .foregroundStyle(row.noteBad ? Theme.failText : Theme.textMuted)
            if row.test == nil, let cause = row.cause {
                Text(SourcesWords.cause(cause)).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
        if row.showUntil, let until = SourcesWords.pausedUntil(row.until) {
            Text(until).font(.footnote).foregroundStyle(Theme.textMuted)
        }
        if row.isDisabled {
            Text(L10n.tr("acq.src.paused.by.you")).font(.footnote).foregroundStyle(Theme.textMuted)
        }
        if row.botCheckHint {
            Button(L10n.tr("acq.src.bot.check.link")) { onBridge() }
                .font(.footnote)
                .buttonStyle(.borderless)
        }
    }

    private var actions: some View {
        HStack(spacing: Theme.Spacing.small) {
            if canEdit {
                Button {
                    store.test(row.id)
                } label: {
                    if row.testing {
                        HStack(spacing: 6) {
                            ProgressView()
                            Text(L10n.tr("acq.src.testing"))
                        }
                    } else {
                        Text(L10n.tr("acq.src.test"))
                    }
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
                .disabled(row.testing || row.busy)
                if row.canTryNow {
                    Button(L10n.tr("acq.src.try.now")) { store.tryNow(row.id) }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .disabled(row.testing || row.busy)
                }
                Spacer()
                Menu {
                    Button(L10n.tr(row.isDisabled ? "acq.src.resume" : "acq.src.pause")) { store.setPaused(row.id, !row.isDisabled) }
                        .disabled(row.busy)
                    Button(L10n.tr("acq.src.health")) { onHealth() }
                    Button(L10n.tr("acq.src.change")) { store.startChange(row.id) }
                    Button(L10n.tr("acq.src.remove"), role: .destructive) { onRemove() }
                        .disabled(row.busy)
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .imageScale(.large)
                        .accessibilityLabel(L10n.tr("acq.src.more"))
                }
                .buttonStyle(.borderless)
            } else {
                Button(L10n.tr("acq.src.health")) { onHealth() }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }
        }
    }
}
