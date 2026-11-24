import SwiftUI

struct SettingsSchedulesView: View {
    @State private var store = SettingsControlsSchedulesStore()
    @Environment(\.openRoute) private var openRoute

    private var info: SettingsControlsSchedulesInfo { store.info }

    var body: some View {
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            list
        }
        .navigationTitle(L10n.tr("settings.page.schedules"))
        .settingsControlsToast(store.toaster)
        .sheet(isPresented: Binding(get: { info.editor != nil }, set: { if !$0 { store.closeEditor() } })) {
            if let editor = info.editor {
                SettingsControlsEditorSheet(editor: editor, store: store) { route in
                    store.closeEditor()
                    openRoute(route)
                }
            }
        }
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
                Section { NoticeBanner(text: L10n.tr("settings.controls.sched.safe.mode")) }
            }
            if !info.running.isEmpty {
                runningSection
            }
            ForEach(info.groups) { group in
                Section {
                    ForEach(group.rows) { row in
                        SettingsControlsScheduleRowView(row: row, busy: !info.busyKind.isEmpty, store: store) { route in
                            openRoute(route)
                        }
                    }
                } header: {
                    Text(SettingsControlsText.group(group.group))
                }
            }
            if !info.loops.isEmpty {
                loopsSection
            }
        }
        .refreshable { await store.refresh() }
    }

    private var runningSection: some View {
        Section {
            ForEach(info.running) { row in
                HStack {
                    Circle()
                        .fill(Theme.dotColor(.wait))
                        .frame(width: 10, height: 10)
                    Text(row.label)
                    Spacer()
                    Text(row.running ? L10n.tr("settings.controls.next.running") : L10n.tr("settings.controls.next.queued"))
                        .font(.footnote)
                        .foregroundStyle(Theme.waitText)
                }
            }
        } header: {
            Text(L10n.tr("settings.controls.sched.running.now"))
        }
    }

    private var loopsSection: some View {
        Section {
            Button(L10n.tr("settings.controls.sched.open.parts")) { openRoute(.settingsParts) }
            ForEach(info.loops) { loop in
                SettingsControlsLoopRowView(loop: loop)
            }
        } header: {
            Text(L10n.tr("settings.controls.sched.stages"))
        } footer: {
            Text(L10n.tr("settings.controls.sched.stages.note"))
        }
    }
}

enum SettingsControlsPages {
    static func route(_ key: String) -> Route? {
        switch key {
        case "sources": .settingsSources
        case "requests": .settingsRequests
        case "search": .settingsSearch
        case "subtitles": .settingsSubtitles
        case "this_app": .settingsThisApp
        default: nil
        }
    }

    static func color(_ dot: SettingsControlsDotKind) -> Color {
        switch dot {
        case .ok: Theme.dotColor(.ok)
        case .busy: Theme.dotColor(.wait)
        case .fail: Theme.dotColor(.fail)
        case .off: Theme.dotColor(.idle)
        }
    }
}

