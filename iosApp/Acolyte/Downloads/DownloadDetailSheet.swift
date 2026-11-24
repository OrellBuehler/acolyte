import SwiftUI

struct DownloadDetailSheet: View {
    let list: DownloadsStore
    let key: String
    let onAction: @MainActor (DownloadItemInfo, DownloadActionKind) -> Void
    let onOpenTitle: @MainActor (Int64) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var detail: DownloadDetailStore

    init(
        list: DownloadsStore,
        key: String,
        onAction: @escaping @MainActor (DownloadItemInfo, DownloadActionKind) -> Void,
        onOpenTitle: @escaping @MainActor (Int64) -> Void
    ) {
        self.list = list
        self.key = key
        self.onAction = onAction
        self.onOpenTitle = onOpenTitle
        _detail = State(initialValue: list.makeDetailStore(key: key))
    }

    var body: some View {
        let item = list.info.item(key)
        NavigationStack {
            Group {
                if let item {
                    content(item)
                } else {
                    Color.clear
                }
            }
            .background(Theme.background)
            .navigationTitle(item?.title ?? "")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.close")) { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
        .task { await detail.watch() }
        .task { await detail.poll() }
        .onChange(of: item) { _, value in
            if value == nil {
                dismiss()
            } else {
                detail.syncRow()
            }
        }
    }

    private func content(_ item: DownloadItemInfo) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                header(item)
                actions(item)
                if !item.decisions.isEmpty {
                    decisions(item)
                }
                tabs
                tabContent
                footer
            }
            .padding(Theme.Spacing.screen)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func header(_ item: DownloadItemInfo) -> some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            DlPosterThumb(source: item.poster, name: item.title, square: item.square, width: item.square ? 64 : 56)
            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                if !item.subLine.display.isEmpty {
                    Text(item.subLine.display)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Text(item.healthLine)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(item.tone.labelColor)
                if let problem = item.problem {
                    Text(problem.kind.label)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(problem.tone.labelColor)
                }
                if let note = item.detail {
                    Text(note.message)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                if !item.history {
                    ProgressView(value: min(max(item.progress, 0), 1))
                        .tint(item.bar.color)
                        .padding(.top, Theme.Spacing.xSmall)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func isPrimary(_ action: DownloadActionKind) -> Bool {
        switch action {
        case .pause, .resume, .retry, .openTitle, .remove: true
        default: false
        }
    }

    @ViewBuilder
    private func actions(_ item: DownloadItemInfo) -> some View {
        let primary = item.actions.filter { isPrimary($0) }
        let rest = item.actions.filter { !isPrimary($0) }
        let busy = list.info.busy.contains(item.key)
        if !item.actions.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(primary) { action in
                        Button {
                            onAction(item, action)
                        } label: {
                            Label(action.label, systemImage: action.symbol)
                        }
                        .buttonStyle(.bordered)
                        .tint(action == .remove ? Theme.fail : Theme.accent)
                        .disabled(busy)
                    }
                    if !rest.isEmpty {
                        Menu {
                            DownloadActionMenuItems(actions: rest) { action in onAction(item, action) }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                                .font(.title3)
                                .frame(width: 36, height: 36)
                        }
                        .disabled(busy)
                        .accessibilityLabel(L10n.tr("downloads.more.actions"))
                    }
                }
            }
        }
    }

