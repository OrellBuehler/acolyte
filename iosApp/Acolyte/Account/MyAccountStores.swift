import Foundation
import Observation

@MainActor
@Observable
final class MyProfileStore {
    private(set) var info: MyProfileInfo = .initial

    let notices = NoticeState()

    private let driver = SDKClient.shared.makeAccountProfile()

    func watch() async {
        await driver.watch { [weak self] value in
            self?.apply(value)
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func saveName(_ name: String) {
        let driver = self.driver
        Task { await driver.saveName(name) }
    }

    func saveLanguage(_ code: String) {
        let driver = self.driver
        Task { await driver.saveLanguage(code) }
    }

    func uploadPicture(_ data: Data) {
        let driver = self.driver
        Task { await driver.uploadPicture(data) }
    }

    func removePicture() {
        let driver = self.driver
        Task { await driver.removePicture() }
    }

    func saveAlerts(email: String, discordId: String, newEpisodes: Bool) {
        let driver = self.driver
        Task { await driver.saveAlerts(email: email, discordId: discordId, newEpisodes: newEpisodes) }
    }

    func unfollow(_ id: String) {
        let driver = self.driver
        Task { await driver.unfollow(id) }
    }

    private func apply(_ value: MyProfileInfo) {
        info = value
        guard let saved = value.saved else { return }
        notices.show(Self.text(saved))
        driver.consume()
    }

    nonisolated static func text(_ saved: MyProfileSavedKind) -> String {
        switch saved {
        case .name: L10n.tr("myaccount.saved.name")
        case .picture: L10n.tr("myaccount.saved.picture")
        case .pictureRemoved: L10n.tr("myaccount.saved.picture.removed")
        case .language: L10n.tr("myaccount.saved.language")
        case .alerts: L10n.tr("myaccount.saved.alerts")
        case .unfollowed: L10n.tr("myaccount.saved.unfollowed")
        }
    }

    nonisolated static func problemText(_ problem: MyProfileProblemKind) -> String {
        switch problem {
        case .nameInvalid: L10n.tr("myaccount.problem.name.invalid")
        case .pictureTooBig: L10n.tr("myaccount.problem.picture.too.big")
        case .pictureEmpty: L10n.tr("picture.error")
        }
    }
}

@MainActor
@Observable
final class MySecurityStore {
    private(set) var info: MySecurityInfo = .initial
    private(set) var plexWaiting = false
    private(set) var successSerial = 0

    let reauth: ReauthStore
    let notices = NoticeState()

    private let driver: MySecurityDriver
    @ObservationIgnored private var plexTask: Task<Void, Never>?
    @ObservationIgnored private var plexRun = 0

    init() {
        let driver = SDKClient.shared.makeAccountSecurity()
        self.driver = driver
        reauth = ReauthStore(driver: driver.reauth)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.apply(value)
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func changePassword(current: String, new: String, confirm: String) {
        let driver = self.driver
        Task { await driver.changePassword(current: current, new: new, confirm: confirm) }
    }

    func startTwoFactor() {
        let driver = self.driver
        Task { await driver.startTwoFactor() }
    }

    func cancelTwoFactor() {
        driver.cancelTwoFactor()
    }

    func enableTwoFactor(_ code: String) {
        let driver = self.driver
        Task { await driver.enableTwoFactor(code) }
    }

    func disableTwoFactor(_ code: String) {
        let driver = self.driver
        Task { await driver.disableTwoFactor(code) }
    }

    func renewBackupCodes(_ code: String) {
        let driver = self.driver
        Task { await driver.renewBackupCodes(code) }
    }

    func dismissBackupCodes() {
        driver.dismissBackupCodes()
    }

    func clearProblems() {
        driver.consume()
    }

    func linkPlex() {
        guard plexTask == nil else { return }
        driver.consume()
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

    func unlinkPlex() {
        let driver = self.driver
        Task { await driver.unlinkPlex() }
    }

    func linkMedia(serverId: Int64, name: String, password: String) {
        let driver = self.driver
        Task { await driver.linkMedia(serverId: serverId, name: name, password: password) }
    }

    func unlinkMedia(_ kind: String) {
        let driver = self.driver
        Task { await driver.unlinkMedia(kind) }
    }

    func unlinkSso(_ id: String) {
        let driver = self.driver
        Task { await driver.unlinkSso(id) }
    }

    private func apply(_ value: MySecurityInfo) {
        let enrolled = info.setup != nil && value.setup == nil && !value.backupCodes.isEmpty
        info = value
        if enrolled {
            notices.show(L10n.tr("myaccount.two.factor.on.notice"))
            successSerial += 1
        }
        guard let notice = value.notice else { return }
        notices.show(Self.text(notice, name: value.noticeText))
        successSerial += 1
        driver.consume()
    }

    nonisolated static func text(_ notice: MySecurityNoticeKind, name: String) -> String {
        switch notice {
        case .passwordChanged: L10n.tr("myaccount.notice.password.changed")
        case .passwordSet: L10n.tr("myaccount.notice.password.set")
        case .twoFactorOff: L10n.tr("myaccount.notice.two.factor.off")
        case .codesRenewed: L10n.tr("myaccount.notice.codes.renewed")
        case .plexLinked: L10n.format("myaccount.notice.plex.linked", name)
        case .plexUnlinked: L10n.tr("myaccount.notice.plex.unlinked")
        case .mediaLinked: L10n.tr("myaccount.notice.media.linked")
        case .mediaUnlinked: L10n.tr("myaccount.notice.media.unlinked")
        case .ssoUnlinked: L10n.tr("myaccount.notice.sso.unlinked")
        }
    }

    nonisolated static func problemText(_ problem: MySecurityProblemKind, minimum: Int) -> String {
        switch problem {
        case .passwordShort: L10n.format("myaccount.password.short", minimum)
        case .passwordsDiffer: L10n.tr("myaccount.password.differ")
        case .codeInvalid: L10n.tr("myaccount.two.factor.code.invalid")
        case .plexTimeout: L10n.tr("wizard.plex.timeout")
        }
    }

    func close() {
        cancelPlex()
    }
}

@MainActor
@Observable
final class MyDevicesStore {
    private(set) var info: MyDevicesInfo = .initial

    let userId: Int64?
    let notices = NoticeState()

    private let driver: MyDevicesDriver

    init(userId: Int64?) {
        self.userId = userId
        driver = SDKClient.shared.makeAccountDevices(userId: userId)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.apply(value)
        }
    }

    func load() async {
        await driver.load()
    }

    func refresh() async {
        await driver.refresh()
    }

    func end(_ id: String) {
        let driver = self.driver
        Task { await driver.end(id) }
    }

    func endAll() {
        let driver = self.driver
        Task { await driver.endAll() }
    }

    private func apply(_ value: MyDevicesInfo) {
        info = value
        guard let ended = value.ended else { return }
        notices.show(ended == 0 ? L10n.tr("myaccount.devices.ended.none") : L10n.plural("myaccount.devices.ended", ended))
        driver.consume()
    }
}

@MainActor
@Observable
final class AiConnectStore {
    private(set) var info: AiConnectInfo = .initial

    let reauth: ReauthStore

    private let driver: AiConnectDriver

    init(link: String) {
        let driver = SDKClient.shared.makeAiConnect(link)
        self.driver = driver
        reauth = ReauthStore(driver: driver.reauth)
    }

    func watch() async {
        await driver.watch { [weak self] value in
            self?.info = value
        }
    }

    func load() async {
        await driver.load()
    }

    func allow() {
        let driver = self.driver
        Task { await driver.allow() }
    }
}
