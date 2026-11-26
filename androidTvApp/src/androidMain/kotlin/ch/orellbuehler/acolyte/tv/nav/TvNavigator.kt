package ch.orellbuehler.acolyte.tv.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowseTarget
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account

class TvTabSelection(
    val tabs: List<Section>,
) {
    var index by mutableIntStateOf(0)

    val current: Section get() = tabs[index.coerceIn(tabs.indices)]

    fun select(section: Section): Boolean {
        val position = tabs.indexOf(section)
        if (position < 0) return false
        index = position
        return true
    }
}

class TvNavigator(
    val account: Account,
    private val nav: NavHostController,
    private val selection: TvTabSelection,
) {
    fun back() {
        nav.popBackStack()
    }

    fun openTitle(
        kind: String,
        tmdb: String,
        libraryId: Long = 0,
    ) {
        nav.navigate(TitleRoute(kind, tmdb, libraryId))
    }

    fun openAsk(
        kind: String,
        tmdb: String,
        fourK: Boolean = false,
    ) {
        nav.navigate(AskRoute(kind, tmdb, fourK))
    }

    fun openPerson(id: String) {
        nav.navigate(PersonRoute(id))
    }

    fun openCollection(id: String) {
        nav.navigate(CollectionRoute(id))
    }

    fun openGenre(
        type: String,
        genre: Int,
    ) {
        nav.navigate(GenreRoute(type, genre))
    }

    fun openBrowse(link: BrowseLink) {
        if (link.target == BrowseTarget.GENRE) {
            openGenre(link.type.wire, link.genre)
        } else {
            nav.navigate(BrowseRoute(link.target.name, link.type.wire, link.genre, link.category, link.mood))
        }
    }

    fun openSection(section: Section) {
        if (selection.select(section)) {
            nav.popBackStack(TabsRoute, inclusive = false)
        } else if (section in TvSections.pushed) {
            nav.navigate(SectionRoute(section.name))
        } else {
            openOnPhone(section)
        }
    }

    fun openOnPhone(section: Section) {
        nav.navigate(PhoneRoute(section = section.name))
    }

    fun openOnPhone(topic: PhoneTopic) {
        nav.navigate(PhoneRoute(topic = topic.name))
    }

    fun openCard(card: DiscoverCard) {
        if (card.opensTitle) {
            openTitle(card.kind.wire, card.tmdb)
        } else if (card.kind == CardKind.ARTIST) {
            openOnPhone(Section.MUSIC)
        }
    }
}

val LocalTvNavigator =
    staticCompositionLocalOf<TvNavigator> { error("TvNavigator is not provided") }