    private func decisions(_ item: DownloadItemInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("downloads.decide.title"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.failText)
            ForEach(item.decisions) { group in
                DecisionCardView(group: group, store: list, onOpenTitle: onOpenTitle)
            }
        }
    }

    private var tabs: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(detail.info.tabs) { tab in
                        DlChipButton(text: tab.label, selected: detail.info.tab == tab) { detail.setTab(tab) }
                    }
                }
            }
            if detail.info.readFailed {
                HStack {
                    Text(L10n.tr("downloads.detail.read.failed"))
                        .font(.footnote)
                        .foregroundStyle(Theme.waitText)
                    Spacer(minLength: Theme.Spacing.small)
                    Button(L10n.tr("common.retry")) { detail.refresh() }
                        .buttonStyle(.borderless)
                }
            }
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        let info = detail.info
        if !info.live && info.tab != .info {
            note(L10n.tr("downloads.detail.watch"))
        } else if info.reading && info.tab != .info {
            note(L10n.tr("downloads.detail.reading"))
        } else {
            switch info.tab {
            case .info: InfoTab(info: info)
            case .files: FilesTab(info: info, store: detail)
            case .peers: PeersTab(info: info)
            case .trackers: TrackersTab(info: info, store: detail)
            case .servers: ServersTab(info: info)
            case .log: LogTab(info: info)
            }
        }
    }

    @ViewBuilder
    private var footer: some View {
        if let notice = detail.info.notice {
            Text(notice.message)
                .font(.footnote)
                .foregroundStyle(Theme.okText)
        }
        if let error = detail.info.actionError, !detail.info.needsReauth {
            Text(error.detailed)
                .font(.footnote)
                .foregroundStyle(Theme.failText)
        }
    }

    private func note(_ text: String) -> some View {
        Text(text)
            .font(.callout)
            .foregroundStyle(Theme.textMuted)
    }
}

private struct EmptyNote: View {
    let title: String
    let message: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            Text(message)
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
        }
    }
}

private struct InfoTab: View {
    let info: DownloadDetailInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            if info.info.isEmpty {
                Text(L10n.tr("downloads.detail.nothing"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            } else {
                if info.torrentError {
                    Text(L10n.tr("downloads.detail.read.failed"))
                        .font(.footnote)
                        .foregroundStyle(Theme.waitText)
                }
                ForEach(info.info) { group in
                    DlSheetSection(title: group.kind.label) {
                        ForEach(group.facts) { fact in
                            DlFactRow(label: fact.kind.label, value: DownloadsText.factValue(fact))
                        }
                    }
                }
            }
        }
    }
}

private struct FilesTab: View {
    let info: DownloadDetailInfo
    let store: DownloadDetailStore
    @State private var filter = ""
    @State private var password = ""
    @State private var code = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.needsReauth { reauth }
            if info.fileCount == 0 {
                EmptyNote(title: L10n.tr("downloads.files.empty.title"), message: L10n.tr("downloads.files.empty"))
            } else {
                if !info.singleFile {
                    TextField(L10n.tr("downloads.files.filter"), text: $filter)
                        .textFieldStyle(.roundedBorder)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .onChange(of: filter) { _, value in store.setFileFilter(value) }
                }
                if info.fileTree.isEmpty {
                    Text(L10n.tr("downloads.files.none.match"))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                }
                ForEach(info.fileTree) { node in
                    nodeRow(node)
                }
            }
        }
    }

    private var reauth: some View {
        DlPanel {
            Text(L10n.tr("downloads.reauth.files"))
                .font(.callout)
            SecureField(L10n.tr("downloads.reauth.password"), text: $password)
                .textFieldStyle(.roundedBorder)
            TextField(L10n.tr("downloads.reauth.code"), text: $code)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.numberPad)
            Button(L10n.tr("downloads.reauth.confirm")) { confirm() }
                .buttonStyle(.borderedProminent)
                .disabled(password.isEmpty)
        }
    }

    private func confirm() {
        let pass = password
        let second = code
        Task {
            let ok = await store.reauth(password: pass, code: second)
            if ok {
                password = ""
                code = ""
            }
        }
    }

    private func nodeRow(_ node: FileNodeInfo) -> some View {
        HStack(alignment: .center, spacing: Theme.Spacing.small) {
            VStack(alignment: .leading, spacing: 2) {
                Text(node.folder ? node.name + "/" : node.name)
                    .font(node.folder ? Font.subheadline.weight(.semibold) : Font.footnote)
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                Text(DlFormat.size(node.size) + " · " + String(DlFormat.percent(node.progress)) + "%")
                    .font(.caption)
                    .foregroundStyle(Theme.textMuted)
            }
            Spacer(minLength: Theme.Spacing.small)
            Menu {
                ForEach(DownloadsText.filePriorities, id: \.self) { value in
                    Button(DownloadsText.filePriority(value)) {
                        store.setFilePriority(indexes: node.indexes, priority: value)
                    }
                }
            } label: {
                Text(DownloadsText.filePriority(node.priority))
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(info.canEdit ? Theme.accentText : Theme.textFaint)
            }
            .disabled(!info.canEdit || info.busy)
        }
        .padding(.leading, CGFloat(node.depth) * 12)
    }
}

