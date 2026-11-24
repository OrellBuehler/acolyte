import SwiftUI

private struct OpenGroup: Identifiable {
    let id: String
}

struct CalendarView: View {
    @State private var store = CalendarStore()
    @State private var filtering = false
    @State private var open: OpenGroup?

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            list
        }
        .navigationTitle(SectionID.calendar.title)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                viewToggle
                Button {
                    store.today()
                } label: {
                    Label(L10n.tr("downloads.calendar.today"), systemImage: "calendar.badge.clock")
                }
                Button {
                    filtering = true
                } label: {
                    Label(L10n.tr("downloads.calendar.filter"), systemImage: "line.3.horizontal.decrease.circle")
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { DlToastHost(model: store.toasts) }
        .sheet(isPresented: $filtering) {
            CalendarFilterSheet(store: store)
        }
        .sheet(item: $open) { group in
            CalendarEntrySheet(
                store: store,
                key: group.id,
                onOpenTitle: { link in
                    open = nil
                    store.route = link.route
                }
            )
        }
        .navigationDestination(item: $store.route) { route in
            DlRouteDestination(route: route)
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    @ViewBuilder
    private var viewToggle: some View {
        let month = store.info.view == .month
        Button {
            store.setView(month ? .agenda : .month)
        } label: {
            Label(
                month ? L10n.tr("downloads.calendar.agenda") : L10n.tr("downloads.calendar.month"),
                systemImage: month ? "list.bullet" : "calendar"
            )
        }
    }

    private var list: some View {
        List {
            if let error = store.info.load.error {
                NoticeBanner(text: error.localized).plainRow()
            }
            monthHeader.plainRow()
            if store.info.hidden > 0 {
                hiddenRow.plainRow()
            }
            if store.info.view == .month {
                monthBody
            } else {
                agendaBody
            }
        }
        .listStyle(.plain)
        .refreshable { await store.refresh() }
    }

    private var monthHeader: some View {
        HStack {
            Button {
                store.previous()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.body.weight(.semibold))
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.borderless)
            .disabled(!store.info.canPrevious)
            .accessibilityLabel(L10n.tr("downloads.calendar.previous"))
            Text(DownloadsText.monthTitle(store.info.month))
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
                .frame(maxWidth: .infinity)
            Button {
                store.next()
            } label: {
                Image(systemName: "chevron.right")
                    .font(.body.weight(.semibold))
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.borderless)
            .disabled(!store.info.canNext)
            .accessibilityLabel(L10n.tr("downloads.calendar.next"))
        }
    }

    private var hiddenRow: some View {
        HStack {
            Text(L10n.format("downloads.calendar.hidden", store.info.hidden))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Spacer(minLength: Theme.Spacing.small)
            Button(L10n.tr("downloads.calendar.show.library")) { store.setFollows(.library) }
                .font(.footnote)
                .buttonStyle(.borderless)
        }
    }

    @ViewBuilder
    private var monthBody: some View {
        MonthGrid(cells: store.info.cells, selected: store.info.selected) { day in store.select(day) }
            .plainRow(insets: EdgeInsets(top: 4, leading: 12, bottom: 4, trailing: 12))
        if let selected = store.info.selected {
            DlDayHeader(
                title: DownloadsText.dayHeading(selected, today: store.info.today, yesterday: false),
                highlighted: selected == store.info.today
            )
            .plainRow()
            let groups = store.info.selectedGroups
            if groups.isEmpty {
                Text(L10n.tr("downloads.calendar.day.empty"))
                    .foregroundStyle(Theme.textMuted)
                    .plainRow()
            }
            ForEach(groups) { group in
                entryRow(group)
            }
        }
    }

    @ViewBuilder
    private var agendaBody: some View {
        if store.info.days.isEmpty {
            DlInlineEmpty(
                title: L10n.tr("downloads.calendar.empty.title"),
                message: L10n.tr("downloads.calendar.empty"),
                systemImage: "calendar"
            )
            .plainRow()
        }
        ForEach(store.info.days) { day in
            Section {
                ForEach(day.groups) { group in
                    entryRow(group)
                }
            } header: {
                DlDayHeader(
                    title: DownloadsText.dayHeading(day.day, today: store.info.today, yesterday: false),
                    highlighted: day.day == store.info.today
                )
                .textCase(nil)
            }
        }
    }

    private func entryRow(_ group: CalendarGroupInfo) -> some View {
        EntryRow(
            group: group,
            busy: store.info.busy.contains("follow-\(group.leadTitleId)"),
            onFollow: { store.follow(group, on: !group.following) }
        )
        .contentShape(Rectangle())
        .onTapGesture { open = OpenGroup(id: group.key) }
        .accessibilityAddTraits(.isButton)
    }
}

