import SwiftUI

private enum SubtitleForm: Identifiable {
    case sync(SubtitleRowInfo)
    case turnDown(SubtitleRowInfo)
    case translate(SubtitleFileInfo, SubtitleRowInfo)

    var id: String {
        switch self {
        case let .sync(row): "sync-\(row.id)"
        case let .turnDown(row): "down-\(row.id)"
        case let .translate(file, row): "translate-\(file.fileId)-\(row.id)"
        }
    }
}

struct SubtitlesView: View {
    let store: SubtitlesStore
    @State private var form: SubtitleForm?

    var body: some View {
        Group {
            if let info = store.info {
                content(info)
            } else {
                ProgressView()
            }
        }
        .task { await store.watch() }
        .task {
            await store.driver.load()
            await store.driver.loadExtras()
        }
        .task { await store.driver.followLive() }
        .sheet(item: $form) { form in
            NavigationStack { formView(form) }
        }
    }

    private func content(_ info: SubtitlesInfo) -> some View {
        List {
            Section {
                if let error = info.load.error { NoticeBanner(text: error.localized) }
                Button(L10n.tr("title.subtitles.search")) { store.run { await $0.search() } }.disabled(info.busy == "search")
                if info.canCleanAll {
                    Button(L10n.tr("title.subtitles.clean.all")) { store.run { await $0.cleanAll() } }.disabled(info.busy == "clean")
                }
                HStack {
                    Text(L10n.format("title.subtitles.languages", wishLine(info.wish, info), fromText(info))).font(.callout)
                    Spacer()
                    if !info.editing { Button(L10n.tr("title.change")) { store.driver.startEdit() }.buttonStyle(.borderless) }
                }
            }
            if info.editing { editor(info) }
            if info.listMode {
                Section {
                    Text(info.missingCount > 0 ? L10n.plural("title.subtitles.missing.count", info.missingCount) : L10n.tr("title.subtitles.none.missing"))
                }
            } else if info.files.isEmpty && info.load.phase == .ready {
                Section { Text(L10n.tr("title.subtitles.no.file")) }
            } else {
                ForEach(info.files) { file in fileSection(file, info) }
            }
            if let choices = info.choices { choicesSection(choices, info) }
        }
        .navigationTitle(L10n.tr("title.subtitles"))
        .navigationBarTitleDisplayMode(.inline)
        .overlay { FeedbackToast(feedback: info.feedback) }
    }

    private func wishLine(_ wants: [SubtitleWantInfo], _ info: SubtitlesInfo) -> String {
        wants.isEmpty ? L10n.tr("title.subtitles.none") : wants.map { wantLabel($0, info) }.joined(separator: " · ")
    }

    private func wantLabel(_ want: SubtitleWantInfo, _ info: SubtitlesInfo) -> String {
        var parts = [info.languageName(want.language)]
        if want.hearingImpaired == "hi" { parts.append(L10n.tr("title.subtitles.hi.only")) }
        if want.hearingImpaired == "either" { parts.append(L10n.tr("title.subtitles.either")) }
        if want.forced { parts.append(L10n.tr("title.subtitles.forced.only")) }
        return parts.joined(separator: ", ")
    }

    private func fromText(_ info: SubtitlesInfo) -> String {
        switch info.wishFrom {
        case "episode": L10n.tr("title.subtitles.from.episode")
        case "version": L10n.tr("title.subtitles.from.version")
        case "title": info.movie ? L10n.tr("title.subtitles.from.movie") : L10n.tr("title.subtitles.from.show")
        default: L10n.tr("title.subtitles.from.default")
        }
    }

    private func hiText(_ value: String) -> String {
        switch value {
        case "hi": L10n.tr("title.subtitles.hi")
        case "either": L10n.tr("title.subtitles.hi.either")
        default: L10n.tr("title.subtitles.hi.normal")
        }
    }

