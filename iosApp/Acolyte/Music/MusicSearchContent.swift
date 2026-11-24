import SwiftUI

struct MusicSearchContent: View {
    let query: String
    let browse: MusicBrowseStore
    let search: MusicSearchStore

    var body: some View {
        MusicOpenerReader { opener in
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                if query.trimmingCharacters(in: .whitespaces).isEmpty {
                    MusicSearchBrowse(info: browse.info, opener: opener)
                } else {
                    MusicSearchHits(query: query.trimmingCharacters(in: .whitespaces), info: search.info, opener: opener)
                }
            }
        }
        .task { await browse.watch() }
        .task { await search.watch() }
        .task { await browse.load() }
    }
}

struct MusicSearchBrowse: View {
    let info: MusicBrowseInfo
    let opener: MusicOpener

    var body: some View {
        SectionHeader(title: L10n.tr("music.search.browse"))
            .padding(.horizontal, Theme.Spacing.screen)
        if info.load.phase == .loading {
            ProgressView().frame(maxWidth: .infinity).padding(Theme.Spacing.xLarge)
        } else {
            if let error = info.load.error {
                NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
            }
            if info.isEmpty || info.kid {
                MusicSearchHint(text: L10n.tr("music.search.empty"))
            } else if info.sections.isEmpty {
                MusicTileGrid(cells: info.tiles.map { MusicTileCell(tile: $0) }, opener: opener)
            } else {
                ForEach(info.sections) { section in
                    SectionHeader(title: section.title).padding(.horizontal, Theme.Spacing.screen)
                    MusicTileGrid(cells: section.tiles.map { MusicTileCell(tile: $0) }, opener: opener)
                }
            }
        }
    }
}

struct MusicSearchHits: View {
    let query: String
    let info: MusicSearchInfo
    let opener: MusicOpener

    var body: some View {
        if info.searched != query {
            pending
        } else {
            results
        }
    }

    @ViewBuilder
    private var pending: some View {
        if info.searching {
            VStack(spacing: Theme.Spacing.small) {
                ProgressView()
                MusicSearchHint(text: L10n.tr("music.search.searching"))
            }
            .frame(maxWidth: .infinity)
            .padding(Theme.Spacing.xLarge)
        } else {
            MusicSearchHint(text: L10n.tr("music.search.hint"))
        }
    }

    @ViewBuilder
    private var results: some View {
        if let error = info.error {
            NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
        }
        if info.hits.isEmpty {
            if info.error == nil { MusicSearchHint(text: L10n.tr("music.search.none")) }
        } else {
            ForEach(info.hits) { hit in
                MusicSearchHitRow(hit: hit, opener: opener)
            }
        }
    }
}

struct MusicSearchHint: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.callout)
            .foregroundStyle(Theme.textMuted)
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
    }
}

struct MusicSearchHitRow: View {
    let hit: MusicArtistHitInfo
    let opener: MusicOpener

    private var subtitle: String {
        if !hit.disambiguation.isEmpty { return hit.disambiguation }
        return [hit.type, hit.country].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            MusicArt(path: hit.image, name: hit.name, round: true, size: 250)
                .frame(width: 56, height: 56)
            VStack(alignment: .leading, spacing: 2) {
                Text(hit.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .lineLimit(2)
                }
            }
            Spacer(minLength: Theme.Spacing.small)
            trailing
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.small)
        .contentShape(Rectangle())
        .onTapGesture { opener.artist(hit.mbid) }
    }

    @ViewBuilder
    private var trailing: some View {
        if hit.titleId > 0 {
            MusicMarkChip(mark: .complete)
        } else if hit.action == .add {
            Button(L10n.tr("music.search.add")) { opener.act(hit) }
                .buttonStyle(.bordered)
        } else if hit.action == .ask {
            Button(L10n.tr(hit.tooAction ? "music.search.ask.too" : "music.search.ask")) { opener.act(hit) }
                .buttonStyle(.bordered)
        } else if hit.asked {
            MusicMarkChip(mark: .requested)
        }
    }
}

struct MusicScopeChips: View {
    let scopes: [SearchScopeID]
    let selected: SearchScopeID
    let select: (SearchScopeID) -> Void

    var body: some View {
        ServiceChipsView(
            chips: scopes.enumerated().map { ServiceChipInfo(id: Int64($0.offset), name: scopeText($0.element)) },
            selected: Int64(scopes.firstIndex(of: selected) ?? 0),
            pick: { index in
                let position = Int(index)
                if scopes.indices.contains(position) { select(scopes[position]) }
            }
        )
    }
}
