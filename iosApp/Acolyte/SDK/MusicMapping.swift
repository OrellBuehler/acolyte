@preconcurrency import shared
import Foundation

extension Mapping {
    private static func musicEnum<T: RawRepresentable>(_ value: Any, _ fallback: T) -> T where T.RawValue == String {
        T(rawValue: key(value)) ?? fallback
    }

    static func musicNote(_ n: MusicNote?) -> MusicNoteInfo? {
        guard let n else { return nil }
        return MusicNoteInfo(
            id: Int(n.id),
            kind: musicEnum(n.kind, MusicNoteKindID.failed),
            text: n.text,
            count: Int(n.count),
            error: error(n.error)
        )
    }

    static func musicLine(_ l: MusicLine) -> MusicLineInfo {
        MusicLineInfo(kind: musicEnum(l.kind, MusicLineKindID.none), artist: l.artist, text: l.text, date: l.date, number: l.number)
    }

    static func musicTarget(_ t: MusicTarget) -> MusicTargetInfo {
        MusicTargetInfo(
            kind: musicEnum(t.kind, MusicOpenKindID.none),
            mbid: t.mbid,
            artist: t.artist,
            titleId: t.titleId,
            partId: t.partId,
            page: t.page,
            slug: t.slug,
            query: t.query
        )
    }

    static func musicAccess(_ a: MusicAccess) -> MusicAccessInfo {
        MusicAccessInfo(manage: a.manage, ask: a.ask, show: a.show, settings: a.settings, kid: a.kid, handsOff: a.handsOff)
    }

    static func musicOptions(_ list: [MusicOption]) -> [MusicOptionInfo] {
        list.map { MusicOptionInfo(id: $0.id, key: $0.key) }
    }

    static func musicAlbum(_ a: MusicAlbum) -> MusicAlbumInfo {
        MusicAlbumInfo(
            mbid: a.mbid,
            name: a.name,
            kind: musicEnum(a.kind, MusicAlbumKindID.album),
            year: a.year,
            date: a.date,
            image: a.image,
            explicit: a.explicit,
            upcoming: a.upcoming,
            mark: musicEnum(a.mark, MusicMarkKind.none),
            following: a.following,
            partId: a.partId,
            artistMbid: a.artistMbid,
            artistName: a.artistName,
            asked: a.asked
        )
    }

    static func musicArtistCard(_ a: MusicArtistCard) -> MusicArtistCardInfo {
        MusicArtistCardInfo(mbid: a.mbid, name: a.name, image: a.image, note: a.note)
    }

    static func musicArtist(_ s: MusicArtistState) -> MusicArtistInfo {
        MusicArtistInfo(
            load: load(s.status),
            mbid: s.mbid,
            name: s.name,
            image: s.image,
            wide: s.wide,
            tint: s.tint,
            kind: musicEnum(s.kind, MusicArtistKindID.artist),
            country: s.country,
            disambiguation: s.disambiguation,
            ended: s.ended,
            genres: s.genres,
            listeners: s.listeners,
            rank: Int(s.rank),
            change: s.change,
            plays: s.plays,
            titleId: s.titleId,
            followKnown: s.followKnown,
            following: s.following,
            bio: s.bio.map { MusicBioInfo(text: $0.text, url: $0.url, license: $0.license, licenseUrl: $0.licenseUrl, attribution: $0.attribution) },
            top: s.top.map { song in
                MusicSongInfo(
                    title: song.title,
                    album: song.album,
                    albumMbid: song.albumMbid,
                    year: song.year,
                    length: song.length,
                    image: song.image,
                    explicit: song.explicit
                )
            },
            essential: s.essential.map { musicAlbum($0) },
            shelves: s.shelves.map { shelf in
                MusicShelfInfo(kind: musicEnum(shelf.kind, MusicShelfKindID.other), albums: shelf.albums.map { musicAlbum($0) })
            },
            storeAlbums: s.storeAlbums.map { item in
                MusicStoreItemInfo(
                    title: item.title,
                    year: item.year,
                    kind: musicEnum(item.kind, MusicAlbumKindID.album),
                    tracks: Int(item.tracks),
                    image: item.image,
                    explicit: item.explicit
                )
            },
            coming: s.coming.map { musicAlbum($0) },
            videos: s.videos.map { MusicVideoInfo(id: $0.id, title: $0.title, length: $0.length, thumb: $0.thumb, url: $0.url) },
            appearsOn: s.appearsOn.map { musicAlbum($0) },
            related: s.related.map { musicArtistCard($0) },
            similar: s.similar.map { musicArtistCard($0) },
            similarLoading: s.similarLoading,
            albumCount: Int(s.albumCount),
            singleCount: Int(s.singleCount),
            activeSince: s.activeSince,
            activeUntil: s.activeUntil,
            gap: s.gap,
            incomplete: s.incomplete,
            lookupLoaded: s.lookupLoaded,
            asked: s.asked,
            askedWhole: s.askedWhole,
            neverAdd: s.neverAdd,
            canAdd: s.canAdd,
            canAsk: s.canAsk,
            askAll: s.askAll.map { MusicAskAllInfo(count: Int($0.count), gb: $0.gb, size: $0.size) },
            asking: s.asking,
            note: musicNote(s.note),
            main: musicEnum(s.main, MusicMainKind.none),
            mainReady: s.mainReady,
            problem: musicEnum(s.problem, MusicProblemKind.none),
            hasDiscography: s.hasDiscography
        )
    }

