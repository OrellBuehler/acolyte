import SwiftUI

struct SubtitleTranslatorSection: View {
    let info: SubtitleCfgInfo
    let store: SubtitleSettingsStore

    private var translator: SubtitleCfgTranslator { info.translator }

    var body: some View {
        Section {
            Picker(L10n.tr("acq.sub.tr.kind"), selection: Binding(get: { translator.kind }, set: { store.setTranslatorKind($0) })) {
                Text(L10n.tr("acq.sub.tr.off")).tag("")
                Text(L10n.tr("acq.sub.tr.openai")).tag("openai")
                Text(L10n.tr("acq.sub.tr.lingarr")).tag("lingarr")
            }
            if translator.kind == "openai" {
                hint(L10n.tr("acq.sub.tr.openai.hint"))
            } else if translator.kind == "lingarr" {
                hint(L10n.tr("acq.sub.tr.lingarr.hint"))
            }
            if !translator.kind.isEmpty { fields }
            if let ok = translator.testOk, !translator.dirty {
                HStack {
                    StatusChip(text: L10n.tr(ok ? "acq.sub.test.ok" : "acq.sub.test.failed"), tone: ok ? .ok : .fail)
                    if !translator.testMessage.isEmpty {
                        Text(translator.testMessage)
                            .font(.footnote)
                            .foregroundStyle(ok ? Theme.textMuted : Theme.failText)
                    }
                }
            }
            if let error = translator.error {
                Text(SubtitleCfgWords.error(error)).font(.footnote).foregroundStyle(Theme.failText)
            }
            if !translator.kind.isEmpty || translator.dirty { buttons }
        } header: {
            Text(L10n.tr("acq.sub.tr.title"))
        } footer: {
            Text(L10n.tr("acq.sub.tr.sub"))
        }
    }

    private func hint(_ text: String) -> some View {
        Text(text).font(.footnote).foregroundStyle(Theme.textMuted)
    }

    @ViewBuilder
    private var fields: some View {
        SubtitleTextRow(
            title: L10n.tr("acq.sub.tr.address"),
            text: translator.url,
            keyboard: .URL,
            prompt: translator.kind == "openai" ? "http://192.168.1.20:11434/v1" : "http://192.168.1.20:9876"
        ) { store.setTranslatorUrl($0) }
        if translator.kind == "openai" {
            SubtitleTextRow(title: L10n.tr("acq.sub.tr.model"), text: translator.model) { store.setTranslatorModel($0) }
            if !translator.models.isEmpty {
                Picker(L10n.tr("acq.sub.tr.model"), selection: Binding(get: { translator.model }, set: { store.setTranslatorModel($0) })) {
                    if !translator.models.contains(translator.model) { Text(translator.model).tag(translator.model) }
                    ForEach(translator.models, id: \.self) { model in Text(model).tag(model) }
                }
            }
        }
        SubtitleTextRow(
            title: L10n.tr("acq.sub.tr.key"),
            text: translator.key,
            secure: true,
            prompt: translator.hasKey ? "••••••••" : L10n.tr("acq.sub.tr.key.empty")
        ) { store.setTranslatorKey($0) }
        hint(L10n.tr(translator.hasKey ? "acq.sub.tr.key.kept" : "acq.sub.tr.key.hint"))
        if translator.hasKey && translator.key.isEmpty {
            Toggle(L10n.tr("acq.sub.tr.clear.key"), isOn: Binding(get: { translator.clearKey }, set: { store.setTranslatorClearKey($0) }))
        }
        SubtitleToggleRow(
            title: L10n.tr("acq.sub.tr.built.in"),
            hint: L10n.tr("acq.sub.tr.built.in.hint"),
            isOn: translator.builtInAuto
        ) { store.setTranslatorBuiltInAuto($0) }
    }

    private var buttons: some View {
        HStack {
            Button {
                store.saveTranslator()
            } label: {
                if translator.saving { ProgressView() } else { Text(L10n.tr("acq.sub.save")) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(!translator.canSave)
            if !translator.kind.isEmpty {
                Button {
                    store.testTranslator()
                } label: {
                    if translator.testing { ProgressView() } else { Text(L10n.tr("acq.sub.tr.test")) }
                }
                .buttonStyle(.bordered)
                .disabled(!translator.canTest)
            }
        }
    }
}
