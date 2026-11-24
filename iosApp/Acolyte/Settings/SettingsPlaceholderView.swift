import SwiftUI

struct SettingsPlaceholderView: View {
    let title: String

    var body: some View {
        EmptyStateView(title: L10n.tr("placeholder.title"), message: L10n.tr("placeholder.message"), systemImage: "gearshape")
            .navigationTitle(title)
    }
}
