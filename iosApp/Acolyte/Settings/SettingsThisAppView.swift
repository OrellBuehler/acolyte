import SwiftUI
import UIKit

struct SettingsThisAppView: View {
    @State private var store = SettingsControlsThisAppStore()
    @State private var advanced = false

    private var info: SettingsControlsThisAppInfo { store.info }

    var body: some View {
        StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
            EmptyView()
        } content: {
            form
        }
        .navigationTitle(L10n.tr("settings.page.this.app"))
        .settingsControlsToast(store.toaster)
        .settingsReauth(info.reauth, confirm: { store.confirmReauth($0, $1) }, cancel: { store.cancelReauth() })
        .task { await store.watch() }
        .task { await store.load() }
        .task { await store.followLive() }
    }

    private var form: some View {
        Form {
            if let error = info.load.error {
                Section { NoticeBanner(text: error.localized) }
            }
            factsSection
            if info.canSettings && info.uiLoaded {
                historySection
            }
            if info.canSettings {
                Section {
                    Button(advanced ? L10n.tr("settings.controls.advanced.hide") : L10n.tr("settings.controls.advanced")) {
                        advanced.toggle()
                    }
                }
            }
            if info.canSettings && advanced {
                if info.uiLoaded { serverSection }
                storageSection
                if info.urlBaseAvailable { urlBaseSection }
                if info.isOwner && info.proxyAvailable && info.proxyLoaded { proxySection }
            }
            Section {
                Text(L10n.tr("settings.controls.licence"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
        .refreshable { await store.refresh() }
    }

    private var factsSection: some View {
        Section {
            LabeledContent(L10n.tr("settings.controls.facts.version"), value: info.version.isEmpty ? "-" : info.version)
            LabeledContent(L10n.tr("settings.controls.facts.uptime"), value: info.uptime.isEmpty ? "-" : info.uptime)
            if info.schema > 0 {
                LabeledContent(L10n.tr("settings.controls.facts.schema"), value: String(info.schema))
            }
            LabeledContent(L10n.tr("settings.controls.facts.live"), value: String(info.liveClients))
        } header: {
            Text(L10n.tr("settings.controls.facts.title"))
        }
    }

    private var historySection: some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Picker(
                    L10n.tr("settings.controls.keep.routine"),
                    selection: Binding(get: { info.historyRoutineDays }, set: { store.setHistoryRoutine($0) })
                ) {
                    ForEach(info.routineChoices, id: \.self) { days in
                        Text(SettingsControlsText.keep(days)).tag(days)
                    }
                }
                Text(L10n.tr("settings.controls.keep.routine.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Picker(
                    L10n.tr("settings.controls.keep.other"),
                    selection: Binding(get: { info.historyOtherDays }, set: { store.setHistoryOther($0) })
                ) {
                    ForEach(info.otherChoices, id: \.self) { days in
                        Text(SettingsControlsText.keep(days)).tag(days)
                    }
                }
                Text(L10n.tr("settings.controls.keep.other.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        } header: {
            Text(L10n.tr("settings.controls.history.title"))
        } footer: {
            if !info.canChangeHistory {
                Text(L10n.tr("settings.controls.history.owner"))
            }
        }
        .disabled(!info.canChangeHistory || info.uiBusy)
    }

    private var serverSection: some View {
        Section {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Picker(
                    L10n.tr("settings.controls.pictures"),
                    selection: Binding(get: { info.cacheGb }, set: { store.setCacheGb($0) })
                ) {
                    ForEach(info.cacheChoices, id: \.self) { gb in
                        Text(L10n.format("settings.controls.gb", gb)).tag(gb)
                    }
                }
                Text(L10n.tr("settings.controls.pictures.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            Toggle(isOn: Binding(get: { info.countInstall }, set: { store.setCountInstall($0) })) {
                VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                    Text(L10n.tr("settings.controls.count"))
                    Text(L10n.tr("settings.controls.count.hint"))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
        } header: {
            Text(L10n.tr("settings.controls.server.title"))
        }
        .disabled(info.uiBusy)
    }

    private var storageSection: some View {
        Section {
            if let storage = info.storage {
                ForEach(storage.app, id: \.path) { row in
                    pathRow(row, folder: false)
                }
                ForEach(storage.folders, id: \.path) { row in
                    pathRow(row, folder: true)
                }
                if !storage.savePath.isEmpty {
                    VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                        Text(L10n.tr("settings.controls.save.folder"))
                            .font(.headline)
                        pathText(storage.savePath)
                    }
                }
            } else if info.storageProof {
                Text(L10n.tr("settings.controls.storage.proof"))
                    .foregroundStyle(Theme.textMuted)
                Button(L10n.tr("settings.controls.storage.confirm")) { store.proveStorage() }
            } else if let error = info.storageError {
                Text(SettingsControlsText.errorText(error))
                    .foregroundStyle(Theme.failText)
            }
        } header: {
            Text(L10n.tr("settings.controls.storage.title"))
        } footer: {
            Text(L10n.tr("settings.controls.storage.desc"))
        }
    }

    private func pathRow(_ row: SettingsControlsPathRowInfo, folder: Bool) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            Text(row.size.isEmpty ? row.name : row.name + " · " + row.size)
                .font(.headline)
            if folder {
                Text(L10n.tr("settings.controls.library.folder"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
            pathText(row.path)
            Text(row.freeKnown ? L10n.format("settings.controls.free", row.free) : L10n.tr("settings.controls.free.unknown"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
    }

    private func pathText(_ path: String) -> some View {
        HStack(alignment: .top) {
            Text(path)
                .font(.footnote.monospaced())
                .textSelection(.enabled)
            Spacer()
            Button {
                UIPasteboard.general.string = path
                store.toaster.show(L10n.tr("settings.controls.copied"))
            } label: {
                Image(systemName: "doc.on.doc")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(L10n.tr("settings.controls.copy.path"))
        }
    }

    private var urlBaseSection: some View {
        Section {
            SettingsControlsField(
                title: L10n.tr("settings.controls.url.base"),
                text: info.urlBaseDraft,
                prompt: "/holyarrs",
                monospaced: true,
                keyboard: .URL
            ) { store.setUrlBase($0) }
            if let urlBase = info.urlBase {
                let inUse = urlBase.inUse.isEmpty ? L10n.tr("settings.controls.none") : urlBase.inUse
                Text(L10n.format("settings.controls.in.use", inUse))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                if urlBase.restartNeeded {
                    Text(L10n.tr("settings.controls.needs.restart"))
                        .font(.footnote)
                        .foregroundStyle(Theme.waitText)
                }
            }
            if let error = info.urlBaseError {
                Text(SettingsControlsText.errorText(error))
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
            }
            Button(L10n.tr("settings.controls.save")) { store.saveUrlBase() }
                .disabled(!info.canSaveUrlBase)
        } header: {
            Text(L10n.tr("settings.controls.url.base"))
        } footer: {
            Text(L10n.tr("settings.controls.url.base.desc"))
        }
    }

    private var proxySection: some View {
        Section {
            Picker(
                L10n.tr("settings.controls.proxy.type"),
                selection: Binding(get: { info.proxy.type }, set: { store.setProxyType($0) })
            ) {
                Text(L10n.tr("settings.controls.proxy.off")).tag("")
                Text("HTTP").tag("http")
                Text("HTTPS").tag("https")
                Text("SOCKS5").tag("socks5")
            }
            if info.proxyOn {
                proxyFields
            }
            if let error = info.proxyError {
                Text(SettingsControlsText.errorText(error))
                    .font(.footnote)
                    .foregroundStyle(Theme.failText)
            }
            Button(L10n.tr("settings.controls.save")) { store.saveProxy() }
                .disabled(!info.canSaveProxy)
        } header: {
            Text(L10n.tr("settings.controls.proxy"))
        } footer: {
            Text(L10n.tr("settings.controls.proxy.desc"))
        }
        .disabled(info.proxySaving)
    }

    @ViewBuilder
    private var proxyFields: some View {
        SettingsControlsField(
            title: L10n.tr("settings.controls.proxy.host"),
            text: info.proxy.host,
            prompt: "proxy.lan",
            monospaced: true,
            keyboard: .URL
        ) { store.setProxyHost($0) }
        SettingsControlsField(
            title: L10n.tr("settings.controls.proxy.port"),
            text: info.proxy.port,
            monospaced: true,
            keyboard: .numberPad
        ) { store.setProxyPort($0) }
        SettingsControlsField(
            title: L10n.tr("settings.controls.proxy.user"),
            text: info.proxy.username,
            monospaced: true
        ) { store.setProxyUsername($0) }
        SettingsControlsField(
            title: L10n.tr("settings.controls.proxy.password"),
            text: info.proxy.password,
            secure: true
        ) { store.setProxyPassword($0) }
        if info.proxyPasswordSaved {
            Text(L10n.tr("settings.controls.proxy.password.kept"))
                .font(.footnote)
                .foregroundStyle(Theme.textMuted)
        }
        SettingsControlsField(
            title: L10n.tr("settings.controls.proxy.bypass"),
            text: info.proxy.bypass,
            prompt: "*.example.com, 10.0.0.0/8",
            monospaced: true
        ) { store.setProxyBypass($0) }
        Text(L10n.tr("settings.controls.proxy.bypass.hint"))
            .font(.footnote)
            .foregroundStyle(Theme.textMuted)
        Toggle(isOn: Binding(get: { info.proxy.bypassLocal }, set: { store.setProxyBypassLocal($0) })) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                Text(L10n.tr("settings.controls.proxy.bypass.local"))
                Text(L10n.tr("settings.controls.proxy.bypass.local.hint"))
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
            }
        }
    }
}
