import SwiftUI
import UIKit

struct SettingsSignInSsoSection: View {
    let store: SettingsSignInStore
    let copied: () -> Void
    @State private var password = ""
    @State private var code = ""

    private var info: SettingsSignInInfo { store.info }

    var body: some View {
        let sso = info.sso
        if sso.phase != .hidden {
            Section {
                SettingsSignInHint(text: L10n.tr("settings.signin.sso.intro"))
                content(sso)
                if sso.phase == .ready {
                    SettingsSignInHint(text: L10n.format("settings.signin.sso.proxy", proxyState(sso)))
                }
            } header: {
                Text(L10n.tr("settings.signin.sso.title"))
            }
        }
    }

    private func proxyState(_ sso: SettingsSignInSsoInfo) -> String {
        sso.forwardOn ? L10n.format("settings.signin.sso.proxy.on", sso.forwardIssuer) : L10n.tr("settings.signin.sso.proxy.off")
    }

    @ViewBuilder
    private func content(_ sso: SettingsSignInSsoInfo) -> some View {
        switch sso.phase {
        case .hidden:
            EmptyView()
        case .needssession:
            SettingsSignInNote(text: L10n.tr("settings.signin.sso.needs.session"), warn: false)
        case .locked:
            SettingsSignInHint(text: L10n.tr("settings.signin.sso.locked"))
            Button(L10n.tr("settings.signin.sso.unlock")) { store.loadSso() }
                .buttonStyle(.borderedProminent)
        case .loading:
            ProgressView()
        case .failed:
            if let error = sso.loadError { SettingsSignInFail(text: SettingsSignInWords.failure(error)) }
            Button(L10n.tr("settings.signin.try.again")) { store.loadSso() }
                .buttonStyle(.bordered)
        case .ready:
            if sso.environment {
                facts(sso)
            } else {
                form(sso)
            }
        }
    }

    @ViewBuilder
    private func facts(_ sso: SettingsSignInSsoInfo) -> some View {
        SettingsSignInNote(text: L10n.tr("settings.signin.sso.env.note"), warn: false)
        LabeledContent(L10n.tr("settings.signin.sso.title"), value: L10n.tr(sso.viewOn ? "settings.signin.on" : "settings.signin.off"))
        if sso.viewOn {
            LabeledContent(L10n.tr("settings.signin.sso.issuer"), value: sso.viewIssuer)
            LabeledContent(L10n.tr("settings.signin.sso.client.id"), value: sso.viewClientId)
            LabeledContent(L10n.tr("settings.signin.sso.callback"), value: sso.callback)
            LabeledContent(
                L10n.tr("settings.signin.sso.new.people"),
                value: L10n.tr(sso.viewAllowJoin ? "settings.signin.sso.may.join" : "settings.signin.join.nobody")
            )
        }
    }

