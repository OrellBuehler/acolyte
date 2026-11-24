import SwiftUI

struct HealthChecksView: View {
    @State private var store = HealthChecksStore()
    @State private var advanced = false
    @State private var toast: String?
    @State private var toastToken = 0

    private var info: HealthChecksInfo { store.info }

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
                    form
                }
            }
        }
        .navigationTitle(L10n.tr("settings.page.health"))
        .toolbar {
            if info.dirty {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("settings.status.hc.discard")) { store.discard() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("settings.status.hc.save")) { store.save() }
                            .disabled(!info.canSave)
                    }
                }
            } else if info.allowed && info.canSeeChecks {
                ToolbarItem(placement: .primaryAction) {
                    if info.running {
                        ProgressView()
                    } else {
                        Button(L10n.tr("settings.status.check.again"), systemImage: "arrow.clockwise") { store.runChecks() }
                    }
                }
            }
        }
        .navigationBarBackButtonHidden(info.dirty)
        .safeAreaInset(edge: .bottom) {
            if let toast {
                NoteToast(text: toast, undo: nil)
            }
        }
        .onChange(of: info.noticeId) { _, id in
            guard id > 0 else { return }
            if info.noticeSaved {
                toast = L10n.tr("settings.status.hc.saved")
            } else if info.noticeRunFailed {
                toast = L10n.tr("settings.status.hc.run.failed")
            } else {
                return
            }
            toastToken = id
            store.dismissNotice()
            Task {
                try? await Task.sleep(for: .seconds(4))
                if toastToken == id { toast = nil }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private var form: some View {
        Form {
            if info.canSeeChecks {
                resultsSection
            }
            if info.canEdit && info.hasSettings {
                settingsSection
            }
        }
    }

    private var resultsSection: some View {
        Section {
            HStack {
                Text(L10n.tr("settings.status.hc.worst"))
                Spacer()
                StatusChip(text: worstText, tone: info.worst)
            }
            ForEach(info.sync) { sync in
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    HStack {
                        Text(L10n.format("settings.status.hc.tracking", sync.name))
                            .font(.subheadline.weight(.semibold))
                        Spacer(minLength: Theme.Spacing.small)
                        StatusChip(
                            text: sync.degraded ? L10n.tr("settings.status.hc.sync.degraded") : L10n.tr("settings.status.hc.checking"),
                            tone: sync.degraded ? .fail : .wait
                        )
                    }
                    Text(sync.degraded ? L10n.tr("settings.status.hc.reader.down") : L10n.tr("settings.status.hc.read.stale"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    if let seen = sync.seenAt {
                        Text(L10n.format("settings.status.hc.last.read", Format.relative(seen)))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
            if info.checks.isEmpty {
                Text(L10n.tr("settings.status.hc.none"))
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.checks) { row in
                HealthChecksRowView(row: row)
            }
        } header: {
            Text(L10n.tr("settings.status.hc.results"))
        } footer: {
            Text(L10n.tr("settings.status.hc.results.sub"))
        }
    }

    private var worstText: String {
        switch info.worst {
        case .ok: L10n.tr("settings.status.hc.fine")
        case .wait: L10n.tr("settings.status.word.warning")
        case .fail: L10n.tr("settings.status.word.broken")
        case .info, .idle: L10n.tr("settings.status.hc.unknown")
        }
    }

    private var settingsSection: some View {
        Group {
            if let error = info.saveError {
                Section { Text(StatusText.errorDetail(error)).foregroundStyle(Theme.failText) }
            }
            if !info.valid {
                Section { Text(L10n.tr("settings.status.hc.invalid")).foregroundStyle(Theme.failText) }
            }
            Section {
                Text(L10n.format("settings.status.hc.privacy", info.version.isEmpty ? "version" : info.version))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Picker(
                    L10n.tr("settings.status.hc.every.days"),
                    selection: Binding(get: { info.updateDays }, set: { store.setDays($0) })
                ) {
                    ForEach(info.days, id: \.self) { days in
                        Text(dayLabel(days)).tag(days)
                    }
                }
                LimitField(title: L10n.tr("settings.status.hc.low.space"), text: info.lowText) { store.setNumber(.lowSpace, $0) }
                Text(L10n.tr("settings.status.hc.low.space.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                DisclosureGroup(L10n.tr("settings.status.hc.advanced"), isExpanded: $advanced) {
                    LimitField(title: L10n.tr("settings.status.hc.minutes"), text: info.everyText) { store.setNumber(.everyMinutes, $0) }
                    LimitField(title: L10n.tr("settings.status.hc.guard"), text: info.guardText) { store.setNumber(.flapGuard, $0) }
                    Text(L10n.tr("settings.status.hc.guard.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    TextField(
                        L10n.tr("settings.status.hc.feed"),
                        text: Binding(get: { info.feed }, set: { store.setFeed($0) }),
                        prompt: Text(info.defaultFeed)
                    )
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    Text(L10n.tr("settings.status.hc.feed.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    Toggle(L10n.tr("settings.status.hc.feed.local"), isOn: Binding(get: { info.local }, set: { store.setLocal($0) }))
                    Toggle(L10n.tr("settings.status.hc.prerelease"), isOn: Binding(get: { info.prerelease }, set: { store.setPrerelease($0) }))
                }
            } header: {
                Text(L10n.tr("settings.status.hc.settings"))
            } footer: {
                Text(L10n.tr("settings.status.hc.only.checks"))
            }
        }
    }

    private func dayLabel(_ days: Int) -> String {
        days == 1 ? L10n.tr("settings.status.hc.day") : L10n.format("settings.status.hc.days", days)
    }
}

struct HealthChecksRowView: View {
    let row: HealthChecksRowInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(alignment: .top, spacing: Theme.Spacing.small) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                    if !row.subject.isEmpty {
                        Text(row.subject)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                    if !row.message.isEmpty {
                        Text(row.message)
                            .font(.callout)
                            .foregroundStyle(Theme.text)
                    }
                }
                Spacer(minLength: Theme.Spacing.small)
                if row.tone != .ok {
                    VStack(alignment: .trailing, spacing: 2) {
                        StatusChip(
                            text: row.tone == .wait ? L10n.tr("settings.status.word.warning") : L10n.tr("settings.status.word.broken"),
                            tone: row.tone
                        )
                        if let since = row.since {
                            Text(Format.relative(since))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                    }
                }
            }
        }
    }

    private var label: String {
        if !row.label.isEmpty { return row.label }
        switch row.kind {
        case .libraryFolder: return L10n.tr("settings.status.hc.kind.library.folder")
        case .diskSpace: return L10n.tr("settings.status.title.free.space")
        case .source: return L10n.tr("settings.status.hc.kind.source")
        case .downloader: return L10n.tr("settings.status.hc.kind.downloader")
        case .backups: return L10n.tr("settings.status.title.backups")
        case .masterKey: return L10n.tr("settings.status.hc.kind.master.key")
        case .update: return L10n.tr("settings.status.title.update")
        case .configDisk: return L10n.tr("settings.status.title.config.disk")
        case .configFolder: return L10n.tr("settings.status.hc.kind.config.folder")
        case .waitingDownloads: return L10n.tr("settings.status.title.waiting.downloads")
        case .clearWaiting: return L10n.tr("settings.status.title.clear.waiting")
        case .saveFolder: return L10n.tr("settings.status.hc.kind.save.folder")
        case .sameDownloader: return L10n.tr("settings.status.hc.kind.same.downloader")
        case .torrentDownloader: return L10n.tr("settings.status.title.torrent.downloader")
        case .usenetDownloader: return L10n.tr("settings.status.title.usenet.downloader")
        case .trailers: return L10n.tr("settings.status.hc.kind.trailers")
        case .peopleCheck: return L10n.tr("settings.status.title.people.check")
        case .other: return L10n.tr("settings.status.hc.kind.other")
        }
    }
}
