import SwiftUI

struct TVCollectionView: View {
    @State private var store: SeriesStore

    init(id: String) {
        _store = State(initialValue: SeriesStore(id: id))
    }

    var body: some View {
        let info = store.info
        ZStack {
            Theme.background.ignoresSafeArea()
            TVStatusContent(load: info.load, isEmpty: info.isEmpty, reload: { await store.refresh() }) {
                TVEmptyView(systemImage: "film.stack")
            } content: {
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        Text(info.name.isEmpty ? L10n.tr("discover.series.title") : info.name)
                            .font(.title2.bold())
                            .foregroundStyle(Theme.text)
                            .accessibilityAddTraits(.isHeader)
                        if let error = info.load.error {
                            TVNoticeBanner(text: error.localized)
                        }
                        ForEach(info.movies) { movie in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(movie.year > 0 ? "\(movie.name) (\(movie.year))" : movie.name)
                                    .font(.callout.weight(.semibold))
                                    .foregroundStyle(Theme.text)
                                Text(L10n.plural("discover.series.versions", movie.versions) + " · " + L10n.plural("discover.series.files", movie.files))
                                    .font(.footnote)
                                    .foregroundStyle(Theme.textMuted)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(20)
                            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.TV.cornerRadius, style: .continuous))
                            .focusable()
                        }
                    }
                    .padding(.vertical, Theme.TV.focusPad)
                }
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }
}
