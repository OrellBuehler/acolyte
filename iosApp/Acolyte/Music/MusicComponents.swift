import SwiftUI

struct MusicArt: View {
    let path: String?
    let name: String
    var round = false
    var size = 500

    var body: some View {
        AuthImage(source: path.flatMap { $0.isEmpty ? nil : MusicFormat.image($0, size: size) }, size: .posterLarge) {
            ArtPlaceholderView(name: name)
        }
        .aspectRatio(1, contentMode: .fit)
        .clipShape(shape)
    }

    private var shape: AnyShape {
        round ? AnyShape(Circle()) : AnyShape(RoundedRectangle(cornerRadius: Theme.Radius.poster, style: .continuous))
    }
}

struct MusicExplicitBadge: View {
    var body: some View {
        Text(L10n.tr("music.explicit.badge"))
            .font(.caption2.weight(.semibold))
            .foregroundStyle(Theme.textMuted)
            .padding(.horizontal, 4)
            .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: 3, style: .continuous))
            .accessibilityHidden(true)
    }
}

struct MusicCornerMark: View {
    let mark: MusicMarkKind

    var body: some View {
        if let info = cardMark {
            OwnedMarkView(mark: info)
        }
    }

    private var cardMark: CardMarkInfo? {
        switch mark {
        case .none, .missing: nil
        case .complete: CardMarkInfo(state: .available, label: .inLibrary, percent: -1)
        case .partial: CardMarkInfo(state: .partial, label: .partlyAvailable, percent: -1)
        case .downloading: CardMarkInfo(state: .downloading, label: .downloading, percent: -1)
        case .requested: CardMarkInfo(state: .requested, label: .requested, percent: -1)
        case .approved: CardMarkInfo(state: .requested, label: .approvedWaiting, percent: -1)
        }
    }
}

struct MusicMarkChip: View {
    let mark: MusicMarkKind

    var body: some View {
        if mark != .none {
            StatusChip(text: MusicText.mark(mark), tone: MusicText.tone(mark))
        }
    }
}

struct MusicBellButton: View {
    let following: Bool
    let name: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: following ? "bell.fill" : "bell")
                .font(.subheadline)
                .foregroundStyle(Theme.text)
                .frame(width: 34, height: 34)
                .background(Theme.surfaceHigh, in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L10n.format(following ? "music.follow.album.on" : "music.follow.album.off", name))
    }
}

struct MusicAddButton: View {
    let action: MusicCardActionKind
    let name: String
    let perform: () -> Void

    var body: some View {
        Button(action: perform) {
            Image(systemName: "plus")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
                .frame(width: 34, height: 34)
                .background(Theme.surfaceHigh, in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L10n.format(action == .add ? "music.action.add.artist" : "music.action.ask.artist", name))
    }
}

struct MusicItemCard: View {
    let item: MusicItemInfo
    var width: CGFloat = 148
    let onOpen: () -> Void
    let onAct: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MusicArt(path: item.image.isEmpty ? item.images.first : item.image, name: item.name, round: item.kind == .artist, size: 500)
                .frame(width: width, height: width)
                .overlay(alignment: .topTrailing) { MusicCornerMark(mark: item.mark).padding(6) }
                .overlay(alignment: .bottomTrailing) {
                    if item.action != .none {
                        MusicAddButton(action: item.action, name: item.artist.isEmpty ? item.name : item.artist, perform: onAct).padding(4)
                    }
                }
            HStack(spacing: 4) {
                Text(item.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if item.explicit { MusicExplicitBadge() }
            }
            .padding(.top, 6)
            let line = MusicText.line(item.line)
            if !line.isEmpty {
                Text(line)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
            if !item.fresh.isEmpty {
                Text(item.fresh)
                    .font(.caption2)
                    .foregroundStyle(Theme.accentText)
                    .lineLimit(1)
            }
        }
        .frame(width: width, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture(perform: onOpen)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

struct MusicAlbumCard: View {
    let album: MusicAlbumInfo
    var width: CGFloat = 148
    var subtitle: String?
    let onOpen: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MusicArt(path: album.image, name: album.name, size: width > 160 ? 500 : 250)
                .frame(width: width, height: width)
                .overlay(alignment: .topTrailing) { MusicCornerMark(mark: album.mark).padding(6) }
            HStack(spacing: 4) {
                Text(album.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if album.explicit { MusicExplicitBadge() }
            }
            .padding(.top, 6)
            let sub = subtitle ?? album.year
            if !sub.isEmpty {
                Text(sub)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        }
        .frame(width: width, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture(perform: onOpen)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

struct MusicArtistCircle: View {
    let artist: MusicArtistCardInfo
    var size: CGFloat = 120
    let onOpen: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            MusicArt(path: artist.image, name: artist.name, round: true, size: 250)
                .frame(width: size, height: size)
            Text(artist.name)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(1)
            if !artist.note.isEmpty {
                Text(artist.note)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        }
        .frame(width: size)
        .contentShape(Rectangle())
        .onTapGesture(perform: onOpen)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

struct MusicTileView: View {
    let name: String
    let color: String
    let cover: String?
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            ZStack(alignment: .topLeading) {
                Color(musicHex: color)
                Text(name)
                    .font(.headline)
                    .foregroundStyle(Color.white)
                    .lineLimit(2)
                    .padding(Theme.Spacing.medium)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let cover, !cover.isEmpty {
                    MusicArt(path: cover, name: name, size: 250)
                        .frame(width: 64, height: 64)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                        .padding(Theme.Spacing.small)
                }
            }
            .aspectRatio(16.0 / 9.0, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.poster, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

extension MusicTileView {
    init(tile: MusicTileInfo, onOpen: @escaping () -> Void) {
        self.init(name: tile.name, color: tile.color, cover: tile.covers.first, onOpen: onOpen)
    }

    init(item: MusicItemInfo, onOpen: @escaping () -> Void) {
        self.init(name: item.name, color: item.color, cover: item.images.first ?? item.image, onOpen: onOpen)
    }
}

struct MusicProblemView: View {
    let problem: MusicProblemKind
    let album: Bool
    var retry: (() -> Void)?

    var body: some View {
        VStack(spacing: Theme.Spacing.medium) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(Theme.textMuted)
            Text(MusicText.problem(problem, album: album))
                .font(.headline)
                .multilineTextAlignment(.center)
                .foregroundStyle(Theme.text)
            if let retry, problem == .offline || problem == .slow || problem == .none {
                Button(L10n.tr("common.retry"), action: retry)
                    .buttonStyle(.bordered)
            }
        }
        .padding(Theme.Spacing.large)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct MusicNoteModifier: ViewModifier {
    let note: MusicNoteInfo?
    @State private var toasts = DlToastModel()

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) { DlToastHost(model: toasts).padding(.bottom, Theme.Spacing.large) }
            .onChange(of: note?.id) { _, _ in
                guard let note else { return }
                let text = MusicText.note(note)
                if !text.isEmpty { toasts.show(text, tone: note.kind == .failed ? .fail : .ok) }
            }
    }
}

extension View {
    func musicNote(_ note: MusicNoteInfo?) -> some View {
        modifier(MusicNoteModifier(note: note))
    }
}
