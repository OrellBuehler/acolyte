import SwiftUI

struct StatusUpdatesView: View {
    let info: HealthInfo
    let store: HealthStore

    var body: some View {
        if info.updateFailed && info.standing == .loading {
            StatusPlate(title: L10n.tr("settings.status.group.app")) {
                Text(L10n.tr("settings.status.update.failed"))
                    .font(.callout)
                    .foregroundStyle(Theme.failText)
                Button(L10n.tr("settings.status.try.again")) { Task { await store.refresh() } }
                    .buttonStyle(.bordered)
            }
        } else {
            StatusVersionBand(info: info, store: store)
            if !info.updateNotes.isEmpty {
                StatusPlate(title: L10n.tr("settings.status.new.since")) {
                    ForEach(Array(info.updateNotes.enumerated()), id: \.offset) { item in
                        HStack(alignment: .top, spacing: Theme.Spacing.small) {
                            Text("•")
                            Text(item.element)
                        }
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                    }
                }
            }
            if info.canAct, let install = info.install {
                StatusInAppCard(info: info, install: install, store: store)
            }
        }
    }
}

struct StatusVersionBand: View {
    let info: HealthInfo
    let store: HealthStore
    @Environment(\.openURL) private var openURL

    var body: some View {
        StatusPlate(title: L10n.tr("settings.status.your.version")) {
            HStack(alignment: .top, spacing: Theme.Spacing.large) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("settings.status.this.version"))
                        .font(.caption)
                        .foregroundStyle(Theme.textMuted)
                    Text(info.updateVersion.isEmpty ? "…" : info.updateVersion)
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(Theme.text)
                }
                if info.standing == .behind && !info.updateLatest.isEmpty {
                    Image(systemName: "arrow.right")
                        .foregroundStyle(Theme.textMuted)
                        .padding(.top, 18)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L10n.tr("settings.status.newest"))
                            .font(.caption)
                            .foregroundStyle(Theme.textMuted)
                        Text(info.updateLatest)
                            .font(.title2.weight(.semibold))
                            .foregroundStyle(Theme.waitText)
                    }
                } else if info.standing == .current {
                    StatusChip(text: L10n.tr("settings.status.up.to.date"), tone: .ok)
                        .padding(.top, 14)
                }
            }
            Text(standingText)
                .font(.callout)
                .foregroundStyle(Theme.text)
            if info.standing == .problem && !info.updateProblem.isEmpty {
                Text(info.updateProblem)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if let checked = info.updateCheckedAt {
                Text(L10n.format("settings.status.update.checked", Format.relative(checked)))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            HStack(spacing: Theme.Spacing.small) {
                if let link = URL(string: info.updateLink), !info.updateLink.isEmpty {
                    Button(L10n.tr("settings.status.what.changed")) { openURL(link) }
                        .buttonStyle(.bordered)
                }
                if info.canAct && info.updateOn {
                    Button(info.updateCheckedAt != nil ? L10n.tr("settings.status.check.again") : L10n.tr("settings.status.check.now")) {
                        store.checkUpdates()
                    }
                    .buttonStyle(.bordered)
                    .disabled(info.checkingUpdate)
                }
            }
        }
    }

    private var standingText: String {
        switch info.standing {
        case .loading: "…"
        case .off: L10n.tr("settings.status.standing.off")
        case .behind: L10n.tr("settings.status.standing.behind")
        case .problem: L10n.tr("settings.status.standing.problem")
        case .none: L10n.tr("settings.status.standing.none")
        case .current: L10n.tr("settings.status.standing.current")
        case .unchecked: L10n.tr("settings.status.standing.unchecked")
        }
    }
}

struct StatusInAppCard: View {
    let info: HealthInfo
    let install: StatusInstallInfo
    let store: HealthStore

    private var canChange: Bool { info.canManageUpdates && !info.installBusy }

