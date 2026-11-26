import SwiftUI
import UIKit

struct AuthImage<Placeholder: View>: View {
    let source: String?
    var size: ImageSizeKind = .poster
    @ViewBuilder var placeholder: () -> Placeholder

    @State private var image: UIImage?

    var body: some View {
        ZStack {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .transition(.opacity)
            } else {
                placeholder()
            }
        }
        .clipped()
        .task(id: source) {
            image = nil
            guard let request = SDKClient.shared.imageRequest(source, size: size) else { return }
            let loaded = await ImageLoader.shared.load(request)
            withAnimation(.easeOut(duration: 0.24)) { image = loaded?.image }
        }
    }
}

struct LoadedImage: @unchecked Sendable {
    let image: UIImage
}

actor ImageLoader {
    static let shared = ImageLoader()

    private let cache = NSCache<NSURL, UIImage>()
    private let session: URLSession

    init() {
        let configuration = URLSessionConfiguration.default
        configuration.httpCookieStorage = nil
        configuration.httpShouldSetCookies = false
        configuration.httpCookieAcceptPolicy = .never
        configuration.urlCache = URLCache(memoryCapacity: 16 << 20, diskCapacity: 200 << 20)
        session = URLSession(configuration: configuration)
    }

    func load(_ request: ImageRequestInfo) async -> LoadedImage? {
        if let cached = cache.object(forKey: request.url as NSURL) {
            return LoadedImage(image: cached)
        }
        var urlRequest = URLRequest(url: request.url)
        for (name, value) in request.headers {
            urlRequest.setValue(value, forHTTPHeaderField: name)
        }
        guard let result = try? await session.data(for: urlRequest), let http = result.1 as? HTTPURLResponse else { return nil }
        let data = result.0
        await SDKClient.shared.storeCookies(ImageLoader.setCookies(http, url: request.url), from: request.url)
        guard http.statusCode == 200, let image = UIImage(data: data) else { return nil }
        cache.setObject(image, forKey: request.url as NSURL)
        return LoadedImage(image: image)
    }

    private static func setCookies(_ response: HTTPURLResponse, url: URL) -> [String] {
        guard let fields = response.allHeaderFields as? [String: String] else { return [] }
        return HTTPCookie.cookies(withResponseHeaderFields: fields, for: url).map { cookie in
            var header = "\(cookie.name)=\(cookie.value); Path=\(cookie.path)"
            if let expires = cookie.expiresDate {
                header += "; Max-Age=\(max(0, Int(expires.timeIntervalSinceNow)))"
            }
            return header
        }
    }
}
