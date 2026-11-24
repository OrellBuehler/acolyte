import Foundation

enum QualityWords {
    static func step(_ id: String) -> String {
        switch id {
        case "cam": L10n.tr("acq.q.step.cam")
        case "telesync": L10n.tr("acq.q.step.telesync")
        case "sd_tv": L10n.tr("acq.q.step.sd.tv")
        case "sd_dvd": L10n.tr("acq.q.step.sd.dvd")
        case "720p_tv": L10n.tr("acq.q.step.720p.tv")
        case "720p_web": L10n.tr("acq.q.step.720p.web")
        case "720p_bluray": L10n.tr("acq.q.step.720p.bluray")
        case "1080p_tv": L10n.tr("acq.q.step.1080p.tv")
        case "1080p_web": L10n.tr("acq.q.step.1080p.web")
        case "1080p_bluray": L10n.tr("acq.q.step.1080p.bluray")
        case "1080p_remux": L10n.tr("acq.q.step.1080p.remux")
        case "2160p_web": L10n.tr("acq.q.step.2160p.web")
        case "2160p_bluray": L10n.tr("acq.q.step.2160p.bluray")
        case "2160p_remux": L10n.tr("acq.q.step.2160p.remux")
        case "lossy_low": L10n.tr("acq.q.step.lossy.low")
        case "lossy_mid": L10n.tr("acq.q.step.lossy.mid")
        case "lossy_high": L10n.tr("acq.q.step.lossy.high")
        case "lossless": L10n.tr("acq.q.step.lossless")
        case "lossless_hires": L10n.tr("acq.q.step.lossless.hires")
        default: id
        }
    }

    static func trait(_ id: String) -> String {
        switch id {
        case "hevc": L10n.tr("acq.q.trait.hevc")
        case "ddp_audio": L10n.tr("acq.q.trait.ddp.audio")
        case "web_dl": L10n.tr("acq.q.trait.web.dl")
        case "upscaled": L10n.tr("acq.q.trait.upscaled")
        case "dubbed_only": L10n.tr("acq.q.trait.dubbed.only")
        case "hardcoded_subs": L10n.tr("acq.q.trait.hardcoded.subs")
        case "3d": L10n.tr("acq.q.trait.3d")
        case "cam": L10n.tr("acq.q.trait.cam")
        case "telesync": L10n.tr("acq.q.trait.telesync")
        case "remux": L10n.tr("acq.q.trait.remux")
        case "av1": L10n.tr("acq.q.trait.av1")
        case "dv": L10n.tr("acq.q.trait.dv")
        case "hdr10": L10n.tr("acq.q.trait.hdr10")
        case "hdr10plus": L10n.tr("acq.q.trait.hdr10plus")
        case "atmos": L10n.tr("acq.q.trait.atmos")
        case "h264": L10n.tr("acq.q.trait.h264")
        case "dts_audio": L10n.tr("acq.q.trait.dts.audio")
        case "truehd_audio": L10n.tr("acq.q.trait.truehd.audio")
        case "dv_no_fallback": L10n.tr("acq.q.trait.dv.no.fallback")
        case "cd_source": L10n.tr("acq.q.trait.cd.source")
        case "web_source": L10n.tr("acq.q.trait.web.source")
        case "vinyl_source": L10n.tr("acq.q.trait.vinyl.source")
        case "log_cue": L10n.tr("acq.q.trait.log.cue")
        case "hires": L10n.tr("acq.q.trait.hires")
        case "mp3": L10n.tr("acq.q.trait.mp3")
        case "aac": L10n.tr("acq.q.trait.aac")
        case "opus": L10n.tr("acq.q.trait.opus")
        case "vorbis": L10n.tr("acq.q.trait.vorbis")
        case "flac": L10n.tr("acq.q.trait.flac")
        case "alac": L10n.tr("acq.q.trait.alac")
        case "single_image": L10n.tr("acq.q.trait.single.image")
        case "lossy_transcode": L10n.tr("acq.q.trait.lossy.transcode")
        case "deluxe": L10n.tr("acq.q.trait.deluxe")
        case "sample": L10n.tr("acq.q.trait.sample")
        case "executable": L10n.tr("acq.q.trait.executable")
        case "password_protected": L10n.tr("acq.q.trait.password.protected")
        case "disc_image": L10n.tr("acq.q.trait.disc.image")
        default: id
        }
    }

