import Foundation

struct QualityProblem: Hashable, Sendable {
    let field: String
    let code: String
    let arg: String
}

struct QualityUsePart: Hashable, Sendable, Identifiable {
    let key: String
    let count: Int

    var id: String { key }
}

struct QualityUseInfo: Hashable, Sendable {
    let parts: [QualityUsePart]

    static let none = QualityUseInfo(parts: [])
}

enum QualityNoticeKind: String, Hashable, Sendable {
    case removed
    case traitMade = "trait_made"
    case traitSaved = "trait_saved"
    case traitRemoved = "trait_removed"
    case made
    case saved
    case applied
}

struct QualityNoticeInfo: Hashable, Sendable {
    let seq: Int
    let kind: QualityNoticeKind
    let name: String
    let count: Int
}

struct QualityRuleRow: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let kinds: [String]
    let preset: Bool
    let inUse: Int
    let readyChip: Bool
}

struct QualityTraitSpecInfo: Hashable, Sendable {
    let name: String
    let kind: String
    let negate: Bool
}

struct QualityTraitRow: Hashable, Sendable, Identifiable {
    let id: Int64
    let name: String
    let match: String
    let terms: [String]
    let source: String
    let app: String
    let specs: [QualityTraitSpecInfo]
    let over: String
    let upTo: String
}

struct QualityTraitSheetInfo: Hashable, Sendable {
    let id: Int64
    let name: String
    let match: String
    let terms: String
    let over: String
    let upTo: String
    let busy: Bool
    let problems: [QualityProblem]
    let formError: ErrorInfo?

    func problem(_ field: String) -> QualityProblem? {
        problems.first { $0.field == field }
    }
}

struct QualityTraitRemoveInfo: Hashable, Sendable {
    let id: Int64
    let name: String
    let busy: Bool
    let error: ErrorInfo?
}

struct QualityRemoveInfo: Hashable, Sendable {
    let id: Int64
    let name: String
    let inUse: Int
    let busy: Bool
    let error: ErrorInfo?
    let use: QualityUseInfo?
}

struct QualityReasonInfo: Hashable, Sendable {
    let effect: String
    let text: String
}

struct QualityCopyInfo: Hashable, Sendable {
    let header: String
    let rulesName: String
    let showRules: Bool
    let verdict: String
    let tone: ToneKind
    let passed: Bool
    let points: Int
    let hasFile: Bool
    let reasons: [QualityReasonInfo]
}

struct QualityCheckResultInfo: Hashable, Sendable {
    let title: String
    let year: Int
    let tags: [String]
    let matchName: String
    let matchDetail: String
    let hint: String
    let copies: [QualityCopyInfo]
}

struct QualityCheckInfo: Hashable, Sendable {
    let name: String
    let rulesId: Int64
    let size: String
    let busy: Bool
    let error: ErrorInfo?
    let result: QualityCheckResultInfo?
    let canCheck: Bool

    static let initial = QualityCheckInfo(name: "", rulesId: 0, size: "", busy: false, error: nil, result: nil, canCheck: false)
}

struct QualityListInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let mayEdit: Bool
    let rules: [QualityRuleRow]
    let traitsLoaded: Bool
    let traitsError: ErrorInfo?
    let handMade: [QualityTraitRow]
    let trashTraits: [QualityTraitRow]
    let importedTraits: [QualityTraitRow]
    let traitSheet: QualityTraitSheetInfo?
    let traitRemove: QualityTraitRemoveInfo?
    let remove: QualityRemoveInfo?
    let check: QualityCheckInfo
    let notice: QualityNoticeInfo?
    let isEmpty: Bool
    let traitsEmpty: Bool

    static let initial = QualityListInfo(
        load: .loading,
        allowed: true,
        mayEdit: false,
        rules: [],
        traitsLoaded: false,
        traitsError: nil,
        handMade: [],
        trashTraits: [],
        importedTraits: [],
        traitSheet: nil,
        traitRemove: nil,
        remove: nil,
        check: .initial,
        notice: nil,
        isEmpty: false,
        traitsEmpty: false
    )
}

struct QualityStepInfo: Hashable, Sendable, Identifiable {
    let quality: String
    let allowed: Bool

    var id: String { quality }
}

struct QualityTraitPointsInfo: Hashable, Sendable {
    let trait: String
    let points: String
}

struct QualityStepSizeInfo: Hashable, Sendable, Identifiable {
    let step: String
    let min: String
    let preferred: String
    let max: String

    var id: String { step }
}

struct QualitySyncedInfo: Hashable, Sendable {
    let name: String
    let app: String
    let profileId: String
    let at: Date?
}

