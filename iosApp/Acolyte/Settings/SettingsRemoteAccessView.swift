import SwiftUI

struct SettingsRemoteAccessView: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        let url = SDKClient.shared.settingsRemoteAccessURL()
        ContentUnavailableView {
            Label(L10n.tr("settings.keys.remote.title"), systemImage: "globe")
        } description: {
            Text(L10n.tr("settings.keys.remote.text"))
        } actions: {
            if let url {
                Button(L10n.tr("settings.keys.remote.open")) { openURL(url) }
                    .buttonStyle(.borderedProminent)
            }
        }
        .navigationTitle(L10n.tr("settings.page.remote.access"))
    }
}
