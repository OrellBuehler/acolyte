package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.api.model.SettingsStatusAnswer
import ch.orellbuehler.acolyte.api.model.SettingsStatusItem
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.downloads.DownloadBadges
import ch.orellbuehler.acolyte.feature.requests.RequestsInbox
import ch.orellbuehler.acolyte.feature.settings.SettingsCatalog
import ch.orellbuehler.acolyte.feature.settings.SettingsGroup
import ch.orellbuehler.acolyte.feature.settings.SettingsHomeLogic
import ch.orellbuehler.acolyte.feature.settings.SettingsHomePresenter
import ch.orellbuehler.acolyte.feature.settings.SettingsHomeReasonKind
import ch.orellbuehler.acolyte.feature.settings.SettingsHomeStanding
import ch.orellbuehler.acolyte.feature.settings.SettingsPage
import ch.orellbuehler.acolyte.feature.settings.SettingsPageText
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountSnapshot
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsHomeTest {
    private fun account(
        role: String,
        perms: List<String>,
        off: List<String> = emptyList(),
        held: List<String> = listOf("ai_apps"),
        mustTwoFactor: Boolean = false,
    ): Account {
        val parts = listOf("movies", "shows", "music", "subtitles", "wishlist", "sources").map { PartView(part = it, on = it !in off) }
        val me = Me(id = 2, name = "sam", role = role, perms = perms, held = held, mustTwoFactor = mustTwoFactor, setupSkipped = true)
        return Account.from(BASE, SignInMethod.PASSWORD, AccountSnapshot(me, Parts(parts)))
    }

    private fun pagesOf(account: Account) = SettingsCatalog.pages(account)

    @Test
    fun ownerSeesEverythingExceptHeldFeatures() {
        val pages = pagesOf(account("owner", listOf("settings")))
        assertTrue(SettingsPage.REMOTE_ACCESS in pages)
        assertTrue(SettingsPage.PARTS in pages)
        assertFalse(SettingsPage.AI in pages)
        assertTrue(SettingsPage.AI in pagesOf(account("owner", listOf("settings"), held = emptyList())))
    }

    @Test
    fun settingsPermissionWithoutUsersRole() {
        val pages = pagesOf(account("member", listOf("settings")))
        assertTrue(SettingsPage.SIGN_IN in pages)
        assertTrue(SettingsPage.APPEARANCE in pages)
        assertFalse(SettingsPage.PARTS in pages)
        assertFalse(SettingsPage.LOGS in pages)
        assertFalse(SettingsPage.DANGER in pages)
        assertFalse(SettingsPage.REMOTE_ACCESS in pages)
    }

    @Test
    fun managerWithoutSettingsSeesTitleDefaultsAndQuality() {
        val account = account("member", listOf("manage_library"))
        val pages = pagesOf(account)
        assertTrue(SettingsPage.TITLE_DEFAULTS in pages)
        assertTrue(SettingsPage.PICK_RULES in pages)
        assertFalse(SettingsPage.SIGN_IN in pages)
        assertFalse(SettingsCatalog.hasHome(account))
    }

    @Test
    fun adminSeesUsersPages() {
        val pages = pagesOf(account("admin", listOf("settings")))
        assertTrue(SettingsPage.PARTS in pages)
        assertTrue(SettingsPage.LOGS in pages)
        assertTrue(SettingsPage.DANGER in pages)
        assertFalse(SettingsPage.REMOTE_ACCESS in pages)
    }

    @Test
    fun partsThatAreOffHideTheirPages() {
        val pages = pagesOf(account("owner", listOf("settings"), off = listOf("music", "subtitles", "wishlist")))
        assertFalse(SettingsPage.MUSIC in pages)
        assertFalse(SettingsPage.SUBTITLES in pages)
        assertFalse(SettingsPage.REQUESTS in pages)
        assertTrue(SettingsPage.TITLE_DEFAULTS in pages)
        val allOff = pagesOf(account("owner", listOf("settings"), off = listOf("movies", "shows", "music")))
        assertFalse(SettingsPage.TITLE_DEFAULTS in allOff)
        assertFalse(SettingsPage.NAMING in allOff)
    }

    @Test
    fun twoFactorSetupLeavesOnlyAccount() {
        val pages = pagesOf(account("owner", listOf("settings"), mustTwoFactor = true))
        assertEquals(listOf(SettingsPage.ACCOUNT), pages)
    }

    @Test
    fun groupsFollowTheWebOrder() {
        val groups = SettingsCatalog.groups(account("owner", listOf("settings"), held = emptyList()))
        assertEquals(
            listOf(
                SettingsGroup.LIBRARY,
                SettingsGroup.DOWNLOADS,
                SettingsGroup.USERS,
                SettingsGroup.CONNECTIONS,
                SettingsGroup.SYSTEM,
                SettingsGroup.ACCOUNT,
            ),
            groups.map { it.first },
        )
        assertEquals(SettingsPage.LIBRARY_FOLDERS, groups.first().second.first())
    }

    private val answer =
        SettingsStatusAnswer(
            items =
                listOf(
                    SettingsStatusItem("maintenance", "/settings/backups", "Backups: No backup yet", "warning"),
                    SettingsStatusItem("linked", "/settings/downloaders", "The Downloader qBit is not answering", "error"),
                    SettingsStatusItem("maintenance", "/settings/this-app", "A newer HolyArrs is out", "info"),
                ),
            unset = mapOf("copies" to listOf("/settings/sources")),
            known = listOf("/settings/alerts", "/settings/health"),
        )

    @Test
    fun standingsFollowTheWebRules() {
        assertEquals(SettingsHomeStanding.ATTENTION to 1, SettingsHomeLogic.standing(answer, listOf("/settings/backups")))
        assertEquals(
            SettingsHomeStanding.UNSET to 0,
            SettingsHomeLogic.standing(answer, listOf("/settings/sources", "/settings/downloaders2")),
        )
        assertEquals(SettingsHomeStanding.OK to 0, SettingsHomeLogic.standing(answer, listOf("/settings/alerts")))
        assertEquals(SettingsHomeStanding.UNKNOWN to 0, SettingsHomeLogic.standing(answer, listOf("/settings/logs")))
        assertEquals(SettingsHomeStanding.UNKNOWN to 0, SettingsHomeLogic.standing(null, listOf("/settings/backups")))
        val groups = SettingsHomeLogic.groups(SettingsCatalog.groups(account("owner", listOf("settings"))), answer)
        val system = groups.first { it.group == SettingsGroup.SYSTEM }
        assertEquals(SettingsHomeStanding.ATTENTION, system.standing)
        assertEquals(2, system.count)
    }

    @Test
    fun reasonsAreSortedAndSplit() {
        val reasons = SettingsHomeLogic.settingReasons(answer.items)
        assertEquals(listOf(Tone.FAIL, Tone.WAIT, Tone.INFO), reasons.map { it.tone })
        val backups = reasons[1]
        assertEquals(SettingsPage.BACKUPS, backups.page)
        assertEquals("No backup yet", backups.title)
        assertEquals("", backups.body)
        val keyKit =
            SettingsHomeLogic.settingReasons(
                listOf(
                    SettingsStatusItem(
                        "",
                        "/settings/backups",
                        "Backups: Download your key kit: Backups don't include the master key",
                        "info",
                    ),
                ),
            )
        assertEquals("Download your key kit", keyKit[0].title)
        assertEquals("Backups don't include the master key", keyKit[0].body)
    }

    @Test
    fun searchIgnoresAccentsAndRanksLabels() {
        val texts =
            listOf(
                SettingsPageText("backups", "Backups", "Save and restore HolyArrs", "Server"),
                SettingsPageText("sign_in", "Anmeldung und Sicherheit", "Passwörter, Zwei-Faktor", "Personen"),
                SettingsPageText("logs", "Protokolle", "Das Server-Protokoll lesen", "Server"),
            )
        assertEquals(listOf("sign_in"), SettingsCatalog.search("passwort", texts))
        assertEquals(listOf("sign_in"), SettingsCatalog.search("PASSWORTER", texts))
        assertEquals(listOf("backups"), SettingsCatalog.search("restore", texts))
        assertEquals(listOf("sign_in"), SettingsCatalog.search("sso", texts))
        assertEquals(listOf("backups", "logs"), SettingsCatalog.search("server", texts))
        assertTrue(SettingsCatalog.search("   ", texts).isEmpty())
    }

    @Test
    fun presenterBuildsDotListAndHidesUpdateNews() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/settings/status",
                """{"items":[{"group":"maintenance","page":"/settings/backups","text":"Backups: No backup yet","severity":"warning"},""" +
                    """{"group":"maintenance","page":"/settings/this-app","text":"A newer HolyArrs is out","severity":"info"}],""" +
                    """"unset":{},"known":["/settings/alerts"],"metadata_up":true}""",
            )
            server.json("GET", "/api/v1/app/update", """{"on":true,"newer":true,"latest":"v1.4.0","version":"1.3.0"}""")
            server.json("GET", "/api/v1/needs-decision", """{"items":[{"slot_id":1},{"slot_id":2}]}""")
            val owner = account("owner", listOf("settings", "manage_library", "approve"))
            val api = server.api()
            val presenter = SettingsHomePresenter(api, { owner }, DownloadBadges(api, { owner }), RequestsInbox(api) { owner })
            assertTrue(presenter.state.value.allowed)
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertTrue(state.needsAttention)
            assertTrue(state.showFinishSetup)
            assertEquals(
                listOf(SettingsHomeReasonKind.SETTING, SettingsHomeReasonKind.UPDATE),
                state.reasons.map { it.kind },
            )
            assertEquals("1.4.0", state.reasons.last().body)
            assertEquals(SettingsHomeStanding.ATTENTION, state.groups.first { it.group == SettingsGroup.SYSTEM }.standing)
        }

    @Test
    fun presenterWithoutSettingsPermissionKnowsNothing() =
        runTest {
            val server = FakeServer()
            val admin = account("admin", listOf("manage_library"))
            val api = server.api()
            val presenter = SettingsHomePresenter(api, { admin }, DownloadBadges(api, { admin }), RequestsInbox(api) { admin })
            presenter.load()
            assertTrue(presenter.state.value.allowed)
            assertFalse(presenter.state.value.needsAttention)
            assertTrue(server.requests.none { it.path == "/api/v1/settings/status" })
        }
}
