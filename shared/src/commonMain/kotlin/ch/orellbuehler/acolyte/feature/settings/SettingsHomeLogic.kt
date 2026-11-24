package ch.orellbuehler.acolyte.feature.settings

import ch.orellbuehler.acolyte.api.model.SettingsStatusAnswer
import ch.orellbuehler.acolyte.api.model.SettingsStatusItem
import ch.orellbuehler.acolyte.feature.Tone

enum class SettingsHomeStanding {
    OK,
    ATTENTION,
    UNSET,
    UNKNOWN,
}

enum class SettingsHomeReasonKind {
    SETTING,
    UPDATE,
    DECISIONS,
    REQUESTS,
}

data class SettingsHomePage(
    val page: SettingsPage,
    val standing: SettingsHomeStanding = SettingsHomeStanding.UNKNOWN,
    val count: Int = 0,
)

data class SettingsHomeGroup(
    val group: SettingsGroup,
    val pages: List<SettingsHomePage>,
    val standing: SettingsHomeStanding = SettingsHomeStanding.UNKNOWN,
    val count: Int = 0,
)

data class SettingsHomeReason(
    val key: String,
    val kind: SettingsHomeReasonKind,
    val tone: Tone,
    val page: SettingsPage?,
    val title: String = "",
    val body: String = "",
    val count: Int = 0,
)

internal object SettingsHomeLogic {
    private val pageLabels: Map<String, String> =
        mapOf(
            "/settings/library-folders" to "Library folders",
            "/settings/title-defaults" to "Movies and TV defaults",
            "/settings/music" to "Music defaults",
            "/settings/subtitles" to "Subtitles",
            "/settings/naming" to "Naming and files",
            "/settings/parts" to "Parts",
            "/settings/outside-lists" to "Outside lists",
            "/settings/move" to "Import from other apps",
            "/settings/downloaders" to "Downloaders",
            "/settings/sources" to "Sources",
            "/settings/pick-rules" to "Quality",
            "/settings/search" to "Search",
            "/settings/schedules" to "Schedules",
            "/settings/users" to "Users and roles",
            "/settings/requests" to "Request rules",
            "/settings/sign-in" to "Sign-in and security",
            "/settings/appearance" to "Appearance",
            "/settings/media-servers" to "Media servers",
            "/settings/alerts" to "Alerts",
            "/settings/ai" to "AI apps",
            "/settings/api" to "API",
            "/settings/remote-access" to "Remote access",
            "/settings/tmdb" to "Metadata",
            "/settings/health" to "Health checks",
            "/settings/backups" to "Backups",
            "/settings/logs" to "Logs",
            "/settings/this-app" to "This app",
            "/settings/danger" to "Danger zone",
        )

    fun standing(
        answer: SettingsStatusAnswer?,
        hrefs: Collection<String>,
    ): Pair<SettingsHomeStanding, Int> {
        if (answer == null || hrefs.isEmpty()) return SettingsHomeStanding.UNKNOWN to 0
        val count = answer.items.count { it.page in hrefs }
        return when {
            count > 0 -> SettingsHomeStanding.ATTENTION to count
            answer.unset.values.any { list -> list.any { it in hrefs } } -> SettingsHomeStanding.UNSET to 0
            answer.known.any { it in hrefs } -> SettingsHomeStanding.OK to 0
            else -> SettingsHomeStanding.UNKNOWN to 0
        }
    }

    fun groups(
        pages: List<Pair<SettingsGroup, List<SettingsPage>>>,
        answer: SettingsStatusAnswer?,
    ): List<SettingsHomeGroup> =
        pages
            .filter { it.first != SettingsGroup.ACCOUNT }
            .map { (group, list) ->
                val (groupStanding, groupCount) = standing(answer, list.map { it.href })
                SettingsHomeGroup(
                    group = group,
                    pages =
                        list.map { page ->
                            val (pageStanding, pageCount) = standing(answer, listOf(page.href))
                            SettingsHomePage(page, pageStanding, pageCount)
                        },
                    standing = groupStanding,
                    count = groupCount,
                )
            }

    fun settingReasons(items: List<SettingsStatusItem>): List<SettingsHomeReason> =
        items
            .map { item ->
                val tone =
                    when (item.severity) {
                        "error" -> Tone.FAIL
                        "warning" -> Tone.WAIT
                        else -> Tone.INFO
                    }
                val (title, body) = split(item)
                SettingsHomeReason(
                    key = "${item.page}|${item.text}",
                    kind = SettingsHomeReasonKind.SETTING,
                    tone = tone,
                    page = SettingsPage.entries.firstOrNull { it.href == item.page },
                    title = title,
                    body = body,
                )
            }.sortedBy { rank(it.tone) }

    fun isUpdateNews(item: SettingsStatusItem): Boolean =
        item.page == "/settings/this-app" &&
            item.severity == "info" &&
            (item.text.contains("newer HolyArrs") || item.text.contains(" is out"))

    private fun rank(tone: Tone): Int =
        when (tone) {
            Tone.FAIL -> 0
            Tone.WAIT -> 1
            else -> 2
        }

    private fun split(item: SettingsStatusItem): Pair<String, String> {
        var text = item.text.trim()
        val label = pageLabels[item.page]
        if (label != null && text.startsWith("$label: ")) text = text.removePrefix("$label: ")
        val cut = text.indexOf(": ")
        val head = if (cut >= 0) text.substring(0, cut) else text
        val tail = if (cut >= 0) text.substring(cut + 2) else ""
        return clean(head) to clean(tail)
    }

    private fun clean(text: String): String {
        val trimmed = text.trim().trimEnd('.')
        if (trimmed.isEmpty()) return trimmed
        val first = trimmed[0]
        val keepsCase = trimmed.length > 1 && first.isLowerCase() && trimmed[1].isUpperCase()
        return if (keepsCase) trimmed else first.uppercaseChar() + trimmed.substring(1)
    }
}
