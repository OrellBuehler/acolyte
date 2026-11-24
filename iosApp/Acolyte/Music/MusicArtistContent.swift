import SwiftUI

struct MusicArtistEmbed {
    let library: MusicLibraryArtistInfo
    let openLibrary: () -> Void
}

struct MusicArtistContent: View {
    let info: MusicArtistInfo
    let store: MusicArtistStore
    var embed: MusicArtistEmbed?
    let onAbout: () -> Void
    let openSheet: (Route) -> Void
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xLarge) {
            top
            songsAndAlbums
            comingAndVideos
            statsAndPeople
            about
        }
        .padding(.bottom, Theme.Spacing.xLarge)
    }

    private var top: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            MusicArtistHero(info: info)
            if info.load.phase == .failed, let error = info.load.error {
                NoticeBanner(text: error.localized)
                    .padding(.horizontal, Theme.Spacing.screen)
            }
            MusicArtistHeader(info: info, store: store, embed: embed, onAbout: onAbout, openSheet: openSheet)
            if let embed {
                MusicArtistEmbedInfo(library: embed.library)
            }
        }
    }

    @ViewBuilder
    private var songsAndAlbums: some View {
        if !info.top.isEmpty { topSongs }
        if !info.essential.isEmpty {
            MusicArtistAlbumShelf(
                title: L10n.tr("music.artist.essential"), albums: info.essential, info: info, withKind: false, openSheet: openSheet
            )
        }
        ForEach(info.shelves) { shelf in
            MusicArtistAlbumShelf(
                title: MusicArtistText.shelfTitle(shelf.kind),
                albums: shelf.albums,
                info: info,
                withKind: shelf.kind != .albums,
                openSheet: openSheet
            )
        }
        if info.shelves.isEmpty && info.coming.isEmpty && !info.storeAlbums.isEmpty { storeShelf }
        if info.shelves.isEmpty && !info.gap.isEmpty {
            MusicArtistNote(text: info.gap == "gap" ? L10n.tr("music.artist.gap.fallback") : info.gap)
        }
    }

    private var topSongs: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("music.artist.top.songs"))
                .padding(.horizontal, Theme.Spacing.screen)
            ForEach(Array(info.top.prefix(10).enumerated()), id: \.offset) { index, song in
                MusicArtistSongRow(number: index + 1, song: song, artistMbid: info.mbid)
            }
        }
    }

    private var storeShelf: some View {
        MusicArtistShelf(title: L10n.tr("music.artist.discography")) {
            ForEach(info.storeAlbums) { album in
                MusicArtistStoreCard(album: album)
            }
        }
    }

    @ViewBuilder
    private var comingAndVideos: some View {
        if !info.coming.isEmpty { coming }
        if !info.videos.isEmpty {
            MusicArtistShelf(title: L10n.tr("music.artist.videos")) {
                ForEach(info.videos) { video in
                    MusicArtistVideoCard(video: video)
                }
            }
        }
    }

    private var coming: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            SectionHeader(title: L10n.tr("music.artist.coming"))
                .padding(.horizontal, Theme.Spacing.screen)
            ForEach(info.coming) { album in
                let mbid = album.mbid
                MusicArtistComingRow(album: album, artistMbid: info.mbid) { store.toggleAlbumFollow(mbid) }
            }
        }
    }

    @ViewBuilder
    private var statsAndPeople: some View {
        if info.listeners > 0 || info.rank > 0 || info.askAll != nil {
            MusicArtistStatsBand(info: info) { store.askAll() }
        }
        if !info.appearsOn.isEmpty {
            MusicArtistShelf(title: L10n.tr("music.artist.appears.on")) {
                ForEach(info.appearsOn) { album in
                    MusicAlbumCard(album: album, subtitle: album.artistName.isEmpty ? album.year : album.artistName) {
                        openRoute(.musicAlbum(mbid: album.mbid, artist: album.artistMbid.isEmpty ? info.mbid : album.artistMbid))
                    }
                }
            }
        }
        if !info.related.isEmpty {
            MusicArtistShelf(title: L10n.tr("music.artist.related")) {
                ForEach(info.related) { card in
                    MusicArtistCircle(artist: card) { openRoute(.musicArtist(mbid: card.mbid)) }
                }
            }
        }
        if !info.similar.isEmpty || info.similarLoading { similar }
    }

    private var similar: some View {
        MusicArtistShelf(title: L10n.tr("music.artist.similar")) {
            if info.similar.isEmpty {
                ForEach(0..<4, id: \.self) { _ in MusicArtistPlaceholderCircle() }
            } else {
                ForEach(info.similar) { card in
                    MusicArtistCircle(artist: card) { openRoute(.musicArtist(mbid: card.mbid)) }
                }
            }
        }
    }

    @ViewBuilder
    private var about: some View {
        if info.bio != nil || !info.genres.isEmpty {
            MusicArtistAboutCard(info: info, onOpen: onAbout)
        }
    }
}

struct MusicArtistHeader: View {
    let info: MusicArtistInfo
    let store: MusicArtistStore
    let embed: MusicArtistEmbed?
    let onAbout: () -> Void
    let openSheet: (Route) -> Void
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            identity
            MusicArtistFlowLayout {
                mainButton
                Button(action: onAbout) {
                    Text(L10n.tr("music.artist.about"))
                }
                .buttonStyle(.bordered)
                followButton
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }

