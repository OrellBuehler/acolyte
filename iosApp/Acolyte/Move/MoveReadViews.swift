import SwiftUI

struct MoveReadStepView: View {
    let store: MoveStore
    let onAdd: () -> Void
    let onEdit: (MoveReadInfo) -> Void
    let onForget: (MoveReadInfo) -> Void
    let onOpenDownloaders: () -> Void
    @State private var showAll = false

    private var info: MoveInfo { store.info }

    var body: some View {
        List {
            Section {
                if info.reads.isEmpty && info.readingKind.isEmpty {
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(L10n.tr("acq.mv.empty.title")).font(.headline)
                        Text(L10n.tr("acq.mv.empty.text"))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                ForEach(info.reads) { row in
                    readRow(row)
                }
                if !info.readingKind.isEmpty {
                    VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                        HStack {
                            Text(store.appName(info.readingKind))
                            Spacer()
                            StatusChip(text: L10n.tr("acq.mv.reading.chip"), tone: .info)
                        }
                        ProgressView()
                        Text(L10n.tr("acq.mv.reading.now"))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                Button(action: onAdd) {
                    Label(L10n.tr("acq.mv.add.app"), systemImage: "plus")
                }
            } header: {
                Text(L10n.tr("acq.mv.step1.title"))
            } footer: {
                Text(L10n.tr("acq.mv.subtitle"))
            }
            if !info.brought.isEmpty {
                Section {
                    ForEach(info.brought) { row in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(row.name)
                                Text("\(row.app) · \(row.kind)")
                                    .font(.footnote)
                                    .foregroundStyle(Theme.textMuted)
                            }
                            Spacer()
                            broughtChip(row.needs)
                        }
                    }
                    if info.brought.contains(where: { !$0.needs.isEmpty }) {
                        Button(L10n.tr("acq.mv.open.downloaders"), action: onOpenDownloaders)
                    }
                } header: {
                    Text(L10n.tr("acq.mv.brought.title"))
                } footer: {
                    Text(L10n.tr("acq.mv.brought.text"))
                }
            }
            if !info.moves.isEmpty || info.movesFailed {
                Section {
                    if info.movesFailed {
                        Text(L10n.tr("acq.mv.earlier.failed")).foregroundStyle(Theme.failText)
                    }
                    ForEach(showAll ? info.moves : Array(info.moves.prefix(5))) { move in
                        earlierRow(move)
                    }
                    if !showAll && info.moves.count > 5 {
                        Button(L10n.format("acq.mv.show.all", Int32(info.moves.count))) { showAll = true }
                    }
                } header: {
                    Text(L10n.tr("acq.mv.earlier"))
                }
            }
        }
        .refreshable { await store.refresh() }
    }

    @ViewBuilder
    private func broughtChip(_ needs: String) -> some View {
        switch needs {
        case "key": StatusChip(text: L10n.tr("acq.mv.needs.key"), tone: .wait)
        case "password": StatusChip(text: L10n.tr("acq.mv.needs.password"), tone: .wait)
        case "token": StatusChip(text: L10n.tr("acq.mv.needs.token"), tone: .wait)
        default: StatusChip(text: L10n.tr("acq.mv.brought.ready"), tone: .ok)
        }
    }

    private func readRow(_ row: MoveReadInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Toggle(isOn: Binding(get: { row.picked }, set: { store.togglePick(row.id, on: $0) })) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(row.showHost ? L10n.format("acq.mv.app.at", row.name, row.host) : row.name)
                    Text(statsLine(row))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    HStack(spacing: Theme.Spacing.small) {
                        if row.version.isEmpty {
                            StatusChip(text: L10n.tr("acq.mv.read.tag"), tone: .idle)
                        } else {
                            StatusChip(text: L10n.format("acq.mv.version.tag", row.version), tone: .ok)
                        }
                        Text(L10n.format("acq.mv.read.on", MoveWords.shortDate(row.createdAt)))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
            HStack {
                Button(L10n.tr("acq.mv.edit")) { onEdit(row) }
                Button(L10n.tr("acq.mv.remove"), role: .destructive) { onForget(row) }
            }
            .buttonStyle(.borderless)
            .font(.footnote)
        }
    }

    private func statsLine(_ row: MoveReadInfo) -> String {
        var parts: [String] = []
        if row.usedBy > 0 { parts.append(L10n.format("acq.mv.used.in", Int32(truncatingIfNeeded: row.usedBy))) }
        parts.append(row.stats.isEmpty ? L10n.tr("acq.mv.nothing.found") : MoveWords.counts(row.stats))
        return parts.joined(separator: " · ")
    }

