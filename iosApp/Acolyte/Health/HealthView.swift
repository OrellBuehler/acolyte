import SwiftUI

struct HealthView: View {
    @State private var store = HealthStore()

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(
                title: L10n.tr("health.empty.title"),
                message: L10n.tr("health.empty.message"),
                systemImage: "shield"
            )
        } content: {
            List {
                if let error = store.info.load.error {
                    NoticeBanner(text: error.localized)
                        .listRowSeparator(.hidden)
                }
                if let error = store.info.actionError {
                    NoticeBanner(text: error.localized)
                        .listRowSeparator(.hidden)
                }
                StatusChip(text: overallText(store.info.overall), tone: store.info.overall)
                    .listRowSeparator(.hidden)
                ForEach(store.info.checks) { check in
                    CheckRow(check: check)
                }
            }
            .listStyle(.plain)
            .refreshable { await store.refresh() }
        }
        .navigationTitle(SectionID.health.title)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                if store.info.running {
                    ProgressView()
                } else {
                    Button(L10n.tr("health.run"), systemImage: "arrow.clockwise") { store.runChecks() }
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private func overallText(_ tone: ToneKind) -> String {
        switch tone {
        case .fail: L10n.tr("health.overall.error")
        case .wait: L10n.tr("health.overall.warning")
        default: L10n.tr("health.overall.ok")
        }
    }
}

private struct CheckRow: View {
    let check: HealthCheckInfo

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(check.subject)
                    .font(.headline)
                if !check.message.isEmpty {
                    Text(check.message)
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                }
                if let since = check.since {
                    Text(Format.relative(since))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            Spacer(minLength: 0)
            StatusChip(text: stateText(check.tone), tone: check.tone)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private func stateText(_ tone: ToneKind) -> String {
        switch tone {
        case .ok: L10n.tr("health.state.ok")
        case .wait: L10n.tr("health.state.warning")
        case .fail: L10n.tr("health.state.error")
        default: L10n.tr("health.state.unknown")
        }
    }
}
