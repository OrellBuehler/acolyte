import SwiftUI

struct ReviewTarget: Identifiable, Hashable {
    let id: Int64
}

struct ChipRow<Item: Hashable>: View {
    let items: [Item]
    let selected: Item?
    let label: (Item) -> String
    let onSelect: (Item) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                ForEach(items, id: \.self) { item in
                    let isOn = item == selected
                    Button(label(item)) { onSelect(item) }
                        .font(.subheadline.weight(isOn ? .semibold : .regular))
                        .padding(.horizontal, Theme.Spacing.medium)
                        .frame(minHeight: 32)
                        .background(isOn ? Theme.text : Theme.surfaceRaised, in: Capsule())
                        .foregroundStyle(isOn ? Theme.background : Theme.text)
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(isOn ? .isSelected : [])
                }
            }
            .padding(.horizontal, Theme.Spacing.screen)
        }
    }
}

struct RequestThumb: View {
    let image: String
    let name: String
    var width: CGFloat = 56

    var body: some View {
        AuthImage(source: image.isEmpty ? nil : image, size: .thumb) {
            ArtPlaceholderView(name: name)
        }
        .frame(width: width, height: width * 1.5)
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

struct NoteToast: View {
    let text: String
    let undo: (() -> Void)?

    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            Text(text)
                .font(.callout)
                .foregroundStyle(Theme.text)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let undo {
                Button(L10n.tr("requests.undo"), action: undo)
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(Theme.accentText)
            }
        }
        .padding(Theme.Spacing.medium)
        .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.bottom, Theme.Spacing.small)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}

struct RequestsView: View {
    let account: AccountInfo
    @State private var store = RequestsStore()
    @Environment(\.openRoute) private var openRoute
    @State private var review: ReviewTarget?
    @State private var declining: AskInfo?
    @State private var removing: ReviewTarget?
    @State private var takingBack: AskInfo?
    @State private var confirmAll = false
    @State private var confirmClear = false

    private var info: RequestsInfo { store.info }

