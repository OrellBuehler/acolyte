package ch.orellbuehler.acolyte.nav

import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part

enum class SectionGroup {
    DISCOVER,
    LIBRARY,
    YOURS,
    ACTIVITY,
    SYSTEM,
    YOU,
}

enum class MoreTab {
    MORE,
    MANAGE,
}

data class MoreGroup(
    val group: SectionGroup,
    val sections: List<Section>,
)

enum class Section(
    internal val group: SectionGroup,
    internal val operation: String,
    internal val need: Need,
    internal val part: Part,
) {
    HOME(SectionGroup.DISCOVER, "GET /api/v1/home", Need.SIGNED_IN, Part.MEDIA),
    EXPLORE(SectionGroup.DISCOVER, "GET /api/v1/explore/head", Need.SIGNED_IN, Part.MEDIA),
    LIBRARY(SectionGroup.LIBRARY, "GET /api/v1/titles", Need.SIGNED_IN, Part.MEDIA),
    SEARCH(SectionGroup.DISCOVER, "GET /api/v1/search", Need.SIGNED_IN, Part.MEDIA),
    POPULAR(SectionGroup.DISCOVER, "GET /api/v1/explore/new", Need.SIGNED_IN, Part.MEDIA),
    MOVIES(SectionGroup.LIBRARY, "GET /api/v1/titles", Need.SIGNED_IN, Part.MEDIA),
    SHOWS(SectionGroup.LIBRARY, "GET /api/v1/titles", Need.SIGNED_IN, Part.MEDIA),
    MUSIC(SectionGroup.LIBRARY, "GET /api/v1/titles", Need.SIGNED_IN, Part.MEDIA),
    STATS(SectionGroup.LIBRARY, "GET /api/v1/library/stats", Need.MANAGE_LIBRARY, Part.MEDIA),
    DOWNLOADS(SectionGroup.YOURS, "GET /api/v1/downloads/view", Need.SIGNED_IN, Part.CORE),
    CALENDAR(SectionGroup.YOURS, "GET /api/v1/calendar", Need.SIGNED_IN, Part.MEDIA),
    REQUESTS(SectionGroup.YOURS, "GET /api/v1/asks", Need.SIGNED_IN, Part.WISHLIST),
    HISTORY(SectionGroup.ACTIVITY, "GET /api/v1/events", Need.MANAGE_LIBRARY, Part.CORE),
    HEALTH(SectionGroup.SYSTEM, "GET /api/v1/health/checks", Need.MANAGE_LIBRARY, Part.CORE),
    PEOPLE(SectionGroup.SYSTEM, "GET /api/v1/users", Need.USERS, Part.CORE),
    SETTINGS(SectionGroup.SYSTEM, "GET /api/v1/settings/status", Need.SETTINGS, Part.CORE),
    ACCOUNT(SectionGroup.YOU, "GET /api/v1/my/profile", Need.SIGNED_IN, Part.CORE),
}
