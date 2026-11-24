import SwiftUI
import Observation

@MainActor
@Observable
final class LibrarySubtitlesStore {
    private(set) var info: LibrarySubtitlesInfo = .initial

    private let driver = SDKClient.shared.makeLibrarySubtitles()

    var language: String {
        get { info.language }
        set { setLanguage(newValue) }
    }

    var kind: String {
        get { info.kind }
        set { setKind(newValue) }
    }

    var titleId: Int64 {
        get { info.titleId }
        set { setTitle(newValue) }
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

    func setLanguage(_ code: String) {
        let driver = self.driver
        Task { await driver.setLanguage(code) }
    }

    func setKind(_ kind: String) {
        let driver = self.driver
        Task { await driver.setKind(kind) }
    }

    func setTitle(_ id: Int64) {
        let driver = self.driver
        Task { await driver.setTitle(id) }
    }

    func nextPage() {
        let driver = self.driver
        Task { await driver.nextPage() }
    }

    func previousPage() {
        let driver = self.driver
        Task { await driver.previousPage() }
    }

    func searchAll() {
        let driver = self.driver
        Task { await driver.searchAll() }
    }

    func searchRow(_ fileId: Int64) {
        let driver = self.driver
        Task { await driver.searchRow(fileId) }
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

struct LibrarySubtitlesView: View {
    @State private var store = LibrarySubtitlesStore()
    @State private var showFilters = false

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(
                title: store.info.filtered ? L10n.tr("library.nothing.matches") : L10n.tr("library.subs.none"),
                systemImage: "captions.bubble"
            )
        } content: {
            List {
                if let error = store.info.load.error {
                    NoticeBanner(text: LibraryText.error(error))
                        .listRowSeparator(.hidden)
                }
                Text(LibraryText.plural("library.subs.count", store.info.total))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .listRowSeparator(.hidden)
                ForEach(store.info.rows) { row in
                    LibrarySubtitlesRow(row: row) { store.searchRow(row.fileId) }
                }
            }
            .listStyle(.plain)
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr("library.subs.title"))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showFilters = true
                } label: {
                    Label(
                        L10n.tr("library.filter.title"),
                        systemImage: store.info.filtered ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle"
                    )
                }
            }
        }
        .libraryNotice(store.info.notice) { store.dismissNotice($0) }
        .safeAreaInset(edge: .bottom) {
            bottomBar
        }
        .sheet(isPresented: $showFilters) {
            LibrarySubtitlesFilterSheet(store: store)
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    @ViewBuilder
    private var bottomBar: some View {
        if store.info.total > 0 || store.info.searchTotal > 0 {
            VStack(spacing: Theme.Spacing.small) {
                if store.info.searchTotal > 0 {
                    progress
                }
                searchAllButton
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
                }
            }
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
            .background(.bar)
        }
    }

    private var progress: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            ProgressView(value: Double(store.info.searchDone), total: Double(max(store.info.searchTotal, 1))) {
                Text(L10n.tr("library.subs.progress.label"))
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(Theme.text)
            }
            .tint(Theme.accent)
            Text(progressText)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var progressText: String {
        if store.info.searching {
            return L10n.format("library.subs.searched", store.info.searchDone, store.info.searchTotal)
        }
        return LibraryText.plural("library.subs.done", store.info.searchTotal)
    }

    private var searchAllButton: some View {
        Button {
            store.searchAll()
        } label: {
            HStack {
                if store.info.queuing || store.info.searching {
                    ProgressView()
                        .tint(Theme.onAccent)
                }
                Label(L10n.tr("library.subs.search.all"), systemImage: "magnifyingglass")
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(store.info.total == 0 || store.info.queuing || store.info.searching)
    }
}

private struct LibrarySubtitlesRow: View {
    let row: LibrarySubtitlesRowInfo
    let search: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            LibraryPosterThumb(source: row.poster, name: row.title)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Text(languageLabel)
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(Theme.text)
                Text(statusText)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            Spacer(minLength: Theme.Spacing.small)
            Button(action: search) {
                if row.busy {
                    ProgressView()
                } else {
                    Text(L10n.tr("library.search"))
                }
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .disabled(row.busy)
            .accessibilityLabel(L10n.format("library.subs.search.for", row.languageName, name))
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var name: String {
        guard row.isEpisode else { return row.title }
        return row.title + " S" + padded(row.season) + "E" + padded(row.episode)
    }

    private func padded(_ number: Int) -> String {
        number < 10 ? "0\(number)" : "\(number)"
    }

    private var languageLabel: String {
        var parts = [row.languageName]
        if row.hearingImpaired { parts.append(L10n.tr("library.subs.hearing.only")) }
        if row.forced { parts.append(L10n.tr("library.subs.forced.only")) }
        return parts.joined(separator: ", ")
    }

    private var statusText: String {
        if row.state == "none_good_enough" {
            var text = LibraryText.plural("library.subs.none.good", row.tries)
            if !row.nextTryAt.isEmpty {
                text += "; " + L10n.format("library.subs.try.next", LibraryText.dateTime(row.nextTryAt))
            }
            return text
        }
        if row.tries > 0 {
            return LibraryText.plural("library.subs.not.found", row.tries)
        }
        return L10n.tr("library.subs.not.looked")
    }
}

private struct LibrarySubtitlesFilterSheet: View {
    let store: LibrarySubtitlesStore

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Picker(L10n.tr("library.subs.language"), selection: Bindable(store).language) {
                    Text(L10n.tr("library.subs.every")).tag("")
                    ForEach(store.info.languages) { language in
                        Text(language.name).tag(language.code)
                    }
                }
                Picker(L10n.tr("library.subs.kind"), selection: Bindable(store).kind) {
                    Text(L10n.tr("library.subs.kind.both")).tag("")
                    Text(L10n.tr("library.kind.movie")).tag("movie")
                    Text(L10n.tr("library.subs.kind.episode")).tag("episode")
                }
                Picker(L10n.tr("library.subs.movie.or.show"), selection: Bindable(store).titleId) {
                    Text(L10n.tr("library.subs.all.titles")).tag(Int64(0))
                    ForEach(store.info.titles) { title in
                        Text(title.name + " (" + title.count.formatted() + ")").tag(title.id)
                    }
                }
            }
            .navigationTitle(L10n.tr("library.filter.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("library.close")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
