import Observation
import SwiftUI

@MainActor
@Observable
final class TabSelection {
    var value: TabID

    init(_ value: TabID) {
        self.value = value
    }
}

@MainActor
@Observable
final class Navigator {
    let account: AccountInfo
    private let router: Router
    private let tabs: TabSelection

    init(account: AccountInfo, router: Router, tabs: TabSelection) {
        self.account = account
        self.router = router
        self.tabs = tabs
    }

    func open(_ route: Route) {
        router.open(route)
    }

    func open(_ section: SectionID) {
        if account.tabs.contains(section) {
            tabs.value = .section(section)
        } else {
            router.path.append(section)
        }
    }
}
