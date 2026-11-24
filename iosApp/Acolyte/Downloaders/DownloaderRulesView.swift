import SwiftUI

struct DownloaderRulesSection: View {
    let rules: DownloaderRulesStore
    @State private var advanced = false

    private var info: DownloaderRulesInfo { rules.info }

    var body: some View {
        if info.allowed {
            switch info.load.phase {
            case .loading:
                Section { ProgressView().frame(maxWidth: .infinity) }
            case .failed:
                Section {
                    Text(info.load.error?.localized ?? L10n.tr("error.unknown"))
                        .foregroundStyle(Theme.failText)
                    Button(L10n.tr("common.retry")) { Task { await rules.refresh() } }
                }
            case .ready:
                card
            }
        }
    }

    private func toggle(_ title: String, _ kind: DownloaderSwitchKind, hint: String? = nil) -> some View {
        Toggle(isOn: Binding(get: { info.flags[kind] ?? false }, set: { rules.setSwitch(kind, $0) })) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                if let hint {
                    Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
        }
        .disabled(!info.canEdit)
    }

    private func number(_ title: String, _ kind: DownloaderNumberKind, hint: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            DownloaderNumberField(title: title, text: info.numbers[kind] ?? "", enabled: info.canEdit) { rules.setNumber(kind, $0) }
            if let hint {
                Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }

    @ViewBuilder
    private var card: some View {
        Section(L10n.tr("acq.dl.wrong")) {
            toggle(L10n.tr("acq.dl.remove.failed.dl"), .removeFailed, hint: L10n.tr("acq.dl.remove.failed.hint"))
            toggle(L10n.tr("acq.dl.auto.add"), .autoAdd, hint: L10n.tr("acq.dl.auto.add.hint"))
            toggle(L10n.tr("acq.dl.sab.outside"), .sabOutside, hint: L10n.tr("acq.dl.sab.outside.hint"))
        }
        Section(L10n.tr("acq.dl.when.remove")) {
            toggle(L10n.tr("acq.dl.delete.files"), .deleteFiles)
            toggle(L10n.tr("acq.dl.turn.down"), .turnDown, hint: L10n.tr("acq.dl.turn.down.hint"))
            toggle(L10n.tr("acq.dl.search.again"), .searchAgain, hint: L10n.tr("acq.dl.search.again.hint"))
        }
        Section {
            DisclosureGroup(L10n.tr("acq.dl.advanced"), isExpanded: $advanced) {
                number(L10n.tr("acq.dl.stalled.after"), .stalledAfter, hint: L10n.tr("acq.dl.stalled.after.hint"))
                number(L10n.tr("acq.dl.try.next.release"), .stalledAct, hint: L10n.tr("acq.dl.try.next.release.hint"))
                Toggle(isOn: Binding(get: { info.deadOn }, set: { rules.setDeadRule($0) })) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L10n.tr("acq.dl.dead.rule"))
                        Text(L10n.tr("acq.dl.dead.rule.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                    }
                }
                .disabled(!info.canEdit)
                if info.deadOn {
                    number(L10n.tr("acq.dl.dead.minutes"), .deadMinutes, hint: L10n.tr("acq.dl.dead.minutes.hint"))
                    number(L10n.tr("acq.dl.crawl.kbps"), .crawlKbps)
                    number(L10n.tr("acq.dl.crawl.hours"), .crawlHours, hint: L10n.tr("acq.dl.crawl.hint"))
                }
                toggle(L10n.tr("acq.dl.fail.programs"), .failDangerous, hint: L10n.tr("acq.dl.fail.programs.hint"))
                toggle(L10n.tr("acq.dl.replace"), .replaceVanished, hint: L10n.tr("acq.dl.replace.hint"))
                toggle(L10n.tr("acq.dl.try.next.dl"), .tryNext, hint: L10n.tr("acq.dl.try.next.dl.hint"))
                number(L10n.tr("acq.dl.spacing"), .spacing)
            }
        }
    }
}

struct DownloaderNumberField: View {
    let title: String
    let text: String
    var enabled = true
    let onChange: @MainActor (String) -> Void
    @State private var value = ""

    var body: some View {
        LabeledContent(title) {
            TextField(title, text: $value)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .frame(maxWidth: 100)
                .disabled(!enabled)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }
}

struct DownloaderRulesBar: View {
    let rules: DownloaderRulesStore

    private var info: DownloaderRulesInfo { rules.info }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if let error = info.saveError {
                Text(DownloadersWords.error(error)).font(.footnote).foregroundStyle(Theme.failText)
            }
            if let issue = info.issue {
                Text(DownloadersWords.rulesIssue(issue)).font(.footnote).foregroundStyle(Theme.failText)
            }
            HStack {
                Text(L10n.tr("acq.dl.unsaved")).font(.footnote).foregroundStyle(Theme.textMuted)
                Spacer()
                Button(L10n.tr("acq.dl.discard")) { rules.discard() }
                if info.saving {
                    ProgressView()
                } else {
                    Button(L10n.tr("acq.dl.save")) { rules.save() }
                        .buttonStyle(.borderedProminent)
                        .disabled(!info.canSave)
                }
            }
        }
        .padding(Theme.Spacing.medium)
        .background(Theme.surfaceHigh, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.bottom, Theme.Spacing.small)
    }
}
