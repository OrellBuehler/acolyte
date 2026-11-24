import SwiftUI
import UniformTypeIdentifiers

struct DownloadsFiltersSheet: View {
    let store: DownloadsStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section(L10n.tr("downloads.filter.library")) {
                    ForEach(DlKindFilter.allCases) { kind in
                        DlChoiceRow(text: kind.label, selected: store.info.filter.kind == kind) { store.setKind(kind) }
                    }
                }
                Section(L10n.tr("downloads.filter.network")) {
                    ForEach(DlNetworkFilter.allCases) { network in
                        DlChoiceRow(text: network.label, selected: store.info.filter.network == network) {
                            store.setNetwork(network)
                        }
                    }
                }
                trackerSection
                sortSection
                Section {
                    Button(L10n.tr("downloads.filter.clear")) { store.clearFilters() }
                        .disabled(store.info.filter.filtersOn == 0)
                }
            }
            .navigationTitle(L10n.tr("downloads.filters"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.format("downloads.filter.show", store.info.items.count)) { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
    }

    @ViewBuilder
    private var trackerSection: some View {
        if !store.info.trackers.isEmpty {
            Section(L10n.tr("downloads.filter.trackers")) {
                DlChoiceRow(text: L10n.tr("downloads.filter.all.trackers"), selected: store.info.filter.tracker.isEmpty) {
                    store.setTracker("")
                }
                DlChoiceRow(text: L10n.tr("downloads.filter.tracker.problems"), selected: store.info.filter.tracker == "p:any") {
                    store.setTracker("p:any")
                }
                ForEach(store.info.trackers) { option in
                    DlChoiceRow(text: option.display, trailing: String(option.count), selected: store.info.filter.tracker == option.key) {
                        store.setTracker(option.key)
                    }
                }
            }
        }
    }

    private var sortSection: some View {
        Section(L10n.tr("downloads.sort")) {
            DlChoiceRow(text: L10n.tr("downloads.sort.default"), selected: store.info.filter.sort == nil) {
                store.setSort(nil)
            }
            ForEach(SortKeyKind.allCases) { key in
                DlChoiceRow(text: key.label, trailing: arrow(key), selected: store.info.filter.sort?.key == key) {
                    toggleSort(key)
                }
            }
        }
    }

    private func arrow(_ key: SortKeyKind) -> String? {
        guard let sort = store.info.filter.sort, sort.key == key else { return nil }
        return sort.descending ? "↓" : "↑"
    }

    private func toggleSort(_ key: SortKeyKind) {
        var descending = DlFormat.firstDescending(key)
        if let sort = store.info.filter.sort, sort.key == key {
            descending = !sort.descending
        }
        store.setSort(DlSortInfo(key: key, descending: descending))
    }
}

struct RemoveSheet: View {
    let store: DownloadsStore
    @Environment(\.dismiss) private var dismiss
    @State private var current: RemovePlanInfo
    @State private var deleteFiles: Bool
    @State private var turnDown: Bool
    @State private var searchAgain: Bool
    @State private var failed = false
    @State private var busy = false

    init(store: DownloadsStore, plan: RemovePlanInfo) {
        self.store = store
        _current = State(initialValue: plan)
        _deleteFiles = State(initialValue: plan.deleteFiles)
        _turnDown = State(initialValue: plan.turnDown)
        _searchAgain = State(initialValue: plan.searchAgain)
    }

    private var client: String {
        current.downloader.isEmpty ? L10n.tr("downloads.remove.the.downloader") : current.downloader
    }

    private var title: String {
        current.keys.count == 1
            ? L10n.tr("downloads.remove.title.one")
            : L10n.format("downloads.remove.title.many", current.keys.count)
    }

    private var confirmLabel: String {
        if deleteFiles && current.showDeleteFiles && current.keepsPath.isEmpty {
            return L10n.tr("downloads.remove.confirm.delete")
        }
        return L10n.tr("downloads.remove.confirm")
    }

