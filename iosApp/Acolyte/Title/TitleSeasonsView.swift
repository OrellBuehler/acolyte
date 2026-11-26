import SwiftUI

struct ShowSeasonsView: View {
    let info: TitleInfo
    let store: TitleStore
    let seasons: ShowSeasonsStore
    @State private var outsideOpen = false

    var body: some View {
        let state = seasons.info
        let ordered = orderedSeasons
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("title.seasons")).padding(.horizontal, Theme.Spacing.screen)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.small) {
                    if !info.episodeGroupName.isEmpty { StatusChip(text: info.episodeGroupName, tone: .idle) }
                    if info.getAllMissing {
                        Button(L10n.tr("title.get.all.missing")) { store.run { await $0.searchMissing() } }
                            .buttonStyle(.bordered)
                            .disabled(info.busy == "missing")
                    }
                    if info.access.manage && (state.belowCount > 0 || state.belowOnly) {
                        Toggle(L10n.format("title.below.count", state.belowCount), isOn: Binding(get: { state.belowOnly }, set: { _ in seasons.driver.toggleBelow() }))
                            .toggleStyle(.button)
                    }
                    let allOpen = !ordered.isEmpty && state.open.count >= ordered.count
                    Button(allOpen ? L10n.tr("title.collapse.all") : L10n.tr("title.expand.all")) {
                        if allOpen {
                            seasons.driver.collapseAll()
                        } else {
                            let numbers = ordered.map(\.number)
                            seasons.run { await $0.expandAll(numbers) }
                        }
                    }
                }
                .padding(.horizontal, Theme.Spacing.screen)
            }
            if info.showOffHint { NoticeBanner(text: L10n.tr("title.turn.show.on")).padding(.horizontal, Theme.Spacing.screen) }
            ForEach(ordered) { season in
                SeasonRowView(season: season, info: info, store: store, seasons: seasons)
                if state.open.contains(season.number) {
                    SeasonEpisodesView(season: season.number, info: info, store: store, seasons: seasons)
                }
                Divider().padding(.horizontal, Theme.Spacing.screen)
            }
            if !state.outside.isEmpty {
                Button(L10n.format("title.not.in.order", state.outside.count)) { outsideOpen.toggle() }
                    .padding(.horizontal, Theme.Spacing.screen)
                if outsideOpen {
                    Text(L10n.tr("title.not.in.order.hint")).font(.footnote).padding(.horizontal, Theme.Spacing.screen)
                    ForEach(state.outside) { EpisodeRowView(episode: $0, info: info, store: store, seasons: seasons, finale: false) }
                }
            }
        }
        .task { await seasons.watch() }
        .task(id: info.episodeGroupId) { if !info.episodeGroupId.isEmpty { await seasons.driver.loadOutside() } }
        .overlay { FeedbackToast(feedback: state.feedback) }
    }

    private var orderedSeasons: [OwnedSeasonInfo] {
        info.ownedSeasons.filter { $0.number > 0 }.sorted { $0.number > $1.number } + info.ownedSeasons.filter { $0.number <= 0 }
    }
}

private struct SeasonRowView: View {
    let season: OwnedSeasonInfo
    let info: TitleInfo
    let store: TitleStore
    let seasons: ShowSeasonsStore

