import Foundation
import Observation

@MainActor
@Observable
final class HistoryStore {
    private(set) var info: HistoryInfo = .initial
    var route: Route?
    let toasts = DlToastModel()

    private let titleId: Int64
    private let episodeId: Int64
    @ObservationIgnored private var storage: HistoryDriver?
    @ObservationIgnored private var seenNotice: Int64 = 0
    @ObservationIgnored private var lastError: ErrorInfo?

    init(titleId: Int64, episodeId: Int64) {
        self.titleId = titleId
        self.episodeId = episodeId
    }

    private var driver: HistoryDriver {
        if let storage { return storage }
        let made = SDKClient.shared.makeHistory(titleId: titleId, episodeId: episodeId)
        storage = made
        return made
    }

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

    private func apply(_ value: HistoryInfo) {
        info = value
        if let notice = value.notice, value.noticeId != seenNotice {
            seenNotice = value.noticeId
            switch notice {
            case .replacing: toasts.show(L10n.tr("downloads.history.replacing"), tone: .info)
            case .noRelease: toasts.show(L10n.tr("downloads.history.no.release"), tone: .wait)
            }
            driver.consumeNotice()
        }
        if let error = value.actionError, error != lastError {
            toasts.show(error.detailed, tone: .fail)
        }
        lastError = value.actionError
    }

    func setQuery(_ query: String) {
        driver.setQuery(query)
    }

    func setFilter(_ filter: HistoryFilterInfo) {
        let driver = self.driver
        Task { await driver.setFilter(filter) }
    }

    func clearScope() {
        let driver = self.driver
        Task { await driver.clearScope() }
    }

    func showMore() {
        let driver = self.driver
        Task { await driver.showMore() }
    }

    func findAnother(_ key: String) {
        let driver = self.driver
        Task { await driver.findAnother(key: key) }
    }

    func openTitle(_ titleId: Int64) {
        let driver = self.driver
        Task {
            guard let link = await driver.titleLink(titleId: titleId) else { return }
            route = link.route
        }
    }
}
