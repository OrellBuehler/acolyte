import SwiftUI

struct MusicLibraryReleases: View {
    let info: MusicLibraryArtistInfo
    let store: MusicLibraryArtistStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            header
            if info.leftOut > 0 { filter }
            if info.shown.isEmpty { empty } else { groups }
        }
    }

    private var header: some View {
        HStack {
            Text(L10n.tr("music.lib.releases"))
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: Theme.Spacing.small)
            if !info.shown.isEmpty {
                Button(info.expanded.isEmpty ? L10n.tr("music.lib.expand.all") : L10n.tr("music.lib.collapse.all")) {
                    if info.expanded.isEmpty {
                        store.expandAll()
                    } else {
                        store.collapseAll()
                    }
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.accentText)
            }
        }
    }

    private var filter: some View {
        Picker(L10n.tr("music.lib.releases"), selection: Binding(get: { info.filter }, set: { store.setFilter($0) })) {
            Text(L10n.tr("music.lib.filter.included")).tag(MusicLibFilterKind.included)
            Text(L10n.format("music.lib.filter.left.out", info.leftOut)).tag(MusicLibFilterKind.leftOut)
            Text(L10n.format("music.lib.filter.all", info.albums.count)).tag(MusicLibFilterKind.all)
        }
        .pickerStyle(.segmented)
    }

    private var empty: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(L10n.tr("music.lib.empty"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            if info.includedCount == 0 {
                Text(L10n.tr("music.lib.empty.none"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var groups: some View {
        ForEach(MusicLibGroupID.allCases, id: \.self) { group in
            let albums = info.shown.filter { $0.group == group }
            if !albums.isEmpty {
                MusicLibraryReleaseGroup(group: group, albums: albums, info: info, store: store)
            }
        }
    }
}

struct MusicLibraryReleaseGroup: View {
    let group: MusicLibGroupID
    let albums: [MusicLibAlbumInfo]
    let info: MusicLibraryArtistInfo
    let store: MusicLibraryArtistStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text("\(MusicLibraryArtistText.group(group)) · \(albums.count)")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.textMuted)
            VStack(spacing: 0) {
                ForEach(albums) { album in
                    MusicLibraryReleaseRow(album: album, info: info, store: store)
                    if album.id != albums.last?.id { Divider() }
                }
            }
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        }
    }
}

struct MusicLibraryReleaseRow: View {
    let album: MusicLibAlbumInfo
    let info: MusicLibraryArtistInfo
    let store: MusicLibraryArtistStore
    @Environment(\.openRoute) private var openRoute

    private var open: Bool { info.expanded.contains(album.id) }

    private var meta: String {
        var parts: [String] = []
        if !album.year.isEmpty { parts.append(album.year) }
        if album.tracks > 0 { parts.append(L10n.plural("music.lib.row.tracks", album.tracks)) }
        parts.append(MusicLibraryArtistText.state(album.state))
        if album.dropped { parts.append(L10n.tr("music.lib.dropped")) }
        return parts.joined(separator: " · ")
    }

    var body: some View {
        VStack(spacing: 0) {
            summary
            if open { details }
        }
    }

    private var summary: some View {
        HStack(spacing: Theme.Spacing.medium) {
            if info.canManage { keepButton }
            MusicArt(path: album.cover, name: album.name, size: 250)
                .frame(width: 46, height: 46)
            VStack(alignment: .leading, spacing: 2) {
                Text(album.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                Text(meta)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(2)
            }
            Spacer(minLength: 4)
            if album.tracks > 0 { countPill }
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Theme.textFaint)
                .rotationEffect(.degrees(open ? 90 : 0))
        }
        .padding(Theme.Spacing.medium)
        .contentShape(Rectangle())
        .onTapGesture { store.expand(album.id) }
        .accessibilityAddTraits(.isButton)
        .accessibilityLabel(album.name)
        .accessibilityHint(open ? L10n.tr("music.lib.collapse") : L10n.tr("music.lib.expand"))
    }

    private var keepLabel: String {
        if !album.included { return L10n.tr("music.lib.keep.disabled") }
        return album.keep ? L10n.tr("music.lib.keep.on") : L10n.tr("music.lib.keep.off")
    }

    private var keepButton: some View {
        Button { store.toggleKeepAlbum(album.id) } label: {
            Image(systemName: album.keep ? "checkmark.circle.fill" : "circle")
                .font(.title3)
                .foregroundStyle(album.keep ? Theme.accent : Theme.textFaint)
                .frame(width: 32, height: 32)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!album.included || info.busy.contains(album.id))
        .accessibilityLabel(keepLabel)
    }

    private var countPill: some View {
        let tone: ToneKind = album.inLibrary >= album.tracks ? .ok : (album.inLibrary > 0 ? .wait : .idle)
        return Text("\(album.inLibrary) / \(album.tracks)")
            .font(.caption.weight(.semibold).monospacedDigit())
            .foregroundStyle(Theme.textColor(tone))
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Theme.surfaceHigh, in: Capsule())
    }

    private var details: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.loadingTracks.contains(album.id) {
                ProgressView()
                    .frame(maxWidth: .infinity)
            }
            ForEach(info.tracks(of: album.id)) { track in
                MusicLibraryTrackRow(track: track, album: album, info: info, store: store)
            }
            HStack(spacing: Theme.Spacing.medium) {
                if info.canManage { searchButton }
                Button(L10n.tr("music.lib.open.album")) {
                    openRoute(.musicLibraryAlbum(titleId: info.titleId, albumId: album.id))
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.accentText)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Theme.Spacing.medium)
        .padding(.bottom, Theme.Spacing.medium)
    }

    private var searchButton: some View {
        Button { store.searchAlbum(album.id) } label: {
            Label(L10n.tr("music.lib.search.album"), systemImage: "magnifyingglass")
        }
        .buttonStyle(.bordered)
        .controlSize(.small)
        .disabled(album.missing == 0 || info.working)
    }
}

struct MusicLibraryTrackRow: View {
    let track: MusicLibTrackInfo
    let album: MusicLibAlbumInfo
    let info: MusicLibraryArtistInfo
    let store: MusicLibraryArtistStore

    private var title: String {
        track.name.isEmpty ? L10n.format("music.lib.track.n", track.number) : track.name
    }

    var body: some View {
        HStack(spacing: Theme.Spacing.small) {
            Text(MusicLibraryArtistText.trackNumber(track))
                .font(.footnote.monospacedDigit())
                .foregroundStyle(Theme.textMuted)
                .frame(width: 36, alignment: .trailing)
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.text)
                .lineLimit(2)
            Spacer(minLength: 4)
            if !track.length.isEmpty {
                Text(track.length)
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(Theme.textMuted)
            }
            if !track.stateLabel.isEmpty {
                StatusChip(text: track.stateLabel, tone: track.tone)
            }
            if info.canManage && track.slotId > 0 { searchButton }
        }
    }

    private var searchButton: some View {
        Button { store.searchTrack(albumId: album.id, slotId: track.slotId, name: title) } label: {
            Image(systemName: "magnifyingglass")
                .font(.footnote)
                .foregroundStyle(Theme.text)
                .frame(width: 30, height: 30)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(info.working)
        .accessibilityLabel(L10n.tr("music.lib.search.track"))
    }
}
