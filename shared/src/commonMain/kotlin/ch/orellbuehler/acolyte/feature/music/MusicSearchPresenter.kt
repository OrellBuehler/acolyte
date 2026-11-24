package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MusicArtistHit(
    val mbid: String,
    val name: String,
    val type: String,
    val country: String,
    val disambiguation: String,
    val image: String,
    val titleId: Long,
    val asked: Boolean,
    val askedByMe: Boolean,
    val action: MusicCardAction,
    val tooAction: Boolean,
)

data class MusicSearchState(
    val query: String = "",
    val searching: Boolean = false,
    val searched: String = "",
    val hits: List<MusicArtistHit> = emptyList(),
    val error: ApiError? = null,
)

class MusicSearchPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(MusicSearchState())
    private var run = 0

    val state: StateFlow<MusicSearchState> = mutableState.asStateFlow()

    suspend fun search(text: String) {
        val term = text.trim().take(MAX)
        val ticket = ++run
        if (term.isEmpty()) {
            mutableState.value = MusicSearchState()
            return
        }
        mutableState.update { it.copy(query = term, searching = true, error = null) }
        var tries = 0
        while (true) {
            val result = api.result(LibraryEndpoints.lookupArtists, "term" to term)
            if (ticket != run) return
            when (result) {
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(searching = false, error = result.error, hits = emptyList(), searched = term) }
                    return
                }
                is ApiResult.Ok -> {
                    val access = MusicAccess.of(account())
                    val hits =
                        result.value.artists.map { a ->
                            val ask = a.ask
                            val open = a.titleId == 0L && !a.neverAdd
                            MusicArtistHit(
                                mbid = a.mbid,
                                name = a.name,
                                type = a.type,
                                country = a.country,
                                disambiguation = a.disambiguation,
                                image = a.image,
                                titleId = a.titleId,
                                asked = ask != null,
                                askedByMe = ask?.mine == true,
                                action =
                                    when {
                                        !open -> MusicCardAction.NONE
                                        access.manage -> MusicCardAction.ADD
                                        access.ask && ask?.mine != true -> MusicCardAction.ASK
                                        else -> MusicCardAction.NONE
                                    },
                                tooAction = ask != null && !access.manage,
                            )
                        }
                    if (result.value.pending && hits.isEmpty() && tries < PAUSES.size) {
                        delay(PAUSES[tries++])
                        continue
                    }
                    mutableState.update { it.copy(searching = false, hits = hits, searched = term) }
                    return
                }
            }
        }
    }

    fun clear() {
        run++
        mutableState.value = MusicSearchState()
    }

    private companion object {
        const val MAX = 200
        val PAUSES = listOf(1_500L, 2_500L, 4_000L, 6_000L, 9_000L)
    }
}
