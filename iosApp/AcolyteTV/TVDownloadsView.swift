import SwiftUI

struct TVDownloadsView: View {
    let account: AccountInfo
    @State private var store = DownloadsStore()
    @State private var ambient: String?
    @FocusState private var focus: String?
    @Environment(\.scenePhase) private var scenePhase
    @Environment(TVNavigator.self) private var navigator

    private var info: DownloadsInfo { store.info }

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            TVBackdropLayer(source: ambient, blurred: true)
            TVStatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
                TVEmptyView(
                    title: info.manager ? L10n.tr("downloads.empty.all.title") : L10n.tr("downloads.empty.watch.title"),
                    message: info.manager ? L10n.tr("downloads.empty.manager") : L10n.tr("downloads.empty.watch"),
                    systemImage: "arrow.down.to.line"
                )
            } content: {
                list
            }
        }
        .onChange(of: store.route) { _, route in
            guard let route else { return }
            store.route = nil
            navigator.open(route)
        }
        .onChange(of: focus) { _, new in
            guard let new, let item = info.item(new) else { return }
            ambient = item.poster
        }
        .task { await store.watch() }
        .task { await store.start(show: "") }
        .task { await store.followLive() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await store.refresh() } }
        }
    }

    private var list: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: Theme.TV.textGap) {
                if let error = info.load.error {
                    TVNoticeBanner(text: error.localized)
                }
                ForEach(info.notes) { note in
                    TVNoticeBanner(text: note.message)
                }
                if info.statusLine.review > 0 {
                    TVNoticeBanner(text: L10n.format("tv.downloads.review", info.statusLine.review))
                }
                TVChipRow {
                    ForEach(info.tabs) { count in
                        TVChip(title: count.tab.label + "  " + String(count.count), selected: info.filter.tab == count.tab) {
                            store.setTab(count.tab)
                        }
                    }
                }
                if !info.subs.isEmpty {
                    TVChipRow {
                        ForEach(info.subs) { count in
                            TVChip(title: count.sub.label + "  " + String(count.count), selected: info.filter.sub == count.sub) {
                                store.setSub(count.sub)
                            }
                        }
                    }
                }
                if info.manager {
                    Text(statusText)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .padding(.bottom, Theme.TV.textGap)
                }
                if info.items.isEmpty {
                    emptyFiltered
                }
                LazyVGrid(columns: Theme.TV.thumbColumns, alignment: .leading, spacing: Theme.TV.gutter) {
                    ForEach(info.items) { item in
                        card(item)
                    }
                }
                .focusSection()
                if info.moreOutside > 0 {
                    TVActionButton(title: L10n.format("downloads.show.more", info.moreOutside), systemImage: "arrow.down.circle") {
                        store.showMore()
                    }
                    .disabled(info.loadingMore)
                    .padding(.vertical, Theme.TV.focusPad)
                }
            }
        }
    }

    private var statusText: String {
        let line = info.statusLine
        var parts: [String] = [L10n.format("downloads.speed.down", DlFormat.speed(line.speed))]
        if line.free > 0 { parts.append(L10n.format("downloads.status.free", DlFormat.freeSize(line.free))) }
        if line.left > 0 { parts.append(L10n.format("downloads.time.left", DownloadsText.duration(line.left))) }
        if line.limited { parts.append(L10n.tr("downloads.status.limited")) }
        return parts.joined(separator: " · ")
    }

    private var emptyFiltered: some View {
        let sub = info.filter.sub
        let useSub = info.empty?.kind == .sub
        return TVEmptyView(
            title: useSub ? (sub ?? .decide).emptyTitle : info.filter.tab.emptyTitle,
            message: useSub ? nil : info.filter.tab.emptyMessage,
            systemImage: "arrow.down.to.line"
        )
        .frame(maxWidth: .infinity)
        .padding(.vertical, Theme.TV.sectionGap)
    }

    private func card(_ item: DownloadItemInfo) -> some View {
        TVThumbCard(
            image: item.poster,
            name: item.title,
            square: item.square,
            focus: TVFocusKey(binding: $focus, id: item.key)
        ) {
            if item.titleId > 0 { store.openTitle(item.titleId) }
        } content: {
            cardContent(item)
        }
    }

    @ViewBuilder
    private func cardContent(_ item: DownloadItemInfo) -> some View {
        Text(item.title)
            .font(.callout.weight(.semibold))
            .foregroundStyle(Theme.text)
            .lineLimit(2)
        if !item.subLine.display.isEmpty {
            Text(item.subLine.display)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .lineLimit(1)
        }
        HStack(spacing: 8) {
            Text(item.word.label)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Theme.textColor(item.tone))
                .lineLimit(1)
                .layoutPriority(1)
            if !item.secondLine.isEmpty {
                Text("· " + item.secondLine)
                    .font(.footnote)
                    .foregroundStyle(item.problem.map { Theme.textColor($0.tone) } ?? Theme.textMuted)
                    .lineLimit(1)
            }
        }
        ProgressView(value: min(max(item.progress, 0), 1))
            .tint(item.bar.tvColor)
        if !item.factsLine.isEmpty {
            Text(item.factsLine)
                .font(.caption)
                .foregroundStyle(Theme.textMuted)
                .lineLimit(1)
        }
    }
}

private extension BarToneKind {
    var tvColor: Color {
        switch self {
        case .go, .done: Theme.ok
        case .queued: Theme.info
        case .held: Theme.wait
        case .bad: Theme.textFaint
        }
    }
}
