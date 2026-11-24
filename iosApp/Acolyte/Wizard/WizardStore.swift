import Foundation
import Observation

enum WizardErrorSource {
    case idle
    case wizard
    case links
}

@MainActor
@Observable
final class WizardStore {
    private(set) var info: WizardInfo = .initial
    private(set) var links: WizardLinksInfo = .initial
    private(set) var plexWaiting = false
    private(set) var errorSource: WizardErrorSource = .idle

    let reauth: ReauthStore
    let securityReauth: ReauthStore

    @ObservationIgnored private let driver: AccountWizardDriver
    @ObservationIgnored private var plexTask: Task<Void, Never>?
    @ObservationIgnored private var plexRun = 0

    init() {
        let driver = SDKClient.shared.makeAccountWizard()
        self.driver = driver
        reauth = ReauthStore(driver: driver.reauth)
        securityReauth = ReauthStore(driver: driver.securityReauth)
    }

    var error: ErrorInfo? {
        switch errorSource {
        case .idle: nil
        case .wizard: info.error
        case .links: links.error
        }
    }

    var busy: Bool {
        info.busy || links.busy || plexWaiting
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func watchLinks() async {
        await driver.watchLinks { [weak self] value in
            self?.links = value
        }
    }

    func load() async {
        await driver.load()
    }

    func discover() {
        let driver = self.driver
        Task { await driver.discover() }
    }

    func linkServer(_ provider: WizardProviderKind, address: String, name: String, password: String) {
        errorSource = .wizard
        let driver = self.driver
        Task { await driver.linkServer(provider, address: address, name: name, password: password) }
    }

    func linkPlex() {
        guard plexTask == nil else { return }
        errorSource = .links
        driver.clearLinkError()
        let driver = self.driver
        plexRun += 1
        let run = plexRun
        plexWaiting = true
        plexTask = Task { [weak self] in
            await driver.linkPlex()
            guard let self, self.plexRun == run else { return }
            self.plexTask = nil
            self.plexWaiting = false
        }
    }

    func cancelPlex() {
        plexRun += 1
        plexTask?.cancel()
        plexTask = nil
        plexWaiting = false
    }

    func unlink(_ provider: WizardProviderKind) {
        errorSource = .links
        let driver = self.driver
        Task {
            if provider == .plex {
                await driver.unlinkPlex()
            } else {
                await driver.unlinkMedia(provider)
            }
        }
    }

    func next() {
        errorSource = .idle
        driver.next()
    }

    func back() {
        errorSource = .idle
        driver.back()
    }

    func finish() {
        errorSource = .wizard
        let driver = self.driver
        Task { await driver.finish(skipped: true) }
    }

    func close() {
        cancelPlex()
    }
}
