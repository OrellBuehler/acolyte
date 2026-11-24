import SwiftUI

struct BrandMark: View {
    var size: CGFloat = 96

    var body: some View {
        Image("BrandMark")
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: size * 0.22, style: .continuous))
            .accessibilityHidden(true)
    }
}

struct StatusChip: View {
    let text: String
    let tone: ToneKind
    var onArt = false

    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(Theme.dotColor(tone))
                .frame(width: 7, height: 7)
            Text(text)
                .font(.footnote.weight(.medium))
                .foregroundStyle(onArt ? Theme.onArt : Theme.textColor(tone))
                .lineLimit(1)
        }
        .padding(.leading, 8)
        .padding(.trailing, 10)
        .frame(height: 24)
        .background(onArt ? Theme.artScrim : Theme.surfaceRaised, in: Capsule())
    }
}

struct SectionHeader: View {
    let title: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        HStack {
            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(1)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: Theme.Spacing.small)
            if let action {
                Button(actionTitle ?? L10n.tr("common.see.all"), action: action)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.accentText)
            }
        }
    }
}

struct ArtPlaceholderView: View {
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

struct PosterCard<Badges: View>: View {
    let title: String
    var subtitle: String?
    let image: String?
    var width: CGFloat = 120
    var progress: Double?
    var mark: CardMarkInfo?
    var fresh: String?
    @ViewBuilder var badges: () -> Badges

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            AuthImage(source: image, size: width > 160 ? .posterLarge : .poster) {
                ArtPlaceholderView(name: title)
            }
            .frame(width: width, height: width * 1.5)
            .overlay(alignment: .topLeading) {
                HStack(spacing: 4) { badges() }.padding(8)
            }
            .overlay(alignment: .bottom) {
                if let progress {
                    ProgressView(value: min(max(progress, 0), 1))
                        .tint(Theme.accent)
                        .padding(8)
                } else if let fresh {
                    FreshTabView(text: fresh)
                }
            }
            .overlay(alignment: .topTrailing) {
                if let mark {
                    OwnedMarkView(mark: mark).padding(8)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.poster, style: .continuous))
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
                .lineLimit(1)
                .padding(.top, 8)
            if let subtitle {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                    .lineLimit(1)
            }
        }
        .frame(width: width, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

extension PosterCard where Badges == EmptyView {
    init(title: String, subtitle: String? = nil, image: String?, width: CGFloat = 120, progress: Double? = nil) {
        self.init(title: title, subtitle: subtitle, image: image, width: width, progress: progress) { EmptyView() }
    }
}

struct NoticeBanner: View {
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
        .padding(Theme.Spacing.medium)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
                .strokeBorder(Theme.fail.opacity(0.45), lineWidth: 1)
        }
    }
}

struct LoadingStateView: View {
    var body: some View {
        ProgressView()
            .controlSize(.large)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityLabel(L10n.tr("common.loading"))
    }
}

struct ErrorStateView: View {
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

struct EmptyStateView: View {
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

struct StatusContent<Content: View, Empty: View>: View {
    let load: LoadInfo
    let isEmpty: Bool
    let reload: @MainActor @Sendable () async -> Void
    @ViewBuilder var empty: () -> Empty
    @ViewBuilder var content: () -> Content

    var body: some View {
        switch load.phase {
        case .loading:
            LoadingStateView()
        case .failed:
            let reload = self.reload
            ScrollView {
                ErrorStateView(error: load.error ?? .unknown) { Task { await reload() } }
                    .containerRelativeFrame(.vertical)
            }
            .refreshable { await reload() }
        case .ready:
            if isEmpty {
                ScrollView {
                    empty().containerRelativeFrame(.vertical)
                }
                .refreshable { await reload() }
            } else {
                content()
            }
        }
    }
}

extension ErrorInfo {
    var localized: String {
        switch kind {
        case .offline: L10n.tr("error.offline")
        case .timeout: L10n.tr("error.timeout")
        case .tls: L10n.tr("error.tls")
        case .unauthorized: L10n.tr("error.unauthorized")
        case .forbidden: L10n.tr("error.forbidden")
        case .notFound: L10n.tr("error.not.found")
        case .badRequest: L10n.tr("error.bad.request")
        case .conflict: L10n.tr("error.conflict")
        case .partOff: L10n.tr("error.part.off")
        case .handsOff: L10n.tr("error.hands.off")
        case .reauth: L10n.tr("error.reauth")
        case .rateLimited: L10n.tr("error.rate.limited")
        case .server: L10n.tr("error.server")
        case .invalidResponse: L10n.tr("error.invalid.response")
        case .unknown: L10n.tr("error.unknown")
        }
    }
}

extension SignInErrorInfo {
    var localized: String {
        switch kind {
        case .invalidAddress: L10n.tr("signin.error.invalid.address")
        case .unreachable: L10n.tr("signin.error.unreachable")
        case .tls: L10n.tr("signin.error.tls")
        case .notHolyArrs: L10n.tr("signin.error.not.holyarrs")
        case .needsSetup: L10n.tr("signin.error.needs.setup")
        case .hostRefused: L10n.tr("signin.error.host.refused")
        case .wrongCredentials: L10n.tr("signin.error.wrong.credentials")
        case .wrongCode: L10n.tr("signin.error.wrong.code")
        case .signInEnded: L10n.tr("signin.error.sign.in.ended")
        case .keyRejected: L10n.tr("signin.error.key.rejected")
        case .notAllowed: L10n.tr("signin.error.not.allowed")
        case .wayOff: L10n.tr("signin.error.way.off")
        case .rateLimited:
            if let retryAfter { L10n.format("signin.error.rate.limited", retryAfter) } else { L10n.tr("error.rate.limited") }
        case .server: L10n.tr("signin.error.server")
        }
    }
}
