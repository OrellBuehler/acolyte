import SwiftUI

struct TVBillboard: View {
    let card: DiscoverCardInfo
    var label: String?
    var pills: [String] = []

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let label {
                TVStatusChip(text: label, tone: .idle, onArt: true)
            }
            if !pills.isEmpty {
                HStack(spacing: 12) {
                    ForEach(pills, id: \.self) { TVStatusChip(text: $0, tone: .info, onArt: true) }
                }
            }
            if let logo = card.logo {
                AuthImage(source: logo, size: .posterLarge) {
                    Text(card.name).font(.title2.bold()).foregroundStyle(Theme.text)
                }
                .scaledToFit()
                .frame(maxWidth: 520, maxHeight: 150, alignment: .leading)
                .accessibilityLabel(card.name)
            } else {
                Text(card.name)
                    .font(.title2.bold())
                    .foregroundStyle(Theme.text)
                    .lineLimit(2)
            }
            let meta = metaLine
            if !meta.isEmpty {
                Text(meta)
                    .font(.callout)
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
            }
            if !card.overview.isEmpty {
                Text(card.overview)
                    .font(.footnote)
                    .foregroundStyle(Theme.text)
                    .lineLimit(3)
                    .frame(maxWidth: Theme.TV.readableWidth, alignment: .leading)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .id(card.id)
        .transition(.opacity)
        .animation(.easeInOut(duration: 0.3), value: card.id)
    }

    private var metaLine: String {
        var parts = [DiscoverText.kind(card.kind)]
        if card.year > 0 { parts.append(String(card.year)) }
        parts.append(contentsOf: card.genres.prefix(3).map(DiscoverText.genre))
        return parts.joined(separator: " · ")
    }
}
