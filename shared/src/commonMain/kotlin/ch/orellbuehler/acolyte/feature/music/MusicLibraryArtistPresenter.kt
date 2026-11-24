package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.MusicPaths
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.AlbumChangeBody
import ch.orellbuehler.acolyte.api.model.ArtistChoicesBody
import ch.orellbuehler.acolyte.api.model.MusicAlbumSummary
import ch.orellbuehler.acolyte.api.model.OwnedTitle
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.TitleChange
import ch.orellbuehler.acolyte.api.model.TitleMove
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

enum class MusicAlbumStateKey {
    NOT_INCLUDED,
    NO_TRACKS,
    COMPLETE,
    NOT_OUT_YET,
    NOT_KEPT,
    PARTIAL,
    MISSING,
}

enum class MusicLibGroup {
    ALBUMS,
    EPS,
    SINGLES,
    LIVE,
    COMPILATIONS,
    OTHER,
}

enum class MusicLibFilter {
    INCLUDED,
    LEFT_OUT,
    ALL,
}

enum class MusicKindsPreset {
    ALBUMS,
    ALBUMS_EPS,
    ALBUMS_EPS_SINGLES,
    CUSTOM,
}

enum class MusicQuickRule {
    ANY,
    LOSSY,
    LOSSLESS,
}

data class MusicQuickRules(
    val kind: MusicQuickRule,
    val id: Long,
)

data class MusicLibAlbum(
    val id: Long,
    val mbid: String,
    val name: String,
    val year: String,
    val date: String,
    val group: MusicLibGroup,
    val tracks: Int,
    val inLibrary: Int,
    val missing: Int,
    val notOutYet: Int,
    val included: Boolean,
    val keep: Boolean,
    val dropped: Boolean,
    val state: MusicAlbumStateKey,
    val future: Boolean,
    val cover: String,
)

data class MusicLibTrack(
    val albumId: Long,
    val id: Long,
    val disc: Int,
    val number: Int,
    val name: String,
    val length: String,
    val slotId: Long,
    val stateLabel: String,
    val tone: Tone,
    val multiDisc: Boolean,
    val slots: List<MusicSlotChip>,
)

data class MusicSlotChip(
    val version: String,
    val label: String,
    val tone: Tone,
)

data class MusicAskLine(
    val askedBy: String,
    val askedAt: String,
    val albums: String,
    val decidedBy: String,
    val decidedAt: String,
    val stateLabel: String,
    val note: String,
)

data class MusicLibraryArtistState(
    val status: LoadStatus = LoadStatus(),
    val titleId: Long = 0,
    val mbid: String = "",
    val name: String = "",
    val image: String = "",
    val disambiguation: String = "",
    val canManage: Boolean = false,
    val canAsk: Boolean = false,
    val handsOff: Boolean = false,
    val keepUpToDate: Boolean = false,
    val folderPath: String = "",
    val folderId: Long = 0,
    val sizeBytes: Long = 0,
    val rulesId: Long = 0,
    val rulesName: String = "",
    val gateReason: String = "",
    val noDownloader: String = "",
    val refreshedAt: String = "",
    val nextRelease: String = "",
    val albumCount: Int = 0,
    val includedCount: Int = 0,
    val haveTracks: Int = 0,
    val totalTracks: Int = 0,
    val missingTracks: Int = 0,
    val notOutYet: Int = 0,
    val fullReleases: Int = 0,
    val keptReleases: Int = 0,
    val newAlbums: String = "all",
    val albumTypes: List<String> = emptyList(),
    val secondaryTypes: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val preset: MusicKindsPreset = MusicKindsPreset.CUSTOM,
    val albums: List<MusicLibAlbum> = emptyList(),
    val filter: MusicLibFilter = MusicLibFilter.INCLUDED,
    val leftOut: Int = 0,
    val expanded: List<Long> = emptyList(),
    val tracks: List<MusicLibTrack> = emptyList(),
    val loadingTracks: List<Long> = emptyList(),
    val busy: List<Long> = emptyList(),
    val quick: List<MusicQuickRules> = emptyList(),
    val rules: List<MusicRulesOption> = emptyList(),
    val folders: List<MusicFolderOption> = emptyList(),
    val unmatched: Int = 0,
    val asks: List<MusicAskLine> = emptyList(),
    val working: Boolean = false,
    val note: MusicNote? = null,
    val removed: Boolean = false,
) {
    val shown: List<MusicLibAlbum>
        get() =
            when (filter) {
                MusicLibFilter.INCLUDED -> albums.filter { it.included }
                MusicLibFilter.LEFT_OUT -> albums.filter { !it.included }
                MusicLibFilter.ALL -> albums
            }

    fun tracksOf(albumId: Long): List<MusicLibTrack> = tracks.filter { it.albumId == albumId }

    val progress: Float get() = if (totalTracks > 0) (haveTracks.toFloat() / totalTracks).coerceIn(0f, 1f) else 0f

    val customQuality: Boolean get() = quick.none { it.id == rulesId }

    val problem: MusicProblem get() = if (status.phase == Phase.FAILED) musicProblem(status.error) else MusicProblem.NONE

    val waitingForTracks: Boolean get() = includedCount > 0 && totalTracks == 0
}

