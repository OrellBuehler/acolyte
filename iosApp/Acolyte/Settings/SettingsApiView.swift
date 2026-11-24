import SwiftUI

struct SettingsApiView: View {
    @State private var store = SettingsApiStore()
    @State private var label = ""
    @State private var confirm: SettingsKeysConfirm?
    @State private var toast: String?
    @State private var sharing: SettingsKeysShareItem?
    @Environment(\.openURL) private var openURL

    private var info: SettingsApiInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    form
                }
            }
        }
        .navigationTitle(L10n.tr("settings.page.api"))
        .settingsKeysMessage(toast: $toast, notice: info.notice) { store.dismissNotice($0) }
        .reauthSheet(store.reauth)
        .confirmationDialog(
            confirmTitle(confirm),
            isPresented: Binding(get: { confirm != nil }, set: { if !$0 { confirm = nil } }),
            titleVisibility: .visible,
            presenting: confirm
        ) { item in
            Button(confirmButton(item), role: .destructive) { run(item) }
            Button(L10n.tr("settings.keys.keep"), role: .cancel) {}
        } message: { item in
            Text(confirmMessage(item))
        }
        .sheet(item: $sharing) { item in
            ActivityShareSheet(items: [item.text])
        }
        .onChange(of: info.fresh?.id) { _, id in
            if id != nil { label = "" }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var form: some View {
        Form {
            if let error = info.load.error {
                Section { NoticeBanner(text: error.localized) }
            }
            keysSection
            startSection
            if !info.seerrGone {
                SettingsSeerrSections(
                    info: info,
                    store: store,
                    copy: { text, done in copyText(text, done) },
                    share: { sharing = SettingsKeysShareItem(text: $0) },
                    confirm: { confirm = $0 }
                )
            }
            referenceSection
        }
        .refreshable { await store.refresh() }
    }

    @ViewBuilder
    private var keysSection: some View {
        Section {
            if let error = info.keysError {
                Text(info.keysNeedPassword ? L10n.tr("settings.keys.api.sign.in") : error.localized)
                    .foregroundStyle(Theme.failText)
            } else {
                if let fresh = info.fresh {
                    SettingsKeysFreshBox(
                        title: fresh.name,
                        text: L10n.tr("settings.keys.api.fresh.text"),
                        secret: fresh.key,
                        copy: { copyText(fresh.key, L10n.tr("settings.keys.copied.key")) },
                        share: { sharing = SettingsKeysShareItem(text: fresh.key) },
                        close: { store.dismissFresh() }
                    )
                }
                TextField(L10n.tr("settings.keys.key.name"), text: $label, prompt: Text(L10n.tr("settings.keys.api.name.hint")))
                    .onChange(of: label) { _, value in
                        if value.count > info.labelMax { label = String(value.prefix(info.labelMax)) }
                    }
                Button(L10n.tr("settings.keys.api.make")) { store.makeKey(label) }
                    .disabled(label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || info.making)
                if info.keys.isEmpty {
                    Text(L10n.tr("settings.keys.api.none"))
                        .foregroundStyle(Theme.textMuted)
                } else {
                    ForEach(info.keys) { key in
                        SettingsKeysRowView(
                            label: key.label,
                            hint: key.hint,
                            lines: [SettingsKeysText.usage(key.createdAt, key.lastUsedAt)]
                        ) {
                            Button(L10n.tr("settings.keys.revoke"), role: .destructive) {
                                confirm = .revoke(id: key.id, name: key.label)
                            }
                            .buttonStyle(.borderless)
                            .disabled(info.revokingId != 0)
                        }
                    }
                }
            }
        } header: {
            Text(L10n.tr("settings.keys.api.keys.title"))
        } footer: {
            Text(L10n.tr("settings.keys.api.keys.hint"))
        }
    }

    private var startSection: some View {
        Section(L10n.tr("settings.keys.api.start.title")) {
            Text("1. " + L10n.tr("settings.keys.api.step1"))
            Text("2. " + L10n.tr("settings.keys.api.step2"))
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text("3. " + L10n.tr("settings.keys.api.step3"))
                SettingsKeysCopyRow(
                    value: info.baseAddress,
                    label: L10n.tr("settings.keys.api.copy.base"),
                    copy: { copyText(info.baseAddress, L10n.tr("settings.keys.copied.address")) }
                )
            }
        }
    }

    private var referenceSection: some View {
        Section {
            Button {
                if let url = URL(string: info.openApiAddress) { openURL(url) }
            } label: {
                Label(L10n.tr("settings.keys.api.openapi"), systemImage: "arrow.up.right.square")
            }
        } header: {
            Text(L10n.tr("settings.keys.api.reference.title"))
        } footer: {
            Text(L10n.tr("settings.keys.api.openapi.hint"))
        }
    }

    private func copyText(_ text: String, _ done: String) {
        settingsKeysCopy(text)
        toast = done
    }

    private func run(_ item: SettingsKeysConfirm) {
        switch item {
        case .revoke(let id, _): store.revokeKey(id)
        case .replace(let side): store.makeSeerrKey(side)
        case .remove(let side): store.removeSeerrKey(side)
        }
    }

    private func confirmTitle(_ item: SettingsKeysConfirm?) -> String {
        switch item {
        case .revoke(_, let name): L10n.format("settings.keys.api.revoke.title", name)
        case .replace(let side): L10n.format("settings.keys.seerr.replace.title", SettingsKeysText.appName(side))
        case .remove(let side): L10n.format("settings.keys.seerr.remove.title", SettingsKeysText.appName(side))
        case nil: ""
        }
    }

    private func confirmMessage(_ item: SettingsKeysConfirm) -> String {
        switch item {
        case .revoke: L10n.tr("settings.keys.api.revoke.text")
        case .replace: L10n.tr("settings.keys.seerr.replace.text")
        case .remove(let side): L10n.format("settings.keys.seerr.remove.text", SettingsKeysText.whatLower(side))
        }
    }

    private func confirmButton(_ item: SettingsKeysConfirm) -> String {
        switch item {
        case .revoke: L10n.tr("settings.keys.revoke")
        case .replace: L10n.tr("settings.keys.replace")
        case .remove: L10n.tr("settings.keys.remove")
        }
    }
}
