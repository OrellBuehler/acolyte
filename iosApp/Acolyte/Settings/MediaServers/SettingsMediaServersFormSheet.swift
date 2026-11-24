import SwiftUI

struct SettingsMediaServersFormSheet: View {
    let store: SettingsMediaServersStore
    @Environment(\.openRoute) private var openRoute

    init(store: SettingsMediaServersStore) {
        self.store = store
    }

    private var info: SettingsMediaServersInfo { store.info }

    var body: some View {
        NavigationStack {
            Form {
                if let form = info.form {
                    fields(form)
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { store.cancelForm() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("settings.mediaservers.save")) { store.save() }
                            .disabled(!info.canSave)
                    }
                }
            }
        }
        .interactiveDismissDisabled(info.saving)
        .reauthSheet(store.reauth)
    }

    private var title: String {
        guard let form = info.form else { return "" }
        return form.isNew ? L10n.tr("settings.mediaservers.add") : L10n.format("settings.mediaservers.change.title", form.originalName)
    }

    @ViewBuilder
    private func fields(_ form: SettingsMediaServersFormInfo) -> some View {
        Section {
            SettingsMediaServersField(
                title: L10n.tr("settings.mediaservers.name"),
                value: form.name,
                revision: info.formRevision,
                onChange: { store.setName($0) },
                maxLength: 80
            )
            if form.isNew {
                Picker(L10n.tr("settings.mediaservers.kind"), selection: Binding(get: { form.kind }, set: { store.setKind($0) })) {
                    ForEach(info.kinds) { option in
                        Text(option.name).tag(option.wire)
                    }
                }
            }
        }
        if info.showsPlexPicker {
            plexSection(form)
        }
        Section {
            SettingsMediaServersField(
                title: L10n.tr("settings.mediaservers.address"),
                value: form.url,
                revision: info.formRevision,
                onChange: { store.setUrl($0) },
                placeholder: form.urlExample,
                mono: true,
                url: true
            )
            if form.takesUsername {
                SettingsMediaServersField(
                    title: L10n.tr("settings.mediaservers.user.name"),
                    value: form.username,
                    revision: info.formRevision,
                    onChange: { store.setUsername($0) },
                    mono: true
                )
            }
            if form.showsToken {
                SettingsMediaServersField(
                    title: secretTitle(form),
                    value: form.token,
                    revision: info.formRevision,
                    onChange: { store.setToken($0) },
                    hint: secretHint(form),
                    placeholder: form.tokenStored ? L10n.tr("settings.mediaservers.stored") : "",
                    secret: true
                )
            }
            if form.isSubsonic {
                SettingsMediaServersToggle(
                    title: L10n.tr("settings.mediaservers.plain"),
                    hint: L10n.tr("settings.mediaservers.plain.hint"),
                    isOn: Binding(get: { form.plainPassword }, set: { store.setPlainPassword($0) })
                )
            }
            if form.takesWebUrl {
                SettingsMediaServersField(
                    title: L10n.tr("settings.mediaservers.web.url"),
                    value: form.webUrl,
                    revision: info.formRevision,
                    onChange: { store.setWebUrl($0) },
                    hint: L10n.tr("settings.mediaservers.web.url.hint"),
                    placeholder: "https://jellyfin.example.com",
                    mono: true,
                    url: true
                )
            }
        }
        Section(L10n.tr("settings.mediaservers.after")) {
            SettingsMediaServersToggle(
                title: L10n.tr("settings.mediaservers.an.import"),
                isOn: Binding(get: { form.onImport }, set: { store.setOnImport($0) })
            )
            SettingsMediaServersToggle(
                title: L10n.tr("settings.mediaservers.a.rename"),
                isOn: Binding(get: { form.onRename }, set: { store.setOnRename($0) })
            )
            SettingsMediaServersToggle(
                title: L10n.tr("settings.mediaservers.a.delete"),
                hint: L10n.tr("settings.mediaservers.a.delete.hint"),
                isOn: Binding(get: { form.onDelete }, set: { store.setOnDelete($0) })
            )
        }
        if form.takesPathMap {
            Section {
                SettingsMediaServersField(
                    title: L10n.tr("settings.mediaservers.path.from"),
                    value: form.pathFrom,
                    revision: info.formRevision,
                    onChange: { store.setPathFrom($0) },
                    placeholder: "/data/movies",
                    mono: true
                )
                SettingsMediaServersField(
                    title: L10n.tr("settings.mediaservers.path.to"),
                    value: form.pathTo,
                    revision: info.formRevision,
                    onChange: { store.setPathTo($0) },
                    hint: L10n.tr("settings.mediaservers.path.hint"),
                    placeholder: "/movies",
                    mono: true
                )
            }
        }
        Section {
            SettingsMediaServersToggle(
                title: L10n.tr("settings.mediaservers.local"),
                isOn: Binding(get: { form.allowLocalNetwork }, set: { store.setAllowLocalNetwork($0) })
            )
            SettingsMediaServersToggle(
                title: L10n.tr("settings.mediaservers.send"),
                isOn: Binding(get: { form.enabled }, set: { store.setEnabled($0) })
            )
        }
        if let error = info.formError {
            Section {
                NoticeBanner(text: error.localized)
                if !info.formDetail.isEmpty {
                    Text(info.formDetail)
                        .font(.footnote)
                        .foregroundStyle(Theme.failText)
                }
            }
        }
    }

    private func plexRowText(_ row: SettingsMediaServersPlexRowInfo) -> String {
        [row.name, row.address, SettingsMediaServersWords.place(row.place)].joined(separator: " · ")
    }

    private func secretTitle(_ form: SettingsMediaServersFormInfo) -> String {
        if form.isPlex { return L10n.tr("settings.mediaservers.plex.token") }
        if form.takesUsername { return L10n.tr("settings.mediaservers.password") }
        return L10n.tr("settings.mediaservers.api.key")
    }

    private func secretHint(_ form: SettingsMediaServersFormInfo) -> String {
        if form.isPlex { return L10n.tr("settings.mediaservers.plex.token.hint") }
        if form.isSubsonic { return L10n.tr("settings.mediaservers.subsonic.hint") }
        if form.isKodi { return L10n.tr("settings.mediaservers.kodi.hint") }
        return L10n.tr("settings.mediaservers.api.key.hint")
    }

    @ViewBuilder
    private func plexSection(_ form: SettingsMediaServersFormInfo) -> some View {
        let plex = info.plex
        Section {
            HStack {
                Button(L10n.tr("settings.mediaservers.plex.find")) { store.findPlexServers() }
                    .disabled(plex.finding)
                if plex.finding {
                    ProgressView()
                }
                if plex.searched {
                    Text(L10n.format("settings.mediaservers.plex.found", plex.rows.count))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            if plex.notLinked {
                Text(L10n.tr("settings.mediaservers.plex.not.linked"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Button(L10n.tr("settings.mediaservers.plex.open.sign.in")) {
                    store.cancelForm()
                    openRoute(.settingsSignIn)
                }
            }
            if let error = plex.error {
                NoticeBanner(text: error.localized)
            }
            if !plex.rows.isEmpty {
                Picker(L10n.tr("settings.mediaservers.plex.pick"), selection: Binding(get: { plex.picked }, set: { store.pickPlex($0) })) {
                    ForEach(plex.rows) { row in
                        Text(verbatim: plexRowText(row)).tag(row.index)
                    }
                    Text(L10n.tr("settings.mediaservers.plex.manual")).tag(-1)
                }
                if plex.addresses.count > 1 {
                    Picker(L10n.tr("settings.mediaservers.plex.address"), selection: Binding(get: { form.url }, set: { store.pickPlexAddress($0) })) {
                        ForEach(plex.addresses, id: \.self) { address in
                            Text(address).tag(address)
                        }
                    }
                }
                if form.usePlexSignIn {
                    Text(L10n.tr("settings.mediaservers.plex.uses.sign.in"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }
}