    static func language(_ code: String) -> String {
        switch code {
        case "en": L10n.tr("acq.q.lang.en")
        case "fr": L10n.tr("acq.q.lang.fr")
        case "de": L10n.tr("acq.q.lang.de")
        case "it": L10n.tr("acq.q.lang.it")
        case "es": L10n.tr("acq.q.lang.es")
        case "ru": L10n.tr("acq.q.lang.ru")
        case "ja": L10n.tr("acq.q.lang.ja")
        case "ko": L10n.tr("acq.q.lang.ko")
        case "pl": L10n.tr("acq.q.lang.pl")
        case "nl": L10n.tr("acq.q.lang.nl")
        case "pt": L10n.tr("acq.q.lang.pt")
        case "sv": L10n.tr("acq.q.lang.sv")
        case "da": L10n.tr("acq.q.lang.da")
        case "no": L10n.tr("acq.q.lang.no")
        case "fi": L10n.tr("acq.q.lang.fi")
        case "cs": L10n.tr("acq.q.lang.cs")
        case "hu": L10n.tr("acq.q.lang.hu")
        case "el": L10n.tr("acq.q.lang.el")
        case "he": L10n.tr("acq.q.lang.he")
        case "tr": L10n.tr("acq.q.lang.tr")
        case "ar": L10n.tr("acq.q.lang.ar")
        case "hi": L10n.tr("acq.q.lang.hi")
        case "zh": L10n.tr("acq.q.lang.zh")
        case "th": L10n.tr("acq.q.lang.th")
        case "vi": L10n.tr("acq.q.lang.vi")
        case "uk": L10n.tr("acq.q.lang.uk")
        default: code
        }
    }

    static func device(_ id: String) -> String {
        switch id {
        case "apple_tv": L10n.tr("acq.q.dev.apple.tv")
        case "fire_tv": L10n.tr("acq.q.dev.fire.tv")
        case "roku": L10n.tr("acq.q.dev.roku")
        case "chromecast": L10n.tr("acq.q.dev.chromecast")
        case "web_browser": L10n.tr("acq.q.dev.web.browser")
        default: id
        }
    }

    static func kind(_ id: String) -> String {
        switch id {
        case "movie": L10n.tr("acq.q.kind.movie")
        case "show": L10n.tr("acq.q.kind.show")
        case "music": L10n.tr("acq.q.kind.music")
        default: id
        }
    }

    static func order(_ id: String) -> String {
        switch id {
        case "quality_then_points": L10n.tr("acq.q.order.quality.then.points")
        case "points_then_quality": L10n.tr("acq.q.order.points.then.quality")
        default: id
        }
    }

    static func orderNote(_ id: String) -> String {
        switch id {
        case "quality_then_points": L10n.tr("acq.q.order.quality.then.points.desc")
        case "points_then_quality": L10n.tr("acq.q.order.points.then.quality.desc")
        default: ""
        }
    }

    static func properMode(_ id: String) -> String {
        switch id {
        case "take_any": L10n.tr("acq.q.proper.take.any")
        case "same_group_only": L10n.tr("acq.q.proper.same.group.only")
        case "prefer_only": L10n.tr("acq.q.proper.prefer.only")
        case "ignore": L10n.tr("acq.q.proper.ignore")
        default: id
        }
    }

    static func properNote(_ id: String) -> String {
        switch id {
        case "take_any": L10n.tr("acq.q.proper.take.any.desc")
        case "same_group_only": L10n.tr("acq.q.proper.same.group.only.desc")
        case "prefer_only": L10n.tr("acq.q.proper.prefer.only.desc")
        case "ignore": L10n.tr("acq.q.proper.ignore.desc")
        default: ""
        }
    }

    static func prefer(_ id: String) -> String {
        switch id {
        case "either": L10n.tr("acq.q.prefer.either")
        case "torrent": L10n.tr("acq.q.prefer.torrent")
        case "usenet": L10n.tr("acq.q.prefer.usenet")
        default: id
        }
    }

    static func verdict(_ id: String) -> String {
        switch id {
        case "would_choose": L10n.tr("acq.q.verdict.would.choose")
        case "would_hold": L10n.tr("acq.q.verdict.would.hold")
        case "refused": L10n.tr("acq.q.verdict.refused")
        case "passed_over": L10n.tr("acq.q.verdict.passed.over")
        case "turned_down": L10n.tr("acq.q.verdict.turned.down")
        default: L10n.tr("acq.q.verdict.judged")
        }
    }

    static func traitKind(_ kind: String) -> String {
        switch kind {
        case "name": L10n.tr("acq.q.ot.kind.name")
        case "group": L10n.tr("acq.q.ot.kind.group")
        case "source": L10n.tr("acq.q.ot.kind.source")
        case "resolution": L10n.tr("acq.q.ot.kind.resolution")
        case "size": L10n.tr("acq.q.ot.kind.size")
        case "edition": L10n.tr("acq.q.ot.kind.edition")
        case "language": L10n.tr("acq.q.ot.kind.language")
        case "release_type": L10n.tr("acq.q.ot.kind.release.type")
        case "year": L10n.tr("acq.q.ot.kind.year")
        default: kind
        }
    }

