import SwiftUI

struct MoveListRequest: Identifiable, Hashable {
    let source: MoveListSource
    let listId: String
    let title: String

    var id: String { "\(source)-\(listId)" }
}

struct MoveReadRequest: Identifiable, Hashable {
    let id = UUID()
    let kind: String
    let address: String
    let editing: Bool
}

struct MoveShareFile: Identifiable {
    let id = UUID()
    let url: URL
}

struct MoveKeysRequest: Identifiable, Hashable {
    let source: MoveListSource

    var id: String { "\(source)" }
}

struct SettingsMoveView: View {
    let account: AccountInfo
    @State private var store = MoveStore()
    @State private var readRequest: MoveReadRequest?
    @State private var forgetting: MoveReadInfo?
    @State private var listing: MoveListRequest?
    @State private var keysRequest: MoveKeysRequest?
    @State private var showNotMoved = false
    @Environment(\.openRoute) private var openRoute

    private var info: MoveInfo { store.info }

    private var serverHost: String {
        URLComponents(string: account.serverURL)?.host ?? ""
    }

    var body: some View {
        Group {
            if !info.allowed {
                EmptyStateView(title: L10n.tr("acq.mv.not.allowed"), message: L10n.tr("acq.mv.not.allowed.text"), systemImage: "lock")
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    wizard
                }
            }
        }
        .navigationTitle(L10n.tr("acq.page.move"))
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .reauthSheet(store.reauth, enabled: !othersPresented)
        .sheet(item: $readRequest, onDismiss: { if !info.sheet.reading { store.resetSheet() } }) { request in
            MoveReadSheet(store: store, request: request, serverHost: serverHost)
        }
        .sheet(item: $listing) { request in
            MoveItemsSheet(store: store, request: request)
        }
        .sheet(item: $keysRequest) { request in
            MoveKeysSheet(store: store, source: request.source)
        }
        .sheet(isPresented: $showNotMoved) {
            MoveNotMovedSheet(store: store)
        }
        .sheet(isPresented: beforeBinding) {
            MoveBeforeSheet(store: store, onShow: { listing = $0 })
        }
        .sheet(item: shareBinding) { file in
            MoveShareSheet(url: file.url)
        }
        .alert(L10n.plural("acq.mv.confirm.title", info.choose.total), isPresented: confirmBinding) {
            Button(L10n.tr("acq.mv.import")) { store.start() }
            Button(L10n.tr("acq.mv.cancel"), role: .cancel) {}
        } message: {
            Text(confirmMessage)
        }
        .alert(L10n.tr("acq.mv.undo.title"), isPresented: undoBinding) {
            Button(L10n.tr("acq.mv.undo.confirm"), role: .destructive) { store.undo() }
            Button(L10n.tr("acq.mv.cancel"), role: .cancel) {}
        } message: {
            Text(L10n.tr("acq.mv.undo.text"))
        }
        .alert(L10n.format("acq.mv.forget.title", forgetting?.name ?? ""), isPresented: forgetBinding) {
            Button(L10n.tr("acq.mv.remove"), role: .destructive) {
                if let row = forgetting { store.forget(row.id) }
                forgetting = nil
            }
            Button(L10n.tr("acq.mv.cancel"), role: .cancel) { forgetting = nil }
        } message: {
            Text(L10n.tr("acq.mv.forget.text"))
        }
    }

    private var othersPresented: Bool {
        readRequest != nil || listing != nil || keysRequest != nil || showNotMoved || info.dialog == .before || store.shareFile != nil
    }

    private var shareBinding: Binding<MoveShareFile?> {
        Binding(get: { store.shareFile }, set: { store.shareFile = $0 })
    }

    private var forgetBinding: Binding<Bool> {
        Binding(get: { forgetting != nil }, set: { if !$0 { forgetting = nil } })
    }

    private var beforeBinding: Binding<Bool> {
        Binding(get: { info.dialog == .before }, set: { if !$0 && info.dialog == .before { store.closeDialog() } })
    }

    private var confirmBinding: Binding<Bool> {
        Binding(get: { info.dialog == .confirm }, set: { if !$0 && info.dialog == .confirm { store.closeDialog() } })
    }

    private var undoBinding: Binding<Bool> {
        Binding(get: { info.dialog == .undo }, set: { if !$0 && info.dialog == .undo { store.closeDialog() } })
    }

    private var confirmMessage: String {
        var lines: [String] = []
        if info.choose.stillChecking > 0 {
            lines.append(L10n.format("acq.mv.still.checking.import", Int32(info.choose.stillChecking)))
        }
        if info.choose.toCheck > 0 { lines.append(L10n.plural("acq.mv.note.check", info.choose.toCheck)) }
        if !info.choose.apps.isEmpty { lines.append(L10n.format("acq.mv.confirm.from", MoveWords.andList(info.choose.apps))) }
        lines.append(L10n.tr("acq.mv.confirm.undo.later"))
        return lines.joined(separator: "\n")
    }

    private var running: Bool {
        info.step == .run && (info.run?.running ?? false)
    }

    private var wizard: some View {
        VStack(spacing: 0) {
            MoveRail(info: info, locked: running) { store.enter($0) }
            if let problem = info.problem {
                HStack(alignment: .top) {
                    NoticeBanner(text: MoveWords.problem(problem))
                    Button(L10n.tr("acq.mv.close")) { store.dismissProblem() }
                        .font(.footnote)
                }
                .padding(.horizontal, Theme.Spacing.screen)
                .padding(.top, Theme.Spacing.small)
            }
            if info.safeMode {
                NoticeBanner(text: L10n.tr("acq.mv.safe.mode"))
                    .padding(.horizontal, Theme.Spacing.screen)
                    .padding(.top, Theme.Spacing.small)
            }
            if let notice = info.notices.first {
                MoveToast(text: MoveWords.notice(notice))
                    .task(id: notice.seq) {
                        try? await Task.sleep(for: .seconds(4))
                        store.dismissNotice(notice.seq)
                    }
            }
            stepContent
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            MoveFooter(store: store)
        }
    }

    @ViewBuilder
    private var stepContent: some View {
        if info.gone {
            ContentUnavailableView {
                Label(L10n.tr("acq.mv.gone"), systemImage: "exclamationmark.triangle")
            } actions: {
                Button(L10n.tr("acq.mv.gone.back")) { store.goBackToReads() }
                    .buttonStyle(.borderedProminent)
            }
        } else {
            switch info.step {
            case .read:
                MoveReadStepView(
                    store: store,
                    onAdd: { readRequest = MoveReadRequest(kind: "radarr", address: "", editing: false) },
                    onEdit: { readRequest = MoveReadRequest(kind: $0.kind, address: $0.address, editing: true) },
                    onForget: { forgetting = $0 },
                    onOpenDownloaders: { openRoute(.settingsDownloaders) }
                )
            case .choose:
                MoveChooseStepView(
                    store: store,
                    onShow: { listing = $0 },
                    onKeys: { keysRequest = MoveKeysRequest(source: $0) },
                    onNotMoved: { showNotMoved = true }
                )
            case .run:
                MoveRunStepView(info: info)
            case .done:
                MoveDoneStepView(store: store, onShow: { listing = $0 }, onOpen: { openRoute($0) })
            }
        }
    }
}

