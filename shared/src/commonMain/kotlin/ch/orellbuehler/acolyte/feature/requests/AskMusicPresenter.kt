package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.model.TitleAsk
import ch.orellbuehler.acolyte.api.model.NewAsk
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class MusicMode {
    EVERY,
    SOME,
}

data class AskAlbumRow(
    val mbid: String,
    val name: String,
    val year: Int,
    val kind: AlbumKind,
    val kept: Boolean,
    val leftOut: Boolean,
    val ticked: Boolean,
) {
    val available: Boolean get() = !kept && !leftOut
}

data class AskMusicState(
    val status: LoadStatus = LoadStatus(),
    val artist: String = "",
    val name: String = "",
    val albums: List<AskAlbumRow> = emptyList(),
    val mode: MusicMode = MusicMode.EVERY,
    val follow: Boolean = true,
    val brief: TitleAsk? = null,
    val busy: Boolean = false,
    val error: ApiError? = null,
    val outcome: AskOutcome? = null,
) {
    val failure: AskFailure? get() = error?.let { askFailure(it) }

    val picked: List<AskAlbumRow> get() = albums.filter { it.ticked && it.available }

    val someAllowed: Boolean get() = albums.any { it.available }

    val joining: Boolean get() = brief?.mine == false

    val canSubmit: Boolean
        get() = status.phase == Phase.READY && !busy && outcome == null && (mode == MusicMode.EVERY || picked.isNotEmpty())
}

class AskMusicPresenter internal constructor(
    private val api: Api,
    private val inbox: RequestsInbox,
    artist: String,
    name: String,
    private val album: String = "",
    private val edition: String = "",
) {
    private val mutableState = MutableStateFlow(AskMusicState(artist = artist, name = name))
    private val lock = Mutex()

    val state: StateFlow<AskMusicState> = mutableState.asStateFlow()

    suspend fun load() {
        lock.withLock {
            if (mutableState.value.status.phase == Phase.READY) return
            mutableState.update { it.copy(status = it.status.started()) }
            val mbid = mutableState.value.artist
            when (val result = api.result(LibraryEndpoints.lookupArtists, "term" to "mbid:$mbid", "albums" to "true")) {
                is ApiResult.Ok -> {
                    val found = result.value.artists.firstOrNull { it.mbid == mbid } ?: result.value.artists.firstOrNull()
                    if (found == null) {
                        mutableState.update { it.copy(status = it.status.failed(ApiError(ApiErrorCode.NOT_FOUND, 404))) }
                        return
                    }
                    val rows =
                        found.askAlbums.map {
                            AskAlbumRow(
                                mbid = it.mbid,
                                name = it.name,
                                year = yearOf(it.releaseDate),
                                kind = albumKind(it.type, it.secondaryTypes, it.name),
                                kept = it.kept,
                                leftOut = it.leftOut,
                                ticked = it.mbid == album,
                            )
                        }
                    val some = album.isNotEmpty() && rows.any { it.mbid == album && it.available }
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            name = found.name.ifEmpty { it.name },
                            albums = rows,
                            brief = found.ask,
                            mode = if (some) MusicMode.SOME else MusicMode.EVERY,
                        )
                    }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    fun setMode(mode: MusicMode) =
        mutableState.update { s ->
            if (mode == MusicMode.SOME &&
                !s.someAllowed
            ) {
                s
            } else {
                s.copy(mode = mode, error = null)
            }
        }

    fun setFollow(on: Boolean) = mutableState.update { it.copy(follow = on) }

    fun toggle(mbid: String) =
        mutableState.update { s ->
            s.copy(albums = s.albums.map { if (it.mbid == mbid && it.available) it.copy(ticked = !it.ticked) else it }, error = null)
        }

    suspend fun submit() {
        val s = mutableState.value
        if (!s.canSubmit) return
        mutableState.update { it.copy(busy = true, error = null) }
        val albums = if (s.mode == MusicMode.EVERY) emptyList() else s.picked.map { it.mbid }
        val editions = if (edition.isNotEmpty() && album in albums) mapOf(album to edition) else null
        val body = NewAsk(id = "mbid:${s.artist}", kind = "artist", albums = albums, editions = editions, follow = s.follow)
        when (val result = api.result(RequestsEndpoints.make, body)) {
            is ApiResult.Ok -> {
                inbox.refresh()
                val answer = result.value
                val outcome =
                    when {
                        answer.ask.state == APPROVED -> AskOutcome.APPROVED
                        answer.ask.state == TURNED_DOWN -> AskOutcome.TURNED_DOWN
                        answer.joined -> AskOutcome.JOINED
                        answer.already -> AskOutcome.ALREADY
                        else -> AskOutcome.REQUESTED
                    }
                mutableState.update { it.copy(busy = false, outcome = outcome) }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }
}
