import SwiftUI
import Observation

@MainActor
@Observable
final class LibraryFoldersStore {
    private(set) var info: LibraryFoldersInfo = .initial

    private let driver = SDKClient.shared.makeLibraryFolders()

    var handsOff: Bool {
        get { info.handsOff }
        set { setHandsOff(newValue) }
    }

    var passwordOpen: Bool {
        get { info.needsPassword }
        set {
            if !newValue { driver.cancelPassword() }
        }
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func add(
        kind: LibraryFolderKindID,
        path: String,
        label: String,
        rulesId: Int64,
        tags: [String],
        onlyTagged: Bool,
        lookOnly: Bool,
        flat: Bool
    ) async -> Bool {
        await driver.add(kind: kind, path: path, label: label, rulesId: rulesId, tags: tags, onlyTagged: onlyTagged, lookOnly: lookOnly, flat: flat)
    }

    func check(_ id: Int64) {
        let driver = self.driver
        Task { await driver.check(id) }
    }

    func rename(_ id: Int64, label: String) {
        let driver = self.driver
        Task { await driver.rename(id, label: label) }
    }

    func setRules(_ id: Int64, rulesId: Int64) {
        let driver = self.driver
        Task { await driver.setRules(id, rulesId: rulesId) }
    }

    func setTags(_ id: Int64, tags: [String], onlyTagged: Bool) {
        let driver = self.driver
        Task { await driver.setTags(id, tags: tags, onlyTagged: onlyTagged) }
    }

    func setAudio(_ id: Int64, text: String) {
        let driver = self.driver
        Task { await driver.setAudio(id, text: text) }
    }

    func setCopiesExpected(_ id: Int64, on: Bool) {
        let driver = self.driver
        Task { await driver.setCopiesExpected(id, on: on) }
    }

    func setLookOnly(_ id: Int64, on: Bool) {
        let driver = self.driver
        Task { await driver.setLookOnly(id, on: on) }
    }

    func setFlat(_ id: Int64, on: Bool) {
        let driver = self.driver
        Task { await driver.setFlat(id, on: on) }
    }

    func changePath(_ id: Int64, path: String) async -> Bool {
        await driver.changePath(id, path: path)
    }

    func remove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.remove(id) }
    }

    func saveImportFolders(extra: String, watch: String) {
        let driver = self.driver
        Task { await driver.saveImportFolders(extra: extra, watch: watch) }
    }

    func saveSideFiles(on: Bool, types: String) {
        let driver = self.driver
        Task { await driver.saveSideFiles(on: on, types: types) }
    }

    func setHandsOff(_ on: Bool) {
        let driver = self.driver
        Task { await driver.setHandsOff(on) }
    }

    func confirmPassword(password: String, code: String) {
        let driver = self.driver
        Task { await driver.confirmPassword(password: password, code: code) }
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

enum LibraryFoldersText {
    static func kindTitle(_ kind: LibraryFolderKindID) -> String {
        switch kind {
        case .movie: L10n.tr("library.kind.movie")
        case .show: L10n.tr("library.kind.show")
        case .music: L10n.tr("library.kind.music")
        }
    }

    static func kindBadge(_ kind: LibraryFolderKindID) -> String {
        switch kind {
        case .movie: L10n.tr("library.folders.for.movie")
        case .show: L10n.tr("library.folders.for.show")
        case .music: L10n.tr("library.folders.for.music")
        }
    }

    static func pathPlaceholder(_ kind: LibraryFolderKindID) -> String {
        switch kind {
        case .movie: "/movies"
        case .show: "/shows"
        case .music: "/music"
        }
    }

    static func health(_ health: LibraryFolderHealthID) -> String {
        switch health {
        case .ok: L10n.tr("library.folders.health.ok")
        case .missing: L10n.tr("library.folders.health.missing")
        case .readOnly: L10n.tr("library.folders.health.read.only")
        case .noHardlink: L10n.tr("library.folders.health.no.hardlink")
        case .copiesExpected: L10n.tr("library.folders.health.copies.expected")
        case .lookOnly: L10n.tr("library.folders.health.look.only")
        case .unknown: L10n.tr("library.folders.health.unknown")
        }
    }

    static func reasonColor(_ tone: ToneKind) -> Color {
        switch tone {
        case .fail: Theme.failText
        case .wait: Theme.waitText
        default: Theme.textMuted
        }
    }

    static func quality(_ row: LibraryFoldersRowInfo) -> String {
        row.rulesIsDefault ? L10n.tr("library.folders.quality.default") : row.rulesName
    }

    static func tagList(_ text: String) -> [String] {
        var seen = Set<String>()
        var tags: [String] = []
        for part in text.split(whereSeparator: { $0 == "," || $0.isNewline }) {
            let words = part.lowercased().split(whereSeparator: { $0.isWhitespace })
            let joined = words.joined(separator: "-")
            let trimmed = String(joined.trimmingCharacters(in: CharacterSet(charactersIn: "-,")).prefix(40))
            if !trimmed.isEmpty, seen.insert(trimmed).inserted {
                tags.append(trimmed)
            }
            if tags.count == 20 { break }
        }
        return tags
    }

    static func audioList(_ text: String) -> [String] {
        let parts = text.split(whereSeparator: { $0 == "," || $0 == ";" || $0.isWhitespace })
        return parts.map { $0.lowercased() }
    }
}

private struct LibraryFoldersSelection: Identifiable, Hashable {
    let id: Int64
}

struct LibraryFoldersView: View {
    @State private var store = LibraryFoldersStore()
    @State private var showAdd = false
    @State private var selected: LibraryFoldersSelection?
    @State private var removing: LibraryFoldersRowInfo?
    @State private var confirmRemove = false

    var body: some View {
        @Bindable var model = store
        StatusContent(load: store.info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            foldersList
        }
        .navigationTitle(L10n.tr("library.folders.title"))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                if store.info.canEdit {
                    Button(L10n.tr("library.folders.add"), systemImage: "plus") { showAdd = true }
                }
            }
        }
        .libraryNotice(store.info.notice) { store.dismissNotice($0) }
        .confirmationDialog(removeTitle, isPresented: $confirmRemove, titleVisibility: .visible) {
            Button(L10n.tr("library.folders.remove"), role: .destructive) {
                if let removing { store.remove(removing.id) }
            }
        }
        .sheet(isPresented: $showAdd) {
            LibraryFoldersAddSheet(store: store)
        }
        .sheet(item: $selected) { item in
            LibraryFoldersDetailSheet(store: store, id: item.id)
        }
        .sheet(isPresented: $model.passwordOpen) {
            LibraryFoldersPasswordSheet(store: store)
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var removeTitle: String {
        L10n.format("library.folders.remove.confirm", removing?.path ?? "")
    }

    private var foldersList: some View {
        List {
            if store.info.isOwner {
                handsOffSection
            }
            if store.info.isEmpty {
                emptySection
            } else {
                ForEach(store.info.groups) { group in
                    groupSection(group)
                }
            }
            if let importFolders = store.info.importFolders {
                LibraryFoldersImportCard(store: store, folders: importFolders)
            }
            if let sideFiles = store.info.sideFiles {
                LibraryFoldersSideCard(store: store, side: sideFiles)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Theme.background)
        .refreshable { await store.refresh() }
    }

    private var handsOffSection: some View {
        @Bindable var model = store
        return Section {
            Toggle(isOn: $model.handsOff) {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("library.folders.hands.off.label"))
                        .font(.subheadline.weight(.semibold))
                    Text(L10n.tr("library.folders.hands.off.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            .disabled(store.info.handsOffBusy)
        } header: {
            Text(L10n.tr("library.folders.hands.off"))
        } footer: {
            Text(L10n.tr("library.folders.hands.off.text"))
        }
    }

    private var emptySection: some View {
        Section {
            ContentUnavailableView {
                Label(L10n.tr("library.folders.empty.title"), systemImage: "folder")
            } description: {
                Text(L10n.tr("library.folders.empty.message"))
            } actions: {
                if store.info.canEdit {
                    Button(L10n.tr("library.folders.add")) { showAdd = true }
                        .buttonStyle(.borderedProminent)
                }
            }
            .listRowBackground(Color.clear)
        }
    }

    private func groupSection(_ group: LibraryFoldersGroupInfo) -> some View {
        Section {
            ForEach(group.folders) { row in
                LibraryFoldersRowView(
                    row: row,
                    rules: store.info.rules(for: row.kind),
                    rulesAvailable: store.info.rulesAvailable,
                    canEdit: store.info.canEdit,
                    details: { selected = LibraryFoldersSelection(id: row.id) },
                    check: { store.check(row.id) },
                    setRules: { store.setRules(row.id, rulesId: $0) }
                )
                .swipeActions(edge: .leading) {
                    Button {
                        store.check(row.id)
                    } label: {
                        Label(L10n.tr("library.folders.check"), systemImage: "arrow.clockwise")
                    }
                    .tint(Theme.info)
                }
                .swipeActions(edge: .trailing) {
                    if store.info.canEdit {
                        Button(role: .destructive) {
                            removing = row
                            confirmRemove = true
                        } label: {
                            Label(L10n.tr("library.folders.remove"), systemImage: "trash")
                        }
                    }
                }
            }
        } header: {
            Text(L10n.format("library.copy.line", LibraryFoldersText.kindTitle(group.kind), String(group.folders.count)))
        }
    }
}

private struct LibraryFoldersRowView: View {
    let row: LibraryFoldersRowInfo
    let rules: [MenuOptionInfo]
    let rulesAvailable: Bool
    let canEdit: Bool
    let details: @MainActor () -> Void
    let check: @MainActor () -> Void
    let setRules: @MainActor (Int64) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Button {
                details()
            } label: {
                topLine
            }
            .buttonStyle(.plain)
            .contextMenu {
                Button(L10n.tr("library.folders.check"), systemImage: "arrow.clockwise") { check() }
                Button(L10n.tr("library.folders.details"), systemImage: "info.circle") { details() }
            }
            if rulesAvailable {
                qualityControl
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var topLine: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            Image(systemName: "folder")
                .font(.title3)
                .foregroundStyle(Theme.textMuted)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(row.title)
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                Text(LibraryFoldersText.kindBadge(row.kind))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Text(row.shortPath)
                    .font(.footnote.monospaced())
                    .foregroundStyle(Theme.textMuted)
                StatusChip(text: LibraryFoldersText.health(row.health), tone: row.tone)
                if !row.reason.isEmpty, row.tone != .ok {
                    Text(row.reason)
                        .font(.footnote)
                        .foregroundStyle(LibraryFoldersText.reasonColor(row.tone))
                        .multilineTextAlignment(.leading)
                }
                if row.freeSpace > 0 {
                    Text(L10n.format("library.folders.free", LibraryText.bytes(row.freeSpace)))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if row.busy {
                ProgressView()
            } else {
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.textFaint)
            }
        }
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var qualityControl: some View {
        if canEdit {
            Menu {
                Button {
                    setRules(0)
                } label: {
                    if row.rulesIsDefault {
                        Label(L10n.tr("library.folders.quality.default"), systemImage: "checkmark")
                    } else {
                        Text(L10n.tr("library.folders.quality.default"))
                    }
                }
                ForEach(rules) { rule in
                    Button {
                        setRules(rule.id)
                    } label: {
                        if !row.rulesIsDefault, rule.id == row.rulesId {
                            Label(rule.label, systemImage: "checkmark")
                        } else {
                            Text(rule.label)
                        }
                    }
                }
            } label: {
                qualityLabel(chevron: true)
            }
            .disabled(row.busy)
        } else {
            qualityLabel(chevron: false)
        }
    }

    private func qualityLabel(chevron: Bool) -> some View {
        HStack(spacing: Theme.Spacing.small) {
            Text(L10n.tr("library.folders.quality"))
                .foregroundStyle(Theme.textMuted)
            Text(LibraryFoldersText.quality(row))
                .fontWeight(.semibold)
                .foregroundStyle(Theme.text)
            if chevron {
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption)
                    .foregroundStyle(Theme.textFaint)
            }
        }
        .font(.subheadline)
    }
}

private struct LibraryFoldersToggle: View {
    let title: String
    var hint: String?
    let isOn: Bool
    let busy: Bool
    let enabled: Bool
    let change: @MainActor (Bool) -> Void
    @State private var local = false

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Toggle(title, isOn: $local)
                .disabled(!enabled)
            if let hint {
                Text(hint)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .onChange(of: isOn, initial: true) { _, value in
            local = value
        }
        .onChange(of: busy) { _, working in
            if !working { local = isOn }
        }
        .onChange(of: local) { _, value in
            if value != isOn { change(value) }
        }
    }
}

private struct LibraryFoldersTagSuggestions: View {
    let suggestions: [String]
    let used: [String]
    let pick: @MainActor (String) -> Void

    var body: some View {
        let fresh = suggestions.filter { !used.contains($0) }.prefix(12)
        if !fresh.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(Array(fresh), id: \.self) { tag in
                        Button {
                            pick(tag)
                        } label: {
                            Text(tag)
                                .font(.footnote.weight(.medium))
                                .foregroundStyle(Theme.text)
                                .padding(.horizontal, Theme.Spacing.medium)
                                .padding(.vertical, Theme.Spacing.small)
                                .background(Theme.surfaceRaised, in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

private struct LibraryFoldersDetailSheet: View {
    let store: LibraryFoldersStore
    let id: Int64
    @Environment(\.dismiss) private var dismiss
    @State private var renaming = false
    @State private var renameDraft = ""
    @State private var moving = false
    @State private var moveDraft = ""
    @State private var moveBusy = false
    @State private var confirmRemove = false
    @State private var tagsDraft = ""
    @State private var audioDraft = ""

    private var row: LibraryFoldersRowInfo? {
        store.info.groups.flatMap { $0.folders }.first { $0.id == id }
    }

    var body: some View {
        NavigationStack {
            List {
                if let row {
                    infoSection(row)
                    actionSection(row)
                    tagsSection(row)
                    if row.kind != .music, store.info.canEdit || !row.audio.isEmpty {
                        audioSection(row)
                    }
                    switchSection(row)
                    if store.info.canEdit {
                        removeSection(row)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(Theme.background)
            .navigationTitle(row?.title ?? "")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.close")) { dismiss() }
                }
            }
            .libraryNotice(store.info.notice) { store.dismissNotice($0) }
            .alert(L10n.tr("library.folders.rename"), isPresented: $renaming) {
                TextField(L10n.tr("library.folders.name.field"), text: $renameDraft)
                Button(L10n.tr("library.save")) { saveRename() }
                Button(L10n.tr("library.cancel"), role: .cancel) {}
            } message: {
                Text(row?.path ?? "")
            }
            .alert(L10n.tr("library.folders.change.path"), isPresented: $moving) {
                TextField(L10n.tr("library.folders.new.path"), text: $moveDraft)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Button(L10n.tr("library.folders.change.path")) { saveMove() }
                Button(L10n.tr("library.cancel"), role: .cancel) {}
            } message: {
                Text(L10n.tr("library.folders.change.path.hint"))
            }
            .confirmationDialog(L10n.format("library.folders.remove.confirm", row?.path ?? ""), isPresented: $confirmRemove, titleVisibility: .visible) {
                Button(L10n.tr("library.folders.remove"), role: .destructive) {
                    store.remove(id)
                    dismiss()
                }
            }
            .onChange(of: row == nil) { _, gone in
                if gone { dismiss() }
            }
            .onChange(of: row?.tags, initial: true) { _, tags in
                tagsDraft = (tags ?? []).joined(separator: ", ")
            }
            .onChange(of: row?.audio, initial: true) { _, audio in
                audioDraft = (audio ?? []).joined(separator: ", ")
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func infoSection(_ row: LibraryFoldersRowInfo) -> some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(row.path)
                    .font(.footnote.monospaced())
                    .textSelection(.enabled)
                Text(LibraryFoldersText.kindBadge(row.kind))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                StatusChip(text: LibraryFoldersText.health(row.health), tone: row.tone)
                if !row.reason.isEmpty, row.tone != .ok {
                    Text(row.reason)
                        .font(.footnote)
                        .foregroundStyle(LibraryFoldersText.reasonColor(row.tone))
                }
                if row.freeSpace > 0 {
                    Text(L10n.format("library.folders.free", LibraryText.bytes(row.freeSpace)))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                if !row.checkedAt.isEmpty {
                    Text(L10n.format("library.folders.checked.at", LibraryText.ago(row.checkedAt)))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        } header: {
            Text(L10n.tr("library.folders.full.path"))
        }
    }

    private func actionSection(_ row: LibraryFoldersRowInfo) -> some View {
        Section {
            Button {
                store.check(row.id)
            } label: {
                HStack {
                    Label(L10n.tr("library.folders.check"), systemImage: "arrow.clockwise")
                    if row.busy {
                        ProgressView()
                    }
                }
            }
            .disabled(row.busy)
            if store.info.canEdit, row.health == .noHardlink || row.health == .copiesExpected {
                Button {
                    store.setCopiesExpected(row.id, on: !row.copiesExpected)
                } label: {
                    Text(row.copiesExpected ? L10n.tr("library.folders.warn.again") : L10n.tr("library.folders.copies.expected"))
                }
                .disabled(row.busy)
            }
            if store.info.canEdit {
                Button {
                    renameDraft = row.title
                    renaming = true
                } label: {
                    Label(L10n.tr("library.folders.rename"), systemImage: "pencil")
                }
                .disabled(row.busy)
                Button {
                    moveDraft = row.path
                    moving = true
                } label: {
                    Label(L10n.tr("library.folders.change.path"), systemImage: "folder.badge.gearshape")
                }
                .disabled(row.busy)
            }
        }
    }

    @ViewBuilder
    private func tagsSection(_ row: LibraryFoldersRowInfo) -> some View {
        if store.info.canEdit || !row.tags.isEmpty {
            Section {
                TextField(L10n.tr("library.folders.tags"), text: $tagsDraft)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .disabled(!store.info.canEdit)
                if store.info.canEdit {
                    LibraryFoldersTagSuggestions(
                        suggestions: store.info.tagSuggestions,
                        used: LibraryFoldersText.tagList(tagsDraft)
                    ) { tag in
                        tagsDraft = tagsDraft.trimmingCharacters(in: .whitespaces).isEmpty ? tag : tagsDraft + ", " + tag
                    }
                }
                if tagsChanged(row) {
                    Button(L10n.tr("library.save")) {
                        store.setTags(row.id, tags: LibraryFoldersText.tagList(tagsDraft), onlyTagged: row.onlyTagged)
                    }
                    .disabled(row.busy)
                }
                if !row.tags.isEmpty {
                    LibraryFoldersToggle(
                        title: L10n.tr("library.folders.only.tagged"),
                        hint: L10n.tr("library.folders.only.tagged.hint"),
                        isOn: row.onlyTagged,
                        busy: row.busy,
                        enabled: store.info.canEdit && !row.busy
                    ) { store.setTags(row.id, tags: row.tags, onlyTagged: $0) }
                }
            } header: {
                Text(L10n.tr("library.folders.tags"))
            } footer: {
                Text(L10n.tr("library.folders.tags.hint"))
            }
        }
    }

    private func tagsChanged(_ row: LibraryFoldersRowInfo) -> Bool {
        Set(LibraryFoldersText.tagList(tagsDraft)) != Set(row.tags)
    }

    private func audioSection(_ row: LibraryFoldersRowInfo) -> some View {
        Section {
            TextField(L10n.tr("library.folders.audio"), text: $audioDraft)
                .font(.body.monospaced())
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .disabled(!store.info.canEdit)
            if Set(LibraryFoldersText.audioList(audioDraft)) != Set(row.audio) {
                Button(L10n.tr("library.save")) {
                    store.setAudio(row.id, text: audioDraft)
                }
                .disabled(row.busy)
            }
        } header: {
            Text(L10n.tr("library.folders.audio"))
        } footer: {
            Text(L10n.tr("library.folders.audio.hint"))
        }
    }

    @ViewBuilder
    private func switchSection(_ row: LibraryFoldersRowInfo) -> some View {
        if store.info.canEdit || row.lookOnly {
            Section {
                LibraryFoldersToggle(
                    title: L10n.tr("library.folders.look.only"),
                    hint: L10n.tr("library.folders.look.only.hint"),
                    isOn: row.lookOnly,
                    busy: row.busy,
                    enabled: store.info.canEdit && !row.busy
                ) { store.setLookOnly(row.id, on: $0) }
            }
        }
        if row.kind == .movie, store.info.canEdit || row.flat {
            Section {
                LibraryFoldersToggle(
                    title: L10n.tr("library.folders.flat"),
                    hint: L10n.tr("library.folders.flat.hint"),
                    isOn: row.flat,
                    busy: row.busy,
                    enabled: store.info.canEdit && !row.busy
                ) { store.setFlat(row.id, on: $0) }
            }
        }
    }

    private func removeSection(_ row: LibraryFoldersRowInfo) -> some View {
        Section {
            Button(role: .destructive) {
                confirmRemove = true
            } label: {
                Label(L10n.tr("library.folders.remove"), systemImage: "trash")
            }
            .disabled(row.busy)
        }
    }

    private func saveRename() {
        guard let row else { return }
        let name = renameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        store.rename(row.id, label: name)
    }

    private func saveMove() {
        guard let row, !moveBusy else { return }
        let newPath = moveDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !newPath.isEmpty else { return }
        moveBusy = true
        Task {
            _ = await store.changePath(row.id, path: newPath)
            moveBusy = false
        }
    }
}

private struct LibraryFoldersAddSheet: View {
    let store: LibraryFoldersStore
    @Environment(\.dismiss) private var dismiss
    @State private var kind: LibraryFolderKindID = .movie
    @State private var path = ""
    @State private var label = ""
    @State private var rulesId: Int64 = 0
    @State private var tags = ""
    @State private var onlyTagged = false
    @State private var lookOnly = false
    @State private var flat = false

    var body: some View {
        NavigationStack {
            Form {
                kindSection
                pathSection
                if store.info.rulesAvailable {
                    qualitySection
                }
                tagsSection
                optionsSection
            }
            .navigationTitle(L10n.tr("library.folders.add.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("library.folders.add.button")) { add() }
                        .disabled(path.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || store.info.adding)
                }
            }
            .libraryNotice(store.info.notice) { store.dismissNotice($0) }
            .onChange(of: kind) { _, newKind in
                rulesId = 0
                if newKind != .movie { flat = false }
            }
        }
        .presentationDetents([.large])
    }

    private var tagList: [String] {
        LibraryFoldersText.tagList(tags)
    }

    private var kindSection: some View {
        Section {
            Picker(L10n.tr("library.folders.holds"), selection: $kind) {
                ForEach(LibraryFolderKindID.allCases) { option in
                    Text(LibraryFoldersText.kindTitle(option)).tag(option)
                }
            }
        } footer: {
            Text(L10n.tr("library.folders.holds.hint"))
        }
    }

    private var pathSection: some View {
        Section {
            TextField(L10n.tr("library.folders.path"), text: $path, prompt: Text(verbatim: LibraryFoldersText.pathPlaceholder(kind)))
                .font(.body.monospaced())
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.URL)
            TextField(L10n.tr("library.folders.name"), text: $label)
                .onChange(of: label) { _, text in
                    if text.count > 64 { label = String(text.prefix(64)) }
                }
        } footer: {
            Text(L10n.tr("library.folders.path.hint"))
        }
    }

    private var qualitySection: some View {
        Section {
            Picker(L10n.tr("library.folders.quality"), selection: $rulesId) {
                Text(L10n.tr("library.folders.quality.default")).tag(Int64(0))
                ForEach(store.info.rules(for: kind)) { rule in
                    Text(rule.label).tag(rule.id)
                }
            }
        } footer: {
            Text(L10n.tr("library.folders.quality.hint"))
        }
    }

    private var tagsSection: some View {
        Section {
            TextField(L10n.tr("library.folders.tags"), text: $tags)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            LibraryFoldersTagSuggestions(suggestions: store.info.tagSuggestions, used: tagList) { tag in
                tags = tags.trimmingCharacters(in: .whitespaces).isEmpty ? tag : tags + ", " + tag
            }
            if !tagList.isEmpty {
                Toggle(isOn: $onlyTagged) {
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(L10n.tr("library.folders.only.tagged"))
                        Text(L10n.tr("library.folders.only.tagged.hint"))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
        } header: {
            Text(L10n.tr("library.folders.tags"))
        } footer: {
            Text(L10n.tr("library.folders.tags.hint"))
        }
    }

    private var optionsSection: some View {
        Section {
            Toggle(isOn: $lookOnly) {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("library.folders.look.only"))
                    Text(L10n.tr("library.folders.look.only.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            if kind == .movie {
                Toggle(isOn: $flat) {
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(L10n.tr("library.folders.flat"))
                        Text(L10n.tr("library.folders.flat.hint"))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
        }
    }

    private func add() {
        guard !store.info.adding else { return }
        let chosenTags = tagList
        Task {
            let added = await store.add(
                kind: kind,
                path: path,
                label: label,
                rulesId: rulesId,
                tags: chosenTags,
                onlyTagged: onlyTagged && !chosenTags.isEmpty,
                lookOnly: lookOnly,
                flat: flat && kind == .movie
            )
            if added { dismiss() }
        }
    }
}

private struct LibraryFoldersPasswordSheet: View {
    let store: LibraryFoldersStore
    @Environment(\.dismiss) private var dismiss
    @State private var password = ""
    @State private var code = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    SecureField(L10n.tr("library.folders.password.label"), text: $password)
                        .textContentType(.password)
                    TextField(L10n.tr("library.folders.code.label"), text: $code)
                        .textContentType(.oneTimeCode)
                        .autocorrectionDisabled()
                } footer: {
                    Text(L10n.tr("library.folders.password.text"))
                }
                if let error = store.info.passwordError {
                    Section {
                        NoticeBanner(text: LibraryText.error(error))
                            .listRowInsets(EdgeInsets())
                            .listRowBackground(Color.clear)
                    }
                }
            }
            .navigationTitle(L10n.tr("library.folders.password.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("library.folders.password.confirm")) {
                        store.confirmPassword(password: password, code: code)
                    }
                    .disabled(password.isEmpty || store.info.handsOffBusy)
                }
            }
        }
        .presentationDetents([.medium])
    }
}

private struct LibraryFoldersImportCard: View {
    let store: LibraryFoldersStore
    let folders: LibraryFoldersImportInfo
    @State private var extra = ""
    @State private var watch = ""

    var body: some View {
        Section {
            editor(
                title: L10n.tr("library.folders.import.folders"),
                hint: L10n.tr("library.folders.import.folders.hint"),
                text: $extra,
                lines: 3
            )
            .onChange(of: folders, initial: true) { _, value in
                extra = value.extra.joined(separator: "\n")
                watch = value.watch.joined(separator: "\n")
            }
            editor(
                title: L10n.tr("library.folders.import.watch"),
                hint: L10n.tr("library.folders.import.watch.hint"),
                text: $watch,
                lines: 2
            )
            if store.info.canSaveImportFolders {
                Button(L10n.tr("library.save")) {
                    store.saveImportFolders(extra: extra, watch: watch)
                }
            } else {
                Text(L10n.tr("library.folders.owner.only"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        } header: {
            Text(L10n.tr("library.folders.import"))
        } footer: {
            Text(L10n.tr("library.folders.import.text"))
        }
    }

    private func editor(title: String, hint: String, text: Binding<String>, lines: Int) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.subheadline.weight(.semibold))
            TextEditor(text: text)
                .font(.footnote.monospaced())
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .scrollContentBackground(.hidden)
                .frame(minHeight: CGFloat(lines) * 22 + Theme.Spacing.medium)
                .padding(Theme.Spacing.small)
                .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
                .disabled(!store.info.canSaveImportFolders)
            Text(hint)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }
}

private struct LibraryFoldersSideCard: View {
    let store: LibraryFoldersStore
    let side: LibraryFoldersSideInfo
    @State private var on = false
    @State private var types = ""

    var body: some View {
        Section {
            Toggle(isOn: $on) {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("library.folders.side.on"))
                        .font(.subheadline.weight(.semibold))
                    Text(L10n.tr("library.folders.side.on.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            .disabled(!store.info.canEdit)
            .onChange(of: side, initial: true) { _, value in
                on = value.on
                types = value.types.joined(separator: ", ")
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("library.folders.side.types"))
                    .font(.subheadline.weight(.semibold))
                TextField(L10n.tr("library.folders.side.types"), text: $types, prompt: Text(verbatim: "srt"))
                    .font(.body.monospaced())
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .disabled(!store.info.canEdit)
                Text(L10n.tr("library.folders.side.types.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if store.info.canEdit {
                Button(L10n.tr("library.save")) {
                    store.saveSideFiles(on: on, types: types)
                }
            } else {
                Text(L10n.tr("library.folders.owner.only"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        } header: {
            Text(L10n.tr("library.folders.side"))
        } footer: {
            Text(L10n.tr("library.folders.side.text"))
        }
    }
}
