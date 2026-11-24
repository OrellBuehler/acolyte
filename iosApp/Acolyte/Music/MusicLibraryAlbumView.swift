import SwiftUI

struct MusicLibraryAlbumView: View {
    let titleId: Int64
    let albumId: Int64
    @State private var store: MusicLibraryAlbumStore
    @State private var sheet: MusicLibraryAlbumSheet?
    @State private var deleted = false
    @State private var pending: MusicPressingInfo?
    @State private var confirming = false
    @Environment(\.openRoute) private var openRoute
    @Environment(\.dismiss) private var dismiss

    init(titleId: Int64, albumId: Int64) {
        self.titleId = titleId
        self.albumId = albumId
        _store = State(initialValue: MusicLibraryAlbumStore(titleId: titleId, albumId: albumId))
    }

    private var info: MusicLibraryAlbumInfo { store.info }

    private var ownerId: Int64 { info.titleId > 0 ? info.titleId : titleId }

    private var actions: MusicLibraryAlbumActions {
        MusicLibraryAlbumActions(
            search: { store.searchNow() },
            toggleKeep: { store.toggleKeep() },
            rename: { sheet = .rename(store.makeRename()) },
            delete: { sheet = .remove(store.makeRemove()) },
            report: { openRoute(.reportProblem(titleId: ownerId, name: info.artistName, albumId: albumId)) }
        )
    }

    var body: some View {
        content
            .navigationTitle(info.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if !info.notFound {
                    ToolbarItem(placement: .primaryAction) {
                        MusicLibraryAlbumMenu(info: info, actions: actions)
                    }
                }
            }
            .sheet(item: $sheet, onDismiss: afterSheet) { current in
                MusicLibraryAlbumSheetView(sheet: current) { deleted = true }
            }
            .confirmationDialog(
                L10n.tr("music.album.lib.follow.edition"),
                isPresented: $confirming,
                titleVisibility: .visible,
                presenting: pending
            ) { pressing in
                Button(L10n.tr("music.album.lib.use.edition")) { store.changePressing(pressing.id) }
                Button(L10n.tr("common.cancel"), role: .cancel) {}
            } message: { _ in
                Text(L10n.tr("music.album.lib.follow.edition.body"))
            }
            .musicNote(info.note)
            .task { await store.watch() }
            .task { await store.load() }
    }

    @ViewBuilder
    private var content: some View {
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            if info.notFound { notFound } else { page }
        }
    }

    private var notFound: some View {
        VStack(spacing: Theme.Spacing.medium) {
            EmptyStateView(
                title: L10n.tr("music.album.lib.not.found"),
                message: L10n.tr("music.album.lib.not.found.hint"),
                systemImage: "opticaldisc"
            )
            Button(L10n.tr("music.album.lib.back.to.artist")) { dismiss() }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accentFill)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var page: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                MusicLibraryAlbumHero(info: info)
                if let error = info.load.error {
                    NoticeBanner(text: error.localized)
                        .padding(.horizontal, Theme.Spacing.screen)
                        .padding(.vertical, Theme.Spacing.small)
                }
                SectionHeader(title: L10n.tr("music.album.tracks"))
                    .padding(.horizontal, Theme.Spacing.screen)
                    .padding(.top, Theme.Spacing.medium)
                tracks
                if info.canManage {
                    MusicLibraryAlbumCard(info: info, actions: actions) { pick($0) }
                }
                if !info.moreBy.isEmpty { MusicLibraryAlbumMore(info: info) }
            }
        }
        .refreshable { await store.refresh() }
    }

    @ViewBuilder
    private var tracks: some View {
        if info.tracks.isEmpty { emptyTracks }
        ForEach(info.tracks) { track in
            MusicLibraryAlbumTrackRow(track: track, staff: info.canManage)
        }
    }

    @ViewBuilder
    private var emptyTracks: some View {
        if !info.included {
            hint(L10n.tr("music.album.lib.left.out"))
        } else if info.tracksWaiting {
            ForEach(0..<6, id: \.self) { _ in MusicAlbumSkeletonRow() }
        } else {
            hint(L10n.tr("music.album.no.songs"))
        }
    }

    private func hint(_ text: String) -> some View {
        Text(text)
            .font(.callout)
            .foregroundStyle(Theme.textMuted)
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.medium)
    }

    private func pick(_ pressing: MusicPressingInfo) {
        pending = pressing
        confirming = true
    }

    private func afterSheet() {
        guard deleted else { return }
        deleted = false
        store.removed()
        dismiss()
    }
}
