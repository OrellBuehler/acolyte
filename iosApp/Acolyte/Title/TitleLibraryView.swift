import SwiftUI

struct TitleLibraryView: View {
    let info: TitleInfo
    let store: TitleStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.large) {
            if let error = info.ownedError {
                NoticeBanner(text: error.localized).padding(.horizontal, Theme.Spacing.screen)
            }
            if info.access.manage && !info.noDownloader.isEmpty {
                NoticeBanner(text: info.noDownloader == "usenet" ? L10n.tr("title.no.usenet") : L10n.tr("title.no.torrent"))
                    .padding(.horizontal, Theme.Spacing.screen)
            }
            StatusCardView(info: info, store: store)
            if info.isShow {
                ShowSeasonsView(info: info, store: store, seasons: store.seasons())
                if info.versions.count > 1 || info.access.manage { ShowVersionsView(info: info, store: store) }
            } else {
                MovieVersionsView(info: info, store: store)
            }
            if info.access.manage && info.heldCount > 0 {
                LibraryCardView {
                    Text(heldText).font(.callout)
                    Button(L10n.tr("title.restore")) { store.run { await $0.restoreHeld() } }
                        .disabled(info.busy == "restore")
                }
            }
            if !info.activeDownloads.isEmpty { downloads }
            HistoryCardView(info: info, store: store)
            if info.access.manage {
                TitleSettingsView(info: info, store: store)
                Button(role: .destructive) {
                    store.openRemove()
                } label: {
                    Text(info.isShow ? L10n.tr("title.delete.show") : L10n.tr("title.delete.movie"))
                }
                .padding(.horizontal, Theme.Spacing.screen)
            }
        }
        .padding(.top, Theme.Spacing.medium)
    }

    private var heldText: String {
        var text = L10n.plural("title.held.copies", info.heldCount)
        if let until = info.heldUntil { text += " · " + L10n.format("title.held.until", TitleText.date(until)) }
        return text
    }

    private var downloads: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("title.downloading")).padding(.horizontal, Theme.Spacing.screen)
            ForEach(info.activeDownloads) { download in
                LibraryCardView {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(download.label).font(.subheadline.weight(.semibold))
                            if !download.text.isEmpty { Text(download.text).font(.footnote).foregroundStyle(Theme.textMuted) }
                        }
                        Spacer()
                        StatusChip(text: "\(Int(min(max(download.progress, 0), 1) * 100))%", tone: .info)
                    }
                    ProgressView(value: min(max(download.progress, 0), 1)).tint(Theme.accent)
                    Button(L10n.tr("title.mark.failed"), role: .destructive) { store.confirm = .markFailed(download.id) }
                }
            }
        }
    }
}

struct LibraryCardView<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) { content() }
            .padding(Theme.Spacing.large)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
            .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct StatusCardView: View {
    let info: TitleInfo
    let store: TitleStore

    var body: some View {
        if let status = info.libraryStatus {
            LibraryCardView {
                HStack(spacing: Theme.Spacing.small) {
                    Circle().fill(Theme.dotColor(status.tone)).frame(width: 10, height: 10)
                    Text(status.kind == .have && !info.isShow && status.label == .inlibrary ? L10n.tr("title.in.your.library") : TitleText.library(status))
                        .font(.headline)
                }
                if !sub.isEmpty { Text(sub).font(.callout).foregroundStyle(Theme.textMuted) }
                if let percent = info.statusPercent { ProgressView(value: Double(percent) / 100).tint(Theme.accent) }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Theme.Spacing.xSmall) {
                        ForEach(chips, id: \.self) { StatusChip(text: $0, tone: .idle) }
                    }
                }
                if info.access.manage {
                    HStack(spacing: Theme.Spacing.small) {
                        switch info.statusButton {
                        case .tryagain:
                            Button(L10n.tr("title.try.again")) { store.run { await $0.searchNow() } }
                                .buttonStyle(.borderedProminent)
                                .tint(Theme.accent)
                                .disabled(info.busy == "search")
                        case .searchnow:
                            Button(L10n.tr("title.search.now")) { store.run { await $0.searchNow() } }
                                .buttonStyle(.borderedProminent)
                                .tint(Theme.accent)
                                .disabled(info.busy == "search")
                        case StatusButtonKind.none:
                            EmptyView()
                        }
                        Button(L10n.tr("title.choose.release")) { store.openReleases(.whole) }
                            .buttonStyle(.bordered)
                    }
                }
            }
        }
    }

    private var sub: String {
        switch info.statusSub {
        case .leftindownloader: return L10n.tr("title.sub.left.in.downloader")
        case .downloadinghandsoff: return L10n.tr("title.sub.downloading.hands.off")
        case .fullseasons:
            return [L10n.plural("title.sub.full.seasons", info.fullSeasons), TitleText.bytes(info.sizeBytes)].filter { !$0.isEmpty }.joined(separator: " · ")
        case .havemovie:
            let added = info.addedAt.map { L10n.format("title.sub.added", Format.relative($0)) } ?? ""
            return [info.ownBadges.joined(separator: " · "), TitleText.bytes(info.mainFile?.size ?? 0), added].filter { !$0.isEmpty }.joined(separator: " · ")
        case .onitsway: return L10n.tr("title.sub.on.its.way")
        case .lasttryfailed: return L10n.tr("title.sub.last.try.failed")
        case .waitsforcopy: return L10n.tr("title.sub.waits")
        case .notkept: return L10n.tr("title.sub.not.kept")
        case nil: return ""
        }
    }

    private var chips: [String] {
        var out: [String] = []
        if !info.folderName.isEmpty { out.append(info.folderName) }
        if info.sizeBytes > 0 { out.append(TitleText.bytes(info.sizeBytes)) }
        if !info.pickRulesName.isEmpty { out.append(info.pickRulesName) }
        out.append(info.keepUpToDate ? L10n.tr("title.kept") : L10n.tr("title.not.kept"))
        if !info.showStatus.isEmpty { out.append(info.showStatus.capitalizingFirst) }
        if !info.network.isEmpty { out.append(info.network) }
        if !info.originalLanguage.isEmpty { out.append(TitleText.language(info.originalLanguage)) }
        return out
    }
}

