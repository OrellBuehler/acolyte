import SwiftUI

struct ConnectView: View {
    let expired: Bool
    @State private var store = ConnectStore()

    var body: some View {
        ScrollView {
            VStack(spacing: Theme.Spacing.large) {
                BrandMark(size: 72)
                    .padding(.top, 48)
                switch store.info.step {
                case .server, .done:
                    ServerStep(info: store.info, expired: expired, store: store)
                case .credentials:
                    CredentialsStep(info: store.info, store: store)
                case .twoFactor:
                    TwoFactorStep(info: store.info, store: store)
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
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Theme.background)
        .task { await store.watch() }
        .onDisappear { store.close() }
    }
}

private struct Heading: View {
    let title: String
    var subtitle: String?

    var body: some View {
        VStack(spacing: Theme.Spacing.small) {
            Text(title)
                .font(.title.weight(.bold))
                .multilineTextAlignment(.center)
            if let subtitle {
                Text(subtitle)
                    .font(.body)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

private struct FieldStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(.horizontal, Theme.Spacing.medium)
            .frame(minHeight: 44)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
                    .strokeBorder(Theme.line, lineWidth: 1)
            }
    }
}

private extension View {
    func field() -> some View {
        modifier(FieldStyle())
    }
}

private struct PrimaryButton: View {
    let title: String
    let busy: Bool
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Text(title).opacity(busy ? 0 : 1)
                if busy {
                    ProgressView().tint(Theme.onAccent)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(!enabled || busy)
    }
}

private struct ServerStep: View {
    let info: SignInInfo
    let expired: Bool
    let store: ConnectStore
    @State private var address = ""

    var body: some View {
        Heading(
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
                .field()
        }
        PrimaryButton(title: L10n.tr("connect.continue"), busy: info.busy, enabled: !address.trimmingCharacters(in: .whitespaces).isEmpty) {
            store.connect(address)
        }
        .onAppear {
            if address.isEmpty { address = info.serverInput }
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

    var body: some View {
        Heading(title: L10n.tr("signin.title"))
        HStack {
            Text(info.serverName)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
            Button(L10n.tr("connect.change.server")) { store.back() }
                .disabled(info.busy)
        }
        Picker(L10n.tr("signin.title"), selection: $useKey) {
            Text(L10n.tr("signin.with.password")).tag(false)
            Text(L10n.tr("signin.with.key")).tag(true)
        }
        .pickerStyle(.segmented)
        if useKey {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                SecureField(L10n.tr("signin.api.key"), text: $key)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.done)
                    .onSubmit { store.useApiKey(key) }
                    .field()
                Text(L10n.tr("signin.api.key.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            PrimaryButton(title: L10n.tr("signin.submit"), busy: info.busy, enabled: !key.isEmpty) {
                store.useApiKey(key)
            }
        } else {
            if info.ways.count > 1 {
                Picker(L10n.tr("signin.account"), selection: $way) {
                    ForEach(info.ways) { option in
                        Text(option.label).tag(option)
                    }
                }
                .pickerStyle(.menu)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            TextField(L10n.tr("signin.name"), text: $name)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .field()
            SecureField(L10n.tr("signin.password"), text: $password)
                .textContentType(.password)
                .submitLabel(.go)
                .onSubmit { store.signIn(name: name, password: password, way: way) }
                .field()
            PrimaryButton(title: L10n.tr("signin.submit"), busy: info.busy, enabled: !name.isEmpty && !password.isEmpty) {
                store.signIn(name: name, password: password, way: way)
            }
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
        EmptyView()
            .onAppear {
                if name.isEmpty { name = info.userName }
            }
    }
}

private struct TwoFactorStep: View {
    let info: SignInInfo
    let store: ConnectStore
    @State private var code = ""

    var body: some View {
        Heading(title: L10n.tr("twofactor.title"), subtitle: L10n.tr("twofactor.message"))
        TextField(L10n.tr("twofactor.code"), text: $code)
            .textContentType(.oneTimeCode)
            .keyboardType(.asciiCapable)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.done)
            .onSubmit { store.verify(code) }
            .field()
        PrimaryButton(title: L10n.tr("twofactor.submit"), busy: info.busy, enabled: !code.isEmpty) {
            store.verify(code)
        }
        Button(L10n.tr("common.back")) { store.back() }
            .disabled(info.busy)
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
