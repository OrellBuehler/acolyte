import SwiftUI

struct MainTabs: View {
    let account: AccountInfo
    @Environment(AppModel.self) private var model
    @State private var tabs: TabSelection

    init(account: AccountInfo) {
        self.account = account
        _tabs = State(initialValue: TabSelection(account.tabs.first.map { TabID.section($0) } ?? .more))
    }

    var body: some View {
        @Bindable var selection = tabs
        TabView(selection: $selection.value) {
            ForEach(account.tabs) { section in
                Tab(section.title, systemImage: section.symbol, value: TabID.section(section)) {
                    RoutedStack(account: account, tabs: tabs) {
                        SectionScreen(section: section, account: account)
                    }
                }
                .badge(section == .requests ? model.inbox.requestsBadge : 0)
            }
            Tab(account.moreTab.title, systemImage: account.moreTab.symbol, value: TabID.more) {
                RoutedStack(account: account, tabs: tabs) {
                    MoreView(account: account)
                }
            }
            .badge(!account.tabs.contains(.requests) && model.inbox.hasAttention ? model.inbox.requestsBadge : 0)
        }
    }
}
