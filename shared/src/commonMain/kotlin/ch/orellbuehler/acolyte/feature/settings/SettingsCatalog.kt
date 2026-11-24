package ch.orellbuehler.acolyte.feature.settings

import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role

enum class SettingsGroup {
    LIBRARY,
    DOWNLOADS,
    USERS,
    CONNECTIONS,
    SYSTEM,
    ACCOUNT,
}

internal enum class SettingsGate {
    SELF,
    USERS,
    OWNER,
    LIBRARY,
    SETTINGS,
}

enum class SettingsPage(
    val group: SettingsGroup,
    val href: String,
    internal val gate: SettingsGate,
    internal val part: Part = Part.CORE,
    internal val feature: String? = null,
    internal val words: String = "",
) {
    LIBRARY_FOLDERS(
        SettingsGroup.LIBRARY,
        "/settings/library-folders",
        SettingsGate.SETTINGS,
        words = "folders import extra where files live",
    ),
    TITLE_DEFAULTS(
        SettingsGroup.LIBRARY,
        "/settings/title-defaults",
        SettingsGate.LIBRARY,
        Part.MEDIA,
        words = "movies shows defaults new add release gate seasons episode order keep up to date search when added quality folder",
    ),
    MUSIC(
        SettingsGroup.LIBRARY,
        "/settings/music",
        SettingsGate.SETTINGS,
        Part.MUSIC,
        words = "artists albums pressing musicbrainz new albums",
    ),
    SUBTITLES(
        SettingsGroup.LIBRARY,
        "/settings/subtitles",
        SettingsGate.SETTINGS,
        Part.SUBTITLES,
        words = "languages sites pass mark hearing impaired sync",
    ),
    NAMING(
        SettingsGroup.LIBRARY,
        "/settings/naming",
        SettingsGate.SETTINGS,
        Part.MEDIA,
        words = "file names patterns free space holding rename on import scan",
    ),
    PARTS(
        SettingsGroup.LIBRARY,
        "/settings/parts",
        SettingsGate.USERS,
        words = "movies shows music subtitles wishlist switch off disable enable stages background reconnect restart",
    ),
    OUTSIDE_LISTS(
        SettingsGroup.LIBRARY,
        "/settings/outside-lists",
        SettingsGate.SETTINGS,
        Part.MEDIA,
        words = "trakt mdblist never add lists",
    ),
    MOVE(SettingsGroup.LIBRARY, "/settings/move", SettingsGate.USERS, words = "import sonarr radarr migrate move"),
    DOWNLOADERS(
        SettingsGroup.DOWNLOADS,
        "/settings/downloaders",
        SettingsGate.SETTINGS,
        words = "torrent usenet qbittorrent sabnzbd stalled remove failed",
    ),
    SOURCES(SettingsGroup.DOWNLOADS, "/settings/sources", SettingsGate.SETTINGS, words = "indexers jackett prowlarr releases feeds"),
    PICK_RULES(
        SettingsGroup.DOWNLOADS,
        "/settings/pick-rules",
        SettingsGate.LIBRARY,
        Part.MEDIA,
        words = "quality profiles traits points size release languages",
    ),
    SEARCH(
        SettingsGroup.DOWNLOADS,
        "/settings/search",
        SettingsGate.SETTINGS,
        Part.MEDIA,
        words = "feed minutes missing nightly season pack discographies early tiny fakes",
    ),
    SCHEDULES(
        SettingsGroup.DOWNLOADS,
        "/settings/schedules",
        SettingsGate.SETTINGS,
        words = "timer every run now how often refresh jobs tasks interval",
    ),
    USERS(SettingsGroup.USERS, "/settings/users", SettingsGate.USERS, words = "people roles invite links kids"),
    REQUESTS(
        SettingsGroup.USERS,
        "/settings/requests",
        SettingsGate.SETTINGS,
        Part.WISHLIST,
        words = "limits per week approved plex watchlists streaming",
    ),
    SIGN_IN(
        SettingsGroup.USERS,
        "/settings/sign-in",
        SettingsGate.SETTINGS,
        words =
            "password length two-factor 2fa forgot plex jellyfin emby single sign-on sso oidc local network skip " +
                "trusted proxies host names allowed",
    ),
    APPEARANCE(
        SettingsGroup.USERS,
        "/settings/appearance",
        SettingsGate.SELF,
        words = "theme language trailers autoplay sign-in art ratings explore rows",
    ),
    MEDIA_SERVERS(
        SettingsGroup.CONNECTIONS,
        "/settings/media-servers",
        SettingsGate.SETTINGS,
        words = "plex jellyfin emby navidrome subsonic kodi library update watch history labels default audio subtitles",
    ),
    ALERTS(
        SettingsGroup.CONNECTIONS,
        "/settings/alerts",
        SettingsGate.SETTINGS,
        words = "notifications discord telegram ntfy gotify pushover push email webhook notify season pack",
    ),
    AI(
        SettingsGroup.CONNECTIONS,
        "/settings/ai",
        SettingsGate.SETTINGS,
        feature = "ai_apps",
        words = "assistant claude chatgpt mcp llm agent bot connect keys",
    ),
    API(
        SettingsGroup.CONNECTIONS,
        "/settings/api",
        SettingsGate.SETTINGS,
        words = "keys token openapi routes seerr overseerr sonarr radarr address",
    ),
    REMOTE_ACCESS(
        SettingsGroup.CONNECTIONS,
        "/settings/remote-access",
        SettingsGate.OWNER,
        words = "tailscale remote away from home vpn tailnet",
    ),
    METADATA(
        SettingsGroup.CONNECTIONS,
        "/settings/tmdb",
        SettingsGate.SETTINGS,
        words = "tmdb tvdb mdblist fanart omdb keys mode privacy firewall hosts posters names",
    ),
    HEALTH(
        SettingsGroup.SYSTEM,
        "/settings/health",
        SettingsGate.SETTINGS,
        words = "checks problems check again interval free space update check release feed",
    ),
    BACKUPS(
        SettingsGroup.SYSTEM,
        "/settings/backups",
        SettingsGate.SETTINGS,
        words = "restore download delete master key kit how often schedule",
    ),
    LOGS(SettingsGroup.SYSTEM, "/settings/logs", SettingsGate.USERS, words = "log level errors warnings debug download"),
    THIS_APP(
        SettingsGroup.SYSTEM,
        "/settings/this-app",
        SettingsGate.SETTINGS,
        words = "version storage paths config history keep days picture cache url base proxy count install",
    ),
    DANGER(SettingsGroup.SYSTEM, "/settings/danger", SettingsGate.USERS, words = "restart reboot shutdown safe mode stop"),
    ACCOUNT(SettingsGroup.ACCOUNT, "/settings/account", SettingsGate.SELF, words = "password two-factor devices sessions profile"),
    ;

    val key: String get() = name.lowercase()
}

