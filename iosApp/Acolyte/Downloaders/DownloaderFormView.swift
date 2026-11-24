import SwiftUI

struct DownloaderField: View {
    let title: String
    let text: String
    var placeholder = ""
    var secure = false
    var mono = false
    var decimal = false
    var hint: String?
    var error: String?
    let onChange: @MainActor (String) -> Void
    @State private var value = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            field
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(decimal ? .decimalPad : .default)
            if let error {
                Text(error).font(.footnote).foregroundStyle(Theme.failText)
            } else if let hint {
                Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }

    @ViewBuilder
    private var field: some View {
        if secure {
            SecureField(placeholder, text: $value)
        } else if mono {
            TextField(placeholder, text: $value).font(.system(.body, design: .monospaced))
        } else {
            TextField(placeholder, text: $value)
        }
    }
}

struct DownloaderFormView: View {
    let store: DownloadersStore
    @State private var moreOptions = false
    @State private var tagDraft = ""
    @Environment(\.openURL) private var openURL

    private var info: DownloadersInfo { store.info }

    var body: some View {
        NavigationStack {
            Group {
                if let form = info.form {
                    formBody(form)
                } else {
                    EmptyView()
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.dl.cancel")) { store.cancelForm() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else if info.form?.showFields == true {
                        Button(L10n.tr("acq.dl.save")) { store.save() }
                            .disabled(info.form?.canSave != true)
                    }
                }
            }
        }
        .interactiveDismissDisabled(info.saving)
    }

    private var title: String {
        guard let form = info.form else { return L10n.tr("acq.dl.add") }
        return form.isNew ? L10n.tr("acq.dl.add") : L10n.format("acq.dl.form.change", form.name)
    }

    private func issue(_ form: DownloaderFormInfo, _ kinds: DownloaderIssueKind...) -> String? {
        kinds.first { form.issues.contains($0) }.map { DownloadersWords.issue($0) }
    }

    @ViewBuilder
    private func formBody(_ form: DownloaderFormInfo) -> some View {
        Form {
            if form.isNew { tiles(form) }
            if form.showFields {
                basics(form)
                signIn(form)
                notes(form)
                if form.kindWire == "qbittorrent" {
                    Section { DisclosureGroup(L10n.tr("acq.dl.more.options"), isExpanded: $moreOptions) { categoryFields(form) } }
                } else {
                    Section { categoryFields(form) }
                }
                Section {
                    Toggle(isOn: Binding(get: { form.allowLocal }, set: { store.setAllowLocalNetwork($0) })) {
                        labeled(L10n.tr("acq.dl.local"), L10n.tr("acq.dl.local.hint"))
                    }
                    Toggle(isOn: Binding(get: { form.writable }, set: { store.setWritable($0) })) {
                        labeled(
                            L10n.tr("acq.dl.writable"),
                            form.writableLocked ? L10n.tr("acq.dl.writable.locked") : L10n.tr("acq.dl.writable.off")
                        )
                    }
                    .disabled(form.writableLocked)
                }
                after(form)
                if !form.isNew { testSection }
            }
            if let error = info.formError {
                Section { Text(DownloadersWords.error(error)).foregroundStyle(Theme.failText) }
            }
        }
    }

    private func labeled(_ title: String, _ hint: String?) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
            if let hint {
                Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }

    private func tiles(_ form: DownloaderFormInfo) -> some View {
        Section(L10n.tr("acq.dl.which.app")) {
            tileGroup(L10n.tr("acq.dl.torrents"), info.torrentKinds, form)
            tileGroup(L10n.tr("acq.dl.usenet"), info.usenetKinds, form)
        }
    }

    private func tileGroup(_ title: String, _ kinds: [DownloaderKindInfo], _ form: DownloaderFormInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(title).font(.footnote).foregroundStyle(Theme.textMuted)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Theme.Spacing.small) {
                ForEach(kinds) { kind in
                    let picked = form.picked && form.kindWire == kind.wire
                    Button {
                        store.pickKind(kind.wire)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(kind.badge).font(.footnote.weight(.bold)).foregroundStyle(Theme.textMuted)
                            Text(kind.appName).font(.headline).foregroundStyle(Theme.text)
                            if kind.wire == "qbittorrent" || kind.wire == "sabnzbd" {
                                Text(L10n.tr("acq.dl.most.used")).font(.caption).foregroundStyle(Theme.textMuted)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(Theme.Spacing.medium)
                        .background(picked ? Theme.surfaceHigh : Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
                                .strokeBorder(picked ? Theme.accent : Theme.line, lineWidth: picked ? 2 : 1)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private func basics(_ form: DownloaderFormInfo) -> some View {
        Section {
            DownloaderField(title: L10n.tr("acq.dl.name"), text: form.name, error: issue(form, .name)) { store.setName($0) }
            DownloaderField(
                title: L10n.tr("acq.dl.address"),
                text: form.url,
                placeholder: form.kind?.placeholder ?? "",
                mono: true,
                hint: L10n.tr("acq.dl.address.hint"),
                error: issue(form, .url)
            ) { store.setUrl($0) }
        }
    }

    @ViewBuilder
    private func signIn(_ form: DownloaderFormInfo) -> some View {
        if let kind = form.kind {
            let placeholder = form.hasSecret ? "••••••••" : ""
            let secretIssue = issue(form, .secret)
            Section {
                if kind.signIn == .qbittorrent {
                    Picker(L10n.tr("acq.dl.sign.in.with"), selection: Binding(get: { form.login }, set: { store.setLogin($0) })) {
                        Text(L10n.tr("acq.dl.api.key")).tag(false)
                        Text(L10n.tr("acq.dl.user.pass")).tag(true)
                    }
                    .pickerStyle(.segmented)
                }
                if form.hasUsername {
                    DownloaderField(
                        title: L10n.tr("acq.dl.username"),
                        text: form.username,
                        hint: kind.signIn == .qbittorrent ? L10n.tr("acq.dl.qbit.user") : L10n.format("acq.dl.leave.empty", kind.appName),
                        error: issue(form, .username)
                    ) { store.setUsername($0) }
                    DownloaderField(
                        title: L10n.tr("acq.dl.password"),
                        text: form.secret,
                        placeholder: placeholder,
                        secure: true,
                        hint: L10n.tr("acq.dl.secret.kept"),
                        error: secretIssue
                    ) { store.setSecret($0) }
                } else {
                    DownloaderField(
                        title: secretTitle(kind),
                        text: form.secret,
                        placeholder: placeholder,
                        secure: true,
                        hint: secretHint(kind) + " " + L10n.tr("acq.dl.secret.kept"),
                        error: secretIssue
                    ) { store.setSecret($0) }
                    if kind.wire == "sabnzbd" { keyHelper(form) }
                }
            }
        }
    }

    private func secretTitle(_ kind: DownloaderKindInfo) -> String {
        switch kind.wire {
        case "sabnzbd", "qbittorrent": L10n.tr("acq.dl.api.key")
        case "aria2": L10n.tr("acq.dl.secret.token")
        default: L10n.tr("acq.dl.password")
        }
    }

    private func secretHint(_ kind: DownloaderKindInfo) -> String {
        switch kind.wire {
        case "sabnzbd": L10n.tr("acq.dl.secret.sab")
        case "qbittorrent": L10n.tr("acq.dl.secret.qbit")
        case "aria2": L10n.tr("acq.dl.secret.aria2")
        default: L10n.tr("acq.dl.secret.deluge")
        }
    }

    @ViewBuilder
    private func keyHelper(_ form: DownloaderFormInfo) -> some View {
        if form.canFetchKey {
            HStack {
                if info.canFetchKey {
                    Button {
                        store.fetchKey()
                    } label: {
                        if info.key.fetching { ProgressView() } else { Text(L10n.tr("acq.dl.fetch.key")) }
                    }
                    .buttonStyle(.bordered)
                    .disabled(info.key.fetching)
                }
                if let url = form.keyPageURL {
                    Button(L10n.tr("acq.dl.get.key")) { openURL(url) }
                        .buttonStyle(.borderless)
                }
            }
            if info.key.loginShown {
                DownloaderField(title: L10n.tr("acq.dl.sab.user"), text: info.key.user) { store.setSabUser($0) }
                DownloaderField(title: L10n.tr("acq.dl.sab.pass"), text: info.key.pass, secure: true) { store.setSabPass($0) }
            }
            if let note = info.key.note {
                Text(DownloadersWords.keyNote(note)).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }

    @ViewBuilder
    private func notes(_ form: DownloaderFormInfo) -> some View {
        switch form.kindWire {
        case "deluge": Section { Text(L10n.tr("acq.dl.deluge.note")).font(.footnote) }
        case "rtorrent": Section { Text(L10n.tr("acq.dl.rtorrent.note")).font(.footnote) }
        case "aria2": Section { Text(L10n.tr("acq.dl.aria2.note")).font(.footnote) }
        default: EmptyView()
        }
    }

    @ViewBuilder
    private func categoryFields(_ form: DownloaderFormInfo) -> some View {
        DownloaderField(
            title: L10n.tr("acq.dl.category"),
            text: form.category,
            hint: DownloadersWords.categoryHint(form.kindWire) + " " + L10n.tr("acq.dl.only.follows"),
            error: issue(form, .category, .categoryLabel, .categoryFolder)
        ) { store.setCategory($0) }
        DownloaderField(
            title: L10n.tr("acq.dl.priority"),
            text: form.priority,
            decimal: true,
            hint: L10n.tr("acq.dl.priority.hint"),
            error: issue(form, .priority)
        ) { store.setPriority($0) }
        kindCategories(form)
        tags(form)
        if form.kindWire == "qbittorrent" {
            DownloaderField(
                title: L10n.tr("acq.dl.category.after.import"),
                text: form.categoryAfterImport,
                hint: L10n.tr("acq.dl.category.after.import.hint"),
                error: issue(form, .afterImport, .afterImportSame)
            ) { store.setCategoryAfterImport($0) }
        }
        if !form.usenet {
            Toggle(isOn: Binding(get: { form.ownOnly }, set: { store.setOwnOnly($0) })) {
                labeled(L10n.tr("acq.dl.own.only"), L10n.tr("acq.dl.own.only.hint"))
            }
        }
        pathPairs(form)
    }

    private func kindFieldTitle(_ field: DownloaderKindField) -> String {
        switch field {
        case .movies: L10n.tr("acq.dl.category.movies")
        case .shows: L10n.tr("acq.dl.category.shows")
        case .music: L10n.tr("acq.dl.category.music")
        }
    }

    @ViewBuilder
    private func kindCategories(_ form: DownloaderFormInfo) -> some View {
        if form.isNew {
            Text(L10n.tr("acq.dl.categories.picked")).font(.footnote).foregroundStyle(Theme.textMuted)
        } else if info.categories.loading {
            EmptyView()
        } else if info.categories.listed == true {
            ForEach(DownloaderKindField.allCases) { field in
                Picker(
                    kindFieldTitle(field),
                    selection: Binding(get: { form.kindCategory(field) }, set: { store.setKindCategory(field, $0) })
                ) {
                    Text(L10n.format("acq.dl.same.as.main", form.category)).tag("")
                    ForEach(info.categories.options(form.kindCategory(field)), id: \.self) { name in
                        Text(name).tag(name)
                    }
                }
            }
        } else {
            ForEach(DownloaderKindField.allCases) { field in
                DownloaderField(
                    title: kindFieldTitle(field),
                    text: form.kindCategory(field),
                    hint: L10n.tr("acq.dl.category.kind.hint"),
                    error: issue(form, .kindCategory)
                ) { store.setKindCategory(field, $0) }
            }
        }
    }

    @ViewBuilder
    private func tags(_ form: DownloaderFormInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("acq.dl.tags")).font(.footnote).foregroundStyle(Theme.textMuted)
            ForEach(form.tags, id: \.self) { tag in
                HStack {
                    Text(tag)
                    Spacer()
                    Button {
                        store.removeTag(tag)
                    } label: {
                        Image(systemName: "xmark.circle.fill").foregroundStyle(Theme.textMuted)
                    }
                    .buttonStyle(.borderless)
                    .accessibilityLabel(L10n.format("acq.dl.tag.remove", tag))
                }
            }
            HStack {
                TextField(L10n.tr("acq.dl.tag.add"), text: $tagDraft)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .onSubmit { addTag() }
                Button(L10n.tr("acq.dl.tag.add.button")) { addTag() }
                    .buttonStyle(.bordered)
                    .disabled(tagDraft.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            Text(L10n.tr("acq.dl.tags.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
        }
    }

    private func addTag() {
        store.addTag(tagDraft)
        tagDraft = ""
    }

    @ViewBuilder
    private func pathPairs(_ form: DownloaderFormInfo) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(L10n.tr("acq.dl.folder.names")).font(.subheadline.weight(.semibold))
            Text(L10n.tr("acq.dl.folder.names.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
        }
        ForEach(Array(form.pathMap.enumerated()), id: \.offset) { index, pair in
            VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                DownloaderField(title: L10n.tr("acq.dl.their.path"), text: pair.theirs, placeholder: "/data/downloads", mono: true) {
                    store.setPathTheirs(index, $0)
                }
                DownloaderField(title: L10n.tr("acq.dl.our.path"), text: pair.ours, placeholder: "/downloads", mono: true) {
                    store.setPathOurs(index, $0)
                }
                Toggle(isOn: Binding(get: { pair.link }, set: { store.setPathLink(index, $0) })) {
                    labeled(L10n.tr("acq.dl.link.pair"), L10n.tr("acq.dl.link.pair.hint"))
                }
                if pair.same {
                    Text(L10n.tr("acq.dl.same.pair")).font(.footnote).foregroundStyle(Theme.textMuted)
                }
                Button(L10n.tr("acq.dl.remove.pair"), role: .destructive) { store.removePathPair(index) }
                    .buttonStyle(.borderless)
            }
        }
        if let message = issue(form, .path) {
            Text(message).font(.footnote).foregroundStyle(Theme.failText)
        }
        Button(L10n.tr("acq.dl.add.pair")) { store.addPathPair() }
    }

    @ViewBuilder
    private func after(_ form: DownloaderFormInfo) -> some View {
        let app = form.kind?.appName ?? form.kindWire
        if form.seeding != .nothing {
            Section(L10n.tr("acq.dl.after.seeding")) {
                Toggle(isOn: Binding(get: { form.seedRemove }, set: { store.setSeedRemove($0) })) {
                    labeled(L10n.tr("acq.dl.seed.remove"), seedRemoveHint(form, app))
                }
                .disabled(!form.writable)
                if form.seedRemove, form.writable {
                    if form.seeding == .goals {
                        DownloaderField(
                            title: L10n.tr("acq.dl.seed.ratio"),
                            text: form.seedRatio,
                            decimal: true,
                            hint: L10n.format("acq.dl.seed.ratio.hint", app),
                            error: issue(form, .ratio)
                        ) { store.setSeedRatio($0) }
                        DownloaderField(
                            title: L10n.tr("acq.dl.seed.hours"),
                            text: form.seedHours,
                            decimal: true,
                            hint: L10n.tr("acq.dl.seed.hours.hint"),
                            error: issue(form, .hours)
                        ) { store.setSeedHours($0) }
                    }
                    Toggle(isOn: Binding(get: { form.seedDelete }, set: { store.setSeedDeleteFiles($0) })) {
                        labeled(L10n.tr("acq.dl.seed.delete"), L10n.tr("acq.dl.seed.delete.hint"))
                    }
                }
            }
        } else {
            Section(L10n.tr("acq.dl.after.import")) {
                Toggle(isOn: Binding(get: { form.clearFinished }, set: { store.setClearFinished($0) })) {
                    labeled(
                        L10n.tr("acq.dl.clear.finished"),
                        form.writable ? L10n.format("acq.dl.clear.finished.hint", app) : L10n.tr("acq.dl.seed.needs.writable")
                    )
                }
                .disabled(!form.writable)
            }
        }
    }

    private func seedRemoveHint(_ form: DownloaderFormInfo, _ app: String) -> String {
        if !form.writable { return L10n.tr("acq.dl.seed.needs.writable") }
        return form.seeding == .goals ? L10n.format("acq.dl.seed.remove.hint.goal", app) : L10n.format("acq.dl.seed.remove.hint", app)
    }

    private var testSection: some View {
        Section {
            Button {
                store.testForm()
            } label: {
                if info.formTesting { ProgressView() } else { Text(L10n.tr("acq.dl.test")) }
            }
            .disabled(info.formTesting)
            if let result = info.formResult {
                StatusChip(text: DownloadersWords.testTag(result), tone: result.ok ? .ok : .fail)
                if !result.ok {
                    Text(DownloadersWords.testText(result)).font(.footnote).foregroundStyle(Theme.failText)
                }
            }
        }
    }
}
