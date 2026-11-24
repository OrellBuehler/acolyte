import SwiftUI

struct SystemBoardView: View {
    let board: SystemBoardInfo
    let info: HomeInfo
    @Environment(Navigator.self) private var navigator

    var body: some View {
        VStack(spacing: Theme.Spacing.medium) {
            Button { navigator.open(.downloads) } label: { downloads }.buttonStyle(.plain)
            attention
            HStack(alignment: .top, spacing: Theme.Spacing.medium) {
                part(L10n.tr("home.check.sources"), board.sources, off: board.sourcesOff, add: L10n.tr("home.sys.add.source"))
                part(L10n.tr("home.check.downloaders"), board.downloaders, off: false, add: L10n.tr("home.sys.add.downloader"))
            }
            Button { navigator.open(.health) } label: {
                card {
                    Text(L10n.tr("home.sys.running.for")).font(.headline)
                    Text(HomeText.duration(board.uptimeSeconds)).font(.title2.bold())
                    if !board.version.isEmpty {
                        Text(L10n.format("home.sys.version", board.version)).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, Theme.Spacing.screen)
    }

    private var downloads: some View {
        card {
            Text(L10n.tr("home.sys.downloading")).font(.headline)
            if board.downloadsFailed {
                Text(L10n.tr("home.could.not.read")).font(.title2.bold())
                Text(L10n.tr("home.could.not.read.downloads")).font(.footnote).foregroundStyle(Theme.textMuted)
            } else {
                Text(board.speed > 0 ? HomeText.bytes(Int64(board.speed)) + "/s" : L10n.format("home.stage.downloading", board.downloading))
                    .font(.title2.bold())
                HStack(spacing: Theme.Spacing.large) {
                    counter(L10n.tr("home.sys.waiting"), board.waiting)
                    counter(L10n.tr("home.sys.downloading"), board.downloading)
                    counter(L10n.tr("home.sys.importing"), board.importing)
                }
                ForEach(board.running) { download in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(download.name).font(.subheadline.weight(.semibold)).lineLimit(1)
                        Text(stateLine(download)).font(.footnote).foregroundStyle(Theme.textMuted)
                        if download.stage == .downloading {
                            ProgressView(value: min(max(download.progress, 0), 1)).tint(Theme.accent)
                        }
                    }
                }
                if board.waiting + board.downloading + board.importing == 0 {
                    if board.finished.isEmpty {
                        Text(L10n.tr("home.sys.idle")).font(.footnote).foregroundStyle(Theme.textMuted)
                    } else {
                        ForEach(board.finished) { done in
                            let when = done.finishedAt.map { L10n.format("home.sys.finished", Format.relative($0)) }
                            Text([done.name, when ?? ""].filter { !$0.isEmpty }.joined(separator: " · "))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                    }
                    if board.paused > 0 {
                        Text(L10n.format("home.sys.paused", board.paused)).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                    if board.notSent > 0 {
                        Text(L10n.format("home.sys.not.sent", board.notSent)).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
            }
        }
    }

    private var attention: some View {
        card {
            Text(L10n.tr("home.tile.attention")).font(.headline)
            if !board.attention.isEmpty {
                ForEach(board.attention.prefix(4)) { row in
                    HStack(spacing: Theme.Spacing.small) {
                        Circle().fill(Theme.dotColor(row.tone)).frame(width: 8, height: 8)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(HomeText.attentionTitle(row)).font(.subheadline.weight(.semibold))
                            Text(HomeText.attentionReason(row)).font(.footnote).foregroundStyle(Theme.textMuted)
                        }
                        Spacer(minLength: 0)
                        Button(HomeText.actionWord(row)) { navigator.open(row.target.section) }
                            .font(.subheadline.weight(.semibold))
                    }
                }
                if board.attention.count > 4 {
                    Text(L10n.format("home.sys.more", board.attention.count - 4)).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                if board.attention.count <= 2, board.passed > 0 {
                    Text(L10n.plural("home.sys.passed", board.passed)).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            } else if board.unchecked {
                Text(L10n.tr("home.could.not.check")).font(.title2.bold())
                Text(L10n.tr("home.sys.unchecked")).font(.footnote).foregroundStyle(Theme.textMuted)
            } else {
                Text(L10n.tr("home.all.good")).font(.title2.bold()).foregroundStyle(Theme.okText)
                Text(L10n.tr("home.sys.all.good")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }

    private func part(_ label: String, _ count: PartCountInfo?, off: Bool, add: String) -> some View {
        Button { navigator.open(.settings) } label: {
            card {
                Text(label).font(.subheadline.weight(.semibold))
                if off {
                    Text(L10n.tr("home.sys.off")).font(.title3.bold())
                    Text(L10n.tr("home.sys.off.parts")).font(.footnote).foregroundStyle(Theme.textMuted)
                } else if let count {
                    if count.total == 0 {
                        Text(L10n.tr("home.sys.none.added")).font(.title3.bold())
                        Text(add).font(.footnote).foregroundStyle(Theme.textMuted)
                    } else {
                        Text(L10n.format("home.sys.working", count.working, count.total)).font(.title3.bold())
                        Text(partLine(count))
                            .font(.footnote)
                            .foregroundStyle(count.troubled.isEmpty ? Theme.textMuted : Theme.failText)
                    }
                } else {
                    Text(L10n.tr("home.could.not.read")).font(.title3.bold())
                    Text(L10n.tr("home.could.not.read.parts")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
        }
        .buttonStyle(.plain)
    }

    private func partLine(_ count: PartCountInfo) -> String {
        if count.troubled.count == 1 { return L10n.format("home.sys.has.problem", count.troubled[0]) }
        if count.troubled.count > 1 { return L10n.format("home.sys.have.problem", count.troubled.count) }
        if count.off > 0 { return L10n.format("home.sys.turned.off", count.off) }
        return L10n.tr("home.sys.all.working")
    }

    private func stateLine(_ download: SystemDownloadInfo) -> String {
        let percent = Int(min(max(download.progress, 0), 1) * 100)
        switch download.stage {
        case .downloading:
            return download.speed > 0 ? "\(percent)% · \(HomeText.bytes(Int64(download.speed)))/s" : "\(percent)%"
        case .queued: return L10n.tr("home.sys.waiting")
        case .importing: return L10n.tr("home.sys.importing")
        case .checking: return L10n.tr("home.sys.checking")
        case .stalled: return L10n.format("home.sys.stalled", percent)
        default: return ""
        }
    }

    private func counter(_ label: String, _ value: Int) -> some View {
        VStack(alignment: .leading) {
            Text(String(value)).font(.title3.weight(.semibold))
            Text(label).font(.footnote).foregroundStyle(Theme.textMuted)
        }
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) { content() }
            .foregroundStyle(Theme.text)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Theme.Spacing.large)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}