private struct MonthGrid: View {
    let cells: [MonthCellInfo]
    let selected: DlDay?
    let onSelect: @MainActor (DlDay) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: 7)

    private var weekdays: [String] {
        let symbols = Calendar.current.veryShortStandaloneWeekdaySymbols
        guard symbols.count == 7 else { return symbols }
        return Array(symbols[1...]) + [symbols[0]]
    }

    var body: some View {
        VStack(spacing: Theme.Spacing.xSmall) {
            HStack(spacing: 0) {
                ForEach(Array(weekdays.enumerated()), id: \.offset) { entry in
                    Text(entry.element)
                        .font(.caption2)
                        .foregroundStyle(Theme.textMuted)
                        .frame(maxWidth: .infinity)
                }
            }
            LazyVGrid(columns: columns, spacing: 2) {
                ForEach(cells) { cell in
                    cellView(cell)
                }
            }
        }
    }

    private func textColor(_ cell: MonthCellInfo, selected isSelected: Bool) -> Color {
        if isSelected { return Theme.onAccent }
        if !cell.inMonth { return Theme.textFaint }
        return Theme.text
    }

    private func dotColor(_ cell: MonthCellInfo, selected isSelected: Bool) -> Color {
        guard let tone = cell.tone, cell.inMonth else { return Color.clear }
        return isSelected ? Theme.onAccent : Theme.dotColor(tone)
    }

    private func cellView(_ cell: MonthCellInfo) -> some View {
        let isSelected = cell.day == selected
        let shape = RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
        return Button {
            onSelect(cell.day)
        } label: {
            VStack(spacing: 2) {
                Text(String(cell.day.day))
                    .font(.callout.weight(cell.count > 0 ? .semibold : .regular))
                    .foregroundStyle(textColor(cell, selected: isSelected))
                Circle()
                    .fill(dotColor(cell, selected: isSelected))
                    .frame(width: 6, height: 6)
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .background(isSelected ? Theme.accent : Color.clear, in: shape)
            .overlay {
                if cell.today && !isSelected {
                    shape.strokeBorder(Theme.accent, lineWidth: 1)
                }
            }
            .contentShape(shape)
        }
        .buttonStyle(.plain)
        .disabled(!cell.inMonth)
        .accessibilityLabel(DownloadsText.day(cell.day))
    }
}

