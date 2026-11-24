package ch.orellbuehler.acolyte.ui.title

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.OwnedEpisode
import ch.orellbuehler.acolyte.api.model.OwnedVersion
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.api.model.TitleReview
import ch.orellbuehler.acolyte.feature.title.AddTitlePresenter
import ch.orellbuehler.acolyte.feature.title.EpisodePresenter
import ch.orellbuehler.acolyte.feature.title.LeftoversPresenter
import ch.orellbuehler.acolyte.feature.title.ReleaseScope
import ch.orellbuehler.acolyte.feature.title.ReleaseScopeKind
import ch.orellbuehler.acolyte.feature.title.ReleasesPresenter
import ch.orellbuehler.acolyte.feature.title.RemovePresenter
import ch.orellbuehler.acolyte.feature.title.RenamePresenter
import ch.orellbuehler.acolyte.feature.title.ShowSeasonsPresenter
import ch.orellbuehler.acolyte.feature.title.SubtitlesPresenter
import ch.orellbuehler.acolyte.feature.title.TitlePresenter
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.feature.title.TrailerSource
import ch.orellbuehler.acolyte.ui.nav.TitleRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface TitleSheet {
    data object Cast : TitleSheet

    data class Cards(
        val title: String,
        val cards: List<TitleCard>,
    ) : TitleSheet

    data object History : TitleSheet

    data object Hide : TitleSheet

    data object Gate : TitleSheet

    data class Releases(
        val presenter: ReleasesPresenter,
    ) : TitleSheet

    data class Add(
        val presenter: AddTitlePresenter,
    ) : TitleSheet

    data class Episode(
        val presenter: EpisodePresenter,
    ) : TitleSheet

    data class SeasonTools(
        val season: Int,
    ) : TitleSheet

    data class Subtitles(
        val presenter: SubtitlesPresenter,
    ) : TitleSheet

    data class Leftovers(
        val presenter: LeftoversPresenter,
    ) : TitleSheet

    data class Rename(
        val presenter: RenamePresenter,
    ) : TitleSheet

    data class Remove(
        val presenter: RemovePresenter,
    ) : TitleSheet

    data class VersionForm(
        val version: OwnedVersion?,
        val keepFileId: Long = 0,
    ) : TitleSheet

    data class Edition(
        val fileId: Long,
        val current: String,
    ) : TitleSheet

    data class Adopt(
        val versionId: Long,
    ) : TitleSheet

    data class Move(
        val folder: LibraryFolder,
    ) : TitleSheet

    data class MarkFailed(
        val downloadId: Long,
    ) : TitleSheet

    data class Unprotect(
        val fileId: Long,
    ) : TitleSheet

    data class RemoveVersion(
        val version: OwnedVersion,
    ) : TitleSheet

    data class Review(
        val review: TitleReview,
    ) : TitleSheet

    data class Trailer(
        val source: TrailerSource,
    ) : TitleSheet
}

class TitleViewModel(
    private val sdk: AcolyteSdk,
    route: TitleRoute,
) : ViewModel() {
    val presenter: TitlePresenter = sdk.title(route.kind, route.tmdb, route.libraryId, route.tab == "library" || route.tab == "episodes")

    val state: StateFlow<TitleState> = presenter.state

    var sheet by mutableStateOf<TitleSheet?>(null)
        private set

    var trailerLoading by mutableStateOf(false)
        private set

    private var seasonsPresenter: ShowSeasonsPresenter? = null
    private var seasonsFor = 0L

    val reviews by lazy { presenter.reviews() }

    val seasonBrowser by lazy { presenter.seasonBrowser(LocalDate.now().year) }

    init {
        launch { presenter.load() }
    }

    fun launch(block: suspend () -> Unit): Job = viewModelScope.launch { block() }

    fun refresh() {
        launch { presenter.refresh() }
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

    fun open(next: TitleSheet) {
        sheet = next
    }

    fun close() {
        val closing = sheet
        sheet = null
        if (closing is TitleSheet.Releases ||
            closing is TitleSheet.Leftovers ||
            closing is TitleSheet.Rename ||
            closing is TitleSheet.Subtitles
        ) {
            launch { presenter.reloadOwned() }
        }
    }

    fun openReleases(scope: ReleaseScope) = open(TitleSheet.Releases(presenter.releases(scope)))

    fun openCheck() = open(TitleSheet.Releases(presenter.releases(ReleaseScope(ReleaseScopeKind.CHECK))))

    fun openAdd(
        choose: Boolean = false,
        checkId: Long = 0,
        release: TitleRelease? = null,
    ) = open(TitleSheet.Add(presenter.add(choose, checkId, release)))

    fun openAddAll() = open(TitleSheet.Add(presenter.addAll()))

    fun openEpisode(episode: OwnedEpisode) = open(TitleSheet.Episode(presenter.episode(episode)))

    fun openSubtitles(itemId: Long) = open(TitleSheet.Subtitles(presenter.subtitles(itemId)))

    fun subtitles(itemId: Long): SubtitlesPresenter = presenter.subtitles(itemId)

    fun openLeftovers() = open(TitleSheet.Leftovers(presenter.leftovers()))

    fun openRename(season: Int = -1) = open(TitleSheet.Rename(presenter.rename(season)))

    fun openRemove(
        season: Int = -1,
        episodeIds: List<Long> = emptyList(),
    ) = open(TitleSheet.Remove(presenter.remove(season, episodeIds)))

    fun playTrailer() {
        if (trailerLoading) return
        trailerLoading = true
        launch {
            val source = presenter.trailerStream()
            trailerLoading = false
            if (source != null) open(TitleSheet.Trailer(source))
        }
    }

    fun mediaHeaders(url: String): Map<String, String> = sdk.images.headers(url)

    fun mediaUrl(source: String): String? = sdk.images.url(source)
}