private struct MoveRail: View {
    let info: MoveInfo
    let locked: Bool
    let pick: (MoveStepKind) -> Void

    private var current: Int {
        switch info.step {
        case .read: 0
        case .choose: 1
        case .run: 2
        case .done: 3
        }
    }

    private var busy: String? {
        guard info.step == .choose else { return nil }
        let c = info.choose
        guard c.planning || c.trying || c.checkingNow else { return nil }
        if let note = c.note { return MoveWords.check(note) }
        return L10n.tr("acq.mv.checking")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(spacing: Theme.Spacing.small) {
                capsule(0, .read, L10n.tr("acq.mv.rail.read"))
                capsule(1, .choose, L10n.tr("acq.mv.rail.choose"))
                capsule(2, .run, L10n.tr("acq.mv.rail.import"))
                capsule(3, .done, L10n.tr("acq.mv.rail.done"))
            }
            if let busy {
                Text(busy)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.small)
    }

    private func capsule(_ index: Int, _ step: MoveStepKind, _ label: String) -> some View {
        let selected = index == current
        let enabled = index < current && !locked
        return Button {
            pick(step)
        } label: {
            Text("\(index + 1) \(label)")
                .font(.footnote.weight(.medium))
                .lineLimit(1)
                .frame(maxWidth: .infinity)
                .padding(.vertical, Theme.Spacing.small)
                .foregroundStyle(selected ? Theme.onAccent : Theme.textMuted)
                .background(selected ? Theme.accentFill : Theme.surfaceHigh, in: Capsule())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

private struct MoveToast: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.text)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.medium)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.top, Theme.Spacing.small)
            .transition(.opacity)
    }
}

private struct MoveFooter: View {
    let store: MoveStore

    private var info: MoveInfo { store.info }

    var body: some View {
        if info.step == .run {
            EmptyView()
        } else {
            VStack(spacing: Theme.Spacing.small) {
                let text = MoveWords.gate(info.gate, note: info.choose.note)
                if info.step != .done && !text.isEmpty {
                    Text(text)
                        .font(.footnote)
                        .foregroundStyle(info.gate.ok ? Theme.textMuted : Theme.failText)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                buttons
            }
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.medium)
            .background(.bar)
        }
    }

    @ViewBuilder
    private var buttons: some View {
        switch info.step {
        case .read:
            Button { store.enter(.choose) } label: {
                Text(L10n.tr("acq.mv.continue")).frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(!info.gate.ok)
        case .choose:
            HStack(spacing: Theme.Spacing.medium) {
                Button { store.enter(.read) } label: {
                    Text(L10n.tr("acq.mv.back")).frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                Button { store.importNow() } label: {
                    Text(L10n.tr("acq.mv.import")).frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!info.gate.ok || info.choose.checkingNow || info.starting)
            }
        case .done:
            if let run = info.run {
                doneButtons(run)
            }
        case .run:
            EmptyView()
        }
    }

    private func doneButtons(_ run: MoveRunInfo) -> some View {
        VStack(spacing: Theme.Spacing.small) {
            HStack(spacing: Theme.Spacing.medium) {
                if run.hasReport && !run.running {
                    Button {
                        store.shareReport()
                    } label: {
                        Text(L10n.tr("acq.mv.report")).frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
                Button { store.done() } label: {
                    Text(L10n.tr("acq.mv.done")).frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(run.running)
            }
            if run.undoFailed {
                Button { store.askUndo() } label: {
                    Text(L10n.tr("acq.mv.undo.again")).frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            } else if run.canUndo {
                Button(role: .destructive) { store.askUndo() } label: {
                    Text(L10n.tr("acq.mv.undo.this")).frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
    }
}

struct MoveShareSheet: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
