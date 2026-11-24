import SwiftUI

struct SettingsSignInView: View {
    @State private var store = SettingsSignInStore()
    @State private var advanced = false
    @State private var confirmUnlink = false
    @State private var toast: String?
    @Environment(\.openRoute) private var openRoute
    @Environment(\.openURL) private var openURL

    private var info: SettingsSignInInfo { store.info }

    var body: some View {
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            form
        }
        .navigationTitle(L10n.tr("settings.page.sign.in"))
        .settingsReauth(
            info.reauth,
            confirm: { password, code in store.confirmReauth(password, code) },
            cancel: { store.cancelReauth() }
        )
        .sheet(isPresented: importPresented) {
            SettingsSignInImportView(store: store)
        }
        .alert(L10n.tr("settings.signin.everyone.title"), isPresented: everyonePresented) {
            Button(L10n.tr("settings.signin.cancel"), role: .cancel) { store.dismissEveryone() }
            Button(L10n.tr("settings.signin.everyone.confirm")) {
                if let ask = info.everyone { store.saveWay(ask.kind, confirmed: true) }
            }
        } message: {
            if let ask = info.everyone { Text(SettingsSignInWords.everyone(ask)) }
        }
        .alert(L10n.tr("settings.signin.unlink.title"), isPresented: $confirmUnlink) {
            Button(L10n.tr("settings.signin.cancel"), role: .cancel) {}
            Button(L10n.tr("settings.signin.unlink"), role: .destructive) { store.unlinkPlex() }
        } message: {
            Text(L10n.tr("settings.signin.unlink.text"))
        }
        .safeAreaInset(edge: .bottom) {
            if let toast {
                NoteToast(text: toast, undo: nil)
            }
        }
        .onChange(of: info.notice) { _, notice in
            guard let notice else { return }
            show(SettingsSignInWords.notice(notice), seconds: notice.kind == .wayunreachable ? 10 : 4)
            store.dismissNotice(notice.seq)
        }
        .onChange(of: info.plex.openCount) { old, new in
            guard new > old, let url = URL(string: info.plex.authUrl) else { return }
            openURL(url)
        }
        .onDisappear { store.stopLinking() }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var importPresented: Binding<Bool> {
        Binding(get: { store.info.importSheet != nil }, set: { if !$0 { store.closeImport() } })
    }

    private var everyonePresented: Binding<Bool> {
        Binding(get: { store.info.everyone != nil }, set: { if !$0 { store.dismissEveryone() } })
    }

    @MainActor
    private func show(_ text: String, seconds: Int) {
        toast = text
        Task {
            try? await Task.sleep(for: .seconds(seconds))
            if toast == text { toast = nil }
        }
    }

    private var form: some View {
        Form {
            if info.apiKey {
                Section { SettingsSignInNote(text: L10n.tr("settings.signin.api.key.banner"), warn: false) }
            }
            if let error = info.load.error {
                Section { NoticeBanner(text: error.localized) }
            }
            SettingsSignInPasswordsSection(store: store)
            SettingsSignInSwitchSections(store: store)
            ForEach(SettingsSignInWayID.allCases) { id in
                SettingsSignInWaySection(store: store, id: id, confirmUnlink: $confirmUnlink)
            }
            SettingsSignInSsoSection(store: store) {
                show(L10n.tr("settings.signin.sso.copied"), seconds: 3)
            }
            if info.showLocal {
                SettingsSignInLocalSection(store: store)
            }
            Section {
                Button {
                    openRoute(.settingsApi)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(L10n.tr("settings.signin.api.keys"))
                                .foregroundStyle(Theme.text)
                            Text(L10n.tr("settings.signin.api.keys.note"))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(Theme.textFaint)
                    }
                }
            }
            advancedSection
        }
    }

    private var advancedSection: some View {
        Section {
            DisclosureGroup(L10n.tr("settings.signin.advanced"), isExpanded: $advanced) {
                VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    Text(L10n.tr("settings.signin.hosts"))
                        .font(.subheadline.weight(.semibold))
                    SettingsSignInHint(text: L10n.tr("settings.signin.hosts.hint"))
                    SettingsSignInArea(
                        title: L10n.tr("settings.signin.host.names"),
                        text: info.hosts.text,
                        onChange: { store.setHosts($0) },
                        disabled: !info.isOwner || info.apiKey
                    )
                    if info.apiKey {
                        SettingsSignInHint(text: L10n.tr("settings.signin.hosts.needs.session"))
                    }
                    if info.isOwner {
                        HStack(spacing: Theme.Spacing.medium) {
                            Button(L10n.tr("settings.signin.save")) { store.saveHosts() }
                                .buttonStyle(.borderedProminent)
                                .disabled(info.apiKey || info.hosts.saving)
                            if info.hosts.saving { ProgressView() }
                        }
                    } else {
                        SettingsSignInHint(text: L10n.tr("settings.signin.owner.only"))
                    }
                }
                VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    Text(L10n.tr("settings.signin.proxies"))
                        .font(.subheadline.weight(.semibold))
                    SettingsSignInHint(text: L10n.tr("settings.signin.proxies.info"))
                }
            }
        }
    }
}
