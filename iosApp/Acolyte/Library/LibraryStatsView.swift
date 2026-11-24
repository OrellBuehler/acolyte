import SwiftUI
import Observation

@MainActor
@Observable
final class LibraryStatsStore {
    private(set) var info: LibraryStatsInfo = .initial
    private(set) var removal: LibraryWatchedTitleInfo?

    private let driver = SDKClient.shared.makeLibraryStats()

    var selectedKind: String {
        get { info.current?.kind ?? info.kind }
        set { driver.setKind(newValue) }
    }

    var removalSheet: LibraryWatchedTitleInfo? {
        get { removal }
        set { removal = newValue }
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func askRemoval(_ title: LibraryWatchedTitleInfo) {
        removal = title
    }

    func commitCleanupDays(_ days: Int) async {
        try? await Task.sleep(for: .milliseconds(450))
        guard !Task.isCancelled, days != info.cleanupDays else { return }
        let driver = self.driver
        Task { await driver.setCleanupDays(days) }
    }

    func toggleCleanupPerson(_ key: String) {
        let driver = self.driver
        Task { await driver.toggleCleanupPerson(key) }
    }

    func removeWatched(id: Int64, deleteFiles: Bool, neverAdd: Bool) {
        let driver = self.driver
        Task { await driver.removeWatched(id: id, deleteFiles: deleteFiles, neverAdd: neverAdd) }
    }

    func dismissNotice(_ seq: Int) {
        driver.dismissNotice(seq)
    }
}

private enum LibraryStatsTab: Hashable {
    case files
    case watched
}

enum LibraryStatsText {
    static func kind(_ kind: String) -> String {
        switch kind {
        case "movies": L10n.tr("library.kind.movie")
        case "shows": L10n.tr("library.kind.show")
        case "music": L10n.tr("library.kind.music")
        default: kind
        }
    }

    static func fact(_ name: String) -> String {
        switch name {
        case "resolution": L10n.tr("library.stats.fact.resolution")
        case "video_codec": L10n.tr("library.stats.fact.video.codec")
        case "hdr": L10n.tr("library.stats.fact.hdr")
        case "audio_codec": L10n.tr("library.stats.fact.audio.codec")
        case "audio_channels": L10n.tr("library.stats.fact.audio.channels")
        case "container": L10n.tr("library.stats.fact.container")
        default: name
        }
    }

    static func hint(_ value: String) -> String? {
        switch value {
        case "unknown": L10n.tr("library.stats.value.unknown.hint")
        case "unsupported": L10n.tr("library.stats.value.unsupported.hint")
        default: nil
        }
    }

    static func bucket(fact: String, value: String) -> String {
        switch value {
        case "other": return L10n.tr("library.stats.value.other")
        case "unchecked": return L10n.tr("library.stats.value.unchecked")
        case "unknown": return L10n.tr("library.stats.value.unknown")
        case "unsupported": return L10n.tr("library.stats.value.unsupported")
        default: break
        }
        switch fact {
        case "video_codec": return videoCodecs[value] ?? value
        case "hdr": return hdrNames[value] ?? value
        case "container": return containers[value] ?? value.uppercased()
        case "audio_codec": return audioCodecs[value] ?? value.uppercased()
        default: return value
        }
    }

    private static let videoCodecs: [String: String] = [
        "h264": "AVC (H.264)",
        "hevc": "HEVC (H.265)",
        "av1": "AV1",
        "vp9": "VP9",
        "mpeg2": "MPEG-2",
        "xvid": "Xvid",
        "vc1": "VC-1"
    ]

    private static let hdrNames: [String: String] = [
        "hdr10": "HDR10",
        "hlg": "HLG",
        "dv": "Dolby Vision",
        "dv+hdr10": "Dolby Vision + HDR10",
        "sdr": "SDR"
    ]

    private static let containers: [String: String] = [
        "matroska": "MKV",
        "webm": "WebM",
        "mp4": "MP4",
        "flac": "FLAC",
        "mp3": "MP3",
        "m4a": "M4A",
        "ogg": "OGG",
        "opus": "Opus",
        "wav": "WAV"
    ]