    static func matchTitle(_ match: String) -> String {
        switch match {
        case "group": L10n.tr("acq.q.ot.match.group")
        case "size": L10n.tr("acq.q.ot.match.size")
        default: L10n.tr("acq.q.ot.match.name")
        }
    }

    static func matchHint(_ match: String) -> String {
        switch match {
        case "group": L10n.tr("acq.q.ot.hint.group")
        case "size": L10n.tr("acq.q.ot.hint.size")
        default: L10n.tr("acq.q.ot.hint.name")
        }
    }

    static func appName(_ app: String) -> String {
        switch app {
        case "sonarr": "Sonarr"
        case "radarr": "Radarr"
        default: app
        }
    }

    static func trashApp(_ app: String) -> String {
        switch app {
        case "radarr": L10n.tr("acq.q.e.trash.movies")
        default: L10n.tr("acq.q.e.trash.shows")
        }
    }

    static func choiceLabel(_ choice: QualityTraitChoiceInfo) -> String {
        guard choice.own else { return trait(choice.id) }
        return choice.name.isEmpty ? L10n.tr("acq.q.e.removed.trait") : choice.name
    }

    static func traitLabel(_ id: String, mine: [QualityTraitChoiceInfo]) -> String {
        if let own = mine.first(where: { $0.id == id }) { return choiceLabel(own) }
        if id.hasPrefix("own:") { return L10n.tr("acq.q.e.removed.trait") }
        return trait(id)
    }

    static func errorText(_ error: ErrorInfo) -> String {
        if error.kind == .unknown, !error.message.isEmpty { return error.message }
        return error.localized
    }

    static func joinWords(_ items: [String]) -> String {
        guard items.count > 1 else { return items.first ?? "" }
        let head = items.dropLast().joined(separator: ", ")
        return L10n.format("acq.q.and.join", head, items[items.count - 1])
    }

    static func usePart(_ part: QualityUsePart) -> String {
        switch part.key {
        case "versions": L10n.plural("acq.q.use.versions", part.count)
        case "library_folders": L10n.plural("acq.q.use.library.folders", part.count)
        case "outside_lists": L10n.plural("acq.q.use.outside.lists", part.count)
        case "wishlist": L10n.plural("acq.q.use.wishlist", part.count)
        case "title_defaults": L10n.plural("acq.q.use.title.defaults", part.count)
        default: String(part.count)
        }
    }

    static func useWords(_ use: QualityUseInfo) -> String {
        L10n.format("acq.q.used.by", joinWords(use.parts.map { usePart($0) }))
    }

    static func usedLine(_ use: QualityUseInfo) -> String {
        if use.parts.isEmpty { return L10n.tr("acq.q.not.used") }
        return L10n.format("acq.q.e.used.line", useWords(use))
    }

    static func ruleLine(_ row: QualityRuleRow) -> String {
        let kinds = row.kinds.isEmpty ? L10n.tr("acq.q.l.every.kind") : row.kinds.map { kind($0) }.joined(separator: ", ")
        let used = row.inUse == 0 ? L10n.tr("acq.q.not.used") : L10n.plural("acq.q.used.in", row.inUse)
        return kinds + " \u{00B7} " + used
    }

    static func sizeRange(_ row: QualityTraitRow) -> String {
        var parts: [String] = []
        if !row.over.isEmpty { parts.append(L10n.format("acq.q.ot.size.over", row.over)) }
        if !row.upTo.isEmpty { parts.append(L10n.format("acq.q.ot.size.up.to", row.upTo)) }
        return parts.joined(separator: ", ")
    }

    static func traitSummary(_ row: QualityTraitRow) -> String {
        switch row.match {
        case "group": L10n.format("acq.q.ot.sum.group", row.terms.joined(separator: ", "))
        case "size": L10n.format("acq.q.ot.sum.size", sizeRange(row))
        default: L10n.format("acq.q.ot.sum.name", row.terms.joined(separator: ", "))
        }
    }

    static func specText(_ spec: QualityTraitSpecInfo) -> String {
        let word = traitKind(spec.kind)
        if spec.negate { return L10n.format("acq.q.ot.spec.not", spec.name, word) }
        return L10n.format("acq.q.ot.spec", spec.name, word)
    }

