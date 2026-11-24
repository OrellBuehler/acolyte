import SwiftUI
import Observation

@MainActor
@Observable
final class LibraryImportStore {
    private(set) var info: LibraryImportInfo = .initial

    private let driver = SDKClient.shared.makeLibraryImport()

    var selectedFolder: Int64 {
        get { info.folderId }
        set { driver.selectFolder(newValue) }
    }

    var follow: Bool {
        get { info.follow }
        set { driver.setFollow(newValue) }
    }

    var lookupOpen: Bool {
        get { info.lookup != nil }
        set {
            if !newValue { driver.closeLookup() }
        }
    }

    var tidyOpen: Bool {
        get { info.tidy != nil }
        set {
            if !newValue { closeTidy() }
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

    func scan() {
        let driver = self.driver
        Task { await driver.scan() }
    }

    func scanRest() {
        let driver = self.driver
        Task { await driver.scanRest() }
    }

    func setFilter(_ status: String) {
        driver.setFilter(status)
    }

    func setQuery(_ text: String) {
        driver.setQuery(text)
    }

    func toggle(_ folder: String) {
        driver.toggle(folder)
    }

    func selectAll() {
        driver.selectAll()
    }

    func selectNone() {
        driver.selectNone()
    }

    func pick(folder: String, key: String) {
        driver.pick(folder: folder, key: key)
    }

    func openLookup(_ folder: String) {
        driver.openLookup(folder)
    }

    func lookup(_ term: String) {
        let driver = self.driver
        Task { await driver.lookup(term) }
    }

    func useLookup(_ result: LibraryImportLookupResultInfo) {
        driver.useLookup(result)
    }

    func adopt() {
        let driver = self.driver
        Task { await driver.adopt() }
    }

    func keepAsFiles() {
        let driver = self.driver
        Task { await driver.keepAsFiles() }
    }

    func openTidy() {
        let driver = self.driver
        Task { await driver.openTidy() }
    }

    func tidy() {
        let driver = self.driver
        Task { await driver.tidy() }
    }

    func undoTidy() {
        let driver = self.driver
        Task { await driver.undoTidy() }
    }

    func closeTidy() {
        let driver = self.driver
        Task { await driver.closeTidy() }
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

enum LibraryImportText {
    static func titleYear(_ name: String, _ year: Int) -> String {
        year > 0 ? name + " (" + String(year) + ")" : name
    }

    static func folder(_ folder: LibraryImportFolderInfo) -> String {
        let base = folder.label.isEmpty ? folder.path : L10n.format("library.import.folder.label", folder.label, folder.path)
        switch folder.kind {
        case .movie: return base
        case .show: return L10n.format("library.import.folder.show", base)
        case .music: return L10n.format("library.import.folder.music", base)
        }
    }

    static func status(_ status: LibraryImportStatusID, kind: LibraryFolderKindID) -> String? {
        switch status {
        case .matched: L10n.tr("library.import.status.matched")
        case .unsure: L10n.tr("library.import.status.unsure")
        case .unmatched: L10n.tr("library.import.status.unmatched")
        case .notLookedUp: L10n.tr("library.import.status.not.looked.up")
        case .inLibrary: L10n.tr("library.import.status.in.library")
        case .neverAdd: L10n.tr("library.import.status.never.add")
        case .noVideo: kind == .music ? L10n.tr("library.import.status.no.audio") : L10n.tr("library.import.status.no.video")
        case .unknown: nil
        }
    }

    static func tone(_ status: LibraryImportStatusID) -> ToneKind {
        switch status {
        case .matched: .ok
        case .unsure: .wait
        case .unmatched: .fail
        case .inLibrary: .info
        case .notLookedUp, .neverAdd, .noVideo, .unknown: .idle
        }
    }

    static func pill(_ row: LibraryImportRowInfo, kind: LibraryFolderKindID) -> String? {
        guard let label = status(row.status, kind: kind) else { return nil }
        guard row.status == .matched || row.status == .unsure, let pick = row.pick, pick.score >= 0, pick.score < 0.9 else {
            return label
        }
        let percent = Int((pick.score * 100).rounded())
        return L10n.format("library.import.status.score", label, percent)
    }

    static func choice(_ pick: LibraryImportPickInfo) -> String {
        let name = titleYear(pick.name, pick.year)
        guard pick.score >= 0 else { return name }
        let percent = Int((pick.score * 100).rounded())
        return L10n.format("library.import.status.score", name, percent)
    }

    static func fold(_ row: LibraryImportRowInfo, kind: LibraryFolderKindID) -> String? {
        var parts: [String] = []
        switch kind {
        case .show:
            guard row.episodes > 0 || row.files > 0 else { return nil }
            let episodes = LibraryText.plural("library.import.show.episodes", row.episodes)
            let files = LibraryText.plural("library.import.show.in.files", row.files)
            parts.append(episodes + " " + files)
            if row.unknown > 0 {
                parts.append(LibraryText.plural("library.import.show.left.alone", row.unknown))
            }
        case .music:
            guard row.albums > 0 || row.tracks > 0 else { return nil }
            parts.append(LibraryText.plural("library.import.music.albums", row.albums))
            parts.append(LibraryText.plural("library.import.music.tracks", row.tracks))
        case .movie:
            return nil
        }
        return parts.joined(separator: " · ")
    }

    static func planHeader(_ plan: LibraryImportPlanInfo) -> String {
        if plan.total > plan.shown {
            let key = plan.flat ? "library.import.part.files" : "library.import.part.folders"
            return LibraryText.plural(key, plan.total, plan.path, plan.offset + 1, plan.offset + plan.shown)
        }
        let key = plan.flat ? "library.import.in.files" : "library.import.in.folders"
        return LibraryText.plural(key, plan.shown, plan.path)
    }

    static func scanRestAsk(_ plan: LibraryImportPlanInfo, ticked: Int) -> String {
        let key = plan.flat ? "library.import.scan.rest.ask.files" : "library.import.scan.rest.ask.folders"
        return LibraryText.plural(key, ticked)
    }

    static func adopt(_ kind: LibraryFolderKindID, count: Int) -> String {
        switch kind {
        case .movie: LibraryText.plural("library.import.adopt.movie", count)
        case .show: LibraryText.plural("library.import.adopt.show", count)
        case .music: LibraryText.plural("library.import.adopt.artist", count)
        }
    }

    static func done(_ kind: LibraryFolderKindID, count: Int) -> String {
        switch kind {
        case .movie: LibraryText.plural("library.import.done.movie", count)
        case .show: LibraryText.plural("library.import.done.show", count)
        case .music: LibraryText.plural("library.import.done.artist", count)
        }
    }

    static func lookupPlaceholder(_ kind: LibraryFolderKindID) -> String {
        switch kind {
        case .movie: L10n.tr("library.import.lookup.movie")
        case .show: L10n.tr("library.import.lookup.show")
        case .music: L10n.tr("library.import.lookup.music")
        }
    }

    static func lookupIdHint(_ kind: LibraryFolderKindID) -> String {
        switch kind {
        case .movie: "tmdb:<id>"
        case .show: "tvdb:<id>"
        case .music: "mbid:<id>"
        }
    }

    static func fileName(_ path: String) -> String {
        path.split(separator: "/").last.map(String.init) ?? path
    }

    static func tidyWhy(_ move: LibraryTidyMoveInfo) -> String {
        switch move.why {
        case "exists": L10n.format("library.import.tidy.why.exists", move.path)
        case "two": L10n.format("library.import.tidy.why.two", move.path)
        case "has_movie": L10n.format("library.import.tidy.why.has.movie", move.path)
        default: move.problem
        }
    }
}

struct LibraryImportView: View {
    @State private var store = LibraryImportStore()
    @State private var query = ""
    @State private var confirmScanRest = false

    var body: some View {
        @Bindable var model = store
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            LibraryImportEmpty(canOpenFolders: store.info.canOpenFolders)
        } content: {
            importList
        }
        .navigationTitle(L10n.tr("library.import.title"))
        .libraryNotice(store.info.notice) { store.dismissNotice($0) }
        .confirmationDialog(L10n.tr("library.import.scan.rest"), isPresented: $confirmScanRest, titleVisibility: .visible) {
            Button(L10n.tr("library.import.scan.rest")) { store.scanRest() }
        } message: {
            Text(scanRestMessage)
        }
        .sheet(isPresented: $model.lookupOpen) {
            LibraryImportLookupSheet(store: store, kind: currentKind)
        }
        .sheet(isPresented: $model.tidyOpen) {
            LibraryImportTidySheet(store: store)
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var scanRestMessage: String {
        guard let plan = store.info.plan else { return "" }
        return LibraryImportText.scanRestAsk(plan, ticked: store.info.tickedCount)
    }

    private var currentKind: LibraryFolderKindID {
        if let plan = store.info.plan { return plan.kind }
        return store.info.folders.first { $0.id == store.info.folderId }?.kind ?? .movie
    }

    private var importList: some View {
        List {
            pickSection
            if store.info.busy {
                progressSection
            }
            if let error = store.info.error {
                Section {
                    NoticeBanner(text: LibraryText.error(error))
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                }
            }
            if let outcome = store.info.outcome {
                LibraryImportOutcomeSection(store: store, outcome: outcome, kind: currentKind)
            }
            if let plan = store.info.plan {
                planSection(plan)
                rowsSection(plan)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Theme.background)
        .scrollDismissesKeyboard(.interactively)
        .refreshable { await store.refresh() }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if store.info.plan != nil {
                LibraryImportBottomBar(store: store, kind: currentKind)
            }
        }
    }

    private var pickSection: some View {
        @Bindable var model = store
        return Section {
            Label(L10n.tr("library.import.stay"), systemImage: "shield")
                .foregroundStyle(Theme.okText)
            Picker(L10n.tr("library.import.folder"), selection: $model.selectedFolder) {
                ForEach(store.info.folders) { folder in
                    Text(LibraryImportText.folder(folder)).tag(folder.id)
                }
            }
            .pickerStyle(.menu)
            .disabled(store.info.busy)
            Button {
                store.scan()
            } label: {
                HStack {
                    Text(L10n.tr("library.import.scan"))
                    if store.info.scanning {
                        ProgressView()
                    }
                }
            }
            .disabled(store.info.busy || store.info.folderId == 0)
            NavigationLink(value: Route.libraryImportFiles(path: "")) {
                Text(L10n.tr("library.import.by.hand"))
                    .font(.footnote)
            }
        } header: {
            Text(L10n.tr("library.import.step.pick"))
        }
    }

    private var progressSection: some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                if store.info.adopting {
                    adoptProgress
                } else {
                    scanProgress
                }
            }
        }
    }

    @ViewBuilder
    private var adoptProgress: some View {
        let total = max(store.info.adoptTotal, 1)
        ProgressView(value: min(Double(store.info.adoptDone) / Double(total), 1))
        Text(L10n.format("library.import.adopting", store.info.adoptDone, store.info.adoptTotal))
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
        if !store.info.progress.note.isEmpty {
            Text(store.info.progress.note)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }

    @ViewBuilder
    private var scanProgress: some View {
        if store.info.progress.progress > 0 {
            ProgressView(value: min(store.info.progress.progress, 1))
        } else {
            ProgressView()
        }
        Text(scanProgressText)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
    }

    private var scanProgressText: String {
        let info = store.info
        if info.progressDone >= 0 {
            let key = (info.plan?.flat ?? false) ? "library.import.progress.files" : "library.import.progress.folders"
            return L10n.format(key, info.progressDone, max(info.progressTotal, info.progressDone))
        }
        if !info.progress.note.isEmpty {
            return info.progress.note
        }
        return L10n.tr("library.import.progress.reading")
    }

    @ViewBuilder
    private func planSection(_ plan: LibraryImportPlanInfo) -> some View {
        Section {
            Text(LibraryImportText.planHeader(plan))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if !plan.warning.isEmpty {
                NoticeBanner(text: plan.warning)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
            if plan.kind == .movie, plan.looseMovies > 0 || plan.looseTop > 0 || store.info.undoFiles > 0 {
                LibraryImportTidyNotice(store: store, plan: plan)
            }
            if store.info.rows.isEmpty, store.info.query.isEmpty, store.info.statusFilter == "all" {
                Text(L10n.tr("library.import.none"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            if plan.more {
                scanRestBlock(plan)
            }
        } header: {
            Text(L10n.tr("library.import.step.check"))
        }
    }

    private func scanRestBlock(_ plan: LibraryImportPlanInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if plan.partial {
                Text(L10n.format("library.import.stopped", plan.next))
                    .font(.callout)
            }
            if store.info.rows.contains(where: { $0.canTick }) {
                Text(L10n.tr("library.import.part.next"))
                    .font(.callout)
            }
            Button(L10n.tr("library.import.scan.rest")) {
                if store.info.tickedCount > 0 {
                    confirmScanRest = true
                } else {
                    store.scanRest()
                }
            }
            .buttonStyle(.bordered)
            .disabled(store.info.busy)
        }
    }

    @ViewBuilder
    private func rowsSection(_ plan: LibraryImportPlanInfo) -> some View {
        if !store.info.statusOptions.isEmpty {
            Section {
                LibraryImportChips(options: store.info.statusOptions, kind: plan.kind) { store.setFilter($0) }
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                HStack(spacing: Theme.Spacing.small) {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(Theme.textMuted)
                    TextField(L10n.tr("library.search"), text: $query)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .submitLabel(.search)
                }
                .onChange(of: query) { _, text in store.setQuery(text) }
                tickBar
            }
            Section {
                if store.info.rows.isEmpty {
                    Text(L10n.tr("library.import.none.match"))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                } else {
                    ForEach(store.info.rows) { row in
                        LibraryImportRowView(
                            row: row,
                            kind: plan.kind,
                            toggle: { store.toggle(row.folder) },
                            pick: { store.pick(folder: row.folder, key: $0) },
                            lookup: { store.openLookup(row.folder) }
                        )
                    }
                }
            }
        }
    }

    private var tickBar: some View {
        HStack(spacing: Theme.Spacing.small) {
            Text(LibraryText.plural("library.selected", store.info.tickedCount))
                .font(.subheadline.weight(.semibold))
                .monospacedDigit()
            Spacer(minLength: 0)
            if store.info.shownTickable > 0 {
                Button(L10n.tr("library.select.all")) { store.selectAll() }
                    .buttonStyle(.borderless)
            }
            if store.info.tickedCount > 0 {
                Button(L10n.tr("library.select.none")) { store.selectNone() }
                    .buttonStyle(.borderless)
            }
        }
    }
}

private struct LibraryImportEmpty: View {
    let canOpenFolders: Bool

    var body: some View {
        ContentUnavailableView {
            Label(L10n.tr("library.import.empty.title"), systemImage: "folder")
        } description: {
            Text(L10n.tr("library.import.empty.message"))
        } actions: {
            if canOpenFolders {
                NavigationLink(value: Route.settingsLibraryFolders) {
                    Text(L10n.tr("library.empty.folders"))
                }
                .buttonStyle(.borderedProminent)
            }
        }
    }
}

private struct LibraryImportChips: View {
    let options: [LibraryImportStatusOptionInfo]
    let kind: LibraryFolderKindID
    let select: @MainActor (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                ForEach(options) { option in
                    chip(option)
                }
            }
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
        }
    }

    private func chip(_ option: LibraryImportStatusOptionInfo) -> some View {
        Button {
            select(option.id)
        } label: {
            Text(L10n.format("library.copy.line", title(option), String(option.count)))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(option.selected ? Theme.onAccent : Theme.text)
                .padding(.horizontal, Theme.Spacing.medium)
                .padding(.vertical, Theme.Spacing.small)
                .background(option.selected ? Theme.accentFill : Theme.surfaceRaised, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(option.selected ? .isSelected : [])
    }

    private func title(_ option: LibraryImportStatusOptionInfo) -> String {
        guard let status = option.status else { return L10n.tr("library.filter.all") }
        return LibraryImportText.status(status, kind: kind) ?? option.id
    }
}

private struct LibraryImportRowView: View {
    let row: LibraryImportRowInfo
    let kind: LibraryFolderKindID
    let toggle: @MainActor () -> Void
    let pick: @MainActor (String) -> Void
    let lookup: @MainActor () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            tickButton
            LibraryPosterThumb(source: row.pick?.poster, name: row.shownName)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(LibraryImportText.titleYear(row.shownName, row.shownYear))
                    .font(.headline)
                Text(row.folder)
                    .font(.footnote.monospaced())
                    .foregroundStyle(Theme.textMuted)
                    .textSelection(.enabled)
                statusLine
                if let fold = LibraryImportText.fold(row, kind: kind) {
                    Text(fold)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                if row.canTick {
                    changeMenu
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
        .disabled(!row.canTick || row.pick == nil)
        .accessibilityLabel(L10n.format("library.import.adopt.folder", row.folder))
        .accessibilityAddTraits(row.ticked ? .isSelected : [])
    }

    @ViewBuilder
    private var statusLine: some View {
        if let text = LibraryImportText.pill(row, kind: kind) {
            StatusChip(text: text, tone: LibraryImportText.tone(row.status))
        }
        if !row.why.isEmpty, row.status != .matched {
            Text(row.why)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private var changeMenu: some View {
        Menu {
            ForEach(row.choices) { choice in
                Button {
                    pick(choice.key)
                } label: {
                    if choice.key == row.pick?.key {
                        Label(LibraryImportText.choice(choice), systemImage: "checkmark")
                    } else {
                        Text(LibraryImportText.choice(choice))
                    }
                }
            }
            Button(L10n.tr("library.import.look.up"), systemImage: "magnifyingglass") {
                lookup()
            }
        } label: {
            Text(L10n.tr("library.import.change"))
                .font(.subheadline.weight(.semibold))
        }
    }
}

private struct LibraryImportTidyNotice: View {
    let store: LibraryImportStore
    let plan: LibraryImportPlanInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if plan.looseTop > 0 {
                Text(flatNote + " " + L10n.tr("library.import.tidy.optional"))
                    .font(.callout)
            }
            if plan.looseMovies > 0 {
                Text(L10n.format("library.import.tidy.loose", plan.looseMovies))
                    .font(.callout)
            }
            if store.info.undoFiles > 0 {
                Text(L10n.format("library.import.tidy.undo.note", store.info.undoFiles))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if plan.looseTop > 0 {
                Button(L10n.tr("library.import.tidy.keep.flat")) { store.keepAsFiles() }
                    .buttonStyle(.borderedProminent)
                    .disabled(store.info.busy)
            }
            if plan.looseMovies > 0 || plan.looseTop > 0 {
                Button(L10n.tr("library.import.tidy.open")) { store.openTidy() }
                    .buttonStyle(.bordered)
                    .disabled(store.info.busy)
            }
            if store.info.undoFiles > 0 {
                Button(L10n.tr("library.import.tidy.undo")) { store.undoTidy() }
                    .buttonStyle(.bordered)
                    .disabled(store.info.busy)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var flatNote: String {
        plan.suggestFlat ? L10n.tr("library.import.tidy.suggest.flat") : L10n.format("library.import.tidy.loose.top", plan.looseTop)
    }
}

private struct LibraryImportOutcomeSection: View {
    let store: LibraryImportStore
    let outcome: LibraryImportOutcomeInfo
    let kind: LibraryFolderKindID

    var body: some View {
        Section {
            Text(LibraryImportText.done(kind, count: outcome.adopted))
                .font(.callout)
            if outcome.failed > 0 {
                Text(L10n.format("library.import.not.count", outcome.failed))
                    .font(.callout)
                    .foregroundStyle(Theme.failText)
            }
            if outcome.unknown > 0 {
                Text(L10n.format("library.import.unknown.count", outcome.unknown))
                    .font(.callout)
            }
            if needsAnotherScan {
                Text(L10n.tr("library.import.left.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(problems) { folder in
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(folder.folder)
                        .font(.footnote.monospaced())
                    Text(reason(folder))
                        .font(.footnote)
                        .foregroundStyle(Theme.failText)
                }
            }
            if needsAnotherScan {
                Button(L10n.tr("library.import.scan.again")) { store.scan() }
                    .buttonStyle(.borderedProminent)
                    .disabled(store.info.busy)
            }
            if store.info.plan?.more == true {
                Button(L10n.tr("library.import.scan.rest")) { store.scanRest() }
                    .buttonStyle(.bordered)
                    .disabled(store.info.busy)
            }
            NavigationLink(value: SectionID.library) {
                Text(L10n.tr("library.import.go.library"))
            }
        } header: {
            Text(L10n.tr("library.import.done"))
        }
    }

    private var needsAnotherScan: Bool {
        outcome.failed > 0 || outcome.unknown > 0
    }

    private var problems: [LibraryImportAdoptedInfo] {
        outcome.folders.filter { !$0.error.isEmpty }
    }

    private func reason(_ folder: LibraryImportAdoptedInfo) -> String {
        folder.titleId > 0 ? folder.error : L10n.format("library.import.not.adopted.why", folder.error)
    }
}

private struct LibraryImportBottomBar: View {
    let store: LibraryImportStore
    let kind: LibraryFolderKindID

    var body: some View {
        @Bindable var model = store
        VStack(spacing: Theme.Spacing.small) {
            Toggle(isOn: $model.follow) {
                Text(L10n.tr("library.import.follow"))
                    .font(.subheadline)
            }
            .disabled(store.info.adopting)
            HStack(spacing: Theme.Spacing.medium) {
                Text(L10n.format("library.import.ticked", store.info.tickedCount))
                    .font(.subheadline)
                    .foregroundStyle(Theme.textMuted)
                    .monospacedDigit()
                Spacer(minLength: 0)
                Button {
                    store.adopt()
                } label: {
                    HStack(spacing: Theme.Spacing.small) {
                        if store.info.adopting {
                            ProgressView()
                        }
                        Text(LibraryImportText.adopt(kind, count: store.info.tickedCount))
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .disabled(store.info.tickedCount == 0 || store.info.busy)
            }
        }
        .padding(Theme.Spacing.medium)
        .background(Theme.surface)
        .overlay(alignment: .top) {
            Divider()
        }
    }
}

private struct LibraryImportLookupSheet: View {
    let store: LibraryImportStore
    let kind: LibraryFolderKindID
    @State private var term = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if let lookup = store.info.lookup {
                    searchSection(lookup)
                    resultsSection(lookup)
                }
            }
            .navigationTitle(L10n.format("library.import.lookup.title", LibraryImportText.fileName(store.info.lookup?.folder ?? "")))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.close")) { dismiss() }
                }
            }
            .onAppear {
                if let lookup = store.info.lookup {
                    term = lookup.term
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func searchSection(_ lookup: LibraryImportLookupInfo) -> some View {
        Section {
            TextField(LibraryImportText.lookupPlaceholder(kind), text: $term)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .submitLabel(.search)
                .onSubmit { search() }
            Button {
                search()
            } label: {
                HStack {
                    Text(L10n.tr("library.import.lookup.button"))
                    if lookup.busy {
                        ProgressView()
                    }
                }
            }
            .disabled(lookup.busy || term.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        } header: {
            Text(lookup.folder)
                .font(.footnote.monospaced())
                .textCase(nil)
        }
    }

    @ViewBuilder
    private func resultsSection(_ lookup: LibraryImportLookupInfo) -> some View {
        if let error = lookup.error {
            Section {
                NoticeBanner(text: LibraryText.error(error))
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
        }
        if lookup.nothing {
            Section {
                Text(L10n.format("library.import.lookup.nothing", LibraryImportText.lookupIdHint(kind)))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        if !lookup.results.isEmpty {
            Section {
                ForEach(lookup.results) { result in
                    resultRow(result)
                }
            }
        }
    }

    private func resultRow(_ result: LibraryImportLookupResultInfo) -> some View {
        HStack(spacing: Theme.Spacing.medium) {
            LibraryPosterThumb(source: result.poster, name: result.name)
            Text(LibraryImportText.titleYear(result.name, result.year))
                .font(.subheadline)
                .frame(maxWidth: .infinity, alignment: .leading)
            if result.inLibrary {
                Text(L10n.tr("library.import.status.in.library"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.trailing)
            } else {
                Button(L10n.tr("library.import.lookup.use")) { store.useLookup(result) }
                    .buttonStyle(.bordered)
            }
        }
    }

    private func search() {
        let text = term.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        store.lookup(text)
    }
}

private struct LibraryImportTidySheet: View {
    let store: LibraryImportStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if let tidy = store.info.tidy {
                    tidyContent(tidy)
                }
            }
            .navigationTitle(L10n.tr("library.import.tidy.open"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.cancel")) { dismiss() }
                }
            }
            .libraryNotice(store.info.notice) { store.dismissNotice($0) }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                moveBar
            }
        }
        .presentationDetents([.large])
    }

    @ViewBuilder
    private func tidyContent(_ tidy: LibraryImportTidyInfo) -> some View {
        if tidy.changed {
            Section {
                NoticeBanner(text: L10n.tr("library.import.tidy.changed"))
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
        }
        if let result = tidy.result {
            resultSection(result)
        } else {
            planSections(tidy)
        }
    }

    @ViewBuilder
    private func planSections(_ tidy: LibraryImportTidyInfo) -> some View {
        Section {
            Text(L10n.tr("library.import.tidy.intro"))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
            if tidy.busy, tidy.moves.isEmpty, tidy.blocked.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity)
            }
        }
        if !tidy.moves.isEmpty {
            Section {
                ForEach(tidy.moves) { move in
                    moveRow(move)
                }
            } header: {
                Text(L10n.format("library.import.tidy.ready", tidy.moves.count))
            }
        } else if !tidy.busy {
            Section {
                Text(L10n.tr("library.import.tidy.nothing"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        if !tidy.blocked.isEmpty {
            Section {
                ForEach(tidy.blocked) { move in
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(move.from)
                            .font(.footnote.monospaced())
                        Text(LibraryImportText.tidyWhy(move))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            } header: {
                Text(L10n.format("library.import.tidy.blocked", tidy.blocked.count))
            }
        }
        if tidy.unmatched > 0 {
            Section {
                Text(L10n.format("library.import.tidy.unmatched", tidy.unmatched))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }

    private func moveRow(_ move: LibraryTidyMoveInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            pathLine(L10n.tr("library.import.tidy.now"), move.from, muted: true)
            pathLine(L10n.tr("library.import.tidy.new"), move.to, muted: false)
            if !move.files.isEmpty {
                let names = move.files.map { LibraryImportText.fileName($0.from) }.joined(separator: ", ")
                Text(L10n.format("library.import.tidy.also", names))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }

    private func pathLine(_ label: String, _ path: String, muted: Bool) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Theme.textMuted)
            Text(path)
                .font(.footnote.monospaced())
                .foregroundStyle(muted ? Theme.textMuted : Theme.text)
        }
    }

    @ViewBuilder
    private func resultSection(_ result: LibraryTidyResultInfo) -> some View {
        Section {
            Text(L10n.format("library.import.tidy.moved", result.moved))
                .font(.callout)
        }
        if !result.failed.isEmpty {
            Section {
                ForEach(result.failed) { failure in
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(failure.from)
                            .font(.footnote.monospaced())
                        Text(failure.why)
                            .font(.footnote)
                            .foregroundStyle(Theme.failText)
                    }
                }
            } header: {
                Text(L10n.format("library.import.tidy.failed", result.failed.count))
            }
        }
    }

    private var moveBar: some View {
        let tidy = store.info.tidy
        let busy = tidy?.busy ?? false
        let ready = !(tidy?.moves.isEmpty ?? true) && tidy?.result == nil
        return Button {
            store.tidy()
        } label: {
            HStack(spacing: Theme.Spacing.small) {
                if busy {
                    ProgressView()
                }
                Text(L10n.tr("library.import.tidy.move"))
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .tint(Theme.accent)
        .controlSize(.large)
        .disabled(busy || !ready)
        .padding(Theme.Spacing.medium)
        .background(Theme.surface)
    }
}