    var body: some View {
        NavigationStack {
            Form {
                namesSection
                if current.showDeleteFiles { filesSection }
                if current.showTurnDown { turnDownSection }
                if current.blocked > 0 {
                    Section {
                        Text(
                            current.keys.isEmpty
                                ? L10n.tr("downloads.remove.blocked.one")
                                : L10n.format("downloads.remove.blocked.many", current.blocked)
                        )
                        .font(.footnote)
                        .foregroundStyle(Theme.waitText)
                    }
                }
                if failed {
                    Section {
                        Text(L10n.tr("downloads.remove.failed"))
                            .font(.footnote)
                            .foregroundStyle(Theme.failText)
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(confirmLabel, role: .destructive) { confirm() }
                        .disabled(busy || current.keys.isEmpty)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private var namesSection: some View {
        Section {
            if !current.single.isEmpty {
                Text(current.single)
                    .font(.headline)
                if !current.release.isEmpty && current.release != current.single {
                    Text(current.release)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            } else {
                ForEach(Array(current.names.enumerated()), id: \.offset) { entry in
                    Text("• " + entry.element)
                        .lineLimit(1)
                }
                if current.more > 0 {
                    Text(L10n.format("downloads.remove.and.more", current.more))
                        .foregroundStyle(Theme.textMuted)
                }
            }
            if let line = current.doneLine {
                Text(doneText(line))
                    .font(.callout)
            }
        }
    }

    private func doneText(_ line: RemoveDoneLineKind) -> String {
        switch line {
        case .doneSeeding: L10n.format("downloads.remove.done.seeding", client)
        case .doneIdle: L10n.format("downloads.remove.done.idle", client)
        case .partSeeding: L10n.format("downloads.remove.part.seeding", client)
        case .partIdle: L10n.format("downloads.remove.part.idle", client)
        }
    }

    private var filesHint: String {
        let clients = current.filesClients.isEmpty ? client : current.filesClients
        if !current.keepsPath.isEmpty { return L10n.format("downloads.remove.keeps.path", current.keepsPath) }
        if !deleteFiles { return L10n.format("downloads.remove.files.off", clients) }
        switch current.filesLine {
        case .path: return L10n.format("downloads.remove.files.path", current.filesPath, clients)
        case .one: return L10n.format("downloads.remove.files.one", clients)
        case .many: return L10n.format("downloads.remove.files.many", clients)
        }
    }

    private var filesSection: some View {
        Section {
            Toggle(L10n.tr("downloads.remove.delete.files"), isOn: $deleteFiles)
        } footer: {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(filesHint)
                if current.keepsSome {
                    Text(L10n.tr("downloads.remove.keeps.some"))
                }
            }
        }
    }

    private var turnDownSection: some View {
        Section {
            Toggle(L10n.tr("downloads.remove.turn.down"), isOn: $turnDown)
            Text(L10n.tr("downloads.remove.turn.down.hint"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Toggle(L10n.tr("downloads.remove.search.again"), isOn: $searchAgain)
                .disabled(!turnDown)
            Text(L10n.tr("downloads.remove.search.again.hint"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private func confirm() {
        busy = true
        let keys = current.keys
        let files = deleteFiles && current.showDeleteFiles
        let down = turnDown
        let again = searchAgain
        Task {
            let left = await store.remove(keys: keys, deleteFiles: files, turnDown: down, searchAgain: again)
            busy = false
            if left.isEmpty {
                dismiss()
            } else {
                failed = true
                current = current.remaining(left)
            }
        }
    }
}

struct DownloadsSpeedSheet: View {
    let store: DownloadsStore
    @Environment(\.dismiss) private var dismiss
    @State private var downs: [String] = []
    @State private var ups: [String] = []

    var body: some View {
        let targets = store.info.speedTargets
        NavigationStack {
            Form {
                Section {
                    Text(L10n.tr("downloads.speed.hint"))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                    if targets.isEmpty {
                        Text(L10n.tr("downloads.speed.none"))
                    }
                }
                ForEach(Array(targets.enumerated()), id: \.element.id) { entry in
                    targetSection(entry.offset, entry.element)
                }
            }
            .navigationTitle(L10n.tr("downloads.speed.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.save")) { save(targets) }
                        .disabled(targets.isEmpty || store.info.batchBusy)
                }
            }
            .task {
                await store.loadClients()
                reset(store.info.speedTargets)
            }
            .onChange(of: store.info.speedTargets) { _, value in reset(value) }
        }
        .presentationDetents([.medium, .large])
    }

    @ViewBuilder
    private func targetSection(_ index: Int, _ target: SpeedTargetInfo) -> some View {
        Section(target.name) {
            if index < downs.count && index < ups.count {
                LabeledContent(L10n.tr("downloads.speed.download")) {
                    TextField("0", text: $downs[index])
                        .keyboardType(.decimalPad)
                        .multilineTextAlignment(.trailing)
                }
                if target.torrent {
                    LabeledContent(L10n.tr("downloads.speed.upload")) {
                        TextField("0", text: $ups[index])
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
            }
            if let slow = target.slowMode {
                Button {
                    store.setSlowMode(id: target.id, on: !slow)
                } label: {
                    HStack {
                        Text(L10n.tr("downloads.speed.slow.mode"))
                            .foregroundStyle(Theme.text)
                        Spacer()
                        Toggle("", isOn: .constant(slow))
                            .labelsHidden()
                            .allowsHitTesting(false)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(.isToggle)
                .accessibilityValue(slow ? L10n.tr("downloads.value.yes") : L10n.tr("downloads.value.no"))
            }
        }
    }

    private func mb(_ value: Double) -> String {
        if value <= 0 { return "0" }
        let text = DlFormat.ratioText(value, decimals: 1)
        return text.hasSuffix(".0") ? String(text.dropLast(2)) : text
    }

    private func reset(_ targets: [SpeedTargetInfo]) {
        downs = targets.map { mb($0.downMb) }
        ups = targets.map { mb($0.upMb) }
    }

    private func number(_ text: String) -> Double? {
        Double(text.replacingOccurrences(of: ",", with: "."))
    }

    private func save(_ targets: [SpeedTargetInfo]) {
        var changed: [SpeedTargetInfo] = []
        for (index, target) in targets.enumerated() {
            let down = index < downs.count ? number(downs[index]) : nil
            let up = index < ups.count ? number(ups[index]) : nil
            changed.append(
                SpeedTargetInfo(
                    id: target.id,
                    name: target.name,
                    torrent: target.torrent,
                    downMb: down ?? target.downMb,
                    upMb: up ?? target.upMb,
                    slowMode: target.slowMode
                )
            )
        }
        Task {
            await store.saveSpeedLimits(changed)
            dismiss()
        }
    }
}

struct AddTorrentsSheet: View {
    let store: DownloadsStore
    @Environment(\.dismiss) private var dismiss
    @State private var targetId: Int64
    @State private var links = ""
    @State private var category: String
    @State private var savePath = ""
    @State private var paused = false
    @State private var bad: [String] = []
    @State private var files: [DlTorrentFile] = []
    @State private var dropped = 0
    @State private var importing = false

    init(store: DownloadsStore) {
        self.store = store
        let first = store.info.addTargets.first
        _targetId = State(initialValue: first?.id ?? 0)
        _category = State(initialValue: first?.category ?? "")
    }

    private var badText: String? {
        guard !bad.isEmpty else { return nil }
        if let line = bad.first(where: { !$0.isEmpty }) {
            return L10n.format("downloads.add.not.link", line)
        }
        return L10n.tr("downloads.add.empty")
    }

    var body: some View {
        let targets = store.info.addTargets
        NavigationStack {
            Form {
                Section {
                    TextField(L10n.tr("downloads.add.links.hint"), text: $links, axis: .vertical)
                        .lineLimit(3...8)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .onChange(of: links) { _, _ in bad = [] }
                } header: {
                    Text(L10n.tr("downloads.add.links"))
                } footer: {
                    if let badText {
                        Text(badText)
                            .foregroundStyle(Theme.failText)
                    }
                }
                Section {
                    Button(L10n.tr("downloads.add.files")) { importing = true }
                    if !files.isEmpty {
                        Text(L10n.format("downloads.add.files.picked", files.count))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                    if dropped > 0 {
                        Text(L10n.format("downloads.add.files.dropped", dropped))
                            .font(.footnote)
                            .foregroundStyle(Theme.waitText)
                    }
                }
                Section {
                    if targets.count > 1 {
                        Picker(L10n.tr("downloads.add.downloader"), selection: $targetId) {
                            ForEach(targets) { target in
                                Text(target.name).tag(target.id)
                            }
                        }
                    }
                    LabeledContent(L10n.tr("downloads.add.category")) {
                        TextField("", text: $category)
                            .multilineTextAlignment(.trailing)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    }
                    LabeledContent(L10n.tr("downloads.add.save.in")) {
                        TextField("", text: $savePath)
                            .multilineTextAlignment(.trailing)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    }
                    Toggle(L10n.tr("downloads.add.paused"), isOn: $paused)
                } footer: {
                    Text(L10n.tr("downloads.add.save.hint"))
                }
            }
            .navigationTitle(L10n.tr("downloads.add.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.add.submit")) { submit() }
                        .disabled(targets.isEmpty || store.info.batchBusy)
                }
            }
            .onChange(of: targetId) { _, id in
                category = targets.first(where: { $0.id == id })?.category ?? ""
            }
            .fileImporter(isPresented: $importing, allowedContentTypes: [.data], allowsMultipleSelection: true) { result in
                pick(result)
            }
        }
        .presentationDetents([.large])
    }

    private func pick(_ result: Result<[URL], Error>) {
        guard case let .success(urls) = result else { return }
        var picked: [DlTorrentFile] = []
        for url in urls {
            let scoped = url.startAccessingSecurityScopedResource()
            defer { if scoped { url.stopAccessingSecurityScopedResource() } }
            guard url.pathExtension.lowercased() == "torrent", let data = try? Data(contentsOf: url), data.count <= 16 << 20 else { continue }
            picked.append(DlTorrentFile(name: url.lastPathComponent, data: data))
        }
        files = picked
        dropped = urls.count - picked.count
        bad = []
    }

    private func submit() {
        let id = targetId
        let text = links
        let cat = category
        let path = savePath
        let hold = paused
        let uploads = files
        Task {
            let left = await store.addTorrents(downloaderId: id, text: text, category: cat, savePath: path, paused: hold, files: uploads)
            if left.isEmpty { dismiss() } else { bad = left }
        }
    }
}

private enum LimitMode: Hashable {
    case global
    case unlimited
    case custom

    init(_ value: Double) {
        if value == -2 {
            self = .global
        } else if value < 0 {
            self = .unlimited
        } else {
            self = .custom
        }
    }

    func resolve(_ value: Double) -> Double {
        switch self {
        case .global: -2
        case .unlimited: -1
        case .custom: value
        }
    }
}

private struct LimitGroup: View {
    let title: String
    @Binding var mode: LimitMode
    @Binding var value: String

    var body: some View {
        Section(title) {
            Picker(title, selection: $mode) {
                Text(L10n.tr("downloads.limit.global")).tag(LimitMode.global)
                Text(L10n.tr("downloads.limit.none")).tag(LimitMode.unlimited)
                Text(L10n.tr("downloads.limit.set")).tag(LimitMode.custom)
            }
            .pickerStyle(.menu)
            if mode == .custom {
                TextField(title, text: $value)
                    .keyboardType(.decimalPad)
            }
        }
    }
}

struct SteerSheet: View {
    let store: DownloadsStore
    let mode: DownloadActionKind
    let keys: [String]
    @Environment(\.dismiss) private var dismiss
    private let start: ShareLimitsInfo
    @State private var ratioMode: LimitMode
    @State private var ratio: String
    @State private var seedMode: LimitMode
    @State private var seed: String
    @State private var idleMode: LimitMode
    @State private var idle: String
    @State private var thenAction: String
    @State private var category = ""
    @State private var folder = ""
    @State private var down = "0"
    @State private var up = "0"
    @State private var problem = ""

    init(store: DownloadsStore, mode: DownloadActionKind, keys: [String]) {
        self.store = store
        self.mode = mode
        self.keys = keys
        let begin = store.steerStart(keys: keys)
        start = begin
        _ratioMode = State(initialValue: LimitMode(begin.ratio))
        _ratio = State(initialValue: begin.ratio >= 0 ? DlFormat.shortRatio(begin.ratio) : "2")
        _seedMode = State(initialValue: LimitMode(Double(begin.seedingMinutes)))
        _seed = State(initialValue: begin.seedingMinutes >= 0 ? String(begin.seedingMinutes) : "1440")
        _idleMode = State(initialValue: LimitMode(Double(begin.inactiveMinutes)))
        _idle = State(initialValue: begin.inactiveMinutes >= 0 ? String(begin.inactiveMinutes) : "60")
        _thenAction = State(initialValue: begin.action)
    }

    private var title: String {
        switch mode {
        case .seedingRules: L10n.tr("downloads.seeding.title")
        case .setLocation: L10n.tr("downloads.location.title")
        default: L10n.tr("downloads.speeds.title")
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(keys.count == 1 ? L10n.tr("downloads.steer.one") : L10n.format("downloads.steer.many", keys.count))
                        .foregroundStyle(Theme.textMuted)
                }
                switch mode {
                case .seedingRules: seedingSections
                case .setLocation: locationSection
                default: speedsSection
                }
                if !problem.isEmpty {
                    Section {
                        Text(problem)
                            .foregroundStyle(Theme.failText)
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(mode == .setLocation ? L10n.tr("downloads.location.move") : L10n.tr("downloads.save")) { save() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    @ViewBuilder
    private var seedingSections: some View {
        Section {
            Text(L10n.tr("downloads.seeding.hint"))
                .font(.callout)
        }
        LimitGroup(title: L10n.tr("downloads.seeding.ratio"), mode: $ratioMode, value: $ratio)
        LimitGroup(title: L10n.tr("downloads.seeding.time"), mode: $seedMode, value: $seed)
        LimitGroup(title: L10n.tr("downloads.seeding.idle"), mode: $idleMode, value: $idle)
        Section {
            Picker(L10n.tr("downloads.seeding.then"), selection: $thenAction) {
                Text(L10n.tr("downloads.then.default")).tag("")
                Text(L10n.tr("downloads.then.stop")).tag("stop")
                Text(L10n.tr("downloads.then.remove")).tag("remove")
                if start.action == "remove_files" {
                    Text(L10n.tr("downloads.then.remove.files")).tag("remove_files")
                }
                if start.action == "super_seed" {
                    Text(L10n.tr("downloads.then.super")).tag("super_seed")
                }
            }
            .pickerStyle(.menu)
        }
    }

    private var locationSection: some View {
        Section {
            LabeledContent(L10n.tr("downloads.location.category")) {
                TextField("", text: $category)
                    .multilineTextAlignment(.trailing)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            LabeledContent(L10n.tr("downloads.location.folder")) {
                TextField(L10n.tr("downloads.location.folder.hint"), text: $folder)
                    .multilineTextAlignment(.trailing)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
        } header: {
            Text(L10n.tr("downloads.location.hint"))
                .textCase(nil)
        }
    }

    private var speedsSection: some View {
        Section {
            LabeledContent(L10n.tr("downloads.speed.download")) {
                TextField("0", text: $down)
                    .keyboardType(.decimalPad)
                    .multilineTextAlignment(.trailing)
            }
            LabeledContent(L10n.tr("downloads.speed.upload")) {
                TextField("0", text: $up)
                    .keyboardType(.decimalPad)
                    .multilineTextAlignment(.trailing)
            }
        } header: {
            Text(L10n.tr("downloads.speeds.hint"))
                .textCase(nil)
        }
    }

    private func number(_ text: String) -> Double? {
        guard let value = Double(text.replacingOccurrences(of: ",", with: ".")), value.isFinite else { return nil }
        return min(max(value, -3), 1e9)
    }

    private func save() {
        switch mode {
        case .seedingRules:
            saveSeeding()
        case .setLocation:
            if folder.trimmingCharacters(in: .whitespaces).isEmpty {
                problem = L10n.tr("downloads.location.needed")
                return
            }
            let cat = category
            let path = folder
            run { await store.setLocation(keys: keys, category: cat, folder: path) }
        default:
            let downKb = number(down) ?? 0
            let upKb = number(up) ?? 0
            run { await store.setTorrentSpeeds(keys: keys, downKb: downKb, upKb: upKb) }
        }
    }

    private func saveSeeding() {
        let r = ratioMode.resolve(number(ratio) ?? -3)
        let s = seedMode.resolve(number(seed) ?? -3)
        let i = idleMode.resolve(number(idle) ?? -3)
        let seedMinutes = Int64(s)
        let idleMinutes = Int64(i)
        if r < -2 || s < -2 || i < -2 || DlFormat.limitsProblem(ratio: r, seedingMinutes: seedMinutes, inactiveMinutes: idleMinutes) {
            problem = L10n.tr("downloads.limits.bad")
            return
        }
        let action = thenAction
        run {
            await store.setShareLimits(keys: keys, ratio: r, seedingMinutes: seedMinutes, inactiveMinutes: idleMinutes, action: action)
        }
    }

    private func run(_ work: @escaping @MainActor () async -> Bool) {
        Task {
            let ok = await work()
            if ok { dismiss() }
        }
    }
}

struct MatchesSheet: View {
    let store: DownloadsStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(L10n.tr("downloads.matches.description"))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                    if store.info.matchesPending > 0 {
                        Text(L10n.format("downloads.matches.pending", store.info.matchesPending))
                            .font(.footnote)
                    }
                    if store.info.matches.isEmpty {
                        Text(L10n.tr("downloads.matches.all.done"))
                    }
                }
                ForEach(store.info.matches) { match in
                    matchSection(match)
                }
            }
            .navigationTitle(L10n.tr("downloads.matches.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("downloads.matches.run")) { store.runMatches() }
                        .disabled(store.info.batchBusy)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.history.done")) { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
    }

    private func matchSection(_ match: DlMatchInfo) -> some View {
        let busy = store.info.busy.contains("match-\(match.id)")
        return Section {
            Text(match.name)
                .font(.footnote)
                .lineLimit(3)
            if match.candidates.isEmpty {
                Text(L10n.tr("downloads.matches.none.found"))
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(match.candidates) { candidate in
                HStack(spacing: Theme.Spacing.medium) {
                    DlPosterThumb(source: candidate.poster.isEmpty ? nil : candidate.poster, name: candidate.name, width: 36)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(candidate.name)
                            .font(.subheadline.weight(.semibold))
                        Text(
                            candidate.kind == "show"
                                ? L10n.format("downloads.matches.show", candidate.year)
                                : L10n.format("downloads.matches.movie", candidate.year)
                        )
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    }
                    Spacer(minLength: Theme.Spacing.small)
                    Button(L10n.tr("downloads.matches.open")) {
                        store.pickMatch(id: match.id, candidate: candidate.id, notMedia: false)
                    }
                    .buttonStyle(.bordered)
                    .disabled(busy)
                }
            }
            HStack {
                Button(L10n.tr("downloads.matches.not.media")) {
                    store.pickMatch(id: match.id, candidate: nil, notMedia: true)
                }
                .disabled(busy)
                Spacer()
                Button(L10n.tr("downloads.matches.skip")) {
                    store.pickMatch(id: match.id, candidate: nil, notMedia: false)
                }
                .disabled(busy)
                if busy { ProgressView() }
            }
            .buttonStyle(.borderless)
        }
    }
}
