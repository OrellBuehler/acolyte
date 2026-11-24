import SwiftUI

struct SettingsSignInPasswordsSection: View {
    let store: SettingsSignInStore
    private var info: SettingsSignInInfo { store.info }

    var body: some View {
        Section {
            SettingsSignInField(
                title: L10n.tr("settings.signin.shortest"),
                text: info.minLengthText,
                onChange: { store.setMinLength($0) },
                hint: L10n.tr("settings.signin.shortest.hint"),
                keyboard: .numberPad,
                disabled: !info.isOwner
            )
            if info.shortPasswordWarning {
                SettingsSignInNote(text: L10n.tr("settings.signin.short.warn"))
            }
            if info.isOwner {
                Button(L10n.tr("settings.signin.save")) { store.saveMinLength() }
                    .disabled(!info.canSaveMinLength)
            } else {
                SettingsSignInHint(text: L10n.tr("settings.signin.owner.only"))
            }
        } header: {
            Text(L10n.tr("settings.signin.passwords"))
        }
    }
}

struct SettingsSignInSwitchSections: View {
    let store: SettingsSignInStore
    private var info: SettingsSignInInfo { store.info }

    var body: some View {
        Section {
            Toggle(
                L10n.tr("settings.signin.admins.must"),
                isOn: Binding(get: { info.requireTwoFactor }, set: { store.setRequireTwoFactor($0) })
            )
            .disabled(!info.isOwner || info.requireTwoFactorSaving)
        } header: {
            Text(L10n.tr("settings.signin.two.factor"))
        } footer: {
            Text(L10n.tr("settings.signin.admins.note") + (info.isOwner ? "" : " " + L10n.tr("settings.signin.only.owner.change")))
        }
        Section {
            Toggle(
                L10n.tr("settings.signin.forgot.on"),
                isOn: Binding(get: { info.forgotLink }, set: { store.setForgotLink($0) })
            )
            .disabled(!info.isOwner || info.forgotLinkSaving)
        } header: {
            Text(L10n.tr("settings.signin.forgot.title"))
        } footer: {
            Text(L10n.tr("settings.signin.forgot.note") + (info.isOwner ? "" : " " + L10n.tr("settings.signin.only.owner.change")))
        }
    }
}

struct SettingsSignInWaySection: View {
    let store: SettingsSignInStore
    let id: SettingsSignInWayID
    @Binding var confirmUnlink: Bool
    @State private var showServerId = false

    private var info: SettingsSignInInfo { store.info }

    var body: some View {
        if let way = info.way(id) {
            Section {
                SettingsSignInHint(
                    text: id == .plex ? L10n.tr("settings.signin.plex.intro") : L10n.format("settings.signin.other.intro", id.app)
                )
                Toggle(
                    L10n.format("settings.signin.show.button", id.app),
                    isOn: Binding(get: { way.on }, set: { store.setWayOn(id, $0) })
                )
                if id == .plex {
                    plexLink(way)
                } else {
                    address(way)
                }
                joinPicker(way)
                if way.join == .list {
                    SettingsSignInArea(
                        title: L10n.tr("settings.signin.names"),
                        text: way.allow,
                        onChange: { store.setWayAllow(id, $0) },
                        monospaced: false
                    )
                }
                actions(way)
            } header: {
                Text(L10n.format("settings.signin.sign.in.with", id.app))
            }
            .disabled(!info.isOwner)
        }
    }

