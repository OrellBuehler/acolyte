import SwiftUI

struct SettingsAiChangeSheet: View {
    let store: SettingsAiStore
    let key: SettingsAiKeyInfo
    let opened: Int
    @State private var ticked: Set<String>
    @Environment(\.dismiss) private var dismiss

    init(store: SettingsAiStore, key: SettingsAiKeyInfo, offered: [String], opened: Int) {
        self.store = store
        self.key = key
        self.opened = opened
        _ticked = State(initialValue: Set(key.groups.filter { offered.contains($0) }))
    }

    private var info: SettingsAiInfo { store.info }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(L10n.format("settings.keys.ai.change.title", key.label))
                        .font(.headline)
                    Text(L10n.tr("settings.keys.ai.change.text"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    if info.changeNeedsSession {
                        Text(L10n.tr("settings.keys.ai.change.needs.session"))
                            .font(.footnote)
                            .foregroundStyle(Theme.failText)
                    }
                }
                Section {
                    ForEach(info.offeredGroups, id: \.self) { group in
                        SettingsKeysGroupToggle(
                            group: group,
                            isOn: ticked.contains(group),
                            enabled: !info.changing
                        ) { on in
                            if on { ticked.formUnion([group]) } else { ticked.subtract([group]) }
                        }
                    }
                }
                if let error = info.changeError {
                    Section {
                        Text(failure(error))
                            .foregroundStyle(Theme.failText)
                    }
                }
            }
            .navigationTitle(L10n.tr("settings.keys.ai.change"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("settings.keys.keep")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.changing {
                        ProgressView()
                    } else {
                        Button(L10n.tr("settings.keys.save")) {
                            store.changeGroups(key.id, groups: info.offeredGroups.filter { ticked.contains($0) })
                        }
                        .disabled(info.changeNeedsSession)
                    }
                }
            }
            .reauthSheet(store.reauth)
        }
        .presentationDetents([.large])
        .onChange(of: info.changedCount) { _, count in
            if count > opened { dismiss() }
        }
    }

    private func failure(_ error: ErrorInfo) -> String {
        [L10n.tr("settings.keys.ai.change.failed"), error.localized, info.changeDetail]
            .filter { !$0.isEmpty }
            .joined(separator: "\n")
    }
}
