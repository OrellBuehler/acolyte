import SwiftUI

enum SettingsLookWords {
    static func gate(_ value: String) -> String {
        switch value {
        case "announced": L10n.tr("settings.look.defaults.gate.announced")
        case "in_cinemas": L10n.tr("settings.look.defaults.gate.in.cinemas")
        case "released": L10n.tr("settings.look.defaults.gate.released")
        default: value
        }
    }

    static func gateHint(_ value: String) -> String? {
        switch value {
        case "announced": L10n.tr("settings.look.defaults.gate.announced.hint")
        case "in_cinemas": L10n.tr("settings.look.defaults.gate.in.cinemas.hint")
        case "released": L10n.tr("settings.look.defaults.gate.released.hint")
        default: nil
        }
    }

    static func season(_ value: String) -> String {
        switch value {
        case "all": L10n.tr("settings.look.defaults.seasons.all")
        case "future": L10n.tr("settings.look.defaults.seasons.future")
        case "latest": L10n.tr("settings.look.defaults.seasons.latest")
        case "none": L10n.tr("settings.look.defaults.seasons.none")
        default: value
        }
    }

    static func seasonHint(_ value: String) -> String? {
        switch value {
        case "all": L10n.tr("settings.look.defaults.seasons.all.hint")
        case "future": L10n.tr("settings.look.defaults.seasons.future.hint")
        case "latest": L10n.tr("settings.look.defaults.seasons.latest.hint")
        case "none": L10n.tr("settings.look.defaults.seasons.none.hint")
        default: nil
        }
    }

    static func laterSeasons(_ value: String) -> String {
        switch value {
        case "all": L10n.tr("settings.look.defaults.new.seasons.all")
        case "none": L10n.tr("settings.look.defaults.new.seasons.none")
        default: value
        }
    }

    static func order(_ value: String) -> String {
        switch value {
        case "aired": L10n.tr("settings.look.defaults.order.aired")
        case "daily": L10n.tr("settings.look.defaults.order.daily")
        case "absolute": L10n.tr("settings.look.defaults.order.absolute")
        default: value
        }
    }

    static func orderHint(_ value: String) -> String? {
        switch value {
        case "aired": L10n.tr("settings.look.defaults.order.aired.hint")
        case "daily": L10n.tr("settings.look.defaults.order.daily.hint")
        case "absolute": L10n.tr("settings.look.defaults.order.absolute.hint")
        default: nil
        }
    }

    static func source(_ value: String) -> String {
        switch value {
        case "tmdb": L10n.tr("settings.look.defaults.source.tmdb")
        case "tvmaze": L10n.tr("settings.look.defaults.source.tvmaze")
        case "tvdb": L10n.tr("settings.look.defaults.source.tvdb")
        default: value
        }
    }

    static func keyLabel(_ name: String) -> String {
        switch name {
        case "tmdb_token": L10n.tr("settings.look.metadata.key.tmdb.token")
        case "tvdb_key": L10n.tr("settings.look.metadata.key.tvdb.key")
        case "tvdb_pin": L10n.tr("settings.look.metadata.key.tvdb.pin")
        case "mdblist_key": L10n.tr("settings.look.metadata.key.mdblist.key")
        case "fanart_key": L10n.tr("settings.look.metadata.key.fanart.key")
        default: L10n.tr("settings.look.metadata.key.omdb.key")
        }
    }

    static func keyHint(_ key: SettingsLookKeyInfo, ownMode: Bool) -> String {
        switch key.name {
        case "tmdb_token": key.fromDocker ? L10n.tr("settings.look.metadata.docker.key") : L10n.tr("settings.look.metadata.hint.tmdb.token")
        case "tvdb_key": ownMode ? L10n.tr("settings.look.metadata.hint.tvdb.key") : L10n.tr("settings.look.metadata.tvdb.desc")
        case "tvdb_pin": L10n.tr("settings.look.metadata.hint.tvdb.pin")
        case "mdblist_key": L10n.tr("settings.look.metadata.hint.mdblist.key")
        case "fanart_key": L10n.tr("settings.look.metadata.hint.fanart.key")
        default: L10n.tr("settings.look.metadata.hint.omdb.key")
        }
    }

    static func sourceName(_ source: SettingsLookSourceInfo) -> String {
        switch source.id {
        case "tmdb": L10n.tr("settings.look.metadata.src.tmdb")
        case "tvmaze": L10n.tr("settings.look.metadata.src.tvmaze")
        case "tvdb": L10n.tr("settings.look.metadata.src.tvdb")
        case "music": L10n.tr("settings.look.metadata.src.music")
        case "trailers": L10n.tr("settings.look.metadata.src.trailers")
        case "ratings": L10n.tr("settings.look.metadata.src.ratings")
        case "art": L10n.tr("settings.look.metadata.src.art")
        case "omdb": L10n.tr("settings.look.metadata.src.omdb")
        case "charts": L10n.tr("settings.look.metadata.src.charts")
        case "streaming": L10n.tr("settings.look.metadata.src.streaming")
        case "wiki": L10n.tr("settings.look.metadata.src.wiki")
        case "anime": L10n.tr("settings.look.metadata.src.anime")
        default: source.name.isEmpty ? source.id : source.name
        }
    }

