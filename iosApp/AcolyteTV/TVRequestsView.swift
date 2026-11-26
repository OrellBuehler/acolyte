import SwiftUI

struct TVRequestsView: View {
    let account: AccountInfo
    @State private var store = RequestsStore()
    @State private var ambient: String?
    @State private var acting: AskInfo?
    @State private var confirmAll = false
    @FocusState private var focus: String?
    @Environment(\.scenePhase) private var scenePhase
    @Environment(TVNavigator.self) private var navigator

    private var info: RequestsInfo { store.info }

    private var canSearch: Bool { TVSections.visible(.search, for: account) }

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            TVBackdropLayer(source: ambient, blurred: true)
            TVStatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
                empty
            } content: {
                list
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let note = info.note {
                TVNoteBanner(text: RequestsWords.note(note), failed: failed(note), undo: note.keys.isEmpty ? nil : { store.undo(note.keys) })
            }
        }
        .animation(.smooth(duration: 0.24), value: info.note?.id)
        .confirmationDialog(
            acting.map { RequestsWords.titleWithYear($0.name, $0.year) } ?? "",
            isPresented: Binding(get: { acting != nil }, set: { if !$0 { acting = nil } }),
            titleVisibility: .visible,
            presenting: acting
        ) { ask in
            actions(ask)
        }
        .confirmationDialog(
            L10n.format("requests.approve.all.title", info.waitingToApprove),
            isPresented: $confirmAll,
            titleVisibility: .visible
        ) {
            Button(L10n.format("requests.approve.n", info.waitingToApprove)) { store.approveAll() }
            Button(L10n.tr("common.cancel"), role: .cancel) {}
        } message: {
            if info.isCut { Text(L10n.tr("requests.approve.all.cut")) }
        }
        .screenshotFocus($focus, id: info.canApproveAll ? "bulk" : info.shown.first.map { String($0.id) }, screen: "requests")
        .onChange(of: focus) { _, new in
            guard let new, let ask = info.shown.first(where: { String($0.id) == new }) else { return }
            ambient = ask.poster.isEmpty ? nil : ask.poster
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .task(id: info.note?.id) {
            guard let note = info.note else { return }
            try? await Task.sleep(for: .seconds(note.keys.isEmpty ? 4 : 6))
            if store.info.note?.id == note.id { store.dismissNote() }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await store.refresh() } }
        }
        .onDisappear { store.flush() }
    }

    private func failed(_ note: RequestsNoteInfo) -> Bool {
        switch note.kind {
        case .approveFailed, .declineFailed, .removeFailed, .takeBackFailed: true
        default: false
        }
    }

    private var empty: some View {
        VStack(spacing: 32) {
            TVEmptyView(
                title: L10n.tr("requests.empty.title"),
                message: L10n.tr(info.canAsk ? "requests.empty.message" : "requests.empty.cannot.ask"),
                systemImage: "checklist"
            )
            if info.canAsk, !info.approver, canSearch {
                TVActionButton(title: L10n.tr("requests.ask.something"), systemImage: "magnifyingglass", prominent: true) {
                    navigator.open(.search)
                }
            }
        }
    }

    private var list: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Theme.TV.textGap) {
                if let error = info.load.error {
                    TVNoticeBanner(text: error.localized)
                }
                if !info.approver {
                    memberHeader
                }
                tabs
                kindChips
                stageChips
                bulkActions
                if info.shown.isEmpty {
                    calm
                }
                LazyVGrid(columns: Theme.TV.thumbColumns, alignment: .leading, spacing: Theme.TV.gutter) {
                    ForEach(info.shown) { ask in
                        card(ask)
                    }
                }
                .focusSection()
                if info.isCut {
                    VStack(alignment: .leading, spacing: Theme.TV.textGap) {
                        Text(L10n.format("requests.showing", info.loaded, info.total))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                        TVActionButton(title: L10n.tr("tv.requests.more"), systemImage: "arrow.down.circle") { store.loadMore() }
                    }
                    .focusSection()
                }
            }
        }
    }

    @ViewBuilder
    private var memberHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(info.limits) { line in
                Text(RequestsWords.limit(line))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if info.canAsk, canSearch {
                TVActionButton(title: L10n.tr("requests.ask.something"), systemImage: "magnifyingglass", prominent: true) {
                    navigator.open(.search)
                }
                .padding(.vertical, Theme.TV.focusPad)
            }
        }
    }

    @ViewBuilder
    private var tabs: some View {
        if info.approver {
            TVChipRow {
                ForEach(ApproverTabKind.allCases) { tab in
                    let n = info.approverCounts[tab] ?? 0
                    let flag = tab == .processing && info.failedCount > 0 ? " ⚠︎" : ""
                    let title = n > 0 ? "\(RequestsWords.approverTab(tab)) \(n)\(flag)" : RequestsWords.approverTab(tab)
                    TVChip(title: title, selected: tab == info.approverTab) { store.setApproverTab(tab) }
                }
            }
        } else {
            TVChipRow {
                ForEach(info.memberTabs) { tab in
                    let n = info.memberCounts[tab] ?? 0
                    let title = n > 0 ? "\(RequestsWords.memberTab(tab)) \(n)" : RequestsWords.memberTab(tab)
                    TVChip(title: title, selected: tab == info.memberTab) { store.setMemberTab(tab) }
                }
            }
        }
    }

    @ViewBuilder
    private var kindChips: some View {
        let kinds = info.approver ? KindFilterKind.allCases : info.kinds
        if !kinds.isEmpty {
            TVChipRow {
                ForEach(kinds) { kind in
                    TVChip(title: RequestsWords.kindFilter(kind), selected: kind == info.kind) { store.setKind(kind) }
                }
            }
        }
    }

    @ViewBuilder
    private var stageChips: some View {
        if info.approver, !info.stages.isEmpty {
            TVChipRow {
                TVChip(title: L10n.tr("requests.filter.all.statuses"), selected: info.stage == nil) { store.setStage(nil) }
                ForEach(info.stages) { stage in
                    TVChip(title: "\(RequestsWords.stage(stage)) (\(info.stageCounts[stage] ?? 0))", selected: info.stage == stage) {
                        store.setStage(stage)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var bulkActions: some View {
        if info.canApproveAll {
            TVActionButton(title: L10n.format("requests.approve.all", info.waitingToApprove), systemImage: "checkmark.circle") {
                confirmAll = true
            }
            .focused($focus, equals: "bulk")
            .disabled(info.busy)
            .padding(.vertical, Theme.TV.focusPad)
        }
    }

    private var calm: some View {
        let words = RequestsWords.calm(info)
        return VStack(spacing: 8) {
            Text(words.title)
                .font(.headline)
                .foregroundStyle(Theme.text)
            if let message = words.message {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if info.approver, info.filtersOn > 0 {
                TVActionButton(title: L10n.tr("requests.show.all"), systemImage: "line.3.horizontal.decrease.circle") {
                    store.clearFilters()
                }
                .padding(.top, Theme.TV.focusPad)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Theme.TV.sectionGap)
    }

    private func card(_ ask: AskInfo) -> some View {
        TVThumbCard(
            image: ask.poster.isEmpty ? nil : ask.poster,
            name: ask.name,
            focus: TVFocusKey(binding: $focus, id: String(ask.id))
        ) {
            tap(ask)
        } content: {
            cardContent(ask)
        }
    }

    @ViewBuilder
    private func cardContent(_ ask: AskInfo) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(ask.name)
                .font(.callout.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(2)
            if info.newIds.contains(ask.id) {
                Text(L10n.tr("requests.new"))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.onAccent)
                    .padding(.horizontal, 12)
                    .background(Theme.accentFill, in: Capsule())
            }
        }
        Text(RequestsWords.facts(ask))
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
            .lineLimit(1)
        TVStatusChip(text: info.approver ? RequestsWords.stage(ask.stage) : RequestsWords.memberStatus(ask), tone: ask.tone)
        AskLinesView(ask: ask, approver: info.approver, myId: info.myId)
        if let pending = info.pending[ask.id] {
            Text(L10n.tr(pending == .approve ? "requests.sending.approve" : "requests.sending.decline"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        } else if ask.state == "approved", !ask.isFailed {
            ProgressView(value: min(max(ask.progress, 0), 1))
                .tint(Theme.dotColor(ask.tone))
        }
    }

    private func canOpen(_ ask: AskInfo) -> Bool {
        !ask.isArtist && !ask.tmdbId.isEmpty
    }

    private func canAct(_ ask: AskInfo) -> Bool {
        if info.pending[ask.id] != nil { return true }
        if info.approver { return ask.isWaiting || ask.isFailed }
        return ask.isWaiting && ask.mine
    }

    private func tap(_ ask: AskInfo) {
        if canAct(ask) {
            acting = ask
        } else if canOpen(ask) {
            navigator.open(.title(kind: ask.kind, tmdb: ask.tmdbId))
        }
    }

    @ViewBuilder
    private func actions(_ ask: AskInfo) -> some View {
        if info.pending[ask.id] != nil {
            Button(L10n.tr("requests.undo")) { store.undo([ask.id]) }
        } else {
            if canOpen(ask) {
                Button(L10n.tr("requests.open.title")) { navigator.open(.title(kind: ask.kind, tmdb: ask.tmdbId)) }
            }
            if info.approver, ask.isWaiting {
                Button(L10n.tr("requests.approve")) { store.approve(ask.id) }
            }
            if info.approver, ask.isWaiting || ask.isFailed {
                Button(L10n.tr("requests.decline"), role: .destructive) { store.decline(ask.id, reason: "") }
            }
            if !info.approver, ask.isWaiting, ask.mine {
                Button(L10n.tr("requests.take.back"), role: .destructive) { store.takeBack(ask.id) }
            }
        }
        Button(L10n.tr("common.cancel"), role: .cancel) {}
    }
}
