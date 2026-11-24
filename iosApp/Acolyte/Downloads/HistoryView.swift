import SwiftUI
import UIKit

private struct OpenKey: Identifiable {
    let id: String
}

struct HistoryView: View {
    @State private var store: HistoryStore
    @State private var search = ""
    @State private var filtering = false
    @State private var open: OpenKey?

    init(titleId: Int64, episodeId: Int64) {
        _store = State(initialValue: HistoryStore(titleId: titleId, episodeId: episodeId))
    }

    var body: some View {
        StatusContent(
            load: store.info.load,
            isEmpty: store.info.isEmpty && !store.info.filtered && !store.info.scoped,
            reload: { await store.refresh() }
        ) {
            EmptyStateView(title: L10n.tr("downloads.history.empty"), systemImage: "clock")
        } content: {
            list
        }
        .navigationTitle(SectionID.history.title)
        .searchable(text: $search, prompt: L10n.tr("downloads.history.search.hint"))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                let count = store.info.filter.count
                Button {
                    filtering = true
                } label: {
                    Label(
                        count > 0 ? L10n.format("downloads.history.filter.on", count) : L10n.tr("downloads.history.filter"),
                        systemImage: count > 0 ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle"
                    )
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { DlToastHost(model: store.toasts) }
        .sheet(isPresented: $filtering) {
            HistoryFilterSheet(store: store)
        }
        .sheet(item: $open) { key in
            DlHistorySheet(
                store: store,
                key: key.id,
                onOpenTitle: { titleId in
                    open = nil
                    store.openTitle(titleId)
                },
                onOpenDownload: { downloadKey in
                    open = nil
                    store.route = .downloads(show: "", open: downloadKey)
                }
            )
        }
        .navigationDestination(item: $store.route) { route in
            DlRouteDestination(route: route)
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .onChange(of: search) { _, value in store.setQuery(value) }
    }

    private var list: some View {
        List {
            if let error = store.info.load.error {
                NoticeBanner(text: error.localized).plainRow()
            }
            if store.info.scoped {
                scopeChip.plainRow()
            }
            if store.info.days.isEmpty {
                Text(L10n.tr("downloads.history.empty.filtered"))
                    .foregroundStyle(Theme.textMuted)
                    .plainRow()
            }
            ForEach(store.info.days) { day in
                Section {
                    ForEach(day.items) { item in
                        HistoryRow(item: item)
                            .contentShape(Rectangle())
                            .onTapGesture { open = OpenKey(id: item.key) }
                            .accessibilityAddTraits(.isButton)
                    }
                } header: {
                    dayHeader(day)
                }
            }
            if store.info.more {
                moreFooter.plainRow()
            }
        }
        .listStyle(.plain)
        .refreshable { await store.refresh() }
    }

    private var scopeChip: some View {
        Button {
            store.clearScope()
        } label: {
            HStack(spacing: Theme.Spacing.small) {
                Text(
                    store.info.scopeName.isEmpty
                        ? L10n.tr("downloads.history.scope.title")
                        : L10n.format("downloads.history.scope", store.info.scopeName)
                )
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Theme.onAccent)
                .lineLimit(1)
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(Theme.onAccent)
                    .accessibilityLabel(L10n.tr("downloads.history.clear"))
            }
            .padding(.horizontal, Theme.Spacing.medium)
            .padding(.vertical, 7)
            .background(Theme.accent, in: Capsule())
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func dayHeader(_ day: HistoryDayInfo) -> some View {
        var tally: [String] = [L10n.format("downloads.history.tally", day.items.count)]
        if day.failed > 0 { tally.append(L10n.format("downloads.history.tally.failed", day.failed)) }
        if day.needsYou > 0 { tally.append(L10n.format("downloads.history.tally.needs", day.needsYou)) }
        return DlDayHeader(
            title: DownloadsText.dayHeading(day.day, today: .today, yesterday: true),
            subtitle: tally.joined(separator: " · ")
        )
        .textCase(nil)
    }

    private var moreFooter: some View {
        VStack(spacing: Theme.Spacing.small) {
            Text(L10n.format("downloads.history.count", store.info.loaded, store.info.total))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Button(L10n.tr("downloads.history.show.more")) { store.showMore() }
                .buttonStyle(.bordered)
                .disabled(store.info.loadingMore)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct HistoryRow: View {
    let item: HistoryItemInfo

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            DlPosterThumb(source: item.poster, name: item.title.isEmpty ? "?" : item.title, square: item.square, width: item.square ? 40 : 36)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: Theme.Spacing.small) {
                    StatusChip(text: item.verb.label, tone: item.tone)
                    if let at = item.at {
                        Text(DownloadsText.clock(at))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                if !item.title.isEmpty {
                    Text(item.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                        .lineLimit(1)
                }
                if !item.bits.isEmpty {
                    Text(item.bits)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .lineLimit(1)
                }
                let line = item.line.message
                if !line.isEmpty {
                    Text(line)
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                        .lineLimit(2)
                }
                if !item.foot.isEmpty {
                    Text(item.foot)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

private struct DlHistorySheet: View {
    let store: HistoryStore
    let key: String
    let onOpenTitle: @MainActor (Int64) -> Void
    let onOpenDownload: @MainActor (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var confirm = false

    var body: some View {
        let item = store.info.item(key)
        NavigationStack {
            Group {
                if let item {
                    content(item)
                } else {
                    Color.clear
                }
            }
            .background(Theme.background)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.history.done")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onChange(of: item == nil) { _, gone in
            if gone { dismiss() }
        }
        .confirmationDialog(L10n.tr("downloads.history.redo.confirm"), isPresented: $confirm, titleVisibility: .visible) {
            Button(L10n.tr("downloads.history.redo.button"), role: .destructive) { store.findAnother(key) }
        }
    }

    private func content(_ item: HistoryItemInfo) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                header(item)
                if !item.steps.isEmpty { steps(item) }
                let line = item.line.message
                if !line.isEmpty {
                    Text(line)
                        .font(.callout)
                }
                facts(item)
                buttons(item)
            }
            .padding(Theme.Spacing.screen)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func header(_ item: HistoryItemInfo) -> some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            DlPosterThumb(source: item.poster, name: item.title.isEmpty ? "?" : item.title, square: item.square, width: item.square ? 64 : 56)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                StatusChip(text: item.verb.label, tone: item.tone)
                if !item.title.isEmpty {
                    Text(item.title)
                        .font(.headline)
                }
                if !item.bits.isEmpty {
                    Text(item.bits)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    private func steps(_ item: HistoryItemInfo) -> some View {
        DlSheetSection(title: L10n.tr("downloads.history.steps")) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(item.steps) { step in
                        StatusChip(text: step.label, tone: step.tone)
                    }
                }
            }
        }
    }

    private func whenText(_ item: HistoryItemInfo) -> String {
        guard let at = item.at else { return "" }
        return DownloadsText.dateTime(at) + " · " + Format.relative(at)
    }

    @ViewBuilder
    private func facts(_ item: HistoryItemInfo) -> some View {
        let when = whenText(item)
        if !when.isEmpty {
            DlFactRow(label: L10n.tr("downloads.history.fact.when"), value: when)
        }
        let by = item.who.message
        if !by.isEmpty {
            DlFactRow(label: L10n.tr("downloads.history.fact.by"), value: by)
        }
        ForEach(item.facts) { fact in
            if !fact.text.trimmingCharacters(in: .whitespaces).isEmpty {
                HStack(alignment: .firstTextBaseline, spacing: Theme.Spacing.small) {
                    DlFactRow(label: fact.kind.label, value: fact.text)
                    if fact.kind.copyable {
                        Button(L10n.tr("downloads.history.copy")) { UIPasteboard.general.string = fact.text }
                            .font(.footnote)
                            .buttonStyle(.borderless)
                    }
                }
            }
        }
    }

    private func buttons(_ item: HistoryItemInfo) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                if let redo = item.redo {
                    Button(redo == .findAnother ? L10n.tr("downloads.history.find.another") : L10n.tr("downloads.history.replace")) {
                        confirm = true
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(store.info.busy.contains(item.key))
                }
                if item.titleId > 0 {
                    Button(L10n.tr("downloads.action.open")) { onOpenTitle(item.titleId) }
                        .buttonStyle(.bordered)
                }
                if item.downloadId > 0 {
                    Button(L10n.tr("downloads.history.open.downloads")) { onOpenDownload("d\(item.downloadId)") }
                        .buttonStyle(.bordered)
                }
            }
        }
        .padding(.top, Theme.Spacing.small)
    }
}

private struct HistoryFilterSheet: View {
    let store: HistoryStore
    @Environment(\.dismiss) private var dismiss
    @State private var filter: HistoryFilterInfo
    @State private var fromDate = Date()
    @State private var toDate = Date()

    init(store: HistoryStore) {
        self.store = store
        let current = store.info.filter
        _filter = State(initialValue: current)
        _fromDate = State(initialValue: HistoryFilterSheet.date(current.from) ?? Date())
        _toDate = State(initialValue: HistoryFilterSheet.date(current.to) ?? Date())
    }

    nonisolated private static func date(_ text: String) -> Date? {
        let parts = text.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return Calendar.current.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(L10n.tr("downloads.history.event")) {
                    ForEach(HistoryStepKind.allCases) { step in
                        DlChoiceRow(text: step.label, selected: filter.steps.contains(step)) { toggle(step) }
                    }
                }
                Section(L10n.tr("downloads.history.type")) {
                    ForEach(HistoryTypeKind.allCases) { type in
                        DlChoiceRow(text: type.label, selected: filter.type == type) { filter.type = type }
                    }
                }
                Section(L10n.tr("downloads.history.who")) {
                    ForEach(HistoryWhoKind.allCases) { who in
                        DlChoiceRow(text: who.label, selected: filter.who == who) { filter.who = who }
                    }
                }
                whenSection
                sourceSection
                Section {
                    Button(L10n.tr("downloads.history.clear")) { filter = .initial }
                }
            }
            .navigationTitle(L10n.tr("downloads.history.filter"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.history.done")) {
                        store.setFilter(filter)
                        dismiss()
                    }
                    .disabled(filter.badRange)
                }
            }
        }
        .presentationDetents([.large])
    }

    private func toggle(_ step: HistoryStepKind) {
        if filter.steps.contains(step) {
            filter.steps.remove(step)
        } else {
            filter.steps.insert(step)
        }
    }

    private var whenSection: some View {
        Section {
            ForEach(HistoryWhenKind.allCases) { period in
                DlChoiceRow(text: period.label, selected: filter.period == period) { filter.period = period }
            }
            if filter.period == .custom {
                customRow(title: L10n.tr("downloads.history.from"), text: filter.from, date: $fromDate) { value in
                    filter.from = value
                }
                customRow(title: L10n.tr("downloads.history.to"), text: filter.to, date: $toDate) { value in
                    filter.to = value
                }
            }
        } header: {
            Text(L10n.tr("downloads.history.when"))
        } footer: {
            if filter.badRange {
                Text(L10n.tr("downloads.history.bad.range"))
                    .foregroundStyle(Theme.failText)
            }
        }
    }

    private func customRow(
        title: String,
        text: String,
        date: Binding<Date>,
        set: @escaping @MainActor (String) -> Void
    ) -> some View {
        HStack {
            Text(title)
            Spacer(minLength: Theme.Spacing.small)
            if text.isEmpty {
                Button("–") { set(DlDay(date: date.wrappedValue).key) }
                    .buttonStyle(.bordered)
            } else {
                DatePicker("", selection: date, displayedComponents: .date)
                    .labelsHidden()
                    .onChange(of: date.wrappedValue) { _, value in set(DlDay(date: value).key) }
                Button {
                    set("")
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Theme.textFaint)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(L10n.tr("downloads.history.clear"))
            }
        }
    }

    @ViewBuilder
    private var sourceSection: some View {
        if store.info.canSources && !store.info.sources.isEmpty {
            Section(L10n.tr("downloads.history.source")) {
                DlChoiceRow(text: L10n.tr("downloads.history.all.sources"), selected: filter.source == 0) { filter.source = 0 }
                ForEach(store.info.sources) { source in
                    DlChoiceRow(text: source.name, selected: filter.source == source.id) { filter.source = source.id }
                }
            }
        }
    }
}

private extension View {
    func plainRow() -> some View {
        listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
    }
}
