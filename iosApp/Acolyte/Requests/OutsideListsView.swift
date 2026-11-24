import SwiftUI
import UIKit

struct OutsideListsView: View {
    @State private var store = OutsideListsStore()
    @State private var removing: OutsideListInfo?
    @State private var traktKey = ""
    @State private var traktToken = ""
    @State private var traktSecret = ""
    @State private var mdblistKey = ""
    @Environment(\.openURL) private var openURL

    private var info: OutsideListsInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    list
                }
            }
        }
        .navigationTitle(L10n.tr("requests.outside.title"))
        .toolbar {
            if info.allowed, !info.lists.isEmpty {
                ToolbarItem(placement: .primaryAction) {
                    Button(L10n.tr("requests.outside.add"), systemImage: "plus") { store.startNew() }
                }
            }
        }
        .sheet(isPresented: Binding(get: { info.formOpen }, set: { if !$0 { store.cancelEdit() } })) {
            OutsideListForm(store: store)
        }
        .alert(L10n.format("requests.outside.remove.title", removing?.name ?? ""), isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } })) {
            Button(L10n.tr("requests.remove"), role: .destructive) {
                if let id = removing?.id { store.remove(id) }
            }
            Button(L10n.tr("common.cancel"), role: .cancel) {}
        } message: {
            Text(L10n.tr(removing?.isShow == true ? "requests.outside.remove.shows" : "requests.outside.remove.movies"))
        }
        .safeAreaInset(edge: .bottom) {
            if let note = info.note {
                NoteToast(text: noteText(note), undo: nil)
            }
        }
        .animation(.smooth(duration: 0.24), value: info.note?.id)
        .task(id: info.note?.id) {
            guard let note = info.note else { return }
            try? await Task.sleep(for: .seconds(4))
            if store.info.note?.id == note.id { store.dismissNote() }
        }
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
        .onDisappear { store.cancelSignIn() }
    }

    private func noteText(_ note: OutsideNoteInfo) -> String {
        switch note.kind {
        case .saved: L10n.tr("requests.outside.saved")
        case .savedOff: L10n.tr("requests.outside.saved.off")
        case .reading: L10n.format("requests.outside.reading", note.name)
        case .readFailed: L10n.tr("requests.outside.read.failed")
        case .removed: L10n.tr("requests.outside.removed")
        case .removeFailed: L10n.tr("requests.outside.remove.failed")
        case .traktSaved: L10n.tr("requests.trakt.saved")
        case .traktKeySaved: L10n.tr("requests.trakt.key.saved")
        case .traktSignedIn: L10n.tr("requests.trakt.signed.in")
        case .traktDenied: L10n.tr("requests.trakt.denied")
        case .traktExpired: L10n.tr("requests.trakt.expired")
        case .traktSignedOut: L10n.tr("requests.trakt.signed.out")
        case .mdblistSaved: L10n.tr("requests.mdblist.saved")
        case .mdblistRemoved: L10n.tr("requests.mdblist.removed")
        }
    }

    private func keyError(_ error: RequestErrorInfo) -> String {
        if error.error.kind == .forbidden, error.error.message.contains("password") { return L10n.tr("requests.key.password") }
        return RequestsWords.error(error)
    }

    private var list: some View {
        List {
            if let error = info.load.error {
                NoticeBanner(text: error.localized)
            }
            Section {
                if info.lists.isEmpty {
                    VStack(spacing: Theme.Spacing.small) {
                        EmptyStateView(
                            title: L10n.tr("requests.outside.empty"),
                            message: L10n.tr("requests.outside.empty.text"),
                            systemImage: "list.bullet.rectangle"
                        )
                        Button(L10n.tr("requests.outside.add")) { store.startNew() }
                            .buttonStyle(.borderedProminent)
                    }
                }
                ForEach(info.lists) { list in
                    row(list)
                        .swipeActions {
                            Button(L10n.tr("requests.remove"), role: .destructive) { removing = list }
                            Button(L10n.tr("requests.outside.change")) { store.edit(list.id) }
                        }
                }
            }
            traktSection
            mdblistSection
        }
    }

    private func row(_ list: OutsideListInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(list.name).font(.headline)
            Text(chips(list)).font(.footnote).foregroundStyle(Theme.textMuted)
            if let url = URL(string: list.link) {
                Link("\(list.ref) · \(RequestsWords.every(list.syncEveryHours))", destination: url)
                    .font(.footnote)
            }
            Text(list.lastSyncedAt.map { L10n.format("requests.outside.last.read", Format.relative($0), list.lastResult) } ?? L10n.tr("requests.outside.not.read"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            if !list.lastError.isEmpty {
                Text(list.lastError).font(.footnote).foregroundStyle(Theme.failText)
            }
            HStack {
                Button(L10n.tr("requests.outside.read.now")) { store.sync(list.id) }
                    .disabled(!list.enabled || list.busy)
                Button(L10n.tr("requests.outside.change")) { store.edit(list.id) }
                    .disabled(list.busy)
            }
            .buttonStyle(.bordered)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private func chips(_ list: OutsideListInfo) -> String {
        var parts = [RequestsWords.listKind(list.kind), L10n.tr(list.isShow ? "requests.kind.shows" : "requests.kind.movies")]
        if !list.enabled { parts.append(L10n.tr("requests.outside.off")) }
        if !list.lastError.isEmpty { parts.append(L10n.tr("requests.outside.problem")) }
        return parts.joined(separator: " · ")
    }

    private var traktSection: some View {
        Section {
            Text(L10n.tr("requests.trakt.text")).font(.footnote).foregroundStyle(Theme.textMuted)
            if info.hasTraktList, !info.traktKeySet {
                Text(L10n.tr("requests.trakt.lists.blocked")).font(.footnote).foregroundStyle(Theme.failText)
            }
            SecureField(L10n.tr(info.traktKeySet ? "requests.trakt.replace.key" : "requests.trakt.paste.key"), text: $traktKey)
            if info.owner {
                SecureField(L10n.tr(info.traktTokenSet ? "requests.trakt.replace.token" : "requests.trakt.token"), text: $traktToken)
                SecureField(L10n.tr("requests.trakt.secret"), text: $traktSecret)
            }
            if let error = info.keyError {
                Text(keyError(error)).foregroundStyle(Theme.failText)
            }
            Button(L10n.tr("requests.save")) {
                Task {
                    if await store.saveTrakt(key: traktKey, token: traktToken, secret: traktSecret) {
                        traktKey = ""
                        traktToken = ""
                        traktSecret = ""
                    }
                }
            }
            .disabled(info.keyBusy || (traktKey.isEmpty && traktToken.isEmpty && traktSecret.isEmpty))
            if info.traktKeySet, info.traktKnown {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("requests.trakt.signin")).font(.headline)
                    Text(L10n.tr("requests.trakt.signin.text")).font(.footnote).foregroundStyle(Theme.textMuted)
                    Text(info.traktSignedIn ? L10n.format("requests.trakt.signed.in.as", info.traktUser) : L10n.tr("requests.trakt.not.signed.in"))
                    if !info.traktProblem.isEmpty {
                        Text(info.traktProblem).font(.footnote).foregroundStyle(Theme.failText)
                    }
                }
                if info.owner {
                    if let device = info.device {
                        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                            let shown = device.url.replacingOccurrences(of: "https://", with: "")
                            Text(L10n.format("requests.trakt.device", shown, device.userCode)).font(.footnote)
                            Text(device.userCode).font(.largeTitle.weight(.bold)).textSelection(.enabled)
                            HStack {
                                Button(L10n.tr("requests.copy")) { UIPasteboard.general.string = device.userCode }
                                    .buttonStyle(.bordered)
                                Button(L10n.tr("requests.trakt.open")) {
                                    if let url = URL(string: device.url) { openURL(url) }
                                }
                                .buttonStyle(.borderedProminent)
                                Button(L10n.tr("common.cancel")) { store.cancelSignIn() }
                                    .buttonStyle(.borderless)
                            }
                        }
                    } else if info.traktSignedIn {
                        Button(L10n.tr("requests.trakt.sign.out"), role: .destructive) { store.signOut() }
                            .disabled(info.keyBusy)
                    } else {
                        Button(L10n.tr("requests.trakt.sign.in")) { store.signIn() }
                            .disabled(!info.canSignIn)
                        if !info.traktSecret {
                            Text(L10n.tr("requests.trakt.needs.secret")).font(.footnote).foregroundStyle(Theme.textMuted)
                        }
                    }
                }
            }
        } header: {
            Text(L10n.tr("requests.trakt.title"))
        } footer: {
            Text(L10n.tr(info.traktKeySet ? "requests.key.saved" : "requests.key.not.set"))
        }
    }

    private var mdblistSection: some View {
        Section {
            Text(L10n.tr("requests.mdblist.text")).font(.footnote).foregroundStyle(Theme.textMuted)
            SecureField(L10n.tr(info.mdblistSet ? "requests.mdblist.replace" : "requests.mdblist.paste"), text: $mdblistKey)
            Button(L10n.tr("requests.save")) {
                Task {
                    if await store.saveMdblist(mdblistKey) { mdblistKey = "" }
                }
            }
            .disabled(info.keyBusy || mdblistKey.isEmpty)
            if info.mdblistSet {
                Button(L10n.tr("requests.mdblist.remove"), role: .destructive) {
                    Task { _ = await store.saveMdblist("") }
                }
                .disabled(info.keyBusy)
            }
        } header: {
            Text(L10n.tr("requests.mdblist.title"))
        } footer: {
            Text(L10n.tr(info.mdblistSet ? "requests.key.saved" : "requests.key.not.set"))
        }
    }
}

