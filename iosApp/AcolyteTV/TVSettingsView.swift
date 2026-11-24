import SwiftUI

struct TVSettingsView: View {
    let account: AccountInfo
    @Environment(TVAppModel.self) private var model
    @State private var confirmSignOut = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    LabeledContent(L10n.tr("tv.settings.server"), value: account.serverName.isEmpty ? account.serverURL : account.serverName)
                    LabeledContent(
                        L10n.tr("tv.settings.account"),
                        value: account.displayName.isEmpty ? account.userName : account.displayName
                    )
                }
                Section {
                    Button(L10n.tr("tv.settings.sign.out"), role: .destructive) {
                        confirmSignOut = true
                    }
                }
            }
            .confirmationDialog(L10n.tr("account.sign.out.title"), isPresented: $confirmSignOut) {
                Button(L10n.tr("account.sign.out"), role: .destructive) { model.signOut() }
            } message: {
                Text(L10n.tr("account.sign.out.message"))
            }
        }
    }
}
