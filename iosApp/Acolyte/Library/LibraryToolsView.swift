import SwiftUI

struct LibraryToolsView: View {
    @State private var tools: [LibraryToolID] = SDKClient.shared.libraryTools()

    var body: some View {
        Group {
            if tools.isEmpty {
                EmptyStateView(systemImage: "wrench.and.screwdriver")
            } else {
                List {
                    ForEach(tools) { tool in
                        row(tool)
                    }
                }
            }
        }
        .navigationTitle(L10n.tr("library.tools"))
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func row(_ tool: LibraryToolID) -> some View {
        switch tool {
        case .stats:
            NavigationLink(value: SectionID.stats) {
                LibraryToolsRow(
                    symbol: "chart.bar",
                    title: L10n.tr("library.grid.tool.stats"),
                    about: L10n.tr("library.grid.tool.stats.about")
                )
            }
        case .deleted:
            NavigationLink(value: Route.libraryDeleted(titleId: 0)) {
                LibraryToolsRow(
                    symbol: "trash",
                    title: L10n.tr("library.grid.tool.deleted"),
                    about: L10n.tr("library.grid.tool.deleted.about")
                )
            }
        case .copies:
            NavigationLink(value: Route.libraryCopies) {
                LibraryToolsRow(
                    symbol: "film.stack",
                    title: L10n.tr("library.grid.tool.copies"),
                    about: L10n.tr("library.grid.tool.copies.about")
                )
            }
        case .held:
            NavigationLink(value: Route.libraryReleases(turnedDown: false)) {
                LibraryToolsRow(
                    symbol: "hourglass",
                    title: L10n.tr("library.grid.tool.held"),
                    about: L10n.tr("library.grid.tool.held.about")
                )
            }
        case .turnedDown:
            NavigationLink(value: Route.libraryReleases(turnedDown: true)) {
                LibraryToolsRow(
                    symbol: "hand.thumbsdown",
                    title: L10n.tr("library.grid.tool.turned.down"),
                    about: L10n.tr("library.grid.tool.turned.down.about")
                )
            }
        case .missingSubtitles:
            NavigationLink(value: Route.librarySubtitles) {
                LibraryToolsRow(
                    symbol: "captions.bubble",
                    title: L10n.tr("library.grid.tool.missing.subtitles"),
                    about: L10n.tr("library.grid.tool.missing.subtitles.about")
                )
            }
        case .rename:
            NavigationLink(value: Route.libraryRename) {
                LibraryToolsRow(
                    symbol: "pencil",
                    title: L10n.tr("library.grid.tool.rename"),
                    about: L10n.tr("library.grid.tool.rename.about")
                )
            }
        case .importLibrary:
            NavigationLink(value: Route.libraryImport) {
                LibraryToolsRow(
                    symbol: "folder.badge.plus",
                    title: L10n.tr("library.grid.tool.import"),
                    about: L10n.tr("library.grid.tool.import.about")
                )
            }
        case .importFiles:
            NavigationLink(value: Route.libraryImportFiles(path: "")) {
                LibraryToolsRow(
                    symbol: "tray.and.arrow.down",
                    title: L10n.tr("library.grid.tool.import.files"),
                    about: L10n.tr("library.grid.tool.import.files.about")
                )
            }
        case .libraryFolders:
            NavigationLink(value: Route.settingsLibraryFolders) {
                LibraryToolsRow(
                    symbol: "folder",
                    title: L10n.tr("library.grid.tool.folders"),
                    about: L10n.tr("library.grid.tool.folders.about")
                )
            }
        }
    }
}

private struct LibraryToolsRow: View {
    let symbol: String
    let title: String
    let about: String

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            Image(systemName: symbol)
                .font(.title3)
                .foregroundStyle(Theme.accentText)
                .frame(width: 28)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(title)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Theme.text)
                Text(about)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
        .accessibilityElement(children: .combine)
    }
}
