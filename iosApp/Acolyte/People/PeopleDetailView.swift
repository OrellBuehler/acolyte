import SwiftUI

private enum DetailMode: Hashable {
    case idle
    case change
    case password
    case server
}

private enum DetailConfirm: Hashable, Identifiable {
    case turnOff
    case resetTwoFactor
    case delete
    case sso(MySsoLinkInfo)

    var id: Self { self }
}

struct PeopleDetailView: View {
    let store: PeopleStore
    let userId: Int64
    @Environment(\.dismiss) private var dismiss
    @State private var mode: DetailMode = .idle
    @State private var admin = false
    @State private var perms: Set<String> = []
    @State private var rating = ""
    @State private var password = ""
    @State private var confirm: DetailConfirm?

    var body: some View {
        let info = store.info
        let user = info.user(userId)
        let name = user?.name ?? ""
        Group {
            if let user {
                content(user, info)
            } else {
                EmptyStateView()
            }
        }
        .navigationTitle(name)
        .navigationBarTitleDisplayMode(.inline)
        .noticeToast(store.notices)
        .reauthSheet(store.reauth)
        .confirmationDialog(
            confirmTitle(confirm, name),
            isPresented: Binding(get: { confirm != nil }, set: { if !$0 { confirm = nil } }),
            titleVisibility: .visible,
            presenting: confirm
        ) { item in
            Button(confirmButton(item), role: .destructive) { perform(item) }
        } message: { item in
            Text(confirmMessage(item))
        }
        .confirmationDialog(
            L10n.format("people.confirm.no.way.in", info.noWayIn?.userName ?? ""),
            isPresented: Binding(get: { info.noWayIn != nil }, set: { if !$0 { store.dismissNoWayIn() } }),
            titleVisibility: .visible,
            presenting: info.noWayIn
        ) { ask in
            Button(L10n.tr("people.confirm.no.way.in.button"), role: .destructive) {
                store.unlinkSsoConfirmed(userId: ask.userId, link: ask.link)
            }
        }
        .onAppear { store.setDetailOpen(true) }
        .onDisappear { store.setDetailOpen(false) }
        .onChange(of: user == nil) { _, gone in
            if gone { dismiss() }
        }
        .onChange(of: store.successSerial) { _, _ in
            mode = .idle
            password = ""
        }
    }

