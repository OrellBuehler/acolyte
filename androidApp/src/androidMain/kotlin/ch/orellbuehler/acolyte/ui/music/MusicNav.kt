package ch.orellbuehler.acolyte.ui.music

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import ch.orellbuehler.acolyte.feature.music.MusicArtistHit
import ch.orellbuehler.acolyte.feature.music.MusicCardAction
import ch.orellbuehler.acolyte.feature.music.MusicCardKind
import ch.orellbuehler.acolyte.feature.music.MusicItem
import ch.orellbuehler.acolyte.feature.music.MusicOpenKind
import ch.orellbuehler.acolyte.feature.music.MusicTarget
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.nav.MusicAddRoute
import ch.orellbuehler.acolyte.ui.nav.MusicAlbumRoute
import ch.orellbuehler.acolyte.ui.nav.MusicArtistRoute
import ch.orellbuehler.acolyte.ui.nav.MusicGenreRoute
import ch.orellbuehler.acolyte.ui.nav.MusicLibraryAlbumRoute
import ch.orellbuehler.acolyte.ui.nav.MusicLibraryArtistRoute
import ch.orellbuehler.acolyte.ui.nav.MusicPageRoute
import ch.orellbuehler.acolyte.ui.nav.Routes

class MusicNav(
    val openArtist: (mbid: String) -> Unit,
    val openAlbum: (mbid: String, artist: String) -> Unit,
    val openGenre: (slug: String) -> Unit,
    val openPage: (kind: String, slug: String) -> Unit,
    val openLibraryArtist: (titleId: Long, tab: String) -> Unit,
    val openLibraryAlbum: (titleId: Long, albumId: Long) -> Unit,
    val add: (mbid: String, name: String) -> Unit,
    val ask: (artist: String, name: String, album: String) -> Unit,
    val openSection: (Section) -> Unit,
    val openRoute: (Any) -> Unit,
    val back: () -> Unit,
) {
    fun open(target: MusicTarget) {
        when (target.kind) {
            MusicOpenKind.ALBUM -> openAlbum(target.mbid, target.artist)
            MusicOpenKind.ARTIST -> if (target.mbid.isNotEmpty()) openArtist(target.mbid)
            MusicOpenKind.GENRE -> openGenre(target.slug)
            MusicOpenKind.PAGE -> openPage(target.page, target.slug)
            MusicOpenKind.LIBRARY_ALBUM -> openLibraryAlbum(target.titleId, target.partId)
            MusicOpenKind.LIBRARY_ARTIST -> openLibraryArtist(target.titleId, "")
            MusicOpenKind.SEARCH -> openSection(Section.SEARCH)
            MusicOpenKind.NONE -> Unit
        }
    }

    fun open(item: MusicItem) = open(item.target)

    fun act(item: MusicItem) {
        val album = if (item.kind == MusicCardKind.ALBUM) item.mbid else ""
        when (item.action) {
            MusicCardAction.ADD -> add(item.artistMbid, item.artist.ifEmpty { item.name })
            MusicCardAction.ASK -> ask(item.artistMbid, item.artist.ifEmpty { item.name }, album)
            MusicCardAction.NONE -> Unit
        }
    }

    fun act(hit: MusicArtistHit) {
        when (hit.action) {
            MusicCardAction.ADD -> add(hit.mbid, hit.name)
            MusicCardAction.ASK -> ask(hit.mbid, hit.name, "")
            MusicCardAction.NONE -> Unit
        }
    }
}

@Composable
fun rememberMusicNav(
    nav: NavHostController,
    openSection: (Section) -> Unit,
): MusicNav =
    remember(nav) {
        MusicNav(
            openArtist = { nav.navigate(MusicArtistRoute(it)) },
            openAlbum = { mbid, artist -> nav.navigate(MusicAlbumRoute(mbid, artist)) },
            openGenre = { nav.navigate(MusicGenreRoute(it)) },
            openPage = { kind, slug -> nav.navigate(MusicPageRoute(kind, slug)) },
            openLibraryArtist = { id, tab -> nav.navigate(MusicLibraryArtistRoute(id, tab)) },
            openLibraryAlbum = { id, album -> nav.navigate(MusicLibraryAlbumRoute(id, album)) },
            add = { mbid, name -> nav.navigate(MusicAddRoute(mbid, name)) },
            ask = { artist, name, album -> nav.navigate(Routes.askMusic(artist, name, album)) },
            openSection = openSection,
            openRoute = { nav.navigate(it) },
            back = { nav.popBackStack() },
        )
    }
