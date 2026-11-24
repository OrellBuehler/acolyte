import SwiftUI

struct AskLinesView: View {
    let ask: AskInfo
    let approver: Bool
    let myId: Int64

    var body: some View {
        let askedBy = L10n.format("requests.asked.by", RequestsWords.names(ask.askers))
        muted(ask.askedAt.map { "\(askedBy) · \(Format.relative($0))" } ?? askedBy)
        if !ask.viaPlugin.isEmpty { muted(L10n.format("requests.via", ask.viaPlugin)) }
        if let first = ask.watchlist.first {
            if ask.watchlist.count > 1 {
                muted(L10n.format("requests.watchlist.many", ask.watchlist.count))
            } else if first.id == myId {
                muted(L10n.tr("requests.watchlist.yours"))
            } else {
                muted(L10n.format("requests.watchlist.of", first.name))
            }
        }
        if ask.autoApproved, ask.state != "asked" {
            muted(L10n.tr("requests.auto.approved"))
        } else if ask.state == "turned_down", !ask.decider.isEmpty {
            muted(L10n.format("requests.declined.by", ask.decider))
        } else if ask.state != "asked", ask.state != "turned_down", !ask.decider.isEmpty {
            muted(L10n.format("requests.approved.by", ask.decider))
        } else if ask.state == "turned_down", let at = ask.decidedAt {
            muted(L10n.format("requests.declined.at", Format.relative(at)))
        }
        if !ask.note.isEmpty {
            Text("“\(ask.note)”")
                .font(.footnote.italic())
                .lineLimit(3)
        }
        if approver, !ask.alsoDeclined.isEmpty {
            muted(L10n.format("requests.declined.before", RequestsWords.names(ask.alsoDeclined)))
        }
        if ask.isFailed {
            Text(ask.problem.isEmpty ? L10n.tr("requests.failed.not.added") : ask.problem)
                .font(.footnote)
                .foregroundStyle(Theme.failText)
        } else if ask.state == "approved" {
            muted(RequestsWords.step(ask) ?? RequestsWords.keeps(ask))
            if approver, !ask.stepText.isEmpty { muted(ask.stepText) }
        }
    }

    private func muted(_ text: String) -> some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
    }
}
