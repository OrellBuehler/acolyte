import SwiftUI

struct ReleasesView: View {
    let store: ReleasesStore
    let title: TitleStore

    var body: some View {
        Group {
            if let info = store.info {
                content(info)
            } else {
                ProgressView()
            }
        }
        .task { await store.watch() }
        .task { await store.driver.load() }
        .task { await store.driver.poll() }
        .onChange(of: store.info?.done ?? false) { _, done in
            guard done, let info = store.info else { return }
            if info.pickedId != nil {
                title.openAdd(after: store)
            } else {
                title.sheet = nil
            }
        }
    }

    private func content(_ info: ReleasesInfo) -> some View {
        List {
            Section {
                if info.check && !info.rules.isEmpty {
                    Picker(L10n.tr("title.pick.rules"), selection: Binding(get: { info.rulesId }, set: { store.driver.setRules($0) })) {
                        ForEach(info.rules) { Text($0.name).tag($0.id) }
                    }
                }
                if info.wholeShow { Text(L10n.tr("title.releases.whole.show")).font(.footnote) }
                Picker(L10n.tr("title.releases.good"), selection: Binding(get: { info.allTab }, set: { store.driver.setAllTab($0) })) {
                    Text(L10n.tr("title.releases.good")).tag(false)
                    Text(L10n.format("title.releases.all", info.total)).tag(true)
                }
                .pickerStyle(.segmented)
                if !info.wholeShow {
                    Button {
                        store.run { await $0.search() }
                    } label: {
                        HStack {
                            if info.running { ProgressView() }
                            Text(info.running ? L10n.tr("title.searching") : (info.total > 0 ? L10n.tr("title.search.again") : L10n.tr("title.search.now")))
                        }
                    }
                    .disabled(info.running || (info.check && info.rulesId == 0))
                }
                if let error = info.load.error { NoticeBanner(text: error.localized) }
                if info.rulesChanged { NoticeBanner(text: L10n.tr("title.releases.rules.changed")) }
                if info.running { Text(L10n.tr("title.releases.asking")).font(.footnote) }
            }
            if info.allCount > 0 { tools(info) }
            Section {
                if info.load.phase == .loading {
                    ProgressView()
                } else if info.empty != ReleaseEmptyKind.none {
                    emptyView(info)
                } else {
                    ForEach(info.shown) { ReleaseRow(release: $0, info: info, store: store) }
                }
                if info.canShowMore {
                    Button(L10n.format("title.releases.more", info.allCount, info.total)) { store.run { await $0.showMore() } }
                }
            }
        }
        .navigationTitle(info.check ? L10n.tr("title.check.releases") : L10n.tr("title.choose.release"))
        .navigationBarTitleDisplayMode(.inline)
        .overlay { FeedbackToast(feedback: info.feedback) }
    }

    private func tools(_ info: ReleasesInfo) -> some View {
        Section {
            Picker(L10n.tr("title.sort"), selection: Binding(get: { info.sort }, set: { store.driver.setSort($0) })) {
                Text(L10n.tr("title.sort.best")).tag(ReleaseSortKind.best)
                Text(L10n.tr("title.sort.points")).tag(ReleaseSortKind.points)
                Text(L10n.tr("title.sort.quality")).tag(ReleaseSortKind.quality)
                Text(L10n.tr("title.sort.size")).tag(ReleaseSortKind.size)
                Text(L10n.tr("title.sort.seeders")).tag(ReleaseSortKind.seeders)
                Text(L10n.tr("title.sort.age")).tag(ReleaseSortKind.age)
            }
            if info.resolutions.count > 1 {
                Picker(L10n.tr("title.quality"), selection: Binding(get: { info.resolution }, set: { store.driver.setResolution($0) })) {
                    Text(L10n.tr("title.any")).tag(0)
                    ForEach(info.resolutions, id: \.self) { Text($0 >= 2160 ? "4K" : "\($0)p").tag($0) }
                }
            }
            if info.sources.count > 1 {
                Picker(L10n.tr("title.source"), selection: Binding(get: { info.source }, set: { store.driver.setSource($0) })) {
                    Text(L10n.tr("title.all.sources")).tag("")
                    ForEach(info.sources, id: \.self) { Text($0).tag($0) }
                }
            }
            if !info.languages.isEmpty {
                Picker(L10n.tr("title.language"), selection: Binding(get: { info.language }, set: { store.driver.setLanguage($0) })) {
                    Text(L10n.tr("title.all.languages")).tag("")
                    ForEach(info.languages, id: \.self) { Text($0.uppercased()).tag($0) }
                }
            }
            TextField(L10n.tr("title.filter.name"), text: Binding(get: { info.query }, set: { store.driver.setQuery($0) }))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        }
    }

