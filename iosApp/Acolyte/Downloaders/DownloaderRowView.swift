import SwiftUI

struct DownloaderGiveUp: Identifiable, Hashable {
    let row: DownloaderRowInfo
    let attempt: DownloaderAttemptInfo

    var id: Int64 { attempt.id }
}

struct DownloaderRowView: View {
    let row: DownloaderRowInfo
    let canEdit: Bool
    let store: DownloadersStore
    let onGiveUp: @MainActor (DownloaderGiveUp) -> Void
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            header
            Text(L10n.format("acq.dl.detail", row.host, row.priority, row.category))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Text(flags)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if row.active > 0 {
                Text(activeLine)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            actions
            if row.showHealthText {
                Text(row.healthText)
                    .font(.footnote)
                    .foregroundStyle(row.healthTextFails ? Theme.failText : Theme.textMuted)
            }
            failedTest
            attempts
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var header: some View {
        HStack(spacing: Theme.Spacing.medium) {
            Text(row.badge)
                .font(.footnote.weight(.bold))
                .foregroundStyle(Theme.textMuted)
                .frame(width: 40, height: 40)
                .background(Theme.surfaceHigh, in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(row.name)
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                Text(L10n.format("acq.dl.sub", row.appName, L10n.tr(row.usenet ? "acq.dl.usenet" : "acq.dl.torrents")))
                    .font(.subheadline)
                    .foregroundStyle(Theme.textMuted)
            }
            Spacer(minLength: Theme.Spacing.small)
            if row.showHealthWord {
                StatusChip(text: DownloadersWords.health(row), tone: row.tone)
            }
        }
    }

    private var flags: String {
        var parts: [String] = []
        if !row.tags.isEmpty { parts.append(L10n.format("acq.dl.tags.list", row.tags.joined(separator: ", "))) }
        if row.allowLocal { parts.append(L10n.tr("acq.dl.local.reach")) }
        if row.locked {
            parts.append(L10n.tr("acq.dl.read.only"))
        } else if row.handsOff {
            parts.append(L10n.tr("acq.dl.hands.off"))
        } else {
            parts.append(L10n.tr("acq.dl.can.change"))
        }
        return parts.joined(separator: " · ")
    }

    private var activeLine: String {
        let active = L10n.format("acq.dl.active", row.active)
        guard let checked = row.checkedAt else { return active }
        return active + " · " + L10n.format("acq.dl.checked", Format.relative(checked))
    }

    private var actions: some View {
        HStack(spacing: Theme.Spacing.small) {
            Button {
                store.test(row.id)
            } label: {
                if row.testing {
                    ProgressView()
                } else {
                    Text(L10n.tr("acq.dl.test"))
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(row.testing || row.busy)
            if let test = row.test {
                StatusChip(text: DownloadersWords.testTag(test), tone: test.ok ? .ok : .fail)
            }
            Spacer(minLength: 0)
            if canEdit {
                Menu {
                    Button(row.paused ? L10n.tr("acq.dl.resume") : L10n.tr("acq.dl.pause")) {
                        if row.paused { store.resume(row.id) } else { store.pause(row.id) }
                    }
                    if row.canChange {
                        Button(L10n.tr("acq.dl.change")) { store.startChange(row.id) }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.title3)
                }
                .disabled(row.busy)
                .accessibilityLabel(L10n.format("acq.dl.more", row.name))
            }
        }
    }

    @ViewBuilder
    private var failedTest: some View {
        if let test = row.test, !test.ok {
            Text(DownloadersWords.testText(test))
                .font(.footnote)
                .foregroundStyle(Theme.failText)
            if let url = row.helpURL {
                Button(L10n.tr("acq.dl.connect.help")) { openURL(url) }
                    .font(.footnote)
            }
            if !test.category.isEmpty {
                if test.canMake, canEdit {
                    Button(L10n.format("acq.dl.make.category", test.category)) { store.makeCategory(row.id, test.category) }
                        .buttonStyle(.bordered)
                        .disabled(row.busy)
                } else if test.makeOff {
                    Text(L10n.tr("acq.dl.make.off"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    @ViewBuilder
    private var attempts: some View {
        if !row.pending.isEmpty {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(L10n.tr("acq.dl.attempt.title"))
                    .font(.subheadline.weight(.semibold))
                Text(L10n.format("acq.dl.attempt.text", row.appName))
                    .font(.footnote)
                ForEach(row.pending) { attempt in
                    HStack {
                        Text(L10n.format("acq.dl.attempt.line", Int(attempt.releaseId), Int(attempt.slotId), DownloadersWords.ago(attempt.createdAt)))
                            .font(.footnote)
                        Spacer(minLength: Theme.Spacing.small)
                        if canEdit {
                            Button(L10n.tr("acq.dl.give.up")) { onGiveUp(DownloaderGiveUp(row: row, attempt: attempt)) }
                                .buttonStyle(.bordered)
                                .disabled(row.abandoning != 0)
                        }
                    }
                }
                Text(L10n.format("acq.dl.attempt.footer", row.appName))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            .padding(Theme.Spacing.medium)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
                    .strokeBorder(Theme.fail.opacity(0.45), lineWidth: 1)
            }
        } else if let error = row.attemptError {
            Text(L10n.format("acq.dl.attempts.unchecked", DownloadersWords.error(error)))
                .font(.footnote)
                .foregroundStyle(Theme.failText)
        }
    }
}
