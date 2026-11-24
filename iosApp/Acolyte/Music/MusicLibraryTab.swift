import SwiftUI

enum MusicLibraryArtistText {
    static func state(_ state: MusicAlbumStateID) -> String {
        switch state {
        case .notIncluded: L10n.tr("music.lib.state.not.included")
        case .noTracks: L10n.tr("music.lib.state.no.tracks")
        case .complete: L10n.tr("music.lib.state.complete")
        case .notOutYet: L10n.tr("music.lib.not.out.yet")
        case .notKept: L10n.tr("music.lib.state.not.kept")
        case .partial: L10n.tr("music.lib.state.partial")
        case .missing: L10n.tr("music.lib.state.missing")
        }
    }

    static func group(_ group: MusicLibGroupID) -> String {
        switch group {
        case .albums: L10n.tr("music.lib.group.albums")
        case .eps: L10n.tr("music.lib.group.eps")
        case .singles: L10n.tr("music.lib.group.singles")
        case .live: L10n.tr("music.lib.group.live")
        case .compilations: L10n.tr("music.lib.group.compilations")
        case .other: L10n.tr("music.lib.group.other")
        }
    }

    static func option(_ option: MusicOptionInfo) -> String {
        NSLocalizedString("music.lib.opt." + option.key.replacingOccurrences(of: "_", with: "."), comment: "")
    }

    static func preset(_ preset: MusicKindsPresetKind) -> String {
        switch preset {
        case .albums: L10n.tr("music.lib.kinds.albums")
        case .albumsEps: L10n.tr("music.lib.kinds.albums.eps")
        case .albumsEpsSingles: L10n.tr("music.lib.kinds.all")
        case .custom: L10n.tr("music.lib.kinds.mix")
        }
    }

    static func newAlbums(_ id: String) -> String {
        switch id {
        case "new": L10n.tr("music.lib.new.new")
        case "none": L10n.tr("music.lib.new.none")
        default: L10n.tr("music.lib.new.all")
        }
    }

    static func quick(_ kind: MusicQuickRuleKind) -> String {
        switch kind {
        case .any: L10n.tr("music.lib.quality.any")
        case .lossy: L10n.tr("music.lib.quality.lossy")
        case .lossless: L10n.tr("music.lib.quality.lossless")
        }
    }

    static func reason(_ file: MusicLeftoverInfo) -> String {
        switch file.reason {
        case "music_two_album": L10n.tr("music.lib.reason.two.album")
        case "music_album_off": L10n.tr("music.lib.reason.album.off")
        case "music_no_track": L10n.tr("music.lib.reason.no.track")
        case "music_unsure": L10n.tr("music.lib.reason.unsure")
        case "music_has_file": L10n.tr("music.lib.reason.has.file")
        case "music_duplicate": L10n.tr("music.lib.reason.duplicate")
        case "music_other_artist": L10n.format("music.lib.reason.other.artist", file.owner)
        case "music_not_listed": L10n.tr("music.lib.reason.not.listed")
        case "unmatched", "replaced_by_hand": L10n.tr("music.lib.reason.by.hand")
        default: L10n.tr("music.lib.reason.no.album")
        }
    }

    static func trackNumber(_ track: MusicLibTrackInfo) -> String {
        let number = String(format: "%02d", track.number)
        return track.multiDisc ? "\(track.disc)-\(number)" : number
    }
}

struct MusicLibraryTabContent: View {
    let info: MusicLibraryArtistInfo
    let store: MusicLibraryArtistStore
    @Binding var sheet: MusicLibrarySheet?
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.large) {
                if info.load.phase == .failed, let error = info.load.error {
                    NoticeBanner(text: error.localized)
                }
                MusicLibraryStatusCard(info: info, store: store)
                if info.unmatched > 0 { unmatchedRow }
                MusicLibraryReleases(info: info, store: store)
                MusicLibrarySettings(info: info, store: store, sheet: $sheet)
                historyRow
            }
            .padding(Theme.Spacing.screen)
        }
        .refreshable { await store.refresh() }
    }

    private var unmatchedRow: some View {
        Button { sheet = .unmatched } label: {
            HStack(spacing: Theme.Spacing.medium) {
                Image(systemName: "questionmark.folder")
                    .foregroundStyle(Theme.waitText)
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("music.lib.unmatched.title"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                    Text(L10n.plural("music.lib.unmatched.files", info.unmatched))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Spacer(minLength: 4)
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.textFaint)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.medium)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private var historyRow: some View {
        Button { openRoute(.history(titleId: info.titleId, episodeId: 0)) } label: {
            HStack {
                Label(L10n.tr("music.lib.history"), systemImage: "clock")
                    .foregroundStyle(Theme.text)
                Spacer(minLength: 4)
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.textFaint)
            }
            .padding(Theme.Spacing.medium)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct MusicLibraryStatusCard: View {
    let info: MusicLibraryArtistInfo
    let store: MusicLibraryArtistStore

    private var facts: [String] {
        var list: [String] = []
        if !info.folderPath.isEmpty { list.append(info.folderPath) }
        let size = TitleText.bytes(info.sizeBytes)
        if !size.isEmpty { list.append(size) }
        if !info.rulesName.isEmpty { list.append(info.rulesName) }
        list.append(info.keepUpToDate ? L10n.tr("music.lib.fact.kept") : L10n.tr("music.lib.fact.not.kept"))
        if info.includedCount > 0 {
            list.append(L10n.format("music.lib.fact.kept.releases", info.keptReleases, info.includedCount))
        }
        return list
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            headline
            if info.fullReleases > 0 {
                Text(L10n.plural("music.lib.full.releases", info.fullReleases))
                    .font(.subheadline)
                    .foregroundStyle(Theme.textMuted)
            }
            if !info.gateReason.isEmpty {
                Text(info.gateReason)
                    .font(.footnote)
                    .foregroundStyle(Theme.waitText)
            }
            if !info.noDownloader.isEmpty {
                NoticeBanner(text: L10n.format("music.lib.no.downloader", info.noDownloader))
            }
            MusicArtistFlowLayout {
                ForEach(facts, id: \.self) { fact in
                    MusicArtistChip(text: fact)
                }
            }
            if info.missingTracks > 0 {
                Button { store.searchAll() } label: {
                    Label(L10n.tr("music.lib.get.missing"), systemImage: "magnifyingglass")
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .disabled(info.working)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.large)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }

    private var headline: some View {
        HStack(spacing: Theme.Spacing.small) {
            Circle()
                .fill(Theme.dotColor(info.missingTracks > 0 || info.waitingForTracks ? .wait : .ok))
                .frame(width: 10, height: 10)
            if info.waitingForTracks {
                Text(L10n.tr("music.lib.waiting"))
                    .font(.headline)
                    .foregroundStyle(Theme.text)
            } else {
                Text(L10n.format("music.lib.status.tracks", info.haveTracks, info.totalTracks))
                    .font(.headline)
                    .foregroundStyle(Theme.text)
            }
        }
    }
}
