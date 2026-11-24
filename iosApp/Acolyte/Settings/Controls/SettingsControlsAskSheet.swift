import SwiftUI

struct SettingsControlsAskSheet: View {
    let ask: SettingsControlsAskInfo
    let busy: Bool
    let confirm: () -> Void
    let cancel: () -> Void

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Theme.Spacing.large) {
                Text(ask.text)
                    .font(.body)
                    .foregroundStyle(Theme.text)
                Text(L10n.format("settings.controls.expires", ask.expiresInSeconds))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Button(role: .destructive, action: confirm) {
                    HStack {
                        if busy { ProgressView() }
                        Text(SettingsControlsText.yes(ask.kind, ask.name))
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accentFill)
                .disabled(busy)
                Button(L10n.tr("common.cancel"), action: cancel)
                    .frame(maxWidth: .infinity)
                Spacer()
            }
            .padding(Theme.Spacing.screen)
            .navigationTitle(L10n.format("settings.controls.ask.title", SettingsControlsText.label(ask.kind, ask.name)))
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium])
    }
}

extension View {
    func settingsControlsAsk(
        _ ask: SettingsControlsAskInfo?,
        busy: Bool,
        confirm: @escaping () -> Void,
        cancel: @escaping () -> Void
    ) -> some View {
        sheet(isPresented: Binding(get: { ask != nil }, set: { if !$0 { cancel() } })) {
            if let ask {
                SettingsControlsAskSheet(ask: ask, busy: busy, confirm: confirm, cancel: cancel)
            }
        }
    }

    func settingsControlsToast(_ toaster: SettingsControlsToaster) -> some View {
        safeAreaInset(edge: .bottom) {
            if let text = toaster.text {
                NoteToast(text: text, undo: nil)
            }
        }
        .animation(.default, value: toaster.text)
    }
}
