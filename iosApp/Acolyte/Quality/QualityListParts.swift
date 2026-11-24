import SwiftUI

struct QualityRuleRowView: View {
    let row: QualityRuleRow
    let mayEdit: Bool
    let show: () -> Void
    let copy: () -> Void
    let remove: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(row.name)
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                Text(QualityWords.ruleLine(row))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                if row.readyChip {
                    StatusChip(text: L10n.tr("acq.q.l.ready.made"), tone: .info)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: show) {
                Text(mayEdit ? L10n.tr("acq.q.l.change") : L10n.tr("acq.q.l.open"))
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .accessibilityLabel(row.name + ", " + (mayEdit ? L10n.tr("acq.q.l.change") : L10n.tr("acq.q.l.open")))
            if mayEdit {
                Menu {
                    Button(L10n.tr("acq.q.l.copy"), systemImage: "doc.on.doc", action: copy)
                    if !row.preset {
                        Button(role: .destructive, action: remove) {
                            Label(L10n.tr("acq.q.l.remove"), systemImage: "trash")
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.title3)
                        .foregroundStyle(Theme.textMuted)
                }
                .accessibilityLabel(L10n.format("acq.q.l.more.for", row.name))
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

struct QualityTraitRowView: View {
    let row: QualityTraitRow
    let mayEdit: Bool
    let change: () -> Void
    let remove: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(row.name)
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                Text(QualityWords.traitSummary(row))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if mayEdit {
                Button(action: change) {
                    Text(L10n.tr("acq.q.l.change"))
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .accessibilityLabel(row.name + ", " + L10n.tr("acq.q.l.change"))
                Menu {
                    Button(role: .destructive, action: remove) {
                        Label(L10n.tr("acq.q.ot.remove"), systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.title3)
                        .foregroundStyle(Theme.textMuted)
                }
                .accessibilityLabel(L10n.format("acq.q.ot.more.for", row.name))
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

struct QualityTraitGroupView: View {
    let title: String
    let note: String
    let rows: [QualityTraitRow]
    let mayEdit: Bool
    let remove: (Int64) -> Void
    @State private var expanded = false

    var body: some View {
        DisclosureGroup(isExpanded: $expanded) {
            QualityHint(text: note)
            ForEach(rows) { row in
                HStack(alignment: .top, spacing: Theme.Spacing.medium) {
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(row.name)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Theme.text)
                        Text(QualityWords.specLine(row))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if mayEdit {
                        Menu {
                            Button(role: .destructive) {
                                remove(row.id)
                            } label: {
                                Label(L10n.tr("acq.q.ot.remove"), systemImage: "trash")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                                .font(.title3)
                                .foregroundStyle(Theme.textMuted)
                        }
                        .accessibilityLabel(L10n.format("acq.q.ot.more.for", row.name))
                    }
                }
            }
        } label: {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
        }
    }
}

struct QualityEmptyBlock: View {
    let title: String
    let text: String
    let actionTitle: String?
    let action: () -> Void

    var body: some View {
        VStack(spacing: Theme.Spacing.small) {
            Text(title)
                .font(.headline)
                .foregroundStyle(Theme.text)
            Text(text)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            if let actionTitle {
                Button(actionTitle, action: action)
                    .buttonStyle(.borderedProminent)
                    .padding(.top, Theme.Spacing.xSmall)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Theme.Spacing.small)
    }
}

struct QualityWhereRow: View {
    let title: String
    let text: String
    let action: (() -> Void)?

    var body: some View {
        if let action {
            Button(action: action) {
                HStack(spacing: Theme.Spacing.medium) {
                    label
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Theme.textFaint)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        } else {
            label
        }
    }

    private var label: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            if !text.isEmpty {
                Text(text)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct QualityCheckResultView: View {
    let result: QualityCheckResultInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            readAs
            if !result.matchName.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.format("acq.q.nc.matches", result.matchName))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                    if !result.matchDetail.isEmpty {
                        Text(result.matchDetail)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
            }
            if let hint = hintText {
                Text(hint)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(Array(result.copies.enumerated()), id: \.offset) { _, copy in
                QualityCopyView(copy: copy)
            }
        }
    }

    private var readAs: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("acq.q.nc.read.as"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Text(titleText)
                .font(.headline)
                .foregroundStyle(Theme.text)
            if !result.tags.isEmpty {
                QualityFlowLayout(spacing: 6) {
                    ForEach(Array(result.tags.enumerated()), id: \.offset) { _, tag in
                        QualityTagPill(text: tag)
                    }
                }
            }
        }
    }

    private var titleText: String {
        let base = result.title.isEmpty ? "\u{2014}" : result.title
        return result.year > 0 ? base + " (" + String(result.year) + ")" : base
    }

    private var hintText: String? {
        switch result.hint {
        case "no_title": L10n.tr("acq.q.nc.no.title")
        case "no_album": L10n.format("acq.q.nc.no.album", result.matchName)
        case "no_copies": L10n.tr("acq.q.nc.no.copies")
        default: nil
        }
    }
}

struct QualityCopyView: View {
    let copy: QualityCopyInfo

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(copy.header)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            QualityFlowLayout(spacing: 6) {
                StatusChip(text: QualityWords.verdict(copy.verdict), tone: copy.tone)
                if copy.passed {
                    QualityTagPill(text: L10n.format("acq.q.nc.points", copy.points))
                }
                if copy.hasFile {
                    QualityTagPill(text: L10n.tr("acq.q.nc.has.file"))
                }
            }
            if copy.showRules, !copy.rulesName.isEmpty {
                Text(copy.rulesName)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(Array(copy.reasons.enumerated()), id: \.offset) { _, reason in
                Text(reason.text)
                    .font(.footnote)
                    .foregroundStyle(reasonColor(reason.effect))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(Theme.Spacing.medium)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }

    private func reasonColor(_ effect: String) -> Color {
        switch effect {
        case "fail": Theme.failText
        case "hold", "warn": Theme.waitText
        default: Theme.text
        }
    }
}

struct QualityTraitSheet: View {
    let store: QualityListStore

    var body: some View {
        NavigationStack {
            Form {
                if let sheet = store.info.traitSheet {
                    fields(sheet)
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.q.ot.cancel")) { store.closeTrait() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if store.info.traitSheet?.busy == true {
                        ProgressView()
                    } else {
                        Button(saveTitle) { store.saveTrait() }
                    }
                }
            }
        }
    }

    private var isNew: Bool {
        (store.info.traitSheet?.id ?? 0) == 0
    }

    private var title: String {
        isNew ? L10n.tr("acq.q.ot.make.title") : L10n.tr("acq.q.ot.change.title")
    }

    private var saveTitle: String {
        isNew ? L10n.tr("acq.q.ot.make.button") : L10n.tr("acq.q.ot.save")
    }

    @ViewBuilder
    private func fields(_ sheet: QualityTraitSheetInfo) -> some View {
        Section {
            QualityField(title: L10n.tr("acq.q.ot.name"), text: sheet.name, kind: .text(120)) { store.setTraitName($0) }
            QualityProblemLine(problem: sheet.problem("name"))
        }
        Section {
            Picker(L10n.tr("acq.q.ot.look.at"), selection: Binding(get: { sheet.match }, set: { store.setTraitMatch($0) })) {
                Text(QualityWords.matchTitle("name")).tag("name")
                Text(QualityWords.matchTitle("group")).tag("group")
                Text(QualityWords.matchTitle("size")).tag("size")
            }
            QualityHint(text: QualityWords.matchHint(sheet.match))
            if sheet.match == "size" {
                QualityField(title: L10n.tr("acq.q.ot.over"), text: sheet.over, kind: .decimal) { store.setTraitOver($0) }
                QualityField(title: L10n.tr("acq.q.ot.up.to"), text: sheet.upTo, kind: .decimal) { store.setTraitUpTo($0) }
            } else {
                QualityTextArea(
                    title: sheet.match == "group" ? L10n.tr("acq.q.ot.groups") : L10n.tr("acq.q.ot.words"),
                    text: sheet.terms
                ) { store.setTraitTerms($0) }
                QualityHint(text: L10n.tr("acq.q.ot.terms.hint"))
            }
            QualityProblemLine(problem: sheet.problem("terms"))
        }
        if let error = sheet.formError {
            Section {
                QualityErrorLine(error: error)
            }
        }
    }
}
