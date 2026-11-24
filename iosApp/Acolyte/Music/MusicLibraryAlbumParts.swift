import SwiftUI

struct MusicLibraryAlbumActions {
    let search: @MainActor () -> Void
    let toggleKeep: @MainActor () -> Void
    let rename: @MainActor () -> Void
    let delete: @MainActor () -> Void
    let report: @MainActor () -> Void
}

struct MusicLibraryAlbumMenu: View {
    let info: MusicLibraryAlbumInfo
    let actions: MusicLibraryAlbumActions

    var body: some View {
        if info.canManage || info.hasFile {
            Menu {
                if info.canManage { manageItems }
                if info.hasFile {
                    Button(L10n.tr("music.album.lib.report")) { actions.report() }
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
            .accessibilityLabel(L10n.tr("music.album.lib.more.actions"))
        }
    }

    @ViewBuilder
    private var manageItems: some View {
        Button(L10n.tr("music.album.lib.search.now")) { actions.search() }
        Toggle(L10n.tr("music.album.lib.keep"), isOn: Binding(get: { info.keep }, set: { _ in actions.toggleKeep() }))
            .disabled(!info.included || info.busy)
        Button(L10n.tr("music.album.lib.rename")) { actions.rename() }
        Divider()
        Button(L10n.tr("music.album.lib.delete"), role: .destructive) { actions.delete() }
    }
}

struct MusicLibraryAlbumCover: View {
    let info: MusicLibraryAlbumInfo

    private var source: String? {
        info.cover.isEmpty ? nil : MusicFormat.image(info.cover, size: 500)
    }

    var body: some View {
        AuthImage(source: source, size: .posterLarge) {
            MusicArt(path: info.artistImage, name: info.name, size: 500)
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(width: 220, height: 220)
        .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}

struct MusicLibraryAlbumHero: View {
    let info: MusicLibraryAlbumInfo
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        VStack(spacing: Theme.Spacing.small) {
            MusicLibraryAlbumCover(info: info)
            if !info.artistName.isEmpty { breadcrumb }
            Text(info.name)
                .font(.title2.weight(.bold))
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.center)
            Text(meta)
                .font(.subheadline)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            if !info.disambiguation.isEmpty {
                Text(info.disambiguation)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            }
            chips
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.large)
        .frame(maxWidth: .infinity)
        .background(LinearGradient(colors: [Theme.surfaceHigh, Theme.background], startPoint: .top, endPoint: .bottom))
    }

    private var meta: String {
        let tracks = info.trackCount > 0 ? MusicAlbumText.plural("music.album.lib.tracks", info.trackCount, info.trackCount) : ""
        let parts = [info.year, MusicText.kind(info.kind), tracks, MusicAlbumText.duration(info.minutes)]
        return parts.filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private var breadcrumb: some View {
        HStack(spacing: 6) {
            Text(L10n.tr("section.music"))
                .font(.caption.weight(.medium))
                .foregroundStyle(Theme.textMuted)
            Text("›")
                .font(.caption.weight(.medium))
                .foregroundStyle(Theme.textMuted)
            Button {
                openRoute(.musicLibraryArtist(titleId: info.titleId, tab: ""))
            } label: {
                Text(info.artistName)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Theme.accentText)
                    .lineLimit(1)
            }
            .buttonStyle(.plain)
        }
    }

    private var chips: some View {
        HStack(spacing: Theme.Spacing.medium) {
            if info.canManage {
                StatusChip(text: MusicAlbumText.state(info.stateKey), tone: MusicAlbumText.stateTone(info.stateKey))
            } else {
                StatusChip(
                    text: info.complete ? L10n.tr("music.album.lib.in.yours") : L10n.tr("music.album.lib.not.here"),
                    tone: info.complete ? .ok : .idle
                )
            }
            if info.dropped {
                StatusChip(text: L10n.tr("music.album.lib.not.listed"), tone: .wait)
            }
        }
    }
}

struct MusicLibraryAlbumTrackRow: View {
    let track: MusicLibTrackInfo
    let staff: Bool

    private var number: String {
        track.multiDisc ? "\(track.disc)-\(track.number)" : "\(track.number)"
    }

    private var haveFile: Bool {
        track.slots.contains { $0.tone == .ok } || (track.slots.isEmpty && track.tone == .ok)
    }

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.small) {
            Text(number)
                .font(.subheadline.monospacedDigit())
                .foregroundStyle(Theme.textMuted)
                .frame(width: 44, alignment: .leading)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(track.name.isEmpty ? L10n.tr("music.album.lib.name.unknown") : track.name)
                    .font(.body)
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                if staff { slotChips } else { memberChip }
            }
            Spacer(minLength: 4)
            if !track.length.isEmpty {
                Text(track.length)
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private var slotChips: some View {
        if track.slots.isEmpty {
            StatusChip(text: track.stateLabel, tone: track.tone)
        } else {
            MusicAlbumFlowLayout(spacing: 6) {
                ForEach(track.slots.indices, id: \.self) { index in
                    let slot = track.slots[index]
                    StatusChip(text: slotText(slot), tone: slot.tone)
                }
            }
        }
    }

    private func slotText(_ slot: MusicSlotChipInfo) -> String {
        track.slots.count > 1 && !slot.version.isEmpty ? "\(slot.version): \(slot.label)" : slot.label
    }

    private var memberChip: some View {
        StatusChip(
            text: haveFile ? L10n.tr("music.album.lib.in.yours") : L10n.tr("music.album.lib.not.here"),
            tone: haveFile ? .ok : .idle
        )
    }
}

struct MusicLibraryAlbumCard: View {
    let info: MusicLibraryAlbumInfo
    let actions: MusicLibraryAlbumActions
    let onPick: (MusicPressingInfo) -> Void
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            Text(L10n.tr("music.album.lib.library"))
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
            HStack(spacing: Theme.Spacing.small) {
                StatusChip(text: MusicAlbumText.state(info.stateKey), tone: MusicAlbumText.stateTone(info.stateKey))
                Text(L10n.format("music.album.lib.of", info.inLibrary, info.trackCount))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            buttons
            Divider()
            Text(L10n.tr("music.album.lib.settings"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            keepToggle
            if info.pressings.count > 1 { edition }
        }
        .padding(Theme.Spacing.large)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.large)
    }

    private var buttons: some View {
        HStack(spacing: Theme.Spacing.small) {
            Button(L10n.tr("music.album.lib.search.now")) { actions.search() }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accentFill)
            Button(L10n.tr("section.history")) {
                openRoute(.history(titleId: info.titleId, episodeId: 0))
            }
            .buttonStyle(.bordered)
            .tint(Theme.text)
        }
    }

    private var keepToggle: some View {
        Toggle(isOn: Binding(get: { info.keep }, set: { _ in actions.toggleKeep() })) {
            VStack(alignment: .leading, spacing: 2) {
                Text(L10n.tr("music.album.lib.keep"))
                Text(info.included ? L10n.tr("music.album.lib.keep.hint") : L10n.tr("music.album.lib.keep.left.out"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .tint(Theme.accent)
        .disabled(!info.included || info.busy)
    }

    private var editionDetail: String {
        let how = info.byRule ? L10n.tr("music.album.lib.by.rule") : L10n.tr("music.album.lib.by.hand")
        guard let current = info.pressings.first(where: { $0.chosen }) else { return how }
        return "\(how) · \(MusicAlbumText.pressing(current))"
    }

    private var edition: some View {
        Menu {
            ForEach(info.pressings) { pressing in
                Button {
                    if !pressing.chosen { onPick(pressing) }
                } label: {
                    if pressing.chosen {
                        Label(MusicAlbumText.pressing(pressing), systemImage: "checkmark")
                    } else {
                        Text(MusicAlbumText.pressing(pressing))
                    }
                }
            }
        } label: {
            HStack(spacing: Theme.Spacing.small) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("music.album.lib.edition"))
                        .foregroundStyle(Theme.text)
                    Text(editionDetail)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
    }
}

struct MusicLibraryAlbumMore: View {
    let info: MusicLibraryAlbumInfo
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        MusicAlbumShelf(title: L10n.format("music.album.more.by", info.artistName)) {
            ForEach(info.moreBy) { entry in
                card(entry)
            }
        }
        .padding(.bottom, Theme.Spacing.xLarge)
    }

    private func card(_ entry: MusicLibAlbumInfo) -> some View {
        let full = entry.tracks > 0 && entry.inLibrary >= entry.tracks
        let subtitle = full
            ? L10n.tr("music.album.lib.in.yours")
            : L10n.format("music.album.lib.tracks.here", entry.inLibrary, entry.tracks)
        return MusicAlbumCard(album: album(entry, full: full), subtitle: subtitle) {
            openRoute(.musicLibraryAlbum(titleId: info.titleId, albumId: entry.id))
        }
    }

    private func album(_ entry: MusicLibAlbumInfo, full: Bool) -> MusicAlbumInfo {
        let mark: MusicMarkKind = full ? .complete : .none
        return MusicAlbumInfo(
            mbid: entry.mbid,
            name: entry.name,
            kind: .album,
            year: entry.year,
            date: entry.date,
            image: entry.cover,
            explicit: false,
            upcoming: entry.future,
            mark: mark,
            following: false,
            partId: entry.id,
            artistMbid: info.artistMbid,
            artistName: info.artistName,
            asked: false
        )
    }
}

enum MusicLibraryAlbumSheet: Identifiable {
    case rename(RenameStore)
    case remove(RemoveStore)

    var id: String {
        switch self {
        case .rename: "rename"
        case .remove: "remove"
        }
    }
}

struct MusicLibraryAlbumSheetView: View {
    let sheet: MusicLibraryAlbumSheet
    let onDeleted: () -> Void

    var body: some View {
        switch sheet {
        case let .rename(store):
            MusicLibraryAlbumRenameSheet(store: store)
        case let .remove(store):
            MusicLibraryAlbumRemoveSheet(store: store, onDone: onDeleted)
        }
    }
}

struct MusicLibraryAlbumRenameSheet: View {
    let store: RenameStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List { RenameSection(store: store) }
                .navigationTitle(L10n.tr("title.rename.files"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(L10n.tr("title.close")) { dismiss() }
                    }
                }
        }
    }
}

struct MusicLibraryAlbumRemoveSheet: View {
    let store: RemoveStore
    let onDone: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            RemoveView(store: store)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(L10n.tr("title.close")) { dismiss() }
                    }
                }
        }
        .onChange(of: store.info?.done) { _, done in
            if done == true { onDone() }
        }
    }
}
