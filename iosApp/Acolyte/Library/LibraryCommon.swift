import SwiftUI

enum LibraryText {
    static func plural(_ key: String, _ count: Int, _ args: CVarArg...) -> String {
        let resolved = count == 1 ? key + ".one" : key + ".other"
        return String(format: NSLocalizedString(resolved, comment: ""), arguments: [count] + args)
    }

    static func bytes(_ value: Int64) -> String {
        let units = ["B", "KB", "MB", "GB", "TB"]
        var size = Double(max(value, 0))
        var unit = 0
        while size >= 1024, unit < units.count - 1 {
            size /= 1024
            unit += 1
        }
        let digits = unit > 0 && size < 10 ? 1 : 0
        return size.formatted(.number.precision(.fractionLength(digits))) + " " + units[unit]
    }

    static func day(_ iso: String) -> String {
        guard let date = Format.date(iso: iso) else { return "" }
        var style = Date.FormatStyle(date: .abbreviated, time: .omitted)
        style.timeZone = TimeZone(identifier: "UTC") ?? .current
        return date.formatted(style)
    }

    static func dateTime(_ iso: String) -> String {
        guard let date = Format.date(iso: iso) else { return "" }
        return date.formatted(date: .abbreviated, time: .shortened)
    }

    static func ago(_ iso: String) -> String {
        guard let date = Format.date(iso: iso) else { return "" }
        return Format.relative(date)
    }

    static func kind(_ kind: LibraryKindID) -> String {
        switch kind {
        case .movie: L10n.tr("library.kind.movie")
        case .show: L10n.tr("library.kind.show")
        case .artist: L10n.tr("library.kind.music")
        }
    }

    static func codec(_ id: String) -> String {
        switch id {
        case "unknown": L10n.tr("library.codec.unknown")
        case "h264": "H.264"
        case "hevc": "HEVC"
        case "av1": "AV1"
        case "vp9": "VP9"
        case "mpeg2": "MPEG-2"
        case "xvid": "Xvid"
        case "vc1": "VC-1"
        default: id.uppercased()
        }
    }

    static func error(_ error: ErrorInfo) -> String {
        if (error.kind == .badRequest || error.kind == .conflict), !error.message.isEmpty {
            return error.localized + "\n" + L10n.format("library.server.says", error.message)
        }
        return error.localized
    }

    static func notice(_ notice: LibraryNoticeInfo) -> String {
        if notice.kind == .failed {
            if notice.name == "missing" { return L10n.tr("library.notice.folder.missing") }
            return notice.error.map { LibraryText.error($0) } ?? L10n.tr("error.unknown")
        }
        if notice.partly, notice.total > 0 {
            var text = L10n.format("library.notice.partly", notice.count, notice.total, notice.total - notice.count)
            if let failure = notice.error { text += "\n" + LibraryText.error(failure) }
            return text
        }
        let n = notice.count
        switch notice.kind {
        case .kept: return plural("library.notice.kept", n)
        case .unkept: return plural("library.notice.unkept", n)
        case .rulesSet: return plural("library.notice.rules", n, notice.name)
        case .audioSet: return plural("library.notice.audio", n)
        case .searching: return plural("library.notice.searching", n)
        case .refreshing: return plural("library.notice.refreshing", n)
        case .moving: return plural("library.notice.moving", n, notice.name)
        case .removed: return plural("library.notice.removed", n)
        case .copiesSearching: return plural("library.notice.copies", n)
        case .missingSearching: return plural("library.notice.missing", n)
        case .subtitlesSearching: return plural("library.notice.subtitles", n)
        case .filterSaved: return L10n.format("library.notice.filter.saved", notice.name)
        case .restored: return plural("library.notice.restored", n)
        case .swapped: return L10n.format("library.notice.swapped", notice.name)
        case .restoreFailed:
            let base = L10n.format("library.notice.restore.failed", notice.name)
            return notice.error.map { base + "\n" + $0.message } ?? base
        case .takenOff: return L10n.tr("library.notice.taken.off")
        case .cleared: return plural("library.notice.cleared", n)
        case .renamed: return plural("library.notice.renamed", n)
        case .renameChanged: return L10n.tr("library.rename.changed")
        case .adopted: return plural("library.notice.adopted", n)
        case .tidied: return plural("library.notice.tidied", n)
        case .untidied: return plural("library.notice.untidied", n)
        case .flatOn: return L10n.tr("library.notice.flat")
        case .imported: return plural("library.notice.imported", n)
        case .folderAdded: return L10n.tr("library.notice.folder.added")
        case .folderChecked: return L10n.tr("library.notice.folder.checked")
        case .folderRemoved: return L10n.tr("library.notice.folder.removed")
        case .pathChanged: return L10n.tr("library.notice.path.changed")
        case .importFoldersSaved: return L10n.tr("library.notice.import.folders")
        case .sideFilesSaved: return L10n.tr("library.notice.side.files")
        case .handsOffOn: return L10n.tr("library.notice.hands.off.on")
        case .handsOffOff: return L10n.tr("library.notice.hands.off.off")
        case .titleDeleted: return L10n.format("library.notice.title.deleted", notice.name)
        case .failed: return L10n.tr("error.unknown")
        }
    }
}

private struct LibraryNoticeModifier: ViewModifier {
    let notice: LibraryNoticeInfo?
    let dismiss: @MainActor (Int) -> Void

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let notice {
                    Text(LibraryText.notice(notice))
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(Theme.Spacing.medium)
                        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous)
                                .strokeBorder(notice.kind == .failed ? Theme.fail.opacity(0.45) : Theme.line, lineWidth: 1)
                        }
                        .padding(Theme.Spacing.screen)
                        .onTapGesture { dismiss(notice.seq) }
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .accessibilityAddTraits(.isStaticText)
                }
            }
            .animation(.smooth(duration: 0.24), value: notice?.seq)
            .task(id: notice?.seq) {
                guard let seq = notice?.seq else { return }
                try? await Task.sleep(for: .seconds(5))
                dismiss(seq)
            }
    }
}

extension View {
    func libraryNotice(_ notice: LibraryNoticeInfo?, dismiss: @escaping @MainActor (Int) -> Void) -> some View {
        modifier(LibraryNoticeModifier(notice: notice, dismiss: dismiss))
    }
}

struct LibraryPosterThumb: View {
    let source: String?
    let name: String
    var width: CGFloat = 40

    var body: some View {
        AuthImage(source: source, size: .thumb) {
            ArtPlaceholderView(name: name)
        }
        .frame(width: width, height: width * 1.5)
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
        .accessibilityHidden(true)
    }
}

struct LibraryPager: View {
    let from: Int
    let to: Int
    let total: Int
    let hasPrevious: Bool
    let hasNext: Bool
    let previous: @MainActor () -> Void
    let next: @MainActor () -> Void

    var body: some View {
        HStack {
            Button(L10n.tr("library.previous"), systemImage: "chevron.left") { previous() }
                .labelStyle(.iconOnly)
                .disabled(!hasPrevious)
            Spacer()
            Text(L10n.format("library.page", from, to, total))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
                .monospacedDigit()
            Spacer()
            Button(L10n.tr("library.next"), systemImage: "chevron.right") { next() }
                .labelStyle(.iconOnly)
                .disabled(!hasNext)
        }
        .buttonStyle(.bordered)
    }
}
