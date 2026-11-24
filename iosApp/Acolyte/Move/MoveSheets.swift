import SwiftUI

private struct MoveItemRowView: View {
    let row: MoveItemInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(alignment: .firstTextBaseline) {
                Text(row.name)
                if row.n > 1 { Text("×\(row.n)").foregroundStyle(Theme.textMuted) }
                Spacer()
                pill
            }
            if !row.hint.isEmpty { note(row.hint) }
            if row.group != .unknown { note(MoveWords.group(row.group, raw: "")) }
            if !row.traits.isEmpty { note(L10n.format("acq.mv.traits", row.traits)) }
            if !row.skipped.isEmpty { note(L10n.format("acq.mv.left.out.list", row.skipped)) }
            if !row.needs.isEmpty { note(L10n.format("acq.mv.needs.what", row.needs)) }
            if !row.section.isEmpty { note(row.section) }
        }
    }

    private func note(_ text: String) -> some View {
        Text(text).font(.footnote).foregroundStyle(Theme.textMuted)
    }

    @ViewBuilder
    private var pill: some View {
        if let word = row.word {
            StatusChip(text: MoveWords.word(word, raw: row.raw), tone: MoveWords.tone(row.tone))
        } else if row.look {
            StatusChip(text: L10n.tr("acq.mv.tag.looks"), tone: .wait)
        }
        switch row.badge {
        case .none: EmptyView()
        case .you: StatusChip(text: L10n.tr("acq.mv.badge.you"), tone: .ok)
        case .plex: StatusChip(text: L10n.tr("acq.mv.badge.plex"), tone: .ok)
        case .jellyfin: StatusChip(text: L10n.tr("acq.mv.badge.jellyfin"), tone: .ok)
        case .emby: StatusChip(text: L10n.tr("acq.mv.badge.emby"), tone: .ok)
        case .invite: StatusChip(text: L10n.tr("acq.mv.badge.invite"), tone: .idle)
        }
    }
}

struct MoveItemsSheet: View {
    let store: MoveStore
    let request: MoveListRequest
    @Environment(\.dismiss) private var dismiss
    @State private var list: MoveListInfo
    @State private var query = ""
    @State private var onlyLook: Bool
    @State private var limit = 100

    init(store: MoveStore, request: MoveListRequest) {
        self.store = store
        self.request = request
        let list = store.items(request.source, id: request.listId)
        _list = State(initialValue: list)
        _onlyLook = State(initialValue: list.showFilter)
    }

    var body: some View {
        let page = list.page(query: query, onlyLook: onlyLook, limit: limit)
        NavigationStack {
            List {
                if list.showFilter {
                    Section {
                        Picker(L10n.tr("acq.mv.list.which"), selection: $onlyLook) {
                            Text(L10n.tr("acq.mv.tag.looks")).tag(true)
                            Text(L10n.tr("acq.mv.list.all")).tag(false)
                        }
                        .pickerStyle(.segmented)
                    }
                }
                Section {
                    ForEach(page.rows) { row in
                        MoveItemRowView(row: row)
                    }
                    if page.rows.isEmpty {
                        Text(query.isEmpty ? L10n.tr("acq.mv.list.empty") : L10n.tr("acq.mv.list.no.match"))
                            .foregroundStyle(Theme.textMuted)
                    }
                    if page.rows.count < page.total {
                        Button(L10n.tr("acq.mv.list.more")) { limit += 100 }
                    }
                } footer: {
                    if page.total > 0 {
                        Text(L10n.format("acq.mv.list.showing", Int32(page.rows.count), Int32(page.total)))
                    }
                }
            }
            .searchable(text: $query, prompt: Text(L10n.tr("acq.mv.list.search")))
            .onChange(of: query) { _, _ in limit = 100 }
            .onChange(of: onlyLook) { _, _ in limit = 100 }
            .navigationTitle(request.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("acq.mv.close")) { dismiss() }
                }
            }
        }
    }
}

struct MoveKeysSheet: View {
    let store: MoveStore
    let source: MoveListSource
    @Environment(\.dismiss) private var dismiss
    @State private var rows: [MoveItemInfo]

    init(store: MoveStore, source: MoveListSource) {
        self.store = store
        self.source = source
        _rows = State(initialValue: store.items(source, id: "").rows)
    }

