package ch.orellbuehler.acolyte.feature.sources

import ch.orellbuehler.acolyte.api.model.SettingsSourceInput
import ch.orellbuehler.acolyte.api.model.SettingsSourceLimits
import ch.orellbuehler.acolyte.api.model.SettingsSourceUse
import ch.orellbuehler.acolyte.api.model.SourceRef
import ch.orellbuehler.acolyte.feature.library.LibraryLogic
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class SettingsBuiltInSite(
    val id: String,
    val name: String,
    val url: String,
    val needsKey: Boolean,
    val needsUser: Boolean,
    val kinds: List<String>,
    val gapMs: Int,
    val perHour: Int,
    val timeoutSeconds: Int,
    val hasNewestList: Boolean,
)

object SettingsSourceSites {
    private val everything = listOf("movie", "tv", "music")

    val all: List<SettingsBuiltInSite> =
        listOf(
            SettingsBuiltInSite("knaben", "Knaben", "https://api.knaben.org", false, false, everything, 2000, 100, 20, true),
            SettingsBuiltInSite("torrentscsv", "TorrentsCSV", "https://torrents-csv.com", false, false, everything, 2000, 100, 20, false),
            SettingsBuiltInSite("subsplease", "SubsPlease", "https://subsplease.org", false, false, listOf("tv"), 2000, 100, 20, true),
            SettingsBuiltInSite("iptorrents", "IPTorrents", "https://iptorrents.com", true, true, everything, 3000, 60, 30, true),
        )

    fun byId(id: String): SettingsBuiltInSite? = all.firstOrNull { it.id == id }
}

enum class SettingsSourceProblem {
    NAME,
    URL,
    URL_QUERY,
    PRIORITY,
    SEEDERS,
    SEARCHES,
    WINDOW,
    GAP,
    TIMEOUT,
    PROXY,
    SITE_USER,
    SITE_AGENT,
    LANGUAGES,
}

enum class SettingsSourceKeyKind {
    NONE,
    API_KEY,
    PASSKEY,
    COOKIE,
}

enum class SettingsSourceAddressHint {
    FEED,
    RSS,
    SITE,
}

data class SettingsDownloaderChoice(
    val id: Long,
    val name: String,
    val protocolName: String,
)

