import SwiftUI

enum DownloadsSheet: Identifiable {
    case filters
    case speed
    case add
    case matches
    case detail(String)
    case remove(RemovePlanInfo)
    case steer(DownloadActionKind, [String])

    var id: String {
        switch self {
        case .filters: "filters"
        case .speed: "speed"
        case .add: "add"
        case .matches: "matches"
        case let .detail(key): "detail-" + key
        case let .remove(plan): "remove-" + plan.id
        case let .steer(mode, keys): "steer-" + mode.rawValue + "-" + keys.joined(separator: ",")
        }
    }
}

private struct MarkFailedAsk {
    let key: String
    let name: String
}

struct DownloadsView: View {
    let show: String
    let open: String

    @State private var store = DownloadsStore()
    @State private var search = ""
    @State private var sheet: DownloadsSheet?
    @State private var pendingSheet: DownloadsSheet?
    @State private var markFailedAsk: MarkFailedAsk?
    @State private var showMarkFailed = false
    @State private var pendingMarkFailed: MarkFailedAsk?
    @State private var priorityKey: String?
    @State private var showPriority = false
    @State private var opened = false

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            watcherEmpty
        } content: {
            list
        }
        .navigationTitle(title)
        .searchable(text: $search, prompt: L10n.tr("downloads.search.hint"))
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                if store.info.selecting {
                    selectionToolbar
                } else {
                    normalToolbar
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { bottomBar }
        .sheet(item: $sheet, onDismiss: afterDismiss) { current in
            sheetContent(current)
        }
        .confirmationDialog(
            markFailedTitle,
            isPresented: $showMarkFailed,
            titleVisibility: .visible,
            presenting: markFailedAsk
        ) { ask in
            Button(L10n.tr("downloads.action.mark.failed"), role: .destructive) {
                store.act(ask.key, .markFailed)
            }
        } message: { _ in
            Text(L10n.tr("downloads.mark.failed.message"))
        }
        .confirmationDialog(L10n.tr("downloads.action.priority"), isPresented: $showPriority, titleVisibility: .visible) {
            ForEach(UsenetPriorityKind.allCases) { priority in
                Button(priority.label) {
                    if let key = priorityKey { store.setPriority(key, priority) }
                }
            }
        }
        .navigationDestination(item: $store.route) { route in
            DlRouteDestination(route: route)
        }
        .task { await store.watch() }
        .task {
            await store.start(show: show)
            openRequested()
        }
        .task { await store.followLive() }
        .onChange(of: search) { _, value in store.setQuery(value) }
        .onChange(of: store.info.filter.query) { _, value in
            if value != search { search = value }
        }
        .onChange(of: store.info.items.count) { _, _ in openRequested() }
    }

    private var title: String {
        if store.info.selecting {
            return L10n.format("downloads.selected", store.info.selection.count)
        }
        return SectionID.downloads.title
    }

    private var markFailedTitle: String {
        guard let ask = markFailedAsk else { return "" }
        return L10n.format("downloads.mark.failed.title", ask.name)
    }

    private func openRequested() {
        guard !opened, !open.isEmpty, store.info.item(open) != nil else { return }
        opened = true
        sheet = .detail(open)
    }

    private func ask(_ next: DownloadsSheet) {
        if sheet != nil {
            pendingSheet = next
            sheet = nil
        } else {
            sheet = next
        }
    }

    private func askMarkFailed(_ next: MarkFailedAsk) {
        if sheet != nil {
            pendingMarkFailed = next
            sheet = nil
        } else {
            markFailedAsk = next
            showMarkFailed = true
        }
    }

    private func afterDismiss() {
        if let next = pendingSheet {
            pendingSheet = nil
            sheet = next
        } else if let next = pendingMarkFailed {
            pendingMarkFailed = nil
            markFailedAsk = next
            showMarkFailed = true
        }
    }

    private func handle(_ item: DownloadItemInfo, _ action: DownloadActionKind) {
        switch action {
        case .openTitle:
            sheet = nil
            store.openTitle(item.titleId)
        case .remove:
            if let plan = store.removePlan(keys: [item.key]) { ask(.remove(plan)) }
        case .markFailed:
            askMarkFailed(MarkFailedAsk(key: item.key, name: item.name))
        case .priority:
            priorityKey = item.key
            showPriority = true
        case .seedingRules, .setLocation, .speedLimits:
            ask(.steer(action, [item.key]))
        default:
            store.act(item.key, action)
        }
    }

    private func bulk(_ action: DownloadActionKind) {
        let keys = store.selectedKeys()
        switch action {
        case .remove:
            if let plan = store.removePlan(keys: keys) { ask(.remove(plan)) }
        case .seedingRules, .setLocation, .speedLimits:
            ask(.steer(action, keys))
        default:
            store.bulk(action)
        }
    }

    private func tap(_ item: DownloadItemInfo) {
        if store.info.selecting {
            if item.selectable { store.toggleSelect(item.key) }
        } else {
            sheet = .detail(item.key)
        }
    }

    private func startSelecting() {
        if let first = store.info.items.first(where: { $0.selectable }) {
            store.toggleSelect(first.key)
        }
    }

    @ViewBuilder
    private func sheetContent(_ current: DownloadsSheet) -> some View {
        switch current {
        case .filters:
            DownloadsFiltersSheet(store: store)
        case .speed:
            DownloadsSpeedSheet(store: store)
        case .add:
            AddTorrentsSheet(store: store)
        case .matches:
            MatchesSheet(store: store)
        case let .detail(key):
            DownloadDetailSheet(
                list: store,
                key: key,
                onAction: { item, action in handle(item, action) },
                onOpenTitle: { titleId in
                    sheet = nil
                    store.openTitle(titleId)
                }
            )
        case let .remove(plan):
            RemoveSheet(store: store, plan: plan)
        case let .steer(mode, keys):
            SteerSheet(store: store, mode: mode, keys: keys)
        }
    }

    private var watcherEmpty: some View {
        EmptyStateView(
            title: store.info.manager ? L10n.tr("downloads.empty.all.title") : L10n.tr("downloads.empty.watch.title"),
            message: store.info.manager ? L10n.tr("downloads.empty.manager") : L10n.tr("downloads.empty.watch"),
            systemImage: "arrow.down.to.line"
        )
    }

    @ViewBuilder
    private var normalToolbar: some View {
        let on = store.info.filter.filtersOn
        Button {
            ask(.filters)
        } label: {
            Label(
                on > 0 ? L10n.format("downloads.filters.on", on) : L10n.tr("downloads.filters"),
                systemImage: on > 0 ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle"
            )
        }
        if store.info.manager && !store.info.addTargets.isEmpty {
            Button {
                ask(.add)
            } label: {
                Label(L10n.tr("downloads.add"), systemImage: "plus")
            }
        }
        if store.info.manager {
            Menu {
                Button(L10n.tr("downloads.select"), systemImage: "checkmark.circle") { startSelecting() }
                Button(L10n.tr("downloads.start.all"), systemImage: "play") { store.startAll() }
                Button(L10n.tr("downloads.pause.all"), systemImage: "pause") { store.pauseAll() }
                Button(L10n.tr("downloads.speed.limit.menu"), systemImage: "speedometer") { ask(.speed) }
            } label: {
                Label(L10n.tr("downloads.all.menu"), systemImage: "ellipsis.circle")
            }
        }
    }

    @ViewBuilder
    private var selectionToolbar: some View {
        Button(L10n.tr("downloads.select.all"), systemImage: "checklist") { store.selectAll() }
        Button(L10n.tr("downloads.clear.selection"), systemImage: "xmark") { store.clearSelection() }
    }

    private var bottomBar: some View {
        VStack(spacing: 0) {
            DlToastHost(model: store.toasts)
            if store.info.selecting {
                selectionBar
            }
        }
    }

    private var selectionBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                ForEach(store.info.selectionActions) { action in
                    Button(action.label) { bulk(action) }
                        .buttonStyle(.bordered)
                        .tint(action == .remove ? Theme.fail : Theme.accent)
                        .disabled(store.info.batchBusy)
                }
            }
            .padding(Theme.Spacing.small)
        }
        .background(Theme.surfaceRaised)
    }

    private var list: some View {
        List {
            headerRows
            ForEach(store.info.items) { item in
                row(item)
            }
            if store.info.moreOutside > 0 && store.info.empty?.kind != .noMatches {
                Button(L10n.format("downloads.show.more", store.info.moreOutside)) { store.showMore() }
                    .buttonStyle(.bordered)
                    .disabled(store.info.loadingMore)
                    .frame(maxWidth: .infinity)
                    .plainRow()
            }
        }
        .listStyle(.plain)
        .refreshable { await store.refresh() }
    }

    @ViewBuilder
    private var headerRows: some View {
        if let error = store.info.load.error {
            NoticeBanner(text: error.localized).plainRow()
        }
        ForEach(store.info.notes) { note in
            NoticeBanner(text: note.message).plainRow()
        }
        if store.info.manager && !store.info.matches.isEmpty {
            matchesBanner.plainRow()
        }
        DlChipRow {
            ForEach(store.info.tabs) { count in
                DlChipButton(text: count.tab.label + "  " + String(count.count), selected: store.info.filter.tab == count.tab) {
                    store.setTab(count.tab)
                }
            }
        }
        .plainRow(insets: EdgeInsets())
        if !store.info.subs.isEmpty {
            DlChipRow {
                ForEach(store.info.subs) { count in
                    DlChipButton(text: count.sub.label + "  " + String(count.count), selected: store.info.filter.sub == count.sub) {
                        store.setSub(count.sub)
                    }
                }
            }
            .plainRow(insets: EdgeInsets())
        }
        if store.info.manager {
            statusLine.plainRow(insets: EdgeInsets())
        }
        if let empty = store.info.empty {
            inlineEmpty(empty).plainRow()
        }
    }

    private var matchesBanner: some View {
        HStack {
            Text(L10n.format("downloads.matches.banner", store.info.matches.count))
                .font(.callout)
                .foregroundStyle(Theme.text)
            Spacer(minLength: Theme.Spacing.small)
            Button(L10n.tr("downloads.matches.open")) { ask(.matches) }
                .buttonStyle(.bordered)
        }
        .padding(Theme.Spacing.medium)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }

    private var statusText: String {
        let line = store.info.statusLine
        var parts: [String] = [L10n.format("downloads.speed.down", DlFormat.speed(line.speed))]
        if line.free > 0 { parts.append(L10n.format("downloads.status.free", DlFormat.freeSize(line.free))) }
        if line.left > 0 { parts.append(L10n.format("downloads.time.left", DownloadsText.duration(line.left))) }
        if line.limited { parts.append(L10n.tr("downloads.status.limited")) }
        return parts.joined(separator: " · ")
    }

    private var statusLine: some View {
        let line = store.info.statusLine
        return DlChipRow {
            Button {
                ask(.speed)
            } label: {
                Text(statusText)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
            .buttonStyle(.plain)
            if line.review > 0 {
                DlChipButton(text: L10n.format("downloads.status.review", line.review), selected: false, textColor: Theme.failText) {
                    store.show("problems.decide")
                }
            }
            if store.info.trackerIssues > 0 {
                DlChipButton(
                    text: L10n.format("downloads.tracker.issues", store.info.trackerIssues),
                    selected: store.info.filter.tracker == "p:any"
                ) {
                    store.setTracker(store.info.filter.tracker == "p:any" ? "" : "p:any")
                }
            }
        }
    }

    @ViewBuilder
    private func inlineEmpty(_ empty: DownloadsEmptyInfo) -> some View {
        let filter = store.info.filter
        switch empty.kind {
        case .noMatches:
            DlInlineEmpty(
                title: L10n.format("downloads.empty.matches", filter.query),
                systemImage: "arrow.down.to.line",
                actionTitle: L10n.tr("downloads.clear.search")
            ) {
                search = ""
                store.setQuery("")
            }
        case .noKind:
            DlInlineEmpty(
                title: kindEmpty(filter.kind),
                systemImage: "arrow.down.to.line",
                actionTitle: L10n.tr("downloads.empty.show.all")
            ) {
                store.clearFilters()
            }
        case .noNetwork:
            DlInlineEmpty(
                title: filter.network == .usenet
                    ? L10n.tr("downloads.empty.network.usenet")
                    : L10n.tr("downloads.empty.network.torrent"),
                systemImage: "arrow.down.to.line",
                actionTitle: L10n.tr("downloads.empty.show.all")
            ) {
                store.clearFilters()
            }
        case .loadingMore:
            DlInlineEmpty(
                title: L10n.tr("downloads.empty.loading.title"),
                message: L10n.format("downloads.empty.loading", empty.count),
                systemImage: "arrow.down.to.line"
            )
        case .sub:
            DlInlineEmpty(title: (filter.sub ?? .decide).emptyTitle, systemImage: "arrow.down.to.line")
        case .tab, .nothing:
            DlInlineEmpty(
                title: filter.tab.emptyTitle,
                message: filter.tab.emptyMessage,
                systemImage: "arrow.down.to.line"
            )
        }
    }

    private func kindEmpty(_ kind: DlKindFilter) -> String {
        switch kind {
        case .show: L10n.tr("downloads.empty.kind.show")
        case .music: L10n.tr("downloads.empty.kind.music")
        default: L10n.tr("downloads.empty.kind.movie")
        }
    }

    private func row(_ item: DownloadItemInfo) -> some View {
        let selecting = store.info.selecting
        return DownloadRowView(
            item: item,
            selecting: selecting,
            selected: store.info.selection.contains(item.key),
            busy: store.info.busy.contains(item.key),
            onAction: { action in handle(item, action) }
        )
        .contentShape(Rectangle())
        .onTapGesture { tap(item) }
        .contextMenu {
            if !selecting {
                DownloadActionMenuItems(actions: item.actions) { action in handle(item, action) }
                if item.selectable {
                    Divider()
                    Button(L10n.tr("downloads.select"), systemImage: "checkmark.circle") {
                        store.toggleSelect(item.key)
                    }
                }
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            if !selecting && item.actions.contains(.remove) {
                Button(role: .destructive) {
                    handle(item, .remove)
                } label: {
                    Label(DownloadActionKind.remove.label, systemImage: DownloadActionKind.remove.symbol)
                }
            }
        }
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            if !selecting && item.actions.contains(.pause) {
                Button {
                    handle(item, .pause)
                } label: {
                    Label(DownloadActionKind.pause.label, systemImage: DownloadActionKind.pause.symbol)
                }
                .tint(Theme.wait)
            }
            if !selecting && item.actions.contains(.resume) {
                Button {
                    handle(item, .resume)
                } label: {
                    Label(DownloadActionKind.resume.label, systemImage: DownloadActionKind.resume.symbol)
                }
                .tint(Theme.ok)
            }
        }
        .listRowInsets(EdgeInsets(top: 0, leading: Theme.Spacing.screen, bottom: 0, trailing: Theme.Spacing.small))
    }
}

