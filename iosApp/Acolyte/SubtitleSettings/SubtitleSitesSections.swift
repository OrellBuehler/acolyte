import SwiftUI

struct SubtitleSitesSections: View {
    let info: SubtitleCfgInfo
    let store: SubtitleSettingsStore
    let remove: (SubtitleCfgSite) -> Void

    var body: some View {
        Section {
            if !info.kinds.isEmpty {
                Button(L10n.tr("acq.sub.add.site")) { store.startAdd() }
            }
            if info.sites.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("acq.sub.none.title"))
                    Text(L10n.tr("acq.sub.none.text")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
        } header: {
            Text(L10n.tr("acq.sub.sites.title"))
        } footer: {
            Text(L10n.tr("acq.sub.sites.sub"))
        }
        ForEach(info.sites) { site in
            SubtitleSiteSection(site: site, store: store, remove: remove)
        }
    }
}

struct SubtitleSiteSection: View {
    let site: SubtitleCfgSite
    let store: SubtitleSettingsStore
    let remove: (SubtitleCfgSite) -> Void

    var body: some View {
        Section {
            Text(site.url)
                .font(.footnote.monospaced())
                .foregroundStyle(Theme.textMuted)
            StatusChip(text: SubtitleCfgWords.stateLabel(site.state), tone: SubtitleCfgWords.stateTone(site.state))
            Text(SubtitleCfgWords.stateText(site)).font(.footnote)
            Text(SubtitleCfgWords.siteLine(site)).font(.footnote).foregroundStyle(Theme.textMuted)
            testResult
            HStack {
                Button {
                    store.testSite(site.id)
                } label: {
                    if site.testing { ProgressView() } else { Text(L10n.tr("acq.sub.site.test")) }
                }
                .buttonStyle(.borderless)
                .disabled(site.testing)
                Spacer()
                menu
            }
        } header: {
            Text(site.name)
        }
    }

    @ViewBuilder
    private var testResult: some View {
        if let ok = site.testOk {
            HStack {
                if ok {
                    StatusChip(text: okLabel, tone: .ok)
                } else {
                    StatusChip(text: L10n.tr("acq.sub.test.failed"), tone: .fail)
                    Text(failText).font(.footnote).foregroundStyle(Theme.failText)
                }
            }
        }
    }

    private var okLabel: String {
        [site.kindName, site.testExtra].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private var failText: String {
        if let error = site.testError { return SubtitleCfgWords.error(error) }
        return site.testMessage
    }

    private var menu: some View {
        Menu {
            if site.offered {
                Button(L10n.tr(site.turnedOff ? "acq.sub.turn.on" : "acq.sub.pause")) { store.togglePaused(site.id) }
                Button(L10n.tr("acq.sub.change")) { store.startChange(site.id) }
            }
            Button(L10n.tr("acq.sub.remove"), role: .destructive) { remove(site) }
        } label: {
            Image(systemName: "ellipsis.circle")
                .accessibilityLabel(site.name)
        }
    }
}

struct SubtitleSiteFormView: View {
    let store: SubtitleSettingsStore
    @State private var limits = false

    private var info: SubtitleCfgInfo { store.info }

