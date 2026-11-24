import SwiftUI

struct SubtitleWhenSection: View {
    let info: SubtitleCfgInfo
    let store: SubtitleSettingsStore
    @State private var advanced = false

    private func number(_ key: String, _ title: String, hint: String? = nil) -> some View {
        SubtitleNumberRow(
            title: L10n.tr(title),
            key: key,
            text: info.numbers[key] ?? "",
            valid: info.numberValid[key] ?? true,
            hint: hint,
            decimal: key.hasSuffix("fps")
        ) { store.setNumber(key, $0) }
    }

    private func flag(_ key: String) -> Bool { info.flags[key] ?? false }

    var body: some View {
        Section {
            SubtitleToggleRow(title: L10n.tr("acq.sub.better"), hint: L10n.tr("acq.sub.better.hint"), isOn: flag("better")) {
                store.setFlag("better", $0)
            }
            DisclosureGroup(L10n.tr("acq.sub.advanced"), isExpanded: $advanced) {
                number("search_hours", "acq.sub.search.hours")
                number("batch", "acq.sub.batch", hint: L10n.tr("acq.sub.batch.hint"))
                SubtitleToggleRow(title: L10n.tr("acq.sub.slow.down"), hint: L10n.tr("acq.sub.slow.down.hint"), isOn: flag("adaptive")) {
                    store.setFlag("adaptive", $0)
                }
                if flag("better") {
                    number("better_hours", "acq.sub.every.hours")
                    number("better_days", "acq.sub.last.days")
                }
            }
        } header: {
            Text(L10n.tr("acq.sub.when.title"))
        } footer: {
            Text(L10n.tr("acq.sub.when.sub"))
        }
    }
}

struct SubtitleMoreSections: View {
    let info: SubtitleCfgInfo
    let store: SubtitleSettingsStore
    @State private var open = false
    @State private var insideAdvanced = false
    @State private var cleanAdvanced = false
    @State private var syncAdvanced = false

    private func flag(_ key: String) -> Bool { info.flags[key] ?? false }

    private func step(_ key: String) -> Bool { info.steps[key] ?? false }

    var body: some View {
        Section {
            Button {
                open.toggle()
            } label: {
                HStack {
                    Text(L10n.tr("acq.sub.more.options"))
                    Spacer()
                    Image(systemName: open ? "chevron.up" : "chevron.down").foregroundStyle(Theme.textMuted)
                }
            }
        }
        if open {
            insideSection
            cleanupSection
            syncSection
        }
    }

    private var insideSection: some View {
        Section {
            SubtitleToggleRow(
                title: L10n.tr("acq.sub.count.inside"),
                hint: L10n.tr("acq.sub.count.inside.hint"),
                isOn: flag("use_built_in")
            ) { store.setFlag("use_built_in", $0) }
            DisclosureGroup(L10n.tr("acq.sub.advanced"), isExpanded: $insideAdvanced) {
                SubtitleToggleRow(
                    title: L10n.tr("acq.sub.no.picture"),
                    hint: L10n.tr("acq.sub.no.picture.hint"),
                    isOn: flag("ignore_picture_tracks"),
                    enabled: flag("use_built_in")
                ) { store.setFlag("ignore_picture_tracks", $0) }
                Picker(L10n.tr("acq.sub.hi.suffix"), selection: Binding(get: { info.hiSuffix }, set: { store.setHiSuffix($0) })) {
                    Text(L10n.tr("acq.sub.suffix.hi")).tag("hi")
                    Text(L10n.tr("acq.sub.suffix.cc")).tag("cc")
                    Text(L10n.tr("acq.sub.suffix.sdh")).tag("sdh")
                }
            }
        } header: {
            Text(L10n.tr("acq.sub.inside.title"))
        }
    }

    private func cleanName(_ code: String) -> String {
        code == SubtitleCfgKeys.everyLanguage ? L10n.tr("acq.sub.clean.every") : info.languageName(code)
    }

    private func stepToggle(_ key: String, _ title: String, hint: String? = nil) -> some View {
        SubtitleToggleRow(title: L10n.tr(title), hint: hint, isOn: step(key)) { store.setStep(key, $0) }
    }

    private var cleanupSection: some View {
        Section {
            Picker(L10n.tr("acq.sub.clean.row"), selection: Binding(get: { info.cleanLanguage }, set: { store.pickCleanLanguage($0) })) {
                ForEach(info.cleanRows, id: \.self) { code in Text(cleanName(code)).tag(code) }
            }
            if !info.cleanAddable.isEmpty {
                Picker(L10n.tr("acq.sub.clean.add.lang"), selection: Binding(get: { "" }, set: { if !$0.isEmpty { store.pickCleanLanguage($0) } })) {
                    Text(L10n.tr("acq.sub.clean.add.lang")).tag("")
                    ForEach(info.cleanAddable) { language in Text(language.name).tag(language.code) }
                }
            }
            if info.cleanLanguage != SubtitleCfgKeys.everyLanguage {
                Button(L10n.tr("acq.sub.clean.remove.lang"), role: .destructive) { store.removeCleanLanguage() }
            }
            stepToggle("ads", "acq.sub.clean.ads", hint: L10n.tr("acq.sub.clean.ads.hint"))
            stepToggle("hi", "acq.sub.clean.hi", hint: L10n.tr("acq.sub.clean.hi.hint"))
            DisclosureGroup(L10n.tr("acq.sub.advanced"), isExpanded: $cleanAdvanced) {
                stepToggle("tags", "acq.sub.clean.tags")
                stepToggle("emoji", "acq.sub.clean.emoji")
                stepToggle("ocr", "acq.sub.clean.ocr", hint: L10n.tr("acq.sub.clean.ocr.hint"))
                stepToggle("caps", "acq.sub.clean.caps")
                stepToggle("spacing", "acq.sub.clean.spacing")
                stepToggle("overlap", "acq.sub.clean.overlap", hint: L10n.tr("acq.sub.clean.overlap.hint"))
            }
            if info.canClean {
                Button {
                    store.cleanAll()
                } label: {
                    if info.cleaning { ProgressView() } else { Text(L10n.tr("acq.sub.clean.all")) }
                }
                .disabled(info.cleaning)
            }
        } header: {
            Text(L10n.tr("acq.sub.clean.title"))
        } footer: {
            Text(L10n.tr("acq.sub.clean.sub") + " " + L10n.tr("acq.sub.clean.note"))
        }
    }

    private func syncNumber(_ key: String, _ title: String, decimal: Bool) -> some View {
        SubtitleNumberRow(
            title: L10n.tr(title),
            key: key,
            text: info.numbers[key] ?? "",
            valid: info.numberValid[key] ?? true,
            decimal: decimal
        ) { store.setNumber(key, $0) }
    }

    private var syncSection: some View {
        Section {
            DisclosureGroup(L10n.tr("acq.sub.advanced"), isExpanded: $syncAdvanced) {
                syncNumber("sync_step_ms", "acq.sub.shift", decimal: false)
                syncNumber("sync_from_fps", "acq.sub.fps.from", decimal: true)
                syncNumber("sync_to_fps", "acq.sub.fps.to", decimal: true)
            }
        } header: {
            Text(L10n.tr("acq.sub.sync.title"))
        } footer: {
            Text(L10n.tr("acq.sub.sync.sub") + ". " + L10n.tr("acq.sub.sync.note"))
        }
    }
}