private struct EntryRow: View {
    let group: CalendarGroupInfo
    let busy: Bool
    let onFollow: @MainActor () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: Theme.Spacing.medium) {
            AuthImage(source: group.art, size: .backdrop) {
                ArtPlaceholderView(name: group.title)
            }
            .frame(width: 96, height: 54)
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.poster / 2, style: .continuous))
            VStack(alignment: .leading, spacing: 2) {
                Text(group.whenText + " · " + group.tagText)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Theme.accentText)
                    .lineLimit(1)
                Text(group.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                ForEach(Array(group.captionLines.enumerated()), id: \.offset) { line in
                    Text(line.element)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .lineLimit(1)
                }
                StatusChip(text: group.dot.label, tone: group.dot.tone)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: onFollow) {
                Image(systemName: group.following ? "bell.fill" : "bell")
                    .font(.body)
                    .foregroundStyle(group.following ? Theme.accentText : Theme.textMuted)
                    .frame(width: 40, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.borderless)
            .disabled(busy)
            .accessibilityLabel(
                group.following
                    ? L10n.format("downloads.calendar.unfollow", group.leadName)
                    : L10n.format("downloads.calendar.follow", group.leadName)
            )
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

private struct CalendarEntrySheet: View {
    let store: CalendarStore
    let key: String
    let onOpenTitle: @MainActor (DlTitleLink) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var texts: [String: String] = [:]

    var body: some View {
        let group = store.info.group(key)
        NavigationStack {
            Group {
                if let group {
                    content(group)
                } else {
                    Color.clear
                }
            }
            .background(Theme.background)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.close")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .task(id: key) {
            if let current = store.info.group(key) {
                texts = await store.describe(current)
            }
        }
        .onChange(of: group == nil) { _, gone in
            if gone { dismiss() }
        }
    }

    private func description(_ entryKey: String) -> String {
        texts[entryKey] ?? L10n.tr("downloads.calendar.no.description")
    }

    private func content(_ group: CalendarGroupInfo) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                AuthImage(source: group.art, size: .backdrop) {
                    ArtPlaceholderView(name: group.title)
                }
                .frame(maxWidth: .infinity)
                .aspectRatio(16.0 / 9.0, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
                Text(group.whenText + " · " + group.tagText)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Theme.accentText)
                Text(group.title)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(Theme.text)
                details(group)
                if !group.followers.isEmpty {
                    Text(L10n.format("downloads.calendar.followed.by", group.followers.joined(separator: ", ")))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                StatusChip(text: group.dot.label, tone: group.dot.tone)
                buttons(group)
            }
            .padding(Theme.Spacing.screen)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    @ViewBuilder
    private func details(_ group: CalendarGroupInfo) -> some View {
        if group.kind == "episode" {
            ForEach(group.entries) { entry in
                episode(entry, many: group.entries.count > 1)
            }
        } else {
            ForEach(Array(group.captionLines.enumerated()), id: \.offset) { line in
                Text(line.element)
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            if group.kind != "album" {
                Text(description(group.leadKey))
                    .font(.callout)
            }
        }
    }

    private func episode(_ entry: CalendarEntryInfo, many: Bool) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(spacing: Theme.Spacing.small) {
                Text(L10n.format("downloads.calendar.season.episode", entry.season, entry.episode))
                    .font(.subheadline.weight(.semibold))
                if many {
                    StatusChip(text: entry.dot.label, tone: entry.dot.tone)
                }
            }
            if !entry.episodeName.isEmpty {
                Text(entry.episodeName)
                    .font(.callout)
            }
            Text(description(entry.key))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private func buttons(_ group: CalendarGroupInfo) -> some View {
        let link = store.link(group)
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                if store.info.canSearch && group.canSearch {
                    Button(L10n.tr("downloads.calendar.search.now")) { store.search(group) }
                        .buttonStyle(.borderedProminent)
                        .disabled(store.info.busy.contains(group.key))
                }
                if let link {
                    Button(link.kind == "show" ? L10n.tr("downloads.calendar.go.show") : L10n.tr("downloads.calendar.go.movie")) {
                        onOpenTitle(link)
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
        .padding(.top, Theme.Spacing.small)
    }
}

private struct CalendarFilterSheet: View {
    let store: CalendarStore
    @Environment(\.dismiss) private var dismiss

    private var types: [CalendarTypeKind] {
        CalendarTypeKind.allCases.filter { $0 != .music || store.info.musicOn }
    }

    private var follows: [CalendarFollowsKind] {
        CalendarFollowsKind.allCases.filter { $0 != .everyone || store.info.people > 1 }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(L10n.tr("downloads.calendar.type")) {
                    ForEach(types) { type in
                        DlChoiceRow(text: type.label, selected: store.info.type == type) { store.setType(type) }
                    }
                }
                Section(L10n.tr("downloads.calendar.which")) {
                    ForEach(follows) { follow in
                        DlChoiceRow(text: follow.label, detail: follow.hint, selected: store.info.follows == follow) {
                            store.setFollows(follow)
                        }
                    }
                }
                if store.info.canSearch && store.info.missing > 0 {
                    Section {
                        Button(L10n.format("downloads.calendar.search.missing", store.info.missing)) {
                            store.searchMissing()
                            dismiss()
                        }
                        .disabled(store.info.busy.contains("missing"))
                    }
                }
            }
            .navigationTitle(L10n.tr("downloads.calendar.filter"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("downloads.history.done")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

private extension View {
    func plainRow(insets: EdgeInsets = EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16)) -> some View {
        listRowInsets(insets)
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
    }
}
