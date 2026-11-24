import SwiftUI

struct SettingsServerAlertFormView: View {
    let store: SettingsServerAlertsStore

    private var form: SettingsServerFormInfo? { store.info.form }

    var body: some View {
        NavigationStack {
            Form {
                if let form {
                    nameSection(form)
                    kindSection(form)
                    ForEach(form.groups) { group in
                        momentSection(group)
                    }
                    switchSection(form)
                    if let error = form.error {
                        Section { SettingsServerErrorText(error: error) }
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { store.cancelForm() }
                        .disabled(form?.saving == true)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if form?.saving == true {
                        ProgressView()
                    } else {
                        Button(L10n.tr("settings.server.save")) { store.save() }
                            .disabled(form?.canSave != true)
                    }
                }
            }
        }
        .interactiveDismissDisabled(form?.saving == true)
    }

    private var title: String {
        guard let form else { return "" }
        return form.isNew ? L10n.tr("settings.server.form.add.title") : L10n.format("settings.server.form.change.title", form.name)
    }

    private func nameSection(_ form: SettingsServerFormInfo) -> some View {
        Section {
            SettingsServerField(
                title: L10n.tr("settings.server.form.name"),
                text: form.name,
                capitalize: true
            ) { store.setText(.alertName, $0) }
            if form.isNew {
                Picker(
                    L10n.tr("settings.server.form.kind"),
                    selection: Binding(get: { form.kind }, set: { store.setKind($0) })
                ) {
                    ForEach(SettingsServerAlertKindInfo.allCases) { kind in
                        Text(SettingsServerWords.pickerKind(kind)).tag(kind)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func kindSection(_ form: SettingsServerFormInfo) -> some View {
        switch form.kind {
        case .discord: discordSection(form)
        case .telegram: telegramSection(form)
        case .ntfy: ntfySection(form)
        case .gotify: gotifySection(form)
        case .pushover: pushoverSection(form)
        case .email: emailSection(form)
        case .webhook: webhookSection(form)
        }
    }

    private func stored() -> String { L10n.tr("settings.server.stored") }

    private func discordSection(_ form: SettingsServerFormInfo) -> some View {
        Section {
            SettingsServerField(
                title: L10n.tr("settings.server.discord.link"),
                text: form.url,
                hint: L10n.tr("settings.server.discord.link.hint"),
                placeholder: form.urlStored ? stored() : "https://discord.com/api/webhooks/…",
                mono: true,
                keyboard: .URL
            ) { store.setText(.url, $0) }
        }
    }

    private func telegramSection(_ form: SettingsServerFormInfo) -> some View {
        let tokenPlaceholder: String = form.isNew ? "123456789:AAH…" : (form.botTokenStored ? stored() : L10n.tr("settings.server.paste.bot"))
        let tokenHint = L10n.tr(form.isNew ? "settings.server.bot.token.new" : "settings.server.kept.keep")
        return Section {
            SettingsServerField(
                title: L10n.tr("settings.server.bot.token"),
                text: form.botToken,
                hint: tokenHint,
                placeholder: tokenPlaceholder,
                secret: true
            ) { store.setText(.botToken, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.chat.id"),
                text: form.chatId,
                hint: L10n.tr("settings.server.chat.id.hint"),
                placeholder: "-1001234567890, @mychannel",
                mono: true
            ) { store.setText(.chatId, $0) }
        }
    }

    private func ntfySection(_ form: SettingsServerFormInfo) -> some View {
        let tokenHint = L10n.tr(form.canClearToken ? "settings.server.kept.keep.server" : "settings.server.access.token.new")
        return Section {
            SettingsServerField(
                title: L10n.tr("settings.server.server.address"),
                text: form.serverUrl,
                hint: L10n.tr("settings.server.ntfy.server.hint"),
                placeholder: "https://ntfy.sh",
                mono: true,
                keyboard: .URL
            ) { store.setText(.serverUrl, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.topic"),
                text: form.topic,
                hint: L10n.tr("settings.server.topic.hint"),
                placeholder: "holyarrs-alerts",
                mono: true
            ) { store.setText(.topic, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.access.token"),
                text: form.appToken,
                hint: tokenHint,
                placeholder: form.canClearToken ? stored() : "tk_…",
                secret: true,
                enabled: !form.clearAppToken
            ) { store.setText(.appToken, $0) }
            if form.canClearToken {
                Toggle(isOn: Binding(get: { form.clearAppToken }, set: { store.setClearToken($0) })) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L10n.tr("settings.server.remove.token"))
                        Text(L10n.tr("settings.server.remove.token.hint"))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
        }
    }

    private func appTokenPlaceholder(_ form: SettingsServerFormInfo) -> String {
        if form.isNew { return "" }
        return form.appTokenStored ? stored() : L10n.tr("settings.server.paste.app")
    }

    private func gotifySection(_ form: SettingsServerFormInfo) -> some View {
        let tokenHint = L10n.tr(form.isNew ? "settings.server.gotify.token.new" : "settings.server.kept.keep.server")
        return Section {
            SettingsServerField(
                title: L10n.tr("settings.server.server.address"),
                text: form.serverUrl,
                hint: L10n.tr("settings.server.gotify.server.hint"),
                placeholder: "https://gotify.example.com",
                mono: true,
                keyboard: .URL
            ) { store.setText(.serverUrl, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.app.token"),
                text: form.appToken,
                hint: tokenHint,
                placeholder: appTokenPlaceholder(form),
                secret: true
            ) { store.setText(.appToken, $0) }
        }
    }

    private func pushoverSection(_ form: SettingsServerFormInfo) -> some View {
        let tokenHint = L10n.tr(form.isNew ? "settings.server.pushover.token.new" : "settings.server.kept.keep")
        return Section {
            SettingsServerField(
                title: L10n.tr("settings.server.user.key"),
                text: form.userKey,
                hint: L10n.tr("settings.server.user.key.hint"),
                mono: true
            ) { store.setText(.userKey, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.app.token"),
                text: form.appToken,
                hint: tokenHint,
                placeholder: appTokenPlaceholder(form),
                secret: true
            ) { store.setText(.appToken, $0) }
        }
    }

    private func emailSection(_ form: SettingsServerFormInfo) -> some View {
        Section {
            SettingsServerField(
                title: L10n.tr("settings.server.mail.server"),
                text: form.smtpHost,
                placeholder: "smtp.example.com",
                keyboard: .URL
            ) { store.setText(.smtpHost, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.port"),
                text: form.smtpPort,
                hint: form.portValid ? nil : L10n.tr("settings.server.port.invalid"),
                keyboard: .numberPad
            ) { store.setText(.smtpPort, $0) }
            Picker(
                L10n.tr("settings.server.security"),
                selection: Binding(get: { form.smtpSecurity }, set: { store.setText(.smtpSecurity, $0) })
            ) {
                Text(L10n.tr("settings.server.sec.starttls")).tag("starttls")
                Text(L10n.tr("settings.server.sec.tls")).tag("tls")
                Text(L10n.tr("settings.server.sec.none")).tag("none")
            }
            SettingsServerField(
                title: L10n.tr("settings.server.from.address"),
                text: form.smtpFrom,
                keyboard: .emailAddress
            ) { store.setText(.smtpFrom, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.mail.user"),
                text: form.smtpUser
            ) { store.setText(.smtpUser, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.mail.pass"),
                text: form.smtpPass,
                placeholder: form.passStored ? stored() : "",
                secret: true
            ) { store.setText(.smtpPass, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.staff"),
                text: form.staffTo,
                hint: L10n.tr("settings.server.staff.hint"),
                placeholder: "you@example.com, other@example.com",
                keyboard: .emailAddress
            ) { store.setText(.staffTo, $0) }
        }
    }

    private func webhookSection(_ form: SettingsServerFormInfo) -> some View {
        Section {
            SettingsServerField(
                title: L10n.tr("settings.server.hook.link"),
                text: form.url,
                hint: L10n.tr("settings.server.hook.link.hint"),
                placeholder: form.urlStored ? stored() : "https://example.com/holyarrs",
                mono: true,
                keyboard: .URL
            ) { store.setText(.url, $0) }
            SettingsServerField(
                title: L10n.tr("settings.server.secret"),
                text: form.secret,
                hint: L10n.tr("settings.server.secret.hint"),
                placeholder: form.secretStored ? stored() : "",
                secret: true
            ) { store.setText(.secret, $0) }
            Picker(
                L10n.tr("settings.server.secret.does"),
                selection: Binding(get: { form.webhookAuth }, set: { store.setText(.webhookAuth, $0) })
            ) {
                Text(L10n.tr("settings.server.auth.sign")).tag("sign")
                Text(L10n.tr("settings.server.auth.bearer")).tag("bearer")
                Text(L10n.tr("settings.server.auth.none")).tag("none")
            }
        }
    }

    private func momentSection(_ group: SettingsServerMomentGroupInfo) -> some View {
        let requests = group.key == "requests"
        let name = L10n.tr(requests ? "settings.server.group.requests" : "settings.server.group.library")
        let hint = L10n.tr(requests ? "settings.server.group.requests.hint" : "settings.server.group.library.hint")
        return Section {
            Button(L10n.tr(group.allOn ? "settings.server.moments.none" : "settings.server.moments.all")) {
                store.setGroup(group.key, !group.allOn)
            }
            ForEach(group.moments) { moment in
                Toggle(isOn: Binding(get: { moment.on }, set: { store.setMoment(moment.id, $0) })) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(SettingsServerWords.momentTitle(moment.id))
                        Text(SettingsServerWords.momentHint(moment.id))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
        } header: {
            Text(L10n.format("settings.server.when.to.send", name))
        } footer: {
            Text(hint)
        }
    }

    private func switchSection(_ form: SettingsServerFormInfo) -> some View {
        Section {
            Toggle(isOn: Binding(get: { form.allowLocalNetwork }, set: { store.setAllowLocalNetwork($0) })) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("settings.server.local"))
                    Text(L10n.tr("settings.server.local.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            Toggle(L10n.tr("settings.server.send"), isOn: Binding(get: { form.enabled }, set: { store.setEnabled($0) }))
        }
    }
}