    @ViewBuilder
    private func form(_ sso: SettingsSignInSsoInfo) -> some View {
        let busy = sso.saving
        Toggle(L10n.tr("settings.signin.sso.show.button"), isOn: Binding(get: { sso.on }, set: { store.setSsoOn($0) }))
            .disabled(busy)
        SettingsSignInField(
            title: L10n.tr("settings.signin.sso.issuer"),
            text: sso.issuer,
            onChange: { store.setSsoIssuer($0) },
            hint: L10n.tr("settings.signin.sso.issuer.hint"),
            prompt: "https://auth.example.com/application/o/holyarrs/",
            monospaced: true,
            keyboard: .URL,
            disabled: busy
        )
        HStack(spacing: Theme.Spacing.medium) {
            Button(L10n.tr("settings.signin.test")) { store.testSso() }
                .buttonStyle(.bordered)
                .disabled(busy || sso.issuer.trimmingCharacters(in: .whitespaces).isEmpty || sso.testing)
            if sso.testing { ProgressView() }
        }
        if let test = sso.test {
            SettingsSignInTestView(test: test, okLabel: L10n.tr("settings.signin.sso.openid"))
        }
        SettingsSignInField(
            title: L10n.tr("settings.signin.sso.client.id"),
            text: sso.clientId,
            onChange: { store.setSsoClientId($0) },
            monospaced: true,
            disabled: busy
        )
        SettingsSignInField(
            title: L10n.tr("settings.signin.sso.secret"),
            text: sso.secret,
            onChange: { store.setSsoSecret($0) },
            hint: L10n.tr(sso.secretKept ? "settings.signin.sso.secret.saved" : "settings.signin.sso.secret.new"),
            secure: true,
            disabled: busy || sso.publicClient
        )
        Toggle(L10n.tr("settings.signin.sso.public.client"), isOn: Binding(get: { sso.publicClient }, set: { store.setSsoPublicClient($0) }))
            .disabled(busy)
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(L10n.tr("settings.signin.sso.callback.address"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Text(sso.callback)
                .font(.body.monospaced())
                .textSelection(.enabled)
            SettingsSignInHint(text: L10n.tr("settings.signin.sso.callback.hint"))
        }
        Button(L10n.tr("settings.signin.sso.copy")) {
            UIPasteboard.general.string = sso.callback
            copied()
        }
        .buttonStyle(.bordered)
        Toggle(L10n.tr("settings.signin.sso.allow.join"), isOn: Binding(get: { sso.allowJoin }, set: { store.setSsoAllowJoin($0) }))
            .disabled(busy)
        SettingsSignInField(
            title: L10n.tr("settings.signin.sso.groups.claim"),
            text: sso.groupsClaim,
            onChange: { store.setSsoGroupsClaim($0) },
            hint: L10n.tr("settings.signin.sso.groups.claim.hint"),
            prompt: "groups",
            monospaced: true,
            disabled: busy
        )
        SettingsSignInField(
            title: L10n.tr("settings.signin.sso.group.roles"),
            text: sso.groupRoles,
            onChange: { store.setSsoGroupRoles($0) },
            hint: L10n.tr("settings.signin.sso.group.roles.hint"),
            prompt: "admins=admin, family=member",
            monospaced: true,
            disabled: busy || sso.groupsClaim.trimmingCharacters(in: .whitespaces).isEmpty
        )
        if sso.opening {
            Toggle(
                L10n.tr("settings.signin.sso.confirm.everyone"),
                isOn: Binding(get: { sso.confirmEveryone }, set: { store.setSsoConfirmEveryone($0) })
            )
            .disabled(busy)
        }
        if sso.moving, sso.viewLinks > 0 {
            SettingsSignInNote(text: L10n.plural("settings.signin.sso.moving", sso.viewLinks))
        }
        if sso.callback.hasPrefix("http://") {
            SettingsSignInHint(text: L10n.tr("settings.signin.sso.plain.http"))
        }
        passwordFields(sso, busy: busy)
    }

    @ViewBuilder
    private func passwordFields(_ sso: SettingsSignInSsoInfo, busy: Bool) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(L10n.tr("settings.signin.sso.password"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            SecureField(L10n.tr("settings.signin.sso.password"), text: $password)
                .textContentType(.password)
                .disabled(busy)
            SettingsSignInHint(text: L10n.tr("settings.signin.sso.password.hint"))
        }
        if sso.showCode {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("settings.signin.sso.code"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                TextField(L10n.tr("settings.signin.sso.code"), text: $code)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .disabled(busy)
                if !sso.needsCode {
                    SettingsSignInHint(text: L10n.tr("settings.signin.sso.code.hint"))
                }
            }
        }
        if let message = SettingsSignInWords.ssoError(sso) {
            SettingsSignInFail(text: message)
        }
        HStack(spacing: Theme.Spacing.medium) {
            Button(L10n.tr("settings.signin.sso.save")) {
                store.saveSso(password, code)
            }
            .buttonStyle(.borderedProminent)
            .disabled(busy || password.isEmpty || (sso.needsCode && code.trimmingCharacters(in: .whitespaces).isEmpty))
            if busy { ProgressView() }
        }
    }
}
