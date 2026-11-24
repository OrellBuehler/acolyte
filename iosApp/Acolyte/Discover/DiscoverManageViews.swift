import SwiftUI

struct SeriesView: View {
    let id: String
    @State private var store: SeriesStore
    @Environment(\.dismiss) private var dismiss

    init(id: String) {
        self.id = id
        _store = State(initialValue: SeriesStore(id: id))
    }

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(systemImage: "film.stack")
        } content: {
            List {
                if let error = info.load.error {
                    NoticeBanner(text: error.localized).listRowSeparator(.hidden)
                }
                if let error = info.actionError {
                    NoticeBanner(text: error.localized).listRowSeparator(.hidden)
                }
                Section {
                    ForEach(info.movies) { movie in
                        SeriesRow(
                            movie: movie,
                            selected: info.selected.contains(movie.id),
                            enabled: info.canManage && info.results == nil,
                            toggle: { store.toggle(movie.id) }
                        )
                    }
                } footer: {
                    Text(L10n.tr("discover.series.note"))
                }
                if let results = info.results {
                    Section(L10n.tr("discover.series.done.title")) {
                        ForEach(results) { row in
                            Text(outcomeText(row))
                        }
                        Button(L10n.tr("discover.series.review.remaining")) { store.reload() }
                        Button(L10n.tr("discover.series.back")) { dismiss() }
                    }
                } else if info.canManage {
                    Section(L10n.tr("discover.series.review.title")) {
                        Toggle(L10n.tr("discover.series.delete.files"), isOn: Binding(get: { info.deleteFiles }, set: { store.setDeleteFiles($0) }))
                        Toggle(L10n.tr("discover.series.never.add"), isOn: Binding(get: { info.neverAdd }, set: { store.setNeverAdd($0) }))
                        if info.needsAcknowledge {
                            Toggle(L10n.tr("discover.series.acknowledge"), isOn: Binding(get: { info.acknowledge }, set: { store.setAcknowledge($0) }))
                        }
                        if !info.neverAdd {
                            Text(L10n.tr("discover.series.outside.note")).font(.footnote).foregroundStyle(Theme.textMuted)
                        }
                        Button(L10n.format("discover.series.review", info.selected.count)) { store.review() }
                            .disabled(!info.canReview)
                    }
                }
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(info.name.isEmpty ? L10n.tr("discover.series.title") : info.name)
        .sheet(isPresented: Binding(get: { info.review != nil }, set: { if !$0 { store.cancelReview() } })) {
            ReviewSheet(info: info, apply: { store.apply() }, cancel: { store.cancelReview() })
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private func outcomeText(_ row: SeriesOutcomeInfo) -> String {
        switch row.outcome {
        case .removed:
            L10n.format("discover.series.removed", row.name)
        case .skipped:
            L10n.format("discover.series.stayed", row.name, row.why.isEmpty ? L10n.tr("discover.series.why.skipped") : row.why)
        case .failed:
            L10n.format("discover.series.failed", row.name, row.why.isEmpty ? L10n.tr("discover.series.why.again") : row.why)
        }
    }
}

private func movieTitle(_ movie: SeriesMovieInfo) -> String {
    movie.year > 0 ? "\(movie.name) (\(movie.year))" : movie.name
}

private func movieCounts(_ movie: SeriesMovieInfo) -> String {
    L10n.plural("discover.series.versions", movie.versions) + " · " + L10n.plural("discover.series.files", movie.files)
}

private struct SeriesRow: View {
    let movie: SeriesMovieInfo
    let selected: Bool
    let enabled: Bool
    let toggle: () -> Void

    var body: some View {
        Button(action: toggle) {
            HStack(spacing: Theme.Spacing.medium) {
                if enabled {
                    Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(selected ? Theme.accent : Theme.textMuted)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(movieTitle(movie)).foregroundStyle(Theme.text)
                    Text(movieCounts(movie)).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                Spacer(minLength: 0)
                if movie.isProtected {
                    StatusChip(text: L10n.tr("discover.series.protected"), tone: .wait)
                }
            }
        }
        .disabled(!enabled)
        .buttonStyle(.plain)
    }
}

private struct ReviewSheet: View {
    let info: SeriesInfo
    let apply: () -> Void
    let cancel: () -> Void

    var body: some View {
        NavigationStack {
            List {
                ForEach(info.review ?? []) { row in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(movieTitle(row.movie)).font(.headline)
                        Text(movieCounts(row.movie)).font(.footnote).foregroundStyle(Theme.textMuted)
                        if !row.skip.isEmpty {
                            Text(L10n.format("discover.series.skip", row.skip)).font(.footnote).foregroundStyle(Theme.failText)
                        }
                        if info.showDetails {
                            Text(
                                L10n.format(
                                    "discover.series.details",
                                    row.movie.versionIds.map(String.init).joined(separator: ", "),
                                    row.movie.fileIds.map(String.init).joined(separator: ", ")
                                )
                            )
                            .font(.caption)
                            .foregroundStyle(Theme.textMuted)
                        }
                    }
                }
                Section {
                    Button(L10n.tr("discover.series.remove"), role: .destructive, action: apply)
                        .disabled(info.busy)
                }
            }
            .navigationTitle(L10n.tr("discover.series.confirm.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel"), action: cancel)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

struct HiddenView: View {
    @State private var store = HiddenStore()

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(title: L10n.tr("discover.hidden.empty"), systemImage: "eye.slash")
        } content: {
            List {
                if let error = store.info.load.error {
                    NoticeBanner(text: error.localized).listRowSeparator(.hidden)
                }
                if let error = store.info.actionError {
                    NoticeBanner(text: error.localized).listRowSeparator(.hidden)
                }
                ForEach(store.info.rows) { row in
                    HiddenRowView(row: row, showAgain: { store.showAgain(row.id) })
                }
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr("discover.hidden.title"))
        .task { await store.watch() }
        .task { await store.load() }
    }
}

struct HiddenRowView: View {
    let row: HiddenRowInfo
    let showAgain: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(row.isGenre ? DiscoverText.genre(row.name) : row.name).font(.headline).foregroundStyle(Theme.text)
                Text(DiscoverText.hiddenLine(row)).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            Spacer(minLength: 0)
            Button(L10n.tr("discover.hide.show.again"), action: showAgain).buttonStyle(.borderless)
        }
    }
}

struct HideSheetView: View {
    let target: HideTarget
    let done: @MainActor (HideDoneInfo) -> Void
    @State private var store: HideSheetStore
    @Environment(\.dismiss) private var dismiss

    init(target: HideTarget, done: @escaping @MainActor (HideDoneInfo) -> Void) {
        self.target = target
        self.done = done
        _store = State(initialValue: HideSheetStore(target: target))
    }

    var body: some View {
        let info = store.info
        NavigationStack {
            Form {
                Section {
                    Text(L10n.tr("discover.hide.body")).font(.callout).foregroundStyle(Theme.textMuted)
                }
                if !info.hidden.isEmpty {
                    Section(L10n.tr("discover.hide.now")) {
                        ForEach(info.hidden) { row in
                            HiddenRowView(row: row, showAgain: { store.showAgain(row.id) })
                        }
                    }
                }
                if !info.genres.isEmpty {
                    Picker(L10n.tr("discover.hide.what"), selection: Binding(get: { info.genre }, set: { store.pickGenre($0) })) {
                        Text(L10n.format("discover.hide.only", target.name)).tag("")
                        ForEach(info.genres, id: \.self) { genre in
                            Text(
                                target.kind == .show
                                    ? L10n.format("discover.hide.all.shows", DiscoverText.genre(genre))
                                    : L10n.format("discover.hide.all.movies", DiscoverText.genre(genre))
                            )
                            .tag(genre)
                        }
                    }
                }
                if !info.people.isEmpty {
                    Section(L10n.tr("discover.hide.for")) {
                        Picker(L10n.tr("discover.hide.for"), selection: Binding(get: { info.everyone }, set: { store.pickEveryone($0) })) {
                            Text(L10n.tr("discover.hide.everyone")).tag(true)
                            Text(L10n.tr("discover.hide.one.person")).tag(false)
                        }
                        .pickerStyle(.segmented)
                        if !info.everyone {
                            Picker(L10n.tr("discover.hide.person"), selection: Binding(get: { info.personId }, set: { store.pickPerson($0) })) {
                                Text(L10n.tr("discover.hide.pick.person")).tag(Int64(0))
                                ForEach(info.people) { Text($0.name).tag($0.id) }
                            }
                            if info.needsPerson {
                                Text(L10n.tr("discover.hide.pick.person")).font(.footnote).foregroundStyle(Theme.failText)
                            }
                        }
                    }
                }
                if let error = info.error ?? info.load.error {
                    Text(error.localized).foregroundStyle(Theme.failText)
                }
            }
            .navigationTitle(L10n.format("discover.hide.title", target.name))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("discover.hide.button")) { store.hide(then: done) }
                    }
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }
}

struct ExploreStatsView: View {
    @State private var store = ExploreStatsStore()

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(title: L10n.tr("discover.stats.empty"), systemImage: "chart.bar")
        } content: {
            List {
                Section {
                    ForEach(store.info.rows) { row in
                        StatRow(name: row.title.isEmpty ? rowName(row.rowType) : row.title, shown: row.shown, opens: row.opens, asks: row.asks, rate: nil)
                    }
                } header: {
                    Text(L10n.tr("discover.stats.row"))
                } footer: {
                    Text(L10n.tr("discover.stats.intro"))
                }
                if !store.info.layers.isEmpty {
                    Section(L10n.tr("discover.stats.titles")) {
                        ForEach(store.info.layers) { layer in
                            StatRow(
                                name: layerName(layer.layer),
                                shown: layer.shown,
                                opens: layer.opens,
                                asks: layer.asks,
                                rate: layer.rate < 0 ? "–" : String(format: "%.1f%%", layer.rate * 100)
                            )
                        }
                    }
                }
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr("discover.stats.title"))
        .task { await store.watch() }
        .task { await store.load() }
    }

    private func rowName(_ type: String) -> String {
        switch type {
        case "because_have": L10n.tr("discover.stats.because.have")
        case "because_asked": L10n.tr("discover.stats.because.asked")
        case "person": L10n.tr("discover.stats.person")
        case "genre": L10n.tr("discover.stats.genre")
        case "popular_genre": L10n.tr("discover.stats.popular.genre")
        case "popular_decade": L10n.tr("discover.stats.popular.decade")
        case "decade": L10n.tr("discover.stats.decade")
        case "mood": L10n.tr("discover.stats.mood")
        case "hero": L10n.tr("discover.stats.hero")
        default: type
        }
    }

    private func layerName(_ layer: String) -> String {
        switch layer {
        case "occasion": L10n.tr("discover.stats.layer.occasion")
        case "time": L10n.tr("discover.stats.layer.time")
        case "genre": L10n.tr("discover.stats.layer.genre")
        case "taste": L10n.tr("discover.stats.layer.taste")
        case "reason": L10n.tr("discover.stats.layer.reason")
        case "plain": L10n.tr("discover.stats.layer.plain")
        default: layer
        }
    }
}

private struct StatRow: View {
    let name: String
    let shown: Int
    let opens: Int
    let asks: Int
    let rate: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(name).foregroundStyle(Theme.text)
            HStack(spacing: Theme.Spacing.medium) {
                Text("\(L10n.tr("discover.stats.shown")) \(shown)")
                Text("\(L10n.tr("discover.stats.opens")) \(opens)")
                Text("\(L10n.tr("discover.stats.asks")) \(asks)")
                if let rate { Text("\(L10n.tr("discover.stats.rate")) \(rate)") }
            }
            .font(.footnote.monospacedDigit())
            .foregroundStyle(Theme.textMuted)
        }
    }
}
