import SwiftUI

struct TitleSheetView: View {
    let sheet: TitleSheet
    let store: TitleStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        content
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("title.close")) { dismiss() }
                }
            }
            .navigationDestination(for: Route.self) { RouteScreen(route: $0) }
    }

    @ViewBuilder
    private var content: some View {
        switch sheet {
        case .cast: CastSheet(info: store.info)
        case let .cards(title, cards): CardsSheet(title: title, cards: cards, manage: store.info.access.manage)
        case .history: HistorySheet(info: store.info)
        case .hide: HideSheet(store: store)
        case .gate: GateSheet(store: store)
        case let .releases(releases): ReleasesView(store: releases, title: store)
        case let .add(add): AddTitleView(store: add, title: store)
        case let .episode(episode): EpisodeSheetView(store: episode, title: store)
        case let .seasonTools(season): SeasonToolsView(season: season, store: store)
        case let .subtitles(subtitles): SubtitlesView(store: subtitles)
        case let .leftovers(leftovers): LeftoversView(store: leftovers, title: store)
        case let .rename(rename): List { RenameSection(store: rename) }.navigationTitle(L10n.tr("title.rename.files"))
        case let .remove(remove): RemoveView(store: remove)
        case let .version(version, fileId): VersionForm(version: version, keepFileId: fileId, store: store)
        case let .edition(fileId, current): EditionForm(fileId: fileId, current: current, store: store)
        case let .adopt(versionId): AdoptForm(versionId: versionId, store: store)
        case let .review(review): ReviewSheet(review: review)
        }
    }
}

private struct CastSheet: View {
    let info: TitleInfo

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 96), spacing: Theme.Spacing.medium)], spacing: Theme.Spacing.medium) {
                ForEach(info.cast) { PersonCircle(person: $0) }
            }
            .padding(Theme.Spacing.screen)
        }
        .navigationTitle(L10n.tr("title.cast"))
    }
}

private struct CardsSheet: View {
    let title: String
    let cards: [TitleCardInfo]
    let manage: Bool

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 120), spacing: Theme.Spacing.medium)], spacing: Theme.Spacing.medium) {
                ForEach(cards) { TitleCardLink(card: $0, manage: manage) }
            }
            .padding(Theme.Spacing.screen)
        }
        .navigationTitle(title)
    }
}

private struct HistorySheet: View {
    let info: TitleInfo

    var body: some View {
        List {
            if info.history.isEmpty { Text(L10n.tr("title.history.empty")) }
            ForEach(info.history) { HistoryRowView(row: $0) }
        }
        .navigationTitle(L10n.tr("title.history"))
    }
}

private struct ReviewSheet: View {
    let review: ReviewInfo
    @State private var shown = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                if review.rating > 0 { Text(String(format: "★ %.1f", review.rating)).font(.headline) }
                if review.spoiler && !shown {
                    Text(L10n.tr("title.review.spoiler"))
                    Button(L10n.tr("title.review.show")) { shown = true }
                } else {
                    Text(review.text).font(.body)
                }
            }
            .padding(Theme.Spacing.screen)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .navigationTitle(L10n.format("title.review.by", review.author.isEmpty ? L10n.tr("title.review.someone") : review.author))
    }
}

private struct HideSheet: View {
    let store: TitleStore
    @State private var genre = ""
    @State private var onePerson = false
    @State private var person: Int64 = 0
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let info = store.info
        Form {
            Section { Text(L10n.tr("title.hide.body")).font(.callout) }
            if !info.hides.isEmpty {
                Section(L10n.tr("title.hidden.now")) {
                    ForEach(info.hides) { hide in
                        HStack {
                            Text((hide.genre.isEmpty ? (hide.name.isEmpty ? info.name : hide.name) : hide.genre) + " · " + who(hide.userId, info))
                            Spacer()
                            Button(L10n.tr("title.show.again")) { store.run { await $0.unhide(hide.id) } }
                        }
                    }
                }
            }
            if !info.genres.isEmpty {
                Picker(L10n.tr("title.hide.what"), selection: $genre) {
                    Text(L10n.format("title.hide.only", info.name)).tag("")
                    ForEach(info.genres, id: \.self) { genre in
                        Text(info.isShow ? L10n.format("title.hide.genre.shows", genre) : L10n.format("title.hide.genre.movies", genre)).tag(genre)
                    }
                }
            }
            if info.users.count > 1 {
                Picker(L10n.tr("title.hide.person"), selection: $onePerson) {
                    Text(L10n.tr("title.hide.everyone")).tag(false)
                    Text(L10n.tr("title.hide.one.person")).tag(true)
                }
                .pickerStyle(.segmented)
                if onePerson {
                    Picker(L10n.tr("title.hide.person"), selection: $person) {
                        Text(L10n.tr("title.hide.pick.person")).tag(Int64(0))
                        ForEach(info.users) { Text($0.name).tag($0.id) }
                    }
                }
            }
            Button(L10n.tr("title.hide.button")) {
                let name = onePerson ? (info.users.first { $0.id == person }?.name ?? "") : ""
                let genre = self.genre
                let userId = onePerson ? person : 0
                Task {
                    if await store.driver.hide(genre: genre, userId: userId, who: name) { dismiss() }
                }
            }
            .disabled(info.busy == "hide" || (onePerson && person == 0))
        }
        .navigationTitle(L10n.format("title.hide.title", info.name))
        .navigationBarTitleDisplayMode(.inline)
        .overlay { FeedbackToast(feedback: info.feedback) }
    }

    private func who(_ id: Int64, _ info: TitleInfo) -> String {
        if id == 0 { return L10n.tr("title.hide.everyone") }
        return info.users.first { $0.id == id }?.name ?? L10n.tr("title.hide.removed.person")
    }
}

