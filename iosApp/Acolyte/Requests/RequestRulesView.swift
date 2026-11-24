import SwiftUI

struct NotAllowedView: View {
    var body: some View {
        EmptyStateView(title: L10n.tr("requests.not.allowed"), message: L10n.tr("requests.not.allowed.text"), systemImage: "lock")
    }
}

struct LimitField: View {
    let title: String
    let text: String
    let onChange: (String) -> Void
    @State private var value = ""

    var body: some View {
        LabeledContent(title) {
            TextField(title, text: $value)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .frame(maxWidth: 100)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }
}

struct LineEditor: View {
    let kind: LineKindInfo
    let line: QualityLineInfo
    let count: Int
    let rules: [PickOption]
    let store: RequestRulesStore
    @State private var label = ""
    @State private var name = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            TextField(L10n.tr("requests.rules.line.label"), text: $label)
            TextField(L10n.tr("requests.rules.line.name"), text: $name, prompt: Text(RequestsWords.quality(line.lineId, "")))
            if line.isOwn {
                Picker(L10n.tr("requests.quality"), selection: Binding(get: { line.pickRulesId }, set: { store.setLineRules(kind, line.index, $0) })) {
                    ForEach(rules) { rule in Text(rule.name).tag(rule.id) }
                }
            }
            Text(hint)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            HStack {
                Toggle(L10n.tr("requests.rules.line.shown"), isOn: Binding(get: { !line.hidden }, set: { store.setLineShown(kind, line.index, $0) }))
                Button {
                    store.moveLine(kind, line.index, up: true)
                } label: {
                    Image(systemName: "arrow.up")
                }
                .buttonStyle(.borderless)
                .disabled(line.index == 0)
                .accessibilityLabel(L10n.tr("requests.move.up"))
                Button {
                    store.moveLine(kind, line.index, up: false)
                } label: {
                    Image(systemName: "arrow.down")
                }
                .buttonStyle(.borderless)
                .disabled(line.index >= count - 1)
                .accessibilityLabel(L10n.tr("requests.move.down"))
                if line.isOwn {
                    Button(role: .destructive) {
                        store.removeLine(kind, line.index)
                    } label: {
                        Image(systemName: "trash")
                    }
                    .buttonStyle(.borderless)
                    .accessibilityLabel(L10n.tr("requests.remove"))
                }
            }
        }
        .onAppear {
            label = line.label
            name = line.name
        }
        .onChange(of: label) { _, value in if value != line.label { store.setLineLabel(kind, line.index, value) } }
        .onChange(of: name) { _, value in if value != line.name { store.setLineName(kind, line.index, value) } }
    }

    private var hint: String {
        let rules = line.rulesName.isEmpty
            ? L10n.tr(line.lineId == "4k" ? "requests.rules.named.4k" : "requests.rules.folder.quality")
            : line.rulesName
        let copy = L10n.tr(line.extra ? "requests.rules.own.copy" : "requests.rules.main.copy")
        return line.ready ? "\(rules) · \(copy)" : "\(rules) · \(copy). \(L10n.tr("requests.rules.not.ready"))"
    }
}

struct RequestRulesView: View {
    @State private var store = RequestRulesStore()
    @State private var advanced = false
    @State private var saved = false
    @Environment(\.openRoute) private var openRoute

