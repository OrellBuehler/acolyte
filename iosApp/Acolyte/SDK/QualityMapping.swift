@preconcurrency import shared
import Foundation

extension Mapping {
    static func qualityProblem(_ p: SettingsPickRuleProblem) -> QualityProblem {
        QualityProblem(field: p.field, code: p.code, arg: p.arg)
    }

    static func qualityUse(_ u: SettingsPickRuleUse) -> QualityUseInfo {
        QualityUseInfo(parts: u.parts.map { QualityUsePart(key: $0.key, count: Int($0.count)) })
    }

    static func qualityNotice(_ n: SettingsQualityNotice?) -> QualityNoticeInfo? {
        guard let n, let kind = QualityNoticeKind(rawValue: n.kind) else { return nil }
        return QualityNoticeInfo(seq: Int(n.seq), kind: kind, name: n.name, count: Int(n.count))
    }

    static func qualityTraitRow(_ t: SettingsTraitRow) -> QualityTraitRow {
        QualityTraitRow(
            id: t.id,
            name: t.name,
            match: t.match,
            terms: t.terms,
            source: t.source,
            app: t.app,
            specs: t.specs.map { QualityTraitSpecInfo(name: $0.name, kind: $0.kind, negate: $0.negate) },
            over: t.over,
            upTo: t.upTo
        )
    }

    static func qualityCheck(_ c: SettingsNameCheckState) -> QualityCheckInfo {
        var result: QualityCheckResultInfo?
        if let r = c.result {
            result = QualityCheckResultInfo(
                title: r.title,
                year: Int(r.year),
                tags: r.tags,
                matchName: r.matchName,
                matchDetail: r.matchDetail,
                hint: r.hint,
                copies: r.copies.map { copy in
                    QualityCopyInfo(
                        header: copy.header,
                        rulesName: copy.rulesName,
                        showRules: copy.showRules,
                        verdict: copy.verdict,
                        tone: tone(copy.tone),
                        passed: copy.passed,
                        points: Int(copy.points),
                        hasFile: copy.hasFile,
                        reasons: copy.reasons.map { QualityReasonInfo(effect: $0.effect, text: $0.text) }
                    )
                }
            )
        }
        return QualityCheckInfo(
            name: c.name,
            rulesId: c.rulesId,
            size: c.size,
            busy: c.busy,
            error: error(c.error),
            result: result,
            canCheck: c.canCheck
        )
    }

    static func qualityList(_ s: SettingsPickRulesState) -> QualityListInfo {
        var sheet: QualityTraitSheetInfo?
        if let t = s.traitSheet {
            sheet = QualityTraitSheetInfo(
                id: t.id,
                name: t.name,
                match: t.match,
                terms: t.terms,
                over: t.over,
                upTo: t.upTo,
                busy: t.busy,
                problems: t.problems.map { qualityProblem($0) },
                formError: error(t.formError)
            )
        }
        var traitRemove: QualityTraitRemoveInfo?
        if let t = s.traitRemove {
            traitRemove = QualityTraitRemoveInfo(id: t.id, name: t.name, busy: t.busy, error: error(t.error))
        }
        var remove: QualityRemoveInfo?
        if let r = s.remove {
            var use: QualityUseInfo?
            if let u = r.use { use = qualityUse(u) }
            remove = QualityRemoveInfo(id: r.id, name: r.name, inUse: Int(r.inUse), busy: r.busy, error: error(r.error), use: use)
        }
        return QualityListInfo(
            load: load(s.status),
            allowed: s.allowed,
            mayEdit: s.mayEdit,
            rules: s.rules.map { row in
                QualityRuleRow(
                    id: row.id,
                    name: row.name,
                    kinds: row.kinds,
                    preset: row.preset,
                    inUse: Int(row.inUse),
                    readyChip: row.readyChip
                )
            },
            traitsLoaded: s.traitsLoaded,
            traitsError: error(s.traitsError),
            handMade: s.handMade.map { qualityTraitRow($0) },
            trashTraits: s.trashTraits.map { qualityTraitRow($0) },
            importedTraits: s.importedTraits.map { qualityTraitRow($0) },
            traitSheet: sheet,
            traitRemove: traitRemove,
            remove: remove,
            check: qualityCheck(s.check),
            notice: qualityNotice(s.notice),
            isEmpty: s.isEmpty,
            traitsEmpty: s.traitsEmpty
        )
    }