struct QualityFormInfo: Hashable, Sendable {
    let name: String
    let kinds: [String]
    let stepsBestFirst: [QualityStepInfo]
    let allowedBestFirst: [String]
    let goodEnoughAt: String
    let goodEnoughPoints: String
    let traits: [QualityTraitPointsInfo]
    let mustHave: [String]
    let never: [String]
    let blockedGroups: [String]
    let devices: [String]
    let order: String
    let upgradeMinPoints: String
    let minPoints: String
    let properMode: String
    let properAge: String
    let sizeMin: String
    let sizeMax: String
    let stepSizes: [QualityStepSizeInfo]
    let sizeChoices: [String]
    let replaceUnderMin: Bool
    let prefer: String
    let waitTorrent: String
    let waitUsenet: String
    let skipWait: Bool
    let tags: [String]
    let languages: [String]
    let frenchVersions: [String]
    let originalOk: Bool
    let languageWait: String
    let music: Bool
    let frenchWanted: Bool
    let synced: QualitySyncedInfo?

    static let blank = QualityFormInfo(
        name: "",
        kinds: [],
        stepsBestFirst: [],
        allowedBestFirst: [],
        goodEnoughAt: "",
        goodEnoughPoints: "",
        traits: [],
        mustHave: [],
        never: [],
        blockedGroups: [],
        devices: [],
        order: "",
        upgradeMinPoints: "",
        minPoints: "",
        properMode: "",
        properAge: "",
        sizeMin: "",
        sizeMax: "",
        stepSizes: [],
        sizeChoices: [],
        replaceUnderMin: false,
        prefer: "",
        waitTorrent: "",
        waitUsenet: "",
        skipWait: false,
        tags: [],
        languages: [],
        frenchVersions: [],
        originalOk: true,
        languageWait: "",
        music: false,
        frenchWanted: false,
        synced: nil
    )
}

struct QualityTraitChoiceInfo: Hashable, Sendable, Identifiable {
    let id: String
    let own: Bool
    let name: String
}

struct QualityTrashProfileInfo: Hashable, Sendable, Identifiable {
    let id: String
    let name: String
}

struct QualityTrashSkippedInfo: Hashable, Sendable {
    let name: String
    let kinds: [String]
    let reason: String
}

struct QualityTrashResultInfo: Hashable, Sendable {
    let scored: Int
    let added: Int
    let updated: Int
    let skipped: [QualityTrashSkippedInfo]
}

struct QualityTrashInfo: Hashable, Sendable {
    let app: String
    let profiles: [QualityTrashProfileInfo]
    let profilesLoaded: Bool
    let pick: String
    let pickNote: String
    let loading: Bool
    let busy: Bool
    let error: ErrorInfo?
    let result: QualityTrashResultInfo?

    static let initial = QualityTrashInfo(
        app: "sonarr",
        profiles: [],
        profilesLoaded: false,
        pick: "",
        pickNote: "",
        loading: false,
        busy: false,
        error: nil,
        result: nil
    )
}

struct QualityEditInfo: Hashable, Sendable {
    let load: LoadInfo
    let allowed: Bool
    let mayEdit: Bool
    let gone: Bool
    let id: Int64
    let isNew: Bool
    let preset: Bool
    let savedName: String
    let use: QualityUseInfo
    let form: QualityFormInfo
    let mine: [QualityTraitChoiceInfo]
    let problems: [QualityProblem]
    let saving: Bool
    let formError: ErrorInfo?
    let formUse: QualityUseInfo?
    let trash: QualityTrashInfo
    let notice: QualityNoticeInfo?
    let dirty: Bool
    let showSaveBar: Bool
    let showTrash: Bool
    let needsPermission: Bool
    let canAddTrait: Bool
    let traitChoices: [QualityTraitChoiceInfo]
    let neverChoices: [QualityTraitChoiceInfo]
    let freeChoices: [[QualityTraitChoiceInfo]]

    static let initial = QualityEditInfo(
        load: .loading,
        allowed: true,
        mayEdit: false,
        gone: false,
        id: 0,
        isNew: true,
        preset: false,
        savedName: "",
        use: .none,
        form: .blank,
        mine: [],
        problems: [],
        saving: false,
        formError: nil,
        formUse: nil,
        trash: .initial,
        notice: nil,
        dirty: false,
        showSaveBar: false,
        showTrash: false,
        needsPermission: false,
        canAddTrait: false,
        traitChoices: [],
        neverChoices: [],
        freeChoices: []
    )

    func problem(_ field: String) -> QualityProblem? {
        problems.first { $0.field == field }
    }

    func hasProblem(_ fields: [String]) -> Bool {
        problems.contains { fields.contains($0.field) }
    }
}
