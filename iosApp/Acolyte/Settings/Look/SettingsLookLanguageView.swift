import SwiftUI

struct SettingsLookLanguageRow: Hashable, Identifiable {
    let code: String
    let name: String

    var id: String { code }
}

struct SettingsLookLanguageView: View {
    let store: SettingsLookAppearanceStore
    @State private var query = ""
    @Environment(\.dismiss) private var dismiss

    private var info: SettingsLookAppearanceInfo { store.info }

    private var automatic: String { L10n.tr("settings.look.appearance.language.automatic") }

    private var needle: String { query.trimmingCharacters(in: .whitespaces).lowercased() }

    private func matches(_ row: SettingsLookLanguageRow) -> Bool {
        needle.isEmpty || row.code.contains(needle) || row.name.lowercased().contains(needle)
    }

    private var mostUsed: [SettingsLookLanguageRow] {
        info.mostUsedLanguages.map { SettingsLookLanguageRow(code: $0, name: SettingsLookWords.languageName($0)) }.filter(matches)
    }

    private var more: [SettingsLookLanguageRow] {
        let rows = info.otherLanguages.map { SettingsLookLanguageRow(code: $0, name: SettingsLookWords.languageName($0)) }
        return rows.filter(matches).sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
    }

    private var showAutomatic: Bool {
        needle.isEmpty || automatic.lowercased().contains(needle)
    }

    private func pick(_ code: String) {
        store.setLanguage(code)
        dismiss()
    }

    private func row(_ name: String, selected: Bool, code: String) -> some View {
        Button {
            pick(code)
        } label: {
            HStack {
                Text(name)
                    .foregroundStyle(selected ? Theme.accentText : Theme.text)
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .foregroundStyle(Theme.accentText)
                }
            }
        }
    }

    var body: some View {
        let most = mostUsed
        let rest = more
        List {
            if showAutomatic {
                row(automatic, selected: info.language.isEmpty, code: "")
            }
            if !most.isEmpty {
                Section(L10n.tr("settings.look.appearance.language.most.used")) {
                    ForEach(most) { item in
                        row(item.name, selected: item.code == info.language, code: item.code)
                    }
                }
            }
            if !rest.isEmpty {
                Section(L10n.tr("settings.look.appearance.language.more")) {
                    ForEach(rest) { item in
                        row(item.name, selected: item.code == info.language, code: item.code)
                    }
                }
            }
            if !showAutomatic && most.isEmpty && rest.isEmpty {
                Text(L10n.tr("settings.look.appearance.language.none"))
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .searchable(text: $query, prompt: L10n.tr("settings.look.appearance.language.search"))
        .navigationTitle(L10n.tr("settings.look.appearance.language.title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}
