@preconcurrency import shared
import Foundation

final class SDKClient: @unchecked Sendable {
    static let shared = SDKClient()

    private let sdk: AcolyteSdk

    private init() {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "dev"
        sdk = AcolyteSdk(store: KeychainStore(), appVersion: version)
    }

    func watchSession(deliver: @MainActor @Sendable (SessionSnapshot) -> Void) async {
        for await state in sdk.session.state {
            let mapped = Mapping.session(state)
            await deliver(mapped)
        }
    }

    func watchLive(deliver: @MainActor @Sendable (LiveState) -> Void) async {
        for await status in sdk.session.liveStatus {
            let mapped = Mapping.live(status)
            await deliver(mapped)
        }
    }

    func restore() async {
        do {
            try await sdk.session.restore()
        } catch {}
    }

    func refresh() async {
        do {
            _ = try await sdk.session.refresh()
        } catch {}
    }

    func signOut() async {
        do {
            try await sdk.session.signOut()
        } catch {}
    }

    func makeSignIn() -> SignInDriver {
        SignInDriver(flow: sdk.signIn())
    }

    func makeHealth() -> HealthDriver {
        HealthDriver(presenter: sdk.health())
    }

    func imageRequest(_ source: String?, size: ImageSizeKind) -> ImageRequestInfo? {
        guard let text = sdk.images.url(source: source, size: Mapping.kotlinSize(size)), let url = URL(string: text) else {
            return nil
        }
        return ImageRequestInfo(url: url, headers: sdk.images.headers(url: text))
    }

    func storeCookies(_ cookies: [String], from url: URL) async {
        guard !cookies.isEmpty else { return }
        do {
            try await sdk.images.storeCookies(url: url.absoluteString, setCookieHeaders: cookies)
        } catch {}
    }

    func placeholder(_ name: String) -> PlaceholderInfo {
        PlaceholderInfo(text: ArtPlaceholder.shared.text(name: name), hue: Double(ArtPlaceholder.shared.hue(name: name)))
    }
}
