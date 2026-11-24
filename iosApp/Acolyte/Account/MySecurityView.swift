import SwiftUI
import UIKit

private enum TwoFactorManage: Hashable {
    case codes
    case off
}

private enum SecurityConfirm: Hashable, Identifiable {
    case plex
    case media(provider: String, name: String)
    case sso(MySsoLinkInfo)

    var id: Self { self }
}

struct MySecurityView: View {
    let account: AccountInfo
    @State private var store = MySecurityStore()
    @State private var editingPassword = false
    @State private var current = ""
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var managing = false
    @State private var manageMode: TwoFactorManage = .codes
    @State private var manageCode = ""
    @State private var setupCode = ""
    @State private var mediaEditing: Int64?
    @State private var mediaName = ""
    @State private var mediaPassword = ""
    @State private var pending: SecurityConfirm?
    @Environment(\.openURL) private var openURL

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyStateView()
        } content: {
            List {
                banners(info)
                if !info.backupCodes.isEmpty {
                    backupCodesSection(info)
                }
                passwordSection(info)
                twoFactorSection(info)
                if account.hasPassword || info.plexLinked || !info.sso.isEmpty {
                    linkedSection(info)
                }
                if !info.servers.isEmpty || !info.mediaLinks.isEmpty {
                    mediaSection(info)
                }
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr("myaccount.security"))
        .noticeToast(store.notices)
        .plexBrowser(url: info.plexURL)
        .reauthSheet(store.reauth)
        .confirmationDialog(
            L10n.tr("myaccount.unlink.title"),
            isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } }),
            titleVisibility: .visible,
            presenting: pending
        ) { item in
            Button(L10n.tr("wizard.unlink"), role: .destructive) { unlink(item) }
        } message: { item in
            Text(unlinkMessage(item))
        }
        .task { await store.watch() }
        .task { await store.load() }
        .onChange(of: store.successSerial) { _, _ in resetForms() }
        .onDisappear { store.close() }
    }

    @ViewBuilder
    private func banners(_ info: MySecurityInfo) -> some View {
        if account.mustSetUpTwoFactor {
            NoticeBanner(text: L10n.tr("myaccount.two.factor.needed"))
                .listRowSeparator(.hidden)
        }
        if let error = info.error {
            NoticeBanner(text: MyErrorText.text(error))
                .listRowSeparator(.hidden)
        }
        if let problem = info.problem {
            NoticeBanner(text: MySecurityStore.problemText(problem, minimum: info.minPasswordLength))
                .listRowSeparator(.hidden)
        }
    }

    private func backupCodesSection(_ info: MySecurityInfo) -> some View {
        let joined = info.backupCodes.joined(separator: "\n")
        return Section {
            ForEach(info.backupCodes, id: \.self) { code in
                Text(code)
                    .font(.body.monospaced())
                    .textSelection(.enabled)
            }
            HStack(spacing: Theme.Spacing.small) {
                Button(L10n.tr("myaccount.copy")) { UIPasteboard.general.string = joined }
                    .buttonStyle(.bordered)
                ShareLink(item: joined)
                    .buttonStyle(.bordered)
                Spacer(minLength: 0)
                Button(L10n.tr("myaccount.close")) { store.dismissBackupCodes() }
                    .buttonStyle(.borderedProminent)
            }
        } header: {
            Text(L10n.tr("myaccount.codes.title"))
        } footer: {
            Text(L10n.tr("myaccount.codes.hint"))
        }
    }

    private func passwordSection(_ info: MySecurityInfo) -> some View {
        let valid = SDKClient.shared.validPassword(newPassword, minimum: info.minPasswordLength)
        let matches = newPassword == confirmPassword
        let ready = valid && matches && (!account.hasPassword || !current.isEmpty)
        return Section {
            if editingPassword {
                if account.hasPassword {
                    SecureField(L10n.tr("myaccount.password.current"), text: $current)
                        .textContentType(.password)
                }
                SecureField(L10n.tr("myaccount.password.new"), text: $newPassword)
                    .textContentType(.newPassword)
                SecureField(L10n.tr("myaccount.password.confirm"), text: $confirmPassword)
                    .textContentType(.newPassword)
                if !newPassword.isEmpty && !valid {
                    FormHint(text: L10n.format("myaccount.password.short", info.minPasswordLength), failed: true)
                } else if !confirmPassword.isEmpty && !matches {
                    FormHint(text: L10n.tr("myaccount.password.differ"), failed: true)
                }
                Button(L10n.tr(account.hasPassword ? "myaccount.password.change" : "myaccount.password.set")) {
                    store.changePassword(current: current, new: newPassword, confirm: confirmPassword)
                }
                .disabled(!ready || info.busy)
                Button(L10n.tr("common.cancel"), role: .cancel) { resetForms() }
            } else {
                Button(L10n.tr(account.hasPassword ? "myaccount.password.change" : "myaccount.password.set")) {
                    editingPassword = true
                }
            }
        } header: {
            Text(L10n.tr("myaccount.password"))
        } footer: {
            Text(L10n.tr(account.hasPassword ? "myaccount.password.hint" : "myaccount.password.hint.none"))
        }
    }

    private func twoFactorSection(_ info: MySecurityInfo) -> some View {
        Section {
            LabeledContent {
                StatusChip(
                    text: L10n.tr(account.twoFactor ? "myaccount.two.factor.on" : "myaccount.two.factor.off"),
                    tone: account.twoFactor ? .ok : .idle
                )
            } label: {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("myaccount.two.factor"))
                    Text(L10n.tr("myaccount.two.factor.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            if let setup = info.setup {
                setupRows(setup, info: info)
            } else if account.twoFactor {
                manageRows(info)
            } else {
                Button(L10n.tr("myaccount.two.factor.set.up")) { store.startTwoFactor() }
                    .disabled(info.busy)
            }
        } header: {
            Text(L10n.tr("myaccount.two.factor"))
        }
    }

    @ViewBuilder
    private func setupRows(_ setup: MyTwoFactorSetupInfo, info: MySecurityInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            Text(L10n.tr("myaccount.two.factor.scan"))
                .font(.callout)
            if !setup.qr.isEmpty {
                QRCodeView(rows: setup.qr)
                    .frame(maxWidth: .infinity)
            }
            if let url = URL(string: setup.uri) {
                Button(L10n.tr("myaccount.two.factor.open")) { openURL(url) }
                    .buttonStyle(.bordered)
            }
            Text(L10n.tr("myaccount.two.factor.type.key"))
                .font(.callout)
            Text(grouped(setup.secret))
                .font(.body.monospaced())
                .textSelection(.enabled)
            Button(L10n.tr("myaccount.two.factor.copy.key")) { UIPasteboard.general.string = setup.secret }
                .buttonStyle(.bordered)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
        TextField(L10n.tr("myaccount.two.factor.code"), text: $setupCode)
            .keyboardType(.numberPad)
            .textContentType(.oneTimeCode)
            .onChange(of: setupCode) { _, value in
                let digits = String(value.filter { $0.isASCII && $0.isNumber }.prefix(6))
                if digits != value { setupCode = digits }
            }
        Button(L10n.tr("myaccount.two.factor.enable")) { store.enableTwoFactor(setupCode) }
            .disabled(setupCode.count != 6 || info.busy)
        Button(L10n.tr("common.cancel"), role: .cancel) {
            store.cancelTwoFactor()
            setupCode = ""
        }
    }

    @ViewBuilder
    private func manageRows(_ info: MySecurityInfo) -> some View {
        if managing {
            Picker(L10n.tr("myaccount.two.factor.manage"), selection: $manageMode) {
                Text(L10n.tr("myaccount.two.factor.new.codes")).tag(TwoFactorManage.codes)
                Text(L10n.tr("myaccount.two.factor.turn.off")).tag(TwoFactorManage.off)
            }
            .pickerStyle(.segmented)
            TextField(L10n.tr("myaccount.two.factor.manage.code"), text: $manageCode)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textContentType(.oneTimeCode)
                .onChange(of: manageCode) { _, value in
                    if value.count > 11 { manageCode = String(value.prefix(11)) }
                }
            if manageMode == .codes {
                Button(L10n.tr("myaccount.two.factor.make.codes")) { store.renewBackupCodes(manageCode) }
                    .disabled(manageCode.trimmingCharacters(in: .whitespaces).isEmpty || info.busy)
            } else {
                Button(L10n.tr("myaccount.two.factor.turn.off"), role: .destructive) { store.disableTwoFactor(manageCode) }
                    .disabled(manageCode.trimmingCharacters(in: .whitespaces).isEmpty || info.busy)
            }
            Button(L10n.tr("common.cancel"), role: .cancel) { resetForms() }
        } else {
            Button(L10n.tr("myaccount.two.factor.manage")) { managing = true }
        }
    }

    private func linkedSection(_ info: MySecurityInfo) -> some View {
        Section {
            if info.plexLinked {
                HStack {
                    Text(L10n.format("myaccount.plex.as", info.plexName))
                    Spacer(minLength: Theme.Spacing.small)
                    if account.hasPassword {
                        Button(L10n.tr("wizard.unlink"), role: .destructive) { pending = .plex }
                            .buttonStyle(.bordered)
                            .controlSize(.small)
                            .disabled(info.busy)
                    }
                }
                if !account.hasPassword {
                    Text(L10n.tr("myaccount.plex.set.password"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            } else if account.hasPassword {
                if store.plexWaiting {
                    PlexWaitingPanel { store.cancelPlex() }
                } else {
                    Button(L10n.tr("myaccount.plex.link")) { store.linkPlex() }
                        .disabled(info.busy)
                }
            }
            ForEach(info.sso) { link in
                HStack {
                    Text(L10n.format("myaccount.sso.as", PeopleText.ssoKind(link.kind), link.issuer, link.name))
                    Spacer(minLength: Theme.Spacing.small)
                    Button(L10n.tr("wizard.unlink"), role: .destructive) { pending = .sso(link) }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                        .disabled(info.busy)
                }
            }
        } header: {
            Text(L10n.tr("myaccount.linked"))
        } footer: {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("myaccount.linked.hint"))
                if !info.sso.isEmpty || account.authMethod.hasPrefix("oidc") || account.authMethod.hasPrefix("forward_auth") {
                    Text(L10n.tr("myaccount.sso.web.only"))
                }
            }
        }
    }

    private func mediaSection(_ info: MySecurityInfo) -> some View {
        Section {
            ForEach(info.servers) { server in
                let linked = info.mediaLink(server.kind) != nil
                let title = linked ? L10n.format("myaccount.media.change", server.name) : L10n.format("myaccount.media.link", server.name)
                Button(title) {
                    mediaName = ""
                    mediaPassword = ""
                    mediaEditing = mediaEditing == server.id ? nil : server.id
                }
                if mediaEditing == server.id {
                    TextField(L10n.format("myaccount.media.name", server.name), text: $mediaName)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .textContentType(.username)
                    SecureField(L10n.tr("myaccount.media.password"), text: $mediaPassword)
                        .textContentType(.password)
                    FormHint(text: L10n.format("myaccount.media.password.hint", server.name))
                    Button(L10n.tr("myaccount.media.submit")) {
                        store.linkMedia(serverId: server.id, name: mediaName, password: mediaPassword)
                    }
                    .disabled(mediaName.trimmingCharacters(in: .whitespaces).isEmpty || mediaPassword.isEmpty || info.busy)
                }
            }
            ForEach(info.mediaLinks, id: \.provider) { link in
                HStack {
                    Text(L10n.format("myaccount.media.as", PeopleText.provider(link.provider), link.name))
                    Spacer(minLength: Theme.Spacing.small)
                    Button(L10n.tr("wizard.unlink"), role: .destructive) {
                        pending = .media(provider: link.provider, name: link.name)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .disabled(info.busy)
                }
            }
        } header: {
            Text(L10n.tr("myaccount.media"))
        } footer: {
            Text(L10n.tr("myaccount.media.hint"))
        }
    }

    private func unlink(_ item: SecurityConfirm) {
        switch item {
        case .plex: store.unlinkPlex()
        case let .media(provider, _): store.unlinkMedia(provider)
        case let .sso(link): store.unlinkSso(link.id)
        }
    }

    private func unlinkMessage(_ item: SecurityConfirm) -> String {
        switch item {
        case .plex: L10n.tr("myaccount.unlink.plex.message")
        case let .media(provider, name): L10n.format("myaccount.unlink.media.message", PeopleText.provider(provider), name)
        case .sso: L10n.tr("myaccount.unlink.sso.message")
        }
    }

    private func resetForms() {
        editingPassword = false
        current = ""
        newPassword = ""
        confirmPassword = ""
        managing = false
        manageCode = ""
        setupCode = ""
        mediaEditing = nil
        mediaName = ""
        mediaPassword = ""
        store.clearProblems()
    }

    private func grouped(_ secret: String) -> String {
        var groups: [String] = []
        var rest = Substring(secret)
        while !rest.isEmpty {
            groups.append(String(rest.prefix(4)))
            rest = rest.dropFirst(4)
        }
        return groups.joined(separator: " ")
    }
}
