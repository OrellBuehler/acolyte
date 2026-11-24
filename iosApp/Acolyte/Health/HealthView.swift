import SwiftUI

enum StatusPageTab: String, CaseIterable, Identifiable {
    case status
    case jobs
    case updates
    case support
    case about

    var id: String { rawValue }

    var title: String {
        switch self {
        case .status: L10n.tr("settings.status.tab.status")
        case .jobs: L10n.tr("settings.status.tab.jobs")
        case .updates: L10n.tr("settings.status.tab.updates")
        case .support: L10n.tr("settings.status.tab.support")
        case .about: L10n.tr("settings.status.tab.about")
        }
    }
}

struct HealthView: View {
    @State private var store = HealthStore()
    @State private var tab: StatusPageTab = .status
    @State private var toast: String?
    @State private var toastToken = 0

    private var info: HealthInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                EmptyStateView(
                    title: L10n.tr("settings.status.not.allowed"),
                    message: L10n.tr("settings.status.not.allowed.text"),
                    systemImage: "lock"
                )
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    page
                }
            }
        }
        .navigationTitle(SectionID.health.title)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                if info.allowed && info.canAct && tab != .updates {
                    if info.isBusy("all") {
                        ProgressView()
                    } else {
                        Button(L10n.tr("settings.status.check.again"), systemImage: "arrow.clockwise") { store.checkAll() }
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let toast {
                NoteToast(text: toast, undo: nil)
            }
        }
        .sheet(item: bundleBinding) { file in
            ActivityShareSheet(items: [URL(fileURLWithPath: file.path)])
        }
        .onChange(of: info.notice) { _, notice in
            guard let notice else { return }
            toast = StatusText.notice(notice)
            toastToken = notice.id
            store.dismissNotice()
            let token = notice.id
            Task {
                try? await Task.sleep(for: .seconds(4))
                if toastToken == token { toast = nil }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private var bundleBinding: Binding<StatusFileInfo?> {
        Binding(
            get: { info.bundle },
            set: { value in
                if value == nil { store.bundleHandled() }
            }
        )
    }

    private var page: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                Text(StatusText.headline(info.headline))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                if let error = info.load.error {
                    NoticeBanner(text: error.localized)
                }
                if !info.failedReads.isEmpty {
                    failedReads
                }
                StatusTabBar(tab: $tab, info: info)
                switch tab {
                case .status: StatusPlates(info: info, store: store)
                case .jobs: StatusJobsView(info: info, store: store)
                case .updates: StatusUpdatesView(info: info, store: store)
                case .support: StatusSupportView()
                case .about: StatusAboutView(info: info, store: store) { tab = .updates }
                }
            }
            .padding(Theme.Spacing.screen)
        }
        .refreshable { await store.refresh() }
    }

    private var failedReads: some View {
        let what = info.failedReads.map { StatusText.read($0) }.joined(separator: ", ")
        return HStack {
            Text(L10n.format("settings.status.could.not.load", what))
                .font(.callout)
                .foregroundStyle(Theme.text)
            Spacer(minLength: Theme.Spacing.small)
            Button(L10n.tr("common.retry")) { Task { await store.refresh() } }
                .buttonStyle(.bordered)
                .controlSize(.small)
        }
    }
}

struct StatusTabBar: View {
    @Binding var tab: StatusPageTab
    let info: HealthInfo

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                ForEach(StatusPageTab.allCases) { item in
                    Button {
                        tab = item
                    } label: {
                        HStack(spacing: 6) {
                            Text(item.title)
                                .font(.subheadline.weight(.semibold))
                            if let tone = badge(item) {
                                Circle()
                                    .fill(Theme.dotColor(tone))
                                    .frame(width: 7, height: 7)
                            }
                        }
                        .padding(.horizontal, Theme.Spacing.medium)
                        .padding(.vertical, Theme.Spacing.small)
                        .foregroundStyle(tab == item ? Theme.text : Theme.textMuted)
                        .background(tab == item ? Theme.surfaceHigh : Theme.surfaceRaised, in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(tab == item ? .isSelected : [])
                }
            }
        }
    }

    private func badge(_ item: StatusPageTab) -> ToneKind? {
        if item == .jobs {
            if info.jobsBadge == .fail { return .fail }
            if info.jobsBadge == .wait { return .wait }
            return nil
        }
        if item == .updates && info.updateAvailable { return .wait }
        return nil
    }
}

struct StatusPlate<Content: View>: View {
    let title: String
    var subtitle: String?
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            HStack {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                    .accessibilityAddTraits(.isHeader)
                Spacer(minLength: Theme.Spacing.small)
                if let subtitle {
                    Text(subtitle)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            content()
        }
        .padding(Theme.Spacing.large)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous)
                .strokeBorder(Theme.line, lineWidth: 1)
        }
    }
}

struct StatusBang: View {
    let tone: ToneKind

    var body: some View {
        Text("!")
            .font(.caption.weight(.bold))
            .foregroundStyle(Theme.background)
            .frame(width: 20, height: 20)
            .background(Theme.dotColor(tone), in: Circle())
            .accessibilityHidden(true)
    }
}