    static func musicAlbumPage(_ s: MusicAlbumState) -> MusicAlbumPageInfo {
        MusicAlbumPageInfo(
            load: load(s.status),
            mbid: s.mbid,
            artistMbid: s.artistMbid,
            name: s.name,
            artistName: s.artistName,
            kind: musicEnum(s.kind, MusicAlbumKindID.album),
            year: s.year,
            date: s.date,
            image: s.image,
            tint: s.tint,
            explicit: s.explicit,
            disambiguation: s.disambiguation,
            label: s.label,
            songs: Int(s.songs),
            minutes: Int(s.minutes),
            tracks: s.tracks.map { row in
                MusicTrackRowInfo(
                    disc: Int(row.disc),
                    number: Int(row.number),
                    name: row.name,
                    length: row.length,
                    explicit: row.explicit,
                    have: musicEnum(row.have, MusicHaveKind.unknown)
                )
            },
            discs: Int(s.discs),
            tracksWaiting: s.tracksWaiting,
            gap: s.gap,
            editions: s.editions.map { MusicEditionInfo(mbid: $0.mbid, label: $0.label, tracks: Int($0.tracks)) },
            edition: s.edition,
            titleId: s.titleId,
            partId: s.partId,
            inLibraryAlbum: s.inLibraryAlbum,
            mark: musicEnum(s.mark, MusicMarkKind.none),
            have: Int(s.have),
            missing: Int(s.missing),
            moreBy: s.moreBy.map { musicAlbum($0) },
            near: s.near.map { musicAlbum($0) },
            similar: s.similar.map { musicArtistCard($0) },
            followKnown: s.followKnown,
            following: s.following,
            lookupLoaded: s.lookupLoaded,
            asked: s.asked,
            canAdd: s.canAdd,
            canAsk: s.canAsk,
            busy: s.busy,
            note: musicNote(s.note),
            cta: musicEnum(s.cta, MusicAlbumCtaKind.none),
            ctaReady: s.ctaReady,
            problem: musicEnum(s.problem, MusicProblemKind.none),
            showCounts: s.showCounts
        )
    }

    static func musicItem(_ i: MusicItem) -> MusicItemInfo {
        MusicItemInfo(
            key: i.key,
            kind: musicEnum(i.kind, MusicCardKindID.album),
            mbid: i.mbid,
            name: i.name,
            artist: i.artist,
            artistMbid: i.artistMbid,
            titleId: i.titleId,
            partId: i.partId,
            image: i.image,
            images: i.images,
            cover: i.cover,
            line: musicLine(i.line),
            mark: musicEnum(i.mark, MusicMarkKind.none),
            fresh: i.fresh,
            kicker: i.kicker,
            badge: i.badge,
            explicit: i.explicit,
            upcoming: i.upcoming,
            following: i.following,
            rank: Int(i.rank),
            hue: Int(i.hue),
            color: i.color,
            target: musicTarget(i.target),
            action: musicEnum(i.action, MusicCardActionKind.none),
            date: i.date,
            track: i.track
        )
    }

    static func musicShelfRow(_ r: MusicShelfRow) -> MusicShelfRowInfo {
        MusicShelfRowInfo(
            id: r.id,
            type: r.type,
            title: r.title,
            subtitle: r.subtitle,
            shape: musicEnum(r.shape, MusicShapeKind.square),
            items: r.items.map { musicItem($0) },
            regional: r.regional
        )
    }

