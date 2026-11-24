import SwiftUI

struct MusicAddView: View {
    let mbid: String
    let name: String
    @State private var store: MusicAddStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openRoute) private var openRoute

    init(mbid: String, name: String) {
        self.mbid = mbid
        self.name = name
        _store = State(initialValue: MusicAddStore(mbid: mbid, name: name))
    }

    private var info: MusicAddInfo { store.info }

    private var title: String {
        L10n.format("music.add.title", info.name.isEmpty ? name : info.name)
    }

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar { toolbar }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    @ViewBuilder
    private var content: some View {
        if let outcome = info.outcome {
            done(outcome)
        } else {
            switch info.load.phase {
            case .loading: LoadingStateView()
            case .failed: ErrorStateView(error: info.load.error ?? .unknown) { Task { await store.load() } }
            case .ready: MusicAddForm(info: info, store: store) { openRoute(.settingsLibraryFolders) }
            }
        }
    }

    @ViewBuilder
    private func done(_ outcome: MusicAddOutcomeKind) -> some View {
        switch outcome {
        case .added:
            AskDoneView(title: L10n.format("music.add.added", info.name), message: L10n.tr("music.add.filling")) { dismiss() }
        case .already:
            AskDoneView(title: L10n.format("music.note.already.in.library", info.name), message: "") { dismiss() }
        }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button(L10n.tr("common.cancel")) { dismiss() }
        }
        if info.outcome == nil, info.load.phase == .ready {
            ToolbarItem(placement: .confirmationAction) {
                if info.busy {
                    ProgressView()
                } else {
                    Button(L10n.tr("music.add.submit")) { Task { await store.submit() } }
                        .disabled(!info.canSubmit)
                }
            }
        }
    }
}

struct MusicAddForm: View {
    let info: MusicAddInfo
    let store: MusicAddStore
    let openFolders: () -> Void
    private let options = MusicOptionCatalog.lists

    var body: some View {
        Form {
            headerSection
            folderSection
            if !info.rules.isEmpty { rulesSection }
            choiceSections
            checklists
            keepSection
            followSection
            errorSection
        }
    }

    @ViewBuilder
    private var headerSection: some View {
        if !info.disambiguation.isEmpty || info.titleId > 0 || info.neverAdd {
            Section {
                if !info.disambiguation.isEmpty {
                    Text(info.disambiguation).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                if info.titleId > 0 {
                    notice(L10n.format("music.note.already.in.library", info.name))
                }
                if info.neverAdd {
                    notice(L10n.tr("music.add.never"))
                }
            }
        }
    }

    private func notice(_ text: String) -> some View {
        NoticeBanner(text: text)
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
    }

    @ViewBuilder
    private var folderSection: some View {
        if info.noFolder {
            Section {
                notice(L10n.tr("music.add.no.folder"))
                Button(L10n.tr("music.add.open.folders"), action: openFolders)
            }
        } else if info.folders.isEmpty {
            Section {
                Text(L10n.tr("music.add.hands.off")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        } else {
            Section {
                Picker(L10n.tr("music.add.folder"), selection: Binding(get: { info.folderId }, set: { store.pickFolder($0) })) {
                    ForEach(info.folders) { folder in
                        Text(folderLabel(folder)).tag(folder.id)
                    }
                }
            }
        }
    }

    private func folderLabel(_ folder: MusicFolderInfo) -> String {
        "\(folder.label.isEmpty ? folder.path : folder.label) (\(folder.path))"
    }

    private var rulesSection: some View {
        Section {
            Picker(L10n.tr("music.add.rules"), selection: Binding(get: { info.rulesId }, set: { store.pickRules($0) })) {
                ForEach(info.rules) { rule in
                    Text(rule.name).tag(rule.id)
                }
            }
        } footer: {
            Text(L10n.tr("music.add.rules.hint"))
        }
    }

    @ViewBuilder
    private var choiceSections: some View {
        MusicOptionPickerSection(
            title: L10n.tr("music.add.choice"),
            group: .choice,
            options: options.albumChoices,
            selected: info.albumChoice
        ) { store.setAlbumChoice($0) }
        MusicOptionPickerSection(
            title: L10n.tr("music.add.new"),
            group: .newAlbums,
            options: options.newAlbums,
            selected: info.newAlbums,
            hint: L10n.tr("music.add.new.hint")
        ) { store.setNewAlbums($0) }
    }

    @ViewBuilder
    private var checklists: some View {
        MusicOptionChecklistSection(
            title: L10n.tr("music.add.kinds"),
            group: .albumType,
            options: options.albumTypes,
            selected: info.albumTypes,
            hint: L10n.tr("music.add.kinds.hint")
        ) { store.toggleAlbumType($0, on: $1) }
        MusicOptionChecklistSection(
            title: L10n.tr("music.add.extra"),
            group: .extra,
            options: options.secondaryTypes,
            selected: info.secondaryTypes,
            hint: L10n.tr("music.add.extra.hint")
        ) { store.toggleSecondary($0, on: $1) }
        MusicOptionChecklistSection(
            title: L10n.tr("music.add.statuses"),
            group: .status,
            options: options.statuses,
            selected: info.statuses
        ) { store.toggleStatus($0, on: $1) }
    }

    private var keepSection: some View {
        Section {
            Toggle(isOn: Binding(get: { info.keep }, set: { store.setKeep($0) })) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("music.add.keep"))
                    Text(L10n.tr("music.add.keep.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            .tint(Theme.accent)
        } footer: {
            if info.keep { Text(L10n.tr("music.add.keep.note")) }
        }
    }

    private var followSection: some View {
        Section {
            Toggle(isOn: Binding(get: { info.follow }, set: { store.setFollow($0) })) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("music.add.follow"))
                    Text(L10n.tr("music.add.follow.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            .tint(Theme.accent)
        }
    }

    @ViewBuilder
    private var errorSection: some View {
        if let error = info.error {
            Section {
                Text(error.localized).foregroundStyle(Theme.failText)
            }
        }
    }
}
