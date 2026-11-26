import Foundation
import Observation

@MainActor
@Observable
final class AppModel {
    private(set) var session: SessionSnapshot = .restoring
    private(set) var live: LiveState = .off
    private(set) var inbox: InboxInfo = .empty

    private let client = SDKClient.shared
    @ObservationIgnored private let inboxDriver = SDKClient.shared.makeInbox()
    @ObservationIgnored private var started = false
    @ObservationIgnored private var lastRefresh = Date()

    func start() async {
        guard !started else { return }
        started = true
        let client = self.client
        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                await client.watchSession { [weak self] value in
                    self?.session = value
                    if case .signedIn = value { self?.refreshInbox() }
                }
            }
            group.addTask {
                await client.watchLive { [weak self] value in
                    self?.live = value
                }
            }
            group.addTask {
                await client.restore()
                #if DEBUG
                await ScreenshotLaunch.signIn()
                #endif
            }
            let inboxDriver = self.inboxDriver
            group.addTask {
                await inboxDriver.watch { [weak self] value in
                    self?.inbox = value
                }
            }
            group.addTask {
                await inboxDriver.poll()
            }
        }
    }

    func foreground() {
        guard case .signedIn = session, Date().timeIntervalSince(lastRefresh) > 30 else { return }
        lastRefresh = Date()
        let client = self.client
        Task { await client.refresh() }
    }

    func refreshInbox() {
        let inboxDriver = self.inboxDriver
        Task { await inboxDriver.refresh() }
    }

    func signOut() {
        let client = self.client
        Task { await client.signOut() }
    }
}
