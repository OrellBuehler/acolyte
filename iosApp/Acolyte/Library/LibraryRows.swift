import SwiftUI

enum LibraryGridText {
    static func stateText(_ state: LibStateInfo) -> String {
        switch state.key {
        case .failed:
            if state.count > 0 { return L10n.format("library.state.failed.n", state.count) }
            return reasons(state, none: L10n.tr("library.state.failed.none"))
        case .needsDecision:
            return reasons(state, none: L10n.tr("library.state.decision.none"))
        case .stalled:
            return L10n.format("library.state.stalled", max(state.percent, 0))
        case .missingFile:
            return L10n.tr("library.state.missing.file")
        case .missing:
            if state.count > 0 { return L10n.format("library.state.missing.n", state.count) }
            return L10n.tr("library.state.missing")
        case .betterWanted:
            if state.count > 0 { return L10n.format("library.state.better.n", state.count) }
            if !state.quality.isEmpty { return L10n.format("library.state.better.q", state.quality) }
            return L10n.tr("library.state.better")
        case .paused:
            return L10n.format("library.state.paused", max(state.percent, 0))
        case .downloading:
            if state.importing { return L10n.tr("library.state.importing") }
            if state.count > 0 { return L10n.format("library.state.downloading.n", state.count) }
            return L10n.format("library.state.downloading", max(state.percent, 0))
        case .queued:
            return L10n.tr("library.state.queued")
        case .approved:
            return L10n.tr("library.state.searching")
        case .waitingApproval:
            return L10n.tr("library.state.waiting")
        case .notOutYet:
            let day = LibraryText.day(state.date)
            if !day.isEmpty { return L10n.format("library.state.upcoming.on", day) }
            return L10n.tr("library.state.upcoming")
        case .notKept:
            if !state.quality.isEmpty { return L10n.format("library.state.not.kept.q", state.quality) }
            return L10n.tr("library.state.not.kept")
        case .specialsOnly:
            return L10n.format("library.state.specials", state.count)
        case .declined:
            return L10n.tr("library.state.declined")
        case .inLibrary:
            if !state.quality.isEmpty { return L10n.format("library.state.in.library.q", state.quality) }
            return L10n.tr("library.state.in.library")
        case .upToDate:
            return L10n.tr("library.state.complete")
        case .ended:
            return L10n.tr("library.state.ended")
        }
    }

    private static func reasons(_ state: LibStateInfo, none: String) -> String {
        var parts = state.reasons
        if state.fileNotFound { parts.append(L10n.tr("library.state.file.not.found")) }
        if parts.isEmpty { return none }
        return parts.joined(separator: "; ")
    }

    static func factText(_ fact: LibraryFactInfo) -> String {
        let day = LibraryText.day(fact.date)
        switch fact.kind {
        case .released: return L10n.format("library.fact.released", day)
        case .homeRelease: return L10n.format("library.fact.home", day)
        case .inCinemas: return L10n.format("library.fact.in.cinemas", day)
        case .cinemas: return L10n.format("library.fact.cinemas", day)
        case .releaseDate: return L10n.format("library.fact.release", day)
        case .notKnown: return L10n.tr("library.fact.not.known")
        case .episodes: return L10n.format("library.fact.episodes", Int(fact.number), Int(fact.total))
        case .tracks: return L10n.format("library.fact.tracks", Int(fact.number), Int(fact.total))
        case .nextEpisode: return L10n.format("library.fact.next", day)
        case .nextRelease: return L10n.format("library.fact.next.release", day)
        case .ended: return L10n.tr("library.fact.ended")
        case .lastAired: return L10n.format("library.fact.last", day)
        case .added: return L10n.format("library.fact.added", day)
        case .size: return LibraryText.bytes(fact.number)
        case .runtime:
            let minutes = Int(fact.number)
            return L10n.format("library.fact.runtime", minutes / 60, minutes % 60)
        case .score:
            let value = fact.score.formatted(.number.precision(.fractionLength(1)))
            return L10n.format("library.fact.score", value)
        case .year: return String(fact.number)
        case .network, .quality: return fact.text
        }
    }

