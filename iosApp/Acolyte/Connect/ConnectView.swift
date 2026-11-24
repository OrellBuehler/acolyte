import SwiftUI

struct ConnectView: View {
    let expired: Bool
    @State private var store = ConnectStore()
    @State private var invite: InviteTargetInfo?

    var body: some View {
        ScrollView {
            VStack(spacing: Theme.Spacing.large) {
                BrandMark(size: 72)
                    .padding(.top, 48)
                switch store.info.step {
                case .server, .done:
                    ServerStep(info: store.info, expired: expired, store: store) { invite = $0 }
                case .credentials:
                    CredentialsStep(info: store.info, store: store)
                case .twoFactor:
                    TwoFactorStep(info: store.info, store: store)
                case .setup:
                    SetupStep(info: store.info, store: store)
                case .resetCode:
                    ResetCodeStep(info: store.info, store: store)
                case .resetPassword:
                    ResetPasswordStep(info: store.info, store: store)
                }
                if let error = store.info.error {
                    Text(error.localized)
                        .font(.callout)
                        .foregroundStyle(Theme.failText)
                        .multilineTextAlignment(.center)
                }
            }
            .frame(maxWidth: 480)
            .padding(Theme.Spacing.xLarge)
            .frame(maxWidth: .infinity)
            .plexBrowser(url: store.info.plexURL)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Theme.background)
        .sheet(item: $invite) { target in
            InviteView(target: target)
        }
        .task { await store.watch() }
        .onDisappear { store.close() }
    }
}

private struct ServerStep: View {
    let info: SignInInfo
    let expired: Bool
    let store: ConnectStore
    let openInvite: (InviteTargetInfo) -> Void
    @State private var address = ""
    @State private var showInvite = false
    @State private var inviteText = ""
    @State private var inviteInvalid = false

    var body: some View {
        FormHeading(
            title: L10n.tr("connect.title"),
            subtitle: L10n.tr(expired ? "connect.expired" : "connect.subtitle")
        )
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(L10n.tr("connect.address"))
                .font(.subheadline.weight(.semibold))
            TextField(L10n.tr("connect.address.hint"), text: $address)
                .keyboardType(.URL)
                .textContentType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.go)
                .onSubmit { store.connect(address) }
                .formField()
        }
        FormPrimaryButton(title: L10n.tr("connect.continue"), busy: info.busy, enabled: !address.trimmingCharacters(in: .whitespaces).isEmpty) {
            store.connect(address)
        }
        .onAppear {
            if address.isEmpty { address = info.serverInput }
        }
        VStack(spacing: Theme.Spacing.small) {
            Button(L10n.tr("invite.have.link")) {
                showInvite.toggle()
            }
            .font(.subheadline.weight(.semibold))
            if showInvite {
                TextField(L10n.tr("invite.link.hint"), text: $inviteText)
                    .keyboardType(.URL)
                    .textContentType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.go)
                    .onSubmit { open() }
                    .onChange(of: inviteText) { _, _ in inviteInvalid = false }
                    .formField()
                if inviteInvalid {
                    FormHint(text: L10n.tr("invite.link.invalid"), failed: true)
                }
                FormPrimaryButton(title: L10n.tr("invite.open"), busy: false, enabled: !inviteText.trimmingCharacters(in: .whitespaces).isEmpty) {
                    open()
                }
            }
        }
    }

    private func open() {
        if let target = store.parseInvite(inviteText) {
            inviteInvalid = false
            openInvite(target)
        } else {
            inviteInvalid = true
        }
    }
}

private struct CredentialsStep: View {
    let info: SignInInfo
    let store: ConnectStore
    @State private var useKey = false
    @State private var name = ""
    @State private var password = ""
    @State private var key = ""
    @State private var way: SignInWayKind = .password

    private var passwordWays: [SignInWayKind] {
        info.ways.filter { $0 != .plex }
    }

