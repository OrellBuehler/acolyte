import SwiftUI

struct StatusSupportView: View {
    var body: some View {
        StatusPlate(title: L10n.tr("settings.status.support.heading")) {
            VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
                text(L10n.tr("settings.status.support.p1"))
                text(L10n.tr("settings.status.support.p2"))
                Text(L10n.tr("settings.status.support.does.heading"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                bullet(L10n.tr("settings.status.support.does1"))
                bullet(L10n.tr("settings.status.support.does2"))
                bullet(L10n.tr("settings.status.support.does3"))
                text(L10n.tr("settings.status.support.p3.lead") + " " + L10n.tr("settings.status.support.p3"))
                text(L10n.tr("settings.status.support.p4"))
            }
            VStack(spacing: Theme.Spacing.small) {
                link(L10n.tr("settings.status.support.sponsor"), StatusLinks.sponsor, prominent: true)
                link(L10n.tr("settings.status.support.coffee"), StatusLinks.coffee, prominent: false)
                link(L10n.tr("settings.status.support.star"), StatusLinks.star, prominent: false)
                link(L10n.tr("settings.status.support.note"), StatusLinks.note, prominent: false)
            }
        }
    }

    private func text(_ value: String) -> some View {
        Text(value)
            .font(.callout)
            .foregroundStyle(Theme.text)
            .fixedSize(horizontal: false, vertical: true)
    }

    private func bullet(_ value: String) -> some View {
        HStack(alignment: .top, spacing: Theme.Spacing.small) {
            Text("•")
            Text(value)
        }
        .font(.callout)
        .foregroundStyle(Theme.text)
    }

    @ViewBuilder
    private func link(_ title: String, _ address: String, prominent: Bool) -> some View {
        if let url = URL(string: address) {
            if prominent {
                Link(destination: url) {
                    Label(title, systemImage: "heart.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            } else {
                Link(destination: url) {
                    Text(title)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
    }
}

struct StatusAboutView: View {
    let info: HealthInfo
    let store: HealthStore
    let showUpdates: () -> Void

    var body: some View {
        let about = info.about
        StatusPlate(title: L10n.tr("settings.status.tab.about")) {
            if info.aboutLoad == .failed && !about.hasDetails {
                Text(L10n.tr("settings.status.about.failed"))
                    .font(.callout)
                    .foregroundStyle(Theme.failText)
                Button(L10n.tr("settings.status.try.again")) { Task { await store.refresh() } }
                    .buttonStyle(.bordered)
            }
            versionRow(about)
            if about.hasDetails {
                row(L10n.tr("settings.status.about.go"), about.go)
                row(L10n.tr("settings.status.about.system"), about.os)
                row(L10n.tr("settings.status.about.container"), about.container ? L10n.tr("settings.status.yes") : L10n.tr("settings.status.no"))
                row(L10n.tr("settings.status.about.database"), L10n.format("settings.status.about.sqlite", about.sqlite))
                row(L10n.tr("settings.status.about.migration"), String(about.migration))
                row(L10n.tr("settings.status.about.config.folder"), about.configDir, mono: true)
                row(L10n.tr("settings.status.about.start.folder"), about.startDir, mono: true)
                uptimeRow(about)
            }
            if about.hasServer {
                row(L10n.tr("settings.status.about.writes"), L10n.format("settings.status.about.writes.value", about.writes, String(about.writeWaitP95Ms)))
                row(L10n.tr("settings.status.about.url.base"), about.urlBase.isEmpty ? L10n.tr("settings.status.about.url.none") : about.urlBase)
                row(L10n.tr("settings.status.about.live.tabs"), String(about.liveClients))
            }
        }
        if info.canBundle {
            StatusPlate(title: L10n.tr("settings.status.help.title")) {
                Text(L10n.tr("settings.status.help.text"))
                    .font(.callout)
                    .foregroundStyle(Theme.text)
                Text(L10n.tr("settings.status.help.slow"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Button {
                    store.downloadBundle()
                } label: {
                    if info.bundleBusy {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    } else {
                        Text(L10n.tr("settings.status.bundle"))
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.bordered)
                .disabled(info.bundleBusy)
            }
        }
    }

    private func row(_ label: String, _ value: String, mono: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Theme.textMuted)
            Text(value)
                .font(mono ? Font.callout.monospaced() : Font.callout)
                .foregroundStyle(Theme.text)
        }
    }

    private func versionRow(_ about: StatusAboutInfo) -> some View {
        let denied = info.aboutLoad == .failed || info.aboutLoad == .denied
        return VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(L10n.tr("settings.status.about.version"))
                .font(.caption)
                .foregroundStyle(Theme.textMuted)
            HStack(spacing: Theme.Spacing.small) {
                Text(about.version.isEmpty ? (denied ? "-" : "…") : about.version)
                    .font(.callout)
                    .foregroundStyle(Theme.text)
                if info.updateAvailable {
                    StatusChip(text: L10n.tr("settings.status.update.available"), tone: .wait)
                } else if info.standing == .current {
                    StatusChip(text: L10n.tr("settings.status.up.to.date"), tone: .ok)
                }
            }
            if info.updateAvailable {
                Button(L10n.tr("settings.status.see.update"), action: showUpdates)
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }
        }
    }

    private func uptimeRow(_ about: StatusAboutInfo) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(L10n.tr("settings.status.about.uptime"))
                .font(.caption)
                .foregroundStyle(Theme.textMuted)
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let now = Int64(context.date.timeIntervalSince1970 * 1000)
                let elapsed = about.loadedAt > 0 ? max(0, now - about.loadedAt) / 1000 : 0
                Text(StatusText.uptime(about.uptimeSeconds + elapsed))
                    .font(.callout.monospacedDigit())
                    .foregroundStyle(Theme.text)
            }
        }
    }
}
