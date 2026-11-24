import PhotosUI
import SwiftUI

struct MyProfileView: View {
    let account: AccountInfo
    @State private var store = MyProfileStore()
    @State private var draft: String?
    @State private var item: PhotosPickerItem?
    @State private var pictureFailed = false

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyStateView()
        } content: {
            List {
                banners(info)
                pictureSection(info)
                nameSection(info)
                Section {
                    LabeledContent(L10n.tr("myaccount.username"), value: info.username)
                    LabeledContent(L10n.tr("account.role"), value: PeopleText.role(account.role))
                } footer: {
                    Text(L10n.tr("myaccount.username.hint"))
                }
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr("myaccount.profile"))
        .noticeToast(store.notices)
        .task { await store.watch() }
        .task { await store.load() }
        .task(id: item) { await pickPicture() }
        .onChange(of: info.displayName) { _, _ in draft = nil }
    }

    @ViewBuilder
    private func banners(_ info: MyProfileInfo) -> some View {
        if let error = info.actionError {
            NoticeBanner(text: MyErrorText.text(error))
                .listRowSeparator(.hidden)
        }
        if let problem = info.problem {
            NoticeBanner(text: MyProfileStore.problemText(problem))
                .listRowSeparator(.hidden)
        }
        if pictureFailed {
            NoticeBanner(text: L10n.tr("picture.error"))
                .listRowSeparator(.hidden)
        }
    }

    private func pictureSection(_ info: MyProfileInfo) -> some View {
        Section {
            HStack(spacing: Theme.Spacing.large) {
                PersonAvatar(source: account.avatarPath, name: info.displayName.isEmpty ? account.displayName : info.displayName, side: 72)
                VStack(alignment: .leading, spacing: Theme.Spacing.small) {
                    PhotosPicker(selection: $item, matching: .images, photoLibrary: .shared()) {
                        Text(L10n.tr(account.avatarPath == nil ? "picture.add" : "picture.change"))
                    }
                    .disabled(info.busy)
                    if account.avatarPath != nil {
                        Button(L10n.tr("picture.remove"), role: .destructive) {
                            store.removePicture()
                        }
                        .buttonStyle(.borderless)
                        .disabled(info.busy)
                    }
                }
                Spacer(minLength: 0)
            }
        }
    }

    private func nameSection(_ info: MyProfileInfo) -> some View {
        let binding = Binding(get: { draft ?? info.displayName }, set: { draft = $0 })
        let trimmed = (draft ?? info.displayName).trimmingCharacters(in: .whitespacesAndNewlines)
        let valid = SDKClient.shared.validDisplayName(trimmed)
        let changed = trimmed != info.displayName
        return Section {
            TextField(info.username, text: binding)
                .textContentType(.nickname)
                .submitLabel(.done)
                .onSubmit { saveName(trimmed, valid: valid, changed: changed) }
            Button(L10n.tr("myaccount.save")) {
                saveName(trimmed, valid: valid, changed: changed)
            }
            .disabled(!changed || !valid || info.busy)
        } header: {
            Text(L10n.tr("myaccount.display.name"))
        } footer: {
            Text(L10n.tr(valid ? "myaccount.display.name.hint" : "myaccount.problem.name.invalid"))
                .foregroundStyle(valid ? Theme.textMuted : Theme.failText)
        }
    }

    private func saveName(_ name: String, valid: Bool, changed: Bool) {
        guard valid, changed else { return }
        store.saveName(name)
    }

    private func pickPicture() async {
        guard let item else { return }
        let data = try? await item.loadTransferable(type: Data.self)
        var jpeg: Data?
        if let data {
            jpeg = await Task.detached(priority: .userInitiated) { PictureEncoder.squareJPEG(from: data) }.value
        }
        pictureFailed = jpeg == nil
        if let jpeg { store.uploadPicture(jpeg) }
        self.item = nil
    }
}

