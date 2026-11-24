import SwiftUI

struct TitleView: View {
    @State private var store: TitleStore

    init(kind: String, tmdb: String, libraryId: Int64, openLibrary: Bool = false) {
        _store = State(initialValue: TitleStore(kind: kind, tmdb: tmdb, libraryId: libraryId, openLibrary: openLibrary))
    }

    var body: some View {
        let info = store.info
        content(info)
            .navigationTitle(info.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if info.ready {
                    ToolbarItemGroup(placement: .primaryAction) { TitleToolbar(store: store) }
                }
            }
            .sheet(item: $store.sheet, onDismiss: { store.dismissed() }) { sheet in
                NavigationStack {
                    TitleSheetView(sheet: sheet, store: store)
                }
                .presentationDragIndicator(.visible)
            }
            .fullScreenCover(item: Binding(get: { store.trailer.map(TrailerItem.init) }, set: { store.trailer = $0?.source })) { item in
                TrailerPlayerView(source: item.source, store: store)
            }
            .titleConfirmations(store: store)
            .overlay { FeedbackToast(feedback: info.feedback) }
            .task { await store.watch() }
            .task { await store.load() }
            .task { await store.followLive() }
            .task(id: info.tab) { if info.tab == .library { store.run { await $0.loadChoices() } } }
    }

    @ViewBuilder
    private func content(_ info: TitleInfo) -> some View {
        switch info.problem {
        case .blocked:
            EmptyStateView(title: L10n.tr("title.blocked"), systemImage: "hand.raised")
        case .notfound:
            EmptyStateView(title: L10n.tr("title.not.found"), message: L10n.tr("title.not.found.hint"), systemImage: "magnifyingglass")
        case .notinlibrary:
            EmptyStateView(title: L10n.tr("title.not.in.library"), message: L10n.tr("title.not.in.library.hint"), systemImage: "books.vertical")
        case TitleProblemKind.none:
            StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                EmptyView()
            } content: {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0, pinnedViews: [.sectionHeaders]) {
                        TitleHeroView(info: info, store: store)
                        Section {
                            if info.tab == .library && info.isOwned {
                                TitleLibraryView(info: info, store: store)
                            } else {
                                TitleOverviewView(info: info, store: store)
                            }
                        } header: {
                            if info.isOwned { TitleTabsBar(info: info, store: store) }
                        }
                    }
                    .padding(.bottom, Theme.Spacing.xLarge * 2)
                }
                .refreshable { await store.refresh() }
            }
        }
    }
}

struct TrailerItem: Identifiable {
    let source: TrailerSourceInfo
    var id: String { source.hls + source.mp4 + source.relay + source.youTubeURL }
}

private struct TitleTabsBar: View {
    let info: TitleInfo
    let store: TitleStore

    var body: some View {
        Picker(L10n.tr("title.tab.overview"), selection: Binding(get: { info.tab }, set: { store.selectTab($0) })) {
            Text(L10n.tr("title.tab.overview")).tag(TitleTabKind.overview)
            Text(info.libraryAlert ? L10n.tr("title.tab.library.alert") : L10n.tr("title.tab.library")).tag(TitleTabKind.library)
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, Theme.Spacing.screen)
        .padding(.vertical, Theme.Spacing.small)
        .background(Theme.background)
    }
}

private struct TitleToolbar: View {
    let store: TitleStore

    var body: some View {
        let info = store.info
        if info.access.followOn && info.hasTmdb {
            Button {
                store.run { await $0.toggleFollow() }
            } label: {
                Label(
                    info.following ? L10n.tr("title.following") : L10n.tr("title.follow"),
                    systemImage: info.following ? "bell.fill" : "bell"
                )
            }
            .disabled(info.followBusy)
        }
        if !info.actionGroups.isEmpty {
            Menu {
                ForEach(Array(info.actionGroups.enumerated()), id: \.offset) { _, group in
                    Section {
                        ForEach(group, id: \.self) { action in
                            Button(role: action == .delete ? .destructive : nil) {
                                run(action)
                            } label: {
                                Text(actionText(action, show: info.isShow))
                            }
                        }
                    }
                }
            } label: {
                Label(L10n.tr("title.more.actions"), systemImage: "ellipsis.circle")
            }
        }
    }

