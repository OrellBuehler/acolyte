import SwiftUI
import UniformTypeIdentifiers

struct SettingsBackupsView: View {
    @State private var store = SettingsServerBackupsStore()
    @State private var deleting: SettingsServerBackupItem?
    @State private var picking = false
    @State private var confirmRestore = false
    @State private var pickedPath = ""
    @State private var pickedName = ""
    @State private var pickFailed = false
    @State private var sharing = false

    private var info: SettingsServerBackupsInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(load: info.load, isEmpty: info.off, reload: { await store.refresh() }) {
                    EmptyStateView(
                        title: L10n.tr("settings.server.backups.off"),
                        message: L10n.tr("settings.server.backups.off.text"),
                        systemImage: "archivebox"
                    )
                } content: {
                    list
                }
            }
        }
        .navigationTitle(L10n.tr("settings.page.backups"))
        .fileImporter(isPresented: $picking, allowedContentTypes: [.zip]) { result in
            handlePick(result)
        }
        .alert(
            askTitle,
            isPresented: Binding(get: { ask != nil }, set: { _ in })
        ) {
            askButtons
        } message: {
            Text(askMessage)
        }
        .sheet(isPresented: $sharing, onDismiss: { store.release() }) {
            if let file = info.file {
                ActivityShareSheet(items: [URL(fileURLWithPath: file.path)])
            }
        }
        .reauthSheet(store.reauth)
        .safeAreaInset(edge: .bottom) {
            if let note = info.notice {
                NoteToast(text: noticeText(note), undo: nil)
            } else if pickFailed {
                NoteToast(text: L10n.tr("settings.server.backups.pick.failed"), undo: nil)
            }
        }
        .animation(.smooth(duration: 0.24), value: info.notice?.seq)
        .task(id: info.notice?.seq) {
            guard let note = info.notice else { return }
            try? await Task.sleep(for: .seconds(4))
            if store.info.notice?.seq == note.seq { store.dismissNotice(note.seq) }
        }
        .task(id: pickFailed) {
            guard pickFailed else { return }
            try? await Task.sleep(for: .seconds(4))
            pickFailed = false
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private enum Ask {
        case restore
        case remove
        case share
    }

    private var ask: Ask? {
        if confirmRestore { return .restore }
        if deleting != nil { return .remove }
        if info.file != nil, !sharing { return .share }
        return nil
    }

    private var askTitle: String {
        let kit = info.file?.kind == .keyKit
        switch ask {
        case .restore: return L10n.format("settings.server.backups.restore.title", pickedName)
        case .remove: return L10n.format("settings.server.backups.remove.title", deleting?.name ?? "")
        case .share: return L10n.tr(kit ? "settings.server.backups.share.kit.title" : "settings.server.backups.share.backup.title")
        case nil: return ""
        }
    }

    private var askMessage: String {
        let kit = info.file?.kind == .keyKit
        switch ask {
        case .restore: return L10n.tr("settings.server.backups.restore.text")
        case .remove: return L10n.tr("settings.server.backups.remove.text")
        case .share: return L10n.tr(kit ? "settings.server.backups.share.kit.text" : "settings.server.backups.share.backup.text")
        case nil: return ""
        }
    }

    @ViewBuilder
    private var askButtons: some View {
        switch ask {
        case .restore:
            Button(L10n.tr("settings.server.backups.restore"), role: .destructive) {
                store.restore(pickedPath, pickedName)
                pickedPath = ""
                pickedName = ""
                confirmRestore = false
            }
            Button(L10n.tr("common.cancel"), role: .cancel) {
                try? FileManager.default.removeItem(atPath: pickedPath)
                pickedPath = ""
                pickedName = ""
                confirmRestore = false
            }
        case .remove:
            Button(L10n.tr("settings.server.backups.remove"), role: .destructive) {
                if let name = deleting?.name { store.delete(name) }
                deleting = nil
            }
            Button(L10n.tr("common.cancel"), role: .cancel) { deleting = nil }
        case .share:
            Button(L10n.tr("settings.server.backups.share")) { sharing = true }
            Button(L10n.tr("common.cancel"), role: .cancel) { store.release() }
        case nil:
            EmptyView()
        }
    }

    private func handlePick(_ result: Result<URL, Error>) {
        guard case .success(let url) = result else { return }
        let name = url.lastPathComponent
        let path = store.stagingPath(name)
        Task {
            let copied = await Task.detached { settingsServerCopyPicked(url, path) }.value
            if copied {
                pickedPath = path
                pickedName = name
                confirmRestore = true
            } else {
                pickFailed = true
            }
        }
    }

    private func noticeText(_ note: SettingsServerBackupsNoticeInfo) -> String {
        switch note.kind {
        case .made:
            return L10n.tr("settings.server.backups.made")
        case .makeFailed:
            return SettingsServerWords.withError(L10n.tr("settings.server.backups.make.failed"), note.error)
        case .deleted:
            return L10n.tr("settings.server.backups.deleted")
        case .deleteFailed:
            return SettingsServerWords.withError(L10n.tr("settings.server.backups.delete.failed"), note.error)
        case .planSaved:
            let preset = SettingsServerBackupPresetInfo(rawValue: note.name) ?? .balanced
            return L10n.format("settings.server.backups.plan.saved", SettingsServerWords.presetName(preset))
        case .planFailed:
            return SettingsServerWords.withError(L10n.tr("settings.server.backups.plan.failed"), note.error)
        case .downloadFailed:
            return SettingsServerWords.withError(L10n.tr("settings.server.backups.download.failed"), note.error)
        case .kitReady:
            return L10n.tr("settings.server.backups.kit.ready")
        case .kitFailed:
            return SettingsServerWords.withError(L10n.tr("settings.server.backups.kit.failed"), note.error)
        case .restoreStaged:
            return L10n.tr("settings.server.backups.restore.staged")
        case .restoreFailed:
            return SettingsServerWords.withError(L10n.tr("settings.server.backups.restore.failed"), note.error)
        case .restarted:
            return L10n.tr("settings.server.backups.restarted")
        case .restartSlow:
            return L10n.tr("settings.server.backups.restart.slow")
        }
    }

    private var restoring: Bool { info.restore != .idle }

    private var list: some View {
        List {
            if let error = info.load.error {
                NoticeBanner(text: SettingsServerWords.errorLine(error))
            }
            if info.restoreWaiting {
                Section { SettingsServerInfoLine(text: L10n.tr("settings.server.backups.restore.ready")) }
            }
            if restoring {
                Section {
                    VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                        SettingsServerInfoLine(text: restoringText)
                        ProgressView()
                    }
                }
            }
            if info.canChange {
                Section {
                    Button {
                        store.makeNow()
                    } label: {
                        HStack {
                            if info.making {
                                ProgressView()
                            } else {
                                Image(systemName: "archivebox")
                            }
                            Text(L10n.tr("settings.server.backups.back.up.now"))
                        }
                    }
                    .disabled(info.making || restoring)
                }
            }
            savedSection
            oftenSection
            keySection
        }
        .refreshable { await store.refresh() }
    }

    private var restoringText: String {
        info.restore == .uploading
            ? L10n.format("settings.server.backups.uploading", info.restoreName)
            : L10n.tr("settings.server.backups.restarting")
    }

    private var savedSection: some View {
        Section {
            if info.isOwner {
                Button {
                    picking = true
                } label: {
                    Label(L10n.tr("settings.server.backups.restore.file"), systemImage: "square.and.arrow.up")
                }
                .disabled(restoring)
            }
            if info.backups.isEmpty {
                Text(L10n.tr("settings.server.backups.none"))
                    .foregroundStyle(Theme.textMuted)
            } else {
                ForEach(info.backups) { backup in
                    row(backup)
                }
            }
            if info.showKeyNote {
                Text(info.keyNote)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        } header: {
            Text(L10n.tr("settings.server.backups.saved"))
        }
    }

    private func row(_ backup: SettingsServerBackupItem) -> some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            VStack(alignment: .leading, spacing: 2) {
                Text(backup.name)
                    .font(.footnote.monospaced())
                Text(line(backup))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            Spacer(minLength: 0)
            if info.isOwner {
                if backup.busy {
                    ProgressView()
                } else {
                    HStack(spacing: Theme.Spacing.medium) {
                        Button(L10n.tr("settings.server.backups.download")) { store.download(backup.name) }
                            .buttonStyle(.borderless)
                        Button(role: .destructive) {
                            deleting = backup
                        } label: {
                            Image(systemName: "trash")
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(L10n.format("settings.server.backups.delete.name", backup.name))
                    }
                }
            }
        }
    }

    private func line(_ backup: SettingsServerBackupItem) -> String {
        let when = backup.createdAt.map { $0.formatted(date: .abbreviated, time: .shortened) } ?? ""
        let size = ByteCountFormatter.string(fromByteCount: backup.size, countStyle: .file)
        let detail = backup.detail.isEmpty ? SettingsServerWords.backupKind(backup.kind) : backup.detail
        return L10n.format("settings.server.backups.row.line", when, size, detail)
    }

    private var oftenSection: some View {
        Section {
            ForEach(SettingsServerBackupPresetInfo.allCases) { preset in
                Button {
                    store.setPreset(preset)
                } label: {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(SettingsServerWords.presetName(preset)).foregroundStyle(Theme.text)
                            Text(SettingsServerWords.presetAbout(preset))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                        Spacer()
                        if info.preset == preset {
                            Image(systemName: "checkmark")
                                .foregroundStyle(Theme.accentText)
                        }
                    }
                }
                .disabled(!info.canChange || info.presetSaving)
                .accessibilityAddTraits(info.preset == preset ? .isSelected : [])
            }
        } header: {
            Text(L10n.tr("settings.server.backups.often"))
        } footer: {
            Text(L10n.tr("settings.server.backups.often.desc"))
        }
    }

    @ViewBuilder
    private var keySection: some View {
        if let kit = info.keyKit, !kit.fingerprint.isEmpty {
            Section {
                if kit.needed {
                    SettingsServerInfoLine(text: kitNeededText(kit), systemImage: kit.warn ? "exclamationmark.triangle" : "info.circle")
                }
                LabeledContent(L10n.tr("settings.server.backups.fingerprint")) {
                    Text(kit.fingerprint).font(.footnote.monospaced())
                }
                if info.canDownloadKit {
                    Button {
                        store.downloadKeyKit()
                    } label: {
                        HStack {
                            if info.keyKitBusy {
                                ProgressView()
                            } else {
                                Image(systemName: "key")
                            }
                            Text(L10n.tr("settings.server.backups.download.kit"))
                        }
                    }
                    .disabled(info.keyKitBusy)
                    Text(L10n.tr("settings.server.backups.restore.key"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                } else if info.isOwner, info.keyKitNeedsSession {
                    Text(L10n.tr("settings.server.backups.kit.needs.session"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            } header: {
                Text(L10n.tr("settings.server.backups.key"))
            } footer: {
                Text(L10n.tr("settings.server.backups.key.desc"))
            }
        }
    }

    private func kitNeededText(_ kit: SettingsServerKeyKitItem) -> String {
        let base = L10n.tr(kit.warn ? "settings.server.backups.no.kit" : "settings.server.backups.kit.recommended")
        return info.isOwner ? base : base + " " + L10n.tr("settings.server.backups.owner.only.kit")
    }
}
