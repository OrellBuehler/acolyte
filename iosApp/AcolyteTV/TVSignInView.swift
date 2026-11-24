import SwiftUI

struct TVSignInView: View {
    let expired: Bool
    @State private var store = ConnectStore()

    var body: some View {
        ScrollView {
            VStack(spacing: 40) {
                TVBrandMark(size: 140)
                switch store.info.step {
                case .server, .done:
                    TVServerStep(info: store.info, expired: expired, store: store)
                case .credentials:
                    TVCredentialsStep(info: store.info, store: store)
                case .twoFactor:
                    TVTwoFactorStep(info: store.info, store: store)
                case .setup, .resetCode, .resetPassword:
                    TVUnsupportedStep(info: store.info, store: store)
                }
                if let error = store.info.error {
                    Text(error.localized)
                        .font(.callout)
                        .foregroundStyle(Theme.failText)
                        .multilineTextAlignment(.center)
                }
            }
            .frame(maxWidth: 900)
            .padding(60)
            .frame(maxWidth: .infinity)
        }
        .task { await store.watch() }
        .onDisappear { store.close() }
    }
}

private struct TVHeading: View {
    let title: String
    var subtitle: String?

    var body: some View {
        VStack(spacing: 12) {
            Text(title)
                .font(.title2.bold())
                .foregroundStyle(Theme.text)
            if let subtitle {
                Text(subtitle)
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

private struct TVServerStep: View {
    let info: SignInInfo
    let expired: Bool
    let store: ConnectStore
    @State private var address = ""

    var body: some View {
        TVHeading(title: L10n.tr("connect.title"), subtitle: L10n.tr(expired ? "connect.expired" : "connect.subtitle"))
        TextField(L10n.tr("connect.address.hint"), text: $address)
            .keyboardType(.URL)
            .textContentType(.URL)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.go)
            .onSubmit { store.connect(address) }
        Button(L10n.tr("connect.continue")) {
            store.connect(address)
        }
        .buttonStyle(.borderedProminent)
        .disabled(info.busy || address.trimmingCharacters(in: .whitespaces).isEmpty)
        .onAppear {
            if address.isEmpty { address = info.serverInput }
        }
    }
}

private struct TVCredentialsStep: View {
    let info: SignInInfo
    let store: ConnectStore
    @State private var name = ""
    @State private var password = ""

    private var way: SignInWayKind {
        let ways = info.ways.filter { $0 != .plex && $0 != .oidc && $0 != .forwardAuth }
        return ways.contains(.password) ? .password : (ways.first ?? .password)
    }

    var body: some View {
        TVHeading(title: L10n.tr("signin.title"), subtitle: info.serverName)
        TextField(L10n.tr("signin.name"), text: $name)
            .textContentType(.username)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.next)
        SecureField(L10n.tr("signin.password"), text: $password)
            .textContentType(.password)
            .submitLabel(.go)
            .onSubmit { store.signIn(name: name, password: password, way: way) }
        Button(L10n.tr("signin.submit")) {
            store.signIn(name: name, password: password, way: way)
        }
        .buttonStyle(.borderedProminent)
        .disabled(info.busy || name.isEmpty || password.isEmpty)
        Button(L10n.tr("connect.change.server")) { store.back() }
            .disabled(info.busy)
        .onAppear {
            if name.isEmpty { name = info.userName }
        }
    }
}

private struct TVTwoFactorStep: View {
    let info: SignInInfo
    let store: ConnectStore
    @State private var code = ""

    var body: some View {
        TVHeading(title: L10n.tr("twofactor.title"), subtitle: L10n.tr("twofactor.message"))
        TextField(L10n.tr("twofactor.code"), text: $code)
            .textContentType(.oneTimeCode)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.done)
            .onSubmit { store.verify(code) }
        Button(L10n.tr("twofactor.submit")) {
            store.verify(code)
        }
        .buttonStyle(.borderedProminent)
        .disabled(info.busy || code.isEmpty)
        Button(L10n.tr("common.back")) { store.back() }
            .disabled(info.busy)
    }
}

private struct TVUnsupportedStep: View {
    let info: SignInInfo
    let store: ConnectStore

    var body: some View {
        TVHeading(title: L10n.tr("signin.title"), subtitle: L10n.tr("tv.signin.unsupported"))
        Button(L10n.tr("common.back")) { store.back() }
            .disabled(info.busy)
    }
}