struct MyPreferencesView: View {
    @State private var store = MyProfileStore()
    @State private var query = ""

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyStateView()
        } content: {
            List {
                if let error = info.actionError {
                    NoticeBanner(text: MyErrorText.text(error))
                        .listRowSeparator(.hidden)
                }
                Section {
                    if matches(code: "") {
                        languageRow(code: "", title: L10n.tr("myaccount.language.automatic"), detail: deviceLanguage, info: info)
                    }
                    ForEach(codes, id: \.self) { code in
                        languageRow(code: code, title: MyLanguages.nativeName(code), detail: detail(code), info: info)
                    }
                } header: {
                    Text(L10n.tr("myaccount.language"))
                } footer: {
                    Text(L10n.format("myaccount.language.hint", MyLanguages.codes.count))
                }
            }
            .searchable(text: $query)
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr("myaccount.preferences"))
        .noticeToast(store.notices)
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var codes: [String] {
        MyLanguages.codes.filter { matches(code: $0) }
    }

    private var deviceLanguage: String {
        let code = Locale.current.language.languageCode?.identifier ?? ""
        return Locale.current.localizedString(forLanguageCode: code) ?? code
    }

    private func detail(_ code: String) -> String {
        let local = MyLanguages.localName(code)
        return local == MyLanguages.nativeName(code) ? "" : local
    }

    private func matches(code: String) -> Bool {
        let text = query.trimmingCharacters(in: .whitespaces)
        if text.isEmpty { return true }
        if code.isEmpty { return L10n.tr("myaccount.language.automatic").localizedStandardContains(text) }
        return MyLanguages.nativeName(code).localizedStandardContains(text)
            || MyLanguages.localName(code).localizedStandardContains(text)
            || code.localizedStandardContains(text)
    }

    private func languageRow(code: String, title: String, detail: String, info: MyProfileInfo) -> some View {
        let selected = info.language.lowercased() == code
        return Button {
            guard !selected else { return }
            store.saveLanguage(code)
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(title)
                        .foregroundStyle(Theme.text)
                    if !detail.isEmpty {
                        Text(detail)
                            .font(.footnote)
                            .foregroundStyle(Theme.textMuted)
                    }
                }
                Spacer(minLength: Theme.Spacing.small)
                if selected {
                    Image(systemName: "checkmark")
                        .foregroundStyle(Theme.accentText)
                        .accessibilityHidden(true)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(info.busy)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

struct MyAlertsView: View {
    @State private var store = MyProfileStore()
    @State private var email: String?
    @State private var discord: String?
    @State private var episodes: Bool?

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyStateView()
        } content: {
            List {
                if let error = info.actionError {
                    NoticeBanner(text: MyErrorText.text(error))
                        .listRowSeparator(.hidden)
                }
                targets(info)
                if info.followsOn {
                    following(info)
                }
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(L10n.tr("myaccount.alerts"))
        .noticeToast(store.notices)
        .task { await store.watch() }
        .task { await store.load() }
        .onChange(of: info.email) { _, _ in email = nil }
        .onChange(of: info.discordId) { _, _ in discord = nil }
        .onChange(of: info.newEpisodes) { _, _ in episodes = nil }
    }

    private func targets(_ info: MyProfileInfo) -> some View {
        let emailValue = email ?? info.email
        let discordValue = discord ?? info.discordId
        let episodesValue = episodes ?? info.newEpisodes
        let changed = emailValue.trimmingCharacters(in: .whitespaces) != info.email
            || discordValue.trimmingCharacters(in: .whitespaces) != info.discordId
            || episodesValue != info.newEpisodes
        return Section {
            TextField(L10n.tr("myaccount.alerts.email"), text: Binding(get: { emailValue }, set: { email = $0 }))
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            TextField(L10n.tr("myaccount.alerts.discord"), text: Binding(get: { discordValue }, set: { discord = $0 }))
                .keyboardType(.numberPad)
                .autocorrectionDisabled()
            Toggle(isOn: Binding(get: { episodesValue }, set: { episodes = $0 })) {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("myaccount.alerts.episodes"))
                    Text(L10n.tr("myaccount.alerts.episodes.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            Button(L10n.tr("myaccount.save")) {
                store.saveAlerts(email: emailValue, discordId: discordValue, newEpisodes: episodesValue)
            }
            .disabled(!changed || info.busy)
        } header: {
            Text(L10n.tr("myaccount.alerts"))
        } footer: {
            Text(L10n.tr("myaccount.alerts.hint"))
        }
    }

    private func following(_ info: MyProfileInfo) -> some View {
        Section {
            if info.follows.isEmpty {
                Text(L10n.tr("myaccount.following.none"))
                    .foregroundStyle(Theme.textMuted)
            }
            ForEach(info.follows) { follow in
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(title(follow))
                        .font(.body)
                    Text(kind(follow.kind))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                .accessibilityElement(children: .combine)
                .swipeActions(edge: .trailing) {
                    Button(L10n.tr("myaccount.unfollow"), role: .destructive) {
                        store.unfollow(follow.id)
                    }
                }
            }
        } header: {
            Text(L10n.tr("myaccount.following"))
        } footer: {
            Text(L10n.tr("myaccount.following.hint"))
        }
    }

    private func title(_ follow: MyFollowInfo) -> String {
        var parts: [String] = []
        if follow.year > 0 { parts.append(String(follow.year)) }
        if !follow.artist.isEmpty { parts.append(follow.artist) }
        return parts.isEmpty ? follow.name : follow.name + " (" + parts.joined(separator: ", ") + ")"
    }

    private func kind(_ kind: String) -> String {
        switch kind {
        case "movie": L10n.tr("myaccount.kind.movie")
        case "show": L10n.tr("myaccount.kind.show")
        case "artist": L10n.tr("myaccount.kind.artist")
        case "album": L10n.tr("myaccount.kind.album")
        default: kind
        }
    }
}