private struct MovieVersionsView: View {
    let info: TitleInfo
    let store: TitleStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(
                title: info.movieFileHeading ? L10n.tr("title.file") : L10n.tr("title.versions"),
                actionTitle: info.access.manage ? L10n.tr("title.add.version") : nil,
                action: info.access.manage ? {
                    store.run { await $0.loadChoices() }
                    store.open(.version(nil, 0))
                } : nil
            )
            .padding(.horizontal, Theme.Spacing.screen)
            if info.slotCards.isEmpty {
                Text(L10n.tr("title.no.file")).padding(.horizontal, Theme.Spacing.screen)
            }
            ForEach(info.slotCards) { card in
                SlotCardView(card: card, version: info.versions.first { $0.id == card.versionId }, info: info, store: store)
            }
        }
    }
}

private struct SlotCardView: View {
    let card: SlotCardInfo
    let version: TitleVersionInfo?
    let info: TitleInfo
    let store: TitleStore

    var body: some View {
        LibraryCardView {
            HStack {
                Text(card.main && (card.label.isEmpty || card.label == "Main") ? L10n.tr("title.main.copy") : card.label).font(.headline)
                Spacer()
                if info.access.manage && (!card.main || card.file != nil || info.legacy) { menu }
            }
            let badges = card.badges + (card.file?.isProtected == true ? [L10n.tr("title.protected")] : [])
            if !badges.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Theme.Spacing.xSmall) { ForEach(badges, id: \.self) { StatusChip(text: $0, tone: .idle) } }
                }
            }
            if let file = card.file {
                InfoLine(label: L10n.tr("title.quality"), value: file.resolutionLabel)
                InfoLine(label: L10n.tr("title.size"), value: TitleText.bytes(file.size))
                if let added = file.addedAt { InfoLine(label: L10n.tr("title.added"), value: TitleText.date(added)) }
                if !file.audioLanguages.isEmpty { InfoLine(label: L10n.tr("title.audio"), value: file.audioLanguages.prefix(6).joined(separator: " ")) }
                if !file.subtitleLanguages.isEmpty {
                    InfoLine(label: L10n.tr("title.subtitles"), value: file.subtitleLanguages.prefix(6).joined(separator: " "))
                }
                Text(file.fileName).font(.footnote).foregroundStyle(Theme.textMuted)
                if info.access.manage { PointsView(points: info.points[card.slotId]) { store.run { await $0.loadPoints(card.slotId) } } }
            } else if card.leftInDownloader {
                Text(L10n.tr("title.left.in.downloader")).font(.callout)
            } else {
                Text(L10n.format("title.no.file.yet", card.stateLabel)).font(.callout)
            }
        }
    }

    private var menu: some View {
        Menu {
            if let file = card.file {
                Button(L10n.tr("title.set.edition")) { store.open(.edition(file.id, file.edition)) }
                Button(file.isProtected ? L10n.tr("title.unprotect") : L10n.tr("title.protect")) {
                    if file.isProtected { store.confirm = .unprotect(file.id) } else { store.run { await $0.protect(file.id, on: true) } }
                }
                Button(L10n.tr("title.keep.and.add")) {
                    store.run { await $0.loadChoices() }
                    store.open(.version(nil, file.id))
                }
            } else if info.legacy, let version {
                Button(L10n.tr("title.adopt")) { store.open(.adopt(version.id)) }
            }
            if card.removable, let version {
                Button(L10n.format("title.remove.version", version.label), role: .destructive) { store.confirm = .removeVersion(version) }
            }
        } label: {
            Image(systemName: "ellipsis.circle").accessibilityLabel(L10n.format("title.version.actions", card.label))
        }
    }
}

