import SwiftUI
import Observation

@MainActor
@Observable
final class LibraryCopiesStore {
    private(set) var info: LibraryCopiesInfo = .initial

    private let driver = SDKClient.shared.makeLibraryCopies()

    @ObservationIgnored private var searchTask: Task<Void, Never>?

    var codec: String {
        get { info.codec }
        set { setCodec(newValue) }
    }

    var sort: String {
        get { info.sort }
        set { setSort(newValue) }
    }

    var reviewSheet: LibraryCopiesReviewInfo? {
        get { info.review }
        set { if newValue == nil { cancelReview() } }
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

    func search(_ text: String) {
        searchTask?.cancel()
        let driver = self.driver
        searchTask = Task { await driver.search(text) }
    }

    func setCodec(_ codec: String) {
        let driver = self.driver
        Task { await driver.setCodec(codec) }
    }

    func setSort(_ sort: String) {
        let driver = self.driver
        Task { await driver.setSort(sort) }
    }

    func nextPage() {
        let driver = self.driver
        Task { await driver.nextPage() }
    }

    func previousPage() {
        let driver = self.driver
        Task { await driver.previousPage() }
    }

    func toggle(_ fileId: Int64) {
        driver.toggle(fileId)
    }

    func review() {
        let driver = self.driver
        Task { await driver.review() }
    }

    func cancelReview() {
        driver.cancelReview()
    }

    func start() {
        let driver = self.driver
        Task { await driver.start() }
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

enum LibraryCopiesText {
    static func containerWord(_ container: String) -> String {
        switch container {
        case "matroska", "matroska,webm": "MKV"
        case "mp4", "mov,mp4,m4a,3gp,3g2,mj2": "MP4"
        case "avi": "AVI"
        case "mpegts": "TS"
        default: container.uppercased()
        }
    }

    static func codecLine(_ row: LibraryCopyRowInfo) -> String {
        var parts: [String] = []
        if !row.container.isEmpty { parts.append(containerWord(row.container)) }
        if !row.inspectionStatus.isEmpty, row.inspectionStatus != "complete" { parts.append(row.inspectionStatus) }
        if parts.isEmpty { parts.append(L10n.tr("library.codecs.not.read")) }
        return LibraryText.codec(row.codec) + " · " + parts.joined(separator: " · ")
    }

    static func audioLine(_ row: LibraryCopyRowInfo) -> String {
        let languages = row.audioLanguages
            .map { Locale.current.localizedString(forLanguageCode: $0) ?? $0 }
            .joined(separator: ", ")
        var parts = [languages.isEmpty ? "—" : languages]
        parts.append(LibraryText.plural("library.codecs.tracks", row.audioTracks))
        if row.audioUnknown { parts.append(L10n.tr("library.codecs.some.unknown")) }
        return parts.joined(separator: " · ")
    }

    static func versionLine(_ row: LibraryCopyRowInfo) -> String {
        row.edition.isEmpty ? row.version : row.version + " · " + row.edition
    }

    static func sortName(_ sort: String) -> String {
        switch sort {
        case "title": L10n.tr("library.codecs.sort.title")
        case "codec": L10n.tr("library.codecs.sort.codec")
        case "added": L10n.tr("library.codecs.sort.added")
        default: sort
        }
    }
}

struct LibraryCopiesView: View {
    @State private var store = LibraryCopiesStore()
    @State private var query = ""

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(
                title: query.isEmpty ? L10n.tr("library.nothing.matches") : L10n.format("library.nothing.for", query),
                systemImage: "film"
            )
        } content: {
            List {
                if let error = store.info.load.error {
                    NoticeBanner(text: LibraryText.error(error))
                        .listRowSeparator(.hidden)
                }
                if store.info.started >= 0 {
                    Label(LibraryText.plural("library.codecs.started", store.info.started), systemImage: "checkmark.circle.fill")
                        .font(.callout.weight(.medium))
                        .foregroundStyle(Theme.okText)
                        .listRowSeparator(.hidden)
                }
                Text(LibraryText.plural("library.codecs.files", store.info.total))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .listRowSeparator(.hidden)
                ForEach(store.info.rows) { row in
                    LibraryCopyRow(row: row) { store.toggle(row.id) }
                }
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
                    .listRowSeparator(.hidden)
                }
            }
            .listStyle(.plain)
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr("library.codecs.title"))
        .searchable(text: $query, prompt: L10n.tr("library.search.movie"))
        .onChange(of: query) { _, text in store.search(text) }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                filterMenu
            }
        }
        .libraryNotice(store.info.notice) { store.dismissNotice($0) }
        .safeAreaInset(edge: .bottom) {
            reviewBar
        }
        .sheet(item: Bindable(store).reviewSheet) { review in
            LibraryCopiesReviewSheet(review: review, busy: store.info.busy) { store.start() }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var filterMenu: some View {
        Menu {
            Section(L10n.tr("library.filter.codec.title")) {
                Picker(L10n.tr("library.filter.codec.title"), selection: Bindable(store).codec) {
                    ForEach(store.info.codecs) { option in
                        Text(codecLabel(option)).tag(option.id)
                    }
                }
            }
            Section(L10n.tr("library.codecs.sort")) {
                Picker(L10n.tr("library.codecs.sort"), selection: Bindable(store).sort) {
                    ForEach(["title", "codec", "added"], id: \.self) { sort in
                        Text(LibraryCopiesText.sortName(sort)).tag(sort)
                    }
                }
            }
        } label: {
            Label(
                L10n.tr("library.filter.title"),
                systemImage: store.info.codec == "all" ? "line.3.horizontal.decrease.circle" : "line.3.horizontal.decrease.circle.fill"
            )
        }
    }

    private func codecLabel(_ option: FilterOptionInfo) -> String {
        if option.id == "all" { return L10n.tr("library.codecs.all") }
        return LibraryText.codec(option.id) + " (" + option.count.formatted() + ")"
    }

    @ViewBuilder
    private var reviewBar: some View {
        if !store.info.rows.isEmpty {
            Button {
                store.review()
            } label: {
                HStack {
                    if store.info.busy {
                        ProgressView()
                            .tint(Theme.onAccent)
                    }
                    Text(reviewTitle)
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(store.info.selectedCount == 0 || store.info.busy)
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
            .background(.bar)
        }
    }

    private var reviewTitle: String {
        if store.info.selectedCount == 0 { return L10n.tr("library.codecs.review") }
        return LibraryText.plural("library.codecs.review.n", store.info.selectedCount)
    }
}

private struct LibraryCopyRow: View {
    let row: LibraryCopyRowInfo
    let toggle: () -> Void

    var body: some View {
        Button(action: toggle) {
            HStack(alignment: .top, spacing: Theme.Spacing.medium) {
                Image(systemName: row.selected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(row.selected ? Theme.accent : Theme.textFaint)
                    .padding(.top, Theme.Spacing.xSmall)
                LibraryPosterThumb(source: row.poster, name: row.title)
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(titleText)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                    Text(LibraryCopiesText.versionLine(row))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    Text(LibraryCopiesText.codecLine(row))
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(Theme.text)
                    Text(LibraryCopiesText.audioLine(row))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    Text(row.fileName)
                        .font(.caption)
                        .foregroundStyle(Theme.textFaint)
                        .lineLimit(2)
                        .truncationMode(.middle)
                    if row.isProtected {
                        StatusChip(text: L10n.tr("library.codecs.protected"), tone: .info)
                    }
                }
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(row.isProtected)
        .opacity(row.isProtected ? 0.7 : 1)
        .accessibilityAddTraits(row.selected ? .isSelected : [])
    }

    private var titleText: String {
        row.year > 0 ? "\(row.title) (\(row.year))" : row.title
    }
}

private struct LibraryCopiesReviewSheet: View {
    let review: LibraryCopiesReviewInfo
    let busy: Bool
    let start: @MainActor () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Text(LibraryText.plural("library.codecs.review.note", review.slotCount))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                    .listRowSeparator(.hidden)
                ForEach(review.items) { item in
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(item.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Theme.text)
                        Text(L10n.format("library.copy.line", item.version, LibraryText.codec(item.codec)))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
            .listStyle(.plain)
            .navigationTitle(LibraryText.plural("library.codecs.review.title", review.items.count))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("library.start")) { start() }
                        .disabled(busy)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