    static func sortText(_ sort: LibrarySortID, kind: LibraryKindID) -> String {
        switch sort {
        case .status: return L10n.tr("library.sort.status")
        case .name: return L10n.tr("library.sort.name")
        case .year: return L10n.tr("library.sort.year")
        case .score: return L10n.tr("library.sort.score")
        case .release: return L10n.tr("library.sort.release")
        case .added: return L10n.tr("library.sort.added")
        case .size: return L10n.tr("library.sort.size")
        case .runtime: return L10n.tr("library.sort.runtime")
        case .rules: return L10n.tr("library.sort.rules")
        case .next:
            if kind == .artist { return L10n.tr("library.sort.next.release") }
            return L10n.tr("library.sort.next")
        case .last: return L10n.tr("library.sort.last")
        case .progress: return L10n.tr("library.sort.progress")
        case .network: return L10n.tr("library.sort.network")
        }
    }

    static func statusText(_ id: String) -> String {
        switch id {
        case "missing": return L10n.tr("library.filter.missing")
        case "not_out_yet": return L10n.tr("library.filter.upcoming")
        case "better": return L10n.tr("library.filter.better")
        case "have": return L10n.tr("library.filter.have")
        default: return L10n.tr("library.filter.all")
        }
    }

    static func chipText(_ chip: ActiveChipInfo) -> String {
        switch chip.filter {
        case .status:
            return statusText(chip.value)
        case .kept:
            if chip.value == "on" { return L10n.tr("library.chip.kept") }
            return L10n.tr("library.chip.not.kept")
        case .asker:
            if chip.value == "me" { return L10n.tr("library.chip.asked.me") }
            return L10n.format("library.chip.asked.by", chip.label)
        case .series:
            return L10n.format("library.chip.series", chip.label)
        case .codec:
            return LibraryText.codec(chip.value)
        case .subs:
            return L10n.tr("library.chip.subs")
        case .watched:
            return L10n.tr("library.chip.watched")
        case .search:
            return L10n.format("library.chip.search", chip.label)
        case .grouped:
            return L10n.tr("library.chip.grouped")
        }
    }

    static func countedText(_ text: String, _ count: Int) -> String {
        if count < 0 { return text }
        return "\(text) · \(count)"
    }

    static func watchedMark(_ item: LibraryItemInfo) -> LibraryGridWatched? {
        guard item.watchedOf > 0, item.watchedSeen > 0 else { return nil }
        if item.watchedSeen >= item.watchedOf {
            return LibraryGridWatched(text: L10n.tr("library.watched"), full: true)
        }
        return LibraryGridWatched(text: L10n.format("library.watched.part", item.watchedSeen, item.watchedOf), full: false)
    }
}

struct LibraryGridWatched: Hashable, Sendable {
    let text: String
    let full: Bool
}

struct LibraryGridWatchedMark: View {
    let watched: LibraryGridWatched
    var onArt = false

    var body: some View {
        Image(systemName: watched.full ? "checkmark.circle.fill" : "circle.lefthalf.filled")
            .font(.footnote)
            .foregroundStyle(onArt ? Theme.onArt : Theme.okText)
            .accessibilityLabel(watched.text)
    }
}

struct LibraryGridCheck: View {
    let selected: Bool

    var body: some View {
        Image(systemName: selected ? "checkmark.circle.fill" : "circle")
            .font(.title3)
            .foregroundStyle(selected ? Theme.accent : Theme.textFaint)
            .accessibilityHidden(true)
    }
}

struct LibraryGridRow: View {
    let item: LibraryItemInfo
    let selecting: Bool
    let selected: Bool

