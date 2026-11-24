import SwiftUI

struct MoreView: View {
    let account: AccountInfo
    @Environment(AppModel.self) private var model
    @State private var confirmSignOut = false
    @State private var badges = DownloadBadgesStore()
    @State private var settingsHome = SettingsHomeStore()

    private var showsDownloads: Bool {
        account.tabs.contains(.downloads) || account.moreGroups.contains { $0.sections.contains(.downloads) }
    }

    var body: some View {
        List {
            if badges.badge.decide > 0 && showsDownloads {
                Section {
                    NavigationLink(value: Route.downloads(show: "problems.decide", open: "")) {
                        Text(L10n.format("downloads.badge.decide", badges.badge.decide))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Theme.failText)
                    }
                }
            }
            ForEach(account.moreGroups) { group in
                Section {
                    ForEach(group.sections) { section in
                        NavigationLink(value: section) {
                            row(section)
                        }
                        .badge(section == .requests ? model.inbox.requestsBadge : 0)
                    }
                } header: {
                    if let title = group.group.title {
                        Text(title)
                    }
                }
            }
            Section {
                Button(L10n.tr("account.sign.out"), role: .destructive) {
                    confirmSignOut = true
                }
            }
        }
        .navigationTitle(account.moreTab.title)
        .task { await badges.watch() }
        .task { await badges.follow() }
        .task { await settingsHome.watch() }
        .task { await settingsHome.load() }
        .task { await settingsHome.followLive() }
        .confirmationDialog(L10n.tr("account.sign.out.title"), isPresented: $confirmSignOut, titleVisibility: .visible) {
            Button(L10n.tr("account.sign.out"), role: .destructive) { model.signOut() }
        } message: {
            Text(L10n.tr("account.sign.out.message"))
        }
    }

    @ViewBuilder
    private func row(_ section: SectionID) -> some View {
        if section == .account {
            Label {
                VStack(alignment: .leading) {
                    Text(section.title)
                    Text(L10n.format("account.summary", account.displayName, account.serverName))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            } icon: {
                Image(systemName: section.symbol)
            }
        } else if section == .downloads {
            HStack {
                Label(section.title, systemImage: section.symbol)
                Spacer(minLength: Theme.Spacing.small)
                DownloadsBadgeLabel(badge: badges.badge)
            }
        } else if section == .settings {
            HStack {
                Label(section.title, systemImage: section.symbol)
                Spacer(minLength: Theme.Spacing.small)
                if settingsHome.info.needsAttention {
                    Circle().fill(Theme.fail).frame(width: 8, height: 8)
                }
            }
        } else {
            Label(section.title, systemImage: section.symbol)
        }
    }
}

private struct DownloadsBadgeLabel: View {
    let badge: DownloadsBadgeInfo

    var body: some View {
        switch badge.kind {
        case .active:
            HStack(spacing: Theme.Spacing.xSmall) {
                ProgressView(value: Double(min(max(badge.percent, 0), 100)) / 100)
                    .progressViewStyle(.circular)
                    .tint(Theme.accent)
                    .frame(width: 16, height: 16)
                Text(L10n.format("downloads.badge.active", badge.active, badge.percent))
                    .font(.caption)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        case .decide:
            Text(L10n.format("downloads.badge.decide", badge.decide))
                .font(.caption)
                .foregroundStyle(Theme.failText)
                .lineLimit(1)
        case .none:
            EmptyView()
        }
    }
}
