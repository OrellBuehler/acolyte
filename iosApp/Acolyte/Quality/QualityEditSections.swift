import SwiftUI

enum QualityAnchor {
    static func id(_ field: String) -> String {
        switch field {
        case "name", "kinds": "name"
        case "steps", "good_enough_at", "good_enough_points": "steps"
        case "traits", "order", "upgrade_min_points", "min_points", "proper_policy", "max_age_days": "traits"
        case "must_have", "never": "must"
        case "size_limits": "size"
        case "prefer", "wait_minutes_torrent", "wait_minutes_usenet": "waiting"
        case "language", "wait_days": "language"
        default: "name"
        }
    }

    static let advanced = ["order", "upgrade_min_points", "min_points", "proper_policy", "max_age_days"]
}

struct QualityChipBlock: View {
    let title: String
    let items: [String]
    let label: (String) -> String
    let remove: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            if items.isEmpty {
                Text(L10n.tr("acq.q.e.nothing.yet"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            } else {
                QualityFlowLayout(spacing: 6) {
                    ForEach(items, id: \.self) { item in
                        QualityChip(text: label(item), removable: true) { remove(item) }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct QualityAddMenu: View {
    let title: String
    let choices: [QualityTraitChoiceInfo]
    let add: (String) -> Void

    var body: some View {
        let builtIn = choices.filter { !$0.own }
        let own = choices.filter { $0.own }
        Menu {
            ForEach(builtIn) { choice in
                Button(QualityWords.choiceLabel(choice)) { add(choice.id) }
            }
            if !own.isEmpty {
                Section(L10n.tr("acq.q.e.your.traits")) {
                    ForEach(own) { choice in
                        Button(QualityWords.choiceLabel(choice)) { add(choice.id) }
                    }
                }
            }
        } label: {
            Label(title, systemImage: "plus.circle")
        }
        .disabled(choices.isEmpty)
    }
}

struct QualityNameSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore

    var body: some View {
        Section {
            if info.preset {
                Text(presetLine)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                QualityHint(text: L10n.tr("acq.q.e.fixed"))
            } else {
                QualityField(title: L10n.tr("acq.q.e.name"), text: info.form.name, kind: .text(200)) { store.setName($0) }
                QualityProblemLine(problem: info.problem("name"))
                Text(L10n.tr("acq.q.e.for"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                ForEach(QualityChoices.kinds, id: \.self) { kind in
                    Toggle(
                        QualityWords.kind(kind),
                        isOn: Binding(get: { info.form.kinds.contains(kind) }, set: { store.setKind(kind, $0) })
                    )
                }
                QualityProblemLine(problem: info.problem("kinds"))
                QualityHint(text: L10n.tr("acq.q.e.switching"))
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.e.name.kinds"))
        }
        .id(QualityAnchor.id("name"))
        .disabled(!info.mayEdit)
    }

    private var presetLine: String {
        info.form.name + " \u{00B7} " + info.form.kinds.map { QualityWords.kind($0) }.joined(separator: ", ")
    }
}

struct QualityStepsSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore

    var body: some View {
        Section {
            ForEach(Array(info.form.stepsBestFirst.enumerated()), id: \.element.id) { index, step in
                stepRow(index, step)
            }
            QualityProblemLine(problem: info.problem("steps"))
            if !info.form.allowedBestFirst.isEmpty {
                Picker(
                    L10n.tr("acq.q.e.good.enough.at"),
                    selection: Binding(get: { info.form.goodEnoughAt }, set: { store.setGoodEnoughAt($0) })
                ) {
                    ForEach(info.form.allowedBestFirst, id: \.self) { quality in
                        Text(QualityWords.step(quality)).tag(quality)
                    }
                }
                QualityHint(text: L10n.tr("acq.q.e.good.enough.hint"))
            }
            QualityProblemLine(problem: info.problem("good_enough_at"))
            QualityField(
                title: L10n.tr("acq.q.e.good.enough.points"),
                text: info.form.goodEnoughPoints,
                kind: .signed
            ) { store.setGoodEnoughPoints($0) }
            QualityHint(text: L10n.tr("acq.q.e.good.enough.points.hint"))
            QualityProblemLine(problem: info.problem("good_enough_points"))
        } header: {
            QualityHeader(title: L10n.tr("acq.q.e.steps"), subtitle: L10n.tr("acq.q.e.steps.desc"))
        }
        .id(QualityAnchor.id("steps"))
        .disabled(!info.mayEdit)
    }

    private func stepRow(_ index: Int, _ step: QualityStepInfo) -> some View {
        HStack(spacing: Theme.Spacing.small) {
            Toggle(isOn: Binding(get: { step.allowed }, set: { store.setStepAllowed(step.quality, $0) })) {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(QualityWords.step(step.quality))
                        .foregroundStyle(Theme.text)
                    if step.allowed, step.quality == info.form.goodEnoughAt {
                        StatusChip(text: L10n.tr("acq.q.e.good.enough"), tone: .ok)
                    }
                }
            }
            .accessibilityLabel(L10n.format("acq.q.e.step.allowed", QualityWords.step(step.quality)))
            if info.mayEdit {
                Button {
                    store.moveStep(step.quality, up: true)
                } label: {
                    Image(systemName: "arrow.up")
                }
                .buttonStyle(.borderless)
                .disabled(index == 0)
                .accessibilityLabel(L10n.tr("acq.q.e.move.up"))
                Button {
                    store.moveStep(step.quality, up: false)
                } label: {
                    Image(systemName: "arrow.down")
                }
                .buttonStyle(.borderless)
                .disabled(index >= info.form.stepsBestFirst.count - 1)
                .accessibilityLabel(L10n.tr("acq.q.e.move.down"))
            }
        }
    }
}

struct QualityTraitsSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore
    @State private var advanced = false
    @Environment(\.openRoute) private var openRoute

    var body: some View {
        Section {
            if info.form.traits.isEmpty {
                QualityHint(text: L10n.tr("acq.q.e.no.traits"))
            }
            ForEach(Array(info.form.traits.enumerated()), id: \.offset) { index, row in
                traitRow(index, row)
            }
            Button(L10n.tr("acq.q.e.add.trait"), systemImage: "plus") { store.addTrait() }
                .disabled(!info.canAddTrait)
            Button(L10n.tr("acq.q.e.own.trait"), systemImage: "wand.and.stars") { openRoute(.settingsPickRules) }
            QualityProblemLine(problem: info.problem("traits"))
            DisclosureGroup(
                L10n.tr("acq.q.e.advanced"),
                isExpanded: Binding(get: { advanced || !info.mayEdit }, set: { advanced = $0 })
            ) {
                advancedRows
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.e.traits"), subtitle: L10n.tr("acq.q.e.traits.desc"))
        }
        .id(QualityAnchor.id("traits"))
        .disabled(!info.mayEdit)
        .onChange(of: info.hasProblem(QualityAnchor.advanced)) { _, has in
            if has { advanced = true }
        }
    }

    private func choices(_ index: Int) -> [QualityTraitChoiceInfo] {
        index < info.freeChoices.count ? info.freeChoices[index] : []
    }

    private func traitRow(_ index: Int, _ row: QualityTraitPointsInfo) -> some View {
        let options = choices(index)
        let label = QualityWords.traitLabel(row.trait, mine: info.mine)
        return VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack(spacing: Theme.Spacing.small) {
                if options.contains(where: { $0.id == row.trait }) {
                    traitPicker(index, row, options)
                } else {
                    Text(label)
                        .foregroundStyle(Theme.text)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Button(role: .destructive) {
                    store.removeTrait(index)
                } label: {
                    Image(systemName: "minus.circle")
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(L10n.format("acq.q.e.remove.trait", label))
            }
            QualityField(title: L10n.format("acq.q.e.points.for", label), text: row.points, kind: .signed) {
                store.setTraitPoints(index, $0)
            }
        }
    }

    private func traitPicker(_ index: Int, _ row: QualityTraitPointsInfo, _ options: [QualityTraitChoiceInfo]) -> some View {
        let builtIn = options.filter { !$0.own }
        let own = options.filter { $0.own }
        return Picker(
            L10n.format("acq.q.e.trait.n", index + 1),
            selection: Binding(get: { row.trait }, set: { store.setTraitKey(index, $0) })
        ) {
            ForEach(builtIn) { choice in
                Text(QualityWords.choiceLabel(choice)).tag(choice.id)
            }
            if !own.isEmpty {
                Section(L10n.tr("acq.q.e.your.traits")) {
                    ForEach(own) { choice in
                        Text(QualityWords.choiceLabel(choice)).tag(choice.id)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var advancedRows: some View {
        Picker(
            L10n.tr("acq.q.e.which.wins"),
            selection: Binding(get: { info.form.order }, set: { store.setOrder($0) })
        ) {
            ForEach(QualityChoices.orders, id: \.self) { order in
                Text(QualityWords.order(order)).tag(order)
            }
        }
        QualityHint(text: QualityWords.orderNote(info.form.order))
        QualityProblemLine(problem: info.problem("order"))
        QualityField(title: L10n.tr("acq.q.e.upgrade"), text: info.form.upgradeMinPoints, kind: .signed) {
            store.setUpgradeMinPoints($0)
        }
        QualityHint(text: L10n.tr("acq.q.e.upgrade.hint"))
        QualityProblemLine(problem: info.problem("upgrade_min_points"))
        QualityField(title: L10n.tr("acq.q.e.min.points"), text: info.form.minPoints, kind: .signed) {
            store.setMinPoints($0)
        }
        QualityProblemLine(problem: info.problem("min_points"))
        Picker(
            L10n.tr("acq.q.e.fixed.copies"),
            selection: Binding(get: { info.form.properMode }, set: { store.setProperMode($0) })
        ) {
            ForEach(QualityChoices.properModes, id: \.self) { mode in
                Text(QualityWords.properMode(mode)).tag(mode)
            }
        }
        QualityHint(text: QualityWords.properNote(info.form.properMode))
        QualityProblemLine(problem: info.problem("proper_policy"))
        if info.form.properMode == "take_any" || info.form.properMode == "same_group_only" {
            QualityField(title: L10n.tr("acq.q.e.proper.age"), text: info.form.properAge, kind: .whole) {
                store.setProperAge($0)
            }
            QualityHint(text: L10n.tr("acq.q.e.proper.age.hint"))
            QualityProblemLine(problem: info.problem("max_age_days"))
        }
    }
}

struct QualityTrashSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore

    var body: some View {
        Section {
            if let synced = info.form.synced {
                syncedRows(synced)
            }
            if info.mayEdit {
                pickRows
            }
            if let error = info.trash.error {
                QualityErrorLine(error: error)
            }
            if let result = info.trash.result {
                resultRows(result)
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.e.trash.title"), subtitle: L10n.tr("acq.q.e.trash.desc"))
        }
    }

    private func syncedRows(_ synced: QualitySyncedInfo) -> some View {
        let when = synced.at?.formatted(date: .abbreviated, time: .shortened) ?? ""
        return VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(synced.name.isEmpty ? L10n.tr("acq.q.e.trash.profile.fallback") : synced.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Text(L10n.format("acq.q.e.trash.synced", QualityWords.trashApp(synced.app), when))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            if info.mayEdit {
                Button {
                    store.syncAgain()
                } label: {
                    if info.trash.busy {
                        ProgressView()
                    } else {
                        Text(L10n.tr("acq.q.e.trash.sync.again"))
                    }
                }
                .buttonStyle(.bordered)
                .disabled(info.dirty || info.trash.busy)
            }
        }
    }

    @ViewBuilder
    private var pickRows: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(L10n.tr("acq.q.e.trash.for"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Picker(
                L10n.tr("acq.q.e.trash.for"),
                selection: Binding(get: { info.trash.app }, set: { store.setTrashApp($0) })
            ) {
                ForEach(QualityChoices.trashApps, id: \.self) { app in
                    Text(QualityWords.trashApp(app)).tag(app)
                }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
        }
        if !info.trash.profilesLoaded {
            Button {
                store.loadTrash()
            } label: {
                if info.trash.loading {
                    ProgressView()
                } else {
                    Text(L10n.tr("acq.q.e.trash.show"))
                }
            }
            .disabled(info.trash.loading)
        } else if info.trash.profiles.isEmpty {
            QualityHint(text: L10n.tr("acq.q.e.trash.none"))
        } else {
            Picker(
                L10n.tr("acq.q.e.trash.profile"),
                selection: Binding(get: { info.trash.pick }, set: { store.setTrashPick($0) })
            ) {
                ForEach(info.trash.profiles) { profile in
                    Text(profile.name).tag(profile.id)
                }
            }
            if !info.trash.pickNote.isEmpty {
                QualityHint(text: info.trash.pickNote)
            }
            QualityHint(text: L10n.tr("acq.q.e.trash.replaces"))
            Button {
                store.applyTrash()
            } label: {
                if info.trash.busy {
                    ProgressView()
                } else {
                    Text(L10n.tr("acq.q.e.trash.apply"))
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(info.dirty || info.trash.pick.isEmpty || info.trash.busy)
            if info.dirty {
                QualityHint(text: L10n.tr("acq.q.e.trash.save.first"))
            }
        }
    }

    @ViewBuilder
    private func resultRows(_ result: QualityTrashResultInfo) -> some View {
        Text(L10n.format("acq.q.e.trash.result", result.scored, result.added, result.updated))
            .font(.footnote)
            .foregroundStyle(Theme.text)
        if !result.skipped.isEmpty {
            DisclosureGroup(L10n.format("acq.q.e.trash.left.out", result.skipped.count)) {
                ForEach(Array(result.skipped.enumerated()), id: \.offset) { _, skipped in
                    Text(skippedLine(skipped))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    private func skippedLine(_ skipped: QualityTrashSkippedInfo) -> String {
        if skipped.kinds.isEmpty { return skipped.name + ": " + skipped.reason }
        let words = skipped.kinds.map { QualityWords.traitKind($0) }.joined(separator: ", ")
        return skipped.name + ": " + L10n.format("acq.q.e.trash.tests", words)
    }
}

struct QualityMustSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore

    var body: some View {
        Section {
            QualityChipBlock(
                title: L10n.tr("acq.q.e.must.have"),
                items: info.form.mustHave,
                label: { QualityWords.traitLabel($0, mine: info.mine) },
                remove: { store.removeMustHave($0) }
            )
            QualityAddMenu(
                title: L10n.tr("acq.q.e.add.must"),
                choices: info.traitChoices.filter { !info.form.mustHave.contains($0.id) },
                add: { store.addMustHave($0) }
            )
            QualityProblemLine(problem: info.problem("must_have"))
            QualityChipBlock(
                title: L10n.tr("acq.q.e.never"),
                items: info.form.never,
                label: { QualityWords.traitLabel($0, mine: info.mine) },
                remove: { store.removeNever($0) }
            )
            QualityAddMenu(
                title: L10n.tr("acq.q.e.add.never"),
                choices: info.neverChoices.filter { !info.form.never.contains($0.id) },
                add: { store.addNever($0) }
            )
            QualityProblemLine(problem: info.problem("never"))
            QualityChipBlock(
                title: L10n.tr("acq.q.e.blocked.groups"),
                items: info.form.blockedGroups,
                label: { $0 },
                remove: { store.removeBlockedGroup($0) }
            )
            QualityAddField(title: L10n.tr("acq.q.e.add.group")) { store.addBlockedGroup($0) }
            QualityHint(text: L10n.tr("acq.q.e.blocked.hint"))
            if !info.form.music {
                devices
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.e.must.never"))
        }
        .id(QualityAnchor.id("must"))
        .disabled(!info.mayEdit)
    }

    @ViewBuilder
    private var devices: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("acq.q.e.devices"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            QualityFlowLayout(spacing: 6) {
                ForEach(QualityChoices.devices, id: \.self) { device in
                    QualityChip(text: QualityWords.device(device), selected: info.form.devices.contains(device)) {
                        store.toggleDevice(device)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        QualityHint(text: L10n.tr("acq.q.e.devices.hint"))
    }
}

struct QualitySizeSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore

    var body: some View {
        Section {
            QualityField(
                title: L10n.tr("acq.q.e.smallest"),
                text: info.form.sizeMin,
                prompt: L10n.tr("acq.q.e.no.limit"),
                kind: .decimal
            ) { store.setSizeMin($0) }
            QualityField(
                title: L10n.tr("acq.q.e.largest"),
                text: info.form.sizeMax,
                prompt: L10n.tr("acq.q.e.no.limit"),
                kind: .decimal
            ) { store.setSizeMax($0) }
            QualityHint(text: L10n.tr("acq.q.e.size.note"))
            ForEach(info.form.stepSizes) { row in
                stepSizeRow(row)
            }
            QualityHint(text: L10n.tr("acq.q.e.step.sizes.note"))
            sizeMenu
            Toggle(isOn: Binding(get: { info.form.replaceUnderMin }, set: { store.setReplaceUnderMin($0) })) {
                Text(L10n.tr("acq.q.e.replace.small"))
            }
            QualityHint(text: L10n.tr("acq.q.e.replace.small.hint"))
            QualityProblemLine(problem: info.problem("size_limits"))
        } header: {
            QualityHeader(
                title: L10n.tr("acq.q.e.size"),
                subtitle: info.form.music ? L10n.tr("acq.q.e.size.music") : L10n.tr("acq.q.e.size.video")
            )
        }
        .id(QualityAnchor.id("size"))
        .disabled(!info.mayEdit)
    }

    private var sizeMenu: some View {
        Menu {
            ForEach(info.form.sizeChoices, id: \.self) { step in
                Button(QualityWords.step(step)) { store.addStepSize(step) }
            }
        } label: {
            Label(L10n.tr("acq.q.e.add.step.size"), systemImage: "plus.circle")
        }
        .disabled(info.form.sizeChoices.isEmpty)
    }

    private func stepSizeRow(_ row: QualityStepSizeInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            HStack {
                Text(QualityWords.step(row.step))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button(role: .destructive) {
                    store.removeStepSize(row.step)
                } label: {
                    Image(systemName: "minus.circle")
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(L10n.format("acq.q.e.remove.step.size", QualityWords.step(row.step)))
            }
            QualityField(title: L10n.tr("acq.q.e.smallest"), text: row.min, kind: .decimal) {
                store.setStepSize(row.step, "min", $0)
            }
            QualityField(
                title: L10n.tr("acq.q.e.preferred"),
                text: row.preferred,
                prompt: L10n.tr("acq.q.e.no.preferred"),
                kind: .decimal
            ) { store.setStepSize(row.step, "preferred", $0) }
            QualityField(title: L10n.tr("acq.q.e.largest"), text: row.max, kind: .decimal) {
                store.setStepSize(row.step, "max", $0)
            }
        }
    }
}

struct QualityWaitingSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore

    var body: some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("acq.q.e.prefer"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Picker(
                    L10n.tr("acq.q.e.prefer"),
                    selection: Binding(get: { info.form.prefer }, set: { store.setPrefer($0) })
                ) {
                    ForEach(QualityChoices.prefers, id: \.self) { prefer in
                        Text(QualityWords.prefer(prefer)).tag(prefer)
                    }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }
            QualityHint(text: L10n.tr("acq.q.e.wins.tie"))
            QualityProblemLine(problem: info.problem("prefer"))
            QualityField(title: L10n.tr("acq.q.e.wait.torrent"), text: info.form.waitTorrent, kind: .whole) {
                store.setWaitTorrent($0)
            }
            QualityProblemLine(problem: info.problem("wait_minutes_torrent"))
            QualityField(title: L10n.tr("acq.q.e.wait.usenet"), text: info.form.waitUsenet, kind: .whole) {
                store.setWaitUsenet($0)
            }
            QualityProblemLine(problem: info.problem("wait_minutes_usenet"))
            Toggle(isOn: Binding(get: { info.form.skipWait }, set: { store.setSkipWait($0) })) {
                Text(L10n.tr("acq.q.e.skip.wait"))
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.e.waiting"), subtitle: L10n.tr("acq.q.e.waiting.desc"))
        }
        .id(QualityAnchor.id("waiting"))
        .disabled(!info.mayEdit)
    }
}

struct QualitySourcesSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore

    var body: some View {
        Section {
            QualityChipBlock(
                title: L10n.tr("acq.q.e.tags"),
                items: info.form.tags,
                label: { $0 },
                remove: { store.removeTag($0) }
            )
            QualityAddField(title: L10n.tr("acq.q.e.add.tag"), commitOnComma: true) { store.addTag($0) }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.e.sources"), subtitle: L10n.tr("acq.q.e.sources.desc"))
        }
        .disabled(!info.mayEdit)
    }
}

struct QualityLanguageSection: View {
    let info: QualityEditInfo
    let store: QualityEditStore

    var body: some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                Text(L10n.tr("acq.q.e.languages"))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                QualityFlowLayout(spacing: 6) {
                    ForEach(QualityChoices.languages, id: \.self) { code in
                        QualityChip(text: QualityWords.language(code), selected: info.form.languages.contains(code)) {
                            store.toggleLanguage(code)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            QualityProblemLine(problem: info.problem("language"))
            if info.form.frenchWanted {
                frenchRows
            }
            Toggle(isOn: Binding(get: { info.form.originalOk }, set: { store.setOriginalOk($0) })) {
                Text(L10n.tr("acq.q.e.original"))
            }
            QualityHint(text: L10n.tr("acq.q.e.original.hint"))
            if info.form.originalOk, !info.form.languages.isEmpty {
                QualityField(title: L10n.tr("acq.q.e.lang.wait"), text: info.form.languageWait, kind: .whole) {
                    store.setLanguageWait($0)
                }
                QualityHint(text: L10n.tr("acq.q.e.lang.wait.hint"))
                QualityProblemLine(problem: info.problem("wait_days"))
            }
        } header: {
            QualityHeader(title: L10n.tr("acq.q.e.language"), subtitle: L10n.tr("acq.q.e.language.desc"))
        }
        .id(QualityAnchor.id("language"))
        .disabled(!info.mayEdit)
    }

    @ViewBuilder
    private var frenchRows: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("acq.q.e.french"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            QualityFlowLayout(spacing: 6) {
                ForEach(QualityChoices.frenchVersions, id: \.self) { version in
                    QualityChip(text: version, selected: info.form.frenchVersions.contains(version)) {
                        store.toggleFrench(version)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        QualityHint(text: L10n.tr("acq.q.e.french.hint"))
    }
}