    static func musicRows(_ s: MusicRowsState) -> MusicRowsInfo {
        MusicRowsInfo(
            load: load(s.status),
            kind: s.kind,
            slug: s.slug,
            tileKind: s.tileKind,
            title: s.title,
            hue: Int(s.hue),
            color: s.color,
            collage: s.collage,
            leads: s.leads,
            hero: s.hero.map { musicItem($0) },
            spotlight: s.spotlight.map { musicItem($0) },
            strip: s.strip.map { musicItem($0) },
            grid: s.grid.map { musicShelfRow($0) },
            rows: s.rows.map { musicShelfRow($0) },
            region: s.region,
            regions: s.regions,
            cold: s.cold,
            thin: s.thin,
            similar: s.similar.map { MusicSimilarInfo(key: $0.key, name: $0.name, target: musicTarget($0.target)) },
            kid: s.kid,
            partial: s.partial,
            more: s.more,
            loadingMore: s.loadingMore,
            canAdd: s.canAdd,
            canAsk: s.canAsk,
            isEmpty: s.isEmpty,
            isPage: s.isPage,
            bandHue: Int(s.bandHue)
        )
    }

    static func musicTile(_ t: MusicTile) -> MusicTileInfo {
        MusicTileInfo(key: t.key, name: t.name, target: musicTarget(t.target), color: t.color, hue: Int(t.hue), covers: t.covers)
    }

    static func musicNew(_ s: MusicNewState) -> MusicNewInfo {
        MusicNewInfo(
            load: load(s.status),
            kid: s.kid,
            building: s.building,
            hero: s.hero.map { musicItem($0) },
            rows: s.rows.map { musicShelfRow($0) },
            tiles: s.tiles.map { musicTile($0) },
            genres: s.genres,
            canAdd: s.canAdd,
            canAsk: s.canAsk,
            note: musicNote(s.note),
            isEmpty: s.isEmpty
        )
    }

    static func musicBrowse(_ s: MusicBrowseState) -> MusicBrowseInfo {
        MusicBrowseInfo(
            load: load(s.status),
            kid: s.kid,
            sections: s.sections.map { section in
                MusicTileSectionInfo(id: section.id, title: section.title, tiles: section.tiles.map { musicTile($0) })
            },
            tiles: s.tiles.map { musicTile($0) },
            isEmpty: s.isEmpty
        )
    }

    static func musicSearch(_ s: MusicSearchState) -> MusicSearchInfo {
        MusicSearchInfo(
            query: s.query,
            searching: s.searching,
            searched: s.searched,
            hits: s.hits.map { hit in
                MusicArtistHitInfo(
                    mbid: hit.mbid,
                    name: hit.name,
                    type: hit.type,
                    country: hit.country,
                    disambiguation: hit.disambiguation,
                    image: hit.image,
                    titleId: hit.titleId,
                    asked: hit.asked,
                    askedByMe: hit.askedByMe,
                    action: musicEnum(hit.action, MusicCardActionKind.none),
                    tooAction: hit.tooAction
                )
            },
            error: error(s.error)
        )
    }

    static func musicAdd(_ s: MusicAddState) -> MusicAddInfo {
        MusicAddInfo(
            load: load(s.status),
            mbid: s.mbid,
            name: s.name,
            disambiguation: s.disambiguation,
            folders: s.folders.map { MusicFolderInfo(id: $0.id, label: $0.label, path: $0.path, defaultRules: $0.defaultRules) },
            rules: s.rules.map { MusicRulesOptionInfo(id: $0.id, name: $0.name) },
            folderId: s.folderId,
            rulesId: s.rulesId,
            handsOff: s.handsOff,
            albumChoice: s.albumChoice,
            newAlbums: s.newAlbums,
            albumTypes: s.albumTypes,
            secondaryTypes: s.secondaryTypes,
            statuses: s.statuses,
            keep: s.keep,
            follow: s.follow,
            titleId: s.titleId,
            neverAdd: s.neverAdd,
            busy: s.busy,
            error: error(s.error),
            outcome: s.outcome.flatMap { MusicAddOutcomeKind(rawValue: key($0)) },
            addedTitleId: s.addedTitleId,
            noFolder: s.noFolder,
            canSubmit: s.canSubmit
        )
    }

    static func musicSettings(_ s: MusicSettingsState) -> MusicSettingsInfo {
        MusicSettingsInfo(
            load: load(s.status),
            albumChoice: s.form.albumChoice,
            newAlbums: s.form.newAlbums,
            albumTypes: s.form.albumTypes,
            secondaryTypes: s.form.secondaryTypes,
            statuses: s.form.releaseStatuses,
            pressingRule: s.form.pressingRule,
            url: s.form.musicbrainzUrl,
            hours: s.hours,
            canChange: s.canChange,
            saving: s.saving,
            error: error(s.error),
            justSaved: s.justSaved,
            dirty: s.dirty,
            emptyKinds: s.emptyKinds,
            emptyExtra: s.emptyExtra,
            emptyStatuses: s.emptyStatuses,
            hoursValid: s.hoursValid,
            canSave: s.canSave
        )
    }

