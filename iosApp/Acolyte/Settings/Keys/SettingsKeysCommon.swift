import SwiftUI
import UIKit

struct SettingsKeysShareItem: Identifiable {
    let id = UUID()
    let text: String
}

enum SettingsKeysConfirm: Identifiable, Hashable, Sendable {
    case revoke(id: Int64, name: String)
    case replace(side: String)
    case remove(side: String)

    var id: String {
        switch self {
        case .revoke(let id, _): "revoke-\(id)"
        case .replace(let side): "replace-\(side)"
        case .remove(let side): "remove-\(side)"
        }
    }
}

enum SettingsKeysText {
    static func appName(_ side: String) -> String {
        side == "sonarr" ? "Sonarr" : "Radarr"
    }

    static func what(_ side: String) -> String {
        side == "sonarr" ? L10n.tr("settings.keys.seerr.shows") : L10n.tr("settings.keys.seerr.movies")
    }

    static func whatLower(_ side: String) -> String {
        side == "sonarr" ? L10n.tr("settings.keys.seerr.shows.lower") : L10n.tr("settings.keys.seerr.movies.lower")
    }

    static func groupLabel(_ group: String) -> String {
        switch group {
        case "see": L10n.tr("settings.keys.group.see")
        case "add": L10n.tr("settings.keys.group.add")
        case "control": L10n.tr("settings.keys.group.control")
        case "settings": L10n.tr("settings.keys.group.settings")
        case "delete": L10n.tr("settings.keys.group.delete")
        case "admin": L10n.tr("settings.keys.group.admin")
        case "approve": L10n.tr("settings.keys.group.approve")
        default: L10n.tr("settings.keys.short.plugins")
        }
    }

    static func groupHint(_ group: String) -> String? {
        switch group {
        case "admin": L10n.tr("settings.keys.group.admin.hint")
        case "approve": L10n.tr("settings.keys.group.approve.hint")
        default: nil
        }
    }

    static func groupShort(_ group: String) -> String {
        switch group {
        case "see": L10n.tr("settings.keys.short.see")
        case "add": L10n.tr("settings.keys.short.add")
        case "control": L10n.tr("settings.keys.short.control")
        case "settings": L10n.tr("settings.keys.short.settings")
        case "delete": L10n.tr("settings.keys.short.delete")
        case "admin": L10n.tr("settings.keys.short.admin")
        case "approve": L10n.tr("settings.keys.short.approve")
        default: L10n.tr("settings.keys.short.plugins")
        }
    }

    static func canLine(_ groups: [String]) -> String {
        let list = groups.isEmpty ? L10n.tr("settings.keys.ai.can.nothing") : groups.map { groupShort($0) }.joined(separator: " · ")
        return L10n.format("settings.keys.ai.can", list)
    }

    static func usage(_ createdAt: Date?, _ lastUsedAt: Date?) -> String {
        let made = createdAt?.formatted(date: .abbreviated, time: .omitted) ?? ""
        if let lastUsedAt {
            return L10n.format("settings.keys.made.used", made, Format.relative(lastUsedAt))
        }
        return L10n.format("settings.keys.made.never", made)
    }

    static func notice(_ notice: SettingsKeysNoticeInfo) -> String {
        let base: String = switch notice.kind {
        case .keyRevoked: L10n.format("settings.keys.notice.revoked", notice.name)
        case .keyRevokeFailed: L10n.tr("settings.keys.notice.revoke.failed")
        case .keyMakeFailed, .seerrKeyMakeFailed: L10n.tr("settings.keys.notice.make.failed")
        case .seerrSaveFailed, .aiSaveFailed: L10n.tr("settings.keys.notice.save.failed")
        case .seerrKeyRemoved: L10n.tr("settings.keys.notice.seerr.removed")
        case .seerrKeyRemoveFailed: L10n.tr("settings.keys.notice.seerr.remove.failed")
        case .aiOn: L10n.tr("settings.keys.notice.ai.on")
        case .aiOff: L10n.tr("settings.keys.notice.ai.off")
        case .groupsSaved: L10n.format("settings.keys.notice.groups.saved", notice.name)
        }
        guard let error = notice.error else { return base }
        return [base, error.localized, notice.detail].filter { !$0.isEmpty }.joined(separator: "\n")
    }
}

