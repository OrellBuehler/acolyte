import SwiftUI

struct MoreView: View {
    let account: AccountInfo
    @Environment(AppModel.self) private var model
    @State private var confirmSignOut = false

    var body: some View {
        List {
            ForEach(account.moreGroups) { group in
                Section {
                    ForEach(group.sections) { section in
                        NavigationLink(value: section) {
                            row(section)
                        }
                        .badge(section == .requests ? model.inbox.requestsBadge : 0)
                    }
                } header: {
                    if let title = group.group.title {
                        Text(title)
                    }
                }
            }
            Section {
                Button(L10n.tr("account.sign.out"), role: .destructive) {
                    confirmSignOut = true
                }
            }
        }
        .navigationTitle(account.moreTab.title)
        .confirmationDialog(L10n.tr("account.sign.out.title"), isPresented: $confirmSignOut, titleVisibility: .visible) {
            Button(L10n.tr("account.sign.out"), role: .destructive) { model.signOut() }
        } message: {
            Text(L10n.tr("account.sign.out.message"))
        }
    }

    @ViewBuilder
    private func row(_ section: SectionID) -> some View {
        if section == .account {
            Label {
                VStack(alignment: .leading) {
                    Text(section.title)
                    Text(L10n.format("account.summary", account.displayName, account.serverName))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            } icon: {
                Image(systemName: section.symbol)
            }
        } else {
            Label(section.title, systemImage: section.symbol)
        }
    }
}