    var body: some View {
        HStack(alignment: .center, spacing: Theme.Spacing.medium) {
            if selecting {
                LibraryGridCheck(selected: selected)
            }
            LibraryPosterThumb(source: item.poster, name: item.name, width: 44)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(verbatim: item.name)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                if let state = item.state {
                    StatusChip(text: LibraryGridText.stateText(state), tone: state.tone)
                }
                if let fact = item.fact {
                    Text(LibraryGridText.factText(fact))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: Theme.Spacing.small)
            VStack(alignment: .trailing, spacing: Theme.Spacing.xSmall) {
                if item.year > 0 {
                    Text(verbatim: String(item.year))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .monospacedDigit()
                }
                if let watched = LibraryGridText.watchedMark(item) {
                    LibraryGridWatchedMark(watched: watched)
                }
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(selecting && selected ? .isSelected : [])
    }
}

struct LibraryGridCard: View {
    let item: LibraryItemInfo
    let width: CGFloat
    let showsState: Bool
    let selecting: Bool
    let selected: Bool

    private var progress: Double? {
        if item.kind == .show, item.total > 0 { return item.share }
        return nil
    }

    var body: some View {
        PosterCard(
            title: item.name,
            subtitle: item.year > 0 ? String(item.year) : nil,
            image: item.poster,
            width: width,
            progress: progress
        ) {
            if showsState, let state = item.state {
                StatusChip(text: LibraryGridText.stateText(state), tone: state.tone, onArt: true)
            }
        }
        .overlay(alignment: .top) {
            if selecting, selected {
                RoundedRectangle(cornerRadius: Theme.Radius.poster, style: .continuous)
                    .strokeBorder(Theme.accent, lineWidth: 3)
                    .frame(width: width, height: width * 1.5)
            }
        }
        .overlay(alignment: .topTrailing) {
            corner
        }
        .accessibilityAddTraits(selecting && selected ? .isSelected : [])
    }

    @ViewBuilder
    private var corner: some View {
        if selecting {
            Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                .font(.title3)
                .foregroundStyle(selected ? Theme.accent : Theme.onArt)
                .padding(4)
                .background(Theme.artScrim, in: Circle())
                .padding(Theme.Spacing.small)
                .accessibilityHidden(true)
        } else if let watched = LibraryGridText.watchedMark(item) {
            LibraryGridWatchedMark(watched: watched, onArt: true)
                .padding(5)
                .background(Theme.artScrim, in: Circle())
                .padding(Theme.Spacing.small)
        }
    }
}

struct LibraryGridItemLink<Content: View>: View {
    let store: LibraryStore
    let item: LibraryItemInfo
    let explain: @MainActor (LibraryItemInfo) -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        let info = store.info
        if info.selecting {
            Button {
                store.toggle(item.id)
            } label: {
                content()
            }
            .buttonStyle(.plain)
        } else if item.kind == .artist {
            NavigationLink(value: Route.musicLibraryArtist(titleId: item.id, tab: "")) {
                content()
            }
            .contextMenu {
                selectMenu(canManage: info.canManage)
            }
        } else if item.canOpen {
            NavigationLink(value: Route.title(kind: item.kind.rawValue, tmdb: item.tmdb)) {
                content()
            }
            .contextMenu {
                selectMenu(canManage: info.canManage)
            }
        } else {
            Button {
                explain(item)
            } label: {
                content()
            }
            .buttonStyle(.plain)
            .contextMenu {
                selectMenu(canManage: info.canManage)
            }
        }
    }

    @ViewBuilder
    private func selectMenu(canManage: Bool) -> some View {
        if canManage {
            Button(L10n.tr("library.select"), systemImage: "checkmark.circle") {
                store.startSelecting(with: item.id)
            }
        }
    }
}

struct LibraryGridBlock: View {
    let info: LibraryInfo
    let remove: @MainActor (ActiveChipInfo) -> Void
    let clear: @MainActor () -> Void
    let searchSubtitles: @MainActor () -> Void
    let searchMissing: @MainActor () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if let error = info.load.error {
                NoticeBanner(text: LibraryText.error(error))
            }
            if info.searchFailed {
                NoticeBanner(text: L10n.tr("library.search.failed"))
            }
            if !info.chips.isEmpty {
                chipRow
                actionRow
            }
        }
    }

    private var chipRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                ForEach(info.chips) { chip in
                    chipButton(chip)
                }
            }
        }
    }

    private func chipButton(_ chip: ActiveChipInfo) -> some View {
        let text = LibraryGridText.chipText(chip)
        return Button {
            remove(chip)
        } label: {
            HStack(spacing: 6) {
                Text(text)
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                Image(systemName: "xmark")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(Theme.textMuted)
            }
            .padding(.leading, 10)
            .padding(.trailing, 8)
            .frame(height: 28)
            .background(Theme.surfaceRaised, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L10n.format("library.chip.remove", text))
    }

    private var actionRow: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: Theme.Spacing.small) {
                actionButtons
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                actionButtons
            }
        }
        .buttonStyle(.bordered)
        .controlSize(.small)
    }

    @ViewBuilder
    private var actionButtons: some View {
        Button(L10n.tr("library.clear.filters")) {
            clear()
        }
        if info.canSearchSubtitles {
            Button(L10n.tr("library.search.subtitles"), systemImage: "captions.bubble") {
                searchSubtitles()
            }
            .disabled(info.working)
        }
        if info.canManage, info.statusFilter == .missing, info.missingCount > 0 {
            Button(missingTitle, systemImage: "magnifyingglass") {
                searchMissing()
            }
            .disabled(info.working)
        }
    }

    private var missingTitle: String {
        if info.kind == .show { return L10n.tr("library.search.missing.show") }
        return L10n.tr("library.search.missing.movie")
    }
}

