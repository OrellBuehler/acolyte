import SwiftUI

struct SubtitleSettingsView: View {
    @State private var store = SubtitleSettingsStore()
    @State private var toast: String?
    @State private var removing: SubtitleCfgSite?

    private var info: SubtitleCfgInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    form
                }
            }
        }
        .navigationTitle(L10n.tr("acq.page.subtitles"))
        .toolbar {
            if info.dirty {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.sub.discard")) { store.discard() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("acq.sub.save")) { store.save() }
                            .disabled(!info.canSave)
                    }
                }
            }
        }
        .navigationBarBackButtonHidden(info.dirty)
        .safeAreaInset(edge: .bottom) {
            if let toast {
                NoteToast(text: toast, undo: nil)
            }
        }
        .onChange(of: info.note) { _, note in
            guard let note else { return }
            toast = SubtitleCfgWords.note(note)
            let id = note.id
            Task {
                try? await Task.sleep(for: .seconds(3))
                if store.info.note?.id == id { toast = nil }
            }
        }
        .sheet(isPresented: Binding(get: { info.form != nil }, set: { if !$0 { store.cancelForm() } })) {
            SubtitleSiteFormView(store: store)
        }
        .confirmationDialog(
            removing.map { L10n.format("acq.sub.confirm.remove", $0.name) } ?? "",
            isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } }),
            titleVisibility: .visible,
            presenting: removing
        ) { site in
            Button(L10n.tr("acq.sub.remove"), role: .destructive) { store.removeSite(site.id) }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private var form: some View {
        Form {
            if let error = info.load.error {
                Section {
                    NoticeBanner(text: error.localized)
                    Button(L10n.tr("acq.sub.retry")) { Task { await store.refresh() } }
                }
            }
            if let error = info.saveError {
                Section {
                    Text(SubtitleCfgWords.error(error)).foregroundStyle(Theme.failText)
                }
            }
            SubtitleLanguagesSections(info: info, store: store)
            SubtitleTranslatorSection(info: info, store: store)
            SubtitleSitesSections(info: info, store: store, remove: { removing = $0 })
            SubtitleWhenSection(info: info, store: store)
            SubtitleMoreSections(info: info, store: store)
        }
    }
}
