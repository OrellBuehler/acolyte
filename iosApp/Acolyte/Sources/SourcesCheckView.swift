import SwiftUI

struct SourceCheckSection: View {
    let store: SourceCheckStore
    @State private var kind = "movie"
    @State private var title = ""

    private var info: SourceCheckInfo { store.info }

    var body: some View {
        Section {
            Picker(L10n.tr("acq.src.check.kind"), selection: Binding(get: { kind }, set: { value in
                title = store.titleAfterKind(kind, title, value)
                kind = value
            })) {
                Text(L10n.tr("acq.src.check.movie")).tag("movie")
                Text(L10n.tr("acq.src.check.show")).tag("show")
                Text(L10n.tr("acq.src.check.music")).tag("music")
            }
            .onAppear {
                if title.isEmpty { title = store.sample(kind) }
            }
            TextField(L10n.tr("acq.src.check.title.field"), text: $title)
                .onChange(of: title) { _, value in
                    if value.count > 200 { title = String(value.prefix(200)) }
                }
            Button {
                store.run(kind, title)
            } label: {
                if info.running {
                    HStack(spacing: 6) {
                        ProgressView()
                        Text(L10n.tr("acq.src.check.asking"))
                    }
                } else {
                    Text(L10n.tr("acq.src.check.run"))
                }
            }
            .disabled(info.running)
            if let error = info.error {
                Text(error.sourceText).font(.footnote).foregroundStyle(Theme.failText)
            }
            if let result = info.result {
                resultRows(result)
            }
        } header: {
            Text(L10n.tr("acq.src.check.title"))
        } footer: {
            Text(L10n.tr("acq.src.check.description"))
        }
    }

    @ViewBuilder
    private func resultRows(_ result: SourceCheckResultInfo) -> some View {
        if result.total == 0 {
            Text(L10n.tr("acq.src.check.none")).font(.footnote).foregroundStyle(Theme.textMuted)
        } else {
            Text(summary(result)).font(.subheadline)
        }
        ForEach(result.lines) { line in
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(line.name).font(.headline)
                    Spacer()
                    StatusChip(text: SourcesWords.checkWord(line.word), tone: line.tone)
                }
                if let meta = meta(line) {
                    Text(meta).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                if !line.detail.isEmpty {
                    Text(line.detail)
                        .font(.footnote)
                        .foregroundStyle(line.word == .notConnected ? Theme.failText : Theme.textMuted)
                }
            }
        }
    }

    private func summary(_ result: SourceCheckResultInfo) -> String {
        var parts = [L10n.format("acq.src.check.summary", result.connected, result.total)]
        if result.failed > 0 { parts.append(L10n.format("acq.src.check.failed", result.failed)) }
        if result.paused > 0 { parts.append(L10n.format("acq.src.check.paused.count", result.paused)) }
        return parts.joined(separator: ", ") + "."
    }

    private func meta(_ line: SourceCheckLineInfo) -> String? {
        var parts: [String] = []
        if line.word == .connected { parts.append(L10n.plural("acq.src.check.results", line.count)) }
        if line.word != .paused && line.ms > 0 { parts.append(SourcesWords.duration(line.ms)) }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}