    var body: some View {
        let open = seasons.info.open.contains(season.number)
        HStack(spacing: Theme.Spacing.small) {
            if info.access.manage {
                Button {
                    let number = season.number
                    let keep = !season.keepUpToDate
                    seasons.run { await $0.keepSeason(number, keep: keep) }
                } label: {
                    Image(systemName: season.keepUpToDate ? "checkmark.square.fill" : "square")
                        .foregroundStyle(season.keepUpToDate ? Theme.accent : Theme.textMuted)
                }
                .buttonStyle(.plain)
                .disabled(seasons.info.busy == "season-\(season.number)")
                .accessibilityLabel(TitleText.season(season.number))
            }
            Button {
                let number = season.number
                seasons.run { await $0.toggle(number) }
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(season.name.isEmpty ? TitleText.season(season.number) : season.name).font(.subheadline.weight(.semibold))
                        if !notes.isEmpty { Text(notes).font(.footnote).foregroundStyle(Theme.textMuted) }
                    }
                    Spacer()
                    StatusChip(text: "\(season.inLibrary) / \(season.episodes)", tone: tone)
                    Image(systemName: open ? "chevron.up" : "chevron.down").foregroundStyle(Theme.textMuted)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if info.access.manage {
                Menu {
                    Button(L10n.format("title.get.missing.season", season.number)) {
                        let number = season.number
                        store.run { await $0.searchSeason(number) }
                    }
                    .disabled(season.missing == 0)
                    Button(L10n.format("title.choose.release.season", season.number)) { store.openReleases(.season, season: season.number) }
                    Button(L10n.format("title.season.tools", season.number)) { store.open(.seasonTools(season.number)) }
                } label: {
                    Image(systemName: "ellipsis").frame(width: 32, height: 32)
                }
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var tone: ToneKind {
        if season.episodes > 0 && season.inLibrary >= season.episodes { return .ok }
        if season.inLibrary > 0 { return .info }
        if season.missing > 0 { return .wait }
        return .idle
    }

    private var notes: String {
        var out: [String] = []
        if season.downloading > 0 { out.append(L10n.format("title.season.downloading", season.downloading)) }
        if season.failed > 0 { out.append(L10n.format("title.season.failed.count", season.failed)) }
        if !season.nextAir.isEmpty { out.append(L10n.format("title.season.next", TitleText.air(season.nextAir))) }
        return out.joined(separator: " · ")
    }
}

private struct SeasonEpisodesView: View {
    let season: Int
    let info: TitleInfo
    let store: TitleStore
    let seasons: ShowSeasonsStore

    var body: some View {
        let loaded = seasons.info.seasons[season]
        if loaded == nil || loaded?.loading == true {
            ProgressView().padding(Theme.Spacing.screen)
        } else if loaded?.failed == true {
            HStack {
                Text(L10n.tr("title.season.load.failed")).font(.callout)
                Spacer()
                Button(L10n.tr("common.retry")) {
                    let number = season
                    seasons.run { await $0.load(number) }
                }
            }
            .padding(.horizontal, Theme.Spacing.screen)
        } else {
            let list = seasons.info.shown[season] ?? []
            let last = list.map(\.episode).max() ?? 0
            ForEach(list) { episode in
                EpisodeRowView(episode: episode, info: info, store: store, seasons: seasons, finale: list.count > 1 && episode.episode == last)
            }
        }
    }
}

struct EpisodeRowView: View {
    let episode: EpisodeInfo
    let info: TitleInfo
    let store: TitleStore
    let seasons: ShowSeasonsStore
    let finale: Bool

    var body: some View {
        HStack(spacing: Theme.Spacing.small) {
            if info.access.manage {
                Button {
                    let id = episode.id
                    let keep = !episode.keepUpToDate
                    seasons.run { await $0.keepEpisode(id, keep: keep) }
                } label: {
                    Image(systemName: episode.keepUpToDate ? "checkmark.square.fill" : "square")
                        .foregroundStyle(episode.keepUpToDate ? Theme.accent : Theme.textMuted)
                }
                .buttonStyle(.plain)
                .disabled(seasons.info.busy == "episode-\(episode.id)")
                .accessibilityLabel(TitleText.episodeCode(episode.season, episode.episode))
            }
            Button { store.openEpisode(episode.id) } label: {
                HStack(spacing: Theme.Spacing.small) {
                    AuthImage(source: episode.still, size: .backdrop) { ArtPlaceholderView(name: String(episode.episode)) }
                        .frame(width: 84, height: 47)
                        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 4) {
                            Text("\(episode.episode). " + (episode.name.isEmpty ? L10n.format("title.episode.number", episode.episode) : episode.name))
                                .font(.subheadline.weight(.semibold))
                                .lineLimit(1)
                            if finale { Text(L10n.tr("title.finale")).font(.caption.weight(.semibold)).foregroundStyle(Theme.accentText) }
                        }
                        if !line.isEmpty { Text(line).font(.footnote).foregroundStyle(Theme.textMuted).lineLimit(1) }
                        if info.access.manage && episode.slotState == "chosen" && !episode.reason.isEmpty {
                            Text(episode.reason).font(.footnote).foregroundStyle(Theme.waitText).lineLimit(2)
                        }
                        HStack(spacing: 6) {
                            Circle().fill(Theme.dotColor(tone)).frame(width: 7, height: 7)
                            Text(status).font(.footnote.weight(.medium))
                        }
                    }
                    Spacer(minLength: 0)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if info.access.manage, let slotId = episode.slotId {
                Menu {
                    Button(L10n.tr("title.look.for.episode")) {
                        let code = TitleText.episodeCode(episode.season, episode.episode)
                        store.run { await $0.searchSlot(slotId, code: code) }
                    }
                    .disabled(episode.tone == .downloading)
                    Button(L10n.tr("title.choose.release.episode")) {
                        store.openReleases(.episode, season: episode.season, episode: episode.episode, itemId: episode.id, slotId: slotId)
                    }
                } label: {
                    Image(systemName: "ellipsis").frame(width: 32, height: 32)
                }
            }
        }
        .opacity(episode.notAired && episode.file == nil ? 0.6 : 1)
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, 2)
    }

    private var tone: ToneKind {
        switch episode.tone {
        case .have: .ok
        case .downloading: .info
        case .failed: .fail
        case .missing: .idle
        }
    }

    private var line: String {
        var out: [String] = []
        let air = TitleText.air(episode.airDay)
        if !air.isEmpty { out.append(air) }
        if episode.minutes > 0 { out.append(TitleText.runtime(episode.minutes)) }
        if let file = episode.file { out.append(TitleText.bytes(file.size)) }
        if info.access.manage, let points = episode.points { out.append(L10n.format("title.points.inline", points, episode.goodEnoughPoints ?? 0)) }
        return out.joined(separator: " · ")
    }

    private var status: String {
        if let file = episode.file { return file.epQuality.isEmpty ? L10n.tr("title.state.in.library") : file.epQuality }
        if episode.notAired { return L10n.tr("title.not.out.yet") }
        return episode.stateLabel.isEmpty ? L10n.tr("title.missing") : episode.stateLabel
    }
}

struct EpisodeSheetView: View {
    let store: EpisodeStore
    let title: TitleStore

    var body: some View {
        Group {
            if let state = store.info {
                content(state)
            } else {
                ProgressView()
            }
        }
        .task { await store.watch() }
        .task { await store.driver.load() }
    }

    private func content(_ state: EpisodeSheetInfo) -> some View {
        let e = state.episode
        return List {
            Section {
                Color.clear
                    .aspectRatio(16 / 9, contentMode: .fit)
                    .overlay { AuthImage(source: e.still, size: .backdrop) { ArtPlaceholderView(name: e.name.isEmpty ? state.code : e.name) } }
                    .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
                    .listRowInsets(EdgeInsets())
                Text(e.name.isEmpty ? L10n.format("title.episode.number", e.episode) : e.name).font(.headline)
                let meta = [TitleText.air(e.airDay), TitleText.runtime(e.minutes)].filter { !$0.isEmpty }
                if !meta.isEmpty { Text(meta.joined(separator: " · ")).font(.footnote).foregroundStyle(Theme.textMuted) }
                if e.file != nil { StatusChip(text: L10n.tr("title.state.in.library"), tone: .ok) }
                if !state.synopsis.isEmpty { Text(state.synopsis).font(.callout) }
            }
            if e.tone != .have {
                Section {
                    Text(e.stateLabel.isEmpty ? L10n.tr("title.missing") : e.stateLabel).font(.subheadline.weight(.semibold))
                    if state.manage && !state.chosenWhy.isEmpty { Text(state.chosenWhy).font(.footnote) }
                    if state.manage && !state.downloading && e.slotId != nil {
                        Button(L10n.tr("title.look.for.it")) { store.run { await $0.lookFor() } }.disabled(state.busy)
                    }
                }
            }
            if let file = e.file {
                Section {
                    InfoLine(label: L10n.tr("title.quality"), value: file.epQuality.isEmpty ? file.resolutionLabel : file.epQuality)
                    InfoLine(label: L10n.tr("title.size"), value: TitleText.bytes(file.size))
                    if let added = file.addedAt { InfoLine(label: L10n.tr("title.added"), value: TitleText.date(added)) }
                    if !file.audioLanguages.isEmpty { InfoLine(label: L10n.tr("title.audio"), value: file.audioLanguages.prefix(8).joined(separator: " ")) }
                    if !file.subtitleLanguages.isEmpty {
                        InfoLine(label: L10n.tr("title.subtitles"), value: file.subtitleLanguages.prefix(8).joined(separator: " "))
                    }
                    Text(file.fileName).font(.footnote).foregroundStyle(Theme.textMuted)
                    if state.manage { PointsView(points: state.points) {} }
                }
            }
            if state.manage {
                Section {
                    Toggle(isOn: Binding(get: { e.keepUpToDate }, set: { value in store.run { await $0.setKeep(value) } })) {
                        VStack(alignment: .leading) {
                            Text(L10n.tr("title.keep.up.to.date"))
                            Text(L10n.tr("title.better.copies")).font(.footnote).foregroundStyle(Theme.textMuted)
                        }
                    }
                    .tint(Theme.accent)
                    .disabled(state.busy)
                    if state.subtitlesOn {
                        Button(L10n.tr("title.subtitles.choose")) { title.open(.subtitles(title.subtitles(itemId: e.id))) }
                    }
                    Button(L10n.tr("title.delete.episode"), role: .destructive) { title.openRemove(episodeIds: [e.id]) }
                }
                Section(L10n.tr("title.history")) {
                    if state.eventsLoaded && state.events.isEmpty { Text(L10n.tr("title.history.nothing.yet")).font(.callout) }
                    ForEach(state.events) { HistoryRowView(row: $0) }
                }
            }
        }
        .navigationTitle(L10n.format("title.episode.title", e.season, e.episode))
        .navigationBarTitleDisplayMode(.inline)
        .overlay { FeedbackToast(feedback: state.feedback) }
    }
}

struct SeasonToolsView: View {
    let season: Int
    let store: TitleStore
    @State private var tab = 0
    @State private var rename: RenameStore?

    var body: some View {
        let seasons = store.seasons()
        let state = seasons.info
        List {
            Picker(L10n.tr("title.tools.files"), selection: $tab) {
                Text(L10n.tr("title.tools.files")).tag(0)
                Text(L10n.tr("title.tools.rename")).tag(1)
                Text(L10n.tr("title.history")).tag(2)
            }
            .pickerStyle(.segmented)
            switch tab {
            case 0:
                let all = state.seasons[season]?.episodes ?? []
                let files = all.filter { $0.file != nil }
                Text(L10n.format("title.tools.summary", state.filesCount[season] ?? 0, all.count, TitleText.bytes(state.filesSize[season] ?? 0)))
                    .font(.callout)
                if store.info.packWindow > 0 {
                    Text(L10n.format("title.tools.pack.note", store.info.packWindow)).font(.footnote)
                    Button(L10n.format("title.tools.pack.next", store.info.packWindow)) {
                        let number = season
                        seasons.run { await $0.packNext(number) }
                    }
                    .disabled(state.packBusy)
                }
                if files.isEmpty { Text(L10n.tr("title.tools.no.files")).font(.callout) }
                ForEach(files) { e in
                    if let file = e.file {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(e.episode). \(file.fileName)").font(.callout).lineLimit(2)
                            Text([file.epQuality, TitleText.bytes(file.size), file.group, file.audioLanguages.prefix(4).joined(separator: " ")]
                                .filter { !$0.isEmpty }
                                .joined(separator: " · "))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                    }
                }
            case 1:
                if let rename { RenameSection(store: rename) }
            default:
                if !state.historyLoaded { ProgressView() }
                let rows = state.seasonHistory[season] ?? []
                if state.historyLoaded && rows.isEmpty { Text(L10n.tr("title.tools.history.empty")).font(.callout) }
                ForEach(rows) { HistoryRowView(row: $0) }
            }
            Button(L10n.format("title.delete.season", TitleText.season(season)), role: .destructive) {
                store.openRemove(season: season)
            }
        }
        .navigationTitle(TitleText.season(season))
        .navigationBarTitleDisplayMode(.inline)
        .task { await seasons.watch() }
        .task {
            let number = season
            await seasons.driver.ensure(number)
        }
        .task(id: tab) {
            if tab == 2 && !seasons.info.historyLoaded { await seasons.driver.loadHistory() }
            if tab == 1 && rename == nil { rename = store.rename(season: season) }
        }
    }
}