struct GateChoicesView: View {
    @Binding var gate: String

    var body: some View {
        Picker(L10n.tr("title.release.gate"), selection: $gate) {
            gateRow("announced", L10n.tr("title.gate.announced"), L10n.tr("title.gate.announced.hint"))
            gateRow("in_cinemas", L10n.tr("title.gate.in.cinemas"), L10n.tr("title.gate.in.cinemas.hint"))
            gateRow("released", L10n.tr("title.gate.released"), L10n.tr("title.gate.released.hint"))
        }
        .pickerStyle(.inline)
    }

    private func gateRow(_ value: String, _ label: String, _ hint: String) -> some View {
        VStack(alignment: .leading) {
            Text(label)
            Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
        }
        .tag(value)
    }
}

private struct GateSheet: View {
    let store: TitleStore
    @State private var gate = ""
    @State private var days = 0
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            GateChoicesView(gate: $gate)
            Section {
                Stepper(value: $days, in: 0 ... 365) {
                    Text(L10n.tr("title.gate.wait.days") + ": \(days)")
                }
                Text(L10n.tr("title.gate.wait.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            Button(L10n.tr("title.save")) {
                let gate = self.gate
                let days = self.days
                store.run { await $0.setGate(gate, days: days) }
                dismiss()
            }
        }
        .navigationTitle(L10n.tr("title.release.gate"))
        .onAppear {
            gate = store.info.releaseGate.isEmpty ? "released" : store.info.releaseGate
            days = store.info.gateDelayDays
        }
    }
}

