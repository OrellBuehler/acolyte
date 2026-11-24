import SwiftUI
import Observation

@MainActor
@Observable
final class LibraryDeletedStore {
    private(set) var info: LibraryDeletedInfo = .initial

    private let driver: LibraryDeletedDriver

    init(titleId: Int64) {
        driver = SDKClient.shared.makeLibraryDeleted(titleId: titleId)
    }

    var show: LibraryDeletedShowKind {
        get { info.show }
        set { driver.setShow(newValue) }
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
            self?.resetShow(value)
        }
    }

    private func resetShow(_ value: LibraryDeletedInfo) {
        if !value.hasBoth, value.show != .all {
            driver.setShow(.all)
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func clearTitle() {
        let driver = self.driver
        Task { await driver.clearTitle() }
    }

    func toggle(_ id: Int64) {
        driver.toggle(id)
    }

    func toggleAll() {
        driver.toggleAll()
    }

    func restore() {
        let driver = self.driver
        Task { await driver.restore() }
    }

    func answer(_ how: String) {
        let driver = self.driver
        Task { await driver.answer(how) }
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

struct LibraryDeletedView: View {
    @State private var store: LibraryDeletedStore
    @State private var dialogOpen = false

    init(titleId: Int64) {
        _store = State(initialValue: LibraryDeletedStore(titleId: titleId))
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
                EmptyStateView(title: L10n.tr("library.deleted.none"), systemImage: "trash")
            } content: {
                list
            }
        }
        .navigationTitle(L10n.tr("library.deleted"))
        .libraryNotice(store.info.notice) { store.dismissNotice($0) }
        .safeAreaInset(edge: .bottom) {
            restoreBar
        }
        .confirmationDialog(
            L10n.tr("library.deleted.taken.title"),
            isPresented: $dialogOpen,
            titleVisibility: .visible,
            presenting: store.info.question
        ) { question in
            if question.canSwap {
                Button(L10n.tr("library.deleted.swap")) { store.answer("swap") }
            }
            Button(L10n.tr("library.deleted.keep.both")) { store.answer("both") }
            Button(L10n.tr("library.cancel"), role: .cancel) { store.answer("") }
        } message: { question in
            Text(takenMessage(question))
        }
        .onChange(of: store.info.question?.id) { _, id in
            dialogOpen = id != nil
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    @ViewBuilder
    private var header: some View {
        if store.info.titleId != 0 || store.info.hasBoth {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                if store.info.titleId != 0 {
                    titleChip
                }
                if store.info.hasBoth {
                    Picker(L10n.tr("library.deleted.show"), selection: Bindable(store).show) {
                        Text(L10n.tr("library.deleted.show.all")).tag(LibraryDeletedShowKind.all)
                        Text(L10n.tr("library.deleted.tag.deleted")).tag(LibraryDeletedShowKind.deleted)
                        Text(L10n.tr("library.deleted.tag.replaced")).tag(LibraryDeletedShowKind.replaced)
                    }
                    .pickerStyle(.segmented)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
        }
    }

    private var titleChip: some View {
        let text = store.info.titleName.isEmpty
            ? L10n.tr("library.deleted.one.title")
            : L10n.format("library.deleted.only.title", store.info.titleName)
        return Button {
            store.clearTitle()
        } label: {
            HStack(spacing: Theme.Spacing.small) {
                Text(text)
                    .lineLimit(1)
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(Theme.textFaint)
            }
            .font(.footnote.weight(.medium))
            .foregroundStyle(Theme.text)
            .padding(.horizontal, Theme.Spacing.medium)
            .frame(height: 30)
            .background(Theme.surfaceRaised, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L10n.format("library.chip.remove", text))
    }

    private var list: some View {
        List {
            if let error = store.info.load.error {
                NoticeBanner(text: LibraryText.error(error))
                    .listRowSeparator(.hidden)
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(LibraryText.plural("library.deleted.files", store.info.count))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Text(noteText)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            .listRowSeparator(.hidden)
            Button {
                store.toggleAll()
            } label: {
                Label(
                    store.info.allSelected ? L10n.tr("library.select.none") : L10n.tr("library.select.all"),
                    systemImage: store.info.allSelected ? "checkmark.circle.fill" : "circle"
                )
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Theme.accentText)
            }
            .buttonStyle(.plain)
            ForEach(store.info.rows) { row in
                LibraryDeletedRow(row: row) { store.toggle(row.id) }
            }
        }
        .listStyle(.plain)
        .refreshable { await store.refresh() }
    }

    private var noteText: String {
        switch store.info.holdingDays {
        case ..<0: L10n.tr("library.deleted.note.unknown")
        case 0: L10n.tr("library.deleted.note.forever")
        default: LibraryText.plural("library.deleted.note.days", store.info.holdingDays)
        }
    }

    @ViewBuilder
    private var restoreBar: some View {
        if !store.info.rows.isEmpty {
            VStack(spacing: Theme.Spacing.xSmall) {
                if !store.info.canRestore {
                    Text(L10n.tr("library.notice.hands.off.on"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Button {
                    store.restore()
                } label: {
                    HStack {
                        if store.info.busy {
                            ProgressView()
                                .tint(Theme.onAccent)
                        }
                        Text(restoreTitle)
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(!store.info.canRestore || store.info.selectedCount == 0 || store.info.busy)
            }
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
            .background(.bar)
        }
    }

    private var restoreTitle: String {
        if store.info.selectedCount == 0 { return L10n.tr("library.deleted.restore") }
        return L10n.format("library.deleted.restore.n", store.info.selectedCount)
    }

    private func takenMessage(_ question: LibraryDeletedQuestionInfo) -> String {
        var lines = [L10n.format("library.deleted.taken.body", question.name)]
        if question.canSwap {
            lines.append(L10n.format("library.deleted.hint.line", L10n.tr("library.deleted.swap"), L10n.tr("library.deleted.swap.hint")))
        }
        lines.append(L10n.format("library.deleted.hint.line", L10n.tr("library.deleted.keep.both"), L10n.tr("library.deleted.keep.both.hint")))
        return lines.joined(separator: "\n")
    }
}

private struct LibraryDeletedRow: View {
    let row: LibraryDeletedRowInfo
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
                    HStack(spacing: Theme.Spacing.small) {
                        Text(row.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Theme.text)
                            .lineLimit(2)
                        StatusChip(
                            text: row.replaced ? L10n.tr("library.deleted.tag.replaced") : L10n.tr("library.deleted.tag.deleted"),
                            tone: row.replaced ? .info : .idle
                        )
                    }
                    Text(row.fileName)
                        .font(.caption)
                        .foregroundStyle(Theme.textFaint)
                        .lineLimit(2)
                        .truncationMode(.middle)
                    Text(detail)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                    Text(daysLeft)
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(row.daysLeft >= 0 && row.daysLeft <= 1 ? Theme.waitText : Theme.textMuted)
                }
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(row.selected ? .isSelected : [])
    }

    private var detail: String {
        let when = LibraryText.day(row.deletedAt)
        var text: String
        if row.replaced {
            text = row.replacedBy.isEmpty
                ? L10n.format("library.deleted.replaced.on", when)
                : L10n.format("library.deleted.replaced.by", when, row.replacedBy)
        } else {
            text = L10n.format("library.deleted.on", when)
        }
        if row.titleDeleted {
            text += " " + L10n.tr("library.deleted.with.title")
        }
        return LibraryText.bytes(row.size) + " · " + text
    }

    private var daysLeft: String {
        switch row.daysLeft {
        case ..<0: L10n.tr("library.deleted.kept")
        case 0: L10n.tr("library.deleted.today")
        default: LibraryText.plural("library.deleted.days.left", row.daysLeft)
        }
    }
}