    private func editor(_ info: SubtitlesInfo) -> some View {
        Section(L10n.tr("title.subtitles.which")) {
            Picker(L10n.tr("title.subtitles.which"), selection: Binding(get: { info.editOwn }, set: { store.driver.setEditOwn($0) })) {
                Text(info.itemId > 0 ? L10n.tr("title.subtitles.same.as.show") : L10n.tr("title.subtitles.default.languages")).tag(false)
                Text(L10n.tr("title.subtitles.own")).tag(true)
            }
            .pickerStyle(.segmented)
            Text(info.editOwn ? L10n.tr("title.subtitles.own.hint") : L10n.tr("title.subtitles.default.hint")).font(.footnote)
            if info.editOwn {
                ForEach(Array(info.editWants.enumerated()), id: \.offset) { index, want in
                    VStack(alignment: .leading) {
                        Picker(L10n.tr("title.language"), selection: Binding(
                            get: { want.language },
                            set: { value in
                                store.driver.updateWant(
                                    index,
                                    SubtitleWantInfo(language: value, hearingImpaired: want.hearingImpaired, forced: want.forced, translateFrom: want.translateFrom)
                                )
                            }
                        )) {
                            ForEach(info.languages) { Text($0.name).tag($0.code) }
                        }
                        Picker(L10n.tr("title.subtitles.hearing"), selection: Binding(
                            get: { want.hearingImpaired },
                            set: { value in
                                store.driver.updateWant(
                                    index,
                                    SubtitleWantInfo(language: want.language, hearingImpaired: value, forced: want.forced, translateFrom: want.translateFrom)
                                )
                            }
                        )) {
                            ForEach(["normal", "hi", "either"], id: \.self) { Text(hiText($0)).tag($0) }
                        }
                        if info.translator {
                            Picker(L10n.tr("title.subtitles.translate.from"), selection: Binding(
                                get: { want.translateFrom },
                                set: { value in
                                    store.driver.updateWant(
                                        index,
                                        SubtitleWantInfo(language: want.language, hearingImpaired: want.hearingImpaired, forced: want.forced, translateFrom: value)
                                    )
                                }
                            )) {
                                Text(L10n.tr("title.subtitles.no.translation")).tag("")
                                ForEach(info.languages) { Text($0.name).tag($0.code) }
                            }
                        }
                        Toggle(L10n.tr("title.subtitles.forced"), isOn: Binding(
                            get: { want.forced },
                            set: { value in
                                store.driver.updateWant(
                                    index,
                                    SubtitleWantInfo(language: want.language, hearingImpaired: want.hearingImpaired, forced: value, translateFrom: want.translateFrom)
                                )
                            }
                        ))
                        .tint(Theme.accent)
                        Button(L10n.tr("title.remove"), role: .destructive) { store.driver.removeWant(index) }.buttonStyle(.borderless)
                    }
                }
                if info.editWants.count < 10 {
                    Button(L10n.tr("title.subtitles.add.language")) { store.driver.addWant() }
                }
            }
            if let problem = info.editProblem { Text(TitleText.wish(problem)).font(.footnote).foregroundStyle(Theme.failText) }
            HStack {
                Button(L10n.tr("common.cancel")) { store.driver.cancelEdit() }.buttonStyle(.borderless)
                Spacer()
                Button(L10n.tr("title.subtitles.save")) { store.run { await $0.saveEdit() } }
                    .buttonStyle(.borderless)
                    .disabled(info.editProblem != nil || info.busy == "wish")
            }
        }
    }

