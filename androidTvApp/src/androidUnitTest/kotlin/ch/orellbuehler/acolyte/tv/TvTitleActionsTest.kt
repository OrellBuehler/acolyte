package ch.orellbuehler.acolyte.tv

import android.content.res.Configuration
import ch.orellbuehler.acolyte.api.model.LookupMovie
import ch.orellbuehler.acolyte.api.model.OwnedTitle
import ch.orellbuehler.acolyte.api.model.TitleAsk
import ch.orellbuehler.acolyte.api.model.TitleDetails
import ch.orellbuehler.acolyte.api.model.TitleVideo
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.title.TitleAccess
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.tv.title.TvTitleActionKind
import ch.orellbuehler.acolyte.tv.title.tvTitleActions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TvTitleActionsTest {
    private val trailer = TitleVideo(key = "dQw4w9WgXcQ", type = "Trailer", language = "en")

    private fun state(
        kind: String = "movie",
        access: TitleAccess = TitleAccess(mayAsk = true, followOn = true),
        owned: OwnedTitle? = null,
        videos: List<TitleVideo> = emptyList(),
        movie: LookupMovie? = null,
        fourKOn: Boolean = false,
        following: Boolean = false,
    ) = TitleState(
        kind = kind,
        tmdb = "1",
        libraryId = owned?.id ?: 0,
        access = access,
        status = LoadStatus(Phase.READY),
        details = TitleDetails(tmdbId = "1", kind = kind, name = "A title", videos = videos),
        owned = owned,
        movie = movie,
        fourKOn = fourKOn,
        following = following,
    )

    private fun kinds(state: TitleState) = tvTitleActions(state, trailerLoading = false).map { it.kind }

    @Test
    fun showInLibraryWithoutTrailerOffersOnlyFollow() {
        val owned = OwnedTitle(id = 7, kind = "show", name = "A title")
        assertEquals(listOf(TvTitleActionKind.FOLLOW), kinds(state(kind = "show", owned = owned)))
    }

    @Test
    fun nothingApplyingYieldsNoActions() {
        assertEquals(emptyList(), kinds(state(access = TitleAccess())))
        assertEquals(emptyList(), kinds(state(access = TitleAccess(followOn = true)).copy(tmdb = "")))
    }

    @Test
    fun notInLibraryOffersRequestTrailerAndFollowInOrder() {
        assertEquals(
            listOf(TvTitleActionKind.ASK, TvTitleActionKind.TRAILER, TvTitleActionKind.FOLLOW),
            kinds(state(videos = listOf(trailer))),
        )
    }

    @Test
    fun requestedMovieIsDisabledAndFourKFollowsOwnedOffer() {
        val asked = state(movie = LookupMovie(ask = TitleAsk(state = "asked", mine = true)))
        val request = tvTitleActions(asked, false).first { it.kind == TvTitleActionKind.ASK }
        assertTrue(!request.enabled)
        val owned = OwnedTitle(id = 7, kind = "movie", name = "A title")
        val withFourK = state(owned = owned, movie = LookupMovie(titleId = 7), fourKOn = true)
        assertTrue(TvTitleActionKind.ASK_4K in kinds(withFourK))
    }

    @Test
    fun trailerAndFollowReflectBusyState() {
        val s = state(videos = listOf(trailer), following = true).copy(followBusy = true)
        val actions = tvTitleActions(s, trailerLoading = true)
        assertTrue(actions.first { it.kind == TvTitleActionKind.TRAILER }.enabled.not())
        assertTrue(actions.first { it.kind == TvTitleActionKind.FOLLOW }.enabled.not())
    }

    @Test
    fun everyActionHasALabelInEnglishAndGerman() {
        val owned = OwnedTitle(id = 7, kind = "movie", name = "A title")
        val states =
            listOf(
                state(videos = listOf(trailer)),
                state(kind = "show", videos = listOf(trailer), following = true),
                state(movie = LookupMovie(ask = TitleAsk(state = "asked", mine = true))),
                state(movie = LookupMovie(ask = TitleAsk(state = "asked", mine = false))),
                state(owned = owned, movie = LookupMovie(titleId = 7), fourKOn = true),
                state(owned = owned, movie = LookupMovie(titleId = 7, ask4k = TitleAsk(state = "asked", mine = true)), fourKOn = true),
            )
        val base = RuntimeEnvironment.getApplication()
        val contexts =
            listOf(Locale.ENGLISH, Locale.GERMAN).map { locale ->
                base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
            }
        val actions = states.flatMap { tvTitleActions(it, trailerLoading = false) }
        assertTrue(actions.isNotEmpty())
        for (action in actions) {
            for (context in contexts) {
                assertTrue(context.getString(action.label).isNotBlank(), "empty label for ${action.kind}")
            }
        }
    }
}
