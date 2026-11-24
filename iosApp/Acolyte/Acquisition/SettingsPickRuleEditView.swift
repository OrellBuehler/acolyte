import SwiftUI

struct SettingsPickRuleEditView: View {
    let id: Int64
    let from: Int64
    let account: AccountInfo
    @State private var store: QualityEditStore
    @State private var leaving = false
    @State private var pendingScroll = false
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openRoute) private var openRoute

    init(id: Int64, from: Int64, account: AccountInfo) {
        self.id = id
        self.from = from
        self.account = account
        _store = State(initialValue: QualityEditStore(id: id, from: from))
    }

    private var info: QualityEditInfo { store.info }

    var body: some View {
        content
            .navigationTitle(info.isNew ? L10n.tr("acq.q.l.add") : info.savedName)
            .navigationBarBackButtonHidden(info.dirty)
            .toolbar {
                if info.dirty {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(L10n.tr("common.back"), systemImage: "chevron.backward") { leaving = true }
                    }
                }
                if canCopy {
                    ToolbarItem(placement: .primaryAction) {
                        Button(L10n.tr("acq.q.e.copy"), systemImage: "doc.on.doc") {
                            openRoute(.settingsPickRuleEdit(id: 0, from: info.id))
                        }
                    }
                }
            }
            .qualityNotice(info.notice) { store.dismissNotice($0) }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if info.showSaveBar {
                    saveBar
                }
            }
            .confirmationDialog(L10n.tr("acq.q.e.leave.title"), isPresented: $leaving, titleVisibility: .visible) {
                Button(L10n.tr("acq.q.e.leave.discard"), role: .destructive) {
                    store.discard()
                    dismiss()
                }
                Button(L10n.tr("acq.q.e.leave.stay"), role: .cancel) {}
            } message: {
                Text(L10n.tr("acq.q.e.leave.text"))
            }
            .task { await store.watch() }
            .task { await store.load() }
    }

    private var canCopy: Bool {
        !info.isNew && info.mayEdit && !info.gone && info.load.phase == .ready
    }

    @ViewBuilder
    private var content: some View {
        if !info.allowed {
            NotAllowedView()
        } else {
            StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                EmptyView()
            } content: {
                page
            }
        }
    }

    @ViewBuilder
    private var page: some View {
        if info.gone {
            message(L10n.tr("acq.q.e.not.found"), L10n.tr("acq.q.e.not.found.text"), "questionmark.folder")
        } else if info.needsPermission {
            message(L10n.tr("acq.q.e.needs.permission"), L10n.tr("acq.q.e.needs.permission.text"), "lock")
        } else {
            form
        }
    }

    private func message(_ title: String, _ text: String, _ symbol: String) -> some View {
        ContentUnavailableView {
            Label(title, systemImage: symbol)
        } description: {
            Text(text)
        } actions: {
            Button(L10n.tr("acq.q.e.all")) { dismiss() }
                .buttonStyle(.borderedProminent)
        }
    }

    private var form: some View {
        ScrollViewReader { proxy in
            Form {
                introSection
                QualityNameSection(info: info, store: store)
                QualityStepsSection(info: info, store: store)
                QualityTraitsSection(info: info, store: store)
                if info.showTrash {
                    QualityTrashSection(info: info, store: store)
                }
                QualityMustSection(info: info, store: store)
                QualitySizeSection(info: info, store: store)
                QualityWaitingSection(info: info, store: store)
                if !info.form.music {
                    QualitySourcesSection(info: info, store: store)
                }
                QualityLanguageSection(info: info, store: store)
            }
            .scrollContentBackground(.hidden)
            .background(Theme.background)
            .scrollDismissesKeyboard(.interactively)
            .onChange(of: info.problems) { _, problems in
                guard pendingScroll, let first = problems.first else { return }
                pendingScroll = false
                withAnimation {
                    proxy.scrollTo(QualityAnchor.id(first.field), anchor: .top)
                }
            }
        }
    }

    private var introSection: some View {
        Section {
            Text(info.isNew ? L10n.tr("acq.q.e.subtitle") : QualityWords.usedLine(info.use))
                .font(.subheadline)
                .foregroundStyle(Theme.textMuted)
            if !info.mayEdit {
                Label(L10n.tr("acq.q.l.read.only"), systemImage: "lock")
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .listRowBackground(Color.clear)
    }

    private var saveBar: some View {
        VStack(spacing: Theme.Spacing.small) {
            if let use = info.formUse {
                Text(L10n.tr("acq.q.e.in.use.error") + " " + QualityWords.useWords(use) + ".")
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else if let error = info.formError {
                Text(QualityWords.errorText(error))
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            HStack(spacing: Theme.Spacing.medium) {
                Button {
                    pendingScroll = false
                    store.discard()
                } label: {
                    Text(L10n.tr("acq.q.e.discard"))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .disabled(!info.dirty || info.saving)
                Button {
                    pendingScroll = true
                    store.save()
                } label: {
                    if info.saving {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    } else {
                        Text(L10n.tr("acq.q.e.save"))
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(info.saving)
            }
            .controlSize(.large)
        }
        .padding(Theme.Spacing.medium)
        .background(.bar)
    }
}
