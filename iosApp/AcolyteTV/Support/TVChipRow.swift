import SwiftUI

struct TVChipRow<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.TV.chipGap) {
                content()
            }
            .padding(.vertical, Theme.TV.focusPad)
        }
        .scrollClipDisabled()
        .focusSection()
    }
}

extension Theme.TV {
    static let chipGap: CGFloat = 20
}
