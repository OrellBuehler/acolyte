import SwiftUI

struct StatusPlates: View {
    let info: HealthInfo
    let store: HealthStore

    var body: some View {
        StatusHealthPlate(info: info, store: store)
        StatusLinkedPlate(info: info, store: store)
        StatusStoragePlate(info: info)
    }
}

struct StatusCalmLine: View {
    let tone: ToneKind
    let text: String

    var body: some View {
        HStack(spacing: Theme.Spacing.small) {
            Circle()
                .fill(Theme.dotColor(tone))
                .frame(width: 10, height: 10)
            Text(text)
                .font(.callout)
                .foregroundStyle(Theme.text)
        }
    }
}

struct StatusRowActions: View {
    let row: StatusRowInfo
    let info: HealthInfo
    let store: HealthStore
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        if info.canAct && (row.checkKind != nil || row.fix != nil) {
            HStack(spacing: Theme.Spacing.small) {
                if row.checkKind != nil {
                    Button(row.group == .jobs ? L10n.tr("settings.status.run.now") : L10n.tr("settings.status.check")) {
                        store.check(row.key)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .disabled(info.isBusy(row.key))
                }
                if let fix = row.fix {
                    Button(L10n.tr("settings.status.fix")) { openRoute(fix.route) }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                }
            }
        }
    }
}

struct StatusRowDetails: View {
    let row: StatusRowInfo

    var body: some View {
        let reason = StatusText.reason(row)
        if !reason.isEmpty {
            Text(reason)
                .font(.callout)
                .foregroundStyle(Theme.text)
        }
        if let since = StatusText.since(row) {
            Text(since)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }
}

struct StatusHealthPlate: View {
    let info: HealthInfo
    let store: HealthStore
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        StatusPlate(title: L10n.tr("settings.status.health")) {
            if !info.problems.isEmpty {
                ForEach(info.problems) { row in
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        HStack(alignment: .top, spacing: Theme.Spacing.small) {
                            StatusBang(tone: row.tone)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(StatusText.title(row))
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(Theme.text)
                                Text(StatusText.group(row.group))
                                    .font(.footnote)
                                    .foregroundStyle(Theme.textMuted)
                            }
                            Spacer(minLength: Theme.Spacing.small)
                            StatusChip(text: StatusText.word(row.word), tone: row.tone)
                        }
                        StatusRowDetails(row: row)
                        StatusRowActions(row: row, info: info, store: store)
                    }
                }
            } else if info.nothingLinked {
                StatusCalmLine(tone: .idle, text: L10n.tr("settings.status.nothing.to.check"))
                if info.canAct {
                    Button(L10n.tr("settings.status.add.source")) { openRoute(StatusFixID.sources.route) }
                        .buttonStyle(.bordered)
                }
            } else if info.linkedUnreadable {
                StatusCalmLine(tone: .idle, text: L10n.tr("settings.status.no.problems.found"))
            } else {
                StatusCalmLine(tone: .ok, text: L10n.tr("settings.status.all.good"))
            }
        }
    }
}

struct StatusLinkedPlate: View {
    let info: HealthInfo
    let store: HealthStore
    @State private var picked: StatusGroupID?

    private var current: StatusKindInfo? {
        let group = picked ?? info.defaultKind
        return info.kinds.first { $0.group == group } ?? info.kinds.first
    }

    var body: some View {
        let ok = info.kinds.reduce(0) { $0 + $1.okCount }
        let on = info.kinds.reduce(0) { $0 + $1.onCount }
        StatusPlate(title: L10n.tr("settings.status.linked"), subtitle: "\(ok)/\(on)") {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Theme.Spacing.small) {
                    ForEach(info.kinds) { kind in
                        Button {
                            picked = kind.group
                        } label: {
                            HStack(spacing: 6) {
                                StatusMarkerView(marker: kind.marker)
                                Text(StatusText.group(kind.group))
                                    .font(.subheadline.weight(.semibold))
                                    .lineLimit(1)
                            }
                            .padding(.horizontal, Theme.Spacing.medium)
                            .padding(.vertical, Theme.Spacing.small)
                            .foregroundStyle(kind.group == current?.group ? Theme.text : Theme.textMuted)
                            .background(kind.group == current?.group ? Theme.surfaceHigh : Theme.surfaceRaised, in: Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(kind.group == current?.group ? .isSelected : [])
                    }
                }
            }
            if let current {
                StatusKindPane(kind: current, info: info, store: store)
                    .id(current.group)
            }
        }
    }
}

struct StatusMarkerView: View {
    let marker: StatusMarkerID

    var body: some View {
        switch marker {
        case .fail: StatusBang(tone: .fail)
        case .wait: StatusBang(tone: .wait)
        case .ok: Circle().fill(Theme.dotColor(.ok)).frame(width: 8, height: 8)
        case .idle, .none: Circle().fill(Theme.dotColor(.idle)).frame(width: 8, height: 8)
        }
    }
}

