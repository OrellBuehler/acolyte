import SwiftUI

struct SettingsLookAppearanceScreen: View {
    @State private var store = SettingsLookAppearanceStore()
    @State private var savedText: String?
    @Environment(\.openRoute) private var openRoute

    private var info: SettingsLookAppearanceInfo { store.info }

    private var currentLanguage: String {
        info.language.isEmpty ? L10n.tr("settings.look.appearance.language.automatic") : SettingsLookWords.languageName(info.language)
    }

    var body: some View {
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            form
        }
        .navigationTitle(L10n.tr("settings.page.appearance"))
        .safeAreaInset(edge: .bottom) {
            if let savedText {
                NoteToast(text: savedText, undo: nil)
            }
        }
        .onChange(of: info.savedCount) { _, count in
            guard count > 0, let text = SettingsLookWords.saved(info.saved) else { return }
            savedText = text
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
            themeSection
            languageSection
            if info.showEveryone {
                everyoneSection
            }
            if info.canSeeStats {
                statsSection
            }
        }
    }

    private var themeSection: some View {
        Section {
            Text(L10n.tr("settings.look.appearance.theme.text"))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
        } header: {
            Text(L10n.tr("settings.look.appearance.theme.title"))
        }
    }

    private var languageSection: some View {
        Section {
            NavigationLink {
                SettingsLookLanguageView(store: store)
            } label: {
                LabeledContent(L10n.tr("settings.look.appearance.language.title"), value: currentLanguage)
            }
            .disabled(info.languageSaving)
            if let error = info.languageError {
                SettingsLookErrorText(error: error, prefixed: true)
            }
        } header: {
            Text(L10n.tr("settings.look.appearance.language.title"))
        } footer: {
            Text(L10n.tr("settings.look.appearance.language.sub"))
        }
    }

    private var everyoneSection: some View {
        Section {
            SettingsLookSwitchRow(
                title: L10n.tr("settings.look.appearance.autoplay"),
                hint: L10n.tr("settings.look.appearance.autoplay.hint"),
                isOn: Binding(get: { info.autoplayTrailers }, set: { store.setAutoplayTrailers($0) })
            )
            .disabled(info.everyoneSaving)
            SettingsLookSwitchRow(
                title: L10n.tr("settings.look.appearance.art"),
                hint: L10n.tr("settings.look.appearance.art.hint"),
                isOn: Binding(get: { info.signInArt }, set: { store.setSignInArt($0) })
            )
            .disabled(info.everyoneSaving)
            if let error = info.everyoneError {
                SettingsLookErrorText(error: error, prefixed: true)
            }
        } header: {
            Text(L10n.tr("settings.look.appearance.everyone.title"))
        } footer: {
            Text(L10n.tr("settings.look.appearance.everyone.sub"))
        }
    }

    private var statsSection: some View {
        Section {
            Button {
                openRoute(.exploreStats)
            } label: {
                Label(L10n.tr("settings.look.appearance.stats.open"), systemImage: "chart.bar")
            }
        } header: {
            Text(L10n.tr("discover.stats.title"))
        } footer: {
            Text(L10n.tr("settings.look.appearance.stats.text"))
        }
    }
}
