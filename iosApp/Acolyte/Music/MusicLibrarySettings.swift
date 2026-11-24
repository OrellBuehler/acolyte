import SwiftUI

struct MusicLibrarySettings: View {
    let info: MusicLibraryArtistInfo
    let store: MusicLibraryArtistStore
    @Binding var sheet: MusicLibrarySheet?
    @State private var pendingFolder: MusicFolderInfo?
    @State private var askMove = false

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            Text(L10n.tr("music.lib.settings"))
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            VStack(alignment: .leading, spacing: Theme.Spacing.large) {
                keepRow
                qualityRow
                kindsRow
                if info.folders.count > 1 { folderRow }
                actionRows
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.large)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
            deleteButton
        }
        .confirmationDialog(
            pendingFolder.map { L10n.format("music.lib.move.title", $0.label) } ?? "",
            isPresented: $askMove,
            titleVisibility: .visible,
            presenting: pendingFolder
        ) { folder in
            Button(L10n.tr("music.lib.move.confirm")) { store.moveTo(folder.id) }
            Button(L10n.tr("common.cancel"), role: .cancel) {}
        } message: { _ in
            Text(L10n.tr("music.lib.move.body"))
        }
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
    }

    private var keepRow: some View {
        Toggle(isOn: Binding(get: { info.keepUpToDate }, set: { store.setKeepArtist($0) })) {
            VStack(alignment: .leading, spacing: 2) {
                Text(L10n.tr("music.lib.keep.artist"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                caption(L10n.tr("music.lib.keep.artist.hint"))
            }
        }
        .tint(Theme.accent)
    }

    private var qualityRow: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("music.lib.quality"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            if !info.quick.isEmpty { qualityPicker }
            if !info.rules.isEmpty { rulesMenu }
        }
    }

    private var qualityPicker: some View {
        Picker(
            L10n.tr("music.lib.quality"),
            selection: Binding<Int64?>(
                get: { info.customQuality ? nil : info.rulesId },
                set: { value in
                    if let value { store.setRules(value) }
                }
            )
        ) {
            ForEach(info.quick) { quick in
                Text(MusicLibraryArtistText.quick(quick.kind)).tag(Int64?.some(quick.id))
            }
        }
        .pickerStyle(.segmented)
    }

    private var rulesMenu: some View {
        Menu {
            ForEach(info.rules) { rule in
                Button { store.setRules(rule.id) } label: {
                    if rule.id == info.rulesId {
                        Label(rule.name, systemImage: "checkmark")
                    } else {
                        Text(rule.name)
                    }
                }
            }
        } label: {
            HStack(spacing: 4) {
                Text(info.customQuality ? info.rulesName : L10n.tr("music.lib.quality.custom"))
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption2)
            }
            .font(.subheadline)
            .foregroundStyle(Theme.accentText)
        }
    }

    private var kindsRow: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("music.lib.kinds"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            Picker(
                L10n.tr("music.lib.kinds"),
                selection: Binding<MusicKindsPresetKind?>(
                    get: { info.preset == .custom ? nil : info.preset },
                    set: { value in
                        if let value { store.applyPreset(value) }
                    }
                )
            ) {
                Text(L10n.tr("music.lib.preset.albums")).tag(MusicKindsPresetKind?.some(.albums))
                Text(L10n.tr("music.lib.preset.eps")).tag(MusicKindsPresetKind?.some(.albumsEps))
                Text(L10n.tr("music.lib.preset.singles")).tag(MusicKindsPresetKind?.some(.albumsEpsSingles))
            }
            .pickerStyle(.segmented)
            caption(L10n.format("music.lib.kinds.hint", MusicLibraryArtistText.preset(info.preset), MusicLibraryArtistText.newAlbums(info.newAlbums)))
            Button(L10n.tr("music.lib.choose.kinds")) { sheet = .kinds }
                .font(.subheadline)
                .foregroundStyle(Theme.accentText)
        }
    }

    private var folderRow: some View {
        HStack {
            Text(L10n.tr("music.lib.folder"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            Spacer(minLength: Theme.Spacing.small)
            Menu {
                ForEach(info.folders) { folder in
                    Button { pick(folder) } label: {
                        if folder.id == info.folderId {
                            Label(folder.label, systemImage: "checkmark")
                        } else {
                            Text(folder.label)
                        }
                    }
                }
            } label: {
                HStack(spacing: 4) {
                    Text(currentFolder)
                        .lineLimit(1)
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.caption2)
                }
                .font(.subheadline)
                .foregroundStyle(Theme.accentText)
            }
        }
    }

    private var currentFolder: String {
        info.folders.first { $0.id == info.folderId }?.label ?? info.folderPath
    }

    private func pick(_ folder: MusicFolderInfo) {
        guard folder.id != info.folderId else { return }
        pendingFolder = folder
        askMove = true
    }

    private var refreshText: String {
        guard let date = Format.date(iso: info.refreshedAt) else { return L10n.tr("music.lib.refresh.hint") }
        return L10n.format("music.lib.refresh.ago", Format.relative(date))
    }

    private var actionRows: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.large) {
            actionRow(title: L10n.tr("music.lib.rename"), hint: L10n.tr("music.lib.rename.hint"), symbol: "pencil") {
                sheet = .rename(store.makeRename())
            }
            actionRow(title: L10n.tr("music.lib.refresh"), hint: refreshText, symbol: "arrow.clockwise") {
                store.refreshDetails()
            }
            .disabled(info.working)
        }
    }

    private func actionRow(title: String, hint: String, symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: Theme.Spacing.medium) {
                Image(systemName: symbol)
                    .foregroundStyle(Theme.accentText)
                    .frame(width: 24)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                    caption(hint)
                }
                Spacer(minLength: 4)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var deleteButton: some View {
        Button(role: .destructive) { sheet = .remove(store.makeRemove()) } label: {
            Label(L10n.tr("music.lib.delete"), systemImage: "trash")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.bordered)
        .tint(Theme.fail)
    }
}
