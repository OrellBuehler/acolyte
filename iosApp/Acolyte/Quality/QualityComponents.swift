import SwiftUI
import UIKit

struct QualityFlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var widest: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            widest = max(widest, x - spacing)
        }
        return CGSize(width: widest, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), anchor: .topLeading, proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

struct QualityChip: View {
    let text: String
    var selected = false
    var removable = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: Theme.Spacing.xSmall) {
                Text(text)
                    .font(.subheadline.weight(.medium))
                    .multilineTextAlignment(.leading)
                if removable {
                    Image(systemName: "xmark")
                        .font(.caption2.weight(.bold))
                }
            }
            .foregroundStyle(selected ? Theme.onAccent : Theme.text)
            .padding(.horizontal, Theme.Spacing.medium)
            .padding(.vertical, 6)
            .background(selected ? Theme.accentFill : Theme.surfaceRaised, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(removable ? L10n.format("acq.q.e.remove.trait", text) : text)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

struct QualityTagPill: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.footnote.weight(.medium))
            .foregroundStyle(Theme.text)
            .padding(.horizontal, Theme.Spacing.small)
            .padding(.vertical, 3)
            .background(Theme.surfaceRaised, in: Capsule())
    }
}

struct QualityHint: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct QualityProblemLine: View {
    let problem: QualityProblem?

    var body: some View {
        if let problem {
            Text(QualityWords.problem(problem))
                .font(.footnote)
                .foregroundStyle(Theme.failText)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

struct QualityErrorLine: View {
    let error: ErrorInfo?

    var body: some View {
        if let error {
            Text(QualityWords.errorText(error))
                .font(.footnote)
                .foregroundStyle(Theme.failText)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

struct QualityHeader: View {
    let title: String
    var subtitle: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.headline)
                .foregroundStyle(Theme.text)
                .accessibilityAddTraits(.isHeader)
            if let subtitle {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .textCase(nil)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct QualityField: View {
    let title: String
    let text: String
    var prompt: String?
    var kind: QualityFieldKind = .text(200)
    let onChange: (String) -> Void
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            TextField("", text: $value, prompt: prompt.map { Text($0) })
                .keyboardType(keyboard)
                .autocorrectionDisabled(isNumber)
                .textInputAutocapitalization(isNumber ? .never : .sentences)
                .accessibilityLabel(title)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in
            if new != value { value = new }
        }
        .onChange(of: value) { _, new in
            let clean = QualityWords.filterText(new, kind)
            if clean != new {
                value = clean
            } else if clean != text {
                onChange(clean)
            }
        }
    }

    private var isNumber: Bool {
        switch kind {
        case .text: false
        default: true
        }
    }

    private var keyboard: UIKeyboardType {
        switch kind {
        case .text: .default
        case .decimal: .decimalPad
        case .whole: .numberPad
        case .signed: .numbersAndPunctuation
        }
    }
}

struct QualityTextArea: View {
    let title: String
    let text: String
    let onChange: (String) -> Void
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            TextEditor(text: $value)
                .frame(minHeight: 110)
                .scrollContentBackground(.hidden)
                .padding(Theme.Spacing.small)
                .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .accessibilityLabel(title)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in
            if new != value { value = new }
        }
        .onChange(of: value) { _, new in
            if new != text { onChange(new) }
        }
    }
}

struct QualityAddField: View {
    let title: String
    var commitOnComma = false
    let onAdd: (String) -> Void
    @State private var value = ""

    var body: some View {
        HStack(spacing: Theme.Spacing.small) {
            TextField("", text: $value, prompt: Text(title))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .onSubmit { commit() }
                .accessibilityLabel(title)
            Button {
                commit()
            } label: {
                Image(systemName: "plus.circle.fill")
                    .font(.title3)
                    .foregroundStyle(value.trimmingCharacters(in: .whitespaces).isEmpty ? Theme.textFaint : Theme.accentText)
            }
            .buttonStyle(.borderless)
            .disabled(value.trimmingCharacters(in: .whitespaces).isEmpty)
            .accessibilityLabel(title)
        }
        .onChange(of: value) { _, new in
            if commitOnComma, new.contains(",") { commit() }
        }
    }

    private func commit() {
        let text = value
        guard !text.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        value = ""
        onAdd(text)
    }
}

struct QualityConfirmSheet: View {
    let title: String
    let name: String
    let notes: [String]
    let error: String?
    let cancelTitle: String
    let confirmTitle: String
    let busy: Bool
    let cancel: () -> Void
    let confirm: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.medium) {
            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
            Text(name)
                .font(.headline)
                .foregroundStyle(Theme.text)
            ForEach(Array(notes.enumerated()), id: \.offset) { _, note in
                Text(note)
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
            }
            if let error {
                Text(error)
                    .font(.callout)
                    .foregroundStyle(Theme.failText)
            }
            Spacer(minLength: Theme.Spacing.small)
            HStack(spacing: Theme.Spacing.medium) {
                Button(cancelTitle, action: cancel)
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                Button(role: .destructive, action: confirm) {
                    if busy {
                        ProgressView()
                    } else {
                        Text(confirmTitle)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.fail)
                .disabled(busy)
                .frame(maxWidth: .infinity)
            }
            .controlSize(.large)
        }
        .padding(Theme.Spacing.large)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Theme.background)
        .presentationDetents([.medium, .large])
    }
}

private struct QualityNoticeModifier: ViewModifier {
    let notice: QualityNoticeInfo?
    let dismiss: @MainActor (Int) -> Void

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let notice {
                    Text(QualityWords.notice(notice))
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(Theme.Spacing.medium)
                        .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
                        .padding(Theme.Spacing.screen)
                        .onTapGesture { dismiss(notice.seq) }
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .accessibilityAddTraits(.isStaticText)
                }
            }
            .animation(.smooth(duration: 0.24), value: notice?.seq)
            .task(id: notice?.seq) {
                guard let seq = notice?.seq else { return }
                try? await Task.sleep(for: .seconds(5))
                dismiss(seq)
            }
    }
}

extension View {
    func qualityNotice(_ notice: QualityNoticeInfo?, dismiss: @escaping @MainActor (Int) -> Void) -> some View {
        modifier(QualityNoticeModifier(notice: notice, dismiss: dismiss))
    }
}
