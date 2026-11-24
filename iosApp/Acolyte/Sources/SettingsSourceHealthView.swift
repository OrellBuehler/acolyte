import SwiftUI

struct SettingsSourceHealthView: View {
    let id: Int64
    let account: AccountInfo
    @State private var store: SourceHealthStore

    init(id: Int64, account: AccountInfo) {
        self.id = id
        self.account = account
        _store = State(initialValue: SourceHealthStore(id: id))
    }

    private var info: SourceHealthInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { [store] in await store.refresh() }) {
                    EmptyView()
                } content: {
                    if let row = info.row {
                        content(row)
                    }
                }
            }
        }
        .navigationTitle(L10n.format("acq.src.health.title", info.row?.name ?? ""))
        .navigationBarTitleDisplayMode(.inline)
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private func content(_ row: SourceRowInfo) -> some View {
        List {
            Section {
                header(row)
            }
            Section {
                tiles(row)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
            }
            overTime
            recent
        }
    }

    private func header(_ row: SourceRowInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            let uses = SourcesWords.uses(feed: row.feed, auto: row.autoSearch, hand: row.handSearch)
            Text(
                L10n.format(
                    "acq.src.health.subtitle",
                    row.formatLabel,
                    SourcesWords.protocolName(row.protocolName),
                    row.priority,
                    uses
                )
            )
            .font(.subheadline)
            .foregroundStyle(Theme.textMuted)
            HStack(spacing: Theme.Spacing.medium) {
                StatusChip(text: SourcesWords.word(row), tone: row.tone)
                if row.canTryNow && info.canEdit {
                    Button {
                        store.tryNow()
                    } label: {
                        if info.trying {
                            HStack(spacing: 6) {
                                ProgressView()
                                Text(L10n.tr("acq.src.testing"))
                            }
                        } else {
                            Text(L10n.tr("acq.src.try.now"))
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .disabled(info.trying)
                }
            }
        }
    }

    private func tile(_ label: String, _ value: String, @ViewBuilder detail: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(Theme.textMuted)
            Text(value).font(.headline)
            detail()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.medium)
        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }

    private func tiles(_ row: SourceRowInfo) -> some View {
        let lastAnswer = SourcesWords.ago(row.lastSuccess) ?? L10n.tr("acq.src.health.never")
        let lastFailure = SourcesWords.ago(row.lastFailure)
        return Grid(horizontalSpacing: Theme.Spacing.small, verticalSpacing: Theme.Spacing.small) {
            GridRow {
                tile(L10n.tr("acq.src.health.state"), SourcesWords.word(row)) { stateDetail(row) }
                tile(L10n.tr("acq.src.health.last.answer"), lastAnswer) {
                    Text(lastFailure.map { L10n.format("acq.src.health.last.failure", $0) } ?? L10n.tr("acq.src.health.no.failures"))
                        .font(.caption)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            GridRow {
                tile(
                    L10n.tr("acq.src.health.today"),
                    row.avgMs > 0 ? SourcesWords.duration(row.avgMs) : L10n.tr("acq.src.health.no.times")
                ) {
                    if row.avgMs > 0 {
                        Text(L10n.tr("acq.src.health.average")).font(.caption).foregroundStyle(Theme.textMuted)
                    }
                }
                tile(L10n.format("acq.src.health.last.days", info.days), L10n.format("acq.src.health.calls", info.totalCalls)) {
                    Text(L10n.format("acq.src.health.failed.picked", info.totalFailures, info.totalGrabs))
                        .font(.caption)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    @ViewBuilder
    private func stateDetail(_ row: SourceRowInfo) -> some View {
        let until = SourcesWords.pausedUntil(row.until).map { $0 + "." }
        let text = [row.note.isEmpty ? nil : row.note, until].compactMap { $0 }.joined(separator: " ")
        if !text.isEmpty {
            Text(text).font(.caption).foregroundStyle(Theme.textMuted)
        }
        if let test = row.test {
            Text(SourcesWords.outcome(test))
                .font(.caption)
                .foregroundStyle(test.ok ? Theme.okText : Theme.failText)
            if !test.ok, let cause = test.cause {
                Text(SourcesWords.cause(cause)).font(.caption).foregroundStyle(Theme.textMuted)
            }
        }
    }

    private var overTime: some View {
        Section(L10n.tr("acq.src.health.over.time")) {
            Picker(L10n.tr("acq.src.health.days.shown"), selection: Binding(get: { info.days }, set: { store.setDays($0) })) {
                ForEach(info.choices, id: \.self) { n in
                    Text(L10n.format("acq.src.health.days", n)).tag(n)
                }
            }
            .pickerStyle(.segmented)
            if !info.hasCalls {
                Text(L10n.tr("acq.src.health.no.calls")).font(.footnote).foregroundStyle(Theme.textMuted)
            } else {
                legend
                SourceCallBars(bars: info.bars)
                    .frame(height: 96)
                    .accessibilityElement()
                    .accessibilityLabel(L10n.tr("acq.src.health.chart.label"))
                axis
            }
            Text(L10n.tr("acq.src.health.response.time")).font(.subheadline.weight(.semibold))
            if !info.hasTimes {
                Text(L10n.tr("acq.src.health.no.times")).font(.footnote).foregroundStyle(Theme.textMuted)
            } else {
                SourceTimeBars(bars: info.bars)
                    .frame(height: 56)
                    .accessibilityElement()
                    .accessibilityLabel(L10n.tr("acq.src.health.time.label"))
                Text(SourcesWords.duration(info.bars.map(\.avgMs).max() ?? 0))
                    .font(.caption)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }

    private var legend: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: Theme.Spacing.medium) {
                dot(Theme.ok, L10n.tr("acq.src.health.answered"))
                dot(Theme.fail, L10n.tr("acq.src.health.failed"))
                dot(Theme.accent, L10n.tr("acq.src.health.picked"))
            }
            Text(L10n.tr("acq.src.health.utc")).font(.caption).foregroundStyle(Theme.textMuted)
        }
    }

    private func dot(_ color: Color, _ label: String) -> some View {
        HStack(spacing: 4) {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(label).font(.caption)
        }
    }

    private var axis: some View {
        HStack {
            Text(SourcesWords.dayLabel(info.bars.first?.day ?? ""))
            Spacer()
            Text(SourcesWords.dayLabel(info.bars.last?.day ?? ""))
        }
        .font(.caption)
        .foregroundStyle(Theme.textMuted)
    }

    private var recent: some View {
        Section(L10n.tr("acq.src.health.recent")) {
            if info.recent.isEmpty {
                Text(L10n.tr("acq.src.health.no.recent")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            ForEach(info.recent) { call in
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(SourcesWords.op(call.op, raw: call.rawOp)).font(.subheadline)
                        Text(SourcesWords.ago(call.at) ?? "").font(.caption).foregroundStyle(Theme.textMuted)
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        if call.ok {
                            Text(L10n.tr("acq.src.health.answered.cell")).font(.subheadline).foregroundStyle(Theme.okText)
                        } else {
                            Text(SourcesWords.errorWord(call.errorKind, raw: call.rawError))
                                .font(.subheadline)
                                .foregroundStyle(Theme.failText)
                        }
                        Text(SourcesWords.duration(call.ms)).font(.caption).foregroundStyle(Theme.textMuted)
                    }
                }
            }
        }
    }
}

struct SourceCallBars: View {
    let bars: [SourceBarInfo]

    var body: some View {
        GeometryReader { geo in
            HStack(alignment: .bottom, spacing: 1) {
                ForEach(bars) { bar in
                    barView(bar, height: geo.size.height)
                }
            }
        }
    }

    private func barView(_ bar: SourceBarInfo, height: CGFloat) -> some View {
        let total = height * CGFloat(bar.callsPercent) / 100
        let failed = height * CGFloat(bar.failedPercent) / 100
        let picked = height * CGFloat(bar.grabsPercent) / 100
        return VStack(spacing: 0) {
            Spacer(minLength: 0)
            Rectangle().fill(Theme.fail).frame(height: failed)
            Rectangle().fill(Theme.ok).frame(height: max(total - failed, 0))
        }
        .frame(maxWidth: .infinity)
        .overlay(alignment: .bottom) {
            if bar.grabs > 0 {
                Circle()
                    .fill(Theme.accent)
                    .frame(width: 5, height: 5)
                    .offset(y: 2.5 - picked)
            }
        }
    }
}

struct SourceTimeBars: View {
    let bars: [SourceBarInfo]

    var body: some View {
        GeometryReader { geo in
            HStack(alignment: .bottom, spacing: 1) {
                ForEach(bars) { bar in
                    VStack(spacing: 0) {
                        Spacer(minLength: 0)
                        Rectangle()
                            .fill(Theme.accent)
                            .frame(height: geo.size.height * CGFloat(bar.timePercent) / 100)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }
}
