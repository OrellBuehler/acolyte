import Foundation

enum SettingsServerWords {
    static func kindLabel(_ wire: String) -> String {
        switch wire {
        case "discord": L10n.tr("settings.server.kind.discord")
        case "telegram": L10n.tr("settings.server.kind.telegram")
        case "ntfy": L10n.tr("settings.server.kind.ntfy")
        case "gotify": L10n.tr("settings.server.kind.gotify")
        case "pushover": L10n.tr("settings.server.kind.pushover")
        case "email": L10n.tr("settings.server.kind.email")
        case "webhook": L10n.tr("settings.server.kind.webhook")
        default: wire
        }
    }

    static func pickerKind(_ kind: SettingsServerAlertKindInfo) -> String {
        switch kind {
        case .discord: L10n.tr("settings.server.kind.discord")
        case .telegram: L10n.tr("settings.server.kind.telegram")
        case .ntfy: L10n.tr("settings.server.kind.ntfy")
        case .gotify: L10n.tr("settings.server.kind.gotify")
        case .pushover: L10n.tr("settings.server.kind.pushover")
        case .email: L10n.tr("settings.server.kind.email")
        case .webhook: L10n.tr("settings.server.kind.webhook.json")
        }
    }

    static func missing(_ missing: SettingsServerMissingInfo) -> String {
        switch missing {
        case .nothing: ""
        case .bot: L10n.tr("settings.server.need.bot")
        case .botAndChat: L10n.tr("settings.server.need.bot.and.chat")
        case .chat: L10n.tr("settings.server.need.chat")
        case .topic: L10n.tr("settings.server.need.topic")
        case .app: L10n.tr("settings.server.need.app")
        case .server: L10n.tr("settings.server.need.server")
        case .serverAndApp: L10n.tr("settings.server.need.server.and.app")
        case .userAndApp: L10n.tr("settings.server.need.user.and.app")
        case .user: L10n.tr("settings.server.need.user")
        case .link: L10n.tr("settings.server.need.link")
        case .address: L10n.tr("settings.server.need.address")
        }
    }

    static func momentTitle(_ id: String) -> String {
        switch id {
        case "ask_waiting": L10n.tr("settings.server.moment.ask.waiting.title")
        case "ask_approved": L10n.tr("settings.server.moment.ask.approved.title")
        case "ask_turned_down": L10n.tr("settings.server.moment.ask.turned.down.title")
        case "ask_ready": L10n.tr("settings.server.moment.ask.ready.title")
        case "new_episodes": L10n.tr("settings.server.moment.new.episodes.title")
        case "new_albums": L10n.tr("settings.server.moment.new.albums.title")
        case "report_made": L10n.tr("settings.server.moment.report.made.title")
        case "report_fixed": L10n.tr("settings.server.moment.report.fixed.title")
        case "grab": L10n.tr("settings.server.moment.grab.title")
        case "downloaded": L10n.tr("settings.server.moment.downloaded.title")
        case "download_failed": L10n.tr("settings.server.moment.download.failed.title")
        case "import": L10n.tr("settings.server.moment.import.title")
        case "rename": L10n.tr("settings.server.moment.rename.title")
        case "add": L10n.tr("settings.server.moment.add.title")
        case "delete": L10n.tr("settings.server.moment.delete.title")
        case "health": L10n.tr("settings.server.moment.health.title")
        case "needs_decision": L10n.tr("settings.server.moment.needs.decision.title")
        case "plugin_message": L10n.tr("settings.server.moment.plugin.message.title")
        default: id
        }
    }

