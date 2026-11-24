import SwiftUI

struct SubtitleLanguagesSections: View {
    let info: SubtitleCfgInfo
    let store: SubtitleSettingsStore
    @State private var advanced = false
    @State private var means = false

    var body: some View {
        Section {
            if info.wants.isEmpty {
                Text(L10n.tr("acq.sub.none.yet")).foregroundStyle(Theme.textMuted)
            }
        } header: {
            Text(L10n.tr("acq.sub.langs.title"))
        } footer: {
            Text(L10n.tr("acq.sub.langs.sub"))
        }
        ForEach(info.wants) { want in
            wantSection(want)
        }
        Section {
            Button(L10n.tr("acq.sub.add.language")) { store.addLanguage() }
                .disabled(!info.canAddLanguage)
            if let problem = info.wishProblem {
                Text(SubtitleCfgWords.problem(problem)).font(.footnote).foregroundStyle(Theme.failText)
            }
            DisclosureGroup(L10n.tr("acq.sub.advanced"), isExpanded: $advanced) {
                passMark("pass_mark_episode", "acq.sub.pass.episode")
                passMark("pass_mark_movie", "acq.sub.pass.movie")
                DisclosureGroup(L10n.tr("acq.sub.pass.means"), isExpanded: $means) {
                    Text(L10n.tr("acq.sub.pass.text1")).font(.footnote).foregroundStyle(Theme.textMuted)
                    Text(L10n.tr("acq.sub.pass.text2")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
        } footer: {
            if !info.wants.isEmpty { Text(L10n.tr("acq.sub.hi.note")) }
        }
    }

    private func passMark(_ key: String, _ title: String) -> some View {
        SubtitleNumberRow(
            title: L10n.tr(title),
            key: key,
            text: info.numbers[key] ?? "",
            valid: info.numberValid[key] ?? true,
            hint: L10n.tr("acq.sub.pass.hint")
        ) { store.setNumber(key, $0) }
    }

    private func languageOptions(_ current: String) -> [SubtitleCfgLanguage] {
        if current.isEmpty || info.languages.contains(where: { $0.code == current }) { return info.languages }
        return [SubtitleCfgLanguage(code: current, name: current)] + info.languages
    }

    private func wantSection(_ want: SubtitleCfgWant) -> some View {
        let index = want.index
        return Section {
            Picker(L10n.tr("acq.sub.lang.language"), selection: Binding(get: { want.language }, set: { store.setWantLanguage(index, $0) })) {
                Text(L10n.tr("acq.sub.lang.pick")).tag("")
                ForEach(languageOptions(want.language)) { language in Text(language.name).tag(language.code) }
            }
            Picker(L10n.tr("acq.sub.lang.type"), selection: Binding(get: { want.type }, set: { store.setWantType(index, $0) })) {
                ForEach(SubtitleCfgKeys.hearingTypes, id: \.self) { type in Text(SubtitleCfgWords.type(type)).tag(type) }
            }
            if info.translatorReady {
                Picker(
                    L10n.tr("acq.sub.translate.from"),
                    selection: Binding(get: { want.translateFrom }, set: { store.setWantTranslateFrom(index, $0) })
                ) {
                    Text(L10n.tr("acq.sub.dont.translate")).tag("")
                    ForEach(info.languages.filter { $0.code != want.language }) { language in Text(language.name).tag(language.code) }
                }
            }
            Toggle(L10n.tr("acq.sub.forced"), isOn: Binding(get: { want.forced }, set: { store.setWantForced(index, $0) }))
            Button(L10n.format("acq.sub.take.off", info.languageName(want.language)), role: .destructive) {
                store.removeLanguage(index)
            }
        }
    }
}
