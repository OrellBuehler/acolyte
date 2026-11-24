import SwiftUI

struct SettingsSignInLocalSection: View {
    let store: SettingsSignInStore
    @State private var howToFix = false

    private var info: SettingsSignInInfo { store.info }

    var body: some View {
        let local = info.local
        Section {
            Toggle(
                L10n.tr("settings.signin.skip.local"),
                isOn: Binding(get: { local.skip }, set: { store.setSkip($0) })
            )
            .disabled(!info.isOwner || local.skipSaving)
            SettingsSignInHint(text: L10n.tr("settings.signin.skip.home"))
            if !local.mediaApps.isEmpty {
                SettingsSignInHint(text: L10n.format("settings.signin.media.on", local.mediaApps.joined(separator: ", ")))
            }
            notices(local)
            if info.isOwner { who(local) }
            networks(local)
            proxies(local)
            save(local)
        } header: {
            Text(L10n.tr("settings.signin.local"))
        }
        .disabled(!info.isOwner)
    }

    @ViewBuilder
    private func notices(_ local: SettingsSignInLocalInfo) -> some View {
        if local.skip, local.skipOff == .persongone {
            SettingsSignInNote(text: L10n.tr("settings.signin.off.gone"))
        }
        if local.skip, local.skipOff == .kid {
            SettingsSignInNote(text: L10n.tr("settings.signin.off.kid"))
        }
        if local.seen == .owngateway {
            SettingsSignInNote(
                text: L10n.tr("settings.signin.bridge.ok.body"),
                title: L10n.format("settings.signin.bridge.ok.title", local.seenAddress),
                warn: false
            )
        }
        if local.seen == .bridge {
            SettingsSignInNote(
                text: L10n.tr("settings.signin.bridge.body"),
                title: L10n.format("settings.signin.bridge.title", local.seenAddress)
            )
            DisclosureGroup(L10n.tr("settings.signin.how.to.fix"), isExpanded: $howToFix) {
                SettingsSignInHint(text: L10n.tr("settings.signin.how.to.fix.body"))
            }
        }
        if local.proxyUntrusted {
            SettingsSignInHint(text: L10n.tr("settings.signin.proxy.untrusted"))
        }
    }

    @ViewBuilder
    private func who(_ local: SettingsSignInLocalInfo) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.small) {
            Text(L10n.tr("settings.signin.who"))
                .font(.subheadline.weight(.semibold))
            if let error = local.peopleError {
                SettingsSignInFail(text: SettingsSignInWords.failure(error))
                Button(L10n.tr("settings.signin.try.again")) { store.retryPeople() }
                    .buttonStyle(.bordered)
            } else if let people = local.people {
                ForEach(people) { person in
                    personRow(person, picked: person.id == local.pickedId)
                }
            } else {
                ProgressView()
            }
            SettingsSignInHint(text: L10n.tr("settings.signin.who.hint"))
        }
    }

    private func personRow(_ person: SettingsSignInPersonInfo, picked: Bool) -> some View {
        Button {
            store.pickPerson(person.id)
        } label: {
            HStack(spacing: Theme.Spacing.medium) {
                AuthImage(source: person.avatar.isEmpty ? nil : person.avatar, size: .thumb) {
                    ArtPlaceholderView(name: person.name)
                }
                .frame(width: 36, height: 36)
                .clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(person.name)
                        .foregroundStyle(Theme.text)
                    Text(SettingsSignInWords.role(person))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
                Spacer(minLength: 0)
                if picked {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Theme.accentText)
                }
            }
            .padding(Theme.Spacing.small)
            .background(Theme.surfaceRaised, in: RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: Theme.Radius.button, style: .continuous)
                    .strokeBorder(picked ? Theme.accent : Theme.line, lineWidth: picked ? 2 : 1)
            }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(picked ? .isSelected : [])
    }

    @ViewBuilder
    private func networks(_ local: SettingsSignInLocalInfo) -> some View {
        SettingsSignInArea(
            title: L10n.tr("settings.signin.home.nets"),
            text: local.networks,
            onChange: { store.setLocalNetworks($0) },
            hint: L10n.tr("settings.signin.home.nets.hint"),
            minHeight: 110,
            invalid: !local.netProblems.isEmpty
        )
        problems(local.netProblems)
        if let seen = SettingsSignInWords.seen(local) {
            HStack(spacing: Theme.Spacing.small) {
                Circle()
                    .fill(local.seen == .owngateway || local.seen == .covered ? Theme.ok : Theme.wait)
                    .frame(width: 8, height: 8)
                Text(seen)
                    .font(.footnote)
            }
        }
        if info.isOwner {
            Button(L10n.tr("settings.signin.add.home")) { store.addHomeRanges() }
                .buttonStyle(.bordered)
            Toggle(isOn: Binding(get: { local.hasTailscale }, set: { store.setTailscale($0) })) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L10n.tr("settings.signin.cgnat"))
                    Text(L10n.format("settings.signin.cgnat.hint", local.cgnatRange))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        }
    }

    @ViewBuilder
    private func proxies(_ local: SettingsSignInLocalInfo) -> some View {
        Text(L10n.tr("settings.signin.proxies"))
            .font(.subheadline.weight(.semibold))
        ForEach(local.fixedProxies, id: \.self) { address in
            HStack(spacing: Theme.Spacing.small) {
                Text(address).font(.body.monospaced())
                Text("· " + L10n.tr("settings.signin.fixed.row"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        SettingsSignInArea(
            title: L10n.tr("settings.signin.proxies"),
            text: local.proxies,
            onChange: { store.setLocalProxies($0) },
            hint: L10n.tr("settings.signin.proxies.help"),
            minHeight: 66,
            disabled: info.apiKey,
            invalid: !local.proxyProblems.isEmpty
        )
        problems(local.proxyProblems)
        if info.apiKey {
            SettingsSignInHint(text: L10n.tr("settings.signin.proxies.needs.session"))
        }
    }

    @ViewBuilder
    private func problems(_ list: [SettingsSignInProblemInfo]) -> some View {
        if !list.isEmpty {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                SettingsSignInFail(text: L10n.plural("settings.signin.lines.fix", list.count))
                ForEach(list) { problem in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L10n.format("settings.signin.line.n", problem.line) + "  " + problem.text)
                            .font(.footnote.monospaced())
                        Text(SettingsSignInWords.fix(problem))
                            .font(.footnote)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func save(_ local: SettingsSignInLocalInfo) -> some View {
        if info.isOwner {
            HStack(spacing: Theme.Spacing.medium) {
                Button(L10n.tr("settings.signin.save")) { store.saveLocal() }
                    .buttonStyle(.borderedProminent)
                    .disabled(!local.canSave)
                if local.saving { ProgressView() }
                if local.problemCount > 0 {
                    Text(L10n.plural("settings.signin.fix.to.save", local.problemCount))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            if let error = local.saveError {
                SettingsSignInFail(text: SettingsSignInWords.failure(error))
            }
        } else {
            SettingsSignInHint(text: L10n.tr("settings.signin.owner.only"))
        }
    }
}
