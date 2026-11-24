@preconcurrency import shared
import Foundation

extension Mapping {
    static func settingsMediaServers(_ s: SettingsMediaServersState) -> SettingsMediaServersInfo {
        SettingsMediaServersInfo(
            load: load(s.status),
            allowed: s.allowed,
            isEmpty: s.isEmpty,
            servers: s.servers.map { mediaServerRow($0) },
            kinds: s.kinds.map { SettingsMediaServersKindOptionInfo(wire: $0.wire, name: $0.name) },
            form: s.form.map { mediaServerForm($0) },
            formRevision: Int(s.formRevision),
            formError: error(s.formError),
            formDetail: s.formDetail,
            canSave: s.canSave,
            saving: s.saving,
            plex: mediaServerPlex(s.plex),
            showsPlexPicker: s.showsPlexPicker,
            showWatch: s.showWatch,
            watchOn: s.watchOn,
            watchBusy: s.watchBusy,
            showLabels: s.showLabels,
            labelsEnabled: s.labelDraft.enabled,
            labelsText: s.labelDraft.label,
            labelsAskers: s.labelDraft.askers,
            labelsSavedOn: s.labels?.enabled ?? false,
            labelsCanSave: s.labelsCanSave,
            labelsCanRun: s.labelsCanRun,
            showTracks: s.showTracks,
            tracksEnabled: s.trackDraft.enabled,
            audioRows: s.audioRows.map { mediaServerTrackRow($0) },
            subtitleRows: s.subtitleRows.map { mediaServerTrackRow($0) },
            canAddAudio: s.canAddAudio,
            canAddSubtitle: s.canAddSubtitle,
            tracksCanSave: s.tracksCanSave,
            cardsRevision: Int(s.cardsRevision),
            notice: s.notice.map { mediaServerNotice($0) },
            reauth: settingsReauth(s.reauth)
        )
    }

    private static func mediaServerTag(_ t: SettingsMediaServersTag) -> SettingsMediaServersTagInfo {
        let libraries = Int(t.libraries)
        let extra = libraries > 0 ? L10n.plural("settings.mediaservers.libraries", libraries) : ""
        let label = t.ok
            ? t.label(connected: L10n.tr("settings.mediaservers.tag.connected"), extra: extra)
            : L10n.tr("settings.mediaservers.tag.not.connected")
        return SettingsMediaServersTagInfo(
            ok: t.ok,
            label: label,
            libraries: libraries,
            hover: t.hover,
            failure: t.failure,
            error: error(t.error),
            helpUrl: t.helpUrl
        )
    }

    private static func mediaServerRow(_ r: SettingsMediaServersRow) -> SettingsMediaServersRowInfo {
        SettingsMediaServersRowInfo(
            id: r.id,
            name: r.name,
            kindName: r.kindName,
            url: r.url,
            enabled: r.enabled,
            onImport: r.onImport,
            onRename: r.onRename,
            onDelete: r.onDelete,
            toldNothing: r.toldNothing,
            pathFrom: r.pathFrom,
            pathTo: r.pathTo,
            hasPathMap: r.hasPathMap,
            showsFailure: r.showsFailure,
            showsLastError: r.showsLastError,
            showsLastChecked: r.showsLastChecked,
            lastError: r.lastError,
            lastOkAt: Format.date(iso: r.lastOkAt),
            tag: r.tag.map { mediaServerTag($0) },
            testing: r.testing,
            busy: r.busy
        )
    }

    private static func mediaServerForm(_ f: SettingsMediaServersForm) -> SettingsMediaServersFormInfo {
        SettingsMediaServersFormInfo(
            id: f.id,
            kind: f.kind,
            name: f.name,
            originalName: f.originalName,
            url: f.url,
            username: f.username,
            token: f.token,
            tokenStored: f.tokenStored,
            webUrl: f.webUrl,
            plainPassword: f.plainPassword,
            onImport: f.onImport,
            onRename: f.onRename,
            onDelete: f.onDelete,
            pathFrom: f.pathFrom,
            pathTo: f.pathTo,
            allowLocalNetwork: f.allowLocalNetwork,
            enabled: f.enabled,
            usePlexSignIn: f.usePlexSignIn,
            isNew: f.isNew,
            isPlex: f.isPlex,
            isSubsonic: f.isSubsonic,
            isKodi: f.isKodi,
            takesUsername: f.takesUsername,
            takesWebUrl: f.takesWebUrl,
            takesPathMap: f.takesPathMap,
            showsToken: f.showsToken,
            urlExample: f.urlExample
        )
    }

    private static func mediaServerPlex(_ p: SettingsMediaServersPlexPick) -> SettingsMediaServersPlexInfo {
        var rows: [SettingsMediaServersPlexRowInfo] = []
        for (index, row) in p.rows.enumerated() {
            rows.append(SettingsMediaServersPlexRowInfo(index: index, name: row.name, address: row.address, place: row.place))
        }
        return SettingsMediaServersPlexInfo(
            finding: p.finding,
            searched: p.searched,
            rows: rows,
            picked: Int(p.picked),
            notLinked: p.notLinked,
            error: error(p.error),
            addresses: p.addresses
        )
    }

    private static func mediaServerTrackRow(_ r: SettingsMediaServersTrackRow) -> SettingsMediaServersTrackRowInfo {
        SettingsMediaServersTrackRowInfo(
            index: Int(r.index),
            code: r.code,
            name: r.name,
            options: r.options.map { SettingsMediaServersLanguageInfo(code: $0.code, name: $0.name) },
            canUp: r.canUp,
            canDown: r.canDown
        )
    }

    private static func mediaServerNotice(_ n: SettingsMediaServersNotice) -> SettingsMediaServersNoticeInfo {
        SettingsMediaServersNoticeInfo(
            id: n.id,
            kind: SettingsMediaServersNoticeKindInfo(rawValue: key(n.kind)) ?? .saved,
            name: n.name,
            error: error(n.error),
            detail: n.detail
        )
    }
}
