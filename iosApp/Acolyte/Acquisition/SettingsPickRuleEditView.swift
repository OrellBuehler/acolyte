import SwiftUI

struct SettingsPickRuleEditView: View {
    let id: Int64
    let from: Int64
    let account: AccountInfo

    var body: some View {
        EmptyStateView(title: L10n.tr("placeholder.title"), message: L10n.tr("placeholder.message"), systemImage: "gearshape")
            .navigationTitle(L10n.tr("acq.page.quality"))
    }
}
