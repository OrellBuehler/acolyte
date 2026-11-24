import SwiftUI

struct AddTitleView: View {
    let store: AddTitleStore
    let title: TitleStore

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
        .onChange(of: store.info?.addedId ?? 0) { _, added in
            guard added > 0, let info = store.info else { return }
            if info.openPick { title.openReleases(.whole) } else { title.sheet = nil }
        }
    }

    private func content(_ info: AddTitleInfo) -> some View {
        Form {
            if info.manage {
                switch info.load.phase {
                case .loading:
                    HStack {
                        ProgressView()
                        Text(L10n.tr("title.add.loading"))
                    }
                case .failed:
                    Text(L10n.tr("title.add.folders.failed"))
                    Button(L10n.tr("common.retry")) { store.run { await $0.load() } }
                case .ready:
                    options(info)
                }
            }
            if !info.isBatch {
                Toggle(isOn: Binding(get: { info.follow }, set: { store.driver.setFollow($0) })) {
                    labelled(L10n.tr("title.follow"), L10n.tr("title.add.follow.hint"))
                }
                .tint(Theme.accent)
            }
            if info.isBatch {
                Section {
                    ForEach(info.batch) { item in
                        HStack {
                            Text(item.name + (item.year > 0 ? " (\(item.year))" : ""))
                            Spacer()
                            batchChip(item, manage: info.manage)
                        }
                    }
                }
            }
            if let error = info.error { Text(error.localized).foregroundStyle(Theme.failText) }
            if !info.batchDone {
                Button(submitText(info)) { store.run { await $0.submit() } }
                    .disabled(!info.canSubmit)
            } else {
                Button(L10n.tr("title.done")) { title.sheet = nil }
            }
        }
        .navigationTitle(navTitle(info))
        .navigationBarTitleDisplayMode(.inline)
        .overlay { FeedbackToast(feedback: info.feedback) }
    }

    @ViewBuilder
    private func options(_ info: AddTitleInfo) -> some View {
        if info.defaultsLate { Text(L10n.tr("title.add.defaults.late")).font(.footnote).foregroundStyle(Theme.waitText) }
        Section {
            if info.noFolder {
                Text(info.kind == "show" ? L10n.tr("title.add.no.folder.show") : L10n.tr("title.add.no.folder.movie")).foregroundStyle(Theme.failText)
            } else if info.folders.isEmpty && info.handsOff {
                Text(L10n.tr("title.add.hands.off")).font(.footnote)
            } else {
                Picker(L10n.tr("title.folder"), selection: Binding(get: { info.folderId }, set: { store.driver.setFolder($0) })) {
                    ForEach(info.folders) { Text("\($0.shown) (\($0.path))").tag($0.id) }
                }
            }
            Picker(L10n.tr("title.pick.rules"), selection: Binding(get: { info.rulesId }, set: { store.driver.setRules($0) })) {
                ForEach(info.rules) { Text($0.name).tag($0.id) }
            }
            Text(L10n.tr("title.pick.rules.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
        }
        if info.kind != "show" {
            Section(L10n.tr("title.release.gate")) {
                GateChoicesView(gate: Binding(get: { info.gate }, set: { store.driver.setGate($0) }))
            }
        } else if !info.isBatch {
            showOptions(info)
        }
        Section {
            Toggle(isOn: Binding(get: { info.keepUpToDate }, set: { store.driver.setKeep($0) })) {
                labelled(L10n.tr("title.keep.up.to.date"), info.kind == "show" ? L10n.tr("title.add.keep.hint.show") : L10n.tr("title.add.keep.hint"))
            }
            if !info.choose {
                Toggle(isOn: Binding(get: { info.searchOnAdd }, set: { store.driver.setSearch($0) })) {
                    labelled(
                        L10n.tr("title.add.search"),
                        info.kind == "show" ? L10n.tr("title.add.search.hint.show") : L10n.tr("title.add.search.hint")
                    )
                }
            }
            if !info.isBatch {
                Toggle(isOn: Binding(get: { info.secondOn }, set: { store.driver.setSecond($0) })) {
                    labelled(L10n.tr("title.add.second"), L10n.tr("title.add.second.hint"))
                }
            }
        }
        .tint(Theme.accent)
        if info.secondOn && !info.isBatch {
            Section {
                TextField(L10n.tr("title.version.name"), text: Binding(get: { info.secondLabel }, set: { store.driver.setSecondLabel(String($0.prefix(40))) }))
                if let problem = info.secondProblem { Text(TitleText.label(problem)).font(.footnote).foregroundStyle(Theme.failText) }
                Picker(L10n.tr("title.version.rules.label"), selection: Binding(get: { info.secondRulesId }, set: { store.driver.setSecondRules($0) })) {
                    ForEach(info.rules) { Text($0.name).tag($0.id) }
                }
                Picker(L10n.tr("title.version.folder"), selection: Binding(get: { info.secondFolderId }, set: { store.driver.setSecondFolder($0) })) {
                    ForEach(info.folders) { Text($0.shown).tag($0.id) }
                }
                if info.sameFolder { Text(L10n.format("title.add.same.folder", info.secondLabel)).font(.footnote).foregroundStyle(Theme.textMuted) }
            }
        }
    }

    @ViewBuilder
    private func showOptions(_ info: AddTitleInfo) -> some View {
        Section(L10n.tr("title.add.seasons")) {
            Picker(L10n.tr("title.add.seasons"), selection: Binding(get: { info.seasons }, set: { store.driver.setSeasons($0) })) {
                option("all", L10n.tr("title.add.seasons.all"), L10n.tr("title.add.seasons.all.hint"))
                option("future", L10n.tr("title.add.seasons.future"), L10n.tr("title.add.seasons.future.hint"))
                option("latest", L10n.tr("title.add.seasons.latest"), L10n.tr("title.add.seasons.latest.hint"))
                option("none", L10n.tr("title.add.seasons.none"), L10n.tr("title.add.seasons.none.hint"))
            }
            .pickerStyle(.inline)
            .labelsHidden()
        }
        Section(L10n.tr("title.add.new.seasons")) {
            Picker(L10n.tr("title.add.new.seasons"), selection: Binding(get: { info.newSeasons }, set: { store.driver.setNewSeasons($0) })) {
                Text(L10n.tr("title.add.new.seasons.all")).tag("all")
                Text(L10n.tr("title.add.new.seasons.none")).tag("none")
            }
            .pickerStyle(.segmented)
            .disabled(info.newSeasonsLocked)
            Text(info.newSeasonsLocked ? L10n.tr("title.add.new.seasons.locked") : L10n.tr("title.add.new.seasons.hint"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        Section(L10n.tr("title.episode.order")) {
            Picker(L10n.tr("title.episode.order"), selection: Binding(get: { info.order }, set: { store.driver.setOrder($0) })) {
                option("aired", L10n.tr("title.add.order.aired"), L10n.tr("title.add.order.aired.hint"))
                option("daily", L10n.tr("title.add.order.daily"), L10n.tr("title.add.order.daily.hint"))
                option("absolute", L10n.tr("title.add.order.absolute"), L10n.tr("title.add.order.absolute.hint"))
            }
            .pickerStyle(.inline)
            .labelsHidden()
        }
        Section(L10n.tr("title.add.details.from")) {
            Picker(L10n.tr("title.add.details.from"), selection: Binding(get: { info.source }, set: { store.driver.setSource($0) })) {
                Text("TMDB").tag("tmdb")
                Text("TVMaze").tag("tvmaze")
                Text("TheTVDB").tag("tvdb")
            }
            .pickerStyle(.segmented)
        }
    }

    private func option(_ value: String, _ label: String, _ hint: String) -> some View {
        labelled(label, hint).tag(value)
    }

    private func labelled(_ label: String, _ hint: String) -> some View {
        VStack(alignment: .leading) {
            Text(label)
            Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
        }
    }

    private func batchChip(_ item: BatchItemInfo, manage: Bool) -> some View {
        switch item.line {
        case .waiting: StatusChip(text: L10n.tr("title.batch.waiting"), tone: .idle)
        case .done: StatusChip(text: manage ? L10n.tr("title.batch.added") : L10n.tr("title.batch.requested"), tone: .ok)
        case .already: StatusChip(text: L10n.tr("title.batch.already"), tone: .wait)
        case .failed: StatusChip(text: item.error?.localized ?? L10n.tr("error.unknown"), tone: .fail)
        }
    }

    private func navTitle(_ info: AddTitleInfo) -> String {
        if info.isBatch { return info.manage ? L10n.format("title.add.all.of", info.name) : L10n.format("title.request.all.of", info.name) }
        return L10n.format("title.add.name", info.name)
    }

    private func submitText(_ info: AddTitleInfo) -> String {
        if info.isBatch { return info.manage ? L10n.tr("title.add.all") : L10n.tr("title.request.all") }
        return info.choose ? L10n.tr("title.add.and.choose") : L10n.tr("title.add.to.library")
    }
}
