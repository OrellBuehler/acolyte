import SwiftUI

struct AccountView: View {
    let account: AccountInfo
    @Environment(AppModel.self) private var model
    @State private var confirmSignOut = false

    var body: some View {
        List {
            Section {
                HStack(spacing: Theme.Spacing.large) {
                    AuthImage(source: account.avatarPath, size: .thumb) {
                        ArtPlaceholderView(name: account.displayName)
                    }
                    .frame(width: 64, height: 64)
                    .clipShape(Circle())
                    VStack(alignment: .leading) {
                        Text(account.displayName)
                            .font(.title2.weight(.bold))
                        Text(account.userName)
                            .font(.callout)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                .padding(.vertical, Theme.Spacing.xSmall)
            }
            if account.mustSetUpTwoFactor || account.safeMode {
                Section {
                    if account.mustSetUpTwoFactor {
                        NoticeBanner(text: L10n.tr("account.two.factor.needed"))
                    }
                    if account.safeMode {
                        NoticeBanner(text: L10n.tr("account.safe.mode"))
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