    private func content(_ user: PeopleUserInfo, _ info: PeopleListInfo) -> some View {
        List {
            if let error = info.actionError {
                NoticeBanner(text: MyErrorText.text(error))
                    .listRowSeparator(.hidden)
            }
            if let problem = info.problem {
                NoticeBanner(text: PeopleStore.problemText(problem, minimum: info.minPasswordLength, unlimited: false))
                    .listRowSeparator(.hidden)
            }
            Section {
                HStack(alignment: .top, spacing: Theme.Spacing.large) {
                    PersonAvatar(source: user.avatar, name: user.name, side: 56)
                    VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                        Text(user.name)
                            .font(.title3.weight(.semibold))
                        PeopleChips(user: user)
                    }
                }
                .padding(.vertical, Theme.Spacing.xSmall)
                .accessibilityElement(children: .combine)
            }
            infoSection(user)
            if user.mayChange {
                actionsSection(user, info)
            }
            switch mode {
            case .idle: EmptyView()
            case .change: changeSection(user, info)
            case .password: passwordSection(user, info)
            case .server: serverSection(user, info)
            }
            if user.maySeeDevices {
                Section {
                    NavigationLink {
                        MyDevicesView(userId: user.id, title: L10n.format("people.devices.title", user.name), personName: user.name)
                    } label: {
                        Label(L10n.tr("people.action.devices"), systemImage: "iphone.and.ipad")
                    }
                }
            }
            if info.viewerOwner {
                ownerSections(user, info)
            }
        }
    }

    private func infoSection(_ user: PeopleUserInfo) -> some View {
        Section {
            LabeledContent(L10n.tr("account.role"), value: PeopleText.role(user.role))
            LabeledContent(L10n.tr("people.detail.permissions"), value: PeopleText.permissionLine(user))
            if user.isKid {
                LabeledContent(L10n.tr("people.rating"), value: L10n.format("people.detail.rating.up.to", user.maxRating))
            }
            ForEach(user.links, id: \.provider) { link in
                LabeledContent(PeopleText.provider(link.provider), value: link.name)
            }
            LabeledContent(L10n.tr("people.detail.last.seen"), value: PeopleText.seenLine(user))
            if !user.offReason.isEmpty {
                Text(L10n.format("people.removed.on", PeopleText.provider(user.offReason)))
                    .font(.footnote)
                    .foregroundStyle(Theme.waitText)
            }
        }
    }

    private func actionsSection(_ user: PeopleUserInfo, _ info: PeopleListInfo) -> some View {
        Section {
            Button(L10n.tr("people.action.change")) {
                if mode == .change {
                    mode = .idle
                } else {
                    admin = user.role == .admin
                    perms = Set(user.perms)
                    rating = user.maxRating
                    mode = .change
                }
            }
            Button(L10n.tr("people.action.set.password")) {
                password = ""
                mode = mode == .password ? .idle : .password
            }
            if user.twoFactor {
                Button(L10n.tr("people.action.reset.two.factor")) { confirm = .resetTwoFactor }
            }
            if user.off {
                Button(L10n.tr("people.action.turn.on")) { store.turnOn(user.id) }
            } else {
                Button(L10n.tr("people.action.turn.off")) { confirm = .turnOff }
            }
            Button(L10n.tr("people.action.delete"), role: .destructive) { confirm = .delete }
        }
        .disabled(info.busy)
    }

    private func changeSection(_ user: PeopleUserInfo, _ info: PeopleListInfo) -> some View {
        Section {
            PeopleAccessEditor(
                admin: $admin,
                perms: $perms,
                rating: $rating,
                grantable: info.grantable,
                canSetRole: info.viewerOwner
            )
            Button(L10n.tr("myaccount.save")) {
                let kept = PeopleRules.perms.filter { perms.contains($0) || (user.perms.contains($0) && !info.grantable.contains($0)) }
                store.save(userId: user.id, admin: admin, perms: kept, rating: admin ? "" : rating)
            }
            .disabled(info.busy)
            Button(L10n.tr("common.cancel"), role: .cancel) { mode = .idle }
        } header: {
            Text(L10n.format("people.change.title", user.name))
        }
    }

    private func passwordSection(_ user: PeopleUserInfo, _ info: PeopleListInfo) -> some View {
        let valid = SDKClient.shared.validPassword(password, minimum: info.minPasswordLength)
        return Section {
            SecureField(L10n.tr("myaccount.password.new"), text: $password)
                .textContentType(.newPassword)
            if !password.isEmpty && !valid {
                FormHint(text: L10n.format("myaccount.password.short", info.minPasswordLength), failed: true)
            }
            Button(L10n.tr("people.password.set")) {
                store.setPassword(userId: user.id, password: password)
            }
            .disabled(!valid || info.busy)
            Button(L10n.tr("common.cancel"), role: .cancel) {
                password = ""
                mode = .idle
            }
        } header: {
            Text(L10n.format("people.password.title", user.name))
        } footer: {
            Text(L10n.format("people.password.note", user.name))
        }
    }

    private func serverSection(_ user: PeopleUserInfo, _ info: PeopleListInfo) -> some View {
        Section {
            if info.servers.isEmpty {
                Text(L10n.tr("people.server.none"))
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.servers) { server in
                Text(L10n.format("people.server.name", server.name, PeopleText.provider(server.kind)))
                    .font(.subheadline.weight(.semibold))
                if server.failed {
                    Text(L10n.format("people.server.failed", PeopleText.provider(server.kind)))
                        .font(.footnote)
                        .foregroundStyle(Theme.failText)
                }
                ForEach(server.accounts) { account in
                    Button {
                        store.linkMedia(userId: user.id, serverId: server.id, accountId: account.id)
                    } label: {
                        HStack {
                            Text(account.name)
                            if account.takenBy != 0 {
                                Text(L10n.format("people.server.taken", account.takenName))
                                    .font(.footnote)
                                    .foregroundStyle(Theme.textMuted)
                            }
                        }
                    }
                    .disabled(account.takenBy != 0 || info.busy)
                }
            }
            Button(L10n.tr("common.cancel"), role: .cancel) { mode = .idle }
        } header: {
            Text(L10n.tr("people.server.pick"))
        }
    }

    @ViewBuilder
    private func ownerSections(_ user: PeopleUserInfo, _ info: PeopleListInfo) -> some View {
        if !user.sso.isEmpty {
            Section {
                ForEach(user.sso) { link in
                    HStack {
                        Text(L10n.format("myaccount.sso.as", PeopleText.ssoKind(link.kind), link.issuer, link.name))
                        Spacer(minLength: Theme.Spacing.small)
                        Button(L10n.tr("wizard.unlink"), role: .destructive) { confirm = .sso(link) }
                            .buttonStyle(.bordered)
                            .controlSize(.small)
                            .disabled(info.busy)
                    }
                }
            } header: {
                Text(L10n.tr("people.sso.title"))
            }
        }
        Section {
            ForEach(user.links.filter { $0.provider != "plex" }, id: \.provider) { link in
                HStack {
                    Text(L10n.format("myaccount.media.as", PeopleText.provider(link.provider), link.name))
                    Spacer(minLength: Theme.Spacing.small)
                    Button(L10n.tr("wizard.unlink"), role: .destructive) {
                        store.unlinkMedia(userId: user.id, kind: link.provider)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .disabled(info.busy)
                }
            }
            Button(L10n.tr("people.server.link")) {
                mode = .server
                store.loadServers()
            }
            .disabled(info.busy)
        } header: {
            Text(L10n.tr("people.server.account"))
        }
    }

    private func perform(_ item: DetailConfirm) {
        switch item {
        case .turnOff: store.turnOff(userId)
        case .resetTwoFactor: store.resetTwoFactor(userId)
        case .delete: store.delete(userId)
        case let .sso(link): store.unlinkSso(userId: userId, link: link)
        }
    }

    private func confirmTitle(_ item: DetailConfirm?, _ name: String) -> String {
        switch item {
        case .turnOff: L10n.format("people.confirm.turn.off", name)
        case .resetTwoFactor: L10n.format("people.confirm.reset.two.factor", name)
        case .delete: L10n.format("people.confirm.delete", name)
        case let .sso(link): L10n.format("people.confirm.sso", name, PeopleText.ssoKind(link.kind))
        case nil: ""
        }
    }

    private func confirmButton(_ item: DetailConfirm) -> String {
        switch item {
        case .turnOff: L10n.tr("people.action.turn.off")
        case .resetTwoFactor: L10n.tr("people.confirm.reset.button")
        case .delete: L10n.tr("people.confirm.delete.button")
        case .sso: L10n.tr("wizard.unlink")
        }
    }

    private func confirmMessage(_ item: DetailConfirm) -> String {
        switch item {
        case .turnOff: L10n.tr("people.confirm.turn.off.message")
        case .resetTwoFactor: L10n.tr("people.confirm.reset.two.factor.message")
        case .delete: L10n.tr("people.confirm.delete.message")
        case .sso: L10n.tr("people.confirm.sso.message")
        }
    }
}
