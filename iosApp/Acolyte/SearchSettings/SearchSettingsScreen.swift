import SwiftUI

struct SearchSettingsNumberField: View {
    let title: String
    let hint: String
    let text: String
    let error: String
    let onChange: @MainActor (String) -> Void
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            LabeledContent(title) {
                TextField(title, text: $value)
                    .keyboardType(.numbersAndPunctuation)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: 120)
            }
            Text(error.isEmpty ? hint : error)
                .font(.footnote)
                .foregroundStyle(error.isEmpty ? Theme.textMuted : Theme.failText)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }
}

struct SearchSettingsScreen: View {
    @State private var store = SearchSettingsStore()
    @State private var toast: String?
    @State private var nightlyAdvanced = false
    @State private var earlyAdvanced = false
    @State private var packsAdvanced = false
    @State private var musicAdvanced = false

    private var info: SearchSettingsInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                EmptyStateView(title: L10n.tr("acq.se.not.allowed"), message: L10n.tr("acq.se.not.allowed.text"), systemImage: "lock")
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    form
                }
            }
        }
        .navigationTitle(L10n.tr("acq.page.search"))
        .toolbar {
            if info.dirty {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.se.discard")) { store.discard() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("acq.se.save")) { store.save() }
                            .disabled(!info.canSave)
                    }
                }
            }
            if info.canRun, info.load.phase == .ready, info.allowed {
                ToolbarItem(placement: .topBarTrailing) {
                    runMenu
                }
            }
        }
        .navigationBarBackButtonHidden(info.dirty)
        .safeAreaInset(edge: .bottom) {
            if let toast {
                NoteToast(text: toast, undo: nil)
            }
        }
        .onChange(of: info.savedCount) { _, count in
            if count > 0 { show(L10n.tr("acq.se.saved")) }
        }
        .onChange(of: info.noteCount) { _, count in
            guard count > 0, let note = info.note else { return }
            switch note {
            case .feeds: show(L10n.tr("acq.se.checking"))
            case .better: show(L10n.tr("acq.se.looking"))
            }
        }
        .onChange(of: info.runErrorCount) { _, count in
            guard count > 0, let error = info.runError else { return }
            show(L10n.tr("acq.se.couldnt") + ". " + NamingText.error(error))
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var runMenu: some View {
        Menu {
            Button(L10n.tr("acq.se.check.feeds")) { store.checkFeeds() }
                .disabled(info.running != nil)
            Button(L10n.tr("acq.se.find.better")) { store.findBetter() }
                .disabled(info.running != nil)
        } label: {
            if info.running != nil {
                ProgressView()
            } else {
                Image(systemName: "ellipsis.circle")
            }
        }
        .accessibilityLabel(L10n.tr("acq.se.more"))
    }

    private func show(_ text: String) {
        toast = text
        Task {
            try? await Task.sleep(for: .seconds(3))
            toast = nil
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
            nightlySection
            earlySection
            packsSection
            musicSection
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private func number(_ wire: String, _ title: String, _ hint: String) -> some View {
        let key = info.numberErrors[wire] ?? ""
        let range = info.ranges[wire] ?? [0, 0]
        let message: String
        if key == "feed" {
            message = L10n.tr("acq.se.err.feed")
        } else if key.isEmpty {
            message = ""
        } else {
            message = L10n.format("acq.se.err.range", range.first ?? 0, range.last ?? 0)
        }
        return SearchSettingsNumberField(title: title, hint: hint, text: info.text(wire), error: message) { store.setNumber(wire, $0) }
    }

    private func toggle(_ wire: String, _ title: String, hint: String? = nil) -> some View {
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
    }

    private var nightlySection: some View {
        Section {
            number("missing_search_count", L10n.tr("acq.se.count"), L10n.tr("acq.se.count.hint"))
            toggle("missing_search_better", L10n.tr("acq.se.better.nightly"), hint: L10n.tr("acq.se.better.nightly.hint"))
            toggle("missing_search_shows", L10n.tr("acq.se.episodes"), hint: L10n.tr("acq.se.episodes.hint"))
            DisclosureGroup(L10n.tr("acq.se.advanced"), isExpanded: $nightlyAdvanced) {
                number("feed_check_minutes", L10n.tr("acq.se.feed.minutes"), L10n.tr("acq.se.feed.minutes.hint"))
            }
        } header: {
            Text(L10n.tr("acq.se.nightly"))
        }
    }

    private var earlySection: some View {
        Section {
            toggle("refuse_early_releases", L10n.tr("acq.se.refuse.early"), hint: L10n.tr("acq.se.refuse.early.hint"))
            toggle("refuse_tiny_releases", L10n.tr("acq.se.refuse.tiny"), hint: L10n.tr("acq.se.refuse.tiny.hint"))
            toggle("import_size_floors", L10n.tr("acq.se.floors"), hint: L10n.tr("acq.se.floors.hint"))
            number("import_min_video_mb", L10n.tr("acq.se.min.video"), L10n.tr("acq.se.min.video.hint"))
            number("import_min_track_kb", L10n.tr("acq.se.min.track"), L10n.tr("acq.se.min.track.hint"))
            number("episode_default_minutes", L10n.tr("acq.se.episode.minutes"), L10n.tr("acq.se.episode.minutes.hint"))
            toggle("hold_unknown_releases", L10n.tr("acq.se.hold"), hint: L10n.tr("acq.se.hold.hint"))
            DisclosureGroup(L10n.tr("acq.se.advanced"), isExpanded: $earlyAdvanced) {
                number("early_release_grace_hours", L10n.tr("acq.se.grace"), L10n.tr("acq.se.grace.hint"))
            }
        } header: {
            Text(L10n.tr("acq.se.early"))
        } footer: {
            Text(L10n.tr("acq.se.early.desc") + ". " + L10n.tr("acq.se.program.note"))
        }
    }

    private var packsSection: some View {
        Section {
            Picker(L10n.tr("acq.se.pack.upgrade"), selection: Binding(get: { info.text("season_pack_upgrade") }, set: { store.setChoice("season_pack_upgrade", $0) })) {
                Text(L10n.tr("acq.se.pack.all")).tag("all")
                Text(L10n.tr("acq.se.pack.any")).tag("any")
            }
            Picker(L10n.tr("acq.se.pack.prefer"), selection: Binding(get: { info.text("season_pack_prefer") }, set: { store.setChoice("season_pack_prefer", $0) })) {
                Text(L10n.tr("acq.se.prefer.rank")).tag("rank")
                Text(L10n.tr("acq.se.prefer.always")).tag("always")
            }
            Text(L10n.tr("acq.se.pack.prefer.hint"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            DisclosureGroup(L10n.tr("acq.se.advanced"), isExpanded: $packsAdvanced) {
                toggle("multi_season_packs", L10n.tr("acq.se.multi"), hint: L10n.tr("acq.se.multi.hint"))
            }
        } header: {
            Text(L10n.tr("acq.se.packs"))
        }
    }

    private var musicSection: some View {
        Section {
            toggle("missing_search_music", L10n.tr("acq.se.albums"), hint: L10n.tr("acq.se.albums.hint"))
            number("album_track_tolerance", L10n.tr("acq.se.tolerance"), L10n.tr("acq.se.tolerance.hint"))
            DisclosureGroup(L10n.tr("acq.se.advanced"), isExpanded: $musicAdvanced) {
                toggle("music_discography_packs", L10n.tr("acq.se.discographies"))
                number("album_extra_files", L10n.tr("acq.se.extra.files"), L10n.tr("acq.se.extra.files.hint"))
                number("artist_search_cap", L10n.tr("acq.se.search.cap"), L10n.tr("acq.se.search.cap.hint"))
            }
        } header: {
            Text(L10n.tr("acq.se.music"))
        }
    }
}
