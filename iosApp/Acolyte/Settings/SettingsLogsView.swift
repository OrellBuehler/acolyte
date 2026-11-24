import SwiftUI

struct SettingsLogsView: View {
    @State private var store = SettingsServerLogsStore()
    @State private var filtering = false

    private var info: SettingsServerLogsInfo { store.info }

    var body: some View {
        Group {
            if !info.allowed {
                NotAllowedView()
            } else if info.noLogFile {
                EmptyStateView(
                    title: L10n.tr("settings.server.logs.no.file"),
                    message: L10n.tr("settings.server.logs.no.file.text"),
                    systemImage: "doc.text"
                )
            } else {
                StatusContent(load: info.load, isEmpty: false, reload: { await store.refresh() }) {
                    EmptyView()
                } content: {
                    list
                }
            }
        }
        .navigationTitle(L10n.tr("settings.page.logs"))
        .toolbar {
            if info.allowed, !info.noLogFile {
                ToolbarItemGroup(placement: .primaryAction) {
                    Button(
                        L10n.tr("settings.server.logs.filter.button"),
                        systemImage: info.filter.active ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle"
                    ) { filtering = true }
                    if info.downloading {
                        ProgressView()
                    } else {
                        Button(L10n.tr("settings.server.logs.download"), systemImage: "square.and.arrow.down") { store.download() }
                    }
                }
            }
        }
        .sheet(isPresented: $filtering) {
            SettingsServerLogsFilterSheet(
                filter: info.filter,
                apply: { level, query, from, to in
                    filtering = false
                    store.applyFilter(level, query, from, to)
                },
                clear: {
                    filtering = false
                    store.clear()
                }
            )
        }
        .sheet(isPresented: Binding(get: { info.file != nil }, set: { if !$0 { store.release() } })) {
            if let file = info.file {
                ActivityShareSheet(items: [URL(fileURLWithPath: file.path)])
            }
        }
        .settingsReauth(
            info.reauth,
            confirm: { store.confirmReauth($0, $1) },
            cancel: { store.cancelReauth() }
        )
        .safeAreaInset(edge: .bottom) {
            if let error = info.downloadError {
                NoteToast(
                    text: SettingsServerWords.withError(L10n.tr("settings.server.logs.download.failed"), error),
                    undo: nil
                )
            }
        }
        .animation(.smooth(duration: 0.24), value: info.downloadError)
        .task(id: info.downloadError) {
            guard info.downloadError != nil else { return }
            try? await Task.sleep(for: .seconds(4))
            store.dismissDownloadError()
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var list: some View {
        List {
            if info.busy {
                ProgressView()
                    .frame(maxWidth: .infinity)
            }
            if let error = info.load.error {
                NoticeBanner(text: SettingsServerWords.errorLine(error))
            }
            if info.filter.active {
                activeFilter
            }
            Section {
                if !info.lines.isEmpty {
                    ForEach(info.lines) { line in
                        row(line)
                    }
                    pager
                }
            } header: {
                Text(rangeText)
                    .textCase(nil)
            }
        }
        .refreshable { await store.refresh() }
    }

    private var rangeText: String {
        info.noMatch
            ? L10n.tr("settings.server.logs.no.match")
            : L10n.format("settings.server.logs.range", info.rangeFrom, info.rangeTo, info.total)
    }

    private var activeFilter: some View {
        Section {
            HStack(alignment: .top) {
                Text(filterSummary)
                    .font(.footnote)
                    .foregroundStyle(Theme.textMuted)
                Spacer()
                Button(L10n.tr("settings.server.logs.clear")) { store.clear() }
                    .buttonStyle(.borderless)
            }
        }
    }

    private var filterSummary: String {
        var parts: [String] = []
        if !info.filter.level.isEmpty { parts.append(SettingsServerWords.logLevelFilter(info.filter.level)) }
        if !info.filter.query.isEmpty { parts.append(info.filter.query) }
        if !info.filter.from.isEmpty { parts.append(L10n.tr("settings.server.logs.from") + " " + info.filter.from) }
        if !info.filter.to.isEmpty { parts.append(L10n.tr("settings.server.logs.to") + " " + info.filter.to) }
        return parts.joined(separator: " · ")
    }

    private func row(_ line: SettingsServerLogLineItem) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
            HStack(spacing: Theme.Spacing.small) {
                StatusChip(text: SettingsServerWords.logLevel(line.level), tone: line.tone)
                if let at = line.at {
                    Text(at.formatted(date: .abbreviated, time: .standard))
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            Text(line.text)
                .font(.footnote.monospaced())
                .textSelection(.enabled)
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var pager: some View {
        HStack {
            Button(L10n.tr("settings.server.logs.newer")) { store.newer() }
                .disabled(!info.hasNewer || info.busy)
            Button(L10n.tr("settings.server.logs.older")) { store.older() }
                .disabled(!info.hasOlder || info.busy)
        }
        .buttonStyle(.bordered)
    }
}

struct SettingsServerLogsFilterSheet: View {
    let filter: SettingsServerLogsFilterItem
    let apply: (String, String, String, String) -> Void
    let clear: () -> Void
    @State private var level = ""
    @State private var query = ""
    @State private var from: Date?
    @State private var to: Date?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker(L10n.tr("settings.server.logs.level"), selection: $level) {
                        Text(L10n.tr("settings.server.logs.every")).tag("")
                        Text(L10n.tr("settings.server.logs.warnings")).tag("warning")
                        Text(L10n.tr("settings.server.logs.errors")).tag("error")
                    }
                    TextField(L10n.tr("settings.server.logs.text"), text: $query, prompt: Text(L10n.tr("settings.server.logs.example")))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .submitLabel(.search)
                        .onSubmit { submit() }
                }
                Section {
                    dateRow(L10n.tr("settings.server.logs.from"), $from)
                    dateRow(L10n.tr("settings.server.logs.to"), $to)
                }
                Section {
                    Button(L10n.tr("settings.server.logs.clear"), role: .destructive) { clear() }
                }
            }
            .navigationTitle(L10n.tr("settings.server.logs.filter"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("settings.server.logs.apply")) { submit() }
                }
            }
        }
        .onAppear {
            level = filter.level
            query = filter.query
            from = SettingsServerDays.date(filter.from)
            to = SettingsServerDays.date(filter.to)
        }
    }

    private func submit() {
        apply(level, query, from.map { SettingsServerDays.text($0) } ?? "", to.map { SettingsServerDays.text($0) } ?? "")
    }

    private func dateRow(_ title: String, _ date: Binding<Date?>) -> some View {
        HStack {
            Text(title)
            Spacer()
            if let value = date.wrappedValue {
                DatePicker("", selection: Binding(get: { value }, set: { date.wrappedValue = $0 }), displayedComponents: .date)
                    .labelsHidden()
                Button {
                    date.wrappedValue = nil
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Theme.textFaint)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(L10n.tr("settings.server.logs.clear.date"))
            } else {
                Button(L10n.tr("settings.server.logs.any.date")) { date.wrappedValue = Date() }
                    .buttonStyle(.borderless)
            }
        }
    }
}
