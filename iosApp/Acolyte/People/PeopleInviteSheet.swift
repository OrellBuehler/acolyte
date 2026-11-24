import SwiftUI

struct PeopleAccessEditor: View {
    @Binding var admin: Bool
    @Binding var perms: Set<String>
    @Binding var rating: String
    let grantable: [String]
    let canSetRole: Bool

    var body: some View {
        Picker(L10n.tr("people.preset"), selection: presetBinding) {
            ForEach(options) { preset in
                Text(PeopleText.preset(preset)).tag(preset)
            }
        }
        if selected == .custom {
            Text(L10n.tr("people.preset.custom.hint"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        if !admin && (rating != "" || selected == .kid) {
            Picker(L10n.tr("people.rating"), selection: $rating) {
                ForEach(PeopleRules.ratings, id: \.self) { value in
                    Text(value).tag(value)
                }
            }
            .pickerStyle(.segmented)
            if rating != "" {
                Text(L10n.format("people.rating.hint", rating, PeopleRules.ratingForShows(rating)))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        ForEach(grantable, id: \.self) { perm in
            Toggle(PeopleText.permission(perm), isOn: toggle(perm))
        }
    }

    private var selected: PeoplePresetKind {
        if admin { return .admin }
        let derived = PeopleRules.preset(perms: Array(perms), rating: rating)
        return derived == .admin ? .custom : derived
    }

    private var options: [PeoplePresetKind] {
        var list = PeoplePresetKind.allCases.filter { preset in
            switch preset {
            case .admin: canSetRole
            case .custom: true
            default: PeopleRules.presetPerms(preset).allSatisfy { grantable.contains($0) }
            }
        }
        if !list.contains(selected) { list.append(selected) }
        return list
    }

    private var presetBinding: Binding<PeoplePresetKind> {
        Binding(get: { selected }, set: { choose($0) })
    }

    private func choose(_ preset: PeoplePresetKind) {
        switch preset {
        case .custom:
            admin = false
        case .admin:
            admin = true
            perms = Set(PeopleRules.presetPerms(.admin).filter { grantable.contains($0) })
            rating = ""
        default:
            admin = false
            perms = Set(PeopleRules.presetPerms(preset).filter { grantable.contains($0) })
            rating = PeopleRules.presetRating(preset)
        }
    }

    private func toggle(_ perm: String) -> Binding<Bool> {
        Binding(
            get: { perms.contains(perm) },
            set: { on in
                if on { perms.insert(perm) } else { perms.remove(perm) }
            }
        )
    }
}

struct PeopleInviteSheet: View {
    let store: PeopleStore
    @Environment(\.dismiss) private var dismiss
    @State private var admin = false
    @State private var perms: Set<String> = []
    @State private var rating = ""
    @State private var note = ""
    @State private var days = 7
    @State private var maxUses = 1
    @State private var seeded = false

    var body: some View {
        let info = store.info
        let unlimited = info.viewerOwner && !admin
        let daysRange = (unlimited ? 0 : 1)...30
        let usesLower = unlimited ? 0 : 1
        let usesValid = admin || (usesLower...1000).contains(maxUses)
        NavigationStack {
            Form {
                if let error = info.actionError {
                    NoticeBanner(text: MyErrorText.text(error))
                }
                if let problem = info.problem {
                    NoticeBanner(text: PeopleStore.problemText(problem, minimum: info.minPasswordLength, unlimited: unlimited))
                }
                Section {
                    PeopleAccessEditor(
                        admin: $admin,
                        perms: $perms,
                        rating: $rating,
                        grantable: info.grantable,
                        canSetRole: info.viewerOwner
                    )
                } header: {
                    Text(L10n.tr("people.invite.access"))
                }
                Section {
                    TextField(L10n.tr("people.invite.note"), text: $note)
                        .onChange(of: note) { _, value in
                            if value.count > 100 { note = String(value.prefix(100)) }
                        }
                    Stepper(value: $days, in: daysRange) {
                        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                            Text(L10n.format("people.invite.days", days))
                            if unlimited && days == 0 {
                                Text(L10n.tr("people.invite.days.zero"))
                                    .font(.footnote)
                                    .foregroundStyle(Theme.textMuted)
                            }
                        }
                    }
                    if !admin {
                        DisclosureGroup(L10n.tr("people.invite.more")) {
                            LabeledContent(L10n.tr("people.invite.max.uses")) {
                                TextField("", value: $maxUses, format: .number)
                                    .keyboardType(.numberPad)
                                    .multilineTextAlignment(.trailing)
                            }
                            if !usesValid {
                                FormHint(text: L10n.tr(unlimited ? "people.problem.uses.unlimited" : "people.problem.uses"), failed: true)
                            } else if unlimited && maxUses == 0 {
                                FormHint(text: L10n.tr("people.invite.days.zero"))
                            }
                        }
                    }
                } footer: {
                    if admin || maxUses == 1 {
                        Text(L10n.tr("people.invite.one.time"))
                    }
                }
                Section {
                    Button {
                        make()
                    } label: {
                        HStack {
                            Text(L10n.tr("people.invite.make"))
                            if info.busy {
                                Spacer()
                                ProgressView()
                            }
                        }
                    }
                    .disabled(info.busy || !usesValid)
                }
            }
            .navigationTitle(L10n.tr("people.invite.someone"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
            }
            .onAppear { seed(info) }
            .onChange(of: admin) { _, value in
                if value {
                    days = max(days, 1)
                    maxUses = 1
                }
            }
            .onChange(of: info.fresh) { _, value in
                if value != nil { dismiss() }
            }
            .reauthSheet(store.reauth)
        }
        .presentationDetents([.large])
    }

    private func seed(_ info: PeopleListInfo) {
        guard !seeded else { return }
        seeded = true
        store.clearProblems()
        perms = Set(PeopleRules.presetPerms(.member).filter { info.grantable.contains($0) })
    }

    private func make() {
        let info = store.info
        let ordered = PeopleRules.perms.filter { perms.contains($0) }
        let draft = PeopleInviteDraftInfo(
            admin: admin && info.viewerOwner,
            perms: ordered,
            rating: admin ? "" : rating,
            note: note.trimmingCharacters(in: .whitespaces),
            days: days,
            maxUses: admin ? 1 : maxUses
        )
        store.makeInvite(draft)
    }
}
