import SwiftUI

struct MainTabs: View {
    let account: AccountInfo
    @State private var selection: TabID

    init(account: AccountInfo) {
        self.account = account
        _selection = State(initialValue: account.tabs.first.map { TabID.section($0) } ?? .more)
    }

    var body: some View {
        TabView(selection: $selection) {
            ForEach(account.tabs) { section in
                Tab(section.title, systemImage: section.symbol, value: TabID.section(section)) {
                    NavigationStack {
                        SectionScreen(section: section, account: account)
                            .navigationDestination(for: SectionID.self) { SectionScreen(section: $0, account: account) }
                            .navigationDestination(for: Route.self) { RouteScreen(route: $0, account: account) }
                    }
                }
            }
            Tab(account.moreTab.title, systemImage: account.moreTab.symbol, value: TabID.more) {
                NavigationStack {
                    MoreView(account: account)
                        .navigationDestination(for: SectionID.self) { SectionScreen(section: $0, account: account) }
                        .navigationDestination(for: Route.self) { RouteScreen(route: $0, account: account) }
                }
            }
        }
    }
}