internal fun musicGroup(
    type: String,
    secondary: List<String>,
): MusicLibGroup {
    val sec = secondary.map { it.lowercase() }.filter { it != "studio" }
    return when {
        "live" in sec -> MusicLibGroup.LIVE
        "compilation" in sec -> MusicLibGroup.COMPILATIONS
        else ->
            when (type.lowercase()) {
                "album" -> if (sec.isEmpty()) MusicLibGroup.ALBUMS else MusicLibGroup.OTHER
                "ep" -> MusicLibGroup.EPS
                "single" -> MusicLibGroup.SINGLES
                else -> MusicLibGroup.OTHER
            }
    }
}

internal fun musicAlbumState(
    value: String,
    future: Boolean,
): MusicAlbumStateKey =
    when (value) {
        "not_included" -> MusicAlbumStateKey.NOT_INCLUDED
        "no_tracks" -> if (future) MusicAlbumStateKey.NOT_OUT_YET else MusicAlbumStateKey.NO_TRACKS
        "complete" -> MusicAlbumStateKey.COMPLETE
        "not_out_yet" -> MusicAlbumStateKey.NOT_OUT_YET
        "not_kept" -> MusicAlbumStateKey.NOT_KEPT
        "partial" -> MusicAlbumStateKey.PARTIAL
        "missing" -> MusicAlbumStateKey.MISSING
        else -> MusicAlbumStateKey.NO_TRACKS
    }

internal fun musicSlotTone(state: String): Tone =
    when (state) {
        "in_library" -> Tone.OK
        "downloading", "importing", "chosen" -> Tone.INFO
        "searching", "held", "wanted", "not_out_yet", "better_wanted" -> Tone.WAIT
        "failed", "stalled", "rejected", "needs_a_decision", "missing" -> Tone.FAIL
        else -> Tone.IDLE
    }

internal fun MusicAlbumSummary.toLib(today: String): MusicLibAlbum {
    val future = MusicRules.isFuture(releaseDate, today)
    return MusicLibAlbum(
        id = id,
        mbid = mbid,
        name = name,
        year = MusicRules.year(releaseDate),
        date = releaseDate,
        group = musicGroup(type, secondaryTypes),
        tracks = tracks,
        inLibrary = inLibrary,
        missing = missing,
        notOutYet = notOutYet,
        included = included,
        keep = keepUpToDate,
        dropped = dropped,
        state = musicAlbumState(state, future),
        future = future,
        cover = MusicPaths.cover(id),
    )
}

internal fun kindsPreset(
    types: List<String>,
    secondary: List<String>,
    statuses: List<String>,
): MusicKindsPreset {
    if (secondary != listOf("studio") || statuses != listOf("official")) return MusicKindsPreset.CUSTOM
    return when (types) {
        listOf("album") -> MusicKindsPreset.ALBUMS
        listOf("album", "ep") -> MusicKindsPreset.ALBUMS_EPS
        listOf("album", "ep", "single") -> MusicKindsPreset.ALBUMS_EPS_SINGLES
        else -> MusicKindsPreset.CUSTOM
    }
}

class MusicLibraryArtistPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val titleId: Long,
    private val today: () -> String,
) {
    private val mutableState = MutableStateFlow(MusicLibraryArtistState(titleId = titleId))
    private val notes = MusicNotes()
    private var title = OwnedTitle()
    private var polls = 0

    val state: StateFlow<MusicLibraryArtistState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started()) }
        reload(first = true)
    }

    private suspend fun reload(first: Boolean = false) {
        val me = account()
        val manage = me?.allows(Need.MANAGE_LIBRARY, Part.MUSIC) == true
        coroutineScope {
            val t = async { api.result(LibraryEndpoints.titleVersions, "id" to titleId) }
            val a = async { api.result(MusicEndpoints.albums, "id" to titleId) }
            val rules = async { if (manage) api.result(LibraryEndpoints.pickRules).valueOrNull() else null }
            val folders = async { if (manage) api.result(LibraryEndpoints.folders).valueOrNull() else null }
            val unmatched = async { if (manage) api.result(MusicEndpoints.unmatched).valueOrNull() else null }
            when (val result = t.await()) {
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(status = it.status.failed(result.error)) }
                    return@coroutineScope
                }
                is ApiResult.Ok -> title = result.value
            }
            val list = a.await().valueOrNull()?.albums
            val sets =
                rules
                    .await()
                    ?.pickRules
                    .orEmpty()
                    .filter { "artist" in it.kinds || "music" in it.kinds }
            val music =
                folders
                    .await()
                    ?.libraryFolders
                    .orEmpty()
                    .filter { it.kind == "music" }
            val files =
                unmatched
                    .await()
                    ?.artists
                    ?.firstOrNull { it.titleId == titleId }
                    ?.files ?: 0
            publish(me, manage, list, sets, music, files, first)
            mutableState.update { it.copy(status = LoadStatus.ready) }
        }
    }

    private fun publish(
        me: Account?,
        manage: Boolean,
        list: List<MusicAlbumSummary>?,
        sets: List<ch.orellbuehler.acolyte.api.model.PickRule>,
        folders: List<ch.orellbuehler.acolyte.api.model.LibraryFolder>,
        files: Int,
        first: Boolean,
    ) {
        val day = today()
        val artist = title.artist
        val albums =
            list
                ?.map {
                    it.toLib(
                        day,
                    )
                }?.sortedWith(compareByDescending<MusicLibAlbum> { it.date.isNotEmpty() }.thenByDescending { it.date })
                ?: mutableState.value.albums
        val inc = albums.filter { it.included }
        val quick =
            sets.mapNotNull { r ->
                when (r.name) {
                    "Any music" -> MusicQuickRules(MusicQuickRule.ANY, r.id)
                    "Lossy 320 or V0" -> MusicQuickRules(MusicQuickRule.LOSSY, r.id)
                    "Lossless first" -> MusicQuickRules(MusicQuickRule.LOSSLESS, r.id)
                    else -> null
                }
            }
        mutableState.update { s ->
            s.copy(
                mbid = artist?.mbid.orEmpty(),
                name = title.name,
                image = title.artwork.firstOrNull().orEmpty(),
                disambiguation = artist?.disambiguation.orEmpty(),
                canManage = manage,
                canAsk = me?.allows(Need.ASK, Part.WISHLIST) == true && !manage,
                handsOff = me?.handsOff == true,
                keepUpToDate = title.keepUpToDate,
                folderPath = title.folderPath,
                folderId = title.libraryFolderId,
                sizeBytes = title.sizeBytes,
                rulesId = title.pickRulesId,
                rulesName = title.pickRulesName,
                gateReason = title.gateReason,
                noDownloader = title.noDownloader,
                refreshedAt = title.refreshedAt,
                nextRelease = artist?.nextRelease.orEmpty(),
                albumCount = artist?.albums ?: albums.size,
                includedCount = artist?.included ?: inc.size,
                haveTracks = inc.sumOf { it.inLibrary },
                totalTracks = inc.sumOf { it.tracks },
                missingTracks = artist?.missing ?: inc.sumOf { it.missing },
                notOutYet = artist?.notOutYet ?: inc.sumOf { it.notOutYet },
                fullReleases = inc.count { it.state == MusicAlbumStateKey.COMPLETE },
                keptReleases = inc.count { it.keep },
                newAlbums = artist?.newAlbums.orEmpty().ifEmpty { "all" },
                albumTypes = artist?.albumTypes.orEmpty(),
                secondaryTypes = artist?.secondaryTypes.orEmpty(),
                statuses = artist?.releaseStatuses.orEmpty(),
                preset = kindsPreset(artist?.albumTypes.orEmpty(), artist?.secondaryTypes.orEmpty(), artist?.releaseStatuses.orEmpty()),
                albums = albums,
                leftOut = albums.count { !it.included },
                filter = if (first && albums.none { it.included }) MusicLibFilter.ALL else s.filter,
                quick = quick,
                rules = sets.map { MusicRulesOption(it.id, it.name) },
                folders = folders.map { MusicFolderOption(it.id, it.shown, it.path, it.defaultPickRulesId ?: 0) },
                unmatched = if (manage) files else 0,
                asks =
                    title.asks.map { ask ->
                        MusicAskLine(
                            askedBy = ask.people.joinToString(", ") { it.shown }.ifEmpty { ask.askedBy.shown },
                            askedAt = ask.askedAt,
                            albums = ask.albumWords,
                            decidedBy = ask.decidedBy?.shown.orEmpty(),
                            decidedAt = ask.decidedAt,
                            stateLabel = ask.stateLabel,
                            note = ask.note,
                        )
                    },
            )
        }
    }

    fun setFilter(filter: MusicLibFilter) = mutableState.update { it.copy(filter = filter) }

    fun collapseAll() = mutableState.update { it.copy(expanded = emptyList()) }

    suspend fun expand(albumId: Long) {
        val s = mutableState.value
        if (albumId in s.expanded) {
            mutableState.update { it.copy(expanded = it.expanded - albumId) }
            return
        }
        mutableState.update { it.copy(expanded = it.expanded + albumId) }
        if (s.tracks.none { it.albumId == albumId }) loadTracks(albumId)
    }

    suspend fun expandAll() {
        val ids = mutableState.value.shown.map { it.id }
        mutableState.update { it.copy(expanded = ids) }
        ids.filter { id -> mutableState.value.tracks.none { it.albumId == id } }.forEach { loadTracks(it) }
    }

    private suspend fun loadTracks(albumId: Long) {
        mutableState.update { it.copy(loadingTracks = it.loadingTracks + albumId) }
        val list = api.result(MusicEndpoints.albumTracks, "id" to albumId).valueOrNull()
        mutableState.update { s ->
            s.copy(
                loadingTracks = s.loadingTracks - albumId,
                tracks = if (list != null) s.tracks.filter { it.albumId != albumId } + list.tracks.toLib(albumId) else s.tracks,
            )
        }
    }

    suspend fun toggleKeepAlbum(albumId: Long) {
        val album = mutableState.value.albums.firstOrNull { it.id == albumId } ?: return
        if (!album.included || albumId in mutableState.value.busy) return
        mutableState.update { it.copy(busy = it.busy + albumId) }
        when (val result = api.result(MusicEndpoints.changeAlbum, AlbumChangeBody(keepUpToDate = !album.keep), "id" to albumId)) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(
                        busy = it.busy - albumId,
                        note = notes.next(if (album.keep) MusicNoteKind.KEEP_ALBUM_OFF else MusicNoteKind.KEEP_ALBUM_ON, album.name),
                        tracks = it.tracks.filter { t -> t.albumId != albumId },
                    )
                }
                reload()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = it.busy - albumId, note = notes.failed(result.error)) }
        }
    }

    suspend fun setKeepArtist(on: Boolean) {
        mutableState.update { it.copy(keepUpToDate = on) }
        when (val result = api.result(LibraryEndpoints.change, TitleChange(keepUpToDate = on), "id" to titleId)) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(
                        note = notes.next(if (on) MusicNoteKind.KEEP_ARTIST_ON else MusicNoteKind.KEEP_ARTIST_OFF, it.name),
                    )
                }
                reload()
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(keepUpToDate = !on, note = notes.failed(result.error)) }
            }
        }
    }

    suspend fun setRules(id: Long) {
        when (val result = api.result(LibraryEndpoints.change, TitleChange(pickRulesId = id), "id" to titleId)) {
            is ApiResult.Ok -> reload()
            is ApiResult.Failed -> mutableState.update { it.copy(note = notes.failed(result.error)) }
        }
    }

    suspend fun searchAll() = search(SearchBody(), MusicNoteKind.SEARCHING_ALL, "")

    suspend fun searchAlbum(albumId: Long) =
        search(
            SearchBody(albumId = albumId),
            MusicNoteKind.SEARCHING,
            mutableState.value.albums
                .firstOrNull {
                    it.id ==
                        albumId
                }?.name
                .orEmpty(),
        )

    suspend fun searchTrack(
        albumId: Long,
        slotId: Long,
        name: String,
    ) = search(SearchBody(slotId = slotId), MusicNoteKind.SEARCHING, name)

    private suspend fun search(
        body: SearchBody,
        kind: MusicNoteKind,
        text: String,
    ) {
        when (val result = api.result(LibraryEndpoints.search, body, "id" to titleId)) {
            is ApiResult.Ok -> mutableState.update { it.copy(note = notes.next(kind, text)) }
            is ApiResult.Failed -> mutableState.update { it.copy(note = notes.failed(result.error)) }
        }
    }

    suspend fun applyPreset(preset: MusicKindsPreset) {
        val types =
            when (preset) {
                MusicKindsPreset.ALBUMS -> listOf("album")
                MusicKindsPreset.ALBUMS_EPS -> listOf("album", "ep")
                MusicKindsPreset.ALBUMS_EPS_SINGLES -> listOf("album", "ep", "single")
                MusicKindsPreset.CUSTOM -> return
            }
        saveKinds(mutableState.value.newAlbums, types, listOf("studio"), listOf("official"))
    }

    suspend fun saveKinds(
        newAlbums: String,
        types: List<String>,
        secondary: List<String>,
        statuses: List<String>,
    ): Boolean {
        if (types.isEmpty() || secondary.isEmpty() || statuses.isEmpty()) return false
        mutableState.update { it.copy(working = true) }
        val body = ArtistChoicesBody(newAlbums, types, secondary, statuses)
        return when (val result = api.result(MusicEndpoints.artistChoices, body, "id" to titleId)) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(working = false, note = notes.next(MusicNoteKind.KINDS_SAVED), tracks = emptyList()) }
                reload()
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(working = false, note = notes.failed(result.error)) }
                false
            }
        }
    }

    suspend fun moveTo(folderId: Long) {
        val label =
            mutableState.value.folders
                .firstOrNull { it.id == folderId }
                ?.label
                .orEmpty()
        when (val result = api.result(LibraryEndpoints.move, TitleMove(folderId, true), "id" to titleId)) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(note = notes.next(MusicNoteKind.MOVING, label)) }
                reload()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(note = notes.failed(result.error)) }
        }
    }

    suspend fun refreshDetails() {
        when (val result = api.result(TitleEndpoints.refresh, "id" to titleId)) {
            is ApiResult.Ok -> mutableState.update { it.copy(note = notes.next(MusicNoteKind.REFRESHING, it.name)) }
            is ApiResult.Failed -> mutableState.update { it.copy(note = notes.failed(result.error)) }
        }
    }

    suspend fun reloadQuiet() {
        if (mutableState.value.status.phase == Phase.READY) reload()
    }

    fun removed() {
        mutableState.update { it.copy(removed = true) }
    }

    suspend fun watch() =
        coroutineScope {
            val pokes = Channel<Unit>(Channel.CONFLATED)
            launch {
                for (poke in pokes) {
                    delay(DEBOUNCE)
                    reload()
                }
            }
            launch {
                while (polls < POLLS) {
                    delay(POLL_MILLIS)
                    if (mutableState.value.waitingForTracks) {
                        polls++
                        pokes.trySend(Unit)
                    }
                }
            }
            api.live.collect { event -> if (concerns(event)) pokes.trySend(Unit) }
        }

    private fun concerns(event: LiveEvent): Boolean {
        val data = event.data as? JsonObject
        val id = listOf("TitleID", "title_id", "id").firstNotNullOfOrNull { (data?.get(it) as? JsonPrimitive)?.longOrNull }
        return when (event.topic) {
            "event", "slot" -> id == null || id == titleId
            else -> false
        }
    }

    private companion object {
        const val DEBOUNCE = 400L
        const val POLL_MILLIS = 3_000L
        const val POLLS = 30
    }
}

internal fun List<ch.orellbuehler.acolyte.api.model.MusicTrackView>.toLib(albumId: Long): List<MusicLibTrack> {
    val multi = map { it.disc }.distinct().size > 1
    return filter { !it.dropped }.map { t ->
        val slots = t.slots
        val first = slots.firstOrNull()
        MusicLibTrack(
            albumId = albumId,
            id = t.id,
            disc = t.disc,
            number = t.number,
            name = t.name,
            length = MusicRules.lengthText(t.lengthMs),
            slotId = first?.id ?: 0,
            stateLabel = first?.stateLabel.orEmpty(),
            tone = musicSlotTone(first?.state.orEmpty()),
            multiDisc = multi,
            slots = slots.map { MusicSlotChip(if (slots.size > 1) it.version else "", it.stateLabel, musicSlotTone(it.state)) },
        )
    }
}