struct StatusKindPane: View {
    let kind: StatusKindInfo
    let info: HealthInfo
    let store: HealthStore
    @State private var showAll = false
    @Environment(\.openRoute) private var openRoute

    private let shown = 6

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(StatusText.group(kind.group))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                    Text(StatusText.summary(kind))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Spacer(minLength: Theme.Spacing.small)
                if info.canAct && !kind.rows.isEmpty {
                    Button(L10n.tr("settings.status.manage")) { openRoute(kind.fix.route) }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                }
            }
            if kind.rows.isEmpty {
                Text(StatusText.none(kind.group))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                if info.canAct {
                    Button(L10n.tr("settings.status.add")) { openRoute(kind.fix.route) }
                        .buttonStyle(.bordered)
                }
            } else {
                let rows = showAll ? kind.rows : Array(kind.rows.prefix(shown))
                ForEach(rows) { row in
                    Divider()
                    StatusKindRow(row: row, info: info, store: store)
                }
                if kind.rows.count > shown {
                    Button(showAll ? L10n.tr("settings.status.show.fewer") : L10n.format("settings.status.show.all", kind.rows.count)) {
                        showAll.toggle()
                    }
                    .font(.subheadline.weight(.semibold))
                }
            }
        }
    }
}

struct StatusKindRow: View {
    let row: StatusRowInfo
    let info: HealthInfo
    let store: HealthStore

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(spacing: Theme.Spacing.small) {
                Text(StatusText.title(row))
                    .font(.body)
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
                Spacer(minLength: Theme.Spacing.small)
                StatusChip(text: StatusText.word(row.word), tone: row.tone)
            }
            if row.needsAttention {
                StatusRowDetails(row: row)
                StatusRowActions(row: row, info: info, store: store)
            }
        }
    }
}

struct StatusStoragePlate: View {
    let info: HealthInfo

    var body: some View {
        StatusPlate(title: L10n.tr("settings.status.group.library")) {
            if !info.disksLoaded {
                ProgressView()
            } else if info.disks.isEmpty {
                Text(L10n.tr("settings.status.no.folders"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            } else {
                ForEach(info.disks) { disk in
                    StatusDiskView(disk: disk, info: info)
                }
            }
            if let backups = info.backups {
                StatusBackupsLine(backups: backups, keyKit: info.keyKit)
            }
        }
    }
}

struct StatusDiskView: View {
    let disk: StatusDiskInfo
    let info: HealthInfo
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(spacing: Theme.Spacing.small) {
                Text(StatusText.diskName(disk))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                if let problem = disk.problem {
                    StatusChip(text: StatusText.word(problem.word), tone: problem.tone)
                } else {
                    StatusChip(text: L10n.tr("settings.status.word.working"), tone: .ok)
                }
            }
            if disk.freeBytes >= 0 {
                let free = ByteCountFormatter.string(fromByteCount: disk.freeBytes, countStyle: .file)
                if disk.totalBytes > 0 {
                    Text(L10n.format("settings.status.free.of", free, ByteCountFormatter.string(fromByteCount: disk.totalBytes, countStyle: .file)))
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                } else {
                    Text(L10n.format("settings.status.free", free))
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                }
            }
            if disk.usedPercent >= 0 {
                ProgressView(value: Double(disk.usedPercent) / 100)
                    .tint(barColor)
            }
            HStack(spacing: Theme.Spacing.small) {
                Text(disk.path)
                    .font(.footnote.monospaced())
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
                    .truncationMode(.middle)
                Spacer(minLength: Theme.Spacing.small)
                if disk.usedPercent >= 0 {
                    Text(L10n.format("settings.status.pct.used", disk.usedPercent))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            if let problem = disk.problem {
                let reason = StatusText.reason(problem)
                if !reason.isEmpty {
                    Text(reason)
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                }
                if info.canAct, let fix = problem.fix {
                    Button(L10n.tr("settings.status.fix")) { openRoute(fix.route) }
                        .buttonStyle(.bordered)
                        .controlSize(.small)
                }
            }
        }
    }

    private var barColor: Color {
        switch disk.level {
        case .normal: Theme.accent
        case .warn: Theme.wait
        case .full: Theme.fail
        }
    }
}

struct StatusBackupsLine: View {
    let backups: StatusRowInfo
    let keyKit: StatusRowInfo?

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Divider()
            HStack(spacing: Theme.Spacing.small) {
                Text(L10n.tr("settings.status.backups"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                StatusChip(text: StatusText.word(backups.word), tone: backups.tone)
            }
            if let at = backups.at {
                Text(L10n.format("settings.status.last.checked", Format.relative(at)))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if backups.tone != .ok {
                let reason = StatusText.reason(backups)
                if !reason.isEmpty {
                    Text(reason)
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                }
            }
            if let keyKit {
                Text(StatusText.reason(keyKit))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }
}