    static func qualityChoice(_ c: SettingsTraitChoice) -> QualityTraitChoiceInfo {
        QualityTraitChoiceInfo(id: c.id, own: c.own, name: c.name)
    }

    static func qualityForm(_ f: SettingsPickRuleForm) -> QualityFormInfo {
        var synced: QualitySyncedInfo?
        if let s = f.synced {
            synced = QualitySyncedInfo(name: s.name, app: s.app, profileId: s.profileId, at: Format.date(iso: s.at))
        }
        return QualityFormInfo(
            name: f.name,
            kinds: f.kinds,
            stepsBestFirst: f.stepsBestFirst.map { QualityStepInfo(quality: $0.quality, allowed: $0.allowed) },
            allowedBestFirst: f.allowedBestFirst.map { $0.quality },
            goodEnoughAt: f.goodEnoughAt,
            goodEnoughPoints: f.goodEnoughPoints,
            traits: f.traits.map { QualityTraitPointsInfo(trait: $0.trait, points: $0.points) },
            mustHave: f.mustHave,
            never: f.never,
            blockedGroups: f.blockedGroups,
            devices: f.devices,
            order: f.order,
            upgradeMinPoints: f.upgradeMinPoints,
            minPoints: f.minPoints,
            properMode: f.properMode,
            properAge: f.properAge,
            sizeMin: f.sizeMin,
            sizeMax: f.sizeMax,
            stepSizes: f.stepSizes.map { QualityStepSizeInfo(step: $0.step, min: $0.min, preferred: $0.preferred, max: $0.max) },
            sizeChoices: f.sizeChoices,
            replaceUnderMin: f.replaceUnderMin,
            prefer: f.prefer,
            waitTorrent: f.waitTorrent,
            waitUsenet: f.waitUsenet,
            skipWait: f.skipWait,
            tags: f.tags,
            languages: f.languages,
            frenchVersions: f.frenchVersions,
            originalOk: f.originalOk,
            languageWait: f.languageWait,
            music: f.music,
            frenchWanted: f.frenchWanted,
            synced: synced
        )
    }

    static func qualityTrash(_ t: SettingsTrashState) -> QualityTrashInfo {
        var result: QualityTrashResultInfo?
        if let r = t.result {
            result = QualityTrashResultInfo(
                scored: Int(r.scored),
                added: Int(r.added),
                updated: Int(r.updated),
                skipped: r.skipped.map { QualityTrashSkippedInfo(name: $0.name, kinds: $0.kinds, reason: $0.reason) }
            )
        }
        return QualityTrashInfo(
            app: t.app,
            profiles: t.profiles.map { QualityTrashProfileInfo(id: $0.id, name: $0.name) },
            profilesLoaded: t.profilesLoaded,
            pick: t.pick,
            pickNote: t.choiceNote,
            loading: t.loading,
            busy: t.busy,
            error: error(t.error),
            result: result
        )
    }

    static func qualityEdit(_ s: SettingsPickRuleEditState) -> QualityEditInfo {
        let count = s.form.traits.count
        var free: [[QualityTraitChoiceInfo]] = []
        for index in 0..<count {
            free.append(s.freeTraitChoices(index: Int32(index)).map { qualityChoice($0) })
        }
        var formUse: QualityUseInfo?
        if let u = s.formUse { formUse = qualityUse(u) }
        return QualityEditInfo(
            load: load(s.status),
            allowed: s.allowed,
            mayEdit: s.mayEdit,
            gone: s.gone,
            id: s.id,
            isNew: s.isNew,
            preset: s.preset,
            savedName: s.savedName,
            use: qualityUse(s.use),
            form: qualityForm(s.form),
            mine: s.mine.map { qualityChoice($0) },
            problems: s.problems.map { qualityProblem($0) },
            saving: s.saving,
            formError: error(s.formError),
            formUse: formUse,
            trash: qualityTrash(s.trash),
            notice: qualityNotice(s.notice),
            dirty: s.dirty,
            showSaveBar: s.showSaveBar,
            showTrash: s.showTrash,
            needsPermission: s.needsPermission,
            canAddTrait: s.canAddTrait,
            traitChoices: s.traitChoices.map { qualityChoice($0) },
            neverChoices: s.neverChoices.map { qualityChoice($0) },
            freeChoices: free
        )
    }
}
