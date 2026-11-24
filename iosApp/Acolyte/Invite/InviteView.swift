import SwiftUI

struct InviteView: View {
    @State private var store: InviteStore
    @State private var mediaWay: SignInWayKind?
    @State private var name = ""
    @State private var password = ""
    @State private var confirm = ""
    @State private var mediaName = ""
    @State private var mediaPassword = ""
    @Environment(\.dismiss) private var dismiss

    init(target: InviteTargetInfo) {
        _store = State(initialValue: InviteStore(target: target))
    }

    var body: some View {
        let info = store.info
        NavigationStack {
            ScrollView {
                VStack(spacing: Theme.Spacing.large) {
                    FormHeading(title: L10n.tr("invite.title"), subtitle: info.serverName.isEmpty ? nil : info.serverName)
                    content(info)
                    if let error = info.error, info.phase != .gone {
                        Text(error.localized)
                            .font(.callout)
                            .foregroundStyle(Theme.failText)
                            .multilineTextAlignment(.center)
                    }
                }
                .frame(maxWidth: 480)
                .padding(Theme.Spacing.xLarge)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Theme.background)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("invite.close")) { dismiss() }
                }
            }
        }
        .plexBrowser(url: info.plexURL)
        .task { await store.watch() }
        .task { await store.load() }
        .onDisappear { store.close() }
    }

    @ViewBuilder
    private func content(_ info: InviteInfo) -> some View {
        switch info.phase {
        case .loading:
            if info.error != nil {
                Button(L10n.tr("common.retry")) { store.reload() }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
            } else {
                ProgressView()
                    .padding(Theme.Spacing.large)
                    .accessibilityLabel(L10n.tr("common.loading"))
            }
        case .gone:
            Text(L10n.tr("invite.gone"))
                .font(.body)
                .foregroundStyle(Theme.failText)
                .multilineTextAlignment(.center)
            Button(L10n.tr("invite.go.sign.in")) { dismiss() }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
        case .open:
            openContent(info)
        case .done:
            ProgressView()
                .padding(Theme.Spacing.large)
                .accessibilityLabel(L10n.tr("invite.signing.in"))
        }
    }

    @ViewBuilder
    private func openContent(_ info: InviteInfo) -> some View {
        Text(lead(info))
            .font(.headline)
            .multilineTextAlignment(.center)
        if let signedIn = info.signedInAs {
            Text(L10n.format("invite.signed.in.as", signedIn))
                .font(.body)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            FormPrimaryButton(title: L10n.tr("invite.sign.out.join"), busy: info.busy, enabled: true) {
                store.signOutAndJoin()
            }
        } else if store.plexWaiting {
            PlexWaitingPanel { store.cancelPlex() }
        } else if let way = mediaWay {
            mediaForm(info, way: way)
        } else {
            joinForm(info)
        }
    }

    private func lead(_ info: InviteInfo) -> String {
        switch info.role {
        case .admin: L10n.tr("invite.lead.admin")
        case .kid: L10n.format("invite.lead.kid", info.kidLimit)
        case .member: L10n.tr("invite.lead.member")
        }
    }

    private func providerName(_ way: SignInWayKind) -> String {
        switch way {
        case .jellyfin: L10n.tr("provider.jellyfin")
        case .emby: L10n.tr("provider.emby")
        case .plex: L10n.tr("provider.plex")
        default: ""
        }
    }

    @ViewBuilder
    private func joinForm(_ info: InviteInfo) -> some View {
        if !info.ways.isEmpty {
            ForEach(info.ways) { way in
                Button {
                    if way == .plex {
                        store.joinPlex()
                    } else {
                        mediaWay = way
                    }
                } label: {
                    Text(L10n.format("invite.continue.with", providerName(way)))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
                .disabled(info.busy)
            }
            FormDivider()
        }
        let nameProblem = !name.isEmpty && !SDKClient.shared.validName(name)
        let passwordProblem = !password.isEmpty && !SDKClient.shared.validPassword(password, minimum: info.minPasswordLength)
        let confirmProblem = !confirm.isEmpty && confirm != password
        let canJoin = SDKClient.shared.validName(name)
            && SDKClient.shared.validPassword(password, minimum: info.minPasswordLength)
            && password == confirm
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            TextField(L10n.tr("invite.name"), text: $name)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .formField()
            if nameProblem {
                FormHint(text: L10n.tr("invite.name.rule"), failed: true)
            }
        }
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            FormPasswordField(title: L10n.tr("invite.password"), text: $password, contentType: .newPassword, submitLabel: .next)
            FormHint(text: L10n.format("invite.password.hint", info.minPasswordLength), failed: passwordProblem)
        }
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            FormPasswordField(title: L10n.tr("invite.confirm"), text: $confirm, contentType: .newPassword, submitLabel: .go)
                .onSubmit {
                    if canJoin, !info.busy { store.join(name: name, password: password) }
                }
            if confirmProblem {
                FormHint(text: L10n.tr("invite.mismatch"), failed: true)
            }
        }
        FormPrimaryButton(title: L10n.tr("invite.join"), busy: info.busy, enabled: canJoin) {
            store.join(name: name, password: password)
        }
    }

    @ViewBuilder
    private func mediaForm(_ info: InviteInfo, way: SignInWayKind) -> some View {
        let provider = providerName(way)
        Text(L10n.format("invite.media.lead", provider))
            .font(.body)
            .foregroundStyle(Theme.textMuted)
            .multilineTextAlignment(.center)
        TextField(L10n.format("invite.media.name", provider), text: $mediaName)
            .textContentType(.username)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.next)
            .formField()
        FormPasswordField(title: L10n.tr("invite.password"), text: $mediaPassword, contentType: .password, submitLabel: .go)
            .onSubmit {
                if !mediaName.isEmpty, !mediaPassword.isEmpty, !info.busy {
                    store.joinMediaServer(way: way, name: mediaName, password: mediaPassword)
                }
            }
        FormPrimaryButton(
            title: L10n.format("invite.media.join", provider),
            busy: info.busy,
            enabled: !mediaName.isEmpty && !mediaPassword.isEmpty
        ) {
            store.joinMediaServer(way: way, name: mediaName, password: mediaPassword)
        }
        Button(L10n.tr("invite.other.ways")) {
            mediaWay = nil
            mediaPassword = ""
        }
        .disabled(info.busy)
    }
}
