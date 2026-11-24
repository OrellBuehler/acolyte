import SwiftUI
import Observation

@MainActor
@Observable
final class LibraryFilesStore {
    private(set) var info: LibraryFilesInfo = .initial
    private(set) var ready = false

    private let driver: LibraryFilesDriver

    init(path: String) {
        driver = SDKClient.shared.makeLibraryFiles(path: path)
    }

    var mode: LibraryFilesModeID {
        get { info.mode }
        set { driver.setMode(newValue) }
    }

    var browserOpen: Bool {
        get { info.browser != nil }
        set {
            if !newValue { driver.closeBrowser() }
        }
    }

    var editorOpen: Bool {
        get { info.editor != nil }
        set {
            if !newValue { driver.closeEditor() }
        }
    }

    var versionChoice: Int64 {
        get { info.editor?.versionId ?? 0 }
        set { driver.editorVersion(newValue) }
    }

    var seasonChoice: Int {
        get { info.editor?.season ?? -1 }
        set { driver.editorSeason(newValue) }
    }

    var episodeChoice: Int {
        get { info.editor?.episode ?? -1 }
        set { driver.editorEpisode(newValue) }
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
            self?.ready = true
        }
    }

    func load() async {
        await driver.load()
    }

    func rescan(path: String) async {
        await driver.scan(path: path)
    }

    func scan(path: String) {
        let driver = self.driver
        Task { await driver.scan(path: path) }
    }

    func toggle(_ path: String) {
        driver.toggle(path)
    }

    func setReplace(path: String, on: Bool) {
        driver.setReplace(path: path, on: on)
    }

    func importFiles() {
        let driver = self.driver
        Task { await driver.importFiles() }
    }

    func openBrowser() {
        let driver = self.driver
        Task { await driver.openBrowser() }
    }

    func browse(_ path: String) {
        let driver = self.driver
        Task { await driver.browse(path) }
    }

    func browseUp() {
        let driver = self.driver
        Task { await driver.browseUp() }
    }

    func useBrowsed() {
        let driver = self.driver
        Task { await driver.useBrowsed() }
    }

    func openEditor(_ path: String) {
        driver.openEditor(path)
    }

    func editorSearch(_ query: String) {
        let driver = self.driver
        Task { await driver.editorSearch(query) }
    }

    func editorChoose(_ titleId: Int64) {
        let driver = self.driver
        Task { await driver.editorChoose(titleId) }
    }

    func editorUse() {
        driver.editorUse()
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

enum LibraryFilesText {
    static func pickLine(_ pick: LibraryFilesPickInfo?) -> String {
        guard let pick else { return L10n.tr("library.files.no.match") }
        let name = LibraryImportText.titleYear(pick.titleName, pick.year)
        switch pick.titleKind {
        case .movie:
            return name
        case .show:
            let episode = pick.season >= 0 && pick.episode >= 0 ? episodeCode(pick.season, pick.episode) : L10n.tr("library.files.pick.episode")
            return [name, episode].joined(separator: " · ")
        case .artist:
            guard pick.albumId > 0 else { return [name, L10n.tr("library.files.pick.album")].joined(separator: " · ") }
            let album = LibraryImportText.titleYear(pick.albumName, pick.albumYear)
            let track = pick.slotId > 0 ? trackLine(pick) : L10n.tr("library.files.pick.track")
            return [name, album, track].joined(separator: " · ")
        }
    }

    static func episodeCode(_ season: Int, _ episode: Int) -> String {
        "S" + twoDigits(season) + "E" + twoDigits(episode)
    }

    static func trackLine(_ pick: LibraryFilesPickInfo) -> String {
        let disc = pick.disc > 1 ? String(pick.disc) + "-" : ""
        let number = disc + twoDigits(pick.track)
        return pick.trackName.isEmpty ? number : number + " " + pick.trackName
    }

    static func twoDigits(_ value: Int) -> String {
        value < 10 ? "0" + String(value) : String(value)
    }

    static func season(_ season: Int) -> String {
        season == 0 ? L10n.tr("library.files.season.specials") : L10n.format("library.files.season.n", season)
    }

    static func episodeTitle(_ episode: LibraryFilesEpisodeInfo) -> String {
        let code = episodeCode(episode.season, episode.episode)
        return episode.name.isEmpty ? code : L10n.format("library.copy.line", code, episode.name)
    }

    static func kind(_ kind: LibraryFilesTitleKindID) -> String {
        switch kind {
        case .movie: L10n.tr("library.files.kind.movie")
        case .show: L10n.tr("library.files.kind.show")
        case .artist: L10n.tr("library.kind.music")
        }
    }

    static func blocker(_ blocker: LibraryFilesBlockerID) -> String? {
        switch blocker {
        case .clear: nil
        case .inLibrary: L10n.tr("library.files.blocker.in.library")
        case .noPick: L10n.tr("library.files.blocker.no.pick")
        case .noEpisode: L10n.tr("library.files.blocker.no.episode")
        case .hasFile: L10n.tr("library.files.blocker.has.file")
        case .noTrack: L10n.tr("library.files.blocker.no.slot")
        }
    }

    static func result(_ result: LibraryFilesResultInfo) -> String {
        switch result.outcome {
        case .placed:
            let how = placedBy(result.placedBy)
            return result.error.isEmpty ? how : L10n.format("library.files.result.placed.error", how, result.error)
        case .refused:
            return L10n.format("library.files.result.refused", why(result))
        case .failed:
            return L10n.format("library.files.result.failed", why(result))
        }
    }

    private static func why(_ result: LibraryFilesResultInfo) -> String {
        result.error.isEmpty ? L10n.tr("library.files.result.no.reason") : result.error
    }

    private static func placedBy(_ placedBy: LibraryFilesPlacedByID) -> String {
        switch placedBy {
        case .move: L10n.tr("library.files.result.moved")
        case .copy: L10n.tr("library.files.result.copied")
        case .hardlink: L10n.tr("library.files.result.linked")
        case .other: L10n.tr("library.files.result.recorded")
        }
    }
}

struct LibraryFilesView: View {
    @State private var store: LibraryFilesStore
    @State private var pathText: String
    @State private var confirmReplace = false
    @FocusState private var pathFocused: Bool

    init(path: String) {
        _store = State(initialValue: LibraryFilesStore(path: path))
        _pathText = State(initialValue: path)
    }

    var body: some View {
        @Bindable var model = store
        content
            .navigationTitle(L10n.tr("library.files.title"))
            .libraryNotice(store.info.notice) { store.dismissNotice($0) }
            .confirmationDialog(L10n.tr("library.files.import.confirm"), isPresented: $confirmReplace, titleVisibility: .visible) {
                Button(L10n.tr("library.files.import.confirm")) { store.importFiles() }
            } message: {
                Text(LibraryText.plural("library.files.replace.ask", store.info.replaceCount))
            }
            .sheet(isPresented: $model.browserOpen) {
                LibraryFilesBrowserSheet(store: store)
            }
            .sheet(isPresented: $model.editorOpen) {
                LibraryFilesEditorSheet(store: store)
            }
            .onChange(of: store.info.path) { _, newPath in
                if !pathFocused, newPath != pathText {
                    pathText = newPath
                }
            }
            .task { await store.watch() }
            .task { await store.load() }
    }

    @ViewBuilder
    private var content: some View {
        if !store.ready {
            LoadingStateView()
        } else if !store.info.allowed {
            EmptyStateView(title: L10n.tr("library.files.owner"), systemImage: "shield")
        } else {
            filesList
        }
    }

    private var filesList: some View {
        List {
            pickSection
            if store.info.scanned {
                checkSection
                if !store.info.rows.isEmpty {
                    LibraryFilesModeSection(store: store)
                }
            } else {
                Section {
                    Text(L10n.tr("library.files.nothing.moves"))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Theme.background)
        .scrollDismissesKeyboard(.interactively)
        .refreshable { await refreshFiles() }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if store.info.scanned, !store.info.rows.isEmpty {
                LibraryFilesFooter(store: store) { startImport() }
            }
        }
    }

    private var trimmedPath: String {
        pathText.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var pickSection: some View {
        Section {
            TextField(L10n.tr("library.files.path.label"), text: $pathText, prompt: Text(verbatim: "/downloads/complete/Some.Show.S01"))
                .font(.body.monospaced())
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .keyboardType(.URL)
                .submitLabel(.search)
                .focused($pathFocused)
                .onSubmit { scan() }
            HStack(spacing: Theme.Spacing.medium) {
                if store.info.canBrowse {
                    Button(L10n.tr("library.files.browse"), systemImage: "folder") {
                        store.openBrowser()
                    }
                    .buttonStyle(.bordered)
                    .disabled(store.info.importing)
                }
                Spacer(minLength: 0)
                Button {
                    scan()
                } label: {
                    HStack(spacing: Theme.Spacing.small) {
                        Text(L10n.tr("library.files.scan"))
                        if store.info.scanning {
                            ProgressView()
                        }
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .disabled(store.info.scanning || store.info.importing || trimmedPath.isEmpty)
            }
            Text(L10n.tr("library.files.where"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if let error = store.info.error {
                NoticeBanner(text: LibraryText.error(error))
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
        } header: {
            Text(L10n.tr("library.files.step.pick"))
        }
    }

    private var checkSection: some View {
        Section {
            if store.info.rows.isEmpty {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("library.files.no.files"))
                        .font(.headline)
                    Text(L10n.tr("library.files.no.files.hint"))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                }
            } else {
                Text(LibraryText.plural("library.files.in", store.info.rows.count, store.info.scanPath))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                if store.info.more {
                    Text(L10n.format("library.files.only.first", store.info.rows.count))
                        .font(.footnote)
                        .foregroundStyle(Theme.waitText)
                }
                ForEach(store.info.rows) { row in
                    LibraryFilesRowView(
                        row: row,
                        importing: store.info.importing,
                        toggle: { store.toggle(row.path) },
                        change: { store.openEditor(row.path) },
                        setReplace: { store.setReplace(path: row.path, on: $0) }
                    )
                }
            }
        } header: {
            Text(L10n.tr("library.files.step.check"))
        }
    }

    private func refreshFiles() async {
        guard store.info.scanned else { return }
        await store.rescan(path: pathText)
    }

    private func scan() {
        guard !trimmedPath.isEmpty, !store.info.scanning, !store.info.importing else { return }
        pathFocused = false
        store.scan(path: trimmedPath)
    }

    private func startImport() {
        if store.info.replaceCount > 0 {
            confirmReplace = true
        } else {
            store.importFiles()
        }
    }
}

private struct LibraryFilesRowView: View {
    let row: LibraryFilesRowInfo
    let importing: Bool
    let toggle: @MainActor () -> Void
    let change: @MainActor () -> Void
    let setReplace: @MainActor (Bool) -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            tickButton
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(row.name)
                    .font(.subheadline.weight(.semibold))
                    .fixedSize(horizontal: false, vertical: true)
                Text(sizeLine)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                matchLine
                if row.canChange {
                    Button(L10n.tr("library.files.change")) { change() }
                        .buttonStyle(.borderless)
                        .font(.subheadline.weight(.semibold))
                        .disabled(importing)
                }
                if showReplace {
                    replaceButton
                }
                if row.result?.outcome != .placed, let hint {
                    Text(hint)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                if let result = row.result {
                    Text(LibraryFilesText.result(result))
                        .font(.footnote)
                        .foregroundStyle(result.outcome == .placed ? Theme.okText : Theme.failText)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var tickButton: some View {
        Button {
            toggle()
        } label: {
            Image(systemName: row.ticked ? "checkmark.circle.fill" : "circle")
                .font(.title2)
                .foregroundStyle(row.ticked ? Theme.accent : Theme.textFaint)
        }
        .buttonStyle(.borderless)
        .disabled(!row.canTick || importing)
        .accessibilityLabel(L10n.format("library.files.tick", row.name))
        .accessibilityAddTraits(row.ticked ? .isSelected : [])
    }

    private var sizeLine: String {
        let size = LibraryText.bytes(row.size)
        return row.quality.isEmpty ? size : L10n.format("library.copy.line", size, row.quality)
    }

    private var matchLine: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(LibraryFilesText.pickLine(row.pick))
                .font(.callout.weight(.semibold))
            switch row.confidence {
            case .likely:
                StatusChip(text: L10n.tr("library.files.likely"), tone: .ok)
            case .guess:
                StatusChip(text: L10n.tr("library.files.guess"), tone: .wait)
            case .unknown:
                EmptyView()
            }
        }
    }

    private var showReplace: Bool {
        row.pick?.hasFile == true && row.blocker != .inLibrary && row.result == nil
    }

    private var replaceButton: some View {
        Button {
            setReplace(!row.replace)
        } label: {
            Label(L10n.tr("library.files.replace"), systemImage: row.replace ? "checkmark.square.fill" : "square")
                .font(.subheadline)
        }
        .buttonStyle(.borderless)
        .disabled(importing)
        .accessibilityAddTraits(row.replace ? .isSelected : [])
    }

    private var hint: String? {
        if let text = LibraryFilesText.blocker(row.blocker) { return text }
        if !row.note.isEmpty { return row.note }
        if row.duplicate { return L10n.tr("library.files.duplicate") }
        return nil
    }
}

private struct LibraryFilesModeSection: View {
    let store: LibraryFilesStore

    var body: some View {
        @Bindable var model = store
        Section {
            Picker(L10n.tr("library.files.mode.title"), selection: $model.mode) {
                Text(L10n.tr("library.files.mode.copy")).tag(LibraryFilesModeID.copy)
                Text(L10n.tr("library.files.mode.move")).tag(LibraryFilesModeID.move)
            }
            .pickerStyle(.segmented)
            .disabled(store.info.importing)
            Text(store.info.mode == .move ? L10n.tr("library.files.mode.move.text") : L10n.tr("library.files.mode.copy.text"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        } header: {
            Text(L10n.tr("library.files.mode.title"))
        }
    }
}

private struct LibraryFilesFooter: View {
    let store: LibraryFilesStore
    let start: @MainActor () -> Void

    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            Text(L10n.format("library.files.ticked", store.info.tickedCount))
                .font(.subheadline)
                .foregroundStyle(Theme.textMuted)
                .monospacedDigit()
            Spacer(minLength: 0)
            Button {
                start()
            } label: {
                HStack(spacing: Theme.Spacing.small) {
                    if store.info.importing {
                        ProgressView()
                    }
                    Text(LibraryText.plural("library.files.import", store.info.tickedCount))
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            .disabled(store.info.tickedCount == 0 || store.info.importing)
        }
        .padding(Theme.Spacing.medium)
        .background(Theme.surface)
        .overlay(alignment: .top) {
            Divider()
        }
    }
}

private struct LibraryFilesBrowserSheet: View {
    let store: LibraryFilesStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if let browser = store.info.browser {
                    browserContent(browser)
                }
            }
            .navigationTitle(L10n.tr("library.files.browser.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.close")) { dismiss() }
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                useBar
            }
        }
        .presentationDetents([.medium, .large])
    }

    @ViewBuilder
    private func browserContent(_ browser: LibraryFilesBrowserInfo) -> some View {
        Section {
            if browser.atRoot {
                Text(L10n.tr("library.files.browser.places"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            } else {
                Button(L10n.tr("library.files.browser.up"), systemImage: "chevron.up") {
                    store.browseUp()
                }
                .disabled(browser.loading)
                Text(browser.path)
                    .font(.footnote.monospaced())
                    .textSelection(.enabled)
            }
        }
        if let error = browser.error {
            Section {
                NoticeBanner(text: LibraryText.error(error))
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
        }
        Section {
            if browser.loading, browser.folders.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity)
            }
            ForEach(browser.folders) { folder in
                Button {
                    store.browse(folder.path)
                } label: {
                    Label(folder.name, systemImage: "folder")
                        .foregroundStyle(Theme.text)
                }
                .disabled(browser.loading)
            }
            if !browser.loading, browser.folders.isEmpty, !browser.atRoot, browser.error == nil {
                Text(L10n.tr("library.files.browser.none"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            if browser.more {
                Text(L10n.tr("library.files.browser.more"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }

    private var useBar: some View {
        let browser = store.info.browser
        let atRoot = browser?.atRoot ?? true
        let loading = browser?.loading ?? true
        return HStack(spacing: Theme.Spacing.medium) {
            if let browser, !atRoot {
                Text(LibraryText.plural("library.files.browser.files", browser.files))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            Spacer(minLength: 0)
            Button(L10n.tr("library.files.browser.use")) {
                store.useBrowsed()
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            .disabled(atRoot || loading)
        }
        .padding(Theme.Spacing.medium)
        .background(Theme.surface)
        .overlay(alignment: .top) {
            Divider()
        }
    }
}

private struct LibraryFilesEditorSheet: View {
    let store: LibraryFilesStore
    @State private var term = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if let editor = store.info.editor {
                    searchSection(editor)
                    resultsSection(editor)
                    if editor.titleId > 0 {
                        chosenSection(editor)
                    }
                }
            }
            .navigationTitle(L10n.tr("library.files.editor.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.close")) { dismiss() }
                }
            }
            .onAppear {
                if let editor = store.info.editor {
                    term = editor.query
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func searchSection(_ editor: LibraryFilesEditorInfo) -> some View {
        Section {
            TextField(L10n.tr("library.files.editor.search"), text: $term)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .onSubmit { search() }
            Button {
                search()
            } label: {
                HStack(spacing: Theme.Spacing.small) {
                    Text(L10n.tr("library.files.editor.look.up"))
                    if editor.searching {
                        ProgressView()
                    }
                }
            }
            .disabled(editor.searching || term.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        } header: {
            Text(LibraryImportText.fileName(editor.path))
                .font(.footnote.monospaced())
                .textCase(nil)
        }
    }

    @ViewBuilder
    private func resultsSection(_ editor: LibraryFilesEditorInfo) -> some View {
        if let error = editor.error {
            Section {
                NoticeBanner(text: LibraryText.error(error))
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
        }
        if editor.nothing {
            Section {
                Text(L10n.tr("library.files.editor.nothing"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        if !editor.results.isEmpty {
            Section {
                ForEach(editor.results) { title in
                    resultRow(title, picked: title.id == editor.titleId)
                }
            }
        }
    }

    private func resultRow(_ title: LibraryFilesTitleInfo, picked: Bool) -> some View {
        HStack(spacing: Theme.Spacing.medium) {
            VStack(alignment: .leading, spacing: 0) {
                Text(LibraryImportText.titleYear(title.name, title.year))
                    .font(.subheadline)
                Text(LibraryFilesText.kind(title.kind))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(picked ? L10n.tr("library.files.editor.picked") : L10n.tr("library.files.editor.pick")) {
                store.editorChoose(title.id)
            }
            .buttonStyle(.bordered)
            .disabled(picked)
        }
    }

    @ViewBuilder
    private func chosenSection(_ editor: LibraryFilesEditorInfo) -> some View {
        @Bindable var bindable = store
        Section {
            Text(chosenLine(editor))
                .font(.subheadline.weight(.semibold))
            if editor.loading {
                ProgressView()
                    .frame(maxWidth: .infinity)
            }
            if editor.versions.count > 1 {
                Picker(L10n.tr("library.files.editor.version"), selection: $bindable.versionChoice) {
                    ForEach(editor.versions) { version in
                        Text(version.label).tag(version.id)
                    }
                }
            }
            if editor.titleKind == .show, !editor.seasons.isEmpty {
                Picker(L10n.tr("library.files.editor.season"), selection: $bindable.seasonChoice) {
                    Text(L10n.tr("library.files.editor.pick.one")).tag(-1)
                    ForEach(editor.seasons, id: \.self) { season in
                        Text(LibraryFilesText.season(season)).tag(season)
                    }
                }
                Picker(L10n.tr("library.files.editor.episode"), selection: $bindable.episodeChoice) {
                    Text(L10n.tr("library.files.editor.pick.one")).tag(-1)
                    ForEach(editor.episodes) { episode in
                        Text(LibraryFilesText.episodeTitle(episode)).tag(episode.episode)
                    }
                }
                .disabled(editor.season < 0)
            }
            Button {
                store.editorUse()
            } label: {
                Text(L10n.tr("library.files.editor.use"))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            .disabled(!editor.canUse)
        }
    }

    private func chosenLine(_ editor: LibraryFilesEditorInfo) -> String {
        let name = LibraryImportText.titleYear(editor.titleName, editor.year)
        guard editor.titleKind == .show, editor.season >= 0, editor.episode >= 0 else { return name }
        return [name, LibraryFilesText.episodeCode(editor.season, editor.episode)].joined(separator: " · ")
    }

    private func search() {
        let text = term.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        store.editorSearch(text)
    }
}