private struct PeersTab: View {
    let info: DownloadDetailInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.peers.isEmpty {
                EmptyNote(title: L10n.tr("downloads.peers.empty.title"), message: L10n.tr("downloads.peers.empty"))
            }
            ForEach(info.peers) { peer in
                VStack(alignment: .leading, spacing: 2) {
                    Text([peer.address, peer.client].filter { !$0.isEmpty }.joined(separator: " · "))
                        .font(.footnote)
                        .foregroundStyle(Theme.text)
                    Text(speedLine(peer))
                        .font(.caption)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    private func speedLine(_ peer: PeerRowInfo) -> String {
        let down = L10n.format("downloads.speed.down", DlFormat.speed(peer.speed))
        let up = L10n.format("downloads.speed.up", DlFormat.speed(peer.upSpeed))
        return String(DlFormat.percent(peer.progress)) + "% · " + down + " · " + up
    }
}

private struct TrackersTab: View {
    let info: DownloadDetailInfo
    let store: DownloadDetailStore
    @State private var editing: TrackerRowInfo?
    @State private var adding = ""
    @State private var bad: [String] = []

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.trackers.isEmpty {
                EmptyNote(title: L10n.tr("downloads.trackers.empty.title"), message: L10n.tr("downloads.trackers.empty"))
            }
            ForEach(info.trackers) { tracker in
                trackerRow(tracker)
            }
            if info.canEdit { addSection }
        }
        .sheet(item: $editing) { tracker in
            TrackerEditSheet(tracker: tracker, store: store)
        }
    }

