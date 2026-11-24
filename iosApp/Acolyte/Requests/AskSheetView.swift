import SwiftUI

struct AskDoneView: View {
    let title: String
    let message: String
    let onDone: () -> Void

    var body: some View {
        VStack(spacing: Theme.Spacing.medium) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 48))
                .foregroundStyle(Theme.ok)
            Text(title)
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(message)
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            Button(L10n.tr("requests.done"), action: onDone)
                .buttonStyle(.borderedProminent)
        }
        .padding(Theme.Spacing.xLarge)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ChoiceRow: View {
    let title: String
    var hint: String?
    let selected: Bool
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top) {
                Image(systemName: selected ? "largecircle.fill.circle" : "circle")
                    .foregroundStyle(selected ? Theme.accentText : Theme.textMuted)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).foregroundStyle(enabled ? Theme.text : Theme.textMuted)
                    if let hint {
                        Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
                Spacer(minLength: 0)
            }
        }
        .disabled(!enabled)
    }
}

struct AskSheetView: View {
    @State private var store: AskSheetStore
    @Environment(\.dismiss) private var dismiss

    init(kind: String, tmdb: String, fourK: Bool) {
        _store = State(initialValue: AskSheetStore(kind: kind, tmdb: tmdb, fourK: fourK))
    }

    private var info: AskSheetInfo { store.info }

