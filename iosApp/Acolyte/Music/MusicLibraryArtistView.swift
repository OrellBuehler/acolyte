import SwiftUI

enum MusicLibraryTabKind: Hashable {
    case overview
    case library
}

struct MusicLibraryArtistView: View {
    let titleId: Int64
    let tab: String
    let account: AccountInfo
    @State private var store: MusicLibraryArtistStore
    @State private var selection: MusicLibraryTabKind
    @State private var sheet: MusicLibrarySheet?
    @State private var leaving = false
    @Environment(\.dismiss) private var dismiss

    init(titleId: Int64, tab: String, account: AccountInfo) {
        self.titleId = titleId
        self.tab = tab
        self.account = account
        _store = State(initialValue: MusicLibraryArtistStore(titleId: titleId))
        _selection = State(initialValue: tab == "library" ? .library : .overview)
    }

    private var info: MusicLibraryArtistInfo { store.info }

    var body: some View {
        content
            .navigationTitle(info.name)
            .navigationBarTitleDisplayMode(.inline)
            .musicNote(info.note)
            .sheet(item: $sheet, onDismiss: sheetClosed) { item in
                MusicLibrarySheetView(sheet: item, store: store) { leaving = true }
            }
            .task { await store.watch() }
            .task { await store.load() }
            .task { await store.followLive() }
    }

    @ViewBuilder
    private var content: some View {
        switch info.load.phase {
        case .loading:
            LoadingStateView()
        case .failed where info.name.isEmpty:
            ErrorStateView(error: info.load.error ?? .unknown) { Task { await store.refresh() } }
        default:
            page
        }
    }

    private var page: some View {
        VStack(spacing: 0) {
            if info.canManage { tabPicker }
            if info.canManage && selection == .library {
                MusicLibraryTabContent(info: info, store: store, sheet: $sheet)
            } else {
                overview
            }
        }
    }

    private var tabPicker: some View {
        Picker(L10n.tr("music.artist.tab.overview"), selection: $selection) {
            Text(L10n.tr("music.artist.tab.overview")).tag(MusicLibraryTabKind.overview)
            Text(L10n.tr("music.artist.tab.library")).tag(MusicLibraryTabKind.library)
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.small)
    }

    @ViewBuilder
    private var overview: some View {
        if info.mbid.isEmpty {
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.large) {
                    Text(info.name)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(Theme.text)
                    MusicArtistEmbedInfo(library: info)
                    MusicLibraryAsksBlock(asks: info.asks)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(Theme.Spacing.screen)
            }
            .refreshable { await store.refresh() }
        } else {
            MusicLibraryOverview(library: info, libraryStore: store) { selection = .library }
                .id(info.mbid)
        }
    }

    private func sheetClosed() {
        if leaving {
            store.removed()
            dismiss()
            return
        }
        Task { await store.reloadQuiet() }
    }
}

struct MusicLibraryOverview: View {
    let library: MusicLibraryArtistInfo
    let libraryStore: MusicLibraryArtistStore
    let openLibrary: () -> Void
    @State private var store: MusicArtistStore
    @State private var about = false
    @State private var ticket = 0
    @Environment(\.openRoute) private var openRoute
    @Environment(\.scenePhase) private var scenePhase

    init(library: MusicLibraryArtistInfo, libraryStore: MusicLibraryArtistStore, openLibrary: @escaping () -> Void) {
        self.library = library
        self.libraryStore = libraryStore
        self.openLibrary = openLibrary
        _store = State(initialValue: MusicArtistStore(mbid: library.mbid))
    }

    private var info: MusicArtistInfo { store.info }

    var body: some View {
        content
            .sheet(isPresented: $about) { MusicArtistAboutSheet(info: info) }
            .musicNote(info.note)
            .musicReload(after: ticket, reloadAction())
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { Task { await store.reloadQuiet() } }
            }
            .task { await store.watch() }
            .task { await store.load() }
    }

    @ViewBuilder
    private var content: some View {
        switch info.load.phase {
        case .loading:
            LoadingStateView()
        case .failed where info.name.isEmpty:
            MusicProblemView(problem: info.problem, album: false) { Task { await store.refresh() } }
        default:
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.xLarge) {
                    MusicArtistContent(
                        info: info,
                        store: store,
                        embed: MusicArtistEmbed(library: library, openLibrary: openLibrary),
                        onAbout: { about = true },
                        openSheet: openSheet
                    )
                    MusicLibraryAsksBlock(asks: library.asks)
                }
            }
            .refreshable { await refreshBoth() }
        }
    }

    private func refreshBoth() async {
        await libraryStore.refresh()
        await store.refresh()
    }

    private func openSheet(_ route: Route) {
        ticket += 1
        openRoute(route)
    }

    private func reloadAction() -> @MainActor @Sendable () async -> Void {
        let store = store
        let libraryStore = libraryStore
        return {
            await store.reloadQuiet()
            await libraryStore.reloadQuiet()
        }
    }
}