    static func musicUnmatched(_ s: MusicUnmatchedState) -> MusicUnmatchedInfo {
        MusicUnmatchedInfo(
            total: Int(s.total),
            shown: s.shown.map { MusicUnmatchedArtistInfo(titleId: $0.titleId, name: $0.name, files: Int($0.files)) },
            more: Int(s.more)
        )
    }

    static func musicLibAlbum(_ a: MusicLibAlbum) -> MusicLibAlbumInfo {
        MusicLibAlbumInfo(
            id: a.id,
            mbid: a.mbid,
            name: a.name,
            year: a.year,
            date: a.date,
            group: musicEnum(a.group, MusicLibGroupID.other),
            tracks: Int(a.tracks),
            inLibrary: Int(a.inLibrary),
            missing: Int(a.missing),
            notOutYet: Int(a.notOutYet),
            included: a.included,
            keep: a.keep,
            dropped: a.dropped,
            state: musicEnum(a.state, MusicAlbumStateID.noTracks),
            future: a.future,
            cover: a.cover
        )
    }

    static func musicLibTrack(_ t: MusicLibTrack) -> MusicLibTrackInfo {
        MusicLibTrackInfo(
            albumId: t.albumId,
            id: t.id,
            disc: Int(t.disc),
            number: Int(t.number),
            name: t.name,
            length: t.length,
            slotId: t.slotId,
            stateLabel: t.stateLabel,
            tone: tone(t.tone),
            multiDisc: t.multiDisc,
            slots: t.slots.map { MusicSlotChipInfo(version: $0.version, label: $0.label, tone: tone($0.tone)) }
        )
    }

    static func musicLibFolder(_ f: MusicFolderOption) -> MusicFolderInfo {
        MusicFolderInfo(id: f.id, label: f.label, path: f.path, defaultRules: f.defaultRules)
    }

    static func musicLibraryArtist(_ s: MusicLibraryArtistState) -> MusicLibraryArtistInfo {
        MusicLibraryArtistInfo(
            load: load(s.status),
            titleId: s.titleId,
            mbid: s.mbid,
            name: s.name,
            image: s.image,
            disambiguation: s.disambiguation,
            canManage: s.canManage,
            canAsk: s.canAsk,
            handsOff: s.handsOff,
            keepUpToDate: s.keepUpToDate,
            folderPath: s.folderPath,
            folderId: s.folderId,
            sizeBytes: s.sizeBytes,
            rulesId: s.rulesId,
            rulesName: s.rulesName,
            gateReason: s.gateReason,
            noDownloader: s.noDownloader,
            refreshedAt: s.refreshedAt,
            nextRelease: s.nextRelease,
            albumCount: Int(s.albumCount),
            includedCount: Int(s.includedCount),
            haveTracks: Int(s.haveTracks),
            totalTracks: Int(s.totalTracks),
            missingTracks: Int(s.missingTracks),
            notOutYet: Int(s.notOutYet),
            fullReleases: Int(s.fullReleases),
            keptReleases: Int(s.keptReleases),
            newAlbums: s.newAlbums,
            albumTypes: s.albumTypes,
            secondaryTypes: s.secondaryTypes,
            statuses: s.statuses,
            preset: musicEnum(s.preset, MusicKindsPresetKind.custom),
            albums: s.albums.map { musicLibAlbum($0) },
            shown: s.shown.map { musicLibAlbum($0) },
            filter: musicEnum(s.filter, MusicLibFilterKind.included),
            leftOut: Int(s.leftOut),
            expanded: s.expanded.map { $0.int64Value },
            tracks: s.tracks.map { musicLibTrack($0) },
            loadingTracks: s.loadingTracks.map { $0.int64Value },
            busy: s.busy.map { $0.int64Value },
            quick: s.quick.map { MusicQuickRulesInfo(kind: musicEnum($0.kind, MusicQuickRuleKind.any), id: $0.id) },
            rules: s.rules.map { MusicRulesOptionInfo(id: $0.id, name: $0.name) },
            folders: s.folders.map { musicLibFolder($0) },
            unmatched: Int(s.unmatched),
            asks: s.asks.map { line in
                MusicAskLineInfo(
                    askedBy: line.askedBy,
                    askedAt: line.askedAt,
                    albums: line.albums,
                    decidedBy: line.decidedBy,
                    decidedAt: line.decidedAt,
                    stateLabel: line.stateLabel,
                    note: line.note
                )
            },
            working: s.working,
            note: musicNote(s.note),
            removed: s.removed,
            progress: s.progress,
            customQuality: s.customQuality,
            waitingForTracks: s.waitingForTracks
        )
    }

