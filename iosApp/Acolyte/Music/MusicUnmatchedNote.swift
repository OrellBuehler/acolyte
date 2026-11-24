import SwiftUI

struct MusicUnmatchedNote: View {
    @State private var store = MusicUnmatchedStore()
    @Environment(\.openRoute) private var openRoute
    @Environment(\.scenePhase) private var scenePhase

    private var info: MusicUnmatchedInfo { store.info }

    var body: some View {
        content
            .task { await store.watch() }
            .task { await store.load() }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { Task { await store.load() } }
            }
    }

    @ViewBuilder
    private var content: some View {
        if info.total > 0 {
            HStack(alignment: .top, spacing: Theme.Spacing.small) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(Theme.waitText)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.plural("music.unmatched.files", info.total))
                        .font(.callout.weight(.bold))
                        .foregroundStyle(Theme.text)
                    ForEach(info.shown) { artist in
                        Button(L10n.format("music.unmatched.link", artist.name, artist.files)) {
                            openRoute(.musicLibraryArtist(titleId: artist.titleId, tab: "library"))
                        }
                        .font(.callout)
                        .foregroundStyle(Theme.accentText)
                        .multilineTextAlignment(.leading)
                    }
                    if info.more > 0 {
                        Text(L10n.format("music.unmatched.more", info.more))
                            .font(.callout)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(Theme.Spacing.medium)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
            .padding(.horizontal, Theme.Spacing.screen)
            .padding(.vertical, Theme.Spacing.small)
            .accessibilityElement(children: .contain)
        }
    }
}