    private func trackerRow(_ tracker: TrackerRowInfo) -> some View {
        let editable = info.canEdit && !tracker.fixed && !tracker.ref.isEmpty
        return VStack(alignment: .leading, spacing: 2) {
            Text(tracker.url)
                .font(.footnote)
                .foregroundStyle(tracker.fixed ? Theme.textMuted : Theme.text)
                .lineLimit(2)
            Text(words(tracker))
                .font(.caption)
                .foregroundStyle(Theme.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture {
            if editable { editing = tracker }
        }
        .accessibilityAddTraits(editable ? .isButton : [])
    }

    private func words(_ tracker: TrackerRowInfo) -> String {
        var parts: [String] = []
        parts.append(tracker.fixed ? L10n.tr("downloads.tracker.peer.source") : DownloadsText.trackerStatus(tracker.status))
        if !tracker.fixed { parts.append(L10n.format("downloads.tracker.tier", tracker.tier)) }
        parts.append(L10n.format("downloads.tracker.counts", tracker.seeds, tracker.peers))
        if !tracker.message.isEmpty { parts.append(tracker.message) }
        return parts.joined(separator: " · ")
    }

    private var addSection: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Divider()
            Text(L10n.tr("downloads.trackers.add.label"))
                .font(.subheadline.weight(.semibold))
            TextField(L10n.tr("downloads.trackers.add.hint"), text: $adding, axis: .vertical)
                .lineLimit(2...5)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .onChange(of: adding) { _, _ in bad = [] }
            if let line = bad.first(where: { !$0.isEmpty }) {
                Text(L10n.format("downloads.tracker.bad", line))
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
            }
            Button(L10n.tr("downloads.trackers.add")) { add() }
                .buttonStyle(.bordered)
                .disabled(info.busy || adding.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
    }

    private func add() {
        let text = adding
        Task {
            let left = await store.addTrackers(text)
            if left.isEmpty { adding = "" } else { bad = left }
        }
    }
}

private struct TrackerEditSheet: View {
    let tracker: TrackerRowInfo
    let store: DownloadDetailStore
    @Environment(\.dismiss) private var dismiss
    @State private var url = ""
    @State private var tier: String

    init(tracker: TrackerRowInfo, store: DownloadDetailStore) {
        self.tracker = tracker
        self.store = store
        _tier = State(initialValue: String(tracker.tier))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(tracker.url)
                        .font(.footnote)
                    Text(L10n.tr("downloads.tracker.hidden"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    TextField(L10n.tr("downloads.tracker.new.address"), text: $url)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    LabeledContent(L10n.tr("downloads.tracker.tier.label")) {
                        TextField("0", text: $tier)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
                Section {
                    Button(L10n.tr("downloads.action.remove"), role: .destructive) {
                        store.removeTracker(ref: tracker.ref)
                        dismiss()
                    }
                }
            }
            .navigationTitle(L10n.tr("downloads.tracker.edit"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.save")) {
                        let newTier = Int(tier).flatMap { $0 != tracker.tier ? $0 : nil }
                        store.editTracker(ref: tracker.ref, url: url, tier: newTier)
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}

private struct ServersTab: View {
    let info: DownloadDetailInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.servers.isEmpty {
                EmptyNote(title: L10n.tr("downloads.servers.empty.title"), message: L10n.tr("downloads.servers.empty"))
            }
            ForEach(info.servers) { row in
                HStack(alignment: .firstTextBaseline) {
                    Text(row.name)
                        .font(.footnote)
                        .foregroundStyle(Theme.text)
                    Spacer(minLength: Theme.Spacing.small)
                    Text(detail(row))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .multilineTextAlignment(.trailing)
                }
            }
        }
    }

    private func detail(_ row: ServerRowInfo) -> String {
        var parts: [String] = []
        if !row.job.isEmpty { parts.append(row.job) }
        if !row.found.isEmpty { parts.append(L10n.format("downloads.server.found", row.found)) }
        return parts.joined(separator: " · ")
    }
}

private struct LogDayGroup: Identifiable {
    let id: Int
    let day: DlDay?
    var entries: [LogEntryInfo]
}

private struct LogTab: View {
    let info: DownloadDetailInfo

    private var groups: [LogDayGroup] {
        var result: [LogDayGroup] = []
        for entry in info.log {
            if let index = result.firstIndex(where: { $0.day == entry.day }) {
                result[index].entries.append(entry)
            } else {
                result.append(LogDayGroup(id: result.count, day: entry.day, entries: [entry]))
            }
        }
        return result
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if info.repair.isEmpty && info.log.isEmpty {
                EmptyNote(title: L10n.tr("downloads.log.empty.title"), message: L10n.tr("downloads.log.empty"))
            }
            ForEach(info.repair) { step in
                VStack(alignment: .leading, spacing: 2) {
                    Text(DownloadsText.repairLabel(step.stage))
                        .font(.subheadline.weight(.semibold))
                    ForEach(Array(step.lines.enumerated()), id: \.offset) { line in
                        Text(line.element)
                            .font(.footnote)
                    }
                }
            }
            ForEach(groups) { group in
                dayGroup(group)
            }
        }
    }

    private func dayGroup(_ group: LogDayGroup) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            if let day = group.day {
                Text(DownloadsText.dayHeading(day, today: .today, yesterday: true))
                    .font(.subheadline.weight(.semibold))
                    .padding(.top, Theme.Spacing.small)
            }
            ForEach(group.entries) { entry in
                HStack(alignment: .firstTextBaseline, spacing: Theme.Spacing.small) {
                    Text(entry.at.map { DownloadsText.clock($0) } ?? "")
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    Text((entry.warn ? "⚠ " : "") + entry.summary)
                        .font(.footnote)
                        .foregroundStyle(entry.warn ? Theme.waitText : Theme.text)
                }
            }
        }
    }
}

private enum DecisionConfirm {
    case importFile(DecisionInfo)
    case turnDown(DecisionInfo, pack: Bool)
    case stop(DecisionInfo, pack: Bool)
}

private struct DecisionCardView: View {
    let group: DecisionGroupInfo
    let store: DownloadsStore
    let onOpenTitle: @MainActor (Int64) -> Void
    @State private var why = false
    @State private var editing = false
    @State private var edition = ""
    @State private var confirm: DecisionConfirm?
    @State private var showConfirm = false

    private var lead: DecisionInfo { group.lead }

    private var busy: Bool {
        store.info.busy.contains(group.key) || store.info.busy.contains("slot-\(lead.slotId)")
    }

    private var leadStep: DecisionStepKind {
        if group.pack { return group.packImportAgain ? .importAgain : .search }
        return lead.plan.lead
    }

    private var moreSteps: [DecisionStepKind] {
        if group.pack { return leadStep == .importAgain ? [.search] : [] }
        return lead.plan.more
    }

    var body: some View {
        DlPanel {
            summary
            if !metaText.isEmpty {
                Text(metaText)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            hint
            if editing { editionForm }
            actionRow
            if why { whyBlock }
        }
        .confirmationDialog(
            L10n.tr("downloads.decide.title"),
            isPresented: $showConfirm,
            titleVisibility: .hidden,
            presenting: confirm
        ) { pending in
            confirmButton(pending)
        } message: { pending in
            Text(confirmMessage(pending))
        }
    }

    @ViewBuilder
    private var summary: some View {
        if group.pack {
            let total = max(lead.packTotal, group.items.count)
            Text(
                lead.kind == "artist"
                    ? L10n.format("downloads.decide.pack.tracks", group.items.count, total)
                    : L10n.format("downloads.decide.pack.episodes", group.items.count, total)
            )
            .font(.callout)
            ForEach(group.items) { item in
                Text(packLine(item))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        } else {
            Text(lead.name)
                .font(.subheadline.weight(.semibold))
            Text(lead.reasonLine.message)
                .font(.callout)
        }
    }

    private func packLine(_ item: DecisionInfo) -> String {
        let label = (item.episodeLabel + " " + item.episodeName).trimmingCharacters(in: .whitespaces)
        return label + " · " + item.reasonLine.message
    }

    private var metaText: String {
        var parts: [String] = []
        if !lead.version.isEmpty { parts.append(lead.version) }
        if let since = Format.date(iso: lead.since) { parts.append(Format.relative(since)) }
        if let minutes = lead.retryMinutes(now: Date()) {
            parts.append(L10n.format("downloads.decide.retry.in", minutes))
        }
        return parts.joined(separator: " · ")
    }

    @ViewBuilder
    private var hint: some View {
        switch lead.plan.hint {
        case .pathPair:
            Text(L10n.tr("downloads.decide.hint.path"))
                .font(.footnote)
                .foregroundStyle(Theme.waitText)
        case .matchByHand:
            Text(L10n.tr("downloads.decide.hint.match"))
                .font(.footnote)
                .foregroundStyle(Theme.waitText)
        case .none:
            EmptyView()
        }
    }

    private var editionForm: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            TextField(L10n.tr("downloads.decide.edition.label"), text: $edition)
                .textFieldStyle(.roundedBorder)
                .onChange(of: edition) { _, value in
                    if value.count > 80 { edition = String(value.prefix(80)) }
                }
            Button(L10n.tr("downloads.decide.edition.save")) { saveEdition() }
                .buttonStyle(.borderedProminent)
                .disabled(edition.trimmingCharacters(in: .whitespaces).isEmpty || busy)
        }
    }

    private func saveEdition() {
        let slot = lead.slotId
        let text = edition
        Task {
            let ok = await store.setEdition(slotId: slot, edition: text)
            if ok { editing = false }
        }
    }

    private var actionRow: some View {
        HStack(spacing: Theme.Spacing.small) {
            Button(leadLabel) { run(leadStep) }
                .buttonStyle(.borderedProminent)
                .disabled(busy || (leadStep == .importAgain && lead.reading))
            if !group.pack, let beside = lead.plan.beside {
                Button(beside.label) { run(beside) }
                    .buttonStyle(.bordered)
                    .disabled(busy || lead.reading)
            }
            Menu {
                ForEach(moreSteps, id: \.self) { step in
                    Button(step.label) { run(step) }
                }
                if lead.plan.turnDown {
                    Button(group.pack ? L10n.tr("downloads.decide.pack.turn.down") : L10n.tr("downloads.decide.turn.down")) {
                        ask(.turnDown(lead, pack: group.pack))
                    }
                }
                Button(
                    group.pack ? L10n.tr("downloads.decide.pack.stop") : L10n.tr("downloads.decide.stop"),
                    role: .destructive
                ) {
                    ask(.stop(lead, pack: group.pack))
                }
                if lead.kind != "artist" {
                    Button(L10n.tr("downloads.action.open")) { onOpenTitle(lead.titleId) }
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.title3)
                    .frame(width: 36, height: 36)
            }
            .disabled(busy)
            .accessibilityLabel(L10n.tr("downloads.more.actions"))
            Spacer(minLength: 0)
            Button(L10n.tr("downloads.decide.why")) { why.toggle() }
                .buttonStyle(.borderless)
        }
    }

    private var leadLabel: String {
        if group.pack && leadStep == .search { return L10n.tr("downloads.decide.pack.search") }
        return leadStep.label
    }

    private var whyBlock: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(lead.why.message)
                .font(.footnote)
            if !lead.reason.trimmingCharacters(in: .whitespaces).isEmpty {
                Text(lead.reason)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if lead.hasCopy {
                Text(L10n.tr("downloads.decide.has.copy"))
                    .font(.footnote)
            }
            if store.info.canSettings && !lead.releaseName.isEmpty {
                Text(L10n.format("downloads.decide.release", lead.releaseName))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }

    private func run(_ step: DecisionStepKind) {
        switch step {
        case .search: store.decide(slotId: lead.slotId, action: .search, pack: group.pack)
        case .importAgain: store.importAgain(slotId: lead.slotId)
        case .importFile: ask(.importFile(lead))
        case .setEdition: editing = true
        case .openVersions, .openReleases: onOpenTitle(lead.titleId)
        }
    }

    private func ask(_ next: DecisionConfirm) {
        confirm = next
        showConfirm = true
    }

    private func confirmMessage(_ pending: DecisionConfirm) -> String {
        switch pending {
        case let .importFile(decision):
            return decision.hasCopy
                ? L10n.format("downloads.decide.import.replace", decision.importFile, decision.name)
                : L10n.format("downloads.decide.import.confirm", decision.importFile, decision.name)
        case let .turnDown(decision, pack):
            if pack { return L10n.tr("downloads.decide.turn.down.pack") }
            return L10n.format(
                "downloads.decide.turn.down.confirm",
                decision.releaseName.isEmpty ? decision.name : decision.releaseName
            )
        case let .stop(decision, pack):
            return L10n.format("downloads.decide.stop.confirm", pack ? L10n.tr("downloads.decide.these") : decision.name)
        }
    }

    @ViewBuilder
    private func confirmButton(_ pending: DecisionConfirm) -> some View {
        switch pending {
        case let .importFile(decision):
            Button(L10n.tr("downloads.decide.import.button")) {
                store.decide(slotId: decision.slotId, action: .importFile, pack: false)
            }
        case let .turnDown(decision, pack):
            Button(L10n.tr("downloads.decide.turn.down")) {
                store.decide(slotId: decision.slotId, action: .turnDown, pack: pack)
            }
        case let .stop(decision, pack):
            Button(L10n.tr("downloads.decide.stop.button"), role: .destructive) {
                store.decide(slotId: decision.slotId, action: .stop, pack: pack)
            }
        }
    }
}
