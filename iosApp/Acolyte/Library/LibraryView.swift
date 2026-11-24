import SwiftUI

struct LibraryView: View {
    @State private var store: LibraryStore
    @State private var searchText = ""
    @State private var kindSelection: LibraryKindID = .movie
    @State private var showFilters = false
    @State private var showCopies = false
    @State private var confirmRemove = false
    @State private var confirmMissing = false
    @State private var showAudio = false
    @State private var audioText = ""
    @State private var showUnopenable = false
    @State private var unopenableName = ""
    @State private var gridWidth: CGFloat = 0

    init(kind: LibraryKindID?) {
        _store = State(initialValue: LibraryStore(kind: kind))
    }

    private var info: LibraryInfo { store.info }

    var body: some View {
        screen
            .onChange(of: searchText) { _, text in
                store.search(text)
            }
            .onChange(of: info.kind, initial: true) { _, kind in
                kindSelection = kind
            }
            .onChange(of: kindSelection) { _, kind in
                if kind != info.kind {
                    searchText = ""
                    store.setKind(kind)
                }
            }
            .onChange(of: info.copiesReview != nil) { _, present in
                showCopies = present
            }
            .onChange(of: showCopies) { _, shown in
                if !shown {
                    store.cancelCopies()
                }
            }
            .task { await store.watch() }
            .task { await store.load() }
            .task { await store.followLive() }
    }

