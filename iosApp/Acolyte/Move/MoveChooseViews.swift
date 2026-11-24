import SwiftUI

struct MoveChooseStepView: View {
    let store: MoveStore
    let onShow: (MoveListRequest) -> Void
    let onKeys: (MoveListSource) -> Void
    let onNotMoved: () -> Void
    @State private var open = false

    private var info: MoveInfo { store.info }
    private var choose: MoveChooseInfo { info.choose }

    var body: some View {
        List {
            summary
            if !choose.needFolders.isEmpty {
                Section {
                    ForEach(choose.needFolders) { row in
                        MoveFolderRowView(store: store, row: row)
                    }
                } header: {
                    Text(L10n.tr("acq.mv.folders"))
                } footer: {
                    Text(L10n.tr("acq.mv.pick.where"))
                }
            }
            if !choose.people.isEmpty { peopleSection }
            if choose.namesAsk { namesSection }
            if choose.sourceKeys > 0 || choose.downloaderKeys > 0 || choose.invitees > 0 { afterSection }
            if choose.planned {
                Section {
                    DisclosureGroup(L10n.tr("acq.mv.change.title"), isExpanded: $open) {
                        changeBody
                    }
                }
            }
        }
        .refreshable { await store.refresh() }
    }

    private var summary: some View {
        Section {
            if !choose.planned {
                if let problem = choose.planProblem {
                    Text(MoveWords.problem(problem)).foregroundStyle(Theme.failText)
                } else {
                    Text(L10n.tr("acq.mv.gate.updating")).foregroundStyle(Theme.textMuted)
                }
            } else if choose.empty {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("acq.mv.choose.empty")).font(.headline)
                    Text(L10n.tr("acq.mv.choose.empty.text")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            } else {
                HStack(alignment: .top, spacing: Theme.Spacing.large) {
                    ForEach(choose.tiles) { tile in
                        VStack(alignment: .leading) {
                            Text("\(tile.n)").font(.title.weight(.bold))
                            Text(tileWord(tile))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                if !choose.alsoGroups.isEmpty {
                    Text(L10n.format("acq.mv.also", MoveWords.andList(choose.alsoGroups.map { MoveWords.also($0) })))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Text(L10n.tr("acq.mv.files.stay")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if choose.stillChecking > 0 {
                Text(L10n.format("acq.mv.still.checking", Int32(choose.stillChecking))).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if choose.tryNotice {
                Text(L10n.tr("acq.mv.checked.again")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if let problem = choose.tryProblem {
                Text(MoveWords.problem(problem)).foregroundStyle(Theme.failText)
            }
        } header: {
            Text(L10n.tr("acq.mv.step2.title"))
        }
    }

    private func tileWord(_ tile: MoveTileInfo) -> String {
        switch tile.group {
        case .titles: L10n.plural("acq.mv.tile.titles", tile.n)
        case .wishlist: L10n.plural("acq.mv.tile.wishlist", tile.n)
        case .people: L10n.plural("acq.mv.tile.people", tile.n)
        default: MoveWords.group(tile.group, raw: tile.label)
        }
    }

    private var peopleSection: some View {
        Section {
            ForEach(choose.people) { person in
                Toggle(isOn: Binding(
                    get: { person.on },
                    set: { on in
                        let choice: MovePersonChoiceKind? = on ? (person.owner ? .owner : .admin) : nil
                        store.setPerson(person.key, choice: choice)
                    }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(person.name) · \(person.owner ? L10n.tr("acq.mv.this.is.me") : L10n.tr("acq.mv.keep.admin"))")
                        Text(person.owner ? L10n.format("acq.mv.owner.in", store.appName(person.app)) : L10n.format("acq.mv.admin.in", store.appName(person.app)))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
        } header: {
            Text(L10n.tr("acq.mv.account.title"))
        }
    }

    private var namesSection: some View {
        Section {
            nameOption(.keep, title: L10n.tr("acq.mv.names.keep"), hint: L10n.tr("acq.mv.names.keep.hint"))
            nameOption(.rename, title: L10n.tr("acq.mv.names.rename"), hint: L10n.tr("acq.mv.names.rename.hint"))
        } header: {
            Text(L10n.tr("acq.mv.names.title"))
        }
    }

    private func nameOption(_ value: MoveNamesKind, title: String, hint: String) -> some View {
        Button {
            store.setNames(value)
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).foregroundStyle(Theme.text)
                    Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                Spacer()
                if choose.names == value { Image(systemName: "checkmark").foregroundStyle(Theme.accentText) }
            }
        }
    }

    private var afterSection: some View {
        Section {
            if choose.sourceKeys > 0 {
                afterRow(L10n.plural("acq.mv.reconnect.sources", choose.sourceKeys), hint: L10n.tr("acq.mv.paused.keys")) { onKeys(.sourceKeys) }
            }
            if choose.downloaderKeys > 0 {
                afterRow(L10n.plural("acq.mv.reconnect.downloaders", choose.downloaderKeys), hint: L10n.tr("acq.mv.paused.passwords")) {
                    onKeys(.downloaderKeys)
                }
            }
            if choose.invitees > 0 {
                afterRow(L10n.plural("acq.mv.invite.users", choose.invitees), hint: L10n.tr("acq.mv.invite.hint")) {
                    onShow(MoveListRequest(source: .invitees, listId: "people", title: L10n.tr("acq.mv.list.people")))
                }
            }
        } header: {
            Text(L10n.tr("acq.mv.after.title"))
        }
    }

    private func afterRow(_ title: String, hint: String, action: @escaping () -> Void) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            Spacer()
            Button(L10n.tr("acq.mv.show"), action: action).buttonStyle(.borderless)
        }
    }

    @ViewBuilder
    private var changeBody: some View {
        ForEach(choose.groups) { group in
            groupRow(group)
            if group.showFormats {
                Toggle(isOn: Binding(get: { !choose.noFormats }, set: { store.setNoFormats(!$0) })) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L10n.tr("acq.mv.formats"))
                        Text(L10n.tr("acq.mv.formats.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            }
        }
        if !choose.folders.isEmpty {
            Text(L10n.tr("acq.mv.folders")).font(.headline)
            ForEach(choose.folders) { row in
                MoveFolderRowView(store: store, row: row)
            }
        }
        if !choose.folders.isEmpty || choose.showRules {
            Text(choose.folders.isEmpty ? L10n.tr("acq.mv.path.rules") : L10n.tr("acq.mv.advanced")).font(.headline)
            Text(L10n.tr("acq.mv.path.rules.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
            ForEach(Array(choose.rules.enumerated()), id: \.offset) { index, rule in
                MoveRuleRowView(store: store, index: index, rule: rule)
                    .id("\(index)-\(choose.rules.count)")
            }
            Button {
                store.addRule("")
            } label: {
                Label(L10n.tr("acq.mv.add.rule"), systemImage: "plus")
            }
            .buttonStyle(.borderless)
        }
        if choose.notMovedCount > 0 {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("acq.mv.not.imported"))
                    Text(L10n.plural("acq.mv.not.imported.n", choose.notMovedCount)).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                Spacer()
                Button(L10n.tr("acq.mv.show"), action: onNotMoved).buttonStyle(.borderless)
            }
        }
        if !choose.emptyGroups.isEmpty {
            Text(L10n.format("acq.mv.nothing.for", choose.emptyGroups.joined(separator: ", ")))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private func groupRow(_ group: MoveGroupInfo) -> some View {
        let label = MoveWords.group(group.group, raw: group.label)
        return HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(label) · \(group.count)")
                let note = noteText(group)
                if !note.isEmpty {
                    Text(note).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            Spacer()
            Button(L10n.tr("acq.mv.show")) {
                onShow(MoveListRequest(source: .planGroup, listId: group.id, title: label))
            }
            .buttonStyle(.borderless)
            .disabled(group.count == 0)
            Toggle("", isOn: Binding(get: { group.on }, set: { store.setGroup(group.id, on: $0) }))
                .labelsHidden()
                .disabled(!group.canSkip)
        }
    }

    private func noteText(_ group: MoveGroupInfo) -> String {
        switch group.note {
        case .none: ""
        case .leftOut: L10n.tr("acq.mv.note.left.out")
        case .invite: L10n.tr("acq.mv.note.invite")
        case .keys: L10n.tr("acq.mv.note.keys")
        case .keysOrPasswords: L10n.tr("acq.mv.note.keys.pw")
        case .checkShows: L10n.plural("acq.mv.note.check", group.noteCount)
        }
    }
}

struct MoveFolderRowView: View {
    let store: MoveStore
    let row: MoveFolderInfo
    @State private var other: String
    @FocusState private var focused: Bool

    init(store: MoveStore, row: MoveFolderInfo) {
        self.store = store
        self.row = row
        _other = State(initialValue: row.other)
    }

    private var selection: String {
        switch row.pick {
        case .none: "none"
        case .leave: "leave"
        case .other: "other"
        case .path: "p:" + row.path
        }
    }

    private var paths: [String] {
        if row.pick == .path && !row.options.contains(row.path) { return row.options + [row.path] }
        return row.options
    }

    private func apply(_ value: String) {
        switch value {
        case "none": store.pickFolder(row.oldPath, kind: .none, path: "")
        case "leave": store.pickFolder(row.oldPath, kind: .leave, path: "")
        case "other": store.pickFolder(row.oldPath, kind: .other, path: "")
        default: store.pickFolder(row.oldPath, kind: .path, path: String(value.dropFirst(2)))
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(row.oldPath)
                .font(.footnote.monospaced())
            Picker(L10n.tr("acq.mv.rule.to"), selection: Binding(get: { selection }, set: { apply($0) })) {
                Text(row.unseen ? L10n.tr("acq.mv.bring.without") : L10n.tr("acq.mv.dont.bring")).tag("none")
                if row.unseen { Text(L10n.tr("acq.mv.dont.bring")).tag("leave") }
                ForEach(paths, id: \.self) { path in
                    Text(path).tag("p:" + path)
                }
                Text(L10n.tr("acq.mv.other.path")).tag("other")
            }
            if row.pick == .path && row.path == row.suggested && !row.suggested.isEmpty {
                Text(L10n.tr("acq.mv.suggested")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if row.pick == .other {
                TextField(L10n.format("acq.mv.path.here.for", row.oldPath), text: $other, prompt: Text("/data/movies"))
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .focused($focused)
                    .onChange(of: other) { _, value in store.typeOther(row.oldPath, text: value) }
                    .onChange(of: focused) { _, isFocused in if !isFocused { store.commitFolders() } }
                    .onSubmit { store.commitFolders() }
            }
            if row.problem != .none {
                Text(MoveWords.pathProblem(row.problem)).font(.footnote).foregroundStyle(Theme.failText)
            }
            if !row.gone.isEmpty {
                Text(L10n.format("acq.mv.gone.note", row.gone)).font(.footnote).foregroundStyle(Theme.failText)
                if row.canAddRule {
                    Button(L10n.tr("acq.mv.add.rule")) { store.addRule(row.oldPath) }
                        .buttonStyle(.bordered)
                }
            }
            if !row.twins.isEmpty {
                Text(L10n.format("acq.mv.twins", row.twins.joined(separator: ", "))).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }
}

struct MoveRuleRowView: View {
    let store: MoveStore
    let index: Int
    let rule: MoveRuleInfo
    @State private var from: String
    @State private var to: String
    @FocusState private var focused: Bool

    init(store: MoveStore, index: Int, rule: MoveRuleInfo) {
        self.store = store
        self.index = index
        self.rule = rule
        _from = State(initialValue: rule.from)
        _to = State(initialValue: rule.to)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack {
                VStack {
                    TextField(L10n.tr("acq.mv.rule.from"), text: $from, prompt: Text("/movies"))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focused)
                    TextField(L10n.tr("acq.mv.rule.to"), text: $to, prompt: Text("/data/movies"))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focused)
                }
                Button {
                    store.removeRule(index)
                } label: {
                    Image(systemName: "minus.circle")
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(L10n.tr("acq.mv.rule.remove"))
            }
            if rule.problem != .none {
                Text(MoveWords.pathProblem(rule.problem)).font(.footnote).foregroundStyle(Theme.failText)
            }
        }
        .onChange(of: from) { _, value in store.editRule(index, from: value, to: to) }
        .onChange(of: to) { _, value in store.editRule(index, from: from, to: value) }
        .onChange(of: focused) { _, isFocused in if !isFocused { store.commitFolders() } }
    }
}