    static func testText(_ result: SettingsLookKeyResultInfo) -> String {
        if let error = result.error { return error.localized }
        switch result.code {
        case "works": return L10n.tr("settings.look.metadata.test.works")
        case "no_key": return L10n.tr("settings.look.metadata.test.no.key")
        case "refused": return L10n.tr("settings.look.metadata.test.refused")
        case "tvdb_refused": return L10n.tr("settings.look.metadata.test.tvdb.refused")
        case "tvdb_pin_refused": return L10n.tr("settings.look.metadata.test.pin.refused")
        case "unreachable": return L10n.tr("settings.look.metadata.test.unreachable")
        default: return result.message
        }
    }

    static func saved(_ kind: SettingsLookSavedKind) -> String? {
        switch kind {
        case .none: nil
        case .language: L10n.tr("settings.look.appearance.language.saved")
        case .autoplayOn: L10n.tr("settings.look.appearance.autoplay.on")
        case .autoplayOff: L10n.tr("settings.look.appearance.autoplay.off")
        case .artLibrary: L10n.tr("settings.look.appearance.art.library")
        case .artTrending: L10n.tr("settings.look.appearance.art.trending")
        }
    }

    static func languageName(_ code: String) -> String {
        let tag = platformTag(code)
        let locale = Locale(identifier: tag)
        guard let name = locale.localizedString(forIdentifier: tag), !name.isEmpty, name.lowercased() != tag.lowercased() else {
            return code
        }
        return name.prefix(1).uppercased() + String(name.dropFirst())
    }

    private static func platformTag(_ code: String) -> String {
        if code == "zh-cn" { return "zh-Hans" }
        if code == "zh-tw" { return "zh-Hant" }
        let parts = code.split(separator: "-")
        guard parts.count == 2, parts[1].count == 2 else { return code }
        return String(parts[0]) + "-" + parts[1].uppercased()
    }
}

struct SettingsLookErrorText: View {
    let error: ErrorInfo
    var prefixed = false

    private var showsServerLine: Bool {
        let statuses = [400, 409, 422, 502, 503]
        return statuses.contains(error.status) && error.kind != .partOff && !error.message.isEmpty
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(prefixed ? L10n.format("settings.look.not.saved", error.localized) : error.localized)
                .font(.callout)
                .foregroundStyle(Theme.failText)
            if showsServerLine {
                Text(L10n.format("settings.look.server.says", error.message))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }
}

struct SettingsLookHint: View {
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
    }
}

struct SettingsLookSwitchRow: View {
    let title: String
    var hint: String?
    let isOn: Binding<Bool>

    var body: some View {
        Toggle(isOn: isOn) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(title)
                if let hint {
                    SettingsLookHint(hint)
                }
            }
        }
    }
}

struct SettingsLookNumberField: View {
    let title: String
    let text: String
    let invalid: Bool
    let onChange: (String) -> Void
    @State private var value = ""

    var body: some View {
        LabeledContent(title) {
            TextField(title, text: $value)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .foregroundStyle(invalid ? Theme.failText : Theme.text)
                .frame(maxWidth: 100)
        }
        .onAppear { value = text }
        .onChange(of: text) { _, new in if new != value { value = new } }
        .onChange(of: value) { _, new in if new != text { onChange(new) } }
    }
}

struct SettingsLookSaveBar: View {
    let canSave: Bool
    let saving: Bool
    let error: ErrorInfo?
    let problem: String?
    let discard: () -> Void
    let save: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            if let error {
                SettingsLookErrorText(error: error)
            }
            if let problem {
                Text(problem)
                    .font(.callout)
                    .foregroundStyle(Theme.failText)
            }
            HStack(spacing: Theme.Spacing.small) {
                Text(L10n.tr("settings.look.unsaved"))
                    .font(.callout)
                    .foregroundStyle(Theme.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button(L10n.tr("settings.look.discard"), action: discard)
                    .buttonStyle(.bordered)
                    .disabled(saving)
                Button(action: save) {
                    if saving {
                        ProgressView()
                    } else {
                        Text(L10n.tr("settings.look.save"))
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(!canSave)
            }
        }
        .padding(Theme.Spacing.medium)
        .frame(maxWidth: .infinity)
        .background(.bar)
    }
}

struct SettingsLookLeaveGuard: ViewModifier {
    let editing: Bool
    @State private var asking = false
    @Environment(\.dismiss) private var dismiss

    func body(content: Content) -> some View {
        content
            .navigationBarBackButtonHidden(editing)
            .toolbar {
                if editing {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            asking = true
                        } label: {
                            Image(systemName: "chevron.backward")
                        }
                        .accessibilityLabel(L10n.tr("common.back"))
                    }
                }
            }
            .confirmationDialog(L10n.tr("settings.look.leave.title"), isPresented: $asking, titleVisibility: .visible) {
                Button(L10n.tr("settings.look.leave.confirm"), role: .destructive) { dismiss() }
                Button(L10n.tr("settings.look.leave.stay"), role: .cancel) {}
            } message: {
                Text(L10n.tr("settings.look.leave.text"))
            }
    }
}

extension View {
    func settingsLookLeaveGuard(editing: Bool) -> some View {
        modifier(SettingsLookLeaveGuard(editing: editing))
    }
}
