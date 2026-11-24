import SwiftUI

struct LeftoversView: View {
    let store: LeftoversStore
    let title: TitleStore
    @State private var leftAlone = false

    var body: some View {
        Group {
            if let info = store.info {
                content(info)
            } else {
                ProgressView()
            }
        }
        .task { await store.watch() }
        .task { await store.driver.load() }
    }

    private func content(_ info: LeftoversInfo) -> some View {
        List {
            Section {
                Text(L10n.tr("title.leftovers.intro")).font(.footnote)
                if let error = info.load.error { NoticeBanner(text: error.localized) }
                if !info.handsOff {
                    Button(info.rescanning ? L10n.tr("title.rescanning") : L10n.tr("title.rescan")) { store.run { await $0.rescan() } }
                        .disabled(info.rescanning)
                }
                if info.renameOffered {
                    Text(L10n.tr("title.leftovers.rename.note")).font(.footnote)
                    Button(L10n.tr("title.rename.to.match")) { title.openRename() }
                    Button(L10n.tr("title.undo")) { store.run { await $0.undoMatch() } }
                }
            }
            if !info.shiftMoves.isEmpty {
                Section(L10n.format("title.shift.title", info.shiftFrom, info.shiftTo, info.shiftBy)) {
                    Text(L10n.plural("title.shift.body", info.shiftMoves.count)).font(.footnote)
                    ForEach(info.shiftMoves) { Text("\($0.from) → \($0.to)").font(.footnote) }
                    Button(L10n.format("title.match.all", info.shiftMoves.count)) { store.run { await $0.matchShift() } }
                        .disabled(info.busy)
                }
            }
            Section {
                if info.load.phase == .loading { ProgressView() }
                if info.load.phase == .ready && info.open.isEmpty { Text(L10n.tr("title.leftovers.none")) }
                ForEach(info.open) { leftover in
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(leftover.fileName).font(.subheadline.weight(.semibold)).lineLimit(2)
                        Text([TitleText.bytes(leftover.size), leftover.resolution > 0 ? "\(leftover.resolution)p" : "", TitleText.leftoverReason(leftover)]
                            .filter { !$0.isEmpty }
                            .joined(separator: " · "))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                        HStack {
                            Button(L10n.tr("title.leave.alone")) {
                                let id = leftover.fileId
                                store.run { await $0.dismiss(id, dismissed: true) }
                            }
                            Spacer()
                            Button(L10n.tr("title.match")) {
                                let id = leftover.fileId
                                if info.show { store.driver.openMatch(id) } else { store.run { await $0.matchMovie(id) } }
                            }
                            .disabled(info.busy || (info.show && !info.episodesLoaded))
                        }
                        .buttonStyle(.borderless)
                    }
                }
            }
            if !info.leftAlone.isEmpty {
                Section {
                    Button(L10n.format("title.left.alone", info.leftAlone.count)) { leftAlone.toggle() }
                    if leftAlone {
                        ForEach(info.leftAlone) { leftover in
                            HStack {
                                Text(leftover.fileName).font(.footnote).lineLimit(2)
                                Spacer()
                                Button(L10n.tr("title.list.again")) {
                                    let id = leftover.fileId
                                    store.run { await $0.dismiss(id, dismissed: false) }
                                }
                                .buttonStyle(.borderless)
                            }
                        }
                    }
                }
            }
            if info.matchFileId != nil { matchSection(info) }
        }
        .navigationTitle(L10n.tr("title.leftovers"))
        .navigationBarTitleDisplayMode(.inline)
        .overlay { FeedbackToast(feedback: info.feedback) }
    }

    private func matchSection(_ info: LeftoversInfo) -> some View {
        Section(L10n.tr("title.match.file")) {
            Text(info.matchName).font(.footnote)
            Picker(L10n.tr("title.seasons"), selection: Binding(get: { info.matchSeason }, set: { store.driver.matchSeason($0) })) {
                ForEach(info.seasonNumbers, id: \.self) { Text(TitleText.season($0)).tag($0) }
            }
            Text(L10n.tr("title.match.pick")).font(.footnote)
            ForEach(info.matchEpisodes) { episode in
                Button {
                    store.driver.togglePick(episode.id)
                } label: {
                    HStack {
                        Image(systemName: info.matchPicked.contains(episode.id) ? "checkmark.square.fill" : "square")
                            .foregroundStyle(Theme.accent)
                        Text(TitleText.episodeCode(episode.season, episode.episode) + (episode.name.isEmpty ? "" : " · \(episode.name)"))
                            .foregroundStyle(Theme.text)
                        Spacer()
                        if episode.hasFile { Text(L10n.tr("title.has.file")).font(.caption).foregroundStyle(Theme.textMuted) }
                    }
                }
                .buttonStyle(.plain)
            }
            if info.pickedHasFile { Text(L10n.tr("title.match.replace.warning")).font(.footnote).foregroundStyle(Theme.waitText) }
            HStack {
                Button(L10n.tr("common.cancel")) { store.driver.closeMatch() }
                Spacer()
                Button(info.pickedHasFile ? L10n.tr("title.match.replace") : L10n.tr("title.match"), role: info.pickedHasFile ? .destructive : nil) {
                    store.run { await $0.confirmMatch() }
                }
                .disabled(info.matchPicked.isEmpty || info.busy)
            }
            .buttonStyle(.borderless)
        }
    }
}

