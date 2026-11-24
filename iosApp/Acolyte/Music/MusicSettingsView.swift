import SwiftUI
import UIKit

struct MusicSettingsView: View {
    @State private var store = MusicSettingsStore()
    @State private var advanced = false
    @State private var brainz = false
    @State private var toasts = DlToastModel()

    private var info: MusicSettingsInfo { store.info }

    private var editing: Bool { info.canChange && info.dirty }

    var body: some View {
        StatusContent(load: info.load, isEmpty: false, reload: { await store.load() }) {
            EmptyView()
        } content: {
            MusicSettingsForm(info: info, store: store, advanced: $advanced, brainz: $brainz)
        }
        .navigationTitle(L10n.tr("music.settings.title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbar }
        .navigationBarBackButtonHidden(editing)
        .overlay(alignment: .bottom) { DlToastHost(model: toasts).padding(.bottom, Theme.Spacing.large) }
        .onChange(of: info.justSaved) { _, saved in
            if saved { toasts.show(L10n.tr("music.note.settings.saved"), tone: .ok) }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        if editing {
            ToolbarItem(placement: .cancellationAction) {
                Button(L10n.tr("requests.discard")) { store.discard() }
            }
            ToolbarItem(placement: .confirmationAction) {
                if info.saving {
                    ProgressView()
                } else {
                    Button(L10n.tr("requests.save")) { Task { await store.save() } }
                        .disabled(!info.canSave)
                }
            }
        }
    }
}

struct MusicSettingsForm: View {
    let info: MusicSettingsInfo
    let store: MusicSettingsStore
    @Binding var advanced: Bool
    @Binding var brainz: Bool
    private let options = MusicOptionCatalog.lists

    var body: some View {
        Form {
            problemsSection
            artistHeaderSection
            choiceSections
            MusicOptionChecklistSection(
                title: L10n.tr("music.settings.kinds"),
                group: .albumType,
                options: options.albumTypes,
                selected: info.albumTypes,
                hint: L10n.tr("music.settings.kinds.hint"),
                enabled: info.canChange
            ) { store.toggleAlbumType($0, on: $1) }
            advancedToggle(L10n.tr("music.settings.advanced"), isOn: $advanced)
            if advanced { advancedSections }
            brainzHeaderSection
            advancedToggle(L10n.tr("music.settings.advanced"), isOn: $brainz)
            if brainz { brainzFields }
        }
    }

    @ViewBuilder
    private var problemsSection: some View {
        if !info.canChange || hasProblems {
            Section {
                if !info.canChange {
                    NoticeBanner(text: L10n.tr("music.settings.cant"))
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                }
                if let error = info.error {
                    Text(error.localized).foregroundStyle(Theme.failText)
                }
                if info.emptyKinds { problem("music.settings.need.kinds") }
                if info.emptyExtra { problem("music.settings.need.extra") }
                if info.emptyStatuses { problem("music.settings.need.statuses") }
                if !info.hoursValid { problem("music.settings.hours.invalid") }
            }
        }
    }

    private var hasProblems: Bool {
        info.error != nil || info.emptyKinds || info.emptyExtra || info.emptyStatuses || !info.hoursValid
    }

    private func problem(_ key: String) -> some View {
        Text(L10n.tr(key)).font(.callout).foregroundStyle(Theme.failText)
    }

    private var artistHeaderSection: some View {
        Section {
            Text(L10n.tr("music.settings.new.artist.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
        } header: {
            Text(L10n.tr("music.settings.new.artist"))
        }
    }

    @ViewBuilder
    private var choiceSections: some View {
        MusicOptionPickerSection(
            title: L10n.tr("music.settings.choice"),
            group: .choice,
            options: options.albumChoices,
            selected: info.albumChoice,
            enabled: info.canChange
        ) { store.setAlbumChoice($0) }
        MusicOptionPickerSection(
            title: L10n.tr("music.settings.new"),
            group: .newAlbums,
            options: options.newAlbums,
            selected: info.newAlbums,
            hint: L10n.tr("music.settings.new.hint"),
            enabled: info.canChange
        ) { store.setNewAlbums($0) }
    }

    private func advancedToggle(_ title: String, isOn: Binding<Bool>) -> some View {
        Section {
            Button {
                isOn.wrappedValue.toggle()
            } label: {
                HStack {
                    Text(title).foregroundStyle(Theme.text)
                    Spacer()
                    Image(systemName: isOn.wrappedValue ? "chevron.up" : "chevron.down")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    @ViewBuilder
    private var advancedSections: some View {
        MusicOptionChecklistSection(
            title: L10n.tr("music.settings.extra"),
            group: .extra,
            options: options.secondaryTypes,
            selected: info.secondaryTypes,
            hint: L10n.tr("music.settings.extra.hint"),
            enabled: info.canChange
        ) { store.toggleSecondary($0, on: $1) }
        MusicOptionChecklistSection(
            title: L10n.tr("music.settings.statuses"),
            group: .status,
            options: options.statuses,
            selected: info.statuses,
            enabled: info.canChange
        ) { store.toggleStatus($0, on: $1) }
        MusicOptionPickerSection(
            title: L10n.tr("music.settings.rule"),
            group: .rule,
            options: options.pressingRules,
            selected: info.pressingRule,
            hint: L10n.tr("music.settings.rule.hint"),
            enabled: info.canChange
        ) { store.setPressingRule($0) }
    }

    private var brainzHeaderSection: some View {
        Section {
            Text(L10n.tr("music.settings.brainz.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
        } header: {
            Text(L10n.tr("music.settings.brainz"))
        }
    }

    @ViewBuilder
    private var brainzFields: some View {
        Section {
            MusicSettingsField(
                title: L10n.tr("music.settings.url"),
                prompt: "https://musicbrainz.org",
                text: info.url,
                keyboard: .URL,
                enabled: info.canChange
            ) { store.setUrl($0) }
        } footer: {
            Text(L10n.tr("music.settings.url.hint"))
        }
        Section {
            MusicSettingsField(
                title: L10n.tr("music.settings.hours"),
                prompt: "24",
                text: info.hours,
                keyboard: .numberPad,
                invalid: !info.hoursValid,
                enabled: info.canChange
            ) { store.setHours($0) }
        }
    }
}

struct MusicSettingsField: View {
    let title: String
    let prompt: String
    let text: String
    var keyboard: UIKeyboardType = .default
    var invalid = false
    var enabled = true
    let onChange: @MainActor (String) -> Void
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title).font(.footnote).foregroundStyle(Theme.textMuted)
            TextField(title, text: $value, prompt: Text(prompt))
                .keyboardType(keyboard)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .foregroundStyle(invalid ? Theme.failText : Theme.text)
                .disabled(!enabled)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in
            if new != value { value = new }
        }
        .onChange(of: value) { _, new in
            if new != text { onChange(new) }
        }
    }
}