    var body: some View {
        NavigationStack {
            if let form = info.form {
                content(form)
                    .navigationTitle(form.isNew ? L10n.tr("acq.sub.add.site") : L10n.format("acq.sub.change.title", form.name))
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button(L10n.tr("acq.sub.cancel")) { store.cancelForm() }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            if form.busy {
                                ProgressView()
                            } else {
                                Button(L10n.tr("acq.sub.save")) { store.submitForm() }
                                    .disabled(!form.canSubmit)
                            }
                        }
                    }
            }
        }
    }

    private func kindFor(_ form: SubtitleCfgForm) -> SubtitleCfgKind? {
        info.kinds.first { $0.kind == form.kind }
    }

    private func content(_ form: SubtitleCfgForm) -> some View {
        Form {
            if form.isNew { kindsSection(form) }
            detailsSection(form)
            if let kind = kindFor(form), kind.needsAccount { accountSection(form, kind) }
            limitsSection(form)
            optionsSection(form)
            if let error = form.error {
                Section { Text(SubtitleCfgWords.error(error)).foregroundStyle(Theme.failText) }
            }
        }
    }

    private func kindsSection(_ form: SubtitleCfgForm) -> some View {
        Section(L10n.tr("acq.sub.which.site")) {
            ForEach(info.kinds) { kind in
                Button {
                    store.pickKind(kind.kind)
                } label: {
                    HStack(alignment: .top) {
                        Image(systemName: kind.kind == form.kind ? "largecircle.fill.circle" : "circle")
                            .foregroundStyle(Theme.accentText)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("\(kind.name) (\(SubtitleCfgWords.covers(kind.covers)))").foregroundStyle(Theme.text)
                            if kind.needsAccount { StatusChip(text: L10n.tr("acq.sub.needs.yours"), tone: .fail) }
                            if !kind.note.isEmpty {
                                Text(kind.note).font(.footnote).foregroundStyle(Theme.textMuted)
                            }
                        }
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func detailsSection(_ form: SubtitleCfgForm) -> some View {
        Section {
            SubtitleTextRow(title: L10n.tr("acq.sub.name"), text: form.name) { store.setFormName($0) }
            numberRow(form, "priority", "acq.sub.priority", hint: L10n.tr("acq.sub.priority.hint"))
            SubtitleTextRow(title: L10n.tr("acq.sub.address"), text: form.url, keyboard: .URL) { store.setFormUrl($0) }
        }
    }

    private func accountSection(_ form: SubtitleCfgForm, _ kind: SubtitleCfgKind) -> some View {
        Section {
            ForEach(kind.account.filter { SubtitleCfgKeys.accountFields.contains($0) }, id: \.self) { field in
                SubtitleTextRow(
                    title: SubtitleCfgWords.accountLabel(field),
                    text: form.account[field] ?? "",
                    secure: field != "username",
                    invalid: form.missing.contains(field)
                ) { store.setFormAccount(field, $0) }
            }
            if !form.missing.isEmpty {
                let fields = form.missing.map { SubtitleCfgWords.accountLabel($0).lowercased() }.joined(separator: ", ")
                Text(L10n.format("acq.sub.needs.fill", fields)).font(.footnote).foregroundStyle(Theme.failText)
            }
        } header: {
            Text(L10n.tr("acq.sub.your.account"))
        } footer: {
            Text(L10n.tr("acq.sub.account.note") + (!form.isNew && form.hasAccount ? " " + L10n.tr("acq.sub.account.keep") : ""))
        }
    }

    private func limitsSection(_ form: SubtitleCfgForm) -> some View {
        Section {
            DisclosureGroup(L10n.tr("acq.sub.limits"), isExpanded: $limits) {
                numberRow(form, "gap_ms", "acq.sub.gap", hint: nil)
                numberRow(form, "per_window", "acq.sub.per.window", hint: nil)
                numberRow(form, "window_seconds", "acq.sub.window", hint: nil)
                numberRow(form, "downloads_per_day", "acq.sub.per.day", hint: nil)
            }
        }
    }

    private func optionsSection(_ form: SubtitleCfgForm) -> some View {
        Section {
            SubtitleToggleRow(
                title: L10n.tr("acq.sub.local"),
                hint: L10n.tr("acq.sub.local.hint"),
                isOn: form.flags["allow_local_network"] ?? false
            ) { store.setFormFlag("allow_local_network", $0) }
            SubtitleToggleRow(
                title: L10n.tr("acq.sub.machine.made"),
                hint: L10n.tr("acq.sub.machine.made.hint"),
                isOn: form.flags["machine_made"] ?? false
            ) { store.setFormFlag("machine_made", $0) }
            SubtitleToggleRow(title: L10n.tr("acq.sub.on"), isOn: form.flags["enabled"] ?? false) { store.setFormFlag("enabled", $0) }
        }
    }

    private func numberRow(_ form: SubtitleCfgForm, _ key: String, _ title: String, hint: String?) -> some View {
        SubtitleNumberRow(
            title: L10n.tr(title),
            key: key,
            text: form.numbers[key] ?? "",
            valid: form.numberValid[key] ?? true,
            hint: hint
        ) { store.setFormNumber(key, $0) }
    }
}