data class SettingsSourceForm(
    val id: Long = 0,
    val isNew: Boolean = true,
    val name: String = "",
    val format: String = FORMAT_TORZNAB,
    val site: String = "",
    val siteUser: String = "",
    val siteLogin: String = LOGIN_PASSKEY,
    val siteAgent: String = "",
    val protocolName: String = PROTOCOL_TORRENT,
    val url: String = "",
    val apiKey: String = "",
    val feed: Boolean = true,
    val autoSearch: Boolean = true,
    val handSearch: Boolean = true,
    val kinds: List<String> = ALL_KINDS,
    val minSeeders: String = "1",
    val priority: String = "25",
    val searchesPerWindow: String = "0",
    val windowHours: String = "1",
    val gapMs: String = "0",
    val timeoutSeconds: String = "30",
    val allowLocalNetwork: Boolean = false,
    val proxy: String = "",
    val proxyPassword: String = "",
    val proxyAllowLocalNetwork: Boolean = false,
    val tags: List<String> = emptyList(),
    val multiLanguages: String = "",
    val noLanguageLanguages: String = "",
    val downloaderId: Long = 0,
    val sendGone: Boolean = false,
    val originalProtocol: String = "",
    val originalDownloaderId: Long = 0,
) {
    val builtIn: SettingsBuiltInSite? get() = SettingsSourceSites.byId(site)

    val isSite: Boolean get() = format == FORMAT_SITE

    val isRss: Boolean get() = format == FORMAT_RSS

    val needsUser: Boolean get() = isSite && builtIn?.needsUser == true

    val cookieMode: Boolean get() = needsUser && siteLogin == LOGIN_COOKIE

    val showSiteFormat: Boolean get() = isNew || isSite

    val formatLocked: Boolean get() = !isNew && isSite

    val siteLocked: Boolean get() = !isNew

    val showLoginChoice: Boolean get() = needsUser && isNew

    val showUser: Boolean get() = needsUser && !cookieMode

    val showAgent: Boolean get() = cookieMode

    val showProtocol: Boolean get() = !isSite

    val showSearchUse: Boolean get() = !isRss

    val showSeeders: Boolean get() = protocolName == PROTOCOL_TORRENT

    val sendProtocol: String get() = if (isSite) PROTOCOL_TORRENT else protocolName

    val keyKind: SettingsSourceKeyKind
        get() =
            when {
                isSite && cookieMode -> SettingsSourceKeyKind.COOKIE
                isSite && builtIn?.needsKey == true -> SettingsSourceKeyKind.PASSKEY
                isSite || isRss -> SettingsSourceKeyKind.NONE
                else -> SettingsSourceKeyKind.API_KEY
            }

    val addressHint: SettingsSourceAddressHint
        get() =
            when {
                isRss -> SettingsSourceAddressHint.RSS
                isSite -> SettingsSourceAddressHint.SITE
                else -> SettingsSourceAddressHint.FEED
            }

    val newestListWorks: Boolean get() = !isSite || builtIn?.hasNewestList != false

    fun withFormat(value: String): SettingsSourceForm {
        val next = copy(format = value)
        return if (value == FORMAT_SITE) {
            val chosen = if (site.isEmpty()) SettingsSourceSites.all.first().id else site
            if (site.isEmpty()) next.withSite(chosen) else next.copy(protocolName = PROTOCOL_TORRENT)
        } else {
            next
        }
    }

    fun withSite(id: String): SettingsSourceForm {
        val chosen = SettingsSourceSites.byId(id) ?: return this
        val before = SettingsSourceSites.byId(site)
        return copy(
            site = chosen.id,
            protocolName = PROTOCOL_TORRENT,
            url = chosen.url,
            name = if (name.isBlank() || name == before?.name) chosen.name else name,
            kinds = chosen.kinds,
            gapMs = chosen.gapMs.toString(),
            searchesPerWindow = chosen.perHour.toString(),
            windowHours = "1",
            timeoutSeconds = chosen.timeoutSeconds.toString(),
            feed = chosen.hasNewestList,
        )
    }

    fun withTagText(text: String): SettingsSourceForm = copy(tags = cleanTags(text.split(',')))

    fun withKind(
        kind: String,
        on: Boolean,
    ): SettingsSourceForm = copy(kinds = if (on) (kinds + kind).distinct() else kinds.filter { it != kind })

    internal fun settled(downloaders: List<SettingsDownloaderChoice>?): SettingsSourceForm {
        if (downloaders == null) return this
        val known = downloaders.any { it.protocolName == sendProtocol && it.id == downloaderId }
        if (downloaderId <= 0 || known) return copy(sendGone = false)
        val gone = !isNew && sendProtocol == originalProtocol && downloaderId == originalDownloaderId
        return copy(downloaderId = 0, sendGone = gone)
    }

    fun sendTo(downloaders: List<SettingsDownloaderChoice>?): List<SettingsDownloaderChoice>? =
        downloaders?.filter { it.protocolName == sendProtocol }

    fun problems(): List<SettingsSourceProblem> {
        val out = mutableListOf<SettingsSourceProblem>()
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > NAME_MAX) out += SettingsSourceProblem.NAME
        when {
            !SettingsSourcesLogic.isWebAddress(url.trim()) -> out += SettingsSourceProblem.URL
            !isRss && url.contains('?') -> out += SettingsSourceProblem.URL_QUERY
        }
        if (priorityValue() == null) out += SettingsSourceProblem.PRIORITY
        if (intOf(minSeeders, 0, Int.MAX_VALUE) == null) out += SettingsSourceProblem.SEEDERS
        if (intOf(searchesPerWindow, 0, MAX_SEARCHES) == null) out += SettingsSourceProblem.SEARCHES
        if (windowSeconds() == null) out += SettingsSourceProblem.WINDOW
        if (intOf(gapMs, 0, MAX_GAP_MS) == null) out += SettingsSourceProblem.GAP
        if (timeoutMs() == null) out += SettingsSourceProblem.TIMEOUT
        if (!SettingsSourcesLogic.isProxy(proxy.trim())) out += SettingsSourceProblem.PROXY
        if (needsUser && !cookieMode && (siteUser.trim().length > USER_MAX || !siteUser.trim().all { it in '0'..'9' })) {
            out += SettingsSourceProblem.SITE_USER
        }
        if (cookieMode && (siteAgent.trim().length > AGENT_MAX || siteAgent.any { it == '\n' || it == '\r' })) {
            out += SettingsSourceProblem.SITE_AGENT
        }
        if (codes(noLanguageLanguages).any { !LANGUAGE.matches(it) }) out += SettingsSourceProblem.LANGUAGES
        return out
    }

    val canSave: Boolean get() = name.isNotBlank() && url.isNotBlank() && problems().isEmpty()

    internal fun body(): SettingsSourceInput {
        val rss = isRss
        val siteId = if (isSite) site else null
        val user = needsUser
        return SettingsSourceInput(
            name = name.trim(),
            format = format,
            site = siteId,
            siteLogin = if (user) (if (cookieMode) LOGIN_COOKIE else LOGIN_PASSKEY) else null,
            siteAgent = if (user && cookieMode) siteAgent.trim() else null,
            siteUser = if (user && !cookieMode) siteUser.trim() else null,
            protocolName = sendProtocol,
            url = url.trim(),
            apiKey = apiKey,
            useFor = SettingsSourceUse(feed, if (rss) false else autoSearch, if (rss) false else handSearch),
            kinds = kinds,
            minSeeders = if (protocolName == PROTOCOL_TORRENT) intOf(minSeeders, 0, Int.MAX_VALUE) ?: 0 else 0,
            priority = priorityValue() ?: DEFAULT_PRIORITY,
            limits =
                SettingsSourceLimits(
                    searchesPerWindow = intOf(searchesPerWindow, 0, MAX_SEARCHES) ?: 0,
                    windowSeconds = windowSeconds() ?: 0,
                    gapMs = intOf(gapMs, 0, MAX_GAP_MS) ?: 0,
                    timeoutMs = timeoutMs() ?: 0,
                ),
            allowLocalNetwork = allowLocalNetwork,
            proxy = proxy.trim(),
            proxyPassword = proxyPassword,
            proxyAllowLocalNetwork = proxyAllowLocalNetwork,
            tags = tags,
            multiLanguages = codes(multiLanguages),
            noLanguageLanguages = codes(noLanguageLanguages),
            downloaderId = downloaderId,
        )
    }

    private fun priorityValue(): Int? {
        val n = intOf(priority, 0, MAX_PRIORITY) ?: return null
        return if (n == 0) DEFAULT_PRIORITY else n
    }

    private fun windowSeconds(): Int? {
        val hours = SettingsSourcesLogic.number(windowHours) ?: return null
        val seconds = (hours * SECONDS_PER_HOUR).roundToLong()
        return if (hours < 0 || seconds > MAX_WINDOW_SECONDS) null else seconds.toInt()
    }

    private fun timeoutMs(): Int? {
        val seconds = SettingsSourcesLogic.number(timeoutSeconds) ?: return null
        val ms = (seconds * MILLIS).roundToLong()
        return if (seconds < 0 || ms > MAX_TIMEOUT_MS) null else ms.toInt()
    }

    companion object {
        const val FORMAT_TORZNAB = "torznab"
        const val FORMAT_NEWZNAB = "newznab"
        const val FORMAT_RSS = "rss"
        const val FORMAT_SITE = "site"
        const val PROTOCOL_TORRENT = "torrent"
        const val PROTOCOL_USENET = "usenet"
        const val LOGIN_PASSKEY = "passkey"
        const val LOGIN_COOKIE = "cookie"
        const val NAME_MAX = 80
        const val DEFAULT_PRIORITY = 25
        const val MAX_PRIORITY = 50
        const val MAX_SEARCHES = 10000
        const val MAX_GAP_MS = 600000
        const val MAX_TIMEOUT_MS = 300000
        const val MAX_WINDOW_SECONDS = 7 * 86400
        const val USER_MAX = 20
        const val AGENT_MAX = 500
        val ALL_KINDS: List<String> = listOf("movie", "tv", "music")
        private const val SECONDS_PER_HOUR = 3600.0
        private const val MILLIS = 1000.0
        private val LANGUAGE = Regex("[a-z]{2,3}")

        fun blank(): SettingsSourceForm = SettingsSourceForm()

        fun from(s: SourceRef): SettingsSourceForm =
            SettingsSourceForm(
                id = s.id,
                isNew = false,
                name = s.name,
                format = if (s.format == "bridge") FORMAT_TORZNAB else s.format,
                site = s.site,
                siteUser = s.siteUser,
                siteLogin = if (s.siteLogin == LOGIN_COOKIE) LOGIN_COOKIE else LOGIN_PASSKEY,
                siteAgent = s.siteAgent,
                protocolName = s.protocolName,
                url = s.url,
                apiKey = s.apiKey,
                feed = s.useFor.feed,
                autoSearch = s.useFor.autoSearch,
                handSearch = s.useFor.handSearch,
                kinds = s.kinds,
                minSeeders = (s.minSeeders ?: 0).toString(),
                priority = s.priority.toString(),
                searchesPerWindow = s.limits.searchesPerWindow.toString(),
                windowHours =
                    if (s.limits.windowSeconds >
                        0
                    ) {
                        SettingsSourcesLogic.formatNumber(s.limits.windowSeconds / SECONDS_PER_HOUR)
                    } else {
                        "1"
                    },
                gapMs = s.limits.gapMs.toString(),
                timeoutSeconds = if (s.limits.timeoutMs > 0) SettingsSourcesLogic.formatNumber(s.limits.timeoutMs / MILLIS) else "30",
                allowLocalNetwork = s.allowLocalNetwork,
                proxy = s.proxy,
                proxyPassword = s.proxyPassword,
                proxyAllowLocalNetwork = s.proxyAllowLocalNetwork,
                tags = s.tags,
                multiLanguages = s.multiLanguages.joinToString(", "),
                noLanguageLanguages = s.noLanguageLanguages.joinToString(", "),
                downloaderId = s.downloaderId,
                originalProtocol = s.protocolName,
                originalDownloaderId = s.downloaderId,
            )

        fun codes(text: String): List<String> =
            text
                .split(Regex("[\\s,;]+"))
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }

        fun cleanTags(tags: List<String>): List<String> = LibraryLogic.cleanTags(tags)

        private fun intOf(
            text: String,
            min: Int,
            max: Int,
        ): Int? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return 0.takeIf { it in min..max }
            val n = SettingsSourcesLogic.number(trimmed) ?: return null
            if (n != n.roundToInt().toDouble() || n < min || n > max) return null
            return n.roundToInt()
        }
    }
}