    private static let audioCodecs: [String: String] = [
        "aac": "AAC",
        "ac3": "AC3",
        "eac3": "EAC3",
        "dts": "DTS",
        "truehd": "TrueHD",
        "flac": "FLAC",
        "alac": "ALAC",
        "mp3": "MP3",
        "mp2": "MP2",
        "opus": "Opus",
        "vorbis": "Vorbis",
        "pcm": "PCM",
        "wavpack": "WavPack",
        "tta": "TTA"
    ]
}

struct LibraryStatsView: View {
    @State private var store = LibraryStatsStore()
    @State private var tab: LibraryStatsTab = .files

    var body: some View {
        VStack(spacing: 0) {
            Picker(L10n.tr("library.stats"), selection: $tab) {
                Text(L10n.tr("library.stats.tab.files")).tag(LibraryStatsTab.files)
                Text(L10n.tr("library.stats.tab.watched")).tag(LibraryStatsTab.watched)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
            switch tab {
            case .files:
                LibraryStatsFilesTab(store: store)
            case .watched:
                LibraryStatsWatchedTab(store: store)
            }
        }
        .background(Theme.background)
        .navigationTitle(L10n.tr("library.stats"))
        .libraryNotice(store.info.notice) { store.dismissNotice($0) }
        .sheet(item: Bindable(store).removalSheet) { title in
            LibraryStatsRemoveSheet(title: title) { deleteFiles, neverAdd in
                store.removeWatched(id: title.id, deleteFiles: deleteFiles, neverAdd: neverAdd)
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }
}

private struct LibraryStatsCard<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            content()
        }
        .padding(Theme.Spacing.medium)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous)
                .strokeBorder(Theme.line, lineWidth: 1)
        }
    }
}

private struct LibraryStatsFilesTab: View {
    let store: LibraryStatsStore

    var body: some View {
        StatusContent(load: store.info.load, isEmpty: store.info.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(
                title: L10n.tr("library.stats.empty.title"),
                message: L10n.tr("library.stats.empty.text"),
                systemImage: "chart.bar"
            )
        } content: {
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.large) {
                    if let error = store.info.load.error {
                        NoticeBanner(text: LibraryText.error(error))
                    }
                    if store.info.kinds.count > 1 {
                        kindPicker
                    }
                    if let kind = store.info.current {
                        tiles(kind)
                        ForEach(kind.facts) { fact in
                            LibraryStatsFactCard(fact: fact)
                        }
                    }
                }
                .padding(Theme.Spacing.screen)
            }
            .refreshable { await store.refresh() }
        }
    }

    private var kindPicker: some View {
        Picker(L10n.tr("library.stats.kind"), selection: Bindable(store).selectedKind) {
            ForEach(store.info.kinds) { kind in
                Text(LibraryStatsText.kind(kind.kind)).tag(kind.kind)
            }
        }
        .pickerStyle(.segmented)
    }

    private func tiles(_ kind: LibraryStatsKindInfo) -> some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Theme.Spacing.medium) {
            LibraryStatsTile(title: L10n.tr("library.stats.tile.titles"), value: kind.titles.formatted())
            LibraryStatsTile(title: L10n.tr("library.stats.tile.files"), value: kind.files.formatted())
            LibraryStatsTile(title: L10n.tr("library.stats.tile.disk"), value: LibraryText.bytes(kind.bytes))
            LibraryStatsTile(title: L10n.tr("library.stats.tile.unchecked"), value: kind.unchecked.formatted())
        }
    }
}

private struct LibraryStatsTile: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(value)
                .font(.title2.weight(.bold))
                .foregroundStyle(Theme.text)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .lineLimit(2)
        }
        .padding(Theme.Spacing.medium)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

private struct LibraryStatsFactCard: View {
    let fact: LibraryStatsFactInfo

    var body: some View {
        LibraryStatsCard {
            Text(LibraryStatsText.fact(fact.name))
                .font(.headline)
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            ForEach(fact.buckets) { bucket in
                LibraryStatsBucketRow(fact: fact.name, bucket: bucket)
            }
        }
    }
}

