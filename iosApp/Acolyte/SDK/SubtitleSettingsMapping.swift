@preconcurrency import shared
import Foundation

extension Mapping {
    private static func subtitleCovers(_ movies: Bool, _ shows: Bool) -> SubtitleCfgCovers {
        if movies && shows { return .both }
        return movies ? .movies : .shows
    }

    private static func subtitleLanguages(_ list: [SubtitleLanguage]) -> [SubtitleCfgLanguage] {
        list.map { SubtitleCfgLanguage(code: $0.code, name: $0.name) }
    }

    private static func subtitleKind(_ k: SettingsSubtitleKind) -> SubtitleCfgKind {
        SubtitleCfgKind(
            kind: k.kind,
            name: k.name,
            note: k.note,
            needsAccount: k.needsAccount,
            account: k.account,
            covers: subtitleCovers(k.movies, k.shows)
        )
    }

    private static func subtitleSite(_ row: SettingsSubtitleSiteRow) -> SubtitleCfgSite {
        let ok: Bool? = row.testOk.map { $0.boolValue }
        return SubtitleCfgSite(
            id: row.id,
            name: row.name,
            url: row.url,
            kindName: row.kindName,
            offered: row.offered,
            covers: SubtitleCfgCovers(rawValue: key(row.covers)) ?? .both,
            priority: Int(row.priority),
            needsAccount: row.needsAccount,
            hasAccount: row.hasAccount,
            allowLocalNetwork: row.allowLocalNetwork,
            downloadsToday: Int(row.downloadsToday),
            downloadsMax: Int(row.downloadsMax),
            state: SubtitleCfgSiteState(rawValue: key(row.state)) ?? .on,
            turnedOff: row.turnedOff,
            pauseReason: row.pauseReason,
            pausedUntil: Format.date(iso: row.pausedUntil),
            testing: row.testing,
            testOk: ok,
            testMessage: row.testMessage,
            testExtra: row.testExtra,
            testError: error(row.testError)
        )
    }

    private static func subtitleTranslator(_ s: SettingsSubtitleState) -> SubtitleCfgTranslator {
        let form = s.translator
        let test = s.translatorTest
        return SubtitleCfgTranslator(
            kind: form.kind,
            url: form.url,
            model: form.model,
            key: form.key,
            clearKey: form.clearKey,
            builtInAuto: form.builtInAuto,
            hasKey: form.hasKey,
            dirty: s.translatorDirty,
            canSave: s.canSaveTranslator,
            canTest: s.canTestTranslator,
            saving: s.translatorSaving,
            testing: s.translatorTesting,
            error: error(s.translatorError),
            testOk: test?.ok,
            testMessage: test?.message ?? "",
            models: s.modelSuggestions
        )
    }

    private static func subtitleForm(_ s: SettingsSubtitleState, _ f: SettingsSubtitleSiteForm) -> SubtitleCfgForm {
        var numbers: [String: String] = [:]
        var valid: [String: Bool] = [:]
        for name in SubtitleCfgKeys.siteNumbers {
            numbers[name] = f.number(key: name)
            valid[name] = f.numberValid(key: name)
        }
        var account: [String: String] = [:]
        for name in SubtitleCfgKeys.accountFields {
            account[name] = f.account(field: name)
        }
        var flags: [String: Bool] = [:]
        for name in SubtitleCfgKeys.siteFlags {
            flags[name] = f.flag(key: name)
        }
        return SubtitleCfgForm(
            isNew: f.isNew,
            id: f.id,
            kind: f.kind,
            name: f.name,
            url: f.url,
            numbers: numbers,
            numberValid: valid,
            account: account,
            flags: flags,
            hasAccount: f.hasAccount,
            busy: f.busy,
            missing: f.missing,
            error: error(f.error),
            canSubmit: f.canSubmit
        )
    }

    static func subtitleSettings(_ s: SettingsSubtitleState) -> SubtitleCfgInfo {
        var numbers: [String: String] = [:]
        var numberValid: [String: Bool] = [:]
        for name in SubtitleCfgKeys.numbers {
            numbers[name] = s.number(key: name)
            numberValid[name] = s.numberValid(key: name)
        }
        var flags: [String: Bool] = [:]
        for name in SubtitleCfgKeys.flags {
            flags[name] = s.flag(key: name)
        }
        var steps: [String: Bool] = [:]
        for name in SubtitleCfgKeys.steps {
            steps[name] = s.step(key: name)
        }
        let wants = s.draft.languages.enumerated().map { index, want in
            SubtitleCfgWant(
                index: index,
                language: want.language,
                type: want.hearingImpaired,
                translateFrom: want.translateFrom,
                forced: want.forced
            )
        }
        var problem: SubtitleCfgProblem?
        if let kotlinProblem = s.wishProblem {
            problem = SubtitleCfgProblem(rawValue: key(kotlinProblem))
        }
        var note: SubtitleCfgNote?
        if let kotlinNote = s.note, let kind = SubtitleCfgNoteKind(rawValue: key(kotlinNote.kind)) {
            note = SubtitleCfgNote(id: Int(kotlinNote.id), kind: kind, name: kotlinNote.name, error: error(kotlinNote.error))
        }
        var form: SubtitleCfgForm?
        if let kotlinForm = s.form {
            form = subtitleForm(s, kotlinForm)
        }
        return SubtitleCfgInfo(
            load: load(s.status),
            allowed: s.allowed,
            canClean: s.canClean,
            dirty: s.dirty,
            canSave: s.canSave,
            saving: s.saving,
            saveError: error(s.saveError),
            wants: wants,
            wishProblem: problem,
            canAddLanguage: s.canAddLanguage,
            translatorReady: s.translatorReady,
            languages: subtitleLanguages(s.languages),
            numbers: numbers,
            numberValid: numberValid,
            flags: flags,
            hiSuffix: s.draft.hiSuffix,
            cleanRows: s.cleanRows,
            cleanAddable: subtitleLanguages(s.cleanAddable),
            cleanLanguage: s.cleanLanguage,
            steps: steps,
            translator: subtitleTranslator(s),
            kinds: s.offeredKinds.map { subtitleKind($0) },
            sites: s.siteRows.map { subtitleSite($0) },
            form: form,
            cleaning: s.cleaning,
            note: note
        )
    }
}
