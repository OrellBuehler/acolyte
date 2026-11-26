package ch.orellbuehler.acolyte.tv.title

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.title.ShowSeasonsPresenter
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.feature.title.TrailerSource
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TvTitleViewModel(
    private val sdk: AcolyteSdk,
    kind: String,
    tmdb: String,
    libraryId: Long,
) : ViewModel() {
    private val presenter = sdk.title(kind, tmdb, libraryId, false)
    private var seasonsPresenter: ShowSeasonsPresenter? = null
    private var seasonsFor = 0L

    val state: StateFlow<TitleState> = presenter.state

    var trailer by mutableStateOf<TrailerSource?>(null)
        private set

    var trailerLoading by mutableStateOf(false)
        private set

    val seasonBrowser by lazy { presenter.seasonBrowser(LocalDate.now().year) }

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun selectSeason(number: Int) {
        viewModelScope.launch { seasonBrowser.select(number) }
    }

    fun toggleFollow() {
        viewModelScope.launch { presenter.toggleFollow() }
    }

    fun playTrailer() {
        if (trailerLoading) return
        trailerLoading = true
        viewModelScope.launch {
            trailer = presenter.trailerStream() ?: TrailerSource()
            trailerLoading = false
        }
    }

    fun closeTrailer() {
        trailer = null
    }

    fun seasons(): ShowSeasonsPresenter {
        val id = state.value.ownedId
        val known = seasonsPresenter
        if (known != null && seasonsFor == id) return known
        return presenter.seasons().also {
            seasonsPresenter = it
            seasonsFor = id
        }
    }

    fun mediaHeaders(url: String): Map<String, String> = sdk.images.headers(url)

    fun mediaUrl(source: String): String? = sdk.images.url(source)

    suspend fun watch() = presenter.watch()
}
