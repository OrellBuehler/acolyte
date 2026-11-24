import SwiftUI

struct SettingsLookMetadataScreen: View {
    @State private var store = SettingsLookMetadataStore()
    @State private var savedText: String?

    private var info: SettingsLookMetadataInfo { store.info }

    var body: some View {
        StatusContent(load: info.load, isEmpty: !info.allowed, reload: { await store.refresh() }) {
            EmptyStateView(
                title: L10n.tr("settings.look.not.allowed"),
                message: L10n.tr("settings.look.not.allowed.text"),
                systemImage: "lock"
            )
        } content: {
            form
        }
        .navigationTitle(L10n.tr("settings.page.metadata"))
        .settingsLookLeaveGuard(editing: info.editing)
        .safeAreaInset(edge: .bottom) {
            if info.editing {
                SettingsLookSaveBar(
                    canSave: info.canSave,
                    saving: info.saving,
                    error: info.saveError,
                    problem: nil,
                    discard: { store.discard() },
                    save: { store.save() }
                )
            } else if let savedText {
                NoteToast(text: savedText, undo: nil)
            }
        }
        .onChange(of: info.modeSavedCount) { _, count in
            if count > 0 { toast(L10n.tr("settings.look.metadata.mode.saved")) }
        }
        .onChange(of: info.keysSavedCount) { _, count in
            if count > 0 { toast(L10n.tr("settings.look.metadata.keys.saved")) }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private func toast(_ text: String) {
        savedText = text
        Task {
            try? await Task.sleep(for: .seconds(3))
            savedText = nil
        }
    }

    private var form: some View {
        Form {
            modeSection
            if info.tvdbClosed {
                tvdbSection
            }
            if !info.shownKeys.isEmpty {
                keysSection
            }
            if !info.offSources.isEmpty {
                offSection
            }
            if info.fullMode {
                serverSection
                sentSection
            }
            neverSection
            if !info.hosts.isEmpty {
                hostsSection
            }
        }
    }

    private func modeRow(_ mode: String, name: String, about: String) -> some View {
        let selected = info.mode == mode
        return Button {
            store.chooseMode(mode)
        } label: {
            HStack(spacing: Theme.Spacing.medium) {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(name)
                        .font(.headline)
                        .foregroundStyle(Theme.text)
                    SettingsLookHint(about)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: selected ? "largecircle.fill.circle" : "circle")
                    .foregroundStyle(selected ? Theme.accentText : Theme.textFaint)
            }
        }
        .disabled(!info.canChooseMode)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private var modeSection: some View {
        Section {
            if !info.dockerValue.isEmpty {
                Text(L10n.format("settings.look.metadata.docker.value", info.dockerValue))
                    .font(.callout)
                    .foregroundStyle(Theme.failText)
            }
            modeRow(
                "full",
                name: L10n.tr("settings.look.metadata.mode.full"),
                about: L10n.tr("settings.look.metadata.mode.full.about")
            )
            modeRow(
                "own",
                name: L10n.tr("settings.look.metadata.mode.own"),
                about: L10n.tr("settings.look.metadata.mode.own.about")
            )
            modeRow(
                "off",
                name: L10n.tr("settings.look.metadata.mode.off"),
                about: L10n.tr("settings.look.metadata.mode.off.about")
            )
            if info.locked {
                Label(L10n.tr("settings.look.metadata.locked"), systemImage: "key")
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if info.ownMode {
                SettingsLookHint(L10n.tr("settings.look.metadata.own.note"))
            }
            if let error = info.modeError {
                SettingsLookErrorText(error: error)
            }
        } header: {
            Text(L10n.tr("settings.look.metadata.mode.title"))
        }
    }

    private var tvdbSection: some View {
        Section {
            Button(L10n.tr("settings.look.metadata.tvdb.use")) { store.openTvdb() }
        } header: {
            Text(L10n.tr("settings.look.metadata.tvdb.title"))
        } footer: {
            Text(L10n.tr("settings.look.metadata.tvdb.desc"))
        }
    }

    private var keysSection: some View {
        Section {
            ForEach(info.shownKeys) { key in
                keyRow(key)
            }
        } header: {
            Text(L10n.tr("settings.look.metadata.keys.title"))
        } footer: {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                if info.ownMode {
                    Text(L10n.tr("settings.look.metadata.keys.desc"))
                }
                Text(info.ownMode ? L10n.tr("settings.look.metadata.keys.kept.own") : L10n.tr("settings.look.metadata.keys.kept"))
            }
        }
    }

    private func keyRow(_ key: SettingsLookKeyInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(SettingsLookWords.keyLabel(key.name))
                .font(.subheadline.weight(.semibold))
                .accessibilityHidden(true)
            SecureField(
                SettingsLookWords.keyLabel(key.name),
                text: Binding(get: { key.shownText }, set: { store.setKey(key.name, $0) }),
                prompt: Text(keyPrompt(key))
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .disabled(key.fromDocker)
            SettingsLookHint(SettingsLookWords.keyHint(key, ownMode: info.ownMode))
            if !key.fromDocker {
                keyActions(key)
            }
            if let result = key.result {
                Text(SettingsLookWords.testText(result))
                    .font(.footnote)
                    .foregroundStyle(result.ok ? Theme.okText : Theme.failText)
            }
        }
    }

    private func keyPrompt(_ key: SettingsLookKeyInfo) -> String {
        if key.removing { return L10n.tr("settings.look.metadata.key.removing") }
        if key.stored { return L10n.tr("settings.look.metadata.key.stored") }
        return ""
    }

    private func keyActions(_ key: SettingsLookKeyInfo) -> some View {
        HStack(spacing: Theme.Spacing.medium) {
            if !key.isPin {
                Button(key.testing ? L10n.tr("settings.look.metadata.testing") : L10n.tr("settings.look.metadata.test")) {
                    store.testKey(key.name)
                }
                .buttonStyle(.bordered)
                .disabled(key.testing)
            }
            if key.removing {
                Button(L10n.tr("settings.look.metadata.key.undo")) { store.restoreKey(key.name) }
                    .buttonStyle(.borderless)
            } else if key.saved {
                Button(L10n.tr("settings.look.metadata.key.remove")) { store.clearKey(key.name) }
                    .buttonStyle(.borderless)
            }
            if let url = URL(string: key.link), !key.link.isEmpty {
                Link(L10n.tr("settings.look.metadata.get.key"), destination: url)
                    .font(.subheadline)
            }
        }
    }

    private var offSection: some View {
        Section {
            ForEach(info.offSources) { source in
                HStack(alignment: .firstTextBaseline) {
                    Text(SettingsLookWords.sourceName(source))
                    Spacer()
                    Text(source.state == "needs_key" ? L10n.tr("settings.look.metadata.needs.key") : L10n.tr("settings.look.metadata.state.off"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        } header: {
            Text(L10n.tr("settings.look.metadata.off.title"))
        } footer: {
            Text(L10n.tr("settings.look.metadata.off.desc"))
        }
    }

    private var healthTitle: String {
        if info.health == nil {
            return info.healthError == nil ? L10n.tr("settings.look.metadata.checking") : L10n.tr("settings.look.metadata.health.unknown")
        }
        return info.health?.up == false ? L10n.tr("settings.look.metadata.not.answering") : L10n.tr("settings.look.metadata.connected")
    }

    private var healthHint: String? {
        guard let health = info.health, !health.up else { return nil }
        if health.fakeHost.isEmpty { return L10n.tr("settings.look.metadata.down.text") }
        return L10n.format("settings.look.metadata.fake.ip", health.fakeHost)
    }

    private var serverSection: some View {
        Section {
            HStack {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(healthTitle)
                    if let hint = healthHint {
                        SettingsLookHint(hint)
                    }
                }
                Spacer()
                if let health = info.health {
                    StatusChip(
                        text: health.up ? L10n.tr("settings.look.metadata.answering") : L10n.tr("settings.look.metadata.down"),
                        tone: health.up ? .ok : .fail
                    )
                }
            }
            if let error = info.healthError {
                SettingsLookErrorText(error: error)
            }
            Button {
                store.checkAgain()
            } label: {
                if info.checking {
                    ProgressView()
                } else {
                    Text(L10n.tr("settings.look.metadata.check.again"))
                }
            }
            .disabled(info.checking)
        } header: {
            Text(L10n.tr("settings.look.metadata.server"))
        }
    }

    private func sentRow(_ lead: String, _ text: String) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(lead)
                .font(.subheadline.weight(.semibold))
            SettingsLookHint(text)
        }
    }

    private var sentSection: some View {
        Section {
            sentRow(L10n.tr("settings.look.metadata.sent.token"), L10n.tr("settings.look.metadata.sent.token.text"))
            sentRow(L10n.tr("settings.look.metadata.sent.lookup"), L10n.tr("settings.look.metadata.sent.lookup.text"))
            sentRow(L10n.tr("settings.look.metadata.sent.display"), L10n.tr("settings.look.metadata.sent.display.text"))
            sentRow(L10n.tr("settings.look.metadata.sent.headers"), L10n.tr("settings.look.metadata.sent.headers.text"))
            sentRow(L10n.tr("settings.look.metadata.sent.country"), L10n.tr("settings.look.metadata.sent.country.text"))
        } header: {
            Text(L10n.tr("settings.look.metadata.sent.title"))
        } footer: {
            Text(L10n.tr("settings.look.metadata.sent.desc"))
        }
    }

    private var neverSection: some View {
        Section {
            Text(L10n.tr("settings.look.metadata.never.1"))
            Text(L10n.tr("settings.look.metadata.never.2"))
            Text(L10n.tr("settings.look.metadata.never.3"))
            Text(L10n.tr("settings.look.metadata.never.4"))
        } header: {
            Text(L10n.tr("settings.look.metadata.never.title"))
        }
    }

    private var hostsSection: some View {
        Section {
            ForEach(info.hosts, id: \.self) { host in
                Text(host)
                    .font(.body.monospaced())
            }
        } header: {
            Text(L10n.tr("settings.look.metadata.hosts.title"))
        } footer: {
            Text(L10n.tr("settings.look.metadata.hosts.desc"))
        }
    }
}
