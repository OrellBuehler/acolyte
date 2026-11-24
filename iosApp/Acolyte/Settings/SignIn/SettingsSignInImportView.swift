import SwiftUI

struct SettingsSignInImportView: View {
    let store: SettingsSignInStore
    @State private var name = ""
    @State private var password = ""

    var body: some View {
        NavigationStack {
            Group {
                if let sheet = store.info.importSheet {
                    content(sheet)
                } else {
                    EmptyView()
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("settings.signin.cancel")) { store.closeImport() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if let sheet = store.info.importSheet, sheet.phase == .ready, !sheet.people.isEmpty {
                        if sheet.busy {
                            ProgressView()
                        } else {
                            Button(L10n.format("settings.signin.import.n", sheet.pickedCount)) {
                                store.runImport(confirmAdults: false)
                            }
                            .disabled(sheet.pickedCount == 0)
                        }
                    }
                }
            }
        }
        .presentationDetents([.large])
        .alert(
            L10n.tr("settings.signin.adult.title"),
            isPresented: Binding(
                get: { !(store.info.importSheet?.confirmAdults.isEmpty ?? true) },
                set: { if !$0 { store.dismissConfirmAdults() } }
            )
        ) {
            Button(L10n.tr("settings.signin.cancel"), role: .cancel) { store.dismissConfirmAdults() }
            Button(L10n.tr("settings.signin.adult.confirm")) { store.runImport(confirmAdults: true) }
        } message: {
            Text(L10n.format("settings.signin.adult.text", (store.info.importSheet?.confirmAdults ?? []).joined(separator: ", ")))
        }
    }

    private var title: String {
        guard let sheet = store.info.importSheet else { return L10n.tr("settings.signin.import.members") }
        return L10n.format("settings.signin.import.from", sheet.kind.app)
    }

    @ViewBuilder
    private func content(_ sheet: SettingsSignInImportInfo) -> some View {
        switch sheet.phase {
        case .ask:
            if sheet.kind == .plex {
                plexRetry(sheet)
            } else {
                adminForm(sheet)
            }
        case .loading:
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .ready:
            people(sheet)
        }
    }

    private func plexRetry(_ sheet: SettingsSignInImportInfo) -> some View {
        Form {
            Section {
                if let error = sheet.error {
                    SettingsSignInFail(text: SettingsSignInWords.failure(error))
                }
                Button(L10n.tr("settings.signin.try.again")) { store.previewImport("", "") }
            }
        }
    }

    private func adminForm(_ sheet: SettingsSignInImportInfo) -> some View {
        Form {
            Section {
                SettingsSignInHint(text: L10n.format("settings.signin.import.intro", sheet.kind.app))
                TextField(L10n.tr("settings.signin.admin.name"), text: $name)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                SecureField(L10n.tr("settings.signin.admin.pass"), text: $password)
                if let error = sheet.error {
                    SettingsSignInFail(text: SettingsSignInWords.failure(error))
                }
                Button(L10n.tr("settings.signin.show.people")) {
                    let typedName = name
                    let typedPassword = password
                    password = ""
                    store.previewImport(typedName, typedPassword)
                }
                .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
    }

    private func people(_ sheet: SettingsSignInImportInfo) -> some View {
        Form {
            if sheet.people.isEmpty {
                Section { SettingsSignInHint(text: L10n.tr("settings.signin.no.one")) }
            } else {
                Section {
                    ForEach(sheet.people) { person in
                        row(person)
                    }
                } footer: {
                    Text(L10n.tr("settings.signin.import.note"))
                }
            }
            if let error = sheet.error {
                Section { SettingsSignInFail(text: SettingsSignInWords.failure(error)) }
            }
        }
    }

    private func row(_ person: SettingsSignInCandidateInfo) -> some View {
        HStack(spacing: Theme.Spacing.medium) {
            Button {
                store.togglePerson(person.id)
            } label: {
                HStack(spacing: Theme.Spacing.medium) {
                    Image(systemName: person.picked ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(person.picked ? Theme.accentText : Theme.textFaint)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(person.name)
                            .foregroundStyle(Theme.text)
                        if let detail = detail(person) {
                            Text(detail)
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                        if person.serverAdmin {
                            StatusChip(text: L10n.tr("settings.signin.server.admin"), tone: .idle)
                        }
                    }
                }
            }
            .buttonStyle(.plain)
            .disabled(person.locked)
            Spacer(minLength: 0)
            Toggle(L10n.tr("settings.signin.kid"), isOn: Binding(get: { person.asKid }, set: { _ in store.toggleKid(person.id) }))
                .toggleStyle(.switch)
                .fixedSize()
                .disabled(person.locked || !person.picked)
        }
    }

    private func detail(_ person: SettingsSignInCandidateInfo) -> String? {
        var parts: [String] = []
        if !person.email.isEmpty { parts.append(person.email) }
        if !person.linkedTo.isEmpty { parts.append(L10n.format("settings.signin.already", person.linkedTo)) }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}
