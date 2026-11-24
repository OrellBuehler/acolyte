import SwiftUI

struct SettingsLookDefaultsScreen: View {
    @State private var store = SettingsLookDefaultsStore()
    @State private var movieAdvanced = false
    @State private var showAdvanced = false
    @State private var savedText: String?
    @Environment(\.openRoute) private var openRoute

    private var info: SettingsLookDefaultsInfo { store.info }

    var body: some View {
        StatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(
                title: L10n.tr("settings.look.defaults.empty"),
                message: L10n.tr("settings.look.defaults.empty.text"),
                systemImage: "film"
            )
        } content: {
            form
        }
        .navigationTitle(L10n.tr("settings.page.title.defaults"))
        .settingsLookLeaveGuard(editing: info.editing)
        .safeAreaInset(edge: .bottom) {
            if info.editing {
                SettingsLookSaveBar(
                    canSave: info.canSave,
                    saving: info.saving,
                    error: info.saveError,
                    problem: info.gateDaysValid ? nil : L10n.tr("settings.look.defaults.gate.days.invalid"),
                    discard: { store.discard() },
                    save: { store.save() }
                )
            } else if let savedText {
                NoteToast(text: savedText, undo: nil)
            }
        }
        .onChange(of: info.savedCount) { _, count in
            guard count > 0 else { return }
            savedText = L10n.tr("settings.look.defaults.saved")
            Task {
                try? await Task.sleep(for: .seconds(3))
                savedText = nil
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var form: some View {
        Form {
            if !info.canEdit {
                Section {
                    Text(L10n.tr("settings.look.defaults.read.only"))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            if let error = info.readError {
                Section { NoticeBanner(text: error.localized) }
            }
            if info.showMovies {
                moviesSection
            }
            if info.showShows {
                showsSection
            }
            footerSection
        }
    }

    private nonisolated static func offered(_ id: Int64, in options: [SettingsLookOptionInfo]) -> Int64 {
        options.contains { $0.id == id } ? id : 0
    }

    private func folderPicker(show: Bool) -> some View {
        let options = show ? info.showFolders : info.movieFolders
        let current = show ? info.showFolder : info.movieFolder
        let first = L10n.tr(show ? "settings.look.defaults.first.show" : "settings.look.defaults.first.movie")
        return Picker(
            L10n.tr("settings.look.defaults.folder"),
            selection: Binding(get: { Self.offered(current, in: options) }, set: { store.setFolder(show: show, $0) })
        ) {
            Text(first).tag(Int64(0))
            ForEach(options) { option in Text(option.name).tag(option.id) }
        }
        .disabled(!info.canEdit)
    }

    private func rulesPicker(show: Bool) -> some View {
        let options = show ? info.showRules : info.movieRules
        let current = show ? info.showRulesId : info.movieRulesId
        return Picker(
            L10n.tr("settings.look.defaults.quality"),
            selection: Binding(get: { Self.offered(current, in: options) }, set: { store.setRules(show: show, $0) })
        ) {
            Text(L10n.tr("settings.look.defaults.folder.quality")).tag(Int64(0))
            ForEach(options) { option in Text(option.name).tag(option.id) }
        }
        .disabled(!info.canEdit)
    }

    private var moviesSection: some View {
        Section {
            folderPicker(show: false)
            rulesPicker(show: false)
            SettingsLookSwitchRow(
                title: L10n.tr("settings.look.defaults.keep"),
                hint: L10n.tr("settings.look.defaults.keep.hint"),
                isOn: Binding(get: { info.movieKeep }, set: { store.setKeepUpToDate(show: false, $0) })
            )
            .disabled(!info.canEdit)
            SettingsLookSwitchRow(
                title: L10n.tr("settings.look.defaults.search"),
                isOn: Binding(get: { info.movieSearch }, set: { store.setSearchOnAdd(show: false, $0) })
            )
            .disabled(!info.canEdit)
            if info.showGate {
                DisclosureGroup(L10n.tr("settings.look.advanced"), isExpanded: $movieAdvanced) {
                    gateRows
                }
            }
        } header: {
            Text(L10n.tr("settings.look.defaults.movies"))
        }
    }

    @ViewBuilder
    private var gateRows: some View {
        Picker(
            L10n.tr("settings.look.defaults.gate"),
            selection: Binding(get: { info.gate }, set: { store.setReleaseGate($0) })
        ) {
            ForEach(info.gateChoices, id: \.self) { value in Text(SettingsLookWords.gate(value)).tag(value) }
        }
        .disabled(!info.canEdit)
        if let hint = SettingsLookWords.gateHint(info.gate) {
            SettingsLookHint(hint)
        }
        SettingsLookNumberField(
            title: L10n.tr("settings.look.defaults.gate.days"),
            text: info.gateDays,
            invalid: !info.gateDaysValid,
            onChange: { store.setGateDays($0) }
        )
        .disabled(!info.canEdit)
        SettingsLookHint(L10n.tr("settings.look.defaults.gate.days.hint"))
    }

    private var showsSection: some View {
        Section {
            folderPicker(show: true)
            rulesPicker(show: true)
            SettingsLookSwitchRow(
                title: L10n.tr("settings.look.defaults.keep"),
                isOn: Binding(get: { info.showKeep }, set: { store.setKeepUpToDate(show: true, $0) })
            )
            .disabled(!info.canEdit)
            SettingsLookSwitchRow(
                title: L10n.tr("settings.look.defaults.search"),
                hint: L10n.tr("settings.look.defaults.search.hint"),
                isOn: Binding(get: { info.showSearch }, set: { store.setSearchOnAdd(show: true, $0) })
            )
            .disabled(!info.canEdit)
            if info.showShowSettings {
                DisclosureGroup(L10n.tr("settings.look.advanced"), isExpanded: $showAdvanced) {
                    showRows
                }
            }
        } header: {
            Text(L10n.tr("settings.look.defaults.shows"))
        }
    }

    @ViewBuilder
    private var showRows: some View {
        Picker(
            L10n.tr("settings.look.defaults.seasons"),
            selection: Binding(get: { info.seasonChoice }, set: { store.setSeasonChoice($0) })
        ) {
            ForEach(info.seasonChoices, id: \.self) { value in Text(SettingsLookWords.season(value)).tag(value) }
        }
        .disabled(!info.canEdit)
        if let hint = SettingsLookWords.seasonHint(info.seasonChoice) {
            SettingsLookHint(hint)
        }
        Picker(
            L10n.tr("settings.look.defaults.new.seasons"),
            selection: Binding(get: { info.laterSeasons }, set: { store.setNewSeasons($0) })
        ) {
            ForEach(info.laterSeasonChoices, id: \.self) { value in Text(SettingsLookWords.laterSeasons(value)).tag(value) }
        }
        .disabled(!info.canEdit)
        Picker(
            L10n.tr("settings.look.defaults.order"),
            selection: Binding(get: { info.episodeOrder }, set: { store.setEpisodeOrder($0) })
        ) {
            ForEach(info.orderChoices, id: \.self) { value in Text(SettingsLookWords.order(value)).tag(value) }
        }
        .disabled(!info.canEdit)
        if let hint = SettingsLookWords.orderHint(info.episodeOrder) {
            SettingsLookHint(hint)
        }
        Picker(
            L10n.tr("settings.look.defaults.details"),
            selection: Binding(get: { info.detailsSource }, set: { store.setDetailsSource($0) })
        ) {
            ForEach(info.sourceChoices, id: \.self) { value in Text(SettingsLookWords.source(value)).tag(value) }
        }
        .disabled(!info.canEdit)
    }

    private var footerSection: some View {
        Section {
            if info.canOpenFolders {
                Button(L10n.tr("settings.page.library.folders")) { openRoute(.settingsLibraryFolders) }
            }
            if info.canOpenRequests {
                Button(L10n.tr("settings.page.requests")) { openRoute(.settingsRequests) }
            }
            if info.canOpenOutsideLists {
                Button(L10n.tr("settings.page.outside.lists")) { openRoute(.settingsOutsideLists) }
            }
        } footer: {
            Text(L10n.tr("settings.look.defaults.footer"))
        }
    }
}