    private var identity: some View {
        HStack(spacing: Theme.Spacing.large) {
            MusicArt(path: info.image.isEmpty ? info.wide : info.image, name: info.name, round: true, size: 250)
                .frame(width: 104, height: 104)
            VStack(alignment: .leading, spacing: 4) {
                Text(info.name)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(3)
                let line = MusicArtistText.listeners(info)
                if !line.isEmpty {
                    Text(line)
                        .font(.subheadline)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    @ViewBuilder
    private var followButton: some View {
        if info.followKnown {
            if info.following {
                Button { store.toggleFollow() } label: {
                    Label(L10n.tr("music.artist.following"), systemImage: "checkmark")
                }
                .buttonStyle(.bordered)
                .tint(Theme.accent)
            } else {
                Button(L10n.tr("music.artist.follow")) { store.toggleFollow() }
                    .buttonStyle(.bordered)
            }
        }
    }

    @ViewBuilder
    private var mainButton: some View {
        if let embed {
            embeddedMain(embed)
        } else {
            standardMain
        }
    }

    @ViewBuilder
    private func embeddedMain(_ embed: MusicArtistEmbed) -> some View {
        if embed.library.canManage {
            Button(action: embed.openLibrary) {
                Text(L10n.tr("music.artist.main.in.library"))
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
        } else if embed.library.canAsk {
            Button { openSheet(.askMusic(artist: info.mbid, name: info.name, album: "", edition: "")) } label: {
                Text(L10n.tr("music.artist.main.ask.albums"))
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
        }
    }

    @ViewBuilder
    private var standardMain: some View {
        switch info.main {
        case .none:
            EmptyView()
        case .openLibrary:
            Button { openRoute(.musicLibraryArtist(titleId: info.titleId, tab: "")) } label: {
                Text(L10n.tr("music.artist.main.open"))
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
        case .add:
            Button(action: add) {
                Label(L10n.tr("music.artist.main.add"), systemImage: "plus")
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
        case .ask:
            Button(action: ask) {
                Label(L10n.tr("music.artist.main.ask"), systemImage: "plus")
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
        case .asked:
            Button {} label: {
                Label(L10n.tr("music.artist.main.asked"), systemImage: "checkmark")
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            .disabled(true)
        }
    }

    private func add() {
        if info.mainReady {
            openSheet(.musicAdd(mbid: info.mbid, name: info.name))
        } else {
            store.stillReading()
        }
    }

    private func ask() {
        if info.mainReady {
            openSheet(.askMusic(artist: info.mbid, name: info.name, album: "", edition: ""))
        } else {
            store.stillReading()
        }
    }
}

struct MusicArtistEmbedInfo: View {
    let library: MusicLibraryArtistInfo

    private var progressText: String {
        var parts: [String] = []
        if library.waitingForTracks {
            parts.append(L10n.format("music.artist.progress.albums", library.includedCount, library.albumCount))
        } else {
            parts.append(MusicArtistText.plural("music.artist.progress.tracks", library.totalTracks, library.haveTracks, library.totalTracks))
            if library.missingTracks > 0 {
                parts.append(L10n.format("music.artist.progress.missing", library.missingTracks))
            } else {
                parts.append(L10n.tr("music.artist.progress.complete"))
            }
            if library.notOutYet > 0 {
                parts.append(L10n.format("music.artist.progress.not.out", library.notOutYet))
            }
        }
        return parts.joined(separator: " · ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if !library.disambiguation.isEmpty {
                Text(library.disambiguation)
                    .font(.subheadline)
                    .foregroundStyle(Theme.textMuted)
            }
            if library.canManage { progress }
            if !library.nextRelease.isEmpty {
                Text(L10n.format("music.artist.next.album", MusicText.shortDay(library.nextRelease)))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Theme.Spacing.screen)
    }

    private var progress: some View {
        HStack(spacing: Theme.Spacing.medium) {
            ProgressView(value: Double(min(max(library.progress, 0), 1)))
                .tint(Theme.accent)
                .frame(width: 120)
            Text(progressText)
                .font(.footnote)
                .foregroundStyle(Theme.text)
        }
    }
}

struct MusicLibraryAsksBlock: View {
    let asks: [MusicAskLineInfo]

    var body: some View {
        if !asks.isEmpty {
            VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                SectionHeader(title: L10n.tr("music.lib.requests"))
                ForEach(asks) { ask in
                    row(ask)
                }
            }
            .padding(.horizontal, Theme.Spacing.screen)
        }
    }

    private func row(_ ask: MusicAskLineInfo) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(L10n.format("music.lib.ask.by", ask.askedBy, MusicText.shortDay(ask.askedAt)))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            if !ask.albums.isEmpty {
                Text(L10n.format("music.lib.ask.for", ask.albums)).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if !ask.decidedBy.isEmpty {
                Text(L10n.format("music.lib.ask.decided", ask.decidedBy, MusicText.shortDay(ask.decidedAt)))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if !ask.stateLabel.isEmpty {
                Text(L10n.format("music.lib.ask.now", ask.stateLabel)).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if !ask.note.isEmpty {
                Text(L10n.format("music.lib.ask.note", ask.note)).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.medium)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}
