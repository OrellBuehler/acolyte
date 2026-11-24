package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.AlbumChangeBody
import ch.orellbuehler.acolyte.api.model.DismissBody
import ch.orellbuehler.acolyte.api.model.MatchBody
import ch.orellbuehler.acolyte.api.model.MatchItem
import ch.orellbuehler.acolyte.api.model.UnmatchBody
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.title.followJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MusicLeftoverRow(
    val fileId: Long,
    val name: String,
    val size: Long,
    val reason: String,
    val owner: String,
    val closest: String,
    val albumId: Long,
    val dismissed: Boolean,
)

data class MusicPressingHintRow(
    val albumId: Long,
    val albumName: String,
    val pressingId: Long,
    val pressingName: String,
    val trackCount: Int,
    val fits: Int,
    val chosen: Int,
    val files: Int,
)

data class MusicMatchTrack(
    val id: Long,
    val disc: Int,
    val number: Int,
    val name: String,
    val hasFile: Boolean,
)

data class MusicMatchAlbum(
    val id: Long,
    val name: String,
)

data class MusicMatchState(
    val file: MusicLeftoverRow,
    val albums: List<MusicMatchAlbum>,
    val albumId: Long,
    val tracks: List<MusicMatchTrack> = emptyList(),
    val loading: Boolean = false,
    val picked: Long = 0,
) {
    val replaces: Boolean get() = tracks.any { it.id == picked && it.hasFile }
}

data class MusicLeftoversState(
    val status: LoadStatus = LoadStatus(),
    val open: List<MusicLeftoverRow> = emptyList(),
    val alone: List<MusicLeftoverRow> = emptyList(),
    val pressings: List<MusicPressingHintRow> = emptyList(),
    val match: MusicMatchState? = null,
    val lastMatched: List<Long> = emptyList(),
    val busy: Boolean = false,
    val rescanning: Boolean = false,
    val note: MusicNote? = null,
)

class MusicLeftoversPresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    private val onChanged: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(MusicLeftoversState())
    private val notes = MusicNotes()

    val state: StateFlow<MusicLeftoversState> = mutableState.asStateFlow()

    suspend fun load() {
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(TitleEndpoints.leftovers, "id" to titleId)) {
            is ApiResult.Ok -> {
                val rows =
                    result.value.leftovers.map { l ->
                        MusicLeftoverRow(
                            fileId = l.fileId,
                            name = l.fileName,
                            size = l.size,
                            reason = MusicRules.unmatchedReason(l.reason),
                            owner = l.music?.owner.orEmpty(),
                            closest = l.music?.albumName.orEmpty(),
                            albumId = l.music?.albumId ?: 0,
                            dismissed = l.dismissed,
                        )
                    }
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        open = rows.filter { r -> !r.dismissed },
                        alone = rows.filter { r -> r.dismissed },
                        pressings =
                            result.value.pressings.map { p ->
                                MusicPressingHintRow(
                                    p.albumId,
                                    p.albumName,
                                    p.pressingId,
                                    p.pressingName,
                                    p.trackCount,
                                    p.fits,
                                    p.chosen,
                                    p.files,
                                )
                            },
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }

    suspend fun dismiss(
        fileId: Long,
        dismissed: Boolean,
    ) {
        api
            .result(TitleEndpoints.dismiss, DismissBody(dismissed), "id" to titleId, "fid" to fileId)
            .onSuccess {
                mutableState.update { s ->
                    val all = (s.open + s.alone).map { if (it.fileId == fileId) it.copy(dismissed = dismissed) else it }
                    s.copy(open = all.filter { r -> !r.dismissed }, alone = all.filter { r -> r.dismissed })
                }
                onChanged()
            }.onFailure { e -> mutableState.update { it.copy(note = notes.failed(e)) } }
    }

    suspend fun openMatch(fileId: Long) {
        val s = mutableState.value
        val file = (s.open + s.alone).firstOrNull { it.fileId == fileId } ?: return
        val albums =
            api
                .result(MusicEndpoints.albums, "id" to titleId)
                .valueOrNull()
                ?.albums
                .orEmpty()
                .filter { it.included }
        val start = albums.firstOrNull { it.id == file.albumId }?.id ?: albums.firstOrNull()?.id ?: 0
        mutableState.update { it.copy(match = MusicMatchState(file, albums.map { a -> MusicMatchAlbum(a.id, a.name) }, start)) }
        if (start > 0) loadTracks(start)
    }

    fun closeMatch() = mutableState.update { it.copy(match = null) }

    suspend fun pickAlbum(albumId: Long) {
        mutableState.update { it.copy(match = it.match?.copy(albumId = albumId, tracks = emptyList(), picked = 0)) }
        loadTracks(albumId)
    }

    private suspend fun loadTracks(albumId: Long) {
        mutableState.update { it.copy(match = it.match?.copy(loading = true)) }
        val list =
            api
                .result(MusicEndpoints.albumTracks, "id" to albumId)
                .valueOrNull()
                ?.tracks
                .orEmpty()
        val rows =
            list.filter { !it.dropped }.map {
                MusicMatchTrack(
                    it.id,
                    it.disc,
                    it.number,
                    it.name,
                    it.slots.any { s ->
                        s.fileId > 0
                    },
                )
            }
        mutableState.update { s -> if (s.match?.albumId == albumId) s.copy(match = s.match.copy(tracks = rows, loading = false)) else s }
    }

    fun pickTrack(id: Long) = mutableState.update { it.copy(match = it.match?.copy(picked = id)) }

    suspend fun confirmMatch() {
        val m = mutableState.value.match ?: return
        if (m.picked == 0L) return
        mutableState.update { it.copy(busy = true) }
        when (
            val result =
                api.result(
                    TitleEndpoints.match,
                    MatchBody(listOf(MatchItem(m.file.fileId, listOf(m.picked))), m.replaces),
                    "id" to titleId,
                )
        ) {
            is ApiResult.Ok -> {
                val linked =
                    result.value.results
                        .filter { it.linked }
                        .map { it.fileId }
                val refused = result.value.results.size - linked.size
                mutableState.update {
                    it.copy(
                        match = null,
                        lastMatched = linked,
                        note =
                            if (linked.isNotEmpty()) {
                                notes.next(
                                    MusicNoteKind.MATCHED,
                                    count = linked.size,
                                )
                            } else {
                                notes.next(MusicNoteKind.NOT_MATCHED, count = refused)
                            },
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(note = notes.failed(result.error)) }
        }
        mutableState.update { it.copy(busy = false) }
        load()
        onChanged()
    }

    suspend fun undoMatch() {
        val ids = mutableState.value.lastMatched
        if (ids.isEmpty()) return
        api
            .result(TitleEndpoints.unmatch, UnmatchBody(ids), "id" to titleId)
            .onSuccess { v ->
                mutableState.update { it.copy(lastMatched = emptyList(), note = notes.next(MusicNoteKind.UNMATCHED, count = v.unmatched)) }
            }.onFailure { e -> mutableState.update { it.copy(note = notes.failed(e)) } }
        load()
        onChanged()
    }

    suspend fun usePressing(hint: MusicPressingHintRow) {
        mutableState.update { it.copy(rescanning = true) }
        when (val result = api.result(MusicEndpoints.changeAlbum, AlbumChangeBody(pressingId = hint.pressingId), "id" to hint.albumId)) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(note = notes.next(MusicNoteKind.PRESSING_CHANGED, hint.pressingName)) }
                api.result(TitleEndpoints.rescan, "id" to titleId).onSuccess { job -> followJob(api, job.jobId) }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(note = notes.next(MusicNoteKind.PRESSING_FAILED, result.error.message)) }
        }
        mutableState.update { it.copy(rescanning = false) }
        load()
        onChanged()
    }
}
