import Observation
import SwiftUI

@MainActor
@Observable
final class NoticeState {
    private(set) var text: String?
    private(set) var serial = 0

    func show(_ text: String) {
        self.text = text
        serial += 1
    }

    func clear() {
        text = nil
    }
}

struct NoticeToastHost: View {
    let state: NoticeState

    var body: some View {
        VStack {
            Spacer()
            if let text = state.text {
                NoteToast(text: text, undo: nil)
                    .accessibilityAddTraits(.isStaticText)
            }
        }
        .allowsHitTesting(false)
        .animation(.smooth(duration: 0.24), value: state.text)
        .task(id: state.serial) {
            guard state.text != nil else { return }
            do {
                try await Task.sleep(for: .seconds(4))
            } catch {
                return
            }
            state.clear()
        }
    }
}

extension View {
    func noticeToast(_ state: NoticeState) -> some View {
        overlay { NoticeToastHost(state: state) }
    }
}

struct PersonAvatar: View {
    let source: String?
    let name: String
    var side: CGFloat = 40

    var body: some View {
        AuthImage(source: source, size: .thumb) {
            ArtPlaceholderView(name: name)
        }
        .frame(width: side, height: side)
        .clipShape(Circle())
    }
}

struct AccountFlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var widest: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            widest = max(widest, x - spacing)
        }
        return CGSize(width: widest, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), anchor: .topLeading, proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

struct QRCodeView: View {
    private let cells: [CGPoint]
    private let modules: Int

    init(rows: [String]) {
        var points: [CGPoint] = []
        var widest = rows.count
        for (y, row) in rows.enumerated() {
            let characters = Array(row)
            widest = max(widest, characters.count)
            for (x, character) in characters.enumerated() where character == "1" {
                points.append(CGPoint(x: x, y: y))
            }
        }
        cells = points
        modules = widest
    }

    var body: some View {
        let cells = self.cells
        let count = CGFloat(modules + 8)
        Canvas { context, size in
            let side = min(size.width, size.height)
            let cell = side / count
            context.fill(Path(CGRect(x: 0, y: 0, width: side, height: side)), with: .color(.white))
            for point in cells {
                let rect = CGRect(x: (point.x + 4) * cell, y: (point.y + 4) * cell, width: cell, height: cell)
                context.fill(Path(rect), with: .color(.black))
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: 240)
        .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
        .accessibilityLabel(L10n.tr("myaccount.two.factor.qr"))
    }
}

enum MyErrorText {
    nonisolated static func text(_ error: ErrorInfo) -> String {
        switch error.kind {
        case .unauthorized: L10n.tr("reauth.error.wrong")
        default: AccountErrorText.text(error)
        }
    }
}

enum PeopleText {
    nonisolated static func provider(_ provider: String) -> String {
        switch provider {
        case "plex": "Plex"
        case "jellyfin": "Jellyfin"
        case "emby": "Emby"
        default: provider.capitalized
        }
    }

    nonisolated static func permission(_ perm: String) -> String {
        switch perm {
        case "ask": L10n.tr("people.perm.ask")
        case "auto_approve": L10n.tr("people.perm.auto.approve")
        case "auto_approve_four_k": L10n.tr("people.perm.auto.approve.four.k")
        case "approve": L10n.tr("people.perm.approve")
        case "manage_library": L10n.tr("people.perm.manage.library")
        case "settings": L10n.tr("people.perm.settings")
        default: perm
        }
    }

    nonisolated static func permissionWord(_ perm: String) -> String {
        switch perm {
        case "ask": L10n.tr("people.word.ask")
        case "auto_approve": L10n.tr("people.word.auto.approve")
        case "auto_approve_four_k": L10n.tr("people.word.auto.approve.four.k")
        case "approve": L10n.tr("people.word.approve")
        case "manage_library": L10n.tr("people.word.manage.library")
        case "settings": L10n.tr("people.word.settings")
        default: perm
        }
    }

    nonisolated static func role(_ role: RoleKind) -> String {
        switch role {
        case .owner: L10n.tr("role.owner")
        case .admin: L10n.tr("role.admin")
        case .member: L10n.tr("role.member")
        case .unknown: L10n.tr("role.unknown")
        }
    }

    nonisolated static func preset(_ preset: PeoplePresetKind) -> String {
        switch preset {
        case .admin: L10n.tr("people.preset.admin")
        case .manager: L10n.tr("people.preset.manager")
        case .member: L10n.tr("people.preset.member")
        case .kid: L10n.tr("people.preset.kid")
        case .custom: L10n.tr("people.preset.custom")
        }
    }

    nonisolated static func ssoKind(_ kind: String) -> String {
        kind == "forward_auth" ? L10n.tr("myaccount.sso.proxy") : L10n.tr("myaccount.sso.oidc")
    }

    nonisolated static func permissionLine(_ user: PeopleUserInfo) -> String {
        if user.role == .owner { return L10n.tr("people.perms.everything") }
        if user.perms.isEmpty { return L10n.tr("people.perms.nothing") }
        return user.perms.map { permissionWord($0) }.joined(separator: ", ")
    }

    nonisolated static func linkLine(_ user: PeopleUserInfo) -> String {
        user.links.map { "\(provider($0.provider)): \($0.name)" }.joined(separator: " · ")
    }

    nonisolated static func seenLine(_ user: PeopleUserInfo) -> String {
        if let seen = user.lastSeen { return L10n.format("people.seen", Format.relative(seen)) }
        return L10n.tr("people.not.seen")
    }
}
