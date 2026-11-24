import Foundation
import Observation

@MainActor
@Observable
final class CalendarStore {
    private(set) var info: CalendarInfo = .initial
    var route: Route?
    let toasts = DlToastModel()

    private let driver = SDKClient.shared.makeCalendar()
    @ObservationIgnored private var seenNotice: Int64 = 0

    func watch() async {
        await driver.watch { [weak self] value in
            self?.apply(value)
        }
    }

    func load() async {
        await driver.load()
    }

    func followLive() async {
        await driver.followLive()
    }

    func refresh() async {
        await driver.refresh()
    }

    private func apply(_ value: CalendarInfo) {
        info = value
        if let notice = value.notice, notice.id != seenNotice {
            seenNotice = notice.id
            toasts.show(notice.message, tone: .ok)
            driver.consumeNotice(notice.id)
        }
        if let error = value.actionError {
            toasts.show(error.detailed, tone: .fail)
            driver.clearActionError()
        }
    }

    func setView(_ view: CalendarViewKind) {
        driver.setView(view)
    }

    func setType(_ type: CalendarTypeKind) {
        driver.setType(type)
    }

    func setFollows(_ follows: CalendarFollowsKind) {
        driver.setFollows(follows)
    }

    func select(_ day: DlDay) {
        driver.select(day)
    }

    func next() {
        let driver = self.driver
        Task { await driver.next() }
    }

    func previous() {
        let driver = self.driver
        Task { await driver.previous() }
    }

    func today() {
        let driver = self.driver
        Task { await driver.goToday() }
    }

    func follow(_ group: CalendarGroupInfo, on: Bool) {
        let driver = self.driver
        let key = group.key
        Task { await driver.follow(groupKey: key, on: on) }
    }

    func search(_ group: CalendarGroupInfo) {
        let driver = self.driver
        let key = group.key
        Task { await driver.search(groupKey: key) }
    }

    func searchMissing() {
        let driver = self.driver
        Task { await driver.searchMissing() }
    }

    func describe(_ group: CalendarGroupInfo) async -> [String: String] {
        await driver.describe(groupKey: group.key)
    }

    func link(_ group: CalendarGroupInfo) -> DlTitleLink? {
        driver.link(groupKey: group.key)
    }
}
