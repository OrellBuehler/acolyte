import SwiftUI

struct MusicAlbumReloadModifier: ViewModifier {
    let ticket: Int
    let reload: @MainActor @Sendable () async -> Void

    func body(content: Content) -> some View {
        content.task(id: ticket) {
            guard ticket > 0 else { return }
            for _ in 0..<6 {
                try? await Task.sleep(for: .seconds(5))
                if Task.isCancelled { return }
                await reload()
            }
        }
    }
}

struct MusicAlbumView: View {
    let mbid: String
    let artist: String
    @State private var store: MusicAlbumStore
    @State private var toasts = DlToastModel()
    @State private var ticket = 0
    @Environment(\.openRoute) private var openRoute
    @Environment(\.scenePhase) private var scenePhase
    @Environment(Navigator.self) private var navigator

    init(mbid: String, artist: String) {
        self.mbid = mbid
        self.artist = artist
        _store = State(initialValue: MusicAlbumStore(mbid: mbid, artist: artist))
    }

    private var info: MusicAlbumPageInfo { store.info }

    private var serverURL: String {
        navigator.account.serverURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    var body: some View {
        content
            .navigationTitle(info.name)
            .navigationBarTitleDisplayMode(.inline)
            .musicNote(info.note)
            .overlay(alignment: .bottom) { DlToastHost(model: toasts).padding(.bottom, Theme.Spacing.large) }
            .modifier(MusicAlbumReloadModifier(ticket: ticket, reload: reloadAction()))
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { Task { await store.reloadQuiet() } }
            }
            .task { await store.watch() }
            .task { await store.load() }
    }

    @ViewBuilder
    private var content: some View {
        if info.problem != .none {
            MusicProblemView(problem: info.problem, album: true) { Task { await store.refresh() } }
        } else {
            StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                EmptyView()
            } content: {
                page
            }
        }
    }

    private var page: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                MusicAlbumHero(info: info, store: store, onPrimary: primary)
                if let error = info.load.error {
                    NoticeBanner(text: error.localized)
                        .padding(.horizontal, Theme.Spacing.screen)
                        .padding(.vertical, Theme.Spacing.small)
                }
                if !info.gap.isEmpty { gapNotice }
                tracksHeader
                tracks
                MusicAlbumFooter(info: info, store: store)
                MusicAlbumRails(info: info)
            }
        }
        .refreshable { await store.refresh() }
    }

    private var gapNotice: some View {
        HStack(spacing: Theme.Spacing.medium) {
            ProgressView().controlSize(.small)
            Text(info.gap == "gap" ? L10n.tr("music.album.gap") : info.gap)
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.small)
    }

    private var tracksHeader: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            SectionHeader(title: L10n.tr("music.album.tracks"))
            if info.showCounts {
                Text(L10n.format("music.album.counts", info.have, info.missing))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.top, Theme.Spacing.medium)
    }

    @ViewBuilder
    private var tracks: some View {
        if info.tracks.isEmpty { emptyTracks }
        ForEach(info.tracks.indices, id: \.self) { index in
            MusicAlbumTrackRow(row: info.tracks[index], info: info, showDisc: startsDisc(index), serverURL: serverURL) {
                toasts.show($0, tone: .ok)
            }
        }
    }

    @ViewBuilder
    private var emptyTracks: some View {
        if !info.gap.isEmpty {
            EmptyView()
        } else if info.tracksWaiting {
            ForEach(0..<6, id: \.self) { _ in MusicAlbumSkeletonRow() }
        } else {
            Text(L10n.tr("music.album.no.songs"))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .padding(.horizontal, Theme.Spacing.screen)
                .padding(.vertical, Theme.Spacing.medium)
        }
    }

    private func startsDisc(_ index: Int) -> Bool {
        guard info.discs > 1 else { return false }
        return index == 0 || info.tracks[index - 1].disc != info.tracks[index].disc
    }

    private func primary() {
        guard info.ctaReady else {
            store.stillReading()
            return
        }
        switch info.cta {
        case .openLibrary, .manageLibrary:
            openRoute(info.libraryRoute)
        case .add:
            openSheet(.musicAdd(mbid: info.artistMbid, name: info.artistName))
        case .ask:
            ask()
        case .openArtist:
            openRoute(.musicArtist(mbid: info.artistMbid))
        case .none, .asked:
            break
        }
    }

    private func ask() {
        if info.edition.isEmpty {
            openSheet(.askMusic(artist: info.artistMbid, name: info.artistName, album: info.mbid, edition: ""))
        } else {
            store.askWithEdition()
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
