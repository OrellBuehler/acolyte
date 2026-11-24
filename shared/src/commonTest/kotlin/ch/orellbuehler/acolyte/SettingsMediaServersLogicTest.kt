package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersForm
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersLogic
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersRow
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsMediaServersLogicTest {
    private val logic = SettingsMediaServersLogic

    @Test
    fun versionIsTheFirstDottedNumberOutsidePathsAndHosts() {
        assertEquals("v4.0.1", logic.testVersion("Sonarr 4.0.1.929"))
        assertEquals("v10.9", logic.testVersion("Jellyfin v10.9"))
        assertEquals("", logic.testVersion("HTPC"))
        assertEquals("", logic.testVersion("http://192.168.1.10:8096"))
        assertEquals("", logic.testVersion("/opt/app-1.2.3/bin"))
        assertEquals("", logic.testVersion("user@host1.2.3"))
        assertEquals("v2.0", logic.testVersion("host-1.2 then 2.0"))
        assertEquals("", logic.testVersion("5"))
        assertEquals("", logic.testVersion(""))
    }

    @Test
    fun appIsTheFirstKnownWord() {
        assertEquals("Jellyfin", logic.testApp("My Jellyfin server 10.9.1"))
        assertEquals("qBittorrent", logic.testApp("qbittorrent v4.6"))
        assertEquals("aria2", logic.testApp("aria2 1.36"))
        assertEquals("", logic.testApp("something else"))
        assertEquals("", logic.testApp(""))
    }

    @Test
    fun labelJoinsAppVersionAndExtra() {
        assertEquals("Jellyfin v10.9.1", logic.testLabel("10.9.1", "Jellyfin"))
        assertEquals("Plex · 3 libraries", logic.testLabel("", "Plex", "3 libraries"))
        assertEquals("Jellyfin", logic.testLabel("Server", "Jellyfin"))
        assertEquals("Connected", logic.testLabel("ok"))
        assertEquals("Verbunden · extra", logic.testLabel("ok", extra = "extra", connected = "Verbunden"))
        assertEquals("Emby v4.8.0", logic.testLabel("Emby 4.8.0.5"))
    }

    @Test
    fun connectHelpUrlPointsAtTheRowOfTheDocsTable() {
        val base = SettingsMediaServersLogic.CONNECT_HELP
        assertEquals("$base:~:text=A%20container%20name%20like", logic.connectHelpUrl("jellyfin → not found."))
        assertEquals("$base:~:text=A%20.local%20name%20like", logic.connectHelpUrl("nas.local → not found."))
        assertEquals("$base:~:text=A%20Tailscale%20name", logic.connectHelpUrl("box.tail.ts.net → not found."))
        assertEquals("$base:~:text=A%20Pi%2Dhole%20or%20router%20name", logic.connectHelpUrl("media.lan → not found."))
        assertEquals("$base:~:text=Not%20answering", logic.connectHelpUrl("jellyfin → 10.0.0.5 → refused on port 8096"))
        assertEquals("$base:~:text=Not%20answering", logic.connectHelpUrl("jellyfin → 10.0.0.5 → no answer on port 8096"))
        assertEquals("", logic.connectHelpUrl("Connection refused."))
        assertEquals("", logic.connectHelpUrl(""))
    }

    @Test
    fun plainMessageCapitalisesAndEndsTheSentence() {
        assertEquals("Another media server has that name.", logic.plainMessage("another media server has that name"))
        assertEquals("Done!", logic.plainMessage("done!"))
        assertEquals("Fine (really)", logic.plainMessage(" fine (really) "))
        assertEquals("", logic.plainMessage("  "))
    }

    @Test
    fun errorDetailOnlyForAnswersTheServerExplained() {
        val conflict = ApiError(ApiErrorCode.CONFLICT, 409, "another media server has that name")
        assertEquals("Another media server has that name.", logic.detail(conflict))
        assertEquals("", logic.detail(ApiError(ApiErrorCode.SERVER, 500, "something went wrong")))
        assertEquals("", logic.detail(ApiError(ApiErrorCode.OFFLINE, 0, "Unable to resolve host")))
        assertEquals("", logic.detail(null))
        assertEquals("Boom.", logic.serverText(ApiError(ApiErrorCode.SERVER, 500, "boom")))
        assertEquals("", logic.serverText(ApiError(ApiErrorCode.OFFLINE, 0, "Unable to resolve host")))
    }

    @Test
    fun plexAddressesPutTheSecureOneFirstOnlyAtHome() {
        val home = "http://10.0.0.2:32400"
        val secure = "https://a.plex.direct:32400"
        assertEquals(listOf(secure, home), logic.plexAddresses(true, home, secure))
        assertEquals(listOf(home), logic.plexAddresses(true, home, home))
        assertEquals(listOf(secure), logic.plexAddresses(false, "http://1.2.3.4:32400", secure))
        assertEquals(listOf("http://1.2.3.4:32400"), logic.plexAddresses(false, "http://1.2.3.4:32400", ""))
        assertEquals(emptyList(), logic.plexAddresses(false, "", ""))
        assertTrue(logic.plexRank(true, false) < logic.plexRank(false, false))
        assertTrue(logic.plexRank(false, false) < logic.plexRank(false, true))
    }

    @Test
    fun nextLanguagePrefersTheDeviceLanguageThenTheCommonOnes() {
        val known = listOf("de", "en", "fr", "nl", "sv")
        assertEquals("de", logic.nextLanguage(emptyList(), known, "de"))
        assertEquals("en", logic.nextLanguage(listOf("de"), known, "de"))
        assertEquals("fr", logic.nextLanguage(listOf("de", "en"), known, "de"))
        assertEquals("nl", logic.nextLanguage(listOf("de", "en", "fr"), known, "de"))
        assertEquals("sv", logic.nextLanguage(listOf("de", "en", "fr", "nl"), known, "xx"))
        assertNull(logic.nextLanguage(known, known, "de"))
        assertEquals("en", logic.nextLanguage(emptyList(), known, "xx"))
    }

    @Test
    fun languageListsKeepOffAtTheEnd() {
        assertEquals(listOf("de", "fr"), logic.replaceLanguage(listOf("de", "en"), 1, "fr"))
        assertEquals(listOf("de", "off"), logic.replaceLanguage(listOf("de", "en", "fr"), 1, "off"))
        assertEquals(listOf("off"), logic.replaceLanguage(listOf("de", "en"), 0, "off"))
        assertEquals(listOf("de"), logic.replaceLanguage(listOf("de"), 3, "fr"))
        assertEquals(listOf("en", "de", "fr"), logic.moveLanguage(listOf("de", "en", "fr"), 0, up = false))
        assertEquals(listOf("en", "de"), logic.moveLanguage(listOf("de", "en"), 1, up = true))
        assertEquals(listOf("de", "en"), logic.moveLanguage(listOf("de", "en"), 0, up = true))
        assertEquals(listOf("de", "off"), logic.moveLanguage(listOf("de", "off"), 0, up = false))
        assertEquals(listOf("de", "off"), logic.moveLanguage(listOf("de", "off"), 1, up = true))
    }

    @Test
    fun formShowsTheFieldsOfItsKind() {
        val plex = SettingsMediaServersForm(kind = "plex")
        assertTrue(plex.isPlex)
        assertTrue(plex.showsToken)
        assertFalse(plex.copy(usePlexSignIn = true).showsToken)
        assertEquals("http://192.168.1.10:32400", plex.urlExample)
        val subsonic = SettingsMediaServersForm(kind = "subsonic")
        assertTrue(subsonic.takesUsername)
        assertFalse(subsonic.takesPathMap)
        assertFalse(subsonic.takesWebUrl)
        assertEquals("http://192.168.1.10:4533", subsonic.urlExample)
        val kodi = SettingsMediaServersForm(kind = "kodi")
        assertTrue(kodi.takesUsername)
        assertTrue(kodi.takesPathMap)
        val jellyfin = SettingsMediaServersForm()
        assertEquals("jellyfin", jellyfin.kind)
        assertTrue(jellyfin.takesWebUrl)
        assertFalse(jellyfin.takesUsername)
        assertFalse(jellyfin.canSave)
        assertTrue(jellyfin.copy(name = "Living room").canSave)
    }

    @Test
    fun rowsPickTheResultLineTheWebPicks() {
        val base =
            SettingsMediaServersRow(
                id = 1,
                name = "Home",
                kind = "jellyfin",
                kindName = "Jellyfin",
                url = "http://j",
                enabled = true,
                onImport = true,
                onRename = false,
                onDelete = false,
                pathFrom = "/data",
                pathTo = "/media",
                lastOkAt = "2026-01-02T03:04:05Z",
                lastError = "",
                tag = null,
                testing = false,
                busy = false,
            )
        assertTrue(base.showsLastChecked)
        assertFalse(base.showsLastError)
        assertTrue(base.hasPathMap)
        assertFalse(base.toldNothing)
        assertTrue(base.copy(lastError = "boom").showsLastError)
        assertFalse(base.copy(lastError = "boom").showsLastChecked)
        val failed = SettingsMediaServersTag(false, "x", "Jellyfin", 0, "x", "X.", null, "")
        assertTrue(base.copy(tag = failed, lastError = "boom").showsFailure)
        assertFalse(base.copy(tag = failed, lastError = "boom").showsLastError)
        val ok = failed.copy(ok = true)
        assertFalse(base.copy(tag = ok).showsFailure)
        assertTrue(base.copy(onImport = false).toldNothing)
        assertEquals("Jellyfin v10.9.1", ok.copy(text = "10.9.1").label("Connected", ""))
    }

    @Test
    fun formInputSendsOnlyWhatTheKindTakes() {
        val body =
            SettingsMediaServersForm(
                kind = "subsonic",
                name = " Music ",
                url = " http://nas:4533 ",
                username = " ana ",
                token = " pw ",
                webUrl = "https://ignored",
                pathFrom = "/a",
                pathTo = "/b",
                plainPassword = true,
            ).input()
        assertEquals("Music", body.name)
        assertEquals("subsonic", body.kind)
        assertEquals("http://nas:4533", body.url)
        assertEquals("ana", body.username)
        assertEquals("pw", body.token)
        assertEquals("", body.webUrl)
        assertEquals("", body.pathFrom)
        assertEquals("", body.pathTo)
        assertEquals(true, body.plainPassword)
        assertNull(body.usePlexSignIn)
        val change =
            SettingsMediaServersForm(id = 4, kind = "jellyfin", name = "J", url = "http://j", webUrl = " https://j.example ").input()
        assertNull(change.kind)
        assertNull(change.token)
        assertNull(change.username)
        assertNull(change.plainPassword)
        assertEquals("https://j.example", change.webUrl)
        val plex = SettingsMediaServersForm(kind = "plex", name = "P", url = "http://p", token = "ignored", usePlexSignIn = true).input()
        assertEquals(true, plex.usePlexSignIn)
        assertNull(plex.token)
    }
}