    @ViewBuilder
    private func emptyView(_ info: ReleasesInfo) -> some View {
        let when = info.lastSearchAt.map { Format.relative($0) } ?? ""
        switch info.empty {
        case .nosearch:
            empty(L10n.tr("title.releases.no.search"), info.check ? L10n.tr("title.releases.no.search.check") : L10n.tr("title.releases.no.search.hint"))
        case .nothingfound:
            empty(L10n.tr("title.releases.nothing"), L10n.format("title.releases.nothing.hint", when))
        case .dropped:
            empty(
                L10n.tr("title.releases.dropped"),
                L10n.format("title.releases.dropped.hint", when, info.droppedSources.joined(separator: ", "), info.dropped)
            )
        case .failed:
            empty(L10n.tr("title.releases.failed"), L10n.format("title.releases.failed.hint", when))
        case .nogood:
            Text(L10n.tr("title.releases.no.good")).font(.headline)
            Button(L10n.format("title.releases.show.hidden", info.allCount - info.goodCount)) { store.driver.setAllTab(true) }
        case .nomatch:
            Text(L10n.tr("title.releases.no.match")).font(.headline)
            Button(L10n.tr("title.clear.filters")) { store.driver.clearFilters() }
        case ReleaseEmptyKind.none:
            EmptyView()
        }
    }

    private func empty(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title).font(.headline)
            Text(body).font(.callout).foregroundStyle(Theme.textMuted)
        }
    }
}

private struct ReleaseRow: View {
    let release: ReleaseInfo
    let info: ReleasesInfo
    let store: ReleasesStore
    @State private var all = false
    @Environment(\.openURL) private var openURL

    var body: some View {
        let sent = info.sent.contains(release.id) || release.state == "chosen"
        let armed = info.armed.contains(release.id)
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.xSmall) {
                    StatusChip(text: TitleText.verdict(release.verdict), tone: release.verdictTone)
                    if release.recommended { StatusChip(text: L10n.tr("title.recommended"), tone: .ok) }
                    ForEach(release.tags, id: \.self) { StatusChip(text: $0, tone: .idle) }
                }
            }
            if release.infoURL.hasPrefix("http"), let url = URL(string: release.infoURL) {
                Button(release.name) { openURL(url) }.font(.subheadline.weight(.semibold))
            } else {
                Text(release.name).font(.subheadline.weight(.semibold))
            }
            Text(facts)
                .font(.footnote)
                .foregroundStyle((release.seeders ?? 5) < 5 && release.protocolName == "torrent" ? Theme.failText : Theme.textMuted)
            ForEach(Array((all ? release.reasons : Array(release.keyReasons.prefix(2))).enumerated()), id: \.offset) { _, reason in
                Text("· " + reason).font(.footnote).lineLimit(3)
            }
            if release.reasons.count > 2 {
                Button(all ? L10n.tr("title.fewer.reasons") : L10n.format("title.all.reasons", release.reasons.count)) { all.toggle() }
                    .font(.footnote)
                    .buttonStyle(.borderless)
            }
            if armed { Text(L10n.tr("title.pick.anyway.warning")).font(.footnote).foregroundStyle(Theme.waitText) }
            HStack {
                if sent {
                    StatusChip(text: L10n.tr("title.sent"), tone: .info)
                } else {
                    Button(armed ? L10n.tr("title.pick.anyway") : L10n.tr("title.pick")) {
                        let id = release.id
                        store.run { await $0.pick(id) }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(armed ? Theme.wait : Theme.accent)
                    .disabled(info.busy == release.id)
                }
                Spacer()
                if !info.check && release.state != "turned_down" {
                    Menu {
                        Button(L10n.tr("title.never.pick")) {
                            let id = release.id
                            store.run { await $0.turnDown(id, blockGroup: false) }
                        }
                        if !release.group.isEmpty {
                            Button(L10n.format("title.block.group", release.group)) {
                                let id = release.id
                                store.run { await $0.turnDown(id, blockGroup: true) }
                            }
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle").accessibilityLabel(L10n.tr("title.more.actions"))
                    }
                }
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var facts: String {
        var out = [TitleText.bytes(release.size)]
        if let seeders = release.seeders { out.append(L10n.plural("title.seeders", seeders)) }
        if !release.sourceName.isEmpty { out.append(release.sourceName) }
        out.append(release.protocolName == "usenet" ? "Usenet" : L10n.tr("title.torrent"))
        if release.passed { out.append(L10n.plural("title.release.points", release.points)) }
        if let published = release.publishedAt {
            let days = Calendar.current.dateComponents([.day], from: published, to: Date()).day ?? 0
            if days <= 0 {
                out.append(L10n.tr("title.age.today"))
            } else if days < 60 {
                out.append(L10n.plural("title.age.days", days))
            } else if days < 730 {
                out.append(L10n.plural("title.age.months", days / 30))
            } else {
                out.append(L10n.plural("title.age.years", days / 365))
            }
        }
        return out.filter { !$0.isEmpty }.joined(separator: " · ")
    }
}
