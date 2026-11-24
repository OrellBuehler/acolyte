import SwiftUI

struct SettingsMediaServersField: View {
    let title: String
    let value: String
    let revision: Int
    let onChange: (String) -> Void
    let hint: String?
    let placeholder: String
    let secret: Bool
    let mono: Bool
    let url: Bool
    let maxLength: Int
    @State private var text = ""

    init(
        title: String,
        value: String,
        revision: Int,
        onChange: @escaping (String) -> Void,
        hint: String? = nil,
        placeholder: String = "",
        secret: Bool = false,
        mono: Bool = false,
        url: Bool = false,
        maxLength: Int = .max
    ) {
        self.title = title
        self.value = value
        self.revision = revision
        self.onChange = onChange
        self.hint = hint
        self.placeholder = placeholder
        self.secret = secret
        self.mono = mono
        self.url = url
        self.maxLength = maxLength
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            field
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(url ? .URL : .default)
            if let hint {
                Text(hint)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .onAppear { text = value }
        .onChange(of: revision) { _, _ in text = value }
        .onChange(of: text) { _, new in
            let clipped = String(new.prefix(maxLength))
            if clipped != new { text = clipped }
            if clipped != value { onChange(clipped) }
        }
    }

    @ViewBuilder
    private var field: some View {
        if secret {
            SecureField(title, text: $text, prompt: Text(placeholder))
                .font(.body.monospaced())
        } else if mono {
            TextField(title, text: $text, prompt: Text(placeholder))
                .font(.body.monospaced())
        } else {
            TextField(title, text: $text, prompt: Text(placeholder))
        }
    }
}

struct SettingsMediaServersToggle: View {
    let title: String
    let hint: String?
    @Binding var isOn: Bool

    init(title: String, hint: String? = nil, isOn: Binding<Bool>) {
        self.title = title
        self.hint = hint
        _isOn = isOn
    }

    var body: some View {
        Toggle(isOn: $isOn) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(title)
                if let hint {
                    Text(hint)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }
}