private struct VersionForm: View {
    let version: TitleVersionInfo?
    let keepFileId: Int64
    let store: TitleStore
    @State private var label = ""
    @State private var rules: Int64 = 0
    @State private var folder: Int64 = 0
    @State private var keep = true
    @State private var edition = ""
    @State private var audio = ""
    @State private var folderName = ""
    @State private var prepared = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let info = store.info
        let taken = info.versions.filter { $0.id != version?.id }.map(\.label)
        Form {
            if version == nil {
                Section { Text(info.isShow ? L10n.tr("title.version.intro.show") : L10n.tr("title.version.intro.movie")).font(.callout) }
            }
            Section {
                TextField(L10n.tr("title.version.name"), text: $label)
                if let problem = labelProblem(label, taken: taken), !label.isEmpty {
                    Text(TitleText.label(problem)).font(.footnote).foregroundStyle(Theme.failText)
                }
                if !info.isShow && version == nil {
                    TextField(L10n.tr("title.edition"), text: $edition)
                }
                TextField(L10n.tr("title.audio.languages"), text: $audio)
                Text(L10n.tr("title.audio.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            Section {
                Picker(L10n.tr("title.version.rules.label"), selection: $rules) {
                    ForEach(info.kindRules) { Text($0.name).tag($0.id) }
                }
                if version == nil {
                    Picker(L10n.tr("title.version.folder"), selection: $folder) {
                        ForEach(info.kindFolders) { Text("\($0.shown) (\($0.path))").tag($0.id) }
                    }
                    if folder == info.libraryFolderId {
                        Text(L10n.tr("title.version.same.folder")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                    if !info.isShow { TextField(L10n.tr("title.version.existing.folder"), text: $folderName) }
                }
                Toggle(isOn: $keep) {
                    VStack(alignment: .leading) {
                        Text(L10n.tr("title.keep.up.to.date"))
                        Text(L10n.tr("title.version.keep.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
                .tint(Theme.accent)
            }
            if let error = info.choicesError { Text(error.localized).foregroundStyle(Theme.failText) }
            Button(version == nil ? L10n.tr("title.add.version.button") : L10n.tr("title.save")) { save() }
                .disabled(labelProblem(label, taken: taken) != nil || info.busy == "version")
        }
        .navigationTitle(version.map { L10n.format("title.version.change", $0.label) } ?? L10n.format("title.version.add", info.name))
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: info.choicesLoaded, initial: true) { prepare() }
        .overlay { FeedbackToast(feedback: info.feedback) }
    }

    private func prepare() {
        guard !prepared else { return }
        let info = store.info
        if let version {
            label = version.label
            rules = version.pickRulesId
            keep = version.keepUpToDate
            audio = version.audioLanguages.joined(separator: ", ")
            prepared = true
            return
        }
        guard info.choicesLoaded else { return }
        let taken = info.versions.map(\.label)
        label = taken.contains { $0.caseInsensitiveCompare("4K") == .orderedSame } ? "" : "4K"
        rules = info.kindRules.first { $0.name.caseInsensitiveCompare("4K") == .orderedSame }?.id
            ?? info.kindRules.first { $0.name.range(of: "4k|2160|uhd", options: [.regularExpression, .caseInsensitive]) != nil }?.id
            ?? info.kindRules.first?.id ?? 0
        folder = info.kindFolders.first {
            $0.id != info.libraryFolderId && ($0.shown + $0.path).range(of: "4k|2160|uhd", options: [.regularExpression, .caseInsensitive]) != nil
        }?.id ?? info.libraryFolderId
        prepared = true
    }

    private func labelProblem(_ text: String, taken: [String]) -> LabelProblemKind? {
        let value = text.trimmingCharacters(in: .whitespaces)
        if value.isEmpty { return .empty }
        if value.utf8.count > 40 { return .toolong }
        if value.rangeOfCharacter(from: CharacterSet(charactersIn: "/\\:*?\"<>|[]").union(.controlCharacters)) != nil { return .badcharacter }
        if value.hasSuffix(".") { return .trailingdot }
        if taken.contains(where: { $0.caseInsensitiveCompare(value) == .orderedSame }) { return .taken }
        return nil
    }

    private func save() {
        let label = self.label
        let rules = self.rules
        let folder = self.folder
        let keep = self.keep
        let edition = self.edition
        let audio = self.audio
        let folderName = self.folderName
        let version = self.version
        let keepFileId = self.keepFileId
        let driver = store.driver
        Task {
            var ok = false
            if let version {
                ok = await driver.changeVersion(version.id, label: label, rulesId: rules, keep: keep, audio: audio)
            } else if keepFileId > 0 {
                ok = await driver.keepAndAdd(
                    fileId: keepFileId, label: label, rulesId: rules, folderId: folder, keep: keep, edition: edition, audio: audio, folderName: folderName
                )
            } else {
                ok = await driver.addVersion(label: label, rulesId: rules, folderId: folder, keep: keep, edition: edition, audio: audio, folderName: folderName)
            }
            if ok { dismiss() }
        }
    }
}

private struct EditionForm: View {
    let fileId: Int64
    let current: String
    let store: TitleStore
    @State private var edition = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            TextField(L10n.tr("title.edition"), text: $edition)
            Button(L10n.tr("title.save")) {
                let value = edition
                let driver = store.driver
                let id = fileId
                Task { if await driver.setEdition(id, edition: value) { dismiss() } }
            }
            .disabled(edition.count > 40)
        }
        .navigationTitle(L10n.tr("title.set.edition"))
        .onAppear { edition = current }
        .overlay { FeedbackToast(feedback: store.info.feedback) }
    }
}

private struct AdoptForm: View {
    let versionId: Int64
    let store: TitleStore
    @State private var path = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Text(L10n.tr("title.adopt.hint")).font(.callout)
            TextField(L10n.tr("title.adopt.path"), text: $path)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            Button(L10n.tr("title.adopt.button")) {
                let value = path
                let driver = store.driver
                let id = versionId
                Task { if await driver.adopt(id, path: value) { dismiss() } }
            }
            .disabled(path.trimmingCharacters(in: .whitespaces).isEmpty)
        }
        .navigationTitle(L10n.tr("title.adopt"))
        .overlay { FeedbackToast(feedback: store.info.feedback) }
    }
}
