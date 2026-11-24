import SwiftUI
import UIKit

struct SettingsSignInField: View {
    let title: String
    let text: String
    let onChange: (String) -> Void
    var hint: String?
    var prompt: String?
    var secure = false
    var monospaced = false
    var keyboard: UIKeyboardType = .default
    var disabled = false
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            field
                .keyboardType(keyboard)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .font(monospaced ? .body.monospaced() : .body)
                .disabled(disabled)
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
    private var field: some View {
        if secure {
            SecureField(title, text: $value, prompt: prompt.map { Text($0) })
        } else {
            TextField(title, text: $value, prompt: prompt.map { Text($0) })
        }
    }
}

struct SettingsSignInArea: View {
    let title: String
    let text: String
    let onChange: (String) -> Void
    var hint: String?
    var minHeight: CGFloat = 88
    var monospaced = true
    var disabled = false
    var invalid = false
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            TextEditor(text: $value)
                .frame(minHeight: minHeight)
                .scrollContentBackground(.hidden)
                .padding(Theme.Spacing.small)
                .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
                        .strokeBorder(invalid ? Theme.fail : Theme.line, lineWidth: 1)
                }
                .font(monospaced ? .body.monospaced() : .body)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .disabled(disabled)
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

struct SettingsSignInNote: View {
    let text: String
    var title: String?
    var warn = true

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.small) {
            Image(systemName: warn ? "exclamationmark.triangle.fill" : "info.circle.fill")
                .foregroundStyle(warn ? Theme.waitText : Theme.infoText)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                if let title {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.text)
                }
                Text(text)
                    .font(.callout)
                    .foregroundStyle(Theme.text)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct SettingsSignInHint: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct SettingsSignInFail: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.failText)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct SettingsSignInTestView: View {
    let test: SettingsSignInTestInfo
    let okLabel: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            if test.ok {
                StatusChip(text: okLabel, tone: .ok)
                if !test.text.isEmpty, test.text != okLabel {
                    SettingsSignInHint(text: test.text)
                }
            } else {
                StatusChip(text: L10n.tr("settings.signin.not.connected"), tone: .fail)
                if let error = test.error {
                    SettingsSignInFail(text: SettingsSignInWords.failure(error))
                } else if !test.text.isEmpty {
                    SettingsSignInFail(text: test.text)
                } else {
                    SettingsSignInFail(text: L10n.tr("settings.signin.no.answer"))
                }
            }
        }
    }
}