    private var info: RequestRulesInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    form
                }
            }
        }
        .navigationTitle(L10n.tr("requests.rules.title"))
        .toolbar {
            if info.dirty {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("requests.discard")) { store.discard() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("requests.save")) { store.save() }
                            .disabled(!info.canSave)
                    }
                }
            }
        }
        .navigationBarBackButtonHidden(info.dirty)
        .safeAreaInset(edge: .bottom) {
            if saved {
                NoteToast(text: L10n.tr("requests.rules.saved"), undo: nil)
            }
        }
        .onChange(of: info.savedCount) { _, count in
            guard count > 0 else { return }
            saved = true
            Task {
                try? await Task.sleep(for: .seconds(3))
                saved = false
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private func flag(_ flag: SettingsFlagKind) -> Binding<Bool> {
        Binding(get: { info.flags[flag] ?? false }, set: { store.setFlag(flag, $0) })
    }

    private func folderPicker(_ title: String, _ field: FolderFieldKind) -> some View {
        let first: String = switch field {
        case .movie: L10n.tr("requests.rules.first.movie.folder")
        case .show: L10n.tr("requests.rules.first.show.folder")
        case .music: L10n.tr("requests.rules.first.music.folder")
        case .movieFourK, .showFourK: L10n.tr("requests.rules.same.folder")
        }
        return Picker(title, selection: Binding(get: { info.folder[field] ?? 0 }, set: { store.setFolder(field, $0) })) {
            Text(first).tag(Int64(0))
            ForEach(info.folders[field] ?? []) { option in Text(option.name).tag(option.id) }
        }
    }

    private func rulesPicker(_ title: String, _ field: FolderFieldKind) -> some View {
        let none = L10n.tr(field == .movieFourK || field == .showFourK ? "requests.rules.named.4k" : "requests.rules.folder.quality")
        return Picker(title, selection: Binding(get: { info.pickRules[field] ?? 0 }, set: { store.setPickRules(field, $0) })) {
            Text(none).tag(Int64(0))
            ForEach(info.rules[field] ?? []) { option in Text(option.name).tag(option.id) }
        }
    }

    private var form: some View {
        Form {
            if let error = info.saveError {
                Text(RequestsWords.error(error)).foregroundStyle(Theme.failText)
            }
            if !info.limitsValid {
                Text(L10n.tr("requests.rules.limit.invalid")).foregroundStyle(Theme.failText)
            }
            Section {
                LimitField(title: L10n.tr("requests.rules.movie.asks"), text: info.limits[.movies] ?? "") { store.setLimit(.movies, $0) }
                LimitField(title: L10n.tr("requests.rules.show.asks"), text: info.limits[.shows] ?? "") { store.setLimit(.shows, $0) }
                LimitField(title: L10n.tr("requests.rules.music.asks"), text: info.limits[.music] ?? "") { store.setLimit(.music, $0) }
            } header: {
                Text(L10n.tr("requests.rules.limits"))
            } footer: {
                Text(L10n.tr("requests.rules.limits.sub") + ". " + L10n.tr("requests.rules.limits.hint"))
            }
            Section(L10n.tr("requests.rules.approved")) {
                folderPicker(L10n.tr("requests.rules.movie.folder"), .movie)
                folderPicker(L10n.tr("requests.rules.show.folder"), .show)
                folderPicker(L10n.tr("requests.rules.music.folder"), .music)
                Toggle(isOn: flag(.searchOnApprove)) {
                    VStack(alignment: .leading) {
                        Text(L10n.tr("requests.rules.search.on.approve"))
                        Text(L10n.tr("requests.rules.search.on.approve.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
                DisclosureGroup(L10n.tr("requests.rules.advanced"), isExpanded: $advanced) {
                    rulesPicker(L10n.tr("requests.rules.movie.quality"), .movie)
                    rulesPicker(L10n.tr("requests.rules.show.quality"), .show)
                    rulesPicker(L10n.tr("requests.rules.music.quality"), .music)
                    Text(L10n.tr("requests.rules.quality.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            Section {
                Toggle(L10n.tr("requests.rules.owner.auto"), isOn: flag(.ownerAutoApprove))
            } header: {
                Text(L10n.tr("requests.rules.approvals"))
            } footer: {
                Text(L10n.tr("requests.rules.approvals.hint"))
            }
            Section {
                folderPicker(L10n.tr("requests.rules.movie.folder"), .movieFourK)
                rulesPicker(L10n.tr("requests.rules.movie.quality"), .movieFourK)
                folderPicker(L10n.tr("requests.rules.show.folder"), .showFourK)
                rulesPicker(L10n.tr("requests.rules.show.quality"), .showFourK)
            } header: {
                Text(L10n.tr("requests.rules.four.k"))
            } footer: {
                Text(L10n.tr("requests.rules.four.k.sub"))
            }
            linesSections
            plexSection
            streamingSection
            cleanupSection
        }
    }

    @ViewBuilder
    private var linesSections: some View {
        ForEach(LineKindInfo.allCases) { kind in
            let lines = info.lines[kind] ?? []
            if !lines.isEmpty {
                Section {
                    ForEach(lines) { line in
                        LineEditor(
                            kind: kind,
                            line: line,
                            count: lines.count,
                            rules: info.rules[kind == .movie ? .movie : .show] ?? [],
                            store: store
                        )
                    }
                    Button(L10n.tr("requests.rules.add.line")) { store.addLine(kind) }
                        .disabled(!(info.canAddLine[kind] ?? false))
                } header: {
                    Text(L10n.tr(kind == .movie ? "requests.rules.movie.asks" : "requests.rules.show.asks"))
                } footer: {
                    if kind == .movie { Text(L10n.tr("requests.rules.lines.sub")) }
                }
            }
        }
        Section {
            Toggle(isOn: flag(.pickMany)) {
                VStack(alignment: .leading) {
                    Text(L10n.tr("requests.rules.pick.many"))
                    Text(L10n.tr("requests.rules.pick.many.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
        } header: {
            Text(L10n.tr("requests.rules.lines"))
        }
    }

    private var plexSection: some View {
        Section {
            Toggle(isOn: flag(.plexWatchlists)) {
                VStack(alignment: .leading) {
                    Text(L10n.tr("requests.rules.plex.on"))
                    Text(L10n.tr("requests.rules.plex.on.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            Toggle(isOn: flag(.plexEveryone)) {
                VStack(alignment: .leading) {
                    Text(L10n.tr("requests.rules.plex.everyone"))
                    Text(L10n.tr("requests.rules.plex.everyone.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            .disabled(!(info.flagEnabled[.plexEveryone] ?? false))
            Toggle(isOn: flag(.plexFriends)) {
                VStack(alignment: .leading) {
                    Text(L10n.tr("requests.rules.plex.friends"))
                    Text(L10n.tr("requests.rules.plex.friends.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            .disabled(!(info.flagEnabled[.plexFriends] ?? false))
            if info.showFriends, info.friendsLoaded {
                if !info.ownerLinked {
                    Text(L10n.tr("requests.friends.link.first")).font(.footnote).foregroundStyle(Theme.textMuted)
                } else if info.friends.isEmpty {
                    Text(L10n.tr("requests.friends.none")).font(.footnote).foregroundStyle(Theme.textMuted)
                } else {
                    ForEach(info.friends) { friend in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(friend.kid ? "\(friend.name) · \(L10n.tr("requests.kid"))" : friend.name)
                            let checked = friend.checkedAt.map { L10n.format("requests.friends.checked", Format.relative($0)) }
                                ?? L10n.tr("requests.friends.not.checked")
                            Text(L10n.format("requests.friends.line", friend.plexName, checked, friend.asked))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                            if !friend.error.isEmpty {
                                Text(friend.error).font(.footnote).foregroundStyle(Theme.failText)
                            }
                        }
                    }
                }
            }
        } header: {
            Text(L10n.tr("requests.rules.plex"))
        } footer: {
            Text(L10n.tr("requests.rules.plex.sub"))
        }
    }

    private var streamingSection: some View {
        Section {
            Picker(L10n.tr("requests.rules.country"), selection: Binding(get: { info.region }, set: { store.setRegion($0) })) {
                Text(L10n.tr("requests.rules.not.set")).tag("")
                ForEach(regionOptions, id: \.self) { code in
                    Text(RequestsWords.region(code)).tag(code)
                }
            }
            if info.region.isEmpty {
                if let mine = Locale.current.region?.identifier, store.regions.contains(mine) {
                    Button(L10n.tr("requests.rules.use.my.country")) { store.setRegion(mine) }
                }
            } else if info.servicesLoading {
                Text(L10n.tr("requests.rules.services.loading")).foregroundStyle(Theme.textMuted)
            } else if let error = info.servicesError {
                Text(error.localized).foregroundStyle(Theme.failText)
            } else if info.services.isEmpty {
                Text(L10n.tr("requests.rules.services.none")).foregroundStyle(Theme.textMuted)
            } else {
                Text(L10n.format("requests.rules.services.picked", info.pickedCount, 30))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                ForEach(info.services) { service in
                    Toggle(isOn: Binding(get: { service.picked }, set: { _ in store.toggleService(service.id) })) {
                        HStack {
                            AuthImage(source: service.logo.isEmpty ? nil : service.logo, size: .thumb) {
                                ArtPlaceholderView(name: service.name)
                            }
                            .frame(width: 28, height: 28)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                            Text(service.name)
                        }
                    }
                }
            }
            Toggle(isOn: flag(.skipOnServices)) {
                VStack(alignment: .leading) {
                    Text(L10n.tr("requests.rules.skip"))
                    Text(L10n.tr("requests.rules.skip.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            .disabled(!(info.flagEnabled[.skipOnServices] ?? false))
        } header: {
            Text(L10n.tr("requests.rules.streaming"))
        } footer: {
            Text(L10n.tr("requests.rules.streaming.sub"))
        }
    }

    private var regionOptions: [String] {
        var codes = store.regions.sorted { RequestsWords.region($0) < RequestsWords.region($1) }
        if !info.region.isEmpty, !codes.contains(info.region) { codes.append(info.region) }
        return codes
    }

    private var cleanupSection: some View {
        Section {
            Button(L10n.tr("requests.cleanup.check")) { store.startCleanup() }
                .disabled(!info.canCheck)
            if info.dirty {
                Text(L10n.tr("requests.cleanup.save.first")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if let error = info.cleanupError {
                Text(RequestsWords.error(error)).foregroundStyle(Theme.failText)
            }
            if let check = info.cleanup {
                if check.running {
                    ProgressView(value: check.total > 0 ? Double(check.checked) / Double(check.total) : 0) {
                        Text(L10n.format("requests.cleanup.progress", check.checked, check.total)).font(.footnote)
                    }
                } else if check.finished {
                    let total = check.found.reduce(Int64(0)) { $0 + $1.bytes }
                    let head = check.found.isEmpty
                        ? L10n.format("requests.cleanup.none", check.total)
                        : L10n.format("requests.cleanup.found", check.found.count, RequestsWords.bytes(total))
                    Text(check.failed > 0 ? head + " " + L10n.format("requests.cleanup.failed", check.failed) : head)
                        .font(.footnote)
                }
                ForEach(check.found) { title in
                    Button {
                        if !title.tmdbId.isEmpty { openRoute(.title(kind: title.kind, tmdb: title.tmdbId)) }
                    } label: {
                        VStack(alignment: .leading) {
                            Text(RequestsWords.titleWithYear(title.name, title.year)).foregroundStyle(Theme.text)
                            Text(L10n.format("requests.cleanup.row", title.service, RequestsWords.bytes(title.bytes)))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                    }
                }
            }
        } header: {
            Text(L10n.tr("requests.cleanup.title"))
        } footer: {
            Text(L10n.tr("requests.cleanup.sub"))
        }
    }
}