    static func specLine(_ row: QualityTraitRow) -> String {
        let specs = row.specs.map { specText($0) }.joined(separator: ", ")
        guard row.source == "import" || row.source == "arrapi", row.app == "sonarr" || row.app == "radarr" else { return specs }
        return L10n.format("acq.q.ot.from.app", appName(row.app)) + " \u{00B7} " + specs
    }

    static func notice(_ notice: QualityNoticeInfo) -> String {
        switch notice.kind {
        case .removed:
            return L10n.format("acq.q.l.removed", notice.name)
        case .traitMade:
            return L10n.format("acq.q.ot.made", notice.name) + " \u{00B7} " + L10n.tr("acq.q.ot.ready")
        case .traitSaved:
            return L10n.format("acq.q.ot.saved", notice.name)
        case .traitRemoved:
            let head = L10n.format("acq.q.ot.removed", notice.name)
            if notice.count > 0 { return head + " \u{00B7} " + L10n.plural("acq.q.ot.out.of", notice.count) }
            return head
        case .made:
            return L10n.format("acq.q.e.made", notice.name)
        case .saved:
            return L10n.tr("acq.q.e.saved")
        case .applied:
            let name = notice.name.isEmpty ? L10n.tr("acq.q.e.trash.profile.fallback") : notice.name
            let head = L10n.format("acq.q.e.trash.applied", name)
            return head + " \u{00B7} " + L10n.format("acq.q.e.trash.scored", notice.count)
        }
    }

    static func problem(_ problem: QualityProblem) -> String {
        switch problem.code {
        case "server": problem.arg
        case "rules_name": L10n.tr("acq.q.err.rules.name")
        case "name_200": L10n.tr("acq.q.err.name.200")
        case "name_60": L10n.tr("acq.q.err.name.60")
        case "kinds": L10n.tr("acq.q.err.kinds")
        case "kind_once": L10n.tr("acq.q.err.kind.once")
        case "music_alone": L10n.tr("acq.q.err.music.alone")
        case "steps_other": L10n.tr("acq.q.err.steps.other")
        case "step_once": L10n.tr("acq.q.err.step.once")
        case "step_on": L10n.tr("acq.q.err.step.on")
        case "good_enough": L10n.tr("acq.q.err.good.enough")
        case "trait_once": L10n.tr("acq.q.err.trait.once")
        case "own_removed": L10n.tr("acq.q.err.own.removed")
        case "not_trait": L10n.format("acq.q.err.not.trait", problem.arg)
        case "points": L10n.tr("acq.q.err.points")
        case "size_step": L10n.tr("acq.q.err.size.step")
        case "size_neg": L10n.tr("acq.q.err.size.neg")
        case "size_order": L10n.tr("acq.q.err.size.order")
        case "size_big": L10n.tr("acq.q.err.size.big")
        case "size_preferred": L10n.tr("acq.q.err.size.preferred")
        case "wait": L10n.tr("acq.q.err.wait")
        case "lang_unknown": L10n.format("acq.q.err.lang.unknown", problem.arg)
        case "lang_once": L10n.tr("acq.q.err.lang.once")
        case "lang_wait": L10n.tr("acq.q.err.lang.wait")
        case "days": L10n.tr("acq.q.err.days")
        case "trait_name": L10n.tr("acq.q.err.trait.name")
        case "terms_min": L10n.tr("acq.q.err.terms.min")
        case "terms_50": L10n.tr("acq.q.err.terms.50")
        case "term_200": L10n.tr("acq.q.err.term.200")
        case "size_range": L10n.tr("acq.q.err.size.range")
        case "cannot_be_must": L10n.format("acq.q.err.cannot.be.must", trait(problem.arg))
        default: problem.arg.isEmpty ? L10n.tr("error.unknown") : problem.arg
        }
    }

    static func filterText(_ text: String, _ kind: QualityFieldKind) -> String {
        switch kind {
        case .text(let limit):
            return String(text.prefix(limit))
        case .decimal:
            var dot = false
            var out = ""
            for c in text {
                if c.isASCII, c.isNumber {
                    out.append(c)
                } else if c == "." || c == ",", !dot {
                    dot = true
                    out.append(c)
                }
            }
            return String(out.prefix(12))
        case .whole:
            return String(text.filter { $0.isASCII && $0.isNumber }.prefix(12))
        case .signed:
            var out = ""
            for (index, c) in text.enumerated() where (c.isASCII && c.isNumber) || (index == 0 && c == "-") {
                out.append(c)
            }
            return String(out.prefix(12))
        }
    }
}

enum QualityFieldKind: Hashable, Sendable {
    case text(Int)
    case decimal
    case whole
    case signed
}
