import Foundation
import SwiftUI
import UIKit

enum SubtitleCfgWords {
    static func error(_ e: ErrorInfo) -> String {
        if (e.kind == .badRequest || e.kind == .conflict) && !e.message.isEmpty { return e.message }
        return e.localized
    }

    static func type(_ type: String) -> String {
        switch type {
        case "hi": L10n.tr("acq.sub.type.hi")
        case "either": L10n.tr("acq.sub.type.either")
        default: L10n.tr("acq.sub.type.normal")
        }
    }

    static func problem(_ problem: SubtitleCfgProblem) -> String {
        switch problem {
        case .tooMany: L10n.tr("acq.sub.problem.too.many")
        case .pick: L10n.tr("acq.sub.problem.pick")
        case .twice: L10n.tr("acq.sub.problem.twice")
        }
    }

    static func covers(_ covers: SubtitleCfgCovers) -> String {
        switch covers {
        case .both: L10n.tr("acq.sub.covers.both")
        case .movies: L10n.tr("acq.sub.covers.movies")
        case .shows: L10n.tr("acq.sub.covers.shows")
        }
    }

    static func accountLabel(_ field: String) -> String {
        switch field {
        case "username": L10n.tr("acq.sub.field.username")
        case "password": L10n.tr("acq.sub.field.password")
        default: L10n.tr("acq.sub.field.api.key")
        }
    }

    static func stateLabel(_ state: SubtitleCfgSiteState) -> String {
        switch state {
        case .needsAccount: L10n.tr("acq.sub.state.needs")
        case .paused: L10n.tr("acq.sub.state.paused")
        case .resting: L10n.tr("acq.sub.state.resting")
        case .on: L10n.tr("acq.sub.state.on")
        }
    }

    static func stateTone(_ state: SubtitleCfgSiteState) -> ToneKind {
        switch state {
        case .needsAccount: .fail
        case .paused: .idle
        case .resting: .wait
        case .on: .ok
        }
    }

    private static func why(_ reason: String) -> String {
        switch reason {
        case "down": L10n.tr("acq.sub.pause.down")
        case "rate_limited": L10n.tr("acq.sub.pause.rate.limited")
        case "download_limit": L10n.tr("acq.sub.pause.download.limit")
        case "login_refused": L10n.tr("acq.sub.pause.login.refused")
        case "blocked": L10n.tr("acq.sub.pause.blocked")
        case "unreadable": L10n.tr("acq.sub.pause.unreadable")
        default: L10n.tr("acq.sub.pause.failed")
        }
    }

    static func stateText(_ site: SubtitleCfgSite) -> String {
        switch site.state {
        case .needsAccount:
            return L10n.tr("acq.sub.state.needs.text")
        case .paused:
            return L10n.tr("acq.sub.state.paused.text")
        case .on:
            return L10n.tr("acq.sub.state.on.text")
        case .resting:
            let reason = why(site.pauseReason)
            guard let until = site.pausedUntil else { return L10n.format("acq.sub.state.resting.text", reason) }
            let when = until.formatted(date: .abbreviated, time: .shortened)
            return L10n.format("acq.sub.state.resting.until", when, reason)
        }
    }

    static func siteLine(_ site: SubtitleCfgSite) -> String {
        var parts = [site.kindName, covers(site.covers), L10n.format("acq.sub.priority.n", site.priority)]
        if site.needsAccount {
            parts.append(L10n.tr(site.hasAccount ? "acq.sub.account.saved" : "acq.sub.account.not.set"))
        }
        if site.allowLocalNetwork { parts.append(L10n.tr("acq.sub.may.reach")) }
        if site.downloadsMax > 0 { parts.append(L10n.format("acq.sub.downloads.today", site.downloadsToday, site.downloadsMax)) }
        return parts.joined(separator: " · ")
    }

    static func note(_ note: SubtitleCfgNote) -> String {
        switch note.kind {
        case .settingsSaved: L10n.tr("acq.sub.note.settings.saved")
        case .translatorSaved: L10n.tr("acq.sub.note.translator.saved")
        case .siteSaved: L10n.format("acq.sub.note.site.saved", note.name)
        case .sitePaused: L10n.format("acq.sub.note.site.paused", note.name)
        case .siteResumed: L10n.format("acq.sub.note.site.resumed", note.name)
        case .pauseFailed: withError(L10n.tr("acq.sub.note.pause.failed"), note.error)
        case .resumeFailed: withError(L10n.tr("acq.sub.note.resume.failed"), note.error)
        case .removeFailed: withError(L10n.tr("acq.sub.note.remove.failed"), note.error)
        case .cleanQueued: L10n.tr("acq.sub.note.clean.queued")
        case .cleanFailed: withError(L10n.tr("acq.sub.note.clean.failed"), note.error)
        }
    }

    private static func withError(_ head: String, _ error: ErrorInfo?) -> String {
        guard let error else { return head }
        return "\(head): \(self.error(error))"
    }

    static func rangeText(_ key: String) -> String? {
        guard let range = SubtitleCfgKeys.ranges[key] else { return nil }
        return L10n.format("acq.sub.range", range.lowerBound, range.upperBound)
    }
}

struct SubtitleTextRow: View {
    let title: String
    let text: String
    var secure = false
    var keyboard: UIKeyboardType = .default
    var prompt: String?
    var invalid = false
    let onChange: (String) -> Void
    @State private var value: String

    init(
        title: String,
        text: String,
        secure: Bool = false,
        keyboard: UIKeyboardType = .default,
        prompt: String? = nil,
        invalid: Bool = false,
        onChange: @escaping (String) -> Void
    ) {
        self.title = title
        self.text = text
        self.secure = secure
        self.keyboard = keyboard
        self.prompt = prompt
        self.invalid = invalid
        self.onChange = onChange
        _value = State(initialValue: text)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(invalid ? Theme.failText : Theme.textMuted)
            field
                .keyboardType(keyboard)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }

    @ViewBuilder
    private var field: some View {
        if secure {
            SecureField(title, text: $value, prompt: prompt.map { Text($0) })
        } else {
            TextField(title, text: $value, prompt: prompt.map { Text($0) })
        }
    }
}

struct SubtitleNumberRow: View {
    let title: String
    let key: String
    let text: String
    let valid: Bool
    var hint: String?
    var decimal = false
    let onChange: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            SubtitleTextRow(
                title: title,
                text: text,
                keyboard: decimal ? .decimalPad : .numberPad,
                invalid: !valid,
                onChange: onChange
            )
            if !valid, let range = SubtitleCfgWords.rangeText(key) {
                Text(range).font(.footnote).foregroundStyle(Theme.failText)
            } else if let hint {
                Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }
}

struct SubtitleToggleRow: View {
    let title: String
    var hint: String?
    let isOn: Bool
    var enabled = true
    let onChange: (Bool) -> Void

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: { onChange($0) })) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                if let hint {
                    Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
        }
        .disabled(!enabled)
    }
}
