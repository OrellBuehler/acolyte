package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.SettingsSourceDay
import ch.orellbuehler.acolyte.api.model.SettingsSourceHealth
import ch.orellbuehler.acolyte.api.model.SettingsSourceLimits
import ch.orellbuehler.acolyte.api.model.SettingsSourceUse
import ch.orellbuehler.acolyte.api.model.SourceRef
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceDownloader
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceForm
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceKeyKind
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceProblem
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceWord
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesLogic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSourcesLogicTest {
    private val torznab = SettingsSourceForm.blank().copy(name = " Jackett TV ", url = " https://jackett.example/api ", apiKey = "abc")

    @Test
    fun torznabBodyMirrorsTheWebForm() {
        val body =
            torznab
                .copy(
                    tags = listOf("anime"),
                    multiLanguages = "FR, en;it  de",
                    noLanguageLanguages = "uk",
                    downloaderId = 4,
                ).body()
        assertEquals("Jackett TV", body.name)
        assertEquals("torznab", body.format)
        assertNull(body.site)
        assertNull(body.siteLogin)
        assertEquals("torrent", body.protocolName)
        assertEquals("https://jackett.example/api", body.url)
        assertEquals("abc", body.apiKey)
        assertEquals(SettingsSourceUse(feed = true, autoSearch = true, handSearch = true), body.useFor)
        assertEquals(listOf("movie", "tv", "music"), body.kinds)
        assertEquals(1, body.minSeeders)
        assertEquals(25, body.priority)
        assertEquals(SettingsSourceLimits(searchesPerWindow = 0, windowSeconds = 3600, gapMs = 0, timeoutMs = 30000), body.limits)
        assertEquals(listOf("fr", "en", "it", "de"), body.multiLanguages)
        assertEquals(listOf("uk"), body.noLanguageLanguages)
        assertEquals(4, body.downloaderId)
    }

    @Test
    fun usenetSendsNoSeedersAndRssNeverSearches() {
        val usenet = torznab.copy(format = "newznab", protocolName = "usenet", minSeeders = "5").body()
        assertEquals(0, usenet.minSeeders)
        val rss = torznab.copy(format = "rss", feed = true, autoSearch = true, handSearch = true).body()
        assertEquals(SettingsSourceUse(feed = true, autoSearch = false, handSearch = false), rss.useFor)
    }

    @Test
    fun limitsConvertHoursAndSeconds() {
        val body =
            torznab
                .copy(
                    windowHours = "0.5",
                    timeoutSeconds = "12,5",
                    searchesPerWindow = "100",
                    gapMs = "2000",
                    priority = "",
                ).body()
        assertEquals(1800, body.limits.windowSeconds)
        assertEquals(12500, body.limits.timeoutMs)
        assertEquals(100, body.limits.searchesPerWindow)
        assertEquals(2000, body.limits.gapMs)
        assertEquals(25, body.priority)
    }

    @Test
    fun pickingASiteFillsTheForm() {
        val site = SettingsSourceForm.blank().withFormat("site")
        assertEquals("knaben", site.site)
        assertEquals("Knaben", site.name)
        assertEquals("https://api.knaben.org", site.url)
        assertEquals("torrent", site.protocolName)
        assertEquals("100", site.searchesPerWindow)
        assertEquals("2000", site.gapMs)
        assertEquals("20", site.timeoutSeconds)
        val ipt = site.withSite("iptorrents")
        assertEquals("IPTorrents", ipt.name)
        assertEquals("60", ipt.searchesPerWindow)
        assertEquals("3000", ipt.gapMs)
        val named = site.copy(name = "My own").withSite("subsplease")
        assertEquals("My own", named.name)
        assertEquals(listOf("tv"), named.kinds)
        assertFalse(site.withSite("torrentscsv").feed)
        assertTrue(site.withSite("unknown").site == "knaben")
    }

    @Test
    fun ipTorrentsSignInModes() {
        val passkey =
            SettingsSourceForm
                .blank()
                .withFormat("site")
                .withSite("iptorrents")
                .copy(siteUser = " 12345 ", apiKey = "pk")
        assertEquals(SettingsSourceKeyKind.PASSKEY, passkey.keyKind)
        assertTrue(passkey.showUser)
        assertTrue(passkey.showLoginChoice)
        val body = passkey.body()
        assertEquals("site", body.format)
        assertEquals("iptorrents", body.site)
        assertEquals("passkey", body.siteLogin)
        assertEquals("12345", body.siteUser)
        assertNull(body.siteAgent)
        assertEquals("torrent", body.protocolName)
        val cookie = passkey.copy(siteLogin = "cookie", siteAgent = " Mozilla ", apiKey = "uid=1; pass=2")
        assertEquals(SettingsSourceKeyKind.COOKIE, cookie.keyKind)
        assertTrue(cookie.showAgent)
        val cookieBody = cookie.body()
        assertEquals("cookie", cookieBody.siteLogin)
        assertEquals("Mozilla", cookieBody.siteAgent)
        assertNull(cookieBody.siteUser)
        val knaben = SettingsSourceForm.blank().withFormat("site")
        assertEquals(SettingsSourceKeyKind.NONE, knaben.keyKind)
        assertNull(knaben.body().siteLogin)
    }

    @Test
    fun editingKeepsTheSiteAndLocksChoices() {
        val source =
            SourceRef(
                id = 3,
                name = "IPT",
                protocolName = "torrent",
                format = "site",
                site = "iptorrents",
                siteUser = "9",
                siteLogin = "cookie",
                siteAgent = "UA",
                url = "https://iptorrents.com",
                apiKey = "••••••••",
                useFor = SettingsSourceUse(true, true, false),
                kinds = listOf("tv"),
                minSeeders = 2,
                limits = SettingsSourceLimits(searchesPerWindow = 60, windowSeconds = 5400, gapMs = 3000, timeoutMs = 30000),
                priority = 7,
                tags = listOf("a"),
                multiLanguages = listOf("fr", "en"),
                noLanguageLanguages = listOf("uk"),
                downloaderId = 2,
            )
        val form = SettingsSourceForm.from(source)
        assertFalse(form.isNew)
        assertTrue(form.formatLocked)
        assertTrue(form.siteLocked)
        assertFalse(form.showLoginChoice)
        assertEquals("1.5", form.windowHours)
        assertEquals("30", form.timeoutSeconds)
        assertEquals("fr, en", form.multiLanguages)
        assertEquals("cookie", form.siteLogin)
        assertEquals("••••••••", form.body().apiKey)
        val bridge = SettingsSourceForm.from(source.copy(format = "bridge", site = ""))
        assertEquals("torznab", bridge.format)
        assertFalse(bridge.formatLocked)
    }

    @Test
    fun validationMirrorsTheServer() {
        assertTrue(torznab.problems().isEmpty())
        assertTrue(torznab.canSave)
        assertFalse(torznab.copy(name = " ").canSave)
        assertFalse(torznab.copy(url = "").canSave)
        assertEquals(listOf(SettingsSourceProblem.NAME), torznab.copy(name = "x".repeat(81)).problems())
        assertEquals(listOf(SettingsSourceProblem.URL), torznab.copy(url = "ftp://x").problems())
        assertEquals(listOf(SettingsSourceProblem.URL), torznab.copy(url = "https://user@host/api").problems())
        assertEquals(listOf(SettingsSourceProblem.URL_QUERY), torznab.copy(url = "https://x/api?apikey=1").problems())
        assertTrue(torznab.copy(format = "rss", url = "https://x/rss?passkey=1").problems().isEmpty())
        assertEquals(listOf(SettingsSourceProblem.PRIORITY), torznab.copy(priority = "51").problems())
        assertEquals(listOf(SettingsSourceProblem.PRIORITY), torznab.copy(priority = "x").problems())
        assertEquals(listOf(SettingsSourceProblem.SEEDERS), torznab.copy(minSeeders = "-1").problems())
        assertEquals(listOf(SettingsSourceProblem.SEARCHES), torznab.copy(searchesPerWindow = "10001").problems())
        assertEquals(listOf(SettingsSourceProblem.WINDOW), torznab.copy(windowHours = "169").problems())
        assertTrue(torznab.copy(windowHours = "168").problems().isEmpty())
        assertEquals(listOf(SettingsSourceProblem.GAP), torznab.copy(gapMs = "600001").problems())
        assertEquals(listOf(SettingsSourceProblem.TIMEOUT), torznab.copy(timeoutSeconds = "301").problems())
        assertEquals(listOf(SettingsSourceProblem.LANGUAGES), torznab.copy(noLanguageLanguages = "uk, 12").problems())
        val ipt = SettingsSourceForm.blank().withFormat("site").withSite("iptorrents")
        assertEquals(listOf(SettingsSourceProblem.SITE_USER), ipt.copy(siteUser = "12a").problems())
        assertEquals(listOf(SettingsSourceProblem.SITE_AGENT), ipt.copy(siteLogin = "cookie", siteAgent = "a\nb").problems())
    }

    @Test
    fun proxyAddresses() {
        assertTrue(SettingsSourcesLogic.isProxy(""))
        assertTrue(SettingsSourcesLogic.isProxy("socks5://name@host:1080"))
        assertTrue(SettingsSourcesLogic.isProxy("http://host:3128/"))
        assertTrue(SettingsSourcesLogic.isProxy("https://[::1]:8080"))
        assertFalse(SettingsSourcesLogic.isProxy("http://host"))
        assertFalse(SettingsSourcesLogic.isProxy("socks4://host:1"))
        assertFalse(SettingsSourcesLogic.isProxy("http://host:0"))
        assertFalse(SettingsSourcesLogic.isProxy("http://host:70000"))
        assertFalse(SettingsSourcesLogic.isProxy("http://host:80/path"))
        assertEquals(listOf(SettingsSourceProblem.PROXY), torznab.copy(proxy = "host:80").problems())
    }

    @Test
    fun sendToFollowsTheProtocolAndNoticesARemovedDownloader() {
        val all =
            listOf(
                SettingsSourceDownloader(1, "qBit", "torrent"),
                SettingsSourceDownloader(2, "SAB", "usenet"),
            )
        assertNull(torznab.sendTo(null))
        assertEquals(listOf(1L), torznab.sendTo(all)?.map { it.id })
        assertEquals(listOf(2L), torznab.copy(protocolName = "usenet").sendTo(all)?.map { it.id })
        val kept = torznab.copy(downloaderId = 1).settled(all)
        assertEquals(1, kept.downloaderId)
        assertFalse(kept.sendGone)
        val editing = SettingsSourceForm.from(SourceRef(id = 5, name = "x", protocolName = "torrent", format = "torznab", downloaderId = 9))
        val gone = editing.settled(all)
        assertEquals(0, gone.downloaderId)
        assertTrue(gone.sendGone)
        val switched = editing.copy(protocolName = "usenet").settled(all)
        assertEquals(0, switched.downloaderId)
        assertFalse(switched.sendGone)
        assertEquals(9, editing.settled(null).downloaderId)
    }

    @Test
    fun healthWordsAndTones() {
        fun health(
            state: String,
            reason: String = "",
        ) = SettingsSourceHealth(state = state, reason = reason)
        assertEquals(SettingsSourceWord.WORKING, SettingsSourcesLogic.word(health("working")))
        assertEquals(Tone.OK, SettingsSourcesLogic.tone(health("working")))
        assertEquals(SettingsSourceWord.UNREACHABLE, SettingsSourcesLogic.word(health("unreachable")))
        assertEquals(Tone.FAIL, SettingsSourcesLogic.tone(health("unreachable")))
        assertEquals(SettingsSourceWord.KEY_TURNED_DOWN, SettingsSourcesLogic.word(health("auth_failed")))
        assertEquals(SettingsSourceWord.PAUSED, SettingsSourcesLogic.word(health("disabled")))
        assertEquals(Tone.IDLE, SettingsSourcesLogic.tone(health("disabled")))
        assertEquals(SettingsSourceWord.SLOWED, SettingsSourcesLogic.word(health("unreachable", "slowed")))
        assertEquals(Tone.WAIT, SettingsSourcesLogic.tone(health("unreachable", "slowed")))
        assertEquals(SettingsSourceWord.DOWN, SettingsSourcesLogic.word(health("unreachable", "down")))
        assertEquals(SettingsSourceWord.BLOCKED, SettingsSourcesLogic.word(health("unreachable", "blocked")))
        assertEquals(SettingsSourceWord.KEY_REFUSED, SettingsSourcesLogic.word(health("auth_failed", "key")))
        assertEquals(Tone.FAIL, SettingsSourcesLogic.tone(health("auth_failed", "key")))
        assertEquals(SettingsSourceWord.OTHER, SettingsSourcesLogic.word(health("new_state")))
        assertEquals("new state", SettingsSourcesLogic.rawWord(health("new_state")))
    }

    @Test
    fun numbersAndDurations() {
        assertEquals("1", SettingsSourcesLogic.formatNumber(1.0))
        assertEquals("0.5", SettingsSourcesLogic.formatNumber(0.5))
        assertEquals("0.0003", SettingsSourcesLogic.formatNumber(1 / 3600.0))
        assertEquals(1.5, SettingsSourcesLogic.number("1,5"))
        assertEquals(0.0, SettingsSourcesLogic.number("  "))
        assertNull(SettingsSourcesLogic.number("abc"))
        assertEquals("—", SettingsSourcesLogic.durationText(0, '.'))
        assertEquals("420 ms", SettingsSourcesLogic.durationText(420, '.'))
        assertEquals("1.2 s", SettingsSourcesLogic.durationText(1234, '.'))
        assertEquals("12,0 s", SettingsSourcesLogic.durationText(12000, ','))
    }

    @Test
    fun barsScaleToTheBusiestDay() {
        val bars =
            SettingsSourcesLogic.bars(
                listOf(
                    SettingsSourceDay("2026-01-01", calls = 10, failures = 2, grabs = 1, avgMs = 500),
                    SettingsSourceDay("2026-01-02", calls = 0, failures = 0, grabs = 0, avgMs = 0),
                    SettingsSourceDay("2026-01-03", calls = 1, failures = 0, grabs = 0, avgMs = 100),
                ),
            )
        assertEquals(100, bars[0].callsPercent)
        assertEquals(20, bars[0].failedPercent)
        assertEquals(8, bars[0].answered)
        assertEquals(0, bars[1].callsPercent)
        assertEquals(10, bars[2].callsPercent)
        assertEquals(20, bars[2].timePercent)
        assertEquals(4, SettingsSourcesLogic.barPercent(1, 1000))
    }

    @Test
    fun callbackAddressForProwlarr() {
        assertEquals("http://saved:6262", SettingsSourcesLogic.callbackAddress("https://x", "", "http://saved:6262"))
        assertEquals(
            "https://holy.example.com/holyarrs",
            SettingsSourcesLogic.callbackAddress("https://holy.example.com/holyarrs/", "", ""),
        )
        assertEquals("http://192.168.1.4:6262", SettingsSourcesLogic.callbackAddress("http://localhost:6262", "192.168.1.4", ""))
        assertEquals("http://192.168.1.4:6262/h", SettingsSourcesLogic.callbackAddress("http://100.100.1.2:6262/h", "192.168.1.4", ""))
        assertEquals("", SettingsSourcesLogic.callbackAddress("http://127.0.0.1:6262", "", ""))
        assertEquals("http://nas.ts.net", SettingsSourcesLogic.callbackAddress("http://nas.ts.net", "", ""))
        assertEquals("http://192.168.1.4", SettingsSourcesLogic.callbackAddress("http://nas.ts.net", "192.168.1.4", ""))
    }

    @Test
    fun checkTitlesFollowTheKindOnlyWhileUntouched() {
        assertEquals("Breaking Bad", SettingsSourcesLogic.titleAfterKind("movie", "The Matrix", "show"))
        assertEquals("Daft Punk", SettingsSourcesLogic.titleAfterKind("show", "Breaking Bad", "music"))
        assertEquals("Dune", SettingsSourcesLogic.titleAfterKind("movie", "Dune", "show"))
    }

    @Test
    fun versionsAndServerText() {
        assertEquals("v2.0.5", SettingsSourcesLogic.version("Prowlarr 2.0.5.5160 answered."))
        assertEquals("v1.2", SettingsSourcesLogic.version("Prowlarr v1.2 answered."))
        assertEquals("", SettingsSourcesLogic.version("Jackett answered with 3 sites."))
        assertTrue(SettingsSourcesLogic.showsServerText(ApiError(ApiErrorCode.CONFLICT, 409, "another Source has that name")))
        assertFalse(SettingsSourcesLogic.showsServerText(ApiError(ApiErrorCode.NOT_FOUND, 404, "no such Source")))
        assertFalse(SettingsSourcesLogic.showsServerText(ApiError(ApiErrorCode.OFFLINE)))
    }
}
