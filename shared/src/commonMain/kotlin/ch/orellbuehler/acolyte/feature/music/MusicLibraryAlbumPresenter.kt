package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.MusicPaths
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.AlbumChangeBody
import ch.orellbuehler.acolyte.api.model.MusicAlbumDetail
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MusicPressingRow(
    val id: Long,
    val name: String,
    val disambiguation: String,
    val year: String,
    val country: String,
    val formats: String,
    val trackCount: Int,
    val chosen: Boolean,
    val byPerson: Boolean,
)

data class MusicLibraryAlbumState(
    val status: LoadStatus = LoadStatus(),
    val albumId: Long = 0,
    val cover: String = "",
    val titleId: Long = 0,
    val artistName: String = "",
    val artistImage: String = "",
    val artistMbid: String = "",
    val name: String = "",
    val kind: MusicAlbumKind = MusicAlbumKind.ALBUM,
    val year: String = "",
    val disambiguation: String = "",
    val dropped: Boolean = false,
    val included: Boolean = false,
    val keep: Boolean = false,
    val stateKey: MusicAlbumStateKey = MusicAlbumStateKey.NO_TRACKS,
    val trackCount: Int = 0,
    val inLibrary: Int = 0,
    val minutes: Int = 0,
    val tracks: List<MusicLibTrack> = emptyList(),
    val tracksWaiting: Boolean = true,
    val pressings: List<MusicPressingRow> = emptyList(),
    val byRule: Boolean = true,
    val moreBy: List<MusicLibAlbum> = emptyList(),
    val canManage: Boolean = false,
    val hasFile: Boolean = false,
    val notFound: Boolean = false,
    val busy: Boolean = false,
    val note: MusicNote? = null,
    val removed: Boolean = false,
) {
    val problem: MusicProblem get() = if (status.phase == Phase.FAILED) musicProblem(status.error) else MusicProblem.NONE

    val complete: Boolean get() = trackCount > 0 && inLibrary >= trackCount
}

class MusicLibraryAlbumPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val titleId: Long,
    private val albumId: Long,
    private val today: () -> String,
) {
    private val mutableState =
        MutableStateFlow(MusicLibraryAlbumState(albumId = albumId, cover = MusicPaths.cover(albumId), titleId = titleId))
    private val notes = MusicNotes()

    val state: StateFlow<MusicLibraryAlbumState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(MusicEndpoints.libraryAlbum, "id" to albumId)) {
            is ApiResult.Failed -> {
                val missing = result.error.status == NOT_FOUND
                mutableState.update {
                    it.copy(
                        status = if (missing) LoadStatus.ready else it.status.failed(result.error),
                        notFound = missing,
                    )
                }
            }
            is ApiResult.Ok -> {
                apply(result.value)
                mutableState.update { it.copy(status = LoadStatus.ready) }
                loadTracks()
                loadTitle()
            }
        }
    }

    private suspend fun loadTitle() {
        val id = mutableState.value.titleId
        if (id <= 0) return
        val title = api.result(LibraryEndpoints.titleVersions, "id" to id).valueOrNull()
        val list =
            api
                .result(MusicEndpoints.albums, "id" to id)
                .valueOrNull()
                ?.albums
                .orEmpty()
        val day = today()
        mutableState.update { s ->
            s.copy(
                artistName = title?.name ?: s.artistName,
                artistImage = title?.artwork?.firstOrNull() ?: s.artistImage,
                artistMbid = title?.artist?.mbid ?: s.artistMbid,
                moreBy = list.filter { it.id != albumId }.map { it.toLib(day) }.take(MORE),
            )
        }
    }

    private fun apply(detail: MusicAlbumDetail) {
        val a = detail.album
        val me = account()
        mutableState.update { s ->
            s.copy(
                titleId = detail.titleId.takeIf { it > 0 } ?: s.titleId,
                name = a.name,
                kind = MusicRules.albumKind(a.type, a.secondaryTypes),
                year = MusicRules.year(a.releaseDate),
                disambiguation = a.disambiguation,
                dropped = a.dropped,
                included = a.included,
                keep = a.keepUpToDate,
                stateKey = musicAlbumState(a.state, MusicRules.isFuture(a.releaseDate, today())),
                trackCount = a.tracks,
                inLibrary = a.inLibrary,
                pressings =
                    detail.pressings.map { p ->
                        MusicPressingRow(
                            p.id,
                            p.name,
                            p.disambiguation,
                            MusicRules.year(p.date),
                            p.country,
                            p.formats.joinToString(" + "),
                            p.trackCount,
                            p.chosen,
                            p.chosen && p.chosenBy == "person",
                        )
                    },
                byRule = detail.pressings.none { it.chosen && it.chosenBy == "person" },
                canManage = me?.allows(Need.MANAGE_LIBRARY, Part.MUSIC) == true,
            )
        }
    }

    private suspend fun loadTracks() {
        var tries = 0
        while (true) {
            val list =
                api
                    .result(MusicEndpoints.albumTracks, "id" to albumId)
                    .valueOrNull()
                    ?.tracks
                    .orEmpty()
            val rows = list.toLib(albumId)
            mutableState.update { s ->
                s.copy(
                    tracks = rows,
                    minutes = MusicRules.minutes(list.filter { !it.dropped }.sumOf { it.lengthMs }),
                    hasFile = list.any { t -> t.slots.any { it.fileId > 0 } },
                )
            }
            val s = mutableState.value
            if (rows.isNotEmpty() || !s.included || tries >= PAUSES.size) break
            delay(PAUSES[tries++])
        }
        mutableState.update { it.copy(tracksWaiting = false) }
    }

    suspend fun toggleKeep() {
        val s = mutableState.value
        if (!s.included || s.busy) return
        mutableState.update { it.copy(busy = true) }
        when (val result = api.result(MusicEndpoints.changeAlbum, AlbumChangeBody(keepUpToDate = !s.keep), "id" to albumId)) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(
                        busy = false,
                        note = notes.next(if (s.keep) MusicNoteKind.KEEP_ALBUM_OFF else MusicNoteKind.KEEP_ALBUM_ON, s.name),
                    )
                }
                reloadDetail()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, note = notes.failed(result.error)) }
        }
    }

    suspend fun changePressing(id: Long) {
        val s = mutableState.value
        if (s.busy) return
        mutableState.update { it.copy(busy = true) }
        when (val result = api.result(MusicEndpoints.changeAlbum, AlbumChangeBody(pressingId = id), "id" to albumId)) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(
                        busy = false,
                        note =
                            notes.next(
                                MusicNoteKind.PRESSING_CHANGED,
                                s.pressings
                                    .firstOrNull { p ->
                                        p.id ==
                                            id
                                    }?.name
                                    .orEmpty(),
                            ),
                    )
                }
                reloadDetail()
                loadTracks()
            }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(
                        busy = false,
                        note = notes.next(MusicNoteKind.PRESSING_FAILED, result.error.message),
                    )
                }
        }
    }

    suspend fun searchNow() {
        val s = mutableState.value
        if (s.titleId <= 0) return
        when (val result = api.result(LibraryEndpoints.search, SearchBody(albumId = albumId), "id" to s.titleId)) {
            is ApiResult.Ok -> mutableState.update { it.copy(note = notes.next(MusicNoteKind.SEARCHING, s.name)) }
            is ApiResult.Failed -> mutableState.update { it.copy(note = notes.failed(result.error)) }
        }
    }

    suspend fun reloadDetail() {
        when (val result = api.result(MusicEndpoints.libraryAlbum, "id" to albumId)) {
            is ApiResult.Ok -> apply(result.value)
            is ApiResult.Failed -> Unit
        }
    }

    fun removed() = mutableState.update { it.copy(removed = true) }

    private companion object {
        const val NOT_FOUND = 404
        const val MORE = 18
        val PAUSES = listOf(1_000L, 1_500L, 2_000L, 2_000L, 2_500L, 3_000L, 4_000L, 5_000L, 6_000L, 8_000L, 8_000L, 8_000L, 8_000L)
    }
}