    private func earlierRow(_ move: MoveEarlierInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack {
                Text(L10n.format("acq.mv.earlier.row", Int32(truncatingIfNeeded: move.id), MoveWords.shortDate(move.createdAt)))
                Spacer()
                StatusChip(text: MoveWords.state(move.word), tone: move.tone)
            }
            Text(move.counts.isEmpty ? L10n.tr("acq.mv.nothing.counted") : MoveWords.summary(move.counts))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            HStack {
                Button(L10n.tr("acq.mv.open")) { store.openMove(move.id) }
                if move.canRemove {
                    Button(L10n.tr("acq.mv.remove"), role: .destructive) { store.removeMove(move.id) }
                }
            }
            .buttonStyle(.borderless)
            .font(.footnote)
        }
    }
}

struct MoveReadSheet: View {
    let store: MoveStore
    let request: MoveReadRequest
    let serverHost: String
    @Environment(\.dismiss) private var dismiss
    @State private var kind: String
    @State private var address: String
    @State private var key = ""
    @State private var username = ""
    @State private var password = ""
    @State private var useSaved = true
    @State private var missing = false
    @State private var showOthers = false

    init(store: MoveStore, request: MoveReadRequest, serverHost: String) {
        self.store = store
        self.request = request
        self.serverHost = serverHost
        _kind = State(initialValue: request.kind)
        _address = State(initialValue: request.address.replacingOccurrences(of: "http://", with: ""))
    }

    private var info: MoveInfo { store.info }
    private var sheet: MoveSheetInfo { info.sheet }
    private var editing: Bool { request.editing }
    private var saved: MoveSavedKeyInfo? {
        info.savedKey(kind: kind, startAddress: editing ? request.address : "", editing: editing)
    }
    private var withSaved: Bool { saved != nil && useSaved }

    private var form: MoveFormInfo {
        MoveFormInfo(
            kind: kind,
            address: withSaved ? (saved?.address ?? "") : address.trimmingCharacters(in: .whitespaces),
            key: withSaved ? "" : key.trimmingCharacters(in: .whitespaces),
            username: withSaved ? "" : username,
            password: withSaved ? "" : password,
            useSavedKey: withSaved
        )
    }