data class SettingsPageText(
    val key: String,
    val label: String,
    val about: String,
    val group: String,
)

object SettingsCatalog {
    fun pages(account: Account): List<SettingsPage> = SettingsPage.entries.filter { mayOpen(account, it) }

    fun groups(account: Account): List<Pair<SettingsGroup, List<SettingsPage>>> {
        val visible = pages(account)
        return SettingsGroup.entries.map { group -> group to visible.filter { it.group == group } }.filter { it.second.isNotEmpty() }
    }

    fun hasHome(account: Account): Boolean = !account.mustSetUpTwoFactor && (account.can(Need.SETTINGS) || account.can(Need.USERS))

    fun mayOpen(
        account: Account,
        page: SettingsPage,
    ): Boolean {
        if (page == SettingsPage.ACCOUNT) return true
        if (account.mustSetUpTwoFactor) return false
        if (page.feature != null && account.isHeld(page.feature)) return false
        if (!account.isOn(page.part)) return false
        return when (page.gate) {
            SettingsGate.SELF -> true
            SettingsGate.USERS -> account.can(Need.USERS)
            SettingsGate.OWNER -> account.role == Role.OWNER
            SettingsGate.LIBRARY -> account.can(Need.SETTINGS) || account.can(Need.MANAGE_LIBRARY)
            SettingsGate.SETTINGS -> account.can(Need.SETTINGS)
        }
    }

    fun search(
        query: String,
        texts: List<SettingsPageText>,
    ): List<String> {
        val folded = fold(query)
        val words = folded.split(' ', ',').filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        return texts
            .mapIndexedNotNull { index, text ->
                val page = SettingsPage.entries.firstOrNull { it.key == text.key } ?: return@mapIndexedNotNull null
                val label = fold(text.label)
                val haystack = "$label ${fold(text.about)} ${fold(text.group)} ${fold(page.words)}"
                if (!words.all { haystack.contains(it) }) return@mapIndexedNotNull null
                val rank =
                    when {
                        label.startsWith(folded) -> 0
                        label.contains(folded) -> 1
                        words.all { label.contains(it) } -> 2
                        else -> 3
                    }
                Triple(rank, index, page.key)
            }.sortedWith(compareBy({ it.first }, { it.second }))
            .map { it.third }
            .take(MAX_RESULTS)
    }

    internal fun fold(text: String): String =
        buildString {
            text.lowercase().forEach { char -> append(ACCENTS[char] ?: char) }
        }.trim()

    private const val MAX_RESULTS = 30

    private val ACCENTS: Map<Char, Char> =
        buildMap {
            fun add(
                base: Char,
                accented: String,
            ) = accented.forEach { put(it, base) }
            add('a', "àáâãäå")
            add('c', "ç")
            add('e', "èéêë")
            add('i', "ìíîï")
            add('n', "ñ")
            add('o', "òóôõö")
            add('u', "ùúûü")
            add('y', "ýÿ")
        }
}
