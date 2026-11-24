import SwiftUI

struct SettingsPartsView: View {
    @State private var store = SettingsControlsPartsStore()
    @State private var advanced = false
    @Environment(\.openRoute) private var openRoute

    private var info: SettingsControlsPartsInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                EmptyStateView(title: L10n.tr("settings.controls.owner.only"), systemImage: "lock")
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    list
                }
            }
        }
        .navigationTitle(L10n.tr("settings.page.parts"))
        .settingsControlsToast(store.toaster)
        .settingsControlsAsk(info.ask, busy: info.busy, confirm: { store.confirm() }, cancel: { store.cancel() })
        .reauthSheet(store.reauth)
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private var list: some View {
        List {
            if let error = info.load.error {
                Section { NoticeBanner(text: error.localized) }
            }
            if info.safeMode {
                Section {
                    VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                        Text(L10n.tr("settings.controls.safe.mode.text"))
                            .font(.callout)
                        Button(L10n.tr("settings.controls.open.danger")) { openRoute(.settingsDanger) }
                    }
                } header: {
                    Text(L10n.tr("settings.controls.safe.mode.title"))
                }
            }
            Section {
                ForEach(info.parts) { row in
                    SettingsControlsPartRowView(row: row, busy: info.busy, working: info.busyKey == row.key, store: store)
                }
            } header: {
                Text(L10n.tr("settings.controls.parts.title"))
            } footer: {
                Text(L10n.tr("settings.controls.parts.desc"))
            }
            Section {
                Button(advanced ? L10n.tr("settings.controls.advanced.hide") : L10n.tr("settings.controls.advanced")) {
                    advanced.toggle()
                }
            }
            if advanced {
                stagesSection
                linksSection
                eventsSection
            }
            Section {
                Button { openRoute(.settingsDanger) } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                            Text(L10n.tr("settings.controls.whole.app"))
                                .foregroundStyle(Theme.text)
                            Text(L10n.tr("settings.controls.whole.app.note"))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(Theme.textFaint)
                    }
                }
            }
        }
        .refreshable { await store.refresh() }
    }

    private var stagesSection: some View {
        Section {
            if info.stages.isEmpty {
                Text(L10n.tr("settings.controls.stages.empty")).foregroundStyle(Theme.textMuted)
            }
            ForEach(info.stages) { row in
                SettingsControlsStageRowView(row: row, busy: info.busy, working: info.busyKey == row.key, store: store)
            }
        } header: {
            Text(L10n.tr("settings.controls.stages.title"))
        } footer: {
            Text(L10n.tr("settings.controls.stages.desc"))
        }
    }

    private var linksSection: some View {
        Section {
            if info.links.isEmpty {
                Text(L10n.tr("settings.controls.links.empty")).foregroundStyle(Theme.textMuted)
            }
            ForEach(info.links) { row in
                SettingsControlsLinkRowView(row: row, busy: info.busy, working: info.busyKey == row.key, store: store)
            }
        } header: {
            Text(L10n.tr("settings.controls.links.title"))
        } footer: {
            Text(L10n.tr("settings.controls.links.desc"))
        }
    }

    private var eventsSection: some View {
        Section {
            if info.events.isEmpty {
                Text(L10n.tr("settings.controls.events.empty")).foregroundStyle(Theme.textMuted)
            }
            ForEach(info.events) { event in
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(event.summary)
                    if let at = event.at {
                        Text(Format.relative(at))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
        } header: {
            Text(L10n.tr("settings.controls.events.title"))
        }
    }
}

struct SettingsControlsPartRowView: View {
    let row: SettingsControlsPartRowInfo
    let busy: Bool
    let working: Bool
    let store: SettingsControlsPartsStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack {
                Text(SettingsControlsText.partName(row.part, row.name))
                    .font(.headline)
                if row.run != .running {
                    StatusChip(text: SettingsControlsText.run(row.run), tone: row.tone)
                }
            }
            if let stops = SettingsControlsText.partStops(row.part) {
                Text(L10n.format("settings.controls.when.off", stops))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if let needs = SettingsControlsText.partNeeds(row.part) {
                Text(needs)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if !row.reason.isEmpty {
                Text(row.reason)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            HStack {
                if row.on {
                    Button(L10n.tr("settings.controls.restart")) { store.restart(row.part) }
                        .buttonStyle(.bordered)
                        .disabled(busy)
                    Button(L10n.tr("settings.controls.turn.off")) { store.turnOff(row.part) }
                        .buttonStyle(.bordered)
                        .disabled(busy)
                } else {
                    Button(L10n.tr("settings.controls.turn.on")) { store.turnOn(row.part) }
                        .buttonStyle(.borderedProminent)
                        .disabled(busy)
                }
                if working { ProgressView() }
            }
            .controlSize(.small)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

struct SettingsControlsStageRowView: View {
    let row: SettingsControlsStageRowInfo
    let busy: Bool
    let working: Bool
    let store: SettingsControlsPartsStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack {
                Text(row.name)
                    .font(.headline)
                if row.run != .running {
                    StatusChip(text: SettingsControlsText.run(row.run), tone: row.tone)
                }
            }
            if !row.reason.isEmpty {
                Text(row.reason)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if row.pausable {
                HStack {
                    if row.paused {
                        Button(L10n.tr("settings.controls.resume")) { store.stage(row.stage, .resume) }
                            .buttonStyle(.bordered)
                            .disabled(busy)
                    } else {
                        Button(L10n.tr("settings.controls.pause")) { store.stage(row.stage, .pause) }
                            .buttonStyle(.bordered)
                            .disabled(busy)
                    }
                    Button(L10n.tr("settings.controls.restart")) { store.stage(row.stage, .restart) }
                        .buttonStyle(.bordered)
                        .disabled(busy)
                    if working { ProgressView() }
                }
                .controlSize(.small)
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

struct SettingsControlsLinkRowView: View {
    let row: SettingsControlsLinkRowInfo
    let busy: Bool
    let working: Bool
    let store: SettingsControlsPartsStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack {
                Text(row.name)
                    .font(.headline)
                if row.run != .running {
                    StatusChip(text: SettingsControlsText.run(row.run), tone: row.tone)
                }
            }
            Text(SettingsControlsText.linkType(row.kind))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if !row.reason.isEmpty {
                Text(row.reason)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if row.canTest {
                HStack {
                    Button(row.kind == .downloader ? L10n.tr("settings.controls.reconnect") : L10n.tr("settings.controls.test.now")) {
                        store.reconnect(row.key)
                    }
                    .buttonStyle(.bordered)
                    .disabled(busy)
                    if working { ProgressView() }
                }
                .controlSize(.small)
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}