    private var screen: some View {
        StatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
            LibraryGridEmpty(info: info)
        } content: {
            content
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            LibraryGridHeader(info: info, selection: $kindSelection)
        }
        .navigationTitle(LibraryText.kind(info.kind))
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $searchText, prompt: searchPrompt)
        .toolbar {
            toolbarContent
        }
        .modifier(dialogs)
        .libraryNotice(info.notice) { store.dismissNotice($0) }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            selectBar
        }
    }

    private var dialogs: LibraryGridDialogs {
        LibraryGridDialogs(
            store: store,
            clear: { clearAll() },
            showFilters: $showFilters,
            showCopies: $showCopies,
            confirmRemove: $confirmRemove,
            confirmMissing: $confirmMissing,
            showAudio: $showAudio,
            audioText: $audioText,
            showUnopenable: $showUnopenable,
            unopenableName: unopenableName
        )
    }

    private var searchPrompt: String {
        if info.kind == .show { return L10n.tr("library.search.show") }
        return L10n.tr("library.search.movie")
    }

    private var showsBlock: Bool {
        info.load.error != nil || info.searchFailed || !info.chips.isEmpty
    }

    private func clearAll() {
        searchText = ""
        store.clearFilters()
    }

    private func dropChip(_ chip: ActiveChipInfo) {
        if chip.filter == .search {
            searchText = ""
        }
        store.removeChip(chip.filter)
    }

    private func explain(_ item: LibraryItemInfo) {
        unopenableName = item.name
        showUnopenable = true
    }

    @ViewBuilder
    private var content: some View {
        if info.layout == .grid {
            posterContent
        } else {
            listContent
        }
    }

    private var block: some View {
        LibraryGridBlock(
            info: info,
            remove: { chip in dropChip(chip) },
            clear: { clearAll() },
            searchSubtitles: { store.searchSubtitles() },
            searchMissing: { confirmMissing = true }
        )
    }

    private var listContent: some View {
        List {
            if showsBlock {
                block
                    .listRowSeparator(.hidden)
            }
            if info.nothingMatches {
                LibraryGridNothing(info: info, clear: { clearAll() })
                    .listRowSeparator(.hidden)
            } else if info.grouped {
                ForEach(info.groups) { group in
                    Section {
                        ForEach(group.items) { item in
                            listRow(item)
                        }
                    } header: {
                        LibraryGridGroupHeader(group: group)
                    }
                }
            } else {
                ForEach(info.items) { item in
                    listRow(item)
                }
            }
        }
        .listStyle(.plain)
        .refreshable { await store.refresh() }
    }

    private func listRow(_ item: LibraryItemInfo) -> some View {
        LibraryGridItemLink(store: store, item: item, explain: { target in explain(target) }) {
            LibraryGridRow(item: item, selecting: info.selecting, selected: info.selected.contains(item.id))
        }
    }

    private var posterContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.large) {
                if showsBlock {
                    block
                }
                if info.nothingMatches {
                    LibraryGridNothing(info: info, clear: { clearAll() })
                } else if info.grouped {
                    ForEach(info.groups) { group in
                        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                            LibraryGridGroupHeader(group: group)
                            posterGrid(group.items)
                        }
                    }
                } else {
                    posterGrid(info.items)
                }
            }
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.medium)
        }
        .onGeometryChange(for: CGFloat.self, of: { proxy in proxy.size.width }, action: { width in gridWidth = width })
        .refreshable { await store.refresh() }
    }

    private var gridMetrics: (count: Int, width: CGFloat) {
        let spacing = Theme.Spacing.medium
        let available = gridWidth - Theme.Spacing.screen * 2
        if available < 1 { return (3, 110) }
        let count = max(2, Int((available + spacing) / (110 + spacing)))
        let width = ((available - spacing * CGFloat(count - 1)) / CGFloat(count)).rounded(.down)
        return (count, width)
    }

    private func posterGrid(_ items: [LibraryItemInfo]) -> some View {
        let metrics = gridMetrics
        let columns = Array(repeating: GridItem(.fixed(metrics.width), spacing: Theme.Spacing.medium), count: metrics.count)
        return LazyVGrid(columns: columns, alignment: .leading, spacing: Theme.Spacing.large) {
            ForEach(items) { item in
                LibraryGridItemLink(store: store, item: item, explain: { target in explain(target) }) {
                    LibraryGridCard(
                        item: item,
                        width: metrics.width,
                        showsState: info.showsPosterState,
                        selecting: info.selecting,
                        selected: info.selected.contains(item.id)
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private var selectBar: some View {
        if info.selecting {
            LibraryGridSelectBar(
                store: store,
                remove: { confirmRemove = true },
                audio: {
                    audioText = ""
                    showAudio = true
                }
            )
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItemGroup(placement: .topBarTrailing) {
            layoutButton
            filterButton
            if info.canManage {
                selectButton
                moreMenu
            }
        }
    }

    private var layoutButton: some View {
        let grid = info.layout == .grid
        return Button {
            store.setLayout(grid ? .list : .grid)
        } label: {
            Label(
                grid ? L10n.tr("library.layout.list") : L10n.tr("library.layout.grid"),
                systemImage: grid ? "list.bullet" : "square.grid.2x2"
            )
            .labelStyle(.iconOnly)
        }
    }

    private var filterButton: some View {
        let count = info.activeFilterCount
        let title = count > 0 ? L10n.format("library.grid.sort.filter.n", count) : L10n.tr("library.sort.filter")
        return Button {
            showFilters = true
        } label: {
            HStack(spacing: 3) {
                Image(systemName: count > 0 ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                if count > 0 {
                    Text(verbatim: String(count))
                        .font(.footnote.weight(.semibold))
                        .monospacedDigit()
                }
            }
        }
        .accessibilityLabel(title)
    }

    private var selectButton: some View {
        let selecting = info.selecting
        return Button {
            if selecting {
                store.stopSelecting()
            } else {
                store.startSelecting()
            }
        } label: {
            Label(
                selecting ? L10n.tr("library.select.stop") : L10n.tr("library.select"),
                systemImage: selecting ? "checkmark.circle.fill" : "checkmark.circle"
            )
            .labelStyle(.iconOnly)
        }
        .disabled(info.items.isEmpty && !selecting)
    }

    private var moreMenu: some View {
        Menu {
            if info.tools.contains(.deleted) {
                NavigationLink(value: Route.libraryDeleted(titleId: 0)) {
                    Label(deletedTitle, systemImage: "trash")
                }
            }
            if !info.tools.isEmpty {
                NavigationLink(value: Route.libraryTools) {
                    Label(L10n.tr("library.tools"), systemImage: "wrench.and.screwdriver")
                }
            }
            ForEach(info.addKinds, id: \.self) { kind in
                addLink(kind)
            }
            if info.tools.contains(.importLibrary) {
                NavigationLink(value: Route.libraryImport) {
                    Label(L10n.tr("library.add.import"), systemImage: "square.and.arrow.down")
                }
            }
        } label: {
            Label(L10n.tr("library.more"), systemImage: "ellipsis.circle")
                .labelStyle(.iconOnly)
        }
    }

    private var deletedTitle: String {
        if info.deletedCount > 0 { return L10n.format("library.deleted.count", info.deletedCount) }
        return L10n.tr("library.deleted")
    }

    @ViewBuilder
    private func addLink(_ kind: LibraryKindID) -> some View {
        switch kind {
        case .movie:
            NavigationLink(value: SectionID.search) {
                Label(L10n.tr("library.add.movie"), systemImage: "plus")
            }
        case .show:
            NavigationLink(value: SectionID.search) {
                Label(L10n.tr("library.add.show"), systemImage: "plus")
            }
        case .artist:
            EmptyView()
        }
    }
}

private struct LibraryGridHeader: View {
    let info: LibraryInfo
    @Binding var selection: LibraryKindID

    private var countText: String {
        let shown = info.items.count
        if shown != info.total { return L10n.format("library.count.of", shown, info.total) }
        switch info.kind {
        case .movie: return LibraryText.plural("library.word.movie", info.total)
        case .show: return LibraryText.plural("library.word.show", info.total)
        case .artist: return String(info.total)
        }
    }

    private var showsLine: Bool {
        info.load.phase == .ready || info.musicEntry || info.tools.contains(.stats)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.kinds.count > 1 {
                Picker(SectionID.library.title, selection: $selection) {
                    ForEach(info.kinds, id: \.self) { kind in
                        Text(LibraryText.kind(kind)).tag(kind)
                    }
                }
                .pickerStyle(.segmented)
            }
            if showsLine {
                HStack(spacing: Theme.Spacing.small) {
                    if info.load.phase == .ready {
                        Text(countText)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 0)
                    if info.musicEntry {
                        NavigationLink(value: SectionID.music) {
                            Label(L10n.tr("library.kind.music"), systemImage: SectionID.music.symbol)
                        }
                    }
                    if info.tools.contains(.stats) {
                        NavigationLink(value: SectionID.stats) {
                            Label(L10n.tr("library.stats"), systemImage: SectionID.stats.symbol)
                        }
                    }
                }
                .font(.footnote)
                .buttonStyle(.bordered)
                .controlSize(.small)
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.small)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.background)
    }
}

private struct LibraryGridDialogs: ViewModifier {
    let store: LibraryStore
    let clear: @MainActor () -> Void
    @Binding var showFilters: Bool
    @Binding var showCopies: Bool
    @Binding var confirmRemove: Bool
    @Binding var confirmMissing: Bool
    @Binding var showAudio: Bool
    @Binding var audioText: String
    @Binding var showUnopenable: Bool
    let unopenableName: String

    func body(content: Content) -> some View {
        content
            .sheet(isPresented: $showFilters) {
                LibraryFilterSheet(store: store, clear: clear)
            }
            .sheet(isPresented: $showCopies) {
                if let review = store.info.copiesReview {
                    LibraryGridCopiesSheet(store: store, review: review)
                }
            }
            .confirmationDialog(
                LibraryText.plural("library.remove.title", store.info.selectedCount),
                isPresented: $confirmRemove,
                titleVisibility: .visible
            ) {
                Button(L10n.tr("library.action.remove"), role: .destructive) {
                    store.removeSelected()
                }
                Button(L10n.tr("library.cancel"), role: .cancel) {}
            } message: {
                Text(L10n.tr("library.remove.text"))
            }
            .confirmationDialog(
                LibraryText.plural("library.search.missing.title", store.info.missingCount),
                isPresented: $confirmMissing,
                titleVisibility: .visible
            ) {
                Button(L10n.tr("library.search")) {
                    store.searchAllMissing()
                }
                Button(L10n.tr("library.cancel"), role: .cancel) {}
            } message: {
                Text(L10n.tr("library.search.missing.note"))
            }
            .alert(LibraryText.plural("library.audio.title", store.info.selectedCount), isPresented: $showAudio) {
                TextField(L10n.tr("library.audio.field"), text: $audioText)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Button(L10n.tr("library.cancel"), role: .cancel) {}
                Button(L10n.tr("library.audio.set")) {
                    store.setAudio(audioText)
                }
            } message: {
                Text(L10n.tr("library.audio.hint"))
            }
            .alert(unopenableName, isPresented: $showUnopenable) {
                Button(L10n.tr("library.close"), role: .cancel) {}
            } message: {
                Text(L10n.tr("library.cannot.open"))
            }
    }
}