private struct LibraryStatsBucketRow: View {
    let fact: String
    let bucket: LibraryStatsBucketInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(alignment: .firstTextBaseline, spacing: Theme.Spacing.small) {
                Text(LibraryStatsText.bucket(fact: fact, value: bucket.value))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                Text(LibraryText.plural("library.stats.bucket", bucket.files, LibraryText.bytes(bucket.bytes)))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .monospacedDigit()
            }
            ProgressView(value: min(max(bucket.share, 0), 1))
                .tint(Theme.accent)
                .accessibilityHidden(true)
            if let hint = LibraryStatsText.hint(bucket.value) {
                Text(hint)
                    .font(.caption)
                    .foregroundStyle(Theme.textFaint)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct LibraryStatsWatchedTab: View {
    let store: LibraryStatsStore

    var body: some View {
        StatusContent(load: store.info.watchedLoad, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            ScrollView {
                VStack(alignment: .leading, spacing: Theme.Spacing.large) {
                    if let error = store.info.watchedLoad.error {
                        NoticeBanner(text: LibraryText.error(error))
                    }
                    history
                    if store.info.canClean, let cleanup = store.info.cleanup {
                        LibraryStatsCleanupCard(store: store, cleanup: cleanup)
                    }
                }
                .padding(Theme.Spacing.screen)
            }
            .refreshable { await store.refresh() }
        }
    }

    @ViewBuilder
    private var history: some View {
        if !store.info.watchedEnabled {
            LibraryStatsNote(title: L10n.tr("library.stats.off.title"), text: L10n.tr("library.stats.off.text"))
        } else if store.info.readAt.isEmpty {
            LibraryStatsNote(title: L10n.tr("library.stats.first.title"), text: L10n.tr("library.stats.first.text"))
        } else if store.info.people.isEmpty {
            Text(L10n.format("library.stats.no.match", LibraryText.ago(store.info.readAt)))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
        } else {
            Text(L10n.tr("library.stats.help"))
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
            Text(L10n.format("library.stats.last.read", LibraryText.ago(store.info.readAt)))
                .font(.footnote)
                .foregroundStyle(Theme.textFaint)
            LibraryStatsCard {
                ForEach(store.info.people) { person in
                    LibraryStatsPersonRow(person: person)
                }
            }
        }
    }
}

private struct LibraryStatsNote: View {
    let title: String
    let text: String

    var body: some View {
        LibraryStatsCard {
            Text(title)
                .font(.headline)
                .foregroundStyle(Theme.text)
            Text(text)
                .font(.callout)
                .foregroundStyle(Theme.textMuted)
        }
    }
}

private struct LibraryStatsPersonRow: View {
    let person: LibraryWatchedPersonInfo

    private var displayName: String {
        person.linked && !person.userName.isEmpty ? person.userName : person.name
    }

    private var serverLine: String {
        if person.linked, !person.userName.isEmpty, person.userName != person.name {
            return person.server + " · " + person.name
        }
        return person.server
    }

    private var counts: String {
        var parts: [String] = []
        if person.movies > 0 { parts.append(LibraryText.plural("library.word.movie", person.movies)) }
        if person.episodes > 0 { parts.append(LibraryText.plural("library.stats.unit.episode", person.episodes)) }
        parts.append(LibraryText.plural("library.stats.unit.play", person.plays))
        return parts.joined(separator: " · ")
    }

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            Text(String(displayName.prefix(1)).uppercased())
                .font(.headline)
                .foregroundStyle(Theme.text)
                .frame(width: 36, height: 36)
                .background(Theme.surfaceHigh, in: Circle())
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                HStack(spacing: Theme.Spacing.small) {
                    Text(displayName)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                    if !person.linked {
                        StatusChip(text: L10n.tr("library.stats.not.linked"), tone: .wait)
                    }
                }
                Text(serverLine)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Text(counts)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                if !person.lastWatchedAt.isEmpty {
                    Text(L10n.format("library.stats.last.watched", LibraryText.ago(person.lastWatchedAt)))
                        .font(.footnote)
                        .foregroundStyle(Theme.textFaint)
                }
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct LibraryStatsCleanupCard: View {
    let store: LibraryStatsStore
    let cleanup: LibraryCleanupInfo

    @State private var days = 30

    var body: some View {
        LibraryStatsCard {
            Text(L10n.tr("library.stats.cleanup.title"))
                .font(.headline)
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            Text(L10n.tr("library.stats.cleanup.desc"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Stepper(value: $days, in: 0...3650) {
                HStack {
                    Text(L10n.tr("library.stats.cleanup.days"))
                        .foregroundStyle(Theme.text)
                    Spacer(minLength: Theme.Spacing.small)
                    Text(verbatim: String(days))
                        .foregroundStyle(Theme.textMuted)
                        .monospacedDigit()
                }
            }
            .onAppear { days = store.info.cleanupDays }
            .task(id: days) { await store.commitCleanupDays(days) }
            Text(L10n.tr("library.stats.cleanup.who"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            ForEach(cleanup.people) { person in
                LibraryStatsCleanupPersonRow(store: store, person: person)
            }
            result
            ForEach(cleanup.found) { title in
                LibraryStatsFoundRow(store: store, title: title)
            }
        }
    }

    @ViewBuilder
    private var result: some View {
        if store.info.cleanupLoading {
            ProgressView()
                .frame(maxWidth: .infinity)
        } else if store.info.cleanupPicked.isEmpty {
            resultText(L10n.tr("library.stats.cleanup.nobody"))
        } else if cleanup.found.isEmpty {
            resultText(LibraryText.plural("library.stats.cleanup.nothing", store.info.cleanupDays))
        } else {
            resultText(LibraryText.plural("library.stats.cleanup.found", cleanup.found.count, LibraryText.bytes(foundBytes)))
        }
    }

    private var foundBytes: Int64 {
        cleanup.found.reduce(Int64(0)) { $0 + $1.bytes }
    }

    private func resultText(_ text: String) -> some View {
        Text(text)
            .font(.footnote.weight(.medium))
            .foregroundStyle(Theme.textMuted)
    }
}

private struct LibraryStatsCleanupPersonRow: View {
    let store: LibraryStatsStore
    let person: LibraryCleanupPersonInfo

    private var picked: Bool {
        store.info.cleanupPicked.contains(person.key)
    }

    var body: some View {
        Button {
            store.toggleCleanupPerson(person.key)
        } label: {
            HStack(spacing: Theme.Spacing.medium) {
                Image(systemName: picked ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(picked ? Theme.accent : Theme.textFaint)
                VStack(alignment: .leading, spacing: 2) {
                    Text(person.name)
                        .foregroundStyle(Theme.text)
                    if !person.server.isEmpty {
                        Text(person.server)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(picked ? .isSelected : [])
    }
}

private struct LibraryStatsFoundRow: View {
    let store: LibraryStatsStore
    let title: LibraryWatchedTitleInfo

    private var name: String {
        title.year > 0 ? "\(title.name) (\(title.year))" : title.name
    }

    var body: some View {
        HStack(alignment: .center, spacing: Theme.Spacing.medium) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Text(L10n.format("library.stats.cleanup.last", LibraryText.ago(title.lastWatchedAt), LibraryText.bytes(title.bytes)))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            Spacer(minLength: Theme.Spacing.small)
            if store.info.removing == title.id {
                ProgressView()
            } else {
                Button(L10n.tr("library.stats.cleanup.remove"), role: .destructive) { store.askRemoval(title) }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .disabled(store.info.removing != 0)
            }
        }
    }
}

private struct LibraryStatsRemoveSheet: View {
    let title: LibraryWatchedTitleInfo
    let remove: @MainActor (Bool, Bool) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var deleteFiles = false
    @State private var neverAdd = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(L10n.tr("library.stats.remove.files"), isOn: $deleteFiles)
                    Toggle(L10n.tr("library.stats.remove.never"), isOn: $neverAdd)
                } footer: {
                    Text(L10n.tr("library.stats.remove.note"))
                }
            }
            .navigationTitle(L10n.format("library.stats.remove.title", title.name))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("library.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("library.stats.remove.button"), role: .destructive) {
                        remove(deleteFiles, neverAdd)
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