struct RenameSection: View {
    let store: RenameStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if let info = store.info {
                if let error = info.load.error { NoticeBanner(text: error.localized) }
                if info.load.phase == .loading {
                    ProgressView()
                } else {
                    Text(info.changes.isEmpty ? L10n.tr("title.rename.nothing") : L10n.plural("title.rename.count", info.changes.count))
                        .font(.subheadline.weight(.semibold))
                    Text(L10n.tr("title.rename.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    ForEach(info.changes) { step in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(L10n.format("title.rename.now", step.from)).font(.footnote).foregroundStyle(Theme.textMuted)
                            Text(L10n.format("title.rename.new", step.to)).font(.callout)
                            ForEach(step.subtitles, id: \.self) { Text($0).font(.footnote) }
                        }
                    }
                    if !info.skipped.isEmpty {
                        Text(L10n.plural("title.rename.skipped", info.skipped.count)).font(.subheadline.weight(.semibold))
                        ForEach(info.skipped) { Text("\($0.from) · \($0.skip)").font(.footnote) }
                    }
                    if info.alreadyRight > 0 { Text(L10n.format("title.rename.right", info.alreadyRight)).font(.footnote) }
                    if info.running {
                        ProgressView(value: min(max(info.progress, 0), 1)).tint(Theme.accent)
                        Text(info.note.isEmpty ? L10n.tr("title.renaming") : info.note).font(.footnote)
                    }
                    if !info.changes.isEmpty {
                        Button(L10n.plural("title.rename.button", info.changes.count)) { store.run { await $0.apply() } }
                            .disabled(info.running)
                    }
                }
            } else {
                ProgressView()
            }
        }
        .task { await store.watch() }
        .task { await store.driver.load() }
        .overlay { FeedbackToast(feedback: store.info?.feedback) }
    }
}

struct RemoveView: View {
    let store: RemoveStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            if let info = store.info {
                content(info)
            } else {
                ProgressView()
            }
        }
        .task { await store.watch() }
        .task { await store.driver.load() }
    }

    private func target(_ info: RemoveInfo) -> String {
        if info.episodeCount > 0 { return L10n.plural("title.episodes.count", info.episodeCount) }
        if info.season >= 0 { return TitleText.season(info.season) }
        return info.name
    }

    private func content(_ info: RemoveInfo) -> some View {
        Form {
            if info.done {
                Text(L10n.format("title.deleted.done", info.name)).font(.headline)
                if info.canUndo {
                    Button(L10n.tr("title.undo")) {
                        store.run { await $0.undo() }
                        dismiss()
                    }
                }
                Button(L10n.tr("title.done")) { dismiss() }
            } else {
                if info.handsOff {
                    Text(L10n.tr("title.delete.hands.off")).font(.footnote)
                } else {
                    Toggle(isOn: Binding(get: { info.deleteFiles }, set: { value in store.run { await $0.setDeleteFiles(value) } })) {
                        VStack(alignment: .leading) {
                            Text(info.whole ? L10n.tr("title.delete.files") : L10n.tr("title.delete.their.files"))
                            Text(L10n.tr("title.delete.files.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                        }
                    }
                    .tint(Theme.accent)
                }
                if info.whole {
                    Toggle(isOn: Binding(get: { info.neverAdd }, set: { value in store.run { await $0.setNeverAdd(value) } })) {
                        VStack(alignment: .leading) {
                            Text(L10n.tr("title.never.add"))
                            Text(L10n.tr("title.never.add.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                        }
                    }
                    .tint(Theme.accent)
                }
                Section(L10n.tr("title.what.happens")) {
                    if let error = info.planError { Text(error.localized).foregroundStyle(Theme.failText) }
                    if !info.hasPlan && info.planError == nil { ProgressView() }
                    if info.hasPlan {
                        ForEach(planLines(info), id: \.self) { Text("· " + $0).font(.callout) }
                        ForEach(info.paths, id: \.self) { Text($0).font(.footnote).foregroundStyle(Theme.textMuted) }
                    }
                }
                Button(role: .destructive) {
                    store.run { await $0.apply() }
                } label: {
                    Text(buttonText(info))
                }
                .disabled(!info.ready)
            }
        }
        .navigationTitle(L10n.format("title.delete.question", target(info)))
        .navigationBarTitleDisplayMode(.inline)
        .overlay { FeedbackToast(feedback: info.feedback) }
    }

    private func buttonText(_ info: RemoveInfo) -> String {
        if info.season >= 0 { return L10n.format("title.delete.season", TitleText.season(info.season)) }
        if info.episodeCount > 0 { return L10n.tr("title.delete.episode") }
        if info.album { return L10n.tr("title.delete.album") }
        if info.artist { return L10n.tr("title.delete.artist") }
        return info.show ? L10n.tr("title.delete.show") : L10n.tr("title.delete.movie")
    }

    private func planLines(_ info: RemoveInfo) -> [String] {
        var lines: [String] = []
        if info.whole { lines.append(L10n.tr("title.plan.leaves")) }
        if info.planSeason > 0 || info.planScope == "season" { lines.append(L10n.format("title.plan.season", info.planSeason)) }
        if !info.planEpisodes.isEmpty { lines.append(L10n.format("title.plan.episodes", info.planEpisodes.joined(separator: ", "))) }
        if info.heldCount > 0 {
            if info.holdingDays > 0 {
                lines.append(
                    info.heldCount == 1
                        ? L10n.format("title.plan.held.days.one", info.heldCount, info.holdingDays)
                        : L10n.format("title.plan.held.days.other", info.heldCount, info.holdingDays)
                )
            } else {
                lines.append(L10n.plural("title.plan.held", info.heldCount))
            }
        }
        if info.keptCount > 0 { lines.append(L10n.plural("title.plan.kept", info.keptCount)) }
        if info.heldCount == 0 && info.keptCount == 0 { lines.append(L10n.tr("title.plan.no.files")) }
        if info.neverAdd && info.whole { lines.append(L10n.tr("title.plan.never.add")) }
        if info.activeDownloads > 0 { lines.append(L10n.plural("title.plan.downloads", info.activeDownloads)) }
        return lines
    }
}
