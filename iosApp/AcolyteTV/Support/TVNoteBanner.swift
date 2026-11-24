import SwiftUI

struct TVNoteBanner: View {
    let text: String
    var failed = false
    var undo: (() -> Void)?

    var body: some View {
        HStack(spacing: Theme.TV.gutter) {
            Label {
                Text(text)
                    .font(.callout)
                    .foregroundStyle(Theme.text)
                    .lineLimit(3)
            } icon: {
                Image(systemName: failed ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                    .foregroundStyle(failed ? Theme.failText : Theme.ok)
            }
            Spacer(minLength: 0)
            if let undo {
                Button(L10n.tr("requests.undo"), action: undo)
                    .buttonStyle(.bordered)
            }
        }
        .padding(Theme.TV.thumbInset)
        .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: Theme.TV.cornerRadius, style: .continuous))
        .padding(.top, Theme.TV.focusPad)
    }
}