    static func momentHint(_ id: String) -> String {
        switch id {
        case "ask_waiting": L10n.tr("settings.server.moment.ask.waiting.hint")
        case "ask_approved": L10n.tr("settings.server.moment.ask.approved.hint")
        case "ask_turned_down": L10n.tr("settings.server.moment.ask.turned.down.hint")
        case "ask_ready": L10n.tr("settings.server.moment.ask.ready.hint")
        case "new_episodes": L10n.tr("settings.server.moment.new.episodes.hint")
        case "new_albums": L10n.tr("settings.server.moment.new.albums.hint")
        case "report_made": L10n.tr("settings.server.moment.report.made.hint")
        case "report_fixed": L10n.tr("settings.server.moment.report.fixed.hint")
        case "grab": L10n.tr("settings.server.moment.grab.hint")
        case "downloaded": L10n.tr("settings.server.moment.downloaded.hint")
        case "download_failed": L10n.tr("settings.server.moment.download.failed.hint")
        case "import": L10n.tr("settings.server.moment.import.hint")
        case "rename": L10n.tr("settings.server.moment.rename.hint")
        case "add": L10n.tr("settings.server.moment.add.hint")
        case "delete": L10n.tr("settings.server.moment.delete.hint")
        case "health": L10n.tr("settings.server.moment.health.hint")
        case "needs_decision": L10n.tr("settings.server.moment.needs.decision.hint")
        case "plugin_message": L10n.tr("settings.server.moment.plugin.message.hint")
        default: ""
        }
    }

    static func momentsLine(_ ids: [String]) -> String {
        ids.isEmpty ? L10n.tr("settings.server.alert.no.moments") : ids.map { momentTitle($0) }.joined(separator: " · ")
    }

    static func presetName(_ preset: SettingsServerBackupPresetInfo) -> String {
        switch preset {
        case .balanced: L10n.tr("settings.server.backups.balanced")
        case .light: L10n.tr("settings.server.backups.light")
        case .careful: L10n.tr("settings.server.backups.careful")
        case .off: L10n.tr("settings.server.backups.off.plan")
        }
    }

    static func presetAbout(_ preset: SettingsServerBackupPresetInfo) -> String {
        switch preset {
        case .balanced: L10n.tr("settings.server.backups.balanced.about")
        case .light: L10n.tr("settings.server.backups.light.about")
        case .careful: L10n.tr("settings.server.backups.careful.about")
        case .off: L10n.tr("settings.server.backups.off.about")
        }
    }

    static func backupKind(_ kind: String) -> String {
        switch kind {
        case "scheduled": L10n.tr("settings.server.backups.kind.scheduled")
        case "manual": L10n.tr("settings.server.backups.kind.manual")
        case "update": L10n.tr("settings.server.backups.kind.update")
        case "pre-update": L10n.tr("settings.server.backups.kind.pre.update")
        default: kind
        }
    }

    static func logLevel(_ level: String) -> String {
        switch level {
        case "error": L10n.tr("settings.server.logs.error")
        case "warning": L10n.tr("settings.server.logs.warning")
        default: L10n.tr("settings.server.logs.info")
        }
    }

    static func logLevelFilter(_ level: String) -> String {
        switch level {
        case "error": L10n.tr("settings.server.logs.errors")
        case "warning": L10n.tr("settings.server.logs.warnings")
        default: L10n.tr("settings.server.logs.every")
        }
    }

    static func showsSentence(_ error: ErrorInfo) -> Bool {
        let statuses: Set<Int> = [400, 409, 422, 500, 502, 503, 507]
        return !error.message.isEmpty && statuses.contains(error.status)
    }

    static func errorLine(_ error: ErrorInfo) -> String {
        showsSentence(error) ? "\(error.localized) \(error.message)" : error.localized
    }

    static func withError(_ title: String, _ error: ErrorInfo?) -> String {
        guard let error else { return title }
        return "\(title): \(errorLine(error))"
    }
}

enum SettingsServerDays {
    static func text(_ date: Date) -> String {
        formatter().string(from: date)
    }

    static func date(_ text: String) -> Date? {
        formatter().date(from: text)
    }

    private static func formatter() -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }
}

func settingsServerCopyPicked(_ url: URL, _ path: String) -> Bool {
    let scoped = url.startAccessingSecurityScopedResource()
    defer { if scoped { url.stopAccessingSecurityScopedResource() } }
    let target = URL(fileURLWithPath: path)
    try? FileManager.default.removeItem(at: target)
    do {
        try FileManager.default.copyItem(at: url, to: target)
        return true
    } catch {
        return false
    }
}