struct DownloadActionMenuItems: View {
    let actions: [DownloadActionKind]
    let onAction: @MainActor (DownloadActionKind) -> Void

    var body: some View {
        ForEach(actions) { action in
            if action == .recheck || action == .markFailed {
                Divider()
            }
            Button(role: action.destructive ? .destructive : nil) {
                onAction(action)
            } label: {
                Label(action.label, systemImage: action.symbol)
            }
        }
    }
}

private struct DownloadRowView: View {
    let item: DownloadItemInfo
    let selecting: Bool
    let selected: Bool
    let busy: Bool
    let onAction: @MainActor (DownloadActionKind) -> Void

    var body: some View {
        HStack(alignment: .center, spacing: Theme.Spacing.medium) {
            if selecting {
                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(selected ? Theme.accent : Theme.textFaint)
                    .opacity(item.selectable ? 1 : 0.35)
                    .accessibilityHidden(true)
            }
            DlPosterThumb(source: item.poster, name: item.title, square: item.square, width: item.square ? 48 : 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if !item.subLine.display.isEmpty {
                    Text(item.subLine.display)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .lineLimit(1)
                }
                statusRow
                ProgressView(value: min(max(item.progress, 0), 1))
                    .tint(item.bar.color)
                    .padding(.vertical, 2)
                if !item.factsLine.isEmpty {
                    Text(item.factsLine)
                        .font(.caption)
                        .foregroundStyle(Theme.textMuted)
                        .lineLimit(1)
                }
            }
            if !item.actions.isEmpty && !selecting {
                Menu {
                    DownloadActionMenuItems(actions: item.actions, onAction: onAction)
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(Theme.textMuted)
                        .frame(width: 36, height: 44)
                        .contentShape(Rectangle())
                }
                .disabled(busy)
                .accessibilityLabel(L10n.tr("downloads.more.actions"))
            }
        }
        .padding(.vertical, Theme.Spacing.small)
        .accessibilityElement(children: .contain)
    }

    private var statusRow: some View {
        HStack(spacing: Theme.Spacing.xSmall) {
            Text(item.word.label)
                .font(.caption.weight(.semibold))
                .foregroundStyle(item.tone.labelColor)
                .lineLimit(1)
                .layoutPriority(1)
            if !item.secondLine.isEmpty {
                Text("· " + item.secondLine)
                    .font(.caption)
                    .foregroundStyle(item.problem?.tone.labelColor ?? Theme.textMuted)
                    .lineLimit(1)
            }
        }
    }
}

private extension View {
    func plainRow(insets: EdgeInsets = EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16)) -> some View {
        listRowInsets(insets)
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
    }
}