    private var title: String {
        editing ? L10n.format("acq.mv.sheet.edit", store.appName(kind)) : L10n.tr("acq.mv.sheet.add")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker(L10n.tr("acq.mv.app"), selection: $kind) {
                        ForEach(store.apps) { app in Text(app.name).tag(app.kind) }
                    }
                    .disabled(editing || sheet.reading)
                    .onChange(of: kind) { _, _ in
                        useSaved = true
                        store.clearFound()
                        changed()
                    }
                    if let saved {
                        Toggle(isOn: Binding(get: { useSaved }, set: { useSaved = $0; changed() })) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(editing ? L10n.tr("acq.mv.saved.key") : L10n.tr("acq.mv.use.saved"))
                                Text(L10n.format("acq.mv.sent.only.to", saved.address))
                                    .font(.footnote)
                                    .foregroundStyle(Theme.textMuted)
                            }
                        }
                    }
                }
                if !withSaved {
                    credentials
                    if info.owner && !editing { find }
                }
                result
                if !sheet.linked.isEmpty { linkedSection }
                if let problem = sheet.readProblem {
                    Section { Text(MoveWords.problem(problem)).foregroundStyle(Theme.failText) }
                }
                if sheet.reading {
                    Section {
                        ProgressView()
                        Text("\(L10n.format("acq.mv.adding", store.appName(kind))) · \(L10n.tr("acq.mv.takes.a.while"))")
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                } else if sheet.stopped {
                    Section { Text(L10n.tr("acq.mv.stopped")).font(.footnote).foregroundStyle(Theme.textMuted) }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.mv.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if sheet.reading {
                        Button(L10n.tr("acq.mv.stop")) { store.stopRead() }
                    } else {
                        Button(editing ? L10n.tr("acq.mv.save") : L10n.format("acq.mv.add.button", store.appName(kind))) { submit() }
                    }
                }
            }
        }
        .interactiveDismissDisabled(false)
    }

    private func changed() {
        missing = false
        store.clearProbe()
    }

    private func submit() {
        if !withSaved && address.trimmingCharacters(in: .whitespaces).isEmpty {
            missing = true
            return
        }
        store.read(form) { ok in
            if ok { dismiss() }
        }
    }

    private var credentials: some View {
        Section {
            TextField(L10n.tr("acq.mv.address"), text: $address, prompt: Text(store.placeholder(kind).replacingOccurrences(of: "http://", with: "")))
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .onChange(of: address) { _, _ in
                    if editing { useSaved = false }
                    changed()
                }
            if missing {
                Text(L10n.tr("acq.mv.address.missing")).font(.footnote).foregroundStyle(Theme.failText)
            }
            if kind == "qbittorrent" {
                TextField(L10n.tr("acq.mv.username"), text: $username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .onChange(of: username) { _, _ in changed() }
                SecureField(L10n.tr("acq.mv.password"), text: $password)
                    .onChange(of: password) { _, _ in changed() }
            }
            TextField(L10n.tr("acq.mv.api.key"), text: $key)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .onChange(of: key) { _, _ in changed() }
        } footer: {
            Text(kind == "qbittorrent" ? L10n.tr("acq.mv.api.key.qb") : L10n.format("acq.mv.api.key.where", store.appName(kind)))
        }
    }

    private var find: some View {
        Section {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("acq.mv.find"))
                    Text(L10n.tr("acq.mv.find.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Spacer()
                Button(L10n.tr("acq.mv.find.search")) { store.find(host: serverHost, form: form) }
                    .buttonStyle(.bordered)
                    .disabled(sheet.finding || sheet.reading)
            }
            if sheet.finding { ProgressView() }
            if let problem = sheet.findProblem {
                Text(MoveWords.problem(problem)).foregroundStyle(Theme.failText)
            }
            if sheet.findRan {
                let mine = sheet.found.filter { $0.kind == kind }
                let others = sheet.found.filter { $0.kind != kind }
                ForEach(mine, id: \.address) { found in foundRow(found) }
                if mine.isEmpty {
                    Text(sheet.found.isEmpty ? L10n.tr("acq.mv.find.none") : L10n.format("acq.mv.find.none.kind", store.appName(kind)))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                if !others.isEmpty {
                    DisclosureGroup(L10n.format("acq.mv.find.others", Int32(others.count)), isExpanded: $showOthers) {
                        ForEach(others, id: \.address) { found in foundRow(found) }
                    }
                }
                if sheet.found.isEmpty && !sheet.searched.isEmpty {
                    Text(L10n.format("acq.mv.find.searched", sheet.searched.joined(separator: ", ")))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    private func foundRow(_ found: MoveFoundInfo) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text("\(found.name) \(found.version)".trimmingCharacters(in: .whitespaces))
                Text(found.address).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            Spacer()
            Button(L10n.tr("acq.mv.find.use")) {
                kind = found.kind
                address = found.address
                useSaved = false
                store.clearFound()
                changed()
            }
            .buttonStyle(.borderless)
        }
    }

    @ViewBuilder
    private var result: some View {
        Section {
            Button {
                if !withSaved && address.trimmingCharacters(in: .whitespaces).isEmpty {
                    missing = true
                } else {
                    store.probe(form)
                }
            } label: {
                Text(sheet.probing ? L10n.tr("acq.mv.testing") : L10n.tr("acq.mv.test"))
            }
            .disabled(sheet.reading || sheet.probing)
            if let probe = sheet.probe {
                if probe.ok {
                    StatusChip(text: "\(probe.app) \(probe.version)".trimmingCharacters(in: .whitespaces), tone: .ok)
                } else if let problem = probe.problem {
                    Text(MoveWords.problem(problem)).foregroundStyle(Theme.failText)
                }
            }
        }
    }

    private var linkedSection: some View {
        Section {
            ForEach(sheet.linked, id: \.address) { row in
                HStack {
                    VStack(alignment: .leading) {
                        Text("\(row.name.isEmpty ? store.appName(row.kind) : row.name) \(row.version)".trimmingCharacters(in: .whitespaces))
                        Text(row.address).font(.footnote).foregroundStyle(Theme.textMuted)
                        if row.hasKey && !row.reachable {
                            Text(L10n.tr("acq.mv.not.reached")).font(.footnote).foregroundStyle(Theme.failText)
                        }
                    }
                    Spacer()
                    if !row.hasKey && row.offered {
                        Button(L10n.tr("acq.mv.find.use")) {
                            kind = row.kind
                            address = row.address
                            useSaved = false
                            changed()
                        }
                        .buttonStyle(.borderless)
                    }
                }
            }
            if !sheet.addable.isEmpty {
                let names = MoveWords.andList(sheet.addable.map { $0.name.isEmpty ? store.appName($0.kind) : $0.name })
                Button(L10n.format("acq.mv.also.add", names, store.appName(sheet.linkedFrom))) { store.addLinked(form) }
                    .disabled(sheet.addingLinked || sheet.reading)
            }
        } header: {
            Text(L10n.tr("acq.mv.linked.title"))
        } footer: {
            if !sheet.addable.isEmpty {
                Text(L10n.format("acq.mv.linked.keys", store.appName(sheet.linkedFrom)))
            }
        }
    }
}
