import SwiftUI

struct MusicUnmatchedSheet: View {
    @State private var store: MusicLeftoversStore
    @State private var showAlone = false
    @Environment(\.dismiss) private var dismiss

    init(titleId: Int64) {
        _store = State(initialValue: MusicLeftoversStore(titleId: titleId))
    }

    private var info: MusicLeftoversInfo { store.info }

    var body: some View {
        NavigationStack {
            list
                .navigationTitle(L10n.tr("music.lib.unmatched.title"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(L10n.tr("title.close")) { dismiss() }
                    }
                }
                .sheet(isPresented: matchPresented) { matchSheet }
                .musicNote(info.note)
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var matchPresented: Binding<Bool> {
        let store = store
        let shown = store.info.match != nil
        return Binding(get: { shown }, set: { value in
            if !value { store.closeMatch() }
        })
    }

    @ViewBuilder
    private var matchSheet: some View {
        if let match = info.match {
            MusicMatchSheet(match: match, busy: info.busy, store: store)
        }
    }

    private var list: some View {
        List {
            Section {
                Text(L10n.tr("music.lib.unm.text"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                if let error = info.load.error, info.load.phase == .failed {
                    NoticeBanner(text: error.localized)
                }
                if info.load.phase == .loading || info.rescanning { ProgressView() }
                if !info.lastMatched.isEmpty { undoRow }
            }
            ForEach(info.pressings) { hint in
                pressingSection(hint)
            }
            filesSection
            if !info.alone.isEmpty { aloneSection }
        }
        .refreshable { await store.load() }
    }

    private var undoRow: some View {
        Button { store.undoMatch() } label: {
            Label(L10n.tr("music.lib.unm.undo"), systemImage: "arrow.uturn.backward")
        }
        .disabled(info.busy)
    }

    private func pressingSection(_ hint: MusicPressingHintInfo) -> some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(L10n.format("music.lib.unm.hint.title", hint.albumName))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Text(L10n.format("music.lib.unm.hint.body", hint.fits, hint.files, hint.pressingName, hint.trackCount, hint.chosen))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Button(L10n.tr("music.lib.unm.use.pressing")) { store.usePressing(hint) }
                    .buttonStyle(.borderless)
                    .disabled(info.busy || info.rescanning)
            }
        }
    }

    private var filesSection: some View {
        Section {
            if info.load.phase == .ready && info.open.isEmpty {
                Text(L10n.tr("music.lib.unm.nothing"))
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.open) { file in
                fileRow(file)
            }
        }
    }

    private func fileRow(_ file: MusicLeftoverInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(file.name)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(2)
            Text([TitleText.bytes(file.size), MusicLibraryArtistText.reason(file)].filter { !$0.isEmpty }.joined(separator: " · "))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if !file.closest.isEmpty {
                Text(L10n.format("music.lib.unm.closest", file.closest))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            HStack {
                Button(L10n.tr("music.lib.unm.leave")) { store.dismiss(file.fileId, dismissed: true) }
                Spacer()
                Button(L10n.tr("music.lib.unm.match")) { store.openMatch(file.fileId) }
                    .disabled(info.busy)
            }
            .buttonStyle(.borderless)
        }
    }

    private var aloneSection: some View {
        Section {
            Button { showAlone.toggle() } label: {
                HStack {
                    Text(L10n.format("music.lib.unm.alone", info.alone.count))
                        .foregroundStyle(Theme.text)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Theme.textFaint)
                        .rotationEffect(.degrees(showAlone ? 90 : 0))
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if showAlone {
                ForEach(info.alone) { file in
                    aloneRow(file)
                }
                Button(L10n.tr("music.lib.unm.refresh")) { Task { await store.load() } }
                    .buttonStyle(.borderless)
            }
        }
    }

    private func aloneRow(_ file: MusicLeftoverInfo) -> some View {
        HStack {
            Text(file.name)
                .font(.footnote)
                .foregroundStyle(Theme.text)
                .lineLimit(2)
            Spacer(minLength: Theme.Spacing.small)
            Button(L10n.tr("music.lib.unm.list.again")) { store.dismiss(file.fileId, dismissed: false) }
                .buttonStyle(.borderless)
                .disabled(info.busy)
        }
    }
}

struct MusicMatchSheet: View {
    let match: MusicMatchInfo
    let busy: Bool
    let store: MusicLeftoversStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                fileSection
                albumSection
                tracksSection
                if match.replaces {
                    Section {
                        Text(L10n.tr("music.lib.unm.replace.note"))
                            .font(.footnote)
                            .foregroundStyle(Theme.waitText)
                    }
                }
            }
            .navigationTitle(L10n.tr("music.lib.unm.match.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbar }
        }
    }

    private var fileSection: some View {
        Section {
            VStack(alignment: .leading, spacing: 2) {
                Text(match.file.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(3)
                Text([TitleText.bytes(match.file.size), MusicLibraryArtistText.reason(match.file)]
                    .filter { !$0.isEmpty }
                    .joined(separator: " · "))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }

    private var albumSection: some View {
        Section {
            Picker(L10n.tr("music.lib.unm.album"), selection: Binding(get: { match.albumId }, set: { store.pickAlbum($0) })) {
                ForEach(match.albums) { album in
                    Text(album.name).tag(album.id)
                }
            }
        }
    }

    private var tracksSection: some View {
        Section(L10n.tr("music.lib.unm.pick.track")) {
            if match.loading {
                ProgressView()
                    .frame(maxWidth: .infinity)
            } else if match.tracks.isEmpty {
                Text(L10n.tr("music.lib.unm.no.tracks"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(match.tracks) { track in
                trackRow(track)
            }
        }
    }

    private func trackRow(_ track: MusicMatchTrackInfo) -> some View {
        Button { store.pickTrack(track.id) } label: {
            HStack(spacing: Theme.Spacing.small) {
                Image(systemName: match.picked == track.id ? "largecircle.fill.circle" : "circle")
                    .foregroundStyle(match.picked == track.id ? Theme.accent : Theme.textFaint)
                Text(number(track))
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(Theme.textMuted)
                Text(track.name)
                    .font(.subheadline)
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                Spacer(minLength: 4)
                if track.hasFile {
                    Text(L10n.tr("music.lib.unm.has.file"))
                        .font(.caption)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(match.picked == track.id ? .isSelected : [])
    }

    private func number(_ track: MusicMatchTrackInfo) -> String {
        let value = String(format: "%02d", track.number)
        return track.disc > 1 ? "\(track.disc)-\(value)" : value
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button(L10n.tr("common.cancel")) {
                store.closeMatch()
                dismiss()
            }
        }
        ToolbarItem(placement: .confirmationAction) {
            Button(match.replaces ? L10n.tr("music.lib.unm.replace") : L10n.tr("music.lib.unm.match"), role: match.replaces ? .destructive : nil) {
                store.confirmMatch()
            }
            .disabled(match.picked == 0 || busy)
        }
    }
}
