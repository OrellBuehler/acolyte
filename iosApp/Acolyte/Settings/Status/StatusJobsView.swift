import SwiftUI

struct StatusJobsView: View {
    let info: HealthInfo
    let store: HealthStore

    var body: some View {
        StatusPlate(title: L10n.tr("settings.status.running.now")) {
            if info.running.isEmpty {
                Text(L10n.tr("settings.status.nothing.running"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.running) { job in
                StatusRunningRow(job: job)
            }
        }
        StatusPlate(title: L10n.tr("settings.status.next.up")) {
            if info.nextUp.isEmpty {
                Text(L10n.tr("settings.status.no.runs.planned"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.nextUp) { next in
                HStack(spacing: Theme.Spacing.small) {
                    Text(StatusText.due(next.due))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .frame(width: 96, alignment: .leading)
                    Circle()
                        .fill(Theme.dotColor(next.tone))
                        .frame(width: 8, height: 8)
                    Text(next.name)
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                    Spacer(minLength: Theme.Spacing.small)
                    if next.tone == .fail || next.tone == .wait {
                        StatusChip(text: StatusText.word(next.word), tone: next.tone)
                    }
                }
            }
        }
        StatusPlate(title: L10n.tr("settings.status.recently.finished")) {
            if info.finished.isEmpty {
                Text(L10n.tr("settings.status.nothing.finished"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.finished) { run in
                HStack(spacing: Theme.Spacing.small) {
                    Text(run.endedAt.map { Format.relative($0) } ?? "")
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .frame(width: 96, alignment: .leading)
                    Circle()
                        .fill(Theme.dotColor(run.tone))
                        .frame(width: 8, height: 8)
                    Text(run.name)
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                    Spacer(minLength: Theme.Spacing.small)
                    if run.tone != .ok {
                        StatusChip(text: StatusText.word(run.word), tone: run.tone)
                    }
                }
            }
        }
        StatusPlate(title: L10n.tr("settings.status.all.jobs")) {
            if info.jobLines.isEmpty {
                Text(info.failedReads.contains(.jobs) ? L10n.tr("settings.status.jobs.failed") : L10n.tr("settings.status.no.jobs"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.jobLines) { line in
                StatusJobLineView(line: line, info: info, store: store)
            }
        }
    }
}

struct StatusRunningRow: View {
    let job: StatusRunningInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(spacing: Theme.Spacing.small) {
                Text(job.name)
                    .font(.body)
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                if job.queued {
                    StatusChip(text: L10n.tr("settings.status.queued"), tone: .idle)
                } else if job.hasProgress {
                    Text("\(job.percent)%")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                } else {
                    StatusChip(text: L10n.tr("settings.status.word.running"), tone: .ok)
                }
            }
            if job.hasProgress {
                ProgressView(value: min(max(job.progress, 0), 1))
                    .tint(Theme.accent)
            }
            if !job.note.isEmpty {
                Text(job.note)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }
}

struct StatusJobLineView: View {
    let line: StatusJobLineInfo
    let info: HealthInfo
    let store: HealthStore

    var body: some View {
        let row = line.row
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Divider()
            HStack(spacing: Theme.Spacing.small) {
                Text(row.name)
                    .font(.body)
                    .foregroundStyle(Theme.text)
                Spacer(minLength: Theme.Spacing.small)
                if row.tone != .ok {
                    StatusChip(text: StatusText.word(row.word), tone: row.tone)
                }
            }
            if row.tone == .fail {
                let reason = StatusText.reason(row)
                if !reason.isEmpty {
                    Text(reason)
                        .font(.footnote)
                        .foregroundStyle(Theme.failText)
                }
            }
            Text(L10n.format("settings.status.last.run", row.lastRun.map { Format.relative($0) } ?? L10n.tr("settings.status.not.yet")))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Text(L10n.format("settings.status.next.run", StatusText.due(line.due)))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if info.canAct {
                Button(L10n.tr("settings.status.run.now")) { store.check(row.key) }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .disabled(info.isBusy(row.key))
            }
        }
    }
}
