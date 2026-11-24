import SwiftUI

enum ReviewAction {
    case approve(AskReviewStore)
    case decline(String)
    case remove
    case openTitle(String, String)
}

struct AskReviewView: View {
    let onAction: (ReviewAction) -> Void
    @State private var store: AskReviewStore
    @State private var declining = false
    @State private var confirmRemove = false
    @Environment(\.dismiss) private var dismiss

    init(askId: Int64, onAction: @escaping (ReviewAction) -> Void) {
        self.onAction = onAction
        _store = State(initialValue: AskReviewStore(askId: askId))
    }

    private var info: AskReviewInfo { store.info }

    var body: some View {
        NavigationStack {
            Group {
                if let ask = info.ask {
                    content(ask)
                } else if let error = info.load.error {
                    ErrorStateView(error: error)
                } else {
                    LoadingStateView()
                }
            }
            .navigationTitle(info.ask.map { RequestsWords.titleWithYear($0.name, $0.year) } ?? "")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                if let ask = info.ask, ask.isWaiting {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(L10n.tr("requests.approve")) { onAction(.approve(store)) }
                            .disabled(!info.canApprove)
                    }
                }
            }
            .navigationDestination(isPresented: $declining) {
                if let ask = info.ask {
                    DeclineForm(ask: ask) { onAction(.decline($0)) }
                }
            }
            .alert(L10n.tr("requests.remove.title"), isPresented: $confirmRemove) {
                Button(L10n.tr("requests.remove"), role: .destructive) { onAction(.remove) }
                Button(L10n.tr("requests.keep"), role: .cancel) {}
            } message: {
                Text(L10n.tr("requests.remove.text"))
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private func content(_ ask: AskInfo) -> some View {
        List {
            Section {
                HStack(alignment: .top, spacing: Theme.Spacing.medium) {
                    RequestThumb(image: ask.poster, name: ask.name, width: 72)
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(RequestsWords.facts(ask))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                        Text(L10n.format("requests.asker.line", RequestsWords.names(ask.askers), ask.askedAt.map { Format.relative($0) } ?? ""))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                        StatusChip(text: RequestsWords.stage(ask.stage), tone: ask.tone)
                        if !ask.isWaiting {
                            AskLinesView(ask: ask, approver: true, myId: 0)
                        }
                    }
                }
                if !ask.isArtist, !ask.tmdbId.isEmpty {
                    Button(L10n.tr("requests.open.title")) { onAction(.openTitle(ask.kind, ask.tmdbId)) }
                }
            }
            if !info.seasons.isEmpty || !info.albums.isEmpty || (ask.isWaiting && ask.kind == "movie") {
                Section {
                    if ask.kind == "movie" { Text(L10n.tr("requests.the.movie")) }
                    ForEach(info.seasons) { season in
                        seasonRow(season, editable: ask.isWaiting)
                    }
                    ForEach(info.albums) { album in
                        albumRow(album, editable: ask.isWaiting)
                    }
                } header: {
                    Text(L10n.tr(ask.isWaiting ? "requests.what.to.add" : "requests.what.asked"))
                } footer: {
                    Text(partsHead)
                }
            }
            if ask.isWaiting, info.showsQuality {
                Section {
                    ForEach(info.pills) { pill in
                        Button {
                            store.select(pill.key)
                        } label: {
                            HStack {
                                Text(pill.rulesId > 0 ? pill.name : RequestsWords.quality(pill.line, pill.name))
                                    .foregroundStyle(Theme.text)
                                Spacer()
                                if pill.key == info.selected {
                                    Image(systemName: "checkmark").foregroundStyle(Theme.accentText)
                                }
                            }
                        }
                    }
                } header: {
                    Text(L10n.tr("requests.quality"))
                } footer: {
                    Text(qualityFooter(ask))
                }
            }
            if ask.isWaiting {
                Section {
                    Text(addsText(ask))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            Section {
                if ask.isWaiting || ask.isFailed {
                    Button(L10n.tr("requests.decline")) { declining = true }
                }
                Button(L10n.tr("requests.remove"), role: .destructive) { confirmRemove = true }
            }
        }
    }

    private var partsHead: String {
        if !info.seasons.isEmpty {
            let seasons = L10n.plural("requests.seasons", info.seasons.count)
            let head = info.keptSeasons == info.seasons.count
                ? L10n.format("requests.parts.all", seasons)
                : L10n.format("requests.parts.some", info.keptSeasons, seasons)
            return info.keptEpisodes > 0 ? "\(head) · \(L10n.plural("requests.episodes", info.keptEpisodes))" : head
        }
        if !info.albums.isEmpty {
            let counts = AlbumKindInfo.allCases.compactMap { kind in info.albumCounts[kind].map { RequestsWords.albumKinds(kind, $0) } }
                .joined(separator: " · ")
            var songs = ""
            if info.allSongs > 0 {
                songs = info.keptSongs == info.allSongs
                    ? L10n.plural("requests.songs", info.allSongs)
                    : L10n.format("requests.some.songs", info.keptSongs, info.allSongs)
            }
            return [counts, songs].filter { !$0.isEmpty }.joined(separator: " · ")
        }
        return ""
    }

    private func addsText(_ ask: AskInfo) -> String {
        if info.empty { return L10n.tr("requests.tick.one") }
        if info.changed { return L10n.tr("requests.adds.ticked") }
        if ask.kind == "movie" { return L10n.tr("requests.adds.movie") }
        return L10n.format("requests.adds.what", RequestsWords.askedWhat(ask))
    }

    private func qualityFooter(_ ask: AskInfo) -> String {
        let asked = L10n.format("requests.asked.for.quality", RequestsWords.names(ask.askers), RequestsWords.quality(ask.qualityTag ?? "standard", ask.qualityName))
        guard info.extraOnly, let pill = info.pills.first(where: { $0.key == info.selected }) else { return asked }
        return asked + " " + L10n.format("requests.adds.only.copy", RequestsWords.quality(pill.line, pill.name))
    }

    private func tickImage(_ tick: TickKind) -> String {
        switch tick {
        case .all: "checkmark.square.fill"
        case .some: "minus.square.fill"
        case .off: "square"
        }
    }

    private func seasonRow(_ season: SeasonPickInfo, editable: Bool) -> some View {
        DisclosureGroup(isExpanded: Binding(get: { season.expanded }, set: { _ in store.expandSeason(season.number) })) {
            if season.loading {
                ProgressView()
            } else if season.failed {
                Text(L10n.tr("requests.episodes.failed")).foregroundStyle(Theme.failText)
            } else if season.episodes.isEmpty {
                Text(L10n.tr("requests.episodes.none")).foregroundStyle(Theme.textMuted)
            } else {
                ForEach(season.episodes) { episode in
                    Toggle(isOn: Binding(get: { episode.ticked }, set: { _ in store.toggleEpisode(season.number, episode.number) })) {
                        Text(episode.name.isEmpty ? L10n.format("requests.episode", episode.number) : "\(episode.number). \(episode.name)")
                    }
                    .disabled(!editable)
                }
            }
        } label: {
            HStack {
                Button {
                    store.toggleSeason(season.number)
                } label: {
                    Image(systemName: tickImage(season.tick))
                        .foregroundStyle(Theme.accentText)
                }
                .buttonStyle(.borderless)
                .disabled(!editable)
                VStack(alignment: .leading) {
                    Text(RequestsWords.seasonName(season.number))
                    Text(seasonLine(season))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    private func seasonLine(_ season: SeasonPickInfo) -> String {
        if season.all == 0 { return L10n.tr("requests.episodes.not.listed") }
        if season.asked >= season.all { return L10n.format("requests.all.episodes", season.all) }
        return L10n.format("requests.some.episodes", season.asked, season.all)
    }

    private func albumRow(_ album: AlbumPickInfo, editable: Bool) -> some View {
        DisclosureGroup(isExpanded: Binding(get: { album.expanded }, set: { _ in store.expandAlbum(album.mbid) })) {
            ForEach(album.songs) { song in
                Toggle(isOn: Binding(get: { song.ticked }, set: { _ in store.toggleSong(album.mbid, song.key) })) {
                    Text("\(song.disc > 1 ? "\(song.disc)-" : "")\(song.number). \(song.name)")
                }
                .disabled(!editable)
            }
        } label: {
            HStack {
                Button {
                    store.toggleAlbum(album.mbid)
                } label: {
                    Image(systemName: tickImage(album.tick))
                        .foregroundStyle(Theme.accentText)
                }
                .buttonStyle(.borderless)
                .disabled(!editable)
                VStack(alignment: .leading) {
                    Text(album.name)
                    Text(albumLine(album))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    if !editable, album.tick == .off {
                        Text(L10n.tr("requests.left.out")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            }
        }
    }

    private func albumLine(_ album: AlbumPickInfo) -> String {
        var parts: [String] = []
        if album.year > 0 { parts.append(String(album.year)) }
        parts.append(RequestsWords.albumKind(album.kind))
        if album.editions > 0 { parts.append(L10n.plural("requests.editions", album.editions)) }
        if !album.songs.isEmpty { parts.append(L10n.plural("requests.songs", album.songs.count)) }
        return parts.joined(separator: " · ")
    }
}
