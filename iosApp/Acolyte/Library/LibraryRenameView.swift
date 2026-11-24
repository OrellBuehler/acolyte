import SwiftUI
import Observation

@MainActor
@Observable
final class LibraryRenameStore {
    private(set) var info: LibraryRenameInfo = .initial

    private let driver = SDKClient.shared.makeLibraryRename()

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

    func rename() {
        let driver = self.driver
        Task { await driver.rename() }
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

struct LibraryRenameView: View {
    @State private var store = LibraryRenameStore()
    @State private var confirm = false
    @State private var skippedOpen = false

    var body: some View {
        StatusContent(
            load: store.info.load,
            isEmpty: store.info.isEmpty && store.info.same == 0,
            reload: { await store.refresh() }
        ) {
            EmptyStateView(title: L10n.tr("library.rename.nothing"), systemImage: "checkmark.circle")
        } content: {
            list
        }
        .navigationTitle(L10n.tr("library.rename.title"))
        .libraryNotice(store.info.notice) { store.dismissNotice($0) }
        .safeAreaInset(edge: .bottom) {
            renameBar
        }
        .confirmationDialog(
            LibraryText.plural("library.rename.confirm", store.info.changes.count),
            isPresented: $confirm,
            titleVisibility: .visible
        ) {
            Button(LibraryText.plural("library.rename.button", store.info.changes.count)) { store.rename() }
        } message: {
            Text(L10n.tr("library.rename.confirm.note"))
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var list: some View {
        List {
            if let error = store.info.load.error {
                NoticeBanner(text: LibraryText.error(error))
                    .listRowSeparator(.hidden)
            }
            if !store.info.changes.isEmpty {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(LibraryText.plural("library.rename.count", store.info.changes.count))
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Theme.text)
                    Text(L10n.tr("library.rename.note"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                .listRowSeparator(.hidden)
            }
            failureBanner
            if store.info.running {
                LibraryRenameProgress(job: store.info.job)
                    .listRowSeparator(.hidden)
            }
            changes
            skipped
            if store.info.same > 0 {
                Text(L10n.format("library.rename.already.right", store.info.same))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .refreshable { await store.refresh() }
    }

    @ViewBuilder
    private var failureBanner: some View {
        switch store.info.failure {
        case .nothing:
            EmptyView()
        case .planChanged:
            NoticeBanner(text: L10n.tr("library.rename.changed"))
                .listRowSeparator(.hidden)
        case .failed:
            NoticeBanner(text: store.info.failureText.isEmpty ? L10n.tr("library.rename.unfinished") : store.info.failureText)
                .listRowSeparator(.hidden)
        }
    }

    @ViewBuilder
    private var changes: some View {
        if store.info.changes.isEmpty {
            Text(L10n.tr("library.rename.nothing"))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .listRowSeparator(.hidden)
        } else {
            ForEach(store.info.changes) { row in
                LibraryRenameChangeRow(row: row)
            }
        }
    }

    @ViewBuilder
    private var skipped: some View {
        if !store.info.skipped.isEmpty {
            if store.info.changes.isEmpty {
                Text(LibraryText.plural("library.rename.skipped", store.info.skipped.count))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                ForEach(store.info.skipped) { row in
                    LibraryRenameSkippedRow(row: row)
                }
            } else {
                DisclosureGroup(isExpanded: $skippedOpen) {
                    ForEach(store.info.skipped) { row in
                        LibraryRenameSkippedRow(row: row)
                    }
                } label: {
                    Text(LibraryText.plural("library.rename.skipped", store.info.skipped.count))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                }
            }
        }
    }

    @ViewBuilder
    private var renameBar: some View {
        if !store.info.changes.isEmpty {
            VStack(spacing: Theme.Spacing.xSmall) {
                if !store.info.canRename {
                    Text(L10n.tr("library.notice.hands.off.on"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Button {
                    confirm = true
                } label: {
                    HStack {
                        if store.info.running {
                            ProgressView()
                                .tint(Theme.onAccent)
                        }
                        Text(LibraryText.plural("library.rename.button", store.info.changes.count))
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(!store.info.canRename || store.info.running)
            }
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
            .background(.bar)
        }
    }
}

private struct LibraryRenameProgress: View {
    let job: JobProgressInfo

    private var note: String {
        if !job.note.isEmpty { return job.note }
        return job.progress > 0 ? L10n.tr("library.rename.renaming") : L10n.tr("library.rename.waiting")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            ProgressView(value: min(max(job.progress, 0), 1)) {
                Text(L10n.tr("library.rename.progress"))
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(Theme.text)
            }
            .tint(Theme.accent)
            Text(note)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct LibraryRenameChangeRow: View {
    let row: LibraryRenameRowInfo

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            LibraryPosterThumb(source: row.poster, name: row.title)
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(row.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                namePair(label: L10n.tr("library.rename.now"), value: row.from, color: Theme.textMuted)
                namePair(label: L10n.tr("library.rename.new"), value: row.to, color: Theme.text)
                ForEach(row.subtitles) { subtitle in
                    Text(subtitleLine(subtitle))
                        .font(.caption)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private func namePair(label: String, value: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(Theme.textFaint)
            Text(value)
                .font(.footnote.monospaced())
                .foregroundStyle(color)
        }
    }

    private func subtitleLine(_ subtitle: LibraryRenameSubtitleInfo) -> String {
        if subtitle.to.isEmpty {
            return L10n.format("library.rename.sub.stays", subtitle.from, subtitle.skip)
        }
        return L10n.format("library.rename.sub.moves", subtitle.from, subtitle.to)
    }
}

private struct LibraryRenameSkippedRow: View {
    let row: LibraryRenameRowInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(row.title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            Text(LibraryRenameSkippedRow.fileName(row.from))
                .font(.footnote.monospaced())
                .foregroundStyle(Theme.textMuted)
            Text(row.skip)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private static func fileName(_ path: String) -> String {
        let normalized = path.replacingOccurrences(of: "\\", with: "/")
        return normalized.split(separator: "/").last.map(String.init) ?? path
    }
}
