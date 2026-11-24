import SwiftUI

extension Theme.TV {
    static let thumbHeight: CGFloat = 150
    static let thumbInset: CGFloat = 24
    static let thumbCorner: CGFloat = 12

    static var thumbColumns: [GridItem] {
        [GridItem(.flexible(), spacing: gutter, alignment: .top), GridItem(.flexible(), spacing: gutter, alignment: .top)]
    }
}

struct TVThumbCard<Content: View>: View {
    let image: String?
    let name: String
    var square = false
    var focus: TVFocusKey?
    let action: () -> Void
    @ViewBuilder var content: () -> Content

    var body: some View {
        let height = Theme.TV.thumbHeight
        Button(action: action) {
            HStack(alignment: .top, spacing: Theme.TV.thumbInset) {
                AuthImage(source: image, size: .poster) { TVArtPlaceholder(name: name) }
                    .frame(width: square ? height : height * 2 / 3, height: height)
                    .clipShape(RoundedRectangle(cornerRadius: Theme.TV.thumbCorner, style: .continuous))
                VStack(alignment: .leading, spacing: 8) {
                    content()
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(Theme.TV.thumbInset)
            .frame(maxWidth: .infinity, minHeight: height + Theme.TV.thumbInset * 2, alignment: .topLeading)
        }
        .buttonStyle(.card)
        .tvTrackFocus(focus)
        .accessibilityLabel(name)
    }
}
