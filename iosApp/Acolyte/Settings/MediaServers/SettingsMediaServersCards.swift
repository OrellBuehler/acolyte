import SwiftUI

struct SettingsMediaServersWatchSection: View {
    let info: SettingsMediaServersInfo
    let turnOn: () -> Void
    let askTurnOff: () -> Void

    var body: some View {
        Section(L10n.tr("settings.mediaservers.watch.title")) {
            SettingsMediaServersToggle(
                title: L10n.tr("settings.mediaservers.watch.read"),
                hint: L10n.tr("settings.mediaservers.watch.hint"),
                isOn: Binding(
                    get: { info.watchOn },
                    set: { on in
                        if on { turnOn() } else { askTurnOff() }
                    }
                )
            )
            .disabled(info.watchBusy)
        }
    }
}

struct SettingsMediaServersLabelsSection: View {
    let info: SettingsMediaServersInfo
    let store: SettingsMediaServersStore

    var body: some View {
        Section(L10n.tr("settings.mediaservers.labels.title")) {
            SettingsMediaServersToggle(
                title: L10n.tr("settings.mediaservers.labels.switch"),
                hint: L10n.tr("settings.mediaservers.labels.hint"),
                isOn: Binding(get: { info.labelsEnabled }, set: { store.setLabelsEnabled($0) })
            )
            if info.labelsEnabled {
                SettingsMediaServersField(
                    title: L10n.tr("settings.mediaservers.labels.text"),
                    value: info.labelsText,
                    revision: info.cardsRevision,
                    onChange: { store.setLabelText($0) },
                    maxLength: 64
                )
                SettingsMediaServersToggle(
                    title: L10n.tr("settings.mediaservers.labels.askers"),
                    isOn: Binding(get: { info.labelsAskers }, set: { store.setLabelAskers($0) })
                )
            }
            HStack {
                Button(L10n.tr("settings.mediaservers.save")) { store.saveLabels() }
                    .disabled(!info.labelsCanSave)
                if info.labelsSavedOn {
                    Button(L10n.tr("settings.mediaservers.labels.existing")) { store.runLabels() }
                        .disabled(!info.labelsCanRun)
                }
            }
            .buttonStyle(.bordered)
        }
    }
}

struct SettingsMediaServersTracksSection: View {
    let info: SettingsMediaServersInfo
    let store: SettingsMediaServersStore

    var body: some View {
        Section(L10n.tr("settings.mediaservers.tracks.title")) {
            SettingsMediaServersToggle(
                title: L10n.tr("settings.mediaservers.tracks.switch"),
                hint: L10n.tr("settings.mediaservers.tracks.hint"),
                isOn: Binding(get: { info.tracksEnabled }, set: { store.setTracksEnabled($0) })
            )
            if info.tracksEnabled {
                list(title: L10n.tr("settings.mediaservers.tracks.audio"), rows: info.audioRows, subtitles: false, canAdd: info.canAddAudio)
                list(title: L10n.tr("settings.mediaservers.tracks.subs"), rows: info.subtitleRows, subtitles: true, canAdd: info.canAddSubtitle)
                Text(L10n.tr("settings.mediaservers.tracks.server.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            Button(L10n.tr("settings.mediaservers.save")) { store.saveTracks() }
                .buttonStyle(.bordered)
                .disabled(!info.tracksCanSave)
        }
    }

    @ViewBuilder
    private func list(title: String, rows: [SettingsMediaServersTrackRowInfo], subtitles: Bool, canAdd: Bool) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(title)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Theme.textMuted)
            ForEach(rows) { row in
                trackRow(row, subtitles: subtitles)
            }
            if canAdd {
                Button(L10n.tr("settings.mediaservers.tracks.add"), systemImage: "plus") { store.addTrackLanguage(subtitles) }
                    .buttonStyle(.bordered)
            }
        }
    }

    private func trackRow(_ row: SettingsMediaServersTrackRowInfo, subtitles: Bool) -> some View {
        let name = SettingsMediaServersWords.language(code: row.code, fallback: row.name)
        return HStack {
            Text("\(row.index + 1)")
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .frame(width: 20, alignment: .trailing)
            Picker(
                L10n.format("settings.mediaservers.tracks.pick", row.index + 1),
                selection: Binding(get: { row.code }, set: { store.setTrackLanguage(subtitles, row.index, $0) })
            ) {
                ForEach(row.options) { option in
                    Text(SettingsMediaServersWords.language(code: option.code, fallback: option.name)).tag(option.code)
                }
            }
            .labelsHidden()
            Button {
                store.moveTrackLanguage(subtitles, row.index, up: true)
            } label: {
                Image(systemName: "arrow.up")
            }
            .buttonStyle(.borderless)
            .disabled(!row.canUp)
            .accessibilityLabel(L10n.format("settings.mediaservers.tracks.up", name))
            Button {
                store.moveTrackLanguage(subtitles, row.index, up: false)
            } label: {
                Image(systemName: "arrow.down")
            }
            .buttonStyle(.borderless)
            .disabled(!row.canDown)
            .accessibilityLabel(L10n.format("settings.mediaservers.tracks.down", name))
            Button(role: .destructive) {
                store.removeTrackLanguage(subtitles, row.index)
            } label: {
                Image(systemName: "xmark")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(L10n.format("settings.mediaservers.tracks.remove", name))
        }
    }
}
