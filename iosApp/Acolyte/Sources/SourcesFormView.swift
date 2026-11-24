import SwiftUI

struct SourceFormSheet: View {
    let target: SourceEditTarget
    let info: SourcesInfo
    let store: SourcesStore
    @State private var form: SourceFormInfo
    @State private var tagText: String
    @State private var submitted = false
    @State private var limitsOpen = false
    @State private var proxyOpen: Bool

    init(target: SourceEditTarget, info: SourcesInfo, store: SourcesStore) {
        self.target = target
        self.info = info
        self.store = store
        _form = State(initialValue: target.form)
        _tagText = State(initialValue: target.form.tags.joined(separator: ", "))
        _proxyOpen = State(initialValue: !target.form.proxy.isEmpty)
    }

    private var title: String {
        form.isNew ? L10n.tr("acq.src.add") : L10n.format("acq.src.change.title", form.name.isEmpty ? target.form.name : form.name)
    }

    private var sendChoices: [SourceDownloaderInfo]? {
        info.downloaders?.filter { $0.protocolName == form.sendProtocol }
    }

    var body: some View {
        NavigationStack {
            Form {
                Group {
                    identity
                    address
                    usage
                    media
                    numbers
                }
                Group {
                    routing
                    languages
                    limits
                    proxy
                    network
                }
                if let error = info.saveError {
                    Section { Text(error.sourceText).foregroundStyle(Theme.failText) }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("acq.src.cancel")) { store.cancelEdit() }
                        .disabled(info.saving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if info.saving {
                        ProgressView()
                    } else {
                        Button(L10n.tr("acq.src.save")) {
                            submitted = true
                            if form.canSave { store.save(form) }
                        }
                        .disabled(form.name.trimmingCharacters(in: .whitespaces).isEmpty || form.url.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                }
            }
            .interactiveDismissDisabled(info.saving)
        }
    }

    private func edit(_ change: (inout SourceFormInfo) -> Void) {
        var next = form
        change(&next)
        form = store.settled(next)
    }

    private func text(_ keyPath: WritableKeyPath<SourceFormInfo, String>) -> Binding<String> {
        Binding(get: { form[keyPath: keyPath] }, set: { value in edit { $0[keyPath: keyPath] = value } })
    }

    private func flag(_ keyPath: WritableKeyPath<SourceFormInfo, Bool>) -> Binding<Bool> {
        Binding(get: { form[keyPath: keyPath] }, set: { value in edit { $0[keyPath: keyPath] = value } })
    }

    private func error(_ kind: SourceProblemKind, shown: Bool = true) -> String? {
        form.problem(kind, shown: submitted || shown) ? SourcesWords.problem(kind) : nil
    }

    @ViewBuilder
    private func field(_ label: String, _ binding: Binding<String>, hint: String? = nil, problem: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            TextField(label, text: binding)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if let problem {
                Text(problem).font(.footnote).foregroundStyle(Theme.failText)
            } else if let hint {
                Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }

    @ViewBuilder
    private func secret(_ label: String, _ binding: Binding<String>, hint: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            SecureField(label, text: binding)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if let hint {
                Text(hint).font(.footnote).foregroundStyle(Theme.textMuted)
            }
        }
    }

    private var identity: some View {
        Section {
            field(L10n.tr("acq.src.name"), text(\.name), problem: error(.name, shown: !form.name.isEmpty))
            Picker(L10n.tr("acq.src.format"), selection: Binding(get: { form.format }, set: { form = store.withFormat(form, $0) })) {
                Text(L10n.tr("acq.src.fmt.torznab")).tag("torznab")
                Text(L10n.tr("acq.src.fmt.newznab")).tag("newznab")
                Text(L10n.tr("acq.src.fmt.rss")).tag("rss")
                if form.showSiteFormat { Text(L10n.tr("acq.src.fmt.site")).tag("site") }
            }
            .disabled(form.formatLocked)
            if form.isSite {
                Picker(L10n.tr("acq.src.site"), selection: Binding(get: { form.site }, set: { form = store.withSite(form, $0) })) {
                    ForEach(store.sites) { site in Text(site.name).tag(site.id) }
                }
                .disabled(form.siteLocked)
            } else {
                Picker(L10n.tr("acq.src.downloads"), selection: Binding(get: { form.protocolName }, set: { value in edit { $0.protocolName = value } })) {
                    Text(L10n.tr("acq.src.downloads.torrent")).tag("torrent")
                    Text(L10n.tr("acq.src.downloads.usenet")).tag("usenet")
                }
            }
        }
    }

    private var addressHint: String {
        switch form.addressHint {
        case .rss: L10n.tr("acq.src.address.rss")
        case .site: L10n.tr("acq.src.address.site")
        case .feed: L10n.tr("acq.src.address.feed")
        }
    }

    private var address: some View {
        Section {
            let urlProblem = error(.url, shown: !form.url.isEmpty) ?? error(.urlQuery, shown: !form.url.isEmpty)
            field(L10n.tr("acq.src.address"), text(\.url), hint: addressHint, problem: urlProblem)
                .keyboardType(.URL)
            if form.showLoginChoice {
                Picker(L10n.tr("acq.src.login"), selection: Binding(get: { form.siteLogin }, set: { value in edit { $0.siteLogin = value } })) {
                    Text(L10n.tr("acq.src.login.passkey")).tag("passkey")
                    Text(L10n.tr("acq.src.login.cookie")).tag("cookie")
                }
            }
            switch form.keyKind {
            case .cookie:
                secret(L10n.tr("acq.src.cookie"), text(\.apiKey), hint: L10n.tr("acq.src.cookie.hint"))
            case .passkey:
                secret(L10n.tr("acq.src.passkey"), text(\.apiKey), hint: L10n.tr("acq.src.passkey.hint"))
            case .apiKey:
                secret(L10n.tr("acq.src.api.key"), text(\.apiKey), hint: L10n.tr("acq.src.api.key.hint"))
            case .none:
                EmptyView()
            }
            if form.showAgent {
                field(L10n.tr("acq.src.agent"), text(\.siteAgent), hint: L10n.tr("acq.src.agent.hint"), problem: error(.siteAgent))
            }
            if form.showUser {
                field(L10n.tr("acq.src.user"), text(\.siteUser), hint: L10n.tr("acq.src.user.hint"), problem: error(.siteUser))
                    .keyboardType(.numberPad)
            }
        }
    }

    private var usage: some View {
        Section(L10n.tr("acq.src.use.for")) {
            Toggle(L10n.tr("acq.src.newest"), isOn: flag(\.feed))
            if form.feed && !form.newestListWorks {
                Text(L10n.tr("acq.src.no.newest")).font(.footnote).foregroundStyle(Theme.textMuted)
            }
            if form.showSearchUse {
                Toggle(L10n.tr("acq.src.auto"), isOn: flag(\.autoSearch))
                Toggle(L10n.tr("acq.src.hand"), isOn: flag(\.handSearch))
            }
        }
    }

    private var media: some View {
        Section(L10n.tr("acq.src.media")) {
            kindToggle("movie", L10n.tr("acq.src.movies"))
            kindToggle("tv", L10n.tr("acq.src.tv"))
            kindToggle("music", L10n.tr("acq.src.music"))
        }
    }

    private func kindToggle(_ kind: String, _ label: String) -> some View {
        Toggle(label, isOn: Binding(get: { form.kinds.contains(kind) }, set: { form = store.withKind(form, kind, $0) }))
    }

    private var numbers: some View {
        Section {
            field(L10n.tr("acq.src.priority"), text(\.priority), hint: L10n.tr("acq.src.priority.hint"), problem: error(.priority))
                .keyboardType(.numberPad)
            if form.showSeeders {
                field(L10n.tr("acq.src.seeders"), text(\.minSeeders), problem: error(.seeders))
                    .keyboardType(.numberPad)
            }
            field(
                L10n.tr("acq.src.tags"),
                Binding(get: { tagText }, set: { value in
                    tagText = value
                    form = store.withTagText(form, value)
                }),
                hint: L10n.tr("acq.src.tags.hint")
            )
        }
    }

    @ViewBuilder
    private var routing: some View {
        if let choices = sendChoices {
            Section {
                Picker(L10n.tr("acq.src.send.to"), selection: Binding(get: { form.downloaderId }, set: { value in edit { $0.downloaderId = value } })) {
                    Text(L10n.tr("acq.src.send.to.any")).tag(Int64(0))
                    ForEach(choices) { choice in Text(choice.name).tag(choice.id) }
                }
                Text(L10n.tr("acq.src.send.to.hint")).font(.footnote).foregroundStyle(Theme.textMuted)
                if form.sendGone {
                    NoticeBanner(text: L10n.tr("acq.src.send.to.gone"))
                }
            }
        }
    }

    private var languages: some View {
        Section {
            field(L10n.tr("acq.src.multi"), text(\.multiLanguages), hint: L10n.tr("acq.src.multi.hint"))
            field(L10n.tr("acq.src.no.language"), text(\.noLanguageLanguages), hint: L10n.tr("acq.src.no.language.hint"), problem: error(.languages))
        }
    }

    private var limits: some View {
        Section {
            DisclosureGroup(L10n.tr("acq.src.limits"), isExpanded: $limitsOpen) {
                field(L10n.tr("acq.src.cap"), text(\.searchesPerWindow), hint: L10n.tr("acq.src.cap.hint"), problem: error(.searches))
                    .keyboardType(.numberPad)
                field(L10n.tr("acq.src.window"), text(\.windowHours), problem: error(.window))
                    .keyboardType(.decimalPad)
                field(L10n.tr("acq.src.gap"), text(\.gapMs), problem: error(.gap))
                    .keyboardType(.numberPad)
                field(L10n.tr("acq.src.timeout"), text(\.timeoutSeconds), problem: error(.timeout))
                    .keyboardType(.decimalPad)
            }
        }
    }

    private var proxy: some View {
        Section {
            DisclosureGroup(L10n.tr("acq.src.proxy"), isExpanded: $proxyOpen) {
                field(L10n.tr("acq.src.proxy.address"), text(\.proxy), hint: L10n.tr("acq.src.proxy.example"), problem: error(.proxy))
                    .keyboardType(.URL)
                secret(L10n.tr("acq.src.proxy.password"), text(\.proxyPassword))
                Toggle(L10n.tr("acq.src.proxy.local"), isOn: flag(\.proxyAllowLocalNetwork))
            }
        }
    }

    private var network: some View {
        Section {
            Toggle(L10n.tr("acq.src.local"), isOn: flag(\.allowLocalNetwork))
        } footer: {
            Text(L10n.tr("acq.src.local.hint"))
        }
    }
}