    var body: some View {
        VStack(spacing: Theme.Spacing.large) {
            FormHeading(title: L10n.tr("signin.title"))
            HStack {
                Text(info.serverName)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Button(L10n.tr("connect.change.server")) { store.back() }
                    .disabled(info.busy)
            }
            if store.plexWaiting {
                PlexWaitingPanel { store.cancelPlex() }
            } else {
                options
            }
        }
        .onAppear {
            if name.isEmpty { name = info.userName }
            if !passwordWays.contains(way), let first = passwordWays.first { way = first }
        }
    }

    @ViewBuilder
    private var options: some View {
        Picker(L10n.tr("signin.title"), selection: $useKey) {
            Text(L10n.tr("signin.with.password")).tag(false)
            Text(L10n.tr("signin.with.key")).tag(true)
        }
        .pickerStyle(.segmented)
        if useKey {
            keyForm
        } else {
            accountForm
        }
        if let host = info.hostRefused {
            NoticeBanner(text: L10n.format("signin.host.refused", host))
        }
        if !info.otherWays.isEmpty {
            Text(L10n.format("signin.other.ways", info.otherWays.map(\.label).joined(separator: ", ")))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
        }
    }

    @ViewBuilder
    private var keyForm: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            SecureField(L10n.tr("signin.api.key"), text: $key)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .onSubmit { store.useApiKey(key) }
                .formField()
            Text(L10n.tr("signin.api.key.hint"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        FormPrimaryButton(title: L10n.tr("signin.submit"), busy: info.busy, enabled: !key.isEmpty) {
            store.useApiKey(key)
        }
    }

    @ViewBuilder
    private var accountForm: some View {
        if info.ways.contains(.plex) {
            Button {
                store.signInWithPlex()
            } label: {
                Text(L10n.tr("signin.plex.continue"))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
            .disabled(info.busy)
            if !passwordWays.isEmpty {
                FormDivider()
            }
        }
        if !passwordWays.isEmpty {
            if passwordWays.count > 1 {
                Picker(L10n.tr("signin.account"), selection: $way) {
                    ForEach(passwordWays) { option in
                        Text(option.label).tag(option)
                    }
                }
                .pickerStyle(.menu)
                .frame(maxWidth: .infinity, alignment: .leading)
            } else if let only = passwordWays.first, only != .password {
                Text(only.label)
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            TextField(L10n.tr("signin.name"), text: $name)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .formField()
            FormPasswordField(title: L10n.tr("signin.password"), text: $password, contentType: .password, submitLabel: .go)
                .onSubmit { store.signIn(name: name, password: password, way: way) }
            FormPrimaryButton(title: L10n.tr("signin.submit"), busy: info.busy, enabled: !name.isEmpty && !password.isEmpty) {
                store.signIn(name: name, password: password, way: way)
            }
            if info.ownerReset {
                Button(L10n.tr("signin.forgot.password")) { store.startReset() }
                    .font(.subheadline.weight(.semibold))
                    .disabled(info.busy)
            }
        }
    }
}

private struct TwoFactorStep: View {
    let info: SignInInfo
    let store: ConnectStore
    @State private var code = ""

    var body: some View {
        FormHeading(title: L10n.tr("twofactor.title"), subtitle: L10n.tr("twofactor.message"))
        TextField(L10n.tr("twofactor.code"), text: $code)
            .textContentType(.oneTimeCode)
            .keyboardType(.asciiCapable)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.done)
            .onSubmit { store.verify(code) }
            .formField()
        FormPrimaryButton(title: L10n.tr("twofactor.submit"), busy: info.busy, enabled: !code.isEmpty) {
            store.verify(code)
        }
        Button(L10n.tr("common.back")) { store.back() }
            .disabled(info.busy)
    }
}

private struct SetupStep: View {
    let info: SignInInfo
    let store: ConnectStore
    @State private var welcomed = false
    @State private var username = ""
    @State private var display = ""
    @State private var displayEdited = false
    @State private var password = ""
    @State private var confirm = ""
    @State private var picture: Data?

    var body: some View {
        VStack(spacing: Theme.Spacing.large) {
            if welcomed {
                form
            } else {
                welcome
            }
        }
    }

    private var welcome: some View {
        VStack(spacing: Theme.Spacing.large) {
            FormHeading(title: L10n.tr("signin.setup.welcome.title"), subtitle: L10n.tr("signin.setup.welcome.lead"))
            VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                feature("arrow.down.circle", "signin.setup.feature.downloads")
                feature("folder", "signin.setup.feature.sorts")
                feature("person.2", "signin.setup.feature.requests")
                feature("captions.bubble", "signin.setup.feature.subtitles")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(L10n.tr("signin.setup.welcome.next"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            if let host = info.hostRefused {
                NoticeBanner(text: L10n.format("signin.host.refused", host))
            }
            FormPrimaryButton(title: L10n.tr("signin.setup.get.started"), busy: false, enabled: true) {
                welcomed = true
            }
            Button(L10n.tr("common.back")) { store.back() }
        }
    }

    private func feature(_ symbol: String, _ key: String) -> some View {
        Label {
            Text(L10n.tr(key))
                .font(.body)
                .foregroundStyle(Theme.text)
        } icon: {
            Image(systemName: symbol)
                .foregroundStyle(Theme.accentText)
        }
    }

    private var passwordProblem: Bool {
        !password.isEmpty && !SDKClient.shared.validPassword(password, minimum: info.minPasswordLength)
    }

    private var confirmProblem: Bool {
        !confirm.isEmpty && confirm != password
    }

    private var canCreate: Bool {
        SDKClient.shared.validName(username)
            && SDKClient.shared.validPassword(password, minimum: info.minPasswordLength)
            && password == confirm
            && SDKClient.shared.validDisplayName(display)
    }

    private var form: some View {
        VStack(spacing: Theme.Spacing.large) {
            FormHeading(title: L10n.tr("signin.setup.title"), subtitle: L10n.tr("signin.setup.lead"))
            PictureChooser(picture: $picture)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("signin.setup.username"))
                    .font(.subheadline.weight(.semibold))
                TextField(L10n.tr("signin.setup.username"), text: $username)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.next)
                    .onChange(of: username) { _, new in
                        let cleaned = SDKClient.shared.cleanName(new)
                        if cleaned != new {
                            username = cleaned
                        } else if !displayEdited {
                            display = cleaned
                        }
                    }
                    .formField()
                FormHint(text: L10n.tr("signin.setup.username.hint"))
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("signin.setup.display.name"))
                    .font(.subheadline.weight(.semibold))
                TextField(
                    L10n.tr("signin.setup.display.name"),
                    text: Binding(
                        get: { display },
                        set: { value in
                            display = value
                            displayEdited = true
                        }
                    )
                )
                .submitLabel(.next)
                .formField()
                FormHint(text: L10n.tr("signin.setup.display.name.hint"))
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("signin.password"))
                    .font(.subheadline.weight(.semibold))
                FormPasswordField(title: L10n.tr("signin.password"), text: $password, contentType: .newPassword, submitLabel: .next)
                FormHint(text: L10n.format("signin.setup.password.hint", info.minPasswordLength), failed: passwordProblem)
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("signin.setup.confirm"))
                    .font(.subheadline.weight(.semibold))
                FormPasswordField(title: L10n.tr("signin.setup.confirm"), text: $confirm, contentType: .newPassword, submitLabel: .go)
                    .onSubmit { create() }
                FormHint(
                    text: L10n.tr(confirmProblem ? "signin.setup.mismatch" : "signin.setup.confirm.hint"),
                    failed: confirmProblem
                )
            }
            if info.configUnmapped {
                NoticeBanner(text: L10n.tr("signin.setup.config.unmapped"))
            }
            if let host = info.hostRefused {
                NoticeBanner(text: L10n.format("signin.host.refused", host))
            }
            FormPrimaryButton(title: L10n.tr("signin.setup.create"), busy: info.busy, enabled: canCreate) {
                create()
            }
            Button(L10n.tr("common.back")) { welcomed = false }
                .disabled(info.busy)
        }
    }

    private func create() {
        guard canCreate, !info.busy else { return }
        store.createOwner(name: username, displayName: display, password: password, picture: picture)
    }
}

