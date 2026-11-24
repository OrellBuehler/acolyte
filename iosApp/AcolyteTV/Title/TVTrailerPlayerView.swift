import AVKit
import SwiftUI

struct TVTrailerPlayerView: View {
    let source: TrailerSourceInfo
    let store: TitleStore
    @State private var player = AVPlayer()
    @State private var failed = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            VideoPlayer(player: player).ignoresSafeArea()
            if failed {
                VStack(spacing: 28) {
                    Image(systemName: "play.slash")
                        .font(.system(size: 80))
                        .foregroundStyle(Theme.textMuted)
                        .accessibilityHidden(true)
                    Text(L10n.tr("tv.trailer.unavailable"))
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                        .multilineTextAlignment(.center)
                    Button(L10n.tr("title.close")) { dismiss() }
                        .buttonStyle(.borderedProminent)
                }
                .padding(60)
                .background(Theme.background)
            }
        }
        .preferredColorScheme(.dark)
        .task { await play() }
        .onExitCommand {
            player.pause()
            dismiss()
        }
        .onDisappear { player.pause() }
    }

    private var streams: [ImageRequestInfo] {
        var out: [ImageRequestInfo] = []
        if let url = URL(string: source.hls), !source.hls.isEmpty { out.append(ImageRequestInfo(url: url, headers: [:])) }
        if let url = URL(string: source.mp4), !source.mp4.isEmpty { out.append(ImageRequestInfo(url: url, headers: [:])) }
        if !source.relay.isEmpty, let relay = store.mediaRequest(source.relay) { out.append(relay) }
        return out
    }

    private func play() async {
        for stream in streams {
            let options: [String: Any]? = stream.headers.isEmpty ? nil : ["AVURLAssetHTTPHeaderFieldsKey": stream.headers]
            let item = AVPlayerItem(asset: AVURLAsset(url: stream.url, options: options))
            player.replaceCurrentItem(with: item)
            player.play()
            if await ready(item) { return }
        }
        player.pause()
        failed = true
    }

    private func ready(_ item: AVPlayerItem) async -> Bool {
        for _ in 0 ..< 60 {
            switch item.status {
            case .readyToPlay: return true
            case .failed: return false
            default: try? await Task.sleep(for: .milliseconds(200))
            }
            if Task.isCancelled { return true }
        }
        return false
    }
}