    private func fileSection(_ file: SubtitleFileInfo, _ info: SubtitlesInfo) -> some View {
        let multi = Set(info.files.map(\.versionLabel)).count > 1
        let prefix = multi ? (file.versionLabel.isEmpty ? L10n.tr("title.main.copy") : file.versionLabel) + " · " : ""
        return Section(prefix + file.fileName) {
            if file.present.isEmpty { Text(L10n.tr("title.subtitles.none.yet")).font(.footnote) }
            ForEach(file.present) { row in
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: Theme.Spacing.xSmall) {
                        Text(info.languageName(row.language)).font(.callout)
                        StatusChip(text: kindText(row), tone: .idle)
                        if let made = madeText(row.made) { StatusChip(text: made, tone: .idle) }
                        if row.picture { StatusChip(text: L10n.tr("title.subtitles.picture"), tone: .idle) }
                        if let score = row.score { StatusChip(text: "\(score)/100", tone: .idle) }
                    }
                    Text(sourceText(row) + (row.savedAt.map { " · " + TitleText.date($0) } ?? "")).font(.footnote).foregroundStyle(Theme.textMuted)
                    if row.ours {
                        HStack {
                            Button(L10n.tr("title.subtitles.sync")) { form = .sync(row) }
                            if row.relativePath.lowercased().hasSuffix(".srt") {
                                Button(L10n.tr("title.subtitles.clean")) {
                                    let id = row.id
                                    store.run { await $0.clean(id) }
                                }
                            }
                            Button(L10n.tr("title.subtitles.turn.down"), role: .destructive) { form = .turnDown(row) }
                        }
                        .buttonStyle(.borderless)
                        .font(.footnote)
                    }
                }
            }
            ForEach(file.missing) { row in
                HStack {
                    Text(info.languageName(row.language) + ": " + missingText(row)).font(.footnote)
                    Spacer()
                    if info.translator && !(file.translatable[row.language] ?? []).isEmpty {
                        Button(L10n.tr("title.subtitles.translate")) { form = .translate(file, row) }.buttonStyle(.borderless)
                    }
                }
            }
            Button(L10n.tr("title.subtitles.choose.one")) {
                let id = file.fileId
                store.run { await $0.openChoices(id) }
            }
        }
    }

    private func choicesSection(_ choices: SubtitleChoicesInfo, _ info: SubtitlesInfo) -> some View {
        Section {
            HStack {
                Text(L10n.tr("title.subtitles.choose.one")).font(.headline)
                Spacer()
                Button(L10n.tr("common.cancel")) { store.driver.closeChoices() }.buttonStyle(.borderless)
            }
            if choices.loading {
                HStack {
                    ProgressView()
                    Text(L10n.tr("title.subtitles.asking"))
                }
            } else {
                if !choices.sitesAsked.isEmpty { Text(L10n.format("title.subtitles.asked", choices.sitesAsked.joined(separator: ", "))).font(.footnote) }
                ForEach(choices.outcomes) { outcome in
                    Text(wantLabel(outcome.want, info)).font(.subheadline.weight(.semibold))
                    if outcome.covered {
                        Text(
                            outcome.coveredBeside ? L10n.tr("title.subtitles.covered.beside")
                                : outcome.coveredPicture ? L10n.tr("title.subtitles.covered.picture") : L10n.tr("title.subtitles.covered.inside")
                        )
                        .font(.footnote)
                    } else if outcome.result == "translating" {
                        Text(L10n.tr("title.subtitles.translating.none")).font(.footnote)
                    } else if outcome.choices.isEmpty {
                        Text(L10n.tr("title.subtitles.nothing.found")).font(.footnote)
                    }
                    ForEach(outcome.choices) { candidate in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(candidate.name).font(.callout)
                            Text(candidateFacts(candidate)).font(.footnote).foregroundStyle(Theme.textMuted)
                            HStack {
                                StatusChip(text: "\(candidate.score)/100", tone: candidate.refused ? .fail : .idle)
                                Spacer()
                                Button(L10n.tr("title.subtitles.choose")) {
                                    let file = choices.fileId
                                    let site = candidate.site
                                    let key = candidate.key
                                    store.run { await $0.choose(file, site: site, key: key) }
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(Theme.accent)
                                .disabled(info.busy == "choose-\(candidate.key)")
                            }
                        }
                    }
                }
            }
        }
    }

    private func candidateFacts(_ c: SubtitleCandidateInfo) -> String {
        var out = [c.site]
        if c.machineMade { out.append(L10n.tr("title.subtitles.machine.made")) }
        if c.hearingImpaired { out.append(L10n.tr("title.subtitles.kind.hi")) }
        if c.forced { out.append(L10n.tr("title.subtitles.kind.forced")) }
        if c.trusted { out.append(L10n.tr("title.subtitles.trusted")) }
        if c.downloads > 0 { out.append(L10n.plural("title.subtitles.downloads", c.downloads)) }
        return out.joined(separator: " · ")
    }

    private func kindText(_ row: SubtitleRowInfo) -> String {
        if row.forced { return L10n.tr("title.subtitles.kind.forced") }
        if row.hearingImpaired { return L10n.tr("title.subtitles.kind.hi") }
        return L10n.tr("title.subtitles.kind.normal")
    }

    private func madeText(_ made: String) -> String? {
        switch made {
        case "translated": L10n.tr("title.subtitles.made.translated")
        case "translated_partial": L10n.tr("title.subtitles.made.partial")
        case "ai_translated": L10n.tr("title.subtitles.made.ai")
        case "machine_translated": L10n.tr("title.subtitles.made.machine")
        default: nil
        }
    }

    private func sourceText(_ row: SubtitleRowInfo) -> String {
        if row.place == "built_in" { return row.codec.isEmpty ? L10n.tr("title.subtitles.inside") : L10n.format("title.subtitles.inside.codec", row.codec) }
        if row.origin == "found" { return L10n.tr("title.subtitles.found") }
        if row.origin == "by_hand" { return row.site.isEmpty ? L10n.tr("title.subtitles.hand") : L10n.format("title.subtitles.hand.from", row.site) }
        return row.site.isEmpty ? L10n.tr("title.subtitles.saved") : L10n.format("title.subtitles.from.site", row.site)
    }

    private func missingText(_ row: SubtitleRowInfo) -> String {
        if row.state == "none_good_enough" {
            return L10n.plural("title.subtitles.none.good", row.tries) + (row.nextTryAt.map { " · " + L10n.format("title.subtitles.next.try", TitleText.date($0)) } ?? "")
        }
        return row.tries > 0 ? L10n.plural("title.subtitles.not.found", row.tries) : L10n.tr("title.subtitles.not.looked")
    }

    @ViewBuilder
    private func formView(_ form: SubtitleForm) -> some View {
        switch form {
        case let .sync(row): SyncForm(row: row, store: store)
        case let .turnDown(row): TurnDownForm(row: row, store: store)
        case let .translate(file, row): TranslateForm(file: file, want: row, store: store, label: { sourceText($0) })
        }
    }
}

