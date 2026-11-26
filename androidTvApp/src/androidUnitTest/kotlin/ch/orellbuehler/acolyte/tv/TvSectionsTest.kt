package ch.orellbuehler.acolyte.tv

import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import ch.orellbuehler.acolyte.tv.nav.TvSections
import ch.orellbuehler.acolyte.tv.nav.TvTabSelection
import ch.orellbuehler.acolyte.tv.nav.icon
import ch.orellbuehler.acolyte.tv.nav.titleRes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvSectionsTest {
    private fun account(
        role: Role,
        permissions: List<String> = emptyList(),
        parts: List<PartView> = emptyList(),
        kid: Boolean = false,
    ) = Account(
        serverUrl = "http://server",
        serverName = "server",
        method = SignInMethod.PASSWORD,
        userId = 1,
        userName = "anna",
        displayName = "Anna",
        role = role,
        permissions = permissions,
        parts = parts,
        serverVersion = null,
        avatarPath = null,
        mustSetUpTwoFactor = false,
        isKid = kid,
        safeMode = false,
    )

    @Test
    fun ownerSeesEveryTvSectionAndSettingsLast() {
        val tabs = TvSections.tabs(account(Role.OWNER))
        assertEquals(
            listOf(Section.HOME, Section.EXPLORE, Section.SEARCH, Section.REQUESTS, Section.CALENDAR, Section.DOWNLOADS, Section.SETTINGS),
            tabs,
        )
    }

    @Test
    fun memberWhoMayAskGetsRequestsAndExploreMovesToMore() {
        val tabs = TvSections.tabs(account(Role.MEMBER, permissions = listOf("ask")))
        assertEquals(
            listOf(Section.HOME, Section.EXPLORE, Section.SEARCH, Section.REQUESTS, Section.CALENDAR, Section.DOWNLOADS, Section.SETTINGS),
            tabs,
        )
    }

    @Test
    fun memberWithoutAskHasNoRequests() {
        val tabs = TvSections.tabs(account(Role.MEMBER))
        assertFalse(Section.REQUESTS in tabs)
        assertEquals(Section.SETTINGS, tabs.last())
    }

    @Test
    fun aSwitchedOffPartHidesItsSection() {
        val off = listOf("wishlist", "movies", "shows", "music").map { PartView(part = it, on = false) }
        val tabs = TvSections.tabs(account(Role.MEMBER, permissions = listOf("ask"), parts = off))
        assertFalse(Section.REQUESTS in tabs)
        assertFalse(Section.HOME in tabs)
        assertTrue(Section.SETTINGS in tabs)
    }

    @Test
    fun visibleFollowsTabsAndMoreGroups() {
        val owner = account(Role.OWNER)
        assertTrue(TvSections.visible(Section.DOWNLOADS, owner))
        assertFalse(TvSections.visible(Section.SETTINGS, account(Role.MEMBER)))
        assertFalse(TvSections.visible(Section.REQUESTS, account(Role.MEMBER)))
    }

    @Test
    fun tabSelectionOnlyAcceptsItsTabs() {
        val selection = TvTabSelection(listOf(Section.HOME, Section.SEARCH, Section.SETTINGS))
        assertEquals(Section.HOME, selection.current)
        assertTrue(selection.select(Section.SETTINGS))
        assertEquals(Section.SETTINGS, selection.current)
        assertFalse(selection.select(Section.EXPLORE))
        assertEquals(Section.SETTINGS, selection.current)
    }

    @Test
    fun everySectionHasATitleAndIcon() {
        Section.entries.forEach {
            it.titleRes
            it.icon
        }
        assertEquals(
            Section.entries.size,
            Section.entries
                .map { it.titleRes }
                .toSet()
                .size,
        )
    }
}