    private func actionText(_ action: TitleActionKind, show: Bool) -> String {
        switch action {
        case .search: L10n.tr("title.search.now")
        case .chooserelease: L10n.tr("title.choose.release")
        case .rename: L10n.tr("title.rename.files")
        case .addversion: L10n.tr("title.add.version")
        case .settings: L10n.tr("title.settings")
        case .keep: L10n.tr("title.keep.up.to.date")
        case .stopkeeping: L10n.tr("title.stop.keeping")
        case .refresh: L10n.tr("title.refresh.details")
        case .history: L10n.tr("title.all.history")
        case .delete: show ? L10n.tr("title.delete.show") : L10n.tr("title.delete.movie")
        case .report: L10n.tr("title.report")
        case .hide: L10n.tr("title.hide")
        }
    }

    private func run(_ action: TitleActionKind) {
        switch action {
        case .search: store.run { await $0.searchNow() }
        case .chooserelease: store.openReleases(.whole)
        case .rename: store.openRename()
        case .addversion:
            store.selectTab(.library)
            store.run { await $0.loadChoices() }
            store.open(.version(nil, 0))
        case .settings: store.selectTab(.library)
        case .keep, .stopkeeping: store.run { await $0.toggleKeep() }
        case .refresh: store.run { await $0.refreshDetails() }
        case .history: store.open(.history)
        case .delete: store.openRemove()
        case .report: store.reportRoute = .report(titleId: store.info.ownedId)
        case .hide:
            store.run { await $0.loadUsers() }
            store.open(.hide)
        }
    }
}

private struct ConfirmModifier: ViewModifier {
    let store: TitleStore

    func body(content: Content) -> some View {
        content
            .confirmationDialog(
                title(store.confirm),
                isPresented: Binding(get: { store.confirm != nil }, set: { if !$0 { store.confirm = nil } }),
                titleVisibility: .visible,
                presenting: store.confirm
            ) { confirm in
                Button(button(confirm), role: .destructive) { run(confirm) }
                Button(L10n.tr("common.cancel"), role: .cancel) {}
            } message: { confirm in
                Text(message(confirm))
            }
    }

    private func title(_ confirm: TitleConfirm?) -> String {
        switch confirm {
        case .markFailed: L10n.tr("title.mark.failed")
        case .unprotect: L10n.tr("title.unprotect")
        case let .removeVersion(version): L10n.format("title.remove.version", version.label)
        case let .move(folder): L10n.format("title.move.to", folder.shown)
        case nil: ""
        }
    }

    private func button(_ confirm: TitleConfirm) -> String {
        switch confirm {
        case .markFailed: L10n.tr("title.mark.failed")
        case .unprotect: L10n.tr("title.unprotect")
        case .removeVersion: L10n.tr("title.stop")
        case .move: L10n.tr("title.move.files")
        }
    }

    private func message(_ confirm: TitleConfirm) -> String {
        switch confirm {
        case .markFailed: L10n.tr("title.mark.failed.confirm")
        case .unprotect: L10n.tr("title.unprotect.confirm")
        case let .removeVersion(version):
            store.info.isShow ? L10n.format("title.remove.version.show", version.label) : L10n.format("title.remove.version.movie", version.label)
        case let .move(folder): L10n.tr("title.move.body") + "\n" + folder.path
        }
    }

    private func run(_ confirm: TitleConfirm) {
        switch confirm {
        case let .markFailed(id): store.run { await $0.markFailed(id) }
        case let .unprotect(id): store.run { await $0.protect(id, on: false) }
        case let .removeVersion(version): store.run { await $0.removeVersion(version.id) }
        case let .move(folder): store.run { await $0.moveTo(folder.id) }
        }
        store.confirm = nil
    }
}

private struct RouteModifier: ViewModifier {
    @Bindable var store: TitleStore

    func body(content: Content) -> some View {
        content.navigationDestination(item: $store.reportRoute) { RouteScreen(route: $0) }
    }
}

extension View {
    func titleConfirmations(store: TitleStore) -> some View {
        modifier(ConfirmModifier(store: store)).modifier(RouteModifier(store: store))
    }
}
