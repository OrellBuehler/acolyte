import SwiftUI

struct BridgeFormTarget: Identifiable {
    let token: Int
    let initial: BridgeDraftInfo

    var id: Int { token }
}

struct BridgeSection: View {
    static let anchor = "bridge-card"

    let store: BridgeStore
    let onUnlink: (BridgeRowInfo) -> Void

    private var info: BridgesInfo { store.info }

    var body: some View {
        Section {
            if info.bridges.isEmpty {
                Text(L10n.tr("acq.src.bridge.description"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .id(Self.anchor)
            }
            if info.load.phase == .failed, let error = info.load.error {
                VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    Text(error.localized).font(.footnote).foregroundStyle(Theme.failText)
                    Button(L10n.tr("acq.src.retry")) { Task { await store.refresh() } }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                }
            }
            ForEach(info.bridges) { bridge in
                BridgeRowView(bridge: bridge, canEdit: info.canEdit, syncing: info.syncing == bridge.id, store: store, onUnlink: onUnlink)
                    .id(info.bridges.first?.id == bridge.id ? Self.anchor : "bridge-\(bridge.id)")
            }
            if info.canEdit && info.form == nil && info.load.phase == .ready {
                Button {
                    store.startConnect("jackett")
                } label: {
                    Label(
                        L10n.tr(info.bridges.isEmpty ? "acq.src.bridge.link" : "acq.src.bridge.add.another"),
                        systemImage: "plus"
                    )
                }
            }
        } header: {
            Text(L10n.tr("acq.src.bridge.title"))
        }
    }
}

struct BridgeRowView: View {
    let bridge: BridgeRowInfo
    let canEdit: Bool
    let syncing: Bool
    let store: BridgeStore
    let onUnlink: (BridgeRowInfo) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack {
                Text(SourcesWords.bridgeName(bridge.kind)).font(.headline)
                Spacer()
                if bridge.lastError.isEmpty {
                    Text(L10n.plural("acq.src.bridge.sources", bridge.sources))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                } else {
                    Text(L10n.tr("acq.src.bridge.not.connected")).font(.footnote).foregroundStyle(Theme.failText)
                }
            }
            Text(bridge.url).font(.footnote).foregroundStyle(Theme.textMuted)
            if let ago = SourcesWords.ago(bridge.lastCheck) {
                Text(L10n.format("acq.src.bridge.checked", ago)).font(.caption).foregroundStyle(Theme.textFaint)
            }
            if !bridge.lastError.isEmpty {
                Text(bridge.lastError).font(.footnote).foregroundStyle(Theme.failText)
            }
            if canEdit {
                HStack(spacing: Theme.Spacing.small) {
                    Button(L10n.tr("acq.src.bridge.sync")) { store.sync(bridge.id) }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .disabled(syncing)
                    Button(L10n.tr("acq.src.bridge.change")) { store.startChange(bridge.id) }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                    Button(L10n.tr("acq.src.bridge.unlink"), role: .destructive) { onUnlink(bridge) }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                }
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

struct BridgeFormSheet: View {
    let target: BridgeFormTarget
    let store: BridgeStore
    @State private var draft: BridgeDraftInfo
    @State private var callbackOpen = false
    @State private var password = ""
    @State private var code = ""

    init(target: BridgeFormTarget, store: BridgeStore) {
        self.target = target
        self.store = store
        _draft = State(initialValue: target.initial)
    }

    private var form: BridgeFormInfo? { store.info.form }

    var body: some View {
        NavigationStack {
            Form {
                if let form {
                    fields(form)
                    if let result = form.result { resultSection(result) }
                    if let error = form.error {
                        Section { Text(error.sourceText).foregroundStyle(Theme.failText) }
                    }
                    if form.needsReauth { reauth(form) }
                }
            }
            .navigationTitle(L10n.tr("acq.src.bridge.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.src.cancel")) { store.cancelForm() }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button(L10n.tr("acq.src.bridge.test")) { store.test(draft) }
                        .disabled(!draft.canSubmit || (form?.busy ?? true))
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr(draft.editingId != 0 ? "acq.src.save" : "acq.src.bridge.link")) { store.link(draft) }
                        .disabled(!draft.canSubmit || (form?.busy ?? true))
                }
            }
        }
    }

