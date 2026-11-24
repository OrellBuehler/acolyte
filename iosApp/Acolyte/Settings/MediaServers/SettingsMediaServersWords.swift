import Foundation

enum SettingsMediaServersWords {
    static func language(code: String, fallback: String) -> String {
        if code == "off" { return L10n.tr("settings.mediaservers.tracks.off") }
        let fallbackName = fallback.isEmpty ? code : fallback
        guard let name = Locale.current.localizedString(forLanguageCode: code), !name.isEmpty else { return fallbackName }
        if name.lowercased() == code.lowercased() { return fallbackName }
        return name.prefix(1).localizedUppercase + name.dropFirst()
    }

    static func noticeText(_ note: SettingsMediaServersNoticeInfo) -> String {
        let head: String
        switch note.kind {
        case .saved: head = L10n.format("settings.mediaservers.saved", note.name)
        case .removeFailed: head = L10n.tr("settings.mediaservers.remove.failed")
        case .watchOn: head = L10n.tr("settings.mediaservers.watch.on.done")
        case .watchOff: head = L10n.tr("settings.mediaservers.watch.off.done")
        case .watchFailed: head = L10n.tr("settings.mediaservers.watch.failed")
        case .labelsSaved: head = L10n.tr("settings.mediaservers.labels.saved")
        case .labelsFailed: head = L10n.tr("settings.mediaservers.labels.failed")
        case .labelsStarted: head = L10n.tr("settings.mediaservers.labels.existing.started")
        case .labelsStartFailed: head = L10n.tr("settings.mediaservers.labels.existing.failed")
        case .tracksSaved: head = L10n.tr("settings.mediaservers.tracks.saved")
        case .tracksSavedOn: head = L10n.tr("settings.mediaservers.tracks.saved.on")
        case .tracksFailed: head = L10n.tr("settings.mediaservers.tracks.failed")
        }
        guard let error = note.error else { return head }
        return [head, error.localized, note.detail].filter { !$0.isEmpty }.joined(separator: "\n")
    }

    static func told(_ row: SettingsMediaServersRowInfo) -> String {
        var text: String
        if row.toldNothing {
            text = L10n.tr("settings.mediaservers.told.nothing")
        } else {
            var parts: [String] = []
            if row.onImport { parts.append(L10n.tr("settings.mediaservers.told.imports")) }
            if row.onRename { parts.append(L10n.tr("settings.mediaservers.told.renames")) }
            if row.onDelete { parts.append(L10n.tr("settings.mediaservers.told.deletes")) }
            text = L10n.format("settings.mediaservers.told.about", parts.joined(separator: ", "))
        }
        if row.hasPathMap {
            text += " · " + L10n.format("settings.mediaservers.path.map", row.pathFrom, row.pathTo)
        }
        return text
    }

    static func place(_ place: String) -> String {
        switch place {
        case "relay": L10n.tr("settings.mediaservers.plex.relay")
        case "local": L10n.tr("settings.mediaservers.plex.local")
        default: L10n.tr("settings.mediaservers.plex.remote")
        }
    }
}
