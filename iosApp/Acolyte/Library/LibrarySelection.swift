import SwiftUI

struct LibraryGridSelectBar: View {
    let store: LibraryStore
    let remove: @MainActor () -> Void
    let audio: @MainActor () -> Void

    var body: some View {
        let info = store.info
        let allPicked = !info.items.isEmpty && info.selectedCount >= info.items.count
        HStack(spacing: Theme.Spacing.medium) {
            Text(LibraryText.plural("library.selected", info.selectedCount))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
                .monospacedDigit()
                .lineLimit(1)
            Spacer(minLength: Theme.Spacing.small)
            if info.working {
                ProgressView()
            }
            Button(allPicked ? L10n.tr("library.select.none") : L10n.tr("library.select.all")) {
                if allPicked {
                    store.selectNone()
                } else {
                    store.selectAll()
                }
            }
            .font(.subheadline)
            actions(info)
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.small)
        .background(.bar)
        .overlay(alignment: .top) {
            Divider()
        }
    }

    private func actions(_ info: LibraryInfo) -> some View {
        Menu {
            Button(L10n.tr("library.action.search"), systemImage: "magnifyingglass") {
                store.searchSelected()
            }
            Button(L10n.tr("library.action.refresh"), systemImage: "arrow.clockwise") {
                store.refreshSelected()
            }
            Button(L10n.tr("library.action.keep"), systemImage: "bell") {
                store.keepSelected(true)
            }
            Button(L10n.tr("library.action.unkeep"), systemImage: "bell.slash") {
                store.keepSelected(false)
            }
            if !info.ruleOptions.isEmpty {
                qualityMenu(info)
            }
            if info.canSetAudio {
                Button(L10n.tr("library.action.audio"), systemImage: "waveform") {
                    audio()
                }
            }
            if !info.moveOptions.isEmpty {
                moveMenu(info)
            }
            if info.canFindCopies {
                Button(L10n.tr("library.action.copies"), systemImage: "doc.on.doc") {
                    store.reviewCopies()
                }
            }
            Divider()
            Button(L10n.tr("library.action.remove"), systemImage: "trash", role: .destructive) {
                remove()
            }
        } label: {
            Label(L10n.tr("library.actions"), systemImage: "ellipsis.circle")
                .labelStyle(.iconOnly)
                .font(.title3)
        }
        .disabled(info.working || info.selectedCount == 0)
    }

    private func qualityMenu(_ info: LibraryInfo) -> some View {
        Menu {
            ForEach(info.ruleOptions) { rule in
                Button(rule.label) {
                    store.setRules(rule.id)
                }
            }
        } label: {
            Label(L10n.tr("library.action.quality"), systemImage: "slider.horizontal.3")
        }
    }

    private func moveMenu(_ info: LibraryInfo) -> some View {
        Menu {
            ForEach(info.moveOptions) { folder in
                Button(folder.label) {
                    store.moveSelected(folder.id)
                }
            }
        } label: {
            Label(L10n.tr("library.action.move"), systemImage: "folder")
        }
    }
}

struct LibraryGridCopiesSheet: View {
    let store: LibraryStore
    let review: CopiesReviewInfo

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(review.items) { copy in
                        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                            Text(verbatim: copy.title)
                                .foregroundStyle(Theme.text)
                            Text(L10n.format("library.copy.line", copy.version, LibraryText.codec(copy.codec)))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                    }
                } header: {
                    Text(LibraryText.plural("library.copies.title", review.items.count))
                        .textCase(nil)
                        .font(.headline)
                        .foregroundStyle(Theme.text)
                } footer: {
                    Text(LibraryText.plural("library.copies.note", review.slotCount))
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.cancel")) {
                        store.cancelCopies()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("library.search")) {
                        store.startCopies()
                    }
                    .disabled(store.info.working)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
