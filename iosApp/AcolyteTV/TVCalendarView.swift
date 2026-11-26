import SwiftUI

struct TVCalendarView: View {
    let account: AccountInfo
    @State private var store = CalendarStore()
    @State private var ambient: String?
    @FocusState private var focus: String?
    @Environment(\.scenePhase) private var scenePhase
    @Environment(TVNavigator.self) private var navigator

    private var info: CalendarInfo { store.info }

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            TVBackdropLayer(source: ambient, blurred: true)
            TVStatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                EmptyView()
            } content: {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: Theme.TV.textGap) {
                        if let error = info.load.error {
                            TVNoticeBanner(text: error.localized)
                        }
                        monthBar
                        filters
                        if info.hidden > 0 {
                            hiddenRow
                        }
                        if info.days.isEmpty {
                            TVEmptyView(
                                title: L10n.tr("downloads.calendar.empty.title"),
                                message: L10n.tr("downloads.calendar.empty"),
                                systemImage: "calendar"
                            )
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, Theme.TV.sectionGap)
                        }
                        ForEach(info.days) { day in
                            shelf(day)
                        }
                    }
                }
            }
        }
        .screenshotFocus($focus, id: info.days.first?.groups.first?.key, screen: "calendar")
        .onChange(of: focus) { _, new in
            guard let new, let group = info.group(new) else { return }
            ambient = group.art
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await store.refresh() } }
        }
    }

    private var monthBar: some View {
        HStack(spacing: Theme.TV.gutter) {
            TVActionButton(title: L10n.tr("downloads.calendar.previous"), systemImage: "chevron.left") { store.previous() }
                .disabled(!info.canPrevious)
            Text(DownloadsText.monthTitle(info.month))
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
            TVActionButton(title: L10n.tr("downloads.calendar.next"), systemImage: "chevron.right") { store.next() }
                .disabled(!info.canNext)
            TVActionButton(title: L10n.tr("downloads.calendar.today"), systemImage: "calendar.badge.clock") { store.today() }
        }
        .padding(.vertical, Theme.TV.focusPad)
        .focusSection()
    }

    private var filters: some View {
        VStack(alignment: .leading, spacing: 0) {
            TVChipRow {
                ForEach(CalendarTypeKind.allCases.filter { $0 != .music || info.musicOn }) { type in
                    TVChip(title: type.label, selected: type == info.type) { store.setType(type) }
                }
            }
            TVChipRow {
                ForEach(CalendarFollowsKind.allCases.filter { $0 != .everyone || info.people > 1 }) { follows in
                    TVChip(title: follows.label, selected: follows == info.follows) { store.setFollows(follows) }
                }
            }
        }
    }

    private var hiddenRow: some View {
        HStack(spacing: Theme.TV.gutter) {
            Text(L10n.format("downloads.calendar.hidden", info.hidden))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            TVActionButton(title: L10n.tr("downloads.calendar.show.library"), systemImage: "books.vertical") {
                store.setFollows(.library)
            }
        }
        .focusSection()
    }

    private func shelf(_ day: CalendarDayInfo) -> some View {
        TVShelf(title: DownloadsText.dayHeading(day.day, today: info.today, yesterday: false)) {
            ForEach(day.groups) { group in
                TVBackdropCard(
                    title: group.title,
                    subtitle: group.headline,
                    detail: group.captionLines.joined(separator: " · "),
                    chip: TVChipInfo(text: group.dot.label, tone: group.dot.tone),
                    image: group.art,
                    focus: TVFocusKey(binding: $focus, id: group.key)
                ) {
                    if let link = store.link(group) { navigator.open(link.route) }
                }
            }
        }
    }
}