    var body: some View {
        StatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
            VStack {
                EmptyStateView(
                    title: L10n.tr("requests.empty.title"),
                    message: L10n.tr(info.canAsk ? "requests.empty.message" : "requests.empty.cannot.ask"),
                    systemImage: "checklist"
                )
                if info.canAsk, !info.approver {
                    NavigationLink(value: SectionID.search) {
                        Text(L10n.tr("requests.ask.something"))
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
        } content: {
            list
        }
        .navigationTitle(SectionID.requests.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbar }
        .safeAreaInset(edge: .bottom) { toast }
        .animation(.smooth(duration: 0.24), value: info.note?.id)
        .sheet(item: $review) { target in
            AskReviewView(askId: target.id) { action in
                review = nil
                switch action {
                case let .approve(reviewStore): store.approve(target.id, review: reviewStore)
                case let .decline(reason): store.decline(target.id, reason: reason)
                case .remove: store.remove(target.id)
                case let .openTitle(kind, tmdb): openRoute(.title(kind: kind, tmdb: tmdb))
                }
            }
        }
        .sheet(item: $declining) { ask in
            DeclineView(ask: ask) { reason in
                declining = nil
                store.decline(ask.id, reason: reason)
            }
        }
        .alert(L10n.tr("requests.remove.title"), isPresented: present($removing)) {
            Button(L10n.tr("requests.remove"), role: .destructive) {
                if let id = removing?.id { store.remove(id) }
            }
            Button(L10n.tr("requests.keep"), role: .cancel) {}
        } message: {
            Text(L10n.tr("requests.remove.text"))
        }
        .alert(L10n.format("requests.take.back.title", takingBack.map { RequestsWords.titleWithYear($0.name, $0.year) } ?? ""), isPresented: present($takingBack)) {
            Button(L10n.tr("requests.take.back"), role: .destructive) {
                if let id = takingBack?.id { store.takeBack(id) }
            }
            Button(L10n.tr("requests.keep"), role: .cancel) {}
        } message: {
            Text(L10n.tr("requests.take.back.text"))
        }
        .alert(L10n.format("requests.approve.all.title", info.waitingToApprove), isPresented: $confirmAll) {
            Button(L10n.format("requests.approve.n", info.waitingToApprove)) { store.approveAll() }
            Button(L10n.tr("common.cancel"), role: .cancel) {}
        } message: {
            if info.isCut { Text(L10n.tr("requests.approve.all.cut")) }
        }
        .alert(clearTitle, isPresented: $confirmClear) {
            Button(L10n.tr("requests.remove.all"), role: .destructive) {
                if let which = info.clearable { store.clear(which) }
            }
            Button(L10n.tr("requests.keep"), role: .cancel) {}
        } message: {
            Text(clearText)
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .task(id: info.note?.id) {
            guard let note = info.note else { return }
            try? await Task.sleep(for: .seconds(note.keys.isEmpty ? 4 : 6))
            if store.info.note?.id == note.id { store.dismissNote() }
        }
        .onDisappear { store.flush() }
    }

    private func present<T>(_ value: Binding<T?>) -> Binding<Bool> {
        Binding(get: { value.wrappedValue != nil }, set: { if !$0 { value.wrappedValue = nil } })
    }

    private var clearTitle: String {
        info.clearable == .failed
            ? L10n.format("requests.clear.failed.title", info.clearCount)
            : L10n.format("requests.clear.finished.title", info.clearCount)
    }

    private var clearText: String {
        let base = L10n.tr(info.clearable == .failed ? "requests.clear.failed.text" : "requests.clear.finished.text")
        return info.shown.contains { $0.sharedCount > 1 } ? base + "\n\n" + L10n.tr("requests.clear.shared") : base
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItemGroup(placement: .primaryAction) {
            if info.reportsOpen >= 0 || info.hasOwnReports {
                Button {
                    openRoute(.reports)
                } label: {
                    Label(L10n.tr("requests.reports"), systemImage: info.reportsOpen > 0 ? "exclamationmark.bubble.fill" : "exclamationmark.bubble")
                }
            }
            if info.approver {
                Menu {
                    if !info.stages.isEmpty {
                        Picker(L10n.tr("requests.filter.status"), selection: Binding(get: { info.stage }, set: { store.setStage($0) })) {
                            Text(L10n.tr("requests.filter.all.statuses")).tag(StageKind?.none)
                            ForEach(info.stages) { stage in
                                Text("\(RequestsWords.stage(stage)) (\(info.stageCounts[stage] ?? 0))").tag(StageKind?.some(stage))
                            }
                        }
                    }
                    Picker(L10n.tr("requests.filter.type"), selection: Binding(get: { info.kind }, set: { store.setKind($0) })) {
                        ForEach(KindFilterKind.allCases) { kind in
                            Text(RequestsWords.kindFilter(kind)).tag(kind)
                        }
                    }
                } label: {
                    Label(L10n.tr("requests.filter"), systemImage: info.filtersOn > 0 ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                }
            }
            if !store.pages.isEmpty {
                Menu {
                    ForEach(store.pages) { page in
                        Button(RequestsWords.page(page)) { openRoute(RequestsWords.route(page)) }
                    }
                } label: {
                    Label(L10n.tr("requests.more"), systemImage: "ellipsis.circle")
                }
            }
        }
    }

    @ViewBuilder
    private var toast: some View {
        if let note = info.note {
            NoteToast(text: noteText(note), undo: note.keys.isEmpty ? nil : { store.undo(note.keys) })
        }
    }

    private func noteText(_ note: RequestsNoteInfo) -> String {
        RequestsWords.note(note)
    }

    private var list: some View {
        List {
            if let error = info.load.error {
                NoticeBanner(text: error.localized)
                    .listRowSeparator(.hidden)
            }
            if !info.approver {
                memberHeader
                    .listRowSeparator(.hidden)
            }
            tabs
                .listRowInsets(EdgeInsets())
                .listRowSeparator(.hidden)
            if !info.approver, !info.kinds.isEmpty {
                ChipRow(items: info.kinds, selected: info.kind, label: RequestsWords.kindFilter) { store.setKind($0) }
                    .listRowInsets(EdgeInsets())
                    .listRowSeparator(.hidden)
            }
            if info.canApproveAll || info.clearable != nil {
                HStack {
                    if info.canApproveAll {
                        Button(L10n.format("requests.approve.all", info.waitingToApprove)) { confirmAll = true }
                            .buttonStyle(.bordered)
                    }
                    if let which = info.clearable {
                        Button(L10n.format(which == .failed ? "requests.clear.failed" : "requests.clear.finished", info.clearCount)) {
                            confirmClear = true
                        }
                        .buttonStyle(.bordered)
                    }
                }
                .disabled(info.busy)
                .listRowSeparator(.hidden)
            }
            if info.shown.isEmpty {
                calm
                    .listRowSeparator(.hidden)
            }
            ForEach(info.shown) { ask in
                row(ask)
            }
            if info.isCut {
                Text(L10n.format("requests.showing", info.loaded, info.total))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .onAppear { store.loadMore() }
                    .listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .refreshable { await store.refresh() }
    }

    @ViewBuilder
    private var tabs: some View {
        if info.approver {
            ChipRow(items: ApproverTabKind.allCases, selected: info.approverTab, label: { tab in
                let n = info.approverCounts[tab] ?? 0
                let flag = tab == .processing && info.failedCount > 0 ? " ⚠︎" : ""
                return n > 0 ? "\(RequestsWords.approverTab(tab)) \(n)\(flag)" : RequestsWords.approverTab(tab)
            }, onSelect: { store.setApproverTab($0) })
        } else {
            ChipRow(items: info.memberTabs, selected: info.memberTab, label: { tab in
                let n = info.memberCounts[tab] ?? 0
                return n > 0 ? "\(RequestsWords.memberTab(tab)) \(n)" : RequestsWords.memberTab(tab)
            }, onSelect: { store.setMemberTab($0) })
        }
    }

    private var memberHeader: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            ForEach(info.limits) { line in
                Text(RequestsWords.limit(line))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            HStack {
                if info.canAsk {
                    NavigationLink(value: SectionID.search) {
                        Text(L10n.tr("requests.ask.something"))
                    }
                    .buttonStyle(.borderedProminent)
                }
                if info.hasOwnReports {
                    Button(L10n.tr("requests.your.reports")) { openRoute(.reports) }
                        .buttonStyle(.bordered)
                }
            }
        }
    }

    private var calm: some View {
        let filtered = info.approver && info.filtersOn > 0
        let words = RequestsWords.calm(info)
        return VStack(spacing: Theme.Spacing.xSmall) {
            Text(words.title).font(.headline)
            if let message = words.message {
                Text(message).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if filtered {
                Button(L10n.tr("requests.show.all")) { store.clearFilters() }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Theme.Spacing.xLarge)
    }

    private func row(_ ask: AskInfo) -> some View {
        let pending = info.pending[ask.id]
        return HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            RequestThumb(image: ask.poster, name: ask.name)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                HStack(alignment: .firstTextBaseline) {
                    Text(ask.name)
                        .font(.headline)
                        .lineLimit(2)
                    Spacer(minLength: 0)
                    if info.newIds.contains(ask.id) {
                        Text(L10n.tr("requests.new"))
                            .font(.caption.weight(.semibold))
                            .padding(.horizontal, 6)
                            .background(Theme.accentFill, in: Capsule())
                            .foregroundStyle(Theme.onAccent)
                    }
                }
                Text(RequestsWords.facts(ask))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                StatusChip(text: info.approver ? RequestsWords.stage(ask.stage) : RequestsWords.memberStatus(ask), tone: ask.tone)
                AskLinesView(ask: ask, approver: info.approver, myId: info.myId)
                if let pending {
                    HStack {
                        Text(L10n.tr(pending == .approve ? "requests.sending.approve" : "requests.sending.decline"))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                        Spacer()
                        Button(L10n.tr("requests.undo")) { store.undo([ask.id]) }
                            .buttonStyle(.borderless)
                    }
                } else if info.approver, ask.isWaiting {
                    HStack {
                        Button(L10n.tr("requests.decline")) { declining = ask }
                            .buttonStyle(.bordered)
                        Button(L10n.tr("requests.approve")) { store.approve(ask.id) }
                            .buttonStyle(.borderedProminent)
                            .tint(Theme.ok)
                    }
                } else if info.approver, ask.isFailed {
                    Button(L10n.tr("requests.decline")) { declining = ask }
                        .buttonStyle(.bordered)
                }
                if ask.state == "approved", !ask.isFailed {
                    ProgressView(value: min(max(ask.progress, 0), 1))
                        .tint(Theme.dotColor(ask.tone))
                }
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
        .contentShape(Rectangle())
        .onTapGesture { open(ask) }
        .swipeActions(edge: .trailing) {
            if pending == nil, info.approver, ask.isWaiting {
                Button(L10n.tr("requests.approve")) { store.approve(ask.id) }
                    .tint(Theme.ok)
            }
            if pending == nil, !info.approver, ask.isWaiting, ask.mine {
                Button(L10n.tr("requests.take.back"), role: .destructive) { takingBack = ask }
            }
        }
        .swipeActions(edge: .leading) {
            if pending == nil, info.approver, ask.isWaiting || ask.isFailed {
                Button(L10n.tr("requests.decline")) { declining = ask }
                    .tint(Theme.fail)
            }
        }
        .contextMenu {
            if !ask.isArtist, !ask.tmdbId.isEmpty {
                Button(L10n.tr("requests.open.title")) { openRoute(.title(kind: ask.kind, tmdb: ask.tmdbId)) }
            }
            if !info.approver, ask.isWaiting, ask.mine, pending == nil {
                Button(L10n.tr("requests.take.back"), role: .destructive) { takingBack = ask }
            }
            if info.approver {
                Button(L10n.tr("requests.remove"), role: .destructive) { removing = ReviewTarget(id: ask.id) }
            }
        }
    }

    private func open(_ ask: AskInfo) {
        if info.approver {
            review = ReviewTarget(id: ask.id)
        } else if !ask.isArtist, !ask.tmdbId.isEmpty {
            openRoute(.title(kind: ask.kind, tmdb: ask.tmdbId))
        }
    }
}

struct DeclineView: View {
    let ask: AskInfo
    let onDecline: (String) -> Void

    var body: some View {
        NavigationStack {
            DeclineForm(ask: ask, onDecline: onDecline)
        }
        .presentationDetents([.medium, .large])
    }
}

struct DeclineForm: View {
    let ask: AskInfo
    let onDecline: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var reason = ""

    private var quick: [String] {
        [
            L10n.tr("requests.reason.have.it"),
            L10n.tr("requests.reason.kids"),
            L10n.tr("requests.reason.asked.before"),
            L10n.tr("requests.reason.space"),
        ]
    }

    var body: some View {
        Form {
                Section {
                    TextField(
                        L10n.format("requests.decline.reason", RequestsWords.names(ask.askers)),
                        text: $reason,
                        prompt: Text(L10n.tr("requests.decline.placeholder")),
                        axis: .vertical
                    )
                    .lineLimit(2 ... 6)
                    .onChange(of: reason) { _, value in
                        if value.count > 500 { reason = String(value.prefix(500)) }
                    }
                } footer: {
                    Text(L10n.tr("requests.decline.undo"))
                }
                Section {
                    ForEach(quick, id: \.self) { words in
                        Button(words) { toggle(words) }
                    }
                }
            }
            .navigationTitle(L10n.format("requests.decline.title", RequestsWords.titleWithYear(ask.name, ask.year)))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("requests.decline")) { onDecline(reason) }
                }
            }
    }

    private func toggle(_ words: String) {
        if reason.contains(words) {
            reason = reason.replacingOccurrences(of: words, with: "").trimmingCharacters(in: CharacterSet(charactersIn: " .;,"))
        } else {
            let head = reason.trimmingCharacters(in: CharacterSet(charactersIn: " ."))
            reason = String((head.isEmpty ? words : "\(head). \(words)").prefix(500))
        }
    }
}