private struct ResetCodeStep: View {
    let info: SignInInfo
    let store: ConnectStore
    @State private var code = ""

    var body: some View {
        VStack(spacing: Theme.Spacing.large) {
            FormHeading(
                title: L10n.tr("signin.reset.title"),
                subtitle: L10n.tr(info.ownerResetEnv ? "signin.reset.lead.env" : "signin.reset.lead")
            )
            if !info.ownerResetEnv {
                Button {
                    store.makeResetCode()
                } label: {
                    Text(L10n.tr("signin.reset.make.code"))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
                .disabled(info.busy)
            }
            if info.resetCodeMade {
                FormHint(text: L10n.tr("signin.reset.made"))
            }
            TextField(L10n.tr("signin.reset.code"), text: $code)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .onSubmit { check() }
                .formField()
            FormPrimaryButton(title: L10n.tr("signin.reset.next"), busy: info.busy, enabled: !code.trimmingCharacters(in: .whitespaces).isEmpty) {
                check()
            }
            DisclosureGroup(L10n.tr("signin.reset.log.title")) {
                Text(L10n.tr("signin.reset.log.where"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, Theme.Spacing.xSmall)
            }
            Button(L10n.tr("signin.reset.back")) { store.back() }
                .disabled(info.busy)
        }
    }

    private func check() {
        guard !code.trimmingCharacters(in: .whitespaces).isEmpty, !info.busy else { return }
        store.checkResetCode(code)
    }
}

private struct ResetPasswordStep: View {
    let info: SignInInfo
    let store: ConnectStore
    @State private var name = ""
    @State private var password = ""
    @State private var confirm = ""

    private var passwordProblem: Bool {
        !password.isEmpty && !SDKClient.shared.validPassword(password, minimum: info.minPasswordLength)
    }

    private var confirmProblem: Bool {
        !confirm.isEmpty && confirm != password
    }

    private var canReset: Bool {
        SDKClient.shared.validName(name)
            && SDKClient.shared.validPassword(password, minimum: info.minPasswordLength)
            && password == confirm
    }

    var body: some View {
        VStack(spacing: Theme.Spacing.large) {
            FormHeading(title: L10n.tr("signin.reset.new.title"), subtitle: L10n.tr("signin.reset.warning"))
            TextField(L10n.tr("signin.reset.owner.name"), text: $name)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .formField()
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                FormPasswordField(title: L10n.tr("signin.reset.new.password"), text: $password, contentType: .newPassword, submitLabel: .next)
                FormHint(text: L10n.format("signin.setup.password.hint", info.minPasswordLength), failed: passwordProblem)
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                FormPasswordField(title: L10n.tr("signin.reset.same.password"), text: $confirm, contentType: .newPassword, submitLabel: .go)
                    .onSubmit { reset() }
                if confirmProblem {
                    FormHint(text: L10n.tr("signin.setup.mismatch"), failed: true)
                }
            }
            FormPrimaryButton(title: L10n.tr("signin.reset.submit"), busy: info.busy, enabled: canReset) {
                reset()
            }
            Button(L10n.tr("signin.reset.back")) { store.back() }
                .disabled(info.busy)
        }
        .onAppear {
            if name.isEmpty { name = info.resetName }
        }
    }

    private func reset() {
        guard canReset, !info.busy else { return }
        store.finishReset(name: name, password: password)
    }
}

private extension SignInWayKind {
    var label: String {
        switch self {
        case .password: L10n.tr("signin.way.password")
        case .jellyfin: L10n.tr("signin.way.jellyfin")
        case .emby: L10n.tr("signin.way.emby")
        case .plex: L10n.tr("way.plex")
        case .oidc: L10n.tr("way.oidc")
        case .forwardAuth: L10n.tr("way.forward.auth")
        }
    }
}
