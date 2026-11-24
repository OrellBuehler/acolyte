import SwiftUI

struct AcquisitionPagesView: View {
    @State private var pages: [AcquisitionPageID] = SDKClient.shared.acquisitionPages()

    var body: some View {
        List(pages) { page in
            NavigationLink(value: AcquisitionPagesView.route(page)) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(AcquisitionPagesView.title(page))
                    Text(AcquisitionPagesView.hint(page))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
        .navigationTitle(L10n.tr("section.settings"))
    }

    nonisolated static func route(_ page: AcquisitionPageID) -> Route {
        switch page {
        case .sources: .settingsSources
        case .downloaders: .settingsDownloaders
        case .pickRules: .settingsPickRules
        case .naming: .settingsNaming
        case .subtitles: .settingsSubtitles
        case .search: .settingsSearch
        case .move: .settingsMove
        }
    }

    static func title(_ page: AcquisitionPageID) -> String {
        switch page {
        case .sources: L10n.tr("acq.page.sources")
        case .downloaders: L10n.tr("acq.page.downloaders")
        case .pickRules: L10n.tr("acq.page.quality")
        case .naming: L10n.tr("acq.page.naming")
        case .subtitles: L10n.tr("acq.page.subtitles")
        case .search: L10n.tr("acq.page.search")
        case .move: L10n.tr("acq.page.move")
        }
    }

    static func hint(_ page: AcquisitionPageID) -> String {
        switch page {
        case .sources: L10n.tr("acq.hint.sources")
        case .downloaders: L10n.tr("acq.hint.downloaders")
        case .pickRules: L10n.tr("acq.hint.quality")
        case .naming: L10n.tr("acq.hint.naming")
        case .subtitles: L10n.tr("acq.hint.subtitles")
        case .search: L10n.tr("acq.hint.search")
        case .move: L10n.tr("acq.hint.move")
        }
    }
}
