import SwiftUI

struct SettingsAlertsView: View {
    @State private var store = SettingsServerAlertsStore()
    @State private var removing: SettingsServerAlertItem?

    private var info: SettingsServerAlertsInfo { store.info }

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
        .navigationTitle(L10n.tr("settings.page.alerts"))
        .toolbar {
            if info.allowed, !info.alerts.isEmpty {
                ToolbarItem(placement: .primaryAction) {
                    Button(L10n.tr("settings.server.alerts.add"), systemImage: "plus") { store.startAdd() }
                }
            }
        }
        .sheet(isPresented: Binding(get: { info.form != nil }, set: { if !$0 { store.cancelForm() } })) {
            SettingsServerAlertFormView(store: store)
        }
        .alert(
            L10n.format("settings.server.alert.remove.title", removing?.name ?? ""),
            isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } })
        ) {
            Button(L10n.tr("settings.server.alert.remove"), role: .destructive) {
                if let id = removing?.id { store.remove(id) }
            }
            Button(L10n.tr("common.cancel"), role: .cancel) {}
        }
        .safeAreaInset(edge: .bottom) {
            if let note = info.notice {
                NoteToast(text: noticeText(note), undo: nil)
            }
        }
        .animation(.smooth(duration: 0.24), value: info.notice?.seq)
        .task(id: info.notice?.seq) {
            guard let note = info.notice else { return }
            try? await Task.sleep(for: .seconds(4))
            if store.info.notice?.seq == note.seq { store.dismissNotice(note.seq) }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private func noticeText(_ note: SettingsServerAlertsNoticeInfo) -> String {
        switch note.kind {
        case .saved:
            return L10n.format("settings.server.alert.saved", note.name)
        case .removeFailed:
            return SettingsServerWords.withError(L10n.tr("settings.server.alert.remove.failed"), note.error)
        case .packSaved:
            let perPack = note.name == "one"
            let style = L10n.tr(perPack ? "settings.server.pack.per.pack" : "settings.server.pack.each")
            return L10n.format("settings.server.pack.saved", style)
        case .packFailed:
            return SettingsServerWords.withError(L10n.tr("settings.server.pack.failed"), note.error)
        }
    }

    private var list: some View {
        List {
            Section {
                Text(L10n.tr("settings.server.alerts.lead"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if let error = info.load.error {
                NoticeBanner(text: SettingsServerWords.errorLine(error))
            }
            if info.alerts.isEmpty {
                Section {
                    VStack(spacing: Theme.Spacing.small) {
                        EmptyStateView(
                            title: L10n.tr("settings.server.alerts.empty"),
                            message: L10n.tr("settings.server.alerts.empty.text"),
                            systemImage: "bell"
                        )
                        Button(L10n.tr("settings.server.alerts.add")) { store.startAdd() }
                            .buttonStyle(.borderedProminent)
                    }
                }
            } else {
                Section {
                    ForEach(info.alerts) { alert in
                        row(alert)
                    }
                }
            }
            if info.showPack {
                packSection
            }
        }
        .refreshable { await store.refresh() }
    }

    private func row(_ alert: SettingsServerAlertItem) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(alert.name).font(.headline)
            HStack(spacing: Theme.Spacing.small) {
                Text(chips(alert))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                if alert.test?.kind == .sent {
                    StatusChip(text: L10n.format("settings.server.test.sent", alert.name), tone: .ok)
                }
            }
            if alert.missing != .nothing {
                Text(L10n.format("settings.server.alert.needs", SettingsServerWords.missing(alert.missing)))
                    .font(.subheadline)
            }
            Text(SettingsServerWords.momentsLine(alert.moments))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            result(alert)
            HStack {
                Button(L10n.tr("settings.server.alert.test")) { store.test(alert.id) }
                    .disabled(alert.testing)
                Button(L10n.tr("settings.server.alert.change")) { store.startChange(alert.id) }
                    .disabled(!alert.editable || alert.testing)
                Button(L10n.tr("settings.server.alert.remove"), role: .destructive) { removing = alert }
                    .disabled(alert.testing)
                    .accessibilityLabel(L10n.format("settings.server.alert.remove.name", alert.name))
                if alert.testing {
                    ProgressView()
                }
            }
            .buttonStyle(.bordered)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private func chips(_ alert: SettingsServerAlertItem) -> String {
        var parts = [SettingsServerWords.kindLabel(alert.kind)]
        if !alert.enabled { parts.append(L10n.tr("settings.server.alert.off")) }
        return parts.joined(separator: " · ")
    }

    @ViewBuilder
    private func result(_ alert: SettingsServerAlertItem) -> some View {
        if alert.test?.kind == .typeFirst {
            Text(L10n.tr("settings.server.test.type.first"))
                .font(.footnote)
                .foregroundStyle(Theme.failText)
        } else if alert.test?.kind == .failed, let error = alert.test?.error {
            SettingsServerErrorText(error: error)
                .font(.footnote)
        } else if !alert.lastError.isEmpty {
            Text(L10n.format("settings.server.alert.last.failed", alert.lastError))
                .font(.footnote)
                .foregroundStyle(Theme.failText)
        } else if let sent = alert.lastSent {
            Text(L10n.format("settings.server.alert.last.sent", Format.relative(sent)))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private var packSection: some View {
        Section {
            if info.pack.loading {
                ProgressView()
            } else if let error = info.pack.error {
                SettingsServerErrorText(error: error)
                Button(L10n.tr("settings.server.pack.retry")) { store.retryPack() }
            } else {
                packChoice("one", title: L10n.tr("settings.server.pack.one"), hint: L10n.tr("settings.server.pack.one.hint"))
                packChoice("each", title: L10n.tr("settings.server.pack.each"), hint: nil)
            }
        } header: {
            Text(L10n.tr("settings.server.packs"))
        } footer: {
            Text(L10n.tr("settings.server.packs.note"))
        }
    }

    private func packChoice(_ style: String, title: String, hint: String?) -> some View {
        Button {
            store.setPack(style)
        } label: {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).foregroundStyle(Theme.text)
                    if let hint {
                        Text(hint)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                Spacer()
                if info.pack.style == style {
                    Image(systemName: "checkmark")
                        .foregroundStyle(Theme.accentText)
                }
            }
        }
        .disabled(info.pack.saving)
        .accessibilityAddTraits(info.pack.style == style ? .isSelected : [])
    }
}
