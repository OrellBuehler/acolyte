package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.FilePoints
import ch.orellbuehler.acolyte.api.model.KeepBody
import ch.orellbuehler.acolyte.api.model.OwnedEpisode
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.TitleEvent
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EpisodeState(
    val episode: OwnedEpisode,
    val manage: Boolean = false,
    val subtitlesOn: Boolean = false,
    val synopsis: String = "",
    val events: List<TitleEvent> = emptyList(),
    val eventsLoaded: Boolean = false,
    val points: FilePoints? = null,
    val busy: Boolean = false,
    val feedback: Feedback? = null,
) {
    val tone: EpisodeTone get() = TitleLogic.episodeTone(episode)

    val file: ch.orellbuehler.acolyte.api.model.OwnedFile? get() = episode.mainSlot?.file

    val code: String get() = TitleLogic.episodeCode(episode.season, episode.episode)

    val downloading: Boolean get() = tone == EpisodeTone.DOWNLOADING

    val chosenWhy: String get() =
        episode.mainSlot
            ?.takeIf { it.state == "chosen" }
            ?.reason
            .orEmpty()
}

class EpisodePresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    private val tmdb: String,
    episode: OwnedEpisode,
    manage: Boolean,
    subtitlesOn: Boolean,
    private val onChanged: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(EpisodeState(episode, manage, subtitlesOn))
    private val feedback = FeedbackCounter()

    val state: StateFlow<EpisodeState> = mutableState.asStateFlow()

    suspend fun load() =
        coroutineScope {
            val e = mutableState.value.episode
            if (tmdb.isNotEmpty()) {
                launch {
                    val season = e.tmdbSeason.takeIf { it > 0 } ?: e.season
                    val number = e.tmdbEpisode.takeIf { it > 0 } ?: e.episode
                    api.result(TitleEndpoints.season, "tmdb_id" to tmdb, "number" to season).onSuccess { v ->
                        val text =
                            v.episodes
                                .firstOrNull { it.number == number }
                                ?.overview
                                .orEmpty()
                        mutableState.update { it.copy(synopsis = text) }
                    }
                }
            }
            if (mutableState.value.manage) {
                launch {
                    val events = api.result(TitleSupportEndpoints.events, "episode" to e.id, "limit" to 20).valueOrNull()?.events
                    mutableState.update {
                        it.copy(events = events.orEmpty().filter { ev -> ev.kind !in TitleState.routineKinds }, eventsLoaded = true)
                    }
                }
                val slot = e.mainSlot
                if (slot != null && slot.file != null) {
                    launch {
                        api.result(TitleEndpoints.points, "id" to titleId, "slot" to slot.id).onSuccess { v ->
                            mutableState.update { it.copy(points = v) }
                        }
                    }
                }
            } else {
                mutableState.update { it.copy(eventsLoaded = true) }
            }
        }

    suspend fun lookFor() {
        val e = mutableState.value.episode
        val slot = e.mainSlot ?: return
        mutableState.update { it.copy(busy = true) }
        api
            .result(LibraryEndpoints.search, SearchBody(slotId = slot.id), "id" to titleId)
            .onSuccess { say(feedback.next(FeedbackKind.SEARCHING_EPISODE, mutableState.value.code)) }
            .onFailure { say(feedback.failed(it)) }
        mutableState.update { it.copy(busy = false) }
    }

    suspend fun setKeep(keep: Boolean) {
        val e = mutableState.value.episode
        mutableState.update { it.copy(busy = true) }
        api
            .result(TitleEndpoints.keepEpisode, KeepBody(keep), "id" to e.id)
            .onSuccess { updated -> mutableState.update { it.copy(episode = it.episode.copy(keepUpToDate = updated.keepUpToDate)) } }
            .onFailure { say(feedback.failed(it)) }
        mutableState.update { it.copy(busy = false) }
        onChanged()
    }

    private fun say(next: Feedback) {
        mutableState.update { it.copy(feedback = next) }
    }
}
