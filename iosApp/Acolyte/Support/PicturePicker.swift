import PhotosUI
import SwiftUI
import UIKit

enum PictureEncoder {
    nonisolated static func squareJPEG(from data: Data, side: CGFloat = 512, quality: CGFloat = 0.85) -> Data? {
        guard let image = UIImage(data: data) else { return nil }
        let size = image.size
        let edge = min(size.width, size.height)
        guard edge > 0 else { return nil }
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: side, height: side), format: format)
        let scale = side / edge
        let drawSize = CGSize(width: size.width * scale, height: size.height * scale)
        let origin = CGPoint(x: (side - drawSize.width) / 2, y: (side - drawSize.height) / 2)
        let rendered = renderer.image { _ in
            image.draw(in: CGRect(origin: origin, size: drawSize))
        }
        return rendered.jpegData(compressionQuality: quality)
    }
}

struct PictureChooser: View {
    @Binding var picture: Data?
    var size: CGFloat = 96
    @State private var item: PhotosPickerItem?
    @State private var failed = false

    var body: some View {
        VStack(spacing: Theme.Spacing.small) {
            PhotosPicker(selection: $item, matching: .images, photoLibrary: .shared()) {
                avatar
            }
            .buttonStyle(.plain)
            .accessibilityLabel(L10n.tr("picture.add"))
            Text(L10n.tr(picture == nil ? "picture.add" : "picture.change"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if picture != nil {
                Button(L10n.tr("picture.remove")) {
                    picture = nil
                    item = nil
                }
                .font(.footnote.weight(.semibold))
            }
            if failed {
                Text(L10n.tr("picture.error"))
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
            }
        }
        .task(id: item) {
            guard let item else { return }
            let data = try? await item.loadTransferable(type: Data.self)
            var jpeg: Data?
            if let data {
                jpeg = await Task.detached(priority: .userInitiated) { PictureEncoder.squareJPEG(from: data) }.value
            }
            failed = jpeg == nil
            if let jpeg { picture = jpeg }
        }
    }

    @ViewBuilder
    private var avatar: some View {
        Group {
            if let picture, let image = UIImage(data: picture) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                ZStack {
                    Theme.surfaceRaised
                    Image(systemName: "camera")
                        .font(.title2)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay { Circle().strokeBorder(Theme.line, lineWidth: 1) }
    }
}