    @ViewBuilder
    private func joinPicker(_ way: SettingsSignInWayInfo) -> some View {
        Text(L10n.tr("settings.signin.who.join"))
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
        ForEach(SettingsSignInJoinKind.allCases) { join in
            Button {
                store.setWayJoin(id, join)
            } label: {
                HStack(spacing: Theme.Spacing.medium) {
                    Text(SettingsSignInWords.join(join, id))
                        .foregroundStyle(Theme.text)
                        .multilineTextAlignment(.leading)
                    Spacer(minLength: 0)
                    if way.join == join {
                        Image(systemName: "checkmark")
                            .foregroundStyle(Theme.accentText)
                    }
                }
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(way.join == join ? .isSelected : [])
        }
        SettingsSignInHint(text: L10n.tr(id == .plex ? "settings.signin.join.hint.plex" : "settings.signin.join.hint"))
    }

    @ViewBuilder
    private func actions(_ way: SettingsSignInWayInfo) -> some View {
        if info.isOwner {
            HStack(spacing: Theme.Spacing.medium) {
                Button(L10n.tr("settings.signin.save")) { store.saveWay(id, confirmed: false) }
                    .buttonStyle(.borderedProminent)
                    .disabled(way.saving)
                Button(L10n.tr("settings.signin.import.members")) { store.openImport(id) }
                    .buttonStyle(.bordered)
                    .disabled(!info.canImport(id))
                if way.saving { ProgressView() }
            }
            if id == .plex, !info.plex.canImport {
                SettingsSignInHint(text: L10n.tr("settings.signin.link.first"))
            }
        } else {
            SettingsSignInHint(text: L10n.tr("settings.signin.owner.only"))
        }
    }

    @ViewBuilder
    private func address(_ way: SettingsSignInWayInfo) -> some View {
        SettingsSignInField(
            title: L10n.format("settings.signin.server.address", id.app),
            text: way.url,
            onChange: { store.setWayUrl(id, $0) },
            prompt: "http://10.0.0.5:8096",
            monospaced: true,
            keyboard: .URL
        )
        HStack(spacing: Theme.Spacing.medium) {
            Button(L10n.tr("settings.signin.test")) { store.testWay(id) }
                .buttonStyle(.bordered)
                .disabled(way.url.isEmpty || way.testing)
            if way.testing { ProgressView() }
        }
        if let test = way.test {
            SettingsSignInTestView(test: test, okLabel: test.text)
        } else if way.untested {
            SettingsSignInNote(text: L10n.tr("settings.signin.not.tested"))
        }
    }

    @ViewBuilder
    private func plexLink(_ way: SettingsSignInWayInfo) -> some View {
        let plex = info.plex
        if plex.linked {
            Text(L10n.format("settings.signin.linked.to", plex.name))
        } else {
            SettingsSignInHint(text: L10n.tr("settings.signin.link.note"))
        }
        if info.isOwner {
            HStack(spacing: Theme.Spacing.medium) {
                if plex.linked {
                    Button(L10n.tr("settings.signin.unlink")) { confirmUnlink = true }
                        .buttonStyle(.bordered)
                }
                Button(L10n.tr(plex.linked ? "settings.signin.link.again" : "settings.signin.link.mine")) { store.linkPlex() }
                    .buttonStyle(.borderedProminent)
            }
            .disabled(plex.linking || plex.unlinking)
        }
        plexStatus(plex)
        if !plex.servers.isEmpty {
            Picker(
                L10n.tr("settings.signin.server.share"),
                selection: Binding(get: { way.serverId }, set: { store.setWayServerId(id, $0) })
            ) {
                Text(L10n.tr("settings.signin.any.server")).tag("")
                ForEach(plex.servers) { server in Text(server.name).tag(server.id) }
                if !way.serverId.isEmpty, !plex.servers.contains(where: { $0.id == way.serverId }) {
                    Text(way.serverId).tag(way.serverId)
                }
            }
        }
        DisclosureGroup(L10n.tr("settings.signin.type.id"), isExpanded: $showServerId) {
            SettingsSignInField(
                title: L10n.tr("settings.signin.server.id"),
                text: way.serverId,
                onChange: { store.setWayServerId(id, $0) },
                hint: L10n.tr("settings.signin.server.id.hint"),
                monospaced: true
            )
        }
    }

    @ViewBuilder
    private func plexStatus(_ plex: SettingsSignInPlexInfo) -> some View {
        switch plex.phase {
        case .idle:
            EmptyView()
        case .starting:
            HStack(spacing: Theme.Spacing.small) {
                ProgressView()
                SettingsSignInHint(text: L10n.tr("settings.signin.link.starting"))
            }
        case .waiting:
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                HStack(spacing: Theme.Spacing.small) {
                    ProgressView()
                    SettingsSignInHint(text: L10n.tr("settings.signin.link.confirm"))
                }
                HStack(spacing: Theme.Spacing.medium) {
                    Button(L10n.tr("settings.signin.link.open")) {
                        if let url = URL(string: plex.authUrl) { openURL(url) }
                    }
                    .buttonStyle(.bordered)
                    Button(L10n.tr("settings.signin.link.stop")) { store.stopLinking() }
                        .buttonStyle(.bordered)
                }
            }
        case .linkedpick:
            SettingsSignInHint(text: L10n.tr("settings.signin.linked.pick"))
        case .linkednone:
            SettingsSignInHint(text: L10n.tr("settings.signin.linked.none"))
        case .timedout:
            SettingsSignInFail(text: L10n.tr("settings.signin.link.timed.out"))
        case .failed:
            SettingsSignInFail(text: SettingsSignInWords.linkFailure(plex))
        }
    }

    @Environment(\.openURL) private var openURL
}
