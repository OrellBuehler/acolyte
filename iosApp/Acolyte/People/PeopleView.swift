import SwiftUI
import UIKit

struct PeopleView: View {
    @State private var store = PeopleStore()
    @State private var showInvite = false

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyStateView()
        } content: {
            List {
                if let error = info.actionError {
                    NoticeBanner(text: MyErrorText.text(error))
                        .listRowSeparator(.hidden)
                }
                if let fresh = info.fresh {
                    freshSection(fresh)
                }
                Section {
                    ForEach(info.users) { user in
                        NavigationLink {
                            PeopleDetailView(store: store, userId: user.id)
                        } label: {
                            PeopleRow(user: user)
                        }
                    }
                } header: {
                    Text(L10n.tr("people.list"))
                }
                invitesSection(info)
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(SectionID.people.title)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(L10n.tr("people.invite.someone"), systemImage: "person.badge.plus") {
                    showInvite = true
                }
            }
        }
        .sheet(isPresented: $showInvite) {
            PeopleInviteSheet(store: store)
        }
        .noticeToast(store.notices)
        .reauthSheet(store.reauth, enabled: !showInvite && !store.detailOpen)
        .task { await store.watch() }
        .task { await store.load() }
    }

    private func freshSection(_ fresh: PeopleFreshInfo) -> some View {
        Section {
            Text(fresh.url)
                .font(.callout.monospaced())
                .textSelection(.enabled)
            HStack(spacing: Theme.Spacing.small) {
                Button(L10n.tr("myaccount.copy")) {
                    UIPasteboard.general.string = fresh.url
                    store.notices.show(L10n.tr("people.link.copied"))
                }
                .buttonStyle(.bordered)
                ShareLink(item: fresh.url)
                    .buttonStyle(.bordered)
                Spacer(minLength: 0)
                Button(L10n.tr("myaccount.close")) { store.dismissInvite() }
                    .buttonStyle(.borderedProminent)
            }
        } header: {
            Text(L10n.tr("people.fresh.title"))
        } footer: {
            Text(L10n.tr(fresh.singleUse ? "people.fresh.single" : "people.fresh.multi"))
        }
    }

    private func invitesSection(_ info: PeopleListInfo) -> some View {
        Section {
            if info.invites.isEmpty {
                Text(L10n.tr("people.invites.none"))
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.invites) { invite in
                PeopleInviteRow(invite: invite)
                    .swipeActions(edge: .trailing) {
                        if invite.state == .open {
                            Button(L10n.tr("people.invite.revoke"), role: .destructive) {
                                store.revokeInvite(invite.id)
                            }
                        }
                    }
            }
        } header: {
            Text(L10n.tr("people.invites"))
        }
    }
}

private struct PeopleRow: View {
    let user: PeopleUserInfo

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            PersonAvatar(source: user.avatar, name: user.name, side: 40)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(user.name)
                    .font(.headline)
                PeopleChips(user: user)
                if !user.links.isEmpty {
                    Text(PeopleText.linkLine(user))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Text(PeopleText.permissionLine(user) + " · " + PeopleText.seenLine(user))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                if !user.offReason.isEmpty {
                    Text(L10n.format("people.removed.on", PeopleText.provider(user.offReason)))
                        .font(.footnote)
                        .foregroundStyle(Theme.waitText)
                }
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
        .accessibilityElement(children: .combine)
    }
}

struct PeopleChips: View {
    let user: PeopleUserInfo

    var body: some View {
        AccountFlowLayout(spacing: Theme.Spacing.small) {
            StatusChip(text: PeopleText.role(user.role), tone: user.role == .member ? .idle : .info)
            if user.off {
                StatusChip(text: L10n.tr("people.chip.off"), tone: .wait)
            }
            if user.twoFactor {
                StatusChip(text: L10n.tr("people.chip.two.factor"), tone: .ok)
            }
            if user.isKid {
                StatusChip(text: L10n.format("people.chip.kid", user.maxRating), tone: .info)
            }
        }
    }
}

private struct PeopleInviteRow: View {
    let invite: PeopleInviteInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.headline)
            AccountFlowLayout(spacing: Theme.Spacing.small) {
                StatusChip(text: PeopleText.role(role), tone: role == .member ? .idle : .info)
                if !invite.maxRating.isEmpty {
                    StatusChip(text: L10n.format("people.chip.kid", invite.maxRating), tone: .info)
                }
            }
            if !invite.createdBy.isEmpty {
                Text(madeBy)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            Text(stateLine)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if let joinedLine {
                Text(joinedLine)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
        .accessibilityElement(children: .combine)
    }

    private var role: RoleKind {
        RoleKind(rawValue: invite.role) ?? .member
    }

    private var title: String {
        invite.note.isEmpty ? L10n.format("people.invite.title", PeopleText.role(role)) : invite.note
    }

    private var madeBy: String {
        if let created = invite.createdAt {
            return L10n.format("people.invite.made.by.when", invite.createdBy, Format.relative(created))
        }
        return L10n.format("people.invite.made.by", invite.createdBy)
    }

    private var stateLine: String {
        switch invite.state {
        case .used:
            if let used = invite.usedAt {
                return L10n.format("people.invite.used.when", invite.usedBy, Format.relative(used))
            }
            return L10n.format("people.invite.used", invite.usedBy)
        case .ended:
            return L10n.tr("people.invite.ended")
        case .open:
            if invite.neverExpires { return L10n.tr("people.invite.open.forever") }
            if let until = invite.expiresAt {
                return L10n.format("people.invite.open.until", until.formatted(date: .abbreviated, time: .omitted))
            }
            return L10n.tr("people.invite.open.forever")
        }
    }

    private var joinedLine: String? {
        guard invite.maxUses != 1, invite.uses > 0 || !invite.joined.isEmpty else { return nil }
        var line = invite.maxUses > 0
            ? L10n.format("people.invite.joined.of", invite.uses, invite.maxUses)
            : L10n.format("people.invite.joined", invite.uses)
        if !invite.joined.isEmpty {
            line += " (" + invite.joined.joined(separator: ", ") + ")"
        }
        return line
    }
}
