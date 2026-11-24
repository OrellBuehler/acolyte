import SwiftUI

struct MainTabs: View {
    let account: AccountInfo
    @Environment(AppModel.self) private var model
    @State private var selection: TabID

    init(account: AccountInfo) {
        self.account = account
        _selection = State(initialValue: account.tabs.first.map { TabID.section($0) } ?? .more)
    }

    var body: some View {
        TabView(selection: $selection) {
            ForEach(account.tabs) { section in
                Tab(section.title, systemImage: section.symbol, value: TabID.section(section)) {
                    RoutedStack(account: account) {
                        SectionScreen(section: section, account: account)
                    }
                }
                .badge(section == .requests ? model.inbox.requestsBadge : 0)
            }
            Tab(account.moreTab.title, systemImage: account.moreTab.symbol, value: TabID.more) {
                RoutedStack(account: account) {
                    MoreView(account: account)
                }
            }
            .badge(!account.tabs.contains(.requests) && model.inbox.hasAttention ? model.inbox.requestsBadge : 0)
        }
    }
}
