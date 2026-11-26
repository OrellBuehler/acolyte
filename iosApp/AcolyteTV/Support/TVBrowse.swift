import SwiftUI

struct TVBrowsePage<Header: View, Content: View>: View {
    let hero: DiscoverCardInfo?
    var pills: [String] = []
    let lookup: [String: DiscoverCardInfo]
    var initialFocus: String?
    @ViewBuilder var header: () -> Header
    @ViewBuilder var content: (FocusState<String?>.Binding) -> Content
    @State private var ambient: DiscoverCardInfo?
    @FocusState private var focus: String?

    var body: some View {
        let shown = ambient ?? hero
        ZStack {
            TVBackdropLayer(source: shown?.wideImage)
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Theme.TV.sectionGap) {
                    VStack(alignment: .leading, spacing: 0) {
                        header()
                        Spacer(minLength: 0)
                        if let shown {
                            TVBillboard(card: shown, pills: shown.id == hero?.id ? pills : [])
                                .padding(.bottom, 20)
                        }
                    }
                    .containerRelativeFrame(.vertical, alignment: .bottomLeading) { height, _ in height * 0.62 }
                    content($focus)
                }
            }
        }
        .screenshotFocus($focus, id: initialFocus)
        .onChange(of: focus) { _, new in
            guard let new, let card = lookup[new] else { return }
            ambient = card
        }
        .onChange(of: hero?.id) { _, _ in ambient = nil }
    }
}

struct TVAmbientPage<Content: View>: View {
    let lookup: [String: String?]
    var fallback: String?
    @ViewBuilder var content: (FocusState<String?>.Binding) -> Content
    @State private var ambient: String?
    @FocusState private var focus: String?

    var body: some View {
        ZStack {
            TVBackdropLayer(source: ambient ?? fallback, blurred: true)
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Theme.TV.sectionGap) {
                    content($focus)
                }
            }
        }
        .onChange(of: focus) { _, new in
            guard let new, let entry = lookup[new], let image = entry else { return }
            ambient = image
        }
    }
}

enum TVFocusLookup {
    static func cards(_ rows: [CardRowInfo]) -> [String: DiscoverCardInfo] {
        var result: [String: DiscoverCardInfo] = [:]
        for row in rows {
            for card in row.cards {
                result[TVFocusKey.id(row: row.id, card: card.id)] = card
            }
        }
        return result
    }

    static func images(_ rows: [CardRowInfo]) -> [String: String?] {
        cards(rows).mapValues { $0.wideImage }
    }
}

extension CardRowInfo {
    static func local(id: String, title: String, subtitle: String = "", cards: [DiscoverCardInfo], wide: Bool = true) -> CardRowInfo {
        CardRowInfo(id: id, type: id, title: title, subtitle: subtitle, cards: cards, wide: wide, ranked: false, seeAll: false)
    }
}

struct TVChipRow<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 24) {
                content()
            }
            .padding(.vertical, Theme.TV.focusPad)
        }
        .scrollClipDisabled()
        .focusSection()
    }
}

struct TVTypeChips: View {
    let type: ExploreTypeID
    let select: (ExploreTypeID) -> Void

    var body: some View {
        ForEach(ExploreTypeID.allCases) { item in
            TVChip(title: DiscoverText.type(item), selected: item == type) { select(item) }
        }
    }
}

struct TVBrowseTile: View {
    let name: String
    let image: String?
    var hue = 0
    var label: String?
    let action: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.TV.textGap) {
            Button(action: action) {
                let color = Theme.tileHues[min(max(hue, 0), Theme.tileHues.count - 1)]
                ZStack(alignment: .bottomLeading) {
                    AuthImage(source: image, size: .backdrop) { color.opacity(0.5) }
                    LinearGradient(colors: [.clear, Theme.artScrimStrong], startPoint: .center, endPoint: .bottom)
                    VStack(alignment: .leading, spacing: 2) {
                        if let label {
                            Text(label)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Theme.onArt.opacity(0.84))
                        }
                        Text(name)
                            .font(.callout.weight(.bold))
                            .foregroundStyle(Theme.onArt)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }
                    .padding(16)
                }
                .frame(width: Theme.TV.backdropWidth, height: Theme.TV.backdropHeight)
            }
            .buttonStyle(.card)
            .accessibilityLabel(name)
        }
        .frame(width: Theme.TV.backdropWidth, alignment: .leading)
    }
}

extension TVNavigator {
    func openBrowse(_ link: BrowseLinkInfo) {
        if link.target == .genre {
            open(.genre(type: link.type, genre: link.genre))
        } else {
            open(.browse(link))
        }
    }
}

enum TVGenreHue {
    static func of(_ name: String) -> Int {
        var hash: UInt32 = 0x811C_9DC5
        for unit in name.lowercased().unicodeScalars {
            hash ^= UInt32(unit.value & 0xFFFF)
            hash = hash &* 0x0100_0193
        }
        return Int(hash % 12)
    }
}
