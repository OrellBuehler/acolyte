import SwiftUI

enum MusicText {
    static func mark(_ mark: MusicMarkKind) -> String {
        switch mark {
        case .none: L10n.tr("music.mark.none")
        case .complete: L10n.tr("music.mark.complete")
        case .partial: L10n.tr("music.mark.partial")
        case .downloading: L10n.tr("music.mark.downloading")
        case .missing: L10n.tr("music.mark.missing")
        case .requested: L10n.tr("music.mark.requested")
        case .approved: L10n.tr("music.mark.approved")
        }
    }

    static func tone(_ mark: MusicMarkKind) -> ToneKind {
        switch mark {
        case .none, .requested: .idle
        case .complete, .partial, .approved: .ok
        case .downloading: .info
        case .missing: .wait
        }
    }

    static func kind(_ kind: MusicAlbumKindID) -> String {
        switch kind {
        case .album: L10n.tr("music.kind.album")
        case .ep: L10n.tr("music.kind.ep")
        case .single: L10n.tr("music.kind.single")
        case .broadcast: L10n.tr("music.kind.broadcast")
        case .compilation: L10n.tr("music.kind.compilation")
        case .live: L10n.tr("music.kind.live")
        case .soundtrack: L10n.tr("music.kind.soundtrack")
        case .remix: L10n.tr("music.kind.remix")
        case .other: L10n.tr("music.kind.other")
        }
    }

    static func shortDay(_ iso: String) -> String {
        let parts = iso.prefix(10).split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let date = Calendar.current.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
        else { return iso }
        return date.formatted(.dateTime.month(.abbreviated).day())
    }

    static func line(_ line: MusicLineInfo) -> String {
        let tail: String
        switch line.kind {
        case .none, .artistText: tail = ""
        case .text: tail = line.text
        case .outOn: tail = L10n.format("music.line.out.on", shortDay(line.date))
        case .outNow: tail = L10n.tr("music.line.out.now")
        case .releasedOn: tail = L10n.format("music.line.released.on", shortDay(line.date))
        case .listens:
            let key = line.number == 1 ? "music.line.listens.one" : "music.line.listens.other"
            tail = L10n.format(key, MusicFormat.shortCount(line.number))
        case .arrivedOn: tail = L10n.format("music.line.arrived.on", shortDay(line.date))
        case .artistYear: tail = line.text
        case .asked: tail = L10n.tr("music.line.asked")
        case .approved: tail = L10n.tr("music.line.approved")
        case .turnedDown: tail = L10n.tr("music.line.turned.down")
        case .arrived: tail = L10n.tr("music.line.arrived")
        }
        return [line.artist, tail].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    static func note(_ note: MusicNoteInfo) -> String {
        switch note.kind {
        case .failed: note.error?.localized ?? ""
        case .stillReading: L10n.tr("music.note.still.reading")
        case .followFailed: L10n.tr("music.note.follow.failed")
        case .askedRequested: L10n.format("music.note.asked.requested", note.text)
        case .askedApproved: L10n.format("music.note.asked.approved", note.text)
        case .askedJoined: L10n.format("music.note.asked.joined", note.text)
        case .askedAlready: L10n.format("music.note.asked.already", note.text)
        case .added: L10n.format("music.note.added", note.text)
        case .alreadyInLibrary: L10n.format("music.note.already.in.library", note.text)
        case .notFound: L10n.format("music.note.not.found", note.text)
        case .keepAlbumOn, .keepArtistOn: L10n.format("music.note.keep.on", note.text)
        case .keepAlbumOff, .keepArtistOff: L10n.format("music.note.keep.off", note.text)
        case .searching: L10n.format("music.note.searching", note.text)
        case .searchingAll: L10n.tr("music.note.searching.all")
        case .kindsSaved: L10n.tr("music.note.kinds.saved")
        case .refreshing: L10n.format("music.note.refreshing", note.text)
        case .moving: L10n.format("music.note.moving", note.text)
        case .pressingChanged: L10n.tr("music.note.pressing.changed")
        case .pressingFailed: L10n.tr("music.note.pressing.failed")
        case .settingsSaved: L10n.tr("music.note.settings.saved")
        case .matched: L10n.plural("music.note.matched", note.count)
        case .notMatched: L10n.plural("music.note.not.matched", note.count)
        case .unmatched: L10n.tr("music.note.unmatched")
        }
    }

    static func problem(_ problem: MusicProblemKind, album: Bool) -> String {
        switch problem {
        case .offline: L10n.tr("music.problem.offline")
        case .notFound: L10n.tr(album ? "music.problem.not.found.album" : "music.problem.not.found.artist")
        case .badLink: L10n.tr(album ? "music.problem.bad.album" : "music.problem.bad.artist")
        case .forbidden: L10n.tr(album ? "music.problem.forbidden.album" : "music.problem.forbidden.artist")
        case .none, .slow: L10n.tr(album ? "music.problem.slow.album" : "music.problem.slow.artist")
        }
    }
}

extension Color {
    init(musicHex hex: String) {
        let clean = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        let value = UInt32(clean, radix: 16) ?? 0x4E6075
        self.init(hex: value)
    }

    init?(musicTint tint: Int64) {
        guard tint != 0 else { return nil }
        self.init(hex: UInt32(truncatingIfNeeded: tint))
    }
}
