@preconcurrency import shared
import Foundation

extension Mapping {
    static func serverTest(_ t: SettingsServerTestResult?) -> SettingsServerTestInfo? {
        guard let t else { return nil }
        return SettingsServerTestInfo(kind: SettingsServerTestKindInfo(rawValue: key(t.kind)) ?? .failed, error: error(t.error))
    }

    static func serverAlert(_ a: SettingsServerAlertRow) -> SettingsServerAlertItem {
        SettingsServerAlertItem(
            id: a.id,
            name: a.name,
            kind: a.kind,
            enabled: a.enabled,
            moments: a.moments,
            missing: SettingsServerMissingInfo(rawValue: key(a.missing)) ?? .nothing,
            lastError: a.lastError,
            lastSent: Format.date(iso: a.lastSentAt),
            test: serverTest(a.test),
            testing: a.testing,
            editable: a.editable
        )
    }

    static func serverGroup(_ g: SettingsServerMomentGroup) -> SettingsServerMomentGroupInfo {
        SettingsServerMomentGroupInfo(
            key: g.key,
            moments: g.moments.map { SettingsServerMomentInfo(id: $0.id, on: $0.on) },
            allOn: g.allOn
        )
    }

    static func serverForm(_ f: SettingsServerAlertForm) -> SettingsServerFormInfo {
        SettingsServerFormInfo(
            id: f.id,
            isNew: f.isNew,
            kind: SettingsServerAlertKindInfo(rawValue: key(f.kind)) ?? .discord,
            name: f.name,
            enabled: f.enabled,
            allowLocalNetwork: f.allowLocalNetwork,
            url: f.url,
            botToken: f.botToken,
            chatId: f.chatId,
            serverUrl: f.serverUrl,
            topic: f.topic,
            userKey: f.userKey,
            appToken: f.appToken,
            secret: f.secret,
            webhookAuth: f.webhookAuth,
            smtpHost: f.smtpHost,
            smtpPort: f.smtpPort,
            smtpSecurity: f.smtpSecurity,
            smtpFrom: f.smtpFrom,
            smtpUser: f.smtpUser,
            smtpPass: f.smtpPass,
            staffTo: f.staffTo,
            clearAppToken: f.clearAppToken,
            canClearToken: f.canClearToken,
            urlStored: f.urlStored,
            botTokenStored: f.botTokenStored,
            appTokenStored: f.appTokenStored,
            secretStored: f.secretStored,
            passStored: f.passStored,
            portValid: f.portValid,
            groups: f.groups.map { serverGroup($0) },
            canSave: f.canSave,
            saving: f.saving,
            error: error(f.error)
        )
    }

    static func settingsAlerts(_ s: SettingsServerAlertsState) -> SettingsServerAlertsInfo {
        SettingsServerAlertsInfo(
            load: load(s.status),
            allowed: s.allowed,
            alerts: s.alerts.map { serverAlert($0) },
            form: s.form.map { serverForm($0) },
            pack: SettingsServerPackInfo(
                style: s.pack.style,
                loading: s.pack.loading,
                saving: s.pack.saving,
                error: error(s.pack.error)
            ),
            showPack: s.showPack,
            notice: s.notice.map {
                SettingsServerAlertsNoticeInfo(
                    seq: Int($0.seq),
                    kind: SettingsServerAlertsNoticeKindInfo(rawValue: key($0.kind)) ?? .saved,
                    name: $0.name,
                    error: error($0.error)
                )
            }
        )
    }

    static func serverFile(_ f: SettingsServerFile?) -> SettingsServerFileInfo? {
        guard let f else { return nil }
        return SettingsServerFileInfo(
            seq: Int(f.seq),
            kind: SettingsServerFileKindInfo(rawValue: key(f.kind)) ?? .backup,
            path: f.path,
            name: f.name,
            bytes: f.bytes
        )
    }