    var body: some View {
        NavigationStack {
            List {
                section("usenet", title: L10n.tr("acq.mv.sec.usenet"))
                section("torrent", title: L10n.tr("acq.mv.sec.torrent"))
                section("downloaders", title: L10n.tr("acq.mv.sec.downloaders"))
            }
            .navigationTitle(source == .downloaderKeys ? L10n.tr("acq.mv.keys.downloaders") : L10n.tr("acq.mv.keys.sources"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("acq.mv.close")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    @ViewBuilder
    private func section(_ id: String, title: String) -> some View {
        let inSection = rows.filter { $0.section == id }
        if !inSection.isEmpty {
            Section {
                ForEach(inSection) { row in
                    HStack {
                        Text(row.name)
                        Spacer()
                        switch row.needs {
                        case "key": StatusChip(text: L10n.tr("acq.mv.need.key"), tone: .idle)
                        case "password": StatusChip(text: L10n.tr("acq.mv.need.password"), tone: .idle)
                        default: EmptyView()
                        }
                    }
                }
            } header: {
                Text("\(title) · \(inSection.count)")
            }
        }
    }
}

struct MoveNotMovedSheet: View {
    let store: MoveStore
    @Environment(\.dismiss) private var dismiss
    @State private var app: String?

    var body: some View {
        NavigationStack {
            List {
                if let app {
                    let rows = store.items(.notMovedApp, id: app).rows
                    Section {
                        ForEach(rows) { row in
                            MoveItemRowView(row: row)
                        }
                    } header: {
                        Button(L10n.tr("acq.mv.all.apps")) { self.app = nil }
                    }
                } else {
                    Section {
                        ForEach(store.info.choose.notMovedApps) { entry in
                            Button {
                                app = entry.app
                            } label: {
                                HStack {
                                    Text(entry.name.isEmpty ? entry.app : entry.name)
                                    Spacer()
                                    Text("\(entry.n)").foregroundStyle(Theme.textMuted)
                                    Image(systemName: "chevron.right").font(.footnote).foregroundStyle(Theme.textFaint)
                                }
                            }
                            .foregroundStyle(Theme.text)
                        }
                    } footer: {
                        Text(L10n.tr("acq.mv.not.imported.lead"))
                    }
                }
            }
            .navigationTitle(app.map { L10n.format("acq.mv.not.imported.app", store.appName($0)) } ?? L10n.tr("acq.mv.not.imported"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("acq.mv.close")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

struct MoveBeforeSheet: View {
    let store: MoveStore
    let onShow: (MoveListRequest) -> Void

    private var info: MoveInfo { store.info }
    private var choose: MoveChooseInfo { info.choose }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(L10n.format("acq.mv.before.text", Int32(choose.total)))
                    if choose.tryNotice {
                        Text(L10n.tr("acq.mv.checked.again")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                    if choose.stillChecking > 0 {
                        Text(L10n.format("acq.mv.still.checking", Int32(choose.stillChecking))).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                    if let outcome = choose.outcome { MoveOutcomeView(outcome: outcome, after: false) }
                }
                if !choose.before.isEmpty {
                    Section {
                        ForEach(choose.before) { row in
                            let label = MoveWords.group(row.group, raw: row.label)
                            HStack {
                                Text(L10n.format("acq.mv.before.row", Int32(row.n), label, MoveWords.summaryWord(row.word, raw: "")))
                                Spacer()
                                Button(L10n.tr("acq.mv.show")) {
                                    onShow(MoveListRequest(source: .beforeGroup, listId: row.id, title: label))
                                }
                                .buttonStyle(.borderless)
                            }
                        }
                    }
                }
                if !choose.choices.isEmpty {
                    Section {
                        ForEach(choose.choices) { row in
                            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                                Text(row.name)
                                Text(why(row.why)).font(.footnote).foregroundStyle(Theme.textMuted)
                                Picker(row.name, selection: Binding(
                                    get: { row.choice },
                                    set: { if let value = $0 { store.chooseOnce(row.key, choice: value) } }
                                )) {
                                    Text(L10n.tr("acq.mv.same.person")).tag(Optional(MovePersonChoiceKind.merge))
                                    Text(L10n.tr("acq.mv.leave.out")).tag(Optional(MovePersonChoiceKind.skip))
                                    if row.choice == nil { Text("").tag(Optional<MovePersonChoiceKind>.none) }
                                }
                                .pickerStyle(.segmented)
                            }
                        }
                    } header: {
                        Text(L10n.tr("acq.mv.needs.choice"))
                    } footer: {
                        Text(L10n.tr("acq.mv.until.choose"))
                    }
                }
                if let problem = choose.tryProblem {
                    Section { Text(MoveWords.problem(problem)).foregroundStyle(Theme.failText) }
                }
            }
            .navigationTitle(L10n.tr("acq.mv.before.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.mv.go.back")) { store.closeDialog() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("acq.mv.import.anyway")) { store.start() }
                        .disabled(choose.trying || choose.stale || info.starting)
                }
            }
        }
        .presentationDetents([.large])
    }

    private func why(_ why: MoveWhyKind) -> String {
        switch why {
        case .sameName: L10n.tr("acq.mv.why.same.name")
        case .numericId: L10n.tr("acq.mv.why.numeric")
        case .mergeGone: L10n.tr("acq.mv.why.gone")
        }
    }
}
