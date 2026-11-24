import SwiftUI

struct SettingsDangerView: View {
    @State private var store = SettingsControlsDangerStore()

    private var info: SettingsControlsDangerInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                EmptyStateView(title: L10n.tr("settings.controls.owner.only"), systemImage: "lock")
            } else if let stopped = info.stopped {
                SettingsControlsStoppedView(stopped: stopped) { store.dismissStopped() }
            } else {
                form
            }
        }
        .navigationTitle(L10n.tr("settings.page.danger"))
        .settingsControlsToast(store.toaster)
        .settingsControlsAsk(info.ask, busy: info.busy, confirm: { store.confirm() }, cancel: { store.cancel() })
        .reauthSheet(store.reauth)
        .task { await store.watch() }
        .task { await store.load() }
        .task(id: info.stopped != nil) {
            if info.stopped != nil { await store.waitForServer() }
        }
    }

    private var form: some View {
        Form {
            Section {
                Text(L10n.tr("settings.controls.danger.intro"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                actionButton(.appRestart, destructive: false)
                actionButton(.appSafeMode, destructive: false)
                actionButton(.appShutdown, destructive: true)
            } header: {
                Text(L10n.tr("settings.controls.whole.app"))
            } footer: {
                Text(L10n.tr("settings.controls.danger.safe.note"))
            }
        }
    }

    private func actionButton(_ kind: SettingsControlsActionKind, destructive: Bool) -> some View {
        Button(role: destructive ? .destructive : nil) {
            store.request(kind)
        } label: {
            HStack {
                Text(SettingsControlsText.label(kind, ""))
                Spacer()
                if info.busyKind == kind { ProgressView() }
            }
        }
        .disabled(info.busy)
    }
}

struct SettingsControlsStoppedView: View {
    let stopped: SettingsControlsStoppedInfo
    let dismiss: () -> Void

    var body: some View {
        VStack(spacing: Theme.Spacing.large) {
            Spacer()
            Text(stopped.text.isEmpty ? L10n.tr("settings.controls.done.stopping") : stopped.text)
                .font(.headline)
                .multilineTextAlignment(.center)
            if stopped.wait != .back {
                ProgressView()
                    .controlSize(.large)
            }
            Text(waitText)
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            if stopped.kind == .appShutdown && stopped.wait != .back {
                Text(L10n.tr("settings.controls.wait.shutdown"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            }
            if stopped.wait == .back {
                Button(L10n.tr("settings.controls.done"), action: dismiss)
                    .buttonStyle(.borderedProminent)
            } else {
                Button(L10n.tr("settings.controls.stop.waiting"), action: dismiss)
            }
            Spacer()
        }
        .padding(Theme.Spacing.xLarge)
        .frame(maxWidth: .infinity)
    }

    private var waitText: String {
        switch stopped.wait {
        case .stopping: L10n.tr("settings.controls.wait.stopping")
        case .waiting: L10n.tr("settings.controls.wait.waiting")
        case .back: L10n.tr("settings.controls.wait.back")
        }
    }
}
