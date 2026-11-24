import SwiftUI

struct MyDevicesView: View {
    let title: String
    var personName = ""
    @State private var store: MyDevicesStore
    @State private var confirmEndAll = false

    init(userId: Int64?, title: String, personName: String = "") {
        self.title = title
        self.personName = personName
        _store = State(initialValue: MyDevicesStore(userId: userId))
    }

    var body: some View {
        let info = store.info
        StatusContent(load: info.load, isEmpty: info.sessions.isEmpty, reload: { await store.refresh() }) {
            EmptyStateView(title: L10n.tr("myaccount.devices.empty"), systemImage: "iphone.and.ipad")
        } content: {
            List {
                if let error = info.actionError {
                    NoticeBanner(text: MyErrorText.text(error))
                        .listRowSeparator(.hidden)
                }
                Section {
                    ForEach(info.sessions) { session in
                        DeviceRow(session: session, busy: info.busy) { store.end(session.id) }
                    }
                } footer: {
                    Text(L10n.tr("myaccount.devices.hint"))
                }
                if info.hasOthers {
                    Section {
                        Button(endAllTitle, role: .destructive) {
                            confirmEndAll = true
                        }
                        .disabled(info.busy)
                    }
                }
            }
            .refreshable { await store.refresh() }
        }
        .navigationTitle(title)
        .noticeToast(store.notices)
        .confirmationDialog(endAllTitle, isPresented: $confirmEndAll, titleVisibility: .visible) {
            Button(endAllTitle, role: .destructive) { store.endAll() }
        }
        .task { await store.watch() }
        .task { await store.load() }
    }

    private var endAllTitle: String {
        personName.isEmpty ? L10n.tr("myaccount.devices.end.all") : L10n.format("people.devices.end.all", personName)
    }
}

private struct DeviceRow: View {
    let session: MySessionInfo
    let busy: Bool
    let end: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.medium) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xSmall) {
                HStack(spacing: Theme.Spacing.small) {
                    Text(session.device.isEmpty ? L10n.tr("myaccount.devices.unknown") : session.device)
                        .font(.headline)
                    if session.current {
                        StatusChip(text: L10n.tr("myaccount.devices.this"), tone: .ok)
                    }
                }
                if let line = detail {
                    Text(line)
                        .font(.footnote)
                        .foregroundStyle(Theme.textMuted)
                }
            }
            Spacer(minLength: Theme.Spacing.small)
            if !session.current {
                Button(L10n.tr("myaccount.devices.end"), role: .destructive, action: end)
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .disabled(busy)
            }
        }
        .padding(.vertical, Theme.Spacing.xSmall)
    }

    private var detail: String? {
        switch (session.createdAt, session.lastUsedAt) {
        case let (created?, used?):
            L10n.format("myaccount.devices.detail", Format.relative(created), Format.relative(used))
        case let (created?, nil):
            L10n.format("myaccount.devices.signed.in", Format.relative(created))
        case let (nil, used?):
            L10n.format("myaccount.devices.last.used", Format.relative(used))
        default:
            nil
        }
    }
}
