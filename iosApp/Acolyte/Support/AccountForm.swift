import SafariServices
import SwiftUI

struct FormHeading: View {
    let title: String
    var subtitle: String?

    var body: some View {
        VStack(spacing: Theme.Spacing.small) {
            Text(title)
                .font(.title.weight(.bold))
                .multilineTextAlignment(.center)
                .accessibilityAddTraits(.isHeader)
            if let subtitle {
                Text(subtitle)
                    .font(.body)
                    .foregroundStyle(Theme.textMuted)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

struct FormFieldStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(.horizontal, Theme.Spacing.medium)
            .frame(minHeight: 44)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
                    .strokeBorder(Theme.line, lineWidth: 1)
            }
    }
}

extension View {
    func formField() -> some View {
        modifier(FormFieldStyle())
    }
}

struct FormPrimaryButton: View {
    let title: String
    let busy: Bool
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Text(title).opacity(busy ? 0 : 1)
                if busy {
                    ProgressView().tint(Theme.onAccent)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(!enabled || busy)
    }
}

struct FormPasswordField: View {
    let title: String
    @Binding var text: String
    var contentType: UITextContentType? = .password
    var submitLabel: SubmitLabel = .done
    @State private var shown = false

    var body: some View {
        HStack(spacing: Theme.Spacing.small) {
            Group {
                if shown {
                    TextField(title, text: $text)
                } else {
                    SecureField(title, text: $text)
                }
            }
            .textContentType(contentType)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(submitLabel)
            Button {
                shown.toggle()
            } label: {
                Image(systemName: shown ? "eye.slash" : "eye")
                    .foregroundStyle(Theme.textMuted)
                    .frame(minWidth: 32, minHeight: 32)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(L10n.tr(shown ? "form.hide.password" : "form.show.password"))
        }
        .formField()
    }
}

struct FormHint: View {
    let text: String
    var failed = false

    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(failed ? Theme.failText : Theme.textMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct FormDivider: View {
    var body: some View {
        HStack(spacing: Theme.Spacing.medium) {
            Rectangle().fill(Theme.line).frame(height: 1)
            Text(L10n.tr("form.or"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Rectangle().fill(Theme.line).frame(height: 1)
        }
        .accessibilityElement(children: .ignore)
    }
}

struct PlexWaitingPanel: View {
    let onCancel: () -> Void

    var body: some View {
        VStack(spacing: Theme.Spacing.medium) {
            ProgressView()
            Text(L10n.tr("signin.plex.waiting.title"))
                .font(.title3.weight(.semibold))
                .accessibilityAddTraits(.isHeader)
            Text(L10n.tr("signin.plex.waiting.message"))
                .font(.body)
                .foregroundStyle(Theme.textMuted)
                .multilineTextAlignment(.center)
            Button(L10n.tr("common.cancel"), action: onCancel)
                .buttonStyle(.bordered)
                .controlSize(.large)
        }
        .frame(maxWidth: .infinity)
        .padding(Theme.Spacing.large)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }
}

struct BrowserTarget: Identifiable, Hashable {
    let url: URL

    var id: URL { url }
}

struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }

    func updateUIViewController(_ controller: SFSafariViewController, context: Context) {}
}

private struct PlexBrowserModifier: ViewModifier {
    let url: URL?
    @State private var shown: BrowserTarget?

    func body(content: Content) -> some View {
        content
            .onChange(of: url) { _, new in
                shown = new.map { BrowserTarget(url: $0) }
            }
            .sheet(item: $shown) { target in
                SafariView(url: target.url)
                    .ignoresSafeArea()
            }
    }
}

extension View {
    func plexBrowser(url: URL?) -> some View {
        modifier(PlexBrowserModifier(url: url))
    }
}

enum AccountErrorText {
    static func text(_ error: ErrorInfo) -> String {
        switch error.kind {
        case .unauthorized: L10n.tr("account.error.wrong.credentials")
        case .rateLimited:
            if let retry = error.retryAfter { L10n.format("signin.error.rate.limited", retry) } else { L10n.tr("error.rate.limited") }
        case .conflict: L10n.tr("account.error.conflict")
        case .forbidden: L10n.tr("account.error.forbidden")
        default: error.localized
        }
    }
}