    static func settingsBackups(_ s: SettingsServerBackupsState) -> SettingsServerBackupsInfo {
        SettingsServerBackupsInfo(
            load: load(s.status),
            allowed: s.allowed,
            off: s.off,
            backups: s.backups.map {
                SettingsServerBackupItem(
                    name: $0.name,
                    kind: $0.kind,
                    createdAt: Format.date(iso: $0.createdAt),
                    size: $0.size,
                    detail: $0.detail,
                    busy: $0.busy
                )
            },
            preset: SettingsServerBackupPresetInfo(rawValue: key(s.preset)) ?? .balanced,
            presetSaving: s.presetSaving,
            restoreWaiting: s.restoreWaiting,
            keyNote: s.keyNote,
            showKeyNote: s.showKeyNote,
            keyKit: s.keyKit.map { SettingsServerKeyKitItem(needed: $0.needed, fingerprint: $0.fingerprint, warn: $0.warn) },
            canChange: s.canChange,
            isOwner: s.isOwner,
            canDownloadKit: s.canDownloadKit,
            keyKitNeedsSession: s.keyKitNeedsSession,
            making: s.making,
            keyKitBusy: s.keyKitBusy,
            restore: SettingsServerRestoreStageInfo(rawValue: key(s.restore)) ?? .idle,
            restoreName: s.restoreName,
            file: serverFile(s.file),
            notice: s.notice.map {
                SettingsServerBackupsNoticeInfo(
                    seq: Int($0.seq),
                    kind: SettingsServerBackupsNoticeKindInfo(rawValue: key($0.kind)) ?? .made,
                    name: $0.name,
                    error: error($0.error)
                )
            },
            reauth: settingsReauth(s.reauth)
        )
    }

    static func settingsLogs(_ s: SettingsServerLogsState) -> SettingsServerLogsInfo {
        let offset = Int(s.offset)
        return SettingsServerLogsInfo(
            load: load(s.status),
            allowed: s.allowed,
            filter: SettingsServerLogsFilterItem(
                level: s.filter.level,
                query: s.filter.query,
                from: s.filter.from,
                to: s.filter.to,
                active: s.filter.active
            ),
            lines: s.lines.enumerated().map { index, line in
                SettingsServerLogLineItem(
                    id: offset + index,
                    at: Format.date(iso: line.at),
                    level: line.level,
                    text: line.text,
                    tone: tone(line.tone)
                )
            },
            total: Int(s.total),
            rangeFrom: Int(s.rangeFrom),
            rangeTo: Int(s.rangeTo),
            hasNewer: s.hasNewer,
            hasOlder: s.hasOlder,
            noMatch: s.noMatch,
            busy: s.busy,
            noLogFile: s.noLogFile,
            downloading: s.downloading,
            downloadError: error(s.downloadError),
            file: serverFile(s.file),
            reauth: settingsReauth(s.reauth)
        )
    }

    static func kotlinServerKind(_ k: SettingsServerAlertKindInfo) -> SettingsServerAlertKind {
        switch k {
        case .discord: SettingsServerAlertKind.discord
        case .telegram: SettingsServerAlertKind.telegram
        case .ntfy: SettingsServerAlertKind.ntfy
        case .gotify: SettingsServerAlertKind.gotify
        case .pushover: SettingsServerAlertKind.pushover
        case .email: SettingsServerAlertKind.email
        case .webhook: SettingsServerAlertKind.webhook
        }
    }

    static func kotlinServerField(_ f: SettingsServerFieldInfo) -> SettingsServerAlertField {
        switch f {
        case .alertName: SettingsServerAlertField.alertName
        case .url: SettingsServerAlertField.url
        case .botToken: SettingsServerAlertField.botToken
        case .chatId: SettingsServerAlertField.chatId
        case .serverUrl: SettingsServerAlertField.serverUrl
        case .topic: SettingsServerAlertField.topic
        case .userKey: SettingsServerAlertField.userKey
        case .appToken: SettingsServerAlertField.appToken
        case .secret: SettingsServerAlertField.secret
        case .webhookAuth: SettingsServerAlertField.webhookAuth
        case .smtpHost: SettingsServerAlertField.smtpHost
        case .smtpPort: SettingsServerAlertField.smtpPort
        case .smtpSecurity: SettingsServerAlertField.smtpSecurity
        case .smtpFrom: SettingsServerAlertField.smtpFrom
        case .smtpUser: SettingsServerAlertField.smtpUser
        case .smtpPass: SettingsServerAlertField.smtpPass
        case .staffTo: SettingsServerAlertField.staffTo
        }
    }

    static func kotlinBackupPreset(_ p: SettingsServerBackupPresetInfo) -> SettingsServerBackupPreset {
        switch p {
        case .balanced: SettingsServerBackupPreset.balanced
        case .light: SettingsServerBackupPreset.light
        case .careful: SettingsServerBackupPreset.careful
        case .off: SettingsServerBackupPreset.off
        }
    }
}
