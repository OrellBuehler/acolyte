package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.nav.MoreTab
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.nav.SectionGroup
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountSnapshot
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountTest {
    private fun account(
        role: String,
        perms: List<String>,
        off: List<String> = emptyList(),
        maxRating: String = "",
        mustTwoFactor: Boolean = false,
    ): Account {
        val parts = listOf("movies", "shows", "music", "subtitles", "wishlist", "sources").map { PartView(part = it, on = it !in off) }
        val me = Me(id = 2, name = "sam", role = role, perms = perms, maxRating = maxRating, mustTwoFactor = mustTwoFactor)
        return Account.from(BASE, SignInMethod.PASSWORD, AccountSnapshot(me, Parts(parts)))
    }

    private fun Account.group(group: SectionGroup) = moreGroups.firstOrNull { it.group == group }?.sections.orEmpty()

    @Test
    fun memberTabsFollowTheWebPhoneLayout() {
        val member = account("member", listOf("ask"))
        assertEquals(listOf(Section.HOME, Section.LIBRARY, Section.SEARCH, Section.REQUESTS), member.tabs)
        assertEquals(MoreTab.MORE, member.moreTab)
        assertEquals(listOf(Section.EXPLORE, Section.POPULAR), member.group(SectionGroup.DISCOVER))
        assertEquals(listOf(Section.DOWNLOADS, Section.CALENDAR), member.group(SectionGroup.YOURS))
        assertEquals(listOf(Section.ACCOUNT), member.group(SectionGroup.YOU))
        assertTrue(member.group(SectionGroup.SYSTEM).isEmpty())
        assertFalse(member.can(Need.MANAGE_LIBRARY))
    }

    @Test
    fun memberWithoutRequestsKeepsExplore() {
        val member = account("member", listOf("ask"), off = listOf("wishlist"))
        assertEquals(listOf(Section.HOME, Section.EXPLORE, Section.LIBRARY, Section.SEARCH), member.tabs)
    }

    @Test
    fun managersGetManage() {
        val admin = account("admin", listOf("ask", "approve", "manage_library"))
        assertEquals(listOf(Section.HOME, Section.EXPLORE, Section.LIBRARY, Section.SEARCH), admin.tabs)
        assertEquals(MoreTab.MANAGE, admin.moreTab)
        assertEquals(listOf(Section.CALENDAR, Section.REQUESTS), admin.group(SectionGroup.YOURS))
        assertEquals(listOf(Section.DOWNLOADS, Section.HISTORY), admin.group(SectionGroup.ACTIVITY))
        assertEquals(listOf(Section.HEALTH, Section.PEOPLE, Section.SETTINGS), admin.group(SectionGroup.SYSTEM))
        assertTrue(Section.STATS in admin.group(SectionGroup.LIBRARY))
        assertFalse(admin.can(Need.OWNER))
        assertTrue(admin.can(Need.USERS))
    }

    @Test
    fun approverWithoutLibraryIsAManagerToo() {
        val approver = account("member", listOf("ask", "approve"))
        assertEquals(MoreTab.MANAGE, approver.moreTab)
        assertTrue(Section.REQUESTS !in approver.tabs)
        assertEquals(listOf(Section.DOWNLOADS, Section.CALENDAR, Section.REQUESTS), approver.group(SectionGroup.YOURS))
    }

    @Test
    fun ownerCanDoEverything() {
        val owner = account("owner", emptyList())
        assertTrue(Need.entries.all { owner.can(it) })
    }

    @Test
    fun partsHideSections() {
        val noMusic = account("owner", emptyList(), off = listOf("wishlist", "music"))
        assertTrue(Section.REQUESTS !in noMusic.sections)
        assertTrue(Section.MUSIC !in noMusic.sections)
        assertTrue(Section.MOVIES in noMusic.sections)
        val noMedia = account("owner", emptyList(), off = listOf("movies", "shows", "music"))
        assertFalse(noMedia.isOn(Part.MEDIA))
        assertTrue(noMedia.tabs.isEmpty())
        assertTrue(Section.HEALTH in noMedia.sections)
    }

    @Test
    fun kidsDoNotSeeMusic() {
        assertTrue(Section.MUSIC !in account("member", listOf("ask"), maxRating = "PG-13").sections)
    }

    @Test
    fun mustSetUpTwoFactorLeavesOnlyAccount() {
        val admin = account("admin", listOf("manage_library"), mustTwoFactor = true)
        assertEquals(listOf(Section.ACCOUNT), admin.sections)
    }
}
