import SwiftUI

struct AccountView: View {
    let account: AccountInfo
    @Environment(AppModel.self) private var model
    @State private var confirmSignOut = false

    var body: some View {
        List {
            Section {
                HStack(spacing: Theme.Spacing.large) {
                    PersonAvatar(source: account.avatarPath, name: account.displayName, side: 64)
                    VStack(alignment: .leading) {
                        Text(account.displayName)
                            .font(.title2.weight(.bold))
                        Text(account.userName)
                            .font(.callout)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                .padding(.vertical, Theme.Spacing.xSmall)
                .accessibilityElement(children: .combine)
            }
            if account.mustSetUpTwoFactor || account.safeMode {
                Section {
                    if account.mustSetUpTwoFactor {
                        NoticeBanner(text: L10n.tr("myaccount.two.factor.needed"))
                    }
                    if account.safeMode {
                        NoticeBanner(text: L10n.tr("account.safe.mode"))
                    }
                }
            }
            Section {
                NavigationLink {
                    MyProfileView(account: account)
                } label: {
                    Label(L10n.tr("myaccount.profile"), systemImage: "person.text.rectangle")
                }
                NavigationLink {
                    MyPreferencesView()
                } label: {
                    Label(L10n.tr("myaccount.preferences"), systemImage: "globe")
                }
                NavigationLink {
                    MySecurityView(account: account)
                } label: {
                    LabeledContent {
                        Text(account.twoFactor ? L10n.tr("myaccount.two.factor.on") : L10n.tr("myaccount.two.factor.off"))
                            .foregroundStyle(Theme.textMuted)
                    } label: {
                        Label(L10n.tr("myaccount.security"), systemImage: "lock.shield")
                    }
                }
                NavigationLink {
                    MyAlertsView()
                } label: {
                    Label(L10n.tr("myaccount.alerts"), systemImage: "bell")
                }
                NavigationLink {
                    MyDevicesView(userId: nil, title: L10n.tr("myaccount.devices"))
                } label: {
                    Label(L10n.tr("myaccount.devices"), systemImage: "iphone.and.ipad")
                }
                if account.mayConnectAiApps {
                    NavigationLink {
                        AiConnectView(account: account)
                    } label: {
                        Label(L10n.tr("myaccount.ai.connect"), systemImage: "sparkles")
                    }
                }
            }
            Section {
                LabeledContent(L10n.tr("account.server"), value: account.serverName)
                if let version = account.serverVersion {
                    LabeledContent(L10n.tr("account.version"), value: version)
                }
                LabeledContent(L10n.tr("account.role"), value: roleText)
                LabeledContent(L10n.tr("account.method"), value: methodText)
                LabeledContent(L10n.tr("account.live")) {
                    StatusChip(text: liveText, tone: liveTone)
                }
                if let url = URL(string: L10n.tr("privacy.policy.url")) {
                    Link(L10n.tr("privacy.policy"), destination: url)
                }
            }
            Section {
                Button(L10n.tr("account.sign.out"), role: .destructive) {
                    confirmSignOut = true
                }
            }
        }
        .navigationTitle(SectionID.account.title)
        .confirmationDialog(L10n.tr("account.sign.out.title"), isPresented: $confirmSignOut, titleVisibility: .visible) {
            Button(L10n.tr("account.sign.out"), role: .destructive) { model.signOut() }
        } message: {
            Text(L10n.tr("account.sign.out.message"))
        }
    }

    private var roleText: String {
        switch account.role {
        case .owner: L10n.tr("role.owner")
        case .admin: L10n.tr("role.admin")
        case .member: L10n.tr("role.member")
        case .unknown: L10n.tr("role.unknown")
        }
    }

    private var methodText: String {
        switch account.method {
        case .password: L10n.tr("method.password")
        case .mediaServer: L10n.tr("method.media.server")
        case .apiKey: L10n.tr("method.api.key")
        }
    }

    private var liveText: String {
        switch model.live {
        case .off: L10n.tr("live.off")
        case .connecting: L10n.tr("live.connecting")
        case .live: L10n.tr("live.live")
        case .reconnecting: L10n.tr("live.reconnecting")
        }
    }

    private var liveTone: ToneKind {
        switch model.live {
        case .off: .idle
        case .connecting, .reconnecting: .wait
        case .live: .ok
        }
    }
}
