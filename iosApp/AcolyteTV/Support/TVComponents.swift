import SwiftUI

struct TVFocusKey {
    let binding: FocusState<String?>.Binding
    let id: String
}

extension View {
    @ViewBuilder
    func tvTrackFocus(_ key: TVFocusKey?) -> some View {
        if let key {
            focused(key.binding, equals: key.id)
        } else {
            self
        }
    }
}

struct TVArtPlaceholder: View {
    let name: String

    var body: some View {
        let info = SDKClient.shared.placeholder(name)
        let hue = info.hue / 360
        ZStack {
            Color(hue: hue, saturation: 0.48, brightness: 0.34)
            Text(info.text)
                .font(.title2.weight(.bold))
                .foregroundStyle(Color(hue: hue, saturation: 0.18, brightness: 0.9))
        }
        .accessibilityHidden(true)
    }
}

struct TVOwnedMark: View {
    let mark: CardMarkInfo

    var body: some View {
        ZStack {
            Circle().fill(fill)
            Circle().strokeBorder(Theme.markRing, lineWidth: 1)
            if mark.state == .downloading, mark.percent >= 0 {
                Circle()
                    .trim(from: 0, to: CGFloat(mark.percent) / 100)
                    .stroke(Theme.onArt, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .padding(4)
            }
            Image(systemName: symbol)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(mark.state == .requested ? Theme.markAskedGlyph : Theme.onArt)
        }
        .frame(width: 44, height: 44)
        .accessibilityLabel(DiscoverText.mark(mark))
    }

    private var fill: Color {
        switch mark.state {
        case .available, .partial: Theme.markHave
        case .downloading: Theme.markDownloading
        case .requested: Theme.markAsked
        case .declined: Theme.markNo
        }
    }

    private var symbol: String {
        switch mark.state {
        case .available: "checkmark"
        case .partial: "minus"
        case .downloading: "arrow.down"
        case .requested: "clock"
        case .declined: "xmark"
        }
    }
}

struct TVFreshTab: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundStyle(Theme.onAccent)
            .lineLimit(1)
            .padding(.horizontal, 14)
            .padding(.vertical, 4)
            .background(Theme.accentFill, in: UnevenRoundedRectangle(topLeadingRadius: 10, topTrailingRadius: 10))
    }
}

struct TVStatusChip: View {
    let text: String
    let tone: ToneKind
    var onArt = false

    var body: some View {
        HStack(spacing: 10) {
            Circle()
                .fill(Theme.dotColor(tone))
                .frame(width: 12, height: 12)
            Text(text)
                .font(.caption.weight(.semibold))
                .foregroundStyle(onArt ? Theme.onArt : Theme.textColor(tone))
                .lineLimit(1)
        }
        .padding(.leading, 14)
        .padding(.trailing, 18)
        .frame(height: 38)
        .background(onArt ? Theme.artScrim : Theme.surfaceRaised, in: Capsule())
    }
}

struct TVNoticeBanner: View {
    let text: String

    var body: some View {
        Label {
            Text(text)
                .font(.callout)
                .foregroundStyle(Theme.text)
        } icon: {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Theme.failText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(24)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.TV.cornerRadius, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: Theme.TV.cornerRadius, style: .continuous)
                .strokeBorder(Theme.fail.opacity(0.45), lineWidth: 2)
        }
    }
}

struct TVChip: View {
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            if selected {
                Label(title, systemImage: "checkmark")
            } else {
                Text(title)
            }
        }
        .buttonStyle(.bordered)
        .buttonBorderShape(.capsule)
        .tint(selected ? Theme.accent : nil)
    }
}

struct TVPosterCard: View {
    let title: String
    var subtitle: String?
    let image: String?
    var mark: CardMarkInfo?
    var fresh: String?
    var progress: Double?
    var focus: TVFocusKey?
    let action: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.TV.textGap) {
            Button(action: action) {
                AuthImage(source: image, size: .posterLarge) { TVArtPlaceholder(name: title) }
                    .frame(width: Theme.TV.posterWidth, height: Theme.TV.posterHeight)
                    .overlay(alignment: .topTrailing) {
                        if let mark { TVOwnedMark(mark: mark).padding(12) }
                    }
                    .overlay(alignment: .bottom) {
                        if let progress {
                            ProgressView(value: min(max(progress, 0), 1))
                                .tint(Theme.accent)
                                .padding(16)
                        } else if let fresh {
                            TVFreshTab(text: fresh)
                        }
                    }
            }
            .buttonStyle(.card)
            .tvTrackFocus(focus)
            .accessibilityLabel(title)
            TVCardText(title: title, subtitle: subtitle)
        }
        .frame(width: Theme.TV.posterWidth, alignment: .leading)
    }
}

struct TVChipInfo: Hashable {
    let text: String
    let tone: ToneKind
}

struct TVActionButton: View {
    let title: String
    var systemImage: String?
    var prominent = false
    let action: () -> Void

    var body: some View {
        if prominent {
            button.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
        } else {
            button.buttonStyle(.bordered).buttonBorderShape(.capsule)
        }
    }

    private var button: some View {
        Button(action: action) {
            if let systemImage {
                Label(title, systemImage: systemImage)
            } else {
                Text(title)
            }
        }
    }
}

