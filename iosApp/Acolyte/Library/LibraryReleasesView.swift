import SwiftUI
import Observation

@MainActor
@Observable
final class LibraryReleasesStore {
    private(set) var info: LibraryReleasesInfo

    private let driver: LibraryReleasesDriver

    init(turnedDown: Bool) {
        info = .initial(turnedDown: turnedDown)
        driver = SDKClient.shared.makeLibraryReleases(turnedDown: turnedDown)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func followLive() async {
        await driver.followLive()
    }

    func nextPage() {
        let driver = self.driver
        Task { await driver.nextPage() }
    }

    func previousPage() {
        let driver = self.driver
        Task { await driver.previousPage() }
    }

    func remove(_ id: Int64) {
        let driver = self.driver
        Task { await driver.remove(id) }
    }

    func clearAll() {
        let driver = self.driver
        Task { await driver.clearAll() }
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

struct LibraryReleasesView: View {
    @State private var showTurnedDown: Bool

    init(turnedDown: Bool) {
        _showTurnedDown = State(initialValue: turnedDown)
    }

    var body: some View {
        VStack(spacing: 0) {
            Picker(L10n.tr("library.releases.lists"), selection: $showTurnedDown) {
                Text(L10n.tr("library.releases.held")).tag(false)
                Text(L10n.tr("library.releases.turned.down")).tag(true)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
            LibraryReleasesList(turnedDown: showTurnedDown)
                .id(showTurnedDown)
        }
        .navigationTitle(showTurnedDown ? L10n.tr("library.releases.turned.down") : L10n.tr("library.releases.held"))
    }
}

private struct LibraryReleasesList: View {
    let turnedDown: Bool

    @State private var store: LibraryReleasesStore
    @State private var confirmClear = false

    init(turnedDown: Bool) {
        self.turnedDown = turnedDown
        _store = State(initialValue: LibraryReleasesStore(turnedDown: turnedDown))
    }

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(title: emptyTitle, message: emptyHint, systemImage: "tray")
        } content: {
            List {
                if let error = store.info.load.error {
                    NoticeBanner(text: LibraryText.error(error))
                        .listRowSeparator(.hidden)
                }
                Text(LibraryText.plural("library.releases.count", store.info.total))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .listRowSeparator(.hidden)
                ForEach(store.info.rows) { row in
                    LibraryReleaseRow(row: row, turnedDown: turnedDown, busy: store.info.busy) { store.remove(row.id) }
                }
            }
            .listStyle(.plain)
            .refreshable { await store.refresh() }
        }
        .libraryNotice(store.info.notice) { store.dismissNotice($0) }
        .safeAreaInset(edge: .bottom) {
            pagerBar
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                if store.info.total > 0 {
                    Button(L10n.tr("library.releases.clear.all"), role: .destructive) { confirmClear = true }
                        .disabled(store.info.busy)
                }
            }
        }
        .confirmationDialog(
            L10n.tr("library.releases.clear.all"),
            isPresented: $confirmClear,
            titleVisibility: .visible
        ) {
            Button(L10n.tr("library.releases.clear.all"), role: .destructive) { store.clearAll() }
        } message: {
            Text(clearMessage)
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private var emptyTitle: String {
        turnedDown ? L10n.tr("library.releases.none.turned") : L10n.tr("library.releases.none.held")
    }

    private var emptyHint: String {
        turnedDown ? L10n.tr("library.releases.none.turned.hint") : L10n.tr("library.releases.none.held.hint")
    }

    private var clearMessage: String {
        let key = turnedDown ? "library.releases.clear.turned" : "library.releases.clear.held"
        return LibraryText.plural(key, store.info.total)
    }

    @ViewBuilder
    private var pagerBar: some View {
        if store.info.hasPrevious || store.info.hasNext {
            LibraryPager(
                from: store.info.from,
                to: store.info.to,
                total: store.info.total,
                hasPrevious: store.info.hasPrevious,
                hasNext: store.info.hasNext,
                previous: { store.previousPage() },
                next: { store.nextPage() }
            )
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
            .background(.bar)
        }
    }
}

private struct LibraryReleaseRow: View {
    let row: LibraryReleaseRowInfo
    let turnedDown: Bool
    let busy: Bool
    let remove: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            LibraryPosterThumb(source: row.poster, name: row.title)
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(titleLine)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Text(row.name)
                    .font(.caption)
                    .foregroundStyle(Theme.textFaint)
                    .lineLimit(2)
                    .truncationMode(.middle)
                LibraryReleasesFlow(spacing: Theme.Spacing.xSmall) {
                    ForEach(Array(row.tags.enumerated()), id: \.offset) { entry in
                        Text(entry.element)
                            .font(.caption2.weight(.medium))
                            .foregroundStyle(Theme.textMuted)
                            .padding(.horizontal, 7)
                            .frame(height: 20)
                            .background(Theme.surfaceHigh, in: Capsule())
                    }
                    StatusChip(text: verdictText, tone: row.verdict.tone)
                }
                let line = facts
                if !line.isEmpty {
                    Text(line)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                ForEach(Array(row.reasons.enumerated()), id: \.offset) { entry in
                    Text(entry.element)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Button(actionWord, action: remove)
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .disabled(busy)
                    .accessibilityLabel(L10n.format("library.releases.action.for", actionWord, row.name))
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(actionWord, action: remove)
                .tint(Theme.accent)
                .disabled(busy)
        }
    }

    private var actionWord: String {
        turnedDown ? L10n.tr("library.releases.take.off") : L10n.tr("library.releases.stop.holding")
    }

    private var titleLine: String {
        row.version.isEmpty ? row.title : row.title + " · " + row.version
    }

    private var verdictText: String {
        switch row.verdict {
        case .turnedDown: L10n.tr("library.releases.verdict.turned.down")
        case .chosen: L10n.tr("library.releases.verdict.chosen")
        case .held: L10n.tr("library.releases.verdict.held")
        case .wouldChoose: L10n.tr("library.releases.verdict.would.choose")
        case .wouldHold: L10n.tr("library.releases.verdict.would.hold")
        case .refused: L10n.tr("library.releases.verdict.refused")
        case .passedOver: L10n.tr("library.releases.verdict.passed.over")
        case .judged: L10n.tr("library.releases.verdict.judged")
        }
    }

    private var facts: String {
        var parts: [String] = []
        if !row.sourceName.isEmpty { parts.append(row.sourceName) }
        if row.size > 0 { parts.append(LibraryText.bytes(row.size)) }
        if row.seeders >= 0 { parts.append(LibraryText.plural("library.releases.seeders", row.seeders)) }
        if !row.transport.isEmpty { parts.append(row.transport) }
        if row.points >= 0 { parts.append(LibraryText.plural("library.releases.point.count", row.points)) }
        if row.rank >= 0 { parts.append(L10n.format("library.releases.ranked", row.rank)) }
        if turnedDown {
            if !row.since.isEmpty { parts.append(L10n.format("library.releases.since", LibraryText.day(row.since))) }
        } else if !row.holdUntil.isEmpty {
            parts.append(L10n.format("library.releases.sent", LibraryText.dateTime(row.holdUntil)))
        }
        return parts.joined(separator: " · ")
    }
}

private struct LibraryReleasesFlow: Layout {
    var spacing: CGFloat = 4

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var widest: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            widest = max(widest, x - spacing)
        }
        return CGSize(width: widest, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), anchor: .topLeading, proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