struct SettingsControlsScheduleRowView: View {
    let row: SettingsControlsScheduleRowInfo
    let busy: Bool
    let store: SettingsControlsSchedulesStore
    let open: (Route) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack(alignment: .top, spacing: Theme.Spacing.medium) {
                Circle()
                    .fill(SettingsControlsPages.color(row.dot))
                    .frame(width: 10, height: 10)
                    .padding(.top, 6)
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(row.label)
                        .font(.headline)
                    if !row.about.isEmpty {
                        Text(row.about)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                meta(L10n.tr("settings.controls.sched.how.often"), SettingsControlsText.every(row.every))
                meta(L10n.tr("settings.controls.sched.last.run"), lastText)
                meta(L10n.tr("settings.controls.sched.next.run"), SettingsControlsText.next(row.next))
                if row.failed {
                    Text(failedText)
                        .font(.footnote)
                        .foregroundStyle(Theme.failText)
                }
                if !row.failed && row.reason != .clear {
                    reasonLine
                }
                if let stale = SettingsControlsText.stale(row.stale) {
                    Text(stale)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            .padding(.leading, 22)
            HStack {
                if row.canRun {
                    Button {
                        store.runNow(row.kind)
                    } label: {
                        HStack {
                            if row.spinning { ProgressView() }
                            Text(L10n.tr("settings.controls.sched.run.now"))
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(row.spinning || busy)
                }
                if row.canChange {
                    Button(L10n.tr("settings.controls.sched.change")) { store.edit(row.kind) }
                        .buttonStyle(.bordered)
                }
            }
            .controlSize(.small)
            .padding(.leading, 22)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var failedText: String {
        if row.failedError.isEmpty { return L10n.tr("settings.controls.sched.failed") }
        return L10n.format("settings.controls.sched.failed.with", row.failedError)
    }

    private var lastText: String {
        guard let at = row.lastAt else { return L10n.tr("settings.controls.sched.not.yet") }
        let ago = Format.relative(at)
        return row.took.isEmpty ? ago : ago + " · " + L10n.format("settings.controls.sched.took", row.took)
    }

    private func meta(_ title: String, _ value: String) -> some View {
        Text(title + ": " + value)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
    }

    @ViewBuilder
    private var reasonLine: some View {
        if row.reasonLinks, let route = SettingsControlsPages.route(row.pageKey) {
            Button { open(route) } label: {
                Text(SettingsControlsText.reason(row.reason))
                    .font(.footnote)
                    .foregroundStyle(Theme.accentText)
            }
            .buttonStyle(.borderless)
        } else {
            Text(SettingsControlsText.reason(row.reason))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }
}

struct SettingsControlsLoopRowView: View {
    let loop: SettingsControlsLoopRowInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack {
                Circle()
                    .fill(SettingsControlsPages.color(loop.dot))
                    .frame(width: 10, height: 10)
                Text(loop.name)
                    .font(.headline)
                if loop.run != .running {
                    StatusChip(text: SettingsControlsText.run(loop.run), tone: loop.tone)
                }
            }
            if !loop.reason.isEmpty {
                line(loop.reason)
            }
            line(L10n.tr("settings.controls.sched.how.often") + ": " + SettingsControlsText.every(loop.every))
            if let at = loop.lastAt {
                line(L10n.tr("settings.controls.sched.last.run") + ": " + Format.relative(at))
            }
            if loop.next.kind != .unplanned {
                line(L10n.tr("settings.controls.sched.next.run") + ": " + SettingsControlsText.next(loop.next))
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private func line(_ text: String) -> some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
    }
}

struct SettingsControlsEditorSheet: View {
    let editor: SettingsControlsEditorInfo
    let store: SettingsControlsSchedulesStore
    let open: (Route) -> Void
    @State private var amount = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    if !editor.about.isEmpty {
                        Text(editor.about)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                    HStack {
                        Button { store.stepAmount(up: false) } label: {
                            Image(systemName: "minus.circle")
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(L10n.tr("settings.controls.sched.less"))
                        TextField(L10n.format("settings.controls.sched.every.field", SettingsControlsText.unit(editor.unit)), text: $amount)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(editor.valid ? Theme.text : Theme.failText)
                        Button { store.stepAmount(up: true) } label: {
                            Image(systemName: "plus.circle")
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(L10n.tr("settings.controls.sched.more"))
                    }
                } header: {
                    Text(L10n.format("settings.controls.sched.every.field", SettingsControlsText.unit(editor.unit)))
                } footer: {
                    Text(L10n.format("settings.controls.sched.range", editor.minAmount, editor.maxAmount, SettingsControlsText.unit(editor.unit)))
                }
                if editor.toggleable {
                    autoSection
                }
                if let error = editor.error {
                    Section {
                        Text(SettingsControlsText.errorText(error))
                            .foregroundStyle(Theme.failText)
                    }
                }
                if let route = SettingsControlsPages.route(editor.pageKey) {
                    Section {
                        Button(L10n.tr("settings.controls.sched.settings")) { open(route) }
                    }
                }
            }
            .navigationTitle(editor.label)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { store.closeEditor() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if editor.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("settings.controls.sched.save")) { store.saveEditor() }
                            .disabled(!editor.valid)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onAppear { amount = editor.amount }
        .onChange(of: editor.amount) { _, new in if new != amount { amount = new } }
        .onChange(of: amount) { _, new in if new != editor.amount { store.setAmount(new) } }
    }

    private var refresh: Bool { editor.kind == "refresh_titles" }

    private var autoSection: some View {
        Section {
            Text(autoState)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if editor.enabled {
                Button(autoLabel, role: .destructive) { store.setEnabled(editor.kind, false) }
                    .disabled(editor.busy)
            } else {
                Button(autoLabel) { store.setEnabled(editor.kind, true) }
                    .disabled(editor.busy)
            }
        } header: {
            Text(refresh ? L10n.tr("settings.controls.sched.auto.refresh") : L10n.tr("settings.controls.sched.auto.rescan"))
        }
    }

    private var autoState: String {
        switch (refresh, editor.enabled) {
        case (true, true): L10n.tr("settings.controls.sched.refresh.on")
        case (true, false): L10n.tr("settings.controls.sched.refresh.off")
        case (false, true): L10n.tr("settings.controls.sched.rescan.on")
        case (false, false): L10n.tr("settings.controls.sched.rescan.off")
        }
    }

    private var autoLabel: String {
        switch (refresh, editor.enabled) {
        case (true, true): L10n.tr("settings.controls.sched.refresh.turn.off")
        case (true, false): L10n.tr("settings.controls.sched.refresh.turn.on")
        case (false, true): L10n.tr("settings.controls.sched.rescan.turn.off")
        case (false, false): L10n.tr("settings.controls.sched.rescan.turn.on")
        }
    }
}
