import SwiftUI

struct ReportsView: View {
    @State private var store = ReportsStore()
    @State private var open: ReportInfo?

    private var info: ReportsInfo { store.info }

    var body: some View {
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            List {
                if let error = info.load.error {
                    NoticeBanner(text: error.localized)
                        .listRowSeparator(.hidden)
                }
                Picker(L10n.tr("requests.filter"), selection: Binding(get: { info.filter }, set: { store.setFilter($0) })) {
                    ForEach(ReportFilterKind.allCases) { filter in
                        Text("\(filterLabel(filter)) \(info.counts[filter] ?? 0)").tag(filter)
                    }
                }
                .pickerStyle(.segmented)
                .listRowSeparator(.hidden)
                if info.shown.isEmpty {
                    empty
                        .listRowSeparator(.hidden)
                }
                ForEach(info.shown) { report in
                    row(report)
                        .contentShape(Rectangle())
                        .onTapGesture { open = report }
                        .swipeActions(edge: .trailing) {
                            if info.manager, report.isOpen {
                                Button(L10n.tr("requests.report.mark.fixed")) {
                                    Task { _ = await store.fix(report.id, reply: "") }
                                }
                                .tint(Theme.ok)
                            }
                        }
                }
            }
            .listStyle(.plain)
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr(info.manager ? "requests.reports" : "requests.your.reports"))
        .safeAreaInset(edge: .bottom) {
            if let note = info.note {
                NoteToast(text: noteText(note), undo: nil)
            }
        }
        .animation(.smooth(duration: 0.24), value: info.note?.id)
        .task(id: info.note?.id) {
            guard let note = info.note else { return }
            try? await Task.sleep(for: .seconds(4))
            if store.info.note?.id == note.id { store.dismissNote() }
        }
        .sheet(item: $open) { report in
            ReportDetailView(report: info.shown.first(where: { $0.id == report.id }) ?? report, store: store, manager: info.manager)
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private func filterLabel(_ filter: ReportFilterKind) -> String {
        switch filter {
        case .open: L10n.tr("requests.report.open")
        case .fixed: L10n.tr("requests.report.fixed")
        case .all: L10n.tr("requests.report.all")
        }
    }

    private func noteText(_ note: ReportsNoteInfo) -> String {
        let error = note.error.map { RequestsWords.error($0) } ?? ""
        switch note.kind {
        case .fixed:
            return note.reporter.isEmpty ? L10n.tr("requests.report.marked.fixed") : L10n.format("requests.report.marked.fixed.tell", note.reporter)
        case .fixFailed: return L10n.format("requests.report.fix.failed", error)
        case .reopened: return L10n.tr("requests.report.reopened")
        case .reopenFailed: return L10n.format("requests.report.reopen.failed", error)
        case .searchStarted: return L10n.format("requests.report.search.started", note.name)
        case .searchFailed: return L10n.format("requests.report.search.failed", error)
        }
    }

    private var empty: some View {
        let title: String
        let message: String
        if info.filter == .fixed {
            title = L10n.tr("requests.report.empty.fixed")
            message = L10n.tr("requests.report.empty.fixed.text")
        } else if info.manager {
            title = L10n.tr(info.filter == .open ? "requests.report.empty.open" : "requests.report.empty")
            message = L10n.tr("requests.report.empty.manager.text")
        } else {
            title = L10n.tr("requests.report.empty")
            message = L10n.tr("requests.report.empty.member.text")
        }
        return EmptyStateView(title: title, message: message, systemImage: "exclamationmark.bubble")
    }

    private func row(_ report: ReportInfo) -> some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            RequestThumb(image: report.poster, name: report.titleName)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(report.titleName).font(.headline)
                Text(RequestsWords.reportFacts(report)).font(.footnote).foregroundStyle(Theme.textMuted)
                StatusChip(
                    text: report.isOpen ? RequestsWords.reportKind(report.kind) : L10n.tr("requests.report.fixed"),
                    tone: report.isOpen ? .wait : .ok
                )
                let who = report.mine ? L10n.tr("requests.you") : report.reporter
                Text(L10n.format("requests.report.by", who, report.createdAt.map { Format.relative($0) } ?? ""))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Text(report.note.isEmpty ? L10n.tr("requests.report.no.note") : report.note)
                    .font(.callout)
                    .lineLimit(3)
                if !report.isOpen {
                    Text(L10n.format(
                        "requests.report.fixed.by",
                        report.fixer.isEmpty ? L10n.tr("requests.someone") : report.fixer,
                        report.closedAt.map { Format.relative($0) } ?? ""
                    ))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                }
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

struct ReportDetailView: View {
    let report: ReportInfo
    let store: ReportsStore
    let manager: Bool
    @Environment(\.dismiss) private var dismiss
    @State private var reply = ""

    private var quick: [String] {
        var out: [String] = []
        switch report.kind {
        case .subtitles: out.append(L10n.tr("requests.reply.subtitles"))
        case .sound: out.append(L10n.tr("requests.reply.sound"))
        case .picture: out.append(L10n.tr("requests.reply.picture"))
        default: break
        }
        out += [L10n.tr("requests.reply.swapped"), L10n.tr("requests.reply.try.again"), L10n.tr("requests.reply.nothing.wrong")]
        return out
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(RequestsWords.reportFacts(report)).font(.footnote).foregroundStyle(Theme.textMuted)
                    StatusChip(text: L10n.tr(report.isOpen ? "requests.report.open" : "requests.report.fixed"), tone: report.isOpen ? .wait : .ok)
                    VStack(alignment: .leading) {
                        Text(RequestsWords.reportKind(report.kind)).font(.headline)
                        Text(RequestsWords.reportHint(report.kind)).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                    Text(report.note.isEmpty ? L10n.tr("requests.report.no.note") : report.note)
                    if !report.isOpen, !report.reply.isEmpty {
                        Text("“\(report.reply)”").font(.callout)
                    }
                }
                if manager, report.isOpen {
                    Section {
                        TextField(
                            report.mine || report.reporter.isEmpty
                                ? L10n.tr("requests.reply.to.reporter")
                                : L10n.format("requests.reply.to", report.reporter),
                            text: $reply,
                            prompt: Text(L10n.tr("requests.reply.placeholder")),
                            axis: .vertical
                        )
                        .lineLimit(2 ... 6)
                        ForEach(quick, id: \.self) { words in
                            Button(words) { reply = words }
                        }
                    } footer: {
                        Text(L10n.tr(reply.isEmpty ? "requests.reply.hint" : "requests.reply.hint.with"))
                    }
                    Section {
                        Button(L10n.tr(store.info.searching.contains(report.id) ? "requests.report.searching" : "requests.report.better")) {
                            store.betterCopy(report.id)
                        }
                        .disabled(store.info.searching.contains(report.id))
                    }
                }
            }
            .navigationTitle(RequestsWords.titleWithYear(report.titleName, report.year))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("requests.done")) { dismiss() }
                }
                if manager {
                    ToolbarItem(placement: .confirmationAction) {
                        if store.info.busy.contains(report.id) {
                            ProgressView()
                        } else if report.isOpen {
                            Button(L10n.tr("requests.report.mark.fixed")) {
                                Task {
                                    if await store.fix(report.id, reply: String(reply.prefix(500))) { dismiss() }
                                }
                            }
                        } else {
                            Button(L10n.tr("requests.report.reopen")) {
                                Task {
                                    if await store.reopen(report.id) { dismiss() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

struct ReportProblemView: View {
    let name: String
    @State private var store: ReportProblemStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openRoute) private var openRoute
    @State private var note = ""

    init(titleId: Int64, name: String, season: Int, itemId: Int64, albumId: Int64, versionId: Int64) {
        self.name = name
        _store = State(initialValue: ReportProblemStore(titleId: titleId, season: season, itemId: itemId, albumId: albumId, versionId: versionId))
    }

    private var info: ReportProblemInfo { store.info }

    var body: some View {
        NavigationStack {
            Group {
                if let sent = info.sent {
                    VStack {
                        AskDoneView(
                            title: L10n.tr("requests.report.thanks"),
                            message: RequestsWords.reportKind(sent.kind) + " · " + RequestsWords.reportFacts(sent)
                        ) { dismiss() }
                        Button(L10n.tr("requests.report.see.yours")) { openRoute(.reports) }
                            .padding(.bottom, Theme.Spacing.xLarge)
                    }
                } else {
                    form
                }
            }
            .navigationTitle(L10n.tr("requests.report.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                if info.sent == nil {
                    ToolbarItem(placement: .confirmationAction) {
                        if info.busy {
                            ProgressView()
                        } else {
                            Button(L10n.tr("requests.report.send")) { store.send() }
                        }
                    }
                }
            }
        }
        .task { await store.watch() }
    }

    private var form: some View {
        Form {
            if !name.isEmpty {
                Section { Text(name).font(.headline) }
            }
            Section {
                ForEach(ReportKindInfo.allCases) { kind in
                    ChoiceRow(title: RequestsWords.reportKind(kind), selected: info.kind == kind) { store.setKind(kind) }
                }
            } header: {
                Text(L10n.tr("requests.report.what"))
            } footer: {
                Text(info.kind.map { RequestsWords.reportHint($0) } ?? L10n.tr("requests.report.pick"))
            }
            Section {
                TextField(L10n.tr("requests.report.note"), text: $note, prompt: Text(L10n.tr("requests.report.note.placeholder")), axis: .vertical)
                    .lineLimit(3 ... 8)
                    .onChange(of: note) { _, value in
                        let cut = String(value.prefix(500))
                        if cut != value { note = cut }
                        store.setNote(cut)
                    }
            } footer: {
                VStack(alignment: .leading) {
                    Text(L10n.format("requests.report.left", info.left))
                    if info.missingKind {
                        Text(L10n.tr("requests.report.pick.first")).foregroundStyle(Theme.failText)
                    }
                    if let error = info.error {
                        Text(RequestsWords.error(error)).foregroundStyle(Theme.failText)
                    }
                }
            }
        }
    }
}
