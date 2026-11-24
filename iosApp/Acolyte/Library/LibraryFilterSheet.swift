import SwiftUI

private struct LibraryGridChoice: Hashable, Identifiable {
    let tag: String
    let text: String

    var id: String { tag }
}

private struct LibraryGridChoiceMenu: View {
    let title: String
    let choices: [LibraryGridChoice]
    let current: String
    let select: @MainActor (String) -> Void

    private var currentText: String {
        choices.first(where: { $0.tag == current })?.text ?? ""
    }

    var body: some View {
        Menu {
            ForEach(choices) { choice in
                Button {
                    select(choice.tag)
                } label: {
                    if choice.tag == current {
                        Label(choice.text, systemImage: "checkmark")
                    } else {
                        Text(choice.text)
                    }
                }
            }
        } label: {
            HStack(spacing: Theme.Spacing.small) {
                Text(title)
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                Text(currentText)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.footnote)
                    .foregroundStyle(Theme.textFaint)
            }
            .contentShape(Rectangle())
        }
    }
}

private struct LibraryGridCheckRow: View {
    let title: String
    let on: Bool
    let action: @MainActor () -> Void

    var body: some View {
        Button {
            action()
        } label: {
            HStack(spacing: Theme.Spacing.small) {
                Text(title)
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                Image(systemName: on ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(on ? Theme.accent : Theme.textFaint)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

struct LibraryFilterSheet: View {
    let store: LibraryStore
    let clear: @MainActor () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var naming = false
    @State private var saveName = ""

    private static let maxName = 60

    var body: some View {
        let info = store.info
        NavigationStack {
            List {
                sortSection(info)
                orderSection(info)
                filterSection(info)
                savedSection(info)
                clearSection(info)
            }
            .navigationTitle(L10n.tr("library.sort.filter"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("library.close")) {
                        dismiss()
                    }
                }
            }
            .alert(L10n.tr("library.saved.save"), isPresented: $naming) {
                TextField(L10n.tr("library.saved.name"), text: $saveName)
                Button(L10n.tr("library.cancel"), role: .cancel) {}
                Button(L10n.tr("library.save")) {
                    store.saveFilter(saveName)
                }
                .disabled(saveName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .onChange(of: saveName) { _, value in
                if value.count > Self.maxName {
                    saveName = String(value.prefix(Self.maxName))
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private func sortSection(_ info: LibraryInfo) -> some View {
        Section {
            ForEach(info.sorts, id: \.self) { sort in
                sortRow(sort, info: info)
            }
        } header: {
            Text(L10n.tr("library.sort.title"))
        }
    }

    private func sortRow(_ sort: LibrarySortID, info: LibraryInfo) -> some View {
        let current = sort == info.sort
        return Button {
            store.setSort(sort)
        } label: {
            HStack {
                Text(LibraryGridText.sortText(sort, kind: info.kind))
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                if current {
                    Image(systemName: info.descending ? "arrow.down" : "arrow.up")
                        .foregroundStyle(Theme.textMuted)
                    Image(systemName: "checkmark")
                        .foregroundStyle(Theme.accentText)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(current ? .isSelected : [])
    }

    private func orderSection(_ info: LibraryInfo) -> some View {
        Section {
            LibraryGridCheckRow(title: L10n.tr("library.sort.ascending"), on: !info.descending) {
                store.setDescending(false)
            }
            LibraryGridCheckRow(title: L10n.tr("library.sort.descending"), on: info.descending) {
                store.setDescending(true)
            }
            if info.canGroup {
                LibraryGridCheckRow(title: L10n.tr("library.group.series"), on: info.grouped) {
                    store.setGrouped(!info.grouped)
                }
            }
        } header: {
            Text(L10n.tr("library.sort.order"))
        }
    }

    private func filterSection(_ info: LibraryInfo) -> some View {
        Section {
            LibraryGridChoiceMenu(
                title: L10n.tr("library.filter.title"),
                choices: statusChoices(info),
                current: info.statusOptions.first(where: { $0.selected })?.id ?? "all",
                select: { chosen in store.setStatus(chosen) }
            )
            LibraryGridChoiceMenu(
                title: L10n.tr("library.filter.kept.title"),
                choices: keptChoices(info),
                current: info.keptOptions.first(where: { $0.selected })?.id ?? "",
                select: { chosen in selectKept(chosen, info: info) }
            )
            if showsAsker(info) {
                LibraryGridChoiceMenu(
                    title: L10n.tr("library.filter.asker.title"),
                    choices: askerChoices(info),
                    current: info.askerOptions.first(where: { $0.selected })?.id ?? "anyone",
                    select: { chosen in store.setAsker(chosen) }
                )
            }
            if !info.codecOptions.isEmpty {
                LibraryGridChoiceMenu(
                    title: L10n.tr("library.filter.codec.title"),
                    choices: codecChoices(info),
                    current: info.codecOptions.first(where: { $0.selected })?.id ?? "",
                    select: { chosen in selectCodec(chosen, info: info) }
                )
            }
            if info.seriesOptions.count > 1 {
                LibraryGridChoiceMenu(
                    title: L10n.tr("library.filter.series.title"),
                    choices: seriesChoices(info),
                    current: info.seriesOptions.first(where: { $0.selected })?.id ?? "0",
                    select: { chosen in store.setSeries(Int64(chosen) ?? 0) }
                )
            }
            if let option = info.subtitlesOption {
                LibraryGridCheckRow(title: LibraryGridText.countedText(L10n.tr("library.chip.subs"), option.count), on: option.selected) {
                    store.toggleSubtitles()
                }
            }
            if let option = info.watchedOption {
                LibraryGridCheckRow(title: LibraryGridText.countedText(L10n.tr("library.filter.watched.anyone"), option.count), on: option.selected) {
                    store.toggleWatched()
                }
            }
        }
    }

    private func showsAsker(_ info: LibraryInfo) -> Bool {
        info.askerOptions.contains { option in
            option.id != "anyone" && (option.count > 0 || option.selected)
        }
    }

    private func statusChoices(_ info: LibraryInfo) -> [LibraryGridChoice] {
        info.statusOptions.map { option in
            LibraryGridChoice(tag: option.id, text: LibraryGridText.countedText(LibraryGridText.statusText(option.id), option.count))
        }
    }

    private func keptChoices(_ info: LibraryInfo) -> [LibraryGridChoice] {
        var choices = [LibraryGridChoice(tag: "", text: L10n.tr("library.filter.all"))]
        for option in info.keptOptions {
            let name = option.id == "on" ? L10n.tr("library.filter.kept.on") : L10n.tr("library.filter.kept.off")
            choices.append(LibraryGridChoice(tag: option.id, text: LibraryGridText.countedText(name, option.count)))
        }
        return choices
    }

    private func askerChoices(_ info: LibraryInfo) -> [LibraryGridChoice] {
        info.askerOptions.map { option in
            let name: String
            switch option.id {
            case "anyone": name = L10n.tr("library.filter.anyone")
            case "me": name = L10n.tr("library.filter.me")
            default: name = option.label
            }
            return LibraryGridChoice(tag: option.id, text: LibraryGridText.countedText(name, option.count))
        }
    }

    private func codecChoices(_ info: LibraryInfo) -> [LibraryGridChoice] {
        var choices = [LibraryGridChoice(tag: "", text: L10n.tr("library.filter.all"))]
        for option in info.codecOptions {
            choices.append(LibraryGridChoice(tag: option.id, text: LibraryGridText.countedText(LibraryText.codec(option.id), option.count)))
        }
        return choices
    }

    private func seriesChoices(_ info: LibraryInfo) -> [LibraryGridChoice] {
        info.seriesOptions.map { option in
            let name = option.id == "0" ? L10n.tr("library.filter.every.movie") : option.label
            return LibraryGridChoice(tag: option.id, text: LibraryGridText.countedText(name, option.count))
        }
    }

    private func selectKept(_ chosen: String, info: LibraryInfo) {
        let current = info.keptOptions.first(where: { $0.selected })?.id ?? ""
        if chosen == current { return }
        if chosen.isEmpty {
            store.toggleKept(current)
        } else {
            store.toggleKept(chosen)
        }
    }

    private func selectCodec(_ chosen: String, info: LibraryInfo) {
        let current = info.codecOptions.first(where: { $0.selected })?.id ?? ""
        if chosen == current { return }
        if chosen.isEmpty {
            store.setCodec(current)
        } else {
            store.setCodec(chosen)
        }
    }

    private func savedSection(_ info: LibraryInfo) -> some View {
        Section {
            ForEach(info.savedFilters) { saved in
                savedRow(saved)
            }
            Button(L10n.tr("library.saved.save"), systemImage: "star") {
                saveName = ""
                naming = true
            }
            .disabled(!info.canSaveFilter)
        } header: {
            Text(L10n.tr("library.saved.title"))
        }
    }

    private func savedRow(_ saved: SavedFilterInfo) -> some View {
        let deleteLabel = L10n.format("library.saved.delete", saved.name)
        return Button {
            store.applySavedFilter(saved.name)
        } label: {
            HStack {
                Image(systemName: "star.fill")
                    .foregroundStyle(Theme.textMuted)
                Text(verbatim: saved.name)
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                if saved.active {
                    Image(systemName: "checkmark")
                        .foregroundStyle(Theme.accentText)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(saved.active ? .isSelected : [])
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                store.deleteSavedFilter(saved.name)
            } label: {
                Image(systemName: "trash")
            }
            .accessibilityLabel(deleteLabel)
        }
        .contextMenu {
            Button(deleteLabel, systemImage: "trash", role: .destructive) {
                store.deleteSavedFilter(saved.name)
            }
        }
    }

    private func clearSection(_ info: LibraryInfo) -> some View {
        Section {
            Button(L10n.tr("library.clear.filters")) {
                clear()
            }
            .disabled(info.chips.isEmpty)
        }
    }
}