private struct SyncForm: View {
    let row: SubtitleRowInfo
    let store: SubtitlesStore
    @State private var offset = 0
    @State private var fps = false
    @State private var from = "25"
    @State private var to = "23.976"
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Text(L10n.tr("title.subtitles.sync.intro")).font(.footnote)
            Stepper(value: $offset, in: -600_000 ... 600_000, step: 500) {
                Text(L10n.tr("title.subtitles.shift") + ": \(offset)")
            }
            Toggle(L10n.tr("title.subtitles.change.fps"), isOn: $fps).tint(Theme.accent)
            if fps {
                TextField(L10n.tr("title.subtitles.from.fps"), text: $from).keyboardType(.decimalPad)
                TextField(L10n.tr("title.subtitles.to.fps"), text: $to).keyboardType(.decimalPad)
            }
            if offset == 0 && !fps { Text(L10n.tr("title.subtitles.sync.needed")).font(.footnote).foregroundStyle(Theme.failText) }
            Button(L10n.tr("title.subtitles.fix")) {
                let id = row.id
                let shift = offset
                let fromValue = fps ? Double(from.replacingOccurrences(of: ",", with: ".")) : nil
                let toValue = fps ? Double(to.replacingOccurrences(of: ",", with: ".")) : nil
                store.run { await $0.sync(id, offset: shift, from: fromValue, to: toValue) }
                dismiss()
            }
            .disabled(offset == 0 && !fps)
        }
        .navigationTitle(L10n.tr("title.subtitles.sync"))
    }
}

private struct TurnDownForm: View {
    let row: SubtitleRowInfo
    let store: SubtitlesStore
    @State private var reason = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Text(L10n.tr("title.subtitles.turn.down.intro")).font(.footnote)
            TextField(L10n.tr("title.why.optional"), text: $reason, prompt: Text(L10n.tr("title.subtitles.why.placeholder")))
            Button(L10n.tr("title.subtitles.turn.it.down"), role: .destructive) {
                let id = row.id
                let text = String(reason.prefix(200))
                store.run { await $0.turnDown(id, reason: text) }
                dismiss()
            }
        }
        .navigationTitle(L10n.tr("title.subtitles.turn.down"))
    }
}

private struct TranslateForm: View {
    let file: SubtitleFileInfo
    let want: SubtitleRowInfo
    let store: SubtitlesStore
    let label: (SubtitleRowInfo) -> String
    @State private var picked: Int64 = 0
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let sources = file.translatable[want.language] ?? []
        Form {
            Picker(L10n.tr("title.subtitles.translate.from"), selection: $picked) {
                ForEach(sources) { source in
                    Text("\(store.info?.languageName(source.language) ?? source.language) · \(label(source))").tag(source.id)
                }
            }
            Button(L10n.tr("title.subtitles.translate")) {
                let fileId = file.fileId
                let wantId = want.id
                let fromId = picked
                store.run { await $0.translate(fileId, wantId: wantId, fromId: fromId) }
                dismiss()
            }
            .disabled(picked == 0)
        }
        .navigationTitle(L10n.tr("title.subtitles.translate"))
        .onAppear { picked = sources.first?.id ?? 0 }
    }
}