    static func musicLibraryAlbum(_ s: MusicLibraryAlbumState) -> MusicLibraryAlbumInfo {
        MusicLibraryAlbumInfo(
            load: load(s.status),
            albumId: s.albumId,
            cover: s.cover,
            titleId: s.titleId,
            artistName: s.artistName,
            artistImage: s.artistImage,
            artistMbid: s.artistMbid,
            name: s.name,
            kind: musicEnum(s.kind, MusicAlbumKindID.album),
            year: s.year,
            disambiguation: s.disambiguation,
            dropped: s.dropped,
            included: s.included,
            keep: s.keep,
            stateKey: musicEnum(s.stateKey, MusicAlbumStateID.noTracks),
            trackCount: Int(s.trackCount),
            inLibrary: Int(s.inLibrary),
            minutes: Int(s.minutes),
            tracks: s.tracks.map { musicLibTrack($0) },
            tracksWaiting: s.tracksWaiting,
            pressings: s.pressings.map { p in
                MusicPressingInfo(
                    id: p.id,
                    name: p.name,
                    disambiguation: p.disambiguation,
                    year: p.year,
                    country: p.country,
                    formats: p.formats,
                    trackCount: Int(p.trackCount),
                    chosen: p.chosen,
                    byPerson: p.byPerson
                )
            },
            byRule: s.byRule,
            moreBy: s.moreBy.map { musicLibAlbum($0) },
            canManage: s.canManage,
            hasFile: s.hasFile,
            notFound: s.notFound,
            busy: s.busy,
            note: musicNote(s.note),
            removed: s.removed,
            problem: musicEnum(s.problem, MusicProblemKind.none),
            complete: s.complete
        )
    }

    static func musicLeftover(_ r: MusicLeftoverRow) -> MusicLeftoverInfo {
        MusicLeftoverInfo(
            fileId: r.fileId,
            name: r.name,
            size: r.size,
            reason: r.reason,
            owner: r.owner,
            closest: r.closest,
            albumId: r.albumId,
            dismissed: r.dismissed
        )
    }

    static func musicLeftovers(_ s: MusicLeftoversState) -> MusicLeftoversInfo {
        MusicLeftoversInfo(
            load: load(s.status),
            open: s.open.map { musicLeftover($0) },
            alone: s.alone.map { musicLeftover($0) },
            pressings: s.pressings.map { h in
                MusicPressingHintInfo(
                    albumId: h.albumId,
                    albumName: h.albumName,
                    pressingId: h.pressingId,
                    pressingName: h.pressingName,
                    trackCount: Int(h.trackCount),
                    fits: Int(h.fits),
                    chosen: Int(h.chosen),
                    files: Int(h.files)
                )
            },
            match: s.match.map { m in
                MusicMatchInfo(
                    file: musicLeftover(m.file),
                    albums: m.albums.map { MusicMatchAlbumInfo(id: $0.id, name: $0.name) },
                    albumId: m.albumId,
                    tracks: m.tracks.map { MusicMatchTrackInfo(id: $0.id, disc: Int($0.disc), number: Int($0.number), name: $0.name, hasFile: $0.hasFile) },
                    loading: m.loading,
                    picked: m.picked,
                    replaces: m.replaces
                )
            },
            lastMatched: s.lastMatched.map { $0.int64Value },
            busy: s.busy,
            rescanning: s.rescanning,
            note: musicNote(s.note)
        )
    }

    static func kotlinMusicLibFilter(_ f: MusicLibFilterKind) -> MusicLibFilter {
        switch f {
        case .included: MusicLibFilter.included
        case .leftOut: MusicLibFilter.leftOut
        case .all: MusicLibFilter.all
        }
    }

    static func kotlinMusicPreset(_ p: MusicKindsPresetKind) -> MusicKindsPreset {
        switch p {
        case .albums: MusicKindsPreset.albums
        case .albumsEps: MusicKindsPreset.albumsEps
        case .albumsEpsSingles: MusicKindsPreset.albumsEpsSingles
        case .custom: MusicKindsPreset.custom
        }
    }
}