struct TVBackdropCard: View {
    let title: String
    var subtitle: String?
    var detail: String?
    var chip: TVChipInfo?
    let image: String?
    var logo: String?
    var mark: CardMarkInfo?
    var fresh: String?
    var progress: Double?
    var badge: String?
    var showsText = true
    var focus: TVFocusKey?
    let action: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.TV.textGap) {
            Button(action: action) {
                AuthImage(source: image, size: .backdrop) { TVArtPlaceholder(name: title) }
                    .frame(width: Theme.TV.backdropWidth, height: Theme.TV.backdropHeight)
                    .overlay(alignment: .bottomLeading) {
                        if let logo {
                            ZStack(alignment: .bottomLeading) {
                                LinearGradient(colors: [.clear, Theme.artScrim], startPoint: .center, endPoint: .bottom)
                                AuthImage(source: logo, size: .poster) { Color.clear }
                                    .scaledToFit()
                                    .frame(maxWidth: 200, maxHeight: 56, alignment: .leading)
                                    .padding(16)
                            }
                        }
                    }
                    .overlay(alignment: .bottomTrailing) {
                        if let mark { TVOwnedMark(mark: mark).padding(12) }
                    }
                    .overlay(alignment: .topLeading) {
                        if let badge {
                            Text(badge)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Theme.onAccent)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 4)
                                .background(Theme.accentFill, in: Capsule())
                                .padding(12)
                        }
                    }
                    .overlay(alignment: .bottom) {
                        if let progress {
                            ProgressView(value: min(max(progress, 0), 1))
                                .tint(Theme.accent)
                                .padding(16)
                        } else if let fresh {
                            TVFreshTab(text: fresh)
                        }
                    }
            }
            .buttonStyle(.card)
            .tvTrackFocus(focus)
            .accessibilityLabel(title)
            if showsText {
                TVCardText(title: title, subtitle: subtitle)
            }
            if let chip {
                TVStatusChip(text: chip.text, tone: chip.tone)
            }
            if let detail, !detail.isEmpty {
                Text(detail)
                    .font(.footnote)
                    .foregroundStyle(Theme.text)
                    .lineLimit(3)
                    .padding(.horizontal, 4)
            }
        }
        .frame(width: Theme.TV.backdropWidth, alignment: .leading)
    }
}

struct TVPersonCard: View {
    let name: String
    var role: String?
    let photo: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: Theme.TV.textGap) {
            Button { action?() } label: {
                AuthImage(source: photo, size: .thumb) { TVArtPlaceholder(name: name) }
                    .frame(width: Theme.TV.personSize, height: Theme.TV.personSize)
                    .clipShape(Circle())
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(name)
            Text(name)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(2)
                .multilineTextAlignment(.center)
            if let role, !role.isEmpty {
                Text(role)
                    .font(.caption)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        }
        .frame(width: Theme.TV.personSize + 40)
    }
}

struct TVCardText: View {
    let title: String
    var subtitle: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.callout.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(1)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }
}

struct TVShelf<Content: View>: View {
    let title: String
    var subtitle: String?
    var seeAll: (() -> Void)?
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                    .accessibilityAddTraits(.isHeader)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                        .lineLimit(1)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: Theme.TV.gutter) {
                    content()
                    if let seeAll {
                        Button(L10n.tr("common.see.all"), systemImage: "chevron.right", action: seeAll)
                            .buttonStyle(.bordered)
                            .padding(.top, Theme.TV.focusPad)
                    }
                }
                .padding(.vertical, Theme.TV.focusPad)
            }
            .scrollClipDisabled()
        }
        .focusSection()
    }
}

struct TVLoadingView: View {
    var body: some View {
        ProgressView()
            .controlSize(.large)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityLabel(L10n.tr("common.loading"))
    }
}

struct TVErrorView: View {
    let error: ErrorInfo
    var retry: (() -> Void)?

    var body: some View {
        ContentUnavailableView {
            Label(L10n.tr("error.title"), systemImage: error.kind == .offline || error.kind == .timeout ? "wifi.slash" : "exclamationmark.triangle")
        } description: {
            Text(error.localized)
        } actions: {
            if let retry {
                Button(L10n.tr("common.retry"), action: retry)
                    .buttonStyle(.borderedProminent)
            }
        }
    }
}

struct TVEmptyView: View {
    var title = L10n.tr("empty.title")
    var message: String?
    var systemImage = "tray"

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: systemImage)
        } description: {
            if let message { Text(message) }
        }
    }
}

struct TVStatusContent<Content: View, Empty: View>: View {
    let load: LoadInfo
    let isEmpty: Bool
    let reload: @MainActor @Sendable () async -> Void
    @ViewBuilder var empty: () -> Empty
    @ViewBuilder var content: () -> Content

    var body: some View {
        switch load.phase {
        case .loading:
            TVLoadingView()
        case .failed:
            let reload = self.reload
            TVErrorView(error: load.error ?? .unknown) { Task { await reload() } }
        case .ready:
            if isEmpty {
                let reload = self.reload
                VStack(spacing: 40) {
                    empty()
                    Button(L10n.tr("tv.reload")) { Task { await reload() } }
                        .buttonStyle(.bordered)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                content()
            }
        }
    }
}
