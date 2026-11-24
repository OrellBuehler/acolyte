import SwiftUI

struct SettingsSeerrSections: View {
    let info: SettingsApiInfo
    let store: SettingsApiStore
    let copy: (String, String) -> Void
    let share: (String) -> Void
    let confirm: (SettingsKeysConfirm) -> Void

    var body: some View {
        Section {
            if let error = info.seerrError {
                Text(error.localized)
                    .foregroundStyle(Theme.failText)
                if info.seerr == nil {
                    Button(L10n.tr("common.retry")) { store.reloadSeerr() }
                }
            }
            if let card = info.seerr {
                Toggle(
                    L10n.tr("settings.keys.seerr.answer"),
                    isOn: Binding(get: { card.enabled }, set: { store.setSeerrEnabled($0) })
                )
                .disabled(info.seerrSaving)
            }
        } header: {
            Text(L10n.tr("settings.keys.seerr.title"))
        } footer: {
            Text(L10n.tr("settings.keys.seerr.description"))
        }
        if let card = info.seerr, card.enabled {
            ForEach(card.sides) { side in
                Section {
                    sideRows(side)
                }
            }
            if !card.sent.isEmpty {
                Section {
                    ForEach(card.sent) { row in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(row.name)
                            Text(sentLine(row))
                                .font(.footnote)
                                .foregroundStyle(Theme.textMuted)
                        }
                    }
                } header: {
                    Text(L10n.tr("settings.keys.seerr.sent.title"))
                } footer: {
                    Text(L10n.tr("settings.keys.seerr.sent.hint"))
                }
            }
            Section {
                Toggle(
                    L10n.tr("settings.keys.seerr.delete.files"),
                    isOn: Binding(get: { card.deleteFiles }, set: { store.setSeerrDeleteFiles($0) })
                )
                .disabled(info.seerrSaving)
            } footer: {
                Text(L10n.tr("settings.keys.seerr.delete.files.hint"))
            }
        }
    }

    @ViewBuilder
    private func sideRows(_ side: SettingsSeerrSideInfo) -> some View {
        let app = SettingsKeysText.appName(side.side)
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(L10n.format("settings.keys.seerr.add.as", SettingsKeysText.what(side.side), app))
                .font(.headline)
            Text(L10n.format("settings.keys.seerr.url.base", side.path))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Text(keyLine(side))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if side.hasKey && !side.ownerActive {
                Text(L10n.format("settings.keys.seerr.owner.off", side.owner))
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
            }
        }
        Button {
            copy(side.path, L10n.tr("settings.keys.copied.url.base"))
        } label: {
            Label(L10n.format("settings.keys.seerr.copy.url", app), systemImage: "doc.on.doc")
        }
        if side.hasKey {
            Button(L10n.tr("settings.keys.seerr.remove"), role: .destructive) {
                confirm(.remove(side: side.side))
            }
            .disabled(side.busy)
        }
        Button(L10n.tr(side.hasKey ? "settings.keys.seerr.new" : "settings.keys.seerr.make")) {
            if side.hasKey {
                confirm(.replace(side: side.side))
            } else {
                store.makeSeerrKey(side.side)
            }
        }
        .disabled(side.busy)
        if let fresh = info.seerrFresh, fresh.side == side.side {
            SettingsKeysFreshBox(
                title: L10n.format("settings.keys.seerr.fresh.title", app),
                text: L10n.tr("settings.keys.seerr.fresh.text"),
                secret: fresh.key,
                copy: { copy(fresh.key, L10n.tr("settings.keys.copied.key")) },
                share: { share(fresh.key) },
                close: { store.dismissSeerrFresh() }
            )
        }
    }

    private func keyLine(_ side: SettingsSeerrSideInfo) -> String {
        guard side.hasKey else { return L10n.tr("settings.keys.seerr.no.key") }
        if let used = side.lastUsedAt {
            return L10n.format("settings.keys.seerr.key.used", side.hint, side.owner, Format.relative(used))
        }
        return L10n.format("settings.keys.seerr.key.new", side.hint, side.owner)
    }

    private func sentLine(_ row: SettingsSeerrSentInfo) -> String {
        var parts = [
            L10n.tr(row.kind == "format" ? "settings.keys.seerr.format" : "settings.keys.seerr.profile"),
            SettingsKeysText.appName(row.side),
        ]
        if let updated = row.updatedAt {
            parts.append(L10n.format("settings.keys.seerr.updated", Format.relative(updated)))
        }
        return parts.joined(separator: " · ")
    }
}
