import SwiftUI

struct TVAskView: View {
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
                    done(outcome)
                } else {
                    switch info.load.phase {
                    case .loading: TVLoadingView()
                    case .failed: TVErrorView(error: info.load.error ?? .unknown) { Task { await store.load() } }
                    case .ready: form
                    }
                }
            }
            .navigationTitle(L10n.format("requests.ask.title", RequestsWords.titleWithYear(info.name, info.year)))
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private func done(_ outcome: AskOutcomeKind) -> some View {
        VStack(spacing: 28) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 96))
                .foregroundStyle(Theme.ok)
                .accessibilityHidden(true)
            Text(RequestsWords.outcomeTitle(outcome, doneName, fourK: info.outcomeFourK))
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(RequestsWords.outcomeMessage(outcome))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
                .frame(maxWidth: Theme.TV.readableWidth)
            Button(L10n.tr("requests.done")) { dismiss() }
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
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
                    TVChoiceRow(title: L10n.tr("requests.mode.all"), hint: L10n.tr("requests.mode.all.hint"), selected: info.mode == .all) {
                        store.setMode(.all)
                    }
                    TVChoiceRow(
                        title: L10n.tr("requests.mode.choose"),
                        hint: L10n.tr("requests.mode.choose.hint"),
                        selected: info.mode == .choose,
                        enabled: info.chooseAllowed
                    ) { store.setMode(.choose) }
                    TVChoiceRow(title: L10n.tr("requests.mode.future"), hint: L10n.tr("requests.mode.future.hint"), selected: info.mode == .future) {
                        store.setMode(.future)
                    }
                }
                if info.mode == .choose {
                    Section {
                        ForEach(info.seasons) { season in seasonRow(season) }
                    }
                }
            }
            if info.showsQuality {
                Section(L10n.tr("requests.quality")) {
                    ForEach(info.pills) { pill in
                        TVChoiceRow(
                            title: RequestsWords.quality(pill.line, pill.name),
                            hint: pill.note == .plain ? nil : L10n.tr(pill.note == .inLibrary ? "requests.pill.in.library" : "requests.pill.requested"),
                            selected: pill.key == info.selected,
                            enabled: !pill.disabled
                        ) { store.select(pill.key) }
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
            Section {
                Button(submitLabel) { store.submit() }
                    .disabled(!info.canSubmit || info.busy)
                Button(L10n.tr("common.cancel"), role: .cancel) { dismiss() }
            }
        }
    }

    private func seasonRow(_ season: AskSeasonInfo) -> some View {
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

    private func seasonNote(_ season: AskSeasonInfo) -> String? {
        if season.kept { return L10n.tr("requests.season.kept") }
        if season.complete { return L10n.tr("requests.season.in.library") }
        if season.picked > 0 { return L10n.format("requests.season.picked", season.picked) }
        return nil
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
}

struct TVChoiceRow: View {
    let title: String
    var hint: String?
    let selected: Bool
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 16) {
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
