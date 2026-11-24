import SwiftUI

struct MusicExploreContent<Header: View>: View {
    @State private var store = MusicRowsStore(kind: "", slug: "", region: SDKClient.region)
    @ViewBuilder var header: () -> Header

    var body: some View {
        VStack(spacing: 0) {
            header()
            MusicGenresBar(strip: store.info.strip)
            MusicRowsContent(info: store.info, store: store)
        }
        .task { await store.watch() }
        .task { await store.load() }
    }
}

struct MusicGenresBar: View {
    let strip: [MusicItemInfo]

    var body: some View {
        if !strip.isEmpty {
            MusicOpenerReader { opener in
                HStack {
                    Spacer(minLength: 0)
                    Menu {
                        ForEach(strip) { item in
                            Button(item.name) { opener.open(item) }
                        }
                        Button(L10n.tr("music.explore.all.genres")) { opener.browseAll() }
                    } label: {
                        Label(L10n.tr("discover.genres"), systemImage: "chevron.down")
                            .labelStyle(.titleAndIcon)
                            .font(.subheadline.weight(.semibold))
                    }
                }
                .padding(.horizontal, Theme.Spacing.screen)
                .padding(.bottom, Theme.Spacing.small)
            }
        }
    }
}
