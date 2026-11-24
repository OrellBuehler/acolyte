import Observation
import SwiftUI

@MainActor
@Observable
final class ReauthStore {
    private(set) var info: ReauthInfo = .hidden

    @ObservationIgnored private let driver: ReauthDriver

    init(driver: ReauthDriver) {
        self.driver = driver
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func confirm(password: String, code: String) {
        let driver = self.driver
        Task { await driver.confirm(password: password, code: code) }
    }

    func cancel() {
        driver.cancel()
    }

    func signOut() {
        driver.cancel()
        Task { await SDKClient.shared.signOut() }
    }
}

private struct ReauthSheetModifier: ViewModifier {
    let store: ReauthStore

    func body(content: Content) -> some View {
        let visible = store.info.visible
        return content
            .task { await store.watch() }
            .sheet(
                isPresented: Binding(
                    get: { visible },
                    set: { shown in
                        if !shown { store.cancel() }
                    }
                )
            ) {
                ReauthSheet(store: store)
            }
    }
}

extension View {
    func reauthSheet(_ store: ReauthStore) -> some View {
        modifier(ReauthSheetModifier(store: store))
    }
}

struct ReauthSheet: View {
    let store: ReauthStore
    @State private var password = ""
    @State private var code = ""

    var body: some View {
        let info = store.info
        NavigationStack {
            ScrollView {
                VStack(spacing: Theme.Spacing.large) {
                    switch info.mode {
                    case .password:
                        passwordForm(info)
                    case .signInAgain:
                        notice(title: L10n.tr("reauth.sign.in.again.title"), message: L10n.tr("reauth.sign.in.again.message"))
                        Button(L10n.tr("reauth.sign.out"), role: .destructive) { store.signOut() }
                            .buttonStyle(.borderedProminent)
                            .controlSize(.large)
                    case .setPassword:
                        notice(title: L10n.tr("reauth.set.password.title"), message: L10n.tr("reauth.set.password.message"))
                    }
                }
                .frame(maxWidth: 480)
                .padding(Theme.Spacing.xLarge)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Theme.background)
            .navigationTitle(L10n.tr("reauth.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { store.cancel() }
                        .disabled(info.busy)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .interactiveDismissDisabled(info.busy)
        .onAppear {
            password = ""
            code = ""
        }
    }

    @ViewBuilder
    private func passwordForm(_ info: ReauthInfo) -> some View {
        Text(L10n.tr("reauth.message"))
            .font(.body)
            .foregroundStyle(Theme.textMuted)
            .multilineTextAlignment(.center)
        FormPasswordField(title: L10n.tr("reauth.password"), text: $password, contentType: .password, submitLabel: info.needsCode ? .next : .go)
            .onSubmit { submit(info) }
        if info.needsCode {
            TextField(L10n.tr("reauth.code"), text: $code)
                .textContentType(.oneTimeCode)
                .keyboardType(.asciiCapable)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.go)
                .onSubmit { submit(info) }
                .formField()
        }
        if let error = info.error {
            Text(Self.errorText(error))
                .font(.callout)
                .foregroundStyle(Theme.failText)
                .multilineTextAlignment(.center)
        }
        FormPrimaryButton(title: L10n.tr("reauth.confirm"), busy: info.busy, enabled: canSubmit(info)) {
            submit(info)
        }
    }

    private func notice(title: String, message: String) -> some View {
        VStack(spacing: Theme.Spacing.small) {
            Text(title)
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(message)
                .font(.body)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
        }
    }

    private func canSubmit(_ info: ReauthInfo) -> Bool {
        !password.isEmpty && (!info.needsCode || !code.trimmingCharacters(in: .whitespaces).isEmpty)
    }

    private func submit(_ info: ReauthInfo) {
        guard canSubmit(info), !info.busy else { return }
        store.confirm(password: password, code: code)
    }

    nonisolated static func errorText(_ error: ErrorInfo) -> String {
        switch error.kind {
        case .unauthorized, .forbidden: L10n.tr("reauth.error.wrong")
        default: AccountErrorText.text(error)
        }
    }
}
