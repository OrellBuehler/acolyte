import SwiftUI

struct FirstRunView: View {
    @State private var store = WizardStore()
    @State private var editing: WizardProviderKind?

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: Theme.Spacing.large) {
                    BrandMark(size: 56)
                        .padding(.top, Theme.Spacing.xLarge)
                    Text(L10n.format("wizard.step", store.info.step == .servers ? 1 : 2, 2))
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Theme.textMuted)
                    switch store.info.step {
                    case .servers:
                        ServersStep(store: store, editing: $editing)
                    case .done:
                        DoneStep(store: store)
                    }
                    if let error = store.error {
                        Text(WizardErrorText.text(error))
                            .font(.callout)
                            .foregroundStyle(Theme.failText)
                            .multilineTextAlignment(.center)
                    }
                }
                .frame(maxWidth: 480)
                .padding(Theme.Spacing.xLarge)
                .frame(maxWidth: .infinity)
                .reauthSheet(store.reauth)
            }
            .scrollDismissesKeyboard(.interactively)
            .reauthSheet(store.securityReauth)
        }
        .plexBrowser(url: store.links.plexURL)
        .task { await store.watch() }
        .task { await store.watchLinks() }
        .task { await store.load() }
        .onDisappear { store.close() }
    }
}

private struct ServersStep: View {
    let store: WizardStore
    @Binding var editing: WizardProviderKind?

    var body: some View {
        VStack(spacing: Theme.Spacing.large) {
            FormHeading(title: L10n.tr("wizard.servers.title"), subtitle: L10n.tr("wizard.servers.lead"))
            if store.plexWaiting {
                PlexWaitingPanel { store.cancelPlex() }
            } else if let provider = editing {
                MediaServerForm(provider: provider, store: store) { editing = nil }
            } else {
                VStack(spacing: Theme.Spacing.medium) {
                    ForEach(WizardProviderKind.allCases) { provider in
                        ProviderRow(provider: provider, store: store) { editing = provider }
                    }
                }
                if store.links.plexTimedOut {
                    FormHint(text: L10n.tr("wizard.plex.timeout"), failed: true)
                }
                FormPrimaryButton(
                    title: L10n.tr(store.links.links.isEmpty ? "wizard.skip.for.now" : "wizard.continue"),
                    busy: false,
                    enabled: !store.busy
                ) {
                    store.next()
                }
            }
        }
    }
}

private struct ProviderRow: View {
    let provider: WizardProviderKind
    let store: WizardStore
    let connect: () -> Void

    var body: some View {
        let link = store.links.link(provider)
        let name = provider.title
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack {
                Text(name)
                    .font(.headline)
                Spacer(minLength: Theme.Spacing.small)
                if link != nil {
                    StatusChip(text: L10n.tr("wizard.linked.chip"), tone: .ok)
                }
            }
            if let link {
                Text(L10n.format("wizard.linked.as", link.name))
                    .font(.subheadline)
                    .foregroundStyle(Theme.textMuted)
                Button(L10n.tr("wizard.unlink"), role: .destructive) {
                    store.unlink(provider)
                }
                .buttonStyle(.bordered)
                .disabled(store.busy)
            } else {
                Text(L10n.format("wizard.sign.in.to.connect", name))
                    .font(.subheadline)
                    .foregroundStyle(Theme.textMuted)
                Button(provider == .plex ? L10n.tr("wizard.plex.sign.in") : L10n.tr("wizard.connect")) {
                    if provider == .plex {
                        store.linkPlex()
                    } else {
                        connect()
                    }
                }
                .buttonStyle(.bordered)
                .disabled(store.busy)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.large)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .accessibilityElement(children: .contain)
    }
}

private struct MediaServerForm: View {
    let provider: WizardProviderKind
    let store: WizardStore
    let close: () -> Void
    @State private var address = ""
    @State private var name = ""
    @State private var password = ""

    private var found: [WizardFoundInfo] {
        store.info.found.filter { $0.kind == provider.rawValue }
    }

