package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.model.MusicUnmatchedArtist
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MusicUnmatchedState(
    val total: Int = 0,
    val artists: List<MusicUnmatchedArtist> = emptyList(),
) {
    val shown: List<MusicUnmatchedArtist> get() = artists.take(SHOWN)

    val more: Int get() = (artists.size - SHOWN).coerceAtLeast(0)

    private companion object {
        const val SHOWN = 5
    }
}

class MusicUnmatchedPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(MusicUnmatchedState())

    val state: StateFlow<MusicUnmatchedState> = mutableState.asStateFlow()

    suspend fun load() {
        val me = account()
        if (me == null || !me.managesLibrary || !me.shows(ch.orellbuehler.acolyte.nav.Section.MUSIC)) {
            mutableState.value = MusicUnmatchedState()
            return
        }
        val result = api.result(MusicEndpoints.unmatched)
        mutableState.value =
            if (result is ApiResult.Ok) MusicUnmatchedState(result.value.total, result.value.artists) else MusicUnmatchedState()
    }
}