    var body: some View {
        StatusPlate(title: L10n.tr("settings.status.inapp.title")) {
            Toggle(
                L10n.tr("settings.status.inapp.check.on"),
                isOn: Binding(get: { install.checkOn }, set: { store.setCheckOn($0) })
            )
            .disabled(!canChange)
            if !info.canManageUpdates {
                hint(L10n.tr("settings.status.inapp.users.only"))
            }
            if install.available {
                modeControl
                if install.modeLocked { hint(L10n.tr("settings.status.inapp.locked")) }
                if !install.checkOn { hint(L10n.tr("settings.status.inapp.check.off.hint")) }
                if install.checkOn || install.mode != .off { hint(modeHint) }
                hint(install.fromUpdate ? L10n.tr("settings.status.inapp.from.update") : L10n.tr("settings.status.inapp.from.image"))
                notes
                progress
                if info.installReady && info.canManageUpdates {
                    Text(L10n.format("settings.status.inapp.ready", install.ready))
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                    if install.oneWay {
                        Text(L10n.tr("settings.status.inapp.one.way"))
                            .font(.callout)
                            .foregroundStyle(Theme.waitText)
                    }
                    Button(L10n.tr("settings.status.inapp.update.now")) { store.installNow() }
                        .buttonStyle(.borderedProminent)
                        .disabled(info.installBusy)
                }
                if info.installCanGoBack && info.canManageUpdates {
                    Button(L10n.format("settings.status.inapp.go.back", install.previous)) { store.goBack() }
                        .buttonStyle(.plain)
                        .foregroundStyle(Theme.accentText)
                        .disabled(info.installBusy)
                }
            }
        }
    }

    private func hint(_ text: String) -> some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
    }

    private var modeHint: String {
        switch install.mode {
        case .auto: L10n.tr("settings.status.inapp.auto.hint")
        case .ask: L10n.tr("settings.status.inapp.ask.hint")
        case .off: L10n.tr("settings.status.inapp.off.hint")
        }
    }

    private var modeControl: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(L10n.tr("settings.status.inapp.mode"))
                .font(.caption)
                .foregroundStyle(Theme.textMuted)
            HStack(spacing: Theme.Spacing.small) {
                ForEach(StatusModeID.allCases, id: \.self) { mode in
                    let enabled = canChange && !install.modeLocked && (mode == .off || install.checkOn)
                    Button {
                        store.setMode(mode)
                    } label: {
                        Text(modeTitle(mode))
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, Theme.Spacing.small)
                            .foregroundStyle(install.mode == mode ? Theme.text : Theme.textMuted)
                            .background(install.mode == mode ? Theme.surfaceHigh : Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(!enabled)
                    .opacity(enabled ? 1 : 0.5)
                    .accessibilityAddTraits(install.mode == mode ? .isSelected : [])
                }
            }
        }
    }

    private func modeTitle(_ mode: StatusModeID) -> String {
        switch mode {
        case .off: L10n.tr("settings.status.inapp.off")
        case .ask: L10n.tr("settings.status.inapp.ask")
        case .auto: L10n.tr("settings.status.inapp.auto")
        }
    }

    @ViewBuilder
    private var notes: some View {
        if install.wentBack {
            hint(L10n.format("settings.status.inapp.went.back", install.wentBackTo, install.wentBackLeft))
        } else if !install.rollback.isEmpty {
            Text(L10n.format("settings.status.inapp.rolled.back", install.rollback))
                .font(.callout)
                .foregroundStyle(Theme.waitText)
        }
        if !install.execError.isEmpty {
            Text(L10n.format("settings.status.inapp.exec.error", install.execError))
                .font(.callout)
                .foregroundStyle(Theme.waitText)
        }
    }

    @ViewBuilder
    private var progress: some View {
        switch install.phase {
        case .downloading: progressLine(L10n.format("settings.status.inapp.downloading", install.progressVersion), failed: false)
        case .checking: progressLine(L10n.format("settings.status.inapp.checking", install.progressVersion), failed: false)
        case .waiting: progressLine(L10n.format("settings.status.inapp.waiting", install.progressMessage), failed: false)
        case .copying: progressLine(L10n.tr("settings.status.inapp.copying"), failed: false)
        case .restarting: progressLine(L10n.tr("settings.status.inapp.restarting"), failed: false)
        case .failed: progressLine(L10n.format("settings.status.inapp.failed", install.progressMessage), failed: true)
        case .idle, .ready: EmptyView()
        }
    }

    private func progressLine(_ text: String, failed: Bool) -> some View {
        Text(text)
            .font(.callout)
            .foregroundStyle(failed ? Theme.waitText : Theme.text)
    }
}