struct LibraryGridGroupHeader: View {
    let group: LibraryGroupInfo

    private var title: Text {
        if group.id == 0 { return Text(L10n.tr("library.group.none")) }
        return Text(verbatim: group.name)
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            title
                .font(.headline)
                .foregroundStyle(Theme.text)
                .lineLimit(1)
            Spacer(minLength: Theme.Spacing.small)
            Text(LibraryText.plural("library.group.count", group.items.count))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        .textCase(nil)
        .accessibilityAddTraits(.isHeader)
    }
}

struct LibraryGridEmpty: View {
    let info: LibraryInfo

    var body: some View {
        ContentUnavailableView {
            Label(L10n.tr("library.empty.title"), systemImage: info.kind == .show ? "tv" : "film")
        } description: {
            Text(L10n.tr("library.empty.message"))
        } actions: {
            ForEach(info.addKinds, id: \.self) { kind in
                addButton(kind)
            }
            if info.canOpenFolders {
                NavigationLink(value: Route.settingsLibraryFolders) {
                    Text(L10n.tr("library.empty.folders"))
                }
                .buttonStyle(.bordered)
            }
        }
    }

    @ViewBuilder
    private func addButton(_ kind: LibraryKindID) -> some View {
        switch kind {
        case .movie:
            NavigationLink(value: SectionID.search) {
                Text(L10n.tr("library.add.movie"))
            }
            .buttonStyle(.borderedProminent)
        case .show:
            NavigationLink(value: SectionID.search) {
                Text(L10n.tr("library.add.show"))
            }
            .buttonStyle(.borderedProminent)
        case .artist:
            EmptyView()
        }
    }
}

struct LibraryGridNothing: View {
    let info: LibraryInfo
    let clear: @MainActor () -> Void

    private var title: String {
        let text = info.query.trimmingCharacters(in: .whitespacesAndNewlines)
        if !text.isEmpty, info.activeFilterCount == 0 { return L10n.format("library.nothing.for", text) }
        return L10n.tr("library.nothing.matches")
    }

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: "magnifyingglass")
        } actions: {
            Button(L10n.tr("library.clear.filters")) {
                clear()
            }
            .buttonStyle(.bordered)
        }
    }
}
