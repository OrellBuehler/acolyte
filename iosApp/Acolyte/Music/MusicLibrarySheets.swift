import SwiftUI

enum MusicLibrarySheet: Identifiable {
    case kinds
    case unmatched
    case rename(RenameStore)
    case remove(RemoveStore)

    var id: String {
        switch self {
        case .kinds: "kinds"
        case .unmatched: "unmatched"
        case .rename: "rename"
        case .remove: "remove"
        }
    }
}

struct MusicLibrarySheetView: View {
    let sheet: MusicLibrarySheet
    let store: MusicLibraryArtistStore
    let onRemoved: () -> Void

    var body: some View {
        switch sheet {
        case .kinds:
            MusicLibraryKindsSheet(info: store.info, store: store)
        case .unmatched:
            MusicUnmatchedSheet(titleId: store.titleId)
        case let .rename(rename):
            MusicLibraryRenameSheet(store: rename)
        case let .remove(remove):
            MusicLibraryRemoveSheet(store: remove, onDone: onRemoved)
        }
    }
}

struct MusicLibraryRenameSheet: View {
    let store: RenameStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List { RenameSection(store: store) }
                .navigationTitle(L10n.tr("title.rename.files"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(L10n.tr("title.close")) { dismiss() }
                    }
                }
        }
    }
}

struct MusicLibraryRemoveSheet: View {
    let store: RemoveStore
    let onDone: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            RemoveView(store: store)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(L10n.tr("title.close")) { dismiss() }
                    }
                }
        }
        .onChange(of: store.info?.done) { _, done in
            if done == true { onDone() }
        }
    }
}

struct MusicLibraryKindsSheet: View {
    let store: MusicLibraryArtistStore
    let options: MusicOptionLists
    @State private var newAlbums: String
    @State private var types: Set<String>
    @State private var secondary: Set<String>
    @State private var statuses: Set<String>
    @State private var more: Bool
    @State private var saving = false
    @Environment(\.dismiss) private var dismiss

    init(info: MusicLibraryArtistInfo, store: MusicLibraryArtistStore) {
        self.store = store
        options = SDKClient.shared.musicOptions()
        _newAlbums = State(initialValue: info.newAlbums)
        _types = State(initialValue: Set(info.albumTypes))
        _secondary = State(initialValue: Set(info.secondaryTypes))
        _statuses = State(initialValue: Set(info.statuses))
        let plain = info.secondaryTypes == ["studio"] && info.statuses == ["official"]
        _more = State(initialValue: !plain)
    }

    private var canSave: Bool {
        !saving && !types.isEmpty && !secondary.isEmpty && !statuses.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                newAlbumsSection
                Section(L10n.tr("music.lib.kinds.types")) {
                    checks(options.albumTypes, selection: $types)
                }
                if more { extraSections }
                Section {
                    Button(more ? L10n.tr("music.lib.kinds.fewer") : L10n.tr("music.lib.kinds.more")) { more.toggle() }
                }
            }
            .navigationTitle(L10n.tr("music.lib.kinds.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbar }
        }
    }

    private var newAlbumsSection: some View {
        Section(L10n.tr("music.lib.kinds.new.albums")) {
            Picker(L10n.tr("music.lib.kinds.new.albums"), selection: $newAlbums) {
                ForEach(options.newAlbums) { option in
                    Text(MusicLibraryArtistText.option(option)).tag(option.id)
                }
            }
            .pickerStyle(.inline)
            .labelsHidden()
        }
    }

    @ViewBuilder
    private var extraSections: some View {
        Section(L10n.tr("music.lib.kinds.extra")) {
            checks(options.secondaryTypes, selection: $secondary)
        }
        Section(L10n.tr("music.lib.kinds.statuses")) {
            checks(options.statuses, selection: $statuses)
        }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button(L10n.tr("common.cancel")) { dismiss() }
        }
        ToolbarItem(placement: .confirmationAction) {
            Button(L10n.tr("music.lib.kinds.save")) { save() }
                .disabled(!canSave)
        }
    }

    private func checks(_ list: [MusicOptionInfo], selection: Binding<Set<String>>) -> some View {
        ForEach(list) { option in
            Button { flip(option.id, selection) } label: {
                HStack {
                    Text(MusicLibraryArtistText.option(option))
                        .foregroundStyle(Theme.text)
                    Spacer(minLength: Theme.Spacing.small)
                    if selection.wrappedValue.contains(option.id) {
                        Image(systemName: "checkmark")
                            .foregroundStyle(Theme.accent)
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(selection.wrappedValue.contains(option.id) ? .isSelected : [])
        }
    }

    private func flip(_ id: String, _ selection: Binding<Set<String>>) {
        var set = selection.wrappedValue
        if set.contains(id) {
            set.remove(id)
        } else {
            set.insert(id)
        }
        selection.wrappedValue = set
    }

    private func ordered(_ list: [MusicOptionInfo], _ chosen: Set<String>) -> [String] {
        list.map(\.id).filter { chosen.contains($0) }
    }

    private func save() {
        saving = true
        let store = store
        let new = newAlbums
        let chosenTypes = ordered(options.albumTypes, types)
        let chosenSecondary = ordered(options.secondaryTypes, secondary)
        let chosenStatuses = ordered(options.statuses, statuses)
        Task {
            let saved = await store.saveKinds(newAlbums: new, types: chosenTypes, secondary: chosenSecondary, statuses: chosenStatuses)
            saving = false
            if saved { dismiss() }
        }
    }
}
