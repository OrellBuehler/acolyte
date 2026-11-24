import SwiftUI

struct TVAskView: View {
    @State private var store: AskSheetStore
    @Environment(\.dismiss) private var dismiss

    init(kind: String, tmdb: String, fourK: Bool) {
        _store = State(initialValue: AskSheetStore(kind: kind, tmdb: tmdb, fourK: fourK))
    }

    private var info: AskSheetInfo { store.info }

    var body: some View {
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
        .padding(.horizontal, 20)
        .background(Theme.background.opacity(0.97).ignoresSafeArea())
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
        ScrollView {
            VStack(alignment: .leading, spacing: 36) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(L10n.format("requests.ask.title", RequestsWords.titleWithYear(info.name, info.year)))
                        .font(.title2.bold())
                        .foregroundStyle(Theme.text)
                        .lineLimit(2)
                    Text(hint)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .frame(maxWidth: Theme.TV.readableWidth, alignment: .leading)
                    if info.neverAdd {
                        Text(L10n.tr("requests.error.never.add")).font(.footnote).foregroundStyle(Theme.failText)
                    }
                }
                if info.isShow {
                    HStack(alignment: .top, spacing: Theme.TV.gutter) {
                        TVModeTile(
                            symbol: "square.stack.3d.up",
                            title: L10n.tr("requests.mode.all"),
                            hint: L10n.tr("requests.mode.all.hint"),
                            selected: info.mode == .all
                        ) { store.setMode(.all) }
                        TVModeTile(
                            symbol: "checklist",
                            title: L10n.tr("requests.mode.choose"),
                            hint: L10n.tr("requests.mode.choose.hint"),
                            selected: info.mode == .choose,
                            enabled: info.chooseAllowed
                        ) { store.setMode(.choose) }
                        TVModeTile(
                            symbol: "calendar.badge.clock",
                            title: L10n.tr("requests.mode.future"),
                            hint: L10n.tr("requests.mode.future.hint"),
                            selected: info.mode == .future
                        ) { store.setMode(.future) }
                    }
                    .focusSection()
                    if info.mode == .choose {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(info.seasons) { season in seasonRow(season) }
                        }
                        .focusSection()
                    }
                }
                if info.showsQuality {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(L10n.tr("requests.quality")).font(.headline).foregroundStyle(Theme.text)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 20) {
                                ForEach(info.pills) { pill in
                                    TVChip(title: RequestsWords.quality(pill.line, pill.name), selected: pill.key == info.selected) {
                                        store.select(pill.key)
                                    }
                                    .disabled(pill.disabled)
                                }
                            }
                            .padding(.vertical, Theme.TV.focusPad)
                        }
                        .scrollClipDisabled()
                        ForEach(info.alsoPills) { pill in
                            Toggle(
                                L10n.format("requests.also.ask", RequestsWords.quality(pill.line, pill.name)),
                                isOn: Binding(get: { info.also.contains(pill.key) }, set: { _ in store.toggleAlso(pill.key) })
                            )
                        }
                    }
                    .focusSection()
                }
                Toggle(isOn: Binding(get: { info.follow }, set: { store.setFollow($0) })) {
                    VStack(alignment: .leading) {
                        Text(L10n.tr("requests.follow"))
                        Text(L10n.tr("requests.follow.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
                if let error = info.error {
                    Text(RequestsWords.error(error)).font(.callout).foregroundStyle(Theme.failText)
                }
                HStack(spacing: 24) {
                    TVActionButton(title: L10n.tr("common.cancel")) { dismiss() }
                    TVActionButton(title: submitLabel, systemImage: "plus", prominent: true) { store.submit() }
                        .disabled(!info.canSubmit || info.busy)
                }
                .focusSection()
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

struct TVModeTile: View {
    let symbol: String
    let title: String
    let hint: String
    let selected: Bool
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: symbol)
                        .font(.system(size: 44))
                    Spacer(minLength: 0)
                    if selected {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 36))
                            .foregroundStyle(Theme.accentText)
                    }
                }
                Text(title)
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(enabled ? Theme.text : Theme.textMuted)
                Text(hint)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(3)
            }
            .padding(24)
            .frame(width: 420, height: 250, alignment: .topLeading)
        }
        .buttonStyle(.card)
        .disabled(!enabled)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