struct PointsView: View {
    let points: PointsInfo?
    let load: () -> Void

    var body: some View {
        if let points {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                HStack {
                    Text(points.goodEnoughPoints.map { L10n.format("title.points.of", points.points, $0) } ?? L10n.format("title.points", points.points))
                        .font(.subheadline.weight(.semibold))
                    StatusChip(text: points.goodEnough ? L10n.tr("title.good.enough") : L10n.tr("title.below.good.enough"), tone: points.goodEnough ? .ok : .wait)
                }
                if !points.goodEnough {
                    if !points.stepReached {
                        Text(L10n.format("title.points.step", TitleText.step(points.step), TitleText.step(points.goodEnoughAt))).font(.footnote)
                    }
                    if let need = points.goodEnoughPoints, points.points < need {
                        Text(L10n.format("title.points.short", need - points.points)).font(.footnote)
                    }
                    if !points.meetsVersion { Text(L10n.tr("title.points.version")).font(.footnote) }
                }
                Text(points.traits.isEmpty ? L10n.tr("title.points.no.traits") : points.traits.joined(separator: " · "))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        } else {
            Button(L10n.tr("title.points.show"), action: load).font(.subheadline)
        }
    }
}

private struct ShowVersionsView: View {
    let info: TitleInfo
    let store: TitleStore
    @State private var all = false

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(
                title: L10n.tr("title.versions"),
                actionTitle: info.access.manage ? L10n.tr("title.add.version") : nil,
                action: info.access.manage ? {
                    store.run { await $0.loadChoices() }
                    store.open(.version(nil, 0))
                } : nil
            )
            .padding(.horizontal, Theme.Spacing.screen)
            ForEach(all ? info.versions : Array(info.versions.prefix(1))) { version in
                LibraryCardView {
                    Text(version.main ? L10n.format("title.version.main", version.label) : version.label).font(.headline)
                    let audio = version.audioLanguages.isEmpty ? L10n.tr("title.version.any") : version.audioLanguages.joined(separator: ", ")
                    Text(L10n.format("title.version.audio", audio)).font(.footnote)
                    Text(L10n.format("title.version.rules", version.pickRulesName) + (version.keepUpToDate ? "" : " · " + L10n.tr("title.not.kept")))
                        .font(.footnote)
                    if !version.folderPath.isEmpty { Text(version.folderPath).font(.footnote).foregroundStyle(Theme.textMuted) }
                    if info.access.manage {
                        HStack {
                            Button(L10n.tr("title.change")) {
                                store.run { await $0.loadChoices() }
                                store.open(.version(version, 0))
                            }
                            if !version.main {
                                Button(L10n.format("title.remove.version", version.label), role: .destructive) { store.confirm = .removeVersion(version) }
                            }
                        }
                        .buttonStyle(.borderless)
                    }
                }
            }
            if info.versions.count > 1 {
                Button(all ? L10n.tr("title.show.less") : L10n.format("title.see.all.count", info.versions.count)) { all.toggle() }
                    .padding(.horizontal, Theme.Spacing.screen)
            }
            if info.access.manage {
                Text(L10n.tr("title.versions.note")).font(.footnote).foregroundStyle(Theme.textMuted).padding(.horizontal, Theme.Spacing.screen)
            }
        }
    }
}

struct HistoryRowView: View {
    let row: HistoryRowInfo

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.small) {
            Circle().fill(Theme.dotColor(row.tone)).frame(width: 8, height: 8).padding(.top, 6)
            VStack(alignment: .leading, spacing: 2) {
                Text(row.summary).font(.callout).foregroundStyle(Theme.text)
                let meta = [row.episode, row.version, row.at.map { Format.relative($0) } ?? ""].filter { !$0.isEmpty }
                if !meta.isEmpty { Text(meta.joined(separator: " · ")).font(.footnote).foregroundStyle(Theme.textMuted) }
            }
        }
    }
}