struct OutsideListForm: View {
    let store: OutsideListsStore
    @State private var name = ""
    @State private var ref = ""
    @State private var every = ""
    @Environment(\.dismiss) private var dismiss

    private var info: OutsideListsInfo { store.info }

    var body: some View {
        NavigationStack {
            Form {
                if let draft = info.draft {
                    fields(draft)
                }
            }
            .navigationTitle(info.isNew ? L10n.tr("requests.outside.add") : L10n.format("requests.outside.change.title", info.draft?.name ?? ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { store.cancelEdit() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("requests.save")) { store.saveList() }
                            .disabled(!info.canSave)
                    }
                }
            }
        }
        .onAppear {
            name = info.draft?.name ?? ""
            ref = info.draft?.ref ?? ""
            every = info.draft.map { String($0.syncEveryHours) } ?? "12"
        }
        .onChange(of: name) { _, value in store.setName(value) }
        .onChange(of: ref) { _, value in store.setRef(value) }
        .onChange(of: every) { _, value in store.setEvery(value) }
    }

    @ViewBuilder
    private func fields(_ draft: ListDraftInfo) -> some View {
        Section {
            TextField(L10n.tr("requests.outside.name"), text: $name, prompt: Text(L10n.tr("requests.outside.name.placeholder")))
            Picker(L10n.tr("requests.outside.kind"), selection: Binding(get: { draft.kind }, set: { store.setKind($0) })) {
                ForEach(ListKindInfo.allCases) { kind in Text(RequestsWords.listKind(kind)).tag(kind) }
            }
            Picker(L10n.tr("requests.outside.adds"), selection: Binding(get: { draft.isShow }, set: { store.setTitleKind(show: $0) })) {
                Text(L10n.tr("requests.kind.movies")).tag(false)
                Text(L10n.tr("requests.kind.shows")).tag(true)
            }
            .pickerStyle(.segmented)
        }
        Section {
            TextField(L10n.tr("requests.outside.ref"), text: $ref)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        } footer: {
            Text(refHint(draft.kind))
        }
        Section {
            if info.draftFolders.isEmpty {
                Text(L10n.tr(draft.isShow ? "requests.outside.no.show.folder" : "requests.outside.no.movie.folder")).foregroundStyle(Theme.failText)
            } else {
                Picker(L10n.tr("requests.outside.folder"), selection: Binding(get: { draft.folderId }, set: { store.setFolder($0) })) {
                    ForEach(info.draftFolders) { folder in Text(folder.name).tag(folder.id) }
                }
            }
            Picker(L10n.tr("requests.quality"), selection: Binding(get: { draft.rulesId }, set: { store.setPickRules($0) })) {
                Text(L10n.tr("requests.outside.folder.own")).tag(Int64(0))
                ForEach(info.draftRules) { rule in Text(rule.name).tag(rule.id) }
            }
        }
        if draft.isShow {
            Section {
                Picker(L10n.tr("requests.outside.seasons"), selection: Binding(get: { draft.seasons }, set: { store.setSeasons($0) })) {
                    ForEach(ListSeasonsKind.allCases) { seasons in Text(RequestsWords.listSeasons(seasons)).tag(seasons) }
                }
                Toggle(L10n.tr("requests.outside.new.seasons"), isOn: Binding(get: { draft.newSeasons }, set: { store.setNewSeasons($0) }))
                    .disabled(draft.seasons == .future)
            } footer: {
                VStack(alignment: .leading) {
                    if draft.seasons == .first { Text(L10n.tr("requests.outside.seasons.first.hint")) }
                    Text(L10n.tr(draft.seasons == .future ? "requests.outside.new.seasons.future" : "requests.outside.new.seasons.hint"))
                }
            }
        }
        Section {
            LabeledContent(L10n.tr("requests.outside.every")) {
                TextField(L10n.tr("requests.outside.every"), text: $every)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: 80)
            }
            Toggle(isOn: Binding(get: { draft.keepUpToDate }, set: { store.setKeepUpToDate($0) })) {
                VStack(alignment: .leading) {
                    Text(L10n.tr("requests.outside.keep"))
                    Text(L10n.tr(draft.isShow ? "requests.outside.keep.show" : "requests.outside.keep.movie")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            Toggle(isOn: Binding(get: { draft.searchOnAdd }, set: { store.setSearchOnAdd($0) })) {
                VStack(alignment: .leading) {
                    Text(L10n.tr("requests.outside.search"))
                    Text(L10n.tr(draft.isShow ? "requests.outside.search.show" : "requests.outside.search.movie")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
            }
            Toggle(L10n.tr("requests.outside.enabled"), isOn: Binding(get: { draft.enabled }, set: { store.setEnabled($0) }))
        } footer: {
            VStack(alignment: .leading) {
                Text(L10n.tr("requests.outside.every.hint"))
                if info.needsTraktKey {
                    Text(L10n.tr("requests.outside.needs.trakt")).foregroundStyle(Theme.failText)
                }
                if let error = info.formError {
                    Text(RequestsWords.error(error)).foregroundStyle(Theme.failText)
                }
            }
        }
    }

    private func refHint(_ kind: ListKindInfo) -> String {
        switch kind {
        case .tmdb: L10n.tr("requests.outside.ref.tmdb")
        case .trakt: L10n.tr("requests.outside.ref.trakt")
        case .mdblist: L10n.tr("requests.outside.ref.mdblist")
        }
    }
}

struct NeverAddView: View {
    @State private var store = NeverAddStore()
    @State private var isShow = false
    @State private var tmdb = ""
    @State private var name = ""
    @State private var reason = ""

    private var info: NeverAddInfo { store.info }

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
        .navigationTitle(L10n.tr("requests.never.title"))
        .safeAreaInset(edge: .bottom) {
            if !info.removed.isEmpty {
                NoteToast(text: L10n.format("requests.never.allowed.again", info.removed), undo: nil)
            }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var form: some View {
        Form {
            Section {
                Picker(L10n.tr("requests.outside.kind"), selection: $isShow) {
                    Text(L10n.tr("requests.kind.movie")).tag(false)
                    Text(L10n.tr("requests.kind.show")).tag(true)
                }
                .pickerStyle(.segmented)
                TextField(L10n.tr("requests.never.tmdb"), text: $tmdb)
                    .keyboardType(.numberPad)
                TextField(L10n.tr("requests.outside.name"), text: $name)
                TextField(L10n.tr("requests.never.reason"), text: $reason)
                Button(L10n.tr("requests.never.add.button")) {
                    Task {
                        if await store.add(show: isShow, tmdb: tmdb, name: name, reason: reason) {
                            tmdb = ""
                            name = ""
                            reason = ""
                        }
                    }
                }
                .disabled(info.busy)
            } header: {
                Text(L10n.tr("requests.never.add"))
            } footer: {
                VStack(alignment: .leading) {
                    Text(L10n.tr("requests.never.sub"))
                    if info.missing {
                        Text(L10n.tr("requests.never.missing")).foregroundStyle(Theme.failText)
                    }
                    if let error = info.error {
                        Text(RequestsWords.error(error)).foregroundStyle(Theme.failText)
                    }
                }
            }
            Section {
                if info.entries.isEmpty {
                    Text(L10n.tr("requests.never.empty")).foregroundStyle(Theme.textMuted)
                }
                ForEach(info.entries) { entry in
                    VStack(alignment: .leading) {
                        Text(entry.name)
                        let kind = entry.isShow ? "\(L10n.tr("requests.kind.show")) · " : ""
                        Text(["\(kind)tmdb:\(entry.tmdb)", entry.reason].filter { !$0.isEmpty }.joined(separator: " · "))
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                    .swipeActions {
                        Button(L10n.tr("requests.remove"), role: .destructive) { store.remove(entry.id) }
                    }
                }
            }
        }
    }
}
