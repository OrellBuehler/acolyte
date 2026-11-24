import SwiftUI

struct NamingPatternField: View {
    let field: NamingField
    let title: String
    let hint: String?
    let text: String
    let enabled: Bool
    var focus: FocusState<NamingField?>.Binding
    let onChange: @MainActor (String) -> Void
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            TextField(title, text: $value, axis: .vertical)
                .font(.system(.body, design: .monospaced))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused(focus, equals: field)
                .disabled(!enabled)
            if let hint {
                Text(hint)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }
}

struct NamingNumberField: View {
    let title: String
    let hint: String
    let text: String
    let error: String
    let onChange: @MainActor (String) -> Void
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            LabeledContent(title) {
                TextField(title, text: $value)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: 120)
            }
            Text(error.isEmpty ? hint : error)
                .font(.footnote)
                .foregroundStyle(error.isEmpty ? Theme.textMuted : Theme.failText)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }
}

struct NamingExampleCard: View {
    let info: NamingInfo
    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(spacing: Theme.Spacing.small) {
                Text(L10n.tr("acq.nm.example"))
                    .font(.caption)
                    .foregroundStyle(Theme.textMuted)
                if info.exampleLoading { ProgressView().controlSize(.mini) }
            }
            if let error = info.exampleError {
                Text(NamingText.error(error))
                    .font(.callout)
                    .foregroundStyle(Theme.failText)
            } else {
                Text(text)
                    .font(.system(.callout, design: .monospaced))
                    .foregroundStyle(Theme.text)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.medium)
        .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}

struct NamingWordsSheet: View {
    let store: NamingStore
    let field: NamingField
    @State private var query = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(store.info.text(field.rawValue))
                        .font(.system(.callout, design: .monospaced))
                    if let error = store.info.exampleError {
                        Text(NamingText.error(error))
                            .font(.footnote)
                            .foregroundStyle(Theme.failText)
                    } else {
                        Text(store.info.focusExamples[field.rawValue] ?? "")
                            .font(.system(.footnote, design: .monospaced))
                            .foregroundStyle(Theme.textMuted)
                    }
                } header: {
                    Text(L10n.format("acq.nm.words.for", NamingText.fieldLabel(field)))
                } footer: {
                    Text(L10n.tr("acq.nm.words.hint"))
                }
                Section {
                    let found = store.words(field, query)
                    if found.isEmpty {
                        Text(L10n.tr("acq.nm.words.none"))
                            .foregroundStyle(Theme.textMuted)
                    }
                    ForEach(found) { word in
                        Button {
                            store.insertWord(field, word.key)
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("{" + word.key + "}")
                                    .font(.system(.body, design: .monospaced))
                                    .foregroundStyle(Theme.accentText)
                                Text(word.about)
                                    .font(.footnote)
                                    .foregroundStyle(Theme.textMuted)
                            }
                        }
                    }
                }
            }
            .searchable(text: $query, prompt: L10n.tr("acq.nm.words.search"))
            .navigationTitle(L10n.tr("acq.nm.words"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("acq.nm.words.close")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