    private var canLink: Bool {
        !address.trimmingCharacters(in: .whitespaces).isEmpty && !name.isEmpty && !password.isEmpty
    }

    var body: some View {
        VStack(spacing: Theme.Spacing.large) {
            Text(L10n.format("wizard.media.title", provider.title))
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(L10n.format("wizard.media.lead", provider.title))
                .font(.body)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("wizard.media.address"))
                    .font(.subheadline.weight(.semibold))
                TextField(L10n.tr("wizard.media.address.hint"), text: $address)
                    .keyboardType(.URL)
                    .textContentType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.next)
                    .formField()
                discovery
            }
            TextField(L10n.tr("signin.name"), text: $name)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .formField()
            FormPasswordField(title: L10n.tr("signin.password"), text: $password, contentType: .password, submitLabel: .go)
                .onSubmit { link() }
            FormPrimaryButton(title: L10n.tr("wizard.media.link"), busy: store.info.busy, enabled: canLink && !store.busy) {
                link()
            }
            Button(L10n.tr("common.cancel")) { close() }
                .disabled(store.info.busy)
        }
        .task { store.discover() }
        .onChange(of: store.links.link(provider) != nil) { _, linked in
            if linked { close() }
        }
    }

    @ViewBuilder
    private var discovery: some View {
        HStack {
            if store.info.discovering {
                ProgressView()
                    .controlSize(.small)
                Text(L10n.tr("wizard.media.looking"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            } else {
                Text(L10n.tr("wizard.media.found"))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.textMuted)
                Spacer(minLength: Theme.Spacing.small)
                Button(L10n.tr("wizard.media.search.again")) { store.discover() }
                    .font(.footnote.weight(.semibold))
            }
        }
        ForEach(found) { server in
            Button {
                address = server.address
            } label: {
                HStack {
                    VStack(alignment: .leading) {
                        Text(server.name.isEmpty ? provider.title : server.name)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Theme.text)
                        Text(server.address)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                    Spacer(minLength: Theme.Spacing.small)
                    if address == server.address {
                        Image(systemName: "checkmark")
                            .foregroundStyle(Theme.accentText)
                    }
                }
                .padding(Theme.Spacing.medium)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
            }
            .buttonStyle(.plain)
        }
    }

    private func link() {
        guard canLink, !store.busy else { return }
        store.linkServer(provider, address: address, name: name, password: password)
    }
}

private struct DoneStep: View {
    let store: WizardStore

    var body: some View {
        VStack(spacing: Theme.Spacing.large) {
            FormHeading(title: L10n.tr("wizard.done.title"), subtitle: L10n.tr("wizard.done.lead"))
            Text(L10n.tr("wizard.done.later"))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            FormPrimaryButton(title: L10n.tr("wizard.open.home"), busy: store.info.busy, enabled: true) {
                store.finish()
            }
            Button(L10n.tr("wizard.skip.setup")) { store.finish() }
                .disabled(store.info.busy)
            Button(L10n.tr("common.back")) { store.back() }
                .disabled(store.info.busy)
        }
    }
}

extension WizardProviderKind {
    var title: String {
        switch self {
        case .plex: L10n.tr("provider.plex")
        case .jellyfin: L10n.tr("provider.jellyfin")
        case .emby: L10n.tr("provider.emby")
        }
    }
}

enum WizardErrorText {
    static func text(_ error: ErrorInfo) -> String {
        let message = error.message.lowercased()
        switch error.kind {
        case .forbidden:
            return message.contains("admin") ? L10n.tr("wizard.error.not.admin") : AccountErrorText.text(error)
        case .badRequest:
            return message.contains("https") ? L10n.tr("wizard.error.https") : L10n.tr("wizard.error.address")
        case .conflict:
            return L10n.tr("wizard.error.conflict")
        case .server, .invalidResponse:
            return L10n.tr("wizard.error.unreachable")
        case .unknown:
            return error.message.isEmpty ? error.localized : error.message
        default:
            return AccountErrorText.text(error)
        }
    }
}
