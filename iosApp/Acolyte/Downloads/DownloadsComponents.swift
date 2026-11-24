import Observation
import SwiftUI

struct DlToastMessage: Identifiable, Equatable {
    let id: Int
    let text: String
    let tone: ToneKind
}

@MainActor
@Observable
final class DlToastModel {
    private(set) var message: DlToastMessage?

    @ObservationIgnored private var counter = 0
    @ObservationIgnored private var task: Task<Void, Never>?

    func show(_ text: String, tone: ToneKind) {
        counter += 1
        let id = counter
        message = DlToastMessage(id: id, text: text, tone: tone)
        task?.cancel()
        task = Task { [weak self] in
            try? await Task.sleep(for: .seconds(3.5))
            guard !Task.isCancelled else { return }
            self?.dismiss(id)
        }
    }

    func dismiss(_ id: Int) {
        if message?.id == id { message = nil }
    }
}

struct DownloadsToast: View {
    let message: DlToastMessage

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.small) {
            Circle()
                .fill(Theme.dotColor(message.tone))
                .frame(width: 8, height: 8)
                .padding(.top, 6)
            Text(message.text)
                .font(.callout)
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.leading)
        }
        .padding(.horizontal, Theme.Spacing.large)
        .padding(.vertical, Theme.Spacing.medium)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
                .strokeBorder(Theme.lineStrong, lineWidth: 1)
        }
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.bottom, Theme.Spacing.small)
        .accessibilityElement(children: .combine)
    }
}

struct DlToastHost: View {
    let model: DlToastModel

    var body: some View {
        ZStack {
            if let message = model.message {
                DownloadsToast(message: message)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.smooth(duration: 0.25), value: model.message)
    }
}

struct DlChipButton: View {
    let text: String
    let selected: Bool
    var textColor: Color?
    let action: @MainActor () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.subheadline.weight(.medium))
                .lineLimit(1)
                .foregroundStyle(selected ? Theme.onAccent : (textColor ?? Theme.text))
                .padding(.horizontal, Theme.Spacing.medium)
                .padding(.vertical, 7)
                .background(selected ? Theme.accent : Theme.surfaceRaised, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

struct DlChoiceRow: View {
    let text: String
    var detail: String?
    var trailing: String?
    let selected: Bool
    let action: @MainActor () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: Theme.Spacing.medium) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(text)
                        .foregroundStyle(Theme.text)
                    if let detail {
                        Text(detail)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                Spacer(minLength: Theme.Spacing.small)
                if let trailing {
                    Text(trailing)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Image(systemName: "checkmark")
                    .foregroundStyle(Theme.accent)
                    .opacity(selected ? 1 : 0)
                    .accessibilityHidden(true)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

struct DlChipRow<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Theme.Spacing.small) {
                content()
            }
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.xSmall)
        }
    }
}

extension DlTitleLink {
    var route: Route {
        if libraryId > 0 {
            return openLibrary ? .libraryTab(id: libraryId, kind: kind, tmdb: tmdb) : .libraryTitle(id: libraryId, kind: kind, tmdb: tmdb)
        }
        return .title(kind: kind, tmdb: tmdb)
    }
}

struct DlRouteDestination: View {
    let route: Route
    @Environment(AppModel.self) private var model

    var body: some View {
        if case let .signedIn(account) = model.session {
            RouteScreen(route: route, account: account)
        }
    }
}

struct DlInlineEmpty: View {
    let title: String
    var message: String?
    var systemImage = "tray"
    var actionTitle: String?
    var action: (@MainActor () -> Void)?

    var body: some View {
        VStack(spacing: Theme.Spacing.small) {
            Image(systemName: systemImage)
                .font(.largeTitle)
                .foregroundStyle(Theme.textFaint)
                .accessibilityHidden(true)
            Text(title)
                .font(.headline)
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.center)
            if let message {
                Text(message)
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            }
            if let action, let actionTitle {
                Button(actionTitle, action: action)
                    .buttonStyle(.bordered)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Theme.Spacing.xLarge)
        .padding(.horizontal, Theme.Spacing.screen)
    }
}

struct DlPosterThumb: View {
    let source: String?
    let name: String
    var square = false
    var width: CGFloat = 40

    var body: some View {
        AuthImage(source: source, size: .poster) {
            ArtPlaceholderView(name: name)
        }
        .frame(width: width, height: square ? width : width * 1.5)
        .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.poster / 2, style: .continuous))
    }
}

extension BarToneKind {
    var color: Color {
        switch self {
        case .go, .done: Theme.ok
        case .queued: Theme.info
        case .held: Theme.wait
        case .bad: Theme.textFaint
        }
    }
}

extension ToneKind {
    var labelColor: Color {
        switch self {
        case .ok: Theme.okText
        case .fail: Theme.failText
        case .wait: Theme.waitText
        case .info: Theme.infoText
        case .idle: Theme.textMuted
        }
    }
}

struct DlFactRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: Theme.Spacing.medium) {
            Text(label)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .frame(maxWidth: 130, alignment: .leading)
            Text(value)
                .font(.footnote)
                .foregroundStyle(Theme.text)
                .frame(maxWidth: .infinity, alignment: .leading)
                .textSelection(.enabled)
        }
        .accessibilityElement(children: .combine)
    }
}

struct DlSheetSection<Content: View>: View {
    let title: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct DlPanel<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            content()
        }
        .padding(Theme.Spacing.medium)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}

struct DlDayHeader: View {
    let title: String
    var subtitle: String?
    var highlighted = false

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.headline)
                .foregroundStyle(highlighted ? Theme.accentText : Theme.text)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

extension ErrorInfo {
    var detailed: String {
        message.isEmpty ? localized : localized + " " + message
    }
}
