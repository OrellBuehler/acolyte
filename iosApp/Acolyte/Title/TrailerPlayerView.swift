import AVKit
import SwiftUI

struct TrailerPlayerView: View {
    let source: TrailerSourceInfo
    let store: TitleStore
    @State private var player = AVPlayer()
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Theme.background.ignoresSafeArea()
            VideoPlayer(player: player).ignoresSafeArea()
            Button {
                player.pause()
                dismiss()
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.title)
                    .foregroundStyle(Theme.onArt)
                    .accessibilityLabel(L10n.tr("title.close"))
            }
            .padding(Theme.Spacing.large)
            if !source.youTubeURL.isEmpty {
                Button { openYouTube() } label: {
                    Label(L10n.tr("title.watch.on.youtube"), systemImage: "arrow.up.right.square")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Theme.onArt)
                        .padding(.horizontal, Theme.Spacing.medium)
                        .padding(.vertical, Theme.Spacing.small)
                        .background(Theme.artScrim, in: Capsule())
                }
                .padding(Theme.Spacing.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
        }
        .preferredColorScheme(.dark)
        .task { await play() }
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
        openYouTube()
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

    private func openYouTube() {
        player.pause()
        if let url = URL(string: source.youTubeURL), !source.youTubeURL.isEmpty { openURL(url) }
        dismiss()
    }
}