    var body: some View {
        NavigationStack {
            Group {
                if let outcome = info.outcome {
                    AskDoneView(
                        title: RequestsWords.outcomeTitle(outcome, doneName, fourK: info.outcomeFourK),
                        message: RequestsWords.outcomeMessage(outcome)
                    ) { dismiss() }
                } else {
                    switch info.load.phase {
                    case .loading: LoadingStateView()
                    case .failed: ErrorStateView(error: info.load.error ?? .unknown) { Task { await store.load() } }
                    case .ready: form
                    }
                }
            }
            .navigationTitle(L10n.format("requests.ask.title", RequestsWords.titleWithYear(info.name, info.year)))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                if info.outcome == nil, info.load.phase == .ready {
                    ToolbarItem(placement: .confirmationAction) {
                        if info.busy {
                            ProgressView()
                        } else {
                            Button(submitLabel) { store.submit() }
                                .disabled(!info.canSubmit)
                        }
                    }
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var doneName: String {
        guard info.isShow, info.mode != .all else { return info.name }
        return "\(info.name) (\(modeWords))"
    }

    private var modeWords: String {
        switch info.mode {
        case .future: L10n.tr("requests.seasons.future")
        case .all: L10n.tr("requests.seasons.whole")
        case .choose: chooseSummary
        }
    }

    private var chooseSummary: String {
        var parts: [String] = []
        if !info.pickedSeasons.isEmpty { parts.append(RequestsWords.seasons(info.pickedSeasons)) }
        if info.pickedEpisodes > 0 { parts.append(L10n.plural("requests.episodes", info.pickedEpisodes)) }
        return parts.joined(separator: " + ")
    }

    private var submitLabel: String {
        if !info.isShow { return L10n.format("requests.ask.for", info.name) }
        switch info.mode {
        case .all: return L10n.tr("requests.ask.all.seasons")
        case .future: return L10n.tr("requests.ask.future")
        case .choose:
            if info.pickedSeasons.isEmpty, info.pickedEpisodes == 0 { return L10n.tr("requests.pick.season") }
            return L10n.format("requests.ask.for", chooseSummary)
        }
    }

    private var hint: String {
        if let brief = info.brief, info.isShow {
            return L10n.format("requests.ask.already.show", RequestsWords.seasons(brief.seasons), brief.stateLabel)
        }
        if let brief = info.brief, info.joining {
            return L10n.format("requests.ask.already", brief.stateLabel)
        }
        return L10n.tr("requests.ask.hear")
    }

    private var form: some View {
        Form {
            Section {
                Text(hint)
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                if info.neverAdd {
                    Text(L10n.tr("requests.error.never.add")).foregroundStyle(Theme.failText)
                }
            }
            if info.isShow {
                Section(L10n.tr("requests.ask.what")) {
                    ChoiceRow(title: L10n.tr("requests.mode.all"), hint: L10n.tr("requests.mode.all.hint"), selected: info.mode == .all) {
                        store.setMode(.all)
                    }
                    ChoiceRow(
                        title: L10n.tr("requests.mode.choose"),
                        hint: L10n.tr("requests.mode.choose.hint"),
                        selected: info.mode == .choose,
                        enabled: info.chooseAllowed
                    ) { store.setMode(.choose) }
                    ChoiceRow(title: L10n.tr("requests.mode.future"), hint: L10n.tr("requests.mode.future.hint"), selected: info.mode == .future) {
                        store.setMode(.future)
                    }
                }
                if info.mode == .choose {
                    Section {
                        ForEach(info.seasons) { season in
                            seasonRow(season)
                        }
                    }
                }
            }
            if info.showsQuality {
                Section(L10n.tr("requests.quality")) {
                    ForEach(info.pills) { pill in
                        Button {
                            store.select(pill.key)
                        } label: {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(RequestsWords.quality(pill.line, pill.name))
                                        .foregroundStyle(pill.disabled ? Theme.textMuted : Theme.text)
                                    if pill.note != .plain {
                                        Text(L10n.tr(pill.note == .inLibrary ? "requests.pill.in.library" : "requests.pill.requested"))
                                            .font(.footnote)
                                            .foregroundStyle(Theme.textMuted)
                                    }
                                }
                                Spacer()
                                if pill.key == info.selected {
                                    Image(systemName: "checkmark").foregroundStyle(Theme.accentText)
                                }
                            }
                        }
                        .disabled(pill.disabled)
                    }
                    ForEach(info.alsoPills) { pill in
                        Toggle(
                            L10n.format("requests.also.ask", RequestsWords.quality(pill.line, pill.name)),
                            isOn: Binding(get: { info.also.contains(pill.key) }, set: { _ in store.toggleAlso(pill.key) })
                        )
                    }
                }
            }
            Section {
                Toggle(isOn: Binding(get: { info.follow }, set: { store.setFollow($0) })) {
                    VStack(alignment: .leading) {
                        Text(L10n.tr("requests.follow"))
                        Text(L10n.tr("requests.follow.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            } footer: {
                if let error = info.error {
                    Text(RequestsWords.error(error)).foregroundStyle(Theme.failText)
                }
            }
        }
    }

    private func seasonRow(_ season: AskSeasonInfo) -> some View {
        DisclosureGroup(isExpanded: Binding(get: { season.expanded }, set: { _ in store.expandSeason(season.number) })) {
            switch season.load {
            case .loading:
                Text(L10n.tr("requests.episodes.reading")).foregroundStyle(Theme.textMuted)
            case .failed:
                Text(L10n.tr("requests.episodes.failed")).foregroundStyle(Theme.failText)
            case .ready, .idle:
                if season.episodes.isEmpty {
                    Text(L10n.tr("requests.episodes.none")).foregroundStyle(Theme.textMuted)
                } else {
                    ForEach(season.episodes) { episode in
                        Toggle(
                            episode.name.isEmpty ? L10n.format("requests.episode", episode.number) : "\(episode.number). \(episode.name)",
                            isOn: Binding(get: { episode.ticked }, set: { _ in store.toggleEpisode(season.number, episode.number) })
                        )
                    }
                }
            }
        } label: {
            Toggle(isOn: Binding(get: { season.ticked || !season.available }, set: { _ in store.toggleSeason(season.number) })) {
                VStack(alignment: .leading) {
                    Text(season.name.isEmpty ? RequestsWords.seasonName(season.number) : season.name)
                    if let note = seasonNote(season) {
                        Text(note).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            }
            .disabled(!season.available)
        }
    }

    private func seasonNote(_ season: AskSeasonInfo) -> String? {
        if season.kept { return L10n.tr("requests.season.kept") }
        if season.complete { return L10n.tr("requests.season.in.library") }
        if season.picked > 0 { return L10n.format("requests.season.picked", season.picked) }
        return nil
    }
}

struct AskMusicView: View {
    @State private var store: AskMusicStore
    @Environment(\.dismiss) private var dismiss

    init(artist: String, name: String, album: String, edition: String) {
        _store = State(initialValue: AskMusicStore(artist: artist, name: name, album: album, edition: edition))
    }

    private var info: AskMusicInfo { store.info }

    var body: some View {
        NavigationStack {
            Group {
                if let outcome = info.outcome {
                    AskDoneView(title: RequestsWords.outcomeTitle(outcome, info.name, fourK: false), message: RequestsWords.outcomeMessage(outcome)) {
                        dismiss()
                    }
                } else {
                    switch info.load.phase {
                    case .loading: LoadingStateView()
                    case .failed: ErrorStateView(error: info.load.error ?? .unknown) { Task { await store.load() } }
                    case .ready: form
                    }
                }
            }
            .navigationTitle(L10n.format("requests.ask.title", info.name))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                if info.outcome == nil, info.load.phase == .ready {
                    ToolbarItem(placement: .confirmationAction) {
                        if info.busy {
                            ProgressView()
                        } else {
                            Button(submitLabel) { store.submit() }
                                .disabled(!info.canSubmit)
                        }
                    }
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var submitLabel: String {
        if info.mode == .every { return L10n.tr("requests.ask.every.album") }
        if info.picked.isEmpty { return L10n.tr("requests.pick.album") }
        return L10n.format("requests.ask.for", RequestsWords.names(info.picked))
    }

    private var form: some View {
        Form {
            Section {
                if let brief = info.brief {
                    let what = brief.albums.isEmpty ? L10n.tr("requests.albums.every") : RequestsWords.names(brief.albums)
                    Text(L10n.format("requests.ask.already.music", what, brief.stateLabel))
                        .font(.callout)
                        .foregroundStyle(Theme.textMuted)
                } else {
                    Text(L10n.tr("requests.ask.hear")).font(.callout).foregroundStyle(Theme.textMuted)
                }
            }
            Section(L10n.tr("requests.ask.what")) {
                ChoiceRow(
                    title: L10n.tr("requests.mode.every.album"),
                    hint: L10n.tr("requests.mode.every.album.hint"),
                    selected: info.mode == .every
                ) { store.setMode(.every) }
                ChoiceRow(
                    title: L10n.tr("requests.mode.some.albums"),
                    hint: L10n.tr("requests.mode.some.albums.hint"),
                    selected: info.mode == .some,
                    enabled: info.someAllowed
                ) { store.setMode(.some) }
            }
            if info.mode == .some {
                Section {
                    ForEach(info.albums) { album in
                        Toggle(isOn: Binding(get: { album.ticked || album.kept }, set: { _ in store.toggle(album.mbid) })) {
                            VStack(alignment: .leading) {
                                Text(album.name)
                                Text(albumLine(album)).font(.footnote).foregroundStyle(Theme.textMuted)
                            }
                        }
                        .disabled(!album.available)
                    }
                }
            }
            Section {
                Toggle(isOn: Binding(get: { info.follow }, set: { store.setFollow($0) })) {
                    VStack(alignment: .leading) {
                        Text(L10n.tr("requests.follow"))
                        Text(L10n.tr("requests.follow.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            } footer: {
                if let error = info.error {
                    Text(RequestsWords.error(error)).foregroundStyle(Theme.failText)
                }
            }
        }
    }

    private func albumLine(_ album: AskAlbumInfo) -> String {
        var parts: [String] = []
        if album.year > 0 { parts.append(String(album.year)) }
        parts.append(RequestsWords.albumKind(album.kind))
        if album.kept { parts.append(L10n.tr("requests.album.in.library")) }
        if album.leftOut { parts.append(L10n.tr("requests.album.left.out")) }
        return parts.joined(separator: " · ")
    }
}
