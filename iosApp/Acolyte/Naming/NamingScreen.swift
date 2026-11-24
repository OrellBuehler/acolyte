import SwiftUI

struct NamingScreen: View {
    @State private var store = NamingStore()
    @FocusState private var focus: NamingField?
    @State private var lastField: NamingField?
    @State private var words: NamingField?
    @State private var toast: String?
    @State private var whereAdvanced = false
    @State private var movieAdvanced = false
    @State private var showAdvanced = false
    @State private var musicAdvanced = false

    private var info: NamingInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                EmptyStateView(title: L10n.tr("acq.nm.not.allowed"), message: L10n.tr("acq.nm.not.allowed.text"), systemImage: "lock")
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    form
                }
            }
        }
        .navigationTitle(L10n.tr("acq.page.naming"))
        .toolbar {
            if info.dirty {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.nm.discard")) { store.discard() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("acq.nm.save")) { store.save() }
                            .disabled(!info.canSave)
                    }
                }
            }
        }
        .navigationBarBackButtonHidden(info.dirty)
        .safeAreaInset(edge: .bottom) { bottom }
        .sheet(item: $words) { field in
            NamingWordsSheet(store: store, field: field)
        }
        .onChange(of: focus) { _, new in if let new { lastField = new } }
        .onChange(of: info.savedCount) { _, count in
            if count > 0 { show(L10n.tr("acq.nm.saved")) }
        }
        .onChange(of: info.cleanCount) { _, count in
            guard count > 0, let note = info.cleanNote else { return }
            switch note {
            case .removing:
                show(L10n.tr("acq.nm.clean.removing") + ". " + L10n.format("acq.nm.clean.older", info.cleanDays))
            case .nothing:
                show(L10n.tr("acq.nm.clean.nothing") + ". " + L10n.tr("acq.nm.clean.kept"))
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followExample() }
    }

    private func show(_ text: String) {
        toast = text
        Task {
            try? await Task.sleep(for: .seconds(3))
            toast = nil
        }
    }

    private func openWords(_ card: [NamingField], _ fallback: NamingField) {
        if let last = lastField, card.contains(last), last.insertable {
            words = last
        } else {
            words = fallback
        }
    }

    @ViewBuilder
    private var bottom: some View {
        VStack(spacing: Theme.Spacing.small) {
            if let toast {
                NoteToast(text: toast, undo: nil)
            }
            if let field = focus {
                NamingExampleCard(info: info, text: info.focusExamples[field.rawValue] ?? "")
                    .padding(.horizontal, Theme.Spacing.screen)
                    .padding(.bottom, Theme.Spacing.small)
            }
        }
    }

    private var form: some View {
        Form {
            if let error = info.saveError {
                Section {
                    Text(NamingText.error(error))
                        .foregroundStyle(Theme.failText)
                }
            }
            whereSection
            movieSection
            showSection
            musicSection
            importingSection
            holdingSection
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private func pattern(_ field: NamingField, _ hint: String?, enabled: Bool = true) -> some View {
        NamingPatternField(
            field: field,
            title: NamingText.fieldLabel(field),
            hint: hint,
            text: info.text(field.rawValue),
            enabled: enabled,
            focus: $focus
        ) { store.setText(field, $0) }
    }

    private func number(_ kind: NamingNumberKind, _ title: String, _ hint: String) -> some View {
        NamingNumberField(
            title: title,
            hint: hint,
            text: info.text(kind.rawValue),
            error: info.numberErrors[kind.rawValue].map { NamingText.numberError($0) } ?? ""
        ) { store.setNumber(kind, $0) }
    }

    private func toggle(_ wire: String, _ title: String, hint: String? = nil, enabled: Bool = true) -> some View {
        Toggle(isOn: Binding(get: { info.isOn(wire) }, set: { store.setSwitch(wire, $0) })) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                if let hint {
                    Text(hint)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
        .disabled(!enabled)
    }

    private func choice(
        _ wire: String,
        _ title: String,
        _ hint: String,
        _ options: [String],
        _ label: @escaping @MainActor (String) -> String
    ) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Picker(title, selection: Binding(get: { info.text(wire) }, set: { store.setChoice(wire, $0) })) {
                ForEach(options, id: \.self) { option in
                    Text(label(option)).tag(option)
                }
            }
            Text(hint)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private func exampleList(_ rows: [NamingRowInfo]) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("acq.nm.examples"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if info.exampleError != nil {
                Text(L10n.tr("acq.nm.see.error"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            } else {
                ForEach(rows) { row in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(NamingText.exampleLabel(row.key))
                            .font(.caption)
                            .foregroundStyle(Theme.textMuted)
                        Text(row.path)
                            .font(.system(.footnote, design: .monospaced))
                    }
                }
            }
        }
    }

    private func wordsButton(_ card: [NamingField], _ fallback: NamingField) -> some View {
        Button {
            openWords(card, fallback)
        } label: {
            Label(L10n.tr("acq.nm.words"), systemImage: "curlybraces")
        }
    }

    private var whereSection: some View {
        Section {
            number(.holdingDays, L10n.tr("acq.nm.holding.days"), L10n.tr("acq.nm.holding.days.hint"))
            toggle("unkeep_deleted_movies", L10n.tr("acq.nm.unkeep.movies"), hint: L10n.tr("acq.nm.unkeep.hint"))
            toggle("unkeep_deleted_episodes", L10n.tr("acq.nm.unkeep.episodes"), hint: L10n.tr("acq.nm.unkeep.hint"))
            DisclosureGroup(L10n.tr("acq.nm.advanced"), isExpanded: $whereAdvanced) {
                number(.adoptScanLimit, L10n.tr("acq.nm.scan.limit"), L10n.tr("acq.nm.scan.limit.hint"))
                number(.musicAdoptScanLimit, L10n.tr("acq.nm.music.scan.limit"), L10n.tr("acq.nm.music.scan.limit.hint"))
                number(.minFreeMb, L10n.tr("acq.nm.min.free"), L10n.tr("acq.nm.min.free.hint"))
                toggle("skip_free_space_check", L10n.tr("acq.nm.skip.check"), hint: L10n.tr("acq.nm.skip.check.hint"))
            }
        } header: {
            Text(L10n.tr("acq.nm.where"))
        }
    }

    private var movieSection: some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(L10n.tr("acq.nm.server.style"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                HStack(spacing: Theme.Spacing.small) {
                    Button(L10n.tr("acq.nm.style.plex")) { store.applyStyle("plex") }
                    Button(L10n.tr("acq.nm.style.jellyfin")) { store.applyStyle("jellyfin") }
                    Button(L10n.tr("acq.nm.style.emby")) { store.applyStyle("emby") }
                }
                .buttonStyle(.bordered)
                Text(L10n.tr("acq.nm.server.style.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            pattern(.filePattern, L10n.tr("acq.nm.file.pattern.hint"))
            pattern(.folderPattern, L10n.tr("acq.nm.folder.pattern.hint"))
            wordsButton(NamingCards.movie, .filePattern)
            NamingExampleCard(info: info, text: info.focusExamples[NamingField.filePattern.rawValue] ?? "")
            toggle(
                "group_by_genre",
                L10n.tr("acq.nm.group.genre"),
                hint: L10n.tr("acq.nm.group.genre.hint") + " " + L10n.tr("acq.nm.group.flat.note")
            )
            toggle("group_by_letter", L10n.tr("acq.nm.group.letter"), hint: L10n.tr("acq.nm.group.letter.hint"))
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Picker(L10n.tr("acq.nm.title.language"), selection: Binding(get: { info.text("title_language") }, set: { store.setChoice("title_language", $0) })) {
                    Text(L10n.tr("acq.nm.title.language.default")).tag("")
                    ForEach(info.languages) { language in
                        Text(language.name).tag(language.code)
                    }
                }
                Text(L10n.tr("acq.nm.title.language.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            DisclosureGroup(L10n.tr("acq.nm.advanced"), isExpanded: $movieAdvanced) {
                choice("colon", L10n.tr("acq.nm.colon"), L10n.tr("acq.nm.colon.hint"), NamingText.colonOptions) { NamingText.colonLabel($0) }
                choice("illegal", L10n.tr("acq.nm.illegal"), L10n.tr("acq.nm.illegal.hint"), NamingText.illegalOptions) { NamingText.illegalLabel($0) }
            }
        } header: {
            Text(L10n.tr("acq.nm.movie.names"))
        }
    }

    private var showSection: some View {
        Section {
            pattern(.showFolderPattern, L10n.tr("acq.nm.show.folder.hint"))
            toggle("show_group_by_letter", L10n.tr("acq.nm.group.letter"), hint: L10n.tr("acq.nm.show.group.letter.hint"))
            toggle("season_folders", L10n.tr("acq.nm.season.folders"))
            pattern(.seasonFolderPattern, L10n.tr("acq.nm.season.pattern.hint"), enabled: info.isOn("season_folders"))
            pattern(.episodePattern, L10n.tr("acq.nm.episode.pattern.hint"))
            wordsButton(NamingCards.show, .episodePattern)
            exampleList(info.episodeRows)
            DisclosureGroup(L10n.tr("acq.nm.advanced"), isExpanded: $showAdvanced) {
                showAdvancedFields
            }
        } header: {
            Text(L10n.tr("acq.nm.show.names"))
        }
    }

    @ViewBuilder
    private var showAdvancedFields: some View {
        choice("show_colon", L10n.tr("acq.nm.colon"), L10n.tr("acq.nm.colon.hint"), NamingText.colonOptions) { NamingText.colonLabel($0) }
        choice("show_illegal", L10n.tr("acq.nm.illegal"), L10n.tr("acq.nm.illegal.hint"), NamingText.illegalOptions) { NamingText.illegalLabel($0) }
        pattern(.specialsFolder, L10n.tr("acq.nm.specials.hint"), enabled: info.isOn("season_folders"))
        pattern(.dailyPattern, L10n.tr("acq.nm.daily.hint"))
        pattern(.animePattern, L10n.tr("acq.nm.anime.hint"))
        choice("multi_episode_style", L10n.tr("acq.nm.multi"), L10n.tr("acq.nm.multi.hint"), NamingText.multiOptions) { NamingText.multiLabel($0) }
        choice("air_date_style", L10n.tr("acq.nm.air.day"), L10n.tr("acq.nm.air.day.hint"), NamingText.dateOptions) { NamingText.dateLabel($0) }
        choice("absolute_digits", L10n.tr("acq.nm.digits"), L10n.tr("acq.nm.digits.hint"), NamingText.digitOptions) { NamingText.digitLabel($0) }
    }

    private var musicSection: some View {
        Section {
            pattern(.artistFolderPattern, L10n.tr("acq.nm.artist.folder.hint"))
            pattern(.albumFolderPattern, L10n.tr("acq.nm.album.folder.hint"))
            pattern(.trackPattern, L10n.tr("acq.nm.track.pattern.hint"))
            wordsButton(NamingCards.music, .trackPattern)
            exampleList(info.musicRows)
            DisclosureGroup(L10n.tr("acq.nm.advanced"), isExpanded: $musicAdvanced) {
                musicAdvancedFields
            }
        } header: {
            Text(L10n.tr("acq.nm.music.names"))
        }
    }

    @ViewBuilder
    private var musicAdvancedFields: some View {
        choice("music_colon", L10n.tr("acq.nm.colon"), L10n.tr("acq.nm.colon.hint"), NamingText.colonOptions) { NamingText.colonLabel($0) }
        choice("music_illegal", L10n.tr("acq.nm.illegal"), L10n.tr("acq.nm.illegal.hint"), NamingText.illegalOptions) { NamingText.illegalLabel($0) }
        pattern(.multiDiscTrackPattern, L10n.tr("acq.nm.multi.disc.hint"), enabled: !info.isOn("disc_folders"))
        toggle("disc_folders", L10n.tr("acq.nm.disc.folders"), hint: L10n.tr("acq.nm.disc.folders.hint"))
        pattern(.discFolderPattern, L10n.tr("acq.nm.disc.pattern.hint"), enabled: info.isOn("disc_folders"))
        toggle("music_read_tags", L10n.tr("acq.nm.read.tags"), hint: L10n.tr("acq.nm.read.tags.hint"))
    }

    private var importingSection: some View {
        Section {
            toggle("rename_on_import", L10n.tr("acq.nm.rename.movies"))
            toggle("show_rename_on_import", L10n.tr("acq.nm.rename.shows"))
            toggle("music_rename_on_import", L10n.tr("acq.nm.rename.music"))
        } header: {
            Text(L10n.tr("acq.nm.importing"))
        } footer: {
            Text(L10n.tr("acq.nm.rename") + ". " + L10n.tr("acq.nm.rename.hint"))
        }
    }

    private var holdingSection: some View {
        Section {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("acq.nm.clear.old"))
                    Text(L10n.tr("acq.nm.clear.old.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Spacer()
                if info.cleaning {
                    ProgressView()
                } else {
                    Button(L10n.tr("acq.nm.clear")) { store.cleanHolding() }
                        .buttonStyle(.bordered)
                        .disabled(!info.canClean)
                }
            }
            if let error = info.cleanError {
                Text(L10n.tr("acq.nm.clean.failed") + ". " + NamingText.error(error))
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
            }
        } header: {
            Text(L10n.tr("acq.nm.holding"))
        }
    }
}