struct SettingsKeysFreshBox: View {
    let title: String
    let text: String
    let secret: String
    let copy: () -> Void
    let share: () -> Void
    let close: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.text)
            Text(text)
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
            Text(secret)
                .font(.system(.callout, design: .monospaced))
                .foregroundStyle(Theme.text)
                .textSelection(.enabled)
            HStack(spacing: Theme.Spacing.small) {
                Button(L10n.tr("settings.keys.copy"), action: copy)
                    .buttonStyle(.borderedProminent)
                Button(L10n.tr("settings.keys.share"), action: share)
                    .buttonStyle(.bordered)
                Button(L10n.tr("settings.keys.close"), action: close)
                    .buttonStyle(.borderless)
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

struct SettingsKeysCopyRow: View {
    let value: String
    let label: String
    let copy: () -> Void

    var body: some View {
        HStack(spacing: Theme.Spacing.small) {
            Text(value)
                .font(.system(.callout, design: .monospaced))
                .foregroundStyle(Theme.text)
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: copy) {
                Image(systemName: "doc.on.doc")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(label)
        }
    }
}

struct SettingsKeysRowView<Actions: View>: View {
    let label: String
    let hint: String
    let lines: [String]
    @ViewBuilder var actions: () -> Actions

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(label)
                .font(.body)
                .foregroundStyle(Theme.text)
            Text(hint)
                .font(.system(.footnote, design: .monospaced))
                .foregroundStyle(Theme.textMuted)
            ForEach(lines, id: \.self) { line in
                Text(line)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            HStack(spacing: Theme.Spacing.medium) {
                actions()
            }
            .padding(.top, Theme.Spacing.xSmall)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }
}

struct SettingsKeysGroupToggle: View {
    let group: String
    let isOn: Bool
    let enabled: Bool
    let onChange: (Bool) -> Void

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: { onChange($0) })) {
            VStack(alignment: .leading, spacing: 2) {
                Text(SettingsKeysText.groupLabel(group))
                if let hint = SettingsKeysText.groupHint(group) {
                    Text(hint)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
        .disabled(!enabled)
    }
}

private struct SettingsKeysMessageModifier: ViewModifier {
    @Binding var toast: String?
    let notice: SettingsKeysNoticeInfo?
    let dismiss: @MainActor (Int) -> Void

    private var text: String? {
        if let toast { return toast }
        if let notice { return SettingsKeysText.notice(notice) }
        return nil
    }

    private var failed: Bool {
        toast == nil && notice?.error != nil
    }

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let text {
                    Text(text)
                        .font(.callout)
                        .foregroundStyle(Theme.text)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(Theme.Spacing.medium)
                        .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous)
                                .strokeBorder(failed ? Theme.fail.opacity(0.45) : Theme.line, lineWidth: 1)
                        }
                        .padding(Theme.Spacing.screen)
                        .onTapGesture {
                            toast = nil
                            if let seq = notice?.seq { dismiss(seq) }
                        }
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.smooth(duration: 0.24), value: text)
            .task(id: toast) {
                guard toast != nil else { return }
                try? await Task.sleep(for: .seconds(3))
                toast = nil
            }
            .task(id: notice?.seq) {
                guard let seq = notice?.seq else { return }
                try? await Task.sleep(for: .seconds(5))
                dismiss(seq)
            }
    }
}

extension View {
    func settingsKeysMessage(
        toast: Binding<String?>,
        notice: SettingsKeysNoticeInfo?,
        dismiss: @escaping @MainActor (Int) -> Void
    ) -> some View {
        modifier(SettingsKeysMessageModifier(toast: toast, notice: notice, dismiss: dismiss))
    }
}

@MainActor
func settingsKeysCopy(_ text: String) {
    UIPasteboard.general.string = text
}
