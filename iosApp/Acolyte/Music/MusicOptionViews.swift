import SwiftUI

enum MusicOptionGroupID: String, Sendable {
    case choice
    case newAlbums = "new"
    case albumType = "type"
    case extra
    case status
    case rule
}

enum MusicOptionCatalog {
    static let lists = SDKClient.shared.musicOptions()
}

enum MusicOptionText {
    static func label(_ group: MusicOptionGroupID, _ option: MusicOptionInfo) -> String {
        let key = base(group, option)
        let value = NSLocalizedString(key, comment: "")
        return value == key ? option.id : value
    }

    static func hint(_ group: MusicOptionGroupID, id: String, in options: [MusicOptionInfo]) -> String? {
        guard let option = options.first(where: { $0.id == id }) else { return nil }
        let key = base(group, option) + ".hint"
        let value = NSLocalizedString(key, comment: "")
        return value == key ? nil : value
    }

    private static func base(_ group: MusicOptionGroupID, _ option: MusicOptionInfo) -> String {
        "music.option.\(group.rawValue).\(option.key.replacingOccurrences(of: "_", with: "."))"
    }
}

struct MusicOptionPickerSection: View {
    let title: String
    let group: MusicOptionGroupID
    let options: [MusicOptionInfo]
    let selected: String
    var hint: String?
    var enabled = true
    let onSelect: @MainActor (String) -> Void

    private var footer: String {
        [MusicOptionText.hint(group, id: selected, in: options), hint].compactMap { $0 }.joined(separator: " ")
    }

    var body: some View {
        Section {
            Picker(title, selection: Binding(get: { selected }, set: { onSelect($0) })) {
                ForEach(options) { option in
                    Text(MusicOptionText.label(group, option)).tag(option.id)
                }
            }
            .disabled(!enabled)
        } footer: {
            if !footer.isEmpty { Text(footer) }
        }
    }
}

struct MusicOptionChecklistSection: View {
    let title: String
    let group: MusicOptionGroupID
    let options: [MusicOptionInfo]
    let selected: [String]
    var hint: String?
    var enabled = true
    let onToggle: @MainActor (String, Bool) -> Void

    var body: some View {
        Section {
            ForEach(options) { option in
                row(option)
            }
        } header: {
            Text(title)
        } footer: {
            if let hint { Text(hint) }
        }
    }

    private func row(_ option: MusicOptionInfo) -> some View {
        Toggle(
            MusicOptionText.label(group, option),
            isOn: Binding(get: { selected.contains(option.id) }, set: { onToggle(option.id, $0) })
        )
        .tint(Theme.accent)
        .disabled(!enabled)
    }
}
