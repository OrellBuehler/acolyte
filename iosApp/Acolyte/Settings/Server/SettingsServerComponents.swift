import SwiftUI

struct SettingsServerField: View {
    let title: String
    let text: String
    var hint: String?
    var placeholder = ""
    var secret = false
    var mono = false
    var keyboard: UIKeyboardType = .default
    var enabled = true
    var capitalize = false
    let onChange: (String) -> Void
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            input
                .textInputAutocapitalization(capitalize ? .sentences : .never)
                .autocorrectionDisabled()
                .keyboardType(keyboard)
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

    @ViewBuilder
    private var input: some View {
        if secret {
            SecureField(placeholder, text: $value)
        } else if mono {
            TextField(placeholder, text: $value)
                .font(.body.monospaced())
        } else {
            TextField(placeholder, text: $value)
        }
    }
}

struct SettingsServerErrorText: View {
    let error: ErrorInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(error.localized)
                .foregroundStyle(Theme.failText)
            if SettingsServerWords.showsSentence(error) {
                Text(error.message)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }
}

struct SettingsServerInfoLine: View {
    let text: String
    var systemImage = "info.circle"

    var body: some View {
        Label {
            Text(text)
                .font(.callout)
                .foregroundStyle(Theme.text)
        } icon: {
            Image(systemName: systemImage)
                .foregroundStyle(Theme.infoText)
        }
    }
}
