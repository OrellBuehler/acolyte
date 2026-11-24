import SwiftUI

private enum AiConnectOutcome: Hashable {
    case connected
    case denied
}

struct AiConnectView: View {
    let account: AccountInfo
    @State private var link = ""
    @State private var store: AiConnectStore?

    var body: some View {
        Group {
            if let store {
                AiConsentView(store: store, account: account) { self.store = nil }
            } else {
                pasteForm
            }
        }
        .navigationTitle(L10n.tr("aiconnect.title"))
    }

    private var ready: Bool {
        link.contains("?") && link.contains("response_type=")
    }

    private var pasteForm: some View {
        Form {
            Section {
                TextField(L10n.tr("aiconnect.paste.placeholder"), text: $link, axis: .vertical)
                    .lineLimit(3...8)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                if !link.isEmpty && !ready {
                    FormHint(text: L10n.tr("aiconnect.paste.invalid"), failed: true)
                }
                Button(L10n.tr("aiconnect.continue")) {
                    store = AiConnectStore(link: link)
                }
                .disabled(!ready)
            } header: {
                Text(L10n.tr("aiconnect.paste.title"))
            } footer: {
                Text(L10n.tr("aiconnect.paste.hint"))
            }
        }
    }
}

private struct AiConsentView: View {
    let store: AiConnectStore
    let account: AccountInfo
    let startOver: () -> Void
    @State private var outcome: AiConnectOutcome?
    @Environment(\.openURL) private var openURL

    var body: some View {
        let info = store.info
        Group {
            if let outcome {
                outcomeView(outcome, info: info)
            } else {
                content(info)
            }
        }
        .reauthSheet(store.reauth)
        .task { await store.watch() }
        .task { await store.load() }
        .onChange(of: info.redirect) { _, new in
            guard let new else { return }
            outcome = .connected
            openURL(new)
        }
    }

    @ViewBuilder
    private func content(_ info: AiConnectInfo) -> some View {
        if !info.valid {
            failure(text: L10n.tr("aiconnect.paste.invalid"))
        } else {
            switch info.load.phase {
            case .loading: LoadingStateView()
            case .failed: failure(text: MyErrorText.text(info.load.error ?? .unknown))
            case .ready: consent(info)
            }
        }
    }

    private func failure(text: String) -> some View {
        VStack(spacing: Theme.Spacing.large) {
            NoticeBanner(text: text)
            Text(L10n.tr("aiconnect.start.again"))
                .font(.body)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            Button(L10n.tr("aiconnect.start.over"), action: startOver)
                .buttonStyle(.bordered)
        }
        .padding(Theme.Spacing.xLarge)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func consent(_ info: AiConnectInfo) -> some View {
        let name = info.clientName.isEmpty ? info.returnsTo : info.clientName
        return List {
            if let error = info.error {
                Section {
                    NoticeBanner(text: MyErrorText.text(error))
                }
            }
            Section {
                VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    Text(L10n.format("aiconnect.wants", info.returnsTo, account.displayName))
                        .font(.headline)
                    Text(info.nameVerified ? L10n.format("aiconnect.says", info.clientName) : L10n.format("aiconnect.says.unverified", info.clientName))
                        .font(.subheadline)
                        .foregroundStyle(Theme.textMuted)
                }
                bullet("magnifyingglass", L10n.tr("aiconnect.can.look.up"))
                bullet("plus.circle", L10n.tr("aiconnect.can.ask"))
                bullet("hand.raised", L10n.tr("aiconnect.cannot"))
                bullet("clock.arrow.circlepath", L10n.tr("aiconnect.shows.in.history"))
            }
            if info.local {
                Section {
                    NoticeBanner(text: L10n.tr("aiconnect.local.warning"))
                }
            }
            if !info.enabled {
                Section {
                    NoticeBanner(text: L10n.tr("aiconnect.off"))
                }
            }
            if !info.mayAllow {
                Section {
                    NoticeBanner(text: L10n.tr(account.usesCookies ? "aiconnect.staff.only" : "aiconnect.cookie.only"))
                }
            }
            Section {
                Button {
                    store.allow()
                } label: {
                    HStack {
                        Text(L10n.tr("aiconnect.allow"))
                        if info.busy {
                            Spacer()
                            ProgressView()
                        }
                    }
                }
                .disabled(!info.canAllow)
                Button(L10n.tr("common.cancel"), role: .cancel) {
                    outcome = .denied
                    if let deny = info.denyURL { openURL(deny) }
                }
                .disabled(info.busy)
            } footer: {
                Text(L10n.format("aiconnect.allow.hint", name))
            }
        }
    }

    private func bullet(_ symbol: String, _ text: String) -> some View {
        Label {
            Text(text)
                .font(.callout)
        } icon: {
            Image(systemName: symbol)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private func outcomeView(_ outcome: AiConnectOutcome, info: AiConnectInfo) -> some View {
        let name = info.clientName.isEmpty ? info.returnsTo : info.clientName
        return VStack(spacing: Theme.Spacing.large) {
            switch outcome {
            case .connected:
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(Theme.ok)
                    .accessibilityHidden(true)
                Text(L10n.format("aiconnect.connected", name))
                    .font(.title3.weight(.semibold))
                Text(L10n.format("aiconnect.connected.hint", name))
                    .font(.body)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            case .denied:
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(Theme.textMuted)
                    .accessibilityHidden(true)
                Text(L10n.tr("aiconnect.not.connected"))
                    .font(.title3.weight(.semibold))
                Text(L10n.format("aiconnect.not.connected.hint", name))
                    .font(.body)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            }
            Button(L10n.tr("aiconnect.start.over"), action: startOver)
                .buttonStyle(.bordered)
        }
        .padding(Theme.Spacing.xLarge)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
