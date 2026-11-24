import SwiftUI

struct SettingsMediaServersView: View {
    @State private var store = SettingsMediaServersStore()
    @State private var removing: SettingsMediaServersRowInfo?
    @State private var askWatchOff = false
    @Environment(\.openURL) private var openURL

    private var info: SettingsMediaServersInfo { store.info }

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
        .navigationTitle(L10n.tr("settings.page.media.servers"))
        .toolbar {
            if info.allowed, !info.servers.isEmpty {
                ToolbarItem(placement: .primaryAction) {
                    Button(L10n.tr("settings.mediaservers.add"), systemImage: "plus") { store.startAdd() }
                }
            }
        }
        .sheet(isPresented: Binding(get: { info.form != nil }, set: { if !$0 { store.cancelForm() } })) {
            SettingsMediaServersFormSheet(store: store)
        }
        .settingsReauth(
            info.form == nil ? info.reauth : .idle,
            confirm: { password, code in store.confirmReauth(password, code) },
            cancel: { store.cancelReauth() }
        )
        .alert(
            L10n.format("settings.mediaservers.remove.title", removing?.name ?? ""),
            isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } })
        ) {
            Button(L10n.tr("settings.mediaservers.remove"), role: .destructive) {
                if let id = removing?.id { store.remove(id) }
            }
            Button(L10n.tr("common.cancel"), role: .cancel) {}
        } message: {
            Text(L10n.tr("settings.mediaservers.remove.text"))
        }
        .alert(L10n.tr("settings.mediaservers.watch.off.title"), isPresented: $askWatchOff) {
            Button(L10n.tr("settings.mediaservers.watch.off.confirm"), role: .destructive) { store.setWatchHistory(false) }
            Button(L10n.tr("common.cancel"), role: .cancel) {}
        } message: {
            Text(L10n.tr("settings.mediaservers.watch.off.text"))
        }
        .safeAreaInset(edge: .bottom) {
            if let note = info.notice {
                NoteToast(text: SettingsMediaServersWords.noticeText(note), undo: nil)
            }
        }
        .animation(.smooth(duration: 0.24), value: info.notice?.id)
        .task(id: info.notice?.id) {
            guard let note = info.notice else { return }
            try? await Task.sleep(for: .seconds(5))
            if store.info.notice?.id == note.id { store.dismissNotice(note.id) }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var list: some View {
        List {
            if let error = info.load.error {
                NoticeBanner(text: error.localized)
            }
            if info.isEmpty {
                VStack(spacing: Theme.Spacing.small) {
                    EmptyStateView(
                        title: L10n.tr("settings.mediaservers.empty.title"),
                        message: L10n.tr("settings.mediaservers.empty.text"),
                        systemImage: "tv"
                    )
                    Button(L10n.tr("settings.mediaservers.add")) { store.startAdd() }
                        .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity)
            }
            ForEach(info.servers) { row in
                serverRow(row)
            }
            if info.showWatch {
                SettingsMediaServersWatchSection(
                    info: info,
                    turnOn: { store.setWatchHistory(true) },
                    askTurnOff: { askWatchOff = true }
                )
            }
            if info.showLabels {
                SettingsMediaServersLabelsSection(info: info, store: store)
            }
            if info.showTracks {
                SettingsMediaServersTracksSection(info: info, store: store)
            }
        }
    }

    private func chip(_ text: String) -> some View {
        Text(text)
            .font(.footnote.weight(.medium))
            .foregroundStyle(Theme.text)
            .lineLimit(1)
            .padding(.horizontal, 10)
            .frame(height: 24)
            .background(Theme.surfaceRaised, in: Capsule())
    }

    private func serverRow(_ row: SettingsMediaServersRowInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(row.name).font(.headline)
            HStack(spacing: Theme.Spacing.small) {
                chip(row.kindName)
                if !row.enabled {
                    chip(L10n.tr("settings.mediaservers.off"))
                }
            }
            if let tag = row.tag {
                StatusChip(text: tag.label, tone: tag.ok ? .ok : .fail)
            }
            Text(row.url)
                .font(.footnote.monospaced())
                .foregroundStyle(Theme.textMuted)
            Text(SettingsMediaServersWords.told(row))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            result(row)
            HStack {
                Button {
                    store.test(row.id)
                } label: {
                    if row.testing {
                        ProgressView()
                    } else {
                        Text(L10n.tr("settings.mediaservers.test"))
                    }
                }
                .disabled(row.testing || row.busy)
                Button(L10n.tr("settings.mediaservers.change")) { store.startChange(row.id) }
                    .disabled(row.busy)
                Button(L10n.tr("settings.mediaservers.remove"), role: .destructive) { removing = row }
                    .disabled(row.busy)
            }
            .buttonStyle(.bordered)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    @ViewBuilder
    private func result(_ row: SettingsMediaServersRowInfo) -> some View {
        if row.showsFailure, let tag = row.tag {
            Text(tag.failure.isEmpty ? (tag.error?.localized ?? "") : tag.failure)
                .font(.footnote)
                .foregroundStyle(Theme.failText)
            if let url = URL(string: tag.helpUrl), !tag.helpUrl.isEmpty {
                Button(L10n.tr("settings.mediaservers.help")) { openURL(url) }
                    .font(.footnote)
                    .buttonStyle(.borderless)
            }
        } else if row.showsLastError {
            Text(L10n.format("settings.mediaservers.last.failed", row.lastError))
                .font(.footnote)
                .foregroundStyle(Theme.failText)
        } else if row.showsLastChecked {
            Text(L10n.format("settings.mediaservers.last.checked", row.lastOkAt.map { Format.relative($0) } ?? ""))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }
}
