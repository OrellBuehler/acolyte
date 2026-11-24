import SwiftUI

struct SettingsHomeView: View {
    let account: AccountInfo
    @State private var store = SettingsHomeStore()
    @State private var query = ""
    @Environment(\.openURL) private var openURL

    private var texts: [SettingsSearchText] {
        store.info.pages.map {
            SettingsSearchText(
                key: $0.rawValue,
                label: L10n.tr($0.labelKey),
                about: L10n.tr($0.aboutKey),
                group: L10n.tr(SettingsHomeView.groupKey(of: $0, in: store.info).titleKey)
            )
        }
    }

    private var found: [SettingsPageID] {
        store.search(query, texts: texts)
    }

    var body: some View {
        content
            .navigationTitle(SectionID.settings.title)
            .searchable(text: $query, prompt: L10n.tr("settings.home.search"))
            .task { await store.watch() }
            .task { await store.load() }
            .task { await store.followLive() }
    }

    @ViewBuilder
    private var content: some View {
        if !store.info.allowed {
            if store.info.load.phase == .loading {
                LoadingStateView()
            } else {
                EmptyStateView(
                    title: L10n.tr("settings.home.not.allowed"),
                    message: L10n.tr("settings.home.not.allowed.text"),
                    systemImage: "lock"
                )
            }
        } else if query.trimmingCharacters(in: .whitespaces).isEmpty {
            overview
        } else {
            results
        }
    }

    private var overview: some View {
        List {
            if store.info.showFinishSetup {
                Section {
                    Button { finishSetup() } label: {
                        Label {
                            VStack(alignment: .leading) {
                                Text(L10n.tr("settings.home.finish.setup")).font(.headline)
                                Text(L10n.tr("settings.home.finish.setup.text"))
                                    .font(.footnote)
                                    .foregroundStyle(Theme.textMuted)
                            }
                        } icon: {
                            Image(systemName: "slider.horizontal.3")
                        }
                    }
                }
            }
            if !store.info.reasons.isEmpty {
                Section {
                    ForEach(store.info.reasons) { SettingsReasonRow(reason: $0) }
                } header: {
                    Text(L10n.plural("settings.home.attention.count", store.info.reasons.count))
                }
            }
            ForEach(store.info.groups) { group in
                Section {
                    ForEach(group.pages) { SettingsPageRow(entry: $0) }
                } header: {
                    SettingsGroupHeader(group: group)
                } footer: {
                    Text(L10n.tr(group.group.lineKey))
                }
            }
            if !store.info.accountPages.isEmpty {
                Section {
                    ForEach(store.info.accountPages) { page in
                        SettingsPageRow(entry: SettingsHomePageInfo(page: page, standing: .unknown, count: 0))
                    }
                } header: {
                    Text(L10n.tr(SettingsGroupID.account.titleKey))
                }
            }
        }
        .refreshable { await store.refresh() }
    }

    @ViewBuilder
    private var results: some View {
        let hits = found
        if hits.isEmpty {
            EmptyStateView(
                title: L10n.tr("settings.home.search.empty.title"),
                message: L10n.tr("settings.home.search.empty.text"),
                systemImage: "magnifyingglass"
            )
        } else {
            List(hits) { page in
                SettingsLink(page: page) {
                    HStack(spacing: Theme.Spacing.medium) {
                        SettingsPageIcon(page: page)
                        VStack(alignment: .leading) {
                            Text(L10n.tr(page.labelKey))
                            Text(L10n.tr(SettingsHomeView.groupKey(of: page, in: store.info).titleKey))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                    }
                }
            }
        }
    }

    private func finishSetup() {
        let base = account.serverURL.hasSuffix("/") ? String(account.serverURL.dropLast()) : account.serverURL
        if let url = URL(string: base + "/welcome?first=1") {
            openURL(url)
        }
    }

    nonisolated static func groupKey(of page: SettingsPageID, in info: SettingsHomeInfo) -> SettingsGroupID {
        if info.accountPages.contains(page) { return .account }
        return info.groups.first { group in group.pages.contains { $0.page == page } }?.group ?? .library
    }
}

struct SettingsLink<Label: View>: View {
    let page: SettingsPageID
    @ViewBuilder var label: () -> Label

    var body: some View {
        if let route = page.route {
            NavigationLink(value: route, label: label)
        } else if let section = page.section {
            NavigationLink(value: section, label: label)
        }
    }
}

private struct SettingsPageIcon: View {
    let page: SettingsPageID

    var body: some View {
        Image(systemName: page.symbol)
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(Theme.accentText)
            .frame(width: 32, height: 32)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct SettingsPageRow: View {
    let entry: SettingsHomePageInfo

    var body: some View {
        SettingsLink(page: entry.page) {
            HStack(spacing: Theme.Spacing.medium) {
                SettingsPageIcon(page: entry.page)
                VStack(alignment: .leading) {
                    Text(L10n.tr(entry.page.labelKey))
                    Text(L10n.tr(entry.page.aboutKey))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Spacer(minLength: Theme.Spacing.small)
                status
            }
        }
    }

    @ViewBuilder
    private var status: some View {
        switch entry.standing {
        case .attention:
            HStack(spacing: Theme.Spacing.xSmall) {
                Circle().fill(Theme.fail).frame(width: 8, height: 8)
                Text(L10n.plural("settings.home.attention.count", entry.count))
                    .font(.caption)
                    .foregroundStyle(Theme.failText)
            }
        case .unset:
            Text(L10n.tr("settings.home.unset"))
                .font(.caption)
                .foregroundStyle(Theme.waitText)
        default:
            EmptyView()
        }
    }
}

private struct SettingsGroupHeader: View {
    let group: SettingsHomeGroupInfo

    var body: some View {
        HStack {
            Text(L10n.tr(group.group.titleKey))
            Spacer()
            switch group.standing {
            case .attention:
                Text(L10n.plural("settings.home.attention.count", group.count))
                    .foregroundStyle(Theme.failText)
            case .unset:
                Text(L10n.tr("settings.home.unset"))
                    .foregroundStyle(Theme.waitText)
            default:
                EmptyView()
            }
        }
    }
}

private struct SettingsReasonRow: View {
    let reason: SettingsReasonInfo

    private var title: String {
        switch reason.kind {
        case .setting: reason.title
        case .update: L10n.tr("settings.home.update.title")
        case .decisions: L10n.format("settings.home.decisions", reason.count)
        case .requests: L10n.format("settings.home.requests", reason.count)
        }
    }

    private var bodyText: String {
        switch reason.kind {
        case .setting: reason.body
        case .update: L10n.format("settings.home.update.body", reason.body)
        default: ""
        }
    }

    var body: some View {
        switch reason.kind {
        case .decisions:
            NavigationLink(value: Route.downloads(show: "problems.decide", open: "")) { row }
        case .requests:
            NavigationLink(value: SectionID.requests) { row }
        default:
            if let page = reason.page {
                SettingsLink(page: page) { row }
            } else {
                row
            }
        }
    }

    private var row: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            Circle().fill(Theme.dotColor(reason.tone)).frame(width: 10, height: 10).padding(.top, 5)
            VStack(alignment: .leading) {
                Text(title)
                if !bodyText.isEmpty {
                    Text(bodyText)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }
}
