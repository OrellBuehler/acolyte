import SwiftUI

struct MusicArtistView: View {
    let mbid: String
    @State private var store: MusicArtistStore
    @State private var about = false
    @State private var ticket = 0
    @Environment(\.openRoute) private var openRoute
    @Environment(\.scenePhase) private var scenePhase

    init(mbid: String) {
        self.mbid = mbid
        _store = State(initialValue: MusicArtistStore(mbid: mbid))
    }

    private var info: MusicArtistInfo { store.info }

    var body: some View {
        content
            .navigationTitle(info.name)
            .navigationBarTitleDisplayMode(.inline)
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
                MusicArtistContent(info: info, store: store, onAbout: { about = true }, openSheet: openSheet)
            }
            .refreshable { await store.refresh() }
        }
    }

    private func openSheet(_ route: Route) {
        ticket += 1
        openRoute(route)
    }

    private func reloadAction() -> @MainActor @Sendable () async -> Void {
        let store = store
        return { await store.reloadQuiet() }
    }
}
