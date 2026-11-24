import SwiftUI

struct SettingsDownloadersView: View {
    let account: AccountInfo
    @State private var store = DownloadersStore()
    @State private var rules = DownloaderRulesStore()
    @State private var removing: DownloaderRowInfo?
    @State private var giving: DownloaderGiveUp?
    @State private var toast: String?

    private var info: DownloadersInfo { store.info }
    private var ruleInfo: DownloaderRulesInfo { rules.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await reloadAll() }) {
                    EmptyView()
                } content: {
                    list
                }
            }
        }
        .navigationTitle(L10n.tr("acq.page.downloaders"))
        .toolbar {
            if info.allowed, info.canEdit, !info.rows.isEmpty {
                ToolbarItem(placement: .primaryAction) {
                    Button(L10n.tr("acq.dl.add"), systemImage: "plus") { store.startAdd() }
                }
            }
        }
        .sheet(isPresented: Binding(get: { info.form != nil }, set: { if !$0 { store.cancelForm() } })) {
            DownloaderFormView(store: store)
        }
        .confirmationDialog(
            L10n.format("acq.dl.confirm.remove.title", removing?.name ?? ""),
            isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } }),
            titleVisibility: .visible,
            presenting: removing
        ) { row in
            Button(L10n.tr("acq.dl.remove"), role: .destructive) { store.remove(row.id) }
            Button(L10n.tr("acq.dl.cancel"), role: .cancel) {}
        } message: { _ in
            Text(L10n.tr("acq.dl.confirm.remove.text"))
        }
        .confirmationDialog(
            L10n.tr("acq.dl.abandon.title"),
            isPresented: Binding(get: { giving != nil }, set: { if !$0 { giving = nil } }),
            titleVisibility: .visible,
            presenting: giving
        ) { item in
            Button(L10n.tr("acq.dl.stop"), role: .destructive) { store.abandon(item.row.id, item.attempt.id) }
            Button(L10n.tr("acq.dl.cancel"), role: .cancel) {}
        } message: { item in
            Text(L10n.format("acq.dl.abandon.text", Int(item.attempt.releaseId), Int(item.attempt.slotId), item.row.name, item.row.appName))
        }
        .safeAreaInset(edge: .bottom) {
            if ruleInfo.dirty {
                DownloaderRulesBar(rules: rules)
            } else if let toast {
                NoteToast(text: toast, undo: nil)
            }
        }
        .onChange(of: info.messageCount) { _, count in
            guard count > 0, let message = info.message else { return }
            show(DownloadersWords.message(message))
        }
        .onChange(of: ruleInfo.savedCount) { _, count in
            guard count > 0 else { return }
            show(L10n.tr("acq.dl.rules.saved"))
        }
        .task { await store.watch() }
        .task { await rules.watch() }
        .task { await store.load() }
        .task { await rules.load() }
        .task { await store.followLive() }
    }

    private func reloadAll() async {
        await store.refresh()
        await rules.refresh()
    }

    private func show(_ text: String) {
        toast = text
        Task {
            try? await Task.sleep(for: .seconds(4))
            if toast == text { toast = nil }
        }
    }

    private var list: some View {
        List {
            if let error = info.load.error {
                Section { NoticeBanner(text: DownloadersWords.error(error)) }
            }
            Section {
                Text(L10n.tr("acq.dl.lead"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if info.rows.isEmpty {
                Section { empty }
            } else {
                Section {
                    ForEach(info.rows) { row in
                        DownloaderRowView(row: row, canEdit: info.canEdit, store: store) { giving = $0 }
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                if info.canEdit {
                                    Button(role: .destructive) {
                                        removing = row
                                    } label: {
                                        Label(L10n.tr("acq.dl.remove"), systemImage: "trash")
                                    }
                                    Button {
                                        if row.paused { store.resume(row.id) } else { store.pause(row.id) }
                                    } label: {
                                        Label(row.paused ? L10n.tr("acq.dl.resume") : L10n.tr("acq.dl.pause"), systemImage: row.paused ? "play" : "pause")
                                    }
                                    .tint(Theme.info)
                                }
                            }
                            .swipeActions(edge: .leading, allowsFullSwipe: false) {
                                if info.canEdit, row.canChange {
                                    Button { store.startChange(row.id) } label: { Label(L10n.tr("acq.dl.change"), systemImage: "pencil") }
                                        .tint(Theme.accent)
                                }
                            }
                    }
                }
            }
            DownloaderRulesSection(rules: rules)
        }
        .listStyle(.insetGrouped)
    }

    private var empty: some View {
        VStack(spacing: Theme.Spacing.small) {
            Image(systemName: "externaldrive.connected.to.line.below")
                .font(.largeTitle)
                .foregroundStyle(Theme.textMuted)
            Text(L10n.tr("acq.dl.none")).font(.headline)
            Text(L10n.tr("acq.dl.none.text"))
                .font(.subheadline)
                .foregroundStyle(Theme.textMuted)
            if info.canEdit {
                Button(L10n.tr("acq.dl.add")) { store.startAdd() }
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Theme.Spacing.large)
    }
}
