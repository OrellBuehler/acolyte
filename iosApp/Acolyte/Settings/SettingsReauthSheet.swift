import SwiftUI

struct SettingsReauthSheet: View {
    let info: SettingsReauthInfo
    let confirm: (String, String) -> Void
    let cancel: () -> Void
    @State private var password = ""
    @State private var code = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(L10n.tr("settings.reauth.text"))
                    SecureField(L10n.tr("settings.reauth.password"), text: $password)
                        .textContentType(.password)
                    TextField(L10n.tr("settings.reauth.code"), text: $code)
                        .keyboardType(.numberPad)
                        .textContentType(.oneTimeCode)
                }
                if let error = info.error {
                    Section { NoticeBanner(text: error.localized) }
                }
            }
            .navigationTitle(L10n.tr("settings.reauth.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("settings.reauth.cancel"), action: cancel)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("settings.reauth.confirm")) { confirm(password, code) }
                        .disabled(password.isEmpty || info.busy)
                }
            }
        }
        .presentationDetents([.medium])
    }
}

extension View {
    func settingsReauth(
        _ info: SettingsReauthInfo,
        confirm: @escaping (String, String) -> Void,
        cancel: @escaping () -> Void
    ) -> some View {
        sheet(isPresented: Binding(get: { info.needed }, set: { if !$0 { cancel() } })) {
            SettingsReauthSheet(info: info, confirm: confirm, cancel: cancel)
        }
    }
}