    @ViewBuilder
    private func fields(_ form: BridgeFormInfo) -> some View {
        Section {
            Picker(L10n.tr("acq.src.bridge.app"), selection: $draft.kind) {
                Text("Prowlarr").tag("prowlarr")
                Text("Jackett").tag("jackett")
            }
            .pickerStyle(.segmented)
            if !form.kindError.isEmpty {
                Text(form.kindError).font(.footnote).foregroundStyle(Theme.failText)
            }
        }
        Section {
            VStack(alignment: .leading, spacing: 4) {
                TextField(L10n.tr("acq.src.bridge.address"), text: $draft.url, prompt: Text("http://192.168.1.10:" + draft.defaultPort))
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                if !form.urlError.isEmpty {
                    Text(form.urlError).font(.footnote).foregroundStyle(Theme.failText)
                } else if !draft.isProwlarr {
                    Text(L10n.tr("acq.src.bridge.base.path")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            VStack(alignment: .leading, spacing: 4) {
                SecureField(L10n.tr("acq.src.bridge.key"), text: $draft.key)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Text(keyHint(form))
                    .font(.footnote)
                    .foregroundStyle(form.keyError.isEmpty ? Theme.textMuted : Theme.failText)
            }
        }
        if draft.isProwlarr {
            callback(form)
            if !form.keysNeeded.isEmpty { keys(form) }
        }
    }

    private func keyHint(_ form: BridgeFormInfo) -> String {
        if !form.keyError.isEmpty { return form.keyError }
        if form.initial.editingId != 0 { return L10n.tr("acq.src.bridge.key.again") }
        return L10n.tr(draft.isProwlarr ? "acq.src.bridge.key.prowlarr" : "acq.src.bridge.key.jackett")
    }

    @ViewBuilder
    private func callback(_ form: BridgeFormInfo) -> some View {
        Section {
            if !draft.callbackUrl.isEmpty && !callbackOpen && form.callbackError.isEmpty {
                LabeledContent(L10n.tr("acq.src.bridge.reaches")) {
                    Text(draft.callbackUrl).font(.footnote).multilineTextAlignment(.trailing)
                }
                Button(L10n.tr("acq.src.bridge.change")) { callbackOpen = true }
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    TextField(L10n.tr("acq.src.bridge.callback"), text: $draft.callbackUrl)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                    Text(form.callbackError.isEmpty ? L10n.tr("acq.src.bridge.callback.hint") : form.callbackError)
                        .font(.footnote)
                        .foregroundStyle(form.callbackError.isEmpty ? Theme.textMuted : Theme.failText)
                }
            }
        }
    }

    @ViewBuilder
    private func keys(_ form: BridgeFormInfo) -> some View {
        Section {
            Toggle(L10n.tr("acq.src.bridge.replace.keys"), isOn: $draft.newKeys)
            if !draft.newKeys {
                Text(L10n.tr(form.keysNeeded.count == 1 ? "acq.src.bridge.current.key" : "acq.src.bridge.current.keys"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                if form.keysNeeded.contains("sonarr") {
                    SecureField(L10n.tr("acq.src.bridge.tv.key"), text: $draft.sonarrKey)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
                if form.keysNeeded.contains("radarr") {
                    SecureField(L10n.tr("acq.src.bridge.movie.key"), text: $draft.radarrKey)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            }
        }
    }

    private func resultSection(_ result: BridgeResultInfo) -> some View {
        let name = SourcesWords.bridgeName(draft.kind)
        let head = result.version.isEmpty ? L10n.format("acq.src.bridge.answered", name) : name + " " + result.version
        return Section {
            Text(head).font(.headline).foregroundStyle(Theme.okText)
            if result.siteCount > 0 {
                Text(L10n.plural("acq.src.bridge.indexers", result.siteCount)).font(.footnote).foregroundStyle(Theme.textMuted)
                Text(result.sites.joined(separator: ", ") + (result.siteCount > result.sites.count ? "…" : ""))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }

    @ViewBuilder
    private func reauth(_ form: BridgeFormInfo) -> some View {
        Section {
            if form.canReauth {
                Text(L10n.tr("acq.src.reauth.text")).font(.footnote).foregroundStyle(Theme.textMuted)
                SecureField(L10n.tr("acq.src.reauth.password"), text: $password)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField(L10n.tr("acq.src.reauth.code"), text: $code)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.numbersAndPunctuation)
                if let error = form.reauthError {
                    Text(error.localized).font(.footnote).foregroundStyle(Theme.failText)
                }
                Button(L10n.tr("acq.src.reauth.confirm")) { store.confirmReauth(password, code) }
                    .disabled(password.isEmpty || form.reauthBusy)
                Button(L10n.tr("acq.src.cancel"), role: .cancel) { store.cancelReauth() }
            } else {
                NoticeBanner(text: L10n.tr("acq.src.bridge.set.password"))
            }
        }
    }
}

struct BridgeUnlinkSheet: View {
    let bridge: BridgeRowInfo
    let store: BridgeStore
    let onClose: () -> Void
    @State private var keep = false

    private var isJackett: Bool { bridge.kind == "jackett" }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(bridge.url).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                Section {
                    if isJackett {
                        Picker("", selection: $keep) {
                            Text(L10n.plural("acq.src.bridge.remove.its", bridge.sources)).tag(false)
                            Text(L10n.tr("acq.src.bridge.keep.stop")).tag(true)
                        }
                        .pickerStyle(.inline)
                        .labelsHidden()
                    } else {
                        Text(L10n.plural("acq.src.bridge.removes.prowlarr", bridge.sources)).font(.subheadline)
                        Toggle(L10n.tr("acq.src.bridge.unlink.here"), isOn: $keep)
                    }
                }
                if let error = store.info.removeError {
                    Section { Text(error.sourceText).foregroundStyle(Theme.failText) }
                }
            }
            .navigationTitle(L10n.format("acq.src.bridge.unlink.title", SourcesWords.bridgeName(bridge.kind)))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.src.cancel")) { onClose() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("acq.src.bridge.unlink"), role: .destructive) {
                        let keepSources = keep
                        let id = bridge.id
                        Task {
                            if await store.unlink(id, keepSources: keepSources) { onClose() }
                        }
                    }
                    .disabled(store.info.removing)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onAppear { store.clearRemoveError() }
    }
}

struct BridgeKeysSheet: View {
    let store: BridgeStore

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(L10n.tr("acq.src.bridge.made.keys.text")).font(.subheadline)
                }
                ForEach(store.info.madeKeys) { entry in
                    Section(L10n.tr(entry.side == "sonarr" ? "acq.src.bridge.tv.key" : "acq.src.bridge.movie.key")) {
                        Text(entry.key).font(.footnote.monospaced()).textSelection(.enabled)
                    }
                }
            }
            .navigationTitle(L10n.tr("acq.src.bridge.made.keys"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("acq.src.close")) { store.dismissKeys() }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
