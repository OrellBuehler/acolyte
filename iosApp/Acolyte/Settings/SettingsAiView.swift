import SwiftUI

struct SettingsAiView: View {
    @State private var store = SettingsAiStore()
    @State private var label = ""
    @State private var ticked: Set<String> = []
    @State private var tickedReady = false
    @State private var byHand = false
    @State private var revoking: SettingsAiKeyInfo?
    @State private var editing: SettingsAiKeyInfo?
    @State private var toast: String?
    @State private var sharing: SettingsKeysShareItem?

    private var info: SettingsAiInfo { store.info }

    var body: some View {
        StatusContent(load: info.load, isEmpty: !info.available, reload: { await store.refresh() }) {
            EmptyStateView(
                title: L10n.tr("settings.keys.ai.unavailable.title"),
                message: L10n.tr("settings.keys.ai.unavailable.text"),
                systemImage: "sparkles"
            )
        } content: {
            form
        }
        .navigationTitle(L10n.tr("settings.page.ai"))
        .settingsKeysMessage(toast: $toast, notice: info.notice) { store.dismissNotice($0) }
        .reauthSheet(store.reauth, enabled: editing == nil)
        .confirmationDialog(
            L10n.format("settings.keys.ai.revoke.title", revoking?.label ?? ""),
            isPresented: Binding(get: { revoking != nil }, set: { if !$0 { revoking = nil } }),
            titleVisibility: .visible,
            presenting: revoking
        ) { key in
            Button(L10n.tr("settings.keys.revoke"), role: .destructive) { store.revokeKey(key.id) }
            Button(L10n.tr("settings.keys.keep"), role: .cancel) {}
        } message: { _ in
            Text(L10n.tr("settings.keys.ai.revoke.text"))
        }
        .sheet(item: $editing, onDismiss: { store.clearChangeError() }) { key in
            SettingsAiChangeSheet(store: store, key: key, offered: info.offeredGroups, opened: info.changedCount)
        }
        .sheet(item: $sharing) { item in
            ActivityShareSheet(items: [item.text])
        }
        .onChange(of: info.defaultGroups, initial: true) { _, groups in
            if !tickedReady && !groups.isEmpty {
                ticked = Set(groups)
                tickedReady = true
            }
        }
        .onChange(of: info.fresh?.id) { _, id in
            if id != nil {
                label = ""
                ticked = Set(info.defaultGroups)
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var form: some View {
        Form {
            if let error = info.load.error {
                Section { NoticeBanner(text: error.localized) }
            }
            switchSection
            connectSection
            if info.canManageKeys {
                linkedSection
            }
        }
        .refreshable { await store.refresh() }
    }

    private var switchSection: some View {
        Section {
            Text(L10n.tr("settings.keys.ai.intro"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Toggle(
                L10n.tr("settings.keys.ai.switch"),
                isOn: Binding(get: { info.enabled }, set: { store.setEnabled($0) })
            )
            .disabled(!info.isOwner || info.saving)
            if !info.isOwner {
                Text(L10n.tr("settings.keys.ai.owner.only"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            fact("settings.keys.ai.fact1")
            fact("settings.keys.ai.fact2")
            fact("settings.keys.ai.fact3")
        } header: {
            Text(L10n.tr("settings.keys.ai.card.title"))
        }
    }

    private func fact(_ key: String) -> some View {
        Text("• " + L10n.tr(key))
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
    }

    private var connectSection: some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(L10n.tr("settings.keys.ai.address"))
                SettingsKeysCopyRow(
                    value: info.address,
                    label: L10n.tr("settings.keys.ai.copy.address"),
                    copy: { copyText(info.address, L10n.tr("settings.keys.copied.address")) }
                )
            }
            Text(L10n.tr("settings.keys.ai.error.expected"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if !info.enabled {
                Text(L10n.tr("settings.keys.ai.turn.on.first"))
                    .font(.footnote)
                    .foregroundStyle(Theme.waitText)
            }
        } header: {
            Text(L10n.tr("settings.keys.ai.connect.title"))
        }
    }

    private var linkedSection: some View {
        Section {
            if let error = info.keysError {
                Text(info.keysNeedPassword ? L10n.tr("settings.keys.ai.sign.in") : error.localized)
                    .foregroundStyle(Theme.failText)
            } else {
                if let fresh = info.fresh {
                    SettingsKeysFreshBox(
                        title: L10n.tr("settings.keys.ai.fresh.title"),
                        text: L10n.format("settings.keys.ai.fresh.text", fresh.name),
                        secret: fresh.key,
                        copy: { copyText(fresh.key, L10n.tr("settings.keys.copied.key")) },
                        share: { sharing = SettingsKeysShareItem(text: fresh.key) },
                        close: { store.dismissFresh() }
                    )
                }
                if info.keys.isEmpty {
                    Text(L10n.tr("settings.keys.ai.none"))
                        .foregroundStyle(Theme.textMuted)
                } else {
                    ForEach(info.keys) { key in
                        SettingsKeysRowView(
                            label: key.label,
                            hint: key.hint,
                            lines: [SettingsKeysText.canLine(key.groups), SettingsKeysText.usage(key.createdAt, key.lastUsedAt)]
                        ) {
                            if info.canChange {
                                Button(L10n.tr("settings.keys.ai.change")) { editing = key }
                                    .buttonStyle(.borderless)
                            }
                            Button(L10n.tr("settings.keys.revoke"), role: .destructive) { revoking = key }
                                .buttonStyle(.borderless)
                                .disabled(info.revokingId != 0)
                        }
                    }
                }
                DisclosureGroup(L10n.tr("settings.keys.ai.by.hand"), isExpanded: $byHand) {
                    byHandForm
                }
            }
        } header: {
            Text(L10n.tr("settings.keys.ai.linked.title"))
        } footer: {
            Text(L10n.tr("settings.keys.ai.linked.note"))
        }
    }

    @ViewBuilder
    private var byHandForm: some View {
        Text(L10n.tr("settings.keys.ai.groups.legend"))
            .font(.subheadline.weight(.semibold))
        ForEach(info.offeredGroups, id: \.self) { group in
            SettingsKeysGroupToggle(group: group, isOn: ticked.contains(group), enabled: !info.making) { on in
                if on { ticked.formUnion([group]) } else { ticked.subtract([group]) }
            }
        }
        Text(L10n.tr("settings.keys.ai.groups.fixed"))
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
        TextField(L10n.tr("settings.keys.key.name"), text: $label, prompt: Text(L10n.tr("settings.keys.ai.key.example")))
            .onChange(of: label) { _, value in
                if value.count > info.labelMax { label = String(value.prefix(info.labelMax)) }
            }
        Button(L10n.tr("settings.keys.ai.make")) {
            store.makeKey(label, groups: info.offeredGroups.filter { ticked.contains($0) })
        }
        .disabled(label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || info.making)
    }

    private func copyText(_ text: String, _ done: String) {
        settingsKeysCopy(text)
        toast = done
    }
}
