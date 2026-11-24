import SwiftUI
import UIKit

struct SettingsControlsField: View {
    let title: String
    let text: String
    var prompt = ""
    var secure = false
    var monospaced = false
    var keyboard: UIKeyboardType = .default
    let onChange: (String) -> Void
    @State private var value = ""

    var body: some View {
        LabeledContent(title) {
            field
                .multilineTextAlignment(.trailing)
                .font(fieldFont)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }

    private var fieldFont: Font {
        monospaced ? Font.body.monospaced() : Font.body
    }

    @ViewBuilder
    private var field: some View {
        if secure {
            SecureField(title, text: $value, prompt: Text(prompt))
                .textContentType(.password)
        } else {
            TextField(title, text: $value, prompt: Text(prompt))
                .keyboardType(keyboard)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        }
    }
}
