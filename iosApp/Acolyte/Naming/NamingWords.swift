import Foundation

enum NamingText {
    static func error(_ error: ErrorInfo) -> String {
        switch error.kind {
        case .badRequest, .conflict, .unknown:
            return error.message.isEmpty ? error.localized : error.message
        default:
            return error.localized
        }
    }

    static func fieldLabel(_ field: NamingField) -> String {
        switch field {
        case .filePattern: L10n.tr("acq.nm.file.pattern")
        case .folderPattern: L10n.tr("acq.nm.folder.pattern")
        case .showFolderPattern: L10n.tr("acq.nm.show.folder")
        case .seasonFolderPattern: L10n.tr("acq.nm.season.pattern")
        case .specialsFolder: L10n.tr("acq.nm.specials")
        case .episodePattern: L10n.tr("acq.nm.episode.pattern")
        case .dailyPattern: L10n.tr("acq.nm.daily")
        case .animePattern: L10n.tr("acq.nm.anime")
        case .artistFolderPattern: L10n.tr("acq.nm.artist.folder")
        case .albumFolderPattern: L10n.tr("acq.nm.album.folder")
        case .trackPattern: L10n.tr("acq.nm.track.pattern")
        case .multiDiscTrackPattern: L10n.tr("acq.nm.multi.disc")
        case .discFolderPattern: L10n.tr("acq.nm.disc.pattern")
        }
    }

    static func numberError(_ key: String) -> String {
        switch key {
        case "days": L10n.tr("acq.nm.err.days")
        case "daysmax": L10n.tr("acq.nm.err.days.max")
        case "free": L10n.tr("acq.nm.err.free")
        case "freemax": L10n.tr("acq.nm.err.free.max")
        default: L10n.tr("acq.nm.err.range")
        }
    }

    static func exampleLabel(_ key: String) -> String {
        switch key {
        case "show_folder": L10n.tr("acq.nm.ex.show.folder")
        case "episode": L10n.tr("acq.nm.ex.episode")
        case "multi": L10n.tr("acq.nm.ex.multi")
        case "special": L10n.tr("acq.nm.ex.special")
        case "daily": L10n.tr("acq.nm.ex.daily")
        case "anime": L10n.tr("acq.nm.ex.anime")
        case "artist_folder": L10n.tr("acq.nm.ex.artist.folder")
        case "album_folder": L10n.tr("acq.nm.ex.album.folder")
        case "track": L10n.tr("acq.nm.ex.track")
        case "multi_disc": L10n.tr("acq.nm.ex.multi.disc")
        default: L10n.tr("acq.nm.ex.various")
        }
    }

    static let colonOptions = ["smart", "dash", "space_dash", "space_dash_space", "remove"]
    static let illegalOptions = ["replace", "remove"]
    static let multiOptions = ["range", "short_range", "each", "each_dash"]
    static let dateOptions = ["dash", "dot", "space"]
    static let digitOptions = ["2", "3", "4"]

    static func colonLabel(_ value: String) -> String {
        switch value {
        case "dash": L10n.tr("acq.nm.colon.dash")
        case "space_dash": L10n.tr("acq.nm.colon.space.dash")
        case "space_dash_space": L10n.tr("acq.nm.colon.space.dash.space")
        case "remove": L10n.tr("acq.nm.colon.remove")
        default: L10n.tr("acq.nm.colon.smart")
        }
    }

    static func illegalLabel(_ value: String) -> String {
        value == "remove" ? L10n.tr("acq.nm.illegal.remove") : L10n.tr("acq.nm.illegal.replace")
    }

    static func multiLabel(_ value: String) -> String {
        switch value {
        case "short_range": L10n.tr("acq.nm.multi.short.range")
        case "each": L10n.tr("acq.nm.multi.each")
        case "each_dash": L10n.tr("acq.nm.multi.each.dash")
        default: L10n.tr("acq.nm.multi.range")
        }
    }

    static func dateLabel(_ value: String) -> String {
        switch value {
        case "dot": L10n.tr("acq.nm.date.dot")
        case "space": L10n.tr("acq.nm.date.space")
        default: L10n.tr("acq.nm.date.dash")
        }
    }

    static func digitLabel(_ value: String) -> String {
        switch value {
        case "2": L10n.tr("acq.nm.digits.2")
        case "4": L10n.tr("acq.nm.digits.4")
        default: L10n.tr("acq.nm.digits.3")
        }
    }
}

enum NamingCards {
    static let movie: [NamingField] = [.filePattern, .folderPattern]
    static let show: [NamingField] = [.showFolderPattern, .seasonFolderPattern, .episodePattern, .dailyPattern, .animePattern]
    static let music: [NamingField] = [.artistFolderPattern, .albumFolderPattern, .trackPattern, .multiDiscTrackPattern, .discFolderPattern]
}