private struct HistoryCardView: View {
    let info: TitleInfo
    let store: TitleStore
    @State private var all = false

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("title.history"))
            if info.history.isEmpty {
                Text(L10n.tr("title.history.empty")).font(.callout).foregroundStyle(Theme.textMuted)
            }
            ForEach(all ? info.history : Array(info.history.prefix(5))) { row in
                Button { store.open(.history) } label: { HistoryRowView(row: row) }.buttonStyle(.plain)
            }
            if info.history.count > 5 {
                Button(all ? L10n.tr("title.show.less") : L10n.format("title.show.all.count", info.history.count)) { all.toggle() }
            }
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

private struct TitleSettingsView: View {
    let info: TitleInfo
    let store: TitleStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            SectionHeader(title: L10n.tr("title.settings")).padding(.horizontal, Theme.Spacing.screen)
            LibraryCardView {
                Toggle(isOn: Binding(get: { info.keepUpToDate }, set: { _ in store.run { await $0.toggleKeep() } })) {
                    VStack(alignment: .leading) {
                        Text(L10n.tr("title.keep.up.to.date"))
                        Text(L10n.tr("title.keep.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
                .disabled(info.busy == "keep")
                .tint(Theme.accent)
                if info.gateNote {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L10n.tr("title.not.looking.yet")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.waitText)
                        Text(info.gateReason + (info.gateOpensAt.map { " · " + TitleText.date($0) } ?? "")).font(.footnote)
                    }
                }
                Divider()
                ChoiceMenu(
                    title: L10n.tr("title.quality"),
                    value: info.kindRules.first { $0.id == info.pickRulesId }?.name ?? info.pickRulesName,
                    options: orderedRules.map { ($0.id, $0.name) },
                    selected: info.pickRulesId
                ) { id in store.run { await $0.setRules(id) } }
                .disabled(!info.choicesLoaded || info.busy == "rules")
                if !info.isShow {
                    Button { store.open(.gate) } label: {
                        RowLabel(title: L10n.tr("title.release.gate"), value: gateValue)
                    }
                    .buttonStyle(.plain)
                }
                if info.isShow { showRows }
                if info.kindFolders.count > 1 {
                    ChoiceMenu(
                        title: L10n.tr("title.folder"),
                        value: info.kindFolders.first { $0.id == info.libraryFolderId }?.shown ?? info.folderName,
                        options: info.kindFolders.map { ($0.id, $0.shown) },
                        selected: info.libraryFolderId
                    ) { id in
                        if let folder = info.kindFolders.first(where: { $0.id == id }) { store.confirm = .move(folder) }
                    }
                }
                if info.access.subtitlesOn { SubtitlesRow(info: info, store: store) }
                if !info.access.handsOff {
                    Button { store.openLeftovers() } label: {
                        RowLabel(title: L10n.tr("title.rescan"), value: L10n.tr("title.rescan.hint"))
                    }
                    .buttonStyle(.plain)
                }
                if info.leftoverCount > 0 {
                    Button { store.openLeftovers() } label: {
                        RowLabel(title: L10n.tr("title.leftovers"), value: L10n.plural("title.leftover.count", info.leftoverCount))
                    }
                    .buttonStyle(.plain)
                }
                if info.renameCount > 0 {
                    Button { store.openRename() } label: {
                        RowLabel(title: L10n.tr("title.rename.files"), value: L10n.plural("title.rename.count", info.renameCount))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var orderedRules: [RulesInfo] {
        let presets = info.presetIds.compactMap { id in info.kindRules.first { $0.id == id } }
        return presets + info.kindRules.filter { !info.presetIds.contains($0.id) }
    }

    private var gateValue: String {
        TitleText.gate(info.releaseGate) + (info.gateDelayDays > 0 ? " · " + L10n.plural("title.gate.days", info.gateDelayDays) : "")
    }

    @ViewBuilder
    private var showRows: some View {
        let pick: String = switch info.seasonPick ?? SeasonPickKind.custom {
        case .all: L10n.tr("title.seasons.all")
        case .latest: L10n.tr("title.seasons.latest")
        case .off: L10n.tr("title.seasons.none")
        case .custom: L10n.tr("title.seasons.custom")
        }
        Menu {
            Button(L10n.tr("title.seasons.all")) { setSeasons(.all) }
            Button(L10n.tr("title.seasons.latest")) { setSeasons(.latest) }
            Button(L10n.tr("title.seasons.none")) { setSeasons(.off) }
        } label: {
            RowLabel(title: L10n.tr("title.seasons"), value: pick)
        }
        .disabled(info.busy == "seasons")
        Menu {
            Section {
                Button(L10n.tr("title.order.aired")) { setOrder("aired") }
                Button(L10n.tr("title.order.daily")) { setOrder("daily") }
                Button(L10n.tr("title.order.absolute")) { setOrder("absolute") }
            }
            if !info.groups.isEmpty {
                Section {
                    ForEach(info.groups) { group in
                        Button("\(group.name) · \(L10n.plural("title.seasons.count", group.seasons)) · \(L10n.plural("title.episodes.count", group.episodes))") {
                            let id = group.id
                            let seasons = store.seasons().driver
                            store.run { driver in
                                await driver.setEpisodeGroup(id)
                                await seasons.reset(grouped: true)
                            }
                        }
                    }
                }
            }
        } label: {
            RowLabel(title: L10n.tr("title.episode.order"), value: orderLabel)
        }
        .disabled(info.busy == "order")
        Menu {
            ForEach([0, 1, 2, 3, 5], id: \.self) { window in
                Button(TitleText.pack(window)) {
                    store.run { await $0.setPackWindow(window) }
                }
            }
        } label: {
            RowLabel(title: L10n.tr("title.season.packs"), value: TitleText.pack(info.packWindow), hint: L10n.tr("title.season.packs.hint"))
        }
        .disabled(info.busy == "packs")
    }

    private var orderLabel: String {
        if !info.episodeGroupId.isEmpty { return info.episodeGroupName.isEmpty ? L10n.tr("title.order.unavailable") : info.episodeGroupName }
        switch info.episodeOrder {
        case "daily": return L10n.tr("title.order.daily")
        case "absolute": return L10n.tr("title.order.absolute")
        default: return L10n.tr("title.order.aired")
        }
    }

    private func setSeasons(_ pick: SeasonPickKind) {
        let seasons = store.seasons().driver
        store.run { driver in
            await driver.setSeasons(pick)
            await seasons.reset(grouped: false)
        }
    }

    private func setOrder(_ order: String) {
        let seasons = store.seasons().driver
        store.run { driver in
            await driver.setOrder(order)
            await seasons.reset(grouped: false)
        }
    }
}

private struct SubtitlesRow: View {
    let info: TitleInfo
    let store: TitleStore
    @State private var subtitles: SubtitlesStore?

    var body: some View {
        let state = subtitles?.info
        let value: String = switch state?.mode {
        case .off: L10n.tr("title.subtitles.off")
        case .custom: state?.wishCodes ?? ""
        default: L10n.tr("title.subtitles.default") + ((state?.wishCodes ?? "").isEmpty ? "" : " · " + (state?.wishCodes ?? ""))
        }
        Menu {
            Button(L10n.tr("title.subtitles.default")) { subtitles?.run { await $0.useDefault() } }
            Button(L10n.tr("title.subtitles.off")) { subtitles?.run { await $0.turnOff() } }
            Button(L10n.tr("title.subtitles.choose")) { if let subtitles { store.open(.subtitles(subtitles)) } }
            Button(L10n.tr("title.subtitles.search")) { subtitles?.run { await $0.search() } }
        } label: {
            RowLabel(title: L10n.tr("title.subtitles"), value: value)
        }
        .task(id: info.ownedId) {
            let created = store.subtitles(itemId: 0)
            let driver = created.driver
            subtitles = created
            await withTaskGroup(of: Void.self) { group in
                group.addTask { await created.watch() }
                group.addTask { await driver.load() }
            }
        }
    }
}

struct RowLabel: View {
    let title: String
    let value: String
    var hint: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
            if !value.isEmpty { Text(value).font(.footnote).foregroundStyle(Theme.accentText) }
            if let hint { Text(hint).font(.footnote).foregroundStyle(Theme.textMuted).multilineTextAlignment(.leading) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, Theme.Spacing.xSmall)
        .contentShape(Rectangle())
    }
}

struct ChoiceMenu: View {
    let title: String
    let value: String
    let options: [(Int64, String)]
    let selected: Int64
    var hint: String?
    let pick: (Int64) -> Void

    var body: some View {
        Menu {
            ForEach(options, id: \.0) { option in
                Button {
                    if option.0 != selected { pick(option.0) }
                } label: {
                    if option.0 == selected { Label(option.1, systemImage: "checkmark") } else { Text(option.1) }
                }
            }
        } label: {
            RowLabel(title: title, value: value, hint: hint)
        }
    }
}
